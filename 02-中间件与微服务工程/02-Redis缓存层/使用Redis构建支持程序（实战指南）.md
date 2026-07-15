# 使用 Redis 构建支持程序（实战指南）

> **定位**：支持程序不直接面向用户，而是为核心业务提供辅助支撑——缓存预热、数据同步、分布式锁服务、监控告警。

---

## 目录

1. [缓存预热支持程序](#1-缓存预热支持程序)
2. [数据同步支持程序](#2-数据同步支持程序)
3. [分布式锁服务](#3-分布式锁服务)
4. [监控告警支持程序](#4-监控告警支持程序)
5. [通用实现步骤](#5-通用实现步骤)

---

## 1. 缓存预热支持程序

```java
// 启动时批量加载热点数据到 Redis
List<Map<String, Object>> hotProducts = jdbcTemplate.queryForList(
    "SELECT id, name, price, stock FROM product WHERE sales > 1000 LIMIT 100");
for (Map<String, Object> product : hotProducts) {
    redisTemplate.opsForValue().set("product:info:" + product.get("id"),
        product, 30, TimeUnit.MINUTES);
}
```

| 注意点 | 说明 |
|--------|------|
| 预热时机 | 启动后自动执行 / 每日凌晨定时 |
| 分批加载 | 避免一次性预热过多导致内存飙升 |
| 失败重试 | 重试 3 次，避免单次失败中断 |

---

## 2. 数据同步支持程序

> 核心：Canal 监听 MySQL binlog → 解析变更 → 同步更新 Redis。

```text
MySQL binlog → Canal 解析 → INSERT/UPDATE → Redis 写入
                           → DELETE → Redis 删除
```

| 注意点 | 说明 |
|--------|------|
| 幂等性 | 多次执行同一操作结果一致 |
| 同步延迟 | 毫秒级，主程序需容忍短暂不一致 |
| 隔离部署 | 同步程序独立部署，不影响主程序 |

---

## 3. 分布式锁服务

> 封装统一锁服务接口，供主程序调用。

```java
// 获取锁
String lockValue = UUID.randomUUID().toString();
Boolean success = redisTemplate.opsForValue()
    .setIfAbsent(lockKey, lockValue, 30, TimeUnit.SECONDS);

// 释放锁（Lua 脚本原子校验+删除）
// 续租：耗时业务定期续租
```

---

## 4. 监控告警支持程序

```java
// 定期 INFO 获取指标 → 对比阈值 → 触发告警
Properties info = connection.info();
long usedMemory = Long.parseLong(info.getProperty("used_memory"));
double memoryUsage = (double) usedMemory / maxMemory;
if (memoryUsage > 0.8) sendAlarm("内存使用率过高");

int connectedClients = Integer.parseInt(info.getProperty("connected_clients"));
if (connectedClients > 1000) sendAlarm("连接数过高");
```

---

## 5. 通用实现步骤

```text
环境准备 → 核心功能开发 → 测试验证 → 部署运维
```

| 原则 | 说明 |
|------|------|
| **独立性** | 与主程序分开部署，异常不影响主程序 |
| **性能可控** | 控制操作频率和数据量 |
| **健壮性** | 异常处理 + 重试 + 日志 + 幂等 |
| **可扩展** | 预留监控指标/数据表/锁类型扩展 |
