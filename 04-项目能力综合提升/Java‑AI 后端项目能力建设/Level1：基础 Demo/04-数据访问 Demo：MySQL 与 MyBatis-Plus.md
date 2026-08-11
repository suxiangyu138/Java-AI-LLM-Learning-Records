# 04 数据访问 Demo：MySQL 与 MyBatis-Plus

> 让数据"落盘"：装 MySQL 8.4、建库建表，用 MyBatis-Plus 完成 CRUD 与事务，理解连接池与 ORM 的本质。数据访问是后续 AI 闭环（知识库、会话记录）的地基。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [MySQL 安装与建库](#2-mysql-安装与建库)
3. [工程集成：依赖与配置](#3-工程集成依赖与配置)
4. [实体、Mapper 与 CRUD](#4-实体mapper-与-crud)
5. [Service 层与事务](#5-service-层与事务)
6. [理解连接池与 ORM](#6-理解连接池与-orm)
7. [常见坑](#7-常见坑)
8. [核心要点](#8-核心要点)

---

## 1. 目标与验收

本 Demo 的产出：订单表与用户表的增删改查接口（用 MyBatis-Plus 而非手写 SQL），一个带事务的转账/改单示例。验收标准：**能从零建库建表并让工程连上**；**CRUD 四个接口都能跑通**；**讲清 MyBatis-Plus 帮你做了什么**（反射生成 SQL？怎么映射的？）。版本基线：MySQL 8.4 LTS、MyBatis-Plus 3.5.x、Spring Boot 4.1.x。注意：MyBatis-Plus 3.5.x 对 Spring Boot 4 的兼容性需要确认 starter 版本（boot3 分支），若集成报错，用 MyBatis 官方 `mybatis-spring-boot-starter` 3.0.x 兜底——Demo 阶段目标是把"连库读写"跑通，框架细节选型放 Level2。

## 2. MySQL 安装与建库

Windows 安装 MySQL 8.4：下载 MySQL Installer → 选 Server only → 设置 root 密码（记牢，Level1 会反复用）→ 默认端口 3306 保持。装完验证：命令行 `mysql -uroot -p` 能登录。图形工具用 IDEA 内置 Database 面板（右侧 Database → + → MySQL → 填地址端口账号密码，测试连接成功即可）——不额外装 Navicat，少一个工具少一类破解与版本问题。

建库建表两条 SQL 是本 Demo 的数据地基：

```sql
CREATE DATABASE demo_db DEFAULT CHARACTER SET utf8mb4;
USE demo_db;

CREATE TABLE `order` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_name VARCHAR(100) NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
```

三个设计点直接对应面试考点：**utf8mb4**（支持 emoji 与生僻字，MySQL 8 默认，建库时显式声明是好习惯）；**DECIMAL 存金额**（浮点存钱是事故，与 02 篇一致）；**InnoDB 引擎**（支持事务与行锁，MySQL 8 默认引擎，理解它的特点——行级锁、MVCC、外键——是 Level2 面试的弹药）。

## 3. 工程集成：依赖与配置

在 03 篇工程基础上加依赖（pom.xml）：`mybatis-plus-spring-boot3-starter`（或 MyBatis 官方 starter）与 `mysql-connector-j`（驱动，版本由 Boot 管理）。application.properties 配置四要素：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/demo_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=你的密码
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

配置要点：**url 里的三件套**（useUnicode + characterEncoding + serverTimezone）是中文乱码与时间错乱的第一防线；**driver-class-name 用 com.mysql.cj.jdbc.Driver**（MySQL 8 的驱动类名，旧版 com.mysql.jdbc.Driver 已废弃）；密码先硬编码在配置里，Level2 再讲配置加密与多环境。连上后启动日志出现 `HikariPool-1 - Start completed` 即成功——HikariCP 是 Boot 默认连接池，这个日志是"数据库连接成功"的标准信号。

## 4. 实体、Mapper 与 CRUD

MyBatis-Plus 的用法三件套：**实体类**（@TableName 映射表名、@TableId 主键）、**Mapper 接口**（继承 BaseMapper，零方法即可获得 CRUD）、**IService/ServiceImpl**（业务层封装）。注意：MyBatis-Plus 用的是 Lombok 风格的实体 + 注解映射，与 02 篇的 record 风格不同（MyBatis-Plus 需要可变 POJO 才能做属性映射，这是 ORM 的实用取舍）。

```java
@Data
@TableName("`order`")
public class OrderEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String productName;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
}
```

```java
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
}

@Service
public class OrderService extends ServiceImpl<OrderMapper, OrderEntity> {
    public OrderEntity getByUser(Long userId) {
        return lambdaQuery().eq(OrderEntity::getUserId, userId).one();
    }
}
```

三个注意点：**表名 `order` 是 MySQL 保留字**，必须用反引号转义（@TableName 里写 "`order`"，建表 SQL 也写反引号）——这是本 Demo 最容易踩的坑；**lambdaQuery 是 MP 的链式查询**（类型安全、防手写 SQL 拼错），比 QueryWrapper 字符串写法更现代；**@Mapper 注解可省**（@MapperScan 在启动类标注一次即可），两者选一，Demo 用 @MapperScan 更简洁。CRUD 接口直接调用 service：insert/removeById/updateById/getById，每个跑一遍并看数据库变化。

**主键策略与逻辑删除**两个设计顺手定下来（Level2 直接沿用）：**主键**用 `IdType.AUTO`（自增，单机 Demo 够用）——面试要能说出 AUTO 与 ASSIGN_ID（雪花算法）的取舍：自增简单、利于索引页顺序写，但分库分表后会冲突；雪花 ID 全局唯一、无中心依赖，但长度大且依赖时钟。Demo 用 AUTO，知道 ASSIGN_ID 的存在与理由即可。**逻辑删除**——删除订单不物理删除，加 `deleted` 字段（0 未删/1 已删），MP 用 `@TableLogic` 注解，查询自动过滤、删除自动变更新——这是电商等真实业务的标准做法（保留数据可审计、可恢复），面试常问，Level1 先知道"为什么"。

**两个日常调试配置**顺手加上：**SQL 日志打印**——`logging.level.com.example.demo.mapper=DEBUG`（把 MP 生成的 SQL 打到控制台，是"验证 SQL 对不对"的第一手段——ORM 帮你生成 SQL，你要能看见它）；**分页**——MP 的分页需要配置分页插件（`MybatisPlusInterceptor` + `PaginationInnerInterceptor` Bean），配好后 `page(new Page<>(1, 10), wrapper)` 即返回分页结果。分页插件是 MyBatis-Plus 面试的标配考点（为什么 MP 分页要用插件——拦截器原理，物理分页 vs 内存分页），Demo 配置一遍，理解"拦截器机制"这个概念，Level2 讲自定义插件时是同一套原理。

## 5. Service 层与事务

事务是本 Demo 的技术核心。模拟一个"改单"场景：给订单退款，同时更新用户余额——两步必须一起成功或一起失败。实现：在 Service 方法上加 `@Transactional`，两步操作分别写两个 mapper 调用，制造一次失败（比如余额字段故意设超范围）观察回滚：

```java
@Transactional
public void refund(Long orderId, Long userId, BigDecimal amount) {
    orderMapper.update(null, new LambdaUpdateWrapper<OrderEntity>()
        .eq(OrderEntity::getId, orderId)
        .set(OrderEntity::getStatus, "REFUNDED"));
    userMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
        .eq(UserEntity::getId, userId)
        .setSql("balance = balance - " + amount));   // 模拟失败点：观察回滚
}
```

理解三个要点：**@Transactional 默认只回滚 RuntimeException**（受检异常不触发回滚，这是经典面试坑）；**事务边界 = 方法边界**（内部调用 this.method() 不经过代理，事务不生效——自调用陷阱）；**回滚验证方法**（故意抛异常，看数据库数据是否还原，还原即事务生效）。这三个要点理解后，事务的基本功就到位了。

## 6. 理解连接池与 ORM

本 Demo 背后有两个机制值得花 20 分钟理解——它们解释"为什么这样配置"以及 Level2 排障的方向。**连接池**：HikariCP 默认维护 10 个连接（maximum-pool-size），请求时借用、用完归还，避免每次操作都新建连接（建连是数据库操作最贵的部分）。验证方式：连续调用接口后看日志 `HikariPool-1 - Starting...Start completed` 只出现一次；连接耗尽时日志出现 `Connection is not available, request timed out`。**ORM 本质**：MyBatis-Plus 在运行时用反射把实体类字段映射为 SQL 列名（驼峰转下划线），执行 SQL 后用 ResultSet 反射回填实体。理解反射开销：批量大数据用 BaseMapper 的批量方法与手写 SQL 差异不大，但千万行级场景要自己写优化 SQL——Demo 阶段不用优化，知道边界即可。

## 7. 常见坑

**驱动类找不到**：没加 mysql-connector-j 依赖，或加了但版本冲突。检查 pom 与依赖树（IDEA 右侧 Maven → Dependencies）。

**中文乱码入库**：url 缺 characterEncoding=utf8，或建库时没指定 utf8mb4。两处一起查。

**`Table 'demo_db.order' doesn't exist`**：表名大小写问题（Windows 不区分、Linux 区分）或没执行建表 SQL。Demo 阶段表名全小写最省心。

**时区错误**：`The server time zone value 'ÖÐ¹ú±ê×¼Ê±¼ä'`——url 加 serverTimezone=Asia/Shanghai 解决。

**自调用事务不生效**：一个 service 方法内部调用同类另一个 @Transactional 方法，后者不生效。需要事务就拆到不同 Bean，或用 AopContext 代理调用。

## 8. 核心要点

1. MySQL 8.4 + utf8mb4 + InnoDB + DECIMAL 存金额，三个设计点都是面试弹药。
2. 连接三件套：useUnicode、characterEncoding、serverTimezone；HikariPool 日志是成功信号。
3. MP 三件套：@TableName 实体 + BaseMapper + ServiceImpl；保留字表名用反引号。
4. 事务三要点：默认只回滚 RuntimeException、边界是方法边界、自调用陷阱。
5. 连接池与反射是"为什么这样配"的答案，20 分钟理解胜过背十篇配置文。

> 🎯 **核心要点**：本 Demo 的完成标志是**"数据真的持久化了"**——重启服务数据还在、事务失败数据还原。理解实体→SQL→结果集的映射过程，是后续 AI 知识库（向量表）与 Level2 全部业务代码的共同地基。

---

**下一模块**：[05 Redis 缓存 Demo](./05-Redis%20缓存%20Demo.md) | **返回总览**：[Level1 总览](./00-Level1%20基础%20Demo%20总览.md)
