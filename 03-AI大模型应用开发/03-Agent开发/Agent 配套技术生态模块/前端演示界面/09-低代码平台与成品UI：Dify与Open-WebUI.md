# 低代码平台与成品 UI：Dify 与 Open WebUI

> 不想写代码就想要界面？两条路：低代码平台（Dify 拖拽搭 Agent 并发布 Web App）与成品 UI（Open WebUI/LobeChat 开箱即用的完整聊天产品）。2026 年它们都已长成"能打的界面"。

## 1. 两条路线的本质区别

| 维度 | 低代码平台（Dify 类） | 成品 UI（Open WebUI/LobeChat 类） |
|---|---|---|
| 核心诉求 | 搭 Agent + 界面一体 | 已有模型服务，要现成界面 |
| 用户 | 非前端的产品/运营/教学 | 自托管爱好者/团队内部 |
| Agent 能力 | 原生工作流/Agent 构建器 | 依赖后端模型/工具能力 |
| 可扩展 | DSL 导入导出、API、插件 | 社区插件、二次开发 |
| 部署重量 | 重（多服务，K8s 友好） | 中（单容器为主） |

> 🎯 核心要点：**Dify 解决"没有开发能力怎么出 Agent 产品"，Open WebUI 解决"有模型怎么快速用上"**——前者交付 Agent+界面，后者交付纯界面。

## 2. Dify（1.16.0/1.16.1，2026-07-19）

| 能力 | 说明 |
|---|---|
| 工作流 | 拖拽节点编排（LLM/工具/知识库/分支），DSL 0.7.0 导入导出 |
| 原生 Agent | 1.16 新增：Linux 沙箱执行、Agent Builder 对话式配置工具与技能、Agent v2 节点（`agent_job`） |
| Web App 发布 | Agent/应用一键发布为品牌化 Web 界面（聊天式/工作流式） |
| MCP | Workflow-as-MCP-Server 支持 2025-06-18 协议、版本协商 |
| AI 生成工作流 | ⌘K → `/create` 自然语言生成工作流（超时 180s 可配） |
| 模型适配 | GPT-5.6 默认 Responses API；DeepSeek/Qwen/Ollama 全兼容 |
| 安全 | 原生 Agent 建议仅对受信任用户开放（沙箱执行不可信代码风险） |

**适用**：产品/运营团队无代码搭建客服、知识库问答 Agent；教程与课程演示（可视化可讲解）。

**局限**：重度定制受平台约束；自托管组件多（API/Worker/DB/向量库）；沙箱 Agent 尚属早期（open beta）。

## 3. Open WebUI（0.11，2026-08"最大版本"）

| 能力 | 说明 |
|---|---|
| 模型管理 | 一键对接 Ollama/OpenAI 兼容端点（含 DeepSeek 等），UI 内切模型 |
| 子 Agent | 复杂子任务委托后台 Agent 并行执行、结果回主对话 |
| 会话分叉（Fork） | 从任意历史消息开新分支探索（0.11 主打） |
| Skills | `$` 命令调用技能集；分析面板看用量/Token 排行 |
| 内置工具 | 联网搜索、代码执行、图片生成（可逐会话开关） |
| 团队功能 | Channels、Notes、RBAC、SSO/OIDC/LDAP、SCIM 2.0 |
| 开放协议 | Open Responses Protocol（实验）：扩展思考与流式推理 token |
| 存储 | SQLite/PostgreSQL（PG 下聊天搜索大幅提速） |

**⚠️ 升级提醒**：0.11 含数据库 schema 变更——升级前必须备份；多 worker 部署需同步升级。

**适用**：自托管模型服务的统一入口（家庭实验室/企业内部）；OpenAI 兼容端点一键接入；评估不同模型表现。

## 4. LobeChat（v2.1.x，2026-03-26）

