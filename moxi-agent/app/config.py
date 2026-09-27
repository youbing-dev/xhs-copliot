"""应用配置：基于 pydantic-settings，从环境变量 / .env 文件加载。"""

from functools import lru_cache

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # LLM
    dashscope_api_key: str = ""
    llm_model: str = "qwen-plus"
    llm_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    embedding_model: str = "text-embedding-v3"

    # Redis
    redis_url: str = "redis://localhost:6379/1"

    # Chroma
    chroma_persist_dir: str = "./data/chroma"

    # Java Service
    java_service_url: str = "http://localhost:8080"
    internal_secret: str = "moxi-internal-2024"

    # Observability
    langsmith_api_key: str = ""
    langsmith_tracing: bool = True
    trace_enabled: bool = True

    # Server
    host: str = "0.0.0.0"
    port: int = 8001

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    """获取全局唯一配置实例（带缓存）。"""
    return Settings()
