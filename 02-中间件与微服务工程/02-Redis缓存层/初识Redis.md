# 初识 Redis

> **定位**：Redis（Remote Dictionary Server）是开源内存数据结构存储系统，以键值对为核心，数据优先存内存（微秒级响应），支持持久化。是分布式系统不可或缺的核心中间件。

---

## 目录

1. [核心概念](#1-核心概念)
2. [核心特性](#2-核心特性)
3. [安装与基础操作](#3-安装与基础操作)
4. [实战要点](#4-实战要点)
5. [避坑总结](#5-避坑总结)

---

## 1. 核心概念

| 维度 | 说明 |
|------|------|
| 全称 | Remote Dictionary Server |
| 存储模型 | 键值对（Key-Value），NoSQL |
| 核心优势 | 内存优先（微秒级），支持持久化 |
| 适用场景 | 分布式环境（单机直接用变量更高效） |

## 2. 核心特性

| 特性 | 说明 |
|------|------|
| **内存优先** | 单机 10 万+ QPS，远超磁盘数据库 |
| **丰富数据结构** | String/Hash/List/Set/ZSet/Bitmap/HyperLogLog/GEO/Stream |
| **持久化** | RDB（快照）+ AOF（日志） |
| **原子性** | 所有单命令原子，支持事务和 Lua 脚本 |
| **高可用** | 主从复制 + 哨兵模式 + Redis Cluster |
| **多语言** | Java/Python/Go/PHP 等全支持 |

### 数据结构速查

| 类型 | 场景 |
|------|------|
| String | 缓存、计数、分布式锁 |
| Hash | 存储对象（用户信息） |
| List | 队列、栈、消息队列 |
| Set | 去重、标签、共同好友 |
| ZSet | 排行榜、延迟队列 |
| Bitmap | 签到、在线状态 |
| HyperLogLog | UV 统计（12KB 存千万级） |
| GEO | 附近的人/门店 |

---

## 3. 安装与基础操作

### 安装方式

| 平台 | 命令 |
|------|------|
| Ubuntu | `sudo apt install redis-server -y` |
| CentOS | `sudo yum install epel-release && sudo yum install redis -y` |
| macOS | `brew install redis` |
| Docker | `docker run -d --name redis -p 6379:6379 redis` |

### 基础命令

```bash
redis-cli
> ping                    # PONG → 服务正常
> SET name "Tom"          # 设置键值
> GET name                # 获取值 → "Tom"
> EXPIRE name 60          # 60 秒后过期
> TTL name                # 查看剩余时间
> DEL name                # 删除
```

### 安全配置

| 措施 | 说明 |
|------|------|
| `requirepass` | 设置密码 |
| `bind 127.0.0.1` | 限制监听接口 |
| 防火墙 | 限制 6379 端口访问 |

---

## 4. 实战要点

| 场景 | 实现 | 数据结构 |
|------|------|----------|
| **缓存** | 热点数据存 Redis，减少 DB 压力 | String/Hash |
| **会话存储** | 分布式 Session 共享 | String/Hash |
| **排行榜** | 实时排名 | ZSet |
| **计数器** | 阅读量/点赞数 | String（INCR） |
| **消息队列** | 简单异步任务 | List（LPUSH/BRPOP） |
| **分布式锁** | 并发互斥 | String（SET NX EX） |
| **实时分析** | UV 统计 | HyperLogLog |
| **地理位置** | 附近商家 | GEO |

---

## 5. 避坑总结

| 坑点 | 正确做法 |
|------|----------|
| 内存溢出 | 设 `maxmemory` + 淘汰策略（`allkeys-lru`） |
| 数据丢失 | 组合 RDB + AOF |
| 慢命令阻塞 | 禁用 `KEYS`/`FLUSHALL`，用 `SCAN` |
| 无密码暴露 | 设置密码 + 限制监听接口 |
| 单机滥用 | 分布式场景才用 Redis，单机直接用变量 |
