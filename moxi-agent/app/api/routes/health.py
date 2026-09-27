"""健康检查与可观测性路由。"""

from typing import Optional

from fastapi import APIRouter, Query

from app.observability.metrics import metrics_collector
from app.observability.tracer import tracer

router = APIRouter()


@router.get("/health")
async def health_check():
    return {"status": "healthy", "service": "moxi-agent"}


@router.get("/metrics")
async def get_metrics():
    """获取 Agent 运行指标（请求数、延迟、Token、成本、质量等聚合统计）。"""
    return metrics_collector.get_summary()


@router.get("/traces")
async def get_traces(
    limit: int = Query(20, ge=1, le=200, description="返回的最近追踪条数"),
    user_id: Optional[str] = Query(None, description="按用户 ID 过滤"),
):
    """获取最近的执行追踪记录。"""
    traces = tracer.get_recent_traces(limit=limit, user_id=user_id)
    return {"traces": traces, "count": len(traces)}
