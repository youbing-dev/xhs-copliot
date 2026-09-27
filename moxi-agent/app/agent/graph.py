"""LangGraph Agent 状态图定义。

拓扑（Plan → Execute → Reflect 闭环）::

    START → retrieve → planner → executor ─┐
                       ↑          ↓        │
                       │     (还有步骤?) ──┘  ← route_check: executor
                       │          ↓ 否
                       └──── reflector
                            (质量达标 或 迭代到上限?)
                                 ↓ 是          ↑ 否 → planner（re-plan）
                            responder → END

条件边：
- ``route_check``   : ``current_step < len(plan)`` → 继续 executor，否则进入 reflector
- ``quality_check`` : ``quality_score >= 0.7`` 或 ``iteration_count >= 3`` → responder，
  否则回到 planner 携带反馈重新规划

导出：
- ``agent_graph``          : 模块级编译好的图实例（默认用法）
- ``create_agent_graph()`` : 工厂函数，需要 checkpointer / 自定义配置时使用
"""
from __future__ import annotations

import logging
from typing import Any, Optional

from langgraph.graph import END, START, StateGraph

from app.agent.nodes.executor import executor_node
from app.agent.nodes.planner import planner_node
from app.agent.nodes.reflector import (
    MAX_ITERATIONS,
    QUALITY_THRESHOLD,
    reflector_node,
)
from app.agent.nodes.responder import responder_node
from app.agent.nodes.retrieve import retrieve_node
from app.agent.state import AgentState
from app.agent.tools.registry import load_tools

logger = logging.getLogger(__name__)

# 图节点名常量（供 API 层做事件过滤 / 单步调试引用）
NODE_RETRIEVE = "retrieve"
NODE_PLANNER = "planner"
NODE_EXECUTOR = "executor"
NODE_REFLECTOR = "reflector"
NODE_RESPONDER = "responder"

ROUTE_EXECUTOR = "executor"
ROUTE_REFLECTOR = "reflector"
ROUTE_PLANNER = "planner"
ROUTE_RESPONDER = "responder"


# ---------------------------------------------------------------------------
# 条件边
# ---------------------------------------------------------------------------


def route_check(state: AgentState) -> str:
    """检查是否还有未执行的步骤。

    Returns:
        ``"executor"``（继续执行下一步）或 ``"reflector"``（计划执行完毕）。
    """
    plan = state.get("plan") or []
    current_step = state.get("current_step") or 0

    if plan and current_step < len(plan):
        # 跳过已被预标记为 failed 且已有结果的步骤（executor 内部也会处理，
        # 这里提前推进可以减少一次无意义的节点调度）
        return ROUTE_EXECUTOR
    return ROUTE_REFLECTOR


def quality_check(state: AgentState) -> str:
    """检查质量是否达标，决定收尾还是重新规划。

    Returns:
        ``"responder"``（达标或已达最大迭代次数）或 ``"planner"``（重新规划）。
    """
    quality_score = float(state.get("quality_score") or 0.0)
    iteration_count = int(state.get("iteration_count") or 0)

    if quality_score >= QUALITY_THRESHOLD or iteration_count >= MAX_ITERATIONS:
        if iteration_count >= MAX_ITERATIONS and quality_score < QUALITY_THRESHOLD:
            logger.info(
                "quality_check: max iterations (%s) reached with score %.3f, "
                "finalizing anyway",
                MAX_ITERATIONS,
                quality_score,
            )
        return ROUTE_RESPONDER
    return ROUTE_PLANNER


# ---------------------------------------------------------------------------
# 图构建
# ---------------------------------------------------------------------------


def build_workflow() -> StateGraph:
    """构建（未编译的）状态图。"""
    workflow = StateGraph(AgentState)

    workflow.add_node(NODE_RETRIEVE, retrieve_node)
    workflow.add_node(NODE_PLANNER, planner_node)
    workflow.add_node(NODE_EXECUTOR, executor_node)
    workflow.add_node(NODE_REFLECTOR, reflector_node)
    workflow.add_node(NODE_RESPONDER, responder_node)

    # 入口：先检索上下文，再交给规划器
    workflow.add_edge(START, NODE_RETRIEVE)
    workflow.add_edge(NODE_RETRIEVE, NODE_PLANNER)
    workflow.add_edge(NODE_PLANNER, NODE_EXECUTOR)

    # 执行循环：还有步骤就继续执行，否则进入反思
    workflow.add_conditional_edges(
        NODE_EXECUTOR,
        route_check,
        {ROUTE_EXECUTOR: NODE_EXECUTOR, ROUTE_REFLECTOR: NODE_REFLECTOR},
    )

    # 反思后：达标收尾，不达标带着反馈重新规划
    workflow.add_conditional_edges(
        NODE_REFLECTOR,
        quality_check,
        {ROUTE_RESPONDER: NODE_RESPONDER, ROUTE_PLANNER: NODE_PLANNER},
    )

    workflow.add_edge(NODE_RESPONDER, END)
    return workflow


def create_agent_graph(
    checkpointer: Optional[Any] = None,
    interrupt_before: Optional[list[str]] = None,
    interrupt_after: Optional[list[str]] = None,
    load_registered_tools: bool = True,
):
    """编译 Agent 状态图（工厂函数）。

    Args:
        checkpointer: LangGraph 持久化器（如 ``MemorySaver``），用于多轮会话续跑。
        interrupt_before: 在指定节点前中断，便于人工审核（human-in-the-loop）。
        interrupt_after: 在指定节点后中断。
        load_registered_tools: 是否在编译前加载工具注册表（默认加载；
            单测中可关闭以避免依赖 Task 5 的工具实现）。

    Returns:
        编译后的 ``CompiledGraph``。
    """
    if load_registered_tools:
        try:
            load_tools()
        except Exception as exc:  # pragma: no cover - 防御性
            logger.warning("create_agent_graph: load_tools failed: %s", exc)

    workflow = build_workflow()
    compile_kwargs: dict[str, Any] = {}
    if checkpointer is not None:
        compile_kwargs["checkpointer"] = checkpointer
    if interrupt_before:
        compile_kwargs["interrupt_before"] = interrupt_before
    if interrupt_after:
        compile_kwargs["interrupt_after"] = interrupt_after

    graph = workflow.compile(**compile_kwargs)
    logger.info("agent graph compiled (checkpointer=%s)", checkpointer is not None)
    return graph


# 模块级默认实例：API 层可直接 ``from app.agent.graph import agent_graph``
agent_graph = create_agent_graph()

__all__ = [
    "agent_graph",
    "create_agent_graph",
    "build_workflow",
    "route_check",
    "quality_check",
    "NODE_RETRIEVE",
    "NODE_PLANNER",
    "NODE_EXECUTOR",
    "NODE_REFLECTOR",
    "NODE_RESPONDER",
]
