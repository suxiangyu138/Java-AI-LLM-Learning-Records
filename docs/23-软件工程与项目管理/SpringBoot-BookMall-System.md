# 在线图书商城（SpringBoot 企业级实战）

> 简历主推项目 · 电商核心域全覆盖 · 重点突出 **事务、库存扣减、状态机、分布式锁、最终一致性**

---

## 一、项目定位与简历价值

| 维度 | 说明 |
| --- | --- |
| 项目名称 | BookMall —— 基于 SpringBoot 的在线图书商城 |
| 技术栈 | SpringBoot 3.2 + MyBatis-Plus + Spring Security + Redis + RabbitMQ + MySQL 8 + Vue 3（可选） |
| 核心域 | 商品中心、购物车、订单中心、库存、支付、物流 |
| 业务难点 | 超卖防控、分布式锁、订单状态机、事务一致性、消息可靠投递 |
| 简历定位 | 第二/第三个项目位，用于深挖 **并发、分布式、消息队列、Redis** |

### 简历话术模板（可直接复制）

```text
项目名称：BookMall 在线图书商城                           2024.xx — 2024.xx
技术栈：SpringBoot 3.2 / MyBatis-Plus / Redis / RabbitMQ / MySQL / Spring Security

· 设计并实现包含商品中心、购物车、订单、库存、物流五大核心域的 B2C 电商系统；
· 基于 Redis Hash 重构购物车存储，读写性能较 MySQL 方案提升 5 倍；
· 采用 "MySQL 行级锁 + Redis 预扣库存 + Lua 原子脚本" 三层防控解决超卖问题；
· 设计订单状态机（待支付→已支付→待发货→已发货→已完成/已取消），结合 RabbitMQ 死信队列实现 30 分钟未支付自动取消；
· 通过 @Transactional + 分布式 ID（雪花算法）+ 幂等 Token 保障下单接口的事务性与幂等性；
· 集成支付沙箱（沙箱版支付宝/模拟支付）+ 物流轨迹模拟，完整跑通电商交易闭环。
```

---

## 二、技术栈选型

```xml
<dependencies>
    <!-- 复用博客系统的 SpringBoot/MyBatis-Plus/Security/Redis/JWT 依赖 -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-amqp</artifactId></dependency>
    <dependency><groupId>org.redisson</groupId><artifactId>redisson-spring-boot-starter</artifactId><version>3.27.2</version></dependency>
    <dependency><groupId>com.alipay.sdk</groupId><artifactId>alipay-sdk-java</artifactId><version>4.38.0.ALL</version></dependency>
    <dependency><groupId>com.github.binarywang</groupId><artifactId>weixin-java-pay</artifactId><version>4.6.0</version></dependency>
</dependencies>
```

> 学生场景没有真实支付资质，**默认使用「模拟支付 + 支付宝沙箱」**，文档第十二节给出沙箱配置流程。

---

## 三、领域模型与表设计（MySQL 8.0）

