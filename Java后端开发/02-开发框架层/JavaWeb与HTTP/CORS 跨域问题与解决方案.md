# CORS 跨域问题与解决方案

## 什么是跨域

浏览器同源策略：协议、域名、端口三者必须一致。任一不同即"跨域"。

```
http://localhost:8080  →  http://localhost:3000  跨域（端口不同）
http://example.com     →  http://api.example.com  跨域（子域名不同）
http://example.com     →  https://example.com     跨域（协议不同）
```

## CORS 响应头

服务器通过响应头告诉浏览器"允许跨域"：

| 响应头 | 含义 |
|--------|------|
| `Access-Control-Allow-Origin` | 允许的来源域名 |
| `Access-Control-Allow-Methods` | 允许的 HTTP 方法 |
| `Access-Control-Allow-Headers` | 允许的请求头 |
| `Access-Control-Allow-Credentials` | 是否允许携带 Cookie |
| `Access-Control-Max-Age` | 预检请求缓存时间（秒） |
| `Access-Control-Expose-Headers` | 允许前端 JS 读取的响应头 |

## 两种请求

**简单请求**（不触发 OPTIONS 预检）：
- 方法：GET、HEAD、POST
- Content-Type 仅限：`text/plain`、`multipart/form-data`、`application/x-www-form-urlencoded`
- 无自定义请求头

**非简单请求**（触发 OPTIONS 预检）：
- 方法为 PUT、DELETE、PATCH
- Content-Type 为 `application/json`
- 带有自定义请求头（如 `Authorization`、`X-Token`）

RESTful API 基本都是非简单请求，所以每次跨域请求前会先发一个 OPTIONS 请求。

## Spring Boot 解决方案

### 方案一：注解（单个 Controller）

```java
@RestController
@CrossOrigin(origins = "http://localhost:3000",
             allowCredentials = "true",
             maxAge = 3600)
public class UserController { }
```

### 方案二：全局配置（推荐）

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")        // 生产环境指定具体域名
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

### 方案三：CorsFilter（兼容 Spring Security）

```java
@Configuration
public class CorsFilterConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

## Spring Security 集成注意

Spring Security 开启后会先生效，需要把 CORS 配置在 Security 之前：

```java
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors().and()  // 使用已注册的 CorsFilter
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        return http.build();
    }
}
```

## 生产环境注意事项

- **不要用 `allowedOrigins("*")` + `allowCredentials(true)` 同时出现**：浏览器会拒绝。要么用 `allowedOriginPatterns("*")`，要么指定具体域名。
- **生产环境指定具体允许的域名**，不要直接 `*`
- **OPTIONS 请求不要做认证拦截**，预检请求不会携带 Authorization
- **Nginx 层也可以配 CORS**，避免重复配置
