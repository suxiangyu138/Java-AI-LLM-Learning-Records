# Claude Code Skill & MCP 使用手册

> 生成时间：2026-05-13
> 适用环境：Windows 11 + Claude Code CLI
> 配置文件：`C:\Users\34966\.claude\settings.json`

---

## 一、内置 Skill（/ 命令）

Skill 是 Claude Code 内置的专项能力。直接在对话中输入 `/skill-name` 即可调用，无需额外安装。

### 1. `/init` — 项目文档初始化
- **用途**：自动扫描当前项目结构，生成 `CLAUDE.md` 代码库文档，方便后续对话理解项目上下文。
- **触发**：`/init`
- **适用场景**：新项目首次使用 Claude Code 时；项目结构发生重大变化后更新文档。
- **示例**：
  ```
  /init
  ```
  执行后会在项目根目录生成 `CLAUDE.md`，包含项目概述、目录结构、关键文件说明等。

### 2. `/review` — Pull Request 代码审查
- **用途**：对 GitHub PR 进行自动化代码审查，检查代码质量、潜在 Bug、风格问题。
- **触发**：`/review`
- **适用场景**：提交代码前自查；Review 同事的 PR。
- **依赖**：需在 Git 仓库内使用，且分支有对应的远程 PR。
- **示例**：
  ```
  /review
  ```

### 3. `/simplify` — 代码简化与重构
- **用途**：扫描最近修改的代码，检查可复用性、质量与效率问题，并自动修复。
- **触发**：`/simplify`
- **适用场景**：写完一段代码后优化；清理技术债。
- **示例**：
  ```
  /simplify
  ```

### 4. `/security-review` — 安全审查
- **用途**：对当前分支的待提交改动进行安全审查，识别注入、XSS、敏感信息泄露等风险。
- **触发**：`/security-review`
- **适用场景**：提交涉及用户输入、网络请求、认证逻辑的代码前必做。
- **示例**：
  ```
  /security-review
  ```

### 5. `/claude-api` — Claude API 应用开发
- **用途**：专门用于构建、调试和优化 Claude API / Anthropic SDK 应用，包含提示缓存、工具调用、模型迁移等最佳实践。
- **触发**：`/claude-api`
- **适用场景**：开发 AI 应用；将项目从旧版 Claude 模型迁移到新版（如 4.5 → 4.6 → 4.7）。
- **示例**：
  ```
  /claude-api
  帮我给这段代码加上 prompt caching
  ```

### 6. `/fewer-permission-prompts` — 减少权限提示
- **用途**：自动扫描对话记录中高频的只读 Bash/MCP 调用，生成权限白名单写入 `settings.json`，减少重复确认弹窗。
- **触发**：`/fewer-permission-prompts`
- **适用场景**：刚配置好 Claude Code，想减少日常使用中的权限打扰。
- **注意**：已在本配置中预置常用白名单（见第三节）。

### 7. `/loop` — 循环定时任务
- **用途**：让 Claude 按固定间隔重复执行某个任务，如监控构建状态、轮询 PR 检查等。
- **触发**：`/loop 5m /task-name`
- **适用场景**：等待 CI 完成；定期检查日志；持续轮询某个状态。
- **示例**：
  ```
  /loop 5m /check-ci-status
  ```

### 8. `/update-config` — 配置管理
- **用途**：修改 `settings.json`、设置环境变量、调整权限、配置 Hook 自动化行为。
- **触发**：`/update-config`
- **适用场景**：需要调整权限白名单；设置 `"当 Claude 停止时显示 X"` 等自动化规则。
- **示例**：
  ```
  /update-config
  允许所有 npm install 命令
  ```

### 9. `/keybindings-help` — 快捷键配置
- **用途**：查看和修改 `~/.claude/keybindings.json`，自定义键盘快捷键、和弦绑定等。
- **触发**：`/keybindings-help`
- **适用场景**：习惯 Vim/Emacs 按键；想把 `Ctrl+S` 改成提交键。

---

## 二、MCP Server（外部工具集成）

MCP（Model Context Protocol）是 Claude Code 与外部系统交互的桥梁。以下 Server 已通过 `npm install -g` 安装到本机，并在 `settings.json` 中完成配置。

### 1. `filesystem` — 文件系统增强
- **用途**：比原生 Bash 更结构化的文件读写、目录遍历、文件搜索。
- **包名**：`@modelcontextprotocol/server-filesystem`
- **当前配置**：
  ```json
  {
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "C:\\Users\\34966"]
    }
  }
  ```
- **可用范围**：`C:\Users\34966` 目录及其子目录。
- **使用方式**：Claude 会自动在需要时调用，无需手动触发。你可以在对话中直接说"列出桌面所有文件"或"读取某个配置文件"。

