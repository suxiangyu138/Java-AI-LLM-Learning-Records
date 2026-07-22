# Nginx 面试宝典（基础篇）
> 基于课程大纲全面覆盖面试高频考点，从 Nginx 架构到反向代理、负载均衡、SSL 配置等核心技能

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

### 1.1 What is Nginx? 什么是 Nginx？
Nginx (engine-x) 是一款轻量级、高性能的 HTTP 和反向代理 Web 服务器，由俄罗斯程序员 Igor Sysoev 开发，最初用于解决 C10K 问题。它采用**异步事件驱动**架构，单进程可处理数万并发连接。

| Feature | Description |
|---------|-------------|
| 地位 | 全球市场份额最高的 Web 服务器之一 |
| 核心优势 | 高并发、低内存、高稳定性 |
| 主要用途 | 静态资源服务、反向代理、负载均衡、API 网关 |

> 💡 Nginx 的设计哲学：**少即是多** — 通过精巧的架构设计而非堆砌资源来解决问题。

### 1.2 What's the difference between Nginx and Apache? Nginx 与 Apache 的区别？
| 对比维度 | Nginx | Apache |
|---------|-------|--------|
| 架构模式 | 事件驱动（Event-Driven） | 进程/线程驱动（Process/Thread） |
| 并发处理 | 异步非阻塞 I/O | 同步阻塞 I/O（传统模式） |
| 静态文件 | 高效（零拷贝 sendfile） | 一般 |
| 动态内容 | 无法直接处理（需转发） | 可直接嵌入 PHP/Python 等 |
| 配置灵活性 | 较简洁 | 更灵活（.htaccess 支持） |
| 模块加载 | 静态编译 | 动态加载（DSO） |

### 1.3 What are the advantages of Nginx? Nginx 有哪些优点？
1. **高并发连接** — 单机支持 5 万 + 并发连接（理论可达 10 万+）
2. **低内存占用** — 1 万个非活跃连接仅消耗约 2.5MB 内存
3. **热部署** — 支持平滑升级，无需停机
4. **高可靠性** — master 进程管理 worker，worker 异常时自动重启
5. **丰富的功能** — 反向代理、负载均衡、缓存、限流、SSL 等

### 1.4 What is the master-worker process model? 什么是 master-worker 进程模型？
Nginx 启动后包含一个 master 进程和多个 worker 进程：

| 进程类型 | 角色 | 数量 |
|---------|------|------|
| Master | 管理进程，读取配置、管理 worker、信号处理 | 1 个 |
| Worker | 工作进程，实际处理请求 | 通常 = CPU 核心数 |
| Cache Loader | 缓存加载进程（可选） | 1 个 |
| Cache Manager | 缓存管理进程（可选） | 1 个 |

```
                 Master
              /    |    \
          Worker  Worker  Worker  (多个 worker 进程)
```

> 💡 Worker 进程之间通过共享内存（如 ngx_http_shm）通信，每个 worker 独立处理请求，互不阻塞。

### 1.5 How does Nginx handle signals? Nginx 的信号控制？
| 信号 | 含义 | 用途 |
|------|------|------|
| `TERM/INT` | 立即停止 | 强制关闭 |
| `QUIT` | 优雅停止 | 处理完当前请求后关闭 |
| `HUP` | 重载配置 | 热加载配置文件，不中断服务 |
| `USR1` | 重新打开日志 | 日志切割/轮转 |
| `USR2` | 平滑升级 | 配合新版本二进制文件升级 |
| `WINCH` | 优雅关闭 worker | 调试时关闭 worker 进程 |

```bash
# 常用信号操作
nginx -s reload          # 重载配置（等价于 kill -HUP）
nginx -s quit            # 优雅停止
nginx -s reopen          # 重新打开日志文件
nginx -t                 # 测试配置文件语法
```

### 1.6 What is the structure of nginx.conf? nginx.conf 文件结构？
Nginx 配置文件采用**分块结构**：

```nginx
# 全局块 — 全局配置
user  nginx;
worker_processes  auto;   # 自动识别 CPU 核心数
error_log  /var/log/nginx/error.log warn;
pid        /var/run/nginx.pid;

# events 块 — 网络连接配置
events {
    worker_connections  1024;     # 单 worker 最大连接数
    use epoll;                    # Linux 高并发 I/O 模型
    multi_accept on;             # 一次 accept 多个连接
}

# http 块 — HTTP 协议配置
http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile      on;
    keepalive_timeout  65;

    # server 块 — 虚拟主机配置
    server {
        listen       80;
        server_name  example.com;

        # location 块 — URL 匹配规则
        location / {
            root   /usr/share/nginx/html;
            index  index.html index.htm;
        }

        location /api/ {
            proxy_pass http://backend;
        }
    }
}
```

> ⚠️ 配置文件结构顺序：**全局块 > events > http > server > location**，外层的指令会继承到内层。

### 1.7 What directives are in the global block? 全局块有哪些常用指令？
| 指令 | 作用 | 示例 |
|------|------|------|
| `user` | 指定 worker 进程运行用户 | `user nginx;` |
| `worker_processes` | worker 进程数量 | `auto` 或数字 |
| `error_log` | 错误日志路径和级别 | `error_log logs/error.log warn;` |
| `pid` | PID 文件路径 | `pid /var/run/nginx.pid;` |
| `worker_rlimit_nofile` | worker 进程最大打开文件数 | `worker_rlimit_nofile 65535;` |

