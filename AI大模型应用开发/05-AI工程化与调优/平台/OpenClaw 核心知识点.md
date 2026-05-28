## OpenClaw 核心知识点

OpenClaw 是一个**运行在本地的开源 AI 智能体框架**,能够通过多个聊天平台控制你的电脑、执行系统命令、自动化浏览器操作,并且支持跨设备协同工作,所有数据完全本地化存储 。 [juejin](https://juejin.cn/post/7618098747671232566)

## 核心架构组件

OpenClaw 采用分层架构设计,包含以下核心模块: [ohya](https://ohya.co/blog/openclaw-complete-guide-2026)

- **Gateway(网关)**:中央控制面板,通过单一 WebSocket 管理所有连接与配置,负责接收消息并分发到对应 Agent
- **Agent(智能体)**:真正的"大脑",理解用户需求并决策执行方案,采用 LLM 推理引擎驱动
- **Tools(工具集)**:Agent 的执行手段,包括命令行、浏览器自动化、文件操作等具体能力
- **Memory(记忆系统)**:长期记忆存储,保存历史对话与知识库,支持语义检索
- **Skills(技能插件)**:按需加载的功能模块,覆盖开发、写作、设计、办公等场景
- **Heartbeat & Cron**:主动任务执行机制,让 AI 定期自动执行预设流程

## 四大技术难点与解决方案

### 安全与权限平衡

OpenClaw 实现了多层防御体系: [juejin](https://juejin.cn/post/7618098747671232566)

1. **DM Pairing(私聊配对)**:陌生人首次发消息需生成 6 位配对码,在电脑端手动批准后才能与 AI 对话,防止未授权访问(`src/infra/device-pairing.ts`)
2. **命令执行审批**:提供 `deny`、`allowlist`、`full` 三种安全等级,危险命令执行前必须征得用户同意(`src/infra/exec-approvals.ts`)
3. **Docker 沙箱隔离**:群组聊天环境下 AI 在隔离容器中运行,无法访问真实文件系统(`src/agents/sandbox/`)
4. **安全审计系统**:内置 `openclaw doctor` 命令自动检测配置风险并提供修复建议(`src/security/audit.ts`)

### 多渠道统一适配

采用**插件化架构**实现 20+ 聊天平台的统一接入: [juejin](https://juejin.cn/post/7618098747671232566)

- 每个平台(Telegram/Discord/Slack 等)作为独立插件,通过统一接口与 Gateway 通信(`src/channels/plugins/`)
- 消息归一化处理,将不同平台格式转换为标准结构(`src/channels/session.ts`)
- 新增平台只需开发对应插件,不影响核心逻辑

### 本地执行与资源管控

通过 Runtime 池化和并发控制优化资源使用: [juejin](https://juejin.cn/post/7618098747671232566)

- **Runtime 池化**:首次请求创建进程(2-3秒),后续复用(100ms),空闲 5 分钟自动回收(`src/acp/control-plane/runtime-cache.ts`)
- **Actor 模型并发控制**:同一会话请求排队执行,避免资源竞争(`src/acp/control-plane/session-actor-queue.ts`)

### 上下文与 Token 优化

实现多维度 Token 节省机制: [juejin](https://juejin.cn/post/7618098747671232566)

- **Bootstrap 文件截断**:单文件最大 50K 字符,总量限制 200K,避免配置文件过大浪费 Token
- **Skills 按需加载**:仅注入 skills 列表(1KB),Agent 选定后再加载具体内容(5KB),避免一次性加载全部(100KB+)
- **Memory 语义检索**:先 `memory_search` 搜索相关片段,再 `memory_get` 按需拉取,而非全量加载 50KB+ 记忆文件
- **Session Compaction**:将历史对话压缩为摘要,长对话后可从 20K 降至 5K tokens

## 十大核心功能

| 功能 | 说明 | 源码位置 |
|------|------|----------|
| Gateway 控制平面 | 单一 WebSocket 管理所有连接 | `src/gateway/` |
| Multi-Agent 路由 | 7 层优先级智能路由 | `src/routing/resolve-route.ts` |
| 跨渠道消息 | 20+ 平台统一接入 | `src/channels/plugins/` |
| 浏览器控制 | Playwright + CDP 完整自动化 | `src/browser/` |
| Canvas + A2UI | Agent 驱动的可视化界面 | `src/canvas-host/` |
| Nodes 系统 | 跨设备能力调用(相机/屏幕/位置) | `src/node-host/` |
| Docker 沙箱 | 群组聊天环境隔离 | `src/agents/sandbox/` |
| DM Pairing | 私聊配对防未授权访问 | `src/infra/device-pairing.ts` |
| Skills 平台 | 按需加载技能插件 | `src/agents/skills/` |
| 心跳机制 | 主动式后台任务执行 | `src/infra/heartbeat-runner.ts` |

## Skills 智能分配机制

采用**两阶段加载 + 强制约束**策略: [developer.aliyun](https://developer.aliyun.com/article/1718388)

1. **扫描阶段**:Agent 先浏览所有 skills 的 `<description>`,判断适用性
2. **加载阶段**:选定最匹配的单个 skill 后,用 `read` 工具读取其 `SKILL.md` 并执行

强制约束规则:必须先扫描再加载、禁止一次加载多个 skills、必须先判断再加载,避免 Token 浪费(`src/agents/system-prompt.ts`) 。 [juejin](https://juejin.cn/post/7618098747671232566)

## 核心技术栈

运行时基于 **Node.js 22+ + TypeScript + Bun**,AI 引擎使用 **pi-mono** 进行 LLM 推理,浏览器自动化依赖 **Playwright**,消息平台集成 **grammY/discord.js/Bolt**,沙箱环境使用 **Docker** 实现隔离 。 [juejin](https://juejin.cn/post/7618098747671232566)
