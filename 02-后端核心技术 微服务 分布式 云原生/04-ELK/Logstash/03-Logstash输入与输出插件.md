# Logstash 输入与输出插件
> 管道两端的世界：输入插件决定「从哪收」（Beats/File/Kafka/HTTP/JDBC），输出插件决定「发到哪」（ES/Kafka/HTTP）——本文件覆盖最常用的 10 个插件配置与选型

## 目录
1. [输入插件全景](#1-输入插件全景)
2. [Beats 输入（与 Filebeat 对接）](#2-beats-输入与-filebeat-对接)
3. [File 输入（直接读文件）](#3-file-输入直接读文件)
4. [Kafka 输入](#4-kafka-输入)
5. [HTTP 与 TCP/UDP 输入](#5-http-与-tcpudp-输入)
6. [JDBC 输入（数据库同步）](#6-jdbc-输入数据库同步)
7. [输出插件全景](#7-输出插件全景)
8. [Elasticsearch 输出详解](#8-elasticsearch-输出详解)
9. [输入输出选型决策](#9-输入输出选型决策)

---

## 1. 输入插件全景

| 插件 | 场景 | 要点 |
|------|------|------|
| beats | Filebeat/其他 Beats 对接（默认首选） | 端口 5044，传输可靠 |
| file | 直接读本地文件 | 需自己管理 sincedb |
| kafka | 队列缓冲消费 | 与生产端解耦 |
| http | REST 接口接收 | 应用直推日志/指标 |
| tcp/udp | 原始 TCP/UDP | 简单文本流 |
| jdbc | 定时查数据库 | 数据同步（非日志） |
| stdin | 调试 | 配 stdout 三件套 |
| elasticsearch | 从 ES 读（较少） | 跨集群场景 |

---

## 2. Beats 输入（与 Filebeat 对接）

### 2.1 标准配置

```ruby
input {
  beats {
    port => 5044
    host => "0.0.0.0"
    ssl_enabled => false          # 生产建议开启 TLS
    # ssl_certificate => "/etc/logstash/certs/ls.crt"
    # ssl_key => "/etc/logstash/certs/ls.key"
  }
}
```

### 2.2 Filebeat 侧对接

```yaml
# filebeat.yml
output.logstash:
  hosts: ["logstash-node:5044"]
```

### 2.3 beats 输入特性

| 特性 | 说明 |
|------|------|
| 可靠传输 | 内置 ACK 确认，失败重传 |
| 压缩 | 默认压缩传输，省带宽 |
| 多 Beats | 一个端口收 Filebeat/Metricbeat 等所有 Beats |
| 性能 | 每连接独立 worker，高吞吐 |
| 版本兼容 | Beats 与 Logstash 主版本一致 |

> 🎯 生产标配：**Filebeat（边缘采集）→ beats input（Logstash）** 是 ELK 日志链路的事实标准组合。

---

## 3. File 输入（直接读文件）

### 3.1 配置示例

```ruby
input {
  file {
    path => ["/var/log/app/*.log"]
    start_position => "beginning"     # 从文件头开始读
    sincedb_path => "/var/lib/logstash/sincedb"  # 断点记录位置
    # 多行日志合并（Java 堆栈）
    codec => multiline {
      pattern => "^\\s"
      negate => false
      what => "previous"
    }
  }
}
```

### 3.2 sincedb 机制（关键）

```text
sincedb = 记录每个文件已读到哪个字节位置的游标文件
作用：重启 Logstash 后从断点继续，不重复不遗漏
⚠️ 注意：
  · 默认 sincedb 在 data 目录，随容器重建会丢失（挂载卷）
  · 文件轮转（rotation）时按 inode 跟踪
  · 删除 sincedb = 重新读全部历史文件（慎用）
```

> 💡 选型提示：**能走 Filebeat 就别用 file 输入**——Filebeat 自带断点、压缩、背压，file 输入适合无 Filebeat 的裸机场景。

---

## 4. Kafka 输入

### 4.1 配置示例

```ruby
input {
  kafka {
    bootstrap_servers => "kafka-1:9092,kafka-2:9092"
    topics => ["app-logs"]
    group_id => "logstash-logs"
    consumer_threads => 4
    auto_offset_reset => "latest"
    codec => json
  }
}
```

### 4.2 Kafka 在 ELK 中的角色

```text
Filebeat → Kafka（削峰缓冲）→ Logstash（解析）→ ES

优势：
  · 解耦：采集端与处理端互不阻塞
  · 削峰：大促日志量暴涨时队列缓冲
  · 重放：消费失败可从 offset 重放
  · 多消费者：同一份日志供 Logstash + Flink 分析
```

### 4.3 Kafka 输入参数要点

| 参数 | 说明 |
|------|------|
| `bootstrap_servers` | Kafka 集群地址 |
| `topics` | 订阅主题（支持正则） |
| `group_id` | 消费组（同组内分片消费） |
| `consumer_threads` | 消费线程数（= 分区数最佳） |
| `auto_offset_reset` | earliest/latest |
| `decorate_events` | 把 topic/partition 信息加进事件 |

---

## 5. HTTP 与 TCP/UDP 输入

### 5.1 HTTP 输入（应用直推）

```ruby
input {
  http {
    port => 8080
    codec => json
    # 鉴权（生产必配）
    user => "logstash"
    password => "xxx"
    # 或 token：token => "secret-token"
  }
}
```

```bash
# 应用侧推送
curl -X POST http://logstash:8080 \
  -H "Content-Type: application/json" \
  -d '{"level":"ERROR","service":"order","message":"timeout"}'
```

### 5.2 TCP 输入（syslog 等）

```ruby
input {
  tcp {
    port => 5514
    mode => "server"
    codec => json_lines
  }
}
```

| 输入 | 适用 |
|------|------|
| http | 应用直推 JSON、指标上报 |
| tcp | syslog 转发、设备日志 |
| udp | 高吞吐可丢场景（不推荐日志核心链路） |

> ⚠️ 安全提示：HTTP/TCP 输入暴露在公网前必须加认证（user/password 或 token）或放内网。

---

## 6. JDBC 输入（数据库同步）

### 6.1 配置示例（定时同步 MySQL）

```ruby
input {
  jdbc {
    jdbc_driver_library => "/opt/logstash/mysql-connector-j-8.4.0.jar"
    jdbc_driver_class => "com.mysql.cj.jdbc.Driver"
    jdbc_connection_string => "jdbc:mysql://db:3306/orders?useSSL=false"
    jdbc_user => "sync_user"
    jdbc_password => "xxx"
    statement => "SELECT * FROM orders WHERE updated_at > :sql_last_value"
    schedule => "*/5 * * * *"          # cron 每 5 分钟
    use_column_value => true
    tracking_column => "updated_at"
    tracking_column_type => "timestamp"
  }
}
```

### 6.2 增量同步机制

```text
:sql_last_value = 上次同步的最大 tracking 值（存于
  jdbc 状态文件 .logstash_jdbc_last_run）

模式：
  · 时间戳增量（推荐）：WHERE updated_at > :sql_last_value
  · 自增 ID 增量：WHERE id > :sql_last_value
  · 全量：不加条件（数据量小时可用）

⚠️ 注意：delete 操作不会被同步（只增不删）——需要全量
   定期重建或双写策略
```

---

## 7. 输出插件全景

| 插件 | 场景 | 要点 |
|------|------|------|
| elasticsearch | 写入 ES（默认首选） | 批量、幂等、失败重试 |
| kafka | 转发到队列 | 级联处理链 |
| stdout | 调试 | rubydebug/dots |
| file | 落盘 | 归档、二次处理 |
| http | 回调接口 | 告警通知等 |
| s3 | 对象存储归档 | 冷数据归档 |

---

## 8. Elasticsearch 输出详解

### 8.1 标准配置

```ruby
output {
  elasticsearch {
    hosts => ["http://es-1:9200", "http://es-2:9200"]
    index => "app-logs-%{+YYYY.MM.dd}"
    user => "logstash_internal"
    password => "xxx"
    document_id => "%{request_id}"    # 幂等写入（可选）
    # 数据流模式（9.x 推荐）：
    # data_stream => "true"
    # data_stream_type => "logs"
    # data_stream_dataset => "app"
  }
}
```

### 8.2 写入机制

| 特性 | 说明 |
|------|------|
| 批量 | 自动攒批（batch.size 相关），吞吐高 |
| 重试 | 失败自动重试 + 指数退避 |
| document_id | 同 ID 覆盖写（幂等，防重复） |
| 模板 | 自动应用索引模板（字段映射） |
| 数据流 | 9.x 推荐 data_stream 模式（对接 ILM） |

### 8.3 失败处理与 DLQ

```ruby
output {
  elasticsearch {
    hosts => ["http://es:9200"]
    # 失败事件进死信队列（需开启）
    dead_letter_queue_enable => true
  }
}

# logstash.yml
dead_letter_queue.enable: true
dead_letter_queue.max_bytes: 1gb
```

```text
DLQ 作用：ES 写入失败（映射冲突/超限）的事件不丢弃
→ 存到 data/dead_letter_queue/ 下
→ 后续可用 dlq 输入插件重新处理

排查 DLQ 命令：
  bin/logstash-plugin list | grep dlq
  管道配置中加 input { dead_letter_queue { pipeline_id => "main" } }
```

---

## 9. 输入输出选型决策

### 9.1 输入选型

```text
日志采集？→ 有 Filebeat → beats input（首选）
          → 无 Filebeat → file input（裸机）
日志量大需缓冲？→ Kafka input
应用直推？→ http input（带鉴权）
数据库同步？→ jdbc input（增量）
```

### 9.2 输出选型

```text
日志入库？→ elasticsearch（data_stream 模式 + ILM）
需要级联处理？→ kafka output
需要通知？→ http output
需要归档？→ s3/file output
```

> 🎯 **核心要点**：输入输出是管道的「两端世界」——**输入端三原则**（Beats 优先、Kafka 兜底削峰、HTTP/JDBC 直推需鉴权）；**输出端两重点**（ES 输出用批量 + 幂等 ID、失败进 DLQ 不丢数据）。生产链路最稳组合：**Filebeat → Logstash（beats input → ES output + DLQ）→ ES**；大流量升级为 **Filebeat → Kafka → Logstash → ES**。

---

**下一模块**：[04-Logstash过滤插件与数据清洗](04-Logstash过滤插件与数据清洗.md) | **返回总览**：[00-Logstash专题总览](00-Logstash专题总览.md)
