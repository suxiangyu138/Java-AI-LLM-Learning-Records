# 02 Docker 与容器化部署

> 开发/测试环境的事实标准：Docker Compose 一键拉起全部组件——模型服务、向量库、编排、数据库；容器健康检查三级验证是排障的第一武器。

## 📚 目录

1. [容器化部署的价值](#1-容器化部署的价值)
2. [服务清单与镜像选型](#2-服务清单与镜像选型)
3. [Docker Compose 编排示例](#3-docker-compose-编排示例)
4. [容器健康检查三级验证](#4-容器健康检查三级验证)
5. [资源分配与性能](#5-资源分配与性能)
6. [容器网络问题排查](#6-容器网络问题排查)
7. [面试高频问法](#7-面试高频问法)

## 1. 容器化部署的价值

### 为什么容器化

| 价值 | 说明 |
|---|---|
| 环境一致性 | 开发/测试/生产同镜像 |
| 组件解耦 | 每组件独立容器 |
| 快速启动 | 秒级拉起/销毁 |
| 资源隔离 | 每容器限制 CPU/内存 |
| 可复现 | docker-compose 文件即部署文档 |

### 与虚拟机的对比

| 维度 | 容器 | 虚拟机 |
|---|---|---|
| 开销 | 低（共享内核） | 高（完整 OS） |
| 启动 | 秒级 | 分钟级 |
| 隔离 | 进程级 | 完全隔离 |
| 适用 | 微服务/无状态 | 强隔离需求 |

## 2. 服务清单与镜像选型

### RAG 全套服务

| 服务 | 镜像（示例） | 用途 |
|---|---|---|
| 编排服务 | 自建（FastAPI） | RAG 链路编排 |
| Embedding | TEI（ghcr.io/huggingface/text-embeddings-inference） | 文本向量化 |
| 向量库 | qdrant/qdrant 或 milvusdb/milvus | 向量检索 |
| LLM 网关 | LiteLLM（ghcr.io/berriai/litellm） | 模型路由 |
| 数据库 | postgres:16 + pgvector | 元数据 |
| 缓存 | redis:7 | 答案/检索缓存 |
| 摄取 | 自建 worker | 异步索引 |
| 反向代理 | nginx | 统一入口 |

### 镜像选择原则

```
① 官方镜像优先（安全/更新及时）
② 锁定版本 tag（不用 latest，防漂移）
③ 轻量基础镜像（alpine 类，减小攻击面）
④ 镜像扫描（trivy 类，CI 内扫描漏洞）
```

## 3. Docker Compose 编排示例

```yaml
version: "3.8"
services:
  rag-api:                      # RAG 编排服务
    build: ./rag-service
    ports: ["8000:8000"]
    environment:
      - EMBEDDING_URL=http://tei:8080
      - QDRANT_URL=http://qdrant:6333
      - REDIS_URL=redis://redis:6379
    depends_on:
      - tei
      - qdrant
      - redis
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8000/health"]
      interval: 30s
      timeout: 5s
      retries: 3

  tei:                          # Embedding 服务（GPU）
    image: ghcr.io/huggingface/text-embeddings-inference:latest
    ports: ["8080:8080"]
    command: --model-id BAAI/bge-large-zh-v1.5 --max-batch-tokens 16384
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]

  qdrant:                       # 向量库
    image: qdrant/qdrant:v1.10
    ports: ["6333:6333"]
    volumes:
      - qdrant_data:/qdrant/storage

  redis:
    image: redis:7
    command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru

volumes:
  qdrant_data:
```

### Compose 编排要点

| 要点 | 说明 |
|---|---|
| depends_on | 控制启动顺序（不要依赖它做健康等待） |
| healthcheck | 每服务健康检查（编排平台依赖它） |
| 环境变量 | 服务间 URL 用服务名（tei/qdrant） |
| 数据卷 | 向量库/DB 数据必须持久化 |
| GPU 透传 | Embedding/LLM 容器声明 GPU 资源 |

## 4. 容器健康检查三级验证

### 三级验证（排障第一武器）

```
① 容器状态：docker ps / docker inspect 看 Health.Status
② 日志分析：docker logs 过滤 error/warn
③ API 验证：curl /health 返回 200
```

### 示例

```bash
# 第一级：容器状态
docker ps                          # 看 STATUS (healthy/unhealthy)
docker inspect --format='{{.State.Health.Status}}' rag-api

# 第二级：日志
docker logs --tail 100 rag-api
docker logs rag-api 2>&1 | grep -iE "error|exception"

# 第三级：接口
curl -s http://localhost:8000/health   # 期望 {"status":"ok"}
```

### 健康检查设计

| 探针类型 | 检查什么 | 失败后果 |
|---|---|---|
| 启动探针 | 服务是否启动完成 | 等待后再查就绪 |
| 就绪探针 | 是否可接受流量 | 从负载均衡摘除 |
| 存活探针 | 是否还活着 | 重启容器 |

## 5. 资源分配与性能

### 资源限制配置

```yaml
deploy:
  resources:
    limits:
      cpus: "2.0"          # 编排服务：2 核
      memory: 4G
```

### 资源分配建议

| 服务 | CPU | 内存 | 说明 |
|---|---|---|---|
| 编排服务 | 2 核 | 4G | 并发请求 |
| Embedding | GPU 1 卡 | 8G+ | 模型加载 |
| 向量库 | 2-4 核 | 内存为主 | HNSW 图 |
| Redis | 1 核 | 256MB-1G | 缓存 |
| 摄取 worker | 2 核 | 4G | 批量向量化 |

> ⚠️ 官方标注的"最低配置"只是容器勉强启动的理论下限——生产并发或大规模向量化时内存呈指数级增长，**按峰值 + 30% 余量规划**（04 篇容量计算）。

### 性能检查

| 检查 | 方法 |
|---|---|
| 容器资源占用 | docker stats |
| 检索延迟 | 压测工具（wrk/ghz） |
| 内存增长 | 长时间运行后看 RSS（防泄漏） |
| GPU 利用率 | nvidia-smi |

## 6. 容器网络问题排查

### 常见网络问题

| 问题 | 原因 | 解法 |
|---|---|---|
| 容器间连不上 | 服务名解析错误 | 用服务名而非 IP（Compose 内建 DNS） |
| 连到 127.0.0.1 | 应用与模型不同容器 | 用宿主机 IP 或服务名 |
| 跨主机不通 | 未配置服务发现 | K8s Service / Consul |
| 端口冲突 | 多容器映射同端口 | 改映射或只用内部网络 |

### 排查命令

```bash
# 容器内网络测试
docker exec rag-api curl http://tei:8080/health

# 网络模式检查
docker network ls
docker inspect rag-api --format='{{.NetworkSettings.Networks}}'

# 端口映射验证
docker port rag-api
```

### 网络设计建议

```
开发（Compose）：默认 bridge 网络，服务名互访
生产（K8s）：每命名空间独立网络策略（01 篇）
避免：依赖固定 IP 互访（重启即变）
```

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| 为什么容器化？ | 环境一致/解耦/秒级启动/资源隔离 |
| Compose 编排哪些服务？ | 编排/Embedding/向量库/DB/Redis/摄取 |
| 健康检查三级验证？ | 容器状态 → 日志 → API |
| 数据持久化怎么做？ | 数据卷（named volume）挂载 |
| GPU 容器怎么配？ | deploy.resources.devices 声明 |
| 容器间怎么互访？ | 服务名 DNS（Compose/K8s Service） |

### 面试加分表达

> "容器化我遵循三件事：官方镜像锁版本、每服务 healthcheck、数据卷持久化。排障先做三级验证——docker ps 看状态、logs 看日志、curl /health 验证接口，90% 的问题在这一步就能定位。"

## 8. 常见部署坑与自查

| 坑 | 现象 | 自查 |
|---|---|---|
| 用 latest 镜像 | 升级后行为突变 | 锁版本 tag |
| 数据没持久化 | 重启丢库 | 检查 volumes 挂载 |
| 依赖顺序裸奔 | 启动即崩 | 健康检查 + 等待就绪 |
| 容器时区/编码 | 日志时间错乱/中文乱码 | 环境变量统一（TZ/UTF-8） |
| 日志无轮转 | 磁盘打满 | 配置日志轮转/限制大小 |

### 部署后自检清单

| 项 | 检查 |
|---|---|
| 全服务 healthcheck | `docker ps` 全 healthy |
| 数据持久化 | 重启后数据仍在 |
| 服务互访 | 服务名可通（docker exec 内测） |
| 资源限制生效 | `docker stats` 看占用 |
| GPU 透传 | `nvidia-smi` 在容器内可见 |
| 日志可查 | `docker logs` 有结构化输出 |

> 🎯 核心要点：Docker Compose 是开发/测试环境的标准形态——服务清单（编排/Embedding/向量库/DB/Redis）+ 健康检查 + 数据卷持久化 + GPU 透传；排障用三级验证（状态→日志→接口）；网络互访用服务名而非 IP；生产规模再上 K8s（03 篇）；部署后跑六项自检清单。

---

**下一模块**：[03-Kubernetes生产部署](03-Kubernetes生产部署.md) / **返回总览**：[00-阶段总览与学习路径](00-阶段总览与学习路径.md)