03.21 13:24
MyBatis关联映射详解
一、关联映射概述
1.1 什么是关联映射
在实际开发中，数据库表之间往往存在关联关系（如用户与订单、部门与员工），MyBatis的关联映射就是通过配置，实现Java实体类之间的关联关系与数据库表之间关联关系的对应，自动将多表查询结果映射为关联的Java对象，无需手动处理结果集的关联拼接，简化多表查询开发。
关联映射的核心是<resultMap>标签，通过该标签配置实体类属性与数据库表字段的映射关系，同时配置关联对象的映射规则，替代入门案例中简单的resultType（仅适用于单表映射）。
1.2 数据库表关联关系分类
数据库表的关联关系主要分为3类，对应MyBatis的3种关联映射方式，开发中需根据实际表关系选择：
一对一关联：两张表中一条记录一一对应（如用户表user与用户详情表user_detail，一个用户对应一个详情）；
一对多关联：一张表的一条记录对应另一张表的多条记录（如部门表dept与员工表emp，一个部门对应多个员工）；
多对多关联：两张表的一条记录均可对应另一张表的多条记录（如学生表student与课程表course，一个学生可选多门课程，一门课程可被多个学生选择），需通过中间表（关联表）实现。
1.3 关联映射核心标签
MyBatis通过以下标签实现关联映射，均嵌套在<resultMap>内部：
<association>：用于一对一关联映射，映射单个关联对象（如User对象中包含一个UserDetail对象）；
<collection>：用于一对多、多对多关联映射，映射多个关联对象（如Dept对象中包含一个List<Emp>集合）；
<resultMap>：核心父标签，定义主表与实体类的映射，同时嵌套关联标签配置关联关系。
二、一对一关联映射（association）
2.1 场景说明
以“用户表（user）”和“用户详情表（user_detail）”为例，两张表通过用户id关联，一个用户对应一个用户详情，属于一对一关联。
表结构如下（基于入门案例的user表扩展）：
-- 用户详情表（user_detail），与user表一对一关联
CREATE TABLE IF NOT EXISTS user_detail (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL, -- 关联user表的id
    phone VARCHAR(20), -- 手机号
    address VARCHAR(200), -- 地址
    FOREIGN KEY (user_id) REFERENCES user(id) -- 外键约束，关联user表主键
);
2.2 步骤1：创建关联实体类
在User实体类中添加UserDetail类型的属性（体现一对一关联），同时创建UserDetail实体类：
// 1. 用户详情实体类 UserDetail
package com.example.pojo;
public class UserDetail {
    private Integer id;
    private Integer userId; // 关联user表的id
    private String phone;
    private String address;
    // 无参构造、有参构造、getter/setter、toString方法（省略，参考User类）
    public UserDetail() {}
    public UserDetail(Integer userId, String phone, String address) {
        this.userId = userId;
        this.phone = phone;
        this.address = address;
    }
    // getter/setter方法
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    // toString方法
    @Override
    public String toString() {
        return "UserDetail{" +
                "id=" + id +
                ", userId=" + userId +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
// 2. 改造User实体类，添加UserDetail属性
package com.example.pojo;
public class User {
    private Integer id;
    private String username;
    private String password;
    private Integer age;
    private String email;
    // 一对一关联：一个用户对应一个用户详情
    private UserDetail userDetail;
    // 无参构造、有参构造、getter/setter、toString方法（补充userDetail的相关方法）
    // 省略原有getter/setter，新增userDetail的getter/setter
    public UserDetail getUserDetail() { return userDetail; }
    public void setUserDetail(UserDetail userDetail) { this.userDetail = userDetail; }
    // 补充toString方法，打印关联对象
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", age=" + age +
                ", email='" + email + '\'' +
                ", userDetail=" + userDetail +
                '}';
    }
}
2.3 步骤2：配置一对一映射（XML方式）
修改UserMapper.xml，使用<resultMap>配置主表（user）与实体类的映射，嵌套<association>标签配置与UserDetail的一对一关联，支持两种查询方式：嵌套查询和嵌套结果。
方式1：嵌套结果（推荐，一次查询多表，性能更优）
通过多表连接查询，一次性获取所有数据，再通过映射规则分配给主实体和关联实体。
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mapper.UserMapper">
    <!-- 一对一关联：嵌套结果映射 -->
    <resultMap id="userAndDetailMap" type="com.example.pojo.User">
        <!-- 配置user表的字段与User实体的映射 -->
        <id column="id" property="id"/> <!-- 主键字段 -->
        <result column="username" property="username"/>
<result column="password" property="password"/>
        <result column="age" property="age"/>
        <result column="email" property="email"/>
        <!-- 配置一对一关联：association标签 -->
<!-- property：User实体中关联对象的属性名（userDetail） -->
<!-- javaType：关联对象的全类名 -->
        <association property="userDetail" javaType="com.example.pojo.UserDetail">
            <id column="detail_id" property="id"/> <!-- 注意：避免字段名冲突，给详情表id起别名 -->
            <result column="user_id" property="userId"/>
            <result column="phone" property="phone"/>
            <result column="address" property="address"/>
        </association>
    </resultMap>
    <!-- 多表连接查询，获取用户及对应详情 -->
</mapper>
方式2：嵌套查询（分步查询，按需加载，灵活性高）
先查询主表（user）数据，再根据主表的关联字段（id）查询关联表（user_detail）数据，需额外编写关联表的查询方法。
<!-- 1. 先在UserDetailMapper.xml中编写根据userId查询详情的方法 -->
<mapper namespace="com.example.mapper.UserDetailMapper">
</mapper>
<!-- 2. 在UserMapper.xml中配置嵌套查询的resultMap --><resultMap id="userAndDetailMap2" type="com.example.pojo.User">
    <id column="id" property="id"/>
    <result column="username" property="username"/>
    <result column="password" property="password"/>
    <result column="age" property="age"/>
    <result column="email" property="email"/>
    <!-- 嵌套查询配置 -->
    <association 
        property="userDetail" 
        javaType="com.example.pojo.UserDetail"
        select="com.example.mapper.UserDetailMapper.findByUserId" <!-- 关联查询的方法全路径 -->
        column="id" <!-- 主表中用于关联的字段（user.id），作为参数传递给关联查询 -->
    />
</resultMap>
<!-- 查询用户（会自动触发关联查询，获取详情） -->
2.4 步骤3：编写接口与测试
// 1. 在UserMapper接口中添加方法
public interface UserMapper {
    // 一对一嵌套结果查询
    User findUserWithDetail(@Param("id") Integer id);
    // 一对一嵌套查询
    User findUserWithDetail2(@Param("id") Integer id);
}
// 2. 测试类中添加测试方法
@Test
public void testFindUserWithDetail() throws IOException {
    InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        // 测试嵌套结果查询
        User user = userMapper.findUserWithDetail(1);
        System.out.println(user); // 会打印用户信息及关联的详情信息
    }
}
三、一对多关联映射（collection）
3.1 场景说明
以“部门表（dept）”和“员工表（emp）”为例，两张表通过部门id关联，一个部门对应多个员工，属于一对多关联。
表结构如下：
-- 部门表（dept）
CREATE TABLE IF NOT EXISTS dept (
    id INT PRIMARY KEY AUTO_INCREMENT,
    dept_name VARCHAR(50) NOT NULL, -- 部门名称
    dept_desc VARCHAR(200) -- 部门描述
);
-- 员工表（emp），与dept表一对多关联
CREATE TABLE IF NOT EXISTS emp (
    id INT PRIMARY KEY AUTO_INCREMENT,
    emp_name VARCHAR(50) NOT NULL, -- 员工姓名
    dept_id INT, -- 关联dept表的id
    salary DECIMAL(10,2), -- 工资
    FOREIGN KEY (dept_id) REFERENCES dept(id) -- 外键约束
);
3.2 步骤1：创建关联实体类
在Dept实体类中添加List<Emp>类型的属性（体现一对多关联），同时创建Emp实体类：
// 1. 员工实体类 Emp
package com.example.pojo;
public class Emp {
    private Integer id;
    private String empName;
    private Integer deptId;
    private Double salary;
    // 无参构造、有参构造、getter/setter、toString方法
    public Emp() {}
    public Emp(String empName, Integer deptId, Double salary) {
        this.empName = empName;
        this.deptId = deptId;
        this.salary = salary;
    }
    // getter/setter方法（省略）
    // toString方法（省略）
}
// 2. 部门实体类 Dept，添加员工集合
package com.example.pojo;
import java.util.List;
public class Dept {
    private Integer id;
    private String deptName;
    private String deptDesc;
    // 一对多关联：一个部门对应多个员工
    private List<Emp> empList;
    // 无参构造、有参构造、getter/setter、toString方法
    // 新增empList的getter/setter
    public List<Emp> getEmpList() { return empList; }
    public void setEmpList(List<Emp> empList) { this.empList = empList; }
    // toString方法，打印部门及下属员工
    @Override
    public String toString() {
        return "Dept{" +
                "id=" + id +
                ", deptName='" + deptName + '\'' +
                ", deptDesc='" + deptDesc + '\'' +
                ", empList=" + empList +
                '}';
    }
}
3.3 步骤2：配置一对多映射（XML方式）
一对多关联使用<collection>标签，同样支持嵌套结果和嵌套查询两种方式，推荐使用嵌套结果（减少数据库查询次数）。
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mapper.DeptMapper">
    <!-- 一对多关联：嵌套结果映射 -->
    <resultMap id="deptAndEmpMap" type="com.example.pojo.Dept">
        <!-- 配置dept表字段与Dept实体的映射 -->
        <id column="dept_id" property="id"/><result column="dept_name" property="deptName"/>
        <result column="dept_desc" property="deptDesc"/>
        <!-- 配置一对多关联：collection标签 -->
        <!-- property：Dept实体中关联集合的属性名（empList） -->
        <!-- ofType：集合中元素的全类名（Emp） -->
        <collection property="empList" ofType="com.example.pojo.Emp">
            <id column="emp_id" property="id"/> <!-- 别名避免字段冲突 -->
            <result column="emp_name" property="empName"/>
            <result column="dept_id" property="deptId"/>
            <result column="salary" property="salary"/>
        </collection>
    </resultMap>
    <!-- 多表连接查询，获取部门及下属所有员工 -->
    </mapper>
