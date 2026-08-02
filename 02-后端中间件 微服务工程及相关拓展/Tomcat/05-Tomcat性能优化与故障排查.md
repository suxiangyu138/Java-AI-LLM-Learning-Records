# 05 - Tomcat 性能优化与故障排查

> 定位：JVM 调优、部署运维、日志分析、常见故障排查、Tomcat 面试题精选——从"能跑"到"能扛"

## 📚 目录

1. [性能优化总览](#1-性能优化总览)
2. [JVM 调优](#2-jvm-调优)
3. [部署与运维](#3-部署与运维)
4. [日志与监控](#4-日志与监控)
5. [常见故障排查](#5-常见故障排查)
6. [Tomcat 面试题精选](#6-tomcat-面试题精选)

---

## 1. 性能优化总览

```
Tomcat 性能优化四层：
  ① JVM 层：堆内存、GC、启动参数
  ② 连接器层：线程池、连接数、IO 模型
  ③ 应用层：数据库连接池、缓存、慢 SQL
  ④ 架构层：Nginx 前置、集群、CDN

⚠️ 面试必答：
"性能优化四层——JVM、连接器、应用、架构；
 先监控再优化（数据驱动），不要盲调参数。"
```

---

## 2. JVM 调优

### 2.1 启动参数

```bash
# catalina.sh 中 JAVA_OPTS 设置
export JAVA_OPTS="
  -Xms1g -Xmx1g                    # ⚠️ 堆初始 = 最大（避免动态伸缩）
  -XX:+UseG1GC                     # G1 垃圾收集器（默认）
  -XX:MaxGCPauseMillis=100         # GC 停顿目标
  -XX:+HeapDumpOnOutOfMemoryError  # ⚠️ OOM 自动堆转储
  -XX:HeapDumpPath=/logs/heap.hprof
  -Xlog:gc*:file=/logs/gc.log:time  # GC 日志
"
```

| 参数 | 说明 |
|------|------|
| -Xms/-Xmx | 堆大小（生产设相等，避免伸缩） |
| -XX:+UseG1GC | G1（JDK 9+ 默认） |
| -XX:MaxMetaspaceSize | 元空间上限（防泄漏） |
| -Djava.security.egd=file:/dev/./urandom | 容器启动加速 |
| -XX:+HeapDumpOnOutOfMemoryError | OOM 自动转储 |

### 2.2 堆大小估算

```
经验公式：
  -Xmx ≈ 物理内存 × 60-70%（留 OS/缓存）
  每连接内存 ≈ 请求处理 + 会话数据

OOM 排查：
  ① 看堆转储（MAT 分析大对象）
  ② 检查 GC 日志（Full GC 频繁？）
  ③ 常见原因：内存泄漏（缓存/连接未关）

⚠️ 面试必答：
"堆大小 = 物理内存 60-70%；OOM 用
 HeapDump + MAT 分析；Full GC 频繁
 是内存压力的第一信号。"
```

---

## 3. 部署与运维

### 3.1 目录结构

```
Tomcat 目录（bin/conf/lib/logs/temp/webapps/work）：
  bin/       启动脚本（startup.sh/catalina.sh）
  conf/      配置（server.xml 等）
  lib/       共享库（JDBC 驱动放这）
  logs/      日志（catalina.out/localhost.log）
  webapps/   应用部署目录
  work/      编译产物（JSP → class）

⚠️ 面试必答：
"目录六件套——bin 启动、conf 配置、
 lib 共享库、logs 日志、webapps 部署、
 work 编译产物。"
```

### 3.2 部署方式

| 方式 | 说明 | 适用 |
|------|------|------|
| 复制 WAR 到 webapps | 自动部署（解压） | 传统 |
| manager 应用 | 管理界面部署 | 开发 |
| 外部目录 | docBase 指向外部 | 生产 |
| Spring Boot 内嵌 | 无需独立 Tomcat | 现代微服务 |

```bash
# 启动/停止/前台运行
./bin/startup.sh              # 后台启动
./bin/shutdown.sh             # 优雅停止
./bin/catalina.sh run         # 前台（看日志）
./bin/catalina.sh stop -force # 强制停止

# 生产建议：systemd 管理
# systemctl start tomcat
```

> 🎯 **要点**：现代部署 = Spring Boot 内嵌（微服务）或独立 Tomcat（传统 WAR）；`catalina.sh run` 前台调试是排查启动问题的标准姿势。

---

## 4. 日志与监控

### 4.1 日志体系

| 日志 | 位置 | 内容 |
|------|------|------|
| catalina.out | logs/ | 主输出（System.out/异常） |
| localhost.log | logs/ | 应用初始化/错误 |
| localhost_access_log | logs/ | 访问日志（AccessLogValve） |
| manager/host-manager | logs/ | 管理应用日志 |

```xml
<!-- 访问日志配置（排查/分析必备） -->
<Valve className="org.apache.catalina.valves.AccessLogValve"
       directory="logs"
       prefix="access_log"
       suffix=".txt"
       pattern="%h %l %u %t &quot;%r&quot; %s %b %D" />
<!-- %D：处理耗时（毫秒）——慢请求排查关键 -->
```

### 4.2 监控手段

```
① 访问日志分析：耗时 %D、状态码分布
② JMX 监控：线程数、连接数、堆内存
   （JConsole/jvisualvm 连接）
③ GC 日志分析：Full GC 频率
④ 压测：ab/JMeter 定位瓶颈
⑤ 实时查看：tail -f catalina.out

⚠️ 面试必答：
"监控四件套——访问日志（耗时/状态）、
 JMX（线程/连接）、GC 日志、压测定位。
 先看数据再优化。"
```

---

## 5. 常见故障排查

### 5.1 故障场景与定位

| 故障 | 现象 | 排查 |
|------|------|------|
| 启动失败 | 端口占用/配置错 | catalina.sh run + localhost.log |
| 连接拒绝 | 线程池满 | 线程数/连接数监控 |
| 内存溢出 | OOM | 堆转储 + MAT |
| 请求慢 | 响应延迟 | 访问日志 %D + 慢 SQL |
| 504 超时 | 应用长时间无响应 | 线程 dump（jstack） |
| 会话丢失 | 重启/集群 | 分布式 Session 方案 |

### 5.2 线程 dump 分析（死锁/卡死）

```bash
# ① 获取线程快照（卡死/死锁排查）
jstack <pid> > thread_dump.txt

# ② 关键信号
#   死锁：Found one Java-level deadlock
#   线程堆积：大量 WAITING/BLOCKED
#   池满：catalina-exec- 线程数 = maxThreads 且全忙

# ③ 多份快照对比（时间差）：
#   同一线程栈不变 → 卡死
#   jstack -l <pid> 连续 3 次，间隔 5 秒

# 常见原因：
#   数据库连接池耗尽（SQL 慢/连接泄漏）
#   锁等待（synchronized 死锁）
#   外部调用超时（第三方接口）

⚠️ 面试必答：
"'请求全卡'排查三步——jstack 看线程状态、
 看数据库连接池是否耗尽、看外部依赖
 是否超时。线程 dump 是卡死问题第一工具。"
```

### 5.3 高并发下的典型瓶颈

```
瓶颈递进：
  ① 连接数满（maxConnections）→ 拒绝连接
  ② 线程池满（maxThreads）→ 请求排队
  ③ 数据库连接池满 → SQL 等待
  ④ GC 频繁 → CPU 飙升

⚠️ 面试必答：
"瓶颈排查从下往上——连接→线程→DB→GC，
 每层看监控数据定位，避免盲目调参。"
```

---

## 6. Tomcat 面试题精选

**Q1: 请求处理流程？**
```
Connector 接收解析 → 四级容器定位（Engine→Host→Context→Wrapper）
→ Valve → Filter 链 → Servlet.service → 响应返回。详见 01 篇。
```

**Q2: Connector 和 Container 的关系？**
```
Connector（Coyote）管通信（HTTP 解析/IO），
Container（Catalina）管业务（Servlet 执行）；
一个 Container 配多个 Connector（HTTP/HTTPS/AJP）。
```

**Q3: NIO 和 BIO 的区别？**
```
BIO 一连接一线程（线程爆炸）；NIO 用 Selector
事件驱动（Acceptor/Poller/Worker 三线程模型），
少量线程服务海量连接。Tomcat 8+ 默认 NIO。
```

**Q4: 线程池怎么调优？**
```
线程数 = 核数 × (1 + 等待/计算比)；IO 密集取 2-4 倍核数；
四级饱和（常驻→扩容→排队→拒绝）；压测验证。
```

**Q5: Session 怎么处理？**
```
JSESSIONID Cookie 机制；失效三时机（超时/invalidate/重启）；
集群用 Spring Session + Redis（现代标准）。
```

**Q6: 怎么配置 HTTPS？**
```
keytool 生成证书 → server.xml SSLEnabled 连接器
→ HTTP 跳转（RewriteValve）→ TLS 1.2+ + HSTS。
```

**Q7: 怎么排查内存溢出？**
```
HeapDumpOnOutOfMemoryError + MAT 分析大对象；
检查 Full GC 频率；常见：缓存/连接泄漏。
```

**Q8: 怎么排查请求卡死？**
```
jstack 线程 dump（连续 3 次对比）→ 死锁/池满/外部超时；
查数据库连接池与慢 SQL。
```

**Q9: Tomcat 10 迁移注意什么？**
```
javax.* → jakarta.* 包名变更（官方迁移工具）；
Java 11+；Servlet 5.0 → 6.x。
```

**Q10: 虚拟线程对 Tomcat 的意义？**
```
Java 21 虚拟线程：阻塞不占平台线程、百万级可创建；
Tomcat 11 支持 → 每请求一虚拟线程（同步风格 + 高并发）。
```

---

> 🎯 **核心要点**：优化排查体系 = **四层优化**（JVM/连接器/应用/架构）+ **JVM 调优**（堆 60-70% + G1 + HeapDump）+ **日志监控**（访问日志 %D + JMX + GC）+ **故障排查**（jstack 卡死/堆转储 OOM/瓶颈递进）。面试十问覆盖全部主线：请求流程、NIO、线程池、Session、HTTPS、排查。

---

**返回总览**：[00-Tomcat总览与核心概念](00-Tomcat总览与核心概念.md) | **上一篇**：[04-Tomcat会话管理与安全](04-Tomcat会话管理与安全.md)
