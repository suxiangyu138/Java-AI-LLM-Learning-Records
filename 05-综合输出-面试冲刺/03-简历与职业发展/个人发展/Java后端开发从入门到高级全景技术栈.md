# Java 后端开发「从入门到高级」全景技术栈

## 概述

本文梳理 Java 后端开发从入门到高级的完整技术栈，按 **10 个阶段 + 时间轴** 组织，覆盖语言基础、数据层、Web 框架、分布式中间件、微服务/云原生、监控可观测、性能高可用、安全、测试质量、持续学习。适用于 2024-2025 年最新技术生态。

---

## 全景路线图

```
阶段 0         阶段 1        阶段 2         阶段 3         阶段 4
语言基础  →   数据层    →  Web 框架层  →  分布式中间件  →  微服务&云原生
  │              │             │              │              │
Java SE       MySQL        Spring 全家桶    MQ/Cache/ES    K8s/Docker
JUC/JVM       Redis        Spring Cloud    任务调度        DevOps/CI/CD
              ES/MongoDB   ORM 高级

阶段 5         阶段 6        阶段 7         阶段 8-10
监控可观测  →  性能高可用 →  安全体系    →  测试质量/持续学习
  │              │             │
Prometheus     JVM 调优      应用/数据安全
ELK/SkyWalking 压测/高可用    OWASP 防护
```

---

## 阶段 0：语言与基础（1~2 个月）

| 模块 | 核心技术点 |
|------|-----------|
| **Java SE** | Java 8/11/17 LTS 语法：Lambda、Stream、Optional、Record、Switch 表达式、var、sealed class |
| **OOP** | 封装/继承/多态、接口默认方法、抽象类 vs 接口 |
| **集合框架** | List/Set/Map 实现类差异、fail-fast vs fail-safe、ConcurrentHashMap、CopyOnWriteArrayList |
| **并发编程** | Thread/Runnable/Callable/Future/CompletableFuture、ForkJoinPool、锁（synchronized/Lock/ReadWriteLock）、CAS、AQS、volatile |
| **IO/NIO** | BIO、NIO、NIO.2、Netty 基础 |
| **JVM** | 内存模型（堆/栈/方法区/元空间）、GC 收集器（G1/ZGC/Shenandoah）、类加载机制、JVM 参数调优 |
| **工具链** | Maven/Gradle（依赖管理/生命周期/插件）、Git 分支模型（Git-Flow、Trunk-Based） |

---

## 阶段 1：数据层（3~4 个月）

### 1.1 关系型数据库

| 技术 | 核心技术点 |
|------|-----------|
| **MySQL** | InnoDB 引擎、索引（B+Tree/Hash/Full-Text）、事务隔离级别、锁（行锁/表锁/意向锁）、MVCC、EXPLAIN 分析、慢查询优化、分库分表（ShardingSphere-JDBC）、主从/组复制 |
| **PostgreSQL** | MVCC 实现差异、JSONB/GIN 索引、窗口函数、CTE、分区表 |
| **持久层** | JDBC、HikariCP 连接池、JPA、Hibernate、MyBatis、MyBatis-Plus |

### 1.2 NoSQL 与缓存

| 技术 | 核心技术点 |
|------|-----------|
| **Redis** | String/Hash/List/Set/ZSet/HyperLogLog/Geo/Stream；持久化（RDB/AOF）、哨兵、Cluster、RedLock、缓存雪崩/穿透/击穿 |
| **MongoDB** | 文档模型、聚合管道、副本集、分片集群 |
| **Elasticsearch** | 倒排索引、DSL 查询、聚合、IK 分词、高亮、集群（Master/Node/Shard） |
| **Neo4j** | Cypher 查询、图算法（最短路径、PageRank） |

### 1.3 数据版本与迁移

| 技术 | 核心要点 |
|------|---------|
| **Schema 迁移** | Flyway / Liquibase 脚本化迁移、回滚策略 |
| **CDC 同步** | Binlog / CDC 实时同步、Canal / Debezium |
| **写入保障** | 幂等写入/更新、乐观锁（version 字段） |

---

## 阶段 2：Web 与框架层（3~4 个月）

### 2.1 Spring 全家桶

