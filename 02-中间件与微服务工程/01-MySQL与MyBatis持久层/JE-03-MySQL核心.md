# MySQL 核心

> **数据库是应用的数据心脏 —— 理解 MySQL 的存储引擎、索引原理、事务与锁机制，是从"能用数据库"到"用好数据库"的关键跨越。**

---

## 目录

- [1. MySQL 架构概述](#1-mysql-架构概述)
- [2. 存储引擎](#2-存储引擎)
- [3. 数据类型深度解析](#3-数据类型深度解析)
- [4. SQL 基础：DDL 与 DML](#4-sql-基础ddl-与-dml)
- [5. 查询进阶：JOIN、子查询、聚合](#5-查询进阶join子查询聚合)
- [6. 窗口函数与 CTE](#6-窗口函数与-cte)
- [7. 索引原理](#7-索引原理)
- [8. 索引类型与使用](#8-索引类型与使用)
- [9. EXPLAIN 深度解析](#9-explain-深度解析)
- [10. 事务与 ACID](#10-事务与-acid)
- [11. MVCC（多版本并发控制）](#11-mvcc多版本并发控制)
- [12. 锁机制](#12-锁机制)
- [13. 日志系统：Undo Log、Redo Log、Binlog](#13-日志系统undo-logredo-logbinlog)
- [14. SQL 优化](#14-sql-优化)
- [15. 表设计与架构优化](#15-表设计与架构优化)
- [16. 最佳实践与常见陷阱](#16-最佳实践与常见陷阱)
- [17. 面试题](#17-面试题)

---

## 1. MySQL 架构概述

### 1.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         MySQL Server                              │
│                                                                   │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │                     Connectors                              │   │
│  │    JDBC      ODBC      .NET      Python      ...           │   │
│  └───────────────────────────────────────────────────────────┘   │
│                              │                                    │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │                   Connection Pool                          │   │
│  │    Authentication    Thread Reuse    Connection Limits     │   │
│  └───────────────────────────────────────────────────────────┘   │
│                              │                                    │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │                SQL Interface / Parser                       │   │
│  │    DDL      DML      Stored Procedures     Views          │   │
│  └───────────────────────────────────────────────────────────┘   │
│                              │                                    │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │                     Optimizer                               │   │
│  │    Query Rewriting    Access Path Selection    Join Order   │   │
│  └───────────────────────────────────────────────────────────┘   │
│                              │                                    │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │                      Executor                                │   │
│  │    DDL/DML Operations    Query Execution    Return          │   │
│  └───────────────────────────────────────────────────────────┘   │
│                              │                                    │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │                   Storage Engine Layer                      │   │
│  │    ┌──────────┐  ┌──────────┐  ┌──────────┐                │   │
│  │    │  InnoDB  │  │  MyISAM  │  │  Memory  │   ...          │   │
│  │    └──────────┘  └──────────┘  └──────────┘                │   │
│  └───────────────────────────────────────────────────────────┘   │
│                              │                                    │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │                      File System                            │   │
│  │    Data Files    Index Files    Log Files    Redo Log       │   │
│  └───────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 各层职责

| 层级 | 组件 | 职责 |
|------|------|------|
| **连接层** | Connection Pool | 管理客户端连接、认证、线程复用 |
| **服务层** | SQL Interface | 接收 SQL 语句、存储过程、视图管理 |
| **服务层** | Parser | 词法/语法分析，生成解析树 |
| **服务层** | Optimizer | 查询优化，选择最优执行计划 |
| **服务层** | Executor | 执行查询计划，调用存储引擎接口 |
| **存储引擎层** | InnoDB/MyISAM 等 | 数据存储与检索、事务管理 |
| **文件系统层** | File System | 数据文件、日志文件的实际存储 |

### 1.3 查询执行流程

```
SQL: SELECT * FROM users WHERE age > 18 ORDER BY name;
                                                    ↓
Step 1: 连接器
  验证用户身份和权限，从连接池分配线程
                                                    ↓
Step 2: 查询缓存（MySQL 8.0 已移除）
  如果缓存命中则直接返回（8.0 不再支持查询缓存）
                                                    ↓
Step 3: 分析器
  词法分析：识别 SELECT、FROM、WHERE 等关键字
  语法分析：检查 SQL 是否符合语法规则 → 生成解析树
                                                    ↓
Step 4: 优化器
  选择索引：使用哪个索引（或全表扫描）
  决定 JOIN 顺序：先关联哪张表
  生成执行计划
                                                    ↓
Step 5: 执行器
  根据执行计划调用存储引擎接口
  存储引擎读取数据并返回
                                                    ↓
Step 6: 返回结果
  将结果集返回给客户端
```

---

## 2. 存储引擎

### 2.1 InnoDB vs MyISAM 对比

| 特性 | InnoDB | MyISAM |
|------|--------|--------|
| **事务支持** | 支持（ACID） | 不支持 |
| **锁粒度** | 行级锁 | 表级锁 |
| **外键** | 支持 | 不支持 |
| **索引实现** | 聚簇索引（数据在索引叶子节点） | 非聚簇索引（数据和索引分离） |
| **MVCC** | 支持 | 不支持 |
| **崩溃恢复** | 支持（Redo Log + Undo Log） | 不支持（损坏后需修复） |
| **全文索引** | 支持（MySQL 5.6+） | 支持 |
| **缓存** | 缓冲池（Buffer Pool）缓存数据和索引 | 仅缓存索引（Key Cache） |
| **表空间** | .ibd（独立表空间）或 ibdata1（共享） | .MYD（数据） + .MYI（索引） |
| **压缩** | 支持（表/页压缩） | 支持（压缩表） |
| **行格式** | COMPACT、REDUNDANT、DYNAMIC、COMPRESSED | FIXED、DYNAMIC、COMPRESSED |
| **存储限制** | 64TB（文件系统上限） | 256TB |

### 2.2 InnoDB 核心特性

**1. 行级锁**

```sql
-- InnoDB 只锁定需要操作的记录行
-- MyISAM 会锁定整张表
-- 高并发场景下 InnoDB 性能远优于 MyISAM
```

**2. 外键约束**

```sql
CREATE TABLE orders (
    id INT PRIMARY KEY,
    user_id INT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

**3. 崩溃安全恢复**

```sql
-- InnoDB 通过 Redo Log 实现 crash-safe
-- 即使服务器突然宕机，重启后可以恢复未完成的事务
```

**4. MVCC（多版本并发控制）**

```sql
-- MVCC 允许多个事务同时读取同一行数据，而不互相阻塞
-- 每个事务看到的是数据的一个"快照"
```

### 2.3 如何选择合适的存储引擎

```
选择 InnoDB（99% 的场景）:
  ✓ 需要事务支持
  ✓ 高并发读写
  ✓ 需要行级锁
  ✓ 需要外键支持
  ✓ 需要崩溃恢复能力

选择 MyISAM（极少数场景）:
  ✓ 只读或读多写少的日志/统计表
  ✓ MySQL 5.5 之前的系统表
  × 大部分场景已不推荐使用
  × 注意：MyISAM 不支持事务，崩溃后可能丢失数据

选择 Memory:
  ✓ 临时表（数据量小，速度快）
  × 重启后数据丢失
  × 不支持大对象（TEXT/BLOB）
  × 表级锁，并发性能差

选择 CSV:
  ✓ 需要直接编辑 CSV 文件
  × 不支持索引
  × 不支持 NULL
```

### 2.4 查看和修改存储引擎

```sql
-- 查看当前默认存储引擎
SHOW VARIABLES LIKE 'default_storage_engine';

-- 查看表存储引擎
SHOW TABLE STATUS LIKE 'users';
SELECT ENGINE FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'database_name' AND TABLE_NAME = 'users';

-- 创建时指定存储引擎
CREATE TABLE my_table (
    id INT PRIMARY KEY
) ENGINE=InnoDB;

-- 修改存储引擎
ALTER TABLE my_table ENGINE=InnoDB;
-- 注意：修改引擎会重建表和索引，大表可能耗时很长
```

---

## 3. 数据类型深度解析

### 3.1 整数类型

| 类型 | 存储字节 | 有符号范围 | 无符号范围 |
|------|---------|-----------|-----------|
| TINYINT | 1 | -128 ~ 127 | 0 ~ 255 |
| SMALLINT | 2 | -32,768 ~ 32,767 | 0 ~ 65,535 |
| MEDIUMINT | 3 | -8,388,608 ~ 8,388,607 | 0 ~ 16,777,215 |
| INT | 4 | -2^31 ~ 2^31-1 | 0 ~ 4,294,967,295 |
| BIGINT | 8 | -2^63 ~ 2^63-1 | 0 ~ 18,446,744,073,709,551,615 |

```sql
-- 选择原则：在满足存储需求的情况下选择最小的类型
-- 用户年龄：TINYINT UNSIGNED (0-255)
age TINYINT UNSIGNED NOT NULL

-- 日常 ID：INT UNSIGNED (42亿，大部分场景够用)
id INT UNSIGNED NOT NULL AUTO_INCREMENT

-- 海量数据 ID：BIGINT（如订单号、分布式 ID）
order_id BIGINT UNSIGNED NOT NULL

-- MySQL 8.0.17+，INT(N) 的 N 不再表示存储限制
-- INT(4) 和 INT(11) 存储空间相同
-- 旧的 INT(N) + ZEROFILL 语法已废弃
```

### 3.2 小数类型

```sql
-- DECIMAL(N, D)：精确小数（推荐用于金额）
-- N = 总位数（最大 65），D = 小数位数
price DECIMAL(10, 2) NOT NULL  -- 99999999.99，金额存储

-- FLOAT / DOUBLE：近似浮点数（不推荐用于精确计算）
score FLOAT(7, 4)             -- 7 位有效数字

-- 对比：
-- DECIMAL(10,2): 精确存储，适合金融场景
-- FLOAT:        4 字节，近似，适合科学计算
-- DOUBLE:       8 字节，近似，精度高于 FLOAT

-- 最佳实践：金额用 DECIMAL，绝不用 FLOAT/DOUBLE
-- 问题示例：
SELECT 0.1 + 0.2;  -- 0.3（DECIMAL 精确）
SELECT 0.1F + 0.2F; -- 0.300000004（FLOAT 不精确！）
```

### 3.3 字符串类型

| 类型 | 最大长度 | 存储方式 | 适用场景 |
|------|---------|---------|---------|
| CHAR(N) | 255 字符 | 固定长度（不足空格填充） | 定长数据：MD5、手机号、身份证 |
| VARCHAR(N) | 65,535 字节（受行大小限制） | 变长，额外 1-2 字节存长度 | 变长数据：用户名、邮箱、地址 |
| TINYTEXT | 255 字节 | 长度前缀 + 数据 | 短文本 |
| TEXT | 65,535 字节 (~64KB) | 长度前缀 + 数据 | 文章正文 |
| MEDIUMTEXT | 16,777,215 字节 (~16MB) | 长度前缀 + 数据 | 中长文本 |
| LONGTEXT | 4,294,967,295 字节 (~4GB) | 长度前缀 + 数据 | 长文档 |

```sql
-- CHAR vs VARCHAR 选择
-- CHAR(11)：手机号，固定 11 位，CHAR 更高效
phone CHAR(11) NOT NULL

-- VARCHAR(50)：用户名，长度可变，VARCHAR 节省空间
username VARCHAR(50) NOT NULL

-- VARCHAR(N) 的 N 单位是字符（不是字节）
-- utf8mb4 下一个中文字符占 3-4 字节
-- VARCHAR(255) 以内的 N 有性能优势（额外 1 字节长度前缀）
-- 超过 255 需要 2 字节长度前缀

-- TEXT 注意事项：
-- 1. TEXT 不能设置默认值
-- 2. TEXT 需要单独的回滚段，不能和行存在一起（溢出页）
-- 3. 排序/分组会使用磁盘临时表（性能差）
-- 4. TEXT 列不能有主键索引（但可以有普通索引）
```

### 3.4 日期时间类型

| 类型 | 存储字节 | 范围 | 精度 | 时区相关 |
|------|---------|------|------|---------|
| DATE | 3 | 1000-01-01 ~ 9999-12-31 | 日 | 否 |
| TIME | 3 | -838:59:59 ~ 838:59:59 | 秒（可达微秒） | 否 |
| DATETIME | 8 | 1000-01-01 00:00:00 ~ 9999-12-31 23:59:59 | 秒（可达微秒） | 否 |
| TIMESTAMP | 4 | 1970-01-01 00:00:01 ~ 2038-01-19 03:14:07 | 秒（可达微秒） | **是** |
| YEAR | 1 | 1901 ~ 2155 | 年 | 否 |

```sql
-- DATETIME vs TIMESTAMP 选择
-- TIMESTAMP 存储 UTC 时间戳，时区转换自动
-- DATETIME 存储字面时间，不涉及时区

-- 创建时间：两种都可，推荐 DATETIME（范围更大）
created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP

-- 更新时间：自动更新
updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP

-- TIMESTAMP 的 2038 年问题
-- MySQL 8.0.28+ 支持 TIMESTAMP 到 2038 年后（需启用）
-- 建议使用 DATETIME 避免"2038年问题"

-- MySQL 5.6.4+ 支持小数秒（最高微秒精度）
started_at DATETIME(3) NOT NULL   -- 毫秒精度
log_time DATETIME(6) NOT NULL     -- 微秒精度（适合日志）
```

### 3.5 JSON 类型（MySQL 5.7+）

```sql
-- 创建 JSON 列
CREATE TABLE products (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    attributes JSON
);

-- 插入 JSON 数据
INSERT INTO products VALUES (
    1,
    'iPhone 15',
    '{"color": "black", "storage": 256, "features": ["5G", "Face ID"]}'
);

-- JSON 查询函数
-- 提取字段
SELECT name,
       JSON_EXTRACT(attributes, '$.color') AS color,
       attributes->>'$.storage' AS storage
FROM products;

-- JSON 路径索引（MySQL 8.0.17+ 多值索引）
ALTER TABLE products ADD INDEX idx_storage (
    (CAST(attributes->>'$.storage' AS UNSIGNED))
);

-- JSON 条件查询
SELECT * FROM products
WHERE JSON_CONTAINS(attributes->>'$.features', '"5G"');

-- 修改 JSON 字段
UPDATE products
SET attributes = JSON_SET(attributes, '$.price', 9999)
WHERE id = 1;

-- JSON 数组操作
SELECT JSON_ARRAY_APPEND(attributes, '$.features', 'MagSafe')
FROM products WHERE id = 1;

-- JSON_TABLE：将 JSON 转为关系表
SELECT p.name, jt.*
FROM products p,
     JSON_TABLE(p.attributes, '$'
         COLUMNS (
             color VARCHAR(20) PATH '$.color',
             storage INT PATH '$.storage'
         )
     ) AS jt;
```

### 3.6 ENUM 和 SET（谨慎使用）

```sql
-- ENUM：枚举类型，存储单值
status ENUM('active', 'inactive', 'banned') NOT NULL DEFAULT 'active'

-- SET：集合类型，存储多值
permissions SET('read', 'write', 'delete', 'admin') NOT NULL

-- 为什么不推荐 ENUM/SET
-- 1. 修改枚举值需要 ALTER TABLE（阻塞 DDL）
-- 2. 数据迁移麻烦（导出时需要特殊处理）
-- 3. 跨数据库兼容性差
-- 4. 排序规则奇怪（按索引值排序，不是字母顺序）

-- 替代方案
-- 方案1：使用 TINYINT + 代码映射
status TINYINT NOT NULL DEFAULT 1 COMMENT '1-正常 2-禁用 3-已删除'

-- 方案2：使用关联表（适合经常变化的枚举）
CREATE TABLE user_statuses (
    id TINYINT PRIMARY KEY,
    name VARCHAR(20) NOT NULL
);
```

### 3.7 类型选择原则

```
1. 够用就好：选择能存储数据的最小类型
   - INT 能存下就用 INT，别用 BIGINT
   - VARCHAR(20) 够用就别用 VARCHAR(255)

2. 避免 TEXT/BLOB：尽量将大字段拆到独立表
   - TEXT 会在溢出页存储，增加 IO

3. 金额用 DECIMAL，不用 FLOAT/DOUBLE
   - 浮点数有精度损失

4. 主键用整数自增或 UUID（有序 UUID）
   - 不要用 VARCHAR 做主键

5. 时间用 DATETIME（建议），避免 TIMESTAMP（2038 问题）

6. 枚举用 TINYINT + 注释，不用 ENUM

7. 能用 NOT NULL 就用，减少判断复杂度
```

---

## 4. SQL 基础：DDL 与 DML

### 4.1 数据库操作

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS myapp
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 查看所有数据库
SHOW DATABASES;

-- 选择数据库
USE myapp;

-- 修改数据库字符集
ALTER DATABASE myapp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 删除数据库
DROP DATABASE IF EXISTS myapp;
```

### 4.2 表操作

```sql
-- 创建表
CREATE TABLE IF NOT EXISTS users (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone CHAR(11) DEFAULT NULL COMMENT '手机号',
    avatar VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    role TINYINT NOT NULL DEFAULT 0 COMMENT '角色: 0-普通 1-管理员',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    INDEX idx_created_at (created_at),
    INDEX idx_role_status (role, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 查看表结构
DESC users;
SHOW CREATE TABLE users;

-- 修改表
-- 添加列
ALTER TABLE users ADD COLUMN birthday DATE DEFAULT NULL AFTER email;

-- 修改列类型
ALTER TABLE users MODIFY COLUMN username VARCHAR(100) NOT NULL;

-- 重命名列
ALTER TABLE users CHANGE COLUMN birthday birth_date DATE;

-- 删除列
ALTER TABLE users DROP COLUMN birthday;

-- 添加索引
ALTER TABLE users ADD INDEX idx_phone (phone);
CREATE INDEX idx_phone ON users(phone);

-- 删除索引
ALTER TABLE users DROP INDEX idx_phone;
DROP INDEX idx_phone ON users;

-- 重命名表
RENAME TABLE users TO app_users;

-- 截断表（清空数据，不可回滚）
TRUNCATE TABLE users;

-- 删除表
DROP TABLE IF EXISTS users;
```

### 4.3 约束

```sql
CREATE TABLE orders (
    id INT UNSIGNED AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL,
    user_id INT UNSIGNED NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 主键约束
    PRIMARY KEY (id),

    -- 唯一约束
    UNIQUE KEY uk_order_no (order_no),

    -- 外键约束（InnoDB 支持）
    CONSTRAINT fk_user_id FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT   -- 禁止删除有关联的用户
        ON UPDATE CASCADE,   -- 用户 ID 更新时自动更新

    -- 检查约束（MySQL 8.0.16+）
    CONSTRAINT chk_amount CHECK (amount > 0),
    CONSTRAINT chk_status CHECK (status BETWEEN 0 AND 4),

    -- 索引
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 外键约束行为选项
-- RESTRICT / NO ACTION：禁止删除/更新
-- CASCADE：级联删除/更新
-- SET NULL：删除/更新后设置为 NULL
-- SET DEFAULT：删除/更新后设为默认值（InnoDB 不支持）
```

### 4.4 CRUD 操作

```sql
-- INSERT
-- 单条插入
INSERT INTO users (username, password, email)
VALUES ('john', 'hash123', 'john@example.com');

-- 批量插入（高效）
INSERT INTO users (username, password, email) VALUES
    ('alice', 'hash1', 'alice@example.com'),
    ('bob', 'hash2', 'bob@example.com'),
    ('charlie', 'hash3', 'charlie@example.com');

-- 插入时处理重复
-- 方式1：忽略重复（不报错）
INSERT IGNORE INTO users (id, username) VALUES (1, 'john');

-- 方式2：重复时更新
INSERT INTO users (id, username, email) VALUES (1, 'john', 'new@email.com')
ON DUPLICATE KEY UPDATE email = VALUES(email);

-- 方式3：替换（先删后插）
REPLACE INTO users (id, username, email) VALUES (1, 'john', 'new@email.com');

-- SELECT
-- 基础查询
SELECT id, username, email FROM users WHERE status = 1;

-- 分页查询
SELECT * FROM users
WHERE status = 1
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;    -- 第一页（MySQL 的 OFFSET 从 0 开始）
-- 或
SELECT * FROM users
WHERE status = 1
ORDER BY created_at DESC
LIMIT 0, 20;           -- LIMIT offset, count

-- 去重
SELECT DISTINCT role FROM users;

-- 条件查询
SELECT * FROM users
WHERE username LIKE 'john%'      -- LIKE 前缀匹配可用索引
  AND email IS NOT NULL
  AND id IN (1, 2, 3)
  AND created_at BETWEEN '2024-01-01' AND '2024-12-31'
  AND (role = 1 OR status = 0);

-- UPDATE
UPDATE users SET email = 'new@example.com'
WHERE id = 1;

-- 关联更新
UPDATE users u
JOIN orders o ON u.id = o.user_id
SET u.status = 0
WHERE o.created_at < '2023-01-01';

-- DELETE
DELETE FROM users WHERE id = 100;

-- 关联删除
DELETE u FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE o.id IS NULL;   -- 删除没有订单的用户
```

---

## 5. 查询进阶：JOIN、子查询、聚合

### 5.1 JOIN 图解

```sql
-- 示例表：users（用户）和 orders（订单）
-- users: id, username
-- orders: id, user_id, amount
```

**INNER JOIN（内连接）：只返回匹配的行**

```
SELECT * FROM users u
INNER JOIN orders o ON u.id = o.user_id;

  users                    orders
┌─────┬──────────┐   ┌─────┬─────────┬────────┐
│ id  │ username │   │ id  │ user_id │ amount │
├─────┼──────────┤   ├─────┼─────────┼────────┤
│ 1   │ alice    │   │ 1   │ 1       │ 100    │
│ 2   │ bob      │ ←→ │ 2   │ 1       │ 200    │
│ 3   │ charlie  │   │ 3   │ 2       │ 150    │
│ 4   │ david    │   └─────┴─────────┴────────┘
└─────┴──────────┘

           INNER JOIN
  ┌───────────────────────────┐
  │ alice → 2 条订单          │
  │ bob   → 1 条订单          │
  │ charlie 和 david: 没有订单 │
  └───────────────────────────┘
```

**LEFT JOIN（左连接）：返回左表所有行，匹配不上的右表为 NULL**

```
SELECT * FROM users u
LEFT JOIN orders o ON u.id = o.user_id;

  ┌───────────────────────────┐
  │ alice   → order#1, order#2│
  │ bob     → order#3         │
  │ charlie → null（无订单）    │
  │ david   → null（无订单）    │
  └───────────────────────────┘
```

**RIGHT JOIN（右连接）：返回右表所有行**

```
SELECT * FROM users u
RIGHT JOIN orders o ON u.id = o.user_id;

  ┌───────────────────────────┐
  │ order#1 → alice           │
  │ order#2 → alice           │
  │ order#3 → bob             │
  │ 所有订单都有对应用户       │
  └───────────────────────────┘
```

**CROSS JOIN（交叉连接/笛卡尔积）：所有组合**

```sql
SELECT * FROM users CROSS JOIN orders;
-- users 4 条 × orders 3 条 = 12 条结果
-- 通常不直接使用（结果过大），可用于生成测试数据
```

### 5.2 JOIN 完整示例

```sql
-- INNER JOIN
SELECT u.username, o.id AS order_id, o.amount, o.created_at
FROM users u
INNER JOIN orders o ON u.id = o.user_id;

-- LEFT JOIN（包含没有订单的用户）
SELECT u.username, COUNT(o.id) AS order_count, COALESCE(SUM(o.amount), 0) AS total
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id, u.username;

-- 自连接（Self JOIN）：查找员工和经理
SELECT e.name AS employee, m.name AS manager
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.id;

-- 多表 JOIN
SELECT u.username, o.id AS order_id, oi.product_name, oi.quantity
FROM users u
JOIN orders o ON u.id = o.user_id
JOIN order_items oi ON o.id = oi.order_id
WHERE u.id = 1;
```

### 5.3 子查询

**非关联子查询：子查询独立执行，先运行子查询再运行外层查询**

```sql
-- 查询价格高于平均价格的订单
SELECT * FROM orders
WHERE amount > (SELECT AVG(amount) FROM orders);

-- IN 子查询
SELECT * FROM users
WHERE id IN (SELECT user_id FROM orders WHERE amount > 100);
```

**关联子查询：子查询引用外层表的列，外层每行执行一次子查询**

```sql
-- 查询每个用户的最新订单
SELECT u.id, u.username, o.id AS order_id, o.amount, o.created_at
FROM users u
JOIN orders o ON u.id = o.user_id
WHERE o.created_at = (
    SELECT MAX(created_at)
    FROM orders
    WHERE user_id = u.id
);

-- EXISTS（通常比 IN 更快，因为可以提前终止）
SELECT * FROM users u
WHERE EXISTS (
    SELECT 1 FROM orders o
    WHERE o.user_id = u.id AND o.amount > 1000
);
-- 只要找到一条满足条件的记录就返回 true，不需要扫描全部
```

**EXISTS vs IN 性能对比：**

```sql
-- 当子查询结果集很小时，IN 效率高
SELECT * FROM users WHERE id IN (1, 2, 3);

-- 当外部表小、子查询表大时，EXISTS 效率高
SELECT * FROM users u
WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id);

-- 注意：NOT IN 和 NOT EXISTS 差异更大
-- NOT IN 如果子查询结果包含 NULL，整个查询返回空结果！
-- NOT EXISTS 不受 NULL 影响
SELECT * FROM users WHERE id NOT IN (SELECT user_id FROM orders);
-- 如果 orders.user_id 有 NULL，此查询返回空！

SELECT * FROM users u
WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id);
-- 安全版本，不受 NULL 影响
```

### 5.4 聚合查询

```sql
-- 基本聚合函数
SELECT
    COUNT(*) AS total_users,           -- 总记录数
    COUNT(email) AS has_email,         -- email 非空的记录数
    COUNT(DISTINCT role) AS role_count, -- 不同角色数量
    MIN(created_at) AS earliest,       -- 最早创建时间
    MAX(created_at) AS latest,         -- 最近创建时间
    AVG(amount) AS avg_amount,         -- 平均金额
    SUM(amount) AS total_amount         -- 总金额
FROM users u
LEFT JOIN orders o ON u.id = o.user_id;

-- GROUP BY + HAVING
SELECT
    u.role,
    COUNT(*) AS user_count,
    AVG(o.amount) AS avg_order_amount,
    SUM(o.amount) AS total_amount
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE u.status = 1
GROUP BY u.role
HAVING user_count > 10       -- HAVING 过滤分组后的结果
ORDER BY total_amount DESC;

-- GROUP BY 注意点（MySQL 8.0+ 默认启用 ONLY_FULL_GROUP_BY）
-- SELECT 中的非聚合列必须出现在 GROUP BY 中
-- 以下 SQL 在 ONLY_FULL_GROUP_BY 下会报错：
SELECT u.username, u.role, COUNT(*)
FROM users u
GROUP BY u.role;  -- username 不在 GROUP BY 中！

-- ROLLUP：分组汇总
SELECT
    COALESCE(username, '总计') AS username,
    SUM(amount) AS total
FROM users u
JOIN orders o ON u.id = o.user_id
GROUP BY u.username WITH ROLLUP;
```

---

## 6. 窗口函数与 CTE

### 6.1 窗口函数（MySQL 8.0+）

窗口函数在不改变行数的情况下对行进行计算，与 GROUP BY 不同，GROUP BY 会折叠行。

```sql
-- 表：scores
-- id | student | subject | score
-- 1  | alice   | Math    | 95
-- 2  | bob     | Math    | 87
-- 3  | alice   | English | 92
-- 4  | bob     | English | 85
```

**ROW_NUMBER()：为每组中的行编号**

```sql
-- 每个学生按分数排名
SELECT
    student,
    subject,
    score,
    ROW_NUMBER() OVER (PARTITION BY student ORDER BY score DESC) AS rn
FROM scores;

-- 结果：
-- alice | Math    | 95 | 1
-- alice | English | 92 | 2
-- bob   | Math    | 87 | 1
-- bob   | English | 85 | 2
```

**RANK() 和 DENSE_RANK()：排名函数**

```sql
SELECT
    student,
    score,
    RANK() OVER (ORDER BY score DESC) AS rank_,
    DENSE_RANK() OVER (ORDER BY score DESC) AS dense_rank_
FROM scores;

-- 如果分数相同：
-- alice | 95 | 1 | 1
-- bob   | 95 | 1 | 1   (RANK 和 DENSE_RANK 相同)
-- charlie | 90 | 3 | 2  (RANK 跳过 2，DENSE_RANK 不跳过)
-- david  | 85 | 4 | 3
-- RANK 有间隔，DENSE_RANK 无间隔
```

**LAG() 和 LEAD()：访问前/后行**

```sql
-- 计算每个用户的订单间隔
SELECT
    user_id,
    order_date,
    LAG(order_date) OVER (PARTITION BY user_id ORDER BY order_date) AS prev_order,
    DATEDIFF(order_date, LAG(order_date) OVER (
        PARTITION BY user_id ORDER BY order_date
    )) AS days_since_last_order
FROM orders;

-- LEAD 同理：获取下一行的值
```

**SUM()/AVG() OVER：移动聚合**

```sql
-- 累计求和（running total）
SELECT
    order_date,
    amount,
    SUM(amount) OVER (ORDER BY order_date) AS running_total
FROM orders;

-- 移动平均（最近 3 天）
SELECT
    order_date,
    amount,
    AVG(amount) OVER (
        ORDER BY order_date
        ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
    ) AS moving_avg
FROM orders;

-- 窗口帧（Window Frame）
-- ROWS BETWEEN ... AND ... 可选项：
-- UNBOUNDED PRECEDING: 到当前行之前的所有行
-- N PRECEDING: 前 N 行
-- CURRENT ROW: 当前行
-- N FOLLOWING: 后 N 行
-- UNBOUNDED FOLLOWING: 到当前行之后的所有行
```

### 6.2 CTE（Common Table Expressions，MySQL 8.0+）

CTE 让复杂查询更清晰，可复用。

```sql
-- 基本 CTE
WITH user_stats AS (
    SELECT
        u.id,
        u.username,
        COUNT(o.id) AS order_count,
        SUM(o.amount) AS total_spent
    FROM users u
    LEFT JOIN orders o ON u.id = o.user_id
    GROUP BY u.id, u.username
)
SELECT * FROM user_stats
WHERE order_count > 5
ORDER BY total_spent DESC;

-- 多 CTE
WITH
high_value_users AS (
    SELECT user_id, SUM(amount) AS total
    FROM orders
    GROUP BY user_id
    HAVING total > 10000
),
recent_orders AS (
    SELECT * FROM orders
    WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
)
SELECT hvu.*, COUNT(ro.id) AS recent_order_count
FROM high_value_users hvu
LEFT JOIN recent_orders ro ON hvu.user_id = ro.user_id
GROUP BY hvu.user_id, hvu.total;
```

**递归 CTE：**

```sql
-- 查询组织架构树
WITH RECURSIVE org_tree AS (
    -- 锚点：根节点（顶级部门）
    SELECT id, name, parent_id, 1 AS level, CAST(name AS CHAR(255)) AS path
    FROM departments
    WHERE parent_id IS NULL

    UNION ALL

    -- 递归：子部门
    SELECT d.id, d.name, d.parent_id, t.level + 1,
           CONCAT(t.path, ' > ', d.name)
    FROM departments d
    JOIN org_tree t ON d.parent_id = t.id
)
SELECT * FROM org_tree ORDER BY path;

-- 生成数字序列
WITH RECURSIVE numbers AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n < 100
)
SELECT * FROM numbers;
```

---

## 7. 索引原理

### 7.1 为什么需要索引？

```sql
-- 没有索引：全表扫描
SELECT * FROM users WHERE username = 'john';
-- 需要扫描 users 表的所有行（假设 100 万行）
-- 磁盘 IO: 读取所有数据页

-- 有索引：B+Tree 搜索
SELECT * FROM users WHERE username = 'john';
-- 通过 B+Tree 索引快速定位（3-4 层树高）
-- 磁盘 IO: 读取 3-4 个索引页 + 1 个数据页
```

### 7.2 B+Tree 数据结构

```
B+Tree 结构（以 3 层为例）：

                    ┌─────────────────────────────────────────┐
                    │   根节点（索引页）                       │
                    │   [50, 100, 150, 200]                   │
                    └──────┬──────┬──────┬──────┬───────────┘
                           │      │      │      │
               ┌───────────┘      │      │      └───────────┐
               ▼                  ▼      ▼                  ▼
         ┌──────────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐
         │ 内部节点      │  │ 内部节点  │  │ 内部节点  │  │ 内部节点      │
         │ [10,30,50]   │  │ [60,80]  │  │ [110,130]│  │ [170,190]    │
         └─┬──┬──┬──┬──┘  └─┬──┬──┬──┘  └─┬──┬──┬──┘  └─┬──┬──┬──┬──┘
           │  │  │  │       │  │  │       │  │  │       │  │  │  │
           ▼  ▼  ▼  ▼       ▼  ▼  ▼       ▼  ▼  ▼       ▼  ▼  ▼  ▼
┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐     ┌──────┐     ┌──────┐
│叶子页│ │叶子页│ │叶子页│ │叶子页│ ... │叶子页│ ... │叶子页│
│1-9   │ │11-29 │ │31-49 │ │51-59 │     │61-79 │     │191-199│
└──────┘ └──────┘ └──────┘ └──────┘     └──────┘     └──────┘
   │        │        │        │            │            │
   └────────┴────────┴────────┴────────────┴────────────┘
                  ↑ 叶子节点通过双向链表连接

特点：
1. 所有数据存储在叶子节点
2. 叶子节点形成有序双向链表（范围查询高效）
3. 内部节点只存索引键和指针
4. 树高通常 3-4 层（百万到千万级数据）
5. 每个节点对应一个磁盘页（默认 16KB）
```

### 7.3 B+Tree vs B-Tree

| 特性 | B-Tree | B+Tree |
|------|--------|--------|
| 数据存储 | 所有节点都存储数据 | **仅叶子节点存储数据** |
| 内部节点 | 存键+数据 | 只存键（不存数据） |
| 内部节点大小 | 小（含数据） | **大（可存更多键）** |
| 树高 | 较高 | **较低（扇出更大）** |
| 范围查询 | 需要中序遍历（来回跳） | **叶子链表直接遍历** |
| 性能稳定 | 访问不同键的 IO 次数不同 | 任何键访问次数相同（树高固定） |

### 7.4 聚簇索引 vs 二级索引（InnoDB）

**聚簇索引（Clustered Index）：**

```
┌──────────────────────────────────────────────────────┐
│                   聚簇索引 = 主键索引                   │
│                                                      │
│  InnoDB 表必然有一个聚簇索引：                         │
│  1. 定义了主键 → 主键作为聚簇索引                       │
│  2. 没有主键 → 第一个 NOT NULL UNIQUE 索引             │
│  3. 都没有 → 隐式生成 ROW_ID（6 字节，自增）            │
│                                                      │
│  聚簇索引特点：                                        │
│  - 索引的叶子节点直接存储整行数据                       │
│  - 数据按主键顺序物理存储                               │
│  - 主键查询只需一次索引查找                             │
│                                                      │
│  B+Tree（聚簇索引）                                    │
│  ┌──────┐  ┌──────┐  ┌──────┐                        │
│  │ id=1  │  │ id=2  │  │ id=3  │  ...                │
│  │ name  │  │ name  │  │ name  │                      │
│  │ email │  │ email │  │ email │  完整数据行           │
│  │ ...   │  │ ...   │  │ ...   │                      │
│  └──────┘  └──────┘  └──────┘                        │
└──────────────────────────────────────────────────────┘
```

**二级索引（Secondary Index）：**

```
┌──────────────────────────────────────────────────────┐
│                   二级索引 = 普通索引                   │
│                                                      │
│  叶子节点存储：索引列的值 + 主键值                    │
│                                                      │
│  B+Tree（二级索引 on username）                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐           │
│  │ 'alice'  │  │ 'bob'    │  │ 'charlie'│  ...       │
│  │ id=3     │  │ id=1     │  │ id=2     │            │
│  └──────────┘  └──────────┘  └──────────┘            │
│       │             │             │                    │
│       └──────┬──────┴──────┬──────┘                   │
│              ▼             ▼                           │
│         聚簇索引查找      聚簇索引查找                   │
│         id=3 → 完整数据  id=1 → 完整数据               │
│                                                      │
│  ★ 通过二级索引查询需要两次索引查找（回表）：            │
│  第一步：二级索引找到主键                               │
│  第二步：通过主键在聚簇索引中找到完整数据                 │
└──────────────────────────────────────────────────────┘
```

### 7.5 索引维护成本

```
插入数据：
  - 主键自增：新数据追加到 B+Tree 末尾 → 无页分裂
  - UUID/VARCHAR 主键：随机插入 → 可能触发页分裂
  
页分裂：
  - 页面满了需要插入 → 分配新页，移动一半数据
  - 产生 IO 开销 + 存储碎片
  - 这就是自增主键比 UUID 主键快的根本原因

删除数据：
  - 标记删除（不立即回收空间）
  - 会造成空洞（碎片） → 需要 OPTIMIZE TABLE 重建

更新索引列：
  - = 删除旧键值 + 插入新键值
  - 索引列频繁更新会产生大量碎片
```

---

## 8. 索引类型与使用

### 8.1 索引类型

```sql
-- 1. 主键索引（PRIMARY KEY）
CREATE TABLE users (
    id INT PRIMARY KEY,   -- 自动创建聚簇索引
    ...
);

-- 2. 唯一索引（UNIQUE）
CREATE UNIQUE INDEX uk_email ON users(email);
-- 或
ALTER TABLE users ADD UNIQUE INDEX uk_email(email);

-- 3. 普通索引（INDEX）
CREATE INDEX idx_username ON users(username);
-- 或
ALTER TABLE users ADD INDEX idx_username(username);

-- 4. 全文索引（FULLTEXT，MyISAM / InnoDB 5.6+ 支持）
CREATE FULLTEXT INDEX ft_content ON articles(title, content);
-- 查询方式
SELECT * FROM articles
WHERE MATCH(title, content) AGAINST('database optimization');

-- 5. 空间索引（SPATIAL，MyISAM / InnoDB 5.7+）
CREATE SPATIAL INDEX idx_location ON places(location);

-- 6. 复合索引（Composite Index / Multi-Column Index）
CREATE INDEX idx_role_status ON users(role, status);
```

### 8.2 复合索引与最左前缀原则

```sql
-- 创建复合索引
CREATE INDEX idx_a_b_c ON users(col_a, col_b, col_c);

-- 走索引的查询：
WHERE col_a = 1                     -- ✓ 使用了 col_a
WHERE col_a = 1 AND col_b = 2       -- ✓ 使用了 col_a + col_b
WHERE col_a = 1 AND col_c = 3       -- ✓ 使用了 col_a（但 col_c 不走索引）
WHERE col_a = 1 AND col_b = 2 AND col_c = 3  -- ✓ 完全使用

-- 不走索引的查询：
WHERE col_b = 2                     -- ✗ 没用到 col_a
WHERE col_c = 3                     -- ✗ 没用到最左列
WHERE col_b = 2 AND col_c = 3       -- ✗ 没用到 col_a

-- 解释：MySQL 只能从最左侧开始匹配，跳过了最左列就无法使用复合索引

-- 复合索引列顺序原则
-- 原则1：区分度高的列放在前面
-- 原则2：经常作为等值查询的列放前面
-- 原则3：经常作为排序的列放前面
-- 原则4：范围查询的列放在后面（范围后的列无法使用索引）

-- 示例：用户查询场景
-- 常见查询：WHERE status = 1 AND role = 'admin' ORDER BY created_at DESC
-- 最优索引：(status, role, created_at)
-- status 最常用于过滤 → 放第一位
-- role 等值查询 → 放第二位
-- created_at 排序 → 放第三位
```

### 8.3 覆盖索引（Covering Index）

```sql
-- 覆盖索引：查询所需的所有列都在索引中，无需回表

-- 表结构
-- users(id PK, username, email, role, status, created_at)

-- 索引
CREATE INDEX idx_username ON users(username);

-- 查询 1：需要回表
SELECT username, email FROM users WHERE username = 'john';
-- 步骤：二级索引找到主键 → 回聚簇索引查 email

-- 查询 2：覆盖索引
SELECT username FROM users WHERE username = 'john';
-- 步骤：二级索引直接返回 username（索引中有 username）

-- 优化：创建复合索引实现覆盖
CREATE INDEX idx_uname_email ON users(username, email);

-- 现在查询 1 也不需要回表了！
SELECT username, email FROM users WHERE username = 'john';

-- 检查是否使用覆盖索引：EXPLAIN 的 Extra 列显示 "Using index"
```

### 8.4 索引下推（Index Condition Pushdown, ICP, MySQL 5.6+）

```sql
-- 没有 ICP 时：
-- 索引：idx_status_created(status, created_at)
-- 查询：SELECT * FROM users WHERE status = 1 AND created_at > '2024-01-01'

-- 无 ICP：
-- 1. 通过索引找到 status=1 的所有主键（假设 10 万条）
-- 2. 回表读取所有行的 created_at（10 万次回表）
-- 3. 在 Server 层过滤 created_at > '2024-01-01'

-- 有 ICP：
-- 1. 在索引遍历时，同时检查 status=1 AND created_at > '2024-01-01'
-- 2. 只对满足条件的行回表（假设 5000 次回表）
-- 3. 减少了 95% 的回表次数！

-- EXPLAIN 显示：Extra = "Using index condition"
```

### 8.5 多值索引（MySQL 8.0.17+）

```sql
-- 创建 JSON 列的多值索引
CREATE TABLE products (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    tags JSON,
    INDEX idx_tags ((CAST(tags->'$[*]' AS CHAR(20) ARRAY)))
);

-- 查询使用多值索引
SELECT * FROM products
WHERE 'electronics' MEMBER OF(tags->'$[*]');

SELECT * FROM products
WHERE JSON_CONTAINS(tags, '"electronics"');
```

### 8.6 什么时候索引会失效？

```sql
-- 1. LIKE 通配符开头
SELECT * FROM users WHERE username LIKE '%john';  -- ✗ 索引失效
SELECT * FROM users WHERE username LIKE 'john%';  -- ✓ 使用索引

-- 2. 对索引列使用函数
SELECT * FROM users WHERE YEAR(created_at) = 2024;  -- ✗ 索引失效
SELECT * FROM users WHERE created_at >= '2024-01-01'
    AND created_at < '2025-01-01';                  -- ✓ 使用索引

-- 3. 隐式类型转换
SELECT * FROM users WHERE phone = 13800138000;     -- ✗ phone 是 VARCHAR
SELECT * FROM users WHERE phone = '13800138000';   -- ✓ 与类型一致

-- 4. OR 条件跨越不同列
SELECT * FROM users WHERE username = 'john' OR email = 'john@example.com';
-- ✗ OR 两边的列分别有索引，但 OR 可能导致全表扫描
-- ✓ 改为 UNION ALL：
SELECT * FROM users WHERE username = 'john'
UNION ALL
SELECT * FROM users WHERE email = 'john@example.com';

-- 5. 复合索引跳过最左列
-- 索引 (a, b, c)
SELECT * FROM users WHERE b = 1 AND c = 2;  -- ✗ 跳过了 a

-- 6. 数据分布特殊
-- 当优化器认为全表扫描比索引更快时（如表中 90% 的行都满足条件）
-- 加了 LIMIT 可能让优化器选择索引
```

---

## 9. EXPLAIN 深度解析

### 9.1 EXPLAIN 输出格式

```sql
EXPLAIN SELECT * FROM users WHERE id = 1;
```

| id | select_type | table | type | possible_keys | key | key_len | ref | rows | Extra |
|----|-------------|-------|------|---------------|-----|---------|-----|------|-------|
| 1 | SIMPLE | users | const | PRIMARY | PRIMARY | 4 | const | 1 | Using index |

### 9.2 type 列详解（从最优到最差）

**system > const > eq_ref > ref > range > index > ALL**

```sql
-- 1. system：系统表，只有一行（极少见）
EXPLAIN SELECT * FROM mysql.user LIMIT 1;

-- 2. const：主键或唯一索引等值查询，最多返回一行
EXPLAIN SELECT * FROM users WHERE id = 1;
-- 最优级别，直接定位到一条记录

-- 3. eq_ref：JOIN 中主键/唯一索引等值匹配，每个匹配行最多返回一行
EXPLAIN SELECT * FROM users u
JOIN orders o ON u.id = o.user_id;
-- 每个订单的 user_id 在 users 表中恰好匹配一个用户

-- 4. ref：普通索引等值查询，返回多行
EXPLAIN SELECT * FROM users WHERE role = 'admin';
-- role 上只有普通索引，可能有多个 admin

-- 5. range：索引范围扫描
EXPLAIN SELECT * FROM users WHERE id BETWEEN 1 AND 100;
EXPLAIN SELECT * FROM users WHERE id IN (1, 2, 3);
EXPLAIN SELECT * FROM users WHERE username > 'a';

-- 6. index：扫描整个索引树（比 ALL 稍好，因为索引比数据小）
EXPLAIN SELECT username FROM users;
-- username 在索引中，且只查 username → 扫描索引而不是表

-- 7. ALL：全表扫描（最差）
EXPLAIN SELECT * FROM users WHERE email LIKE '%@example.com';
-- 没有合适的索引，需要扫描整张表
```

### 9.3 Extra 列详解

```sql
-- Using index：覆盖索引，不需要回表
EXPLAIN SELECT username FROM users WHERE username = 'john';
-- Extra: Using index

-- Using index condition：索引下推（ICP）
EXPLAIN SELECT * FROM users
WHERE status = 1 AND created_at > '2024-01-01';
-- Extra: Using index condition

-- Using where：Server 层过滤数据
EXPLAIN SELECT * FROM users WHERE email LIKE '%@example.com';
-- Extra: Using where

-- Using filesort：需要额外排序（没有用到索引排序）
EXPLAIN SELECT * FROM users ORDER BY created_at DESC;
-- Extra: Using filesort
-- 意味着 MySQL 需要将数据读到内存（或磁盘临时表）中排序

-- Using temporary：使用临时表（通常与 GROUP BY/DISTINCT 有关）
EXPLAIN SELECT role, COUNT(*) FROM users GROUP BY role;
-- Extra: Using temporary

-- Using join buffer：JOIN 没有使用索引
EXPLAIN SELECT * FROM users u
LEFT JOIN orders o ON u.email = o.note;
-- Extra: Using where; Using join buffer (hash join)

-- Impossible WHERE：WHERE 条件永远为假
EXPLAIN SELECT * FROM users WHERE 1 = 0;
-- Extra: Impossible WHERE
```

### 9.4 常见执行计划分析

```sql
-- 慢查询示例1：全表扫描 + 文件排序
EXPLAIN SELECT * FROM users
WHERE email LIKE '%@gmail.com'
ORDER BY created_at DESC;

-- type: ALL（全表扫描）
-- Extra: Using where; Using filesort
-- 优化：在 (email, created_at) 上创建索引（但 LIKE '%...' 仍然不能走索引）
-- 更好的方案：考虑 FULLTEXT 索引或搜索引擎

-- 慢查询示例2：不需要的临时表和文件排序
EXPLAIN SELECT username, COUNT(*)
FROM users
GROUP BY username
ORDER BY COUNT(*) DESC;

-- 优化：确保 username 上有索引（GROUP BY 可以使用索引排序）

-- 慢查询示例3：分页深度偏移
EXPLAIN SELECT * FROM users
ORDER BY id
LIMIT 100000, 20;

-- type: ALL（全表扫描！即使 id 有主键索引）
-- 因为需要跳过 100000 行，优化器选择全表扫描
-- 优化：使用延迟 JOIN 或基于游标的分页
```

---

## 10. 事务与 ACID

### 10.1 ACID 详解

```sql
-- 事务示例：转账
START TRANSACTION;

UPDATE accounts SET balance = balance - 1000 WHERE id = 1;  -- 扣款
UPDATE accounts SET balance = balance + 1000 WHERE id = 2;  -- 入账

COMMIT;  -- 或 ROLLBACK
```

**A - 原子性（Atomicity）**

```
事务中的所有操作要么全部成功，要么全部失败回滚。

解释：
  BEGIN
    UPDATE account SET balance = balance - 1000 WHERE id = 1;  → 成功
    UPDATE account SET balance = balance + 1000 WHERE id = 2;  → 失败（数据库宕机）
  ROLLBACK（自动回滚）

结果：id=1 的余额不变（不会出现扣了钱但没到账的情况）

实现原理：Undo Log + 回滚段
```

**C - 一致性（Consistency）**

```
事务前后，数据完整性约束没有被破坏。

解释：
  转账前：A = 1000, B = 1000, 总和 = 2000
  转账后：A = 0,    B = 2000, 总和 = 2000  ✓ 总和不变
  错误：  A = 0,    B = 1000, 总和 = 1000  ✗ 钱丢了

约束：余额不能为负数（CHECK 约束）、外键完整性等
```

**I - 隔离性（Isolation）**

```
并发执行的事务之间互不干扰。
具体通过隔离级别 + 锁 + MVCC 实现。
```

**D - 持久性（Durability）**

```
已提交的事务的数据修改是永久的，即使系统崩溃也不丢失。

实现原理：Redo Log + WAL（Write-Ahead Logging）
在数据写入磁盘前，先写入 Redo Log；崩溃后可以通过 Redo Log 恢复。
```

### 10.2 事务隔离级别

```sql
-- 查看当前隔离级别
SELECT @@transaction_isolation;  -- MySQL 8.0
SELECT @@tx_isolation;          -- MySQL 5.7

-- 设置隔离级别（当前 Session）
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;

-- 设置隔离级别（全局）
SET GLOBAL TRANSACTION ISOLATION LEVEL READ COMMITTED;
```

**四种隔离级别对比：**

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 实现方式 |
|---------|------|-----------|------|---------|
| READ UNCOMMITTED | 可能 | 可能 | 可能 | 直接读最新版 |
| **READ COMMITTED** | 不会 | 可能 | 可能 | 每次读最新快照 |
| **REPEATABLE READ** （MySQL 默认） | 不会 | 不会 | 可能（InnoDB 通过 Gap Lock 避免） | 事务开始时的快照 |
| SERIALIZABLE | 不会 | 不会 | 不会 | 所有操作加锁 |

**脏读（Dirty Read）：**

```
事务 A                   事务 B
│                        │
│  UPDATE users SET      │
│  balance = 900         │
│  WHERE id = 1          │
│（未提交）              │
│                        │
│                        │  SELECT balance FROM users WHERE id = 1
│                        │  → 读到 900（事务 A 未提交的数据！）
│                        │  → 如果 A 回滚，B 读到的就是脏数据
│  ROLLBACK              │
│  balance 恢复 1000     │
│                        │  脏读！
```

**不可重复读（Non-Repeatable Read）：**

```
事务 A                   事务 B
│                        │
│  SELECT balance        │
│  WHERE id = 1          │
│  → 1000                │
│                        │
│                        │  UPDATE accounts SET balance = 500
│                        │  WHERE id = 1
│                        │  COMMIT
│                        │
│  SELECT balance        │
│  WHERE id = 1          │
│  → 500（变了！）        │
│                        │  不可重复读：同一事务中两次读到不同值
│                        │  （针对已有行的修改）
```

**幻读（Phantom Read）：**

```
事务 A                   事务 B
│                        │
│  SELECT * FROM users   │
│  WHERE id > 100        │
│  → 0 行                │
│                        │
│                        │  INSERT INTO users (id, name)
│                        │  VALUES (101, 'phantom')
│                        │  COMMIT
│                        │
│  INSERT INTO users     │
│  (id, name)            │
│  VALUES (101, 'test')  │
│  → 主键冲突！           │
│                        │  幻读：同一事务中两次查询结果集不同
│                        │  （针对新插入的行）
```

### 10.3 默认隔离级别的选择

```sql
-- MySQL 默认：REPEATABLE READ
-- 原因：MySQL 的主从复制中，基于 STATEMENT 的 binlog 格式
-- 需要 REPEATABLE READ 来保证主从一致性

-- 但大多数生产环境推荐使用 READ COMMITTED
-- 原因：
-- 1. REPEATABLE READ 的 Gap Lock 可能导致更多锁竞争
-- 2. 使用 ROW 格式的 binlog 后，READ COMMITTED 在主从复制中安全

-- 应用场景：
-- 互联网高并发写：READ COMMITTED（减少间隙锁竞争）
-- 金融系统报表：REPEATABLE READ（保证数据一致性）
-- 银行转账：SERIALIZABLE（最高安全性，但性能最差）
```

---

## 11. MVCC（多版本并发控制）

### 11.1 MVCC 核心概念

MVCC 允许一个事务读取数据时，其他事务可以同时修改数据，而每个事务看到的是数据的一个**一致性快照**。

```
关键组件：
1. undo log（回滚日志）：保存数据的历史版本
2. ReadView（读视图）：事务启动时生成的可见性判断条件
3. 事务 ID：每开启一个事务，InnoDB 分配一个自增的 trx_id
4. 隐藏列：每行数据有两个隐藏列
   - trx_id：最后一次修改这行数据的事务 ID
   - roll_pointer：指向 undo log 中上一个版本的指针
```

### 11.2 数据行多版本链

```
一行数据（主键 id=1）的版本链：

trx_id=100（初始插入）
  balance = 1000
  roll_pointer → NULL       （这是初始版本）
      ↑
trx_id=150（第一次更新）
  balance = 800
  roll_pointer → trx_100    （指向上一版本）
      ↑
trx_id=200（第二次更新）
  balance = 500
  roll_pointer → trx_150    （指向上一版本）

undo log 单向链表：
  trx_200 → trx_150 → trx_100 → NULL
  最新版          旧版本          最老版本
```

### 11.3 ReadView 可见性判断

```sql
-- REPEATABLE READ 隔离级别下，ReadView 在事务第一次查询时创建
-- READ COMMITTED 隔离级别下，每次查询都创建新的 ReadView

ReadView 包含：
  m_ids:    当前活跃的事务 ID 列表（未提交的事务）
  min_trx_id: m_ids 中的最小值（最早活跃事务）
  max_trx_id: 下一个要分配的事务 ID（m_ids 最大值 + 1）
  creator_trx_id: 创建这个 ReadView 的事务 ID

可见性规则：
  1. trx_id == creator_trx_id → 可见（自己修改的当然能看）
  2. trx_id < min_trx_id     → 可见（已提交的旧事务）
  3. trx_id >= max_trx_id    → 不可见（将来事务，不可见）
  4. trx_id IN m_ids         → 不可见（未提交的事务）
  5. 其他情况                → 可见（已提交的事务）
```

**图解 REPEATABLE READ：**

```
假设有三个事务：A(trx_100), B(trx_101), C(trx_102)

时间线：
  A 开始 (trx_100)        → ReadView: m_ids=[100,101,102]（假设都活跃）
  B 开始 (trx_101)
  C 开始 (trx_102)
  
  A 查询：
    ReadView: m_ids=[100,101,102], min=100, max=103
    → 读取数据时，只能看到 trx_id < 100 的数据
    → trx_101 和 trx_102 的修改对 A 不可见
  
  B 提交 (trx_101)
  C 提交 (trx_102)
  
  A 再次查询（REPEATABLE READ）：
    使用相同的 ReadView（事务开始时生成的那个！）
    → 仍然看不到 B 和 C 的修改
    → 实现了"可重复读"
```

**READ COMMITTED 的区别：**

```
  A 开始 (trx_100)
  B 开始 (trx_101)
  
  A 第一次查询：
    ReadView: m_ids=[100,101]
    → 看不到 trx_101 的修改（未提交）
  
  B 提交 (trx_101)
  
  A 第二次查询（READ COMMITTED）：
    创建新的 ReadView: m_ids=[100]
    → 现在能看到 trx_101 的修改了（已提交）
    → 这就是"不可重复读"的原因！
```

---

## 12. 锁机制

### 12.1 锁分类

```
InnoDB 锁分类：

┌─────────────────────────────────────────────┐
│                   InnoDB 锁                   │
├─────────────────────────────────────────────┤
│  按粒度分：                                    │
│  ├── 行级锁（Row Lock）                        │
│  │   ├── 共享锁（S Lock）                     │
│  │   ├── 排他锁（X Lock）                     │
│  │   ├── 间隙锁（Gap Lock）                   │
│  │   ├── 临键锁（Next-Key Lock）              │
│  │   └── 插入意向锁（Insert Intention Lock）  │
│  │                                             │
│  ├── 表级锁（Table Lock）                      │
│  │   ├── 意向共享锁（IS）                      │
│  │   ├── 意向排他锁（IX）                      │
│  │   ├── AUTO-INC 锁                           │
│  │   └── 元数据锁（MDL）                       │
│  │                                             │
│  按类型分：                                    │
│  ├── 共享锁（S Lock）：允许其他事务读取，禁止修改   │
│  └── 排他锁（X Lock）：禁止其他事务读取或修改      │
└─────────────────────────────────────────────┘
```

### 12.2 行级锁（Record Lock）

```sql
-- 共享锁（S Lock）：SELECT ... LOCK IN SHARE MODE
BEGIN;
SELECT * FROM users WHERE id = 1 LOCK IN SHARE MODE;
-- 其他事务可以读取 id=1，但不能修改（UPDATE/DELETE 会等待）
COMMIT;

-- 排他锁（X Lock）：SELECT ... FOR UPDATE
BEGIN;
SELECT * FROM users WHERE id = 1 FOR UPDATE;
-- 其他事务不能读取（SELECT ... FOR UPDATE 和 LOCK IN SHARE MODE）也不能修改
COMMIT;

-- 隐式排他锁
-- UPDATE/DELETE/INSERT 会自动添加排他锁
BEGIN;
UPDATE users SET balance = balance - 100 WHERE id = 1;
-- 隐式加了 X Lock on id=1
COMMIT;
```

### 12.3 间隙锁（Gap Lock）和临键锁（Next-Key Lock）

```sql
-- Gap Lock：锁定一个范围（不包含记录本身）
-- Next-Key Lock = Record Lock + Gap Lock

-- 表中有以下数据：
-- id: 1, 5, 10, 15, 20（主键）

-- 场景：REPEATABLE READ 下
BEGIN;
SELECT * FROM users WHERE id > 5 AND id < 10 FOR UPDATE;
-- Next-Key Lock 锁定了 (5, 10] 范围（不包含 5，包含 10？）
-- 实际锁了 (5, 10) 的间隙 + id=10 的行锁

-- 结果：
-- INSERT INTO users (id) VALUES (7);  → 等待（间隙锁阻止插入）
-- INSERT INTO users (id) VALUES (3);  → 成功（不在锁定范围）
-- INSERT INTO users (id) VALUES (2);  → 成功（不在锁定范围）
-- INSERT INTO users (id) VALUES (11); → 等待（还在 gap 范围？取决于具体范围）

-- Gap Lock 只在 REPEATABLE READ 中存在
-- READ COMMITTED 没有 Gap Lock（这是性能差异的关键原因之一）
```

### 12.4 意向锁（Intention Lock）

```sql
-- 意向锁是表级锁，表示事务"打算"对某些行加锁
-- IS：打算加共享锁
-- IX：打算加排他锁

-- 为什么需要意向锁？
-- 如果没有意向锁，事务 A 要锁表：
--   → 需要检查表中的每一行是否被锁 → O(n) 操作
-- 有了意向锁后，事务 A 要锁表：
--   → 检查 table 上是否有 IS/IX 锁 → O(1)

-- 锁兼容性矩阵：
--          X     IX    S     IS
--   X      ✗     ✗     ✗     ✗
--   IX     ✗     ✓     ✗     ✓
--   S      ✗     ✗     ✓     ✓
--   IS     ✗     ✓     ✓     ✓

-- 示例：
BEGIN;
SELECT * FROM users WHERE id = 1 FOR UPDATE;
-- 1. 对 users 表加 IX 锁（表级）
-- 2. 对 id=1 的行加 X 锁（行级）
COMMIT;
```

### 12.5 死锁检测与处理

```sql
-- 死锁场景
-- 事务 A                         事务 B
BEGIN;                          BEGIN;
UPDATE users SET balance=900     UPDATE users SET balance=900
WHERE id=1;                     WHERE id=2;
-- 持有 id=1 的 X 锁            -- 持有 id=2 的 X 锁

UPDATE users SET balance=1100    UPDATE users SET balance=1100
WHERE id=2;                     WHERE id=1;
-- 等待 id=2 的 X 锁 ← 死锁！→  -- 等待 id=1 的 X 锁

-- MySQL 的处理：
-- InnoDB 的死锁检测机制会检测到循环等待
-- 选择一个回滚代价较小的事务（通常是影响行数少的）作为"牺牲品"
-- 牺牲品事务回滚，释放锁
-- 另一个事务继续执行

-- 查看最近的死锁信息
SHOW ENGINE INNODB STATUS;

-- 避免死锁的实践：
-- 1. 所有事务按照相同的顺序访问资源
-- 2. 保持事务简短
-- 3. 在业务低峰期执行大事务
-- 4. 使用 READ COMMITTED 代替 REPEATABLE READ
```

---

## 13. 日志系统：Undo Log、Redo Log、Binlog

### 13.1 三大日志概览

```
┌─────────────────────────────────────────────────────┐
│                    MySQL 日志体系                      │
│                                                      │
│  Undo Log（InnoDB 内部）                             │
│  ├── 存储位置：ibdata1 或独立的 undo 表空间           │
│  ├── 用途：事务回滚 + MVCC 快照                       │
│  └── 记录方式：记录修改前的数据（逻辑日志）             │
│                                                      │
│  Redo Log（InnoDB 内部）                             │
│  ├── 存储位置：ib_logfile0, ib_logfile1              │
│  ├── 用途：崩溃恢复（crash-safe）                     │
│  ├── 记录方式：记录修改后的物理页变化（物理日志）        │
│  └── WAL（Write-Ahead Logging）：数据刷盘前先写日志     │
│                                                      │
│  Binlog（MySQL Server 层）                           │
│  ├── 存储位置：mysql-bin.000001 等                    │
│  ├── 用途：主从复制 + 数据恢复                         │
│  ├── 记录方式：逻辑日志（SQL 语句或行变更记录）          │
│  └── 三种格式：STATEMENT / ROW / MIXED               │
└─────────────────────────────────────────────────────┘
```

### 13.2 Redo Log（重做日志）

```sql
-- 日志写入流程（WAL）：
-- 1. 事务更新数据
-- 2. 写入 Redo Log（顺序写，非常快）
-- 3. 标记 Redo Log 为 PREPARE
-- 4. 写入 Binlog
-- 5. 标记 Redo Log 为 COMMIT
-- 6. 后台线程将数据页刷入磁盘

-- 为什么需要 Redo Log？
-- 如果没有 Redo Log，每次修改都要立即刷盘（随机写，很慢）
-- Redo Log 顺序写（追加），速度远快于随机写数据页

-- Redo Log 配置
SHOW VARIABLES LIKE 'innodb_log_file_size';    -- 默认 48MB（生产建议 1-4GB）
SHOW VARIABLES LIKE 'innodb_log_files_in_group'; -- 默认 2 个文件
SHOW VARIABLES LIKE 'innodb_log_buffer_size';   -- 默认 16MB

-- 查看 Redo Log 使用情况
SHOW ENGINE INNODB STATUS;
-- 关注：Log sequence number 和 Log flushed up to
-- 差值过大表示日志写入跟不上

-- 崩溃恢复过程
-- 1. 检查 Redo Log 中是否有未刷盘的数据
-- 2. 重放 Redo Log（前滚）
-- 3. 检查 Undo Log 中是否有未提交的事务
-- 4. 回滚未提交的事务
```

### 13.3 Undo Log（回滚日志）

```sql
-- Undo Log 存储历史版本数据
-- 用途 1：事务回滚

BEGIN;
INSERT INTO users (id, name) VALUES (1, 'Alice');
UPDATE users SET name = 'Bob' WHERE id = 1;
DELETE FROM users WHERE id = 1;

ROLLBACK;
-- Undo Log 中记录了各个操作的"反向操作"
-- INSERT → 记录主键，回滚时 DELETE
-- UPDATE → 记录旧值，回滚时恢复旧值
-- DELETE → 记录整行数据，回滚时重新 INSERT

-- 用途 2：MVCC 快照
-- 事务读数据时，通过 Undo Log 构建历史版本
```

### 13.4 Binlog（二进制日志）

```sql
-- 查看 Binlog 配置
SHOW VARIABLES LIKE 'log_bin';          -- 是否开启
SHOW VARIABLES LIKE 'binlog_format';    -- STATEMENT / ROW / MIXED
SHOW VARIABLES LIKE 'expire_logs_days'; -- 过期天数

-- Binlog 三种格式对比
-- STATEMENT：记录 SQL 语句
--   ✓ 日志量小
--   ✗ 非确定性函数（NOW(), UUID()）导致主从不一致

-- ROW：记录行变更
--   ✓ 精确一致（推荐）
--   ✗ 日志量大（大事务会产生巨量日志）

-- MIXED：混合模式
--   大部分用 STATEMENT，不安全的用 ROW

-- 推荐使用 ROW 格式
-- MySQL 8.0 默认 ROW
```

### 13.5 两阶段提交（2PC）

Redo Log 和 Binlog 的一致性是 MySQL 崩溃安全的关键：

```
事务提交的两个阶段：

阶段 1（Prepare）：
  ┌─────────────┐
  │ Redo Log    │──→ PREPARE（写入并刷盘）
  └─────────────┘

阶段 2（Commit）：
  ┌─────────────┐
  │ Binlog      │──→ 写入并刷盘
  └─────────────┘
  ┌─────────────┐
  │ Redo Log    │──→ COMMIT（标记为已提交）
  └─────────────┘


崩溃场景分析：

场景 1：Prepare 后崩溃
  → Redo Log PREPARE 但 Binlog 未写入
  → 恢复时回滚事务（数据一致性）

场景 2：Binlog 写入后崩溃
  → Redo Log PREPARE + Binlog 已写入
  → 恢复时认为事务已提交（数据不丢失）

场景 3：Redo Log COMMIT 后崩溃
  → 事务已完整提交
  → 恢复时 Redo Log 重放
```

---

## 14. SQL 优化

### 14.1 慢查询日志

```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;        -- 超过 1 秒的查询记录
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow.log';

-- MySQL 8.0 推荐使用 SET GLOBAL 永久配置在 my.cnf 中
-- my.cnf 配置：
-- slow_query_log = 1
-- slow_query_log_file = /var/log/mysql/slow.log
-- long_query_time = 1

-- 使用 mysqldumpslow 分析慢查询
-- mysqldumpslow -t 10 /var/log/mysql/slow.log  （最慢的 10 条）
-- mysqldumpslow -s c -t 10 /var/log/mysql/slow.log  （按次数排序）

-- 查看正在执行的查询
SHOW FULL PROCESSLIST;

-- 查看是否有锁等待
SELECT * FROM information_schema.INNODB_TRX\G
SELECT * FROM performance_schema.events_statements_current\G
```

### 14.2 查询优化技术

**避免 SELECT *：**

```sql
-- 不推荐
SELECT * FROM users WHERE id = 1;
-- 读取所有列，浪费 IO 和内存

-- 推荐
SELECT id, username, email FROM users WHERE id = 1;
-- 只读需要的列，可能使用覆盖索引
```

**分页优化（大偏移量问题）：**

```sql
-- 传统分页（大偏移量性能差）
SELECT * FROM users ORDER BY id LIMIT 100000, 20;
-- MySQL 需要读取 100020 行，然后丢弃前 100000 行

-- 优化方案 1：延迟 JOIN
SELECT * FROM users
INNER JOIN (
    SELECT id FROM users
    ORDER BY id
    LIMIT 100000, 20
) AS tmp ON users.id = tmp.id;

-- 优化方案 2：基于游标的分页（推荐）
-- 记录上次查询的最后一条记录 id
SELECT * FROM users
WHERE id > last_seen_id  -- 客户端传入上一页最后一条的 id
ORDER BY id
LIMIT 20;

-- 优化方案 3：基于排序字段的分页
SELECT * FROM users
WHERE created_at < '2024-01-01'  -- 上一页最后一条的时间
ORDER BY created_at DESC
LIMIT 20;
```

**COUNT 优化：**

```sql
-- COUNT(*) vs COUNT(column)
-- COUNT(*) 统计行数，最快（InnoDB 会选最小的索引来数）
-- COUNT(column) 统计非 NULL 的行数

-- 大表 COUNT 优化
-- InnoDB 不支持 COUNT 的"即时返回"，必须扫描行
-- 近似值：SHOW TABLE STATUS 中的 rows 字段
-- 精确计数：使用 Redis 或计数表维护

-- 条件 COUNT
SELECT COUNT(*) FROM users WHERE status = 1;
-- 确保 status 上有索引

-- 多条件聚合
SELECT
    COUNT(CASE WHEN status=1 THEN 1 END) AS active,
    COUNT(CASE WHEN status=0 THEN 1 END) AS inactive,
    COUNT(*) AS total
FROM users;
```

**ORDER BY 优化：**

```sql
-- ORDER BY 使用索引排序（避免 filesort）
-- 索引 (role, status, created_at)

-- ✓ 使用索引排序
SELECT * FROM users WHERE role = 'admin' ORDER BY status;
SELECT * FROM users WHERE role = 'admin' ORDER BY status, created_at;
SELECT * FROM users WHERE role = 'admin' ORDER BY status DESC;

-- ✗ 无法使用索引排序
SELECT * FROM users ORDER BY username;  -- username 不在索引中
SELECT * FROM users WHERE status = 1 ORDER BY role;
  -- status 不是最左列（索引是 role,status,created_at）
SELECT * FROM users ORDER BY status ASC, created_at DESC;
  -- 排序方向不一致

-- 排序 + LIMIT 优化
SELECT * FROM users WHERE status = 1 ORDER BY created_at DESC LIMIT 10;
-- 如果 (status, created_at) 上有复合索引，非常高效
```

**GROUP BY 优化：**

```sql
-- GROUP BY 默认会排序，如果不需要排序可以使用 ORDER BY NULL
SELECT role, COUNT(*) FROM users GROUP BY role ORDER BY NULL;
-- 减少一次 filesort

-- 使用索引优化 GROUP BY
-- 索引 (role, status)
SELECT role, COUNT(*) FROM users GROUP BY role;  -- 使用索引
SELECT status, COUNT(*) FROM users GROUP BY status;
  -- status 不是最左列，需要临时表
```

### 14.3 配置调优

```ini
# my.cnf 关键参数

# InnoDB 缓冲池（最重要！通常设为可用内存的 60-80%）
innodb_buffer_pool_size = 4G

# 缓冲池实例数量（减少内部锁竞争）
innodb_buffer_pool_instances = 8

# Redo Log 大小
innodb_log_file_size = 1G
innodb_log_files_in_group = 2

# 日志缓冲区
innodb_log_buffer_size = 32M

# IO 线程数
innodb_read_io_threads = 8
innodb_write_io_threads = 8

# 刷新方式（双写缓冲区，保证页完整性）
innodb_doublewrite = 1

# 事务隔离级别
transaction-isolation = READ-COMMITTED

# 连接池
max_connections = 500
# 连接超时
wait_timeout = 600
interactive_timeout = 600

# 临时表大小
tmp_table_size = 64M
max_heap_table_size = 64M

# 排序/分组缓冲区（用于 filesort）
sort_buffer_size = 4M
join_buffer_size = 4M

# 查询缓存（MySQL 8.0 已移除）
# query_cache_type = 0

# 日志
slow_query_log = 1
long_query_time = 1
```

---

## 15. 表设计与架构优化

### 15.1 三范式 vs 反范式

**第一范式（1NF）：每个列不可再分**

```sql
-- 违反 1NF：phone 列存了多个手机号
CREATE TABLE users (
    id INT PRIMARY KEY,
    phones VARCHAR(255)  -- "13800138000,13900139000"
);
```

**第二范式（2NF）：非主键列完全依赖于主键**

```sql
-- 违反 2NF：
CREATE TABLE order_items (
    order_id INT,
    product_id INT,
    product_name VARCHAR(100),  -- product_name 只依赖于 product_id，不是联合主键
    quantity INT,
    PRIMARY KEY (order_id, product_id)
);
```

**第三范式（3NF）：非主键列不传递依赖**

```sql
-- 违反 3NF：
CREATE TABLE orders (
    id INT PRIMARY KEY,
    user_id INT,
    user_name VARCHAR(100),  -- user_name 通过 user_id 传递依赖
    amount DECIMAL(10, 2)
);
```

**反范式化：适当的冗余提升查询性能**

```sql
-- 订单列表展示：每次都需要 JOIN users 查 username
-- 反范式化：在 orders 表中冗余 user_name
CREATE TABLE orders (
    id INT PRIMARY KEY,
    user_id INT NOT NULL,
    user_name VARCHAR(100) NOT NULL,  -- 冗余字段
    amount DECIMAL(10, 2) NOT NULL,
    created_at DATETIME NOT NULL
);
-- 更新用户名字时需要同步更新 orders 表
```

### 15.2 水平拆分 vs 垂直拆分

```sql
-- 垂直拆分：将大表的列拆分到多个表
-- 原始表
CREATE TABLE users (
    id INT PRIMARY KEY,
    username VARCHAR(50),
    password VARCHAR(255),
    email VARCHAR(100),
    phone VARCHAR(20),
    avatar BLOB,           -- 大字段拆分
    bio TEXT,              -- 大字段拆分
    login_ip VARCHAR(45),
    login_count INT,
    created_at DATETIME
);

-- 垂直拆分后
CREATE TABLE users_base (
    id INT PRIMARY KEY,
    username VARCHAR(50),
    password VARCHAR(255),
    email VARCHAR(100),
    phone VARCHAR(20),
    created_at DATETIME
);

CREATE TABLE users_profile (
    user_id INT PRIMARY KEY,
    avatar BLOB,
    bio TEXT,
    login_ip VARCHAR(45),
    login_count INT,
    FOREIGN KEY (user_id) REFERENCES users_base(id)
);

-- 水平拆分：将数据按条件拆分到多个表/数据库
-- 例如：按用户 ID 取模分表
-- users_0 (id % 4 = 0)
-- users_1 (id % 4 = 1)
-- users_2 (id % 4 = 2)
-- users_3 (id % 4 = 3)

-- 或按时间分表
-- orders_202401 (2024 年 1 月的订单)
-- orders_202402 (2024 年 2 月的订单)
```

---

## 16. 最佳实践与常见陷阱

### 16.1 最佳实践

| 类别 | 建议 |
|------|------|
| **主键** | 自增整型主键（顺序写，无页分裂）；分布式用雪花算法 |
| **索引** | 区分度大的列放前面、为 GROUP BY/ORDER BY 创建复合索引、不过度索引 |
| **查询** | 只查需要的列、避免 SELECT *、合理使用 LIMIT |
| **JOIN** | 小表驱动大表、JOIN 列必须有索引、避免多表 JOIN |
| **事务** | 简短、不跨网络、不在事务中调用远程接口 |
| **配置** | innodb_buffer_pool_size = 内存的 60-80%、binlog_format = ROW |
| **编码** | 推荐 utf8mb4（支持 emoji）、统一字符集 |
| **日志** | 开启慢查询日志、定期分析慢查询 |

### 16.2 常见陷阱

| 陷阱 | 问题 | 解决 |
|------|------|------|
| **NULL 索引** | 索引列含 NULL，查询可能不准确 | 设置 NOT NULL + 默认值 |
| **隐式类型转换** | `WHERE phone=138...` 使索引失效 | 类型匹配 |
| **前模糊 LIKE** | `LIKE '%xxx'` 无法使用索引 | 全文索引或搜索引擎 |
| **在索引列用函数** | `WHERE DATE(create_at)='...'` 索引失效 | 改用范围查询 |
| **大事务** | 锁持有时间长、Undo Log 膨胀 | 缩短事务、分批处理 |
| **连接泄漏** | 应用程序不关闭连接，池耗尽 | try-with-resources 确保关闭 |
| **MySQL 5.7 默认排序** | 不同数据库排序结果不同 | 明确 ORDER BY |
| **ENUM 枚举值变更** | ALTER TABLE 修改枚举值阻塞写 | 用 TINYINT 代替 |
| **分页深度偏移** | LIMIT 100000,20 性能差 | 游标分页或延迟 JOIN |

---

## 17. 面试题

### 基础题

**Q1: InnoDB 和 MyISAM 有什么区别？**

A:
- 事务：InnoDB 支持，MyISAM 不支持
- 锁粒度：InnoDB 行级锁，MyISAM 表级锁
- 外键：InnoDB 支持，MyISAM 不支持
- 索引：InnoDB 聚簇索引（数据在索引中），MyISAM 非聚簇索引（数据和索引分离）
- 崩溃恢复：InnoDB 支持（Redo Log），MyISAM 不支持
- 存储：InnoDB .ibd，MyISAM .MYD + .MYI

**Q2: B+Tree 索引的特点？**

A:
- 所有数据存储在叶子节点，内部节点只存索引键
- 叶子节点形成有序双向链表（高效范围查询）
- 树高通常 3-4 层（千万级数据也只需 3-4 次磁盘 IO）
- 每个节点对应一个磁盘页（16KB）

**Q3: ACID 分别指什么？InnoDB 如何实现？**

A:
- A（原子性）：Undo Log，回滚时撤销所有修改
- C（一致性）：约束 + 事务机制保证
- I（隔离性）：MVCC + 锁机制
- D（持久性）：Redo Log + WAL

**Q4: 什么是 MVCC？**

A: 多版本并发控制，让事务读数据时不需要等待写锁。
核心组件：Undo Log（多版本链）+ ReadView（可见性判断）+ 事务 ID。
MySQL 的 REPEATABLE READ 和 READ COMMITTED 基于 MVCC 实现。

**Q5: 事务隔离级别有哪些？MySQL 默认是什么？**

A:
- READ UNCOMMITTED：脏读、不可重复读、幻读都可能
- READ COMMITTED：避免脏读，不可重复读和幻读仍可能
- REPEATABLE READ（MySQL 默认）：避免脏读和不可重复读，InnoDB 通过 Gap Lock 避免幻读
- SERIALIZABLE：全部避免，性能最差

### 进阶题

**Q6: 什么是聚簇索引和非聚簇索引的区别？**

A:
- 聚簇索引：索引叶子节点直接存储完整行数据，一张表只有一个（通常是主键）
- 非聚簇索引（二级索引）：叶子节点存储索引列 + 主键值，需要通过主键回表查完整数据
- 聚簇索引数据按主键顺序物理存储，二级索引不影响物理存储顺序

**Q7: 什么是索引下推（ICP）？**

A: MySQL 5.6+ 的优化，在索引遍历时直接过滤 WHERE 条件，减少回表次数。
EXPLAIN 的 Extra 列显示 "Using index condition"。

**Q8: 什么是覆盖索引？**

A: 查询所需的所有列都在索引中，不需要回表。EXPLAIN 的 Extra 显示 "Using index"。
常用于优化高频查询，通过创建复合索引覆盖查询的所有列。

**Q9: 什么是 Gap Lock？为什么要有它？**

A: Gap Lock 锁定索引记录之间的间隙（不包括记录本身），防止其他事务在间隙中插入数据。
目的是在 REPEATABLE READ 级别下防止幻读。Next-Key Lock = Record Lock + Gap Lock。

**Q10: 什么是两阶段提交？**

A: MySQL 通过两阶段提交保证 Redo Log 和 Binlog 的一致性：
1. Prepare 阶段：Redo Log 写入 PREPARE
2. 写入 Binlog
3. Commit 阶段：Redo Log 标记 COMMIT

崩溃恢复时，若 Binlog 已写入则事务可提交，否则回滚。

**Q11: 为什么 MySQL 不建议用 UUID 做主键？**

A:
- UUID 无序插入会导致页分裂（随机插入到 B+Tree 中间位置，性能差）
- UUID 占 36 字节，INT 仅 4 字节（BIGINT 8 字节）
- UUID 会导致索引变大，二级索引叶子节点存主键值也变大
- 方案：用自增 INT/BIGINT，或用雪花算法生成有序分布式 ID

**Q12: 如何进行 SQL 优化？**

A:
1. EXPLAIN 分析执行计划（type、key、rows、Extra）
2. 检查索引使用情况（是否使用覆盖索引、索引下推）
3. 避免索引失效（LIKE 前模糊、函数嵌套、类型转换）
4. 优化分页（延迟 JOIN、游标分页）
5. 优化 JOIN（小表驱动大表、JOIN 列建索引）
6. 避免 SELECT *
7. 检查慢查询日志
8. 分析是否进行了不必要的排序和临时表

**Q13: 什么是死锁？如何解决？**

A: 两个或多个事务互相持有对方需要的锁，形成循环等待。
解决：InnoDB 死锁检测自动回滚其中一个事务。
预防：
- 所有事务按相同顺序访问资源
- 事务尽量简短
- 使用 READ COMMITTED（减少 Gap Lock）
- 业务低峰期执行大事务

**Q14: MVCC 在 READ COMMITTED 和 REPEATABLE READ 的实现差异？**

A:
- READ COMMITTED：每次查询都创建新的 ReadView，所以能读到其他事务已提交的数据（不可重复读）
- REPEATABLE READ：事务第一次查询时创建 ReadView，之后复用，所以始终看到一致的数据（可重复读）
- 两者都使用 Undo Log 多版本链，但 ReadView 的创建时机不同

**Q15: Binlog 三种格式的区别？**

A:
- STATEMENT：记录 SQL，日志量小，但非确定性函数导致主从不一致
- ROW：记录行变更，精确一致，但大事务日志量大
- MIXED：自动选择，大部分用 STATEMENT，不安全的用 ROW
- 推荐：MySQL 8.0 默认 ROW，生产环境也推荐 ROW

---

## 参考资源

- [MySQL 官方文档](https://dev.mysql.com/doc/) -- 权威参考
- [MySQL 8.0 Reference Manual](https://dev.mysql.com/doc/refman/8.0/en/) -- 完整手册
- [高性能 MySQL（第3版）](https://book.douban.com/subject/23008813/) -- 经典书籍
- [MySQL 是怎样运行的](https://book.douban.com/subject/35231266/) -- 中国开发者必读
- [MySQL 实战 45 讲（极客时间）](https://time.geekbang.org/column/intro/139) -- 丁奇
- [MySQL 索引原理及慢查询优化（美团技术）](https://tech.meituan.com/2014/06/30/mysql-index.html)
- [InnoDB Locking 官方文档](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html)

---

*最后更新: 2026-05-31*
