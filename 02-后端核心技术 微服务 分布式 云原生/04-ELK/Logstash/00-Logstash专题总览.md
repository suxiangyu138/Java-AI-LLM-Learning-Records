# Logstash 专题总览
> 数据管道的心脏——接收、解析、清洗、路由、输出，7 篇文档覆盖 Logstash 全部核心面（基于 Elastic Stack 9.x，2026 现状）

## 📚 目录
1. [专题导航](#1-专题导航)
2. [核心概念速查](#2-核心概念速查)
3. [学习路线推荐](#3-学习路线推荐)
4. [面试要点](#4-面试要点)

---

## 1. 专题导航

| 序号 | 模块 | 核心内容 | 适用人群 |
|:---:|------|------|------|
| [01-Logstash核心概念与安装配置](01-Logstash核心概念与安装配置.md) | 定位、执行模型、Docker/Linux 部署、多管道、Beats/Ingest 选型 | 所有人（入门必读） |
| [02-Logstash管道结构与配置语法](02-Logstash管道结构与配置语法.md) | 三阶段、Event 字段、条件路由、Codec、调试三件套 | 后端、运维 |
| [03-Logstash输入与输出插件](03-Logstash输入与输出插件.md) | Beats/File/Kafka/HTTP/JDBC 输入、ES 输出、DLQ | 后端、运维 |
| [04-Logstash过滤插件与数据清洗](04-Logstash过滤插件与数据清洗.md) | Grok/Dissect/Mutate/Date/JSON、富化三兄弟、Nginx 实战 | 后端、运维（核心） |
| [05-Logstash性能调优与可靠传输](05-Logstash性能调优与可靠传输.md) | workers/batch、JVM、PQ 持久队列、DLQ、监控 API、9.x 变更 | 运维（SRE） |
| [06-日志采集全链路实战](06-日志采集全链路实战.md) | SpringBoot JSON 日志 → Filebeat → Logstash → ES → Kibana 完整落地 | 所有人（实战） |

---

## 2. 核心概念速查

| 概念 | 一句话 |
|------|------|
| Logstash | 中心化数据管道（Java + JRuby，端口 9600 监控） |
| Pipeline | input → filter → output 三段式处理链 |
| Event | 管道中流动的事件（字段集合，不是字符串） |
| Grok / Dissect | 正则解析 / 定界符快速切分 |
| 条件路由 | if/else 按字段分发到不同输出 |
| PQ（持久队列） | 重启不丢数据的落盘队列 |
| DLQ（死信队列） | 输出失败不丢弃的兜底队列 |
| workers / batch | 并行度 / 批大小（吞吐三参数之二） |
| 背压 | 下游慢时自动回传暂停上游 |
| beats input | 与 Filebeat 对接的标准入口（5044） |

---

## 3. 学习路线推荐

| 路线 | 顺序 | 适用 |
|------|------|------|
| 后端路线 | 01 → 02 → 04 → 06 | 会写管道、接入应用日志 |
| 运维路线 | 01 → 03 → 05 → 06 | 会部署、调优、保可靠 |
| 完整路线 | 01-06 全走 | 全面掌握 |

---

## 4. 面试要点

| 问题 | 标准回答要点 |
|------|------|
| Logstash 三阶段？ | input 收 / filter 改 / output 发，Event 流式处理 |
| Grok vs Dissect？ | Grok 正则灵活慢；Dissect 定界符快；固定格式用 Dissect |
| 怎么不丢数据？ | PQ（重启不丢）+ DLQ（失败不丢）双保险 |
| 吞吐怎么调？ | 先定位瓶颈：下游慢调输出、解析重调 workers、延迟调 batch.delay |
| Logstash vs Ingest Pipeline？ | Ingest 无 PQ、单一输出；复杂管道 + 可靠性选 Logstash |
| 9.x 有什么变化？ | 自带 JDK 21、默认 buffer direct → heap（堆压力）、Stats API 增强 |

> 🎯 **核心要点**：Logstash 七篇覆盖「概念 → 语法 → 插件 → 清洗 → 性能 → 实战」全链路。学习主线：**01 懂定位 → 02 会写管道 → 03/04 会选插件 → 05 会调优 → 06 全链路落地**。面试记住三句话：**管道是 Event 流、可靠性是 PQ+DLQ、调优先找瓶颈**。

---

**下一篇**：[01-Logstash核心概念与安装配置](01-Logstash核心概念与安装配置.md) | **关联专题**：[Kibana](../Kibana/00-Kibana专题总览.md)、[Elasticsearch](../Elasticsearch/01-ES核心概念与安装配置.md)