| 模块 | 核心技术点 |
|------|-----------|
| **Spring Core** | IoC/DI、BeanFactory、AOP（JDK 动态代理/CGLIB）、事务抽象（声明式/编程式） |
| **Spring Boot** | 自动装配、Starter、Actuator、Profile、外部化配置（YAML/Properties/EnvironmentPostProcessor） |
| **Spring MVC** | DispatcherServlet、HandlerMapping、HandlerAdapter、消息转换器、统一异常处理、文件上传、异步请求（Callable/WebFlux） |
| **Spring Security** | 认证（UsernamePassword/JWT/OAuth2）、授权（RBAC/Method Security）、CSRF/CORS/Session 固定攻击防御 |
| **Spring Data** | JPA、MongoDB、Redis Repository、QueryDSL、Specification |

### 2.2 Spring Cloud 微服务组件

| 组件类型 | 技术选项 |
|---------|---------|
| 注册中心 | Eureka、Consul、Nacos |
| 配置中心 | Spring Cloud Config、Apollo、Nacos |
| 熔断/限流 | Hystrix → Resilience4j、Sentinel |
| 网关 | Spring Cloud Gateway、Zuul |
| 链路追踪 | Sleuth + Zipkin/Jaeger |
| 消息总线 | Spring Cloud Stream（Kafka/Rabbit Binder） |

### 2.3 ORM 高级

| 框架 | 核心要点 |
|------|---------|
| **MyBatis** | XML 与注解混用、二级缓存、插件（PageHelper/Sharding） |
| **Hibernate/JPA** | 二级缓存（EhCache/Redis）、脏检查、N+1 问题、DTO 投影 |

---

## 阶段 3：分布式中间件（5~6 个月）

### 3.1 消息队列

| MQ | 核心技术点 |
|----|-----------|
| **RabbitMQ** | AMQP 模型（Exchange/Queue/Binding）、确认模式、死信队列、延迟队列、集群镜像 |
| **Kafka** | Topic/Partition/Offset、ISR、Producer/Consumer Group、Exactly-Once、幂等 Producer、事务 API、Kafka Streams |
| **RocketMQ** | NameServer、事务消息、顺序消息、延迟消息 |

### 3.2 缓存与搜索

| 领域 | 核心技术点 |
|------|-----------|
| **本地缓存** | Caffeine、Guava Cache |
| **分布式缓存** | Redis Cluster、Codis；缓存模式：Cache-Aside / Read-Through / Write-Behind；BigKey/HotKey 探测与治理 |
| **搜索分析** | ES 集群调优、分片与副本、Index Template、ILM、Kibana 可视化；Logstash/Beats 数据管道；ELK/EFK 日志体系 |

### 3.3 任务调度

Quartz、Elastic-Job、XXL-JOB、PowerJob

---

## 阶段 4：微服务与云原生（7~8 个月）

| 领域 | 核心技术点 |
|------|-----------|
| **服务通信** | RESTful API 设计规范（URI/HTTP 动词/状态码/版本控制）、GraphQL、gRPC、Dubbo（Triple 协议）、OpenFeign/Retrofit、负载均衡（Ribbon → Spring Cloud LoadBalancer） |
| **服务治理** | Nacos/Consul/Eureka/Zookeeper（注册发现）、Nacos/Apollo（配置中心）、Sentinel（热点/系统规则）、Resilience4j（时间窗口）、SkyWalking/Zipkin/Jaeger（链路追踪 OpenTelemetry） |
| **容器编排** | Docker（镜像分层/多阶段构建/Dockerfile 最佳实践）、Kubernetes（Pod/Service/Ingress/ConfigMap/Secret、Deployment/StatefulSet/DaemonSet、HPA/VPA、CNI/CSI） |
| **Service Mesh** | Istio、Envoy、Sidecar 注入、mTLS、熔断、灰度发布 |
| **CI/CD** | Jenkins Pipeline、GitHub Actions、GitLab CI、Argo CD、Tekton、Helm Chart、Kustomize、Skaffold |

---

## 阶段 5：监控与可观测性（7~8 个月）

