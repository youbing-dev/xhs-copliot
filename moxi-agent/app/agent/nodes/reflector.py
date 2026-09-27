"""Reflector 节点：以「小红书资深审稿人」身份评估生成质量。

评估维度（各 1~10 分）：
- 标题吸引力 ``title_score``
- 正文与标题匹配度 ``content_match_score``
- 标签相关性 ``tag_relevance_score``
- 整体可读性 ``readability_score``
- 敏感内容 ``has_sensitive_content``（布尔，命中即大幅扣分）

输出：
- ``quality_score``：0~1 归一化综合评分（供 ``quality_check`` 条件边判断是否重规划）
- ``quality_feedback``：给 planner 的可执行改进建议
- ``iteration_count``：轮次 +1

容错：LLM 不可用时使用规则式兜底评分（基于工具执行成功率与内容长度），
保证流程始终能收敛到 responder。
"""
from __future__ import annotations

import logging
from typing import Any, Optional

from langchain_core.messages import HumanMessage, SystemMessage
from pydantic import BaseModel, Field

from app.agent.llm import get_llm
from app.agent.nodes._common import (
    EVENT_REFLECTION,
    EVENT_THINKING,
    find_result_by_action,
    first_field,
    get_task_type,
    get_topic,
    has_artifacts,
    make_event,
    truncate,
)
from app.agent.state import AgentState

logger = logging.getLogger(__name__)

# 评估节点使用低温度，保证评分稳定
REFLECTOR_TEMPERATURE = 0.2

# 质量达标阈值与最大迭代次数（与 graph.quality_check 保持一致）
QUALITY_THRESHOLD = 0.7
MAX_ITERATIONS = 3


# ---------------------------------------------------------------------------
# 结构化输出 Schema
# ---------------------------------------------------------------------------


class ReflectionOutput(BaseModel):
    """LLM 审稿结果。"""

    title_score: float = Field(description="标题吸引力评分 1-10")
    content_match_score: float = Field(description="正文与标题匹配度 1-10")
    tag_relevance_score: float = Field(description="标签相关性评分 1-10")
    readability_score: float = Field(description="整体可读性评分 1-10")
    has_sensitive_content: bool = Field(description="是否包含敏感内容")
    overall_score: float = Field(description="综合评分 0-1")
    feedback: str = Field(description="改进建议")


# ---------------------------------------------------------------------------
# Prompt
# ---------------------------------------------------------------------------

SYSTEM_PROMPT = """你是小红书平台的资深内容审稿人，负责评估 AI 生成的笔记质量。
评分标准（每项 1~10 分，允许小数）：

1. **标题吸引力**：是否有钩子、是否制造好奇或利益点、是否符合小红书爆款标题特征
   （数字、悬念、身份代入、emoji 适度）。
2. **正文与标题匹配度**：正文是否兑现标题承诺，无标题党、无跑题。
3. **标签相关性**：话题标签是否与主题强相关，覆盖品类词 + 场景词 + 人群词。
4. **整体可读性**：段落节奏、口语化程度、是否像真人分享而非广告腔、排版是否适合移动端。

敏感内容判定：出现医疗功效承诺、绝对化用语（最、第一、100%）、政治敏感、
色情低俗、诱导投资、贬损特定群体等，``has_sensitive_content`` 置为 true。

综合评分 ``overall_score`` 计算方式：四项均分 / 10，若命中敏感内容则再乘以 0.5，
结果保留两位小数，范围 0~1。

``feedback`` 必须具体、可执行，直接告诉规划器应该改哪个步骤、改什么参数
（例如「标题过于平淡：请在标题生成步骤中要求使用数字型钩子并突出人群标签」）。
若质量已达标，feedback 写「质量达标，无需修改」。

只输出符合 schema 的 JSON，不要额外解释。"""

USER_PROMPT_TEMPLATE = """## 任务背景
- 主题：{topic}
- 任务类型：{task_type}
- 期望风格：{style}
- 博主人设：{blogger_type}
- 期望字数：{word_count}
- 当前迭代轮次：{iteration}

## 待评审内容
### 标题候选
{titles}

### 正文
{content}

### 标签
{tags}

### 敏感词校验工具结果
{sensitive}

## 执行过程概况
{execution_summary}

请按评分标准输出评审结果。"""


def _format_list(value: Any, limit: int = 10) -> str:
    """把标题/标签列表格式化为多行文本。"""
    if value is None:
        return "（无）"
    if isinstance(value, str):
        return truncate(value, 1000)
    if isinstance(value, (list, tuple)):
        items = [str(v).strip() for v in value if str(v).strip()][:limit]
        return "\n".join(f"{i}. {item}" for i, item in enumerate(items, 1)) or "（无）"
    return truncate(str(value), 1000)


