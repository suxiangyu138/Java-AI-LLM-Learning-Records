# 06 - Spring 生态面试题集锦

> 🎯 生态层专属面试题 — 版本兼容、Boot 4 新特性、选型决策、Spring AI 集成。区别于 06-Spring全家桶附录D（原理题），本章聚焦"全景视野"型问题

---

## 目录

1. [生态版图与版本题](#1-生态版图与版本题)
2. [Boot 4 新特性题](#2-boot-4-新特性题)
3. [选型决策题](#3-选型决策题)
4. [Spring AI 题](#4-spring-ai-题)
5. [高频追问链](#5-高频追问链)

---

## 1. 生态版图与版本题

### Q1. 说说 Spring 生态的完整版图？

**参考答案（分层表达）：**

```text
核心层：Spring Framework（IoC/AOP/事务）→ MVC / WebFlux
开发层：Spring Boot（自动配置 + Starter）
数据层：Spring Data（JPA/Redis/MongoDB/ES）
安全层：Spring Security + Authorization Server
微服务层：Spring Cloud + Spring Cloud Alibaba
AI 层：Spring AI（ChatClient/RAG/Agent/MCP）
周边：Batch / Integration / Modulith / GraphQL / Session / Shell
```

**加分表达：** 主动补一句"2026 年重点：Boot 4 换代 + Modulith 模块化单体 + Spring AI 双轨"。

### Q2. Boot / Framework / Cloud 版本是怎么对应的？

| 记忆点 | 内容 |
|--------|------|
| 对应关系 | Boot 4.0/4.1 ↔ Framework 7.x ↔ Cloud 2026.x |
| Java 基线 | Boot 4 最低 17，推荐 21 |
| Jakarta | Boot 4 = Jakarta EE 11（javax 彻底退出） |
| 支持周期 | Boot 3.5 社区支持 2026.06 截止 |

**追问：** 版本对不上会怎样？→ 运行时 NoSuchMethodError、自动配置失效、CVE 无补丁 — 版本矩阵是项目基建。

### Q3. 你所在项目用什么 Spring 版本？为什么？

**答题要点：** 诚实 + 懂迁移 — 老项目："Boot 2.7/3.x，正按路线图升 3.5 → 4.0，卡点是 XX 库的 javax 依赖"；新项目："Boot 4.1 + Java 21，因为 LTS + 虚拟线程 + 支持周期长"。

---

## 2. Boot 4 新特性题

### Q4. Spring Boot 4 最大的变化是什么？

**参考答案（三个关键词）：**

| 关键词 | 内容 |
|--------|------|
| 基线换代 | Java 17+（推荐 21）、Jakarta EE 11、javax 彻底退出、Undertow 移除 |
| 性能重构 | 自动配置拆 47 模块（启动 -33%、镜像 -19%）、虚拟线程默认、AOT 一等公民（<50ms 启动） |
| 能力整合 | 内置 @Retryable/@ConcurrencyLimit、@HttpServiceClient 替代 Feign、可观测性 2.0、Jackson 3 |

**追问：** 升级到 Boot 4 最大的坑？→ ① javax → jakarta 全局替换 ② 三方库兼容性核查（Jackson 3、Servlet 6.1）③ 配置属性改名 ④ 自动配置模块按需引入。

### Q5. @HttpServiceClient 和 Feign 比怎么样？

| 对比 | 结论 |
|------|------|
| 性能 | 官方实现优于 Feign（代理/反射更少） |
| AOT | 原生镜像全面支持（Feign 需额外配置） |
| 生态 | Feign 成熟但停更趋缓，官方主推前者 |
| 迁移 | 接口注解 1:1 映射，成本低 |

**加分表达：** "RestTemplate 在 7.1 标记 @Deprecated，新代码统一 RestClient / @HttpServiceClient。"

### Q6. 虚拟线程对 Spring 应用意味着什么？

**参考答案：** Boot 4 中 MVC/WebFlux/@Scheduled/@Async 默认虚拟线程 — 每请求一线程成为可行方案，线程池耗尽问题消失，同步写法获得高并发 IO 能力。

**追问：** 虚拟线程的坑？→ synchronized 内阻塞 IO 会钉住载体线程（用 ReentrantLock）；ThreadLocal 在百万级虚拟线程下开销放大（考虑 Scoped Values）；CPU 密集无收益。

---

## 3. 选型决策题

### Q7. 新项目你会用 MVC 还是 WebFlux？为什么？

**参考答案：** 默认 **MVC + 虚拟线程** — 虚拟线程解决了 MVC 的并发瓶颈，保留命令式可读性 + 完整生态；WebFlux 仅在"极端 IO 场景 + 团队熟练响应式"时考虑。

**追问：** 什么业务真的需要 WebFlux？→ 网关（Netty 事件循环）、超高连接数长连接、流式数据处理 — 都是少数场景。

### Q8. 单体还是微服务？模块化单体是什么？

**参考答案：**

```text
新项目默认模块化单体（Spring Modulith）：
  ✅ 模块边界编译期强校验（不能跨模块私有访问）
  ✅ 单体部署（运维简单）+ 预留拆分边界
  ✅ 规模到了再拆微服务，边界现成
拆微服务的信号：
  多个独立交付团队 / 资源需求差异大 / 故障隔离硬需求 / 全量回归成本失控
```

### Q9. 你们项目为什么用 MyBatis 而不用 JPA？

**参考答案（国内主流语境）：**

```text
① SQL 可控：复杂查询/性能优化直接操作 SQL（互联网业务核心诉求）
② 动态 SQL 强大：XML 标签 + 条件构造器
③ MyBatis-Plus：CRUD/分页/逻辑删除开箱即用，开发效率补足
④ 团队习惯：国内从业者普遍 MyBatis 经验
例外：DDD 领域建模、表结构简单、团队 JPA 熟练 → 可 JPA
```

### Q10. Spring Cloud 官方和 Alibaba 组件怎么选？

**参考答案：** 国内业务"官方骨架 + Alibaba 组件"混搭：Nacos（注册/配置）+ Sentinel（熔断限流）+ Gateway（官方）+ HttpServiceClient（调用）+ Seata（事务）。

**追问：** 为什么 Nacos 是事实标准？→ 注册+配置一体化、中文文档、阿里生产验证、K8s 支持成熟。

---

## 4. Spring AI 题

### Q11. Spring AI 是什么？和 LangChain4j 怎么选？

**参考答案：** Spring AI 是 Spring 官方 AI 应用框架 — ChatClient 统一模型 API（20+ 模型）、RAG（20+ 向量库）、Agent（5 种工作流）、MCP（官方 Java SDK 提供方）。选型：Spring 栈内团队 → Spring AI；需要 LangChain 生态兼容 → LangChain4j。

**追问：** Spring AI 版本怎么对应？→ 1.0/1.1 适配 Boot 3（Java 17）；2.0 适配 Boot 4（Java 21+）— 新项目选 2.x 线。

### Q12. 用 Spring AI 实现一个 RAG 问答怎么设计？

**参考答案（分步）：**

```text
① 数据接入：DocumentReader（文件/网页/DB）→ 分块 → 嵌入
② 存储：VectorStore（pgvector/Redis/Milvus）
③ 检索：QuestionAnswerAdvisor 自动检索注入上下文
④ 生成：ChatClient + Advisor 管道
⑤ 评估：RelevancyEvaluator 持续回归
工程要点：分块策略、嵌入维度匹配、知识更新、Token 成本监控
```

### Q13. MCP 是什么？Spring AI 和 MCP 什么关系？

**参考答案：** MCP 是模型与工具之间的标准化协议 — 工具方实现一次，任意 AI 应用可接入。Spring AI 团队是 **MCP 协议 Java SDK 官方提供方**：starter-mcp-client 连工具、starter-mcp-server 暴露工具（@Tool）、McpFunctionCallback 注入 ChatClient。

**追问：** 2.0 里 MCP 有什么变化？→ Streamable HTTP 取代 SSE 成为默认服务端协议。

---

## 5. 高频追问链

### 5.1 追问题：Spring Boot 一条链

```text
Q1: Boot 核心机制？          → 自动配置 + Starter + 外部化配置 + Actuator
Q2: 自动配置怎么实现的？      → @EnableAutoConfiguration + 条件注解（@ConditionalOnClass）
Q3: 和 Framework 什么关系？  → Boot 依赖 Framework，封装开箱即用
Q4: Boot 3 和 4 的区别？     → 基线（Jakarta EE 11）+ 模块化 + 虚拟线程 + AOT
Q5: 升级迁移路线？           → 3.5.x 清弃用 → Java 21 → 4.0 → 适配模块/配置 → 可选原生镜像
```

### 5.2 追问题：Spring AI 一条链

```text
Q1: AI 应用怎么接入模型？    → ChatClient（统一 API）
Q2: 怎么让它基于公司文档回答？→ RAG（ETL + VectorStore + Advisor）
Q3: 怎么让它调用业务方法？   → @Tool 工具调用（或 MCP）
Q4: 多个模型怎么切换？       → 改配置（starter 依赖 + model 配置）
Q5: 怎么评估回答质量？       → RelevancyEvaluator / FactCheckingEvaluator + 回归集
Q6: 生产注意什么？          → 成本（Token）、延迟、隐私、可观测性
```

### 5.3 追问题：选型一条链

```text
Q1: 单体还是微服务？        → 模块化单体起步
Q2: 什么时候拆？            → 团队/资源/隔离/回归 四个信号
Q3: 微服务注册用啥？        → Nacos
Q4: 服务间调用用啥？        → HttpServiceClient（Boot 4）/ OpenFeign（存量）
Q5: 熔断限流？             → Sentinel / 内置 @ConcurrencyLimit
Q6: 分布式事务？            → Seata（按业务评估必要性！）
```

---

> 🎯 **核心要点**：生态层面试的差异化 = **"知道 2026 年的 Spring 长什么样"** — Boot 4 已 GA、3.5 支持到期、Spring AI 双轨、Feign 被官方替代。把这些趋势带进答案，就是"持续关注生态"的最佳证明。

**返回总览**：[00-Spring生态知识体系总览](00-Spring生态知识体系总览.md)
