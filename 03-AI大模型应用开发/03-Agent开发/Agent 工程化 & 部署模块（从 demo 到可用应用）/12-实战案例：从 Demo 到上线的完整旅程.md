# 12 实战案例：从 Demo 到上线的完整旅程

> 定位：用"客服退款 Agent"一个完整案例串起本模块全部篇章——每个决策讲清"选了什么、为什么、代价是什么"（2026-08 基准）

## 📚 目录

1. [案例背景与 Demo 现状](#1-案例背景与-demo-现状)
2. [阶段 0：差距盘点与改造顺序](#2-阶段-0差距盘点与改造顺序)
3. [阶段 1：工程化重构](#3-阶段-1工程化重构)
4. [阶段 2：质量体系（测试 + 评估）](#4-阶段-2质量体系测试--评估)
5. [阶段 3：性能与安全](#5-阶段-3性能与安全)
6. [阶段 4：部署与发布](#6-阶段-4部署与发布)
7. [阶段 5：上线后治理](#7-阶段-5上线后治理)
8. [关键决策复盘表](#8-关键决策复盘表)
9. [验收检查单](#9-验收检查单)

## 1. 案例背景与 Demo 现状

```text
业务：某电商客服 Agent——三个能力：
  1. 查订单（状态/物流）        —— 只读工具
  2. 两段式退款（查询 → 申请退款）—— 写操作，需审批
  3. 闲聊与商品问答             —— 只答不动

Demo 现状（典型 L1）：
  - 单文件 agent.py（900 行），key 硬编码，print 排错
  - 工具 5 个全部放开，max_steps=20 写死
  - 手动测了 10 个场景"看起来不错"，没有 eval
  - 同步 HTTP，用户等完整答案（长任务时等 40 秒）
  - prompt 内联在代码里，模型写死
```

## 2. 阶段 0：差距盘点与改造顺序

按 01 篇清单逐项核对，输出改造顺序（先看得到 → 再跑得起 → 后防得住）：

| 序 | 差距 | 风险等级 | 对应篇章 |
|:---:|------|:---:|---------|
| 1 | 无 trace 无日志 | 🔴 上线即瞎 | 04 |
| 2 | 无评估基线 | 🔴 改了不知道好坏 | 06 |
| 3 | 退款无审批门 | 🔴 一次注入=资金损失 | 08 |
| 4 | key 硬编码 | 🔴 泄露即账单爆炸 | 03 |
| 5 | 长任务同步等待 | 🟡 体验差 | 09 |
| 6 | prompt 内联 | 🟡 改版要发版 | 10 |
| 7 | 无缓存无路由 | 🟢 账单偏高 | 07 |

## 3. 阶段 1：工程化重构

### 3.1 架构（02 篇落地）

```text
分层落地：
  api/       → FastAPI：/chat（同步）、/chat/stream（SSE）、/jobs（异步）
  orchestration/ → agent_loop.py：主循环 + 预算策略 + 审批门
  tools/     → order_query（只读）、refund_apply（写+审批）、product_qa（只读）
               refund_apply 实现幂等：idempotency_key = 会话+订单号
  infra/     → llm/（OpenAI + DeepSeek 双 Provider）、cache.py、telemetry.py
  data/      → session_store（Redis）

边界决策：
  - 退款金额校验、双重退款防重、审批流 = 确定性代码
  - 只有"理解诉求 + 起草请求"交给 LLM（04 篇混合架构）
```

### 3.2 配置（03 篇落地）

```text
dev/staging/prod 三套独立账户 key（staging 与 prod 不同账户）
模型配置外置：default=deepseek-chat / reasoner=deepseek-reasoner
runtime 热更新：max_tool_calls、tool_timeout、预算（配中心）
prompt 迁移到注册表：v1（初始版），label: prod→v1
```

## 4. 阶段 2：质量体系（测试 + 评估）

### 4.1 测试（05 篇落地）

```text
单元：refund 金额校验、幂等、预算逻辑（纯函数，无网络）
组件：Mock LLM（ReplayProvider + 契约 stub）覆盖编排：
      - 死循环防护（fake 一直请求退款工具 → max_tool_calls 截断）
      - 错误恢复（工具返回 retry:false → 转人工话术）
      - 审批门（金额 > 300 元 → 触发审批事件而非直接执行）
契约：退款请求 JSON Schema 锁定；非法输出走降级
故障注入：Provider 超时 → 重试 → fallback 模型；流式中断 → 重连
```

### 4.2 评估（06 篇落地）

```text
golden set：120 例（查询 50 / 退款 40 / 闲聊 30），含 15 个边界
  （跨用户查单拒绝、金额超限、订单不存在、退款状态冲突…）
三层级联：确定性检查（schema/拒绝名单）→ NLI → LLM-as-Judge
统计门禁：Welch t-test + |delta|>=0.03，基线存 git
轨迹评估：工具选择准确率、参数正确性、结果利用、错误恢复
judge 校准：抽 40 例人工 vs judge 一致性验证通过后启用
```

## 5. 阶段 3：性能与安全

### 5.1 性能（07 篇落地）

```text
Prompt 缓存：system + 工具声明前缀固定，工具不增删 → 命中率目标 >80%
缓存感知路由：请求前缀哈希路由（多实例不盲轮询）
语义缓存：订单状态查询类高频重复 → 命中 60%+（带 60s TTL 防过期状态）
并行工具：查询类无依赖工具 asyncio.gather
模型路由：闲聊 → cheap，退款推理 → reasoner
实测收益：查询路径 TTFT ~1.5s → ~300ms（缓存命中）
```

### 5.2 安全（08 篇落地）

```text
工具白名单：order_query/product_qa 只读；refund_apply 仅内部接口可调
权限：per-tool 白名单 + max_tool_calls=50 + 单请求预算
审批门：退款金额 > 300 元 或 任一写操作 → 人工审批
        （LangGraph interrupt；审计 approved_by: auto|human）
提示注入防线：工具结果包 sentinel 标签；输入分类；jailbreak 上报审计
身份：Agent 用 OAuth 2.1 PKCE 身份调用内部 API，无共享 key
供应链：MCP 工具版本 pin；AiBOM 记录模型/工具清单
脱敏：trace/日志 PII mask（订单号、手机号）
```

## 6. 阶段 4：部署与发布

### 6.1 部署形态（09 篇落地）

```text
三形态并存：
  /chat             → 闲聊、缓存命中查询（同步，<2s）
  /chat/stream      → 复杂查询、退款流程（SSE，过程可见）
  /jobs             → 批量报告（队列：Redis Streams + worker + SSE 回放）

网关：LiteLLM Proxy（deepseek-chat/reasoner 逻辑名路由 + 备用供应商 fallback）
K8s：无状态容器、HPA（同步按 QPS，异步按队列深度）、优雅停机
MCP：退款内部工具走 Streamable HTTP（非 stdio），独立凭据
```

### 6.2 发布（10 篇落地）

```text
第一次真实发布：prompt v2（新增"订单状态冲突"处理规则）
  shadow（0%）：影子流量对比 v1/v2 输出 → 分数持平
  canary（5%）：在线 eval + 成本 + 拒绝率监控 → 无异常
  promote（100%）：旧版本热备 24h
版本快照：trace 记录 (prompt v2, deepseek-chat, index v3, toolset v4)
回滚演练：季度一次（label 回指 v1 + 缓存失效验证）
```

## 7. 阶段 5：上线后治理

```text
SLO：可用性 99.9%、TTFT p95 < 1.5s、工具失败率 < 1%
预算：单请求 $0.05 / 会话 $1 / 日预算 $500（80% 预警、100% 熔断）
成本周报：按 Agent/模型/路径归因 → 发现"闲聊吃 40% 成本"→ 路由优化
质量闭环：在线采样 5% → 失败样本回流 golden set（上线 2 周 +23 例）
供应商预案：deepseek 故障 → fallback 备用 → 高影响退款转人工队列
漂移告警：eval 分数 3pp 下滑 → 自动回滚 + 告警
```

**上线后两周真实数据**（示意）：

| 指标 | 上线前（demo 估） | 上线 2 周 | 主要杠杆 |
|------|:---:|:---:|---------|
| 查询 TTFT p95 | ~3s | ~800ms | 缓存 + 缓存感知路由 |
| 每会话成本 | $0.18 | $0.06 | 缓存 + 路由 + 预算 |
| 人工介入率 | 30% | 12% | 审批门 + 错误回传优化 |
| 退款事故 | 潜在（无审计） | 0（全审批留痕） | 审批门 + 幂等 |

## 8. 关键决策复盘表

| 决策 | 选择 | 为什么 | 代价 |
|------|------|--------|------|
| 同步/流式/异步并存 | 三形态 | 短任务快、长任务不阻塞、过程可见 | 接口面变宽 |
| 退款走审批门 | 确定性代码 + HITL | 资金操作零容忍（LLM06） | 12% 单需人工 |
| 双 Provider | OpenAI + DeepSeek | 降本 + fallback 冗余 | 双份适配测试 |
| 统计门禁 | t-test + 效果量 | 固定阈值误报多 | 需 100+ 样本/路由 |
| 缓存感知路由 | 前缀哈希 | 盲轮询命中率跌至 8% | 实例需共享 KV 状态 |
| 语义缓存限 60s TTL | 短 TTL | 订单状态会过期（不能返回旧状态） | 命中率受限于 TTL |
| 先做可观测再部署 | trace 先行 | 上线即瞎 = 事故 | 多花 1-2 天埋点 |

## 9. 验收检查单

```text
□ 三形态接口全部可用（同步/SSE/队列+回放），幂等键生效
□ 审批门覆盖所有写操作，审计 approved_by 齐全
□ trace 贯通：prompt hash + 版本快照 + 成本 + 工具结果
□ 评估门禁跑在每次发布前（120 例 + 统计回归）
□ 缓存命中率 > 60%，预算三级生效
□ 金丝雀发布 + 漂移自动回滚演练通过
□ 供应商 fallback 演练通过（季度）
□ 脱敏巡检通过，无 PII 进 trace/日志
□ 成本周报闭环：归因 → 优化 → 复盘
```

---

## 【参考来源】

- 本案例全部决策细节对应本模块各篇章；关键数据来源：
- [viqus.ai: Async Agent Architectures（异步形态决策）](https://viqus.ai/blog/async-agent-architectures-queue-vs-streaming)
- [Atlas research（2026-06）: Evals in CI/CD（统计门禁参数）](https://github.com/Laoujin/Atlas/blob/main/research/2026-06-04-evals-how-do-you-know-your-ai-works-session-blueprint/evals-in-ci-cd/index.md)
- [futureagi.com: CI/CD for AI Agents in 2026（发布流程）](https://futureagi.com/blog/ci-cd-for-ai-agents-best-practices-2026/)
- [Mindflow: 9 Engineering Practices（混合架构与治理）](https://mindflow.io/blog/the-production-ai-agent-reality-check-9-engineering-practices-that-actually-work)
- 排错与调优实战：见 [Agent 调优、排错与安全模块 09 篇](../Agent%20调优、排错与安全模块（生产必备）/09-实战案例库.md)

---

**返回总览**：[00-总览：Agent 工程化与部署模块（从 demo 到可用应用）](00-总览：Agent%20工程化与部署模块（从%20demo%20到可用应用）.md)

**下一模块**：[13-面试与自测](13-面试与自测.md)
