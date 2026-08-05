# 00 - Maven与Gradle 知识体系总览

> 🎯 Maven 和 Gradle 是 JVM 生态的两大构建工具 — 从依赖管理到多模块构建、从插件开发到 CI/CD 集成，掌握构建工具是 Java 后端工程化的第一课

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [精通级学习路线](#3-精通级学习路线)

---

## 1. 知识全景

```
Maven与Gradle 精通体系（18个文件）
│
├── 📦 Maven 核心（01-07）
│   ├── 01-Maven核心概念与依赖管理.md    # GAV坐标/依赖传递/Scope/BOM/生命周期
│   ├── 02-Maven环境搭建.md             # JDK配置/安装/settings.xml/镜像
│   ├── 03-Maven核心概念.md             # POM/坐标/仓库体系/插件机制
│   ├── 04-Maven入门实战.md             # 第一个项目/目录结构/pom.xml详解
│   ├── 05-Maven灵活构建.md             # 单模块/多模块/Profile/资源过滤
│   ├── 06-Maven版本管理.md             # 语义化版本/SNAPSHOT/版本锁定
│   └── 07-Maven测试.md                # Surefire/Failsafe/JUnit集成
│
├── 🌐 Maven Web与工程化（08-13）
│   ├── 08-Maven-Web应用开发.md         # WAR项目/Servlet/JSP/Tomcat
│   ├── 09-Maven-企业级Web应用.md       # 多模块架构/父POM/多环境配置
│   ├── 10-Maven-IDE集成.md            # IDEA/Eclipse配置/插件面板
│   ├── 11-Maven-Archetype扩展.md       # 项目模板/自定义Archetype
│   ├── 12-Maven-自定义插件.md          # MOJO开发/生命周期绑定
│   └── 13-Maven-项目站点.md            # Site生成/测试报告/JavaDoc
│
├── 🔧 Gradle 进阶（14-16）
│   ├── 14-Gradle核心概念与Kotlin DSL.md  # 生命周期/DAG/Kotlin DSL/依赖配置
│   ├── 15-Gradle构建Task与依赖管理.md     # Task定义/增量构建/依赖解析
│   └── 16-Gradle多模块构建与插件开发.md   # 多项目/自定义插件/性能优化
│
├── ⚖️ 对比选型（17）
│   └── 17-Maven与Gradle对比选型指南.md    # 场景对比/迁移指南/团队选型
│
└── 📌 00-Maven与Gradle知识体系总览.md      # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | Maven与Gradle知识体系总览 | 全景导航 + 学习路线 | — |
| 01 | Maven核心概念与依赖管理 | GAV坐标/依赖传递/Scope/BOM/生命周期/多模块 | ⭐⭐⭐ |
| 02 | Maven环境搭建 | JDK配置/安装/settings.xml/镜像仓库 | ⭐ |
| 03 | Maven核心概念 | POM结构/坐标系统/仓库体系/插件机制 | ⭐⭐ |
| 04 | Maven入门实战 | 第一个项目/目录结构/pom.xml详解 | ⭐ |
| 05 | Maven灵活构建 | 单模块/多模块/Profile/资源过滤/Spring Boot | ⭐⭐⭐ |
| 06 | Maven版本管理 | 语义化版本/SNAPSHOT vs RELEASE/版本锁定 | ⭐⭐ |
| 07 | Maven测试 | Surefire单元测试/Failsafe集成测试/JUnit | ⭐⭐ |
| 08 | Maven Web应用开发 | WAR打包/Servlet/JSP/Tomcat部署 | ⭐ |
| 09 | Maven企业级Web应用 | 多模块架构/父POM/多环境Profile/CI-CD | ⭐⭐⭐ |
| 10 | Maven IDE集成 | IDEA/Eclipse Maven配置/Maven面板操作 | ⭐ |
| 11 | Maven Archetype扩展 | 项目模板/自定义Archetype/团队标准化 | ⭐⭐ |
| 12 | Maven自定义插件 | MOJO开发/生命周期绑定/参数配置 | ⭐⭐⭐⭐ |
| 13 | Maven项目站点 | Site生成/测试报告/覆盖率/JavaDoc | ⭐⭐ |
| 14 | Gradle核心概念与Kotlin DSL | 生命周期/DAG/Kotlin DSL/依赖配置/Wrapper | ⭐⭐⭐ |
| 15 | Gradle构建Task与依赖管理 | Task定义/增量构建/依赖解析/构建缓存 | ⭐⭐⭐ |
| 16 | Gradle多模块构建与插件开发 | 多项目/复合构建/自定义插件/性能优化 | ⭐⭐⭐⭐ |
| 17 | Maven与Gradle对比选型指南 | 场景对比/迁移指南/团队选型决策树 | ⭐⭐⭐ |

---

## 3. 精通级学习路线

### 🟢 L1：能用 Maven 管理项目（1天）

```
02-环境搭建 → 04-入门实战 → 03-核心概念 → 10-IDE集成
产出：能在 IDEA 中创建 Maven 项目、添加依赖、执行构建
```

### 🔵 L2：企业级 Maven 工程化（1天）

```
01-核心概念与依赖管理 → 05-灵活构建 → 06-版本管理 → 09-企业级Web应用
产出：能管理多模块项目、解决依赖冲突、配置多环境构建
```

### 🟣 L3：Maven 高级与自定义（半天）

```
07-测试 → 11-Archetype → 12-自定义插件 → 13-项目站点
产出：能开发 Maven 插件、自定义项目模板、生成项目站点
```

### 🟡 L4：Gradle 进阶（1天）

```
14-Gradle核心概念 → 15-Task与依赖管理 → 16-多模块与插件 → 17-对比选型
产出：能使用 Gradle Kotlin DSL 构建 Spring Boot 项目、编写自定义 Task
```

---

> 🎯 **构建工具是工程化的起点** — 依赖管理、多模块协作、CI/CD 流水线都以构建工具为核心。Maven 是 Java 后端的事实标准，Gradle 是未来趋势，两者都要掌握。
