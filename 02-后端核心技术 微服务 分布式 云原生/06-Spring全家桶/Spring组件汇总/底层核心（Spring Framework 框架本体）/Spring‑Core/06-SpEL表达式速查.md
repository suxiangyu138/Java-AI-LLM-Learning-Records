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

**SpEL 求值流程（源码级）**：

```text
parseExpression("...")  → ① 词法分析（SpelExpressionParser → InternalSpelExpressionParser）
                         ② 语法树构建（AST：SpelNodeImpl 节点树，如 OpPlus/PropertyOrFieldReference）
getValue(ctx)           → ③ 求值：自顶向下递归遍历 AST 节点
                         ④ 可选：SpelCompiler 将 AST 编译为字节码（IMMEDIATE/MIXED 模式）
                         ⑤ 结果转换：按目标类型（getValue(Class)）走 ConversionService
```

| 阶段 | 开销 | 优化方式 |
|------|------|---------|
| 解析 | 较高（一次性） | **缓存 Expression 实例**（解析一次，求值多次） |
| 求值 | 低（AST 遍历） | 编译模式（IMMEDIATE）转字节码 |
| 转换 | 中 | 目标类型一致时避免包装 |

```java
// 生产实践：表达式缓存（高并发场景必须）
private final Map<String, Expression> cache = new ConcurrentHashMap<>();
private final ExpressionParser parser = new SpelExpressionParser();

public String eval(String expr, EvaluationContext ctx) {
    return cache.computeIfAbsent(expr, parser::parseExpression)
            .getValue(ctx, String.class);     // 只缓存解析结果，ctx 每次传入
}
```

**适用边界**（什么时候该用 SpEL）：

| 场景 | 用 SpEL | 不用 SpEL 而用 |
|------|:---:|------|
| 注解属性（缓存键/条件） | ✅ | - |
| 动态规则（可配置规则引擎） | ⚠️ 受限上下文 | 规则引擎（Drools）/策略代码 |
| 通用脚本执行（复杂业务逻辑） | ❌ | Groovy/JavaScript 引擎 |
| 字符串模板 | ⚠️ 简单拼接可 | `MessageFormat`/模板引擎 |

> 💡 判断标准："**表达式是否寄生在 Spring 对象上**（Bean/属性/注解）"——是则 SpEL 合适；否则选通用脚本引擎。

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
| 赋值 | `=`（须在属性写入上下文） | `#order.status = 'paid'`（Standard 可用） |
| 安全导航 | `?.` 与 `?:` 组合 | `user?.age ?: 0` |
| 变量 | `#var` / `#root` | `#order.amount` |
| Bean 引用 | `@beanName`（需 BeanResolver） | `@orderService.getTotal()` |
| 安全导航组合 | `?.` + `?:` | `user?.age ?: 0` |

```java
// 集合操作经典案例：从列表选出成年用户并取姓名
List<String> names = (List<String>) parser
        .parseExpression("users.?[age >= 18].![name]")
        .getValue(ctx);
```

**模板表达式（TemplateExpression）**：`parseExpression` 默认整串是一个表达式；模板模式把字符串拆成"文本 + 表达式"交替段，`#{...}` 内是表达式、其余是纯文本：

```java
// 模板解析：parser 构造时开启（表达式部分用 #{} 包裹）
SpelExpressionParser parser = new SpelExpressionParser(
        new SpelParserConfiguration(null, null, false, true));   // 末参数 templateMode=true
Expression tpl = parser.parseExpression("Hello #{#user.name}, 余额 #{#user.balance} 元");
String msg = tpl.getValue(ctx, String.class);                     // "Hello 张三, 余额 100 元"
```

| 模式 | 写法 | 用途 |
|------|------|------|
| 普通表达式 | `"a + b"` | 整体计算 |
| 模板表达式 | `"文本 #{expr} 文本"` | 邮件/消息/文案动态拼接 |

> ⚠️ 模板模式与普通模式的区别要分清：模板里 `#{...}` 才是表达式；普通模式 `#var` 直接可用。**@Value 的 `#{}` 是普通模式**（整个值是一个表达式），不要与模板模式混淆——`"Hello #{name}"` 写在 @Value 里会因花括号语法报错，正确写法是 `"'Hello ' + #name"`。

