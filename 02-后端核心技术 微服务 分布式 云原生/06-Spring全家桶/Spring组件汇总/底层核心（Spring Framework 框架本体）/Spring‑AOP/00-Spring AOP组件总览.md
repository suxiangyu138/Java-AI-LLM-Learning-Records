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

## 4. 与深度体系的映射

| 速查文档 | 深度体系模块 |
|---------|-------------|
| 01-模块清单 | [Spring框架核心-05-AOP](../../../Spring框架核心/05-AOP面向切面编程.md) |
| 02-核心类与注解速查 | [Spring生态深度剖析-04-AOP代理创建与事务管理内核](../../../Spring生态深度剖析/04-AOP代理创建与事务管理内核.md) |
| 03-配置属性速查 | [Spring框架核心-05](../../../Spring框架核心/05-AOP面向切面编程.md) |
| 04-集成地图与常见问题 | [Spring Core-06-@Configuration与AOP代理机制](../../../Spring Core/06-@Configuration与AOP代理机制.md) |

> 💡 本系列定位"查得快"，深度体系定位"学得透"——速查命中后，跳转深度体系看机制与源码。

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

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单](01-模块清单.md) | spring-aop 及相关 artifact 依赖体系 |
| [02-核心类与注解速查](02-核心类与注解速查.md) | 代理/切点/通知/切面关键 API 与注解 |
| [03-配置属性速查](03-配置属性速查.md) | spring.aop.* 属性与 @Proxyable（7.0） |
| [04-集成地图与常见问题](04-集成地图与常见问题.md) | 与事务/缓存/异步联动 + 代理失效坑 |

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页
