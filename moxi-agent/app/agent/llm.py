"""LLM 工厂

所有需要调用大模型的节点（planner / reflector / responder）统一从此处获取客户端，
避免在各节点中重复读取配置、重复构造连接。

底层使用 DashScope 的 OpenAI 兼容端点，因此直接复用 ``langchain_openai.ChatOpenAI``。
"""
from __future__ import annotations

from langchain_openai import ChatOpenAI

from app.config import get_settings


def get_llm(
    temperature: float = 0.7,
    model: str | None = None,
    max_retries: int = 2,
    **kwargs,
) -> ChatOpenAI:
    """构造一个指向 DashScope 兼容端点的 ChatOpenAI 客户端。

    Args:
        temperature: 采样温度。规划/评估类节点建议低温（0.2~0.4），创作类节点建议高温。
        model: 覆盖默认模型名（``settings.llm_model``）。
        max_retries: 网络异常重试次数。
        **kwargs: 透传给 ``ChatOpenAI`` 的其它参数（如 ``max_tokens``、``streaming``）。

    Returns:
        ChatOpenAI 实例。
    """
    settings = get_settings()
    return ChatOpenAI(
        model=model or settings.llm_model,
        openai_api_key=settings.dashscope_api_key or "EMPTY",
        openai_api_base=settings.llm_base_url,
        temperature=temperature,
        max_retries=max_retries,
        **kwargs,
    )
