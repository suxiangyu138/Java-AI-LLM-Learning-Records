# 03-Spring事务管理深度剖析
> 🔥 Spring面试第一高频考点 — 掌握事务隔离级别、传播行为、@Transactional失效全场景，理解PlatformTransactionManager底层运作机制

---

## 目录
1. [本章总览](#1-本章总览)
2. [事务基础理论-ACID与隔离级别](#2-事务基础理论-acid与隔离级别)
3. [Spring事务管理两种方式](#3-spring事务管理两种方式)
4. [事务传播行为详解](#4-事务传播行为详解)
5. [@Transactional注解深入](#5-transactional注解深入)
6. [事务失效全场景-12大场景](#6-事务失效全场景-12大场景)
7. [事务底层源码分析](#7-事务底层源码分析)
8. [高频踩坑与误区](#8-高频踩坑与误区)
9. [随堂基础练习](#9-随堂基础练习)
10. [章节综合实操案例](#10-章节综合实操案例)
11. [分层综合习题](#11-分层综合习题)
12. [本章复盘速记清单](#12-本章复盘速记清单)
13. [精通拓展补充-P2](#13-精通拓展补充-p2)

---

## 1. 本章总览

### 1.1 知识定位
- **归属**：Spring Framework 核心 → P0核心必学（重中之重⭐⭐⭐⭐⭐）
- **前置依赖**：IoC容器 + AOP（事务底层基于AOP实现）
- **面试频率**：Spring模块**最高频**考点，没有之一

### 1.2 三层学习目标

| 级别 | 目标 | 检验标准 |
|------|------|----------|
| **基础** | 理解ACID、会使用`@Transactional` | 完成带事务的Service方法 |
| **熟练** | 掌握传播行为、隔离级别，能正确配置事务 | 业务场景中正确选择传播行为 |
| **精通** | 吃透失效全场景、理解`PlatformTransactionManager`源码 | 面试画事务流程图，排查生产事务失效问题 |

---

## 2. 事务基础理论-ACID与隔离级别

### 2.1 事务四大特性 ACID

| 特性 | 含义 | 实现方式 |
|------|------|----------|
| **A**tomicity 原子性 | 要么全做，要么全不做 | undo log（回滚日志） |
| **C**onsistency 一致性 | 事务前后数据满足完整性约束 | 由其他三个特性共同保证 |
| **I**solation 隔离性 | 并发事务间互不干扰 | 锁 + MVCC（多版本并发控制） |
| **D**urability 持久性 | 提交后数据永久保存 | redo log（重做日志） |

### 2.2 数据库隔离级别与并发问题

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | MySQL默认 |
|----------|------|-----------|------|-----------|
| **READ UNCOMMITTED** | ✅ 可能 | ✅ 可能 | ✅ 可能 | |
| **READ COMMITTED** | ❌ 解决 | ✅ 可能 | ✅ 可能 | Oracle默认 |
| **REPEATABLE READ** | ❌ 解决 | ❌ 解决 | ⚠️ 可能（InnoDB中通过间隙锁解决） | ✅ MySQL默认 |
| **SERIALIZABLE** | ❌ 解决 | ❌ 解决 | ❌ 解决 | |

### 2.3 Spring中的隔离级别配置

```java
@Service
public class UserService {
    
    // 默认使用数据库的隔离级别
    @Transactional
    public void defaultLevel() { }
    
    // 显式指定隔离级别
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void readCommittedLevel() { }
    
    // 可重复读
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void repeatableReadLevel() { }
}
```

---

## 3. Spring事务管理两种方式

### 3.1 编程式事务 vs 声明式事务

| 维度 | 编程式事务 | 声明式事务(@Transactional) |
|------|-----------|---------------------------|
| 实现方式 | `TransactionTemplate` / `PlatformTransactionManager` | `@Transactional`注解 |
| 代码侵入性 | ⚠️ 高，业务代码中混合事务逻辑 | ✅ 低，注解即可 |
| 粒度控制 | ✅ 精细（可精确到某几行代码） | ⚠️ 粗粒度（方法级别） |
| 使用场景 | 少量特殊场景需要精确事务控制时 | 95%的业务场景 |
| 推荐度 | 特定场景 | ⭐⭐⭐⭐⭐ |

```java
// 编程式事务
@Service
public class OrderService {
    @Autowired
    private TransactionTemplate transactionTemplate;
    
    public void createOrder(Order order) {
        transactionTemplate.execute(status -> {
            // 事务内的操作
            orderDao.insert(order);
            inventoryDao.decrease(order.getProductId(), order.getCount());
            return null;
        });
    }
}

// 声明式事务（最常用）
@Service
public class OrderService {
    @Transactional
    public void createOrder(Order order) {
        orderDao.insert(order);
        inventoryDao.decrease(order.getProductId(), order.getCount());
    }
}
```

---

## 4. 事务传播行为详解

> 🎯 传播行为定义了一个事务方法被另一个事务方法调用时的行为

### 4.1 七种传播行为完整对比

| 传播行为 | 含义 | 外层有事务 | 外层无事务 | 使用频率 |
|----------|------|-----------|-----------|----------|
| **REQUIRED**（默认） | 必须要有事务 | 加入外层事务 | 新建事务 | ⭐⭐⭐⭐⭐ |
| **REQUIRES_NEW** | 必须新建事务 | 挂起外层，新建独立事务 | 新建事务 | ⭐⭐⭐⭐ |
| **NESTED** | 嵌套事务 | 创建保存点，可部分回滚 | 新建事务 | ⭐⭐⭐ |
| **SUPPORTS** | 支持事务 | 加入外层事务 | 以非事务执行 | ⭐⭐ |
| **NOT_SUPPORTED** | 不支持事务 | 挂起外层，非事务执行 | 非事务执行 | ⭐ |
| **MANDATORY** | 强制事务 | 加入外层事务 | 抛异常 | ⭐ |
| **NEVER** | 绝不使用事务 | 抛异常 | 非事务执行 | ⭐ |

### 4.2 核心传播行为代码演示

```java
// REQUIRED（默认）：有事务则加入，无则新建
@Transactional(propagation = Propagation.REQUIRED)
public void methodA() {
    // 整个methodA在一个事务中
    userDao.save(user);         // 操作1
    methodB();                  // 调用内层方法 —— 不开启新事务！共享同一个事务
    orderDao.save(order);       // 操作3
}

@Transactional(propagation = Propagation.REQUIRED)
public void methodB() {
    productDao.update(product); // 操作2
}
// → 操作1、2、3 都在同一个事务中，任意一个失败全部回滚

// REQUIRES_NEW：总是新建独立事务，外层事务被挂起
@Transactional(propagation = Propagation.REQUIRED)
public void methodA() {
    userDao.save(user);         // 事务1：操作1
    methodB();                  // 事务1挂起，新建事务2
    // 如果methodB失败回滚，不影响事务1
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void methodB() {
    productLogDao.save(log);    // 事务2：操作2（独立提交回滚）
}

// NESTED：嵌套事务，内层回滚只到保存点
@Transactional(propagation = Propagation.REQUIRED)
public void methodA() {
    userDao.save(user);          // 外层事务
    try {
        methodB();               // 嵌套事务（创建保存点savepoint）
    } catch (Exception e) {
        // 只回滚methodB，不影响外层
    }
}

@Transactional(propagation = Propagation.NESTED)
public void methodB() {
    productDao.update(product);  // 失败只回滚到此保存点
}
```

### 4.3 REQUIRED vs REQUIRES_NEW vs NESTED

| 维度 | REQUIRED | REQUIRES_NEW | NESTED |
|------|----------|-------------|--------|
| 事务数量 | 1个事务 | 2个独立事务 | 1个事务+保存点 |
| 内层回滚影响 | 影响外层 | **不影响**外层 | **不影响**外层（可选） |
| 外层回滚影响 | 影响内层 | **不影响**内层（已完成） | 影响内层 |
| 实现依赖 | 通用 | 通用 | 需要JDBC Savepoint支持 |
| 常用场景 | 一般业务 | 日志记录（失败不影响主流程） | 批处理中可跳过失败项 |

---

## 5. @Transactional注解深入

### 5.1 完整属性列表

```java
@Transactional(
    propagation = Propagation.REQUIRED,           // 传播行为
    isolation = Isolation.DEFAULT,                // 隔离级别
    timeout = 30,                                  // 超时时间（秒），默认-1不超时
    readOnly = false,                              // 只读事务（优化查询性能）
    rollbackFor = {Exception.class},               // 哪些异常回滚（默认RuntimeException+Error）
    noRollbackFor = {IllegalArgumentException.class}, // 哪些异常不回滚
    transactionManager = "transactionManager",     // 指定事务管理器（多数据源时）
    label = "orderTransaction",                    // 事务标签
    value = "transactionManager"                   // transactionManager的别名
)
public void createOrder(Order order) { ... }
```

### 5.2 rollbackFor陷阱

```java
// ❌ 默认不回滚受检异常（Checked Exception）！
@Transactional
public void saveFile() throws IOException {
    fileDao.save(file);     // 如果这里抛出SQLException(RuntimeException) → 回滚
    file.saveToDisk();      // 如果这里抛出IOException(Checked Exception) → 不回滚！
}

// ✅ 明确指定回滚所有异常
@Transactional(rollbackFor = Exception.class)
public void saveFile() throws IOException {
    fileDao.save(file);
    file.saveToDisk();      // IOException也会回滚
}

// 最佳实践：业务异常继承RuntimeException
public class BusinessException extends RuntimeException { ... }
```

### 5.3 readOnly优化

```java
// ✅ 查询方法使用readOnly=true（MySQL InnoDB中可提升性能）
@Transactional(readOnly = true)
public User getUser(Long id) {
    return userDao.findById(id);
}

// ❌ 写操作不能加readOnly
@Transactional(readOnly = true)
public void createUser(User user) { // 会报错或静默失效！
    userDao.insert(user);
}
```

### 5.4 @Transactional可标注位置

```java
// 类级别：所有public方法生效（不推荐，粒度太粗）
@Service
@Transactional
public class UserService { ... }

// 方法级别：单个方法生效（推荐）
@Transactional
public void createUser(User user) { ... }

// 方法级别 > 类级别（就近原则）
@Service
@Transactional(readOnly = true)  // 类级别：默认只读
public class UserService {
    
    @Transactional  // 方法级别：覆盖类的readOnly，实际可写
    public void createUser(User user) { ... }
    
    public User getUser(Long id) { // 继承类级别：readOnly=true
        return userDao.findById(id);
    }
}

// 接口上（不推荐，仅在基于接口的代理中生效）
public interface UserService {
    @Transactional
    void createUser(User user);
}
```

---

## 6. 事务失效全场景-12大场景

> 🔥🔥🔥 这是Spring面试和线上排查的最核心内容，必须逐条掌握！

### 场景汇总表

| 序号 | 场景 | 原因 | 严重度 |
|------|------|------|--------|
| 1 | **方法非public** | Spring事务通过AOP代理，无法代理非public方法 | 🔴 高频 |
| 2 | **同类内部调用** | this调用绕过代理对象 | 🔴 最高频 |
| 3 | **异常被捕获** | try-catch吞了异常，AOP感知不到 | 🔴 高频 |
| 4 | **rollbackFor设置错误** | 受检异常默认不回滚 | 🔴 高频 |
| 5 | **传播行为配置不当** | 如REQUIRES_NEW/NEVER等 | 🟡 偶发 |
| 6 | **数据库引擎不支持** | MySQL MyISAM不支持事务 | 🟡 |
| 7 | **多线程环境** | 线程中拿不到Spring事务上下文 | 🟡 |
| 8 | **没有被Spring管理** | 自己new的对象没有代理 | 🟡 |
| 9 | **final方法** | CGLIB无法代理final方法 | 🟡 |
| 10 | **static方法** | Spring AOP不支持static | 🟢 较少 |
| 11 | **异常类型不匹配** | 抛的是noRollbackFor指定的异常 | 🟢 |
| 12 | **多数据源未指定事务管理器** | 多数据源时未指定正确的事务管理器 | 🟢 |

### 场景1：方法非public

```java
// ❌ 失效：非public方法
@Service
public class UserService {
    @Transactional
    void createUser(User user) { // 包级访问权限，AOP无法代理
        userDao.insert(user);
    }
}

// ✅ 修复：改为public
@Transactional
public void createUser(User user) {
    userDao.insert(user);
}
```

### 场景2：同类内部调用（最高频！）

```java
@Service
public class UserService {
    
    public void register(User user) {
        userDao.insert(user);        // 在事务外
        this.sendWelcomeEmail(user); // ❌ this调用，不经过代理！事务不生效！
    }
    
    @Transactional
    public void sendWelcomeEmail(User user) {
        emailDao.insert(new Email(user)); // 发送失败不会回滚user插入！
    }
}

// ✅ 解决方案1：拆分为两个类（推荐）
@Service
public class UserService {
    @Autowired private EmailService emailService;
    
    @Transactional
    public void register(User user) {
        userDao.insert(user);
        emailService.sendWelcomeEmail(user); // 通过注入的代理调用
    }
}

// ✅ 解决方案2：注入自身
@Service
public class UserService {
    @Autowired private UserService self;
    
    @Transactional
    public void register(User user) {
        userDao.insert(user);
        self.sendWelcomeEmail(user); // 通过self代理调用
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendWelcomeEmail(User user) { ... }
}
```

### 场景3：异常被捕获吞掉

```java
// ❌ 失效：try-catch吞了异常
@Transactional
public void createOrder(Order order) {
    try {
        orderDao.insert(order);          // 成功插入
        inventoryDao.decrease(order);    // 这里抛RuntimeException！
    } catch (Exception e) {
        log.error("创建订单失败", e);     // 异常被捕获吞掉，事务不回滚！
    }
}
// → order已经插入数据库且提交了！

// ✅ 修复1：让异常抛出
@Transactional
public void createOrder(Order order) {
    try {
        orderDao.insert(order);
        inventoryDao.decrease(order);
    } catch (Exception e) {
        log.error("创建订单失败", e);
        throw new RuntimeException("创建订单失败", e); // 抛出异常让事务回滚
    }
}

// ✅ 修复2：手动回滚
@Transactional
public void createOrder(Order order) {
    try {
        orderDao.insert(order);
        inventoryDao.decrease(order);
    } catch (Exception e) {
        log.error("创建订单失败", e);
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); // 手动设置回滚
    }
}
```

### 场景4：rollbackFor错误

```java
// ❌ 受检异常不回滚！
@Transactional
public void importExcel(File file) throws IOException {
    userDao.batchInsert(parse(file)); // SQL成功
    // 业务异常
    throw new IOException("文件格式错误"); // 受检异常，默认不回滚！数据已持久化！
}

// ✅ 指定所有异常都回滚
@Transactional(rollbackFor = Exception.class)
public void importExcel(File file) throws IOException {
    userDao.batchInsert(parse(file));
    throw new IOException("文件格式错误"); // 现在会回滚了
}
```

### 场景5：多线程

```java
// ❌ 失效：子线程中获取不到事务上下文
@Transactional
public void batchProcess(List<User> users) {
    users.parallelStream().forEach(user -> {
        userDao.insert(user); // ❌ 多线程并行，每个线程独立事务（或无事务）
    });
}

// ✅ 正确：每个线程单独管理事务
@Autowired
private TransactionTemplate transactionTemplate;

public void batchProcess(List<User> users) {
    users.parallelStream().forEach(user -> {
        transactionTemplate.execute(status -> {
            userDao.insert(user); // 每个线程独立事务
            return null;
        });
    });
}
```

---

## 7. 事务底层源码分析

### 7.1 核心接口关系

```
PlatformTransactionManager (接口) — 事务管理器顶层接口
    ├── DataSourceTransactionManager   — JDBC事务
    ├── JpaTransactionManager          — JPA事务  
    └── JtaTransactionManager          — 分布式事务(JTA)

TransactionDefinition (接口) — 事务定义（传播行为/隔离级别/超时/只读）
    └── DefaultTransactionDefinition   — 默认实现

TransactionStatus (接口) — 事务状态（是否新事务/是否已完成/回滚标记）
    └── DefaultTransactionStatus       — 默认实现
```

### 7.2 Spring事务执行完整流程

```
1. 方法调用进入代理对象（@Transactional触发AOP）
    ↓
2. TransactionInterceptor.invoke() 拦截
    ↓
3. TransactionAspectSupport.invokeWithinTransaction()
    ↓
4. 获取TransactionAttribute（解析@Transactional注解属性）
    ↓
5. 获取PlatformTransactionManager（多数据源时按qualifier获取）
    ↓
6. tm.getTransaction(definition) → 关键步骤！
    ├── 如果当前有事务 → 根据传播行为决定加入/挂起/抛异常
    └── 如果当前无事务 → 根据传播行为决定新建/非事务执行/抛异常
    ↓
7. 执行目标方法（MethodInvocation.proceed()）
    ↓
8. 根据结果处理：
    ├── 正常返回 → tm.commit(status)
    └── 抛异常 → 判断是否回滚异常
        ├── 是 → tm.rollback(status)
        └── 否 → tm.commit(status)  ← 不回滚！直接提交！
```

### 7.3 事务同步机制 - TransactionSynchronization

```java
// 在事务提交前后执行自定义逻辑
@Transactional
public void createOrder(Order order) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 事务提交成功后执行（如发送MQ消息）
                rocketMQTemplate.send("order-topic", order);
            }
            
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    log.warn("事务回滚了，清理资源");
                }
            }
        }
    );
    
    orderDao.insert(order);
}

// 或使用简化写法（Spring 4.2+）
@Transactional
public void createOrder(Order order) {
    orderDao.insert(order);
    // 事务提交成功后执行
    TransactionSynchronizationManager
        .registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendMQ(order); // 只有事务提交成功才发消息
            }
        });
}
```

---

## 8. 高频踩坑与误区

| 序号 | 踩坑 | 正确做法 |
|------|------|----------|
| 1 | `@Transactional`放Controller层 | 默认放在Service层（符合DDD分层） |
| 2 | 大事务不设timeout | 设置合理的timeout，避免长事务锁表 |
| 3 | 大事务中调用RPC/HTTP | IO远程调用不要放在事务中！提出来 |
| 4 | `readOnly=true`写了数据 | 只读事务中写数据会报错或被忽略 |
| 5 | 忘了`@EnableTransactionManagement` | SpringBoot自动配置已包含，Spring MVC需手动加 |

---

## 9. 随堂基础练习

1. 写出七种事务传播行为的名称和含义
2. MySQL的四种隔离级别分别是什么？默认是哪个？
3. 使用`@Transactional(rollbackFor = Exception.class)`的正确时机是什么？

---

## 10. 章节综合实操案例

```java
// 电商下单：事务+传播行为综合示例
@Service
public class OrderService {
    @Autowired private OrderDao orderDao;
    @Autowired private InventoryService inventoryService;
    @Autowired private LogService logService;
    
    // 主事务：REQUIRED（默认）
    @Transactional(rollbackFor = Exception.class, timeout = 30)
    public Order createOrder(Order order) {
        // 1. 保存订单（主事务中）
        orderDao.insert(order);
        
        // 2. 扣减库存（REQUIRED，加入主事务 → 失败时订单也回滚）
        inventoryService.decreaseStock(order.getProductId(), order.getCount());
        
        // 3. 记录操作日志（REQUIRES_NEW，独立事务 → 即使主事务回滚，日志也保留）
        logService.recordOperateLog("创建订单", order.getId());
        
        return order;
    }
}

@Service
public class InventoryService {
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void decreaseStock(Long productId, int count) {
        int rows = productDao.decreaseStock(productId, count);
        if (rows == 0) {
            throw new BusinessException("库存不足");
        }
    }
}

@Service
public class LogService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOperateLog(String action, Long targetId) {
        logDao.insert(new OperateLog(action, targetId));
        // 即使此处抛异常影响日志记录，也不会回滚主事务的订单和库存
    }
}
```

---

## 11. 分层综合习题

### 基础题
1. 什么是事务的ACID特性？逐一解释
2. `@Transactional(readOnly = true)`的作用是什么？

### 进阶应用题
3. 描述REQUIRED和REQUIRES_NEW的区别，各适用于什么场景？
4. 为什么同类中方法调用会导致事务失效？给出两种解决方案

### 精通拔高题
5. 画出Spring事务的完整执行流程图（从`@Transactional`到数据库commit/rollback）
6. 在一个REQUIRED事务中调用REQUIRES_NEW事务，内层事务的commit发生在什么时机？外层事务如果之后回滚，内层事务会怎样？
7. 多数据源时如何配置各自的事务管理器？`@Transactional`如何指定使用哪个？

---

## 12. 本章复盘速记清单

| 类别 | 要点 |
|------|------|
| **ACID** | 原子性(undo log)、一致性、隔离性(锁+MVCC)、持久性(redo log) |
| **隔离级别** | READ_UNCOMMITTED → READ_COMMITTED → REPEATABLE_READ(MySQL默认) → SERIALIZABLE |
| **传播行为** | REQUIRED(默认)、REQUIRES_NEW(独立事务)、NESTED(保存点) |
| **注解属性** | `propagation` `isolation` `timeout` `readOnly` `rollbackFor` `noRollbackFor` |
| **失效场景TOP4** | 非public → 同类调用 → 异常被吞 → rollbackFor不匹配 |
| **核心接口** | `PlatformTransactionManager` `TransactionDefinition` `TransactionStatus` |
| **底层原理** | AOP代理 + TransactionInterceptor + 传播行为决定新建/加入/挂起 |

---

## 13. 精通拓展补充-P2

### 13.1 多数据源事务管理

```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    @Primary
    public DataSource primaryDataSource() { ... } // 主数据源
    
    @Bean
    public DataSource secondaryDataSource() { ... } // 从数据源
    
    @Bean
    @Primary
    public PlatformTransactionManager primaryTM() {
        return new DataSourceTransactionManager(primaryDataSource());
    }
    
    @Bean
    public PlatformTransactionManager secondaryTM() {
        return new DataSourceTransactionManager(secondaryDataSource());
    }
}

// 使用不同的数据源事务
@Transactional(transactionManager = "primaryTM")
public void primaryOperation() { ... }

@Transactional(transactionManager = "secondaryTM")
public void secondaryOperation() { ... }
```

### 13.2 分布式事务方案对比

| 方案 | 实现 | 一致性 | 性能 | 复杂度 |
|------|------|--------|------|--------|
| 2PC/XA | 两阶段提交 | 强一致 | 低 | 中 |
| TCC | Try-Confirm-Cancel | 最终一致 | 高 | 高 |
| 可靠消息 | 本地消息表 + MQ | 最终一致 | 中 | 中 |
| Seata AT | 自动补偿 | 最终一致 | 中 | 低 |
| Saga | 正向补偿 | 最终一致 | 高 | 中 |

### 13.3 事务事件监听（@TransactionalEventListener）

```java
// 事务提交后异步处理（如发通知、更新缓存）
@Component
public class OrderEventListener {
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        // 这里保证在事务提交后才执行
        smsService.sendOrderNotify(event.getOrder());
        cacheService.evictCache(event.getOrder().getUserId());
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void onOrderCreationFailed(OrderFailedEvent event) {
        // 事务回滚后执行补偿逻辑
        log.error("订单创建失败，补偿处理");
    }
}
```
