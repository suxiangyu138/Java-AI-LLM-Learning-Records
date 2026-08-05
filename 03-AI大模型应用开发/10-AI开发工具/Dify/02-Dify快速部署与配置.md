# Dify 快速部署与配置

> 🛠️ Docker Compose 一键部署、源码开发模式、200+ 模型供应商配置、环境变量调优 —— 从零到上线 Dify 的完整运维手册

---

## 📚 目录

1. [Docker Compose 部署](#1-docker-compose-部署)
2. [源码部署（开发模式）](#2-源码部署开发模式)
3. [模型供应商配置](#3-模型供应商配置)
4. [环境变量与扩展配置](#4-环境变量与扩展配置)
5. [生产环境加固](#5-生产环境加固)

---

## 1. Docker Compose 部署

### 1.1 系统要求

| 资源 | 最低配置 | 推荐配置 |
|------|:---:|:---:|
| **CPU** | 2 核 | 4 核+ |
| **内存** | 4 GB (含向量数据库) | 8 GB+ |
| **磁盘** | 20 GB | 50 GB+ SSD |
| **Docker** | 20.10+ | 最新版 |
| **Docker Compose** | v2.0+ | 最新版 |
| **OS** | Linux / macOS / Windows (WSL2) | Ubuntu 22.04 LTS |

### 1.2 一键部署

```bash
# 1. 克隆 Dify 仓库
git clone https://github.com/langgenius/dify.git
cd dify/docker

# 2. 复制环境变量配置
cp .env.example .env

# 3. 编辑 .env 文件（详见下文）

# 4. 启动所有服务
docker compose up -d

# 5. 查看状态
docker compose ps

# 6. 查看日志
docker compose logs -f api      # API 服务
docker compose logs -f worker   # 异步任务
docker compose logs -f web      # 前端

# 7. 访问
# http://localhost:80 (默认端口)
# 首次访问会引导创建管理员账户
```

### 1.3 Docker Compose 服务清单

```yaml
# docker-compose.yaml 核心服务
services:
  api:              # Python Flask API 服务
  worker:           # Celery 异步任务
  web:              # Next.js 前端界面
  db:               # PostgreSQL 15 数据库
  redis:            # Redis 7 缓存 + 消息队列
  weaviate:         # Weaviate 向量数据库（可选）
  nginx:            # Nginx 反向代理
  sandbox:          # 代码沙箱（代码节点执行环境）
  ssrf_proxy:       # SSRF 防护代理
```

### 1.4 关键 .env 配置

```bash
# ==========================================
# 基础配置
# ==========================================
# 部署模式
DEPLOY_ENV=PRODUCTION

# 访问地址
CONSOLE_API_URL=http://localhost
CONSOLE_WEB_URL=http://localhost
SERVICE_API_URL=http://localhost/api

# 管理员初始化密码（首次启动后自动创建）
INIT_PASSWORD=your_secure_password

# ==========================================
# 数据库配置
# ==========================================
DB_USERNAME=postgres
DB_PASSWORD=difyai123456
DB_HOST=db
DB_PORT=5432
DB_DATABASE=dify

# ==========================================
# Redis 配置
# ==========================================
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=difyai123456

# ==========================================
# 向量数据库（四选一）
# ==========================================
# 方式 1: Weaviate（默认，内置容器）
VECTOR_STORE=weaviate
WEAVIATE_ENDPOINT=http://weaviate:8080

# 方式 2: Qdrant
# VECTOR_STORE=qdrant
# QDRANT_URL=http://qdrant:6333

# 方式 3: Milvus
# VECTOR_STORE=milvus
# MILVUS_HOST=milvus

# 方式 4: pgvector (PostgreSQL 扩展，最轻量)
# VECTOR_STORE=pgvector

# ==========================================
# 文件存储
# ==========================================
# 本地存储（默认）
STORAGE_TYPE=local
STORAGE_LOCAL_PATH=storage

# 对象存储（S3/MinIO）
# STORAGE_TYPE=s3
# S3_ENDPOINT=https://s3.amazonaws.com
# S3_BUCKET_NAME=dify-storage
# S3_ACCESS_KEY=xxx
# S3_SECRET_KEY=xxx
```

### 1.5 启动后初始化

```text
1. 浏览器打开 http://localhost
2. 首次进入 → 设置管理员邮箱和密码
3. 进入设置页面（右上角头像 → 设置）
4. 配置模型供应商（关键步骤！）
5. 开始创建应用
```

---

## 2. 源码部署（开发模式）

### 2.1 后端（API + Worker）

```bash
# 前提条件
# - Python 3.11+
# - Poetry (Python 包管理)
# - Node.js 18+ (前端)

# 1. 克隆仓库
git clone https://github.com/langgenius/dify.git
cd dify

# 2. 后端 API
cd api
cp .env.example .env
# 编辑 .env 配置数据库/Redis/向量数据库连接

# 3. 安装依赖
poetry install

# 4. 数据库迁移
poetry run flask db upgrade

# 5. 启动 API 服务
poetry run flask run --host 0.0.0.0 --port 5001

# 6. 另开终端，启动 Worker
poetry run celery -A app.celery worker -Q dataset,generation,mail,ops_trace -l INFO
```

### 2.2 前端

```bash
cd dify/web

# 安装依赖
npm install

# 配置环境变量 .env.local
NEXT_PUBLIC_API_PREFIX=http://localhost:5001/console/api
NEXT_PUBLIC_PUBLIC_API_PREFIX=http://localhost:5001/api

# 启动开发服务器
npm run dev
# → http://localhost:3000
```

---

## 3. 模型供应商配置

### 3.1 支持的模型供应商一览

```text
Dify 支持 200+ 模型供应商，分类如下：

🔥 国际主流：
├── OpenAI (GPT-4o, GPT-4.1, o4-mini...)
├── Anthropic (Claude Opus 5, Sonnet 5, Haiku 4.5...)
├── Google (Gemini 2.5 Pro/Flash...)
├── Meta (Llama 3.1/3.2/4...)
├── Mistral (Large, Small, Codestral...)
└── Cohere (Command R+, Embed, Rerank...)

🇨🇳 国内主流：
├── DeepSeek (V4 Pro, V4, R1, V3)
├── 通义千问 (Qwen-Max, Qwen-Plus, Qwen-Turbo)
├── 智谱 AI (GLM-4-Plus, GLM-4-Flash)
├── 月之暗面 (Moonshot-v1)
├── 百川 (Baichuan4)
├── 零一万物 (Yi-Large, Yi-Vision)
└── MiniMax (abab6.5s)

🖥️ 本地/开源：
├── Ollama (Llama3.2, Qwen2.5, DeepSeek-R1...)
├── Xinference (本地模型推理平台)
├── LocalAI (OpenAI 兼容的本地 API)
├── vLLM (高性能推理引擎)
└── OpenRouter (聚合 API 网关)
```

### 3.2 配置示例

#### OpenAI

```text
设置 → 模型供应商 → OpenAI → 安装

配置：
├── API Key: sk-xxxxxxxxxxxxxxxxxxxx
├── Organization ID: (可选，组织账户需要)
└── API Base URL: (默认 https://api.openai.com/v1)

可用模型：
├── gpt-4o (推荐) — 多模态旗舰
├── gpt-4.1 — 最大上下文 1M
├── o4-mini — 推理模型，性价比高
├── text-embedding-3-small — Embedding (便宜)
└── text-embedding-3-large — Embedding (精度高)
```

#### DeepSeek

```text
设置 → 模型供应商 → DeepSeek → 安装

配置：
├── API Key: sk-xxxxxxxxxxxxxxxxxxxx
│   获取地址: https://platform.deepseek.com
└── API Base URL: https://api.deepseek.com

可用模型：
├── deepseek-v4-pro — 最强旗舰 1.6T MoE
├── deepseek-v4 — 通用主力
├── deepseek-r1 — 深度推理
└── deepseek-chat — 通用对话
```

#### Ollama 本地模型

```text
设置 → 模型供应商 → Ollama → 安装

配置：
├── Base URL: http://host.docker.internal:11434
│   (Dify 容器访问宿主机 Ollama)
│   或 http://localhost:11434 (源码部署)
│
└── 模型名称：须先在宿主机拉取
    ollama pull llama3.2
    ollama pull qwen2.5:14b
```

### 3.3 模型配置最佳实践

```text
💡 四类模型的配置建议：

┌─────────────────────────────────────────────────────┐
│ 功能             模型选择                    成本    │
├─────────────────────────────────────────────────────┤
│ 系统推理 LLM    GPT-4o / DeepSeek-V4-Pro    高     │
│ 简单对话 LLM    GPT-4o-mini / DeepSeek-V4   低     │
│ Embedding       text-embedding-3-small       极低   │
│ Rerank          bge-reranker-v2-m3           免费   │
│ 本地开发/测试   Ollama + qwen2.5:7b          免费   │
└─────────────────────────────────────────────────────┘
```

---

## 4. 环境变量与扩展配置

### 4.1 邮箱服务（SMTP）

```bash
# 用于用户注册、密码重置等
MAIL_TYPE=smtp
MAIL_DEFAULT_SEND_FROM=dify@yourdomain.com
SMTP_SERVER=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your@gmail.com
SMTP_PASSWORD=your_app_password
SMTP_USE_TLS=true
```

### 4.2 对象存储（S3/MinIO）

```bash
# 用于上传的文档、图片等文件存储
STORAGE_TYPE=s3
S3_ENDPOINT=https://s3.amazonaws.com
S3_BUCKET_NAME=dify-files
S3_ACCESS_KEY=AKIAXXXXXXXXXXXX
S3_SECRET_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxx
S3_REGION=us-east-1
```

### 4.3 SSO / OAuth

```bash
# GitHub OAuth
GITHUB_CLIENT_ID=xxx
GITHUB_CLIENT_SECRET=xxx

# Google OAuth
GOOGLE_CLIENT_ID=xxx
GOOGLE_CLIENT_SECRET=xxx

# 企业微信
WECOM_CORP_ID=xxx
WECOM_AGENT_ID=xxx
WECOM_SECRET=xxx
```

---

## 5. 生产环境加固

### 5.1 安全检查清单

```text
□ 修改所有默认密码（PostgreSQL、Redis、管理员账户）
□ .env 文件权限设为 600
□ 配置 HTTPS（Nginx 反向代理 + Let's Encrypt）
□ 启用 API 鉴权（SECRET_KEY 使用随机长字符串）
□ 限制 SSRF 白名单（SSRF_PROXY_WHITELIST）
□ 配置日志轮转（LOG_FILE_MAX_BYTES + LOG_FILE_BACKUP_COUNT）
□ 定期备份 PostgreSQL + 向量数据库
□ 启用 Web 密码复杂度策略
□ 配置请求频率限制
□ 移除不需要的模型供应商
```

### 5.2 性能优化

```bash
# Worker 并发数（根据 CPU 核数调整）
CELERY_WORKER_CONCURRENCY=4

# 数据库连接池
SQLALCHEMY_POOL_SIZE=30
SQLALCHEMY_POOL_RECYCLE=3600

# 上传文件大小限制（默认 15MB）
UPLOAD_FILE_SIZE_LIMIT=15

# 知识库文档大小限制
ETL_TYPE_MAX_SEGMENTS=2000
MAX_TOKENS_IN_EMBEDDING=8192
```

### 5.3 监控与日志

```bash
# 改用 Gunicorn 生产模式（Docker 默认）
# API 服务启动命令：
gunicorn app:app \
    -b 0.0.0.0:5001 \
    --workers 4 \
    --worker-class gevent \
    --timeout 360 \
    --log-level info \
    --access-logfile /var/log/dify/access.log \
    --error-logfile /var/log/dify/error.log
```

---

**上一模块**：[01-Dify平台概述与架构](./01-Dify平台概述与架构.md) ｜ **下一模块**：[03-Dify应用构建：提示词编排](./03-Dify应用构建：提示词编排.md) ｜ **返回总览**：[00-Dify知识体系总览](./00-Dify知识体系总览.md)

---

*创建于：2026年7月*
