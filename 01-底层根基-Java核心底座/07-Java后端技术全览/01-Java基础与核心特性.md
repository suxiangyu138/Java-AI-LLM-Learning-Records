# 01 - Java基础与核心特性

> 从语言底层机制到企业级特性，全面覆盖 Java SE 核心知识。

---

## 目录

1. [Java 生态体系概览](#1-java-生态体系概览)
2. [数据类型与内存](#2-数据类型与内存)
3. [面向对象深度解析](#3-面向对象深度解析)
4. [集合框架完全剖析](#4-集合框架完全剖析)
5. [异常处理机制](#5-异常处理机制)
6. [泛型与类型擦除](#6-泛型与类型擦除)
7. [注解与反射](#7-注解与反射)
8. [Lambda、Stream 与函数式编程](#8-lamda-stream-与函数式编程)
9. [并发与多线程](#9-并发与多线程)
10. [I/O、NIO 与网络编程](#10-ionio-与网络编程)
11. [Java 各版本演进与企业选型](#11-java-各版本演进与企业选型)
12. [常见面试题深度解析](#12-常见面试题深度解析)

---

## 1. Java 生态体系概览

### 1.1 JDK / JRE / JVM 关系图

```
┌───────────────────────────────────────────────────────────────────┐
│                           JDK（开发工具包）                          │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │  开发工具                                                     │  │
│  │  javac(编译器)  java(运行器)  javap(反编译)  jdb(调试器)       │  │
│  │  javadoc(文档)  jar(打包)  jshell(交互式)  jlink(自定义JRE)   │  │
│  ├─────────────────────────────────────────────────────────────┤  │
│  │  JRE（运行环境）                                              │  │
│  │  ┌───────────────────────────────────────────────────────┐  │  │
│  │  │ 核心类库（rt.jar / java.base 模块）                     │  │  │
│  │  │ java.lang / java.util / java.io / java.nio / ...       │  │  │
│  │  ├───────────────────────────────────────────────────────┤  │  │
│  │  │ JVM（Java 虚拟机）                                      │  │  │
│  │  │ ┌─────────────────────────────────────────────────┐   │  │  │
│  │  │ │ 类加载器 → 运行时数据区 → 执行引擎 → 本地方法接口  │   │  │  │
│  │  │ │ GC(垃圾回收)  JIT(即时编译)  解释器               │   │  │  │
│  │  │ └─────────────────────────────────────────────────┘   │  │  │
│  │  └───────────────────────────────────────────────────────┘  │  │
│  └─────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────┘
```

### 1.2 JVM 版本选型（企业实践）

| JDK 版本 | LTS | 发布时间 | 主流使用情况 | 企业建议 |
|---------|-----|---------|-------------|---------|
| JDK 8 | ✅ | 2014.03 | 🟡 大量遗留项目仍在使用 | 尽快迁移 |
| JDK 11 | ✅ | 2018.09 | 🟡 过渡版本 | 已选则可保留 |
| JDK 17 | ✅ | 2021.09 | 🟢 当前企业主流 LTS | **推荐首选** |
| JDK 21 | ✅ | 2023.09 | 🟢 新一代 LTS | **新项目强烈推荐** |
| JDK 25 | ✅ | 2025.09 | 🔵 最新 LTS | 评估中 |

```
Spring Boot 3.x → 要求 JDK 17+
Spring Boot 4.x → 将要求 JDK 21+

企业选型建议：
  新项目 → JDK 21，虚拟线程 + 模式匹配 + Record 类
  存量项目 → JDK 17，最稳妥的 LTS 版本
  云原生/容器 → GraalVM Native Image（AOT 编译，启动快、内存小）
```

### 1.3 编译与运行过程

```
┌────────────────────────────────────────────────────────────┐
│ 编译：Java 源码 (.java)                                     │
│   → javac 编译                                              │
│   → 字节码 (.class)                                         │
│   → 可以在任何平台的 JVM 上运行（跨平台）                      │
│                                                            │
│ 运行方式：                                                   │
│ 1. 解释执行（启动时，逐条解释字节码）                          │
│ 2. JIT 即时编译（热点代码编译为本地机器码，提高执行效率）       │
│    - C1（Client Compiler）：快速编译，较少优化                 │
│    - C2（Server Compiler）：深度优化，编译慢但执行快            │
│    - 分层编译（Tiered Compilation，JDK 7+ 默认）              │
│      Level 0: 纯解释                                        │
│      Level 1-3: C1 编译（逐步增强优化）                       │
│      Level 4: C2 编译（最高优化）                             │
│ 3. AOT 编译（GraalVM Native Image，构建时全量编译）           │
│    优点：启动快、内存占用小                                   │
│    缺点：不支持所有反射/动态代理场景                           │
└────────────────────────────────────────────────────────────┘
```

---

## 2. 数据类型与内存

### 2.1 基本类型完整对比

| 类型 | 大小 | 范围 | 包装类 | 默认值 | 缓存 |
|------|------|------|--------|--------|------|
| byte | 1 byte | -128 ~ 127 | Byte | 0 | ByteCache -128~127 |
| short | 2 bytes | -32768 ~ 32767 | Short | 0 | ShortCache -128~127 |
| int | 4 bytes | -2^31 ~ 2^31-1 | Integer | 0 | IntegerCache -128~127(可调) |
| long | 8 bytes | -2^63 ~ 2^63-1 | Long | 0L | LongCache -128~127 |
| float | 4 bytes | ±3.4E-38 ~ ±3.4E+38 | Float | 0.0f | 无 |
| double | 8 bytes | ±1.7E-308 ~ ±1.7E+308 | Double | 0.0d | 无 |
| char | 2 bytes | 0 ~ 65535 (Unicode) | Character | 'NULL' | ASCII 0~127 |
| boolean | JVM 决定 | true/false | Boolean | false | TRUE/FALSE 常量 |

### 2.2 自动装箱/拆箱陷阱

```java
// 陷阱 1：Integer 缓存范围
Integer a = 127;  Integer b = 127;
System.out.println(a == b);  // true（缓存范围内，同一对象）

Integer c = 128;  Integer d = 128;
System.out.println(c == d);  // false（超出缓存范围，不同对象）

// 可调整缓存上限：java -Djava.lang.Integer.IntegerCache.high=256

// 陷阱 2：三元运算符的类型提升
// 条件为 true → 返回 100，被自动装箱为 Integer
// 条件为 false → 返回 100.0，被自动装箱为 Double
// 结果：NPE！因为 Integer 无法被 unbox 再 box 为 Double
Object result = true ? 100 : 100.0;  // result instanceof Double → true

// 陷阱 3：Long 比较
Long l1 = 100L; Long l2 = 100L;
System.out.println(l1 == l2);      // true (缓存)
System.out.println(l1.equals(l2)); // true (值比较)
// 但 Long 和 Integer 之间 equals 永远 false
Long l = 100L;
System.out.println(l.equals(100)); // false！100 被装箱为 Integer，类型不同

// 最佳实践：包装类之间比较用 .equals()，别用 ==
```

### 2.3 浮点数精度问题

```java
// 经典问题：0.1 + 0.2 != 0.3
System.out.println(0.1 + 0.2);  // 0.30000000000000004

// 原因：十进制小数无法精确转为二进制浮点数
// 解决方案 1：BigDecimal（金融计算必须用）
BigDecimal a = new BigDecimal("0.1");
BigDecimal b = new BigDecimal("0.2");
BigDecimal sum = a.add(b);
System.out.println(sum);  // 0.3
// 注意：必须用字符串构造器，new BigDecimal(0.1) 仍然不精确！

// 比较 BigDecimal 用 compareTo，不用 equals
// equals 比较值和精度（scale），compareTo 只比较数值
new BigDecimal("1.0").equals(new BigDecimal("1.00"));  // false!
new BigDecimal("1.0").compareTo(new BigDecimal("1.00"));  // 0 (相等)

// 解决方案 2：分转元（金额用最小单位存储）
// 数据库存 10000（分），展示时除以 100 显示 100.00（元）
// 彻底避免浮点数精度问题
```

### 2.4 String 深度解析

```java
// JDK 9+ 底层结构：byte[] + coder（不再是 char[]）
// coder = LATIN1(0) 表示 Latin-1 编码，UTF16(1) 表示 UTF-16
// 对于拉丁字符，节省一半内存（Compact Strings）

// 字符串常量池（String Pool）
// JDK 7+ 移到堆中，可以 GC 回收
String s1 = "hello";           // 在常量池中
String s2 = "hello";           // 复用常量池中的对象
System.out.println(s1 == s2);  // true

String s3 = new String("hello");  // 在堆中创建新对象
System.out.println(s1 == s3);     // false

String s4 = s3.intern();  // 如果常量池已有"hello"，返回其引用
System.out.println(s1 == s4);  // true

// String 不可变的好处：
// 1. 线程安全（不可变对象天然线程安全）
// 2. 可以缓存 hash（HashMap 的 key 高效）
// 3. 字符串常量池（复用）
// 4. 安全性（作为类加载器参数、数据库连接等不被篡改）

// StringBuilder vs StringBuffer vs String.concat()
// 循环拼接：
String result = "";
for (int i = 0; i < 100; i++) {
    result += "hello";       // 每次创建新 String，共 100 个对象！
}
// 正确做法：
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 100; i++) {
    sb.append("hello");      // 同一个对象，性能优
}

// JDK 9+ 编译器优化：简单字符串拼接自动转为 invokedynamic（StringConcatFactory）
// 但循环中的拼接仍需手动用 StringBuilder
```

---

## 3. 面向对象深度解析

### 3.1 封装、继承、多态的本质

```
封装（Encapsulation）的本质：
  不是"把变量设为 private 然后生成 getter/setter"
  而是"隐藏内部实现细节，对外暴露稳定的行为接口"
  
  反例（这不是真正的封装）：
  public class User {
      private String name;
      public String getName() { return name; }
      public void setName(String name) { this.name = name; }
      // 如果 getter/setter 直接暴露内部字段，跟 public 没本质区别
  }
  
  正例（行为导向的封装）：
  public class User {
      private String name;
      private List<Role> roles;
      
      // 暴露行为而非数据
      public void changeName(String newName) { ... }
      public boolean hasPermission(String permissionCode) { ... }
      public void assignRole(Role role) {
          if (this.roles.size() >= MAX_ROLES) throw new BusinessException("...");
          this.roles.add(role);
      }
      // getter 做防御性拷贝
      public List<Role> getRoles() {
          return Collections.unmodifiableList(roles);
      }
  }
```

### 3.2 抽象类 vs 接口（设计层面的分析）

| 维度 | 抽象类 | 接口 |
|------|--------|------|
| **设计意图** | "是什么"（is-a），模板方法模式 | "能做什么"（can-do），行为契约 |
| **复用维度** | 代码复用（单继承） | 类型复用（多实现，mixin） |
| **演变能力** | 可添加非抽象方法而不破坏子类 | Java 8+ 可添加 default 方法 |
| **构造逻辑** | 可以有构造器，初始化公共状态 | 无构造器，不持有状态 |
| **访问控制** | 可以 private/protected | 默认 public（Java 9+ 允许 private 辅助方法） |
| **适用场景** | 有共同状态和行为的继承层次 | 跨层次的行为能力 |

```java
// 企业级实战：模板方法模式 + 策略模式
// 抽象类：定义算法骨架（模板方法）
public abstract class AbstractMessageHandler {
    // 模板方法（final 防止子类篡改）
    public final void handle(Message msg) {
        validate(msg);       // 步骤1：校验
        preProcess(msg);     // 步骤2：预处理（钩子方法）
        doHandle(msg);       // 步骤3：核心处理（抽象方法，子类实现）
        postProcess(msg);    // 步骤4：后处理（钩子方法）
        recordLog(msg);      // 步骤5：记录日志
    }
    protected abstract void doHandle(Message msg);
    protected void preProcess(Message msg) {}   // 钩子，可选覆盖
    protected void postProcess(Message msg) {}  // 钩子，可选覆盖
    private void validate(Message msg) { ... }
    private void recordLog(Message msg) { ... }
}

// 接口：定义可组合的能力
public interface Cacheable { String cacheKey(); }
public interface Auditable { String auditInfo(); }
public interface Retryable { int maxRetries(); }

public class OrderMessageHandler extends AbstractMessageHandler
    implements Cacheable, Auditable, Retryable {
    // 组合式设计，可以按需添加能力
}
```

### 3.3 重写(Override) vs 重载(Overload)

| 维度 | 重写 (Override) | 重载 (Overload) |
|------|----------------|-----------------|
| 定义位置 | 有继承关系的子类中 | 同一个类中 |
| 方法名 | 必须相同 | 必须相同 |
| 参数列表 | 必须相同 | 必须不同（类型/数量/顺序） |
| 返回值 | 必须相同或协变返回 | 可以不同 |
| 访问权限 | 不能比父类更严格 | 可以不同 |
| 异常 | 不能比父类更宽泛（或不同） | 可以不同 |
| 多态 | 运行时多态（动态绑定） | 编译时多态（静态绑定） |

### 3.4 对象初始化顺序

```java
// 对象创建时的执行顺序（面试常考）：
public class Child extends Parent {
    // 按以下顺序执行：
    // 1. 父类静态代码块和静态字段（按书写顺序）
    // 2. 子类静态代码块和静态字段（按书写顺序）
    // 3. 父类实例代码块和实例字段（按书写顺序）
    // 4. 父类构造方法
    // 5. 子类实例代码块和实例字段（按书写顺序）
    // 6. 子类构造方法

    private static int staticField = initStatic();     // 2
    private int instanceField = initInstance();         // 5

    static { System.out.println("子类静态代码块"); }      // 2
    { System.out.println("子类实例代码块"); }             // 5

    public Child() {
        super();  // 隐式调用父类构造器（4）
        System.out.println("子类构造方法");              // 6
    }
}
```

---

## 4. 集合框架完全剖析

### 4.1 集合框架全景图

```
                          Iterable (接口)
                              ↑
                         Collection (接口)
                  ┌──────────┼──────────────┐
                 List       Set           Queue/Deque
            ┌─────┴─────┐  ┌──┴────┐    ┌───┴────┐
        ArrayList  LinkedList HashSet TreeSet PriorityQueue ArrayDeque
        Vector    CopyOnWrite  LinkedHashSet   ConcurrentLinkedQueue
                    ArrayList
                                            BlockingQueue (接口)
                                        ┌───────┼────────┐
                                  ArrayBlockingQueue  LinkedBlockingQueue

                         Map (独立体系，非 Collection 子接口)
              ┌───────────┼──────────┐
            HashMap   TreeMap   Hashtable (遗留)
         ┌──┴──┐                       |
        Linked  ConcurrentHashMap   Properties (遗留)
        HashMap   ConcurrentSkipListMap
```

### 4.2 List 家族

| 实现类 | 底层结构 | 初始容量 | 扩容机制 | 线程安全 | 随机访问 | 增删效率 |
|--------|---------|---------|---------|---------|---------|---------|
| **ArrayList** | Object[] | 10 | old × 1.5 | ❌ | O(1) | O(n) |
| **LinkedList** | 双向链表 | 无 | 无 | ❌ | O(n) | O(1) |
| **Vector** | Object[] | 10 | old × 2 | ✅(synchronized) | O(1) | O(n) |
| **CopyOnWriteArrayList** | Object[] | 0 | +1(复制) | ✅(ReentrantLock) | O(1) | O(n) 且复制 |

```
ArrayList 扩容细节：
  1. new ArrayList() → elementData = {}（空数组，延迟初始化）
  2. 第一次 add → 扩容到 10
  3. 容量不够时 → 扩容到 oldCapacity * 1.5（即右移 1 位：old + old >> 1）
  4. 如果扩容后还不够 → 扩容到 minCapacity
  5. 使用 Arrays.copyOf() 复制数据

实战建议：
  - 已知数据量时，构造器指定 initialCapacity，避免多次扩容
  - 频繁随机访问 → ArrayList（数组连续内存，CPU 缓存友好）
  - 频繁头尾增删 → LinkedList（但遍历慢，大部分场景 ArrayList 仍然更快）
  - 读多写少 → CopyOnWriteArrayList（每次修改复制全数组，写开销大）
```

### 4.3 Map 家族深度对比

| 实现类 | 底层 | 初始容量 | 扩容 | 线程安全 | null key | null value | 有序性 | JDK |
|--------|------|---------|------|---------|----------|----------|--------|-----|
| **HashMap** | 数组+链表+红黑树 | 16 | 翻倍 | ❌ | ✅(1个) | ✅ | ❌ | 1.2 |
| **Hashtable** | 数组+链表 | 11 | ×2+1 | ✅(synchronized) | ❌ | ❌ | ❌ | 1.0(过时) |
| **LinkedHashMap** | HashMap+双向链表 | 16 | 翻倍 | ❌ | ✅ | ✅ | 插入/访问序 | 1.4 |
| **TreeMap** | 红黑树 | 无 | 无 | ❌ | ❌ | ✅ | 自然序 | 1.2 |
| **ConcurrentHashMap** | 同HashMap+同步 | 16 | 翻倍 | ✅(CAS+synchronized) | ❌ | ❌ | ❌ | 1.5 |

### 4.4 HashMap 源码剖析

```java
// JDK 8 HashMap 核心字段
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;  // 16
static final int MAXIMUM_CAPACITY = 1 << 30;
static final float DEFAULT_LOAD_FACTOR = 0.75f;
static final int TREEIFY_THRESHOLD = 8;              // 链表转红黑树阈值（链表长度）
static final int UNTREEIFY_THRESHOLD = 6;            // 红黑树退化为链表阈值
static final int MIN_TREEIFY_CAPACITY = 64;          // 树化的最小数组长度

// put 方法完整流程：
final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
    // 1. 数组为空 → resize() 初始化（容量 16）
    // 2. 计算下标：(n - 1) & hash
    //    为什么用 & 而不是 %？因为 n 是 2 的幂，(n-1) & hash 效率更高
    // 3. 该位置为空 → 直接放入
    // 4. 该位置不为空
    //    a. 第一个节点 key 相同 → 覆盖 value
    //    b. 是红黑树 → 调用 putTreeVal()
    //    c. 是链表 → 遍历
    //       - 找到相同 key → 覆盖
    //       - 遍历到末尾 → 尾插（JDK 7 是头插，并发扩容时会产生循环链表！）
    //       - 插入后长度 >= 8 且 数组长度 >= 64 → 转红黑树
    // 5. size++，检查是否超过 threshold → 是则 resize()
}

// hash 计算方法（扰动函数）：
// 为什么要 (h = key.hashCode()) ^ (h >>> 16)？
//   让高 16 位也参与下标计算，减少 hash 碰撞
//   hashCode 是 int(32位)，但数组长度通常较小（如 16，只用到低 4 位）
//   让高位也参与，使 hash 值更分散

// resize（扩容）过程：
// 1. 容量翻倍
// 2. 遍历旧数组每个位置
// 3. 单个节点 → 直接迁移到新位置（index 或 index + oldCap）
//    为什么只可能这两个位置？因为扩容翻倍，hash 要么不变要么 + oldCap
//    例子：oldCap=16 (10000), newCap=32 (100000)
//         旧位置 = hash & 15 (01111)
//         新位置 = hash & 31 (11111)
//         差异仅在 hash 的第 5 位是 0 还是 1
// 4. 链表 → 拆成两个链表，分别放到两个位置
// 5. 红黑树 → 拆成两棵，<=6 个节点退化为链表

// 关键设计（面试常考）：
// 1. 为什么容量是 2 的幂？
//    → (n-1) & hash 高效取模；扩容时迁移简单（只可能两个位置）
// 2. 为什么负载因子 0.75？
//    → 空间和时间的折中。太大则哈希碰撞多（链表长），太小则频繁扩容
//    该值基于泊松分布计算，桶中节点达到 8 的概率约百万分之一
// 3. JDK 7 vs JDK 8 区别？
//    → 7: 数组+链表(头插)   → 并发扩容可能死循环
//    → 8: 数组+链表(尾插)+红黑树 → 解决死循环，树化提升极端情况性能
```

### 4.5 ConcurrentHashMap 演进

```java
// JDK 7：分段锁（Segment），默认 16 个 Segment，每个 Segment 独立加锁
//       理论上最多支持 16 个线程并发写
//       问题：Segment 数量固定，查询需要两次 hash

// JDK 8：CAS + synchronized（细粒度锁），锁粒度到桶级别
//       数据结构改为跟 HashMap 一致：数组 + 链表/红黑树
//       锁只锁链表头节点，并发度更高

// put 操作核心流程：
// 1. key/hashCode 都不能是 null（抛 NPE）
// 2. for 循环 + CAS 自旋
//    a. 数组未初始化 → initTable()（CAS 设置 sizeCtl）
//    b. 桶为空 → CAS 放进去
//    c. 正在扩容 → 帮助扩容（helpTransfer）
//    d. 桶有数据 → synchronized 锁住头节点，然后插入/覆盖
//    e. 插入后检查是否转树

// size 计算：
// 不维护单一 size 变量（竞争太激烈）
// 使用 BaseCount + CounterCell[] 数组分段计数
// sumCount() = baseCount + sum(CounterCell[].value)
// 类似于 LongAdder（JDK 8 新增，高性能累加器）
```

### 4.6 集合框架面试进阶

```java
// Q: HashMap 和 Hashtable 的区别？
// A: 5 个方面：
//    1. 线程安全：Hashtable 是，HashMap 不是
//    2. null：Hashtable key/value 都不允许 null；HashMap 都允许（key 只一个 null）
//    3. 继承：Hashtable 继承 Dictionary；HashMap 继承 AbstractMap
//    4. 初始容量：Hashtable 11，HashMap 16；扩容：Hashtable ×2+1，HashMap ×2
//    5. 效率：Hashtable synchronized 方法级，HashMap 无锁 → 相差很大

// Q: 为什么重写 equals() 必须重写 hashCode()？
// A: HashMap 先比较 hashCode 再 equals。两个对象 equals 相等但 hashCode 不同，
//    会导致同一个 key 放到不同的桶中，put 和 get 的桶不一致。
//    违反 Object.hashCode() 约定：相等对象必须有相同 hashCode。

// Q: Collections.synchronizedMap 和 ConcurrentHashMap 的区别？
// A: synchronizedMap 使用 synchronized 代码块锁住整个 map 对象（一个锁）
//    ConcurrentHashMap 分段锁/桶级锁，并发度远超 synchronizedMap
```

---

## 5. 异常处理机制

### 5.1 异常体系全景

```
                          Throwable
                    ┌─────────┴──────────┐
                  Error               Exception
              ┌────┼──────┐        ┌─────┴──────┐
         OOM   SO  NoClassDef  RuntimeException  CheckedException
        (OutOf (Stack (类加载 (非受检异常)       (受检异常)
        Memory) Overflow) 错误)
                              ├─ NullPointerException     ├─ IOException
                              ├─ IndexOutOfBoundsException├─ SQLException
                              ├─ IllegalArgumentException├─ ClassNotFoundException
                              ├─ ArithmeticException      ├─ InterruptedException
                              ├─ ConcurrentModificationEx 
                              └─ NumberFormatException
```

### 5.2 Error vs Exception

```
Error：
  - 系统级错误，程序无法处理
  - 不应 try-catch（因为即使 catch 了也无法恢复）
  - 常见：OutOfMemoryError、StackOverflowError、NoClassDefFoundError
  
Exception：
  - 程序可处理的异常
  - 受检异常（Checked）：编译期强制处理（try-catch 或 throws 声明）
    设计意图：调用者必须知道并处理这种异常情况
    例子：IOException、SQLException、FileNotFoundException
    争议：被过度使用，导致 catch 块空着，或者 throws 一路抛到顶层
  
  - 非受检异常（Unchecked = RuntimeException + Error）：
    设计意图：编程错误或不可恢复的错误，调用者不应该处理
    例子：NullPointerException、IllegalArgumentException、IndexOutOfBoundsException
    Spring 事务默认只对 RuntimeException 和 Error 回滚
```

### 5.3 try-with-resources

```java
// JDK 7 前：finally 关闭资源（丑陋）
FileInputStream fis = null;
try {
    fis = new FileInputStream("file.txt");
    // ...
} catch (IOException e) {
    // ...
} finally {
    if (fis != null) {
        try { fis.close(); } catch (IOException e) { }
    }
}

// JDK 7+：try-with-resources（优雅，自动关闭 AutoCloseable 资源）
try (FileInputStream fis = new FileInputStream("file.txt");
     BufferedInputStream bis = new BufferedInputStream(fis)) {
    // 自动关闭，关闭顺序与声明顺序相反
}
// JDK 9+：可以在 try 外声明变量
FileInputStream fis = new FileInputStream("file.txt");
try (fis) { ... }  // 更简洁
```

### 5.4 企业级异常处理最佳实践

```java
// 分层异常设计
// 1. 业务异常（可预见的错误，如"用户不存在"）
public class BusinessException extends RuntimeException {
    private final int code;
    private final String message;
    // 可选：错误码枚举、国际化 key、附加数据
}

// 2. 系统异常（底层技术异常的统一包装）
public class SystemException extends RuntimeException {
    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }
}

// 3. Service 层异常转换
@Slf4j
public class UserServiceImpl implements UserService {
    public User getUser(Long id) {
        try {
            return userMapper.findById(id);
        } catch (DataAccessException e) {
            // 将技术异常转为业务异常或系统异常
            log.error("数据库查询用户失败, id={}", id, e);
            throw new SystemException("查询用户服务暂时不可用", e);
        }
    }
}

// 4. 全局异常处理（返回统一结构给前端）
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusiness(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ":" + err.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return Result.error(400, msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleUnknown(Exception e) {
        log.error("未捕获的异常", e);
        // 生产环境不返回细节给前端，避免信息泄露
        return Result.error(500, "服务器内部错误，请联系管理员");
    }
}

// 5. 异常处理的反模式（千万不要这样做！）
// ❌ 吞掉异常
try { doSomething(); } catch (Exception e) { }
// ❌ 只打印不处理
try { doSomething(); } catch (Exception e) { e.printStackTrace(); }
// ❌ 异常用在流程控制
try { user = findUser(id); } catch (UserNotFoundException e) { return null; }
// → 应该用 Optional<User> 或专门的 exists() 方法

// ✅ 正确做法
try {
    doSomething(); // 尝试
} catch (SpecificException e) {
    log.error("操作失败，参数={}", param, e);
    throw new BusinessException("用户友好的提示", e);  // 包装并重新抛出
}
```

---

## 6. 泛型与类型擦除

### 6.1 基本语法

```java
// 泛型类
public class Result<T> {
    private T data;
    public T getData() { return data; }
}

// 泛型接口
public interface Repository<T, ID extends Serializable> {
    Optional<T> findById(ID id);
    List<T> findAll();
}

// 泛型方法
public static <T> T getFirst(List<T> list) {
    return list.isEmpty() ? null : list.get(0);
}

// 类型通配符
// ? extends T → 上界通配符（get 数据，生产者）
// ? super T   → 下界通配符（put 数据，消费者）
// PECS 原则：Producer Extends, Consumer Super

// 应用场景
public void copy(List<? extends Number> src, List<? super Number> dest) {
    // src：只读（保证至少是 Number 类型）
    // dest：只写（保证能接收 Number 类型）
    for (Number n : src) { dest.add(n); }
}
```

### 6.2 类型擦除影响

```java
// Java 的泛型是编译期特性，运行时会擦除类型信息
// 编译后 List<Integer> 和 List<String> 都是 List
List<Integer> intList = new ArrayList<>();
List<String> strList = new ArrayList<>();
System.out.println(intList.getClass() == strList.getClass());  // true! 同是 ArrayList

// 类型擦除带来的限制：
// 1. 不能 new 泛型类型
// T t = new T();  // ❌ 编译错误

// 2. 不能 instanceof 泛型类型
// if (obj instanceof List<String>) { }  // ❌ 编译错误

// 3. 不能创建泛型数组
// T[] arr = new T[10];  // ❌ 编译错误

// 4. 泛型类不能继承 Throwable
// class MyException<T> extends Exception { }  // ❌ 编译错误

// 5. 静态方法中不能使用类的泛型参数
// class MyClass<T> {
//     static T field;  // ❌ 编译错误
// }

// 绕过类型擦除的技巧：
// - 反射获取泛型信息（通过 ParameterizedType，如 Gson/Jackson 反序列化时的 TypeToken）
// - 在方法参数中保留类型信息
public <T> T create(Class<T> clazz) throws Exception {
    return clazz.getDeclaredConstructor().newInstance();
}
```

---

## 7. 注解与反射

### 7.1 注解详解

```java
// 元注解（注解的注解）
@Target({ElementType.TYPE, ElementType.METHOD})     // 可以用在哪里
@Retention(RetentionPolicy.RUNTIME)                 // 保留到运行时
@Documented                                         // 包含在 javadoc 中
@Inherited                                          // 子类继承（仅对类注解有效）
@Repeatable(Authors.class)                          // 可重复标注（JDK 8+）
public @interface Author {
    String name();
    String date() default "2024-01-01";
}

// 编译器注解（编译期处理，运行时不需要）
@Retention(RetentionPolicy.SOURCE)
public @interface NotThreadSafe { }
// 如 Lombok 的 @Data, @Builder，编译期生成代码，class 文件里无此注解

// 运行时注解（Spring 的核心能力来源）
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component { String value() default ""; }
// Spring 启动时扫描类路径，通过反射获取带 @Component 的类，注册为 Bean
```

### 7.2 反射详解

```java
// 反射的 4 种获取 Class 对象的方式
Class<?> c1 = Class.forName("com.example.User");  // 全限定类名
Class<?> c2 = User.class;                          // 类字面量
Class<?> c3 = user.getClass();                     // 对象 getClass()
Class<?> c4 = ClassLoader.getSystemClassLoader().loadClass("com.example.User");

// 反射操作
Class<?> clazz = User.class;

// 构造器
Constructor<?> ctor = clazz.getDeclaredConstructor(String.class, Integer.class);
ctor.setAccessible(true);  // 绕过 private
User user = (User) ctor.newInstance("张三", 25);

// 字段
Field field = clazz.getDeclaredField("name");
field.setAccessible(true);
field.set(user, "李四");
String name = (String) field.get(user);

// 方法
Method method = clazz.getDeclaredMethod("setAge", int.class);
method.setAccessible(true);
method.invoke(user, 30);

// 实际应用场景：
// 1. Spring 依赖注入（@Autowired 注入到 private 字段）
// 2. MyBatis 结果映射（ResultSet → Java 对象）
// 3. JSON 序列化/反序列化（Jackson/Fastjson）
// 4. ORM 框架（JPA/Hibernate 实体映射）
// 5. 单元测试框架（JUnit 调用 @Test 方法）
// 6. AOP 动态代理（JDK 动态代理用反射调用目标方法）

// 性能问题：
// 反射比直接调用慢数十倍（需要安全检查、类型包装等）
// 优化手段：MethodHandle（JDK 7）、LambdaMetafactory（JDK 8）
// 但框架设计中的使用不可避免，且框架会缓存反射对象
```

---

## 8. Lambda、Stream 与函数式编程

### 8.1 函数式接口

```java
// 四大核心函数式接口
@FunctionalInterface
public interface Predicate<T> {     // 断言型：T → boolean
    boolean test(T t);
}

@FunctionalInterface
public interface Consumer<T> {      // 消费型：T → void
    void accept(T t);
}

@FunctionalInterface
public interface Function<T, R> {   // 函数型：T → R
    R apply(T t);
}

@FunctionalInterface
public interface Supplier<T> {      // 供给型：() → T
    T get();
}

// 还有 BiXxx 变体（两个参数）：
// BiPredicate<T, U>、BiConsumer<T, U>、BiFunction<T, U, R>
// UnaryOperator<T> extends Function<T, T>  （输入输出同类型）
// BinaryOperator<T> extends BiFunction<T, T, T>
```

### 8.2 Stream 完整操作分类

```java
// Stream 操作分为三类：

// === 1. 创建流 ===
Stream.of(1, 2, 3);
Arrays.stream(array);
list.stream();
list.parallelStream();        // 并行流（ForkJoinPool.commonPool()）
IntStream.range(1, 100);      // 聚合统计专用
Stream.iterate(0, n -> n + 1).limit(10);  // 无限流
Stream.generate(Math::random).limit(10);

// === 2. 中间操作（惰性求值，返回 Stream） ===
// 过滤和切片
filter(Predicate)        // 过滤
distinct()              // 去重（基于 equals）
limit(long)             // 截断前 N 个
skip(long)              // 跳过前 N 个

// 映射
map(Function)           // 一对一映射
flatMap(Function)       // 一对多映射 + 扁平化
mapToInt/mapToLong...   // 映射为基本类型流（避免装箱）

// 排序
sorted()                // 自然排序
sorted(Comparator)      // 自定义排序
// 注意：使用 sorted() 时所有元素需要先加载到内存

// 调试
peek(Consumer)          // 查看中间结果（常用于 debug）

// === 3. 终端操作（触发计算，返回结果） ===
// 匹配和查找
allMatch(Predicate)     // 全部匹配 → boolean
anyMatch(Predicate)     // 任意匹配 → boolean
noneMatch(Predicate)    // 全部不匹配 → boolean
findFirst()             // 返回第一个 → Optional
findAny()               // 返回任意一个（并行流高效）→ Optional

// 聚合
reduce(BinaryOperator)  // 归约 → Optional
count()                 // 计数 → long
max(Comparator)/min(Comparator) // 最大/最小 → Optional

// 收集（重要！）
collect(Collectors.toList())
collect(Collectors.toSet())
collect(Collectors.toMap(Function keyMapper, Function valueMapper))
collect(Collectors.groupingBy(Function))        // 分组
collect(Collectors.partitioningBy(Predicate))   // 分区（只有两组，true/false）
collect(Collectors.joining(","))                 // 字符串拼接
collect(Collectors.summarizingInt())            // 汇总统计（count/sum/min/avg/max）

// 遍历
forEach(Consumer)
```

### 8.3 Stream 实战与注意事项

```java
// 常见业务场景 1：分组统计
Map<String, Long> deptUserCount = users.stream()
    .collect(Collectors.groupingBy(User::getDeptName, Collectors.counting()));

// 常见业务场景 2：List 转 Map（注意 key 冲突处理）
Map<Long, User> userMap = users.stream()
    .collect(Collectors.toMap(User::getId, Function.identity(),
        (existing, replacement) -> replacement));  // key 冲突时保留后者

// 常见业务场景 3：多级分组
Map<String, Map<Integer, List<User>>> deptAgeGroups = users.stream()
    .collect(Collectors.groupingBy(User::getDeptName,
                Collectors.groupingBy(User::getAge)));

// ⚠️ 注意事项：
// 1. 流不能重复使用
Stream<String> stream = list.stream();
stream.forEach(...);  // OK
stream.collect(...);  // ❌ IllegalStateException: stream has already been operated

// 2. Lambda 中使用的局部变量必须是「事实上的 final」
int x = 10;
list.stream().filter(i -> i > x);  // OK（x 未被修改）
// x = 20;  // 如果加这句，上面的 filter 就编译不过

// 3. 并行流谨慎使用
// ForkJoinPool 线程数是 CPU 核心数 - 1
// IO 密集场景用并行流需要自定义线程池：
ForkJoinPool customPool = new ForkJoinPool(20);
customPool.submit(() -> list.parallelStream().forEach(...));

// 4. Collectors.toMap 的 NPE 陷阱
// 如果 valueMapper 返回 null，toMap 直接 NPE
// 原因：HashMap 允许 null value，但 toMap 实现中调用了 merge，merge 不允许 null

// 5. 大集合分组注意内存
// 100 万数据 groupBy 到 1000 个组 → JVM 堆压力大
```

---

## 9. 并发与多线程

### 9.1 Java 内存模型 (JMM)

```
Java Memory Model（JMM，Java 内存模型）规定了：
  1. 什么是共享变量可见性
  2. 什么是指令重排序
  3. 什么是 happens-before 关系

JMM 抽象结构：
┌─────────┐  ┌─────────┐
│ 线程 A   │  │ 线程 B   │
│ ┌─────┐ │  │ ┌─────┐ │
│ │本地内存│  │  │本地内存│  ← 每个线程有自己的工作内存（缓存/寄存器）
│ └──┬──┘ │  │ └──┬──┘ │
└────┼────┘  └────┼────┘
     │  读取/写入   │
┌────┴─────────────┴────┐
│       主内存            │  ← 堆内存（所有线程共享）
│   (共享变量存储位置)      │
└────────────────────────┘

happens-before 规则（8 条核心规则）：
  1. 程序顺序规则：同一线程中，前面的操作 happens-before 后面的操作
  2. 监视器锁规则：解锁 happens-before 后续加锁
  3. volatile 规则：volatile 写 happens-before 后续 volatile 读
  4. 传递性规则：A hb B + B hb C → A hb C
  5. 线程启动规则：Thread.start() hb 该线程的任何操作
  6. 线程终止规则：线程的任何操作 hb 该线程的 join() 返回
  7. 中断规则：interrupt() hb 被中断线程检测到中断
  8. 终结器规则：对象构造完成 hb finalize() 开始
```

### 9.2 线程创建方式（4 种）

```java
// 方式 1：继承 Thread（不推荐，Java 单继承，扩展性差）
class MyThread extends Thread {
    public void run() { System.out.println("thread running"); }
}
new MyThread().start();

// 方式 2：实现 Runnable（无返回值）
new Thread(() -> System.out.println("thread running")).start();

// 方式 3：实现 Callable + FutureTask（有返回值，可抛异常）
Callable<String> task = () -> { Thread.sleep(1000); return "result"; };
FutureTask<String> futureTask = new FutureTask<>(task);
new Thread(futureTask).start();
String result = futureTask.get(2, TimeUnit.SECONDS);  // 带超时

// 方式 4：线程池（推荐！复用线程，控制资源）
ExecutorService executor = Executors.newFixedThreadPool(10);
Future<String> future = executor.submit(() -> "result");
executor.shutdown();  // 不接新任务，已有任务执行完再停止
// executor.shutdownNow();  // 立即停止，返回未开始的任务列表
```

### 9.3 线程池完全详解

```java
// ThreadPoolExecutor 7 个参数
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5,                              // corePoolSize 核心线程数
    10,                             // maximumPoolSize 最大线程数
    60L, TimeUnit.SECONDS,          // keepAliveTime 空闲非核心线程存活时间
    new LinkedBlockingQueue<>(100), // workQueue 工作队列
    new ThreadFactory() {           // threadFactory 线程工厂
        private final AtomicInteger count = new AtomicInteger(1);
        public Thread newThread(Runnable r) {
            return new Thread(r, "pool-thread-" + count.getAndIncrement());
        }
    },
    new ThreadPoolExecutor.CallerRunsPolicy() // rejectedHandler 拒绝策略
);

// 执行流程（面试重点！）：
// 新任务提交 →
//   1. 核心线程有空闲？→ 是 → 核心线程执行
//   2. 核心线程满了 → 入队列
//   3. 队列满了？→ 否 → 等待
//   4. 队列满了 → 当前线程数 < maximumPoolSize？→ 是 → 创建临时线程执行
//   5. 当前线程数 == maximumPoolSize → 执行拒绝策略

// 四种拒绝策略：
// AbortPolicy           抛 RejectedExecutionException（默认）
// CallerRunsPolicy      由调用者线程执行该任务（反馈机制，减缓提交速度）
// DiscardOldestPolicy   丢弃队列中最旧的任务，然后重试提交
// DiscardPolicy         静默丢弃

// 常见线程池及问题：
// FixedThreadPool → core=max=n, 队列 LinkedBlockingQueue 无界 → 可能 OOM
// CachedThreadPool → core=0, max=MAX_VALUE, 队列 SynchronousQueue → 无限创建线程 OOM
// ScheduledThreadPool → 核心线程固定，无限队列 → 可能 OOM
// SingleThreadExecutor → core=max=1, 无界队列 → 可能 OOM

// 正确姿势：必须自定义 ThreadPoolExecutor！
// 阿里规约：线程池不允许使用 Executors 去创建！要通过 ThreadPoolExecutor 的方式

// 线程数公式参考：
// CPU 密集型：N+1（N = CPU 核心数，+1 是为了在某线程缺页中断时顶上）
// IO 密集型：2N（IO 等待时 CPU 空闲，可以多来几个线程）
// 混合型：实际压测决定
```

### 9.4 线程同步技术栈

```java
// 从 synchronized → Lock → CAS → ThreadLocal 的演进

// === 1. synchronized（JVM 内置锁） ===
// 修饰实例方法 → 锁 this
// 修饰静态方法 → 锁 Class 对象
// 修饰代码块   → 锁指定对象
// 可重入：同一个线程再次获取同一个锁不会阻塞

// 锁升级过程（JDK 6 起，synchronized 不再是重量级锁）：
// 无锁 → 偏向锁 → 轻量级锁 → 重量级锁
//  偏向锁：同一个线程多次进入同步块，CAS 设置线程 ID，无需加锁解锁
//  轻量级锁：多个线程交替进入（无竞争），自旋 CAS 替换 Mark Word
//  重量级锁：竞争激烈，挂起线程，内核态切换

// === 2. Lock 显式锁（java.util.concurrent.locks） ===
// ReentrantLock：可重入互斥锁
ReentrantLock lock = new ReentrantLock(true);  // true = 公平锁
lock.lock();
try {
    // 临界区代码
} finally {
    lock.unlock();  // 必须 unlock！否则死锁
}
// vs synchronized：
//   - Lock 可尝试获取锁（tryLock）、可中断获取锁（lockInterruptibly）
//   - Lock 可获得等待线程信息、Condition 精确唤醒
//   - synchronized JVM 自动释放，Lock 必须手动释放

// ReentrantReadWriteLock：读写锁
// 读共享，写互斥 → 适用于读多写少场景
// 注意：写锁可以降级为读锁，但读锁不能升级为写锁

// StampedLock（JDK 8）：乐观读模式，比读写锁性能更好
// 乐观读模式下完全不阻塞，读完检查 stamp 是否有写入

// === 3. AQS（AbstractQueuedSynchronizer，队列同步器） ===
// ReentrantLock、CountDownLatch、Semaphore、ReentrantReadWriteLock
// 全部基于 AQS 实现！
// 核心：一个 volatile int state + 一个 CLH 等待队列（双向链表）
// state = 0 → 无锁；state = 1 → 有锁（重入时 +1）

// === 4. CAS 无锁（Compare And Swap） ===
// 硬件级别的原子操作（CPU 指令 cmpxchg）
// AtomicInteger、AtomicReference 等
AtomicInteger ai = new AtomicInteger(0);
ai.incrementAndGet();  // while(!CAS(current, current+1))
// CAS 三大问题：
//   ABA 问题 → AtomicStampedReference（加版本号）
//   自旋开销大 → 适合锁持有时间短的场景
//   只能保证一个变量的原子性 → AtomicReference 或多变量用锁

// === 5. ThreadLocal ===
// 每个线程持有独立的变量副本，彻底避免共享
ThreadLocal<SimpleDateFormat> formatter = ThreadLocal.withInitial(
    () -> new SimpleDateFormat("yyyy-MM-dd")
);
// 内存泄漏风险：ThreadLocalMap 的 key 是弱引用，但 value 是强引用
// ThreadLocal 对象被回收后，key 变为 null，但 value 还在
// 必须调用 remove()！否则在线程池场景会积累泄漏

// === 6. JUC 工具类 ===
// CountDownLatch：一个线程等待多个线程完成
CountDownLatch latch = new CountDownLatch(3);
// 各线程: latch.countDown()
// 主线程: latch.await()

// CyclicBarrier：多个线程互相等待到齐
CyclicBarrier barrier = new CyclicBarrier(3, () -> System.out.println("全部到齐"));
// 可重复使用（计数重置）

// Semaphore：信号量，控制同时访问数量
Semaphore semaphore = new Semaphore(5);  // 最多 5 个并发
semaphore.acquire();  // 获取许可（可获取多个）
semaphore.release();

// Exchanger：两个线程交换数据

// CompletableFuture：异步编排（Java 8+，功能最强）
CompletableFuture.supplyAsync(() -> fetchUser())
    .thenApply(user -> enrichUserInfo(user))
    .thenAccept(user -> saveUser(user))
    .exceptionally(e -> { log.error("error", e); return null; });
```

### 9.5 虚拟线程（Virtual Threads，Java 21） ⭐

```java
// 虚拟线程是 Java 21 最重要的新特性

// 传统平台线程的痛点：
//   - 1:1 映射到 OS 线程（天然重量级，创建和切换成本高）
//   - 默认栈 1MB（创建 1000 个就需要 1GB 内存）
//   - IO 阻塞时线程挂起，但占用栈空间，浪费资源

// 虚拟线程的革命：
//   - M:N 映射到平台线程（对应 Go 的 goroutine、Kotlin 协程）
//   - 大量虚拟线程由少量平台线程承载
//   - 虚拟线程在 IO 阻塞时自动让出平台线程（称为 "mount/unmount"）
//   - 可以创建百万级别的虚拟线程
//   - 栈是小对象（StackChunk），在堆上分配，可灵活扩展

// 创建虚拟线程：
Thread vt = Thread.startVirtualThread(() -> {
    System.out.println("running in virtual thread");
});

// 通过 ExecutorService（推荐，每个任务自动分配虚拟线程）
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    // 假设有 10000 个 IO 任务
    List<Future<String>> futures = new ArrayList<>();
    for (int i = 0; i < 10000; i++) {
        futures.add(executor.submit(() -> fetchFromAPI()));
    }
}

// 适用场景：
// ✅ IO 密集型任务（HTTP 请求、数据库查询、文件操作）
// ✅ 需要大量并发线程的场景
// ✅ 替代 CompletableFuture 的异步回调地狱（代码更像同步）
// ❌ CPU 密集型任务（计算密集，虚拟线程帮不上忙）
// ❌ 需要固定线程控制并发数（虚拟线程擅长大量并发）

// 企业实践：
// Spring Boot 3.2+ 支持虚拟线程
// spring.threads.virtual.enabled=true
// 替代 @Async + TaskExecutor，Controller 可以直接返回虚拟线程处理的结果
```

---

## 10. I/O、NIO 与网络编程

### 10.1 I/O 模型对比

```
BIO（Blocking I/O）同步阻塞：
  ServerSocket.accept() 阻塞等待连接
  InputStream.read() 阻塞等待数据
  每连接一线程 → 线程数 = 连接数 → 大量连接时线程资源耗尽

NIO（Non-blocking I/O）同步非阻塞：
  Channel + Buffer + Selector（IO 多路复用）
  一个 Selector 线程管理大量连接 → 连接数 >> 线程数
  Selector 轮询已就绪的 Channel → 只处理有数据的连接
  Netty / Tomcat NIO connector / Redis 底层都用了 NIO

AIO（Asynchronous I/O）异步非阻塞：
  操作系统回调通知
  Java 7 NIO.2 引入，但 Linux 不支持真正的 AIO（epoll 仍是轮询）
  应用较少，Windows 下 IOCP 是真正的 AIO
```

### 10.2 NIO 核心组件

```java
// Buffer（缓冲区）：数据容器
// ByteBuffer、CharBuffer、IntBuffer、LongBuffer 等
ByteBuffer buffer = ByteBuffer.allocate(1024);     // 堆内存
ByteBuffer directBuffer = ByteBuffer.allocateDirect(1024);  // 直接内存（零拷贝）
// Buffer 四个核心属性：
// capacity: 容量（固定）
// position: 当前位置（读/写到哪了）
// limit:    限制（最多读/写到哪）
// mark:     标记（可回退到标记位置）
buffer.put("hello".getBytes());
buffer.flip();      // limit = position, position = 0（切换到读模式）
byte b = buffer.get();
buffer.compact();   // 保留未读数据，准备再写
buffer.clear();     // 清空（position = 0, limit = capacity，实际数据还在）

// Channel（通道）：双向数据传输
// FileChannel、SocketChannel、ServerSocketChannel、DatagramChannel
ServerSocketChannel serverChannel = ServerSocketChannel.open();
serverChannel.configureBlocking(false);  // 设置为非阻塞
serverChannel.bind(new InetSocketAddress(8080));

// Selector（选择器）：IO 多路复用的核心
Selector selector = Selector.open();
serverChannel.register(selector, SelectionKey.OP_ACCEPT);

while (true) {
    selector.select();  // 阻塞直到至少一个 Channel 就绪
    Set<SelectionKey> keys = selector.selectedKeys();
    for (SelectionKey key : keys) {
        if (key.isAcceptable()) { /* 处理新连接 */ }
        if (key.isReadable())   { /* 处理读事件 */ }
        if (key.isWritable())   { /* 处理写事件 */ }
    }
    keys.clear();
}
```

### 10.3 Netty 为什么快？

```
Netty 是 Java 网络编程的事实标准（Dubbo、RocketMQ、gRPC、Spring WebFlux 等都用它）

高性能的原因：
1. IO 模型：NIO（epoll）+ 主从 Reactor 线程模型
   Boss Group（处理连接） + Worker Group（处理读写）
   非阻塞 + 多路复用，少数线程处理大量连接

2. 零拷贝（Zero Copy）：
   - 堆外内存（Direct Buffer）：直接操作堆外内存，避免 JVM 堆 ↔ 堆外拷贝
   - CompositeByteBuf：合并多个 Buffer 不复制数据
   - FileRegion.transferTo()：sendfile 系统调用，数据不经过用户态

3. 内存池（PooledByteBufAllocator）：
   重用 ByteBuf，减少 GC 压力

4. 高效的线程模型：
   串行化处理（一个 Channel 的所有操作在同一个线程）
   避免了锁竞争和上下文切换

5. 批量处理：
   - 批量读取：一次 read 尽可能多读数据
   - 批量 flush：将多个写请求合并为一个系统调用

6. 无锁化设计：
   每个 Channel 绑定到一个 EventLoop 线程上
   同一个 Channel 的操作不需要加锁
```

---

## 11. Java 各版本演进与企业选型

### 11.1 关键版本特性

| 版本 | 年份 | 关键特性 | 企业影响 |
|------|------|---------|---------|
| **Java 5** | 2004 | 泛型、注解、枚举、自动装箱、for-each | 奠定了现代 Java 语法基础 |
| **Java 6** | 2006 | JVM 性能优化、锁升级 | synchronized 不再是重量级 |
| **Java 7** | 2011 | try-with-resources、NIO.2、String in switch、G1 预览 | 资源管理的革命 |
| **Java 8** | 2014 | **Lambda、Stream、Optional、新日期 API、接口 default 方法** | **改变最大的版本** |
| **Java 11** | 2018 | HTTP Client、ZGC、Flight Recorder、String 新方法 | 模块化成熟 |
| **Java 14** | 2020 | Records(预览)、switch 表达式、有用的 NPE 信息 | 减少样板代码 |
| **Java 17** | 2021 | **Records、密封类、Pattern Matching for instanceof、增强 PRNG** | **当前最主流 LTS** |
| **Java 21** | 2023 | **Virtual Threads、Record Patterns、Pattern Matching for switch** | **下一代主力 LTS** |

### 11.2 JDK 发行版对比

```
企业选 JDK 不只是选版本号，还要选发行版：

Oracle JDK：
  - 官方版本
  - JDK 11 起商用收费（开发/测试免费）
  - 企业使用建议选择 OpenJDK 发行版

OpenJDK 发行版（推荐企业使用）：
  - Eclipse Temurin (Adoptium)   ← 最推荐，Eclipse 基金会维护
  - Amazon Corretto              ← AWS 维护，有长期支持承诺
  - Azul Zulu                    ← Azul 公司，企业支持好
  - 阿里 Dragonwell              ← 阿里维护，内部验证
  - 腾讯 Kona                    ← 腾讯维护
  - 微软 Microsoft Build of OpenJDK  ← 微软维护
  - GraalVM                      ← Oracle 的 AOT 编译器 + polyglot 运行时

企业选择建议：
  - 互联网/普通企业 → Eclipse Temurin（社区驱动，免费）
  - 使用 AWS → Amazon Corretto
  - 需要商业支持 → Azul Zulu（有付费支持计划）
  - 追求极致性能的云原生场景 → GraalVM Native Image
```

---

## 12. 常见面试题深度解析

### Q1: 为什么说 String 是不可变的？如何证明？

```java
// String 源码（简略）
public final class String implements java.io.Serializable, Comparable<String>, CharSequence {
    private final byte[] value;  // JDK 9+ byte[]（之前是 final char[]）
    // final class → 不能被继承
    // final byte[] → 数组引用不可变
    // 所有修改操作返回新 String → "abc".concat("def") 返回新的 String 对象

    // 证明：
    String s = "hello";
    System.out.println(s.hashCode());  // 比如 99162322
    Field field = String.class.getDeclaredField("value");
    field.setAccessible(true);
    byte[] value = (byte[]) field.get(s);
    value[0] = 'H';  // 虽然可以反射修改，但这不是 String 设计的方式
    System.out.println(s.hashCode());  // 哈希码不变！因为 hashCode 缓存了但值变了
    // 这恰恰说明了为什么：不能通过反射修改 — 破坏了约定
}
```

### Q2: hashCode() 和 equals() 的关系？

```java
// 来自 Object 类的约定
// 1. 如果两个对象 equals() 相等，则 hashCode() 必须相等
// 2. 如果两个对象 equals() 不等，hashCode() 不要求不等（但最好不等，减少冲突）
// 3. 如果两个对象 hashCode() 不等，则 equals() 一定不等
// 4. 同一对象多次调用 hashCode()，值不变（前提是 equals 比较的属性没变）

// 典型实现（IDEA 自动生成）
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof User)) return false;
    User user = (User) o;
    return Objects.equals(id, user.id) &&
           Objects.equals(name, user.name);
}

@Override
public int hashCode() {
    return Objects.hash(id, name);  // 基于相同的字段
}
```

### Q3: final、finally、finalize 的区别？

```
final：
  - 修饰类 → 不能被继承（如 String 是 final）
  - 修饰方法 → 不能被子类重写
  - 修饰变量 → 基本类型值不可变，引用类型引用不可变（对象内容可变）
  - 修饰参数 → 方法内不能改变参数引用

finally：
  - try-catch-finally 中的 finally 块
  - 一定会执行（除非 System.exit(0) 或 JVM 崩溃）
  - 用于释放资源（但 try-with-resources 更优）

finalize：
  - Object 类的方法
  - GC 回收对象前调用一次
  - JDK 9 标记为 @Deprecated
  - JDK 18 彻底移除
  - 替代方案：Cleaner（JDK 9）、try-with-resources
```

---

> **下一篇：** [02-构建工具Maven与Gradle详解](./02-构建工具Maven与Gradle详解.md)
