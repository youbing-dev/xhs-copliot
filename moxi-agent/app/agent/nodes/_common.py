"""节点公共工具：流式事件构造与状态读取助手。

``stream_events`` 中的每个事件统一形如::

    {"event": "<event_type>", "data": {...}, "ts": 1710000000.0}

事件类型约定：

- ``thinking``    : 检索/思考过程提示（retrieve、planner）
- ``plan``        : 规划结果
- ``tool_start``  : 某个工具开始执行
- ``tool_result`` : 某个工具执行结束（成功或失败）
- ``reflection``  : 质量评估结果
- ``result``      : 最终产物
- ``done``        : 流程结束
- ``error``       : 异常提示
"""
from __future__ import annotations

import time
from typing import Any, Optional

from app.agent.state import AgentState

EVENT_THINKING = "thinking"
EVENT_PLAN = "plan"
EVENT_TOOL_START = "tool_start"
EVENT_TOOL_RESULT = "tool_result"
EVENT_REFLECTION = "reflection"
EVENT_RESULT = "result"
EVENT_DONE = "done"
EVENT_ERROR = "error"


def make_event(event: str, **data: Any) -> dict:
    """构造一个流式事件。"""
    return {"event": event, "data": data, "ts": time.time()}


def make_events(event: str, **data: Any) -> list[dict]:
    """构造单元素事件列表（节点返回值中 ``stream_events`` 的常用形态）。"""
    return [make_event(event, **data)]


def truncate(text: Any, limit: int = 200) -> str:
    """将任意值截断为字符串，避免事件负载过大。"""
    if text is None:
        return ""
    value = text if isinstance(text, str) else str(text)
    return value if len(value) <= limit else value[:limit] + "..."


# ---------------------------------------------------------------------------
# 状态读取（AgentState 为 total=False，字段可能缺失）
# ---------------------------------------------------------------------------


def get_task_type(state: AgentState) -> str:
    return state.get("task_type") or "generate_note"


def get_topic(state: AgentState) -> str:
    """获取任务主题；未显式传入时回退到最后一条用户消息。"""
    topic = state.get("topic") or ""
    if topic:
        return topic

    for message in reversed(list(state.get("messages") or [])):
        content = getattr(message, "content", None)
        if content and getattr(message, "type", "") == "human":
            return str(content)
    return ""


def get_user_input(state: AgentState) -> str:
    """获取用户原始输入文本（最后一条 human 消息）。"""
    for message in reversed(list(state.get("messages") or [])):
        content = getattr(message, "content", None)
        if content and getattr(message, "type", "") == "human":
            return str(content)
    return state.get("topic") or ""


def get_extra_params(state: AgentState) -> dict:
    return state.get("extra_params") or {}


def find_result_by_action(
    state: AgentState,
    action: str,
    require_success: bool = True,
) -> Optional[dict]:
    """按计划顺序查找某个工具（``action``）最后一次的结果。

    Args:
        state: Agent 状态。
        action: 工具名，如 ``title_gen``。
        require_success: 为 True 时只返回 ``status == "done"`` 的步骤结果。

    Returns:
        工具返回的 dict；未找到时返回 ``None``。
    """
    plan = state.get("plan") or []
    tool_results = state.get("tool_results") or {}

    matched: Optional[dict] = None
    for step in plan:
        if step.get("action") != action:
            continue
        if require_success and step.get("status") != "done":
            continue
        result = tool_results.get(step.get("id"), step.get("result"))
        if isinstance(result, dict):
            matched = result
        elif result is not None:
            matched = {"value": result}
    return matched


def find_results_by_actions(
    state: AgentState,
    actions: list[str],
) -> dict[str, dict]:
    """批量查找多个工具的结果，返回 ``{action: result_dict}``（缺失的 action 不出现）。"""
    found: dict[str, dict] = {}
    for action in actions:
        result = find_result_by_action(state, action)
        if result is not None:
            found[action] = result
    return found


def has_artifacts(state: AgentState) -> bool:
    """判断本轮是否产出了实际内容（标题或正文）。

    用于区分「生成类任务」与「纯闲聊」：闲聊任务没有工具产物，
    不应进入质量评估与重新规划循环。
    """
    title_result = find_result_by_action(state, "title_gen")
    content_result = find_result_by_action(state, "content_gen")
    humanize_result = find_result_by_action(state, "humanize")

    title = first_field(title_result, ["best_title", "titles", "title_list", "value"])
    content = first_field(
        humanize_result, ["content", "humanized_content", "text", "value"]
    ) or first_field(content_result, ["content", "text", "body", "value"])
    return bool(title) or bool(content)


def first_field(result: Optional[dict], keys: list[str], default: Any = None) -> Any:
    """从工具结果中按候选字段名顺序取第一个非空值。

    工具实现（Task 5）可能采用不同的字段命名，这里做兼容处理。
    """
    if not isinstance(result, dict):
        return default
    for key in keys:
        value = result.get(key)
        if value not in (None, "", [], {}):
            return value
    return default
