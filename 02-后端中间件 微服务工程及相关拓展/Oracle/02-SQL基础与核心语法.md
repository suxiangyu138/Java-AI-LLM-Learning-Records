# 02 - SQL 基础与核心语法（Oracle vs MySQL）

> 🎯 Oracle SQL 和 MySQL 有 30% 的语法差异——分页、字符串拼接、日期函数、自增主键都不同。本章用对比方式快速掌握 Oracle SQL 的核心语法

---

## 目录

1. [基础 DML 差异](#1-基础-dml-差异)
2. [函数与表达式](#2-函数与表达式)
3. [分页与分层查询](#3-分页与分层查询)

---

## 1. 基础 DML 差异

| 操作 | MySQL | Oracle |
|------|-------|--------|
| 字符串拼接 | `CONCAT(a, b)` | `a \|\| b` |
| 自增主键 | `AUTO_INCREMENT` | `SEQUENCE` + 触发器 |
| 日期当前值 | `NOW()` | `SYSDATE` |
| 字符串截取 | `SUBSTRING(s, 1, 5)` | `SUBSTR(s, 1, 5)` |
| 空值处理 | `IFNULL(col, 0)` | `NVL(col, 0)` |
| 判断分支 | `IF(cond, a, b)` | `DECODE` / `CASE WHEN` |
| 限制行数 | `LIMIT 10` | `FETCH FIRST 10 ROWS ONLY` |

```sql
-- 自增主键：序列 + 触发器
CREATE SEQUENCE seq_users START WITH 1 INCREMENT BY 1;

-- FETCH 分页（Oracle 12c+）
SELECT * FROM users ORDER BY id OFFSET 10 ROWS FETCH NEXT 10 ROWS ONLY;

-- ROWNUM 分页（旧版 Oracle）
SELECT * FROM (
    SELECT a.*, ROWNUM rn FROM (
        SELECT * FROM users ORDER BY id
    ) a WHERE ROWNUM <= 20
) WHERE rn > 10;
```

## 2. 函数与表达式

```sql
-- 字符串
SELECT UPPER(name), LOWER(email), INITCAP(name),  -- 大小写
       LENGTH(name), SUBSTR(name, 1, 3),          -- 长度/截取
       INSTR(name, '张'), REPLACE(name, ' ', '')   -- 查找/替换
FROM users;

-- 日期
SELECT SYSDATE,                                           -- 当前时间
       ADD_MONTHS(SYSDATE, 3),                            -- 3个月后
       MONTHS_BETWEEN(date1, date2),                      -- 月份差
       TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI:SS'),         -- 格式化
       TO_DATE('2026-07-28', 'YYYY-MM-DD')                -- 字符串转日期
FROM dual;  -- Oracle 特有的虚拟表（MySQL 可以省略 FROM）

-- NULL 处理
SELECT NVL(email, 'N/A'),                                 -- Oracle 版 IFNULL
       NVL2(email, '有邮箱', '无邮箱'),                     -- 非空=A, 空=B
       COALESCE(email, phone, '无联系方式')                -- 返回第一个非空
FROM users;

-- 条件分支
SELECT CASE WHEN score >= 90 THEN 'A'
            WHEN score >= 80 THEN 'B'
            ELSE 'C' END AS grade
FROM results;
```

## 3. 分页与分层查询

```sql
-- CONNECT BY 树形查询（部门/组织架构）— Oracle 特有！
SELECT LEVEL, LPAD(' ', LEVEL*2) || name AS org_tree
FROM departments
START WITH parent_id IS NULL           -- 根节点
CONNECT BY PRIOR id = parent_id        -- 父子关系
ORDER SIBLINGS BY name;                -- 兄弟节点排序

-- WITH 递归 CTE（Oracle 11gR2+）
WITH dept_tree(id, name, parent_id, lvl) AS (
    SELECT id, name, parent_id, 1 FROM departments WHERE parent_id IS NULL
    UNION ALL
    SELECT d.id, d.name, d.parent_id, t.lvl + 1
    FROM departments d JOIN dept_tree t ON d.parent_id = t.id
)
SELECT * FROM dept_tree;
```

## 核心要点回顾

- 字符串拼接用 `||`（不是 CONCAT）
- 分页用 `OFFSET ... FETCH`（12c+）或 `ROWNUM` 嵌套
- `NVL` = MySQL 的 `IFNULL`，`DECODE` = MySQL 的 `IF`
- 树形查询用 `CONNECT BY PRIOR`（Oracle 最独特的语法）
- `FROM dual` — Oracle 特有的虚拟表

## 参考资料

1. Oracle SQL Language Reference
