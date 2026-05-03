# 中间件实战项目合集（Spring Boot + Redis + RabbitMQ + MySQL）

本文档给出 4 个可直接落地到简历的企业级中间件项目方案，统一采用 `Spring Boot 3 + MyBatis Plus/JPA + MySQL 8 + Redis 7 + RabbitMQ 3` 技术栈，重点覆盖高并发、缓存、异步解耦、数据库优化、分布式会话等核心能力。Redis Lua 脚本具备原子执行特性，适合库存扣减与并发控制场景；RabbitMQ 死信交换机可承接拒收、TTL 到期、队列超长等异常消息；MySQL 分区与索引优化适用于大表查询治理 [cite:1][cite:2][cite:8]。

## 项目总览

| 项目 | 核心问题 | 技术重点 | 简历价值 |
|---|---|---|---|
| Redis 秒杀系统 | 高并发下超卖、重复下单、库存一致性 | Redis 预热、Lua 原子扣减、分布式锁、异步落库 | 体现高并发系统设计能力 |
| RabbitMQ 消息异步通知 | 同步链路过长、通知失败影响主流程 | 交换机/队列、死信队列、延迟重试、幂等消费 | 体现异步解耦与可靠消息能力 |
| MySQL 电商订单系统 | 大表慢查询、事务冲突、热点数据 | 索引优化、事务隔离、分表、慢 SQL 治理 | 体现数据库内功与性能优化能力 |
| Redis 分布式 Session 共享 | 集群登录状态不一致、缓存异常流量 | Session 共享、TTL、布隆过滤、随机过期、降级兜底 | 体现微服务登录与缓存治理能力 |

---

## 1. Redis 秒杀系统

### 1.1 项目目标

构建一个支持高并发秒杀的活动下单系统，要求在瞬时流量冲击下仍能做到：

- 不超卖。
- 不重复下单。
- 请求尽量不直打 MySQL。
- 下单主链路足够短。

### 1.2 业务场景

用户在活动开始后抢购限量商品，系统需要在毫秒级完成资格校验、库存扣减与下单请求受理。Lua 脚本在 Redis 服务端执行且具备原子性，适合把“校验是否重复下单 + 判断库存 + 扣减库存”合并为一次操作，减少网络往返并降低并发竞争 [cite:1]。

### 1.3 架构设计

```text
Nginx
  ↓
Spring Boot 秒杀服务
  ↓
Redis
  ├─ 商品缓存
  ├─ 库存 Key
  ├─ 用户下单标记
  └─ Lua 原子脚本
  ↓
RabbitMQ / Redis Stream（异步削峰，可二选一）
  ↓
订单服务
  ↓
MySQL
```

### 1.4 核心功能

- 缓存预热：活动开始前把商品信息、库存数量写入 Redis，减少数据库冷启动压力。
- 库存扣减：使用 Lua 脚本原子判断库存并扣减，避免 `get + decrement` 分步操作导致并发超卖 [cite:1]。
- 一人一单：使用 `userId + voucherId` 作为 Redis 标记，抢购成功后立刻写入。
- 异步下单：请求通过后只返回“抢购成功，排队中”，订单写库交给 MQ 异步处理。
- 最终一致性：消费者落库失败时进行补偿或人工兜底。

### 1.5 表设计

#### `tb_seckill_voucher`

```sql
CREATE TABLE tb_seckill_voucher (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    voucher_id BIGINT NOT NULL,
    stock INT NOT NULL,
    begin_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_voucher_time (voucher_id, begin_time, end_time)
);
```

#### `tb_voucher_order`

```sql
CREATE TABLE tb_voucher_order (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    voucher_id BIGINT NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_voucher (user_id, voucher_id),
    KEY idx_voucher (voucher_id),
    KEY idx_create_time (create_time)
);
```

### 1.6 Redis Key 设计

```text
seckill:stock:{voucherId}         -> 商品库存
seckill:order:{voucherId}:{userId} -> 用户是否已下单
seckill:goods:{voucherId}          -> 活动商品详情
lock:order:{userId}                -> 用户维度分布式锁
```

### 1.7 Lua 脚本

