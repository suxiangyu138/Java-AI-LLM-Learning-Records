# Nginx 在 Kubernetes 与微服务中的应用

## 一、Nginx 在 K8s 中的角色

| 角色 | 说明 |
|------|------|
| **Ingress Controller**（核心） | K8s 集群入口网关，统一路由外部请求 |
| **容器化应用** | Nginx 作为 Pod 部署，提供静态/代理服务 |

### Ingress Controller 工作流程

```
外部请求 → 集群节点:80/443 → Nginx Ingress Controller（Pod）
  → Ingress 规则（路由配置）→ Service → 微服务 Pod
```

## 二、Nginx 容器化部署

### 2.1 ConfigMap（存储配置）

```bash
kubectl create configmap nginx-config --from-file=nginx.conf=./nginx.conf
```

### 2.2 Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
spec:
  replicas: 2
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.24.0
        ports:
        - containerPort: 80
        volumeMounts:
        - name: nginx-config-volume
          mountPath: /etc/nginx/nginx.conf
          subPath: nginx.conf
        resources:
          requests:
            cpu: "100m"
            memory: "128Mi"
          limits:
            cpu: "500m"
            memory: "256Mi"
      volumes:
      - name: nginx-config-volume
        configMap:
          name: nginx-config
```

### 2.3 Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-service
spec:
  type: NodePort
  selector:
    app: nginx
  ports:
  - port: 80
    targetPort: 80
    nodePort: 30080
```

### 2.4 运维命令

```bash
# 更新配置
kubectl create configmap nginx-config --from-file=nginx.conf=./nginx.conf \
  -o yaml --dry-run=client | kubectl replace -f -

# 扩缩容
kubectl scale deployment nginx-deployment --replicas=3

# 查看日志
kubectl logs -f -l app=nginx
```

## 三、Ingress Controller 实战

### 3.1 安装

```bash
wget https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml
kubectl apply -f deploy.yaml

# 测试环境改为 NodePort
kubectl patch service ingress-nginx-controller -n ingress-nginx \
  -p '{"spec":{"type":"NodePort"}}'
```

### 3.2 Ingress 路由规则

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: api-ingress
  annotations:
    kubernetes.io/ingress.class: "nginx"
    nginx.ingress.kubernetes.io/rewrite-target: /$1
spec:
  rules:
  - host: api.example.com
    http:
      paths:
      - path: /api/v1/(.*)
        pathType: Prefix
        backend:
          service:
            name: java-service-1
            port:
              number: 8080
      - path: /api/v2/(.*)
        pathType: Prefix
        backend:
          service:
            name: java-service-2
            port:
              number: 8080
```

### 3.3 HTTPS 配置

```bash
kubectl create secret tls api-ssl-secret --cert=./cert.pem --key=./key.pem
```

```yaml
spec:
  tls:
  - hosts:
    - api.example.com
    secretName: api-ssl-secret
  rules:
    # ... 路由规则
```

### 3.4 Ingress 注解优化

| 注解 | 作用 | 示例值 |
|------|------|--------|
| `limit-rps` | 单 IP 限流 | `"100"` |
| `limit-burst` | 突发请求数 | `"20"` |
| `proxy-cache` | 开启缓存 | `"on"` |
| `proxy-cache-valid` | 缓存时间 | `"200 302 10m"` |
| `proxy-connect-timeout` | 连接超时 | `"5s"` |
| `proxy-read-timeout` | 读取超时 | `"10s"` |
| `force-ssl-redirect` | HTTP 强制跳转 HTTPS | `"true"` |

## 四、微服务架构中的 Nginx

### 4.1 核心角色

```
客户端 → Nginx（API 网关 + 负载均衡）
           ├── /api/user/*  → 用户服务集群
           ├── /api/order/* → 订单服务集群
           ├── /api/goods/* → 商品服务集群
           └── 限流 / 缓存 / SSL / 安全防护
```

### 4.2 作为 API 网关

```nginx
upstream user_service {
    least_conn;
    server 192.168.1.103:8080 weight=2;
    server 192.168.1.104:8080 weight=1;
    keepalive 100;
}

upstream order_service {
    least_conn;
    server 192.168.1.105:8080 weight=2;
    server 192.168.1.106:8080 weight=1;
    keepalive 100;
}

server {
    listen       443 ssl;
    server_name  api.example.com;

    # 全局跨域
    add_header Access-Control-Allow-Origin *;
    add_header Access-Control-Allow-Methods GET,POST,PUT,DELETE,OPTIONS;

    # 用户服务
    location /api/user/ {
        proxy_pass http://user_service/api/user/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 订单服务
    location /api/order/ {
        proxy_pass http://order_service/api/order/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 4.3 服务间代理

```nginx
# 内部域名，仅允许集群内网访问
server {
    listen       80;
    server_name  internal.example.com;

    allow 192.168.1.0/24;         # 仅集群内网
    deny all;

    location /api/goods/ {
        proxy_pass http://goods_service/api/goods/;
        proxy_next_upstream error timeout http_500 http_502;
        proxy_connect_timeout 3s;
        proxy_read_timeout 5s;
    }
}
```

### 4.4 动态负载均衡（对接 Nacos）

通过 `nginx-upsync-module` 插件动态获取服务实例：

```nginx
upstream user_service {
    upsync 192.168.1.200:8848/nacos/v1/ns/instance?serviceName=user-service
           upsync_timeout=60000 upsync_interval=5000 upsync_type=nacos;
    upsync_dump_path /etc/nginx/upsync_dump/user_service.conf;

    least_conn;
    keepalive 100;

    check interval=3000 rise=2 fall=3 timeout=1000 type=http;
    check_http_send "HEAD /api/user/health HTTP/1.0\r\n\r\n";
    check_http_expect_alive http_200;
}
```

## 五、微服务安全防护

```nginx
http {
    # 限流
    limit_req_zone $binary_remote_addr zone=api_req:10m rate=100r/s;

    # 恶意请求拦截
    if ($request_uri ~* "union|select|insert|delete|drop|exec") {
        return 403;
    }

    server {
        # 隐藏版本号
        server_tokens off;

        location /api/ {
            limit_req zone=api_req burst=20 nodelay;
            proxy_pass http://backend/api/;
        }
    }
}
```

## 六、最佳实践

| 类别 | 实践 |
|------|------|
| **部署** | Nginx 集群 + Keepalived + K8s Ingress Controller |
| **配置** | ConfigMap/Secret 管理，Git 版本控制 |
| **路由** | 统一路径格式 `/api/服务名/接口路径` |
| **健康检查** | 所有 upstream 必配主动健康检查 |
| **日志** | 统一格式，对接 ELK |
| **监控** | Prometheus + Grafana，核心指标告警 |
| **安全** | HTTPS 必开，版本隐藏，IP 白名单，恶意请求拦截 |

## 七、常见问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| Ingress 路由 404/503 | 规则配置错误 / 后端 Pod 故障 | `kubectl describe ingress` + 检查 Pod |
| Ingress Controller Pod 启动失败 | 镜像拉取/权限/资源不足 | 查看 Pod 日志，调整配置 |
| 动态实例不更新 | Nacos 对接失败 | 检查 upsync 配置和网络连通性 |
| HTTPS 证书错误 | Secret 配置错误 / 过期 | `kubectl describe secret` 验证 |
| 负载不均 | 策略配置不当 | 使用 least_conn + 权重 |
