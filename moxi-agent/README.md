# Moxi Agent - 小红书 AI 内容创作 Agent

基于 LangGraph 的智能内容生成 Agent 服务，具备工具调用、多步推理、RAG 检索增强和流式交互能力。

## 技术栈

- **Web 框架**: FastAPI
- **Agent 框架**: LangGraph (Plan-Execute-Reflect 循环)
- **LLM**: 通义千问 (DashScope OpenAI 兼容接口)
- **向量库**: Chroma (RAG + 长期记忆)
- **缓存**: Redis (短期对话记忆)
- **可观测性**: 自定义 Trace + LangSmith (可选)

## 架构

```
用户请求 → FastAPI → LangGraph Agent
                         │
                    ┌────┴────┐
                    │ retrieve │ ← Memory + RAG 上下文
                    └────┬────┘
                         │
                    ┌────┴────┐
                    │ planner  │ ← LLM 生成执行计划
                    └────┬────┘
                         │
                    ┌────┴────┐
              ┌────►│ executor │ ← 工具调用
              │     └────┬────┘
              │          │
              │     route_check
              │     │        │
              └─────┘   ┌───┴────┐
                        │reflector│ ← 质量评估
                        └───┬────┘
                            │
                       ┌────┴────┐
                       │responder │ ← 组装输出
                       └─────────┘
```

## 快速开始

### 环境要求
- Python 3.11+
- Redis 7+
- DashScope API Key

### 安装

```bash
cd moxi-agent
pip install -r requirements.txt
cp .env.example .env
# 编辑 .env 填入你的 DASHSCOPE_API_KEY
```

### 初始化 RAG 索引

```bash
python -m scripts.index_seed
```

### 启动服务

```bash
# 开发模式（热重载）
python -m app.main

# 或使用 uvicorn
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

### Docker 启动

```bash
docker-compose up -d
```

## API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /health | 健康检查 |
| GET | /metrics | 运行指标 |
| GET | /traces | 执行追踪记录 |
| POST | /api/agent/generate | 内容生成（SSE 流式） |
| POST | /api/agent/generate/sync | 内容生成（同步） |
| POST | /api/agent/chat | 对话交互（SSE 流式） |

## Agent Tools

| 工具 | 功能 |
|------|------|
| title_gen | 生成小红书爆款标题（带评分） |
| content_gen | 生成口语化笔记正文 |
| tag_gen | 生成话题标签 |
| humanize | 去AI味改写 |
| trend_search | RAG 检索爆款笔记参考 |
| sensitive_check | 敏感词校验（回调 Java 服务） |

## 项目结构

```
moxi-agent/
├── app/
│   ├── main.py              # FastAPI 入口
│   ├── config.py            # 配置管理
│   ├── api/                 # API 路由和模型
│   ├── agent/               # LangGraph Agent 核心
│   │   ├── graph.py         # 状态图定义
│   │   ├── state.py         # Agent 状态
│   │   ├── llm.py           # LLM 工厂
│   │   ├── nodes/           # 图节点（planner/executor/reflector/responder）
│   │   └── tools/           # Agent 工具集
│   ├── memory/              # 双层记忆系统
│   ├── rag/                 # RAG Pipeline
│   ├── streaming/           # SSE 流式推送
│   └── observability/       # 可观测性
├── data/
│   ├── seed_notes/          # RAG 种子数据
│   ├── chroma/              # 向量库持久化
│   └── traces/              # 执行追踪日志
├── scripts/                 # 工具脚本
└── tests/                   # 测试
```

## 运行测试

```bash
pytest tests/ -v
```

## 配置说明

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| DASHSCOPE_API_KEY | - | 通义千问 API Key（必填） |
| LLM_MODEL | qwen-plus | LLM 模型名 |
| REDIS_URL | redis://localhost:6379/1 | Redis 连接地址 |
| CHROMA_PERSIST_DIR | ./data/chroma | Chroma 持久化目录 |
| JAVA_SERVICE_URL | http://localhost:8080 | Java 后端服务地址 |
| INTERNAL_SECRET | moxi-internal-2024 | 内部通信密钥 |
| TRACE_ENABLED | true | 是否启用执行追踪 |
