Java 核心包体系
Java 的包（Package）是类/接口的组织单元，按功能划分为核心基础包、常用工具包、IO网络包、高级功能包四大类，以下是开发/刷题/面试最常用的完整清单。
 
一、java.lang（默认导入，无需手动 import）
Java 最核心的基础包，所有程序默认自动导入，不用写任何  import  语句。
- 基础类型包装类： Integer 、 Long 、 Double 、 Boolean 、 Character 
- 根父类： Object （所有类的父类）
- 字符串： String 、 StringBuilder 、 StringBuffer 
- 系统工具： System 、 Runtime 
- 异常体系： Exception 、 RuntimeException 、 Error 
- 线程相关： Thread 、 Runnable 、 ThreadLocal 
- 数学工具： Math 
 
二、java.util（集合+工具类，刷题/开发高频）
你刚问过的包，集合框架全部在这里，开发使用频率最高。
- 集合框架： List 、 ArrayList 、 LinkedList 、 Set 、 HashSet 、 Map 、 HashMap 、 PriorityQueue 
- 工具类： Collections 、 Arrays 、 Objects 
- 日期时间： Date 、 Calendar 
- 其他： Random 、 Scanner 、 Optional 、 UUID 
 
三、java.io（文件+流操作）
负责文件读写、数据流、序列化。
- 文件操作： File 
- 字节流： InputStream 、 OutputStream 
- 字符流： Reader 、 Writer 
- 缓冲流： BufferedReader 、 BufferedWriter 
- 序列化： Serializable 
 
四、java.net（网络编程）
负责Socket、HTTP、网络通信。
- 基础网络： Socket 、 ServerSocket 
- URL 访问： URL 、 URLConnection 
- 网络地址： InetAddress 
 
五、java.nio（新IO，高性能IO）
Java 1.4 推出的非阻塞IO，性能优于传统 IO。
- 缓冲区： ByteBuffer 
- 通道： Channel 
- 选择器： Selector 
- 文件工具： Files 、 Paths 
 
六、java.time（Java8 全新日期时间）
替代老旧的 Date/Calendar，线程安全、API友好。
- 日期： LocalDate 
- 时间： LocalTime 
- 日期+时间： LocalDateTime 
- 时间戳： Instant 
- 格式化： DateTimeFormatter 
 
七、java.math（高精度数学运算）
处理超大数、高精度小数，避免浮点丢失精度。
- 大整数： BigInteger 
- 高精度小数： BigDecimal 
 
八、java.sql / javax.sql（数据库操作）
JDBC 数据库连接专用包，Java后端必学。
- 核心接口： Connection 、 Statement 、 PreparedStatement 、 ResultSet 
- 数据源： DataSource 
 
九、java.util.concurrent（并发编程）
Java 多线程、并发工具核心包，面试高频考点。
- 线程池： ExecutorService 、 ThreadPoolExecutor 
- 锁： Lock 、 ReentrantLock 
- 并发集合： ConcurrentHashMap 、 CopyOnWriteArrayList 
- 工具类： CountDownLatch 、 CyclicBarrier 、 Semaphore 
 
十、java.lang.reflect（反射）
Java 反射机制，框架底层核心（Spring、MyBatis 全靠它）。
- 核心类： Class 、 Field 、 Method 、 Constructor 
 
刷题必背极简清单
只需要记住这 5 个，刷 LeetCode 完全够用：
1.  java.lang ：默认导入，基础类全在这里
2.  java.util ：集合、数组、随机数
3.  java.io ：文件读写
4.  java.math ：高精度计算
5.  java.util.concurrent ：并发集合、线程池
 
补充：包的使用规则
1. 同一个包下的类，可直接使用
2. 不同包的类，需要  import  导入
3.  java.lang  包永远默认导入，无需手动写 import
