# 00 Spring Boot 组件总览

> 组件卡片：Spring Boot 是什么、版本现状、能做什么、与深度体系如何衔接——本系列是"组件汇总"速查型文档，深挖见 [Spring Boot 深度知识体系](../../SpringBoot/00-SpringBoot总览与核心概念.md)（9 篇完整教程）

---

## 📚 目录

1. [组件一句话定位](#1-组件一句话定位)
2. [版本现状（2026-08）](#2-版本现状2026-08)
3. [能力地图](#3-能力地图)
4. [与深度体系的映射](#4-与深度体系的映射)
5. [快速上手 3 步](#5-快速上手-3-步)
6. [速查导航](#6-速查导航)
7. [学习路线推荐](#7-学习路线推荐)
8. [核心概念速查](#8-核心概念速查)

---

## 1. 组件一句话定位

**Spring Boot 是 Spring 生态的"应用框架"**——以"约定优于配置 + 自动配置"让 Spring 应用秒级起步，内嵌服务器、开箱即用的 starter 体系与生产级运维能力，是整个 Java 后端生态的默认起点。

```text
核心心智模型：
  约定优于配置（Convention over Configuration）
    ├── 默认内嵌 Tomcat/Netty：加依赖即启动
    ├── starter 体系：一个依赖打包一组能力
    └── application.yml：极少量配置接管默认行为
  自动配置（Auto-Configuration）
    ├── @EnableAutoConfiguration → AutoConfiguration.imports 注册表
    ├── @Conditional*：classpath 有谁，就配谁
    └── 自定义：@ConfigurationProperties + 属性绑定
  生产能力
    ├── Actuator（健康/指标/环境）
    ├── 外部化配置（profile / config.import / 环境变量）
    └── 可观测（Micrometer + OTel）
```

> 🎯 **一句话**：Spring Boot = "把 Spring 的复杂度打包进约定与自动配置"——写代码前零配置，写完后生产可观测。

## 2. 版本现状（2026-08）

| 版本线 | 最新 | 发布 | 支持截止（OSS） | 状态 |
|--------|------|------|----------------|------|
| **4.1.x** | 4.1.0 | 2026-06-10 | 2027-07-31 | **当前主线（新项目首选）** |
| 4.0.x | 4.0.7 | 2026-06-10 | 2026-12-31 | 活跃但将到期（尽快迁 4.1） |
| 3.5.x | 3.5.16 | 2026-06-25 | 2027-06 | 3.x 收官线（存量迁移期） |

**4.0 大版本（2025-11-20 GA）**：Spring Framework 7 / Jakarta EE 11 / Java 17+（支持至 Java 26）——虚拟线程（`spring.threads.virtual.enabled`）、AOP 全面 CGLIB + `@Proxyable`、结构化并发、AOT/GraalVM 强化。

**4.1.0（2026-06-10）新特性**：

- **原生 gRPC**：Spring gRPC 1.1.0 纳入默认依赖管理（server + client 零手动装配）；
- **SSRF 防护**：HTTP 客户端新增 `InetAddressFilter`；
- **OpenTelemetry 强化**：SDK 可关闭、sampler/span 上限/日志上限/批量处理器/OTLP 导出增强；
- **Log4j 滚动升级**：新增按大小/时间/组合/Cron 四种轮转策略；
- 含 4.0.7 全部修复与安全补丁。

> ⚠️ **4.0 → 4.1 升级注意**：4.0 中废弃的类/方法/属性在 4.1 **全部移除**；`layertools` jar 模式移除（用 `tools`）；**Apache Derby 弃用**（建议迁 H2/HSQL）；`-DskipTests` 不再跳过 AOT 测试处理（用 `maven.test.skip`）；jOOQ 3.20 需 Java 21+。

## 3. 能力地图

| 能力域 | 能力 | 代表组件 |
|--------|------|---------|
| 起步 | 内嵌服务器 + starter | spring-boot-starter-web/webflux、内嵌 Tomcat/Netty |
| 自动配置 | 条件装配 | @EnableAutoConfiguration、AutoConfiguration.imports |
| 配置 | 外部化配置 | application.yml、profile、config.import、@ConfigurationProperties |
| 数据 | JDBC/JPA/Redis/MQ 全接入 | starter-data-*、starter-jdbc |
| Web | MVC / WebFlux / 安全 | starter-web、starter-webflux、starter-security |
| 运维 | 健康/指标/审计 | Actuator、Micrometer、OpenTelemetry |
| 测试 | 切片测试 | @SpringBootTest、@WebMvcTest、Testcontainers |
| 部署 | 可执行 jar / 原生镜像 | spring-boot-maven-plugin、GraalVM AOT |
| 云原生 | 配置中心/服务发现对接 | spring-cloud 体系、gRPC（4.1） |

## 4. 与深度体系的映射

| 本卡片模块 | 深度体系对应 | 衔接说明 |
|-----------|-------------|---------|
| 00-04 全卡 | [00-SpringBoot总览与核心概念](../../SpringBoot/00-SpringBoot总览与核心概念.md) | 9 篇完整教程（自动配置原理/配置体系/Web/数据/测试/安全/运维/微服务） |
| 02 核心类 | [01-SpringBoot自动配置原理](../../SpringBoot/01-SpringBoot自动配置原理.md) | 条件装配与启动流程深挖 |
| 03 配置属性 | [02-SpringBoot配置体系](../../SpringBoot/02-SpringBoot配置体系.md) | 配置优先级与绑定机制 |
| 04 集成地图 | [07-SpringBoot生产运维与可观测](../../SpringBoot/07-SpringBoot生产运维与可观测.md) | Actuator 与生产实践 |

> ⚠️ **版本桥接**：深度体系（2026-07 校验）以 **Boot 3.5.x** 为主——阅读时注意 4.x 差异：虚拟线程默认、@Proxyable、spring-boot 模块化、layertools→tools；本卡片的版本现状（4.1.0）以 2026-08 检索为准。

## 5. 快速上手 3 步

```text
① start.spring.io 生成项目（或手写 pom 引 spring-boot-starter-parent + 目标 starter）
② 写一个 @SpringBootApplication 主类 + 一个 @RestController
③ mvn spring-boot:run（或打包 java -jar）→ 访问 localhost:8080
```

```java
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

@RestController
class HelloController {
    @GetMapping("/hello")
    String hello() { return "hello"; }
}
```

> 💡 运行即得：内嵌 Tomcat 自动起（8080）；Actuator 加 `spring-boot-starter-actuator` 即暴露 `/actuator/health`。

## 6. 速查导航

| 卡片 | 内容 | 何时翻 |
|------|------|--------|
| [01-模块清单](01-模块清单.md) | spring-boot 内部模块与 starter 体系 | 加依赖、理解结构 |
| [02-核心类与注解速查](02-核心类与注解速查.md) | 启动/自动配置/配置绑定/测试注解 | 写代码时随手查 |
| [03-配置属性速查](03-配置属性速查.md) | 高频 application.yml 属性 | 写配置时 |
| [04-集成地图与常见问题](04-集成地图与常见问题.md) | 生态集成 + 升级注意 + 排障 | 集成/升级/排障 |

## 7. 学习路线推荐

```text
快速上手：00 → 01 → 05（三步）→ 02 核心类
系统学习：00 → 深度体系 01（自动配置原理）→ 02 配置 → 05 测试 → 07 运维
面试冲刺：00 版本现状（4.x 差异）→ 深度体系 08（微服务进阶与面试题）
```

## 8. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| @SpringBootApplication | 启动类三合一（配置 + 自动配置 + 扫描） |
| Auto-Configuration | 按 classpath 条件自动装配（imports 注册表） |
| starter | 一个依赖 = 一组能力（版本由 BOM 锁定） |
| @ConfigurationProperties | 属性绑定到类型安全配置类 |
| Actuator | 生产运维端点（health/metrics/env） |
| profile | 环境分组配置（dev/prod） |
| 外部化配置 | 属性优先级链（命令行 > 环境变量 > yml） |
| 可执行 jar | 自包含可运行（Boot 插件打包） |

---

> 🎯 **核心要点**：Spring Boot = 约定优于配置 + 自动配置 + starter 生态 + 生产能力四件套；2026-08 版本窗口是 **4.1.0 当前主线、4.0 年底到期、3.5 收官**；升级 4.1 先清"4.0 已废弃 API + layertools + Derby"。

**下一模块**：[01-模块清单](01-模块清单.md)
