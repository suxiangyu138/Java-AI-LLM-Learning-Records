# 00 Spring AOP 组件总览

> 组件卡片：Spring AOP 是什么、版本现状、能做什么、与深度体系如何衔接——本系列是"组件汇总"速查型文档，深挖见 [Spring框架核心-05-AOP面向切面编程](../../../Spring框架核心/05-AOP面向切面编程.md)

---

## 📚 目录

1. [组件一句话定位](#1-组件一句话定位)
2. [版本现状（2026-08）](#2-版本现状2026-08)
3. [能力地图](#3-能力地图)
4. [与深度体系的映射](#4-与深度体系的映射)
5. [快速上手 3 步](#5-快速上手-3-步)
6. [速查导航](#6-速查导航)
7. [学习路线推荐](#7-学习路线推荐)
8. [核心概念速查](#8-核心概念速查)

---

## 1. 组件一句话定位

**Spring AOP 是 Spring 生态的面向切面编程实现**——通过动态代理（CGLIB/JDK）在方法调用前后织入横切逻辑，是 @Transactional、@Cacheable、@Async、@PreAuthorize、@Retryable 等一切"注解即能力"的底层地基。

```text
核心心智模型：
  Bean 被代理包裹（ProxyFactory）
    ├── 切点 Pointcut："哪些方法"（表达式/注解匹配）
    ├── 通知 Advice："做什么"（Before/AfterReturning/AfterThrowing/Around）
    └── 切面 Advisor = 切点 + 通知 的组装单元

  调用链：外部调用 → 代理 → 环绕通知链 → 目标方法
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring Framework 底层核心模块（spring-aop） |
| 版本线 | 随 Spring Framework 7.x（2025-11 起） |
| 语言要求 | Java 17+ |
| 定位 | 横切关注点的声明式织入（事务/缓存/安全/异步/重试的底座） |

### 1.1 AOP 解决什么问题

面向对象（OOP）擅长纵向组织业务逻辑，却难以优雅处理"横向贯穿"的关注点——日志、事务、权限、监控几乎出现在每个业务方法中。若在每个方法中手写这些逻辑，会带来三重代价：**代码重复**（每个方法都要 try/catch 计时）、**业务污染**（核心逻辑被横切代码淹没）、**修改困难**（新增一个横切需求要改动大量方法）。AOP 把横切关注点抽取为独立"切面"，在运行时由代理统一织入，业务代码保持纯净。

| 维度 | OOP 纵向封装 | AOP 横切织入 |
|------|-------------|-------------|
| 关注点 | 类、继承、多态 | 事务、日志、安全、监控 |
| 代码位置 | 业务类内部 | 独立切面类 |
| 修改横切逻辑 | 改动每个业务方法 | 只改切面一处 |
| 运行时机制 | 普通方法调用 | 代理对象 + 通知链 |

### 1.2 声明式与编程式 AOP 的取舍

Spring AOP 提供两条使用路径，选型直接决定代码可维护性：

| 方式 | 入口 | 优点 | 缺点 | 适用 |
|------|------|------|------|------|
| 声明式 | `@Aspect` + 五类通知注解 | 配置即能力、易读易维护、团队规范统一 | 织入时机固定（容器启动时）；表达式出错难排查 | 业务切面（日志/审计/监控） |
| 编程式 | `ProxyFactory` / `AspectJProxyFactory` | 运行时动态织入、可按条件临时创建 | 样板代码多、易漏组装、难测试 | 框架开发、动态增强工具 |

> 💡 经验法则：**业务代码永远用声明式**；只有"代理对象本身需运行时动态决定"的场景（通用增强工具、测试桩）才用编程式。

### 1.3 适用与不适用场景

| 场景 | 适合 AOP？ | 说明 |
|------|:---:|------|
| 日志/审计/耗时监控 | ✅ | 典型横切，注解驱动 |
| 事务/缓存/安全/重试 | ✅ | Spring 声明式能力的地基 |
| 高频热路径（每秒百万级调用） | ⚠️ 慎用 | 代理链有调用开销，必要时考虑字节码级优化替代 |
| 需要修改对象内部状态的复杂逻辑 | ❌ | 应放业务层，切面只做"横切" |
| 需要编译期确定性的织入 | ❌ | 考虑 AspectJ 编译期织入（代价高，一般不值得） |

> 🎯 判断标准一句话：**"这个逻辑横着穿过了很多方法吗？"**——是，用 AOP；否，别硬塞进切面。

## 2. 版本现状（2026-08）

| 版本 | 说明 |
|------|------|
| Spring Framework 7.0（2025-11） | 当前主线（Boot 4.0 配套） |
| Spring Framework 7.1（2026-05） | 最新维护线（Boot 4.1 配套） |
| Spring Framework 6.x | 存量主线（Boot 3.x 配套） |

**7.0 AOP 关键变化：**

- **`@Proxyable` 注解（新）**：按 Bean 指定代理类型（如 `@Proxyable(proxyTargetClass = true)`），细粒度控制代理方式；
- **接口代理 Bean 属性（新）**：BeanDefinition 级属性可强制接口代理（与 @Proxyable 互补）；
- **CGLIB 一致默认**：Spring Boot 全面统一用 CGLIB 代理——@Retryable、@Async 等一切代理注解与 @Transactional/@Cacheable 行为一致（不再出现"有些注解是 JDK 代理、有些是 CGLIB"的混差）。

> ⚠️ **要点**：7.0 的 AOP 变化是"一致性收敛"——代理方式统一 CGLIB、按需用 @Proxyable 微调，不需要业务代码大改；升级主要影响"依赖 JDK 接口代理"的存量代码。

### 2.1 6.x 与 7.x 代理行为对照

| 行为 | Spring 6.x（Boot 3.x） | Spring 7.x（Boot 4.x） |
|------|----------------------|----------------------|
| 全局默认代理 | 依赖 proxy-target-class 配置 | 统一 CGLIB |
| 各类注解代理方式 | @Transactional 与 @Async 等存在混差 | 全部一致 CGLIB |
| 按 Bean 指定代理类型 | 不支持 | `@Proxyable`（7.0 新） |
| 升级主要风险 | — | 依赖 JDK 代理语义的存量代码（类型强转、Proxy.isProxyClass 判断） |

> 💡 6.x 时代"同一类上 @Transactional 生效但 @Async 不生效"是高频疑难杂症，根源就是两类注解走了不同的代理链路；7.x 统一后该问题从根上消失。

### 2.2 升级 7.x 的 AOP 检查清单

| 检查项 | 说明 |
|--------|------|
| 类型强转代码 | 依赖 JDK 代理时强转具体类会失败（CGLIB 代理类是子类，可强转，行为有差异） |
| `Proxy.isProxyClass` 判断 | 统一 CGLIB 后该判断返回 false，依赖它的分支逻辑失效 |
| 序列化逻辑 | CGLIB 子类与接口代理序列化结果不同，缓存/网络传输前需验证 |
| 第三方库 AOP 假设 | 审计依赖"接口代理语义"的库升级说明 |
| 测试桩与断言 | Mockito/代理相关测试中"对象类型"断言需同步调整 |

### 2.3 关键演进时间线

| 版本 | AOP 里程碑 |
|------|-----------|
| Spring 2.0 | 引入 @AspectJ 注解式 AOP（借 AspectJ 语法） |
| Spring 4.x | CGLIB 在 Boot 生态中逐步成为默认 |
| Spring 5.2.7 | 同切面内通知顺序调整（@Around 先于 @Before） |
| Spring 6.x | 基线与 Boot 3.x 配套，稳定性打磨期 |
| Spring 7.0 | `@Proxyable` + 全部代理注解统一 CGLIB |

> 💡 演进主线：语法二十年没大变，**"一致性"（代理方式、执行顺序）逐年收敛**——理解这条主线，比背每个版本的细节更能回答"为什么 7.x 这么设计"。

## 3. 能力地图

| 能力域 | 能力点 | 对应注解/API |
|--------|--------|-------------|
| 切面定义 | 注解式切面 | `@Aspect`、`@Pointcut` |
| 通知类型 | 五种通知 | `@Before`/`@AfterReturning`/`@AfterThrowing`/`@After`/`@Around` |
| 切点表达式 | 方法/类型/注解匹配 | `execution(...)`、`@annotation(...)`、`within(...)` |
| 织入机制 | 动态代理（CGLIB 默认） | `ProxyFactory`、`ProxyFactoryBean` |
| 声明式能力 | 事务/缓存/异步/安全/重试 | @Transactional、@Cacheable、@Async、@PreAuthorize、@Retryable |
| 编程式 AOP | 运行时织入 | `AopContext`、`AspectJProxyFactory` |
| 配置 | 代理类型开关 | `spring.aop.proxy-target-class`、`@Proxyable`（7.0） |

### 3.1 织入边界速查

Spring AOP 的织入边界一句话：**代理只增强"从容器外部、经代理对象、调用的 public 非 final 实例方法"**。对照四问：

| 问题 | 边界结论 |
|------|---------|
| 哪些方法可被代理？ | public 非 final 实例方法（CGLIB 可覆写 protected，一般不建议） |
| 哪些方法不可被代理？ | private / static / final 方法 |
| 哪些调用走代理？ | 外部 Bean 注入后发起的方法调用 |
| 哪些调用绕过代理？ | `this.method()` 自调用、`new` 出来的对象、反射直调目标类 |
| 哪些 Bean 可被代理？ | 容器管理的 Bean（`new` 出来的对象不在容器里，无从代理） |
| 多个切面可叠加吗？ | 可，命中同一方法时按 Advisor 链顺序执行 |

> ⚠️ 表中"哪些调用绕过代理"一行是生产事故第一来源——**凡是 `new` 出来或反射直调的对象，注解一律不生效**，排查时先问"对象哪来的"。

### 3.2 与其他横切技术的边界

AOP 不是唯一的"横切"手段，与相近技术划清边界有助于选型：

| 技术 | 粒度 | 与 AOP 的关系 |
|------|------|--------------|
| Servlet Filter / 拦截器 | 请求级别 | Web 层横切；AOP 粒度更细（方法级） |
| BeanPostProcessor | Bean 生命周期 | AOP 的载体——AutoProxyCreator 本身就是一个 BPP |
| 装饰器模式 | 对象级 | 手工代理，AOP 是其声明式、可复用的版本 |
| Spring 事件 | 解耦通知 | 观察者模式，不依赖 AOP（事务事件除外） |

> 💡 选型参考：**请求级横切（鉴权/日志头）用 Filter，方法级横切（事务/缓存/审计）用 AOP**——两者互补而非互斥。

## 4. 与深度体系的映射

| 速查文档 | 深度体系模块 |
|---------|-------------|
| 01-模块清单 | [Spring框架核心-05-AOP](../../../Spring框架核心/05-AOP面向切面编程.md) |
| 02-核心类与注解速查 | [Spring生态深度剖析-04-AOP代理创建与事务管理内核](../../../Spring生态深度剖析/04-AOP代理创建与事务管理内核.md) |
| 03-配置属性速查 | [Spring框架核心-05](../../../Spring框架核心/05-AOP面向切面编程.md) |
| 04-集成地图与常见问题 | [Spring Core-06-@Configuration与AOP代理机制](../../../Spring Core/06-@Configuration与AOP代理机制.md) |

> 💡 本系列定位"查得快"，深度体系定位"学得透"——速查命中后，跳转深度体系看机制与源码。

### 4.1 从问题到文档的查阅路径

| 你的问题 | 先查哪篇 | 还不懂再看 |
|---------|---------|-----------|
| 切面/通知怎么写？ | 02-核心类与注解速查 | [Spring框架核心-05-AOP](../../../Spring框架核心/05-AOP面向切面编程.md) |
| 代理方式怎么配？ | 03-配置属性速查 | 同上 |
| 注解不生效怎么查？ | 04-集成地图与常见问题 | [Spring生态深度剖析-04](../../../Spring生态深度剖析/04-AOP代理创建与事务管理内核.md) |
| 依赖怎么引？ | 01-模块清单 | — |

## 5. 快速上手 3 步

```xml
<!-- ① 依赖（Boot 项目 starter 已内置，无需单独引） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

```java
// ② 定义切面（记录日志为例）
@Aspect
@Component
public class LogAspect {

    @Around("@annotation(com.demo.annotation.SysLog)")   // 匹配带 @SysLog 的方法
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();                        // 执行目标方法
        } finally {
            System.out.println(pjp.getSignature().getName() + " cost "
                    + (System.currentTimeMillis() - start) + "ms");
        }
    }
}
```

```java
// ③ 使用（注解即织入）
@Service
public class OrderService {
    @SysLog
    public void create(Order order) { ... }
}
```

> ⚠️ **上线前必查**：**同类自调用 `this.method()` 绕过代理**（切面不生效）——注入自身代理或拆类；private/final 方法不可被代理。

### 5.1 完整五通知示例

以"服务层耗时监控切面"为例，演示五种通知的完整写法与参数绑定：

```java
@Aspect
@Component
@Order(10)                                // 数值小的切面先执行
public class ServiceMonitorAspect {

    @Pointcut("execution(* com.demo.service..*.*(..))")   // ① 可复用切点
    public void serviceLayer() { }

    @Before("serviceLayer()")                             // ② 前置：目标执行前
    public void before(JoinPoint jp) {
        System.out.println("[before] " + jp.getSignature().getName());
    }

    @AfterReturning(pointcut = "serviceLayer()", returning = "result")
    public void afterReturning(JoinPoint jp, Object result) {
        System.out.println("[afterReturning] result = " + result);   // ③ 正常返回后
    }

    @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
    public void afterThrowing(JoinPoint jp, Exception ex) {
        System.out.println("[afterThrowing] " + ex.getMessage());    // ④ 抛异常后
    }

    @After("serviceLayer()")                              // ⑤ 最终：finally 语义
    public void after(JoinPoint jp) {
        System.out.println("[after] 无论成败都执行");
    }

    @Around("serviceLayer()")                             // ⑥ 环绕：包裹以上全部
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();                         // 必须调用，否则目标不执行
        } finally {
            System.out.println("cost: " + (System.currentTimeMillis() - start) + "ms");
        }
    }
}
```

> 🎯 同一方法命中多个通知时：`@Around` 前半段 → `@Before` → 目标方法 → `@AfterReturning`（成功）/ `@AfterThrowing`（异常）→ `@After` → `@Around` 后半段——环绕通知是"包在外面的一层壳"。

### 5.2 三种常用切点模板

| 模板 | 表达式 | 适用场景 |
|------|--------|---------|
| 注解驱动 | `@annotation(com.demo.annotation.SysLog)` | 自定义注解 + 切面（最常用，与业务解耦） |
| 包级拦截 | `execution(* com.demo.service..*.*(..))` | 整层统一处理（审计/监控/耗时） |
| Bean 定向 | `bean(orderService)` | 单 Bean 精准切面（匹配最快） |

### 5.3 上线前五分钟自检

| 检查项 | 自查方式 |
|--------|---------|
| 依赖就绪 | @Aspect 场景确认 spring-boot-starter-aop 已引入 |
| 切面注册 | 切面类有 @Component（或被 @Bean 注册） |
| 表达式正确 | 先用最宽表达式验证命中，再逐步收紧 |
| 方法可代理 | 目标方法 public 非 final，类非 final |
| 调用路径 | 确认从外部 Bean 经代理调用（无 this 自调用） |
| 代理方式 | 默认 CGLIB 即正确，例外用 @Proxyable 标注 |
| 自调用扫描 | 全局搜索 `this.` 调用本类代理方法的写法（IDE 支持查找） |
| 版本核对 | 确认 Spring/Boot 版本配套（7.x 代理行为统一 CGLIB） |

> 💡 把这六项做成团队评审的 AOP 检查单，能拦下绝大多数"注解不生效"事故——成本远低于线上排障。

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单](01-模块清单.md) | spring-aop 及相关 artifact 依赖体系 |
| [02-核心类与注解速查](02-核心类与注解速查.md) | 代理/切点/通知/切面关键 API 与注解 |
| [03-配置属性速查](03-配置属性速查.md) | spring.aop.* 属性与 @Proxyable（7.0） |
| [04-集成地图与常见问题](04-集成地图与常见问题.md) | 与事务/缓存/异步联动 + 代理失效坑 |

### 6.1 快速记忆卡

| 一句话 | 内容 |
|--------|------|
| 代理三条件 | 外部调用 + public + 非 final |
| 顺序一句话 | @Before 正序、@After 逆序、@Around 包裹 |
| 失效一句话 | `this` 调用 = 绕过代理 |
| 7.x 一句话 | 全 CGLIB + @Proxyable 微调例外 |
| 排障一句话 | 先答三问：代理存在吗？外部调的吗？切点命中吗？ |
| 依赖一句话 | Boot 用 starter-aop，纯 Spring 加 aspects |

## 7. 学习路线推荐

| 路线 | 适用 | 顺序 |
|------|------|------|
| 入门速查 | 理解 AOP 是什么 | 00 总览 → 02 核心类与注解 → 01 模块清单 → 04 常见问题 |
| 项目实践 | 用注解/排查失效 | 02 速查 → 03 配置属性 → 04 集成地图 → [Spring框架核心-05-AOP](../../../Spring框架核心/05-AOP面向切面编程.md) |
| 面试冲刺 | 代理原理考点 | 02 代理机制 → 04 自调用失效 → [Spring生态深度剖析-04](../../../Spring生态深度剖析/04-AOP代理创建与事务管理内核.md) |

## 8. 核心概念速查

| 术语 | 一句话解释 |
|------|-----------|
| 切点 Pointcut | "哪些方法"被拦截（表达式/注解匹配） |
| 通知 Advice | "做什么"（Before/After/AfterReturning/AfterThrowing/Around） |
| 切面 Advisor | 切点 + 通知的组装单元 |
| 织入 | 代理创建时把通知挂到方法调用链 |
| JDK 代理 / CGLIB | 接口代理 vs 类代理（7.0 默认 CGLIB） |
| @Proxyable（7.0） | 按 Bean 指定代理类型的注解 |
| 自调用失效 | this.method() 绕过代理 → 切面不生效 |
| AutoProxyCreator | BeanPostProcessor 中创建代理的组件 |

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页