```sql
CREATE DATABASE bookmall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bookmall;

-- ============ 商品域 ============
CREATE TABLE book (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    isbn          VARCHAR(20)  UNIQUE,
    title         VARCHAR(200) NOT NULL,
    author        VARCHAR(100),
    publisher     VARCHAR(100),
    category_id   BIGINT,
    cover         VARCHAR(255),
    description   TEXT,
    price         DECIMAL(10,2) NOT NULL,
    cost_price    DECIMAL(10,2),
    stock         INT          NOT NULL DEFAULT 0,
    sales_count   INT          DEFAULT 0,
    status        TINYINT      DEFAULT 1 COMMENT '1上架 0下架',
    version       INT          DEFAULT 0 COMMENT '乐观锁',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT      DEFAULT 0,
    INDEX idx_category (category_id),
    INDEX idx_title (title)
);

CREATE TABLE book_category (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    parent_id   BIGINT       DEFAULT 0,
    sort        INT          DEFAULT 0
);

-- ============ 用户/地址域 ============
CREATE TABLE user_address (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    receiver    VARCHAR(50)  NOT NULL,
    phone       VARCHAR(20)  NOT NULL,
    province    VARCHAR(50),
    city        VARCHAR(50),
    district    VARCHAR(50),
    detail      VARCHAR(255),
    is_default  TINYINT      DEFAULT 0,
    INDEX idx_user (user_id)
);

-- ============ 购物车（兜底，主存 Redis） ============
CREATE TABLE cart_item (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    book_id     BIGINT       NOT NULL,
    quantity    INT          NOT NULL,
    selected    TINYINT      DEFAULT 1,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_book (user_id, book_id)
);

-- ============ 订单域 ============
CREATE TABLE `order` (
    id              BIGINT       PRIMARY KEY COMMENT '雪花ID',
    order_no        VARCHAR(32)  NOT NULL UNIQUE COMMENT '订单号',
    user_id         BIGINT       NOT NULL,
    total_amount    DECIMAL(10,2) NOT NULL,
    pay_amount      DECIMAL(10,2) NOT NULL,
    freight         DECIMAL(10,2) DEFAULT 0,
    discount        DECIMAL(10,2) DEFAULT 0,
    pay_type        TINYINT      COMMENT '1支付宝 2微信 3模拟',
    status          TINYINT      NOT NULL COMMENT '0待支付 1已支付 2待发货 3已发货 4已完成 5已取消 6退款中 7已退款',
    receiver_info   VARCHAR(500) NOT NULL COMMENT '冗余收货信息JSON',
    remark          VARCHAR(255),
    pay_time        DATETIME,
    deliver_time    DATETIME,
    finish_time     DATETIME,
    cancel_time     DATETIME,
    cancel_reason   VARCHAR(100),
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      DEFAULT 0,
    INDEX idx_user_status (user_id, status),
    INDEX idx_create (create_time)
);

CREATE TABLE order_item (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    order_id    BIGINT       NOT NULL,
    book_id     BIGINT       NOT NULL,
    book_title  VARCHAR(200) NOT NULL COMMENT '冗余',
    book_cover  VARCHAR(255),
    price       DECIMAL(10,2) NOT NULL COMMENT '下单时单价',
    quantity    INT          NOT NULL,
    INDEX idx_order (order_id)
);

-- ============ 物流域 ============
CREATE TABLE logistics (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    order_id        BIGINT       NOT NULL UNIQUE,
    tracking_no     VARCHAR(50),
    company         VARCHAR(50)  COMMENT '顺丰/中通/圆通',
    current_status  VARCHAR(50)  COMMENT '已揽收/运输中/派送中/已签收',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE logistics_track (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    logistics_id BIGINT      NOT NULL,
    node        VARCHAR(100) NOT NULL COMMENT '节点描述',
    location    VARCHAR(100),
    track_time  DATETIME     NOT NULL,
    INDEX idx_logistics (logistics_id)
);

-- ============ 支付域 ============
CREATE TABLE payment (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(32)  NOT NULL,
    trade_no        VARCHAR(64)  COMMENT '第三方流水号',
    pay_type        TINYINT,
    amount          DECIMAL(10,2) NOT NULL,
    status          TINYINT      COMMENT '0待支付 1成功 2失败',
    pay_time        DATETIME,
    callback_data   TEXT         COMMENT '回调原文',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_no (order_no)
);

-- ============ 库存流水（关键，保障对账） ============
CREATE TABLE stock_log (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    book_id     BIGINT       NOT NULL,
    order_no    VARCHAR(32),
    type        TINYINT      COMMENT '1扣减 2回滚 3入库',
    quantity    INT          NOT NULL,
    before_stock INT,
    after_stock  INT,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_book (book_id)
);
```

---

## 四、订单状态机（核心面试点）