| 领域 | 核心技术点 |
|------|-----------|
| **指标监控** | Prometheus + Grafana（Counter/Gauge/Histogram/Summary）、Recording Rule、Alertmanager |
| **日志链路** | ELK（ES + Logstash + Kibana）/ EFK（Fluent Bit）集中日志、ElastAlert 告警 |
| **分布式追踪** | OpenTelemetry → Jaeger/Zipkin；SkyWalking 无侵入 Agent |
| **APM & Profiling** | Arthas 在线诊断、JProfiler、Async-Profiler、Java Flight Recorder |

---

## 阶段 6：性能与高可用（9~10 个月）

| 领域 | 核心技术点 |
|------|-----------|
| **JVM 性能** | GC 日志分析（GCeasy）、JMH 基准测试、JITWatch、内存 dump（MAT/Eclipse MAT） |
| **连接池/线程池** | HikariCP 调优、Tomcat/Jetty 线程池、CompletableFuture 并行度 |
| **压测与容量** | JMeter、Gatling、Locust；Little 定律计算并发用户数 |
| **高可用模式** | 主从读写分离、双写一致性（Binlog+MQ）、分库分表路由（ShardingSphere/MyCAT）；异地多活、单元化架构、同城双活 |

---

## 阶段 7：安全体系（9~10 个月）

| 领域 | 核心技术点 |
|------|-----------|
| **应用安全** | Spring Security OAuth2 授权码模式、JWT/JWE/JWS；防刷（验证码/滑动验证/接口限流 Bucket4j/Sentinel） |
| **数据安全** | 加密（Jasypt、AES-256、RSA-2048、国密 SM 系列）；脱敏（手机号/邮箱/身份证中间隐藏、敏感字段加密列） |
| **传输安全** | HTTPS/TLS 1.3、HSTS、双向 TLS |
| **漏洞防护** | OWASP Top 10（SQL 注入/XSS/CSRF/反序列化/权限绕过）；防御（预编译/CSP/SameSite Cookie/Rate Limit） |

---

## 阶段 8：测试与质量

| 领域 | 核心技术点 |
|------|-----------|
| **单元测试** | JUnit 5、Mockito、AssertJ、SpringBootTest |
| **集成测试** | Testcontainers（Docker 版 MySQL/Redis/Kafka）、Spring Test Slices |
| **契约测试** | Spring Cloud Contract、OpenAPI Generator |
| **代码质量** | SonarQube、Checkstyle、PMD、SpotBugs |

---

## 阶段 9：数据版本与迁移

| 领域 | 核心技术点 |
|------|-----------|
| **Schema 版本控制** | Flyway/Liquibase 脚本化迁移、基于 Git 分支的迁移策略 |
| **灰度发布** | Feature Toggle、蓝绿 + 滚动 + 金丝雀 |
| **回滚策略** | 数据库快照 + 应用镜像双轨回滚 |

---

## 阶段 10：持续学习与社区

| 方式 | 具体资源 |
|------|---------|
| **官方文档** | Spring.io、MySQL、Kubernetes.io |
| **社区与会议** | SpringOne、KubeCon、QCon、ArchSummit |
| **开源贡献** | Spring Cloud Alibaba、Dubbo、RocketMQ、Sentinel、ShardingSphere |

---

## 学习时间轴

```
第 1-2 月    第 3-4 月      第 5-6 月        第 7-8 月
Java SE    Spring Boot    Spring Cloud    K8s + SkyWalking
Git/Maven  MyBatis        RabbitMQ/Kafka  Prometheus + ELK
           MySQL + Redis  Docker

第 9-10 月       第 11-12 月
高并发项目实战     参与开源
压测 + 安全加固    面试冲刺
                  技术博客输出
```

---

## 技能阶段自查矩阵

| 能力域 | 初级 | 中级 | 高级 |
|--------|:---:|:---:|:---:|
| Java SE / JVM | ✅ | ✅ | ✅ |
| Spring Boot / MVC | ✅ | ✅ | ✅ |
| MySQL / Redis | ✅ | ✅ | ✅ |
| MyBatis / JPA | ✅ | ✅ | ✅ |
| Spring Cloud 微服务 | — | ✅ | ✅ |
| MQ / ES / NoSQL | — | ✅ | ✅ |
| Docker / K8s | — | ✅ | ✅ |
| 监控可观测 | — | — | ✅ |
| JVM 调优 / 高可用 | — | — | ✅ |
| 安全体系 | — | — | ✅ |

---

*最后更新：2026-07-15*
