"""Executor 节点：按计划逐步调用工具。

职责：
1. 取出 ``plan[current_step]`` 作为当前待执行步骤；
2. 解析参数中的 ``{{stepN.field}}`` 变量引用，替换为前序步骤的真实结果；
3. 从工具注册表按名称查找工具并调用（``BaseTool.ainvoke`` 或普通可调用对象）；
4. 写回步骤状态（``done`` / ``failed``）与 ``tool_results``，并推进 ``current_step``；
5. 产出 ``tool_start`` / ``tool_result`` 流式事件。

容错原则：任何一步失败都不会中断整张图 —— 记录错误结果、标记 ``failed``、继续下一步，
由 reflector 判断整体质量是否需要重新规划。
"""
from __future__ import annotations

import asyncio
import inspect
import json
import logging
import re
import time
from typing import Any, Optional

from langchain_core.tools import BaseTool

from app.agent.nodes._common import (
    EVENT_ERROR,
    EVENT_TOOL_RESULT,
    EVENT_TOOL_START,
    make_event,
    truncate,
)
from app.agent.state import AgentState, PlanStep
from app.agent.tools.registry import get_tool, load_tools

logger = logging.getLogger(__name__)

# 变量引用语法：{{step2.best_title}}
VARIABLE_PATTERN = re.compile(r"\{\{step(\d+)\.(\w+)\}\}")

# 单个工具调用的超时时间（秒）
TOOL_TIMEOUT_SECONDS = 120.0

# 防御性上限：即使条件边出现异常也不会无限循环
MAX_TOTAL_STEPS = 50


# ---------------------------------------------------------------------------
# 变量引用解析
# ---------------------------------------------------------------------------


def resolve_params(params: Any, tool_results: dict) -> dict:
    """解析参数中的 ``{{stepN.field}}`` 变量引用。

    - 整个字符串恰好是一个变量引用，且目标是**非字符串**结构（list / dict / 数字）时，
      直接返回原始对象（保留类型，而不是被 ``str()`` 破坏）；
    - 字符串中嵌入了变量引用时，做文本替换；
    - 嵌套的 dict / list 递归解析。

    Args:
        params: LLM 规划出的原始参数。
        tool_results: ``{step_id: result}`` 映射。

    Returns:
        解析后的参数字典。
    """
    if not isinstance(params, dict):
        # LLM 偶尔会输出非 dict 的 params，做兜底处理
        return {} if params is None else {"input": params}

    resolved: dict[str, Any] = {}
    for key, value in params.items():
        resolved[key] = _resolve_value(value, tool_results)
    return resolved


def _resolve_value(value: Any, tool_results: dict) -> Any:
    """递归解析单个参数值。"""
    if isinstance(value, str):
        if "{{" in value:
            return _resolve_variable(value, tool_results)
        return value
    if isinstance(value, dict):
        return {k: _resolve_value(v, tool_results) for k, v in value.items()}
    if isinstance(value, list):
        return [_resolve_value(v, tool_results) for v in value]
    return value


def _lookup(tool_results: dict, step_id: int, field: str) -> Optional[Any]:
    """从工具结果中取字段值。"""
    result = tool_results.get(step_id)
    if result is None:
        # 兼容字符串 key（部分序列化场景会把 dict key 变成 str）
        result = tool_results.get(str(step_id))
    if isinstance(result, dict):
        return result.get(field)
    if field in ("value", "result", "output"):
        return result
    return None


def _resolve_variable(template: str, tool_results: dict) -> Any:
    """解析变量引用，如 ``{{step2.best_title}}`` → ``tool_results[2]["best_title"]``。

    解析不到时**保留原始占位符**，便于后续排查与 reflector 感知。
    """
    # 整体就是一个引用：保留原始类型
    full_match = VARIABLE_PATTERN.fullmatch(template.strip())
    if full_match:
        step_id = int(full_match.group(1))
        field = full_match.group(2)
        value = _lookup(tool_results, step_id, field)
        if value is not None:
            return value
        logger.warning(
            "executor: unresolved variable reference step%s.%s", step_id, field
        )
        return template

    def replacer(match: re.Match) -> str:
        step_id = int(match.group(1))
        field = match.group(2)
        value = _lookup(tool_results, step_id, field)
        if value is None:
            logger.warning(
                "executor: unresolved variable reference step%s.%s", step_id, field
            )
            return match.group(0)
        if isinstance(value, (dict, list)):
            return json.dumps(value, ensure_ascii=False)
        return str(value)

    return VARIABLE_PATTERN.sub(replacer, template)


