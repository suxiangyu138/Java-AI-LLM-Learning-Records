# 11 - Nginx 性能调优与故障排查

> 🎯 Nginx 性能调优不只是改几个数字 — 从 Worker 进程到内核参数、从缓冲区到连接池，系统性地将 Nginx 调教到最佳状态，并掌握常见故障的快速定位能力

---

## 目录

1. [Worker 进程优化](#1-worker-进程优化)
2. [连接与缓冲区调优](#2-连接与缓冲区调优)
3. [静态资源与传输优化](#3-静态资源与传输优化)
4. [代理与 upstream 调优](#4-代理与-upstream-调优)
5. [内核参数调优](#5-内核参数调优)
6. [故障排查速查表](#6-故障排查速查表)
7. [常见故障场景与解决方案](#7-常见故障场景与解决方案)

---

## 1. Worker 进程优化

### 1.1 核心配置

```nginx
# ═══ 推荐配置 ═══
worker_processes auto;                # 自动匹配 CPU 核心数
worker_cpu_affinity auto;             # CPU 亲和性绑定（减少上下文切换）
worker_rlimit_nofile 65535;           # Worker 最大文件句柄数

events {
    worker_connections 10240;          # 单 Worker 最大连接数
    use epoll;                         # Linux 高性能 IO 模型
    multi_accept on;                   # 同时接受多个新连接
}
```

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| `worker_processes` | `auto` (CPU 核数) | CPU 密集型可设 `auto`，IO 密集型可设 `auto × 1.5-2` |
| `worker_connections` | 1024-65535 | 总并发 = `worker_processes × worker_connections` |
| `multi_accept` | `on` | 高并发场景开启，低并发场景关闭 |
| `accept_mutex` | `off` | 高并发时关闭（epoll 下 `multi_accept` 更优） |

### 1.2 最大并发数计算

```
总并发数 = worker_processes × worker_connections / 2 (反向代理时每个请求占 2 连接)

示例：4 核 × 10240 / 2 = 20480 并发连接
```

---

## 2. 连接与缓冲区调优

### 2.1 超时配置

```nginx
http {
    # ═══ 客户端超时 ═══
    keepalive_timeout 30s;              # 长连接保持时间
    keepalive_requests 1000;            # 单长连接最大请求数
    client_header_timeout 15s;          # 客户端头发送超时
    client_body_timeout 30s;            # 客户端 body 发送超时
    send_timeout 15s;                   # 向客户端发送响应超时

    # ═══ 代理超时 ═══
    proxy_connect_timeout 10s;          # 连接后端超时
    proxy_send_timeout 60s;             # 发送到后端超时
    proxy_read_timeout 60s;             # 读取后端响应超时
}
```

### 2.2 缓冲区大小

```nginx
http {
    # ═══ 客户端缓冲区 ═══
    client_body_buffer_size 128k;       # 请求体缓冲区
    client_header_buffer_size 4k;       # 请求头缓冲区
    large_client_header_buffers 4 32k;  # 大请求头（如长 Cookie）
    client_max_body_size 20m;           # 最大请求体（上传文件）

    # ═══ 代理缓冲区 ═══
    proxy_buffer_size 16k;              # 响应头缓冲区
    proxy_buffers 8 64k;                # 响应体缓冲区（8个×64KB）
    proxy_busy_buffers_size 128k;       # 忙碌缓冲区总大小
    proxy_temp_file_write_size 128k;    # 临时文件写块大小
}
```

### 2.3 上游连接池

```nginx
upstream java_backend {
    server 192.168.1.100:8080;
    server 192.168.1.101:8080;
    keepalive 32;                       # 到后端的空闲长连接数
}

server {
    location / {
        proxy_http_version 1.1;         # 必须设为 HTTP/1.1
        proxy_set_header Connection ""; # 清除 Connection 头（启用长连接）
        proxy_pass http://java_backend;
    }
}
```

> ⚠️ **关键**：启用 upstream keepalive 必须同时设置 `proxy_http_version 1.1` + 清除 `Connection` 头，否则长连接不生效。

---

## 3. 静态资源与传输优化

### 3.1 Gzip 压缩

```nginx
http {
    gzip on;
    gzip_min_length 1k;                 # 小于 1KB 不压缩
    gzip_comp_level 5;                  # 压缩级别 1-9（推荐 4-6）
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml text/javascript;
    gzip_vary on;                       # 添加 Vary: Accept-Encoding
    gzip_disable "MSIE [1-6]\.";        # 禁用 IE6
    gzip_proxied any;                   # 对代理请求也压缩
    gzip_buffers 16 8k;                 # 压缩缓冲区
    gzip_http_version 1.1;
}
```

### 3.2 静态资源缓存与高效传输

```nginx
location ~* \.(css|js|jpg|jpeg|png|gif|webp|svg|ico|woff2?)$ {
    expires 30d;                        # 浏览器缓存 30 天
    add_header Cache-Control "public, immutable";
    sendfile on;                        # 零拷贝传输
    tcp_nopush on;                      # sendfile 模式下优化
}
```

| 参数 | 说明 |
|------|------|
| `sendfile on` | 零拷贝（kernel 直接发送文件，绕过用户态） |
| `tcp_nopush on` | sendfile 时打包发送（减少 TCP 分段） |
| `tcp_nodelay on` | 长连接时立即发送小包（权衡延迟） |

### 3.3 HTTP/2 配置

```nginx
server {
    listen 443 ssl http2;
    server_name api.example.com;

    # HTTP/2 推送（预加载关键资源）
    location / {
        http2_push /css/main.css;
        http2_push /js/app.js;
        proxy_pass http://java_backend;
    }
}
```

---

## 4. 代理与 upstream 调优

### 4.1 代理头传递

```nginx
location /api/ {
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_pass http://java_backend;
}
```

### 4.2 健康检查调优

```nginx
upstream java_backend {
    server 192.168.1.100:8080 max_fails=3 fail_timeout=30s;
    server 192.168.1.101:8080 max_fails=3 fail_timeout=30s;

    # Nginx Plus 主动健康检查（开源版不支持）
    # health_check interval=5s fails=3 passes=2;
}
```

| 参数 | 说明 |
|------|------|
| `max_fails=3` | 30 秒内失败 3 次标记为 down |
| `fail_timeout=30s` | 标记 down 后 30 秒再尝试 |
| `backup` | 备用节点（主节点全部 down 时启用） |
| `down` | 手动下线节点 |

### 4.3 错误重试

```nginx
location /api/ {
    proxy_next_upstream error timeout http_502 http_503 http_504;
    proxy_next_upstream_tries 2;           # 最多重试 2 个节点
    proxy_next_upstream_timeout 10s;       # 重试超时
    proxy_pass http://java_backend;
}
```

---

## 5. 内核参数调优

```bash
# /etc/sysctl.d/99-nginx.conf
# ═══ 网络队列 ═══
net.core.somaxconn = 65535                # 监听队列最大长度
net.core.netdev_max_backlog = 65535       # 网卡接收队列

# ═══ TCP 连接 ═══
net.ipv4.tcp_max_syn_backlog = 65535      # SYN 队列长度
net.ipv4.tcp_synack_retries = 2           # SYN-ACK 重试次数
net.ipv4.ip_local_port_range = 1024 65535 # 本地端口范围

# ═══ TIME_WAIT 优化 ═══
net.ipv4.tcp_tw_reuse = 1                 # 复用 TIME_WAIT 连接
net.ipv4.tcp_fin_timeout = 15             # FIN_WAIT-2 超时（默认 60s）

# ═══ Keepalive ═══
net.ipv4.tcp_keepalive_time = 300         # 空闲 5 分钟发 Keepalive
net.ipv4.tcp_keepalive_intvl = 30         # Keepalive 探测间隔
net.ipv4.tcp_keepalive_probes = 3         # Keepalive 探测次数

# ═══ 文件句柄 ═══
fs.file-max = 1000000                     # 系统最大文件句柄数

# 生效
# sysctl -p /etc/sysctl.d/99-nginx.conf
```

---

## 6. 故障排查速查表

### 6.1 按错误码排查

| 错误 | 含义 | 常见原因 | 排查方向 |
|------|------|----------|----------|
| **502 Bad Gateway** | 后端无响应 | Java 服务挂了/超时 | `ss -tlnp` 查端口、`journalctl` 查服务 |
| **503 Service Unavailable** | 后端全部不可用 | 所有 upstream 节点 down | 检查健康检查配置、后端负载 |
| **504 Gateway Timeout** | 后端响应超时 | 处理时间 > `proxy_read_timeout` | 调大超时或优化后端逻辑 |
| **499 Client Closed** | 客户端主动断开 | 用户刷新/关闭、客户端超时 | 检查客户端超时、CDN 健康检查频率 |
| **413 Request Entity Too Large** | 请求体过大 | 上传文件超过限制 | 调大 `client_max_body_size` |
| **400 Bad Request** | 请求格式错误 | 请求头过大 | 调大 `large_client_header_buffers` |

### 6.2 性能诊断命令

```bash
# ═══ 连接状态统计 ═══
ss -ant | awk '{print $1}' | sort | uniq -c | sort -rn

# ═══ 查看 Nginx 错误日志 ═══
tail -f /var/log/nginx/error.log

# ═══ 查看 upstream 响应时间 ═══
tail -f /var/log/nginx/access.log | awk '{print $NF}'

# ═══ Stub Status（需先配置） ═══
curl http://localhost/nginx_status
# Active connections / accepts / handled / requests
# Reading / Writing / Waiting

# ═══ 文件句柄统计 ═══
lsof -p $(pgrep nginx | head -1) | wc -l
```

### 6.3 Stub Status 配置

```nginx
server {
    listen 127.0.0.1:8080;
    location /nginx_status {
        stub_status on;
        access_log off;
        allow 127.0.0.1;
        deny all;
    }
}
```

---

## 7. 常见故障场景与解决方案

### 7.1 Nginx 启动失败

```bash
# 1. 测试配置语法
nginx -t

# 2. 常见错误
# "nginx: [emerg] bind() to 0.0.0.0:80 failed (98: Address already in use)"
# → 端口被占用：ss -tlnp | grep :80

# "nginx: [emerg] unknown directive"
# → 语法错误或模块未编译

# "nginx: [emerg] SSL_CTX_use_PrivateKey_file"
# → SSL 证书与私钥不匹配
```

### 7.2 502 Bad Gateway 排查流程

```bash
# 1. 确认后端服务是否存活
ss -tlnp | grep 8080
curl -I http://127.0.0.1:8080/actuator/health

# 2. 确认 upstream 配置是否正确
nginx -T | grep -A5 upstream

# 3. 查看 Nginx 错误日志
tail -50 /var/log/nginx/error.log

# 4. 确认 proxy_pass 地址可达
nc -zv 192.168.1.100 8080

# 5. 检查 SELinux（CentOS）
getenforce
ausearch -m avc -ts recent
# 如果 SELinux 阻止 → setsebool -P httpd_can_network_connect on
```

### 7.3 高并发时 Nginx 响应慢

```bash
# 1. 检查 Worker 进程数
ps aux | grep nginx | grep -v grep | wc -l

# 2. 检查连接数是否打满
curl http://localhost/nginx_status

# 3. 检查文件句柄限制
cat /proc/$(pgrep nginx | head -1)/limits | grep "Max open files"

# 4. 检查系统连接状态
ss -s

# 5. 调整配置
# - 增加 worker_connections
# - 增大 keepalive 连接数
# - 优化缓冲区大小
# - 检查后端 upstream 连接池
```

### 7.4 SSL 握手慢

```nginx
# ═══ 优化项 ═══
ssl_session_cache shared:SSL:50m;       # 增大 session 缓存
ssl_session_timeout 1d;                 # 延长 session 超时
ssl_session_tickets off;                # 关闭 session tickets（安全性更好）
ssl_buffer_size 8k;                     # 缩小 SSL 缓冲区（TLSv1.3）
```

> 🎯 **排查原则**：先确认配置语法 → 再查错误日志 → 确认后端可达 → 检查系统资源 → 逐层定位根因。
