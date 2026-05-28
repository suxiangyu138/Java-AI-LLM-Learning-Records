# Claude Code 安装与配置详解（实战版）

> **文档定位**：Claude Code CLI 安装配置速查
> **版本**：Claude Code 2026.x
> **环境**：macOS / Linux / Windows（WSL）

---

## 一、安装

### 1.1 macOS / Linux

```bash
# 方式一：npm 全局安装（推荐）
npm install -g @anthropic-ai/claude-code

# 方式二：直接运行（无需安装）
npx @anthropic-ai/claude-code

# 验证安装
claude --version
```

### 1.2 Windows

```powershell
# 需要 Node.js 18+ 环境
npm install -g @anthropic-ai/claude-code

# 通过 WSL（推荐）
# 在 WSL2 Ubuntu 中执行 Linux 安装命令
```

### 1.3 认证

```bash
# 首次运行会引导 OAuth 登录
claude

# 或使用 API Key
export ANTHROPIC_API_KEY="sk-ant-xxx"
claude
```

---

## 二、配置文件体系

```
项目根目录/
│
├── CLAUDE.md                    # 项目级 Agent 指令（自动加载）
├── .claude/                     # 项目级配置目录
│   ├── settings.json            # 项目级设置
│   ├── settings.local.json     # 本地覆盖设置（不提交 Git）
│   ├── memory/                  # 项目记忆文件
│   ├── hooks/                   # 项目级 Hook 脚本
│   └── worktrees/              # 临时工作树
│
用户目录 ~/
├── .claude.json                 # 用户级全局设置（已废弃）
├── .claude/                     # 用户级配置
│   ├── settings.json            # 全局设置
│   ├── credentials.json         # 认证凭据
│   ├── history.jsonl            # 对话历史
│   └── keybindings.json         # 自定义快捷键
```

---

## 三、settings.json 详解

### 3.1 常用配置项

```json
{
  "model": "claude-sonnet-4-6",
  "theme": "dark",
  "permissions": {
    "allow": [
      "Bash(npm test)",
      "Bash(git diff)",
      "Bash(git status)",
      "Bash(git log *)",
      "Bash(ls *)",
      "Read(/src/**)",
      "Grep(*)",
      "Glob(*)"
    ],
    "deny": [
      "Bash(rm -rf *)",
      "Bash(git push --force *)",
      "Bash(git reset --hard *)",
      "Bash(curl *)"
    ]
  },
  "env": {
    "JAVA_HOME": "/usr/lib/jvm/java-17",
    "NODE_ENV": "development"
  },
  "hooks": {
    "preToolUse": [
      {
        "matcher": "Bash(git push*)",
        "command": "echo '即将推送代码到远程仓库'"
      }
    ],
    "postToolUse": [
      {
        "matcher": "Write*",
        "command": "echo '文件已写入'"
      }
    ]
  }
}
```

### 3.2 关键配置项说明

| 配置项 | 说明 |
|---|---|
| `model` | 默认模型（Opus/Sonnet/Haiku） |
| `theme` | 终端主题（dark/light） |
| `permissions.allow` | 自动允许的工具调用 |
| `permissions.deny` | 禁止的工具调用 |
| `env` | 注入到 Claude Code 运行环境的变量 |
| `hooks` | 事件钩子（pre/post tool use） |
| `statusLine` | 状态栏自定义 |

### 3.3 权限配置最佳实践

```json
{
  "permissions": {
    "allow": [
      "Read(*)",                    // 读取任意文件
      "Grep(*)",                    // 搜索任意内容
      "Glob(*)",                    // 文件名搜索
      "Bash(git *)",                // 所有 git 命令
      "Bash(ls *)",                 // 目录列表
      "Bash(cd *)",                 // 目录切换
      "Bash(npm *)",                // npm 操作
      "Bash(mvn *)",                // Maven 操作
      "Bash(node *)",               // Node.js 运行
      "Bash(python *)"              // Python 运行
    ],
    "deny": [
      "Bash(rm -rf *)",             // 禁止递归删除
      "Bash(> *)",                  // 禁止输出重定向覆盖
      "Bash(git push*)",            // 推送需手动确认
      "Bash(curl *)",               // curl 需手动确认
      "Bash(wget *)"                // wget 需手动确认
    ]
  }
}
```

---

## 四、CLAUDE.md —— 项目级指令

### 4.1 自动加载

CLAUDE.md 放在项目根目录，Claude Code 启动时**自动读取**并注入到 System Prompt：

```bash
# 快速初始化
claude --init
# 或手动创建
touch CLAUDE.md
```

### 4.2 内容模板

```markdown
# 项目：xxx 管理系统

## 技术栈
- 后端：Spring Boot 3.x + MyBatis Plus + MySQL 8.x + Redis 7.x
- 前端：Vue 3 + Element Plus
- 部署：Docker + Nginx

## 项目结构
- src/main/java/com/xxx/       Java 源码
- src/main/resources/          配置文件
- docs/                        文档

## 编码规范
- Java 使用阿里巴巴规范
- Controller → Service → Mapper 三层架构
- 统一返回 Result<T> 包装类
- SQL 字段名用下划线，Java 用驼峰

## 常用命令
- mvn spring-boot:run          启动服务
- mvn test                     运行测试
- npm run dev                  启动前端

## 注意事项
- 数据库连接串在 application-dev.yml 中
- Redis 端口 6379，密码在环境变量 REDIS_PASSWORD
- 不要提交 application-prod.yml 到 Git
```

---

## 五、多模型配置

```json
{
  "model": "claude-sonnet-4-6",
  "modelOverrides": {
    "fast": "claude-haiku-4-5",
    "agent": "claude-sonnet-4-6"
  }
}
```

| 模型 | 适用场景 |
|---|---|
| `claude-opus-4-7` | 复杂推理、架构设计 |
| `claude-sonnet-4-6` | 日常开发（默认） |
| `claude-haiku-4-5` | 快速简单任务 |

### 快速模式切换

```
/fast     → 切换到快速模式（Opus → 更快输出）
/config   → 手动选择模型
```

---

## 六、环境诊断

```bash
# 检查 Claude Code 环境
claude --doctor

# 常见问题：
# - Node.js 版本不够 → 升级到 18+
# - 网络问题 → 配置代理
# - 权限问题 → 检查文件权限
```

---

## 七、极简总结

```
安装 = npm install -g @anthropic-ai/claude-code
认证 = OAuth 登录 或 ANTHROPIC_API_KEY 环境变量
CLAUDE.md = 项目根目录，自动加载为 Agent 指令
settings.json = .claude/ 目录，权限/钩子/环境变量
权限 = allow（自动运行）+ deny（禁止）+ 默认（询问）
```