```text
        ┌──────────────┐  支付成功    ┌──────────────┐
        │  待支付 (0)   │ ──────────> │  已支付 (1)   │
        └──────┬───────┘              └──────┬───────┘
               │ 30分钟未支付/手动取消             │ 商家发货
               v                               v
        ┌──────────────┐              ┌──────────────┐
        │  已取消 (5)   │              │  已发货 (3)   │
        └──────────────┘              └──────┬───────┘
                                             │ 用户确认收货 / 7天自动确认
                                             v
                                      ┌──────────────┐
                                      │  已完成 (4)   │
                                      └──────────────┘
```

```java
@Getter
@AllArgsConstructor
public enum OrderStatus {
    PENDING_PAY(0, "待支付"),
    PAID(1, "已支付"),
    DELIVERING(2, "待发货"),
    DELIVERED(3, "已发货"),
    FINISHED(4, "已完成"),
    CANCELLED(5, "已取消"),
    REFUNDING(6, "退款中"),
    REFUNDED(7, "已退款");

    private final Integer code;
    private final String desc;

    public static OrderStatus of(Integer code) {
        return Arrays.stream(values()).filter(s -> s.code.equals(code)).findFirst().orElseThrow();
    }
}
```

```java
@Component
public class OrderStateMachine {

    /** 状态流转白名单：from -> [allowedTo...] */
    private static final Map<OrderStatus, Set<OrderStatus>> RULES = Map.of(
        PENDING_PAY, Set.of(PAID, CANCELLED),
        PAID,        Set.of(DELIVERING, REFUNDING),
        DELIVERING,  Set.of(DELIVERED),
        DELIVERED,   Set.of(FINISHED, REFUNDING),
        REFUNDING,   Set.of(REFUNDED, PAID)
    );

    public void check(OrderStatus from, OrderStatus to) {
        if (!RULES.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException(400, String.format("非法状态流转: %s -> %s", from.getDesc(), to.getDesc()));
        }
    }
}
```

---

## 五、Redis 购物车（主存）

```text
Key: cart:{userId}    Type: Hash
Field: bookId         Value: { "quantity": 2, "selected": 1, "addTime": 1714000000 }
TTL: 30天
```

```java
@Service
@RequiredArgsConstructor
public class CartService {

    private final StringRedisTemplate redis;
    private final BookMapper bookMapper;
    private final ObjectMapper objectMapper;

    private String key(Long userId) { return "cart:" + userId; }

    @SneakyThrows
    public void addItem(Long userId, Long bookId, Integer quantity) {
        Book b = Optional.ofNullable(bookMapper.selectById(bookId))
                .orElseThrow(() -> new BusinessException(2001, "图书不存在"));
        if (b.getStock() < quantity) throw new BusinessException(2002, "库存不足");

        HashOperations<String, String, String> ops = redis.opsForHash();
        String existed = ops.get(key(userId), bookId.toString());
        CartItemVO item = existed == null
                ? new CartItemVO(bookId, quantity, 1, System.currentTimeMillis())
                : objectMapper.readValue(existed, CartItemVO.class);
        if (existed != null) item.setQuantity(item.getQuantity() + quantity);

        ops.put(key(userId), bookId.toString(), objectMapper.writeValueAsString(item));
        redis.expire(key(userId), 30, TimeUnit.DAYS);
    }

    @SneakyThrows
    public List<CartItemDetailVO> list(Long userId) {
        Map<String, String> map = redis.<String, String>opsForHash().entries(key(userId));
        if (CollUtil.isEmpty(map)) return List.of();
        List<Long> bookIds = map.keySet().stream().map(Long::valueOf).toList();
        Map<Long, Book> books = bookMapper.selectBatchIds(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));
        List<CartItemDetailVO> result = new ArrayList<>();
        for (Map.Entry<String, String> e : map.entrySet()) {
            CartItemVO i = objectMapper.readValue(e.getValue(), CartItemVO.class);
            Book b = books.get(i.getBookId());
            if (b == null) continue;
            result.add(CartItemDetailVO.of(i, b));
        }
        return result;
    }

    public void update(Long userId, Long bookId, Integer quantity) { /* 同 addItem 逻辑 */ }

    public void remove(Long userId, Long bookId) {
        redis.opsForHash().delete(key(userId), bookId.toString());
    }

    public void clear(Long userId, Collection<Long> bookIds) {
        redis.opsForHash().delete(key(userId), bookIds.stream().map(String::valueOf).toArray());
    }
}
```

