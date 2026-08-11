# 07-Java 与 Spring 生态实现

> 定位：战术模型落到 Java/Spring 的具体形态——包结构、聚合实现、JPA 映射、事务边界。2026 年的工具化趋势（Round-trip 工程）正在把"聚合必须如此写"变成可自动校验的约束。

## 包结构：按限界上下文与聚合组织

DDD 的包结构纪律：**顶层按上下文分**，上下文内按"应用层-领域层-基础设施层"分层（同 05 篇的职责归属）。领域层内部按聚合组织，聚合内文件聚在一起：

```text
order-context/
├── application/        # 应用服务、DTO、命令处理
│   └── OrderApplicationService.java
├── domain/             # 领域层：聚合 + 领域服务 + 事件
│   ├── model/          # 实体、值对象、聚合根
│   │   ├── Order.java          # 聚合根
│   │   ├── OrderItem.java      # 聚合内实体
│   │   ├── OrderId.java        # 值对象（ID）
│   │   └── Money.java          # 值对象
│   ├── service/        # 领域服务
│   │   └── OrderPlacementService.java
│   └── event/          # 领域事件定义
│       └── OrderPaidEvent.java
├── infrastructure/     # 仓储实现、事件发布、外部客户端
│   └── JpaOrderRepository.java
└── interfaces/         # 控制器、API 适配（或并入 application）
    └── OrderController.java
```

三条硬纪律：**领域层不依赖 Spring/持久化注解的污染**（除极少数如 @Entity 的映射注解，理想情况领域层纯 POJO）；**依赖方向单向**（interfaces/application/infrastructure → domain，domain 不反向依赖）；**跨上下文禁止 import 对方 domain 类**（只能通过 API/事件契约，见 02/06 篇）。

## 聚合的 Java 实现形态

```java
public class Order {                        // 聚合根
    private OrderId id;                     // 值对象做 ID
    private CustomerId customerId;          // 通过 ID 引用其他聚合
    private OrderStatus status;
    private List<OrderItem> items;          // 聚合内实体
    private Money total;                    // 值对象封装金额规则

    private Order() {}                      // JPA 用，禁止外部直接 new

    public static Order place(OrderId id, CustomerId customerId, List<OrderItem> items) {
        Order order = new Order();
        order.id = id; order.customerId = customerId; order.items = List.copyOf(items);
        order.total = items.stream().map(OrderItem::subtotal)
                           .reduce(Money.ZERO, Money::add);
        order.status = OrderStatus.PLACED;
        return order;                       // 工厂形态：静态工厂方法
    }

    public void cancel(String reason) {     // 行为封装：规则在实体内部
        if (status == OrderStatus.SHIPPED) throw new IllegalStateException("已发货不可取消");
        this.status = OrderStatus.CANCELLED;
        registerEvent(new OrderCancelledEvent(id, reason));  // 聚合内收集事件
    }
}
```

实现要点：聚合根私有构造（外部只能经工厂方法进入合法状态）；setter 不暴露（状态只经业务方法变更）；**不变量在变更方法内校验**（cancel 前的状态检查）；领域事件先收集在聚合内（事件列表字段），事务提交时统一发布——这是 Outbox 与事务一致性的模型侧基础。JPA 映射层面：聚合根 @Entity + @Table，聚合内对象 @OneToMany（级联持久化），值对象 @Embeddable 或 @EmbeddedId（Money 等内嵌），聚合间只存 ID 字段（customer_id 列而非 @ManyToOne 对象关联——后者会把聚合边界重新粘起来，是 JPA 上 DDD 最常见的破坏行为）。

## 事务边界：应用服务是唯一事务锚点

事务的边界纪律：**事务开在应用服务，不在领域层**——应用服务方法上 @Transactional，内部调领域服务与仓储；聚合方法内不感知事务（领域层与事务解耦，可单测）。跨聚合的强一致：事务开在应用服务，跨多个仓储保存——但这是"同一进程内"的强一致；跨服务/跨上下文的强一致是陷阱（见 06 篇，用事件 + 最终一致）。**读模型分离**：聚合加载走仓储（事务内），复杂列表/报表查询走只读通道（@Transactional(readOnly=true) 或独立投影表），避免查询把聚合加载搞得臃肿——这是 CQRS 的轻量姿态（查询不经过聚合是纪律：读模型可以按查询反范式建索引/投影表，写模型保持聚合纯度）。

## 仓储与事务的完整示例

把 04/05 篇的模型落到 Spring 的完整形态（订单下单的应用服务 + 仓储 + 事务）：

```java
@Transactional
public class OrderApplicationService {           // 应用服务 = 事务锚点
    private final OrderRepository orders;
    private final OrderPlacementService placement; // 领域服务：跨聚合规则

    public OrderId place(PlaceOrderCommand cmd) {   // 命令对象承载入参
        OrderId id = OrderId.generate();            // 代理键值对象
        Order order = placement.place(id, cmd);     // 领域服务执行业务规则
        orders.save(order);                         // 仓储只按聚合根操作
        return id;
    }
}

public interface OrderRepository {                 // 语义契约：领域语言
    Optional<Order> findByOrderId(OrderId id);
    void save(Order order);
}

@Repository
class JpaOrderRepository implements OrderRepository {
    private final JpaOrderDao dao;                 // JPA 细节藏在基础设施层
    @Override public Optional<Order> findByOrderId(OrderId id) {
        return dao.findById(id.getValue());
    }
}
```

