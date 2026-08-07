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

### 1.1 属性组合实战

```java
// 场景一：读多写少查询服务——只读 + 合理超时
@Transactional(readOnly = true, timeout = 5)
public List<Order> listOrders(Long userId) { ... }

// 场景二：审计落库——独立事务 + 全异常回滚 + 短超时
@Transactional(propagation = Propagation.REQUIRES_NEW,
               rollbackFor = Exception.class, timeout = 10)
public void auditLog(String op, String detail) { ... }

// 场景三：批量任务——NESTED 逐条保存点 + 业务异常不回滚外层
@Transactional(propagation = Propagation.NESTED,
               noRollbackFor = BadRowException.class)
public void saveRow(Row row) { ... }
```

| 组合 | 语义 | 典型场景 |
|------|------|---------|
| readOnly + timeout | 只读提示 + 快速失败 | 报表查询 |
| REQUIRES_NEW + rollbackFor=Exception | 独立事务且全回滚 | 审计、通知 |
| NESTED + noRollbackFor | 保存点部分回滚 + 可容忍业务异常 | 批量导入 |
| REQUIRED + rollbackFor=Exception | 常规写操作统一回滚 | 团队默认规范 |

> 💡 团队规范建议：**写方法统一 `rollbackFor = Exception.class`**——把"受检异常要不要回滚"的争论用一行配置终结（受检异常语义见 [07-回滚机制与测试速查](07-回滚机制与测试速查.md)）。

### 1.2 属性默认值速记

| 属性 | 默认值 | 一句话记忆 |
|------|--------|-----------|
| propagation | REQUIRED | 常规业务默认加入 |
| isolation | DEFAULT | 跟随数据库默认，别乱改 |
| timeout | -1 | 不限时（生产建议显式设） |
| readOnly | false | 优化提示不是约束 |
| rollbackFor | 运行时异常 + Error | 受检异常需显式声明 |
| transactionManager | @Primary 管理器 | 多库必须显式指定 |

> 🎯 **默认值心智**：六个默认值里只有"REQUIRED + 运行时回滚"是真正影响语义的；其余（isolation=DEFAULT、timeout=-1）是"不添乱"设计——**改默认值前先问自己：我真的需要吗**，这是事务配置最少化的原则。

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

### 2.1 代理机制：JDK 接口代理 vs CGLIB

| 维度 | JDK 接口代理 | CGLIB（7.0 默认） |
|------|-------------|------------------|
| 生成方式 | `java.lang.reflect.Proxy` | 字节码生成子类（ASM） |
| 约束 | 目标必须实现接口 | 目标类**不能 final**，方法不能 final/static/private |
| 注解位置 | 接口或实现类均可 | 实现类方法（接口注解也可解析） |
| 7.0 状态 | 需 `@Proxyable(INTERFACES)` 恢复 | 默认 |
| 调用性能 | 反射调用 | 直接方法调用（略优） |

```text
代理对象行为（两种代理一致）：
  外部调用 → 代理对象 → TransactionInterceptor.invoke → 目标方法
  自调用（this.method()）→ 直接到目标方法，拦截器不参与
```

> ⚠️ **7.0 迁移重点**：存量"final 类 + @Transactional"在接口代理时代可用（代理对象基于接口生成），CGLIB 时代直接失效——升级时用 IDE 搜索 `final class` 与事务注解共存的位置逐一处理。

### 2.2 生效验证的三种手段

| 手段 | 做法 | 特点 |
|------|------|------|
| 日志 | `logging.level.org.springframework.transaction=DEBUG` | 最快，生产可开（量小） |
| 断点 | 在方法入口断点，看调用栈是否有 `TransactionInterceptor` | 直观，本地调试 |
| 测试 | `@SpringBootTest` + 插入后抛异常断言无残留 | 最可靠，回归兜底 |

