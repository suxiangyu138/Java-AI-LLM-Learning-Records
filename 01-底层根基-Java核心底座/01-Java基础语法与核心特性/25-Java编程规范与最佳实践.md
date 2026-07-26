# 25 - Java编程规范与最佳实践

> 定位：掌握企业级Java命名规范、代码风格、阿里巴巴开发手册核心规约、常见代码坏味道及重构策略，写出专业、可维护的Java代码

## 目录

1. [命名规范（阿里巴巴规约）](#1-命名规范阿里巴巴规约)
2. [代码格式与注释规范](#2-代码格式与注释规范)
3. [常量与魔法值](#3-常量与魔法值)
4. [OOP规约：POJO/Getter-Setter/toString](#4-oop规约pojogetter-settertostring)
5. [集合操作规约](#5-集合操作规约)
6. [异常处理规约](#6-异常处理规约)
7. [并发处理规约](#7-并发处理规约)
8. [代码坏味道与重构策略](#8-代码坏味道与重构策略)
9. [安全编码规范摘要](#9-安全编码规范摘要)
10. [日志规约（SLF4J/Logback）](#10-日志规约slf4jlogback)
11. [工具链与代码检查（CheckStyle/SonarQube）](#11-工具链与代码检查checkstylesonarqube)
12. [总结清单](#12-总结清单)

---

## 1. 命名规范（阿里巴巴规约）

### 1.1 分维度规范表

| 维度 | 规则 | 正例 | 反例 |
|------|------|------|------|
| **包名** | 全小写，反向域名+模块+分层 | `com.xxx.user.controller` | `com.xxx.User.utils` |
| **类/枚举** | 大驼峰（PascalCase），名词 | `UserController`、`StatusEnum` | `userService` |
| **抽象类** | 前缀 `Abstract` | `AbstractBaseService` | `BaseService` |
| **异常类** | 后缀 `Exception` | `BizException` | `BizError` |
| **方法** | 小驼峰，动宾短语 | `getUserById` | `GetUserById` |
| **变量** | 小驼峰，名词短语 | `userName` | `a`、`name1` |
| **布尔变量** | 前缀 `is`/`has`/`can` | `isVip`、`hasPermission` | `flag` |
| **常量** | 全大写 + 下划线 | `MAX_COUNT` | `maxCount` |

### 1.2 通用原则

```java
// 语义化：见名知意，禁止拼音
// 无关键字：不使用 Java 关键字（class、int 等）
// 所有类必须有 @author + 功能描述注释
```

### 1.3 方法命名前缀

| 操作类型 | 动词前缀 | 示例 |
|----------|---------|------|
| 查询 | `get/find/query` | `getUserById`、`findByCondition` |
| 创建 | `create/add/save` | `createUser`、`saveOrder` |
| 更新 | `update/modify` | `updateUserInfo` |
| 删除 | `delete/remove` | `deleteById` |
| 批量 | 前缀 `batch` | `batchUpdateStatus` |

### 1.4 标准包结构模板

```
com.xxx
├── common/constant, exception, utils, enums
├── user/controller, service(impl), mapper, entity, dto, vo
├── order/...
└── config/
```

---

## 2. 代码格式与注释规范

### 2.1 代码格式

```java
public class Example {
    public void method(int param) {     // { 前空格，不换行
        int sum = a + b * c;            // 运算符左右空格
        if (condition) {
            // ...
        } else {                        // } else { 同行
            // ...
        }
    }
}

// 缩进 4 空格（禁用 Tab）；行宽 ≤ 120 字符；方法间空一行
```

### 2.2 注释规范

```java
// 类/接口必须有 Javadoc
/**
 * 用户服务接口
 * @author Zhang San
 * @since 1.0
 */
public interface UserService { ... }

// 公有方法必须有 Javadoc（@param、@return、@throws）
// 代码注释：解释 Why 而非 What
// ❌ i = i + 1;   // 加 1  （无意义）
// ✅ 跳过第一个表头行，从索引 1 开始处理
```

---

## 3. 常量与魔法值

### 3.1 魔法值定义

> 代码中直接出现的未经定义的、含义不明的字面量。

```java
// ❌ 反例：魔法值遍布
if (status == 1) { order.setStatus(2); }

// ✅ 正例：常量定义
public class OrderConstants {
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_PAID = 2;
}
if (status == OrderConstants.STATUS_PENDING) {
    order.setStatus(OrderConstants.STATUS_PAID);
}
```

### 3.2 常量和枚举

```java
// 常量：final static，全大写 + 下划线
public static final long CACHE_EXPIRE = 3600;

// 枚举适合固定常量集合
public enum OrderStatus {
    PENDING(1, "待支付"), PAID(2, "已支付");
    private final int code;
    private final String desc;
    OrderStatus(int code, String desc) { this.code = code; this.desc = desc; }
}

// JDK 7+ 数字下划线提升可读性
long money = 1_000_000_000L;
```

---

## 4. OOP规约：POJO/Getter-Setter/toString

### 4.1 POJO 规约

```java
// ✅ 正确：布尔字段用 Boolean，不设 is 前缀
public class UserDO {
    private Long id;
    private String userName;
    private Boolean isVip;
    // getter/setter...
}

// ❌ 反例：boolean isDeleted → 某些框架序列化行为不一致
```

> ⚠️ POJO 的布尔变量不加 `is` 前缀，否则 FastJSON 等框架序列化可能异常。

### 4.2 Record 简化数据载体（JDK 17+）

```java
public record UserDTO(Long id, String name, String email) {}
// 自动生成构造器、equals、hashCode、toString
```

### 4.3 equals + hashCode 必须同时覆写

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserDO that = (UserDO) o;
    return Objects.equals(id, that.id);
}
@Override
public int hashCode() { return Objects.hash(id); }
```

### 4.4 工具类设计

```java
public final class DateUtil {
    private DateUtil() { throw new AssertionError("禁止实例化"); }
    public static String format(LocalDateTime dt, String p) {
        return dt.format(DateTimeFormatter.ofPattern(p));
    }
}
```

---

## 5. 集合操作规约

### 5.1 初始化指定容量

```java
// ❌ 不指定容量：频繁扩容（1.5 倍数组复制）
List<String> list = new ArrayList<>();
Map<String, Object> map = new HashMap<>();

// ✅ 预估容量
List<String> list = new ArrayList<>(100);
Map<String, Object> map = new HashMap<>(32);   // 负载因子 0.75
```

### 5.2 foreach 禁止增删

```java
List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));

// ❌ ConcurrentModificationException
for (String s : list) { if ("b".equals(s)) { list.remove(s); } }

// ✅ 方案一：Iterator.remove()
// ✅ 方案二：list.removeIf(s -> s.equals("b"))  （JDK 8+）
// ✅ 方案三：stream().filter().collect()
```

### 5.3 线程安全选择

| 场景 | 推荐实现 | 说明 |
|------|---------|------|
| 单线程 | `HashMap` / `ArrayList` | 性能最优 |
| 多线程读多写少 | `ConcurrentHashMap` / `CopyOnWriteArrayList` | 读无锁 |
| 多线程写多 | `ConcurrentHashMap` | CAS + 分段锁 |

> 💡 返回集合时用 `Collections.emptyList()` 替代 `return null`，避免调用方 NPE。

---

## 6. 异常处理规约

### 6.1 异常处理原则

- **精确捕获**：捕获具体异常，不捕获 `Exception` 或 `Throwable`
- **勿忽略异常**：catch 块必须有日志和正确处理，不可空 catch
- **业务异常继承 RuntimeException**：避免强制 try-catch
- **异常包含上下文**：信息中包含关键参数

### 6.2 自定义异常

```java
public class BusinessException extends RuntimeException {
    private final int code;
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}

// 使用
throw new BusinessException(400, "用户 %s 不存在", userId);
```

### 6.3 try-with-resources

```java
try (Connection conn = dataSource.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql);
     ResultSet rs = ps.executeQuery()) {
    while (rs.next()) { /* 处理 */ }
} catch (SQLException e) {
    log.error("查询失败, sql: {}", sql, e);
    throw new BusinessException(500, "查询失败");
}
// 资源自动关闭
```

### 6.4 异常日志打印

```java
// ❌ 仅 message，丢失栈信息
log.error("错误: " + e.getMessage());

// ✅ 完整栈信息
log.error("处理失败, orderId: {}", orderId, e);   // e 作为最后一个参数
```

---

## 7. 并发处理规约

### 7.1 线程池手动创建

```java
// ❌ 禁止：Executors.newFixedThreadPool() 无界队列可能 OOM

// ✅ 手动 ThreadPoolExecutor
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5, 10, 60L, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100),              // 有界队列
    new ThreadPoolExecutor.CallerRunsPolicy()    // 拒绝策略
);
```

### 7.2 锁的选择

```java
// synchronized：自动释放，简单互斥
// ReentrantLock：支持公平锁、可中断、tryLock
// ReadWriteLock：读多写少场景优化

private final ReentrantLock lock = new ReentrantLock();
public void method() {
    lock.lock();
    try { /* 临界区 */ } finally { lock.unlock(); }    // 务必 finally 释放
}
```

### 7.3 原子类与并发容器

```java
private final AtomicInteger count = new AtomicInteger(0);
count.incrementAndGet();                               // 线程安全自增

private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
cache.computeIfAbsent(key, k -> loadFromDB(k));        // 原子化写入
```

> ⚠️ `SimpleDateFormat` 线程不安全，使用 `DateTimeFormatter`；`volatile` 不保证原子性，计数用 `AtomicInteger`。

---

## 8. 代码坏味道与重构策略

### 8.1 坏味道速查

| 坏味道 | 症状 | 重构方案 |
|--------|------|----------|
| 过长方法 | > 80 行 | 抽取独立方法，单一职责 |
| 过多参数 | > 3 个 | 封装为 DTO |
| 深层嵌套 | if-else > 3 层 | 卫语句、策略模式 |
| 重复代码 | 多处相同逻辑 | 抽取公共方法 |
| 散弹式修改 | 改一个需求改多个类 | 合并相关逻辑 |
| 基本类型偏执 | String 表示 Phone/Email | 封装值对象 |

### 8.2 重构示例：卫语句

```java
// ❌ 深层嵌套
public String process(Order order) {
    if (order != null) {
        if (order.isPaid()) {
            if (order.getAmount() > 0) { return ship(order); }
            else { return "金额异常"; }
        } else { return "未支付"; }
    } else { return "订单为空"; }
}

// ✅ 卫语句提前返回
public String process(Order order) {
    if (order == null) { return "订单为空"; }
    if (!order.isPaid()) { return "未支付"; }
    if (order.getAmount() <= 0) { return "金额异常"; }
    return ship(order);
}
```

### 8.3 方法设计最佳实践

```java
// 方法 ≤ 80 行，参数 ≤ 3 个
// 返回空集合而非 null：Collections.emptyList()
// 可选返回值用 Optional：Optional<User> findUser(Long id)
```

---

## 9. 安全编码规范摘要

### 9.1 常见漏洞防护

| 漏洞 | 防护方案 |
|------|----------|
| XSS | HTML 转义 + CSP 响应头 |
| CSRF | Spring Security CSRF Token |
| SQL注入 | MyBatis `#{}` 绑定，禁止 `${}` |
| 路径穿越 | 禁止用户输入拼接路径 |
| 反序列化 | 白名单校验 |

### 9.2 密码与加密

```java
// ❌ 禁用：MD5、SHA-1、DES、ECB 模式
// ✅ 推荐
String hash = BCrypt.hashpw(password, BCrypt.gensalt());  // 密码哈希
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");  // 对称加密 256 位
// 非对称：RSA 密钥 ≥ 2048；签名：SHA256withRSA
```

### 9.3 安全自检

```java
// [ ] 用户输入校验转义   [ ] SQL 参数化（#{}）
// [ ] 密码 BCrypt 存储   [ ] 生产强制 HTTPS
// [ ] 日志敏感信息脱敏   [ ] 最小权限原则
// [ ] 密钥不硬编码       [ ] 生产关闭远程调试
```

---

## 10. 日志规约（SLF4J/Logback）

### 10.1 日志级别

| 级别 | 使用场景 |
|------|----------|
| `ERROR` | 影响业务的异常（DB 异常、第三方失败） |
| `WARN` | 需关注的异常场景（参数校验失败、降级） |
| `INFO` | 关键业务流程节点（入参出参、状态变更） |
| `DEBUG` | 开发调试，生产关闭 |
| `TRACE` | 极细粒度调试 |

### 10.2 SLF4J 规范写法

```java
private static final Logger log = LoggerFactory.getLogger(UserService.class);

// ❌ 字符串拼接（即使不输出也创建对象）
log.debug("User id: " + userId);

// ✅ 占位符（延迟构建）
log.debug("User id: {}, name: {}", userId, name);
log.error("处理失败, bizId: {}", bizId, e);        // 异常在最后

// 禁止 System.out.println
// 敏感信息脱敏（不打印密码、Token）
```

---

## 11. 工具链与代码检查（CheckStyle/SonarQube）

### 11.1 工具对比

| 工具 | 定位 | 检查范围 |
|------|------|----------|
| **CheckStyle** | 编码风格 | 命名、格式、Javadoc |
| **PMD** | 代码缺陷 | 空 catch、重复代码 |
| **SpotBugs** | 字节码分析 | NPE 风险、线程安全 |
| **SonarQube** | 全量平台 | 技术债、漏洞、坏味道 |

### 11.2 CheckStyle 核心规则

```bash
# 命名正则：^[a-z][a-zA-Z0-9]*$  （本地变量）
# 方法 ≤ 80 行，参数 ≤ 5 个
# 强制 Javadoc：类、公有方法
# 禁止 import 通配符、魔法值
```

> 💡 IntelliJ IDEA 安装 "Alibaba Java Coding Guidelines" 插件实现实时检查。

---

## 12. 总结清单

### 12.1 提交前自检

| 类别 | 检查项 | 通过 |
|------|--------|:----:|
| 命名 | 类/方法/变量符合规范，无拼音无单字母 | [ ] |
| 命名 | 常量全大写，布尔字段不加 is 前缀 | [ ] |
| 注释 | public 方法有 Javadoc，注释解释 Why | [ ] |
| 格式 | 缩进 4 空格，行宽 ≤ 120 | [ ] |
| 常量 | 无魔法值，字面量抽取为常量 | [ ] |
| 集合 | 初始化指定容量，foreach 无增删 | [ ] |
| 异常 | 精确捕获，异常含上下文参数 | [ ] |
| 资源 | try-with-resources 管理 IO/DB | [ ] |
| 日志 | 占位符写法，敏感信息脱敏 | [ ] |
| 安全 | SQL 参数化，密码 BCrypt，无密钥硬编码 | [ ] |
| 方法 | ≤ 80 行，参数 ≤ 3 个 | [ ] |

### 12.2 十大黄金法则

| # | 法则 | 说明 |
|:-:|------|------|
| 1 | **命名即文档** | 好名字胜过注释，见名知意 |
| 2 | **职责单一** | 一个方法只做一件事 |
| 3 | **尽早返回** | 卫语句减少嵌套 |
| 4 | **拒绝魔法值** | 所有字面量定义为具名常量 |
| 5 | **防御性编程** | 参数校验 + 边界检查 + 空值处理 |
| 6 | **资源自动关闭** | try-with-resources 替代 finally |
| 7 | **异常不沉默** | catch 必有日志和处理逻辑 |
| 8 | **返回空集合** | 绝不返回 null |
| 9 | **安全左移** | 编码阶段即防范注入和敏感信息泄露 |
| 10 | **持续重构** | 发现坏味道立即处理 |

> 🎯 编程规范的核心不是约束，而是**沟通**——让你的代码不仅被机器执行，更被人理解。好的规范能降低团队协作成本 50% 以上，是企业级开发的必修课。