> 💡 7.0 增强：`Optional` 值在空安全导航（`?.`）与 Elvis（`?:`）中**自动解包**——`optVal?.x` 不再需要手动 `.orElse(null)` 中转。

**语法深度示例**（直接可跑）：

```java
StandardEvaluationContext ctx = new StandardEvaluationContext();
ctx.setVariable("order", new Order(120, "paid"));
ctx.setVariable("threshold", 100);

// 三元 + 属性链 + 变量
parser.parseExpression("#order.amount > #threshold ? 'big' : 'small'")   // "big"

// 空安全 + Elvis 组合（7.0 Optional 自动解包）
parser.parseExpression("#order?.address?.city ?: 'unknown'")              // "unknown"

// 集合选择 + 投影 + 排序
parser.parseExpression("#orders.?[amount >= #threshold].![id]")           // 过滤+取列
parser.parseExpression("#orders.^[amount >= 100]")                        // 第一个匹配（^=first，$=last）

// Map 构造与访问
parser.parseExpression("{'a': 1, 'b': 2}['a'] + 1")                      // 2

// 正则与 instanceof
parser.parseExpression("'abc-123'.matches('[a-z]+-\\\\d+')")              // true
parser.parseExpression("'x' instanceof T(String)")                       // true

// 安全导航后的方法调用
parser.parseExpression("#order?.getItems()?.size() ?: 0")
```

| 运算符注意点 | 说明 |
|-------------|------|
| `and`/`or`/`not` | 用单词而非 `&&`/`\|\|`（两者也支持） |
| `?:` 与 `?.` 区别 | `?.` 防 NPE 短路；`?:` 提供默认值 |
| `^[`/`$[` | 取首个/末个匹配（`?[` 是全部） |
| 字符串转义 | 正则内 `\\d` 在 Java 字符串里要写成 `\\\\d` |
| 数字比较 | `1.0 == 1` 为 false（Double vs Integer 不相等）——用 `1.0 == 1L` 场景注意类型 |

> ⚠️ **常见语法错误**：① 单引号才是字符串字面量（双引号是模板场景）；② 属性名含特殊字符用 `['key']`；③ 方法调用返回 void 的表达式报错；④ 集合投影 `![name]` 对 null 元素抛 NPE（先 `?[name != null]` 过滤）——这些错误在**解析期**就暴露（SpelParseException），定位快。

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

**StandardEvaluationContext 能力详解**（框架内部使用姿势）：

```java
StandardEvaluationContext ctx = new StandardEvaluationContext();
ctx.setRootObject(order);                       // #root 指向的对象
ctx.setVariable("user", currentUser);           // #user 变量
ctx.registerFunction("toUpper", String.class.getMethod("toUpperCase"));  // 注册静态函数
ctx.setBeanResolver(new BeanFactoryResolver(beanFactory));  // @bean 引用支持（默认无！）
ctx.addPropertyAccessor(new MapAccessor());     // 自定义属性访问器（如 Map 键直访）
```

| 能力 | API | 生效条件 |
|------|-----|---------|
| Bean 引用 `@name` | `setBeanResolver` | **不设置则 @ 引用抛异常**（常见"为什么 @bean 不生效"根因） |
| 静态函数 | `registerFunction` | 方法与上下文共享（线程安全需注意注册后不再修改） |
| 属性访问扩展 | `PropertyAccessor` | Map/反射/JavaBean 三套默认；自定义实现可插拔 |
| 根对象 | `setRootObject` | `#root` 或裸属性访问的基础 |

**SimpleEvaluationContext 的能力边界**（安全默认的代价）：

| 能力 | Simple | Standard |
|------|:---:|:---:|
| 属性读取 | ✅（需显式启用） | ✅ |
| 属性写入 | ❌（只读构建器） | ✅ |
| 方法调用 | ❌ | ✅ |
| 类型引用 T() | ❌ | ✅ |
| 构造器 new | ❌ | ✅ |
| Bean 引用 @ | ❌ | ✅（需 BeanResolver） |
| 变量 #var | ✅ | ✅ |

