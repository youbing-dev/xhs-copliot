"""正文生成工具 - 生成小红书笔记正文"""
from __future__ import annotations

import logging

from langchain_core.tools import tool
from pydantic import BaseModel, Field

from app.agent.llm import get_llm

logger = logging.getLogger(__name__)


class ContentGenInput(BaseModel):
    title: str = Field(description="笔记标题")
    topic: str = Field(description="笔记主题")
    blogger_type: str = Field(default="生活博主", description="博主类型")
    word_count: int = Field(default=800, description="目标字数")
    style: str = Field(default="种草", description="内容风格")
    rag_context: str = Field(default="", description="RAG检索的参考内容")


@tool(args_schema=ContentGenInput)
def content_generation_tool(
    title: str,
    topic: str,
    blogger_type: str = "生活博主",
    word_count: int = 800,
    style: str = "种草",
    rag_context: str = "",
) -> dict:
    """生成小红书笔记正文内容。根据标题和主题创作口语化、有感染力的种草/分享文案。"""
    try:
        llm = get_llm(temperature=0.8)

        system_prompt = (
            "你是一位顶级小红书内容创作者，擅长写出让人忍不住看完、收藏、点赞的笔记正文。\n\n"
            "【写作规范】\n"
            "1. 口语化表达：像跟闺蜜/好友聊天，用「姐妹们」「真的绝了」「谁懂啊」等自然表达\n"
            "2. 适当使用 emoji：用来分隔段落、标注重点，但不要过度（每段1-2个即可）\n"
            "3. 开头必须有 hook：第一句话就要抓住注意力，可以用反转、提问、惊叹\n"
            "4. 中间是干货/体验分享：真实感、细节感很重要，要有具体的使用场景和感受\n"
            "5. 结尾有互动引导：「你们觉得呢？」「快试试吧！」「有问题评论区见～」\n"
            "6. 段落短小：每段 2-3 句话，方便手机阅读\n"
            "7. 避免：机械化的「首先/其次/最后」，过于正式/书面的表达，广告感太强的用语\n"
            "8. 语气真实：有个人情感，有主观判断，不回避缺点（适当提一两个小缺点更真实）\n\n"
            "【格式要求】\n"
            "- 直接输出正文内容，不要加标题\n"
            "- 段落之间空一行\n"
            "- 不要输出 JSON 或其他格式"
        )

        # 构建 user prompt
        user_parts = [
            f"请根据以下信息创作小红书笔记正文：\n",
            f"- 标题：{title}",
            f"- 主题：{topic}",
            f"- 博主人设：{blogger_type}",
            f"- 内容风格：{style}",
            f"- 目标字数：约 {word_count} 字",
        ]

        if rag_context:
            user_parts.append(f"\n【参考内容（仅供灵感，不要照抄）】\n{rag_context}")

        user_parts.append("\n请开始创作正文：")
        user_prompt = "\n".join(user_parts)

        response = llm.invoke([
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ])

        text = response.content.strip()
        actual_word_count = len(text)

        return {
            "text": text,
            "word_count": actual_word_count,
            "title": title,
        }

    except Exception as exc:
        logger.warning("content_generation_tool failed: %s", exc)
        fallback_text = f"今天来跟大家聊聊{topic}～\n\n作为一个{blogger_type}，这方面的经验还是蛮多的，之后会慢慢分享给大家！\n\n你们有什么想了解的，评论区告诉我哦～"
        return {
            "text": fallback_text,
            "word_count": len(fallback_text),
            "title": title,
            "error": f"生成失败，使用默认内容: {str(exc)}",
        }
