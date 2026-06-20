# ☕ Java 后端技术栈完全指南

> 涵盖 Java 后端开发所有核心名词、技术栈、核心知识点，适合入门学习与快速复习。
>
> 📅 最近更新：2026-06-12

---

## 📚 文档索引

| # | 文档 | 核心内容 |
|---|------|---------|
| 01 | [Java 基础与核心特性](docs/01-Java基础与核心特性.md) | 数据类型、集合、异常、泛型、注解/反射、Lambda/Stream、多线程、虚拟线程、NIO/Netty |
| 02 | [构建工具 Maven 与 Gradle](docs/02-构建工具Maven与Gradle详解.md) | 坐标系统、依赖管理、生命周期、多模块、BOM、私有仓库、依赖冲突解决 |
| 03 | [Web 基础与 RESTful API 设计](docs/03-Web基础与RESTful%20API设计.md) | HTTP 协议、状态码、REST 规范、CORS、HTTPS、WebSocket、文件上传下载、Servlet/Tomcat |
| 04 | [Spring 全家桶深度详解](docs/04-Spring全家桶深度详解.md) | IoC/DI、Bean 生命周期、AOP、事务管理、MVC 流程、自动配置原理、Security、事件机制 |
| 05 | [持久层与数据库技术](docs/05-持久层与数据库技术.md) | JDBC、连接池、MyBatis/MyBatis-Plus、JPA/Hibernate、MySQL 原理/索引/事务/MVCC、分库分表、SQL 优化 |
| 06 | [缓存技术与 Redis 深度解析](docs/06-缓存技术与Redis深度解析.md) | Caffeine、Redis 数据结构/持久化/高可用/集群、缓存三问题、分布式锁、缓存一致性、Spring Cache |
| 07 | [消息队列技术选型与实践](docs/07-消息队列技术选型与实践.md) | RocketMQ/Kafka/RabbitMQ 对比、可靠性、幂等性、顺序消息、延迟消息、死信队列 |
| 08 | [微服务架构与分布式系统](docs/08-微服务架构与分布式系统.md) | CAP/BASE、注册中心/配置中心、OpenFeign、Gateway、Sentinel、分布式事务、链路追踪 |
| 09 | [认证授权与安全防护](docs/09-认证授权与安全防护.md) | JWT、OAuth 2.0/OIDC、Spring Security、RBAC、CSRF/XSS 防护、数据脱敏 |
| 10 | [测试体系与质量保障](docs/10-测试体系与质量保障.md) | JUnit 5、Mockito、Spring Boot Test、Testcontainers、性能测试、契约测试 |
| 11 | [DevOps 与云原生部署](docs/11-DevOps与云原生部署.md) | Docker/Docker Compose、K8s 核心资源、CI/CD Pipeline、GraalVM Native Image、可观测性 |
| 12 | [JVM 原理与性能调优](docs/12-JVM原理与性能调优.md) | 类加载、内存模型、GC 算法/回收器、JIT 编译、调优参数、OOM 排查 |
| 13 | [企业级设计模式与架构](docs/13-企业级设计模式与架构.md) | GoF 23 种设计模式、Spring 中的应用、DDD 领域驱动设计、架构演进 |
| 14 | [项目工程规范与最佳实践](docs/14-项目工程规范与最佳实践.md) | 编码规范、项目结构、Git 工作流、代码审查、日志规范、API 规范、工具库推荐 |
| 15 | [学习路线与面试高频考点](docs/15-学习路线与面试高频考点.md) | 5 阶段学习路线、资源推荐、Spring/MySQL/Redis/分布式/JVM 高频面试题 |

---

## 🚀 快速开始

### 如果你是新手

1. 从 **01-Java 基础与核心特性** 开始，打好语言基础
2. 然后看 **03-Web 基础** 和 **04-Spring 全家桶**，学会做 CRUD 接口
3. 接着看 **05-持久层** 和 **02-构建工具**，理解数据层和工程化
4. 最后按照 **15-学习路线** 规划后续学习

### 如果你要面试

1. 先通读 **15-学习路线与面试高频考点**，里面有 50 道高频题
2. 再到各专题文档中深入理解答案背后的原理
3. 重点看：04-Spring（最常问）、05-持久层（MySQL 索引/事务）、06-Redis、08-微服务、12-JVM

### 如果你要搭建项目

1. 参考 **14-项目工程规范** 设计项目结构
2. 参考 **11-DevOps** 配置 CI/CD 和部署
3. 参考 **09-认证授权** 接入安全体系

---

## 📖 使用建议

- **看 3 分，写 7 分**：看懂概念后一定要动手写代码
- **交叉阅读**：各文档之间有交叉引用，遇到相关概念可以跳转阅读
- **按需查阅**：不需要一次通读全部内容，有具体问题时查阅对应专题
- **持续更新**：技术栈在演进，建议关注 Spring、JDK 的版本更新

---

## 🔗 推荐资源

- Spring 官方文档：https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/
- MyBatis 官方文档：https://mybatis.org/mybatis-3/zh/
- 阿里 Java 开发手册：https://github.com/alibaba/p3c
- JavaGuide：https://javaguide.cn
