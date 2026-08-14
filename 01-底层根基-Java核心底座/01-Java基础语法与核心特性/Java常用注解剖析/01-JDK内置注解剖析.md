# 01 JDK 内置注解剖析

> JDK 内置注解分三类：编译期契约（@Override/@Deprecated/@FunctionalInterface，编译器校验语义）、告警抑制（@SuppressWarnings/@SafeVarargs，干预编译器诊断）、元注解（定义注解的注解）。它们自身几乎不含逻辑，价值全在「javac 如何处理它们」——这正是面试区分"用过"与"懂原理"的分水岭

## 📚 目录

1. [标记类注解：编译期契约](#1-标记类注解编译期契约)
2. [抑制类注解：与编译器讨价还价](#2-抑制类注解与编译器讨价还价)
3. [元注解五件套](#3-元注解五件套)
4. [注解元素的设计约束](#4-注解元素的设计约束)
5. [高频坑与面试题](#5-高频坑与面试题)

---

## 1. 标记类注解：编译期契约

### 1.1 @Override——最常用的编译期保险

`@Override` 的 `@Retention(SOURCE)` 意味着它只存在于源码阶段，字节码里毫无痕迹。它的全部价值在于触发 javac 的**重写校验**：目标方法必须真正覆盖父类（或接口，Java 6 起支持）中签名一致的方法，否则编译失败。这个校验实际上替开发者挡住了三类经典事故：

- 参数类型拼错导致悄悄变成重载而非重写，多态调用失效后极难排查；
- 父类方法改名后子类残留"孤儿方法"，逻辑仍然执行但语义已断裂；
- 重构时方法签名调整，所有覆盖点被编译器逐一暴露。

从字节码角度，重写校验依赖 `Method` 的结构匹配（名称 + 参数 + 返回值协变），与注解无关——注解只是请求校验的开关。因此 `@Override` 是"零成本保险"：运行时无反射开销、无字节码膨胀，理应无条件使用。

### 1.2 @Deprecated——从"标记过时"到"标记移除"

`@Deprecated` 是少数 `@Retention(RUNTIME)` 的内置标记（反射可读），Java 9 起新增两个属性：

```java
@Deprecated(since = "9", forRemoval = true)
public final class Observer { }
```

`since` 声明废弃起始版本（生成文档），`forRemoval` 是语义更强的承诺：**该 API 将在未来版本被删除**。二者对编译器的告警强度不同——`forRemoval = true` 时，即使代码包裹在 `@SuppressWarnings("deprecation")` 中，javac 仍会给出 removal 告警，这是 JDK 9 后的显式设计，防止"抑制掉一个即将删除的 API 的使用"。

JDK 26 时代的实用意义：JDK 的 `forRemoval = true` 清单就是升级必读清单（例如 Thread API 的过时成员、SecurityManager 相关 API）。生产规范上，自研 SDK 标记废弃时应同时给出替代方案（Javadoc `@deprecated` 标签），否则下游无从迁移。

RUNTIME 级 Retention 让工具链可以反射扫描废弃 API 使用情况，这是升级治理的自动化抓手：

```java
// 构建期扫描依赖中的 forRemoval 使用点
Deprecated d = OldApi.class.getAnnotation(Deprecated.class);
if (d != null && d.forRemoval()) {
    log.warn("使用了即将移除的 API: {} (since {})", OldApi.class.getName(), d.since());
}
```

### 1.3 @FunctionalInterface——SAM 契约守护

`@FunctionalInterface` 强制接口只有一个抽象方法（SAM），编译器会拒绝第二种抽象方法的加入。它不改变字节码（接口本来就可以被 Lambda 实现），价值在于**契约锁定**：防止后续维护者无意中添加抽象方法，破坏所有既有 Lambda 表达式调用点。

注意一个反直觉事实：**@FunctionalInterface 是可选的**。任何仅含一个抽象方法的接口自动具备 Lambda 能力；加注解只是让编译器替你守住这个结构。接口演化规则同理：抽象方法一旦增多，此前所有 `(x) -> ...` 调用点全部编译失败——这正是 JDK 在 Comparator 等接口上坚持单抽象方法的原因。

> 🎯 **核心要点**：三个标记注解都是"编译器契约"——@Override 锁多态、@FunctionalInterface 锁 SAM 结构、@Deprecated(forRemoval) 锁移除预期。它们运行时零开销，属于无条件使用的注解。

---

## 2. 抑制类注解：与编译器讨价还价

### 2.1 @SuppressWarnings——最小范围的告警抑制

`value` 是字符串数组，常用值：`"unchecked"`（类型擦除导致的未检查转换）、`"rawtypes"`（原始类型）、`"deprecation"`、`"serial"`、`"unused"`。三个工程纪律：

1. **作用域最小化**：优先压到单条语句或单个字段，而不是整类——整类抑制会掩盖同一告警类别下的真实问题；
2. **拼写即有效**：字符串不是枚举，`@SuppressWarnings("uncheckd")` 拼错会**静默无效**，编译器不报错；
3. **必须有理由**：每个 `@SuppressWarnings` 旁应注释为什么这里可以安全抑制（例如"已通过 instanceof 校验"），否则它是技术债的温床。

### 2.2 @SafeVarargs——泛型可变参数的免责声明

`@SafeVarargs` 抑制的是"泛型可变参数可能造成堆污染"的告警，三个使用硬约束：

- 只能加在 **static、final、private**（Java 9 起放开 private）方法或构造器上——因为可被覆盖的方法无法保证子类行为安全；
- 方法体必须真的**不向可变参数数组写入**：`varargs` 数组是编译器创建的共享数组，写入会污染后续调用者的视图；
- 违反上述约束时注解只是"承诺"，编译器不验证——误用仍会埋下 `ArrayStoreException` 或堆污染的运行时炸弹。

```java
@SafeVarargs
static <T> List<T> ofAll(T... items) {   // 只读消费 items，安全
    return Collections.unmodifiableList(Arrays.asList(items));
}
```

> ⚠️ **陷阱**：`@SafeVarargs` 与 `@SuppressWarnings("unchecked")` 的区别是本质的——前者是"向编译器声明方法实现安全"，后者是"屏蔽编译器诊断"。前者有使用位置限制，后者无限制。

---

## 3. 元注解五件套

元注解决定自定义注解的"元属性"，是 10 篇自定义注解工程实战的设计基础：

- **@Target**：11 个 `ElementType` 常量中，Java 8 新增的 `TYPE_USE` 最重要——允许注解出现在任何类型用法上（泛型实参、cast、`new` 前），配合 `TYPE_PARAMETER` 支撑了 Checker Framework 等类型检查器生态；未标注 @Target 时注解可用于所有位置。`TYPE_USE` 与 `TYPE_PARAMETER` 的差别在一个具体例子：

```java
List<@NonNull String> list;     // TYPE_USE：标注在类型使用处（String 这个用法上）
class Box<@NonNull T> { }       // TYPE_PARAMETER：标注在类型参数声明处（T 本身）
```

不声明 `TYPE_USE` 的注解无法出现在泛型实参里——设计面向类型检查器的注解时，@Target 必须同时包含 `TYPE_USE`；
- **@Retention**：三档 `SOURCE / CLASS / RUNTIME`。**默认是 CLASS**——这是最常见的元注解错误：忘写 @Retention(RUNTIME)，运行时反射 `getAnnotation()` 返回 null，框架静默失明。选档口诀：只给编译器看选 SOURCE（@Override）；给 APT 编译期处理选 CLASS（Lombok）；要反射读选 RUNTIME（Spring/Jackson/校验）；
- **@Documented**：让注解出现在 Javadoc 中，纯文档作用，自定义公开 API 注解建议加上；
- **@Inherited**：**仅对类继承有效**——子类继承父类上的注解，但接口实现、接口继承、方法重写均不继承。且 JDK 的 `getAnnotationsByType()` 在查询"注解的注解"（元注解链）时会特殊处理 `@Inherited` 的传播，`getDeclaredAnnotations()` 则完全不传播；
- **@Repeatable**：Java 8 起允许同位置重复标注，机制是**容器注解**——`@Repeatable(Schedules.class)` 要求指定一个 `value()` 返回本注解数组的容器。反射侧注意：`getAnnotation(Schedules.class)` 拿到容器，`getAnnotationsByType(Schedule.class)` 才是正确姿势，直接 `getAnnotation(Schedule.class)` 在重复标注时会抛 `AnnotationFormatError` 或返回 null（取决于实现）。

`@Native`（JDK 8 引入，SOURCE 级）标记可从 native 代码读取的常量字段，文档性质，实际开发几乎不接触。

---

## 4. 注解元素的设计约束

自定义注解的元素（`@interface` 中的"方法"）受编译器硬约束，这些约束直接塑造了注解 API 的设计风格：

1. **返回类型八选一**：基本类型、String、Class、枚举、注解类型、以上类型的数组——不能是包装类（`Integer` 不行，`int` 可以）、不能是自定义对象、不能是泛型（`List<String>` 不行）；
2. **不支持 throws**、元素隐式 `public abstract`；
3. **属性值必须编译期常量**：`@Retention(RUNTIME + 1)` 这种表达式会编译失败，所有值在编译期固化进 `AnnotationDefault` 属性；
4. **default 惯例**：`value` 是惯例属性名，唯一允许省略属性名的写法是 `@MyAnn("x")`（等价 `@MyAnn(value = "x")`）。

---

## 5. 高频坑与面试题

1. **@Retention 默认 CLASS**：自定义注解不加 @Retention 或只加 @Retention(CLASS)，运行时反射永远读不到——排查方向：先 javap 看字节码 `RuntimeVisibleAnnotations` 是否出现；
2. **@Inherited 的三不继承**：接口实现不继承（`interface A {@Ann} ...; class B implements A` 中 B 读不到 @Ann）、接口继承不继承、方法重写不继承；
3. **@Repeatable 与 getAnnotation**：重复标注后 `getAnnotation(单注解.class)` 会抛 `AnnotationFormatError`，必须 `getAnnotationsByType()`；
4. **@SuppressWarnings 拼写错误静默无效**：没有任何机制报错，只能靠 code review；
5. **面试必答框架**：「@Override 的 Retention 是什么？为什么 SOURCE 就够？」——因为消费者是 javac 而非运行时，字节码中保留无意义；追问「哪些内置注解是 RUNTIME？」——@Deprecated（反射可查询）与 @FunctionalInterface 之外的大部分标记（@SafeVarargs/@SuppressWarnings/@Override 均为 SOURCE 或 CLASS 级）；
6. **「@SafeVarargs 为什么限制 static/final/private？」**——varargs 数组是编译器创建的共享数组，方法体写入会污染调用方；可覆盖的方法无法保证子类遵守"只读"承诺，所以注解只允许标在不可覆盖的方法上——本质是"能力越可验证，承诺越可信"；
7. **「注解类型能继承注解吗？」**——不能。@interface 编译后是 `interface extends java.lang.annotation.Annotation`，注解之间**没有继承关系**；"继承"的唯一机制是 @Inherited 元注解，且仅限类继承链——所以自定义注解想获得"派生"能力，靠的是 Spring 式元注解组合（10 篇详述），不是语言层面的继承。

---

**下一模块**：[02 JVM 内部注解深潜](./02-JVM内部注解深潜.md) · **返回总览**：[00 总览](./00-Java常用注解知识体系总览.md)

**相关体系**：[泛型 反射 注解（注解本质与 APT 原理）](../泛型%20反射%20注解/00-泛型反射注解知识体系总览.md)

---

【参考来源】
- [JDK 26 Release Notes（Oracle）](https://www.oracle.com/java/technologies/javase/26-relnote-issues.html)
- [What's New in Java 26 | JRebel](https://www.jrebel.com/blog/java-26)
- [Inside Java Newscast #106: LazyConstants in JDK 26](https://inside.java/2026/02/05/newscast-106/)
- [OpenJDK: jdk.internal.vm.annotation JavaDoc（@Stable 等）](https://apidia.net/java/OpenJDK/24/jdk.internal.vm.annotation.html)
- [JDK-8144223: Move j.l.invoke.{ForceInline, DontInline, Stable} to jdk.internal.vm.annotation](https://marc.info/?l=openjdk-hotspot-compiler-dev&m=144890532712845&w=3)