### 1.8 What directives are in the events block? events 块配置指令？
| 指令 | 说明 | 建议值 |
|------|------|--------|
| `worker_connections` | 单 worker 最大连接数 | 1024 ~ 65535 |
| `use` | I/O 多路复用模型 | Linux 用 `epoll` |
| `multi_accept` | 是否一次接受多个新连接 | `on` |
| `accept_mutex` | 互斥锁避免惊群 | `on` |

> 💡 `worker_connections` + `worker_processes` 决定了 Nginx 的最大并发能力：`max_clients = worker_processes * worker_connections / 2`（HTTP 请求一半用于接收，一半用于发送）。

### 1.9 What are listen and server_name? listen 和 server_name 的配置？
**listen** — 监听端口和地址：
```nginx
listen 80;                          # IPv4 端口 80
listen [::]:80;                     # IPv6
listen 192.168.1.1:8080;            # 指定 IP+端口
listen 443 ssl;                     # SSL 监听
listen unix:/var/run/nginx.sock;    # Unix Socket
```

**server_name** — 域名匹配（三种方式）：
| 匹配方式 | 示例 | 说明 |
|---------|------|------|
| 精确匹配 | `server_name www.example.com;` | 完全相等 |
| 通配符匹配 | `server_name *.example.com;` | 前缀或后缀通配 |
| 正则匹配 | `server_name ~^www\d+\.example\.com$;` | 使用 `~` 开头 |

**匹配优先级**：精确匹配 > 最长通配符前缀 > 最长通配符后缀 > 正则匹配（按顺序）

### 1.10 How does location matching work? location 匹配规则？
```nginx
location [=|~|~*|^~] /uri/ { ... }
```

| 修饰符 | 含义 | 优先级 |
|--------|------|--------|
| `=` | 精确匹配 | 最高 |
| `^~` | 前缀匹配（不检查正则） | 高 |
| `~` | 正则匹配（区分大小写） | 中 |
| `~*` | 正则匹配（不区分大小写） | 中 |
| 无 | 普通前缀匹配 | 低 |

> 🎯 **匹配顺序**：精确匹配 > 前缀匹配（带 `^~`）> 正则匹配（按配置顺序）> 最长前缀匹配

```nginx
location / {                    # 兜底规则
    root /data/www;
}
location /images/ {             # 前缀匹配
    root /data;
}
location ~ \.(gif|jpg)$ {      # 正则匹配
    root /data/images;
}
location = /favicon.ico {      # 精确匹配（最高优先级）
    root /data/images;
}
```

### 1.11 What's the difference between root and alias? root 与 alias 的区别？
这是面试**高频易错题**。

| 指令 | 路径拼接方式 | 示例 |
|------|-------------|------|
| `root` | root + URI | `root /data;` + URI `/images/a.jpg` → `/data/images/a.jpg` |
| `alias` | 替换匹配部分 | `alias /data;` + location `/images/` → `/data/a.jpg` |

```nginx
# root 用法
location /images/ {
    root /var/www;             # 实际路径: /var/www/images/
}

# alias 用法
location /images/ {
    alias /var/www/images/;   # 实际路径: /var/www/images/
}

# ⚠️ 错误用法 - alias 不能这样用
location /images/ {
    alias /var/www/;          # 实际路径: /var/www/a.jpg ✓
}
```

> ⚠️ `alias` 末尾必须加 `/`，而 `root` 末尾不加 `/`。另外，`alias` 不支持在 `location /`（根路径）中使用。

### 1.12 How to configure error pages? 如何配置错误页面？
```nginx
# 单条配置
error_page 404 /404.html;

# 根据状态码跳转
error_page 500 502 503 504 /50x.html;

# 配合 location
error_page 404 /404.html;
location = /404.html {
    root /usr/share/nginx/errors;
    internal;                    # 仅内部重定向可访问
}

# 自定义错误码 + 保持状态码
error_page 404 =200 /empty.gif; # 返回 200 状态码
```

### 1.13 What is sendfile? sendfile 是什么？
`sendfile` 是 Linux 系统调用，实现**零拷贝**数据传输，数据直接从内核缓冲区发送到网络 socket，无需经过用户空间。

```nginx
sendfile on;    # 开启零拷贝（静态资源服务必须开启）

# 辅助优化
tcp_nopush on;  # 优化数据包发送，减少网络小包（sendfile 需开启）
tcp_nodelay on; # 禁用 Nagle 算法，减少延迟（长连接场景）
```

| 模式 | 数据流向 | 性能 |
|------|---------|------|
| 传统 read+write | 磁盘 → 内核 → 用户 → 内核 → 网卡 | 慢（4 次拷贝，4 次上下文切换） |
| sendfile 零拷贝 | 磁盘 → 内核 → 网卡 | 快（2 次拷贝，2 次上下文切换） |