---

## 六、下单核心：事务 + 防超卖 + 幂等

### 6.1 三层防超卖策略

```text
请求 ──> [幂等 Token 校验] ──> [Redis Lua 原子预扣] ──> [DB 行锁/乐观锁] ──> 写订单
        ↑                     ↑                       ↑
    防止重复下单           防止瞬时超卖             兜底强一致
```

### 6.2 Redis Lua 预扣脚本

```lua
-- stock_decr.lua  KEYS[1]=stock:{bookId}  ARGV[1]=扣减数量
local stock = tonumber(redis.call('GET', KEYS[1]))
if stock == nil then return -1 end
local need = tonumber(ARGV[1])
if stock < need then return 0 end
return redis.call('DECRBY', KEYS[1], need)
```

```java
@Component
@RequiredArgsConstructor
public class StockRedisService {

    private final StringRedisTemplate redis;

    private static final DefaultRedisScript<Long> DECR_SCRIPT;
    static {
        DECR_SCRIPT = new DefaultRedisScript<>();
        DECR_SCRIPT.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/stock_decr.lua")));
        DECR_SCRIPT.setResultType(Long.class);
    }

    public boolean tryDecr(Long bookId, Integer quantity) {
        Long r = redis.execute(DECR_SCRIPT,
                List.of("stock:" + bookId), quantity.toString());
        if (r == null || r == -1) throw new BusinessException(2001, "库存未初始化");
        return r >= 0;
    }

    public void rollback(Long bookId, Integer quantity) {
        redis.opsForValue().increment("stock:" + bookId, quantity);
    }
}
```

### 6.3 下单 Service

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final BookMapper bookMapper;
    private final OrderItemMapper orderItemMapper;
    private final StockLogMapper stockLogMapper;
    private final StockRedisService stockRedis;
    private final CartService cartService;
    private final RabbitTemplate rabbit;
    private final IdGenerator idGen;            // 雪花算法
    private final RedissonClient redisson;
    private final StringRedisTemplate redisTemplate;
    private final OrderStateMachine stateMachine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO create(CreateOrderDTO dto, Long userId) {
        // 1. 幂等 Token
        String tokenKey = "order:token:" + dto.getIdempotentToken();
        Boolean ok = redisTemplate.delete(tokenKey);
        if (Boolean.FALSE.equals(ok)) throw new BusinessException(400, "请勿重复提交");

        // 2. 校验商品 + 计算金额（一次性查全）
        List<Long> bookIds = dto.getItems().stream().map(CreateOrderDTO.Item::getBookId).toList();
        Map<Long, Book> bookMap = bookMapper.selectBatchIds(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));
        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderDTO.Item it : dto.getItems()) {
            Book b = Optional.ofNullable(bookMap.get(it.getBookId()))
                    .orElseThrow(() -> new BusinessException(2001, "图书不存在"));
            if (b.getStatus() != 1) throw new BusinessException(2003, "图书已下架");
            total = total.add(b.getPrice().multiply(BigDecimal.valueOf(it.getQuantity())));
        }

        // 3. Redis 预扣
        List<Long> deductedBooks = new ArrayList<>();
        try {
            for (CreateOrderDTO.Item it : dto.getItems()) {
                if (!stockRedis.tryDecr(it.getBookId(), it.getQuantity())) {
                    throw new BusinessException(2002, "库存不足: " + bookMap.get(it.getBookId()).getTitle());
                }
                deductedBooks.add(it.getBookId());
            }

            // 4. DB 行锁扣减（兜底）
            for (CreateOrderDTO.Item it : dto.getItems()) {
                int rows = bookMapper.deductStock(it.getBookId(), it.getQuantity());
                if (rows == 0) throw new BusinessException(2002, "库存扣减失败，请重试");
                stockLogMapper.insertLog(it.getBookId(), it.getQuantity(), 1);
            }

            // 5. 写订单 + 订单项
            long orderId = idGen.nextId();
            String orderNo = "BM" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(4);
            Order order = Order.builder()
                    .id(orderId).orderNo(orderNo).userId(userId)
                    .totalAmount(total).payAmount(total)
                    .status(OrderStatus.PENDING_PAY.getCode())
                    .receiverInfo(JSONUtil.toJsonStr(dto.getAddress()))
                    .build();
            baseMapper.insert(order);

            List<OrderItem> items = dto.getItems().stream().map(it -> {
                Book b = bookMap.get(it.getBookId());
                return OrderItem.builder()
                        .orderId(orderId).bookId(b.getId())
                        .bookTitle(b.getTitle()).bookCover(b.getCover())
                        .price(b.getPrice()).quantity(it.getQuantity())
                        .build();
            }).toList();
            orderItemMapper.insertBatch(items);

            // 6. 清购物车
            cartService.clear(userId, bookIds);

            // 7. 发延迟消息：30 分钟未支付自动取消
            rabbit.convertAndSend("order.delay.exchange", "order.delay",
                    orderNo, msg -> {
                        msg.getMessageProperties().setHeader("x-delay", 30 * 60 * 1000);
                        return msg;
                    });

            return OrderVO.from(order, items);

        } catch (Exception e) {
            // 8. Redis 回滚（DB 由 @Transactional 回滚）
            for (int i = 0; i < deductedBooks.size(); i++) {
                CreateOrderDTO.Item it = dto.getItems().get(i);
                stockRedis.rollback(deductedBooks.get(i), it.getQuantity());
            }
            throw e;
        }
    }
}
```

### 6.4 库存 Mapper（行锁 + 乐观锁两种写法，二选一）

```java
public interface BookMapper extends BaseMapper<Book> {

