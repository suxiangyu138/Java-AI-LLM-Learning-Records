# Swagger 知识体系总览
> 一句话定位：API 文档的事实工具链——OpenAPI 规范是契约、Swagger UI 是可视化、springdoc 是 Spring 集成；2026 年最新 springdoc 3.1.0 已把 REST 端点变成 AI 工具的 MCP 集成。

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [常见误区](#5-常见误区)
6. [一周计划](#6-一周计划)
7. [自测题](#7-自测题)
8. [参考来源](#8-参考来源)

## 1. 知识体系导图

```text
Swagger（API 文档工具链）
├── 01 是什么（OpenAPI vs Swagger 命名史/工具链/2026基线）
├── 02 快速开始（springdoc 依赖/三端点/第一个文档）
├── 03 注解体系（@Operation/@Parameter/@ApiResponse/@Schema）
├── 04 OpenAPI 文档生成（自动生成原理/配置项/3.1 输出）
├── 05 Swagger UI 使用（界面/调试/定制/多环境）
├── 06 契约驱动实践（契约优先/导出/客户端生成）
├── 07 Knife4j 与增强（对比/选型/国内实践）
├── 08 安全与生产（生产禁用/认证/脱敏/审计）
├── 09 与 AI 生态集成（springdoc 3.x MCP/Spring AI/agent）
└── 10 生产实战与自测（文档体系实战/面试/20题）
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 01 | 是什么 | 命名史、工具链、2026 基线 | 所有人 |
| 02 | 快速开始 | springdoc、三端点、首个文档 | 新手 |
| 03 | 注解体系 | 注解全解、描述质量 | 必须 |
| 04 | 文档生成 | 原理、配置、3.1 输出 | 必须 |
| 05 | UI 使用 | 界面、调试、定制 | 必须 |
| 06 | 契约实践 | 契约优先、客户端生成 | 进阶 |
| 07 | Knife4j | 对比、选型、增强能力 | 进阶 |
| 08 | 安全生产 | 禁用、认证、脱敏 | 必须 |
| 09 | AI 集成 | MCP、Spring AI、agent | 进阶 |
| 10 | 实战与自测 | 文档体系、面试、20 题 | 毕业 |

## 3. 学习路线推荐

**路线一：快速上手（1-2 天）**——01 → 02 → 05 → 10。目标：Spring Boot 项目接入 springdoc，Swagger UI 跑通，接口文档可查可测。

**路线二：文档工程（3-5 天）**——路线一 + 03 → 04 → 06。目标：注解规范、契约导出、客户端生成，文档成为团队资产。

**路线三：生产与 AI（一周）**——路线二 + 07 → 08 → 09。目标：生产安全管控、Knife4j 选型、MCP 集成让 API 变成 AI 工具。

**先厘清定位**：本体系是「API 文档工具链」——Swagger 工具家族与 Spring 集成；契约设计的决策逻辑（版本化、幂等、错误格式）见 [[../../../02-后端核心技术%20微服务%20分布式%20云原生/09-Web开发全流程/RESTful%20API/00-RESTful%20API总览|RESTful API 体系]] 08 篇；Spring 实现机制见 [[../../../02-后端核心技术%20微服务%20分布式%20云原生/09-Web开发全流程/SpringBoot%20Web/00-SpringBootWeb总览|SpringBoot Web 体系]]。

## 4. 核心概念速查

| 概念 | 一句话 |
|------|-------|
| OpenAPI 规范 | API 契约标准（3.1 为最新），机器可读 |
| Swagger | 规范改名 OpenAPI 后的工具家族名（UI/Editor/Codegen） |
| Swagger UI | 契约的可视化界面：浏览 + 调试 + 认证 |
| springdoc-openapi | Spring Boot 的 OpenAPI 集成（3.1.0 = 2026-07-31） |
| /v3/api-docs | springdoc 生成的 OpenAPI JSON 端点 |
| /swagger-ui.html | Swagger UI 入口（默认重定向） |
| @Operation | 接口级描述注解（summary/description） |
| @Schema | 模型字段描述注解（示例/必填/格式） |
| 契约优先 | 先写 OpenAPI 再写实现的工作流 |
| 代码优先 | 从注解生成契约（springdoc 默认路径） |
| Knife4j | 国内增强版 UI（离线文档/鉴权/全局参数） |
| MCP 集成 | springdoc 3.x：REST 端点自动变成 AI 工具 |
| @McpToolDescription | AI 工具描述注解（3.1 新增） |
| Swagger Codegen | 从契约生成客户端/服务端代码 |

## 5. 常见误区

**误区一：Swagger 就是规范**。2015 年后规范改名 OpenAPI，Swagger 是工具家族名——「Swagger 规范」是旧称，文档用 OpenAPI 规范。

**误区二：文档是自动生成的不用写**。springdoc 生成的是「骨架」——summary/description/示例都要人写——**文档质量取决于注解质量**，不写描述 = 生成个目录。

**误区三：生产环境开着 Swagger**。Swagger UI 暴露全部接口与请求结构——生产环境默认禁用或加认证（08 篇）——「上线忘关 Swagger」是信息泄露事故。

**误区四：springfox 还在用**。springfox 停维护多年（不支持 Boot 3）——2026 一律 springdoc。

**误区五：注解越多越好**。注解是文档，契约是真相——复杂业务先在 OpenAPI 里定契约（契约优先），注解只是实现侧的表达。

**误区六：Swagger UI 就是全部**。Swagger 工具链（UI/Editor/Codegen）是一套——文档、调试、生成三件事，UI 只是可见的那部分。

**误区七：文档和代码总会同步**。没有契约测试的文档必然漂移——06 篇契约测试把关。

**误区八：文档只是给人看的**。2026 年的文档读者还包括 AI agent——描述质量决定「API 能否被 AI 正确调用」——09 篇 MCP 集成。

**误区九：Swagger 只能展示不能生成**。契约导出、客户端生成（OpenAPI Generator）、契约测试（Dredd）——工具链是「文档 → 资产」的完整链路——06 篇。

**误区十：UI 定制越花哨越好**。默认配置覆盖 90% 需求——为定制而定制是浪费——排序/过滤/示例这些「配置级定制」优先。

九个误区的共同根源只有一个：**把 Swagger 当成「看一眼接口的工具」**。它是「契约工程」——OpenAPI 规范是契约、springdoc 从代码生成契约、注解补契约质量、契约是资产（评审/版本化/生成客户端/喂 AI）、生产要管控暴露面。带着「看一眼」的心态，注解质量、契约漂移、生产暴露、AI 可用性这些环节必然漏；换成「契约工程」的心智——文档是资产不是装饰、契约是真相不是投影、AI 是读者不是噱头——整套体系才立得住。

**学习姿态的约定**：文档工程的每个环节都要「接入 + 验收」——依赖引入后三端点逐个 curl 验证；注解写完后导出契约 grep 检查；生产管控配置后发布检查脚本实测；MCP 集成后用真实客户端调一遍。本体系代码以 Java/Spring 为主（生态标准），契约设计逻辑对照 [[../../../02-后端核心技术%20微服务%20分布式%20云原生/09-Web开发全流程/RESTful%20API/00-RESTful%20API总览|RESTful 体系]]——「工具落地看这边、契约决策看那边」。

## 6. 一周计划

| 天 | 内容 | 产出 |
|:---:|------|------|
| Day1 | 01 + 02：定位与接入 | springdoc 跑通、UI 可见 |
| Day2 | 03：注解体系 | 一个 Controller 全注解 |
| Day3 | 04：文档生成 | 配置调优、3.1 输出 |
| Day4 | 05 + 06：UI 与契约 | 调试 + 契约导出 |
| Day5 | 07：Knife4j | 对比与选型 |
| Day6 | 08 + 09：安全与 AI | 生产管控 + MCP 集成 |
| Day7 | 10：综合实战 + 自测 | 文档体系 + 20 题 |

**本体系的定位补充**：Swagger 体系处于「01-底层根基-Java核心底座/09-计算机工具与常识补全」目录——定位是「Java 后端必备工具常识」——它与 RESTful API 体系（契约设计）、SpringBoot Web 体系（MVC 实现）、MCP 体系（AI 工具协议）三者衔接：**本体系讲「API 文档工具怎么用」**——工具的使用、注解的写法、契约的资产化、生产的安全管控、AI 的集成——「设计决策在那边、工具落地在这里」的分工贯穿全篇。

## 7. 自测题

1. OpenAPI 与 Swagger 的关系是什么？
2. springdoc 的三个默认端点是什么？
3. @Operation 与 @Schema 各管什么？
4. springdoc 生成文档的原理是什么？
5. 生产环境 Swagger 怎么管控？
6. Knife4j 相对原生 UI 增强什么？
7. springdoc 3.x 的 MCP 集成是什么？
8. 契约优先与代码优先的区别？
9. 文档漂移怎么防？
10. springfox 为什么不能用？

## 8. 参考来源

- [springdoc-openapi 官网（v3.1.0）](https://springdoc.org/)
- [springdoc-openapi Releases](https://github.com/springdoc/springdoc-openapi/releases.atom)
- [How To Generate an OpenAPI Document With Spring Boot](https://www.speakeasy.com/openapi/frameworks/springboot)
- [OPENAPI 3.1 WITH SPRING BOOT 4：契约优先](https://blog.vvauban.com/blog/openapi-3-1-with-spring-boot-4-document-your-api-contract-not-just-your)
- [OpenAPI Specification 3.1](https://spec.openapis.org/oas/v3.1.0)

---

**下一模块**：[01-Swagger是什么](01-Swagger是什么.md) → 从命名史与 2026 基线开始。