### 1.14 How does Nginx implement reverse proxy? Nginx 如何实现反向代理？
```nginx
location /api/ {
    proxy_pass http://backend_server:8080/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

| 头部 | 作用 |
|------|------|
| `Host` | 传递原始请求的 Host |
| `X-Real-IP` | 客户端真实 IP |
| `X-Forwarded-For` | 代理链路（可追加） |
| `X-Forwarded-Proto` | 原始协议（http/https） |

> 💡 `proxy_pass` 带 `/` 和不带 `/` 的区别是**高频面试题**：带 `/` 会截断匹配路径，不带 `/` 会将完整路径转发。

```nginx
location /api/ {
    proxy_pass http://backend/;       # 请求 /api/user → /user
}
location /api/ {
    proxy_pass http://backend;        # 请求 /api/user → /api/user
}
```

### 1.15 What are the load balancing strategies? Nginx 负载均衡策略有哪些？
| 策略 | 指令 | 说明 | 适用场景 |
|------|------|------|---------|
| 轮询 | 默认 | 按顺序轮流分发 | 无状态应用 |
| 加权轮询 | `weight=N` | 权重越高，分配越多 | 异构服务器集群 |
| IP Hash | `ip_hash` | 客户端 IP 哈希，会话保持 | 需要会话粘滞 |
| 最少连接 | `least_conn` | 转发给连接数最少的服务器 | 长连接场景 |
| URL Hash | `hash $request_uri` | URL 哈希，缓存友好 | 缓存服务器 |
| Fair | `fair` | 响应时间短的优先（第三方模块） | 响应时间差异大 |

```nginx
upstream backend {
    # 加权轮询
    server 192.168.1.10 weight=5;
    server 192.168.1.11 weight=3;
    # IP Hash
    ip_hash;
    # URL Hash
    hash $request_uri consistent;
    # 最少连接
    least_conn;
}

server {
    location / {
        proxy_pass http://backend;
    }
}
```

### 1.16 What upstream server states are available? 上游服务器有哪些状态？
| 状态 | 含义 | 使用场景 |
|------|------|---------|
| `down` | 永久下线，不参与负载 | 维护模式 |
| `backup` | 备用服务器，主服务器全挂时启用 | 高可用兜底 |
| `max_fails=N` | 最大失败次数，超过后标记不可用 | 故障检测 |
| `fail_timeout=T` | 失败超时时间 | 配合 max_fails 使用 |

```nginx
upstream backend {
    server 192.168.1.10 weight=5 max_fails=3 fail_timeout=30s;
    server 192.168.1.11 weight=3;
    server 192.168.1.12 down;              # 下线维护
    server 192.168.1.13 backup;            # 热备
}
```

> 🎯 面试追问：`max_fails=0` 表示不检查失败，该服务器永不标记为不可用。

### 1.17 How to configure Gzip compression? 如何配置 Gzip 压缩？
```nginx
gzip on;                         # 开启 gzip
gzip_min_length 1k;              # 小于 1KB 不压缩
gzip_comp_level 6;               # 压缩级别 1-9（6 是平衡值）
gzip_vary on;                    # 添加 Vary: Accept-Encoding
gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript image/svg+xml;
gzip_disable "msie6";            # 禁用老旧浏览器
gzip_buffers 32 4k;              # 压缩缓冲区
gzip_proxied any;                # 对代理请求也压缩
```

> 💡 `gzip_static` — 预压缩静态文件（需要 ngx_http_gzip_static_module）：
```nginx
gzip_static on;    # 优先发送 .gz 预压缩文件，减少 CPU 开销
```

### 1.18 How to configure browser cache? 如何配置浏览器缓存？
```nginx
location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
    expires 30d;                    # 过期时间
    add_header Cache-Control "public, immutable";
    add_header ETag $1_$2;          # 实体标签
}

# 强缓存 vs 协商缓存
location ~* \.(css|js)$ {
    expires 7d;                     # 强缓存 7 天
    add_header Cache-Control "public";
}

