# 07 - Spring Boot 生产运维与可观测

> 定位：Actuator 监控、结构化日志、OpenTelemetry 集成、优雅关闭、健康检查、部署方式——从开发到生产

## 📚 目录

1. [Actuator 生产监控](#1-actuator-生产监控)
2. [日志体系](#2-日志体系)
3. [OpenTelemetry 链路追踪](#3-opentelemetry-链路追踪)
4. [优雅关闭与生命周期](#4-优雅关闭与生命周期)
5. [健康检查与探针](#5-健康检查与探针)
6. [部署方式](#6-部署方式)

---

## 1. Actuator 生产监控

### 1.1 引入与配置

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,loggers,env   # ⚠️ 按需暴露
  endpoint:
    health:
      show-details: always          # 健康详情（生产建议 when-authorized）
  info:
    env:
      enabled: true
```

### 1.2 核心端点

| 端点 | 作用 | 生产建议 |
|------|------|:---:|
| /actuator/health | 健康检查（探针用） | ✅ 公开 |
| /actuator/info | 应用信息 | ✅ 公开 |
| /actuator/metrics | 指标（JVM/HTTP） | 内网 |
| /actuator/loggers | 动态日志级别 | ⚠️ 内网 |
| /actuator/env | 环境变量 | ❌ 敏感 |
| /actuator/heapdump | 堆转储 | ⚠️ 内网 |

> 🎯 **要点**：Actuator = 生产监控端点——**health 公开（K8s 探针用）、敏感端点（env/heapdump）内网或禁用**。

---

## 2. 日志体系

### 2.1 Logback 配置

```yaml
# application.yml 日志级别
logging:
  level:
    root: INFO
    com.example.order: DEBUG           # 包级覆盖
  file:
    name: logs/app.log
```

```xml
<!-- logback-spring.xml：结构化 JSON 日志（生产推荐） -->
<configuration>
    <!-- ⚠️ 生产：JSON 格式（可采集/检索） -->
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"order-service"}</customFields>
        </encoder>
    </appender>

    <!-- ⚠️ 开发：可读格式 + trace_id（链路关联） -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss} [%thread] %-5level
                [%X{trace_id}] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <springProfile name="prod">
        <root level="INFO"><appender-ref ref="JSON"/></root>
    </springProfile>
    <springProfile name="!prod">
        <root level="DEBUG"><appender-ref ref="CONSOLE"/></root>
    </springProfile>
</configuration>
```

### 2.2 日志规范

```
✅ 结构化（JSON）+ 携带 trace_id（链路关联）
✅ 分级（DEBUG 开发/INFO 生产）
✅ 禁止打印敏感信息（密码/令牌）
✅ 异步日志（Logback AsyncAppender 防阻塞）
✅ 动态级别（Actuator /loggers 调整，免重启）
```

> 🎯 **要点**：日志规范 = JSON 结构化 + trace_id + 动态级别 + 异步输出。**trace_id 贯穿日志与链路**是可观测性关键。

---

## 3. OpenTelemetry 链路追踪

### 3.1 OTel Starter（Boot 4 官方）

```xml
<!-- ⚠️ Boot 4：一个依赖搞定全链路追踪 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-opentelemetry</artifactId>
</dependency>
```

```yaml
# application.yml
management:
  opentelemetry:
    enabled: true                # Boot 4.1 新增开关
  tracing:
    sampling:
      probability: 0.1           # ⚠️ 采样率（高流量 10%）
    export:
      otlp:
        endpoint: http://collector:4317   # OTLP 导出
```

```java
// 业务关键点手动埋点
@RestController
public class OrderController {

    @GetMapping("/orders/{id}")
    public Order getOrder(@PathVariable Long id,
                          @Observed(name = "order.get")     // ⚠️ 观察注解
                          ObservationRegistry registry) {
        // Observation 自动记录指标 + 生成 Span
    }
}
```

### 3.2 可观测三件套（Boot 视角）

```
Metrics：Micrometer（Boot 内置）→ Prometheus
Logs：Logback JSON + trace_id
Traces：OTel Starter → Jaeger/Tempo

⚠️ 面试必答：
"Boot 可观测 = Micrometer（指标）+
 Logback JSON（日志）+ OTel（链路），
 三者的关联键是 trace_id。"
```

---

## 4. 优雅关闭与生命周期

### 4.1 优雅关闭

```yaml
server:
  shutdown: graceful          # ⚠️ 优雅关闭（Boot 2.3+）
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s    # 每阶段最多 30s
```

```
优雅关闭流程：
  ① 停止接收新请求
  ② 等待处理中的请求完成（超时 30s）
  ③ 释放资源（连接池/线程池）
  ④ 退出

⚠️ 面试必答：
"优雅关闭 = 先拒新请求 → 等存量完成 →
 释放资源 → 退出；
 K8s 滚动更新依赖优雅关闭（否则请求中断）。"
```

### 4.2 生命周期钩子

```java
// 应用生命周期监听（启动/关闭）
@Component
public class AppLifecycleListener
        implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // ⚠️ 应用就绪后执行（预热缓存/注册服务）
        log.info("应用就绪");
    }
}

// 关闭时清理
@PreDestroy
public void cleanup() {
    // 关闭前清理（连接池/定时任务）
}
```

---

## 5. 健康检查与探针

### 5.1 健康检查端点

```java
// 自定义健康检查（依赖外部服务时）
@Component
public class DatabaseHealthIndicator
        implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // 检查数据库连通性
            return Health.up().withDetail("db", "可用").build();
        } catch (Exception e) {
            return Health.down().withDetail("db", e.getMessage()).build();
        }
    }
}
// GET /actuator/health → {"status":"UP","components":{...}}
```

### 5.2 K8s 探针

```yaml
# Kubernetes 探针配置（Boot + Actuator）
livenessProbe:              # 存活探针（挂死重启）
  httpGet:
    path: /actuator/health/liveness
  initialDelaySeconds: 30

