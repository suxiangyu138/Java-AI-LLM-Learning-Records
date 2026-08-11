# 06-Spring Framework 7 MVC 新特性

> 定位：Spring Framework 7（Spring Boot 4 的底座）给 MVC 带来了五年最大的能力升级——API 版本化成为一等公民、错误契约标准化、路径匹配与序列化框架换代。本文逐项拆解，标注与 Spring 6 的迁移差异。

## API 版本化：从样板代码到一等公民

Spring 6 及以前，API 版本化靠团队自研（自定义拦截器、路径约定 v1/v2、网关转发规则），写法五花八门。Spring 7 把版本化做成框架能力：**@ApiVersion 注解 + 处理器方法级 version 属性 + 可配置的解析策略**。

```java
@RestController
@RequestMapping("/api/products")
@ApiVersion("v1")                                  // 控制器级基线版本
public class ProductController { ... }

@GetMapping(path = "/{id}", version = "2.0+")      // 基线版本：覆盖 2.0 及以上
public ProductDto getV2(@PathVariable Long id) { ... }

@GetMapping(path = "/{id}", version = "1.0")       // 固定版本：仅精确匹配 1.0
public ProductDto getV1(@PathVariable Long id) { ... }
```

**四种版本解析策略**（经 WebMvcConfigurer.configureApiVersioning 或 Boot 4 属性配置）：

| 策略 | 配置 | 适用场景 |
|---|---|---|
| 请求头 | useRequestHeader("X-API-Version") | 内部服务间 API（推荐默认） |
| 路径段 | usePathSegment(1) | 公开 API（URL 可书签） |
| 查询参数 | useQueryParam("version") | 调试方便（易漏进缓存键） |
| 媒体类型 | useMediaTypeParameter(APPLICATION_JSON, "version") | REST 原教旨（客户端最费劲） |

**版本语义**：SemanticApiVersionParser 按语义化解析（"1" = 1.0.0）；匹配规则是"取小于等于请求版本的最高声明版本"——因此**基线版本**（"1.2+"）意味着"1.2 及以上都归我，直到更高版本处理器接管"，契约不变时无需新增处理器。配套 API：addSupportedVersions（未声明版本返回干净 400）、setVersionRequired(true)（强制客户端带版本）、setDefaultVersion（兜底）、StandardApiVersionDeprecationHandler（注册弃用日期 + 迁移链接）。**一致性是铁律**：跨端点混用策略会让客户端无法预测，选一种全站统一。

## 错误契约：ProblemDetail 进入标准库

Spring 6 引入的 ProblemDetail（RFC 7807）在 Spring 7 全面标准化：ResponseEntityExceptionHandler 内置常见异常映射（400/404/405/415...），业务异常经 @ExceptionHandler 统一转换（见 05 篇）。Spring 7 的增强：ProblemDetail 支持类型化扩展（JSON 序列化时扩展字段与标准字段同构）、默认隐藏堆栈（生产环境 trace 字段为 null）、与校验错误聚合（MethodArgumentNotValidException 映射为字段错误列表）。**迁移注意**：Spring 6 项目若已自研错误结构（{code, message} 老格式），升级 Spring 7 时统一换 ProblemDetail——客户端契约变更要在发布计划中显式登记。

## 路径匹配：PathPatternParser 全面接管

Spring 7 移除 UrlPathHelper 的 setAlwaysUseFullPath（已弃用），路径匹配全面转向 **PathPatternParser**：更快的解析、更严格的语义（不再接受 servlet 路径的某些历史兼容行为）、尾斜杠默认不匹配（Spring 6 起已改，Boot 4 无内置 SPA fallback——详见仓库 SpringBoot Web 体系）。**迁移影响**：依赖 setAlwaysUseFullPath(true) 的旧项目需改为"路径前缀规划"；MockMvc 测试需显式 servletPath；自定义路径解析器的团队改用 PathPattern 的匹配语义重写。对绝大多数项目：只影响测试写法与个别通配符习惯（通配符匹配的语义差异变严格）。

## Jackson 3：序列化框架换代

Spring Boot 4 配套 **Jackson 3**（包名 tools.jackson，不再是 com.fasterxml.jackson）：JsonMapper 取代 ObjectMapper（`JsonMapper.builder().build()`）、模块系统统一（java.time 模块默认集成，无需手动注册）、更好的 record 支持（紧凑构造器、组件名绑定）。**迁移影响**：import 全量替换（com.fasterxml.jackson → tools.jackson）、ObjectMapper 注入类型改为 JsonMapper、第三方库若仍绑定 Jackson 2 需兼容层（Boot 4 提供 Jackson 2 兼容模块）。与 MVC 的直接关系：@JsonCreator/@JsonValue 绑定值对象、@ResponseBody 序列化 DTO 的行为不变，但配置 API 全面换代——**升级前先做序列化黄金样例测试**（枚举、日期、null 处理、循环引用四类）。

## 迁移决策：Spring 7 值不值得升

升级决策用三问评估：**收益**——版本化（@ApiVersion）、ProblemDetail 标准化、虚拟线程支持、record 一等公民，四者任一成为团队痛点即值得；**成本**——Jackson 3 换包（import 全量替换 + 第三方兼容层）、PathPattern 语义差异（通配符与尾斜杠行为）、测试写法调整（servletPath），规模与存量 API 数量成正比；**时机**——新项目直接上（无迁移成本，直接享受全部新特性）；存量项目在"API 重构窗口"（版本化改造同步进行）或"框架安全升级"（EOL 驱动）时升级，避免为升级而升级。风险缓释三件套：契约测试全量跑（破坏面定位）、黄金样例（序列化/错误格式/路径匹配各一组）、灰度升级（先非核心服务）。结论模板：**新项目无脑上，存量项目按痛点驱动**——Spring 7 的 MVC 升级收益真实但迁移成本也真实，用数据决策而非跟风。最后提醒一点：升级不等于改架构——Spring 7 的新特性是"框架能力"，薄控制器、DTO 契约、错误统一这些架构纪律不随版本改变，别把升级当成重构架构的借口。

