# 04-OpenAPI文档生成
> 定位：springdoc 是「从代码生成契约」的引擎——理解扫描原理、掌握配置项、控制 3.1 输出；契约是文档体系的真相来源，UI 只是它的视图。

## 📚 目录
1. [生成原理](#1-生成原理)
2. [扫描控制](#2-扫描控制)
3. [配置项全解](#3-配置项全解)
4. [OpenAPI 3.1 输出](#4-openapi-31-输出)
5. [全局定义](#5-全局定义)
6. [契约质量检查](#6-契约质量检查)
7. [常见坑](#7-常见坑)
8. [练习](#8-练习)

## 1. 生成原理

springdoc 的生成链路：**应用启动时扫描**（`@RestController`/`@Controller` 注解的 Bean）→ **解析每个方法**（Spring MVC 注解 `@GetMapping` 等提取路径与方法、参数、返回类型）→ **合并 OpenAPI 注解**（@Operation/@Schema 等语义信息）→ **组装契约模型**（OpenAPI 对象）→ **序列化输出**（`/v3/api-docs` 端点）。**「契约是启动时生成、请求时输出」**——改代码要重启才更新契约（开发期热重载会重新生成）。

**生成的三个信息源**：**Spring MVC 注解**（路径/方法/参数/状态码——结构骨架）；**OpenAPI 注解**（03 篇——语义信息）；**类型反射与校验注解**（DTO 字段、@NotBlank 约束——schema 细节）。**理解信息源的意义**：排查「文档少了什么」时按三源排查——结构问题查 MVC 注解、语义问题查 OpenAPI 注解、模型问题查 DTO。

## 2. 扫描控制

默认扫描「应用包下所有 Controller」——多模块、特殊场景要显式控制：

```yaml
springdoc:
  packages-to-scan: com.example.api,com.example.admin   # 限定扫描包
  paths-to-match: /api/**,/admin/**                      # 限定路径
  show-actuator: false                                   # 隐藏 actuator 端点
```

**`paths-to-match` 是文档范围的阀门**——**「文档里出现什么接口」由它决定**：内部接口（`/internal/**`）不匹配 → 不出现在文档——比「注解隐藏」更系统；**actuator 端点默认隐藏**（健康检查等运维端点不该出现在 API 文档——`show-actuator` 显式开才显示）。

## 3. 配置项全解

生产常用的配置项族（02 篇基础 + 本篇进阶）：

```yaml
springdoc:
  api-docs:
    enabled: true                 # 契约端点开关
    path: /v3/api-docs
    version: openapi_3_1          # 3.1 输出
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    disable-swagger-default-url: true    # 关默认 petstore 示例
    try-it-out-enabled: true             # 调试开关
    persist-authorization: true          # 记住认证信息
  cache:
    disabled: true                # 开发期契约缓存关闭（改注解即时生效）
  model-and-view-allowed: false
  default-flat-param-object: false
```

**`disable-swagger-default-url` 是第一个该配的**（默认 UI 会加载 petstore 示例——内网环境联网加载失败/噪音）；**`cache.disabled` 开发期开**（热改注解即时看效果）；**try-it-out-enabled** 生产环境配合 08 篇（内网可留、公网必关）。

## 4. OpenAPI 3.1 输出

3.1 契约与 3.0 的差异（2026 新项目一律 3.1，配置见 02 篇）：

**JSON Schema 2020-12 对齐**——nullable 表达变化：3.0 的 `nullable: true` → 3.1 的 `type: [string, 'null']`；**$defs 替代 definitions**；**examples 数组化**。**对 Java 侧的影响**：DTO 的 `@Nullable`/`Optional` 字段在 3.1 输出为联合类型——**契约更精确**（「可能为 null」是类型的一部分）；**工具链兼容**——3.1 已被主流工具支持（Swagger UI 5.x、OpenAPI Generator 7.x）。

**验证输出**：`curl /v3/api-docs | jq '.openapi'` 输出 `3.1.0`；**契约即资产**——3.1 契约可直接导入 Stoplight/Postman/生成客户端（06 篇）。

## 5. 全局定义

@OpenAPIDefinition 是契约级配置（info、servers、securitySchemes）：

```java
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "订单服务 API",
        version = "2.1.0",
        description = "订单服务的完整接口文档",
        contact = @Contact(name = "订单团队", email = "order@example.com")
    ),
    servers = { @Server(url = "https://api.example.com/v2", description = "生产环境") },
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig { }
```

要点：**info 是文档的「封面」**（标题/版本/描述——UI 顶部展示，对接方第一眼）；**servers 声明环境**（生产/测试 URL——UI 的 Explore 与 Try it out 的目标地址）；**securitySchemes + SecurityRequirement 是认证声明**（bearer JWT——配合 03 篇 @SecurityRequirement 与 [[../../../02-后端核心技术%20微服务%20分布式%20云原生/10-前端知识拓展/Token/00-Token总览|Token 体系]]——「接口要什么凭证」在文档里自解释）。

## 6. 契约质量检查

「契约好不好」要可检查——三个自动化姿势：**结构检查**——脚本统计：接口总数、无 summary 的操作数、无错误响应的操作数（「文档完整性指标」）；**格式校验**——OpenAPI 契约过 Spectral 等 lint 工具（[[../../../02-后端核心技术%20微服务%20分布式%20云原生/09-Web开发全流程/RESTful%20API/00-RESTful%20API总览|RESTful 体系]] 08 篇的 CI 工具链）；**契约测试**——Dredd 等「契约声明 vs 实现响应」一致性验证（06 篇）——**「文档漂移」的根治是自动校验**：不是靠人记着「改接口要改文档」，而是 CI 里契约测试拦「文档与实现不一致」。

**契约的「真相地位」与使用纪律**：契约是文档体系的真相来源——**「UI 显示什么、客户端生成什么、AI 读什么」都来自契约**——围绕这个地位的纪律三条：**契约不进手改**（springdoc 生成的是产物——想改契约内容改注解/配置，别手改 JSON——手改会被下次生成覆盖且误导）；**契约可导出不可依赖运行时**（CI 里导出快照用于评审与测试——别让业务代码依赖 `/v3/api-docs` 运行时内容）；**契约变更走评审**（契约进仓库 + diff 评审——接口变更是评审对象，[[../../../02-后端核心技术%20微服务%20分布式%20云原生/09-Web开发全流程/RESTful%20API/00-RESTful%20API总览|RESTful 体系]] 的 oasdiff 检测破坏性变更的输入就是它）。

## 7. 常见坑

**坑一：改了注解 UI 不变**。契约缓存——开发期 `springdoc.cache.disabled=true`。

**坑二：多模块漏扫描**。子模块 Controller 没出现——`packages-to-scan` 覆盖。

**坑三：内部接口暴露在文档**。`paths-to-match` 没限定——内部路径匹配掉。

**坑四：UI 加载 petstore 示例**。默认 URL 没关——`disable-swagger-default-url: true`。

**坑五：3.0 与 3.1 混用**。老工具链读 3.1 契约报错——确认工具链版本（Swagger UI 5.x 起支持）。

**坑六：securitySchemes 没声明**。@SecurityRequirement 引用了不存在的方案——UI 认证不可用——全局声明配对。

**坑七：契约从不校验**。文档漂移无人知——结构检查 + lint + 契约测试进 CI。

**坑八：契约里出现内部信息**。服务器地址、内部字段名、错误详情混入契约——契约是半公开资产（08 篇）——生成后 grep 检查敏感词（内部域名、内部字段）。

**坑九：多实例部署契约不一致**。灰度/多副本下各实例生成契约可能有差异（动态信息）——契约以「构建产物」为准（CI 里导出快照）而非运行时各实例——「契约的一致性靠构建期快照」。

**坑十：@OpenAPIDefinition 与注解的覆盖关系**。全局定义（servers/security）与接口级注解并存——接口级覆盖全局（同 key 时）——「全局给默认、接口给特例」的理解到位，配置不会打架。

**坑十一：契约端点被安全框架拦在测试环境**。集成测试要消费 `/v3/api-docs` 但 Security 拦截——测试配置放行契约端点（与 08 篇生产管控的「放行侧」对应——测试环境允许、生产环境禁止）。

**坑十二：契约里的 servers 地址写死开发机**。`servers` 声明写死 `localhost:8080`——对接方/测试环境调错——servers 按环境配置（`@OpenAPIDefinition` 的 servers 用配置注入——不同环境不同值）——「servers 是环境声明，别写死」。

**坑十三：依赖顺序影响契约生成**。springdoc 的扫描依赖 Spring 容器初始化——启动失败时契约也失败——「契约生成异常先看应用能否正常启动」——契约问题排查的第一问是「应用本身起来了没」。

## 8. 练习

1. 生成的三个信息源与排查思路？
2. paths-to-match 的文档范围价值？
3. 3.1 与 3.0 的 nullable 差异？
4. @OpenAPIDefinition 的四个配置块？
5. 契约质量检查的三个自动化姿势？

> 🎯 **核心要点**：契约是启动时生成的资产（三信息源：MVC 注解/OpenAPI 注解/类型反射）；paths-to-match 管文档范围；3.1 输出显式配置；全局定义（info/servers/security）是文档封面；契约质量进 CI（结构检查 + lint + 契约测试）。

---

**下一模块**：[05-Swagger UI使用](05-Swagger%20UI使用.md)｜**返回总览**：[00-Swagger总览](00-Swagger总览.md)
