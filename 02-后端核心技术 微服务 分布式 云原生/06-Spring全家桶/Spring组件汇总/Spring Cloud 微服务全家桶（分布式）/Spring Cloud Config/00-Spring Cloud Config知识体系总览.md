# 00 Spring Cloud Config 知识体系总览

> 组件卡片：Spring Cloud Config 是什么、版本现状、能做什么、与对照体系如何衔接——**微服务外部化配置中心（Server 集中管理 + Client 启动拉取）**，Git 仓库是默认存储后端；5.x 主线。对照体系见 [Nacos 配置中心](../Spring Cloud Alibaba（阿里实现，属于 Spring Cloud 生态实现）/Nacos：注册中心 + 配置中心/00-Nacos知识体系总览.md) 与 [Spring Cloud Bus](../Spring Cloud Bus/00-Spring Cloud Bus知识体系总览.md)（动态刷新广播配套）。

---

## 📚 目录

1. [组件一句话定位](#1-组件一句话定位)
2. [版本现状（2026-08）](#2-版本现状2026-08)
3. [能力地图](#3-能力地图)
4. [与对照体系的映射](#4-与对照体系的映射)
5. [快速上手 3 步](#5-快速上手-3-步)
6. [速查导航](#6-速查导航)
7. [学习路线推荐](#7-学习路线推荐)
8. [核心概念速查](#8-核心概念速查)

---

## 1. 组件一句话定位

**Spring Cloud Config 是 Spring Cloud 官方的外部化配置中心**——Config **Server** 从存储后端（默认 Git 仓库）读取配置并通过 HTTP 暴露，微服务作为 Config **Client** 启动时拉取配置——解决"50 个服务的配置散落在各环境、改配置要逐个改逐个重启"的治理问题。

```text
核心心智模型：
    Git 配置仓库（application.yml / order-service.yml，按环境分文件）
        │  Server 启动时 clone 到本地工作目录
        ▼
    Config Server（端口 8888）── HTTP: /{application}/{profile}/{label}
        ▲                                        │
        │  spring.config.import=configserver:     │ 启动时拉取
        │                                        ▼
    ┌─────┬─────┬─────┬─────┐              Config Client（各微服务）
    │ 服务A │ 服务B │ 服务C │ …  │ ◄──── 每个实例启动时都来拉
    └─────┴─────┴─────┴─────┘
        动态刷新：/actuator/refresh（单实例）或 Bus 广播（全网）
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring Cloud 官方组件（配置中心三件套：Server/Client/Encryption） |
| 版本线 | **5.x 主线（5.0.4 为当前主流，2025.1/Oakwood，Boot 4）**；4.3.x 供 Boot 3.5 |
| 存储后端 | **Git 默认**，另有 Vault/JDBC/Redis/MongoDB/AWS/Google/复合后端 |
| 客户端接入 | **Config Data（spring.config.import，Boot 2.4+ 默认）**；bootstrap 为遗留方式 |
| 动态刷新 | /actuator/refresh（单实例）+ **Bus 广播（全网，配套件）** |
| 特色 | **{cipher} 加密配置**（对称/非对称、多密钥轮换）、多环境 Profile/Label |
| 定位 | 只做"配置的集中管理与下发"——不负责注册发现、不负责限流 |

### 1.1 Config 解决什么问题

**① 配置散落治理**：没配置中心时，改一个 Redis 地址要改 50 个服务的配置文件并逐个重启；Config 把配置**集中在 Git 仓库**，一处修改、按环境（dev/test/prd）分文件管理（[02 篇](02-快速开始与配置读取速查.md)）。

**② 配置的版本化管理**：Git 仓库天然支持**历史回溯、分支（label）、评审（MR）**——配置变更像代码一样可审计可回滚。

**③ 敏感配置加密**：数据库密码等敏感项在 Git 里以 **{cipher} 密文**存储，Config Server 解密后下发（[06 篇](06-加密解密与密钥管理速查.md)）。

| 维度 | 没有 Config | 有 Config |
|------|-----------|----------|
| 配置存放 | 散落各服务 jar 内/各环境 | 集中 Git 仓库，环境分文件 |
| 改配置 | 逐个服务改+重启（50 次） | 改 Git 一处 → 刷新（+Bus 全网广播） |
| 历史版本 | 无（不知道谁改过） | Git 全量历史可回滚 |
| 敏感信息 | 明文躺在代码里 | {cipher} 密文 + 服务端解密 |
| 灰度/回滚 | 手动 | label（分支）切换 |

> 🎯 判断标准一句话：**"微服务数量 > 3 且配置要按环境区分管理？"**——是，上 Config；但**如果你已在 SCA 生态用 Nacos 配置中心，不需要 Config**（Nacos 内置长轮询推送，[08 篇](08-生产实践与选型避坑.md) 选型对比）。

### 1.2 适用与不适用场景

| 场景 | 适合？ | 说明 |
|------|:---:|------|
| 多微服务集中配置管理 | ✅ | 主战场（Git 后端） |
| 按环境区分配置 | ✅ | profile（dev/test/prd）+ label（分支） |
| 配置历史回溯/审计 | ✅ | Git 天然版本化 |
| 敏感配置加密 | ✅ | {cipher} + 对称/非对称密钥 |
| 配置变更广播 | ✅ | Bus 配套（全网刷新） |
| SCA 生态（Nacos 配置中心） | ❌ | Nacos 内置推送，Config 多余 |
| 动态规则实时下发（限流/熔断规则） | ⚠️ | 能刷但要重启或 refresh；Sentinel 控制台体验更好 |
| 超大配置/频繁变更 | ⚠️ | Git 拉取模式，秒级刷新延迟，不是毫秒级推送 |

> ⚠️ **最大认知误区**：把 Config 当"实时配置推送系统"——**Config 是"启动拉取 + 手动/事件触发刷新"模型，不是推送模型**；毫秒级热更新该选 Nacos（长轮询）或 Apollo（推送），Config + Bus 是"广播事件让各实例自己刷新"（[08 篇](08-生产实践与选型避坑.md) 刷新模型对比）。

### 1.3 与配置中心方案对照

| 方案 | 存储 | 推送机制 | 动态刷新 | 加密 | 生态 |
|------|------|---------|:---:|:---:|------|
| **Spring Cloud Config** | Git 默认，后端可换 | 启动拉取 + refresh 事件 | ✅（refresh/Bus） | ✅ {cipher} | **官方 Cloud** |
| **Nacos 配置中心** | 内嵌存储 | **长轮询拉取（秒级）** | ✅ 内置 | ⚠️ 基础 | SCA |
| Apollo | 数据库 | 推送 + 长轮询 | ✅ | ✅ | 携程（独立） |
| Consul KV | KV 存储 | Watch 通知 | ✅ | ❌ | HashiCorp |
| Vault | 秘钥引擎 | API 拉取 | ⚠️ | ✅（本职） | HashiCorp（常与 Config 复合） |

> 🎯 面试必答：**"Config 和 Nacos 配置中心什么区别？"**——Config：**Git 存储 + 启动拉取**，刷新靠 refresh 端点/Bus 广播事件，需配套组件；Nacos：**内嵌存储 + 长轮询推送**，配置变更服务端通知客户端秒级生效，**不依赖额外组件**——功能上 Nacos 更省事，但 Config 的优势是 **Git 版本化 + 加密体系 + 官方生态**（[08 篇](08-生产实践与选型避坑.md) 全维度）。

## 2. 版本现状（2026-08）

| 版本线 | 状态 | 关键点 |
|--------|------|--------|
| **5.x（5.0.4 主流）** | **当前主线** | Spring Cloud **2025.1.x（Oakwood）**、Boot 4.0/4.1、Framework 7；**Jackson 2→3**、移除全部 deprecated API |
| 4.3.x（4.3.4） | 存量（EOL） | Spring Cloud 2025.0.x（Northfields）、Boot 3.5；OSS 已于 2026-06-30 结束 |
| 4.2.x（4.2.4） | 存量（EOL） | Spring Cloud 2024.0.x（Moorgate）、Boot 3.4；**多 label 支持（4.2.0 起）** |
| 4.1.x（4.1.7） | 存量 | Spring Cloud 2023.0.x（Leyton）、Boot 3.2/3.3 |
| 4.0.x | 老存量 | Spring Cloud 2022.0.x（Kilburn）、Boot 3.0/3.1 |
| 3.x | 老存量 | Spring Cloud 2021.0.x（Jubilee）、Boot 2.6/2.7（javax） |
| 1.x/2.x | 废弃 | Hoxton/Ilford 时代 |

> ⚠️ **版本策略（2026 起）**：**新项目 Boot 4.x + Cloud 2025.1.x + Config 5.0.x**；存量 Boot 3.5 用 4.3.x（**OSS 已 EOL，建议尽快升级**）；Config 版本与 SCCB/Bus 同为 release train 成员，**统一跟 BOM 走**（[01 篇](01-模块清单与版本矩阵.md) 矩阵）。

### 2.1 5.0 关键变化（4.3.x → 5.0.x 迁移要点）

| 变化 | 说明 |
|------|------|
| Jackson 2 → 3 | JSON 处理迁移（编译期注意 ObjectMapper 相关代码） |
| 移除全部 deprecated API | 大版本清理；若有 @Deprecated 用法先查官方迁移说明 |
| Boot 4 对齐 | jakarta 全系、Framework 7、AOT 支持延续 |
| 客户端 API 不变 | spring.config.import / spring.cloud.config.* 属性稳定（迁移主要是换版本） |
| 4.2.0+ 特性保留 | 多 label（send-all-labels）、Config Data 两步加载 |

### 2.2 关键演进时间线

| 版本 | 里程碑 |
|------|--------|
| 1.x（2014-2016） | 从 Netflix Archaius 演进，Git 后端 + bootstrap 模式诞生 |
| 2.x（2019-2020） | Boot 2.2+ 配套，/actuator/refresh |
| 3.x（2021） | 对齐 Cloud 2021.0（Jubilee），Boot 2.6/2.7 |
| 4.0（2022.12） | **Boot 3 首线**：jakarta、AOT/原生镜像支持 |
| 4.1-4.3（2023-2025） | Leyton/Moorgate/Northfields 维护线；**4.2.0 多 label**、4.3.x 兼容迭代 |
| 5.0（2025.11） | **Oakwood 大版本**：Jackson 3、移除 deprecated、Boot 4 |

## 3. 能力地图

| 能力域 | 能力点 | 对应机制 |
|--------|--------|---------|
| 集中配置 | Git 仓库统一管理 | Git 后端（clone + 工作目录） |
| 多环境 | 按环境分配置 | profile（application-{profile}.yml）+ label（分支） |
| 客户端拉取 | 启动即拉 | **Config Data（spring.config.import）** |
| 动态刷新 | 单实例刷新 | `/actuator/refresh` + @RefreshScope |
| 全网刷新 | 广播刷新事件 | **Bus 集成**（bus-refresh，[05 篇](05-动态刷新与Bus集成速查.md)） |
| 敏感配置 | 加密存储与解密下发 | {cipher} + /encrypt /decrypt + 密钥管理（[06 篇](06-加密解密与密钥管理速查.md)） |
| 多后端 | 按场景换存储 | Vault/JDBC/Redis/MongoDB/AWS/复合（[03 篇](03-配置仓库后端全景.md)） |
| 版本化 | 配置可回溯可回滚 | Git 分支/label/历史 |
| 高可用 | 多 Server 部署 | 无状态 + 共享 Git；Client 多 URI 容错 |
| 安全 | 访问控制 | Basic Auth/TLS/客户端凭证（[07 篇](07-安全认证与生产运维.md)） |
| 健康检查 | 后端可达性 | /actuator/health + 自定义健康指示器 |

### 3.1 能力边界：Config 不做什么

| 能力边界 | 谁来做 | 说明 |
|---------|--------|------|
| 服务注册发现 | Nacos/Eureka | Config 只下发配置 |
| 毫秒级热更新 | Nacos（长轮询）/ Apollo（推送） | Config 是"启动拉取 + 事件刷新"，秒级 |
| 规则实时治理（限流等） | Sentinel 控制台 | Config 能存规则但无控制台体验 |
| 分布式事务 | Seata | 与配置无关 |
| 业务消息 | MQ | 与配置无关 |

> ⚠️ **常见归因错误**：改配置后"刷新了但没生效"怪 Config Server"没推送"——**排查路径："Git 仓库有没有改 → Config Server 有没有刷到新版本（/actuator/env 看 version）→ 客户端 refresh 事件有没有到 → @RefreshScope Bean 有没有重建"**——链路每段独立验证（[08 篇](08-生产实践与选型避坑.md) 排错表）。

## 4. 与对照体系的映射

| 速查文档 | 对照/深度体系 |
|---------|-------------|
| 02-配置读取 | [Spring Boot 外部化配置](../../../../Spring框架核心/)（配置优先级）、[Spring Cloud Bus](../Spring Cloud Bus/00-Spring Cloud Bus知识体系总览.md)（刷新配套） |
| 03-仓库后端 | [Nacos 配置中心](../Spring Cloud Alibaba（阿里实现，属于 Spring Cloud 生态实现）/Nacos：注册中心 + 配置中心/00-Nacos知识体系总览.md)（存储对比）、[消息队列理论与实战](../../../../03-消息队列/消息队列理论与实战/00-消息队列知识体系总览.md)（Bus 广播底座） |
| 05-动态刷新 | [Spring Cloud Bus 系列](../Spring Cloud Bus/00-Spring Cloud Bus知识体系总览.md)（全网刷新原理） |
| 06-加密 | [密码学知识体系](../../../../../01-底层根基-Java核心底座/数学基础/密码学/)（对称/非对称原理） |
| 08-选型 | [Nacos 配置中心](../Spring Cloud Alibaba（阿里实现，属于 Spring Cloud 生态实现）/Nacos：注册中心 + 配置中心/00-Nacos知识体系总览.md)（vs Nacos 全维度） |

> 💡 分工约定：**本系列回答"Config 怎么配、配置怎么拉、怎么刷、怎么加密、怎么选型"**；Git 本身的用法在 [Linux/Git 工具体系](../../../../../02-后端核心技术 微服务 分布式 云原生/07-工程运维基础/) 对照，动态刷新的消息底座在 [Spring Cloud Bus](../Spring Cloud Bus/00-Spring Cloud Bus知识体系总览.md) 对照。

## 5. 快速上手 3 步

**① 建配置仓库**（Git 仓库里放配置，按环境分文件）：

```yaml
# config-repo/order-service.yml
server:
  port: 8081
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/order
    username: root
    password: root

# config-repo/order-service-dev.yml（dev 环境覆盖项）
spring:
  datasource:
    url: jdbc:mysql://dev-db:3306/order
```

**② 起 Config Server**（新 Spring Boot 项目加依赖）：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

```java
@SpringBootApplication
@EnableConfigServer          // ★ 声明为配置服务器
public class ConfigServerApplication {
    public static void main(String[] args) { SpringApplication.run(ConfigServerApplication.class, args); }
}
```

```yaml
# application.yml（Config Server 自身）
server:
  port: 8888
spring:
  application:
    name: config-server
  cloud:
    config:
      server:
        git:
          uri: https://github.com/you/config-repo   # ★ 配置仓库地址
```

**③ 客户端拉取**（任意微服务加依赖 + 一行配置）：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

```yaml
# 客户端 application.yml
spring:
  config:
    import: optional:configserver:http://localhost:8888   # ★ 启动时拉配置
  application:
    name: order-service                                    # ★ 按这个名字找配置
```

> 🎯 跑通即及格：**① 访问 `http://localhost:8888/order-service/dev` 能看到 JSON 配置；② 起客户端，日志出现 `Located property source` / 接口读到配置仓库里的值；③ 改 Git 配置 → 客户端 `/actuator/refresh` → 新值生效**——三件事都通，主链路打通（[02 篇](02-快速开始与配置读取速查.md)）。

### 5.1 三步验证与排障起点

| 步骤 | 做法 | 预期 |
|------|------|------|
| ① Server 可读 | 浏览器访问 /order-service/dev | 返回含 propertySources 的 JSON |
| ② 客户端拉到 | 起客户端，看日志 | `Located property source: [configserver:...]` |
| ③ 刷新生效 | 改 Git → refresh 端点 | @RefreshScope Bean 值更新（无需重启） |

> 💡 排障起点：**先查 Server 端 `/order-service/dev` 通不通（404 = 仓库路径/文件名不对），再查客户端 `spring.config.import` 和 `spring.application.name` 匹配**（[08 篇](08-生产实践与选型避坑.md) 排错表）。

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单与版本矩阵](01-模块清单与版本矩阵.md) | artifact 坐标（config-server/config-client/starter）、包结构、**版本矩阵（Config↔Cloud↔Boot）**、5.0 变化 |
| [02-快速开始与配置读取速查](02-快速开始与配置读取速查.md) | Git 后端跑通、profile/label 多环境、配置查找顺序、三种配置格式 |
| [03-配置仓库后端全景](03-配置仓库后端全景.md) | Git/文件系统/Vault/JDBC/Redis/MongoDB/AWS/Google/复合/自定义后端 |
| [04-客户端原理：配置加载与导入](04-客户端原理：配置加载与导入.md) | Config Data 两步加载、import vs bootstrap、fail-fast/重试/discovery、多 label、优先级 |
| [05-动态刷新与Bus集成速查](05-动态刷新与Bus集成速查.md) | @RefreshScope、refresh 端点、Bus 广播、Webhook 自动刷新、刷新排查 |
| [06-加密解密与密钥管理速查](06-加密解密与密钥管理速查.md) | {cipher}、/encrypt /decrypt、对称/非对称、invalid 机制、多密钥轮换 |
| [07-安全认证与生产运维](07-安全认证与生产运维.md) | Server 认证、客户端凭证、健康检查、高可用、AOT/原生镜像 |
| [08-生产实践与选型避坑](08-生产实践与选型避坑.md) | vs Nacos 选型、坑位表、刷新链路排查、升级路径 |

### 6.1 阅读顺序建议

- **第一次接触**：02（快速跑通）→ 05（动态刷新）→ 跑通全链路；
- **项目实战**：02 → 07 安全 → 08 避坑；
- **面试冲刺**：04 原理（加载机制/优先级）→ 08 vs Nacos → 06 加密；
- **架构设计**：03 后端选型 → 08 刷新模型对比。

### 6.2 前置知识建议

| 前置主题 | 所在文档 | 与本系列的关系 |
|---------|---------|--------------|
| Spring Boot 外部化配置 | [Spring 框架核心](../../../../Spring框架核心/) | 配置优先级/@ConfigurationProperties 是 Client 端基础 |
| Git 基础 | [Git 工具体系](../../../../../02-后端核心技术 微服务 分布式 云原生/07-工程运维基础/) | Config 默认存储后端 |
| Bus 事件广播 | [Spring Cloud Bus](../Spring Cloud Bus/00-Spring Cloud Bus知识体系总览.md) | 全网刷新的配套件 |
| 对称/非对称加密 | [密码学知识体系](../../../../../01-底层根基-Java核心底座/数学基础/密码学/) | 加密解密篇的数学基础 |

## 7. 学习路线推荐

| 路线 | 适用 | 顺序 |
|------|------|------|
| 入门速查 | 会用 Spring Boot | 00 总览 → 02 快速跑通 → 05 刷新 |
| 项目实践 | 上生产 | 02 → 07 安全 → 08 避坑 |
| 面试冲刺 | 全考点 | 04 原理 → 08 vs Nacos → 06 加密 |
| 架构设计 | 选型负责人 | 03 后端 → 08 刷新模型 → 00 能力地图 |

## 8. 核心概念速查

| 术语 | 一句话解释 |
|------|-----------|
| Config Server | 配置服务器：从后端读取配置、HTTP 暴露 |
| Config Client | 配置客户端：启动时拉取配置的微服务 |
| Git 后端 | 默认存储：clone 仓库按 应用/环境/分支 找文件 |
| spring.config.import | 客户端导入配置的方式（`optional:configserver:`） |
| profile | 环境：application-{profile}.yml 区分 dev/test/prd |
| label | 分支/标签：按 Git 分支取配置（灰度/回滚） |
| @RefreshScope | 刷新时重建 Bean 的注解 |
| /actuator/refresh | 单实例刷新端点 |
| bus-refresh | Bus 广播刷新端点（全网） |
| {cipher} | 加密值前缀（Config Server 解密后下发） |
| /encrypt /decrypt | 加解密端点 |
| encrypt.key / keyStore | 对称密钥 / 非对称（RSA 证书库） |
| 配置优先级 | 本地配置 < 远程配置（Config Data 加载后覆盖） |

---

**下一模块**：[01-模块清单与版本矩阵](01-模块清单与版本矩阵.md)　**返回总览**：本页

**【参考来源】**：[Spring Cloud Config 5.0.4 官方参考文档](https://docs.spring.io/spring-cloud-config/reference/)、[Spring Cloud 2025.1.0（Oakwood）发布公告](https://spring.io/blog/2025/11/25/spring-cloud-2025-1-0-aka-oakwood-has-been-released/)、[Spring Cloud 2025.1.1（Oakwood 兼容修复）](https://spring.io/blog/2026/01/29/spring-cloud-2025-1-1-aka-oakwood-has-been-released)、[spring-cloud-config-server 版本（Snyk/Sonatype）](https://security.snyk.io/package/maven/org.springframework.cloud:spring-cloud-config-server/versions)
