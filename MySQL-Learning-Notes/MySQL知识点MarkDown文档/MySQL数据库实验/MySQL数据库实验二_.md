# 一、创建数据库 + 建表 + 插入数据（必执行）
```sql
-- 创建数据库（修改为：数据库实验二_MySQL）
CREATE DATABASE IF NOT EXISTS 数据库实验二_MySQL;
USE 数据库实验二_MySQL;

-- 1. 供应商表 S_E164
CREATE TABLE S_E164 (
    SNO CHAR(4) PRIMARY KEY,
    SNAME VARCHAR(20),
    STAT CHAR(1),
    CITY VARCHAR(10)
);

INSERT INTO S_E164 VALUES 
('S001', '天津安贝儿', 'B', '天津'),
('S002', '北京启明星', 'A', '北京'),
('S003', '北京新天地', 'C', '北京'),
('S004', '天津丰泰盛', 'B', '天津'),
('S005', '上海普丰', 'C', '上海'),
('S006', '合肥四达', 'B', '合肥');

-- 2. 零件表 P_E164
CREATE TABLE P_E164 (
    PNO CHAR(4) PRIMARY KEY,
    PNAME VARCHAR(10),
    COLOR VARCHAR(4),
    WT INT
);

INSERT INTO P_E164 VALUES 
('P001', '螺母', '红', 12),
('P002', '螺栓', '绿', 17),
('P003', '螺丝刀', '蓝', 14),
('P004', '螺丝刀', '红', 14),
('P005', '凸轮', '蓝', 40),
('P006', '齿轮', '红', 30);

-- 3. 工程项目表 J_E164
CREATE TABLE J_E164 (
    JNO CHAR(4) PRIMARY KEY,
    JNAME VARCHAR(20),
    CITY VARCHAR(20)
);

INSERT INTO J_E164 VALUES 
('J001', '北京三建', '北京'),
('J002', '长春一汽', '长春'),
('J003', '新安弹簧厂', '天津'),
('J004', '临江造船厂', '天津'),
('J005', '唐山机车厂', '唐山'),
('J006', '新新无线电厂', '常州'),
('J007', '铭泰半导体厂', '南京');

-- 4. 供应表 SPJ_E164
CREATE TABLE SPJ_E164 (
    SNO CHAR(4),
    PNO CHAR(4),
    JNO CHAR(4),
    QTY INT,
    PRIMARY KEY (SNO, PNO, JNO)
);

INSERT INTO SPJ_E164 VALUES 
('S001', 'P001', 'J001', 200),
('S001', 'P001', 'J003', 100),
('S001', 'P001', 'J004', 700),
('S001', 'P002', 'J002', 100),
('S002', 'P003', 'J001', 400),
('S002', 'P003', 'J002', 200),
('S002', 'P003', 'J004', 500),
('S002', 'P003', 'J005', 400),
('S002', 'P005', 'J001', 400),
('S002', 'P005', 'J002', 100),
('S003', 'P001', 'J001', 200),
('S003', 'P003', 'J001', 200),
('S004', 'P005', 'J001', 100),
('S004', 'P006', 'J003', 300),
('S004', 'P006', 'J004', 200),
('S005', 'P002', 'J004', 100),
('S005', 'P003', 'J001', 200),
('S005', 'P006', 'J002', 200),
('S005', 'P006', 'J004', 500);
```

---

# 二、14道查询题目（直接执行交作业）
```sql
-- 使用数据库
USE 数据库实验二_MySQL;

-- 1. 查询所有供应商的信息，用中文表头显示
SELECT 
    SNO AS "供应商号",
    SNAME AS "供应商名称",
    STAT AS "状态",
    CITY AS "所在城市"
FROM S_E164;

-- 2. 查询位于“北京”的名称包含“星”的供应商信息
SELECT * FROM S_E164 WHERE CITY = '北京' AND SNAME LIKE '%星%';

-- 3. 查询供应商名中最后一个字是“丰”的供应商信息
SELECT * FROM S_E164 WHERE SNAME LIKE '%丰';

-- 4. 查询零件名以“螺丝”开头的零件信息
SELECT * FROM P_E164 WHERE PNAME LIKE '螺丝%';

-- 5. 查询名称含有“车”的工程项目信息
SELECT * FROM J_E164 WHERE JNAME LIKE '%车%';

-- 6. 查询名称为“螺母”、“螺栓”、“螺丝刀”的零件信息
SELECT * FROM P_E164 WHERE PNAME IN ('螺母','螺栓','螺丝刀');

-- 7. 查询“S001”号供应商的供应情况
SELECT * FROM SPJ_E164 WHERE SNO = 'S001';

-- 8. 查询“P002”号零件的总供应量
SELECT SUM(QTY) AS "P002零件总供应量" FROM SPJ_E164 WHERE PNO = 'P002';

-- 9. 查询“P002”号零件供应量的最大、最小和平均值
SELECT 
    MAX(QTY) AS "最大供应量",
    MIN(QTY) AS "最小供应量",
    AVG(QTY) AS "平均供应量"
FROM SPJ_E164 WHERE PNO = 'P002';

-- 10. 分组计算每个工程项目使用每种零件的供应量
SELECT 
    JNO AS "工程号",
    PNO AS "零件号",
    SUM(QTY) AS "总供应量"
FROM SPJ_E164 GROUP BY JNO, PNO;

-- 11. 查询供应量在300以上的供应信息
SELECT * FROM SPJ_E164 WHERE QTY > 300;

-- 12. 查询供应量最低的两个供应信息
SELECT * FROM SPJ_E164 ORDER BY QTY ASC LIMIT 2;

-- 13. 查询供应量前三名的供应商的编号
SELECT SNO AS "供应商号"
FROM (
    SELECT SNO, SUM(QTY) AS total_qty
    FROM SPJ_E164
    GROUP BY SNO
    ORDER BY total_qty DESC
) AS t LIMIT 3;

-- 14. 分组统计每个供应商供应每种零件的供应量
SELECT 
    SNO AS "供应商号",
    PNO AS "零件号",
    SUM(QTY) AS "总供应量"
FROM SPJ_E164 GROUP BY SNO, PNO;
```

---
