# Nginx 面试宝典（实战篇）
> 基于课程大纲聚焦实战场景，覆盖 Brotli 压缩、限流、缓存、SSL 证书、WebSocket 等高频实用技能

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

### 1.1 What is Brotli compression? 什么是 Brotli 压缩？
Brotli 是 Google 开发的**无损压缩算法**，比 Gzip 压缩率更高（通常小 20%-30%），于 2015 年发布，已被主流浏览器广泛支持。

| 对比项 | Gzip | Brotli |
|--------|------|--------|
| 压缩率 | 较低（基准） | 比 Gzip 高 20-30% |
| 压缩速度 | 快 | 相对较慢（级别 11 时） |
| 解压速度 | 快 | 与 Gzip 接近 |
| 字典支持 | 无 | 预定义字典（HTML/CSS/JS 等） |
| MIME Type | `application/gzip` | `application/brotli` |
| Nginx 模块 | 内置 | 需第三方模块 `ngx_brotli` |

```nginx
# 安装 Brotli 模块后配置
brotli on;
brotli_comp_level 6;
brotli_static on;                      # 优先使用预压缩 .br 文件
brotli_types text/plain text/css application/json application/javascript text/xml application/xml text/javascript image/svg+xml;
```

> 💡 实战建议：Gzip + Brotli 双开启，Nginx 根据客户端 `Accept-Encoding` 自动选择最优算法。`Accept-Encoding: br` 优先 Brotli，其次是 `gzip`。

### 1.2 How does Nginx implement rate limiting? Nginx 如何实现限流？
Nginx 通过 `ngx_http_limit_req_module` 实现**请求频率限制**。

```nginx
# 定义限流区域（共享内存）
limit_req_zone $binary_remote_addr zone=mylimit:10m rate=10r/s;

server {
    location /api/ {
        limit_req zone=mylimit burst=20 nodelay;
        limit_req_status 429;           # 自定义限流状态码
        proxy_pass http://backend;
    }
}
```

| 配置项 | 含义 | 示例 |
|--------|------|------|
| `limit_req_zone` | 定义限流区域和速率 | `rate=10r/s`（每秒 10 次） |
| `zone` | 共享内存区域名称和大小 | `mylimit:10m`（10MB） |
| `burst` | 突发请求缓冲队列 | `burst=20`（最多缓冲 20 个） |
| `nodelay` | 不延迟处理突发请求 | 不加 nodelay 则会排队延迟处理 |
| `limit_req_status` | 限流响应状态码 | 默认 503，推荐 429 |

> ⚠️ **高频面试点**：`burst=20 nodelay` 表示允许瞬间 20 个突发请求按正常速率处理，超出 20 个的部分直接拒绝（返回 429）。如果去掉 `nodelay`，超出的请求会排队延迟处理，相当于降速。

### 1.3 What is the leaky bucket algorithm? 什么是漏桶算法？
漏桶算法（Leaky Bucket）是 Nginx `limit_req` 的默认实现：

```
请求 → [  桶  ] → 匀速流出 → 后端服务
         ↑
      漏水速率
```

**核心思想**：
- 请求以任意速率进入桶
- 桶以固定速率漏水（处理请求）
- 桶满则溢出（拒绝请求）

```nginx
# 漏桶算法配置示例
limit_req_zone $binary_remote_addr zone=leaky:10m rate=5r/s;

location / {
    limit_req zone=leaky burst=10;           # 不加 nodelay = 漏桶
    # ⚡ burst=10 表示桶容量 10，超出则排队
}
```

### 1.4 What is the token bucket algorithm? 什么是令牌桶算法？
令牌桶算法（Token Bucket）相比漏桶算法，允许一定程度的突发流量：

```
令牌以固定速率放入桶
     ↓
[ 令牌桶 ]  ← 请求消耗令牌
     ↓
  有令牌 → 处理请求
  无令牌 → 拒绝/排队
```

**与漏桶的区别**：
| 特性 | 漏桶算法 | 令牌桶算法 |
|------|---------|-----------|
| 输出速率 | 严格固定 | 允许突发 |
| 实现机制 | 请求进桶，匀速流出 | 令牌生成，请求消费 |
| 突发处理 | 排队降速 | 积累令牌后突发 |
| Nginx 支持 | `limit_req` 默认 | 配合 `nodelay` 模拟 |

> 💡 Nginx 的 `limit_req` 实际是**漏桶实现**（请求排队匀速处理），但加上 `nodelay` 后行为更接近令牌桶（允许突发，超出的直接拒绝）。

### 1.5 How to configure connection limiting? 如何配置连接数限制？
使用 `ngx_http_limit_conn_module`：

```nginx
# 定义连接数限制区域
limit_conn_zone $binary_remote_addr zone=addr:10m;

server {
    location /download/ {
        limit_conn addr 1;            # 每个 IP 只允许 1 个并发连接
        limit_conn_status 503;
        limit_rate 200k;              # 限速 200KB/s
    }
}

# 多维度限制
limit_conn_zone $server_name zone=server:10m;
limit_conn_zone $binary_remote_addr zone=perip:10m;

server {
    limit_conn server 100;            # 整个 server 不超过 100 连接
    limit_conn perip 10;              # 每个 IP 不超过 10 连接
}
```

### 1.6 What is the concat module for merging requests? 合并请求的 concat 模块？
Tengine 的 `concat` 模块（可移植到 Nginx）用于合并多个 CSS/JS 请求为一个，减少 HTTP 请求数。

```nginx
# concat 模块配置
location /static/ {
    concat on;
    concat_max_files 20;              # 最多合并 20 个文件
    concat_unique on;                 # 同类型合并
    concat_types text/css application/javascript;
    concat_delimiter ';';             # 文件间分隔符
}

# 使用方式：原多个请求合并为一个
# 原：GET /a.css 和 GET /b.css
# 合：GET /static/??a.css,b.css
```

### 1.7 How to configure proxy cache? 如何配置反向代理缓存？
```nginx
# 定义缓存路径和参数
proxy_cache_path /data/nginx/cache levels=1:2 keys_zone=mycache:10m
                 max_size=10g inactive=60m use_temp_path=off;

server {
    location / {
        proxy_cache mycache;                     # 使用 mycache 缓存区
        proxy_cache_key "$scheme$request_method$host$request_uri";
        proxy_cache_valid 200 302 60m;           # 200/302 缓存 60 分钟
        proxy_cache_valid 404 1m;                # 404 缓存 1 分钟
        proxy_cache_valid any 5m;                # 其他状态缓存 5 分钟
        proxy_cache_min_uses 3;                  # 访问 3 次才缓存
        proxy_cache_use_stale error timeout updating http_500 http_502 http_503 http_504;
        proxy_cache_background_update on;        # 后台异步更新缓存
        proxy_no_cache $cookie_nocache;          # 带特定 cookie 不缓存
        proxy_cache_bypass $http_pragma;         # 绕过缓存

        add_header X-Cache-Status $upstream_cache_status;  # 调试缓存命中状态
        proxy_pass http://backend;
    }
}
```

