# AI 终端工具全家桶 · 说明书

> **统一 API**：DeepSeek V4 Pro / Flash | **Key**：见环境变量 `DEEPSEEK_API_KEY`（勿写入文档） | **更新日期**：2026-06-24

---

## 环境依赖

| 依赖 | 版本 | 说明 |
|------|------|------|
| Node.js | v22.23.1 | Volta 管理 |
| Bun | 1.3.14 | — |
| uv | 0.11.23 | — |
| Git | 2.53.0 | — |
| Java | — | `D:\JDK25` |

---

## 环境变量

> **配置文件**：`Documents\WindowsPowerShell\Microsoft.PowerShell_profile.ps1`（PowerShell Profile 自动加载）

| 变量 | 值 | 用途 |
|------|-----|------|
| `DEEPSEEK_API_KEY` | `sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx` | 通用 |
| `ANTHROPIC_API_KEY` | `sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx` | Claude Code / Langcli |
| `ANTHROPIC_BASE_URL` | `https://api.deepseek.com/anthropic` | DeepSeek Anthropic 兼容端点 |

---

## 一、编程 Agent（终端内使用）

### 1. GitHub Copilot CLI

| 属性 | 值 |
|------|-----|
| 命令 | `copilot` |
| 模型 | `deepseek-v4-pro` |
| 协议 | Anthropic Messages API |
| 配置 | 环境变量（Profile 已配） |

**说明**：输入 `copilot` 进入交互，支持 Agent 模式、工具调用、MCP。

---

### 2. Kilo Code CLI

| 属性 | 值 |
|------|-----|
| 命令 | `kilo` |
| 模型 | `deepseek-v4-pro`（启动后 `/models` 切换） |
| 协议 | OpenAI Completions（`DEEPSEEK_API_KEY`） |
| 配置 | 环境变量自动检测 |

**说明**：`kilo` 进项目目录后启动，`/connect` 切换提供商。

---

### 3. CodeBuddy (WorkBuddy)

| 属性 | 值 |
|------|-----|
| 启动 | 桌面应用，无 CLI 命令 |
| 模型 | `deepseek-v4-pro`（模型选择器切换） |
| 协议 | OpenAI Completions |
| 配置 | `C:\Users\13686\.codebuddy\models.json` |

