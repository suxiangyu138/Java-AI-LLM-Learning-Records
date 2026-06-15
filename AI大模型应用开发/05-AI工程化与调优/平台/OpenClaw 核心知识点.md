# ⚙️ OpenClaw 核心知识点

> **核心摘要**：OpenClaw 是运行在本地的开源 AI 智能体框架，可通过多个聊天平台控制电脑、执行系统命令、自动化浏览器操作。本文涵盖核心架构组件、四大技术难点解决方案（安全/多渠道/资源管控/Token 优化）及十大核心功能。

> **前置阅读**：[[快速精通OpenClaw]]、[[OpenClaw 部署与运维实战]]

---

## 一、核心架构组件

OpenClaw 采用分层架构设计，核心模块如下：

| 组件 | 说明 |
|---|---|
| **Gateway（网关）** | 中央控制面板，WebSocket 管理所有连接与配置 |
| **Agent（智能体）** | 真正的"大脑"，LLM 推理引擎驱动，理解需求并决策 |
| **Tools（工具集）** | 命令行、浏览器自动化、文件操作等执行手段 |
| **Memory（记忆系统）** | 长期记忆存储，支持语义检索 |
| **Skills（技能插件）** | 按需加载的功能模块 |
| **Heartbeat & Cron** | 主动任务执行机制，定期执行预设流程 |

---

## 二、四大技术难点与解决方案

### 2.1 安全与权限平衡

| 防御层 | 说明 |
|---|---|
| **DM Pairing（私聊配对）** | 陌生人需 6 位配对码在电脑端批准后才能与 AI 对话 |
| **命令执行审批** | deny/allowlist/full 三级安全等级 |
| **Docker 沙箱隔离** | 群组环境 AI 在隔离容器中运行 |
| **安全审计** | `openclaw doctor` 自动检测配置风险 |

### 2.2 多渠道统一适配

20+ 聊天平台采用插件化架构统一接入。每个平台作为独立插件，通过统一接口与 Gateway 通信，消息归一化为标准格式。

### 2.3 本地执行与资源管控

- **Runtime 池化**：首次请求 2-3 秒创建进程，后续 100ms 复用，空闲 5 分钟自动回收
- **Actor 模型并发控制**：同会话请求排队执行

### 2.4 上下文与 Token 优化

| 机制 | 效果 |
|---|---|
| **Bootstrap 文件截断** | 单文件 ≤ 50K 字符，总量 ≤ 200K |
| **Skills 按需加载** | 仅加载选中的技能，避免 100KB+ 一次性加载 |
| **Memory 语义检索** | 先搜索片段，再按需拉取 |
| **Session Compaction** | 历史对话压缩为摘要，20K → 5K tokens |

---

## 三、十大核心功能

| 功能 | 说明 |
|---|---|
| Gateway 控制平面 | 单一 WebSocket 管理所有连接 |
| Multi-Agent 路由 | 7 层优先级智能路由 |
| 跨渠道消息 | 20+ 平台统一接入 |
| 浏览器控制 | Playwright + CDP 完整自动化 |
| Canvas + A2UI | Agent 驱动的可视化界面 |
| Nodes 系统 | 跨设备能力调用（相机/屏幕/位置） |
| Docker 沙箱 | 群组环境隔离 |
| DM Pairing | 私聊配对防未授权访问 |
| Skills 平台 | 按需加载技能插件 |
| 心跳机制 | 主动式后台任务执行 |

---

## 四、Skills 智能分配机制

采用**两阶段加载 + 强制约束**策略：

1. **扫描阶段**：Agent 浏览所有 Skills 的 `<description>`，判断适用性
2. **加载阶段**：选定最匹配的单个 Skill，用 `read` 工具读取 `SKILL.md` 并执行

强制约束：必须先扫描再加载、禁止一次加载多个 Skills、必须先判断再加载。

---

## 五、核心技术栈

| 技术 | 说明 |
|---|---|
| 运行时 | Node.js 22+ + TypeScript + Bun |
| AI 引擎 | pi-mono LLM 推理 |
| 浏览器自动化 | Playwright |
| 消息平台 | grammY / discord.js / Bolt |
| 沙箱环境 | Docker |

---

## 核心要点回顾

- OpenClaw 是本地运行的开源 AI Agent 框架，支持 20+ 聊天平台
- 四层安全防御：DM Pairing + 命令审批 + Docker 沙箱 + 安全审计
- 五大 Token 优化机制显著降低 LLM 调用成本
- Skills 两阶段加载策略（扫描 → 加载）提升效率
- 核心技术栈：Node.js + TypeScript + Playwright + Docker

## 参考资料

1. OpenClaw GitHub：https://github.com/openclaw/openclaw
2. OpenClaw 文档：https://docs.openclaw.ai
3. Playwright 自动化：https://playwright.dev
4. Docker 沙箱：https://docs.docker.com
