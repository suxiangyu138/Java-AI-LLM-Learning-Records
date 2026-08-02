# 00 - Spring 生态知识体系总览

> 🎯 Spring 不是"一个框架"，而是一个庞大家族 — Framework / Boot / Cloud / Data / Security / AI……本体系是**生态全景层**：家族版图、版本矩阵、2026 新特性（Boot 4）、Spring AI 集成、选型实战。与 06-Spring全家桶（原理纵深）互补

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [与现有 Spring 知识体系的分工](#3-与现有-spring-知识体系的分工)
4. [2026 生态大事记](#4-2026-生态大事记)
5. [核心概念速查](#5-核心概念速查)

---

## 1. 知识全景

```
Spring 生态体系（7个文件 — 地图层，俯瞰全家族）
│
├── 🗺️ 版图（01）
│   └── 01-Spring家族版图全览.md        # 17+ 家族成员：定位/核心模块/适用场景
│
├── 📊 版本（02）
│   └── 02-Spring版本演进与兼容矩阵.md   # Boot↔Framework↔Cloud↔Java 对照/支持周期/迁移
│
├── 🆕 新特性（03）
│   └── 03-SpringBoot4与SpringFramework7.md  # 2025.11 大版本革命：10 大新特性
│
├── 🤖 AI（04）
│   └── 04-SpringAI与AI应用生态.md      # ChatClient/RAG/Agent/MCP/双轨版本/对标LangChain4j
│
├── ⚖️ 选型（05）
│   └── 05-Spring技术选型实战指南.md     # WebMVC vs WebFlux/单体 vs 微服务/脚手架
│
├── 🎤 面试（06）
│   └── 06-Spring生态面试题集锦.md       # 版本/兼容/新特性/AI 集成高频题
│
└── 📌 00-Spring生态知识体系总览.md       # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 生态总览 | 全景导航 + 分工 + 大事记 + 速查 | — |
| 01 | 家族版图全览 | Framework/Boot/Cloud/Data/Security/AI 等全成员 | ⭐ |
| 02 | 版本演进与兼容矩阵 | 版本对照表/支持周期/3.x→4.0 迁移 | ⭐⭐ |
| 03 | Boot 4 与 Framework 7 | 2025.11 革命：模块化/虚拟线程/内置重试/HttpServiceClient | ⭐⭐⭐ |
| 04 | Spring AI 生态 | 1.0/1.1/2.0 双轨/RAG/Agent/MCP/模型矩阵 | ⭐⭐⭐ |
| 05 | 技术选型实战 | 技术栈决策/单体 vs 微服务/项目脚手架 | ⭐⭐ |
| 06 | 面试题集锦 | 生态层高频面试题（版本/选型/新特性/AI） | ⭐⭐ |

---

## 3. 与现有 Spring 知识体系的分工

| 现有系统 | 定位 | 本体系补充什么 |
|----------|------|----------------|
| `06-Spring全家桶`（28 文件） | **原理纵深**：IoC/AOP/事务/MVC/Boot 自动装配 + 微服务全链路 | 不重复原理，聚焦"生态地图 + 版本 + 新特性 + AI" |
| `SpringCloud微服务`（15 文件） | 微服务组件实战（Nacos/Feign/Sentinel/Seata） | 从生态视角看 Cloud 版本矩阵、Boot4 时代 Feign 的替代（HttpServiceClient） |
| `SpringCloud Alibaba` | Alibaba 组件深度 | 对齐 2026 年组件版本与选型 |

> 💡 **用法建议**：先看本体系 01-02 建立全景 → 需要原理时跳去 06-Spring全家桶 → 最新特性看 03 → AI 应用看 04。

---

## 4. 2026 生态大事记

| 时间 | 事件 | 影响 |
|------|------|------|
| 2025.11.20 | **Spring Boot 4.0 + Framework 7.0 GA** | 10 年最大升级：Jakarta EE 11、虚拟线程默认、AOT 一等公民 |
| 2026.05 | Spring Boot 4.1 发布 | 稳定演进 |
| 2026.06 | Boot 3.5.x 社区版支持结束 | **存量 3.x 项目的升级压力点** |
| 2026.06.09 | Framework 7.0.8（修复 16 个高危 CVE） | 生产环境务必跟进补丁 |
| 2026.05 | Spring AI 1.0.8 / 1.1.7 / 2.0.0-M7 | 双轨演进：1.x 稳定线 + 2.x 适配 Boot 4 前沿线 |
| 2026.11（预计） | Framework 7.1 GA | RestTemplate 标记 @Deprecated |

---

## 5. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| Spring 生态 | 以 Spring Framework 为核心的 17+ 框架家族 |
| Spring Boot | 快速开发脚手架：自动配置 + 起步依赖 + Actuator |
| Spring Cloud | 微服务全家桶：注册/配置/网关/熔断/链路 |
| Spring AI | AI 应用框架：ChatClient/RAG/Agent/MCP（Java 版官方 AI 方案） |
| Jakarta EE 11 | Boot 4 基线：javax.* 彻底退出，全部 jakarta.* |
| 模块化自动配置 | Boot 4 把 6.2MB 的 autoconfigure 拆成 47 个模块 |
| 内置弹性 | Framework 7 内置 @Retryable / @ConcurrencyLimit，替代第三方 |
| HttpServiceClient | @HttpExchange 声明式 HTTP 客户端，Feign 的官方替代 |
| 虚拟线程默认 | Boot 4 中 MVC/WebFlux/定时任务默认虚拟线程 |
| AOT/原生镜像 | 启动 <50ms，内存降 30-60%，反射自动收集 |

---

> 🎯 **核心要点**：2026 年的 Spring 生态正处于"十年一遇的换代窗口" — Boot 4 已 GA、3.x 支持到期、AI 双轨演进。本体系帮你建立**生态全局观**：知道家族有哪些成员、版本怎么对、新特性怎么用、AI 怎么接、面试怎么答。

**下一模块**：[01-Spring家族版图全览](01-Spring家族版图全览.md)
