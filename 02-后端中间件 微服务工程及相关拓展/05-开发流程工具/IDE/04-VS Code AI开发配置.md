# 04 - VS Code AI 开发配置

> 🎯 VS Code 已成为 AI 开发的事实标准——GitHub Copilot、Cline、Continue、通义灵码，四大 AI 插件让 VS Code 从编辑器变成 AI 编程助手

---

## 目录

1. [GitHub Copilot](#1-github-copilot)
2. [Cline：开源的 Cursor 替代](#2-cline开源的-cursor-替代)
3. [Continue：本地模型](#3-continue本地模型)
4. [完整 AI 开发配置](#4-完整-ai-开发配置)

---

## 1. GitHub Copilot

```text
GitHub Copilot — 最成熟的 AI 编程助手

核心能力：
├── 代码补全（Tab 接受）
├── Copilot Chat（Ctrl+I 内联对话）
├── /explain → 解释选中代码
├── /fix → 修复选中代码
├── /tests → 生成测试
└── Agent Mode (2025) → 自主编辑多文件

快捷键：
  Tab → 接受建议
  Alt+] → 下一条建议
  Ctrl+I → 内联 Copilot Chat
  Ctrl+Shift+I → 侧边栏 Chat
```

## 2. Cline：开源的 Cursor 替代

```bash
# Cline (原 Claude Dev) — 开源 AI 编程 Agent
# VS Code 插件: claude-dev

# 特点：
# → 支持 Claude/GPT/DeepSeek/本地模型
# → 自主编辑文件、执行命令、创建项目
# → 类似 Cursor 的 Agent 模式，但开源免费
# → 支持 MCP Server

# 配置 .vscode/settings.json
{
  "cline.model": "claude-sonnet",
  "cline.maxTokens": 8192,
  "cline.autoApprove": false,  // 每次操作需确认
  "cline.mcpServers": {
    "database": {
      "command": "node",
      "args": ["mcp-server.js"]
    }
  }
}
```

## 3. Continue：本地模型

```bash
# Continue — 支持本地模型的开源 AI 编程助手

# 优势：
# → 数据不出本机（金融/医疗场景）
# → 免费（用自己的 GPU）
# → 支持 Ollama/LM Studio/llama.cpp

# .continue/config.json
{
  "models": [{
    "title": "Qwen 7B",
    "provider": "ollama",
    "model": "qwen2.5:7b"
  }, {
    "title": "DeepSeek V3",
    "provider": "openai",
    "apiBase": "https://api.deepseek.com",
    "model": "deepseek-chat"
  }],
  "tabAutocompleteModel": {
    "title": "Starcoder",
    "provider": "ollama",
    "model": "starcoder2:3b"
  }
}
```

## 4. 完整 AI 开发配置

```json
// .vscode/extensions.json — 推荐插件列表
{
  "recommendations": [
    "github.copilot",           // AI 补全（主力）
    "github.copilot-chat",      // AI 对话
    "saoudrizwan.claude-dev",   // Cline AI Agent
    "continue.continue",        // 本地模型
    "ms-python.python",         // Python
    "redhat.java",              // Java
    "ms-vscode-remote.remote-ssh", // 远程开发
    "ms-azuretools.vscode-docker"  // Docker
  ]
}
```

```text
AI 插件选择指南：
├── 预算充足 → GitHub Copilot（$10/月，最省心）
├── 开源免费 → Cline + DeepSeek API（效果接近）
├── 数据安全 → Continue + Ollama 本地模型
└── 中文友好 → 通义灵码（阿里免费，IDEA/VS Code）
```

## 核心要点回顾

- Copilot = 最成熟，$10/月 = 闭眼买
- Cline = 开源 Cursor 替代，支持 MCP Server
- Continue = 本地模型，数据不出本机
- 通义灵码 = 阿里免费，中文场景最佳
- MCP Server 配置 = VS Code + Cline = 完整的 AI 开发环境

## 参考资料

1. GitHub Copilot 文档 — docs.github.com/copilot
2. Cline GitHub — github.com/cline/cline
3. Continue 文档 — docs.continue.dev
