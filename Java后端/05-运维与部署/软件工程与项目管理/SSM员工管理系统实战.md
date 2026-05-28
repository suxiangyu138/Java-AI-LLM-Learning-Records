# SSM 员工管理系统实战教程

> 技术栈：Spring 5 + SpringMVC 5 + MyBatis 3 + MySQL 8 + Maven + Tomcat 9
> 核心考点：**多表联查**、**动态 SQL**、**登录拦截器**、**全局异常处理**
> 适用场景：简历项目（Java 后端方向）、SSM 框架期末作业、面试八股复盘
> 开发环境：Windows + VSCode（搭配 Extension Pack for Java + Tomcat for Java）

---

## 一、项目目录结构

```
ssm-employee/
├── pom.xml
├── src/main/java/com/ahu/ems/
│   ├── controller/        # 控制层
│   ├── service/           # 业务接口
│   │   └── impl/          # 业务实现
│   ├── mapper/            # MyBatis 接口
│   ├── pojo/              # 实体（Employee / Department / EmployeeVO）
│   ├── common/            # 通用类（Result、BizException、ResultCode）
│   ├── interceptor/       # 拦截器
│   └── handler/           # 全局异常处理器
├── src/main/resources/
│   ├── mapper/            # *.xml SQL 映射
│   ├── jdbc.properties
│   ├── spring-dao.xml
│   ├── spring-service.xml
│   ├── spring-mvc.xml
│   ├── mybatis-config.xml
│   └── logback.xml
└── src/main/webapp/
    ├── WEB-INF/web.xml
    └── login.html
```

---

## 二、数据库设计（核心：一对多关系）

```sql
CREATE DATABASE ems DEFAULT CHARSET utf8mb4;
USE ems;

CREATE TABLE department (
  id          INT PRIMARY KEY AUTO_INCREMENT,
  dept_name   VARCHAR(50) NOT NULL,
  dept_leader VARCHAR(20)
);

CREATE TABLE employee (
  id         INT PRIMARY KEY AUTO_INCREMENT,
  username   VARCHAR(30) NOT NULL UNIQUE,
  password   VARCHAR(64) NOT NULL,
  real_name  VARCHAR(20),
  gender     TINYINT      DEFAULT 1,
  age        INT,
  salary     DECIMAL(10,2),
  hire_date  DATE,
  dept_id    INT,
  status     TINYINT      DEFAULT 1,
  CONSTRAINT fk_emp_dept FOREIGN KEY (dept_id) REFERENCES department(id)
);

INSERT INTO department(dept_name, dept_leader) VALUES
('研发部','张三'),('运维部','李四'),('产品部','王五');

INSERT INTO employee(username,password,real_name,gender,age,salary,hire_date,dept_id) VALUES
('admin','123456','管理员',1,25,15000,'2024-01-01',1),
('tom','123456','汤姆',1,28,12000,'2023-05-12',1),
('jerry','123456','杰瑞',2,26,11000,'2024-03-08',2);
```

---

## 三、Maven 依赖（pom.xml 关键片段）

