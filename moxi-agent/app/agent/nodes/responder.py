"""Responder 节点：组装最终输出并落库记忆。

职责：
1. 从 ``tool_results`` 中抽取标题、正文、标签、敏感词校验结果，拼装 ``final_output``；
2. 若存在 ``humanize`` 步骤的结果，优先使用改写后的正文；
3. 闲聊类任务（无工具产物）直接用 LLM 生成回复，失败时回退到提示文案；
4. 写入长期记忆（``save_generation_result``，内部按评分阈值过滤）
   与短期对话记忆（``short_term_memory.add_message``）；
5. 产出 ``result`` 与 ``done`` 事件，并追加一条 ``AIMessage`` 到消息历史。
"""
from __future__ import annotations

import json
import logging
from typing import Any, Optional

from langchain_core.messages import AIMessage, HumanMessage, SystemMessage

from app.agent.llm import get_llm
from app.agent.nodes._common import (
    EVENT_DONE,
    EVENT_ERROR,
    EVENT_RESULT,
    EVENT_THINKING,
    find_result_by_action,
    first_field,
    get_task_type,
    get_topic,
    get_user_input,
    make_event,
    truncate,
)
from app.agent.state import AgentState
from app.memory.long_term import long_term_memory
from app.memory.short_term import short_term_memory

logger = logging.getLogger(__name__)

# 闲聊回复的采样温度
CHAT_TEMPERATURE = 0.8

# 摘要长度上限（写入长期记忆用）
SUMMARY_LIMIT = 200


CHAT_SYSTEM_PROMPT = """你是「墨西」，一个专注小红书内容创作的 AI 助手。
你熟悉平台调性、爆款笔记结构和话题标签玩法，说话亲切、专业、不啰嗦。
用户此刻没有要求生成完整笔记，请直接用自然语言回答或引导他说出创作需求
（主题、博主人设、风格、字数）。回答控制在 200 字以内，不要输出 Markdown 标题。"""


# ---------------------------------------------------------------------------
# 结果抽取
# ---------------------------------------------------------------------------


def _as_list(value: Any) -> list[str]:
    """把标签 / 标题候选统一成字符串列表。"""
    if value is None:
        return []
    if isinstance(value, str):
        text = value.strip()
        if not text:
            return []
        # 兼容 "#标签 #标签" 与 "标签, 标签" 两种写法
        if text.startswith("#"):
            parts = [p for p in text.replace("\n", " ").split("#") if p.strip()]
        else:
            parts = [p for p in text.replace("，", ",").split(",") if p.strip()]
        return [p.strip() for p in parts] or [text]
    if isinstance(value, (list, tuple)):
        items: list[str] = []
        for item in value:
            if isinstance(item, dict):
                text = first_field(item, ["tag", "title", "name", "text", "value"])
                if text:
                    items.append(str(text).strip())
            elif item is not None and str(item).strip():
                items.append(str(item).strip())
        return items
    return [str(value)]


def _extract_final_content(state: AgentState) -> dict[str, Any]:
    """从工具结果中抽取标题 / 正文 / 标签 / 敏感校验。"""
    title_result = find_result_by_action(state, "title_gen")
    content_result = find_result_by_action(state, "content_gen")
    tag_result = find_result_by_action(state, "tag_gen")
    humanize_result = find_result_by_action(state, "humanize")
    sensitive_result = find_result_by_action(state, "sensitive_check")

    titles = _as_list(
        first_field(title_result, ["titles", "title_list", "candidates", "value", "text"])
    )
    best_title = first_field(title_result, ["best_title", "selected_title", "title"])
    if not best_title and titles:
        best_title = titles[0]
    best_title = str(best_title).strip() if best_title else ""

    if best_title and best_title not in titles:
        titles.insert(0, best_title)

    # 人味化改写优先
    content = first_field(
        humanize_result, ["content", "humanized_content", "text", "value"]
    ) or first_field(content_result, ["content", "text", "body", "value"])
    content = str(content).strip() if content else ""
    humanized = bool(content) and bool(humanize_result)

    tags = _as_list(first_field(tag_result, ["tags", "hashtags", "topics", "value"]))

    sensitive: Optional[dict] = None
    if sensitive_result:
        sensitive = {
            "is_safe": bool(
                first_field(sensitive_result, ["is_safe", "safe", "passed"], True)
            ),
            "sensitive_words": _as_list(
                first_field(sensitive_result, ["sensitive_words", "hits", "violations"])
            ),
        }

    return {
        "titles": titles,
        "best_title": best_title,
        "content": content,
        "humanized": humanized,
        "tags": tags,
        "sensitive_check": sensitive,
    }


