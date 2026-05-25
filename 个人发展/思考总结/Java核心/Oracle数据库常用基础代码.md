# Oracle 数据库常用基础代码

以下是 Oracle 数据库中最常用的基础代码，包含查询、增删改、表操作、函数使用等核心场景，适配新手学习与实战：

## 一、基础查询（SELECT）

### 1. 查询全部字段

```sql
SELECT * FROM emp; -- 查询emp表所有数据
```

### 2. 查询指定字段 + 别名

```sql
SELECT empno AS 员工编号, ename AS 员工姓名, sal AS 工资 FROM emp;
```

### 3. 带条件查询（WHERE）

```sql
-- 查询工资大于3000的员工姓名和部门
SELECT ename, deptno FROM emp WHERE sal > 3000;
-- 多条件（AND/OR）
SELECT ename FROM emp WHERE sal > 2000 AND deptno = 10;
```

### 4. 排序（ORDER BY）

```sql
-- 按工资升序（ASC默认），降序用DESC
SELECT ename, sal FROM emp ORDER BY sal DESC;
```

### 5. 去重（DISTINCT）

```sql
SELECT DISTINCT deptno FROM emp; -- 查询所有不重复的部门编号
```

## 二、增删改（INSERT/UPDATE/DELETE）

### 1. 插入数据（INSERT）

```sql
-- 方式1：指定字段
INSERT INTO emp (empno, ename, job, sal) VALUES (1001, '张三', '程序员', 5000);
-- 方式2：插入所有字段（顺序与表结构一致）
INSERT INTO emp VALUES (1002, '李四', '测试', 4000, SYSDATE, 20);
```

### 2. 更新数据（UPDATE）

```sql
-- 修改员工编号1001的工资，必须加WHERE，否则修改全表
UPDATE emp SET sal = 5500 WHERE empno = 1001;
```

### 3. 删除数据（DELETE）

```sql
-- 删除指定员工，不加WHERE会删除全表数据
DELETE FROM emp WHERE empno = 1002;
-- 清空表（不可回滚，慎用）
TRUNCATE TABLE emp;
```

## 三、表操作（CREATE/ALTER/DROP）

### 1. 创建表（CREATE TABLE）

```sql
CREATE TABLE student (
    id NUMBER(10) PRIMARY KEY, -- 主键，数字类型长度10
    name VARCHAR2(20) NOT NULL, -- 字符串类型，非空
    age NUMBER(3),
    create_time DATE DEFAULT SYSDATE -- 默认当前时间
);
```

### 2. 修改表（ALTER TABLE）

```sql
-- 添加字段
ALTER TABLE student ADD gender VARCHAR2(2);
-- 修改字段类型
ALTER TABLE student MODIFY age NUMBER(2);
-- 删除字段
ALTER TABLE student DROP COLUMN gender;
```

### 3. 删除表（DROP TABLE）

```sql
DROP TABLE student; -- 删除表结构和数据
```

## 四、常用函数

### 1. 字符串函数

```sql
SELECT LENGTH('Oracle') FROM dual; -- 长度：6
SELECT UPPER('oracle') FROM dual; -- 转大写：ORACLE
SELECT SUBSTR('HelloOracle', 1, 5) FROM dual; -- 截取：Hello
```

### 2. 日期函数

```sql
SELECT SYSDATE FROM dual; -- 当前系统时间
SELECT ADD_MONTHS(SYSDATE, 3) FROM dual; -- 3个月后的时间
```

### 3. 聚合函数（统计）

```sql
SELECT COUNT(*) FROM emp; -- 总记录数
SELECT SUM(sal) FROM emp; -- 工资总和
SELECT AVG(sal) FROM emp; -- 平均工资
SELECT MAX(sal) FROM emp; -- 最高工资
```

## 五、分组查询（GROUP BY + HAVING）

```sql
-- 按部门分组，查询每个部门的平均工资，且平均工资大于2000
SELECT deptno, AVG(sal) FROM emp GROUP BY deptno HAVING AVG(sal) > 2000;
```

## 六、多表关联查询

```sql
-- 内连接：查询员工姓名和对应的部门名称
SELECT e.ename, d.dname
FROM emp e
JOIN dept d ON e.deptno = d.deptno;
```
