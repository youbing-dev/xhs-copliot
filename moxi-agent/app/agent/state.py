"""LangGraph Agent 状态定义

``AgentState`` 是贯穿整张状态图（retrieve → planner → executor → reflector → responder）
的唯一数据载体。每个节点函数只返回需要更新的字段（partial dict），
LangGraph 会按照 reducer 规则自动 merge：

- ``messages``      : 使用 ``add_messages`` reducer，追加而非覆盖
- ``stream_events`` : 使用 ``operator.add`` reducer，各节点产生的事件按顺序累积
- 其余字段          : 默认覆盖语义（后写覆盖先写）
"""
from __future__ import annotations

import operator
from typing import Annotated, Any, Literal, Optional, Sequence, TypedDict

from langchain_core.messages import BaseMessage
from langgraph.graph.message import add_messages

# ---------------------------------------------------------------------------
# 计划步骤
# ---------------------------------------------------------------------------

StepStatus = Literal["pending", "running", "done", "failed"]


class PlanStep(TypedDict):
    """执行计划中的单步。

    Attributes:
        id: 步骤序号（从 1 开始），也是 ``tool_results`` 的 key。
        action: 工具名，对应 ``app/agent/tools/registry.py`` 中注册的 key，
            如 ``title_gen`` / ``content_gen`` / ``tag_gen`` / ``humanize``
            / ``trend_search`` / ``sensitive_check``。
        params: 工具入参。字符串值中可包含 ``{{stepN.field}}`` 形式的变量引用，
            由 executor 在执行前解析为上一步的实际结果。
        status: 步骤状态，``pending`` → ``running`` → ``done`` / ``failed``。
        result: 执行结果（成功为工具返回值，失败为 ``{"error": ...}``）。
    """

    id: int
    action: str
    params: dict
    status: StepStatus
    result: Optional[Any]


# ---------------------------------------------------------------------------
# Agent 状态
# ---------------------------------------------------------------------------


class AgentState(TypedDict, total=False):
    """LangGraph Agent 的完整状态。

    使用 ``total=False``：入口只需提供少量字段（user_id / session_id / topic 等），
    其余字段由各节点在执行过程中逐步填充。
    """

    # ----------------------- 消息历史 -----------------------
    messages: Annotated[Sequence[BaseMessage], add_messages]

    # ----------------------- 用户与会话 -----------------------
    user_id: str
    session_id: str
    task_type: str  # "generate_note" | "chat" | "refine"

    # ----------------------- 用户输入参数 -----------------------
    topic: str
    blogger_type: str
    style: str
    word_count: int
    extra_params: Optional[dict]

    # ----------------------- 规划 -----------------------
    plan: Optional[list[PlanStep]]
    current_step: int  # 指向 plan 中"下一个待执行步骤"的下标（0-based）

    # ----------------------- 执行结果 -----------------------
    tool_results: dict  # {step_id: result_dict}

    # ----------------------- 上下文（retrieve 节点填充） -----------------------
    memory_context: str  # 短期对话历史 + 长期用户偏好
    rag_context: str  # 爆款笔记 RAG 检索结果

    # ----------------------- 反思 -----------------------
    quality_score: float  # 0-1 归一化综合评分
    quality_feedback: str  # 反思给出的改进建议
    iteration_count: int  # 已完成的"规划-执行-反思"轮次

    # ----------------------- 输出 -----------------------
    final_output: Optional[dict]

    # ----------------------- 流式事件 -----------------------
    stream_events: Annotated[list[dict], operator.add]


# ---------------------------------------------------------------------------
# 辅助函数
# ---------------------------------------------------------------------------

def default_state() -> dict[str, Any]:
    """返回一份全新的默认状态字典。

    以函数而非常量提供，避免 ``list`` / ``dict`` 默认值在多个请求之间被共享。
    """
    return {
        "messages": [],
        "user_id": "",
        "session_id": "",
        "task_type": "generate_note",
        "topic": "",
        "blogger_type": "",
        "style": "",
        "word_count": 500,
        "extra_params": None,
        "plan": None,
        "current_step": 0,
        "tool_results": {},
        "memory_context": "",
        "rag_context": "",
        "quality_score": 0.0,
        "quality_feedback": "",
        "iteration_count": 0,
        "final_output": None,
        "stream_events": [],
    }


def make_plan_step(
    step_id: int,
    action: str,
    params: Optional[dict] = None,
    status: StepStatus = "pending",
    result: Optional[Any] = None,
) -> PlanStep:
    """构造一个规范的 ``PlanStep``（补齐缺省字段）。"""
    return PlanStep(
        id=step_id,
        action=action,
        params=params or {},
        status=status,
        result=result,
    )


def initial_state(**overrides: Any) -> AgentState:
    """构造带默认值的初始状态，供 API 层直接调用 ``agent_graph.ainvoke`` 使用。"""
    state: dict[str, Any] = default_state()
    state.update(overrides)
    return state  # type: ignore[return-value]


def get_state_value(state: AgentState, key: str, default: Any = None) -> Any:
    """安全读取状态字段（``AgentState`` 为 ``total=False``，字段可能缺失或为 None）。"""
    value = state.get(key, default)  # type: ignore[call-overload]
    return default if value is None else value
