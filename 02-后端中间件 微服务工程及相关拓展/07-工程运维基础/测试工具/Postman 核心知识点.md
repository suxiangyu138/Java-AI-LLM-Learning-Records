# Postman 核心知识点

> API 全生命周期管理工具 —— 从设计、调试、测试、文档到监控的一站式平台，2026 年已深度集成 AI 能力。

**官网：** https://www.postman.com

---

## 目录

1. [概述](#1-概述)
2. [请求构建与协议支持](#2-请求构建与协议支持)
3. [Collections 集合管理与组织架构](#3-collections-集合管理与组织架构)
4. [环境变量与作用域](#4-环境变量与作用域)
5. [Pre-request Scripts 前置脚本](#5-pre-request-scripts-前置脚本)
6. [Tests 断言与测试脚本](#6-tests-断言与测试脚本)
7. [动态变量参考表](#7-动态变量参考表)
8. [Postman Runner 与 Newman CLI](#8-postman-runner-与-newman-cli)
9. [Mock Server 模拟服务](#9-mock-server-模拟服务)
10. [API 文档自动生成](#10-api-文档自动生成)
11. [Monitors 监控器](#11-monitors-监控器)
12. [Postman Flows 可视化流程编排](#12-postman-flows-可视化流程编排)
13. [WebSocket 测试](#13-websocket-测试)
14. [GraphQL 测试](#14-graphql-测试)
15. [Postbot AI 增强](#15-postbot-ai-增强)
16. [团队协作与版本控制](#16-团队协作与版本控制)
17. [完整示例：User CRUD 集合](#17-完整示例user-crud-集合)
18. [导出 / 导入与备份](#18-导出--导入与备份)
19. [替代品对比](#19-替代品对比)
20. [总结](#20-总结)

---

## 1. 概述

Postman 是当前全球使用最广泛的 API 开发和测试工具。最初仅作为 Chrome 插件（Postman Interceptor）存在，现已发展为拥有 2500 万+ 注册用户的独立桌面客户端，支持 REST、GraphQL、gRPC、WebSocket 等多种协议。

### 1.1 核心定位

```
API 全生命周期管理
├── 设计阶段 ──→ 蓝图（API Builder）、Schema 定义
├── 开发阶段 ──→ 请求调试、Collections 组织
├── 测试阶段 ──→ Pre-request Script + Tests + Runner
├── 文档阶段 ──→ 自动生成交互式 API 文档
├── 模拟阶段 ──→ Mock Server 提前联调
└── 监控阶段 ──→ Monitors 定时健康检查
```

### 1.2 版本信息

| 版本 | 说明 |
|------|------|
| **Postman Desktop App** | 全功能桌面客户端（Windows / macOS / Linux） |
| **Postman Web** | 浏览器端轻量版，需配合桌面 Agent 使用 |
| **Postman CLI / Newman** | 命令行运行器，用于 CI/CD 集成 |
| **Postman Cloud** | 云端 Workspace、Monitors、Mock Server |

> 💡 2026 年 Postman 已全面接入 AI（Postbot），支持自然语言生成测试脚本、Mock 数据和响应解读。

---

## 2. 请求构建与协议支持

### 2.1 HTTP 请求构建

Postman 提供可视化的 HTTP 请求编辑器：

```
HTTP Request 结构
├── Method: GET / POST / PUT / PATCH / DELETE / HEAD / OPTIONS
├── URL: {{base_url}}/api/users/{{user_id}}   ← 支持环境变量
├── Params: Query 参数可视化编辑
├── Headers: 常见 Header 预设 + 自定义
├── Body:
│   ├── none
│   ├── form-data (multipart/form-data)
│   ├── x-www-form-urlencoded
│   ├── raw (JSON / XML / Text / HTML / JavaScript)
│   └── binary (文件上传)
└── Auth: Inherit / No Auth / API Key / Bearer Token / OAuth 2.0 / Digest / AWS Signature / NTLM
```

### 2.2 支持的协议

| 协议 | 说明 | 适用场景 |
|------|------|----------|
| **REST** | 传统 HTTP API | 最常用，适用于绝大多数字服务 |
| **GraphQL** | 声明式查询语言 | 前端灵活控制返回字段 |
| **gRPC** | 基于 Protobuf 的高性能 RPC | 微服务间通信 |
| **WebSocket** | 全双工通信协议 | 实时消息、推送服务 |
| **Socket.IO** | 基于 WebSocket 的封装 | 兼容老版本浏览器 |
| **MQTT** | 物联网消息协议 | IoT 设备通信 |

### 2.3 认证方式详解

```javascript
// Bearer Token 自动注入（Pre-request Script 或 Tests 中）
pm.environment.set("token", res.json().accessToken);

// OAuth 2.0 流程自动完成
// 设置: Authorization → OAuth 2.0 → Get New Access Token
```

> 💡 Postman 自动保存 Token 到当前环境变量，过期自动刷新。

---

## 3. Collections 集合管理与组织架构

### 3.1 集合层级与文件夹组织

Collections 是 Postman 的核心组织单元，建议按以下方式分层：

```
Project Name
├── 📁 Auth
│   ├── POST Login
│   └── POST Refresh Token
├── 📁 Users
│   ├── GET List Users
│   ├── POST Create User
│   ├── GET Get User By ID
│   ├── PUT Update User
│   └── DELETE Delete User
├── 📁 Orders
│   ├── GET List Orders
│   ├── POST Create Order
│   └── GET Get Order Detail
├── 📁 Reports
│   └── GET Generate Report
└── 📁 Health
    └── GET Health Check
```

### 3.2 集合级脚本

集合支持在 **集合级别** 定义 Pre-request 和 Tests 脚本，该文件夹下的所有请求自动继承：

```javascript
// Collection-level Pre-request Script（所有请求自动执行）
const baseUrl = pm.environment.get("base_url");
const token = pm.environment.get("token");

if (token && pm.request.url.toString().startsWith(baseUrl)) {
    pm.request.headers.add({
        key: "Authorization",
        value: "Bearer " + token
    });
}
```

### 3.3 集合变量（Collection Variables）

```javascript
// 集合变量：仅属于当前集合，优先级低于 Environment 变量
pm.collectionVariables.set("defaultPageSize", "20");
pm.collectionVariables.get("defaultPageSize");
pm.collectionVariables.unset("defaultPageSize");
```

> 💡 **最佳实践：** 集合变量存储集合级别的固定参数（如默认分页大小、API 版本号）；环境变量存储环境差异值（域名、Token）；全局变量存储跨集合共享值。

### 3.4 集合描述与文档

```markdown
# 用户管理 API 集合
> 提供用户 CRUD 操作的完整接口

## 使用前必读
1. 先执行 `Auth/Login` 获取 Token
2. Token 自动注入到后续请求的 Authorization Header
3. 支持分页查询，默认每页 20 条

## 环境要求
- Dev: {{base_url}} = http://localhost:8080
- Staging: {{base_url}} = https://staging-api.example.com
```

---

## 4. 环境变量与作用域

### 4.1 变量作用域层次

Postman 的变量遵循 **就近优先** 原则：

```
全局变量 (Globals)           ← 最低优先级
   └── 集合变量 (Collection)
          └── 环境变量 (Environment)
                 └── 数据变量 (Data)
                        └── 局部变量 (Local)  ← 最高优先级
```

### 4.2 变量操作 API

```javascript
// ─── 环境变量（Environment）───
pm.environment.get("key");         // 读取
pm.environment.set("key", "val");  // 写入
pm.environment.unset("key");       // 删除

// ─── 全局变量（Globals）───
pm.globals.get("key");
pm.globals.set("key", "val");
pm.globals.unset("key");

// ─── 集合变量（Collection Variables）───
pm.collectionVariables.get("key");
pm.collectionVariables.set("key", "val");
pm.collectionVariables.unset("key");

// ─── 局部变量（Local Variables）───
pm.variables.get("key");          // 从当前作用域链查找
pm.variables.set("key", "val");   // 设置到当前最高优先级作用域
pm.variables.replaceIn("{{key}}"); // 字符串中的变量替换
```

### 4.3 典型环境配置

| 环境 | base_url | 说明 |
|------|----------|------|
| **Local Dev** | `http://localhost:8080` | 本地开发 |
| **Development** | `http://dev-api.example.com` | 开发服务器 |
| **Staging** | `https://staging-api.example.com` | 预发布 |
| **Production** | `https://api.example.com` | 生产环境（只读测试） |

### 4.4 环境变量文件示例

```json
{
    "name": "Development",
    "values": [
        { "key": "base_url",  "value": "http://localhost:8080", "type": "default" },
        { "key": "username",  "value": "admin",                "type": "default" },
        { "key": "password",  "value": "admin123",             "type": "secret" },
        { "key": "token",     "value": "",                     "type": "default" },
        { "key": "pageSize",  "value": "20",                   "type": "default" }
    ]
}
```

> ⚠️ **安全提示：** 敏感变量（密码、Token、Secret Key）设置 `type: "secret"`，在 Postman UI 中会被隐藏显示。切勿将生产环境凭据提交到版本控制。

---

## 5. Pre-request Scripts 前置脚本

### 5.1 典型使用场景

Pre-request Script 在 **请求发送之前** 执行，常用于：

| 场景 | 说明 |
|------|------|
| **生成时间戳** | 动态设置请求中的 timestamp 参数 |
| **HMAC 签名** | 计算请求签名（Signature） |
| **Token 自动获取** | 请求前检查 Token 是否过期，过期自动刷新 |
| **随机测试数据** | 生成随机用户名、邮箱、手机号 |
| **环境变量初始化** | 确保必备变量已设置，未设置则自动生成 |
| **请求参数动态构造** | 从前置请求的响应中提取数据 |

### 5.2 时间戳与 HMAC 签名

```javascript
// ─── 生成时间戳 ───
const timestamp = Date.now();  // 毫秒时间戳
pm.environment.set("timestamp", timestamp);

// 格式化时间字符串
const now = new Date();
const formattedTime = now.toISOString().replace(/[:-]/g, "").split(".")[0] + "Z";
pm.environment.set("formattedTime", formattedTime);

// ─── HMAC-SHA256 签名 ───
const CryptoJS = require("crypto-js");

const method = pm.request.method;
const path = pm.request.url.getPath();
const body = pm.request.body ? JSON.stringify(pm.request.body.raw) : "";
const secretKey = pm.environment.get("apiSecret");

const message = method + path + timestamp + body;
const signature = CryptoJS.HmacSHA256(message, secretKey).toString(CryptoJS.enc.Hex);

pm.request.headers.add({
    key: "X-Signature",
    value: signature
});
pm.request.headers.add({
    key: "X-Timestamp",
    value: timestamp.toString()
});
```

> 💡 在 Pre-request 中使用 `pm.request.headers.add()` 动态添加 HTTP Header，比在 UI 中写死更灵活。

### 5.3 自动获取并刷新 Token

```javascript
// ─── Token 自动管理逻辑 ───
const token = pm.environment.get("token");
const tokenExpiry = pm.environment.get("tokenExpiry");

// Token 不存在或已过期，重新获取
if (!token || !tokenExpiry || Date.now() > parseInt(tokenExpiry)) {
    const loginRequest = {
        url: pm.environment.get("base_url") + "/auth/login",
        method: "POST",
        header: {
            "Content-Type": "application/json"
        },
        body: {
            mode: "raw",
            raw: JSON.stringify({
                username: pm.environment.get("username"),
                password: pm.environment.get("password")
            })
        }
    };

    pm.sendRequest(loginRequest, (err, res) => {
        if (!err) {
            const body = res.json();
            pm.environment.set("token", body.accessToken);
            // Token 有效期 1 小时（3600000 ms）
            pm.environment.set("tokenExpiry", String(Date.now() + 3600000));
            pm.environment.set("refreshToken", body.refreshToken);
        }
    });
}
```

### 5.4 随机测试数据生成

```javascript
// ─── 生成随机测试数据 ───
const chance = require("chance");

pm.environment.set("randomName", chance.name());
pm.environment.set("randomEmail", chance.email({ domain: "test.example.com" }));
pm.environment.set("randomPhone", chance.phone());
pm.environment.set("randomUUID", chance.guid());
pm.environment.set("randomInt", chance.integer({ min: 1, max: 1000 }));
pm.environment.set("randomSentence", chance.sentence({ words: 5 }));
```

### 5.5 动态变量（Dynamic Variables）

```javascript
// 直接使用 Postman 内置动态变量（见第 7 章）
// Pre-request 中也可以手动生成
pm.variables.set("requestId", "{{$guid}}");
pm.variables.set("createdAt", "{{$timestamp}}");
pm.variables.set("randomEmail", "user_{{9$randomInt}}@test.com");
```

---

## 6. Tests 断言与测试脚本

### 6.1 断言基础

Postman 的 Tests 脚本基于 JavaScript + **Chai Assertion Library**（BDD 风格），在请求返回后执行：

```javascript
// ─── 基础结构 ───
pm.test("测试用例名称", () => {
    // 断言逻辑
});

// ─── 状态码断言 ───
pm.test("状态码为 200", () => {
    pm.response.to.have.status(200);
});

pm.test("状态码为 2xx", () => {
    pm.response.to.have.status(200);
    // 或：pm.response.to.be.success;   // 2xx
    // 或：pm.response.to.be.error;     // 4xx/5xx
    // 或：pm.response.to.be.clientError;  // 4xx
    // 或：pm.response.to.be.serverError;  // 5xx
});
```

### 6.2 pm.expect() 断言全览

```javascript
const jsonData = pm.response.json();

// ─── 类型断言 ───
pm.expect(jsonData).to.be.an("object");
pm.expect(jsonData.data).to.be.an("array");
pm.expect(jsonData.message).to.be.a("string");

// ─── 值断言 ───
pm.expect(jsonData.code).to.equal(0);
pm.expect(jsonData.data.length).to.equal(10);
pm.expect(jsonData.data.length).to.be.greaterThan(0);
pm.expect(jsonData.data.length).to.be.below(100);
pm.expect(jsonData.page).to.be.within(1, 100);

// ─── 属性存在 ───
pm.expect(jsonData).to.have.property("data");
pm.expect(jsonData.data[0]).to.have.all.keys("id", "name", "email", "createdAt");

// ─── 正则匹配 ───
pm.expect(jsonData.data[0].email).to.match(/^[\w.-]+@[\w.-]+\.\w+$/);

// ─── null / undefined / truthy ───
pm.expect(jsonData.data).to.not.be.null;
pm.expect(jsonData.data).to.not.be.undefined;
pm.expect(jsonData.success).to.be.true;
pm.expect(jsonData.success).to.not.be.false;

// ─── 深度相等 ───
pm.expect(jsonData).to.deep.equal({
    code: 0,
    message: "success",
    data: []
});
```

### 6.3 pm.response 断言快捷方式

```javascript
// ─── 响应体断言 ───
pm.response.to.have.status(200);
pm.response.to.have.header("Content-Type");
pm.response.to.have.header("X-Request-Id");
pm.response.to.have.body("登录成功");
pm.response.to.have.jsonBody();  // 验证是合法 JSON

// ─── 响应时间断言 ───
pm.test("响应时间在 500ms 以内", () => {
    pm.expect(pm.response.responseTime).to.be.below(500);
});

// ─── Cookie 断言 ───
pm.test("包含 Session Cookie", () => {
    pm.expect(pm.response.cookies.has("SESSION_ID")).to.be.true;
});
```

### 6.4 JSON Schema 校验

使用 `tv4`（Tiny Validator for JSON Schema）进行响应结构校验：

```javascript
// ─── JSON Schema 校验 ───
const schema = {
    type: "object",
    required: ["code", "message", "data"],
    properties: {
        code: { type: "integer", minimum: 0 },
        message: { type: "string" },
        data: {
            type: "array",
            items: {
                type: "object",
                required: ["id", "name", "email"],
                properties: {
                    id: { type: "integer" },
                    name: { type: "string", minLength: 1 },
                    email: { type: "string", format: "email" },
                    age: { type: "integer", minimum: 0, maximum: 150 }
                }
            }
        }
    }
};

pm.test("响应结构符合 Schema", () => {
    const jsonData = pm.response.json();
    pm.expect(tv4.validate(jsonData, schema)).to.be.true;
});
```

### 6.5 数据传递与链式请求

```javascript
// ─── 提取响应数据到环境变量 ───
const jsonData = pm.response.json();

pm.test("提取用户 ID", () => {
    pm.expect(jsonData.data).to.have.property("id");
    pm.environment.set("userId", jsonData.data.id);
    pm.environment.set("userEmail", jsonData.data.email);
});

// ─── 执行后续请求 ───
pm.test("创建资源后查询验证", () => {
    pm.sendRequest({
        url: pm.environment.get("base_url") + "/api/users/" + pm.environment.get("userId"),
        method: "GET",
        header: { "Authorization": "Bearer " + pm.environment.get("token") }
    }, (err, res) => {
        pm.expect(res.code).to.equal(200);
        const user = res.json().data;
        pm.expect(user.email).to.equal(pm.environment.get("userEmail"));
    });
});
```

### 6.6 迭代与数据驱动测试

```javascript
// ─── 数据驱动测试（配合 Data File）───
const row = pm.iterationData.get("username");

pm.test(`用户 ${row} 创建成功`, () => {
    pm.response.to.have.status(201);
    pm.expect(pm.response.json().data.name).to.equal(row);
});

// ─── 循环计数 ───
pm.test(`第 ${pm.info.iteration + 1} 次迭代通过`, () => {
    pm.response.to.have.status(200);
});
```

---

## 7. 动态变量参考表

Postman 内置动态变量，在请求 URL、Headers、Body 中直接使用：

| 变量 | 示例输出 | 说明 |
|------|----------|------|
| `{{$guid}}` | `c56a4180-65aa-42ec-a945-5fd21dec0538` | 随机 UUID v4 |
| `{{$timestamp}}` | `1721980800` | 当前 Unix 时间戳（秒） |
| `{{$isoTimestamp}}` | `2026-07-26T12:00:00.000Z` | ISO 8601 格式时间 |
| `{{$randomInt}}` | `782` | 0~1000 随机整数 |
| `{{$randomEmail}}` | `user.1234@example.com` | 随机邮箱 |
| `{{$randomPassword}}` | `aB3!xQ9$` | 随机密码（含大小写+数字+特殊字符） |
| `{{$randomPhone}}` | `+1-555-782-4901` | 随机手机号 |
| `{{$randomCity}}` | `Beijing` | 随机城市名 |
| `{{$randomStreet}}` | `Main Street` | 随机街道名 |
| `{{$randomName}}` | `Alice Johnson` | 随机姓名 |
| `{{$randomFullName}}` | `Dr. James Smith` | 随机全名 |
| `{{$randomBoolean}}` | `true` | 随机布尔值 |
| `{{$randomUUID}}` | `8f3e2a1c-...` | 同 `$guid` |
| `{{$randomUrl}}` | `https://example.com/path` | 随机 URL |
| `{{$randomIP}}` | `192.168.1.45` | 随机 IPv4 地址 |
| `{{$randomIPV6}}` | `::1` | 随机 IPv6 地址 |
| `{{$randomBankAccount}}` | `GB29NWBK60161331926819` | 随机银行账号 |
| `{{$randomHex}}` | `a3f2c8` | 随机十六进制字符串 |
| `{{$randomNationalID}}` | `123-45-6789` | 随机身份证号格式 |

> 💡 动态变量在 Pre-request、URL、Body、Headers 中均可使用，每次发送请求时重新生成。

---

## 8. Postman Runner 与 Newman CLI

### 8.1 Collection Runner

Runner 用于批量执行集合中的所有请求，支持数据驱动和迭代控制：

```
Runner 配置参数
├── Collection: 选择要运行的集合
├── Environment: 选择环境
├── Iterations: 迭代次数（配合数据文件可实现 N 组数据 N 次运行）
├── Delay: 请求间延迟（毫秒）
├── Data:
│   ├── CSV 文件（带 header 行）
│   └── JSON 文件（对象数组）
├── Keep variable values: 保留变量修改
├── Run collection without using stored cookies: 不使用 Cookie
└── Save responses: 保存响应日志
```

### 8.2 Newman 命令行运行器

Newman 是 Postman 的 CLI 工具，用于 CI/CD 流水线集成：

```bash
# 安装
npm install -g newman

# 基本运行
newman run collection.json

# 指定环境变量文件
newman run collection.json -e environment.json

# 指定全局变量文件
newman run collection.json -g globals.json

# 数据驱动
newman run collection.json -d test-data.csv

# 多迭代
newman run collection.json -n 5

# 设置延迟
newman run collection.json --delay-request 1000

# 时间超时
newman run collection.json --timeout-request 30000
```

### 8.3 Newman 报告器

```bash
# HTML 报告（需安装 htmlextra）
npm install -g newman-reporter-htmlextra
newman run collection.json -e env.json --reporters cli,htmlextra
newman run collection.json --reporters htmlextra --reporter-htmlextra-export ./report.html

# JUnit XML 报告（用于 Jenkins 等 CI 工具）
newman run collection.json --reporters junit --reporter-junit-export ./junit-report.xml

# JSON 报告
newman run collection.json --reporters json --reporter-json-export ./result.json
```

### 8.4 Newman CI/CD 集成

```yaml
# GitHub Actions 集成示例
name: API Tests

on: [push]

jobs:
  api-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: "20"
      - name: Install Newman
        run: npm install -g newman newman-reporter-htmlextra
      - name: Run API Tests
        run: |
          newman run postman/collection.json \
            -e postman/prod-env.json \
            --reporters cli,htmlextra
      - name: Upload Test Report
        uses: actions/upload-artifact@v4
        with:
          name: api-test-report
          path: ./newman/
```

```groovy
// Jenkins Pipeline 集成
pipeline {
    agent any
    stages {
        stage('API Test') {
            steps {
                sh 'npm install -g newman'
                sh 'newman run collection.json -e env.json -r cli,junit --reporter-junit-export report.xml'
            }
        }
        stage('Publish Report') {
            steps {
                junit 'report.xml'
            }
        }
    }
}
```

---

## 9. Mock Server 模拟服务

### 9.1 创建 Mock Server

Mock Server 允许在后端 API 尚未开发完成时，基于请求定义返回模拟数据：

```
创建步骤：
1. 选择一个 Collection
2. 右键 → Mock Server
3. 选择 Environment（可选）
4. 配置 Mock Server 名称
5. 获取 Mock URL: https://xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.mock.pstmn.io
```

### 9.2 Mock Server 示例

```javascript
// 在请求的响应中编写 Example Response
// Postman 会根据 Example 自动生成 Mock 响应

// Pre-request Script 中设置 Mock 行为
pm.environment.set("mockResponseDelay", "200");  // 模拟延迟 200ms
pm.environment.set("mockStatusCode", "200");      // 模拟状态码
```

### 9.3 Mock 调用日志

Mock Server 记录所有请求日志，可在 Postman 控制台查看：
- 请求方法、路径、Headers
- 匹配的 Example 名称
- 响应状态码和延迟时间
- 未匹配的请求（返回 404）

> 💡 **Mock Server 最佳实践：** 每个 API 请求至少保存 2 个 Example（成功响应 + 错误响应），Mock 时会根据请求参数自动匹配最合适的 Example。

---

## 10. API 文档自动生成

### 10.1 文档生成

Postman 自动从 Collection 中生成交互式 API 文档：

```
文档内容自动包含：
├── 请求方法 + URL
├── 请求参数（Query / Path / Header）
├── 请求 Body 示例
├── 响应状态码
├── 响应 Body 示例（Example）
├── 响应 Headers
└── 代码片段（自动生成 20+ 语言的调用示例）
```

### 10.2 文档发布与分享

```bash
# 文档发布后获得公共 URL
# 任何人都可通过浏览器查看、调试 API
# 支持设置域名白名单 / 密码保护

# 示例文档 URL
https://documenter.getpostman.com/view/1234567/2sA3QmDnFv
```

### 10.3 文档配置

- **Readme 页面**：为集合编写使用指南、前置条件、注意事项
- **分组排序**：自定义文档中接口的排列顺序
- **代码生成**：支持 cURL、Python、Java、JavaScript、Go、Ruby 等语言

---

## 11. Monitors 监控器

Monitors 允许定时执行 Postman 集合，对生产 API 进行健康检查：

### 11.1 监控配置

```
监控配置参数
├── Collection: 选择要运行的集合
├── Environment: 选择环境
├── Schedule: 每 5 分钟 / 每小时 / 每天 / 自定义 Cron
├── Region: 选择运行区域（多个区域可选）
├── Notifications:
│   ├── Email
│   ├── Slack Webhook
│   └── PagerDuty
└── Run behavior: 失败时重试
```

### 11.2 监控用例

| 场景 | 检查内容 |
|------|----------|
| **服务健康** | 定时调用 `/health` 确认服务存活 |
| **API 可用性** | 核心 API 状态码 + 响应时间监控 |
| **证书过期** | 检查 SSL 证书剩余有效期 |
| **数据完整性** | 定时检查关键数据是否正常返回 |
| **SLA 达标** | 统计 API 响应时间分布 |

> ⚠️ Monitors 在 Postman Cloud 运行，不会消耗本地资源。免费版每月 1000 次运行配额。

---

## 12. Postman Flows 可视化流程编排

Postman Flows 是一个可视化的 API 工作流构建器，无需编写代码即可编排多个 API 调用：

### 12.1 核心概念

```
Block（块）→ 连线 → Flow（流程）
├── Request Block: 发送 API 请求
├── Query Block: 从响应中提取数据
├── Transformation Block: 转换数据格式
├── Send Email Block: 发送通知
├── Condition Block: 条件判断
├── Delay Block: 延迟执行
└── Return Block: 返回数据
```

### 12.2 Flows 用例

```text
典型场景：用户注册自动化测试
1. [Request] POST /auth/register → 创建用户
2. [Query] 提取 userId
3. [Request] GET /users/{userId} → 验证用户存在
4. [Condition] 如果 status === 201
   ├── [Request] POST /auth/login → 验证可登录
   └── [Request] DELETE /users/{userId} → 清理测试数据
5. [Output] 返回测试结果报告
```

> 💡 Flows 适合自动化编排场景：数据同步、定时任务、多步骤 E2E 测试。

---

## 13. WebSocket 测试

### 13.1 建立 WebSocket 连接

```
操作步骤：
1. 新建请求 → 选择 WebSocket 协议
2. 输入 WebSocket URL: ws://localhost:8080/ws
3. 点击 Connect
```

### 13.2 WebSocket 消息测试

```javascript
// 发送消息
const message = JSON.stringify({
    type: "subscribe",
    channel: "orders",
    userId: pm.environment.get("userId")
});
pm.websocket.send(message);

// 接收消息验证
pm.websocket.on("message", (data) => {
    console.log("收到消息:", data);
    pm.test("接收订单更新消息", () => {
        const msg = JSON.parse(data);
        pm.expect(msg.type).to.equal("order_update");
        pm.expect(msg.data).to.have.property("orderId");
    });
});

// 断开连接
pm.websocket.close();
```

### 13.3 Socket.IO 测试

```javascript
// Socket.IO 协议支持
pm.io.on("connect", () => {
    console.log("Socket 已连接");
    pm.io.emit("join", { room: "test-room" });
});

pm.io.on("message", (data) => {
    pm.test("收到实时消息", () => {
        pm.expect(data).to.have.property("content");
    });
});
```

---

## 14. GraphQL 测试

### 14.1 GraphQL 请求构建

```
GraphQL Request
├── Method: POST（推荐）
├── URL: {{base_url}}/graphql
├── Body → GraphQL:
│   ├── Query: { users { id name email } }
│   └── Variables: { "page": 1, "size": 20 }
└── Headers: Content-Type: application/json
```

### 14.2 GraphQL 测试脚本

```javascript
// GraphQL 查询测试
const query = `
    query GetUsers($page: Int!, $size: Int!) {
        users(page: $page, size: $size) {
            id
            name
            email
            posts {
                title
            }
        }
    }
`;

pm.test("GraphQL 返回正确结构", () => {
    const response = pm.response.json();
    pm.expect(response).to.not.have.property("errors");
    pm.expect(response.data.users).to.be.an("array");
    pm.expect(response.data.users[0]).to.have.all.keys("id", "name", "email", "posts");
});

// GraphQL 变量预处理
const variables = {
    page: parseInt(pm.environment.get("pageNum")),
    size: parseInt(pm.environment.get("pageSize"))
};
pm.variables.set("variables", JSON.stringify(variables));
```

---

## 15. Postbot AI 增强

2026 年，Postman 内置 Postbot 智能助手，通过自然语言交互大幅提升效率：

### 15.1 AI 功能矩阵

| AI 功能 | 操作方式 | 说明 |
|---------|----------|------|
| **生成测试脚本** | 输入自然语言 → 自动生成 Tests JS 代码 | "验证返回数据不为空且包含 id 和 name" |
| **生成测试数据** | 描述数据需求 → 自动生成 Mock 数据 | "生成 5 条用户数据包含姓名邮箱" |
| **解释响应** | 选中响应体 → AI 解读 JSON 结构 | 自动分析响应字段含义 |
| **修复脚本** | 脚本报错 → AI 分析并给出修复建议 | "代码有语法错误，建议修改为..." |
| **查询构建** | 自然语言 → 自动构造复杂查询 | "获取最近 7 天登录过的用户" |
| **变量提取** | 选择响应字段 → 自动提取到变量 | 自动生成 pm.environment.set 代码 |

### 15.2 Postbot 使用示例

```text
用户输入: "验证状态码为 201，响应体包含 id 字段，
           且 id 不为 null，然后将 id 设置到环境变量"

Postbot 生成:
```

```javascript
pm.test("状态码为 201", () => {
    pm.response.to.have.status(201);
});

pm.test("响应包含 id 字段", () => {
    const jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property("id");
    pm.expect(jsonData.id).to.not.be.null;
    pm.environment.set("newResourceId", jsonData.id);
});
```

---

## 16. 团队协作与版本控制

### 16.1 Workspace 层次

```
Personal Workspace（个人空间）
    └── 个人测试、调试、探索

Team Workspace（团队空间）
    ├── 共享 Collections
    ├── 共享 Environments
    ├── 共享 Monitors
    └── 角色权限管理（Admin / Editor / Viewer）

Public Workspace（公共空间）
    └── API Network：对外发布的公开 API

Partner Workspace（合作伙伴空间）
    └── 与第三方合作共享
```

### 16.2 版本控制策略

```text
推荐做法：
├── Collection 导出为 JSON → 纳入 Git 仓库
├── Environment 导出为 JSON → 纳入 Git 仓库（加密敏感变量）
├── .gitignore 排除含真实密码的 env 文件
└── Team Workspace 作为中央同步枢纽

目录结构：
project-root/
├── postman/
│   ├── collections/
│   │   ├── user-api.json
│   │   └── order-api.json
│   ├── environments/
│   │   ├── dev.json
│   │   ├── staging.json
│   │   └── prod.json（.gitignore）
│   └── data/
│       └── test-users.csv
```

### 16.3 角色权限

| 角色 | 权限 |
|------|------|
| **Admin** | 管理成员、编辑集合、发布 API |
| **Editor** | 编辑集合和请求 |
| **Viewer** | 查看集合和请求（只读） |

---

## 17. 完整示例：User CRUD 集合

### 17.1 集合结构

```
User Management API
├── Auth
│   └── POST /auth/login
├── Users
│   ├── GET /users           （分页查询用户列表）
│   ├── POST /users          （创建用户）
│   ├── GET /users/:id       （查询单个用户）
│   ├── PUT /users/:id       （更新用户）
│   └── DELETE /users/:id    （删除用户）
```

### 17.2 环境变量

| 变量 | 初始值 | 获取方式 |
|------|--------|----------|
| `base_url` | `http://localhost:8080` | 手动设置 |
| `token` | (空) | Login 后自动设置 |
| `userId` | (空) | 创建用户后自动设置 |

### 17.3 Collection-level Pre-request Script

```javascript
// 自动注入 Token
const token = pm.environment.get("token");
const noAuthPaths = ["/auth/login"];

const requestPath = pm.request.url.getPath();
if (token && !noAuthPaths.some(path => requestPath.includes(path))) {
    pm.request.headers.add({
        key: "Authorization",
        value: "Bearer " + token
    });
}
```

### 17.4 各请求的 Pre-request + Tests

**POST /auth/login -- Pre-request Script:**

```javascript
// 无需特殊处理
```

**POST /auth/login -- Tests:**

```javascript
pm.test("登录成功", () => {
    pm.response.to.have.status(200);
    const body = pm.response.json();
    pm.expect(body).to.have.property("accessToken");
    pm.expect(body).to.have.property("refreshToken");
    pm.environment.set("token", body.accessToken);
    pm.environment.set("refreshToken", body.refreshToken);
});
```

**POST /users -- Pre-request Script:**

```javascript
// 生成随机用户数据
pm.variables.set("userName", "TestUser_{{9$randomInt}}");
pm.variables.set("userEmail", "test_{{9$randomInt}}@example.com");
pm.variables.set("userAge", {{$randomInt}} % 50 + 18);
```

**POST /users -- Tests:**

```javascript
pm.test("用户创建成功", () => {
    pm.response.to.have.status(201);
    const body = pm.response.json();
    pm.expect(body).to.have.property("id");
    pm.expect(body.name).to.equal(pm.variables.get("userName"));
    pm.environment.set("userId", body.id);
});
```

**GET /users/:id -- Tests:**

```javascript
pm.test("查询用户详情成功", () => {
    pm.response.to.have.status(200);
    const body = pm.response.json();
    pm.expect(body.id).to.equal(parseInt(pm.environment.get("userId")));
    pm.expect(body).to.have.all.keys("id", "name", "email", "age", "createdAt");
});
```

**PUT /users/:id -- Tests:**

```javascript
pm.test("更新用户成功", () => {
    pm.response.to.have.status(200);
    const body = pm.response.json();
    pm.expect(body.name).to.equal("UpdatedName");
});

pm.test("验证更新持久化", () => {
    pm.sendRequest({
        url: pm.environment.get("base_url") + "/api/users/" + pm.environment.get("userId"),
        method: "GET",
        header: { "Authorization": "Bearer " + pm.environment.get("token") }
    }, (err, res) => {
        pm.expect(res.json().name).to.equal("UpdatedName");
    });
});
```

**DELETE /users/:id -- Tests:**

```javascript
pm.test("删除用户成功", () => {
    pm.response.to.have.status(204);
});

pm.test("验证用户已删除", () => {
    pm.sendRequest({
        url: pm.environment.get("base_url") + "/api/users/" + pm.environment.get("userId"),
        method: "GET",
        header: { "Authorization": "Bearer " + pm.environment.get("token") }
    }, (err, res) => {
        pm.expect(res.code).to.equal(404);
    });
});
```

---

## 18. 导出 / 导入与备份

### 18.1 导出格式

```json
// collection.json（节选）
{
    "info": {
        "name": "User Management API",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "item": [
        {
            "name": "Login",
            "request": {
                "method": "POST",
                "url": {
                    "raw": "{{base_url}}/auth/login",
                    "host": ["{{base_url}}"],
                    "path": ["auth", "login"]
                }
            }
        }
    ]
}
```

```json
// environment.json（节选）
{
    "name": "Development",
    "values": [
        { "key": "base_url", "value": "http://localhost:8080", "type": "default" }
    ]
}
```

### 18.2 导入方式

| 来源 | 方式 |
|------|------|
| **文件** | 拖拽 JSON 文件到 Postman |
| **URL** | 输入集合公开 URL |
| **cURL** | 粘贴 cURL 命令自动解析 |
| **Swagger / OpenAPI** | 导入 OpenAPI 规范文件 |
| **Git 仓库** | 连接 GitHub / GitLab / Bitbucket 自动同步 |
| **Code Repository** | 从代码仓库同步 API 定义 |

---

## 19. 替代品对比

| 工具 | 核心特点 | 协议支持 | 价格模式 |
|------|----------|----------|----------|
| **Postman** | 生态最全、AI 赋能、全生命周期管理 | REST, GraphQL, gRPC, WS | 免费版够用，团队版收费 |
| **Apifox** | 国产、Postman + Swagger + Mock 一体 | REST, GraphQL, WebSocket | 免费 + 专业版 |
| **Insomnia** | 开源轻量、设计简洁、插件丰富 | REST, GraphQL, gRPC | 免费 + 云端同步付费 |
| **HTTPie** | 命令行 + GUI 双模式 | REST, GraphQL | 开源免费 |
| **Bruno** | 本地优先、Git 原生、无需云账号 | REST | 开源免费 |
| **Swagger UI** | OpenAPI 标准、文档优先 | REST | 开源免费 |
| **Hoppscotch** | 开源、浏览器端、轻量快捷 | REST, GraphQL, WebSocket | 开源免费 |

---

## 20. 总结

- **核心工作流：** Collection 管理 API → Environment 切换环境 → Pre-request 动态准备 → Tests 自动验证 → Runner / Newman 批量执行
- **AI 时代：** Postbot 让测试脚本编写从手写 JavaScript 变为自然语言描述，大幅降低使用门槛
- **CI/CD 集成：** Newman CLI + 多格式 Reporters 让 API 测试无缝融入 GitHub Actions / Jenkins 等自动化流水线
- **Mock & 文档：** Mock Server 提前联调后端未完成的 API；自动文档发布提升团队协作效率
- **实时协议：** WebSocket / Socket.IO / GraphQL 的全方位支持，覆盖现代微服务架构的多样化通信方式
- **生态优势：** 2000+ 集成（Slack、Datadog、AWS、Azure 等）、200+ 语言代码生成、团队协作与权限管理

> 🎯 Postman 已从简单的 HTTP 调试工具演变为 API 全生命周期管理平台，2026 年的 AI 能力（Postbot）进一步将其推向了新的高度。无论是个人开发者还是企业团队，Postman 都是 API 开发与测试的首选工具。
