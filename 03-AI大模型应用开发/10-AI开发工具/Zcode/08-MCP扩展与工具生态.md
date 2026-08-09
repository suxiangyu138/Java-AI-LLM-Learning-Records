# 08 MCP 扩展与工具生态

> ZCode 的工具扩展层：MCP 协议接入外部能力、Zread 构建项目知识库、Git/Release 自动化收尾——从"会写代码"到"会用工具"。

## 📚 目录

1. [MCP 支持总览](#1-mcp-支持总览)
2. [MCP 三种配置形态](#2-mcp-三种配置形态)
3. [配置位置与优先级](#3-配置位置与优先级)
4. [官方推荐 MCP 工具](#4-官方推荐-mcp-工具)
5. [从其他工具导入 MCP](#5-从其他工具导入-mcp)
6. [Zread 智能项目知识库](#6-zread-智能项目知识库)
7. [Git 与 Release 自动化](#7-git-与-release-自动化)

## 1. MCP 支持总览

MCP（Model Context Protocol）是模型上下文协议，让 Agent 通过标准化接口调用外部工具。

| 维度 | ZCode 支持 |
|---|---|
| 协议类型 | stdio、HTTP/SSE、完整 JSON 配置 |
| 配置范围 | 用户级 + 工作区级 |
| 导入能力 | 从 Claude Code、Codex CLI、OpenCode 一键导入 |
| 官方工具 | GLM Coding Plan 捆绑 Web Search、Web Reader、Zread、Vision 等 |

## 2. MCP 三种配置形态

| 形态 | 适用 | 特点 |
|---|---|---|
| stdio | 本地命令行工具 | 通过标准输入输出通信，最常见 |
| HTTP/SSE | 远程服务 | 网络通信，适合共享服务 |
| JSON 完整配置 | 复杂服务 | 完整参数配置形态 |

配置示例（stdio 型）：

```json
{
  "mcpServers": {
    "web-search": {
      "command": "npx",
      "args": ["-y", "web-search-mcp"],
      "env": { "API_KEY": "xxx" }
    }
  }
}
```

## 3. 配置位置与优先级

| 位置 | 作用域 | 说明 |
|---|---|---|
| `~/.zcode/cli/config.json` | 用户级 | 所有项目可用 |
| `<项目根>/.zcode/config.json` | 工作区级 | 仅当前项目 |
| `~/.agents/mcp.json` | 兼容目录 | 兼容其他 Agent 工具的 MCP 配置 |

优先级：工作区级 > 用户级（同名工具以工作区为准）。

> ⚠️ 工作区级配置不要提交到 Git 仓库（可能含密钥），或用环境变量引用敏感信息。

## 4. 官方推荐 MCP 工具

| 工具 | 能力 | 场景 |
|---|---|---|
| zai-mcp-server | 视觉理解 | 图片分析、设计稿理解 |
| web-search-prime | 联网搜索 | 查文档、查最新资讯 |
| web-reader | 网页读取 | 读网页内容融入上下文 |

GLM Coding Plan 捆绑以上 MCP 工具，订阅用户开箱即用。

## 5. 从其他工具导入 MCP

支持从 Claude Code、Codex CLI、OpenCode 一键导入已有 MCP 配置：

- 迁移场景：Claude Code 用户切到 ZCode，`~/.claude.json` 里的 MCP 服务器配置直接导入
- 兼容目录 `~/.agents/mcp.json`：多工具共享同一份 MCP 配置
- 导入后检查：确认导入的工具在 ZCode 内能正常调用（不同工具的参数形态可能有差异）

## 6. Zread 智能项目知识库

Zread 是 ZCode 3.0 内置的智能项目知识库：

| 能力 | 说明 |
|---|---|
| 自动读取 | 扫描整个项目生成结构化文档 |
| 目录结构说明 | 项目骨架地图 |
| 模块依赖可视化 | 关键模块与依赖关系图 |
| 进度监测 | 哪些文件已分析、进行到哪里 |
| 一键重新生成 | 代码变化后刷新知识库 |
| 随时终止 | 大项目分析耗时可控 |

价值场景：

- **接手陌生项目**：5 分钟生成"项目地图"，不用人肉通读
- **长程任务前**：先让 Zread 建立知识库，Agent 后续引用更精准
- **与 AGENTS.md 互补**：Zread 是"现状地图"（自动生成），AGENTS.md 是"约定规则"（手写）

## 7. Git 与 Release 自动化

### Git 集成能力

（04 篇已列基础能力，此处聚焦自动化）

- AI 生成标准提交信息：符合 Conventional Commits 规范
- 分支图谱可视化操作

### Release Bot

| 能力 | 说明 |
|---|---|
| changelog 生成 | 按提交历史自动生成更新日志 |
| GitHub Release 草稿 | 自动创建发布草稿 |
| tag 校验 | 版本号与 tag 一致性检查 |
| 版本号同步 | 多文件版本号联动 |
| release note 预览 | 发布前人工预览确认 |

适用场景：开源项目发布、定期发版——把"写 changelog、打 tag、发 release"的机械工作交给 Bot。

> 🎯 核心要点：ZCode 的生态观是"GLM 做大脑、MCP 做手脚、Zread 做地图、Release Bot 做收尾"——工具链完整覆盖"理解项目 → 干活 → 验证 → 发布"全流程。

---

**下一模块**：[09-生产实践避坑与竞品对比](09-生产实践避坑与竞品对比.md) / **返回总览**：[00-ZCode知识体系总览](00-ZCode知识体系总览.md)