> 💡 **取舍结论**：Simple 不是"残缺版"而是"安全版"——它面向"**数据读取型**表达式"（规则匹配、条件判断），Standard 面向"**框架内部受信表达式**"（@Value/缓存键，表达式作者是开发者自己）；团队动态规则一律 Simple + 只读。

**EvaluationContext 的线程安全与复用**：

| 上下文 | 线程安全 | 复用姿势 |
|--------|:---:|---------|
| `StandardEvaluationContext` | 读操作安全（属性访问器注册后不再变） | 可缓存复用；**避免运行期 setVariable 并发修改** |
| `SimpleEvaluationContext` | 同上 | 可缓存 |
| `Expression`（解析结果） | ✅ 不可变 | 全局缓存（见第 1 节） |

> ⚠️ **并发坑**：`StandardEvaluationContext.setVariable` 不是线程安全的"读时写入"——多线程共用上下文且各自 setVariable 会相互覆盖/竞争；正确姿势是**每请求新建上下文**（开销小）或上下文只读复用 + 变量用 `#root` 传入。同理 `registerFunction` 必须在初始化阶段完成。

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

**更多内建场景代码形态**：

```java
// 定时任务：cron 表达式动态化（Bean 提供）
@Scheduled(cron = "#{@cronProvider.getSchedule()}")
public void nightlySync() { ... }

// 安全表达式（Spring Security 方言，但基于 SpEL 语法）
@PreAuthorize("hasRole('ADMIN') or (#order.owner == authentication.name)")
public void cancelOrder(Order order) { ... }

// 缓存条件引用方法参数与返回值
@Cacheable(cacheNames = "stock", key = "#skuId",
           condition = "#skuId != null",
           unless = "#result == null || #result.quantity == 0")
public Stock getStock(String skuId) { ... }
```

| 场景 | SpEL 承担的角色 | 表达式作者 |
|------|----------------|-----------|
| @Value | 取值/计算 | 开发者 |
| @Cacheable key/condition/unless | 键生成 + 条件 | 开发者 |
| @Scheduled cron | 动态调度 | 开发者/运维 |
| @PreAuthorize | 鉴权条件 | 安全团队/开发者 |
| XML 配置 | 属性注入 | 开发者 |

> 💡 **共性规律**：所有场景的 SpEL 都是"**注解属性 + 方法上下文**"模式——表达式可访问方法参数（`#参数名`/`#p0`）、返回值（`#result`）、Bean（`@bean`）；理解这个统一模型后，换场景只是换关键字。

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

**编译模式（SpelCompilerMode）细节**：`IMMEDIATE` 在**首次解析后立即编译**（首次求值更慢、后续更快），`MIXED` 先解释执行、达到阈值后自动编译（平滑过渡）——两者都要求 **ClassLoader 可创建新类**（JDK 17+ 强封装下需配置 `--add-opens java.base/java.lang=ALL-UNNAMED` 或走 Spring 提供的 `SpelCompiler` 独立类加载方案）。编译模式适合"固定表达式 + 高频调用"，对动态拼接表达式收益有限（每次都要重新编译）。

> 🎯 **核心要点**：7.0.8 的 10,000 次操作上限是 SpEL 的"防炸弹"护栏——普通表达式（属性链/集合操作）远低于此；误伤场景（合法大表达式）显式调大配置即可。

### 5.1 安全纵深与生产实践

**表达式注入攻击面与防护**：

| 攻击面 | 攻击手法 | 防护 |
|--------|---------|------|
| 表达式内容来自用户 | 构造 `T(java.lang.Runtime).getRuntime().exec(...)` | SimpleEvaluationContext（禁方法/类型/构造） |
| 属性名来自用户 | `['xxx']` 注入恶意键 | 白名单校验属性名；禁用 Map 属性访问器 |
| Bean 名称来自用户 | `@evilBean` 引用任意 Bean | 不开 BeanResolver（Simple 默认无） |
| 编译模式利用 | 编译后的字节码绕过限制（历史漏洞 CVE-2022-22963 系） | 不可信输入一律不编译；升级到 7.x 修复线 |