    /** 方案A：UPDATE 自带行锁，原子扣减（推荐） */
    @Update("UPDATE book SET stock = stock - #{qty}, sales_count = sales_count + #{qty} " +
            "WHERE id = #{id} AND stock >= #{qty} AND deleted = 0")
    int deductStock(@Param("id") Long id, @Param("qty") Integer qty);

    /** 方案B：乐观锁（适合读多写少） */
    @Update("UPDATE book SET stock = stock - #{qty}, version = version + 1 " +
            "WHERE id = #{id} AND version = #{version} AND stock >= #{qty}")
    int deductStockWithVersion(@Param("id") Long id, @Param("qty") Integer qty, @Param("version") Integer version);
}
```

### 6.5 幂等 Token 接口

```java
@GetMapping("/order/token")
public Result<String> token(@AuthenticationPrincipal String username) {
    String token = IdUtil.fastSimpleUUID();
    redisTemplate.opsForValue().set("order:token:" + token, "1", 5, TimeUnit.MINUTES);
    return Result.success(token);
}
```

---

## 七、RabbitMQ 延迟队列（30 分钟未支付自动取消）

> 使用 **rabbitmq-delayed-message-exchange** 插件，比"死信队列+TTL"更优雅。

```java
@Configuration
public class RabbitConfig {

    public static final String DELAY_EXCHANGE = "order.delay.exchange";
    public static final String DELAY_QUEUE    = "order.delay.queue";
    public static final String DELAY_KEY      = "order.delay";