**说明**：腾讯云桌面应用，需从 [codebuddy.cn](https://codebuddy.cn) 下载。启动后模型选择器可见 DeepSeek V4 Pro / Flash。

---

### 4. Oh My Pi (OMP)

| 属性 | 值 |
|------|-----|
| 命令 | `omp --model deepseek/deepseek-v4-pro` |
| 模型 | `deepseek-v4-pro` / `deepseek-v4-flash` |
| 协议 | OpenAI Completions（`DEEPSEEK_API_KEY`） |
| 配置 | `C:\Users\13686\.omp\agent\models.yml` |

**说明**：`Ctrl+L` 或 `/model` 切换模型。需要 Bun 运行时（Profile 已加 PATH）。

---

### 5. OpenClaw

| 属性 | 值 |
|------|-----|
| 命令 | `openclaw dashboard` / `openclaw tui` / `openclaw terminal` |
| 模型 | `deepseek-v4-pro`（默认） |
| 协议 | OpenAI Completions |
| 配置 | `C:\Users\13686\.openclaw\openclaw.json` |

**说明**：已接入微信频道。支持飞书、QQ、Telegram 等。

```bash
openclaw dashboard    # Web UI (http://127.0.0.1:18789)
openclaw tui          # 终端 TUI
openclaw terminal     # 终端对话
```

---

### 6. AstrBot

| 属性 | 值 |
|------|-----|
| 命令 | `cd D:\Git\mnt\c\Users\13686\astrbot && astrbot run` |
| 模型 | `deepseek-v4-pro` |
| WebUI | `http://localhost:6185` |
| 协议 | OpenAI Completions |
| 配置 | `D:\Git\mnt\c\Users\13686\astrbot\data\cmd_config.json` |

**登录凭据**：

| 字段 | 值 |
|------|-----|
| 用户名 | `astrbot` |
| 密码 | `I7yqbxuQvTg73JA2osrZyvke` |

**说明**：已接入 QQ 官方机器人。支持微信、飞书、Telegram 等。WebUI 管理模型、人格、插件、知识库。

---

### 7. nanobot

| 属性 | 值 |
|------|-----|
| 命令 | `nanobot agent` |
| 模型 | `deepseek-v4-pro` |
| 协议 | OpenAI Completions |
| 配置 | `C:\Users\13686\.nanobot\config.json` |

**说明**：
- 单次对话：`nanobot agent -m "问题"`
- 支持 QQ / 微信 / 飞书 / Discord / Telegram 等频道
- 编辑 `config.json` 中 `channels` 对应平台即可启用

---

### 8. Crush

| 属性 | 值 |
|------|-----|
| 命令 | `cd my-project && crush` |
| 模型 | `deepseek-v4-pro` / `deepseek-v4-flash` |
| 协议 | OpenAI 兼容（`$DEEPSEEK_API_KEY` 环境变量引用） |
| 配置 | `C:\Users\13686\.config\crush\crush.json` |

**说明**：`Ctrl+L` 或 `/model` 切换供应商和模型。Charm 出品，支持 LSP 集成、MCP 服务器。

---

### 9. Pi

| 属性 | 值 |
|------|-----|
| 命令 | `cd my-project && pi` |
| 模型 | `deepseek-v4-pro` / `deepseek-v4-flash` |
| 协议 | OpenAI Completions |
| 配置 | `C:\Users\13686\.pi\agent\models.json` |

**说明**：`/model` 切换模型。

> ⚠️ **注意**：当前使用 `@mariozechner/pi-coding-agent` v0.73.1，官方已迁移到 `@earendil-works/pi-coding-agent`，后续可升级。

---

### 10. Reasonix

| 属性 | 值 |
|------|-----|
| 命令 | `npx reasonix code` |
| 模型 | 默认 `deepseek-v4-flash`，`/pro` 切 V4 Pro |
| 协议 | DeepSeek 原生 API |
| 配置 | `~\.reasonix\config.json`（首次启动向导生成） |

**说明**：DeepSeek 原生后端，无需全局安装，`npx` 即用。

| 命令 | 功能 |
|------|------|
| `/preset max` | 整个 session 走 Pro |
| `/pro` | 下一轮切 Pro |

---

### 11. Langcli

| 属性 | 值 |
|------|-----|
| 命令 | `langcli` |
| 模型 | `deepseek-v4-pro` |
| 协议 | Anthropic Messages API（DeepSeek 兼容端点） |
| 配置 | `C:\Users\13686\.claude\settings.json`（env 块） |

**说明**：Claude Code 封装，走 DeepSeek Anthropic 端点。需要 Git Bash（`CLAUDE_CODE_GIT_BASH_PATH` 已配）。

---

## 二、配置速查表

| 工具 | 配置文件路径 |
|------|-------------|
| Copilot CLI | 环境变量（Profile） |
| Kilo Code | 环境变量（`DEEPSEEK_API_KEY`） |
| CodeBuddy | `~\.codebuddy\models.json` |
| Oh My Pi | `~\.omp\agent\models.yml` |
| OpenClaw | `~\.openclaw\openclaw.json` |
| AstrBot | `D:\Git\mnt\c\Users\13686\astrbot\data\cmd_config.json` |
| nanobot | `~\.nanobot\config.json` |
| Crush | `~\.config\crush\crush.json` |
| Pi | `~\.pi\agent\models.json` |
| Reasonix | `~\.reasonix\config.json` |
| Langcli | `~\.claude\settings.json` |

---

## 三、常用操作

### 更改 API Key

1. 编辑 `Documents\WindowsPowerShell\Microsoft.PowerShell_profile.ps1`，修改 `DEEPSEEK_API_KEY` 和 `ANTHROPIC_API_KEY`
2. 编辑 `C:\Users\13686\.claude\settings.json`，修改 `env.ANTHROPIC_API_KEY`
3. 编辑 `C:\Users\13686\.nanobot\config.json`，修改 `providers.deepseek.apiKey`
4. 重启 PowerShell

### 切换模型

大部分工具默认 `deepseek-v4-pro`，需要更快响应时换 `deepseek-v4-flash`：

| 工具 | 切换方式 |
|------|----------|
| Copilot CLI | `$env:COPILOT_MODEL="deepseek-v4-flash"` |
| OMP | `omp --model deepseek/deepseek-v4-flash` |
| Langcli | `langcli --model deepseek-v4-flash` |
| Reasonix | 默认就是 Flash |
| 其他 | 启动后 `/model` 或模型选择器切换 |

### 查看模型列表

| 工具 | 方式 |
|------|------|
| Kilo Code | `kilo models deepseek` |
| OMP | 启动后 `/model` |
| Crush | 启动后 `Ctrl+L` |

---

## 四、终端对话工具对比

| 工具 | 类型 | 交互方式 | 特色 |
|------|------|----------|------|
| Langcli | Claude 封装 | 终端 TUI | Claude Code 体验 |
| Copilot CLI | 独立 CLI | 终端 TUI | Agent + MCP |
| Kilo Code | 独立 CLI | 终端 TUI | 多提供商 |
| OMP | 独立 CLI | 终端 TUI | 极简框架 |
| Pi | 独立 CLI | 终端 TUI | TS 扩展 |
| Crush | 独立 CLI | 终端 TUI | Charm 出品 |
| Reasonix | 独立 CLI | 终端 TUI | DeepSeek 原生 |
| nanobot | 独立 CLI | 终端 Agent | 聊天平台接入 |
| OpenClaw | 桌面服务 | WebUI / TUI | 多频道集成 |
| AstrBot | 桌面服务 | WebUI | 多平台 + 插件 |
| CodeBuddy | 桌面应用 | GUI | 腾讯云 |

---

## 五、故障排查

### Auth 冲突

> `ANTHROPIC_AUTH_TOKEN` vs `ANTHROPIC_API_KEY`

- **原因**：`~\.claude\settings.json` 和 Profile 同时设置了不同认证方式
- **解决**：确保 `settings.json` `env` 块使用 `ANTHROPIC_API_KEY`

### 404 错误

- **原因**：认证方式不对导致请求到了错误端点
- **解决**：确认 `ANTHROPIC_BASE_URL=https://api.deepseek.com/anthropic`

### DeepSeek thinking mode 下 tool call 报 400

- **原因**：DeepSeek V4 要求 `reasoning_content` 回传
- **解决**：OMP 已通过 `compat` 字段配置，其他工具各有内部处理

### OMP 报错

- **原因**：缺少 Bun 运行时
- **解决**：确认 `$env:USERPROFILE\.bun\bin` 在 PATH 中

### OpenClaw Gateway 启动失败

- **原因**：旧版服务路径失效
- **解决**：`openclaw daemon uninstall && openclaw daemon install`

### nanobot / pi 命令找不到

- **原因**：uv 安装的工具不在 PATH
- **解决**：确认 `$env:USERPROFILE\.local\bin` 在 PATH 中

---

> **更新日期**：2026-06-24