Redis 官方文档说明 Lua 脚本在服务端原子执行，脚本运行期间其他服务端活动会被阻塞，因此非常适合把库存判断与扣减组合成一次不可分割的操作 [cite:1]。

```lua
local stockKey = KEYS[1]
local orderKey = KEYS[2]
local userId = ARGV[1]

local stock = tonumber(redis.call('GET', stockKey))
if not stock or stock <= 0 then
    return 1
end

if redis.call('SISMEMBER', orderKey, userId) == 1 then
    return 2
end

redis.call('DECR', stockKey)
redis.call('SADD', orderKey, userId)
return 0
```

返回值约定：

- `0`：成功。
- `1`：库存不足。
- `2`：重复下单。

### 1.8 核心代码骨架

#### Controller

```java
@RestController
@RequestMapping("/seckill")
public class SeckillController {

    @Resource
    private SeckillService seckillService;

    @PostMapping("/{voucherId}")
    public Result seckill(@PathVariable Long voucherId) {
        Long userId = UserHolder.getUserId();
        return seckillService.doSeckill(voucherId, userId);
    }
}
```

#### Service

```java
@Service
public class SeckillService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RabbitTemplate rabbitTemplate;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT = new DefaultRedisScript<>();

    static {
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    public Result doSeckill(Long voucherId, Long userId) {
        Long res = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Arrays.asList(
                        "seckill:stock:" + voucherId,
                        "seckill:order:" + voucherId
                ),
                userId.toString()
        );
        if (res == null) {
            return Result.fail("系统异常");
        }
        if (res == 1L) {
            return Result.fail("库存不足");
        }
        if (res == 2L) {
            return Result.fail("不能重复下单");
        }
        SeckillOrderMessage msg = new SeckillOrderMessage(IdWorker.nextId(), userId, voucherId);
        rabbitTemplate.convertAndSend("seckill.order.exchange", "seckill.order", msg);
        return Result.ok("抢购成功，订单处理中");
    }
}
```

#### MQ 消费落库

```java
@Component
public class SeckillOrderConsumer {

    @Resource
    private VoucherOrderService voucherOrderService;

    @RabbitListener(queues = "seckill.order.queue")
    public void consume(SeckillOrderMessage message) {
        voucherOrderService.createVoucherOrder(message);
    }
}
```

### 1.9 关键优化点

- 先查 Redis，后异步落库，避免瞬时流量压垮 MySQL。
- 唯一索引 `uk_user_voucher` 作为数据库最后一道幂等防线。
- 库存以 Redis 为前置扣减层，数据库只做最终订单持久化。
- 秒杀开始前进行缓存预热，避免活动开始瞬间缓存未命中。
- 可增加令牌桶/滑动窗口限流，进一步保护后端服务。

### 1.10 面试可讲亮点

- 为什么单纯 `synchronized` 不够：单机锁无法覆盖分布式部署。
- 为什么优先 Lua 而不是 `setnx + decr`：Lua 可以把多步操作合并为服务端原子事务 [cite:1]。
- 为什么还要数据库唯一索引：Redis 属于前置拦截，最终一致性仍需数据库兜底。
- 为什么要异步下单：缩短 RT，削峰填谷，提升活动吞吐。

---

## 2. RabbitMQ 消息异步通知

### 2.1 项目目标

构建一个订单完成后的异步通知系统，实现邮件、短信、站内信三类通知解耦，并支持失败重试、死信队列、人工补偿。

### 2.2 业务场景

用户支付成功后，订单主流程不能被“发短信慢、邮件服务偶发失败”拖垮，因此需要把通知任务投递到 MQ，由独立消费者异步处理。RabbitMQ 死信交换机可接收被拒收、TTL 到期、队列超长等消息，适合实现延迟重试和失败兜底 [cite:2][cite:15]。

### 2.3 架构设计

```text
订单服务
  ↓ 发布订单支付成功事件
RabbitMQ Exchange
  ↓
通知队列
  ├─ 邮件消费者
  ├─ 短信消费者
  └─ 站内信消费者
       ↓
失败消息进入 DLX + DLQ
       ↓
重试消费者 / 人工补偿后台
```

