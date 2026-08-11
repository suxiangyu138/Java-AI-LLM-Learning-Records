# 06-Redis 阻塞与数据故障
> 大 key、阻塞命令、持久化阻塞、主从切换——"Redis 变慢与丢数据"的现象与解决

## 📚 目录
1. [故障一：大 key](#1-故障一大-key)
2. [故障二：阻塞命令与慢命令](#2-故障二阻塞命令与慢命令)
3. [故障三：持久化阻塞](#3-故障三持久化阻塞)
4. [故障四：主从切换与数据丢失](#4-故障四主从切换与数据丢失)
5. [Redis 变慢的排查路径](#5-redis-变慢的排查路径)
6. [核心要点](#6-核心要点)
7. [参考来源](#7-参考来源)

## 1. 故障一：大 key

### 1.1 为什么大 key 危险

```text
大 key（单 key 数据过大）的危害：
  ① 阻塞：读写大 key 耗时（Redis 单线程，阻塞全体）
  ② 网络：大响应占带宽
  ③ 内存：内存暴涨（见 05）
  ④ 删除阻塞：DEL 大 key 也耗时

常见大 key：
  list 存了百万条消息
  hash 单 key 海量字段
  value 是超长字符串（大 JSON/大文件）
```

### 1.2 定位

```bash
# 大 key 扫描（生产慎用——本身会阻塞！低峰期执行）
redis-cli --bigkeys

# 单个 key 大小（8.4+）
redis-cli memory usage "orders:20260808"

# 集合类型长度
redis-cli llen orders:queue
redis-cli hlen user:1001
```

| 命令 | 说明 |
|------|------|
| `--bigkeys` | 全库扫描（低峰期） |
| `memory usage key` | 单 key 内存（8.4+，推荐） |
| `llen/hlen/scard` | 集合长度检查 |

### 1.3 解决

```text
大 key 治理：
  ① 拆分：按业务维度拆（orders:20260808 → 按小时/ID 段）
  ② 分片：hash 分片（user:1001 → user:1001:0..N）
  ③ 结构替换：list → stream；大字符串 → 压缩/存文件
  ④ 删除：用 UNLINK（异步删除，不阻塞）
```

> ⚠️ **删除大 key 用 `UNLINK` 不用 `DEL`**：DEL 同步阻塞（大 key 删除 = 阻塞事故），UNLINK 异步释放（8.4 支持）。

## 2. 故障二：阻塞命令与慢命令

### 2.1 现象

```text
现象：Redis 整体变慢（所有命令延迟升高）
原因：单线程——一个慢命令阻塞全体
```

```bash
# 慢日志（定位慢命令）
redis-cli slowlog get 10
# 每条：id/时间戳/耗时(微秒)/命令+参数

# 配置慢日志阈值
redis-cli CONFIG SET slowlog-log-slower-than 10000   # 10ms
redis-cli CONFIG SET slowlog-max-len 128
```

### 2.2 高危命令清单

| 命令 | 风险 | 替代 |
|------|------|------|
| `KEYS *` | 全库遍历（生产禁用！） | `SCAN` 游标 |
| `SMEMBERS`（大集合） | O(N) 全量 | `SSCAN` |
| `HGETALL`（大 hash） | O(N) 全量 | `HSCAN` |
| `LRANGE 0 -1`（大 list） | O(N) 全量 | 分页 |
| `ZRANGE`（大 zset） | O(N) | 分页 |
| `SORT` | 排序耗时 | 应用侧 |
| `FLUSHALL`/`FLUSHDB` | 清库阻塞 | 慎用 |

> 🎯 **生产铁律**：**`KEYS *` 生产禁用**——用 `SCAN`（游标分批）；大集合读全量用 `SSCAN/HSCAN` 分页。面试问"Redis 变慢"先答慢命令。

## 3. 故障三：持久化阻塞

### 3.1 现象与机制

```text
现象：周期性变慢（与 AOF 重写/RDB 保存时间吻合）
```

```bash
redis-cli info persistence
# rdb_bgsave_in_progress: 1        ← RDB 保存中
# aof_rewrite_in_progress: 1       ← AOF 重写中
# aof_last_bgrewrite_status: ok
```

| 持久化操作 | 阻塞点 | 优化 |
|-----------|--------|------|
| RDB 快照（save 触发） | fork + 写盘 | 用 bgsave（异步）；低峰 |
| RDB fork 瞬间 | **fork 阻塞（大内存耗时更长）** | 内存控制 + 硬件 |
| AOF 重写 | fork + 写盘 | 错峰/调频 |
| AOF fsync（always） | 每写同步（最慢） | 用 everysec |

> 💡 **fork 是隐藏阻塞点**：RDB/AOF 重写的 fork 瞬间会阻塞（内存越大越久）——**大内存实例（>10G）的 fork 可达秒级**，需评估。

## 4. 故障四：主从切换与数据丢失

### 4.1 现象

```text
现象：主从切换后数据丢失 / 哨兵切换期间不可用
```

| 丢失场景 | 原因 | 防护 |
|----------|------|------|
| 主从切换丢数据 | 主库写未同步到从库（异步复制） | `min-replicas-to-write` + 同步等待 |
| 故障期间不可用 | 哨兵切换有检测+选举时间 | 客户端超时/重连 |
| 脑裂 | 分区导致双主 | 哨兵 quorum 合理配置 |

```bash
# 复制状态
redis-cli info replication
# master_link_status: up        ← 主从链路
# slave_repl_offset / master_repl_offset  ← 复制偏移（对比延迟）
```

### 4.2 数据丢失防护

```text
配置（redis.conf）：
  # 主库至少 N 个从库同步才接受写（防异步复制丢数据）
  min-replicas-to-write 1
  min-replicas-max-lag 10

  # 持久化开启（AOF everysec）——宕机少丢 1 秒
  appendonly yes
  appendfsync everysec
```

> ⚠️ **Redis 的丢失边界**：异步复制 + AOF everysec 下，**极端故障可能丢 1 秒 + 未同步数据**——"Redis 只放可容忍丢失的数据"再次强调；要强一致用 DB。

## 5. Redis 变慢的排查路径

```text
Redis 变慢排查五步：
  ① 慢日志：slowlog get 10（慢命令？）
  ② 大 key：memory usage / bigkeys（大 key？）
  ③ 持久化：info persistence（RDB/AOF 重写中？）
  ④ 内存：info memory（淘汰/swap？）
  ⑤ 系统：top/dmesg（fork/IO/CPU 竞争？）
```

```bash
# 综合体检
redis-cli info | grep -E "connected_clients|used_memory|evicted_keys"
redis-cli --stat                    # 实时吞吐统计
```

| 步骤 | 命令 | 结论 |
|------|------|------|
| ① 慢日志 | `slowlog get` | 慢命令清单 |
| ② 大 key | `memory usage` | 大 key 定位 |
| ③ 持久化 | `info persistence` | 重写/保存状态 |
| ④ 内存 | `info memory` | 淘汰/碎片 |
| ⑤ 系统 | `top`/`dmesg` | fork/IO/CPU |

## 6. 核心要点

> 🎯 **核心要点**：
> - 大 key 四害：阻塞/带宽/内存/删除阻塞——拆分治理 + `UNLINK` 删除；
> - 生产禁 `KEYS *`（用 SCAN）；大集合用 SSCAN/HSCAN 分页；
> - 慢日志是变慢的第一证据（slowlog get）；
> - fork 是隐藏阻塞点（大内存实例评估）；
> - 主从丢失防护：min-replicas-to-write + AOF everysec（接受丢失边界）；
> - 变慢五步：慢日志 → 大 key → 持久化 → 内存 → 系统；
> - 强一致数据不放 Redis——这是架构铁律。

## 7. 参考来源

- [Redis 官方文档：延迟问题排查](https://redis.io/docs/latest/operate/oss_and_stack/management/latency/)
- [Redis 官方文档：bigkeys 扫描](https://redis.io/docs/latest/commands/scan/)
- [Redis 复制与持久化文档](https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/)

---

**下一模块**：[07-MQ消息堆积与消费故障](07-MQ消息堆积与消费故障.md)　/　**返回总览**：[00-总览](00-中间件运维故障排查总览.md)
