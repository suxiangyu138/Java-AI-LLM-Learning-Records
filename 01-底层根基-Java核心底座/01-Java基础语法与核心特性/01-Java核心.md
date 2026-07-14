# Java 核心

## 📌 课程定位
Java是后端开发的主流语言，理解Java核心机制（JVM、集合、多线程）是后端面试的必备项。学C++理解底层，学Java理解工程化。

## 🎯 核心章节

### 1. Java 基础
- **JVM + JRE + JDK 关系**：JDK(开发工具+JRE) ⊃ JRE(运行环境+核心类库) ⊃ JVM(执行字节码)
- **编译与运行**：`.java` → javac → `.class`(字节码) → JVM解释/即时编译(JIT)
- **基本类型与包装类**：int↔Integer(缓存池-128~127)、自动拆装箱的NPE陷阱
- **String**：不可变(底层final char[]→byte[])、常量池、StringBuilder/StringBuffer(线程安全)
- **equals vs ==**：==比引用地址，equals默认也是比地址(需重写)

### 2. 面向对象（Java特色）
- **访问修饰符**：public/ protected/ default(包级可见)/ private
- **抽象类 vs 接口**：
  | 抽象类 | 接口 |
  |--------|------|
  | 单继承 | 多实现 |
  | 可以有构造器 | 无构造器 |
  | 可以有非抽象方法 | Java8+ default方法 |
  | is-a关系 | can-do能力 |
- **内部类**：成员内部类、静态嵌套类、局部内部类、匿名内部类→Lambda(Java8)
- **泛型**：类型擦除——编译期检查，运行时不保留类型信息。`? extends T`(上界)、`? super T`(下界)

### 3. 集合框架（⭐ 必掌握所有常用容器）
- **Collection 体系**：
  - List：ArrayList(数组，随机O(1))、LinkedList(双向链表，增删O(1))、Vector(线程安全，已淘汰)
  - Set：HashSet(哈希，O(1))、TreeSet(红黑树，有序，O(log n))、LinkedHashSet(插入序)
  - Queue：PriorityQueue(堆)、ArrayDeque(双端队列)
- **Map 体系**：
  - HashMap（⭐ 重点）：数组+链表+红黑树(≥8转树)，负载因子0.75 → JDK7头插法(死循环) → JDK8尾插法
  - TreeMap：红黑树，key有序
  - LinkedHashMap：插入序/访问序(LRU实现)
  - ConcurrentHashMap：JDK7分段锁 → JDK8 CAS+synchronized
- **HashMap 扩容**：2倍扩容，rehash(JDK8优化了高位算法)，初始容量16

### 4. 异常体系
- Throwable → Error(不可恢复，如OOM) + Exception
- Exception → RuntimeException(非受检，如NPE/ArrayIndexOut) + CheckedException(受检，如IOException)
- **try-catch-finally**：return在finally中的陷阱。try-with-resources(AutoCloseable)

### 5. 反射机制
- 运行时获取类的完整信息：Class对象、Field、Method、Constructor
- 应用：Spring IoC、动态代理、注解处理器
- 破坏单例、绕过泛型检查——了解双刃剑

### 6. Java 8+ 新特性
- **Lambda + 函数式接口**：`(a,b) -> a+b`，@FunctionalInterface——Predicate/Function/Consumer/Supplier
- **Stream API**：filter/map/reduce/collect——函数式数据处理
- **Optional**：优雅处理null，避免NPE
- **CompletableFuture**：异步编程，链式回调

## ✅ 学习建议
- HashMap 源码必读（put/get/resize），面试必有
- ArrayList vs LinkedList 选型依据：读多用ArrayList，中间频繁增删用LinkedList
- 《Effective Java》是Java程序员必读——90条建议覆盖最佳实践
