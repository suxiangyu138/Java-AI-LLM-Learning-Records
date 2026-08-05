# JMeter 基础入门与测试计划

> 🔰 从零开始：安装配置、GUI 界面详解、Test Plan 结构、Thread Group 线程模型、元件执行顺序

---

## 📚 目录

1. [JMeter 简介](#1-jmeter-简介)
2. [安装与目录结构](#2-安装与目录结构)
3. [GUI 界面详解](#3-gui-界面详解)
4. [Test Plan 测试计划](#4-test-plan-测试计划)
5. [Thread Group 线程组](#5-thread-group-线程组)
6. [元件执行顺序](#6-元件执行顺序)
7. [第一个测试脚本](#7-第一个测试脚本)

---

## 1. JMeter 简介

```text
Apache JMeter = 100% Java 开发的性能测试工具

核心功能：
  ├── 压力测试  → 模拟高并发，测试系统吞吐量
  ├── 负载测试  → 不同负载级别下的系统表现
  ├── 稳定性测试 → 长时间运行检测内存泄漏
  ├── 功能测试  → 配合断言验证接口正确性
  └── 多协议支持 → HTTP/HTTPS, JDBC, FTP, TCP, JMS, LDAP...

竞品对比：
  LoadRunner  → 商业重量级，功能全但贵
  Gatling     → Scala-based，代码即场景，轻量
  Locust      → Python, 代码灵活
  wrk/wrk2    → C 编写，极高并发
  k6          → Go + JS, 云原生友好

JMeter 优势：
  ✅ 开源免费
  ✅ GUI + CLI 双模式
  ✅ 插件生态丰富
  ✅ 分布式压测支持
  ✅ 多协议支持
```

---

## 2. 安装与目录结构

### 2.1 安装

```bash
# 前提：JDK 8+（推荐 11 或 17）
java -version

# 1. 下载：https://jmeter.apache.org/download_jmeter.cgi
# 2. 解压到任意目录

# 3. 启动 GUI
cd apache-jmeter-5.6.3/bin
./jmeter           # Linux/Mac
jmeter.bat         # Windows

# 4. 启动 CLI（压测时必须用 CLI）
jmeter -n -t test.jmx -l result.jtl
```

### 2.2 目录结构

```text
apache-jmeter-5.6.3/
├── bin/
│   ├── jmeter          → 启动脚本（GUI）
│   ├── jmeter.bat      → Windows 启动
│   ├── jmeter.properties → 核心配置
│   ├── user.properties    → 用户自定义配置
│   └── report-template/  → Dashboard 报告模板
├── lib/
│   ├── ext/            → 插件目录（放 .jar）
│   └── *.jar           → 核心库
├── docs/               → 文档
├── printable_docs/     → 可打印文档
└── extras/             → 额外工具
```

---

## 3. GUI 界面详解

```text
┌─────────────────────────────────────────────────────────┐
│  [File] [Edit] [Search] [Run] [Options] [Help]          │
├─────────────────────┬───────────────────────────────────┤
│                     │                                   │
│   左侧：树形导航     │     右侧：元件配置面板              │
│                     │                                   │
│   📁 Test Plan      │  Name: HTTP Request               │
│   ├── Thread Group  │  Comments: ...                    │
│   │   ├── HTTP Req  │  ┌───────────────────────────┐   │
│   │   │   ├── Assert│  │ Protocol: https           │   │
│   │   │   └── Listnr│  │ Server:   api.example.com │   │
│   │   ├── HTTP Req2 │  │ Port:     443             │   │
│   │   └── Listener  │  │ Method:   POST            │   │
│   └── Config Elem   │  │ Path:     /api/users      │   │
│                     │  └───────────────────────────┘   │
│                     │                                   │
└─────────────────────┴───────────────────────────────────┘

常用图标：
  📁 Test Plan             → 根节点，全局配置
  👥 Thread Group          → 虚拟用户组
  🌐 HTTP Request          → HTTP 请求采样器
  ✅ Response Assertion    → 断言
  📊 View Results Tree     → 调试用结果树
  📈 Aggregate Report      → 聚合报告
  ⚙️ CSV Data Set Config   → 数据驱动配置
```

---

## 4. Test Plan 测试计划

### 4.1 Test Plan 配置

| 配置项 | 说明 | 建议 |
|--------|------|------|
| **Name** | 测试计划名称 | 业务场景命名 |
| **Comments** | 说明 | 记录测试目的和数据 |
| **Run Thread Groups consecutively** | 串行执行线程组 | 一般不打勾（并行） |
| **Functional Test Mode** | 保存完整响应数据 | 调试时打开，压测时关闭 |
| **Add directory or jar to classpath** | 加载外部类 | JDBC 驱动等 |

### 4.2 元件树结构

```text
Test Plan (根)
├── Config Element (配置元件)     ← 全局配置，仅执行一次
├── Listener (监听器)             ← 可放全局，收集所有结果
│
├── Thread Group 1
│   ├── Config Element           ← 线程组级配置
│   ├── Pre-Processor            ← 每个请求前执行
│   ├── Sampler (采样器)         ← 核心：发请求
│   │   ├── Assertion (断言)     ← 验证响应
│   │   └── Post-Processor       ← 请求后提取数据
│   └── Listener                 ← 查看结果
│
└── Thread Group 2
    └── ...
```

---

## 5. Thread Group 线程组

### 5.1 核心参数

```text
┌──────────────────────────────────────────────┐
│ Thread Group 核心参数                         │
├──────────────────────────────────────────────┤
│                                              │
│  Number of Threads (users):  100            │
│    → 并发线程数 = 模拟的用户数                │
│                                              │
│  Ramp-up Period (seconds):  30              │
│    → 启动所有线程的时间                       │
│    → 本例：30秒内启动100个线程               │
│    → 启动速度：100/30 ≈ 3.3 线程/秒          │
│                                              │
│  Loop Count: 10                              │
│    → 每个线程执行测试的次数                   │
│    → Infinite：无限循环                       │
│    → 总请求数 = 100线程 × 10次 = 1000        │
│                                              │
│  Duration (seconds): 300                     │
│    → 持续运行时间（与 Loop Count 互斥）       │
│    → 更真实的场景："不管几次，跑5分钟"        │
│                                              │
│  Delay Thread creation until needed          │
│    → 延迟创建线程（节省内存）                 │
│                                              │
├──────────────────────────────────────────────┤
│  典型配置：                                   │
│  摸底测试：1线程 × 1次                        │
│  基准测试：10线程 × Ramp-up 10s               │
│  负载测试：逐步增加线程数                      │
│  压力测试：500+ 线程，找瓶颈                   │
└──────────────────────────────────────────────┘
```

### 5.2 三种 Thread Group

| 类型 | 用途 | 说明 |
|------|------|------|
| **普通 Thread Group** | 通用场景 | 固定线程数 |
| **Stepping Thread Group** | 阶梯加压 | 逐步增加并发（需插件） |
| **Ultimate Thread Group** | 复杂调度 | 多阶段不同并发（需插件） |

---

## 6. 元件执行顺序

```text
同一作用域内，按以下顺序执行：

  1. Configuration Elements (配置元件)
  2. Pre-Processors (前置处理器)
  3. Timer (定时器)
  4. Sampler (采样器 → 发请求)
  5. Post-Processors (后置处理器) —— 仅在 Sampler 成功时
  6. Assertions (断言) —— 仅在 Sampler 成功时
  7. Listeners (监听器) —— 仅在 Sampler 成功时

作用域继承规则：
  → 子元件继承父元件的配置
  → 同级元件按上述顺序执行
  → 子级元件覆盖父级同名配置

示例：
  Test Plan
  ├── HTTP Header Manager (全局请求头)
  └── Thread Group
      ├── User Defined Variables (线程组级变量)
      ├── CSV Data Set Config
      ├── HTTP Request 1
      │   ├── HTTP Header Manager (覆盖全局)  ← 只影响此请求
      │   └── Response Assertion
      └── HTTP Request 2
          └── JSON Assertion
```

---

## 7. 第一个测试脚本

### 7.1 创建 REST API 测试

```text
Step 1: Test Plan
  → Name: "用户服务性能测试"

Step 2: Thread Group
  → Number of Threads: 50
  → Ramp-up: 10
  → Loop Count: 10
  → 总请求 500 次

Step 3: HTTP Request Defaults (Config Element)
  → Protocol: https
  → Server: api.example.com
  → Port: 443
  → (所有 HTTP Request 自动继承)

Step 4: HTTP Header Manager
  → Content-Type: application/json
  → Authorization: Bearer ${token}

Step 5: HTTP Request
  → Method: GET
  → Path: /api/v1/users

Step 6: Response Assertion
  → Response Code: 200
  → Response Message: OK

Step 7: Listener
  → View Results Tree（调试用）
  → Aggregate Report（结果分析）
```

### 7.2 运行

```text
GUI 模式（调试/录制）：
  → 点击绿色 ▶ 按钮运行
  → View Results Tree 查看每个请求细节
  → Aggregate Report 看汇总数据

CLI 模式（正式压测）：
  jmeter -n -t user-service-test.jmx -l result.jtl -e -o report/
  → -n：非 GUI 模式
  → -t：测试计划文件
  → -l：结果文件
  → -e -o：生成 HTML Dashboard 报告
```

---

> 🎯 **核心要点**：Test Plan 是根，Thread Group 定义并发模型，Sampler 发请求，Assertion 验结果，Listener 看数据。**正式压测务必用 CLI 模式**，GUI 只用于调试。

---

*创建于：2026年7月*
