# 01-SOLID 五大原则

> 定位：面向对象设计的五大原则（Robert Martin 提出），2026 年仍是评估模块性、可测试性、可维护性的标准透镜。每条给出定义、工程体现、反例、与设计模式的关系、面试话术。

## S：单一职责原则（SRP）

**定义**：一个类只做一件事，只有一个变更理由。工程体现：接口职责收敛、不写大杂烩类；避免一个方法既查库、又算业务、又发消息。2026 年的实践深化：SRP 本质是"人"的维度——**不同角色驱动的逻辑应放不同模块**（订单价格规则由业务方驱动、订单存储格式由 DBA 驱动，两类变更理由混在一个类里，任何一方改都要波及另一方）；内聚度可用 LCOM（方法缺乏内聚度量）量化，内聚差即 SRP 违规信号（NDepend 2026）。

**反例**：一个 OrderService 处理订单校验、支付调用、物流通知、优惠计算——四个变更理由共用一个类，任何业务变更都在 500 行里找。**面试话术**：SRP 的判据是"变更理由数"，不是"方法数"——一个类有 10 个方法但都服务于同一变更理由，仍是合格 SRP。

## O：开闭原则（OCP）

**定义**：对扩展开放、对修改关闭——新增能力尽量不改动原有稳定代码。工程体现：策略模式、接口抽象、配置驱动；新增业务走新增类，不反复改老 if-else。2026 年的可操作化框架是**变化点（Point of Variation）原则**：识别"预测会变的方向"，围绕它建立稳定接口；真正的挑战是时机——YAGNI 警告不要为从未发生的变化预建抽象，正确姿势是"需求出现的那一刻立刻重构引入抽象"（被坑一次可以，被坑两次就是失职）。

**反例**：新增支付方式（支付宝→微信→银联）时，每次都在支付处理器里加 else 分支；OCP 解法是 PayStrategy 接口 + 实现类注册。**红牌信号**（2026 静态分析清单）：向下转型（downcasting）、按类型分支（is/as 判断类型）——出现即 OCP 违规的征兆。**与模式的关系**：策略、模板方法、访问者（多变化方向时）都是 OCP 的落地。

## L：里氏替换原则（LSP）

**定义**：子类可以完全替换父类，不能破坏父类原有行为契约。工程体现：重写方法不能篡改逻辑语义；父类方法约定"入参可 null"，子类直接空指针就是破坏；父类抛 IllegalArgumentException，子类抛完全无关的异常也不行。**可替换性 = 契约兼容**：子类方法的前置条件只能更宽松（不能比父类要求更多）、后置条件只能更严格（不能比父类承诺更少）、抛出的异常只能是父类异常的子集或子类。

**反例**：正方形继承矩形——矩形的 setWidth(4) 不改变高度，正方形重写后同时改变宽高，使用矩形接口的客户端在"正方形"上行为异常（经典 LSP 案例）；父类支持 null 入参、子类空指针崩溃。**面试话术**：LSP 的判断标准是"客户端视角的行为契约"，不是继承关系本身——破坏 LSP 的继承应当改为组合或重新建模。

## I：接口隔离原则（ISP）

**定义**：不要大胖接口，客户端不应该依赖自己用不到的方法。工程体现：拆分粗粒度接口、按需提供小接口、避免实现类出现大量空实现（UnsupportedOperationException 是 ISP 违规的标志性代码）。**与 SRP 的关系**：SRP 管"类的职责"，ISP 管"接口的切面"——一个实现类可以有多个小接口（各自服务不同客户端），避免"一个接口让所有客户端都背上多余方法"。

**反例**：Animal 接口定义 eat/fly/swim/run，Fish 实现类里 fly() 抛 UnsupportedOperationException；正确切分为 Flyable/Swimmable/Runnable 小接口。**判断信号**：接口方法被"空实现/抛异常/返回 null"地应付，就是该拆接口的信号；2026 年的 ACM 案例研究把"上帝接口"（God interface）列为 SOLID 违规的典型模式之一。

## D：依赖倒置原则（DIP）

**定义**：高层模块不依赖低层模块，二者都依赖抽象；抽象不依赖细节，细节依赖抽象。工程体现：Spring 依赖注入；业务层依赖 Repository 接口，不直接依赖 JPA 实现类；配置与策略通过接口注入。

**三个易混概念的区分**（2026 NDepend 实证的高频面试题）：**DIP 是设计原则**（依赖抽象——设计决策）；**DI 是技术手段**（外部注入具体实例——实现技巧）；**IoC 是框架风格**（框架反向调用你的代码——控制反转）。DIP 改善可维护性与可测试性，DI 提供灵活性。**常见误用**：给每个类都套接口——只有一个实现且无第二实现前景的接口"只增加间接层不增加价值"（NDepend 2026）；判断标准是"是否存在真实的替换需求"（mock 测试、多实现、可插拔）。**Level 度量**：依赖链过长（Service → DAO → Connection → Driver）说明中间缺抽象层。

## 五原则的工程化落地工具

