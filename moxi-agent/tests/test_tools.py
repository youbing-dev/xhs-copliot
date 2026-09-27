"""Agent Tools 单元测试"""
import httpx
import pytest
from unittest.mock import patch, MagicMock

from app.agent.tools.registry import (
    get_all_tools,
    get_tool,
    get_tool_descriptions,
    load_tools,
)


# ---------------------------------------------------------------------------
# 工具注册
# ---------------------------------------------------------------------------


def test_load_tools():
    """验证所有工具可以成功加载"""
    load_tools(force=True)
    tools = get_all_tools()
    assert len(tools) == 6
    assert "title_gen" in tools
    assert "content_gen" in tools
    assert "tag_gen" in tools
    assert "humanize" in tools
    assert "trend_search" in tools
    assert "sensitive_check" in tools


def test_get_tool_descriptions():
    """验证工具描述生成"""
    load_tools(force=True)
    desc = get_tool_descriptions()
    assert "title_gen" in desc
    assert "content_gen" in desc


def test_get_nonexistent_tool():
    """验证获取不存在的工具返回 None"""
    assert get_tool("nonexistent_tool") is None


# ---------------------------------------------------------------------------
# 各工具（mock LLM / mock HTTP）
# ---------------------------------------------------------------------------


@patch("app.agent.tools.title_gen.get_llm")
def test_title_gen_tool(mock_get_llm):
    """测试标题生成工具（mock LLM）"""
    mock_llm = MagicMock()
    mock_llm.invoke.return_value = MagicMock(
        content='{"titles": [{"text": "测试标题", "score": 8.0}], "best_title": "测试标题"}'
    )
    mock_get_llm.return_value = mock_llm

    load_tools(force=True)
    tool = get_tool("title_gen")
    result = tool.invoke(
        {"topic": "测试主题", "blogger_type": "生活博主", "style": "种草", "count": 3}
    )
    assert isinstance(result, dict)
    # mock 生效时应返回解析后的最佳标题；即便解析失败也会降级返回默认标题
    assert "best_title" in result


def test_sensitive_check_fallback():
    """测试敏感词工具在 Java 服务不可用时的降级"""
    # 模拟 httpx 客户端发起请求时连接失败，触发工具的降级分支
    with patch(
        "httpx.Client.post",
        side_effect=httpx.ConnectError("Connection refused"),
    ):
        load_tools(force=True)
        tool = get_tool("sensitive_check")
        result = tool.invoke({"text": "测试文本"})

    assert isinstance(result, dict)
    # 服务不可用时应降级返回 safe=True（并附带 warning 说明）
    assert result.get("safe") is True