```xml
<properties>
    <spring.version>5.3.30</spring.version>
    <mybatis.version>3.5.13</mybatis.version>
</properties>

<dependencies>
    <!-- Spring -->
    <dependency><groupId>org.springframework</groupId><artifactId>spring-webmvc</artifactId><version>${spring.version}</version></dependency>
    <dependency><groupId>org.springframework</groupId><artifactId>spring-jdbc</artifactId><version>${spring.version}</version></dependency>
    <dependency><groupId>org.springframework</groupId><artifactId>spring-tx</artifactId><version>${spring.version}</version></dependency>
    <!-- MyBatis -->
    <dependency><groupId>org.mybatis</groupId><artifactId>mybatis</artifactId><version>${mybatis.version}</version></dependency>
    <dependency><groupId>org.mybatis</groupId><artifactId>mybatis-spring</artifactId><version>2.1.1</version></dependency>
    <!-- 连接池 / 驱动 -->
    <dependency><groupId>com.alibaba</groupId><artifactId>druid</artifactId><version>1.2.20</version></dependency>
    <dependency><groupId>mysql</groupId><artifactId>mysql-connector-java</artifactId><version>8.0.33</version></dependency>
    <!-- JSON -->
    <dependency><groupId>com.fasterxml.jackson.core</groupId><artifactId>jackson-databind</artifactId><version>2.15.3</version></dependency>
    <!-- Servlet / JSTL -->
    <dependency><groupId>javax.servlet</groupId><artifactId>javax.servlet-api</artifactId><version>4.0.1</version><scope>provided</scope></dependency>
    <!-- Lombok（可选） -->
    <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><version>1.18.30</version></dependency>
</dependencies>

<build>
    <resources>
        <resource><directory>src/main/java</directory><includes><include>**/*.xml</include></includes></resource>
        <resource><directory>src/main/resources</directory></resource>
    </resources>
</build>
```

---

## 四、Spring 整合配置

### 4.1 jdbc.properties

```properties
jdbc.url=jdbc:mysql://localhost:3306/ems?useSSL=false&serverTimezone=Asia/Shanghai
jdbc.username=root
jdbc.password=123456
jdbc.driver=com.mysql.cj.jdbc.Driver
```

### 4.2 spring-dao.xml（数据源 + SqlSessionFactory + Mapper 扫描）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">

    <context:property-placeholder location="classpath:jdbc.properties"/>

    <bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource" init-method="init" destroy-method="close">
        <property name="url" value="${jdbc.url}"/>
        <property name="username" value="${jdbc.username}"/>
        <property name="password" value="${jdbc.password}"/>
        <property name="driverClassName" value="${jdbc.driver}"/>
    </bean>

    <bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
        <property name="dataSource" ref="dataSource"/>
        <property name="configLocation" value="classpath:mybatis-config.xml"/>
        <property name="mapperLocations" value="classpath:mapper/*.xml"/>
        <property name="typeAliasesPackage" value="com.ahu.ems.pojo"/>
    </bean>

    <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
        <property name="basePackage" value="com.ahu.ems.mapper"/>
    </bean>
</beans>
```

### 4.3 spring-service.xml

```xml
<context:component-scan base-package="com.ahu.ems.service"/>
<bean id="txManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
    <property name="dataSource" ref="dataSource"/>
</bean>
<tx:annotation-driven transaction-manager="txManager"/>
```

### 4.4 spring-mvc.xml

```xml
<context:component-scan base-package="com.ahu.ems.controller"/>
<context:component-scan base-package="com.ahu.ems.handler"/>
<mvc:annotation-driven/>
<mvc:default-servlet-handler/>

<!-- 拦截器注册 -->
<mvc:interceptors>
    <mvc:interceptor>
        <mvc:mapping path="/**"/>
        <mvc:exclude-mapping path="/user/login"/>
        <mvc:exclude-mapping path="/login.html"/>
        <mvc:exclude-mapping path="/static/**"/>
        <bean class="com.ahu.ems.interceptor.LoginInterceptor"/>
    </mvc:interceptor>
</mvc:interceptors>
```

### 4.5 web.xml

```xml
<context-param>
    <param-name>contextConfigLocation</param-name>
    <param-value>classpath:spring-dao.xml,classpath:spring-service.xml</param-value>
</context-param>
<listener>
    <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
</listener>

<servlet>
    <servlet-name>dispatcher</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
    <init-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>classpath:spring-mvc.xml</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
</servlet>
<servlet-mapping>
    <servlet-name>dispatcher</servlet-name>
    <url-pattern>/</url-pattern>
</servlet-mapping>

<filter>
    <filter-name>encoding</filter-name>
    <filter-class>org.springframework.web.filter.CharacterEncodingFilter</filter-class>
    <init-param><param-name>encoding</param-name><param-value>UTF-8</param-value></init-param>
