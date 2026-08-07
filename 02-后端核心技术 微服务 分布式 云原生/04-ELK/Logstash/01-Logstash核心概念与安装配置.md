# Logstash 核心概念与安装配置
> 数据管道的心脏——接收、解析、清洗、路由、输出，Java + JRuby 实现的中心化日志处理引擎（当前 9.x 系列，自带 JDK 21），本文件覆盖定位、执行模型、部署与配置

## 目录
1. [Logstash 是什么](#1-logstash-是什么)
2. [架构与定位](#2-架构与定位)
3. [执行模型：worker、batch 与背压](#3-执行模型workerbatch-与背压)
4. [Docker 部署](#4-docker-部署)
5. [Linux 部署](#5-linux-部署)
6. [核心配置详解](#6-核心配置详解)
7. [多 Pipeline 配置](#7-多-pipeline-配置)
8. [Logstash vs Beats vs Ingest Pipeline](#8-logstash-vs-beats-vs-ingest-pipeline)
9. [生产部署 Checklist](#9-生产部署-checklist)

---

## 1. Logstash 是什么

### 1.1 概述

| 属性 | 说明 |
|------|------|
| 全称 | Logstash |
| 定位 | 服务端数据管道（采集 → 解析 → 输出） |
| 开发语言 | Java 核心 + JRuby 插件 |
| 运行时 | 自带 JDK 21（9.x 起免装 Java） |
| 默认端口 | 5044（Beats 输入）/ 9600（监控 API） |
| 开源协议 | Apache 2.0（核心） |
| 当前版本 | 9.x 系列（2026-05 已到 9.4.2） |

### 1.2 核心能力

| 能力 | 说明 |
|------|------|
| 多源采集 | File、Beats、Kafka、HTTP、JDBC、TCP/UDP 等 50+ 输入 |
| 实时解析 | Grok/Dissect 正则解析、JSON、日期、地理信息 |
| 数据清洗 | Mutate 字段增删改、类型转换、脱敏 |
| 多路路由 | 按条件分发到不同输出（if/else 条件） |
| 可靠传输 | 持久化队列、死信队列（DLQ） |
| 弹性扩展 | 多 worker、多 pipeline、集群部署 |

---

## 2. 架构与定位

### 2.1 ELK 数据流中的位置

```text
应用日志 → Filebeat（边缘采集）
              ↓ 5044
          Logstash（解析清洗路由）──→ Elasticsearch（存储检索）
              ↑                          ↓
         Kafka（缓冲可选）           Kibana（可视化）
```

| 组件 | 职责 | 部署位置 |
|------|------|------|
| Filebeat | 轻量采集、压缩传输 | 每台应用服务器 |
| Logstash | 中心化解析、路由、富化 | 集中式处理节点 |
| Kafka | 削峰缓冲（可选） | 消息队列集群 |
| Elasticsearch | 存储与检索 | 数据节点集群 |

### 2.2 Logstash 的两种部署形态

| 形态 | 说明 | 适用 |
|------|------|------|
| 单节点 | 一个 Logstash 处理全部 | 中小规模（< 100MB/s） |
| 集群 | 多 Logstash + Kafka 缓冲 | 大规模（多租户、峰值高） |

---

## 3. 执行模型：worker、batch 与背压

### 3.1 Pipeline 内部执行流程

```text
input 插件 → 输入队列（内存/PQ）→ worker 线程
                                     ├─ filter 链（串行）
                                     └─ output 链
多 worker 并行 → 吞吐 = worker 数 × 单 worker 速率
```

### 3.2 三大核心参数（面试必考）

| 参数 | 默认值 | 说明 | 调优方向 |
|------|:---:|------|------|
| `pipeline.workers` | CPU 核数 | 并行 worker 数 | filter CPU 密集 → 调大；output IO 密集 → 不调 |
| `pipeline.batch.size` | 125 | 每 worker 每次取多少事件 | 调大提升吞吐，增加内存与尾部延迟 |
| `pipeline.batch.delay` | 50ms | 不足 batch 时的等待时间 | 低流量场景延迟下限 |

### 3.3 背压机制（Backpressure）

```text
output 变慢（ES 写入延迟高）→ 输出队列积压
→ 背压回传 → input 暂停/降速接收
→ 保护内存不爆

配置相关：
  · 内存队列：默认，无持久化
  · 持久化队列（PQ）：落盘，重启不丢数据
```

> 🎯 面试要点：**Logstash 吞吐瓶颈通常在下游（ES 写入）而非解析**——调优先看 output 延迟，再动 workers/batch。

### 3.4 内存计算模型

```text
内存占用 ≈ worker 数 × batch.size × 单事件内存
例：8 workers × 125 events × 1KB = 1MB 批次内存（事件本身还有对象开销）
⚠️ 9.x 注意：logstash_pipeline_buffer_type 默认值从 direct 改为 heap，
   升级到 9.x 会静默增加 JVM 堆压力，需关注堆配置
```

---

## 4. Docker 部署

### 4.1 单容器快速启动

```bash
# 拉取镜像（与 ES 同版本）
docker pull docker.elastic.co/logstash/logstash:9.4.0

# 运行（挂载配置）
docker run -d --name logstash \
  -p 5044:5044 -p 9600:9600 \
  -v "$PWD/config:/usr/share/logstash/config" \
  -v "$PWD/pipeline:/usr/share/logstash/pipeline" \
  -v "$PWD/data:/usr/share/logstash/data" \
  docker.elastic.co/logstash/logstash:9.4.0
```

### 4.2 最小管道示例

```ruby
# pipeline/logstash.conf
input {
  beats {
    port => 5044
  }
}

filter {
  # 解析示例
  grok {
    match => { "message" => "%{TIMESTAMP_ISO8601:log_time} %{LOGLEVEL:level} %{GREEDYDATA:content}" }
  }
}

output {
  elasticsearch {
    hosts => ["http://es-node:9200"]
    index => "app-logs-%{+YYYY.MM.dd}"
    user => "logstash_internal"
    password => "xxx"
  }
}
```

---

## 5. Linux 部署

### 5.1 下载与解压（以 9.4.2 为例）

```bash
# 下载（9.x 自带 JDK 21，无需单独装 Java）
wget https://artifacts.elastic.co/downloads/logstash/logstash-9.4.2-linux-x86_64.tar.gz
tar -zxvf logstash-9.4.2-linux-x86_64.tar.gz
cd logstash-9.4.2
```

### 5.2 验证安装

```bash
# 版本
bin/logstash --version

# 语法检查（写配置后必跑）
bin/logstash -f /etc/logstash/conf.d/test.conf --config.test_and_exit

# 前台调试运行（stdin → stdout 自测管道）
bin/logstash -e 'input { stdin {} } output { stdout { codec => rubydebug } }'
# 输入 hello 回车，能看到 rubydebug 格式输出即正常
```

### 5.3 systemd 服务

```ini
# /etc/systemd/system/logstash.service
[Unit]
Description=Logstash
After=network.target

[Service]
Type=simple
User=logstash
ExecStart=/opt/logstash/bin/logstash
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

---

## 6. 核心配置详解

### 6.1 logstash.yml 关键参数

| 配置项 | 默认值 | 说明 |
|------|------|------|
| `path.config` | config/logstash.yml | 管道配置目录（`*.conf`） |
| `pipeline.workers` | CPU 核数 | 每管道 worker 数 |
| `pipeline.batch.size` | 125 | 批次大小 |
| `pipeline.batch.delay` | 50 | 批次等待（ms） |
| `queue.type` | memory | 队列类型：memory / persisted |
| `path.data` | data | 数据目录（PQ 落盘位置） |
| `http.host` / `http.port` | 127.0.0.1 / 9600 | 监控 API 地址 |
| `xpack.monitoring.enabled` | false | 监控上报开关 |
| `log.level` | info | 日志级别 |

### 6.2 JVM 堆配置

```yaml
# jvm.options（9.x 默认堆 1GB）
-Xms1g
-Xmx1g

# 生产建议：根据事件大小与 worker 数调整
# 规则：堆 ≥ workers × batch.size × 单事件内存 × 3（含对象开销）
```

> ⚠️ Logstash 是 JVM 进程——**调优对象是 JVM 堆（jvm.options）**，与 Kibana（Node.js）完全不同。

---

## 7. 多 Pipeline 配置

### 7.1 pipelines.yml 多管道

```yaml
# config/pipelines.yml：多个管道独立运行，互不阻塞
- pipeline.id: beats-logs
  path.config: "/etc/logstash/pipeline.d/beats.conf"
  pipeline.workers: 4

- pipeline.id: kafka-metrics
  path.config: "/etc/logstash/pipeline.d/kafka_metrics.conf"
  queue.type: persisted
  queue.max_bytes: 4gb
```

### 7.2 多管道适用场景

| 场景 | 说明 |
|------|------|
| 日志与指标分离 | 各自独立管道，故障不互扰 |
| 不同租户 | 不同解析逻辑分管道 |
| 优先级隔离 | 高优管道独立 worker 与队列 |
| 权限隔离 | 各管道独立配置错误不影响全局 |

---

## 8. Logstash vs Beats vs Ingest Pipeline

| 维度 | Beats（Filebeat） | Logstash | ES Ingest Pipeline |
|------|------|------|------|
| 位置 | 边缘（每台机器） | 中心（集中处理） | ES 内（写入时处理） |
| 资源 | 极轻（Go 单进程） | 较重（JVM，1GB+ 堆） | 无独立进程 |
| 解析能力 | 弱（基本字段） | **强**（Grok 等 50+ 插件） | 中（内置 processor） |
| 持久队列 | 有（本地 spool） | 有（PQ） | **无** |
| 多输出 | 少 | **多输出路由** | 仅单一输出 |
| 适用 | 每台服务器采集 | 复杂解析 + 路由 + 富化 | 简单解析下沉 |

### 8.1 选型决策

```text
简单采集 + 简单解析（无多输出）→ Filebeat + ES Ingest Pipeline
复杂解析 / 多输出路由 / 需要 PQ/DLQ → Logstash（中心化）
采集量大、需要削峰缓冲 → Filebeat → Kafka → Logstash
```

> 🎯 面试要点：**Ingest Pipeline 无法替代 Logstash**——无持久队列、仅单一输出、失败处理弱；Logstash 的价值在「复杂管道 + 可靠性」。

---

## 9. 生产部署 Checklist

| # | 检查项 | 标准 |
|:---:|------|------|
| 1 | 版本 | 与 ES/Kibana 同主版本（9.x） |
| 2 | 资源 | 堆 ≥ 1GB（按量评估），CPU 预留解析余量 |
| 3 | 可靠性 | 重要管道开持久队列（PQ）+ 死信队列（DLQ） |
| 4 | 监控 | 开启 xpack.monitoring 上报 Stack Monitoring |
| 5 | 配置管理 | 管道配置入库（Git）版本化 |
| 6 | 语法检查 | 上线前 `--config.test_and_exit` |
| 7 | 集群模式 | 大规模部署前置 Kafka 削峰 |
| 8 | 9.x 升级注意 | 默认 buffer 类型 direct → heap，评估堆压力 |

> 🎯 **核心要点**：Logstash = **输入队列 + worker 链 + 输出路由**的管道引擎（JVM 进程，9.x 自带 JDK 21）。三大必知：**① 执行模型三参数**（workers/batch.size/batch.delay + 背压）；**② 可靠性两件套**（PQ 持久队列 + DLQ 死信队列）；**③ 定位**（中心化复杂解析，边缘采集用 Beats，简单解析下沉 Ingest Pipeline）。9.x 特别提醒：默认队列 buffer 类型变更（direct → heap）会增加堆压力，升级需评估。

---

**下一模块**：[02-Logstash管道结构与配置语法](02-Logstash管道结构与配置语法.md) | **返回总览**：[00-Logstash专题总览](00-Logstash专题总览.md)
