# 05 连接池与 DataSource

> 创建连接很贵，所以有了连接池——DataSource 是标准接口，HikariCP 是 Spring Boot 默认，Druid 是阿里方案，参数调优是性能关键

---

## 📚 目录

1. [为什么需要连接池](#1-为什么需要连接池)
2. [DataSource：连接工厂的标准接口](#2-datasource连接工厂的标准接口)
3. [HikariCP：Spring Boot 默认连接池](#3-hikaricpspring-boot-默认连接池)
4. [Druid：阿里方案](#4-druid阿里方案)
5. [连接池核心参数调优](#5-连接池核心参数调优)
6. [连接池排查速查](#6-连接池排查速查)

---

## 1. 为什么需要连接池

**创建连接的昂贵成本**（连接池存在的原因）：

```text
一次连接创建（DriverManager.getConnection）：
  ① 网络握手（TCP 连接）
  ② 数据库认证（用户名/密码验证）
  ③ 会话初始化（权限/变量/时区设置）
  → 耗时：毫秒 ~ 几十毫秒（高并发时放大）

无连接池的后果：
  100 并发 × 每次创建连接 = 100 次握手/认证
  → 数据库连接数暴涨（上限耗尽）+ 延迟飙升

连接池的解决方案：
  预先创建 N 个连接 → 复用（getConnection 是"借"，close 是"还"）
  → 创建成本只付一次（初始化）
```

**连接池的三个价值**：

| 价值 | 说明 |
|------|------|
| 性能 | 复用连接（免握手/认证） |
| 控制 | 限制最大连接数（防压垮数据库） |
| 管理 | 空闲回收/泄漏检测/健康检查 |

> 🎯 **核心要点**：连接池 = "**创建贵（握手+认证）→ 预先建池复用**"——**"getConnection 是借、close 是还"**是理解连接池行为的第一认知（归还而非断开）。

---

## 2. DataSource：连接工厂的标准接口

**DataSource（javax.sql，JDBC 2.0 可选包）**：连接工厂的标准——连接池实现的统一接口：

```java
// 标准接口：唯一核心方法
public interface DataSource {
    Connection getConnection() throws SQLException;
    Connection getConnection(String username, String password) throws SQLException;
}

// 用法（生产标准）：面向接口编程
DataSource ds = new HikariDataSource(config);   // 或 DruidDataSource
try (Connection conn = ds.getConnection()) {    // 从池中"借"
    // 使用
}                                               // TWR 关闭 = "还"回池
```

**DataSource vs DriverManager**：

| 维度 | DriverManager | DataSource（连接池） |
|------|:-------------:|:-------------------:|
| 连接 | 每次新建 | **复用（池化）** |
| 性能 | 差（高并发） | ✅ 好 |
| 控制 | 无限制 | 最大连接/超时 |
| 生产 | ❌ 学习/测试 | ✅ 标准 |

**为什么 Spring/框架都注入 DataSource**：

```text
面向接口：换连接池（HikariCP → Druid）零代码改动
事务协作：DataSourceUtils 从池中取"线程绑定连接"（见 04 模块）
监控：连接池暴露统计（活跃/空闲/等待）
```

> 🎯 **核心要点**：DataSource = "**连接工厂标准（池化连接的抽象）**"——**"生产一律 DataSource（DriverManager 只教学用）"是规范**；面向接口让换连接池零成本（MyBatis/Spring 都注入它）。

---

## 3. HikariCP：Spring Boot 默认连接池

**HikariCP**：Spring Boot 2+ 默认（以"快"著称）：

```yaml
# Spring Boot 配置（HikariCP 默认）
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db?serverTimezone=Asia/Shanghai
    username: root
    password: root
    hikari:
      maximum-pool-size: 10        # 最大连接数（默认 10）
      minimum-idle: 5              # 最小空闲（默认 = max）
      connection-timeout: 30000    # 获取连接超时（默认 30s）
      idle-timeout: 600000         # 空闲超时（默认 10min）
      max-lifetime: 1800000        # 连接最大存活（默认 30min）
      pool-name: MyHikariPool
```

```java
// 手动配置（非 Spring Boot）
HikariConfig config = new HikariConfig();
config.setJdbcUrl(url);
config.setUsername(user);
config.setPassword(pwd);
config.setMaximumPoolSize(10);
DataSource ds = new HikariDataSource(config);
```

**HikariCP 的"快"来自**（面试谈资）：

```text
① 字节码优化（Javassist 生成代理，无反射）
② 连接状态跟踪精简
③ 并发设计（无锁/细粒度锁）
→ 社区基准：比其他池快一个数量级（获取连接的微基准）
```

> 🎯 **核心要点**：HikariCP = "**Spring Boot 默认（快：字节码代理 + 无锁设计）**"——**五参数（max/min/timeout/idle/max-lifetime）是调优基础**；生产优先考虑它（除非需要 Druid 的监控）。

---

## 4. Druid：阿里方案

**Druid（阿里）**：功能最全（监控是核心卖点）：

```yaml
# Spring Boot 配置（需引入 druid-spring-boot-starter）
spring:
  datasource:
    druid:
      initial-size: 5              # 初始连接
      min-idle: 5
      max-active: 20               # 最大活跃
      max-wait: 60000              # 获取超时
      # 监控（Druid 特色）
      stat-view-servlet:
        enabled: true              # 监控页面
        url-pattern: /druid/*
      filter:
        stat:                      # 统计过滤器
          enabled: true
        wall:                      # SQL 防火墙（防注入）
          enabled: true
```

**HikariCP vs Druid 选型**（2026 实践）：

| 维度 | HikariCP | Druid |
|------|:--------:|:-----:|
| 性能 | ✅ 更快 | 中 |
| 监控 | 基础（metrics） | **✅ 丰富（StatViewServlet）** |
| SQL 防火墙 | ❌ | ✅ WallFilter |
| 生态 | Spring Boot 默认 | 阿里系 |
| 选型 | **无监控需求首选** | 需要监控/防火墙 |

> 🎯 **核心要点**：Druid = "**功能最全（监控页 + SQL 防火墙）**"——**"默认 HikariCP、要监控/防火墙选 Druid"**是 2026 选型结论；Druid 的 wall 过滤器是额外的注入防线（但预编译已是主防线）。

---

## 5. 连接池核心参数调优

**核心参数与调优方法**：

| 参数 | 默认 | 调优 | 影响 |
|------|:----:|------|------|
| maximum-pool-size | 10 | **按并发与数据库能力** | 过大压垮 DB、过小排队 |
| minimum-idle | = max | 波动负载可调小 | 空闲成本 |
| connection-timeout | 30s | 业务等待容忍 | 获取超时异常 |
| max-lifetime | 30min | 小于数据库超时（如 MySQL wait_timeout） | 防"死连接" |
| idle-timeout | 10min | 波动场景 | 空闲回收 |

**连接池大小的经验公式**（2026 实践）：

```text
池大小 = 核数 × 2 + 磁盘数（HikariCP 作者建议）
  → 不是"越大越好"（连接是数据库资源）
  → 关键：连接数 × 单连接工作负载 < 数据库处理能力

实际调优路径：
  ① 压测观察：活跃连接峰值（HikariCP metrics）
  ② 排队时间：getConnection 等待 > 阈值 → 池太小
  ③ 数据库侧：连接数上限（max_connections）+ 慢查询
  ④ 经验：多数业务 10-30 够用（数据库 100+ 连接是压力）
```

**连接池与线程池的关系**（重要认知）：

```text
连接池大小 ≤ 线程池大小（每个数据库操作占用一个连接）
→ 线程池 100、连接池 10 → 90 线程在等连接（排队）
→ 调优联动：连接池与线程池一起评估（见 Java多线程/06）
```

> 🎯 **核心要点**：参数调优 = "**max-pool-size 按并发（核数×2+磁盘起步）+ 排队观察 + 与线程池联动**"——**"连接池 ≤ 线程池"是容量规划的约束**；max-lifetime 必须小于数据库 wait_timeout（防死连接）。

---

## 6. 连接池排查速查

**连接池常见问题**：

| 症状 | 根因 | 排查/解法 |
|------|------|---------|
| 获取连接超时 | 池耗尽 | 活跃连接数/慢 SQL/泄漏 |
| 连接泄漏 | close 未执行 | 泄漏检测（Hikari leakDetectionThreshold） |
| 连接不可用 | 数据库重启/超时 | max-lifetime 配置/健康检查 |
| 池大小失效 | 参数未生效 | 配置确认（actuator） |
| 连接数暴涨 | 慢 SQL 占用 | 慢查询 + 池上限 |

**泄漏检测配置**（HikariCP）：

```yaml
spring:
  datasource:
    hikari:
      leak-detection-threshold: 60000    # 连接借用超 60s 告警（日志）
      # 说明：连接借出超过阈值 → 记录堆栈（定位谁没还）
```

**连接池监控指标**：

```text
活跃连接数（active）—— 池利用率
空闲连接数（idle）
等待获取线程数（pending）—— 排队压力
获取连接平均耗时 —— 池性能
→ 接入 Prometheus（HikariCP metrics 支持）
```

> 🎯 **核心要点**：排查 = "**超时先看池是否耗尽 + 泄漏用 leakDetectionThreshold + 监控活跃/等待**"——**"连接泄漏 = 借了没还（close 未执行）"是最高频事故**（TWR 是根治规范）；池参数与线程池联动评估。

---

**下一模块**：[06-批量操作与批处理](./06-批量操作与批处理.md) / **返回总览**：[00-JDBC知识体系总览](./00-JDBC知识体系总览.md)