```java
// 测试验证法：事务生效则数据不残留
@SpringBootTest
class TxActiveTest {
    @Autowired OrderService orderService;

    @Test
    void rollback_should_happen() {
        assertThrows(RuntimeException.class, () -> orderService.createAndFail());
        // 若事务生效：orderDao 中无数据（自动回滚）
        assertEquals(0, orderDao.count());
    }
}
```

> 🎯 **推荐组合**：本地用断点看代理类型，CI 用测试兜底，线上用 DEBUG 日志抽检——三种手段覆盖"开发期确认、回归期防退化、线上期定位"三个时段。

### 2.3 生效条件的底层逻辑

生效条件的本质只有两条：

```text
① 方法调用必须经过代理对象（外部调用 → 代理 → 拦截器）
② 方法必须能被代理拦截（public + 非 final + 类可继承）

其余条件（类在容器中、管理器存在、异常匹配）都是这两条的推论：
  - 类不在容器 → 没有代理对象可注入
  - 没有管理器 → 代理拦截了也开不了事务（抛异常）
  - 异常不匹配 → 事务开了但没回滚（语义问题，不是生效问题）
```

> 🎯 **分类认知**：把失效场景分成两类处理——"没被拦"（代理/可见性问题，改结构）与"拦了没回滚"（异常/规则问题，改配置）——排查时先归类再动手，效率翻倍。

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

### 3.1 更多失效边界与应对代码

| 边界场景 | 现象 | 应对 |
|---------|------|------|
| 序列化/反序列化后调用服务 | 代理链丢失 | 不要在 DTO 里调服务方法 |
| 事务方法内 new 对象调服务 | new 出的不是代理 | 依赖注入替代 new |
| 异步线程内 `@Transactional` | 新线程新事务（可能非预期） | 明确语义或改 TransactionTemplate |
| 测试类上 `@Transactional` | 测试方法全部走测试事务 | 有意为之（见 [07-回滚机制与测试速查](07-回滚机制与测试速查.md)） |
| 工具类 static 方法 | 无 Bean、无代理 | 编程式事务包裹 |

```java
// 自调用的三种正确解法
@Service
public class OrderService {
    private final OrderService self;                    // ① 注入自身代理
    public OrderService(ObjectProvider<OrderService> p) {
        this.self = p.getObject();
    }

    public void wrapper() {
        self.create(cmd);                               // ✅ 走代理
        // this.create(cmd);                            // ❌ 绕过代理
    }

    @Transactional
    public void create(OrderCmd cmd) { ... }
}

// ② 拆类：把事务方法移到独立 Service（最干净，推荐）
// ③ 编程式：TransactionTemplate 包裹（见 06 篇）
```

> 🎯 **失效排查口诀**：调用者是谁、方法 public 吗、类 final 吗、在容器里吗、异常类型对吗——五连问覆盖 95% 失效场景。

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

### 4.1 TransactionAspectSupport 环绕逻辑细节

```text
invokeWithinTransaction(method, targetClass, invocation)   ← 模板方法
  ├── 1. 解析事务属性（TransactionAttributeSource）
  │       ├── 无 @Transactional → 直接执行目标方法（零额外开销）
  │       └── 有 → 进入事务路径
  ├── 2. 拿到事务管理器（按 value/transactionManager 属性选）
  ├── 3. txInfo = createTransactionIfNecessary  → getTransaction(def)
  ├── 4. try { invocation.proceed() 业务逻辑 }
  │        ├── 正常返回 → completeTransactionAfterReturning → commit
  │        └── 抛异常 → completeTransactionAfterThrowing
  │               ├── rollbackOn(异常) → 回滚
  │               └── 否则 → 提交并继续向外抛异常
  └── 5. finally → cleanupTransactionInfo（恢复挂起上下文、清理绑定）
```

