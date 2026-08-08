# 07 Proxy 生产实践
> 从 Demo 到生产：Docker 部署、数据库、可观测、数据面/控制面分离与高可用

## 📚 目录
1. [生产架构总览](#1-生产架构总览)
2. [Docker 部署](#2-docker-部署)
3. [数据库：Postgres 与记账](#3-数据库postgres-与记账)
4. [可观测：日志/指标/追踪](#4-可观测日志指标追踪)
5. [数据面/控制面分离（2026）](#5-数据面控制面分离2026)
6. [高可用与扩容](#6-高可用与扩容)
7. [版本管理与升级](#7-版本管理与升级)
8. [常见坑](#8-常见坑)
9. [核心要点](#9-核心要点)

---

## 1. 生产架构总览

```text
生产形态（推荐）：

        ┌──────────────────────────┐
        │   Nginx（TLS + 限流）     │
        └───────────┬──────────────┘
       ┌────────────┴────────────┐
       │  LiteLLM Proxy 实例 1   │  数据面（请求处理）
       │  LiteLLM Proxy 实例 2   │  ← 水平扩展（无状态）
       └──────┬─────────────┬────┘
              │             │
     ┌────────▼────┐  ┌─────▼─────────┐
     │ PostgreSQL  │  │ Redis（缓存）  │
     │ 记账/key/团队│  │ 缓存/锁        │
     └─────────────┘  └───────────────┘
              │
        ┌─────▼─────┐
        │ 供应商 API │  （真实 key 只在此层/网关环境变量）
        └───────────┘
```

| 组件 | 职责 |
|------|------|
| Nginx | TLS 终止 + 限流 + 负载均衡 |
| LiteLLM Proxy | 路由/虚拟 key/记账（无状态可扩展） |
| PostgreSQL | key/团队/花费持久化 |
| Redis | 响应缓存/速率计数 |
| 监控 | OTel 指标 + 日志 |

> 🎯 **核心要点**：**Proxy 无状态 + 数据库持久化 = 可水平扩展**。生产形态 = "Nginx 入口 + 多实例 + PG + Redis"——与 FastAPI 体系生产架构同构（09 章），治理逻辑在网关，状态在数据库。

## 2. Docker 部署

```dockerfile
# Dockerfile
FROM ghcr.io/berriai/litellm:main-v1.94.0       # 锁定版本镜像
COPY config.yaml /app/config.yaml
CMD ["--config", "/app/config.yaml", "--port", "4000"]
```

```yaml
# docker-compose.yml
services:
  litellm:
    image: ghcr.io/berriai/litellm:main-v1.94.0
    ports: ["4000:4000"]
    environment:
      - LITELLM_MASTER_KEY=${LITELLM_MASTER_KEY}      # 主 key（环境变量！）
      - DATABASE_URL=postgresql://litellm:pass@postgres:5432/litellm
      - REDIS_URL=redis://redis:6379/0
      - OPENAI_API_KEY=${OPENAI_API_KEY}              # 供应商真实 key
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
      - DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY}
    volumes:
      - ./config.yaml:/app/config.yaml:ro
    depends_on: [postgres, redis]
    healthcheck:
      test: ["CMD", "curl", "-sf", "http://localhost:4000/health/liveliness"]
      interval: 10s
      timeout: 3s
      retries: 3

  postgres:
    image: postgres:16
    environment: {POSTGRES_PASSWORD: ${DB_PASSWORD}}
    volumes: ["pg-data:/var/lib/postgresql/data"]     # ⚠️ 持久化

  redis:
    image: redis:8
    volumes: ["redis-data:/data"]
```

| Docker 化要点 | 说明 |
|--------------|------|
| 镜像锁定版本 | 不 latest（升级破坏） |
| 密钥环境变量 | master/供应商 key 全走 env |
| 数据库持久化 | PG 数据卷（key/记账不能丢） |
| 健康检查 | readiness 探针（编排用） |
| 配置只读挂载 | config.yaml 不可变（改配置重新部署） |

> ⚠️ **config.yaml 里不写密钥**——全部环境变量引用（`os.environ/OPENAI_API_KEY`）。配置文件可以进 git，密钥永远不进。

## 3. 数据库：Postgres 与记账

```bash
# 首次启动自动建表（Prisma 迁移）
# 数据库管什么：
#   Virtual Keys（key/预算/归属）
#   Spend Logs（每请求花费）
#   Teams/Users（团队与用户）
#   Budget（预算状态）

# 验证记账
curl "http://localhost:4000/spend/logs?limit=5" \
  -H "Authorization: Bearer sk-master-xxx"
```

| 数据 | 生命周期 | 备份要求 |
|------|---------|---------|
| Virtual Keys | 长期 | ⭐ 必须备份（丢了 key 全失效） |
| Spend Logs | 长期 | 必须（成本审计） |
| 缓存数据 | 短期 | 可丢 |

> 💡 备份策略：**PG 每日备份（含 key 表）**——key 数据丢失 = 所有调用方要换 key，事故级别。Spend Logs 定期归档到数仓做成本分析（05 章周报）。

## 4. 可观测：日志/指标/追踪

```yaml
# config.yaml 可观测配置
general_settings:
  otel: true                    # OpenTelemetry（FastAPI 体系 08 章）
  json_logs: true               # 结构化日志

# 指标（OTel 导出 Prometheus）：
#   litellm_request_total{model, key, team}    请求计数
#   litellm_request_latency{model}             延迟
#   litellm_cost_total{model, team}            成本
#   litellm_cache_hit_ratio                    缓存命中率
#   litellm_router_healthy_models              健康模型数
```

| 观测维度 | 指标 | 告警 |
|---------|------|------|
| 请求 | QPS/延迟 P99 | 延迟飙升 |
| 成本 | 每模型/团队花费 | 超预算（05 章） |
| 健康 | 模型失败率/可用数 | 供应商故障（fallback 生效） |
| 缓存 | 命中率 | 低于 50% 查 leakage |

> 🎯 **核心要点**：网关是**观测的黄金点位**——所有请求/成本/失败都过它。OTel 一行配置，Prometheus + Grafana 全家桶接入（FastAPI 体系 08 章可观测三支柱同款模式）。**告警三件套：成本超预算、模型失败率、缓存命中率**。

## 5. 数据面/控制面分离（2026）

**2026-05 组件化部署**：LLM 数据面与管理工作面分离。

```text
之前：管理查询（慢）拖累请求处理（快）→ 数据面可靠性被控制面拖底

之后：
  数据面（data plane）：/chat/completions、/embeddings、passthrough
    → 独立实例，追求低延迟高吞吐
  控制面（control plane）：/key、/team、/spend、SSO、审计
    → 独立实例，慢查询不影响请求

部署：两组实例 + 各自扩展策略
```

| 面 | 端点 | 扩缩容策略 |
|----|------|-----------|
| 数据面 | 模型调用端点 | 按流量扩容 |
| 控制面 | key/团队/花费管理 | 按管理量 |

> 💡 工程含义：**大规模生产把两组实例分开部署**——数据面按 QPS 扩容，控制面按管理并发扩容，互不拖累。小规模（<100 QPS）单组即可。

## 6. 高可用与扩容

| 层 | 高可用手段 |
|----|-----------|
| 网关实例 | 多实例 + Nginx 负载均衡（无状态） |
| 数据库 | PG 主从 + 自动故障切换 |
| Redis | 哨兵/集群 |
| 供应商 | 多供应商 fallback（04 章）——供应商故障降级为"另一家" |
| 本地兜底 | 关键场景本地模型冷备（03 章） |

```bash
# 扩容：docker compose up -d --scale litellm=3
# 或 K8s Deployment replicas=3（readiness 探针摘流量）
```

> 🎯 **核心要点**：**网关自身的 HA 简单（无状态多实例）；真正的 HA 在供应商层**——多供应商 fallback（04 章）让"某家供应商故障"对业务不可见，这才是 LLM 生产架构的可靠核心。

## 7. 版本管理与升级

| 策略 | 做法 |
|------|------|
| 锁定版本 | 镜像/依赖 pin（v1.94.0） |
| 升级节奏 | 每月跟 minor，读 release notes 的 Breaking 标记 |
| 预发验证 | 测试环境先升，跑回归（key/预算/路由用例） |
| 回滚 | 旧镜像 tag 即回滚 |
| 配置兼容 | 升级前 diff config 字段（迁移文档） |

> ⚠️ LiteLLM 迭代极快（每周多个 minor），**Breaking Changes 偶发**——升级三步：读 release notes → 测试环境回归 → 灰度切换。生产严禁盲升。

## 8. 常见坑

| # | 坑 | 现象 | 解法 |
|:---:|-----|------|------|
| 1 | 无数据库 | 重启 key 全丢 | PG 持久化 |
| 2 | key 表没备份 | 灾难恢复失败 | 每日备份 |
| 3 | 单实例无 HA | 网关挂全挂 | 多实例 + Nginx |
| 4 | 忽略供应商 HA | 供应商故障业务挂 | 多供应商 fallback |
| 5 | 镜像 latest | 升级爆炸 | 锁定版本 |
| 6 | 控制面拖数据面 | 请求延迟波动 | 数据面/控制面分离 |
| 7 | 无监控 | 故障盲区 | OTel + 告警三件套 |
| 8 | 配置进 git 带密钥 | 泄漏 | 密钥全环境变量 |

## 9. 核心要点

| 序号 | 要点 |
|:---:|------|
| 1 | 生产架构：Nginx + 多实例 + PG + Redis，与 FastAPI 体系同构 |
| 2 | Docker：锁定版本 + 密钥环境变量 + PG 数据卷 |
| 3 | PG 管 key/花费/团队；key 表必须备份 |
| 4 | OTel 一行接入；告警三件套：成本/失败率/缓存命中 |
| 5 | 数据面/控制面分离（2026-05）：大规模部署必备 |
| 6 | 真正的 HA 在供应商 fallback，不在网关本身 |
| 7 | 升级三步：release notes → 测试回归 → 灰度 |
| 8 | 八大坑：数据库/备份/HA/版本/密钥 |

---

**下一模块**：[08-生态集成](08-生态集成.md) / **返回总览**：[00-LiteLLM知识体系总览](00-LiteLLM知识体系总览.md)

## 参考来源

- [LiteLLM 官方：Docker 部署](https://docs.litellm.ai/docs/proxy/deploy)
- [LiteLLM 官方：数据库设置](https://docs.litellm.ai/docs/proxy/configs#database)
- [LiteLLM 官方：OTel 集成](https://docs.litellm.ai/docs/proxy/otel)
- [LiteLLM Blog：组件化部署（数据面/控制面）](https://docs.litellm.ai/blog)
