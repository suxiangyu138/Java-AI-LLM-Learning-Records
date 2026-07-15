# Redis 的 Lua 脚本编程详解

> **定位**：Redis 从 2.6 开始原生支持 Lua 脚本——将多个命令封装为原子执行的脚本，减少网络往返、保证复杂逻辑原子性。

---

## 目录

1. [核心价值](#1-核心价值)
2. [基础语法](#2-基础语法)
3. [实操案例](#3-实操案例)
4. [注意事项](#4-注意事项)

---

## 1. 核心价值

| 价值 | 说明 |
|------|------|
| **减少网络开销** | 一次请求执行多个命令 |
| **原子性** | 整个脚本不被其他命令打断 |
| **简化开发** | 支持条件判断、循环、函数 |

---

## 2. 基础语法

### Redis API 调用

```lua
redis.call('SET', KEYS[1], ARGV[1])   -- 失败抛异常，中断脚本
redis.pcall('SET', KEYS[1], ARGV[1])  -- 失败返回错误，脚本继续

-- KEYS[n]：第 n 个键参数
-- ARGV[n]：第 n 个普通参数
```

### 执行命令

| 命令 | 说明 |
|------|------|
| `EVAL script numkeys key... arg...` | 直接执行脚本 |
| `SCRIPT LOAD script` | 加载脚本，返回 SHA |
| `EVALSHA sha numkeys key... arg...` | 通过 SHA 执行（免传脚本） |
| `SCRIPT EXISTS sha` | 判断脚本是否已加载 |
| `SCRIPT FLUSH` | 清空所有脚本 |
| `SCRIPT KILL` | 终止正在执行的脚本 |

---

## 3. 实操案例

### 案例 1：原子性库存扣减

```lua
local stock = tonumber(redis.call('GET', KEYS[1]))
local num = tonumber(ARGV[1])
if not stock then return -1 end        -- 键不存在
if stock < num then return 0 end       -- 库存不足
redis.call('DECRBY', KEYS[1], num)
return tonumber(redis.call('GET', KEYS[1]))
```

### 案例 2：点赞去重

```lua
local isLiked = redis.call('SISMEMBER', KEYS[2], ARGV[1])
if isLiked == 1 then return 2 end      -- 已点赞
redis.call('INCR', KEYS[1])            -- 点赞数 +1
redis.call('SADD', KEYS[2], ARGV[1])   -- 记录用户
return 1
```

### 案例 3：分布式锁释放

```lua
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
```

---

## 4. 注意事项

| 注意点 | 说明 |
|--------|------|
| **避免脚本过长** | 单线程执行，超过 100ms 会阻塞 Redis |
| **变量加 local** | 避免全局变量污染 |
| **禁止耗时操作** | 不能 sleep、循环等待、访问外部资源 |
| **错误处理** | 根据需求选 `redis.call()`（中断）或 `redis.pcall()`（继续） |
| **脚本复用** | `SCRIPT LOAD` + `EVALSHA` 减少网络开销 |