    @Bean
    public CustomExchange delayExchange() {
        return new CustomExchange(DELAY_EXCHANGE, "x-delayed-message", true, false,
                Map.of("x-delayed-type", "direct"));
    }

    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE).build();
    }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(DELAY_KEY).noargs();
    }
}
```

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutListener {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitConfig.DELAY_QUEUE)
    public void handle(String orderNo, Channel channel, Message msg) throws IOException {
        long deliveryTag = msg.getMessageProperties().getDeliveryTag();
        try {
            orderService.cancelIfUnpaid(orderNo, "支付超时自动取消");
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("订单超时处理失败 orderNo={}", orderNo, e);
            channel.basicNack(deliveryTag, false, false); // 进死信
        }
    }
}
```

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void cancelIfUnpaid(String orderNo, String reason) {
    Order order = baseMapper.selectOne(Wrappers.lambdaQuery(Order.class).eq(Order::getOrderNo, orderNo));
    if (order == null || !OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) return;

    stateMachine.check(OrderStatus.of(order.getStatus()), OrderStatus.CANCELLED);
    order.setStatus(OrderStatus.CANCELLED.getCode());
    order.setCancelReason(reason);
    order.setCancelTime(LocalDateTime.now());
    baseMapper.updateById(order);

    // 回滚库存（DB + Redis）
    List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
    for (OrderItem it : items) {
        bookMapper.addStock(it.getBookId(), it.getQuantity());
        stockRedis.rollback(it.getBookId(), it.getQuantity());
        stockLogMapper.insertLog(it.getBookId(), it.getQuantity(), 2);
    }
}
```

---

## 八、支付集成（沙箱 / 模拟）

### 8.1 支付宝沙箱接入流程

```text
1. 登录开放平台 -> 沙箱环境 -> 拿 APP_ID、应用私钥、支付宝公钥
2. application.yml 填入配置
3. 调用 alipay.trade.page.pay 生成支付页面
4. 配置异步通知 URL（内网穿透：natapp/cpolar）
5. 回调中验签 -> 改订单状态 -> 发货
```

```yaml
alipay:
  app-id: 2021000000000000
  app-private-key: "MIIE..."
  alipay-public-key: "MIIB..."
  gateway-url: https://openapi-sandbox.dl.alipaydev.com/gateway.do
  notify-url: http://your-natapp.cc/api/pay/alipay/notify
  return-url: http://localhost:8080/order/result
```

### 8.2 模拟支付接口（学习/演示用）

```java
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {

    private final OrderService orderService;

    /** 模拟支付：直接调用即成功 */
    @PostMapping("/mock/{orderNo}")
    public Result<Void> mockPay(@PathVariable String orderNo) {
        orderService.paySuccess(orderNo, "MOCK_" + System.currentTimeMillis(), 3);
        return Result.success();
    }

    /** 支付宝异步通知 */
    @PostMapping("/alipay/notify")
    public String alipayNotify(HttpServletRequest req) {
        Map<String, String> params = parseRequest(req);
        boolean ok = AlipaySignature.rsaCheckV1(params,
                alipayPublicKey, "UTF-8", "RSA2");
        if (!ok) return "fail";
        if ("TRADE_SUCCESS".equals(params.get("trade_status"))) {
            orderService.paySuccess(params.get("out_trade_no"), params.get("trade_no"), 1);
        }
        return "success";
    }
}
```

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void paySuccess(String orderNo, String tradeNo, Integer payType) {
    String lockKey = "lock:pay:" + orderNo;
    RLock lock = redisson.getLock(lockKey);
    try {
        if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) throw new BusinessException(500, "支付处理中");
        Order order = baseMapper.selectOne(Wrappers.lambdaQuery(Order.class).eq(Order::getOrderNo, orderNo));
        if (order == null) throw new BusinessException(404, "订单不存在");
        if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) return; // 幂等

        stateMachine.check(OrderStatus.PENDING_PAY, OrderStatus.PAID);
        order.setStatus(OrderStatus.PAID.getCode());
        order.setPayType(payType);
        order.setPayTime(LocalDateTime.now());
        baseMapper.updateById(order);

        // 写支付流水
        paymentMapper.insert(Payment.builder()
                .orderNo(orderNo).tradeNo(tradeNo).payType(payType)
                .amount(order.getPayAmount()).status(1)
                .payTime(LocalDateTime.now()).build());

        // 发消息：通知发货中心
        rabbit.convertAndSend("order.event.exchange", "order.paid", orderNo);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new BusinessException(500, "支付处理中断");
    } finally {
        if (lock.isHeldByCurrentThread()) lock.unlock();
    }
}
```

