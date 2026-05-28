# 第6章 MyBatis 高级查询

## 6.1 本章学习目标
1. 掌握**多对一、一对多**关联查询
2. 熟练使用 `resultMap` 嵌套映射
3. 理解**关联查询两种方式**：嵌套查询、嵌套结果
4. 掌握分页查询、模糊查询、聚合查询
5. 理清 MyBatis 关联查询业务适用场景

---

## 6.2 环境准备（表结构）

### 1）多对一场景
**员工表 emp**（多方）
**部门表 dept**（一方）
多条员工 → 对应**一个**部门

```sql
CREATE TABLE dept(
    dept_id INT PRIMARY KEY AUTO_INCREMENT,
    dept_name VARCHAR(20)
);

CREATE TABLE emp(
    emp_id INT PRIMARY KEY AUTO_INCREMENT,
    emp_name VARCHAR(20),
    salary DOUBLE,
    dept_id INT
);
```

### 2）实体类

#### 部门实体（一方）
```java
public class Dept {
    private Integer deptId;
    private String deptName;
    // 一对多：一个部门对应多个员工
    private List<Emp> empList;
}
```

#### 员工实体（多方）
```java
public class Emp {
    private Integer empId;
    private String empName;
    private Double salary;
    // 多对一：一个员工对应一个部门
    private Dept dept;
}
```

---

## 6.3 多对一查询（重点）
需求：查询员工信息 **并携带所属部门信息**

### 方式一：嵌套结果（联表查询，推荐）
通过 `join` 联表查询，一次SQL查出所有数据，`resultMap` 嵌套封装。

#### Mapper 接口
```java
List<Emp> getEmpAndDept();
```

#### Mapper XML
```xml
<resultMap id="EmpDeptMap" type="com.pojo.Emp">
    <id column="emp_id" property="empId"/>
    <result column="emp_name" property="empName"/>
    <result column="salary" property="salary"/>
    <!-- 多对一 关联部门对象 -->
    <association property="dept" javaType="com.pojo.Dept">
        <id column="dept_id" property="deptId"/>
        <result column="dept_name" property="deptName"/>
    </association>
</resultMap>

<select id="getEmpAndDept" resultMap="EmpDeptMap">
    SELECT e.*,d.*
    FROM emp e
    LEFT JOIN dept d ON e.dept_id = d.dept_id
</select>
```
- `<association>`：封装**单个对象**（多对一）

### 方式二：嵌套查询（子查询）
两条SQL：先查员工 → 根据外键再查部门
```xml
<resultMap id="EmpDeptBySubMap" type="com.pojo.Emp">
    <id column="emp_id" property="empId"/>
    <result column="emp_name" property="empName"/>
    <result column="dept_id" property="deptId"/>
    <association property="dept"
                 select="com.mapper.DeptMapper.getDeptById"
                 column="dept_id"/>
</resultMap>

<select id="getEmpAndDeptSub" resultMap="EmpDeptBySubMap">
    SELECT emp_id,emp_name,dept_id FROM emp
</select>
```

---

## 6.4 一对多查询
需求：查询部门信息 **并携带旗下所有员工**

### 嵌套结果 实现
```xml
<resultMap id="DeptEmpMap" type="com.pojo.Dept">
    <id column="dept_id" property="deptId"/>
    <result column="dept_name" property="deptName"/>
    <!-- 一对多 封装集合 -->
    <collection property="empList" ofType="com.pojo.Emp">
        <id column="emp_id" property="empId"/>
        <result column="emp_name" property="empName"/>
        <result column="salary" property="salary"/>
    </collection>
</resultMap>

<select id="getDeptAndEmp" resultMap="DeptEmpMap">
    SELECT d.*,e.*
    FROM dept d
    LEFT JOIN emp e ON d.dept_id = e.dept_id
</select>
```
- `<collection>`：封装**集合**（一对多）
- `ofType`：集合中泛型类型

---

## 6.5 分页查询

### 1）原生 limit 分页

#### 接口
```java
List<Emp> getEmpByPage(@Param("start") Integer start,
                       @Param("pageSize") Integer pageSize);
```

#### XML
```xml
<select id="getEmpByPage" resultType="com.pojo.Emp">
    SELECT * FROM emp
    LIMIT #{start},#{pageSize}
</select>
```

### 2）主流企业用法
实际开发使用 **PageHelper 分页插件**，无需手写 limit。

---

## 6.6 模糊查询
两种写法：
1. **推荐**：`#{}` 传参拼接
```xml
<select id="getEmpLike" resultType="com.pojo.Emp">
    SELECT * FROM emp
    WHERE emp_name LIKE CONCAT('%',#{name},'%')
</select>
```

2. 拼接字符串（不推荐，有注入风险）
```xml
WHERE emp_name LIKE '${name}%'
```

---

## 6.7 聚合查询
常用：count、sum、avg、max、min
```xml
<!-- 查询员工总数 -->
<select id="getEmpCount" resultType="Integer">
    SELECT COUNT(*) FROM emp
</select>

<!-- 部门平均工资 -->
<select id="getAvgSalary" resultType="Double">
    SELECT AVG(salary) FROM emp
</select>
```

---

## 6.8 关联查询标签总结
| 标签 | 作用 | 适用关系 |
|------|------|----------|
| `<association>` | 封装单个实体对象 | 多对一 |
| `<collection>` | 封装集合对象 | 一对多 |
| `resultMap` | 自定义字段与属性映射 | 所有高级查询 |

---

## 6.9 嵌套结果 vs 嵌套查询
1. **嵌套结果（联表 join）**
    - 只执行**一条SQL**
    - 效率高、开发常用
    - 适合数据量不大、关联简单场景

2. **嵌套查询（子查询）**
    - 执行**多条SQL**
    - 会产生N+1问题
    - 适合拆分复杂业务、按需加载

---

## 6.10 本章核心总结
1. 多对一使用 **association**，一对多使用 **collection**
2. 高级查询必须手动编写 `resultMap` 完成映射
3. 联表查询优先使用 LEFT JOIN 保证数据完整
4. 模糊查询使用 `CONCAT` 函数拼接，安全防注入
5. 分页基础使用 limit，企业开发使用 PageHelper
6. 多表复杂查询优先 XML，注解不适合关联复杂SQL

---