| 细节 | 说明 |
|------|------|
| 属性解析缓存 | 相同方法 + 类缓存解析结果，避免反射重复 |
| 挂起恢复 | 内层 REQUIRES_NEW 结束时恢复外层上下文（suspend/resume 成对） |
| 异常吞噬 | 拦截器不吞异常——回滚后原异常继续向外抛（调用方可见） |
| 编程式兜底 | `TransactionAspectSupport.currentTransactionStatus()` 供捕获异常后标记回滚 |

> 🎯 **核心理解**：拦截器的职责是"编排提交/回滚决策"，业务方法只是被包裹的执行体——try/catch 的边界即事务边界；"方法返回 = 提交点"的直觉来自 completeTransactionAfterReturning 这一步。

### 4.2 拦截器链路上的其他通知

`@Transactional` 常与其他 AOP 通知共存，顺序决定行为：

| 通知 | 注解 | 默认顺序 | 典型问题 |
|------|------|---------|---------|
| 事务通知 | `@Transactional` | 先于业务拦截 | 自定义切面里抛异常会影响回滚判定 |
| 缓存切面 | `@Cacheable` | 事务内缓存失效问题 | 缓存未随事务回滚——提交后缓存的是旧数据 |
| 重试切面 | `@Retryable` | 重试发生在事务外 | 重试每次新建事务（正确姿势） |
| 异步切面 | `@Async` | 方法另起线程 | 事务边界在调用线程，异步方法内事务另算 |

```java
// 典型误区：重试放在事务内（❌）——第一次失败已回滚，重试仍拿旧状态
@Transactional
@Retryable(maxAttempts = 3)        // ❌ 重试逻辑在事务环绕内
public void pay() { ... }

// 正确姿势：重试在外、事务在内（✅）——每次尝试都是全新事务
@Retryable(maxAttempts = 3)        // ✅ 重试包事务
@Transactional
public void pay() { ... }
```

> ⚠️ **顺序铁律**：**重试在外、事务在内**——`@Retryable` 在上、`@Transactional` 在下（声明顺序即优先级）；反了会导致"回滚后重试同一脏事务状态"或"重试次数内消耗同一连接"。

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

### 5.1 面试追问与易错点

**面试追问：**

1. "类级 @Transactional 影响哪些方法？"——所有 public 方法（含父类公开方法，代理子类继承）；
2. "@Transactional 方法调用同类非事务方法？"——非事务方法在事务内执行（代理只拦被调方法，事务上下文已在线程上）；
3. "接口上标注与实现类标注冲突？"——实现类方法级注解优先（Spring 解析顺序：方法级 > 类级 > 接口方法）；
4. "两个 @Transactional 方法互相调用，属性怎么合并？"——各自独立解析：外层 REQUIRED 内层 REQUIRES_NEW 时按各自属性执行，无"合并"概念；
5. "注解能放在 private 方法上吗？"——能放但永不生效；7.0 CGLIB 下编译期也不报错，运行时静默失效——这就是最隐蔽的坑。

**易错点：**

| 易错点 | 正解 |
|--------|------|
| 认为类级事务"包含"方法级 | 每个方法独立解析，无"包含"概念 |
| 忽略代理目标实际类型 | 调试注意 Bean 是 `$Proxy` / `EnhancerBySpringCGLIB` 类型 |
| 方法级覆盖只写部分属性 | 未写属性仍继承类级（如类级 readOnly=true 会继承） |
| 想在事务方法内修改注解参数 | 注解是编译期常量，运行时不可改——动态传播用编程式 |
| 依赖 Spring Boot 自动代理不生效的旧姿势 | Boot 4 默认 `@EnableTransactionManagement` 已开启，无需手写 |

> 🎯 **@Transactional 专题总结**：注解 = 属性声明（是什么）+ 代理机制（怎么拦）+ 拦截器（怎么做）三层；面试从"失效五连问"切入，再落到"拦截器环绕逻辑"，即覆盖该注解全部考点。

---

**下一模块**：[04-传播行为速查](04-传播行为速查.md)　**返回总览**：[00-Spring TX组件总览](00-Spring TX组件总览.md)
