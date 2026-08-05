# 02 - IntelliJ IDEA 高级技巧

> 🎯 IDEA 的基本使用人人都会——真正拉开效率差距的是多模块管理、数据库工具、HTTP Client、远程调试这些高级特性

---

## 目录

1. [多模块项目管理](#1-多模块项目管理)
2. [数据库工具](#2-数据库工具)
3. [HTTP Client](#3-http-client)
4. [远程开发与调试](#4-远程开发与调试)

---

## 1. 多模块项目管理

```text
IDEA 多模块项目结构：
parent-project/
├── pom.xml (parent)
├── common/
├── service-a/
├── service-b/
└── service-c/

技巧：
→ 右键 pom.xml → "Add as Maven Project" 导入
→ Maven 工具窗 → 切换"按模块分组"视图
→ Ctrl+Shift+Alt+U → 查看 Maven 依赖图
→ "Load/Unload Modules" → 暂时禁用不用的模块（省内存）
```

## 2. 数据库工具

```text
IDEA 内置数据库管理（替代 Navicat/DBeaver）：

→ View → Tool Windows → Database
→ 支持 MySQL/PostgreSQL/Redis/MongoDB/...
→ SQL 自动补全 + 语法高亮 + 格式化
→ 直接运行 SQL / 导出数据 / 图表展示
→ 从数据库生成 JPA Entity（JPA Buddy 插件）

快捷键：
  Ctrl+Enter → 运行选中的 SQL
  F4 → 从代码跳转到数据库表
  Ctrl+F12 → 查看表结构
```

## 3. HTTP Client

```text
IDEA 内置 HTTP Client（替代 Postman）：

→ 在项目中创建 .http 文件
→ 自动补全 URL/Headers/Body
→ 支持变量、环境切换、断言
→ 可版本管理（和代码一起走 Git）

示例 (.http 文件)：
### 获取用户列表
GET http://localhost:8080/api/users
Authorization: Bearer {{token}}

### 创建用户
POST http://localhost:8080/api/users
Content-Type: application/json

{
  "name": "Alice",
  "email": "alice@example.com"
}
```

## 4. 远程开发与调试

```bash
# 远程调试（连接远程 JVM）
# 在远程服务启动参数加：
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005

# IDEA: Run → Edit Configurations → Remote JVM Debug → 填 IP:5005
```

```text
远程开发方案：
→ JetBrains Gateway: SSH 到远程服务器，本地 UI + 远程代码
→ VS Code Remote: SSH/DevContainer/Tunnel
→ GitHub Codespaces: 浏览器里完整 IDE

场景：服务器 GPU 环境调试 AI 代码 → Remote 开发是必备技能
```

## 核心要点回顾

- 多模块：Load/Unload Modules 节省内存
- 数据库：IDEA 内置工具替代 Navicat（SQL 补全 + JPA 生成）
- HTTP Client：替代 Postman，和代码一起版本管理
- 远程调试：`-agentlib:jdwp` + Remote JVM Debug

## 参考资料

1. IntelliJ IDEA 官方文档 — jetbrains.com/idea
