# 01 - 数据库进阶：窗口函数与 SQL 执行顺序

> 定位：SQL 题的进阶体系——窗口函数全解、SQL 执行顺序、JOIN 与去重陷阱、NULL 三值逻辑、查询优化与索引

## 📚 目录

1. [窗口函数全解](#1-窗口函数全解)
2. [SQL 执行顺序](#2-sql-执行顺序)
3. [JOIN 的完整体系](#3-join-的完整体系)
4. [NULL 三值逻辑](#4-null-三值逻辑)
5. [去重与分组的陷阱](#5-去重与分组的陷阱)
6. [查询优化与索引](#6-查询优化与索引)
7. [面试要点与追问](#7-面试要点与追问)

---

## 1. 窗口函数全解

### 1.1 窗口函数 vs GROUP BY

```
GROUP BY：分组后每组合并成一行（失去明细）
窗口函数：分组后每行保留，额外算"组内值"（不丢明细）

⚠️ 面试必答：
"窗口函数 = 在'窗口'（分区）内计算，结果附加到
 每行——与 GROUP BY 的区别是保留明细行。"
```

### 1.2 窗口函数分类

| 类别 | 函数 | 用途 |
|------|------|------|
| 排名 | ROW_NUMBER / RANK / DENSE_RANK | 组内排名 |
| 聚合 | SUM/AVG/COUNT OVER | 组内累计/均值 |
| 位移 | LAG / LEAD | 前一行/后一行 |
| 首尾 | FIRST_VALUE / LAST_VALUE | 组内首尾 |
| 分位 | NTILE(n) | 分成 n 桶 |

```
⚠️ ROW_NUMBER vs RANK vs DENSE_RANK：
  ROW_NUMBER：1,2,3,4（并列也占位）
  RANK：1,1,3,4（并列跳号）
  DENSE_RANK：1,1,2,3（并列不跳号）

⚠️ 面试必答：
"三种排名：ROW_NUMBER 不并列、RANK 并列跳号、
 DENSE_RANK 并列不跳号——'第 N 名'题
 用 DENSE_RANK（185 部门前三）。"
```

### 1.3 窗口函数语法

```sql
-- 语法：函数() OVER (PARTITION BY 分区 ORDER BY 排序 [ROWS 范围])
SELECT employee_id,
       salary,
       ROW_NUMBER() OVER (PARTITION BY department_id ORDER BY salary DESC) AS rn,
       SUM(salary) OVER (PARTITION BY department_id ORDER BY salary
                         ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cum
FROM employees;
-- ⚠️ ORDER BY 决定窗口内顺序；ROWS 控制滑动范围（累计/滑动）
-- ⚠️ 不加 PARTITION BY = 全表一个窗口
```

---

## 2. SQL 执行顺序

### 2.1 逻辑执行顺序（必背）

```
FROM → JOIN → WHERE → GROUP BY → HAVING → SELECT → DISTINCT → ORDER BY → LIMIT

⚠️ 关键推论：
  ① WHERE 不能用别名（SELECT 后执行）
  ② HAVING 可以用聚合函数（GROUP BY 后）
  ③ ORDER BY 可以用别名（SELECT 后执行）
  ④ LIMIT 最后（结果已定）

⚠️ 面试必答：
"SQL 逻辑顺序 FROM→WHERE→GROUP→HAVING→SELECT→ORDER。
 WHERE 过滤原始行、HAVING 过滤分组——'
 先过滤再分组'是性能关键。"
```

### 2.2 执行顺序的实战应用

```sql
-- ① WHERE 先过滤 → 聚合量小（性能）
SELECT department_id, COUNT(*)
FROM employees
WHERE hire_date > '2020-01-01'        -- 先过滤（不能放 HAVING）
GROUP BY department_id
HAVING COUNT(*) > 5;                  -- 分组后过滤

-- ② 别名陷阱
SELECT salary * 1.1 AS new_salary
FROM employees
WHERE new_salary > 5000;              -- ❌ WHERE 先执行，别名不存在
-- 正确：WHERE salary * 1.1 > 5000 或子查询

-- ③ 分组列选择陷阱
SELECT department_id, employee_id     -- ⚠️ employee_id 不在 GROUP BY → 非法
FROM employees GROUP BY department_id; --（MySQL 宽松模式除外）
```

---

## 3. JOIN 的完整体系

### 3.1 七种 JOIN

```
INNER JOIN：两表都有（交集）
LEFT JOIN：左表全保留 + 右表匹配（无匹配 NULL）
RIGHT JOIN：右表全保留
FULL OUTER JOIN：两表全保留（MySQL 用 UNION 模拟）
CROSS JOIN：笛卡尔积
SELF JOIN：自连接（同一张表两别名）
NATURAL JOIN：同名列自动等值（少用）

⚠️ 面试必答：
"LEFT JOIN 是'保留主表 + 补匹配'——
 无匹配行右表列是 NULL。
 自连接是'同一表扮演两个角色'（180 连续数字）。"
```

### 3.2 JOIN 的过滤位置（易错）

```sql
-- ⚠️ ON vs WHERE 的过滤差异：
-- LEFT JOIN 中 WHERE 右表条件会"变回 INNER"！
SELECT *
FROM A LEFT JOIN B ON A.id = B.id
WHERE B.x IS NOT NULL;              -- 过滤掉 NULL → 等效 INNER JOIN

-- 正确：条件放 ON（保留左表全量）
SELECT *
FROM A LEFT JOIN B ON A.id = B.id AND B.x > 0;   -- 不匹配仍保留 A

-- ⚠️ 面试必答：
"'LEFT JOIN + WHERE 右表条件'会退化——过滤条件
 放 ON（JOIN 时）与 WHERE（结果后）语义不同。"
```

---

## 4. NULL 三值逻辑

### 4.1 NULL 的比较

```
NULL 的三种逻辑值：TRUE / FALSE / UNKNOWN
  任何比较（=、>、<）与 NULL 结果都是 UNKNOWN
  → WHERE 不满足 UNKNOWN → 行被过滤！

⚠️ 常见陷阱：
  WHERE x = NULL  ❌（永远 UNKNOWN → 空结果）
  正确：WHERE x IS NULL
  WHERE x <> NULL ❌（同样 UNKNOWN）
  正确：WHERE x IS NOT NULL

⚠️ 面试必答：
"NULL 与任何值比较都是 UNKNOWN（不是 FALSE）——
 判断用 IS NULL / IS NOT NULL，
 任何 =/<> 与 NULL 比较都会过滤掉该行。"
```

### 4.2 NULL 与聚合/排序

```
聚合函数忽略 NULL：
  COUNT(*) 计行数；COUNT(col) 不计 NULL
  SUM/AVG 忽略 NULL（AVG 分母不含 NULL）

排序中 NULL 的位置：
  MySQL：NULL 最小（ASC 排最前）
  Oracle：NULL 最大
  → 跨库行为不一致（可 ORDER BY col IS NULL 控制）

⚠️ 面试必答：
"COUNT(*) 与 COUNT(col) 的区别就是 NULL——
 统计'非空值个数'用 COUNT(col)。
 176 第二高薪水（可能为 NULL）是经典。"
```

---

## 5. 去重与分组的陷阱

### 5.1 去重三兄弟

| 方式 | 语义 | 场景 |
|------|------|------|
| DISTINCT | 结果去重 | 简单查重 |
| GROUP BY | 分组聚合 | 需要计数/聚合 |
| ROW_NUMBER | 组内取一条 | 每组 Top1 |

```
⚠️ 面试表达：
"去重三选一：只查重 DISTINCT；
 要聚合 GROUP BY；要'每组一行/第 N 名'
 窗口函数（ROW_NUMBER 取 rn=1）。"
```

### 5.2 删除重复（196 的思路）

```sql
-- 196. 删除重复邮箱（保留 id 最小者）
DELETE p1 FROM Person p1, Person p2
WHERE p1.email = p2.email AND p1.id > p2.id;
-- ⚠️ 自连接 + 保留最小 id
-- 或窗口函数版：
DELETE FROM Person
WHERE id NOT IN (
    SELECT id FROM (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY email ORDER BY id) rn
        FROM Person) t
    WHERE rn = 1);
-- ⚠️ 注意：MySQL 不能直接 DELETE 子查询同表 → 多包一层
```

---

## 6. 查询优化与索引

### 6.1 性能优化的核心

```
① 索引：WHERE/ORDER BY/JOIN 列建索引
② 避免 SELECT *：只取需要的列
③ 避免函数包列：WHERE UPPER(name) = 'X' 无法用索引
④ 小表驱动大表：JOIN 顺序（优化器一般自动）
⑤ EXPLAIN 分析：type（ALL=全表扫描应避免）

⚠️ 面试必答：
"SQL 优化第一原则：让过滤走索引——
 函数包裹列、隐式类型转换都会破坏索引。"
```

### 6.2 索引失效场景

```
① 函数/运算包裹列：WHERE salary * 1.1 > 100（失效）
② 隐式类型转换：varchar 列与数字比较（失效）
③ 前导模糊：LIKE '%abc'（失效，'abc%' 可用）
④ OR 条件有一侧无索引（失效）
⑤ 索引列 IS NOT NULL 有时失效

⚠️ 面试表达：
"索引失效五场景：函数包裹、类型转换、
 前导模糊、OR 混合、NULL 判断——
 写 SQL 时保持列'裸用'。"
```

---

## 7. 面试要点与追问

### 7.1 面试话术模板

```
"SQL 题先想执行顺序（FROM→WHERE→GROUP→HAVING→SELECT→ORDER），
 再选工具：排名/组内 → 窗口函数（RANK 家族）；
 分组统计 → GROUP BY + HAVING；
 保留主表 → LEFT JOIN（条件放 ON）。
 NULL 用 IS NULL 判断。性能：索引 + 避免函数包裹列。"
```

### 7.2 高频追问 TOP 8

| # | 追问 | 标准回答 |
|:---:|------|------|
| 1 | 窗口函数 vs GROUP BY？ | 窗口保留明细，GROUP 合并 |
| 2 | 三种排名区别？ | ROW_NUMBER/RANK/DENSE_RANK |
| 3 | 执行顺序？ | FROM→WHERE→GROUP→HAVING→SELECT→ORDER |
| 4 | ON vs WHERE？ | LEFT JOIN 中 WHERE 右表条件会退化 |
| 5 | NULL 比较？ | 三值逻辑，IS NULL 判断 |
| 6 | COUNT(*) vs COUNT(col)？ | 后者忽略 NULL |
| 7 | 去重方式？ | DISTINCT/GROUP BY/ROW_NUMBER |
| 8 | 索引失效？ | 函数包裹/类型转换/前导模糊 |

### 7.3 常见错误

| 错误 | 后果 | 修正 |
|------|------|------|
| WHERE 用别名 | 报错 | 子查询/重复表达式 |
| LEFT JOIN + WHERE 右表条件 | 退化 INNER | 条件放 ON |
| x = NULL | 空结果 | IS NULL |
| COUNT(col) 当行数 | 少计 | 需含 NULL 用 COUNT(*) |
| 窗口函数忘记 ORDER BY | 顺序随机 | 明确排序 |

---

> 🎯 **核心要点**：SQL 进阶 = **窗口函数**（排名/聚合/位移）+ **执行顺序**（WHERE 先于 SELECT）+ **JOIN 语义**（ON vs WHERE）+ **NULL 三值** + **优化**（索引裸用）。SQL 题"先画执行顺序，再选工具"是标准姿势。

---

**返回总览**：[00-算法汇总知识体系总览](../../刷题实战与综合/算法汇总/00-算法汇总知识体系总览.md) | **上一篇**：[00-数据库专题精要](00-数据库专题精要.md) | **下一篇**：[02-数据库高频题解](02-数据库高频题解.md)
