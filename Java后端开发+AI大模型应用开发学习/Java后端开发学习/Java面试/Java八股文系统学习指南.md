03.29 22:39
Java八股文系统学习指南
Java八股文是面试高频考点的汇总，核心围绕「基础语法、集合框架、JVM、多线程、IO、Spring、数据库、分布式」八大模块，以下按模块拆解，重点突出考点、易错点，兼顾理解和记忆，适合面试备考和基础巩固。
一、Java基础语法（必背，入门核心）
1. 基础概念辨析
Java的三大特性：封装、继承、多态。
封装：隐藏对象内部细节，通过get/set方法暴露接口，提高安全性（如实体类的私有属性）。
继承：子类继承父类的非私有属性和方法，减少代码冗余，子类可重写父类方法（extends关键字，Java单继承、多实现）。
多态：同一方法在不同对象上有不同实现，核心是「父类引用指向子类对象」，分为编译时多态（方法重载）和运行时多态（方法重写）。
重载（Overload）vs 重写（Override）：
重载：同一类中，方法名相同，参数列表（个数、类型、顺序）不同，与返回值、修饰符无关（编译时确定）。
重写：子类继承父类后，方法名、参数列表、返回值（子类返回值可小于等于父类）完全一致，修饰符权限不能低于父类（运行时确定），不能重写final、static方法。
final、static、abstract关键字：
final：修饰类（不能被继承，如String）、方法（不能被重写）、变量（常量，初始化后不能修改，基本类型值不变，引用类型地址不变）。
static：修饰变量（类变量，属于类，全局唯一，优先于对象加载）、方法（类方法，不能调用非static变量/方法，无this指针）、代码块（静态代码块，类加载时执行，只执行一次）。
abstract：修饰类（抽象类，不能实例化，必须被继承）、方法（抽象方法，无方法体，子类必须重写，除非子类也是抽象类）。
2. 数据类型与包装类
基本数据类型（8种）：byte（1字节）、short（2）、int（4）、long（8）、float（4）、double（8）、char（2）、boolean（1）。
包装类：基本类型的封装类，用于泛型、集合（集合只能存引用类型），如Integer、Long、Boolean等（除char对应Character、int对应Integer，其余都是首字母大写）。
自动装箱与拆箱：
装箱：基本类型→包装类（如int → Integer，JDK1.5后自动完成，底层调用valueOf()）。
拆箱：包装类→基本类型（如Integer → int，自动调用intValue()）。
易错点：Integer缓存池（-128~127），在此范围内的Integer对象复用，超出范围则新建对象（如Integer a=127，b=127，a==b为true；a=128，b=128，a==b为false，需用equals()判断）。
3. 字符串相关（高频考点）
String、StringBuffer、StringBuilder区别：
String：不可变字符串（底层是char数组，被final修饰），每次修改都会新建对象，效率低，适合少量、不频繁修改的场景。
StringBuffer：可变字符串，线程安全（方法加synchronized），效率中等，适合多线程环境下的字符串拼接。
StringBuilder：可变字符串，线程不安全，效率最高，适合单线程环境下的字符串拼接（日常开发首选）。
String常用方法：equals()（判断内容相等，区分大小写）、equalsIgnoreCase()（忽略大小写）、length()（长度）、charAt()（获取指定索引字符）、substring()（截取字符串）、replace()（替换）、split()（分割）、trim()（去除首尾空格）。
易错点：String str = "abc" 和 String str = new String("abc") 的区别：前者存在字符串常量池，后者新建对象（堆内存），== 判断地址，equals判断内容。
二、Java集合框架（核心，面试必问）
集合框架核心：Collection（单列集合）和Map（双列集合），重点掌握List、Set、Map的实现类、底层结构、优缺点及使用场景。
1. Collection接口（单列集合）
（1）List接口（有序、可重复、有索引）
ArrayList（底层：动态数组）：
优点：查询快（通过索引直接访问），遍历效率高。
缺点：增删慢（需要移动数组元素，尤其在中间位置），线程不安全。
扩容机制：初始容量10，扩容时按1.5倍扩容（oldCapacity + (oldCapacity >> 1)），底层是Object[]数组。
LinkedList（底层：双向链表）：
优点：增删快（只需修改链表指针，无需移动元素），适合频繁增删的场景。
缺点：查询慢（需要从头/尾遍历查找索引），线程不安全。
Vector（底层：动态数组）：线程安全（方法加synchronized），效率低，扩容时按2倍扩容，已被ArrayList替代，日常开发很少用。
（2）Set接口（无序、不可重复、无索引）
HashSet（底层：HashMap的key，哈希表）：
优点：增删查效率高（O(1)），线程不安全。
去重原理：先通过hashCode()计算哈希值，哈希值不同则直接存；哈希值相同，再通过equals()判断内容，内容不同则存（哈希冲突，用链表/红黑树解决）。
易错点：存储对象时，必须重写hashCode()和equals()，否则无法正确去重。
LinkedHashSet（底层：LinkedHashMap的key）：有序（保留插入顺序），不可重复，效率略低于HashSet，底层用链表维护插入顺序。
TreeSet（底层：红黑树）：有序（自然排序/自定义排序），不可重复，查询效率O(logn)，适合需要排序的场景（如按年龄排序），底层通过Comparable/Comparator接口实现排序。
2. Map接口（双列集合，key-value映射，key唯一，value可重复）
HashMap（底层：哈希表，JDK1.8后：数组+链表+红黑树）：
核心考点：扩容机制、哈希冲突解决、线程安全问题。
初始容量16，负载因子0.75，当元素个数超过16*0.75=12时，触发扩容，扩容为原来的2倍。
哈希冲突：当两个key的hashCode()相同，用链表存储；当链表长度超过8，且数组长度≥64时，链表转为红黑树；当链表长度≤6时，红黑树转回链表（减少内存消耗）。
线程安全：不安全，多线程环境下可能出现死循环（JDK1.7及之前）、数据错乱，解决方案：使用ConcurrentHashMap，或Collections.synchronizedMap()。
HashTable（底层：哈希表）：线程安全（方法加synchronized），效率低，key和value都不能为null，已被ConcurrentHashMap替代。
ConcurrentHashMap（底层：JDK1.8后：数组+链表+红黑树，CAS+ synchronized）：
线程安全，效率高于HashTable，底层用CAS保证原子性，对链表/红黑树的节点加synchronized，而非全局锁，支持高并发。
JDK1.7与1.8区别：1.7底层是分段锁（Segment），1.8取消分段锁，用CAS+节点锁优化性能。
TreeMap（底层：红黑树）：key有序（自然排序/自定义排序），key不能为null，适合需要按key排序的场景。
三、JVM（Java虚拟机，核心难点）
JVM核心考点：内存结构、垃圾回收（GC）、类加载机制、调优参数，是中高级面试的重点。
1. JVM内存结构（JDK1.8）
方法区（Method Area）：
存储：类信息（类名、方法、字段）、常量池、静态变量、即时编译后的代码。
JDK1.8前：方法区是永久代（PermGen），受JVM内存限制；JDK1.8后：永久代取消，改为元空间（Metaspace），元空间使用本地内存，不受JVM内存限制，避免OOM。
堆（Heap）：
存储：对象实例、数组，是GC的主要区域（垃圾回收的核心战场）。
分区：年轻代（Young Generation）+ 老年代（Old Generation），比例默认1:2。
年轻代：分为Eden区（80%）、From Survivor区（10%）、To Survivor区（10%），对象优先在Eden区创建，Eden满了触发Minor GC（轻GC），存活对象进入Survivor区；Survivor区满了，存活对象进入老年代。
老年代：存储存活时间长的对象（默认存活15次Minor GC后进入老年代），老年代满了触发Major GC（重GC），Major GC会伴随Minor GC，效率低。
程序计数器（Program Counter Register）：
存储：当前线程执行的字节码指令地址，线程私有（每个线程有独立的程序计数器），不会出现OOM。
虚拟机栈（VM Stack）：
存储：栈帧（每个方法调用对应一个栈帧，包含局部变量表、操作数栈、返回地址等），线程私有。
异常：栈深度超过限制（如递归调用过深），抛出StackOverflowError；栈内存不足，抛出OutOfMemoryError。
本地方法栈（Native Method Stack）：
存储：本地方法（native修饰的方法，如Object的hashCode()）的调用栈，线程私有，可能抛出StackOverflowError和OOM。
2. 垃圾回收（GC）
（1）垃圾判断标准
引用计数法：给对象加引用计数器，引用一次+1，引用失效-1，计数器为0则为垃圾；缺点：无法解决循环引用（如A引用B，B引用A，计数器都不为0，但实际无可用引用）。
可达性分析算法（JVM实际使用）：以“GC Roots”为起点，遍历对象引用链，无法到达的对象即为垃圾。GC Roots包括：虚拟机栈中的局部变量、方法区中的静态变量、本地方法栈中的本地方法引用等。
（2）垃圾回收算法
标记-清除算法（Mark-Sweep）：先标记垃圾对象，再清除垃圾；优点：简单，缺点：产生内存碎片，后续分配大对象时可能无法找到连续内存。
标记-复制算法（Mark-Copy）：将内存分为两块，只使用一块，标记垃圾后，将存活对象复制到另一块，清除原块垃圾；优点：无内存碎片，缺点：内存利用率低（只使用一半），适合年轻代（存活对象少）。
标记-整理算法（Mark-Compact）：标记垃圾后，将存活对象移动到内存一端，再清除垃圾；优点：无内存碎片，内存利用率高，缺点：移动对象耗时，适合老年代（存活对象多）。
分代收集算法（JVM实际使用）：根据对象存活时间，将堆分为年轻代和老年代，年轻代用标记-复制算法，老年代用标记-整理/标记-清除算法，兼顾效率和内存利用率。
（3）垃圾回收器（GC收集器）
年轻代收集器：Serial GC（串行GC，单线程，效率低，适合小内存）、ParNew GC（并行GC，多线程，配合老年代CMS使用）、G1 GC（区域化分代，兼顾年轻代和老年代）。
老年代收集器：CMS GC（并发标记清除，低延迟，适合高并发场景，缺点：内存碎片、CPU消耗高）、Serial Old GC（串行老年代GC，单线程）、G1 GC（可处理老年代，无内存碎片）。
最新收集器：ZGC、Shenandoah GC（低延迟，大内存场景，如100G以上内存）。
3. 类加载机制
类加载流程（5步，不可逆）：加载 → 验证 → 准备 → 解析 → 初始化。
加载：通过类加载器（ClassLoader）将.class文件加载到内存，生成Class对象。
验证：校验.class文件的合法性（如文件格式、语法、语义），防止恶意文件。
准备：为类的静态变量分配内存，设置默认初始值（如int默认0，String默认null），不执行赋值语句。
解析：将符号引用（如类名、方法名）转为直接引用（内存地址）。
初始化：执行静态代码块、静态变量赋值语句，是类加载的最后一步，只有当类被主动使用时才会触发（如new对象、调用静态方法、访问静态变量等）。
类加载器层级（双亲委派模型）：
Bootstrap ClassLoader（启动类加载器）：最顶层，加载JDK核心类（如rt.jar），由C++实现，无法通过Java代码获取。
Extension ClassLoader（扩展类加载器）：加载JDK扩展类（如jre/lib/ext目录下的类）。
Application ClassLoader（应用类加载器）：加载项目中的类（classpath下的类），是默认的类加载器。
双亲委派机制：加载类时，先委托父类加载器加载，父类加载不了，再由子类加载器自己加载；优点：避免类重复加载，保证核心类的安全性（如不能自定义java.lang.String类）。
四、Java多线程（核心，高频面试）
多线程核心：线程创建、线程状态、线程同步、线程池、并发工具类，重点掌握同步机制和线程池。
1. 线程创建方式（4种）
继承Thread类：重写run()方法，调用start()方法启动线程（start()会调用JVM的start0()方法，开启新线程；直接调用run()只是普通方法调用，不会开启新线程）。
实现Runnable接口：重写run()方法，将Runnable对象传入Thread类，调用start()启动；优点：避免单继承的限制，可多实现。
实现Callable接口：重写call()方法，有返回值，可抛出异常，配合FutureTask使用（FutureTask可获取返回值、判断线程是否执行完成）。
线程池创建：通过Executors工具类或ThreadPoolExecutor创建（推荐用ThreadPoolExecutor，可自定义参数，避免Executors的潜在风险）。
2. 线程状态（6种，JDK1.5后）
新建（New）：线程对象创建后，未调用start()方法。
就绪（Runnable）：调用start()方法后，线程进入就绪队列，等待CPU调度。
运行（Running）：CPU调度线程后，线程执行run()方法。
阻塞（Blocked）：线程等待锁（如synchronized未获取到锁），进入阻塞状态，释放CPU。
等待（Waiting）：线程调用wait()方法（无超时），进入等待状态，需其他线程调用notify()/notifyAll()唤醒。
超时等待（Timed Waiting）：线程调用wait(long timeout)、sleep(long timeout)等方法，进入超时等待状态，超时后自动唤醒，或被其他线程唤醒。
终止（Terminated）：线程执行完run()方法，或抛出未捕获的异常，线程终止。
3. 线程同步机制（解决线程安全问题）
（1）synchronized关键字（重量级锁，JDK1.8优化后有偏向锁、轻量级锁）
作用：保证同一时刻只有一个线程执行同步代码块，解决线程安全问题（如多线程卖票、转账）。
使用场景：修饰方法（锁当前对象this）、修饰静态方法（锁当前类的Class对象）、修饰代码块（锁指定对象，如synchronized(obj)）。
锁升级过程：无锁 → 偏向锁（单线程多次获取锁，减少锁开销） → 轻量级锁（多线程竞争不激烈，用CAS自旋） → 重量级锁（多线程竞争激烈，阻塞线程，依赖操作系统内核）。
（2）Lock锁（java.util.concurrent.locks，轻量级锁，手动控制）
核心实现类：ReentrantLock（可重入锁，默认非公平锁，可设置为公平锁）、ReentrantReadWriteLock（读写锁，读锁共享，写锁独占，提高读操作效率）。
优点：可手动获取和释放锁（lock()获取，unlock()释放，需在finally中释放，避免死锁）、可中断锁、可超时获取锁、支持公平锁/非公平锁，比synchronized灵活。
synchronized vs Lock：
synchronized：自动释放锁，无需手动操作；Lock：手动释放锁，必须在finally中释放。
synchronized：不可中断、不可超时；Lock：可中断、可超时获取锁。
synchronized：非公平锁；Lock：可设置公平锁/非公平锁。
synchronized：适合简单场景；Lock：适合复杂场景（如多线程读写、超时获取锁）。
（3）其他同步工具
volatile关键字：保证变量的可见性（一个线程修改变量后，其他线程立即可见）、禁止指令重排序，但不保证原子性（如i++，不是原子操作，需配合synchronized或CAS使用）。
CAS（Compare and Swap）：无锁机制，通过比较并交换实现原子操作（如AtomicInteger），底层依赖CPU的CAS指令，优点：无锁，效率高；缺点：ABA问题（可通过AtomicStampedReference解决）、循环自旋消耗CPU。
ThreadLocal：线程本地变量，每个线程有独立的变量副本，避免线程安全问题（如SimpleDateFormat线程不安全，用ThreadLocal封装）；注意：线程池环境下，ThreadLocal可能导致内存泄漏（需手动remove()）。
4. 线程池（核心，面试高频）
线程池核心作用：复用线程，减少线程创建和销毁的开销，控制线程数量，避免线程过多导致CPU过载、内存溢出。
ThreadPoolExecutor核心参数（7个）：
corePoolSize：核心线程数（线程池长期保持的线程数，即使空闲也不销毁）。
maximumPoolSize：最大线程数（线程池能容纳的最大线程数）。
keepAliveTime：空闲线程的存活时间（核心线程除外，空闲超过该时间则销毁）。
unit：keepAliveTime的时间单位（如TimeUnit.SECONDS）。
workQueue：任务队列（存放等待执行的任务，如ArrayBlockingQueue、LinkedBlockingQueue）。
threadFactory：线程工厂（用于创建线程，可自定义线程名称）。
handler：拒绝策略（当线程池满、任务队列满时，处理新任务的策略），共4种：
AbortPolicy（默认）：抛出RejectedExecutionException异常，拒绝新任务。
CallerRunsPolicy：由调用线程（提交任务的线程）执行新任务。
DiscardPolicy：默默丢弃新任务，不抛出异常。
DiscardOldestPolicy：丢弃任务队列中最老的任务，再提交新任务。
线程池执行流程：提交任务 → 核心线程未满，创建核心线程执行任务 → 核心线程满，任务放入队列 → 队列满，创建非核心线程执行任务 → 非核心线程满（达到最大线程数），执行拒绝策略。
易错点：Executors工具类创建线程池的风险（如FixedThreadPool、CachedThreadPool可能导致OOM），推荐用ThreadPoolExecutor自定义参数，根据业务场景设置核心线程数、队列大小。
五、Java IO（输入输出，基础考点）
IO核心：字节流、字符流，重点掌握流的分类、常用实现类、NIO的核心思想。
1. IO流分类
按操作数据类型：字节流（InputStream/OutputStream，处理所有类型数据，如图片、视频）、字符流（Reader/Writer，处理文本数据，按字符编码读取）。
按流向：输入流（读数据，如FileInputStream、FileReader）、输出流（写数据，如FileOutputStream、FileWriter）。
按是否缓冲：缓冲流（BufferedInputStream、BufferedReader，增加缓冲，提高读写效率，需关闭外层流）、非缓冲流（直接操作文件，效率低）。
2. 常用IO流实现类
字节流：
FileInputStream/FileOutputStream：读取/写入文件（字节）。
BufferedInputStream/BufferedOutputStream：缓冲字节流，提高读写效率。
DataInputStream/DataOutputStream：读取/写入基本数据类型（如int、double）。
字符流：
FileReader/FileWriter：读取/写入文本文件（字符），默认编码（如GBK），可能出现乱码。
BufferedReader/BufferedWriter：缓冲字符流，支持readLine()（读取一行）、newLine()（换行）。
InputStreamReader/OutputStreamWriter：转换流，将字节流转为字符流，可指定编码（如UTF-8），解决乱码问题。
3. NIO（New IO，JDK1.4引入）
核心思想：面向缓冲区（Buffer）、非阻塞IO（Non-blocking IO），基于Selector（选择器）实现一个线程管理多个通道（Channel），提高并发效率。
核心组件：
Buffer：缓冲区，存储数据（如ByteBuffer、CharBuffer），核心方法：put()（存数据）、get()（取数据）、flip()（切换读/写模式）、clear()（清空缓冲区）。
Channel：通道，双向传输数据（可读可写），如FileChannel（文件通道）、SocketChannel（Socket通道）。
Selector：选择器，监听多个Channel的事件（如连接、读、写），一个线程可处理多个Channel的事件，实现非阻塞IO。
IO vs NIO：IO是面向流、阻塞的；NIO是面向缓冲区、非阻塞的，适合高并发场景（如服务器端）。
六、Spring框架（主流框架，面试必问）
Spring核心：IOC（控制反转）、AOP（面向切面编程），延伸Spring Boot、Spring Cloud，重点掌握IOC、AOP的原理和使用。
1. IOC（控制反转，Inversion of Control）
核心思想：将对象的创建、依赖注入交给Spring容器管理，而非手动new对象，降低代码耦合度。
DI（依赖注入，Dependency Injection）：IOC的具体实现，Spring容器将依赖的对象注入到需要的类中（如通过构造器、setter方法、注解注入）。
Bean的核心概念：Spring容器管理的对象称为Bean，Bean的生命周期：实例化 → 属性注入 → 初始化 → 销毁。
Bean的注入方式：
构造器注入：通过构造方法注入依赖，推荐使用（避免循环依赖）。
setter方法注入：通过setter方法注入依赖，灵活但易出现循环依赖。
注解注入：@Autowired（按类型注入）、@Qualifier（按名称注入，配合@Autowired使用）、@Resource（按名称注入，JDK注解）。
Bean的作用域（默认singleton）：
singleton：单例，整个Spring容器中只有一个Bean实例（默认）。
prototype：多例，每次获取Bean都创建新实例。
request：每个HTTP请求创建一个Bean（Web场景）。
session：每个HTTP会话创建一个Bean（Web场景）。
2. AOP（面向切面编程，Aspect-Oriented Programming）
核心思想：将通用功能（如日志、事务、权限校验）抽取为切面（Aspect），在不修改业务代码的前提下，通过动态代理织入到业务方法中，实现解耦。
AOP核心术语：
切面（Aspect）：通用功能的封装类（如日志切面）。
连接点（JoinPoint）：业务方法中可以被织入切面的点（如方法执行前、执行后）。
切入点（Pointcut）：指定哪些连接点需要被织入切面（如所有service层的方法）。
通知（Advice）：切面的具体逻辑（如日志的打印），分为5种：
前置通知（@Before）：方法执行前执行。
后置通知（@After）：方法执行后执行（无论是否异常）。
返回通知（@AfterReturning）：方法正常返回后执行。
异常通知（@AfterThrowing）：方法抛出异常后执行。
环绕通知（@Around）：方法执行前后都执行，可控制方法的执行（最强大）。
织入（Weaving）：将切面的通知织入到切入点的过程（Spring默认动态代理织入）。
AOP实现原理：Spring AOP基于动态代理，分为两种：
JDK动态代理：基于接口，只能代理实现了接口的类，生成接口的代理对象。
CGLIB动态代理：基于继承，可代理未实现接口的类，生成目标类的子类作为代理对象。
3. Spring Boot核心
核心思想：约定优于配置（Convention Over Configuration），简化Spring配置，快速搭建项目。
核心特性：自动配置（AutoConfiguration）、起步依赖（Starter）、嵌入式容器（Tomcat、Jetty）、无XML配置。
自动配置原理：Spring Boot启动时，加载META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports文件，根据类路径下的依赖，自动配置对应的Bean（如引入spring-boot-starter-web，自动配置Tomcat、Spring MVC）。
常用注解：@SpringBootApplication（组合注解，包含@SpringBootConfiguration、@EnableAutoConfiguration、@ComponentScan）、@RestController（@Controller + @ResponseBody，返回JSON数据）、@RequestMapping（映射请求路径）、@Autowired（依赖注入）。
七、数据库（MySQL为主，核心考点）
数据库核心：SQL语法、索引、事务、锁、优化，重点掌握索引和事务。
1. SQL语法（基础，必背）
DDL（数据定义语言）：CREATE（创建表）、ALTER（修改表）、DROP（删除表）、TRUNCATE（清空表，不可回滚）。
DML（数据操纵语言）：INSERT（插入）、UPDATE（修改）、DELETE（删除，可回滚）、SELECT（查询）。
DCL（数据控制语言）：GRANT（授权）、REVOKE（回收权限）。
常用查询：GROUP BY（分组）、ORDER BY（排序）、LIMIT（分页）、JOIN（关联查询，内连接、左连接、右连接）、WHERE（条件查询）。
易错点：TRUNCATE和DELETE的区别：TRUNCATE清空表，不记录日志，不可回滚，效率高；DELETE删除数据，记录日志，可回滚，效率低。
2. 索引（核心，面试高频）
核心作用：提高查询效率，降低数据库IO开销（类似书的目录）。
索引类型：
主键索引（PRIMARY KEY）：唯一标识表中的记录，不可为null，一个表只能有一个主键索引。
唯一索引（UNIQUE）：索引列的值唯一，可为null，一个表可多个唯一索引。
普通索引（INDEX）：无约束，仅用于提高查询效率，可多个。
联合索引（复合索引）：多个列组成的索引，遵循“最左前缀原则”（查询时，必须使用联合索引的最左列，否则索引失效）。
全文索引（FULLTEXT）：用于文本搜索，适合大文本字段（如content）。
索引底层结构（MySQL InnoDB）：B+树（平衡二叉树的优化版），特点：
叶子节点存储数据（主键索引叶子节点存整行数据，普通索引叶子节点存主键值）。
非叶子节点只存索引值，用于导航。
所有叶子节点用链表连接，便于范围查询。
索引失效场景（重点）：
索引列使用函数（如WHERE SUBSTR(name,1,3) = 'abc'）。
索引列使用运算符（如WHERE age + 1 = 10）。
索引列使用模糊查询（如WHERE name LIKE '%abc'，%在开头）。
联合索引不遵循最左前缀原则。
WHERE子句中用OR连接，其中一个列无索引。
3. 事务（ACID特性，必背）
事务：一组SQL操作，要么全部执行成功，要么全部执行失败（如转账：扣钱和加钱必须同时成功或同时失败）。
ACID特性（核心）：
原子性（Atomicity）：事务是一个不可分割的整体，要么全部执行，要么全部回滚。
一致性（Consistency）：事务执行前后，数据库的完整性约束不变（如转账前总金额=转账后总金额）。
隔离性（Isolation）：多个事务并发执行时，事务之间相互隔离，互不影响。
持久性（Durability）：事务执行成功后，数据永久保存到数据库，即使数据库崩溃也不会丢失。
事务隔离级别（MySQL默认Repeatable Read）：
Read Uncommitted（读未提交）：最低级别，可读取未提交的事务数据，会出现脏读、不可重复读、幻读。
Read Committed（读已提交）：可读取已提交的事务数据，避免脏读，会出现不可重复读、幻读（Oracle默认）。
Repeatable Read（可重复读）：同一事务中，多次读取同一数据结果一致，避免脏读、不可重复读，会出现幻读（MySQL默认，InnoDB通过MVCC解决幻读）。
Serializable（串行化）：最高级别，事务串行执行，避免所有问题，效率最低，适合并发量低的场景。
事务并发问题：
脏读：读取到未提交的事务数据，该数据可能被回滚，导致读取的数据无效。
不可重复读：同一事务中，多次读取同一数据，结果不一致（被其他事务修改并提交）。
幻读：同一事务中，多次查询同一条件，结果行数不一致（被其他事务插入/删除数据）。
4. 锁机制（MySQL InnoDB）
行锁：锁定单行数据，粒度细，并发度高，适合读多写少场景（InnoDB默认行锁）。
表锁：锁定整个表，粒度粗，并发度低，适合写多读少场景（MyISAM默认表锁）。
间隙锁：锁定一个范围的间隙（如WHERE age BETWEEN 10 AND 20，锁定10~20之间的间隙），防止幻读，InnoDB在Repeatable Read级别下会自动添加间隙锁。
八、分布式（中高级面试，延伸考点）
分布式核心：分布式事务、分布式锁、负载均衡、服务注册与发现，重点掌握分布式事务和分布式锁。
1. 分布式事务
问题背景：分布式系统中，多个服务操作不同的数据库，需要保证所有操作要么全部成功，要么全部失败（如订单服务和库存服务，下单时需扣库存、创建订单，两者必须同步）。
常用解决方案：
2PC（两阶段提交）：分为准备阶段和提交阶段，由协调者管理参与者；优点：简单，缺点：阻塞、单点故障（协调者故障）、数据不一致。
TCC（Try-Confirm-Cancel）：分为尝试、确认、取消三个阶段，自定义业务逻辑；优点：无阻塞、高可用，缺点：开发成本高，需手动实现三个阶段。
SAGA模式：将分布式事务拆分为多个本地事务，每个本地事务执行后，通过补偿事务回滚；优点：高可用、无阻塞，缺点：数据一致性弱（最终一致性）。
本地消息表：每个服务操作后，将消息存入本地消息表，通过消息队列异步通知其他服务，失败则重试；优点：实现简单，缺点：耦合度高。
事务消息（RocketMQ、Kafka）：基于消息队列的事务机制，保证消息的原子性和一致性；优点：解耦、高可用，缺点：依赖消息队列。
2. 分布式锁
问题背景：分布式系统中，多个服务竞争同一资源（如秒杀商品），需要保证同一时刻只有一个服务能操作资源，避免数据错乱。
常用实现方案：
Redis分布式锁：基于SET NX EX命令（SET key value NX EX 30，不存在则设置，过期时间30秒），配合Lua脚本保证原子性；优点：高性能、高可用，缺点：需处理锁超时、重入、释放别人的锁等问题（推荐用Redisson框架，自动处理这些问题）。
ZooKeeper分布式锁：基于临时有序节点，利用ZK的Watcher机制实现锁的获取和释放；优点：可靠性高、支持重入，缺点：性能略低于Redis。
数据库分布式锁：基于数据库表（如lock_table），通过insert、update操作实现锁；优点：实现简单，缺点：性能低、易出现死锁。
3. 其他分布式考点
负载均衡：将请求分发到多个服务节点，提高系统可用性和并发量，分为客户端负载均衡（如Ribbon）和服务端负载均衡（如Nginx）。
服务注册与发现：服务启动时，将自身信息注册到注册中心（如Eureka、Nacos、Consul），其他服务从注册中心获取服务信息，实现服务调用。
CAP理论：分布式系统中，一致性（Consistency）、可用性（Availability）、分区容错性（Partition Tolerance）三者不可兼得，只能满足其中两个；分布式系统必须满足分区容错性，因此通常在一致性和可用性之间权衡（如Nacos支持AP和CP模式）。
九、学习建议（面试备考重点）
基础优先：先掌握Java基础、集合、多线程、JVM，这是所有考点的核心，也是入门的关键。
重点突破：Spring（IOC、AOP）、MySQL（索引、事务）是面试高频，必须吃透原理和易错点。
结合实践：每学一个知识点，结合代码练习（如手写线程池、实现单例模式、编写Spring AOP切面），避免死记硬背。
总结易错点：将每个模块的易错点、高频考点整理成笔记，反复记忆（如Integer缓存池、索引失效场景、线程安全问题）。
循序渐进：从基础到进阶，先掌握初级考点（如基础语法、集合使用），再攻克难点（如JVM调优、分布式事务）。

