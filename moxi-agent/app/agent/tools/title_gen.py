"""标题生成工具 - 生成小红书笔记标题候选列表"""
from __future__ import annotations

import json
import logging
import re

from langchain_core.tools import tool
from pydantic import BaseModel, Field

from app.agent.llm import get_llm

logger = logging.getLogger(__name__)


class TitleGenInput(BaseModel):
    topic: str = Field(description="笔记主题/关键词")
    blogger_type: str = Field(default="生活博主", description="博主类型，如美妆博主、穿搭博主、美食博主")
    style: str = Field(default="种草", description="内容风格，如种草、教程、测评、分享")
    count: int = Field(default=5, description="生成标题数量")


def _parse_json_response(text: str) -> dict:
    """从 LLM 响应中解析 JSON，兼容 markdown code block 包裹的情况。"""
    # 尝试去除 markdown code block
    cleaned = text.strip()
    code_block_match = re.search(r"```(?:json)?\s*\n?(.*?)\n?\s*```", cleaned, re.DOTALL)
    if code_block_match:
        cleaned = code_block_match.group(1).strip()
    # 尝试找到第一个 { 和最后一个 }
    start = cleaned.find("{")
    end = cleaned.rfind("}")
    if start != -1 and end != -1:
        cleaned = cleaned[start : end + 1]
    return json.loads(cleaned)


@tool(args_schema=TitleGenInput)
def title_generation_tool(
    topic: str,
    blogger_type: str = "生活博主",
    style: str = "种草",
    count: int = 5,
) -> dict:
    """生成小红书笔记标题候选列表，每个标题带吸引力评分(1-10)。适用于需要创作吸引眼球的小红书标题时使用。"""
    try:
        llm = get_llm(temperature=0.9)

        system_prompt = (
            "你是一位资深小红书运营专家，擅长创作吸引眼球的笔记标题。\n"
            "小红书爆款标题的特点：\n"
            "1. 适当使用 emoji 增加视觉吸引力（但不是每个都要有）\n"
            "2. 包含数字（如「5个方法」「3分钟搞定」）\n"
            "3. 制造悬念或好奇心（如「后悔没早知道」「绝了」）\n"
            "4. 口语化表达，像跟朋友分享\n"
            "5. 字数控制在 15-25 字之间\n"
            "6. 避免过度营销感，要自然真实\n\n"
            "请严格按照 JSON 格式输出，不要输出其他内容。"
        )

        user_prompt = (
            f"请为以下主题生成 {count} 个小红书笔记标题：\n"
            f"- 主题/关键词：{topic}\n"
            f"- 博主类型：{blogger_type}\n"
            f"- 内容风格：{style}\n\n"
            f"输出格式：\n"
            f'{{"titles": [{{"text": "标题文本", "score": 8.5}}]}}\n'
            f"其中 score 为吸引力评分(1-10分)，表示该标题在小红书获得高点击率的潜力。"
        )

        response = llm.invoke([
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ])

        result = _parse_json_response(response.content)
        titles = result.get("titles", [])

        # 确保标题列表有效
        if not titles:
            raise ValueError("LLM 返回的标题列表为空")

        # 验证每个标题都有 text 和 score
        validated_titles = []
        for t in titles:
            if isinstance(t, dict) and "text" in t:
                validated_titles.append({
                    "text": str(t["text"]),
                    "score": float(t.get("score", 5.0)),
                })
            elif isinstance(t, str):
                validated_titles.append({"text": t, "score": 5.0})

        if not validated_titles:
            raise ValueError("无有效标题")

        # 按分数降序排列，选出最佳标题
        validated_titles.sort(key=lambda x: x["score"], reverse=True)
        best_title = validated_titles[0]["text"]

        return {"titles": validated_titles, "best_title": best_title}

    except Exception as exc:
        logger.warning("title_generation_tool failed: %s", exc)
        fallback = f"关于{topic}的分享"
        return {
            "titles": [{"text": fallback, "score": 5.0}],
            "best_title": fallback,
            "error": f"解析失败，使用默认标题: {str(exc)}",
        }
