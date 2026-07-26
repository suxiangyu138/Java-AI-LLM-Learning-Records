# JMeter 压力测试从入门到精通
> Apache JMeter 是一款纯 Java 开源的压力测试工具，专为测量系统在高并发下的性能表现而设计，支持 HTTP、HTTPS、JDBC、JMS、FTP、TCP 等多种协议，是 Java 后端工程师进行性能压测的首选工具。

## 目录

1. [JMeter 概述与架构](#1-jmeter-概述与架构)
2. [安装与 GUI 概览](#2-安装与-gui-概览)
3. [测试计划结构](#3-测试计划结构)
4. [线程组配置](#4-线程组配置)
5. [HTTP 请求采样器](#5-http-请求采样器)
6. [监听器](#6-监听器)
7. [断言](#7-断言)
8. [定时器](#8-定时器)
9. [CSV 数据驱动测试](#9-csv-数据驱动测试)
10. [HTTP 管理器](#10-http-管理器)
11. [关联与提取器](#11-关联与提取器)
12. [HTML 报告生成](#12-html-报告生成)
13. [分布式测试](#13-分布式测试)
14. [CLI 命令行模式](#14-cli-命令行模式)
15. [常见测试场景](#15-常见测试场景)
16. [最佳实践](#16-最佳实践)
17. [JMeter vs 其他压测工具](#17-jmeter-vs-其他压测工具)
18. [完整示例：SpringBoot REST API 压测](#18-完整示例springboot-rest-api-压测)

---

## 1. JMeter 概述与架构

### 1.1 什么是 JMeter

> Apache JMeter 是 Apache 基金会旗下的纯 Java 桌面应用，最初为 Web 应用性能测试而设计，后扩展至多种协议。它的核心能力是 **模拟大量并发用户向目标系统发送请求，收集并分析响应数据**，从而评估系统的吞吐量、响应时间、错误率等关键性能指标。

### 1.2 JMeter 核心组件架构

JMeter 的测试执行基于以下八大核心组件，它们协同工作构成一个完整的测试计划：

| 组件 | 英文名称 | 职责描述 | 示例 |
|------|----------|----------|------|
| **线程组** | Thread Group | 模拟用户池，定义并发数、循环次数、启动时长 | 100 线程、Ramp-Up 10s |
| **采样器** | Sampler | 发送特定协议的请求并等待响应 | HTTP Request、JDBC Request |
| **监听器** | Listener | 收集并展示测试结果数据 | View Results Tree、Aggregate Report |
| **定时器** | Timer | 控制请求之间的延迟，模拟用户思考时间 | Constant Timer、Gaussian Random Timer |
| **断言** | Assertion | 验证响应是否符合预期，标记失败请求 | Response Assertion、JSON Assertion |
| **配置元件** | Config Element | 提供请求所需的静态配置数据 | CSV Data Set Config、HTTP Header Manager |
| **前置处理器** | Pre-Processor | 在采样器执行前进行预处理 | User Parameters、JSR223 PreProcessor |
| **后置处理器** | Post-Processor | 在采样器执行后提取响应数据（关联） | Regular Expression Extractor、JSON Extractor |

### 1.3 测试执行流程

```
Thread Group
    |
    ├── Config Element (初始化配置)
    ├── Pre-Processor (请求前处理)
    ├── Timer (等待指定时间)
    ├── Sampler (发送请求)
    ├── Post-Processor (提取响应数据)
    ├── Assertion (验证响应)
    └── Listener (记录/展示结果)
```

> 💡 **组件执行顺序**：同一作用域内，各组件按上述顺序执行。若多个同类型组件存在，则按它们在测试计划中的出现顺序执行。

---

## 2. 安装与 GUI 概览

### 2.1 环境要求

| 依赖 | 版本要求 |
|------|----------|
| JDK | 8+（JMeter 5.x 需要 JDK 8+，JMeter 5.5+ 需要 JDK 9+） |
| 内存 | 建议至少 4GB（GUI 模式）+ 2GB（CLI 模式） |

### 2.2 安装步骤

```bash
# 1. 下载 JMeter
# 访问 https://jmeter.apache.org/download_jmeter.cgi
# 下载 apache-jmeter-5.x.zip 或 apache-jmeter-5.x.tgz

# 2. 解压到安装目录
# Windows: 解压到 C:\JMeter\apache-jmeter-5.6.3
# Linux/macOS:
tar -xzf apache-jmeter-5.6.3.tgz -C /opt/

# 3. 可选：配置环境变量
# Windows: 添加 %JMETER_HOME%/bin 到 PATH
# Linux/macOS: export PATH=$PATH:/opt/apache-jmeter-5.6.3/bin

# 4. 启动 GUI
jmeter
# 或 jmeter.bat (Windows)
```

### 2.3 GUI 界面概览

```
┌──────────────────────────────────────────────────────┐
│ 菜单栏: File | Edit | Search | Run | Options | Help  │
├──────────────────────────────────────────────────────┤
│ 工具栏: New | Open | Save | Start | Stop | Clear ... │
├────────────────┬─────────────────────────────────────┤
│                │                                     │
│  树形面板       │     内容面板                         │
│  (Test Plan)   │     (当前选中组件的配置)              │
│    ├── Thread  │                                     │
│    │   Group   │                                     │
│    ├── HTTP    │                                     │
│    │   Request │                                     │
│    └── Listener│                                     │
│                │                                     │
├────────────────┴─────────────────────────────────────┤
│ 日志面板: 显示运行日志、错误信息                        │
└──────────────────────────────────────────────────────┘
```

> ⚠️ **重要**：JMeter GUI 模式仅用于测试脚本开发和调试。**切勿**在 GUI 模式下执行正式压力测试，否则会因 GUI 本身消耗大量内存而导致测试结果不准确。

### 2.4 GUI 模式快捷键

| 快捷键 | 功能 |
|--------|------|
| `Ctrl + N` | 新建测试计划 |
| `Ctrl + S` | 保存测试计划 |
| `Ctrl + O` | 打开测试计划 |
| `Ctrl + R` | 启动测试 |
| `Ctrl + E` | 停止测试 |
| `Ctrl + Shift + E` | 立即停止（不等待当前请求） |
| `Ctrl + Del` | 清除所有数据 |

---

## 3. 测试计划结构

### 3.1 测试计划层级

一个完整的 JMeter 测试计划采用树形层级结构：

```
Test Plan (.jmx 文件)
│
├── User Defined Variables         # 用户自定义变量（全局变量）
├── HTTP Request Defaults          # HTTP 请求默认值
├── HTTP Header Manager            # 公共请求头
├── HTTP Cookie Manager            # Cookie 管理
│
├── Thread Group (用户组1)
│   ├── CSV Data Set Config        # 数据驱动
│   ├── HTTP Header Manager        # 该线程组专用请求头
│   │
│   ├── Sampler: HTTP Request      # 登录请求
│   │   ├── Regular Expression Extractor  # 提取 Token
│   │   └── Response Assertion     # 校验登录响应
│   │
│   ├── Sampler: HTTP Request      # 业务请求
│   │   ├── JSON Extractor         # 提取业务数据
│   │   ├── Duration Assertion     # 超时断言
│   │   └── Response Assertion     # 校验业务响应
│   │
│   ├── Constant Timer             # 请求间等待
│   ├── Synchronizing Timer        # 同步定时器
│   │
│   └── Listener
│       ├── View Results Tree
│       ├── Summary Report
│       └── Aggregate Report
│
├── Thread Group (用户组2)
│   └── ...
│
└── Listener (全局)
    └── Simple Data Writer         # 全局结果写入文件
```

### 3.2 测试计划属性

| 属性 | 说明 | 推荐值 |
|------|------|--------|
| `Name` | 测试计划名称 | 有意义的业务名称 |
| `Comment` | 注释说明 | 描述测试场景 |
| `User Defined Variables` | 全局变量定义 | URL、端口、账号等 |
| `Functional Test Mode` | 是否存储完整响应数据 | `false`（提高性能） |
| `Run Thread Groups consecutively` | 线程组是否串行执行 | 默认 `false`（并行） |

> 💡 **建议**：将频繁变化的配置（如 target URL、端口号、超时时间）定义为变量，方便不同环境切换。

---

## 4. 线程组配置

### 4.1 线程组参数详解

线程组（Thread Group）是 JMeter 中最核心的执行单元，它定义了虚拟用户的行为模式。

| 参数 | 含义 | 示例 | 说明 |
|------|------|------|------|
| `Number of Threads (users)` | 线程数（虚拟用户数） | `100` | 模拟 100 个并发用户 |
| `Ramp-Up Period (seconds)` | 线程启动时间 | `10` | 在 10 秒内启动全部 100 个线程 |
| `Loop Count` | 循环次数 | `5` | 每个线程执行 5 次请求 |
| `Same user on each iteration` | 每次迭代是否使用相同用户 | `true` | 配合 Cookie Manager 使用 |
| `Scheduler Configuration` | 调度器设置 | - | 设置测试持续时间 |

### 4.2 线程数策略

| 策略 | 线程数 | Ramp-Up | Loop Count | 适用场景 |
|------|--------|---------|------------|----------|
| 冒烟测试 | 1-5 | 1s | 1 | 功能验证、快速检查 |
| 基准测试 | 10-50 | 5-10s | 10-100 | 获取基线性能数据 |
| 负载测试 | 100-500 | 30-60s | -1（持续） | 评估系统在预期负载下的表现 |
| 压力测试 | 500-5000 | 60-300s | -1（持续） | 找到系统瓶颈崩溃点 |
| 极限测试 | 逐步递增 | 自定义 | -1（持续） | 确定系统最大承载能力 |

### 4.3 Ramp-Up 计算公式

```
Ramp-Up = (总线程数 × 目标请求间隔) / 目标QPS

# 示例：1000 线程，目标 QPS 为 200
# 请求间隔 = 1000ms / 200 = 5ms
# Ramp-Up = (1000 × 5) / 1000 = 5s (约)
```

> 💡 **经验法则**：Ramp-Up 设为线程数 / 10，如 500 线程则 Ramp-Up 为 50 秒。观察系统负载曲线是否平滑上升。

### 4.4 调度器配置

| 参数 | 含义 | 配置示例 |
|------|------|----------|
| `Duration (seconds)` | 测试持续时长 | `300`（持续 5 分钟） |
| `Startup delay (seconds)` | 启动延迟 | `0` |

```text
# 常用调度配置
# 场景一：固定持续时间
Loop Count = √ (勾选 Forever)
Duration = 600  # 持续 10 分钟

# 场景二：固定请求次数
Loop Count = 100
Duration = (留空)
```

---

## 5. HTTP 请求采样器

### 5.1 HTTP Request 配置

HTTP Request Sampler 是压测中最常用的采样器，支持 GET、POST、PUT、DELETE、PATCH 等 HTTP 方法。

#### 基础配置

| 参数 | 说明 | 示例 |
|------|------|------|
| `Protocol` | 协议 | `https` |
| `Server Name or IP` | 服务器地址 | `api.example.com` |
| `Port Number` | 端口号 | `443` / `8080` |
| `Method` | HTTP 方法 | `GET` / `POST` |
| `Path` | 请求路径 | `/api/users/{id}` |
| `Content encoding` | 编码 | `UTF-8` |
| `Auto Redirects` | 自动重定向 | `false`（压测时建议关闭） |
| `Follow Redirects` | 跟随重定向 | `true` |
| `Use KeepAlive` | 使用长连接 | `true` |

### 5.2 GET 请求

#### 带 Path 参数的 GET

```text
# JMeter 配置
Protocol: https
Server: api.example.com
Port: 443
Method: GET
Path: /api/users/${userId}

# 变量定义
userId = 1001
```

#### 带 Query 参数的 GET

```text
# 方式一：在 Path 中直接带参数
Path: /api/users?page=${page}&size=${size}

# 方式二：使用 Parameters 参数表（推荐）
Path: /api/users
Parameters:
  Name      Value     Encode?
  page      ${page}   ✓
  size      ${size}   ✓
  sort      name,asc  ✓
```

### 5.3 POST 请求

#### 表单参数 (Form Data)

```text
Path: /api/login
Method: POST
Parameters:
  Name        Value          Encode?
  username    admin@test.com  ✓
  password    password123    ✓
```

#### JSON 请求体 (Body Data)

```text
Path: /api/users
Method: POST
Content-Type: application/json
Body Data:
{
  "name": "${userName}",
  "email": "${userEmail}",
  "role": "admin",
  "age": ${age}
}
```

#### Multipart 文件上传

```text
Path: /api/upload
Method: POST
Files Upload:
  File Path:      /tmp/test-file.pdf
  Parameter Name: file
  MIME Type:      application/pdf

Parameters:
  Name        Value
  description 测试文件上传
```

### 5.4 PUT / PATCH / DELETE

```text
# PUT 更新资源
Path: /api/users/${userId}
Method: PUT
Body Data:
{
  "name": "${userName}",
  "email": "${userEmail}"
}

# DELETE 删除资源
Path: /api/users/${userId}
Method: DELETE
```

### 5.5 HTTP Request Defaults

当测试计划中有多个 HTTP Sampler 共享相同配置（协议、服务器、端口）时，使用 **HTTP Request Defaults** 提取公共配置：

```
Test Plan
 └── HTTP Request Defaults            # 公共配置
     ├── Protocol: https
     ├── Server: api.example.com
     ├── Port: 443
     │
     ├── Thread Group
     │   ├── HTTP Request: GET /users     # 只需配置 Path
     │   ├── HTTP Request: POST /users    # 只需配置 Path + Body
     │   └── HTTP Request: GET /users/1   # 只需配置 Path
```

> 💡 **变量引用**：JMeter 中使用 `${variableName}` 引用变量，变量来源可以是 User Defined Variables、CSV Data Set Config、Extractor 等。

---

## 6. 监听器

### 6.1 监听器类型概览

| 监听器 | 用途 | 性能影响 | 适用阶段 |
|--------|------|----------|----------|
| **View Results Tree** | 查看单个请求/响应详情 | 🔴 极高 | 调试阶段 |
| **Summary Report** | 汇总统计（Avg, Min, Max, TPS） | 🟡 中 | 压测阶段（慎用） |
| **Aggregate Report** | 分区间统计 + 汇总 | 🟡 中 | 压测阶段（慎用） |
| **Graph Results** | 折线图展示 | 🔴 高 | 调试阶段 |
| **Simple Data Writer** | 写入 CSV 文件 | 🟢 低 | 压测阶段推荐 |
| **Backend Listener** | 发送结果到 InfluxDB 等 | 🟢 低 | 压测阶段推荐 |
| **Generate Summary Results** | 输出行摘要到日志 | 🟢 极低 | 压测阶段推荐 |

### 6.2 View Results Tree

用于 **调试阶段**，查看每个请求的详细数据：

```
Tree View:
 ├── Sample #1: GET /api/users ✅ (200)
 │   ├── Request: GET https://api.example.com/api/users
 │   ├── Response Headers: HTTP/1.1 200 OK
 │   │   ├── Content-Type: application/json
 │   │   └── Content-Length: 2345
 │   ├── Response Data: {"total":100, "data":[...]}
 │   └── Response Time: 23ms
 │
 └── Sample #2: POST /api/users ✅ (201)
     ├── Request: POST https://api.example.com/api/users
     ├── Response Headers: HTTP/1.1 201 Created
     └── ...
```

| 配置项 | 说明 | 建议 |
|--------|------|------|
| `Results File Name` | 保存结果的 CSV 文件路径 | 仅在需要时填写 |
| `Configure` | 选择展示内容 | 默认全选 |
| `Log/Display Only` | 过滤展示 | `Errors`（仅错误） |

> ⚠️ View Results Tree **会保存每个请求的完整 Request/Response 数据**，内存消耗巨大，**严禁**在正式压测时开启。

### 6.3 Summary Report

提供请求的统计摘要信息：

| 指标 | 含义 | 示例值 |
|------|------|--------|
| `# Samples` | 请求总数 | 50000 |
| `Average` | 平均响应时间 (ms) | 45 |
| `Min` | 最小响应时间 (ms) | 12 |
| `Max` | 最大响应时间 (ms) | 320 |
| `Std. Dev.` | 标准差 | 28.5 |
| `Error %` | 错误率 | 0.02% |
| `Throughput` | 吞吐量 (req/s) | 1250.3 |
| `Received KB/sec` | 接收速率 | 456.2 |
| `Sent KB/sec` | 发送速率 | 18.7 |

### 6.4 Aggregate Report

分区间统计，展示不同响应时间段的请求分布：

```
Aggregate Report
┌────────────┬──────┬──────┬──────┬──────┬───────────────┐
│ Label      │ #Samp│  Avg │  90% │  99% │ Throughput    │
├────────────┼──────┼──────┼──────┼──────┼───────────────┤
│ GET /users │ 25000│ 23ms │ 45ms │ 89ms │ 1250.3/sec   │
│ POST /users│ 25000│ 56ms │ 98ms │ 201ms│ 892.7/sec    │
│ TOTAL      │ 50000│ 39ms │ 72ms │ 156ms│ 1065.2/sec   │
└────────────┴──────┴──────┴──────┴──────┴───────────────┘
```

| 统计列 | 含义 |
|--------|------|
| `90% Line` | 90% 的请求响应时间小于此值（P90） |
| `95% Line` | 95% 的请求响应时间小于此值（P95） |
| `99% Line` | 99% 的请求响应时间小于此值（P99） |
| `Median` | 中位数响应时间（P50） |

### 6.5 压测阶段的监听器策略

```text
# 调试阶段：开启 View Results Tree
# 压测阶段：全部关闭 GUI 监听器，改为 CLI 模式

# 压测阶段推荐的结果收集方式：
# 方式一：CLI 模式输出 JTL 文件
jmeter -n -t test.jmx -l result.jtl -e -o report/

# 方式二：Backend Listener -> InfluxDB -> Grafana
# 后端监听器配置：
Backend Listener Implementation: org.apache.jmeter.visualizers.backend.influxdb.InfluxdbBackendListenerClient
Parameters:
  influxdbUrl: http://influxdb:8086/write?db=jmeter
  application: my-app
  measurement: jmeter
```

> 🎯 **核心原则**：GUI 监听器会占用大量内存和 CPU，严重影响测试结果的准确性。正式压测一律使用 CLI 模式 + 后端存储。

---

## 7. 断言

### 7.1 断言分类

| 断言类型 | 用途 | 适用场景 |
|----------|------|----------|
| **Response Assertion** | 响应文本/状态码验证 | 通用响应校验 |
| **JSON Assertion** | JSON 响应字段验证 | REST API 测试 |
| **Duration Assertion** | 响应时间超时校验 | SLA 监控 |
| **Size Assertion** | 响应大小校验 | 内容完整性检查 |
| **XPath Assertion** | XML 响应验证 | SOAP/XML 接口 |
| **HTML Assertion** | HTML 语法检查 | Web 页面测试 |
| **MD5Hex Assertion** | 响应内容完整性校验 | 文件下载校验 |

### 7.2 Response Assertion

最通用的响应断言器，支持对响应文本、响应头、状态码进行模式匹配。

```text
# 配置示例
Apply to: Main sample only
Test Field: Response Code        # 可选: Response Code / Response Headers / Response Data
Pattern Matching Rules: Equals   # 可选: Contains / Matches / Equals / Substring

Patterns to Test:
  200

# ----------------------------------------
# 校验 JSON 响应体是否包含成功标记
Test Field: Response Data
Pattern Matching Rules: Contains
Patterns to Test:
  "success": true

# ----------------------------------------
# 多条件校验
Test Field: Response Code
Pattern Matching Rules: Equals
Patterns to Test:
  200

Test Field: Response Data
Pattern Matching Rules: Contains
Patterns to Test:
  "code": 0
```

| 匹配规则 | 说明 | 示例 Pattern |
|----------|------|-------------|
| `Contains` | 响应内容**包含**指定文本 | `"success": true` |
| `Matches` | 响应内容**正则匹配** | `\\{"code":0.*\\}` |
| `Equals` | 响应内容**完全等于** | `{"code":0}` |
| `Substring` | 响应内容**包含子串**（非正则） | `success` |

### 7.3 JSON Assertion

专门为 JSON 响应设计的断言器，支持通过 JSON Path 验证特定字段。

```text
# 配置示例
Assert JSON Path exists: $.data.users.length()     # 验证字段存在
Additionally assert value: ✓
  Expected Value: 10
  Expect null: false     # 是否期待为空
  Invert assertion: false

# 常用 JSON Path 示例
$.code                          # 根节点字段
$.data.user.name                # 嵌套字段
$.data.users[0].id              # 数组第 1 个元素
$.data.users[?(@.role=='admin')].id  # 条件过滤
$.data.total                    # 数值字段
$.data.users.length()           # 数组长度
```

### 7.4 Duration Assertion

验证响应时间是否在预期范围内。

```text
# 配置示例
Duration in milliseconds: 2000   # 超过 2 秒即标记为失败

Apply to:
  ✓ Main sample only
  □ Sub samples
  □ ...

# 结合业务需求设置阈值
# 普通查询 API: 500ms
# 复杂报表 API: 3000ms
# 批量导入 API: 10000ms
```

### 7.5 Size Assertion

验证响应体大小是否在指定范围内。

```text
# 配置示例
Size in bytes: 1024

Type of comparison:
  ○ Equals          # 等于
  ○ Not Equals      # 不等于
  ○ Greater than    # 大于
  ○ Greater or equal# 大于等于
  ○ Less than       # 小于
  ○ Less or equal   # 小于等于

# 场景示例
# 1. 文件下载接口：验证文件大小大于 1MB
Size in bytes: 1048576
Type of comparison: Greater or equal

# 2. 空列表校验：验证响应体小于 100 字节
Size in bytes: 100
Type of comparison: Less than
```

---

## 8. 定时器

### 8.1 定时器作用

定时器控制采样器执行前的等待时间，用于 **模拟真实用户的思考时间** 或 **制造并发峰值**。

| 定时器 | 作用 | 适用场景 |
|--------|------|----------|
| **Constant Timer** | 固定延迟 | 简单模拟用户思考时间 |
| **Gaussian Random Timer** | 正态分布延迟 | 更真实的用户行为模拟 |
| **Uniform Random Timer** | 均匀分布延迟 | 随机延迟场景 |
| **Synchronizing Timer** | 集合点（瞬间并发） | 测试某时刻的并发能力 |
| **Constant Throughput Timer** | 控制吞吐量 | 稳定 QPS 场景 |
| **Poisson Random Timer** | 泊松分布延迟 | 模拟到达率场景 |

### 8.2 Constant Timer

每个请求前增加固定时间的延迟。

```text
# 配置
Thread Delay (in milliseconds): 1000

# 效果：每两个请求之间固定等待 1 秒
# 线程 1: [请求] --等待1s--> [请求] --等待1s--> [请求]
# 线程 2: [请求] --等待1s--> [请求] --等待1s--> [请求]
```

### 8.3 Gaussian Random Timer

延迟时间符合正态（高斯）分布，更接近真实用户行为。

```text
# 配置
Deviation (in milliseconds): 100     # 标准差
Constant Delay Offset (in milliseconds): 500  # 基础偏移

# 效果：延迟时间 = 500ms + random.gaussian() * 100ms
# 大多数延迟落在 400-600ms 之间
# 小部分延迟超出此范围

# 延迟分布示例（均值 500ms，标准差 100ms）：
# 68% 请求: 400-600ms
# 95% 请求: 300-700ms
# 99% 请求: 200-800ms
```

### 8.4 Synchronizing Timer（集合点）

让所有线程在发送请求前等待，直到达到指定数量的线程，然后同时发送请求，制造并发峰值。

```text
# 配置
Number of Simultaneous Users to Group by: 50   # 每 50 个线程一起发送
Timeout in milliseconds: 10000                  # 超时时间：10 秒后不再等待

# 场景示例：测试登录接口的瞬时并发
# Thread Group: 200 线程
# Synchronizing Timer: 50 用户同时请求
# 效果：每次放行 50 个线程同时发送登录请求

# ┌────────────────────────────────────────────┐
# │ 线程 1-50  →  全部到达集合点 →  同时发送请求  │
# │ 线程 51-100 → 全部到达集合点 →  同时发送请求  │
# │ 线程 101-150→ 全部到达集合点 →  同时发送请求  │
# │ 线程 151-200→ 全部到达集合点 →  同时发送请求  │
# └────────────────────────────────────────────┘
```

> ⚠️ **注意事项**：Synchronizing Timer 会阻塞线程直到达到指定数量或超时，过多的集合点会显著降低测试吞吐量。超时时间不宜过长，防止线程死等。

---

## 9. CSV 数据驱动测试

### 9.1 CSV Data Set Config

CSV Data Set Config 是 JMeter 中 **数据驱动测试** 的核心组件，允许从 CSV 文件中读取测试数据并注入到变量中。

#### 配置文件格式

```csv
# test_users.csv
userId,username,password,role,expectedStatus
1001,alice,pass123,admin,200
1002,bob,pass456,editor,200
1003,charlie,pass789,viewer,200
1004,,invalid_user,viewer,401
1005,dave,wrong_pass,admin,401
```

#### 配置参数

| 参数 | 说明 | 推荐值 |
|------|------|--------|
| `Filename` | CSV 文件路径 | `test_data/test_users.csv` |
| `File Encoding` | 文件编码 | `UTF-8` |
| `Variable Names` | 变量名列表（逗号分隔） | `userId,username,password,role,expectedStatus` |
| `Delimiter` | 分隔符 | `,` |
| `Recycle on EOF` | 文件末尾是否循环 | `false`（压测通常关闭） |
| `Stop thread on EOF` | 文件末尾是否停止线程 | `true`（确保每个线程使用不同数据） |
| `Sharing mode` | 数据共享模式 | `All threads` |

#### 数据共享模式

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| `All threads` | 所有线程共享一个数据指针 | 全局数据池 |
| `Current thread group` | 同线程组内线程共享 | 分组数据 |
| `Current thread` | 每个线程拥有独立的迭代器 | 保证每个线程读不同数据 |
| `Identifier` | 自定义标识分组 | 高级场景 |

### 9.2 完整的数据驱动测试计划

```text
Test Plan
 ├── User Defined Variables
 │   ├── BASE_URL = api.example.com
 │   └── PORT = 443
 │
 └── Thread Group (100 线程, Loop: 1)
      ├── CSV Data Set Config
      │   ├── Filename: test_users.csv
      │   └── Variable Names: userId,username,password,role,expectedStatus
      │
      ├── HTTP Request: Login
      │   ├── Method: POST
      │   ├── Path: /api/auth/login
      │   ├── Body: {"username":"${username}","password":"${password}"}
      │   └── JSON Extractor
      │       ├── Names of created variables: token
      │       └── JSON Path Expressions: $.data.token
      │
      ├── HTTP Header Manager
      │   └── Header: Authorization = Bearer ${token}
      │
      ├── HTTP Request: Get User Profile
      │   ├── Method: GET
      │   ├── Path: /api/users/${userId}
      │   └── Response Assertion
      │       ├── Test Field: Response Code
      │       └── Pattern: ${expectedStatus}
      │
      └── Summary Report (禁用，仅调试)
```

> 💡 **CSV 最佳实践**：
> 1. CSV 文件使用相对路径（相对于 JMeter 工作目录或测试计划所在目录）
> 2. 包含表头时勾选 "Ignore first line"（仅当 Variable Names 手动填写时）
> 3. 大型数据集（10w+ 行）建议使用 `__CSVRead()` 函数替代 CSV Data Set Config
> 4. 密码等敏感数据建议加密存储，测试脚本中解密使用

---

## 10. HTTP 管理器

### 10.1 HTTP Header Manager

用于定义 HTTP 请求的公共请求头。

```text
# 配置示例
Name:                  Value:
Content-Type           application/json
Accept                 application/json
Authorization          Bearer ${token}
X-Request-ID           ${__UUID()}
X-Client-Version       v1.2.3
User-Agent             JMeter-LoadTest/1.0
```

| 常见请求头 | 用途 | 示例 |
|------------|------|------|
| `Content-Type` | 请求体格式 | `application/json` / `multipart/form-data` |
| `Accept` | 期望响应格式 | `application/json` |
| `Authorization` | 认证令牌 | `Bearer eyJhbGciOi...` |
| `X-Request-ID` | 链路追踪 ID | `${__UUID()}` |
| `Cookie` | Cookie 值 | `sessionId=abc123`（推荐用 Cookie Manager 管理） |

### 10.2 HTTP Cookie Manager

自动管理 Cookie，支持 Cookie 存储和发送。

```text
# 配置选项
Options:
  ○ No Cookies (禁用)
  ● Manage Cookies (like a browser)  # 像浏览器一样管理 Cookie
  □ User Cookie Policy: standard

  Clear cookies each iteration: ✓     # 每次迭代清除 Cookie
  Use Thread Local Storage: ✓         # 线程本地存储（防止线程间 Cookie 冲突）
```

| 场景 | 配置 | 说明 |
|------|------|------|
| 单用户多次迭代 | 关闭 Clear cookies | 保持会话 |
| 多用户不同会话 | 开启 Clear cookies | 每个迭代模拟新用户 |
| 线程组间隔离 | 开启 Thread Local Storage | 防止 Cookie 互相覆盖 |

### 10.3 HTTP Cache Manager

模拟浏览器缓存行为，控制 HTTP 请求的缓存策略。

```text
# 配置选项
● Clear cache each iteration: ✓
  Use Cache-Control/Expires header: ✓
  Max Number of cached elements: 5000
```

> 💡 **压测时建议**：
> - **关闭 Cache Manager**：大多数性能测试关注的是服务器处理能力，而非客户端缓存
> - 如要模拟真实带缓存的用户行为，开启 Cache Manager 并设置合理的缓存限制

---

## 11. 关联与提取器

### 11.1 什么是关联

> **关联 (Correlation)** 是从上一个请求的响应中提取动态数据（如 Token、Session ID、CSRF Token），传递给后续请求的过程。这是构建复杂测试场景的核心技术。

### 11.2 Regular Expression Extractor

通过正则表达式从响应中提取数据。

```text
# 配置示例：从登录响应中提取 Token
Apply to: Main sample only
Response Field to Check: Body

Reference Name: token                     # 变量名（后续用 ${token} 引用）
Regular Expression: "token":"([^"]+)"     # 正则匹配
Template: $1$                             # 提取第 1 个分组
Match No.: 1                              # 匹配第 1 个结果（0=随机，-1=全部）
Default Value: TOKEN_NOT_FOUND            # 默认值
```

#### 常用正则表达式

| 提取目标 | 响应片段 | 正则表达式 | Template |
|----------|----------|------------|----------|
| Token | `"token":"eyJhbGci..."` | `"token":"([^"]+)"` | `$1$` |
| Session ID | `Set-Cookie: JSESSIONID=ABC123` | `JSESSIONID=(\w+)` | `$1$` |
| CSRF Token | `name="csrf" value="xyz789"` | `value="([^"]+?)"` | `$1$` |
| ID 列表 | `"id": 101,"id": 102,"id": 103` | `"id": (\d+)` | `$1$` |
| URL 链接 | `"url":"/files/abc.pdf"` | `"url":"(/[^"]+)"` | `$1$` |

#### 提取 ID 列表示例

```text
# 响应数据: {"users":[{"id":1,"name":"A"},{"id":2,"name":"B"},{"id":3,"name":"C"}]}

# 提取所有用户 ID
Regular Expression: "id":(\d+)
Template: $1$
Match No.: -1              # -1 表示提取全部

# 引用方式：
${userId_1}  → "1"
${userId_2}  → "2"
${userId_3}  → "3"
${userId_matchNr} → "3"  # 匹配总数
```

### 11.3 JSON Extractor

专为 JSON 响应设计，使用 JSON Path 语法提取数据，比正则表达式更简洁、更可靠。

```text
# 配置示例
Apply to: Main sample only
Names of created variables: token
JSON Path expressions: $.data.token
Match No.: 1
Default Value: TOKEN_NOT_FOUND
```

#### JSON Path 语法速查

| JSON Path | 含义 | 示例值 |
|-----------|------|--------|
| `$.code` | 根节点 `code` 字段 | `0` |
| `$.data.token` | 嵌套 `data.token` 字段 | `eyJhbGci...` |
| `$.data.users[0].id` | 数组第 1 个元素的 `id` | `1001` |
| `$.data.users[*].id` | 所有用户的 `id` | `[1001,1002,1003]` |
| `$.data.users[?(@.role=='admin')]` | 条件过滤 | `[{...}]` |
| `$.data.total` | 总数 | `100` |
| `$.data.users.length()` | 数组长度 | `10` |

#### 多字段提取

```text
# 一次提取多个字段
Names of created variables: token userId role
JSON Path expressions:
  $.data.token
  $.data.user.id
  $.data.user.role
Match No.: 1
Default Value: NOT_FOUND

# 提取后变量：
# ${token}  → eyJhbGci...
# ${userId} → 1001
# ${role}   → admin
```

### 11.4 Boundary Extractor

JMeter 5.x 新增的边界提取器，比正则表达式更简洁。

```text
# 配置示例
Apply to: Main sample only
Reference Name: csrfToken

Left Boundary: value="csrf"
Right Boundary: "
Match No.: 1
Default Value: CSRF_NOT_FOUND

# 输入: <input type="hidden" name="csrf" value="abc123xyz" />
# 输出: abc123xyz
```

> 💡 **提取器选择建议**：
> - **JSON 响应优先使用 JSON Extractor**：语法简洁，性能好，不易出问题
> - **HTML 响应使用 Boundary Extractor**：简单直观
> - **复杂场景使用 Regular Expression Extractor**：最灵活，但正则表达式本身有学习成本
> - **需要编程逻辑时使用 JSR223 PostProcessor + Groovy**：最强大

---

## 12. HTML 报告生成

### 12.1 命令生成报告

JMeter 5.x 支持从测试结果 JTL 文件生成静态 HTML 报告。

```bash
# 第一步：CLI 模式执行测试，输出结果文件
jmeter -n -t test-plan.jmx -l result.jtl

# 第二步：从结果文件生成 HTML 报告
jmeter -g result.jtl -o report/

# 或一步到位（推荐）
jmeter -n -t test-plan.jmx -l result.jtl -e -o report/
```

### 12.2 HTML 报告内容

生成的 HTML 报告包含以下核心图表和统计数据：

| 报告模块 | 展示内容 | 说明 |
|----------|----------|------|
| **Overview** | 总体概览 | 测试持续时间、请求总数、吞吐量、错误率 |
| **Statistics** | 详细统计 | 每个请求的 Avg/Min/Max/P90/P95/P99 |
| **Errors** | 错误分析 | 错误类型分布、错误率趋势 |
| **Top 5 Errors** | Top 5 错误 | 最常见的 5 个错误及其堆栈 |
| **APDEX** | 应用性能指数 | 基于用户满意度的评分（0-1） |
| **Response Times** | 响应时间分布 | 各请求的响应时间百分位图 |
| **Throughput** | 吞吐量趋势 | 每秒请求数的变化曲线 |
| **Latencies** | 延迟趋势 | 网络延迟随时间的变化 |
| **Response Time Overview** | 响应时间总览 | 各请求平均响应时间柱状图 |
| **Response Time Percentiles** | 响应时间百分位 | 各百分位的响应时间曲线 |
| **Active Threads Over Time** | 活跃线程数 | 线程数随时间的变化 |
| **Bytes Throughput** | 字节吞吐量 | 每秒发送/接收字节数 |
| **Time vs Threads** | 时间 vs 线程 | 不同线程数下的响应时间变化 |

### 12.3 APDEX 配置

APDEX（Application Performance Index）是衡量用户满意度的标准指标。

```bash
# 默认配置：满意 < 500ms，可容忍 < 1500ms
# 通过属性自定义阈值
jmeter -n -t test.jmx -l result.jtl -e -o report/ \
  -Jjmeter.reportgenerator.apdex_satisfied_threshold=500 \
  -Jjmeter.reportgenerator.apdex_tolerated_threshold=1500

# APDEX 计算公式：
# APDEX = (满意请求数 + 可容忍请求数 × 0.5) / 总请求数
# 0.94+ = 优秀 | 0.85+ = 良好 | 0.70+ = 一般 | < 0.70 = 差
```

---

## 13. 分布式测试

### 13.1 什么是分布式测试

当单台机器无法模拟足够多的并发用户时（通常 > 2000 线程），可以采用 **Master-Slave** 架构进行分布式测试。

### 13.2 架构图

```
                      +-----------------+
                      |    Master       |
                      | (Controller)    |
                      |  - 分发测试计划   |
                      |  - 汇总结果       |
                      +--------+--------+
                               |
              +----------------+----------------+
              |                |                |
     +--------+--------+  +--------+--------+  +--------+--------+
     |   Slave 1       |  |   Slave 2       |  |   Slave N       |
     | (Agent)         |  | (Agent)         |  | (Agent)         |
     | 192.168.1.101   |  | 192.168.1.102   |  | 192.168.1.10N   |
     | - 执行测试       |  | - 执行测试       |  | - 执行测试       |
     | - 上报结果       |  | - 上报结果       |  | - 上报结果       |
     +-----------------+  +-----------------+  +-----------------+
```

### 13.3 分布式配置步骤

#### Slave 节点配置

```bash
# 1. 在 Slave 机器上启动 JMeter Server
# Windows: 双击 %JMETER_HOME%/bin/jmeter-server.bat
# Linux:
cd /opt/apache-jmeter-5.6.3/bin
./jmeter-server

# 2. 可选：指定 Slave 的 RMI 端口
# 编辑 jmeter.properties
server_port=1099
server.rmi.localport=1099
server.rmi.port=1099

# 3. 多网卡时指定 IP
# 编辑 jmeter-server 脚本，添加：
RMI_HOST_DEF=-Djava.rmi.server.hostname=192.168.1.101
```

#### Master 节点配置

```bash
# 1. 编辑 %JMETER_HOME%/bin/jmeter.properties
# 添加所有 Slave 节点 IP
remote_hosts=192.168.1.101:1099,192.168.1.102:1099,192.168.1.103:1099

# 2. 确保 master.properties 中关闭 SSL（简单内网场景）
server.rmi.ssl.disable=true

# 3. Master 执行分布式测试（GUI 模式）
# Run -> Remote Start All

# 4. 或 CLI 模式执行分布式测试
jmeter -n -t test-plan.jmx -R 192.168.1.101:1099,192.168.1.102:1099 \
  -l distributed-result.jtl -e -o report/

# 5. 分布式 CLI + 报告生成
jmeter -n -t test-plan.jmx -R slave1,slave2,slave3 \
  -l result.jtl -e -o report/ \
  -Jremote_hosts=192.168.1.101:1099,192.168.1.102:1099,192.168.1.103:1099
```

### 13.4 分布式测试关键配置

```properties
# jmeter.properties (所有节点保持一致)
# 关闭 SSL（内网环境）
server.rmi.ssl.disable=true

# Server 模式
server_port=1099
server.rmi.port=1099
server.rmi.localport=1099

# 禁用系统监听器（减少资源消耗）
mode=StrippedBatch

# 结果收集方式
mode=Standard        # 所有样本实时发送
mode=Batch           # 批量发送（默认）
mode=StrippedBatch   # 精简批量发送（推荐）
mode=StrippedSync    # 精简同步发送（慎用）
```

### 13.5 分布式测试注意事项

| 注意事项 | 说明 |
|----------|------|
| **网络延迟** | Master 和 Slaves 之间网络延迟应 < 1ms，建议同机房/同交换机 |
| **时钟同步** | 所有节点时间通过 NTP 同步，否则时间戳混乱 |
| **结果收集** | `StrippedBatch` 模式最省资源，`Standard` 模式最实时但资源消耗大 |
| **文件依赖** | CSV 数据文件需在每个 Slave 节点上相同路径存在 |
| **防火墙** | 开放 RMI 端口（1099）和相关随机端口 |
| **数据一致性** | 分布式测试不保证不同 Slave 之间的数据完全有序 |
| **线程计算** | 总并发数 = Slave 数 × 每个 Slave 的线程数 |

---

## 14. CLI 命令行模式

### 14.1 CLI 模式基础用法

CLI（Command-Line Interface / Non-GUI）模式是 JMeter 用于生产环境压测的推荐模式。

```bash
# 基本语法
jmeter -n -t <test-plan.jmx> -l <result.jtl> [options]

# 参数说明
# -n          : Non-GUI 模式
# -t <file>   : 测试计划文件路径
# -l <file>   : 结果文件路径（JTL/CSV）
# -e          : 测试结束后生成 HTML 报告
# -o <dir>    : HTML 报告输出目录
# -H <host>   : 代理主机
# -P <port>   : 代理端口
# -J<prop>=<value> : 设置 JMeter 属性
# -G<prop>=<value> : 设置全局属性（分布式）
# -R <hosts>  : 远程 Slave 列表
# -L <category>=<level> : 日志级别
# -d <dir>    : JMeter 家目录

# 最简用法
jmeter -n -t test.jmx -l result.jtl

# 完整用法（推荐）
jmeter -n -t test.jmx -l result.jtl -e -o report/ \
  -Jjmeter.save.saveservice.output_format=csv \
  -Jjmeter.save.saveservice.response_data=false \
  -Jjmeter.save.saveservice.sampler_data=false
```

### 14.2 常用 CLI 组合

```bash
# 1. 快速压测 + 报告
jmeter -n -t load-test.jmx -l result.jtl -e -o report/

# 2. 设置线程数（覆盖测试计划中的值）
jmeter -n -t test.jmx -l result.jtl \
  -Jthreads=200 -Jrampup=30 -Jduration=600

# 3. 分布式压测
jmeter -n -t test.jmx -l result.jtl \
  -R 192.168.1.101:1099,192.168.1.102:1099 \
  -Jremote_hosts=192.168.1.101:1099,192.168.1.102:1099

# 4. 指定测试结果保存的字段
jmeter -n -t test.jmx -l result.csv \
  -Jjmeter.save.saveservice.output_format=csv \
  -Jjmeter.save.saveservice.time=true \
  -Jjmeter.save.saveservice.latency=true \
  -Jjmeter.save.saveservice.successful=true \
  -Jjmeter.save.saveservice.thread_counts=true

# 5. 只保存错误请求（减少结果文件大小）
jmeter -n -t test.jmx -l errors-only.jtl \
  -Jjmeter.save.saveservice.output_format=csv \
  -Jjmeter.save.saveservice.errorsonly=true

# 6. 指定日志级别调试
jmeter -n -t test.jmx -l result.jtl \
  -L org.apache.jmeter.protocol.http=DEBUG \
  -L org.apache.jmeter.util=INFO
```

### 14.3 CLI 模式性能相关属性

```properties
# jmeter.properties 或 -J 参数

# 结果保存配置
jmeter.save.saveservice.output_format=csv      # CSV 格式（比 XML 小 80%）
jmeter.save.saveservice.response_data=false     # 不保存响应数据
jmeter.save.saveservice.sampler_data=false      # 不保存采样器数据
jmeter.save.saveservice.request_headers=false   # 不保存请求头
jmeter.save.saveservice.response_headers=false  # 不保存响应头
jmeter.save.saveservice.thread_counts=true      # 保存线程数

# 监听器性能
jmeter.engine.force.system.exit=false           # 强制退出

# 结果文件管理
jmeter.save.saveservice.autoflush=false         # 降低 I/O 频率（提高性能）

# 分布式模式
mode=StrippedBatch                               # 精简批量模式
```

### 14.4 CLI 模式输出解读

```bash
# 执行输出示例
$ jmeter -n -t test.jmx -l result.jtl
Creating summariser <summary>
Created the tree successfully using test.jmx
Starting the test @ Wed Jul 26 10:30:00 CST 2026 (1672345800000)
Waiting for possible Shutdown/StopTestNow/HeapDump/ThreadDump message on port 4445
summary +    500 in 00:00:01 =  500.0/s Avg:    45 Min:    12 Max:   156 Err:     0 (0.00%)
summary +   1500 in 00:00:01 = 1500.0/s Avg:    42 Min:    10 Max:   201 Err:     1 (0.07%)
summary +   2000 in 00:00:01 = 2000.0/s Avg:    38 Min:     8 Max:   189 Err:     0 (0.00%)
summary =   4000 in 00:00:03 = 1333.3/s Avg:    42 Min:     8 Max:   201 Err:     1 (0.03%)
summary +   2000 in 00:00:01 = 2000.0/s Avg:    41 Min:     9 Max:   178 Err:     0 (0.00%)
...
Tidying up ... @ Wed Jul 26 10:35:00 CST 2026 (1672346100000)
... end of run
```

| 输出字段 | 含义 |
|----------|------|
| `summary +` | 最近一个时间窗口的增量数据（默认 30 秒） |
| `summary =` | 从测试开始到现在的累计数据 |
| `500 in 00:00:01` | 窗口内请求数 / 窗口持续时间 |
| `500.0/s` | 当前吞吐量（req/s） |
| `Avg: 45` | 平均响应时间 (ms) |
| `Min: 12` | 最小响应时间 (ms) |
| `Max: 156` | 最大响应时间 (ms) |
| `Err: 0 (0.00%)` | 错误数及错误率 |

---

## 15. 常见测试场景

### 15.1 REST API 负载测试

**场景描述**：对一组 RESTful API 进行持续负载测试，评估系统在不同并发下的表现。

```text
Test Plan: REST API Load Test
├── User Defined Variables
│   ├── BASE_URL = api.example.com
│   ├── PORT = 443
│   ├── THREADS = 100
│   ├── RAMP_UP = 30
│   └── DURATION = 600
│
├── HTTP Request Defaults
│   ├── Protocol: https
│   ├── Server: ${BASE_URL}
│   └── Port: ${PORT}
│
├── HTTP Header Manager
│   └── Content-Type: application/json
│   └── Accept: application/json
│
├── Thread Group (${THREADS}, Ramp-Up: ${RAMP_UP}, Duration: ${DURATION})
│   ├── CSV Data Set Config
│   │   └── Filename: api-test-data.csv
│   │   └── Variables: userId,userName
│   │
│   ├── GET /api/users/profile
│   │   ├── Path: /api/users/${userId}
│   │   ├── JSON Assertion: $.code == 200
│   │   └── Duration Assertion: 2000ms
│   │
│   ├── POST /api/users
│   │   ├── Method: POST
│   │   ├── Path: /api/users
│   │   ├── Body: {"name":"${userName}","email":"${userName}@test.com"}
│   │   ├── JSON Extractor: $.data.userId → newUserId
│   │   ├── Response Assertion: 201
│   │   └── Duration Assertion: 3000ms
│   │
│   ├── PUT /api/users/${newUserId}
│   │   ├── Method: PUT
│   │   ├── Path: /api/users/${newUserId}
│   │   └── Body: {"name":"${userName}-updated"}
│   │
│   ├── DELETE /api/users/${newUserId}
│   │   └── Method: DELETE
│   │
│   └── Gaussian Random Timer
│       ├── Deviation: 100
│       └── Constant Delay: 500
```

### 15.2 登录并发测试

**场景描述**：模拟大量用户同时登录系统，测试认证模块的并发处理能力。

```text
Test Plan: Login Concurrency Test
├── Thread Group (500 线程, Ramp-Up: 10s, Loop: 1)
│   ├── CSV Data Set Config
│   │   └── Filename: login-users.csv
│   │   └── Variables: username,password
│   │   └── Stop Thread on EOF: true
│   │
│   ├── Synchronizing Timer
│   │   ├── Number of Simultaneous Users: 100
│   │   └── Timeout: 30000
│   │
│   ├── POST /api/auth/login
│   │   ├── Method: POST
│   │   ├── Body: {"username":"${username}","password":"${password}"}
│   │   ├── Response Assertion: 200
│   │   ├── JSON Assertion: $.code == 0
│   │   ├── Duration Assertion: 5000ms
│   │   └── JSON Extractor: $.data.token → authToken
│   │   └── JSON Extractor: $.data.sessionId → sessionId
│   │
│   ├── HTTP Header Manager
│   │   └── Authorization: Bearer ${authToken}
│   │
│   ├── GET /api/user/profile
│   │   ├── Response Assertion: 200
│   │   └── JSON Assertion: $.data.username == ${username}
│   │
│   └── GET /api/user/permissions
│       ├── Response Assertion: 200
│       └── Duration Assertion: 2000ms
```

### 15.3 文件上传压力测试

**场景描述**：测试文件上传接口在高并发下的稳定性和性能。

```text
Test Plan: File Upload Stress Test
├── User Defined Variables
│   ├── THREADS = 50
│   ├── RAMP_UP = 20
│   └── FILE_SIZE = 1MB
│
├── Thread Group (${THREADS}, Ramp-Up: ${RAMP_UP}, Loop: 10)
│   ├── CSV Data Set Config
│   │   ├── Filename: upload-files.csv
│   │   └── Variables: filePath,fileDesc
│   │
│   ├── POST /api/files/upload
│   │   ├── Method: POST
│   │   ├── Files Upload:
│   │   │   ├── File Path: ${filePath}
│   │   │   ├── Parameter Name: file
│   │   │   └── MIME Type: application/octet-stream
│   │   ├── Parameters:
│   │   │   ├── description: ${fileDesc}
│   │   │   └── category: test
│   │   ├── JSON Extractor: $.data.fileId → fileId
│   │   ├── Response Assertion: 201
│   │   ├── Size Assertion: > 0
│   │   └── Duration Assertion: 30000ms
│   │
│   ├── GET /api/files/${fileId}
│   │   └── Method: GET
│   │   └── Duration Assertion: 5000ms
│   │
│   ├── GET /api/files/${fileId}/download
│   │   └── Duration Assertion: 60000ms
│   │
│   └── Constant Timer
│       └── Thread Delay: 1000
│
├── Summary Report
├── Aggregate Report (CLI 模式中禁用)
└── Simple Data Writer
    └── Filename: upload-results.jtl
```

---

## 16. 最佳实践

### 16.1 测试计划设计

| 实践 | 详细说明 |
|------|----------|
| **使用变量** | 将 URL、端口、线程数、超时等配置参数化，避免硬编码 |
| **模块化设计** | 使用 Include Controller 和 Module Controller 复用测试片段 |
| **保持简单** | 一个测试计划只测试一个业务场景，不要大杂烩 |
| **提前调试** | 在低线程数下验证测试计划正确性再放大并发 |
| **数据隔离** | 使用 CSV 数据文件为每个虚拟用户提供独立测试数据 |

### 16.2 性能优化

```bash
# 1. 增加 JMeter 堆内存（修改 jmeter.sh/jmeter.bat）
set HEAP="-Xms4g -Xmx8g -XX:MaxMetaspaceSize=512m"

# 2. 关键属性优化（user.properties）
# 禁用 GUI 相关组件
jmeter.engine.force.system.exit=true
jmeter.save.saveservice.output_format=csv
jmeter.save.saveservice.response_data=false
jmeter.save.saveservice.sampler_data=false
jmeter.save.saveservice.autoflush=false

# 3. 避免以下操作（压测时）
# - 使用 View Results Tree
# - 使用 Graph Results
# - 使用 XPath Assertion
# - 使用大量 JSR223 脚本
# - 记录所有请求的完整响应数据
```

### 16.3 压测禁忌清单

| ❌ 禁止 | ✅ 推荐 |
|---------|---------|
| GUI 模式下执行正式压测 | CLI 模式执行 + 结果收集 |
| View Results Tree 在压测时开启 | 仅调试阶段开启 |
| 单个测试持续过长时间 | 5-15 分钟，多次不同负载 |
| 结果文件保存为 XML | 保存为 CSV，文件大小减少 80% |
| 在测试执行中修改脚本 | 先停止，修改，再启动 |
| 依赖 GUI 监听器做实时监控 | 使用 CLI 日志 + Backend Listener |
| 使用默认堆内存 | 根据线程数调整堆大小 |
| 使用大量正则表达式提取 | 优先使用 JSON Extractor/Boundary Extractor |
| 在线程组之间共享数据 | 使用线程间隔离的数据文件 |

### 16.4 结果分析要点

```text
# 关键性能指标（KPI）
# 1. 吞吐量 (Throughput)
#    - 单位：req/s 或 tpm（事务/分钟）
#    - 关注：随并发数增长的趋势
#    - 拐点：吞吐量不再随并发增加时，系统已达瓶颈

# 2. 响应时间 (Response Time)
#    - 关注 P50, P90, P95, P99
#    - P99 比 Average 更能反映系统真实表现
#    - 容忍长尾：P99 < 500ms 优秀，< 1000ms 良好

# 3. 错误率 (Error Rate)
#    - < 0.1%：优秀
#    - < 1%：可接受
#    - > 1%：需要排查
#    - 错误率突增通常意味着系统过载

# 4. 资源利用率
#    - CPU：理想 70-85%，超过 90% 说明过载
#    - 内存：GC 频率和 Full GC 次数
#    - 网络：带宽是否打满
#    - I/O：磁盘 I/O 等待时间
```

### 16.5 CI/CD 集成

```yaml
# GitHub Actions 集成示例
name: Performance Test

on:
  schedule:
    - cron: '0 2 * * *'  # 每天凌晨 2 点执行
  workflow_dispatch:       # 手动触发

jobs:
  perf-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Download JMeter
        run: |
          wget https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.6.3.tgz
          tar -xzf apache-jmeter-5.6.3.tgz

      - name: Run Load Test
        run: |
          apache-jmeter-5.6.3/bin/jmeter -n -t test-plans/api-load-test.jmx \
            -Jthreads=200 -Jrampup=30 -Jduration=300 \
            -l results/result.jtl -e -o results/report/

      - name: Upload Test Report
        uses: actions/upload-artifact@v4
        with:
          name: perf-report
          path: results/report/

      - name: Check Performance Thresholds
        run: |
          # 从 JTL 文件解析错误率和响应时间，判断是否通过
          python scripts/check-perf-results.py results/result.jtl
```

---

## 17. JMeter vs 其他压测工具

### 17.1 工具对比总表

| 特性 | JMeter | Gatling | wrk | ab (Apache Bench) |
|------|--------|---------|-----|-------------------|
| **开发语言** | Java | Scala | C | C |
| **首次发布** | 1998 | 2012 | 2014 | 2000 |
| **UI 界面** | 有（GUI） | 有（Recorder） | 无 | 无 |
| **协议支持** | HTTP/HTTPS/JDBC/FTP/JMS/SOAP/... | HTTP/HTTPS/WebSocket/JMS | HTTP/HTTPS | HTTP/HTTPS |
| **脚本语言** | GUI + Groovy + Beanshell | Scala DSL | Lua | 无 |
| **分布式** | 支持（Master-Slave） | 不支持原生 | 不支持 | 不支持 |
| **HTML 报告** | 内置 | 内置 | 无 | 无 |
| **实时监控** | Backend Listener + Grafana | Graphite/Grafana | 无 | 无 |
| **学习曲线** | 中（GUI 友好） | 中-高 | 低 | 极低 |
| **资源消耗** | 中-高 | 中 | 极低 | 低 |
| **最大并发** | 5000+（分布式） | 3000+ | 10000+ | 5000+ |
| **断言能力** | 强大 | 一般 | 无 | 无 |
| **提取/关联** | 强大 | 一般 | 无 | 无 |
| **CI/CD 集成** | 容易（CLI） | 容易（Maven/Gradle） | 简单 | 简单 |
| **商业版本** | 无（纯开源） | Gatling FrontLine（付费） | 无 | 无 |

### 17.2 工具选型建议

| 场景 | 推荐工具 | 理由 |
|------|----------|------|
| **复杂业务场景压测**（多步操作、登录态保持） | JMeter | 组件丰富，关联能力强 |
| **REST API 简单压力测试** | wrk / ab | 轻量、高性能、零配置 |
| **CI/CD 集成性能测试** | Gatling | Scala DSL 代码化，版本易管理 |
| **微服务全链路压测** | JMeter（分布式） | 分布式架构支持 |
| **快速确认接口性能基线** | ab | 一条命令出结果 |
| **精细化性能指标分析** | JMeter + Grafana | Backend Listener 集成度高 |
| **开发人员本地调试** | Gatling | IDE 可运行，代码即测试 |
| **非技术人员使用** | JMeter（GUI 模式） | 可视化操作，学习门槛低 |

### 17.3 对比结论

```text
1. JMeter 的优势在于：
   - 最全面的功能集（GUI + 丰富组件 + 分布式 + 多种协议）
   - 活跃的社区和丰富的教程资源
   - Groovy 脚本支持使得扩展性极强
   - 图形化的测试计划设计降低了学习门槛

2. Gatling 的优势在于：
   - 代码化测试（Scala/Java DSL），天然适合 Git 管理
   - 高性能异步 IO，同等硬件下能模拟更多用户
   - 内置报告比 JMeter 更现代、更美观

3. wrk 的优势在于：
   - 极致性能，系统资源消耗最小
   - 命令行操作，非常适合快速压测
   - Lua 脚本提供了基本的定制能力

4. ab 的优势在于：
   - 几乎无处不在（预装在大部分 Linux 发行版）
   - 最简单的使用方式（一条命令）
   - 足够用于初步的性能摸底
```

---

## 18. 完整示例：SpringBoot REST API 压测

### 18.1 被测应用简介

假设有一个 SpringBoot 应用提供用户管理 REST API：

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping
    public Result<List<User>> list(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        // 分页查询用户列表
        return Result.success(userService.list(page, size));
    }

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @PostMapping
    public Result<User> create(@Valid @RequestBody UserCreateReq req) {
        return Result.success(userService.create(req));
    }

    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @Valid @RequestBody UserUpdateReq req) {
        return Result.success(userService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success(null);
    }
}
```

### 18.2 JMeter 测试计划 (test-plan.jmx)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0">
  <hashTree>
    <!-- 测试计划 -->
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="SpringBoot User API Load Test">
      <elementProp name="TestPlan.user_defined_variables" elementType="Arguments">
        <collectionProp name="Arguments.arguments">
          <elementProp name="BASE_URL" elementType="Argument">
            <stringProp name="Argument.name">BASE_URL</stringProp>
            <stringProp name="Argument.value">localhost</stringProp>
          </elementProp>
          <elementProp name="PORT" elementType="Argument">
            <stringProp name="Argument.name">PORT</stringProp>
            <stringProp name="Argument.value">8080</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
    <hashTree>

      <!-- HTTP Request Defaults -->
      <ConfigTestElement guiclass="HttpDefaultsGui" testclass="ConfigTestElement" testname="HTTP Request Defaults">
        <stringProp name="HTTPSampler.domain">${BASE_URL}</stringProp>
        <stringProp name="HTTPSampler.port">${PORT}</stringProp>
        <stringProp name="HTTPSampler.protocol">http</stringProp>
        <stringProp name="HTTPSampler.contentEncoding">UTF-8</stringProp>
      </ConfigTestElement>
      <hashTree/>

      <!-- HTTP Header Manager -->
      <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="HTTP Header Manager">
        <collectionProp name="HeaderManager.headers">
          <elementProp name="" elementType="Header">
            <stringProp name="Header.name">Content-Type</stringProp>
            <stringProp name="Header.value">application/json</stringProp>
          </elementProp>
          <elementProp name="" elementType="Header">
            <stringProp name="Header.name">Accept</stringProp>
            <stringProp name="Header.value">application/json</stringProp>
          </elementProp>
        </collectionProp>
      </HeaderManager>
      <hashTree/>

      <!-- ========== Thread Group 1: 查询接口压测 ========== -->
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Query APIs - 100 users">
        <stringProp name="ThreadGroup.num_threads">100</stringProp>
        <stringProp name="ThreadGroup.ramp_time">30</stringProp>
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController">
          <boolProp name="LoopController.continue_forever">true</boolProp>
          <stringProp name="LoopController.loops">-1</stringProp>
        </elementProp>
        <stringProp name="ThreadGroup.duration">300</stringProp>
      </ThreadGroup>
      <hashTree>

        <!-- CSV Data Set Config -->
        <CSVDataSet guiclass="TestBeanGUI" testclass="CSVDataSet" testname="CSV Data - Query Users">
          <stringProp name="filename">test-data/query-users.csv</stringProp>
          <stringProp name="variableNames">page,size</stringProp>
          <stringProp name="delimiter">,</stringProp>
          <boolProp name="recycle">true</boolProp>
          <boolProp name="stopThread">false</boolProp>
        </CSVDataSet>
        <hashTree/>

        <!-- HTTP Request: GET /api/users -->
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="GET /api/users">
          <stringProp name="HTTPSampler.method">GET</stringProp>
          <stringProp name="HTTPSampler.path">/api/users</stringProp>
          <boolProp name="HTTPSampler.follow_redirects">true</boolProp>
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
            <collectionProp name="Arguments.arguments">
              <elementProp name="page" elementType="HTTPArgument">
                <stringProp name="Argument.value">${page}</stringProp>
                <stringProp name="Argument.name">page</stringProp>
              </elementProp>
              <elementProp name="size" elementType="HTTPArgument">
                <stringProp name="Argument.value">${size}</stringProp>
                <stringProp name="Argument.name">size</stringProp>
              </elementProp>
            </collectionProp>
          </elementProp>
        </HTTPSamplerProxy>
        <hashTree>
          <ResponseAssertion guiclass="AssertionGui" testclass="ResponseAssertion" testname="Status Code Assertion">
            <collectionProp name="Asserion.test_strings">
              <stringProp name="49586">200</stringProp>
            </collectionProp>
            <stringProp name="Assertion.test_field">Assertion.response_code</stringProp>
            <intProp name="Assertion.test_type">8</intProp>
          </ResponseAssertion>
          <hashTree/>
          <DurationAssertion guiclass="DurationAssertionGui" testclass="DurationAssertion" testname="Duration Assertion">
            <stringProp name="DurationAssertion.duration">2000</stringProp>
          </DurationAssertion>
          <hashTree/>
        </hashTree>

        <!-- Gaussian Random Timer -->
        <GaussianRandomTimer guiclass="GaussianRandomTimerGui" testclass="GaussianRandomTimer" testname="Think Time">
          <stringProp name="ConstantTimer.delay">500</stringProp>
          <stringProp name="RandomTimer.range">100</stringProp>
        </GaussianRandomTimer>
        <hashTree/>

      </hashTree>

      <!-- ========== Thread Group 2: 写接口压测 ========== -->
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Write APIs - 50 users">
        <stringProp name="ThreadGroup.num_threads">50</stringProp>
        <stringProp name="ThreadGroup.ramp_time">20</stringProp>
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController">
          <boolProp name="LoopController.continue_forever">true</boolProp>
          <stringProp name="LoopController.loops">-1</stringProp>
        </elementProp>
        <stringProp name="ThreadGroup.duration">300</stringProp>
      </ThreadGroup>
      <hashTree>

        <!-- CSV Data -->
        <CSVDataSet guiclass="TestBeanGUI" testclass="CSVDataSet" testname="CSV Data - Create Users">
          <stringProp name="filename">test-data/create-users.csv</stringProp>
          <stringProp name="variableNames">userName,email,password,role</stringProp>
        </CSVDataSet>
        <hashTree/>

        <!-- POST /api/users: 创建用户 -->
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="POST /api/users">
          <stringProp name="HTTPSampler.method">POST</stringProp>
          <stringProp name="HTTPSampler.path">/api/users</stringProp>
          <boolProp name="HTTPSampler.postBodyRaw">true</boolProp>
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
            <collectionProp name="Arguments.arguments">
              <elementProp name="" elementType="HTTPArgument">
                <stringProp name="Argument.value">{"name":"${userName}","email":"${email}","password":"${password}","role":"${role}"}</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>
            </collectionProp>
          </elementProp>
        </HTTPSamplerProxy>
        <hashTree>
          <ResponseAssertion guiclass="AssertionGui" testclass="ResponseAssertion" testname="Status Code Assertion">
            <collectionProp name="Asserion.test_strings">
              <stringProp name="49586">201</stringProp>
            </collectionProp>
            <stringProp name="Assertion.test_field">Assertion.response_code</stringProp>
            <intProp name="Assertion.test_type">8</intProp>
          </ResponseAssertion>
          <hashTree/>
          <!-- 提取新创建的用户 ID -->
          <JSONPostProcessor guiclass="JSONPostProcessorGui" testclass="JSONPostProcessor" testname="Extract UserId">
            <stringProp name="JSONPostProcessor.referenceNames">newUserId</stringProp>
            <stringProp name="JSONPostProcessor.jsonPathExprs">$.data.id</stringProp>
            <stringProp name="JSONPostProcessor.match_numbers">1</stringProp>
          </JSONPostProcessor>
          <hashTree/>
        </hashTree>

        <!-- PUT /api/users/${newUserId}: 更新用户 -->
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="PUT /api/users">
          <stringProp name="HTTPSampler.method">PUT</stringProp>
          <stringProp name="HTTPSampler.path">/api/users/${newUserId}</stringProp>
          <boolProp name="HTTPSampler.postBodyRaw">true</boolProp>
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
            <collectionProp name="Arguments.arguments">
              <elementProp name="" elementType="HTTPArgument">
                <stringProp name="Argument.value">{"name":"${userName}-updated","email":"${email}"}</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>
            </collectionProp>
          </elementProp>
        </HTTPSamplerProxy>
        <hashTree>
          <ResponseAssertion>
            <collectionProp name="Asserion.test_strings">
              <stringProp name="49586">200</stringProp>
            </collectionProp>
            <stringProp name="Assertion.test_field">Assertion.response_code</stringProp>
            <intProp name="Assertion.test_type">8</intProp>
          </ResponseAssertion>
          <hashTree/>
        </hashTree>

        <!-- DELETE /api/users/${newUserId}: 删除用户 -->
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="DELETE /api/users">
          <stringProp name="HTTPSampler.method">DELETE</stringProp>
          <stringProp name="HTTPSampler.path">/api/users/${newUserId}</stringProp>
        </HTTPSamplerProxy>
        <hashTree>
          <ResponseAssertion>
            <collectionProp name="Asserion.test_strings">
              <stringProp name="49586">200</stringProp>
            </collectionProp>
            <stringProp name="Assertion.test_field">Assertion.response_code</stringProp>
            <intProp name="Assertion.test_type">8</intProp>
          </ResponseAssertion>
          <hashTree/>
        </hashTree>

      </hashTree>

    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

### 18.3 执行命令

```bash
# 1. 准备测试数据
# test-data/query-users.csv
page,size
1,20
2,20
1,10
1,50
3,20

# test-data/create-users.csv
userName,email,password,role
testuser1,test1@test.com,pass123,user
testuser2,test2@test.com,pass456,editor
# ... (更多用户数据)

# 2. 启动应用
java -jar user-service.jar --server.port=8080

# 3. 执行压测（调试阶段：使用 GUI，低线程数）
jmeter -t test-plan.jmx

# 4. 正式压测（CLI 模式）
jmeter -n -t test-plan.jmx \
  -Jthreads_query=200 -Jthreads_write=100 \
  -l results/springboot-api-result.jtl \
  -e -o results/springboot-api-report/

# 5. 分析结果
# 打开 results/springboot-api-report/index.html
```

### 18.4 结果分析示例

```text
# 假设结果数据
┌──────────────────┬──────────┬───────┬───────┬───────┬────────┬─────────┐
│ API              │ Samples │ Avg   │ P90   │ P99   │ TPS    │ Err%    │
├──────────────────┼──────────┼───────┼───────┼───────┼────────┼─────────┤
│ GET /api/users   │ 15000   │ 12ms  │ 25ms  │ 78ms  │ 1500/s │ 0.00%   │
│ POST /api/users  │ 5000    │ 45ms  │ 89ms  │ 210ms │ 500/s  │ 0.02%   │
│ PUT /api/users   │ 5000    │ 38ms  │ 72ms  │ 189ms │ 500/s  │ 0.00%   │
│ DELETE /api/users│ 5000    │ 20ms  │ 45ms  │ 112ms │ 500/s  │ 0.00%   │
│ TOTAL            │ 30000   │ 29ms  │ 58ms  │ 145ms │ 3000/s │ 0.01%   │
└──────────────────┴──────────┴───────┴───────┴───────┴────────┴─────────┘

# 分析结论：
# 1. 查询接口性能优秀，TPS 1500/s，P99 仅 78ms
# 2. 写入接口性能良好，TPS 500/s，P99 < 210ms
# 3. 整体错误率 0.01%，系统稳定
# 4. 建议：继续增加并发找到拐点，关注 CPU 和数据库连接池使用情况
```

> 🎯 **JMeter 核心心法**：测试计划是剧本，线程组是演员，采样器是台词，断言是考核，监听器是录像。好的压测在于真实地模拟用户行为，而非堆高并发数字。

---

> 本文档系统梳理了 JMeter 压力测试的完整知识体系，涵盖从基础架构到高级特性、从单机压测到分布式部署的各个层面。掌握这些内容，可胜任 Java 后端工程中绝大部分性能测试工作。持续实践、持续积累，才能真正做到从入门到精通。
