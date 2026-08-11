# 02-三组件深度解析：Model、View、Controller

> 定位：MVC 的全部争议都来自三组件的职责边界没有画清楚。本文逐组件深潜——各自的职责、禁止触碰的领域、协作协议，以及"贫血模型"如何毁掉 MVC。

## Model：数据与规则，UI 绝缘

Model 是应用的数据核心与业务规则，理论上它不感知 View 与 Controller 的存在。合格的 Model 应该做到：**数据完整性自守**（订单 Model 知道"金额必须非负"、状态机合法转换，而不是依赖外部校验）；**可独立测试**（不依赖 UI 环境，纯单元测试即可验证）；**与持久化解耦**（Model 是内存对象，数据库是外部世界——2026 年 Java 后端实践即"领域模型与 JPA 实体分离"的讨论，见 DDD 体系 07 篇）。Model 最常见的退化是**贫血模型**：只剩 getter/setter 的数据容器，业务规则全部散落在 Controller/Service 里——此时 MVC 的"关注点分离"名存实亡，Model 退化为 DTO，整个架构实际上退化成了"Controller 大泥球 + 数据袋子"。判断 Model 健康度的一句话：**给 Model 加一个业务规则，是需要改 Model 本身还是改外部调用方？** 需要改外部，就是贫血。

## View：呈现与输入，不碰业务

View 的职责是"把 Model 的状态呈现给用户，并把用户操作转达给 Controller"。2026 年的 View 形态极其多样：服务端模板（Thymeleaf、JSP）、前端组件（React 函数组件、Vue SFC）、原生界面（SwiftUI、Jetpack Compose）——但职责纪律一致：**View 不包含业务逻辑**（"订单金额超 1000 打 95 折"属于 Model，不属于 View 的 if）；View 不直接修改 Model 状态（用户点"提交"，View 把事件交给 Controller/ViewModel，而不是自己改数据）；View 可以被替换（同一 Model 换皮肤、换平台）。View 的常见退化是"胖 View"：把数据格式化、权限判断、状态推导全写进模板/组件——2026 年前端把它叫做"组件里的面条逻辑"，与后端的"贫血模型"是同一病根的两种表现：职责放错了层。

## Controller：路由、绑定、编排——三条边界

Controller 是 MVC 的枢纽，也是职责最容易被侵蚀的角色。正确定位是**薄控制器**（Thin Controller）：**路由**（URL/事件 → 处理函数映射，Spring 中即 @GetMapping 等注解）；**绑定**（请求参数 → 方法参数/领域对象的转换与校验，Spring 的 @RequestParam/@ModelAttribute）；**编排**（调用服务/Model 完成业务、决定返回什么 View/响应）。三条不可逾越的边界：**不写业务规则**（价格计算、状态流转判断属于 Model/Service）；**不做数据访问**（查询逻辑在仓储/服务层，Controller 只拿结果）；**不写展示细节**（HTML 结构/JSON 字段组织属于 View/序列化层）。

Controller 膨胀的路径是渐进的：先放一个"顺手"的校验，再放一个"临时"的状态判断，半年后变成上帝控制器。防御手段：**代码行数门禁**（iOS 实践建议 Controller 超过 250-300 行触发重构，2026 社区沿用）；**业务规则进 Model 测试驱动**（先写"金额超限拒绝下单"的领域测试，Controller 就没有机会塞逻辑）；**代码审查清单**（"这个 if 该属于谁？"是审查提问）。

## 实体、值对象与 MVC 的 Model 再辨析

把 DDD 体系的战术概念引入 MVC 语境，Model 层会分裂出三个子概念，常被混用：**领域对象（Entity/VO）**——携带业务规则的内存对象，是真正的"Model"；**DTO**——跨层/跨进程传输的数据容器（Controller 出入参、API 契约）；**视图模型（ViewModel）**——为呈现定制组装的数据形态（下拉选项、格式化字段）。三者的纪律：Controller 的入参是 DTO（命令对象），出参是 DTO/视图模型；Service 层出入参可用领域对象（层内传递）；**领域对象不直接出网**（序列化面与领域形态耦合，DTO 隔离）。这条"Model 三形态"的区分在面试与代码评审中都是高频点："你的 Controller 返回的是实体还是 DTO？"——返回实体意味着 API 契约被领域结构绑架，是 MVC 项目最常见的架构债之一。与前端对照：前端的 Model 同样分裂为 Store 状态（领域）、组件 props（DTO）、派生状态（视图模型）——三形态的思维是跨端的。

## 协作协议：请求的生命周期

MVC 的一次完整协作（后端形态）：

1. 请求到达 → **路由**：DispatcherServlet 找到对应 Controller 方法（Spring 的 HandlerMapping）；
2. **绑定**：请求参数经转换器与校验器变为方法参数（@Valid 失败即 400，业务逻辑尚未启动）；
3. **编排**：Controller 调服务层/Model 完成业务（事务边界在此之上）；
4. **选 View/序列化**：返回 ModelAndView（模板渲染）或 @ResponseBody（JSON 序列化）；
5. 响应返回，请求结束。

