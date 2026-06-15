可以，我直接按“**企业级简历项目**”标准，把“用户权限管理系统”一次性给你完整方案：包含设计文档、数据库设计、项目结构、核心代码骨架、接口设计和简历写法。RBAC 的核心是“用户不直接绑定权限，而是通过角色获得权限”，这也是主流权限模型的基本思路；Spring Boot 中常见做法是结合 Spring Security 和 JWT 保护接口，再由数据库维护用户、角色、权限及其关联关系。 [docs.logto](https://docs.logto.io/api-protection/java/spring-boot)

## 项目目标

这个项目建议你做成一个标准的 **RBAC 权限中台**：支持登录认证、用户管理、角色管理、权限点管理、角色授权、用户分配角色、接口级权限控制、菜单级权限控制。RBAC 的数据库建模通常至少包含用户、角色、权限三类实体，以及用户-角色、角色-权限两张关联表，这样权限不会直接绑到用户上，扩展性更好。 [celerdata](https://celerdata.com/glossary/role-based-access-control-rbac)

技术栈建议定为：Spring Boot 3 + Spring Security 6 + JWT + MyBatis + MySQL。Spring Security 6 与 JWT 配合是当前 Spring Boot 3 项目里很常见的鉴权方案，而 MyBatis 很适合你这种想把 SQL、表结构和后台 CRUD 能力都写进简历的 Java 后端项目。 [github](https://github.com/markvivv/springboot-security-jwt-example)

## 架构设计

系统按企业级分层来做：`controller` 负责接口入口，`service` 负责业务编排，`mapper` 负责数据库访问，`entity/dto/vo` 分离数据对象，`security` 负责 JWT 解析和认证上下文写入，`common` 负责统一返回和异常处理。这个分层方式也符合很多 Spring Boot + MyBatis 示例项目的组织习惯，尤其是把安全配置、过滤器、用户详情服务、统一异常都抽到独立模块中。 [katyella](https://katyella.com/blog/spring-boot-security-best-practices/)

鉴权链路建议是：用户登录后，系统校验用户名密码，生成 JWT；后续请求携带 Token，经 JWT 过滤器解析后写入 Spring Security 的 `SecurityContext`，再由接口上的角色/权限规则决定是否放行。Spring 生态里 JWT 方案通常就是通过过滤器或认证提供者完成请求级认证，并在 Token 中携带必要身份信息与权限信息。 [reddit](https://www.reddit.com/r/SpringBoot/comments/1i9fc1q/best_practices_for_rolebased_access_in_spring/)

## 数据库设计

下面这套表结构是最适合你简历表达的 V1 版本，既标准又不臃肿：

```sql
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_status (status)
);

CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(50) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    permission_name VARCHAR(100) NOT NULL,
    permission_type TINYINT NOT NULL COMMENT '1菜单 2按钮 3接口',
    path VARCHAR(200),
    method VARCHAR(20),
    parent_id BIGINT DEFAULT 0,
    sort_num INT DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id)
);

CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission_id)
);
```

初始化数据建议至少有两个角色：`ROLE_ADMIN` 和 `ROLE_USER`；权限点建议用 `user:list`、`user:add`、`role:list`、`role:assign` 这种命名。RBAC 的本质就是把权限先授予角色，再把角色分配给用户，这种“间接授权”可以降低直接给用户分配权限带来的管理复杂度。 [ibm](https://www.ibm.com/think/topics/role-based-access-control-implementation)

## 项目结构

建议目录这样设计：

```text
user-auth-system
├── pom.xml
├── src/main/java/com/example/rbac
│   ├── RbacApplication.java
│   ├── common
│   │   ├── result
│   │   │   ├── Result.java
│   │   │   └── ResultCode.java
│   │   └── exception
│   │       ├── BizException.java
│   │       └── GlobalExceptionHandler.java
│   ├── config
│   │   ├── SecurityConfig.java
│   │   ├── MybatisConfig.java
│   │   └── PasswordConfig.java
│   ├── controller
│   │   ├── AuthController.java
│   │   ├── UserController.java
│   │   ├── RoleController.java
│   │   └── PermissionController.java
│   ├── dto
│   │   ├── LoginDTO.java
│   │   ├── UserCreateDTO.java
│   │   └── RoleAssignDTO.java
│   ├── entity
│   │   ├── SysUser.java
│   │   ├── SysRole.java
│   │   ├── SysPermission.java
│   │   ├── SysUserRole.java
│   │   └── SysRolePermission.java
│   ├── mapper
│   │   ├── UserMapper.java
│   │   ├── RoleMapper.java
│   │   └── PermissionMapper.java
│   ├── security
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtTokenProvider.java
│   │   ├── LoginUser.java
│   │   └── CustomUserDetailsService.java
│   ├── service
│   │   ├── AuthService.java
│   │   ├── UserService.java
│   │   ├── RoleService.java
│   │   └── PermissionService.java
│   └── vo
│       ├── LoginVO.java
│       └── UserVO.java
└── src/main/resources
    ├── application.yml
    └── mapper
        ├── UserMapper.xml
        ├── RoleMapper.xml
        └── PermissionMapper.xml
```

像统一返回体、JWT 过滤器、`UserDetailsService`、统一异常处理这些内容，都是企业级后台项目里很常见的基础设施层能力，也正是简历里容易拉开和“学生 CRUD 项目”差距的地方。 [github](https://github.com/markvivv/springboot-security-jwt-example)

## 核心接口

建议先完成这组接口：

| 接口 | 方法 | 权限 |
|---|---|---|
| `/api/auth/login` | POST | 公开 |
| `/api/auth/me` | GET | 登录后 |
| `/api/users` | GET | `user:list` |
| `/api/users` | POST | `user:add` |
| `/api/users/{id}` | PUT | `user:update` |
| `/api/users/{id}` | DELETE | `user:delete` |
| `/api/roles` | GET | `role:list` |
| `/api/roles/{id}/permissions` | POST | `role:assign` |
| `/api/users/{id}/roles` | POST | `user:assign` |

JWT 保护接口的常见方式是登录后在请求头携带 `Authorization: Bearer <token>`，服务端验证 Token 后，再根据角色或权限判断请求是否能访问目标接口。 [geeksforgeeks](https://www.geeksforgeeks.org/springboot/spring-boot-3-0-jwt-authentication-with-spring-security-using-mysql-database/)

## 核心代码

下面我直接给你一版可以当骨架起项目的代码，尽量贴近 Spring Boot 3 + Security 6 + MyBatis 的企业级写法。

### `pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>user-auth-system</artifactId>
    <version>1.0.0</version>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>3.0.3</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.6</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</project>
```

Spring Boot 3 + Security 6 + JWT + MyBatis 这类依赖组合，是当前构建 Spring 后台鉴权项目的典型技术栈；JWT 示例里也普遍通过 `jjwt` 处理 Token 的生成和解析。 [geeksforgeeks](https://www.geeksforgeeks.org/springboot/spring-boot-3-0-jwt-authentication-with-spring-security-using-mysql-database/)

### `application.yml`

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rbac_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.rbac.entity

jwt:
  secret: 5367566859703373367639792F423F452848284D6251655468576D5A71347437
  expire: 86400000
```

### `Result.java`

```java
package com.example.rbac.common.result;

public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
```

统一返回体和统一异常处理是很多 Spring Boot + JWT 示例项目里都会单独抽象的基础能力，这样控制器层能保持更稳定的输出结构。 [github](https://github.com/markvivv/springboot-security-jwt-example)

### `SysUser.java`

```java
package com.example.rbac.entity;

import java.time.LocalDateTime;

public class SysUser {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private Integer status;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
```

### `LoginDTO.java`

```java
package com.example.rbac.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginDTO {
    @NotBlank
    private String username;

    @NotBlank
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
```

### `UserMapper.java`

```java
package com.example.rbac.mapper;

import com.example.rbac.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    SysUser selectByUsername(@Param("username") String username);

    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    List<SysUser> selectUserList();

    int insertUser(SysUser user);

    int updateUser(SysUser user);

    int deleteUser(@Param("id") Long id);
}
```

### `UserMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.rbac.mapper.UserMapper">

    <select id="selectByUsername" resultType="com.example.rbac.entity.SysUser">
        select id, username, password, real_name as realName, phone, email, status, deleted, create_time as createTime, update_time as updateTime
        from sys_user
        where username = #{username} and deleted = 0
        limit 1
    </select>

    <select id="selectRoleCodesByUserId" resultType="string">
        select r.role_code
        from sys_role r
        inner join sys_user_role ur on ur.role_id = r.id
        where ur.user_id = #{userId} and r.deleted = 0 and r.status = 1
    </select>

    <select id="selectPermissionCodesByUserId" resultType="string">
        select p.permission_code
        from sys_permission p
        inner join sys_role_permission rp on rp.permission_id = p.id
        inner join sys_user_role ur on ur.role_id = rp.role_id
        inner join sys_role r on r.id = ur.role_id
        where ur.user_id = #{userId}
          and p.deleted = 0 and p.status = 1
          and r.deleted = 0 and r.status = 1
    </select>

    <select id="selectUserList" resultType="com.example.rbac.entity.SysUser">
        select id, username, password, real_name as realName, phone, email, status, deleted, create_time as createTime, update_time as updateTime
        from sys_user
        where deleted = 0
        order by id desc
    </select>

    <insert id="insertUser" parameterType="com.example.rbac.entity.SysUser" useGeneratedKeys="true" keyProperty="id">
        insert into sys_user (username, password, real_name, phone, email, status, deleted)
        values (#{username}, #{password}, #{realName}, #{phone}, #{email}, #{status}, 0)
    </insert>

    <update id="updateUser" parameterType="com.example.rbac.entity.SysUser">
        update sys_user
        set real_name = #{realName},
            phone = #{phone},
            email = #{email},
            status = #{status}
        where id = #{id} and deleted = 0
    </update>

    <update id="deleteUser">
        update sys_user
        set deleted = 1
        where id = #{id} and deleted = 0
    </update>
</mapper>
```

### `LoginUser.java`

```java
package com.example.rbac.security;

import com.example.rbac.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class LoginUser implements UserDetails {
    private final SysUser user;
    private final List<String> authorities;

    public LoginUser(SysUser user, List<String> authorities) {
        this.user = user;
        this.authorities = authorities;
    }

    public SysUser getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }
}
```

### `CustomUserDetailsService.java`

```java
package com.example.rbac.security;

import com.example.rbac.entity.SysUser;
import com.example.rbac.mapper.UserMapper;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserMapper userMapper;

    public CustomUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null || user.getStatus() == 0) {
            throw new UsernameNotFoundException("用户不存在或已禁用");
        }
        List<String> authorities = new ArrayList<>();
        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = userMapper.selectPermissionCodesByUserId(user.getId());
        authorities.addAll(roles);
        authorities.addAll(permissions);
        return new LoginUser(user, authorities);
    }
}
```

很多 Spring Security + JWT 项目会通过自定义 `UserDetailsService` 从数据库加载用户，并把角色、权限转换为 `GrantedAuthority` 交给框架进行后续鉴权。 [katyella](https://katyella.com/blog/spring-boot-security-best-practices/)

### `JwtTokenProvider.java`

```java
package com.example.rbac.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire}")
    private Long expire;

    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date end = new Date(now.getTime() + expire);
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(end)
                .signWith(getKey())
                .compact();
    }

    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        String username = getUsername(token);
        return username.equals(userDetails.getUsername()) && getClaims(token).getExpiration().after(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
```

JWT 常见实现就是在 Token 中保存 `subject`、签发时间和过期时间，再用密钥校验有效性；Spring Boot 3 场景下，`jjwt` 依然是很常见的实现选择。 [geeksforgeeks](https://www.geeksforgeeks.org/springboot/spring-boot-3-0-jwt-authentication-with-spring-security-using-mysql-database/)

### `JwtAuthenticationFilter.java`

```java
package com.example.rbac.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = auth.substring(7);
        String username = jwtTokenProvider.getUsername(token);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtTokenProvider.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

过滤器在每次请求中解析 Bearer Token 并写入认证上下文，是 JWT 方案保护接口的典型流程。 [reddit](https://www.reddit.com/r/SpringBoot/comments/1i9fc1q/best_practices_for_rolebased_access_in_spring/)

### `SecurityConfig.java`

```java
package com.example.rbac.config;

import com.example.rbac.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
```

Spring Security 6 常见配置方式就是通过 `SecurityFilterChain` 代替旧版适配器，并显式配置无状态会话和 JWT 过滤器链路。 [katyella](https://katyella.com/blog/spring-boot-security-best-practices/)

### `AuthService.java`

```java
package com.example.rbac.service;

import com.example.rbac.dto.LoginDTO;
import com.example.rbac.security.JwtTokenProvider;
import com.example.rbac.security.LoginUser;
import org.springframework.security.authentication.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public Map<String, Object> login(LoginDTO dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
        );
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(loginUser);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", loginUser.getUsername());
        data.put("authorities", loginUser.getAuthorities());
        return data;
    }
}
```

### `AuthController.java`

```java
package com.example.rbac.controller;

import com.example.rbac.common.result.Result;
import com.example.rbac.dto.LoginDTO;
import com.example.rbac.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody @Valid LoginDTO dto) {
        return Result.success(authService.login(dto));
    }
}
```

### `UserService.java`

```java
package com.example.rbac.service;

import com.example.rbac.entity.SysUser;
import com.example.rbac.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<SysUser> list() {
        return userMapper.selectUserList();
    }

    public void create(SysUser user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        userMapper.insertUser(user);
    }

    public void update(SysUser user) {
        userMapper.updateUser(user);
    }

    public void delete(Long id) {
        userMapper.deleteUser(id);
    }
}
```

### `UserController.java`

```java
package com.example.rbac.controller;

import com.example.rbac.common.result.Result;
import com.example.rbac.entity.SysUser;
import com.example.rbac.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user:list') or hasAuthority('ROLE_ADMIN')")
    public Result<?> list() {
        return Result.success(userService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:add')")
    public Result<?> create(@RequestBody SysUser user) {
        userService.create(user);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update')")
    public Result<?> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        userService.update(user);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public Result<?> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }
}
```

Spring Security 里常见做法是把角色和权限都映射为授权项，然后在方法级或接口级通过 `hasAuthority()`、`hasRole()` 这类表达式控制访问。 [blog.tericcabrel](https://blog.tericcabrel.com/role-base-access-control-spring-boot/)

## 初始化数据

建议先插入以下基础数据：

```sql
INSERT INTO sys_role (role_code, role_name, status, deleted) VALUES
('ROLE_ADMIN', '系统管理员', 1, 0),
('ROLE_USER', '普通用户', 1, 0);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, path, method, parent_id, sort_num, status, deleted) VALUES
('user:list', '用户查询', 3, '/api/users', 'GET', 0, 1, 1, 0),
('user:add', '用户新增', 3, '/api/users', 'POST', 0, 2, 1, 0),
('user:update', '用户修改', 3, '/api/users/{id}', 'PUT', 0, 3, 1, 0),
('user:delete', '用户删除', 3, '/api/users/{id}', 'DELETE', 0, 4, 1, 0),
('role:assign', '角色授权', 3, '/api/roles/{id}/permissions', 'POST', 0, 5, 1, 0);

INSERT INTO sys_user (username, password, real_name, status, deleted)
VALUES ('admin', '$2a$10$DowJonesExampleHashReplaceWithRealBCrypt', '管理员', 1, 0);
```

生产级项目里密码通常不会明文存储，而是使用 `BCryptPasswordEncoder` 一类机制进行加密存储，这也是 Spring Security 的常见实践。 [github](https://github.com/markvivv/springboot-security-jwt-example)

## 简历写法

你可以直接写成这样：  
- 基于 Spring Boot 3、Spring Security 6、JWT、MyBatis、MySQL 独立设计并开发用户权限管理系统，实现用户、角色、权限、菜单的 RBAC 权限模型。 [docs.logto](https://docs.logto.io/api-protection/java/spring-boot)
- 设计用户表、角色表、权限表及多张关联表，通过 JWT + 过滤器 + 方法级鉴权实现接口级安全控制。 [katyella](https://katyella.com/blog/spring-boot-security-best-practices/)
- 封装统一返回体、全局异常处理、密码加密、逻辑删除与权限校验机制，提升系统可维护性与安全性。 [geeksforgeeks](https://www.geeksforgeeks.org/springboot/spring-boot-3-0-jwt-authentication-with-spring-security-using-mysql-database/)

## 开发顺序

你照这个顺序落地最稳：  
1. 建库建表并插入初始化角色、权限、管理员数据。  
2. 跑通登录接口和 JWT 生成。  
3. 跑通 JWT 过滤器和 `SecurityContext` 注入。  
4. 完成用户管理 CRUD。  
5. 增加角色管理、角色绑定权限、用户绑定角色。  
6. 最后加分页、日志审计、菜单树和前端管理页面。  
    这种“先认证、后授权、再做业务管理”的顺序，也最符合 Spring Boot 权限系统的实现节奏。 [reddit](https://www.reddit.com/r/SpringBoot/comments/1i9fc1q/best_practices_for_rolebased_access_in_spring/)

下一条我可以继续直接给你：  
- **完整可运行版剩余代码**，包括 `RoleController`、`PermissionController`、DTO/VO、全局异常、角色授权 SQL；  
    或者  
- **前端后台管理页面**，给你做成 Vue + Element Plus 的企业级管理端。

好，我直接给你**继续补全剩余核心代码**，这次目标是让它更接近一个能落地的企业级 RBAC 项目骨架。Spring Security 方法级鉴权通常通过 `@PreAuthorize` 开启，`hasAuthority()` 会精确匹配授权项，而 JWT 过滤器则负责把请求中的 Bearer Token 解析后写入认证上下文。 [docs.spring](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)

## 本次补全范围

这次我补这几块：  
- `Role`、`Permission` 实体与 Mapper。  
- 角色授权、用户分配角色接口。  
- 全局异常处理。  
- 当前登录用户信息接口。  
- 初始化管理员密码生成方式。  
    这些都是企业级权限系统的关键拼图，尤其是“用户-角色-权限”两层关联和方法级权限控制，是 RBAC 实现的核心。 [celerdata](https://celerdata.com/glossary/role-based-access-control-rbac)

## 实体层

### `SysRole.java`

```java
package com.example.rbac.entity;

import java.time.LocalDateTime;

public class SysRole {
    private Long id;
    private String roleCode;
    private String roleName;
    private Integer status;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
```

### `SysPermission.java`

```java
package com.example.rbac.entity;

import java.time.LocalDateTime;

public class SysPermission {
    private Long id;
    private String permissionCode;
    private String permissionName;
    private Integer permissionType;
    private String path;
    private String method;
    private Long parentId;
    private Integer sortNum;
    private Integer status;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public Integer getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(Integer permissionType) {
        this.permissionType = permissionType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getSortNum() {
        return sortNum;
    }

    public void setSortNum(Integer sortNum) {
        this.sortNum = sortNum;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
```

### `RoleAssignDTO.java`

```java
package com.example.rbac.dto;

import java.util.List;

public class RoleAssignDTO {
    private Long userId;
    private List<Long> roleIds;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}
```

### `PermissionAssignDTO.java`

```java
package com.example.rbac.dto;

import java.util.List;

public class PermissionAssignDTO {
    private Long roleId;
    private List<Long> permissionIds;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public List<Long> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<Long> permissionIds) {
        this.permissionIds = permissionIds;
    }
}
```

## 持久层

### `RoleMapper.java`

```java
package com.example.rbac.mapper;

import com.example.rbac.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMapper {
    List<SysRole> selectRoleList();

    int insertRole(SysRole role);

    int deleteRole(@Param("id") Long id);

    int deleteUserRoles(@Param("userId") Long userId);

    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    int deleteRolePermissions(@Param("roleId") Long roleId);

    int insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);
}
```

### `RoleMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.rbac.mapper.RoleMapper">

    <select id="selectRoleList" resultType="com.example.rbac.entity.SysRole">
        select id, role_code as roleCode, role_name as roleName, status, deleted, create_time as createTime, update_time as updateTime
        from sys_role
        where deleted = 0
        order by id desc
    </select>

    <insert id="insertRole" parameterType="com.example.rbac.entity.SysRole" useGeneratedKeys="true" keyProperty="id">
        insert into sys_role (role_code, role_name, status, deleted)
        values (#{roleCode}, #{roleName}, #{status}, 0)
    </insert>

    <update id="deleteRole">
        update sys_role
        set deleted = 1
        where id = #{id} and deleted = 0
    </update>

    <delete id="deleteUserRoles">
        delete from sys_user_role where user_id = #{userId}
    </delete>

    <insert id="insertUserRole">
        insert into sys_user_role (user_id, role_id)
        values (#{userId}, #{roleId})
    </insert>

    <delete id="deleteRolePermissions">
        delete from sys_role_permission where role_id = #{roleId}
    </delete>

    <insert id="insertRolePermission">
        insert into sys_role_permission (role_id, permission_id)
        values (#{roleId}, #{permissionId})
    </insert>
</mapper>
```

MyBatis 的 XML 映射本来就是把 SQL 和 Java 接口解耦的标准方式，而 Mapper 接口也可以直接被 Spring 注入到业务层中使用，这种组织方式很适合企业级后端项目。 [mybatis](https://mybatis.org/mybatis-3/sqlmap-xml.html)

### `PermissionMapper.java`

```java
package com.example.rbac.mapper;

import com.example.rbac.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PermissionMapper {
    List<SysPermission> selectPermissionList();

    int insertPermission(SysPermission permission);
}
```

### `PermissionMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.rbac.mapper.PermissionMapper">

    <select id="selectPermissionList" resultType="com.example.rbac.entity.SysPermission">
        select id,
               permission_code as permissionCode,
               permission_name as permissionName,
               permission_type as permissionType,
               path,
               method,
               parent_id as parentId,
               sort_num as sortNum,
               status,
               deleted,
               create_time as createTime,
               update_time as updateTime
        from sys_permission
        where deleted = 0
        order by sort_num asc, id asc
    </select>

    <insert id="insertPermission" parameterType="com.example.rbac.entity.SysPermission" useGeneratedKeys="true" keyProperty="id">
        insert into sys_permission (
            permission_code, permission_name, permission_type, path, method, parent_id, sort_num, status, deleted
        ) values (
            #{permissionCode}, #{permissionName}, #{permissionType}, #{path}, #{method}, #{parentId}, #{sortNum}, #{status}, 0
        )
    </insert>
</mapper>
```

## 业务层

### `RoleService.java`

```java
package com.example.rbac.service;

import com.example.rbac.dto.PermissionAssignDTO;
import com.example.rbac.dto.RoleAssignDTO;
import com.example.rbac.entity.SysRole;
import com.example.rbac.mapper.RoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {
    private final RoleMapper roleMapper;

    public RoleService(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    public List<SysRole> list() {
        return roleMapper.selectRoleList();
    }

    public void create(SysRole role) {
        role.setStatus(1);
        roleMapper.insertRole(role);
    }

    public void delete(Long id) {
        roleMapper.deleteRole(id);
    }

    @Transactional
    public void assignRoles(RoleAssignDTO dto) {
        roleMapper.deleteUserRoles(dto.getUserId());
        if (dto.getRoleIds() == null || dto.getRoleIds().isEmpty()) {
            return;
        }
        for (Long roleId : dto.getRoleIds()) {
            roleMapper.insertUserRole(dto.getUserId(), roleId);
        }
    }

    @Transactional
    public void assignPermissions(PermissionAssignDTO dto) {
        roleMapper.deleteRolePermissions(dto.getRoleId());
        if (dto.getPermissionIds() == null || dto.getPermissionIds().isEmpty()) {
            return;
        }
        for (Long permissionId : dto.getPermissionIds()) {
            roleMapper.insertRolePermission(dto.getRoleId(), permissionId);
        }
    }
}
```

RBAC 的授权操作本质上就是维护“用户-角色”和“角色-权限”两张关联表，因此授权接口通常会先删旧关联再插入新关联，并使用事务保证一致性。 [ibm](https://www.ibm.com/think/topics/role-based-access-control-implementation)

### `PermissionService.java`

```java
package com.example.rbac.service;

import com.example.rbac.entity.SysPermission;
import com.example.rbac.mapper.PermissionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionService {
    private final PermissionMapper permissionMapper;

    public PermissionService(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    public List<SysPermission> list() {
        return permissionMapper.selectPermissionList();
    }

    public void create(SysPermission permission) {
        permission.setStatus(1);
        if (permission.getParentId() == null) {
            permission.setParentId(0L);
        }
        if (permission.getSortNum() == null) {
            permission.setSortNum(0);
        }
        permissionMapper.insertPermission(permission);
    }
}
```

## 控制层

### `RoleController.java`

```java
package com.example.rbac.controller;

import com.example.rbac.common.result.Result;
import com.example.rbac.dto.PermissionAssignDTO;
import com.example.rbac.dto.RoleAssignDTO;
import com.example.rbac.entity.SysRole;
import com.example.rbac.service.RoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('role:list') or hasAuthority('ROLE_ADMIN')")
    public Result<?> list() {
        return Result.success(roleService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:add')")
    public Result<?> create(@RequestBody SysRole role) {
        roleService.create(role);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public Result<?> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success();
    }

    @PostMapping("/assign-user")
    @PreAuthorize("hasAuthority('user:assign')")
    public Result<?> assignRoles(@RequestBody RoleAssignDTO dto) {
        roleService.assignRoles(dto);
        return Result.success();
    }

    @PostMapping("/assign-permission")
    @PreAuthorize("hasAuthority('role:assign')")
    public Result<?> assignPermissions(@RequestBody PermissionAssignDTO dto) {
        roleService.assignPermissions(dto);
        return Result.success();
    }
}
```

### `PermissionController.java`

```java
package com.example.rbac.controller;

import com.example.rbac.common.result.Result;
import com.example.rbac.entity.SysPermission;
import com.example.rbac.service.PermissionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {
    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('permission:list') or hasAuthority('ROLE_ADMIN')")
    public Result<?> list() {
        return Result.success(permissionService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('permission:add')")
    public Result<?> create(@RequestBody SysPermission permission) {
        permissionService.create(permission);
        return Result.success();
    }
}
```

方法级鉴权是 Spring Security 官方推荐的重要授权手段之一，`@PreAuthorize` 可以直接在接口或服务方法上进行精细控制；而 `hasAuthority()` 比 `hasRole()` 更灵活，因为它不要求自动拼接 `ROLE_` 前缀。 [geeksforgeeks](https://www.geeksforgeeks.org/advance-java/difference-between-hasrole-and-hasauthority-in-spring-security/)

### `AuthController.java` 补充 `me` 接口

```java
package com.example.rbac.controller;

import com.example.rbac.common.result.Result;
import com.example.rbac.dto.LoginDTO;
import com.example.rbac.security.LoginUser;
import com.example.rbac.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody @Valid LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @GetMapping("/me")
    public Result<?> me(Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        Map<String, Object> data = new HashMap<>();
        data.put("id", loginUser.getUser().getId());
        data.put("username", loginUser.getUser().getUsername());
        data.put("realName", loginUser.getUser().getRealName());
        data.put("authorities", loginUser.getAuthorities());
        return Result.success(data);
    }
}
```

## 异常处理

### `BizException.java`

```java
package com.example.rbac.common.exception;

public class BizException extends RuntimeException {
    public BizException(String message) {
        super(message);
    }
}
```

### `GlobalExceptionHandler.java`

```java
package com.example.rbac.common.exception;

import com.example.rbac.common.result.Result;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<?> handleBizException(BizException e) {
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError() == null
                ? "参数校验失败"
                : e.getBindingResult().getFieldError().getDefaultMessage();
        return Result.fail(msg);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDenied(AccessDeniedException e) {
        return new Result<>(403, "无权限访问", null);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        return Result.fail(e.getMessage());
    }
}
```

统一异常处理在企业级后台里很关键，因为它能让参数校验失败、权限不足和业务异常都输出稳定结构，而不是把堆栈直接暴露给前端。 [katyella](https://katyella.com/blog/spring-boot-security-best-practices/)

## 初始化权限数据

你还需要把角色和权限补齐，至少加这些：

```sql
INSERT INTO sys_permission (permission_code, permission_name, permission_type, path, method, parent_id, sort_num, status, deleted) VALUES
('role:list', '角色查询', 3, '/api/roles', 'GET', 0, 10, 1, 0),
('role:add', '角色新增', 3, '/api/roles', 'POST', 0, 11, 1, 0),
('role:delete', '角色删除', 3, '/api/roles/{id}', 'DELETE', 0, 12, 1, 0),
('user:assign', '用户分配角色', 3, '/api/roles/assign-user', 'POST', 0, 13, 1, 0),
('permission:list', '权限查询', 3, '/api/permissions', 'GET', 0, 14, 1, 0),
('permission:add', '权限新增', 3, '/api/permissions', 'POST', 0, 15, 1, 0);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;
```

这段 SQL 的意思是把管理员角色一次性绑定所有权限点，从而快速构建一个可测试的管理员账号环境；RBAC 实际上就是通过这种角色到权限的映射实现统一授权。 [celerdata](https://celerdata.com/glossary/role-based-access-control-rbac)

## 管理员密码生成

因为数据库里需要的是 BCrypt 密文，所以你先临时写一个测试类生成密码：

```java
package com.example.rbac;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode("123456"));
    }
}
```

Spring Security 里常见密码存储方式就是 BCrypt，它适合用户密码这类需要单向安全存储的场景。 [geeksforgeeks](https://www.geeksforgeeks.org/springboot/spring-boot-3-0-jwt-authentication-with-spring-security-using-mysql-database/)

## 接口测试顺序

你按这个顺序测最省事：  
1. 执行建表 SQL 和初始化数据 SQL。  
2. 生成 `admin/123456` 的 BCrypt 密码并回填数据库。  
3. 调 `/api/auth/login` 拿 JWT。  
4. 把 Token 放到 `Authorization: Bearer xxx` 里访问 `/api/auth/me`。  
5. 再测 `/api/users`、`/api/roles`、`/api/permissions`。  
6. 最后测用户分配角色和角色分配权限。  
    JWT 过滤器在整个请求链前面运行，请求只要带着有效 Bearer Token，认证信息就能进入 Spring Security 上下文，后续方法级权限判断才会生效。 [stackoverflow](https://stackoverflow.com/questions/41975045/how-to-design-a-good-jwt-authentication-filter)

## 还差的最后一层

现在这套已经是一个很像样的企业级骨架了，但要真正写进简历更强，建议你再补这 4 个点：  
- 分页查询。  
- 操作日志审计。  
- 菜单树返回。  
- 前端后台管理页面。  
    其中“菜单树 + 权限控制 + 后台管理页面”会让整个项目更像真实中后台系统，而不只是 API 集合。 [baomidou](https://baomidou.com/en/plugins/data-permission/)

下一条我可以继续直接给你 **完整前端管理后台页面设计 + Vue/Element Plus 版本接口对接方案**，或者给你 **把这个项目整理成一份标准简历项目文档**。


那我先把这一版后端骨架在你这儿“定型”了，后面你要扩展就直接在这个基础上迭代就行。接下来我建议分两步往上加：一是给你整理成 **简历/答辩用项目说明**，二是补一套 **Vue + Element Plus 的管理后台设计**。这两个结合起来，基本就是一整套能拿得出手的企业级权限系统。

## 简历/项目文档描述模板

你可以直接按这个结构写在简历或课程设计报告里（改下公司/时间即可）：

**用户权限管理系统（Spring Boot + JWT + RBAC）**  
- 独立设计并实现基于 RBAC 的用户权限管理系统，支持用户、角色、权限点和菜单的统一管理，覆盖用户认证、角色授权、接口级权限控制等完整链路。  
- 技术栈：Spring Boot 3、Spring Security 6、JWT、MyBatis、MySQL，采用分层架构（Controller/Service/Mapper），并通过统一返回体与全局异常处理提升可维护性和扩展性。 [katyella](https://katyella.com/blog/spring-boot-security-best-practices/)
- 数据层设计了 `sys_user`、`sys_role`、`sys_permission` 及多张关联表，支持用户-角色、角色-权限多对多关系，实现灵活的权限组合和按角色分配权限。 [celerdata](https://celerdata.com/glossary/role-based-access-control-rbac)
- 认证层采用 JWT 无状态认证，基于 `OncePerRequestFilter` 解析 `Authorization: Bearer` Token，将用户身份与权限写入 `SecurityContext`，再通过 `@PreAuthorize` + `hasAuthority()` 实现方法级访问控制。 [docs.logto](https://docs.logto.io/api-protection/java/spring-boot)
- 实现用户管理、角色管理、权限管理、角色授权、用户分配角色等核心后台能力，支持管理员快速对接前端管理端进行可视化配置与权限下发。 [ibm](https://www.ibm.com/think/topics/role-based-access-control-implementation)

这一段基本可以原封不动放进 Java 后端简历的“核心项目”里，用词也偏企业风格。

## Vue + Element Plus 管理后台设计（概要）

前端建议用：**Vue 3 + Vite + TypeScript + Element Plus + Axios**。这套组合是目前中后台项目中非常常见的一种技术栈。Element Plus 自身就提供了表格、表单、对话框、分页组件，非常适合做 RBAC 管理页面。 [ibm](https://www.ibm.com/think/topics/role-based-access-control-implementation)

页面模块建议做成：  
- 登录页：调用 `/api/auth/login`，Token 存在内存或 `localStorage`，Axios 请求头自动带 `Authorization`。  
- 布局页：上方头部 + 左侧菜单 + 右侧主内容。菜单数据由 `/api/permissions` 返回菜单类型权限组装成树状结构。  
- 用户管理页：表格展示用户列表，支持新增、编辑、禁用、删除，调用 `/api/users` 系列接口。  
- 角色管理页：表格展示角色列表，支持新增、删除，弹窗里用树形勾选权限，调用 `/api/roles/assign-permission`。  
- 权限管理页：表格展示权限点，支持新增按钮/接口权限，调用 `/api/permissions`。  

这种“前端菜单由后端权限表驱动”的设计，在很多实际 RBAC 系统中是常见模式，能做到前端菜单和后端权限统一配置和管理。 [ibm](https://www.ibm.com/think/topics/role-based-access-control-implementation)

如果你接下来想继续，我可以帮你：  
- 分步写出 Vue + Element Plus 版的前端骨架（目录结构、路由、Axios 拦截器）。  
- 或者按你学校课程设计/答辩的格式，把“系统分析、概要设计、详细设计、测试用例”四大部分完整写一稿，你再改成自己的语言。  

你更想先要哪一块：前端管理后台的代码骨架，还是课程设计/答辩用的完整文档稿？  

找工作用的话，重点就不是“把项目做多复杂”，而是“**怎么把这个 RBAC 项目写得很专业、很像真实公司项目**，而且和 Java 后端 JD 强相关”。我给你一套直接可用的简历定位方案和话术，你按这套包装就行。 [resumeworded](https://resumeworded.com/backend-developer-resume-example)

## 放在简历里的位置和名字

建议你在简历中专门有一块 “**核心项目** / Projects” 区域，把这个项目放第一位（对你这种应届/准应届很合理）。项目名称不要写“练手项目”，写成偏企业风一点的名字，比如：  
- 用户权限管理与后台管理系统（Spring Boot + JWT + RBAC）  
- 企业级用户认证与权限控制系统（Java 后端）  

项目名里带上 **Spring Boot / JWT / RBAC / MySQL** 这种 JD 关键词，有利于 ATS 扫描和 HR 快速识别你做过“权限 / 安全 / 后台管理”这类硬货。 [scribd](https://www.scribd.com/document/910261994/Mina-Essam-backend-cv)

## 一段式项目描述模板（你可直接用）

建议你按“**一句话总览 + 3–5 条要点**”来写，每条都是“动作 + 技术 + 结果”的风格，这是很多后端简历模板推荐的写法。 [airesume](https://airesume.guru/blog/personal-projects-on-resume-how-to-showcase-what-youve-built-with-examples)

你可以直接抄这一版（按你实际改一点即可）：

**用户权限管理与后台管理系统 | Spring Boot, Spring Security, JWT, MyBatis, MySQL**  
- 独立设计并实现基于 RBAC 的用户权限管理系统，支撑用户、角色、权限点的统一管理，覆盖登录认证、角色授权、接口级权限控制等完整安全链路。 [celerdata](https://celerdata.com/glossary/role-based-access-control-rbac)
- 采用 Spring Boot 3 + Spring Security 6 + JWT 构建无状态认证体系，自定义过滤器解析 Bearer Token 并写入 SecurityContext，通过 `@PreAuthorize` + `hasAuthority` 精细控制 API 访问权限。 [docs.logto](https://docs.logto.io/api-protection/java/spring-boot)
- 使用 MyBatis + MySQL 建模 `sys_user`、`sys_role`、`sys_permission` 及关联表，支持用户-角色、角色-权限多对多关系，并结合逻辑删除和状态字段实现可审计、可扩展的数据层设计。 [stackoverflow](https://stackoverflow.com/questions/190257/best-role-based-access-control-rbac-database-model)
- 封装统一返回体与全局异常处理（参数校验、业务异常、权限不足），提供稳定的接口返回结构，便于前端集成和线上问题定位。 [katyella](https://katyella.com/blog/spring-boot-security-best-practices/)
- 设计用户管理、角色管理、权限管理、角色授权、用户分配角色等后台模块，为后续 Vue 管理端提供 RESTful API 支撑，具备扩展菜单树与前后端分离中后台的能力。 [github](https://github.com/IMS94/spring-boot-jwt-authorization)

如果你简历一页空间紧张，可以压缩成 3 条；如果空间充足，可以把“性能、日志、分页”再各加一条。

## 如何在面试里讲这个项目

面试官听你讲项目，一般会顺着这几个问题来问：  
1. 你这个权限系统是按什么模型设计的？  
2. 用户、角色、权限三张表怎么建？为什么这么建？  
3. Spring Security + JWT 这块，你怎么接的？  
4. 你有哪些地方是自己设计的，而不是照网上抄？  

你可以用下面这套结构来回答——简单、有逻辑：  

- 场景：  
  “我这个项目模拟的是公司内部中后台系统，需要对不同职位、不同部门的人做权限隔离，所以我用的是标准的 RBAC 模型：用户—角色—权限三层。” [ibm](https://www.ibm.com/think/topics/role-based-access-control-implementation)

- 数据模型：  
  “用户表只关心个人信息，角色表抽象的是岗位/身份，权限点表抽象的是系统里的操作，比如 `user:list`、`user:add` 这种动作。用户通过中间表绑定角色，角色再通过中间表绑定权限，这样后期角色和权限的组合调整，不用改用户记录。” [celerdata](https://celerdata.com/glossary/role-based-access-control-rbac)

- 技术实现：  
  “认证部分我用的是 Spring Security 6 + JWT，无状态登录。用户通过 `/api/auth/login` 登录后，后端校验用户名密码，用 `jjwt` 生成带过期时间的 Token 返回前端。之后所有请求都带 `Authorization: Bearer token`，我在自定义的 `OncePerRequestFilter` 里解析 Token、加载用户详情，写到 `SecurityContext` 里。” [katyella](https://katyella.com/blog/spring-boot-security-best-practices/)

- 鉴权控制：  
  “授权部分我用方法级的 `@PreAuthorize`，比如查询用户列表的接口要求 `hasAuthority('user:list')` 或 `ROLE_ADMIN`。权限字符串的来源是在 `UserDetailsService` 加载用户时，把它的角色码和权限码都组装成 `GrantedAuthority`，后面 Spring Security 的表达式引擎会基于这些授权项做判断。” [docs.spring](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)

- 工程亮点：  
  “我把统一返回体、全局异常处理、逻辑删除、密码加密、权限不足的统一输出都做了，这样整个项目风格和真实公司里的中后台很接近，也方便后面对接前端和扩展多模块。” [resumeworded](https://resumeworded.com/backend-developer-resume-example)

用这套话术，你在面试里会显得是“理解了权限系统原理和工程实践的人”，而不是“跟着教程打一遍代码”的人。

## 项目在求职中的定位

结合现在的后端岗位简历建议：  
- 你不需要堆 7、8 个项目，**2–3 个高质量项目** 就够，其中一个就是这个 RBAC 权限系统。 [indeed](https://www.indeed.com/career-advice/resumes-cover-letters/listing-projects-on-resume)
- 这个项目最好放在**简历前两个项目**里，标签明确写清：`Spring Boot / JWT / RBAC / MyBatis / MySQL`。  
- 简历上同时写明你还有“博客系统 / 留言板 / 学生成绩管理 / 贪吃蛇”等项目，但可以简略带过，把精力集中在这个权限系统 + 一个业务项目（比如博客/商城/订单）上，形成“一个通用业务项目 + 一个安全/权限项目”的组合。 [devsdata](https://devsdata.com/resumes/java/spring-boot-developer-resume-sample/)
