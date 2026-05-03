03.30 10:00
常用的SQL语句
一、查询语句（DQL）
1. 查询所有字段
SELECT * FROM 表名;
2. 查询指定字段
SELECT 字段1, 字段2 FROM 表名;
3. 条件查询
SELECT * FROM 表名 WHERE 条件;
4. 模糊查询（LIKE）
SELECT * FROM 表名 WHERE 字段 LIKE '%关键词%';
5. 范围查询（IN/BETWEEN）
SELECT * FROM 表名 WHERE 字段 IN (值1, 值2);
SELECT * FROM 表名 WHERE 字段 BETWEEN 起始值 AND 结束值;
6. 排序（ORDER BY）
SELECT * FROM 表名 ORDER BY 字段 ASC/DESC;
7. 分页（LIMIT）
SELECT * FROM 表名 LIMIT 起始位置, 条数;
8. 去重（DISTINCT）
SELECT DISTINCT 字段 FROM 表名;
9. 聚合查询
SELECT COUNT(*) FROM 表名;
SELECT SUM(字段) FROM 表名;
SELECT AVG(字段) FROM 表名;
SELECT MAX(字段) FROM 表名;
SELECT MIN(字段) FROM 表名;
10. 分组查询（GROUP BY）
SELECT 字段, COUNT(*) FROM 表名 GROUP BY 字段;
11. 分组后筛选（HAVING）
SELECT 字段, COUNT() FROM 表名 GROUP BY 字段 HAVING COUNT() > 数值;
12. 多表连接（JOIN）
SELECT * FROM 表1 JOIN 表2 ON 表1.关联字段 = 表2.关联字段;
SELECT * FROM 表1 LEFT JOIN 表2 ON 表1.关联字段 = 表2.关联字段;
二、增删改语句（DML）
1. 插入数据
INSERT INTO 表名 (字段1, 字段2) VALUES (值1, 值2);
2. 批量插入
INSERT INTO 表名 (字段1, 字段2) VALUES (值1, 值2), (值3, 值4);
3. 更新数据
UPDATE 表名 SET 字段1=值1, 字段2=值2 WHERE 条件;
4. 删除数据
DELETE FROM 表名 WHERE 条件;
三、表结构操作（DDL）
1. 创建表
CREATE TABLE 表名 (
字段1 类型 约束,
字段2 类型 约束
);
2. 删除表
DROP TABLE 表名;
3. 修改表名
ALTER TABLE 旧表名 RENAME TO 新表名;
4. 添加字段
ALTER TABLE 表名 ADD 字段 类型;
5. 修改字段
ALTER TABLE 表名 MODIFY 字段 新类型;
6. 删除字段
ALTER TABLE 表名 DROP 字段;
四、约束相关
1. 主键约束
ALTER TABLE 表名 ADD PRIMARY KEY (字段);
2. 唯一约束
ALTER TABLE 表名 ADD UNIQUE (字段);
3. 非空约束
ALTER TABLE 表名 MODIFY 字段 类型 NOT NULL;
4. 外键约束
ALTER TABLE 表名 ADD FOREIGN KEY (字段) REFERENCES 主表(主键);
五、索引操作
1. 创建索引
CREATE INDEX 索引名 ON 表名(字段);
2. 删除索引
DROP INDEX 索引名 ON 表名;
六、常用函数
1. 字符串函数
CONCAT(字段1, 字段2)
SUBSTRING(字段, 起始位置, 长度)
LENGTH(字段)
2. 日期函数
NOW()
CURDATE()
DATE_FORMAT(字段, '%Y-%m-%d')
3. 条件函数
IF(条件, 真值, 假值)
CASE WHEN 条件 THEN 结果 END