</filter>
<filter-mapping><filter-name>encoding</filter-name><url-pattern>/*</url-pattern></filter-mapping>
```

---

## 五、实体类（POJO）

```java
@Data
public class Department {
    private Integer id;
    private String deptName;
    private String deptLeader;
}

@Data
public class Employee {
    private Integer id;
    private String username;
    private String password;
    private String realName;
    private Integer gender;
    private Integer age;
    private BigDecimal salary;
    private LocalDate hireDate;
    private Integer deptId;
    private Integer status;
}

@Data
public class EmployeeVO extends Employee {
    private Department department;
}

@Data
public class EmployeeQuery {
    private String realName;
    private Integer deptId;
    private Integer gender;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
}
```

---

## 六、核心一：多表联查 + 动态 SQL（EmployeeMapper.xml）

```java
public interface EmployeeMapper {
    Employee findByUsername(String username);
    List<EmployeeVO> selectByCondition(EmployeeQuery query);
    int batchUpdateStatus(@Param("ids") List<Integer> ids, @Param("status") Integer status);
    int updateSelective(Employee emp);
}
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ahu.ems.mapper.EmployeeMapper">

    <!-- 多表联查 ResultMap：association 处理一对一 -->
    <resultMap id="empVOMap" type="com.ahu.ems.pojo.EmployeeVO">
        <id     column="id"        property="id"/>
        <result column="username"  property="username"/>
        <result column="real_name" property="realName"/>
        <result column="gender"    property="gender"/>
        <result column="age"       property="age"/>
        <result column="salary"    property="salary"/>
        <result column="hire_date" property="hireDate"/>
        <result column="dept_id"   property="deptId"/>
        <result column="status"    property="status"/>
        <association property="department" javaType="com.ahu.ems.pojo.Department">
            <id     column="d_id"     property="id"/>
            <result column="d_name"   property="deptName"/>
            <result column="d_leader" property="deptLeader"/>
        </association>
    </resultMap>

    <!-- 登录用 -->
    <select id="findByUsername" resultType="Employee">
        SELECT id,username,password,real_name realName,gender,age,salary,
               hire_date hireDate,dept_id deptId,status
        FROM employee WHERE username = #{username} AND status = 1
    </select>

    <!-- 动态 SQL + 多表联查 -->
    <select id="selectByCondition" resultMap="empVOMap">
        SELECT e.id, e.username, e.real_name, e.gender, e.age, e.salary,
               e.hire_date, e.dept_id, e.status,
               d.id AS d_id, d.dept_name AS d_name, d.dept_leader AS d_leader
        FROM employee e
        LEFT JOIN department d ON e.dept_id = d.id
        <where>
            <if test="realName != null and realName != ''">
                AND e.real_name LIKE CONCAT('%',#{realName},'%')
            </if>
            <if test="deptId != null">
                AND e.dept_id = #{deptId}
            </if>
            <if test="gender != null">
                AND e.gender = #{gender}
            </if>
            <if test="minSalary != null">
                AND e.salary &gt;= #{minSalary}
            </if>
            <if test="maxSalary != null">
                AND e.salary &lt;= #{maxSalary}
            </if>
        </where>
        ORDER BY e.id DESC
    </select>

    <!-- 动态 SQL：批量 + foreach -->
    <update id="batchUpdateStatus">
        UPDATE employee SET status = #{status}
        WHERE id IN
        <foreach collection="ids" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
    </update>

    <!-- 动态 SQL：set 标签按需更新 -->
    <update id="updateSelective" parameterType="Employee">
        UPDATE employee
        <set>
            <if test="realName != null">real_name = #{realName},</if>
            <if test="gender   != null">gender    = #{gender},</if>
            <if test="age      != null">age       = #{age},</if>
            <if test="salary   != null">salary    = #{salary},</if>
            <if test="deptId   != null">dept_id   = #{deptId},</if>
            <if test="status   != null">status    = #{status},</if>
        </set>
        WHERE id = #{id}
    </update>
</mapper>
```

> **重点掌握**
> - `<resultMap>` + `<association>`：一对一映射（员工 → 部门）
> - `<where>` 自动处理首个 `AND`
> - `<set>` 自动处理末尾逗号
> - `<foreach>` 实现 IN 批量操作
> - `&gt;` / `&lt;`：XML 中转义大于小于号

---

## 七、Service 层

```java
public interface EmployeeService {
    Employee login(String username, String password);
    List<EmployeeVO> list(EmployeeQuery query);
    void updateEmp(Employee emp);
    void disable(List<Integer> ids);
}

@Service
public class EmployeeServiceImpl implements EmployeeService {
    @Autowired private EmployeeMapper employeeMapper;

    @Override
    public Employee login(String username, String password) {
        Employee emp = employeeMapper.findByUsername(username);
        if (emp == null) throw new BizException(ResultCode.USER_NOT_EXIST);
        if (!emp.getPassword().equals(password)) throw new BizException(ResultCode.PASSWORD_ERROR);
        return emp;
    }

    @Override
    public List<EmployeeVO> list(EmployeeQuery q) { return employeeMapper.selectByCondition(q); }

    @Override
    @Transactional
    public void updateEmp(Employee emp) { employeeMapper.updateSelective(emp); }

    @Override
    @Transactional
    public void disable(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) throw new BizException(ResultCode.PARAM_ERROR);
        employeeMapper.batchUpdateStatus(ids, 0);
    }
}
```

---

## 八、统一返回值与业务异常

```java
public enum ResultCode {
    SUCCESS(200,"成功"),
    PARAM_ERROR(400,"参数错误"),
    UNAUTHORIZED(401,"未登录"),
    USER_NOT_EXIST(1001,"用户不存在"),
    PASSWORD_ERROR(1002,"密码错误"),
    SYSTEM_ERROR(500,"系统异常");

    public final int code; public final String msg;
    ResultCode(int c,String m){this.code=c;this.msg=m;}
}

@Data @AllArgsConstructor @NoArgsConstructor
public class Result<T> {
    private int code;
    private String msg;
    private T data;
    public static <T> Result<T> ok(T data){ return new Result<>(200,"成功",data); }
    public static <T> Result<T> ok(){ return ok(null); }
    public static <T> Result<T> fail(ResultCode rc){ return new Result<>(rc.code,rc.msg,null); }
}

public class BizException extends RuntimeException {
    private final ResultCode resultCode;
    public BizException(ResultCode rc){ super(rc.msg); this.resultCode = rc; }
    public ResultCode getResultCode(){ return resultCode; }
}
```

---

## 九、Controller 层

```java
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired private EmployeeService employeeService;

    @PostMapping("/login")
    public Result<Employee> login(@RequestParam String username,
                                  @RequestParam String password,
                                  HttpSession session){
        Employee emp = employeeService.login(username,password);
        session.setAttribute("loginUser", emp);
        return Result.ok(emp);
    }

    @GetMapping("/logout")
    public Result<Void> logout(HttpSession session){ session.invalidate(); return Result.ok(); }
}

@RestController
@RequestMapping("/emp")
public class EmployeeController {
    @Autowired private EmployeeService employeeService;

    @GetMapping("/list")
    public Result<List<EmployeeVO>> list(EmployeeQuery query){
        return Result.ok(employeeService.list(query));
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody Employee emp){
        employeeService.updateEmp(emp);
        return Result.ok();
    }

    @DeleteMapping("/disable")
    public Result<Void> disable(@RequestBody List<Integer> ids){
        employeeService.disable(ids);
        return Result.ok();
    }
}
```

---

## 十、核心二：登录拦截器

```java
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        // 放行 OPTIONS 预检
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) return true;

        Object loginUser = req.getSession().getAttribute("loginUser");
        if (loginUser != null) return true;

        // 未登录：返回 JSON
        resp.setContentType("application/json;charset=UTF-8");
        resp.setStatus(401);
        Result<Void> r = Result.fail(ResultCode.UNAUTHORIZED);
        resp.getWriter().write(new ObjectMapper().writeValueAsString(r));
        return false;
    }
}
```

> **链路**：请求进入 → DispatcherServlet → 拦截器 `preHandle` → Controller → `postHandle` → 视图渲染 → `afterCompletion`
> 注册位置：`spring-mvc.xml` 的 `<mvc:interceptors>`，**已排除登录接口与静态资源**。

---

## 十一、核心三：全局异常处理器

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e){
        log.warn("业务异常：{}", e.getMessage());
        return Result.fail(e.getResultCode());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleParam(MissingServletRequestParameterException e){
        log.warn("参数缺失：{}", e.getParameterName());
        return Result.fail(ResultCode.PARAM_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleAll(Exception e){
        log.error("系统异常", e);
        return Result.fail(ResultCode.SYSTEM_ERROR);
    }
}
```

> **关键点**
> - `@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`
> - 异常匹配遵循**最具体优先**原则
> - 业务异常（`BizException`）走业务码；未知异常统一返回 500，避免堆栈泄露

---

## 十二、运行 & 测试

### 12.1 启动

VSCode 安装 **Tomcat for Java** 插件 → 添加 Tomcat 9 → 右键 war 包 Run on Server → 访问 `http://localhost:8080/ssm-employee/login.html`

### 12.2 接口测试（推荐 Apifox / curl）

```bash
# 登录
curl -X POST -d "username=admin&password=123456" -c cookie.txt http://localhost:8080/ssm-employee/user/login

# 条件查询（动态 SQL + 多表联查）
curl -b cookie.txt "http://localhost:8080/ssm-employee/emp/list?deptId=1&minSalary=10000"

# 未登录访问 → 触发拦截器
curl http://localhost:8080/ssm-employee/emp/list
# 返回 {"code":401,"msg":"未登录","data":null}

# 触发全局异常
curl -b cookie.txt "http://localhost:8080/ssm-employee/emp/list?gender=abc"
# 返回 {"code":400,"msg":"参数错误","data":null}
```

---

## 十三、易错点速查

| 现象 | 根因 | 解决 |
|---|---|---|
| Mapper XML 找不到 | resources 未打包 XML | pom.xml 加 `<resources>` 配置 |
| `BindingException` | namespace ≠ 接口全限定名 | 严格一致 |
| 字段映射不上 | 下划线 vs 驼峰 | `mybatis-config.xml` 开启 `mapUnderscoreToCamelCase` |
| 拦截器对静态资源生效 | 未排除路径 | `<mvc:exclude-mapping>` + `<mvc:default-servlet-handler/>` |
| 全局异常不生效 | 未扫描到 `handler` 包 | `spring-mvc.xml` 加 component-scan |
| 中文乱码 | 缺编码过滤器 | `CharacterEncodingFilter` |

---

## 十四、简历包装话术（一句话版）

> **基于 SSM 的企业员工管理系统**
> - 基于 Spring + SpringMVC + MyBatis 搭建分层架构，使用 Druid 连接池与声明式事务保障数据一致性
> - 通过 MyBatis `<resultMap>` + `<association>` 实现员工-部门**一对一多表联查**，结合 `<where>` / `<set>` / `<foreach>` 动态 SQL 支持多条件检索与批量操作
> - 自定义 `HandlerInterceptor` 实现登录态校验，统一拦截未授权请求并返回标准 JSON
> - 通过 `@RestControllerAdvice` 实现**全局异常处理**，结合自定义业务异常 + 枚举码体系，统一接口响应格式，提升前后端协作效率

---

## 十五、可拓展方向（加分项）

1. 引入 **PageHelper** 实现物理分页
2. 整合 **Spring Security / JWT** 替换 Session 拦截器
3. 接入 **Redis** 缓存部门列表、登录态
4. 前端使用 **Vue3 + Element Plus** 提供管理后台
5. 抽取 AOP 日志切面，记录所有 Controller 入参/返回