| `$upstream_cache_status` | 含义 |
|-------------------------|------|
| `MISS` | 未命中 |
| `HIT` | 命中 |
| `EXPIRED` | 过期 |
| `STALE` | 过期但使用旧缓存 |
| `UPDATING` | 后台更新中 |
| `BYPASS` | 绕过缓存 |

### 1.8 How to purge proxy cache? 如何清理代理缓存？
```nginx
# 方案一：手动删除缓存文件
rm -rf /data/nginx/cache/*

# 方案二：使用 ngx_cache_purge 模块
location ~ /purge(/.*) {
    allow 127.0.0.1;
    allow 192.168.1.0/24;
    deny all;
    proxy_cache_purge mycache "$scheme$request_method$host$1";
}

# 方案三：设置缓存不缓存特定 URL
location /no-cache/ {
    proxy_no_cache 1;
    proxy_cache_bypass 1;
}
```

> 💡 生产环境推荐配合管理后台，通过 ngx_cache_purge 模块定向清理特定 URL 的缓存。

### 1.9 What are the limitations of using a reverse proxy to avoid ICP filing? 反向代理免备案的限制？
在中国大陆，所有网站必须 ICP 备案。可以使用**反向代理**的方式，将未备案域名的请求代理到已备案的服务器，但存在以下风险：

| 风险 | 说明 |
|------|------|
| 合规风险 | 本质上是规避备案，接入商可随时阻断 |
| HTTPS 问题 | 需要处理证书绑定 |
| 搜索引擎 | 搜索引擎不收录非备案域名 |
| 运营商 | 运营商可能直接封锁 |

```nginx
# 免备案反向代理示例（仅用于学习/测试场景）
server {
    listen 80;
    server_name my-unregistered-domain.com;

    location / {
        proxy_pass https://registered-domain.com;
        proxy_set_header Host $proxy_host;       # 修改 Host
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 1.10 How to obtain a free SSL certificate with Let's Encrypt? 如何用 Let's Encrypt 获取免费证书？
```bash
# 1. 安装 certbot
sudo apt install certbot python3-certbot-nginx

# 2. 获取证书（自动配置 Nginx）
sudo certbot --nginx -d example.com -d www.example.com

# 3. 手动获取
sudo certbot certonly --webroot -w /var/www/html -d example.com

# 4. 证书路径
/etc/letsencrypt/live/example.com/fullchain.pem
/etc/letsencrypt/live/example.com/privkey.pem

# 5. 自动续期
sudo certbot renew --dry-run    # 测试续期
# certbot 默认每天检查两次，过期前 30 天自动续期
```

```nginx
# Nginx 配置引用 Let's Encrypt 证书
server {
    listen 443 ssl;
    server_name example.com;

    ssl_certificate     /etc/letsencrypt/live/example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/example.com/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
}
```

### 1.11 How to configure a WebSocket proxy? 如何配置 WebSocket 反向代理？
```nginx
upstream ws_backend {
    hash $binary_remote_addr consistent;   # WebSocket 需要会话保持
    server 192.168.1.10:8080;
    server 192.168.1.11:8080;
}