def _extract_content(state: AgentState) -> tuple[str, str, str, str]:
    """从工具结果中提取标题、正文、标签、敏感词校验信息。"""
    title_result = find_result_by_action(state, "title_gen")
    content_result = find_result_by_action(state, "content_gen")
    tag_result = find_result_by_action(state, "tag_gen")
    humanize_result = find_result_by_action(state, "humanize")
    sensitive_result = find_result_by_action(state, "sensitive_check")

    titles = first_field(title_result, ["titles", "title_list", "candidates", "value"])
    best_title = first_field(title_result, ["best_title", "selected_title", "title"])

    # 人味化改写后的正文优先
    content = first_field(
        humanize_result, ["content", "humanized_content", "text", "value"]
    ) or first_field(content_result, ["content", "text", "body", "value"])

    tags = first_field(tag_result, ["tags", "hashtags", "topics", "value"])

    titles_text = _format_list(titles)
    if best_title and str(best_title) not in titles_text:
        titles_text = f"【推荐标题】{best_title}\n{titles_text}"

    sensitive_text = "（未执行敏感词校验）"
    if sensitive_result:
        safe = first_field(sensitive_result, ["is_safe", "safe", "passed"])
        hits = first_field(sensitive_result, ["sensitive_words", "hits", "violations"])
        sensitive_text = f"是否安全：{safe}；命中：{_format_list(hits)}"

    return titles_text, str(content or "（无正文）"), _format_list(tags), sensitive_text


def _execution_summary(state: AgentState) -> str:
    """生成执行概况文本：哪些步骤成功、哪些失败。"""
    plan = state.get("plan") or []
    if not plan:
        return "（无执行计划）"

    lines: list[str] = []
    for step in plan:
        status = step.get("status", "pending")
        mark = {"done": "✓", "failed": "✗", "running": "…", "pending": "○"}.get(status, "?")
        detail = ""
        result = step.get("result")
        if status == "failed" and isinstance(result, dict):
            detail = f" — {result.get('error', '')}"
        lines.append(f"{mark} step{step.get('id')} {step.get('action')}{detail}")
    return "\n".join(lines)


def _build_messages(state: AgentState, iteration: int) -> list[Any]:
    titles, content, tags, sensitive = _extract_content(state)
    user_prompt = USER_PROMPT_TEMPLATE.format(
        topic=get_topic(state) or "（未提供）",
        task_type=get_task_type(state),
        style=state.get("style") or "（未指定）",
        blogger_type=state.get("blogger_type") or "（未指定）",
        word_count=state.get("word_count") or 500,
        iteration=iteration + 1,
        titles=titles,
        content=truncate(content, 4000),
        tags=tags,
        sensitive=sensitive,
        execution_summary=_execution_summary(state),
    )
    return [SystemMessage(content=SYSTEM_PROMPT), HumanMessage(content=user_prompt)]


# ---------------------------------------------------------------------------
# 评分归一化 / 兜底
# ---------------------------------------------------------------------------


def _clamp_score(value: Any, low: float = 1.0, high: float = 10.0) -> float:
    """把评分裁剪到 [low, high]，非法值回落到中位数。"""
    try:
        score = float(value)
    except (TypeError, ValueError):
        return (low + high) / 2
    if score != score:  # NaN
        return (low + high) / 2
    return max(low, min(high, score))


def compute_overall_score(
    title_score: float,
    content_match_score: float,
    tag_relevance_score: float,
    readability_score: float,
    has_sensitive_content: bool,
) -> float:
    """按约定公式计算 0~1 的综合评分。"""
    average = (
        title_score + content_match_score + tag_relevance_score + readability_score
    ) / 4.0
    overall = average / 10.0
    if has_sensitive_content:
        overall *= 0.5
    return round(max(0.0, min(1.0, overall)), 4)


def _heuristic_reflection(state: AgentState) -> tuple[float, str, dict]:
    """规则式兜底评估：LLM 不可用时使用。

    依据工具执行成功率 + 正文长度 + 标题/标签是否齐全给出粗略评分。
    """
    plan = state.get("plan") or []
    titles, content, tags, _ = _extract_content(state)

    total = len(plan)
    done = sum(1 for s in plan if s.get("status") == "done")
    success_ratio = (done / total) if total else 0.0

    score = 0.4 * success_ratio
    details: list[str] = []

    if content and content != "（无正文）":
        length = len(content)
        word_count = state.get("word_count") or 500
        if length >= word_count * 0.6:
            score += 0.35
        else:
            score += 0.15
            details.append(f"正文偏短（{length} 字，期望约 {word_count} 字）")
    else:
        details.append("未生成正文内容")

    if titles and titles != "（无）":
        score += 0.15
    else:
        details.append("未生成标题候选")

    if tags and tags != "（无）":
        score += 0.10
    else:
        details.append("未生成话题标签")

    score = round(max(0.0, min(1.0, score)), 4)

    failed = [
        f"step{s.get('id')}({s.get('action')})"
        for s in plan
        if s.get("status") == "failed"
    ]
    if failed:
        details.insert(0, f"以下步骤执行失败：{', '.join(failed)}")
    if not details:
        details.append("各步骤执行正常，建议模型复核内容质量")

    feedback = (
        "【规则式兜底评估（模型评审不可用）】" + "；".join(details)
        + "。请重新规划时优先修复失败步骤，并显式传递标题给正文生成步骤。"
    )
    return score, feedback, {"eval_mode": "heuristic", "success_ratio": success_ratio}


