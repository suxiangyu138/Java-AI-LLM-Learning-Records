# 02-Spring AOP面向切面编程
> 🎯 AOP是Spring两大核心之一 — 掌握代理机制、通知类型、切点表达式，理解AOP底层如何实现事务、日志、权限等横切关注点

---

## 目录
1. [本章总览](#1-本章总览)
2. [AOP核心概念](#2-aop核心概念)
3. [通知类型详解](#3-通知类型详解)
4. [切点表达式语法](#4-切点表达式语法)
5. [JDK动态代理 vs CGLIB代理](#5-jdk动态代理-vs-cglib代理)
6. [实际落地场景](#6-实际落地场景)
7. [代理失效场景全解](#7-代理失效场景全解)
8. [高频踩坑与误区](#8-高频踩坑与误区)
9. [随堂基础练习](#9-随堂基础练习)
10. [章节综合实操案例](#10-章节综合实操案例)
11. [分层综合习题](#11-分层综合习题)
12. [本章复盘速记清单](#12-本章复盘速记清单)
13. [精通拓展补充-P2](#13-精通拓展补充-p2)

---

## 1. 本章总览

### 1.1 知识定位
- **归属**：Spring Framework 核心 → 层级1 P0核心必学
- **前置依赖**：IoC容器（必须先理解Bean管理和依赖注入）

### 1.2 三层学习目标

| 级别 | 目标 |
|------|------|
| **基础** | 会使用`@Aspect`/`@Before`/`@After`编写简单切面 |
| **熟练** | 能写出生产级日志切面/权限切面，理解切点表达式的各种写法 |
| **精通** | 吃透JDK/CGLIB代理选择规则，能分析代理失效场景，理解AOP在事务中的运用 |

---

## 2. AOP核心概念

### 2.1 AOP要解决什么问题

> 🔍 **横切关注点**：散布在多个模块中的相同逻辑（日志、事务、权限），与核心业务代码耦合，难以维护。

```
传统方式：每个方法都写一遍                         AOP方式：统一切面拦截
┌──────────────┐  ┌──────────────┐              ┌──────────────┐  ┌──────────────┐
│ 日志记录     │  │ 日志记录     │              │              │  │              │
│ 权限校验     │  │ 权限校验     │              │ UserService  │  │ OrderService │
│ 事务管理     │  │ 事务管理     │              │   (纯业务)   │  │   (纯业务)   │
│ ↓ 业务逻辑  │  │ ↓ 业务逻辑  │              └──────┬───────┘  └──────┬───────┘
└──────────────┘  └──────────────┘                     │                  │
     重复代码！         重复代码！              └───────┼──────────────────┘
                                                         ↓
                                                  ┌──────────────────┐
                                                  │  AOP拦截层       │
                                                  │  日志+权限+事务  │
                                                  └──────────────────┘
```

### 2.2 AOP核心术语

| 术语 | 英文 | 含义 | 举例 |
|------|------|------|------|
| **切面** | Aspect | 横切关注点的模块化，=切点+通知 | `@Aspect`标注的类 |
| **切点** | Pointcut | 在哪些方法上织入 | `execution(* com.example.service.*.*(..))` |
| **通知** | Advice | 在切点上执行什么逻辑 | `@Before`、`@After`、`@Around` |
| **连接点** | JoinPoint | 程序执行中的某个点（方法调用） | Spring AOP中只有方法执行 |
| **目标对象** | Target | 被代理的对象 | UserServiceImpl |
| **代理** | Proxy | 对目标对象增强后的代理对象 | JDK动态代理 / CGLIB代理 |
| **织入** | Weaving | 将切面应用到目标对象的过程 | Spring在运行时通过代理织入 |

---

## 3. 通知类型详解

### 3.1 五种通知完整对比

| 通知类型 | 注解 | 执行时机 | 能否阻止目标方法执行 | 获取返回值 |
|----------|------|----------|---------------------|-----------|
| **前置通知** | `@Before` | 目标方法执行前 | ❌ 不能（不抛异常则目标必执行） | ❌ |
| **后置通知** | `@After` | 目标方法执行后（finally语义） | ❌ | ❌ |
| **返回通知** | `@AfterReturning` | 目标方法正常返回后 | ❌ | ✅ `returning`属性 |
| **异常通知** | `@AfterThrowing` | 目标方法抛出异常后 | ❌ | ✅ `throwing`属性获取异常 |
| **环绕通知** | `@Around` | 包裹目标方法前后 | ✅ `proceed()`控制 | ✅ |

### 3.2 完整代码演示

```java
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    // ===== 1. @Before 前置通知 =====
    @Before("execution(* com.example.service.*.*(..))")
    public void before(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        log.info(">> 调用方法: {}，参数: {}", methodName, Arrays.toString(args));
    }

    // ===== 2. @After 后置通知（finally语义，无论如何都执行） =====
    @After("execution(* com.example.service.*.*(..))")
    public void after(JoinPoint joinPoint) {
        log.info("<< 方法结束: {}", joinPoint.getSignature().getName());
    }

    // ===== 3. @AfterReturning 返回通知（可以拿到返回值） =====
    @AfterReturning(
        pointcut = "execution(* com.example.service.*.*(..))",
        returning = "result"
    )
    public void afterReturning(JoinPoint joinPoint, Object result) {
        log.info("返回值: {}", result);
    }

    // ===== 4. @AfterThrowing 异常通知（可以拿到异常信息） =====
    @AfterThrowing(
        pointcut = "execution(* com.example.service.*.*(..))",
        throwing = "ex"
    )
    public void afterThrowing(JoinPoint joinPoint, Exception ex) {
        log.error("异常: {} → {}", joinPoint.getSignature().getName(), ex.getMessage());
    }

    // ===== 5. @Around 环绕通知（功能最强，需手动调proceed） =====
    @Around("@annotation(com.example.annotation.TimeLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        
        try {
            // 调用目标方法（不调用则目标方法不执行！）
            Object result = joinPoint.proceed();
            long end = System.currentTimeMillis();
            log.info("{} 耗时: {}ms", methodName, end - start);
            return result;
        } catch (Exception e) {
            log.error("{} 异常: {}", methodName, e.getMessage());
            throw e; // 必须抛出，否则异常被吞
        }
    }
}
```

> ⚠️ **关键点**：`@Around`环绕通知中**必须调用**`joinPoint.proceed()`且**必须return其返回值**或抛出异常，否则目标方法不会执行且返回值丢失。

### 3.3 多个通知的执行顺序

```
正常执行：@Around#before → @Before → 目标方法 → @Around#afterReturning → @AfterReturning → @After → @Around#after
异常执行：@Around#before → @Before → 目标方法(抛异常) → @AfterThrowing → @After → @Around#after(接异常)
```

---

## 4. 切点表达式语法

### 4.1 execution 表达式（最常用）

```
execution(修饰符? 返回值类型 包名.类名.方法名(参数类型列表) throws 异常类型?)
```

| 示例 | 匹配范围 |
|------|----------|
| `execution(* com.example.service.*.*(..))` | service包下所有类的所有方法 |
| `execution(public * com.example..*.*(..))` | com.example及其子包下所有public方法 |
| `execution(* com.example.service.UserService.find*(..))` | UserService中以find开头的方法 |
| `execution(* com.example..*.*(String, Long))` | 参数类型恰好为(String, Long)的方法 |
| `execution(* com.example..*.*(String, ..))` | 第一个参数为String，其余任意 |

### 4.2 其他指示符

| 指示符 | 说明 | 示例 |
|--------|------|------|
| `within` | 按类匹配 | `within(com.example.service.*)` |
| `args` | 按参数类型匹配 | `args(Long)` |
| `@annotation` | 按方法上的注解匹配 | `@annotation(com.example.annotation.Log)` |
| `@within` | 按类上的注解匹配 | `@within(org.springframework.stereotype.Service)` |
| `bean` | 按Bean名称匹配（Spring独有） | `bean(userService)` |
| `this` | 代理对象类型匹配 | `this(com.example.service.UserService)` |
| `target` | 目标对象类型匹配 | `target(com.example.repository.UserRepository)` |

### 4.3 组合条件

```java
// &&（且）、||（或）、!（非）
@Pointcut("execution(* com.example.service.*.*(..))")
public void serviceLayer() {}

@Pointcut("!execution(* com.example.service.*.get*(..))")
public void notGetter() {}

@Around("serviceLayer() && notGetter()")
public Object around(ProceedingJoinPoint pjp) { ... }
```

---

## 5. JDK动态代理 vs CGLIB代理

> 🔥 面试高频：Spring AOP什么时候用JDK代理？什么时候用CGLIB？

### 5.1 对比表

| 维度 | JDK动态代理 | CGLIB代理 |
|------|------------|----------|
| **原理** | 基于接口，生成接口的实现类（`Proxy.newProxyInstance`） | 基于继承，生成目标类的子类 |
| **要求** | 目标对象**必须实现接口** | 目标对象不能是final类，方法不能是final |
| **性能** | 创建快，执行略慢（反射） | 创建慢，执行快（直接调用） |
| **Spring默认** | 有接口时默认使用 | 无接口时使用 |
| **SpringBoot 2.x** | 与Spring相同 | 与Spring相同 |
| **SpringBoot 3.x** | 不再默认使用 | **默认使用CGLIB**（spring.aop.proxy-target-class=true） |

### 5.2 代理选择规则

```java
// 场景1：有接口 → 默认JDK代理
public interface UserService { void save(); }

@Service
public class UserServiceImpl implements UserService {
    @Override
    public void save() { ... }
}
// → 代理对象类型: com.sun.proxy.$Proxy (实现了UserService接口)
// → 获取方式: ctx.getBean(UserService.class) ✅
//            ctx.getBean(UserServiceImpl.class) ❌ 类型不匹配！

// 场景2：无接口 → CGLIB代理
@Service
public class OrderService {  // 没有实现接口
    public void save() { ... }
}
// → 代理对象类型: com.example.OrderService$$SpringCGLIB$$0 (继承OrderService)

// 场景3：强制使用CGLIB（SpringBoot默认行为）
// application.yml: spring.aop.proxy-target-class=true
// 或代码: @EnableAspectJAutoProxy(proxyTargetClass = true)
```

> 💡 **最佳实践**：面向接口编程 + 构造器注入，可以不受代理类型影响。

---

## 6. 实际落地场景

### 6.1 接口耗时统计切面

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TimeLog {
    String value() default "";
}

@Aspect
@Component
@Slf4j
public class TimeLogAspect {
    
    @Around("@annotation(timeLog)")
    public Object around(ProceedingJoinPoint pjp, TimeLog timeLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long cost = System.currentTimeMillis() - start;
        
        String desc = timeLog.value().isEmpty() 
            ? pjp.getSignature().toShortString() 
            : timeLog.value();
        
        if (cost > 500) {
            log.warn("⚠️ 慢方法: {} 耗时 {}ms", desc, cost);
        } else {
            log.info("{} 耗时 {}ms", desc, cost);
        }
        return result;
    }
}
```

### 6.2 权限校验切面

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value(); // 需要的角色列表
}

@Aspect
@Component
public class AuthAspect {
    
    @Autowired
    private HttpServletRequest request;
    
    @Before("@annotation(requireRole)")
    public void checkPermission(RequireRole requireRole) {
        String token = request.getHeader("Authorization");
        User user = parseToken(token); // 解析当前用户
        
        List<String> userRoles = user.getRoles();
        boolean hasRole = Arrays.stream(requireRole.value())
            .anyMatch(userRoles::contains);
        
        if (!hasRole) {
            throw new AccessDeniedException("无权限访问");
        }
    }
}
```

### 6.3 统一日志切面（记录入参、出参、异常）

```java
@Aspect
@Component
@Slf4j
@Order(1) // 控制切面执行顺序
public class WebLogAspect {
    
    @Pointcut("execution(* com.example.controller..*.*(..))")
    public void controllerLayer() {}
    
    @Around("controllerLayer()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) 
            RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs.getRequest();
        
        log.info("URL: {} {}, IP: {}, ARGS: {}",
            request.getMethod(), request.getRequestURI(),
            request.getRemoteAddr(), Arrays.toString(pjp.getArgs()));
        
        Object result = pjp.proceed();
        
        log.info("RESPONSE: {}", JSON.toJSONString(result));
        return result;
    }
}
```

---

## 7. 代理失效场景全解

> 🔥 AOP代理失效是面试和生产环境最高频问题之一

### ❌ 场景1：同类内部方法调用

```java
@Service
public class UserService {
    
    @TimeLog  // 这个注解不会生效！
    public void doSomething() {
        System.out.println("执行业务");
    }
    
    public void outerMethod() {
        // 直接通过this调用，绕过了代理对象
        this.doSomething(); // ❌ AOP不生效！
    }
}
```

> 💡 **原因**：`this`是目标对象本身，不是代理对象。AOP拦截发生在代理对象的方法调用上。
>
> **解决方案**：
> 1. 注入自己：`@Autowired private UserService self; self.doSomething();`
> 2. 获取代理：`((UserService) AopContext.currentProxy()).doSomething();`（需`@EnableAspectJAutoProxy(exposeProxy = true)`）
> 3. 拆分到不同类（推荐）

### ❌ 场景2：private方法

```java
@Service
public class UserService {
    
    @TimeLog
    private void doSomething() { // ❌ CGLIB通过继承，无法代理private方法
        ...
    }
}
```

### ❌ 场景3：final方法/类

```java
@Service
public final class UserService { // ❌ CGLIB无法代理final类
    @TimeLog
    public final void doSomething() { // ❌ CGLIB无法代理final方法
        ...
    }
}
```

### ❌ 场景4：static方法

```java
// ❌ Spring AOP不支持static方法代理
@TimeLog
public static String format() { ... }
```

---

## 8. 高频踩坑与误区

| 序号 | 问题 | 原因 | 解决方案 |
|------|------|------|----------|
| 1 | 切面不生效 | 没有`@Component`让切面被Spring管理 | 加`@Component`或`@Bean`注册 |
| 2 | `@After`在异常时也执行 | `@After`是finally语义 | 正常行为，如需仅在正常时执行用`@AfterReturning` |
| 3 | `@Around`忘记调`proceed()` | 目标方法被跳过 | 必须调用`joinPoint.proceed()` |
| 4 | `@Around`吞异常 | 没有throw异常 | 捕获后必须`throw e` |
| 5 | 多个切面顺序不确定 | 没有指定`@Order` | 使用`@Order(N)`，值越小越先执行 |
| 6 | `@Transactional`不生效 | 同类内部调用（事务本质是AOP） | 拆分到不同类 |

---

## 9. 随堂基础练习

1. 编写一个`@Aspect`切面，在调用`UserService`的所有方法前打印日志
2. 使用`@Around`统计`OrderService.createOrder()`的耗时
3. 分析：`@After`和`@AfterReturning`的区别？
4. 编写一个基于`@annotation`的自定义注解权限校验切面

---

## 10. 章节综合实操案例

```java
// 完整示例：操作日志记录
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperateLog {
    String module();    // 操作模块
    String action();    // 操作类型
}

@Aspect
@Component
@Slf4j
public class OperateLogAspect {
    
    @Autowired
    private OperateLogService logService; // 持久化日志
    
    @AfterReturning(
        pointcut = "@annotation(operateLog)",
        returning = "result"
    )
    public void recordLog(JoinPoint jp, OperateLog operateLog, Object result) {
        // 获取当前用户
        String username = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        
        OperateLogEntity entity = OperateLogEntity.builder()
            .module(operateLog.module())
            .action(operateLog.action())
            .username(username)
            .method(jp.getSignature().toShortString())
            .params(JSON.toJSONString(jp.getArgs()))
            .result(JSON.toJSONString(result))
            .createTime(LocalDateTime.now())
            .build();
        
        logService.save(entity);
    }
}

// 业务使用
@OperateLog(module = "用户管理", action = "删除用户")
public void deleteUser(Long userId) {
    userRepository.deleteById(userId);
}
```

---

## 11. 分层综合习题

### 基础题
1. Spring AOP的五种通知类型分别是什么？列出注解和含义
2. `execution(* com.example..*.*(..))`匹配哪些方法？

### 进阶应用题
3. 为什么同类中的内部调用AOP不生效？如何解决？
4. JDK动态代理和CGLIB代理各有什么限制？

### 精通拔高题
5. Spring如何决定使用JDK代理还是CGLIB代理？SpringBoot 3.x的默认行为有何变化？
6. AOP代理对象和目标对象的内存布局是怎样的？如果有多个切面，代理链如何形成？
7. 分析`@Transactional`注解底层是如何通过AOP实现事务管理的？

---

## 12. 本章复盘速记清单

| 类别 | 要点 |
|------|------|
| **核心术语** | Aspect=Pointcut+Advice，JoinPoint=方法连接点 |
| **5种通知** | @Before < @After(returning) < @Around(最强) < @After(finally) < @AfterThrowing |
| **切点表达式** | `execution`、`@annotation`、`within`、`args`、`bean` |
| **代理机制** | JDK=接口代理、CGLIB=继承代理，SpringBoot默认CGLIB |
| **失效场景** | 同类内部调用、private/final/static方法、未注册切面 |
| **执行顺序** | `@Around前`→`@Before`→目标→`@Around后`→`@AfterReturning`→`@After` |
| **@Order** | 值越小越先执行（包裹在外层），用多个切面时必加 |

---

## 13. 精通拓展补充-P2

### 13.1 Spring AOP vs AspectJ

| 维度 | Spring AOP | AspectJ |
|------|-----------|---------|
| 织入时机 | 运行时（代理） | 编译时/类加载时/运行时 |
| 连接点 | 仅方法执行 | 方法执行、构造器、字段访问... |
| 性能 | 略有代理开销 | 几乎无开销（编译期织入） |
| 使用便利性 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| 功能完整度 | 80%常见场景 | 100%所有场景 |

### 13.2 AOP在Spring事务中的应用

```java
// @Transactional底层就是一个@Around切面
// 伪代码展示事务切面的工作方式
@Around("@annotation(org.springframework.transaction.annotation.Transactional)")
public Object transactionalAround(ProceedingJoinPoint pjp) throws Throwable {
    // 1. 获取事务管理器
    PlatformTransactionManager tm = getTransactionManager();
    
    // 2. 开启事务
    TransactionStatus status = tm.getTransaction(transactionDefinition);
    
    try {
        // 3. 执行目标方法
        Object result = pjp.proceed();
        
        // 4. 提交事务
        tm.commit(status);
        return result;
    } catch (Exception e) {
        // 5. 回滚事务
        tm.rollback(status);
        throw e;
    }
}
```

### 13.3 AOP的代理链（责任链模式）

当多个切面作用于同一个方法时，Spring通过`ProxyFactory`将它们组织成责任链：

```
调用者 → 代理对象 → Advice1.before → Advice2.before → 目标方法 
                                                   → Advice2.after → Advice1.after → 返回结果
```

拦截器链底层数据结构：`CglibMethodInvocation`（CGLIB）或`ReflectiveMethodInvocation`（JDK）中的`interceptorsAndDynamicMethodMatchers`列表，每次`proceed()`将索引+1。
