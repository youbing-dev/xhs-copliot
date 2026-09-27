"""Agent 工具注册表

所有工具在此统一注册，executor 节点通过工具名查找并调用。

设计要点：
- 注册表是模块级单例 ``_TOOLS``，键为工具名（planner 输出的 ``action``），
  值为 ``BaseTool`` 实例（也兼容普通可调用对象）。
- 工具本身在 Task 5 中实现，本模块只负责"注册 / 发现 / 描述"。
- ``load_tools()`` 对每个工具模块单独 try/except：某个工具尚未实现或依赖缺失时，
  只记录警告并跳过，不影响其余工具与整条 Agent 链路。
- ``get_tool_descriptions()`` 生成注入 planner system prompt 的工具清单文本。
"""
from __future__ import annotations

import importlib
import logging
from typing import Any, Callable, Optional, Union

from langchain_core.tools import BaseTool

logger = logging.getLogger(__name__)

ToolLike = Union[BaseTool, Callable[..., Any]]

# 工具注册表 —— Task 5 实现具体工具后由 load_tools() 自动填充
_TOOLS: dict[str, ToolLike] = {}

# 工具名 → (模块路径, 属性名)。用于按需加载，避免包导入期的循环依赖。
_TOOL_SPECS: dict[str, tuple[str, str]] = {
    "title_gen": ("app.agent.tools.title_gen", "title_generation_tool"),
    "content_gen": ("app.agent.tools.content_gen", "content_generation_tool"),
    "tag_gen": ("app.agent.tools.tag_gen", "tag_generation_tool"),
    "humanize": ("app.agent.tools.humanize", "humanize_tool"),
    "trend_search": ("app.agent.tools.trend_search", "trend_search_tool"),
    "sensitive_check": ("app.agent.tools.sensitive_check", "sensitive_check_tool"),
}


# ---------------------------------------------------------------------------
# 注册 / 查询
# ---------------------------------------------------------------------------


def register_tool(name: str, tool: ToolLike) -> None:
    """注册一个工具（同名覆盖，便于热替换与测试注入）。"""
    _TOOLS[name] = tool
    logger.debug("tool registered: %s", name)


def unregister_tool(name: str) -> None:
    """注销一个工具（主要用于测试）。"""
    _TOOLS.pop(name, None)


def get_tool(name: str) -> Optional[ToolLike]:
    """根据名称获取工具；不存在时返回 ``None``。"""
    return _TOOLS.get(name)


def get_all_tools() -> dict[str, ToolLike]:
    """获取所有已注册工具的浅拷贝。"""
    return _TOOLS.copy()


def has_tool(name: str) -> bool:
    """判断工具是否已注册。"""
    return name in _TOOLS


# ---------------------------------------------------------------------------
# 描述文本（注入 planner prompt）
# ---------------------------------------------------------------------------


def _tool_description(tool: ToolLike) -> str:
    """提取工具的简要描述。"""
    desc = getattr(tool, "description", None)
    if not desc and callable(tool):
        desc = (tool.__doc__ or "").strip().split("\n")[0]
    return (desc or "无描述").strip()


def _tool_params(tool: ToolLike) -> list[str]:
    """提取工具的参数签名文本，如 ``["topic(string)", "count(integer)"]``。"""
    args_schema = getattr(tool, "args_schema", None)
    if args_schema is None:
        return []

    schema: dict[str, Any] = {}
    # pydantic v2 推荐 model_json_schema；v1 兼容 schema()
    if hasattr(args_schema, "model_json_schema"):
        try:
            schema = args_schema.model_json_schema()
        except Exception as exc:  # pragma: no cover - 防御性
            logger.debug("args_schema.model_json_schema failed: %s", exc)
    elif hasattr(args_schema, "schema"):
        try:
            schema = args_schema.schema()  # type: ignore[union-attr]
        except Exception as exc:  # pragma: no cover - 防御性
            logger.debug("args_schema.schema failed: %s", exc)

    properties = schema.get("properties", {}) or {}
    required = set(schema.get("required", []) or [])

    params: list[str] = []
    for key, spec in properties.items():
        if not isinstance(spec, dict):
            params.append(f"{key}(any)")
            continue
        p_type = spec.get("type")
        if p_type is None and "anyOf" in spec:
            p_type = "/".join(
                str(t.get("type", "any")) for t in spec["anyOf"] if isinstance(t, dict)
            )
        mark = "" if key in required else "?"
        desc = spec.get("description", "")
        text = f"{key}{mark}({p_type or 'any'})"
        if desc:
            text += f": {desc}"
        params.append(text)
    return params


def get_tool_descriptions() -> str:
    """获取所有工具的描述文本，用于注入 planner 的 system prompt。"""
    if not _TOOLS:
        return "暂无可用工具"

    descriptions: list[str] = []
    for name, tool in _TOOLS.items():
        desc = f"- {name}: {_tool_description(tool)}"
        params = _tool_params(tool)
        if params:
            desc += f" | 参数: {', '.join(params)}"
        descriptions.append(desc)
    return "\n".join(descriptions)


def get_tool_names() -> list[str]:
    """获取所有已注册工具名。"""
    return list(_TOOLS.keys())


# ---------------------------------------------------------------------------
# 加载
# ---------------------------------------------------------------------------


def load_tools(force: bool = False) -> dict[str, ToolLike]:
    """加载所有工具（在应用启动时调用）。

    Args:
        force: 为 True 时重新加载全部工具（默认已注册则跳过，保证幂等）。

    Returns:
        加载后的工具注册表。
    """
    if _TOOLS and not force:
        return _TOOLS.copy()

    for name, (module_path, attr) in _TOOL_SPECS.items():
        if name in _TOOLS and not force:
            continue
        try:
            module = importlib.import_module(module_path)
            tool = getattr(module, attr)
        except (ImportError, AttributeError) as exc:
            # 工具尚未实现（Task 5）或依赖缺失：优雅降级，不中断启动
            logger.warning("load tool '%s' skipped: %s", name, exc)
            continue
        except Exception as exc:  # pragma: no cover - 防御性
            logger.warning("load tool '%s' failed: %s", name, exc)
            continue
        register_tool(name, tool)

    logger.info("tools loaded: %s", ", ".join(_TOOLS) or "<none>")
    return _TOOLS.copy()
