# MySQL 核心知识点学习笔记（完整版）
> 项目地址：https://github.com/suxiangyu138/MySQL-Learning-Notes
> 学习目标：Java后端开发必备MySQL知识体系，**适配面试+业务开发+生产实践**

## 1. 数据库基础概念
### 1.1 什么是MySQL
- MySQL是**开源关系型数据库管理系统(RDBMS)**，采用**C/S（客户端/服务端）**架构，支持Windows/Linux/Mac多平台，是Java后端、微服务、分布式系统最主流数据库
- 核心特点：**开源免费、高性能、支持事务ACID、支持索引优化、适配高并发/大数据量业务**
- 应用场景：电商订单、用户管理、支付、后台管理系统等几乎所有Java后端业务

### 1.2 核心术语
- 数据库(DataBase)：数据集合，对应一个业务模块（如：订单库、用户库）
- 数据表(Table)：二维表结构，存储同一类实体数据（如：用户表、商品表）
- 字段(Column)：表中列，对应实体属性（如：用户名、手机号）
- 记录(Row)：表中行，对应一个具体实体对象（如：张三这条用户数据）
- 主键(Primary Key)：唯一标识一条记录，非空且唯一（推荐使用自增ID/雪花ID）
- 外键(Foreign Key)：关联其他表的主键，建立表间关系（业务开发**少用物理外键**，多用逻辑关联）
- 索引(Index)：提升查询效率的数据结构（面试+开发核心）

### 1.3 存储引擎（重点）
- **InnoDB**：MySQL5.5+默认引擎，**支持事务、行锁、外键、崩溃恢复**，适合**增删改频繁、需要事务安全**的业务（订单、用户、支付）
- MyISAM：不支持事务，表锁，查询速度快，适合只读/少写的统计类业务（生产极少用）

---

## 2. MySQL基础语法（企业高频）
### 2.1 SQL语言分类（必背）
1. **DDL 数据定义语言**：定义库、表、字段（CREATE/ALTER/DROP）
2. **DML 数据操作语言**：增删改数据（INSERT/UPDATE/DELETE）
3. **DQL 数据查询语言**：查询数据（SELECT，企业最常用）
4. **DCL 数据控制语言**：权限管理（GRANT/REVOKE）
5. **TCL 事务控制语言**：事务管理（COMMIT/ROLLBACK）

### 2.2 DDL（数据定义语言）
```sql
-- 创建数据库（指定字符集utf8mb4，支持emoji）
CREATE DATABASE IF NOT EXISTS java_shop DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE java_shop;

-- 创建用户表（企业标准规范：主键、非空、注释、引擎、字符集）
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID（雪花ID/自增ID）',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（唯一）',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（加密存储）',
  `phone` VARCHAR(20) COMMENT '手机号',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1-正常 0-禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 修改表：添加字段
ALTER TABLE sys_user ADD COLUMN email VARCHAR(50) COMMENT '邮箱';

-- 修改表：修改字段
ALTER TABLE sys_user MODIFY COLUMN phone VARCHAR(30) COMMENT '手机号（加长）';

-- 删除表
DROP TABLE IF EXISTS sys_user;
```

### 2.3 DML（数据操作语言）
```sql
-- 单条插入
INSERT INTO sys_user (username, password, phone) 
VALUES ('zhangsan', '123456', '13800138000');

-- 批量插入（企业高频，性能更高）
INSERT INTO sys_user (username, password, phone) 
VALUES ('lisi', '123456', '13900139000'),
       ('wangwu', '654321', '13700137000');

-- 更新数据（必须加WHERE条件！禁止全表更新）
UPDATE sys_user SET phone='13900139999', update_time=NOW() WHERE id=1;

-- 删除数据（物理删除，必须加WHERE；业务优先用逻辑删除：status=0）
DELETE FROM sys_user WHERE id=1;

-- 逻辑删除（企业标准方案）
UPDATE sys_user SET status=0 WHERE id=1;
```

### 2.4 DQL（数据查询语言，核心）
#### 2.4.1 基础查询
```sql
-- 查询指定字段
SELECT id, username, phone FROM sys_user;

-- 查询所有字段（不推荐*，影响性能）
SELECT * FROM sys_user;

-- 条件查询
SELECT * FROM sys_user WHERE status=1 AND username LIKE '%zhang%';

-- 去重查询
SELECT DISTINCT phone FROM sys_user;
```

#### 2.4.2 排序 & 分页（Java后端必用）
```sql
-- 排序：DESC降序 / ASC升序
SELECT * FROM sys_user ORDER BY create_time DESC, id ASC;

-- 分页查询（LIMIT 起始行, 每页条数）
-- 第1页：每页10条
SELECT * FROM sys_user WHERE status=1 LIMIT 0, 10;
-- 第2页
SELECT * FROM sys_user WHERE status=1 LIMIT 10, 10;

-- 标准分页写法（适配MyBatis/MyBatis-Plus）
SELECT * FROM sys_user LIMIT 10 OFFSET 0;
```

