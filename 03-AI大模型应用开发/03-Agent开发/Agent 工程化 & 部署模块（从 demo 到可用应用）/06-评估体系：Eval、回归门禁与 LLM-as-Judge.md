# 06 评估体系：Eval、回归门禁与 LLM-as-Judge

> 定位：证明"Agent 没变笨"的量化体系——golden set、三层级联门禁、统计回归、在线采样（2026-08 基准）

## 📚 目录

1. [Eval-driven development：评估即规范](#1-eval-driven-development评估即规范)
2. [评估数据集：golden set 的构建](#2-评估数据集golden-set-的构建)
3. [三层级联门禁：便宜的先判](#3-三层级联门禁便宜的先判)
4. [LLM-as-Judge：用法与失败模式](#4-llm-as-judge用法与失败模式)
5. [统计回归门禁：替代固定阈值](#5-统计回归门禁替代固定阈值)
6. [Agent 评估：轨迹才是评估单元](#6-agent-评估轨迹才是评估单元)
7. [在线评估与离线评估](#7-在线评估与离线评估)
8. [工具与平台选型](#8-工具与平台选型)
9. [落地检查单](#9-落地检查单)

## 1. Eval-driven development：评估即规范

```text
2026 主流工作流（EDD）：
  1. eval 套件是工作规范，分数是发布判决（shipping oracle）
  2. 维护版本化 golden set 作为回归基线
  3. 每次改 prompt/模型后，跑套件、读分数增量（分钟级）
  4. 生产 trace 里浮现的新边界案例 → 回流进 golden set

一句话：没有 eval 门禁的发布 = 拿用户当测试集
```

> 🎯 **核心价值**：模型升级、prompt 修改、工具变更——任何一次变更都是"质量风险事件"。评估体系把"凭感觉"变成"读分数"，把"上线后发现"变成"发布前拦截"。

## 2. 评估数据集：golden set 的构建

| 项 | 要求 | 说明 |
|----|------|------|
| 规模 | 50-100 题起步；统计门禁用 100-200 例/路由 | 30 例的置信区间约 ±0.07，无法区分信号与噪声 |
| 来源 | 真实会话脱敏 + 覆盖边界案例 | 生产 trace 回流是主要扩充来源 |
| 标注 | 每题带参考答案 + 判定标准 | 人标一次，机器复用 |
| 分层 | 按路由/能力分桶（查询/退款/闲聊…） | 聚合分数会掩盖单桶恶化（0.85 平均可藏 0.62 的桶） |
| 版本 | golden set 版本化，与 prompt 版本绑定 | 基线漂移可追溯 |

**黄金样本陷阱**：
- 只放"漂亮案例"→ 测不出真实分布
- 不放边界（拒绝类、敏感类、多意图类）→ 上线炸的都是边界
- 用例放得太少 → 统计上无法判定

## 3. 三层级联门禁：便宜的先判

```text
第 1 层：确定性检查（sub-ms，$0）
   JSON schema 校验、正则、精确匹配、工具参数合法性
   → 能拦截 30-60% 的生产失败，先行闸门

第 2 层：NLI 分类器（10-50ms，~$0.001/次）
   faithfulness / 主张支撑 判定
   → 为第 3 层筛出"确实模糊"的样本

第 3 层：Frontier LLM-as-Judge（$0.05-0.5/次，100ms-3s）
   只处理残差模糊样本
   → 控制成本：10000 例/天约 $150-500/月

顺序不可颠倒：第 1 层不过不进第 2 层——用最便宜的手段干最多的活
```

> ⚠️ **成本警示**：naive 的"全量 judge 当 PR 门禁"在 5000 例数据集上每次跑要烧 $250-2500——这就是三层级联存在的理由。

## 4. LLM-as-Judge：用法与失败模式

### 4.1 用法

- 默认评分器（LangSmith/Braintrust/Phoenix/DeepEval 一致）
- 按 rubric 打分：正确性、忠实性、工具使用恰当性、回复风格
- 跑在离线样本上做批量评分

### 4.2 失败模式（必须设计规避）

| 失败模式 | 现象 | 规避 |
|---------|------|------|
| 长度偏见 | 系统性地给长输出更高分 | rubric 明示"简洁优先"；按长度分层抽样 |
| 成本/延迟 | judge 调用 5-50¢、100ms-3s | 三层级联只让模糊样本进第 3 层 |
| 方差 | 同输入多次判定不同——CI 里天然 flaky | 设计方差：judge 温度=0、borderline 重跑 3 次取多数、敏感判决才 ensemble、分数滑动平滑 |
| 校准缺失 | judge 与人类判断不一致 | 上线前做 judge-vs-human 一致性验证（kappa），不一致则换 rubric/换 judge 模型 |

> 🎯 **2026 共识**：不假装 judge 是确定性的，而是**为方差设计**——控制温度、边界重跑、集成就高影响决策、先校准再信任。

## 5. 统计回归门禁：替代固定阈值

```text
传统做法："分数 < 0.8 就失败"——固定阈值对噪声敏感，改个无关 prompt 就误报

2026 做法：统计回归门禁
  判失败需同时满足：
  1. 均值下降（delta < 0）
  2. 统计显著（Welch t-test，p < 0.05，对比 per-example 分数数组）
  3. 效果量达标（|delta| >= 0.03，超过噪音底线）

CI 退出码契约：0=通过可合并 / 2=硬失败 / 3=警告(发 Slack) / 6=API 错误(重试)
```

| 原则 | 内容 |
|------|------|
| 样本量 | 100-200 例/路由才能让 t-test 分离信号与方差 |
| 滚动基线 | 基线存 git，与代码同步演进 |
| 分级反应 | 确定性失败/安全用例失败 → 硬阻断；噪声带内的单次下滑 → 警告 |
| 防污染 | judge 判定过的样本标注版本，防新版本误判污染基线 |

## 6. Agent 评估：轨迹才是评估单元

```text
LLM 评估：输入-输出 单点评分
Agent 评估：轨迹评分——工具选择序列、参数正确性、结果利用、错误恢复

单位是 trace，不是 response。评分最终答案会漏掉：
  12 步才绕对的轨迹（正确结果掩盖低效路径）
  工具参数提取 0.62 分被平均分数掩盖
```

| 轨迹指标 | 内容 |
|---------|------|
| 工具选择准确率 | 该用的工具选对没 |
| 参数正确性 | 工具参数提取是否正确（如日期/金额） |
| 结果利用 | 工具结果有没有被真正用于后续推理 |
| 错误恢复 | 工具失败后能否自救（结构化错误码回传） |
| 效率 | 是否 4 步能完成的走了 12 步 |

**执行验证（最硬的标准）**：验证端状态而非最后一条消息——tau-bench 检查数据库状态、SWE-Bench 跑测试套件。你的 Agent 也应有"执行后状态断言"（如退款后查数据库确认只退了一次）。

## 7. 在线评估与离线评估

| 维度 | 离线评估 | 在线评估 |
|------|---------|---------|
| 位置 | CI/发布门禁 | 生产流量采样 |
| 数据 | golden set（可复现） | 实时用户请求（不可复现） |
| 评分器 | 三层级联全用 | 确定性 scorer（<50ms）+ 约 5% 深判采样 |
| 用途 | 变更门禁 | 漂移检测、金丝雀自动回滚信号 |
| 反馈 | 失败回流数据集 | 触发告警/回滚/更新用例 |

> 💡 **金丝雀闭环**：canary 版本在线分数持续 2-3pp 下滑 → 自动回滚（10 篇）。在线与离线必须跑**同一套 judge 与 rubric**，版本间对比才有意义。

## 8. 工具与平台选型

| 工具 | 定位 | 亮点 | 注意 |
|------|------|------|------|
| [DeepEval](https://github.com/confident-ai/deepeval) | 开源"pytest for LLMs" | `assert_test()` 断言、G-Eval/faithfulness/工具正确性等 14+ 指标、与 pytest 栈全兼容 | Python only；CI 是 system of record 时首选 |
| Braintrust | eval 优先 SaaS | autoevals 打分库、数据集版本管理、A/B 显著对比、PR Action 阻断 | 托管为主（$249/月 百人团队）；trace 次之 |
| Promptfoo | 声明式 CLI/YAML | 每次 PR 跑 before-vs-after、缓存 LLM 调用、红队测试 | 配置式，重模板 |
| Langfuse | 开源可自托管 | 观察 + eval + 数据集一体（ClickHouse 收购后仍 MIT） | eval 深度弱于前两者 |
| LangSmith | LangChain 生态 | 与 LangGraph 无缝、pytest/Vitest 集成 | 绑定框架；inform 不强制阻断 |

**选型原则**：CI 是 system of record → DeepEval；UI 是产品（PM 要看结果）→ Braintrust；要开源自托管 → Langfuse。多数成熟团队跑多个：CI 判分数、生产看 trace、网关管限制——同一套 judge 三处复用。

> ⚠️ **押注单一闭源供应商的风险**：Humanloop 被 Anthropic 收购团队后关停（2026）——评估数据与工具链要能导出迁移，或选开源为底座。

## 9. 落地检查单

```text
□ golden set ≥100 例/路由，版本化，含边界案例
□ 三层级联门禁已实现（确定性 → NLI → judge）
□ judge 与人工校准过（一致性验证）
□ 统计回归门禁生效（t-test + 效果量 + 滚动基线）
□ 轨迹评估已覆盖（工具选择/参数/结果利用/错误恢复）
□ 在线采样评估接入 trace（5% 深判 + 确定性快判）
□ 金丝雀回滚信号已配置（2-3pp 漂移自动回滚）
□ 评估数据可导出迁移
```

---

## 【参考来源】

- [Atlas research（2026-06）: Evals in CI/CD（统计回归门禁、三层级联、退出码契约）](https://github.com/Laoujin/Atlas/blob/main/research/2026-06-04-evals-how-do-you-know-your-ai-works-session-blueprint/evals-in-ci-cd/index.md)
- [Atlas research（2026-06）: Eval Framework Landscape 2026（工具矩阵）](https://github.com/Laoujin/Atlas/blob/main/research/2026-06-09-evals-vibes-don-t-scale-a-complete-technical-session-blueprint/eval-framework-landscape-2026/index.md)
- [respan.ai: Best LLM Evaluation Tools in 2026](https://www.respan.ai/articles/best-llm-evaluation-tools)
- [futureagi.com: Agent Evaluation Frameworks in 2026](https://futureagi.com/blog/agent-evaluation-frameworks-2026/)
- [futureagi.com: CI/CD for AI Agents in 2026](https://futureagi.com/blog/ci-cd-for-ai-agents-best-practices-2026/)
- [MLflow: Building Production-Ready AI Agents in 2026（golden set 与 RAGAS 指标）](https://mlflow.org/articles/building-production-ready-ai-agents-in-2026/)
- [DeepEval（GitHub）](https://github.com/confident-ai/deepeval)

---

**返回总览**：[00-总览：Agent 工程化与部署模块（从 demo 到可用应用）](00-总览：Agent%20工程化与部署模块（从%20demo%20到可用应用）.md)

**下一模块**：[07-性能与成本：Prompt 缓存、并发与流式](07-性能与成本：Prompt%20缓存、并发与流式.md)
