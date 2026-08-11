# Postman Collection 与变量管理

> 📦 Collection 请求组织、Pre-request Script 动态变量、Environment 环境切换、Data File 数据驱动 —— 高效管理 API 测试的基石

---

## 📚 目录

1. [Collection 集合](#1-collection-集合)
2. [变量体系](#2-变量体系)
3. [环境 Environment](#3-环境-environment)
4. [数据驱动测试](#4-数据驱动测试)
5. [变量实战场景](#5-变量实战场景)

---

## 1. Collection 集合

### 1.1 什么是 Collection

```text
Collection = 一组 API 请求的逻辑容器

典型组织方式（按业务模块）：
  User Service
    ├── POST 用户注册
    ├── POST 用户登录
    ├── GET  获取用户信息
    ├── PUT  更新用户信息
    └── DELETE 注销用户

层次结构：
  Collection
    └── Folder（文件夹，可嵌套）
         └── Request（单个请求）
              └── Example（请求示例，含不同参数组合）
```

### 1.2 Collection 级别的设置

| 设置项 | 说明 | 用途 |
|--------|------|------|
| **Authorization** | 集合级认证 | 该集合下所有请求共用同一认证 |
| **Pre-request Script** | 集合级前置脚本 | 所有请求执行前先跑这段 |
| **Tests** | 集合级测试脚本 | 所有请求执行后先跑这段 |
| **Variables** | 集合级变量 | 集合内共享的变量 |

```text
继承顺序（优先级从低到高）：
  Global < Collection < Environment < Data File < Local/运行时

同一变量名，高优先级覆盖低优先级！
```

---

## 2. 变量体系

### 2.1 变量类型

| 变量类型 | 作用域 | 使用方式 | 典型场景 |
|---------|:----:|---------|---------|
| **Global** | 全局 | `{{varName}}` | base_url, api_key |
| **Collection** | 当前集合 | `{{varName}}` | 模块特有参数 |
| **Environment** | 当前环境 | `{{varName}}` | 随环境切换的值 |
| **Data** | 单次运行 | `{{varName}}` | CSV/JSON 导入的测试数据 |
| **Local** | 脚本内 | `pm.variables.set()` | 临时计算值 |

### 2.2 动态变量（Dynamic Variables）

```javascript
// 无需手动赋值的动态变量：
{{$guid}}           // UUID，如 "611c2b8b-c848-4e8b-8b2b-f7e0872e4f76"
{{$timestamp}}      // 当前 Unix 时间戳
{{$randomInt}}      // 0-1000 随机整数
{{$randomAlphaNumeric}}  // 随机字母数字
{{$randomPhoneNumber}}   // 随机手机号
{{$randomEmail}}          // 随机邮箱
{{$randomFirstName}}      // 随机英文名
{{$randomCity}}           // 随机城市名
{{$randomCountry}}        // 随机国家
{{$randomHexColor}}       // 随机十六进制颜色

// 用于给字段生成唯一值，避免"用户名已存在"这类问题
```

### 2.3 脚本中设置变量

```javascript
// Pre-request Script 或 Tests 中：
pm.globals.set("token", "Bearer xxx");          // 全局变量
pm.collectionVariables.set("userId", "12345");  // 集合变量
pm.environment.set("env", "production");        // 环境变量
pm.variables.set("temp", "临时值");              // 本次运行有效

// 获取变量
let baseUrl = pm.variables.get("baseUrl");
// pm.variables.get() 自动按优先级查找，推荐使用！
```

---

## 3. 环境 Environment

### 3.1 环境的作用

```text
同一套 API，切换环境即可指向不同的服务器：

环境: Development
  ├── baseUrl = http://localhost:8080
  ├── dbHost = localhost
  └── logLevel = DEBUG

环境: Staging
  ├── baseUrl = https://staging-api.example.com
  ├── dbHost = staging-db.internal
  └── logLevel = INFO

环境: Production
  ├── baseUrl = https://api.example.com
  ├── dbHost = prod-db.internal
  └── logLevel = WARN

→ 右上角下拉框一键切换环境，所有 {{变量}} 自动变化！
```

### 3.2 环境变量建议

```text
推荐在 Environment 中定义：
  ✅ baseUrl       → 不同环境的 API 地址
  ✅ apiKey        → 不同环境的 API Key
  ✅ username/password → 测试账号
  ✅ timeout       → 超时设置

不要放在 Environment 中：
  ❌ 生产环境密码 → 用 Secret 变量或外部密钥管理
  ❌ 频繁变化的值 → 用脚本动态生成
```

---

## 4. 数据驱动测试

### 4.1 数据文件格式

```json
// JSON 格式
[
    { "username": "alice", "email": "alice@test.com", "expectedCode": 201 },
    { "username": "bob",   "email": "bob@test.com",   "expectedCode": 201 },
    { "username": "",      "email": "",               "expectedCode": 400 }
]
```

```csv
// CSV 格式（更轻量）
username,email,expectedCode
alice,alice@test.com,201
bob,bob@test.com,201
,,400
```

### 4.2 数据驱动执行

```text
Runner 中使用数据文件：
  1. Collection Runner → Select File → 选择 JSON/CSV
  2. 迭代次数 = 数据行数
  3. 每次迭代读取一行数据
  4. 请求中通过 {{columnName}} 引用

Iteration 1: {{username}} = "alice", {{email}} = "alice@test.com"
Iteration 2: {{username}} = "bob",   {{email}} = "bob@test.com"
Iteration 3: {{username}} = "",      {{email}} = ""

// 测试脚本中访问当前行数据
let currentUser = pm.iterationData.get("username");
console.log(`测试第 ${pm.info.iteration}/${pm.info.iterationCount} 次：${currentUser}`);
```

---

## 5. 变量实战场景

### 5.1 Token 自动传递（链式请求）

```javascript
// ===== 登录请求的 Tests =====
let response = pm.response.json();
// 把 token 存为集合变量，后续请求自动使用
pm.collectionVariables.set("authToken", response.data.token);

// ===== 其他请求的 Headers 中 =====
// Authorization: Bearer {{authToken}}
// 无需手动复制粘贴！
```

### 5.2 提取响应 ID 传给下一个请求

```javascript
// ===== 创建用户请求的 Tests =====
let response = pm.response.json();
// 获取创建成功返回的 ID
pm.collectionVariables.set("newUserId", response.data.id);
pm.collectionVariables.set("newUserName", response.data.name);

// ===== 下一个请求：查询用户详情 =====
// GET /users/{{newUserId}}
// 自动使用上一个请求创建的用户 ID
```

### 5.3 动态签名生成

```javascript
// ===== Pre-request Script =====
// 某些 API 需要对参数进行签名（MD5 / HMAC）
let timestamp = Date.now().toString();
let appSecret = pm.variables.get("appSecret");

let params = {
    appId: pm.variables.get("appId"),
    timestamp: timestamp,
    nonce: Math.random().toString(36).substring(2)
};

// 排序参数、拼接、加密
let sortedKeys = Object.keys(params).sort();
let signStr = sortedKeys.map(k => `${k}=${params[k]}`).join('&') + appSecret;
let sign = CryptoJS.MD5(signStr).toString();

// 设置到变量（请求中使用 {{sign}}）
pm.variables.set("timestamp", timestamp);
pm.variables.set("sign", sign);
pm.variables.set("nonce", params.nonce);
```

---

> 🎯 **核心要点**：变量是 Postman 的灵魂——Environment 做环境隔离、Collection Variables 做链式传参、Dynamic Variables 做随机数据、Data File 做数据驱动。

---

*创建于：2026年7月*
