"""FastAPI 应用入口。"""

import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.agent.tools.registry import load_tools
from app.api.routes import agent, health
from app.config import get_settings

logger = logging.getLogger(__name__)

settings = get_settings()

app = FastAPI(
    title="Moxi Agent Service",
    description="小红书 AI 内容创作 Agent - 基于 LangGraph 的智能内容生成服务",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health.router, tags=["health"])
app.include_router(agent.router, prefix="/api/agent", tags=["agent"])


@app.on_event("startup")
async def _startup_load_tools() -> None:
    """启动时加载 Agent 工具注册表。

    工具实现缺失时 ``load_tools`` 内部会逐个降级跳过，不会阻断服务启动。
    """
    try:
        load_tools()
    except Exception as exc:  # pragma: no cover - 防御性
        logger.warning("startup: load_tools failed: %s", exc)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host=settings.host, port=settings.port, reload=True)
