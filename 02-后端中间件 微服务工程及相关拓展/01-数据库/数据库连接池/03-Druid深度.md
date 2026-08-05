# 03 Druid 深度

> Druid（阿里）是功能最全的连接池——StatViewServlet 监控页面、WallFilter SQL 防火墙、慢 SQL 统计，是"连接池 + 监控 + 安全"的一体方案

---

## 📚 目录

1. [Druid 定位与特性全景](#1-druid-定位与特性全景)
2. [监控体系：StatViewServlet 与统计](#2-监控体系statviewservlet-与统计)
3. [WallFilter：SQL 防火墙](#3-wallfiltersql-防火墙)
4. [慢 SQL 与连接监控](#4-慢-sql-与连接监控)
5. [HikariCP vs Druid 选型](#5-hikaricp-vs-druid-选型)

---

## 1. Druid 定位与特性全景

**Druid（阿里开源）**：连接池 + 监控 + 安全一体方案：

```text
Druid 的三大块（区别于纯连接池）：
  ① 连接池：标准池能力（参数/借还/生命周期）
  ② 监控：StatViewServlet 页面 + 统计过滤器（SQL/连接/慢查询）
  ③ 安全：WallFilter SQL 防火墙（防注入/防危险操作）

依赖：
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-starter</artifactId>
    <version>1.2.x</version>
</dependency>
```

**基本配置**：

```yaml
spring:
  datasource:
    druid:
      initial-size: 5          # 初始连接
      min-idle: 5              # 最小空闲
      max-active: 20           # 最大活跃
      max-wait: 60000          # 获取超时（ms）
      time-between-eviction-runs-millis: 60000   # 回收扫描间隔
      validation-query: SELECT 1
      test-while-idle: true    # 空闲校验
```

> 🎯 **核心要点**：Druid = "**连接池 + 监控 + 防火墙三合一**"——**"选 Druid 的理由是监控与安全，不是性能"**（性能 HikariCP 更优）；配置前缀 `spring.datasource.druid.*`。

---

## 2. 监控体系：StatViewServlet 与统计

**StatViewServlet：Druid 的监控页面**：

```yaml
spring:
  datasource:
    druid:
      stat-view-servlet:
        enabled: true          # 开启监控页面
        url-pattern: /druid/*  # 访问路径
        login-username: admin  # 页面登录（安全！）
        login-password: admin123
        reset-enable: false    # 禁止重置（防误操作）
```

**监控页面能看什么**：

```text
① 数据源状态：活跃/空闲连接数、池使用率
② SQL 监控：每条 SQL 的执行次数/耗时/并发（按 SQL 统计）
③ 连接监控：获取连接耗时、归还耗时
④ 慢 SQL：超过阈值（slowSqlMillis）的 SQL 列表
⑤ Web 应用：请求/URI 统计（需 web 统计过滤器）
```

**统计过滤器（StatFilter）**——监控的数据来源：

```yaml
spring:
  datasource:
    druid:
      filter:
        stat:
          enabled: true        # 统计过滤器
          slow-sql-millis: 1000   # 慢 SQL 阈值（1s）
          log-slow-sql: true      # 慢 SQL 打日志
```

> 🎯 **核心要点**：Druid 监控 = "**StatViewServlet 页面 + StatFilter 统计**"——**"慢 SQL 阈值 + 页面看 SQL 耗时分布"是性能排查利器**；页面必须设登录（安全红线）。

---

## 3. WallFilter：SQL 防火墙

**WallFilter：SQL 级安全防线**（连接池层的注入防御）：

```yaml
spring:
  datasource:
    druid:
      filter:
        wall:
          enabled: true        # 防火墙
          config:
            multi-statement-allow: false   # 禁止多语句（防注入放大）
            comment-allow: false           # 禁止注释（防绕过）
            delete-allow: true             # 允许 DELETE
            drop-table-allow: false        # 禁止 DROP TABLE
            truncate-allow: false          # 禁止 TRUNCATE
```

**WallFilter 拦截什么**：

```text
① SQL 注入：基于语义分析（预编译之外的第二道防线）
   - 恒真条件（'1'='1'）、联合查询、堆叠语句
② 危险操作：
   - DROP/TRUNCATE（生产保护）
   - 多语句（防注入放大）
   - 注释绕过（防攻击混淆）
③ 白名单/黑名单：自定义函数/表限制

与预编译的关系：
  预编译：参数化（防注入的主防线）
  WallFilter：SQL 语义检查（纵深防御的第二层）
```

> 🎯 **核心要点**：WallFilter = "**SQL 语义防火墙（预编译之外的第二道防线）**"——**"禁 DROP/多语句 + 注释拦截"是生产保护**；纵深防御（预编译 + 防火墙）是 2026 安全实践（与 `Java有关Hash的一切/05` 注入攻击联动）。

---

## 4. 慢 SQL 与连接监控

**慢 SQL 监控（Druid 特色能力）**：

```yaml
spring:
  datasource:
    druid:
      filter:
        stat:
          slow-sql-millis: 1000      # 阈值
          log-slow-sql: true         # 打日志
          merge-sql: true            # 合并相同结构 SQL（统计准确）
```

```text
慢 SQL 的使用：
  ① 页面看"SQL 监控"：按执行次数/总耗时排序
  ② 慢 SQL 日志：定位阈值以上的 SQL（+ 参数）
  ③ 与 EXPLAIN 联动：慢 SQL → 分析执行计划（SQL调优/ 系统）
  ④ 告警：慢 SQL 频繁 → 索引问题/大表扫描

连接监控：
  获取连接耗时（借出耗时）→ 池压力信号
  归还耗时 → 归还路径异常
  活跃/空闲趋势 → 泄漏与容量
```

> 🎯 **核心要点**：慢 SQL = "**Druid 的差异化能力（阈值 + 日志 + 页面统计）**"——**"慢 SQL 监控是连接池层的第一手性能数据"**（比数据库慢查询日志更贴近应用）；与 `SQL调优/` 联动形成闭环。

---

## 5. HikariCP vs Druid 选型

**完整对比**（2026 选型决策）：

| 维度 | HikariCP | Druid |
|------|:--------:|:-----:|
| 性能 | ✅ 最快 | 中 |
| 监控 | 基础（Metrics） | **✅ 丰富（页面/慢 SQL）** |
| 防火墙 | ❌ | ✅ WallFilter |
| 生态 | Spring Boot 默认 | 阿里系（Nacos/Seata 联动） |
| 依赖 | 轻 | 中（功能多） |
| 维护 | 活跃 | 维护放缓（1.2.x 稳定） |

**选型决策树**：

```text
需要监控页面/慢 SQL/SQL 防火墙？
├── 是 → Druid（阿里系 + 监控 + 安全一体）
└── 否 → HikariCP（Spring Boot 默认 + 最快）

2026 共识：
  默认 HikariCP（性能 + 生态）
  需要"可视化监控 + 防火墙" → Druid
  阿里系框架（Nacos/Seata）→ Druid 联动更顺
  混合：HikariCP + 外部监控（Prometheus/Micrometer）
```

**混合方案**（2026 实践）：

```text
HikariCP + Micrometer（active/pending 指标）
  → 指标进 Prometheus + Grafana
  → 慢 SQL 由数据库慢查询日志/APM 承担
→ 不需要 Druid 页面也能获得监控（拆分配置）
```

> 🎯 **核心要点**：选型 = "**默认 HikariCP、要监控防火墙选 Druid**"——**"监控可以外置（Micrometer+Prometheus）则 HikariCP 足够"是 2026 结论**；阿里系联动场景 Druid 更顺（Nacos/Seata）。

---

**下一模块**：[04-参数调优与压测](./04-参数调优与压测.md) / **返回总览**：[00-数据库连接池知识体系总览](./00-数据库连接池知识体系总览.md)
