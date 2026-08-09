# 06 接入 Claude Code 与客户端配置

> 把自研 Server 接进真实客户端：`claude mcp add` 命令、mcp.json 配置格式、/mcp 界面管理、项目级 vs 全局、连接失败排查——"自己写的工具被 AI 用起来"的最后一公里。

## 📚 目录

1. [Claude Code 的 MCP 支持](#1-claude-code-的-mcp-支持)
2. [方式一：claude mcp add 命令](#2-方式一claude-mcp-add-命令)
3. [方式二：mcp.json 配置文件](#3-方式二mcpjson-配置文件)
4. [验证与使用](#4-验证与使用)
5. [项目级 vs 全局配置](#5-项目级-vs-全局配置)
6. [连接失败排查](#6-连接失败排查)

## 1. Claude Code 的 MCP 支持

| 能力 | 说明 |
|---|---|
| 配置位置 | 全局 `~/.config/claude/mcp.json`；项目级 `.mcp.json` |
| 配置方式 | `claude mcp add` 命令 / `/mcp` 界面 / 手写 JSON |
| 传输支持 | stdio（本地工具）、Streamable HTTP（远程服务） |
| 工具暴露 | 配置后 Server 的工具自动进入会话，模型可按需调用 |
| 本仓库 | 本会话即在用 MCP——配置了多个插件服务器（stdio） |

**流程总览**：写 Server（02 篇）→ Inspector 验证（05 篇）→ 配置进 Claude Code（本篇）→ 对话中自然调用。

## 2. 方式一：claude mcp add 命令

```bash
# 1) 添加 stdio Server（在项目目录下 = 项目级）
claude mcp add demo-server -- uv run python server.py

# 2) 查看已配置的服务器
claude mcp list

# 3) 测试连接
claude mcp get demo-server

# 4) 删除
claude mcp remove demo-server
```

**常用选项**：

```bash
# 设置环境变量
claude mcp add demo-server -e API_KEY=xxx -- uv run python server.py

# 指定传输类型（远程 HTTP）
claude mcp add --transport http demo-server http://127.0.0.1:8080/mcp

# 全局安装（不带 --scope project）
claude mcp add --scope user demo-server -- uv run python server.py
```

> ⚠️ 在**项目目录内**运行 `claude mcp add` 默认是项目级（写入 `.mcp.json` 并通常建议提交到仓库，团队共享）；加 `--scope user` 才是全局。

## 3. 方式二：mcp.json 配置文件

**全局配置**（`~/.config/claude/mcp.json`，所有项目生效）：

```json
{
  "mcpServers": {
    "demo-server": {
      "command": "uv",
      "args": ["run", "python", "server.py"],
      "env": {
        "API_KEY": "your-key"
      }
    },
    "remote-server": {
      "type": "http",
      "url": "https://example.com/mcp",
      "headers": {
        "Authorization": "Bearer <token>"
      }
    }
  }
}
```

**项目级配置**（项目根目录 `.mcp.json`，随仓库分发）：

```json
{
  "mcpServers": {
    "demo-server": {
      "command": "uv",
      "args": ["run", "python", "server.py"]
    }
  }
}
```

**配置格式要点**：

| 字段 | 说明 | 易错点 |
|---|---|---|
| `command` | 可执行文件 | **必须绝对路径或 PATH 内命令**；`~` 不展开！ |
| `args` | 参数数组 | 不要拼成字符串 |
| `env` | 环境变量 | 敏感信息别提交进项目级配置 |
| `type` | 传输类型 | stdio 省略；http 写 `"type": "http"` + `url` |
| `headers` | HTTP 头 | 远程认证 Bearer Token 放这里 |

## 4. 验证与使用

```bash
# 检查 Server 是否正常连接
claude mcp get demo-server
# 输出示例：Connected to demo-server（含工具数量）

# 会话内查看/管理
# 输入 /mcp 打开管理界面
```

**在对话中使用**：

```text
你：帮我看一下当前上海时间
Claude：正在调用 get_current_time(timezone=Asia/Shanghai)...
      当前时间：2026-08-09T18:40:00+08:00
```

**使用前检查清单**：

- [ ] `claude mcp list` 能看到 demo-server
- [ ] `claude mcp get demo-server` 返回 Connected
- [ ] 对话中模型能列出并调用工具（无法调用时提示模型查看工具列表）
- [ ] 工具报错时，Server 的 stderr 日志可查（`claude mcp logs`）

## 5. 项目级 vs 全局配置

| 维度 | 项目级（.mcp.json） | 全局（~/.config/claude/mcp.json） |
|---|---|---|
| 生效范围 | 当前项目目录 | 所有项目 |
| 团队共享 | ✅（提交进仓库） | ❌ 仅本机 |
| 适用 | 项目专属工具（本项目的脚本、数据库等） | 通用工具（浏览器、搜索、系统命令） |
| 环境变量 | 注意勿提交密钥 | 个人密钥安全 |

**实践建议**：团队工具放项目级并提交；个人通用工具放全局；敏感密钥用 `claude mcp add -e` 或环境变量引用，不写进 JSON。

## 6. 连接失败排查

| 现象 | 根因 | 解决 |
|---|---|---|
| 找不到命令 | `command` 用 `~` 或相对路径 | 改绝对路径或 PATH 内命令 |
| 子进程启动失败 | Python 依赖未装/路径错 | 先在终端手动跑 `uv run python server.py` |
| 连接超时 | HTTP 地址/端口错、防火墙 | curl 先手测（04 篇）再配客户端 |
| 401/403 | Token 缺失/过期 | 检查 `headers.Authorization` 与 Server 侧校验 |
| 工具列表为空 | Server 启动报错但静默 | 查 stderr 日志（Inspector 连一次对比） |
| 改了代码不生效 | 配置了旧路径/进程未重启 | 重启 Claude Code 或 `claude mcp remove` 后重加 |
| Windows 特殊字符 | 中文路径/引号转义 | 项目放英文路径；参数用数组不用字符串 |

**排障顺序铁律**：终端手跑 Server → curl（HTTP）或 printf（stdio）手测 → Inspector 验证 → 再查 Claude Code 配置。**层层递进，不要跳级**。

> 🎯 **核心要点**：接入 Claude Code 的本质是"把 `command + args + env` 这份启动描述交给客户端"——与 03 篇 Client 的 `StdioServerParameters` 完全同构。配置就三件事：**路径要绝对、环境变量要到位、先 Inspector 验证再接入**。自己写的 Server 第一次被模型调用时，MCP 全链路就真正闭环了。

---

**参考来源**：
- [MCP Inspector 调试实战：本地开发、CLI 自动化与安全配置](https://learnagent.org/library/playbooks/mcp-inspector-guide/)
- [Explore and debug tools with MCP Inspector - Gcore Docs](https://docs.gcore.com/developer-tools/mcp-server/test-tools-with-mcp-inspector)

---

**下一模块**：[07-常见报错与排错手册](07-常见报错与排错手册.md) / **返回总览**：[00-阶段总览与学习路径](00-阶段总览与学习路径.md)