def _plan_summary(state: AgentState) -> list[dict]:
    """输出执行轨迹，便于前端展示与问题排查。"""
    summary: list[dict] = []
    for step in state.get("plan") or []:
        result = step.get("result")
        item: dict[str, Any] = {
            "id": step.get("id"),
            "action": step.get("action"),
            "status": step.get("status", "pending"),
        }
        if step.get("status") == "failed" and isinstance(result, dict):
            item["error"] = result.get("error", "")
        summary.append(item)
    return summary


# ---------------------------------------------------------------------------
# 闲聊回复
# ---------------------------------------------------------------------------


async def _generate_chat_reply(state: AgentState) -> tuple[str, bool]:
    """为闲聊类任务生成回复。

    Returns:
        ``(回复文本, 是否由 LLM 生成)``。
    """
    user_input = get_user_input(state) or get_topic(state) or "你好"
    memory_context = state.get("memory_context") or ""

    messages: list[Any] = [SystemMessage(content=CHAT_SYSTEM_PROMPT)]
    if memory_context:
        messages.append(
            SystemMessage(content=f"以下是与用户的历史对话和偏好，供参考：\n{memory_context}")
        )
    messages.append(HumanMessage(content=user_input))

    try:
        llm = get_llm(temperature=CHAT_TEMPERATURE)
        reply = await llm.ainvoke(messages)
        text = str(getattr(reply, "content", "") or "").strip()
        if text:
            return text, True
    except Exception as exc:
        logger.warning("responder: chat LLM call failed: %s", exc)

    return (
        "抱歉，我暂时没能理解你的需求。你可以直接告诉我想写的主题"
        "（例如「秋季通勤穿搭」），以及博主人设、风格和期望字数，我来帮你生成小红书笔记。",
        False,
    )


# ---------------------------------------------------------------------------
# 记忆写入
# ---------------------------------------------------------------------------


async def _persist_memory(state: AgentState, final_output: dict) -> None:
    """写入长期记忆与短期对话记忆（失败只记录日志，不影响返回）。"""
    user_id = state.get("user_id") or ""
    session_id = state.get("session_id") or ""
    topic = get_topic(state)
    title = final_output.get("title", "")
    content = final_output.get("content", "")
    quality_score = float(final_output.get("quality_score") or 0.0)

    # 长期记忆：save_generation_result 内部按评分阈值（>0.8）过滤
    if user_id and (title or content):
        try:
            await long_term_memory.save_generation_result(
                user_id=user_id,
                topic=topic,
                title=title,
                quality_score=quality_score,
                summary=truncate(content, SUMMARY_LIMIT),
            )
        except Exception as exc:
            logger.warning("responder: save_generation_result failed: %s", exc)

    # 短期记忆：记录本轮产出，供后续多轮对话引用
    if session_id:
        summary_text = title or truncate(content, 100) or "（本轮未产出内容）"
        try:
            await short_term_memory.add_message(
                session_id=session_id,
                role="assistant",
                content=f"主题「{topic}」的内容已生成：{truncate(summary_text, 160)}",
                metadata={
                    "task_type": get_task_type(state),
                    "quality_score": quality_score,
                    "iteration_count": state.get("iteration_count") or 0,
                },
            )
        except Exception as exc:
            logger.warning("responder: add_message(assistant) failed: %s", exc)


