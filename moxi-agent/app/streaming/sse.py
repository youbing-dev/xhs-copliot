"""SSE (Server-Sent Events) 事件流管理。

将 LangGraph 的执行过程转换为前端可消费的 SSE 事件流。

协议格式（sse-starlette 约定）::

    event: <event_type>
    data: <json_string>

事件类型：
- thinking      : 节点开始执行的思考状态提示
- plan          : 规划结果
- tool_start    : 工具开始执行
- tool_result   : 工具执行完成
- content_chunk : 内容增量片段（预留）
- reflection    : 质量评估结果
- result        : 最终产物
- done          : 流程结束
- error         : 异常
"""
from __future__ import annotations

import json
import logging
import time
from dataclasses import dataclass, field
from typing import Any, AsyncGenerator, Optional

logger = logging.getLogger(__name__)

# Agent 图中的有效节点名集合
_AGENT_NODES = frozenset({"retrieve", "planner", "executor", "reflector", "responder"})


@dataclass
class SSEEvent:
    """SSE 事件封装。"""

    event: str  # 事件类型
    data: dict = field(default_factory=dict)  # 事件数据

    def to_dict(self) -> dict:
        """转换为 sse-starlette EventSourceResponse 所需的字典格式。"""
        return {
            "event": self.event,
            "data": json.dumps(self.data, ensure_ascii=False, default=str),
        }


class EventType:
    """SSE 事件类型常量。"""

    THINKING = "thinking"
    PLAN = "plan"
    TOOL_START = "tool_start"
    TOOL_RESULT = "tool_result"
    CONTENT_CHUNK = "content_chunk"
    REFLECTION = "reflection"
    RESULT = "result"
    DONE = "done"
    ERROR = "error"


def format_sse_event(event_type: str, data: dict) -> SSEEvent:
    """创建 SSE 事件。"""
    return SSEEvent(event=event_type, data=data)


def _get_thinking_message(node_name: str) -> str:
    """获取节点对应的思考状态描述。"""
    messages = {
        "retrieve": "正在检索相关知识和历史记忆...",
        "planner": "正在分析需求并制定执行计划...",
        "executor": "正在执行工具调用...",
        "reflector": "正在评估生成质量...",
        "responder": "正在组装最终结果...",
    }
    return messages.get(node_name, f"正在处理 {node_name}...")


async def agent_stream_to_sse(
    graph: Any,
    initial_state: dict,
    config: Optional[dict] = None,
) -> AsyncGenerator[SSEEvent, None]:
    """将 LangGraph agent 执行过程转换为 SSE 事件流。

    使用 ``astream`` API 逐节点获取执行输出，提取每个节点产生的
    ``stream_events`` 并映射为前端可消费的 SSE 协议事件。

    Args:
        graph: 编译好的 LangGraph CompiledGraph 实例。
        initial_state: Agent 初始状态字典。
        config: LangGraph 运行配置（可选）。

    Yields:
        SSEEvent 实例，按执行顺序产出。
    """
    start_time = time.time()
    done_emitted = False

    try:
        # 使用 astream 获取每个节点执行后的 state 增量
        # chunk 格式: {node_name: node_output_dict}
        async for chunk in graph.astream(initial_state, config=config or {}):
            for node_name, node_output in chunk.items():
                # 仅处理 Agent 图中的已知节点
                if node_name not in _AGENT_NODES:
                    continue

                # 发送 thinking 事件，告知前端当前执行阶段
                yield format_sse_event(EventType.THINKING, {
                    "step": node_name,
                    "content": _get_thinking_message(node_name),
                })

                # 提取节点产出的 stream_events
                if not isinstance(node_output, dict):
                    continue

                stream_events = node_output.get("stream_events")
                if not stream_events or not isinstance(stream_events, list):
                    continue

                for se in stream_events:
                    if not isinstance(se, dict):
                        continue
                    event_type = se.get("event", EventType.THINKING)
                    event_data = se.get("data", {})

                    # 确保 event_data 是 dict
                    if not isinstance(event_data, dict):
                        event_data = {"value": event_data}

                    yield format_sse_event(event_type, event_data)

                    # 跟踪是否已经由节点发出了 done 事件
                    if event_type == EventType.DONE:
                        done_emitted = True

        # 如果节点未发出 done 事件，补充一个
        duration_ms = int((time.time() - start_time) * 1000)
        if not done_emitted:
            yield format_sse_event(EventType.DONE, {
                "duration_ms": duration_ms,
                "status": "completed",
            })

    except Exception as exc:
        logger.exception("agent_stream_to_sse: execution error")
        duration_ms = int((time.time() - start_time) * 1000)
        yield format_sse_event(EventType.ERROR, {
            "code": "AGENT_EXECUTION_ERROR",
            "message": str(exc),
            "duration_ms": duration_ms,
        })


async def agent_stream_to_sse_events(
    graph: Any,
    initial_state: dict,
    config: Optional[dict] = None,
) -> AsyncGenerator[SSEEvent, None]:
    """使用 astream_events v2 API 的备选实现。

    提供更细粒度的事件（节点开始/结束），适用于需要实时推送
    节点启动状态的场景。当 ``agent_stream_to_sse`` 不满足需求时可切换到此实现。

    Args:
        graph: 编译好的 LangGraph CompiledGraph 实例。
        initial_state: Agent 初始状态字典。
        config: LangGraph 运行配置（可选）。

    Yields:
        SSEEvent 实例。
    """
    start_time = time.time()
    done_emitted = False

    try:
        async for event in graph.astream_events(
            initial_state, config=config or {}, version="v2"
        ):
            kind = event.get("event", "")

            # 节点开始执行
            if kind == "on_chain_start":
                node_name = event.get("name", "")
                if node_name in _AGENT_NODES:
                    yield format_sse_event(EventType.THINKING, {
                        "step": node_name,
                        "content": _get_thinking_message(node_name),
                    })

            # 节点执行完成 - 提取 stream_events
            elif kind == "on_chain_end":
                node_name = event.get("name", "")
                if node_name not in _AGENT_NODES:
                    continue

                output = event.get("data", {}).get("output", {})
                if not isinstance(output, dict):
                    continue

                stream_events = output.get("stream_events")
                if not stream_events or not isinstance(stream_events, list):
                    continue

                for se in stream_events:
                    if not isinstance(se, dict):
                        continue
                    event_type = se.get("event", EventType.THINKING)
                    event_data = se.get("data", {})

                    if not isinstance(event_data, dict):
                        event_data = {"value": event_data}

                    yield format_sse_event(event_type, event_data)

                    if event_type == EventType.DONE:
                        done_emitted = True

        # 补充 done 事件
        duration_ms = int((time.time() - start_time) * 1000)
        if not done_emitted:
            yield format_sse_event(EventType.DONE, {
                "duration_ms": duration_ms,
                "status": "completed",
            })

    except Exception as exc:
        logger.exception("agent_stream_to_sse_events: execution error")
        duration_ms = int((time.time() - start_time) * 1000)
        yield format_sse_event(EventType.ERROR, {
            "code": "AGENT_EXECUTION_ERROR",
            "message": str(exc),
            "duration_ms": duration_ms,
        })