### 2.4 核心功能

- 异步解耦：订单服务只负责发消息，不直接调用通知服务。
- 多渠道推送：邮件、短信、站内信可并行扩展。
- 死信队列：消费异常后进入死信交换机，便于重试与排查 [cite:2]。
- 延迟重试：失败消息设置 TTL，过期后重新路由到原业务队列 [cite:2][cite:11]。
- 幂等消费：基于消息 ID 或业务 ID 做去重。

### 2.5 消息结构

```json
{
  "messageId": "18999887766",
  "bizType": "ORDER_PAY_SUCCESS",
  "userId": 10001,
  "email": "test@qq.com",
  "phone": "13800000000",
  "orderNo": "ORD202605010001",
  "retryCount": 0,
  "sendTime": "2026-05-01 12:00:00"
}
```

### 2.6 RabbitMQ 设计

| 组件 | 名称 | 作用 |
|---|---|---|
| 主交换机 | `notify.direct.exchange` | 接收支付成功通知事件 |
| 主队列 | `notify.queue` | 正常通知消费 |
| 死信交换机 | `notify.dlx.exchange` | 接收失败或过期消息 |
| 死信队列 | `notify.dlq.queue` | 存储失败消息 |
| 重试队列 | `notify.retry.queue` | TTL 到期后回流主队列 |

RabbitMQ 官方文档建议优先通过 policy 配置 DLX，而不是把 `x-arguments` 硬编码在应用中，因为 policy 可以动态更新，无需重发版 [cite:2]。

### 2.7 配置代码

```java
@Configuration
public class RabbitConfig {

    @Bean
    public DirectExchange notifyExchange() {
        return new DirectExchange("notify.direct.exchange");
    }

    @Bean
    public DirectExchange notifyDlxExchange() {
        return new DirectExchange("notify.dlx.exchange");
    }

    @Bean
    public Queue notifyQueue() {
        return QueueBuilder.durable("notify.queue")
                .deadLetterExchange("notify.dlx.exchange")
                .deadLetterRoutingKey("notify.dlq")
                .build();
    }

    @Bean
    public Queue notifyDlqQueue() {
        return QueueBuilder.durable("notify.dlq.queue").build();
    }

    @Bean
    public Binding notifyBinding() {
        return BindingBuilder.bind(notifyQueue())
                .to(notifyExchange())
                .with("notify.send");
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(notifyDlqQueue())
                .to(notifyDlxExchange())
                .with("notify.dlq");
    }
}
```

### 2.8 发送消息

```java
@Service
public class NotifyProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void send(OrderNotifyMessage message) {
        rabbitTemplate.convertAndSend("notify.direct.exchange", "notify.send", message);
    }
}
```

### 2.9 消费与重试

Spring Cloud Stream 文档说明，消息被拒绝且不重新入队时，可以进入死信流程；另一种常见方案是给死信队列设置 TTL，到期后重新投递到原队列，实现延迟重试 [cite:11]。

```java
@Component
public class NotifyConsumer {

    @RabbitListener(queues = "notify.queue")
    public void consume(OrderNotifyMessage message) {
        boolean success = sendEmailOrSms(message);
        if (!success) {
            throw new AmqpRejectAndDontRequeueException("send failed");
        }
    }

    private boolean sendEmailOrSms(OrderNotifyMessage message) {
        return true;
    }
}
```

### 2.10 重试策略设计

- 第 1 次失败：进入 DLQ。
- DLQ 消费者检查 `retryCount`。
- 未超出阈值时投递到带 TTL 的重试队列。
- TTL 到期后自动死信回主队列，再次消费。
- 超出最大次数后写入 `notify_fail_record` 表，后台人工补偿。

RabbitMQ 文档指出，消息变成死信的典型原因包括消费者拒收且 `requeue=false`、消息 TTL 到期、队列长度超限等，这正好覆盖通知系统中的失败重试和异常兜底场景 [cite:2]。

### 2.11 幂等设计

#### `notify_log`

```sql
CREATE TABLE notify_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(64) NOT NULL,
    biz_type VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    error_msg VARCHAR(255) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_message_id (message_id)
);
```