### 2. `chrome-devtools` — 浏览器开发者工具
- **用途**：网页调试、性能分析、DOM 检查、网络请求抓取。
- **包名**：`chrome-devtools-mcp`
- **当前配置**：
  ```json
  {
    "chrome-devtools": {
      "command": "npx",
      "args": ["-y", "chrome-devtools-mcp"]
    }
  }
  ```
- **使用场景**：前端开发调试；抓取网页内容分析；检查页面性能瓶颈。
- **使用方式**：在对话中描述你要调试的网页或需要抓取的内容。

### 3. `context7` — 技术文档检索
- **用途**：接入 Context7 知识库，检索最新技术文档、API 参考、开源项目 README。
- **包名**：`@upstash/context7-mcp`
- **当前配置**：
  ```json
  {
    "context7": {
      "command": "npx",
      "args": ["-y", "@upstash/context7-mcp"]
    }
  }
  ```
- **使用场景**：查最新框架文档（如 React、Vue、Next.js 等）；获取准确的 API 签名；避免模型幻觉导致的错误 API 用法。
- **使用方式**：直接问"查一下 React 19 的 use Hook 用法"或"Context7 帮我找某库的文档"。

### 4. `github` — GitHub 操作
- **用途**：在对话中直接查询仓库、提交 Issue、查看 PR、浏览代码、管理分支。
- **包名**：`github-mcp-custom`
- **当前配置**：
  ```json
  {
    "github": {
      "command": "npx",
      "args": ["-y", "github-mcp-custom"],
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": ""
      }
    }
  }
  ```
- **⚠️ 注意**：`GITHUB_PERSONAL_ACCESS_TOKEN` 目前为空，**必须配置后才能使用**。
- **配置步骤**：
  1. 打开 GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
  2. 点击 **Generate new token (classic)**
  3. 至少勾选以下权限：
     - `repo`（完整仓库访问）
     - `read:user`（读取用户信息）
  4. 复制生成的 Token
  5. 打开 `C:\Users\34966\.claude\settings.json`
  6. 将 `"GITHUB_PERSONAL_ACCESS_TOKEN": ""` 替换为 `"GITHUB_PERSONAL_ACCESS_TOKEN": "ghp_你的Token"`
  7. 保存文件，重启 Claude Code
- **使用场景**："帮我看看这个 PR 的改动"、"列出仓库最近的 Issue"、"提交一个新 Issue 描述某 Bug"。

---

## 三、权限白名单（已预置）

以下内容已写入 `settings.json` 的 `permissions.allow` 中，日常使用不会弹权限确认框：

| 类型 | 命令示例 |
|------|----------|
| Git 操作 | `git status`, `git log`, `git diff`, `git branch`, `git remote`, `git config` |
| 文件浏览 | `ls`, `dir`, `find`, `cat`, `type`, `head`, `tail` |
| 文本搜索 | `grep`, `rg` |
| 开发环境 | `node --version`, `python --version`, `pip list`, `go version`, `rustc --version`, `cargo --version`, `javac --version`, `java --version` |
| npm 查询 | `npm list`, `npm view` |

如需添加更多免确认命令，可运行 `/fewer-permission-prompts` 自动分析，或手动编辑 `settings.json`。

---

## 四、快速参考卡片

```
┌─────────────────────────────────────────────────────────────┐
│                      Claude Code 速查表                      │
├─────────────────────────────────────────────────────────────┤
│ Skill（直接输入）                                           │
│   /init           → 生成 CLAUDE.md                         │
│   /review         → 审查 PR                                │
│   /simplify       → 优化代码                               │
│   /security-review→ 安全检查                               │
│   /claude-api     → API 开发助手                           │
│   /loop 5m /cmd   → 定时循环任务                           │
│   /update-config  → 改配置                                 │
│   /keybindings-help→ 改快捷键                              │
├─────────────────────────────────────────────────────────────┤
│ MCP（自动调用）                                             │
│   filesystem      → 读写本地文件                           │
│   chrome-devtools → 浏览器调试                             │
│   context7        → 查技术文档                             │
│   github          → 操作 GitHub（需 Token）                │
└─────────────────────────────────────────────────────────────┘
```

---

## 五、故障排查

| 问题 | 解决方案 |
|------|----------|
| MCP 报错 "command not found" | 检查 Node.js 是否安装：`node --version`。如未安装，先装 Node.js。 |
| GitHub MCP 提示认证失败 | 检查 `settings.json` 中 `GITHUB_PERSONAL_ACCESS_TOKEN` 是否已填入有效 Token。 |
| filesystem 无法访问某目录 | 确认该目录在 `args` 的允许路径列表内，或添加新路径。 |
| 某命令仍弹权限提示 | 运行 `/fewer-permission-prompts` 自动添加，或手动编辑 `settings.json`。 |
| 修改 settings.json 后不生效 | 保存文件后，重启 Claude Code 会话。 |

---

*本文档由 Claude Code 自动生成，如需更新 Skill/MCP 配置，请修改 `C:\Users\34966\.claude\settings.json`。*
