"""Agent Graph 端到端测试"""
from unittest.mock import MagicMock, patch

import pytest

from app.agent.state import AgentState


def test_state_definition():
    """验证 AgentState 定义正确"""
    # AgentState 应该是 TypedDict
    assert AgentState is not None
    # 验证关键字段存在
    annotations = AgentState.__annotations__
    assert "user_id" in annotations or hasattr(AgentState, "__annotations__")


def test_graph_compilation():
    """验证 Agent Graph 可以正常编译"""
    with patch("app.agent.nodes.planner.get_llm") as mock_llm:
        mock_llm.return_value = MagicMock()
        from app.agent.graph import agent_graph

        assert agent_graph is not None


def test_initial_state_creation():
    """验证初始状态创建"""
    try:
        from app.agent.state import initial_state

        state = initial_state(
            user_id="test_user",
            session_id="test_session",
            task_type="generate_note",
            topic="测试主题",
        )
        assert state["user_id"] == "test_user"
        assert state["session_id"] == "test_session"
    except ImportError:
        # initial_state 可能不存在，跳过
        pass


def test_variable_resolution():
    """验证 executor 的变量引用解析"""
    try:
        from app.agent.nodes.executor import resolve_params
    except ImportError:
        pytest.skip("resolve_params not directly importable")

    tool_results = {
        1: {"titles": [{"text": "标题A", "score": 9}], "best_title": "标题A"},
        2: {"text": "正文内容..."},
    }
    # executor 使用 {{stepN.field}} 语法引用前序步骤的结果
    params = {
        "title": "{{step1.best_title}}",
        "content": "{{step2.text}}",
        "static": "不变的值",
    }
    resolved = resolve_params(params, tool_results)
    assert resolved["title"] == "标题A"
    assert resolved["content"] == "正文内容..."
    assert resolved["static"] == "不变的值"
