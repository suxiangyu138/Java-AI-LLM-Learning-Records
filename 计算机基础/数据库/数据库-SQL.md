# 数据库系统概念 —— SQL

> 从 Java 后端开发视角深度剖析结构化查询语言的核心理论与实践

---

## 📑 目录

1. [SQL 概述与分类](#1-sql-概述与分类)
2. [DDL —— 数据定义语言](#2-ddl--数据定义语言)
3. [DML —— 数据操纵语言](#3-dml--数据操纵语言)
4. [DCL —— 数据控制语言](#4-dcl--数据控制语言)
5. [TCL —— 事务控制语言](#5-tcl--事务控制语言)
6. [SQL 高级特性](#6-sql-高级特性)
7. [Java 后端视角深度解读](#7-java-后端视角深度解读)
8. [常见误区与总结](#8-常见误区与总结)

---

## 1. SQL 概述与分类

### 1.1 什么是 SQL

**SQL**（Structured Query Language，结构化查询语言）是关系数据库的标准语言，用于实现数据的定义、操纵和控制。

### 1.2 SQL 四大分类

| 分类 | 全称 | 功能 | 核心命令 |
|------|------|------|----------|
| **DDL** | Data Definition Language | 定义数据库对象结构 | `CREATE`, `ALTER`, `DROP`, `TRUNCATE` |
| **DML** | Data Manipulation Language | 操纵数据内容 | `SELECT`, `INSERT`, `UPDATE`, `DELETE` |
| **DCL** | Data Control Language | 控制访问权限 | `GRANT`, `REVOKE` |
| **TCL** | Transaction Control Language | 控制事务行为 | `BEGIN`, `COMMIT`, `ROLLBACK`, `SAVEPOINT` |

---

## 2. DDL —— 数据定义语言

### 2.1 数据库操作

```sql
-- 创建数据库
CREATE DATABASE bookstore CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 修改数据库
ALTER DATABASE bookstore CHARACTER SET utf8mb4;

-- 删除数据库
DROP DATABASE IF EXISTS bookstore;
```

### 2.2 表操作

```sql
-- 创建表
CREATE TABLE book (
    id          INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name        VARCHAR(100) NOT NULL COMMENT '书名',
    author      VARCHAR(50) COMMENT '作者',
    price       DECIMAL(10, 2) COMMENT '价格',
    stock       INT DEFAULT 0 COMMENT '库存',
    category_id INT COMMENT '分类ID',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (category_id) REFERENCES category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书籍表';

-- 修改表结构
ALTER TABLE book ADD COLUMN cover VARCHAR(200) COMMENT '封面图片URL';
ALTER TABLE book MODIFY COLUMN price DECIMAL(12, 2);
ALTER TABLE book DROP COLUMN cover;

-- 删除表
DROP TABLE IF EXISTS book;

-- 清空表（保留结构）
TRUNCATE TABLE book;
```

### 2.3 索引与视图

```sql
-- 创建索引
CREATE INDEX idx_book_name ON book(name);
CREATE UNIQUE INDEX idx_book_isbn ON book(isbn);
CREATE INDEX idx_book_category_price ON book(category_id, price);  -- 复合索引

-- 删除索引
DROP INDEX idx_book_name ON book;

-- 创建视图
CREATE VIEW v_book_list AS
SELECT id, name, author, price FROM book WHERE stock > 0;

-- 删除视图
DROP VIEW IF EXISTS v_book_list;
```

---

## 3. DML —— 数据操纵语言

### 3.1 查询数据 (SELECT)

```sql
-- 基础查询
SELECT name, author, price FROM book;

-- 条件查询
SELECT * FROM book WHERE price < 100 AND category_id = 2;

-- 排序查询
SELECT * FROM book ORDER BY price DESC, created_at ASC;

-- 分页查询 (MySQL)
SELECT * FROM book LIMIT 0, 10;   -- 第1页，每页10条
SELECT * FROM book LIMIT 10 OFFSET 20;  -- 第3页，每页10条

-- 去重查询
SELECT DISTINCT category_id FROM book;
```

### 3.2 聚合查询

```sql
-- 聚合函数
SELECT COUNT(*) FROM book;                          -- 总数
SELECT SUM(stock) FROM book;                        -- 总库存
SELECT AVG(price) FROM book;                        -- 平均价格
SELECT MAX(price), MIN(price) FROM book;            -- 最高/最低价格

-- 分组查询
SELECT category_id, COUNT(*) AS cnt, AVG(price) AS avg_price
FROM book
GROUP BY category_id
HAVING cnt > 5;

-- 排序 + 分页 + 聚合
SELECT category_id, COUNT(*) AS cnt
FROM book
WHERE stock > 0
GROUP BY category_id
HAVING cnt >= 3
ORDER BY cnt DESC
LIMIT 5;
```

### 3.3 增删改操作

```sql
-- 插入单行
INSERT INTO book (name, author, price, stock) 
VALUES ('Java实战', '张三', 69.90, 100);

-- 批量插入（推荐）
INSERT INTO book (name, author, price, stock) VALUES 
    ('Java实战', '张三', 69.90, 100),
    ('Spring揭秘', '李四', 89.00, 50),
    ('MySQL入门', '王五', 45.00, 200);

-- 更新（必须带 WHERE 条件）
UPDATE book SET stock = stock - 1 WHERE id = 1 AND stock > 0;

-- 逻辑删除（推荐）
UPDATE book SET is_deleted = 1 WHERE id = 1;

-- 物理删除
DELETE FROM book WHERE id = 1;
```

---

## 4. DCL —— 数据控制语言

```sql
-- 创建用户
CREATE USER 'app_user'@'%' IDENTIFIED BY 'StrongPassword123!';

-- 授予权限（最小权限原则）
GRANT SELECT, INSERT, UPDATE, DELETE ON bookstore.* TO 'app_user'@'%';

-- 授予特定列的权限
GRANT SELECT (id, name, price) ON bookstore.book TO 'readonly_user'@'%';

-- 回收权限
REVOKE DELETE ON bookstore.* FROM 'app_user'@'%';

-- 刷新权限
FLUSH PRIVILEGES;

-- 查看用户权限
SHOW GRANTS FOR 'app_user'@'%';
```

---

## 5. TCL —— 事务控制语言

```sql
-- 开启事务
START TRANSACTION;
-- 或
BEGIN;

-- 设置保存点
SAVEPOINT sp1;

-- 执行操作
UPDATE book SET stock = stock - 1 WHERE id = 1;
INSERT INTO order_item (order_id, book_id, quantity) VALUES ('ORD001', 1, 1);

-- 回滚到保存点
ROLLBACK TO SAVEPOINT sp1;

-- 提交事务
COMMIT;

-- 全部回滚
ROLLBACK;
```

---

## 6. SQL 高级特性

### 6.1 连接查询

| 连接类型 | 说明 | 使用场景 |
|----------|------|----------|
| `INNER JOIN` | 返回两表匹配的行 | 查询有订单的用户 |
| `LEFT JOIN` | 保留左表全部行 | 查询所有用户及其订单 |
| `RIGHT JOIN` | 保留右表全部行 | 查询所有订单及其用户 |
| `FULL JOIN` | 保留两表全部行 | 全量数据合并 |
| `CROSS JOIN` | 笛卡尔积 | 组合枚举 |

```sql
-- 内连接
SELECT u.name, o.order_no
FROM user u
INNER JOIN `order` o ON u.id = o.user_id;

-- 左连接（用户即使没有订单也显示）
SELECT u.name, o.order_no
FROM user u
LEFT JOIN `order` o ON u.id = o.user_id;

-- 多表连接
SELECT o.id, b.name, oi.quantity, oi.price
FROM `order` o
JOIN order_item oi ON o.id = oi.order_id
JOIN book b ON oi.book_id = b.id
WHERE o.user_id = 1;
```

### 6.2 子查询

```sql
-- WHERE 子查询
SELECT name, price FROM book
WHERE category_id IN (SELECT id FROM category WHERE name = '技术');

-- FROM 子查询（派生表）
SELECT t.category_name, AVG(t.price) AS avg_price
FROM (
    SELECT c.name AS category_name, b.price
    FROM book b
    JOIN category c ON b.category_id = c.id
) t
GROUP BY t.category_name;

-- EXISTS 子查询
SELECT name FROM book b
WHERE EXISTS (SELECT 1 FROM order_item oi WHERE oi.book_id = b.id);
```

### 6.3 集合运算

```sql
-- UNION（去重合并）
SELECT name FROM book WHERE price < 50
UNION
SELECT name FROM book WHERE stock > 100;

-- UNION ALL（保留重复）
SELECT name FROM book WHERE price < 50
UNION ALL
SELECT name FROM book WHERE stock > 100;
```

---

## 7. Java 后端视角深度解读

### 7.1 SQL 与 Java 技术栈的绑定

```
Java 应用层
    ↓
ORM 框架 (MyBatis / JPA / Hibernate)
    ↓
JDBC 驱动
    ↓
SQL → 数据库
```

| 框架 | SQL 关系 | 特点 |
|------|---------|------|
| **MyBatis** | XML/注解编写原生 SQL | 灵活，适配复杂查询 |
| **JPA/Hibernate** | JPQL（面向对象 SQL），自动生成 | 简化开发，CRUD 自动化 |
| **MyBatis-Plus** | 无 SQL 通用 CRUD | 基于 SQL 底层，极简开发 |

### 7.2 DDL 工程化实践

```java
// Flyway 版本化管理 SQL 脚本
// V1__init.sql
CREATE TABLE book (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL
);

// V2__add_price.sql
ALTER TABLE book ADD COLUMN price DECIMAL(10,2);
```

> **最佳实践：** 使用 Flyway 或 Liquibase 管理所有 DDL 变更，确保开发/测试/生产环境表结构一致。

### 7.3 DML 查询优化要点

| 优化策略 | 说明 | 示例 |
|----------|------|------|
| 避免 `SELECT *` | 按需查询字段，减少网络传输 | `SELECT id, name FROM book` |
| 合理使用 JOIN | 小表驱动大表 | 避免笛卡尔积 |
| 分页查询 | 使用 LIMIT 防止全表扫描 | `LIMIT 0, 20` |
| 配合索引 | WHERE 条件字段建索引 | `CREATE INDEX idx_name ON book(name)` |

### 7.4 安全实践

```java
// ❌ 错误：拼接 SQL 字符串（SQL 注入风险）
String sql = "SELECT * FROM user WHERE name = '" + username + "'";

// ✅ 正确：使用预编译语句
// MyBatis 中使用 #{ } 参数占位符
@Select("SELECT * FROM user WHERE name = #{username}")
User findByUsername(@Param("username") String username);
```

### 7.5 事务管理

```java
// Spring 声明式事务
@Service
public class OrderService {
    
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(OrderDTO dto) {
        // 1. 扣减库存
        bookMapper.decreaseStock(dto.getBookId(), dto.getQuantity());
        // 2. 创建订单
        orderMapper.insert(order);
        // 3. 创建订单项
        orderItemMapper.insertBatch(items);
        // 任一操作失败，全部回滚
    }
}
```

---

## 8. 常见误区与总结

### 8.1 Java 后端 SQL 常见误区

| 误区 | 后果 | 正确做法 |
|------|------|----------|
| 过度依赖 ORM，不懂 SQL | 生成低效 SQL，性能问题 | 理解 SQL 本质，审查 ORM 生成的 SQL |
| 滥用 `SELECT *` | 传输冗余数据，增加开销 | 只查需要的字段 |
| 无索引查询 | 全表扫描，高并发下崩溃 | 为高频查询字段建立索引 |
| 事务过大 | 长时间锁等待，影响并发 | 缩小事务范围，异步化非核心操作 |
| 拼接 SQL 字符串 | SQL 注入安全漏洞 | 使用预编译语句 / `#{}` |

### 8.2 总结

> **核心认知：** SQL 是连接业务与数据的桥梁，贯穿 Java 后端开发全流程。

掌握 SQL 对 Java 后端的价值：

- ✅ **数据操作基础** — 支撑所有数据相关业务
- ✅ **性能优化关键** — 慢查询优化、索引设计是系统响应速度的核心
- ✅ **工程化规范** — SQL 脚本版本化、事务控制保障系统稳定性
- ✅ **复杂业务支撑** — 高级查询、聚合统计实现电商/金融等复杂场景

---

> 📖 **相关阅读：** [数据库-引言](./数据库-引言.md) | [数据库-事务](./数据库-事务.md) | [数据库-中级SQL](./数据库-中级SQL.md) | [数据库-高级SQL](./数据库-高级SQL.md)