readinessProbe:             # 就绪探针（就绪才接流量）
  httpGet:
    path: /actuator/health/readiness
  periodSeconds: 5

startupProbe:               # 启动探针（启动慢的应用）
  httpGet:
    path: /actuator/health/readiness
  failureThreshold: 30
```

> 🎯 **要点**：健康检查三探针——liveness（死没死）、readiness（能不能接流量）、startup（启动完没）。**readiness 与优雅关闭配合实现零中断发布**。

---

## 6. 部署方式

### 6.1 三种部署

| 方式 | 场景 | 特点 |
|------|------|------|
| java -jar | 简单部署 | 直接运行 |
| Docker 容器 | 现代标准 | 环境一致 + 编排 |
| 原生镜像（AOT） | 极致启动 | Boot 3+ GraalVM，启动 <50ms |

```dockerfile
# ⚠️ 多阶段构建（Boot 4 推荐）
# 阶段 1：构建
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline          # 依赖缓存层
COPY src ./src
RUN mvn package -DskipTests

# 阶段 2：运行（distroless 瘦镜像）
FROM eclipse-temurin:21-jre
COPY --from=build /app/target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
```

### 6.2 生产启动参数

```bash
# 生产 JVM 参数
java -jar app.jar \
  -Xms1g -Xmx1g \
  -XX:MaxRAMPercentage=75 \      # 容器内自适应内存
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  --spring.profiles.active=prod \
  --spring.datasource.password=${DB_PASSWORD}
```

> 🎯 **要点**：部署三件套——多阶段构建（瘦镜像）、容器内内存自适应（MaxRAMPercentage）、配置外置（环境变量）。Boot 4 的 AOT 原生镜像是低延迟场景答案。

---

> 🎯 **核心要点**：生产运维体系 = **Actuator**（health 公开 + 敏感端点内网）+ **日志**（JSON + trace_id + 动态级别）+ **OTel**（一个依赖全链路）+ **优雅关闭**（拒新 → 等存量 → 清理）+ **健康探针**（liveness/readiness/startup）+ **部署**（多阶段构建 + 参数外置）。"优雅关闭 + 探针 + 可观测"是 K8s 环境三标配。

---

**返回总览**：[00-SpringBoot总览与核心概念](00-SpringBoot总览与核心概念.md) | **上一篇**：[06-SpringBoot安全与认证](06-SpringBoot安全与认证.md) | **下一篇**：[08-SpringBoot微服务进阶与面试题](08-SpringBoot微服务进阶与面试题.md)
