# 06 SpEL 表达式速查

> 语法速查、求值上下文、安全限制（7.0 十万次上限）与使用场景——"Spring 内建表达式语言"

---

## 📚 目录

1. [SpEL 是什么](#1-spel-是什么)
2. [语法速查表](#2-语法速查表)
3. [求值上下文：Standard vs Simple](#3-求值上下文standard-vs-simple)
4. [Spring 内建使用场景](#4-spring-内建使用场景)
5. [安全与性能（7.0 操作上限）](#5-安全与性能70-操作上限)

---

## 1. SpEL 是什么

**SpEL（Spring Expression Language）** 是 Spring 内建表达式语言（`org.springframework.core.expression`），在编译期/运行期求值，广泛用于 `@Value("#{...}")`、缓存键、鉴权表达式、XML 配置等。

```java
// 三步使用：解析 → 求值
ExpressionParser parser = new SpelExpressionParser();
Expression exp = parser.parseExpression("'Hello ' + name.toUpperCase()");
StandardEvaluationContext ctx = new StandardEvaluationContext();
ctx.setVariable("name", "world");
String result = exp.getValue(ctx, String.class);     // "Hello WORLD"
```

> 🎯 **核心要点**：SpEL 不是通用脚本引擎——它**寄生在 Spring 语义**上（Bean 引用、注解属性、方法调用受权限控制）；`#{}` 在 @Value 中与 `${}`（占位符）互补：`${}` 取配置值，`#{}` 算表达式值，`${x:#{...}}` 可嵌套。

## 2. 语法速查表

| 分类 | 语法 | 示例 |
|------|------|------|
| 字面量 | `'str'` / `1` / `1.5` / `true` | `'hello'` |
| 属性/嵌套 | `.` / `[]` | `user.name`、`user['name']` |
| 方法调用 | `m(args)` | `'abc'.length()` |
| 运算符 | 算术/比较/逻辑 | `a > 1 ? 'big' : 'small'`、`x instanceof T(String)` |
| 空安全导航 | `?.`（7.0 对 Optional 自动解包） | `user?.address?.city` |
| Elvis | `?:` | `name ?: 'default'` |
| 类型引用 | `T(Type)` | `T(java.lang.Math).PI` |
| 构造 | `new Type(...)` | `new java.util.Date()` |
| 集合 | 选择 `?[ ]`、投影 `![ ]`、索引 `[0]`、切片 `[1..2]` | `users.?[age > 18]`、`users.![name]` |
| Map | `{'k': v}`、`map['k']` | `{'a': 1, 'b': 2}['a']` |
| 正则 | `matches` | `'abc'.matches('[a-z]+')` |
| 模板 | 字符串拼接表达式 | `"Hello #{name}"`（TemplateExpression） |
| 变量 | `#var` / `#root` | `#order.amount` |
| Bean 引用 | `@beanName`（需 BeanResolver） | `@orderService.getTotal()` |
| 安全导航组合 | `?.` + `?:` | `user?.age ?: 0` |

```java
// 集合操作经典案例：从列表选出成年用户并取姓名
List<String> names = (List<String>) parser
        .parseExpression("users.?[age >= 18].![name]")
        .getValue(ctx);
```

> 💡 7.0 增强：`Optional` 值在空安全导航（`?.`）与 Elvis（`?:`）中**自动解包**——`optVal?.x` 不再需要手动 `.orElse(null)` 中转。

## 3. 求值上下文：Standard vs Simple

| 上下文 | 能力 | 适用 |
|--------|------|------|
| `StandardEvaluationContext` | 全量：方法调用、类型引用、构造器、BeanResolver、PropertyAccessor | ✅ Spring 内部（@Value/缓存键）；**受信表达式** |
| `SimpleEvaluationContext` | 受限：仅属性访问/基本运算（**默认拒绝方法调用/类型引用/构造器**） | ✅ 用户输入/不可信表达式（安全默认） |

```java
// 安全姿势：处理外部输入必须用 SimpleEvaluationContext
EvaluationContext ctx = SimpleEvaluationContext
        .forReadOnlyDataBinding()          // 只读属性 + 基本运算
        .build();
```

> ⚠️ **安全红线**：`StandardEvaluationContext` 可调用任意方法（含 `Runtime.exec` 类危险路径）——**禁止**用它对用户输入求值；动态规则引擎/表达式网关一律 `SimpleEvaluationContext`，且配 `spelcompiler` 关闭。

## 4. Spring 内建使用场景

| 场景 | 写法 | 归属 |
|------|------|------|
| @Value | `@Value("#{systemProperties['user.dir']}")` | spring-beans（context 解析） |
| 缓存键 | `@Cacheable(key = "#order.id + '-' + #order.type")` | spring-context 缓存 |
| 缓存条件 | `@Cacheable(condition = "#order.amount > 100")` | 同上 |
| 事务表达式 | `@Transactional` 相关 SpEL 少用（如编程式模板） | spring-tx |
| 鉴权 | `@PreAuthorize("hasRole('ADMIN')")`（安全表达式另有方言） | spring-security |
| XML | `<property name="x" value="#{beanName.prop}"/>` | beans |
| 定时 | `@Scheduled(cron = "#{@cronProvider.get()}")` | context 调度 |

```java
// 缓存键 + 条件 + 参数 SpEL 组合
@Cacheable(cacheNames = "orders", key = "#orderId", 
           condition = "#orderId != null", unless = "#result == null")
public Order getOrder(Long orderId) { ... }
```

> 💡 面试考点：缓存 key 用 SpEL 而非字符串拼接——**类型安全、null 安全（`?.`）、可引用 Bean（`@bean`）**；`#result` 引用方法返回值（unless 场景）。

## 5. 安全与性能（7.0 操作上限）

| 风险 | 说明 | 对策 |
|------|------|------|
| 表达式炸弹 | 深嵌套/大量方法调用导致 CPU 膨胀 | ✅ 7.0.8 起**单次求值操作上限 10,000**（默认），超限抛异常 |
| 危险方法调用 | StandardEvaluationContext 可调任意方法 | 不可信输入用 SimpleEvaluationContext |
| 编译模式 | `SpelCompilerMode.IMMEDIATE` 可编译为字节码提速 | 高频表达式开启（要求独立类加载） |
| 循环引用 | 表达式引用 Bean 造成 A→B→A 求值环 | 表达式保持浅层属性访问 |

```java
// 7.0 操作上限配置（默认 10000）
SpelParserConfiguration config = new SpelParserConfiguration(
        SpelCompilerMode.OFF, null, 50_000);          // 显式调大
ExpressionParser parser = new SpelExpressionParser(config);

// 或全局 JVM 属性
// -Dspring.expression.maxOperations=50000
```

> 🎯 **核心要点**：7.0.8 的 10,000 次操作上限是 SpEL 的"防炸弹"护栏——普通表达式（属性链/集合操作）远低于此；误伤场景（合法大表达式）显式调大配置即可。

---

**下一模块**：[07-重试与弹性速查](07-重试与弹性速查.md)　**返回总览**：[00-Spring Core组件总览](00-Spring Core组件总览.md)
