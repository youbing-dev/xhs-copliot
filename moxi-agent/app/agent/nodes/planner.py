"""Planner 节点：任务拆解与执行计划编排。

职责：
1. 读取检索到的记忆上下文（``memory_context``）与爆款参考（``rag_context``）；
2. 让 LLM 以「小红书内容策划」的身份产出一份**结构化**执行计划（``PlanOutput``）；
3. 将计划写回状态，并把 ``current_step`` 归零，交由 executor 逐步执行；
4. 若为二次规划（``iteration_count > 0``），把上一轮反思的 ``quality_feedback``
   作为改进约束注入 prompt，实现 Plan-Execute-Reflect 闭环。

容错：LLM 不可用 / 结构化输出解析失败时，回退到内置的启发式计划，
保证整条链路不会因为单次模型调用失败而中断。
"""
from __future__ import annotations

import json
import logging
from typing import Any, Optional

from langchain_core.messages import HumanMessage, SystemMessage
from pydantic import BaseModel, Field

from app.agent.llm import get_llm
from app.agent.nodes._common import (
    EVENT_ERROR,
    EVENT_PLAN,
    EVENT_THINKING,
    get_extra_params,
    get_task_type,
    get_topic,
    make_event,
    truncate,
)
from app.agent.state import AgentState, PlanStep, make_plan_step
from app.agent.tools.registry import (
    get_all_tools,
    get_tool_descriptions,
    load_tools,
)

logger = logging.getLogger(__name__)

# 规划节点使用的采样温度：偏低以保证计划稳定可复现
PLANNER_TEMPERATURE = 0.2

# 单个计划的步骤数上限，防止 LLM 产出冗长无效计划
MAX_PLAN_STEPS = 8


# ---------------------------------------------------------------------------
# 结构化输出 Schema
# ---------------------------------------------------------------------------


class PlanStepOutput(BaseModel):
    """LLM 输出的单个计划步骤。"""

    id: int = Field(description="步骤序号，从 1 开始递增")
    action: str = Field(description="工具名，必须是可用工具列表中的 key")
    params: dict = Field(
        default_factory=dict,
        description="工具参数；值中可用 {{stepN.field}} 引用第 N 步结果的某个字段",
    )


class PlanOutput(BaseModel):
    """LLM 输出的完整计划。"""

    reasoning: str = Field(description="规划理由，简述为什么这样拆解任务")
    steps: list[PlanStepOutput] = Field(
        default_factory=list,
        description="按执行顺序排列的步骤列表",
    )


# ---------------------------------------------------------------------------
# Prompt
# ---------------------------------------------------------------------------

SYSTEM_PROMPT_TEMPLATE = """你是「墨西」小红书内容助手的任务规划器（Planner）。
你的唯一职责：把用户的请求拆解成一份**可直接执行**的工具调用计划，不要自己创作内容。

## 可用工具
{tool_descriptions}

## 参数变量引用
后续步骤可以使用 ``{{{{stepN.field}}}}`` 引用第 N 步返回结果中的某个字段。
例如第 1 步是 title_gen，返回 ``{{"best_title": "...", "titles": [...]}}``，
那么第 2 步的参数可以写成 ``{{"title": "{{{{step1.best_title}}}}"}}``。
只能引用**序号小于当前步骤**的已规划步骤。

## 规划原则
1. 只使用上面列出的工具名，禁止编造工具；无法完成的部分就不要安排步骤。
2. 步骤数量控制在 3~6 步，按依赖顺序排列，编号从 1 开始连续递增。
3. 生成类任务的标准骨架：选题参考（可选）→ 标题生成 → 正文生成 → 标签生成 → 人味化改写（可选）。
4. 需要引用前序结果时，务必使用变量引用而不是把示例内容写死。
5. 闲聊类请求（用户只是打招呼或提问）返回**空步骤列表**，由响应节点直接回复。
6. 输出必须严格符合给定的 JSON schema。
"""

REPLAN_HINT = """
## 上一轮执行的反思反馈（必须针对性改进）
综合评分：{score}
改进建议：
{feedback}

请重新规划：保留上一轮有效的步骤，针对上述问题调整参数或增删步骤（例如评分低是因为标题平庸，
就在标题生成时传入更明确的风格约束；若正文与标题不匹配，就把标题结果显式传给正文生成步骤）。
"""

