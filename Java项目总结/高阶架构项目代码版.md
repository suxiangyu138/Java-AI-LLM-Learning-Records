# 高阶架构项目代码版

下面这份不是空谈方案，而是你可以直接拿去拆项目的“核心代码骨架”。

定位：

- 适合你做 Java 后端简历项目。
- 适合继续往 Spring Boot 单体 → 微服务演进。
- 适合面试时讲“核心链路怎么实现”。

---

## 1. 分布式短链接系统

### 1.1 表结构

```sql
CREATE TABLE t_link (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(16) NOT NULL UNIQUE,
    long_url VARCHAR(2048) NOT NULL,
    expire_time DATETIME NULL,
    status TINYINT NOT NULL DEFAULT 1,
    pv BIGINT NOT NULL DEFAULT 0,
    uv BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE t_link_access_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(16) NOT NULL,
    ip VARCHAR(64),
    user_agent VARCHAR(512),
    referer VARCHAR(1024),
    access_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 1.2 Base62 工具类

```java
public class Base62Util {
    private static final String CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String encode(long num) {
        if (num == 0) return "0";
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(CHARS.charAt((int)(num % 62)));
            num /= 62;
        }
        return sb.reverse().toString();
    }
}
```

### 1.3 雪花 ID

```java
public class IdGenerator {
    private final long workerId;
    private final long datacenterId;
    private long sequence;
    private long lastTimestamp = -1L;

    public IdGenerator(long workerId, long datacenterId) {
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) throw new RuntimeException("Clock moved backwards");
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & 4095;
            if (sequence == 0) {
                while ((timestamp = System.currentTimeMillis()) <= lastTimestamp) {}
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        return ((timestamp - 1704067200000L) << 22) | (datacenterId << 17) | (workerId << 12) | sequence;
    }
}
```

### 1.4 生成短链核心服务

```java
@Service
public class ShortLinkService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private LinkMapper linkMapper;
    @Resource
    private IdGenerator idGenerator;

    public String createShortLink(String longUrl, LocalDateTime expireTime) {
        long id = idGenerator.nextId();
        String code = Base62Util.encode(id);
        LinkDO link = new LinkDO();
        link.setCode(code);
        link.setLongUrl(longUrl);
        link.setExpireTime(expireTime);
        linkMapper.insert(link);
        stringRedisTemplate.opsForValue().set("short:url:" + code, longUrl, 7, TimeUnit.DAYS);
        return code;
    }
}
```

### 1.5 跳转接口

```java
@RestController
@RequestMapping("/s")
public class ShortLinkController {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private LinkMapper linkMapper;
    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @GetMapping("/{code}")
    public void redirect(@PathVariable String code, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String key = "short:url:" + code;
        String longUrl = stringRedisTemplate.opsForValue().get(key);
        if (longUrl == null) {
            LinkDO link = linkMapper.selectByCode(code);
            if (link == null || (link.getExpireTime() != null && link.getExpireTime().isBefore(LocalDateTime.now()))) {
                response.setStatus(404);
                return;
            }
            longUrl = link.getLongUrl();
            stringRedisTemplate.opsForValue().set(key, longUrl, 7, TimeUnit.DAYS);
        }
        kafkaTemplate.send("link-access-topic", code + "|" + request.getRemoteAddr());
        response.sendRedirect(longUrl);
    }
}
```

### 1.6 布隆过滤器初始化

```java
@Configuration
public class BloomFilterConfig {
    @Bean
    public RBloomFilter<String> linkBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("link:bloom");
        bloomFilter.tryInit(1000000L, 0.01);
        return bloomFilter;
    }
}
```

### 1.7 查询前布隆校验

```java
@Service
public class RedirectService {
    @Resource
    private RBloomFilter<String> linkBloomFilter;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private LinkMapper linkMapper;

