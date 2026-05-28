# 第三章 Redis 常用命令

## 3.1 Redis 命令基础说明
1. 命令不区分大小写，`SET` 与 `set` 等效；
2. 默认数据库：**16 个数据库**，编号 0~15，默认使用 `0` 号库；
3. 核心通用操作：切换库、清空库、键管理、过期设置。

---

## 3.2 通用 Key 命令（所有数据类型通用）
```bash
# 切换数据库
select 1

# 查看当前库所有 key
keys *

# 查看 key 是否存在
exists key_name

# 删除指定 key
del key_name

# 设置 key 过期时间（秒）
expire key_name 60

# 查看 key 剩余过期时间
ttl key_name

# 取消过期，永久有效
persist key_name

# 查看 key 对应数据类型
type key_name

# 清空当前数据库
flushdb

# 清空所有数据库
flushall
```

---

## 3.3 String 字符串类型（最常用）

### 3.3.1 基础增删查改
```bash
# 设置键值
set k1 v1

# 获取值
get k1

# 不存在则设置，存在不覆盖（分布式锁常用）
setnx k1 v1

# 设置值 + 过期时间（秒）
setex k2 30 abc

# 批量设置
mset k3 v3 k4 v4

# 批量获取
mget k3 k4
```

### 3.3.2 数字原子操作
```bash
# 数值自增 1
incr count

# 数值自减 1
decr count

# 指定步长自增
incrby count 10
```

---

## 3.4 Hash 哈希类型（适合存储对象）
适合存储**用户、商品**等结构化对象，节省内存。
```bash
# 设置单个 field
hset user:1 name zhangsan

# 设置多个 field
hset user:1 age 20 sex male

# 获取单个 field
hget user:1 name

# 获取全部 field+value
hgetall user:1

# 获取所有字段名
hkeys user:1

# 获取所有字段值
hvals user:1

# 删除指定字段
hdel user:1 sex
```

---

## 3.5 List 列表类型（有序、可重复）
底层双向链表，**左进右出**，适合做简易队列、消息栈。
```bash
# 左侧添加元素
lpush list1 a b c

# 右侧添加元素
rpush list1 d e

# 范围查询 0:-1 查询全部
lrange list1 0 -1

# 左侧弹出元素
lpop list1

# 右侧弹出元素
rpop list1

# 获取列表长度
llen list1
```

---

## 3.6 Set 集合类型（无序、不可重复）
自动去重，适合**点赞、收藏、共同好友**场景。
```bash
# 添加集合元素
sadd set1 1 2 3 3

# 查看所有元素
smembers set1

# 删除指定元素
srem set1 2

# 判断元素是否在集合中
sismember set1 1

# 集合交集（共同好友）
sinter set1 set2

# 集合并集
sunion set1 set2
```

---

## 3.7 ZSet 有序集合（有序、不可重复）
每个元素带 **score 分值**，按分值排序，用于排行榜、积分排名。
```bash
# 添加元素 + 分值
zadd rank 100 a 90 b 80 c

# 正序查询（从小到大）
zrange rank 0 -1

# 倒序查询（从大到小，排行榜常用）
zrevrange rank 0 -1

# 删除元素
zrem rank a

# 查询元素排名
zrank rank b
```

---

## 3.8 本章小结
1. **通用命令**：统一管理 Key 的生命周期、过期、删除、库操作；
2. **String**：缓存、计数器、验证码、Token 存储；
3. **Hash**：单行对象存储，优化对象缓存结构；
4. **List**：简易消息队列、时序数据；
5. **Set**：去重业务、交集对比；
6. **ZSet**：各类排行榜、有序延时任务。