USER_PROMPT_TEMPLATE = """## 任务信息
- 任务类型：{task_type}
- 主题：{topic}
- 博主人设：{blogger_type}
- 内容风格：{style}
- 期望字数：{word_count}
- 附加参数：{extra_params}

## 用户记忆与偏好
{memory_context}

## 爆款笔记参考（RAG 检索结果）
{rag_context}

## 用户原始输入
{user_input}

请输出执行计划。"""


def _build_messages(state: AgentState) -> list[Any]:
    """构造 planner 的对话消息。"""
    tool_descriptions = get_tool_descriptions()
    system_prompt = SYSTEM_PROMPT_TEMPLATE.format(tool_descriptions=tool_descriptions)

    iteration_count = state.get("iteration_count") or 0
    if iteration_count > 0 and state.get("quality_feedback"):
        system_prompt += REPLAN_HINT.format(
            score=f"{state.get('quality_score') or 0:.2f}",
            feedback=state["quality_feedback"],
        )

    extra_params = get_extra_params(state)
    user_prompt = USER_PROMPT_TEMPLATE.format(
        task_type=get_task_type(state),
        topic=get_topic(state) or "（未提供）",
        blogger_type=state.get("blogger_type") or "（未指定）",
        style=state.get("style") or "（未指定）",
        word_count=state.get("word_count") or 500,
        extra_params=json.dumps(extra_params, ensure_ascii=False) if extra_params else "无",
        memory_context=state.get("memory_context") or "（无历史记忆）",
        rag_context=truncate(state.get("rag_context"), 2000) or "（无参考内容）",
        user_input=truncate(state.get("topic") or "", 500) or "（无）",
    )

    return [SystemMessage(content=system_prompt), HumanMessage(content=user_prompt)]


# ---------------------------------------------------------------------------
# 计划归一化 / 兜底
# ---------------------------------------------------------------------------


def _normalize_plan(
    plan_output: PlanOutput,
    state: AgentState,
) -> list[PlanStep]:
    """把 LLM 输出转换成规范的 ``PlanStep`` 列表，并过滤未注册的工具。"""
    available = set(get_all_tools().keys())
    steps: list[PlanStep] = []

    for index, raw in enumerate(plan_output.steps[:MAX_PLAN_STEPS], start=1):
        action = (raw.action or "").strip()
        if not action:
            continue

        step_id = int(raw.id) if raw.id else index
        params = dict(raw.params or {})
        steps.append(make_plan_step(step_id, action, params, status="pending"))

    # 未注册的工具（Task 5 尚未实现时）预先标记为 failed，executor 会直接跳过
    for step in steps:
        if available and step["action"] not in available:
            step["status"] = "failed"
            step["result"] = {
                "error": f"工具 '{step['action']}' 未注册或尚未实现",
                "skipped": True,
            }
            logger.warning("plan step %s uses unavailable tool: %s", step["id"], step["action"])

    _reindex_steps(steps)
    return steps


def _reindex_steps(steps: list[PlanStep]) -> None:
    """重排步骤编号，保证从 1 开始连续，并同步修正参数中的变量引用。"""
    id_map: dict[int, int] = {}
    for new_id, step in enumerate(steps, start=1):
        old_id = int(step["id"]) if step.get("id") else new_id
        id_map[old_id] = new_id
        step["id"] = new_id

    if not id_map or all(old == new for old, new in id_map.items()):
        return

    for step in steps:
        params = step.get("params") or {}
        for key, value in params.items():
            if not isinstance(value, str) or "{{" not in value:
                continue
            for old_id, new_id in id_map.items():
                if old_id != new_id:
                    value = value.replace(f"{{{{step{old_id}.", f"{{{{step{new_id}.")
            params[key] = value