消费时先根据 `messageId` 查重，已成功处理则直接 ack，避免重复推送。

### 2.12 面试可讲亮点

- 为什么订单成功后不能同步发短信：外部接口慢且不稳定，会拖垮主交易链路。
- 为什么要 DLQ：失败消息不能直接丢，需要可追踪、可重试、可补偿 [cite:2][cite:15]。
- 为什么消费端也要幂等：MQ 至少一次投递语义下，重复消费是常态。
- 为什么重试不能无限次：会形成消息堆积和无效资源消耗。

---

## 3. MySQL 电商订单系统

### 3.1 项目目标

实现一个电商订单系统，重点不在页面，而在数据库设计与性能治理，覆盖索引优化、事务隔离级别、分表、慢查询优化。

### 3.2 业务模块

- 商品模块：查询商品、库存、价格。
- 购物车模块：加购、修改数量、勾选下单。
- 订单模块：创建订单、取消订单、查询订单列表。
- 支付模块：支付成功回调更新状态。
- 后台模块：按时间与状态筛选订单。

### 3.3 核心表设计

#### `tb_order`

```sql
CREATE TABLE tb_order (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status TINYINT NOT NULL,
    pay_time DATETIME DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_status_ctime (user_id, status, create_time),
    KEY idx_ctime (create_time)
);
```

#### `tb_order_item`

```sql
CREATE TABLE tb_order_item (
    id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    num INT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_order_id (order_id),
    KEY idx_product_id (product_id)
);
```

### 3.4 索引优化思路

- 针对“用户订单列表”建立联合索引 `(user_id, status, create_time)`，匹配最左前缀原则。
- 针对“按订单号查询详情”建立唯一索引 `order_no`。
- 避免对索引列做函数、隐式类型转换、前导模糊查询。
- 高选择性字段优先放在联合索引前部。

### 3.5 事务隔离级别

MySQL 常见事务隔离级别包括 `READ UNCOMMITTED`、`READ COMMITTED`、`REPEATABLE READ`、`SERIALIZABLE`，电商订单系统通常在 InnoDB 默认的 `REPEATABLE READ` 基础上配合行锁与业务约束实现一致性与性能平衡。对于“扣库存 + 创建订单 + 扣余额”一类操作，应保证事务边界清晰，避免把远程调用塞进本地事务中造成长事务。

```sql
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
```

### 3.6 分表方案

当订单量达到千万级后，单表查询与维护成本会上升，此时可以考虑按时间或按用户维度分表。MySQL 分区/分片的价值在于缩小扫描范围，尤其是按时间查询订单时，合理的范围分区可以触发 partition pruning，仅访问相关分区 [cite:8]。

#### 按月分表思路

```text
tb_order_202601
tb_order_202602
tb_order_202603
```

#### 按用户哈希分表思路

```text
tb_order_00 ~ tb_order_15
user_id % 16
```

### 3.7 分区表示例

资料指出，MySQL 分区适合大表管理，但分区数量不宜过多，并且需要结合查询模式与索引一起设计，否则反而会拖慢性能 [cite:8]。

```sql
CREATE TABLE tb_order_partition (
    id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    status TINYINT NOT NULL,
    create_time DATETIME NOT NULL,
    PRIMARY KEY (id, create_time),
    KEY idx_user_status_ctime (user_id, status, create_time)
)
PARTITION BY RANGE (TO_DAYS(create_time)) (
    PARTITION p202601 VALUES LESS THAN (TO_DAYS('2026-02-01')),
    PARTITION p202602 VALUES LESS THAN (TO_DAYS('2026-03-01')),
    PARTITION p202603 VALUES LESS THAN (TO_DAYS('2026-04-01')),
    PARTITION pmax VALUES LESS THAN MAXVALUE
);
```

### 3.8 慢查询优化

#### 常见慢 SQL 问题

- 没有索引导致全表扫描。
- 联合索引建立了但查询条件没有命中最左前缀。
- `select *` 读取无用列，增加回表成本。
- 大分页 `limit 100000, 20` 导致扫描过深。
- 长事务持锁时间过久，引发阻塞。

