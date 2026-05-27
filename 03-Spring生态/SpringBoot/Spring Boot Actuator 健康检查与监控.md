# Spring Boot Actuator 健康检查与监控

## 基础接入

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
        include: health,info,metrics,prometheus  # 暴露的端点
      base-path: /actuator
  endpoint:
    health:
      show-details: always  # 显示详情
```

## 常用端点

| 端点 | 用途 |
|------|------|
| `/actuator/health` | 健康状态（K8s 探针） |
| `/actuator/info` | 应用信息（版本、描述） |
| `/actuator/metrics` | 指标列表 |
| `/actuator/metrics/{name}` | 某个指标详情 |
| `/actuator/env` | 环境变量和配置 |
| `/actuator/loggers` | 动态修改日志级别 |
| `/actuator/threaddump` | 线程堆栈快照 |
| `/actuator/heapdump` | 堆 dump 下载 |
| `/actuator/prometheus` | Prometheus 格式指标 |

## K8s 探针配置

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true  # 开启 /actuator/health/liveness 和 /actuator/health/readiness
```

两个探针的区别：
- **Liveness**：容器是否还活着。失败 → K8s 重启容器。检查死锁、OOM 等致命问题。
- **Readiness**：能否接收流量。失败 → K8s 摘除该 Pod。检查 DB/Redis 等外部依赖。

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30    # 启动后等 30 秒才检查
  periodSeconds: 10          # 每 10 秒检查一次
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
```

## 自定义健康指示器

```java
@Component
public class RedisHealthIndicator implements HealthIndicator {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Health health() {
        try {
            redisTemplate.opsForValue().get("health-check");
            return Health.up().withDetail("redis", "connected").build();
        } catch (Exception e) {
            return Health.down().withDetail("redis", "disconnected")
                    .withException(e).build();
        }
    }
}
```

访问 `/actuator/health` 会看到：
```json
{
  "status": "UP",
  "components": {
    "redis": { "status": "UP", "details": { "redis": "connected" } },
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

## 动态修改日志级别

无需重启，线上排查利器：

```bash
# 查看当前级别
curl http://localhost:8080/actuator/loggers/com.example.user

# 临时改成 DEBUG
curl -X POST http://localhost:8080/actuator/loggers/com.example.user \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

10-30 分钟后记得改回来，避免日志爆炸。

## 常用 Metrics

```bash
# JVM 内存
/actuator/metrics/jvm.memory.used

# GC 暂停时间
/actuator/metrics/jvm.gc.pause

# HTTP 请求统计
/actuator/metrics/http.server.requests

# 数据库连接
/actuator/metrics/hikaricp.connections.active

# CPU 使用率
/actuator/metrics/process.cpu.usage
```

## 安全防护

生产环境不要暴露所有端点：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus  # 只暴露安全的
  endpoint:
    health:
      show-details: never     # 生产不要展示详情
```

或通过 Spring Security 保护：
```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
    .requestMatchers("/actuator/**").hasRole("ADMIN")
);
```