---

## 九、物流模拟（定时任务推进状态）

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class LogisticsScheduler {

    private final LogisticsMapper logisticsMapper;
    private final LogisticsTrackMapper trackMapper;
    private final OrderService orderService;

    private static final List<String> NODES = List.of(
            "已揽收", "运输中（合肥分拨中心）", "派送中", "已签收"
    );

    /** 每分钟推进所有进行中物流到下一节点（演示用，生产改为接物流API） */
    @Scheduled(cron = "0 * * * * ?")
    public void advance() {
        List<Logistics> list = logisticsMapper.selectList(
                Wrappers.lambdaQuery(Logistics.class).ne(Logistics::getCurrentStatus, "已签收"));
        for (Logistics l : list) {
            int idx = NODES.indexOf(l.getCurrentStatus());
            int next = Math.min(idx + 1, NODES.size() - 1);
            String nextNode = NODES.get(next);

            l.setCurrentStatus(nextNode);
            logisticsMapper.updateById(l);
            trackMapper.insert(LogisticsTrack.builder()
                    .logisticsId(l.getId()).node(nextNode)
                    .location("合肥市").trackTime(LocalDateTime.now()).build());

            if ("已签收".equals(nextNode)) {
                orderService.markDelivered(l.getOrderId());
            }
        }
    }
}
```

```java
// 商家发货
@Override
@Transactional(rollbackFor = Exception.class)
public void deliver(Long orderId, String company) {
    Order order = baseMapper.selectById(orderId);
    stateMachine.check(OrderStatus.PAID, OrderStatus.DELIVERED);
    order.setStatus(OrderStatus.DELIVERED.getCode());
    order.setDeliverTime(LocalDateTime.now());
    baseMapper.updateById(order);

    Logistics l = Logistics.builder()
            .orderId(orderId).company(company)
            .trackingNo("SF" + RandomUtil.randomNumbers(12))
            .currentStatus("已揽收").build();
    logisticsMapper.insert(l);
    trackMapper.insert(LogisticsTrack.builder()
            .logisticsId(l.getId()).node("已揽收")
            .location("合肥市").trackTime(LocalDateTime.now()).build());
}
```

---

## 十、雪花 ID 生成器

```java
@Component
public class IdGenerator {
    private final Snowflake snowflake = IdUtil.getSnowflake(1, 1);
    public long nextId() { return snowflake.nextId(); }
}
```

---

## 十一、项目结构

```text
bookmall/
├── pom.xml
├── src/main/java/com/bookmall/
│   ├── BookMallApplication.java
│   ├── common/                  # Result/异常/分页（同博客系统）
│   ├── config/
│   │   ├── RabbitConfig.java
│   │   ├── RedissonConfig.java
│   │   ├── SecurityConfig.java
│   │   ├── MybatisPlusConfig.java
│   │   └── AlipayConfig.java
│   ├── domain/
│   │   ├── book/                # 商品域
│   │   ├── cart/                # 购物车
│   │   ├── order/
│   │   │   ├── entity/
│   │   │   ├── enums/OrderStatus.java
│   │   │   ├── statemachine/OrderStateMachine.java
│   │   │   ├── service/
│   │   │   └── controller/
│   │   ├── pay/
│   │   ├── logistics/
│   │   └── stock/
│   ├── mq/
│   │   ├── OrderTimeoutListener.java
│   │   └── OrderPaidListener.java
│   ├── scheduler/
│   │   └── LogisticsScheduler.java
│   ├── infra/
│   │   ├── id/IdGenerator.java
│   │   ├── lock/                # Redisson 封装
│   │   └── redis/StockRedisService.java
│   └── security/                # 同博客系统
└── src/main/resources/
    ├── application.yml
    ├── lua/stock_decr.lua
    └── mapper/
