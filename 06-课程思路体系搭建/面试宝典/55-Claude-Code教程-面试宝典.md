# Claude Code 面试宝典
> 基于课程大纲全面覆盖 Claude Code CLI 工具、权限系统、Hooks、MCP 集成、Agent 编排及工作流优化面试高频考点

## 目录
1. [一、基础概念速答](#一基础概念速答)
2. [二、深度原理剖析](#二深度原理剖析)
3. [三、实战场景题](#三实战场景题)
4. [四、配置与实现题](#四配置与实现题)
5. [五、系统设计题](#五系统设计题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（20题）

### 1. 什么是 Claude Code？
Claude Code 是 Anthropic 官方推出的 AI 编程 CLI 工具，深度集成到终端开发环境中，支持代码生成、重构、调试、批量处理等任务。它不是一个 IDE 插件，而是一个终端原生的 AI 编程助手。

### 2. Claude Code 与 Cursor、Copilot、Codex 的对比

| 对比维度 | Claude Code | Cursor | GitHub Copilot | OpenAI Codex |
|---------|-------------|--------|----------------|--------------|
| 运行形态 | 终端 CLI 工具 | VS Code 改造的 IDE | VS Code 插件 | API 模型 |
| 核心能力 | 自主 Agent 多步执行 | Tab 补全 + Composer + Agent | 代码补全 + Chat | 代码生成 API |
| 多文件操作 | 原生支持 | Composer 支持 | 单文件 | 需上层封装 |
| Agent 自主性 | 最高（自主规划执行） | 中等（IDE 内 Agent） | 最低（仅补全） | 无（纯模型） |
| 权限模型 | 三级审批（Allow/Always Allow/Deny） | 规则驱动（.cursorrules） | 无 | API Key |
| 协议扩展 | MCP 开放协议 | MDC 语法 | 无 | 无 |
| 多 Agent 协作 | 原生支持 Worktree | 不支持 | 不支持 | 不支持 |
| Headless 模式 | 支持 CI/CD 自动化 | 不支持 | 不支持 | 不支持 |
| 项目记忆 | CLAUDE.md + Memory System | .cursorrules | 无 | 无 |
| 模型 | Claude 系列 | Claude + GPT | OpenAI Codex | GPT 衍生 |
| 适用场景 | 复杂重构、CI/CD、批量任务 | 日常 IDE 编码 | 代码补全、简单问答 | API 集成开发 |

> 💡 **一句话区分**：Copilot 是补全工具，Cursor 是 AI IDE，Codex 是模型，Claude Code 是终端 AI Agent。

### 3. Claude Code 的三种模式是什么？
| 模式 | 启动方式 | 说明 | 适用场景 |
|------|----------|------|----------|
| Agent 模式 | `claude` | 全自主模式，Claude 可自主使用工具、修改代码、执行命令 | 日常开发、重构、多步骤任务 |
| Ask 模式 | `claude --ask` | 问答模式，仅回答不修改代码 | 技术咨询、代码解读、文档查询 |
| Manual 模式 | `claude --manual` | 手动确认模式，每次工具调用需要用户确认 | 高风险操作、学习阶段、精细控制 |

### 4. 什么是 CLAUDE.md 文件？
CLAUDE.md 是项目根目录下的 Markdown 文件，用于定义项目的上下文规则、编码规范、约束条件等。Claude Code 每次启动时自动读取此文件作为全局记忆。相当于项目的"宪法"，指导 Claude 的行为。

### 5. Claude Code 的权限系统是什么？
Claude Code 采用分级权限管理：
- **白名单**：自动允许的命令/工具
- **确认提醒**：每次执行需用户确认
- **拒绝**：禁止执行的命令/工具
- 通过 `settings.json` 或 `/config` 命令配置

### 6. 什么是 Hook（钩子）？
Hook 是 Claude Code 的生命周期回调，可在特定事件发生时自动执行自定义逻辑。支持：
- **PreToolUse**：工具调用前触发
- **PostToolUse**：工具调用后触发
- **Stop**：Claude 停止/退出时触发
- **SessionStart**：会话启动时触发

### 7. 什么是 Slash Command（斜杠命令）？
斜杠命令是以 `/` 开头的快捷指令，在 Claude Code 对话中触发预设操作。如 `/init` 初始化项目配置，`/review` 审查代码等。

### 8. 什么是 MCP（Model Context Protocol）？
MCP 是 Anthropic 推出的模型上下文协议，允许 Claude Code 连接外部工具和服务（如 Playwright、数据库、文件系统等）。MCP 服务器提供标准化的工具接口供 Claude 调用。

### 9. Claude Code 的 /init 命令是做什么的？
`/init` 命令用于在项目目录中初始化 Claude Code 配置，生成 `CLAUDE.md` 文件并引导用户设置项目上下文规则、构建命令、测试命令等。

### 10. 如何安装 Claude Code？
```bash
npm install -g @anthropic-ai/claude-code
# 或使用其他包管理器
# brew install claude-code  # macOS
```

### 11. 什么是 Multi-Claude 协作？
多个独立的 Claude Code 实例同时处理同一项目，每个实例负责不同模块，通过协议/消息传递协同工作。适用于大型项目的并行开发。

### 12. 什么是 Headless Mode（无头模式）？
Claude Code 的非交互模式，不启动交互式对话框，直接执行预定义的命令序列或工作流。适用于 CI/CD 流水线和自动化任务。

### 13. MCP Server 和 Plugin 有什么区别？
| 维度 | MCP Server | Plugin |
|------|------------|--------|
| 协议 | Model Context Protocol 标准 | 自定义接口 |
| 范围 | 跨工具共享 | Claude Code 专属 |
| 发现 | 动态发现工具列表 | 静态配置 |
| 典型 | Playwright、Filesystem、Git | 自定义 Hook、Skill |

### 14. Claude Code 支持哪些配置方式？
- **CLAUDE.md**：项目级上下文配置
- **settings.json**：全局/项目级设置（权限、模型、主题等）
- **.claude/settings.local.json**：本地私有配置
- **/config 命令**：交互式修改配置
- **环境变量**：如 `ANTHROPIC_API_KEY`

### 15. 什么是 Agent Orchestration（Agent 编排）？
Agent 编排是指将复杂任务拆解为多个子步骤，Claude Code 自主规划执行顺序、调用工具、汇总结果的过程。编排引擎负责任务分解、依赖管理和资源调度。

### 16. 什么是 Playwright MCP？
Playwright MCP 是一个 MCP 服务器，将 Playwright 浏览器自动化能力暴露给 Claude Code。Claude 可以控制浏览器执行导航、点击、截图、表单填写等操作。

### 17. 什么是 Batch Processing（批量处理）？
批量处理是指一次性对多个文件执行相同的操作，如批量重命名、批量格式转换、批量代码重构等。Claude Code 通过多文件编辑和并行处理机制实现。

### 18. Claude Code 成功率提升的 11 个技巧是什么？

1. **明确目标**：给出具体可量化的任务描述，避免模糊需求
2. **分解任务**：复杂任务拆分为多个子任务逐步执行
3. **提供上下文**：CLAUDE.md 配置项目规则和编码规范
4. **指定文件路径**：明确告知 Agent 要操作的文件
5. **先 Ask 再 Agent**：先咨询方案，确认后再动手执行
6. **定期 /clear**：每完成一个模块清理上下文，防止污染
7. **配置 MCP 工具**：接入项目特定工具，扩展 Agent 能力
8. **封装 Skills**：重复任务封装为 Skill 一键复用
9. **权限白名单**：Always Allow 常用命令，减少审批中断
10. **Headless 批处理**：批量任务用无头模式高效处理
11. **分段验证**：每步修改后运行测试，及早发现问题

> 💡 这 11 个技巧来自官方最佳实践，核心思路是"减少不确定性 + 提供高质量上下文"。

### 19. 如何自定义 Claude Code 配置？
通过 `settings.json` 文件：
- 全局：`~/.claude/settings.json`
- 项目：`.claude/settings.json`
- 本地覆盖：`.claude/settings.local.json`

### 20. Claude Code 的 Tool 系统如何工作？
Claude Code 内置一系列工具（Read、Edit、Write、Grep、Bash 等），通过工具调用机制执行操作。工具调用由权限系统控制，支持自动允许或手动确认。

## 二、深度原理剖析（10题）

### 2.1 Claude Code 的架构设计

```
用户终端 → CLI 界面 ←→ Session Manager ←→ LLM (Claude API)
                             ↕
                      Tool Execution Engine
                             ↕
                    ┌───────┼───────┐
                    ↓       ↓       ↓
                Read/Write  Bash   MCP Servers
                Edit/Grep   ...
```

**核心组件**：
- **CLI 层**：终端交互界面，支持流式输出和彩色渲染
- **Session Manager**：会话管理，维护上下文窗口和对话历史
- **Tool Execution Engine**：工具执行引擎，解析和调度工具调用
- **Permission System**：权限系统，控制工具执行权限
- **Hook System**：钩子系统，在工具生命周期中插入自定义逻辑

### 2.2 工具调用流程详解

```
用户输入 → LLM 分析 → 决定调用工具 → 权限检查
    ↓                                              ↓
    ↓ (通过/白名单) → 执行工具 → 结果返回 → LLM 继续
    ↓                                              ↓
    ↓ (需确认) → 用户确认 → 执行工具 → 结果返回 → LLM 继续
    ↓                                              ↓
    ↓ (拒绝) → 通知用户 → LLM 调整策略
```

### 2.3 Hook 系统执行原理

Hook 按照事件驱动的生命周期执行：

```javascript
// SessionStart Hook：会话启动时运行
// → 初始化环境，加载配置

// 循环：每个用户消息
//   → PreToolUse Hook（工具调用前）
//   → 执行工具
//   → PostToolUse Hook（工具调用后）
//   → 重复直到任务完成

// Stop Hook：会话结束时运行
// → 清理资源，输出摘要
```

> 💡 Hook 的执行顺序是同步的，PreToolUse 的返回值可以修改或阻止工具调用。

### 2.4 MCP 协议通信原理

```
Claude Code ↔ MCP Client ↔ MCP Server (通过 stdio/HTTP)
```

**通信流程**：
1. **初始化**：Client 发送 `initialize` 请求，交换协议版本和能力
2. **工具发现**：Client 调用 `tools/list` 获取服务器提供的工具列表
3. **工具调用**：Client 发送 `tools/call` 请求，指定工具名称和参数
4. **流式响应**：Server 返回结果，支持流式输出

**传输层**：
- `stdio`：子进程标准输入/输出（默认，本地）
- `HTTP+SSE`：远程 MCP 服务器

### 2.5 CLAUDE.md 上下文管理机制

```
CLAUDE.md → System Prompt Embedding → 每次请求拼接 → LLM 处理
```

CLAUDE.md 的内容会被注入到系统提示词中，作为项目的长期上下文。关键内容包括：
- 项目描述和架构
- 编码规范和约定
- 构建/测试命令
- 部署流程
- 关键约束和边界

> ⚠️ CLAUDE.md 不是动态更新的，需要手动或通过工具调用更新。

### 2.6 权限系统架构

```
请求 → Permission Evaluator → Rule Matcher → Decision (Allow/Deny/Prompt)
                ↑                              ↑
           settings.json                 用户输入/缓存
```

**规则优先级**（从高到低）：
1. 会话中用户临时授权
2. `.claude/settings.local.json`（本地覆盖）
3. `.claude/settings.json`（项目设置）
4. `~/.claude/settings.json`（全局设置）
5. 默认行为（确认提醒）

### 2.7 Slash Command 实现机制

Slash Command 本质上是预设的系统消息模版，输入 `/command` 时：
1. CLI 解析到 `/` 前缀
2. 匹配已注册的命令列表
3. 展开为对应的系统消息或指令序列
4. 发送给 LLM 执行

**内置命令**：`/init`、`/review`、`/clear`、`/help`、`/config`、`/cost` 等。

### 2.8 三种模式的执行差异

| 维度 | Agent 模式 | Ask 模式 | Manual 模式 |
|------|-----------|----------|-------------|
| 工具自动执行 | 是 | 否（仅回答） | 每次需确认 |
| 适用场景 | 独立开发、重构 | 技术咨询 | 首次使用、高风险操作 |
| 效率 | 最高 | 最低 | 中等 |
| 安全控制 | 依赖权限配置 | 严格 | 最严格 |
| 多步任务 | 自动规划执行 | 不适用 | 逐步确认 |

### 2.9 Agent Orchestration 编排原理

```
用户输入 → Task Decomposer → Task Graph (DAG)
                                    ↓
                          Scheduler → Worker Pool
                                    ↓
                          Result Aggregator
                                    ↓
                          Response Generator
```

**编排策略**：
- **顺序执行**：依赖关系的任务按序执行
- **并行执行**：无依赖的任务同时执行
- **动态规划**：根据中间结果调整后续计划
- **失败恢复**：子任务失败后重试或切换策略

### 2.10 Headless Mode 自动化机制

```
Headless Mode:
claude --print "run task" | headless runner | batch output

# 非交互式，所有 I/O 通过 stdio
# 适合 CI/CD 集成
# 支持 Pipe 和重定向
```

关键特点：
- 无终端交互
- 标准输入接收指令
- 标准输出返回结果
- 退出码表示执行状态

## 三、实战场景题（10题）

### 3.1 如何使用 Claude Code 重构一个大型项目？
1. **阅读 CLAUDE.md**：确保理解项目架构和规范
2. **分析代码**：使用 `Read` 和 `Grep` 工具了解当前实现
3. **制定计划**：先列出需要重构的模块和步骤
4. **分步执行**：从底层模块开始，逐层向上重构
5. **测试验证**：每个步骤后运行测试确保不破坏现有功能
6. **多文件编辑**：使用 `Edit` 工具批量修改相关文件
7. **代码审查**：使用 `/review` 命令或 code-review 进行最终审查

### 3.2 如何配置 MCP 服务器（如 Playwright）？

```yaml
# .claude/mcp.json
{
  "mcpServers": {
    "playwright": {
      "command": "npx",
      "args": ["@playwright/mcp"],
      "env": {
        "PLAYWRIGHT_BROWSER_PATH": "/usr/bin/chromium"
      }
    },
    "filesystem": {
      "command": "npx",
      "args": ["@anthropic/mcp-filesystem", "/allowed/path"]
    }
  }
}
```

> 💡 MCP 配置放在项目 `.claude/mcp.json` 或全局 `~/.claude/mcp.json`。

### 3.3 如何优化 Claude Code 的工作流程？
- **自定义 CLAUDE.md**：精确描述项目结构和编码规范
- **配置权限白名单**：减少不必要的确认提示
- **使用 Slash Command**：为常用操作创建快捷命令
- **设置 PreToolUse Hook**：自动执行代码格式化等前置操作
- **并行处理**：同时处理多个独立文件
- **会话管理**：复杂任务使用独立会话，避免上下文干扰

### 3.4 如何使用 Claude Code 修复 Bug？
1. **理解 Bug**：用自然语言描述现象
2. **定位问题**：Claude Code 搜索相关代码
3. **分析根因**：阅读关联文件，追踪调用链
4. **生成修复**：使用 `Edit` 工具修改代码
5. **验证修复**：运行测试或手动验证
6. **代码审查**：确保修复没有引入新问题

### 3.5 如何使用 Playwright MCP 增强测试？

```bash
# MCP 配置后，Claude 可执行浏览器操作
claude "打开应用首页，截图，检查登录按钮是否可见"
```

**典型场景**：
- 端到端测试自动化生成
- 页面截图比对
- 表单填写测试
- 用户操作流程录制

### 3.6 如何在会议中使用 Claude Code 实时编码？
- **快速原型**：描述需求，立即生成代码
- **代码审查**：团队展示代码，Claude 实时提供改进建议
- **问题排查**：遇到问题时直接提问，加速解决
- **文档生成**：为讨论内容即时生成技术文档

### 3.7 如何理解和改造开源项目？
1. **整体阅读**：Claude Code 分析项目结构和核心模块
2. **入口追踪**：从 main/入口文件开始追踪关键链路
3. **功能定位**：根据需求定位需要修改的模块
4. **修改验证**：小步修改，频繁测试
5. **贡献准备**：生成符合项目规范的 PR

### 3.8 如何实现多 Claude 协作处理大型项目？

```bash
# 终端1：处理前端模块
cd frontend && claude "实现用户登录页面..."

# 终端2：处理后端 API
cd backend && claude "实现登录 API 接口..."

# 终端3：协调和集成
claude "检查前端和后端的接口契约是否一致..."
```

### 3.9 如何用 Claude Code 做批量文件处理？
- **批量重命名**：`claude "将所有 .js 文件重命名为 .ts"`
- **批量添加 License**：`claude "为所有源文件添加 Apache License 头"`
- **批量格式修复**：`claude "修复所有文件中错误的缩进"`
- **批量接口迁移**：`claude "将 axios 调用全部替换为 fetch"`

### 3.10 如何调试 Claude Code 自身的问题？
| 问题 | 诊断方法 | 解决方案 |
|------|----------|----------|
| 工具调用失败 | 查看错误输出 | 检查权限配置或工具参数 |
| 上下文超限 | 观察 Token 使用量 | 使用 `/clear` 或开始新会话 |
| 响应不符合预期 | 检查 CLAUDE.md 规则 | 精确定义规则描述 |
| 权限频繁提示 | 查看 settings.json | 添加白名单规则 |
| MCP 连接失败 | 检查 MCP 配置和日志 | 验证 MCP Server 是否正常运行 |

## 四、配置与实现题（8题）

### 4.1 Hook 配置示例：PreToolUse 和 PostToolUse

```json
{
  "hooks": {
    "PreToolUse": {
      "scope": "project",
      "script": "scripts/pre-tool-check.sh",
      "description": "在工具调用前检查工作目录是否干净",
      "env": {
        "ALLOWED_PATHS": "./src,./tests"
      }
    },
    "PostToolUse": {
      "scope": "global",
      "script": "scripts/post-tool-format.sh",
      "description": "在文件编辑后自动格式化代码",
      "condition": {
        "tool": "Edit",
        "match": "*.{js,ts,jsx,tsx}"
      }
    },
    "SessionStart": {
      "scope": "project",
      "script": "scripts/session-init.sh",
      "description": "会话启动时检查环境依赖"
    },
    "Stop": {
      "scope": "global",
      "script": "scripts/session-summary.sh",
      "description": "会话结束时输出操作摘要"
    }
  }
}
```

### 4.2 Slash Command 自定义配置

```json
{
  "slash_commands": {
    "test": {
      "description": "运行当前项目的测试套件",
      "command": "运行测试：先检查测试框架，然后执行测试命令，最后汇总测试结果"
    },
    "deploy": {
      "description": "部署到测试环境",
      "command": "执行部署流程：1. 运行完整测试套件 2. 构建项目 3. 部署到测试服务器 4. 验证部署状态"
    },
    "clean": {
      "description": "清理生成的文件和缓存",
      "command": "清理项目：1. 删除 dist 目录 2. 删除 node_modules/.cache 3. 运行 npm cache clean"
    },
    "review": {
      "description": "审查当前修改的代码",
      "command": "使用 code-review 工具对当前 git diff 进行全面代码审查"
    }
  }
}
```

### 4.3 MCP Server 配置（JSON + YAML 双格式）

**JSON 格式**（`~/.claude/mcp.json`）：

```json
{
  "mcpServers": {
    "playwright": {
      "command": "npx",
      "args": ["@playwright/mcp"],
      "env": {
        "PLAYWRIGHT_BROWSER_PATH": "/usr/bin/chromium",
        "PLAYWRIGHT_HEADLESS": "true"
      }
    },
    "sqlite": {
      "command": "uvx",
      "args": ["mcp-server-sqlite", "--db-path", "./data.db"]
    },
    "github": {
      "command": "node",
      "args": ["./mcp-servers/github-server.js"],
      "env": {
        "GITHUB_TOKEN": "${GITHUB_PERSONAL_TOKEN}"
      }
    }
  }
}
```

**YAML 格式**：

```yaml
mcpServers:
  playwright:
    command: npx
    args:
      - "@playwright/mcp"
    env:
      PLAYWRIGHT_BROWSER_PATH: "/usr/bin/chromium"
      PLAYWRIGHT_HEADLESS: "true"

  sqlite:
    command: uvx
    args:
      - mcp-server-sqlite
      - "--db-path"
      - "./data.db"

  github:
    command: node
    args:
      - "./mcp-servers/github-server.js"
    env:
      GITHUB_TOKEN: "${GITHUB_PERSONAL_TOKEN}"
```

### 4.4 权限配置 settings.json

```json
{
  "permissions": {
    "allow": [
      "npm install",
      "npm test",
      "npm run build",
      "git status",
      "git diff",
      "git log",
      "ls",
      "cat"
    ],
    "always_allow": [
      "Read",
      "Edit",
      "Write",
      "Glob",
      "Grep"
    ],
    "prompt": [
      "git add",
      "git commit",
      "git push",
      "rm -rf",
      "sudo *"
    ],
    "deny": [
      "curl * | sh",
      "eval $( *)"
    ]
  },
  "notifications": {
    "on_tool_use": false,
    "on_task_complete": true
  }
}
```

### 4.5 CLAUDE.md 项目配置示例

```markdown
# CLAUDE.md - 项目配置

## 项目概述
这是一个基于 Next.js 14 的电商平台前端项目。

## 技术栈
- Next.js 14 (App Router)
- TypeScript
- Tailwind CSS
- Prisma ORM
- PostgreSQL

## 编码规范
- 使用函数式组件和 Hooks，避免类组件
- 文件名使用 kebab-case（如 user-profile.tsx）
- 组件名使用 PascalCase
- 函数和变量使用 camelCase
- 常量使用 UPPER_SNAKE_CASE
- 所有公共 API 必须编写 JSDoc 注释

## 命令
- 构建：`npm run build`
- 开发：`npm run dev`
- 测试：`npm run test`
- Lint：`npm run lint`

## 测试规范
- 使用 Jest + React Testing Library
- 每个组件至少有一个渲染测试
- Mock 外部 API 调用

## 目录结构
- `src/app/` - Next.js App Router 页面
- `src/components/` - 共享组件
- `src/lib/` - 工具函数和库
- `src/server/` - 服务端逻辑
- `prisma/` - 数据库 Schema

## 关键约束
- 不要修改 `src/lib/api-client.ts`（由后端团队维护）
- 不要在客户端组件中直接调用 Prisma
- 环境变量通过 `.env.local` 管理
```

### 4.6 Headless Mode 自动化配置

```bash
# headless-pipeline.sh
# 无头模式 CI/CD 流水线

# 1. 运行代码审查
claude --print \
  "Review all staged changes for bugs and security issues" \
  --model claude-sonnet-4-20250514 \
  --output-format json > review-report.json

# 2. 自动修复 lint 问题
claude --print \
  "Fix all ESLint errors in the src/ directory" \
  --allowed-tools "Read,Edit,Write,Grep"

# 3. 生成变更日志
claude --print \
  "Generate a changelog based on git log since last tag" \
  --output-format text > CHANGELOG.md
```

### 4.7 多文件重构指令序列

```bash
# 批量重命名组件文件
claude "将 src/components 下所有以 .js 结尾的文件重命名为 .tsx，并添加 TypeScript 类型声明"

# 批量导入语句重构
claude "将 src/pages 下所有 import { X } from 'react-router-dom' 替换为 import { X } from 'next/navigation'"

# 批量样式迁移
claude "将 src/styles 下的所有 .css 文件中的样式迁移到 Tailwind CSS 类名"
```

### 4.8 会话管理和上下文优化配置

```json
{
  "session": {
    "max_turns": 100,
    "max_tokens": 128000,
    "auto_summarize": true,
    "summarize_threshold": 80000,
    "context_strategy": "sliding_window",
    "window_size": 40000
  },
  "hooks": {
    "PostToolUse": {
      "script": "scripts/log-usage.sh",
      "description": "记录每个工具的调用频率和 Token 消耗"
    }
  }
}
```

## 五、系统设计题（5题）

### 5.1 设计一个基于 Claude Code 的自动化代码审查系统

**需求**：在 CI/CD 流水线中集成 Claude Code，对每次 PR 进行自动化代码审查。

**系统架构**：

```
Git Push → Webhook → CI Server (GitHub Actions) → Checkout Code
                                                       ↓
                                            CLAUDE.md 加载项目规范
                                                       ↓
                                            Claude Code (Headless Mode)
                                                       ↓
                                            ┌── Static Analysis (ESLint/Prettier)
                                            ├── Code Review (安全/性能/规范)
                                            ├── Test Execution
                                            └── Report Generation
                                                       ↓
                                            PR Comment (行级评论)
                                                       ↓
                                            Status Check (Pass/Fail)
```

**配置示例**：
```yaml
# .github/workflows/claude-review.yml
name: Claude Code Review
on: [pull_request]
jobs:
  review:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Claude Code Review
        run: |
          claude --print \
            "Review the diff of this PR. Check for bugs, security issues,
             performance problems, and adherence to project conventions.
             Output findings as GitHub-flavored markdown." \
            --model claude-sonnet-4-20250514
```

### 5.2 设计一个多 Claude 协作的微服务开发流水线

**需求**：3 个 Claude Code 实例并行开发微服务，服务间通过 API 通信，最终集成测试。

```
        ┌─── Claude A (User Service) ───┐
        │  - 数据库设计                  │
        │  - API 实现                    │
        │  - 单元测试                    │
        └────────┬──────────────────────┘
                 │ REST/gRPC
        ┌────────▼──────────────────────┐
        │   Claude B (Order Service)     │
        │   - 依赖 User Service API     │
        │   - 业务逻辑实现               │
        │   - 集成测试                   │
        └────────┬──────────────────────┘
                 │ 消息队列
        ┌────────▼──────────────────────┐
        │   Claude C (Notification)      │
        │   - 消息订阅处理               │
        │   - 多渠道通知发送             │
        │   - 端到端测试                 │
        └───────────────────────────────┘
```

**协调机制**：
1. 先定义 API 契约（OpenAPI/Grpc proto）
2. Claude A 和 B 并行开发
3. Claude B 开发时读取 Claude A 的 API 文档
4. Claude C 依赖 A 和 B 的输出
5. 最后集成测试

### 5.3 设计一个 Claude Code 自动化工单处理系统

**需求**：当开发者创建工单（Issue）时，Claude Code 自动分析并生成修复代码。

```
创建 Issue → Webhook → Claude Code (Headless) → 分析 Issue
                                                       ↓
                                            ┌── 理解问题描述
                                            ├── 搜索相关代码
                                            ├── 定位根因
                                            └── 生成修复方案
                                                       ↓
                                            ┌── 生成 Patch
                                            ├── 创建 PR
                                            └── @提及开发者审查
```

**关键设计**：
- **Issue Parser**：从 Issue 文本中提取关键信息（错误栈、版本号、重现步骤）
- **Code Searcher**：使用 `Grep` 和 `Glob` 工具搜索相关代码
- **Patch Generator**：生成最小化的修复方案
- **PR Creator**：使用 Git 工具创建分支和提交

### 5.4 设计一个知识库驱动的开发辅助系统

**需求**：整合公司内部文档、代码库、API 规范到 MCP Server，Claude Code 在编码时自动检索。

```
                ┌──────────────────────┐
                │   MCP Gateway Server  │
                │   (统一入口/路由)      │
                └──┬───┬───┬───┬──────┘
                   │   │   │   │
                   ▼   ▼   ▼   ▼
              ┌──┐ ┌──┐ ┌──┐ ┌──┐
              │内部│ │API│ │代码│ │最佳│
              │WIKI│ │文档│ │示例│ │实践│
              └──┘ └──┘ └──┘ └──┘

Claude Code → MCP Client → MCP Gateway → 内部知识库
```

**MCP Server 设计**：
```json
{
  "mcpServers": {
    "knowledge-gateway": {
      "command": "node",
      "args": ["mcp-servers/knowledge-gateway.js"],
      "env": {
        "WIKI_API": "https://wiki.company.com/api",
        "API_DOCS_PATH": "./api-docs",
        "CODE_EXAMPLES_PATH": "./examples"
      }
    }
  }
}
```

### 5.5 设计一个多语言国际化（i18n）自动化方案

**需求**：使用 Claude Code 自动扫描代码中的硬编码字符串，提取、翻译并生成多语言资源文件。

```
源代码 → Claude Code 扫描 → 提取硬编码字符串 → 生成翻译文件
                                                       ↓
                                              LLM 翻译 (Claude API)
                                                       ↓
                                              生成各语言 JSON
                                                       ↓
                                              替换源代码 → 使用 i18n 函数
                                                       ↓
                                              验证 → 无遗漏字符串
```

**执行指令**：
```bash
claude "扫描 src/ 目录下所有 React 组件，找出所有中文硬编码字符串，
提取到 zh-CN.json 中，然后使用 Claude API 翻译生成 en-US.json 和 ja-JP.json，
最后将源代码中的字符串替换为 t('key') 函数调用"
```

## 六、常见坑点与最佳实践（表格：坑点|原因|解决方案）

### 6.1 常见坑点

| 坑点 | 原因 | 解决方案 |
|------|------|----------|
| CLAUDE.md 规则不生效 | 文件格式错误或路径不对 | 确保放在项目根目录，使用正确的 Markdown 格式 |
| Hook 脚本执行失败 | 脚本没有可执行权限或路径错误 | 检查脚本权限 `chmod +x`，使用绝对路径 |
| MCP 连接超时 | MCP Server 启动慢或网络问题 | 增加初始化超时时间，检查网络连通性 |
| 上下文窗口溢出 | 会话过长，Token 超限 | 适时使用 `/clear` 或开启自动摘要 |
| 工具调用权限频繁提示 | 权限白名单配置不全 | 在 settings.json 中添加 `always_allow` 规则 |
| 多文件编辑不一致 | 并行编辑时相互冲突 | 按依赖顺序执行编辑，避免同时修改同一文件 |
| LLM 输出截断 | Token 限制或输出长度不足 | 设置 `max_tokens` 参数，或分步骤处理 |
| Headless 模式无法交互 | 没有传递 `--print` 参数 | Headless 模式必须使用 `--print` 指定指令 |
| Slash Command 未注册 | 配置格式错误或文件未加载 | 检查 settings.json 中的 slash_commands 格式 |
| Playwright MCP 浏览器无法启动 | 缺少浏览器依赖 | 安装 Chromium：`npx playwright install chromium` |
| 权限配置被忽略 | 配置文件层级优先级问题 | 检查 local.json 是否覆盖了项目配置 |
| Agent 模式下误操作 | 自动执行导致意外修改 | 使用 Manual 模式或限制工具权限 |

### 6.2 安全风险与代码审查挑战

| 风险/挑战 | 具体表现 | 防范措施 |
|-----------|----------|----------|
| Prompt 注入攻击 | 恶意用户输入被 Agent 解释为指令 | PreToolUse Hook 输入过滤；参数化命令 |
| 命令注入 | Agent 执行拼接了用户输入的 shell 命令 | 使用参数化命令；限制 Bash 工具参数 |
| 路径遍历 | Agent 读取/写入项目外的敏感文件 | 文件访问白名单；MCP fs 限制路径 |
| 敏感信息泄露 | API Key、密码被写入代码或日志 | 环境变量管理；PreToolUse 检测敏感词 |
| 供应链攻击 | Agent 自动安装含恶意代码的依赖 | 依赖审查；锁定版本号 |
| 代码审查新挑战 | AI 生成代码量激增，审查工作量翻倍 | 自动化预审 + 人工重点抽检 |
| 审查者偏差 | 开发者容易信任 Agent 生成的代码 | 建立"所有 Agent 代码必须审查"制度 |
| 版权合规 | Agent 生成代码可能包含 GPL 等传染性协议代码 | 使用代码溯源工具；企业级许可管理 |

> ⚠️ **关键原则**：AI Agent 引入的不仅是效率提升，还有新型安全风险。代码审查流程必须适应"代码量暴增 + Agent 自主行为"的新常态。建议建立 AI 生成代码的专项审查 checklist。

### 6.3 最佳实践

> 💡 **CLAUDE.md 质量至上**：投入时间写好 CLAUDE.md，它是 Claude Code 行为的"宪法"。包含项目架构、编码规范、关键约束和常用命令，能显著提升代码生成质量。

> 💡 **合理使用三种模式**：调试/探索用 Ask 模式，常规开发用 Agent 模式，高风险操作（多文件删除、部署）用 Manual 模式。

> 💡 **MCP Server 的合理设计**：每个 MCP Server 职责单一，过多的 MCP Server 会增加上下文负担。只在需要时启用。

> 💡 **会话管理**：一个会话只做一个任务。复杂项目拆分为多个独立会话，避免上下文污染和 Token 浪费。

> 💡 **利用 Hook 自动化**：PostToolUse Hook 自动格式化代码，PreToolUse Hook 自动检查权限，减少手动操作。

> 💡 **权限白名单精细化**：将安全、常用的命令加入白名单，减少不必要的确认提示，提高效率。

> 🎯 **关键原则**：Claude Code 是协作工具，不是替代品。始终审查生成的代码，特别是安全敏感逻辑。

## 七、面试回答模板（Top 5）

### Q1: 请介绍 Claude Code 的三种模式及其适用场景
> Claude Code 有三种工作模式。Agent 模式是全自主模式，Claude 可以主动规划任务、调用工具、修改代码，适合日常编码开发和重构工作。Ask 模式是纯问答模式，不执行任何工具调用，适合技术咨询、概念解释和代码阅读。Manual 模式每次工具调用都需要用户确认，适合首次使用场景或高风险操作。实际工作中，我会根据任务类型灵活切换：探索型任务用 Ask，常规开发用 Agent，关键操作切 Manual。

### Q2: 什么是 MCP？Claude Code 如何利用 MCP 扩展能力？
> MCP 是 Model Context Protocol 的缩写，是 Anthropic 推出的模型上下文协议。它定义了 AI 模型与外部工具之间的标准化通信接口。Claude Code 通过 MCP 可以连接各种外部服务，比如 Playwright（浏览器自动化）、数据库、文件系统、GitHub 等。配置方式是在 `.claude/mcp.json` 中定义 MCP 服务器，指定命令行和参数。MCP 的优势在于标准化——不同工具遵循同一协议，Claude 可以动态发现工具能力，无需硬编码集成。

### Q3: 如何通过 Hook 系统自定义 Claude Code 的行为？
> Hook 系统是 Claude Code 的生命周期回调机制，支持四个事件：PreToolUse（工具调用前）、PostToolUse（工具调用后）、SessionStart（会话启动）、Stop（会话结束）。配置方式是项目 settings.json 中定义 hooks 字段，指定触发条件和执行脚本。举例：在 PreToolUse 中检查工作目录状态，确保不会误操作；在 PostToolUse 中自动格式化修改后的代码；在 SessionStart 中检查依赖是否安装。Hook 让 Claude Code 的行为符合团队规范，是提升自动化水平的关键。

### Q4: 如何设计一个高效的 CLAUDE.md 文件？
> CLAUDE.md 应该包含六个核心部分：第一，项目概述——项目做什么，技术栈是什么；第二，编码规范——命名约定、代码风格、最佳实践；第三，目录结构——关键文件和目录的作用；第四，常用命令——构建、测试、部署命令；第五，关键约束——不能修改的文件、安全规则；第六，文档标准——注释规范、Readme 要求。关键原则是：准确且简洁，不要写大段无关内容；定期更新，反映项目最新状态；团队统一维护，避免个人化偏好。

### Q5: Claude Code 在处理大型项目重构时有哪些策略？
> 大型项目重构的核心策略是分解和分步。第一步，通过 CLAUDE.md 和代码分析全面理解项目架构。第二步，将重构任务分解为多个独立的子任务，每个子任务在一个独立的会话中完成，避免上下文污染。第三步，从底层依赖开始重构，逐层向上，确保每一步都有测试覆盖。第四步，利用批量文件处理能力，对同类修改（如重命名、迁移 API）一次性处理。第五步，使用多 Claude 协作，不同模块并行重构，然后集成测试。整个过程要频繁提交和验证，避免大范围破坏。

## 八、快速查漏补缺 Checklist

- [ ] 理解 Claude Code 的三种模式（Agent/Ask/Manual）及适用场景
- [ ] 掌握 CLAUDE.md 的结构和最佳实践
- [ ] 熟悉 Hook 系统：PreToolUse / PostToolUse / Stop / SessionStart
- [ ] 掌握 MCP 协议原理和配置方式（JSON/YAML 格式）
- [ ] 理解权限系统的分层架构和配置规则
- [ ] 掌握 Slash Command 的自定义方法
- [ ] 理解 Headless Mode 的原理和自动化场景
- [ ] 掌握 /init 命令的使用
- [ ] 了解 Playwright MCP 的配置和使用场景
- [ ] 理解 Agent Orchestration 编排原理
- [ ] 掌握会话管理和上下文窗口优化策略
- [ ] 了解 Multi-Claude 协作模式和协调机制
- [ ] 理解工具调用的完整流程（用户输入→LLM→权限检查→执行→返回）
- [ ] 熟悉 Batch Processing 批量处理命令模式
- [ ] 掌握 settings.json 配置格式：权限/钩子/环境变量
- [ ] 了解 PostToolUse 中自动格式化代码的配置方式
- [ ] 理解 PreToolUse 中安全检查的实现原理
- [ ] 掌握 CI/CD 中集成 Claude Code 的方法
- [ ] 了解大规模项目重构的分步策略
- [ ] 记住三种模式的核心差异和执行特征

---

> 🎯 **面试核心提示**：Claude Code 的核心面试点围绕"如何让 AI 更安全、更高效地辅助开发"展开。重点在于：理解权限控制和 Hook 机制（安全维度），MCP 和 Agent 编排（能力扩展维度），会话管理和批量处理（效率维度）。展示你对这些机制的深入理解，而不仅仅是会用。
