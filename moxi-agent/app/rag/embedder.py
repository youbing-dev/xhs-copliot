"""向量化：调用 DashScope text-embedding 模型生成文本向量（兼容 OpenAI 协议）"""

from langchain_openai import OpenAIEmbeddings

from app.config import get_settings


def get_embeddings() -> OpenAIEmbeddings:
    """获取 DashScope Embedding 模型实例（兼容 OpenAI 协议）"""
    settings = get_settings()
    return OpenAIEmbeddings(
        model=settings.embedding_model,
        openai_api_key=settings.dashscope_api_key,
        openai_api_base=settings.llm_base_url,
    )
