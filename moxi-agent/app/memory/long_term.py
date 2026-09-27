"""
Chroma 长期用户记忆管理

设计要点：
- 存储：复用 RAG 层的 ``user_memory`` collection（由 ``vector_store_manager`` 提供）。
- 内容：用户写作偏好、历史高质量生成结果摘要、用户反馈、关键交互记录。
- 检索：语义相似度检索，按 ``user_id`` 过滤；可选 ``memory_type`` 进一步收窄。
- 容错：向量库异常或 collection 不存在时，所有写/读操作降级为静默 no-op / 空结果，
  避免上层 Agent 链路被打断。
"""
from __future__ import annotations

import logging
import time
import uuid
from typing import Optional

from langchain_core.documents import Document

from app.rag.vector_store import vector_store_manager

logger = logging.getLogger(__name__)


class LongTermMemory:
    """基于 Chroma 向量库的长期用户记忆。"""

    def __init__(self) -> None:
        self._store = None

    @property
    def store(self):
        """延迟初始化 Chroma store。"""
        if self._store is None:
            try:
                self._store = vector_store_manager.get_memory_store()
            except Exception as exc:
                logger.warning("LongTermMemory: memory store init failed: %s", exc)
                self._store = None
        return self._store

    async def save_memory(
        self,
        user_id: str,
        content: str,
        memory_type: str = "preference",
        metadata: Optional[dict] = None,
    ) -> str:
        """保存一条长期记忆。

        Args:
            user_id: 用户 ID。
            content: 记忆内容（自然语言描述）。
            memory_type: 记忆类型：

                - ``preference``: 用户写作偏好。
                - ``generation``: 历史高质量生成结果摘要。
                - ``feedback``: 用户反馈和修改记录。
                - ``interaction``: 重要交互记录。
            metadata: 额外元数据。

        Returns:
            新记忆的 ID；写入失败时返回空字符串。
        """
        store = self.store
        if store is None:
            return ""

        memory_id = str(uuid.uuid4())
        doc = Document(
            page_content=content,
            metadata={
                "memory_id": memory_id,
                "user_id": user_id,
                "memory_type": memory_type,
                "timestamp": time.time(),
                **(metadata or {}),
            },
        )

        try:
            store.add_documents([doc], ids=[memory_id])
        except Exception as exc:
            logger.warning("LongTermMemory.save_memory failed: %s", exc)
            return ""
        return memory_id

    async def recall_memories(
        self,
        user_id: str,
        query: str,
        top_k: int = 5,
        memory_type: Optional[str] = None,
    ) -> list[Document]:
        """语义检索相关记忆。

        Args:
            user_id: 用户 ID。
            query: 检索查询（通常是当前用户请求）。
            top_k: 返回数量。
            memory_type: 可选，按类型过滤。

        Returns:
            相关记忆文档列表；向量库异常或无数据时返回空列表。
        """
        store = self.store
        if store is None:
            return []

        filters: dict = {"user_id": user_id}
        if memory_type:
            filters["memory_type"] = memory_type

        try:
            results = store.similarity_search(query, k=top_k, filter=filters)
            return results
        except Exception as exc:
            # 没有该用户的记忆数据或 collection 为空时，Chroma 可能抛错
            logger.debug("LongTermMemory.recall_memories miss: %s", exc)
            return []

    async def get_user_preference_context(self, user_id: str, query: str) -> str:
        """获取用户偏好上下文字符串，用于注入 Agent prompt。

        Returns:
            格式化的用户偏好描述；无任何记忆时返回空字符串。
        """
        memories = await self.recall_memories(user_id, query, top_k=5)
        if not memories:
            return ""

        sections: dict[str, list[str]] = {
            "preference": [],
            "generation": [],
            "feedback": [],
            "interaction": [],
        }

        for doc in memories:
            mem_type = doc.metadata.get("memory_type", "interaction")
            if mem_type in sections:
                sections[mem_type].append(doc.page_content)

        context_parts: list[str] = []

        if sections["preference"]:
            context_parts.append(
                "用户写作偏好：\n" + "\n".join(f"- {p}" for p in sections["preference"])
            )

        if sections["feedback"]:
            context_parts.append(
                "历史反馈：\n" + "\n".join(f"- {f}" for f in sections["feedback"])
            )

        if sections["generation"]:
            context_parts.append(
                "历史优质内容参考：\n"
                + "\n".join(f"- {g[:100]}" for g in sections["generation"])
            )

        return "\n\n".join(context_parts)

    async def save_generation_result(
        self,
        user_id: str,
        topic: str,
        title: str,
        quality_score: float,
        summary: str,
    ) -> None:
        """保存高质量生成结果作为长期记忆。

        仅当 ``quality_score > 0.8`` 时才会写入，避免污染向量库。
        """
        if quality_score <= 0.8:
            return

        content = (
            f"主题「{topic}」的生成结果（评分{quality_score:.1f}）："
            f"标题「{title}」- {summary}"
        )
        await self.save_memory(
            user_id=user_id,
            content=content,
            memory_type="generation",
            metadata={"topic": topic, "quality_score": quality_score},
        )

    async def save_user_feedback(
        self,
        user_id: str,
        feedback: str,
        context: str = "",
    ) -> None:
        """保存用户反馈。"""
        content = f"用户反馈：{feedback}"
        if context:
            content += f"（上下文：{context}）"
        await self.save_memory(
            user_id=user_id,
            content=content,
            memory_type="feedback",
        )

    async def delete_user_memories(self, user_id: str) -> int:
        """删除指定用户的所有长期记忆（用于隐私合规）。

        Returns:
            1 表示删除成功，0 表示向量库不可用或删除失败。
        """
        store = self.store
        if store is None:
            return 0

        try:
            store.delete(filter={"user_id": user_id})
            return 1
        except Exception as exc:
            logger.warning("LongTermMemory.delete_user_memories failed: %s", exc)
            return 0


# 全局单例
long_term_memory = LongTermMemory()
