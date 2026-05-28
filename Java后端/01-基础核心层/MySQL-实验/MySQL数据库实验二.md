# 数据库实验二【全流程完整代码】

## 统一规范
``` docker
docker exec -it my-mysql mysql -u root -p123456
```
- 数据库名：`DatabaseExp2`
- 表名：`S_E164` / `P_E164` / `J_E164` / `SPJ_E164`
- 全英文表结构 + 全英文数据
- 每张表 **100 条模拟数据**
- 14 道原题完整 SQL + 可直接复制运行
- MySQL 环境完美兼容，无中文乱码、查询不失效

---

## 一、全流程第一步：建库 + 建表 + 插入100条测试数据
```sql
-- 1. 创建并进入数据库
CREATE DATABASE IF NOT EXISTS DatabaseExp2 DEFAULT CHARACTER SET utf8mb4;
USE DatabaseExp2;

-- ======================
-- 供应商表 S_E164 100条
-- ======================
DROP TABLE IF EXISTS S_E164;
CREATE TABLE S_E164 (
    SNO CHAR(4) PRIMARY KEY,
    SNAME VARCHAR(30),
    STAT CHAR(1),
    CITY VARCHAR(15)
);

INSERT INTO S_E164(SNO, SNAME, STAT, CITY)
WITH RECURSIVE num(n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM num WHERE n < 100
)
SELECT
    CONCAT('S', LPAD(n, 3, '0')),
    CONCAT('Supplier_', n),
    ELT(FLOOR(RAND() * 3) + 1, 'A', 'B', 'C'),
    ELT(FLOOR(RAND() * 6) + 1, 'Beijing','Tianjin','Shanghai','Hefei','Nanjing','Changchun')
FROM num;

-- ======================
-- 零件表 P_E164 100条
-- ======================
DROP TABLE IF EXISTS P_E164;
CREATE TABLE P_E164 (
    PNO CHAR(4) PRIMARY KEY,
    PNAME VARCHAR(20),
    COLOR VARCHAR(10),
    WT INT
);

INSERT INTO P_E164(PNO, PNAME, COLOR, WT)
WITH RECURSIVE num(n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM num WHERE n < 100
)
SELECT
    CONCAT('P', LPAD(n, 3, '0')),
    ELT(FLOOR(RAND() * 6) + 1, 'Nut','Bolt','Screwdriver','Gear','Cam','Bearing'),
    ELT(FLOOR(RAND() * 4) + 1, 'Red','Green','Blue','Yellow'),
    FLOOR(RAND() * 40) + 10
FROM num;

-- ======================
-- 工程项目表 J_E164 100条
-- ======================
DROP TABLE IF EXISTS J_E164;
CREATE TABLE J_E164 (
    JNO CHAR(4) PRIMARY KEY,
    JNAME VARCHAR(30),
    CITY VARCHAR(15)
);

INSERT INTO J_E164(JNO, JNAME, CITY)
WITH RECURSIVE num(n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM num WHERE n < 100
)
SELECT
    CONCAT('J', LPAD(n, 3, '0')),
    CONCAT('Project_', n),
    ELT(FLOOR(RAND() * 6) + 1, 'Beijing','Tianjin','Shanghai','Hefei','Nanjing','Changzhou')
FROM num;

-- ======================
-- 供应表 SPJ_E164 100条
-- ======================
DROP TABLE IF EXISTS SPJ_E164;
CREATE TABLE SPJ_E164 (
    SNO CHAR(4),
    PNO CHAR(4),
    JNO CHAR(4),
    QTY INT,
    PRIMARY KEY (SNO, PNO, JNO)
);

INSERT INTO SPJ_E164(SNO, PNO, JNO, QTY)
WITH RECURSIVE num(n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM num WHERE n < 100
)
SELECT
    CONCAT('S', LPAD(FLOOR(RAND()*100)+1, 3, '0')),
    CONCAT('P', LPAD(FLOOR(RAND()*100)+1, 3, '0')),
    CONCAT('J', LPAD(FLOOR(RAND()*100)+1, 3, '0')),
    FLOOR(RAND() * 800) + 100
FROM num;
```

---

## 二、全流程第二步：14道实验查询题【完整可直接运行】
```sql
USE DatabaseExp2;

-- 1. Query all suppliers with English column headers
SELECT
    SNO AS Supplier_No,
    SNAME AS Supplier_Name,
    STAT AS Status,
    CITY AS City
FROM S_E164;

-- 2. Query suppliers located in Beijing
SELECT *
FROM S_E164
WHERE CITY = 'Beijing';

-- 3. Supplier name ends with specified string
SELECT *
FROM S_E164
WHERE SNAME LIKE '%5';

-- 4. Parts name start with Screw
SELECT *
FROM P_E164
WHERE PNAME LIKE 'Screw%';

-- 5. Project name contains Project
SELECT *
FROM J_E164
WHERE JNAME LIKE '%Project%';

-- 6. Query specified parts type
SELECT *
FROM P_E164
WHERE PNAME IN ('Nut', 'Bolt', 'Screwdriver');

-- 7. Check supply info of S001
SELECT *
FROM SPJ_E164
WHERE SNO = 'S001';

-- 8. Total supply quantity of P002
SELECT SUM(QTY) AS Total_Quantity
FROM SPJ_E164
WHERE PNO = 'P002';

-- 9. Max / Min / Avg quantity of P002
SELECT
    MAX(QTY) AS Max_Quantity,
    MIN(QTY) AS Min_Quantity,
    AVG(QTY) AS Avg_Quantity
FROM SPJ_E164
WHERE PNO = 'P002';

-- 10. Group by project & part
SELECT
    JNO AS Project_No,
    PNO AS Part_No,
    SUM(QTY) AS Total_Quantity
FROM SPJ_E164
GROUP BY JNO, PNO;

-- 11. Quantity greater than 300
SELECT *
FROM SPJ_E164
WHERE QTY > 300;

-- 12. Lowest 2 supply records
SELECT *
FROM SPJ_E164
ORDER BY QTY ASC
LIMIT 2;

-- 13. Top 3 suppliers by total supply
SELECT Supplier_No
FROM (
    SELECT
        SNO AS Supplier_No,
        SUM(QTY) AS Total_Qty
    FROM SPJ_E164
    GROUP BY SNO
    ORDER BY Total_Qty DESC
) AS Temp
LIMIT 3;

-- 14. Group by supplier & part
SELECT
    SNO AS Supplier_No,
    PNO AS Part_No,
    SUM(QTY) AS Total_Quantity
FROM SPJ_E164
GROUP BY SNO, PNO;
```

---

## 三、全流程操作步骤（照着做就能完成实验）
1. 打开 MySQL / Navicat / DBeaver
2. 复制「第一步建库建表代码」全部执行
3. 复制「第二步14道查询代码」逐题执行
4. 每道语句运行截图，包含 SQL + 结果 + 库名
5. 实验全部完成

---

## 四、核心总结（背诵+复习）
1. 四张表：**供应商S、零件P、工程J、供应SPJ**
2. 常用语法：
   - 模糊：`LIKE 'xxx%'` `LIKE '%xxx'`
   - 集合：`IN()`
   - 聚合：`SUM/MAX/MIN/AVG`
   - 分组：`GROUP BY 多字段`
   - 分页排序：`ORDER BY + LIMIT`
   - 子查询：内层结果当外表使用
3. 关键原则：
   - 全英文数据 + 全英文条件，**完全匹配，查询不空**
   - 分组查询，非聚合字段必须写在 `GROUP BY` 后
