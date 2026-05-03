# MySQL 数据库基本操作（最全入门版，考试/作业通用）
## 一、连接与基础语法规则
1. SQL 不区分大小写，建议关键字大写
2. 语句末尾必须加 `;`
3. 注释：
```sql
-- 单行注释
/* 多行注释 */
```

---

## 二、数据库操作（DDL）
### 1. 查看所有数据库
```sql
SHOW DATABASES;
```

### 2. 创建数据库
```sql
CREATE DATABASE 数据库名;
-- 指定编码
CREATE DATABASE 数据库名 DEFAULT CHARACTER SET utf8mb4;
```

### 3. 使用/切换数据库
```sql
USE 数据库名;
```

### 4. 查看当前数据库
```sql
SELECT DATABASE();
```

### 5. 删除数据库
```sql
DROP DATABASE 数据库名;
```

---

## 三、数据表操作（DDL）
### 1. 查看当前库所有表
```sql
SHOW TABLES;
```

### 2. 创建数据表
语法：
```sql
CREATE TABLE 表名(
    字段1 数据类型 约束,
    字段2 数据类型 约束,
    ...
)DEFAULT CHARSET=utf8mb4;
```
示例：
```sql
CREATE TABLE student(
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(20) NOT NULL,
    age INT DEFAULT 18,
    gender VARCHAR(10),
    phone VARCHAR(11) UNIQUE
);
```

### 3. 查看表结构
```sql
DESC 表名;
```

### 4. 修改表结构
```sql
-- 添加字段
ALTER TABLE 表名 ADD 字段名 类型;

-- 修改字段类型
ALTER TABLE 表名 MODIFY 字段名 新类型;

-- 修改字段名+类型
ALTER TABLE 表名 CHANGE 旧字段 新字段 类型;

-- 删除字段
ALTER TABLE 表名 DROP 字段名;

-- 修改表名
RENAME TABLE 旧表名 TO 新表名;
```

### 5. 删除数据表
```sql
DROP TABLE 表名;
```

---

## 四、数据增删改（DML）
### 1. 添加数据 INSERT
```sql
-- 指定字段插入
INSERT INTO student(name,age) VALUES ('张三',20);

-- 全字段插入
INSERT INTO student VALUES(NULL,'李四',19,'男','13800138000');

-- 批量插入
INSERT INTO student(name,age) 
VALUES 
('王五',21),
('赵六',18);
```

### 2. 修改数据 UPDATE
```sql
-- 带条件修改（必须加where）
UPDATE student SET age=22 WHERE name='张三';

-- 多字段修改
UPDATE student SET gender='女',age=19 WHERE id=3;
```

### 3. 删除数据 DELETE
```sql
-- 删除指定条件数据
DELETE FROM student WHERE id=5;

-- 清空表数据（保留表结构）
DELETE FROM student;
TRUNCATE TABLE student;
```
> 区别：
> `DELETE` 逐条删除，可回滚；
> `TRUNCATE` 清空重建，速度更快，自增归零。

---

## 五、数据查询（DQL 核心）
### 1. 基础查询
```sql
-- 查询所有列
SELECT * FROM student;

-- 查询指定列
SELECT name,age FROM student;

-- 别名
SELECT name AS 姓名,age AS 年龄 FROM student;

-- 去重
SELECT DISTINCT gender FROM student;
```

### 2. 条件查询 WHERE
运算符：`> < >= <= = != AND OR NOT BETWEEN...AND IN LIKE IS NULL`
```sql
SELECT * FROM student WHERE age >= 18;
SELECT * FROM student WHERE age BETWEEN 18 AND 22;
SELECT * FROM student WHERE name IN ('张三','李四');
-- 模糊查询
SELECT * FROM student WHERE name LIKE '张%';
```

### 3. 排序
```sql
-- ASC升序(默认)  DESC降序
SELECT * FROM student ORDER BY age DESC;
```

### 4. 分页
```sql
-- LIMIT 起始下标,条数
SELECT * FROM student LIMIT 0,5;
```

### 5. 聚合函数
```sql
COUNT() -- 统计行数
MAX()   -- 最大值
MIN()   -- 最小值
SUM()   -- 求和
AVG()   -- 平均值
```

### 6. 分组查询
```sql
SELECT gender,COUNT(*) FROM student GROUP BY gender;
-- 分组后条件 HAVING
SELECT age,COUNT(*) FROM student GROUP BY age HAVING COUNT(*)>=2;
```

---

## 六、常用约束（快速回顾）
- `PRIMARY KEY` 主键：唯一+非空
- `AUTO_INCREMENT` 自增
- `NOT NULL` 非空
- `UNIQUE` 唯一
- `DEFAULT` 默认值
- `CHECK` 范围限制

---

## 七、核心口诀
1. 建库建表用 `CREATE`，删库删表用 `DROP`
2. 改结构 `ALTER`，改数据 `UPDATE`
3. 查数据永远用 `SELECT`
4. 改、删必须加 `WHERE`，防止全表操作
