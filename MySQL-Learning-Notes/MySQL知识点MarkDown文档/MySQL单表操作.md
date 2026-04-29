# MySQL 单表操作 核心知识点（精简速记版）
这是**最常用、面试必考、开发必用**的 MySQL 单表核心知识点，按**建库建表 → 增删改查 → 约束 → 函数 → 索引**整理，直接背会就能搞定 80% 单表场景。

---

## 一、库 & 表基础操作
### 1. 数据库操作
```sql
-- 创建库
CREATE DATABASE 库名;
-- 删除库
DROP DATABASE 库名;
-- 使用库
USE 库名;
```

### 2. 表操作
```sql
-- 创建表（核心）
CREATE TABLE 表名(
  字段名 数据类型 约束,
  id INT PRIMARY KEY AUTO_INCREMENT  -- 主键自增（最常用）
);

-- 查看表结构
DESC 表名;

-- 删除表
DROP TABLE 表名;

-- 修改表名
RENAME TABLE 旧表名 TO 新表名;

-- 添加字段
ALTER TABLE 表名 ADD 字段名 数据类型;

-- 修改字段
ALTER TABLE 表名 MODIFY 字段名 新数据类型;
```

---

## 二、常用数据类型
### 1. 数值
- `INT`：整数
- `DECIMAL(m,n)`：小数（总长度m，小数位n，用于钱）
- `DOUBLE`：浮点型

### 2. 字符串
- `VARCHAR(n)`：可变长度字符串（姓名、标题）
- `CHAR(n)`：固定长度（手机号、身份证）

### 3. 日期时间
- `DATE`：日期（2025-01-01）
- `DATETIME`：日期+时间（2025-01-01 12:00:00）
- `TIMESTAMP`：时间戳

---

## 三、5 大约束（核心）
约束 = 给字段加**规则**，保证数据正确。

1. **PRIMARY KEY**：主键（唯一+非空，一张表一个）
2. **NOT NULL**：非空
3. **UNIQUE**：唯一（不能重复）
4. **DEFAULT**：默认值
5. **FOREIGN KEY**：外键（单表一般不用，多表关联用）

示例：
```sql
CREATE TABLE user(
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(20) NOT NULL,
  phone VARCHAR(11) UNIQUE NOT NULL,
  age INT DEFAULT 18
);
```

---

## 四、单表核心 SQL：增删改查（CRUD）
### 1. 新增（INSERT）
```sql
-- 全字段插入
INSERT INTO 表名 VALUES(值1,值2...);

-- 指定字段插入（推荐）
INSERT INTO 表名(字段1,字段2) VALUES(值1,值2);
```

### 2. 删除（DELETE）
```sql
-- 删除符合条件的数据
DELETE FROM 表名 WHERE 条件;

-- 清空全表（慎用）
TRUNCATE TABLE 表名;
```

### 3. 修改（UPDATE）
```sql
UPDATE 表名 SET 字段=值 WHERE 条件;
```
⚠️ **必须加 WHERE**，否则全表被修改！

### 4. 查询（SELECT）—— 最重要
```sql
-- 基础查询
SELECT * FROM 表名;  -- *代表所有字段
SELECT 字段1,字段2 FROM 表名;

-- 条件查询 WHERE
SELECT * FROM 表名 WHERE 条件;

-- 排序 ORDER BY（ASC升序/DESC降序）
SELECT * FROM 表名 ORDER BY 字段 DESC;

-- 分页 LIMIT（开发必用）
SELECT * FROM 表名 LIMIT 起始索引, 条数;
-- 例：第1页10条 → LIMIT 0,10；第2页 → LIMIT 10,10
```

---

## 五、WHERE 常用条件
```sql
=  >  <  >=  <=  !=
AND  OR  NOT
BETWEEN 开始值 AND 结束值
IN(值1,值2)
LIKE '%模糊值%'  -- %匹配任意字符
IS NULL / IS NOT NULL
```

示例：
```sql
-- 年龄18-30，姓名带"张"
SELECT * FROM user WHERE age BETWEEN 18 AND 30 AND name LIKE '%张%';
```

---

## 六、聚合函数（统计）
```sql
COUNT(*)  -- 总条数
SUM(字段)  -- 求和
AVG(字段)  -- 平均值
MAX(字段)  -- 最大值
MIN(字段)  -- 最小值
```

示例：
```sql
SELECT COUNT(*) FROM user;  -- 查询总人数
```

---

## 七、分组查询 GROUP BY
```sql
SELECT 分组字段,聚合函数 FROM 表名 GROUP BY 分组字段;

-- 分组后过滤 HAVING
SELECT dept,COUNT(*) FROM user 
GROUP BY dept 
HAVING COUNT(*) > 5;
```
⚠️ `WHERE` 分组前过滤，`HAVING` 分组后过滤。

---

## 八、去重 & 别名
```sql
-- 去重
SELECT DISTINCT 字段 FROM 表名;

-- 别名（AS可省略）
SELECT name AS 姓名 FROM user;
```

---

## 九、单表索引（优化必学）
作用：**加快查询速度**
```sql
-- 创建普通索引
CREATE INDEX 索引名 ON 表名(字段);

-- 创建唯一索引
CREATE UNIQUE INDEX 索引名 ON 表名(字段);
```
适用：经常出现在 `WHERE` / `ORDER BY` 的字段。

---

# 核心速记总结
1. **单表操作 = 建表 + 增删改查**
2. **查询最核心**：`SELECT + WHERE + ORDER BY + LIMIT`
3. **约束保证数据安全**：主键、非空、唯一、默认
4. **聚合+分组**做统计：`COUNT/GROUP BY/HAVING`
5. **索引优化查询**