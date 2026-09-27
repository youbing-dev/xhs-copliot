"""向量库：基于 Chroma 的持久化向量存储封装，支持多 Collection 管理"""

from langchain_chroma import Chroma

from app.config import get_settings
from app.rag.embedder import get_embeddings


class VectorStoreManager:
    """管理 Chroma 向量库的多个 Collection"""

    COLLECTIONS = {
        "xhs_notes": "小红书爆款笔记知识库",
        "user_memory": "用户长期记忆",
    }

    def __init__(self):
        self.settings = get_settings()
        self._stores: dict[str, Chroma] = {}

    def get_store(self, collection_name: str) -> Chroma:
        """获取或创建指定 collection 的 Chroma 实例"""
        if collection_name not in self._stores:
            self._stores[collection_name] = Chroma(
                collection_name=collection_name,
                embedding_function=get_embeddings(),
                persist_directory=self.settings.chroma_persist_dir,
            )
        return self._stores[collection_name]

    def get_notes_store(self) -> Chroma:
        """获取爆款笔记知识库向量存储"""
        return self.get_store("xhs_notes")

    def get_memory_store(self) -> Chroma:
        """获取用户长期记忆向量存储"""
        return self.get_store("user_memory")


# 全局单例
vector_store_manager = VectorStoreManager()
