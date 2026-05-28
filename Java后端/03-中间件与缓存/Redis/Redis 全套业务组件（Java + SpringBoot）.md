# 完整版：Redis 全套业务组件（Java + SpringBoot）
整合**底层工具类 + 8大业务组件**，单文件可直接复制进项目，开箱即用，企业级规范、key统一前缀、自带过期、线程安全。

## 1. 统一Redis配置类
```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);
        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
```

## 2. 底层基础工具类
```java
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisBaseUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long expireSec) {
        if (expireSec > 0) {
            redisTemplate.opsForValue().set(key, value, expireSec, TimeUnit.SECONDS);
        } else {
            set(key, value);
        }
    }

    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Long deleteBatch(List<String> keyList) {
        if (CollectionUtils.isEmpty(keyList)) {
            return 0L;
        }
        return redisTemplate.delete(keyList);
    }

    public Boolean expire(String key, long expireSec) {
        return redisTemplate.expire(key, expireSec, TimeUnit.SECONDS);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long incr(String key, long step) {
        return redisTemplate.opsForValue().increment(key, step);
    }

    public Boolean setNx(String key, Object value, long expireSec) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, expireSec, TimeUnit.SECONDS);
    }
}
```

## 3. 缓存组件
```java
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

@Component
public class CacheComponent {

    @Resource
    private RedisBaseUtil redisBaseUtil;

    private static final String KEY_PREFIX = "cache:";

    public void put(String bizKey, Object data, long expireSec) {
        String key = KEY_PREFIX + bizKey;
        redisBaseUtil.set(key, data, expireSec);
    }

    public Object get(String bizKey) {
        String key = KEY_PREFIX + bizKey;
        return redisBaseUtil.get(key);
    }

    public void remove(String bizKey) {
        String key = KEY_PREFIX + bizKey;
        redisBaseUtil.delete(key);
    }
}
```

## 4. 分布式锁组件
```java
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

@Component
public class RedisLockComponent {

    @Resource
    private RedisBaseUtil redisBaseUtil;

    private static final String KEY_PREFIX = "lock:";

    public boolean tryLock(String lockBizKey, long expireSec) {
        String key = KEY_PREFIX + lockBizKey;
        return redisBaseUtil.setNx(key, System.currentTimeMillis(), expireSec);
    }

    public void releaseLock(String lockBizKey) {
        String key = KEY_PREFIX + lockBizKey;
        redisBaseUtil.delete(key);
    }
}
```

## 5. 限流防刷组件
```java
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

@Component
public class RateLimitComponent {

    @Resource
    private RedisBaseUtil redisBaseUtil;

    private static final String KEY_PREFIX = "limit:";

    public boolean isLimited(String uniqueKey, int maxCount, long cycleSec) {
        String key = KEY_PREFIX + uniqueKey;
        Long count = redisBaseUtil.incr(key);
        if (count == 1) {
            redisBaseUtil.expire(key, cycleSec);
        }
        return count > maxCount;
    }
}
```

## 6. 计数器组件
```java
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

@Component
public class CounterComponent {

    @Resource
    private RedisBaseUtil redisBaseUtil;

    private static final String KEY_PREFIX = "count:";

    public Long add(String bizKey) {
        return redisBaseUtil.incr(KEY_PREFIX + bizKey);
    }

    public Long add(String bizKey, long step) {
        return redisBaseUtil.incr(KEY_PREFIX + bizKey, step);
    }

    public Long sub(String bizKey) {
        return redisBaseUtil.incr(KEY_PREFIX + bizKey, -1);
    }
}
```

## 7. 验证码组件
```java
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.Objects;

@Component
public class SmsCodeComponent {

    @Resource
    private RedisBaseUtil redisBaseUtil;

    private static final String KEY_PREFIX = "code:sms:";
    private static final long DEFAULT_EXPIRE = 300;

    public void saveCode(String phone, String code) {
        String key = KEY_PREFIX + phone;
        redisBaseUtil.set(key, code, DEFAULT_EXPIRE);
    }

    public boolean verifyCode(String phone, String code) {
        String key = KEY_PREFIX + phone;
        String cacheCode = (String) redisBaseUtil.get(key);
        if (Objects.isNull(cacheCode)) {
            return false;
        }
        boolean result = cacheCode.equals(code);
        if (result) {
            redisBaseUtil.delete(key);
        }
        return result;
    }
}
```

## 8. 简易消息队列组件
```java
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

@Component
public class SimpleQueueComponent {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    private static final String KEY_PREFIX = "queue:";

    public void push(String queueName, Object task) {
        String key = KEY_PREFIX + queueName;
        redisTemplate.opsForList().rightPush(key, task);
    }

    public Object poll(String queueName) {
        String key = KEY_PREFIX + queueName;
        return redisTemplate.opsForList().leftPop(key);
    }
}
```

## 9. 黑名单组件
```java
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

@Component
public class BlackListComponent {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    private static final String KEY_PREFIX = "black:";

    public void addBlack(String type, String value) {
        String key = KEY_PREFIX + type;
        redisTemplate.opsForSet().add(key, value);
    }

    public boolean inBlackList(String type, String value) {
        String key = KEY_PREFIX + type;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }
}
```

## 10. 排行榜组件
```java
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.Set;

@Component
public class RankComponent {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    private static final String KEY_PREFIX = "rank:";

    public void putScore(String rankType, String member, double score) {
        String key = KEY_PREFIX + rankType;
        redisTemplate.opsForZSet().add(key, member, score);
    }

    public Set<ZSetOperations.TypedTuple<Object>> getTop(String rankType, int topN) {
        String key = KEY_PREFIX + rankType;
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, topN - 1);
    }
}
```

---

# 统一使用示例
```java
@RestController
public class DemoController {

    @Resource
    private CacheComponent cacheComponent;
    @Resource
    private RedisLockComponent lockComponent;
    @Resource
    private RateLimitComponent rateLimitComponent;

    @GetMapping("/demo")
    public String demo() {
        // 1.缓存
        cacheComponent.put("user:1001","用户数据",3600);

        // 2.分布式锁
        if(lockComponent.tryLock("order:123",10)){
            try {
                // 业务逻辑
            }finally {
                lockComponent.releaseLock("order:123");
            }
        }

        // 3.限流
        if(rateLimitComponent.isLimited("ip:127.0.0.1",100,60)){
            return "访问频繁";
        }
        return "ok";
    }
}
```

---

# 依赖 & 配置
pom.xml
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

application.yml
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
```

---
