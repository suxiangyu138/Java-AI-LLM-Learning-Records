# Nginx 面试宝典（高级篇）
> 基于课程大纲聚焦亿级流量架构设计，覆盖高可用、内核调优、多级缓存、OpenResty/Lua 开发等深度技能

## 目录
1. [一、基础概念速答（20题）](#一基础概念速答20题)
2. [二、深度原理剖析（12题）](#二深度原理剖析12题)
3. [三、实战场景题（10题）](#三实战场景题10题)
4. [四、手写代码/配置文件题（6题）](#四手写代码配置文件题6题)
5. [五、系统设计题（4题）](#五系统设计题4题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板（Top 5）](#七面试回答模板top-5)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答（20题）

### 1.1 How does the multi-process model work? Nginx 多进程模型深入理解？
Nginx 采用 **pre-fork 多进程模型**，与 Apache 不同，Nginx 的 worker 进程之间是对等的，不存在父子关系（真正的父进程是 master）。

```
Master 进程（root 权限）
  │  读取配置、绑定端口、fork worker
  ├── Worker 1 (nobody)
  ├── Worker 2 (nobody)
  ├── Worker 3 (nobody)
  └── Worker 4 (nobody)
       每个 worker：独立的事件循环
       竞争 accept 新连接（accept_mutex 互斥）
       非阻塞处理已建立连接
```

| 进程 | 角色 | 关键行为 |
|------|------|---------|
| Master | 配置读取、端口绑定、fork/管理 worker | 以 root 运行（绑定 80/443） |
| Worker | 实际处理请求 | 以低权限用户运行 |
| Cache Manager | 缓存过期清理 | 定期检查缓存 |
| Cache Loader | 缓存预热加载 | 启动时加载已有缓存 |

> 💡 Nginx 的惊群问题：多个 worker 同时 accept 同一个 socket 时，只有一个成功，其余空转。通过 `accept_mutex on;` 解决，让 worker 排队 accept。

### 1.2 What is the keepalive mechanism? Keepalive 的客户端和上游配置？
Nginx 需要区分配置两个方向的 keepalive：

```
客户端 keepalive: Client ↔ Nginx
上游 keepalive:   Nginx ↔ Backend Server
```

**客户端 keepalive**：
```nginx
keepalive_timeout  65;           # 保持连接超时
keepalive_requests 100;          # 单连接最多处理 100 个请求
```

**上游 keepalive**：
```nginx
upstream backend {
    server 192.168.1.10:8080;
    server 192.168.1.11:8080;
    keepalive 32;                # 空闲连接池大小
    keepalive_requests 100;      # 单连接最大请求数
    keepalive_timeout 60s;       # 空闲超时
}

location / {
    proxy_http_version 1.1;      # upstream keepalive 必须
    proxy_set_header Connection "";
    proxy_pass http://backend;
}
```

**性能对比**（来自课程压测数据）：
| 场景 | QPS | 提升 |
|------|-----|------|
| 无 keepalive | ~8000 | 基准 |
| 客户端 keepalive | ~12000 | +50% |
| 上游 keepalive | ~16000 | +100% |
| 双端 keepalive | ~20000 | +150% |

### 1.3 How does Nginx + keepalived achieve high availability? Nginx + keepalived 如何实现高可用？
Keepalived 基于 **VRRP（Virtual Router Redundancy Protocol）** 实现 Nginx 高可用：

```yaml
虚拟 IP (VIP): 192.168.1.100
    │
    ├── MASTER (权重 100)
    │   └── Nginx + keepalived
    │
    └── BACKUP (权重 50)
        └── Nginx + keepalived
```

```nginx
# /etc/keepalived/keepalived.conf
global_defs {
    router_id NGINX_HA
}

vrrp_script chk_nginx {
    script "/etc/keepalived/check_nginx.sh"    # 检查 Nginx 进程
    interval 2
    weight -20
}

vrrp_instance VI_1 {
    state MASTER
    interface eth0
    virtual_router_id 51
    priority 100
    advert_int 1
    authentication {
        auth_type PASS
        auth_pass 1234
    }
    virtual_ipaddress {
        192.168.1.100/24
    }
    track_script {
        chk_nginx
    }
}
```

```bash
# /etc/keepalived/check_nginx.sh
#!/bin/bash
if [ "$(ps -C nginx --no-header | wc -l)" -eq 0 ]; then
    systemctl start nginx
    sleep 2
    if [ "$(ps -C nginx --no-header | wc -l)" -eq 0 ]; then
        killall keepalived
        exit 1
    fi
fi
```

> 💡 VRRP 原理：MASTER 定期组播发送 VRRP 通告（advert_int 1s），BACKUP 收到则重置计时器，若 3 个通告间隔内未收到，BACKUP 升为 MASTER 接管 VIP。

### 1.4 What Linux kernel parameters need tuning for Nginx? Nginx 需要调整哪些 Linux 内核参数？
| 参数 | 说明 | 推荐值 |
|------|------|--------|
| `worker_rlimit_nofile` | worker 进程最大文件句柄数 | 65535 |
| `net.core.somaxconn` | listen 队列最大长度 | 65535 |
| `net.ipv4.tcp_tw_reuse` | TIME_WAIT 重用 | 1 |
| `net.ipv4.tcp_tw_recycle` | TIME_WAIT 快速回收 | 0（内核 4.12+ 已移除） |
| `net.ipv4.tcp_fin_timeout` | FIN 等待超时 | 15 |
| `net.ipv4.tcp_keepalive_time` | TCP keepalive 探测间隔 | 300 |
| `net.core.netdev_max_backlog` | 网卡接收队列 | 65535 |
| `net.ipv4.tcp_max_syn_backlog` | SYN 半连接队列 | 65535 |
| `net.ipv4.tcp_fastopen` | TCP 快速打开 | 3 |
| `vm.swappiness` | 内存交换策略 | 10 |
| `fs.file-max` | 系统级文件句柄上限 | 1000000 |

```bash
# /etc/sysctl.conf 配置示例
fs.file-max = 1000000
net.core.somaxconn = 65535
net.core.netdev_max_backlog = 65535
net.ipv4.tcp_max_syn_backlog = 65535
net.ipv4.tcp_fin_timeout = 15
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_keepalive_time = 300
net.ipv4.tcp_fastopen = 3
vm.swappiness = 10
```

> ⚠️ `tcp_tw_recycle` 在 NAT 环境下会导致连接问题，Linux 4.12+ 已删除该参数。使用 `tcp_tw_reuse` + 调整 `tcp_fin_timeout` 替代。

### 1.5 How does the GEOIP module work? GEOIP 模块的工作原理？
GEOIP 模块根据客户端 IP 查询地理位置数据库，实现地域分发：

```nginx
# 加载 GEOIP 模块
load_module modules/ngx_http_geoip_module.so;

http {
    # 指定 GeoIP 数据库文件
    geoip_country /usr/share/GeoIP/GeoIP.dat;
    geoip_city    /usr/share/GeoIP/GeoLiteCity.dat;

    # GEOIP 变量
    # $geoip_country_code       → CN, US, JP...
    # $geoip_country_code3      → CHN, USA, JPN...
    # $geoip_country_name       → China, United States...
    # $geoip_city               → Beijing, Shanghai...
    # $geoip_latitude           → 39.9042
    # $geoip_longitude          → 116.4074

    # 地域分发 upstream
    map $geoip_country_code $backend_pool {
        default   global_pool;
        CN        china_pool;
        US        us_pool;
        JP        japan_pool;
    }

    upstream china_pool {
        server cn-node1.example.com;
        server cn-node2.example.com;
    }

    upstream us_pool {
        server us-node1.example.com;
        server us-node2.example.com;
    }

    server {
        location / {
            proxy_pass http://$backend_pool;
        }
    }
}
```

### 1.6 What is multi-level caching architecture? 什么是多级缓存架构？
```
请求路径：
用户 → CDN (L1) → Nginx (L2) → Redis (L3) → 应用 (L4) → 数据库 (L5)
         ↓           ↓            ↓            ↓           ↓
     静态资源     代理缓存     热点数据     业务缓存     持久化
     全球加速     边缘缓存     分布式缓存   LRU 淘汰    最终数据
```

**各缓存层级特点**：
| 层级 | 延迟 | 容量 | 成本 | 命中率目标 |
|------|------|------|------|-----------|
| CDN (L1) | <10ms | 无限 | 高 | 30%-50% |
| Nginx (L2) | <1ms | GB-TB | 低 | 20%-30% |
| Redis (L3) | <1ms | GB | 中 | 10%-20% |
| 应用本地 (L4) | <0.1ms | MB | 免费 | 5%-10% |

### 1.7 What is SSI (Server Side Includes)? 什么是 SSI？
SSI 是 Nginx 在服务端将多个文件片段合并为一个响应的技术：

```nginx
ssi on;
ssi_silent_errors off;           # SSI 解析错误时报告
ssi_types text/html;             # 只处理 HTML
ssi_min_file_chunk 1k;           # 最小文件块
```

```html
<!-- HTML 中使用 SSI 指令 -->
<!DOCTYPE html>
<html>
<body>
    <!--# include virtual="/header.html" -->
    <!--# include virtual="/content.html" -->
    <!--# set $page_title "Home Page" -->
    <!--# echo var="page_title" -->
    <!--# if expr="$query_string = /page=/" -->
        <!--# include virtual="/dynamic/page.html" -->
    <!--# endif -->
    <!--# include virtual="/footer.html" -->
</body>
</html>
```

> 💡 SSI 适用于 CMS 网站将页头/页脚/导航栏等公共部分合并，但高并发场景下推荐使用 CDN 或前端组件化替代。

### 1.8 How does Rsync + inotify achieve static resource synchronization? Rsync + inotify 如何实现静态资源同步？
多台 Nginx 节点之间的静态文件同步方案：

```bash
# inotify 监控脚本
#!/bin/bash
MONITOR_DIR=/data/nginx/static
RSYNC_USER=rsync_user
RSYNC_PASS=rsync_pass
REMOTE_HOSTS=("10.0.1.11" "10.0.1.12")

# 监控文件变更
inotifywait -mrq --timefmt '%Y-%m-%d %H:%M:%S' --format '%w%f %e' \
    -e modify,create,delete,move $MONITOR_DIR | while read file event; do

    # 遍历同步到所有远程节点
    for host in "${REMOTE_HOSTS[@]}"; do
        rsync -avz --delete --password-file=$RSYNC_PASS \
            $MONITOR_DIR/ ${RSYNC_USER}@${host}::static/
    done
done
```

```bash
# rsync 服务端配置 (/etc/rsyncd.conf)
uid = root
gid = root
[static]
    path = /data/nginx/static
    comment = Nginx Static Files
    read only = no
    auth users = rsync_user
    secrets file = /etc/rsyncd.secrets
```

| 同步方式 | 延迟 | 复杂度 | 适用场景 |
|---------|------|--------|---------|
| NFS 共享存储 | 实时 | 低 | 小集群（<10 节点） |
| Rsync + inotify | 秒级 | 中 | 中等集群 |
| 分布式存储（Ceph/GlusterFS） | 实时 | 高 | 大规模集群 |
| 对象存储（OSS/S3） | 实时 | 低 | 云原生场景 |

### 1.9 How does Nginx integrate with Redis/Memcached? Nginx 如何对接 Redis/Memcached？
Nginx 可以直接从 Redis/Memcached 获取缓存数据，避免请求到达后端的语言运行时（PHP/Java）：

```nginx
# Nginx + Memcached
location / {
    set $memcached_key "$uri";
    memcached_pass 127.0.0.1:11211;
    error_page 404 502 504 = @fallback;
}

location @fallback {
    proxy_pass http://backend;
}

# Nginx + Redis（通过 redis2-nginx-module）
location /redis {
    redis2_query get $uri;
    redis2_pass 127.0.0.1:6379;
}

# 更推荐：通过 OpenResty + lua-resty-redis 连接 Redis
location / {
    content_by_lua_block {
        local redis = require "resty.redis"
        local red = redis:new()
        red:set_timeouts(1000, 1000, 1000)
        local ok, err = red:connect("127.0.0.1", 6379)
        if not ok then
            ngx.say("redis connection failed: ", err)
            return
        end
        local res, err = red:get(ngx.var.uri)
        if not res then
            res = ngx.location.capture("/backend")
            red:set(ngx.var.uri, res.body)
        end
        ngx.say(res)
    }
}
```

### 1.10 How to configure TCP/UDP proxy with Nginx stream module? 如何配置 Nginx Stream 模块的 TCP/UDP 代理？
```nginx
stream {
    # TCP 代理（MySQL 透明代理）
    upstream mysql_proxy {
        server 192.168.1.10:3306;
        server 192.168.1.11:3306;
        server 192.168.1.12:3306 backup;
    }
    server {
        listen 3306;
        proxy_pass mysql_proxy;
        proxy_connect_timeout 5s;
        proxy_timeout 30s;
        proxy_buffer_size 16k;
    }

    # UDP 代理（DNS 代理）
    upstream dns_servers {
        server 8.8.8.8:53;
        server 1.1.1.1:53;
    }
    server {
        listen 53 udp;
        proxy_pass dns_servers;
        proxy_timeout 5s;
    }

    # SSL/TLS 终止
    upstream ssl_backend {
        server 10.0.1.10:443;
    }
    server {
        listen 443 ssl;
        ssl_certificate     /etc/nginx/ssl/server.crt;
        ssl_certificate_key /etc/nginx/ssl/server.key;
        proxy_pass ssl_backend;
    }
}
```

### 1.11 What is OpenResty? 什么是 OpenResty？
OpenResty = Nginx + LuaJIT + 精选 Lua 库，是一个**全功能的 Web 应用服务器**，能在 Nginx 阶段中嵌入 Lua 代码。

| 组件 | 作用 |
|------|------|
| Nginx | 事件驱动核心 |
| LuaJIT | 高性能 Lua 即时编译器 |
| ngx_lua_module | Lua 与 Nginx 的桥梁 |
| lua-resty-* | 丰富生态库（Redis/MySQL/Redis/memcached） |

```nginx
# OpenResty 配置示例
events {
    worker_connections 1024;
}

http {
    # Lua 模块路径
    lua_package_path "/usr/local/openresty/lualib/?.lua;;";

    # Lua 共享字典
    lua_shared_dict my_cache 10m;

    server {
        listen 80;

        location / {
            default_type text/plain;
            content_by_lua_block {
                ngx.say("Hello OpenResty!")
            }
        }
    }
}
```

### 1.12 What is shared_dict and lrucache in OpenResty? OpenResty 的 shared_dict 和 lrucache？
| 缓存 | 作用域 | 数据一致性 | 适用场景 |
|------|--------|-----------|---------|
| `ngx.shared.DICT` | 跨 worker 进程 | 共享内存，强一致 | 全局计数器、频率统计 |
| `lua-resty-lrucache` | 单 worker 进程 | 本地缓存，无同步 | 热点数据、频繁访问 |

```lua
-- shared_dict (跨进程共享)
local dict = ngx.shared.my_cache
dict:set("key", "value", 60)           -- TTL 60s
local val = dict:get("key")
dict:incr("counter", 1)                -- 原子自增

-- lrucache (进程内 LRU)
local lrucache = require "resty.lrucache"
local cache, err = lrucache.new(200)   -- 最大 200 个条目
cache:set("key", "value", 60)
local val = cache:get("key")
```

> 💡 **性能对比**：lrucache 在单 worker 内访问时延约 0.1μs，shared_dict 约 0.5μs（需跨进程同步）。

### 1.13 How does OpenResty connect to Redis? OpenResty 如何连接 Redis？
```lua
local redis = require "resty.redis"

local function get_redis()
    local red = redis:new()
    red:set_timeouts(1000, 1000, 1000)   -- connect/read/write 超时

    local ok, err = red:connect("127.0.0.1", 6379)
    if not ok then
        ngx.log(ngx.ERR, "failed to connect to redis: ", err)
        return nil
    end

    -- 连接池
    red:set_keepalive(60000, 100)        -- 60s 空闲超时，池大小 100
    return red
end

-- 使用
local red = get_redis()
if red then
    local res, err = red:get(ngx.var.uri)
    if res and res ~= ngx.null then
        ngx.say(res)
        return
    end
end

-- 回源
local res = ngx.location.capture("/backend")
ngx.say(res.body)
```

### 1.14 How does OpenResty connect to MySQL? OpenResty 如何连接 MySQL？
```lua
local mysql = require "resty.mysql"

local function query_db(sql)
    local db, err = mysql:new()
    if not db then
        ngx.log(ngx.ERR, "mysql new failed: ", err)
        return nil
    end

    db:set_timeout(1000)

    local ok, err = db:connect({
        host = "127.0.0.1",
        port = 3306,
        database = "app_db",
        user = "app_user",
        password = "app_password",
        charset = "utf8mb4",
        max_packet_size = 1024 * 1024,
    })

    if not ok then
        ngx.log(ngx.ERR, "mysql connect failed: ", err)
        return nil
    end

    local res, err, errno, sqlstate = db:query(sql)
    if not res then
        ngx.log(ngx.ERR, "mysql query failed: ", err)
        return nil
    end

    local ok, err = db:set_keepalive(10000, 50)
    return res
end
```

### 1.15 What is active health check? 什么是主动健康检查？
Nginx 开源版只支持**被动健康检查**（根据请求响应判断），Tengine 或 Nginx Plus 支持**主动健康检查**（定时发包检测）。

**主动健康检查（Tengine）**：
```nginx
upstream backend {
    server 192.168.1.10:8080;
    server 192.168.1.11:8080;

    # 主动健康检查
    check interval=3000 rise=2 fall=5 timeout=1000 type=http;
    check_http_send "GET /health HTTP/1.0\r\n\r\n";
    check_http_expect_alive http_2xx http_3xx;
    check_keepalive_requests 100;
}
```

| 参数 | 含义 |
|------|------|
| `interval` | 检查间隔（ms） |
| `rise` | 连续成功次数标记为存活 |
| `fall` | 连续失败次数标记为死亡 |
| `timeout` | 检查超时（ms） |
| `type` | 检查方式（http/tcp/ssl） |

### 1.16 What is passive retry mechanism? 什么是被动重试机制？
Nginx 的被动重试在 `proxy_next_upstream` 配置：

```nginx
location / {
    proxy_pass http://backend;
    proxy_next_upstream error timeout invalid_header http_500 http_502 http_503;
    proxy_next_upstream_tries 3;          # 最多重试 3 次（含首次）
    proxy_next_upstream_timeout 30s;      # 重试总超时
    proxy_connect_timeout 5s;
    proxy_read_timeout 10s;
}
```

| 参数 | 触发条件 |
|------|---------|
| `error` | 连接错误 |
| `timeout` | 连接/读/写超时 |
| `invalid_header` | 无效响应头 |
| `http_500` | 后端 500 |
| `http_502` | 后端 502 |
| `http_503` | 后端 503 |
| `http_429` | 后端限流 |
| `non_idempotent` | 非幂等方法（POST 等，默认不重试） |

### 1.17 How to analyze Nginx logs? 如何进行 Nginx 日志分析？
**常用分析指标和命令**：
```bash
# 1. QPS 统计
tail -n 10000 access.log | awk '{print $4}' | cut -d: -f1,2 | sort | uniq -c | sort -rn

# 2. 最慢的 10 个请求
cat access.log | awk '{print $NF, $0}' | sort -rn | head -10

# 3. 状态码分布
cat access.log | awk '{print $9}' | sort | uniq -c | sort -rn

# 4. TOP 10 访问 IP
cat access.log | awk '{print $1}' | sort | uniq -c | sort -rn | head -10

# 5. TOP 10 请求路径
cat access.log | awk '{print $7}' | sort | uniq -c | sort -rn | head -10

# 6. 统计 4xx/5xx 占比
total=$(wc -l < access.log)
errors=$(grep -cE ' "(4[0-9]{2}|5[0-9]{2}) ' access.log)
echo "Error rate: $((errors * 100 / total))%"
```

**日志缓冲优化**：
```nginx
# 减少磁盘 I/O
access_log /var/log/nginx/access.log main buffer=64k flush=5s;
# buffer=64k: 缓冲区满 64KB 写入
# flush=5s:   每 5 秒强制写入一次
```

### 1.18 How does Nginx buffer work? Nginx 缓冲区原理？
```nginx
# 客户端缓冲区
client_body_buffer_size 128k;
client_max_body_size 100m;
client_body_temp_path /tmp/client_body;

# 代理缓冲区
proxy_buffer_size 4k;
proxy_buffers 8 4k;
proxy_busy_buffers_size 8k;
proxy_temp_path /tmp/proxy_temp;
proxy_max_temp_file_size 1024m;

# 快速文件打开缓存
open_file_cache max=10000 inactive=60s;
open_file_cache_valid 30s;
open_file_cache_min_uses 2;
open_file_cache_errors on;
```

| 缓冲区 | 作用 | 过大影响 | 过小影响 |
|--------|------|---------|---------|
| `client_body_buffer_size` | 请求体缓冲区 | 占用内存 | 频繁写磁盘 |
| `proxy_buffer_size` | 上游响应头 | 浪费 | 截断头部 |
| `proxy_buffers` | 上游响应体 | 内存占用高 | 频繁写磁盘 |
| `open_file_cache` | 文件句柄/元信息 | 缓存陈旧 | 频繁 open |

### 1.19 How does Nginx achieve connection pooling? Nginx 连接池机制？
Nginx 为每个 worker 进程维护独立的连接池，包括：

```nginx
events {
    worker_connections 1024;        # 每个 worker 的最大连接数
    use epoll;
    multi_accept on;                # 一次 accept 多个连接
    accept_mutex on;                # 避免惊群
    accept_mutex_delay 500ms;       # 重试间隔
}
```

连接池管理：
```
epoll 实例
  ├── 活跃连接（正在处理请求）
  ├── 空闲连接（keepalive 池）
  └── 待关闭连接（time_wait 等）
```

> 💡 每个连接占用约 2.5KB 内存（包括连接结构体和读/写缓冲区）。`worker_connections 10240` 约占用 25MB。

### 1.20 What is the epoll model in Nginx? Nginx 的 epoll 模型？
Epoll 是 Linux 上最高效的 I/O 多路复用机制，Nginx 利用 epoll 实现高并发：

| I/O 模型 | 时间复杂度 | 原理 | 最大连接数 |
|---------|-----------|------|-----------|
| `select` | O(n) | 轮询 fd 集合 | 1024 |
| `poll` | O(n) | 轮询 fd 链表 | 无限制（但性能 O(n)） |
| `epoll` | O(1) | 事件驱动回调 | 无限制 |

Epoll 工作模式：
- **LT (Level Triggered)**：水平触发，未处理会重复通知
- **ET (Edge Triggered)**：边缘触发，只通知一次（Nginx 默认使用 LT）

---

## 二、深度原理剖析（12题）

### 2.1 Nginx 多进程模型的惊群问题和解决方案
**惊群现象**：当一个新的连接到达时，所有 worker 进程都被唤醒竞争 accept，但只有一个能成功，其余空转，造成 CPU 资源浪费。

```nginx
events {
    accept_mutex on;             # 启用互斥锁（默认 on）
    accept_mutex_delay 500ms;    # 获取锁失败时的重试间隔
}
```

**解决方案**：
1. **accept_mutex 互斥锁**：worker 进程先获取锁再 accept，获取不到则等待
2. **reuseport（Linux 3.9+）**：内核将连接直接分发到 worker，彻底解决惊群

```nginx
# 最优方案：reuseport（需要内核支持）
server {
    listen 80 reuseport;         # 内核级负载均衡
    # 每个 worker 独立监听 socket，内核直接分发
}
```

> 🎯 **面试亮点**：Nginx 1.9.1+ 支持 reuseport 特性，通过 `SO_REUSEPORT` 让内核将连接均匀分配给 worker，既解决惊群又提高 CPU cache 亲和性。

### 2.2 Nginx 的请求处理阶段（Phases）
Nginx 的 HTTP 请求处理分为 11 个阶段：

```
NGX_HTTP_POST_READ         → 读取请求头
NGX_HTTP_SERVER_REWRITE    → server 级 rewrite
NGX_HTTP_FIND_CONFIG       → location 匹配
NGX_HTTP_REWRITE           → location 级 rewrite
NGX_HTTP_POST_REWRITE      → rewrite 后检查是否需要重走
NGX_HTTP_PREACCESS         → 访问控制前（limit_req/limit_conn）
NGX_HTTP_ACCESS            → 访问控制（allow/deny）
NGX_HTTP_POST_ACCESS       → 访问控制后
NGX_HTTP_PRECONTENT        → 内容产生前（try_files）
NGX_HTTP_CONTENT           → 内容产生（proxy_pass/fastcgi_pass）
NGX_HTTP_LOG               → 日志记录
```

> 💡 理解请求处理阶段对排查问题至关重要。例如 `limit_req` 在第 6 阶段执行，`proxy_pass` 在第 10 阶段执行，`access_log` 在最后阶段。

### 2.3 VRRP 协议工作原理
VRRP（Virtual Router Redundancy Protocol）是 Keepalived 的核心协议：

```
MASTER 角色：
  │  优先级 100
  │  发送 VRRP 通告（组播 224.0.0.18）
  │  持有 VIP
  │  响应 ARP 请求
  │
BACKUP 角色：
  │  优先级 90
  │  监听 VRRP 通告
  │  不持有 VIP
  │
故障切换流程：
  1. MASTER 宕机 → 停止发送 VRRP 通告
  2. BACKUP 在 3*advert_int 内未收到通告
  3. BACKUP 切换为 MASTER
  4. 发送免费 ARP 广播 VIP 映射
  5. 流量切换完成
```

```yaml
VRRP 关键参数:
  priority:   0-255，越大越优先成为 MASTER
  advert_int: 通告间隔（秒），默认 1s
  preempt:    是否允许高优先级抢占（默认是）
```

### 2.4 Linux 内核的 TCP 三次握手与 Nginx 的关系
```bash
# 半连接队列（SYN Queue）
net.ipv4.tcp_max_syn_backlog = 65535
net.ipv4.tcp_syn_retries = 2          # SYN 重试次数

# 全连接队列（Accept Queue）
net.core.somaxconn = 65535            # 应用调用 listen(fd, backlog)
```

Nginx 监听 socket 的全连接队列大小由 `listen` 指令的 `backlog` 参数决定：
```nginx
server {
    listen 80 backlog=65535;          # 等同于内核 somaxconn
}
```

**连接建立流程**：
```
Client                Nginx
   │                   │
   │── SYN ──────────→ │  (1) 放入 SYN Queue（半连接）
   │←─ SYN+ACK ─────── │  (2) tcp_max_syn_backlog
   │── ACK ──────────→ │  (3) 移入 Accept Queue（全连接）
   │                   │  (4) somaxconn 控制队列长度
   │←─ accept() ────── │  (5) worker 调用 accept 取出连接
```

> ⚠️ 如果 `somaxconn` 太小，高并发下客户端在握手第三步时收到 RST 包，表现为连接被拒绝（connection reset）。

### 2.5 TCP Fast Open (TFO) 原理
TFO 允许在 TCP 握手的 SYN 包中就携带应用数据：

```
传统三次握手：
C ──SYN──→ S
C ←─SYN+ACK── S    (1 RTT 后才发送数据)
C ──ACK+数据──→ S

TFO 模式（重复连接）：
C ──SYN+数据──→ S  (0 RTT 发送数据)
C ←─SYN+ACK── S
```

```bash
# 开启 TFO（服务端 + 客户端）
net.ipv4.tcp_fastopen = 3
# 1 = 客户端开启, 2 = 服务端开启, 3 = 双向开启
```

```nginx
server {
    listen 80 deferred;              # deferred = 延迟 accept
    # 或
    listen 80 fastopen=256;          # TFO 队列大小
}
```

> 💡 TFO 在重复连接场景下能减少约 1 RTT 的延迟，对移动端/长轮询场景特别有效。

### 2.6 Nginx 惊群与 reuseport 深入对比
| 机制 | 原理 | 性能 | 要求 |
|------|------|------|------|
| `accept_mutex` | 进程级锁，竞争 accept | 中等 | 无 |
| `SO_REUSEPORT` | 内核分发，多个 socket 独立监听 | 高（均摊 + CPU 亲和） | Linux 3.9+ |

```nginx
# 性能对比配置
server {
    listen 80 reuseport;             # 方式 A: reuseport
}

server {
    listen 80;                       # 方式 B: accept_mutex
}
```

`reuseport` 的额外优势：
1. CPU 缓存亲和性：同一连接的请求始终由同一 worker 处理
2. 连接均匀分发：内核使用哈希保证均衡
3. 无锁竞争：每个 worker 有独立的 socket 监听队列

### 2.7 Keepalived 的 VRRP 抢占模式与非抢占模式
```nginx
# 抢占模式（默认）
vrrp_instance VI_1 {
    state MASTER
    priority 100
    preempt on                       # 高优先级自动抢占 VIP
    # MASTER 恢复后重新抢占 VIP，导致两次切换
}

# 非抢占模式
vrrp_instance VI_1 {
    state MASTER
    priority 100
    preempt off
    # 即使 MASTER 恢复也不抢占，避免频繁切换
}

# 延迟抢占
vrrp_instance VI_1 {
    priority 100
    preempt_delay 300                # 至少等 300 秒再抢占
}
```

| 模式 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| 抢占 | 保证最优节点处理流量 | 主恢复时两次切换 | 对切换延迟不敏感 |
| 非抢占 | 减少切换次数 | 可能不是最优节点服务 | 对抖动敏感的业务 |

### 2.8 Nginx 的 proxy_buffering 与流式传输
```nginx
# 缓冲区模式（默认）
proxy_buffering on;
proxy_buffer_size 4k;
proxy_buffers 8 4k;                 # 8 * 4k = 32k 总缓冲区
proxy_busy_buffers_size 8k;         # 向客户端发送时的忙缓冲区

# 流式模式（逐块转发）
proxy_buffering off;
proxy_buffers off;

# 大文件 - 使用缓冲区避免磁盘 I/O
proxy_buffering on;
proxy_max_temp_file_size 0;         # 禁止写入临时文件（全内存）
proxy_buffers 256 4k;               # 256 * 4k = 1MB 缓冲区
```

**原理分析**：
- `proxy_buffering on`：Nginx 先接收完整响应到缓冲区 → 再转发给客户端
- 优点：后端响应速度快于客户端接收速度时，Nginx 缓存后后端可释放连接
- 缺点：响应较大时需占用大量内存或写入临时磁盘文件

### 2.9 OpenResty 的 cosocket 机制
Cosocket (Coroutine Socket) 是 OpenResty 的核心创新，在 Nginx 事件循环中实现**非阻塞 TCP Socket**：

```lua
-- cosocket 的工作方式
-- 1. 调用 connect → 挂起当前协程
-- 2. Nginx 事件循环继续处理其他请求
-- 3. Socket 连接建立 → 恢复协程继续执行
-- 4. 看起来像是"同步"代码，实际上是"异步非阻塞"

local function fetch_data()
    local sock = ngx.socket.tcp()
    local ok, err = sock:connect("127.0.0.1", 6379)  -- 挂起
    -- ↓ 此处代码在连接建立后才继续执行
    local res, err = sock:receive()                    -- 挂起
    -- ↓ 此处代码在数据到达后才继续执行
    sock:close()
    return res
end
```

**优势**：
| 特征 | 传统方式 | Cosocket |
|------|---------|----------|
| 代码风格 | 回调嵌套 | 同步风格 |
| 并发能力 | 阻塞 worker | 不阻塞，继续处理其他请求 |
| 开发效率 | 低 | 高 |
| 资源占用 | 每个连接独立线程 | 单线程事件驱动 |

### 2.10 Nginx 的 keyval 模块和 map 模块的差异
| 特性 | `map` 模块 | `keyval` 模块（Nginx Plus） |
|------|-----------|---------------------------|
| 数据来源 | 配置文件静态定义 | 外部 API 动态更新 |
| 变更方式 | 需 reload | 运行时动态修改 |
| 存储 | nginx.conf 中 | 共享内存 |
| 使用场景 | 静态映射规则 | 动态黑白名单、特性开关 |

```nginx
# map 模块（静态）
map $http_host $backend {
    hostnames;
    default       default_backend;
    *.example.com example_backend;
    ~^api\.       api_backend;
}

# keyval 模块（动态，Nginx Plus）
keyval_zone zone=blacklist:1m state=/etc/nginx/state/blacklist.json;
keyval $remote_addr $is_blacklisted zone=blacklist;

# API 更新动态 keyval（需 Nginx Plus）
# curl -X POST -d '{"10.0.0.1":"1"}' http://localhost:8080/api/3/http/keyvals/blacklist
```

### 2.11 SSI 模板的执行原理
SSI 在 Nginx 中属于 **子请求（subrequest）** 机制：

```
请求 /index.html
  │
  ├── Nginx 读取文件内容
  ├── 解析 SSI 指令
  │     ├── <!--# include virtual="/header.html" -->
  │     │     └── 创建子请求 `/header.html`
  │     ├── <!--# include virtual="/content.html" -->
  │     │     └── 创建子请求 `/content.html`
  │     └── <!--# echo var="date_local" -->
  │
  ├── 等待所有子请求完成
  └── 合并所有片段，返回完整响应
```

```nginx
location / {
    ssi on;
    # 每个 SSI 指令触发一次子请求
    # 子请求可以访问 proxy_pass / fastcgi_pass 等
    proxy_pass http://backend;
}
```

### 2.12 Nginx 的 sendfile 与 DMA 零拷贝
```nginx
sendfile on;
sendfile_max_chunk 512k;           # 单次 sendfile 最大传输量
```

**sendfile 的演进**：
```
传统 I/O（4 次拷贝，4 次上下文切换）：
磁盘 → DMA → 内核缓冲区 → CPU → 用户缓冲区 → CPU → 内核 Socket → DMA → 网卡

sendfile（3 次拷贝，2 次上下文切换）：
磁盘 → DMA → 内核缓冲区 → CPU → 内核 Socket → DMA → 网卡

sendfile + DMA Scatter/Gather（2 次拷贝，2 次上下文切换）：
磁盘 → DMA → 内核缓冲区 → DMA → 网卡
           └── 仅传输描述信息（文件位置、长度）
```

> 💡 Nginx 的 `sendfile` 通过 `splice()` 系统调用在内核空间完成数据搬运，完全不经过用户空间，是静态文件服务的核心性能保障。

---

## 三、实战场景题（10题）

### 3.1 配置 Keepalived 高可用 Nginx 双机热备
```bash
# 主节点 (192.168.1.10)
global_defs {
    router_id NGINX_MASTER
}
vrrp_script chk_nginx {
    script "/etc/keepalived/check_nginx.sh"
    interval 2
    weight -20
}
vrrp_instance VI_1 {
    state MASTER
    interface eth0
    virtual_router_id 51
    priority 100
    advert_int 1
    unicast_src_ip 192.168.1.10
    unicast_peer {
        192.168.1.11
    }
    authentication {
        auth_type PASS
        auth_pass 1234
    }
    virtual_ipaddress {
        192.168.1.100/24 dev eth0 label eth0:vip
    }
    track_script {
        chk_nginx
    }
}
```

```bash
# 备用节点 (192.168.1.11)
# 与主节点基本相同，修改如下：
state BACKUP
priority 90
unicast_src_ip 192.168.1.11
unicast_peer {
    192.168.1.10
}
```

### 3.2 配置 Nginx 内核参数优化（单机 10 万并发）
```bash
# /etc/sysctl.conf 优化
# 文件句柄
fs.file-max = 2000000

# 网络核心
net.core.somaxconn = 65535
net.core.netdev_max_backlog = 65535
net.core.rmem_default = 87380
net.core.wmem_default = 87380
net.core.rmem_max = 16777216
net.core.wmem_max = 16777216

# TCP 参数
net.ipv4.tcp_max_syn_backlog = 65535
net.ipv4.tcp_syn_retries = 2
net.ipv4.tcp_synack_retries = 2
net.ipv4.tcp_fin_timeout = 15
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_keepalive_time = 300
net.ipv4.tcp_keepalive_intvl = 10
net.ipv4.tcp_keepalive_probes = 3
net.ipv4.tcp_max_tw_buckets = 20000
net.ipv4.tcp_fastopen = 3
net.ipv4.tcp_rmem = 4096 87380 16777216
net.ipv4.tcp_wmem = 4096 87380 16777216
net.ipv4.tcp_mtu_probing = 1

# 内存
vm.swappiness = 10
vm.dirty_ratio = 20
vm.dirty_background_ratio = 5
```

```nginx
# worker 优化
worker_processes auto;
worker_rlimit_nofile 65535;

events {
    worker_connections 65535;
    use epoll;
    multi_accept on;
    accept_mutex on;
    accept_mutex_delay 100ms;
}
```

### 3.3 配置基于 GEOIP 的全球 CDN 调度
```nginx
# 安装和加载 GEOIP 模块
load_module modules/ngx_http_geoip_module.so;

http {
    geoip_country /usr/share/GeoIP/GeoIP.dat;
    geoip_city    /usr/share/GeoIP/GeoLiteCity.dat;

    # 中国地区
    geo $is_china {
        default 0;
        CN 1;
    }

    # 根据国家分发到不同集群
    upstream china_cluster {
        server cn-beijing-1.example.com;
        server cn-beijing-2.example.com;
        server cn-shanghai-1.example.com;
    }

    upstream asia_cluster {
        server sg-1.example.com;
        server jp-1.example.com;
    }

    upstream global_cluster {
        server us-west-1.example.com;
        server us-east-1.example.com;
        server eu-frankfurt-1.example.com;
    }

    server {
        listen 80;
        server_name www.example.com;

        location / {
            if ($geoip_country_code = "CN") {
                proxy_pass http://china_cluster;
                break;
            }
            if ($geoip_country_code ~ "^(JP|KR|SG|HK|TW)$") {
                proxy_pass http://asia_cluster;
                break;
            }
            proxy_pass http://global_cluster;
        }
    }
}
```

### 3.4 配置多级缓存架构（浏览器 + Nginx + OpenResty + Redis）
```nginx
# 第一级：浏览器缓存
location ~* \.(css|js|png|jpg|ico|svg)$ {
    expires 7d;
    add_header Cache-Control "public, immutable";
}

# 第二级：Nginx 代理缓存
proxy_cache_path /data/nginx/cache levels=1:2 keys_zone=nginx_cache:10g
                 max_size=100g inactive=7d use_temp_path=off;

location / {
    proxy_cache nginx_cache;
    proxy_cache_key "$host$uri";
    proxy_cache_valid 200 10m;
    proxy_cache_use_stale error timeout updating;
    add_header X-Cache-Status $upstream_cache_status;

    # 第三级：OpenResty + Redis（动态内容缓存）
    # 见下方的 Lua 代码
    proxy_pass http://app_cluster;
}

# OpenResty Lua 缓存逻辑
location /api/ {
    # 优先查 Redis 缓存
    access_by_lua_block {
        local redis = require "resty.redis"
        local red = redis:new()
        red:set_timeouts(1000, 1000, 1000)
        local ok, err = red:connect("redis-cluster", 6379)
        if ok then
            local cache_key = "api:" .. ngx.var.uri
            local cached = red:get(cache_key)
            if cached and cached ~= ngx.null then
                ngx.header["X-Cache"] = "redis-hit"
                ngx.say(cached)
                return ngx.exit(200)
            end
            red:set_keepalive(60000, 100)
        end
    }

    # Redis 未命中 → 请求后端
    proxy_pass http://api_backend;

    # 后端响应后写入 Redis 缓存
    body_filter_by_lua_block {
        if ngx.status == 200 and ngx.ctx.buffer then
            local redis = require "resty.redis"
            local red = redis:new()
            red:set_timeouts(1000, 1000, 1000)
            local ok, err = red:connect("redis-cluster", 6379)
            if ok then
                local cache_key = "api:" .. ngx.var.uri
                red:setex(cache_key, 600, ngx.ctx.buffer)   # 10 分钟过期
                red:set_keepalive(60000, 100)
            end
        end
    }
}
```

### 3.5 配置 OpenResty 综合网关（鉴权 + 限流 + 缓存 + 降级）
```nginx
http {
    # 共享字典
    lua_shared_dict rate_limit 10m;
    lua_shared_dict token_cache 50m;

    server {
        listen 80;
        server_name gateway.example.com;

        # 鉴权
        access_by_lua_block {
            local token = ngx.var.http_authorization
            if not token then
                ngx.status = 401
                ngx.say('{"code":401,"msg":"Missing auth token"}')
                return ngx.exit(401)
            end

            -- 校验 token（缓存）
            local dict = ngx.shared.token_cache
            local cached = dict:get(token)
            if not cached then
                -- 调用认证服务
                local res = ngx.location.capture("/auth/verify", {
                    body = token,
                    method = ngx.HTTP_POST,
                })
                if res.status ~= 200 then
                    ngx.status = 401
                    ngx.say('{"code":401,"msg":"Invalid token"}')
                    return ngx.exit(401)
                end
                dict:set(token, "valid", 300)  -- 缓存 5 分钟
            end
        }

        # 限流（针对 API Key）
        limit_by_lua_block {
            local key = ngx.var.http_x_api_key
            if not key then key = ngx.var.remote_addr end

            local dict = ngx.shared.rate_limit
            local current = dict:get(key)
            if current and current >= 100 then
                ngx.status = 429
                ngx.say('{"code":429,"msg":"Rate limit exceeded"}')
                return ngx.exit(429)
            end

            if current then
                dict:incr(key, 1)
            else
                dict:set(key, 1, 60)  -- 60 秒过期
            end
        }

        # 内容生成 + 缓存
        location /data/ {
            content_by_lua_block {
                local cache = ngx.shared.token_cache
                local uri = ngx.var.uri

                -- 尝试缓存命中
                local data = cache:get("data:" .. uri)
                if data then
                    ngx.header["X-Cache"] = "lua-hit"
                    ngx.say(data)
                    return
                end

                -- 缓存未命中 → 子请求获取后端数据
                local res = ngx.location.capture("/backend/internal" .. uri, {
                    args = ngx.var.args
                })
                if res.status == 200 then
                    cache:set("data:" .. uri, res.body, 30)
                    ngx.header["X-Cache"] = "miss"
                    ngx.say(res.body)
                else
                    -- 降级：返回过期数据（如果有）
                    local stale = cache:get("stale:" .. uri)
                    if stale then
                        ngx.header["X-Cache"] = "stale"
                        ngx.say(stale)
                        return
                    end
                    ngx.status = res.status
                    ngx.say(res.body)
                end
            }
        }
    }
}
```

### 3.6 配置 Nginx + Stream 模块代理 MySQL 集群
```nginx
stream {
    # 日志格式
    log_format stream '$remote_addr [$time_local] $protocol $status $bytes_sent '
                      '$bytes_received $session_time "$upstream_addr"';
    access_log /var/log/nginx/stream.log stream buffer=64k;

    upstream mysql_cluster {
        server 192.168.1.10:3306 max_fails=3 fail_timeout=10s;
        server 192.168.1.11:3306 max_fails=3 fail_timeout=10s;
        server 192.168.1.12:3306 backup;            # 热备
    }

    # 读写分离代理
    server {
        listen 3306;
        proxy_pass mysql_cluster;
        proxy_connect_timeout 5s;
        proxy_timeout 30s;
        proxy_buffer_size 16k;
        proxy_socket_keepalive on;
    }

    # 读库代理（多个只读实例）
    upstream mysql_readonly {
        server 192.168.1.20:3306;
        server 192.168.1.21:3306;
        server 192.168.1.22:3306;
    }

    server {
        listen 3307;
        proxy_pass mysql_readonly;
    }

    # Redis 代理
    upstream redis_cluster {
        server 192.168.1.30:6379;
        server 192.168.1.31:6379;
    }

    server {
        listen 6379;
        proxy_pass redis_cluster;
    }
}
```

### 3.7 配置 OpenResty + Redis + MySQL 模板引擎
```lua
-- /usr/local/openresty/nginx/lua/app.lua
local redis = require "resty.redis"
local mysql = require "resty.mysql"
local cjson = require "cjson"
local template = require "resty.template"

-- 获取缓存数据
local function get_from_redis(key)
    local red = redis:new()
    red:set_timeouts(1000, 1000, 1000)
    local ok, err = red:connect("127.0.0.1", 6379)
    if not ok then return nil, err end
    local data, err = red:get(key)
    red:set_keepalive(60000, 100)
    return data, err
end

-- 设置缓存
local function set_to_redis(key, value, ttl)
    local red = redis:new()
    red:set_timeouts(1000, 1000, 1000)
    local ok, err = red:connect("127.0.0.1", 6379)
    if not ok then return false, err end
    local res, err = red:setex(key, ttl or 60, value)
    red:set_keepalive(60000, 100)
    return res, err
end

-- 查询数据库
local function query_mysql(sql)
    local db = mysql:new()
    db:set_timeout(1000)
    local ok, err = db:connect({
        host = "127.0.0.1",
        port = 3306,
        database = "app",
        user = "app",
        password = "password"
    })
    if not ok then return nil, err end
    local res, err = db:query(sql)
    db:set_keepalive(10000, 50)
    return res, err
end

-- 处理请求
local function handle_request()
    local uri = ngx.var.uri

    -- 尝试从 Redis 获取缓存
    local cached, err = get_from_redis("page:" .. uri)
    if cached then
        ngx.header["X-Cache"] = "redis"
        ngx.say(cached)
        return
    end

    -- 查询数据库
    local users, err = query_mysql("SELECT * FROM users LIMIT 10")
    if not users then
        ngx.status = 500
        ngx.say("Database error")
        return
    end

    -- 使用模板引擎渲染
    local html = template.render("list.html", {
        title = "User List",
        users = users,
        count = #users
    })

    -- 写入 Redis 缓存
    set_to_redis("page:" .. uri, html, 60)

    ngx.header["X-Cache"] = "miss"
    ngx.say(html)
end

handle_request()
```

### 3.8 配置 Tengine 主动健康检查
```nginx
# Tengine 特有功能：主动健康检查
upstream backend {
    server 192.168.1.10:8080;
    server 192.168.1.11:8080;

    # 主动健康检查
    check interval=3000 rise=2 fall=5 timeout=1000 type=http;
    check_http_send "GET /health HTTP/1.0\r\n\r\n";
    check_http_expect_alive http_2xx http_3xx;
    check_keepalive_requests 100;

    # 健康检查状态页面
    check_status /status;
}

server {
    listen 80;
    location / {
        proxy_pass http://backend;
        proxy_next_upstream error timeout http_500;
    }

    # 健康检查状态页面（需限制访问）
    location /status {
        check_status;
        access_log off;
        allow 127.0.0.1;
        allow 10.0.0.0/8;
        deny all;
    }
}
```

### 3.9 配置 Nginx 日志分析 + 实时告警
```nginx
# nginx.conf - 日志配置
log_format extended '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for" '
                    '$request_time $upstream_response_time $upstream_addr';

# 错误日志按模块分级
error_log /var/log/nginx/error.log warn;
error_log /var/log/nginx/error_critical.log crit;

# 单独记录 4xx/5xx 请求
access_log /var/log/nginx/access.log main;
access_log /var/log/nginx/error_access.log combined if=$loggable;

# map 判断是否需要记录
map $status $loggable {
    ~^[4-5]  1;
    default  0;
}
```

```bash
# 日志分析脚本 (analyze_log.sh)
#!/bin/bash
LOG=/var/log/nginx/access.log
DATE=$(date +%Y-%m-%d)

echo "=== Nginx Log Analysis: $DATE ==="

# QPS 峰值
echo "Peak QPS:"
cat $LOG | awk '{print $4}' | cut -d: -f2 | sort -n | uniq -c |
    sort -rn | head -5

# 5xx 错误
echo "5xx errors:"
grep -cE ' "5[0-9]{2} ' $LOG

# 最慢接口（超过 5s）
echo "Slow requests (>5s):"
cat $LOG | awk '$NF > 5 {print $NF, $7}' | sort -rn | head -10

# upstram 响应时间分布
echo "Upstream response time distribution:"
cat $LOG | awk '{print $NF}' | awk '{
    if ($1 < 0.1) a++
    else if ($1 < 0.5) b++
    else if ($1 < 1) c++
    else if ($1 < 3) d++
    else e++
} END {
    printf "<0.1s: %d\n0.1-0.5s: %d\n0.5-1s: %d\n1-3s: %d\n>3s: %d\n", a, b, c, d, e
}'
```

### 3.10 配置 Nginx 断点续传 + 大文件下载
```nginx
server {
    listen 80;
    server_name download.example.com;
    root /var/www/downloads;

    # 大文件下载
    location /files/ {
        # 断点续传支持
        add_header Accept-Ranges bytes;
        add_header ETag $1_$2;               # 文件唯一标识

        # 限速
        limit_rate 1m;
        limit_rate_after 50m;                # 前 50MB 不限速

        # 并发限制
        limit_conn download_conn 3;
        limit_conn_zone $binary_remote_addr zone=download_conn:10m;

        # 不缓存大文件
        proxy_buffering off;

        # 日志格式（记录下载量）
        log_format download '$remote_addr [$time_local] "$request" '
                            '$status $body_bytes_sent "$http_range" '
                            '$upstream_http_content_range';
        access_log /var/log/nginx/download.log download;
    }
}
```

---

## 四、手写代码/配置文件题（6题）

### 4.1 手写完整的 OpenResty Lua 网关限流 + 缓存
```lua
-- /usr/local/openresty/nginx/lua/gateway.lua

local redis = require "resty.redis"
local cjson = require "cjson"

-- 配置
local config = {
    rate_limit = 100,          -- 每秒最大请求数
    burst = 200,               -- 突发请求数
    cache_ttl = 60,            -- 缓存时间（秒）
    redis_host = "127.0.0.1",
    redis_port = 6379,
    redis_timeout = 1000,
}

-- 限流检查
local function check_rate_limit(client_ip)
    local red = redis:new()
    red:set_timeouts(config.redis_timeout, config.redis_timeout, config.redis_timeout)
    local ok, err = red:connect(config.redis_host, config.redis_port)
    if not ok then
        ngx.log(ngx.ERR, "redis connect failed: ", err)
        return true    -- 缓存失败则放行
    end

    local key = "ratelimit:" .. client_ip
    local current = red:get(key)
    if current then
        current = tonumber(current)
        if current >= config.rate_limit + config.burst then
            red:set_keepalive(60000, 100)
            return false   -- 限流
        end
        red:incr(key)
    else
        red:setex(key, 1, 1)  -- 每秒过期
    end
    red:set_keepalive(60000, 100)
    return true
end

-- 缓存获取
local function get_cache(key)
    local red = redis:new()
    red:set_timeouts(config.redis_timeout, config.redis_timeout, config.redis_timeout)
    local ok, err = red:connect(config.redis_host, config.redis_port)
    if not ok then return nil end
    local data, err = red:get(key)
    red:set_keepalive(60000, 100)
    return data
end

-- 缓存设置
local function set_cache(key, value)
    local red = redis:new()
    red:set_timeouts(config.redis_timeout, config.redis_timeout, config.redis_timeout)
    local ok, err = red:connect(config.redis_host, config.redis_port)
    if not ok then return end
    red:setex(key, config.cache_ttl, value)
    red:set_keepalive(60000, 100)
end

-- 主处理逻辑
local client_ip = ngx.var.remote_addr
local cache_key = "cache:" .. ngx.var.uri

-- 1. 限流检查
if not check_rate_limit(client_ip) then
    ngx.status = 429
    ngx.header["Content-Type"] = "application/json"
    ngx.say(cjson.encode({
        code = 429,
        message = "Rate limit exceeded",
        retry_after = 1
    }))
    return ngx.exit(429)
end

-- 2. 缓存检查
local cached = get_cache(cache_key)
if cached and cached ~= "null" then
    ngx.header["X-Cache"] = "HIT"
    ngx.say(cached)
    return ngx.exit(200)
end

-- 3. 子请求获取数据
local res = ngx.location.capture("/backend" .. ngx.var.uri)
if res.status == 200 then
    set_cache(cache_key, res.body)
    ngx.header["X-Cache"] = "MISS"
    ngx.say(res.body)
else
    ngx.status = res.status
    ngx.say(res.body)
end
```

### 4.2 手写 keepalived 高可用 + Nginx 双主模式
```nginx
# /etc/keepalived/keepalived.conf (节点 A)
global_defs {
    router_id NGINX_A
}

vrrp_script chk_nginx {
    script "/etc/keepalived/chk_nginx.sh"
    interval 2
    weight -20
}

# VIP1（A 主 B 备）— 业务入口 1
vrrp_instance VI_1 {
    state MASTER
    interface eth0
    virtual_router_id 51
    priority 100
    advert_int 1
    virtual_ipaddress {
        192.168.1.100/24
    }
    track_script { chk_nginx }
}

# VIP2（B 主 A 备）— 业务入口 2（双主模式）
vrrp_instance VI_2 {
    state BACKUP
    interface eth0
    virtual_router_id 52
    priority 90
    advert_int 1
    virtual_ipaddress {
        192.168.1.101/24
    }
    track_script { chk_nginx }
}
```

```nginx
# /etc/keepalived/keepalived.conf (节点 B)
global_defs {
    router_id NGINX_B
}

vrrp_script chk_nginx {
    script "/etc/keepalived/chk_nginx.sh"
    interval 2
    weight -20
}

# VIP1（A 主 B 备）
vrrp_instance VI_1 {
    state BACKUP
    interface eth0
    virtual_router_id 51
    priority 90
    advert_int 1
    virtual_ipaddress {
        192.168.1.100/24
    }
    track_script { chk_nginx }
}

# VIP2（B 主 A 备）
vrrp_instance VI_2 {
    state MASTER
    interface eth0
    virtual_router_id 52
    priority 100
    advert_int 1
    virtual_ipaddress {
        192.168.1.101/24
    }
    track_script { chk_nginx }
}
```

> 💡 双主模式资源利用率从 50% 提升到 100%，每个节点处理部分流量，故障时全部流量转移到健康节点。

### 4.3 手写 Nginx 全局安全加固配置
```nginx
# 隐藏版本号
server_tokens off;

# 禁用不安全的 HTTP 方法
if ($request_method !~ ^(GET|HEAD|POST|PUT)$) {
    return 405;
}

# 限制请求体大小
client_max_body_size 10m;
client_body_buffer_size 128k;

# 缓冲区溢出防护
client_body_timeout 10s;
client_header_timeout 10s;
send_timeout 10s;

# SSL 安全
ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
ssl_prefer_server_ciphers on;
ssl_session_cache shared:SSL:10m;
ssl_session_timeout 10m;

# HTTP 安全头
add_header X-Frame-Options SAMEORIGIN always;
add_header X-Content-Type-Options nosniff always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
add_header Referrer-Policy strict-origin-when-cross-origin always;
add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;

# 限制某些路径
location ~ /\.git {
    deny all;
    log_not_found off;
    access_log off;
}

location ~ /\.env {
    deny all;
}

location = /wp-admin {
    deny all;
}

# 限制并发
limit_conn_zone $binary_remote_addr zone=conn_per_ip:10m;
limit_conn conn_per_ip 20;

# DNS 防劫持
resolver 8.8.8.8 1.1.1.1 valid=300s ipv6=off;
resolver_timeout 5s;
```

### 4.4 手写 Rsync + inotify 实时同步配置
```bash
#!/bin/bash
# /usr/local/bin/sync_static.sh
# Rsync + inotify 实时同步静态资源

SRC=/data/nginx/static
DST_USER=syncuser
DST_HOSTS=("10.0.1.11" "10.0.1.12" "10.0.1.13")

# inotifywait 监控文件变更
inotifywait -mrq --format '%w%f' -e modify,create,delete,move \
    $SRC | while read file; do

    # 获取相对路径
    rel_path=${file#$SRC/}
    echo "[$(date '+%H:%M:%S')] Changed: $rel_path"

    # 同步到所有远程节点
    for host in "${DST_HOSTS[@]}"; do
        rsync -az --delete --timeout=10 \
            -e "ssh -o StrictHostKeyChecking=no -i /root/.ssh/id_rsa" \
            $SRC/ ${DST_USER}@${host}:/data/nginx/static/ &

        # 记录同步日志
        echo "$(date '+%Y-%m-%d %H:%M:%S') $rel_path -> $host" \
            >> /var/log/nginx/sync.log
    done

    # 等待所有同步完成
    wait
done
```

```bash
# systemd 服务配置
# /etc/systemd/system/inotify-sync.service
[Unit]
Description=Nginx Static File Sync via inotify + rsync
After=network.target

[Service]
Type=simple
ExecStart=/usr/local/bin/sync_static.sh
Restart=always
RestartSec=5
User=root

[Install]
WantedBy=multi-user.target
```

### 4.5 手写 Nginx 缓存 + CDN 回源配置（企业级）
```nginx
# 缓存层配置
proxy_cache_path /data/nginx/cache levels=1:2 keys_zone=cdn_cache:20g
                 max_size=200g inactive=30d use_temp_path=off;

# 缓存 key 包含域名、路径、查询参数
proxy_cache_key "$scheme$host$uri$is_args$args";

server {
    listen 80;
    server_name cdn.example.com;

    # 缓存状态监控
    location /nginx_status {
        stub_status on;
        access_log off;
        allow 127.0.0.1;
        deny all;
    }

    # 资源请求
    location / {
        proxy_cache cdn_cache;
        proxy_cache_valid 200 302 7d;
        proxy_cache_valid 404 1h;
        proxy_cache_valid 500 0;

        # 缓存锁防止雪崩
        proxy_cache_lock on;
        proxy_cache_lock_timeout 5s;

        # 缓存更新策略
        proxy_cache_use_stale error timeout invalid_header updating http_500;
        proxy_cache_background_update on;

        # 忽略客户端缓存控制
        proxy_ignore_headers Cache-Control Expires Set-Cookie;
        proxy_ignore_client_abort on;

        # 回源
        proxy_pass http://origin_backend;

        # 调试头
        add_header X-Cache-Status $upstream_cache_status;
        add_header X-Cache-Date $upstream_response_time;

        # 限速（防止单个连接占用过多带宽）
        limit_rate 5m;
    }

    # 缓存清除 API
    location ~ ^/purge(/.*) {
        allow 127.0.0.1;
        allow 10.0.0.0/8;
        deny all;

        proxy_cache_purge cdn_cache "$scheme$host$1$is_args$args";
    }
}
```

### 4.6 手写 OpenResty 自定义负载均衡（带权重 + 健康状态）
```lua
-- /usr/local/openresty/nginx/lua/balancer.lua

local redis = require "resty.redis"
local cjson = require "cjson"

-- 后端服务器列表
local servers = {
    { host = "10.0.1.10", port = 8080, weight = 5, health = true, fail_count = 0 },
    { host = "10.0.1.11", port = 8080, weight = 3, health = true, fail_count = 0 },
    { host = "10.0.1.12", port = 8080, weight = 2, health = true, fail_count = 0 },
    { host = "10.0.1.13", port = 8080, weight = 1, health = true, fail_count = 0 },
}

local total_weight = 0
for _, s in ipairs(servers) do
    if s.health then total_weight = total_weight + s.weight end
end

-- 加权轮询算法（平滑加权）
local current_weight = {}
for i, s in ipairs(servers) do
    current_weight[i] = 0
end

function select_server()
    local total = 0
    local best = nil
    local best_idx = -1

    for i, s in ipairs(servers) do
        if s.health then
            current_weight[i] = current_weight[i] + s.weight
            total = total + s.weight
            if best == nil or current_weight[i] > current_weight[best_idx] then
                best = s
                best_idx = i
            end
        end
    end

    if best then
        current_weight[best_idx] = current_weight[best_idx] - total
        return best.host, best.port
    end
    return nil, nil
end

-- 标记服务器故障
function mark_failed(host, port)
    for _, s in ipairs(servers) do
        if s.host == host and s.port == port then
            s.fail_count = s.fail_count + 1
            if s.fail_count >= 3 then
                s.health = false
                ngx.log(ngx.WARN, "server marked unhealthy: ", host, ":", port)
                -- 启动恢复检查
                check_health_async(s)
            end
            break
        end
    end
end

-- 标记服务器恢复
function mark_healthy(host, port)
    for _, s in ipairs(servers) do
        if s.host == host and s.port == port then
            s.health = true
            s.fail_count = 0
            ngx.log(ngx.NOTICE, "server recovered: ", host, ":", port)
            break
        end
    end
end

-- 使用示例
local host, port = select_server()
if host then
    local ok, err = ngx.location.capture("/proxy", {
        upstream = {
            host = host,
            port = port,
        }
    })
    if not ok then
        mark_failed(host, port)
        -- 重试其他服务器
        host, port = select_server()
        if host then
            -- 重试逻辑
        end
    end
end
```

---

## 五、系统设计题（4题）

### 5.1 设计一个支持 100 万 QPS 的静态资源分发系统

```
DNS 智能解析 (按地域)
    │
CDN 厂商多节点 (Akamai / CloudFront / 阿里云CDN)
    │  回源
Nginx 集群 (LVS + Keepalived HA)
    │  多级缓存
    ├── Cache Tier 1: Nginx 本地缓存 (SSD)
    │         │ 缓存策略: LRU, 按文件类型、大小分级
    │         │ 内存: 10GB, 磁盘: 200GB
    │         │
    ├── Cache Tier 2: Redis/Memcached 分布式缓存
    │         │ 热点文件缓存
    │         │ 小文件 (<1MB) 全量缓存
    │         │
    └── Origin Server (源站)
          │ 使用 Rsync + inotify 同步静态资源
          │ 构建时自动推送文件到所有节点
```

**Nginx 配置要点**：
```nginx
# 100万 QPS 的 Nginx 核心优化
worker_processes auto;           # CPU 核心数
worker_rlimit_nofile 1000000;    # 文件句柄上限
worker_cpu_affinity auto;        # CPU 绑定

events {
    worker_connections 65535;
    use epoll;
    accept_mutex off;            # reuseport 模式下关闭
    multi_accept on;
}

http {
    # 访问日志缓存
    access_log off;

    # 打开文件缓存
    open_file_cache max=100000 inactive=60s;
    open_file_cache_valid 30s;
    open_file_cache_min_uses 2;
    open_file_cache_errors off;

    # sendfile 零拷贝
    sendfile on;
    sendfile_max_chunk 512k;
    tcp_nopush on;

    # 最小的请求头缓冲区
    client_header_buffer_size 1k;
    large_client_header_buffers 2 1k;

    # 关闭不必要的功能
    server_tokens off;
    server_name_in_redirect off;

    server {
        listen 80 reuseport backlog=65535;
        # 每个 worker 独立 socket，内核分发
    }
}
```

### 5.2 设计一个 API 网关 + 微服务架构的全链路方案

```
客户端 → DNS (智能调度)
    │
Nginx API Gateway (LVS + Keepalived 高可用)
    │
    ├── 流量管理
    │     ├── 限流 (按 API Key / IP / 路径)
    │     ├── 鉴权 (JWT / OAuth2)
    │     ├── 路由 (按 URL 分发到微服务)
    │     └── 降级 (熔断 + 返回缓存)
    │
    ├── 可观测性
    │     ├── 请求日志 (JSON 格式 → ELK)
    │     ├── 性能指标 (Prometheus + Grafana)
    │     └── 链路追踪 (OpenTelemetry)
    │
    └── 微服务集群
          ├── User Service
          ├── Order Service
          ├── Payment Service
          └── ...
```

**关键配置**：
```nginx
# 全链路日志
log_format fulltrace escape=json '{'
    '"trace_id":"$http_x_trace_id",'
    '"span_id":"$http_x_span_id",'
    '"timestamp":"$time_iso8601",'
    '"client":"$remote_addr",'
    '"method":"$request_method",'
    '"path":"$uri",'
    '"query":"$args",'
    '"status":$status,'
    '"request_time":$request_time,'
    '"upstream":"$upstream_addr",'
    '"upstream_status":"$upstream_status",'
    '"upstream_time":"$upstream_response_time",'
    '"body_bytes":$body_bytes_sent,'
    '"user_agent":"$http_user_agent"'
'}';
access_log /var/log/nginx/gateway.log fulltrace buffer=64k flush=3s;

# 超时控制
proxy_connect_timeout 5s;
proxy_read_timeout 30s;
proxy_send_timeout 10s;
```

### 5.3 设计一个抖音/TikTok 级别的短视频分发系统

```
上传 → 转码服务 (FFmpeg) → 对象存储 (OSS/S3)
                                       │
CDN 预热 ─→ 边缘节点 (多个 Region)
               │
            Nginx 集群
               │
          视频请求路径:
          1. CDN 边缘缓存 (HLS/MP4 切片)
          2. Nginx 本地缓存 (热视频)
          3. 分布式缓存 (Redis 记录视频元数据)
          4. 回源到 OSS (冷视频)
```

```nginx
# 视频流分发配置
proxy_cache_path /data/nginx/video_cache levels=1:2 keys_zone=video:50g
                 max_size=500g inactive=7d;

server {
    listen 80 reuseport;

    location /video/ {
        proxy_cache video;
        proxy_cache_key "$host$uri";
        proxy_cache_valid 200 206 7d;

        # HLS 分片缓存（.ts 缓存时间长，.m3u8 不缓存）
        location ~* \.ts$ {
            expires 7d;
            add_header Cache-Control "public, immutable";
        }
        location ~* \.m3u8$ {
            expires -1;
            add_header Cache-Control "no-cache";
        }

        # 视频限速（保证公平）
        limit_rate 2m;
        limit_rate_after 5m;

        proxy_pass http://oss_origin;
        add_header X-Cache-Status $upstream_cache_status;
    }
}
```

### 5.4 设计一个双 11 级别的秒杀 + 抢购系统

```
用户 → WAF → Nginx 接入层 → 缓存层 → 队列 → 服务层 → 数据库
                    │
                ┌───┴───┐
            Nginx 集群 (1000+ 节点)
                │
            ┌───┴───┐
          限流    防刷
            │       ├── IP 黑白名单
          Token     ├── User-Agent 检测
          桶限流    ├── Referer 验证
            │       └── 设备指纹校验
            │
        ┌───┴───┐
      Redis 集群 (库存预扣)
            │
     ┌──────┴──────┐
  消息队列 (削峰)  异步落库
```

```nginx
# 秒杀接入层配置
http {
    # 全球 IP 黑名单
    geo $blacklist {
        default 0;
        include /etc/nginx/geo/blacklist.conf;
    }

    # Token 桶限流（使用共享内存）
    lua_shared_dict seckill_token 10m;

    server {
        listen 80 reuseport;
        server_name seckill.example.com;

        # 请求预处理
        access_by_lua_block {
            -- 1. 黑名单检查
            if ngx.var.blacklist == "1" then
                ngx.exit(403)
            end

            -- 2. User-Agent 检查
            local ua = ngx.var.http_user_agent
            if not ua or ua == "" then
                ngx.exit(403)
            end

            -- 3. Token 桶限流
            local dict = ngx.shared.seckill_token
            local token_bucket = "tb:" .. ngx.var.remote_addr
            local tokens = dict:get(token_bucket)

            if tokens and tokens <= 0 then
                ngx.status = 429
                ngx.header["Content-Type"] = "application/json"
                ngx.say('{"code":429,"msg":"Too many requests"}')
                return ngx.exit(429)
            end

            if tokens then
                dict:decr(token_bucket, 1)
            else
                -- 初始化 5 个令牌，每秒补充
                dict:set(token_bucket, 5, 1)
            end
        }

        # 秒杀专用路径
        location /seckill/buy {
            # 只接受 POST
            limit_except POST { deny all; }

            # 限流
            limit_req zone=seckill:10m rate=5r/s burst=10 nodelay;
            limit_req_status 429;

            proxy_pass http://seckill_backend;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
}
```

---

## 六、常见坑点与最佳实践

### 6.1 keepalived 脑裂问题
**症状**：主备节点同时持有 VIP，导致双向流量混乱。

**检测脚本**：
```bash
#!/bin/bash
# /etc/keepalived/check_split_brain.sh
VIP=192.168.1.100
PEER=192.168.1.11

# 检查是否同时持有 VIP
if ip addr show | grep -q $VIP; then
    # 如果自己持有 VIP，检查对端是否也持有
    if ssh $PEER "ip addr show | grep -q $VIP" 2>/dev/null; then
        # 检测到脑裂
        if [ $(hostname) = "nginx-master" ]; then
            echo "[WARN] Split-brain detected! Forcing VIP removal on peer."
            ssh $PEER "ip addr del $VIP/24 dev eth0" 2>/dev/null
        fi
    fi
fi
```

**预防措施**：
- 使用 `unicast_peer` 而非组播
- 启用 `nopreempt` 减少切换
- 监控 VIP 持有情况并告警

### 6.2 open_file_cache 缓存溢出
```nginx
# 错误配置：缓存条目过多导致内存耗尽
open_file_cache max=9999999;   # ❌ 太大

# 正确配置：根据实际文件量设置
open_file_cache max=10000 inactive=60s;
```

### 6.3 proxy_cache 缓存穿透和雪崩
| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 缓存穿透 | 大量请求缓存未命中，直接打到后端 | `proxy_cache_lock on;` 加锁 |
| 缓存雪崩 | 大量缓存同时过期，请求涌入后端 | 设置不同过期时间 + `proxy_cache_use_stale updating` |
| 热点 key | 单个 key 被高频访问 | 本地 LRU Cache + 分布式缓存结合 |

```nginx
# 缓存防护配置
proxy_cache_lock on;
proxy_cache_lock_timeout 5s;
proxy_cache_use_stale error timeout updating;
proxy_cache_background_update on;
```

### 6.4 Lua 代码性能陷阱
| 陷阱 | 说明 | 正确做法 |
|------|------|---------|
| 全局变量 | 跨请求污染 | 使用 `local` 声明 |
| 阻塞调用 | 如 os.execute | 使用 cosocket 替代 |
| 频繁创建对象 | GC 压力 | 使用 table 池复用 |
| 大量字符串拼接 | 内存分配 | `table.concat()` 替代 `..` |
| 未使用连接池 | 每次创建连接 | `set_keepalive()` |

### 6.5 Nginx + keepalived 的常见故障
| 故障 | 原因 | 解决方案 |
|------|------|---------|
| VIP 无法启动 | 网卡名不一致 | 确认 `interface` 配置 |
| 健康检查失败 | 脚本无执行权限 | `chmod +x check_nginx.sh` |
| 主备频繁切换 | 网络抖动 | 增加 `advert_int` 或关闭抢占 |
| 虚拟路由 ID 冲突 | 子网中有其他 VRRP 实例 | 改 `virtual_router_id` |

### 6.6 高性能最佳实践总结
| 类别 | 最佳实践 |
|------|---------|
| 进程 | `worker_processes auto;` + CPU 亲和性 |
| 连接 | `worker_connections 65535;` + `ulimit -n 1000000` |
| 网络 | `reuseport` + `tcp_nopush` + `sendfile` |
| 缓存 | `open_file_cache` + `proxy_cache` + `gzip_static` |
| SSL | 会话缓存 + OCSP Stapling + Session Ticket |
| 日志 | 缓冲写入 + 切分 + JSON 格式 |
| 监控 | stub_status + Prometheus + 日志分析 |
| 安全 | 版本隐藏 + 限流 + 黑白名单 + HSTS |

---

## 七、面试回答模板（Top 5）

### 7.1 "Nginx 的高可用方案怎么设计？"
> 我使用 Keepalived + Nginx 实现双机热备。Keepalived 基于 VRRP 协议，主节点持有虚拟 IP（VIP）处理流量，备节点监控主节点的 VRRP 通告。主节点宕机后，备节点在几个通告间隔内未收到心跳，自动接管 VIP，实现秒级故障切换。我们还会配合健康检查脚本，当 Nginx 进程异常退出时自动重启或切换。双主模式可以进一步提升资源利用率，让两台机器同时承载流量。

### 7.2 "亿级流量下如何优化 Nginx？"
> 首先进行内核调优，包括 `somaxconn`、`tcp_tw_reuse`、`tcp_fastopen` 等参数。其次调整 Nginx 配置：`worker_processes auto`、`worker_connections 65535`、`reuseport`、`sendfile on`、`tcp_nopush on`。缓存方面开启 `open_file_cache`、`proxy_cache` 和 `gzip_static`。如果使用 OpenResty，还可以用 Lua + Redis 做精细化限流和缓存。单机优化到极限后，通过 LVS + Keepalived 横向扩展。最终达到单机 10 万+ 并发，集群百万级 QPS。

### 7.3 "OpenResty 和 Nginx 的区别是什么？"
> OpenResty 是 Nginx 的超集，集成了 LuaJIT 和丰富的 Lua 库，允许在 Nginx 的各个处理阶段嵌入 Lua 代码。传统 Nginx 的功能受限于静态配置和内置模块，而 OpenResty 可以实现动态路由、灵活限流、自定义缓存策略。核心优势是 cosocket 机制，用同步代码风格实现异步非阻塞 I/O，既保持了 Nginx 的高性能，又大幅降低了开发复杂度。

### 7.4 "Nginx 多级缓存怎么设计？"
> 多级缓存分四层：第一层是 CDN，缓存静态资源，减少回源。第二层是 Nginx 本地缓存，用 `proxy_cache` 缓存热点资源，响应时间 <1ms。第三层是 Redis 分布式缓存，存储动态内容和 API 数据，通过 OpenResty Lua 实现缓存策略。第四层是应用本地缓存，使用 LRU 算法。配合 `proxy_cache_use_stale updating` 和 `proxy_cache_background_update on` 防止缓存雪崩。缓存穿透用 `proxy_cache_lock` 加锁解决。

### 7.5 "Nginx 的四层代理和七层代理有什么区别？"
> 七层代理（HTTP 层）在 `http` 块中配置，能理解 HTTP 协议，按 URL、Header、Cookie 等做路由，功能丰富但性能较低。四层代理（TCP/UDP 层）在 `stream` 块中配置，基于 IP 和端口转发，不解析应用层协议，性能高、延迟低。我一般在 MySQL、Redis、DNS 这些协议使用四层代理，在 Web 应用、API 网关使用七层代理。混合使用两者可以兼顾性能和功能。

---

## 八、快速查漏补缺 Checklist

| 知识点 | 掌握程度 | 备注 |
|--------|---------|------|
| [ ] 多进程模型深入 | / | 惊群问题 + accept_mutex + reuseport |
| [ ] 请求处理 11 阶段 | / | POST_READ → SERVER_REWRITE → FIND_CONFIG → ... |
| [ ] keepalive 双端配置 | / | client keepalive vs upstream keepalive |
| [ ] Keepalived + VRRP | / | 主备 / 双主 / 脑裂处理 |
| [ ] Linux 内核调优 | / | somaxconn / tcp_tw_reuse / tcp_fastopen |
| [ ] GEOIP 地域分发 | / | geoip_country / geoip_city |
| [ ] 多级缓存架构 | / | CDN → Nginx → Redis → 应用 |
| [ ] SSI 服务端包含 | / | include / set / echo / if 指令 |
| [ ] Rsync + inotify | / | 实时文件同步方案 |
| [ ] Nginx + Redis | / | redis2-nginx-module / OpenResty |
| [ ] Nginx + Memcached | / | memcached_pass 模块 |
| [ ] Stream 四层代理 | / | TCP/UDP/MySQL/Redis 代理 |
| [ ] OpenResty 基础 | / | cosocket / shared_dict / lrucache |
| [ ] OpenResty + Redis | / | lua-resty-redis |
| [ ] OpenResty + MySQL | / | lua-resty-mysql |
| [ ] 主动健康检查 | / | Tengine check 模块 |
| [ ] 被动重试机制 | / | proxy_next_upstream |
| [ ] 日志分析与缓冲 | / | buffer/flush + JSON 格式 |
| [ ] proxy_buffering | / | 缓冲模式 vs 流式模式 |
| [ ] 断点续传 | / | Accept-Ranges + Range 头 |
| [ ] 连接池管理 | / | keepalive_requests / keepalive_timeout |
| [ ] sendfile 零拷贝 | / | DMA / Scatter-Gather / splice |
| [ ] TLS 1.3 + 0-RTT | / | 会话恢复机制 |
| [ ] 双主高可用 | / | 资源利用率 100% |

---

> 🎯 **总结**：高级篇覆盖了亿级流量场景下的 Nginx 核心技能。**高可用设计**、**内核参数调优**、**多级缓存架构**、**OpenResty/Lua 开发**是架构师级别的面试必考点。关键是在理解原理的基础上，具备从单机优化到分布式扩容的完整架构视野。