def has_unresolved(params: dict) -> bool:
    """判断参数中是否仍存在未解析的变量引用。"""
    return bool(VARIABLE_PATTERN.search(json.dumps(params, ensure_ascii=False, default=str)))


# ---------------------------------------------------------------------------
# 工具调用
# ---------------------------------------------------------------------------


def _normalize_result(raw: Any) -> dict:
    """把工具返回值统一成 dict，方便变量引用与结果组装。"""
    if isinstance(raw, dict):
        return raw
    if raw is None:
        return {}
    if isinstance(raw, str):
        return {"value": raw, "text": raw}
    if isinstance(raw, (list, tuple)):
        return {"value": list(raw), "items": list(raw)}
    return {"value": raw}


def _summarize(result: dict, limit: int = 300) -> str:
    """生成用于流式事件的结果摘要。"""
    if not result:
        return "（空结果）"
    for key in ("content", "text", "best_title", "value", "summary"):
        value = result.get(key)
        if isinstance(value, str) and value:
            return truncate(value, limit)
    try:
        return truncate(json.dumps(result, ensure_ascii=False, default=str), limit)
    except (TypeError, ValueError):  # pragma: no cover - 防御性
        return truncate(str(result), limit)


async def _invoke_tool(tool: Any, params: dict) -> Any:
    """统一调用入口：兼容 BaseTool、async 函数与普通函数。"""
    if isinstance(tool, BaseTool):
        return await asyncio.wait_for(tool.ainvoke(params), timeout=TOOL_TIMEOUT_SECONDS)

    if inspect.iscoroutinefunction(tool):
        return await asyncio.wait_for(tool(**params), timeout=TOOL_TIMEOUT_SECONDS)

    if callable(tool):
        # 同步函数放到线程池，避免阻塞事件循环
        loop = asyncio.get_running_loop()
        return await asyncio.wait_for(
            loop.run_in_executor(None, lambda: tool(**params)),
            timeout=TOOL_TIMEOUT_SECONDS,
        )

    raise TypeError(f"工具对象不可调用: {type(tool).__name__}")


# ---------------------------------------------------------------------------
# 节点实现
# ---------------------------------------------------------------------------


def _clone_plan(plan: list[PlanStep]) -> list[PlanStep]:
    """深拷贝计划，避免原地修改状态导致 reducer 合并异常。"""
    return [
        PlanStep(
            id=step.get("id"),
            action=step.get("action"),
            params=dict(step.get("params") or {}),
            status=step.get("status", "pending"),
            result=step.get("result"),
        )
        for step in (plan or [])
    ]


