# 05 - Nginx 负载均衡

> 🎯 Nginx 负载均衡是分布式架构的第一道分流 — upstream 集群+六种策略+健康检查，将流量合理分发到多台 Java 服务节点

---

## 目录

1. [核心组件](#一核心组件)
2. [负载均衡策略（6 种）](#二负载均衡策略6-种)
3. [健康检查与故障转移](#三健康检查与故障转移)
4. [Session 保持方案](#四session-保持方案)
5. [Java 后端最佳实践](#五java-后端最佳实践)

---

## 一、核心组件

| 组件 | 说明 |
|------|------|
| **upstream** | 定义后端服务集群（节点列表、策略、权重） |
| **proxy_pass** | 将请求转发到 upstream 定义的集群 |
| **负载策略** | 决定请求如何分发到后端节点 |
| **健康检查** | 自动检测节点状态，剔除/恢复故障节点 |

## 二、负载均衡策略

### 2.1 轮询（默认）

请求依次分发到每个节点，适合节点配置一致的场景：

```nginx
upstream java_backend {
    server 192.168.1.100:8080;
    server 192.168.1.101:8080;
    server 192.168.1.102:8080;
}
```

### 2.2 权重（最常用）

```nginx
upstream java_backend {
    server 192.168.1.100:8080 weight=3;    # 高配节点
    server 192.168.1.101:8080 weight=2;
    server 192.168.1.102:8080 weight=1;    # 低配节点
}
```

权重与节点配置成正比（如 8 核:4 核 = 2:1）。

### 2.3 IP 哈希

同一客户端 IP 始终分配到同一节点，解决本地 Session 问题：

```nginx
upstream java_backend {
    ip_hash;
    server 192.168.1.100:8080;
    server 192.168.1.101:8080;
}
```

> **企业推荐**：优先使用 Redis 共享 Session，避免依赖 ip_hash（会导致负载不均）。

### 2.4 最少连接

请求分发到当前连接数最少的节点：

```nginx
upstream java_backend {
    least_conn;
    server 192.168.1.100:8080 weight=2;
    server 192.168.1.101:8080;
}
```

### 2.5 URL 哈希

相同 URL 固定到同一节点，提高缓存命中率：

```nginx
upstream java_backend {
    hash $request_uri consistent;
    server 192.168.1.100:8080;
    server 192.168.1.101:8080;
}
```

## 三、策略选型速查

| 场景 | 推荐策略 |
|------|---------|
| 服务器配置相同、无状态 | 轮询 |
| 服务器配置不同 | **加权轮询** |
| 本地 Session（无 Redis） | ip_hash（临时方案） |
| 接口响应时间差异大 | **最少连接** |
| 缓存服务 | url_hash |

## 四、完整配置示例

```nginx
http {
    # 定义 Java 服务集群
    upstream java_backend {
        least_conn;                              # 最少连接策略
        server 192.168.1.100:8080 weight=2;      # 高配，权重 2
        server 192.168.1.101:8080 weight=1;      # 低配，权重 1
        server 192.168.1.102:8080 backup;        # 备用节点

        keepalive 100;                           # 长连接数
        keepalive_timeout 60s;

        # 被动健康检查
        proxy_next_upstream error timeout invalid_header http_500 http_502 http_503;
        proxy_next_upstream_tries 3;
        proxy_next_upstream_timeout 10s;
    }

    server {
        listen       80;
        server_name  api.example.com;

        location /api/ {
            proxy_pass http://java_backend/api/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

            proxy_connect_timeout 5s;
            proxy_read_timeout 10s;
        }
    }
}
```

## 五、健康检查

### 5.1 被动健康检查（默认）

```nginx
upstream java_backend {
    server 192.168.1.100:8080 max_fails=3 fail_timeout=30s;
    # max_fails=3：连续失败 3 次标记不可用
    # fail_timeout=30s：30 秒后重新探测
}
```

### 5.2 主动健康检查（推荐）

需安装 `nginx-module-ngx_http_upstream_check_module` 模块：

```nginx
upstream java_backend {
    server 192.168.1.100:8080;
    server 192.168.1.101:8080;

    # 每 3 秒探测一次，2 次成功恢复，3 次失败剔除
    check interval=3000 rise=2 fall=3 timeout=1000 type=http;
    check_http_send "HEAD /api/health HTTP/1.0\r\n\r\n";
    check_http_expect_alive http_200;
}
```

Java 后端配合：

```java
@RestController
@RequestMapping("/api")
public class HealthController {
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
}
```

## 六、高并发优化

```nginx
# 全局
worker_processes  auto;
worker_rlimit_nofile 65535;

events {
    worker_connections  10240;
    use epoll;
    multi_accept on;
}

# http
upstream java_backend {
    # ...
    keepalive 100;           # 与后端保持长连接
    keepalive_timeout 60s;
}

# 代理缓冲区
proxy_buffer_size 4k;
proxy_buffers 4 32k;
proxy_busy_buffers_size 64k;
```

## 七、限流保护

```nginx
http {
    # 按客户端 IP 限流：每秒 100 请求，突发 20
    limit_req_zone $binary_remote_addr zone=api_req:10m rate=100r/s;

    server {
        location /api/ {
            limit_req zone=api_req burst=20 nodelay;
            proxy_pass http://java_backend/api/;
        }
    }
}
```

## 八、Session 共享方案

推荐 **Redis 共享 Session** 替代 ip_hash：

```yaml
# Spring Boot
spring:
  session:
    store-type: redis
  redis:
    host: 192.168.1.103
    port: 6379
```

## 九、常见问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 请求全到同一节点 | ip_hash 启用 / 权重配置错误 | 检查策略配置，切换客户端验证 |
| 故障节点仍收请求 | 健康检查未配置或未生效 | 启用主动健康检查 |
| 负载不均 | 权重不合理 / 长连接导致 | 调整权重，使用 least_conn |
| 会话丢失 | 未共享 Session 且无 ip_hash | 优先 Redis 共享 Session |
| 节点切换慢 | 被动检查默认 10s+ 才剔除 | 启用主动健康检查（3s 探测） |
