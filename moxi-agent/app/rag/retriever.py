"""检索器：相似笔记召回与相关性评分"""

from typing import Optional

from langchain_core.documents import Document

from app.rag.vector_store import vector_store_manager


async def retrieve_notes(
    query: str,
    top_k: int = 5,
    filters: Optional[dict] = None,
) -> list[Document]:
    """从爆款笔记知识库检索相关文档

    Args:
        query: 检索查询文本
        top_k: 返回文档数量
        filters: Chroma metadata 过滤条件，如 {"category": "美妆"}

    Returns:
        相关文档列表
    """
    store = vector_store_manager.get_notes_store()
    kwargs: dict = {"k": top_k}
    if filters:
        kwargs["filter"] = filters
    results = store.similarity_search_with_relevance_scores(query, **kwargs)
    return [doc for doc, score in results]


async def retrieve_with_scores(
    query: str,
    top_k: int = 5,
    filters: Optional[dict] = None,
) -> list[tuple[Document, float]]:
    """检索并返回相关性分数

    Args:
        query: 检索查询文本
        top_k: 返回文档数量
        filters: Chroma metadata 过滤条件

    Returns:
        (文档, 相关性分数) 元组列表
    """
    store = vector_store_manager.get_notes_store()
    kwargs: dict = {"k": top_k}
    if filters:
        kwargs["filter"] = filters
    return store.similarity_search_with_relevance_scores(query, **kwargs)


def format_context(documents: list[Document], max_length: int = 3000) -> str:
    """将检索结果格式化为上下文字符串，用于注入 prompt

    Args:
        documents: Document 列表
        max_length: 最大上下文字符数

    Returns:
        格式化后的上下文字符串
    """
    context_parts: list[str] = []
    total_length = 0

    for i, doc in enumerate(documents, 1):
        part = f"[参考{i}] {doc.metadata.get('title', '无标题')}\n{doc.page_content}\n"
        if total_length + len(part) > max_length:
            break
        context_parts.append(part)
        total_length += len(part)

    return "\n---\n".join(context_parts) if context_parts else "暂无相关参考内容"
