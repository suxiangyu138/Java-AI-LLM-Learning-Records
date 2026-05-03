# Redis Lua 脚本核心知识点
Redis 内置了 Lua 解释器，支持直接执行 Lua 脚本，是实现**原子性复杂操作、高性能业务逻辑**的核心利器，也是解决分布式锁误删、限流、事务等问题的关键方案。

---

## 一、核心优势与适用场景
### 1. 为什么要用 Lua 脚本？
| 特性 | 说明 |
|------|------|
| **原子执行** | 脚本中的所有命令会作为一个整体原子执行，不会被其他命令打断，天然避免竞态条件 |
| **减少网络开销** | 多条命令打包成一个脚本，一次网络往返完成，性能远高于多次调用 |
| **逻辑封装** | 可以在服务端实现复杂的业务逻辑，避免客户端多次交互 |
| **复用性强** | 脚本可以缓存到 Redis 服务器，后续直接通过 SHA1 调用 |

### 2. 高频使用场景
- 分布式锁安全释放（防误删）
- 原子限流/计数器（滑动窗口、令牌桶）
- 复杂数据操作（如社交关系链批量修改）
- 条件执行（如“库存充足才扣减”）

---

## 二、基础语法与 Redis 命令调用
### 1. 核心 API：`redis.call()` / `redis.pcall()`
- `redis.call()`：执行 Redis 命令，出错直接抛出异常，脚本终止
- `redis.pcall()`：执行 Redis 命令，出错返回错误信息，脚本继续执行
```lua
-- 示例：设置键值对并获取值
redis.call('SET', 'user:1001:name', '张三')
local name = redis.call('GET', 'user:1001:name')
return name
```

### 2. 数据类型转换规则
Redis 命令返回值与 Lua 类型会自动转换：
| Redis 类型 | Lua 类型 |
|------------|----------|
| 整数回复 | number |
| 字符串回复 | string |
| 多行字符串回复 | table（数组形式） |
| 错误回复 | 错误对象 |
| 空回复 | `nil` |

---

## 三、关键命令与脚本管理
### 1. 常用 Redis 命令
| 命令 | 作用 |
|------|------|
| `EVAL script numkeys key [key ...] arg [arg ...]` | 直接执行 Lua 脚本 |
| `EVALSHA sha1 numkeys key [key ...] arg [arg ...]` | 通过缓存的 SHA1 调用脚本 |
| `SCRIPT LOAD script` | 将脚本缓存到服务器，返回 SHA1 |
| `SCRIPT EXISTS sha1 [sha1 ...]` | 检查脚本是否已缓存 |
| `SCRIPT FLUSH` | 清空服务器缓存的所有脚本 |
| `SCRIPT KILL` | 终止正在执行的脚本（仅当脚本未执行写命令时） |

### 2. 执行流程示例
```bash
# 1. 加载脚本到 Redis，得到 SHA1
redis-cli SCRIPT LOAD "return redis.call('GET', KEYS[1])"
# 输出 "a5260dd36e6...（SHA1值）"

# 2. 通过 SHA1 执行脚本
redis-cli EVALSHA a5260dd36e6 1 "user:1001:name"
```

---

## 四、企业级实战案例
### 1. 安全释放分布式锁（经典场景）
解决“锁被其他请求误删”的问题，只有持有锁的客户端才能释放锁：
```lua
-- KEYS[1]：锁的key
-- ARGV[1]：锁的唯一标识（如UUID）
if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('DEL', KEYS[1])
else
    return 0
end
```
Java 中调用示例：
```java
String script = "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end";
Long result = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Collections.singletonList(lockKey), requestId);
```

### 2. 原子限流（滑动窗口简化版）
实现“1分钟内最多访问100次”的原子限流：
```lua
-- KEYS[1]：限流key
-- ARGV[1]：窗口大小（秒）
-- ARGV[2]：最大请求数
local current = redis.call('INCR', KEYS[1])
if tonumber(current) == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[1])
end
if tonumber(current) > tonumber(ARGV[2]) then
    return 0 -- 限流
else
    return 1 -- 放行
end
```