location ~* \.(html)$ {
    expires -1;                     # 不缓存 HTML
    add_header Cache-Control "no-cache";  # 每次都要验证
}
```

| 缓存类型 | 判断依据 | 请求次数 | 适用 |
|---------|---------|---------|------|
| 强缓存 | `Expires` / `Cache-Control: max-age` | 不请求服务器 | 版本稳定的静态资源 |
| 协商缓存 | `Etag` / `Last-Modified` | 请求一次（304） | HTML、动态资源 |

### 1.19 How to configure CORS cross-origin? 如何配置跨域？
```nginx
location /api/ {
    # 允许的来源
    add_header Access-Control-Allow-Origin "https://www.example.com" always;
    # 允许的请求方法
    add_header Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS";
    # 允许的请求头
    add_header Access-Control-Allow-Headers "DNT, X-CustomHeader, Keep-Alive, User-Agent, X-Requested-With, If-Modified-Since, Cache-Control, Content-Type, Authorization";
    # 预检请求缓存时间（秒）
    add_header Access-Control-Max-Age 1728000;

    # 处理预检请求
    if ($request_method = 'OPTIONS') {
        return 204;
    }
}
```

> ⚠️ 面试常问：简单请求（GET/POST + 特定 Content-Type）不会发 OPTIONS；复杂请求（自定义头、PUT/DELETE）先发 OPTIONS 预检。

### 1.20 How to prevent hotlinking? 如何配置防盗链？
```nginx
location ~* \.(gif|jpg|jpeg|png|bmp|swf|flv|mp4|avi)$ {
    valid_referers none blocked server_names
                   ~\.google\.com
                   ~\.baidu\.com
                   *.example.com;

    if ($invalid_referer) {
        return 403;
        # 或返回一个警告图片
        # rewrite ^/ /images/forbidden.png;
    }
}
```

| 参数 | 含义 |
|------|------|
| `none` | 允许空 Referer（直接访问） |
| `blocked` | 允许没有协议头的 Referer |
| `server_names` | 允许本站域名 |
| `~regex` | 正则匹配允许的域名 |
| `$invalid_referer` | 变量，`1` 表示来源非法 |

---

## 二、深度原理剖析（12题）

### 2.1 Explain Nginx's event-driven architecture. 解释 Nginx 的事件驱动架构。
Nginx 使用异步、非阻塞事件驱动模型。与传统线程模型不同，Nginx 的工作进程在单线程内使用高效的 I/O 多路复用技术（如 epoll）处理成千上万个并发连接。

**工作流程**：
1. Worker 进程在事件循环中等待事件（新连接、数据可读、数据可写等）
2. 事件到达后，交给对应的处理模块执行回调
3. 处理完成后，继续等待新事件，不阻塞

```
传统模型：        请求 → 线程 → 阻塞 → 响应 → 释放线程
Nginx 模型： 请求 → Event Loop → 非阻塞处理 → 回调 → 响应
```

> 💡 正是因为这种架构，Nginx 才能用很少的 worker 进程处理海量并发。对比：Apache 每个连接需要独立进程/线程，Nginx 只需少量 worker。

### 2.2 How does Nginx implement hot reload (HUP signal)? Nginx 热部署原理（HUP 信号）？
当执行 `nginx -s reload` 或发送 `HUP` 信号时：
1. Master 进程检查配置文件语法是否正确
2. 语法正确 → master 启动新的 worker 进程（加载新配置）
3. 旧 worker 继续处理已有请求，不再接受新请求
4. 旧 worker 处理完所有请求后优雅退出
5. 新旧共存期间，新请求由新 worker 处理

```bash
# 热部署三部曲
nginx -t                    # 1. 测试配置
nginx -s reload             # 2. 热重载
# 或
kill -HUP $(cat /var/run/nginx.pid)   # 传统信号方式
```

### 2.3 Nginx 平滑升级原理（USR2 信号）
Nginx 的二进制执行文件替换需要平滑升级：
1. 备份旧二进制并替换为新版本
2. 发送 `USR2` 信号 → master 启动新 master + 新 worker
3. 发送 `WINCH` 信号 → 逐步关闭旧 worker
4. 观察新版本运行正常后，发送 `QUIT` 信号关闭旧 master

```bash
# 平滑升级步骤
kill -USR2 $(cat /var/run/nginx.pid)   # 启动新 master
kill -WINCH $(cat /var/run/nginx.pid)  # 关闭旧 worker
kill -QUIT $(cat /var/run/nginx.oldbin.pid)  # 关闭旧 master（确认新版本正常后）
```

### 2.4 How does proxy_pass resolve DNS? proxy_pass 的 DNS 解析机制？
```nginx
# 如果 proxy_pass 使用域名
location / {
    proxy_pass http://backend.example.com;
}
```

Nginx 默认只在**启动时或 reload 时**解析一次 upstream 域名。若后端 IP 变更，Nginx 不会自动感知。

**解决方案**：
```nginx
# 方案一：使用变量强制运行时解析（每次请求都会解析，性能稍差）
location / {
    set $backend "http://backend.example.com";
    proxy_pass $backend;
    resolver 8.8.8.8 valid=30s;    # DNS 解析器，30s 缓存
}

# 方案二：upstream 中配置（需要 ngx_http_upstream_dynamic 模块）
upstream backend {
    server backend.example.com resolve;
}
```

### 2.5 How does Nginx's load balancing ensure fairness? Nginx 负载均衡的权重调度原理？
**加权轮询算法平滑调度原理**：
- 每个 peer 有三个权重值：`weight`（初始权重）、`current_weight`（当前权重）、`effective_weight`（有效权重）
- 每次选择：所有 peer 的 `current_weight += effective_weight`
- 选择 `current_weight` 最大的 peer，令其 `current_weight -= total_weight`

> 💡 这种算法保证了在权重数组不变的情况下，总请求能均匀分配，不会出现"短时间连续请求打到同一台服务器"的问题。

### 2.6 What is the keepalive mechanism in Nginx? Nginx 的 keepalive 机制？
客户端 keepalive（client → nginx）：
```nginx
keepalive_timeout  65;           # 保持连接超时时间
keepalive_requests 100;          # 单连接最大请求数
```

上游 keepalive（nginx → backend）：
```nginx
upstream backend {
    server 192.168.1.10:8080;
    keepalive 32;                # 保留的空闲连接数
}
```

> 🎯 **面试追问**：客户端 keepalive 减少了三次握手开销；上游 keepalive 减少了 Nginx 到后端服务器的连接建立开销。两者配合能显著提升吞吐量。

### 2.7 How does Nginx handle SSL/TLS? Nginx SSL/TLS 握手流程？
1. Client Hello → 客户端发送支持的 SSL 版本、加密套件
2. Server Hello → Nginx 选择加密套件并发送证书
3. Certificate → 服务端证书链验证
4. Key Exchange → 密钥交换（RSA 或 ECDHE）
5. Finished → SSL 握手完成，建立加密通道

```nginx
server {
    listen 443 ssl;
    server_name www.example.com;
    ssl_certificate     /path/to/fullchain.pem;
    ssl_certificate_key /path/to/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;      # SSL 会话缓存
    ssl_session_timeout 10m;              # 会话过期时间
}
```

> 💡 `ssl_session_cache` 能显著减少重复 SSL 握手开销，在高并发场景下极其重要。

### 2.8 What is the difference between 4-layer and 7-layer load balancing? 四层和七层负载均衡的区别？
| 维度 | 四层负载均衡（L4） | 七层负载均衡（L7） |
|------|-------------------|-------------------|
| 协议 | TCP/UDP | HTTP/HTTPS |
| 决策依据 | IP + 端口 | URL、Header、Cookie |
| 性能 | 高（无需解析应用层） | 较低（需解析 HTTP 协议） |
| 灵活度 | 低 | 高（可按内容分发） |
| Nginx 配置 | `stream` 块 | `http` + `upstream` |

```nginx
# 四层负载均衡（stream 模块）
stream {
    upstream mysql_backend {
        server 192.168.1.10:3306;
        server 192.168.1.11:3306;
    }
    server {
        listen 3306;
        proxy_pass mysql_backend;
    }
}
```

### 2.9 How does Nginx's log system work? Nginx 日志系统工作原理？
Nginx 日志分为 `access_log` 和 `error_log`。

```nginx
# 日志格式定义
log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                '$status $body_bytes_sent "$http_referer" '
                '"$http_user_agent" "$http_x_forwarded_for"';