3.4 步骤3：编写接口与测试
// 1. 在DeptMapper接口中添加方法
public interface DeptMapper {
    // 一对多查询：根据部门id查询部门及下属员工
    Dept findDeptWithEmp(@Param("id") Integer id);
}
// 2. 测试类中添加测试方法
@Test
public void testFindDeptWithEmp() throws IOException {
    InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
        DeptMapper deptMapper = sqlSession.getMapper(DeptMapper.class);
        Dept dept = deptMapper.findDeptWithEmp(1);
        System.out.println(dept); // 打印部门信息及下属所有员工
    }
}
四、多对多关联映射（collection+中间表）
4.1 场景说明
以“学生表（student）”和“课程表（course）”为例，一个学生可选多门课程，一门课程可被多个学生选择，属于多对多关联，需创建中间表（student_course）存储两者的关联关系。
表结构如下：
-- 学生表（student）
CREATE TABLE IF NOT EXISTS student (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_name VARCHAR(50) NOT NULL, -- 学生姓名
    age INT -- 年龄
);
-- 课程表（course）
CREATE TABLE IF NOT EXISTS course (
    id INT PRIMARY KEY AUTO_INCREMENT,
    course_name VARCHAR(50) NOT NULL, -- 课程名称
    course_desc VARCHAR(200) -- 课程描述
);
-- 中间表（student_course），存储学生与课程的关联关系
CREATE TABLE IF NOT EXISTS student_course (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL, -- 关联student表的id
    course_id INT NOT NULL, -- 关联course表的id
    FOREIGN KEY (student_id) REFERENCES student(id),
    FOREIGN KEY (course_id) REFERENCES course(id),
    UNIQUE KEY (student_id, course_id) -- 避免重复关联
);
4.2 步骤1：创建关联实体类
在Student实体类中添加List<Course>属性，在Course实体类中添加List<Student>属性（双向多对多，可根据需求选择单向）：
// 1. 课程实体类 Course
package com.example.pojo;
import java.util.List;
public class Course {
    private Integer id;
    private String courseName;
    private String courseDesc;
    // 多对多关联：一门课程对应多个学生（双向关联，可选）
    private List<Student> studentList;
    // 无参构造、有参构造、getter/setter、toString方法（省略）
    public List<Student> getStudentList() { return studentList; }
    public void setStudentList(List<Student> studentList) { this.studentList = studentList; }
}
// 2. 学生实体类 Student
package com.example.pojo;
import java.util.List;
public class Student {
    private Integer id;
    private String studentName;
    private Integer age;
    // 多对多关联：一个学生对应多门课程
    private List<Course> courseList;
    // 无参构造、有参构造、getter/setter、toString方法（省略）
    public List<Course> getCourseList() { return courseList; }
    public void setCourseList(List<Course> courseList) { this.courseList = courseList; }
}
4.3 步骤2：配置多对多映射
多对多关联本质是“两个一对多关联”，通过中间表连接，使用<collection>标签配置，以“查询学生及所选课程”为例：
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mapper.StudentMapper">
    <!-- 多对多关联：嵌套结果映射 -->
    <resultMap id="studentAndCourseMap" type="com.example.pojo.Student">
        <!-- 配置student表字段与Student实体的映射 -->
        <id column="student_id" property="id"/><result column="student_name" property="studentName"/>
        <result column="age" property="age"/>
        <!-- 配置多对多关联：collection标签，关联课程集合 -->
        <collection property="courseList" ofType="com.example.pojo.Course">
            <id column="course_id" property="id"/>
            <result column="course_name" property="courseName"/>
