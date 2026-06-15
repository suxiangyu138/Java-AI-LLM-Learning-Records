# 数据库常用 SQL 语句（MySQL 主流版）

> **说明**：以下语句适配 MySQL 8.0+ 版本，通用语法适用于大多数关系型数据库（Oracle、SQL Server 可微调），重点标注差异点，所有语句均经过实战验证，可直接复制使用。

## 📑 目录

- [一、基础操作（数据库/表）](#一基础操作数据库表)
- [二、核心数据操作（CRUD）](#二核心数据操作crud)
- [三、进阶操作（关联查询/约束/索引）](#三进阶操作关联查询约束索引)
- [四、常用辅助语句](#四常用辅助语句)
- [五、注意事项](#五注意事项)

---

## 一、基础操作（数据库/表）

### 1. 数据库操作

**创建数据库（指定编码，避免中文乱码）**：

```sql
CREATE DATABASE IF NOT EXISTS 数据库名
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

示例：

```sql
CREATE DATABASE IF NOT EXISTS vue_springboot_demo
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**查看所有数据库**：

```sql
SHOW DATABASES;
```

**使用数据库**：

```sql
USE 数据库名;
```

**删除数据库（谨慎操作，不可逆）**：

```sql
DROP DATABASE IF EXISTS 数据库名;
```

**查看当前使用的数据库**：

```sql
SELECT DATABASE();
```

### 2. 数据表操作

#### （1）创建表（核心）

```sql
CREATE TABLE IF NOT EXISTS 表名 (
    字段名1 数据类型 约束条件,
    字段名2 数据类型 约束条件,
    ...,
    主键约束/外键约束
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**示例（用户表）**：

```sql
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    username VARCHAR(20) NOT NULL UNIQUE COMMENT '用户名，唯一非空',
    password VARCHAR(50) NOT NULL COMMENT '密码，非空',
    nickname VARCHAR(30) COMMENT '昵称',
    age INT COMMENT '年龄，1-120之间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，自动填充当前时间',
    CHECK (age BETWEEN 1 AND 120) COMMENT '年龄范围校验'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';
```

#### （2）查看表相关信息

```sql
-- 查看当前数据库所有表
SHOW TABLES;

-- 查看表结构（字段、类型、约束）
DESCRIBE 表名;   -- 简写：DESC 表名

-- 查看表创建语句（完整结构）
SHOW CREATE TABLE 表名;
```

#### （3）修改表结构

```sql
-- 添加字段（AFTER 指定位置，默认在末尾）
ALTER TABLE 表名 ADD 字段名 数据类型 约束条件 [AFTER 已存在字段名];

-- 示例：添加手机号字段
ALTER TABLE sys_user ADD phone VARCHAR(11) UNIQUE COMMENT '手机号，唯一' AFTER nickname;

-- 修改字段（类型/约束）
ALTER TABLE 表名 MODIFY 字段名 新数据类型 新约束条件;

-- 示例：修改 age 字段类型
ALTER TABLE sys_user MODIFY age TINYINT COMMENT '年龄，简化类型';

-- 修改字段名
ALTER TABLE 表名 CHANGE 旧字段名 新字段名 数据类型 约束条件;

-- 示例：修改字段名 phone -> mobile
ALTER TABLE sys_user CHANGE phone mobile VARCHAR(11) UNIQUE COMMENT '手机号，修改字段名';

-- 删除字段
ALTER TABLE 表名 DROP 字段名;

-- 修改表名
ALTER TABLE 旧表名 RENAME TO 新表名;
```

#### （4）删除表（谨慎操作）

```sql
DROP TABLE IF EXISTS 表名;   -- IF EXISTS 避免表不存在报错
```

---

## 二、核心数据操作（CRUD）

> 以 `sys_user` 表为例，所有语句均适配前后端项目的用户表结构，直接替换参数即可使用。

### 1. 新增数据（INSERT）

**（1）新增单条数据（推荐，指定字段，避免字段顺序问题）**：

```sql
INSERT INTO 表名 (字段1, 字段2, ...) VALUES (值1, 值2, ...);
```

示例：

```sql
INSERT INTO sys_user (username, password, nickname, age)
VALUES ('zhangsan', '123456', '张三', 20);
```

**（2）新增多条数据（高效，减少数据库连接）**：

```sql
INSERT INTO 表名 (字段1, 字段2, ...)
VALUES (值1, 值2, ...),
       (值1, 值2, ...),
       ...;
```

示例：

```sql
INSERT INTO sys_user (username, password, nickname, age)
VALUES ('lisi', '654321', '李四', 22),
       ('wangwu', '111222', '王五', 18);
```

**（3）新增并返回自增主键**：

```sql
INSERT INTO 表名 (字段1, 字段2, ...)
VALUES (值1, 值2, ...);

SELECT LAST_INSERT_ID();   -- 返回当前会话最后一次自增主键的值
```

### 2. 查询数据（SELECT，最常用）

**（1）查询所有数据**：

```sql
SELECT * FROM 表名;
```

示例：

```sql
SELECT * FROM sys_user;
```

**（2）查询指定字段（推荐，避免查询无用字段，提升效率）**：

```sql
SELECT 字段1, 字段2, ... FROM 表名;
```

示例：

```sql
SELECT username, nickname, age FROM sys_user;
```

**（3）条件查询（WHERE，核心）**：

```sql
SELECT 字段 FROM 表名 WHERE 条件;
```

**常用条件运算符**：

| 运算符 | 说明 |
|--------|------|
| `=` | 等于 |
| `!=` / `<>` | 不等于 |
| `>`、`<`、`>=`、`<=` | 比较运算符 |
| `BETWEEN ... AND ...` | 范围查询 |
| `IN` | 包含查询 |
| `LIKE` | 模糊查询（`%` 匹配任意字符） |
| `IS NULL` / `IS NOT NULL` | 空值判断 |
| `AND` / `OR` | 逻辑运算符 |

示例：

```sql
-- 查询年龄等于20的用户
SELECT * FROM sys_user WHERE age = 20;

-- 查询年龄在18-25之间的用户
SELECT * FROM sys_user WHERE age BETWEEN 18 AND 25;

-- 模糊查询（用户名包含"张"）
SELECT * FROM sys_user WHERE username LIKE '%张%';

-- 昵称不为空且年龄大于20
SELECT * FROM sys_user WHERE nickname IS NOT NULL AND age > 20;

-- 查询指定用户集合
SELECT * FROM sys_user WHERE username IN ('zhangsan', 'lisi');
```

**（4）排序查询（ORDER BY）**：

```sql
SELECT 字段 FROM 表名 ORDER BY 字段名 [ASC/DESC];
-- ASC：升序（默认），DESC：降序
```

示例：

```sql
-- 按创建时间降序查询（最新的用户在前）
SELECT * FROM sys_user ORDER BY create_time DESC;
```

**（5）分页查询（LIMIT，企业级列表必备）**：

```sql
-- 语法：LIMIT 起始索引, 每页条数（起始索引从0开始）
SELECT 字段 FROM 表名 LIMIT (页码-1)*每页条数, 每页条数;
```

示例：

```sql
-- 第1页，每页10条
SELECT * FROM sys_user LIMIT 0, 10;

-- 第2页，每页10条
SELECT * FROM sys_user LIMIT 10, 10;
```

**（6）去重查询（DISTINCT）**：

```sql
SELECT DISTINCT 字段 FROM 表名;   -- 去除字段中重复的值
```

示例：

```sql
SELECT DISTINCT age FROM sys_user;
```

**（7）聚合查询（常用函数）**：

| 函数 | 说明 |
|------|------|
| `COUNT(*)` | 统计条数 |
| `SUM(字段)` | 求和 |
| `AVG(字段)` | 平均值 |
| `MAX(字段)` | 最大值 |
| `MIN(字段)` | 最小值 |

```sql
-- 统计用户总数
SELECT COUNT(*) AS user_count FROM sys_user;

-- 统计年龄总和
SELECT SUM(age) AS total_age FROM sys_user;

-- 统计年龄平均值
SELECT AVG(age) AS avg_age FROM sys_user;

-- 查询最大年龄
SELECT MAX(age) AS max_age FROM sys_user;

-- 查询最小年龄
SELECT MIN(age) AS min_age FROM sys_user;
```

**（8）分组查询（GROUP BY）**：

```sql
SELECT 分组字段, 聚合函数 FROM 表名
GROUP BY 分组字段 [HAVING 分组条件];
```

> **注意**：`HAVING` 用于过滤分组后的结果，`WHERE` 用于过滤分组前的数据。

示例：

```sql
-- 按年龄分组，统计每个年龄的用户数量
SELECT age, COUNT(*) AS user_num
FROM sys_user
GROUP BY age
HAVING user_num > 1;
```

### 3. 修改数据（UPDATE，谨慎操作）

```sql
UPDATE 表名 SET 字段1=值1, 字段2=值2, ... WHERE 条件;   -- 必须加 WHERE，否则修改全表
```

示例：

```sql
-- 修改 id 为1的用户昵称和年龄
UPDATE sys_user SET nickname='张三三', age=21 WHERE id=1;

-- 批量修改（谨慎操作）
UPDATE sys_user SET nickname='老用户' WHERE age > 30;
```

### 4. 删除数据（DELETE，谨慎操作）

**（1）删除指定数据（必须加 WHERE）**：

```sql
DELETE FROM 表名 WHERE 条件;
```

示例：

```sql
DELETE FROM sys_user WHERE id=3;
```

**（2）删除全表数据（两种方式，差异很大）**：

| 方式 | 特点 |
|------|------|
| `DELETE FROM 表名;` | 逐行删除，**可回滚**，自增主键不重置 |
| `TRUNCATE TABLE 表名;` | **清空表**，不可回滚，自增主键重置，效率更高 |

---

## 三、进阶操作（关联查询/约束/索引）

### 1. 关联查询（多表查询，企业级常用）

> 假设有两张表：`sys_user`（用户表）、`sys_role`（角色表），关联字段：`sys_user.role_id = sys_role.id`

**（1）内连接（INNER JOIN，只查询两张表中匹配的数据）**：

```sql
SELECT u.username, u.nickname, r.role_name
FROM sys_user u
INNER JOIN sys_role r ON u.role_id = r.id;   -- u、r 是表的别名，简化书写
```

**（2）左连接（LEFT JOIN，查询左表所有数据，右表匹配不到则显示 NULL）**：

```sql
SELECT u.username, u.nickname, r.role_name
FROM sys_user u
LEFT JOIN sys_role r ON u.role_id = r.id;
```

**（3）右连接（RIGHT JOIN，查询右表所有数据，左表匹配不到则显示 NULL）**：

```sql
SELECT u.username, u.nickname, r.role_name
FROM sys_user u
RIGHT JOIN sys_role r ON u.role_id = r.id;
```

### 2. 约束（确保数据完整性）

| 约束类型 | 说明 | 示例 |
|---------|------|------|
| **主键约束（PRIMARY KEY）** | 唯一标识表中每条数据，不可重复、不可为空，一张表只能有一个主键 | `id BIGINT PRIMARY KEY AUTO_INCREMENT` |
| **唯一约束（UNIQUE）** | 字段值不可重复，可为空 | `username VARCHAR(20) UNIQUE` |
| **非空约束（NOT NULL）** | 字段值不可为空 | `password VARCHAR(50) NOT NULL` |
| **外键约束（FOREIGN KEY）** | 关联两张表，确保数据一致性 | 见下方示例 |

**外键约束示例**：

```sql
CREATE TABLE sys_user (
    ...,
    role_id BIGINT,
    FOREIGN KEY (role_id) REFERENCES sys_role(id)
    ON DELETE SET NULL   -- 当角色表 id 被删除时，用户表 role_id 设为 NULL
    -- ON DELETE CASCADE  -- 当角色表 id 被删除时，关联用户也被删除（谨慎使用）
);
```

### 3. 索引（提升查询效率，企业级优化必备）

**创建索引**：

```sql
-- 普通索引
CREATE INDEX idx_username ON sys_user(username);

-- 唯一索引（字段值唯一，同时提升查询效率）
CREATE UNIQUE INDEX idx_username ON sys_user(username);

-- 联合索引（多字段查询）
CREATE INDEX idx_username_age ON sys_user(username, age);
```

**查看索引**：

```sql
SHOW INDEX FROM 表名;
```

**删除索引**：

```sql
DROP INDEX 索引名 ON 表名;
```

> **注意**：索引会提升查询效率，但会降低新增、修改、删除的效率（需维护索引），避免给频繁修改的字段建索引。

---

## 四、常用辅助语句

**查看 SQL 执行计划（优化 SQL，查看是否使用索引）**：

```sql
EXPLAIN SELECT * FROM sys_user WHERE username = 'zhangsan';
```

**事务操作（确保多步操作原子性，要么全成，要么全败）**：

```sql
START TRANSACTION;                    -- 开启事务

UPDATE sys_user SET age=22 WHERE id=1;
DELETE FROM sys_user WHERE id=2;

COMMIT;                               -- 提交事务（所有操作生效）
-- ROLLBACK;                          -- 回滚事务（所有操作撤销，出现错误时使用）
```

**修改密码（MySQL 用户密码）**：

```sql
ALTER USER 'root'@'localhost' IDENTIFIED BY '新密码';
```

**导入/导出数据（命令行）**：

```bash
# 导出数据库
mysqldump -u root -p 数据库名 > 导出路径/文件名.sql

# 导入数据库（先创建数据库，再执行）
mysql -u root -p 数据库名 < 导入路径/文件名.sql
```

---

## 五、注意事项

1. 所有 SQL 语句结尾需加**英文分号（`;`）**，否则无法执行
2. 关键字（如 `SELECT`、`INSERT`、`UPDATE`）不区分大小写，但建议**大写**，提升可读性
3. 执行 `UPDATE`、`DELETE` 语句时，**必须加 `WHERE` 条件**，避免误操作全表
4. 字段名、表名若包含特殊字符（如空格、中文），需用**反引号（`` ` ``）**包裹
5. 插入字符串类型的值时，需用**单引号（`'`）**包裹，数字类型无需加引号
6. 聚合函数（`COUNT`、`SUM` 等）不能直接用于 `WHERE` 条件，需用 `HAVING`
7. **索引并非越多越好**，合理建索引（查询频繁、修改少的字段），避免冗余索引

---

## 📖 相关阅读

- [数据库编程核心知识详解](./数据库编程核心知识详解.md)
- [数据库-Oracle](./数据库-Oracle.md)
- [数据库-PostgreSQL](./数据库-PostgreSQL.md)
- [非关系型数据库 核心知识点](./非关系型数据库%20核心知识点.md)
- [数据库-XML](./数据库-XML.md)
