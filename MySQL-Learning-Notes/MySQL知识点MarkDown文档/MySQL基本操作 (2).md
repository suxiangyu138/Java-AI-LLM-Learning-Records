
 # MySQL基本操作


--------------------------------------------------------------------------------------------------------------------------------------
## 一、数据库操作

``` sql
1. 查看所有数据库
SHOW DATABASES;
2. 创建数据库
-- 基础创建
CREATE DATABASE IF NOT EXISTS mydb;
-- 指定字符集
CREATE DATABASE IF NOT EXISTS mydb 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;
3. 选择/切换数据库
USE mydb;
4. 删除数据库
DROP DATABASE IF EXISTS mydb;
```
--------------------------------------------------------------------------------------------------------------------------------------
## 二、表操作
``` sql
1. 查看当前数据库的所有表
SHOW TABLES;
2. 查看表结构
DESC user;
DESCRIBE user;
3. 创建表
CREATE TABLE IF NOT EXISTS user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    age TINYINT UNSIGNED DEFAULT 0,
    email VARCHAR(100) COMMENT '用户邮箱',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
4. 修改表
-- 添加字段
ALTER TABLE user ADD COLUMN phone VARCHAR(20) AFTER email;
-- 修改字段
ALTER TABLE user MODIFY COLUMN age INT UNSIGNED DEFAULT 18;
-- 删除字段
ALTER TABLE user DROP COLUMN phone;
-- 修改表名
ALTER TABLE user RENAME TO t_user;
5. 删除表
DROP TABLE IF EXISTS user;
```

--------------------------------------------------------------------------------------------------------------------------------------
## 三、数据操作（增删改）
    
``` sql
1. 插入数据
-- 单条插入（指定字段）
INSERT INTO user (username, age, email) 
VALUES ('zhangsan', 20, 'zhangsan@163.com');
-- 批量插入
INSERT INTO user (username, age, email) 
VALUES 
('lisi', 25, 'lisi@qq.com'),
('wangwu', 30, 'wangwu@126.com');
2. 更新数据
UPDATE user 
SET age = 22, email = 'zhangsan_new@163.com' 
WHERE id = 1;
3. 删除数据
-- 带条件删除
DELETE FROM user WHERE id = 2;
-- 清空表（方式1）
DELETE FROM user;
-- 清空表（方式2）
TRUNCATE TABLE user;
```

--------------------------------------------------------------------------------------------------------------------------------------
## 四、数据查询

``` sql
1. 查询指定字段
SELECT username, age, create_time FROM user;
2. 带条件查询
SELECT * FROM user WHERE age > 25 AND username LIKE 'z%';
3. 排序查询
SELECT * FROM user ORDER BY age DESC, create_time ASC;
4. 限制结果行数
SELECT * FROM user LIMIT 10;
SELECT * FROM user LIMIT 5, 10;
5. 去重查询
SELECT DISTINCT age FROM user;
6. 聚合查询
SELECT COUNT(*) AS total, MAX(age) AS max_age, AVG(age) AS avg_age FROM user;
```
--------------------------------------------------------------------------------------------------------------------------------------
## 总结
1. 执行DROP、UPDATE、DELETE操作时，需加IF EXISTS或WHERE条件避免误操作
2. 创建库/表指定utf8mb4字符集，解决中文乱码问题
3. 查询时避免SELECT *，优先指定字段，LIMIT用于分页，ORDER BY支持多字段排序