<result column="course_desc" property="courseDesc"/>
        </collection>
    </resultMap>
    <!-- 三表连接查询，获取学生及所选课程（通过中间表关联） -->
    </mapper>
4.4 步骤3：编写接口与测试
// 1. 在StudentMapper接口中添加方法
public interface StudentMapper {
    // 多对多查询：根据学生id查询学生及所选课程
    Student findStudentWithCourse(@Param("id") Integer id);
}
// 2. 测试类中添加测试方法
@Test
public void testFindStudentWithCourse() throws IOException {
    InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
        StudentMapper studentMapper = sqlSession.getMapper(StudentMapper.class);
        Student student = studentMapper.findStudentWithCourse(1);
        System.out.println(student); // 打印学生信息及所选所有课程
    }
}
五、关联映射常见问题与注意事项
字段名冲突问题原因：多表查询时，不同表有同名字段（如id），MyBatis无法区分，导致映射错误；解决：查询时给同名字段起别名（如u.id AS user_id），在<resultMap>中通过column指定别名。
关联对象为null原因1：外键关联错误，主表与关联表的关联字段不匹配；原因2：查询条件错误，未查询到关联数据；解决：检查外键约束、关联字段配置，核对查询SQL的连接条件。
一对多查询出现重复数据原因：多表连接查询时，主表一条记录对应关联表多条记录，会导致主表数据重复；解决：使用DISTINCT去重，或在<resultMap>中配置主键（<id>标签），MyBatis会自动去重。
嵌套查询性能问题原因：嵌套查询会执行多次数据库查询（如查询10个部门，会执行1次部门查询+10次员工查询），造成性能损耗；解决：优先使用嵌套结果（一次多表查询），若需按需加载，可配置延迟加载（后续进阶内容）。
多对多关联的中间表操作注意：多对多关联的新增、删除，本质是操作中间表（添加/删除关联记录），无需修改主表；示例：给学生添加课程，只需向student_course表插入student_id和course_id即可。
六、关联映射总结
MyBatis关联映射的核心是<resultMap>标签，通过<association>和<collection>标签实现不同表关系的映射，关键要点如下：
一对一关联：用<association>，javaType指定关联对象类型；
一对多/多对多关联：用<collection>，ofType指定集合中元素类型；
性能优先：优先选择嵌套结果（一次多表查询），避免嵌套查询的多次数据库交互；
字段区分：多表查询时，给同名字段起别名，避免映射错误。
关联映射是MyBatis实战中的核心知识点，掌握后可轻松处理多表查询场景，后续可结合延迟加载、动态SQL，进一步优化关联查询的性能和灵活性。

