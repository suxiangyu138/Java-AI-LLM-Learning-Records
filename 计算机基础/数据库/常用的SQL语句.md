# 常用的 SQL 语句

> SQL（Structured Query Language）是操作关系型数据库的标准语言。本文汇总了日常开发中最常用的 SQL 语句，涵盖查询、增删改、表结构操作、约束、索引和常用函数，方便快速查阅。

## 📑 目录

- [一、查询语句（DQL）](#一查询语句dql)
- [二、增删改语句（DML）](#二增删改语句dml)
- [三、表结构操作（DDL）](#三表结构操作ddl)
- [四、约束相关](#四约束相关)
- [五、索引操作](#五索引操作)
- [六、常用函数](#六常用函数)

---

## 一、查询语句（DQL）

### 1. 查询所有字段

```sql
SELECT * FROM 表名;
```

### 2. 查询指定字段

```sql
SELECT 字段1, 字段2 FROM 表名;
```

### 3. 条件查询

```sql
SELECT * FROM 表名 WHERE 条件;
```

### 4. 模糊查询（LIKE）

```sql
SELECT * FROM 表名 WHERE 字段 LIKE '%关键词%';
```

### 5. 范围查询（IN / BETWEEN）

```sql
SELECT * FROM 表名 WHERE 字段 IN (值1, 值2);
SELECT * FROM 表名 WHERE 字段 BETWEEN 起始值 AND 结束值;
```

### 6. 排序（ORDER BY）

```sql
SELECT * FROM 表名 ORDER BY 字段 ASC;   -- 升序
SELECT * FROM 表名 ORDER BY 字段 DESC;  -- 降序
```

### 7. 分页（LIMIT）

```sql
SELECT * FROM 表名 LIMIT 起始位置, 条数;
```

### 8. 去重（DISTINCT）

```sql
SELECT DISTINCT 字段 FROM 表名;
```

### 9. 聚合查询

| 聚合函数 | 说明 |
|---------|------|
| `COUNT(*)` | 统计行数 |
| `SUM(字段)` | 求和 |
| `AVG(字段)` | 求平均值 |
| `MAX(字段)` | 求最大值 |
| `MIN(字段)` | 求最小值 |

```sql
SELECT COUNT(*) FROM 表名;
SELECT SUM(字段) FROM 表名;
SELECT AVG(字段) FROM 表名;
SELECT MAX(字段) FROM 表名;
SELECT MIN(字段) FROM 表名;
```

### 10. 分组查询（GROUP BY）

```sql
SELECT 字段, COUNT(*) FROM 表名 GROUP BY 字段;
```

### 11. 分组后筛选（HAVING）

```sql
SELECT 字段, COUNT(*) FROM 表名 GROUP BY 字段 HAVING COUNT(*) > 数值;
```

> **注意**：`WHERE` 用于分组前筛选，`HAVING` 用于分组后筛选。

### 12. 多表连接（JOIN）

```sql
-- 内连接：只返回匹配的行
SELECT * FROM 表1 JOIN 表2 ON 表1.关联字段 = 表2.关联字段;

-- 左连接：返回左表所有行，右表无匹配则为 NULL
SELECT * FROM 表1 LEFT JOIN 表2 ON 表1.关联字段 = 表2.关联字段;
```

---

## 二、增删改语句（DML）

### 1. 插入数据

```sql
INSERT INTO 表名 (字段1, 字段2) VALUES (值1, 值2);
```

### 2. 批量插入

```sql
INSERT INTO 表名 (字段1, 字段2) VALUES (值1, 值2), (值3, 值4);
```

### 3. 更新数据

```sql
UPDATE 表名 SET 字段1 = 值1, 字段2 = 值2 WHERE 条件;
```

> **注意**：更新操作务必带上 `WHERE` 条件，否则会更新整张表！

### 4. 删除数据

```sql
DELETE FROM 表名 WHERE 条件;
```

> **注意**：删除操作务必带上 `WHERE` 条件，否则会清空整张表！

---

## 三、表结构操作（DDL）

### 1. 创建表

```sql
CREATE TABLE 表名 (
    字段1 类型 约束,
    字段2 类型 约束
);
```

### 2. 删除表

```sql
DROP TABLE 表名;
```

### 3. 修改表名

```sql
ALTER TABLE 旧表名 RENAME TO 新表名;
```

### 4. 添加字段

```sql
ALTER TABLE 表名 ADD 字段 类型;
```

### 5. 修改字段

```sql
ALTER TABLE 表名 MODIFY 字段 新类型;
```

### 6. 删除字段

```sql
ALTER TABLE 表名 DROP 字段;
```

---

## 四、约束相关

### 1. 主键约束

```sql
ALTER TABLE 表名 ADD PRIMARY KEY (字段);
```

### 2. 唯一约束

```sql
ALTER TABLE 表名 ADD UNIQUE (字段);
```

### 3. 非空约束

```sql
ALTER TABLE 表名 MODIFY 字段 类型 NOT NULL;
```

### 4. 外键约束

```sql
ALTER TABLE 表名 ADD FOREIGN KEY (字段) REFERENCES 主表(主键);
```

---

## 五、索引操作

### 1. 创建索引

```sql
CREATE INDEX 索引名 ON 表名(字段);
```

### 2. 删除索引

```sql
DROP INDEX 索引名 ON 表名;
```

---

## 六、常用函数

### 1. 字符串函数

| 函数 | 说明 |
|------|------|
| `CONCAT(字段1, 字段2)` | 字符串拼接 |
| `SUBSTRING(字段, 起始位置, 长度)` | 截取子串 |
| `LENGTH(字段)` | 返回字符串长度 |

### 2. 日期函数

| 函数 | 说明 |
|------|------|
| `NOW()` | 当前日期时间 |
| `CURDATE()` | 当前日期 |
| `DATE_FORMAT(字段, '%Y-%m-%d')` | 日期格式化 |

### 3. 条件函数

```sql
-- IF 函数
IF(条件, 真值, 假值)

-- CASE 表达式
CASE WHEN 条件 THEN 结果 END
```

---

## 📖 相关阅读

- [DBeaver 理论 + 实战（完整可直接上手）](./DBeaver%20理论%20+%20实战（完整可直接上手）.md)
- [Navicat 手把手教程（从安装到企业级使用）](./Navicat%20手把手教程（从安装到企业级使用）.md)
- [数据库-Microsoft SQL Server](./数据库-Microsoft%20SQL%20Server.md)
- [数据库-IBM DB2 Universal Database](./数据库-IBM%20DB2%20Universal%20Database.md)
- [这条信息可以插入数据表吗](./这条信息可以插入数据表吗.md)
