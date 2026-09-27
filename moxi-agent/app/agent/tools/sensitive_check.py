"""敏感词校验工具 - 回调 Java 服务检查内容安全性"""
from __future__ import annotations

import logging

import httpx
from langchain_core.tools import tool
from pydantic import BaseModel, Field

from app.config import get_settings

logger = logging.getLogger(__name__)


class SensitiveCheckInput(BaseModel):
    text: str = Field(description="需要检查的文本内容")


@tool(args_schema=SensitiveCheckInput)
def sensitive_check_tool(text: str) -> dict:
    """检查文本是否包含敏感词或违规内容。调用Java后端服务进行安全校验，确保内容合规后再发布。"""
    try:
        settings = get_settings()
        url = f"{settings.java_service_url}/internal/content/sensitive-check"
        headers = {
            "X-Internal-Secret": settings.internal_secret,
            "Content-Type": "application/json",
        }
        payload = {"text": text}

        with httpx.Client(timeout=10.0) as client:
            response = client.post(url, json=payload, headers=headers)
            response.raise_for_status()

        data = response.json()

        return {
            "safe": data.get("safe", True),
            "violations": data.get("violations", []),
        }

    except httpx.TimeoutException:
        logger.warning("sensitive_check_tool timeout: Java 服务响应超时")
        return {
            "safe": True,
            "violations": [],
            "warning": "敏感词服务不可用（超时），已跳过检查",
        }

    except httpx.ConnectError:
        logger.warning("sensitive_check_tool connection error: 无法连接到 Java 服务")
        return {
            "safe": True,
            "violations": [],
            "warning": "敏感词服务不可用（连接失败），已跳过检查",
        }

    except httpx.HTTPStatusError as exc:
        logger.warning("sensitive_check_tool HTTP error: %s", exc)
        return {
            "safe": True,
            "violations": [],
            "warning": f"敏感词服务返回错误（HTTP {exc.response.status_code}），已跳过检查",
        }

    except Exception as exc:
        logger.warning("sensitive_check_tool failed: %s", exc)
        return {
            "safe": True,
            "violations": [],
            "warning": f"敏感词服务不可用，已跳过检查: {str(exc)}",
        }