#### 2.4.3 聚合函数 & 分组
```sql
-- 统计数量
SELECT COUNT(*) AS total FROM sys_user WHERE status=1;

-- 求和/最大值/最小值/平均值
SELECT SUM(price) AS total_price, MAX(price) AS max_price FROM product;

-- 分组查询（GROUP BY + HAVING）
SELECT status, COUNT(*) AS num FROM sys_user GROUP BY status HAVING num > 1;
```

#### 2.4.4 多表查询（企业核心）
**1）内连接（INNER JOIN）**：查询两张表匹配的数据
```sql
-- 用户表 + 订单表：查询有订单的用户
SELECT u.username, o.order_no
FROM sys_user u
INNER JOIN order_info o ON u.id = o.user_id
WHERE o.status = 1;
```

**2）左连接（LEFT JOIN）**：左表全部数据 + 右表匹配数据（最常用）
```sql
-- 查询所有用户，包括没有订单的用户
SELECT u.username, o.order_no
FROM sys_user u
LEFT JOIN order_info o ON u.id = o.user_id;
```

**3）子查询**：查询嵌套
```sql
-- 查询有订单的用户信息
SELECT * FROM sys_user WHERE id IN (SELECT user_id FROM order_info);
```

---

## 3. 约束（表设计规范）
- **主键约束(PRIMARY KEY)**：非空+唯一，一张表只能有一个
- **非空约束(NOT NULL)**：字段必须有值
- **唯一约束(UNIQUE)**：字段值不能重复（用户名、手机号）
- **默认约束(DEFAULT)**：未赋值时使用默认值
- **外键约束(FOREIGN KEY)**：关联表关系（**生产禁用物理外键**）

---

## 4. 索引（面试+性能优化核心）
### 4.1 索引作用
- **加快查询速度**，降低数据库IO成本
- 缺点：占用存储空间，降低增删改速度

### 4.2 索引分类
1. **主键索引**：PRIMARY KEY，自动创建
2. **唯一索引**：UNIQUE，值唯一
3. **普通索引**：INDEX，仅加速查询
4. **联合索引**：多个字段组合，**最左前缀原则**（面试高频）

### 4.3 索引实操
```sql
-- 创建普通索引
CREATE INDEX idx_username ON sys_user(username);

-- 创建联合索引（最常用）
CREATE INDEX idx_user_status_create ON sys_user(status, create_time);

-- 查看索引
SHOW INDEX FROM sys_user;

-- 删除索引
DROP INDEX idx_username ON sys_user;
```

### 4.4 索引失效场景（必背）
- 使用 `LIKE '%xxx'` 左模糊
- 对字段进行函数/运算操作
- 违反**最左前缀原则**
- 使用 `OR` 连接未索引字段
- 数据量太少，优化器放弃索引

---

## 5. 事务（面试必考）
### 5.1 事务特性（ACID）
1. **原子性(Atomicity)**：要么全部成功，要么全部失败
2. **一致性(Consistency)**：执行前后数据完整性不变
3. **隔离性(Isolation)**：多个事务互不干扰
4. **持久性(Durability)**：提交后数据永久生效

### 5.2 事务操作
```sql
-- 开启事务
START TRANSACTION;

-- 执行业务SQL
UPDATE account SET balance = balance - 100 WHERE id=1;
UPDATE account SET balance = balance + 100 WHERE id=2;

-- 提交事务（成功）
COMMIT;

-- 回滚事务（失败）
ROLLBACK;
```

### 5.3 事务隔离级别（从低到高）
1. **读未提交(READ UNCOMMITTED)**：脏读、不可重复读、幻读
2. **读已提交(READ COMMITTED)**：**MySQL默认**，解决脏读
3. **可重复读(REPEATABLE READ)**：解决不可重复读
4. **串行化(SERIALIZABLE)**：解决所有问题，性能极低

---

## 6. 开发规范 & 生产实践（Java后端必备）
1. **表名/字段名**：使用小写+下划线，禁止使用关键字、中文
2. **主键**：必须有，推荐 `BIGINT` 自增/雪花ID
3. **字符集**：统一使用 `utf8mb4`（支持emoji）
4. **字段注释**：所有表、字段必须加 `COMMENT`
5. **禁止使用**：`SELECT *`、物理外键、无WHERE的更新/删除
6. **分页**：大数据量禁止深分页，用游标/主键分页
7. **逻辑删除**：使用 `status TINYINT DEFAULT 1` 代替物理删除
8. **时间字段**：必须包含 `create_time`、`update_time`

---

## 7. 高频面试题总结
1. InnoDB和MyISAM的区别？
2. 事务ACID特性？
3. 索引原理？B+树优势？
4. 联合索引最左前缀原则？
5. 事务隔离级别及解决的问题？
6. 脏读、不可重复读、幻读是什么？
7. 慢查询优化方案？
8. 为什么推荐使用自增主键？

### 总结
这份完整版笔记覆盖了**Java后端开发+面试**的全部MySQL核心知识点：
1. **基础篇**：概念、语法、约束，满足日常业务开发
2. **进阶篇**：索引、事务、隔离级别，搞定面试高频考点
3. **实践篇**：企业规范、生产禁忌，直接适配工作场景
4. 可直接用于学习、面试准备、项目开发，是Java工程师必备手册