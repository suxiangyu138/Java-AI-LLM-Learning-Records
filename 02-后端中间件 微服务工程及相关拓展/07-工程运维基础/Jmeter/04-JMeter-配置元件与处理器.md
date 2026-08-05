# JMeter 配置元件与处理器

> 🔧 CSV Data Set Config 数据驱动、HTTP Header/Cookie Manager、User Defined Variables、前后置处理器 —— 构建真实压测场景

---

## 📚 目录

1. [配置元件 Config Element](#1-配置元件-config-element)
2. [CSV Data Set Config](#2-csv-data-set-config)
3. [HTTP Manager 三件套](#3-http-manager-三件套)
4. [前置处理器 Pre-Processor](#4-前置处理器-pre-processor)
5. [后置处理器 Post-Processor](#5-后置处理器-post-processor)
6. [定时器 Timer](#6-定时器-timer)

---

## 1. 配置元件 Config Element

```text
Config Element = 为 Sampler 提供配置/数据/环境

核心配置元件一览：
  ├── CSV Data Set Config         → 数据文件驱动（最重要）
  ├── HTTP Request Defaults       → HTTP 请求默认值
  ├── HTTP Header Manager         → 请求头管理
  ├── HTTP Cookie Manager         → Cookie 自动管理
  ├── HTTP Cache Manager          → 模拟浏览器缓存
  ├── JDBC Connection Config      → 数据库连接池
  ├── User Defined Variables      → 用户自定义变量
  ├── Random Variable             → 生成随机变量
  └── Counter                     → 递增计数器

执行时机：仅执行一次（线程启动时）
作用范围：从声明位置向下，直到作用域结束
```

---

## 2. CSV Data Set Config

### 2.1 核心配置

```text
CSV Data Set Config = JMeter 数据驱动的核心！

配置项：
  Filename：users.csv（相对路径基于 .jmx 文件）

  File encoding：UTF-8

  Variable Names：username,password,role,expected
    → CSV 列名，逗号分隔
    → 引用：${username}, ${password}

  Delimiter：,  (逗号分隔) 或 \t (Tab)

  Allow quoted data?：True  → 支持 "值,包含,逗号" 的引用格式

  Recycle on EOF?：True → 读完从头循环；False → 线程停止
  Stop thread on EOF?：False（与 Recycle 互斥）

  Sharing mode：
    All threads     → 所有线程共享（每个线程取不同行）
    Current thread group → 每个线程组独立
    Current thread  → 每个线程独立副本
```

### 2.2 CSV 数据文件

```csv
// users.csv
username,password,role,expectedStatus
admin,admin123,ADMIN,200
normal_user,pass456,USER,200
guest,guest789,GUEST,200
invalid_user,wrong,UNKNOWN,401
```

```text
在 HTTP Request 中使用：
  Body Data:
  {
    "username": "${username}",
    "password": "${password}"
  }

  Response Assertion:
  Response Code: ${expectedStatus}
```

### 2.3 常用 Sharing Mode 场景

| 场景 | Sharing Mode | 说明 |
|------|:----------:|------|
| 100线程×10行数据，每人不同 | All threads | 轮流取，第101个线程会回到第1行 |
| 100线程×100行以上，每人固定 | All threads | 每行被用一次 |
| 参数化用户登录 | All threads | 不同线程用不同账号（避免锁） |
| 每线程从固定数据池取 | Current thread | 线程独立迭代数据 |

---

## 3. HTTP Manager 三件套

### 3.1 HTTP Header Manager

```text
配置请求头（JSON API 必配）：
  ┌─────────────────┬───────────────────────────┐
  │ Content-Type     │ application/json          │
  │ Authorization    │ Bearer ${authToken}       │
  │ Accept           │ application/json          │
  │ X-Request-Id     │ ${__UUID()}              │
  └─────────────────┴───────────────────────────┘

作用域：
  → Test Plan 级别 → 全局默认请求头
  → Thread Group 级别 → 覆盖全局
  → Sampler 子级 → 仅此请求（追加/覆盖）
```

### 3.2 HTTP Cookie Manager

```text
自动管理 Cookie：
  ✅ 接收 Set-Cookie → 自动存储
  ✅ 后续请求自动带 Cookie
  ✅ 按域名隔离
  ✅ 模拟浏览器行为

配置项：
  Clear cookies each iteration? → 每轮迭代清空
  Cookie Policy: standard
```

### 3.3 HTTP Cache Manager

```text
模拟浏览器缓存行为：
  → 缓存 GET 请求的静态资源
  → 条件请求（If-Modified-Since）
  → 更真实模拟用户浏览器流量

⚠️ 注意：
  → 压测静态资源本身时不要开启
  → 混合压测时根据需要开启
```

---

## 4. 前置处理器 Pre-Processor

```text
Pre-Processor = 在 Sampler 执行前运行

常用场景：
  ├── 生成动态签名
  ├── 计算参数值
  ├── 修改请求体
  └── 设置动态 Header
```

```groovy
// JSR223 PreProcessor（Groovy）
// 在发送请求前动态计算签名

import java.security.MessageDigest;

def timestamp = String.valueOf(System.currentTimeMillis());
def secret = vars.get("apiSecret");
def params = "appId=123&timestamp=${timestamp}";
def sign = MessageDigest.getInstance("MD5")
    .digest((params + secret).getBytes("UTF-8"))
    .encodeHex()
    .toString();

vars.put("timestamp", timestamp);
vars.put("sign", sign);
```

---

## 5. 后置处理器 Post-Processor

### 5.1 常用后置处理器

| 处理器 | 功能 | 场景 |
|--------|------|------|
| **Regular Expression Extractor** | 正则提取响应内容 | 提取 token, sessionId |
| **JSON Extractor** | JSON Path 提取 | REST API 响应提取 ✅ |
| **CSS/JQuery Extractor** | CSS 选择器提取 HTML | 网页内容提取 |
| **XPath Extractor** | XML 路径提取 | SOAP 响应 |
| **Boundary Extractor** | 左右边界提取 | 简单字符串提取 |
| **JSR223 PostProcessor** | 自定义脚本 | 复杂提取逻辑 |
| **Debug PostProcessor** | 打印变量 | 调试 |

### 5.2 JSON Extractor（最常用）

```text
配置示例：
  Names of created variables: userId,token,status
  JSON Path expressions:
    $.data.id
    $.data.accessToken
    $.code
  Match No.: 1（取第一个匹配）
  Default Values: NOT_FOUND,NOT_FOUND,0

响应 JSON:
  {
    "code": 200,
    "data": {
      "id": 12345,
      "accessToken": "eyJhbGci..."
    }
  }

提取后变量：
  ${userId} = 12345
  ${token}  = eyJhbGci...
  ${status} = 200

后续请求中直接用 ${userId}, ${token}！
```

### 5.3 Regular Expression Extractor

```text
适用场景：响应非 JSON（HTML/纯文本/Token 在 Headers）

配置示例：
  Reference Name: csrfToken
  Regular Expression: name="_csrf" value="(.+?)"
  Template: $1$
  Match No.: 1

  → 从 HTML 中提取 CSRF Token
```

---

## 6. 定时器 Timer

```text
Timer = 控制请求之间的等待时间

常用 Timer：
  Constant Timer            → 固定延迟（如每次等待 500ms）
  Uniform Random Timer      → 随机延迟（min ~ max）
  Gaussian Random Timer     → 正态分布延迟
  Constant Throughput Timer → 控制固定吞吐量（如限定 100 QPS）
  Synchronizing Timer       → 集合点（同时释放请求）
  Poisson Random Timer      → 泊松分布延迟

使用建议：
  ✅ Think Time：Gaussian Random Timer（模拟真实用户）
  ✅ 限流目标：Constant Throughput Timer
  ✅ 瞬时并发：Synchronizing Timer（如秒杀场景）
```

---

> 🎯 **核心要点**：**CSV Data Set Config** 是数据驱动的灵魂，**JSON Extractor** 是链式请求的枢纽，**HTTP Header Manager** 是请求头的管家。配置元件执行一次，处理器每次请求都跑。

---

*创建于：2026年7月*
