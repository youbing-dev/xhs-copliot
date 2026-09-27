# 墨西 AI - 小红书智能内容创作平台

基于 Java + Python 混合架构的 AI Agent 内容创作平台，集成 LangGraph 多步推理、RAG 检索增强、Tool Calling 工具调用等 Agent 核心能力，为小红书创作者提供智能内容生成服务。

## 技术架构

```
┌─────────────────────────────────────────────────────────┐
│                    Vue 3 Frontend                        │
│              (Vite + TypeScript + Pinia)                │
└─────────────────────┬───────────────────────────────────┘
                      │ SSE / REST
┌─────────────────────▼───────────────────────────────────┐
│              Java Spring Boot Backend                    │
│    ┌─────────┬──────────┬──────────┬─────────────┐      │
│    │  User   │ Billing  │ Content  │   Common    │      │
│    │ Module  │  Module  │  Module  │   Module    │      │
│    └─────────┴──────────┴──────────┴─────────────┘      │
└─────────────────────┬───────────────────────────────────┘
                      │ REST / SSE
┌─────────────────────▼───────────────────────────────────┐
│              Python Agent Service                       │
│    ┌──────────────────────────────────────────────┐     │
│    │         LangGraph Agent (Plan-Execute-Reflect)│     │
│    ├──────────────────────────────────────────────┤     │
│    │  Tools: title_gen | content_gen | tag_gen    │     │
│    │         humanize | trend_search | sensitive   │     │
│    ├──────────┬───────────────┬───────────────────┤     │
│    │   RAG    │    Memory     │   Observability   │     │
│    │ (Chroma) │ (Redis+Chroma)│  (Trace+Metrics)  │     │
│    └──────────┴───────────────┴───────────────────┘     │
└─────────────────────────────────────────────────────────┘
```

## 核心特性

### AI Agent 能力
- **LangGraph Plan-Execute-Reflect 循环** — LLM 驱动的动态任务规划，自动质量评估与重规划（最多3轮）
- **6 个 Agent Tools** — 标题生成、正文创作、标签推荐、去AI味改写、趋势检索、敏感词校验
- **RAG 检索增强** — DashScope Embedding + Chroma 向量检索，爆款笔记知识库辅助创作
- **双层 Memory 系统** — Redis 短期对话记忆 + Chroma 长期用户偏好记忆
- **SSE 流式交互** — 实时推送 Agent 思考过程、工具调用状态和生成内容
- **可观测性** — 完整执行链路追踪、Token/成本统计、质量评分分析

### 业务能力
- 用户注册/登录（JWT 鉴权）
- 会员计费系统（多档位套餐）
- 内容管理（CRUD + 版本历史）
- 敏感词过滤
- 用量配额管理
- 双链路容灾（Python Agent / Java 直调 LLM 可切换）

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Vite + Pinia |
| 后端 | Java 17 + Spring Boot 3 + MyBatis Plus + Redis |
| AI Agent | Python 3.11 + FastAPI + LangGraph + LangChain |
| 向量库 | Chroma |
| LLM | 通义千问 (DashScope OpenAI 兼容接口) |
| 数据库 | MySQL 8 |
| 容器化 | Docker + Docker Compose |

## 项目结构

```
.
├── moxi-backend/          # Java 后端服务
│   ├── moxi-ai/           # AI 模块（AgentClient + 原有直调链路）
│   ├── moxi-app/          # Spring Boot 启动入口 + 配置
│   ├── moxi-billing/      # 计费模块
│   ├── moxi-common/       # 公共模块（异常、响应、工具类）
│   ├── moxi-content/      # 内容模块（生成、管理、敏感词）
│   ├── moxi-user/         # 用户模块（注册、登录、JWT）
│   └── docker-compose.yml # 容器编排
├── moxi-agent/            # Python Agent 服务
│   ├── app/
│   │   ├── agent/         # LangGraph Agent 核心
│   │   ├── rag/           # RAG Pipeline
│   │   ├── memory/        # 双层记忆系统
│   │   ├── streaming/     # SSE 流式推送
│   │   ├── observability/ # 可观测性
│   │   └── api/           # FastAPI 路由
│   ├── data/              # 种子数据 + 向量库 + 追踪日志
│   ├── scripts/           # 工具脚本
│   └── tests/             # 测试
├── moxi-vue/              # Vue 3 前端
└── README.md
```

## 快速开始

### 环境要求
- JDK 17+
- Python 3.11+
- Node.js 18+
- MySQL 8
- Redis 7
- Docker & Docker Compose（可选）

### 1. 克隆项目
```bash
git clone https://github.com/your-username/xhs-copilot.git
cd xhs-copilot
```

### 2. 启动基础设施
```bash
cd moxi-backend
docker-compose up -d mysql redis
```

### 3. 启动 Python Agent 服务
```bash
cd moxi-agent
pip install -r requirements.txt
cp .env.example .env
# 编辑 .env，填入 DASHSCOPE_API_KEY

# 初始化 RAG 向量库
python -m scripts.index_seed

# 启动服务
python -m app.main
```

### 4. 启动 Java 后端
```bash
cd moxi-backend
# 配置 application.yml 中的数据库连接和 agent.python-service-url
mvn clean package -DskipTests
java -jar moxi-app/target/moxi-app-1.0-SNAPSHOT.jar
```

### 5. 启动前端
```bash
cd moxi-vue
npm install
npm run dev
```

### 一键 Docker 启动（推荐）
```bash
cd moxi-backend
docker-compose up -d
```

## API 文档

启动后端后访问：
- Java API: http://localhost:8080/swagger-ui.html
- Python Agent API: http://localhost:8001/docs

## 配置说明

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| DASHSCOPE_API_KEY | 通义千问 API Key | (必填) |
| agent.mode | Agent 链路模式 | python |
| agent.python-service-url | Python Agent 服务地址 | http://localhost:8001 |
| agent.internal-secret | 内部通信密钥 | moxi-internal-2024 |

## License

MIT License
