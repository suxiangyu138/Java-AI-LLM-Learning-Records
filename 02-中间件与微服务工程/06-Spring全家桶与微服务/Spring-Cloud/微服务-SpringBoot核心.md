# 微服务 - Spring Boot 核心

> **定位**：Spring Boot 是 Spring Cloud 的根基——搭建单个微服务骨架，Spring Cloud 为多个骨架赋予协同治理的灵魂。

---

## 目录

1. [两者依存关系](#1-两者依存关系)
2. [四大核心能力](#2-四大核心能力)
3. [搭建微服务骨架](#3-搭建微服务骨架)
4. [常见问题](#4-常见问题)

---

## 1. 两者依存关系

| 维度 | Spring Boot | Spring Cloud |
|------|------------|--------------|
| **定位** | 单个微服务开发框架 | 多个微服务治理方案 |
| **功能** | 自动配置、starter、内嵌容器 | 注册发现、负载均衡、熔断降级 |
| **依赖** | 可独立使用 | 必须依赖 Spring Boot |
| **类比** | 大楼的每个房间 | 大楼的电梯、走廊、安保 |

### 版本匹配

| Spring Boot | Spring Cloud |
|:----------:|:-----------:|
| 2.7.x | 2021.0.x |

---

## 2. 四大核心能力

| 能力 | 作用 | 关键点 |
|------|------|--------|
| **自动配置** | `@EnableAutoConfiguration` 扫描依赖自动配置 | 引入 starter → 自动初始化组件 |
| **Starter 依赖** | 依赖包集合，统一版本管理 | `spring-boot-starter-web/jdbc/data-redis` |
| **内嵌容器** | Tomcat/Jetty/Undertow 内嵌 | `java -jar` 一键部署 |
| **监控调试** | Actuator + DevTools + 日志 | `/actuator/health` 健康检查 |

### 常用 Starter

| Starter | 用途 |
|---------|------|
| `spring-boot-starter-web` | Web 开发（MVC + Tomcat） |
| `spring-boot-starter-data-redis` | Redis 缓存 |
| `spring-cloud-starter-openfeign` | 服务调用 |
| `spring-cloud-starter-nacos-discovery` | 服务注册 |

---

## 3. 搭建微服务骨架

### 父工程统一版本

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.10</version>
</parent>
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2021.0.9</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 子模块配置

```yaml
server:
  port: 8081
spring:
  application:
    name: user-service
  datasource:
    url: jdbc:mysql://localhost:3306/user_db
    username: root
    password: 123456
management:
  endpoints:
    web:
      exposure:
        include: "*"
```

---

## 4. 常见问题

| 问题 | 原因 | 方案 |
|------|------|------|
| 版本不匹配 | Boot/Cloud 版本错配 | 查官方对应表，父工程统一管理 |
| 启动慢/内存高 | 默认 JVM 参数 + 依赖过多 | `-Xms512m -Xmx512m` + 精简依赖 |
| 自动配置失效 | 依赖缺失/扫描范围不足 | 检查 starter 依赖 + `@ComponentScan` |
| 注册中心发现不了 | 端口占用/名称不一致 | 检查端口 + `spring.application.name` |