## 其他值得注意的 Spring 7 MVC 变化

- **Record 与不可变对象的一等支持**：控制器方法参数直接绑定 record DTO（构造器绑定，无需 @Data 类，天然不可变——请求对象不该被中途修改），值对象转换器生态更顺；
- **空安全与弹性原生支持**：@NonNull/@Nullable 注解语义化，方法返回值可空性在 OpenAPI 文档中体现；
- **虚拟线程的成熟支持**：Boot 4 中 spring.threads.virtual.enabled=true 与 MVC 阻塞式控制器天然兼容（Servlet 6.1 异步支持），吞吐提升明显——Controller 保持"薄 + 阻塞式"写法即可吃到红利；注意不要与 WebFlux 混用（虚拟线程解决的是阻塞式 IO 的线程开销，与响应式是两条并行路线，选其一即可）；
- **OpenAPI 集成增强**：springdoc 生态对齐 Spring 7 的注解（@ApiVersion 自动进 OpenAPI 文档、ProblemDetail 的错误 schema 自动生成、版本与弃用信息进入文档标题），客户端 SDK 生成（OpenAPI Generator）直接消费版本化契约——文档、契约、实现三者同源，是版本化落地的最佳配套。

## 版本化的工程配套实践

@ApiVersion 只解决"版本解析与路由"，工程上还需要三件配套：**版本与弃用策略文档化**——每个版本的契约变更记录（新增/修改/删除端点）、弃用日期与迁移路径（StandardApiVersionDeprecationHandler 的 deprecation/sunset 字段直接进 OpenAPI 文档，客户端可见，弃用期结束前客户端有充足迁移窗口）；**多版本共存的生命周期**——固定版本（"1.0"）与基线版本（"1.2+"）的配合：契约不变时基线版本不新增处理器，契约变更时新增处理器并声明新基线——"只有契约真变才写新 handler"是版本化样板最小的关键；**测试矩阵**——MockMvc 测试覆盖"请求版本 → 命中处理器"的映射矩阵（每版本至少一正例一负例，未声明版本返回 400），契约测试（Pact）按版本分别锁定。配套实践的总体目标：**版本化不是防御机制，而是 API 生命周期的管理体系**——声明、文档、测试、弃用四件事齐备，版本化才真正降低维护成本。一个常见误区：版本化 ≠ 无限堆版本——每新增一个版本都要评估"能否通过兼容性策略避免新版本"（字段新增向后兼容即可复用旧版本），版本数量本身就是 API 稳定性的度量指标。

## 升级路线与风险清单

1. **依赖基线**：Spring 7.0.x + Boot 4.0.x + Jackson 3 + Jakarta EE 11 + Tomcat 11（与仓库 SpringBoot Web 体系版本矩阵一致）；
2. **先测后迁**：契约测试全量跑（Pact/MockMvc），错误格式（ProblemDetail）与序列化（Jackson 3）是两大破坏面；
3. **版本化先行**：升级 Spring 7 的恰当时机引入 @ApiVersion——一次迁移解决两个问题；并把契约测试全量（Pact + MockMvc）作为升级回归基线；
4. **不要动的**：控制器薄纪律、服务层划分、DTO 契约——Spring 7 改的是框架能力，不是你的架构本身；架构纪律的迁移与框架升级完全解耦，两者各自独立演进。

> 🎯 **核心要点**：Spring 7 的 MVC 升级是"框架能力补课"——版本化、错误契约、路径匹配、序列化四件事从团队自研走向平台内置，工程纪律从"手写样板"变成"配置项"。对开发者的真实影响是迁移成本（Jackson 3 换包、PathPattern 语义、ProblemDetail 格式），而非架构变化——MVC 的骨架二十多年未动，动的始终是周围的世界。版本化配套实践提醒：注解只是入口，声明-文档-测试-弃用的完整生命周期管理才是 API 版本化的真实价值。

---

**参考来源**：

- [How to Version REST APIs in Spring Framework 7 (Spring Boot 4)](https://dev.to/rabinarayanpatra/how-to-version-rest-apis-in-spring-framework-7-spring-boot-4-56jl)
- [API Versioning in Spring 7 | ITNEXT](https://itnext.io/api-versioning-in-spring-7-0b2c82519d2a)
- [What is the alternative to UrlPathHelper.setAlwaysUseFullPath after Spring 7?](https://stackoverflow.com/questions/79947950/what-is-the-alternative-to-urlpathhelper-setalwaysusefullpathtrue-after-spring/79947960)
- [Spring MVC 7.0.8 Professional Handbook](https://www.betterread.com.au/book/spring-mvc-708-professional-handbook-security-and-stability-improvements-in-spring-mvc-708-release.do)
- [Spring Annotations: The 2026 Essential Cheat Sheet](https://marmo.dev/spring-annotation-meaning)

**下一模块**：[07-现代前端替代：MVVM 与数据绑定](07-现代前端替代：MVVM与数据绑定.md) / **返回总览**：[00-MVC架构模式总览](00-MVC架构模式总览.md)
