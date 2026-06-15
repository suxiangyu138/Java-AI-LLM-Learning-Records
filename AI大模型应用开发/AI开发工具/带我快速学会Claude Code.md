# 快速学会 Claude Code

> **核心摘要**：Claude Code 是 Anthropic 推出的 AI 原生终端编程工具，能通过自然语言协作完成代码开发、重构、调试等复杂任务。本文涵盖安装配置、核心操作、实战工作流。

> 前置阅读：[[Claude Code 核心概念与架构]]

---

## 一、安装配置

### Windows 系统安装

```powershell
# PowerShell 执行
irm https://claude.ai/install.ps1 | iex

# CMD 执行
curl -fsSL https://claude.ai/install.cmd -o install.cmd && install.cmd && del install.cmd
```

安装后首次启动需要登录 Claude Pro/Max 账号或配置 API 密钥。

## 二、核心操作技巧

### 2.1 基础交互

| 操作 | 说明 |
|------|------|
| `@文件路径` | 精准引用文件，如 `@src/app.tsx 解释这个文件` |
| `!命令` | 直接执行终端命令，如 `!npm test` |
| 双击 `Esc` | 回退上一轮对话（不撤销代码修改） |
| `Shift+Tab` | 切换自动接受模式，减少确认提示 |

### 2.2 关键命令

| 命令 | 作用 | 使用场景 |
|------|------|----------|
| `/init` | 自动生成 CLAUDE.md 项目配置 | 新项目初始化 |
| `/plan` | 制定分步骤实施计划 | 复杂功能开发前 |
| `/diff` | 查看当前未提交改动 | 代码审查/提交前 |
| `/compact` | 压缩上下文节省 Token | 长对话后优化 |
| `/context` | 查看 Token 使用情况 | 成本控制 |

## 三、实战工作流

### 3.1 完整开发流程

```bash
# 1. 进入项目目录启动
cd your-project
claude

# 2. 规划任务
/plan
添加用户认证功能

# 3. 开发实现
创建 src/hooks/useAuth.ts 实现认证逻辑

# 4. 生成测试
为 useAuth Hook 生成单元测试

# 5. 查看改动并提交
/diff
!git add -A
!git commit -m "feat(auth): add user authentication hook"
```

### 3.2 Java 后端场景示例

```bash
# 创建 Spring Boot Controller
创建 UserController.java, 实现 RESTful 用户管理 API, 包含 CRUD 操作

# 优化 MyBatis 查询
@src/mapper/UserMapper.xml 优化这个 SQL 查询的性能, 添加索引建议

# 生成单元测试
为 UserService 生成 JUnit 5 测试, 覆盖所有业务逻辑分支
```

## 四、优化配置

### 4.1 减少 Token 消耗

创建 `.claudeignore` 文件排除无关文件：

```
node_modules/
target/
*.class
*.jar
logs/
.idea/
```

### 4.2 项目记忆配置

运行 `/init` 自动生成 `CLAUDE.md`：

```markdown
## 技术栈
- Java 17 + Spring Boot 3.2
- MyBatis Plus + MySQL 8
- Redis 缓存 + RabbitMQ

## 代码规范
- Controller 层统一返回 Result<T> 封装
- Service 层事务注解 @Transactional
- 日志使用 SLF4J
```

## 核心要点回顾

- `@文件路径` 精准引用文件，`!命令` 直接执行终端命令
- `/init` 初始化项目配置，`/plan` 制定计划，`/compact` 压缩上下文
- .claudeignore 排除无关文件减少 Token 消耗
- 代码生成后需人工审查，配合 Git 版本控制随时回滚
- 长时间开发建议每 5-6 轮对话执行 `/compact`

## 参考资料

1. Anthropic 官方文档 - Claude Code Quickstart
2. Claude Code 文档 - Installation & Configuration
