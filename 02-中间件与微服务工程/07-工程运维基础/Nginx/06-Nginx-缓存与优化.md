# 06 - Nginx 缓存与优化

> 🎯 缓存是性能优化的银弹 — proxy_cache 减少后端 90% 请求、gzip 压缩传输体积 70%、sendfile 零拷贝加速静态文件

---

## 目录

1. [缓存概述](#一缓存概述)
2. [全局缓存存储配置](#二全局缓存存储配置)
3. [浏览器缓存控制](#三浏览器缓存控制)
4. [Gzip 压缩优化](#四gzip-压缩优化)
5. [高效传输与连接优化](#五高效传输与连接优化)
6. [Java 后端缓存策略建议](#六java-后端缓存策略建议)

---

## 一、缓存概述

Nginx 缓存将频繁访问的资源存储在本地磁盘/内存，后续直接返回缓存数据，减少后端请求。

| 缓存类型 | 适用对象 | 不适用对象 |
|---------|---------|-----------|
| 静态资源缓存 | JS/CSS/图片/视频/PDF | — |
| 接口响应缓存 | 查询接口、低频更新接口 | 登录/下单/支付等动态接口 |
| 第三方接口缓存 | 天气/地图等外部 API | — |

> **核心原则**：缓存读多写少的数据，动态交互接口禁止缓存。

## 二、全局缓存存储配置

```nginx
http {
    # 全局缓存路径配置（仅需配置一次）
    proxy_cache_path /var/nginx/proxy_cache
                     levels=1:2                   # 二级目录，避免单目录文件过多
                     keys_zone=proxy_cache:10m    # 缓存区域名 + 内存大小
                     max_size=10g                 # 最大磁盘空间
                     inactive=60m                 # 60 分钟未访问自动清理
                     use_temp_path=off;           # 禁用临时目录
}
```

创建目录并授权：

```bash
mkdir -p /var/nginx/proxy_cache
chown -R nginx:nginx /var/nginx/proxy_cache/
```

## 三、静态资源缓存

```nginx
location ~* \.(jpg|jpeg|png|gif|css|js|pdf|mp4|ico)$ {
    root   /usr/local/nginx/html/dist;

    # 浏览器缓存
    expires 7d;
    add_header Cache-Control "public, max-age=604800";

    # Nginx 本地缓存
    proxy_cache proxy_cache;
    proxy_cache_key "$host$request_uri";
    proxy_cache_valid 200 304 7d;      # 正常响应缓存 7 天
    proxy_cache_valid any 1m;          # 异常响应缓存 1 分钟
    proxy_cache_min_uses 2;            # 访问 2 次后才缓存

    # 缓存状态头（便于验证：HIT/MISS/EXPIRED）
    add_header X-Proxy-Cache $upstream_cache_status;
}
```

## 四、Java 接口缓存

```nginx
upstream java_backend {
    server 192.168.1.100:8080;
    server 192.168.1.101:8080;
}

server {
    listen       80;
    server_name  api.example.com;

    # 查询接口缓存（仅缓存 GET 请求的查询接口）
    location /api/query/ {
        proxy_pass http://java_backend/api/query/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;

        proxy_cache proxy_cache;
        proxy_cache_key "$host$request_uri$args";
        proxy_cache_valid 200 10m;         # 正常响应缓存 10 分钟
        proxy_cache_valid 404 1m;          # 404 缓存 1 分钟
        proxy_cache_min_uses 3;            # 访问 3 次后缓存
        proxy_cache_bypass $arg_refresh;   # ?refresh=1 强制刷新
        proxy_no_cache $cookie_admin;      # 管理员登录后不使用缓存

        add_header X-Proxy-Cache $upstream_cache_status;
    }

    # 写操作接口：禁止缓存
    location /api/add/ {
        proxy_pass http://java_backend/api/add/;
        proxy_cache off;
    }
}
```

## 五、缓存相关指令速查

| 指令 | 说明 |
|------|------|
| `proxy_cache_path` | 定义缓存存储路径、大小、过期策略 |
| `proxy_cache` | 启用缓存，关联缓存区域 |
| `proxy_cache_key` | 缓存唯一标识（域名 + 路径 + 参数） |
| `proxy_cache_valid` | 按状态码设置缓存时间 |
| `proxy_cache_min_uses` | 访问 N 次后开始缓存 |
| `proxy_cache_bypass` | 满足条件时跳过缓存 |
| `proxy_no_cache` | 满足条件时不缓存响应 |
| `proxy_cache_lock` | 防缓存击穿，同一请求只转发一次 |
| `proxy_cache_use_stale` | 后端故障时使用过期缓存应急 |
| `proxy_cache_background_update` | 后台更新缓存，客户端继续使用旧缓存 |

## 六、缓存优化策略

### 6.1 防缓存雪崩（分散过期时间）

```nginx
proxy_cache_valid 200 10m ~2m;    # 8~12 分钟随机过期
```

### 6.2 防缓存穿透（缓存无效请求）

```nginx
proxy_cache_valid 404 5m;         # 缓存 404，减少无效请求
proxy_cache_valid 403 5m;

# 配合限流
limit_req_zone $binary_remote_addr zone=query_req:10m rate=10r/s;
```

### 6.3 防缓存击穿（缓存锁）

```nginx
proxy_cache_lock on;               # 同一资源只转发一次到后端
proxy_cache_lock_timeout 5s;       # 锁超时时间
```

### 6.4 后端故障应急

```nginx
proxy_cache_use_stale error timeout invalid_header updating;
proxy_cache_background_update on;
```

## 七、缓存管理

### 7.1 全量清理

```bash
rm -rf /var/nginx/proxy_cache/*
```

### 7.2 精准清理（需安装 ngx_cache_purge 模块）

```nginx
location ~ /purge(/.*) {
    allow 127.0.0.1;
    allow 192.168.1.0/24;
    deny all;
    proxy_cache_purge proxy_cache "$host$1$is_args$args";
}
```

```bash
# 清理指定缓存
curl http://127.0.0.1/purge/api/query/goods?id=1
```

### 7.3 缓存命中率统计

```bash
# 查看缓存命中情况
grep -o 'HIT\|MISS\|EXPIRED' /var/log/nginx/access.log | sort | uniq -c

# 计算命中率（企业级建议 ≥ 80%）
```

## 八、通用性能优化

```nginx
# Gzip 压缩
gzip on;
gzip_min_length 1k;
gzip_buffers 4 16k;
gzip_http_version 1.1;
gzip_types text/plain text/css application/json application/javascript;
gzip_vary on;

# 文件缓存
open_file_cache max=10000 inactive=60s;
open_file_cache_valid 30s;

# 长连接
keepalive_timeout 65;
keepalive_requests 100;

# 高效文件传输
sendfile on;
tcp_nopush on;
tcp_nodelay on;
```

## 九、常见问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 缓存始终 MISS | 缓存条件未满足 / 后端禁止缓存 | 降低 min_uses，检查后端 Cache-Control 头 |
| 缓存命中率低 | 缓存时间过短 / 范围过窄 | 延长缓存时间，扩大缓存范围 |
| 数据不一致 | 缓存时间过长 / 更新未清理 | 缩短缓存时间，更新后执行 purge |
| 缓存雪崩 | 大量缓存同时过期 | 使用 `~2m` 添加随机过期时间 |
| 磁盘空间不足 | 缓存文件过多 | 减小 max_size，缩短 inactive 时间 |
