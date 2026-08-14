# Java 常用注解剖析 知识体系总览

> 日常开发中每个注解背后都是一套机制：本体系按「语言层 → JVM 层 → 生态层」全景剖析 JDK 内置注解、Lombok、Jackson、Jakarta 校验、Spring、持久层、测试注解的源码级原理、属性语义、高频坑与 2026 版本基线——与「泛型 反射 注解」体系分工：那边讲注解机制（本质/元注解/APT 原理），这边讲注解使用（每个常用注解怎么用、为什么、踩什么坑）

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [版本基线（2026-08 检索校准）](#5-版本基线2026-08-检索校准)
6. [常见疑问速答](#6-常见疑问速答)

---

## 1. 知识体系导图

```text
Java 常用注解剖析
│
├── 语言层（JDK 内置 + JVM 内部）
│   ├── 01 JDK 内置注解剖析
│   │   ├── 标记类：@Override / @Deprecated / @FunctionalInterface
│   │   ├── 抑制类：@SuppressWarnings / @SafeVarargs
│   │   └── 元注解五件套：@Target / @Retention / @Documented / @Inherited / @Repeatable（+ @Native）
│   └── 02 JVM 内部注解深潜（jdk.internal.vm.annotation）
│       ├── JIT 行为：@Stable / @ForceInline / @DontInline / @IntrinsicCandidate
│       └── 内存与栈：@Contended（伪共享）/ @ReservedStackAccess / @Hidden
│
├── 生态层（六大框架注解家族）
│   ├── 03 Lombok：@Data / @Builder / @Slf4j / @Jacksonized（APT + AST 魔改）
│   ├── 04 Jackson：@JsonProperty / @JsonIgnore / @JsonFormat / @JsonView / @JsonTypeInfo（Jackson 3 迁移）
│   ├── 05 Jakarta 校验：@NotNull / @NotBlank / @Valid / 分组 / 自定义约束（Validator 9.1）
│   ├── 06 Spring 核心：@Component 族 / @Autowired / @Configuration / @Transactional / @Retryable（Spring 7 新注解）
│   ├── 07 Spring Web：@RestController / @GetMapping / @RequestParam / @ControllerAdvice（@ApiVersion 版本化）
│   ├── 08 持久层：JPA @Entity / @OneToMany / @Enumerated、MyBatis @Select / @Mapper、Hibernate 8 新注解
│   └── 09 测试：JUnit 6 全谱系 / Mockito @Mock / Spring Boot Test @MockitoBean
│
└── 工程层
    └── 10 自定义注解工程实战：设计决策树 / @AliasFor 组合 / 审计日志·限流·脱敏·APT 四案例
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 01 | [JDK 内置注解剖析](./01-JDK内置注解剖析.md) | @Override/@Deprecated(forRemoval)/@SuppressWarnings/@SafeVarargs/@Repeatable/元注解五件套语义与坑 | 初中级必读 |
| 02 | [JVM 内部注解深潜](./02-JVM内部注解深潜.md) | @Stable/@ForceInline/@IntrinsicCandidate/@Contended 如何驱动 JIT 优化与伪共享消除 | 中级 → 高级 |
| 03 | [Lombok 注解剖析](./03-Lombok注解剖析.md) | @Data/@Builder/@SuperBuilder/@Slf4j/@Jacksonized 原理（AST 修改）、1.18.46 基线、与 record 分工 | 初中级必读 |
| 04 | [Jackson 注解剖析](./04-Jackson注解剖析.md) | @JsonProperty/@JsonIgnore/@JsonFormat/@JsonView/@JsonTypeInfo；Jackson 3 迁移（注解包不动、默认行为变化） | 初中级必读 |
| 05 | [Jakarta 校验注解剖析](./05-Jakarta校验注解剖析.md) | @NotNull/@NotEmpty/@NotBlank 边界、级联 @Valid、分组校验、自定义约束、@Validated vs @Valid | 初中级必读 |
| 06 | [Spring 核心注解剖析](./06-Spring核心注解剖析.md) | 组件四件套/@Autowired/@Configuration/@Transactional 失效场景/Spring 7 新注解 @Retryable/@ConcurrencyLimit/@ApiVersion | 中级 |
| 07 | [Spring Web 注解剖析](./07-SpringWeb注解剖析.md) | @RestController/@GetMapping/参数绑定五注解/@ExceptionHandler/@CrossOrigin 全属性与坑 | 中级 |
| 08 | [持久层注解剖析](./08-持久层注解剖析.md) | JPA 实体/关系/乐观锁注解、@Enumerated 坑、MyBatis @Select 注解 SQL、MyBatis-Plus 3.5.17、Hibernate 8 新注解 | 中级 |
| 09 | [测试注解剖析](./09-测试注解剖析.md) | JUnit 6 全谱系（@Parallelizable 虚拟线程/@AutoClose）、Mockito 四注解、@MockitoBean 替代 @MockBean | 中级 |
| 10 | [自定义注解工程实战](./10-自定义注解工程实战.md) | Retention/Target 决策树、@AliasFor 元注解组合、审计日志/限流/脱敏/APT 四案例 | 高级 |

---

## 3. 学习路线推荐

### 路线 A：日常开发速成型（3~4 天）

```text
Day 1: 01 JDK 内置注解（基础打底）→ 05 Jakarta 校验（接口入参必用）
Day 2: 03 Lombok + 04 Jackson（DTO/实体/序列化三件套）
Day 3: 06 Spring 核心 + 07 Spring Web（业务开发主力）
Day 4: 08 持久层 + 09 测试（数据与质量闭环）
```

### 路线 B：原理深挖型（1~2 周）

```text
第 1 周: 01 → 02（JIT 层）→ 10（自定义注解，串起反射/APT 全链路）
第 2 周: 03~09 按项目实际栈精读对应模块，每篇对照源码验证一个注解的处理链路
```

### 路线 C：面试冲刺型（按题型索引）

```text
注解本质与元注解 → 看「泛型 反射 注解」体系 03 篇 + 本体系 01 篇
@Transactional 失效 → 本体系 06 篇
@Valid vs @Validated / 分组 → 本体系 05 篇
手写自定义注解 + AOP/APT → 本体系 10 篇
```

---

## 4. 核心概念速查

### 4.1 注解全景地图

| 生态 | 代表注解 | Retention | 处理方式 | 2026 版本基线 |
|------|---------|:---:|---------|---------|
| JDK 内置 | @Override / @Deprecated | SOURCE / RUNTIME | javac 检查 | JDK 26 |
| JVM 内部 | @Stable / @IntrinsicCandidate | RUNTIME | HotSpot JIT | JDK 26 |
| Lombok | @Data / @Builder | SOURCE | APT + AST 修改 | 1.18.46（2026-04-22，支持 JDK 26） |
| Jackson | @JsonProperty / @JsonFormat | RUNTIME | 反射 + 注解内省 | 3.0.x（注解包仍为 com.fasterxml.jackson.annotation） |
| Jakarta 校验 | @NotNull / @Valid | RUNTIME | ConstraintValidator | 3.1 现行；4.0.0-M1 已发（目标 EE 12）；Hibernate Validator 9.1.2.Final（2026-07-06） |
| Spring 核心 | @Component / @Transactional | RUNTIME | 容器扫描 + AOP | Framework 7.0 / Boot 4.1（2025-11 发布） |
| Spring Web | @GetMapping / @RequestParam | RUNTIME | HandlerMapping / 参数解析器 | Framework 7.0（@ApiVersion、version 属性） |
| JPA / Hibernate | @Entity / @OneToMany | RUNTIME | 元模型 + ORM | Jakarta Persistence 3.2；Hibernate 8.0.0.Beta1（2026-06-16，JP 4.0） |
| MyBatis / MP | @Select / @TableId | RUNTIME | MapperProxy 代理 | MyBatis 3.5.19；MyBatis-Plus 3.5.17（2026-07-09） |
| JUnit 6 | @Test / @Parallelizable | RUNTIME | 测试引擎反射 | 6.0.2（2026-01-06），Java 17 基线 |

### 4.2 六条关键认知

1. **注解本身不干活**：注解只是元数据，必须有"消费者"——javac（@Override）、JIT（@Stable）、APT（Lombok）、反射框架（Spring/Jackson）。没有消费者的注解等于注释。
2. **Retention 决定消费时机**：SOURCE 只有编译器看得到（@Override）；CLASS 编译期 APT 看得到（Lombok）；RUNTIME 运行期反射读得到（Spring/Jackson/校验）。选错 Retention，框架就"看不到"注解。
3. **同名注解含义因框架而异**：`@Transient` 在 JPA 是"不映射字段"、在 Jackson 是"不序列化"；`@Value` 在 Lombok 是"生成不可变类"、在 Spring 是"注入配置值"。剖析注解必须绑定其所在生态。
4. **@NonNull 有三胞胎**：Lombok 的生成运行时 NPE 检查、JSpecify 的纯编译期契约、Spring 旧版的文档标注——包名不同、行为不同，混用是空安全的最大事故源。
5. **派生注解是框架扩展的主通道**：@Service 就是"叠了 @Component 的注解"，自定义团队注解复用这条机制（@AliasFor + 元注解组合），是注解工程化的核心姿势。
6. **注解 API 也要向后兼容**：新增属性必须带 default，否则所有存量使用点编译失败——注解是公共 API，演进规则与代码接口相同。

---

## 5. 版本基线（2026-08 检索校准）

- **JDK 26**（2026-03 GA）：无新增内置注解 JEP；JEP 526 Lazy Constants（二次预览）内部用 `@Stable` 字段实现常量折叠；JDK 27 已进入 EA。
- **Lombok 1.18.46**：官方支持 JDK 26；修复 `@Jacksonized` 与显式 `@JsonIgnore` 共存时 `@JsonProperty` 停止生成的问题（#4022）。
- **Jackson 3**：核心类迁至 `tools.jackson.*`，但**注解包保持 `com.fasterxml.jackson.annotation` 不变**（与 2.x 共享 2.20+ 注解 JAR）；`@JsonSerialize`/`@JsonDeserialize` 移至 `tools.jackson.databind.annotation`；`ObjectMapper` 被不可变 `JsonMapper.builder()` 取代；默认行为三变（日期 ISO-8601、未知属性不报错、null 赋给原语报错）。Spring Boot 4 默认 Jackson 3。
- **Jakarta Validation**：3.1 随 EE 11 现行；4.0 目标 EE 12（发布计划 2026-01-31，目前仅 4.0.0-M1 里程碑）。
- **Spring Framework 7 / Boot 4**：新增 `@Retryable`/`@ConcurrencyLimit`/`@Recover`/`@Timeout`（Resilience 模块，`@EnableResilientMethods` 替代 `@EnableRetry`）、`@ApiVersion` + `@GetMapping(version=...)`；全面拥抱 JSpecify（`@NonNull`/`@Nullable`/`@NullMarked`）；`javax.*` 注解编译通过但被静默忽略。
- **JUnit 6**：Java 17 基线、JSpecify 空安全注解、`@Parallelizable(VIRTUAL)` 虚拟线程并行测试、`@AutoClose`、Vintage 弃用；`@MockBean`/`@SpyBean` 已移除，由 `@MockitoBean`/`@MockitoSpyBean` 取代。
- **持久层**：Hibernate ORM 8.0.0.Beta1 支持 Jakarta Persistence 4.0（新注解 `@Fetch`/`@ExcludedFromVersioning`），稳定线仍为 7.x；MyBatis 3.5.19（核心无 2026 新版本）；MyBatis-Plus 3.5.17；MapStruct 1.7.0.Beta2（JSpecify 支持、`@Mapper(accessibility=...)`）。

---

## 6. 常见疑问速答

**Q：注解和注释有什么区别？** 注释（comment）只给人看，编译后彻底消失；注解是编译进字节码的**结构化元数据**（按 Retention 保留到不同阶段），有类型、有属性、可被程序读取——"注释是给人看的，注解是给程序看的"。

**Q：为什么 @Retention 默认是 CLASS？** 历史兼容设计：注解最初（Java 5 草案）就以 CLASS 为默认，运行期反射是后来扩展的能力。默认 CLASS 意味着"编译期工具可见、运行期不可见"，对 JAR 体积与运行期负担最平衡——但自研注解忘记显式声明 RUNTIME，运行期消费者就会静默失明。

**Q：如何快速定位一个陌生注解的消费者？** 三步：看 import 的包名定位生态 → 看 @Retention 推断消费阶段（SOURCE 看编译器、CLASS 看 APT、RUNTIME 搜框架的注解扫描代码）→ 用 IDE "查找用法"在框架源码里搜注解类名。Spring 系注解的消费者几乎都在 `MergedAnnotations`/AOP 基础设施里。

**Q：升级 JDK 大版本前为什么先检查 Lombok？** Lombok 依赖 javac 的非公开内部 API 修改 AST，每个 JDK 大版本都可能破坏它。JDK 26 需要 Lombok 1.18.46 才官方支持——升级顺序是"先 Lombok 后 JDK"，否则整个项目无法编译。

**Q：新项目还要全面引入 Lombok 吗？** 不。record（JDK 16+）已覆盖 @Value/@Data 的不可变数据载体场景，Lombok 的生存空间收缩到"可变 DTO + Builder + 日志注解"。新项目默认 record、按需 Lombok 是 2026 年的主流共识。

**Q：本体系与「泛型 反射 注解」体系怎么配合读？** 那套体系解决"注解是什么、怎么被处理"（机制层：本质/元注解/APT/三剑合璧），本体系解决"每个常用注解怎么用、为什么这么设计、踩什么坑"（使用层：六大生态逐注解剖析）。先读那套 03 篇建立机制认知，再按本体系模块导航逐生态深化。

**Q：运行期注解有性能问题吗？** RUNTIME 注解实例在类加载后由 JVM 缓存，反射读取成本集中在**首次解析**（框架启动期的扫描与切点匹配）；请求路径上的 @Transactional/@Cacheable 等 AOP 拦截器走代理缓存，注解读取不在热路径。真正要警惕的是自研代码在**循环里反复 getAnnotation**——一次解析、缓存复用的纪律见 10 篇。

---

**相关体系**：[泛型 反射 注解（机制层分工）](../泛型%20反射%20注解/00-泛型反射注解知识体系总览.md) · [装箱拆箱 泛型擦除](../装箱拆箱%20泛型擦除/00-装箱拆箱与泛型擦除知识体系总览.md) · [Spring 框架核心](../../../02-后端核心技术%20微服务%20分布式%20云原生/06-Spring全家桶/Spring框架核心/00-Spring框架核心知识体系总览.md) · [SpringBoot Web](../../../02-后端核心技术%20微服务%20分布式%20云原生/09-Web开发全流程/SpringBoot%20Web/00-SpringBootWeb总览.md) · [Spring Data JPA](../../../02-后端核心技术%20微服务%20分布式%20云原生/06-Spring全家桶/Spring组件汇总/Spring%20Data%20系列【数据访问层】/Spring%20Data%20JPA/00-Spring%20Data%20JPA组件总览.md) · [MyBatisPlus](../../../02-后端核心技术%20微服务%20分布式%20云原生/01-关系型数据库/MyBatisPlus/00-MyBatisPlus知识体系总览.md) · [JUnit](../../../02-后端核心技术%20微服务%20分布式%20云原生/07-工程运维测试拓展/测试/JUnit/00-JUnit知识体系总览.md)

---

**下一模块**：[01 JDK 内置注解剖析](./01-JDK内置注解剖析.md)
