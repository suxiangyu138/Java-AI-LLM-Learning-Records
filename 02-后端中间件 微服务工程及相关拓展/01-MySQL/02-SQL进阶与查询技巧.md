# 02 - SQL 进阶与查询技巧
> 定位：JOIN 家族、子查询、分组聚合、窗口函数、CTE 与递归 CTE——MySQL 8.0 查询能力的完整解析

## 1. SQL 基础与进阶

### 1.1 多表连接

多表连接是关系型数据库最核心的操作之一。MySQL 支持以下几种 JOIN 类型，理解它们的语义是写出正确 SQL 的前提。

#### INNER JOIN（内连接）

内连接返回两个表的交集部分，即只返回两个表中满足连接条件的行。

```sql
-- 查询每位员工及其所属部门（只显示有部门的员工）
SELECT e.emp_name, d.dept_name
FROM employee e
INNER JOIN department d ON e.dept_id = d.dept_id;
```

逻辑等价于用 WHERE 做隐式连接：

```sql
SELECT e.emp_name, d.dept_name
FROM employee e, department d
WHERE e.dept_id = d.dept_id;
```

#### LEFT JOIN（左外连接）

左表全部保留，右表只返回匹配的行，不匹配的地方补 NULL。

```sql
-- 查询所有员工及其部门（没有部门的员工也要显示）
SELECT e.emp_name, d.dept_name
FROM employee e
LEFT JOIN department d ON e.dept_id = d.dept_id;
```

一个高频面试考点：**用 LEFT JOIN 查「在左表但不在右表」的数据**。

```sql
-- 查询没有部门的员工
SELECT e.emp_name
FROM employee e
LEFT JOIN department d ON e.dept_id = d.dept_id
WHERE d.dept_id IS NULL;
```

#### RIGHT JOIN（右外连接）

与 LEFT JOIN 对称，右表全保留，左表匹配。

```sql
SELECT e.emp_name, d.dept_name
FROM employee e
RIGHT JOIN department d ON e.dept_id = d.dept_id;
```

实践中建议统一使用 LEFT JOIN，避免 LEFT/RIGHT 混用增加理解成本。

#### FULL JOIN（全外连接）

MySQL 不直接支持 FULL OUTER JOIN，需要用 UNION 模拟：

```sql
-- 查询所有员工和所有部门（无论是否匹配）
SELECT e.emp_name, d.dept_name
FROM employee e
LEFT JOIN department d ON e.dept_id = d.dept_id
UNION
SELECT e.emp_name, d.dept_name
FROM employee e
RIGHT JOIN department d ON e.dept_id = d.dept_id;
```

UNION 自带去重，如果需要保留重复行可使用 UNION ALL。

#### CROSS JOIN（交叉连接 / 笛卡尔积）

两表所有行组合，结果行数 = 左表行数 × 右表行数。通常需要配合 WHERE 条件使用，否则数据量会爆炸。

```sql
-- 所有员工与所有部门的组合
SELECT e.emp_name, d.dept_name
FROM employee e
CROSS JOIN department d;

-- 等价写法
SELECT e.emp_name, d.dept_name
FROM employee e, department d;
```

**实践建议**：在线上环境务必警惕 CROSS JOIN，一个 10 万行的表和 1 万行的表做笛卡尔积会产生 10 亿条中间结果，直接打爆内存。

---

### 1.2 子查询

子查询是嵌套在另一个 SQL 语句内部的查询，根据返回结果的不同分为三类。

#### 标量子查询（返回单个值）

```sql
-- 查询工资高于平均值的员工
SELECT emp_name, salary
FROM employee
WHERE salary > (SELECT AVG(salary) FROM employee);
```

标量子查询可以出现在 SELECT、WHERE、HAVING 子句中。

#### 行子查询（返回一行多列）

```sql
-- 查询与 '张三' 部门和职位都相同的员工
SELECT emp_name, dept_id, job_title
FROM employee
WHERE (dept_id, job_title) = (
    SELECT dept_id, job_title FROM employee WHERE emp_name = '张三'
);
```

#### 列子查询（返回一列多行）

```sql
-- 查询在销售部的所有员工
SELECT emp_name
FROM employee
WHERE dept_id IN (
    SELECT dept_id FROM department WHERE dept_name = '销售部'
);
```

#### EXISTS vs IN

这是一个面试常考点。EXISTS 和 IN 在语义上都可以判断存在性，但执行逻辑不同。

