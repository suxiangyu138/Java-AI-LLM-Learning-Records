# 05 - Spring 技术选型实战指南

> 🎯 技术选型 = 面试高分区 + 架构师基本功。本章给出 2026 年 Spring 生态的"决策树"：框架怎么选、架构怎么定、数据层怎么办、AI 怎么接 — 每个决策带理由

---

## 目录

1. [选型总原则](#1-选型总原则)
2. [Web 层：Spring MVC vs WebFlux](#2-web-层spring-mvc-vs-webflux)
3. [架构形态：单体 vs 微服务 vs 模块化单体](#3-架构形态单体-vs-微服务-vs-模块化单体)
4. [数据层：Spring Data JPA vs MyBatis](#4-数据层spring-data-jpa-vs-mybatis)
5. [微服务组件：官方 vs Alibaba](#5-微服务组件官方-vs-alibaba)
6. [AI 接入：Spring AI vs LangChain4j](#6-ai-接入spring-ai-vs-langchain4j)
7. [新项目脚手架决策模板](#7-新项目脚手架决策模板)
8. [生产落地清单](#8-生产落地清单)

---

## 1. 选型总原则

```text
三个问题先问自己：
① 团队熟悉什么？      （新技术成本 > 技术本身优劣）
② 业务形态是什么？    （规模/并发/交付节奏决定架构）
③ 生态匹配度？       （选型要"顺势"，不逆势创新）
```

| 原则 | 说明 |
|------|------|
| 保守优先 | 无压倒性理由不换主流方案 |
| 生态优先 | 选 Spring 官方或大厂维护组件 |
| 演进优先 | 能演进到微服务的模块化单体 > 一步到位的微服务 |
| 成本透明 | 技术债按"未来 3 年维护成本"评估 |

---

## 2. Web 层：Spring MVC vs WebFlux

| 对比 | Spring MVC（Servlet） | Spring WebFlux（响应式） |
|------|----------------------|--------------------------|
| 模型 | 线程池 + 阻塞 IO | 事件循环 + 非阻塞 |
| 编程模型 | 命令式（好写） | 响应式（Mono/Flux，陡峭学习曲线） |
| 并发上限 | 依赖线程池 | 极高（事件循环） |
| 调试/排障 | 简单（栈清晰） | 难（异步栈） |
| 生态 | 全（老库都兼容） | 部分库需适配 |

**2026 决策：**

```text
✅ 默认选 Spring MVC + 虚拟线程（Boot 4 默认）
   —— 虚拟线程让"同步写法"获得接近响应式的并发能力，
      同时保留 MVC 的可读性和生态
⚠️ 只有当：极端 IO 场景（网关/长连接/流式）+ 团队熟练响应式
   才选 WebFlux
```

**追问：** 虚拟线程出现后 WebFlux 还有存在意义吗？→ 仍有（托管线程模型在超大规模连接下更稳），但**大部分项目的 MVC + 虚拟线程已够用** — 这是 2026 的主流共识。

---

## 3. 架构形态：单体 vs 微服务 vs 模块化单体

| 维度 | 传统单体 | 模块化单体（Modulith） | 微服务 |
|------|----------|------------------------|--------|
| 拆分粒度 | 无（包混乱） | 模块边界 + 公开 API | 独立进程部署 |
| 开发效率 | 高 | 高（模块并行） | 低（跨服务协作） |
| 部署运维 | 简单 | 简单（单体部署） | 复杂（注册/网关/链路） |
| 故障隔离 | 无 | 模块间隔离 | 强隔离 |
| 扩展 | 整体扩 | 整体扩（模块内优化） | 按服务伸缩 |
| 适用 | 小团队/小业务 | **大多数业务（2026 推荐起点）** | 大团队/大业务/多域 |

**2026 决策：**

```text
✅ 起步：模块化单体（Spring Modulith）—— 模块边界在代码里，
   后续按需拆微服务，边界天然
⚠️ 何时拆微服务？
   ① 团队规模 ≥ 3-4 个独立交付组
   ② 模块资源需求差异大（如推荐引擎 vs 简单 CRUD）
   ③ 故障隔离成为硬需求（核心链路不容被拖垮）
   ④ 已经出现"改一行等全量回归"
❌ 不要为了"架构好看"而微服务 —— 分布式事务/链路排查/发布
   复杂度是真实的成本
```

**追问：** Spring Modulith 具体提供什么？→ 模块边界校验（编译期防止跨模块私有访问）、模块内事件（ApplicationEvent 异步解耦）、模块观察（Actuator 端点看模块依赖图）。

---

## 4. 数据层：Spring Data JPA vs MyBatis

| 对比 | Spring Data JPA | MyBatis / MyBatis-Plus |
|------|-----------------|------------------------|
| 风格 | 对象关系映射（ORM，Hibernate） | 半自动 SQL 映射 |
| SQL 控制 | 弱（JPQL/Criteria，复杂 SQL 难优化） | **强（手写 SQL 全控）** |
| 简单 CRUD | 极快（方法名即查询） | 需要写 mapper |
| 性能调优 | 需懂 Hibernate 机制（N+1/一级缓存） | 直接控制 SQL 和索引 |
| 国内主流 | 部分（DDD/领域建模） | **互联网公司主流** |
| 动态 SQL | 复杂（Specification） | 强大（XML 标签/条件构造器） |

**2026 决策：**

```text
✅ 国内互联网业务（重 SQL、高并发）：MyBatis-Plus
   —— SQL 可控 + 代码生成 + 分页/逻辑删除开箱即用
✅ 领域模型驱动（DDD）/ 表结构简单：Spring Data JPA
   —— 实体建模、仓库模式、约定优于配置
⚠️ 混合使用也常见：JPA 做领域主链路 + JdbcTemplate 做复杂查询
```

**追问：** MyBatis-Plus 和原生 MyBatis？→ Plus 是增强封装（CRUD 方法、条件构造器、分页插件），国内新项目基本直接用 Plus。

---

## 5. 微服务组件：官方 vs Alibaba

| 能力 | Spring 官方 | Spring Cloud Alibaba | 决策 |
|------|-------------|----------------------|------|
| 注册/配置 | Consul / Eureka | **Nacos（国内事实标准）** | 国内选 Nacos |
| 熔断限流 | Circuit Breaker 抽象 | **Sentinel（动态规则强）** | 复杂场景选 Sentinel |
| 网关 | Gateway | Gateway（共用） | 官方 Gateway |
| 分布式事务 | — | **Seata** | 需要时选 Seata |
| 远程调用 | **HttpServiceClient（Boot 4）** | OpenFeign（存量） | 新项目官方 |
| 消息 | Cloud Stream | RocketMQ | 按 MQ 选型 |

**追问：** 能不能混搭？→ 能 — 主流实践就是"官方骨架 + Alibaba 组件"：Gateway + HttpServiceClient + Nacos + Sentinel + Seata，按需组合而非整套绑定。

---

## 6. AI 接入：Spring AI vs LangChain4j

| 决策条件 | 推荐 | 理由 |
|----------|------|------|
| 已有 Spring Boot 项目 + 团队会 Spring | **Spring AI** | 生态一致、自动配置、官方 MCP |
| 需要 LangChain 生态/概念参照 | LangChain4j | API 对齐 LangChain，教程多 |
| 国内模型（通义/DeepSeek）+ 百炼平台 | Spring AI Alibaba | 国产模型深度优化 + MultiAgent |
| 本地/私有化模型（Ollama/vLLM） | 两者皆可 | OpenAI 兼容端点都支持 |

**追问：** AI 应用架构的"非框架"决策？→ ① 模型选型（能力/成本/延迟三角）② 向量库（pgvector 起步 → Milvus 规模化）③ 评估体系（回归测试集 + LLM Judge）④ 成本监控（Token 预算）— 框架只是入口，工程问题才是大头。

---

## 7. 新项目脚手架决策模板

```text
场景：2026 年新启动的"Java 后端 + AI 能力"业务系统
─────────────────────────────────────────────
✅ Spring Boot 4.1 + Framework 7.1 + Java 21 LTS
✅ 架构：模块化单体起步（Spring Modulith），预留拆服务边界
✅ Web：Spring MVC + 虚拟线程（默认）
✅ 持久层：MyBatis-Plus（业务库）+ Spring Data Redis
✅ 微服务预留：Nacos + Sentinel + Gateway（规模到了再启）
✅ AI：Spring AI 2.x + RAG（向量库 pgvector 起步）+ MCP
✅ 可观测：starter-opentelemetry + Grafana 栈
✅ 构建：Maven + 可选 GraalVM 原生镜像（Serverless 场景）
✅ 安全：Spring Security 7 + OAuth2（授权服务器独立）
```

---

## 8. 生产落地清单

| 类别 | 检查项 |
|------|--------|
| 版本 | 依赖树无冲突（dependency:tree）、支持周期未过期、CVE 已跟进 |
| 配置 | 配置属性无 deprecated 警告、环境隔离（dev/test/prod） |
| 并发 | 线程池显式配置（不裸用 Executors）、虚拟线程场景避 synchronized 钉住 |
| 数据 | 连接池参数（HikariCP）、慢 SQL 监控、缓存策略明确 |
| 安全 | Security 默认值（密码加密/会话）、依赖审计（npm 同理：audit） |
| 可观测 | health/metrics/链路三件套、日志结构化、告警规则 |
| 部署 | 镜像瘦身（模块化依赖）、优雅停机、滚动发布 |

---

> 🎯 **核心要点**：选型的本质是"用成本换收益的显式决策" — MVC+虚拟线程 > 盲目 WebFlux；模块化单体 > 盲目微服务；MyBatis-Plus 适合国内业务；组件按需混搭。面试答选型题的口诀：**结论 → 三个理由 → 一个例外场景**。

**下一模块**：[06-Spring生态面试题集锦](06-Spring生态面试题集锦.md) / **返回总览**：[00-Spring生态知识体系总览](00-Spring生态知识体系总览.md)