    public String getLongUrl(String code) {
        if (!linkBloomFilter.contains(code)) return null;
        String cache = stringRedisTemplate.opsForValue().get("short:url:" + code);
        if (cache != null) return cache;
        LinkDO link = linkMapper.selectByCode(code);
        if (link == null) {
            stringRedisTemplate.opsForValue().set("short:null:" + code, "1", 5, TimeUnit.MINUTES);
            return null;
        }
        stringRedisTemplate.opsForValue().set("short:url:" + code, link.getLongUrl(), 7, TimeUnit.DAYS);
        return link.getLongUrl();
    }
}
```

### 1.8 访问统计消费

```java
@Component
public class LinkAccessConsumer {
    @Resource
    private LinkMapper linkMapper;
    @Resource
    private LinkAccessLogMapper linkAccessLogMapper;

    @KafkaListener(topics = "link-access-topic", groupId = "link-stat-group")
    public void consume(String message) {
        String[] arr = message.split("\\|");
        String code = arr[0];
        String ip = arr.length > 1 ? arr[1] : "unknown";
        linkMapper.incrPv(code);
        LinkAccessLogDO log = new LinkAccessLogDO();
        log.setCode(code);
        log.setIp(ip);
        linkAccessLogMapper.insert(log);
    }
}
```

---

## 2. 高并发直播弹幕系统

### 2.1 WebSocket 接入核心

```java
public class DanmuServer {
    public static void main(String[] args) throws Exception {
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(65536));
                            ch.pipeline().addLast(new ChunkedWriteHandler());
                            ch.pipeline().addLast(new WebSocketServerProtocolHandler("/ws"));
                            ch.pipeline().addLast(new DanmuHandler());
                        }
                    });
            bootstrap.bind(9001).sync().channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
```

### 2.2 房间连接管理

```java
public class RoomChannelManager {
    private static final ConcurrentHashMap<Long, ChannelGroup> ROOM_CHANNELS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<ChannelId, Long> CHANNEL_ROOM = new ConcurrentHashMap<>();

    public static void joinRoom(Long roomId, Channel channel) {
        ROOM_CHANNELS.computeIfAbsent(roomId, k -> new DefaultChannelGroup(GlobalEventExecutor.INSTANCE)).add(channel);
        CHANNEL_ROOM.put(channel.id(), roomId);
    }

    public static void remove(Channel channel) {
        Long roomId = CHANNEL_ROOM.remove(channel.id());
        if (roomId != null) {
            ChannelGroup group = ROOM_CHANNELS.get(roomId);
            if (group != null) group.remove(channel);
        }
    }

    public static void broadcast(Long roomId, String message) {
        ChannelGroup group = ROOM_CHANNELS.get(roomId);
        if (group != null) group.writeAndFlush(new TextWebSocketFrame(message));
    }
}
```

### 2.3 弹幕消息处理

```java
public class DanmuHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        RoomChannelManager.remove(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws Exception {
        DanmuMessage msg = MAPPER.readValue(frame.text(), DanmuMessage.class);
        if ("join".equals(msg.getType())) {
            RoomChannelManager.joinRoom(msg.getRoomId(), ctx.channel());
            return;
        }
        if ("danmu".equals(msg.getType())) {
            RoomChannelManager.broadcast(msg.getRoomId(), MAPPER.writeValueAsString(msg));
        }
    }
}
```

### 2.4 限流器

```java
@Service
public class DanmuRateLimiter {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public boolean allow(Long userId) {
        String key = "danmu:limit:" + userId;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, 1, TimeUnit.SECONDS);
        }
        return count != null && count <= 5;
    }
}
```

### 2.5 弹幕接入服务

```java
@Service
public class DanmuService {
    @Resource
    private DanmuRateLimiter danmuRateLimiter;
    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void send(DanmuMessage message) throws Exception {
        if (!danmuRateLimiter.allow(message.getUserId())) {
            throw new RuntimeException("Too many requests");
        }
        kafkaTemplate.send("danmu-topic", MAPPER.writeValueAsString(message));
    }
}
```

### 2.6 MQ 消费并广播

```java
@Component
public class DanmuConsumer {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @KafkaListener(topics = "danmu-topic", groupId = "danmu-group")
    public void consume(String text) throws Exception {
        DanmuMessage msg = MAPPER.readValue(text, DanmuMessage.class);
        RoomChannelManager.broadcast(msg.getRoomId(), text);
    }
}
```

### 2.7 敏感词过滤

```java
@Component
public class SensitiveWordFilter {
    private final List<String> words = Arrays.asList("傻X", "违规词1", "违规词2");

