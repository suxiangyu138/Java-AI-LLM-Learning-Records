# DDD 领域驱动设计入门

## DDD 解决什么问题

传统 MVC 三层（Controller-Service-DAO）在复杂业务下会出现"胖 Service"——几千行的 Service 类，业务逻辑缠绕在一起，改一处影响一片。

DDD 的核心思想：**让代码结构与业务概念对齐。** 业务专家说"订单包含订单项、有状态流转"，代码里也应该有对应的 `Order` 聚合、`OrderItem` 实体、`OrderStatus` 值对象。

## 核心概念

### 实体（Entity）
有唯一标识，会变化的对象。标识不变，属性可变。
```java
public class User {
    private Long id;           // 唯一标识
    private String name;       // 可变属性
    private String email;      // 可变属性
}
```

### 值对象（Value Object）
没有唯一标识，通过属性值来定义相等性。不可变。
```java
public record Address(String province, String city, String street) { }
public record Money(BigDecimal amount, String currency) { }
```

### 聚合（Aggregate）与聚合根（Aggregate Root）
一组相关对象的集合，有一个"根实体"作为外部访问的唯一入口。
```java
// Order 是聚合根，OrderItem 是聚合内部实体
public class Order {                     // 聚合根
    private Long id;
    private List<OrderItem> items;       // 内部实体
    private OrderStatus status;
    private Address shippingAddress;     // 值对象

    public void addItem(Product product, int quantity) {
        // 聚合根控制内部一致性
        if (status != OrderStatus.DRAFT) {
            throw new BusinessException("只有草稿状态才能添加商品");
        }
        items.add(new OrderItem(product, quantity));
    }
}
```

### 领域服务（Domain Service）
不属于任何实体或值对象的业务逻辑。
```java
public class PricingService {
    public Money calculatePrice(Order order, Coupon coupon) {
        // 跨聚合的计算逻辑
    }
}
```

### 仓储（Repository）
聚合的持久化接口，定义在领域层，实现在基础设施层。
```java
public interface OrderRepository {
    Order findById(Long id);
    void save(Order order);
}
```

## 分层架构

```
用户界面层 (Controller / REST API)
    ↓
应用层 (Application Service) — 编排业务流程，不包含业务规则
    ↓
领域层 (Domain) — 实体、值对象、领域服务、Repository 接口
    ↓
基础设施层 (Infrastructure) — 数据库实现、消息队列、外部 API
```

## 代码目录结构

```
order/
├── domain/                    # 领域层
│   ├── model/
│   │   ├── Order.java         # 聚合根
│   │   ├── OrderItem.java     # 实体
│   │   └── OrderStatus.java   # 值对象（枚举）
│   ├── service/
│   │   └── PricingService.java
│   └── repository/
│       └── OrderRepository.java  # 接口
├── application/               # 应用层
│   └── OrderApplicationService.java
├── infrastructure/            # 基础设施层
│   └── repository/
│       └── OrderRepositoryImpl.java
└── interfaces/                # 接口层
    └── OrderController.java
```

## 什么时候用 DDD

**适合：**
- 业务逻辑复杂（如电商订单流程、金融交易、保险理赔）
- 团队规模大，需要清晰的代码边界
- 长期维护的核心系统

**不适合：**
- 简单 CRUD 系统（如后台管理、数据报表）
- 业务不复杂的初创项目
- 团队小、追求快速交付的阶段

## 实战建议

作为校招/实习生阶段：
- 理解 DDD 的核心思想（聚合、实体、值对象、分层），面试能说清楚
- 不要一上来就强行 DDD，简单的 CRUD 用三层架构就够了
- 可以从"把业务逻辑从 Service 抽到 Domain 对象里"开始，逐步过渡
- 关键是：**让代码说出业务故事，而不是让业务迁就代码结构**
