"""标签生成工具 - 生成小红书话题标签"""
from __future__ import annotations

import json
import logging
import re

from langchain_core.tools import tool
from pydantic import BaseModel, Field

from app.agent.llm import get_llm

logger = logging.getLogger(__name__)


class TagGenInput(BaseModel):
    content: str = Field(description="笔记正文内容")
    topic: str = Field(default="", description="笔记主题")
    count: int = Field(default=10, description="生成标签数量(8-12)")


def _parse_json_response(text: str) -> dict:
    """从 LLM 响应中解析 JSON，兼容 markdown code block 包裹的情况。"""
    cleaned = text.strip()
    code_block_match = re.search(r"```(?:json)?\s*\n?(.*?)\n?\s*```", cleaned, re.DOTALL)
    if code_block_match:
        cleaned = code_block_match.group(1).strip()
    start = cleaned.find("{")
    end = cleaned.rfind("}")
    if start != -1 and end != -1:
        cleaned = cleaned[start : end + 1]
    return json.loads(cleaned)


def _ensure_hashtag(tag: str) -> str:
    """确保标签有 # 前缀。"""
    tag = tag.strip()
    if not tag.startswith("#"):
        tag = "#" + tag
    return tag


@tool(args_schema=TagGenInput)
def tag_generation_tool(content: str, topic: str = "", count: int = 10) -> dict:
    """根据笔记内容生成相关的小红书话题标签。标签应覆盖主题词、场景词、情感词等多个维度。"""
    try:
        llm = get_llm(temperature=0.7)

        system_prompt = (
            "你是一位小红书标签运营专家，擅长为笔记匹配最优的话题标签组合。\n\n"
            "【标签规范】\n"
            "1. 每个标签以 # 开头\n"
            "2. 标签长度 2-6 个字（不含 #）\n"
            "3. 标签组合策略：\n"
            "   - 大流量词（如 #好物分享 #日常记录）：获取曝光\n"
            "   - 精准主题词（如 #秋冬穿搭 #油皮护肤）：匹配目标用户\n"
            "   - 长尾词（如 #小个子显高穿搭）：降低竞争\n"
            "   - 情感/场景词（如 #治愈系 #周末去哪儿）：增加共鸣\n"
            "4. 避免过于宽泛或与内容无关的标签\n"
            "5. 标签之间要有差异化，不要重复含义\n\n"
            "请严格按照 JSON 格式输出，不要输出其他内容。"
        )

        # 截取部分内容避免 prompt 过长
        content_preview = content[:2000] if len(content) > 2000 else content

        user_prompt = (
            f"请为以下小红书笔记生成 {count} 个话题标签：\n\n"
        )
        if topic:
            user_prompt += f"【笔记主题】{topic}\n\n"
        user_prompt += (
            f"【笔记内容】\n{content_preview}\n\n"
            f"输出格式：\n"
            f'{{"tags": ["#标签1", "#标签2", ...]}}\n'
            f"请生成恰好 {count} 个标签。"
        )

        response = llm.invoke([
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ])

        result = _parse_json_response(response.content)
        raw_tags = result.get("tags", [])

        if not raw_tags:
            raise ValueError("LLM 返回的标签列表为空")

        # 确保每个标签都有 # 前缀，并去重
        tags = []
        seen = set()
        for tag in raw_tags:
            if not isinstance(tag, str):
                continue
            normalized = _ensure_hashtag(tag)
            if normalized not in seen:
                seen.add(normalized)
                tags.append(normalized)

        if not tags:
            raise ValueError("无有效标签")

        # 限制数量
        tags = tags[:count]

        return {"tags": tags, "count": len(tags)}

    except Exception as exc:
        logger.warning("tag_generation_tool failed: %s", exc)
        # 基于主题生成默认标签
        default_tags = ["#好物分享", "#日常记录", "#生活分享"]
        if topic:
            default_tags.insert(0, f"#{topic[:6]}")
        return {
            "tags": default_tags,
            "count": len(default_tags),
            "error": f"标签生成失败，使用默认标签: {str(exc)}",
        }
