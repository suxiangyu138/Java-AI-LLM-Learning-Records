# MySQL 数据库设计与优化
> 数据库设计是后端开发的根基，SQL 优化是后端性能的核心。本文涵盖数据库设计流程、E-R 模型、范式理论、表结构优化、索引优化、SQL 优化、配置优化及分库分表策略。

## 目录
1. [数据库设计概述](#1-数据库设计概述)
2. [E-R 模型与表间关系](#2-e-r-模型与表间关系)
3. [三大范式与反范式](#3-三大范式与反范式)
4. [表结构优化](#4-表结构优化)
5. [索引优化](#5-索引优化)
6. [SQL 语句优化](#6-sql-语句优化)
7. [数据库配置优化](#7-数据库配置优化)
8. [分库分表与读写分离](#8-分库分表与读写分离)
9. [慢查询排查](#9-慢查询排查)

---

## 1. 数据库设计概述

### 1.1 设计流程

| 阶段 | 活动 | 产出 |
|------|------|------|
| 需求分析 | 明确业务、数据、功能需求 | 需求文档 |
| 概念设计 | E-R 模型（实体、属性、关系） | E-R 图 |
| 逻辑设计 | 转为二维表结构、字段、约束 | 表结构设计 |
| 物理设计 | 建库、建表、索引、引擎选型 | DDL 语句 |
| 维护优化 | 调优、分表、权限管理 | 持续迭代 |

### 1.2 设计原则

| 原则 | 说明 |
|------|------|
| 完整性 | 数据准确、一致、约束完整 |
| 低冗余 | 合理设计减少数据重复 |
| 高效性 | 查询高效、写入不阻塞 |
| 可扩展 | 预留扩展字段，方便业务迭代 |
| 安全性 | 权限分级、敏感数据加密 |

---

## 2. E-R 模型与表间关系

### 2.1 E-R 三大要素

| 要素 | 说明 | 对应数据库 |
|------|------|-----------|
| 实体（Entity） | 现实中的事物 | 数据表 |
| 属性（Attribute） | 实体的特征 | 字段 |
| 关系（Relationship）| 实体间的关联 | 外键/中间表 |

### 2.2 三种表间关系

| 关系 | 示例 | 实现方式 |
|------|------|----------|
| 一对一 | 用户 ↔ 用户身份证信息 | 任意一方加外键 + UNIQUE |
| 一对多（最常用） | 部门(1) → 员工(n) | "多"方加外键 |
| 多对多 | 学生(n) ↔ 课程(n) | 新建中间表 |

```sql
-- 一对多：员工表加 dept_id 外键
CREATE TABLE t_employee (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    name    VARCHAR(50),
    dept_id BIGINT COMMENT '部门ID',
    FOREIGN KEY (dept_id) REFERENCES t_dept(id)
);

-- 多对多：新建中间表
CREATE TABLE t_student_course (
    student_id BIGINT NOT NULL,
    course_id  BIGINT NOT NULL,
    PRIMARY KEY (student_id, course_id)
);
```

> ⚠️ **企业开发少用物理外键**（`FOREIGN KEY`），多用逻辑关联（代码层面保证一致性）。物理外键会导致高并发下锁竞争、扩容迁移困难。

---

## 3. 三大范式与反范式

### 3.1 范式概念

| 范式 | 核心要求 | 说明 |
|------|----------|------|
| 1NF | 列不可再分 | 每个字段都是原子值 |
| 2NF | 非主键列完全依赖于主键 | 消除部分依赖 |
| 3NF | 非主键列不传递依赖于主键 | 消除传递依赖 |

### 3.2 范式示例

```sql
-- 违反 1NF（address 可再分）
CREATE TABLE t_user (
    address VARCHAR(200)  -- "北京市海淀区中关村" 可拆分为省/市/区
);

-- 违反 2NF（score 只依赖于 student_id + course_id 的组合主键，而非部分依赖）
CREATE TABLE t_score (
    student_id INT,
    course_id INT,
    score INT,
    course_name VARCHAR(50),  -- 只依赖 course_id，不依赖 student_id
    PRIMARY KEY (student_id, course_id)
);

-- 违反 3NF（dept_name 传递依赖于 user_id → dept_id → dept_name）
CREATE TABLE t_user (
    id INT PRIMARY KEY,
    dept_id INT,
    dept_name VARCHAR(50)  -- 应从部门表查询
);
```

### 3.3 反范式

**合理反范式化**——在查询性能要求高的场景，适当冗余字段以减少 JOIN：

| 场景 | 反范式做法 | 优点 | 缺点 |
|------|-----------|------|------|
| 订单列表需显示用户名 | 订单表冗余 `username` | 避免 JOIN 用户表 | 用户名变更需同步更新 |
| 文章列表需显示评论数 | 文章表冗余 `comment_count` | 避免 COUNT 子查询 | 增删评论时需计数维护 |

> 💡 **范式化是理论指导，反范式是工程权衡**——"先满足 3NF，在性能瓶颈处适度反范式"。

---

## 4. 表结构优化

### 4.1 字段设计最佳实践

| 实践 | 说明 |
|------|------|
| 能用整型不用字符串 | 如性别用 TINYINT 不用 VARCHAR |
| 金额使用 DECIMAL | 禁用 DOUBLE，避免精度丢失 |
| 字符串优先 VARCHAR | 固定短文本用 CHAR |
| 时间统一 DATETIME | 范围大、不受时区影响 |
| 加入通用字段 | `created_at`、`updated_at`、`is_deleted` |
| 主键简短 | 自增 ID 或雪花 ID，禁用大字段/复合主键 |
| 存储引擎统一 InnoDB | 支持事务、行锁、崩溃恢复 |

### 4.2 字段类型选择建议

| 数据类型 | 适用场景 |
|----------|----------|
| TINYINT | 状态、性别、删除标记 |
| INT / BIGINT | 主键 ID |
| DECIMAL(10,2) | 金额、价格（必用） |
| VARCHAR(50~200) | 姓名、地址、描述 |
| CHAR(11) | 手机号（固定长度） |
| DATETIME | 时间字段 |
| TEXT | 长文本内容（文章正文等） |

---

## 5. 索引优化

### 5.1 索引设计原则

| 原则 | 说明 |
|------|------|
| 查询频繁的字段建索引 | WHERE、JOIN、ORDER BY 涉及的字段 |
| 区分度高的字段优先 | 如身份证号 > 性别 |
| 联合索引注意最左匹配 | (a,b,c) 可匹配 a, a+b, a+b+c |
| 索引不是越多越好 | 每个索引增加写入开销和存储空间 |
| 长字段用前缀索引 | `INDEX(column(N))` |
| 避免冗余索引 | 已有 (a,b) 则 (a) 是冗余的 |

### 5.2 索引失效场景

| 场景 | 示例 | 解决方案 |
|------|------|----------|
| LIKE 以 % 开头 | `LIKE '%keyword'` | 改 `LIKE 'keyword%'` 或全文索引 |
| 隐式类型转换 | `WHERE phone = 13800000000` | 字符串加引号 |
| OR 两边未都建索引 | `WHERE a=1 OR b=2` | 两边都建索引或用 UNION |
| NOT IN / NOT EXISTS | `WHERE id NOT IN (1,2)` | 改写为 LEFT JOIN + IS NULL |
| 函数操作 | `WHERE YEAR(created_at)=2024` | 改 `created_at BETWEEN ... AND ...` |
| 联合索引跳列 | 索引(a,b,c)，查询 b 条件 | 调整索引顺序或补 a 条件 |

### 5.3 EXPLAIN 解读

| 字段 | 说明 | 重点关注 |
|------|------|----------|
| type | 访问类型 | `const > ref > range > index > ALL`（ALL 全表扫描要避免） |
| key | 实际使用的索引 | NULL 表示没走索引 |
| rows | 预估扫描行数 | 越小越好 |
| Extra | 额外信息 | `Using filesort` 需优化，`Using index` 是好事（覆盖索引） |

```sql
EXPLAIN SELECT u.name, o.order_no
FROM t_user u
JOIN t_order o ON u.id = o.user_id
WHERE u.status = 1
ORDER BY o.created_at DESC;
```

---

## 6. SQL 语句优化

### 6.1 优化原则

| 原则 | 说明 |
|------|------|
| 禁止 SELECT * | 只查需要的字段（走覆盖索引、减少 IO） |
| 分页优化 | 大分页用主键回溯代替 OFFSET |
| 少用子查询 | JOIN 通常优于子查询 |
| 批量操作 | INSERT 用批量、循环操作合并为一条 SQL |
| 合理用 JOIN | 多表 JOIN 控制在 3 张以内 |
| 用 EXISTS 代替 IN | 子表数据量大时 EXISTS 更优 |

### 6.2 常见优化示例

```sql
-- 大分页优化（主键回溯）
-- 慢：OFFSET 越大越慢
SELECT * FROM t_order ORDER BY id LIMIT 100000, 10;
-- 快：记录上一页最后一个 id
SELECT * FROM t_order WHERE id > 100000 ORDER BY id LIMIT 10;

-- COUNT 优化
-- InnoDB 下 count(*) 最优，count(字段) 最慢（判非空）
SELECT COUNT(*) FROM t_order;
SELECT COUNT(1) FROM t_order;     -- 同 count(*)

-- 避免函数操作索引列
-- 慢
SELECT * FROM t_order WHERE DATE(created_at) = '2024-01-01';
-- 快
SELECT * FROM t_order WHERE created_at >= '2024-01-01' AND created_at < '2024-01-02';

-- 用 UNION 代替 OR
SELECT * FROM t_user WHERE name = '张三'
UNION
SELECT * FROM t_user WHERE phone = '13800138000';
```

### 6.3 大表加索引

```sql
-- MySQL 8.0+ 在线加索引（不锁表）
ALTER TABLE t_order ADD INDEX idx_user_id (user_id), ALGORITHM=INPLACE, LOCK=NONE;
```

---

## 7. 数据库配置优化

### 7.1 服务器配置

| 配置项 | 建议值 | 说明 |
|--------|--------|------|
| max_connections | 200~500 | 最大连接数，根据业务调整 |
| innodb_buffer_pool_size | 物理内存的 60%~70% | InnoDB 缓存池，最重要的性能参数 |
| innodb_log_file_size | 256MB~1GB | redo log 文件大小 |
| query_cache_size | 0（MySQL 8.0 已废弃） | 查询缓存，生产环境建议关闭 |

### 7.2 配置示例（my.ini）

```ini
[mysqld]
port=3306
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
max_connections=200
innodb_buffer_pool_size=2G
innodb_log_file_size=512M
innodb_flush_log_at_trx_commit=2
default-storage-engine=InnoDB
sql_mode=STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION
```

---

## 8. 分库分表与读写分离

### 8.1 分库分表策略

| 方式 | 说明 | 适用场景 |
|------|------|----------|
| 垂直拆分 | 按业务模块拆到不同库 | 微服务架构 |
| 水平拆分 | 按 ID/时间分片，拆大表到多张表 | 单表数据超千万 |

### 8.2 水平分片常见策略

| 策略 | 说明 | 优点 | 缺点 |
|------|------|------|------|
| 取模分片 | `id % 16` | 数据均匀 | 扩容需迁移 |
| 范围分片 | 按时间区间 | 扩容方便 | 热点集中 |
| 哈希分片 | 对分片键哈希 | 数据均匀 | 不支持范围查询 |

### 8.3 读写分离

```text
主库（Master）：写操作（INSERT/UPDATE/DELETE）
从库（Slave）：读操作（SELECT），可多台分担
          ┌─────────┐
          │ 应用服务  │
          └────┬────┘
               │
        ┌──────┴──────┐
        │   中间件     │（ShardingSphere / MyCat）
        └──────┬──────┘
               │
      ┌────────┴────────┐
      │                 │
   ┌──┴──┐          ┌──┴──┐
   │ Master │      │ Slave │
   │ 写入   │      │ 读取   │
   └───────┘      └──────┘
```

> 💡 **读写分离的核心**：应用层或中间件决定 SQL 的路由目标，写走主库、读走从库。

---

## 9. 慢查询排查

### 9.1 慢查询日志

```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 1;       -- 超过 1 秒记录
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow.log';

-- 查看慢查询配置
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';
```

### 9.2 常用排查命令

```sql
-- 查看当前连接状态
SHOW PROCESSLIST;

-- 查看 InnoDB 状态（含锁信息）
SHOW ENGINE INNODB STATUS;

-- 查看 SQL 执行耗时
SET profiling = 1;
SELECT * FROM t_order WHERE amount > 1000;
SHOW PROFILES;
SHOW PROFILE FOR QUERY 1;

-- EXPLAIN 分析执行计划（核心）
EXPLAIN FORMAT=JSON SELECT * FROM t_order WHERE amount > 1000;
```

### 9.3 优化流程

```
定位慢查询 → EXPLAIN 分析 → 检查索引 → 改写 SQL → 验证效果
     ↓                                                     ↓
  开启慢查询日志                                     EXPLAIN 确认
     ↓                                                     ↓
  SHOW PROCESSLIST                                  type/key/rows 改善
```

> 🎯 **数据库设计是在项目开始时就决定的，后期改造成本极高**——在写第一行代码之前，先把表结构设计好。
