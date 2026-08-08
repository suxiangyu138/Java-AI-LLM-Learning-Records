# Lua 语言基础
> Redis 内嵌 Lua 5.1 的速成课：类型系统、table、函数、控制流、字符串与模块——只讲写 Redis 脚本用得上的部分，半小时上手

## 📚 目录
1. [Lua 是什么：5.1 的定位](#1-lua-是什么51-的定位)
2. [类型系统](#2-类型系统)
3. [变量与作用域](#3-变量与作用域)
4. [table：一切皆 table](#4-table一切皆-table)
5. [函数与闭包](#5-函数与闭包)
6. [控制流](#6-控制流)
7. [字符串与常用标准库](#7-字符串与常用标准库)
8. [写 Redis 脚本的 Lua 速查](#8-写-redis-脚本的-lua-速查)

## 1. Lua 是什么：5.1 的定位

| 事实 | 说明 |
|------|------|
| 版本 | **Lua 5.1**（Redis 内嵌版本，2026 源码仍为 5.1，见 [总览](00-Lua脚本总览.md)） |
| 设计哲学 | 极简：约 20 个关键字，一张语法表 |
| 与 Redis 的结合 | Redis 把 Lua 解释器嵌入进程（deps/lua），EVAL 传脚本即执行 |
| 定位 | 嵌入式脚本语言（游戏配置/Redis 脚本/Wireshark） |

> 🎯 学习心态：**别按"一门完整语言"学 Lua**——写 Redis 脚本只用到：table、函数、字符串操作、if/for、redis.call。30 分钟即可上手。

## 2. 类型系统

```lua
-- 八种类型：nil / boolean / number / string / table / function / userdata / thread
local a = nil          -- 未定义即 nil（区分"空"与"不存在"）
local b = true         -- boolean
local c = 42           -- number（统一双精度，无整数/浮点之分）
local d = "hello"      -- string
local e = {1, 2, 3}    -- table（数组+字典二合一）
local f = function() end  -- function

print(type(c))   --> "number"
print(1 == "1")  --> false（数字与字符串不自动相等）
```

| 类型 | Redis 脚本注意点 |
|------|-----------------|
| nil | 判断键不存在（`redis.call('GET', k) == false` 或 nil） |
| number | Redis 返回的整数是 number |
| string | 与 Redis 交互的主要类型 |
| table | 数组从 **1 开始**（不是 0！） |

> ⚠️ **索引从 1 开始是最大新手坑**：`local t = {"a","b"}` 中 `t[1]` 是 `"a"`，`t[0]` 是 nil。写 Redis 脚本时 KEYS[1]/ARGV[1] 同样是 1 起始。

## 3. 变量与作用域

```lua
-- local：局部变量（推荐，Redis 7.0+ 沙箱强制！）
local count = 10

-- 全局变量（不写 local）
count = 10   -- 7.0+ 沙箱中会报错：Attempt to modify a readonly table

-- 2026 重要变化：Redis 7.0+ 沙箱加固后【禁止全局函数/变量】！
-- 所有变量、函数必须 local（详见 03 §5 沙箱）
local function myHelper()
    return 42
end
```

> ⚠️ **全局变量在 Redis 7.0+ 已不可用**（安全加固，CVE 修复）：脚本里一切变量/函数必须 `local` 声明——老教程里的全局写法在新版本直接报错，迁移注意（详见 [03-原子性与执行语义](03-原子性与执行语义.md) §5）。

## 4. table：一切皆 table

```lua
-- 数组部分（索引从 1 开始）
local arr = {"a", "b", "c"}
print(arr[1])        --> a
print(#arr)          --> 3（长度运算符）

-- 字典部分
local map = {name = "redis", version = 8}
print(map.name)      --> redis
print(map["version"]) --> 8

-- 混合
local mixed = {"x", key = "y"}
print(mixed[1], mixed.key)  --> x  y

-- 遍历
for i, v in ipairs(arr) do print(i, v) end       -- 数组：1 a / 2 b / 3 c
for k, v in pairs(map) do print(k, v) end        -- 字典：无序
```

| 函数 | 用途 |
|------|------|
| `#t` | 数组长度（空 table 为 0） |
| `table.insert(t, v)` | 尾部插入 |
| `table.remove(t, i)` | 删除并返回 |
| `ipairs()` | 按序遍历数组（1..n） |
| `pairs()` | 遍历全部键值（无序） |

> 💡 与 Redis 的对应：`redis.call('HGETALL', k)` 返回的数组 table 就是 `{field1, val1, field2, val2, ...}`——用 `for i = 1, #t, 2 do` 成对读取。

## 5. 函数与闭包

```lua
-- 定义（local 必须，见 §3）
local function add(a, b)
    return a + b
end

-- 匿名函数（Redis 脚本常用：注册 Function 时）
local f = function(x) return x * 2 end

-- 多返回值
local function stats()
    return 1, 2
end
local x, y = stats()

-- 闭包：函数捕获外部局部变量
local counter = 0
local inc = function()
    counter = counter + 1        -- 捕获 counter（每个脚本实例独立）
    return counter
end
```

> 🎯 Redis 脚本里的函数用法：**把公共逻辑抽成 local 函数**（如"校验并扣减"），一个 EVAL 内多次调用——Lua 脚本内代码可复用，但脚本之间不能互相调用（这是 Function 的改进点，见 [05](05-Redis-Function机制.md)）。

## 6. 控制流

```lua
-- if / elseif / else
local score = redis.call('GET', KEYS[1])
if score == false then
    return -1                          -- 键不存在
elseif tonumber(score) > 100 then
    return -2                          -- 超限
else
    return redis.call('INCR', KEYS[1]) -- 正常
end

-- 数值 for（含首不含尾）
for i = 1, 5 do print(i) end           -- 1 2 3 4 5

-- 泛型 for（遍历 table）
for k, v in pairs(map) do ... end

-- while
local n = 0
while n < 10 do
    n = n + 1
end

-- repeat until
repeat
    n = n - 1
until n == 0
```

| 控制流 | Redis 场景 |
|--------|-----------|
| if/elseif/else | 条件分支（核心） |
| for i = a, b | 固定次数（限流窗口遍历等） |
| for k, v in pairs/ipairs | 遍历哈希/列表 |
| while/repeat | 循环（⚠️ 注意脚本执行时间限制，见 03 §5） |

> ⚠️ 无限循环是脚本最大杀手：`lua-time-limit`（默认 5 秒）触发后脚本被强制终止（详见 [03 §5](03-原子性与执行语义.md)）——**循环必须有退出条件，且别遍历超大集合**。

## 7. 字符串与常用标准库

```lua
-- 字符串拼接（.. 运算符，不是 +）
local id = "user:" .. 42        --> user:42

-- 类型转换
tonumber("42")                  --> 42
tostring(42)                    --> "42"

-- string 库（常用）
string.format("key:%s:%d", "order", 7)   --> key:order:7
string.len("redis")                       --> 5
string.sub("hello", 1, 3)                 --> hel
string.lower / string.upper
string.match("v1.2.3", "v(%d+)")          --> 1（捕获）

-- table 库
table.insert / table.remove / #t
```

| Redis 脚本可用库 | 说明 |
|-----------------|------|
| `string` | 字符串处理（最常用） |
| `table` | table 操作 |
| `math` | 数学（限流算法用：math.floor/math.abs） |
| `cjson` | **JSON 编解码**（Redis 内置！`cjson.encode`/`cjson.decode`） |
| `cmsgpack` | MessagePack 编解码 |
| `redis` | Redis 专属（call/pcall/log/status_reply） |
| 其余库 | **不可用**（无 io/os/网络等，见 03 §5 沙箱） |

```lua
-- cjson 实战：脚本内处理 JSON 结构（如限流计数器存储）
local data = cjson.decode(redis.call('GET', KEYS[1]))
data.count = data.count + 1
redis.call('SET', KEYS[1], cjson.encode(data))
```

> 🎯 **cjson 是 Redis 脚本的"秘密武器"**：很多"脚本做不到"其实只是不知道 cjson——在脚本内编解码 JSON，配合 HASH 可做出复杂的业务结构。

## 8. 写 Redis 脚本的 Lua 速查

```lua
-- 一个完整的 Redis 脚本骨架（覆盖 80% 场景）
-- 用法：EVAL script 2 key1 key2 arg1 arg2
-- 注意：KEYS 必须在脚本外传（不能写死在脚本里，集群模式强制！）

-- 1. 获取键与参数
local key = KEYS[1]
local limit = tonumber(ARGV[1])

-- 2. 读取 + 判断
local current = tonumber(redis.call('GET', key) or '0')
if current >= limit then
    return 0                       -- 拒绝
end

-- 3. 修改 + 返回
redis.call('INCR', key)
return limit - current             -- 剩余额度
```

| 速记规则 | 说明 |
|---------|------|
| 一切变量 local | 7.0+ 沙箱强制 |
| 键必须走 KEYS[i] | 集群槽位计算依赖它（硬性要求） |
| 值走 ARGV[i] | 与键分离，防止键名被拼进脚本 |
| 返回类型 | 数字/字符串/table（自动转 Redis 类型）；false → nil |
| 错误用 return redis.error_reply("msg") | 返回错误信息给客户端 |

---

**下一模块**：[02-脚本机制与命令族](02-脚本机制与命令族.md) / **返回总览**：[00-Lua脚本总览](00-Lua脚本总览.md)
