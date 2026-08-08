# 01-Java 生态与 JVM 官方文档
> JDK API → JLS/JVMS/JEP → Spring Framework → Spring Boot → Spring AI → Spring Cloud 子集 → JUnit：本方向的精读章节清单、优先级与避坑

## 📚 目录
1. [文档地图与优先级](#1-文档地图与优先级)
2. [JDK 官方文档（API + 工具 + 规范 + JEP）](#2-jdk-官方文档api--工具--规范--jep)
3. [Spring Framework Reference](#3-spring-framework-reference)
4. [Spring Boot Reference](#4-spring-boot-reference)
5. [Spring AI Reference](#5-spring-ai-reference)
6. [Spring Cloud 必啃子集](#6-spring-cloud-必啃子集)
7. [JUnit 用户指南](#7-junit-用户指南)
8. [本方向避坑清单](#8-本方向避坑清单)
9. [参考来源](#9-参考来源)

## 1. 文档地图与优先级

| 文档 | 地址 | 优先级 | 版本窗口（2026-08） |
|------|------|:---:|------|
| JDK API 文档 | https://docs.oracle.com/en/java/javase/ | 🔴 | 17/21/**25（LTS）** |
| JLS 语言规范 | https://docs.oracle.com/javase/specs/jls/se25/html/ | 🟡 | Java 25 |
| JVMS 虚拟机规范 | https://docs.oracle.com/javase/specs/jvms/se25/html/ | 🟡 | Java 25 |
| JEP 索引 | https://openjdk.org/jeps/ | 🟢 | 滚动 |
| JDK 工具参考 | https://docs.oracle.com/en/java/javase/25/docs/specs/man/ | 🟢 | 25 |
| Spring Framework | https://docs.spring.io/spring-framework/reference/ | 🔴 | **7.x**（Boot 4）/ 6.2（Boot 3.5） |
| Spring Boot | https://docs.spring.io/spring-boot/reference/ | 🔴 | **4.x** |
| Spring AI | https://docs.spring.io/spring-ai/reference/ | 🟡 | 2.0.x ↔ Boot 4.x；1.1.x ↔ Boot 3.5 |
| Spring Cloud（子集） | https://docs.spring.io/spring-cloud/ | 🟡 | 2025.1 Oakwood |
| JUnit | https://junit.org/junit5/docs/current/user-guide/ | 🟢 | 6.x |

> 💡 **版本纪律**：页脚版本决定一切。Boot 3.x 网文与 Boot 4 行为差异集中在配置属性、`spring-boot-starter-*` 默认值与 API 调整；看文档务必先核对页脚版本号。

## 2. JDK 官方文档（API + 工具 + 规范 + JEP）

### 2.1 API 文档精读清单（按面试频率排序）

| 精读模块 | 必看类/章节 | 面试触点 |
|---------|-------------|---------|
| java.util（集合） | `HashMap` 注释（含扰动函数与树化条件）、`ConcurrentHashMap`、`ArrayList`、`LinkedList`、`TreeMap` | 扩容、哈希碰撞、fail-fast |
| java.util.concurrent | `ThreadPoolExecutor` 注释、`CompletableFuture` 方法契约、`Semaphore`/`CountDownLatch`/`CyclicBarrier`、并发集合 | 线程池参数、异步编排 |
| java.lang | `String`（不可变与池）、`ClassLoader`、`Thread`/`ThreadLocal`、`record`/`sealed`（JEP 395/409） | 常量池、类加载双亲委派 |
| java.nio | `Files`/`Path`（NIO2）、`ByteBuffer`、`FileChannel` | 零拷贝、IO 模型 |
| java.util.stream | Stream 方法与并行流契约、`Collectors` | 流式处理、短路 |
| java.time | `LocalDateTime`/`Instant` 边界语义 | 时间 API 易错点 |
| 异常体系 | `Throwable` 层级与受检/非受检 | 面试必问基础 |

> 🎯 **核心要点**：JDK API 的正确读法 = **读类注释与关键方法契约**（`@throws`、线程安全声明、默认值），"类注释讲设计意图，方法注释讲调用约束"，比任何博客都准。

### 2.2 规范类文档（JLS / JVMS）

| 规范 | 精读章节 | 用途 |
|------|---------|------|
| JLS | 4 类型与值、5 转换、8 类、9 接口、14 语句、15 表达式（含三元/switch）、17 线程语义、6.5 解析（泛型擦除） | 回答"语言层为什么" |
| JVMS | 2 运行时数据区与栈帧、3 编译为字节码（javac 示例）、5 加载/链接/初始化、6 指令集（查表用） | 回答"虚拟机层怎么实现" |

> 💡 JLS/JVMS 是**规范不是教程**，读法：先被面试题击中（如"类加载过程"），再翻对应章节找官方定义，而不是从头啃。GC 章节（JVMS 2.5.2 堆 + 各收集器行为）配合 JDK 工具参考的 jcmd/jstat 一起学。

### 2.3 JEP 与工具参考

| 资源 | 关注点 |
|------|--------|
| JEP 索引 | 虚拟线程（425）、结构化并发（525）、ScopedValue（506）、值类型 Valhalla（401）、向量 API、ZGC 演进 |
| JDK 工具参考 | `java`（启动与调优选项）、`jcmd`、`jstat`、`jmap`、`jstack`、`jfr`、`jshell`、`jdeps` |

> ⚠️ 校招阶段 JEP 挑 3~5 个热点读原文摘要即可（虚拟线程、Loom、ScopedValue 是 2025-2026 面试新宠），不必全读。

## 3. Spring Framework Reference

### 3.1 精读章节清单

| 章节（Core） | 必读小节 | 面试考点 |
|-------------|---------|---------|
| IoC 容器 | 容器总览、Bean 概述（命名/实例化）、依赖注入（构造器 vs Setter）、Bean 作用域、生命周期回调、基于注解的容器配置、Bean 定义继承与 `@Import` | 容器初始化、三级缓存、循环依赖 |
| AOP | 概念（切点/通知/织入）、代理机制（JDK vs CGLIB）、声明式事务原理 | 代理失效场景、事务失效场景 |
| 事务 | 事务传播行为表、回滚规则（`rollbackFor` 默认只回滚 RuntimeException）、声明式事务实现 | 传播机制 7 种、自调用失效 |
| 事件 | 事件发布/监听、`@TransactionalEventListener` | 事务后事件 |
| 基础设施 | Resource 抽象、类型转换、校验（JSR-303）、SpEL | 资源注入、参数绑定 |

> ⚠️ **版本提示**：Framework 7.x 是 Boot 4 基线；若项目仍是 Boot 3.5（Framework 6.2），绝大多数章节通用，但注解默认值、`@Lazy` 行为、AOT 章节需对照 6.2 版本文档。

### 3.2 读法建议

- 第一遍：只读"容器与依赖注入"整章（面试占比最高）；
- 第二遍：按考点打点（AOP 代理、事务传播、事件）；
- 第三遍：面试前复习"为什么"类结论（为什么构造器注入推荐、为什么循环依赖默认不解决）。

## 4. Spring Boot Reference

### 4.1 精读章节清单

| 章节 | 必读小节 | 面试考点 |
|------|---------|---------|
| 使用 Spring Boot | 构建系统（starter/BOM）、自动配置原理（`@EnableAutoConfiguration`/`@ConditionalOnXxx`）、运行与调试 | 自动配置原理是校招必考 |
| 核心特性 | 外部配置（application.properties/yaml、`@ConfigurationProperties`、Profile）、条件注解全家、类型安全配置绑定、错误处理 | 配置加载顺序、配置绑定 |
| Web | WebMVC 控制器、`@RestControllerAdvice`、参数校验、WebFlux 与 WebMVC 取舍 | REST 开发规范 |
| 数据 | DataSource 初始化、JPA/JDBC、事务（复用 Framework 章节） | 数据访问栈 |
| 测试 | 切片测试（`@WebMvcTest` 等）、`@SpringBootTest`、MockBean | 测试策略 |
| 生产就绪 | Actuator（health/metrics/info）、外部化监控配置 | 生产监控 |

### 4.2 Boot 4 特有变化（2025-11 起）

| 变化 | 说明 |
|------|------|
| Java 17+ 基线（默认 21/25） | 老教程的 Java 8 配置无效 |
| 自动配置重构 | 部分 starter 拆分为 modularized auto-configuration 模块 |
| Jackson 3 升级 | `spring-boot-starter-json` 依赖体系变化 |
| 配置属性调整 | 部分属性改名/默认值变化（以官方 migration guide 为准） |

> 💡 面试前自查：能讲清"一个 `spring-boot-starter-web` 依赖进来后，容器里多了哪些自动配置 Bean、为什么"——这是 Boot 章节学没学透的试金石。

## 5. Spring AI Reference

| 章节 | 必读小节 | 场景 |
|------|---------|------|
| Getting Started | 依赖管理（`spring-ai-bom`）、starter 选择、模型/向量库配置 | 项目搭建 |
| ChatClient API | 同步/流式对话、Advisors（记忆/上下文） | 对话应用 |
| Structured Outputs | 结构化输出映射 POJO | RAG/Agent 数据抽取 |
| Tool Calling | 工具调用与 MCP 集成 | Agent 开发 |
| RAG 与 Vector Store | 文档 ETL 框架、向量库接入（Milvus/PGVector/Redis 等）、检索与重排 | RAG 应用 |
| Observability 与 Evaluation | 指标、追踪、模型评估 | 生产化 |

> ⚠️ **版本对齐**：Spring AI 2.0.x 只支持 Boot 4.0.x/4.1.x；Boot 3.5 项目必须用 1.1.x。先看 Getting Started 页脚确认版本，再选 starter。

## 6. Spring Cloud 必啃子集

校招不必啃完全套（组件迭代快），但以下子集是微服务面试的基本盘：

| 组件 | 必看章节 | 深度要求 |
|------|---------|---------|
| Nacos | 注册中心（服务发现流程）、配置中心（动态刷新原理）、AP/CP 一致性 | 原理级 |
| OpenFeign | 声明式调用、超时/重试、与 LoadBalancer 协同 | 使用级 + 原理简述 |
| Gateway | 路由四要素、过滤器链、限流 | 概念级 |
| Spring Cloud Stream | 函数式模型、Binder 抽象、重试/DLQ | 概念级（消息链路） |

> 💡 学习顺序：Nacos（原理最重要）→ OpenFeign（最常用）→ Gateway/Stream（概念与选型）。详细内容见本仓库"Spring Cloud 微服务全家桶"系列文档。

## 7. JUnit 用户指南

| 章节 | 内容 | 场景 |
|------|------|------|
| 断言与假设 | 全套断言 API、`assertThrows`/`assertTimeout` | 单元测试 |
| 生命周期 | `@BeforeEach`/`@AfterEach`/嵌套测试 | 测试结构 |
| 参数化测试 | `@ParameterizedTest` + ValueSource/CsvSource/MethodSource | 数据驱动 |
| 扩展模型 | `@ExtendWith` 与 `TestWatcher`/自定义扩展 | 框架级理解 |
| Spring 集成 | `@SpringBootTest`/切片测试的 JUnit 支撑 | 工程实践 |

## 8. 本方向避坑清单

| # | 坑 | 规避 |
|---|----|------|
| 1 | JDK API 版本看错（17 vs 25） | 文档顶部版本下拉选自己 JDK 的 LTS（21/25） |
| 2 | 用 Boot 3 网文答 Boot 4 面试题 | 以 Boot 4 Reference + migration guide 为准 |
| 3 | 把 JLS/JVMS 当教程通读 | 只按面试题打点对应章节 |
| 4 | Spring AI 版本不匹配（2.x 用在 Boot 3.5） | 查 Getting Started 兼容性说明 |
| 5 | 只看 QuickStart 不看 Reference | QuickStart 跑通后必须回 Reference 读原理章节 |
| 6 | 官方 Javadoc 太散，只看博客 | 以"类注释 + 方法契约"为事实源，博客只做索引 |

## 9. 参考来源

- [JDK 25 Documentation](https://docs.oracle.com/en/java/javase/25/)
- [JLS / JVMS（Java SE 25 规范）](https://docs.oracle.com/javase/specs/)
- [OpenJDK JEP Index](https://openjdk.org/jeps/)
- [Spring Framework Reference](https://docs.spring.io/spring-framework/reference/)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/reference/)
- [Spring AI Reference / Getting Started](https://docs.spring.io/spring-ai/reference/getting-started.html)
- [JUnit 5/6 用户指南](https://junit.org/junit5/docs/current/user-guide/)

---

**下一模块**：[02-中间件官方文档](02-中间件官方文档.md)　/　**返回总览**：[00-总览](00-官方文档学习总览.md)
