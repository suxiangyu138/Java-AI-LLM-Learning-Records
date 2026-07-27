# 06 - Codex 与 Copilot、Claude Code 全面对比

> 🎯 "Codex 就是 Copilot 吧？"——面试中的经典陷阱题。答出两代区分 + 三方对比才拿满分

---

## 1. Codex vs GitHub Copilot（最核心区分）

| 维度 | 新一代 OpenAI Codex | GitHub Copilot |
|------|------|------|
| 定位 | 独立 AI 软件工程智能体（完整产品） | IDE 嵌入式代码补全助手 |
| 核心能力 | 全流程自主开发：读仓库、改多文件、跑测试、提交 PR | 编辑器实时单行/函数代码补全、Chat 辅助 |
| 运行环境 | 云端隔离沙箱 + 本地 CLI + ChatGPT 内置 | VS Code / JetBrains 本地 IDE |
| 模型底座 | GPT-5/5.3-Codex 专用代码模型 | GPT-4o 多模型混合（不再依赖 Codex！） |
| 适用场景 | 大型项目重构、批量 Bug 修复、完整功能开发 | 日常编码快速写样板代码、单行逻辑补全 |
| 自主性 | 高（可无人值守批量任务） | 低（每次需人工触发） |
| 更新模型 | Codex CLI 开源（自己部署） | 云端自动更新 |

**关键理解**：初代 Copilot 用 Codex 模型，但现在二者已完全独立。Copilot 已迁移到 GPT-4o，新一代 Codex 是 Agent 产品。

---

## 2. Codex vs Claude Code

| 维度 | Codex | Claude Code |
|------|:---:|:---:|
| 代码生成 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 架构理解 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 多文件编辑 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 云端沙箱 | ✅ 原生 | ❌ 本地执行 |
| 上下文窗口 | 14KB（长代码记忆） | 200K（超大） |
| Token 效率 | 高（约 Claude 1/4） | 低 |
| Agent 成熟度 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 幻觉率 | 中 | 低 |
| Java 生态 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

**选型建议**：
- 批量任务/成本敏感 → Codex（Token 便宜 4×，云端并行）
- 超大项目架构分析 → Claude Code（200K 上下文，幻觉更少）

---

## 3. Codex vs Gemini CLI

| 维度 | Codex | Gemini CLI |
|------|:---:|:---:|
| 生态整合 | OpenAI 生态（ChatGPT/DALL-E） | Google 生态（Google Cloud/搜索） |
| 代码能力 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 多模态 | 图片理解（ChatGPT 入口） | 原生多模态（Gemini 2.5） |
| 开源 | Codex CLI 开源 | 闭源 |

---

## 4. 常见认知误区

| 误区 | 真相 |
|------|------|
| "Codex 就是 Copilot" | 初代 Copilot 用 Codex 模型，现在二者是完全独立产品线 |
| "Codex 只能生成代码片段" | 新一代可独立完成跨文件完整功能开发 |
| "Codex 生成的代码可直接上线" | 存在代码幻觉、开源许可证合规风险，必须人工审查 |
| "Codex 会替代所有程序员" | 替代的是重复性编码，架构决策/代码审查仍需人类 |

---

## 5. 选型决策树

```text
日常编码补全 → Copilot（IDE 原生、最快）
复杂重构/批量任务 → Codex CLI（Agent 自主执行）
超大项目架构 → Claude Code（200K 上下文、幻觉少）
Google 生态 → Gemini CLI
预算有限 → Codex（Token 效率高）
```
