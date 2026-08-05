# JMeter 断言与监听器

> ✅ 响应断言、JSON 断言、大小断言、持续时间断言 + 聚合报告、Dashboard、结果树 —— 验证与结果分析

---

## 📚 目录

1. [断言概述](#1-断言概述)
2. [常用断言详解](#2-常用断言详解)
3. [监听器 Listener](#3-监听器-listener)
4. [聚合报告解读](#4-聚合报告解读)
5. [HTML Dashboard 报告](#5-html-dashboard-报告)

---

## 1. 断言概述

```text
Assertion = 验证 Sampler 的响应是否符合预期

断言时机：
  → Sampler 成功返回 → 执行 Post-Processor → 执行 Assertion
  → Sampler 失败（超时/连接失败）→ 不执行 Assertion → 直接标记为失败

断言作用域：
  → 放在 Sampler 下：仅对该 Sampler 生效
  → 放在 Controller 下：对该 Controller 下所有 Sampler 生效
  → 放在 Test Plan / Thread Group 下：全局生效

断言失败后的行为：
  → 默认：该 Sampler 标记为失败，继续执行后续请求
  → 可以在 Thread Group 设置 "Stop Thread on Error"
```

---

## 2. 常用断言详解

### 2.1 Response Assertion（最常用）

```text
Response Assertion 配置：

  Apply to:
    Main sample only          → 仅主请求
    Main sample + sub-samples → 含内嵌资源
    Sub-samples only          → 仅子请求

  Field to Test:
    Response Code   → 200, 201, 404...
    Response Message → OK, Created, Not Found...
    Response Body    → 响应体文本
    Response Headers → Content-Type: application/json
    URL Sampled      → 请求 URL

  Pattern Matching Rules:
    Contains       → 包含指定文本（最常用）
    Matches        → 正则匹配
    Equals         → 完全等于
    Substring      → 子字符串
    Not            → 不包含/不匹配

  Patterns to Test:
    ┌────────────────────────────────────┐
    │ 200              ← 期望 200 状态码 │
    │ OK               ← 期望 OK 消息    │
    │ "success":true   ← 响应体包含      │
    └────────────────────────────────────┘
```

### 2.2 JSON Assertion

```json
// 假设响应为：
{
  "code": 200,
  "data": {
    "id": 12345,
    "name": "Alice",
    "email": "alice@test.com"
  },
  "message": "success"
}
```

```text
JSON Assertion 配置（JSON Path）：

  Assert JSON Path exists：
    $.code                        → 验证 code 字段存在
    $.data                        → 验证 data 字段存在

  Additionally assert value：
    $.code    → 200               → 验证 code 等于 200
    $.data.name → Alice           → 验证 name 等于 Alice
    $.message → success           → 验证 message

  ☑ Validate against expected JSON Schema
    → 粘贴 JSON Schema 进行结构校验

  Invert assertion (NOT)：
    → 断言"不满足以下条件"
```

### 2.3 其他常用断言

| 断言 | 功能 | 配置 |
|------|------|------|
| **Duration Assertion** | 响应时间断言 | Max Duration: 5000ms → 超 5s 失败 |
| **Size Assertion** | 响应大小断言 | Max Size: 10240 bytes |
| **XPath Assertion** | XML 内容断言 | XPath: /user/name |
| **BeanShell Assertion** | 自定义脚本断言 | 自由编写验证逻辑 |
| **JSR223 Assertion** | 高性能脚本断言 | Groovy 脚本 |

```groovy
// JSR223 Assertion 示例（Groovy）
// 比 BeanShell 快 10-50 倍！

def response = prev.getResponseDataAsString();
def json = new groovy.json.JsonSlurper().parseText(response);

// 自定义验证逻辑
assert json.code == 200 : "期望 200，实际 ${json.code}";
assert json.data != null : "data 不能为空";
assert json.data.name.length() >= 2 : "名字至少两个字符";

// prev = SampleResult 对象，可访问请求/响应所有信息
// prev.setSuccessful(false)  → 标记失败
// prev.setResponseMessage("自定义错误信息")
```

---

## 3. 监听器 Listener

### 3.1 监听器分类

```text
调试用（GUI 模式，压测时禁用！）：
  ├── View Results Tree       → 查看每个请求的详细响应
  ├── View Results in Table   → 表格形式查看
  └── Assertion Results       → 查看断言结果

统计用（压测时使用）：
  ├── Aggregate Report        → 核心聚合统计数据（推荐）
  ├── Summary Report          → 简化版汇总
  ├── Aggregate Graph         → 聚合结果图表
  └── Graph Results           → 简单趋势图

报告生成用：
  ├── Simple Data Writer      → 最小开销写结果到文件
  └── Generate Summary Results → 生成汇总行

存储用：
  └── Backend Listener        → 发送结果到 InfluxDB/Graphite
```

### 3.2 View Results Tree（调试必备）

```text
调试时使用，可查看：

  Sampler Result 标签：
    ├── Thread Name: Thread Group 1-1
    ├── Load time: 45ms
    ├── Connect time: 12ms
    ├── Latency: 45ms
    ├── Size in bytes: 1024
    ├── Sent bytes: 256
    ├── Response Code: 200
    └── Response Message: OK

  Request 标签：
    → 查看实际发送的请求（URL、Headers、Body）

  Response 标签：
    → 查看原始响应文本/JSON/HTML

  ⚠️ 压测时禁用！大量数据会 OOM + 严重拖慢测试
```

---

## 4. 聚合报告解读

### 4.1 核心指标

```text
Aggregate Report 列说明：

┌──────────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┐
│ Label    │#Samp │Average│Median│ 90% │ 95% │ 99% │Min  │Max   │
├──────────┼──────┼──────┼──────┼──────┼──────┼──────┼──────┤
│ 用户注册  │ 1000 │ 320  │ 280  │ 450  │ 520  │ 890  │ 45  │ 1200 │
│ 用户查询  │ 5000 │ 45   │ 38   │ 62   │ 78   │ 120  │ 12  │ 350  │
│ 用户登录  │ 2000 │ 210  │ 180  │ 320  │ 380  │ 650  │ 35  │ 900  │
│ TOTAL    │ 8000 │ 158  │ 95   │ 280  │ 350  │ 680  │ 12  │ 1200 │
└──────────┴──────┴──────┴──────┴──────┴──────┴──────┴──────┴──────┘

指标含义：
  #Samples：  请求总数
  Average：   平均响应时间 (ms)
  Median：    中位数（50% 请求的响应时间）
  90% Line：  90% 请求的响应时间 → 10% 的用户超过这个时间
  95% Line：  95% 请求的响应时间
  99% Line：  99% 请求的响应时间 → 长尾延迟
  Min/Max：   最小/最大响应时间
  Error%：    错误率
  Throughput：吞吐量（请求/秒）
  Received/Sent KB/sec：接收/发送速率
```

### 4.2 关键指标分析

```text
性能分析三要素（黄金信号）：

  1. 吞吐量 Throughput
     → 系统每秒能处理多少请求
     → 太低 → 需优化

  2. 响应时间（关注 P99！）
     → 平均值会掩盖长尾问题
     → P99 超过阈值 → 部分用户体验差

  3. 错误率 Error%
     → 非 0% → 立即排查！
     → 允许的误差：< 0.1%

  典型异常模式：
    → 吞吐量上不去 + 响应时间稳定 → 线程不够
    → 吞吐量上不去 + 响应时间上升 → 系统瓶颈（CPU/DB/连接池）
    → 错误率突增 → 系统崩溃阈值
```

---

## 5. HTML Dashboard 报告

### 5.1 生成报告

```bash
# 先生成 .jtl 结果文件
jmeter -n -t test.jmx -l result.jtl

# 再从 .jtl 生成 Dashboard
jmeter -g result.jtl -o report/

# 或者一步到位
jmeter -n -t test.jmx -l result.jtl -e -o report/
```

### 5.2 Dashboard 核心面板

```text
Dashboard 报告内容：

  ├── APDEX (Application Performance Index) → 应用性能指数
  │   └── 满意 ≤ T, 可容忍 ≤ 4T, 不满意 > 4T
  │   └── APDEX = (满意 + 容忍/2) / 总数
  │
  ├── Statistics 统计表
  │   └── 同 Aggregate Report 的详细指标
  │
  ├── Response Time Percentiles
  │   └── 各百分位的折线图
  │
  ├── Response Time Overview
  │   └── 响应时间随时间变化趋势
  │
  ├── Active Threads Over Time
  │   └── 活跃线程数变化
  │
  ├── Response Time vs Request
  │   └── 各接口的响应时间中位数 + 分布
  │
  ├── Top 5 Errors by Sampler
  │   └── 出错最多的 5 个接口
  │
  └── Errors Table
      └── 所有错误的详细列表
```

---

> 🎯 **核心要点**：**调试用 View Results Tree，压测用 Aggregate Report**。断言优先用 Response Assertion + JSON Assertion，复杂逻辑用 JSR223 (Groovy)。最终报告靠 HTML Dashboard。

---

*创建于：2026年7月*
