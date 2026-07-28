# 08 阿里巴巴 Java 开发手册精讲

> 阿里巴巴 Java 开发手册不是"又一份规范文档"，而是阿里 10 年技术沉淀的"血泪清单"——每一条强制规则背后，都是真实的线上事故

---

## 📚 目录

1. [手册全景结构](#1-手册全景结构)
2. [强制规则精讲 TOP 20](#2-强制规则精讲-top-20)
3. [推荐规则精讲 TOP 15](#3-推荐规则精讲-top-15)
4. [编程规约核心提炼](#4-编程规约核心提炼)
5. [异常日志核心提炼](#5-异常日志核心提炼)
6. [MySQL 数据库核心提炼](#6-mysql-数据库核心提炼)
7. [安全规约核心提炼](#7-安全规约核心提炼)
8. [工程结构核心提炼](#8-工程结构核心提炼)
9. [设计规约核心提炼](#9-设计规约核心提炼)
10. [手册的团队落地](#10-手册的团队落地)
11. [高频面试考点](#11-高频面试考点)
12. [动手练习](#12-动手练习)

---

## 1. 手册全景结构

### 1.1 七大章节

```text
阿里巴巴 Java 开发手册
├── 一、编程规约
│   ├── 命名风格
│   ├── 常量定义
│   ├── 代码格式
│   ├── OOP 规约
│   ├── 集合处理
│   ├── 并发处理
│   └── 控制语句
├── 二、异常日志
│   ├── 异常处理
│   └── 日志规约
├── 三、单元测试
├── 四、安全规约
├── 五、MySQL 数据库
│   ├── 建表规约
│   ├── 索引规约
│   ├── SQL 语句
│   └── ORM 映射
├── 六、工程结构
│   ├── 应用分层
│   ├── 依赖管理
│   └── 二方库依赖
└── 七、设计规约
```

### 1.2 三级规则分类

| 级别 | 标记 | 说明 | 违反后果 |
|------|------|------|---------|
| 强制 | 【强制】 | 必须遵守 | 可能导致 bug 或事故 |
| 推荐 | 【推荐】 | 最佳实践 | 代码质量下降 |
| 参考 | 【参考】 | 个人习惯 | 团队风格不统一 |

### 1.3 版本演进

| 版本 | 发布时间 | 特点 |
|------|---------|------|
| 嵩山版 | 2020.08 | 新增设计规约、工程结构 |
| 黄山版 | 2022.02 | 细化安全规约、MySQL 规约 |
| 泰山版 | 2019.06 | 新增并发处理、ORM 映射 |

---

## 2. 强制规则精讲 TOP 20

### TOP 1-5：命名风格

**【强制】1.1.1 代码中的命名均不能以下划线或美元符号开始或结束**
```java
// ❌
_name / name_ / $name / name$
// ✅
name
```

**【强制】1.1.2 严禁使用拼音与英文混合命名，更不允许直接使用中文命名**
```java
// ❌
DaZhePromotion（打折）/ getPingfenByName()
// ✅
discountPromotion / getScoreByName()
```

**【强制】1.1.3 类名使用 UpperCamelCase 风格**
```java
// ✅
ForceCode / UserDO / HTMLDocument
```

**【强制】1.1.4 方法名、参数名、成员变量、局部变量都统一使用 lowerCamelCase 风格**
```java
// ✅
localValue / getHttpMessage()
```

**【强制】1.1.5 常量命名全部大写，单词间用下划线隔开**
```java
// ✅
MAX_STOCK_COUNT / CACHE_EXPIRED_TIME
```

### TOP 6-10：OOP 规约

**【强制】1.4.1 避免通过一个类的对象引用访问此类的静态变量或静态方法，无谓增加编译器解析成本，直接用类名来访问即可**
```java
// ❌
User user = new User();
user.staticMethod();
// ✅
User.staticMethod();
```

**【强制】1.4.2 所有的覆写方法，必须加 @Override 注解**
```java
// ✅
@Override
public String toString() { }
```

**【强制】1.4.3 相同参数类型，相同业务含义，才可以使用可变参数，避免使用 Object**
```java
// ❌
public void method(Object... args)
// ✅
public void method(String... args)
```

**【强制】1.4.4 外部正在调用的或者二方库依赖的接口，不允许修改方法签名，避免对接口调用方产生影响。接口过时必须加 @Deprecated 注解**

**【强制】1.4.5 Object 的 equals 方法容易抛空指针异常，应使用常量或确定有值的对象来调用 equals**
```java
// ❌
user.equals("admin")  // user 可能为 null
// ✅
"admin".equals(user)
// ✅ 推荐
Objects.equals(user, "admin")
```

### TOP 11-15：集合处理

**【强制】2.1.1 关于 hashCode 和 equals 的处理，遵循如下规则**：
- 只要覆写 equals，就必须覆写 hashCode
- Set 存储的对象必须覆写这两个方法
- 如果自定义对象作为 Map 的键，那么必须覆写这两个方法

**【强制】2.1.2 判断所有集合内部的元素是否为空，使用 isEmpty() 方法，而不是 size() == 0**

**【强制】2.1.3 在使用 java.util.stream.Collectors 类的 toMap() 方法转为 Map 集合时，要使用 containsKey 方法进行判断，否则会抛出 NullPointerException**

**【强制】2.1.4 在使用 java.util.stream.Collectors 类的 toMap() 方法转为 Map 集合时，一定要注意当 value 为 null 时会抛 NPE**

**【强制】2.1.5 ArrayList 的 subList 结果不可强转成 ArrayList，否则会抛出 ClassCastException**

### TOP 16-20：并发处理

**【强制】3.1.1 获取单例对象需要保证线程安全，其中的方法也要保证线程安全**

**【强制】3.1.2 创建线程或线程池时请指定有意义的线程名称，方便出错时回溯**

**【强制】3.1.3 线程资源必须通过线程池提供，不允许自行显式创建线程**

**【强制】3.1.4 线程池不允许使用 Executors 去创建，而是通过 ThreadPoolExecutor 的方式**

**【强制】3.1.5 SimpleDateFormat 是线程不安全的类，一般不要定义为 static 变量，如果定义为 static，必须加锁，或者使用 DateUtils 工具类**

---

## 3. 推荐规则精讲 TOP 15

**【推荐】1.1.1 对于 Service 和 DAO 类，暴露出来的服务一定是接口，内部的实现类用 Impl 的后缀与接口区别**

**【推荐】1.1.2 如果是描述底层或系统特性的命名，使用名词 + 形容词的形式**
```java
// ✅
CacheExpiredTime / UserActiveStatus
```

**【推荐】1.2.1 不允许任何魔法值直接出现在代码中**
```java
// ❌
if (type == 1)
// ✅
private static final int TYPE_ACTIVE = 1;
if (type == TYPE_ACTIVE)
```

**【推荐】1.4.1 使用索引访问用 String 的 split 方法得到的数组时，需做最后一个分隔符后有无内容的检查**

**【推荐】1.4.2 当一个类有多个构造方法，或者多个同名方法，这些方法应该按顺序放置在一起**

**【推荐】1.4.3 设定布尔类型的数据表字段，均使用 is_xxx 的方式命名**

**【推荐】2.1.1 集合初始化时，尽量指定集合初始值大小**

**【推荐】2.1.2 使用 entrySet 遍历 Map 类集合 KV，而不是 keySet 方式进行遍历**

**【推荐】2.1.3 Map 类集合 K/V 能不能存储 null 值的情况**

**【推荐】3.1.1 资金相关的金融敏感信息，使用悲观锁策略**

**【推荐】3.1.2 使用 CountDownLatch 进行异步转操作，每个线程退出前必须调用 countDown 方法**

**【推荐】3.1.3 避免 Random 实例被多线程使用，虽然共享该实例是线程安全的，但会因竞争同一 seed 导致的性能下降**

**【推荐】4.1.1 应用中不可直接使用日志系统（Log4j、Logback）中的 API，而应依赖使用日志框架 SLF4J 中的 API**

**【推荐】4.1.2 所有的日志文件至少保存 15 天，因为有些异常具备以"周"为频次发生的特点**

**【推荐】5.1.1 业务上具有唯一特性的字段，即使是组合字段，也必须建成唯一索引**

---

## 4. 编程规约核心提炼

### 4.1 命名风格速查

| 对象 | 风格 | 示例 |
|------|------|------|
| 包名 | 全小写 | com.alibaba.user |
| 类名 | UpperCamelCase | UserService |
| 方法名 | lowerCamelCase | getUserById |
| 变量名 | lowerCamelCase | userName |
| 常量名 | 全大写+下划线 | MAX_COUNT |
| 布尔变量 | is 前缀 | isDeleted |

### 4.2 OOP 规约速查

| 规则 | 说明 |
|------|------|
| @Override | 覆写方法必须加 |
| equals | 常量放前面：`"admin".equals(user)` |
| POJO | 必须重写 toString |
| 构造器 | 只做初始化，不放业务逻辑 |

### 4.3 集合处理速查

| 规则 | 说明 |
|------|------|
| 判空 | 用 isEmpty() 不用 size() == 0 |
| 遍历 Map | 用 entrySet |
| asList | 返回不可变 List |
| subList | 是视图，修改影响原 List |

### 4.4 并发处理速查

| 规则 | 说明 |
|------|------|
| 线程池 | 禁止 Executors，用 ThreadPoolExecutor |
| ThreadLocal | 用完必须 remove |
| 锁 | 放 try-finally |
| SimpleDateFormat | 线程不安全，用 DateTimeFormatter |

---

## 5. 异常日志核心提炼

### 5.1 异常处理速查

| 规则 | 说明 |
|------|------|
| 不要 catch Throwable | 会捕获 Error |
| catch 后必须处理 | 至少打日志 |
| 异常链传递 | throw new BizException("msg", e) |
| finally 不要 return | 会覆盖 try 的 return |

### 5.2 日志规约速查

| 规则 | 说明 |
|------|------|
| 用 SLF4J | 不要直接用 Logback/Log4j |
| 占位符 | log.info("user: {}", user) |
| 异常日志 | log.error("msg", e) 最后一个参数传异常 |
| 日志级别 | ERROR 系统错误、WARN 潜在问题、INFO 业务节点 |

---

## 6. MySQL 数据库核心提炼

### 6.1 建表规约

| 规则 | 说明 |
|------|------|
| 必备字段 | id / create_time / update_time / is_deleted |
| 表名 | 小写下划线，单数 |
| 字段名 | 小写下划线 |
| 布尔字段 | is_xxx |
| 主键 | BIGINT UNSIGNED AUTO_INCREMENT |

### 6.2 索引规约

| 规则 | 说明 |
|------|------|
| 最左前缀 | 联合索引必须从最左列开始 |
| 避免函数 | WHERE 中不要对索引列做函数操作 |
| 覆盖索引 | 只查索引列，不回表 |
| 索引命名 | idx_xxx / uk_xxx |

### 6.3 SQL 语句

| 规则 | 说明 |
|------|------|
| 禁止 SELECT * | 只查需要的字段 |
| 分页带排序 | LIMIT 必须配合 ORDER BY |
| 深分页优化 | 用游标分页替代 OFFSET |
| 参数化查询 | 用 #{} 不用 ${} |

---

## 7. 安全规约核心提炼

| 规则 | 说明 |
|------|------|
| SQL 注入 | 参数化查询，禁止字符串拼接 |
| XSS | 输出编码，CSP 策略 |
| CSRF | Token 机制，GET 无副作用 |
| 敏感信息 | 日志脱敏，密码加密，传输加密 |
| 权限校验 | 每个接口必须校验权限 |

---

## 8. 工程结构核心提炼

### 8.1 应用分层

```text
开放接口层
    ↓
Web 层（Controller）
    ↓
Service 层（业务逻辑）
    ↓
Manager 层（通用业务处理）
    ↓
DAO 层（数据访问）
    ↓
MySQL / Redis / RPC
```

### 8.2 依赖管理

| 规则 | 说明 |
|------|------|
| 二方库版本号 | 主版本号.次版本号.修订号 |
| 禁止依赖传递 | 显式声明需要的依赖 |
| 避免 SNAPSHOT | 生产环境用正式版 |

---

## 9. 设计规约核心提炼

| 规则 | 说明 |
|------|------|
| 接口设计 | 只放抽象方法和常量 |
| 幂等设计 | 查询天然幂等，增删改需要设计 |
| 超时设计 | 所有外部调用必须设置超时 |
| 降级设计 | 核心链路有兜底方案 |

---

## 10. 手册的团队落地

### 10.1 IDE 插件

```text
IntelliJ IDEA 插件：Alibaba Java Coding Guidelines
- 实时检查代码规范
- 一键修复部分问题
```

### 10.2 Checkstyle 集成

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <configuration>
        <configLocation>alibaba-checkstyle.xml</configLocation>
    </configuration>
</plugin>
```

### 10.3 SonarQube 规则

```text
启用 Alibaba Java Coding Guidelines 规则集
- 强制规则：Blocker / Critical
- 推荐规则：Major
- 参考规则：Minor
```

### 10.4 Git pre-commit hook

```bash
#!/bin/sh
# .git/hooks/pre-commit
mvn checkstyle:check
if [ $? -ne 0 ]; then
    echo "代码规范检查失败"
    exit 1
fi
```

### 10.5 CI/CD 静态扫描

```yaml
# Jenkinsfile
stage('Code Quality') {
    steps {
        sh 'mvn checkstyle:check'
        sh 'mvn sonar:sonar'
    }
}
```

### 10.6 Code Review 清单

```text
□ 命名是否规范
□ equals/hashCode 是否同时重写
□ 集合是否预估容量
□ 线程池是否自定义
□ 异常是否正确处理
□ 日志是否使用占位符
□ SQL 是否参数化
□ 是否有敏感信息泄露
```

---

## 11. 高频面试考点

### Q1：为什么线程池禁止使用 Executors？
**A**：Executors.newFixedThreadPool 的队列是无界的（LinkedBlockingQueue 容量 Integer.MAX_VALUE），任务堆积会导致 OOM。newCachedThreadPool 的线程数是无界的，会创建大量线程导致 OOM。必须使用 ThreadPoolExecutor 自定义参数。

### Q2：为什么 SimpleDateFormat 线程不安全？
**A**：SimpleDateFormat 内部使用 Calendar 对象，是共享状态。多线程并发调用 format/parse 时会互相干扰，导致结果错误。应该使用线程安全的 DateTimeFormatter 或 ThreadLocal 包装。

### Q3：equals 和 hashCode 的关系？
**A**：相等的对象必须有相同的 hashCode（equals 为 true 则 hashCode 必须相等），但 hashCode 相等的对象不一定相等（哈希冲突）。HashSet/HashMap 依赖这个契约工作。

### Q4：Arrays.asList 返回的是什么？
**A**：返回的是 java.util.Arrays$ArrayList，是 Arrays 的内部类，不是 java.util.ArrayList。它是固定大小的，不支持 add/remove 操作，修改会写回原数组。

### Q5：subList 和 ArrayList 的关系？
**A**：subList 返回的是原 List 的视图，不是独立的 ArrayList。修改 subList 会影响原 List，修改原 List 会导致 subList 抛 ConcurrentModificationException。不能强转为 ArrayList。

### Q6：ThreadLocal 为什么会内存泄漏？
**A**：ThreadLocalMap 的 Entry 是弱引用（key），但 value 是强引用。当 ThreadLocal 被 GC 回收后，key 变为 null，但 value 无法被访问也无法被回收。线程池场景下线程不销毁，value 会一直持有。必须调用 remove()。

### Q7：为什么禁止 SELECT *？
**A**：1) 网络 IO 大，传输不需要的字段；2) 无法使用覆盖索引，必须回表；3) 字段变更时可能影响业务逻辑；4) 无法利用索引优化。

### Q8：@Transactional 什么情况下会失效？
**A**：1) 同类内部方法调用（绕过代理）；2) 非 public 方法；3) 异常类型不匹配（默认只回滚 RuntimeException）；4) 数据库引擎不支持事务（MyISAM）。

### Q9：HashMap 的扩容机制？
**A**：默认容量 16，负载因子 0.75。当元素数量超过 capacity * loadFactor 时扩容为 2 倍。JDK 8 使用高低位拆分优化 rehash，不需要重新计算 hash。

### Q10：ConcurrentHashMap 如何保证线程安全？
**A**：JDK 7 使用分段锁（Segment），JDK 8 改用 CAS + synchronized 锁单个 Node。put 时先 CAS 尝试，失败则 synchronized 锁当前 Node，粒度更细，并发度更高。

---

## 12. 动手练习

### 练习 1：按手册重构代码

```java
public class user_service {
    public static List getUserList() {
        List list = new ArrayList();
        // ...
        return list;
    }
    
    public void process() {
        try {
            doSomething();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
```

<details>
<summary>参考答案</summary>

```java
@Service
public class UserService {
    
    public List<User> getUsers() {
        List<User> users = new ArrayList<>();
        // ...
        return users;
    }
    
    public void process() {
        try {
            doSomething();
        } catch (Exception e) {
            log.error("process failed", e);
        }
    }
}
```

改进点：
1. 类名 UpperCamelCase
2. 返回类型指定泛型
3. 集合初始化
4. catch Exception 不是 Throwable
5. 用日志框架不是 printStackTrace

</details>

---

**上一模块**：[07-安全与性能规范](./07-安全与性能规范.md)  
**返回总览**：[00-Java代码规范知识体系总览](./00-Java代码规范知识体系总览.md)
