"""
Memory 系统 - 双层记忆架构

- :class:`ShortTermMemory` / ``short_term_memory``：
  Redis 短期对话记忆（滑动窗口，24h TTL）。
- :class:`LongTermMemory` / ``long_term_memory``：
  Chroma 长期用户偏好记忆（语义检索，按 user_id 隔离）。
"""

from app.memory.long_term import LongTermMemory, long_term_memory
from app.memory.short_term import ShortTermMemory, short_term_memory

__all__ = [
    "ShortTermMemory",
    "short_term_memory",
    "LongTermMemory",
    "long_term_memory",
]
