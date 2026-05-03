03.17 19:36
Java集合知识点完整版（面试+实战）
Java集合是Java开发中用于存储、管理一组数据的核心工具，核心作用是替代数组（解决数组固定长度、类型单一的弊端），分为两大体系：Collection接口（存储单个元素）和Map接口（存储键值对），以下从核心体系、常用类、核心特性、面试高频考点四个维度，全面梳理，兼顾基础与实战。
一、Java集合核心体系（顶层架构）
Java集合的顶层接口为 Collection 和 Map，二者无继承关系，共同构成Java集合的核心骨架，体系结构如下（重点记忆）：
1. Collection接口（存储单个元素，核心子接口2个）
List接口：有序、可重复、有索引，支持随机访问，核心实现类：ArrayList、LinkedList、Vector；
Set接口：无序、不可重复（底层依赖equals()和hashCode()），核心实现类：HashSet、LinkedHashSet、TreeSet；
补充：Queue接口（队列，子接口，遵循FIFO先进先出），实现类：LinkedList（兼作队列）、PriorityQueue（优先级队列）。
2. Map接口（存储键值对，key唯一，value可重复）
核心实现类：HashMap、LinkedHashMap、TreeMap、Hashtable，无继承Collection接口，独立存在。
3. 核心注意点
集合只能存储引用类型（如String、Integer），不能存储基本类型（int、char等），需用包装类（Integer、Character）；
所有集合类都位于 java.util包下，使用时需导入；
集合的“有序/无序”：有序指元素的存储顺序与遍历顺序一致，并非排序（如ArrayList有序，HashSet无序）。
二、Collection体系核心实现类（重点掌握）
重点掌握List和Set的核心实现类，明确各自的底层结构、优缺点及适用场景，是面试高频考点。
（一）List接口核心实现类
共性：有序、可重复、有索引，支持add()、remove()、get()、set()等方法，区别在于底层结构不同。
1. ArrayList（最常用）
底层结构：动态数组（数组扩容机制），初始容量10，扩容时默认扩大为原来的1.5倍；
核心特性：查询快（通过索引直接访问，时间复杂度O(1)），增删慢（需移动数组元素，时间复杂度O(n)）；
线程安全：非线程安全（多线程环境下需手动加锁，或使用Collections.synchronizedList()）；
适用场景：查询频繁、增删较少的场景（如展示列表、数据查询）。
2. LinkedList
底层结构：双向链表（每个节点存储前后节点引用）；
核心特性：查询慢（需遍历链表，时间复杂度O(n)），增删快（只需修改节点引用，时间复杂度O(1)）；
线程安全：非线程安全；
适用场景：增删频繁、查询较少的场景（如队列、栈、消息队列）。
3. Vector（过时，了解即可）
底层结构：动态数组，与ArrayList类似；
核心特性：线程安全（所有方法加了synchronized锁），但效率极低；
注意：现在已被ArrayList替代，多线程场景优先用ConcurrentArrayList（java.util.concurrent包），而非Vector。
（二）Set接口核心实现类
共性：无序、不可重复，不支持索引（无get()方法），判断元素是否重复依赖 equals() 和 hashCode() 方法。
1. HashSet（最常用）
底层结构：哈希表（数组+链表/红黑树，JDK 8后优化）；
核心特性：无序（存储顺序≠遍历顺序）、不可重复，查询/增删效率高（时间复杂度O(1)）；
去重原理：先通过hashCode()计算元素哈希值，哈希值不同则元素不同；哈希值相同，再通过equals()判断是否为同一元素；
线程安全：非线程安全；
适用场景：无需保证顺序、需去重的场景（如存储唯一标识、去重数据）。
2. LinkedHashSet
底层结构：哈希表+双向链表（继承HashSet，额外维护链表保证顺序）；
核心特性：有序（存储顺序=遍历顺序）、不可重复，效率略低于HashSet；
适用场景：需去重且保证存储顺序的场景。
3. TreeSet
底层结构：红黑树（平衡二叉树，自动排序）；
核心特性：无序（存储顺序≠遍历顺序）、不可重复，自动排序（默认自然排序，如Integer升序、String字典序）；
排序方式：可自定义排序（实现Comparator接口）；
适用场景：需去重且需要排序的场景（如排行榜、有序去重数据）。
三、Map体系核心实现类（重点掌握）
Map存储键值对（key-value），key唯一（重复会覆盖），value可重复，核心方法：put(key,value)、get(key)、remove(key)、containsKey(key)等。
1. HashMap（最常用）
底层结构：哈希表（数组+链表/红黑树，JDK 8优化）；
核心特性：key无序、唯一，value可重复，查询/增删效率高（O(1)）；
关键细节： - 初始容量16，扩容因子0.75（当元素数量达到容量×0.75时，扩容为原来的2倍）； - key可为null（仅允许一个null key），value可为null； - 去重原理：与HashSet一致（依赖key的hashCode()和equals()）；
线程安全：非线程安全；
适用场景：绝大多数键值对存储场景（如配置信息、缓存）。
2. LinkedHashMap
底层结构：哈希表+双向链表（继承HashMap，额外维护链表保证顺序）；
核心特性：key有序（存储顺序=遍历顺序）、唯一，value可重复，效率略低于HashMap；
适用场景：需保证键值对存储顺序的场景（如LRU缓存的底层实现）。
3. TreeMap
底层结构：红黑树（自动排序）；
核心特性：key有序（自动排序，默认自然排序，可自定义Comparator）、唯一，value可重复；
注意：key不能为null；
适用场景：需排序的键值对场景（如有序映射、排行榜）。
4. Hashtable（过时，了解即可）
底层结构：哈希表，与HashMap类似；
核心特性：线程安全（所有方法加synchronized锁），效率低；key和value都不能为null；
替代方案：多线程场景用ConcurrentHashMap（效率高于Hashtable）。
四、Java集合核心特性与注意事项（实战避坑）
线程安全问题： - 非线程安全集合（常用）：ArrayList、LinkedList、HashSet、LinkedHashSet、HashMap、LinkedHashMap、TreeMap； - 线程安全集合：Vector、Hashtable（效率低）、ConcurrentArrayList、ConcurrentHashMap（推荐，JUC包下）；
集合遍历方式： - List：for循环（索引）、增强for循环、迭代器（Iterator）； - Set：增强for循环、迭代器（无索引，不能用普通for循环）； - Map：keySet()（遍历key）、entrySet()（遍历key-value，推荐）、values()（遍历value）；
去重与排序注意： - Set/Map去重：必须重写元素（key）的equals()和hashCode()方法（二者要一致，hashCode相同，equals必须相同）； - TreeSet/TreeMap排序：元素（key）需实现Comparable接口，或创建时传入Comparator；
空指针避坑：避免用null作为集合元素（尤其是TreeMap、TreeSet），避免用集合的get()方法直接赋值（需先判断是否存在）；
集合与数组转换： - 集合转数组：list.toArray()； - 数组转集合：Arrays.asList(数组)（注意：返回的集合不可修改，需重新new ArrayList<>()包装）。
五、面试高频考点（必背）
1. ArrayList和LinkedList的区别？
底层结构：ArrayList是动态数组，LinkedList是双向链表；
效率：ArrayList查询快（O(1)）、增删慢（O(n)）；LinkedList查询慢（O(n)）、增删快（O(1)）；
内存占用：ArrayList占用连续内存，LinkedList每个节点需存储前后引用，内存占用更高；
线程安全：均为非线程安全。
2. HashMap和Hashtable的区别？
线程安全：HashMap非线程安全，Hashtable线程安全（效率低）；
null值：HashMap允许key和value为null（key仅一个），Hashtable不允许；
底层优化：JDK 8后HashMap引入红黑树，Hashtable无；
扩容机制：HashMap初始容量16，扩容2倍；Hashtable初始容量11，扩容2倍+1。
3. HashSet的去重原理？
底层依赖HashMap实现（HashSet的value是一个固定对象），去重逻辑：
调用元素的hashCode()方法，计算哈希值，确定元素在哈希表中的位置；
若该位置无元素，直接存入；
若该位置有元素，调用equals()方法比较两个元素，相等则去重（不存入），不相等则存入（链表/红黑树）。
4. HashMap的底层实现（JDK 8）？
底层是“数组+链表+红黑树”的组合结构：
数组：存储哈希值对应的桶（bucket），初始容量16；
链表：当多个元素哈希值相同（哈希冲突），用链表存储；
红黑树：当链表长度超过8，且数组容量≥64时，链表转为红黑树（提升查询效率，从O(n)变为O(logn)）；当链表长度≤6时，红黑树转回链表。
六、总结
1. 集合核心体系：Collection（单元素）和Map（键值对），重点掌握List、Set、Map的常用实现类；
2. 选型原则：查询多⽤ArrayList，增删多⽤LinkedList；去重⽤HashSet，有序去重⽤LinkedHashSet，排序去重⽤TreeSet；键值对存储⽤HashMap，有序键值对⽤LinkedHashMap，排序键值对⽤TreeMap；
3. 面试重点：常用类的底层结构、区别、去重原理、HashMap底层实现，需结合实战场景记忆；
4. 实战避坑：注意线程安全、空指针、集合与数组转换的细节，避免踩坑。

