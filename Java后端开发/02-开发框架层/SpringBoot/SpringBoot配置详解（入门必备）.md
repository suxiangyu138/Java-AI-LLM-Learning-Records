# Spring Boot 配置详解（入门必备）

> **文档定位**：Java 后端企业级技术文档 | Spring Boot 配置详解  
> **核心配置格式**：Properties（`.properties`）、YAML（`.yml`）  
> **前置基础**：Spring Boot 开发入门

---

## 一、核心概念

### 1.1 配置文件类型

| 格式 | 文件名 | 特点 | 适用场景 |
|------|--------|------|----------|
| **Properties** | `application.properties` | `key=value` 格式，语法简单但层级不清晰 | 简单项目、配置项较少 |
| **YAML（推荐）** | `application.yml` / `application.yaml` | 缩进表示层级，可读性强，支持列表/对象 | 中大型项目，推荐 |
| JSON | `application.json` | 格式严格 | 极少使用 |

### 1.2 配置文件位置与优先级

Spring Boot 自动扫描以下位置（高优先级覆盖低优先级）：

1. `./config/application.yml`（项目根目录 config 文件夹，推荐生产环境）
2. `src/main/resources/application.yml`（默认位置）

> 文件名必须为 `application`；若同时存在 `.properties` 和 `.yml`，`.properties` 优先级更高。

### 1.3 配置优先级总览

```
命令行参数 > 环境变量 > application.properties > application.yml > 默认值
```

---

## 二、底层原理

### 2.1 配置绑定机制

Spring Boot 通过 `PropertySource` 读取配置文件，使用 `Binder` 将配置值绑定到 `@Value` 或 `@ConfigurationProperties` 标注的字段上。

- `@Value("${key}")`：单个配置项注入，适合简单场景
- `@ConfigurationProperties(prefix = "xxx")`：批量注入，适合一组相关配置

### 2.2 约定优于配置

Spring Boot 为所有配置项提供了合理的默认值，开发者只需覆盖需要自定义的配置。

---

## 三、代码实现

### 3.1 服务器配置

```yaml
# application.yml
server:
  port: 8081                          # 修改默认端口
  servlet:
    context-path: /api               # 应用路径前缀
  tomcat:
    uri-encoding: UTF-8              # 编码设置
```

```properties
# application.properties
server.port=8081
server.servlet.context-path=/api
server.tomcat.uri-encoding=UTF-8
```

### 3.2 日志配置

```yaml
# application.yml
logging:
  level:
    root: INFO                       # 全局日志级别
    com.example.springboot.controller: DEBUG  # 指定包级别
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{50} - %msg%n"
  file:
    name: D:/logs/springboot-log.log  # 日志文件路径
```

| 日志级别 | 用途 |
|----------|------|
| DEBUG | 开发调试 |
| INFO | 正常运行日志（默认） |
| WARN | 警告信息 |
| ERROR | 错误日志 |

### 3.3 自定义配置

#### 方式 1：`@Value`（单个配置项）

```yaml
# application.yml
api:
  key: springboot2024
thirdparty:
  api:
    url: https://api.example.com
```

```java
@RestController
public class ConfigController {

    @Value("${api.key}")
    private String apiKey;

    @Value("${thirdparty.api.url}")
    private String thirdPartyUrl;

    @RequestMapping("/getConfig")
    public String getConfig() {
        return "接口密钥：" + apiKey + "，第三方地址：" + thirdPartyUrl;
    }
}
```

#### 方式 2：`@ConfigurationProperties`（推荐，批量配置）

```yaml
# application.yml
app:
  max:
    page:
      size: 10

cors:
  allowed-origins:
    - http://localhost:8080
    - http://127.0.0.1:8080
```

```java
/**
 * 应用自定义配置类 —— @ConfigurationProperties 实现批量配置注入。
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    /** 属性名与配置文件 key 一致（max.page.size → maxPageSize，自动驼峰映射） */
    private Integer maxPageSize;

    // 必须提供 getter/setter
    public Integer getMaxPageSize() { return maxPageSize; }
    public void setMaxPageSize(Integer maxPageSize) { this.maxPageSize = maxPageSize; }
}
```

```java
/** 启动类开启配置绑定 */
@SpringBootApplication
@EnableConfigurationProperties(AppConfig.class)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3.4 多环境配置

```yaml
# application.yml（主配置，指定激活哪个环境）
spring:
  profiles:
    active: dev                      # 激活 dev 环境

---
# application-dev.yml（开发环境）
server:
  port: 8080
logging:
  level:
    root: DEBUG

---
# application-prod.yml（生产环境）
server:
  port: 80
logging:
  level:
    root: WARN
```

---

## 四、实战要点

### 4.1 `@Value` vs `@ConfigurationProperties`

| 维度 | `@Value` | `@ConfigurationProperties` |
|------|----------|---------------------------|
| 适用场景 | 单个/少量配置项 | 一组相关配置 |
| 类型安全 | 需手动转换 | 自动类型映射 |
| 复杂结构 | 不支持列表/对象 | 支持列表/对象（YAML） |
| 推荐度 | 简单场景 | **推荐** |

### 4.2 高级配置技巧

- **占位符**：`${server.port:8080}`（冒号后是默认值）
- **外部配置**：`java -jar app.jar --server.port=9090`（命令行覆盖）
- **加密敏感配置**：使用 Jasypt（`jasypt-spring-boot`）加密数据库密码
- **配置提示**：添加 `spring-boot-configuration-processor` 依赖，IDE 中自动提示自定义配置

---

## 五、避坑总结

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| **中文乱码** | 编码配置未设置 | `File Encodings` 全设 UTF-8；`server.tomcat.uri-encoding=UTF-8` |
| **配置文件不生效** | 文件命名不符合规范 | 必须命名为 `application.properties` 或 `application.yml` |
| **`@Value` 取不到值** | 缺少 `@Component` 或不在 Spring 管理下 | 确保类被 Spring 扫描 |
| **`@ConfigurationProperties` 不生效** | 缺少 setter 方法 或 `@EnableConfigurationProperties` | 检查 getter/setter |
| **properties vs yml 冲突** | 同项目两种格式都存在 | 统一使用一种格式 |

---

## 六、企业级最佳实践

### 6.1 配置管理规范

| 规范 | 说明 |
|------|------|
| **多环境隔离** | 开发/测试/生产用不同配置文件 |
| **敏感信息加密** | 数据库密码、API 密钥使用 Jasypt 加密 |
| **配置中心** | 生产环境使用 Nacos/Apollo 实现动态配置 |
| **统一 YAML** | 推荐统一使用 `.yml` 格式，层级清晰 |

### 6.2 配置文件模板

```yaml
# application.yml — 通用配置
spring:
  application:
    name: my-service
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}  # 环境变量 > 默认 dev

server:
  port: ${SERVER_PORT:8080}

logging:
  level:
    root: INFO
    com.example: DEBUG
```
