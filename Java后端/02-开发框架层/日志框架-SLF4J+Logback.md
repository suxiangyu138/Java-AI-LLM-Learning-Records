# 日志框架 SLF4J + Logback 实战

## 门面 + 实现

Java 日志体系采用"门面 + 实现"模式：

- **SLF4J**：门面（Simple Logging Facade），提供统一 API
- **Logback**：实现，Spring Boot 默认，Log4j 作者的新作
- **Log4j2**：另一种实现，异步性能更好

代码只依赖 SLF4J API，可以随时替换底层实现而不改代码。

## 基本使用

```java
@Slf4j  // Lombok，代替 LoggerFactory.getLogger
@RestController
public class UserController {

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        log.info("查询用户, id={}", id);  // 占位符方式，避免字符串拼接
        log.debug("调试信息");
        log.warn("警告：用户 {} 不存在", id);
        log.error("查询用户异常", exception);  // 异常对象放最后参数
        return userService.getById(id);
    }
}
```

**占位符原则**：始终用 `{}` 占位符而不是字符串拼接。拼接会先执行 toString，即使该日志级别被关闭。

## 日志级别

```
TRACE < DEBUG < INFO < WARN < ERROR
```

**使用建议：**
- `ERROR`：系统错误，需要人工介入（如数据库挂了）
- `WARN`：潜在问题，不影响主流程（如降级、重试成功）
- `INFO`：关键业务节点（请求进来、重要操作完成、定时任务执行）
- `DEBUG`：开发调试信息（SQL 参数、方法入参出参）
- `TRACE`：极其详细的追踪（很少用）

## Logback 配置

`src/main/resources/logback-spring.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 按天滚动文件 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/app.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/app.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>  <!-- 保留30天 -->
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 异步输出（提升性能） -->
    <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="FILE"/>
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
    </appender>

    <!-- 按包名控制级别 -->
    <logger name="com.example.mapper" level="DEBUG"/>   <!-- 打印 SQL -->
    <logger name="org.springframework" level="INFO"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC"/>
    </root>
</configuration>
```

## pattern 占位符

| 符号 | 含义 |
|------|------|
| `%d` | 时间 |
| `%thread` | 线程名 |
| `%-5level` | 日志级别（左对齐 5 字符） |
| `%logger{36}` | Logger 名（最长 36 字符） |
| `%msg` | 日志消息 |
| `%n` | 换行 |
| `%X{traceId}` | MDC 中的 traceId |

## MDC：全链路追踪

在拦截器中设置 traceId：

```java
@Component
public class TraceInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null) traceId = UUID.randomUUID().toString().replace("-", "");
        MDC.put("traceId", traceId);
        return true;
    }

    @Override
    public void afterCompletion(...) {
        MDC.clear();  // 线程池场景必须清理
    }
}
```

配合 pattern 中使用 `%X{traceId}`，同一请求的所有日志可串联。

## 生产环境实践

- **不要用 `System.out.println`**，会直接写控制台不走日志框架
- **不要用 `e.printStackTrace()`**，输出到 stderr 无法被日志系统收集
- **异常日志用两个参数**：`log.error("msg", exception)`，第一个是描述，第二个是异常对象
- **日志里不要打印敏感信息**（密码、手机号、身份证号），需要脱敏
- **大对象不要整对象 toString**，只打印关键字段
- **不要在生产环境开 DEBUG**（尤其是 MyBatis SQL 大查询时）
