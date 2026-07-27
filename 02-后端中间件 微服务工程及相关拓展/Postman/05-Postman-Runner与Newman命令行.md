# Postman Runner 与 Newman CLI

> 🚀 Collection Runner 批量执行 + Newman 命令行集成 CI/CD + 数据驱动测试 + HTML 报告 —— 自动化测试完整方案

---

## 📚 目录

1. [Collection Runner](#1-collection-runner)
2. [Newman CLI](#2-newman-cli)
3. [数据驱动测试](#3-数据驱动测试)
4. [CI/CD 集成](#4-cicd-集成)
5. [工作流编排](#5-工作流编排)

---

## 1. Collection Runner

### 1.1 打开方式

```text
三种打开方式：
  1. Collection 右键 → "Run collection"
  2. 集合详情页 → "Run" 按钮
  3. Postman Footer → "Runner" 按钮
```

### 1.2 配置参数

```text
Runner 面板配置：

  运行范围：
    ├── 整个 Collection
    ├── 某个 Folder
    └── 手动选择 N 个请求

  执行参数：
    Iterations：执行轮数（默认 1）
    Delay：请求间延迟（毫秒，避免打爆服务器）
    Data：选择数据文件（JSON/CSV）
    Save Responses：是否保存响应体

  高级设置：
    Persist Variables：持久化变量（Runner 修改的变量是否保留）
    Keep Variable Values：是否保留环境变量值
    Stop on Error：遇到第一个错误就停止
```

### 1.3 Runner 结果面板

```text
┌────────────────────────────────────────┐
│  Runner Results                         │
├────────────────────────────────────────┤
│  ✅ 18 passed  ❌ 2 failed  20 total    │
│  Duration: 12.5s  Avg: 625ms           │
├────────────────────────────────────────┤
│  GET  /users          200 OK  45ms ✅   │
│  POST /users          201 C   120ms ✅  │
│  GET  /users/1        200 OK  38ms ✅   │
│  PUT  /users/1        200 OK  89ms ✅   │
│  GET  /users/999      404 NF  25ms ✅   │ ← 预期 404
│  DELETE /users/1      204 NC  52ms ✅   │
│  POST /users (dupl)   409 CF  30ms ❌   │ ← 断言失败！
│         Expected 201 but got 409         │
└────────────────────────────────────────┘
```

---

## 2. Newman CLI

### 2.1 安装

```bash
# 全局安装
npm install -g newman

# 验证
newman --version
```

### 2.2 基础用法

```bash
# 基本运行
newman run collection.json

# 指定环境
newman run collection.json -e environment.json

# 指定轮数
newman run collection.json -n 5

# 数据驱动
newman run collection.json -d data.csv

# 生成 HTML 报告
newman run collection.json -r html --reporter-html-export report.html

# 生成多种报告
newman run collection.json \
  -r cli,htmlextra,junit \
  --reporter-htmlextra-export report.html \
  --reporter-junit-export report.xml

# 超时和重试
newman run collection.json \
  --timeout-request 10000 \
  --delay-request 100
```

### 2.3 常用参数速查

| 参数 | 说明 | 示例 |
|------|------|------|
| `-e` | 环境文件 | `-e prod.json` |
| `-g` | 全局变量文件 | `-g globals.json` |
| `-d` | 数据文件 | `-d data.csv` |
| `-n` | 迭代次数 | `-n 10` |
| `--delay-request` | 请求间延迟(ms) | `--delay-request 200` |
| `--timeout-request` | 请求超时(ms) | `--timeout-request 5000` |
| `-r` | 报告类型 | `-r cli,htmlextra,junit` |
| `--bail` | 遇错停止 | `--bail` |
| `--env-var` | 覆盖变量 | `--env-var "baseUrl=https://new.example.com"` |

### 2.4 HTML 报告增强

```bash
# 安装 htmlextra 报告器（比默认 HTML 更好看）
npm install -g newman-reporter-htmlextra

# 生成增强版报告
newman run collection.json \
  -r htmlextra \
  --reporter-htmlextra-title "API 自动化测试报告" \
  --reporter-htmlextra-browserTitle "测试报告" \
  --reporter-htmlextra-showOnlyFails
```

---

## 3. 数据驱动测试

### 3.1 数据文件示例

```csv
// users.csv
scenario,username,email,expectedStatus,expectedMessage
正常创建,zhangsan,zhangsan@test.com,201,创建成功
重复创建,zhangsan,zhangsan@test.com,409,用户名已存在
空用户名,,blank@test.com,400,用户名不能为空
无效邮箱,wangwu,not-an-email,400,邮箱格式不正确
```

```javascript
// 在 Tests 中读取数据行的信息
pm.test(`[${pm.iterationData.get("scenario")}] 状态码应为 ${pm.iterationData.get("expectedStatus")}`, () => {
    pm.response.to.have.status(parseInt(pm.iterationData.get("expectedStatus")));
    pm.expect(pm.response.json().message).to.include(pm.iterationData.get("expectedMessage"));
});
```

```bash
# Newman 执行
newman run api-tests.json -d users.csv -n 4
```

---

## 4. CI/CD 集成

### 4.1 Jenkins Pipeline

```groovy
// Jenkinsfile
stage('API Test') {
    steps {
        sh '''
            newman run api-tests.json \
              -e environment.json \
              -r cli,junit,htmlextra \
              --reporter-junit-export newman-report.xml \
              --reporter-htmlextra-export newman-report.html \
              --bail
        '''
    }
    post {
        always {
            junit 'newman-report.xml'
            publishHTML(target: [
                reportName: 'API Test Report',
                reportDir: '.',
                reportFiles: 'newman-report.html'
            ])
        }
    }
}
```

### 4.2 GitHub Actions

```yaml
# .github/workflows/api-test.yml
name: API Tests

on:
  push:
    branches: [main, develop]
  pull_request:

jobs:
  api-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: '20'

      - name: Install Newman
        run: npm install -g newman newman-reporter-htmlextra

      - name: Run API Tests
        run: |
          newman run tests/collection.json \
            -e tests/staging.json \
            -r cli,htmlextra \
            --reporter-htmlextra-export api-report.html \
            --timeout-request 10000

      - name: Upload Report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: api-test-report
          path: api-report.html
```

---

## 5. 工作流编排

### 5.1 请求间数据流

```text
Collection Runner 中的执行顺序：

  1. POST /login
     └── Tests: 提取 accessToken → Collection Variable

  2. POST /users
     └── Header: Authorization: Bearer {{accessToken}}
     └── Tests: 提取 newUserId → Collection Variable

  3. GET /users/{{newUserId}}
     └── Header: Authorization: Bearer {{accessToken}}

  4. PUT /users/{{newUserId}}
     └── Body: { "name": "updated name" }

  5. DELETE /users/{{newUserId}}
     └── Tests: 清理 Collection Variable

通过 Collection Variables 实现请求间"管道"传递数据！
```

### 5.2 条件执行

```javascript
// 在 Pre-request Script 中跳过某些请求
if (pm.info.requestName === "DELETE /admin/users" &&
    pm.environment.get("env") === "production") {
    // 生产环境不允许删除管理员 → 跳过
    pm.execution.skipRequest();
}

// 根据上一个请求结果决定是否执行
if (pm.collectionVariables.get("needUpdate") !== "true") {
    pm.execution.skipRequest();
}
```

```javascript
// 在 Tests 中根据结果设置下一个请求
if (pm.response.code === 201) {
    postman.setNextRequest("GET /users/{{newUserId}}");  // 跳转到查询
} else {
    postman.setNextRequest("POST /login");  // 失败则重新登录
}
// 仅 Runner/Newman 模式下有效！
```

---

> 🎯 **核心要点**：Runner 做本地批量验证，Newman 做 CI/CD 集成。Collection Variables 做请求间管道传参，CSV/JSON 做数据驱动。`postman.setNextRequest()` 实现条件跳转和循环。

---

*创建于：2026年7月*
