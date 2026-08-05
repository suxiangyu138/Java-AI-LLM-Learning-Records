# 05 AOP 面向切面编程

> AOP 把横切关注点（日志、事务、鉴权、监控）从业务代码中剥离——切点、通知、代理模型与自调用陷阱，是 AOP 的完整地图

---

## 📚 目录

1. [AOP 解决的问题与核心概念](#1-aop-解决的问题与核心概念)
2. [切点表达式 Pointcut 语法全解](#2-切点表达式-pointcut-语法全解)
3. [五类通知与执行顺序](#3-五类通知与执行顺序)
4. [注解驱动 @Aspect 实战](#4-注解驱动-aspect-实战)
5. [JDK 动态代理 vs CGLIB](#5-jdk-动态代理-vs-cglib)
6. [自调用失效与内部调用陷阱](#6-自调用失效与内部调用陷阱)
7. [Spring 7.0：CGLIB 一致默认与 @Proxyable](#7-spring-70cglib-一致默认与-proxyable)

---

## 1. AOP 解决的问题与核心概念

**AOP（面向切面编程）**：把**横切关注点**（cross-cutting concerns）——贯穿多个业务模块的公共逻辑——从业务代码中抽离，在运行时"织入"：

```java
// 没有 AOP：每个方法都要手写日志+事务+耗时统计 —— 重复、易漏、难维护
public Order getOrder(String id) {
    log.info("调用 getOrder, id=" + id);          // 横切逻辑
    long start = System.currentTimeMillis();
    try {
        beginTransaction();                        // 横切逻辑
        Order o = dao.findById(id);
        commit();
        return o;
    } catch (Exception e) {
        rollback();
        throw e;
    } finally {
        log.info("耗时: " + (System.currentTimeMillis() - start));
    }
}

// 有 AOP：业务方法只写业务，横切逻辑由切面统一处理
public Order getOrder(String id) {
    return dao.findById(id);                       // 干净的业务
}
```

**四个核心概念**：

| 概念 | 含义 | 类比 |
|------|------|------|
| **切点（Pointcut）** | "在哪些方法上织入"——匹配规则 | 地图上的目标区域 |
| **通知（Advice）** | "织入什么逻辑"——前置/后置/环绕等 | 要在目标区域执行的动作 |
| **切面（Aspect）** | 切点 + 通知的组合 | 完整的地图 + 行动计划 |
| **织入（Weaving）** | 把通知装进目标方法的时机（Spring 在运行时代理织入） | 执行计划 |

> 🎯 **核心要点**：AOP 的本质是**"横切逻辑与业务逻辑的分离 + 运行期代理织入"**——Spring 的声明式事务、@Async、@Cacheable、安全注解全部建立在 AOP 之上。

---

## 2. 切点表达式 Pointcut 语法全解

**切点表达式**（AspectJ 语法子集）精确定位"织入点"：

```java
// 基本形式：execution(修饰符 返回类型 类.方法(参数))
execution(public * com.example.service.*.*(..))
//        修饰符可省  返回值   包.类      方法(参数)

// 常用变体
execution(* com.example.service.*.*(..))                    // 包内所有方法
execution(* com.example.service.OrderService.*(..))         // 指定类的所有方法
execution(* com.example.service..*.*(..))                   // 包及子包
execution(* *(String, ..))                                  // 首个参数是 String
execution(public Order com.example..OrderService.getOrder(String))
```

**其他切点指示符**：

```java
@annotation(annotation)                  // 方法上有指定注解 —— 最常用（自定义注解切面）
within(com.example.service..*)           // 类型内所有方法
bean(orderService) / bean(*Service)      // 按 Bean 名
@within(org.springframework.stereotype.Service)   // 类上有注解
```

```java
// 实战：自定义注解切点 —— "标记即织入"，比 execution 精确且解耦
@Aspect
@Component
public class TimingAspect {

    @Pointcut("@annotation(com.example.annotation.Timed)")   // 切点：带 @Timed 的方法
    public void timedMethods() { }

    @Around("timedMethods()")
    public Object time(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        try {
            return pjp.proceed();                                  // 执行目标方法
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            log.info("{} 耗时 {}ms", pjp.getSignature(), ms);
        }
    }
}

// 使用：业务方法只加一个注解
@Timed
public Order getOrder(String id) { return dao.findById(id); }
```

> 🎯 **核心要点**：**注解切点 > execution 切点**——`@annotation(MyAnnotation)` 让"织入点"由业务侧显式声明（可读、可控制），execution 按包名类名匹配则容易误伤（升级时类路径变化即失效）。

---

## 3. 五类通知与执行顺序

| 通知 | 注解 | 时机 | 典型用途 |
|------|------|------|---------|
| 前置 | `@Before` | 方法执行前 | 鉴权、参数校验、日志 |
| 后置返回 | `@AfterReturning` | 正常返回后 | 结果记录、脱敏 |
| 后置异常 | `@AfterThrowing` | 抛出异常后 | 异常告警、转译 |
| 后置最终 | `@After` | 无论成败（finally） | 清理、释放 |
| **环绕** | `@Around` | 全权包裹（唯一能控制方法执行） | 耗时、重试、事务、缓存 |

**执行顺序**（单切面）：

```text
@Around 前段 → @Before → 目标方法 → @AfterReturning / @AfterThrowing → @After → @Around 后段
```

**多切面**：按 `@Order` 排序（数字小先执行）——外层先入先出（洋葱模型）：

```text
@Order(1) 切面 A：Around 前段 → @Order(2) 切面 B：Around 前段 → 目标方法 → B 后段 → A 后段
```

```java
// 环绕通知是"唯一能控制方法执行"的通知 —— 重试/事务的机制基础
@Around("@annotation(Retry)")
public Object retry(ProceedingJoinPoint pjp) throws Throwable {
    int attempts = 3;
    while (true) {
        try {
            return pjp.proceed();
        } catch (Exception e) {
            if (--attempts <= 0) throw e;
            log.warn("重试剩余 {} 次", attempts);
        }
    }
}
```

> ⚠️ **@AfterReturning 拿返回值的坑**：参数里声明 `Object result` 要求切点返回值匹配；拿不到返回值时**不要声明参数**（Spring 会报参数绑定错误）。

---

## 4. 注解驱动 @Aspect 实战

**Spring Boot 开箱即用**（`spring-boot-starter-aop`），完整例子——接口耗时 + 异常告警切面：

```java
@Aspect
@Component
public class MonitorAspect {
    private static final Logger log = LoggerFactory.getLogger(MonitorAspect.class);

    // 切点：service 包下所有 public 方法
    @Pointcut("execution(public * com.example.service.*.*(..))")
    public void serviceMethods() { }

    @Around("serviceMethods()")
    public Object monitor(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long cost = System.currentTimeMillis() - start;
            if (cost > 500) {                        // 慢调用告警
                log.warn("慢调用 {} 耗时 {}ms", pjp.getSignature(), cost);
            }
            return result;
        } catch (Throwable t) {
            log.error("Service 异常: {} 参数: {}",
                    pjp.getSignature(), Arrays.toString(pjp.getArgs()), t);
            throw t;                                 // 异常必须继续上抛！
        }
    }
}
```

**三个使用要点**：

1. **切面本身必须是 Bean**（`@Component` + `@Aspect`）——Spring 才会处理它；
2. **pjp.proceed() 的异常必须继续抛**——吞掉异常等于切面改变了业务语义；
3. **切点表达式用常量抽离**（`@Pointcut` 方法），多通知复用同一规则。

> 💡 AOP 的经典使用清单：日志（请求/响应）、耗时监控、鉴权校验、事务（@Transactional 本身）、缓存（@Cacheable）、重试、幂等、数据脱敏、审计。

---

## 5. JDK 动态代理 vs CGLIB

| 维度 | JDK 动态代理 | CGLIB |
|------|:-----------:|:-----:|
| 原理 | 反射 `Proxy.newProxyInstance` 生成接口代理 | 生成目标类的**子类**（字节码） |
| 前提 | **必须实现接口** | 无需接口，类可继承即可 |
| 限制 | 只能代理接口方法 | **final 类/final 方法无法代理** |
| 性能 | 反射调用（现代 JDK 已优化） | 子类方法直接调用 |
| 历史 | Spring 默认（有接口时） | 无接口时兜底 |

**Spring 的代理选择逻辑**（6.x）：

```text
目标 Bean 有接口 → 默认 JDK 动态代理（可配置 proxyTargetClass=true 强制 CGLIB）
目标 Bean 无接口 → CGLIB
```

```yaml
# 强制 CGLIB（Spring Boot 默认已开启）
spring.aop.proxy-target-class: true   # Boot 2.x+ 默认 true
```

**Spring 7.0 的重大变化（2025-11）**：**CGLIB 成为一致默认**——不再"有接口用 JDK 代理"，统一用 CGLIB 子类代理（配合新注解 `@Proxyable` 控制代理行为），代理行为更可预期，也解决了一些 JDK 代理在 AOT 下的限制。

> 🎯 **核心要点**：代理模型的本质是"**运行时生成替身**"——JDK 代理替身实现接口，CGLIB 替身继承类。**final 类/方法永远无法被任何代理拦截**（CGLIB 也做不到），这是"为什么 @Transactional 不能加在 final 方法上"的底层原因。

---

## 6. 自调用失效与内部调用陷阱

**AOP 生效的前提：方法调用必须**经过代理对象**。同类内部的 `this.method()` 调用绕过代理 → 通知不执行**：

```java
@Service
public class OrderService {
    @Transactional
    public void pay(String orderId) { ... }          // 希望被事务代理

    public void checkout(String orderId) {
        this.pay(orderId);       // ❌ 内部调用：走的是 this（原始对象），事务不生效！
        // 容器注入的是代理对象，但 this 指向原始对象 —— AOP 通知全部跳过
    }
}
```

**为什么失效**：容器注入给外部的是**代理对象**（CGLIB 子类），外部调 `proxy.pay()` 会经过切面；但类内部 `this` 是原始对象，`this.pay()` 直接执行原方法——**切面根本不知道这次调用**。

**四种解法**：

```java
// 解法 1：拆分 —— 把被代理方法放到另一个 Bean（推荐，职责也清晰）
@Service
public class PayService {
    @Transactional public void pay(String id) { ... }
}
@Service
public class OrderService {
    private final PayService payService;              // 注入另一个 Bean → 走代理
    public void checkout(String id) { payService.pay(id); }
}

// 解法 2：注入自己（代理对象）
@Service
public class OrderService {
    @Autowired private OrderService self;             // 注入的是代理
    public void checkout(String id) { self.pay(id); } // 走代理 → 事务生效
}

// 解法 3：AopContext 获取代理（需要 @EnableAspectJAutoProxy(exposeProxy = true)）
// 解法 4：TransactionTemplate 编程式事务（无需代理）
```

> 🎯 **核心要点**：自调用失效影响**所有 AOP 功能**（事务、@Async、@Cacheable、自定义切面）——面试答"事务为什么没生效"的第一候选原因就是它。**根治方案是拆分 Bean**（内聚 + 可代理）。

---

## 7. Spring 7.0：CGLIB 一致默认与 @Proxyable

**Spring Framework 7.0（2025-11）代理模型的升级**：

| 变化 | 说明 |
|------|------|
| CGLIB 一致默认 | 所有 Bean 统一 CGLIB 子类代理，不再按"有无接口"切换 JDK/CGLIB——行为一致、AOT 友好 |
| `@Proxyable` 注解 | 显式控制代理行为的开关（与 `@Proxyable(false)` 关闭代理） |
| 代理可预期性 | 类代理统一后，"为什么这个 Bean 没被代理"的问题大幅减少 |

```java
// Spring 7.0 示例：显式声明代理行为
@Proxyable
@Service
public class OrderService { ... }        // 默认即代理，可省略；@Proxyable(false) 显式关闭

// 对既有代码的影响：升级到 7.0 时 CGLIB 默认意味着——
// ① 非 final 类即可代理（不再需要接口） ② 若有 JDK 代理依赖（如跨模块接口契约）需检查
```

> ⚠️ **升级提示**：Spring 6 → 7 是代际升级（Boot 3.x → 4.x 对应）——AOP 相关注意：CGLIB 默认化带来的行为变化、`javax.*` 注解移除、旧代理配置项失效。迁移清单见 `Spring生态/03-SpringBoot4与SpringFramework7新特性.md`。

---

**下一模块**：[06-声明式事务管理](./06-声明式事务管理.md) / **返回总览**：[00-Spring框架核心知识体系总览](./00-Spring框架核心知识体系总览.md)
