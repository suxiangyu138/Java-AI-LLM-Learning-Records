# ☕ Java 后端技术栈完全指南

> 涵盖 Java 后端开发所有核心名词、技术栈、核心知识点，适合入门学习与快速复习。

---

## 目录

- [一、Java 语言核心基础](#一java-语言核心基础)
- [二、构建工具与依赖管理](#二构建工具与依赖管理)
- [三、Web 基础](#三web-基础)
- [四、Spring 全家桶](#四spring-全家桶)
- [五、持久层与数据库](#五持久层与数据库)
- [六、缓存技术](#六缓存技术)
- [七、消息队列](#七消息队列)
- [八、微服务与分布式系统](#八微服务与分布式系统)
- [九、认证与授权](#九认证与授权)
- [十、测试体系](#十测试体系)
- [十一、DevOps 与部署](#十一devops-与部署)
- [十二、性能优化与 JVM](#十二性能优化与-jvm)
- [十三、设计模式与架构](#十三设计模式与架构)
- [十四、项目工程规范](#十四项目工程规范)
- [十五、推荐学习路线](#十五推荐学习路线)
- [十六、高频面试知识点](#十六高频面试知识点)

---

## 一、Java 语言核心基础

### 1.1 基础概念

| 名词 | 全称 | 解释 |
|------|------|------|
| **JDK** | Java Development Kit | Java 开发工具包，包含编译器（javac）、运行时（java）、调试器（jdb）、文档工具（javadoc）等 |
| **JRE** | Java Runtime Environment | Java 运行环境，只包含 JVM + 核心类库，用于运行 Java 程序，不包含开发工具 |
| **JVM** | Java Virtual Machine | Java 虚拟机，运行编译后的 .class 字节码文件，是"一次编写，到处运行"的核心 |
| **JIT** | Just-In-Time Compiler | 即时编译器，将频繁执行的字节码编译为本地机器码，大幅提升执行效率 |
| **GC** | Garbage Collection | 垃圾回收机制，自动释放不再被引用的对象所占用的内存 |
| **AOT** | Ahead-Of-Time Compilation | 提前编译，在构建时就将字节码编译为原生代码（GraalVM Native Image） |

### 1.2 面向对象核心

```
面向对象三大特性：

1. 封装（Encapsulation）
   - 用 private 隐藏内部实现，通过 public getter/setter 暴露
   - 目的：保护数据，降低耦合

2. 继承（Inheritance）
   - 子类 extends 父类，复用代码，建立类之间的层次关系
   - Java 单继承，但可以多实现接口（implements）

3. 多态（Polymorphism）
   - 同一个方法调用，在不同子类中表现出不同行为
   - 实现方式：方法重写（Override）+ 父类引用指向子类对象
```

#### 重载(Overload) vs 重写(Override)

| 对比维度 | 重载 Overload | 重写 Override |
|---------|-------------|-------------|
| 发生位置 | 同一个类中 | 父子类之间 |
| 方法签名 | 方法名相同，参数列表不同 | 方法名相同，参数列表相同 |
| 返回值 | 可以不同 | 必须相同（或协变返回类型） |
| 访问权限 | 可以不同 | 不能比父类更严格 |
| 多态体现 | 编译时多态 | 运行时多态 |
| 注解 | 无 | @Override |

#### 抽象类(abstract class) vs 接口(interface)

| 对比维度 | 抽象类 | 接口 |
|---------|-------|------|
| 关键字 | abstract class | interface |
| 继承数量 | 单继承 | 多实现 |
| 构造方法 | 可以有 | 不能有 |
| 成员变量 | 可以有任何类型 | 只能是 public static final 常量 |
| 方法体 | 可以有具体方法 | Java 8+ 支持 default/static 方法 |
| 访问修饰符 | 任意 | 默认 public |
| 使用场景 | "is-a" 关系，有共同状态和行为 | "can-do" 能力，定义行为契约 |
| 设计理念 | 模板模式，复用代码 | 解耦，定义规范 |

### 1.3 基础语法要点

#### 数据类型

```
基本类型（8 种）：
┌──────┬─────────┬────────┬─────────────────┐
│ 类型  │ 大小    │ 默认值  │ 包装类           │
├──────┼─────────┼────────┼─────────────────┤
│ byte  │ 1 字节  │ 0      │ Byte            │
│ short │ 2 字节  │ 0      │ Short           │
│ int   │ 4 字节  │ 0      │ Integer         │
│ long  │ 8 字节  │ 0L     │ Long            │
│ float │ 4 字节  │ 0.0f   │ Float           │
│ double│ 8 字节  │ 0.0d   │ Double          │
│ char  │ 2 字节  │ \u0000 │ Character      │
│ boolean│ JVM决定 │ false │ Boolean          │
└──────┴─────────┴────────┴─────────────────┘

自动装箱（Autoboxing）/ 拆箱（Unboxing）：
  Integer i = 10;  // 自动装箱 int → Integer
  int j = i;       // 自动拆箱 Integer → int

注意：Integer 缓存池 -128 ~ 127，超出范围会 new 新对象
```

#### 字符串

```java
// String：不可变类（final），每次修改产生新对象
String s = "hello";
s = s + " world";  // 实际上创建了新的 String 对象

// StringBuilder：可变，非线程安全，单线程下性能最好
StringBuilder sb = new StringBuilder();
sb.append("hello").append(" world");

// StringBuffer：可变，线程安全（方法都用 synchronized 修饰），性能稍差
StringBuffer sbf = new StringBuffer();

// JDK 9+ 字符串底层从 char[] 改为 byte[] + coder，节省内存（Compact Strings）
// JDK 12+ 新增 String.indent()、String.transform() 等
// JDK 15+ Text Blocks（文本块）: """ ... """ 多行字符串
```

#### 集合框架（Collections Framework）

```
                          Collection
                     ┌─────────┴─────────┐
                    List                 Set                    (Queue 独立继承)
               ┌──────┼──────┐     ┌──────┼──────┐
          ArrayList LinkedList Vector HashSet TreeSet    (LinkedHashSet 继承 HashSet)

                                           Map（独立体系，不属于 Collection）
                                      ┌──────┼──────┐
                                    HashMap  TreeMap  (LinkedHashMap 继承 HashMap, HashTable 已过时)
```

| 集合类 | 底层结构 | 线程安全 | 有序性 | 排序 | 允许 null | 特点 |
|--------|---------|---------|-------|------|----------|------|
| **ArrayList** | 动态数组 Object[] | ❌ | 插入顺序 | ❌ | ✅ | 查询快 O(1)，增删慢 O(n) |
| **LinkedList** | 双向链表 | ❌ | 插入顺序 | ❌ | ✅ | 增删快 O(1)，查询慢 O(n) |
| **Vector** | 动态数组 | ✅ (synchronized) | 插入顺序 | ❌ | ✅ | 已过时，用 CopyOnWriteArrayList 代替 |
| **HashSet** | HashMap | ❌ | ❌ | ❌ | ✅ | 去重，O(1) 查找 |
| **LinkedHashSet** | LinkedHashMap | ❌ | 插入顺序 | ❌ | ✅ | 有序去重 |
| **TreeSet** | 红黑树 | ❌ | ❌ | ✅ (自然排序) | ❌ | 排序去重，O(log n) |
| **HashMap** | 数组+链表+红黑树 | ❌ | ❌ | ❌ | ✅ (key 可一个 null) | Java 8 起链表长度≥8 转红黑树 |
| **LinkedHashMap** | HashMap+双向链表 | ❌ | 插入/访问顺序 | ❌ | ✅ | 记录插入顺序，可用于 LRU |
| **TreeMap** | 红黑树 | ❌ | ❌ | ✅ (key 排序) | ❌ (key 不能 null) | key 有序 |
| **Hashtable** | 数组+链表 | ✅ (synchronized) | ❌ | ❌ | ❌ | 已过时，用 ConcurrentHashMap |
| **ConcurrentHashMap** | 分段锁/CAS | ✅ | ❌ | ❌ | ❌ | 高并发 Map 首选 |

#### HashMap 工作原理（高频考点）

```
JDK 1.7：
  数组 + 链表（头插法），扩容时可能产生循环链表（死循环）

JDK 1.8+：
  数组 + 链表/红黑树（尾插法）
  - 默认容量 16，负载因子 0.75
  - put 流程：
    1. 计算 key 的 hashCode()
    2. hash = hashCode ^ (hashCode >>> 16)  // 高位参与运算，减少碰撞
    3. index = (n-1) & hash  // 取模，n 为数组长度（2 的幂）
    4. 该位置为空 → 直接放入
    5. 该位置有值 → 判断 key 是否相同：
       - 相同 → 覆盖
       - 不同 → 链表尾插，链表长度 ≥ 8 且数组长度 ≥ 64 → 转红黑树
  - 扩容：容量翻倍，rehash 迁移数据
  - 红黑树节点数 ≤ 6 时转回链表
```

#### 异常体系

```
                    Throwable
                   ┌────┴────┐
                 Error     Exception
              (不可处理)   ┌────┴────┐
                     RuntimeException   Checked Exception
                      (非受检异常)       (受检异常/编译时异常)
                     ├─ NullPointerException    ├─ IOException
                     ├─ IndexOutOfBoundsException├─ SQLException
                     ├─ IllegalArgumentException├─ ClassNotFoundException
                     └─ ArithmeticException     └─ InterruptedException
```

```java
// 异常处理最佳实践
try {
    // 可能抛出异常的代码
} catch (SpecificException e) {  // 捕获具体异常，不要 catch(Exception)
    // 处理异常：日志记录 + 业务处理
    log.error("具体错误信息", e);
    throw new BusinessException("用户友好的提示");  // 包装后重新抛出
} finally {
    // 一定会执行的代码，关闭资源
    // try-with-resources (Java 7+) 自动关闭 AutoCloseable 资源：
    // try (FileInputStream fis = new FileInputStream("file.txt")) { ... }
}
```

#### 泛型（Generics）

```java
// 泛型类
public class Box<T> {
    private T value;
    public T get() { return value; }
    public void set(T value) { this.value = value; }
}

// 泛型方法
public <T> T getFirst(List<T> list) { return list.get(0); }

// 泛型接口
public interface Repository<T, ID> {
    T findById(ID id);
}

// 类型通配符
// <?>         无界通配符，任意类型
// <? extends T> 上界通配符，T 或 T 的子类（生产者，只能 get）
// <? super T>   下界通配符，T 或 T 的父类（消费者，只能 add）
// PECS 原则：Producer Extends, Consumer Super

// 类型擦除（Type Erasure）：
// 编译后泛型信息被擦除，替换为上限类型或 Object
// 这就是为什么不能 new T()、不能 instanceof 泛型类型
```

#### 反射（Reflection）

```java
// 运行时获取类的信息并操作
Class<?> clazz = Class.forName("com.example.User");
Constructor<?> constructor = clazz.getDeclaredConstructor();
Object obj = constructor.newInstance();

Field field = clazz.getDeclaredField("name");
field.setAccessible(true);  // 绕过 private 限制
field.set(obj, "张三");

Method method = clazz.getDeclaredMethod("sayHello", String.class);
method.invoke(obj, "world");

// 应用：框架底层（Spring DI、MyBatis 映射）、注解处理、动态代理
// 缺点：性能低、破坏封装、安全限制
```

#### 注解（Annotation）

```java
// 元注解（注解的注解）
@Target(ElementType.METHOD)     // 注解可以用在哪儿（方法/类/字段/参数...）
@Retention(RetentionPolicy.RUNTIME) // 注解保留到什么时候（源码/类文件/运行时）
@Documented                     // 是否包含在 javadoc 中
@Inherited                      // 子类是否继承父类的该注解
@Repeatable                     // Java 8+，是否可重复标注

// 常见内置注解
@Override       // 重写父类方法（编译期校验）
@Deprecated     // 标记已过时
@SuppressWarnings  // 抑制警告
@FunctionalInterface  // 标记函数式接口（只有一个抽象方法的接口）

// 自定义注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {
    String value() default "";
    boolean printParam() default true;
}
```

### 1.4 Java 8+ 新特性速览

| 版本 | 核心新特性 |
|------|-----------|
| **Java 8** | Lambda、Stream API、Optional、新的日期时间 API（LocalDate/LocalTime）、接口 default 方法、方法引用 |
| **Java 9** | 模块系统（Jigsaw）、集合工厂方法 `List.of()`、私有接口方法、JShell |
| **Java 10** | var 局部变量类型推断 |
| **Java 11** | HTTP Client（标准）、字符串新方法、ZGC |
| **Java 14** | Record 记录类（预览→16 正式）、instanceof 模式匹配、switch 表达式 |
| **Java 17** | **LTS 长期支持版本**、密封类（Sealed Classes）、增强伪随机数生成器 |
| **Java 21** | **LTS 长期支持版本**、**虚拟线程（Virtual Threads）**、记录模式、switch 模式匹配、字符串模板（预览） |

#### Lambda 表达式 & Stream API

```java
// Lambda 语法：(参数列表) -> { 方法体 }
// 本质：函数式接口的匿名实现

// 常用函数式接口
// Predicate<T>   : T → boolean   (test)
// Consumer<T>    : T → void      (accept)
// Function<T,R>  : T → R         (apply)
// Supplier<T>    : void → T      (get)

// Stream 操作
List<String> names = users.stream()
    .filter(u -> u.getAge() > 18)          // 过滤（中间操作）
    .sorted(Comparator.comparing(User::getAge))  // 排序（中间操作）
    .map(User::getName)                     // 转换（中间操作）
    .distinct()                             // 去重（中间操作）
    .limit(10)                              // 截断（中间操作）
    .collect(Collectors.toList());          // 收集（终端操作）

// 惰性求值：没有终端操作时，中间操作不执行
// 短路操作：limit()、findFirst() 找到满足条件的就停止
```

#### Optional

```java
// 解决 NullPointerException 的容器类
Optional<User> opt = userRepository.findById(id);

// 错误用法：opt.get()  // 跟直接调用一样可能抛异常

// 正确用法
User user = opt.orElseThrow(() -> new NotFoundException("用户不存在"));
String name = opt.map(User::getName).orElse("默认名称");
opt.ifPresent(u -> System.out.println(u.getName()));

// Optional 不应该用作字段类型或方法参数，只应用于返回值
```

### 1.5 并发与多线程

#### 线程创建方式

```java
// 方式一：继承 Thread
class MyThread extends Thread {
    public void run() { /* 任务代码 */ }
}
new MyThread().start();

// 方式二：实现 Runnable
new Thread(() -> { /* 任务代码 */ }).start();

// 方式三：实现 Callable + Future（有返回值）
FutureTask<String> task = new FutureTask<>(() -> "结果");
new Thread(task).start();
String result = task.get();  // 阻塞直到获取结果

// 方式四：线程池（推荐）
ExecutorService executor = Executors.newFixedThreadPool(10);
executor.submit(() -> { /* 任务 */ });
executor.shutdown();
```

#### 线程状态（6 种）

```
New → Runnable → Running → Terminated
                      ↓
                   Blocked（等待锁）
                   Waiting（wait/join，无限等待）
                   Timed_Waiting（sleep/wait(timeout)，超时等待）
```

#### 线程同步机制

```java
// 1. synchronized 关键字（悲观锁）
public synchronized void method() { }  // 锁 this 对象
public static synchronized void method() { }  // 锁 Class 对象
synchronized(obj) { /* 代码块 */ }

// 2. Lock 接口（显式锁）
Lock lock = new ReentrantLock();
lock.lock();
try { /* 业务代码 */ } finally { lock.unlock(); }

// 3. ReadWriteLock 读写锁（读共享，写独占）
ReadWriteLock rwLock = new ReentrantReadWriteLock();

// 4. volatile 关键字（保证可见性 + 禁止指令重排，不保证原子性）
private volatile boolean flag = true;

// 5. Atomic 原子类（CAS 无锁实现，性能优于 synchronized）
AtomicInteger count = new AtomicInteger(0);
count.incrementAndGet();  // 原子自增

// 6. ThreadLocal（线程局部变量，每个线程独立副本）
ThreadLocal<String> local = new ThreadLocal<>();
local.set("value");
// 注意：使用后必须 remove()，否则可能内存泄漏

// 7. CountDownLatch（倒计时门闩，一个线程等多个线程完成）
CountDownLatch latch = new CountDownLatch(3);
latch.countDown();  // 计数减 1
latch.await();      // 阻塞直到计数归零

// 8. CyclicBarrier（循环栅栏，多个线程相互等待到齐）
CyclicBarrier barrier = new CyclicBarrier(3, () -> { /* 到齐后执行 */ });
barrier.await();    // 每个线程调用，等满 3 个后一起放行

// 9. Semaphore（信号量，控制并发访问数量）
Semaphore semaphore = new Semaphore(5);  // 最多 5 个并发
semaphore.acquire();  // 获取许可
semaphore.release();  // 释放许可

// 10. CompletableFuture（异步编排，Java 8+）
CompletableFuture.supplyAsync(() -> fetchUser())
    .thenApply(user -> user.getName())
    .thenAccept(name -> System.out.println(name))
    .exceptionally(e -> { log.error("错误", e); return null; });
```

#### 线程池（ThreadPoolExecutor）

```java
// 7 个核心参数：
new ThreadPoolExecutor(
    corePoolSize,       // 核心线程数
    maximumPoolSize,    // 最大线程数
    keepAliveTime,      // 空闲线程存活时间
    TimeUnit.SECONDS,   // 时间单位
    workQueue,          // 工作队列
    threadFactory,      // 线程工厂
    rejectedHandler     // 拒绝策略
);

// 执行流程：
// 新任务来 → 核心线程有空闲？→ 是 → 执行
//                    → 否 → 队列满了？
//                              → 否 → 入队等待
//                              → 是 → 线程数 < 最大线程数？
//                                        → 是 → 创建新线程
//                                        → 否 → 执行拒绝策略

// 四种拒绝策略：
// AbortPolicy        抛异常（默认）
// CallerRunsPolicy   由调用线程执行（反馈机制）
// DiscardOldestPolicy 丢弃最旧的任务
// DiscardPolicy       直接丢弃

// 常用线程池（不推荐 Executors 创建，应手动 new ThreadPoolExecutor）
// Executors 的问题：FixedThreadPool/CachedThreadPool 队列无界可能 OOM
```

#### synchronized 锁升级过程

```
无锁 → 偏向锁 → 轻量级锁（自旋锁/CAS）→ 重量级锁（互斥量）
        ↑ 单线程    ↑ 多线程交替          ↑ 多线程竞争激烈
        反复访问    不阻塞，自旋等待        挂起-唤醒，内核态切换
```

#### 虚拟线程（Virtual Threads）— Java 21

```java
// 传统线程：平台线程（Platform Thread），1:1 映射到 OS 线程，约 1MB 栈内存
// 虚拟线程：轻量级用户态线程，M:N 映射，约几 KB 栈内存，可创建百万级

// 创建虚拟线程
Thread vt = Thread.startVirtualThread(() -> { /* 任务 */ });
// 或通过 ExecutorService（每个任务一个虚拟线程）
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> { /* 任务 */ });
}

// 适用场景：大量 IO 密集型任务（网络请求、数据库调用）
// 不适用：CPU 密集型（计算密集）、需要固定线程数量的场景
```

---

## 二、构建工具与依赖管理

### 2.1 Maven

```xml
<!-- pom.xml 最小结构 -->
<project>
    <!-- 坐标 -->
    <groupId>com.example</groupId>      <!-- 组织/公司标识（反向域名） -->
    <artifactId>my-app</artifactId>     <!-- 项目/模块标识 -->
    <version>1.0.0-SNAPSHOT</version>   <!-- 版本号，SNAPSHOT=开发版 -->
    <packaging>jar</packaging>          <!-- 打包类型：jar/war/pom -->

    <!-- 依赖管理 -->
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <!-- version 可能由父 POM 或 BOM 管理，可不写 -->
        </dependency>
    </dependencies>

    <!-- 多模块聚合 -->
    <modules>
        <module>module-a</module>
        <module>module-b</module>
    </modules>
</project>
```

#### 核心概念

| 概念 | 解释 |
|------|------|
| **GAV 坐标** | groupId + artifactId + version 唯一定位一个依赖 |
| **SNAPSHOT** | 快照版本，开发中，每次构建拉取最新；RELEASE 是正式版，只拉一次缓存本地 |
| **scope** | 依赖作用域：compile(默认)、provided(容器提供，如 servlet-api)、runtime(运行时)、test(测试) |
| **传递性依赖** | A 依赖 B，B 依赖 C，A 自动依赖 C |
| **依赖冲突** | 不同版本的同名依赖，遵循：最短路径优先 → 先声明优先 |
| **BOM** | Bill of Materials，统一管理一组依赖版本号 |
| **plugin** | Maven 插件，执行编译、测试、打包等生命周期任务 |
| **lifecycle** | 生命周期：clean → validate → compile → test → package → verify → install → deploy |

```bash
# 常用 Maven 命令
mvn clean           # 清理 target 目录
mvn compile         # 编译源代码
mvn test            # 运行测试
mvn package         # 打包（jar/war）
mvn install         # 安装到本地仓库
mvn deploy          # 部署到远程仓库
mvn clean package -DskipTests  # 跳过测试打包
mvn dependency:tree             # 查看依赖树（排查冲突）
mvn spring-boot:run              # 启动 Spring Boot
```

### 2.2 Gradle

```groovy
// build.gradle (Groovy DSL)
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.example'
version = '1.0.0'

repositories {
    mavenCentral()  // 中央仓库
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

#### Maven vs Gradle 对比

| 对比维度 | Maven | Gradle |
|---------|-------|--------|
| 构建文件 | pom.xml (XML) | build.gradle (Groovy/Kotlin DSL) |
| 构建速度 | 较慢 | 较快（增量构建、构建缓存） |
| 灵活性 | 较低，约定大于配置 | 高，可编程 |
| 学习成本 | 低，XML 固定格式 | 中，需了解 Groovy/Kotlin |
| 市场份额 | 传统项目、银行/金融 | 新项目、Android、微服务 |

---

## 三、Web 基础

### 3.1 HTTP 协议

```http
# 请求格式
GET /api/users/1 HTTP/1.1
Host: example.com
Authorization: Bearer <token>
Content-Type: application/json

# 响应格式
HTTP/1.1 200 OK
Content-Type: application/json
Set-Cookie: sessionId=abc123

{"id":1,"name":"张三"}
```

#### HTTP 方法

| 方法 | 含义 | 幂等性 | 安全 | 用途 |
|------|------|--------|------|------|
| GET | 查询 | ✅ | ✅ | 获取资源，参数在 URL Query String |
| POST | 新增 | ❌ | ❌ | 创建资源，参数在 Request Body |
| PUT | 全量更新 | ✅ | ❌ | 替换整个资源 |
| PATCH | 部分更新 | ❌ | ❌ | 修改部分字段 |
| DELETE | 删除 | ✅ | ❌ | 删除资源 |
| HEAD | 获取头部 | ✅ | ✅ | 只获取响应头，不返回 Body |
| OPTIONS | 查询支持的方法 | ✅ | ✅ | CORS 预检请求 |

#### HTTP 状态码

```
1xx  信息          100 Continue / 101 Switching Protocols
2xx  成功          200 OK / 201 Created / 204 No Content
3xx  重定向        301 永久重定向 / 302 临时重定向 / 304 Not Modified
4xx  客户端错误    400 Bad Request / 401 Unauthorized / 403 Forbidden
                  404 Not Found / 405 Method Not Allowed / 429 Too Many Requests
5xx  服务器错误    500 Internal Server Error / 502 Bad Gateway
                  503 Service Unavailable / 504 Gateway Timeout
```

#### RESTful API 设计规范

```
GET    /api/users           查询用户列表
GET    /api/users/{id}      查询单个用户
POST   /api/users           创建用户
PUT    /api/users/{id}      全量更新用户
PATCH  /api/users/{id}      局部更新用户
DELETE /api/users/{id}      删除用户

GET    /api/users/{id}/orders   查询用户的订单
GET    /api/orders?page=1&size=20   分页查询

// 命名规范
- 资源名用名词复数
- 层级关系用路径表达
- 过滤、排序、分页用 Query Parameter
- 版本号放 URL 或 Header：/api/v1/users 或 Accept: application/vnd.api.v1+json
```

### 3.2 Cookie / Session / Token

| 概念 | 解释 |
|------|------|
| **Cookie** | 浏览器存储的小数据片段（4KB），每次请求自动携带，可设置过期时间、域名、路径 |
| **Session** | 服务端存储的用户会话数据，通过 Cookie 中的 SessionId 关联 |
| **Token** | 令牌，通常用 JWT 承载用户信息，无状态、可跨域，无须服务端存储 |

### 3.3 跨域 CORS

```
CORS（Cross-Origin Resource Sharing 跨源资源共享）：
浏览器安全策略，默认禁止跨域 AJAX 请求

Spring Boot 解决跨域：
1. @CrossOrigin 注解（在 Controller 或方法上）
2. WebMvcConfigurer.addCorsMappings() 全局配置
3. CorsFilter 过滤器
4. Nginx 反向代理（推荐生产方案）
```

### 3.4 Tomcat / Servlet

```
┌────────────────────────────────────────┐
│ 浏览器请求                               │
│     ↓                                   │
│ Nginx（反向代理）                        │
│     ↓                                   │
│ Tomcat（Servlet 容器 / Web 服务器）      │
│     ↓                                   │
│ Filter 链（过滤器）→ 前置处理            │
│     ↓                                   │
│ DispatcherServlet（前端控制器）          │
│     ↓                                   │
│ Controller（你写的）                     │
│     ↓                                   │
│ Interceptor 链（拦截器）→ 后置处理       │
│     ↓                                   │
│ 返回响应                                 │
└────────────────────────────────────────┘

Filter（过滤器）vs Interceptor（拦截器）：
- Filter：Servlet 级别，可修改请求/响应，在 Spring 容器外
- Interceptor：Spring MVC 级别，可访问 Spring Bean，在 HandlerAdapter 前后
```

#### Java Web 发展简史

```
Servlet (1997)
  ↓ 编程繁琐，大量重复代码
JSP (1999)
  ↓ 前后端混合，维护困难
Struts (2001)
  ↓ XML 配置地狱
Spring MVC (2004~)
  ↓ 注解驱动，约定优于配置
Spring Boot (2014~)
  ↓ 自动配置，开箱即用，内嵌 Tomcat
Spring Boot 3.x (2022~) — Jakarta EE 9+、Java 17 Baseline、原生镜像
```

---

## 四、Spring 全家桶

### 4.1 Spring Framework 核心

#### IoC（Inversion of Control 控制反转）

> 传统方式：自己 new 对象，管理对象的生命周期
> IoC 方式：把对象创建的"控制权"交给 Spring 容器，需要的时候找容器要

```java
// 传统方式
UserService service = new UserServiceImpl();  // 自己控制创建

// Spring IoC 方式
@Autowired
private UserService service;  // 容器注入，不用关心怎么来的
```

#### DI（Dependency Injection 依赖注入）

```java
// 1. 字段注入（不推荐，不利于单元测试，隐藏依赖）
@Autowired
private UserService userService;

// 2. Setter 注入（可选依赖）
@Autowired
public void setUserService(UserService userService) {
    this.userService = userService;
}

// 3. 构造器注入（推荐！不可变，强制依赖，方便测试）
// 只有一个构造器时 @Autowired 可省略
private final UserService userService;

// @RequiredArgsConstructor (Lombok) 自动生成构造器注入
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;  // final 修饰
}

// 4. @Resource（JSR-250，按名称注入）vs @Autowired（Spring，按类型注入）
@Resource(name = "userServiceImpl")
private UserService userService;
```

#### Bean 的生命周期

```
┌──────────────────────────────────────────────────────┐
│ 1. 实例化（Instantiation）                            │
│    Spring 通过反射调用构造方法创建对象                  │
│    ↓                                                 │
│ 2. 属性赋值（Populate Properties）                    │
│    给 Bean 的属性（@Autowired、@Value 等）注入值       │
│    ↓                                                 │
│ 3. Aware 接口回调                                      │
│    BeanNameAware → BeanFactoryAware → ApplicationContextAware
│    ↓                                                 │
│ 4. BeanPostProcessor.postProcessBeforeInitialization()│
│    ↓                                                 │
│ 5. @PostConstruct 注解方法（JSR-250）                  │
│    ↓                                                 │
│ 6. InitializingBean.afterPropertiesSet()              │
│    ↓                                                 │
│ 7. 自定义 init-method                                  │
│    ↓                                                 │
│ 8. BeanPostProcessor.postProcessAfterInitialization() │
│    ↓                                                 │
│ 9. Bean 就绪，可以被使用                               │
│    ↓                                                 │
│ 10. @PreDestroy 注解方法                               │
│    ↓                                                 │
│ 11. DisposableBean.destroy()                          │
│    ↓                                                 │
│ 12. 自定义 destroy-method                              │
│    ↓                                                 │
│ 13. 销毁完成                                           │
└──────────────────────────────────────────────────────┘
```

#### Bean 的作用域（Scope）

| 作用域 | 说明 |
|--------|------|
| **singleton** | 默认，整个容器中只有一个实例 |
| **prototype** | 每次获取都创建新实例 |
| **request** | 每个 HTTP 请求一个实例（仅 Web 应用） |
| **session** | 每个 HTTP Session 一个实例（仅 Web 应用） |
| **application** | 整个 Web 应用一个实例 |

#### AOP（Aspect-Oriented Programming 面向切面编程）

```
为什么需要 AOP？
  日志记录、事务管理、权限校验等横切关注点（Cross-cutting Concerns）
  如果没有 AOP，这些代码会散落在各个业务方法中，难以维护

AOP 核心概念：
┌──────────────┬──────────────────────────────────────────┐
│ Aspect 切面   │ 切面 = 切入点 + 通知，封装横切逻辑        │
│ JoinPoint 连接点│ 程序执行中的某个点（方法调用/异常抛出）   │
│ Pointcut 切入点│ 匹配连接点的表达式，指定在哪些方法上执行   │
│ Advice 通知   │ 在切入点做什么：@Before / @After / @Around│
│ Weaving 织入  │ 将切面应用到目标对象的过程                │
└──────────────┴──────────────────────────────────────────┘

5 种通知类型：
  @Before       前置通知：目标方法执行前
  @AfterReturning 后置返回通知：方法正常返回后
  @AfterThrowing  后置异常通知：方法抛出异常后
  @After          最终通知：方法执行后（无论是否异常）
  @Around         环绕通知：最强大，可控制方法是否执行，可修改参数和返回值
```

```java
// AOP 示例：方法执行时间日志
@Aspect
@Component
public class LogAspect {

    @Pointcut("execution(* com.example.service.*.*(..))")
    public void servicePointcut() {}

    @Around("servicePointcut()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();  // 执行目标方法
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("{} 执行耗时: {}ms", joinPoint.getSignature(), elapsed);
        }
    }
}

// Pointcut 表达式语法
// execution(修饰符 返回类型 包名.类名.方法名(参数))
// execution(* com.example.service.*.*(..))
//   *      → 任意返回类型
//   *      → 任意类
//   *      → 任意方法
//   (..)   → 任意参数
```

#### Spring 事务管理

```java
// @Transactional 原理：AOP + 动态代理
// 默认：只有 RuntimeException 和 Error 才回滚

@Transactional(
    propagation = Propagation.REQUIRED,    // 传播行为
    isolation = Isolation.READ_COMMITTED,   // 隔离级别
    timeout = 30,                           // 超时秒数
    readOnly = false,                       // 只读事务
    rollbackFor = Exception.class,          // 哪些异常回滚
    noRollbackFor = IllegalArgumentException.class  // 哪些异常不回滚
)

// 传播行为（Propagation）：
// REQUIRED       默认，有事务则加入，无则新建（最常用）
// REQUIRES_NEW   总是新建事务，挂起当前事务
// NESTED         嵌套事务，内部回滚不影响外部（需 Savepoint 支持）
// SUPPORTS       有则加入，无则非事务执行
// NOT_SUPPORTED  非事务执行，挂起当前事务
// NEVER          非事务执行，有事务则抛异常
// MANDATORY      必须有事务，无则抛异常

// 注意：@Transactional 必须通过代理调用才生效
// 同类方法互调（this.method()）不经过代理，事务不生效 → 需要 self-injection 或抽到另一个 Service
```

### 4.2 Spring MVC 详解

#### DispatcherServlet 处理流程

```
┌─────────────────────────────────────────────────────────┐
│                       请求到达                           │
│                          ↓                              │
│  1. HandlerMapping（处理器映射）                          │
│     根据请求 URL 找到对应的 Handler（Controller 方法）     │
│                          ↓                              │
│  2. HandlerAdapter（处理器适配器）                        │
│     调用 Handler，处理参数绑定、类型转换等                 │
│                          ↓                              │
│  3. HandlerInterceptor.preHandle()（拦截器前置）          │
│                          ↓                              │
│  4. Controller 方法执行（你的代码）                       │
│                          ↓                              │
│  5. HandlerInterceptor.postHandle()（拦截器后置）         │
│                          ↓                              │
│  6. ViewResolver（视图解析器）                            │
│     前后端分离：@RestController 直接 @ResponseBody 返回 JSON│
│                          ↓                              │
│  7. HandlerInterceptor.afterCompletion()（完成回调）      │
│                          ↓                              │
│  8. 响应返回客户端                                        │
└─────────────────────────────────────────────────────────┘
```

#### Spring MVC 核心注解

```java
@RestController  = @Controller + @ResponseBody  // 返回 JSON
@Controller      // 声明为控制器（返回视图名）

@RequestMapping("/api/users")  // 类级别路径映射

// HTTP 方法映射（方法级别）
@GetMapping("/{id}")       // GET 请求
@PostMapping               // POST 请求
@PutMapping("/{id}")       // PUT 请求
@DeleteMapping("/{id}")    // DELETE 请求
@PatchMapping("/{id}")     // PATCH 请求

// 参数绑定
public Result<User> getUser(
    @PathVariable Long id,                    // URL 路径参数 /users/{id}
    @RequestParam(defaultValue = "1") int page, // Query 参数 ?page=1
    @RequestBody @Valid UserDTO dto,          // JSON 请求体 + 校验
    @RequestHeader("Authorization") String token,  // 请求头
    @CookieValue("JSESSIONID") String sessionId   // Cookie
) { }

// 响应
@ResponseBody   // 返回值序列化为 JSON
@ResponseStatus(HttpStatus.CREATED)  // 指定响应状态码
ResponseEntity<User>  // 完整控制响应（状态码、头、体）
```

### 4.3 Spring Boot 核心机制

#### 自动配置原理

```
@SpringBootApplication 实际上是三个注解的组合：
  @SpringBootConfiguration  ← 等同于 @Configuration
  @EnableAutoConfiguration   ← 自动配置的核心
  @ComponentScan             ← 组件扫描

自动配置流程：
1. @EnableAutoConfiguration 引入 AutoConfigurationImportSelector
2. 读取 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
   （Spring Boot 2.x 是 spring.factories）
3. 加载其中列出的所有自动配置类（如 DataSourceAutoConfiguration、RedisAutoConfiguration...）
4. 每个自动配置类通过 @ConditionalOnXxx 条件注解判断是否生效：
   @ConditionalOnClass      → 类路径存在指定类
   @ConditionalOnMissingBean → 容器中没有指定 Bean
   @ConditionalOnProperty   → 配置文件中存在指定属性
   @ConditionalOnBean       → 容器中有指定 Bean
   @ConditionalOnResource   → 资源文件存在
   @ConditionalOnWebApplication → 是 Web 应用
5. 满足条件的配置类生效，向容器注册对应的 Bean
```

#### 配置文件

```yaml
# application.yml (更推荐 YAML 格式，层级清晰)
server:
  port: 8080                        # 服务端口
  servlet:
    context-path: /api              # 上下文路径

spring:
  application:
    name: user-service              # 应用名称
  profiles:
    active: dev                     # 激活的环境（dev/test/prod）
  datasource:
    url: jdbc:mysql://localhost:3306/mydb?useUnicode=true&characterEncoding=utf-8
    username: root
    password: ${DB_PASSWORD}        # 环境变量
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:                         # 连接池配置
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

# 多环境配置
# application-dev.yml    开发环境
# application-test.yml   测试环境
# application-prod.yml   生产环境

# 自定义配置
app:
  jwt:
    secret: my-secret-key
    expiration: 86400000  # 24 小时
```

#### Spring Boot Starter

```
Starter 是一站式依赖描述符，引入一个 Starter 即引入该功能所需的所有依赖：

spring-boot-starter-web              Web 开发（Spring MVC + 内嵌 Tomcat）
spring-boot-starter-data-jpa         JPA + Hibernate
spring-boot-starter-data-redis       Redis（默认 Lettuce 客户端）
spring-boot-starter-security         安全框架
spring-boot-starter-validation       参数校验（Hibernate Validator）
spring-boot-starter-test             测试（JUnit 5 + Mockito）
spring-boot-starter-aop              AOP
spring-boot-starter-actuator         监控端点
spring-boot-starter-mail             邮件发送
spring-boot-starter-amqp             RabbitMQ
spring-boot-starter-websocket        WebSocket

自定义 Starter 的命名规范：
  官方：spring-boot-starter-xxx
  第三方：xxx-spring-boot-starter（如 mybatis-spring-boot-starter）
```

#### Actuator 监控

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus  # 暴露的端点
  endpoint:
    health:
      show-details: always  # 显示健康详情
```

```
常用端点：
/actuator/health       健康检查
/actuator/info         应用信息
/actuator/metrics      指标数据（JVM 内存、GC、线程...）
/actuator/env          环境变量
/actuator/loggers      动态修改日志级别
/actuator/beans        容器中的 Bean 列表
/actuator/mappings     所有 URL 映射
/actuator/threaddump   线程转储
```

### 4.4 Spring Security

#### 核心概念

```
┌─────────────────────────────────────────────┐
│          Spring Security 核心概念             │
├─────────────────────────────────────────────┤
│ Authentication（认证）：你是谁？              │
│   - 验证用户身份（用户名/密码、Token、OAuth） │
│   - 结果存入 SecurityContext                  │
├─────────────────────────────────────────────┤
│ Authorization（授权）：你能干什么？           │
│   - 检查用户是否有权限访问某个资源            │
│   - 角色 ROLE_xxx、权限 xxx:read             │
├─────────────────────────────────────────────┤
│ Principal（主体）：当前登录的用户             │
├─────────────────────────────────────────────┤
│ GrantedAuthority（权限）：用户拥有的权限列表  │
├─────────────────────────────────────────────┤
│ SecurityContext：持有当前请求的安全上下文     │
├─────────────────────────────────────────────┤
│ Filter Chain：安全过滤器链                    │
```

#### JWT 认证流程

```
┌──────────────────────────────────────────────────────────┐
│ 1. 用户登录 → 提交用户名+密码                             │
│ 2. 服务端验证 → 生成 JWT → 返回给客户端                    │
│ 3. 客户端存储 Token（localStorage/Cookie）                │
│ 4. 后续请求 → Header: Authorization: Bearer <token>       │
│ 5. 服务端 JWT Filter 拦截 → 解析验证 Token → 提取用户信息   │
│ 6. 刷新 Token：Access Token 短期（15min）                  │
│               Refresh Token 长期（7d）                    │
└──────────────────────────────────────────────────────────┘

JWT 结构：Header.Payload.Signature（三个 Base64 字符串用 . 连接）
  Header   = {"alg":"HS256","typ":"JWT"}
  Payload  = {"sub":"123","name":"张三","exp":1234567890,"iat":1234567890}
  Signature = HMAC-SHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)
```

### 4.5 Spring WebFlux（响应式编程）

```
Spring MVC（Servlet 栈）→ 同步阻塞，一个请求一个线程
Spring WebFlux（Reactive 栈）→ 异步非阻塞，事件驱动

核心：Reactor 库
  Mono<T>    — 返回 0 或 1 个元素
  Flux<T>    — 返回 0 到 N 个元素

特点：
  - 高并发下资源利用率高（不需要大量线程）
  - 适合 IO 密集型、流式处理
  - 不适合 CPU 密集型
  - 生态支持不完整（JDBC 是阻塞的，需用 R2DBC）
  - 调试困难
```

---

## 五、持久层与数据库

### 5.1 JDBC（Java Database Connectivity）

```java
// JDBC 是 Java 连接数据库的最底层 API
// 任何 ORM 框架底层都是 JDBC

// 手动 JDBC（不推荐，了解即可）
String url = "jdbc:mysql://localhost:3306/mydb";
try (Connection conn = DriverManager.getConnection(url, "root", "password");
     PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?")) {
    ps.setLong(1, userId);
    try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            String name = rs.getString("name");
        }
    }
}
```

### 5.2 数据库连接池

| 连接池 | 特点 |
|--------|------|
| **HikariCP** | Spring Boot 2.x+ 默认，极致性能，字节码级优化 |
| **Druid** | 阿里开源，功能丰富，自带 SQL 监控、防火墙、可视化控制台 |
| **DBCP2** | Apache 出品，老牌 |
| **C3P0** | 老牌，已不活跃 |

```yaml
# HikariCP 配置
spring:
  datasource:
    hikari:
      minimum-idle: 10           # 最小空闲连接
      maximum-pool-size: 20      # 最大连接数
      idle-timeout: 600000       # 空闲超时 10min
      max-lifetime: 1800000      # 连接最大存活时间 30min（应比数据库 wait_timeout 短）
      connection-timeout: 30000  # 获取连接超时 30s
      connection-test-query: SELECT 1  # 连接校验 SQL
```

### 5.3 MyBatis / MyBatis-Plus

#### MyBatis

```java
// MyBatis 是半自动 ORM，SQL 由开发者编写
// 核心组件：
//   SqlSessionFactory → SqlSession → Mapper

// Mapper 接口
@Mapper  // 或启动类 @MapperScan("com.example.mapper")
public interface UserMapper {
    User findById(@Param("id") Long id);
    List<User> findByCondition(UserQuery query);
    int insert(User user);
    int update(User user);
    int deleteById(@Param("id") Long id);
}
```

```xml
<!-- UserMapper.xml -->
<mapper namespace="com.example.mapper.UserMapper">
    <!-- 结果映射 -->
    <resultMap id="BaseResultMap" type="com.example.entity.User">
        <id column="id" property="id"/>
        <result column="user_name" property="userName"/>
        <result column="create_time" property="createTime"/>
        <!-- 一对一关联 -->
        <association property="dept" javaType="Dept"
                     select="com.example.mapper.DeptMapper.findById" column="dept_id"/>
        <!-- 一对多关联 -->
        <collection property="orders" ofType="Order"
                     select="com.example.mapper.OrderMapper.findByUserId" column="id"/>
    </resultMap>

    <select id="findById" resultMap="BaseResultMap">
        SELECT * FROM users WHERE id = #{id}
    </select>

    <!-- 动态 SQL -->
    <select id="findByCondition" resultMap="BaseResultMap">
        SELECT * FROM users
        <where>
            <if test="userName != null and userName != ''">
                AND user_name LIKE CONCAT('%', #{userName}, '%')
            </if>
            <if test="status != null">
                AND status = #{status}
            </if>
        </where>
        ORDER BY create_time DESC
    </select>
</mapper>
```

#### `#{}` vs `${}`（高频考点）

```sql
-- #{} ：预编译占位符 ?，防止 SQL 注入，自动加引号
SELECT * FROM users WHERE id = #{id}
-- 实际执行：SELECT * FROM users WHERE id = ?

-- ${} ：字符串拼接，有 SQL 注入风险
SELECT * FROM ${tableName} WHERE id = #{id}
-- 仅用于动态表名、动态列名、ORDER BY 字段等无法预编译的场景
-- 使用时必须做白名单校验！
```

#### MyBatis-Plus

```java
// MyBatis-Plus 是 MyBatis 的增强工具，提供通用 CRUD

// Mapper 继承 BaseMapper，自带常用方法
public interface UserMapper extends BaseMapper<User> {
    // 无需写 XML，自动拥有：
    //   selectById, selectList, selectPage
    //   insert, updateById, deleteById
}

// Service 层
public interface UserService extends IService<User> { }
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService { }

// 条件构造器
List<User> list = userMapper.selectList(
    new LambdaQueryWrapper<User>()
        .eq(User::getStatus, 1)
        .like(User::getUserName, "张")
        .gt(User::getAge, 18)
        .orderByDesc(User::getCreateTime)
);

// 分页插件
Page<User> page = new Page<>(1, 10);  // 第 1 页，每页 10 条
Page<User> result = userMapper.selectPage(page, null);
```

#### MyBatis 插件机制

```
四大核心对象（可拦截）：
  Executor       → 执行器（执行 SQL、事务处理）
  StatementHandler → 处理 SQL 语句（预编译、参数设置）
  ParameterHandler → 处理参数
  ResultSetHandler → 处理结果集

常用插件：
  PageHelper      → 物理分页
  MyBatis-Plus 分页插件 → 物理分页
  自定义插件       → SQL 性能监控、数据脱敏、读写分离路由
```

### 5.4 JPA / Hibernate / Spring Data JPA

```
JPA（Java Persistence API）：Java 持久化规范/接口
Hibernate：JPA 的实现（最流行的实现）
Spring Data JPA：Spring 对 JPA 的进一步封装，声明式查询
```

```java
// 实体类
@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;

    @ManyToOne(fetch = FetchType.LAZY)  // 多对一，懒加载
    @JoinColumn(name = "dept_id")
    private Dept dept;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders;
}

// Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 方法命名即查询（Spring Data 自动实现）
    Optional<User> findByUserName(String userName);
    List<User> findByAgeGreaterThanAndStatus(int age, int status);
    Page<User> findByDeptId(Long deptId, Pageable pageable);

    // JPQL 查询
    @Query("SELECT u FROM User u WHERE u.userName LIKE %:keyword%")
    List<User> search(@Param("keyword") String keyword);

    // 原生 SQL
    @Query(value = "SELECT * FROM users WHERE age > ?1", nativeQuery = true)
    List<User> findByAgeGreaterThan(int age);

    // @Modifying 用于 UPDATE/DELETE，需配合 @Transactional
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") int status);
}

// 实体状态
// Transient  → 自由态（new 出来的，不在持久化上下文中）
// Persistent → 持久态（被 EntityManager 管理，改动自动同步到 DB）
// Detached   → 游离态（曾经持久化，但 Session 关闭了）
// Removed    → 删除态（标记为删除，事务提交时删除）

// 一级缓存：EntityManager 级别，同一个 Session 内相同查询只执行一次 SQL
// 二级缓存：SessionFactory 级别，跨 Session 共享
```

#### MyBatis vs JPA 对比

| 对比维度 | MyBatis | JPA/Hibernate |
|---------|---------|---------------|
| SQL 控制 | 开发者手写，完全可控 | 框架自动生成，复杂查询需 JPQL/HQL |
| 学习曲线 | 较低，会 SQL 就行 | 较高，需理解 ORM 概念 |
| 动态 SQL | XML 标签或 LambdaWrapper | Criteria API / QueryDSL（较复杂） |
| 性能优化 | 直接优化 SQL | 需理解 N+1 问题、FetchType 等 |
| 数据库移植 | 差（SQL 可能不兼容） | 好（方言自动适配） |
| 适用场景 | 复杂报表、遗留数据库、高定制 | 标准 CRUD、快速开发、微服务 |
| 自动 DDL | ❌ | ✅（自动建表），生产环境建议关闭 |

### 5.5 MySQL 核心知识点

#### 索引

```sql
-- 索引类型
-- 按数据结构：
--   B+Tree 索引（最常用，InnoDB 默认）
--   Hash 索引（等值查询快，范围查询不支持）
--   Full-Text 索引（全文搜索）

-- 按物理存储：
--   聚簇索引（Clustered Index）：数据跟索引放一起，一个表只有一个（主键索引）
--   非聚簇索引（Secondary Index）：索引叶子存主键值，需要回表查聚簇索引

-- 创建索引
CREATE INDEX idx_user_name ON users(user_name);
CREATE INDEX idx_name_age ON users(user_name, age);  -- 联合索引

-- 最左前缀原则：联合索引 (a, b, c)
-- WHERE a=1           → 用到索引
-- WHERE a=1 AND b=2   → 用到索引
-- WHERE b=2           → 用不到！
-- WHERE a=1 AND c=3   → 只用到 a，c 走不到
-- WHERE a=1 AND b>2 AND c=3 → 用到 a, b（范围查询后的列不生效）
```

#### 索引优化原则

```
1. 选择性高的列建索引（字段值分散度大）
2. WHERE、JOIN、ORDER BY 的列建索引
3. 避免在索引列上使用函数或运算（WHERE YEAR(create_time) = 2024 → 索引失效）
4. 避免 SELECT *，用覆盖索引避免回表
5. LIKE '%xxx' 前置模糊查询索引失效（LIKE 'xxx%' 走索引）
6. OR 条件两边都有索引才走索引
7. != / <> / NOT IN / IS NULL 可能导致索引失效
8. EXPLAIN 分析 SQL 执行计划：
   type 列：system > const > eq_ref > ref > range > index > ALL（从左到右越来越差）
   extra 列：Using index（覆盖索引，最好）/ Using filesort（文件排序，差）/ Using temporary（临时表，最差）
```

#### 事务 ACID & 隔离级别

```
ACID：
  Atomicity   原子性 → 要么全成功，要么全回滚（undo log 实现）
  Consistency 一致性 → 事务前后数据完整性约束不变
  Isolation   隔离性 → 并发事务互不干扰（MVCC + 锁实现）
  Durability  持久性 → 提交的数据永久保存（redo log 实现）

隔离级别（从低到高）：
  级别              | 脏读 | 不可重复读 | 幻读 | 实现方式
  READ UNCOMMITTED   |  ✅  |    ✅     |  ✅ | 无
  READ COMMITTED      |  ❌  |    ✅     |  ✅ | 每次读取生成 ReadView
  REPEATABLE READ     |  ❌  |    ❌     |  ✅*| 事务开始时生成 ReadView
  SERIALIZABLE        |  ❌  |    ❌     |  ❌ | 加锁

* InnoDB 的 REPEATABLE READ 通过 Next-Key Lock 在一定程度上解决了幻读
  MySQL 默认隔离级别是 REPEATABLE READ
```

#### MVCC（多版本并发控制）

```
InnoDB 每行数据有两个隐藏列：
  DB_TRX_ID  → 最近修改这行的事务 ID
  DB_ROLL_PTR → 回滚指针，指向 undo log 中的旧版本

ReadView 包含：
  - 创建 ReadView 时所有活跃事务 ID 列表
  - 最小活跃事务 ID
  - 下一个将要分配的事务 ID

可见性判断：通过对比行数据的 DB_TRX_ID 和 ReadView，判断该版本是否可见

这就是为什么 REPEATABLE READ 能避免不可重复读：
  快照读时，整个事务期间使用同一个 ReadView
```

#### 数据库三大范式

```
1NF：每列不可再分（原子性）
2NF：满足 1NF，且非主键列完全依赖于主键（消除部分依赖）
3NF：满足 2NF，且非主键列不依赖于其他非主键列（消除传递依赖）

实际开发中不一定要完全遵守，适当冗余提升查询效率（反范式）
```

### 5.6 分库分表

```
垂直拆分：
  垂直分库 → 按业务模块拆成不同的库（用户库、订单库、商品库）
  垂直分表 → 把一张宽表拆成多张（常用字段和非常用字段分开）

水平拆分：
  水平分表 → 同一张表按行拆分到多个表中（user_0, user_1, ...）
  水平分库分表 → 数据分布到不同库的多个表中

拆分算法：
  - 取模：id % N → 简单，扩容麻烦
  - 范围：按时间/ID范围 → 扩容方便，可能热点
  - 一致性 Hash → 扩容影响小

中间件：
  - ShardingSphere（Apache，JDBC 层代理）
  - MyCat（代理层）
  - Vitess（YouTube 开源的 MySQL 集群方案）
```

### 5.7 分布式 ID 生成

| 方案 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| UUID | 随机生成 128 位 | 简单、无中心 | 无序、索引性能差、占空间大 |
| 雪花算法(Snowflake) | 1bit + 41bit时间戳 + 10bit机器ID + 12bit序列号 | 有序、高性能、趋势递增 | 依赖机器时钟，时钟回拨出问题 |
| 数据库号段 | 一次取一批号段 [0,999] | 简单、不依赖时钟 | 有中心依赖 |
| Redis 自增 | INCR 命令 | 简单 | 需持久化 |
| Leaf（美团） | 雪花 + 号段双模式 | 可靠 | 需额外部署 |

---

## 六、缓存技术

### 6.1 缓存架构

```
客户端（浏览器缓存）
     ↓
CDN（内容分发网络，静态资源缓存）
     ↓
反向代理（Nginx 缓存）
     ↓
应用层缓存（本地：Caffeine/Ehcache；分布式：Redis）
     ↓
数据库（MySQL Query Cache 已废弃 → 8.0 移除）
```

### 6.2 Redis 核心知识

#### 五种基本数据类型

| 类型 | 底层编码 | 常用命令 | 典型场景 |
|------|---------|---------|---------|
| **String** | int/embstr/raw | SET GET SETEX INCR DECR | 缓存、计数器、分布式锁、Session |
| **Hash** | ziplist/hashtable | HSET HGET HGETALL HDEL | 用户信息、购物车、对象缓存 |
| **List** | quicklist | LPUSH RPUSH LPOP RPOP LRANGE | 消息队列、最新列表、时间线 |
| **Set** | intset/hashtable | SADD SREM SMEMBERS SINTER SUNION | 标签、好友关系、去重、交集 |
| **ZSet** | ziplist/skiplist | ZADD ZRANGE ZRANK ZREVRANKBYSCORE | 排行榜、优先级队列、延迟队列 |

#### Redis 进阶数据结构

```
HyperLogLog    → 基数统计（UV 统计），误差 0.81%，极省内存
Bitmap         → 位图，签到打卡、布隆过滤器的底层
GEO            → 地理位置，附近的人
Stream         → 5.0 新增，持久化消息队列，消费者组
```

#### 缓存三大问题

```
┌─────────────────┬──────────────────────┬──────────────────────────────┐
│ 问题             │ 现象                  │ 解决方案                      │
├─────────────────┼──────────────────────┼──────────────────────────────┤
│ 缓存穿透          │ 查不存在的 key，      │ 1. 布隆过滤器                  │
│ (Penetration)   │ 每次都打到 DB          │ 2. 缓存空值（短 TTL）          │
│                 │ 场景：恶意攻击          │                              │
├─────────────────┼──────────────────────┼──────────────────────────────┤
│ 缓存击穿          │ 热点 key 过期瞬间，    │ 1. 互斥锁（setnx）             │
│ (Breakdown)     │ 大量请求打到 DB        │ 2. 永不过期 + 异步更新          │
│                 │                       │ 3. 逻辑过期（不设 TTL，异步刷新）│
├─────────────────┼──────────────────────┼──────────────────────────────┤
│ 缓存雪崩          │ 大量 key 同时过期或    │ 1. TTL + 随机值                │
│ (Avalanche)     │ Redis 宕机            │ 2. 高可用（哨兵/集群）          │
│                 │                       │ 3. 本地缓存降级                │
│                 │                       │ 4. 限流熔断                    │
└─────────────────┴──────────────────────┴──────────────────────────────┘
```

#### 缓存淘汰策略

```
noeviction        → 不淘汰，内存满时写入报错
allkeys-lru       → 所有 key 中 LRU 淘汰（最常用）
volatile-lru      → 设置过期时间的 key 中 LRU 淘汰
allkeys-random    → 所有 key 中随机淘汰
volatile-random   → 有过期时间的 key 随机淘汰
volatile-ttl      → 有过期时间的 key 按 TTL 淘汰（即将过期的优先）
allkeys-lfu       → 所有 key 中 LFU 淘汰（Redis 4.0+）
volatile-lfu      → 有过期时间的 key 中 LFU 淘汰
```

#### 持久化

```
RDB（快照持久化）：
  定时把内存数据快照保存到 dump.rdb
  优点：文件紧凑、恢复快、对性能影响小
  缺点：可能丢失最后一次快照之后的数据
  触发方式：save 90 1 (90秒内1次修改) / bgsave（后台 fork 子进程）
  配置：save 900 1 / save 300 10 / save 60 10000

AOF（追加文件持久化）：
  记录所有写命令到 appendonly.aof
  appendfsync: always（每条命令） / everysec（每秒，推荐） / no（OS 控制）
  优点：数据安全性高
  缺点：文件大、恢复慢
  AOF 重写：压缩冗余命令，减小文件

混合持久化（Redis 4.0+，推荐）：
  AOF 文件中前半段是 RDB 格式的快照，后半段是增量 AOF 命令
  兼具 RDB 的恢复速度和 AOF 的数据安全性
```

#### 数据过期删除策略

```
惰性删除：访问 key 时才检查是否过期，过期则删除
定期删除：每 100ms 随机抽取一批 key 检查，过期删除

两者结合使用，Redis 使用惰性 + 定期删除
```

#### 内存淘汰流程（8 种策略 + 过期删除）

```
申请内存 → 有空闲？→ 是 → 分配
                   → 否 → 达到 maxmemory？→ 淘汰 → 继续
```

#### 高可用架构

```
主从复制（Replication）：
  一主多从，读写分离
  主写从读，异步复制，可能主从延迟

哨兵模式（Sentinel）：
  监控主节点
  主节点宕机时自动故障转移（主从切换）
  通知客户端新的主节点地址
  至少需要 3 个哨兵实例（避免脑裂，需要 quorum 投票）

Cluster 集群：
  Redis 3.0+ 支持，去中心化
  数据分片：16384 个 hash slot（CRC16(key) % 16384）
  每个节点负责一部分 slot
  Gossip 协议通信
  至少 3 主 3 从保证高可用
  不支持多 key 跨 slot 操作（可用 hash tag {key} 固定 slot）
```

#### 分布式锁

```java
// Redis 实现分布式锁的核心要点：
// 1. 互斥：SETNX + 过期时间
// 2. 防误删：value 用唯一标识（UUID/线程ID），释放时判断
// 3. 续期：watch dog 定时续期（Redisson 实现）
// 4. Lua 脚本保证原子性（加锁、释放）

// Redisson 分布式锁（推荐）
RLock lock = redissonClient.getLock("lock:order:" + orderId);
try {
    if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
        // 业务逻辑
    }
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}

// Redisson 看门狗机制：
// 如果未指定锁的过期时间，Redisson 默认给 30s
// 然后每 10s（过期时间/3）自动续期
```

### 6.3 本地缓存

```
Caffeine（推荐）：
  - 基于 W-TinyLFU 算法，综合 LRU + LFU 的优势
  - Window TinyLFU：一个窗口缓存 + 主缓存（SLRU + TinyLFU）
  - 高性能，接近理论最优命中率

Ehcache：
  - 老牌，功能全面
  - 支持堆内、堆外、磁盘三级缓存
  - 支持 JCache (JSR-107) 标准

Guava Cache：
  - Google 出品，简单易用
  - 功能较基础，已被 Caffeine 超越（Caffeine 作者也是 Guava Cache 作者）
```

---

## 七、消息队列

### 7.1 MQ 的作用

```
削峰填谷：突发流量缓存起来，慢慢消费
异步解耦：服务间通过 MQ 通信，不直接调用
数据分发：一个消息多个系统消费
最终一致性：分布式事务的基础
```

### 7.2 主流 MQ 对比

| 对比维度 | RabbitMQ | Kafka | RocketMQ | ActiveMQ | Pulsar |
|---------|----------|-------|----------|----------|--------|
| 语言 | Erlang | Java/Scala | Java | Java | Java |
| 吞吐量 | 万级 | 百万级 | 十万级 | 万级 | 百万级 |
| 延迟 | 微秒级 | 毫秒级 | 毫秒级 | 毫秒级 | 毫秒级 |
| 可靠性 | 高（主从） | 高（副本） | 高（主从同步） | 中 | 高 |
| 事务消息 | ❌ | ❌ | ✅ | ✅ | ❌ |
| 延迟消息 | 插件 | ❌ | ✅ | ✅ | ❌ |
| 顺序消息 | ✅ | ✅（分区内） | ✅ | ✅ | ✅ |
| 社区活跃度 | 高 | 高 | 中（阿里主导） | 低 | 增长中 |
| 适用场景 | 业务解耦 | 大数据/日志 | 电商金融 | 传统企业 | 云原生 |

### 7.3 Kafka 核心概念

```
┌──────────────────────────────────────────────────┐
│ Broker     → Kafka 服务器节点                      │
│ Topic      → 消息主题（逻辑分类）                   │
│ Partition  → 分区（物理存储，顺序写，可并行消费）     │
│ Producer   → 生产者（发送消息）                     │
│ Consumer   → 消费者（消费消息）                     │
│ Consumer Group → 消费者组（组内竞争，组间广播）       │
│ Offset     → 消息在分区中的偏移量（消费者记录位置）   │
│ Replica    → 副本（Leader + Follower）             │
│ ISR        → In-Sync Replicas，与 Leader 保持同步的副本集合│
│ Segment    → 分区物理存储单元（日志分段）            │
│ Zookeeper/KRaft → 集群管理（KRaft 3.3+ 替代 ZK）   │
└──────────────────────────────────────────────────┘

高吞吐原因：
  1. 顺序写磁盘（比随机写快）
  2. 零拷贝技术（sendfile）
  3. 批量发送、批量压缩
  4. 分区并行
  5. Page Cache 缓存
```

### 7.4 RabbitMQ 核心概念

```
┌──────────────────────────────────────────────────┐
│ Connection → TCP 连接                              │
│ Channel    → 虚拟连接（复用 TCP，节省资源）          │
│ Exchange   → 交换机（接收消息，路由到队列）          │
│             Direct：精确匹配 routing key             │
│             Topic：通配符匹配（* 一个词，# 多个词）   │
│             Fanout：广播到所有绑定队列               │
│             Headers：按消息头匹配                    │
│ Queue      → 消息队列                               │
│ Binding    → 交换机和队列的绑定关系                  │
│ Virtual Host → 虚拟主机（多租户隔离）                │
└──────────────────────────────────────────────────┘

消息确认机制：
  Publisher Confirm   → 生产者确认消息到达 Broker
  Consumer ACK        → 消费者确认消息处理完毕
    autoAck=false     → 手动确认（推荐）
    重试 + 死信队列    → 失败 N 次后进入死信队列（DLX），人工处理

消息流转：
  Producer → Exchange → [Binding Rule] → Queue → Consumer
  生产者可指定 routing key 和消息属性（TTL、持久化等）
```

### 7.5 消息可靠性保障

```
生产者→Broker：
  - 同步发送/异步发送 + 回调
  - 发送重试
  - Publisher Confirm

Broker 存储：
  - 消息持久化（磁盘）
  - 副本机制

Broker→消费者：
  - Consumer ACK（处理成功才确认）
  - 消费重试 → 死信队列

防重复消费（幂等性）：
  - 数据库唯一约束
  - Redis setnx
  - 消息 ID + 消费记录表
```

### 7.6 常见问题

```
消息丢失 → 生产者确认 + 持久化 + 消费确认（三步布防）
消息重复 → 消费端幂等处理
消息积压 → 紧急扩容消费者 / 降级非核心 / 批量消费
消息顺序 → 同一个 key 发到同一个分区/队列（Kafka: 分区内有序；RabbitMQ: 单队列单消费者有序）
```

---

## 八、微服务与分布式系统

### 8.1 分布式理论基础

#### CAP 定理

```
C（Consistency）一致性 → 所有节点同一时间看到相同数据
A（Availability）可用性 → 每个请求都能获得非错误的响应
P（Partition Tolerance）分区容错 → 网络分区时系统仍能正常工作

理论：三者最多同时满足两个。
实际：分区容错必须保证（网络不可靠），在 C 和 A 之间权衡。
      CP 系统：ZooKeeper、Consul（优先保证一致性）
      AP 系统：Eureka、Nacos（AP 模式）（优先保证可用性）
```

#### BASE 理论

```
Basically Available     基本可用（允许部分服务不可用或降级）
Soft state              软状态（允许系统中存在中间状态）
Eventually consistent   最终一致性（不要求实时一致，但最终会达到一致）

BASE 是 CAP 中 AP 的延伸，是分布式系统的实践指导
```

#### 一致性协议

```
2PC（两阶段提交）：
  阶段一：协调者问所有参与者是否准备好提交
  阶段二：全部准备好 → 提交；有任何一个不 ok → 回滚
  问题：同步阻塞、单点故障、数据不一致风险

3PC（三阶段提交）：
  在 2PC 基础上加了预提交阶段 + 超时机制，减少阻塞

TCC（Try-Confirm-Cancel）：
  Try      → 预留资源（检查、冻结）
  Confirm  → 确认执行（使用预留资源，不会失败）
  Cancel   → 取消执行（释放预留资源）
  适用：资金转账、库存扣减等强一致性场景

Saga 模式：
  长事务拆为多个短事务，每个短事务有对应的补偿操作
  编排式：Saga 协调器顺序调用各服务，失败则反向调用补偿
  控制式：各服务通过事件驱动，自己决定是继续还是补偿
```

### 8.2 注册中心（服务发现）

| 注册中心 | 语言 | CAP | 特点 |
|---------|------|-----|------|
| **Nacos** | Java | AP/CP 可切换 | 阿里开源，注册+配置二合一，生产首选 |
| **Consul** | Go | CP | 健康检查强、支持多数据中心 |
| **Eureka** | Java | AP | Netflix 出品，2.x 已停更，不再推荐 |
| **ZooKeeper** | Java | CP | 老牌分布式协调，但作为注册中心较重 |
| **Etcd** | Go | CP | K8s 底层使用，适合云原生 |

```yaml
# Nacos 使用
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        namespace: dev       # 命名空间（环境隔离）
        group: DEFAULT_GROUP # 分组
```

### 8.3 配置中心

```
为什么需要配置中心？
  - 配置集中管理，不用每个服务改配置文件
  - 配置动态刷新，不重启服务生效
  - 权限控制和审计

主流方案：
  Nacos   → 阿里开源，注册+配置一体
  Apollo  → 携程开源，功能最细（多环境、灰度发布、权限管理）
  Spring Cloud Config → 轻量，基于 Git 存储
  Consul KV → 简单场景够用
```

### 8.4 服务调用

```java
// 1. RestTemplate（同步 HTTP 调用）
RestTemplate restTemplate = new RestTemplate();
String result = restTemplate.getForObject(
    "http://user-service/api/users/" + userId, String.class);

// 2. OpenFeign（声明式 HTTP 客户端，推荐）
@FeignClient(name = "user-service", fallback = UserFeignFallback.class)
public interface UserFeignClient {
    @GetMapping("/api/users/{id}")
    Result<User> getUserById(@PathVariable("id") Long id);
}
// 使用：userFeignClient.getUserById(1L);
// 集成：负载均衡（Spring Cloud LoadBalancer）、熔断（Sentinel）

// 3. Dubbo（高性能 RPC 框架）
// 传输层：Netty，序列化：Hessian2 / Protobuf
// 适用：大规模微服务、服务治理要求高的场景
@Service(version = "1.0.0")
public class UserServiceImpl implements UserService { }
@DubboReference(version = "1.0.0")
private UserService userService;

// 4. gRPC（Google RPC，HTTP/2 + Protobuf）
// 跨语言，性能好，强类型
// 适合：多语言异构系统、高性能内部通信
```

### 8.5 服务网关

```yaml
# Spring Cloud Gateway（响应式网关，基于 WebFlux + Netty）
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service  # 负载均衡
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=0
            - name: RequestRateLimiter  # 限流
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20

# 网关作用：
#   路由转发、负载均衡
#   统一鉴权（在网关校验 Token，不用每个服务校验）
#   限流、熔断
#   日志、监控
#   跨域处理
#   灰度发布（按 Header/Cookie 路由到不同版本）
```

### 8.6 熔断、限流、降级

```
熔断（Circuit Breaker）：
  当被调用服务失败率达到阈值 → 熔断器打开 → 快速失败 → 不让请求打到故障服务
  半开状态：一段时间后尝试放少量请求，成功则关闭熔断器，失败则继续打开

限流（Rate Limiting）：
  控制单位时间内的请求量，保护系统不被冲垮
  算法：计数器、滑动窗口、漏桶、令牌桶

降级（Fallback）：
  服务出现故障时，返回兜底数据或执行备用逻辑
  确保核心服务可用，牺牲非核心功能
```

```java
// Sentinel 使用示例
@SentinelResource(
    value = "getUser",
    blockHandler = "handleBlock",    // 限流/熔断降级方法
    fallback = "handleFallback"      // 异常降级方法
)
public User getUser(Long id) {
    return userService.getUser(id);
}

// Sentinel 规则
// QPS 超过 10 直接限流
// 响应时间超过 500ms 且比例超过 50% 触发熔断
// 异常比例超过 50% 触发熔断
```

### 8.7 分布式事务

```java
// Seata（阿里开源分布式事务解决方案）

// AT 模式（推荐，对业务无侵入）：
//   第一阶段：各分支事务执行并提交，Seata 记录 undo log
//   第二阶段：全部成功 → 异步删除 undo log
//            有失败 → 根据 undo log 回滚

// TCC 模式：
//   业务需要实现 Try/Confirm/Cancel 三个方法

// Saga 模式：
//   适合长事务、老系统接入（无全局锁）

@GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
public void createOrder(OrderDTO orderDTO) {
    orderService.create(orderDTO);       // 本地事务
    storageService.deduct(orderDTO);     // 远程调用，扣库存
    accountService.debit(orderDTO);      // 远程调用，扣余额
}
```

### 8.8 分布式链路追踪

```
为什么需要？微服务调用链长，定位问题需要一个请求的全链路视图

SkyWalking：
  - Apache 顶级项目，Java Agent 无侵入
  - 支持指标、追踪、日志

Sleuth + Zipkin：
  - Sleuth 给每个请求加 TraceId/SpanId
  - Zipkin 收集并可视化调用链

Jaeger：
  - Uber 开源，CNCF 毕业项目
```

---

## 九、认证与授权

### 9.1 概念区分

```
认证（Authentication）：你是谁？
  验证用户身份 → 登录

授权（Authorization）：你能干什么？
  检查用户权限 → 是否有权限访问某个 API

凭证（Credentials）：
  认证成功后获得的凭证 → SessionId / Token
```

### 9.2 OAuth 2.0

```
┌────────────────────────────────────────────────────────┐
│ OAuth 2.0 四种授权模式                                   │
├────────────────────────────────────────────────────────┤
│ 授权码模式（Authorization Code）                         │
│   最安全最完整，需要客户端密钥                            │
│   流程：重定向到授权服务器 → 用户授权 → 获取授权码        │
│         → 后端用授权码 + 密钥换 Token                    │
│   适用：有后端的 Web 应用                                │
├────────────────────────────────────────────────────────┤
│ 隐式模式（Implicit）                                     │
│   简化版，直接返回 Token（无授权码步骤）                  │
│   已不推荐（安全考虑），被 PKCE 替代                      │
├────────────────────────────────────────────────────────┤
│ 密码模式（Resource Owner Password Credentials）          │
│   用户直接把用户名密码给客户端                            │
│   适用：自家 App、高度信任的应用                          │
│   已不推荐                                              │
├────────────────────────────────────────────────────────┤
│ 客户端凭证模式（Client Credentials）                      │
│   服务间调用、机器对机器                                  │
│   适用：微服务间认证、CLI 工具                            │
└────────────────────────────────────────────────────────┘

PKCE（Proof Key for Code Exchange）：
  授权码模式的增强版，适用于没有后端密钥的客户端（SPA、移动 App）
  增加 code_challenge 和 code_verifier 防止授权码截获攻击
```

### 9.3 JWT（JSON Web Token）

```
结构：header.payload.signature

Header: {"alg": "HS256", "typ": "JWT"}
Payload: {
  "sub": "123",           // 主体（用户ID）
  "name": "张三",
  "iat": 1234567890,      // 签发时间
  "exp": 1234657890,      // 过期时间
  "iss": "my-app",        // 签发者
  "aud": "my-client"      // 接收方
}
Signature: HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)

优点：
  - 无状态，服务端不存 Session
  - 自带用户信息，减少查库
  - 跨域友好

缺点：
  - 不能主动失效（除非用黑名单或短 TTL）
  - Payload 只 Base64 编码，加密用 JWE
  - 包体积较大，Header 中携带
```

### 9.4 RBAC（Role-Based Access Control）

```
用户（User） → 角色（Role） → 权限（Permission）

权限模型：
  User ←→ Role ←→ Permission

举例：
  张三 → admin → user:create, user:update, user:delete
  李四 → editor → user:create, user:update
  王五 → viewer → user:read

Spring Security 集成 RBAC：
  - hasRole('ADMIN')        基于角色
  - hasAuthority('user:create')  基于权限
  - @PreAuthorize("hasRole('ADMIN')")  方法级别权限控制
```

---

## 十、测试体系

### 10.1 测试金字塔

```
          ┌──────────┐
          │  E2E 测试  │  少量，端到端，模拟真实用户操作，最慢
          ├──────────┤
          │  集成测试  │  适量，多个组件协作测试，中等速度
          ├──────────┤
          │  单元测试  │  大量，测试单个类/方法，最快
          └──────────┘
```

### 10.2 单元测试（JUnit 5 + Mockito）

```java
// JUnit 5 核心注解
@Test               // 标记测试方法
@BeforeEach         // 每个测试前执行
@AfterEach          // 每个测试后执行
@BeforeAll          // 所有测试前执行（需 static）
@AfterAll           // 所有测试后执行（需 static）
@DisplayName("测试名称") // 给测试取名
@Disabled           // 临时禁用
@ParameterizedTest  // 参数化测试
@RepeatedTest(3)    // 重复测试 N 次
@Nested             // 嵌套测试类
@Tag("unit")        // 标签分类

// 断言
assertEquals(expected, actual)
assertNotNull(object)
assertThrows(Exception.class, () -> { ... })
assertTrue(condition)
assertTimeout(Duration.ofSeconds(1), () -> { ... })

// Mockito 核心用法
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock                    // 创建 Mock 对象
    private UserMapper userMapper;

    @InjectMocks             // 注入 Mock 到被测试对象
    private UserServiceImpl userService;

    @Test
    void testGetUser() {
        // given（准备数据）
        User mockUser = new User(1L, "张三");
        when(userMapper.findById(1L)).thenReturn(mockUser);

        // when（执行）
        User result = userService.getUser(1L);

        // then（验证）
        assertEquals("张三", result.getName());
        verify(userMapper, times(1)).findById(1L);
    }

    @Test
    void testException() {
        when(userMapper.findById(-1L)).thenThrow(new RuntimeException());
        assertThrows(RuntimeException.class, () -> userService.getUser(-1L));
    }
}
```

### 10.3 集成测试

```java
// Spring Boot Test
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Transactional  // 测试后自动回滚
    void testCreateUser() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"张三\"}"))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.data.name").value("张三"));
    }
}

// @DataJpaTest    → 只加载 JPA 相关（slice test，快）
// @WebMvcTest     → 只加载 MVC 相关
// @RestClientTest → 只加载 RestClient 相关
// @JsonTest       → 只加载 JSON 序列化相关
```

---

## 十一、DevOps 与部署

### 11.1 Docker

```dockerfile
# Dockerfile 示例（多阶段构建）
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline   # 缓存依赖
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-alpine  # JRE 即可，体积小
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```yaml
# docker-compose.yml（本地多容器编排）
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: mydb
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/mydb
      SPRING_REDIS_HOST: redis

volumes:
  mysql-data:
```

### 11.2 Kubernetes（K8s）

```
核心概念：
┌───────────────────────────────────────────────────────┐
│ Pod          → 最小部署单元（1 个或多个容器）            │
│ Service      → 稳定的网络入口（ClusterIP/NodePort/LoadBalancer）│
│ Deployment   → 声明式 Pod 管理（滚动更新、回滚）         │
│ ConfigMap    → 配置存储                                 │
│ Secret       → 敏感数据存储（base64 编码）               │
│ Ingress      → 外部 HTTP(S) 路由到 Service              │
│ Namespace    → 资源逻辑隔离                              │
│ StatefulSet  → 有状态应用（数据库等）                    │
│ DaemonSet    → 每个节点运行一个 Pod（日志收集等）         │
│ HPA          → 水平自动扩缩容（基于 CPU/内存指标）        │
│ Helm         → K8s 包管理器                             │
└───────────────────────────────────────────────────────┘
```

### 11.3 CI/CD

```
CI（持续集成）：
  代码 Push → 自动构建 → 自动测试 → 代码检查 → 反馈结果
  工具：Jenkins、GitHub Actions、GitLab CI、阿里云云效

CD（持续交付/部署）：
  CI 通过 → 构建镜像 → 推送镜像仓库 → 部署到环境 → 健康检查
  蓝绿部署：新旧环境并存，切换流量
  滚动更新：逐个替换 Pod
  灰度/金丝雀发布：一小部分流量打到新版本
```

### 11.4 Nginx

```nginx
# Nginx 核心配置
server {
    listen 80;
    server_name example.com;

    # 静态资源
    location /static/ {
        root /var/www;
        expires 30d;
    }

    # 反向代理到后端
    location /api/ {
        proxy_pass http://backend-cluster/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 60s;
    }

    # 负载均衡
    upstream backend-cluster {
        # 策略：轮询(默认)、weight(权重)、ip_hash(IP哈希)、least_conn(最少连接)
        server 192.168.1.10:8080 weight=5;
        server 192.168.1.11:8080 weight=3;
        server 192.168.1.12:8080 backup;  # 备用节点
    }

    # HTTPS
    # listen 443 ssl;
    # ssl_certificate /path/to/cert.pem;
    # ssl_certificate_key /path/to/key.pem;
}
```

---

## 十二、性能优化与 JVM

### 12.1 JVM 内存模型

```
┌──────────────────────────────────────────────────────────┐
│                    JVM 运行时数据区                         │
├──────────┬───────────────────────────────────────────────┤
│          │  堆（Heap）→ 所有线程共享，GC 主要区域            │
│          │    ├── 新生代（Young Generation）→ 1/3 堆        │
│          │    │   ├── Eden（伊甸园）→ 80% 新生代            │
│          │    │   ├── Survivor0（S0）→ 10% 新生代           │
│          │    │   └── Survivor1（S1）→ 10% 新生代           │
│  线程共享 │    ├── 老年代（Old Generation）→ 2/3 堆          │
│          │    └── 元空间（Metaspace）→ 方法区（类信息、常量池）│
│          │         JDK 8+ 从 PermGen 移到本地内存           │
├──────────┼───────────────────────────────────────────────┤
│          │  程序计数器（PC Register）                       │
│  线程私有 │  虚拟机栈（VM Stack）→ 栈帧（局部变量表/操作数栈）│
│          │  本地方法栈（Native Method Stack）              │
└──────────┴───────────────────────────────────────────────┘
```

### 12.2 对象从创建到回收

```
1. new → 尝试在栈上分配（逃逸分析 → 没逃逸 → 栈上分配，随栈帧销毁回收）
2. 栈上放不下 → TLAB (Thread Local Allocation Buffer) 分配（Eden 区，线程私有，无锁）
3. TLAB 用完 → Eden 区同步分配（CAS）
4. Minor GC（Young GC）：
   Eden 满了 → 标记 Eden + Survivor → 存活对象复制到另一个 Survivor
   → 年龄 +1 → 年龄超过阈值(-XX:MaxTenuringThreshold=15) → 晋升老年代
5. Major GC / Full GC（Old GC）：
   老年代满了 → 标记-清除/标记-整理
6. 大对象直接进入老年代（-XX:PretenureSizeThreshold）

GC Roots：哪些对象是"活着"的？
  - 虚拟机栈中引用的对象
  - 静态变量引用的对象
  - 常量池引用的对象
  - JNI 引用的对象
  - 被同步锁持有的对象
```

### 12.3 垃圾回收器

| 回收器 | 算法 | 特点 | 适用场景 |
|--------|------|------|---------|
| **Serial** | 标记-复制 | 单线程 GC，停顿长 | 客户端、小内存 |
| **Parallel** | 标记-复制 | 多线程 GC，吞吐量优先 | 后台计算 |
| **CMS** | 标记-清除 | 最短停顿，并发，CPU 敏感 | Web 应用（JDK 14 已移除） |
| **G1** | 标记-复制+整理 | 低停顿，分 Region，可预测 | JDK 9+ 默认，4G+ 堆 |
| **ZGC** | 标记-复制（染色指针） | 亚毫秒停顿，超大堆（TB级） | JDK 11 实验，17+ 成熟 |
| **Shenandoah** | 标记-复制 | 低停顿，与 G1 类似 | Red Hat 维护 |

```
G1 核心思想：
  - 不再分新生代/老年代的固定物理区域，而是把堆分为大小相等的 Region
  - 新生代、老年代、大对象区都是 Region 的逻辑集合
  - 优先回收价值最大的 Region（Garbage First）
  - Mixed GC：同时回收年轻代和部分老年代
  - 可设置预期停顿时间：-XX:MaxGCPauseMillis=200
```

### 12.4 常见 OOM 及排查

| 异常 | 原因 | 排查方向 |
|------|------|---------|
| **java.lang.OutOfMemoryError: Java heap space** | 堆内存不足 | 堆设置太小 / 内存泄漏 / 数据量过大 |
| **java.lang.OutOfMemoryError: Metaspace** | 类元数据区满 | 动态生成太多类（如 CGLIB、Groovy） |
| **java.lang.OutOfMemoryError: GC overhead limit exceeded** | 98% 时间 GC 但回收不到 2% 内存 | 堆太小 / 内存泄漏 |
| **java.lang.StackOverflowError** | 栈溢出 | 递归太深 / 循环依赖 |

#### 排查工具与命令

```bash
# JVM 诊断命令
jps           # 查看 Java 进程
jstat -gc <pid> 1000 10  # 每秒查看 GC 情况（共 10 次）
jmap -heap <pid>     # 堆内存概况
jmap -histo <pid>    # 堆中对象统计
jmap -dump:format=b,file=heap.hprof <pid>  # 堆转储（生产慎用，会 STW）
jstack <pid>         # 线程栈（排查死锁、高 CPU）
jcmd <pid> VM.flags  # JVM 启动参数
jinfo <pid>          # JVM 配置信息

# JVM 参数
-Xms2g -Xmx2g                     # 初始/最大堆内存（建议相同，避免动态伸缩）
-Xss256k                          # 线程栈大小
-XX:+PrintGCDetails               # 打印 GC 详情
-XX:+HeapDumpOnOutOfMemoryError   # OOM 时自动 dump
-XX:HeapDumpPath=/tmp/heap.hprof  # dump 路径
-XX:MaxMetaspaceSize=256m         # 最大元空间
-XX:+UseG1GC                      # 使用 G1 回收器
-XX:MaxGCPauseMillis=200          # G1 最大停顿目标
```

### 12.5 性能优化方向

```
1. 代码层面：
   - 避免在循环中创建对象
   - 合理使用 String/StringBuilder
   - 集合初始化时指定初始容量
   - key 对象重写 hashCode() 和 equals()
   - 避免在 try-catch 块中写大量代码
   - 使用线程池而非手动创建线程

2. 数据库层面：
   - SQL 优化 → EXPLAIN 分析，加合适的索引
   - 连接池调优 → 合适的连接数和超时时间
   - 读写分离 → 主库写从库读
   - 分库分表 → 数据量大到单库单表扛不住时

3. 架构层面：
   - 缓存 → 本地缓存（Caffeine）+ 分布式缓存（Redis）
   - 异步 → MQ 解耦，非关键路径异步化
   - 限流 → 保护系统，防止雪崩
   - 降级 → 核心路径保底

4. JVM 层面：
   - 选合适的 GC 回收器（Web 应用推荐 G1/ZGC）
   - 合理设置堆大小（不要太小也不要太大，建议不超过 32G，否则压缩指针失效）
   - 避免大对象频繁进出堆
```

---

## 十三、设计模式与架构

### 13.1 常用设计模式

```
创建型：
  ├── 单例（Singleton）→ Spring Bean 默认单例
  ├── 工厂方法（Factory Method）→ Spring BeanFactory
  ├── 抽象工厂（Abstract Factory）
  ├── 建造者（Builder）→ Lombok @Builder、StringBuilder
  └── 原型（Prototype）→ Object.clone()

结构型：
  ├── 代理（Proxy）→ Spring AOP 动态代理（JDK 动态代理 / CGLIB）
  ├── 适配器（Adapter）→ Spring MVC HandlerAdapter
  ├── 装饰器（Decorator）→ Java IO 流（BufferedReader 包装 Reader）
  ├── 外观（Facade）→ 对外提供统一接口
  └── 享元（Flyweight）→ Integer 缓存池、数据库连接池

行为型：
  ├── 观察者（Observer）→ Spring Event、MQ 发布订阅
  ├── 模板方法（Template Method）→ JdbcTemplate、RestTemplate
  ├── 策略（Strategy）→ 同接口不同实现，运行时选择
  ├── 责任链（Chain of Responsibility）→ Filter Chain、Interceptor Chain
  ├── 命令（Command）→ Runnable、Callable
  └── 状态（State）→ 订单状态流转
```

### 13.2 JDK 动态代理 vs CGLIB

```
JDK 动态代理：
  - 基于接口
  - 通过 Proxy.newProxyInstance() + InvocationHandler 生成代理
  - 被代理的类必须实现至少一个接口

CGLIB 动态代理：
  - 基于继承
  - 通过生成目标类的子类来实现代理
  - 不能代理 final 类和方法

Spring Boot 2.x 以前默认：接口用 JDK，无接口用 CGLIB
Spring Boot 2.x 以后默认：统一使用 CGLIB
可通过 spring.aop.proxy-target-class=false 改回 JDK
```

### 13.3 架构演进

```
单体架构 → SOA（面向服务）→ 微服务 → 服务网格（Service Mesh）→ Serverless

DDD（领域驱动设计）：
  战略设计：
    - 限界上下文（Bounded Context）→ 一个微服务的边界
    - 通用语言（Ubiquitous Language）→ 团队统一术语

  战术设计：
    - 实体（Entity）→ 有唯一标识的领域对象
    - 值对象（Value Object）→ 无唯一标识，不可变
    - 聚合（Aggregate）→ 一组相关对象的集合，通过聚合根访问
    - 领域事件（Domain Event）→ 领域中发生的重要事件
    - 仓储（Repository）→ 聚合的持久化接口
```

### 13.4 贫血模型 vs 充血模型

```
贫血模型（传统）：
  Entity 只有属性 + getter/setter，没有业务逻辑
  所有业务逻辑在 Service 层
  → 适合简单 CRUD，快速开发

充血模型（DDD 推荐）：
  Entity 包含属性和行为方法
  业务逻辑内聚在领域对象中
  Service 层变薄，只负责编排
  → 适合复杂业务，可维护性好
```

---

## 十四、项目工程规范

### 14.1 标准项目结构

```
src/
├── main/
│   ├── java/com/example/project/
│   │   ├── ProjectApplication.java       ← 启动类
│   │   │
│   │   ├── controller/                   ← 表现层：接收请求、参数校验、调用 Service、返回响应
│   │   │   └── UserController.java
│   │   │
│   │   ├── service/                      ← 业务逻辑层（接口 + 实现）
│   │   │   └── impl/
│   │   │       └── UserServiceImpl.java
│   │   │
│   │   ├── mapper/ (或 dao/ 或 repository/) ← 数据访问层
│   │   │   └── UserMapper.java
│   │   │
│   │   ├── model/ (分层清晰的进一步拆分)：
│   │   │   ├── entity/                   ← 数据库实体（PO：Persistent Object）
│   │   │   ├── dto/                      ← 数据传输对象（接收/发送）
│   │   │   ├── vo/                       ← 视图对象（返回给前端）
│   │   │   └── query/                    ← 查询对象（封装查询条件）
│   │   │
│   │   ├── config/                       ← 配置类（@Configuration）
│   │   │   ├── WebMvcConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── RedisConfig.java
│   │   │
│   │   ├── common/                       ← 公共/基础设施
│   │   │   ├── Result.java               ← 统一响应体
│   │   │   ├── GlobalExceptionHandler.java ← 全局异常处理
│   │   │   ├── BaseController.java       ← 控制器基类
│   │   │   └── annotation/               ← 自定义注解
│   │   │
│   │   ├── interceptor/                  ← 拦截器
│   │   ├── filter/                       ← 过滤器
│   │   ├── utils/                        ← 工具类
│   │   ├── enums/                        ← 枚举类
│   │   ├── constant/                     ← 常量类
│   │   └── exception/                    ← 自定义异常
│   │
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
│       ├── mapperxml/                    ← MyBatis XML（推荐跟接口放一起，或单独目录）
│       ├── db/migration/                 ← Flyway/Liquibase 数据库迁移脚本
│       └── logback-spring.xml            ← 日志配置
│
└── test/
    └── java/com/example/project/
        ├── controller/                   ← Controller 层测试
        ├── service/                      ← Service 层测试
        └── mapper/                       ← Mapper 层测试
```

### 14.2 对象转换规范

```
PO (Persistent Object)    → 数据库实体，与表字段一一对应
DTO (Data Transfer Object) → 各层数据传输，不含业务逻辑
VO (View Object)           → 返回给前端展示的数据
BO (Business Object)       → 包含业务逻辑的对象
Query / Request             → 前端->后端 请求参数对象
```

### 14.3 统一响应体

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private int code;       // 状态码
    private String message; // 提示信息
    private T data;         // 数据
    private long timestamp; // 时间戳

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data, System.currentTimeMillis());
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }
}
```

### 14.4 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusiness(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        return Result.error(400, message);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleUnknown(Exception e) {
        log.error("未知异常", e);
        return Result.error(500, "服务器内部错误");
    }
}
```

### 14.5 常用工具库

| 工具库 | 用途 |
|--------|------|
| **Lombok** | @Data @Builder @Slf4j 等注解，减少样板代码 |
| **MapStruct** | 编译期生成对象映射代码，无反射，高性能（PO ↔ DTO ↔ VO） |
| **Hutool** | 国产 Java 工具集，文件/加密/HTTP/日期等一站式 |
| **Guava** | Google 工具库，集合/缓存/字符串/并发 |
| **Apache Commons** | 老牌工具库（Commons Lang/IO/Collections） |
| **Fastjson/Jackson/Gson** | JSON 序列化（Spring Boot 默认 Jackson） |
| **EasyExcel** | 阿里开源，Excel 读写，解决 POI OOM 问题 |

---

## 十五、推荐学习路线

```
第一阶段：Java 基础（2-4 周）
├── Java 语法：数据类型、集合、异常、IO
├── 面向对象：封装继承多态、abstract vs interface
├── Java 8+ 特性：Lambda、Stream、Optional、新的日期API
├── 并发基础：线程创建、synchronized、线程池
└── SQL 基础 + MySQL 增删改查

第二阶段：Web 开发入门（2-4 周）
├── HTTP 协议、Cookie/Session/Token
├── Servlet/Tomcat（理解即可）
├── Spring Boot：自动配置、配置文件、Actuator
├── MyBatis / MyBatis-Plus：CRUD、动态SQL、分页
├── 写一个完整的 CRUD 项目
└── 单元测试 JUnit 5 + Mockito

第三阶段：进阶提升（4-6 周）
├── Spring 核心：IoC、AOP、事务
├── Spring Security + JWT
├── Redis：5 种数据类型、缓存策略、分布式锁
├── Maven/Gradle 深入：多模块、依赖管理、插件
├── Docker：Dockerfile、docker-compose
└── Git 工作流

第四阶段：微服务（4-8 周）
├── Spring Cloud Alibaba：Nacos + Sentinel + Gateway + OpenFeign
├── 消息队列：RocketMQ/RabbitMQ
├── 分布式事务：Seata
├── 链路追踪：SkyWalking + Sleuth
├── 分库分表：ShardingSphere
└── CI/CD：Jenkins/GitHub Actions

第五阶段：高级（持续）
├── JVM 调优：GC 选型、内存参数、dump 分析
├── MySQL 优化：索引优化、SQL 优化、主从复制
├── 设计模式：GoF 23 + DDD
├── 系统设计：高并发、高可用、秒杀系统
├── 源码阅读：Spring 核心、MyBatis 核心
└── Cloud Native：K8s、Istio、Prometheus
```

---

## 十六、高频面试知识点

### Spring 相关

| 问题 | 核心要点 |
|------|---------|
| Spring Boot 自动配置原理？ | @EnableAutoConfiguration → AutoConfigurationImportSelector → 读取 spring.factories/AutoConfiguration.imports → @ConditionalOnXxx 条件注解过滤 → 加载匹配的配置类 |
| Bean 生命周期？ | 实例化 → 属性注入 → Aware 回调 → BeanPostProcessor.before → @PostConstruct → InitializingBean → BeanPostProcessor.after → Bean 就绪 → @PreDestroy → DisposableBean → 销毁 |
| Spring 事务失效场景？ | 方法非 public、同类调用不经过代理、异常被 catch 没抛出、数据库引擎不支持事务（MyISAM）、@Transactional 注解在接口/抽象类上 |
| Spring 如何解决循环依赖？ | 三级缓存：singletonObjects(成品) → earlySingletonObjects(半成品) → singletonFactories(ObjectFactory) |
| @Autowired vs @Resource？ | @Autowired 按类型注入，@Resource 按名称注入；@Autowired 需配合 @Qualifier 指定名称 |
| BeanFactory vs ApplicationContext？ | BeanFactory 是底层容器(懒加载)，ApplicationContext 是高级容器(启动时初始化所有单例 Bean，支持国际化、事件发布等) |

### Redis 相关

| 问题 | 核心要点 |
|------|---------|
| Redis 快的原因？ | 基于内存操作 + 单线程模型(省去上下文切换和锁竞争) + IO 多路复用(epoll) + 高效数据结构 |
| 缓存穿透/击穿/雪崩？ | 穿透=查不存在的数据→布隆过滤器/缓存空值；击穿=热点Key过期→互斥锁/逻辑过期；雪崩=大量Key同时过期→随机TTL/多级缓存/限流 |
| Redis 过期删除+淘汰？ | 惰性删除(访问时检查) + 定期删除(定时抽样)；内存满后按 maxmemory-policy 淘汰 |
| 分布式锁要点？ | 互斥 + 防死锁(设过期) + 防误删(value唯一标识+Lua释放) + 可重入 + 自动续期(Redisson看门狗) + 红锁(多节点) |

### MySQL 相关

| 问题 | 核心要点 |
|------|---------|
| 索引数据结构为什么选 B+Tree？ | 多叉树比二叉树矮胖(减少磁盘IO) + 叶子节点链表支持范围查询 + 非叶子不存数据(更多索引项) |
| 聚簇索引 vs 非聚簇索引？ | 聚簇索引叶子存整行数据(主键索引)；非聚簇索引叶子存主键值(需回表)；覆盖索引不回表 |
| 最左前缀原则？ | 联合索引 (a,b,c) → WHERE a=1 走索引 → WHERE b=1 不走 → WHERE a=1 AND c=3 只有 a 生效 |
| 事务隔离级别？ | RU/RC/RR/Serializable，MySQL默认RR；脏读/不可重复读/幻读的区别 |
| MVCC 原理？ | 隐藏列(DB_TRX_ID+DB_ROLL_PTR) + ReadView + undo log 版本链 → 实现非锁定读(快照读) |
| SQL 优化思路？ | EXPLAIN 分析 → type(至少range)/key(走索引)/Extra(避免filesort) → 加索引 → 覆盖索引 → 表结构/分库分表 |

### 分布式相关

| 问题 | 核心要点 |
|------|---------|
| CAP 理论？ | 一致性/可用性/分区容错三选二，实践中分区容错必选，在 C 和 A 间权衡 |
| 分布式事务方案？ | 2PC(强一致) / TCC(Try-Confirm-Cancel，强一致) / Saga(长事务+补偿) / 本地消息表+MQ(最终一致) / Seata AT(自动，最终一致) |
| 分布式 ID 方案？ | Snowflake(趋势递增) / 号段模式(Leaf) / Redis(fast) / UUID(无序) |
| 服务雪崩如何防止？ | 限流(入口拦截) + 熔断(快速失败) + 降级(兜底逻辑) + 隔离(线程池/信号量) |
| 分布式 Session？ | Spring Session + Redis 统一存储 / JWT 无状态 |
| 分布式锁？ | Redis SETNX + Lua 释放 / Redisson / ZooKeeper 临时顺序节点 |

---

> 📌 本指南覆盖了 Java 后端开发的完整技术栈。建议先通读一遍建立整体认知，然后按学习路线逐步深入。遇到具体技术点时再回查对应章节。
> 
> 学习建议：**看 3 分，写 7 分**——看懂概念后一定要动手写代码，光看不练等于没学。
