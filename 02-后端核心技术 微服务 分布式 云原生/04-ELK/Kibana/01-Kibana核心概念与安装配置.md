# Kibana 核心概念与安装配置
> Elastic Stack 的可视化与操作门户——查询、可视化、告警、安全管理全在一处，版本与 ES 严格对齐（当前 9.x 系列），本文覆盖定位、架构、部署与配置

## 目录
1. [Kibana 是什么](#1-kibana-是什么)
2. [Kibana 与 Elastic Stack 的关系](#2-kibana-与-elastic-stack-的关系)
3. [核心功能全景](#3-核心功能全景)
4. [架构与请求链路](#4-架构与请求链路)
5. [Docker 部署](#5-docker-部署)
6. [Linux 部署](#6-linux-部署)
7. [核心配置详解](#7-核心配置详解)
8. [安全启动（HTTPS + 账号）](#8-安全启动https--账号)
9. [版本对齐与升级注意](#9-版本对齐与升级注意)
10. [生产部署 Checklist](#10-生产部署-checklist)

---

## 1. Kibana 是什么

### 1.1 概述

| 属性 | 说明 |
|------|------|
| 全称 | Kibana |
| 定位 | Elastic Stack 的可视化与操作门户（UI 层） |
| 开发语言 | TypeScript + Node.js |
| 数据来源 | Elasticsearch（只读为主，管理操作为辅） |
| 默认端口 | 5601（HTTP/HTTPS） |
| 开源协议 | Elastic License 2.0（9.x 起部分功能需订阅） |
| 当前版本 | 9.x 系列（2026-05 已到 9.4） |

### 1.2 企业架构位置

```text
                 ┌─────────────────────────────┐
  用户浏览器 ────→│  Kibana (5601)              │
                 │  · Discover 检索            │
                 │  · Dashboard 可视化          │
                 │  · Alerting 告警            │
                 │  · Stack Monitoring 监控    │
                 │  · Dev Tools / 管理          │
                 └─────────────┬───────────────┘
                               │ HTTP(S) / 9200
                 ┌─────────────▼───────────────┐
                 │  Elasticsearch              │
                 │  （查询 · 聚合 · 告警执行）    │
                 └─────────────────────────────┘
```

> 🎯 一句话定位：**Kibana 不存数据，它是 ES 的「前端」**——所有查询、聚合、可视化、告警最终都翻译成 ES REST API 请求执行。

### 1.3 Kibana 与 ES 的关系（核心认知）

| 维度 | Kibana | Elasticsearch |
|------|------|------|
| 角色 | 门户/UI | 引擎/存储 |
| 数据 | 只存元数据（Saved Objects） | 存全部业务数据 |
| 查询 | 翻译成 DSL 发给 ES | 真正执行 |
| 告警 | 定义规则 | 执行查询（ES 9.x 由 ES 侧执行） |
| 版本 | **必须与 ES 同版本** | 决定生态版本 |

> ⚠️ 版本铁律：**Kibana 与 ES 必须同主版本**（如都是 9.x），否则连接失败；跨主版本升级必须先升 ES 再升 Kibana（或同时）。

---

## 2. Kibana 与 Elastic Stack 的关系

### 2.1 完整栈视角

```text
Beats（轻量采集）──→ Logstash（聚合处理）──→ Elasticsearch（存储检索）
                                                  ↑
                                              Kibana（可视化）
```

| 组件 | 职责 | 语言/运行时 |
|------|------|------|
| Beats（Filebeat 等） | 边缘采集、轻量传输 | Go 单二进制 |
| Logstash | 中心化管道：解析/清洗/路由 | Java + JRuby |
| Elasticsearch | 分布式存储与检索 | Java |
| Kibana | 可视化、监控、告警、管理 | Node.js |

### 2.2 数据从采集到可视化的全链路

```text
应用日志 → Filebeat → Logstash（grok 解析） → ES（索引存储）
                                                ↓
Kibana Discover（检索） → Lens/Dashboard（可视化） → Alerting（告警）
```

---

## 3. 核心功能全景

| 功能模块 | 用途 | 对应章节 |
|------|------|------|
| Discover | 日志检索与探索（KQL/ES|QL） | 02 篇 |
| Dashboard | 多面板组合可视化 | 03 篇 |
| Lens | 拖拽式可视化编辑器 | 03 篇 |
| Alerting | 告警规则与通知 | 04 篇 |
| Stack Monitoring | ES/Logstash/Kibana 自身监控 | 04 篇 |
| Security | 用户、角色、权限、Spaces | 05 篇 |
| Index Management | 索引生命周期管理 | 05 篇 |
| Dev Tools | Console 直接调 ES API | 05 篇 |
| Saved Objects | 保存的搜索/仪表盘等对象管理 | 05 篇 |
| Canvas | 像素级自定义报表（逐步弱化） | 03 篇 |

### 3.1 9.x 新增能力速览（2026 现状，逐条核实）

| 能力 | 版本 | 说明 |
|------|------|------|
| ES|QL 可视化 | 9.1 GA | ES|QL 查询可直接出图（官方确认） |
| PromQL 支持 | 9.4 技术预览 | ES|QL 编辑器 `PROMQL` 命令，>80% 覆盖率（官方确认） |
| Elastic Agent Builder | 9.x 技术预览 | 对话式构建 AI 智能体（MCP 标准；9.1 起相关能力陆续引入） |
| 图表配置持久化 | 9.1 文档已描述 | 改查询不丢配置（官方确认功能，无独立 GA 标注） |
| 多字段 Breakdown | 9.x | 柱/折/面按多字段分组（官方确认功能，无独立 GA 标注） |
| 图表默认调整 | 9.4 | 时间序列默认折线图、Top value 默认 9 个系列（官方确认） |

---

## 4. 架构与请求链路

### 4.1 Kibana 请求处理流程

```text
浏览器 → Kibana 前端（React） → Kibana 服务端（Node.js）
    → 校验权限（Security） → 翻译为 ES REST 请求
    → ES 执行 → 结果回传 → 前端渲染
```

| 环节 | 说明 |
|------|------|
| 前端 | React SPA，负责交互与渲染 |
| 服务端 | Node.js，负责鉴权、代理、Saved Objects 存取 |
| Saved Objects | 元数据存 ES 内部索引 `.kibana_*` |
| 数据请求 | 全部代理到 ES 执行（Kibana 不计算数据） |

### 4.2 Kibana 自身索引（内部）

| 索引 | 用途 |
|------|------|
| `.kibana_<ver>` | Saved Objects（仪表盘/搜索/可视化） |
| `.kibana_task_manager_<ver>` | 后台任务（告警执行等） |
| `.kibana_security_*` | 安全配置 |
| `.kibana_alerting_cases_*` | 告警与案例 |

> 💡 备份恢复 Kibana = 备份恢复这些 `.kibana_*` 索引（通过 ES 快照即可）。

---

## 5. Docker 部署

### 5.1 单节点快速启动（Docker Compose）

```yaml
# docker-compose.yml（ES + Kibana 同版本 9.x）
version: "3.8"
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:9.4.0
    container_name: es
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false        # 测试环境关闭安全
      - ES_JAVA_OPTS=-Xms1g -Xmx1g
    ports:
      - "9200:9200"
    volumes:
      - es_data:/usr/share/elasticsearch/data

  kibana:
    image: docker.elastic.co/kibana/kibana:9.4.0
    container_name: kibana
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
      - SERVER_NAME=kibana
    ports:
      - "5601:5601"
    depends_on:
      - elasticsearch

volumes:
  es_data:
```

### 5.2 启动与验证

```bash
# 启动
docker compose up -d

# 等待 ES 就绪（约 30-60 秒）
curl http://localhost:9200

# 访问 Kibana
http://localhost:5601

# 验证 Kibana 状态 API
curl http://localhost:5601/api/status
# 期望: "overall" 状态为 green/黄色警告均可访问，但 ES 连接必须正常
```

> ⚠️ 常见坑：Kibana 容器启动但页面报「Kibana server is not ready yet」——通常是因为 ES 未就绪或版本不匹配，查看容器日志 `docker logs kibana` 定位。

---

## 6. Linux 部署

### 6.1 下载与解压（以 9.4 为例）

```bash
# 下载（与 ES 版本严格一致）
wget https://artifacts.elastic.co/downloads/kibana/kibana-9.4.0-linux-x86_64.tar.gz

# 解压
tar -zxvf kibana-9.4.0-linux-x86_64.tar.gz
cd kibana-9.4.0
```

### 6.2 修改配置并启动

```bash
# 修改 config/kibana.yml
# server.host: "0.0.0.0"
# elasticsearch.hosts: ["http://127.0.0.1:9200"]
# elasticsearch.username: "kibana_system"   # 启用安全后需要
# elasticsearch.password: "xxx"

# 前台启动（调试）
bin/kibana

# 后台启动（生产推荐 systemd）
sudo useradd -r kibana
sudo systemctl enable kibana   # 需编写 systemd unit 文件
```

### 6.3 systemd 服务示例

```ini
# /etc/systemd/system/kibana.service
[Unit]
Description=Kibana
After=network.target elasticsearch.service

[Service]
Type=simple
User=kibana
ExecStart=/opt/kibana/bin/kibana
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

---

## 7. 核心配置详解

### 7.1 kibana.yml 关键参数

| 配置项 | 默认值 | 说明 |
|------|------|------|
| `server.host` | localhost | 监听地址，生产必须 `0.0.0.0` |
| `server.port` | 5601 | 监听端口 |
| `server.name` | hostname | 实例名（监控区分多实例） |
| `elasticsearch.hosts` | http://localhost:9200 | ES 地址，支持数组（多节点） |
| `elasticsearch.username/password` | — | 连接 ES 的账号（kibana_system） |
| `elasticsearch.ssl.certificateAuthorities` | — | ES 为 HTTPS 时的 CA 证书路径 |
| `server.ssl.enabled` | false | Kibana 自身 HTTPS |
| `i18n.locale` | en | 界面语言，可设 zh-CN |
| `xpack.security.enabled` | true（9.x 默认） | 安全功能开关 |
| `xpack.encryptedSavedObjects.encryptionKey` | — | 加密 Saved Objects 的密钥 |

### 7.2 多 ES 节点配置

```yaml
elasticsearch.hosts:
  - http://es-node1:9200
  - http://es-node2:9200
  - http://es-node3:9200
# Kibana 会自动负载均衡与故障转移
```

### 7.3 内存相关（Node.js 进程）

| 配置 | 说明 |
|------|------|
| `NODE_OPTIONS=--max-old-space-size=4096` | 环境变量方式设置 Node 堆 |
| 服务端内存需求 | 建议 ≥ 4GB（大型集群 8GB+） |
| 前端静态资源 | 浏览器端加载，与服务端内存无关 |

> ⚠️ Kibana 是 Node.js 进程，**不是 JVM**——网上「调 JVM 内存」的说法不适用于 Kibana；Logstash 才是 JVM 进程。

---

## 8. 安全启动（HTTPS + 账号）

### 8.1 生产推荐的安全配置链路

```text
ES：xpack.security.enabled=true + 生成证书 + 设置内置账号密码
Kibana：server.ssl.enabled=true + elasticsearch 走 HTTPS + kibana_system 账号
浏览器：访问 https://kibana:5601，用 kibana_admin 等用户登录
```

### 8.2 Kibana 侧 HTTPS 配置

```yaml
server.ssl.enabled: true
server.ssl.certificate: "/etc/kibana/certs/kibana.crt"
server.ssl.key: "/etc/kibana/certs/kibana.key"
elasticsearch.hosts: ["https://es-node:9200"]
elasticsearch.ssl.certificateAuthorities: ["/etc/kibana/certs/ca.crt"]
elasticsearch.username: "kibana_system"
elasticsearch.password: "<生成的服务账号密码>"
```

### 8.3 内置账号速查（Elastic Stack 9.x）

| 账号 | 用途 | 初始密码 |
|------|------|------|
| `elastic` | 超级管理员 | 首次启动自动生成（`bin/elasticsearch-setup-passwords`） |
| `kibana_system` | Kibana 连接 ES 用 | 同上 |
| `logstash_system` | Logstash 监控上报用 | 同上 |
| `beats_system` | Beats 监控上报用 | 同上 |

> 💡 设置密码：ES 启动后执行 `bin/elasticsearch-setup-passwords interactive` 逐个设置；测试环境可用 `auto` 自动生成。

---

## 9. 版本对齐与升级注意

### 9.1 版本铁律与兼容矩阵

| 场景 | 要求 |
|------|------|
| Kibana 与 ES | **同主版本**（9.x ↔ 9.x），推荐同小版本 |
| Kibana 与浏览器 | 支持最新两个主版本的 Chrome/Firefox/Edge/Safari |
| 跨大版本升级 | ES → Kibana → 其他组件，逐版本跳（如 7.17 → 8.17 → 9.x） |

### 9.2 8.x → 9.x 升级要点（2026 现状）

```text
① 先升级 ES 到 9.x，再升级 Kibana（同版本）
② 9.x 中 ES|QL 可视化已 GA（9.1+），旧的 Table 可视化逐步迁移
③ 检查已保存的 Dashboard 面板兼容性（Lens 优先）
④ Elastic Agent Builder（技术预览）、PromQL（9.4 技术预览）等新能力勿用于生产
⑤ 备份 .kibana_* 索引（快照）后再升级
```

---

## 10. 生产部署 Checklist

| # | 检查项 | 标准 |
|:---:|------|------|
| 1 | 版本 | Kibana 与 ES 完全同版本 |
| 2 | 安全 | HTTPS + 账号启用，禁用 `elastic` 默认密码 |
| 3 | 网络 | Kibana 仅内网可达，前端经网关/Nginx 反代 |
| 4 | 资源 | 服务端内存 ≥ 4GB，磁盘预留日志空间 |
| 5 | 高可用 | 多 Kibana 实例 + Nginx 负载均衡（无状态） |
| 6 | 备份 | `.kibana_*` 索引定期快照 |
| 7 | 监控 | 启用 Stack Monitoring 观察自身健康 |
| 8 | 时区 | `i18n.locale` 与日志时区统一（UTC 存储） |

> 🎯 **核心要点**：Kibana = ES 的「操作门户」，三件事必知——**① 版本与 ES 严格对齐**（9.x ↔ 9.x）；**② 数据请求全部代理给 ES**，Kibana 自身只存 `.kibana_*` 元数据；**③ 它是 Node.js 进程，调优方向是内存与反向代理**而非 JVM。9.x 时代（2026）的新边界：ES|QL 可视化已 GA、PromQL 与 AI Agent 为预览、告警执行下沉到 ES 侧。部署先跑通「Docker Compose 单节点」，生产再上「HTTPS + systemd + 反代」三件套。

---

**下一模块**：[02-Kibana数据探索Discover与检索](02-Kibana数据探索Discover与检索.md) | **返回总览**：[00-Kibana专题总览](00-Kibana专题总览.md)
