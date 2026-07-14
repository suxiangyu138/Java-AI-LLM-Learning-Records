# MySQL 期末复习实操项目（适配期末考点）

> 以"学生成绩管理系统"为载体，覆盖 MySQL 期末核心考点（数据库/表创建、数据操作、查询、约束、索引、视图、存储过程/函数等），完成项目即可掌握 80% 以上期末高频考点。

---

## 目录

- [前期准备](#前期准备)
- [步骤 1：创建数据库](#步骤-1创建数据库)
- [步骤 2：创建数据表](#步骤-2创建数据表)
- [步骤 3：插入测试数据](#步骤-3插入测试数据)
- [步骤 4：数据查询](#步骤-4数据查询)
- [步骤 5：数据修改与删除](#步骤-5数据修改与删除)
- [步骤 6：进阶操作](#步骤-6进阶操作)
- [步骤 7：项目总结](#步骤-7项目总结)
- [期末真题适配](#期末真题适配)

---

## 前期准备

### 环境要求

| 项 | 说明 |
|----|------|
| MySQL | 5.7 / 8.0 均可（推荐 8.0） |
| 工具 | Navicat / SQLyog / MySQL 命令行 |
| 前提 | 掌握 DDL、DML、DQL 基础语法 |

### 考点梳理

| 分类 | 考点 |
|------|------|
| **DDL** | 数据库创建、表创建（含数据类型、约束）、表修改/删除 |
| **DML** | 插入、修改、删除数据（含批量操作、条件删除） |
| **DQL** | 基础查询、条件查询、排序、分组、聚合函数、联表查询、子查询 |
| **约束** | 主键、外键、非空、唯一、检查约束 |
| **进阶** | 索引、视图、存储过程、函数 |

---

## 步骤 1：创建数据库

> **考点：** DDL-数据库操作

```sql
-- 1. 创建数据库（指定字符集 utf8mb4，避免中文乱码）
CREATE DATABASE IF NOT EXISTS student_score_system
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_general_ci;

-- 2. 查看所有数据库（验证是否创建成功）
SHOW DATABASES;

-- 3. 使用该数据库
USE student_score_system;
```

| 考点 | 说明 |
|------|------|
| `IF NOT EXISTS` | 避免重复创建报错 |
| `DEFAULT CHARACTER SET` | 字符集设置，utf8mb4 兼容所有中文 |

---

## 步骤 2：创建数据表

> **考点：** DDL-表创建、约束、数据类型

设计 3 张核心表：
| 表名 | 说明 |
|------|------|
| `student` | 学生基本信息 |
| `course` | 课程信息 |
| `score` | 学生课程成绩（关联学生表和课程表，使用外键） |

```sql
-- 1. 创建学生表（student）
CREATE TABLE IF NOT EXISTS student (
    student_id INT PRIMARY KEY AUTO_INCREMENT,     -- 主键，自增
    student_name VARCHAR(50) NOT NULL,             -- 非空约束
    student_gender CHAR(2) CHECK (student_gender IN ('男','女')),  -- 检查约束
    student_age INT NOT NULL,
    student_class VARCHAR(50) NOT NULL,
    student_no VARCHAR(20) UNIQUE NOT NULL         -- 唯一约束（学号）
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 创建课程表（course）
CREATE TABLE IF NOT EXISTS course (
    course_id INT PRIMARY KEY AUTO_INCREMENT,
    course_name VARCHAR(50) NOT NULL UNIQUE,       -- 非空 + 唯一
    course_credit INT NOT NULL,
    course_teacher VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 创建成绩表（score）（核心：外键关联）
CREATE TABLE IF NOT EXISTS score (
    score_id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,                       -- 关联学生表
    course_id INT NOT NULL,                        -- 关联课程表
    score INT CHECK (score BETWEEN 0 AND 100),     -- 检查约束（成绩 0-100）
    exam_date DATE,
    FOREIGN KEY (student_id) REFERENCES student(student_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    FOREIGN KEY (course_id) REFERENCES course(course_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    UNIQUE (student_id, course_id)                 -- 一个学生一门课只能有一个成绩
);
```

| 约束类型 | 关键字 | 作用 |
|----------|--------|------|
| 主键 | `PRIMARY KEY` | 唯一标识，非空且唯一 |
| 外键 | `FOREIGN KEY` | 关联两表，级联操作 |
| 非空 | `NOT NULL` | 字段不能为空 |
| 唯一 | `UNIQUE` | 字段值不能重复 |
| 检查 | `CHECK` | 限制字段取值范围 |

---

## 步骤 3：插入测试数据

> **考点：** DML-插入数据（单条、批量插入）

```sql
-- 学生表
INSERT INTO student (student_name, student_gender, student_age, student_class, student_no)
VALUES
('张三', '男', 20, '计算机2班', '2024001'),
('李四', '女', 19, '计算机2班', '2024002'),
('王五', '男', 20, '计算机1班', '2024003'),
('赵六', '女', 19, '计算机1班', '2024004'),
('孙七', '男', 20, '计算机2班', '2024005'),
('周八', '女', 19, '计算机1班', '2024006');

-- 课程表
INSERT INTO course (course_name, course_credit, course_teacher)
VALUES
('MySQL数据库', 3, '张老师'),
('Java编程', 4, '李老师'),
('Python基础', 3, '王老师'),
('计算机网络', 3, '赵老师');

-- 成绩表
INSERT INTO score (student_id, course_id, score, exam_date)
VALUES
(1, 1, 88, '2024-12-10'), (1, 2, 92, '2024-12-11'),
(2, 1, 79, '2024-12-10'), (2, 3, 85, '2024-12-12'),
(3, 2, 68, '2024-12-11'), (3, 4, 90, '2024-12-13'),
(4, 1, 95, '2024-12-10'), (4, 3, 82, '2024-12-12'),
(5, 2, 75, '2024-12-11'), (5, 4, 88, '2024-12-13');
```

---

## 步骤 4：数据查询

> **考点：** DQL — 期末大题重中之重

### 4.1 基础查询

```sql
-- 指定字段查询
SELECT student_name, student_no, student_class FROM student;

-- 去重查询
SELECT DISTINCT course_name, course_credit FROM course;
```

### 4.2 条件查询（WHERE）

| 条件类型 | 运算符 | 示例 |
|----------|--------|------|
| 等于 | `=` | `WHERE student_class = '计算机2班'` |
| 多条件 | `AND` | `WHERE student_age > 19 AND student_gender = '男'` |
| 范围 | `BETWEEN...AND` | `WHERE score BETWEEN 80 AND 90` |
| 集合 | `IN` | `WHERE course_name IN ('MySQL数据库', 'Java编程')` |
| 模糊 | `LIKE` | `WHERE student_name LIKE '%张%'` |

### 4.3 排序查询（ORDER BY）

```sql
-- 升序
SELECT student_name, student_age FROM student ORDER BY student_age ASC;

-- 降序
SELECT * FROM score WHERE course_id = 1 ORDER BY score DESC;
```

### 4.4 聚合函数 + 分组查询（GROUP BY）

```sql
-- 统计每班人数
SELECT student_class, COUNT(student_id) AS 学生人数
FROM student GROUP BY student_class;

-- 每门课的平均、最高、最低成绩
SELECT course_id,
    AVG(score) AS 平均成绩,
    MAX(score) AS 最高成绩,
    MIN(score) AS 最低成绩
FROM score GROUP BY course_id;

-- HAVING 过滤分组（重点：HAVING vs WHERE）
SELECT course_id, COUNT(student_id) AS 及格人数
FROM score
WHERE score >= 60            -- WHERE 过滤行（先过滤，再分组）
GROUP BY course_id
HAVING 及格人数 > 2;         -- HAVING 过滤分组（分组后过滤）
```

| 对比 | WHERE | HAVING |
|------|-------|--------|
| 过滤对象 | 行数据（分组前） | 分组结果（分组后） |
| 聚合函数 | 不可用 | 可用 |
| 位置 | GROUP BY 之前 | GROUP BY 之后 |

### 4.5 联表查询（期末大题重中之重）

```sql
-- 内连接：只显示有成绩的记录
SELECT s.student_name, c.course_name, sc.score
FROM student s
INNER JOIN score sc ON s.student_id = sc.student_id
INNER JOIN course c ON sc.course_id = c.course_id;

-- 左连接：显示所有学生（无成绩也显示）
SELECT s.student_name, c.course_name, sc.score
FROM student s
LEFT JOIN score sc ON s.student_id = sc.student_id
LEFT JOIN course c ON sc.course_id = c.course_id;
```

| 连接类型 | 说明 |
|----------|------|
| `INNER JOIN` | 两表交集 |
| `LEFT JOIN` | 左表全量，右表匹配 |
| `RIGHT JOIN` | 右表全量，左表匹配 |

### 4.6 子查询

```sql
-- 查询最高成绩对应的学生
SELECT student_name FROM student
WHERE student_id = (
    SELECT student_id FROM score
    WHERE course_id = 1
    AND score = (SELECT MAX(score) FROM score WHERE course_id = 1)
);

-- 查询成绩大于平均值的记录
SELECT * FROM score WHERE score > (SELECT AVG(score) FROM score);

-- 查询选课 ≥2 门的学生
SELECT student_name FROM student
WHERE student_id IN (
    SELECT student_id FROM score
    GROUP BY student_id
    HAVING COUNT(course_id) >= 2
);
```

---

## 步骤 5：数据修改与删除

> **考点：** DML-UPDATE、DELETE

```sql
-- 修改数据（必须带 WHERE 条件）
UPDATE student SET student_age = 21 WHERE student_id = 1;
UPDATE student SET student_class = '计算机01班' WHERE student_class = '计算机1班';

-- 删除数据（必须带 WHERE 条件）
DELETE FROM score WHERE student_id = 5;
DELETE FROM course WHERE course_id = 3;  -- 触发外键级联删除

-- ⚠️ 禁止执行无条件删除
-- DELETE FROM 表名;  -- 会删除表中所有数据！
```

| 注意点 | 说明 |
|--------|------|
| 必须加 WHERE | 除非明确要操作所有数据 |
| 外键级联 | `ON DELETE CASCADE` 会自动删除从表关联数据 |
| 数据格式 | 修改值需符合表约束（如 CHECK） |

---

## 步骤 6：进阶操作

> **考点：** 索引、视图、存储过程/函数（占 10-15 分）

### 索引

```sql
-- 创建唯一索引
CREATE UNIQUE INDEX idx_student_no ON student(student_no);

-- 创建普通索引
CREATE INDEX idx_score ON score(score);

-- 查看索引
SHOW INDEX FROM student;

-- 删除索引
DROP INDEX idx_score ON score;
```

| 索引类型 | 作用 |
|----------|------|
| `UNIQUE INDEX` | 提高效率 + 保证数据唯一 |
| `INDEX` | 仅提高查询效率 |

### 视图

```sql
-- 创建视图：简化联表查询
CREATE VIEW v_student_score AS
SELECT s.student_name, c.course_name, sc.score
FROM student s
INNER JOIN score sc ON s.student_id = sc.student_id
INNER JOIN course c ON sc.course_id = c.course_id;

-- 查询视图（与查询表一样）
SELECT * FROM v_student_score;

-- 删除视图
DROP VIEW IF EXISTS v_student_score;
```

### 存储过程

```sql
DELIMITER //
CREATE PROCEDURE get_class_student_count(IN class_name VARCHAR(50), OUT count INT)
BEGIN
    SELECT COUNT(student_id) INTO count
    FROM student WHERE student_class = class_name;
END //
DELIMITER ;

-- 调用
CALL get_class_student_count('计算机2班', @count);
SELECT @count AS 计算机2班人数;
```

### 函数

```sql
DELIMITER //
CREATE FUNCTION get_score_grade(score INT) RETURNS VARCHAR(10)
DETERMINISTIC
BEGIN
    DECLARE grade VARCHAR(10);
    IF score < 60 THEN SET grade = '不及格';
    ELSEIF score <= 79 THEN SET grade = '及格';
    ELSE SET grade = '优秀';
    END IF;
    RETURN grade;
END //
DELIMITER ;

-- 调用
SELECT score, get_score_grade(score) AS 成绩等级 FROM score;
```

| 对比 | 存储过程 | 函数 |
|------|----------|------|
| 返回值 | 无（靠 OUT 参数） | 有返回值 |
| SQL 语句 | 可执行任意 SQL | 只能查询 |
| 参数类型 | IN / OUT / INOUT | 仅 IN |

---

## 步骤 7：项目总结

| 分类 | 掌握重点 | 分值占比 |
|------|----------|----------|
| **DDL** | 数据库/表创建，约束和数据类型 | 基础 |
| **DML** | 批量插入、条件修改/删除、外键级联 | 基础 |
| **DQL** | 联表查询、子查询、分组+聚合函数 | **最高** |
| **进阶** | 索引、视图、存储过程/函数的基本语法 | 10-15% |

---

## 期末真题适配

> 题目：查询每个班级的平均成绩（保留 2 位小数），按平均成绩降序排序，只显示平均成绩 ≥ 80 分的班级。

```sql
SELECT
    s.student_class,
    ROUND(AVG(sc.score), 2) AS 平均成绩
FROM student s
INNER JOIN score sc ON s.student_id = sc.student_id
GROUP BY s.student_class
HAVING 平均成绩 >= 80
ORDER BY 平均成绩 DESC;
```

> **涉及考点：** 联表查询（INNER JOIN）、分组（GROUP BY）、聚合函数（AVG、ROUND）、HAVING 过滤、排序（ORDER BY DESC）