def _fallback_plan(state: AgentState) -> tuple[list[PlanStep], str]:
    """启发式兜底计划：LLM 不可用时使用。

    只编排当前**已注册**的工具，因此在 Task 5 完成前可能返回空计划。
    """
    available = set(get_all_tools().keys())
    task_type = get_task_type(state)
    topic = get_topic(state)
    style = state.get("style") or ""
    blogger_type = state.get("blogger_type") or ""
    word_count = state.get("word_count") or 500
    extra = get_extra_params(state)

    steps: list[PlanStep] = []

    if task_type == "chat":
        return [], "闲聊类请求，无需调用工具，由响应节点直接回复。"

    def add(action: str, params: dict) -> None:
        if action in available:
            steps.append(make_plan_step(len(steps) + 1, action, params))

    if task_type == "refine":
        content = extra.get("content") or extra.get("text") or ""
        add("content_gen", {"topic": topic, "style": style, "word_count": word_count,
                            "reference_content": content, "task": "refine"})
        add("humanize", {"content": "{{step1.content}}" if steps else content})
        return steps, "内容优化任务：重写正文并做人味化处理。"

    add("trend_search", {"topic": topic})
    add(
        "title_gen",
        {"topic": topic, "style": style, "blogger_type": blogger_type, "count": 5},
    )
    title_ref = "{{step%d.best_title}}" % len(steps) if steps else topic
    add(
        "content_gen",
        {"topic": topic, "title": title_ref, "style": style, "word_count": word_count},
    )
    content_ref = "{{step%d.content}}" % len(steps) if steps else ""
    add("tag_gen", {"topic": topic, "content": content_ref, "count": 8})
    add("humanize", {"content": content_ref})
    add("sensitive_check", {"content": content_ref})

    _reindex_steps(steps)
    return steps, "按标准内容生产流程编排（标题 → 正文 → 标签 → 人味化 → 敏感词校验）。"


# ---------------------------------------------------------------------------
# 节点实现
# ---------------------------------------------------------------------------


async def _plan_with_llm(state: AgentState) -> Optional[tuple[list[PlanStep], str]]:
    """调用 LLM 生成结构化计划；失败返回 ``None``。"""
    messages = _build_messages(state)
    try:
        llm = get_llm(temperature=PLANNER_TEMPERATURE)
        structured_llm = llm.with_structured_output(PlanOutput)
        output: PlanOutput = await structured_llm.ainvoke(messages)
    except Exception as exc:
        logger.warning("planner LLM call failed: %s", exc)
        return None

    if output is None:
        logger.warning("planner LLM returned empty structured output")
        return None

    steps = _normalize_plan(output, state)
    reasoning = (output.reasoning or "").strip() or "（模型未提供规划理由）"
    return steps, reasoning


async def planner_node(state: AgentState) -> dict:
    """规划节点：LLM 生成执行计划。

    Returns:
        状态增量：``plan`` / ``current_step`` / ``tool_results`` / ``stream_events``。
    """
    # 确保工具已加载（幂等；Task 5 未实现时内部会静默跳过）
    try:
        load_tools()
    except Exception as exc:  # pragma: no cover - 防御性
        logger.warning("planner: load_tools failed: %s", exc)

    iteration_count = state.get("iteration_count") or 0
    events: list[dict] = [
        make_event(
            EVENT_THINKING,
            stage="planner",
            message=(
                f"正在根据反馈重新规划（第 {iteration_count + 1} 轮）..."
                if iteration_count > 0
                else "正在分析任务，规划内容生产流程..."
            ),
        )
    ]

    planned = await _plan_with_llm(state)
    degraded = planned is None
    if planned is None:
        steps, reasoning = _fallback_plan(state)
        events.append(
            make_event(
                EVENT_ERROR,
                stage="planner",
                message="模型规划不可用，已回退到内置默认流程",
            )
        )
    else:
        steps, reasoning = planned

    events.append(
        make_event(
            EVENT_PLAN,
            reasoning=reasoning,
            steps=[
                {"id": s["id"], "action": s["action"], "params": s["params"]}
                for s in steps
            ],
            iteration=iteration_count,
            degraded=degraded,
        )
    )

    logger.info(
        "planner: task_type=%s iteration=%s steps=%s",
        get_task_type(state),
        iteration_count,
        [(s["id"], s["action"], s["status"]) for s in steps],
    )

    update: dict[str, Any] = {
        "plan": steps,
        "current_step": 0,
        "stream_events": events,
    }
    # 重新规划时清空上一轮结果，避免 executor 解析变量引用时读到过期数据
    if iteration_count > 0:
        update["tool_results"] = {}
    return update