```java
@Entity
@Table(name = "orders")
class OrderJpaEntity {                              // 持久化模型与领域模型分离（可选）
    @Id private String id;
    @Column(name = "customer_id") private String customerId;  // ID 引用，非 @ManyToOne
    @Enumerated(EnumType.STRING) private OrderStatus status;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderItemJpaEntity> items;         // 聚合内级联，跨聚合不导航
}
// 注意：id 用 String 列承载 OrderId 值对象，防止 JPA 直接暴露领域对象的内部形态
```

两个实现选择的说明：**JPA 实体与领域模型合一或分离**——简单场景合一（@Entity 直接标在领域类上，接受持久化注解轻污染），复杂场景分离（JPA 实体类独立映射，领域模型保持纯净）；合一省代码但增加耦合，分离保纯度但多一层转换——2026 年社区倾向合一起步，出现映射痛苦再分离。**命令对象（Command）**——应用服务入参用不可变命令对象而非散参，便于校验、幂等键携带与测试构造。

## 测试策略：按层级对应

DDD 分层的测试金字塔与通用测试有细微差别：**领域层**——纯单元测试，不启动 Spring：值对象构造合法性、实体状态机（取消前置条件）、聚合不变量（总额计算）全部可在毫秒级验证；这是 DDD 的最大红利——业务规则可以在无数据库、无容器环境下完整测试，测试即文档（测试名用领域语言："下单时商品已下架则抛异常"）。**应用层**——集成测试，真实事务 + 内存数据库（H2 兼容模式），验证事务边界、幂等、事件发布（提交后才发、回滚不发）。**接口层**——契约测试（Pact 类）：对外 API 的请求/响应 schema 锁定，防"悄悄改契约破坏下游"；跨服务契约测试是微服务独立部署的前提（见 06 篇质量门禁）。**测试替身纪律**——仓储接口用内存实现（领域测试不碰 JPA），事件总线用捕获型替身（断言事件内容而非总线行为）。测试金字塔的比例建议：领域单元测试 60%、应用集成 30%、接口契约 10%——DDD 团队测试量最大且最有价值的部分恰恰是传统项目最缺的领域层。

## Spring 生态的落地清单

- **Spring Data JPA**：仓储接口继承 JpaRepository 即可获得默认实现，但接口签名用领域语言（`Optional<Order> findByOrderId(OrderId id)`），把 JPA 的实现细节留在 infrastructure；
- **事件发布**：Spring 的 @TransactionalEventListener 在事务提交后发布事件（相位 AFTER_COMMIT），配合 Outbox 表 + relay 才是生产级（见 10 篇）；
- **校验**：值对象构造时自校验（jarkarta validation 在 application 层做入参校验，领域不变量在模型内做业务校验，两层不重叠）；
- **模块边界强制**：Spring Modulith（2025-2026 逐渐普及）在单体内部强制模块边界——模块接口可见性、跨模块依赖检测、模块内测试切片，是"模块化单体 + DDD"的组合拳；
- **测试**：领域层纯单元测试（内存仓储）、应用层集成测试（真实事务）、接口层契约测试（Pact），三层测试金字塔对应三层职责（比例详见下文测试策略节）。

## 2026 工具化趋势：约束驱动的战术建模

KASTEL 的 Round-trip Engineering 研究（arXiv:2603.26987）正在把上述"实现纪律"变成工具：以聚合/实体/值对象为一等原语的元模型 + 实时约束验证引擎（建模时拦截"跨聚合实体引用""值对象可变性违规""聚合边界泄漏"），并通过增量双向同步保持模型与代码一致。它的目标人群是"没有持续专家审查的小团队"——意味着 DDD 实现纪律正在从"专家手把手评审"走向"工具自动门禁"。工程含义：团队应提前把自己的 DDD 实现规范（如上文清单）脚本化成检查规则（静态扫描 + 测试断言），不等工具成熟，先把能自动化的纪律自动化。

> 🎯 **核心要点**：Java/Spring 落地的核心矛盾是"框架的反向牵引"——JPA 的 @ManyToOne 与全局 setter 会悄悄把聚合边界重新粘回数据模型，Spring 的便利会稀释领域纪律。落地纪律三句话：领域层纯 POJO + 工厂方法进合法状态、事务锚点在应用服务、JPA 只做持久化映射不做对象导航。测试金字塔的倒挂是常见信号——领域单元测试占比低、全是 Controller 测试的团队，模型大概率已经贫血。2026 年这些纪律正在被约束验证工具接管（Round-trip 工程），提前把纪律脚本化成 CI 门禁是团队的务实选择。

---

**参考来源**：

- [Round-trip Engineering for Tactical DDD | arXiv 2603.26987](https://ar5iv.labs.arxiv.org/html/2603.26987)
- [使用战术 DDD 设计微服务 | Azure Architecture Center](https://learn.microsoft.com/zh-cn/azure/architecture/microservices/model/tactical-domain-driven-design)
- [DDD 认知升级：从单服务战术落地，到分布式中台战略全景](https://blog.csdn.net/qq_24597659/article/details/159020037)
- [领域驱动设计DDD建模实战指南 | OSCHINA](https://my.oschina.net/emacs_7986100/blog/19262491)

**下一模块**：[08-实战案例：电商订单端到端](08-实战案例：电商订单端到端.md) / **返回总览**：[00-DDD领域驱动设计总览](00-DDD领域驱动设计总览.md)
