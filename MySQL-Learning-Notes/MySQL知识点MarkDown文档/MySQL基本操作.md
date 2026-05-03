# MySQL 基础完整操作语句

## 一、数据库操作
```mysql
-- 创建数据库
CREATE DATABASE test_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 使用数据库
USE test_db;

-- 删除数据库
DROP DATABASE IF EXISTS test_db;
```

## 二、数据表操作

```mysql
-- 创建学生表
CREATE TABLE student (
id INT PRIMARY KEY AUTO_INCREMENT,
name VARCHAR(30) NOT NULL,
gender CHAR(2),
age INT,
address VARCHAR(100),
create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 查看表结构
DESC student;

-- 修改表-新增字段
ALTER TABLE student ADD COLUMN phone VARCHAR(11);

-- 修改表-修改字段类型
ALTER TABLE student MODIFY COLUMN age TINYINT;

-- 修改表-删除字段
ALTER TABLE student DROP COLUMN phone;

-- 删除数据表
DROP TABLE IF EXISTS student;
```


## 三、新增数据 INSERT
```mysql
-- 单条插入
INSERT INTO student(name,gender,age,address)
VALUES ('张三','男',20,'合肥市');

-- 批量插入
INSERT INTO student(name,gender,age,address)
VALUES
('李四','女',19,'阜阳市'),
('王五','男',21,'芜湖市');
```

## 四、查询数据 SELECT

```mysql
-- 全表查询
SELECT * FROM student;

-- 指定字段查询
SELECT id,name,age FROM student;

-- 条件查询
SELECT * FROM student WHERE age >= 20;

-- 模糊查询
SELECT * FROM student WHERE name LIKE '张%';

-- 排序
SELECT * FROM student ORDER BY age DESC;

-- 分页
SELECT * FROM student LIMIT 0,2;

-- 聚合函数
SELECT COUNT(*) AS 总人数,AVG(age) AS 平均年龄 FROM student;

```
## 五、修改数据 UPDATE
```mysql
-- 带条件更新（务必加where）
UPDATE student SET age=22,address='蚌埠市' WHERE id=3;
```


## 六、删除数据 DELETE

```mysql
-- 条件删除
DELETE FROM student WHERE id=2;

-- 清空表数据（自增清零）
TRUNCATE TABLE student;

```

## 七、常用约束
```mysql
-- 主键约束：PRIMARY KEY
-- 自增：AUTO_INCREMENT
-- 非空约束：NOT NULL
-- 唯一约束：UNIQUE
-- 默认值：DEFAULT
-- 外键约束：FOREIGN KEY
```