# 使用格式
access_log /var/log/nginx/access.log main buffer=32k flush=5s;
error_log  /var/log/nginx/error.log warn;
```

> 💡 `buffer=32k flush=5s` — 启用日志缓冲区，每 5 秒或 32KB 写入一次，减少磁盘 I/O。

日志切割常用方案：
```bash
# logrotate 自动分割
/var/log/nginx/*.log {
    daily
    rotate 30
    compress
    postrotate
        [ -f /var/run/nginx.pid ] && kill -USR1 `cat /var/run/nginx.pid`
    endscript
}
```

### 2.10 How does Nginx implement reverse proxy tunneling mode? Nginx 反向代理的隧道模式？
Nginx 作为反向代理时，客户端和后端服务器之间建立一条"隧道"：
1. Nginx 接收客户端的 HTTP 请求
2. 解析请求头，根据配置决定路由
3. 与上游服务器建立连接（维护连接池）
4. 转发请求并接收响应
5. 将响应返回给客户端

对于 WebSocket，Nginx 使用 HTTP Upgrade 机制将连接切换到隧道模式：
```nginx
location /ws/ {
    proxy_pass http://ws_backend;
    proxy_http_version 1.1;               # WebSocket 需要 HTTP/1.1
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_read_timeout 86400s;            # 长连接超时
}
```

### 2.11 How is the ngx_http_rewrite_module implemented? rewrite 模块的执行顺序？
Rewrite 模块的执行分为两个阶段：

**Phase 1: 请求处理阶段（以 URL 重写为主）**
- `server` 块中的 `rewrite` 指令在 location 匹配前执行
- `location` 块中的 `rewrite` 指令匹配后执行

**Phase 2: 返回响应阶段（以重定向为主）**
- `return` 指令可立即终止处理

```nginx
# 执行顺序示例
server {
    rewrite ^/old/(.*) /new/$1 redirect;  # 1. 先执行 server 级 rewrite

    location / {
        rewrite ^/foo/(.*) /bar/$1;        # 2. location 内 rewrite
        return 200 "final";                # 3. return 立即终止
    }
}
```

> ⚠️ 注意：rewrite 指令默认在内部重定向中最多循环 10 次，通过 `rewrite_log on;` 可查看循环次数。

### 2.12 How does the ngx_http_static_module handle static files? 静态资源处理模块的工作机制？
```
请求 → location 匹配 → root/alias 拼接路径 → 文件存在性检查 → ETag/Last-Modified 生成 → Range 处理 → sendfile 发送
```

关键流程：
1. 根据 `root` 或 `alias` 指令拼接文件路径
2. 检查文件是否存在（`try_files` 可用于降级）
3. 自动生成 `ETag`（文件 inode + 大小 + mtime 的 MD5）
4. 生成 `Last-Modified`（文件的修改时间）
5. 如果客户端带 `If-Modified-Since` / `If-None-Match`，返回 304
6. 使用 `sendfile` 零拷贝发送文件内容

---

## 三、实战场景题（10题）

### 3.1 给一个 Vue/React SPA 项目配置 Nginx
```nginx
server {
    listen 80;
    server_name www.example.com;
    root /var/www/dist;

    # SPA 路由兜底
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 静态资源缓存（打包带 hash 的文件）
    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
        access_log off;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://backend:8080/;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

> 💡 `try_files $uri $uri/ /index.html` 是 SPA 部署的标配，保证前端路由刷新不 404。

### 3.2 配置一个域名跳转（www 跳转裸域 或 反之）
```nginx
# 统一 www 访问
server {
    listen 80;
    server_name example.com;
    return 301 http://www.example.com$request_uri;
}

server {
    listen 80;
    server_name www.example.com;
    # 正常服务配置...
}
```

### 3.3 配置 HTTPS 完整示例
```nginx
# HTTP → HTTPS 强制跳转
server {
    listen 80;
    server_name www.example.com;
    return 301 https://$server_name$request_uri;
}

# HTTPS 服务
server {
    listen 443 ssl http2;
    server_name www.example.com;

    ssl_certificate     /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # HSTS（告诉浏览器强制 HTTPS）
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    location / {
        root /var/www/html;
        index index.html;
    }
}
```

### 3.4 配置基于 URL 路径的灰度发布
```nginx
upstream stable {
    server 192.168.1.10:8080;
}

upstream canary {
    server 192.168.1.20:8080;
}

# 根据 Cookie 灰度
server {
    location / {
        if ($cookie_canary = "enabled") {
            proxy_pass http://canary;
            break;
        }
        proxy_pass http://stable;
    }
}

# 根据 IP 灰度
geo $is_canary {
    default 0;
    113.87.0.0/16 1;     # 测试 IP 段
}

upstream backend {
    server 192.168.1.10:8080 weight=9;
    server 192.168.1.20:8080 weight=1;
}
```

### 3.5 正向代理配置（内网机器访问外网）
```nginx
server {
    listen 8888;
    resolver 8.8.8.8;            # DNS 解析器
    location / {
        proxy_pass $scheme://$host$request_uri;
        proxy_set_header Host $http_host;
    }
}
```

> ⚠️ 正向代理需要 `resolver` 指令，否则 Nginx 不会解析 upstream 中的域名。

### 3.6 配置动静分离
```nginx
# 动态请求 → Tomcat
upstream tomcat_servers {
    server 192.168.1.10:8080;
    server 192.168.1.11:8080;
}

server {
    listen 80;
    server_name www.example.com;

    # 静态资源 → Nginx 本地
    location ~* \.(html|css|js|png|jpg|jpeg|gif|ico|svg|woff2?|ttf|eot)$ {
        root /var/www/static;
        expires 30d;
        access_log off;
    }

    # 动态请求 → Tomcat 集群
    location / {
        proxy_pass http://tomcat_servers;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 3.7 配置多域名虚拟主机
```nginx
# 虚拟主机 A
server {
    listen 80;
    server_name blog.example.com;
    root /var/www/blog;
    index index.html;
}

# 虚拟主机 B
server {
    listen 80;
    server_name shop.example.com;
    root /var/www/shop;
    index index.html;
}

# 虚拟主机 C（泛域名）
server {
    listen 80;
    server_name *.example.com;
    root /var/www/$subdomain;
    index index.html;
}
```

### 3.8 配置 Nginx 作为文件下载站点
```nginx
server {
    listen 80;
    server_name download.example.com;
    root /var/www/downloads;

    # 目录列表
    location / {
        autoindex on;
        autoindex_exact_size off;    # 以友好格式显示大小
        autoindex_localtime on;      # 显示本地时间
        charset utf-8;
    }

    # 限速下载
    location /bigfile.zip {
        limit_rate 200k;             # 下载限速 200KB/s
    }
}
```

### 3.9 配置 Nginx 用户认证
```nginx
location /admin/ {
    auth_basic "Administrator Login";
    auth_basic_user_file /etc/nginx/.htpasswd;
}

# 生成密码文件
# htpasswd -c /etc/nginx/.htpasswd admin
# 或使用 openssl:
# echo "admin:$(openssl passwd -apr1 123456)" > /etc/nginx/.htpasswd
```

### 3.10 配置 SSL 证书（使用自签名证书）
```bash
# 使用 openssl 生成自签名证书
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout /etc/nginx/ssl/self.key \
    -out /etc/nginx/ssl/self.crt \
    -subj "/C=CN/ST=Beijing/L=Beijing/O=Dev/CN=localhost"
```

```nginx
server {
    listen 443 ssl;
    server_name localhost;
    ssl_certificate     /etc/nginx/ssl/self.crt;
    ssl_certificate_key /etc/nginx/ssl/self.key;
}
```

---

## 四、手写代码/配置文件题（6题）

### 4.1 手写一个完整的 nginx.conf
要求：包含全局配置、events、http、gzip、反向代理、负载均衡、HTTPS、日志。

> 💡 面试官想看你对配置结构的整体把握，注意注释规范。

```nginx
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 1024;
    use epoll;
    multi_accept on;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';
    access_log /var/log/nginx/access.log main buffer=32k flush=5s;

    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;

    # Gzip 配置
    gzip on;
    gzip_min_length 1k;
    gzip_comp_level 6;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript image/svg+xml;
    gzip_vary on;
    gzip_proxied any;

    # 静态资源缓存
    include /etc/nginx/conf.d/*.conf;
}
```

### 4.2 手写一个配置：www 域名统一 + 强制 HTTPS + SPA 路由
```nginx
# 裸域 → www
server {
    listen 80;
    server_name example.com;
    return 301 https://www.example.com$request_uri;
}

# HTTP → HTTPS
server {
    listen 80;
    server_name www.example.com;
    return 301 https://$server_name$request_uri;
}

# HTTPS 主服务
server {
    listen 443 ssl http2;
    server_name www.example.com;

    ssl_certificate /etc/nginx/ssl/example.com.pem;
    ssl_certificate_key /etc/nginx/ssl/example.com.key;

    root /var/www/dist;
    index index.html;

    # SPA 路由兜底
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理
    location /api/ {
        proxy_pass http://backend:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### 4.3 手写 rewrite 规则：http 跳转 https + 域名迁移 + 伪静态
```nginx
# 域名迁移 301 永久跳转
server {
    listen 80;
    server_name old-site.com www.old-site.com;
    return 301 $scheme://new-site.com$request_uri;
}

# PHP 伪静态：将 /article/123 重写为 /article.php?id=123
location /article/ {
    rewrite ^/article/(\d+)$ /article.php?id=$1 last;
}

# 目录合并：将 /news/2024/01/01/title.html 重写为 /news/title.html
location /news/ {
    rewrite ^/news/(\d{4})/(\d{2})/(\d{2})/(.+)\.html$ /news/$4.html last;
}

# 浏览器类型重定向
location /mobile/ {
    if ($http_user_agent ~* "(Android|iPhone|Mobile)") {
        rewrite ^/mobile/(.*)$ /mobile-site/$1 redirect;
    }
}
```

### 4.4 手写负载均衡 + 健康检查配置
```nginx
upstream backend {
    # 加权轮询
    server 192.168.1.10:8080 weight=5 max_fails=3 fail_timeout=30s;
    server 192.168.1.11:8080 weight=3 max_fails=3 fail_timeout=30s;
    server 192.168.1.12:8080 weight=2;
    server 192.168.1.13:8080 backup;

    # 保持连接池
    keepalive 32;
}

server {
    listen 80;
    server_name api.example.com;

    location / {
        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_next_upstream error timeout invalid_header http_500 http_502 http_503;
        proxy_connect_timeout 5s;
        proxy_read_timeout 10s;
    }
}
```

### 4.5 手写防盗链 + 跨域配置（企业级）
```nginx
location ~* \.(gif|jpg|jpeg|png|bmp|swf|flv|mp4|webm|ogg|mp3|wav|ico)$ {
    # 防盗链
    valid_referers none blocked server_names
                   ~\.example\.com
                   ~\.google\.com
                   ~\.baidu\.com
                   *.trusted-site.com;

    if ($invalid_referer) {
        return 403;
    }

    # 跨域（图片资源允许跨域引用）
    add_header Access-Control-Allow-Origin "*";  # 注意：生产环境慎用 *
    add_header Cache-Control "public, immutable";
    expires 7d;
}
```

### 4.6 手写基于 IP + 请求频率的访问控制
```nginx
# 限制特定路径的访问来源
location /admin/ {
    # 白名单 IP
    allow 192.168.1.0/24;
    allow 10.0.0.0/8;
    deny all;

    auth_basic "Admin Access";
    auth_basic_user_file /etc/nginx/.htpasswd;

    proxy_pass http://admin_backend;
}
```

---

## 五、系统设计题（4题）

### 5.1 设计一个日活千万的图片服务
**需求**：图片上传和访问，要求高并发低延迟。

```
Client → CDN → Nginx (LVS + Keepalived HA) → 本地缓存 → 应用服务器 → 对象存储
```

**方案要点**：
1. **CDN 加速**：静态图片通过 CDN 分发，回源到 Nginx
2. **Nginx 缓存**：`proxy_cache` 缓存热图
3. **缩略图实时生成**：`ngx_http_image_filter_module`
4. **防盗链**：`valid_referers` + 时间戳签名

```nginx
location ~* ^/images/(\d+)x(\d+)/(.*)\.(jpg|png)$ {
    image_filter resize $1 $2;
    image_filter_buffer 2M;
    proxy_pass http://image_backend;
}
```

### 5.2 设计一个跨云多活部署方案
**需求**：服务部署在阿里云和 AWS 多 Region，要求故障自动切换。

```nginx
upstream global_backend {
    # 阿里云（主）
    server aliyun-1.example.com:8080 weight=10;
    server aliyun-2.example.com:8080 weight=10;

    # AWS（备）
    server aws-1.example.com:8080 weight=5;
    server aws-2.example.com:8080 weight=5;
}

# 健康检查自动剔除故障节点
location / {
    proxy_pass http://global_backend;
    proxy_next_upstream error timeout invalid_header http_500 http_502 http_503;
    proxy_connect_timeout 3s;
    proxy_read_timeout 5s;
}
```

### 5.3 设计一个 API 网关
**需求**：统一管理微服务 API，包含限流、鉴权、路由、日志。

```nginx
# 路由映射
upstream user_svc    { server 10.0.1.10:8080; }
upstream order_svc   { server 10.0.1.11:8080; }
upstream payment_svc { server 10.0.1.12:8080; }

server {
    listen 80;
    server_name gateway.example.com;

    # 日志（记录完整请求）
    log_format gateway '$remote_addr - [$time_local] "$request" $status '
                       '$body_bytes_sent "$upstream_addr" "$upstream_status" '
                       '"$http_x_forwarded_for" "$request_time"';
    access_log /var/log/nginx/gateway.log gateway buffer=32k;

    # 用户服务
    location /api/user/ {
        proxy_pass http://user_svc/;
    }

    # 订单服务
    location /api/order/ {
        proxy_pass http://order_svc/;
    }

    # 支付服务
    location /api/payment/ {
        proxy_pass http://payment_svc/;
    }
}
```

### 5.4 设计一个双 11 秒杀系统的接入层
**需求**：瞬间高并发，保护后端，防刷单。

```nginx
# 限流
limit_req_zone $binary_remote_addr zone=seckill:10m rate=1r/s;

# 黑名单
geo $blacklist {
    default 0;
    113.87.0.0/16 1;    # 恶意 IP 段
}

server {
    # 黑白名单过滤
    if ($blacklist) {
        return 403;
    }

    location /seckill/ {
        # 限流 1r/s per IP + burst 5
        limit_req zone=seckill burst=5 nodelay;
        limit_req_status 429;

        # 只允许 POST
        if ($request_method != "POST") {
            return 405;
        }

        proxy_pass http://seckill_backend;
    }
}
```

---

## 六、常见坑点与最佳实践

### 6.1 `if` is evil — Nginx 中 if 指令的陷阱
Nginx 文档明确建议**避免在 location 块中使用 `if`**，因为 if 指令在 Nginx 中存在很多奇怪的 bug 和不可预期的行为。

```nginx
# ❌ 错误的 if 用法
location / {
    if ($request_method = "DELETE") {
        return 405;
    }
    proxy_pass http://backend;
}

# ✅ 正确做法：使用 more_set_headers 或限制方法
limit_except GET POST {
    deny all;
}
```

### 6.2 proxy_pass 尾部斜杠的陷阱
```nginx
# 区别至关重要
# 1. 带斜杠：截断匹配路径
location /api/ {
    proxy_pass http://backend/;
    # 请求 /api/users → http://backend/users
}

# 2. 不带斜杠：保留完整路径
location /api/ {
    proxy_pass http://backend;
    # 请求 /api/users → http://backend/api/users
}
```

### 6.3 server_name 与 listen 指令混淆
```nginx
# ❌ 错误：认为指定了域名就能只响应这个域名
listen 80;                # 监听所有 IP 的 80 端口
server_name example.com;

# ✅ server_name 只是判断虚拟主机的依据，listen 控制监听地址
listen 192.168.1.1:80;    # 绑定到特定 IP
server_name example.com;
```

### 6.4 常见性能陷阱
| 陷阱 | 后果 | 解决方案 |
|------|------|---------|
| 未开 `sendfile` | 静态文件性能下降 30%+ | `sendfile on;` |
| `worker_processes` 太少 | CPU 资源未充分利用 | `worker_processes auto;` |
| 未配置 `worker_connections` | 并发连接数受限 | 结合 `ulimit -n` 设置 |
| 日志级别过高 | 磁盘 I/O 瓶颈 | 生产环境用 `warn` |
| `proxy_buffering` 关闭 | 后端响应慢传递慢 | `proxy_buffering on;` |

---

## 七、面试回答模板（Top 5）

### 7.1 "请介绍一下 Nginx 的架构"
> Nginx 采用的是 master-worker 多进程架构。Master 进程负责读取配置文件、管理 worker 进程和处理信号；Worker 进程使用异步非阻塞事件驱动模型，通过 epoll 等 I/O 多路复用技术处理并发请求。这种架构的优势在于：第一，进程间隔离，一个 worker 挂了不影响其他 worker；第二，充分利用多核 CPU；第三，热部署能力，可以通过信号实现热加载配置和平滑升级。

### 7.2 "Nginx 反向代理和负载均衡如何配置"
> 反向代理核心是用 `proxy_pass` 将请求转发到后端，同时通过 `proxy_set_header` 设置 Host、X-Real-IP 等头部。负载均衡通过 `upstream` 块定义服务器组，支持轮询、加权轮询、ip_hash、least_conn 等策略。我通常会配合 `proxy_next_upstream` 配置故障转移，以及 `max_fails` 和 `fail_timeout` 进行健康检查。

### 7.3 "location 匹配规则是什么"
> Location 有四种匹配方式：精确匹配 `=`、前缀匹配 `^~`、正则匹配 `~` 和 `~*`、普通前缀匹配。优先级从高到低的顺序是：精确匹配 > 前缀匹配(^~) > 按顺序的正则匹配 > 最长前缀匹配。我实际工作中经常用 `=` 匹配精确路径（如 /favicon.ico）获得最高性能，用 `~*` 匹配静态文件扩展名。

### 7.4 "root 和 alias 的区别"
> Root 是 Nginx 的默认文档根目录，会将 `root + URI` 拼接为实际文件路径；Alias 则会用 alias 路径替换掉 location 匹配的部分。比如 `location /images/` 中，root `/var/www` 实际指向 `/var/www/images/`，而 alias `/var/www/images/` 实际指向 `/var/www/images/`。Alias 末尾必须有 `/`，且不能在 `location /` 中使用。

### 7.5 "nginx.conf 的配置文件结构"
> Nginx 的配置分为几个层级：最外层是全局块（user、worker_processes 等），然后是 events 块（网络连接配置），接着是 http 块，http 块内包含多个 server 块（虚拟主机），每个 server 块内包含多个 location 块（URL 匹配规则）。配置继承是外层到内层，内层可覆盖外层配置。

---

## 八、快速查漏补缺 Checklist

| 知识点 | 掌握程度 | 备注 |
|--------|---------|------|
| [ ] Nginx 与 Apache 对比 | / | 事件驱动 vs 进程驱动 |
| [ ] master-worker 模型 | / | 信号管理（HUP/QUIT/USR1/USR2） |
| [ ] 配置文件结构 | / | global → events → http → server → location |
| [ ] listen / server_name | / | 三种匹配方式及优先级 |
| [ ] location 匹配 | / | `=` / `^~` / `~` / `~*` / 前缀 |
| [ ] root vs alias | / | **高频易错** |
| [ ] sendfile/零拷贝 | / | `sendfile` + `tcp_nopush` |
| [ ] proxy_pass 斜杠陷阱 | / | 带 `/` 截断 vs 不带 `/` 保留 |
| [ ] proxy_set_header | / | Host/X-Real-IP/X-Forwarded-For |
| [ ] upstream 状态 | / | down/backup/max_fails/fail_timeout |
| [ ] 负载均衡策略 | / | 轮询/加权/ip_hash/least_conn/url_hash/fair |
| [ ] Gzip 配置 | / | gzip on + gzip_types + gzip_static |
| [ ] 浏览器缓存 | / | 强缓存(max-age) vs 协商缓存(ETag) |
| [ ] 防盗链 | / | valid_referers + $invalid_referer |
| [ ] CORS 跨域 | / | Access-Control-Allow-* |
| [ ] SSL/HTTPS | / | 证书配置 + HTTP 跳转 |
| [ ] Rewrite 指令 | / | set/if/break/return/rewrite |
| [ ] 动静分离 | / | 静态 local / 动态 proxy_pass |

---

> 🎯 **总结**：Nginx 基础面试的核心是**架构理解 + 配置实操**。掌握 master-worker 模型、配置文件层级结构、location 匹配规则、reverse proxy 和 load balancing 配置，基本能应对 80% 的中级面试题。建议在实际服务器上反复练习配置，理解每个指令背后的原理。
