# 01-Swagger是什么
> 定位：Swagger 是 API 文档工具家族——规范已改名 OpenAPI，工具仍是事实标准；2026 年 Java 生态的集成方案是 springdoc 3.1.0，且已长出 AI 时代的 MCP 集成。

## 📚 目录
1. [命名史：Swagger → OpenAPI](#1-命名史swagger--openapi)
2. [工具家族](#2-工具家族)
3. [2026 基线](#3-2026-基线)
4. [解决什么问题](#4-解决什么问题)
5. [与相邻体系的分工](#5-与相邻体系的分工)
6. [五条边界](#6-五条边界)
7. [练习](#7-练习)

## 1. 命名史：Swagger → OpenAPI

2015 年以前的「Swagger 规范」在 2015 年底捐赠给 Linux 基金会下的 OpenAPI Initiative，正式改名 **OpenAPI 规范（OAS）**——从此「Swagger」这个词从「规范名」退位为「工具家族名」。2026 年的正确用法：**「OpenAPI 规范」是契约格式（目前 3.1），「Swagger」是围绕它的工具（Swagger UI/Editor/Codegen）**——「Swagger 规范」是历史旧称。

版本脉络：Swagger 2.0（2014，仍是旧名时期）→ OpenAPI 3.0（2019，JSON Schema 对齐）→ **OpenAPI 3.1（2023-2024 普及，与 JSON Schema 2020-12 完全对齐）**——2026 年新项目一律 3.1。这条命名史解释了无数混乱：老教程说「Swagger 2.0 文档」、新工具说「OpenAPI 3.1」——**同一件事的两个时代名称**。

## 2. 工具家族

Swagger 家族三件套，各管一段：

**Swagger UI**——契约的可视化界面：浏览全部端点、展开参数与响应结构、在线调试（Try it out）、认证配置——**开发者与对接方看 API 的第一入口**。**Swagger Editor**——在线编辑 OpenAPI 契约的编辑器（左侧 YAML 右侧实时预览）——**契约优先工作流的编辑工具**（06 篇）。**Swagger Codegen / OpenAPI Generator**——从契约生成客户端 SDK 与服务端骨架（2026 年社区主推 OpenAPI Generator——Swagger Codegen 的继任者，支持 50+ 语言）。

家族之外还有平台生态：**springdoc-openapi**（Spring 集成——本体系主角）、**Knife4j**（国内增强 UI——07 篇）、**Stoplight/Redoc**（专业文档工具）、**Postman**（调试为主）。**「Swagger UI 只是冰山一角」**——完整链路是「契约（OpenAPI）→ 工具（UI 展示/Codegen 生成）→ 流程（契约测试）」。

**「工具家族」与「规范」的对应**：三件套对应契约生命周期的三个阶段——**写契约**（Swagger Editor：编辑与校验）、**看契约**（Swagger UI：渲染与调试）、**用契约**（Codegen/OpenAPI Generator：生成代码）——理解这个「写-看-用」的阶段划分，工具选型就清晰了：你的阶段在哪个环节，就选哪个工具；**「多阶段都要」就是完整工具链**（本体系各篇按阶段展开）。

## 3. 2026 基线

**springdoc-openapi 最新 3.1.0（2026-07-31）**——Java/Spring 生态的 OpenAPI 集成事实标准：**Spring Boot 4.1.0 兼容、swagger-ui 5.32.11、swagger-core 2.2.52、Spring AI 2.0.0**。核心能力：扫描 `@RestController` 自动生成 OpenAPI 契约，三端点开箱即用（`/v3/api-docs` JSON、`/v3/api-docs.yaml`、`/swagger-ui.html`）。

**3.x 的里程碑是 AI 集成**：**MCP（Model Context Protocol）支持**——`springdoc-openapi-starter-webmvc-mcp` 依赖 + `springdoc.ai.mcp.enabled=true` 后，**已文档化的 REST 端点自动变成 AI 工具**（Claude Desktop、Cursor 等 MCP 客户端可直接调用你的 API）——`@McpToolDescription` 注解做 AI 优化的描述、内置 MCP Dashboard 界面、guardrails 区分安全/变更操作（变更操作默认需人工确认）。**「文档是给 AI 看的使用说明」**——2026 年的 API 文档价值升级（09 篇详解）。

**版本警示**：**springfox 已死**（2020 年后停维护、不支持 Boot 3）——网上老教程的 springfox 配置全部过时，2026 一律 springdoc。

## 4. 解决什么问题

**「接口没人知道长什么样」**——后端改接口、前端对着猜——Swagger UI 让契约可视化，接口结构一目了然；**「文档滞后于代码」**——手写文档必过期——springdoc 从代码生成，文档与代码同源；**「接口不可调试」**——UI 内置 Try it out，开发期自测零成本；**「对接成本高」**——契约导出（JSON/YAML）给任何客户端工具，第三方接入不用问人；**「AI 无法用你的 API」**——2026 新问题——MCP 集成让文档化端点成为 agent 可发现、可调用的工具（09 篇）。

## 5. 与相邻体系的分工

**与 [[../../../02-后端核心技术%20微服务%20分布式%20云原生/09-Web开发全流程/RESTful%20API/00-RESTful%20API总览|RESTful API 体系]]**：那边 08 篇讲 OpenAPI 契约的工程决策（Spec-first、CI 工具链、agent 友好契约）；本体系讲 Swagger 工具链与 Spring 集成的具体使用——**契约决策看那边、工具落地看这边**。**与 [[../../../02-后端核心技术%20微服务%20分布式%20云原生/09-Web开发全流程/SpringBoot%20Web/00-SpringBootWeb总览|SpringBoot Web 体系]]**：springdoc 的底层是 Spring MVC 的扫描与反射——实现机制在那边；**与 [[../../../03-AI大模型应用开发/06-MCP协议与Agent%20Skill/MCP学习/MCP%20核心架构（CS%20客户端‑服务器）/00-MCP核心架构知识体系总览|MCP 体系]]**：springdoc 3.x 的 MCP 集成是「REST → AI 工具」的桥梁——MCP 协议本身在那边。

**「Swagger 工具」与「Spring 集成」的关系**：Swagger 工具家族（UI/Editor/Codegen）是语言无关的——任何语言的 OpenAPI 契约都能用；springdoc 是 Java 生态的「契约生成器」（从 Spring 代码生成契约）——**「Swagger 看契约、springdoc 造契约」**——一条典型的 Java 链路：Controller（代码）→ springdoc（生成契约）→ Swagger UI（展示契约）→ OpenAPI Generator（消费契约）——**springdoc 是链路里的「生成环节」**，Swagger 工具是「展示与消费环节」——理解这个分工，本体系 02-06 篇（生成与展示）与 07-09 篇（增强与生产）的篇章结构就顺了。

## 6. 五条边界

**不是契约设计工具**——Swagger UI 展示契约、Editor 编辑契约，契约「怎么设计」（版本化/幂等/错误格式）是 RESTful 体系的决策。

**不保证文档与代码同步**——注解生成的文档会漂移——契约测试是补位（06 篇）。

**不是安全工具**——Swagger 暴露信息，安全管控（认证/禁用）要自己做（08 篇）。

**不替代调试工具**——Try it out 是轻量自测；复杂场景（鉴权流、上传）用 Postman/curl。

**不覆盖所有语言**——springdoc 是 Java 生态；其他语言用各自的 OpenAPI 集成（FastAPI 自带、Go 用 swag 等）。

**生态全景与选择地图**：2026 年的 API 文档生态可以画成一张选择地图——**规范层**：OpenAPI 3.1（唯一事实标准——RAML/API Blueprint 已边缘化）；**Spring 集成层**：springdoc（Java 生态标准——springfox 已死）；**UI 层**：Swagger UI（默认）、Knife4j（国内增强，07 篇）、Redoc/Stoplight（专业渲染）；**契约工具层**：Swagger Editor（在线编辑）、Stoplight Studio（可视化设计）、OpenAPI Generator（客户端生成）；**平台层**：SwaggerHub（协作）、Postman（调试+导入）。**「选型的核心问题」**：团队在哪个生态（Java → springdoc）、要什么增强（Knife4j）、契约怎么用（生成客户端/契约测试）——**大部分团队的地图路径是「springdoc + Swagger UI + 契约 CI」**——本体系十篇就是这条主路径的完整展开。

**版本节奏与升级意识**：springdoc 的版本节奏与 Spring Boot 强绑定（3.1.0 对应 Boot 4.1.0）——**「升级 Boot 必查 springdoc 配套」是 Java 后端升级清单的固定项**；Swagger UI 是前端资产（5.x 系列持续迭代——UI 版本由 springdoc 传递依赖管理，一般不用手动管）；**「文档工具的升级」**——springdoc 升级后验证三件事：契约生成正常（3.1 输出）、UI 可用、注解兼容（新版本废弃注解）——**「工具链升级 = 文档回归测试」**（与 Java 基础体系的升级纪律同构）。

## 7. 练习

1. 讲清「Swagger → OpenAPI」的命名史与 2026 正确用法。
2. 工具家族三件套各管什么？
3. 2026 基线的三个数字（springdoc/UI/core）？
4. springdoc 3.x 的 MCP 集成解决什么问题？
5. 五条边界里哪条最容易被忽略？

> 🎯 **核心要点**：规范叫 OpenAPI（3.1）、工具叫 Swagger；springdoc 3.1.0 是 Java 生态标准（Boot 4.1/swagger-ui 5.32）；springfox 已死；3.x 的 MCP 集成让文档化 API 变成 AI 工具；契约决策看 RESTful 体系、工具落地看本体系。

---

**下一模块**：[02-快速开始](02-快速开始.md)｜**返回总览**：[00-Swagger总览](00-Swagger总览.md)
