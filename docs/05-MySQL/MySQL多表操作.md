# MySQL 多表操作 核心完整版（考试+实操）
## 一、多表关系
1. **一对一**
   用户表 — 用户详情表
2. **一对多（最常用）**
   班级(1) → 学生(多)、部门 → 员工
   **多方加外键**：`class_id`、`dept_id`
3. **多对多**
   学生 ↔ 课程
   **新建中间表**，保存两张表主键

---

## 二、笛卡尔积
多张表直接查询，不加条件，会产生**笛卡尔积**（数据错乱、冗余）
```sql
-- 错误：产生笛卡尔积
SELECT * FROM student,class;
```
**解决**：添加关联条件，消除笛卡尔积。

---

## 三、多表查询分类
### 1. 内连接 `INNER JOIN`
只查询**两张表中匹配到**的数据
```sql
SELECT 字段
FROM 表1
INNER JOIN 表2
ON 表1.外键 = 表2.主键;
```
简写（隐式内连接）：
```sql
SELECT * FROM student s,class c
WHERE s.class_id = c.id;
```

### 2. 左外连接 `LEFT JOIN`
以**左表**为基准，左表数据全部保留，右表匹配不到显示 `NULL`
```sql
SELECT *
FROM 表1
LEFT JOIN 表2
ON 关联条件;
```

### 3. 右外连接 `RIGHT JOIN`
以**右表**为基准，右表数据全部保留，左表无匹配为 `NULL`

### 4. 全外连接
MySQL **不支持** `FULL JOIN`，
可通过 **左连接 UNION 右连接** 模拟实现。

---

## 四、UNION 与 UNION ALL
- `UNION`：合并结果集，**自动去重**
- `UNION ALL`：直接拼接，**不去重、效率更高**
  要求：多张查询的**字段数量、类型一致**
```sql
SELECT name FROM student
UNION
SELECT class_name FROM class;
```

---

## 五、子查询（嵌套查询）
### 1. 标量子查询（单行单列）
```sql
SELECT * FROM student
WHERE class_id = (SELECT id FROM class WHERE class_name='一班');
```

### 2. 列子查询（多行单列）
搭配 `IN`、`ANY`、`ALL`
```sql
SELECT * FROM student
WHERE class_id IN (SELECT id FROM class);
```

### 3. 行子查询、表子查询
查询结果当作**一张临时表**，必须起别名
```sql
SELECT * FROM (SELECT name,age FROM student) s;
```

---

## 六、三大关联查询 必背区别
1. **内连接**：只查两边匹配数据
2. **左连接**：左表全有，右表匹配
3. **右连接**：右表全有，左表匹配

---

## 七、多表增删改（了解）
1. 多表**不推荐联合修改**
2. 企业规范：单表操作 + 业务代码控制关联
3. 外键可约束关联数据，项目开发一般禁用物理外键

---

## 八、经典实操示例
### 表结构
```sql
-- 班级表
CREATE TABLE class(
    id INT PRIMARY KEY AUTO_INCREMENT,
    class_name VARCHAR(20)
);
-- 学生表
CREATE TABLE student(
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(20),
    class_id INT
);
```

### 内连接示例
```sql
SELECT s.name,c.class_name
FROM student s
JOIN class c
ON s.class_id = c.id;
```

### 左连接示例（查询所有学生，含无班级的学生）
```sql
SELECT s.name,c.class_name
FROM student s
LEFT JOIN class c
ON s.class_id = c.id;
```

---

## 九、高频考点总结
1. 多表查询必须加 **ON 关联条件**，防止笛卡尔积
2. 优先使用 `JOIN` 写法，可读性更高
3. 左连接场景：需要**左表全部数据**
4. 子查询结果为临时表，必须别名
5. `UNION` 去重，`UNION ALL` 高效率