"""趋势检索工具 - 从爆款笔记知识库检索相关内容"""
from __future__ import annotations

import logging
from typing import Optional

from langchain_core.tools import tool
from pydantic import BaseModel, Field

from app.rag.vector_store import vector_store_manager
from app.rag.retriever import format_context

logger = logging.getLogger(__name__)


class TrendSearchInput(BaseModel):
    topic: str = Field(description="检索主题/关键词")
    limit: int = Field(default=5, description="返回结果数量")
    category: str = Field(default="", description="可选分类过滤：美妆/穿搭/美食/旅行/家居/健身/数码")


@tool(args_schema=TrendSearchInput)
def trend_search_tool(topic: str, limit: int = 5, category: str = "") -> dict:
    """从爆款笔记知识库中检索与主题相关的趋势内容和参考笔记。用于获取创作灵感和热门话题参考。"""
    try:
        # 构造过滤条件
        filters: Optional[dict] = None
        if category:
            filters = {"category": category}

        # 直接使用 Chroma 的同步 API
        store = vector_store_manager.get_notes_store()
        search_kwargs: dict = {"k": limit}
        if filters:
            search_kwargs["filter"] = filters

        documents = store.similarity_search(topic, **search_kwargs)

        if not documents:
            return {
                "trends": "暂无相关趋势数据",
                "reference_notes": [],
                "count": 0,
            }

        # 格式化上下文
        context = format_context(documents)

        # 提取参考笔记的结构化信息
        reference_notes = []
        for doc in documents:
            meta = doc.metadata or {}
            reference_notes.append({
                "title": meta.get("title", "无标题"),
                "category": meta.get("category", "未分类"),
                "likes": meta.get("likes", 0),
                "content_preview": doc.page_content[:200] if doc.page_content else "",
            })

        return {
            "trends": context,
            "reference_notes": reference_notes,
            "count": len(documents),
        }

    except Exception as exc:
        logger.warning("trend_search_tool failed: %s", exc)
        return {
            "trends": "暂无相关趋势数据",
            "reference_notes": [],
            "count": 0,
            "error": f"检索失败: {str(exc)}",
        }