```sql
-- IN 子查询：先执行子查询，结果集物化后去重，外层逐行匹配
SELECT * FROM employee
WHERE dept_id IN (SELECT dept_id FROM department);

-- EXISTS 子查询：外层逐行遍历，对每一行执行子查询，找到第一个匹配即停止
SELECT * FROM employee e
WHERE EXISTS (
    SELECT 1 FROM department d WHERE d.dept_id = e.dept_id
);
```

- **IN 适合子查询结果集小、外层表大的场景**：因为子查询结果可以缓存。
- **EXISTS 适合子查询结果集大、外层表小的场景**：因为它是外层驱动内层，可以利用内层索引。
- **NOT IN vs NOT EXISTS**：NOT IN 在有 NULL 值时结果集为空（因为 `NULL IN (...) ` 返回 UNKNOWN），而 NOT EXISTS 不受 NULL 影响。因此能用 NOT EXISTS 就不要用 NOT IN。

#### 相关子查询 vs 非相关子查询

- **非相关子查询**：可以独立执行，不依赖外层查询。执行一次即可，性能通常更好。
- **相关子查询**：依赖外层查询的列，外层每一行都要执行一次子查询。性能较差，但表达能力更强。

```sql
-- 相关子查询：查询每个部门中工资最高的员工
SELECT e1.emp_name, e1.salary, e1.dept_id
FROM employee e1
WHERE e1.salary = (
    SELECT MAX(e2.salary) FROM employee e2 WHERE e2.dept_id = e1.dept_id
);
```

---

### 1.3 分组聚合

#### GROUP BY 与聚合函数

GROUP BY 将数据按指定列分组，配合聚合函数（COUNT、SUM、AVG、MAX、MIN）对每组进行计算。

```sql
-- 统计每个部门的员工数和平均工资
SELECT dept_id,
       COUNT(*) AS emp_count,
       AVG(salary) AS avg_salary
FROM employee
GROUP BY dept_id;
```

#### HAVING vs WHERE

这是最容易被混淆的概念。两者的核心区别在于**执行时机**：

- **WHERE** 在 GROUP BY **之前**执行，筛选的是**原始行数据**，不能使用聚合函数。
- **HAVING** 在 GROUP BY **之后**执行，筛选的是**分组后的结果**，可以使用聚合函数。

```sql
-- WHERE: 先过滤工资 >= 5000 的员工，再分组统计
SELECT dept_id, COUNT(*) AS high_salary_count
FROM employee
WHERE salary >= 5000
GROUP BY dept_id;

-- HAVING: 分组后筛选平均工资 > 8000 的部门
SELECT dept_id, AVG(salary) AS avg_salary
FROM employee
GROUP BY dept_id
HAVING AVG(salary) > 8000;
```

#### ROLLUP（小计）

ROLLUP 在 GROUP BY 的基础上增加小计和总计行，在报表场景非常实用。

```sql
-- 按部门和职位分组统计人数，附带小计和总计
SELECT dept_id, job_title, COUNT(*) AS cnt
FROM employee
GROUP BY dept_id, job_title WITH ROLLUP;
```

结果中 `job_title` 为 NULL 的行表示该部门的小计行，`dept_id` 和 `job_title` 都为 NULL 的行表示全表总计。

---

### 1.4 窗口函数（重点）

窗口函数是 MySQL 8.0 引入的杀手级特性，它能在不改变行数的情况下对每行数据执行聚合或排序计算。相比于 GROUP BY 会压缩行数，窗口函数保留所有原始行。

#### 基本语法

```sql
<窗口函数>() OVER (
    PARTITION BY <分组列>
    ORDER BY <排序列>
    [ROWS | RANGE BETWEEN <帧开始> AND <帧结束>]
)
```

#### ROW_NUMBER / RANK / DENSE_RANK

这三兄弟用于组内排序编号，区别在于处理并列值的方式：

```sql
-- 每个部门按工资降序排名
SELECT emp_name, dept_id, salary,
       ROW_NUMBER() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rn,
       RANK()       OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rk,
       DENSE_RANK() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS dr
FROM employee;
```

| 场景             | ROW_NUMBER | RANK | DENSE_RANK |
| ---------------- | ---------- | ---- | ---------- |
| 工资 100, 100, 90 | 1, 2, 3    | 1, 1, 3 | 1, 1, 2    |
| 是否有间隔        | —          | 有    | 无         |

**经典应用：分组 Top-N**

```sql
-- 每个部门工资前 3 名
SELECT * FROM (
    SELECT *,
           ROW_NUMBER() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rn
    FROM employee
) t
WHERE rn <= 3;
```

#### LAG / LEAD（前后行访问）

