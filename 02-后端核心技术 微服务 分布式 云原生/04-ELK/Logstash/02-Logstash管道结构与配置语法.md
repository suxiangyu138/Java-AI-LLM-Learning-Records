# Logstash 管道结构与配置语法
> 一段 pipeline 三件事：收（input）、改（filter）、发（output）。本文件覆盖配置语法、事件字段、条件路由、编解码器与调试工具——写管道前必读

## 目录
1. [Pipeline 三阶段](#1-pipeline-三阶段)
2. [配置语法基础](#2-配置语法基础)
3. [事件（Event）与字段](#3-事件event与字段)
4. [条件路由 if/else](#4-条件路由-ifelse)
5. [Codec 编解码器](#5-codec-编解码器)
6. [变量与引用](#6-变量与引用)
7. [调试管道：stdin/stdout 三件套](#7-调试管道stdinstdout-三件套)
8. [管道设计最佳实践](#8-管道设计最佳实践)

---

## 1. Pipeline 三阶段

```text
input（数据入口）→ filter（数据处理）→ output（数据出口）
      │                  │                   │
   beats / file       grok / mutate        elasticsearch
   kafka / http       date / json          kafka / stdout
   jdbc / tcp         geoip / dissect      file / s3
```

| 阶段 | 职责 | 常用插件 |
|------|------|------|
| input | 从哪收数据 | beats、file、kafka、http、jdbc、tcp、stdin |
| filter | 如何加工 | grok、mutate、date、json、dissect、geoip、useragent |
| output | 发到哪 | elasticsearch、kafka、stdout、file、http |

### 1.1 事件在管道中的流转

```text
原始日志（一行文本）
   ↓ input codec 解码
Event（字段集合：message/@timestamp/host）
   ↓ filter 链（按顺序逐个执行）
解析、清洗、富化后的 Event
   ↓ output 分发
写入 ES / 转发 Kafka / 输出文件
```

> 🎯 核心认知：**管道里流动的是 Event（事件），不是字符串**——filter 做的是「字段增删改查」，一切操作围绕字段展开。

---

## 2. 配置语法基础

### 2.1 基本结构

```ruby
# 注释用 # 号
input {
  beats { port => 5044 }
}

filter {
  grok {
    match => { "message" => "%{TIMESTAMP_ISO8601:log_time}" }
  }
}

output {
  elasticsearch {
    hosts => ["http://localhost:9200"]
    index => "logs-%{+YYYY.MM.dd}"
  }
}
```

### 2.2 语法要点

| 语法 | 说明 | 示例 |
|------|------|------|
| 区块 | `plugin { 参数 => 值 }` | `grok { match => ... }` |
| 字符串 | 双引号 | `"app-logs"` |
| 数组 | 方括号 | `hosts => ["a:9200", "b:9200"]` |
| 哈希 | 花括号 | `match => { "message" => "..." }` |
| 布尔 | true/false | `overwrite => true` |
| 数字 | 直接写 | `port => 5044` |
| 字段引用 | 方括号 | `[level]`、`[log][time]` |
| 插值 | `%{字段名}` | `index => "logs-%{+YYYY.MM.dd}"` |

### 2.3 常见语法错误

| 错误 | 现象 | 修正 |
|------|------|------|
| 漏逗号/花括号 | 配置加载失败 | 用 `--config.test_and_exit` 校验 |
| 字符串忘引号 | 解析错误 | 字符串必须双引号 |
| 字段引用错 | 过滤不生效 | `[a][b]` 是嵌套，`[a.b]` 是带点字段名 |
| 条件写法错 | 条件永不成立 | `if [level] == "ERROR"` 空格严格 |

---

## 3. 事件（Event）与字段

### 3.1 事件的默认字段

| 字段 | 来源 | 说明 |
|------|------|------|
| `message` | input | 原始日志内容 |
| `@timestamp` | input | 处理时间（可用 date 插件覆盖为日志时间） |
| `@version` | 自动 | 事件版本（1） |
| `host` | input | 来源主机 |
| `tags` | filter 常用 | 标记数组（打标用） |
| `type` | 兼容字段 | 可自定义类型标记 |

### 3.2 字段操作的核心认知

```text
· 字段名区分大小写
· 嵌套字段：[a][b] 表示 a 对象下的 b
· 不存在的字段引用 = nil（不报错，需判空）
· filter 内新增字段 → 直接出现在输出中
· 删除字段 → mutate remove_field
```

---

## 4. 条件路由 if/else

### 4.1 条件语法

```ruby
if [level] == "ERROR" {
  # 走错误分支
} else if [level] in ["WARN", "INFO"] {
  # 走警告/信息分支
} else {
  # 兜底
}
```

### 4.2 常用比较操作符

| 操作符 | 含义 | 示例 |
|------|------|------|
| `==` / `!=` | 相等/不等 | `[level] == "ERROR"` |
| `<` `>` `<=` `>=` | 数值比较 | `[response_time] > 500` |
| `=~` / `!~` | 正则匹配 | `[message] =~ /支付失败/` |
| `in` / `not in` | 集合包含 | `[status] in [400, 500]` |
| `and` / `or` / `not` | 逻辑组合 | `[a] == 1 and [b] == 2` |
| 存在性 | 字段是否非空 | `if [field]` |

### 4.3 条件路由实战

```ruby
output {
  if [level] == "ERROR" {
    elasticsearch {
      hosts => ["http://es-error:9200"]
      index => "errors-%{+YYYY.MM.dd}"
    }
    # 错误同时发告警通道
    http {
      url => "http://alert-service:8080/notify"
      http_method => "post"
      body => '{"message": "%{message}"}'
    }
  } else {
    elasticsearch {
      hosts => ["http://es-main:9200"]
      index => "logs-%{+YYYY.MM.dd}"
    }
  }
}
```

> 🎯 路由是 Logstash 的「多输出」核心能力——**一个管道按条件分发到不同索引/通道**，这是 Ingest Pipeline 做不到的。

---

## 5. Codec 编解码器

### 5.1 常用 codec

| Codec | 作用 | 使用场景 |
|------|------|------|
| `plain` | 原样文本（默认） | 普通日志 |
| `json` | JSON 解析 | 结构化日志（SpringBoot 等） |
| `json_lines` | 每行一个 JSON | 容器/云日志 |
| `rubydebug` | 调试输出 | stdout 调试 |
| `multiline` | 多行合并 | Java 堆栈异常、SQL 多行 |
| `gzip_lines` | gzip 压缩行 | 压缩日志 |

### 5.2 multiline 合并 Java 异常堆栈

```ruby
input {
  file {
    path => "/app/logs/*.log"
    codec => multiline {
      pattern => "^\\s"                 # 缩进行是上一行继续
      negate => false
      what => "previous"                # 追加到前一行
    }
  }
}
```

> ⚠️ multiline 的性能：逐行正则判断，大日志量时是瓶颈；9.x 也可考虑 Filebeat 侧 multiline（更轻）。

---

## 6. 变量与引用

### 6.1 字段插值

```ruby
# %{字段名} 插值到字符串
index => "logs-%{+YYYY.MM.dd}"        # 日期插值
index => "app-%{service_name}"         # 字段插值
document_id => "%{request_id}"         # 幂等写入
```

### 6.2 环境变量引用

```ruby
# 从环境变量读取敏感配置
output {
  elasticsearch {
    user => "${ES_USER}"
    password => "${ES_PASSWORD}"
  }
}
# 启动时传：ES_USER=logstash_internal bin/logstash -f pipeline.conf
```

### 6.3 引用嵌套字段

```ruby
# 嵌套字段引用
filter {
  mutate {
    add_field => { "top_level" => "%{[nested][inner]}" }
  }
}
```

---

## 7. 调试管道：stdin/stdout 三件套

### 7.1 最小调试管道

```ruby
# debug.conf
input { stdin {} }
filter {
  # 先测 filter 逻辑
  grok { match => { "message" => "%{LOGLEVEL:level} %{GREEDYDATA:content}" } }
}
output { stdout { codec => rubydebug } }
```

```bash
# 运行并手动输入测试
bin/logstash -f debug.conf
# 输入：ERROR 订单超时
# 输出：rubydebug 格式（含解析出的 level/content 字段）
```

### 7.2 调试技巧

| 技巧 | 说明 |
|------|------|
| 语法校验 | `--config.test_and_exit`（上线前必跑） |
| 单 filter 测试 | 只留一个 filter 逐步加 |
| rubydebug 输出 | 完整字段视图 |
| `stdout { codec => dots }` | 只打点号，测吞吐不刷屏 |
| 查看中间状态 | 在 filter 链中间插入 stdout 输出观察 |

---

## 8. 管道设计最佳实践

### 8.1 设计原则

| # | 原则 | 说明 |
|:---:|------|------|
| 1 | 一个管道一个职责 | 日志管道与指标管道分离 |
| 2 | Grok 从粗到细 | 先用 GREEDYDATA 兜底，再逐步细化 |
| 3 | 日期尽早 | date 插件尽早覆盖 @timestamp |
| 4 | 尽量少嵌套 | 嵌套字段检索麻烦 |
| 5 | 统一字段命名 | `service_name` 全栈一致 |
| 6 | 敏感字段脱敏 | mutate gsub 或专用脱敏插件 |
| 7 | tags 打标 | 处理失败的标记 `_grokparsefailure` |

### 8.2 失败处理规范

```text
grok 失败 → 自动打 tag: _grokparsefailure
处理：
  · 可保留原始 message 供人工排查
  · 路由 _grokparsefailure 到单独索引（failures-*）
  · 设置收敛后告警：解析失败率过高 → 配置问题
```

> 🎯 **核心要点**：Logstash 配置 = **三段式（input/filter/output）+ 条件路由 + 编解码器**。写管道的标准流程：stdin/stdout 调试三件套跑通 → 加 filter 逐段验证 → 切真实 input/output → 上线前 `--config.test_and_exit`。记住三个机制：**Event 是字段集合、if/else 是多路分发、multiline 处理堆栈**。

---

**下一模块**：[03-Logstash输入与输出插件](03-Logstash输入与输出插件.md) | **返回总览**：[00-Logstash专题总览](00-Logstash专题总览.md)
