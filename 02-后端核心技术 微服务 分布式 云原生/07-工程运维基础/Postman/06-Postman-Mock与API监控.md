# Postman Mock 与 API 监控

> 🎭 前端无后端也能联调 —— Mock Server；24/7 自动检测 API 健康 —— Monitor 监控告警

---

## 📚 目录

1. [Mock Server](#1-mock-server)
2. [Mock 实战场景](#2-mock-实战场景)
3. [API Monitor](#3-api-monitor)
4. [监控告警配置](#4-监控告警配置)

---

## 1. Mock Server

### 1.1 什么是 Mock Server

```text
Mock Server = 模拟真实 API 的假服务器

使用场景：
  ✅ 后端接口还没开发完 → 前端先用 Mock 数据联调
  ✅ 第三方 API 有调用次数限制 → 测试时用 Mock
  ✅ 需要模拟各种边界情况（超时、错误码、慢响应）

创建方式：
  1. 选中 Collection → 右键 "Mock Collection"
  2. 或 Collection 详情页 → Mock 标签页 → Create Mock Server
```

### 1.2 Mock 配置

```text
Mock Server 配置项：
  ├── Mock Server Name：标识名称
  ├── Environment：Mock 使用的环境（变量来源）
  ├── Simulate Network Delay：模拟网络延迟（可设为 0）
  └── Make Private：是否私有（需 API Key 访问）

创建后得到：
  Mock URL：https://{{mockId}}.mock.pstmn.io/api/users

  GET https://{{mockId}}.mock.pstmn.io/api/users
  → 返回该请求保存的 Example 中的响应！
```

### 1.3 Example 的作用

```text
每个 Request 可以有多个 Example：

  GET /users 的 3 个 Example：
  ├── "正常用户列表"
  │   Response (200):
  │   { "code": 200, "data": [{ "id": 1, "name": "Alice" }] }
  │
  ├── "空列表"
  │   Response (200):
  │   { "code": 200, "data": [] }
  │
  └── "服务异常"
      Response (500):
      { "code": 500, "message": "Internal Server Error" }

Mock Server 根据请求参数返回匹配的 Example！
如果 URL 相同但 Query 参数不同 → 返回不同 Example
```

### 1.4 Mock 的高级匹配

```text
Mock 匹配规则（优先级从高到低）：

  1. x-mock-match-request-headers：
     精确匹配请求头 → 返回指定 Example
     如：req-header: {"x-scenario": "error"}
     → 发送请求时带上 x-scenario: error → 返回 500

  2. x-mock-match-request-body：
     精确匹配请求体 → 返回指定 Example
     如：req-body: {"status": "deleted"}

  3. 智能匹配：
     → URL 参数匹配
     → 无匹配时返回第一个 Example
```

---

## 2. Mock 实战场景

### 2.1 模拟 CRUD

```text
Collection: User Mock API

  GET  /users      → 返回列表
  GET  /users/:id  → 返回单个用户
  POST /users      → 返回创建成功
  PUT  /users/:id  → 返回更新成功
  DELETE /users/:id → 返回删除成功

每个写好 Example，Mock URL 即可响应所有操作。
前端不需要任何后端代码就能跑通整个流程！
```

### 2.2 模拟分页/搜索

```javascript
// Mock Server 会自动匹配 Query 参数不同的请求

// GET /users?page=1&size=10  → 返回第一页
// Example: "第一页"
{ "code": 200, "data": { "list": [...10条], "total": 95, "page": 1 } }

// GET /users?page=10&size=10 → 返回最后一页
// Example: "最后一页"
{ "code": 200, "data": { "list": [...5条], "total": 95, "page": 10 } }
```

---

## 3. API Monitor

### 3.1 Monitor 是什么

```text
Monitor = 定时运行的 API 测试 + 异常告警

使用场景：
  ✅ 生产环境 API 健康检查（每 5 分钟一次）
  ✅ SLA 监控（P99 响应时间 > 500ms → 告警）
  ✅ 核心业务流程定期回归（每小时跑一遍）
  ✅ 第三方 API 可用性监控

Monitor 会：
  1. 按设定频率运行 Collection
  2. 记录每次运行的结果和响应时间
  3. 失败/超时 → 发送告警通知
```

### 3.2 配置 Monitor

```text
配置项：
  ├── Monitor Name：监控名称
  ├── Collection：要运行的集合
  ├── Environment：使用的环境
  ├── Run Frequency：运行频率
  │   └── 5 minutes / 15 minutes / 1 hour / Daily / Weekly
  ├── Region：运行区域（选择离服务器最近的）
  ├── Retry on Failure：失败是否重试
  └── Request Timeout：请求超时时间
```

---

## 4. 监控告警配置

### 4.1 可用监控指标

```text
Monitor 结果指标：

  ├── 请求成功率
  │   ├── All tests passed → ✅ Healthy
  │   └── Any test failed  → ❌ Unhealthy
  │
  ├── 响应时间变化
  │   └── 平均值显著上升 → ⚠️ 性能退化
  │
  └── 虚拟用户数（从不同区域模拟并发）

告警触发条件：
  → 请求失败
  → 测试断言失败
  → 响应超时
  → 响应时间超过阈值
```

### 4.2 集成通知渠道

```text
Postman 支持的告警通知渠道：
  ├── Email（团队邮件）
  ├── Slack（频道消息）
  ├── Microsoft Teams
  ├── PagerDuty
  ├── Webhook（自定义接收）
  └── Datadog

推荐配置：
  → 非工作时间 + 非关键 API → Email（避免骚扰）
  → 核心支付链路 → Slack + PagerDuty（立即响应）
```

### 4.3 健康检查 Collection 模板

```text
构建生产环境健康检查 Collection：

  📋 Health Check Collection
  ├── GET /actuator/health     → Status 200 + {"status":"UP"}
  ├── GET /api/v1/users?size=1  → Status 200 + 非空数据
  ├── POST /api/v1/auth/login   → Status 200 + 返回 token
  └── GET /api/v1/orders?size=1 → Status 200

  每个请求的 Tests：
  pm.test("Status 200", () => pm.response.to.have.status(200));
  pm.test("Response < 2s", () => pm.expect(pm.response.responseTime).below(2000));
```

---

> 🎯 **核心要点**：Mock Server 让前后端并行开发，Monitor 让 API 问题早于用户发现。Mock + Monitor 构成了开发到运维的 API 全生命周期保障。

---

*创建于：2026年7月*
