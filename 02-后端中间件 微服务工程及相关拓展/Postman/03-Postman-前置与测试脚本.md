# Postman 前置与测试脚本

> 📝 Pre-request Script 动态构建请求 + Tests 脚本断言响应 —— `pm.*` API 全家桶、Chai 断言库、常用脚本模板

---

## 📚 目录

1. [脚本执行时机](#1-脚本执行时机)
2. [pm 对象 API 详解](#2-pm-对象-api-详解)
3. [断言框架 Chai](#3-断言框架-chai)
4. [常用测试脚本模板](#4-常用测试脚本模板)
5. [高级脚本技巧](#5-高级脚本技巧)

---

## 1. 脚本执行时机

```text
┌────────────────┐
│  收到请求触发   │
│  Pre-request    │  ← 发请求前执行
│  Script         │     → 设置动态参数
│                 │     → 生成签名
│                 │     → 获取时间戳
└────────┬───────┘
         ▼
┌────────────────┐
│  发送 HTTP 请求 │
└────────┬───────┘
         ▼
┌────────────────┐
│  收到响应触发   │
│  Tests 脚本     │  ← 收响应后执行
│                 │     → 断言状态码
│                 │     → 断言响应体
│                 │     → 提取变量传递给下一请求
└────────────────┘
```

---

## 2. pm 对象 API 详解

### 2.1 pm.response —— 响应操作

```javascript
// 获取响应
let response = pm.response;
response.code;          // 200
response.status;        // "OK"
response.responseTime;  // 45 (ms)
response.responseSize;  // 1024 (bytes)

// JSON 响应
let json = pm.response.json();
json.data.id;           // 直接取值

// 文本响应
let text = pm.response.text();

// 响应头
pm.response.headers.get("Content-Type");
```

### 2.2 pm.expect —— 断言

```javascript
// 状态码断言
pm.test("状态码为 200", () => {
    pm.response.to.have.status(200);
});

// 也可以直接用 pm.expect
pm.test("响应时间 < 200ms", () => {
    pm.expect(pm.response.responseTime).to.be.below(200);
});
```

### 2.3 pm.sendRequest —— 发子请求

```javascript
// 在脚本中发送额外请求（不依赖当前请求）
pm.sendRequest("https://api.example.com/config", (err, res) => {
    if (!err) {
        let config = res.json();
        pm.collectionVariables.set("featureFlag", config.featureFlag);
    }
});

// 异步版本（Pre-request 中推荐）
pm.sendRequest({
    url: pm.variables.get("authUrl") + "/token",
    method: 'POST',
    header: { 'Content-Type': 'application/json' },
    body: { mode: 'raw', raw: JSON.stringify({ client_id: "xxx" }) }
}, (err, res) => {
    if (!err) {
        pm.collectionVariables.set("dynamicToken", res.json().access_token);
    }
});
```

### 2.4 pm.environment / pm.variables —— 变量操作

```javascript
// 变量操作全家桶
pm.globals.set("key", "value");             // 设置全局
pm.globals.get("key");                      // 获取全局
pm.globals.unset("key");                    // 删除全局
pm.globals.clear();                         // 清空全局

pm.environment.set / get / unset;           // 环境变量
pm.collectionVariables.set / get / unset;   // 集合变量

pm.variables.get("key");  // 自动按优先级查找（推荐用于读）
pm.variables.set("key", "value");  // 设置 local（本次运行）

// 检查变量是否存在
pm.variables.has("baseUrl");
```

### 2.5 pm.cookies

```javascript
// Cookie 操作
let jar = pm.cookies.jar();
jar.set("http://example.com", "sessionId", "abc123");

// 获取 Cookie
let cookies = jar.getAll("http://example.com");
```

---

## 3. 断言框架 Chai

### 3.1 BDD 风格（推荐）

```javascript
// pm.expect 就是 Chai expect 的封装

// 相等判断
pm.expect(200).to.equal(200);
pm.expect("hello").to.not.equal("world");
pm.expect({a: 1}).to.eql({a: 1});     // 深度相等
pm.expect({a: 1}).to.deep.equal({a: 1});

// 类型判断
pm.expect("hello").to.be.a("string");
pm.expect([1,2,3]).to.be.an("array");
pm.expect(null).to.be.null;
pm.expect(undefined).to.be.undefined;

// 包含判断
pm.expect("hello world").to.include("hello");
pm.expect([1,2,3]).to.include(2);
pm.expect({name: "John", age: 25}).to.have.property("name");

// 长度判断
pm.expect("hello").to.have.lengthOf(5);
pm.expect([1,2,3]).to.have.lengthOf(3);

// 数值比较
pm.expect(100).to.be.above(50);
pm.expect(100).to.be.below(200);
pm.expect(100).to.be.within(50, 200);

// 正则匹配
pm.expect("hello@test.com").to.match(/^[\w.-]+@[\w.-]+\.\w+$/);
```

### 3.2 常用 JSON Schema 校验

```javascript
// 校验响应结构
let schema = {
    "type": "object",
    "required": ["code", "data", "message"],
    "properties": {
        "code": { "type": "integer" },
        "data": {
            "type": "object",
            "required": ["id", "name", "email"],
            "properties": {
                "id":   { "type": "integer" },
                "name": { "type": "string" },
                "email":{ "type": "string", "format": "email" }
            }
        },
        "message": { "type": "string" }
    }
};

pm.test("响应符合 Schema", () => {
    pm.response.to.have.jsonSchema(schema);
});

// 用 tv4 手动校验
let tv4 = require("tv4");
let result = tv4.validateResult(pm.response.json(), schema);
pm.expect(result.valid).to.be.true;
```

---

## 4. 常用测试脚本模板

### 4.1 通用状态码检查

```javascript
// 复制即用的状态码检查
pm.test("状态码为 200", () => {
    pm.response.to.have.status(200);
});
```

### 4.2 响应时间检查

```javascript
pm.test("响应时间小于 500ms", () => {
    pm.expect(pm.response.responseTime).to.be.below(500);
});
```

### 4.3 响应体字段检查

```javascript
let jsonData = pm.response.json();

pm.test("返回数据包含必要字段", () => {
    pm.expect(jsonData).to.have.property("code");
    pm.expect(jsonData).to.have.property("data");
    pm.expect(jsonData.code).to.equal(200);
});

pm.test("data 是数组且不为空", () => {
    pm.expect(jsonData.data).to.be.an("array");
    pm.expect(jsonData.data.length).to.be.above(0);
});

pm.test("每个元素包含 id 和 name", () => {
    jsonData.data.forEach(item => {
        pm.expect(item).to.have.property("id");
        pm.expect(item).to.have.property("name");
    });
});
```

### 4.4 提取变量 + 断言

```javascript
let response = pm.response.json();

pm.test("创建成功并获取 ID", () => {
    pm.expect(response.code).to.equal(201);
    pm.expect(response.data.id).to.be.a("number");
    pm.expect(response.data.id).to.be.above(0);

    // 保存给后续请求使用
    pm.collectionVariables.set("createdId", response.data.id);
});
```

---

## 5. 高级脚本技巧

### 5.1 条件判断

```javascript
// 根据环境跳过某些测试
if (pm.environment.get("env") === "production") {
    pm.test("生产环境：更新用户需要审核", () => {
        pm.expect(pm.response.json().status).to.equal("pending_review");
    });
} else {
    pm.test("测试环境：更新用户直接生效", () => {
        pm.expect(pm.response.json().status).to.equal("active");
    });
}
```

### 5.2 循环断言

```javascript
let response = pm.response.json();

pm.test("所有用户状态为 active", () => {
    let allActive = response.data.every(user => user.status === "active");
    pm.expect(allActive).to.be.true;
});

pm.test("id 不能重复", () => {
    let ids = response.data.map(u => u.id);
    let uniqueIds = [...new Set(ids)];
    pm.expect(ids.length).to.equal(uniqueIds.length);
});
```

### 5.3 将响应保存为 JSON 文件

```javascript
// 在 Tests 中，将响应保存为文件（Runner 模式下常用）
let response = pm.response.json();
pm.collectionVariables.set("savedData", JSON.stringify(response));
```

---

> 🎯 **核心要点**：`pm.test()` 做断言、`pm.expect()` 做校验、`pm.variables.set()` 做链式传参——三个 API 组合起来实现完整的自动化测试。

---

*创建于：2026年7月*