SOLID 从"评审原则"到"自动化门禁"的 2026 工具链：**架构约束测试**——ArchUnit（Java）把架构规则写成测试（"Controller 不得依赖 Repository""Service 接口必须实现于 service 包"），违规即 CI 失败——ArchUnit 是 DIP/SoC 的自动化执行者；**静态分析**——NDepend（.NET）/SonarQube 的依赖图、循环依赖检测、LCOM 内聚度量，把 SRP/OCP 违规变成可量化的指标；**依赖图可视化**——依赖扫描（dependency-cruiser 等）暴露"高耦合区域"（DIP 需要抽象的位置）。落地顺序建议：**先 ArchUnit 锁 DIP 与分层**（规则明确、代价低），再静态分析指标监控内聚与耦合趋势（指标基线 + 趋势告警），最后代码评审用 SOLID 清单做人工兜底（机器测不出的语义级违规——"这个类的变更理由有几个"只有人判断得了）。2026 年 Apache Fineract 的 FSIP-8 提案（给十年老代码库引入 SOLID 解耦模块）是这套工具链在存量项目上的完整案例：ArchUnit 边界 + 模块化重构 + "原则非盲从、用工程判断"的务实姿态。

## 面试高频问答

**Q：OCP 与 YAGNI 怎么平衡？** 变化点原则：只对"已预测且高概率"的变化建抽象（策略接口等）；不确定的变化保持具体实现 + 可重构性。被坑一次再抽象（fool me once），同一变化第二次出现立刻抽象——"按时抽象"而非"提前抽象"或"永后抽象"。

**Q：DIP 与 DI 的区别？** DIP 是设计原则（依赖抽象），DI 是技术手段（注入实现），IoC 是框架风格（控制反转）——三层关系：原则指导、手段实现、风格承载。常见误用：无替换需求的接口（单实现 + 无第二前景）只增加间接层。

**Q：LSP 与继承的关系？** LSP 要求"继承必须保契约"——子类放宽前置、收紧后置、异常收敛；破坏 LSP 的继承（正方形-矩形类）应改为组合或接口隔离。答法补一句：现代实践"组合优于继承"部分原因就是 LSP 约束难守。

**Q：SRP 的"一个理由"怎么界定？** 以"角色"界定——不同角色的变更需求（业务方改规则 vs 运维改格式）是不同理由；以"变更方向"辅助判断——同一方向的变化（促销规则的各种变体）可共处一个类。

## 五原则的组合使用

SOLID 不是五个孤立规则，而是递进关系：**SRP 决定类怎么切**（职责）、**OCP 决定扩展怎么留**（变化点）、**LSP 决定继承怎么用**（契约）、**ISP 决定接口怎么分**（切面）、**DIP 决定依赖怎么指**（抽象方向）。面试进阶答法：五原则共同服务于"降低变更爆炸半径"——SRP 减少变更面、OCP 隔离变更点、LSP 保护继承链、ISP 收敛客户端影响、DIP 切断跨层依赖。2026 年实证（奥地利学术期刊）确认 SOLID 是后端系统"架构韧性"的方法论基础，同时强调"不是每条类的检查清单，而是管理复杂度的工具箱"——过度套用本身违反 KISS。

> 🎯 **核心要点**：SOLID 的五句记忆——SRP"一个类一个变更理由"、OCP"扩展开放修改关闭"、LSP"子类契约可替换"、ISP"接口按需切小"、DIP"依赖抽象不依赖细节"。答题范式：定义 → 工程体现（Spring/策略）→ 反例 → 与模式关系，四步即可拿满分。2026 的修正：原则是工具箱非清单，DIP 不等于"每个类都套接口"。

---

**参考来源**：

- [SOLID Design: The Single Responsibility Principle | NDepend 2026](https://blog.ndepend.com/solid-design-the-single-responsibility-principle-srp/)
- [SOLID Design: The Open-Close Principle | NDepend 2026](https://blog.ndepend.com/solid-design-the-open-close-principle-ocp/)
- [SOLID Design: The Dependency Inversion Principle | NDepend 2026](https://blog.ndepend.com/solid-design-the-dependency-inversion-principle-dip/)
- [APPLICATION OF SOLID PRINCIPLES IN BACKEND DEVELOPMENT | 2026](https://openurl.ebsco.com/EPDB%3Agcd%3A2%3A39960322/detailv2)
- [FSIP-8: Introducing SOLID Principles To Decouple Modules | Apache Fineract 2026](https://cwiki.apache.org/confluence/spaces/FINERACT/pages/406622698/FSIP-8+Introducing+SOLID+Principles+To+Decouple+Modules)
- [Building Better Research Software: Technical Refactoring vs Software Quality | ACM 2026](https://dl.acm.org/doi/10.1145/3786172.3788364)

**下一模块**：[02-类与模块设计：迪米特、高内聚低耦合、正交](02-类与模块设计：迪米特、高内聚低耦合、正交.md) / **返回总览**：[00-设计思想总览](00-设计思想总览.md)
