# 07 - LangGraph Platform 与生产部署

> LangGraph Platform 将你本地开发的 Graph 变成可水平扩展、带持久化、有可视化调试的生产级 API 服务——LangChain 生态从"库"到"平台"的跨越。

---

## 📚 目录

1. [LangGraph Platform 全景](#1-langgraph-platform-全景)
2. [LangGraph Server：自建 API 服务](#2-langgraph-server自建-api-服务)
3. [LangGraph Cloud：SaaS 托管](#3-langgraph-cloudsaas-托管)
4. [LangGraph Studio：可视化调试](#4-langgraph-studio可视化调试)
5. [Assistant API：开箱即用 Agent](#5-assistant-api开箱即用-agent)
6. [自建部署方案对比](#6-自建部署方案对比)
7. [生产环境 Checklist](#7-生产环境-checklist)
8. [性能调优](#8-性能调优)

---

## 1. LangGraph Platform 全景

```text
LangGraph Platform 四个组件：

┌──────────────────────────────────────────────────────┐
│              LangGraph Platform                       │
│                                                      │
│  ┌────────────────┐    ┌──────────────────────┐     │
│  │ LangGraph Cloud│    │  LangGraph Server     │     │
│  │ (SaaS 托管)    │    │  (自建/Docker/K8s)    │     │
│  └────────────────┘    └──────────────────────┘     │
│           │                       │                  │
│           └───────────┬───────────┘                  │
│                       ▼                              │
│         ┌─────────────────────────┐                 │
│         │    LangGraph API        │                 │
│         │  /threads, /runs,       │                 │
│         │  /assistants, /stream   │                 │
│         └─────────────────────────┘                 │
│                       │                              │
│           ┌───────────┴───────────┐                 │
│           ▼                       ▼                  │
│  ┌────────────────┐    ┌──────────────────────┐     │
│  │ LangSmith      │    │  LangGraph Studio     │     │
│  │ (可观测性)     │    │  (可视化调试)          │     │
│  └────────────────┘    └──────────────────────┘     │
└──────────────────────────────────────────────────────┘
```

---

## 2. LangGraph Server：自建 API 服务

### 2.1 是什么

```text
LangGraph Server = 把你的 Graph 包装成 HTTP API 服务
内置：
  ✅ REST API（Threads, Runs, Streaming）
  ✅ Checkpoint 管理（PostgresSaver 自动集成）
  ✅ 水平扩展（多实例共享 Postgres）
  ✅ 异步任务（长时间运行的后台任务）
  ✅ CORS / Auth（可配置）
```

### 2.2 项目结构

```text
my-agent/
├── langgraph.json          ← Server 配置
├── requirements.txt
├── .env
├── agent/
│   ├── __init__.py
│   ├── graph.py            ← 编译好的 Graph
│   └── state.py            ← State Schema
└── tests/
```

### 2.3 langgraph.json 配置

```json
{
  "dependencies": ["."],
  "graphs": {
    "agent": "./agent/graph.py:graph",
    "code_reviewer": "./agent/code_review.py:graph"
  },
  "env": ".env",
  "python_version": "3.12",
  "dockerfile_lines": [
    "RUN apt-get update && apt-get install -y curl"
  ]
}
```

```python
# agent/graph.py
from agent.state import AgentState

# 导出编译好的 graph（变量名与 langgraph.json 中一致）
graph = build_agent_graph().compile(
    checkpointer=PostgresSaver.from_conn_string(
        os.environ["DATABASE_URL"]
    )
)
```

### 2.4 启动方式

```bash
# 方式 1：CLI 启动（开发/测试）
pip install langgraph-cli
langgraph dev --port 8123

# 方式 2：Docker 启动（生产）
docker build -t my-agent .
docker run -p 8123:8000 \
  -e DATABASE_URL=postgresql://... \
  my-agent

# 方式 3：Docker Compose
# docker-compose.yml
version: '3.8'
services:
  agent:
    build: .
    ports:
      - "8123:8000"
    environment:
      - DATABASE_URL=postgresql://user:pass@db:5432/langgraph
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    depends_on:
      - db

  db:
    image: postgres:16
    environment:
      - POSTGRES_USER=user
      - POSTGRES_PASSWORD=pass
      - POSTGRES_DB=langgraph
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

### 2.5 API 端点

```text
POST   /threads                          ← 创建会话
GET    /threads/{thread_id}              ← 获取会话状态
GET    /threads/{thread_id}/state        ← 获取最新 Checkpoint
POST   /threads/{thread_id}/runs/stream  ← 流式执行
POST   /threads/{thread_id}/runs/wait    ← 同步执行
GET    /runs/{run_id}                    ← 查询任务状态
POST   /threads/{thread_id}/runs/{run_id}/cancel  ← 取消任务
```

```bash
# 使用示例
# 1. 创建 Thread
curl -X POST http://localhost:8123/threads \
  -H "Content-Type: application/json" \
  -d '{"thread_id": "user-123"}'

# 2. 流式执行
curl -X POST http://localhost:8123/threads/user-123/runs/stream \
  -H "Content-Type: application/json" \
  -d '{
    "assistant_id": "agent",
    "input": {
      "messages": [{"role": "user", "content": "你好"}]
    }
  }'

# 3. 查看状态
curl http://localhost:8123/threads/user-123/state

# 4. 继续对话（在同一 Thread 中）
curl -X POST http://localhost:8123/threads/user-123/runs/stream \
  -H "Content-Type: application/json" \
  -d '{
    "assistant_id": "agent",
    "input": {
      "messages": [{"role": "user", "content": "刚才说了什么？"}]
    }
  }'
```

---

## 3. LangGraph Cloud：SaaS 托管

### 3.1 是什么

```text
LangGraph Cloud = LangGraph Server 的托管版本
  ✅ 一键部署（从 GitHub 仓库）
  ✅ 自动缩放
  ✅ 内置监控（LangSmith 集成）
  ✅ 无服务器（按用量计费）
  ✅ 内置认证

适合：
  → 不想自己运维数据库和服务器
  → 快速验证 Agent 产品
  → 中小规模（大规模自建更经济）
```

### 3.2 部署流程

```bash
# 1. 安装 LangGraph CLI
pip install langgraph-cli

# 2. 初始化项目（如有必要）
langgraph new my-agent

# 3. 部署到 Cloud
langgraph deploy \
  --api-key ls__your_langsmith_key \
  --github-repo your-username/my-agent
```

---

## 4. LangGraph Studio：可视化调试

### 4.1 功能

```text
LangGraph Studio 可以：
  ✅ 可视化 Graph 结构（节点、边、条件分支）
  ✅ 实时查看每个 Node 的输入/输出
  ✅ 查看 Checkpoint 历史
  ✅ 手动触发节点执行（Step-through）
  ✅ Human-in-the-Loop 调试
  ✅ LLM Token 和延迟分析
```

### 4.2 启动

```bash
# 本地启动 Studio（连接到运行的 LangGraph Server）
langgraph dev --studio-port 8124

# 或者使用 LangSmith 托管的 Studio
# https://smith.langchain.com/studio
```

### 4.3 使用流程

```text
1. 打开 Studio → 看到你的 Graph 结构图
2. 输入测试消息
3. 点击"Step"逐步执行
   → 观察 agent node 的输入：(HumanMessage)
   → 观察 agent node 的输出：(AIMessage with tool_calls)
   → 观察路由决策：→ tools
   → 观察 tools node 的输入/输出
   → 观察回到 agent node
4. 随时可以查看任意 Checkpoint 的 State 快照
```

---

## 5. Assistant API：开箱即用 Agent

### 5.1 概念

```text
LangGraph Assistant API = 在 LangGraph Server 之上的更高层封装

内置：
  ✅ 多 Assistant 管理（一个 Server 跑多个 Agent）
  ✅ Assistant 配置（模型、工具、Prompt 模板）
  ✅ 版本管理（Assistant v1, v2...）
  ✅ 内置 Tool 注册
```

### 5.2 配置 Assistant

```python
# agent/graph.py — 导出多个 Assistant
from langgraph.prebuilt import create_react_agent

# Assistant 1：通用 Agent
general_agent = create_react_agent(
    llm, tools=[search, calculator]
)

# Assistant 2：代码审查 Agent（专业配置）
code_reviewer = create_react_agent(
    llm_code,
    tools=[read_file, run_linter, suggest_fix],
    state_modifier=SystemMessage(content="""
        你是专业的代码审查员。审查代码时关注：
        1. 安全隐患
        2. 性能问题
        3. 最佳实践偏离
        4. 潜在 Bug
    """)
)
```

```json
// langgraph.json
{
  "graphs": {
    "general": "./agent/graph.py:general_agent",
    "code-reviewer": "./agent/graph.py:code_reviewer"
  }
}
```

---

## 6. 自建部署方案对比

### 6.1 四种部署架构

```text
架构 1：单机部署（最小可行）
┌─────────────────────┐
│   Docker Container   │
│  ┌─────────────────┐│
│  │ LangGraph Server ││
│  └────────┬────────┘│
│  ┌────────▼────────┐│
│  │  SQLite /本地PG  ││
│  └─────────────────┘│
└─────────────────────┘
适合：内部工具、小规模 POC

架构 2：Docker Compose（小中型生产）
┌──────────────┐   ┌──────────────┐
│ LangGraph Svr│   │ LangGraph Svr│  ← 多实例（可选）
│  Instance 1  │   │  Instance 2  │
└──────┬───────┘   └──────┬───────┘
       └────────┬─────────┘
         ┌──────▼──────┐
         │ PostgreSQL  │   ← 共享数据库
         └─────────────┘
适合：小型生产环境

架构 3：Kubernetes（中大型生产）
┌─────────────────────────────────────┐
│  Kubernetes Cluster                  │
│                                      │
│  ┌─────────────────────────────┐    │
│  │ LangGraph Server Deployment  │    │
│  │  replicas: 3-10              │    │
│  │  HPA: CPU > 70% → scale     │    │
│  └──────────┬──────────────────┘    │
│             │                       │
│  ┌──────────▼──────────────────┐    │
│  │ PostgreSQL (HA)              │    │
│  │  Primary + Replica           │    │
│  └─────────────────────────────┘    │
│                                      │
│  ┌─────────────────────────────┐    │
│  │ Redis (缓存 + 会话)          │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
适合：面向用户的生产环境

架构 4：混合架构（Java 前置）
┌──────────┐   ┌──────────────┐   ┌──────────────┐
│  Nginx   │──▶│  Spring Boot │──▶│ LangGraph Svr│
│  (LB)    │   │  (业务逻辑)   │   │  (Agent编排)  │
└──────────┘   └──────────────┘   └──────┬───────┘
                                    ┌────▼────┐
                                    │Postgres │
                                    │ (共享)   │
                                    └─────────┘
适合：Java 技术栈团队的最佳选择
```

### 6.2 K8s 部署示例

```yaml
# k8s/langgraph-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: langgraph-server
spec:
  replicas: 3
  selector:
    matchLabels:
      app: langgraph-server
  template:
    metadata:
      labels:
        app: langgraph-server
    spec:
      containers:
      - name: langgraph
        image: my-agent:latest
        ports:
        - containerPort: 8000
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: langgraph-secrets
              key: database-url
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: langgraph-secrets
              key: openai-api-key
        - name: LANGCHAIN_API_KEY
          valueFrom:
            secretKeyRef:
              name: langgraph-secrets
              key: langsmith-api-key
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /ok
            port: 8000
          initialDelaySeconds: 30
        readinessProbe:
          httpGet:
            path: /ok
            port: 8000
          initialDelaySeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: langgraph-server
spec:
  selector:
    app: langgraph-server
  ports:
  - port: 8000
    targetPort: 8000
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: langgraph-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: langgraph-server
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

---

## 7. 生产环境 Checklist

### 7.1 部署前检查

```text
□ 数据持久化
  □ PostgresSaver 已配置（非 MemorySaver）
  □ 数据库已设置备份策略
  □ Migration 脚本已测试

□ 安全
  □ API Key 使用 K8s Secret / Vault（非 .env 文件）
  □ 所有外部调用通过 HTTPS
  □ 输入验证（防止 Prompt Injection）
  □ Rate Limiting 已配置
  □ 审计日志已启用

□ 可靠性
  □ 多副本部署（≥ 2）
  □ Readiness / Liveness Probe 已配置
  □ 健康检查端点可访问
  □ Graceful Shutdown 已测试

□ 可观测性
  □ LangSmith 追踪已开启
  □ Prometheus Metrics 已配置
  □ Grafana Dashboard 已创建
  □ 告警规则已配置（错误率、延迟、成本暴涨）

□ 性能
  □ LLM 调用的 timeout 已设置
  □ Prompt Cache 已优化
  □ 最大迭代次数已设置（防止死循环）
  □ Token 上限已配置
```

### 7.2 安全加固

```python
# 在 LangGraph Server 中添加中间件
from fastapi import FastAPI, Request, HTTPException
import time
from collections import defaultdict

app = FastAPI()

# 简易 Rate Limiter
rate_limit_store = defaultdict(list)

@app.middleware("http")
async def rate_limiter(request: Request, call_next):
    client_ip = request.client.host
    now = time.time()
    # 每分钟 60 次
    rate_limit_store[client_ip] = [
        t for t in rate_limit_store[client_ip] if now - t < 60
    ]
    if len(rate_limit_store[client_ip]) > 60:
        raise HTTPException(429, "Too Many Requests")
    rate_limit_store[client_ip].append(now)
    return await call_next(request)

# Input 长度限制
@app.middleware("http")
async def input_size_limit(request: Request, call_next):
    if request.headers.get("content-length"):
        content_length = int(request.headers["content-length"])
        if content_length > 1_000_000:  # 1MB 限制
            raise HTTPException(413, "Request too large")
    return await call_next(request)
```

---

## 8. 性能调优

### 8.1 关键调优点

```text
调优维度 1：LLM 调用
  ├── 使用 streaming: true（减少 TTFT）
  ├── 设置合理的 max_tokens（避免浪费）
  ├── Prompt Cache：固定前缀放最前面
  └── 选择合适的模型（小模型做简单判断）

调优维度 2：Graph 结构
  ├── 避免过深嵌套（subgraph 最多 3 层）
  ├── 能用 Send API 就用 Send（并行优于串行）
  ├── 减少不必要的 State 字段（序列化开销）
  └── interrupt() 只在必要节点使用（暂停有开销）

调优维度 3：Checkpointer
  ├── Postgres 连接池（max_connections: 20-50）
  ├── Checkpoint 保留策略（定期清理旧 checkpoint）
  └── 避免在 hot path 做 checkpoint

调优维度 4：部署
  ├── LangGraph Server 实例数 = CPU 核心数 * 2
  ├── 使用 uvicorn workers（而非单进程）
  └── 就近部署 LLM 推理（降低网络延迟）
```

### 8.2 Checkpoint 清理策略

```python
# 定期清理旧 Checkpoint
async def cleanup_old_checkpoints(checkpointer, retention_days=7):
    """
    保留最近 7 天的 checkpoint，删除更旧的
    防止 Postgres 无限膨胀
    """
    cutoff = datetime.utcnow() - timedelta(days=retention_days)

    async with checkpointer.conn() as conn:
        await conn.execute("""
            DELETE FROM checkpoint_blobs
            WHERE thread_id IN (
                SELECT thread_id FROM checkpoint_writes
                WHERE created_at < $1
            )
        """, cutoff)
        await conn.execute("""
            DELETE FROM checkpoint_writes
            WHERE created_at < $1
        """, cutoff)
        await conn.execute("""
            DELETE FROM checkpoints
            WHERE created_at < $1
        """, cutoff)

# 可以放到 cron job 或 K8s CronJob
```

### 8.3 生产级 uvicorn 配置

```bash
# LangGraph Server 的生产启动命令
uvicorn app:app \
  --host 0.0.0.0 \
  --port 8000 \
  --workers 4 \            # CPU 核心数
  --loop uvloop \          # 高性能事件循环
  --http httptools \       # 高性能 HTTP 解析
  --log-level warning \    # 减少日志量
  --no-access-log          # 关闭访问日志（用 LB 日志代替）
```

---

> 🎯 **核心要点**：LangGraph Server 是生产部署的最优解——内置 REST API + Checkpoint 管理 + 水平扩展能力。对于 Java 团队，推荐架构 4（Spring Boot → LangGraph Server），Java 负责业务和权限，Python 负责 Agent 编排。PostgresSaver 是唯一的生产级 Checkpointer 选择。

---

**返回总览**：[00 - LangGraph 知识体系总览](./00-LangGraph知识体系总览.md)
