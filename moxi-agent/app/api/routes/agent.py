"""Agent API 路由。

提供内容生成（流式 SSE / 同步）和对话交互端点。
"""
from __future__ import annotations

import logging
import time
from typing import AsyncGenerator

from fastapi import APIRouter, HTTPException
from langchain_core.messages import HumanMessage
from sse_starlette.sse import EventSourceResponse

from app.agent.graph import agent_graph
from app.agent.state import initial_state as make_initial_state
from app.api.schemas import (
    ChatRequest,
    GenerateRequest,
    GenerateResponse,
)
from app.memory.short_term import short_term_memory
from app.streaming.sse import agent_stream_to_sse

logger = logging.getLogger(__name__)

router = APIRouter()


# ---------------------------------------------------------------------------
# SSE 流式端点
# ---------------------------------------------------------------------------


@router.post("/generate")
async def generate_content(request: GenerateRequest):
    """内容生成端点（SSE 流式）。

    返回 SSE 事件流，实时推送 Agent 的思考过程、工具调用和生成结果。
    前端通过 EventSource 或 fetch + ReadableStream 消费此流。

    事件类型：
    - thinking: 节点执行状态提示
    - plan: 规划结果
    - tool_start / tool_result: 工具调用生命周期
    - reflection: 质量评估
    - result: 最终产物
    - done: 流程结束
    - error: 异常
    """
    # 记录用户消息到短期记忆
    await _save_user_message(request.session_id, request.topic)

    # 构造 Agent 初始状态
    state = make_initial_state(
        user_id=request.user_id,
        session_id=request.session_id,
        task_type=request.task_type or "generate_note",
        topic=request.topic,
        blogger_type=request.blogger_type,
        style=request.style,
        word_count=request.word_count,
        extra_params=request.extra_params or {},
        messages=[HumanMessage(content=request.topic)],
    )

    async def event_generator() -> AsyncGenerator[dict, None]:
        async for sse_event in agent_stream_to_sse(agent_graph, state):
            yield sse_event.to_dict()

    return EventSourceResponse(event_generator())


@router.post("/chat")
async def chat(request: ChatRequest):
    """对话式交互端点（SSE 流式）。

    支持多轮对话，Agent 会根据对话内容决定是否需要生成内容。
    纯闲聊时直接返回 LLM 回复；识别到创作意图时走完整生成流程。
    """
    # 记录用户消息到短期记忆
    await _save_user_message(request.session_id, request.message)

    # 构造 Agent 初始状态
    state = make_initial_state(
        user_id=request.user_id,
        session_id=request.session_id,
        task_type="chat",
        topic=request.message,
        blogger_type="",
        style="",
        word_count=0,
        extra_params={},
        messages=[HumanMessage(content=request.message)],
    )

    async def event_generator() -> AsyncGenerator[dict, None]:
        async for sse_event in agent_stream_to_sse(agent_graph, state):
            yield sse_event.to_dict()

    return EventSourceResponse(event_generator())


# ---------------------------------------------------------------------------
# 同步端点
# ---------------------------------------------------------------------------


@router.post("/generate/sync", response_model=GenerateResponse)
async def generate_content_sync(request: GenerateRequest):
    """同步生成端点（非流式）。

    等待 Agent 完整执行后一次性返回结果。
    适用于 Java 后端服务内部调用（无需 SSE 解析）。
    """
    start_time = time.time()

    # 记录用户消息到短期记忆
    await _save_user_message(request.session_id, request.topic)

    # 构造 Agent 初始状态
    state = make_initial_state(
        user_id=request.user_id,
        session_id=request.session_id,
        task_type=request.task_type or "generate_note",
        topic=request.topic,
        blogger_type=request.blogger_type,
        style=request.style,
        word_count=request.word_count,
        extra_params=request.extra_params or {},
        messages=[HumanMessage(content=request.topic)],
    )

    try:
        # 使用 ainvoke 同步等待完成
        result = await agent_graph.ainvoke(state)

        duration_ms = int((time.time() - start_time) * 1000)
        final_output = result.get("final_output") or {}

        # 提取标题列表，兼容多种格式
        titles_raw = final_output.get("titles", [])
        titles = _normalize_titles(titles_raw)

        return GenerateResponse(
            titles=titles,
            body=final_output.get("content", ""),
            tags=final_output.get("tags", []),
            quality_score=float(result.get("quality_score") or final_output.get("quality_score") or 0.0),
            total_tokens=final_output.get("total_tokens", 0),
            duration_ms=duration_ms,
            iterations=int(result.get("iteration_count") or final_output.get("iteration_count") or 1),
        )
    except Exception as exc:
        logger.exception("generate_content_sync: agent execution failed")
        raise HTTPException(
            status_code=500,
            detail={
                "code": "AGENT_ERROR",
                "message": str(exc),
            },
        )


# ---------------------------------------------------------------------------
# 辅助函数
# ---------------------------------------------------------------------------


async def _save_user_message(session_id: str, content: str) -> None:
    """将用户消息写入短期记忆（失败不阻断主流程）。"""
    if not session_id or not content:
        return
    try:
        await short_term_memory.add_message(
            session_id=session_id,
            role="user",
            content=content,
        )
    except Exception as exc:
        logger.warning("agent route: save user message failed: %s", exc)


def _normalize_titles(titles_raw: list) -> list[dict]:
    """将标题列表统一为 ``[{"title": "...", "score": ...}]`` 格式。"""
    result: list[dict] = []
    for item in titles_raw:
        if isinstance(item, dict):
            result.append(item)
        elif isinstance(item, str):
            result.append({"title": item, "score": 0.0})
        elif item is not None:
            result.append({"title": str(item), "score": 0.0})
    return result
