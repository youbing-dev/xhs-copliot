"""Agent 状态图节点包。

导出四个核心节点函数与检索节点，供 ``app.agent.graph`` 组装状态图：

- ``retrieve_node``  : 记忆 + RAG 上下文检索
- ``planner_node``   : LLM 规划执行计划
- ``executor_node``  : 逐步调用工具
- ``reflector_node`` : 质量评估与反馈
- ``responder_node`` : 组装最终输出
"""
from app.agent.nodes.executor import executor_node, resolve_params
from app.agent.nodes.planner import PlanOutput, PlanStepOutput, planner_node
from app.agent.nodes.reflector import ReflectionOutput, reflector_node
from app.agent.nodes.responder import responder_node, serialize_final_output
from app.agent.nodes.retrieve import retrieve_node

__all__ = [
    "retrieve_node",
    "planner_node",
    "executor_node",
    "reflector_node",
    "responder_node",
    "resolve_params",
    "serialize_final_output",
    "PlanOutput",
    "PlanStepOutput",
    "ReflectionOutput",
]