### 3. 社交关系链原子操作
实现“关注用户并将其动态同步到自己时间线”的原子操作，避免中间状态：
```lua
-- KEYS[1]：用户关注列表key
-- KEYS[2]：用户粉丝列表key
-- KEYS[3]：用户时间线key
-- ARGV[1]：目标用户ID
-- ARGV[2]：目标用户动态的score（时间戳）
redis.call('SADD', KEYS[1], ARGV[1])
redis.call('SADD', KEYS[2], ARGV[1])
redis.call('ZADD', KEYS[3], ARGV[2], ARGV[1])
return 1
```

---

## 五、关键注意事项与避坑
### 1. 脚本执行的原子性限制
- Redis 单线程执行脚本，脚本执行期间会阻塞其他所有命令，**禁止编写长时间运行的脚本**
- 脚本中禁止使用需要用户交互或长时间阻塞的操作
- 脚本执行时间超过配置的 `lua-time-limit`（默认5秒）后，Redis 会：
  - 拒绝执行新命令
  - 允许执行 `SCRIPT KILL` 终止脚本（未执行写命令时）
  - 若脚本已执行写命令，只能通过 `SHUTDOWN NOSAVE` 关闭服务器终止

### 2. 脚本中的 key 必须通过 `KEYS` 传递
- 必须通过 `KEYS` 数组传递 key，不能直接在脚本中硬编码 key
- 原因：Redis Cluster 模式下，脚本执行时需要根据 `KEYS` 确定槽位，路由到正确节点

### 3. 避免在脚本中使用不确定的随机函数
- 脚本必须是“纯函数”：相同输入必须产生相同输出，以保证主从复制和持久化的一致性
- 禁止在脚本中使用 `RANDOM`、`TIME` 等会产生不确定结果的命令

### 4. 脚本缓存与一致性
- 脚本加载到 Redis 后，修改客户端脚本内容不会影响服务器缓存的版本
- 若脚本逻辑修改，需要重新加载并更新 SHA1

---

## 六、与 Redis 事务的对比
| 特性 | Lua 脚本 | Redis 事务（MULTI/EXEC） |
|------|----------|---------------------------|
| 原子性 | 完全原子，脚本整体执行 | 事务内命令依次入队，执行时整体原子 |
| 错误处理 | 可通过 `pcall` 捕获错误 | 事务执行中某条命令失败，后续命令仍会执行 |
| 逻辑复杂度 | 支持复杂条件判断、循环 | 仅支持命令批量执行，无分支逻辑 |
| 性能 | 单次网络往返，性能高 | 多次网络往返（入队+执行），性能较低 |
| 适用场景 | 复杂原子操作、条件执行 | 简单命令批量执行 |

---

## 七、Java 中集成 Lua 脚本的标准方式
```java
@Service
public class RedisLuaService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 1. 安全释放锁脚本
    private static final String UNLOCK_SCRIPT = "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end";
    private static final DefaultRedisScript<Long> UNLOCK_REDIS_SCRIPT;

    static {
        UNLOCK_REDIS_SCRIPT = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
    }

    public boolean unlock(String lockKey, String requestId) {
        Long result = redisTemplate.execute(UNLOCK_REDIS_SCRIPT, Collections.singletonList(lockKey), requestId);
        return result != null && result == 1;
    }
}
```

---

## 八、核心总结
1. **Lua 脚本是 Redis 实现复杂原子操作的核心工具**，解决了客户端多命令执行的竞态问题
2. 核心优势：原子执行、减少网络开销、逻辑封装
3. 高频场景：分布式锁释放、限流、条件执行、批量关系链操作
4. 关键避坑：脚本必须简短高效、key 必须通过 `KEYS` 传递、禁止使用不确定函数
5. Java 中通过 `DefaultRedisScript` 封装脚本，复用性和可维护性更高

