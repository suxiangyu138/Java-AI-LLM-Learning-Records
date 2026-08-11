# 03-MVC 家族对比：MVP、MVVM、Flux

> 定位：MVC 之后出现的每个"新模式"都是对 Controller 膨胀的一次修正——MVP 让 Presenter 显式接管 View，MVVM 用数据绑定消灭手工刷新，Flux 用单向流让状态变更可追溯。2026 年的 MVCPVM 提案则揭示了它们其实是同一问题的三种切法。

## 等价性悖论：MVCPVM 的统一视角

汉阳大学 2026 年 3 月的 MVCPVM 论文给出了一个犀利的观察：**Model 与 View 在 MVC（1979）、MVP（1996）、MVVM（2005）中完全不变，变的只是"第三个角色"**——Controller、Presenter、ViewModel 轮流接管"输入处理 + 状态转换 + 视图驱动"的组合职责。因为每个"第三角色"都要同时承担这三个子职责，任何单一角色都必然在某一维度膨胀：Controller 在输入多时膨胀、Presenter 在视图交互复杂时膨胀、ViewModel 在状态转换频繁时膨胀。这就是**等价性悖论**——模式之争的本质不是"选哪个"，而是"第三个角色的三个子职责如何分配"。MVCPVM 的提议：让 Controller（输入路由）、Presenter（视图驱动）、ViewModel（状态转换）**五角色显式共存**，各管一段。工程启示：架构评审别问"这是 MVC 还是 MVVM"，问"输入、状态、视图驱动三段职责各自在哪"。

## 三家族核心对比

| 维度 | MVC | MVP | MVVM |
|---|---|---|---|
| 第三角色 | Controller | Presenter | ViewModel |
| View 主动程度 | 半主动（读 Model） | 完全被动（只暴露接口） | 被动 + 数据绑定 |
| View-Model 关系 | View 可能直读 Model | 完全隔离 | 通过绑定自动同步 |
| 同步机制 | 手动刷新（或观察者） | Presenter 显式更新 | 框架级绑定引擎 |
| 可测试性 | Controller 依赖 UI 环境，难测 | Presenter 纯对象，易测 | ViewModel 纯对象，最易测 |
| 典型代表 | Spring MVC、Rails | Android MVP、WinForms | Vue、Angular、WPF |

**MVP** 的贡献是"View 彻底被动"：View 只暴露显示接口，Presenter 持有 View 接口引用并显式驱动——View 与 Model 完全隔离，Presenter 变成纯逻辑对象，单元测试不再需要 UI 环境。代价是接口爆炸（每个 View 配一套接口）与 Presenter 自身的膨胀转移。**MVVM** 的贡献是"消灭同步样板"：ViewModel 暴露状态与命令，框架级绑定引擎自动完成 View ↔ ViewModel 同步——View 变成纯声明式描述，开发者的精力从"刷新"解放到"状态"。代价是绑定调试的黑盒与过度依赖框架。可测试性排序（2026 共识）：MVVM ≥ MVP > MVC——原因是 ViewModel/Presenter 是零 UI 依赖的纯对象，两行代码即可实例化测试；MVC 的 Controller 绑死 servlet/UI 环境。

## Flux：跳出"第三角色"框架的第四范式

Flux（2014，Facebook）不再争论第三角色叫什么，而是**重新定义数据流方向**：单向数据流 Action → Dispatcher → Store → View，View 只能通过 Action 触发变更，Store 是唯一状态持有者。它解决 MVC 双向绑定的核心痛点：**状态变更路径不可追溯**（任何组件都能改状态，出 bug 不知道谁改的）。Flux 的纪律：状态集中（Store）、变更只能经 Action（显式意图）、视图只读状态（纯展示）。代价是样板代码（Action/Reducer/Store 三件套）——这直接催生了 Redux 的"三大原则"（单一数据源、状态只读、纯函数变更）与 2026 年的原子化状态修正（见 08 篇）。从等价性视角看，Flux 的 Store 扮演了"无歧义的 Model"，Action 扮演了"被规范化的 Controller 输入"——它把 MVC 最模糊的两个接口（输入怎么进、状态怎么变）显式契约化了。

## 选择框架（2026 共识）

- **后端 API / SSR** → MVC（Spring MVC 7 仍是标准，职责纪律见 05/06 篇）。
- **简单前端页面**（<250 行视图逻辑）→ MVC 或纯组件都够，别上框架重器。
- **交互复杂的前端**（>300 行视图逻辑 / >20 屏）→ MVVM（Vue 3 最贴 MVVM 思想；React 走"类 MVVM"的单向流 + hooks）。
- **状态跨组件共享且变更频繁**（电商购物车、实时协作）→ Flux/Redux 或原子化 Store。
- **需要深度单元测试的 UI 逻辑** → MVVM（ViewModel 零 UI 依赖可测）是首要理由——2026 的迁移研究结论：可测试性是团队从 MVC 迁走的第一驱动力。

## 第三角色职责分配的检查清单

