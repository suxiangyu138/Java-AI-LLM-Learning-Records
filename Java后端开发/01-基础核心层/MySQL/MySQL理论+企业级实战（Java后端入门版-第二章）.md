# MySQL 数据库基本操作（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MySQL 数据库基本操作  
> **所属章节**：第二章：MySQL 数据库基本操作（Java 后端实战核心）  
> **前置基础**：[第一章：数据库入门](./MySQL理论+企业级实战（Java后端入门版）.md)  
> **核心说明**：数据库基本操作是 Java 后端开发的必备技能，所有业务接口（登录、注册、下单等）都依赖这些操作。本章以"用户管理"场景的 `db_user_manage` 数据库和 `t_user` 表为基础，讲解 **数据库操作、表操作、数据操作** 三大核心，全程贴合企业级开发规范，重点标注 Java 后端实战注意点。

---

## 目录

- [一、数据库操作（DB Operation）](#一数据库操作db-operation)
  - [1.1 核心操作语法](#11-核心操作语法)
  - [1.2 企业级实战注意事项](#12-企业级实战注意事项)
- [二、表操作（Table Operation）](#二表操作table-operation)
  - [2.1 创建表（CREATE TABLE）](#21-创建表create-table)
  - [2.2 查询表（SHOW / DESC）](#22-查询表show--desc)
  - [2.3 修改表（ALTER TABLE）](#23-修改表alter-table)
  - [2.4 删除表（DROP TABLE）](#24-删除表drop-table)
- [三、数据操作 — CRUD（Java 后端核心实战）](#三数据操作--crudjava-后端核心实战)
  - [3.1 新增数据（INSERT）](#31-新增数据insert)
  - [3.2 查询数据（SELECT）](#32-查询数据select)
  - [3.3 修改数据（UPDATE）](#33-修改数据update)
  - [3.4 删除数据（DELETE）](#34-删除数据delete)
- [四、本章实战练习](#四本章实战练习)

---

## 一、数据库操作（DB Operation）

数据库操作主要针对 **数据库本身**，常用操作包括：创建数据库、查询数据库、删除数据库、切换数据库。

> **Java 后端视角**：创建数据库通常由运维或开发负责人执行，后端工程师主要负责 **切换数据库** 和 **查询数据库信息**（排查环境问题）。

### 1.1 核心操作语法

#### 1. 创建数据库

企业级创建数据库必须指定字符集和排序规则，避免中文乱码。

```sql
-- IF NOT EXISTS：避免重复创建报错（生产环境必备）
CREATE DATABASE IF NOT EXISTS 数据库名
    CHARACTER SET utf8mb4          -- 字符集，企业级固定 utf8mb4
    COLLATE utf8mb4_general_ci;    -- 排序规则，适配中文查询
```

**示例：创建电商数据库**

```sql
CREATE DATABASE IF NOT EXISTS db_ecommerce
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;
```

#### 2. 查询所有数据库

```sql
-- 排查环境，确认数据库是否存在
SHOW DATABASES;
```

#### 3. 查询当前使用的数据库

```sql
-- Java 后端排查连接异常常用
SELECT DATABASE();
```

#### 4. 切换数据库

```sql
-- 后续表操作、数据操作，必须先切换到目标数据库
USE 数据库名;
```

**示例：**

```sql
USE db_user_manage;
```

#### 5. 删除数据库

```sql
-- IF EXISTS：避免删除不存在的数据库报错
DROP DATABASE IF EXISTS 数据库名;
```

**示例：**

```sql
-- 仅本地测试使用
DROP DATABASE IF EXISTS db_test;
```

> **⚠️ 高危操作！** 生产环境禁止直接执行，删除前必须备份数据，需走审批流程。

### 1.2 企业级实战注意事项

| 注意事项 | 说明 | 实践要点 |
|----------|------|----------|
| **禁止直接删除数据库** | 生产环境中需走审批流程，删除前必须备份 | 后端开发无删除数据库权限，通常由运维负责 |
| **字符集固定 utf8mb4** | 无论什么项目，创建数据库时统一指定 | 避免中文、表情符号乱码；Java 后端接收前端传参时也需统一编码 |
| **确认当前数据库** | Java 代码通过连接池指定数据库，无需手动 `USE` | 排查问题时（Navicat、命令行调试）必须确认，否则出现"表不存在"报错 |

---

## 二、表操作（Table Operation）

表操作是 Java 后端开发的高频操作（如需求迭代时新增字段、修改字段类型），核心围绕 **表的结构** 操作，以 `t_user` 表为示例，贴合 Java 实体类设计。

| 操作 | SQL 关键字 | 使用频率 | 风险等级 |
|------|-----------|----------|----------|
| 创建表 | `CREATE TABLE` | ⭐⭐⭐⭐⭐ | 低 |
| 查询表 | `SHOW TABLES` / `DESC` | ⭐⭐⭐⭐ | 无 |
| 修改表 | `ALTER TABLE` | ⭐⭐⭐⭐ | 中 |
| 删除表 | `DROP TABLE` | ⭐ | 🔴 高危 |

### 2.1 创建表（CREATE TABLE）

#### 理论核心

创建表时需指定 **表名、字段名、字段类型、字段约束**，企业级表设计需遵循"字段精简、约束明确"原则，与 Java 实体类一一对应。

#### 基本语法

```sql
CREATE TABLE IF NOT EXISTS 表名 (
    字段名1 数据类型 约束条件 COMMENT '注释',
    字段名2 数据类型 约束条件 COMMENT '注释',
    ...
    字段名n 数据类型 约束条件 COMMENT '注释'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

> **企业级规范**：引擎固定 `InnoDB`（支持事务、外键），字符集固定 `utf8mb4`，每个字段必加 `COMMENT`。

#### 示例 1：创建商品表（t_goods）

```sql
CREATE TABLE IF NOT EXISTS t_goods (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    goods_name  VARCHAR(100) NOT NULL COMMENT '商品名称',
    price       DECIMAL(10,2) NOT NULL COMMENT '商品价格（元）',
    stock       INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 示例 2：创建用户表（t_user）

```sql
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username    VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT '密码（加密存储）',
    phone       VARCHAR(20) COMMENT '手机号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### Java 类型映射对照

| MySQL 字段 | MySQL 类型 | Java 类型 | 说明 |
|-----------|-----------|----------|------|
| `id` | `BIGINT` | `Long` | 主键自增 |
| `username` | `VARCHAR(50)` | `String` | 非空、唯一 |
| `password` | `VARCHAR(100)` | `String` | 加密后存储 |
| `phone` | `VARCHAR(20)` | `String` | 可空 |
| `create_time` | `DATETIME` | `LocalDateTime` | 默认当前时间 |
| `price` | `DECIMAL(10,2)` | `BigDecimal` | 金额字段，禁止用 `Double` |

> **💡 Java 后端关联**：表结构与 Java 实体类字段名、数据类型完全对应，后续通过 MyBatis 框架映射时无需额外配置，减少数据映射异常。

### 2.2 查询表（SHOW / DESC）

核心用途：排查表结构是否正确（如字段是否存在、数据类型是否匹配），Java 后端调试时高频使用。

```sql
-- 1. 查询当前数据库中所有的表
SHOW TABLES;

-- 2. 查询表的详细结构（最常用：查看字段、类型、约束）
DESC 表名;
-- 示例
DESC t_user;

-- 3. 查询创建表的完整 SQL（排查表引擎、字符集、约束细节）
SHOW CREATE TABLE 表名;
-- 示例
SHOW CREATE TABLE t_goods;
```

### 2.3 修改表（ALTER TABLE）

> **高频实战**：Java 后端需求迭代时经常需要修改表结构，核心语法为 `ALTER TABLE`，需注意避免影响现有数据。

#### 前置操作

```sql
-- 必须先切换到目标数据库
USE db_user_manage;
```

#### 操作速查

| 操作 | 语法 | 风险 |
|------|------|------|
| 新增字段 | `ALTER TABLE 表名 ADD COLUMN 字段名 类型 约束 COMMENT '注释';` | 低 |
| 修改字段类型 | `ALTER TABLE 表名 MODIFY COLUMN 字段名 新类型;` | ⚠️ 中 |
| 修改字段名 | `ALTER TABLE 表名 CHANGE COLUMN 旧名 新名 类型;` | ⚠️ 中 |
| 删除字段 | `ALTER TABLE 表名 DROP COLUMN 字段名;` | 🔴 高 |
| 修改表名 | `ALTER TABLE 旧表名 RENAME TO 新表名;` | ⚠️ 中 |

#### 1. 新增字段（最常用）

```sql
-- 给 t_user 表新增"邮箱"字段
ALTER TABLE t_user
    ADD COLUMN email VARCHAR(100) UNIQUE COMMENT '用户邮箱';
```

> **企业级规范**：`COMMENT` 必加，方便团队协作；字段尽量设置默认值或允许为空，避免插入数据时报错。

#### 2. 修改字段类型（谨慎使用）

```sql
-- 将 phone 字段长度从 20 改为 11（适配手机号规范）
ALTER TABLE t_user
    MODIFY COLUMN phone VARCHAR(11);
```

> **⚠️ 注意**：修改字段类型可能导致数据截断或丢失，执行前确认现有数据兼容新类型。

#### 3. 修改字段名（极少用）

```sql
ALTER TABLE t_user
    CHANGE COLUMN phone user_phone VARCHAR(11);
```

> **⚠️ 注意**：修改字段名会影响 Java 代码映射，需同步修改实体类和 MyBatis 配置。

#### 4. 删除字段（高危）

```sql
-- 删除 email 字段（仅测试使用）
ALTER TABLE t_user
    DROP COLUMN email;
```

> **🔴 高危**：需确认字段无业务依赖，生产环境操作前必须备份。

#### 5. 修改表名（极少用）

```sql
ALTER TABLE t_user
    RENAME TO t_sys_user;
```

> **⚠️ 注意**：需同步修改 Java 实体类名、Mapper 映射、所有引用该表的代码。

#### 企业级注意事项

- 修改表结构前，先备份表数据
- 生产环境修改表结构需在低峰期执行（避免锁表影响业务）
- 新增字段时，尽量设置默认值或允许为空

### 2.4 删除表（DROP TABLE）

```sql
-- IF EXISTS：避免删除不存在的表报错
DROP TABLE IF EXISTS 表名;

-- 示例：删除测试用的表
DROP TABLE IF EXISTS t_test;
```

> **🔴 高危操作！** 删除表会彻底删除表结构和所有数据，生产环境严格禁止直接执行，仅本地测试时使用。后端开发通常无删除表权限，生产环境中表删除需运维配合，且必须先备份数据。

---

## 三、数据操作 — CRUD（Java 后端核心实战）

数据操作是 Java 后端最核心的操作，对应业务中的 **新增数据（注册）、查询数据（登录/列表）、修改数据（编辑）、删除数据（注销）**，全程以 `t_user` 表为示例。

| 操作 | SQL 关键字 | 业务场景 |
|------|-----------|----------|
| **C**reate | `INSERT` | 用户注册、下单、新增商品 |
| **R**ead | `SELECT` | 用户登录、列表查询、详情查看 |
| **U**pdate | `UPDATE` | 编辑个人信息、修改库存、更新订单状态 |
| **D**elete | `DELETE` / 逻辑删除 | 用户注销、订单取消 |

### 3.1 新增数据（INSERT）

#### 核心用途

用户注册、下单、新增商品等场景，Java 后端通过代码执行 `INSERT` 语句，将前端传参存入数据库。

#### 前置操作

```sql
USE db_user_manage;
```

#### 方式 1：指定字段新增（推荐）

```sql
-- 指定所有字段，避免字段顺序变化导致报错
INSERT INTO t_user (username, password, phone)
VALUES ('lisi', '654321', '13900139000');
```

#### 方式 2：不指定字段新增（不推荐）

```sql
-- 字段顺序变化会报错，不推荐
INSERT INTO t_user VALUES (null, 'wangwu', '111111', '13700137000', NOW());
```

> **说明**：`id` 是自增字段，可传 `null`，MySQL 会自动生成自增 ID。

#### 方式 3：批量新增（企业级高频）

```sql
-- 批量导入用户，提升效率
INSERT INTO t_user (username, password, phone)
VALUES
    ('zhaoliu', '222222', '13600136000'),
    ('qianqi',  '333333', '13500135000');
```

#### 企业级优化：避免重复新增

```sql
-- IGNORE：如果用户名重复（唯一约束），自动忽略，不报错
INSERT IGNORE INTO t_user (username, password, phone)
VALUES ('zhangsan', '123456', '13800138000');
```

> **💡 Java 后端实战**：通过 MyBatis 的 `insert` 标签执行新增操作，参数从前端接收。密码需先加密再传入数据库，禁止明文存储。

### 3.2 查询数据（SELECT）

查询是 Java 后端最高频的操作（如用户登录、商品列表），核心语法为 `SELECT`，可搭配条件、排序、分页。

#### 1. 查询所有数据

```sql
-- 仅本地测试用，生产环境禁止（数据量大时会卡顿）
SELECT * FROM t_user;
```

#### 2. 查询指定字段（推荐）

```sql
-- 减少数据传输，提升效率，适配用户列表接口
SELECT id, username, phone FROM t_user;
```

#### 3. 带条件查询（最常用）

```sql
-- 用户登录：根据用户名查询
SELECT * FROM t_user WHERE username = 'zhangsan';

-- 模糊查询：查询手机号以 138 开头的用户
SELECT id, username, phone FROM t_user WHERE phone LIKE '138%';
```

#### 4. 排序查询

```sql
-- 用户列表按创建时间倒序
SELECT * FROM t_user ORDER BY create_time DESC;
-- DESC：倒序 | ASC：正序（默认）
```

#### 5. 分页查询（企业级高频）

```sql
-- 语法：LIMIT 起始索引, 每页条数（起始索引从 0 开始）
-- 查询第 1 页，每页 2 条
SELECT * FROM t_user LIMIT 0, 2;

-- 查询第 2 页，每页 2 条（起始索引 = (页码 - 1) × 每页条数）
SELECT * FROM t_user LIMIT 2, 2;
```

#### 6. 去重查询

```sql
-- 查询所有不重复的手机号
SELECT DISTINCT phone FROM t_user;
```

> **💡 Java 后端实战**：登录时执行 `SELECT * FROM t_user WHERE username = ?`，将前端用户名作为参数查询用户信息，再对比加密后的密码。列表查询必用分页，避免数据量大导致接口超时，分页参数（页码、每页条数）从前端接收。

### 3.3 修改数据（UPDATE）

#### 核心用途

用户编辑个人信息、修改商品库存、更新订单状态等场景。

> **🔴 核心原则：必须加 WHERE 条件，否则会修改表中所有数据！**

#### 基本语法

```sql
UPDATE 表名 SET 字段 = 值 WHERE 条件;
```

#### 示例 1：精确修改

```sql
-- 重置密码：修改用户名为 zhangsan 的密码
UPDATE t_user SET password = '888888' WHERE username = 'zhangsan';
```

#### 示例 2：多字段修改

```sql
-- 编辑个人信息：修改 id 为 1 的用户的手机号和邮箱
-- 先确保 email 字段存在（参考 2.3 节）
ALTER TABLE t_user ADD COLUMN email VARCHAR(100) UNIQUE COMMENT '用户邮箱';

UPDATE t_user
SET phone = '13800000000', email = 'zhangsan@163.com'
WHERE id = 1;
```

#### 示例 3：批量修改

```sql
-- 批量修改：将 id 为 1、2、3 的商品库存设为 0
UPDATE t_goods SET stock = 0 WHERE id IN (1, 2, 3);
```

#### ❌ 禁止操作

```sql
-- 生产环境绝对禁止！无 WHERE 条件会修改表中所有数据
UPDATE t_user SET password = '123456';
```

#### 企业级注意事项

| 规范 | 说明 |
|------|------|
| **WHERE 精准** | 优先使用主键 `id` 作为条件，避免误改其他数据 |
| **操作记录** | 生产环境修改操作需记录日志，便于追溯 |
| **权限校验** | 修改敏感数据（密码、手机号）需验证用户权限 |

### 3.4 删除数据（DELETE）

删除数据分为 **物理删除** 和 **逻辑删除**，企业级开发优先使用逻辑删除。

| 删除方式 | 说明 | 是否可恢复 | 企业级态度 |
|----------|------|-----------|-----------|
| **逻辑删除** | 仅标记删除，不删除实际数据 | ✅ 可恢复 | ⭐ 首选 |
| **物理删除** | 彻底删除数据 | ❌ 不可恢复 | 🔴 谨慎使用 |

#### 物理删除（高危）

```sql
-- 删除 id 为 5 的用户（物理删除，无法恢复）
DELETE FROM t_user WHERE id = 5;
```

> **🔴 必须加 WHERE 条件**，否则删除整张表所有数据！

#### 逻辑删除（企业级首选）

```sql
-- 步骤 1：给表新增"删除标记"字段
ALTER TABLE t_user
    ADD COLUMN is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除 1已删除';

-- 步骤 2：逻辑删除 — 仅修改 is_delete 字段
UPDATE t_user SET is_delete = 1 WHERE id = 4;

-- 步骤 3：查询时过滤已删除数据（Java 后端接口必做）
SELECT * FROM t_user WHERE is_delete = 0;

-- 批量逻辑删除
UPDATE t_user SET is_delete = 1 WHERE id IN (6, 7);
```

#### 企业级实践

> **💡 Java 后端实战**：用户注销功能采用逻辑删除（修改 `is_delete = 1`），后续所有查询（用户列表、登录等）都需过滤 `is_delete = 1` 的数据。物理删除仅用于测试数据清理，且需审批。

---

## 四、本章实战练习

### 练习目标

结合本章知识点，完成"用户管理"相关的基本操作，模拟 Java 后端业务场景，为后续 Java 代码操作数据库铺垫。

### 练习内容

1. **创建数据库** `db_test_manage`，指定字符集 `utf8mb4`

2. **创建表** `t_student`，字段如下（对应 Java 实体类 `Student`）：

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | 主键自增 |
| `student_name` | `VARCHAR(50)` | `NOT NULL` | 学生姓名 |
| `age` | `INT` | `NOT NULL DEFAULT 0` | 年龄 |
| `gender` | `VARCHAR(10)` | 可空 | 性别 |
| `create_time` | `DATETIME` | `DEFAULT CURRENT_TIMESTAMP` | 创建时间 |
| `is_delete` | `TINYINT` | `NOT NULL DEFAULT 0` | 逻辑删除标记 |

3. **批量插入** 3 条测试数据

4. **查询** 年龄大于 18 的学生信息，按创建时间倒序排列

5. **修改** id 为 1 的学生的年龄为 20

6. **逻辑删除** id 为 3 的学生

7. **分页查询** 未删除的学生信息（第 1 页，每页 2 条）

### 参考答案

<details>
<summary>点击展开参考 SQL</summary>

```sql
-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS db_test_manage
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE db_test_manage;

-- 2. 创建表
CREATE TABLE IF NOT EXISTS t_student (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    student_name VARCHAR(50) NOT NULL COMMENT '学生姓名',
    age          INT NOT NULL DEFAULT 0 COMMENT '年龄',
    gender       VARCHAR(10) COMMENT '性别',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_delete    TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 批量插入测试数据
INSERT INTO t_student (student_name, age, gender) VALUES
    ('张三', 20, '男'),
    ('李四', 17, '女'),
    ('王五', 22, '男');

-- 4. 查询年龄大于 18 的学生，按创建时间倒序
SELECT * FROM t_student WHERE age > 18 ORDER BY create_time DESC;

-- 5. 修改 id 为 1 的学生的年龄为 20
UPDATE t_student SET age = 20 WHERE id = 1;

-- 6. 逻辑删除 id 为 3 的学生
UPDATE t_student SET is_delete = 1 WHERE id = 3;

-- 7. 分页查询未删除的学生（第 1 页，每页 2 条）
SELECT * FROM t_student WHERE is_delete = 0 LIMIT 0, 2;
```

</details>

> **💡 提示**：练习时可结合可视化工具（Navicat、DBeaver）和命令行，模拟 Java 后端开发中"调试 SQL"的场景，确保每一步操作符合企业级规范。

---

## 本章小结

| 维度 | 核心要点 |
|------|----------|
| **数据库操作** | 创建（指定 `utf8mb4`）、切换（`USE`）、查询（`SHOW` / `SELECT DATABASE()`），禁止直接删除 |
| **表操作** | `CREATE` / `ALTER` / `DESC` / `DROP`，企业级表设计字段精简、约束明确、必加 `COMMENT` |
| **数据操作** | `INSERT` 指定字段 / 批量 / `IGNORE`；`SELECT` 指定字段 / 条件 / 分页；`UPDATE` 必须加 `WHERE`；`DELETE` 优先逻辑删除 |
| **Java 关联** | 表结构 ↔ 实体类一一对应，字段名 / 类型 / 约束保持一致，为 MyBatis 映射打基础 |
| **企业级红线** | 禁止无 `WHERE` 的 `UPDATE` / `DELETE`；生产环境禁止直接 `DROP`；密码禁止明文存储；修改表结构需低峰期 + 备份 |