```

---

## 十二、运行与部署

```bash
# 1. 启动依赖
docker run -d --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root mysql:8.0
docker run -d --name redis -p 6379:6379 redis:7-alpine
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management

# 2. 安装 RabbitMQ 延迟插件
docker exec rabbitmq rabbitmq-plugins enable rabbitmq_delayed_message_exchange

# 3. 导入 SQL + 初始化 Redis 库存
mysql -uroot -proot bookmall < schema.sql
# Redis CLI: SET stock:1 100   （把每本书的库存写入 Redis）

# 4. 启动项目
mvn spring-boot:run
```

| 入口 | 地址 |
| --- | --- |
| 前台 | http://localhost:8080 |
| 后台 | http://localhost:8080/admin |
| RabbitMQ 控制台 | http://localhost:15672 (guest/guest) |
| Knife4j | http://localhost:8080/doc.html |

---

## 十三、面试高频问题预演

| # | 题目 | 简答提纲 |
| --- | --- | --- |
| 1 | 如何防止超卖？ | 三层：幂等Token + Redis Lua预扣 + DB UPDATE 行锁 `WHERE stock >= qty` |
| 2 | Redis 与 MySQL 数据如何对齐？ | 启动时全量加载；下单失败必回滚；定时对账任务（凌晨）+ 库存流水表兜底 |
| 3 | 订单超时自动取消怎么实现？ | RabbitMQ 延迟插件 / DelayQueue / Redisson DelayedQueue / 定时扫表 — 各方案对比 |
| 4 | 支付回调如何保证幂等？ | Redisson 分布式锁 + 状态机校验，已支付状态直接 return |
| 5 | 分布式锁为什么用 Redisson 不用 SETNX？ | 看门狗续期、可重入、释放安全（Lua 脚本判 owner） |
| 6 | 雪花算法为什么用？时钟回拨怎么办？ | 高并发分布式 ID；时钟回拨拒绝生成 / 等待追平 / 备用 workerId |
| 7 | 订单状态机的好处？ | 集中校验非法跃迁，避免散落在各处的 if-else，便于审计与扩展 |
| 8 | RabbitMQ 消息丢失/重复怎么处理？ | 生产端 confirm + return；消费端 manual ack；幂等用 messageId + Redis SETNX |
| 9 | 你的事务边界在哪？ | 下单方法 `@Transactional`；MQ 发送放在事务内（异常回滚消息）或用本地消息表 |
| 10 | 长事务问题？ | 拆分 + 异步化：DB 落库走事务，发货/通知通过 MQ 解耦 |

---

## 十四、可扩展方向

| 方向 | 简历加分点 |
| --- | --- |
| ELK 日志收集 | "全链路日志、TraceId 串联订单链路" |
| Sentinel 限流降级 | "下单接口 QPS 阈值保护，秒杀场景预案" |
| 分库分表（ShardingSphere） | "按 user_id 哈希分库、订单按月分表" |
| Canal 同步 ES | "亿级订单秒级搜索" |
| Seata 分布式事务 | 拆服务后引入，AT 模式 |
| 优惠券/积分/秒杀 | 直接对标真实电商 |

---

## 十五、4 周开发节奏

| 周次 | 目标 |
| --- | --- |
| W1 | 商品 CRUD + 分类、用户登录复用博客模块、Redis 库存初始化 |
| W2 | 购物车（Redis）+ 地址管理 + 下单接口（含三层防超卖） |
| W3 | 支付沙箱接入 + RabbitMQ 延迟取消 + 订单状态机 + 状态查询 |
| W4 | 物流模拟 + 后台管理（订单列表/发货）+ 对账任务 + README + Docker Compose |

---

> 文档落地建议：先把 **库存扣减链路（接口 → Lua → DB → 流水表）** 跑通并写一份压测报告（jmeter 1000 并发），这是简历最硬的素材。其次是 **延迟队列 + 状态机** 这条线，可以在面试里讲 30 分钟。其余功能按 W1—W4 节奏推进即可。