前端形态的协作环：View 事件 → Controller 处理器 → Model 更新 → View 刷新（主动模型由数据绑定自动完成，被动模型手动 render）。两条线共享同一骨架：**输入经过控制层、规则归属数据层、呈现归属视图层**——理解这个骨架，Spring MVC、Vue、React 的具体实现都只是参数不同。

## 三组件在 Spring MVC 中的映射

Spring MVC 把三个抽象角色映射为具体机制，理解映射是读源码的地图：**Controller** = @Controller/@RestController 类与处理方法（HandlerMapping 负责路由、HandlerAdapter 负责参数绑定与调用）；**Model** = 服务层返回的领域对象（注意：Spring 的"Model"参数与 MVC 的 Model 概念不完全一致——Spring 的 Model 是视图数据容器，真正的业务 Model 在 Service 层之下）；**View** = ViewResolver 解析出的视图（Thymeleaf/JSP）或消息转换器序列化的 JSON（MappingJackson2JsonView 类）。DispatcherServlet 是"总控制器"（Front Controller 模式）：它统一接收请求、分发到具体控制器、解析视图——所以 Spring MVC 实际是"Front Controller + 每资源一个 Controller"的双层结构。理解双层结构的意义：全局横切（认证、日志、CORS）挂在 DispatcherServlet 的拦截器链上（Interceptor），业务路由挂在具体 Controller 上，两者互不干扰——这是"横切关注点与业务关注点分离"在 MVC 框架层的实现。

## 状态与行为的归属再辨析

"Model 管状态，Controller 管行为"是常见误读——状态与行为不可分离，分离即贫血。正确的划分是：**Model 管"领域状态 + 领域行为"**（订单的金额与"打折"规则同在）；**Controller 管"请求状态 + 请求行为"**（参数解析、路由选择）；**View 管"呈现状态 + 呈现行为"**（展开/收起、选中高亮）。三类"状态-行为对"各自内聚，互相通过接口协作。这个划分解释了三个经典问题的答案：为什么 Controller 里算金额是错的（金额是领域状态，打折是领域行为——都属 Model）；为什么 View 里做权限判断是错的（权限是请求级决策，属 Controller 层）；为什么"购物车高亮选中"不是业务状态（呈现状态，属 View/ViewModel）。**状态-行为配对内聚**是判断职责归属的通用标尺，比背职责清单更可靠——面试被问"某个逻辑该放哪层"时，用"它管理的状态属于谁"作答，比背清单更能体现理解。

MVC 的依赖规则：**View 依赖 Controller 与 Model 的接口**（View 只通过接口调用，不依赖具体类）；**Controller 依赖 Model 的接口**；**Model 不依赖任何组件**。依赖方向一旦反转（Model 里 import View，或 View 直接改 Model 内部状态），MVC 的隔离就崩了。在 Spring 中这个规则表现为：Controller 注入 Service 接口、Service 注入 Repository 接口、领域模型零框架依赖——2026 年"接口注入 + 依赖倒置"仍是后端 MVC 的黄金纪律。前端的对应物：组件通过 props/回调与父层通信（单向），状态提升到独立 Store——React 的"数据向下、事件向上"就是 MVC 依赖方向的组件化表达。

> 🎯 **核心要点**：三组件的健康度判断各有口诀——Model "加规则要改自己"（防贫血）、View "不碰业务不碰状态"（防胖视图）、Controller "路由绑定编排三件事，多一件就危险"（防上帝控制器）。MVC 的价值不在于三件套的写法，而在于依赖方向与职责归属这两条不可见的纪律。在 Spring 中理解"DispatcherServlet 双层结构"（总控制器 + 业务控制器）后，横切与业务的分工位置一目了然；再叠加"Model 三形态"（领域对象/DTO/视图模型）的区分——三组件的纪律从抽象概念变成可执行的工程地图。

---

**参考来源**：

- [Spring MVC Framework: A Practical, Modern Guide | TheLinuxCode](https://thelinuxcode.com/spring-mvc-framework-a-practical-modern-guide-to-building-maintainable-web-apps/)
- [MVC vs MVVM in iOS: Key Differences & When to Use Each](https://www.netguru.com/blog/mvc-vs-mvvm-on-ios-differences-with-examples)
- [从MVC，MVP到MVVM：前端/客户端架构演变](https://blog.csdn.net/huzhangka7378/article/details/161074448)
- [MVCPVM: A Unified Architectural Pattern | 2026](https://zenodo.org/records/19166089)

**下一模块**：[03-MVC 家族对比：MVP、MVVM、Flux](03-MVC家族对比：MVP、MVVM、Flux.md) / **返回总览**：[00-MVC架构模式总览](00-MVC架构模式总览.md)
