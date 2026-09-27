"""Retrieve 节点：会话记忆与 RAG 上下文检索。

作为状态图的入口节点，负责把两类上下文写回状态：

- ``memory_context``：短期对话历史（Redis）+ 长期用户偏好（Chroma）
- ``rag_context``   ：爆款笔记知识库的语义检索结果

所有外部依赖都做了降级处理：Redis / 向量库不可用时只记录日志、返回空上下文，
不会中断 Agent 主流程。
"""
from __future__ import annotations

import asyncio
import logging
from typing import Any, Optional

from app.agent.nodes._common import (
    EVENT_ERROR,
    EVENT_THINKING,
    get_extra_params,
    get_task_type,
    get_topic,
    get_user_input,
    make_event,
)
from app.agent.state import AgentState
from app.memory.long_term import long_term_memory
from app.memory.short_term import short_term_memory
from app.rag.retriever import format_context, retrieve_notes

logger = logging.getLogger(__name__)

# RAG 召回数量
DEFAULT_TOP_K = 5

# 检索超时（秒）：外部依赖卡住时快速降级
RETRIEVE_TIMEOUT = 15.0

# 闲聊类任务不做向量检索，避免无意义开销
_SKIP_RAG_TASK_TYPES = {"chat"}


async def _safe_await(coro, label: str, default: Any) -> Any:
    """带超时与异常兜底地执行协程。"""
    try:
        return await asyncio.wait_for(coro, timeout=RETRIEVE_TIMEOUT)
    except asyncio.TimeoutError:
        logger.warning("retrieve: %s timed out after %.1fs", label, RETRIEVE_TIMEOUT)
    except Exception as exc:
        logger.warning("retrieve: %s failed: %s", label, exc)
    return default


def _build_memory_context(short_context: str, long_context: str) -> str:
    """合并短期与长期记忆为单段上下文文本。"""
    sections: list[str] = []
    if short_context:
        sections.append(f"【最近对话】\n{short_context}")
    if long_context:
        sections.append(f"【用户长期偏好与历史反馈】\n{long_context}")
    return "\n\n".join(sections)


def _rag_query(state: AgentState) -> str:
    """构造 RAG 检索 query：主题 + 风格 + 人设，提升召回相关性。"""
    parts = [get_topic(state) or get_user_input(state)]
    if state.get("style"):
        parts.append(str(state["style"]))
    if state.get("blogger_type"):
        parts.append(str(state["blogger_type"]))
    return " ".join(p for p in parts if p).strip()


def _rag_filters(state: AgentState) -> Optional[dict]:
    """从附加参数中提取向量库 metadata 过滤条件。"""
    extra = get_extra_params(state)
    filters: dict[str, Any] = {}
    for key in ("category", "blogger_type", "tag"):
        value = extra.get(key)
        if isinstance(value, str) and value.strip():
            filters[key] = value.strip()
    return filters or None


async def retrieve_node(state: AgentState) -> dict:
    """检索节点：加载记忆与参考内容。

    Returns:
        状态增量：``memory_context`` / ``rag_context`` / ``stream_events``。
    """
    session_id = state.get("session_id") or ""
    user_id = state.get("user_id") or ""
    task_type = get_task_type(state)
    topic = get_topic(state)
    query = _rag_query(state)

    events: list[dict] = [
        make_event(
            EVENT_THINKING,
            stage="retrieve",
            message="正在检索对话记忆与爆款笔记参考...",
        )
    ]

    # ------------------------- 短期 + 长期记忆（并发） -------------------------
    short_coro = (
        short_term_memory.get_context_string(session_id)
        if session_id
        else asyncio.sleep(0, result="")
    )
    long_coro = (
        long_term_memory.get_user_preference_context(user_id, query or topic)
        if user_id
        else asyncio.sleep(0, result="")
    )

    short_context, long_context = await asyncio.gather(
        _safe_await(short_coro, "short_term_memory", ""),
        _safe_await(long_coro, "long_term_memory", ""),
    )
    memory_context = _build_memory_context(short_context or "", long_context or "")

    # ------------------------- RAG 检索 -------------------------
    rag_context = ""
    doc_count = 0
    if task_type in _SKIP_RAG_TASK_TYPES:
        rag_context = "（闲聊类请求，未检索参考笔记）"
    elif not query:
        rag_context = "（未提供主题，无法检索参考笔记）"
        events.append(
            make_event(
                EVENT_ERROR,
                stage="retrieve",
                message="缺少检索主题，已跳过 RAG 检索",
            )
        )
    else:
        documents = await _safe_await(
            retrieve_notes(query, top_k=DEFAULT_TOP_K, filters=_rag_filters(state)),
            "retrieve_notes",
            [],
        )
        doc_count = len(documents or [])
        try:
            rag_context = format_context(documents) if documents else "暂无相关参考内容"
        except Exception as exc:  # pragma: no cover - 防御性
            logger.warning("retrieve: format_context failed: %s", exc)
            rag_context = "暂无相关参考内容"

    events.append(
        make_event(
            EVENT_THINKING,
            stage="retrieve",
            message=(
                f"已加载记忆上下文 {len(memory_context)} 字，"
                f"召回参考笔记 {doc_count} 篇"
            ),
            memory_chars=len(memory_context),
            rag_docs=doc_count,
            has_short_memory=bool(short_context),
            has_long_memory=bool(long_context),
        )
    )

    logger.info(
        "retrieve: session=%s user=%s memory_chars=%d rag_docs=%d",
        session_id or "-",
        user_id or "-",
        len(memory_context),
        doc_count,
    )

    return {
        "memory_context": memory_context,
        "rag_context": rag_context,
        "stream_events": events,
    }
