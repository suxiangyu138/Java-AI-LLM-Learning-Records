04.28 09:33
java.util  包核心内容全解
 java.util  是 Java 最常用的工具类核心包，包含集合框架、工具类、日期时间、随机数、数组操作等高频工具，是开发必掌握的基础包。
 
一、集合框架（最常用，开发核心）
1. List 系列（有序、可重复）
-  ArrayList ：动态数组，查询快、增删慢
-  LinkedList ：双向链表，增删快、查询慢
-  Vector ：线程安全的动态数组（性能差，基本不用）
-  Stack ：栈结构（后进先出）
2. Set 系列（无序、不可重复）
-  HashSet ：哈希表实现，查询/去重效率最高
-  LinkedHashSet ：有序+去重，按插入顺序存储
-  TreeSet ：红黑树，自动排序
3. Map 系列（键值对，key唯一）
-  HashMap ：哈希表，存取效率最高
-  LinkedHashMap ：有序键值对
-  TreeMap ：红黑树，key自动排序
-  Hashtable ：线程安全的Map（过时）
-  Properties ：配置文件专用，key/value 都是字符串
4. 队列 Queue
-  ArrayDeque ：双端队列，栈/队列都能用
-  PriorityQueue ：优先队列（堆结构，默认小顶堆）
 
二、工具类（静态方法，直接调用）
-  Collections ：集合工具类，提供排序、反转、线程安全包装等静态方法
-  Arrays ：数组工具类，数组排序、查找、转List、打印等
-  Objects ：对象判空、equals 安全校验
 
三、日期 & 时间类
-  Date ：基础日期时间类
-  Calendar ：日历类，日期运算
-  TimeZone ：时区工具
-  Locale ：国际化语言/地区配置
 
四、其他高频工具类
-  Random ：随机数生成
-  Scanner ：控制台输入读取
-  Optional ：Java8 空指针安全容器
-  UUID ：生成唯一标识符
-  Timer  /  TimerTask ：定时任务
 
五、和你刷题的关联
你之前写的 LeetCode 三角形题目里：
java
import java.util.List;
 
 List  接口就来自  java.util  包，所有集合类都在这个包里。
刷题最常用的：
 List 、 ArrayList 、 HashMap 、 HashSet 、 PriorityQueue 
 
六、极简记忆口诀
集合、工具、时间、随机数，全在 util 包里找

