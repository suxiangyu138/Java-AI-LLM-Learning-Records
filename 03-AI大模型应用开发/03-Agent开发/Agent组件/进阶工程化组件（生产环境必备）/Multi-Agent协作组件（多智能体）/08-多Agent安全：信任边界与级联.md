# 多 Agent 安全：信任边界与级联

> 多 Agent 安全 = 2026 最严峻的 Agent 安全战场：**级联失败是主导威胁**（~12% 长运行含单步评估从不标记的传播错误）；**每次交接都是信任边界**，下游很少再验证；协议（A2A/handoff）让级联"结构性更容易"。本章给多 Agent 威胁模型与防御完整设计。

## 1. 级联失败：定义与实证

| 项 | 说明 |
|---|---|
| 定义 | 一个上游错误经下游 Agent/步骤传播放大——比任何单点错误更糟 |
| 典型链 | 规划器幻觉工具名 → 执行器调不存在端点 → 恢复 Agent 编造合理辩解 → 汇总 Agent 报成功——**每步都合理，整条轨迹是虚构** |
| 数据 | **~12% 长运行含传播错误，单步评估从不标记**（FutureAGI 2026） |
| 案例 | 退款请求被当欺诈调查：入站 Agent 因邮件签名注入误分类 → 路由信任 → 欺诈队列 → 下游放大——**无单步 eval 能抓到** |

> 🎯 核心要点：**级联的本质是"错误被信任传播"**——交接点下游很少再验证；协议化交接（A2A）让级联"结构性更容易"（每个交接都是不再验证的信任边界）。

## 2. 攻击技术目录（2026）

| 技术 | 机制 | 数据 |
|---|---|---|
| ACI（Agent Cascading Injection） | 被攻破 Agent 借互信传播恶意指令 | ACIArena 基准 1356 用例（ACL 2026） |
| Tool-chaining | 单个都授权、组合越权 | **91% of 847 部署易感** |
| 内存毒化 | 单次毒化写污染所有读共享内存者 | **94% 跨会话记忆 Agent 易感**（~84-95% 注入成功率） |
| Morris-II 蠕虫 | 零点击提示词级联传播，输出被当下一输入 | OWASP ASI08 |
| 假完成报告 | 报完成但系统状态矛盾 | 执行验证防御 |
| 身份伪装 | 多用户环境无法区分授权/未授权来源 | 身份管理 |
| 语义绕过 | "分享"拒绝但"转发"同意（关键词依赖非概念依赖） | Agents of Chaos 实证（2026-02） |

> ⚠️ **Agents of Chaos 核心洞察（38 研究者，2026-02）**：**"局部对齐 ≠ 全局稳定"**——单独对齐的 Agent 部署在一起产生系统性失败；"转发 vs 分享"案例暴露安全训练是关键词依赖而非概念依赖。

## 3. 五结构缺陷（D3 Security，多 Agent 固有）

| 缺陷 | 机制 |
|---|---|
| 协调延迟 | 每次交接序列化/传输/反序列化——4000 告警/天时累积延迟主导推理时间 |
| 上下文碎片 | 每次交接是上下文丢失事件——下游消费第三代摘要 |
| 幻觉传播 | 一个 Agent 的捏造被下游当 ground truth"洗白"成确认证据 |
| API drift | 独立集成维护负担随 Agent 数倍增 |
| 审计碎片 | 事故记录需从 N 个 Agent 日志重建 |

> 🎯 核心要点：**五缺陷是架构性质不是可修补 bug**——只能设计规避（DAG 约束、状态高保真、共享审计平面）；"多 Agent 一定更好"的团队在五缺陷上摔跤。

## 4. 官方风险框架（NCSC/Five Eyes 2026-05）

| 风险 | 场景 |
|---|---|
| 权限提升 | 被攻破采购 Agent 用过度权限改合同批付款，无人发现 |
| 不安全工具集成 | 第三方工具被攻破 |
| 流氓 Agent | 群内恶意 Agent 被当可信对等 |
| 级联系统失败 | "单一编排缺陷或第三方工具被攻破可经自主委派与隐式互信引发下游大范围破坏" |
| 问责缺口 | 无明确责任主体 |

> 💡 NCSC 缓解建议：最小权限 + 纵深防御 + 强身份管理 + 持续监控 + 限制低风险任务 + **假设 Agent"可能意外行为"**。

## 5. 防御架构：六道防线