#### 典型优化方式

1. 用 `EXPLAIN` 看执行计划，重点关注 `type`、`key`、`rows`、`extra`。
2. 对高频条件建立覆盖索引，减少回表。
3. 订单列表使用“基于上次最大 ID/时间”的游标分页代替深分页。
4. 读写分离仅解决读压力，不解决错误索引导致的慢 SQL。
5. 定期分析慢查询日志，持续治理热点 SQL。

### 3.9 下单事务示例

```java
@Service
public class OrderService {

    @Transactional
    public Long createOrder(CreateOrderCommand cmd) {
        checkStock(cmd.getProductId(), cmd.getNum());
        deductStock(cmd.getProductId(), cmd.getNum());
        Long orderId = saveOrder(cmd);
        saveOrderItems(orderId, cmd.getItems());
        return orderId;
    }
}
```

### 3.10 面试可讲亮点

- 为什么索引不是越多越好：索引会增加写入成本与空间占用。
- 为什么联合索引顺序重要：决定能否命中最左前缀。
- 为什么大表要考虑分区/分表：降低单次扫描与维护成本 [cite:8]。
- 为什么长事务危险：锁持有时间长，容易放大阻塞与死锁概率。

---

## 4. Redis 分布式 Session 共享

### 4.1 项目目标

实现多实例部署下的统一登录状态管理，让用户无论访问哪台应用节点，都能拿到一致的会话信息，并解决缓存击穿、穿透、雪崩问题。

### 4.2 业务痛点

传统单机 Session 存在以下问题：

- 用户第一次登录在 A 机器，第二次请求被负载均衡到 B 机器后 Session 丢失。
- 节点扩缩容后本地 Session 无法共享。
- 大量恶意或异常请求可能击穿数据库。

### 4.3 架构设计

```text
浏览器
  ↓
Nginx
  ↓
Spring Boot 集群
  ↓
Redis Cluster
  ├─ Spring Session
  ├─ 用户登录态缓存
  ├─ 热点数据缓存
  └─ 布隆过滤 / 空值缓存
```

### 4.4 核心功能

- 登录成功后，将 Session 存入 Redis。
- 任意应用节点都能从 Redis 获取用户状态。
- 设置合理 TTL，支持滑动续期。
- 使用布隆过滤器、空值缓存、随机过期时间治理缓存穿透与雪崩。

### 4.5 接入方案

推荐基于 Spring Session + Redis 实现，核心思想是把原本存在应用内存中的 Session 替换为 Redis 存储。Redis 脚本和服务端执行逻辑具备原子能力，适合在高并发缓存访问场景中封装复杂操作 [cite:1]。

### 4.6 配置示例

```yaml
spring:
  session:
    store-type: redis
  data:
    redis:
      host: 127.0.0.1
      port: 6379
server:
  servlet:
    session:
      timeout: 30m
```

```java
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class SessionConfig {
}
```

### 4.7 登录流程

1. 用户提交账号密码。
2. 服务端校验成功后创建 Session。
3. Session 数据写入 Redis，返回 Cookie 或 Token。
4. 后续请求到任意节点时，根据会话标识去 Redis 取用户信息。
5. 若开启滑动续期，则在活跃访问期间刷新 TTL。

### 4.8 缓存击穿、穿透、雪崩治理

#### 缓存穿透

请求的数据根本不存在，如果每次都打到数据库，会造成无效穿透。解决方式：

- 布隆过滤器拦截非法 key。
- 查询为空时缓存空对象，TTL 设短一点。

#### 缓存击穿

热点 key 刚好失效，大量请求同时访问数据库。解决方式：

- 热点数据永不过期，后台异步更新。
- 使用互斥锁或逻辑过期，保证只有一个线程重建缓存。

#### 缓存雪崩

大量 key 在同一时刻失效，数据库被集中打爆。解决方式：

- 给不同 key 加随机 TTL。
- 多级缓存与降级兜底。
- Redis 集群高可用部署。

### 4.9 用户信息缓存示例

