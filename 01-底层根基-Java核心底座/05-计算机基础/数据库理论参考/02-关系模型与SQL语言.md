# 关系模型与 SQL 语言
> 关系模型是关系型数据库的理论基础，SQL 是操作数据库的标准语言。本文涵盖关系代数、SQL 语法（DDL/DML/DQL/TCL）、高级查询（JOIN/子查询/窗口函数）及执行顺序。

## 目录
1. [关系代数基础](#1-关系代数基础)
2. [SQL 语言分类](#2-sql-语言分类)
3. [DDL 数据定义](#3-ddl-数据定义)
4. [DML 数据操作](#4-dml-数据操作)
5. [DQL 数据查询](#5-dql-数据查询)
6. [JOIN 连接查询](#6-join-连接查询)
7. [子查询](#7-子查询)
8. [窗口函数](#8-窗口函数)
9. [SQL 执行顺序](#9-sql-执行顺序)
10. [中级 SQL 进阶](#10-中级-sql-进阶)

---

## 1. 关系代数基础

关系代数是 SQL 的理论基础——每个 SQL 查询都可以表示为关系代数表达式。

### 1.1 集合运算

| 运算 | 符号 | SQL 对应 | 说明 |
|------|------|----------|------|
| 并 | `∪` | `UNION` | 合并两个查询结果 |
| 交 | `∩` | `INTERSECT` | 两个查询结果的交集（MySQL 不支持） |
| 差 | `−` | `EXCEPT` | 第一个结果减去第二个（MySQL 不支持） |
| 笛卡尔积 | `×` | `CROSS JOIN` | 所有可能的组合 |

### 1.2 专门关系运算

| 运算 | 符号 | SQL 对应 | 说明 |
|------|------|----------|------|
| 选择 | `σ` | `WHERE` | 选出满足条件的行 |
| 投影 | `π` | `SELECT 列名` | 选出指定列 |
| 连接 | `⋈` | `JOIN` | 按条件组合两个表 |
| 除 | `÷` | 复杂 SQL | 找出"满足所有条件"的元组 |

### 1.3 关系代数示例

```
π_name, age(σ_age > 18(Student))
→ SELECT name, age FROM Student WHERE age > 18

π_name(Student ⋈_Student.id = Enroll.sid Enroll)
→ SELECT name FROM Student JOIN Enroll ON Student.id = Enroll.sid
```

---

## 2. SQL 语言分类

| 分类 | 全称 | 作用 | 关键字 |
|------|------|------|--------|
| DDL | Data Definition Language | 定义库、表、视图 | CREATE, ALTER, DROP, TRUNCATE |
| DML | Data Manipulation Language | 数据增删改 | INSERT, UPDATE, DELETE |
| DQL | Data Query Language | 数据查询 | SELECT |
| DCL | Data Control Language | 权限管理 | GRANT, REVOKE |
| TCL | Transaction Control Language | 事务管理 | COMMIT, ROLLBACK, SAVEPOINT |

---

## 3. DDL 数据定义

```sql
-- 创建库
CREATE DATABASE bookstore CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建表
CREATE TABLE users (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username   VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    email      VARCHAR(100) NOT NULL COMMENT '邮箱',
    age        TINYINT      DEFAULT 0 COMMENT '年龄',
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 修改表结构
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
ALTER TABLE users MODIFY COLUMN age TINYINT DEFAULT 18;
ALTER TABLE users DROP COLUMN phone;

-- 删除表
DROP TABLE IF EXISTS users;

-- 清空表
TRUNCATE TABLE users;   -- 不可回滚，自动提交
```

---

## 4. DML 数据操作

```sql
-- 插入
INSERT INTO users (username, email, age) VALUES ('zhangsan', 'zhang@example.com', 25);
INSERT INTO users (username, email) VALUES ('lisi', 'li@example.com'), ('wangwu', 'wang@example.com');

-- 更新
UPDATE users SET age = 26 WHERE username = 'zhangsan';

-- 删除
DELETE FROM users WHERE id = 1;
```

---

## 5. DQL 数据查询

### 5.1 基本查询

```sql
SELECT [DISTINCT] col1, col2, AGG(col3)
FROM table1
WHERE 条件              -- 分组前筛选
GROUP BY col1           -- 分组
HAVING 聚合条件          -- 分组后筛选
ORDER BY col1 ASC/DESC  -- 排序
LIMIT n OFFSET m;       -- 分页
```

### 5.2 聚合函数

| 函数 | 说明 | 注意 |
|------|------|------|
| `COUNT(*)` | 统计行数 | InnoDB 最优 |
| `COUNT(列名)` | 统计非 NULL 行数 | 比 COUNT(*) 慢 |
| `SUM(列)` | 求和 | - |
| `AVG(列)` | 平均值 | - |
| `MAX/MIN(列)` | 最大/最小值 | - |

```sql
-- 每个年龄段的用户数
SELECT age, COUNT(*) AS cnt FROM users GROUP BY age;

-- 筛选分组后数量大于 5 的组
SELECT age, COUNT(*) AS cnt FROM users GROUP BY age HAVING cnt > 5;
```

---

## 6. JOIN 连接查询

### 6.1 JOIN 类型

| 类型 | 说明 | SQL |
|------|------|-----|
| 内连接 | 两表交集 | `INNER JOIN ... ON` |
| 左外连接 | 左表全部 + 右表匹配 | `LEFT JOIN ... ON` |
| 右外连接 | 右表全部 + 左表匹配 | `RIGHT JOIN ... ON` |
| 全外连接 | 全部保留 | `FULL OUTER JOIN`（MySQL 不支持，用 UNION 模拟） |
| 交叉连接 | 笛卡尔积 | `CROSS JOIN` |

### 6.2 示例

```sql
-- 内连接
SELECT u.name, o.order_no FROM users u INNER JOIN orders o ON u.id = o.user_id;

-- 左外连接（保留所有用户，包括没下过单的）
SELECT u.name, o.order_no FROM users u LEFT JOIN orders o ON u.id = o.user_id;

-- 自连接
SELECT e1.name AS employee, e2.name AS manager
FROM emp e1 LEFT JOIN emp e2 ON e1.manager_id = e2.id;

-- 多表连接
SELECT u.name, o.order_no, p.product_name
FROM users u
JOIN orders o ON u.id = o.user_id
JOIN products p ON o.product_id = p.id;
```

### 6.3 USING 简化

当连接列名相同时：

```sql
-- ON
SELECT * FROM users JOIN orders ON users.id = orders.user_id;
-- USING（等价）
SELECT * FROM users JOIN orders USING(id);
```

---

## 7. 子查询

| 类型 | 返回 | 可使用位置 |
|------|------|-----------|
| 标量子查询 | 单个值 | SELECT, WHERE |
| 行子查询 | 单行多列 | WHERE |
| 列子查询 | 单列多行 | WHERE（配合 IN/ANY/ALL） |
| 表子查询 | 多行多列 | FROM（派生表） |

```sql
-- 标量子查询：查询工资高于平均的员工
SELECT name, salary FROM emp
WHERE salary > (SELECT AVG(salary) FROM emp);

-- 列子查询：有订单的用户
SELECT * FROM users WHERE id IN (SELECT DISTINCT user_id FROM orders);

-- 表子查询（派生表）：各部门工资最高的人
SELECT dept, name, salary FROM (
    SELECT dept, name, salary,
           RANK() OVER (PARTITION BY dept ORDER BY salary DESC) AS rk
    FROM emp
) t WHERE rk = 1;

-- EXISTS：比 IN 更高效（相关子查询，找到即返回）
SELECT * FROM users u
WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id);
```

> 💡 **EXISTS vs IN**：EXISTS 是相关子查询，一行匹配就返回 true（适合子表大）；IN 先算子查询结果集（适合子表小）。通常 EXISTS 性能更好。

---

## 8. 窗口函数

窗口函数在 MySQL 8.0+ 中可用，是进阶 SQL 必备技能。

```sql
SELECT name, dept, salary,
       ROW_NUMBER() OVER (PARTITION BY dept ORDER BY salary DESC) AS rn,
       RANK()       OVER (PARTITION BY dept ORDER BY salary DESC) AS rk,
       DENSE_RANK() OVER (PARTITION BY dept ORDER BY salary DESC) AS dr,
       SUM(salary)  OVER (PARTITION BY dept) AS dept_total,
       LAG(salary)  OVER (PARTITION BY dept ORDER BY salary) AS prev_salary,
       LEAD(salary) OVER (PARTITION BY dept ORDER BY salary) AS next_salary
FROM employees;
```

| 函数 | 说明 |
|------|------|
| `ROW_NUMBER()` | 连续排名（1, 2, 3） |
| `RANK()` | 并列占位（1, 1, 3） |
| `DENSE_RANK()` | 并列不占位（1, 1, 2） |
| `LAG(col, n)` | 前 n 行值 |
| `LEAD(col, n)` | 后 n 行值 |

**应用场景**：Top N 每组、累计求和、环比计算。

---

## 9. SQL 执行顺序

> **书写顺序与执行顺序不同！**

```sql
-- 书写顺序：
SELECT → FROM → JOIN → ON → WHERE → GROUP BY → HAVING → ORDER BY → LIMIT

-- 执行顺序（逻辑）：
FROM → ON → JOIN → WHERE → GROUP BY → HAVING → SELECT → DISTINCT → ORDER BY → LIMIT
```

| 步骤 | 说明 |
|------|------|
| 1. FROM + JOIN | 确定数据源，生成笛卡尔积 |
| 2. ON | 过滤连接条件 |
| 3. WHERE | 行级过滤 |
| 4. GROUP BY | 分组 |
| 5. HAVING | 分组后过滤（可使用聚合函数） |
| 6. SELECT | 选择列，计算表达式 |
| 7. DISTINCT | 去重 |
| 8. ORDER BY | 排序 |
| 9. LIMIT | 分页 |

---

## 10. 中级 SQL 进阶

### 10.1 GROUP BY 扩展

```sql
-- WITH ROLLUP：分组汇总行
SELECT dept, SUM(salary) FROM emp GROUP BY dept WITH ROLLUP;
```

### 10.2 视图

```sql
-- 创建视图
CREATE VIEW high_earners AS
SELECT name, dept, salary FROM emp WHERE salary > 10000;

-- 使用视图（像查表一样）
SELECT * FROM high_earners;
```

### 10.3 索引

```sql
-- 创建索引
CREATE INDEX idx_name ON users(name);

-- 唯一索引
CREATE UNIQUE INDEX idx_email ON users(email);

-- 复合索引
CREATE INDEX idx_name_dept ON emp(name, dept);
```

### 10.4 授权

```sql
GRANT SELECT, INSERT ON bookstore.* TO 'app_user'@'localhost';
REVOKE DELETE ON bookstore.* FROM 'app_user'@'localhost';
```

> 🎯 **SQL 执行顺序必须理解——这是写复杂查询不出错的根本。** 建议刷 50+ 道 LeetCode 数据库题以应对面试手写 SQL。
