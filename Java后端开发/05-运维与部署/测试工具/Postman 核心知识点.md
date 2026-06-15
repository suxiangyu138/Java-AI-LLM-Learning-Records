# Postman 核心知识点

## 一、概述

Postman 是全球使用最广泛的 API 开发和测试工具，支持 REST、GraphQL、gRPC、WebSocket 等多种协议。2026 年，Postman 集成了 AI 能力（Postbot），可自动生成测试脚本和测试数据。

**核心定位：** API 全生命周期管理——从设计、调试、测试到文档发布的一站式工具。

**官网：** https://www.postman.com

## 二、核心功能

### 2.1 功能矩阵

| 功能 | 说明 |
|------|------|
| **请求构建** | 可视化构造 HTTP 请求，支持所有方法、Headers、Body、Auth 配置 |
| **集合（Collections）** | 按项目/模块组织 API 请求，支持文件夹层级 |
| **环境变量（Environments）** | 多环境切换（dev/test/prod），变量复用 |
| **测试脚本** | 基于 JavaScript + Chai 断言库，自动验证响应 |
| **Pre-request Script** | 请求前执行脚本，动态生成 Header/参数 |
| **自动化运行（Runner）** | 批量执行集合中的请求，生成测试报告 |
| **Mock Server** | 根据 API 定义创建 Mock 服务 |
| **API 文档** | 自动生成交互式 API 文档 |
| **Postbot (AI)** | 自然语言生成测试脚本、数据、解释响应 |

## 三、快速上手

### 3.1 环境变量

```
环境变量定义（Environment）：
  ├── base_url: http://localhost:8080（dev）
  ├── base_url: https://api-staging.example.com（staging）
  └── base_url: https://api.example.com（prod）

请求 URL 中使用：
  GET {{base_url}}/api/users/{{user_id}}
```

### 3.2 测试脚本（Tests 标签）

```javascript
// 基础断言
pm.test("状态码为 200", () => {
    pm.response.to.have.status(200);
});

pm.test("响应时间 < 500ms", () => {
    pm.expect(pm.response.responseTime).to.be.below(500);
});

// JSON 响应断言
const response = pm.response.json();
pm.test("包含用户列表", () => {
    pm.expect(response.data).to.be.an("array");
    pm.expect(response.data.length).to.be.greaterThan(0);
});

// 设置环境变量
pm.environment.set("auth_token", response.token);

// 复杂断言
pm.test("用户数据验证", () => {
    const user = response.data[0];
    pm.expect(user).to.have.property("id");
    pm.expect(user.email).to.match(/@example\.com$/);
    pm.expect(user.age).to.be.within(18, 100);
});
```

### 3.3 Pre-request Script

```javascript
// 自动获取 Token
const loginRequest = {
    url: pm.environment.get("base_url") + "/auth/login",
    method: "POST",
    header: { "Content-Type": "application/json" },
    body: {
        mode: "raw",
        raw: JSON.stringify({
            username: "admin",
            password: "admin123"
        })
    }
};

pm.sendRequest(loginRequest, (err, res) => {
    if (!err) {
        const token = res.json().accessToken;
        pm.environment.set("token", token);
    }
});
```

### 3.4 集合 Runner

```
Runner 配置：
  ├── 选择集合 + 环境
  ├── 设置迭代次数
  ├── 设置请求延迟（delay）
  └── 选择数据文件（CSV/JSON）→ 数据驱动测试
```

## 四、AI 增强（Postbot）

2026 年 Postman 内置 Postbot，支持：

| AI 功能 | 操作 |
|---------|------|
| **生成测试脚本** | "验证返回数据不为空，且包含 id 和 name 字段" → 自动生成 Tests 代码 |
| **生成测试数据** | 自动生成符合字段类型的 Mock 数据 |
| **解释响应** | 选中响应体 → AI 解读 JSON 结构 |
| **修复脚本** | 脚本报错 → AI 分析并给出修复建议 |

## 五、团队协作

### 5.1 Workspace

```
Personal Workspace（个人）
     ↓
Team Workspace（团队共享集合+环境）
     ↓
API Network（跨团队/对外发布）
```

### 5.2 版本控制

- 与 Git 集成，集合导出为 JSON 文件纳入版本管理
- Newman（CLI 工具）在 CI/CD 中运行 Postman 集合：

```bash
npm install -g newman
newman run collection.json -e environment.json --reporters cli,html
```

## 六、替代品

| 工具 | 特点 |
|------|------|
| **Apifox** | 国产 API 工具，集 Postman + Swagger + Mock 于一体 |
| **Insomnia** | 开源、轻量，支持 GraphQL |
| **HTTPie** | 命令行 + GUI，简洁美观 |
| **Swagger UI** | 由 OpenAPI 文档自动生成交互页面 |

## 七、总结

- **核心工作流**：集合管理 API → 环境变量切换 → 写测试脚本 → Runner 批量执行
- **AI 时代**：Postbot 让测试脚本编写从手写 JS 变为自然语言描述
- **CI/CD 集成**：Newman 命令行工具让 API 测试融入自动化流水线
- **国内替代**：Apifox 提供更适合国内团队的一体化方案
