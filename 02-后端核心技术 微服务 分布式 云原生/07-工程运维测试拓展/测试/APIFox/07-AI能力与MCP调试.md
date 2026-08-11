# 07-AI 能力与 MCP 调试
> APIFox 的 AI 三件套：文档自愈（Self-Healing）、CLI+Skill for AI Agent、MCP 调试——API 工具链的 AI 原生进化

## 📚 目录
1. [AI 能力全景](#1-ai-能力全景)
2. [文档自愈（Self-Healing）](#2-文档自愈self-healing)
3. [CLI + Skill：AI Agent 工作流入口](#3-cli--skillai-agent-工作流入口)
4. [cli-schema：结构化写入校验](#4-cli-schema结构化写入校验)
5. [MCP 调试](#5-mcp-调试)
6. [AI 分支](#6-ai-分支)
7. [AI 工作流实战闭环](#7-ai-工作流实战闭环)
8. [核心要点](#8-核心要点)
9. [参考来源](#9-参考来源)

## 1. AI 能力全景

| 能力 | 解决的问题 | 2026 状态 |
|------|-----------|-----------|
| 文档自愈（Self-Healing） | 后端重构后文档/测试/Mock 手动更新的痛苦 | ✅ 已上线 |
| CLI + Skill for Agent | AI Agent（Cursor/Claude Code 等）无法稳定操作 APIFox | ✅ 已上线 |
| cli-schema | Agent 写入复杂 JSON 结构前的校验 | ✅ 已上线 |
| MCP 调试 | MCP Server 的 Tools/Resources/Prompts 调试 | ✅ 已上线（1 月） |
| AI 分支 | AI 修改资源的隔离与可审查 | ✅ 已上线 |

> 🎯 **主线**：APIFox 正在成为"AI Agent 的 API 工作台"——不仅人用，AI 也能用，且 AI 的修改可审查、可回滚。

## 2. 文档自愈（Self-Healing）

### 2.1 解决的问题

```text
传统流程（后端重构后）：
  后端改代码（字段重命名/结构变化）
  → 手动更新接口文档（3 天人工工作量，800+ 接口）
  → 手动更新 Mock 规则与测试断言
  → 遗漏 → 前后端联调失败

AI 自愈流程：
  感知代码变更（Helper 插件 / Git Webhook）
  → AI 扫描分析 → 自动更新文档/断言/Mock
  → 版本回溯 + 影子对比（自愈预览）→ 确认生效
```

### 2.2 自愈能力细节

| 能力 | 说明 |
|------|------|
| 变更感知 | Apifox Helper 插件 / Git Webhook 实时感知代码变更 |
| AST 深度扫描 | 解析代码结构而非简单文本匹配 |
| 语义对齐 | 识别字段重命名意图（`is_deleted` → `logic_delete_flag` 可识别 98% 语义重合度） |
| 依赖映射修复 | Mock 注入规则和断言逻辑自动迁移到新字段 |
| 版本回溯 | 自愈前版本可回溯 |
| 影子对比 | 自愈预览（改前/改后对比），确认后生效 |

### 2.3 实战效果

```text
案例：某团队 800+ 接口结构大改
  传统人力：3 天
  AI 自愈：2 小时（+ 人工确认）
```

> ⚠️ **自愈不是无人值守**：AI 生成"建议修改"，人工确认后合入——与代码 AI 助手（Copilot）的定位一致：**AI 提议，人决策**。

## 3. CLI + Skill：AI Agent 工作流入口

### 3.1 为什么需要

```text
AI Agent（Cursor/Claude Code/Trae/Codex）直接操作 APIFox 的难点：
  × 没有稳定、结构化的操作接口
  × 不知道 APIFox 的命令与流程
  × 写入的 JSON 结构可能不合法

CLI + Skill 的解法：
  ✅ CLI 提供结构化命令（接口查看/用例创建/场景编排/测试运行）
  ✅ 8 个 SKILL 教 Agent 理解 CLI 命令与任务流程
  ✅ cli-schema 写入前校验 JSON 结构
```

### 3.2 Agent 可用操作

```text
接口操作：查看接口列表/详情 → 理解现有 API
用例创建：AI 根据接口定义自动生成测试用例
场景编排：AI 编排多接口业务链路
结构校验：cli-schema 校验写入的 JSON（字段名/枚举值/嵌套结构）
资源导入导出：CLI import/export
测试运行：apifox run 执行并读取报告
```

```bash
# Agent 在 Claude Code / Cursor 中调用示例
apifox run --project <ID> --test-suite <ID> --fail-on-error
apifox test-case create --project <ID> --from-api <接口ID> --assertions '...'
apifox cli-schema validate --schema ./schema.json --data ./payload.json
```

### 3.3 8 个 SKILL

SKILL 帮助 Agent 理解"任务 → 命令 → 流程"的映射：

```text
示例 SKILL 类型：
  API 查看（如何查接口）
  用例生成（如何从接口定义生成用例）
  场景编排（如何编排链路）
  结构校验（何时用 cli-schema）
  资源导入导出
  测试运行与报告解读
  ...
```

## 4. cli-schema：结构化写入校验

```bash
# 写入前校验复杂 JSON 结构
apifox cli-schema validate \
  --schema ./order-schema.json \      # 目标结构定义
  --data ./payload.json               # 待写入数据
```

| 校验维度 | 说明 |
|----------|------|
| 字段名 | 与 Schema 定义一致 |
| 枚举值 | 值在允许集合内 |
| 嵌套结构 | 深层对象/数组结构正确 |
| 类型 | 字段类型匹配 |

> 💡 价值：**AI 生成的用例/接口数据先过校验再写入**——避免"AI 写入了结构错误的数据"污染 APIFox 资源。

## 5. MCP 调试

### 5.1 能力

APIFox 可作为 **MCP Client 调试 MCP Server**（2026-01 发布）：

| 调试对象 | 说明 |
|----------|------|
| Tools | MCP 工具（可执行函数） |
| Resources | 资源（可读取数据） |
| Prompts | 提示词模板 |

| 协议支持 | 说明 |
|----------|------|
| STDIO | 本地进程 MCP Server |
| Streamable HTTP | 远程 MCP Server（现代默认） |
| OAuth 2.0 | 自动配置 MCP Server 的认证流程 |

### 5.2 使用场景

```text
场景 1：开发自己的 MCP Server
  → APIFox 当客户端调试 Tools 的参数/返回

场景 2：调试第三方 MCP Server（GitHub/搜索等）
  → 验证工具行为再接入 Agent

场景 3：AI 应用开发（配合本仓库 Spring AI / LangChain4j 体系）
  → MCP Server 上线前用 APIFox 完整验证
```

```bash
# MCP Server 配置（示意）
mcpServers:
  my-server:
    url: http://localhost:3001/mcp        # Streamable HTTP
    # 或 stdio: ["node", "server.js"]
```

> 💡 与 [03-AI大模型官方文档](../../../06-Spring全家桶/Spring%20AI/03-模型接入与结构化输出.md) 联动：调试 MCP Server 是 Agent 工具链上线的第一道验证——APIFox 补上了"API 调试工具 → MCP 调试"的能力缺口。

## 6. AI 分支

AI 分支 = 面向 AI Agent 修改的隔离机制（详见 [06](06-团队协作与导入导出.md) 第 3 节）：

```text
Agent 修改资源 → 自动创建分支 → 独立修改 → 差异预览 → 人工确认 → 合入
```

| 防护价值 | 说明 |
|----------|------|
| 主分支安全 | AI 自动化修改不直接污染主分支 |
| 可审查 | 合入前差异对比（AI 改错可放弃） |
| 可回滚 | 分支记录可回溯 |

## 7. AI 工作流实战闭环

```text
完整闭环（AI Agent 驱动 API 测试）：
① Agent 读取接口定义（CLI 查询）
② Agent 分析接口 → 生成测试用例（AI 生成断言/参数）
③ cli-schema 校验写入结构
④ Agent 在 AI 分支写入用例（防污染）
⑤ 回读确认（CLI 查询验证写入）
⑥ apifox run 运行测试 → 读取报告
⑦ 失败 → Agent 分析报告 → 修复用例 → 重跑
```

| 环节 | 工具 | 人做什么 |
|------|------|---------|
| ①-② | CLI + AI | 监督生成质量 |
| ③ | cli-schema | - |
| ④ | AI 分支 | 审查差异 |
| ⑤-⑥ | CLI | 查看报告 |
| ⑦ | AI + CLI | 确认修复 |

## 8. 核心要点

> 🎯 **核心要点**：
> - AI 三件套：文档自愈（感知代码变更自动更新）、CLI+Skill（Agent 可操作）、MCP 调试（MCP Server 验证）；
> - 自愈四能力：AST 扫描、语义对齐（98% 重命名识别）、依赖映射修复、版本回溯——**AI 提议，人决策**；
> - CLI+Skill：8 个 SKILL 教 Agent 用命令；cli-schema 保证写入结构合法；
> - MCP 调试：Tools/Resources/Prompts + STDIO/Streamable HTTP + OAuth2——Agent 工具链上线前验证；
> - AI 分支：AI 修改可审查、可回滚；
> - 完整闭环：读接口 → 生成用例 → 校验 → 分支写入 → 回读 → 运行 → 修复。

## 9. 参考来源

- [Apifox CLI + Skill 发布说明](https://www.apifox.cn/blog/apifox-cli/)
- [APIFox 1 月更新（MCP 调试/测试套件）](https://xie.infoq.cn/article/57da266c0a0e42fcdf9ad2219)
- [APIFox AI 文档自愈实战（CSDN）](https://aicoding.csdn.net/6a5e060b662f9a54cb918482.html)
- [APIFox AI 工作流标签页](https://apifox.cn/blog/tag/aigong-zuo-liu/)

---

**下一模块**：[08-生产实践与选型避坑](08-生产实践与选型避坑.md)　/　**返回总览**：[00-总览](00-APIFox知识体系总览.md)
