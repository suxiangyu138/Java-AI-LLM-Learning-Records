# 快速学会 SpringSecurity
## 一、核心定位
SpringSecurity 是 **Spring 生态安全框架**
核心两大功能：
1. **认证**：你是谁（登录、账号密码、Token）
2. **授权**：你能干什么（角色、权限、接口访问控制）
额外能力：
防跨域、防CSRF、防XSS、会话管理、密码加密、注销、RememberMe、OAuth2、JWT

---

## 二、底层核心流程
1. 用户提交账号密码
2. **认证管理器**校验身份
3. 认证成功 → 存入上下文 `SecurityContext`
4. 访问接口 → 拦截器校验**权限/角色**
5. 无权限拦截、拒绝访问

---

## 三、快速整合 SpringBoot
### 1. 引入依赖
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```
**引入后直接生效**：
- 所有接口默认拦截
- 自动生成默认密码，控制台打印
- 默认登录页：`/login`

---

## 四、基础配置：内存用户（快速测试）
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 关闭csrf（前后端分离必关）
            .csrf().disable()
            .authorizeRequests()
            // 放行接口
            .antMatchers("/login").permitAll()
            // 其余全部需要认证
            .anyRequest().authenticated();

        return http.build();
    }

    // 内存中配置账号密码
    @Bean
    public UserDetailsService userDetailsService(){
        UserDetails user = User
                .withUsername("admin")
                .password(passwordEncoder().encode("123456"))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    // 密码加密器
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
```

---

## 五、核心注解（开发必用）
### 1. 开启权限控制
```java
// 开启注解授权
@EnableGlobalMethodSecurity(prePostEnabled = true)
```

### 2. 接口粒度权限控制
```java
// 需要 ADMIN 角色
@PreAuthorize("hasRole('ADMIN')")

// 需要指定权限
@PreAuthorize("hasPermission('system:user:list')")

// 登录即可访问
@PreAuthorize("isAuthenticated()")
```

---

## 六、核心概念
### 1. 核心对象
- `UserDetails`：用户信息封装（账号、密码、角色、权限）
- `UserDetailsService`：加载用户数据（数据库查用户）
- `PasswordEncoder`：密码加密（**BCrypt 主流**）
- `SecurityContext`：当前登录用户全局上下文

### 2. 认证方式
- 内存用户（测试用）
- 数据库查询用户（生产常用）
- JWT / Token 无状态认证（前后端分离）

### 3. 授权方式
- 基于**角色**：ROLE_ADMIN、ROLE_USER
- 基于**权限**：system:user:add、system:user:list

---

## 七、前后端分离必备配置
1. 关闭 CSRF
```java
http.csrf().disable();
```
2. 放行跨域
3. 关闭默认登录页，使用自定义 JSON 登录
4. 自定义认证失败、未授权、未登录返回 JSON

---

## 八、获取当前登录用户
```java
// 方式1
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
String username = authentication.getName();

// 方式2 注入
@Autowired
private SecurityContextHolderStrategy contextHolder;
```

---

## 九、企业标准架构
1. 自定义 `UserDetailsService` → 从 MySQL 查询用户+角色+权限
2. 密码 BCrypt 加密存储
3. 整合 JWT，登录下发 Token
4. 自定义 JWT 拦截器，替换 Session 机制
5. 注解 + 配置类 双层权限控制

---

## 十、和 Shiro 对比（面试常问）
1. SpringSecurity：Spring 原生、功能强、整合好、偏重
2. Shiro：轻量、简单、上手快、社区维护弱
**现在企业主流：SpringSecurity + JWT**

---

## 十一、极简总结
1. 核心：**认证 + 授权**
2. 核心类：`SecurityConfig`、`UserDetailsService`、`PasswordEncoder`
3. 关键操作：关闭CSRF、配置放行路径、加密密码、角色权限
4. 拓展：无缝整合 JWT 实现无状态登录
