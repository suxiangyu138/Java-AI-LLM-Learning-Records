# 06-Java 应用网络问题排查
> 网络问题在 Java 应用侧的落地：连接池、超时配置、Socket/SSL 异常、jstack/Arthas 定位——"异常信息"到"根因"的翻译表

## 📚 目录
1. [Java 网络异常信息翻译表](#1-java-网络异常信息翻译表)
2. [连接池问题](#2-连接池问题)
3. [HTTP 客户端配置](#3-http-客户端配置)
4. [数据库连接池网络问题](#4-数据库连接池网络问题)
5. [SSL/HTTPS 异常](#5-sslhttps-异常)
6. [jstack / Arthas 定位](#6-jstack--arthas-定位)
7. [Java 常见框架网络参数速查](#7-java-常见框架网络参数速查)
8. [核心要点](#8-核心要点)
9. [参考来源](#9-参考来源)

## 1. Java 网络异常信息翻译表

| 异常信息 | 含义 | 对应排查 |
|----------|------|---------|
| `ConnectException: Connection refused` | 端口未监听/防火墙 reject | `ss -tlnp` 看监听（[03](03-TCP连接问题排查.md)） |
| `SocketTimeoutException: connect timed out` | 连接超时（包被丢） | 网络/防火墙 drop/队列满 |
| `SocketTimeoutException: Read timed out` | 发送后等待响应超时 | **服务端处理慢**（应用/DB/GC） |
| `Connection reset by peer` | 对端 RST | 连接被中断/超时回收/负载均衡 |
| `Broken pipe` | 已关闭连接上继续写 | 连接被对端关闭后复用 |
| `UnknownHostException` | DNS 解析失败 | [04-DNS与域名问题排查](04-DNS与域名问题排查.md) |
| `SSLHandshakeException` | TLS 握手失败 | 证书/协议（[05](05-HTTP层问题排查.md)） |
| `NoRouteToHostException` | 路由不可达 | 网络层/防火墙 |
| `Too many open files` | 文件句柄耗尽 | **连接泄漏**（CLOSE_WAIT 堆积） |

> 🎯 **异常信息即线索**：Java 异常类型直接对应排查方向——"read timed out"查服务端处理，"reset"查连接生命周期，"refused"查端口监听。

## 2. 连接池问题

### 2.1 连接池是什么

```text
连接池 = TCP 连接复用：
  避免每次请求都三次握手（性能）
  控制连接数上限（资源保护）

Java 常见连接池：
  HTTP：HttpClient 连接池 / Apache HttpComponents PoolingHttpClientConnectionManager
  数据库：HikariCP / Druid
  Redis：JedisPool / Lettuce
```

### 2.2 高频问题

| 问题 | 现象 | 根因与解决 |
|------|------|-----------|
| 连接池耗尽 | `Pool exhausted` / 请求排队 | 泄漏（未归还）+ 峰值超容量；查代码归还逻辑 |
| 空闲连接被服务端回收 | 复用时报 RST/reset | 池的空闲超时 > 服务端 keepalive；配验证 |
| 池参数不合理 | 偶发慢/超时 | minimumIdle 过小（冷启动慢）、maximumPoolSize 过大（资源浪费） |
| 连接验证缺失 | 复用坏连接 | `testOnBorrow`/`validationQuery` |

```yaml
# HikariCP 网络相关参数
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 3000        # 借连接超时（3s）
      validation-timeout: 1000
      connection-test-query: SELECT 1
      max-lifetime: 1800000           # 连接最大存活（30 分钟，须 < 服务端超时）
      keepalive-time: 300000           # 空闲保活探测（5 分钟）
```

> 💡 **池参数铁律**：`max-lifetime` < 服务端/防火墙连接超时；`keepalive-time` 防空闲被回收；`connection-test-query` 防复用坏连接——三件套防"偶发 reset"。

## 3. HTTP 客户端配置

```java
// JDK HttpClient（现代推荐）
HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))      // 连接超时
        .version(HttpClient.Version.HTTP_2)
        .build();

// 请求级
HttpRequest request = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(10))            // 响应超时
        .build();
```

| 配置 | 建议 | 说明 |
|------|------|------|
| connectTimeout | 3-5s | 建连失败快速失败 |
| responseTimeout | 10-30s | 按下游 SLA 设置 |
| 连接池 | 复用 + 空闲回收 | 防 TIME_WAIT 堆积 |
| 重试 | 幂等才重试 | 见 [05](05-HTTP层问题排查.md) |
| 日志 | 记录 URL/耗时/状态码 | 排障基本盘 |

> ⚠️ **JDK HttpClient 默认不重试**（除幂等 GET 的连接级重试）——需要业务重试自己实现（有限次 + 退避 + 幂等键）。

## 4. 数据库连接池网络问题

| 现象 | 根因 | 解决 |
|------|------|------|
| `Communications link failure` | 连接中断/MySQL 超时 | `max-lifetime` < MySQL `wait_timeout`；keepalive |
| 连接池耗尽 | 慢 SQL 占满连接 | 慢 SQL 优化 + 池扩容（治标） |
| 偶发超时 | 网络抖动/主从切换 | 连接验证 + 重连配置 |
| `Connection is not available` | 池空 + 借连接超时 | 查泄漏 + 峰值容量评估 |

```sql
-- 服务端视角：MySQL 连接与超时
SHOW VARIABLES LIKE 'wait_timeout';    -- 空闲连接回收时间（默认 28800s）
SHOW STATUS LIKE 'Threads_connected';  -- 当前连接数
SHOW PROCESSLIST;                      -- 连接状态与慢查询
```

> 💡 数据库连接池网络问题 = 池参数与 MySQL 超时参数的"对齐问题"：**应用侧 max-lifetime < MySQL wait_timeout**，否则空闲连接被服务端回收，复用时报错。

## 5. SSL/HTTPS 异常

| 异常 | 根因 | 解决 |
|------|------|------|
| `PKIX path building failed` | 证书链不可信（自签名/缺中间证书） | 导入 CA 到 truststore |
| `Certificate expired` | 证书过期 | 续期 + 监控 |
| `No subject alternative names` | 证书 SAN 不含访问域名 | 换证书/用匹配域名 |
| `Received fatal alert: protocol_version` | TLS 版本不兼容 | 对齐 TLS 版本 |
| `unable to find valid certification path` | JVM truststore 缺证书 | `keytool -importcert` |

```bash
# 导入证书到 JVM truststore
keytool -importcert -alias api-cert \
  -file cert.pem -keystore $JAVA_HOME/lib/security/cacerts \
  -storepass changeit

# 调试 SSL（打印握手细节）
java -Djavax.net.debug=ssl:handshake -jar app.jar
```

> ⚠️ **生产纪律**：不用 `--verify=no`/禁用校验解决问题——正确做法是**把证书导入 truststore**（自签名内部证书的标准姿势）。

## 6. jstack / Arthas 定位

### 6.1 jstack：线程状态看网络

```bash
jstack <pid> > thread.txt

# 网络相关状态：
#   RUNNABLE + SocketInputStream.read ← 正在等网络响应（可能慢）
#   WAITING + LockSupport.park ← 连接池借连接等待
#   BLOCKED ← 锁等待（可能拖慢网络处理）
```

| 线程状态 | 网络含义 |
|----------|---------|
| `SocketInputStream.read` | 等待对端响应（readTimeout 内） |
| `connect` 相关 | 正在建连 |
| 大量 `park` 在连接池 | **连接池耗尽**（借不到连接） |
| 线程数暴涨 | 请求堆积（下游慢/池耗尽） |

### 6.2 Arthas 实时定位

```bash
# 查看线程（含等待连接池的）
thread -n 3

# 监控方法耗时（定位慢调用）
trace com.demo.OrderService create

# 查看 JVM 网络相关统计
vmoption | grep -i network
```

> 🎯 **组合拳**：`ss -tnp`（连接状态）+ `jstack`（线程在等什么）——"连接在但请求慢"用 jstack 看线程是否卡在 read/池等待。

## 7. Java 常见框架网络参数速查

| 框架 | 参数 | 位置 |
|------|------|------|
| Spring Boot（服务端） | `server.tomcat.accept-count` / `connection-timeout` / `max-connections` | application.yml |
| JDK HttpClient | `connectTimeout` / `.timeout()` | 代码 |
| RestTemplate/OkHttp | `connectTimeout`/`readTimeout`（OkHttp 默认 10s） | 代码/工厂 |
| HikariCP | `connection-timeout`/`max-lifetime`/`keepalive-time` | application.yml |
| Redis（Lettuce） | `spring.data.redis.timeout` / 池参数 | application.yml |
| Kafka | `request.timeout.ms` / `connections.max.idle.ms` | producer/consumer 配置 |

> 💡 排查前先查"当前配置值"：很多"网络问题"其实是**默认超时参数与业务不匹配**（如 OkHttp 默认 readTimeout 10s，下游偶发慢即超时）。

## 8. 核心要点

> 🎯 **核心要点**：
> - 异常翻译表是第一步：refused/timeout/reset/handshake 各有明确指向；
> - 连接池三件套：max-lifetime < 服务端超时、keepalive 防回收、验证查询防坏连接；
> - 池耗尽先查泄漏（归还逻辑）再谈扩容；
> - DB 连接池问题 = 应用参数与 MySQL 超时的对齐问题；
> - SSL 正确姿势：证书导入 truststore（不是禁用校验）；
> - jstack 看线程卡点（read/park/connect）+ ss 看连接状态 = 双视角定位；
> - 排查前先确认框架超时配置——很多问题只是参数与业务不匹配。

## 9. 参考来源

- [JDK HttpClient 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpClient.html)
- [HikariCP 配置文档](https://github.com/brettwooldridge/HikariCP)
- [JDK 网络调试（javax.net.debug）](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html)
- [Arthas 官方文档](https://arthas.aliyun.com/doc/)

---

**下一模块**：[07-容器与微服务网络排查](07-容器与微服务网络排查.md)　/　**返回总览**：[00-总览](00-网络问题排查总览.md)
