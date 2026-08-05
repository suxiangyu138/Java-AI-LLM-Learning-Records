# 02 - Spring Boot 配置体系

> 定位：application.yml、类型安全绑定、Profile 环境隔离、配置优先级、配置刷新——Spring Boot 配置全解

## 📚 目录

1. [配置文件与语法](#1-配置文件与语法)
2. [配置绑定：@ConfigurationProperties](#2-配置绑定configurationproperties)
3. [Profile 环境隔离](#3-profile-环境隔离)
4. [配置优先级](#4-配置优先级)
5. [配置刷新与配置中心](#5-配置刷新与配置中心)

---

## 1. 配置文件与语法

### 1.1 application.yml

```yaml
# src/main/resources/application.yml（约定位置）
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  application:
    name: order-service          # 服务名（微服务必配）
  datasource:
    url: jdbc:mysql://localhost:3306/order
    username: root
    password: ${DB_PASSWORD}     # ⚠️ 环境变量引用（敏感信息外置）

app:
  cache:
    enabled: true
    ttl-seconds: 3600
```

### 1.2 yml vs properties

| 维度 | YAML | properties |
|------|:---:|:---:|
| 结构 | 层级缩进 | 扁平 key |
| 可读性 | ✅ 高 | 中 |
| 数组/对象 | ✅ 原生 | 需下标 |
| 注释 | # 支持 | 支持 |
| 推荐 | ✅ 现代标准 | 兼容遗留 |

```yaml
# YAML 数组与对象
app:
  servers:
    - host: a.example.com
      port: 8080
    - host: b.example.com
      port: 8080
```

---

## 2. 配置绑定：@ConfigurationProperties

### 2.1 类型安全绑定

```java
// ⚠️ 类型安全配置绑定（替代 @Value 逐个取值）
@Component
@ConfigurationProperties(prefix = "app.cache")
@Data
public class CacheProperties {
    private boolean enabled;
    private int ttlSeconds = 300;           // 默认值
    private Redis redis = new Redis();      // 嵌套对象

    @Data
    public static class Redis {
        private String host = "localhost";
        private int port = 6379;
    }
}
```

```java
// 使用：构造器注入（Boot 3+ 推荐）
@Service
public class CacheService {
    private final CacheProperties props;

    public CacheService(CacheProperties props) {   // ⚠️ 构造器注入
        this.props = props;
    }
}
```

### 2.2 @ConfigurationProperties vs @Value

| 维度 | @ConfigurationProperties | @Value |
|------|:---:|:---:|
| 类型 | ✅ 类型安全 | 需转换 |
| 分组 | ✅ 前缀分组 | 分散 |
| 校验 | ✅ @Validated | ❌ |
| 刷新 | ✅（配置中心配合） | ❌ |
| 适用 | 一组配置 | 单值引用 |

> 🎯 **要点**：一组配置用 `@ConfigurationProperties`（类型安全 + 可校验），单值才用 `@Value`。这是 Boot 配置的黄金规则。

---

## 3. Profile 环境隔离

### 3.1 多环境配置

```yaml
# application.yml（公共配置）
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}   # ⚠️ 环境变量指定

# application-dev.yml（开发）
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/dev

# application-prod.yml（生产）
server:
  port: 80
spring:
  datasource:
    url: jdbc:mysql://prod-host:3306/prod
    password: ${DB_PASSWORD}
```

### 3.2 Profile 激活方式

```
激活方式（优先级从高到低）：
  ① 命令行：--spring.profiles.active=prod
  ② 环境变量：SPRING_PROFILES_ACTIVE=prod
  ③ application.yml 的 spring.profiles.active

⚠️ 面试必答：
"Profile = 环境隔离（dev/test/prod）——
 命名约定 application-{profile}.yml，
 激活靠环境变量（生产禁止写死在 yml）。"
```

---

## 4. 配置优先级

### 4.1 优先级排序（从高到低）

```
① 命令行参数（--server.port=8081）      ← 最高
② Java 系统属性（-Dserver.port）
③ 环境变量（SERVER_PORT）
④ application-{profile}.yml
⑤ application.yml
⑥ @PropertySource 注解导入
⑦ 默认值                              ← 最低

⚠️ 面试必答：
"配置优先级'命令行 > 系统属性 > 环境变量
 > profile > 主配置'——部署时用命令行/
 环境变量覆盖默认配置（12-Factor 配置外置）。"
```

### 4.2 覆盖示例

```bash
# 部署时覆盖配置（不修改 jar 内配置）
java -jar app.jar \
  --server.port=8081 \
  --spring.datasource.url=jdbc:mysql://new-host:3306/db

# 或环境变量
export SPRING_DATASOURCE_PASSWORD='secret'
java -jar app.jar
```

---

## 5. 配置刷新与配置中心

### 5.1 配置刷新

```java
// ⚠️ 动态刷新：@RefreshScope（配合配置中心）
@RestController
@RefreshScope                       // 配置变更自动重建 Bean
public class ConfigController {
    @Value("${app.feature.enabled}")
    private boolean featureEnabled;
}
// 触发：配置中心变更 → 发送刷新事件 → Bean 重建
// ⚠️ 局限：刷新会重建 Bean（有状态 Bean 需谨慎）
```

### 5.2 配置中心对比

| 方案 | 动态刷新 | 特点 |
|------|:---:|------|
| Spring Cloud Config | ✅（需 Bus） | Spring 原生 |
| **Nacos** | ✅ 原生 | 国产主流（注册 + 配置一体） |
| Apollo | ✅ 原生 | 携程开源（企业级功能全） |

```yaml
# Nacos 配置中心示例
spring:
  cloud:
    nacos:
      config:
        server-addr: nacos:8848
        file-extension: yaml
  config:
    import: optional:nacos:order-service.yaml   # Boot 3+ 导入方式
```

> 🎯 **要点**：配置中心三选——微服务标配 Nacos（注册发现 + 配置一体）。动态刷新 = 配置中心 + @RefreshScope。

---

> 🎯 **核心要点**：配置体系 = **YAML 语法**（层级/数组/环境变量引用）+ **类型安全绑定**（@ConfigurationProperties 优先 @Value）+ **Profile 隔离**（dev/test/prod + 环境变量激活）+ **优先级**（命令行 > 环境变量 > 配置文件）+ **配置中心**（Nacos + @RefreshScope 动态刷新）。"配置外置 + 环境隔离 + 可覆盖"是企业级配置三原则。

---

**返回总览**：[00-SpringBoot总览与核心概念](00-SpringBoot总览与核心概念.md) | **上一篇**：[01-SpringBoot自动配置原理](01-SpringBoot自动配置原理.md) | **下一篇**：[03-SpringBootWeb开发](03-SpringBootWeb开发.md)
