# Java 后端开发面试

---

Java 后端面试一般分为 10 大模块

企业面试基本就是从这里面抽 5～8 个来问

---

## 一、Java 基础

主要问语法、特性、基础原理

常见问题：
- 八种基本数据类型
- == 和 equals 区别
- String、StringBuilder、StringBuffer 区别
- 装箱和拆箱
- 异常体系，运行时异常和编译异常区别
- final、finally、finalize 区别
- 接口和抽象类区别
- 反射是什么，有什么用
- 注解是什么，常见注解有哪些

---

## 二、集合框架

面试必问，频率非常高

常见问题：
- ArrayList 和 LinkedList 区别
- ArrayList 扩容机制
- HashMap 底层原理
- JDK 1.7 和 1.8 HashMap 变化
- ConcurrentHashMap 原理
- HashSet 底层
- 集合的线程安全问题
- Collection 和 Collections 区别

---

## 三、JVM

中大厂必问，小公司也会问基础

常见问题：
- JVM 内存模型
- 堆和栈的区别
- 垃圾回收机制
- 垃圾回收算法
- GC Roots 有哪些
- 类加载过程
- 双亲委派模型
- 内存溢出、内存泄漏是什么
- 常见 JVM 参数

---

## 四、多线程与并发

必问，难度较高

常见问题：
- 线程和进程的区别
- 创建线程的四种方式
- start() 和 run() 区别
- 线程生命周期
- synchronized 原理
- volatile 作用
- Lock 和 synchronized 区别
- 线程池参数，为什么要用线程池
- Callable 和 Future
- ThreadLocal 原理
- 死锁的四个条件

---

## 五、MySQL 数据库

所有公司必问，重中之重

常见问题：
- 存储引擎 InnoDB 和 MyISAM 区别
- 索引是什么，B+ 树
- 聚簇索引和非聚簇索引
- 最左前缀原则
- 事务四大特性 ACID
- 事务隔离级别
- 脏读、不可重复读、幻读
- MVCC 原理
- SQL 优化
- 慢查询优化
- 分库分表概念

---

## 六、Redis

现在面试基本必问

常见问题：
- Redis 数据类型
- Redis 持久化 RDB 和 AOF
- 缓存穿透、击穿、雪崩
- 解决方案
- 分布式锁
- Redis 线程模型
- 过期键淘汰策略
- Redis 集群模式

---

## 七、Spring + Spring Boot

必问，企业标配

常见问题：
- IoC 是什么
- AOP 是什么
- Spring Bean 生命周期
- Spring 事务传播机制
- @Transactional 原理
- Spring Boot 自动配置原理
- Spring Boot 启动流程
- Spring Boot 常用注解
- Bean 作用域
- 循环依赖解决方式

---

## 八、MyBatis / MyBatis-Plus

必问

常见问题：
- #{} 和 ${} 区别
- MyBatis 一级缓存、二级缓存
- MyBatis 动态 SQL
- MyBatis 生命周期
- MyBatis-Plus 常用注解
- MyBatis-Plus 条件构造器

---

## 九、Spring Cloud 微服务

中大厂必问

常见问题：
- 微服务优缺点
- 服务注册与发现
- Nacos、Eureka
- OpenFeign 作用
- Gateway 网关作用
- Sentinel 熔断、限流、降级
- 分布式事务
- Seata
- 微服务常见问题

---

## 十、项目 + 场景题 + 算法

面试最后一定会问

项目问题：
- 项目介绍
- 你负责什么模块
- 遇到什么难题，怎么解决
- 项目亮点
- 项目难点

场景题：
- 高并发怎么处理
- 接口变慢怎么优化
- 线上问题怎么排查
- 大量数据怎么导入

算法题：
- 简单题：两数之和、反转链表、二分查找
- 中等题：二叉树遍历、动态规划基础
- 排序算法

---
