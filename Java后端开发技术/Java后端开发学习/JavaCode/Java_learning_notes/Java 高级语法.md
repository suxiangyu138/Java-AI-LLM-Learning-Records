# Java 高级语法 完整清单
## 一、泛型
- 泛型类、泛型接口、泛型方法
- 类型通配符：`?`、`? extends`、`? super`
- 泛型上下界、PECS原则、泛型擦除

## 二、集合框架（高级数据结构）
- List：ArrayList、LinkedList、Vector
- Set：HashSet、TreeSet、LinkedHashSet
- Map：HashMap、TreeMap、LinkedHashMap、ConcurrentHashMap
- 迭代器 Iterator、增强for遍历
- 集合工具类 Collections

## 三、多线程与并发
- 线程创建：继承Thread、实现Runnable、Callable+Future
- 线程生命周期、线程状态
- 同步：synchronized、Lock、ReentrantLock
- 线程通信：wait / notify / notifyAll
- 线程池：ThreadPoolExecutor、Executors
- JUC包：CountDownLatch、CyclicBarrier、Semaphore
- volatile、CAS、原子类、并发容器

## 四、异常体系
- 受检异常、非受检异常、错误
- try-catch-finally、throws、throw
- 自定义异常、异常链

## 五、IO 与 NIO
- 传统IO：字节流、字符流、缓冲流、序列化
- 序列化：Serializable、transient
- NIO：Buffer、Channel、Selector、非阻塞IO
- 文件操作：Path、Files

## 六、反射机制
- Class类、获取Class对象三种方式
- 反射构造器、方法、成员变量
- 动态创建对象、调用方法、修改属性
- 框架底层核心原理（Spring、MyBatis）

## 七、注解（Annotation）
- 内置注解：`@Override`、`@Deprecated`、`@SuppressWarnings`
- 元注解：@Target、@Retention、@Documented、@Inherited
- 自定义注解、注解解析（配合反射）

## 八、Lambda 表达式 & 函数式编程（JDK8+）
- 函数式接口、@FunctionalInterface
- Lambda简化匿名内部类
- 方法引用：对象引用、静态引用、构造引用
- Stream流式编程、Optional空值处理

## 九、接口新特性（JDK8/9+）
- JDK8：默认方法、静态方法
- JDK9：私有方法
- 解决接口升级兼容问题

## 十、枚举 Enum
- 枚举类、常量定义
- 枚举单例、枚举常用方法
- 抽象枚举、策略模式应用

## 十一、内部类
- 成员内部类、局部内部类、匿名内部类、静态内部类
- 应用场景、访问权限特点

## 十二、面向对象高级特性
- 多态、向上转型、向下转型
- 重写、重载区别
- 抽象类 abstract
- 接口 interface
- 封装、继承、多态三大特性延伸

## 十三、模块化 & 新特性（拓展）
- JDK9 模块 module
- 密封类 sealed（JDK17）
- 记录类 Record、模式匹配

## 十四、其他高级语法
- 可变参数 `...`
- 静态导入 import static
- 自动装箱&拆箱
- 常量池、字符串常量池
- 泛型+反射+注解：主流框架三大基石

---

