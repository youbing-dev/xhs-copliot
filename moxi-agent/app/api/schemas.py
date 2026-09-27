"""API 请求 / 响应数据模型。"""

from typing import Optional

from pydantic import BaseModel, Field


class GenerateRequest(BaseModel):
    user_id: str = Field(..., description="用户ID")
    session_id: str = Field(..., description="会话ID")
    task_type: str = Field(
        default="generate_note",
        description="任务类型: generate_note, chat, refine",
    )
    topic: str = Field(..., description="主题/关键词")
    blogger_type: str = Field(default="生活博主", description="博主类型")
    style: str = Field(default="种草", description="内容风格")
    word_count: int = Field(default=800, description="目标字数")
    extra_params: Optional[dict] = Field(default=None, description="额外参数")


class ChatRequest(BaseModel):
    user_id: str = Field(..., description="用户ID")
    session_id: str = Field(..., description="会话ID")
    message: str = Field(..., description="用户消息")


class GenerateResponse(BaseModel):
    titles: list[dict] = Field(default_factory=list, description="标题候选列表")
    body: str = Field(default="", description="正文内容")
    tags: list[str] = Field(default_factory=list, description="话题标签")
    quality_score: float = Field(default=0.0, description="质量评分")
    total_tokens: int = Field(default=0, description="总Token消耗")
    duration_ms: int = Field(default=0, description="总耗时(ms)")
    iterations: int = Field(default=1, description="Agent循环次数")


class AgentErrorResponse(BaseModel):
    code: str = Field(..., description="错误码")
    message: str = Field(..., description="错误信息")
