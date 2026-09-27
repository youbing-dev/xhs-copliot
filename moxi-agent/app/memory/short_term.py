"""
Redis 短期对话记忆管理

设计要点：
- 存储结构：Redis List，key 形如 ``agent:memory:{session_id}``。
- 内容：最近 N 轮对话消息（滑动窗口，默认保留 20 条）。
- 过期策略：每次写入刷新 TTL，默认 24 小时无活动自动过期。
- 容错：Redis 连接失败时不会导致整个服务崩溃，所有写操作降级为静默 no-op，
  读操作降级为空结果，由上层决定是否提示"会话记忆不可用"。
"""
from __future__ import annotations

import json
import logging
import time
from typing import Optional

import redis.asyncio as redis

from app.config import get_settings

logger = logging.getLogger(__name__)


class ShortTermMemory:
    """基于 Redis 的短期对话记忆。"""

    DEFAULT_MAX_MESSAGES = 20
    DEFAULT_TTL_SECONDS = 86400  # 24 hours

    def __init__(
        self,
        max_messages: int = DEFAULT_MAX_MESSAGES,
        ttl: int = DEFAULT_TTL_SECONDS,
    ) -> None:
        self.settings = get_settings()
        self.max_messages = max_messages
        self.ttl = ttl
        self._redis: Optional[redis.Redis] = None

    async def _get_redis(self) -> Optional[redis.Redis]:
        """懒加载 Redis 客户端；连接异常时返回 None 以触发降级。"""
        if self._redis is None:
            try:
                self._redis = redis.from_url(
                    self.settings.redis_url,
                    decode_responses=True,
                )
            except Exception as exc:  # pragma: no cover - 防御性
                logger.warning("ShortTermMemory: redis client init failed: %s", exc)
                return None
        return self._redis

    def _key(self, session_id: str) -> str:
        return f"agent:memory:{session_id}"

    async def add_message(
        self,
        session_id: str,
        role: str,
        content: str,
        metadata: Optional[dict] = None,
    ) -> None:
        """添加一条消息到对话历史。

        Args:
            session_id: 会话 ID。
            role: 角色 (user / assistant / system / tool)。
            content: 消息内容。
            metadata: 额外元数据（如 tool_name、tokens 等）。
        """
        r = await self._get_redis()
        if r is None:
            return

        key = self._key(session_id)
        message = {
            "role": role,
            "content": content,
            "timestamp": time.time(),
            "metadata": metadata or {},
        }

        try:
            await r.rpush(key, json.dumps(message, ensure_ascii=False))
            # 滑动窗口：只保留最近 N 条
            await r.ltrim(key, -self.max_messages, -1)
            # 刷新 TTL
            await r.expire(key, self.ttl)
        except Exception as exc:
            logger.warning("ShortTermMemory.add_message failed: %s", exc)
            # 连接层失败时清空客户端，下次调用重建
            self._redis = None

    async def get_messages(
        self,
        session_id: str,
        limit: Optional[int] = None,
    ) -> list[dict]:
        """获取对话历史。

        Args:
            session_id: 会话 ID。
            limit: 最多返回条数，None 表示全部（不超过 ``max_messages``）。

        Returns:
            消息列表，元素形如
            ``{"role": "...", "content": "...", "timestamp": ..., "metadata": {...}}``。
            Redis 不可用时返回空列表。
        """
        r = await self._get_redis()
        if r is None:
            return []

        key = self._key(session_id)
        count = limit or self.max_messages

        try:
            raw_messages = await r.lrange(key, -count, -1)
        except Exception as exc:
            logger.warning("ShortTermMemory.get_messages failed: %s", exc)
            self._redis = None
            return []

        messages: list[dict] = []
        for raw in raw_messages:
            try:
                messages.append(json.loads(raw))
            except json.JSONDecodeError:
                continue

        return messages

    async def get_context_string(self, session_id: str, limit: int = 10) -> str:
        """获取格式化的对话上下文字符串，用于注入 prompt。

        Returns:
            形如::

                [用户]: 帮我写一篇秋季穿搭笔记
                [助手]: 好的，我来为你生成...

            没有任何历史消息时返回空字符串。
        """
        messages = await self.get_messages(session_id, limit)
        if not messages:
            return ""

        role_map = {
            "user": "用户",
            "assistant": "助手",
            "system": "系统",
            "tool": "工具",
        }
        lines: list[str] = []
        for msg in messages:
            role_name = role_map.get(msg.get("role", ""), msg.get("role", ""))
            content = (msg.get("content") or "")[:200]  # 截断过长内容
            lines.append(f"[{role_name}]: {content}")

        return "\n".join(lines)

    async def clear_session(self, session_id: str) -> None:
        """清除指定会话的所有记忆。"""
        r = await self._get_redis()
        if r is None:
            return
        try:
            await r.delete(self._key(session_id))
        except Exception as exc:
            logger.warning("ShortTermMemory.clear_session failed: %s", exc)
            self._redis = None

    async def close(self) -> None:
        """关闭 Redis 连接，通常在 FastAPI shutdown 钩子中调用。"""
        if self._redis is not None:
            try:
                await self._redis.close()
            except Exception as exc:  # pragma: no cover - 防御性
                logger.warning("ShortTermMemory.close failed: %s", exc)
            finally:
                self._redis = None


# 全局单例
short_term_memory = ShortTermMemory()