把"职责归属三问"落成代码评审可用的清单：**输入路由**——检查"谁在解析输入并映射到处理逻辑"（请求参数/事件/手势），答案应唯一且集中（Controller/事件处理器），分散即违规；**状态转换**——检查"状态变化的规则在哪"（业务分支/校验/计算），应归属领域对象/Store（业务规则）与 ViewModel（视图状态），出现在视图模板或控制器 if 中即违规；**视图驱动**——检查"状态如何反映到界面"（渲染/刷新/绑定），应归属绑定引擎或显式渲染函数，手写 DOM 拼接散落各处即违规。每条检查对应一个反模式：输入分散 → "隐形 Controller"；规则上浮 → "贫血模型"；手写刷新 → "同步地狱"。清单的用法：每周代码评审过一遍三问，每次架构变更前过一遍三问——比模式标签检查更早发现腐烂。2026 年 MVCPVM 论文的意义正在于此：它给出的不是"第五个模式"，而是"五角色显式分配"的检查框架。

## 对比表的使用注意

上面的对比表是简化模型，使用时注意两条：**表里是"典型实现"，不是"模式边界"**——Vue 3 有双向绑定也有单向流（Pinia 的 action 语义），React 有单向流也允许手写双向（受控组件），模式在实现中是连续光谱而非离散类别；**可测试性差异的前提是职责守纪**——MVVM 可测试是因为 ViewModel 零 UI 依赖，若 ViewModel 里 import 了 DOM 或引用了组件实例，可测试性立刻崩回 MVC 水平——模式给的只是潜力，纪律兑现潜力。面试中被追问"具体项目里怎么选"时，用这两条注意事项收尾，比继续背模式定义更能体现理解深度。

## 从"选模式"到"角色分配"的实践转变

等价性悖论对工程实践的真正影响是**评审问题的变化**：不再问"这个页面是 MVC 还是 MVVM"（模式贴标签没有决策价值），改问三个具体问题——**输入路由归谁**（事件/请求如何映射到处理逻辑——Controller 的职责）；**状态转换归谁**（数据如何变化、变化规则在哪——ViewModel/Model 的职责）；**视图驱动归谁**（状态变化如何反映到界面——绑定引擎/Presenter 的职责）。三问都能明确回答的代码库，无论贴什么模式标签都是健康的；三问含糊的代码库，贴什么标签都会腐烂。这个转变也解释了为什么 2026 年架构评审不再开"模式一致性"会议，而是做"职责归属"检查——模式是手段，职责分配是目的。落地形式：代码评审模板中加入"这个 if 属于哪段职责"的提问；组件/控制器拆分决策用"职责数量"而非"行数"作主要依据（行数是辅助信号）。

## React Native / SwiftUI 的 2026 验证

两个新兴平台的实践验证了家族模式的映射规律：**React Native 中 Controller 没有位置**——hooks"吃掉"了它（useState 管状态、useEffect 管副作用，组件化后 600 行的 Controller 被拆散）；MVP 映射为"presenter hooks + 被动视图"；MVVM 映射出人意料地好——自定义 hooks 拥有状态、暴露命令，就是 ViewModel；Zustand/Jotai 的 store 暴露状态与命令且不关心谁在监听，也是 ViewModel——React 的渲染循环替代了 WPF 的双向绑定（单向替代双向）。**SwiftUI** 中 @ObservableObject + @StateObject 让 MVVM 天然契合，而 MVC 没有一等公民形态——把逻辑写进 View body 只是"以值类型外衣重新造上帝控制器"。结论：**MVVM 的"状态与视图解耦"思想在 2026 年的新平台全面获胜，但实现机制从双向绑定演化为单向渲染循环**。

> 🎯 **核心要点**：家族对比的最终结论是"模式不是选择，是分配"——Model 与 View 恒定，真正要设计的是"输入路由（Controller）、视图驱动（Presenter）、状态转换（ViewModel）"三段职责的归属。2026 年实践答案：后端 MVC 管请求路由，前端 MVVM 管状态同步，复杂状态用 Flux 单向流兜底，三者共存而非互斥。评审时把"贴模式标签"换成"问职责归属三问"（输入路由/状态转换/视图驱动各归谁），再对照三反模式（隐形 Controller/贫血模型/同步地狱）检查——这是等价性悖论带来的最实用转变。

---

**参考来源**：

- [MVCPVM: A Unified Architectural Pattern | Hanyang University 2026](https://zenodo.org/records/19166089)
- [MVC vs MVVM in iOS: Key Differences & When to Use Each](https://www.netguru.com/blog/mvc-vs-mvvm-on-ios-differences-with-examples)
- [MVC, MVP, MVVM in React Native: what survives the trip](https://dev.to/aoligama/mvc-mvp-mvvm-in-react-native-what-survives-the-trip-4cg4)
- [Flux架构与MVC架构深度对比 | 百度开发者](https://developer.baidu.com/article/detail.html?id=7239016)
- [从MVC，MVP到MVVM：前端/客户端架构演变](https://blog.csdn.net/huzhangka7378/article/details/161074448)

**下一模块**：[04-前端 MVC 的困境与演进](04-前端MVC的困境与演进.md) / **返回总览**：[00-MVC架构模式总览](00-MVC架构模式总览.md)
