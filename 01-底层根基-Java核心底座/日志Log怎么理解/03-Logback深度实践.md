# 03 - Logback 深度实践

> **核心摘要**：Logback 是 2026 主流日志实现——三层架构、Appender 体系、滚动策略、异步日志、MDC 支持。本文给出可直接复用的生产级 logback.xml 配置与性能调优。

> **前置阅读**：[[02-Java日志体系全景]]

---

## 📚 目录

1. [Logback 三层架构](#1-logback-三层架构)
2. [核心组件：Logger/Appender/Layout](#2-核心组件loggerappenderlayout)
3. [生产级配置模板](#3-生产级配置模板)
4. [滚动策略](#4-滚动策略)
5. [异步日志](#5-异步日志)
6. [过滤器与级别控制](#6-过滤器与级别控制)
7. [运行时调级](#7-运行时调级)
8. [常见陷阱](#8-常见陷阱)
9. [核心要点](#9-核心要点)

---

## 1. Logback 三层架构

> **背景**：Logback 是 SLF4J 官方实现——三层模块化设计（core/classic/access）。
> **目的**：理解模块分工，选对依赖。
> **适用范围**：SLF4J + Logback 项目。
> **不适用场景**：已选 Log4j2 的项目（不必换）。

```text
Logback 三层
├── ① logback-core：核心（Appender/Layout 基础设施）
├── ② logback-classic：SLF4J 实现层（Logger 体系）
│   └── ⚠️ 业务开发只用这个（core 是传递依赖）
├── ③ logback-access：HTTP 访问日志（Servlet 容器）
│   └── 用途：与 Tomcat/Jetty 集成（访问日志）
└── 依赖（Maven）
    ├── logback-classic（核心使用）
    └── logback-access（需要访问日志时）
```

---

## 2. 核心组件：Logger/Appender/Layout

```text
Logback 三组件
├── ① Logger：日志记录器（代码中使用）
│   ├── 获取：LoggerFactory.getLogger(类)
│   ├── 层级：包层级继承（com.example 覆盖 com）
│   └── 级别：可在 Logger 上设置
├── ② Appender：输出目标（写哪）
│   ├── ConsoleAppender：控制台
│   ├── FileAppender：文件
│   ├── RollingFileAppender：滚动文件（生产）
│   ├── AsyncAppender：异步包装
│   └── SocketAppender：网络（采集）
├── ③ Layout/Encoder：格式（怎么排版）
│   ├── PatternLayout：文本格式（%d/%level/%msg）
│   └── JsonEncoder：JSON 结构化（见 06 篇）
└── 关系：Logger（产生）→ Appender（输出）→ Encoder（格式）
```

---

## 3. 生产级配置模板

### 3.1 基础配置（可直接使用）

```xml
<!-- src/main/resources/logback-spring.xml（Spring Boot 命名） -->
<configuration scan="true" scanPeriod="60 seconds">
    <!-- ① 属性定义 -->
    <property name="LOG_PATH" value="${LOG_PATH:-/var/log/app}"/>
    <property name="PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} [%X{traceId:-}] - %msg%n"/>

    <!-- ② 控制台输出（开发/排查） -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- ③ 滚动文件输出（生产核心） -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/app.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <!-- 按天 + 大小滚动 -->
            <fileNamePattern>${LOG_PATH}/app.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>          <!-- 保留 30 天 -->
            <totalSizeCap>30GB</totalSizeCap>    <!-- 总容量上限 -->
        </rollingPolicy>
        <encoder>
            <pattern>${PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- ④ 错误日志单独文件（排查高效） -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/error.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/error.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
        <encoder>
            <pattern>${PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- ⑤ 根日志级别（生产 INFO，可动态调） -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
        <appender-ref ref="ERROR_FILE"/>
    </root>

    <!-- ⑥ 包级别控制（排查时开 DEBUG） -->
    <logger name="com.example.order" level="INFO"/>
</configuration>
```

### 3.2 配置要点

```text
配置要点
├── ① Spring Boot 用 logback-spring.xml（支持 profile）
├── ② scan=true：配置热更新（60 秒检查）
├── ③ LOG_PATH 环境变量：部署时可覆盖
├── ④ ERROR 单独文件：排障只看错误（高效）
├── ⑤ %X{traceId:-}：MDC 输出（默认空）（见 05 篇）
└── ⑥ 编码 UTF-8：中文日志不乱码
```

---

## 4. 滚动策略

### 4.1 滚动策略对比

| 策略 | 触发 | 适用 |
|------|------|------|
| **TimeBased** | 按时间（天/小时） | 常规（按天查） |
| **SizeBased** | 按大小 | 大日志量 |
| **SizeAndTimeBased** | 时间 + 大小（推荐） | **生产标准** |
| **FixedWindow** | 固定窗口 | 简单场景 |

```text
SizeAndTimeBased（2026 生产标准）
├── 触发：到时间（天）或超大小（100MB）先到先切
├── 命名：app.2026-08-04.0.log（日期 + 序号）
├── maxHistory：保留 30 天
├── totalSizeCap：总容量上限（防磁盘爆满）
├── cleanHistoryOnStart：启动时清理过期
└── 金句：滚动策略 = 「切分 + 保留 + 上限」三件套
```

---

## 5. 异步日志

### 5.1 为什么需要异步

> **背景**：日志写入 I/O 会阻塞业务线程——高并发下日志成为性能瓶颈。
> **目的**：日志写入异步化（业务线程不等待 I/O）。
> **适用范围**：高并发生产环境。
> **不适用场景**：日志量小（异步开销反而多余）；需要实时日志的场景。

### 5.2 异步配置

```xml
<!-- 异步包装（推荐参数） -->
<appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
    <appender-ref ref="FILE"/>
    <queueSize>2048</queueSize>          <!-- 队列大小 -->
    <discardingThreshold>0</discardingThreshold>  <!-- 不丢弃 INFO -->
    <neverBlock>true</neverBlock>        <!-- 队列满不阻塞业务 -->
    <includeCallerData>false</includeCallerData>  <!-- 性能 -->
</appender>
```

```text
异步参数详解（生产推荐值）
├── queueSize：2048（默认 256——太小易丢）
├── discardingThreshold：0（默认 20%——会丢 INFO！）
│   ├── ⚠️ 默认：队列满时丢弃 20% 尾部（含 INFO/WARN）
│   └── 设为 0：只丢 TRACE/DEBUG（不丢 INFO）
├── neverBlock：true（队列满不阻塞业务线程）
├── includeCallerData：false（不记录调用位置——性能）
└── 金句：异步日志 = 「不丢 INFO + 不阻塞业务」的参数组合
```

---

## 6. 过滤器与级别控制

### 6.1 常用过滤器

```xml
<!-- 级别过滤（ERROR 单独文件用） -->
<filter class="ch.qos.logback.classic.filter.LevelFilter">
    <level>ERROR</level>
    <onMatch>ACCEPT</onMatch>
    <onMismatch>DENY</onMismatch>
</filter>

<!-- 阈值过滤（>= 指定级别） -->
<filter class="ch.qos.logback.classic.filter.ThresholdFilter">
    <level>WARN</level>      <!-- 只输出 WARN 及以上 -->
</filter>

<!-- 评估器过滤（条件复杂） -->
<filter class="ch.qos.logback.core.filter.EvaluatorFilter">
    <evaluator>
        <expression>message.contains("payment")</expression>
    </evaluator>
</filter>
```

### 6.2 包级别控制

```xml
<!-- 包级别（排查时开 DEBUG） -->
<logger name="com.example.order" level="DEBUG"/>
<logger name="org.springframework" level="WARN"/>   <!-- 框架降噪 -->
<logger name="com.example.payment" level="DEBUG"/>
```

---

## 7. 运行时调级

### 7.1 三种方式（2026）

```text
运行时调整日志级别
├── ① 配置热更新：scan=true + scanPeriod（60 秒）
│   ├── 改 logback.xml → 自动生效（无需重启）
│   └── ⚠️ 生产改配置谨慎（回滚方案）
├── ② JMX：JMX 控制台调整
│   ├── 生产开启 JMX 需安全
│   └── 适合运维工具集成
├── ③ 配置中心（推荐 2026）：
│   ├── Nacos/Spring Cloud Config 动态下发
│   ├── 排查期临时开 DEBUG → 排完恢复
│   └── 结合 actuator：/actuator/loggers 端点调整
└── ④ 金句：排查 = 「临时开 DEBUG → 定位 → 恢复 INFO」
```

```text
Spring Boot actuator 调级
├── GET  /actuator/loggers            # 查看级别
├── GET  /actuator/loggers/包名        # 查看指定
├── POST /actuator/loggers/包名       # 修改
│   {"configuredLevel": "DEBUG"}
└── 注意：actuator 暴露需鉴权（见端口系列）
```

---

## 8. 常见陷阱

| # | 陷阱 | 表现 | 解法 |
|---|------|------|------|
| 1 | **异步丢日志** | 高并发丢 INFO | discardingThreshold=0 |
| 2 | **阻塞业务** | 日志慢拖垮接口 | neverBlock=true + 异步 |
| 3 | **磁盘爆满** | 日志占满磁盘 | totalSizeCap + maxHistory |
| 4 | **中文乱码** | 日志中文乱码 | charset UTF-8 |
| 5 | **重复输出** | 一条打两遍 | root + 子 logger 重复 ref |
| 6 | **配置不生效** | 改了没反应 | scan=true / 检查文件名 |
| 7 | **traceId 没有** | 日志缺链路 ID | MDC 注入 + %X 输出（05 篇） |
| 8 | **大对象日志** | 性能下降 | 不打印大对象（摘要） |
| 9 | **敏感泄露** | 密码进日志 | 脱敏（见 04 篇） |

---

## 9. 核心要点

> 🎯 **核心要点**：
> 1. 三层架构：core（基础）/classic（SLF4J 实现——业务用）/access（HTTP 访问）
> 2. 三组件：Logger（产生）+ Appender（输出）+ Encoder（格式）
> 3. 生产配置模板：滚动（时间+大小）+ ERROR 单独文件 + traceId 输出 + UTF-8
> 4. 滚动策略三件套：切分（SizeAndTime）+ 保留（maxHistory 30 天）+ 上限（totalSizeCap）
> 5. 异步参数：queueSize 2048 + **discardingThreshold=0（不丢 INFO）** + neverBlock=true
> 6. 运行时调级：配置中心/actuator——排查 = 临时 DEBUG → 定位 → 恢复

---

**下一模块**：[04-日志级别与最佳实践](04-日志级别与最佳实践.md) | **返回总览**：[00-日志Log知识体系总览](00-日志Log知识体系总览.md)
