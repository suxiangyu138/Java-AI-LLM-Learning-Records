# 03 @Transactional 速查

> 注解属性全表、生效条件与失效场景、拦截器链路——"一行注解如何变成完整事务"的答案

---

## 📚 目录

1. [注解属性全表](#1-注解属性全表)
2. [生效条件清单](#2-生效条件清单)
3. [失效场景与原因](#3-失效场景与原因)
4. [拦截器链路：@Transactional 幕后](#4-拦截器链路transactional-幕后)
5. [类级 vs 方法级与组合使用](#5-类级-vs-方法级与组合使用)

---

## 1. 注解属性全表

```java
@Transactional(
    value = "orderTxManager",                    // 指定事务管理器（多管理器时）
    transactionManager = "orderTxManager",       // 同上（别名）
    propagation = Propagation.REQUIRED,          // 传播行为（默认）
    isolation = Isolation.DEFAULT,               // 隔离级别（默认跟随数据库）
    timeout = 30,                                // 事务超时（秒），默认 -1 不超时
    readOnly = false,                            // 只读优化提示（非强制）
    rollbackFor = Exception.class,               // 指定回滚的异常类型
    rollbackForClassName = "java.lang.Exception",// 字符串形式
    noRollbackFor = NoRetryException.class,      // 不回滚的异常类型
    noRollbackForClassName = "...",              // 字符串形式
    label = {"order", "reporting"}               // 标签（监控/审计用）
)
```

| 属性 | 默认 | 说明 |
|------|------|------|
| `propagation` | REQUIRED | 见 [04-传播行为速查](04-传播行为速查.md) |
| `isolation` | DEFAULT | 跟随数据库默认（MySQL=可重复读，PG=读已提交） |
| `timeout` | -1 | 事务执行超时秒数（0/-1 不限制） |
| `readOnly` | false | **提示性优化**：JDBC 下不强制；JPA 下 flush 模式优化 |
| `rollbackFor` | RuntimeException/Error | **受检异常默认不回滚**（见 [07-回滚机制与测试速查](07-回滚机制与测试速查.md)） |
| `transactionManager` | @Primary 管理器 | 多管理器必须显式指定 |

> 🎯 **核心要点**：`readOnly=true` 不是数据库只读锁——是给框架的优化提示（JPA 关 dirty checking、JDBC 部分驱动跳过获取写锁）；真正只读约束在数据库层做。

## 2. 生效条件清单

| 条件 | 说明 |
|------|------|
| 方法 public | private/protected/包私有**不生效**（CGLIB/JDK 代理都拦不到） |
| 通过代理调用 | 外部 Bean 调用（容器注入的是代理对象） |
| 类在容器中 | @Component/@Service 等注册为 Bean |
| 管理器存在 | 容器内有对应 PlatformTransactionManager（或事务管理器显式指定） |
| 异常满足回滚规则 | 见回滚机制篇 |
| 方法不抛出自调用陷阱 | 见失效场景 |

```java
@Service
public class OrderService {
    @Transactional
    public void create(OrderCmd cmd) { ... }        // ✅ 外部调用生效

    @Transactional
    private void secret() { ... }                    // ❌ private 永不生效

    public void wrapper() {
        create(cmd);                                  // ❌ 同类自调用：绕过代理
    }
}
```

> 💡 构造器、初始化后回调（@PostConstruct）中调用事务方法同样不生效——代理还没就位/自调用。

## 3. 失效场景与原因

| 场景 | 根因 | 解法 |
|------|------|------|
| 同类自调用 `this.method()` | 绕过代理（代理在外部调用时才拦） | 注入自身代理 `ObjectProvider<Self>` / 拆类 / `TransactionTemplate` 包裹 |
| private/final 方法 | 代理无法拦截 | 改 public；final 类用 CGLIB 时也拦不住——7.0 默认 CGLIB，**final 类/方法不可代理** |
| 异常被 catch 吞掉 | 事务只看"方法抛不抛" | 捕获后决定：`TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()` 或重新抛出 |
| 异常类型不匹配 | 受检异常默认不回滚 | `rollbackFor = Exception.class` 显式声明 |
| @Async 线程内调用 | 事务资源线程绑定（ThreadLocal） | 事务逻辑放调用方线程；异步任务内部自己开事务 |
| 多数据源未指定管理器 | 用了 @Primary 以外的库 | `transactionManager` 属性显式指定 |
| 类未注册为 Bean | 没有 @Component 等 | 检查扫描路径 |
| @Transactional 与代理初始化顺序 | BeanPostProcessor 时序 | 避免在构造器/@PostConstruct 内依赖事务能力 |
| 7.0 final 类问题 | CGLIB 不能代理 final 类 | 去掉 final（7.0 默认 CGLIB 后需注意） |

> ⚠️ **7.0 新坑**：Boot 4 默认 CGLIB——**final 类/方法、private 方法都不再被代理**（6.x JDK 接口代理时代 final 类反而可以）；把 @Transactional 放在 final 类上 7.0 会静默失效。

## 4. 拦截器链路：@Transactional 幕后

```text
@EnableTransactionManagement → 注册 TransactionInterceptor（BeanPostProcessor 装配代理）
  → 方法调用 → TransactionInterceptor.invoke
      → TransactionAspectSupport.invokeWithinTransaction
          1. TransactionAttributeSource 解析方法上的 @Transactional（类级/方法级合并）
          2. txManager.getTransaction(def)          → 开启/加入（连接绑定线程）
          3. 执行业务方法（invoke 目标）
          4. 成功 → commit(status)；异常 → 按回滚规则 rollback/commit
          5. 清理：解除绑定、归还连接
```

> 🎯 **核心要点**：@Transactional 本质是 **TransactionInterceptor 的环绕通知**（AOP 概念）——拦截器的环绕逻辑定义在 `TransactionAspectSupport`（事务模板方法），业务方法只是被包裹的执行体；7.0 代理默认 CGLIB 只影响"如何生成代理"，链路不变。

## 5. 类级 vs 方法级与组合使用

| 位置 | 语义 | 注意 |
|------|------|------|
| 类级 | 类内所有 public 方法默认生效 | 类上先声明默认属性 |
| 方法级 | **覆盖**类级（合并解析：方法属性优先） | 未写属性继承类级 |
| 接口标注 | 类实现接口时注解可放接口方法上 | 7.0 CGLIB 下接口注解也能识别（推荐放类/方法上更明确） |

```java
@Service
@Transactional(readOnly = true)                    // 类级：默认只读
public class OrderQueryService {
    @Transactional(readOnly = false)               // 方法级覆盖：写操作
    public void updateStatus(Long id, String status) { ... }

    public Order findById(Long id) { ... }          // 继承类级 readOnly=true
}
```

> 💡 团队约定：**读接口类级 readOnly=true、写方法显式覆盖**——语义清晰且给 JPA 优化提示；注意同类的"读方法调写方法"仍是自调用陷阱（读方法代理内调 this.updateStatus 不走代理）。

---

**下一模块**：[04-传播行为速查](04-传播行为速查.md)　**返回总览**：[00-Spring TX组件总览](00-Spring TX组件总览.md)
