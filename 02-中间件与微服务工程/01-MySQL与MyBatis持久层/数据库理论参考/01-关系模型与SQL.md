# 关系模型与 SQL

## 📌 课程定位
数据库是后端开发的"心脏"——所有业务系统最终都是在做数据的增删改查。关系代数→SQL→索引优化→事务，这是数据库的学习路径。

## 🎯 核心章节

### 1. 关系模型基础
- **关系（Relation）= 表**：元组(行)+属性(列)+域(取值范围)
- **超键/候选键/主键/外键**：
  - 超键：能唯一标识元组的属性集
  - 候选键：最小的超键（去掉任何属性就不唯一了）
  - 主键：选中的候选键
  - 外键：引用另一个关系的主键，保证参照完整性
- **关系三大完整性**：实体完整性(主键非空)、参照完整性(外键约束)、用户定义完整性

### 2. 关系代数（⭐ SQL的理论基础）
- **集合运算**：并∪、交∩、差-、笛卡尔积×
- **专门关系运算**：
  - 选择σ：σ_age>18_(Student)——选择出满足条件的行（SQL WHERE）
  - 投影π：π_name,age_(Student)——选出指定列（SQL SELECT col1, col2）
  - 连接⋈：自然连接(同名列等值，去重复列)、外连接(保留悬空元组)
  - 除法÷：找出"满足所有条件"的元组——最抽象但有用

### 3. SQL 核心语法（⭐ 必熟练）

**DQL（数据查询）——最重要的部分**：
```sql
SELECT [DISTINCT] col1, col2, AGG(col3)
FROM table1 [JOIN table2 ON ...]
WHERE 条件              -- 分组前筛选
GROUP BY col1           -- 分组
HAVING 聚合条件          -- 分组后筛选（WHERE用不了聚合函数的原因所在）
ORDER BY col1 ASC/DESC  -- 排序
LIMIT n OFFSET m;       -- 分页
```

**SQL 执行顺序（⭐ 与书写顺序不同！）**：
```
FROM → ON → JOIN → WHERE → GROUP BY → HAVING → SELECT → ORDER BY → LIMIT
```

**连接（JOIN）类型**：
- **INNER JOIN**：取交集——两边都匹配的行
- **LEFT JOIN**：保留左表全部+右表匹配(无匹配填NULL)
- **RIGHT JOIN**：保留右表全部+左表匹配
- **FULL OUTER JOIN**：全保留——MySQL不支持但可用UNION模拟
- **CROSS JOIN**：笛卡尔积——无ON条件

**子查询**：
- 标量子查询：返回单个值——可用于SELECT、WHERE
- 行子查询/列子查询：IN、ANY/SOME、ALL
- **EXISTS vs IN**：EXISTS相关子查询，一行匹配就返回true；IN先算子查询结果集

### 4. DML/DDL/DCL
- **DML**：INSERT、UPDATE、DELETE（操作数据）
- **DDL**：CREATE、ALTER、DROP、TRUNCATE（操作表结构——自动提交、不可回滚）
- **DCL**：GRANT、REVOKE（权限控制）
- **TCL**：COMMIT、ROLLBACK、SAVEPOINT（事务控制）

### 5. 窗口函数（⭐ 进阶SQL必备）
```sql
SELECT name, dept, salary,
       RANK() OVER (PARTITION BY dept ORDER BY salary DESC) as rk,
       SUM(salary) OVER (PARTITION BY dept) as dept_total
FROM employees;
```
- **常用**：ROW_NUMBER()、RANK()(同值占位)、DENSE_RANK()(同值不占位)、LAG()、LEAD()
- **应用**：Top N 每组、累计求和、环比计算

## ✅ 学习建议
- SQL执行顺序必须理解——这是写复杂查询不出错的根本
- JOIN和子查询熟练切换——两者在很多场景可以互换
- LeetCode数据库题（50+道）全部刷一遍，应对面试SQL手写
- 推荐：SQLZoo、牛客网SQL实战