server {
    listen 80;
    server_name ws.example.com;

    location /ws/ {
        proxy_pass http://ws_backend;
        proxy_http_version 1.1;               # WebSocket 必须 HTTP/1.1
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 3600s;             # 长连接超时（必须设置足够大）
        proxy_send_timeout 3600s;
    }
}
```

> ⚠️ **关键点**：WebSocket 代理必须设置 `proxy_http_version 1.1` 和 `Connection "upgrade"`，否则握手会失败。`proxy_read_timeout` 要设置足够大（建议 3600s+），避免长时间空闲连接被断开。

### 1.12 What is X-Forwarded-For? X-Forwarded-For 头的作用？
`X-Forwarded-For` 是一个 HTTP 扩展头部，用于标识通过代理连接到服务器的客户端原始 IP。

```
X-Forwarded-For: <client>, <proxy1>, <proxy2>
```

```nginx
location / {
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

| 变量 | 含义 |
|------|------|
| `$remote_addr` | 直连客户端的 IP（不经过代理时） |
| `$proxy_add_x_forwarded_for` | 追加当前客户端 IP 到已有 XFF 之后 |

### 1.13 How to configure Nginx log format in JSON? 如何配置 JSON 格式日志？
```nginx
log_format json escape=json '{'
    '"time_local":"$time_local",'
    '"remote_addr":"$remote_addr",'
    '"remote_user":"$remote_user",'
    '"request":"$request",'
    '"status":$status,'
    '"body_bytes_sent":$body_bytes_sent,'
    '"request_time":$request_time,'
    '"http_referer":"$http_referer",'
    '"http_user_agent":"$http_user_agent",'
    '"http_x_forwarded_for":"$http_x_forwarded_for",'
    '"upstream_addr":"$upstream_addr",'
    '"upstream_status":"$upstream_status",'
    '"upstream_response_time":"$upstream_response_time"'
'}';

access_log /var/log/nginx/access.json.log json buffer=32k;
```

> 💡 JSON 格式日志有利于日志收集系统（如 ELK、Loki）直接解析。

### 1.14 What is proxy_buffering? proxy_buffering 的作用？
```nginx
location / {
    proxy_buffering on;                # 开启缓冲（默认）
    proxy_buffer_size 4k;              # 响应头缓冲区
    proxy_buffers 8 4k;               # 响应体缓冲区数量与大小
    proxy_busy_buffers_size 8k;        # 向客户端发送的忙缓冲区大小
    proxy_temp_path /tmp/proxy_temp;   # 临时文件路径
    proxy_max_temp_file_size 1024m;    # 临时文件最大大小
}
```

| 模式 | 行为 | 适用场景 |
|------|------|---------|
| `proxy_buffering on` | Nginx 收完后端响应再发给客户端 | 大文件、慢客户端 |
| `proxy_buffering off` | Nginx 即时转发到客户端 | 实时流式响应、SSE、WebSocket |

### 1.15 How does Nginx support the CONNECT method? Nginx 如何支持 CONNECT 方法？
CONNECT 方法用于建立 HTTP 隧道（如 HTTPS 代理）。Nginx `stream` 模块支持 TCP 层代理：

```nginx
# 四层代理：支持 CONNECT 隧道
stream {
    server {
        listen 443;
        proxy_pass backend_server:443;
    }
}
```

### 1.16 How to configure Nginx's SSL session cache? SSL 会话缓存配置？
```nginx
ssl_session_cache shared:SSL:50m;     # 共享内存缓存 50MB（约 40000 个会话）
ssl_session_timeout 1d;               # 会话过期时间
ssl_session_tickets on;               # 会话票据（TLS 1.3 必须）

# 不同缓存类型
ssl_session_cache off;                # 关闭
ssl_session_cache builtin:1000;       # 内置缓存（进程内）
ssl_session_cache shared:SSL:10m;     # 共享缓存（跨进程，推荐）
ssl_session_cache builtin:1000 shared:SSL:10m;  # 两种都用
```

### 1.17 What HTTP/2 features does Nginx support? Nginx 的 HTTP/2 支持？
```nginx
server {
    listen 443 ssl http2;              # 开启 HTTP/2
    http2_push_preload on;             # 服务端推送预加载
}
```

**HTTP/2 核心特性**：
| 特性 | 说明 | 优势 |
|------|------|------|
| 多路复用 | 单连接并发多个请求 | 消除队头阻塞 |
| 头部压缩 | HPACK 算法压缩头部 | 减少传输量 |
| 服务端推送 | 服务器主动推送资源 | 减少请求数 |
| 二进制帧 | 二进制分帧传输 | 解析效率高 |

> ⚠️ HTTP/2 必须与 HTTPS 配合使用（浏览器要求），不支持明文 HTTP/2。

### 1.18 What is the stream module? Nginx stream 模块？
`ngx_stream_core_module` 提供四层（TCP/UDP）负载均衡和代理功能，无需进入 HTTP 协议层。

```nginx
stream {
    upstream mysql_cluster {
        server 192.168.1.10:3306;
        server 192.168.1.11:3306;
    }

    server {
        listen 3306;
        proxy_pass mysql_cluster;
        proxy_connect_timeout 5s;
        proxy_timeout 30s;
    }
}

stream {
    server {
        listen 53 udp;                 # UDP DNS 代理
        proxy_pass dns_server:53;
    }
}
```

### 1.19 How to configure Nginx's access control by IP? 如何按 IP 控制访问？
```nginx
location /admin/ {
    # 白名单模式
    allow 192.168.1.0/24;
    allow 10.0.0.0/8;
    deny all;

    proxy_pass http://admin_backend;
}

# 使用 geo 模块批量管理
geo $limited_access {
    default 1;
    192.168.0.0/16 0;
    10.0.0.0/8 0;
    172.16.0.0/12 0;
}

server {
    location / {
        if ($limited_access) {
            return 403;
        }
    }
}
```

### 1.20 What are Nginx variables? Nginx 有哪些常用内置变量？
| 变量类别 | 变量名 | 说明 |
|---------|--------|------|
| 请求信息 | `$request_method` | 请求方法（GET/POST） |
| | `$request_uri` | 原始请求 URI |
| | `$uri` | 重写后的 URI |
| | `$args` | 查询参数 |
| | `$arg_xxx` | 指定查询参数值 |
| 客户端 | `$remote_addr` | 客户端 IP |
| | `$remote_port` | 客户端端口 |
| | `$http_user_agent` | 浏览器 UA |
| | `$http_referer` | 来源 URL |
| 服务端 | `$server_name` | 服务器名称 |
| | `$host` | 请求的 Host |
| | `$scheme` | 协议（http/https） |
| 代理 | `$proxy_add_x_forwarded_for` | XFF 追加值 |
| | `$upstream_addr` | 上游服务器地址 |
| | `$upstream_status` | 上游响应状态码 |
| | `$upstream_response_time` | 上游响应时间 |

---

## 二、深度原理剖析（12题）

### 2.1 How does `limit_req` implement the leaky bucket? limit_req 如何实现漏桶算法？
Nginx 的漏桶算法实现基于**共享内存中的计数器**：

```
每毫秒处理逻辑：
1. 根据 rate 计算当前时间应该处理的请求数 (excess)
2. 如果 accumulated excess > burst（桶容量），拒绝请求
3. 否则，接受请求，excess 按 rate 速率衰减
```

```nginx
limit_req_zone $binary_remote_addr zone=api:10m rate=5r/s;
# rate=5r/s → 每 200ms 处理 1 个请求
# zone=api:10m → 10MB 共享内存（约 16 万个 IP 条目）
```

> 🎯 **面试追问**：Nginx 的 rate 支持 `r/s`（每秒）和 `r/m`（每分钟），内部实现是将速率转换为毫秒级精度的时间戳比较。

### 2.2 How does proxy_cache_key work? proxy_cache_key 如何生成？
缓存键的生成策略决定了缓存命中率：

```nginx
# 默认键
proxy_cache_key "$scheme$request_method$host$request_uri";

# 带查询参数的键
proxy_cache_key "$scheme$request_method$host$uri$is_args$args";

# 忽略特定参数
proxy_cache_key "$scheme$request_method$host$uri$is_args$args";
# 并通过 $arg_xxx 排除特定参数

# 多语言站点键
proxy_cache_key "$scheme$host$uri$is_args$args$http_accept_language";
```

> 💡 `$uri` 是重写后的 URI，`$request_uri` 是原始 URI，不含查询参数时使用 `$uri` 可提高命中率。

### 2.3 What is the difference between Gzip and Brotli compression? Gzip 和 Brotli 的区别和选择？
| 对比维度 | Gzip (deflate) | Brotli |
|---------|---------------|--------|
| 算法基础 | Deflate (LZ77 + Huffman) | LZ77 + Huffman + 二阶上下文建模 |
| 压缩级别 | 1-9 | 0-11 |
| 预定义字典 | 无 | 有（HTML/CSS/JS 等） |
| 小文件 (<1KB) | 可能变大 | 仍然有效 |
| Nginx 支持 | 内置 | 需要额外模块 |
| 浏览器支持 | 所有浏览器 | 所有现代浏览器（Chrome/Firefox/Safari/Edge） |

**最佳实践**：同时配置两种压缩，Nginx 根据 `Accept-Encoding` 头自动选择。

```nginx
gzip on;
gzip_types text/plain text/css application/json;

brotli on;
brotli_comp_level 6;
brotli_types text/plain text/css application/json;
```

### 2.4 How does Nginx's WebSocket proxy work technically? WebSocket 代理的技术原理？
WebSocket 代理依赖于 HTTP 的 **Upgrade 机制**：

```
1. Client → Nginx: GET /ws (Upgrade: websocket, Connection: Upgrade)
2. Nginx → Backend: 转发同样的 Upgrade 请求
3. Backend → Nginx: 101 Switching Protocols
4. Nginx → Client: 101 Switching Protocols
5. 此时 TCP 连接被"升级"为 WebSocket 隧道
6. Nginx 在中间透明转发双向数据帧
```

关键配置详解：
```nginx
location /ws/ {
    proxy_pass http://ws_backend;
    proxy_http_version 1.1;               # HTTP/1.1 是 WebSocket 前提
    proxy_set_header Upgrade $http_upgrade;  # 传递 Upgrade 头
    proxy_set_header Connection "upgrade";   # 固定为 upgrade

    # 心跳和超时
    proxy_read_timeout 3600s;
    proxy_send_timeout 3600s;

    # 缓冲必须关闭（WebSocket 需要实时双向）
    proxy_buffering off;

    # 避免 Nginx 缓存 WebSocket 响应
    proxy_cache off;
}
```

### 2.5 How does the cache purge module work? ngx_cache_purge 的工作原理？
1. 客户端请求 `/purge/uri` 路径
2. Nginx 收到请求后，根据 `proxy_cache_purge` 指令解析缓存键
3. 在缓存目录中查找对应的缓存文件
4. 找到后删除文件，返回 200
5. 未找到返回 404

```nginx
location ~ /purge(/.*) {
    allow 127.0.0.1;
    deny all;
    proxy_cache_purge mycache "$scheme$request_method$host$1";
}
```

> 💡 缓存目录结构：`levels=1:2` 表示缓存文件存放在 2 层子目录中（如 `/data/nginx/cache/c/29/xxxxxxxxxxxxxxxxxxxxxxxxxxxx29c`），这是为了避免单个目录中文件过多。

### 2.6 How does SSL session resumption work? SSL 会话恢复原理？
SSL 会话恢复减少完整 TLS 握手的开销，有两种机制：

| 机制 | 方式 | 是否需要会话 ID 缓存 | 适用场景 |
|------|------|---------------------|---------|
| Session ID | 服务端缓存会话 ID | 需要共享缓存 | 传统方式 |
| Session Ticket | 客户端保存加密票据 | 不需要服务端缓存 | 分布式环境 |

```nginx
ssl_session_cache shared:SSL:50m;       # 最多缓存约 40000 个会话
ssl_session_timeout 1d;                 # 会话有效期
ssl_session_tickets on;                 # 启用 Session Ticket（Nginx 1.5.9+）
ssl_session_ticket_key /etc/nginx/ticket.key;  # 多实例共享密钥
```

> 🎯 **性能提升**：完整 TLS 握手需要 2-RTT，会话恢复只需 1-RTT（Session ID）或 0-RTT（TLS 1.3 Session Ticket）。

### 2.7 What is the dynamic module mechanism in Nginx 1.9.11+? Nginx 动态模块机制？
Nginx 从 1.9.11 开始支持动态加载模块（需要编译时 `--with-http_ssl_module=dynamic`）：

```nginx
# 加载动态模块
load_module modules/ngx_http_image_filter_module.so;
load_module modules/ngx_http_geoip_module.so;
load_module modules/ngx_stream_module.so;
```

| 加载方式 | 优点 | 缺点 |
|---------|------|------|
| 静态编译 | 性能最好，无兼容问题 | 需重新编译整个 Nginx |
| 动态加载 | 灵活，按需加载 | 需 ABI 兼容，模块版本需匹配 |

### 2.8 How does Nginx implement keepalive with upstream servers? Nginx 上游 keepalive 原理？
```nginx
upstream backend {
    server backend1:8080;
    server backend2:8080;
    keepalive 32;                      # 空闲连接池大小
    keepalive_requests 100;            # 单连接最大请求数
    keepalive_timeout 60s;             # 空闲连接超时
}
```

工作原理：
1. 请求完成后，连接不立即关闭，而是放入空闲连接池
2. 下一个请求复用池中连接，减少三次握手
3. 连接数超过 `keepalive 32` 时，LRU 淘汰
4. 空闲超时后关闭连接

> 💡 配合 `proxy_http_version 1.1;` 和 `proxy_set_header Connection "";` 使用，这是启用上游 keepalive 的前提条件。

### 2.9 How does Nginx handle SSL certificate chain? Nginx 的证书链处理？
Nginx 需要三个证书文件：

```nginx
ssl_certificate     /path/to/fullchain.pem;      # 服务器证书 + 中间证书
ssl_certificate_key /path/to/privkey.pem;        # 私钥（必须保密）
```

证书链的组成：
```
最终证书 (server cert)
    ↑ 签发
中间 CA 证书 (intermediate CA)
    ↑ 签发
根 CA 证书 (root CA)  ← 浏览器内置信任
```

> ⚠️ `fullchain.pem` 必须包含服务器证书和中间证书，缺了中间证书会导致 iOS/Safari 等设备报 SSL 错误。

### 2.10 What is the merge request mechanism? 合并请求的技术原理？
合并请求（如 concat 模块、SSI）将多个小文件合并为一个响应，减少 HTTP 请求数。

**前端合并（concat 模块）**：
```
GET /static/??a.css,b.css
↓
Nginx 读取 a.css 和 b.css 的内容
↓
合并为一个响应返回（Content-Type 根据文件类型）
```

**服务端合并（SSI）**：
```html
<!--# include file="header.html" -->
<!--# include file="content.html" -->
<!--# include file="footer.html" -->
```
SSI 在 Nginx 端解析这些指令，将多个文件拼接后返回给客户端。

### 2.11 How does OCSP stapling work in Nginx? OCSP 装订原理？
OCSP（Online Certificate Status Protocol）Stapling 是证书状态查询的优化机制：

```
传统方式：
浏览器 → CA 的 OCSP 服务器：查询证书是否吊销
          ↓ 额外 DNS 查询 + HTTP 请求，增加延迟

OCSP Stapling：
Nginx 定期向 CA 查询证书状态（缓存结果）
浏览器 → Nginx：请求证书时，Nginx 随证书一起返回 OCSP 响应
          ↓ 减少一次外部请求
```

```nginx
ssl_stapling on;
ssl_stapling_verify on;
ssl_trusted_certificate /etc/letsencrypt/live/example.com/chain.pem;
resolver 8.8.8.8 119.29.29.29 valid=300s;
```

### 2.12 How does Nginx handle certificate revocation (CRL)? Nginx 的证书吊销处理？
```nginx
# 配置证书吊销列表
ssl_crl /etc/nginx/ssl/example.crl;
```

但是 CRL 存在**大小和实时性**问题，实际更推荐使用 OCSP Stapling。

---

## 三、实战场景题（10题）

### 3.1 给大型电商网站配置 Brotli + Gzip 双压缩
```nginx
http {
    # Gzip 配置
    gzip on;
    gzip_min_length 1000;
    gzip_comp_level 5;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript image/svg+xml;
    gzip_vary on;
    gzip_proxied any;

    # Brotli 配置
    brotli on;
    brotli_comp_level 6;
    brotli_types text/plain text/css application/json application/javascript text/xml application/xml text/javascript image/svg+xml;
    brotli_static on;

    # 预压缩：构建时生成 .gz 和 .br 文件
    # gzip_static on;    # 优先使用 .gz 预压缩文件
}
```

> 💡 构建阶段使用更高压缩级别（gzip 9, brotli 11）预压缩静态资源，Nginx 直接发送预压缩文件，避免实时压缩消耗 CPU。

### 3.2 配置 API 限流（按用户、按 IP、按接口）
```nginx
# 按 IP 限流
limit_req_zone $binary_remote_addr zone=ip_limit:10m rate=30r/s;

# 按用户 ID 限流（需应用传递 user-id header）
limit_req_zone $http_x_user_id zone=user_limit:10m rate=10r/s;

# 按接口路径限流
limit_req_zone $uri zone=uri_limit:10m rate=100r/m;

server {
    location /api/ {
        # 多层限流
        limit_req zone=ip_limit burst=50 nodelay;
        limit_req zone=user_limit burst=20 nodelay;
        limit_req_status 429;

        # 限流时返回 JSON 格式
        default_type application/json;
        add_header Retry-After 10;

        proxy_pass http://api_backend;
    }

    # 登录接口特殊限制
    location /api/login {
        limit_req zone=login_limit:10m rate=5r/m burst=3 nodelay;
        proxy_pass http://auth_backend;
    }
}
```

### 3.3 配置基于缓存的动静分离 + CDN 回源
```nginx
# 代理缓存
proxy_cache_path /data/nginx/cache levels=1:2 keys_zone=static_cache:10g
                 max_size=100g inactive=7d use_temp_path=off;

server {
    listen 80;
    server_name cdn.example.com;

    # 缓存静态资源
    location ~* \.(jpg|jpeg|png|gif|ico|css|js|svg|woff2?)$ {
        proxy_cache static_cache;
        proxy_cache_key "$host$uri$is_args$args";
        proxy_cache_valid 200 7d;
        proxy_cache_valid 404 1m;
        proxy_cache_use_stale error timeout updating;
        add_header X-Cache-Status $upstream_cache_status;

        # 回源到源服务器
        proxy_pass http://origin_server;
        proxy_set_header Host $host;
    }

    # 不缓存 HTML
    location / {
        proxy_no_cache 1;
        proxy_cache_bypass 1;
        proxy_pass http://origin_server;
    }
}
```

### 3.4 配置 WebSocket 在线聊天系统
```nginx
map $http_upgrade $connection_upgrade {
    default  upgrade;
    ''       close;
}

upstream chat_backend {
    ip_hash;                                  # 会话保持
    server 192.168.1.10:8080;
    server 192.168.1.11:8080;
}

server {
    listen 443 ssl http2;
    server_name chat.example.com;

    ssl_certificate /etc/letsencrypt/live/chat.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/chat.example.com/privkey.pem;

    # WebSocket 端点
    location /ws/ {
        proxy_pass http://chat_backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;

        # WebSocket 长连接
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;
        proxy_buffering off;

        # WebSocket 限流
        limit_conn conn_per_ip 5;
        limit_conn_zone $binary_remote_addr zone=conn_per_ip:10m;
    }
}
```

### 3.5 配置 Nginx 作为正向 HTTPS 代理
```nginx
server {
    listen 0.0.0.0:8888;

    resolver 8.8.8.8 114.114.114.114 valid=300s;
    resolver_timeout 10s;

    # HTTP 正向代理
    location / {
        proxy_pass $scheme://$http_host$request_uri;
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # 禁止代理内网地址（安全考虑）
        if ($http_host ~* "^(10\.|172\.(1[6-9]|2[0-9]|3[01])\.|192\.168\.)") {
            return 403;
        }

        proxy_connect_timeout 30s;
        proxy_read_timeout 60s;
    }
}

# HTTPS CONNECT 正向代理（需要 ngx_http_proxy_connect_module）
server {
    listen 0.0.0.0:8888;

    proxy_connect;
    proxy_connect_allow 443 80;
    proxy_connect_connect_timeout 10s;
    proxy_connect_read_timeout 10s;

    resolver 8.8.8.8;

    location / {
        proxy_pass $scheme://$http_host$request_uri;
        proxy_set_header Host $http_host;
    }
}
```

### 3.6 配置多级缓存架构（浏览器 + Nginx + Redis）
```nginx
# 第一级：浏览器缓存
location ~* \.(css|js|png|jpg)$ {
    expires 30d;
    add_header Cache-Control "public, immutable";
}

# 第二级：Nginx 代理缓存
proxy_cache_path /data/nginx/cache levels=1:2 keys_zone=nginx_cache:5g
                 max_size=50g inactive=60m use_temp_path=off;

location / {
    proxy_cache nginx_cache;
    proxy_cache_key "$host$scheme$request_uri";
    proxy_cache_valid 200 10m;
    proxy_cache_use_stale error timeout;
    add_header X-Cache-Status $upstream_cache_status;

    # 第三级：Redis 缓存（通过 OpenResty/Lua 实现）
    # 见 File 3 中的 OpenResty + Redis 配置
    proxy_pass http://app_backend;
}
```

### 3.7 配置 Nginx 实现灰度/蓝绿部署
```nginx
# 蓝绿部署：通过上游服务器切换
upstream blue {
    server 192.168.1.10:8080;
    server 192.168.1.11:8080;
}

upstream green {
    server 192.168.2.10:8080;
    server 192.168.2.11:8080;
}

# 默认指向蓝色环境
map $cookie_env $backend {
    default "blue";
    "green" "green";
}

server {
    location / {
        # 根据 cookie 切换环境
        if ($cookie_env = "green") {
            proxy_pass http://green;
            break;
        }
        proxy_pass http://blue;
    }
}

# 简单切换：注释掉一个 upstream，启用另一个
# 配合 nginx -s reload 实现秒级切换
```

### 3.8 配置 Let's Encrypt 自动续期 Nginx
```bash
#!/bin/bash
# /etc/cron.daily/certbot-renew.sh

# 自动续期
/usr/bin/certbot renew --quiet --post-hook "nginx -s reload"

# 查看证书状态
# certbot certificates
```

```nginx
# 为 certbot 验证提供静态资源访问
location ^~ /.well-known/acme-challenge/ {
    root /var/www/letsencrypt;
    default_type text/plain;
}
```

### 3.9 配置 Nginx 对接 Node.js 应用的 WebSocket + HTTP
```nginx
upstream node_app {
    server 127.0.0.1:3000;
    keepalive 64;
}

server {
    listen 80;
    server_name app.example.com;

    # HTTP 请求
    location / {
        proxy_pass http://node_app;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # WebSocket 请求（socket.io）
    location /socket.io/ {
        proxy_pass http://node_app;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 86400s;

        # 限流
        limit_conn conn_per_ip 3;
        limit_conn_zone $binary_remote_addr zone=conn_per_ip:10m;
    }
}
```

### 3.10 配置高性能 WordPress Nginx
```nginx
server {
    listen 80;
    server_name blog.example.com;
    root /var/www/wordpress;
    index index.php;

    # 静态资源缓存
    location ~* \.(css|js|png|jpg|jpeg|gif|ico|svg|woff2?)$ {
        expires 365d;
        add_header Cache-Control "public, immutable";
        access_log off;
        log_not_found off;
    }

    # WordPress 伪静态
    location / {
        try_files $uri $uri/ /index.php?$args;
    }

    # PHP 转发到 PHP-FPM
    location ~ \.php$ {
        fastcgi_pass unix:/var/run/php/php8.1-fpm.sock;
        fastcgi_index index.php;
        fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
        include fastcgi_params;

        # PHP 缓存
        fastcgi_cache wordpress_cache;
        fastcgi_cache_key "$scheme$request_method$host$request_uri";
        fastcgi_cache_valid 200 1h;
        fastcgi_cache_use_stale error timeout updating;
        add_header X-FastCGI-Cache $upstream_cache_status;
    }

    # 拒绝访问敏感文件
    location ~* /(\.htaccess|\.git|\.svn|wp-config\.php) {
        deny all;
    }
}
```

---

## 四、手写代码/配置文件题（6题）

### 4.1 手写完整限流配置（按 IP + URL + 并发连接）
```nginx
# 请求频率限流
limit_req_zone $binary_remote_addr zone=per_ip:10m rate=10r/s;
limit_req_zone $server_name$uri zone=per_route:10m rate=50r/m;

# 并发连接限流
limit_conn_zone $binary_remote_addr zone=conn_per_ip:10m;

server {
    listen 80;
    server_name api.example.com;

    location / {
        # 请求频率
        limit_req zone=per_ip burst=20 nodelay;
        limit_req zone=per_route burst=10 nodelay;
        limit_req_status 429;

        # 并发连接
        limit_conn conn_per_ip 10;
        limit_conn_status 503;

        # 限速
        limit_rate 1m;                    # 最大速率 1MB/s
        limit_rate_after 10m;             # 前 10MB 不限速

        proxy_pass http://backend;
    }
}
```

### 4.2 手写完整的缓存配置（带缓存清除接口）
```nginx
proxy_cache_path /data/nginx/cache levels=1:2 keys_zone=mycache:10g
                 max_size=100g inactive=7d use_temp_path=off;

server {
    listen 80;
    server_name www.example.com;

    # 主服务
    location / {
        proxy_cache mycache;
        proxy_cache_key "$scheme$request_method$host$request_uri";
        proxy_cache_valid 200 302 10m;
        proxy_cache_valid 404 1m;
        proxy_cache_valid any 1m;

        # 自定义缓存条件
        proxy_no_cache $http_x_no_cache;
        proxy_cache_bypass $http_x_purge;

        # 缓存过期后异步更新
        proxy_cache_use_stale error timeout updating http_500;
        proxy_cache_background_update on;
        proxy_cache_lock on;                  # 防雪崩
        proxy_cache_lock_timeout 5s;

        add_header X-Cache-Status $upstream_cache_status;
        proxy_pass http://backend;
    }

    # 缓存清除接口
    location ~ /purge(/.*) {
        allow 127.0.0.1;
        allow 10.0.0.0/8;
        deny all;

        proxy_cache_purge mycache "$scheme$request_method$host$1";
    }

    # 静态资源不记录缓存
    location ~* \.(css|js|jpg|png)$ {
        access_log off;
        proxy_cache mycache;
        proxy_cache_valid 200 30d;
        expires 30d;
    }
}
```

### 4.3 手写 WebSocket + HTTP 双协议代理
```nginx
map $http_upgrade $connection_upgrade {
    default upgrade;
    ''      close;
}

upstream backend {
    server 192.168.1.10:8080;
    server 192.168.1.11:8080;
    keepalive 32;
}

server {
    listen 443 ssl http2;
    server_name example.com;

    ssl_certificate     /etc/letsencrypt/live/example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/example.com/privkey.pem;

    # WebSocket 路径
    location /ws {
        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
        proxy_set_header Host $host;
        proxy_read_timeout 3600s;
        proxy_buffering off;
    }

    # 普通 HTTP API
    location /api/ {
        proxy_pass http://backend/;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # 限流
        limit_req zone=api_limit:10m rate=100r/s burst=200 nodelay;
    }
}
```

### 4.4 手写 HTTPS 完整优化配置
```nginx
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name example.com;

    # 证书路径
    ssl_certificate     /etc/letsencrypt/live/example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/example.com/privkey.pem;

    # 协议和加密套件
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers on;
    ssl_ecdh_curve secp384r1;

    # 会话缓存
    ssl_session_cache shared:SSL:50m;
    ssl_session_timeout 1d;
    ssl_session_tickets on;

    # OCSP Stapling
    ssl_stapling on;
    ssl_stapling_verify on;
    resolver 8.8.8.8 1.1.1.1 valid=300s;
    resolver_timeout 5s;

    # HSTS
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload" always;

    # 安全头
    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options SAMEORIGIN always;
    add_header X-XSS-Protection "1; mode=block" always;

    # DH 参数（生成: openssl dhparam -out /etc/nginx/ssl/dhparam.pem 4096）
    ssl_dhparam /etc/nginx/ssl/dhparam.pem;
}
```

### 4.5 手写反向代理免备案配置（学习测试场景）
```nginx
# 注意：此配置仅用于学习和测试
# 生产环境中请确保所有服务已完成 ICP 备案

server {
    listen 80;
    server_name dev-test.example.com;

    # 代理到已备案服务器
    location / {
        proxy_pass https://production.example.com;

        # 修改 Host 头部，绕过后端 Host 检查
        proxy_set_header Host production.example.com;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 禁用重定向修改
        proxy_redirect https://production.example.com/ /;
    }
}
```

### 4.6 手写代理缓存 + 限速下载配置
```nginx
proxy_cache_path /data/nginx/cache levels=1:2 keys_zone=download_cache:5g
                 max_size=50g inactive=30d use_temp_path=off;

server {
    listen 80;
    server_name download.example.com;

    # 大文件下载限速
    location /files/ {
        proxy_cache download_cache;
        proxy_cache_key "$host$uri";
        proxy_cache_valid 200 7d;

        # 限速：前 10MB 不限速，之后限速 500KB/s
        limit_rate 500k;
        limit_rate_after 10m;

        # 断点续传支持
        proxy_set_header Range $http_range;
        proxy_set_header If-Range $http_if_range;
        proxy_cache_valid 206 7d;

        # 单连接限制
        limit_conn conn_per_ip 2;
        limit_conn_zone $binary_remote_addr zone=conn_per_ip:10m;

        proxy_pass http://file_storage_server;
        add_header X-Cache-Status $upstream_cache_status;
    }
}
```

---

## 五、系统设计题（4题）

### 5.1 设计一个支持 10 万并发学生的在线考试系统
**需求**：瞬时高并发 WebSocket 连接 + API 请求 + 答案提交。

**架构方案**：
```
学生 → CDN (静态资源) → Nginx 集群 (LVS + Keepalived)
                               │
                    ┌──────────┼──────────┐
                WebSocket     HTTP API    WebSocket
               (考试作答)     (登录/查询)   (心跳)
                    │           │          │
                WS 集群      API 集群    WS 集群
```

**Nginx 配置要点**：
```nginx
upstream ws_exam {
    ip_hash;
    server 10.0.1.10:8080 max_fails=3 fail_timeout=30s;
    server 10.0.1.11:8080 max_fails=3 fail_timeout=30s;
}

upstream api_exam {
    server 10.0.2.10:8080;
    server 10.0.2.11:8080;
    keepalive 128;
}

# 限流
limit_req_zone $binary_remote_addr zone=exam_api:10m rate=30r/s;
limit_conn_zone $binary_remote_addr zone=ws_conn:10m;

server {
    listen 443 ssl http2;

    # WebSocket - 考试核心
    location /exam/ws {
        limit_conn ws_conn 3;                    # 每人最多 3 个 WS 连接
        proxy_pass http://ws_exam;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 7200s;                # 考试时长支持
    }

    # API
    location /api/ {
        limit_req zone=exam_api burst=50 nodelay;
        proxy_pass http://api_exam;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 5.2 设计一个电商秒杀系统的接入层
**需求**：双 11 级别流量，防刷单，保护后端。

```
用户 → WAF → Nginx (限流 + 黑白名单) → 缓存层 (Redis) → 队列 (MQ) → 订单服务
```

```nginx
# IP 黑名单（从 Redis/文件动态更新）
geo $blacklist {
    default 0;
    include /etc/nginx/blacklist.conf;     # 动态更新文件
}

# 限流
limit_req_zone $binary_remote_addr zone=seckill:10m rate=2r/s;
limit_req_zone $http_x_forwarded_for zone=flash:10m rate=1r/m;

server {
    # 全局黑名单
    if ($blacklist) {
        return 403;
    }

    location /seckill/ {
        # 多层限流
        limit_req zone=seckill burst=5 nodelay;
        limit_req zone=flash burst=1 nodelay;
        limit_req_status 429;

        # 仅允许 POST
        if ($request_method != POST) {
            return 405;
        }

        # 只允许特定 Referer
        valid_referers server_names ~\.example\.com;
        if ($invalid_referer) {
            return 403;
        }

        # 限流时返回 JSON
        default_type application/json;
        add_header Retry-After 3;

        proxy_pass http://seckill_backend;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 5.3 设计一个微服务 API 网关（统一认证 + 路由 + 限流 + 日志）
```nginx
# 上游服务定义
upstream user_service    { server 10.0.1.10:8080; server 10.0.1.11:8080; }
upstream order_service   { server 10.0.2.10:8080; server 10.0.2.11:8080; }
upstream payment_service { server 10.0.3.10:8080; server 10.0.3.11:8080; }

# 限流
limit_req_zone $binary_remote_addr zone=gateway:10m rate=100r/s;
limit_req_zone $http_x_uid zone=user_limit:10m rate=20r/s;

server {
    listen 443 ssl http2;
    server_name gateway.example.com;

    # JSON 格式日志
    log_format json escape=json '{'
        '"time":"$time_iso8601",'
        '"client":"$remote_addr",'
        '"method":"$request_method",'
        '"path":"$uri",'
        '"status":$status,'
        '"upstream":"$upstream_addr",'
        '"upstream_status":"$upstream_status",'
        '"request_time":"$request_time",'
        '"uid":"$http_x_uid"'
    '}';
    access_log /var/log/nginx/gateway.json.log json buffer=32k;

    location / {
        limit_req zone=gateway burst=200 nodelay;
        limit_req zone=user_limit burst=30 nodelay;

        # 路由分发
        location /api/user/ {
            proxy_pass http://user_service/;
        }
        location /api/order/ {
            proxy_pass http://order_service/;
        }
        location /api/payment/ {
            proxy_pass http://payment_service/;
        }

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Request-Id $request_id;
    }
}
```

### 5.4 设计一个视频直播系统的边缘节点
**需求**：流媒体分发，支持 RTMP/HLS/DASH，低延迟。

```nginx
# HLS 流分发
server {
    listen 80;
    server_name live.example.com;

    location /hls/ {
        # HLS 切片缓存
        alias /var/www/hls;
        expires -1;                         # 不缓存 m3u8
        add_header Cache-Control "no-cache";

        # 分片短时间缓存（TS 文件）
        location ~* \.ts$ {
            expires 30s;
        }
    }
}

# WebSocket 信令服务
location /signaling/ {
    proxy_pass http://signaling_backend;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_read_timeout 86400s;
}
```

---

## 六、常见坑点与最佳实践

### 6.1 SSL 证书配置后无法启动
```nginx
# ❌ 常见错误：证书文件路径错误或权限不足
nginx: [emerg] cannot load certificate "/etc/nginx/ssl/cert.pem"

# ✅ 排查步骤
# 1. 检查文件是否存在
ls -la /etc/nginx/ssl/cert.pem

# 2. 检查权限（Nginx worker 用户需要有读权限）
chmod 644 /etc/nginx/ssl/cert.pem
chmod 600 /etc/nginx/ssl/privkey.pem

# 3. 检查证书格式
openssl x509 -in /etc/nginx/ssl/cert.pem -text -noout
openssl rsa -in /etc/nginx/ssl/privkey.pem -check
```

### 6.2 限流失效的常见原因
| 原因 | 现象 | 解决方案 |
|------|------|---------|
| `$binary_remote_addr` 在 CDN 后都是 CDN IP | 对 CDN 节点限流而非真实用户 | 用 `$http_x_forwarded_for` |
| 限流区域过小 | 请求未被记录到共享内存 | 增加 `zone` 大小 |
| burst 设置不合理 | 突发流量直接拒绝 | 根据业务设置合理的 burst |
| 多层限流顺序错误 | 外层限流先触发 | 调整配置顺序 |

### 6.3 proxy_cache 缓存不生效
```nginx
# ❌ 常见问题：Cookies 导致不缓存
proxy_no_cache $http_cookie;     # 默认情况下带 Cookie 不缓存

# ✅ 解决方案：配置允许缓存的条件
proxy_cache_bypass $cookie_nocache;
proxy_no_cache $http_pragma;

# 或者绕过 Cookie 检查
proxy_ignore_headers Cache-Control Set-Cookie;
```

### 6.4 WebSocket 连接频繁断开
| 原因 | 解决方案 |
|------|---------|
| `proxy_read_timeout` 过小 | 设置为 3600s+ |
| Nginx 与后端超时不一致 | 确保 Nginx 和 APP 的超时配置匹配 |
| 负载均衡切换后端 | 使用 `ip_hash` 保证会话保持 |
| 未关闭缓冲 | `proxy_buffering off;` |

### 6.5 正向代理 HTTPS 连接失败
```nginx
# 需要 ngx_http_proxy_connect_module 模块支持 CONNECT
# 检查是否已安装该模块
nginx -V 2>&1 | grep proxy_connect

# 如果没有，编译安装
./configure --add-module=/path/to/ngx_http_proxy_connect_module
```

### 6.6 最佳实践清单
| 实践 | 说明 |
|------|------|
| 定期备份 nginx.conf | 使用版本管理（Git） |
| 修改配置前先 `nginx -t` | 语法检查必做 |
| 使用 `include` 拆分配置 | 模块化管理 |
| 监控 `$upstream_cache_status` | 了解缓存命中率 |
| 调整 `worker_connections` + `ulimit` | 避免 too many open files |
| 生产环境关闭 `server_tokens off` | 隐藏 Nginx 版本 |
| 使用 `proxy_next_upstream_tries` | 限制重试次数 |

---

## 七、面试回答模板（Top 5）

### 7.1 "Nginx 限流的实现原理"
> Nginx 限流基于漏桶算法，通过 `limit_req_zone` 在共享内存中维护每个 IP 的请求计数器。基本思路是：请求到达时检查当前速率是否超过配置的阈值（如 10r/s），超过的请求进入 burst 缓冲区排队，排队队列满了就直接拒绝（返回 429）。加上 `nodelay` 参数后，超出的请求不排队直接拒绝，更接近令牌桶的效果。

### 7.2 "WebSocket 反向代理怎么配？要注意什么？"
> WebSocket 代理核心是 Upgrade 机制，需要设置 `proxy_http_version 1.1`、`proxy_set_header Upgrade $http_upgrade` 和 `proxy_set_header Connection "upgrade"`。三个关键注意点：第一，`proxy_read_timeout` 要设大（至少 3600s）；第二，必须关闭 `proxy_buffering`；第三，负载均衡要用 `ip_hash` 保证会话保持。

### 7.3 "Let's Encrypt 证书和普通证书有什么区别？"
> Let's Encrypt 是免费的自动化证书颁发机构，证书有效期 90 天，需要通过 certbot 自动续期。它的优势是零成本，适合个人和小型企业。传统付费证书有效期可达 1-2 年，有商业支持。技术层面两者都是 X.509 证书，无本质区别。我使用 Let's Encrypt 配合 cron 自动续期，实现零维护的 HTTPS。

### 7.4 "Nginx 代理缓存怎么配置？清理缓存有哪些方式？"
> 代理缓存通过 `proxy_cache_path` 定义缓存存储，`proxy_cache` 在 location 中启用。关键配置包括 `keys_zone`（缓存键）、`inactive`（过期时间）、`proxy_cache_valid`（状态码缓存策略）。缓存清理有三种方式：手动删除文件、ngx_cache_purge 模块按 URL 清理、或者通过 `proxy_no_cache` 绕过缓存。

### 7.5 "Gzip 和 Brotli 怎么选？"
> 我是两者同时开启，让 Nginx 根据客户端 `Accept-Encoding` 自动选择。Brotli 压缩率平均比 Gzip 高 20-30%，但压缩速度较慢。对于构建时预压缩的静态资源，我使用 Brotli 级别 11 最高压缩；对于实时压缩的动态内容，使用 Gzip 级别 5-6 平衡性能和压缩比。实际测试中，Brotli 能减少约 15% 的传输量，对移动端用户特别有价值。

---

## 八、快速查漏补缺 Checklist

| 知识点 | 掌握程度 | 备注 |
|--------|---------|------|
| [ ] Brotli 压缩 | / | 配置 + 与 Gzip 配合 |
| [ ] 限流（limit_req） | / | 漏桶算法 + burst + nodelay |
| [ ] 连接限制（limit_conn） | / | 并发连接 + 限速 |
| [ ] 漏桶 vs 令牌桶 | / | 概念区别 |
| [ ] 合并请求（concat/SSI） | / | 减少 HTTP 请求 |
| [ ] 代理缓存 | / | proxy_cache_path + proxy_cache |
| [ ] 缓存键策略 | / | proxy_cache_key 设计 |
| [ ] 缓存清理 | / | 手动 + ngx_cache_purge |
| [ ] 免备案反向代理 | / | 原理与风险 |
| [ ] SSL 证书配置 | / | Let's Encrypt + 自动续期 |
| [ ] OCSP Stapling | / | 原理与配置 |
| [ ] SSL 会话恢复 | / | Session Cache + Session Ticket |
| [ ] WebSocket 代理 | / | Upgrade + 长连接配置 |
| [ ] HTTP/2 配置 | / | http2 + Server Push |
| [ ] Stream 模块 | / | TCP/UDP 四层代理 |
| [ ] 正向代理 | / | HTTP + HTTPS CONNECT |
| [ ] JSON 日志 | / | 日志格式优化 |
| [ ] proxy_buffering | / | 缓冲 vs 实时转发 |
| [ ] HSTS 配置 | / | 强制 HTTPS |
| [ ] 安全头配置 | / | X-Frame-Options 等 |

---

> 🎯 **总结**：实战篇的核心是掌握 Nginx 在实际业务场景中的应用能力。**限流**保护后端、**缓存**提升性能、**SSL** 保障安全、**WebSocket** 支持实时通信，是面试中最高频的四类实战问题。建议在本地搭建环境，亲手配置每个场景并验证效果。
