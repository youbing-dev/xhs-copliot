"""RAG Pipeline 测试"""
import json
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest


def test_seed_data_exists():
    """验证种子数据文件存在且格式正确"""
    seed_file = Path(__file__).parent.parent / "data" / "seed_notes" / "sample_notes.json"
    assert seed_file.exists(), "种子数据文件不存在"

    with open(seed_file, "r", encoding="utf-8") as f:
        data = json.load(f)

    assert isinstance(data, list)
    assert len(data) >= 10, "种子数据应至少有10篇笔记"

    # 验证每篇笔记的字段完整性
    for note in data:
        assert "title" in note
        assert "content" in note
        assert "tags" in note
        assert "category" in note
        assert isinstance(note["tags"], list)
        assert len(note["content"]) > 100


def test_indexer_initialization():
    """验证 NoteIndexer 可以正常初始化

    NoteIndexer 初始化会创建 Chroma 向量库并需要 Embedding 模型实例，
    后者依赖 DASHSCOPE_API_KEY。单测中 mock 掉 embedding 工厂，
    使其不依赖真实 API Key 与网络。
    """
    from app.rag.indexer import NoteIndexer

    with patch("app.rag.vector_store.get_embeddings", return_value=MagicMock()):
        indexer = NoteIndexer(chunk_size=500, chunk_overlap=50)
    assert indexer is not None
    assert indexer.splitter is not None


def test_format_context():
    """验证上下文格式化函数"""
    from langchain_core.documents import Document

    from app.rag.retriever import format_context

    docs = [
        Document(page_content="测试内容1", metadata={"title": "标题1"}),
        Document(page_content="测试内容2", metadata={"title": "标题2"}),
    ]

    context = format_context(docs)
    assert "标题1" in context
    assert "测试内容1" in context
    assert "参考1" in context


def test_format_context_empty():
    """验证空文档列表的格式化"""
    from app.rag.retriever import format_context

    context = format_context([])
    assert "暂无" in context
