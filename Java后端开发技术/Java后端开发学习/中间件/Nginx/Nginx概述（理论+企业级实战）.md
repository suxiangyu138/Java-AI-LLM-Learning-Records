# Nginx 概述（理论 + 企业级实战）

> **文档定位**：Java 后端企业级技术文档 | Nginx 核心知识  
> **定位**：高性能 HTTP 和反向代理服务器  
> **核心价值**：入口网关 + 负载均衡 + 静态资源处理

---

## 一、核心概念

### 1.1 什么是 Nginx

Nginx（发音 "engine x"）是由俄罗斯程序员 Igor Sysoev 开发的 **高性能 HTTP 和反向代理服务器**，同时是 IMAP/POP3/SMTP 代理服务器。核心定位是"轻量、高效、高并发"，是目前互联网行业最主流的 Web 服务器/反向代理服务器，与 Java 后端服务（Spring Boot、Tomcat）搭配使用，是企业级架构的核心组件。

### 1.2 Nginx 核心优势（Java 后端视角）

| 优势 | 说明 | Java 后端受益点 |
|------|------|----------------|
| **高并发能力** | 单台支持 10 万+ 并发连接，基于 epoll/kqueue IO 多路复用 | Tomcat 默认仅几百到几千并发，Nginx 承担入口抗压 |
| **反向代理** | 隐藏后端 Java 服务真实地址，统一入口 | 不暴露 Tomcat 8080 端口 |
| **负载均衡** | 请求均匀分发到多台 Java 服务节点，故障自动切换 | 实现 Tomcat 集群高可用 |
| **静态资源处理** | 直接处理 JS/CSS/图片/视频 | 减轻 Java 服务压力，提升访问速度 |
| **轻量稳定** | 内存占用几十 MB，7×24 小时运行稳定 | 低运维成本 |

### 1.3 Nginx 与 Java 后端的关系

```
客户端 → Nginx（守门人 + 调度员） → Tomcat 集群（业务处理） → 数据库
         ├── 过滤无效请求
         ├── 处理静态资源（不经过 Java）
         ├── 分发动态请求到 Java 服务
         ├── 负载均衡
         └── SSL 终结
```

---

## 二、底层原理

### 2.1 Master-Worker 进程架构

```
Master 进程
   ├── Worker 1（处理请求）
   ├── Worker 2（处理请求）
   ├── Worker 3（处理请求）
   └── ...（数量 = CPU 核心数）
```

- **Master 进程**：读取配置文件、管理 Worker 进程（启动/停止/热重载）、处理信号，不处理具体请求
- **Worker 进程**：基于 IO 多路复用模型（epoll），一个 Worker 可同时处理成千上万的连接，**非阻塞**

### 2.2 请求处理流程

```
客户端请求 → Nginx（根据配置判断）
    ├── 静态资源（.jpg/.css/.js）→ 直接读本地文件返回（不经过 Java）
    └── 动态请求（/api/*）→ proxy_pass 转发到 Java 服务 → 接收响应 → 返回客户端
```

> Java 后端重点关注"请求如何转发到 Java 服务"和"如何配置负载均衡"，底层 IO 模型了解即可。

---

## 三、代码实现

### 3.1 反向代理配置（最基础、最常用）

```nginx
# /etc/nginx/conf.d/default.conf
server {
    listen       80;                    # Nginx 监听 80 端口
    server_name  localhost;            # 替换为实际域名

    # 静态资源：/static/ 开头的请求直接读本地文件
    location /static/ {
        root   /usr/share/nginx/html;
        index  index.html index.htm;
        expires 1d;                    # 浏览器缓存 1 天
    }

    # 动态请求：/api/ 开头的请求转发到 Java 服务
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 兜底：根路径转发
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

> `proxy_set_header` 三个配置必须加，否则 Java 服务 `request.getRemoteAddr()` 获取到的是 Nginx IP 而非客户端真实 IP。

### 3.2 负载均衡配置（多节点集群）

```nginx
# 定义负载均衡集群
upstream java_server {
    server localhost:8080;             # Java 服务节点 1
    server localhost:8081;             # Java 服务节点 2
    # server localhost:8080 weight=2;  # 权重配置（越大分配到请求越多）
    # server localhost:8082 backup;    # 备用节点（主节点故障时启用）

    keepalive 100;                     # 保持长连接，减少连接建立开销
    proxy_next_upstream error timeout invalid_header; # 节点异常时自动切换
}

