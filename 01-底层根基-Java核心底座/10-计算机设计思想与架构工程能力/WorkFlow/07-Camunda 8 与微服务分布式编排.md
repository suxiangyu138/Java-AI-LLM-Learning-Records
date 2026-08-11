# 07-Camunda 8 与微服务分布式编排：Zeebe 内核与 Saga

> 定位：Camunda 8 不是 Camunda 7 的升级版，而是以 Zeebe 为核心的**分布式编排基础设施**——流程状态从应用数据库挪进独立集群的分区日志，业务代码退居外部 Job Worker。本篇讲清架构、7 代迁移、Saga 结合与成本现实。

## 1. Zeebe 架构全景

```text
客户端(Java/JS/Go) → Gateway(路由/REST-gRPC) → Broker 集群
                                              ├── Partition 1(流程分片, Raft 主从复制)
                                              ├── Partition N
                                              └── Exporter → Operate(监控) / Tasklist(人工任务)
业务逻辑：Job Worker(任意语言, 长轮询取任务) ←── 引擎只存状态, 不执行业务
```

- **分区（Partition）**：流程实例按 key 哈希分片，每分区一份 Raft 日志做主从复制——引擎的高可用与水平扩展来自这里。
- **Job Worker 外置**：业务代码不进引擎，Worker 长轮询领取 Job、执行、报成功/失败——跨语言、可独立扩缩容，这是与嵌入式引擎最大的思维转换（开发者不再写 `TaskService.complete`，而是写 Worker 循环）。
- **查询与运维分离**：运行时状态在分区日志，查询视图（Operate/Tasklist）由 Exporter 异步导出——保证写路径高吞吐，代价是视图有秒级延迟。
- **8.9 起**：PostgreSQL、MySQL、Oracle、SQL Server、Aurora 等关系型数据库成为二级存储一等选项，但主执行存储仍是 Raft + RocksDB。

> ⚠️ 架构真相：Camunda 8 的"流程实例修改/迁移"能力存在活跃元素类型、并发状态与映射约束，直接包装成无限制业务跳转（中国式自由跳转）风险很高——分布式引擎擅长确定性的长期运行编排，不擅长高频动态改流。

### 1.1 Worker 开发与消息关联

```java
// Java Worker：长轮询取 Job、执行业务、报结果（任意语言同构）
ZeebeClient client = ZeebeClient.newClientBuilder()
    .gatewayAddress("localhost:26500").build();
client.newWorker().jobType("send-email")
    .handler((job, ctx) -> {
        Map<String, Object> vars = job.getVariablesAsMap();
        emailService.send((String) vars.get("to"), (String) vars.get("content"));
        ctx.newCompleteCommand(job.getKey()).send();   // 成功
        // 失败：ctx.newFailCommand(job.getKey()).retries(0).errorMessage(...).send();
    }).open();
```

- **幂等铁律**：Worker 执行必须幂等——Job 超时未确认会被其他 Worker 重新领取，重复执行是常态不是事故。
- **消息关联（Message Correlation）**：流程等待外部事件（支付回调、审核结果）时用消息事件，客户端发消息按 `correlationKey`（业务键）路由到具体实例——比轮询省资源、实时性好，是分布式引擎替代定时器的首选。
- **定时器仍在但语义不同**：分布式下的定时器由引擎集群统一调度，精度与规模优于嵌入式 JobExecutor，但"等待外部结果"场景优先消息事件。

### 1.2 部署拓扑参考

- 生产最小集：Zeebe 集群（3 Broker，Raft 保证多数派）+ Operate + Tasklist + Identity（8.8 起收敛为 Orchestration Cluster）。
- 官方推荐 K8s + Helm 部署，支持跨可用区；开发/演示可用 Docker Compose 单机（`camunda/camunda:8.9` 全家桶镜像）。
- 高吞吐调优维度：分区数（默认 1，按吞吐预估扩）、RocksDB 内存、Exporter 消费能力——写路径性能瓶颈通常在 Exporter 拖慢，8.9 引入 RDBMS 二级存储后查询压力可从集群剥离。

## 2. Camunda 7 存量迁移

- **7.24 为终版，CE 已于 2025-10 EOL**，仓库归档；企业版支持延长至 2030。
- 存量替代：**Operaton 2.1.3**（社区驱动的 7 延续分支）与 **CIB seven Community 2.2.0** 可承接存量资产。
- 迁 8 不是 Maven 升级：流程模型、Java Delegate、事务边界、历史查询、运维与数据迁移全部重设计——迁移决策按"3-5 年 TCO 对比"做，别为版本号而迁。

**迁移评估五问**：其一，存量流程是否以人工审批为主（7 的强项，8 的弱项）？是则优先考虑 Operaton 延续而非迁 8。其二，是否有跨语言/高吞吐诉求（8 的强项）？无则迁移收益为零。其三，团队是否具备 Zeebe 集群运维能力（分区、Raft、Exporter 调优）？其四，生产许可预算是否落实？其五，数据迁移方案是否验证过（历史数据要迁到 8 的查询存储，不是拷表）？五问中超过三个"否"，迁移方案直接否决——这也是 2026 年大量存量项目选择 Operaton 或继续付费企业版 7 的真实原因。

