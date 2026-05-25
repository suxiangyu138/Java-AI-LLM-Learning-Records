Claude Code是Anthropic推出的AI原生终端编程工具,能通过自然语言协作完成代码开发、重构、调试等复杂任务。以下是快速上手指南: [code.claude](https://code.claude.com/docs/zh-CN/quickstart)

## 安装配置

**Windows系统安装方式:**
```bash
# PowerShell执行
irm https://claude.ai/install.ps1 | iex

# CMD执行
curl -fsSL https://claude.ai/install.cmd -o install.cmd && install.cmd && del install.cmd
```

安装后首次启动需要登录Claude Pro/Max账号或API密钥。 [code.claude](https://code.claude.com/docs/zh-CN/quickstart)

## 核心操作技巧

### 基础交互

- `@文件路径` - 精准引用文件,如`@src/app.tsx 解释这个文件` [raymondhouch](https://raymondhouch.com/lifehacker/digital-workflow/claude-code-tutorial/)
- `!命令` - 直接执行终端命令,如`!npm test` [raymondhouch](https://raymondhouch.com/lifehacker/digital-workflow/claude-code-tutorial/)
- 双击`Esc` - 回退上一轮对话(不撤销代码修改) [raymondhouch](https://raymondhouch.com/lifehacker/digital-workflow/claude-code-tutorial/)
- `Shift+Tab` - 切换自动接受模式,减少确认提示 [raymondhouch](https://raymondhouch.com/lifehacker/digital-workflow/claude-code-tutorial/)

### 关键命令

| 命令 | 作用 | 使用场景 |
|------|------|----------|
| `/init` | 自动生成CLAUDE.md项目配置 | 新项目初始化 [raymondhouch](https://raymondhouch.com/lifehacker/digital-workflow/claude-code-tutorial/) |
| `/plan` | 制定分步骤实施计划 | 复杂功能开发前 [raymondhouch](https://raymondhouch.com/lifehacker/digital-workflow/claude-code-tutorial/) |
| `/diff` | 查看当前未提交改动 | 代码审查/提交前 [raymondhouch](https://raymondhouch.com/lifehacker/digital-workflow/claude-code-tutorial/) |
| `/compact` | 压缩上下文节省Token | 长对话后优化 [raymondhouch](https://raymondhouch.com/lifehacker/digital-workflow/claude-code-tutorial/) |
| `/context` | 查看Token使用情况 | 成本控制 [raymondhouch](https://raymondhouch.com/lifehacker/digital-workflow/claude-code-tutorial/) |

## 实战工作流

### 完整开发流程

```bash
# 1. 进入项目目录启动
cd your-project
claude

# 2. 规划任务
/plan
添加用户认证功能

# 3. 开发实现
创建src/hooks/useAuth.ts实现认证逻辑

# 4. 生成测试
为useAuth Hook生成单元测试

# 5. 查看改动并提交
/diff
请基于当前diff生成Conventional Commit message
!git add -A
!git commit -m "feat(auth): add user authentication hook"
```

### Java后端场景示例

```bash
# 创建Spring Boot Controller
创建UserController.java,实现RESTful用户管理API,包含CRUD操作,使用@RestController注解

# 优化MyBatis查询
@src/mapper/UserMapper.xml 优化这个SQL查询的性能,添加索引建议

# 生成单元测试
为UserService生成JUnit 5测试,覆盖所有业务逻辑分支
```

## 优化配置

### 减少Token消耗

创建`.claudeignore`文件排除无关文件: [raymondhouch](https://raymondhouch.com/lifehacker/digital-workflow/claude-code-tutorial/)
```
node_modules/
target/
*.class
*.jar
logs/
.idea/
```

### 项目记忆配置

运行`/init`自动生成`CLAUDE.md`,手动完善技术栈/规范: [raymondhouch](https://raymondhouch.com/lifehacker/digital-workflow/claude-code-tutorial/)
```markdown
## 技术栈
- Java 17 + Spring Boot 3.2
- MyBatis Plus + MySQL 8
- Redis缓存 + RabbitMQ

## 代码规范
- Controller层统一返回Result<T>封装
- Service层事务注解@Transactional
- 日志使用SLF4J
```

## 注意事项

- 代码生成后需人工审查,配合Git版本控制随时回滚 [raymondhouch](https://raymondhouch.com/lifehacker/digital-workflow/claude-code-tutorial/)
- 敏感操作(删除文件/提交代码)会二次确认,可通过`.claude/settings.json`调整权限 [raymondhouch](https://raymondhouch.com/lifehacker/digital-workflow/claude-code-tutorial/)
- 长时间开发建议每5-6轮对话执行`/compact`压缩上下文 [raymondhouch](https://raymondhouch.com/lifehacker/digital-workflow/claude-code-tutorial/)
- Windows用户推荐安装Git for Windows获得完整Bash工具支持 [code.claude](https://code.claude.com/docs/zh-CN/quickstart)