    public String filter(String content) {
        String result = content;
        for (String word : words) {
            result = result.replace(word, "**");
        }
        return result;
    }
}
```

---

## 3. 多级缓存商品详情页

### 3.1 表结构

```sql
CREATE TABLE t_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL,
    detail TEXT,
    status TINYINT NOT NULL DEFAULT 1,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 3.2 Caffeine + Redis 多级缓存

```java
@Configuration
public class CacheConfig {
    @Bean
    public Cache<Long, ProductVO> localProductCache() {
        return Caffeine.newBuilder()
                .initialCapacity(1000)
                .maximumSize(10000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
    }
}
```

### 3.3 商品详情查询

```java
@Service
public class ProductService {
    @Resource
    private Cache<Long, ProductVO> localProductCache;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ProductMapper productMapper;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ProductVO getDetail(Long productId) throws Exception {
        ProductVO local = localProductCache.getIfPresent(productId);
        if (local != null) return local;

        String redisKey = "product:detail:" + productId;
        String redisValue = stringRedisTemplate.opsForValue().get(redisKey);
        if (redisValue != null) {
            ProductVO vo = MAPPER.readValue(redisValue, ProductVO.class);
            localProductCache.put(productId, vo);
            return vo;
        }

        ProductDO product = productMapper.selectById(productId);
        if (product == null) {
            stringRedisTemplate.opsForValue().set("product:null:" + productId, "1", 2, TimeUnit.MINUTES);
            return null;
        }

        ProductVO vo = new ProductVO(product.getId(), product.getName(), product.getPrice(), product.getStock(), product.getDetail());
        stringRedisTemplate.opsForValue().set(redisKey, MAPPER.writeValueAsString(vo), 30, TimeUnit.MINUTES);
        localProductCache.put(productId, vo);
        return vo;
    }
}
```

### 3.4 更新商品并删除缓存

```java
@Service
public class ProductAdminService {
    @Resource
    private ProductMapper productMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private Cache<Long, ProductVO> localProductCache;

    @Transactional
    public void updateProduct(ProductDO product) {
        productMapper.updateById(product);
        String key = "product:detail:" + product.getId();
        stringRedisTemplate.delete(key);
        localProductCache.invalidate(product.getId());
    }
}
```

### 3.5 逻辑过期模型

```java
public class RedisData<T> {
    private LocalDateTime expireTime;
    private T data;

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
```

### 3.6 逻辑过期查询

```java
@Service
public class ProductCacheService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ProductMapper productMapper;
    @Resource
    private ThreadPoolTaskExecutor taskExecutor;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ProductVO queryWithLogicalExpire(Long productId) throws Exception {
        String key = "product:logic:" + productId;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) return null;
        RedisData<ProductVO> redisData = MAPPER.readValue(json, new TypeReference<RedisData<ProductVO>>(){});
        if (redisData.getExpireTime().isAfter(LocalDateTime.now())) {
            return redisData.getData();
        }
        String lockKey = "product:lock:" + productId;
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 30, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lock)) {
            taskExecutor.execute(() -> {
                try {
                    ProductDO product = productMapper.selectById(productId);
                    ProductVO vo = new ProductVO(product.getId(), product.getName(), product.getPrice(), product.getStock(), product.getDetail());
                    RedisData<ProductVO> newData = new RedisData<>();
                    newData.setData(vo);
                    newData.setExpireTime(LocalDateTime.now().plusMinutes(30));
                    stringRedisTemplate.opsForValue().set(key, MAPPER.writeValueAsString(newData));
                } catch (Exception ignored) {
                } finally {
                    stringRedisTemplate.delete(lockKey);
                }
            });
        }
        return redisData.getData();
    }
}
```

---

## 4. 分布式定时任务平台

### 4.1 引入 XXL-Job 执行器配置

```java
@Configuration
public class XxlJobConfig {
    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses("http://127.0.0.1:8080/xxl-job-admin");
        executor.setAppname("demo-executor");
        executor.setIp("127.0.0.1");
        executor.setPort(9999);
        executor.setLogPath("/data/applogs/xxl-job/jobhandler");
        executor.setLogRetentionDays(30);
        return executor;
    }
}
```

