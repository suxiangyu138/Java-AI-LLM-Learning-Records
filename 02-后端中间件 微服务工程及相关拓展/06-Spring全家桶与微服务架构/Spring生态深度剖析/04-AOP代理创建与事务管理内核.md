# 04 - AOP 代理创建与事务管理内核

> 🎯 AOP 代理的创建决策链 + @Transactional 事务传播行为的内部链路。核心问题：代理什么时候创建？事务什么时候开启/挂起？七种传播行为在源码中如何体现？

---

## 目录

1. [AOP 代理创建决策链](#1-aop-代理创建决策链)
2. [JDK 动态代理 vs CGLIB：Spring 如何选择？](#2-jdk-动态代理-vs-cglibspring-如何选择)
3. [@Transactional 处理全链路](#3-transactional-处理全链路)
4. [七种传播行为源码映射](#4-七种传播行为源码映射)
5. [事务失效的六大经典场景](#5-事务失效的六大经典场景)
6. [AOP 与事务调试](#6-aop-与事务调试)

---

## 1. AOP 代理创建决策链

```text
★ 核心入口：AbstractAutoProxyCreator#postProcessAfterInitialization
   — 这是 Bean 初始化后（initializeBean 的最后一步）执行的 BPP

wrapIfNecessary(bean, beanName, cacheKey)  — 决策三部曲：
│
├── ① getAdvicesAndAdvisorsForBean(beanClass, beanName, null)
│      → 扫描所有 Advisor（切面 = Advisor = Pointcut + Advice）
│      → 对当前 Bean 的每个方法匹配切点表达式
│      → 如果没有任何 Advisor 匹配 → 返回 null（不代理）
│      → 如果有匹配 → 返回匹配到的 Advisor 数组
│
├── ② 如果有匹配的 Advisor → 调用 createProxy()
│      → buildAdvisors()   ：构建完整的 Advisor 列表（匹配 + 拦截器）
│      → ProxyFactory.getProxy() → createAopProxy() 决定代理方式
│
└── ③ createAopProxy() — JDK vs CGLIB 决策点
       → 详见第 2 节
```

**追问：** 哪些 Bean 不会被 AOP 代理？→ ① 没有任何 Advisor 匹配的 Bean ② BeanPostProcessor 自身（not eligible warning）③ `@Aspect` 标注的切面类本身。`earlyProxyReferences` 的作用？→ 记录"已在三级缓存中提前创建代理的 BeanName"，保证 afterInitialization 不再重复创建。

---

## 2. JDK 动态代理 vs CGLIB：Spring 如何选择？

### 2.1 DefaultAopProxyFactory 决策逻辑

```java
// createAopProxy() 中的决策链
public AopProxy createAopProxy(AdvisedSupport config) {
    if (config.isOptimize() || config.isProxyTargetClass()
            || hasNoUserSuppliedProxyInterfaces(config)) {
        Class<?> targetClass = config.getTargetClass();
        if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
            return new JdkDynamicAopProxy(config);        // 目标本身是接口
        }
        return new ObjenesisCglibAopProxy(config);       // CGLIB
    }
    return new JdkDynamicAopProxy(config);               // 常规：JDK 代理
}
```

### 2.2 决策表

| 条件 | 代理方式 |
|------|----------|
| 目标类实现了接口 + proxyTargetClass=false | **JDK 动态代理** |
| 目标类实现了接口 + proxyTargetClass=true | CGLIB |
| 目标类没有实现接口 | **CGLIB**（强制） |
| Spring Boot 2.x+ 默认 `proxyTargetClass=true` | **CGLIB**（Spring Boot 全局偏好） |

### 2.3 对比速查

| 对比 | JDK 动态代理 | CGLIB |
|------|-------------|-------|
| 原理 | 反射 + Proxy.newProxyInstance | 继承 + ASM 字节码 |
| 限制 | 必须有接口 | 不能代理 final 类/方法 |
| 性能 | 反射调用（现代 JDK 优化后差异小） | 子类调用（现代环境差异不大） |
| Spring 默认 | 无接口场景下自动退化为 CGLIB | **Boot 2.x+ 全局默认 CGLIB** |

**追问：** 什么是 Objenesis？→ 绕过构造器创建实例的库 — CGLIB 通过它创建代理对象时不触发目标类的构造器（避免未初始化的字段被误用）。

---

## 3. @Transactional 处理全链路

### 3.1 骨架：代理 → Advisor → Interceptor

```text
@Transactional 工作原理：
① @EnableTransactionManagement → 注册 InfrastructureAdvisorAutoProxyCreator（IAAPC）
   它继承 AbstractAutoProxyCreator，在 afterInitialization 中做代理决策
② 扫描所有 bean → 匹配是否需要 @Transactional → 是则创建代理
③ 代理对象（JDK/CGLIB）的方法调用被 TransactionInterceptor 拦截
④ TransactionInterceptor.invoke() → 执行事务管理逻辑
```

### 3.2 TransactionInterceptor 核心流程

```java
// TransactionInterceptor#invoke(MethodInvocation invocation)
public Object invoke(MethodInvocation invocation) {
    Class<?> targetClass = invocation.getThis().getClass();
    // ① 获取事务属性：读 @Transactional 注解（propagation/isolation/timeout/readOnly 等）
    TransactionAttribute attr = getTransactionAttribute(method, targetClass);

    // ② 获取事务管理器（PlatformTransactionManager — DataSource/JTA）
    PlatformTransactionManager tm = determineTransactionManager(attr);

    // ③ 开启事务（内部处理传播行为）← ★ 核心
    TransactionInfo txInfo = createTransactionIfNecessary(tm, attr, joinpointId);

    Object retVal;
    try {
        retVal = invocation.proceed();        // 执行目标方法
    } catch (Throwable ex) {
        completeTransactionAfterThrowing(txInfo, ex);  // 回滚判定
        throw ex;
    } finally {
        cleanupTransactionInfo(txInfo);       // 清理线程绑定的事务信息
    }
    commitTransactionAfterReturning(txInfo);  // 提交
    return retVal;
}
```

### 3.3 事务状态的线程绑定

```java
// 事务上下文通过 TransactionSynchronizationManager 绑定到当前线程：
// ThreadLocal<Map<DataSource, Connection>> — 保证同一事务复用同一数据库连接
// ThreadLocal<TransactionSynchronization> — 事务同步回调（如发送事务完成事件）

TransactionSynchronizationManager.getResource(dataSource);     // 获取当前事务连接
TransactionSynchronizationManager.isActualTransactionActive(); // 判断是否在事务中
```

---

## 4. 七种传播行为源码映射

**核心方法：** `AbstractPlatformTransactionManager#getTransaction(TransactionDefinition)`

| 传播行为 | 源码行为 |
|----------|----------|
| **REQUIRED**（默认） | 有事务则加入；无则**新建** |
| **REQUIRES_NEW** | 不管有没有，**挂起当前**，始终新建独立事务 |
| **NESTED** | 有事务则创建**保存点**（savepoint），无则同 REQUIRED（仅 JDBC 支持） |
| SUPPORTS | 有事务则加入，无则非事务运行 |
| NOT_SUPPORTED | **挂起当前事务**，非事务运行 |
| MANDATORY | 必须有事务，否则抛异常 |
| NEVER | 有事务抛异常 |

**关键源码片段：**

```java
// REQUIRES_NEW 的挂起逻辑
if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
    SuspendedResourcesHolder suspended = suspend(transaction); // 解绑当前事务资源
    newTransaction = true;
    // ... 创建新事务连接
}

// NESTED 的保存点逻辑
if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
    // 基于 JDBC 的 Savepoint 实现
    SavepointManager spm = new JdbcTransactionObjectSupport();
    spm.createAndHoldSavepoint();
}
```

---

## 5. 事务失效的六大经典场景

| 场景 | 原因 | 对策 |
|------|------|------|
| ① **自调用（this.xxx）** | this 调用绕过代理对象 → 事务拦截器不执行 | 注入自身 / AopContext.currentProxy() / 拆出独立 Service |
| ② **非 public 方法** | 默认只代理 public（CGLIB 可配 proxyTargetClass 生效但非标准） | 改为 public |
| ③ **异常被 catch 吞掉** | Spring 只在方法抛出异常时才回滚 | catch 内重新 throw / 手动 `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()` |
| ④ **rollbackFor 不匹配** | 默认只回滚 RuntimeException 和 Error | `@Transactional(rollbackFor = Exception.class)` |
| ⑤ **数据库引擎不支持事务** | MyISAM 不支持 | 确认使用 InnoDB |
| ⑥ **多线程调用** | 事务绑定在线程上，新线程 = 新事务 | 用分布式事务方案 |

**追问：** 自调用失效怎么验证？→ 在这个 Service 的方法内 `System.out.println(this.getClass())` — 如果是原始类而非 `$Proxy` 或 CGLIB 增强类，说明走了 this 绕过代理。

---

## 6. AOP 与事务调试

```bash
# 确认代理类型
System.out.println(bean.getClass().getName());
# CGLIB：com.example.UserService$$SpringCGLIB$$0
# JDK：  com.sun.proxy.$Proxy123

# 核心断点位置
TransactionInterceptor#invoke()                     # 事务拦截器入口
AbstractPlatformTransactionManager#getTransaction()  # 传播行为决策
AbstractAutoProxyCreator#wrapIfNecessary()            # 代理创建决策
DefaultAopProxyFactory#createAopProxy()               # JDK vs CGLIB 决策
```

**排查口诀：** 代理类型先确认 → 自调用排查 this → 异常类型看 rollbackFor → 数据库引擎确认 InnoDB。

---

> 🎯 **核心要点**：AOP 三大核心问题 — ① 代理何时创建（afterInitialization）② JDK vs CGLIB 怎么选（有接口且非强制可选 JDK、无接口强制 CGLIB、Boot 默认 CGLIB）③ 为什么自调用事务失效（this 绕过代理、事务拦截器未触发）。事务传播的核心在 `getTransaction()` — REQUIRED 是加入或新建、REQUIRES_NEW 是挂起新建、NESTED 是保存点。

**下一模块**：[05-DispatcherServlet请求处理全链路](05-DispatcherServlet请求处理全链路.md) / **返回总览**：[00-深度剖析总览](00-Spring生态深度剖析总览.md)