| 防线 | 机制 |
|---|---|
| 零信任交接 | 每个 A2A 调用单独认证授权（mTLS+OAuth2，03 篇）——内部流量当外部 |
| 运行时可干预 | **监控不够，必须能在运行时干预**——治理在决策转化为动作处（工具/权限/记忆/企业系统） |
| 加密信任 | 签名/验证/拜占庭容错的共享内存写——防内存毒化、界定级联（OWASP ASI06/07/08/10） |
| 断路器监控 | 轻量小模型盯"agent tennis"（争论循环）/停滞/"礼貌螺旋"（语言签名） |
| 分级事件响应 | 隔离（撤销写权限/只读沙箱）→ 快照全状态 → 回滚 system prompt/工具定义 |
| 最小权限 + JIT | 每 Agent 只持任务所需权限（Guardrails 09 篇） |

> ⚠️ 2026 金句：**"监控本身不够，若系统无法在运行时干预"**——多 Agent 防御的关键在"执行边界"（决策变成动作的地方）设闸；遥测（观测 09 篇）+ 干预（本防线）双管齐下。

## 6. 与 Guardrails 体系的分工

| 体系 | 职责 |
|---|---|
| Guardrails（安全护栏） | 单点拦截：输入/输出/工具调用校验（五 rail） |
| 本体系 08 | 信任边界：交接认证、级联检测、共享内存保护 |
| 观测 09 | 轨迹证据：级联检测信号、分布式追踪 |

> 🎯 核心要点：**单点护栏挡不住级联**——"每个 Agent 的输入输出都干净"仍然可能整条轨迹是虚构；级联防御必须加"交接信任边界"层（本体系）+"轨迹级检测"层（观测 09）。

## 7. 常见误区

| 误区 | 真相 |
|---|---|
| "每 Agent 安全=系统安全" | 局部对齐≠全局稳定——级联在交接点发生 |
| "内部流量可信" | 零信任——内部当外部 |
| "监控够了" | 必须运行时可干预——执行边界设闸 |
| "交接后下游会验证" | 交接是最小再验证处——下游很少重验 |
| "共享内存方便" | 单次毒化写污染全体（94%）——加密签名写 |
| "工具单个授权就行" | tool-chaining 91%——组合越权 |
| "单步 eval 够" | 12% 传播错误单步不标记——轨迹级评估 |

## 8. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 级联失败？ | 上游错误经信任交接放大——12% 长运行含单步不标记错误 |
| 级联案例？ | 退款→欺诈：注入误分类→路由信任→放大——无单步 eval 能抓 |
| ACI？ | 被攻破 Agent 借互信传播恶意指令（ACIArena 1356 用例） |
| Tool-chaining？ | 单授权组合越权——91% of 847 部署 |
| 内存毒化？ | 单次写污染全体——94% 跨会话 Agent 易感 |
| Morris-II？ | 零点击蠕虫级联——OWASP ASI08 |
| 五结构缺陷？ | 协调延迟/上下文碎片/幻觉传播/API drift/审计碎片 |
| 局部对齐≠？ | 全局稳定——Agents of Chaos 核心洞察 |
| 六道防线？ | 零信任交接/运行时可干预/加密信任/断路器/分级 IR/最小权限 |
| 2026 金句？ | 监控不够，若无法运行时干预 |
| 与护栏分工？ | 护栏单点拦截，本体系信任边界 |
| 级联检测？ | 轨迹级评估——观测 09 篇 |

---

**下一模块**：[09-多 Agent 评估与观测](09-多Agent评估与观测.md)　**返回总览**：[00-Multi-Agent 协作组件总览](00-Multi-Agent协作组件总览.md)

## 参考来源

- [ACIArena: Toward Unified Evaluation for Agent Cascading Injection（ACL 2026）](https://aclanthology.org/2026.acl-long.457/)
- [What Is a Cascading Failure?（FutureAGI）](https://futureagi.com/glossary/cascading-failure/)
- [Agents of Chaos: What Happens When Autonomous AI Agents Get Real Tools（NYU Shanghai）](http://rits.shanghai.nyu.edu/ai/agents-of-chaos-what-happens-when-autonomous-ai-agents-get-real-tools/)
- [NCSC and Five Eyes warn over 'agentic AI' risks（Reseller）](https://www.reseller.co.nz/article/4168114/ncsc-and-five-eyes-cyber-agencies-warn-channel-partners-over-agentic-ai-risks-report.html)
- [5 Architectural Flaws in Agentic AI SOC Platforms（D3 Security）](https://d3security.com/resources/5-architectural-flaws-agentic-ai-soc/)
- [When AI Agents Collide: Orchestration Failure Playbook（NASSCOM）](https://community.nasscom.in/communities/ai/when-ai-agents-collide-multi-agent-orchestration-failure-playbook-2026)
