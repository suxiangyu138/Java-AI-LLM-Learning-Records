# Nginx 核心配置

## 一、配置块层级

```
全局块 → events 块 → http 块 → server 块 → location 块
```

## 二、全局块与 events 块

```nginx
user  nginx nginx;                        # 运行用户（安全必配）
worker_processes  auto;                   # 进程数 = CPU 核数
error_log  /var/log/nginx/error.log warn; # 错误日志
pid        /var/run/nginx.pid;            # PID 文件

events {
    worker_connections  10240;            # 单 Worker 最大连接数
    use epoll;                            # Linux 下高性能 IO 模型
    multi_accept on;                      # 同时接收多连接
}
```

## 三、http 块（全局 HTTP 配置）

### 3.1 基础配置

```nginx
http {
    include       mime.types;
    default_type  application/octet-stream;

    # 日志格式（Java 后端必配）
    log_format  main  '$remote_addr [$time_local] "$request" '
                      '$status $body_bytes_sent "$http_referer" '
                      '"$http_user_agent" "$http_x_forwarded_for" '
                      '$upstream_addr $upstream_response_time';

    access_log  /var/log/nginx/access.log  main;

    # 文件传输优化
    sendfile        on;
    tcp_nopush      on;
    keepalive_timeout  65;

    # Gzip 压缩
    gzip  on;
    gzip_min_length  1k;
    gzip_types  text/plain text/css application/json application/javascript;
}
```

### 3.2 关键变量

| 变量 | 含义 | Java 后端用途 |
|------|------|-------------|
| `$remote_addr` | 客户端 IP | 日志记录 |
| `$http_x_forwarded_for` | 经过代理的真实客户端 IP | `request.getHeader("X-Forwarded-For")` |
| `$request` | 请求行（方法 + 路径 + 协议） | 排查请求路径错误 |
| `$status` | 响应状态码 | 排查 404/502 等异常 |
| `$upstream_addr` | 后端节点 IP:端口 | 排查负载分发情况 |
| `$upstream_response_time` | 后端响应耗时 | 排查后端性能瓶颈 |

## 四、server 块（虚拟主机）

```nginx
server {
    listen       80;                   # 监听端口
    server_name  api.example.com;     # 绑定域名

    # 访问日志可单独配置
    access_log  /var/log/nginx/api_access.log  main;
}
```

## 五、location 块（请求匹配与处理）

### 5.1 匹配规则优先级

| 优先级 | 规则 | 示例 |
|:------:|------|------|
| 1 | `=` 精确匹配 | `location = /api` |
| 2 | `^~` 前缀匹配（不检查正则） | `location ^~ /static/` |
| 3 | `~` / `~*` 正则匹配 | `location ~* \.(jpg\|png)$` |
| 4 | 普通前缀匹配 | `location /api/` |

### 5.2 proxy_pass（反向代理核心）

```nginx
location /api/ {
    # 转发到单节点
    proxy_pass http://127.0.0.1:8080/api/;

    # 转发到集群
    # proxy_pass http://backend_cluster/api/;

    # 必配：传递客户端真实信息
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

    # 超时控制
    proxy_connect_timeout 5s;
    proxy_read_timeout 10s;
    proxy_send_timeout 10s;
}
```

> **关键**：`proxy_pass` 末尾 `/` 决定路径拼接规则。  
> `/api/` + `proxy_pass http://host/api/` → 去掉 `/api/` 前缀后转发  
> `/api/` + `proxy_pass http://host` → 完整路径转发

### 5.3 静态资源处理

```nginx
# root：实际路径 = root + location 路径
location /static/ {
    root   /usr/local/nginx/html;     # /usr/local/nginx/html/static/
    expires 7d;
}

# alias：实际路径 = alias 路径（忽略 location）
location /static/ {
    alias  /data/static/;             # /data/static/
    expires 7d;
}

# 正则匹配静态资源后缀
location ~* \.(jpg|jpeg|png|gif|css|js|pdf)$ {
    root   /usr/local/nginx/static;
    expires 30d;
    gzip on;
}
```

### 5.4 HTTPS 配置

```nginx
server {
    listen       443 ssl;
    server_name  api.example.com;

    ssl_certificate      /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key  /etc/nginx/ssl/privkey.pem;

    ssl_session_cache    shared:SSL:1m;
    ssl_session_timeout  10m;
    ssl_ciphers  HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers  on;

    location /api/ {
        proxy_pass http://backend/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}

# HTTP 自动跳转 HTTPS
server {
    listen       80;
    server_name  api.example.com;
    return 301 https://$host$request_uri;
}
```

## 六、企业级完整配置模板

```nginx
user  nginx nginx;
worker_processes  auto;
error_log  /var/log/nginx/error.log  warn;
pid        /var/run/nginx.pid;

events {
    worker_connections  10240;
    use epoll;
    multi_accept on;
}

http {
    include       mime.types;
    default_type  application/octet-stream;

    log_format  main  '$remote_addr [$time_local] "$request" '
                      '$status $body_bytes_sent "$http_x_forwarded_for" '
                      '$upstream_addr $upstream_response_time $request_time';

    access_log  /var/log/nginx/access.log  main;
    sendfile     on;
    tcp_nopush   on;
    keepalive_timeout  65;

    gzip  on;
    gzip_types  text/plain text/css application/json application/javascript;

    # 后端集群
    upstream java_backend {
        server 127.0.0.1:8080 weight=2;
        server 127.0.0.1:8081;
        keepalive 100;
    }

    # HTTP → HTTPS 重定向
    server {
        listen       80;
        server_name  api.example.com;
        return 301 https://$host$request_uri;
    }

    # HTTPS 主服务
    server {
        listen       443 ssl http2;
        server_name  api.example.com;

        ssl_certificate      /etc/nginx/ssl/fullchain.pem;
        ssl_certificate_key  /etc/nginx/ssl/privkey.pem;

        # 安全头
        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header X-Content-Type-Options "nosniff" always;

        # 静态资源
        location ~* \.(jpg|png|gif|css|js|woff2)$ {
            root /var/www/static;
            expires 30d;
        }

        # API 转发
        location /api/ {
            proxy_pass http://java_backend/api/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_read_timeout 30s;
        }
    }
}
```

## 七、常见配置问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| Java 获取不到真实 IP | 未配置 `X-Real-IP` / `X-Forwarded-For` | 必须配置 `proxy_set_header` |
| 转发后接口 404 | `proxy_pass` 末尾 `/` 问题 | 检查路径拼接规则 |
| 静态资源仍经 Java | Spring Boot 未禁用静态映射 | `spring.web.resources.add-mappings=false` |
| SSL 不生效 | 证书路径或域名不匹配 | 检查证书路径 + `server_name` 一致性 |