```java
@Service
public class UserSessionService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void refreshToken(String token, String userJson) {
        String key = "login:token:" + token;
        stringRedisTemplate.opsForValue().set(key, userJson, 30, TimeUnit.MINUTES);
    }

    public String getUser(String token) {
        return stringRedisTemplate.opsForValue().get("login:token:" + token);
    }
}
```

### 4.10 面试可讲亮点

- 为什么分布式环境下本地 Session 不可靠：请求会被负载均衡到不同实例。
- 为什么登录态适合 Redis：访问快、天然支持 TTL、便于多实例共享。
- 为什么缓存治理必须一起做：Session 共享解决的是一致性，穿透/击穿/雪崩解决的是稳定性。
- 为什么要滑动续期：提升活跃用户体验，避免频繁掉线。

---

## 5. 项目统一目录结构

```text
middleware-projects/
├── seckill-demo/
│   ├── controller/
│   ├── service/
│   ├── mapper/
│   ├── entity/
│   ├── mq/
│   └── resources/
├── rabbitmq-notify-demo/
│   ├── config/
│   ├── producer/
│   ├── consumer/
│   ├── entity/
│   └── resources/
├── mysql-order-demo/
│   ├── controller/
│   ├── service/
│   ├── mapper/
│   ├── entity/
│   └── resources/
└── redis-session-demo/
    ├── config/
    ├── controller/
    ├── service/
    └── resources/
```

---

## 6. 技术栈建议

| 层级 | 选型 |
|---|---|
| 后端框架 | Spring Boot 3 |
| ORM | MyBatis Plus / JPA |
| 数据库 | MySQL 8 |
| 缓存 | Redis 7 |
| 消息队列 | RabbitMQ 3 |
| 权限认证 | Spring Security + JWT / Session |
| 构建工具 | Maven |
| 文档测试 | Apifox / Postman |
| 部署 | Docker + Docker Compose |

---

## 7. 简历写法模板

### Redis 秒杀系统

- 独立设计并实现高并发秒杀系统，基于 Redis 缓存预热与 Lua 原子脚本完成库存扣减和一人一单校验，解决并发超卖问题 [cite:1]。
- 引入 RabbitMQ 异步下单链路，实现请求削峰与订单最终一致性，提升活动高峰期系统吞吐。
- 通过 Redis + MySQL 双层幂等控制与唯一索引兜底，保障订单数据一致性。

### RabbitMQ 异步通知系统

- 基于 RabbitMQ 搭建订单支付成功异步通知系统，实现邮件、短信、站内信解耦投递与并发消费。
- 设计死信队列、TTL 延迟重试与失败补偿机制，提高消息可靠性与故障恢复能力 [cite:2][cite:11][cite:15]。
- 实现消息幂等消费与通知日志追踪，避免重复推送。

### MySQL 电商订单系统

- 设计电商订单核心表结构与联合索引方案，围绕订单列表、详情、支付回调等高频场景完成 SQL 优化。
- 基于事务隔离级别、慢查询日志与执行计划分析治理大表性能问题，并设计分表/分区扩展方案 [cite:8]。
- 优化深分页、覆盖索引与长事务问题，提升订单查询效率与系统稳定性。

### Redis 分布式 Session 共享

- 基于 Spring Session + Redis 实现集群环境下的登录态共享，解决多实例部署会话不一致问题。
- 针对缓存穿透、击穿、雪崩场景设计布隆过滤、空值缓存、随机 TTL 与热点互斥重建方案。
- 通过滑动续期与统一会话管理机制提升系统可用性与用户体验。

---

## 8. 项目落地顺序

建议按以下顺序完成，便于你从单点能力逐步扩展到企业级综合能力：

1. 先做 MySQL 电商订单系统，打牢表设计、索引、事务基础。
2. 再做 Redis 分布式 Session，共享登录态与缓存治理。
3. 接着做 RabbitMQ 异步通知，掌握异步解耦、重试、死信。
4. 最后做 Redis 秒杀系统，把 Redis、MQ、MySQL 三者能力串起来。

这样整理后，项目之间会形成明显的递进关系：数据库基础能力 → 缓存与会话能力 → 异步消息能力 → 高并发综合实战，更适合用于 Java 后端校招/实习简历。
