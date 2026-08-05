# CLI 与开源 AI 编程工具

> 🔓 Aider（Git原生开源）、Cline（审批门控）、Codex CLI（OpenAI沙盒）、Amazon Q（AWS免费层）、Augment Code（SWE-bench榜首）

---

## 📚 目录

1. [Aider](#1-aider)
2. [Cline](#2-cline)
3. [Codex CLI](#3-codex-cli)
4. [Amazon Q Developer](#4-amazon-q-developer)
5. [Augment Code](#5-augment-code)

---

## 1. Aider

| 维度 | 说明 |
|------|------|
| **类型** | 终端 CLI Agent |
| **许可** | Apache 2.0 开源 |
| **价格** | 工具免费，仅付模型 API |
| **模型** | 任何 LLM（Claude/GPT/DeepSeek/Ollama） |

```text
Git 原生 → AI 修改自动 commit，可 review diff
Repo Map → 自动构建代码库结构图
自由选模型 → 用 DeepSeek 省钱或用 Claude 追质量
多模态输入 → 支持语音、图片、网页

最适合：终端极客、Git 重度用户、开源爱好者
成本：API $5-20/月（轻）~$25/天（重度）
```

---

## 2. Cline

| 维度 | 说明 |
|------|------|
| **类型** | VS Code 插件 + CLI |
| **许可** | Apache 2.0 开源 |
| **价格** | 免费（团队 $20/用户/月） |
| **模型** | 任何（OpenRouter/Anthropic/OpenAI/本地） |

```text
审批门控 → 每次编辑和命令执行前需人工批准
MCP 支持 → 连接任何 MCP 服务器
浏览器自动化 → 操作浏览器完成 Web 任务

最适合：安全审计要求高的团队
```

---

## 3. Codex CLI

| 维度 | 说明 |
|------|------|
| **类型** | 终端 CLI Agent |
| **许可** | Apache 2.0 开源 |
| **价格** | 含 ChatGPT Plus ($20/月) |
| **模型** | GPT-5.3-Codex（仅 OpenAI） |

```text
沙盒执行 → 代码在隔离环境运行，不污染本地
云任务运行器 → 长任务后台并行执行

最适合：已有 ChatGPT 订阅的 OpenAI 生态用户
⚠️ 2026.04 起 token 额度制，重度使用可能额外付费
```

---

## 4. Amazon Q Developer

| 维度 | 说明 |
|------|------|
| **类型** | IDE 插件 + CLI |
| **价格** | 免费层 50 次/月；Pro $19/用户/月 |

```text
AWS 原生 → 自动感知 IAM、生成 IaC
IP 赔偿 → Pro 层知识产权侵权赔偿
⚠️ 2026.05 停止新注册，迁移至新产品 "Kiro"
```

---

## 5. Augment Code

| 维度 | 说明 |
|------|------|
| **类型** | CLI + IDE 插件 |
| **价格** | $20-60/月（信用额度制） |
| **模型** | Claude Opus 4.5 等 |

```text
Context Engine → 索引完整代码库+依赖+Git历史
SWE-bench Pro 第一 → MongoDB/Spotify/Webflow 客户

最适合：大型企业复杂代码库、合规要求高
```

---

## 6. 快速选型

| 需求 | 推荐 |
|------|:--:|
| 开源免费+Git | **Aider** |
| 安全审批 | **Cline** |
| 已有ChatGPT | **Codex CLI** |
| AWS生态 | **Amazon Q**(迁Kiro中) |
| 企业级合规 | **Augment Code** |

---

> 🎯 开源免费选 **Aider**/**Cline**，ChatGPT 用户选 **Codex CLI**，大企业选 **Augment Code**。

---

*创建于：2026年7月*
