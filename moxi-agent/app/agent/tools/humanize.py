"""去AI味改写工具 - 让文本更自然口语化"""
from __future__ import annotations

import logging

from langchain_core.tools import tool
from pydantic import BaseModel, Field

from app.agent.llm import get_llm

logger = logging.getLogger(__name__)


class HumanizeInput(BaseModel):
    text: str = Field(description="需要改写的文本")
    style: str = Field(default="口语化", description="目标风格：口语化/文艺/幽默")


@tool(args_schema=HumanizeInput)
def humanize_tool(text: str, style: str = "口语化") -> dict:
    """将AI生成的文本改写为更自然、口语化的表达，去除AI味道。保留原意的同时让文字更像真人写的。"""
    try:
        llm = get_llm(temperature=0.8)

        system_prompt = (
            "你是一位文字改写专家，擅长将 AI 生成的文本改写为自然的人类表达。\n\n"
            "【改写原则】\n"
            "1. 去除机械化连接词：删掉「首先」「其次」「最后」「总之」「综上所述」等模板化表达\n"
            "2. 加入口语化表达（根据目标风格调整）：\n"
            "   - 口语化风格：「真的绝了」「谁懂啊」「救命」「真的会谢」「属于是」「一整个爱住」\n"
            "   - 文艺风格：更诗意、更含蓄、有画面感\n"
            "   - 幽默风格：玩梗、自嘲、夸张\n"
            "3. 保持段落节奏感：长短句交替，不要每句话都差不多长度\n"
            "4. 不改变核心信息：改写是换一种说法，不是换内容\n"
            "5. 加入个人感受：「我觉得」「说实话」「不得不承认」等主观表达\n"
            "6. 避免过于整齐：真人写作不会每段都差不多长度\n"
            "7. 适当使用语气词：「嘛」「啦」「呀」「呢」「哦」\n"
            "8. 可以加一些不完美的表达：犹豫、转折、补充说明\n\n"
            "【注意】\n"
            "- 直接输出改写后的文本\n"
            "- 不要加任何前缀说明（如「改写后：」）\n"
            "- 不要输出 JSON\n"
            "- 保持原文的段落结构（大致）"
        )

        user_prompt = (
            f"请将以下文本改写为「{style}」风格，去除 AI 味道，让它读起来像真人写的：\n\n"
            f"---\n{text}\n---\n\n"
            f"改写后的文本："
        )

        response = llm.invoke([
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ])

        new_text = response.content.strip()

        return {
            "text": new_text,
            "original_length": len(text),
            "new_length": len(new_text),
        }

    except Exception as exc:
        logger.warning("humanize_tool failed: %s", exc)
        return {
            "text": text,
            "original_length": len(text),
            "new_length": len(text),
            "error": f"改写失败，返回原文: {str(exc)}",
        }
