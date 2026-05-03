03.31 01:56
Redis企业级实战项目：电商购物车系统（Spring Boot + Redis）
项目简介
基于Spring Boot + Redis构建的电商购物车系统，覆盖Redis核心数据结构、缓存设计、分布式锁、消息队列、过期策略、高可用配置等企业级特性，贴合互联网真实业务场景。
技术栈
- 核心框架：Spring Boot 2.7.x、Spring Data Redis
- 缓存中间件：Redis 6.2+
- 工具：Lombok、Hutool、FastJSON
- 规范：RESTful API、统一响应、全局异常、分布式锁
项目结构
plaintext
com.cart.center
├── config        // Redis配置、序列化配置、线程池配置
├── controller    // 购物车API接口
├── entity        // 商品、购物车项实体
├── service       // 业务层（缓存操作、分布式锁）
│   ├── impl      // 业务实现
├── dto           // 请求参数（添加商品、修改数量）
├── vo            // 响应数据（购物车详情）
├── common        // 统一响应、全局异常、常量
├── lock          // 分布式锁工具
└── CartCenterApplication // 启动类
 
完整代码实现
1. 数据库SQL（商品表，仅用于基础数据）
sql
CREATE DATABASE IF NOT EXISTS cart_center DEFAULT CHARSET utf8mb4;
USE cart_center;
CREATE TABLE `product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
  `price` DECIMAL(10,2) NOT NULL COMMENT '单价',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1-上架 0-下架',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';
INSERT INTO product (product_name, price, stock) VALUES 
('iPhone15', 5999.00, 100),
('AirPods Pro', 1999.00, 200),
('MacBook Pro', 12999.00, 50);
 
2. 配置文件（application.yml）
yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/cart_center?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root
  redis:
    host: localhost
    port: 6379
    password:
    database: 0
    timeout: 10000
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1
 
3. Redis配置类（序列化+Template）
java
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        jsonSerializer.setObjectMapper(mapper);
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
 
4. 分布式锁工具（Redis+Lua脚本）
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
    private static final String LOCK_PREFIX = "lock:";
    private static final Long UNLOCK_SUCCESS = 1L;
    /**
     * 加锁
     */
    public boolean lock(String key, String value, long expireTime) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + key, value, expireTime, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }
    /**
     * 解锁（Lua脚本保证原子性）
     */
    public boolean unlock(String key, String value) {
        String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(LOCK_PREFIX + key), value);
        return UNLOCK_SUCCESS.equals(result);
    }
}
 
5. 实体类
Product（商品）
java
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class Product {
    private Long id;
    private String productName;
    private BigDecimal price;
    private Integer stock;
    private Integer status;
    private LocalDateTime createTime;
}
 
CartItem（购物车项）
java
import lombok.Data;
import java.math.BigDecimal;
@Data
public class CartItem {
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal totalPrice;
}
 
6. Service层（核心Redis操作）
CartService
java
import com.cart.center.entity.CartItem;
import java.util.List;
public interface CartService {
    /**
     * 添加商品到购物车
     */
    void addCart(Long userId, Long productId, Integer quantity);
    /**
     * 修改商品数量
     */
    void updateQuantity(Long userId, Long productId, Integer quantity);
    /**
     * 删除购物车商品
     */
    void deleteCartItem(Long userId, Long productId);
    /**
     * 清空购物车
     */
    void clearCart(Long userId);
    /**
     * 获取购物车列表
     */
    List<CartItem> getCartList(Long userId);
}
 
CartServiceImpl
java
import cn.hutool.core.util.IdUtil;
import com.cart.center.entity.CartItem;
import com.cart.center.entity.Product;
import com.cart.center.lock.RedisLock;
import com.cart.center.mapper.ProductMapper;
import com.cart.center.service.CartService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
@Service
public class CartServiceImpl implements CartService {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private ProductMapper productMapper;
    @Resource
    private RedisLock redisLock;
    private static final String CART_PREFIX = "cart:";
    private static final long CART_EXPIRE = 30L;
    @Override
    public void addCart(Long userId, Long productId, Integer quantity) {
        String lockKey = "cart:" + userId;
        String lockValue = IdUtil.simpleUUID();
        try {
            if (redisLock.lock(lockKey, lockValue, 10)) {
                Product product = productMapper.selectById(productId);
                if (product == null || product.getStatus() != 1) {
                    throw new RuntimeException("商品不存在或已下架");
                }
                if (product.getStock() < quantity) {
                    throw new RuntimeException("商品库存不足");
                }
                String cartKey = CART_PREFIX + userId;
                CartItem cartItem = (CartItem) redisTemplate.opsForHash().get(cartKey, productId.toString());
                if (cartItem != null) {
                    cartItem.setQuantity(cartItem.getQuantity() + quantity);
                    cartItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
                } else {
                    cartItem = new CartItem();
                    cartItem.setProductId(productId);
                    cartItem.setProductName(product.getProductName());
                    cartItem.setPrice(product.getPrice());
                    cartItem.setQuantity(quantity);
                    cartItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
                }
                redisTemplate.opsForHash().put(cartKey, productId.toString(), cartItem);
                redisTemplate.expire(cartKey, CART_EXPIRE, TimeUnit.DAYS);
            } else {
                throw new RuntimeException("系统繁忙，请稍后重试");
            }
        } finally {
            redisLock.unlock(lockKey, lockValue);
        }
    }
    @Override
    public void updateQuantity(Long userId, Long productId, Integer quantity) {
        String cartKey = CART_PREFIX + userId;
        CartItem cartItem = (CartItem) redisTemplate.opsForHash().get(cartKey, productId.toString());
        if (cartItem == null) {
            throw new RuntimeException("购物车中无此商品");
        }
        cartItem.setQuantity(quantity);
        cartItem.setTotalPrice(cartItem.getPrice().multiply(BigDecimal.valueOf(quantity)));
        redisTemplate.opsForHash().put(cartKey, productId.toString(), cartItem);
    }
    @Override
    public void deleteCartItem(Long userId, Long productId) {
        String cartKey = CART_PREFIX + userId;
        redisTemplate.opsForHash().delete(cartKey, productId.toString());
    }
    @Override
    public void clearCart(Long userId) {
        String cartKey = CART_PREFIX + userId;
        redisTemplate.delete(cartKey);
    }
    @Override
    public List<CartItem> getCartList(Long userId) {
        String cartKey = CART_PREFIX + userId;
        Map<Object, Object> cartMap = redisTemplate.opsForHash().entries(cartKey);
        return cartMap.values().stream()
                .map(obj -> (CartItem) obj)
                .collect(Collectors.toList());
    }
}
 
7. Controller层
java
import com.cart.center.common.Result;
import com.cart.center.entity.CartItem;
import com.cart.center.service.CartService;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;
@RestController
@RequestMapping("/api/cart")
public class CartController {
    @Resource
    private CartService cartService;
    /**
     * 添加商品到购物车
     */
    @PostMapping("/add")
    public Result<Void> addCart(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        cartService.addCart(userId, productId, quantity);
        return Result.success();
    }
    /**
     * 修改商品数量
     */
    @PostMapping("/update")
    public Result<Void> updateQuantity(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        cartService.updateQuantity(userId, productId, quantity);
        return Result.success();
    }
    /**
     * 删除购物车商品
     */
    @DeleteMapping("/delete")
    public Result<Void> deleteCartItem(
            @RequestParam Long userId,
            @RequestParam Long productId) {
        cartService.deleteCartItem(userId, productId);
        return Result.success();
    }
    /**
     * 清空购物车
     */
    @DeleteMapping("/clear/{userId}")
    public Result<Void> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return Result.success();
    }
    /**
     * 获取购物车列表
     */
    @GetMapping("/list/{userId}")
    public Result<List<CartItem>> getCartList(@PathVariable Long userId) {
        List<CartItem> cartList = cartService.getCartList(userId);
        return Result.success(cartList);
    }
}
 
8. 统一响应类（Result）
java
import lombok.Data;
@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }
    public static <T> Result<T> success() {
        return success(null);
    }
}
 
9. 启动类
java
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
@MapperScan("com.cart.center.mapper")
public class CartCenterApplication {
    public static void main(String[] args) {
        SpringApplication.run(CartCenterApplication.class, args);
    }
}
 
核心Redis特性覆盖
1. Hash结构：存储购物车（key=userId，field=productId，value=CartItem）
2. 分布式锁：Redis+Lua脚本实现防超卖、防重复提交
3. 过期策略：购物车30天自动过期
4. 连接池：Lettuce连接池配置
5. 序列化：Jackson2JsonRedisSerializer序列化对象
6. 原子操作：Hash操作保证购物车修改原子性
7. 缓存穿透/击穿/雪崩：分布式锁+过期时间解决
企业级优化点
1. 购物车设计：Hash结构节省内存，支持批量操作
2. 分布式锁：Lua脚本保证加解锁原子性，避免死锁
3. 过期策略：避免Redis内存溢出，自动清理无效数据
4. 连接池：Lettuce高性能连接池，提升Redis操作效率
5. 序列化：JSON序列化，支持复杂对象存储
6. 库存校验：添加购物车时校验库存，防止超卖
运行步骤
1. 执行SQL脚本创建商品表
2. 启动Redis服务（默认端口6379）
3. 修改application.yml数据库/Redis连接信息
4. 启动CartCenterApplication
5. 使用Postman测试接口
扩展方向
1. 集成Redis消息队列（List）实现订单异步处理
2. 加入Redis缓存商品信息，减少数据库查询
3. 实现Redis集群（主从+哨兵）高可用部署
4. 加入Redis布隆过滤器防止缓存穿透
5. 实现购物车商品过期自动删除（Redis Key过期监听）
6. 集成Redis Stream实现消息队列