```java
// 生产安全模板：动态规则引擎入口（白名单 + 只读 + 操作上限）
public boolean evaluate(String rule, Map<String, Object> data) {
    EvaluationContext ctx = SimpleEvaluationContext
            .forReadOnlyDataBinding()
            .withInstanceMethods()          // 若业务确需方法调用，逐个显式放行（谨慎）
            .build();
    data.forEach(ctx::setVariable);
    return Boolean.TRUE.equals(parser.parseExpression(rule).getValue(ctx, Boolean.class));
}
```

> ⚠️ **经验法则**："凡表达式字符串可能来自配置文件以外的地方，一律按不可信处理"；`StandardEvaluationContext` 只允许出现在**代码字面量**中（@Value/缓存注解的属性由开发者书写）。

**性能优化清单**：

| 手段 | 收益 | 适用 |
|------|------|------|
| 缓存 Expression 实例 | 省解析开销（最大头） | 一切重复求值 |
| 编译模式 IMMEDIATE | 求值转字节码（1.5-3 倍） | 高频表达式 + 独立类加载 |
| 避免每次 new Context | StandardEvaluationContext 可复用 | 单线程或同步使用 |
| 精确类型 getValue(Class) | 免装箱转换 | 返回值已知 |
| 深链拆分 | 减少单次操作数（防超限） | 复杂表达式 |

**7.0 操作上限的触发与调整**：

```yaml
# application.yml：全局调大（默认 10000）
spring:
  expression:
    maxOperations: 50000
```

| 触发表现 | 判断 |
|---------|------|
| `SpelEvaluationException: Maximum operations reached` | 表达式本身过大（循环/深链）——先优化表达式而非直接调大 |
| 合法大集合操作超限 | `orders.![...]` 对万级集合投影——按集合规模评估后调大 |
| 规则引擎批量求值超限 | 单次求值含太多操作——拆分子表达式 |

### 5.2 面试追问点

- **"SpEL 与 EL/JSP EL 的区别"**：SpEL 是独立的表达式语言体系（方法调用/集合操作/类型引用齐全），EL 是 JSP 视图层简化语法——SpEL 定位"框架内嵌脚本"，EL 定位"视图取值"；
- **"为什么 @Value 要分 #{} 与 ${}"**：`${}` 走占位符解析（属性源查找，字符串替换），`#{}` 走 SpEL（表达式计算，类型化）——前者"取值"、后者"计算"，组合 `${a:#{...}}` 是先取后算；
- **"SpEL 求值为什么慢"**：解析构建 AST 是主要开销（词法+语法分析）；求值本身是节点遍历，编译模式可消除大部分——所以**缓存解析结果**是第一优化；
- **"操作上限是防什么的"**：防**表达式炸弹**——构造深度嵌套/超大集合操作的表达式耗尽 CPU（CVE 系列攻击的通用变体）；10,000 是"正常业务永达不到、恶意表达式必然触发"的阈值；
- **"SimpleEvaluationContext 为什么默认禁方法调用"**：方法调用是 RCE 的载体（Runtime.exec/反射链）——禁方法 + 禁类型引用 + 禁构造 = 把攻击面压到"纯数据读取"；
- **"SpEL 在 AOT 下有什么变化"**：7.0 的 AOT 对注解中的 SpEL 表达式**编译期求值预计算**（能静态求值的直接内联，减少运行期开销）——但依赖运行期变量的表达式保持动态。

> 🎯 **全篇收束**：SpEL 的心智模型是"**受信脚本、分层放权**"——框架场景全量能力（Standard）、用户场景最小能力（Simple）、操作上限兜底防炸弹；面试答 SpEL 先讲语法三件套（属性/方法/集合），再讲上下文二分法，最后讲安全护栏——深度自然呈现。实践层面记住三句话：表达式缓存起来、不可信输入用 Simple、超限先优化表达式再调配置。若想系统梳理 Spring 全家桶表达式体系（含 Security 方言差异），可对照 [Spring框架核心-03（@Value 与 SpEL）](../../../Spring框架核心/03-依赖注入详解与自动装配.md) 深读。至此 06 篇完毕，下一站是重试与弹性——SpEL 是"表达"的引擎，retry 是"容错"的引擎，两者都属 core 的地基能力，也都是面试"Spring 7 新特性"的高频考点。

---

**下一模块**：[07-重试与弹性速查](07-重试与弹性速查.md)　**返回总览**：[00-Spring Core组件总览](00-Spring Core组件总览.md)
