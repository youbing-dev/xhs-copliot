"""
Agent 执行链路追踪
记录每次 Agent 执行的完整过程，用于调试、分析和优化。

设计要点：
- Tracer 使用全局单例，追踪过程以内存对象聚合，结束时落盘为 JSON Lines。
- 追踪属于「旁路」能力，任何 IO / 序列化异常都不得影响主业务流程。
"""
import json
import uuid
from dataclasses import dataclass, field, asdict
from datetime import datetime
from pathlib import Path
from typing import Optional

from app.config import get_settings


# 输入 / 输出摘要的最大长度，避免把超长 prompt 塞进 trace 文件
_SUMMARY_MAX_LEN = 200


def _truncate(text: object, limit: int = _SUMMARY_MAX_LEN) -> str:
    """把任意对象安全地截断为摘要字符串。"""
    if text is None:
        return ""
    if not isinstance(text, str):
        try:
            text = json.dumps(text, ensure_ascii=False, default=str)
        except Exception:
            text = str(text)
    text = text.replace("\n", " ").strip()
    if len(text) > limit:
        return text[: limit - 3] + "..."
    return text


@dataclass
class StepTrace:
    """单步执行追踪。"""
    step_id: int
    tool_name: str
    input_summary: str          # 输入参数摘要（自动截断到 200 字符）
    output_summary: str         # 输出结果摘要（自动截断到 200 字符）
    tokens_used: int = 0
    duration_ms: int = 0
    status: str = "pending"     # pending | running | done | failed

    def __post_init__(self) -> None:
        # 强制摘要长度约束，避免超长 prompt 落盘撞大 trace 文件
        self.input_summary = _truncate(self.input_summary)
        self.output_summary = _truncate(self.output_summary)


@dataclass
class AgentTrace:
    """完整 Agent 执行追踪。"""
    trace_id: str
    user_id: str
    session_id: str
    task_type: str
    start_time: datetime
    end_time: Optional[datetime] = None
    total_tokens: int = 0
    total_cost: float = 0.0
    iterations: int = 0
    plan_steps: list[StepTrace] = field(default_factory=list)
    quality_score: float = 0.0
    error: Optional[str] = None
    metadata: dict = field(default_factory=dict)

    def to_dict(self) -> dict:
        """转换为可序列化的 dict（datetime -> isoformat 字符串）。"""
        d = asdict(self)
        d["start_time"] = self.start_time.isoformat()
        d["end_time"] = self.end_time.isoformat() if self.end_time else None
        return d


class Tracer:
    """Agent 执行追踪器（全局单例使用）。"""

    def __init__(self, trace_dir: str = "./data/traces"):
        self.settings = get_settings()
        self.trace_dir = Path(trace_dir)
        self._active_traces: dict[str, AgentTrace] = {}
        # 目录创建属于旁路操作，失败也不应阻断导入 / 启动
        try:
            self.trace_dir.mkdir(parents=True, exist_ok=True)
        except Exception as e:  # pragma: no cover - 防御性
            print(f"[Tracer] trace 目录创建失败: {e}")
        self.trace_file = self.trace_dir / "agent_traces.jsonl"

    def start_trace(
        self,
        user_id: str,
        session_id: str,
        task_type: str,
        metadata: Optional[dict] = None,
    ) -> str:
        """开始一次新的追踪，返回 trace_id。"""
        trace_id = str(uuid.uuid4())[:12]
        trace = AgentTrace(
            trace_id=trace_id,
            user_id=user_id,
            session_id=session_id,
            task_type=task_type,
            start_time=datetime.now(),
            metadata=metadata or {},
        )
        self._active_traces[trace_id] = trace
        return trace_id

    def record_step(self, trace_id: str, step_trace: StepTrace) -> None:
        """记录一步执行。"""
        if trace_id in self._active_traces:
            self._active_traces[trace_id].plan_steps.append(step_trace)

    def update_step(self, trace_id: str, step_id: int, **kwargs) -> None:
        """更新已有步骤的信息（按字段名安全赋值）。"""
        if trace_id not in self._active_traces:
            return
        trace = self._active_traces[trace_id]
        for step in trace.plan_steps:
            if step.step_id == step_id:
                for key, value in kwargs.items():
                    if hasattr(step, key):
                        setattr(step, key, value)
                break

    def end_trace(
        self,
        trace_id: str,
        quality_score: float = 0.0,
        total_tokens: int = 0,
        error: Optional[str] = None,
    ) -> None:
        """结束追踪并持久化。

        total_tokens 传 0 时，自动累加各步骤已记录的 tokens_used，
        以便调用方无需重复统计。
        """
        if trace_id not in self._active_traces:
            return

        trace = self._active_traces.pop(trace_id)
        trace.end_time = datetime.now()
        trace.quality_score = quality_score
        if not total_tokens:
            total_tokens = sum(s.tokens_used for s in trace.plan_steps)
        trace.total_tokens = total_tokens
        trace.total_cost = calculate_cost(self.settings.llm_model, total_tokens, 0)
        trace.iterations = len([s for s in trace.plan_steps if s.status == "done"])
        trace.error = error

        self._persist_trace(trace)

    def _persist_trace(self, trace: AgentTrace) -> None:
        """持久化到 JSONL 文件。失败仅告警，不抛出。"""
        if not self.settings.trace_enabled:
            return
        try:
            with open(self.trace_file, "a", encoding="utf-8") as f:
                f.write(json.dumps(trace.to_dict(), ensure_ascii=False) + "\n")
        except Exception as e:
            # 追踪失败不应影响主流程
            print(f"[Tracer] 持久化失败: {e}")

    def get_recent_traces(self, limit: int = 20, user_id: Optional[str] = None) -> list[dict]:
        """读取最近的追踪记录（按文件追加顺序，取末尾 limit 条）。"""
        if not self.trace_file.exists():
            return []

        traces: list[dict] = []
        try:
            with open(self.trace_file, "r", encoding="utf-8") as f:
                for line in f:
                    if not line.strip():
                        continue
                    try:
                        trace = json.loads(line)
                    except json.JSONDecodeError:
                        continue
                    if user_id is None or trace.get("user_id") == user_id:
                        traces.append(trace)
        except Exception:
            return []

        return traces[-limit:]


def calculate_cost(model: str, input_tokens: int, output_tokens: int) -> float:
    """计算 LLM 调用成本（人民币，单位：元）。

    定价单位：元 / 千 tokens。未配置的模型按 qwen-plus 兜底。
    """
    # 模型定价（元/千tokens）
    pricing = {
        "qwen-plus": {"input": 0.004, "output": 0.012},
        "qwen-turbo": {"input": 0.002, "output": 0.006},
        "qwen-max": {"input": 0.02, "output": 0.06},
        "qwen-long": {"input": 0.0005, "output": 0.002},
    }

    model_pricing = pricing.get(model, pricing["qwen-plus"])
    cost = (
        input_tokens / 1000 * model_pricing["input"]
        + output_tokens / 1000 * model_pricing["output"]
    )
    return round(cost, 6)


# 全局单例
tracer = Tracer()
