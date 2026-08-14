# 03 Lombok 注解剖析

> Lombok 是"编译期魔法"的代名词：@Data 一行注解生成六件套代码。它与其他注解生态的本质区别是——通过 JSR-269 注解处理器直接**修改 javac 的 AST 语法树**，生成的方法直接编译进字节码，源文件里永远看不到。理解这个机制，才能理解它的所有坑与边界

## 📚 目录

1. [原理：AST 修改而非代码生成](#1-原理ast-修改而非代码生成)
2. [@Data 全家桶与不可变类 @Value](#2-data-全家桶与不可变类-value)
3. [@Builder / @SuperBuilder 与 @Jacksonized](#3-builder--superbuilder-与-jacksonized)
4. [@Slf4j 族与其他常用注解](#4-slf4j-族与其他常用注解)
5. [版本基线、风险与 record 分工](#5-版本基线风险与-record-分工)

---

## 1. 原理：AST 修改而非代码生成

Lombok 运行在 `@Retention(SOURCE)` 之上的逻辑很特别：处理器是 JSR-269 标准 `AnnotationProcessor`，但它**不产出任何新源文件**——标准 APT 只允许"读 AST + 生成新文件"，Lombok 则直接调用 `com.sun.tools.javac` 的非公开内部 API 篡改 AST，把 getter/setter/构造器等方法**直接注入正在编译的类**，然后才生成字节码。

这带来三个区别于 MapStruct/QueryDSL 等"正统" APT 工具的后果：

1. **生成代码不可见也不可引用**：IDE 里写 `user.getName()` 能编译，但源码中没有该方法——依赖 IDE 插件做静态补全，反编译字节码才能看到真实方法。团队里 IDE 插件缺失时，代码阅读体验显著下降；
2. **对 JDK 版本敏感**：每个 JDK 大版本可能调整 javac 内部 API，历史上 JDK 16 强封装后 Lombok 一度需要升级才能支持新 JDK。**升级 JDK 必须先升级 Lombok**（1.18.46 才官方支持 JDK 26），这是选型时必须写入团队规范的成本；
3. **注解间存在处理顺序**：Lombok 处理器与 MapStruct 等生成器共存时，有 `lombok-mapstruct-binding` 或注解处理器顺序配置问题，混用需验证生成结果。

> 🎯 **核心要点**：Lombok 的"魔法"本质是编译期黑客行为——用非公开 API 改语法树。它赢在代码极简，代价是版本耦合、调试困难、与记录类型的功能重叠。

---

## 2. @Data 全家桶与不可变类 @Value

### 2.1 @Data 的精确语义

`@Data` 是组合注解，精确等价于 `@Getter + @Setter + @RequiredArgsConstructor + @ToString + @EqualsAndHashCode` 五件套的叠加，属性 `staticConstructor = "of"` 会把构造器私有化并生成静态工厂方法 `of(...)`（建议命名 `of`，与 `List.of` 风格一致）。生成的构造器只包含 **final 字段与 @NonNull 字段**。

`@Data` 在 JPA 实体上的坑是高频事故：实体的 `equals/hashCode` 基于全部非 transient 字段，而 Hibernate 延迟加载代理的字段是未初始化的——比较两个代理对象会触发意外 SQL 甚至 `LazyInitializationException`；且 `hashCode` 若在持久化前后值变化，放入 `Set` 后再修改字段会导致元素"丢失"。实体类的标准做法是：不用 @Data，手写基于业务主键（@Id）的 equals/hashCode，或使用 `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` 配合 `@EqualsAndHashCode.Include` 只比较 id。

### 2.2 @Value 与 @With：不可变风格

`@Value` 等价于"所有字段 private final + 全参构造器 + getter（无 setter）+ equals/hashCode/toString"，是 DTO/值对象的利器。`@With` 为每个字段生成 `withXxx(newValue)` 返回新副本，实现"修改即复制"。两者组合即可写出轻量不可变对象，但与 record 高度重叠——见第 5 节的分工结论。

### 2.3 @Getter/@Setter 细节

- 字段级与类级都可用，类级可排除：`@Getter(value = AccessLevel.PROTECTED, onMethod_ = ...)`；
- boolean 字段的 getter 命名为 `isXxx()`（非 `getXxx()`），Jackson 等框架按 JavaBean 规范兼容处理；
- `@Getter(lazy = true)` 生成双重检查锁的惰性 getter，要求字段 private final 且初始化表达式代价高时使用；
- 与手写方法冲突时 Lombok 默认**跳过**已存在的方法（`lombok.getter.noIsPrefix` 等配置可调命名）。

### 2.4 @Accessors 与命名风格控制

`@Accessors(fluent = true, chain = true)` 控制 getter/setter 的命名风格：`chain = true` 让 setter 返回 `this` 支持链式调用（`user.setName("a").setAge(1)`）；`fluent = true` 去掉 get/set 前缀（`user.name("a").age(1)`）。它影响的不只是代码风格——**Jackson 等按 JavaBean 规范发现属性的框架依赖 get/set 前缀**：Jackson 3 收紧属性发现后，`@Accessors(fluent = true)` 的字段可能被序列化为空对象（Lombok #4004），需配合 @Jacksonized 或显式 @JsonProperty。结论：DTO/实体与序列化框架交互的类**不要用 fluent 风格**，链式风格也只在内部 Builder 场景使用。

---

## 3. @Builder / @SuperBuilder 与 @Jacksonized

`@Builder` 生成 Builder 模式代码，三个高频坑：

1. **继承失效**：@Builder 只构建当前类的字段，父类字段无法通过 Builder 设置，子类使用 @Builder 会编译错误——必须父类子类都用 **@SuperBuilder**；
2. **@Builder.Default 陷阱**：Builder 模式的默认值有两层——构造器默认值与 Builder 默认值。字段初始化 `private int retries = 3;` 只在直接用构造器时生效，通过 Builder 构建时该初始值会被覆盖为 0，必须加 `@Builder.Default` 才把初始值带到 Builder 路径。这是最隐蔽的行为差异；
3. **@Singular** 为集合字段生成单元素添加方法（`item(...)`/`items(...)`），内部用 `ImmutableList` 等不可变集合，注意返回类型是 Guava 风格不可变集合而非 `ArrayList`。

`@Jacksonized` 解决 Builder 与 Jackson 反序列化的配合问题：默认 Jackson 无法通过 Builder 构造对象（它不认识 Lombok 生成的 Builder 类型），@Jacksonized 生成 `@JsonPOJOBuilder` 与 `@JsonDeserialize(builder = ...)` 的等价注解，让 `User.builder().build()` 链路成为反序列化入口。Jackson 3 时代此注解特别重要——Lombok 1.18.46（2026-04-22）修复了 **@Jacksonized 与显式 @JsonIgnore 共存时不再生成 @JsonProperty** 的 bug（#4022），以及 Eclipse 下与 `fluent = true` 混用的 @JsonProperty 重复注解错误（#3934）。Jackson 3 + @Accessors(fluent=true) 组合下字段属性发现机制收紧（#4004），可能序列化为空对象，需冗余 @JsonProperty 或 @Jacksonized 显式声明。

---

## 4. @Slf4j 族与其他常用注解

日志注解族是"按门面生成字段"的典型：`@Slf4j`（SLF4J 门面，配合 Logback/Log4j2）、`@Log4j2`、`@Log`、`@CommonsLog`、`@XSlf4j`、`@JBossLog` 各自生成对应门面的 `log` 静态字段，属性 `topic` 可自定义 logger 名（默认类名）。核心价值不是少写一行字段，而是**字段名统一为 log**，团队代码搜索、静态检查规则都能依赖这个约定。

其他高频注解快速定位：

- `@SneakyThrows`：让受检异常绕过编译检查直接抛出，**异常栈照常可见**（不是吞掉），但会让"受检异常契约"形同虚设，公共 API 慎用；
- `@Cleanup`：为局部变量生成 try-finally 关闭逻辑（等价 try-with-resources 的旧式写法），JDK 7 后 TWR 已原生，仅存量代码可见；
- `@Synchronized`：生成私有锁对象上的同步块，避免锁 this 的公开性风险；
- `@NonNull`：生成参数 null 检查（NullPointerException），注意与 JSpecify `@NonNull`、Spring `@NonNull` 同名不同包——**Lombok 的 @NonNull 会生成运行时代码**，另两者只是编译期契约，这是三个 @NonNull 中唯一有运行时行为的。

---

## 5. 版本基线、风险与 record 分工

**版本基线（2026-08）**：Lombok 1.18.46（2026-04-22）——官方支持 JDK 26，@Jacksonized 修复 #4022/#3934，Jackson 3 支持收尾（1.18.44 引入）；构建已切换 EA_JDK 27。

**选型决策**：record（JDK 16+）已经原生覆盖 @Value 的核心场景（不可变数据载体 + 全参构造 + equals/hashCode），且与 Jackson/校验生态原生配合良好——**纯数据载体一律用 record，Lombok 的生存空间收缩到 record 无法覆盖的场景**：

- 需要可变 DTO + 链式 Builder（@Data/@Builder 组合）；
- 需要 @Slf4j 等便捷注解；
- 存量项目的习惯一致性。

最终建议是"新项目默认 record + 按需 Lombok，而非全面引入"。引入 Lombok 时必须锁版本并写入 JDK 升级检查单：**JDK 升大版本前先确认 Lombok 已发布兼容版本**，否则整个项目无法编译。

**lombok.config 的团队级配置**是工程化细节：项目根目录放 `lombok.config` 统一约束（`lombok.copyableAnnotations += com.example.Sensitive` 让自定义注解在生成的方法/构造器上**保留**——否则 Lombok 生成的构造器会丢掉参数上的自定义校验/脱敏注解，这是自研注解与 Lombok 组合的头号静默坑）；`config.stopBubbling = true` 防止父目录配置穿透；`lombok.equalsAndHashCode.callSuper = call` 全局修正继承场景的 equals 遗漏。团队引入 Lombok 应同时提交 config 文件与使用规范。

**面试速答**：「Lombok 生成的代码去哪了？」——不在源码里，处理器直接改 AST 后编译进字节码，javap 反编译可见；「@Data 为什么不能无脑用？」——equals/hashCode 全字段策略对 JPA 代理/可变实体危险、RequiredArgsConstructor 会与手写构造器冲突、继承场景漏 callSuper；「Lombok 与 record 怎么分工？」——不可变数据载体用 record，可变 DTO/Builder/日志注解场景用 Lombok；「@SneakyThrows 为什么有争议？」——它让受检异常绕过编译检查，调用方不知道可能抛什么——异常契约是受检异常的设计初衷，静默抹掉契约让公共 API 的使用者失去编译期保护。

---

**下一模块**：[04 Jackson 注解剖析](./04-Jackson注解剖析.md) · **返回总览**：[00 总览](./00-Java常用注解知识体系总览.md)

**相关体系**：[装箱拆箱 泛型擦除（record 与值类型）](../装箱拆箱%20泛型擦除/00-装箱拆箱与泛型擦除知识体系总览.md)

---

【参考来源】
- [Lombok Changelog（1.18.46 发布记录）](https://projectlombok.org/changelog)
- [projectlombok/lombok Issue #4004: Jackson 3.0.3 与 @Accessors(fluent=true) 的字段发现](https://github.com/projectlombok/lombok/issues/4004)
- [Lombok @Data API 文档（1.18.48-SNAPSHOT）](https://lars-sh.github.io/lombok-annotations/apidocs/lombok/Data.html)
- [Jackson 3 迁移指南（官方 MIGRATING_TO_JACKSON_3.md）](https://github.com/FasterXML/jackson/blob/master/jackson3/MIGRATING_TO_JACKSON_3.md)
- [Spring Boot 4 升级 Jackson 3 详解（包名/日期格式/配置迁移）](https://yunpan.plus/t/7941-1-1)
