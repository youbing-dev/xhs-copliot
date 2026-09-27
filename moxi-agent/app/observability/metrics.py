"""
Agent 运行指标收集与统计
提供请求计数、延迟分布、Token 消耗、质量评分等聚合统计。

设计要点：
- 指标保存在内存中，服务重启后重置（对当前项目足够）。
- 只保留最近 ``max_history`` 条明细，避免长时间运行内存无限增长。
"""
import statistics
import time
from collections import defaultdict
from dataclasses import dataclass, field
from typing import Optional

from app.config import get_settings
from app.observability.tracer import calculate_cost


@dataclass
class RequestMetric:
    """单次请求的指标。"""
    timestamp: float
    user_id: str
    task_type: str
    duration_ms: int
    total_tokens: int
    quality_score: float
    iterations: int
    success: bool
    error: Optional[str] = None
    tool_calls: list[str] = field(default_factory=list)


class MetricsCollector:
    """指标收集器（全局单例使用）。"""

    def __init__(self, max_history: int = 1000):
        self.max_history = max_history
        self._metrics: list[RequestMetric] = []
        self._token_by_user: dict[str, int] = defaultdict(int)
        self._token_by_tool: dict[str, int] = defaultdict(int)
        self._request_count: int = 0
        self._error_count: int = 0
        self._start_time: float = time.time()

    def record(self, metric: RequestMetric) -> None:
        """记录一次请求指标。"""
        self._metrics.append(metric)
        self._request_count += 1

        if not metric.success:
            self._error_count += 1

        self._token_by_user[metric.user_id] += metric.total_tokens

        # 工具级 token 明细若随请求携带，也累加进 by_tool
        # （tool_calls 仅记录工具名，token 归属由 record_tool_tokens 精确累加）

        # 只保留最近 max_history 条
        if len(self._metrics) > self.max_history:
            self._metrics = self._metrics[-self.max_history:]

    def record_tool_tokens(self, tool_name: str, tokens: int) -> None:
        """记录工具级别的 token 消耗。"""
        self._token_by_tool[tool_name] += tokens

    def get_summary(self) -> dict:
        """获取聚合统计摘要。"""
        if not self._metrics:
            return {
                "total_requests": self._request_count,
                "success_rate": self._success_rate(),
                "uptime_seconds": int(time.time() - self._start_time),
                "latency": {"p50_ms": 0, "p95_ms": 0, "avg_ms": 0, "max_ms": 0},
                "tokens": {
                    "total": 0,
                    "avg_per_request": 0,
                    "by_user": dict(self._token_by_user),
                    "by_tool": dict(self._token_by_tool),
                },
                "quality": {"avg_score": 0.0, "min_score": 0.0, "max_score": 0.0},
                "iterations": {"avg": 0, "distribution": {}},
                "cost": {"total_yuan": 0.0},
                "task_types": self._task_type_distribution(),
            }

        durations = [m.duration_ms for m in self._metrics]
        tokens = [m.total_tokens for m in self._metrics]
        scores = [m.quality_score for m in self._metrics if m.quality_score > 0]
        iterations = [m.iterations for m in self._metrics]

        summary = {
            "total_requests": self._request_count,
            "success_rate": self._success_rate(),
            "uptime_seconds": int(time.time() - self._start_time),
            "latency": {
                "p50_ms": int(statistics.median(durations)) if durations else 0,
                "p95_ms": int(self._percentile(durations, 95)) if durations else 0,
                "avg_ms": int(statistics.mean(durations)) if durations else 0,
                "max_ms": max(durations) if durations else 0,
            },
            "tokens": {
                "total": sum(tokens),
                "avg_per_request": int(statistics.mean(tokens)) if tokens else 0,
                "by_user": dict(self._token_by_user),
                "by_tool": dict(self._token_by_tool),
            },
            "quality": {
                "avg_score": round(statistics.mean(scores), 3) if scores else 0.0,
                "min_score": round(min(scores), 3) if scores else 0.0,
                "max_score": round(max(scores), 3) if scores else 0.0,
            },
            "iterations": {
                "avg": round(statistics.mean(iterations), 2) if iterations else 0,
                "distribution": self._iteration_distribution(iterations),
            },
            "cost": {
                "total_yuan": round(
                    calculate_cost(get_settings().llm_model, sum(tokens), 0), 4
                ),
            },
            "task_types": self._task_type_distribution(),
        }

        return summary

    def _success_rate(self) -> float:
        """成功率（无请求时返回 0）。"""
        if not self._request_count:
            return 0.0
        return round((self._request_count - self._error_count) / self._request_count, 4)

    def _percentile(self, data: list, p: int) -> float:
        """计算百分位数（线性插值）。"""
        if not data:
            return 0
        sorted_data = sorted(data)
        idx = (len(sorted_data) - 1) * p / 100
        lower = int(idx)
        upper = lower + 1
        if upper >= len(sorted_data):
            return sorted_data[-1]
        weight = idx - lower
        return sorted_data[lower] * (1 - weight) + sorted_data[upper] * weight

    def _iteration_distribution(self, iterations: list[int]) -> dict[str, int]:
        """迭代次数分布。"""
        dist: dict[str, int] = defaultdict(int)
        for i in iterations:
            dist[str(i)] += 1
        return dict(dist)

    def _task_type_distribution(self) -> dict[str, int]:
        """任务类型分布。"""
        dist: dict[str, int] = defaultdict(int)
        for m in self._metrics:
            dist[m.task_type] += 1
        return dict(dist)


# 全局单例
metrics_collector = MetricsCollector()
