# SpringBoot 安全管理（入门实战，避坑版）

> **定位**：Spring Security 实现认证（Authentication）+ 授权（Authorization）。核心：配置类 + PasswordEncoder + UserDetailsService。

---

## 目录

1. [核心认知](#1-核心认知)
2. [基础配置](#2-基础配置)
3. [数据库认证](#3-数据库认证)
4. [避坑指南](#4-避坑指南)

---

## 1. 核心认知

| 概念 | 说明 |
|------|------|
| **认证** | 验证用户身份（账号密码是否正确） |
| **授权** | 验证用户权限（能否访问 `/admin` 接口） |

### 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

> 引入后默认拦截所有请求，默认用户名 `user`，密码在控制台打印。

---

## 2. 基础配置

### 2.1 安全配置类（2.7.x）

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();        // ⚠️ 必须配置
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()               // 内存用户
                .withUser("user").password(passwordEncoder().encode("123456")).roles("USER")
                .and()
                .withUser("admin").password(passwordEncoder().encode("admin123")).roles("ADMIN");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()                       // 入门可关闭
            .authorizeRequests()
                .antMatchers("/login", "/static/**").permitAll()
                .antMatchers("/admin/**").hasRole("ADMIN")
                .antMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            .and().formLogin()
                .defaultSuccessUrl("/index")
            .and().logout().logoutSuccessUrl("/login");
    }
}
```

### 2.2 授权规则

| 方法 | 说明 |
|------|------|
| `permitAll()` | 放行，无需登录 |
| `hasRole("ADMIN")` | 仅指定角色 |
| `hasAnyRole("USER","ADMIN")` | 多角色均可 |
| `authenticated()` | 需要登录 |

### 2.3 测试

```java
@RestController
public class SecurityController {
    @GetMapping("/user/info")
    public String userInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return "当前用户：" + auth.getName();
    }

    @GetMapping("/admin/info")
    public String adminInfo() { /* 仅 ADMIN 可访问 */ }
}
```

---

## 3. 数据库认证

### Step 1：新增角色字段

```sql
ALTER TABLE user ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
```

### Step 2：实现 UserDetailsService

```java
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    private UserXmlMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectByUsername(username);
        if (user == null) throw new UsernameNotFoundException("用户名不存在");
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getName())
                .password(user.getPassword())   // 数据库存加密后的密码
                .roles(user.getRole())          // ⚠️ 不加 ROLE_ 前缀
                .build();
    }
}
```

### Step 3：替换内存用户

```java
@Override
protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.userDetailsService(userDetailsService()).passwordEncoder(passwordEncoder());
}
```

---

## 4. 避坑指南

| 问题 | 解决 |
|------|------|
| `There is no PasswordEncoder mapped` | 配置 `BCryptPasswordEncoder` + 密码必须加密 |
| 用户名存在但提示"不存在" | 检查 SQL + 用户名大小写 |
| 403 Forbidden | 检查角色匹配（不加 `ROLE_` 前缀） + `antMatchers` 路径 |
| 自定义登录页不生效 | 确保 `loginPage("/login")` + `/login` 接口放行 |