# ---------------------------------------------------------------------------
# 节点实现
# ---------------------------------------------------------------------------


async def responder_node(state: AgentState) -> dict:
    """响应节点：组装最终输出。

    Returns:
        状态增量：``final_output`` / ``messages`` / ``stream_events``。
    """
    task_type = get_task_type(state)
    topic = get_topic(state)
    events: list[dict] = []

    extracted = _extract_final_content(state)
    has_artifact = bool(extracted["content"] or extracted["titles"])

    if task_type == "chat" and not has_artifact:
        events.append(
            make_event(EVENT_THINKING, stage="responder", message="正在组织回复...")
        )
        reply, from_llm = await _generate_chat_reply(state)
        if not from_llm:
            events.append(
                make_event(
                    EVENT_ERROR,
                    stage="responder",
                    message="模型回复不可用，已返回引导文案",
                )
            )
        final_output: dict[str, Any] = {
            "task_type": task_type,
            "topic": topic,
            "title": "",
            "titles": [],
            "content": reply,
            "tags": [],
            "word_count": len(reply),
            "humanized": False,
            "sensitive_check": None,
            "quality_score": state.get("quality_score") or 0.0,
            "quality_feedback": state.get("quality_feedback") or "",
            "iteration_count": state.get("iteration_count") or 0,
            "steps": _plan_summary(state),
        }
    else:
        content = extracted["content"]
        if not content:
            events.append(
                make_event(
                    EVENT_ERROR,
                    stage="responder",
                    message="未能生成正文内容，请检查工具执行结果",
                )
            )
        final_output = {
            "task_type": task_type,
            "topic": topic,
            "title": extracted["best_title"],
            "titles": extracted["titles"],
            "content": content,
            "tags": extracted["tags"],
            "word_count": len(content),
            "humanized": extracted["humanized"],
            "sensitive_check": extracted["sensitive_check"],
            "quality_score": state.get("quality_score") or 0.0,
            "quality_feedback": state.get("quality_feedback") or "",
            "iteration_count": state.get("iteration_count") or 0,
            "steps": _plan_summary(state),
        }

    # 记忆落库（异步、失败降级）
    await _persist_memory(state, final_output)

    # 消息历史：追加一条 AIMessage，便于多轮对话续接
    message_text = final_output.get("content") or final_output.get("title") or ""
    if not message_text:
        message_text = "本次未能生成有效内容，建议调整主题后重试。"

    events.append(
        make_event(
            EVENT_RESULT,
            stage="responder",
            title=final_output.get("title", ""),
            titles=final_output.get("titles", []),
            content=truncate(message_text, 4000),
            tags=final_output.get("tags", []),
            quality_score=final_output.get("quality_score"),
            iteration_count=final_output.get("iteration_count"),
            output=final_output,
        )
    )
    events.append(
        make_event(
            EVENT_DONE,
            stage="responder",
            message="内容生成完成",
            quality_score=final_output.get("quality_score"),
            iterations=final_output.get("iteration_count"),
        )
    )

    logger.info(
        "responder: task_type=%s title=%s word_count=%s score=%.3f",
        task_type,
        truncate(final_output.get("title", ""), 40),
        final_output.get("word_count"),
        float(final_output.get("quality_score") or 0.0),
    )

    return {
        "final_output": final_output,
        "messages": [AIMessage(content=truncate(message_text, 4000))],
        "stream_events": events,
    }


def serialize_final_output(final_output: Optional[dict]) -> str:
    """把最终输出序列化为 JSON 字符串（供 SSE 推送使用）。"""
    if not final_output:
        return "{}"
    return json.dumps(final_output, ensure_ascii=False, default=str)
