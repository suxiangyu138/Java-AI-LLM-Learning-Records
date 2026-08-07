# Logstash 过滤插件与数据清洗
> 管道的中段才是价值所在——Grok 正则解析、Dissect 快速切分、Mutate 字段手术、Date 时间校准、JSON 结构化、GeoIP 地理富化。本文件覆盖最常用的过滤插件与实战模式

## 目录
1. [过滤插件全景](#1-过滤插件全景)
2. [Grok：正则解析之王](#2-grok正则解析之王)
3. [Dissect：快速切分](#3-dissect快速切分)
4. [Mutate：字段手术刀](#4-mutate字段手术刀)
5. [Date：时间校准](#5-date时间校准)
6. [JSON：结构化解析](#6-json结构化解析)
7. [富化插件：GeoIP / UserAgent / Translate](#7-富化插件geoip--useragent--translate)
8. [Grok 实战：Nginx 访问日志](#8-grok-实战nginx-访问日志)
9. [解析失败处理与调优](#9-解析失败处理与调优)

---

## 1. 过滤插件全景

| 插件 | 职责 | 使用频率 |
|------|------|:---:|
| grok | 正则解析（最强最常用） | ★★★★★ |
| dissect | 定界符快速切分（快但简单） | ★★★★ |
| mutate | 字段增删改、类型转换、替换 | ★★★★★ |
| date | 时间字符串 → @timestamp | ★★★★★ |
| json | JSON 字符串 → 对象 | ★★★★ |
| geoip | IP → 地理位置 | ★★★ |
| useragent | UA 字符串解析 | ★★★ |
| translate | 字典映射 | ★★★ |
| ruby | 任意脚本逻辑（兜底） | ★★ |

> 🎯 解析性能排序：**Dissect（最快）> Grok > Ruby（最慢但最灵活）**——能用 dissect 不用 grok，能用 grok 不用 ruby。

---

## 2. Grok：正则解析之王

### 2.1 基本用法

```ruby
filter {
  grok {
    match => { "message" => "%{TIMESTAMP_ISO8601:log_time} %{LOGLEVEL:level} %{GREEDYDATA:content}" }
  }
}
```

### 2.2 常用内置模式

| 模式 | 匹配内容 | 示例 |
|------|------|------|
| `%{IP:ip}` | IP 地址 | 192.168.1.1 |
| `%{TIMESTAMP_ISO8601:time}` | ISO 时间 | 2026-08-07T14:00:00Z |
| `%{LOGLEVEL:level}` | 日志级别 | ERROR/WARN/INFO |
| `%{NUMBER:num:int}` | 数字（可转 int） | 500 |
| `%{WORD:word}` | 单词 | ok |
| `%{NOTSPACE:value}` | 非空格 | /api/order |
| `%{URIPATH:path}` | URL 路径 | /api/order |
| `%{IPORHOST:host}` | IP 或主机名 | app-01 |
| `%{GREEDYDATA:rest}` | 贪婪匹配到末尾 | 任意 |
| `%{DATA:val}` | 任意非贪婪 | 任意 |

### 2.3 自定义模式

```ruby
# 方式一：filter 内定义
filter {
  grok {
    patterns_dir => ["/etc/logstash/patterns"]
    match => { "message" => "%{MY_PATTERN:field}" }
  }
}

# 方式二：patterns 文件（/etc/logstash/patterns/extra）
# MY_ORDER_ID \d{12,16}
```

### 2.4 Grok 的解析过程

```text
message: "2026-08-07T14:00:01Z ERROR 订单超时 request_id=20260807140001"
   ↓ grok 按模式逐个匹配
log_time: "2026-08-07T14:00:01Z"
level: "ERROR"
content: "订单超时 request_id=20260807140001"
   ↓ 可继续加模式细化（建议一个 message 最多 2-3 个 grok）
```

> ⚠️ Grok 性能注意：**模式越多越慢**；`GREEDYDATA` 放最后；长 message 用 dissect 更优。

---

## 3. Dissect：快速切分

### 3.1 基本用法

```ruby
filter {
  dissect {
    mapping => {
      "message" => "%{time} %{level} %{content}"
    }
  }
}
```

### 3.2 Grok vs Dissect 对比

| 维度 | Grok | Dissect |
|------|------|:---:|
| 原理 | 正则匹配 | 定界符切分 |
| 速度 | 慢（正则引擎） | **快（字符串查找）** |
| 灵活性 | 高（模式库） | 低（必须有固定分隔符） |
| 容错 | 部分匹配 | 缺定界符即失败 |
| 适用 | 复杂/可变格式 | **固定格式日志（首选）** |

```text
选型规则：
  日志格式固定（nginx/网关/自研统一格式）→ dissect
  格式多变/需模式匹配 → grok
  最佳实践：先 dissect 快速切，再用 grok 细化复杂字段
```

---

## 4. Mutate：字段手术刀

### 4.1 常用操作

```ruby
filter {
  mutate {
    # 重命名字段
    rename => { "old_name" => "new_name" }

    # 删除字段（敏感字段脱敏）
    remove_field => ["password", "token", "[user][id_card]"]

    # 类型转换
    convert => {
      "response_time" => "integer"
      "price" => "float"
      "enabled" => "boolean"
    }

    # 字符串替换（脱敏手机号）
    gsub => [ "phone", "\d{4}$", "****" ]

    # 大小写转换
    uppercase => ["level"]
    lowercase => ["service_name"]

    # 拼接/拆分
    join => { "tags" => "," }
    split => { "ips" => "," }

    # 复制字段
    copy => { "@timestamp" => "ingest_time" }

    # 追加
    add_field => { "environment" => "prod" }
    add_tag => ["processed"]
  }
}
```

### 4.2 敏感信息脱敏实战

```ruby
filter {
  mutate {
    gsub => [
      "message", "(password[=: ])[^\\s,;]+", "\1***",
      "message", "(token[=: ])[^\\s,;]+", "\1***"
    ]
  }
}
# 效果：password=123456 → password=***
```

> 🎯 数据合规要点：**日志入库前完成脱敏**（手机号/身份证/密码/Token），比入库后清理更可靠。

---

## 5. Date：时间校准

### 5.1 为什么必须用 date 插件

```text
问题：@timestamp 默认 = Logstash 处理时间，不是日志产生时间
后果：日志延迟入库时，Discover 按 @timestamp 排序会错乱
解决：用 date 插件把日志自带时间覆盖到 @timestamp
```

### 5.2 配置示例

```ruby
filter {
  date {
    match => ["log_time", "ISO8601", "yyyy-MM-dd HH:mm:ss.SSS"]
    target => "@timestamp"
    timezone => "Asia/Shanghai"        # 日志时区
  }
}
```

| 参数 | 说明 |
|------|------|
| match | [字段, 格式...] 依次尝试 |
| target | 写入目标（默认 @timestamp） |
| timezone | 解析时区（不写默认 UTC） |
| locale | 月份名称等本地化 |

### 5.3 常用时间格式

| 格式 | 匹配 |
|------|------|
| `ISO8601` | 2026-08-07T14:00:01Z |
| `yyyy-MM-dd HH:mm:ss` | 2026-08-07 14:00:01 |
| `yyyy-MM-dd HH:mm:ss.SSS` | 带毫秒 |
| `EEE MMM dd HH:mm:ss yyyy` | Apache 格式 |
| `UNIX` / `UNIX_MS` | 时间戳秒/毫秒 |

---

## 6. JSON：结构化解析

### 6.1 基础用法

```ruby
filter {
  json {
    source => "message"          # 解析 message 里的 JSON
    target => "log"              # 放到 log 对象下（可选）
  }
}
```

### 6.2 应用日志（JSON 格式）场景

```ruby
# SpringBoot 输出 JSON 日志时
# message: {"ts":"2026-08-07T14:00:01Z","level":"ERROR","service":"order","msg":"timeout","requestId":"abc123"}

filter {
  json {
    source => "message"
  }
  date {
    match => ["ts", "ISO8601"]
  }
  mutate {
    remove_field => ["message"]   # 原始 JSON 已被解析，可移除
  }
}
```

### 6.3 JSON 解析的坑

| 坑 | 处理 |
|------|------|
| message 非 JSON | 解析失败打 `_jsonparsefailure` tag |
| 部分字段缺失 | 解析后字段为 nil，检索时注意 |
| 嵌套过深 | 拆平（mutate + 引用）或保留嵌套 |
| 大 JSON | 性能考虑先 dissect 切再 json |

> 💡 结构化日志（JSON）是日志治理的方向——**应用侧输出 JSON 格式日志，Logstash 侧直接 json 解析，减少 grok 负担**。

---

## 7. 富化插件：GeoIP / UserAgent / Translate

### 7.1 GeoIP（IP 定位）

```ruby
filter {
  geoip {
    source => "client_ip"
    # fields => ["city_name", "country_name", "location"]
  }
}
# 输出：client_ip 增加 geoip.city_name / geoip.country_name / geoip.location
# 配合 Kibana region_map 地图可视化
```

### 7.2 UserAgent（浏览器解析）

```ruby
filter {
  useragent {
    source => "user_agent"
    target => "ua"
  }
}
# 输出：ua.name（Chrome）/ ua.os（Windows）/ ua.major（版本）
```

### 7.3 Translate（字典映射）

```ruby
filter {
  translate {
    source => "[status_code]"
    target => "[status_desc]"
    dictionary_path => "/etc/logstash/dict/status.yml"
    fallback => "UNKNOWN"          # 未命中默认值
  }
}
# status.yml:
#  200: OK
#  500: Server Error
```

---

## 8. Grok 实战：Nginx 访问日志

### 8.1 Nginx 默认格式

```text
192.168.1.5 - - [07/Aug/2026:14:00:01 +0800] "GET /api/order/123 HTTP/1.1" 200 532 "-" "Mozilla/5.0 ..."
```

### 8.2 完整解析管道

```ruby
filter {
  grok {
    match => { "message" => "%{IPORHOST:client_ip} - %{DATA:user} \[%{HTTPDATE:access_time}\] \"%{WORD:method} %{URIPATH:uri} HTTP/%{NUMBER:http_version}\" %{NUMBER:status:int} %{NUMBER:bytes:int} \"%{DATA:referer}\" \"%{DATA:user_agent}\"" }
  }

  date {
    match => ["access_time", "dd/MMM/yyyy:HH:mm:ss Z"]
    timezone => "Asia/Shanghai"
  }

  useragent {
    source => "user_agent"
    target => "ua"
  }

  geoip {
    source => "client_ip"
  }

  mutate {
    convert => { "status" => "integer" }
    remove_field => ["user_agent"]
  }
}
```

### 8.3 解析后的字段（ES 检索视角）

| 字段 | 含义 | 用途 |
|------|------|------|
| client_ip | 客户端 IP | 地域分析 |
| method / uri | 请求方法/路径 | 接口分析 |
| status | 状态码 | 错误率 |
| bytes | 响应大小 | 流量分析 |
| ua.name / ua.os | 浏览器/系统 | 终端分析 |
| geoip.city_name | 城市 | 地图可视化 |

> 🎯 实战要点：**解析一次、富化三件（时间/UA/IP）**——日期覆盖 @timestamp、UA 拆浏览器、IP 转地理，一套管道让原始日志变成可分析的结构化数据。

---

## 9. 解析失败处理与调优

### 9.1 失败标签体系

| 失败标签 | 含义 |
|------|------|
| `_grokparsefailure` | grok 未匹配 |
| `_jsonparsefailure` | json 解析失败 |
| `_dateparsefailure` | date 解析失败 |
| `_dissectfailure` | dissect 切分失败 |

### 9.2 失败处理规范

```ruby
# 路由解析失败到独立索引，便于发现配置问题
output {
  if "_grokparsefailure" in [tags] {
    elasticsearch {
      index => "parse-failures-%{+YYYY.MM.dd}"
    }
  } else {
    elasticsearch {
      index => "logs-%{+YYYY.MM.dd}"
    }
  }
}
```

### 9.3 性能调优

| 场景 | 手段 |
|------|------|
| grok 慢 | 模式精简、dissect 替代、固定格式用 JSON |
| 长 message | 先截断（mutate truncate）再解析 |
| 多 grok 叠加 | 合并为一个大 grok（一次遍历） |
| 匹配顺序 | 常用分支放前（条件 if 包 grok） |
| 解析失败率高 | 检查模式与日志格式漂移（配置版本化） |

> 🎯 **核心要点**：过滤插件五件套——**Grok（复杂解析）、Dissect（快速切分）、Mutate（字段手术）、Date（时间校准）、JSON（结构化）**，再加富化三兄弟（GeoIP/UA/Translate）。工程铁律：**固定格式用 dissect、@timestamp 必须用 date 覆盖、敏感字段入库前脱敏、解析失败路由独立索引盯配置漂移**。

---

**下一模块**：[05-Logstash性能调优与可靠传输](05-Logstash性能调优与可靠传输.md) | **返回总览**：[00-Logstash专题总览](00-Logstash专题总览.md)
