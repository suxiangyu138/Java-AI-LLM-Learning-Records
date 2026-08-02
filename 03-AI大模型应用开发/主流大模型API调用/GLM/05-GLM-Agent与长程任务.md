# 05 - GLM Agent 与长程任务

> 🎯 GLM 的核心差异化 = Agent 长程任务开源 SOTA — 8 小时自主工作、OpenClaw 龙虾套餐深度绑定、MCP-Atlas 开源第一。本章拆解长程 Agent 的能力与落地

---

## 目录

1. [Agent 能力全景](#1-agent-能力全景)
2. [8 小时自主工作：GLM-5.1](#2-8-小时自主工作glm-51)
3. [GLM-5-Turbo：OpenClaw 龙虾搭档](#3-glm-5-turboopenclaw-龙虾搭档)
4. [MCP 生态与工具调用](#4-mcp-生态与工具调用)
5. [Agent 应用实战场景](#5-agent-应用实战场景)

---

## 1. Agent 能力全景

| 能力 | 详情 | 支撑技术 |
|------|------|----------|
| **长程任务** | 8 小时自主工作（GLM-5.1） | 异步 RL + DSA |
| **MCP 工具调用** | MCP-Atlas 开源第一 | 工具调用专项优化 |
| **多步推理** | BrowseComp 开源第一 | 思维链质量 |
| **办公文件代理** | .docx/.pdf/.xlsx 直接处理 | 文件处理能力 |
| **终端操作** | Terminal Bench 56.2 | 命令执行 |

**开源 SOTA 成绩：**

| 基准 | 排名 | 衡量 |
|------|:---:|------|
| BrowseComp | 开源第一 | 浏览+检索 |
| MCP-Atlas | 开源第一 | MCP 工具调用 |
| τ-Bench | 开源第一 | 真实业务 Agent |
| SWE-bench | 77.8 | 代码 Agent |
| Terminal Bench | 56.2 | 终端 Agent |

---

## 2. 8 小时自主工作：GLM-5.1

**GLM-5.1（2026.04.08）的核心卖点 — 可自主工作 8 小时：**

```text
8 小时自主任务示例（软件工程师 Agent）：
├── 上午 9:00  接收任务：为订单系统添加分页功能
├── 9:00-10:00  分析代码结构 → 设计实现方案
├── 10:00-12:00 实现代码 → 编写单元测试
├── 12:00-13:00 运行测试 → 修复失败用例
├── 13:00-15:00 代码审查 → 重构优化
├── 15:00-16:00 编写文档 → 提交 PR
└── 16:00-17:00 处理 Review 反馈 → 合并完成

支撑能力：
├── 长上下文（200K）→ 8 小时任务上下文不丢失
├── 稳定工具调用 → 数百次命令执行不失控
├── 长程一致性 → 不偏离初始目标
└── 自主决策 → 遇到问题自动修复/换方案
```

---

## 3. GLM-5-Turbo：OpenClaw 龙虾搭档

**GLM-5-Turbo（2026.03.16）— 专为 OpenClaw"龙虾"长链路 Agent 场景优化：**

| 维度 | 详情 |
|------|------|
| 定位 | OpenClaw 长链路 Agent 专用 |
| 定价 | ¥5 / ¥22 |
| **龙虾套餐** | 个人版 39 元/月（3500 万 Token） |
| | 进阶版 99 元/月（1 亿 Token） |
| 概念 | "99 元雇一个 AI 员工" |

```json
// OpenClaw 中配置 GLM-5-Turbo
{
  "models": {
    "providers": {
      "glm-5-turbo": {
        "type": "openai-compatible",
        "baseUrl": "https://open.bigmodel.cn/api/paas/v4",
        "apiKey": "${ZHIPU_API_KEY}",
        "model": "glm-5-turbo"
      }
    }
  }
}
```

**为什么 GLM-5-Turbo 适合 OpenClaw：**
1. 长链路 Agent 优化（龙虾场景就是长链路）
2. 工具调用稳定（MCP-Atlas 开源第一）
3. 39 元/月套餐 = 极低的 Agent 运营成本
4. 与 OpenClaw 生态深度绑定

---

## 4. MCP 生态与工具调用

```text
GLM 的 MCP 支持：
├── MCP-Atlas 基准开源第一
├── 工具调用与 OpenAI 完全同构（JSON Schema）
├── 支持并行工具调用
└── 与 LangChain4j/Spring AI/OpenClaw 无缝集成

MCP-Atlas 衡量什么：
├── MCP 服务器发现
├── 工具选择准确性
├── 参数填充正确性
└── 工具结果利用
```

```python
# GLM + MCP 工具调用（通过 OpenAI 兼容接口）
tools = [{
    "type": "function",
    "function": {
        "name": "search_docs",
        "description": "搜索公司内部文档",
        "parameters": {"type": "object", "properties": {...}}
    }
}]
# 与 04 章的 Function Calling 循环完全一致
```

---

## 5. Agent 应用实战场景

| 场景 | 模型 | 架构 |
|------|------|------|
| **金融分析** | GLM-5 | 数据检索 → 结构化推理 → 报告生成 |
| **法律合同审查** | GLM-5 | 提取条款 → 逐条分析 → 风险报告 |
| **代码库重构** | GLM-5.1 | 8 小时长程：分析→实现→测试→提交 |
| **OpenClaw 运维** | GLM-5-Turbo | 持续监控 → 告警响应 → 修复 |
| **深度调研** | GLM-5 | BrowseComp 能力：搜索+推理组合 |
| **办公自动化** | GLM-5 | .docx/.pdf/.xlsx 代理处理 |

---

> 🎯 **核心要点**：GLM 的 Agent 三张牌 — **① 8 小时自主（GLM-5.1 长程）② 龙虾套餐（GLM-5-Turbo + OpenClaw，39 元/月）③ MCP-Atlas 开源第一（工具调用最稳）**。定位总结：它是"Agent 长程任务"的开源最优解，配合 OpenClaw 生态能实现极低成本的 AI 员工。

**下一模块**：[06-GLM在开发框架中的集成](06-GLM在开发框架中的集成.md) / **返回总览**：[00-总览](00-GLM-API知识体系总览.md)
