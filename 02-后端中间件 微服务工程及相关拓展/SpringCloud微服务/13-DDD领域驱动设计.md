# 13 - DDD 领域驱动设计

> 🎯 DDD 是微服务拆分的理论武器 — 限界上下文划定服务边界、聚合保证事务一致性、领域事件实现服务解耦。代码和业务语言对齐是核心价值

---

## 目录

1. [DDD 核心概念](#1-ddd-核心概念)
2. [战略设计：限界上下文](#2-战略设计限界上下文)
3. [战术设计：聚合与实体](#3-战术设计聚合与实体)
4. [领域事件](#4-领域事件)
5. [DDD 分层架构](#5-ddd-分层架构)

---

## 1. DDD 核心概念

| 概念 | 说明 | 微服务映射 |
|------|------|-----------|
| **限界上下文** | 领域模型的边界 | ⭐ 微服务拆分依据 |
| **实体** | 有唯一标识的对象 | JPA Entity |
| **值对象** | 无标识，不可变 | `@Embeddable` |
| **聚合** | 一组强关联的实体和值对象 | 事务一致性边界 |
| **聚合根** | 聚合的唯一入口 | `@AggregateRoot` |
| **领域事件** | 聚合中发生的重要事件 | MQ 消息 |
| **仓储** | 聚合的持久化接口 | `@Repository` |
| **领域服务** | 无状态的领域操作 | `@Service` + 领域逻辑 |

---

## 2. 战略设计：限界上下文

> ⭐ **限界上下文 = 微服务的天然边界**。

```text
电商 DDD 限界上下文拆分：

  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
  │  用户上下文  │   │  订单上下文  │   │  商品上下文  │
  │  User        │   │  Order      │   │  Product    │
  │  Address     │   │  OrderItem  │   │  Category   │
  │  ★ 值对象    │   │  ★ 聚合根   │   │  Inventory  │
  └──────┬──────┘   └──────┬──────┘   └──────┬──────┘
         │                 │                  │
   user-service      order-service     product-service
```

### 限界上下文的识别方法

| 方法 | 说明 |
|------|------|
| **业务能力分析** | 按业务功能划分：用户管理、订单管理、商品管理 |
| **事件风暴** | 全团队通过事件梳理业务流程，自然浮现边界 |
| **语言边界** | 同一个词在不同上下文有不同含义 → 划分信号 |
| **数据主权** | 谁拥有这份数据谁负责 |

```text
语言边界示例：
  "用户" 在 用户上下文中 = 注册信息（用户名/密码/邮箱）
  "用户" 在 订单上下文中 = 买家信息（收货地址/联系方式）
  "用户" 在 物流上下文中 = 收件人信息（姓名/电话/地址）

→ 同一个词，不同含义 → 应该在不同的限界上下文中
```

---

## 3. 战术设计：聚合与实体

### 3.1 聚合设计原则

```text
聚合设计三原则：
  1. 聚合内的对象必须保持业务不变性（一致性边界）
  2. 聚合要尽量小（一次事务只修改一个聚合）
  3. 通过 ID 引用其他聚合（而非对象引用）
```

```java
// ⭐ 订单聚合（Order = 聚合根）
@AggregateRoot
public class Order {
    private OrderId id;                    // 值对象 — 订单ID
    private UserId userId;                 // ⚠️ 引用其他聚合用 ID，不用对象
    private List<OrderItem> items;         // 实体（聚合内部）
    private OrderStatus status;            // 值对象 — 状态
    private Money totalAmount;             // 值对象 — 金额

    // ⭐ 聚合根是唯一入口，保证不变性
    public void addItem(ProductId productId, int quantity, Money price) {
        if (status != OrderStatus.PENDING) {
            throw new OrderException("只有待支付订单才能修改商品");
        }
        items.add(new OrderItem(productId, quantity, price));
        recalculateTotal();
    }

    public void pay() {
        if (status != OrderStatus.PENDING) {
            throw new OrderException("订单状态不允许支付");
        }
        status = OrderStatus.PAID;
        // 发布领域事件
        DomainEventPublisher.publish(new OrderPaidEvent(this.id, this.totalAmount));
    }
}
```

### 3.2 值对象 vs 实体

| 维度 | 实体 | 值对象 |
|------|------|--------|
| 唯一标识 | ✅ 有 ID | ❌ 无 ID |
| 可变性 | 可变 | ⭐ 不可变（推荐） |
| 相等性 | ID 相等 | 属性相等 |
| 持久化 | 单独表 | 嵌入父表 / JSON 列 |
| 示例 | User, Order | Money, Address, OrderId |

```java
// 值对象 — 不可变、自校验
@Embeddable
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金额不能为负");
        }
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("货币类型不一致");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

---

## 4. 领域事件

```java
// 领域事件 — 表示"已经发生的事实"
public record OrderPaidEvent(
    OrderId orderId,
    Money amount,
    Instant occurredAt
) implements DomainEvent {
    public OrderPaidEvent(OrderId orderId, Money amount) {
        this(orderId, amount, Instant.now());
    }
}

// 发布领域事件
@Transactional
public void payOrder(OrderId orderId) {
    Order order = orderRepository.findById(orderId);
    order.pay();                                    // 聚合根内部发布事件
    orderRepository.save(order);
    // Spring Data 自动收集 @DomainEvents 并发布
}

// 消费领域事件 — 跨服务解耦
@Component
public class OrderPaidHandler {
    @EventListener
    public void onOrderPaid(OrderPaidEvent event) {
        // 发 MQ 通知库存服务、积分服务
        kafkaTemplate.send("order-paid", event);
    }
}
```

---

## 5. DDD 分层架构

```text
┌──────────────────────────────────────────────┐
│   Interface Layer（接口层）                    │
│   Controller / DTO / Converter               │
├──────────────────────────────────────────────┤
│   Application Layer（应用层，薄层）            │
│   编排领域对象，处理事务、权限、发布事件        │
├──────────────────────────────────────────────┤
│   Domain Layer（领域层 ⭐ 核心）              │
│   Entity / ValueObject / Aggregate / Service  │
│   ★ 不依赖任何外部框架                        │
├──────────────────────────────────────────────┤
│   Infrastructure Layer（基础设施层）           │
│   Repository Impl / MQ / Cache / 外部 API     │
└──────────────────────────────────────────────┘
```

```java
// 应用服务 — 编排，不包含业务逻辑
@Service
public class OrderApplicationService {
    private final OrderRepository orderRepository;

    @Transactional
    public OrderDTO payOrder(Long orderId) {
        Order order = orderRepository.findById(new OrderId(orderId))
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.pay();                          // 委托给领域层
        orderRepository.save(order);
        return OrderConverter.toDTO(order);
    }
}

// 领域服务 — 跨聚合的业务逻辑
@DomainService
public class OrderPricingService {
    public Money calculatePrice(Order order, List<Coupon> coupons) {
        Money subtotal = order.getItems().stream()
            .map(OrderItem::getTotalPrice)
            .reduce(Money.ZERO, Money::add);
        // 应用优惠券逻辑
        return applyCoupons(subtotal, coupons);
    }
}
```

> 🎯 **DDD 微服务铁三角**：限界上下文 = 服务边界、聚合 = 事务边界、领域事件 = 服务解耦。理解这三个映射，DDD 才算学到位。
