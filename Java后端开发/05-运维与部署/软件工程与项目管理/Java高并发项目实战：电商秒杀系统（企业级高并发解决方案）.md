Java高并发项目实战：电商秒杀系统（企业级高并发解决方案）
项目简介
基于Java高并发核心技术构建的电商秒杀系统，解决高并发场景下的超卖、库存一致性、接口限流、缓存击穿、消息异步削峰等问题，覆盖高并发开发全流程。
技术栈
- 核心框架：Spring Boot 2.7.x
- 缓存：Redis（分布式锁、限流、库存预减）
- 消息队列：RabbitMQ（异步削峰、解耦）
- 数据库：MySQL 8.0（事务、行锁）
- 高并发工具：Semaphore、CountDownLatch、线程池
- 其他：JWT、Lua脚本、分布式锁、接口限流
    项目结构
    plaintext
    seckill-system/
    ├── src/main/java/com/seckill
    │   ├── config          // 配置类（Redis、RabbitMQ、线程池）
    │   ├── controller      // 接口层（秒杀、商品、订单）
    │   ├── service         // 业务层
    │   │   └── impl        // 业务实现
    │   ├── mapper          // 数据访问层
    │   ├── entity          // 实体类
    │   ├── vo              // 响应对象
    │   ├── mq              // 消息队列生产者、消费者
    │   ├── lock            // 分布式锁
    │   ├── limit           // 限流工具
    │   └── util            // 工具类
    └── resources
    ├── application.yml // 配置文件
    └── lua             // Redis Lua脚本
 
核心代码实现
1. 数据库SQL脚本
    sql
    CREATE DATABASE IF NOT EXISTS seckill DEFAULT CHARSET utf8mb4;
    USE seckill;
    -- 商品表
    CREATE TABLE product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    price DECIMAL(10,2) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
    );
    -- 订单表
    CREATE TABLE `order` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    status TINYINT DEFAULT 1 COMMENT '1-待支付 2-已支付 3-已取消',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
    );
    -- 初始化商品
    INSERT INTO product (name, stock, price) VALUES ('iPhone 15', 100, 5999.00);
 
2. Redis配置类
    java
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.data.redis.connection.RedisConnectionFactory;
    import org.springframework.data.redis.core.RedisTemplate;
    import org.springframework.data.redis.serializer.StringRedisSerializer;
    @Configuration
    public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
    }
 
3. 分布式锁（Redis+Lua）
    java
    import org.springframework.data.redis.core.RedisTemplate;
    import org.springframework.data.redis.core.script.DefaultRedisScript;
    import org.springframework.stereotype.Component;
    import javax.annotation.Resource;
    import java.util.Collections;
    import java.util.concurrent.TimeUnit;
    @Component
    public class RedisLock {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    private static final String LOCK_PREFIX = "seckill:lock:";
    // 加锁
    public boolean lock(String key, String value, long expire) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + key, value, expire, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }
    // 解锁（Lua保证原子性）
    public boolean unlock(String key, String value) {
        String lua = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(lua);
        script.setResultType(Long.class);
        Long res = redisTemplate.execute(script, Collections.singletonList(LOCK_PREFIX + key), value);
        return res == 1;
    }
    }
 
4. 库存预减Lua脚本（resources/lua/stock.lua）
    lua
    local stock = redis.call('get', KEYS[1])
    if tonumber(stock) <= 0 then
    return 0
    end
    redis.call('decr', KEYS[1])
    return 1
 
5. 秒杀Service（核心高并发逻辑）
    java
    import com.seckill.lock.RedisLock;
    import com.seckill.mq.OrderProducer;
    import com.seckill.util.IdUtil;
    import org.springframework.data.redis.core.RedisTemplate;
    import org.springframework.data.redis.core.script.DefaultRedisScript;
    import org.springframework.stereotype.Service;
    import javax.annotation.Resource;
    import java.util.Collections;
    import java.util.concurrent.TimeUnit;
    @Service
    public class SeckillService {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RedisLock redisLock;
    @Resource
    private OrderProducer orderProducer;
    private static final String STOCK_KEY = "seckill:stock:";
    private static final String USER_KEY = "seckill:user:";
    /**
     * 秒杀核心方法（高并发优化）
     */
    public String seckill(Long userId, Long productId) {
        // 1. 防重复秒杀（Redis Set）
        String userSetKey = USER_KEY + productId;
        Boolean isMember = redisTemplate.opsForSet().isMember(userSetKey, userId.toString());
        if (Boolean.TRUE.equals(isMember)) {
            return "已参与过秒杀";
        }
        // 2. 库存预减（Lua原子操作）
        String stockKey = STOCK_KEY + productId;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new org.springframework.core.io.ClassPathResource("lua/stock.lua"));
        script.setResultType(Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(stockKey));
        if (result == 0) {
            return "库存不足";
        }
        // 3. 标记用户已参与
        redisTemplate.opsForSet().add(userSetKey, userId.toString());
        // 4. 发送消息队列异步创建订单
        String orderNo = IdUtil.simpleUUID();
        orderProducer.sendOrderMessage(userId, productId, orderNo);
        return orderNo;
    }
    }
 
