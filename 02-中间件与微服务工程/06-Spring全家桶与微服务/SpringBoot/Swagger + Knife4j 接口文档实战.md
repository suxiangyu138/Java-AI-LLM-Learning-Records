# Swagger + Knife4j 接口文档自动生成

## 为什么需要

手写接口文档的问题：不及时更新、容易出错、前后端联调反复对不齐。Swagger 从代码注解自动生成在线文档 + 在线调试页面。

## 引入依赖

```xml
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-openapi3-spring-boot-starter</artifactId>
    <version>4.4.0</version>
</dependency>
```

Knife4j 是 Swagger 的增强 UI，比原版 Swagger UI 更好看更好用。

## 基本配置

```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs

knife4j:
  enable: true
  setting:
    language: zh_cn
```

```java
@Configuration
public class Knife4jConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("订单系统 API")
                        .version("1.0")
                        .description("订单系统接口文档"));
    }
}
```

## 常用注解

```java
@Tag(name = "用户管理")                    // Controller 分组
@RestController
public class UserController {

    @Operation(summary = "创建用户")        // 接口说明
    @PostMapping("/users")
    public Result<User> create(
            @Parameter(description = "用户信息")  // 参数说明
            @Valid @RequestBody UserCreateDTO dto) {
        return Result.ok(userService.create(dto));
    }
}
```

实体类注解：
```java
@Data
@Schema(description = "用户创建请求")
public class UserCreateDTO {
    @Schema(description = "用户名", example = "zhangsan")
    @NotBlank
    private String username;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    @Email
    private String email;

    @Schema(description = "角色", allowableValues = {"ADMIN", "USER"})
    private String role;
}
```

## 开启鉴权

如果接口需要 Token，在配置中加入：

```java
@Bean
public OpenAPI openAPI() {
    return new OpenAPI()
            .addSecurityItem(new SecurityRequirement().addList("Bearer"))
            .components(new Components()
                    .addSecuritySchemes("Bearer", new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")))
            .info(new Info().title("订单系统 API").version("1.0"));
}
```

## 访问地址

- Knife4j UI：`http://localhost:8080/doc.html`
- 原生 Swagger UI：`http://localhost:8080/swagger-ui.html`
- API JSON：`http://localhost:8080/v3/api-docs`

## 生产环境

生产环境应关闭 Swagger 文档暴露：

```yaml
springdoc:
  api-docs:
    enabled: false       # 关闭 JSON API 文档
  swagger-ui:
    enabled: false       # 关闭 UI 页面
```

或用 `@Profile("dev")` 只在开发环境注册配置类：

```java
@Configuration
@Profile({"dev", "test"})
public class Knife4jConfig { }
```

## 实战建议

- **每个接口都加 `@Operation(summary = "...")`**，一句话说清楚这个接口干什么
- **DTO 字段加 `@Schema(description = "...")` 和 `example`**，比写文档省力
- **生产环境一定要关掉**，暴露接口文档 = 暴露攻击面
- Knife4j 的离线文档导出功能：`doc.html` 页面右上角可以导出 Markdown/Word
