# 02-Logback 配置与生产实践
> 生产级 Logback：Pattern 设计、滚动策略、异步日志、MDC 链路、条件配置、JSON 结构化日志——从"能打日志"到"日志好用"

## 📚 目录
1. [配置结构速览](#1-配置结构速览)
2. [Pattern 设计](#2-pattern-设计)
3. [滚动策略](#3-滚动策略)
4. [异步日志](#4-异步日志)
5. [MDC：上下文注入](#5-mdc上下文注入)
6. [条件配置与多环境](#6-条件配置与多环境)
7. [JSON 结构化日志](#7-json-结构化日志)
8. [生产级完整配置模板](#8-生产级完整配置模板)
9. [核心要点](#9-核心要点)
10. [参考来源](#10-参考来源)

## 1. 配置结构速览

```text
logback.xml 三要素：
  appender（输出到哪：控制台/文件/异步）
  encoder（怎么格式化：Pattern / JSON）
  logger/root（哪些类什么级别 → 哪个 appender）
```

| 组件 | 职责 | 常用实现 |
|------|------|---------|
| Appender | 输出目标 | ConsoleAppender、RollingFileAppender、AsyncAppender |
| Encoder | 格式化 | PatternLayoutEncoder（文本）、LogstashEncoder（JSON） |
| Filter | 过滤 | LevelFilter、ThresholdFilter、MDCFilter |
| Logger | 级别与去向 | 按包配置 |

## 2. Pattern 设计

```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
```

| 占位符 | 含义 | 生产建议 |
|--------|------|---------|
| `%d{...}` | 时间 | 带毫秒：`yyyy-MM-dd HH:mm:ss.SSS` |
| `%-5level` | 级别（左对齐 5 位） | ✅ |
| `%thread` | 线程名 | ✅（定位并发问题） |
| `%logger{36}` | 类名（截断） | ✅ |
| `%msg` / `%n` | 消息 / 换行 | ✅ |
| `%X{traceId}` | MDC 值 | ✅ 必须（链路关联） |
| `%X{userId}` | 业务上下文 | ✅ 建议 |
| `%caller{1}` | 调用位置 | ❌ 性能昂贵 |
| `%L` | 行号 | ❌ 性能昂贵（生产禁） |

> ⚠️ 生产 Pattern 铁律：**含 `%X{traceId}`**（链路关联）；**禁 `%caller`/`%L`**（反射定位，性能杀手）。

## 3. 滚动策略

```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/app.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <!-- 按天滚动 + 压缩 -->
        <fileNamePattern>logs/app-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <timeBasedFileNamingAndTriggeringPolicy
                class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
            <maxFileSize>500MB</maxFileSize>    <!-- 单文件上限 -->
        </timeBasedFileNamingAndTriggeringPolicy>
        <maxHistory>30</maxHistory>             <!-- 保留 30 天 -->
        <totalSizeCap>20GB</totalSizeCap>       <!-- 总容量上限 -->
    </rollingPolicy>
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
        <charset>UTF-8</charset>                <!-- 防中文乱码 -->
    </encoder>
</appender>
```

| 配置 | 说明 | 建议 |
|------|------|------|
| `fileNamePattern` | **必须含日期占位符**（`%d`） | 按天 `app-%d{yyyy-MM-dd}.log` |
| `maxFileSize` | 单文件大小上限 | 500MB |
| `maxHistory` | 保留天数 | 30 天（按合规调整） |
| `totalSizeCap` | 总容量上限（防磁盘爆） | 20GB |
| `charset` | 编码 | **UTF-8（中文乱码元凶）** |
| `.gz` | 压缩历史文件 | ✅ |

> 💡 滚动策略 = 磁盘安全三件套：**按天滚动 + 大小上限 + 总量上限**。

## 4. 异步日志

```xml
<appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
    <appender-ref ref="FILE"/>
    <queueSize>2048</queueSize>            <!-- 队列容量 -->
    <neverBlock>true</neverBlock>          <!-- 满时丢弃而非阻塞业务线程 -->
    <discardingThreshold>0</discardingThreshold>  <!-- 不丢弃（配合 neverBlock 权衡） -->
    <includeCallerData>false</includeCallerData>  <!-- 性能关键 -->
</appender>
```

| 参数 | 默认 | 说明 |
|------|------|------|
| `queueSize` | 256 | 队列容量（生产建议 1024-2048） |
| `neverBlock` | false | **true = 队列满丢弃日志，不阻塞业务**（生产必配） |
| `discardingThreshold` | 20% | 队列剩余 20% 时丢弃 TRACE/DEBUG/INFO |
| `includeCallerData` | false | 收集调用位置（性能昂贵，保持 false） |

> ⚠️ **异步日志风险**：队列积压、应用异常退出时**可能丢日志**。核心业务/审计日志不宜只靠异步——重要日志同步落盘 + 常规日志异步。

## 5. MDC：上下文注入

### 5.1 基础用法

```java
import org.slf4j.MDC;

// 请求入口注入
MDC.put("traceId", traceId);
MDC.put("userId", userId);
try {
    // ... 业务逻辑（所有日志自动带 traceId/userId）
} finally {
    MDC.remove("traceId");    // 必须清理！防线程池复用串号
    MDC.remove("userId");
}
```

```xml
<pattern>%d{HH:mm:ss.SSS} %-5level [%thread] [%X{traceId}] [%X{userId}] %logger{36} - %msg%n</pattern>
```

### 5.2 线程池场景（易错点）

```java
// ❌ 线程池内日志丢失 traceId（MDC 是 ThreadLocal）
executor.submit(() -> log.info("异步任务"));   // 无 traceId！

// ✅ 方案 1：TaskDecorator 传递
@Bean
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setTaskDecorator(runnable -> {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return () -> {
            MDC.setContextMap(context);
            try { runnable.run(); } finally { MDC.clear(); }
        };
    });
    return executor;
}

// ✅ 方案 2：TransmittableThreadLocal（阿里 TTL）
```

> 🎯 **MDC 铁律**：入口 put → finally remove；线程池用 TaskDecorator/TTL 传递——否则异步日志全部"无主"。

## 6. 条件配置与多环境

```xml
<!-- logback-spring.xml（Spring Boot 用 springProfile 按环境切换） -->
<configuration>
    <!-- 开发：控制台 DEBUG -->
    <springProfile name="dev">
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <!-- 生产：文件 INFO + 异步 -->
    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="ASYNC"/>
        </root>
    </springProfile>
</configuration>
```

| 方式 | 说明 |
|------|------|
| `logback-spring.xml` + `springProfile` | Spring Boot 按 profile 切换（推荐） |
| `logback.xml` + `<if>` 条件 | 按属性判断（需 janino 依赖） |
| `application.yml` 配置 `logging.*` | 简单场景（级别/文件路径） |

> 💡 文件名注意：**`logback-spring.xml` 才支持 `springProfile`**；`logback.xml` 不支持 Spring 扩展。

## 7. JSON 结构化日志

```xml
<!-- 依赖：net.logstash.logback:logstash-logback-encoder -->
<appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/app-json-%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <!-- 可选：自定义字段 -->
        <customFields>{"app":"order-service","env":"prod"}</customFields>
    </encoder>
</appender>
```

```json
// 输出示例（采集平台直接解析）
{"@timestamp":"2026-08-08T10:00:00.000+08:00","level":"INFO",
 "thread":"http-nio-8080-exec-1","logger":"com.demo.OrderService",
 "traceId":"a1b2c3d4","userId":"1001",
 "message":"创建订单","orderId":"SO-1001"}
```

> 💡 **JSON 日志的价值**：采集平台（Loki/ES）无需解析文本 Pattern——字段天然结构化，查询/聚合/告警全部直接可用。**上集中式平台前先改 JSON 日志**（详见 [03](03-日志采集与集中式平台.md)）。

## 8. 生产级完整配置模板

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 通用 Pattern（含 MDC） -->
    <property name="PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] [%X{traceId}] [%X{userId}] %logger{36} - %msg%n"/>

    <!-- 控制台 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder><pattern>${PATTERN}</pattern><charset>UTF-8</charset></encoder>
    </appender>

    <!-- 文件（按天滚动 + 大小 + 总量上限） -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/app.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/app-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy
                    class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>500MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
            <totalSizeCap>20GB</totalSizeCap>
        </rollingPolicy>
        <encoder><pattern>${PATTERN}</pattern><charset>UTF-8</charset></encoder>
    </appender>

    <!-- 异步（生产文件日志走异步） -->
    <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="FILE"/>
        <queueSize>2048</queueSize>
        <neverBlock>true</neverBlock>
        <includeCallerData>false</includeCallerData>
    </appender>

    <!-- 环境切换 -->
    <springProfile name="dev">
        <root level="DEBUG"><appender-ref ref="CONSOLE"/></root>
    </springProfile>
    <springProfile name="prod">
        <logger name="com.demo" level="INFO"/>
        <root level="WARN"><appender-ref ref="ASYNC"/></root>
    </springProfile>
</configuration>
```

## 9. 核心要点

> 🎯 **核心要点**：
> - Pattern 铁律：`%X{traceId}` 必带、`%caller`/`%L` 生产禁；
> - 滚动三件套：按天 + 单文件 500MB + 总量 20GB，charset=UTF-8；
> - 异步四参数：queueSize 1024-2048、neverBlock=true、discardingThreshold 权衡、includeCallerData=false；**审计日志不异步**；
> - MDC：入口 put → finally remove；线程池 TaskDecorator/TTL 传递；
> - 环境切换用 `logback-spring.xml` + `springProfile`；
> - 上集中式平台前先切 **JSON 结构化日志**（采集/查询/告警全链路受益）。

## 10. 参考来源

- [Logback 官方文档（Configuration）](https://logback.qos.ch/manual/configuration.html)
- [Logback 异步 Appender 文档](https://logback.qos.ch/manual/appenders.html#AsyncAppender)
- [Logstash Logback Encoder（JSON）](https://github.com/logfellow/logstash-logback-encoder)
- [Logback + SLF4J + MDC 链路追踪实战](https://developer.aliyun.com/article/1720026)

---

**下一模块**：[03-日志采集与集中式平台](03-日志采集与集中式平台.md)　/　**返回总览**：[00-总览](00-日志监控指标总览.md)
