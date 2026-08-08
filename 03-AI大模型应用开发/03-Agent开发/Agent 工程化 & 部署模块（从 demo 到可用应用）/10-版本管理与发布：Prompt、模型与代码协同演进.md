# 10 版本管理与发布：Prompt、模型与代码协同演进

> 定位：让"行为变更"（prompt/模型/工具）像代码发布一样可对比、可灰度、可回滚（2026-08 基准）

## 📚 目录

1. [三种版本纠缠的问题](#1-三种版本纠缠的问题)
2. [Prompt 版本注册表](#2-prompt-版本注册表)
3. [依赖快照：真正的版本是元组](#3-依赖快照真正的版本是元组)
4. [发布流程：shadow → canary → promote](#4-发布流程shadow--canary--promote)
5. [回滚机制：label 指针与缓存陷阱](#5-回滚机制label-指针与缓存陷阱)
6. [评估门禁与发布自动化](#6-评估门禁与发布自动化)
7. [工具选型](#7-工具选型)
8. [反模式速查](#8-反模式速查)

## 1. 三种版本纠缠的问题

```text
代码 / prompt / 模型——三者独立演进，行为由三者共同决定：

  ✗ prompt 写死在代码里  → 改 prompt 要发版；回滚 prompt 是回滚代码
  ✗ 模型版本不记录       → 供应商一升级，行为漂移无感知
  ✗ 无发布流程           → "0 到 100% 一把梭"（2026 被称作 agentic 部署的原罪）

  ✓ 三者独立版本化 + 运行时解析 + 组合快照记录
```

> 🎯 **核心原则**：每次变更都是新版本，绝不原地编辑——版本化才有对比（A/B）、回滚（指针）、审计（谁在何时改了什么）。

## 2. Prompt 版本注册表

### 2.1 机制

```text
prompt 从代码迁出，存为注册表中的版本化对象：

  prompt_v13 = {
    template: "...",
    model_hint: "deepseek-chat",     # 依赖快照（见下节）
    toolset: ["order_query", "refund"],
    created_at, author, comment
  }

环境标签（label）部署：
  prod 标签 → v12      staging 标签 → v13      canary 标签 → v14
运行时按 label 解析；切换版本 = 改 label 指向，不是重新部署
```

### 2.2 版本注册表要求

| 要求 | 说明 |
|------|------|
| 不可变 | 版本内容冻结，一经发布不可修改（只能发新版本） |
| 标签 | prod/canary/staging 三标签，指向任意版本 |
| 审计 | 谁、何时、为何改（comment 必填） |
| 回滚 | label 指回旧版本 = 秒级回滚 |
| 快照 | 记录依赖组合（prompt+model+index+toolset） |

## 3. 依赖快照：真正的版本是元组

```text
行为 = f(prompt, model, retrieval_index, toolset, runtime_config)

发布时必须记录完整元组——只看 prompt 版本无法解释行为：
  - 模型升级（deepseek-chat → 新版本号）后 prompt 没动，行为照样变
  - 检索索引重建后 RAG 结果变，回答跟着变
  - 工具集增删工具 = 行为边界变化

生产要求：trace 里记录版本快照（04 篇），
         排错时"prompt hash + model version + code version"三件套定位
```

## 4. 发布流程：shadow → canary → promote

```text
阶段 1 Shadow（0% 服务）
  新版本跑真实流量，输出只记录不返回
  用 golden set + 在线对比评分（06 篇）
  → 捕获 golden set 没覆盖的真实边界

阶段 2 Canary（5-10% 流量）
  按 user_id 哈希切片给真实用户
  KPI 实时监控：eval 分数 / 成本 / 拒绝率 / P99
  回归信号（分数持续 2-3pp 下滑 / 成本突增）→ 自动回滚

阶段 3 Promote（100%）
  通过后全量；旧版本保持热备 ~24h（随时秒回）
```

| 阶段 | 流量 | 验证 | 通过标准 |
|------|:---:|------|---------|
| shadow | 0% | 离线 + 影子对比 | 分数 ≥ 当前版本 |
| canary | 5-10% | 在线 KPI | 无显著下滑 + 无成本异常 |
| promote | 100% | 在线监控 | 持续观察 24h |

> 💡 **金丝雀工具**：Argo Rollouts / Flagger（K8s）+ OpenFeature（per-user flag 灰度）；prompt 级灰度直接靠注册表 label + user 路由。

## 5. 回滚机制：label 指针与缓存陷阱

### 5.1 回滚的正确姿势

```text
  ✓ label 指回已知良好版本（v12）——秒级、无重部署
  ✓ 版本不可变 → 指回的一定是发布过的确切内容
  ✓ 回滚演练：季度一次——"一年能用一次的回滚就是不能用的回滚"
```

### 5.2 缓存幽灵陷阱（必踩）

```text
场景：v13 上线 → 缓存了 v13 的响应指纹 → 回滚到 v12
后果：缓存命中仍返回 v13 指纹的结果——回滚了但用户还在"吃"新版本

铁律：回滚 = label 移动 + 缓存失效（响应缓存 / 语义缓存 / 前缀缓存一并清）
     否则幽灵服务旧版本，回滚"看起来成功实则没生效"
```

## 6. 评估门禁与发布自动化

```text
发布流水线：
  PR → CI（测试 05 篇）→ 评估门禁（06 篇）→ staging → shadow → canary → promote

评估门禁细节：
  - candidate vs 当前 prod 版本，同一 golden set 对比
  - 统计回归（t-test）不过 → 构建失败，不进入发布
  - 每次发布产出"版本对比报告"（分数增量 + 差异样本）——发布评审的唯一依据

trace 闭环：
  - 每次发布给新版本打上版本标签（trace 元数据）
  - 线上行为回放 → 回流 golden set → 喂给下一轮优化
```

## 7. 工具选型

| 工具 | 亮点 | 注意 |
|------|------|------|
| Vellum | 最正式的发布机制：Immutable Releases、SOC 2、RBAC、审计日志 | 商业化 |
| PromptLayer | 最干净的独立注册表 + 流量拆分 release labels | 需接 SDK |
| Langfuse | 开源首选：production labels + 运行时拉取（MIT 核心） | eval 深度弱 |
| Portkey | label 部署 + 多供应商网关（路由/fallback 一体） | 网关绑定 |
| LangSmith | LangChain 生态原生 | 绑定框架 |
| Future AGI | label 部署 + 内置 eval 门禁 + canary 路由网关（~29k req/s，P99 ≤21ms） | 新一代自托管栈 |
| Argo Rollouts + OpenFeature | 服务级金丝雀 + 用户级 flag（开源组合） | 需 K8s 运维 |

## 8. 反模式速查

| 反模式 | 后果 | 修正 |
|--------|------|------|
| prompt 内联字符串 | 回滚=代码回滚，无审计 | 注册表 + label |
| 无 eval 门禁 | 发布=赌运气，用户当测试集 | 统计回归门禁 |
| 0→100% 一把梭 | 坏了全量受影响 | shadow→canary→promote |
| 无 per-version trace 指标 | canary"好不好"无从回答 | 版本标签入 trace |
| 回滚不清缓存 | 幽灵服务旧版本 | 回滚=指针+清缓存 |
| 无漂移告警 | 模型悄悄升级，质量悄悄下滑 | eval 分数漂移告警（11 篇） |
| 不记录依赖快照 | 模型变了无法归因 | 元组快照入 trace |

---

## 【参考来源】

- [futureagi.com: Prompt Versioning and Lifecycle Management in 2026](https://futureagi.com/blog/prompt-versioning-lifecycle-management-2026/)
- [futureagi.com: Best Prompt Deployment Platforms for Production LLM Apps in 2026](https://futureagi.com/blog/best-prompt-deployment-platforms-for-production-llm-apps-in-2026/)
- [futureagi.com: CI/CD for AI Agents in 2026（canary、回滚、OpenFeature）](https://futureagi.com/blog/ci-cd-for-ai-agents-best-practices-2026/)
- [futureagi.com: LLM Deployment Best Practices in 2026](https://futureagi.com/blog/llm-deployment-best-practices-2026/)
- [callsphere.ai: Chat Agent Prompt Versioning and Rollback in Production: 2026 Patterns](https://callsphere.ai/blog/vw3b-chat-agent-prompt-versioning-rollback-2026)
- [GitHub: llmops-cicd（LLMOps CI/CD 参考实现：版本、eval、canary、成本）](https://github.com/techlearn-center/llmops-cicd)
- [GitHub: proofhound（开源 prompt 管理/发布平台）](https://github.com/proofhound/proofhound)
- 评估门禁细节：见 [06-评估体系](06-评估体系：Eval、回归门禁与%20LLM-as-Judge.md)

---

**返回总览**：[00-总览：Agent 工程化与部署模块（从 demo 到可用应用）](00-总览：Agent%20工程化与部署模块（从%20demo%20到可用应用）.md)

**下一模块**：[11-生产治理：配额、SLA、成本告警与监控闭环](11-生产治理：配额、SLA、成本告警与监控闭环.md)