| 能力 | 说明 |
|---|---|
| 多 Agent 编排 | 聊天群组、Supervisor 模式、并行多 Agent 回复（v2 核心） |
| Agent Builder | 自然语言描述即建 Agent；Agent Groups 共享上下文协作 |
| 知识库 RAG | pgvector 语义检索 |
| 插件生态 | 10,000+ MCP 兼容工具/插件 |
| Bot 平台 | Discord + 微信 Bot 发布 |
| 语音 | TTS/STT（OpenAI Audio 等） |
| 形态 | PWA + 桌面客户端 + Docker/Vercel 一键部署 |

**定位**：比 Open WebUI 更"产品化"、更好看（60K+ stars 自称最漂亮的自托管 ChatGPT 替代品）；多用户需 PostgreSQL。

## 5. Marimo（0.23.12，2026-07-01）

| 能力 | 说明 |
|---|---|
| 反应式笔记本 | 数据依赖驱动重算（替代 Jupyter），可当 Web 应用部署 |
| marimo pair | **Agent 驻进笔记本**：AI 直接读写变量、执行单元格、装包，人机共享画布 |
| SQL/图表 | 原生 SQL 查询 + 交互图表 |
| AI 集成 | Copilot 内置、MCP 支持、`marimo check` 代码规范检查 |

**适用**：数据分析型 Agent 的演示界面——"Agent 边干活边留痕"的科研/数据场景；与[代码执行沙盒](../代码执行沙盒/00-代码执行沙盒总览.md)模块的 Jupyter 篇互补。

## 6. 五者横向对比

| 维度 | Dify | Open WebUI | LobeChat | Marimo | 自建（03-08 方案） |
|---|---|---|---|---|---|
| 写代码量 | 近零 | 零 | 零 | 少 | 全量 |
| Agent 能力 | 原生最强 | 中（靠后端） | 强（编排） | 数据场景 | 任意 |
| 界面品牌化 | 可配置 | 固定 | 可换肤 | 固定 | 完全自主 |
| 多用户 | 完整 | 完整（RBAC） | 完整 | 弱 | 自建 |
| 部署 | 重 | 轻（单容器） | 轻 | 轻 | 自建 |
| 典型人群 | 产品/运营 | 自托管玩家 | 个人/团队 | 数据科学家 | 开发团队 |

## 7. 决策树：界面方案怎么选

```text
有开发团队？ ──是──→ 需要产品化/定制？ ──是──→ AI SDK + React（07）
                 │                    └─否──→ Chainlit（05）
                 └─否──→ 只演示不长期用？ ──是──→ Streamlit / Gradio（03/04）
                           │
                           └─否──→ 要搭 Agent 产品？ ──是──→ Dify（09）
                                     │
                                     └─否──→ 有模型要现成界面？──是──→ Open WebUI / LobeChat（09）
                                               └─否──→ 按 01 模块六诉求重新评估
```

> 🎯 核心要点：成品 UI 与低代码平台的共同前提是**"你的 Agent 逻辑足够标准，能塞进平台的模板"**——一旦需要平台没有的交互（自定义审批、私有协议），回落到 [07](07-现代前端方案：AI-SDK与React生态.md)/[05](05-Chainlit对话界面.md) 自建的成本反而更低。

---

**下一模块**：[10-生产实践与面试冲刺](10-生产实践与面试冲刺.md)　**返回总览**：[00-前端演示界面总览](00-前端演示界面总览.md)

## 参考来源

- [Dify Release v1.16.0（GitHub）](https://github.com/langgenius/dify/releases/tag/1.16.0)
- [Dify Release v1.14.2（GitHub）](https://github.com/langgenius/dify/releases/tag/1.14.2)
- [Open WebUI Releases（GitHub）](https://github.com/open-webui/open-webui/releases)
- [Open WebUI 0.8: Skills, Analytics（Smart Home Digest）](https://smarthomedigest.com/articles/open-webui-0-8-skills-analytics)
- [LobeChat Release v2.1.47（GitHub）](https://github.com/lobehub/lobehub/releases/tag/v2.1.47)
- [marimo 0.22.5 Release（GitHub）](https://github.com/marimo-team/marimo/releases/tag/0.22.5)
- [Introducing marimo pair（marimo Blog）](https://marimo.io/blog/marimo-pair)
