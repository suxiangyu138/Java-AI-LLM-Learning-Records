# 02 - Spring 版本演进与兼容矩阵

> 🎯 版本对齐是 Spring 项目的第一基建 — Boot、Framework、Cloud、Java、Jakarta 五者必须匹配，错一个版本就是一片兼容性事故。本章是"版本字典"

---

## 目录

1. [版本演进时间线](#1-版本演进时间线)
2. [版本兼容矩阵](#2-版本兼容矩阵)
3. [支持周期与淘汰节点](#3-支持周期与淘汰节点)
4. [3.x → 4.0 迁移路线图](#4-3x--40-迁移路线图)
5. [常见版本事故与排查](#5-常见版本事故与排查)

---

## 1. 版本演进时间线

```text
Spring Framework 历史
 2.5 (2007) → 3.x (2009) → 4.x (2013) → 5.x (2017, 响应式) → 6.x (2022, Boot 3)
 → 7.0 (2025.11, Boot 4, Jakarta EE 11) → 7.1 (2026.11 预计)

Spring Boot 历史
 1.x (2014) → 2.x (2018) → 3.0 (2022.11, Framework 6 + Jakarta EE 9)
 → 3.5 (2024, 3.x 收官线) → 4.0 (2025.11, Framework 7) → 4.1 (2026.05)

Spring Cloud 节奏（按年命名）
 2023.x → 2024.x → 2025.x → 2026.x（每 Boot 大版本配套）

Spring AI 历史
 0.9 (预览) → 1.0 GA (2025 年中) → 1.1 (2026) → 2.0 (2026 里程碑, 适配 Boot 4)
```

| 世代 | Boot | Framework | 基线 | 关键特性 |
|------|------|-----------|------|----------|
| 经典 | 2.x | 5.x | Java 8+ / EE 8 | 传统 Servlet 时代主流 |
| 现代 | 3.x | 6.x | Java 17+ / Jakarta EE 9-10 | Boot 3 全面 jakarta |
| **当前** | **4.x** | **7.x** | Java 17+（推荐 21）/ Jakarta EE 11 | 模块化、虚拟线程、AOT |

---

## 2. 版本兼容矩阵

### 2.1 Boot 4.x ↔ Framework ↔ Java ↔ Cloud（2026）

| Spring Boot | Spring Framework | 最低 Java | 推荐 Java | Spring Cloud |
|:-----------:|:----------------:|:---------:|:---------:|:------------:|
| **4.0 / 4.1** | 7.0.x / 7.1 | 17 | **21** | 2026.x |
| 3.5.x | 6.2.x | 17 | 17/21 | 2024.0.x |
| 3.2-3.4 | 6.1.x | 17 | 17 | 2023-2024.x |
| 2.7（维护） | 5.3 | 8 | 8/11 | 2021.x |

### 2.2 Boot ↔ 各组件版本对应（4.x）

| 组件 | Boot 4 对应版本 | 注意 |
|------|-----------------|------|
| Jakarta EE | **11**（Servlet 6.1、JPA 3.2、Validation 3.1） | javax.* 已彻底移除 |
| Kotlin | 2.2+ | 新基线 |
| Jackson | **3.x**（包名 tools.jackson） | 2.x 提供兼容模块，7.2 移除 |
| Micrometer / OTel | 2.x + OpenTelemetry | 内置 starter |
| Spring Security | 7.x | OIDC/OAuth2 重构 |
| Spring AI | 1.1（稳定）/ 2.0（前沿） | 2.0 强制 Java 21+ |
| Tomcat | 11+（Servlet 6.1） | **Undertow 已移除**（不支持 Servlet 6.1） |

### 2.3 版本选择决策表

```text
新项目 2026 年起：
  ✅ 选 Boot 4.1 + Framework 7.1 + Java 21（LTS）+ Jakarta EE 11
  ✅ 微服务 → Spring Cloud 2026.x；AI 需求 → Spring AI 2.x（Boot 4 线）
  ⚠️ 若依赖生态未适配 Boot 4（老库仍用 javax.*）→ 暂用 Boot 3.5.x 过渡

存量项目：
  3.0-3.4 → 先升 3.5（修弃用 API）→ 再评估升 4.0
  2.x → 直接跨到 3.5/4.0 需重写 javax.* 部分，成本较高
```

---

## 3. 支持周期与淘汰节点

| 版本 | OSS 开源支持截止 | 说明 |
|------|------------------|------|
| Boot 2.7 | 已结束（2023） | 遗留系统，尽快迁移 |
| Boot 3.2-3.4 | 已结束 | 未升 3.5 的有 CVE 风险 |
| **Boot 3.5.x** | **2026.06（社区）/ 2026.11（整体）** | ⚠️ 现在的"升级压力线" |
| Boot 4.0/4.1 | 长期（商业支持 72 个月） | 当前主线 |
| Framework 7.0.x | 活跃 | 7.0.8（2026.06）修复 16 个高危 CVE |

> ⚠️ **生产铁律**：支持已结束的版本没有免费安全补丁 — 2026 年面试/述职的常见题："你们项目升级 Spring Boot 了吗？为什么？"

---

## 4. 3.x → 4.0 迁移路线图

```text
官方推荐七步：
① 升级到 3.5.x 最新补丁版（先清弃用 API）
② Java 升到 21（LTS）→ 顺手启用虚拟线程验证
③ 升级到 4.0（处理：javax→jakarta、配置属性改名）
④ 适配自动配置模块化（按需引入拆分后的 starter 模块）
⑤ 更新配置属性（新命名空间）
⑥ 可选：Feign → @HttpServiceClient；Resilience4j → 内置 @Retryable
⑦ 集成 OpenTelemetry（可观测性 2.0）→ 尝试 AOT 原生镜像
```

### 4.1 迁移踩坑清单

| 坑 | 现象 | 对策 |
|----|------|------|
| javax.* 找不到 | NoClassDefFoundError | 全局替换为 jakarta.*（IDE 自动迁移） |
| 配置属性改名 | 启动警告 unknown property | 用官方配置迁移工具对比 |
| 模块拆分影响 | 某些自动配置不生效 | 确认引入对应新模块依赖 |
| Undertow 用户 | 容器启动失败 | 换 Tomcat 11 / Jetty 12 |
| 三方库不兼容 | Bean 创建失败 | 查 Spring Boot 4 支持矩阵，升级依赖 |
| Jackson 2 混用 | 序列化异常 | 用兼容模块过渡 |

---

## 5. 常见版本事故与排查

| 事故 | 根因 | 排查命令/手段 |
|------|------|---------------|
| NoSuchMethodError | 依赖版本冲突（旧版被覆盖） | `mvn dependency:tree` 找冲突 |
| 启动报 Bean 重复 | 多版本 starter 混入 | dependency:tree 排查 |
| 循环依赖报错 | Boot 4 默认禁止循环依赖（Boot 3 仅警告→4 禁止？） | 重构设计 |
| jakarta/javax 混淆 | 依赖树中混有老库 | 全量搜 javax. 引用 |
| CVE 漏洞 | 版本过旧 | `mvn versions:display-dependency-updates` |

> ⚠️ **注意**：Boot 3.x 已对循环依赖默认禁用（启动直接失败）— 这是从 2.x 升级最常见的第一坑；Boot 4 延续并收紧。

---

> 🎯 **核心要点**：版本矩阵记住"一条主线 + 两个节点" — 主线：Boot 4.1 + Framework 7.1 + Java 21 + Cloud 2026.x；节点：Boot 3.5 支持 2026.06/11 截止、Framework 7.1 预计 2026.11 GA。任何新项目先对表，任何存量项目先看支持周期。

**下一模块**：[03-SpringBoot4与SpringFramework7新特性](03-SpringBoot4与SpringFramework7新特性.md) / **返回总览**：[00-Spring生态知识体系总览](00-Spring生态知识体系总览.md)
