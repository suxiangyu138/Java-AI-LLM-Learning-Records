# 数据库连接池详解（HikariCP + Druid）

## 为什么需要连接池

数据库连接是昂贵的资源：
- TCP 握手 + TLS + MySQL 认证，新建一个连接约耗时 50-200ms
- 每个连接占用 MySQL 内存约 256KB-2MB
- 连接数有限（MySQL 默认 151）

连接池的核心思想：**预先创建一批连接并复用，用完后归还，而不是销毁重建。**

## HikariCP（Spring Boot 默认）

Spring Boot 2.x 起默认连接池，以极致的性能著称。

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: xxx
    hikari:
      maximum-pool-size: 20        # 最大连接数
      minimum-idle: 5              # 最小空闲连接
      idle-timeout: 600000         # 空闲超时 10分钟
      connection-timeout: 30000    # 获取连接等待超时
      max-lifetime: 1800000        # 连接最大存活时间 30分钟
      connection-test-query: SELECT 1  # 连接有效性检测
```

**核心配置原则：**
- `maximum-pool-size` 不是越大越好。CPU 核心数 × 2 + 磁盘数是一个常见起点。
- `max-lifetime` 应比 MySQL 的 `wait_timeout` 短，避免被 MySQL 断开。
- `idle-timeout` 控制空闲连接回收，减少不必要占用。

## Druid（阿里开源）

HikariCP 之外最常用的连接池，提供了监控和防火墙功能。

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-starter</artifactId>
    <version>1.2.20</version>
</dependency>
```

```yaml
spring:
  datasource:
    druid:
      url: jdbc:mysql://localhost:3306/mydb
      username: root
      password: xxx
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 30000
      # 监控配置
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        login-username: admin
        login-password: admin
      # SQL 防火墙
      filter:
        wall:
          enabled: true
      # 慢 SQL 记录
      filter:
        stat:
          enabled: true
          slow-sql-millis: 1000
          log-slow-sql: true
```

**Druid 优势：**
- 内置监控面板（`/druid`），查看连接池状态、SQL 执行统计
- SQL 防火墙防注入
- 慢 SQL 日志
- 连接泄漏检测

## 选择建议

- **Spring Boot 单体 / 中小项目**：HikariCP（默认，开箱即用）
- **需要 SQL 监控、防火墙**：Druid（调试排查更方便）
- 可以先用 HikariCP，排查问题时加入 Druid 的数据源监控

## 常见问题

**1. "Connection is not available"**
连接池耗尽。检查：
- 是否有连接泄漏（获取后未关闭）
- `maximum-pool-size` 是否设太小
- 是否有慢查询大量占用连接

**2. 连接泄漏排查**

Druid 可以在配置中开启连接泄漏检测：
```yaml
druid:
  remove-abandoned: true
  remove-abandoned-timeout: 1800  # 超过 30 分钟自动回收
```

代码层面确保 `try-with-resources` 或 finally 中关连接：
```java
try (Connection conn = dataSource.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {
    // 自动关闭
}
```

**3. `max-lifetime` 设置**

设置原则：`max-lifetime` < MySQL `wait_timeout`，建议差 30 秒。
MySQL 默认 `wait_timeout` = 28800 秒（8 小时），HikariCP 默认 `max-lifetime` = 1800000 毫秒（30 分钟），足够安全。