server {
    listen       80;
    server_name  localhost;

    location /api/ {
        proxy_pass http://java_server/api/;  # 指向集群名称（非具体地址）
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location / {
        proxy_pass http://java_server;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

**核心机制**：
- 默认 **轮询策略**（Round-Robin），依次分发请求
- `weight` 配置权重，高性能节点分配更多请求
- 故障节点自动剔除，恢复后自动加入

### 3.3 静态资源优化

```nginx
server {
    listen       80;
    server_name  localhost;

    # 匹配静态资源后缀，直接处理
    location ~* \.(jpg|jpeg|png|gif|css|js|pdf|mp4)$ {
        root   /usr/share/nginx/static;
        expires 7d;                    # 浏览器缓存 7 天
        gzip on;                       # 开启 Gzip 压缩
        gzip_types image/jpeg image/png text/css application/javascript;
    }

    location /api/ {
        proxy_pass http://java_server/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

**Java 后端配合**：Spring Boot 中禁止静态资源映射以避免冲突：

```yaml
spring:
  web:
    resources:
      add-mappings: false  # 禁止 Spring Boot 处理静态资源
```

### 3.4 SSL/HTTPS 配置

```nginx
server {
    listen       443 ssl;              # HTTPS 默认端口
    server_name  www.example.com;      # 必须与 SSL 证书域名一致

    ssl_certificate      /etc/nginx/ssl/example.crt;  # 证书公钥
    ssl_certificate_key  /etc/nginx/ssl/example.key;  # 证书私钥
    ssl_session_cache    shared:SSL:1m;
    ssl_session_timeout  10m;
    ssl_ciphers  HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers  on;

    location /api/ {
        proxy_pass http://java_server/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 3.5 环境准备

```bash
# 安装 Nginx（CentOS）
yum install -y gcc pcre pcre-devel zlib zlib-devel openssl openssl-devel
yum install -y nginx

# 启动 + 开机自启
systemctl start nginx
systemctl enable nginx

# 核心配置文件
# /etc/nginx/nginx.conf         主配置文件
# /etc/nginx/conf.d/default.conf  自定义站点配置（推荐修改此文件）
```

---

## 四、实战要点

### 4.1 Java 后端常用场景速查

| 场景 | Nginx 配置 | Java 服务配合 |
|------|-----------|-------------|
| 反向代理 API | `proxy_pass http://localhost:8080/api/` | 不需特殊处理 |
| 负载均衡 | `upstream` 定义集群 + `proxy_pass http://upstream_name` | 多节点部署 |
| 静态资源分离 | `location ~* \.(jpg|png|css|js)$` | 禁止 Spring Boot 静态资源映射 |
| HTTPS 加密 | `listen 443 ssl` + 证书配置 | 无变化（SSL 终结于 Nginx） |
| 访问日志排查 | 默认 access.log + error.log | Nginx 转发真实客户端 IP |

### 4.2 性能调优要点

| 参数 | 建议值 | 说明 |
|------|--------|------|
| `worker_processes` | `auto`（= CPU 核数） | Worker 进程数 |
| `worker_connections` | 1024~4096 | 单 Worker 最大连接数 |
| `keepalive_timeout` | 65 | 长连接超时 |
| `gzip on` | 开启 | 压缩响应体 |

---

## 五、避坑总结

| 坑点 | 原因 | 解决方案 |
|------|------|----------|
| **Java 服务获取不到客户端真实 IP** | 漏配 `proxy_set_header X-Real-IP` | 必须配置 `X-Real-IP` 和 `X-Forwarded-For` |
| **Nginx 转发后接口 404** | `proxy_pass` 尾部 `/` 问题 | `proxy_pass http://host/api/` vs `proxy_pass http://host/api` 区分 |
| **静态资源仍经过 Java** | Spring Boot 未禁用静态资源映射 | `spring.web.resources.add-mappings=false` |
| **SSL 证书不生效** | 证书路径错误或与 `server_name` 不匹配 | 检查路径 + 域名一致性 |
| **负载均衡节点未切换** | 故障检测周期较长 | 添加 `proxy_next_upstream error timeout` |

---

## 六、企业级最佳实践

### 6.1 企业级配置模板

```nginx
upstream backend {
    server 127.0.0.1:8080 weight=3 max_fails=3 fail_timeout=30s;
    server 127.0.0.1:8081 weight=2 max_fails=3 fail_timeout=30s;
    keepalive 64;
}

server {
    listen 80;
    server_name api.example.com;
    return 301 https://$host$request_uri;  # HTTP → HTTPS 强制跳转
}

server {
    listen 443 ssl http2;
    server_name api.example.com;

    # SSL 配置
    ssl_certificate     /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;

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
        proxy_pass http://backend/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 30s;
    }
}
```

### 6.2 运维 Checklist

- [ ] Nginx 开机自启已配置（`systemctl enable nginx`）
- [ ] 日志轮转已配置（防止磁盘占满）
- [ ] 健康检查端点已配置（`/health`）
- [ ] 监控告警已部署（Nginx 状态页 + Prometheus exporter）
- [ ] 配置文件有版本控制（Git 管理）
- [ ] 配置变更前已测试（`nginx -t`）
