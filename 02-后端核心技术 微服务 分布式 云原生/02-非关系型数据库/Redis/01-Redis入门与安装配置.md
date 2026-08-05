# Redis 入门与安装配置
> Redis（Remote Dictionary Server）是开源内存 KV 存储系统，单机 QPS 10 万+，支持丰富数据结构、持久化、高可用，是分布式系统核心中间件。

## 目录
1. [什么是 Redis](#1-什么是-redis)
2. [核心特性](#2-核心特性)
3. [Redis vs MySQL](#3-redis-vs-mysql)
4. [安装指南](#4-安装指南)
5. [redis.conf 核心配置](#5-redisconf-核心配置)
6. [redis-cli 基础操作](#6-redis-cli-基础操作)
7. [安全配置](#7-安全配置)
8. [学习资源](#8-学习资源)

---

## 1. 什么是 Redis

### 1.1 基本概念

| 项目 | 说明 |
|------|------|
| 全称 | Remote Dictionary Server（远程字典服务器） |
| 作者 | Salvatore Sanfilippo（Antirez） |
| 开发语言 | ANSI C |
| 模型 | 单线程事件驱动 + 内存存储 |
| 协议 | RESP（REdis Serialization Protocol） |
| 定位 | 不是普通缓存，是数据结构服务器 |

Redis 将数据存储在内存中，通过异步 IO 多路复用实现高吞吐。单机 QPS 可达 10 万+，读写延迟通常在微秒级。

### 1.2 核心定位

```
┌──────────────────────────────────────────┐
│              应用层                       │
│         (Spring Boot / Go / Python)       │
└──────────────────┬───────────────────────┘
                   │
┌──────────────────▼───────────────────────┐
│              Redis 层                     │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐       │
│  │缓存 │ │锁   │ │队列 │ │计数器│       │
│  └─────┘ └─────┘ └─────┘ └─────┘       │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐       │
│  │会话 │ │排行 │ │GEO  │ │Stream│       │
│  └─────┘ └─────┘ └─────┘ └─────┘       │
└──────────────────┬───────────────────────┘
                   │
┌──────────────────▼───────────────────────┐
│             持久化层                      │
│    RDB 快照  +  AOF 日志  +  混合持久化    │
└──────────────────────────────────────────┘
```

> 💡 Redis 不仅仅是一个缓存，它是一个基于内存的数据结构服务器，可以作为数据库、缓存和消息中间件使用。

### 1.3 企业应用场景

| 场景 | 说明 | 典型实现 |
|------|------|----------|
| 热点数据缓存 | 商品详情、用户信息、配置数据 | String / Hash |
| 分布式锁 | 秒杀、库存扣减、幂等控制 | SETNX + Lua |
| 计数器 | 点赞数、浏览量、限流计数 | INCR / DECR |
| 排行榜 | 积分排名、热榜、实时榜单 | ZSet |
| 消息队列 | 异步任务、延迟消息、日志收集 | List / Stream |
| 会话存储 | 分布式 Session 共享 | String / Hash |
| 地理位置 | 附近的人、门店推荐 | GEO |
| 布隆过滤器 | 缓存穿透防护、推荐去重 | Bloom Filter 模块 |
| 位图统计 | 用户签到、DAU/MAU 统计 | Bitmap |
| UV 统计 | 海量独立访客计数 | HyperLogLog |

### 1.4 Redis 历史版本

| 版本 | 发布时间 | 核心特性 |
|------|----------|----------|
| 1.0 | 2009 | 基础 KV 存储 |
| 2.0 | 2010 | 主从复制、Redis Sentinel 雏形 |
| 2.6 | 2012 | Lua 脚本支持 |
| 2.8 | 2013 | Sentinel 正式发布 |
| 3.0 | 2015 | Redis Cluster 分布式集群 |
| 3.2 | 2016 | GEO 地理空间支持 |
| 4.0 | 2017 | 混合持久化、模块系统 |
| 5.0 | 2018 | Stream 消息流、动态配置 |
| 6.0 | 2020 | 多线程 IO、RESP3、SSL、ACL |
| 7.0 | 2022 | RediSearch 2.0、listpack 替代 ziplist、shutdown 改进 |
| 7.2 | 2023 | 性能优化、自动故障转移增强 |
| 7.4 | 2024 | 最新稳定版 |

> 💡 生产环境建议使用 6.x 或 7.x 版本。6.0 引入了多线程 IO 和 ACL 访问控制，7.0 优化了底层数据结构。

---

## 2. 核心特性

### 2.1 特性总览

| 特性 | 说明 |
|------|------|
| 高性能 | 纯内存 + 单线程 + IO 多路复用，QPS 10 万+，延迟微秒级 |
| 丰富数据结构 | String / Hash / List / Set / ZSet + Bitmap / HyperLogLog / GEO / Stream |
| 持久化 | RDB 快照 + AOF 日志 + 混合持久化（Redis 4.0+） |
| 原子性 | 单命令原子执行，支持事务（MULTI/EXEC）和 Lua 脚本 |
| 高可用 | 主从复制 + Sentinel 哨兵 + Cluster 集群 |
| 多语言客户端 | Java（Jedis / Lettuce / Redisson）、Python、Go、Node.js 等 |
| 发布订阅 | PUB / SUB 机制，支持消息广播 |
| Lua 脚本 | 服务端脚本执行，保证原子性和逻辑复用 |
| 管道 | Pipeline 批量发送命令，减少网络 RTT |
| 内存淘汰 | 8 种淘汰策略（LRU / LFU / Random / TTL） |
| ACL（6.0+） | 细粒度用户权限控制 |
| SSL/TLS（6.0+） | 加密通信 |

### 2.2 IO 多路复用模型

```text
Redis 单线程模型：
                    网络 IO 读/写
                    ┌──────────┐
    客户端 1 ───────→│          │
    客户端 2 ───────→│  epoll   │──→ 命令解析 ──→ 命令执行 ──→ 返回结果
    客户端 3 ───────→│          │
                    └──────────┘
                    IO 多路复用
                    （单线程处理）
```

> 💡 Redis 6.0 引入了多线程 IO，但仅限于处理网络数据的读写，命令执行仍然是单线程的。这样做既提高了网络吞吐，又保证了命令执行的原子性。

### 2.3 为什么单线程还这么快

| 原因 | 说明 |
|------|------|
| 纯内存访问 | 内存响应时间约 100ns，比磁盘快几个数量级 |
| 单线程无锁竞争 | 避免上下文切换和锁竞争开销 |
| IO 多路复用 | epoll 机制高效处理大量并发连接 |
| 高效数据结构 | SDS、跳表、ziplist、intset 等精心优化的结构 |
| 数据结构简单 | KV 模型比关系型数据库查询更高效 |

---

## 3. Redis vs MySQL

### 3.1 全面对比

| 维度 | Redis | MySQL |
|------|-------|-------|
| 存储介质 | 内存为主（可持久化到磁盘） | 磁盘为主（内存 Buffer Pool 加速） |
| 数据结构 | KV 非关系型，丰富的数据类型 | 表结构关系型（行+列） |
| 读写性能 | 10 万+ QPS，微秒级延迟 | 约 1 万 QPS（简单查询），毫秒级延迟 |
| 存储容量 | 受物理内存限制 | 磁盘海量存储（TB 级） |
| 事务 | 弱事务（不支持回滚），保证原子性 | ACID 事务，支持回滚 |
| 查询能力 | 基于 Key 的简单操作 | 复杂 SQL 查询、JOIN、子查询 |
| 索引 | 无（Key 本身是索引） | B+ 树索引、全文索引等 |
| 扩展性 | Cluster 扩展，一致性哈希 | 读写分离、分库分表 |
| 持久化 | RDB / AOF 持久化 | 数据写入磁盘 |
| 典型场景 | 缓存、中间件、实时计算 | 持久化存储、复杂查询 |

### 3.2 缓存 + 数据库双写架构

```text
             ┌──────────────┐
             │   客户端      │
             └──┬───────┬───┘
                │       │
        先查缓存 │       │ 未命中查库
                ▼       ▼
         ┌──────────┐───────┐
         │  Redis    │      │  MySQL  │
         │  缓存层   │←──────│  存储层 │
         └──────────┘       └─────────┘
                │               │
                └───────┬───────┘
                        │ 写入后更新缓存
                        ▼
                ┌──────────────┐
                │ 缓存与DB一致 │
                └──────────────┘
```

> 💡 Redis + MySQL 是互联网架构的黄金组合，Redis 扛高并发读，MySQL 做可靠存储。但需注意缓存一致性、缓存穿透、缓存雪崩等问题。

---

## 4. 安装指南

### 4.1 Linux (Ubuntu / Debian)

```bash
# APT 安装
sudo apt update
sudo apt install redis-server -y

# 启停管理
sudo systemctl enable redis-server
sudo systemctl start redis-server
sudo systemctl status redis-server

# 验证
redis-cli ping
# 输出: PONG
```

### 4.2 Linux (CentOS / RHEL)

```bash
# EPEL 源安装
sudo yum install epel-release -y
sudo yum install redis -y

# 启停管理
sudo systemctl start redis
sudo systemctl enable redis

# 配置文件路径
/etc/redis.conf

# 验证
redis-cli ping
```

### 4.3 macOS

```bash
# Homebrew 安装
brew install redis

# 启动服务（后台运行）
brew services start redis

# 前台运行
redis-server

# 停止
brew services stop redis

# 验证
redis-cli ping
```

### 4.4 Windows (WSL2)

```powershell
# Step 1: 启用 WSL2 功能（管理员 PowerShell）
dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart
dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart

# Step 2: 重启电脑后设置 WSL2 为默认
wsl --set-default-version 2

# Step 3: Microsoft Store 安装 Ubuntu

# Step 4: 在 WSL2 Ubuntu 中安装 Redis
sudo apt update
sudo apt install redis-server -y
redis-cli ping
```

> 💡 WSL2 是 Windows 上运行 Linux 程序的最佳方式，性能接近原生 Linux。使用 WSL2 安装 Redis 比原生 Windows 版本更稳定。

### 4.5 Docker（最便捷的方式）

```bash
# 基本启动
docker run --name redis -p 6379:6379 -d redis:latest

# 带密码启动
docker run --name redis -p 6379:6379 -d redis:latest --requirepass "your_password"

# 数据持久化挂载 + AOF
docker run --name redis \
  -p 6379:6379 \
  -v /data/redis:/data \
  -d redis:latest \
  redis-server --appendonly yes

# 使用自定义配置文件
docker run --name redis \
  -p 6379:6379 \
  -v /data/redis/conf/redis.conf:/usr/local/etc/redis/redis.conf \
  -d redis:latest \
  redis-server /usr/local/etc/redis/redis.conf

# Docker Compose 方式
cat > docker-compose.yml <<EOF
version: '3'
services:
  redis:
    image: redis:7.2
    container_name: redis
    ports:
      - "6379:6379"
    volumes:
      - ./data:/data
      - ./redis.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf
    restart: always
EOF

docker-compose up -d
```

### 4.6 源码编译安装

```bash
# 下载源码
wget https://download.redis.io/releases/redis-7.2.4.tar.gz

# 解压
tar -zxvf redis-7.2.4.tar.gz
cd redis-7.2.4

# 编译（无需 configure）
make

# 安装到指定目录
make install PREFIX=/usr/local/redis

# 将二进制加入 PATH
echo 'export PATH=/usr/local/redis/bin:$PATH' >> ~/.bashrc
source ~/.bashrc

# 安装后的二进制文件
# redis-server    Redis 服务器
# redis-cli       Redis 命令行客户端
# redis-sentinel  Redis 哨兵
# redis-benchmark 性能测试工具
# redis-check-aof AOF 文件修复
# redis-check-rdb RDB 文件检查
```

### 4.7 验证安装

```bash
# 检查版本
redis-server --version

# 启动服务（前台）
redis-server

# 启动服务（后台守护进程）
redis-server --daemonize yes

# 客户端连接
redis-cli

# 基本验证
127.0.0.1:6379> ping
PONG
127.0.0.1:6379> set hello "world"
OK
127.0.0.1:6379> get hello
"world"
127.0.0.1:6379> info server
# Server
redis_version:7.2.4
redis_mode:standalone
os:Linux
```

---

## 5. redis.conf 核心配置

### 5.1 配置总表

| 配置项 | 默认值 | 说明 | 生产推荐 |
|--------|--------|------|----------|
| `daemonize` | no | 守护进程模式 | yes |
| `bind` | 127.0.0.1 -::1 | 监听 IP 地址 | 内网 IP 或 0.0.0.0 |
| `port` | 6379 | 监听端口 | 6379（可改） |
| `requirepass` | (空) | 访问密码 | 强密码必设 |
| `maxmemory` | 0（无限制） | 最大可用内存 | 物理内存 70%-80% |
| `maxmemory-policy` | noeviction | 内存淘汰策略 | allkeys-lru |
| `appendonly` | no | AOF 持久化开关 | yes |
| `appendfsync` | everysec | AOF 刷盘策略 | everysec |
| `save` | 900 1 300 10 60 10000 | RDB 触发条件 | 按需调整 |
| `databases` | 16 | 数据库数量 | 16（默认） |
| `lua-time-limit` | 5000 | Lua 脚本超时（ms） | 5000 |
| `timeout` | 0 | 连接空闲超时（秒） | 300 |
| `tcp-backlog` | 511 | TCP 连接队列 | 511 |
| `loglevel` | notice | 日志级别 | notice |
| `logfile` | "" | 日志文件路径 | /var/log/redis.log |
| `pidfile` | /var/run/redis.pid | PID 文件 | /var/run/redis.pid |
| `rename-command` | — | 危险命令重命名 | KEYS / FLUSHALL |
| `slave-read-only` | yes | 从节点只读 | yes |
| `repl-backlog-size` | 1mb | 复制积压缓冲区 | 10mb-100mb |

### 5.2 最小化生产配置

```bash
# 基础
daemonize yes
pidfile /var/run/redis.pid
port 6379
bind 0.0.0.0
timeout 300
loglevel notice
logfile "/var/log/redis.log"

# 安全
requirepass "YourStrongPassword!"
rename-command FLUSHALL ""
rename-command FLUSHDB ""
rename-command KEYS ""
rename-command CONFIG ""

# 持久化
save 900 1
save 300 10
save 60 10000
appendonly yes
appendfsync everysec

# 内存
maxmemory 2gb
maxmemory-policy allkeys-lru
maxmemory-samples 5
```

### 5.3 运行时动态修改配置

```bash
# 查看配置
redis-cli CONFIG GET requirepass
redis-cli CONFIG GET maxmemory

# 修改配置（运行时生效，重启失效）
redis-cli CONFIG SET requirepass "new_password"
redis-cli CONFIG SET maxmemory 1gb
redis-cli CONFIG SET maxmemory-policy allkeys-lru

# 持久化到配置文件（保存当前运行时配置）
redis-cli CONFIG REWRITE
```

> ⚠️ 运行时用 CONFIG SET 修改的配置，重启后丢失。必须执行 CONFIG REWRITE 写入 redis.conf 才能持久化。

---

## 6. redis-cli 基础操作

### 6.1 连接方式

```bash
# 默认连接（本机 6379）
redis-cli

# 指定主机和端口
redis-cli -h 127.0.0.1 -p 6379

# 带密码连接
redis-cli -h 127.0.0.1 -p 6379 -a "your_password"

# 指定数据库（0-15）
redis-cli -n 1

# 连接后认证
redis-cli
> auth "your_password"
```

### 6.2 键通用操作

```bash
# 切换数据库 (0-15)
SELECT 0

# 查看所有键（⚠️ 生产禁用，用 SCAN 替代）
KEYS *

# 遍历键（推荐）
SCAN 0 MATCH user:* COUNT 100

# 判断键是否存在
EXISTS key_name

# 查看键类型
TYPE key_name

# 删除键
DEL key_name

# 异步删除（大 Key）
UNLINK key_name

# 设置过期时间（秒）
EXPIRE key_name 60

# 设置过期时间（毫秒）
PEXPIRE key_name 60000

# 查看剩余存活时间（秒）
TTL key_name

# 查看剩余存活时间（毫秒）
PTTL key_name

# 移除过期时间
PERSIST key_name

# 重命名键
RENAME old_key new_key

# 随机返回一个键
RANDOMKEY

# 键数量
DBSIZE
```

### 6.3 字符串操作

```bash
# 设置值
SET name "redis"

# 获取值
GET name

# 设置并指定过期时间
SET name "redis" EX 10

# 不存在时才设置（分布式锁基础）
SETNX name "redis"

# 批量设置
MSET key1 "value1" key2 "value2"

# 批量获取
MGET key1 key2

# 自增/自减
INCR count
INCRBY count 10
DECR count
DECRBY count 10

# 浮点数自增
INCRBYFLOAT price 0.5

# 追加字符串
APPEND name " is awesome"

# 获取字符串长度
STRLEN name

# 获取子串
GETRANGE name 0 4

# 设置新值并返回旧值
GETSET name "new_redis"
```

### 6.4 服务器管理

```bash
# 服务器信息
INFO              # 所有信息
INFO server       # 服务器信息
INFO memory       # 内存信息
INFO stats        # 统计信息
INFO persistence  # 持久化信息
INFO replication  # 复制信息
INFO cluster      # 集群信息
INFO keyspace     # 键空间统计

# 慢查询
SLOWLOG GET 100          # 查看最近 100 条慢查询
SLOWLOG LEN              # 慢查询数量
SLOWLOG RESET            # 清空慢查询

# 客户端管理
CLIENT LIST              # 查看所有客户端连接
CLIENT KILL addr:port    # 断开指定客户端
CLIENT SETNAME myapp     # 设置连接名称

# 持久化操作
SAVE                     # 同步持久化（阻塞）
BGSAVE                   # 异步持久化（后台执行）
BGREWRITEAOF             # AOF 重写
LASTSAVE                 # 上次成功保存的时间戳

# 监控
MONITOR                  # 实时监控（⚠️ 影响性能，仅调试用）

# 清空数据
FLUSHDB                  # 清空当前库
FLUSHALL                 # 清空所有库（⚠️ 慎用）
```

> ⚠️ `KEYS *`、`MONITOR`、`FLUSHALL` 等命令在生产环境要极度谨慎使用。KEYS * 会遍历所有键阻塞 Redis，MONITOR 会大幅降低吞吐，FLUSHALL 会清空所有数据。

### 6.5 管道批量操作

```bash
# 使用管道批量发送命令，减少网络 RTT
(printf "SET key1 value1\r\nSET key2 value2\r\nGET key1\r\n"; sleep 1) | nc localhost 6379

# 或使用 --pipe 模式
cat commands.txt | redis-cli --pipe
```

---

## 7. 安全配置

### 7.1 安全措施清单

| 措施 | 配置 / 操作 | 优先级 |
|------|-------------|--------|
| 密码认证 | `requirepass` 设置强密码 | 强制 |
| 绑定 IP | `bind` 设置为内网 IP | 强制 |
| 端口变更 | 修改默认 6379 端口 | 建议 |
| 防火墙限制 | iptables / security group 限制 6379 端口 | 强制 |
| 危险命令禁用 | `rename-command FLUSHALL ""` | 建议 |
| 非 root 运行 | 创建 redis 用户运行 | 建议 |
| ACL 控制（6.0+） | 创建不同权限的用户 | 生产环境 |
| TLS/SSL（6.0+） | 配置证书启用加密通信 | 公网环境 |
| 内核参数优化 | vm.overcommit_memory = 1 | 建议 |
| 禁用危险模块 | 不加载非必要模块 | 建议 |

### 7.2 ACL 配置示例（Redis 6.0+）

```bash
# 创建只读用户
ACL SETUSER readonly ON > readonly_password ~* +@read

# 创建管理员用户
ACL SETUSER admin ON > admin_password ~* +@all

# 查看 ACL 配置
ACL LIST

# 持久化 ACL
ACL SAVE
```

### 7.3 防火墙配置

```bash
# iptables 只允许内网访问
iptables -A INPUT -p tcp --dport 6379 -s 10.0.0.0/8 -j ACCEPT
iptables -A INPUT -p tcp --dport 6379 -j DROP

# 或直接禁用外网访问（bind 配置）
# redis.conf
bind 10.0.0.1 127.0.0.1
```

> ⚠️ 未设置密码且暴露在公网的 Redis 极易被入侵。攻击者可以利用 Redis 写定时任务或 SSH 公钥实现服务器提权。**生产环境务必设置密码 + bind 内网 IP + 防火墙三重防护。**

---

## 8. 学习资源

### 8.1 推荐书籍

| 书名 | 作者 | 推荐理由 |
|------|------|----------|
| 《Redis 实战》(Redis in Action) | Josiah L. Carlson | 最经典入门教材，覆盖命令、持久化、高可用、实战案例 |
| 《Redis 深度历险》 | 钱文品 | 中文进阶佳作，深入底层原理和高级用法 |
| 《Redis 设计与实现》 | 黄健宏 | 源码级深入，适合理解数据结构内部实现 |
| 《Redis 开发与运维》 | 付磊 / 张益军 | 实战运维经验丰富 |

> 💡 初学者建议先读《Redis 实战》建立全景认知，再看《Redis 深度历险》深入核心原理。

### 8.2 在线资源

| 资源 | 地址 | 说明 |
|------|------|------|
| 官方文档 | https://redis.io/docs | 权威文档，建议常翻 |
| 命令参考 | https://redis.io/commands | 所有命令详细说明 |
| GitHub | https://github.com/redis/redis | 源码仓库 |
| Redis 在线试验 | https://try.redis.io | 在线交互式学习 |

---

## 避坑总结

| 坑点 | 问题后果 | 正确做法 |
|------|----------|----------|
| 内存溢出 | Redis OOM 导致服务崩溃 | 设置 `maxmemory` + 适当的淘汰策略 |
| 数据丢失 | 重启后缓存数据全部丢失 | RDB + AOF 同时开启，或启用混合持久化 |
| 慢命令阻塞 | KEYS `*` 导致 Redis 阻塞数秒 | 禁用 KEYS，使用 SCAN 游标遍历 |
| 无密码暴露公网 | Redis 被入侵，服务器被提权 | 密码 + bind + 防火墙三重防护 |
| 单机滥用 | Redis 单机瓶颈影响业务 | 按场景合理使用，必要时上集群 |
| 大 Key 问题 | 阻塞 Redis、导致主从同步延迟 | 拆分大 Key，或使用 UNLINK 异步删除 |
| 缓存穿透 | 大量请求穿透 Redis 打垮 DB | 缓存空值 + 布隆过滤器 + 限流 |
| 缓存雪崩 | 大量缓存集中过期，DB 压力暴增 | 过期时间加随机偏移 + 本地缓存 |
| 缓存击穿 | 热点 key 过期，高并发同时查询 DB | 互斥锁重建缓存 + 逻辑过期 |
| 主从不一致 | 读写分离场景读到旧数据 | 根据数据一致性要求选择强读或容忍最终一致 |

> 🎯 Redis 入门的核心：搞懂它是什么（数据结构服务器）、能做什么（缓存/锁/队列/计数）、怎么用好（持久化/淘汰策略/安全配置）、怎么避坑（大 Key/穿透/雪崩/击穿）。

