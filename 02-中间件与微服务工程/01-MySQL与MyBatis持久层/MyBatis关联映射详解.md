# MyBatis 关联映射详解

> **文档定位**：Java 后端技术参考文档 | MyBatis 关联映射  
> **核心作用**：实现 Java 实体类之间的关联关系与数据库表关联关系的对应，自动将多表查询结果映射为关联 Java 对象  
> **核心标签**：`<resultMap>` + `<association>`（一对一）+ `<collection>`（一对多/多对多）

---

## 目录

- [一、关联映射概述](#一关联映射概述)
- [二、一对一关联映射（association）](#二一对一关联映射association)
- [三、一对多关联映射（collection）](#三一对多关联映射collection)
- [四、多对多关联映射（collection + 中间表）](#四多对多关联映射collection--中间表)
- [五、常见问题与注意事项](#五常见问题与注意事项)

---

## 一、关联映射概述

### 数据库表关联 vs MyBatis 映射

| 关联类型 | 示例 | MyBatis 标签 |
|----------|------|-------------|
| **一对一** | 用户 ↔ 用户详情 | `<association>` |
| **一对多** | 部门 ↔ 多个员工 | `<collection>` |
| **多对多** | 学生 ↔ 课程（通过中间表） | `<collection>` + 中间表 |

### 两种查询方式

| 方式 | 说明 | 性能 |
|------|------|------|
| **嵌套结果** | 一次多表查询，一次性映射 | ⭐⭐⭐ 推荐 |
| **嵌套查询** | 分步查询，按需加载 | 需延迟加载优化 |

---

## 二、一对一关联映射（association）

### 场景：用户（user）→ 用户详情（user_detail）

```sql
CREATE TABLE IF NOT EXISTS user_detail (
    id       INT PRIMARY KEY AUTO_INCREMENT,
    user_id  INT NOT NULL,
    phone    VARCHAR(20),
    address  VARCHAR(200),
    FOREIGN KEY (user_id) REFERENCES user(id)
);
```

### 实体类

```java
public class User {
    private Integer id;
    private String username;
    private UserDetail userDetail;  // 一对一关联
    // getter/setter...
}
```

### XML 映射 — 嵌套结果（推荐）

```xml
<resultMap id="userAndDetailMap" type="User">
    <id column="id" property="id"/>
    <result column="username" property="username"/>

    <association property="userDetail" javaType="UserDetail">
        <id column="detail_id" property="id"/>
        <result column="user_id" property="userId"/>
        <result column="phone" property="phone"/>
        <result column="address" property="address"/>
    </association>
</resultMap>

<select id="findUserWithDetail" resultMap="userAndDetailMap">
    SELECT u.*, d.id AS detail_id, d.phone, d.address
    FROM user u
    LEFT JOIN user_detail d ON u.id = d.user_id
    WHERE u.id = #{id}
</select>
```

### XML 映射 — 嵌套查询（延迟加载）

```xml
<resultMap id="userAndDetailMap2" type="User">
    <id column="id" property="id"/>
    <result column="username" property="username"/>

    <association property="userDetail"
        select="com.example.mapper.UserDetailMapper.findByUserId"
        column="id"/>
</resultMap>

<select id="findUserWithDetail2" resultMap="userAndDetailMap2">
    SELECT * FROM user WHERE id = #{id}
</select>
```

---

## 三、一对多关联映射（collection）

### 场景：部门（dept）→ 多个员工（emp）

```sql
CREATE TABLE dept (
    id        INT PRIMARY KEY AUTO_INCREMENT,
    dept_name VARCHAR(50) NOT NULL
);

CREATE TABLE emp (
    id       INT PRIMARY KEY AUTO_INCREMENT,
    emp_name VARCHAR(50) NOT NULL,
    dept_id  INT,
    salary   DECIMAL(10,2),
    FOREIGN KEY (dept_id) REFERENCES dept(id)
);
```

### 实体类

```java
public class Dept {
    private Integer id;
    private String deptName;
    private List<Emp> empList;  // 一对多关联
    // getter/setter...
}
```

### XML 映射（嵌套结果）

```xml
<resultMap id="deptAndEmpMap" type="Dept">
    <id column="dept_id" property="id"/>
    <result column="dept_name" property="deptName"/>

    <collection property="empList" ofType="Emp">
        <id column="emp_id" property="id"/>
        <result column="emp_name" property="empName"/>
        <result column="salary" property="salary"/>
    </collection>
</resultMap>

<select id="findDeptWithEmp" resultMap="deptAndEmpMap">
    SELECT d.id AS dept_id, d.dept_name, e.id AS emp_id, e.emp_name, e.salary
    FROM dept d
    LEFT JOIN emp e ON d.id = e.dept_id
    WHERE d.id = #{id}
</select>
```

---

## 四、多对多关联映射（collection + 中间表）

### 场景：学生（student）↔ 课程（course）

```sql
CREATE TABLE student (id INT PRIMARY KEY AUTO_INCREMENT, student_name VARCHAR(50));
CREATE TABLE course (id INT PRIMARY KEY AUTO_INCREMENT, course_name VARCHAR(50));

-- 中间表
CREATE TABLE student_course (
    id         INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    course_id  INT NOT NULL,
    FOREIGN KEY (student_id) REFERENCES student(id),
    FOREIGN KEY (course_id) REFERENCES course(id),
    UNIQUE KEY (student_id, course_id)
);
```

### 实体类

```java
public class Student {
    private Integer id;
    private String studentName;
    private List<Course> courseList;  // 多对多关联
}
```

### XML 映射

```xml
<resultMap id="studentAndCourseMap" type="Student">
    <id column="student_id" property="id"/>
    <result column="student_name" property="studentName"/>

    <collection property="courseList" ofType="Course">
        <id column="course_id" property="id"/>
        <result column="course_name" property="courseName"/>
    </collection>
</resultMap>

<select id="findStudentWithCourse" resultMap="studentAndCourseMap">
    SELECT s.id AS student_id, s.student_name, c.id AS course_id, c.course_name
    FROM student s
    LEFT JOIN student_course sc ON s.id = sc.student_id
    LEFT JOIN course c ON sc.course_id = c.id
    WHERE s.id = #{id}
</select>
```

---

## 五、常见问题与注意事项

| 问题 | 原因 | 解决 |
|------|------|------|
| **字段名冲突** | 多表有同名字段（如 id） | 给同名字段起别名，`<resultMap>` 中对应别名 |
| **关联对象为 null** | 外键关联错误或查询条件错误 | 检查外键约束和关联字段 |
| **一对多重复数据** | 连接查询导致主表数据重复 | 使用 `<id>` 标签，MyBatis 自动去重 |
| **嵌套查询性能差** | N+1 查询问题 | 优先嵌套结果，或配置延迟加载 + 按需加载 |
| **多对多中间表操作** | 新增/删除本质是操作中间表 | 向中间表插入/删除关联记录即可 |

---

## 核心总结

| 关联类型 | 标签 | 关键属性 |
|----------|------|----------|
| **一对一** | `<association>` | `javaType` 指定关联对象类型 |
| **一对多** | `<collection>` | `ofType` 指定集合元素类型 |
| **多对多** | `<collection>` + 中间表 | 通过中间表 JOIN 实现 |

> **性能优先**：优先选择嵌套结果（一次多表查询），避免嵌套查询的多次数据库交互。