async def _reflect_with_llm(
    state: AgentState, iteration: int
) -> Optional[tuple[ReflectionOutput, float]]:
    """调用 LLM 评审；失败返回 ``None``。"""
    try:
        llm = get_llm(temperature=REFLECTOR_TEMPERATURE)
        structured_llm = llm.with_structured_output(ReflectionOutput)
        output: ReflectionOutput = await structured_llm.ainvoke(_build_messages(state, iteration))
    except Exception as exc:
        logger.warning("reflector LLM call failed: %s", exc)
        return None

    if output is None:
        logger.warning("reflector LLM returned empty structured output")
        return None

    title_score = _clamp_score(output.title_score)
    match_score = _clamp_score(output.content_match_score)
    tag_score = _clamp_score(output.tag_relevance_score)
    read_score = _clamp_score(output.readability_score)

    # 以本地公式为准重算综合分，避免模型给出的 overall_score 与各维度不一致
    overall = compute_overall_score(
        title_score,
        match_score,
        tag_score,
        read_score,
        bool(output.has_sensitive_content),
    )

    normalized = ReflectionOutput(
        title_score=title_score,
        content_match_score=match_score,
        tag_relevance_score=tag_score,
        readability_score=read_score,
        has_sensitive_content=bool(output.has_sensitive_content),
        overall_score=overall,
        feedback=(output.feedback or "").strip() or "未提供改进建议",
    )
    return normalized, overall


# ---------------------------------------------------------------------------
# 节点实现
# ---------------------------------------------------------------------------


async def reflector_node(state: AgentState) -> dict:
    """反思节点：评估生成质量。

    Returns:
        状态增量：``quality_score`` / ``quality_feedback`` / ``iteration_count``
        / ``stream_events``。
    """
    iteration = state.get("iteration_count") or 0
    task_type = get_task_type(state)

    # 闲聊类任务没有内容产物，跳过质量评估，直接放行到 responder，
    # 否则会陷入「评分低 → 重新规划 → 仍然没有产物」的无意义循环。
    if task_type == "chat" and not has_artifacts(state):
        feedback = "闲聊类请求，无需内容质量评估"
        logger.info("reflector: skip evaluation for chat task")
        return {
            "quality_score": 1.0,
            "quality_feedback": feedback,
            "iteration_count": iteration + 1,
            "stream_events": [
                make_event(
                    EVENT_REFLECTION,
                    stage="reflector",
                    quality_score=1.0,
                    scores={},
                    feedback=feedback,
                    iteration=iteration + 1,
                    threshold=QUALITY_THRESHOLD,
                    max_iterations=MAX_ITERATIONS,
                    passed=True,
                    mode="skipped",
                )
            ],
        }

    events: list[dict] = [
        make_event(
            EVENT_THINKING,
            stage="reflector",
            message="正在以审稿人视角评估内容质量...",
        )
    ]

    llm_result = await _reflect_with_llm(state, iteration)

    if llm_result is None:
        score, feedback, extra = _heuristic_reflection(state)
        scores: dict[str, Any] = {}
        mode = "heuristic"
    else:
        reflection, score = llm_result
        feedback = reflection.feedback
        mode = "llm"
        extra = {}
        scores = {
            "title_score": reflection.title_score,
            "content_match_score": reflection.content_match_score,
            "tag_relevance_score": reflection.tag_relevance_score,
            "readability_score": reflection.readability_score,
            "has_sensitive_content": reflection.has_sensitive_content,
        }

    next_iteration = iteration + 1
    passed = score >= QUALITY_THRESHOLD or next_iteration >= MAX_ITERATIONS

    events.append(
        make_event(
            EVENT_REFLECTION,
            stage="reflector",
            quality_score=round(score, 4),
            scores=scores,
            feedback=truncate(feedback, 800),
            iteration=next_iteration,
            threshold=QUALITY_THRESHOLD,
            max_iterations=MAX_ITERATIONS,
            passed=passed,
            mode=mode,
            **extra,
        )
    )

    logger.info(
        "reflector: iteration=%s score=%.3f passed=%s mode=%s",
        next_iteration,
        score,
        passed,
        mode,
    )

    return {
        "quality_score": round(score, 4),
        "quality_feedback": feedback,
        "iteration_count": next_iteration,
        "stream_events": events,
    }
