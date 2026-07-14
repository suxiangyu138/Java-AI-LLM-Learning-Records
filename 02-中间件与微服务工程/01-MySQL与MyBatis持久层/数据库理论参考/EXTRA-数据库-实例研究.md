# 数据库系统概念（实例研究）—— Java后端开发视角深度剖析

## 📑 目录

- [一、实例研究概述](#一实例研究概述)
- [二、需求分析（电商交易系统核心需求）](#二需求分析电商交易系统核心需求)
- [三、概念数据模型（E-R模型设计）](#三概念数据模型e-r模型设计)
- [四、逻辑数据模型（关系模式设计）](#四逻辑数据模型关系模式设计)
- [五、物理数据模型（MySQL建表语句）](#五物理数据模型mysql建表语句)
- [六、Java后端核心业务实现](#六java后端核心业务实现)
- [七、性能优化（数据库+Java层）](#七性能优化数据库java层)
- [八、分布式扩展（分库分表+读写分离）](#八分布式扩展分库分表读写分离)
- [九、运维与监控](#九运维与监控)
- [十、实例研究总结](#十实例研究总结)

---

## 一、实例研究概述

本次实例研究以**企业级电商平台核心交易系统**为具体场景，完整覆盖数据库需求分析、概念建模、逻辑设计、物理实现、SQL开发、事务处理、性能优化、分布式扩展、运维监控全流程，严格贴合Java后端开发实践。

> 所有设计、代码、优化均基于 **MySQL 8.0 + Spring Boot 2.7 + MyBatis-Plus** 技术栈，可直接落地到真实项目。

---

## 二、需求分析（电商交易系统核心需求）

### 2.1 核心业务模块

| 模块 | 功能说明 |
|------|---------|
| 用户模块 | 用户注册、登录、信息管理、地址管理 |
| 商品模块 | 商品上架/下架、库存管理、分类管理、规格管理 |
| 购物车模块 | 添加/删除商品、数量修改、价格计算 |
| 订单模块 | 创建订单、订单状态流转、订单查询、订单取消 |
| 支付模块 | 支付对接、支付状态同步、退款处理 |
| 库存模块 | 实时扣减、库存回滚、库存预警 |
| 物流模块 | 物流信息录入、轨迹查询、配送状态更新 |

### 2.2 核心数据特征

| 指标 | 数值 |
|------|------|
| 数据量 | 用户1000万+、商品100万+、订单日均100万+ |
| 并发特征 | 秒杀场景峰值QPS 1万+，普通交易QPS 1000+ |
| 一致性要求 | 订单创建、库存扣减、支付扣款必须强一致 |
| 实时性要求 | 库存、订单状态、支付状态需实时更新 |
| 可扩展性 | 支持分库分表、读写分离、分布式事务 |

---

## 三、概念数据模型（E-R模型设计）

### 3.1 核心实体及属性

| 实体 | 关键属性 |
|------|---------|
| 用户（user） | 用户ID、用户名、密码、手机号、邮箱、创建时间、状态 |
| 商品（product） | 商品ID、商品名、价格、分类ID、库存、状态、创建时间 |
| 商品分类（category） | 分类ID、分类名、父分类ID、层级、状态 |
| 购物车（cart） | 购物车ID、用户ID、商品ID、数量、创建时间 |
| 订单（order_info） | 订单ID、用户ID、总金额、状态、支付方式、创建时间、地址ID |
| 订单详情（order_item） | 详情ID、订单ID、商品ID、单价、数量、小计金额 |
| 支付记录（payment） | 支付ID、订单ID、支付金额、支付状态、支付时间、交易号 |
| 库存记录（stock_log） | 日志ID、商品ID、变动数量、变动类型、关联订单ID、时间 |
| 收货地址（address） | 地址ID、用户ID、收货人、手机号、地址、默认状态 |

### 3.2 实体间关系

| 关系 | 类型 |
|------|------|
| 用户 ↔ 购物车 | 一对多（一个用户多个购物车记录） |
| 用户 ↔ 订单 | 一对多（一个用户多个订单） |
| 订单 ↔ 订单详情 | 一对多（一个订单多个商品详情） |
| 订单 ↔ 支付 | 一对一（一个订单对应一条支付记录） |
| 商品 ↔ 库存记录 | 一对多（一个商品多条库存变动日志） |
| 商品分类 ↔ 商品 | 一对多（一个分类多个商品） |
| 用户 ↔ 收货地址 | 一对多（一个用户多个地址） |

---

## 四、逻辑数据模型（关系模式设计）

基于E-R模型转换为关系模式，遵循**第三范式（3NF）**，消除冗余，保证数据一致性：

1. `user(user_id, username, password, phone, email, create_time, status)`
2. `category(category_id, category_name, parent_id, level, status)`
3. `product(product_id, product_name, price, category_id, stock, status, create_time)`
4. `cart(cart_id, user_id, product_id, quantity, create_time)`
5. `address(address_id, user_id, receiver, phone, address, is_default)`
6. `order_info(order_id, user_id, total_amount, order_status, pay_type, create_time, address_id)`
7. `order_item(item_id, order_id, product_id, unit_price, quantity, subtotal)`
8. `payment(pay_id, order_id, pay_amount, pay_status, pay_time, trade_no)`
9. `stock_log(log_id, product_id, change_num, change_type, order_id, create_time)`

---

## 五、物理数据模型（MySQL建表语句）

### 5.1 用户表（user）

```sql
CREATE TABLE `user` (
    `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` varchar(50) NOT NULL COMMENT '用户名',
    `password` varchar(100) NOT NULL COMMENT '密码（BCrypt加密）',
    `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
    `email` varchar(50) DEFAULT NULL COMMENT '邮箱',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1正常 0禁用',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 5.2 商品表（product）

```sql
CREATE TABLE `product` (
    `product_id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `product_name` varchar(100) NOT NULL COMMENT '商品名称',
    `price` decimal(10,2) NOT NULL COMMENT '商品价格',
    `category_id` bigint NOT NULL COMMENT '分类ID',
    `stock` int NOT NULL DEFAULT '0' COMMENT '库存数量',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1上架 0下架',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`product_id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';
```

### 5.3 订单主表（order_info）

```sql
CREATE TABLE `order_info` (
    `order_id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
    `order_status` tinyint NOT NULL DEFAULT '0' COMMENT '订单状态：0待支付 1已支付 2已发货 3已完成 4已取消',
    `pay_type` tinyint DEFAULT NULL COMMENT '支付方式：1微信 2支付宝',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `address_id` bigint NOT NULL COMMENT '收货地址ID',
    PRIMARY KEY (`order_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status` (`order_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';
```

### 5.4 订单详情表（order_item）

```sql
CREATE TABLE `order_item` (
    `item_id` bigint NOT NULL AUTO_INCREMENT COMMENT '详情ID',
    `order_id` bigint NOT NULL COMMENT '订单ID',
    `product_id` bigint NOT NULL COMMENT '商品ID',
    `unit_price` decimal(10,2) NOT NULL COMMENT '商品单价',
    `quantity` int NOT NULL COMMENT '购买数量',
    `subtotal` decimal(10,2) NOT NULL COMMENT '小计金额',
    PRIMARY KEY (`item_id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单详情表';
```

### 5.5 支付记录表（payment）

```sql
CREATE TABLE `payment` (
    `pay_id` bigint NOT NULL AUTO_INCREMENT COMMENT '支付ID',
    `order_id` bigint NOT NULL COMMENT '订单ID',
    `pay_amount` decimal(10,2) NOT NULL COMMENT '支付金额',
    `pay_status` tinyint NOT NULL DEFAULT '0' COMMENT '支付状态：0待支付 1已支付 2已退款',
    `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
    `trade_no` varchar(64) DEFAULT NULL COMMENT '第三方交易号',
    PRIMARY KEY (`pay_id`),
    UNIQUE KEY `uk_order` (`order_id`),
    KEY `uk_trade_no` (`trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';
```

### 5.6 库存变动日志表（stock_log）

```sql
CREATE TABLE `stock_log` (
    `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `product_id` bigint NOT NULL COMMENT '商品ID',
    `change_num` int NOT NULL COMMENT '变动数量（正数增加 负数减少）',
    `change_type` tinyint NOT NULL COMMENT '变动类型：1下单扣减 2取消回滚 3补货',
    `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变动时间',
    PRIMARY KEY (`log_id`),
    KEY `idx_product` (`product_id`),
    KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变动日志表';
```

---

## 六、Java后端核心业务实现（基于Spring Boot + MyBatis-Plus）

### 6.1 订单创建核心事务（强一致性，防超卖）

#### 6.1.1 Service层代码

```java
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderInfo> implements OrderService {
    
    private final ProductMapper productMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final StockLogMapper stockLogMapper;

    public OrderServiceImpl(ProductMapper productMapper, OrderItemMapper orderItemMapper,
                            PaymentMapper paymentMapper, StockLogMapper stockLogMapper) {
        this.productMapper = productMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentMapper = paymentMapper;
        this.stockLogMapper = stockLogMapper;
    }

    /**
     * 创建订单（核心事务：扣库存+生成订单+创建支付记录）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderInfo createOrder(OrderCreateDTO dto) {
        // 1. 查询商品并加悲观锁，防止超卖
        Product product = productMapper.selectByIdForUpdate(dto.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (product.getStock() < dto.getQuantity()) {
            throw new RuntimeException("库存不足");
        }

        // 2. 扣减商品库存
        int newStock = product.getStock() - dto.getQuantity();
        product.setStock(newStock);
        productMapper.updateById(product);

        // 3. 记录库存变动日志
        StockLog stockLog = new StockLog();
        stockLog.setProductId(dto.getProductId());
        stockLog.setChangeNum(-dto.getQuantity());
        stockLog.setChangeType(1);
        stockLogMapper.insert(stockLog);

        // 4. 创建订单主表
        OrderInfo order = new OrderInfo();
        order.setUserId(dto.getUserId());
        order.setAddressId(dto.getAddressId());
        BigDecimal totalAmount = product.getPrice().multiply(new BigDecimal(dto.getQuantity()));
        order.setTotalAmount(totalAmount);
        order.setOrderStatus(0);
        this.save(order);

        // 5. 创建订单详情
        OrderItem item = new OrderItem();
        item.setOrderId(order.getOrderId());
        item.setProductId(dto.getProductId());
        item.setUnitPrice(product.getPrice());
        item.setQuantity(dto.getQuantity());
        item.setSubtotal(totalAmount);
        orderItemMapper.insert(item);

        // 6. 创建支付记录
        Payment payment = new Payment();
        payment.setOrderId(order.getOrderId());
        payment.setPayAmount(totalAmount);
        payment.setPayStatus(0);
        paymentMapper.insert(payment);

        return order;
    }
}
```

#### 6.1.2 Mapper层（悲观锁查询）

```java
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ProductMapper extends BaseMapper<Product> {
    /**
     * 根据ID查询商品并加排他锁
     */
    @Select("SELECT * FROM product WHERE product_id = #{productId} FOR UPDATE")
    Product selectByIdForUpdate(@Param("productId") Long productId);
}
```

### 6.2 订单取消与库存回滚（事务补偿）

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void cancelOrder(Long orderId) {
    // 1. 查询订单
    OrderInfo order = this.getById(orderId);
    if (order == null || order.getOrderStatus() != 0) {
        throw new RuntimeException("订单状态不允许取消");
    }

    // 2. 更新订单状态为已取消
    order.setOrderStatus(4);
    this.updateById(order);

    // 3. 查询订单详情
    OrderItem item = orderItemMapper.selectOne(
        new QueryWrapper<OrderItem>().eq("order_id", orderId));

    // 4. 回滚商品库存
    Product product = productMapper.selectById(item.getProductId());
    product.setStock(product.getStock() + item.getQuantity());
    productMapper.updateById(product);

    // 5. 记录库存回滚日志
    StockLog stockLog = new StockLog();
    stockLog.setProductId(item.getProductId());
    stockLog.setChangeNum(item.getQuantity());
    stockLog.setChangeType(2);
    stockLog.setOrderId(orderId);
    stockLogMapper.insert(stockLog);

    // 6. 更新支付记录状态
    Payment payment = paymentMapper.selectOne(
        new QueryWrapper<Payment>().eq("order_id", orderId));
    if (payment != null) {
        payment.setPayStatus(2);
        paymentMapper.updateById(payment);
    }
}
```

### 6.3 订单列表查询（关联查询，性能优化）

```java
@Override
public List<OrderVO> getOrderList(Long userId) {
    // 1. 查询用户订单主表
    List<OrderInfo> orderList = this.list(
        new QueryWrapper<OrderInfo>().eq("user_id", userId)
            .orderByDesc("create_time"));
    if (CollectionUtils.isEmpty(orderList)) {
        return Collections.emptyList();
    }

    // 2. 批量查询订单详情（避免N+1查询）
    List<Long> orderIds = orderList.stream()
        .map(OrderInfo::getOrderId).collect(Collectors.toList());
    List<OrderItem> itemList = orderItemMapper.selectList(
        new QueryWrapper<OrderItem>().in("order_id", orderIds));
    Map<Long, List<OrderItem>> itemMap = itemList.stream()
        .collect(Collectors.groupingBy(OrderItem::getOrderId));

    // 3. 封装VO返回
    return orderList.stream().map(order -> {
        OrderVO vo = new OrderVO();
        vo.setOrderId(order.getOrderId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setCreateTime(order.getCreateTime());
        vo.setItemList(itemMap.getOrDefault(order.getOrderId(), Collections.emptyList()));
        return vo;
    }).collect(Collectors.toList());
}
```

---

## 七、性能优化（数据库+Java层）

### 7.1 索引优化

1. **订单表**：`user_id`、`order_status`、`create_time` 建立联合索引，优化用户订单列表查询

```sql
CREATE INDEX idx_user_status_time ON order_info(user_id, order_status, create_time DESC);
```

2. **商品表**：`category_id` + `status` 联合索引，优化分类商品查询

```sql
CREATE INDEX idx_category_status ON product(category_id, status);
```

3. **避免冗余索引**：删除重复、低效索引，定期使用 `EXPLAIN` 分析慢查询

### 7.2 SQL优化

- 禁止 `SELECT *`，只查询业务需要的字段
- 关联查询控制在3张表以内，复杂查询拆分为多次简单查询
- 批量操作替代单条循环：批量插入订单详情、批量更新库存
- 避免在索引字段使用函数、隐式类型转换，防止索引失效

### 7.3 Java层优化

| 优化方式 | 说明 |
|---------|------|
| 本地缓存（Caffeine） | 缓存商品分类、热门商品信息，减少数据库查询 |
| Redis缓存 | 缓存用户订单列表、商品库存，缓存过期时间设置为5分钟 |
| 异步处理 | 订单创建后，异步发送消息通知、更新用户积分，不阻塞主流程 |
| 分页查询 | 所有列表查询强制分页，避免全表扫描 |

### 7.4 高并发优化（秒杀场景）

1. **库存预加载到Redis**：秒杀开始前，将商品库存写入Redis，扣减优先走Redis

```java
// 初始化秒杀库存
public void initSeckillStock(Long productId, Integer stock) {
    redisTemplate.opsForValue().set("seckill:stock:" + productId, stock.toString());
}

// Redis扣减库存（原子操作）
public boolean deductSeckillStock(Long productId) {
    Long remain = redisTemplate.opsForValue().decrement("seckill:stock:" + productId);
    return remain >= 0;
}
```

2. **消息队列削峰**：用户秒杀请求先进入Kafka，消费者异步处理订单创建
3. **接口限流**：使用Sentinel对秒杀接口限流，防止服务崩溃

---

## 八、分布式扩展（分库分表+读写分离）

### 8.1 分库分表（Sharding-JDBC配置）

订单表数据量过大，按 `order_id` 哈希分表（2库4表）：

```yaml
spring:
  shardingsphere:
    datasource:
      names: ds0, ds1
    rules:
      sharding:
        tables:
          order_info:
            actual-data-nodes: ds$->{0..1}.order_info_$->{0..1}
            database-strategy:
              standard:
                sharding-column: order_id
                sharding-algorithm-name: database-inline
            table-strategy:
              standard:
                sharding-column: order_id
                sharding-algorithm-name: table-inline
        sharding-algorithms:
          database-inline:
            type: INLINE
            props:
              algorithm-expression: ds$->{order_id % 2}
          table-inline:
            type: INLINE
            props:
              algorithm-expression: order_info_$->{order_id % 2}
```

### 8.2 读写分离

主库写入，从库读取，配置Sharding-JDBC读写分离策略，读请求自动路由到从库，写请求路由到主库。

### 8.3 分布式事务（Seata AT模式）

跨库操作（订单+库存+支付）使用Seata保证分布式事务一致性：

```java
@GlobalTransactional(rollbackFor = Exception.class)
public void createSeckillOrder(OrderCreateDTO dto) {
    // 订单服务（ds0）
    orderService.createOrder(dto);
    // 库存服务（ds1）
    stockService.deductStock(dto.getProductId(), dto.getQuantity());
    // 支付服务（ds0）
    payService.createPayment(dto.getOrderId(), dto.getTotalAmount());
}
```

---

## 九、运维与监控

### 9.1 慢查询监控

开启MySQL慢查询日志，阈值设置为1秒，定期分析慢查询日志，优化低效SQL。

### 9.2 索引监控

使用 `sys.schema_unused_indexes` 查看未使用索引，及时清理冗余索引。

### 9.3 数据备份

| 备份类型 | 策略 |
|---------|------|
| 全量备份 | 每日凌晨执行 `mysqldump` 全量备份 |
| 增量备份 | 开启binlog，按小时备份binlog文件 |
| 恢复演练 | 每月进行一次数据恢复演练，保证备份有效性 |

### 9.4 高可用保障

- **主从架构**：一主两从，主库故障自动切换
- **哨兵模式**：Redis哨兵监控缓存服务，保证缓存高可用
- **服务熔断**：使用Resilience4j对数据库依赖服务做熔断，防止级联故障

---

## 十、实例研究总结

本次电商交易系统实例研究，完整覆盖了数据库从需求到落地的全流程，核心价值如下：

1. **理论落地**：将E-R模型、范式、事务、索引等数据库理论转化为真实表结构与业务代码
2. **工程实践**：掌握Java后端与数据库交互的核心技巧，包括事务控制、SQL优化、缓存设计
3. **高可用设计**：解决高并发、分布式场景下的一致性、性能、扩展性问题
4. **运维能力**：掌握数据库监控、备份、优化的实用方法，适配企业级运维需求

> 该实例可作为Java后端开发的通用数据库模板，适用于电商、零售、生鲜等交易类业务，通过调整分库分表策略、索引设计、缓存规则，可适配不同规模的业务场景。

---

## 📖 相关阅读

- [数据库-恢复系统](数据库-恢复系统.md)
- [数据库-并发控制](数据库-并发控制.md)
- [数据库-数据仓库与数据挖掘](数据库-数据仓库与数据挖掘.md)
- [Java后端-Spring事务](../Java%20后端/01-基础核心层/Spring/Spring事务管理.md)
- [Java后端-MyBatis-Plus](../Java%20后端/01-基础核心层/ORM框架/MyBatis-Plus.md)