## 3. 微服务编排：Saga 与 Outbox 结合

工作流引擎在微服务体系里干两件事：**跨服务流程编排**与**分布式事务的 Saga 落地**。

- **编排式 Saga**：流程定义即 Saga 蓝图——服务任务调各服务，失败走补偿节点（BPMN 里的服务任务 + 错误边界事件天然表达"正向执行/反向补偿"），引擎负责状态持久化与重试，比手写补偿代码可靠得多。
- **Outbox 模式配套**：业务写库与发消息不同步会丢事件——业务事务内写 outbox 表，投递器异步发 MQ 唤醒引擎（消息启动事件）。工作流引擎 + Outbox 是"本地事务一致性 + 异步编排"的黄金组合。
- **幂等铁律**：Worker 执行必须幂等（重复领取 Job 是常态），配合引擎的 Job 超时与重试语义。
- **编排式 vs 手写补偿的对比**：手写 Saga 的常见形态是"状态枚举 + if-else 判断补偿顺序"，流程每多一个分支，判断复杂度就指数上升，且补偿逻辑与正向逻辑散落各处难以核对。编排式 Saga 把蓝图画在 BPMN 里：正向节点与服务任务一一对应，补偿节点挂在错误边界事件上，引擎负责"执行到哪一步、失败时该补偿哪几个"——开发者只写服务与补偿函数，顺序与状态交给引擎。业界普遍反映，编排式方案的维护成本（改流程）显著低于手写方案（改代码），这是微服务编排场景选择引擎而非自研的根本原因。
- 与事务型方案（Seata AT/TCC）的分工：Saga 适合"跨服务、无强一致、可补偿"的长流程；短事务、强一致场景用 Seata（详见同级《Seata 分布式事务》体系）。
- **Saga 的可靠性与人机兜底**：Saga 最怕的是"补偿链断裂"——某个服务不可达导致补偿失败，流程悬在半空。编排式方案对此有天然优势：补偿节点失败可重试（引擎 Job 语义）、可设置补偿超时、最终可升级为人工干预任务（BPMN 里给补偿路径加用户任务"人工处理补偿失败"）。相比之下手写 Saga 一旦补偿失败只能靠监控发现，处理窗口和手段都差很多——这是评估"要不要上分布式引擎"时最容易被低估的价值点。

## 4. vs Temporal / Conductor（分布式编排三选一）

| 维度 | Camunda 8 | Temporal | Netflix Conductor |
|---|---|---|---|
| 模型 | BPMN 图驱动 | 代码驱动（Workflow 代码即编排） | JSON 工作流定义 |
| 人工任务 | Tasklist 一等公民 | 需自己封装 Human Task | 内置但生态弱 |
| 语言 | 任意（Worker 协议） | Go/Java/Python/TS 为主 | Java（多语言 SDK 有限） |
| 中国式审批 | 弱（无会签/退回语义） | 无 | 无 |
| 定位 | 流程 + 人机混合编排 | 长期运行代码编排 | 微服务任务编排 |

- 有人工审批、要审计可视化 → Camunda 8；纯代码级长期运行任务（ETL、订餐状态机）→ Temporal；已投 Netflix 生态 → Conductor。
- 三者都不是中国式审批的答案——那仍是 Flowable 的地盘（见 09 篇分类）。

## 5. 许可与成本现实

- **8.6 起 Self-Managed 生产使用需要生产许可证**（Zeebe、Operate、Tasklist、Identity、Optimize 均覆盖），Web Modeler 无企业许可有并发用户限制——"开源 ≠ 可自由商用"是 2026 年选型最大认知转变。
- 成本账：集群资源（Broker/Operate/Tasklist 三件套常驻）+ 运维人力 + 许可费，小规模项目可能比自研嵌入式方案贵一个数量级。
- PoC 之前先确认许可边界与三年 TCO，再谈技术选型。

**开源与商业功能的边界要提前划清**：Camunda 8 社区版保留了核心引擎能力（建模、执行、Worker、基础监控），但生产级组件（Operate 完整视图、Identity、多租户、合规功能）的边界随版本调整——PoC 阶段就把"团队真正需要的功能"逐项对着许可文档核对，避免上线半年后发现关键组件需要补商业订阅，届时替换成本已不可逆。这条教训同样适用于所有"开源 + 商业双轨"的中间件选型。

> 🎯 核心要点：Camunda 8 的价值 = 确定性编排 + 高吞吐 + 跨语言 + 运维可视；代价 = 集群成本 + 许可 + 中国式语义缺失。微服务编排用它，审批流用它以外的引擎。

---

**下一模块**：[08-生产运维与性能调优](./08-生产运维与性能调优.md) / **返回总览**：[00-工作流知识体系总览](./00-工作流知识体系总览.md)

**参考来源**：

- [Camunda 8 文档（Zeebe 架构）](https://docs.camunda.io/)
- [Camunda 8 许可变更公告](https://camunda.com/platform/)
- [2026 年 Java 开源工作流引擎选型（博客园）](https://www.cnblogs.com/hibpm/p/22358552)
- [Orchestrating AI Agents with BPMN（QuantumBPM）](https://quantumbpm.com/blog/orchestrating-ai-agents-with-bpmn)
