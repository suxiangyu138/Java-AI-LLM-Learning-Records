# SpringBoot Web 应用支持详解

> **定位**：`spring-boot-starter-web` 核心依赖，内置 Tomcat + Spring MVC + Jackson，零配置启动 Web 应用。

---

## 目录

1. [核心前提](#1-核心前提)
2. [核心组件](#2-核心组件)
3. [常用功能](#3-常用功能)
4. [实战配置](#4-实战配置)

---

## 1. 核心前提

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

**自动配置**：`DispatcherServlet` → `HandlerMapping` → `HandlerAdapter` + 内嵌 Tomcat（8080）+ Jackson JSON。

---

## 2. 核心组件

### 2.1 Controller 注解速查

| 注解 | 用途 |
|------|------|
| `@RestController` | `@Controller` + `@ResponseBody`，返回 JSON |
| `@RequestMapping` | 类/方法路径映射 |
| `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` | 简化映射 |
| `@PathVariable` | URL 路径参数 `/api/user/{id}` |
| `@RequestParam` | 查询参数 `?name=张三` |
| `@RequestBody` | POST 请求体 JSON |

### 2.2 嵌入式服务器

```properties
server.port=8081
server.tomcat.uri-encoding=UTF-8
server.tomcat.max-connections=1000
```

> 可替换为 Jetty/Undertow（排除 Tomcat + 引入对应 starter）。

### 2.3 静态资源目录

| 优先级 | 目录 |
|:------:|------|
| 1 | `src/main/resources/static/`（推荐） |
| 2 | `src/main/resources/public/` |
| 3 | `src/main/resources/resources/` |

---

## 3. 常用功能

### 统一异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        return new Result(500, "服务器异常：" + e.getMessage(), null);
    }
}
```

### 跨域配置

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("*")
                .allowCredentials(true);
    }
}
```

---

## 4. 实战配置

```properties
server.port=8081
server.servlet.context-path=/api
spring.jackson.date-format=yyyy-MM-dd HH:mm:ss
spring.jackson.time-zone=GMT+8
spring.jackson.default-property-inclusion=non_null
logging.level.com.example.controller=DEBUG
```

### 常见问题

| 问题 | 解决 |
|------|------|
| JSON 中文乱码 | `server.tomcat.uri-encoding=UTF-8` |
| 静态资源 404 | 检查是否放在 `static/` 目录、路径不冲突 |
| 跨域不生效 | `@Configuration` + `allowedOrigins` 正确 |
| `@RequestBody` 参数失败 | 实体类必须有 getter/setter |