### 4.2 订单超时取消任务

```java
@Component
public class OrderJobHandler {
    @Resource
    private OrderService orderService;

    @XxlJob("closeTimeoutOrderJob")
    public void closeTimeoutOrderJob() {
        orderService.closeTimeoutOrders();
    }
}
```

### 4.3 订单关闭实现

```java
@Service
public class OrderService {
    @Resource
    private OrderMapper orderMapper;

    @Transactional
    public void closeTimeoutOrders() {
        orderMapper.batchCloseTimeoutOrders();
    }
}
```

```sql
UPDATE t_order
SET status = 3
WHERE status = 1
  AND create_time < DATE_SUB(NOW(), INTERVAL 30 MINUTE);
```

### 4.4 失败重试任务

```java
@Component
public class RetryJobHandler {
    @Resource
    private RemoteSyncService remoteSyncService;

    @XxlJob("retrySyncJob")
    public void retrySyncJob() {
        List<Long> ids = remoteSyncService.queryRetryIds();
        for (Long id : ids) {
            try {
                remoteSyncService.retry(id);
            } catch (Exception e) {
                remoteSyncService.markFail(id, e.getMessage());
            }
        }
    }
}
```

### 4.5 分片任务

```java
@Component
public class ShardingJobHandler {
    @XxlJob("userPointShardJob")
    public void userPointShardJob() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        for (long userId = shardIndex + 1; userId <= 1000000; userId += shardTotal) {
            handle(userId);
        }
    }

    private void handle(long userId) {
    }
}
```

### 4.6 任务日志封装

```java
@Component
public class JobLogHelper {
    public static void log(String content) {
        XxlJobHelper.log("[{}] {}", LocalDateTime.now(), content);
    }

    public static void fail(String reason) {
        XxlJobHelper.log("fail: {}", reason);
        XxlJobHelper.handleFail(reason);
    }
}
```

### 4.7 幂等控制

```java
@Service
public class IdempotentService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public boolean tryExec(String key) {
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent("job:idem:" + key, "1", 1, TimeUnit.HOURS);
        return Boolean.TRUE.equals(success);
    }
}
```

```java
@Component
public class CouponExpireJobHandler {
    @Resource
    private IdempotentService idempotentService;
    @Resource
    private CouponService couponService;

    @XxlJob("couponExpireJob")
    public void couponExpireJob() {
        String jobKey = "couponExpire:" + LocalDate.now();
        if (!idempotentService.tryExec(jobKey)) return;
        couponService.expireCoupons();
    }
}
```

---

## 5. 项目目录建议

### 5.1 短链接系统

```text
short-link-system
├── short-link-admin
├── short-link-api
├── short-link-common
├── short-link-dao
├── short-link-job
└── short-link-stat
```

### 5.2 直播弹幕系统

```text
live-danmu-system
├── danmu-gateway
├── danmu-core
├── danmu-common
├── danmu-mq-consumer
└── danmu-storage
```

### 5.3 商品详情页系统

```text
product-detail-cache
├── product-api
├── product-service
├── product-common
├── product-dao
└── product-admin
```

### 5.4 定时任务平台

```text
distributed-job-platform
├── job-admin-extension
├── job-executor-biz
├── job-common
└── job-monitor
```

---

## 6. 你接下来怎么做最合理

建议你按这个节奏推进：

1. 先把 4 个项目都做成单体可运行版本。
2. 每个项目先打通主链路，不要一开始就疯狂堆功能。
3. 主链路通了之后，再补限流、异步、监控、幂等、压测。
4. 最后再做简历包装、项目图、面试话术。

---

## 7. 最适合你当前阶段的开发顺序

推荐顺序：

1. 多级缓存商品详情页
2. 分布式短链接系统
3. 分布式定时任务平台
4. 高并发直播弹幕系统

原因很直接：

- 商品详情页最容易打通，也最贴近 Java 后端面试。
- 短链接系统最适合练缓存、布隆过滤器、异步统计。
- XXL-Job 平台能快速做出企业项目感。
- Netty 弹幕系统最难，适合最后冲高阶。