async def executor_node(state: AgentState) -> dict:
    """执行节点：调用当前步骤对应的工具。

    Returns:
        状态增量：``plan`` / ``current_step`` / ``tool_results`` / ``stream_events``。
    """
    plan = _clone_plan(state.get("plan") or [])
    current_step = state.get("current_step") or 0
    tool_results: dict[Any, Any] = dict(state.get("tool_results") or {})
    events: list[dict] = []

    # 防御：计划为空或已越界，直接交给 reflector
    if not plan or current_step >= len(plan):
        logger.warning(
            "executor: nothing to run (plan_len=%s, current_step=%s)",
            len(plan),
            current_step,
        )
        return {
            "plan": plan,
            "current_step": max(current_step, len(plan)),
            "tool_results": tool_results,
            "stream_events": events,
        }

    # 防御：总步数上限
    executed_total = sum(1 for s in plan if s.get("status") in ("done", "failed"))
    if executed_total >= MAX_TOTAL_STEPS:  # pragma: no cover - 极端防御
        logger.error("executor: reached MAX_TOTAL_STEPS, aborting execution")
        events.append(
            make_event(EVENT_ERROR, stage="executor", message="执行步数超过上限，已中止")
        )
        return {
            "plan": plan,
            "current_step": len(plan),
            "tool_results": tool_results,
            "stream_events": events,
        }

    step = plan[current_step]
    step_id = step.get("id") or (current_step + 1)
    action = step.get("action") or ""

    # 预标记失败的步骤（如工具未注册）直接跳过，不再重复执行
    if step.get("status") == "failed":
        message = (step.get("result") or {}).get("error", "步骤已被标记为失败")
        events.append(
            make_event(
                EVENT_TOOL_RESULT,
                step_id=step_id,
                tool=action,
                status="failed",
                success=False,
                skipped=True,
                error=message,
                duration_ms=0,
            )
        )
        tool_results.setdefault(step_id, step.get("result") or {"error": message})
        return {
            "plan": plan,
            "current_step": current_step + 1,
            "tool_results": tool_results,
            "stream_events": events,
        }

    # ------------------------------------------------------------------
    # 1. 解析参数
    # ------------------------------------------------------------------
    try:
        resolved_params = resolve_params(step.get("params"), tool_results)
    except Exception as exc:  # pragma: no cover - 防御性
        logger.exception("executor: resolve_params failed for step %s", step_id)
        resolved_params = dict(step.get("params") or {})
        events.append(
            make_event(
                EVENT_ERROR,
                stage="executor",
                step_id=step_id,
                message=f"参数解析失败：{exc}",
            )
        )

    unresolved = has_unresolved(resolved_params)
    if unresolved:
        logger.warning("executor: step %s has unresolved variable references", step_id)

    step["status"] = "running"
    events.append(
        make_event(
            EVENT_TOOL_START,
            step_id=step_id,
            tool=action,
            params=truncate(
                json.dumps(resolved_params, ensure_ascii=False, default=str), 500
            ),
            total_steps=len(plan),
        )
    )

    # ------------------------------------------------------------------
    # 2. 查找工具
    # ------------------------------------------------------------------
    tool = None
    try:
        tool = get_tool(action)
        if tool is None:
            # 工具可能在运行期才注册成功，这里再尝试加载一次
            load_tools()
            tool = get_tool(action)
    except (ImportError, AttributeError) as exc:
        # 工具模块尚未实现（Task 5）：优雅降级
        logger.warning("executor: tool module '%s' unavailable: %s", action, exc)
    except Exception as exc:  # pragma: no cover - 防御性
        logger.warning("executor: load_tools failed: %s", exc)

    started_at = time.perf_counter()

    if tool is None:
        error = f"工具 '{action}' 未注册或尚未实现，已跳过该步骤"
        logger.warning("executor: %s", error)
        step["status"] = "failed"
        step["result"] = {"error": error, "skipped": True}
        tool_results[step_id] = step["result"]
        events.append(
            make_event(
                EVENT_TOOL_RESULT,
                step_id=step_id,
                tool=action,
                status="failed",
                success=False,
                skipped=True,
                error=error,
                duration_ms=int((time.perf_counter() - started_at) * 1000),
            )
        )
        return {
            "plan": plan,
            "current_step": current_step + 1,
            "tool_results": tool_results,
            "stream_events": events,
        }

    # ------------------------------------------------------------------
    # 3. 调用工具
    # ------------------------------------------------------------------
    try:
        raw_result = await _invoke_tool(tool, resolved_params)
        result = _normalize_result(raw_result)
        if unresolved:
            result.setdefault("warning", "部分参数引用未能解析，结果可能不完整")
        step["status"] = "done"
        step["result"] = result
        tool_results[step_id] = result
        events.append(
            make_event(
                EVENT_TOOL_RESULT,
                step_id=step_id,
                tool=action,
                status="done",
                success=True,
                summary=_summarize(result),
                duration_ms=int((time.perf_counter() - started_at) * 1000),
            )
        )
        logger.info(
            "executor: step %s (%s) done in %dms",
            step_id,
            action,
            int((time.perf_counter() - started_at) * 1000),
        )
    except asyncio.TimeoutError:
        error = f"工具 '{action}' 执行超时（>{TOOL_TIMEOUT_SECONDS:.0f}s）"
        logger.error("executor: %s", error)
        step["status"] = "failed"
        step["result"] = {"error": error, "timeout": True}
        tool_results[step_id] = step["result"]
        events.append(
            make_event(
                EVENT_TOOL_RESULT,
                step_id=step_id,
                tool=action,
                status="failed",
                success=False,
                error=error,
                duration_ms=int((time.perf_counter() - started_at) * 1000),
            )
        )
    except Exception as exc:
        error = f"工具 '{action}' 执行失败：{type(exc).__name__}: {exc}"
        logger.exception("executor: step %s (%s) failed", step_id, action)
        step["status"] = "failed"
        step["result"] = {"error": error}
        tool_results[step_id] = step["result"]
        events.append(
            make_event(
                EVENT_TOOL_RESULT,
                step_id=step_id,
                tool=action,
                status="failed",
                success=False,
                error=error,
                duration_ms=int((time.perf_counter() - started_at) * 1000),
            )
        )

    return {
        "plan": plan,
        "current_step": current_step + 1,
        "tool_results": tool_results,
        "stream_events": events,
    }
