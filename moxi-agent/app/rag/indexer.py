"""索引构建：语料清洗、切分与批量入库"""

import json
from pathlib import Path

from langchain_core.documents import Document
from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.rag.vector_store import vector_store_manager


class NoteIndexer:
    """小红书笔记索引管线"""

    def __init__(self, chunk_size: int = 500, chunk_overlap: int = 50):
        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            separators=["\n\n", "\n", "。", "！", "？", ".", " ", ""],
        )
        self.store = vector_store_manager.get_notes_store()

    def index_file(self, file_path: str) -> int:
        """索引单个 JSON 文件，返回索引的文档数量

        Args:
            file_path: JSON 文件路径

        Returns:
            成功索引的文档片段数量
        """
        with open(file_path, "r", encoding="utf-8") as f:
            data = json.load(f)

        notes = data if isinstance(data, list) else [data]
        total_indexed = 0

        for note in notes:
            docs = self._process_note(note)
            if docs:
                self.store.add_documents(docs)
                total_indexed += len(docs)

        return total_indexed

    def _process_note(self, note: dict) -> list[Document]:
        """将单条笔记转换为 Document 列表

        Args:
            note: 笔记字典，包含 title, content, tags, category 等字段

        Returns:
            Document 列表
        """
        title = note.get("title", "")
        content = note.get("content", "")
        full_text = f"标题：{title}\n\n{content}"

        chunks = self.splitter.split_text(full_text)

        documents = []
        for i, chunk in enumerate(chunks):
            doc = Document(
                page_content=chunk,
                metadata={
                    "title": title,
                    "category": note.get("category", "未分类"),
                    "tags": ",".join(note.get("tags", [])),
                    "likes": note.get("likes", 0),
                    "collects": note.get("collects", 0),
                    "chunk_index": i,
                    "source": "seed_notes",
                },
            )
            documents.append(doc)

        return documents

    def index_directory(self, dir_path: str) -> int:
        """索引目录下所有 JSON 文件

        Args:
            dir_path: 目录路径

        Returns:
            总共索引的文档片段数量
        """
        total = 0
        path = Path(dir_path)
        for json_file in path.glob("*.json"):
            count = self.index_file(str(json_file))
            total += count
            print(f"  已索引 {json_file.name}: {count} 个文档片段")
        return total
