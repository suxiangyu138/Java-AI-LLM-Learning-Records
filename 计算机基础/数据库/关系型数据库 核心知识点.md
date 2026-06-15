# 关系型数据库 —— 核心知识点

> 以 Bookstore 项目为实战场景，系统梳理关系型数据库核心概念与 SQL 操作

---

## 📑 目录

1. [核心定义与特征](#1-核心定义与特征)
2. [核心概念 —— Bookstore 场景映射](#2-核心概念--bookstore-场景映射)
3. [表关系类型](#3-表关系类型)
4. [SQL 核心操作](#4-sql-核心操作)
5. [事务管理](#5-事务管理)
6. [Bookstore 项目实践要点](#6-bookstore-项目实践要点)
7. [核心总结](#7-核心总结)

---

## 1. 核心定义与特征

### 1.1 什么是关系型数据库

基于**关系模型**（二维表结构）的数据库，数据以**行**（记录）和**列**（字段）的形式存储。

### 1.2 四大核心特征

| 特征 | 说明 |
|------|------|
| **ACID 原则** | 原子性、一致性、隔离性、持久性，保证数据操作可靠性 |
| **SQL 支持** | 结构化查询语言，用于数据增删改查和管理 |
| **外键关联** | 数据之间通过外键建立关联，保证数据完整性 |
| **关系类型** | 表之间支持一对一、一对多、多对多关系 |

### 1.3 主流产品对比

| 产品 | 定位 | 适用场景 |
|------|------|----------|
| **MySQL** | 开源免费，社区活跃 | 中小型项目、Web 应用首选 |
| **Oracle** | 商业旗舰，功能全面 | 大型企业级、金融核心系统 |
| **SQL Server** | 微软生态深度集成 | Windows 生态、商业智能 |
| **PostgreSQL** | 开源高性能，标准兼容 | 复杂查询、地理空间数据 |

---

## 2. 核心概念 —— Bookstore 场景映射

| 概念 | 定义 | Bookstore 示例 |
|------|------|---------------|
| **数据库 (Database)** | 存储一组相关数据的集合 | `bookstore` 数据库 |
| **表 (Table)** | 数据库的基本存储单元 | `user`、`book`、`cart`、`order`、`order_item` |
| **行 (Row)** | 表中的一条数据记录 | `user` 表中一条用户注册记录 |
| **列 (Column)** | 表中的一个属性字段 | `book` 表的 `name`、`price`、`stock` |
| **主键 (Primary Key)** | 唯一标识每行数据 | `book.id`（自增主键） |
| **外键 (Foreign Key)** | 关联两个表的字段 | `cart.user_id` → `user.id` |
| **索引 (Index)** | 提升查询效率的结构 | `username`、`user_id + book_id` 复合索引 |

---

## 3. 表关系类型

### 3.1 关系图示

```
┌──────┐         ┌──────────┐         ┌──────┐
│ user │ 1────N  │  order   │ 1────N  │order │
│      │         │          │         │_item │──┐
└──────┘         └──────────┘         └──────┘  │
    │                                           N
    │    ┌──────┐         ┌──────┐              │
    └─── │ cart │ ─────── │ book │──────────────┘
         └──────┘  M    N └──────┘
```

| 关系类型 | 说明 | Bookstore 示例 |
|----------|------|---------------|
| **一对一 (1:1)** | 一条记录对应另一表的一条记录 | `user` ↔ `user_info`（用户详情） |
| **一对多 (1:N)** | 一条记录对应另一表的多条记录 | `user` → `order`、`book` → `order_item` |
| **多对多 (M:N)** | 两表记录互相对应多条 | `user` ↔ `book`（通过 `cart` 中间表关联） |

---

## 4. SQL 核心操作

### 4.1 DDL —— 数据定义语言

```sql
-- 创建数据库
CREATE DATABASE bookstore CHARACTER SET utf8mb4;

-- 创建表（以 book 表为例）
CREATE TABLE book (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    author      VARCHAR(50),
    price       DECIMAL(10, 2),
    stock       INT DEFAULT 0,
    category_id INT,
    FOREIGN KEY (category_id) REFERENCES category(id)
);

-- 修改表结构
ALTER TABLE book ADD COLUMN cover VARCHAR(200);

-- 删除表
DROP TABLE IF EXISTS book;
```

### 4.2 DML —— 数据操作语言

```sql
-- 新增
INSERT INTO book (name, author, price, stock)
VALUES ('JavaWeb实战', '张三', 69.9, 100);

-- 修改（安全库存扣减）
UPDATE book SET stock = stock - 1 WHERE id = 1 AND stock > 0;

-- 删除
DELETE FROM cart WHERE user_id = 1 AND book_id = 2;
```

### 4.3 DQL —— 数据查询语言

```sql
-- 基础查询
SELECT name, author, price FROM book;

-- 条件查询
SELECT * FROM book WHERE price < 100 AND category_id = 2;

-- 排序查询
SELECT * FROM book ORDER BY price DESC;

-- 分页查询
SELECT * FROM book LIMIT 0, 10;  -- 第 1 页，每页 10 条

-- 关联查询（订单详情）
SELECT o.id, b.name, oi.quantity, oi.price
FROM `order` o
JOIN order_item oi ON o.id = oi.order_id
JOIN book b ON oi.book_id = b.id
WHERE o.user_id = 1;
```

### 4.4 DCL —— 数据控制语言

```sql
-- 创建用户
CREATE USER 'bookstore_user'@'%' IDENTIFIED BY 'SecurePass123!';

-- 最小权限授权
GRANT SELECT, INSERT, UPDATE, DELETE ON bookstore.* TO 'bookstore_user'@'%';

-- 刷新权限
FLUSH PRIVILEGES;
```

---

## 5. 事务管理

### 5.1 事务定义

事务是一组**不可分割**的 SQL 操作，要么全部执行成功，要么全部回滚。

### 5.2 Bookstore 核心场景：生成订单

```sql
START TRANSACTION;

-- 1. 扣减书籍库存
UPDATE book SET stock = stock - 2 WHERE id = 1 AND stock >= 2;

-- 2. 插入订单
INSERT INTO `order` (id, user_id, total_price, address, status)
VALUES ('ORDER123', 1, 139.8, '北京市', 0);

-- 3. 插入订单项
INSERT INTO order_item (order_id, book_id, quantity, price)
VALUES ('ORDER123', 1, 2, 69.9);

-- 全部成功则提交
COMMIT;

-- 任一失败则回滚
-- ROLLBACK;
```

### 5.3 ACID 在本场景的体现

| 特性 | 含义 | 订单场景保障 |
|------|------|-------------|
| **原子性** | 全部成功或全部回滚 | 库存扣减 + 订单创建 + 订单项插入不可分割 |
| **一致性** | 事务前后数据完整性不变 | 库存减少数量 = 订单项数量 |
| **隔离性** | 并发事务互不干扰 | 多用户同时下单不相互影响 |
| **持久性** | 提交后数据永久保存 | 订单提交后不丢失 |

---

## 6. Bookstore 项目实践要点

### 6.1 表设计原则

| 原则 | 说明 |
|------|------|
| **遵循三范式** | 减少数据冗余（订单表不重复存储书籍名称，通过外键关联） |
| **主键必设** | 核心表必须设置主键，关联表设置外键 |
| **合理索引** | 常用查询字段建立索引，提升查询效率 |

### 6.2 性能优化

| 策略 | 说明 |
|------|------|
| 避免 `SELECT *` | 只查询需要的字段，减少数据传输 |
| 分页查询 | 使用 `LIMIT` 避免一次性查询大量数据 |
| 优先优化 SQL | 复杂查询优先优化 SQL，而非增加硬件 |

### 6.3 安全规范

| 规范 | 说明 |
|------|------|
| **防 SQL 注入** | 禁止拼接 SQL 字符串，使用 `PreparedStatement` / MyBatis `#{}` |
| **最小权限** | 数据库用户仅授予最小必要权限 |
| **定期备份** | 定时备份数据，防止数据丢失 |

---

## 7. 核心总结

> **关系型数据库**以二维表存储数据，支持 SQL 和事务，遵循 ACID 原则。

| 维度 | 核心内容 |
|------|----------|
| **核心概念** | 数据库、表、主键、外键、索引 |
| **表关系** | 一对一、一对多、多对多（通过外键建立） |
| **SQL 分类** | DDL（定义）、DML（操作）、DQL（查询）、DCL（控制） |
| **事务保障** | 订单生成等核心场景必须使用事务保证数据一致性 |
| **性能安全** | 通过索引优化查询性能，防止 SQL 注入保证安全 |

---

> 📖 **相关阅读：** [数据库-引言](./数据库-引言.md) | [数据库-SQL](./数据库-SQL.md) | [数据库-事务](./数据库-事务.md) | [非关系型数据库 核心知识点](./非关系型数据库%20核心知识点.md)
