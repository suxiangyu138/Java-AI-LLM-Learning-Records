# 05-Redis 连接与内存故障
> 连接超时、内存淘汰与 OOM、maxmemory 规划——"Redis 最先遇到的两类问题"

## 📚 目录
1. [故障一：连接超时/拒绝](#1-故障一连接超时拒绝)
2. [故障二：内存淘汰（数据消失）](#2-故障二内存淘汰数据消失)
3. [故障三：OOM/内存暴涨](#3-故障三oom内存暴涨)
4. [内存规划与监控](#4-内存规划与监控)
5. [核心要点](#5-核心要点)
6. [参考来源](#6-参考来源)

## 1. 故障一：连接超时/拒绝

### 1.1 现象与定位

```text
现象：应用报 Connection refused / timed out / "max number of clients reached"
```

```bash
redis-cli info clients
# connected_clients: 10000    ← 当前连接
# maxclients: 10000           ← 上限

# 连接状态
redis-cli client list | wc -l
redis-cli client list | grep -c "idle"     # 空闲连接

# 服务端连接数
ss -tnp | grep 6379 | wc -l
```

### 1.2 根因矩阵

| 根因 | 特征 | 解决 |
|------|------|------|
| **连接泄漏** | connected_clients 爬升不回 | 应用侧（Jedis/Lettuce 池未归还） |
| 连接池过大 | 峰值超 maxclients | 池参数合理化 |
| 阻塞命令 | 请求堆积占连接 | 见 [06](06-Redis阻塞与数据故障.md) |
| 网络/防火墙 | 超时非拒绝 | 网络排查体系 |
| 大流量突发 | 曲线陡升 | 扩容/限流 |

> 🎯 **Redis 连接排查与 MySQL 同构**：**连接数爬升 = 泄漏嫌疑**——先看 connected_clients 趋势，再查应用连接池（与 [02](02-MySQL连接与性能故障.md) 故障四同理）。

## 2. 故障二：内存淘汰（数据消失）

### 2.1 现象与机制

```text
现象：某些 key 神秘消失 / 命中率骤降 / 业务数据缺失
原因：内存达到 maxmemory → 按淘汰策略驱逐

淘汰策略（maxmemory-policy）：
  noeviction：不淘汰，写报错（默认）
  allkeys-lru：所有 key 按 LRU 淘汰
  volatile-lru：仅有过期时间的按 LRU
  allkeys-lfu：按 LFU（访问频率）
  allkeys-random / volatile-random：随机
  volatile-ttl：即将过期的先淘汰
```

```bash
redis-cli info memory
# used_memory_human: 1.2G
# maxmemory_human: 1.0G
# maxmemory_policy: allkeys-lru
# evicted_keys: 12345        ← 被淘汰数量（>0 = 正在淘汰！）
```

### 2.2 解决

| 手段 | 说明 |
|------|------|
| 扩容内存 | 治标（立即） |
| 检查大 key/无界 key | 缓存 key 无限增长（见 06 大 key） |
| 淘汰策略评估 | 业务能容忍淘汰吗？（缓存可以，数据不行） |
| 数据 vs 缓存分离 | 重要数据用 DB，Redis 只放缓存 |

> ⚠️ **淘汰 ≠ 删除问题**：`evicted_keys` 增长说明**内存不够用**——被淘汰的可能是热点数据（连锁命中率下降）；**Redis 只放可容忍丢失的数据**是铁律。

## 3. 故障三：OOM/内存暴涨

### 3.1 现象与定位

```text
现象：进程被杀（OOM）/系统内存耗尽/swap 抖动
检查：dmesg | grep -i oom；redis-cli info memory
```

```bash
# 内存构成分析
redis-cli info memory | grep -E "used_memory|used_memory_rss"
# used_memory：数据占用
# used_memory_rss：实际物理内存（含碎片）

# 找出内存大头
redis-cli --bigkeys                    # 大 key 扫描（生产慎用，慢）
redis-cli memory usage key名           # 单个 key 内存（8.4+）
```

### 3.2 内存暴涨的常见元凶

| 元凶 | 特征 | 解决 |
|------|------|------|
| **大 key** | 单个 key 巨大（list/hash 海量元素） | 拆分/分片 |
| **无界 key** | 缓存 key 永不过期持续增长 | TTL 规范 |
| **内存碎片** | rss 远大于 used | `memory purge`（8.4+）/重启 |
| **管道/批量写** | 短时大量写入 | 限流/错峰 |

> 🎯 **内存排查主线**：used vs maxmemory（够不够）→ rss vs used（碎片？）→ bigkeys（大头在哪）→ TTL 检查（无界增长？）——**四条线定位内存去向**。

## 4. 内存规划与监控

### 4.1 内存规划

```text
Redis 内存预算：
  maxmemory = 系统内存 × 60-70%（预留系统/碎片/峰值）
  例：8G 机器 → maxmemory 5G

配置（redis.conf）：
  maxmemory 5gb
  maxmemory-policy allkeys-lru
  maxmemory-samples 5
```

### 4.2 监控指标

| 指标 | 告警阈值 | 说明 |
|------|---------|------|
| used_memory / maxmemory | > 80% | 内存压力 |
| evicted_keys | > 0 持续 | 淘汰发生（紧急） |
| connected_clients | > 80% maxclients | 连接压力 |
| 命中率（hits/(hits+misses)） | < 80% | 缓存效率 |

> 💡 **Redis 监控三件套**：内存水位、淘汰数、命中率——与[日志监控指标](../日志监控指标/00-日志监控指标总览.md)体系联动（Prometheus 采集 redis_exporter）。

## 5. 核心要点

> 🎯 **核心要点**：
> - 连接故障：connected_clients 趋势 + maxclients——爬升 = 泄漏；
> - 淘汰是信号不是原因：evicted_keys > 0 = 内存不够——扩容 + 查无界 key + 数据缓存分离；
> - 内存暴涨四查：used vs max → rss vs used（碎片）→ bigkeys → TTL 规范；
> - 内存规划：maxmemory = 内存 60-70%，淘汰策略按业务（缓存用 LRU）；
> - 监控三件套：内存水位 >80% / 淘汰数 / 命中率 <80%；
> - **Redis 只放可容忍丢失的数据**是架构铁律。

## 6. 参考来源

- [Redis 官方文档：内存管理](https://redis.io/docs/latest/operate/oss_and_stack/management/memory-optimization/)
- [Redis 官方文档：缓存淘汰](https://redis.io/docs/latest/reference/eviction/)
- [Redis INFO 命令文档](https://redis.io/docs/latest/commands/info/)

---

**下一模块**：[06-Redis阻塞与数据故障](06-Redis阻塞与数据故障.md)　/　**返回总览**：[00-总览](00-中间件运维故障排查总览.md)
