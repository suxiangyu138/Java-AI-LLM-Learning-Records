# @Configuration 与 AOP 代理机制
> 为什么 @Configuration 类必须被 CGLIB 代理？@Bean 的单例语义怎么保证？JDK 动态代理与 CGLIB 怎么选？AOP 代理链如何构建——本文件拆解代理机制全部源码细节

## 目录
1. [代理机制总览：JDK vs CGLIB](#1-代理机制总览jdk-vs-cglib)
2. [@Configuration 的 CGLIB 代理](#2-configuration-的-cglib-代理)
3. [AOP 代理创建：AnnotationAwareAspectJAutoProxyCreator](#3-aop-代理创建annotationawareaspectjautoproxycreator)
4. [代理链构建：Advisor 组装](#4-代理链构建advisor-组装)
5. [JDK 代理 vs CGLIB 代理源码对比](#5-jdk-代理-vs-cglib-代理源码对比)
6. [代理失效场景与调试](#6-代理失效场景与调试)

---

## 1. 代理机制总览：JDK vs CGLIB

### 1.1 两种代理对比

| 维度 | JDK 动态代理 | CGLIB 代理 |
|------|------|------|
| 原理 | 接口代理（Proxy + InvocationHandler） | 子类代理（字节码生成子类） |
| 要求 | 必须实现接口 | 无接口要求（final 类除外） |
| 性能 | 创建快，调用略慢 | 创建慢，调用快（生成类） |
| 适用 | 有接口的 bean | 无接口 / 强制 proxyTargetClass |
| Spring 默认 | 有接口时默认 | Spring Boot 默认强制 CGLIB |

### 1.2 选择规则（源码位置）

```text
DefaultAopProxyFactory.createAopProxy：
  if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces) {
      // proxyTargetClass=true 或没有接口 → CGLIB
      return new ObjenesisCglibAopProxy(...);
  }
  // 默认有接口 → JDK 动态代理
  return new JdkDynamicAopProxy(...);

Spring Boot 默认：spring.aop.proxy-target-class=true（强制 CGLIB）
```

> 🎯 面试必答：**「Spring Boot 为什么默认 CGLIB？」**——统一行为（不因接口有无而不同）、性能更好（调用快）、支持更多特性（如自调用代理在 CGLIB 下更完整）。

---

## 2. @Configuration 的 CGLIB 代理

### 2.1 为什么必须代理

```java
@Configuration
public class AppConfig {
    @Bean
    public A a() { return new A(b()); }      // 内部调用 b()
    @Bean
    public B b() { return new B(); }
}
```

```text
问题：如果 a() 内部 new b()，每次调用 a() 都会 new 一个新的 b
      → 破坏「单例」语义（b 应该是容器里的单例）

解决：@Configuration 类被 CGLIB 代理
  → 代理拦截 @Bean 方法调用
  → 若容器已有该 bean → 返回容器中的单例（不再执行方法体）
  → 实现单例语义
```

### 2.2 @Bean 方法的拦截逻辑

```text
ConfigurationClassEnhancer 生成子类（CGLIB）：
  BeanMethodInterceptor 拦截 @Bean 方法：
    ① 若 bean 正在创建中（当前调用链）→ 直接执行方法
    ② 否则查容器（getBean）→ 命中返回容器实例（单例语义）
    ③ 未命中 → 执行方法体并注册

注意：@Configuration(proxyBeanMethods = false) 可关闭代理
  → 无单例保证（每次方法调用都 new）→ 用于性能敏感/无内部调用的场景
```

> 🎯 面试必答：**「@Configuration 为什么被 CGLIB 代理？」——保证 @Bean 方法内部互调时返回容器单例**；`proxyBeanMethods=false` 可关闭（Lite 模式），但内部互调会破坏单例语义。

### 2.3 两阶段配置类（full vs lite）

| 模式 | 条件 | 行为 |
|------|------|------|
| Full（代理） | @Configuration 标注 | @Bean 方法拦截，单例保证 |
| Lite（不代理） | @Component/@Bean 方法所在类 | 方法直接执行，无单例保证 |

---

## 3. AOP 代理创建：AnnotationAwareAspectJAutoProxyCreator

### 3.1 触发链

```text
@EnableAspectJAutoProxy
  → 注册 AnnotationAwareAspectJAutoProxyCreator（InfrastructureAdvisorAutoProxyCreator 子类）
  → 它是 SmartInstantiationAwareBeanPostProcessor
  → 在 initializeBean 的 afterInitialization 被调用
  → wrapIfNecessary：判断是否需要代理
```

### 3.2 wrapIfNecessary 逻辑

```java
// AbstractAutoProxyCreator.wrapIfNecessary
protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
    // ① 是否已有代理（循环依赖早期代理过）→ 跳过
    // ② 获取该 bean 的 Advisor 集合（getAdvicesAndAdvisorsForBean）
    //    → 无匹配 Advisor → 不代理，原样返回
    // ③ 有匹配 → createProxy（创建代理对象）
}
```

| 判断 | 说明 |
|------|------|
| Advisor 是否匹配 | 切点表达式（execution/@annotation/@within）匹配 bean 类/方法 |
| 循环依赖提前代理 | earlyProxyReferences 记录，避免二次代理 |
| 基础组件跳过 | 框架内部 bean（Advisor/切面本身）不代理 |

---

## 4. 代理链构建：Advisor 组装

### 4.1 Advisor 来源

```text
Advisor = 通知（Advice）+ 切点（Pointcut）
来源：
  · @Aspect 类的方法（@Before/@After/@Around）→ AspectJExpressionPointcutAdvisor
  · 编程式 Advisor（xml aop:config / Advisor 注册）
  · 事务：TransactionInterceptor 包装为 BeanFactoryTransactionAttributeSourceAdvisor
```

### 4.2 代理执行链（责任链模式）

```text
方法调用 → JdkDynamicAopProxy.invoke / CglibAopProxy.intercept
  → 构建拦截器链：MethodInterceptor 列表
  → 依次执行：@Around → @Before → 目标方法 → @After → @AfterReturning/@AfterThrowing
  → 链式调用（ReflectiveMethodInvocation）

顺序规则（@Order 控制）：
  · 优先级数字越小越先执行
  · @Around 先于 @Before 进入，后于 @AfterReturning 退出
```

### 4.3 拦截器链执行顺序

```text
@Around（进入）
  @Before
    目标方法
  @AfterReturning（成功）/ @AfterThrowing（异常）
  @After
@Around（退出/环绕收尾）

事务与切面的顺序：事务拦截器通常在最外层（Order 靠前）
  → 事务先开启，再进业务切面
```

---

## 5. JDK 代理 vs CGLIB 代理源码对比

### 5.1 JDK 动态代理（JdkDynamicAopProxy）

```java
// 核心：InvocationHandler.invoke
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // ① Object 方法（equals/hashCode/toString）直接处理
    // ② 获取拦截器链
    // ③ 无链 → 直接反射调用目标
    // ④ 有链 → ReflectiveMethodInvocation 执行链
    // 限制：只能代理接口方法；final/静态方法不可代理
}
```

### 5.2 CGLIB 代理（CglibAopProxy）

```java
// 核心：生成目标类的子类 + MethodInterceptor（Callback）
// ① 生成子类（Enhancer）：重写非 final 方法
// ② 调用 intercept() → 同 JDK 的链式执行
// ③ 特有：可代理无接口类、可用 protected 方法
// 限制：final 类/方法不可代理（CGLIB 无法继承）
```

### 5.3 自调用问题（高频面试）

```text
场景：UserService 的 a() 调用本类 b()，b() 有 @Transactional/@Async
问题：a() 内部 this.b() → 调用的是【原始对象】的方法 → 不走代理
      → 事务/异步失效！

原因：代理对象的方法里，this 指向的是「代理持有的目标」，不是代理本身
解决：
  ① 注入自身（@Autowired UserService self）→ self.b()
  ② 拆 bean：把 b() 放到另一个被代理的 bean
  ③ AopContext.currentProxy()（需暴露代理）
```

---

## 6. 代理失效场景与调试

### 6.1 失效场景清单

| 场景 | 原因 | 修复 |
|------|------|------|
| 自调用 | this 调用不走代理 | 注入自身/拆 bean |
| final 类/方法 | CGLIB 无法继承 | 去掉 final |
| private 方法 | 代理不可见 | 改 public/protected |
| 静态方法 | 不参与实例代理 | 无法代理，重构 |
| 非容器 bean（new） | 未走代理链 | 交给容器 |
| 切点未匹配 | 表达式没覆盖 | 检查 execution 表达式 |
| 循环依赖 + @Async | 提前暴露无代理 | ObjectProvider/@Lazy（05 篇第 6 节） |

### 6.2 调试手段

```text
① 确认 bean 是否代理：AopUtils.isAopProxy(bean) / bean.getClass() 打印
② 查看代理类名：$Proxy（JDK）/ EnhancerByCGLIB（CGLIB）
③ 开启 AOP 日志：logging.level.org.springframework.aop=DEBUG
④ 检查 Advisor：AbstractAdvisorAutoProxyCreator 日志输出匹配结果
```

```java
// 快速判断代理类型（调试用）
Object bean = ctx.getBean("userService");
System.out.println(AopUtils.isJdkDynamicProxy(bean));   // JDK 代理
System.out.println(AopUtils.isCglibProxy(bean));        // CGLIB 代理
```

> 🎯 **核心要点**：代理机制 = **两种实现（JDK 接口代理 / CGLIB 子类代理）+ 三个入口（@Configuration 单例保证、@EnableAspectJAutoProxy 的 AOP、循环依赖提前代理）+ 一条链（Advisor 责任链）**。面试四问必背：@Configuration 为什么代理（单例语义）、Boot 为什么默认 CGLIB（统一+性能）、自调用为什么失效（this 不走代理）、final/private 为什么不行（CGLIB 限制）。Spring 7.0 代理选择逻辑不变（JDK/CGLIB 规则稳定）。

---

**下一模块**：[07-SpringCore面试源码题清单](07-SpringCore面试源码题清单.md) | **返回总览**：[00-SpringCore专题总览](00-SpringCore专题总览.md)
