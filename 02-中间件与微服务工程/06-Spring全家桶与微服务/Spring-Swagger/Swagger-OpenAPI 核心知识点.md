# Swagger / OpenAPI 核心知识点

## 一、概述

Swagger 是最主流的 API 文档规范工具链，OpenAPI 是其标准化规范（OpenAPI Specification, OAS）。在 Java 生态中，SpringDoc OpenAPI 已取代 SpringFox 成为 Spring Boot 集成 Swagger 的标准方案。

**核心定位：** API 文档自动生成 + 交互式调试界面，RESTful API 的标配工具。

**官网：** https://springdoc.org

**概念关系：**
```
OpenAPI 规范（标准协议）
  └── Swagger 工具链（实现）
       ├── Swagger UI（交互式文档页面）
       ├── Swagger Editor（在线编辑 OAS 文档）
       └── Swagger Codegen（基于 OAS 生成客户端/服务端代码）
```

## 二、SpringDoc OpenAPI 集成

### 2.1 依赖配置

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
```

**零配置：** 添加依赖后，Spring Boot 应用自动生成 API 文档。

### 2.2 访问地址

| 地址 | 说明 |
|------|------|
| `http://localhost:8080/swagger-ui.html` | Swagger UI 交互页面 |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON 格式文档 |
| `http://localhost:8080/v3/api-docs.yaml` | OpenAPI YAML 格式文档 |

### 2.3 基础配置

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("用户管理服务 API")
                .version("1.0.0")
                .description("提供用户注册、登录、信息管理等功能")
                .contact(new Contact()
                    .name("开发团队")
                    .email("dev@example.com")))
            .addSecurityItem(new SecurityRequirement().addList("JWT"))
            .components(new Components()
                .addSecuritySchemes("JWT", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
```

## 三、常用注解

### 3.1 Controller 层

```java
@RestController
@Tag(name = "用户管理", description = "用户注册、登录、信息管理接口")
@RequestMapping("/api/users")
public class UserController {

    @Operation(summary = "创建用户", description = "注册新用户账号")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败")
    })
    @PostMapping
    public UserDTO createUser(
        @RequestBody @Valid UserCreateRequest request) {
        return userService.create(request);
    }

    @Operation(summary = "根据ID查询用户")
    @GetMapping("/{id}")
    public UserDTO getUser(
        @Parameter(description = "用户ID", example = "1")
        @PathVariable Long id) {
        return userService.findById(id);
    }
}
```

### 3.2 DTO 层

```java
@Schema(description = "用户创建请求")
public class UserCreateRequest {

    @Schema(description = "用户名", example = "zhangsan", required = true)
    @NotBlank
    private String username;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    @Email
    private String email;

    @Schema(description = "年龄", example = "25", minimum = "1", maximum = "150")
    @Min(1) @Max(150)
    private Integer age;
}
```

### 3.3 注解速查表

| 位置 | 注解 | 说明 |
|------|------|------|
| Controller | `@Tag` | API 分组标签 |
| 方法 | `@Operation` | 接口描述 |
| 方法 | `@ApiResponses` + `@ApiResponse` | 响应说明 |
| 参数 | `@Parameter` | 参数描述 |
| 实体 | `@Schema` | 模型描述 |
| 字段 | `@Schema` | 字段描述/示例/约束 |
| 接口 | `@Hidden` | 隐藏接口/字段 |

## 四、高级功能

### 4.1 分组

```properties
# application.yml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
  group-configs:
    - group: user-api
      paths-to-match: /api/users/**
    - group: order-api
      paths-to-match: /api/orders/**
```

### 4.2 环境控制

```java
// 生产环境禁用 Swagger
@Profile({"dev", "staging"})
@Configuration
public class OpenApiConfig {
    // ...
}
```

### 4.3 全局 Header

```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .components(new Components()
            .addParameters("X-Trace-Id", new Parameter()
                .in(ParameterIn.HEADER.toString())
                .name("X-Trace-Id")
                .description("链路追踪ID")
                .schema(new StringSchema())));
}
```

## 五、AI 时代的 API 工具

| 工具 | 定位 |
|------|------|
| **Swagger UI** | 传统交互式文档 |
| **Postman** | API 调试 + 测试 + AI 生成脚本 |
| **Apifox** | 国内一体化（Swagger + Postman + Mock） |

## 六、SpringDoc vs SpringFox

| 维度 | SpringDoc OpenAPI | SpringFox |
|------|-------------------|-----------|
| Spring Boot 3.x | **完全支持** | 不支持 |
| 维护状态 | **活跃** | 2020 年停止更新 |
| OpenAPI 规范 | 3.0 / 3.1 | 2.0 / 3.0（不完整） |
| 社区活跃度 | **高** | 已停更 |
| 推荐 | **当前标准** | 遗留项目 |

## 七、总结

SpringDoc OpenAPI 是 2026 年 Spring Boot 项目的标准 API 文档方案。核心价值在于**代码即文档**——写好 Controller 和 DTO 注解，Swagger UI 自动生成可交互的 API 文档，并与 Postman/Apifox 等工具链互通。
