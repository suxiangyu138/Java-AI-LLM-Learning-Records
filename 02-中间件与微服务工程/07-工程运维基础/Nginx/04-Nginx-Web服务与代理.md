# 04 - Nginx Web 服务与代理

> 🎯 Nginx 的两大核心能力：静态资源服务（比 Tomcat 快 10 倍）和反向代理（隐藏后端、统一入口） — Java 后端架构的标配组合

---

## 目录

1. [静态 Web 站点部署](#一静态-web-站点部署)
2. [反向代理](#二反向代理)
3. [动静分离](#三动静分离)
4. [WebSocket 代理](#四websocket-代理)
5. [SSL/HTTPS 基础配置](#五sslhttps-基础配置)

---

## 一、静态 Web 站点部署

### 部署前端项目（Vue/React）

```bash
# 复制打包文件到 Nginx 静态目录
cp -r /root/dist /usr/local/nginx/html/
```

```nginx
server {
    listen       80;
    server_name  www.example.com;

    location / {
        root   /usr/local/nginx/html/dist;
        index  index.html;
        try_files $uri $uri/ /index.html;    # 解决前端路由刷新 404
    }

    location ~* \.(jpg|png|css|js)$ {
        root   /usr/local/nginx/html/dist;
        expires 7d;
        gzip on;
    }
}
```

### Spring Boot 配合

```yaml
# 禁止 Spring Boot 处理静态资源，全部交给 Nginx
spring:
  web:
    resources:
      add-mappings: false
```

## 二、反向代理（Java 后端最常用）

### 单节点代理

```nginx
server {
    listen       80;
    server_name  api.example.com;

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### 前后端同域名

```nginx
server {
    listen       80;
    server_name  www.example.com;

    # 前端静态资源
    location / {
        root   /usr/local/nginx/html/dist;
        index  index.html;
    }

    # 后端接口转发
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 三、代理类型

| 类型 | 核心作用 | 典型场景 |
|------|---------|---------|
| **反向代理**（最常用） | 隐藏后端服务，统一入口 | Java 接口代理、集群入口 |
| **正向代理** | 客户端通过代理访问外网 | 内网爬虫、访问限制突破 |
| **透明代理** | 无需客户端配置，自动转发 | 企业网关、流量监控 |

### 正向代理配置

```nginx
server {
    listen       8081;
    resolver     8.8.8.8 114.114.114.114;   # DNS 解析器（必填）

    location / {
        proxy_pass http://$http_host$request_uri;
        proxy_set_header Host $http_host;
    }
}
```

## 四、跨域解决方案

前端与 Java 接口不同域时，通过 Nginx 代理统一入口解决跨域：

```nginx
server {
    listen       80;
    server_name  www.example.com;

    # 前端
    location / {
        root   /usr/local/nginx/html/dist;
        index  index.html;
    }

    # 接口代理（同域访问，无跨域问题）
    location /api/ {
        proxy_pass http://java_backend/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

前端请求改为：`/api/user/list`（不再写完整后端域名），浏览器同源策略不再拦截。

## 五、动静分离

```nginx
server {
    listen       443 ssl;
    server_name  www.example.com;

    # 静态资源：Nginx 直接处理
    location ~* \.(jpg|jpeg|png|gif|css|js|pdf|mp4)$ {
        root   /usr/local/nginx/static;
        expires 7d;
        gzip on;
        add_header Cache-Control "public, max-age=604800";
    }

    # 动态接口：转发到 Java 服务
    location /api/ {
        proxy_pass http://java_backend/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 前端首页
    location / {
        root   /usr/local/nginx/html/dist;
        index  index.html;
    }
}
```

## 六、HTTPS + 反向代理

```nginx
# 定义后端集群
upstream java_backend {
    server 192.168.1.100:8080;
    server 192.168.1.101:8080;
}

# HTTPS 主服务
server {
    listen       443 ssl;
    server_name  api.example.com;

    ssl_certificate      /etc/nginx/ssl/cert.pem;
    ssl_certificate_key  /etc/nginx/ssl/key.pem;

    ssl_session_cache    shared:SSL:1m;
    ssl_session_timeout  10m;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers  HIGH:!aNULL:!MD5;

    location /api/ {
        proxy_pass http://java_backend/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}

# HTTP 自动跳转 HTTPS
server {
    listen       80;
    server_name  api.example.com;
    return 301 https://$host$request_uri;
}
```

## 七、常见问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 反向代理返回 502 | Java 服务未启动或端口错误 | `curl http://127.0.0.1:8080/api/` 验证 |
| 静态资源 404 | 路径错误或权限不足 | 检查 root 路径 + `chown nginx:nginx` |
| 跨域配置不生效 | OPTIONS 预检请求未处理 | 添加 `if ($request_method = 'OPTIONS') { return 204; }` |
| HTTPS 证书错误 | 证书路径或域名不匹配 | 检查路径 + `server_name` 一致性 |
| Java 获取不到真实 IP | 未配置 proxy_set_header | 必配 `X-Real-IP` 和 `X-Forwarded-For` |
