# 06 - MySQL 基础操作速查
> MySQL 日常开发中最常用的 SQL 操作速查手册，涵盖 DDL/DML/DQL/TCL、单表与多表操作、视图、事务、数据类型、约束、索引、用户权限及安装配置，适合日常编码参考。

## 目录
1. [SQL 语言分类](#1-sql-语言分类)
2. [库与表 DDL](#2-库与表-ddl)
3. [数据类型速查](#3-数据类型速查)
4. [约束速查](#4-约束速查)
5. [单表操作 DML/DQL](#5-单表操作-dmldql)
6. [多表操作 JOIN](#6-多表操作-join)
7. [事务 TCL](#7-事务-tcl)
8. [视图](#8-视图)
9. [索引基础](#9-索引基础)
10. [用户与权限 DCL](#10-用户与权限-dcl)
11. [MySQL 安装配置（Windows）](#11-mysql-安装配置windows)

---

## 1. SQL 语言分类

| 分类 | 全称 | 作用 | 关键字 |
|------|------|------|--------|
| DDL | Data Definition Language | 定义库、表、字段 | CREATE, ALTER, DROP |
| DML | Data Manipulation Language | 增删改数据 | INSERT, UPDATE, DELETE |
| DQL | Data Query Language | 查询数据 | SELECT |
| DCL | Data Control Language | 权限管理 | GRANT, REVOKE |
| TCL | Transaction Control Language | 事务管理 | COMMIT, ROLLBACK, SAVEPOINT |

---

## 2. 库与表 DDL

### 数据库操作

```sql
-- 创建库（指定字符集 utf8mb4，支持 emoji）
CREATE DATABASE IF NOT EXISTS java_shop
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- 查看所有库
SHOW DATABASES;

-- 使用库
USE java_shop;

-- 查看当前所在库
SELECT DATABASE();

-- 删除库
DROP DATABASE IF EXISTS java_shop;
```

### 表操作

```sql
-- 创建表
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username    VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT '加密后密码',
    phone       VARCHAR(11) UNIQUE COMMENT '手机号',
    gender      TINYINT DEFAULT 0 COMMENT '性别：0未知 1男 2女',
    status      TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 查看表结构
DESC t_user;

-- 查看建表语句
SHOW CREATE TABLE t_user;

-- 修改表（ALTER）
ALTER TABLE t_user ADD email VARCHAR(100) COMMENT '邮箱';
ALTER TABLE t_user MODIFY phone VARCHAR(20) COMMENT '手机号';
ALTER TABLE t_user CHANGE phone mobile VARCHAR(20) COMMENT '手机号';
ALTER TABLE t_user DROP COLUMN email;
ALTER TABLE t_user RENAME TO t_user_new;

-- 删除表
DROP TABLE IF EXISTS t_user;

-- 清空表（delete 逐行删，truncate 直接重置）
TRUNCATE TABLE t_user;
```

---

## 3. 数据类型速查

### 数值类型

| 类型 | 字节 | 范围/特点 | 适用场景 |
|------|------|-----------|----------|
| `TINYINT` | 1 | -128~127 / 0~255 | 状态、性别、开关 |
| `SMALLINT` | 2 | -32768~32767 | 小规模数字 |
| `INT` | 4 | -21亿~21亿 | 常规 ID、年龄、数量（最常用） |
| `BIGINT` | 8 | 极大范围 | 订单号、雪花 ID |
| `FLOAT` | 4 | 单精度浮点 | 不推荐存金额 |
| `DOUBLE` | 8 | 双精度浮点 | 科学计算 |
| `DECIMAL(m,n)` | 变长 | 定点数，精确 | **金额/财务必用** |

> ⚠️ **金额必须使用 DECIMAL**，禁止使用 DOUBLE/FLOAT，避免精度丢失。

### 字符串类型

| 类型 | 特点 | 适用场景 |
|------|------|----------|
| `CHAR(n)` | 固定长度，存取快 | 手机号（CHAR(11)）、身份证、固定编码 |
| `VARCHAR(n)` | 可变长度，节省空间 | 用户名、地址、文本描述（最常用） |
| `TEXT` | 大文本，无固定限制 | 文章内容、长备注 |

> `CHAR` 长度不足自动补空格；`VARCHAR` 按需占用空间。

### 日期时间类型

| 类型 | 格式 | 说明 |
|------|------|------|
| `DATE` | yyyy-MM-dd | 仅日期 |
| `TIME` | HH:mm:ss | 仅时间 |
| `DATETIME` | yyyy-MM-dd HH:mm:ss | 常用时间字段（推荐） |
| `TIMESTAMP` | 时间戳 | 范围 1970~2038，受时区影响 |

---

## 4. 约束速查

| 约束 | 关键字 | 说明 |
|------|--------|------|
| 主键 | `PRIMARY KEY` | 非空 + 唯一，一张表一个 |
| 非空 | `NOT NULL` | 字段不能为 NULL |
| 唯一 | `UNIQUE` | 字段值不能重复 |
| 默认值 | `DEFAULT` | 未赋值时的默认值 |
| 自增 | `AUTO_INCREMENT` | 整数自动增长（常用于主键） |
| 外键 | `FOREIGN KEY` | 关联另一张表（**业务开发少用物理外键**） |

```sql
-- 约束示例
CREATE TABLE t_order (
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no  VARCHAR(32) NOT NULL UNIQUE COMMENT '订单号',
    user_id   BIGINT NOT NULL COMMENT '用户ID',
    amount    DECIMAL(10,2) DEFAULT 0.00 COMMENT '金额',
    status    TINYINT DEFAULT 0 COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 5. 单表操作 DML/DQL

### INSERT

```sql
-- 插入单条
INSERT INTO t_user(username, password, phone)
VALUES ('zhangsan', '123456', '13800138000');

-- 批量插入（推荐）
INSERT INTO t_user(username, password) VALUES
('lisi', 'abc123'),
('wangwu', 'def456');

-- 复制表数据
INSERT INTO t_user_backup SELECT * FROM t_user;
```

### UPDATE

```sql
-- 更新单字段
UPDATE t_user SET phone = '13900139000' WHERE id = 1;

-- 更新多字段
UPDATE t_user SET password = 'newpwd', updated_at = NOW() WHERE id = 1;

-- 不带 WHERE = 全表更新（慎用）
UPDATE t_user SET status = 1;
```

### DELETE

```sql
-- 删除指定记录
DELETE FROM t_user WHERE id = 1;

-- 清空全表（可回滚）
DELETE FROM t_user;

-- 清空全表（不可回滚，速度更快）
TRUNCATE TABLE t_user;

-- 企业级：逻辑删除（推荐）
UPDATE t_user SET status = 0 WHERE id = 1;
```

### SELECT 基础

```sql
-- 全字段查询（开发禁止用 *）
SELECT id, username, phone FROM t_user;

-- 去重
SELECT DISTINCT status FROM t_user;

-- 别名
SELECT u.username AS name FROM t_user u;

-- 分页
SELECT * FROM t_user ORDER BY id LIMIT 10 OFFSET 0;
-- 简写
SELECT * FROM t_user ORDER BY id LIMIT 0, 10;

-- 条件查询
SELECT * FROM t_user WHERE status = 1 AND age > 18;
SELECT * FROM t_user WHERE phone IS NOT NULL;
SELECT * FROM t_user WHERE id IN (1, 2, 3);
SELECT * FROM t_user WHERE username LIKE '张%';
SELECT * FROM t_user WHERE age BETWEEN 18 AND 30;

-- 排序
SELECT * FROM t_user ORDER BY created_at DESC, id ASC;

-- 聚合函数
SELECT COUNT(*) FROM t_user WHERE status = 1;
SELECT MAX(age), MIN(age), AVG(age) FROM t_user;
SELECT status, COUNT(*) AS cnt FROM t_user GROUP BY status;

-- HAVING（分组后过滤）
SELECT status, COUNT(*) AS cnt
FROM t_user
GROUP BY status
HAVING cnt > 10;

-- 窗口函数（MySQL 8.0+）
SELECT username, age,
       ROW_NUMBER() OVER (ORDER BY age DESC) AS rn,
       RANK() OVER (ORDER BY age DESC) AS rk,
       DENSE_RANK() OVER (ORDER BY age DESC) AS dr
FROM t_user;
```

---

## 6. 多表操作 JOIN

### 表关系

| 关系 | 示例 | 实现方式 |
|------|------|----------|
| 一对一 | 用户 ↔ 身份证 | 任意一方加外键 + UNIQUE |
| 一对多 | 部门 → 员工 | 多方加外键（员工表加 dept_id） |
| 多对多 | 学生 ↔ 课程 | 新建中间表（student_course） |

### JOIN 查询

```sql
-- INNER JOIN：两表交集
SELECT e.emp_name, d.dept_name
FROM employee e
INNER JOIN department d ON e.dept_id = d.dept_id;

-- 隐式内连接
SELECT * FROM employee e, department d WHERE e.dept_id = d.dept_id;

-- LEFT JOIN：左表全部保留
SELECT e.emp_name, d.dept_name
FROM employee e
LEFT JOIN department d ON e.dept_id = d.dept_id;

-- RIGHT JOIN：右表全部保留
SELECT e.emp_name, d.dept_name
FROM employee e
RIGHT JOIN department d ON e.dept_id = d.dept_id;

-- 自连接：同一张表自己关联自己
SELECT e1.emp_name AS '员工', e2.emp_name AS '上级'
FROM employee e1
LEFT JOIN employee e2 ON e1.manager_id = e2.id;

-- 多表 JOIN
SELECT u.username, o.order_no, o.amount
FROM t_user u
JOIN t_order o ON u.id = o.user_id
JOIN t_order_item oi ON o.id = oi.order_id;

-- 子查询
SELECT * FROM t_user WHERE id IN (
    SELECT user_id FROM t_order WHERE amount > 100
);

-- EXISTS
SELECT * FROM t_user u
WHERE EXISTS (
    SELECT 1 FROM t_order o WHERE o.user_id = u.id
);
```

### 笛卡尔积

```sql
-- 错误：无关联条件 → 数据错乱
SELECT * FROM student, class;

-- 正确：加关联条件
SELECT * FROM student s, class c WHERE s.class_id = c.id;
```

---

## 7. 事务 TCL

### 事务四大特性（ACID）

| 特性 | 说明 | 实现机制 |
|------|------|----------|
| 原子性（Atomicity） | 全部成功或全部回滚 | undo log |
| 一致性（Consistency） | 事务前后数据约束完整 | 应用层 + 数据库约束 |
| 隔离性（Isolation） | 事务之间互不干扰 | MVCC + 锁 |
| 持久性（Durability） | 提交后永久保存 | redo log |

### 事务操作

```sql
-- 开启事务
START TRANSACTION;

-- 执行 DML
UPDATE account SET money = money - 100 WHERE id = 1;
UPDATE account SET money = money + 100 WHERE id = 2;

-- 提交（正常）
COMMIT;

-- 回滚（异常）
ROLLBACK;

-- 保存点
SAVEPOINT sp1;
ROLLBACK TO sp1;
```

### 隔离级别

| 级别 | 脏读 | 不可重复读 | 幻读 | 默认 |
|------|------|-----------|------|------|
| READ UNCOMMITTED | 可能 | 可能 | 可能 | - |
| READ COMMITTED | 避免 | 可能 | 可能 | Oracle 默认 |
| REPEATABLE READ | 避免 | 避免 | 可能（InnoDB 间隙锁抑制） | **MySQL 默认** |
| SERIALIZABLE | 避免 | 避免 | 避免 | - |

---

## 8. 视图

视图是**虚拟表**，本身不存储数据，只保存一条 SELECT 查询语句。

```sql
-- 创建视图
CREATE VIEW v_user_order AS
SELECT u.id, u.username, o.order_no, o.amount
FROM t_user u
JOIN t_order o ON u.id = o.user_id;

-- 查询视图（像查表一样）
SELECT * FROM v_user_order WHERE amount > 100;

-- 修改视图
ALTER VIEW v_user_order AS
SELECT u.id, u.username, o.order_no
FROM t_user u
JOIN t_order o ON u.id = o.user_id;

-- 删除视图
DROP VIEW IF EXISTS v_user_order;

-- 查看视图定义
SHOW CREATE VIEW v_user_order;
```

> 💡 **视图的优点**：简化复杂查询、提高安全性（隐藏表结构）、逻辑封装。但**不提升查询性能**——执行视图本质还是执行内部的 SELECT。

---

## 9. 索引基础

```sql
-- 创建索引
CREATE INDEX idx_username ON t_user(username);          -- 普通索引
CREATE UNIQUE INDEX idx_phone ON t_user(phone);          -- 唯一索引
CREATE INDEX idx_name_age ON t_user(username, age);      -- 联合索引

-- 查看索引
SHOW INDEX FROM t_user;

-- 删除索引
DROP INDEX idx_username ON t_user;

--  EXPLAIN 查看是否使用索引
EXPLAIN SELECT * FROM t_user WHERE username = 'zhangsan';
```

> 💡 **索引不是越多越好**——每个索引增加写入开销和存储空间，需根据查询模式权衡。

---

## 10. 用户与权限 DCL

```sql
-- 创建用户
CREATE USER 'app_user'@'localhost' IDENTIFIED BY 'password123';

-- 授予权限
GRANT SELECT, INSERT, UPDATE, DELETE ON java_shop.* TO 'app_user'@'localhost';

-- 授予全部权限
GRANT ALL PRIVILEGES ON java_shop.* TO 'app_user'@'%';

-- 查看权限
SHOW GRANTS FOR 'app_user'@'localhost';

-- 刷新权限
FLUSH PRIVILEGES;

-- 修改密码
ALTER USER 'app_user'@'localhost' IDENTIFIED BY 'newpassword';

-- 撤销权限
REVOKE DELETE ON java_shop.* FROM 'app_user'@'localhost';

-- 删除用户
DROP USER 'app_user'@'localhost';
```

> ⚠️ **生产环境权限最小化原则**：只授予业务需要的权限，避免使用 `ALL PRIVILEGES`。

---

## 11. MySQL 安装配置（Windows）

### 安装步骤

1. 从 [MySQL 官网](https://dev.mysql.com/downloads/mysql/) 下载 MSI 安装包（推荐 MySQL 8.0+）
2. 运行安装程序，选择 **Developer Default** 或 **Server only**
3. 设置 root 密码
4. 选择 **MySQL Service** 以 Windows 服务方式运行

### 验证安装

```bash
mysql --version
mysql -u root -p
```

### 常用配置（my.ini）

```ini
[mysqld]
port=3306
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
max_connections=200
default-storage-engine=InnoDB

[client]
default-character-set=utf8mb4
```

> 🎯 **本文是日常开发高频操作的速查参考**，深入原理（MVCC、锁机制、B+树索引等）请参见同目录下 `01-MySQL核心原理.md`。

---

**返回总览**：[00-MySQL总览与技术术语](00-MySQL总览与技术术语.md) | **上一篇**：[05-SQL优化与性能调优](05-SQL优化与性能调优.md) | **下一篇**：[07-数据库设计规范](07-数据库设计规范.md)
