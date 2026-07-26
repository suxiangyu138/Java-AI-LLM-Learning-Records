# DDD领域驱动设计入门

> 领域驱动设计（Domain-Driven Design）的核心思想是让代码结构与业务概念对齐，解决传统MVC在复杂业务下出现的"胖Service"问题。

## 目录

1. [DDD解决什么问题](#1-ddd解决什么问题)
2. [核心概念](#2-核心概念)
3. [分层架构](#3-分层架构)
4. [代码目录结构](#4-代码目录结构)
5. [什么时候用DDD](#5-什么时候用ddd)
6. [实战建议](#6-实战建议)

---

## 1. DDD解决什么问题

传统MVC三层（Controller-Service-DAO）在复杂业务下会出现——**"胖Service"**：几千行的Service类，业务逻辑缠绕在一起，改一处影响一片，代码的可维护性急剧下降。

DDD的核心思想：**让代码结构与业务概念对齐。**

业务专家说"订单包含订单项、有状态流转"，代码里也应该有对应的`Order`聚合、`OrderItem`实体、`OrderStatus`值对象。业务语言就是代码语言，代码即设计。

---

## 2. 核心概念

### 2.1 实体（Entity）

**有唯一标识，会变化的对象。** 标识不变，属性可变。

```java
public class User {
    private Long id;           // 唯一标识
    private String name;       // 可变属性
    private String email;      // 可变属性

    // 通过id判断相等性，而非所有属性
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

### 2.2 值对象（Value Object）

**没有唯一标识，通过属性值来定义相等性。不可变。**

```java
// 使用record天然满足值对象特征：不可变、基于属性判等
public record Address(String province, String city, String street) { }

public record Money(BigDecimal amount, String currency) { }

// 两个Address如果属性值全部相等，则为同一个对象
Address addr1 = new Address("北京", "海淀", "中关村大街");
Address addr2 = new Address("北京", "海淀", "中关村大街");
System.out.println(addr1.equals(addr2)); // true
```

> 值对象在DDD中非常重要。将无唯一标识的概念封装为值对象，避免散落在实体中成为"字符串地狱"。

### 2.3 聚合（Aggregate）与聚合根（Aggregate Root）

**一组相关对象的集合，有一个"根实体"作为外部访问的唯一入口。** 外部只能通过聚合根操作聚合内部的对象，保证数据一致性和不变量。

```java
// Order 是聚合根，OrderItem 是聚合内部实体
public class Order {                     // 聚合根
    private Long id;
    private List<OrderItem> items;       // 内部实体
    private OrderStatus status;          // 值对象（枚举）
    private Address shippingAddress;     // 值对象

    // 聚合根控制内部一致性
    public void addItem(Product product, int quantity) {
        if (status != OrderStatus.DRAFT) {
            throw new BusinessException("只有草稿状态才能添加商品");
        }
        // 业务规则：每个商品最多购买10件
        if (quantity > 10) {
            throw new BusinessException("单件商品购买数量不能超过10");
        }
        items.add(new OrderItem(product, quantity));
    }

    // 聚合根封装内部操作
    public void removeItem(Long itemId) {
        if (status != OrderStatus.DRAFT) {
            throw new BusinessException("只有草稿状态才能删除商品");
        }
        items.removeIf(item -> item.getId().equals(itemId));
    }

    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

**聚合设计原则**：
1. 聚合根是外部访问的唯一入口
2. 聚合内部的对象不能从外部直接获取和修改
3. 一个事务只修改一个聚合
4. 聚合内强一致性，聚合间最终一致性

### 2.4 领域服务（Domain Service）

**不属于任何实体或值对象的业务逻辑。** 当某个业务操作横跨多个实体/值对象，或者涉及多个聚合时，应放在领域服务中。

```java
public class PricingService {
    /**
     * 计算订单最终价格
     * 涉及Order聚合、Coupon聚合、User聚合的跨聚合计算
     */
    public Money calculateFinalPrice(Order order, Coupon coupon, User user) {
        Money basePrice = order.getTotalAmount();

        // 应用用户等级折扣
        Money afterUserDiscount = applyUserLevelDiscount(basePrice, user.getLevel());

        // 应用优惠券
        Money afterCoupon = applyCoupon(afterUserDiscount, coupon);

        return afterCoupon;
    }

    private Money applyUserLevelDiscount(Money price, UserLevel level) {
        // 根据用户等级计算折扣
        return switch (level) {
            case VIP -> price.multiply(new BigDecimal("0.9"));
            case NORMAL -> price;
        };
    }

    private Money applyCoupon(Money price, Coupon coupon) {
        if (coupon == null || !coupon.isValid()) return price;
        return price.subtract(coupon.getDiscount());
    }
}
```

### 2.5 仓储（Repository）

**聚合的持久化接口，定义在领域层，实现在基础设施层。** 对上层只暴露类似集合的操作接口（get/save/delete），隐藏数据库实现细节。

```java
// 领域层：定义仓储接口
public interface OrderRepository {
    Order findById(Long id);
    void save(Order order);
    void delete(Long id);
    Page<Order> findByUserId(Long userId, Pageable pageable);
}

// 基础设施层：实现仓储接口
@Repository
public class OrderRepositoryImpl implements OrderRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ObjectMapper objectMapper; // 用于值对象序列化

    @Override
    public void save(Order order) {
        // 保存聚合根
        String orderSql = "INSERT INTO `order` (id, status, address) VALUES (?, ?, ?)";
        jdbcTemplate.update(orderSql, order.getId(), order.getStatus(), objectMapper.writeValueAsString(order.getShippingAddress()));

        // 保存聚合内部的实体
        String itemSql = "INSERT INTO order_item (id, order_id, product_id, quantity, price) VALUES (?, ?, ?, ?, ?)";
        for (OrderItem item : order.getItems()) {
            jdbcTemplate.update(itemSql, item.getId(), order.getId(), item.getProductId(), item.getQuantity(), item.getPrice());
        }
    }
}
```

---

## 3. 分层架构

```
┌──────────────────────────────────────────────────────┐
│           用户界面层 (User Interface)                  │
│       Controller / REST API / DTO / Assembler         │
└──────────────────────┬───────────────────────────────┘
                       │ 调用
┌──────────────────────▼───────────────────────────────┐
│             应用层 (Application Layer)                  │
│        Application Service — 编排业务流程               │
│        事务管理、权限校验、事件发布                      │
│             不包含业务规则！                             │
└──────────────────────┬───────────────────────────────┘
                       │ 调用
┌──────────────────────▼───────────────────────────────┐
│             领域层 (Domain Layer)                       │
│  实体(Entity) / 值对象(VO) / 聚合根(Aggregate Root)    │
│  领域服务(Domain Service) / 仓储接口(Repository)       │
│             核心业务逻辑在此                            │
└──────────────────────┬───────────────────────────────┘
                       │ 实现
┌──────────────────────▼───────────────────────────────┐
│          基础设施层 (Infrastructure)                   │
│       Repository实现 / ORM / MQ / 外部API调用          │
└──────────────────────────────────────────────────────┘
```

### 各层职责

| 层级 | 职责 | 不包含 |
|------|------|--------|
| **用户界面层** | 请求接收、响应返回、参数校验、DTO转换 | 业务逻辑 |
| **应用层** | 业务流程编排、事务管理、事件发布 | 业务规则 |
| **领域层** | 核心业务逻辑、实体、值对象、聚合、领域服务 | 技术细节 |
| **基础设施层** | 数据库、MQ、缓存、外部API实现 | 业务代码 |

---

## 4. 代码目录结构

```
com.example.order/
├── domain/                          # 领域层
│   ├── model/
│   │   ├── Order.java               # 聚合根
│   │   ├── OrderItem.java           # 实体
│   │   ├── OrderStatus.java         # 值对象（枚举）
│   │   ├── Address.java             # 值对象
│   │   └── Money.java               # 值对象
│   ├── service/
│   │   └── PricingService.java      # 领域服务
│   └── repository/
│       └── OrderRepository.java     # 仓储接口
├── application/                     # 应用层
│   └── OrderApplicationService.java # 应用服务
├── infrastructure/                  # 基础设施层
│   └── repository/
│       └── OrderRepositoryImpl.java # 仓储实现
└── interfaces/                      # 接口层
    ├── OrderController.java         # REST控制器
    └── dto/
        ├── CreateOrderRequest.java  # 请求DTO
        └── OrderResponse.java       # 响应DTO
```

---

## 5. 什么时候用DDD

### 适合使用DDD的场景

| 场景 | 说明 |
|------|------|
| **业务逻辑复杂** | 电商订单流程、金融交易、保险理赔 |
| **团队规模大** | 需要清晰的代码边界和统一语言 |
| **长期维护** | 核心系统，生命周期超过3-5年 |
| **频繁需求变更** | 业务规则经常变化，需要灵活应对 |

### 不适合使用DDD的场景

| 场景 | 说明 |
|------|------|
| **简单CRUD系统** | 后台管理、数据报表、配置中心 |
| **业务不复杂的初创项目** | MVP阶段快速验证为主 |
| **团队小、快速交付阶段** | 短期项目，DDD会增加前期设计成本 |

---

## 6. 实战建议

### 6.1 渐进式引入

不要试图一步到位地推DDD。推荐的渐进式路径：

1. **从分层开始**：先按四层结构（接口层、应用层、领域层、基础设施层）组织代码
2. **把业务逻辑从Service抽到领域对象**：识别Service中的业务规则，迁移到对应的实体或值对象中
3. **识别聚合边界**：找到一组紧密相关的对象，确定聚合根
4. **引入仓储模式**：将数据访问抽象为仓储接口

### 6.2 注意事项

| 事项 | 建议 |
|------|------|
| **不要过度设计** | 简单CRUD用三层架构就够了 |
| **统一语言** | 团队使用统一的业务术语，代码中的类名、方法名与业务语言一致 |
| **聚合不宜过大** | 一个聚合包含3-5个实体为宜，过大增加事务复杂度 |
| **跨聚合用最终一致性** | 不同聚合间的数据同步，使用事件驱动实现最终一致性 |

> 核心原则：**让代码说出业务故事，而不是让业务迁就代码结构。**

### 6.3 DDD与设计模式的关系

DDD中的许多概念本身就是设计模式的应用：

| DDD概念 | 对应设计模式 | 说明 |
|---------|-------------|------|
| 聚合根 | 外观模式 | 对外提供统一入口，隐藏内部细节 |
| 仓储 | 工厂方法+适配器 | 接口定义在领域层，实现在基础设施层 |
| 值对象 | 原型模式 | 不可变，可共享 |
| 领域事件 | 观察者模式 | 事件驱动，解耦聚合间通信 |
| 应用服务 | 外观模式 | 编排业务流程，封装复杂调用 |
| 规范模式 | 策略模式 | 封装业务规则，灵活组合 |

> DDD不是设计模式的替代，而是设计模式在业务复杂场景下的系统性运用。

---

## 7. DDD核心模式进阶：领域事件

### 7.1 什么是领域事件

领域事件（Domain Event）是DDD中用于**聚合间通信**的核心机制。当一个聚合发生状态变更时，发布一个领域事件，其他聚合监听并做出响应。

```java
// 领域事件基类
public abstract class DomainEvent {
    private final Long id;
    private final LocalDateTime occurredAt;

    public DomainEvent() {
        this.id = SnowflakeIdGenerator.nextId();
        this.occurredAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}

// 具体领域事件
public class OrderPaidEvent extends DomainEvent {
    private final Long orderId;
    private final Long userId;
    private final Money paidAmount;

    public OrderPaidEvent(Long orderId, Long userId, Money paidAmount) {
        super();
        this.orderId = orderId;
        this.userId = userId;
        this.paidAmount = paidAmount;
    }
    // getters...
}
```

### 7.2 在聚合中发布领域事件

```java
public class Order {
    private List<DomainEvent> domainEvents = new ArrayList<>();

    public void pay(Money amount) {
        if (this.status != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException("订单状态不正确");
        }
        if (!this.totalAmount.equals(amount)) {
            throw new BusinessException("支付金额不正确");
        }

        this.status = OrderStatus.PAID;

        // 发布领域事件（暂存，待应用层发送）
        domainEvents.add(new OrderPaidEvent(this.id, this.userId, amount));
    }

    // 获取并清除已发布的事件
    public List<DomainEvent> releaseEvents() {
        List<DomainEvent> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return events;
    }
}
```

### 7.3 应用层发送事件

```java
@Service
@Transactional
public class OrderApplicationService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private DomainEventPublisher eventPublisher;

    public void payOrder(Long orderId, Money amount) {
        Order order = orderRepository.findById(orderId);
        order.pay(amount);
        orderRepository.save(order);

        // 发布领域事件至消息中间件
        for (DomainEvent event : order.releaseEvents()) {
            eventPublisher.publish(event);
        }
    }
}

// 监听方：库存聚合
@Component
public class OrderPaidEventHandler {
    @EventListener
    public void on(OrderPaidEvent event) {
        // 扣减库存（跨聚合，最终一致性）
        inventoryService.deduct(event.getOrderId());
    }
}
```

> 领域事件是聚合间实现**最终一致性**的关键手段。一个事务只修改一个聚合，跨聚合的状态同步通过事件驱动异步完成。

---

## 8. DDD常见问题（FAQ）

### 8.1 DDD和微服务的关系？

DDD的**限界上下文（Bounded Context）** 天然对应微服务的服务边界。每个限界上下文可以演化为一个独立的微服务，不同上下文之间通过事件或API通信。

```text
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  订单上下文  │────▶│  库存上下文  │     │  支付上下文  │
│  (订单微服务) │事件  │ (库存微服务) │     │ (支付微服务) │
└─────────────┘     └─────────────┘     └─────────────┘
```

### 8.2 聚合根太大怎么办？

拆分聚合。识别聚合内部的内聚性，将松耦合的实体拆分为独立的聚合。比如：

- 原始的`Order`聚合包含`Order`、`OrderItem`、`Payment`、`Delivery`
- 拆分为：`Order`聚合（Order + OrderItem）、`Payment`聚合、`Delivery`聚合
- 通过订单ID关联，通过领域事件同步状态

### 8.3 DDD对性能有影响吗？

DDD的设计本身不牺牲性能，但需要注意：

| 关注点 | 优化策略 |
|--------|----------|
| 聚合加载 | 使用懒加载，一次只加载需要的聚合 |
| 大聚合 | 拆分为小聚合，减少单次加载数据量 |
| 频繁查询 | 使用读模型（CQRS），绕过聚合直接查询 |
| 跨聚合事务 | 使用事件驱动，最终一致性替代强一致性 |

### 8.4 值对象一定要用record吗？

不一定。`record`是Java 14+的特性，天然满足值对象要求（不可变、基于属性判等），但不是必须的。可以用普通类，需要手动实现：

```java
// 普通类实现值对象
public final class Address {
    private final String province;
    private final String city;
    private final String street;

    public Address(String province, String city, String street) {
        this.province = province;
        this.city = city;
        this.street = street;
    }

    // 只提供getter，不提供setter
    public String getProvince() { return province; }
    public String getCity() { return city; }
    public String getStreet() { return street; }

    // 基于所有属性判等
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address)) return false;
        Address address = (Address) o;
        return Objects.equals(province, address.province)
                && Objects.equals(city, address.city)
                && Objects.equals(street, address.street);
    }

    @Override
    public int hashCode() {
        return Objects.hash(province, city, street);
    }
}
```

---

## 9. 推荐学习资源

| 资源 | 说明 |
|------|------|
| **《领域驱动设计》** Eric Evans 著（蓝皮书） | DDD理论奠基之作，必读 |
| **《实现领域驱动设计》** Vaughn Vernon 著（红皮书） | DDD实践指南，更贴近工程 |
| **《敏捷软件开发：原则、模式与实践》** Robert C. Martin 著 | SOLID原则的最佳实践 |
| **DDD Sample App**（GitHub: ddd-by-examples） | DDD的代码示例，工程化实现 |
| **Axon Framework** | Java DDD + CQRS + Event Sourcing框架 |

---

## 10. 总结

```
传统三层架构                    DDD四层架构
┌──────────────┐              ┌──────────────┐
│  Controller   │              │   接口层      │
├──────────────┤              ├──────────────┤
│  Service     │ ← 胖Service  │   应用层      │ ← 只编排，无规则
│  (业务+数据)  │              ├──────────────┤
├──────────────┤              │   领域层      │ ← 业务规则在此
│  DAO         │              ├──────────────┤
└──────────────┘              │  基础设施层    │
                              └──────────────┘
```

DDD的核心不在于使用复杂的架构模式，而在于**让代码反映业务**。从最简单的"把业务逻辑放到实体中"开始，逐步引入聚合、领域事件等概念，关键是保持代码与业务语言的一致性。

> 对校招/实习生阶段来说：理解DDD的核心思想（聚合、实体、值对象、分层），面试能说清楚即可。不要一上来就强行DDD，简单的CRUD用三层架构就够了。可以从"把业务逻辑从Service抽到Domain对象里"开始，逐步过渡。
