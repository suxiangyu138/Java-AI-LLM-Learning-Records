# Spring AOP 基础应用

> **定位**：AOP = 面向切面编程，将日志/事务/权限等横切逻辑与业务代码分离。前提：切面类和目标对象都必须是 Spring 管理的 Bean。

---

## 1. 六大核心概念

| 概念 | 说明 |
|------|------|
| **切面（Aspect）** | 横切逻辑 + 切入点的集合（`@Aspect` 类） |
| **切入点（Pointcut）** | 指定对哪些方法增强（`@Pointcut` 表达式） |
| **通知（Advice）** | 增强逻辑的具体实现（`@Before`/`@After` 等） |
| **目标对象（Target）** | 被增强的 Bean（`@Service`/`@Repository`） |
| **代理对象（Proxy）** | 动态代理生成，实际调用的是代理 |
| **连接点（JoinPoint）** | 所有可能被增强的方法（切入点是其子集） |

### 五种通知

| 通知 | 注解 | 触发时机 |
|------|------|----------|
| **前置** | `@Before` | 目标方法执行前 |
| **后置** | `@After` | 目标方法执行后（无论异常） |
| **返回** | `@AfterReturning` | 正常返回后 |
| **异常** | `@AfterThrowing` | 异常时 |
| **环绕** | `@Around` | 包裹目标方法（最灵活） |

### 执行顺序

```text
正常：@Before → 目标方法 → @AfterReturning → @After
异常：@Before → 目标方法(异常) → @AfterThrowing → @After
```

---

## 2. 底层原理

| 代理方式 | 条件 | 原理 |
|----------|------|------|
| **JDK 动态代理** | 目标实现接口 | `Proxy` 基于接口生成代理 |
| **CGLIB 动态代理** | 目标未实现接口 | 基于继承生成子类代理 |

> Spring 自动选择：有接口→JDK，无接口→CGLIB。

---

## 3. 代码实现

### 环境搭建

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aop</artifactId>
    <version>5.3.28</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aspects</artifactId>
    <version>5.3.28</version>
</dependency>
```

### 注解方式开启 AOP

```java
@Configuration
@ComponentScan("com.example")
@EnableAspectJAutoProxy  // 开启 AOP 注解支持
public class SpringConfig {}
```

### 日志切面完整代码

```java
@Component
@Aspect
public class LogAspect {

    @Pointcut("execution(* com.example.service.*.*(..))")
    public void logPointcut() {}

    @Before("logPointcut()")
    public void before(JoinPoint joinPoint) {
        System.out.println("【前置】" + joinPoint.getSignature().getName() + " 开始执行");
    }

    @AfterReturning(value = "logPointcut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        System.out.println("【返回】" + joinPoint.getSignature().getName() + " 返回值: " + result);
    }

    @AfterThrowing(value = "logPointcut()", throwing = "e")
    public void afterThrowing(JoinPoint joinPoint, Exception e) {
        System.out.println("【异常】" + joinPoint.getSignature().getName() + " 异常: " + e.getMessage());
    }

    @After("logPointcut()")
    public void after(JoinPoint joinPoint) {
        System.out.println("【后置】" + joinPoint.getSignature().getName() + " 执行结束");
    }
}
```

### 切入点表达式

```java
execution(* com.example.service.*.*(..))     // service 包下所有类所有方法
execution(* com.example.service.UserService.*(..))  // UserService 所有方法
execution(* com.example..*(..))              // com.example 及子包所有方法
```

---

## 4. 常见错误

| 错误 | 现象 | 解决 |
|------|------|------|
| 忘记 `@EnableAspectJAutoProxy` | AOP 不生效 | 添加注解 |
| 切面未加 `@Component` | 切面未被 Spring 管理 | 添加 `@Component` |
| 切入点表达式错误 | 切面无法匹配 | 检查包路径 |
| 目标未 Spring 管理 | 无法被增强 | 确保 `@Service`/`@Repository` |
| 依赖缺失 | 找不到 `@Aspect` | 添加 `spring-aspects` 依赖 |

---

## 5. AOP 应用场景

| 场景 | 通知类型 |
|------|----------|
| 日志记录 | `@Before` + `@AfterReturning` + `@AfterThrowing` |
| 事务控制 | `@Around` |
| 权限校验 | `@Before` |
| 性能监控 | `@Around` 计时 |
| 异常统一处理 | `@AfterThrowing` |
