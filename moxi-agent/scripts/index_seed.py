"""
初始化 RAG 向量库索引
用法: python -m scripts.index_seed
"""

import sys
from pathlib import Path

# 确保项目根目录在 sys.path
sys.path.insert(0, str(Path(__file__).parent.parent))

from app.rag.indexer import NoteIndexer
from app.config import get_settings


def main():
    settings = get_settings()
    seed_dir = Path(__file__).parent.parent / "data" / "seed_notes"

    if not seed_dir.exists():
        print(f"错误: 种子数据目录不存在: {seed_dir}")
        sys.exit(1)

    print("开始索引种子数据...")
    print(f"数据目录: {seed_dir}")
    print(f"Chroma 持久化目录: {settings.chroma_persist_dir}")

    indexer = NoteIndexer(chunk_size=500, chunk_overlap=50)
    total = indexer.index_directory(str(seed_dir))

    print(f"\n索引完成! 共索引 {total} 个文档片段")


if __name__ == "__main__":
    main()