LAG 访问前 N 行，LEAD 访问后 N 行，常用于计算差值或同比环比。

```sql
-- 计算每位员工与其同部门前一名员工的工资差
SELECT emp_name, dept_id, salary,
       LAG(salary, 1) OVER (PARTITION BY dept_id ORDER BY salary) AS prev_salary,
       salary - LAG(salary, 1) OVER (PARTITION BY dept_id ORDER BY salary) AS diff
FROM employee;
```

#### 聚合窗口函数与窗口帧

SUM/AVG/COUNT 等聚合函数配合 OVER 子句可以实现累计计算。

```sql
-- 按日期累计销售额
SELECT sale_date, amount,
       SUM(amount) OVER (ORDER BY sale_date) AS running_total
FROM sales;
```

**ROWS vs RANGE**：

```sql
-- ROWS BETWEEN: 按行数滑动
SUM(amount) OVER (ORDER BY sale_date ROWS BETWEEN 2 PRECEDING AND CURRENT ROW)

-- RANGE BETWEEN: 按值范围滑动（相同值纳入同一帧）
SUM(amount) OVER (ORDER BY sale_date RANGE BETWEEN INTERVAL 7 DAY PRECEDING AND CURRENT ROW)
```

#### 窗口函数的执行顺序

窗口函数在 SQL 执行流程中的位置很特殊：它在 **WHERE、GROUP BY、HAVING 之后**，在 **ORDER BY 之前**执行。这意味着你不能在 WHERE 中直接引用窗口函数。

```sql
-- 错误写法：WHERE 中不能使用窗口函数
SELECT * FROM (
    SELECT *, ROW_NUMBER() OVER (ORDER BY salary DESC) AS rn
    FROM employee
) t
WHERE rn <= 10;  -- 需要嵌套子查询
```

完整的 SQL 执行顺序为：

```
FROM -> JOIN -> WHERE -> GROUP BY -> HAVING -> WINDOW -> SELECT -> DISTINCT -> UNION -> ORDER BY -> LIMIT
```

---

### 1.5 UNION vs UNION ALL

两者都用于合并多个查询结果集，区别在于：

- **UNION**：合并后自动去重，会多一次排序/去重的开销。
- **UNION ALL**：直接合并，保留所有行，性能更好。

```sql
-- 合并两个表的数据（去重）
SELECT name FROM customers
UNION
SELECT name FROM employees;

-- 合并两个表的数据（不去重，性能更优）
SELECT name FROM customers
UNION ALL
SELECT name FROM employees;
```

**实践原则**：明确不需要去重时，一律用 UNION ALL。如果数据本身没有重复，UNION 不仅白消耗性能，还可能导致意外的排序。

---

### 1.6 CTE（公用表表达式）与递归 CTE

CTE 用 WITH 子句定义临时命名结果集，可以看作只在当前 SQL 中存在的临时视图，极大提升复杂查询的可读性。

```sql
-- 基本 CTE
WITH dept_stats AS (
    SELECT dept_id, COUNT(*) AS cnt, AVG(salary) AS avg_sal
    FROM employee
    GROUP BY dept_id
)
SELECT d.dept_name, s.cnt, s.avg_sal
FROM department d
JOIN dept_stats s ON d.dept_id = s.dept_id;
```

#### 递归 CTE（处理树形结构）

递归 CTE 是 MySQL 8.0 的另一大杀器，专门用于处理树状数据（组织架构、分类层级、评论回复等）。

```sql
-- 递归查询所有下级部门（包含根部门）
WITH RECURSIVE dept_tree AS (
    -- 锚点成员：根部门
    SELECT dept_id, dept_name, parent_id, 1 AS level
    FROM department
    WHERE parent_id IS NULL

    UNION ALL

    -- 递归成员：查询子部门
    SELECT d.dept_id, d.dept_name, d.parent_id, t.level + 1
    FROM department d
    JOIN dept_tree t ON d.parent_id = t.dept_id
)
SELECT * FROM dept_tree ORDER BY level, dept_id;
```

递归 CTE 的关键点：
1. **锚点成员**：递归的起点，只执行一次。
2. **递归成员**：引用 CTE 自身，不断迭代直到没有新行产生。
3. 递归深度由 `cte_max_recursion_depth` 参数控制（默认 1000）。

---

**返回总览**：[00-MySQL总览与技术术语](00-MySQL总览与技术术语.md) | **上一篇**：[01-存储引擎与InnoDB架构](01-存储引擎与InnoDB架构.md) | **下一篇**：[03-事务与锁机制](03-事务与锁机制.md)