6. RabbitMQ消息生产者
    java
    import org.springframework.amqp.rabbit.core.RabbitTemplate;
    import org.springframework.stereotype.Component;
    import javax.annotation.Resource;
    @Component
    public class OrderProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;
    private static final String EXCHANGE = "seckill.order";
    private static final String ROUTING_KEY = "order.create";
    public void sendOrderMessage(Long userId, Long productId, String orderNo) {
        String msg = userId + "," + productId + "," + orderNo;
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, msg);
    }
    }
 
7. RabbitMQ消息消费者（异步创建订单）
    java
    import com.seckill.entity.Order;
    import com.seckill.mapper.OrderMapper;
    import com.seckill.mapper.ProductMapper;
    import org.springframework.amqp.rabbit.annotation.RabbitListener;
    import org.springframework.stereotype.Component;
    import org.springframework.transaction.annotation.Transactional;
    import javax.annotation.Resource;
    @Component
    public class OrderConsumer {
    @Resource
    private OrderMapper orderMapper;
    @Resource
    private ProductMapper productMapper;
    @RabbitListener(queues = "seckill.order.queue")
    @Transactional
    public void handleOrderMessage(String msg) {
        String[] arr = msg.split(",");
        Long userId = Long.parseLong(arr[0]);
        Long productId = Long.parseLong(arr[1]);
        String orderNo = arr[2];
        // 1. 扣减数据库库存（行锁）
        int rows = productMapper.reduceStock(productId);
        if (rows == 0) {
            throw new RuntimeException("库存不足");
        }
        // 2. 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setProductId(productId);
        orderMapper.insert(order);
    }
    }
 
8. 秒杀Controller
    java
    import com.seckill.service.SeckillService;
    import com.seckill.vo.Result;
    import org.springframework.web.bind.annotation.PostMapping;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RequestParam;
    import org.springframework.web.bind.annotation.RestController;
    import javax.annotation.Resource;
    @RestController
    @RequestMapping("/seckill")
    public class SeckillController {
    @Resource
    private SeckillService seckillService;
    @PostMapping("/do")
    public Result<String> doSeckill(@RequestParam Long userId, @RequestParam Long productId) {
        String result = seckillService.seckill(userId, productId);
        return Result.success(result);
    }
    }
 
9. 库存预热（项目启动时加载到Redis）
    java
    import com.seckill.entity.Product;
    import com.seckill.mapper.ProductMapper;
    import org.springframework.boot.CommandLineRunner;
    import org.springframework.data.redis.core.RedisTemplate;
    import org.springframework.stereotype.Component;
    import javax.annotation.Resource;
    import java.util.List;
    @Component
    public class StockWarmup implements CommandLineRunner {
    @Resource
    private ProductMapper productMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    private static final String STOCK_KEY = "seckill:stock:";
    @Override
    public void run(String... args) {
        List<Product> products = productMapper.selectList(null);
        for (Product product : products) {
            redisTemplate.opsForValue().set(STOCK_KEY + product.getId(), product.getStock().toString());
        }
    }
    }
 
高并发核心优化点
1. Redis预减库存：避免直接请求数据库，减少DB压力
2. Lua原子操作：防止库存超卖，保证原子性
3. 防重复秒杀：Redis Set集合记录用户，避免重复下单
4. 消息队列削峰：异步创建订单，缓解同步压力
5. 数据库行锁：扣减库存时使用行锁，保证一致性
6. 分布式锁：防止并发重复操作
7. 接口限流：使用Semaphore或Redis限流，防止压垮服务
    运行步骤
    1. 执行SQL脚本创建库表
    2. 启动Redis、RabbitMQ
    3. 配置application.yml连接信息
    4. 启动项目，库存自动预热到Redis
    5. 压测秒杀接口，验证高并发效果
    扩展方向
    1. 加入接口限流（Guava RateLimiter）
    2. 实现订单超时取消（死信队列）
    3. 加入缓存击穿/穿透防护（布隆过滤器）
    4. 集成Sentinel实现熔断降级
    5. 实现分布式Session共享
    6. 加入监控（Prometheus+Grafana）
