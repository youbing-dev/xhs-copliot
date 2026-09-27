"""Agent 核心包：状态图、节点与工具。

对外暴露：

- ``agent_graph`` / ``create_agent_graph``：LangGraph 状态图（编译实例与工厂）
- ``AgentState`` / ``PlanStep`` / ``initial_state``：状态定义与初始状态构造
- ``get_llm``：统一的 LLM 客户端工厂（DashScope OpenAI 兼容端点）
- 节点函数：``retrieve_node`` / ``planner_node`` / ``executor_node``
  / ``reflector_node`` / ``responder_node``

注意：``graph`` 模块在导入时会编译状态图并尝试加载工具注册表，
工具本体（Task 5）缺失时会静默降级，不影响导入。
"""
from app.agent.graph import agent_graph, create_agent_graph
from app.agent.llm import get_llm
from app.agent.nodes import (
    executor_node,
    planner_node,
    reflector_node,
    responder_node,
    retrieve_node,
)
from app.agent.state import (
    AgentState,
    PlanStep,
    default_state,
    initial_state,
    make_plan_step,
)

__all__ = [
    "agent_graph",
    "create_agent_graph",
    "get_llm",
    "AgentState",
    "PlanStep",
    "default_state",
    "initial_state",
    "make_plan_step",
    "retrieve_node",
    "planner_node",
    "executor_node",
    "reflector_node",
    "responder_node",
]
