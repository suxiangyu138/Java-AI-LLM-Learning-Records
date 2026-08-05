# 07-Ingress与七层路由
> 🎯 Service负责K8s集群内部的L4层负载均衡 — Pod IP变化时自动更新DNS。但在生产环境中，我们需要**从外部访问集群内服务**，并且在域名+路径级别做路由分发，这就是Ingress的使命

---

## 目录
1. [Ingress解决了什么问题](#1-ingress解决了什么问题)
2. [Ingress Controller — 真正的网关引擎](#2-ingress-controller--真正的网关引擎)
3. [Ingress规则详解](#3-ingress规则详解)
4. [TLS/HTTPS配置](#4-tlshttps配置)
5. [常用Annotations速查](#5-常用annotations速查)
6. [金丝雀发布（Canary）](#6-金丝雀发布canary)
7. [Ingress vs Gateway API](#7-ingress-vs-gateway-api)
8. [完整示例：多域名多服务路由](#8-完整示例多域名多服务路由)

---

## 1. Ingress解决了什么问题

### 1.1 从Service到Ingress的演进

```
外部请求
    │
    ├──  无Ingress时代：每个Service一个LoadBalancer ──── 成本爆炸！
    │
    │   api.example.com  ──►  LoadBalancer 1 ──► Service A (NodePort/LB)
    │   blog.example.com ──►  LoadBalancer 2 ──► Service B (NodePort/LB)
    │   admin.example.com─►  LoadBalancer 3 ──► Service C (NodePort/LB)
    │
    └──  Ingress时代：统一入口 ──── 一个入口网关搞定所有服务！
    
    api.example.com ──┐
    blog.example.com──┤
    admin.example.com─┤
                      │
    *.example.com ────┼──►  Ingress (Nginx/Traefik)
                      │         │
                      │         ├── /api/*  ──► Service A
                      │         ├── /blog/* ──► Service B
                      │         └── /admin/* ─► Service C
                      │
                      └──►  LoadBalancer (或NodePort) ──► 集群入口
```

### 1.2 核心价值

| 维度 | 只有Service | 加上Ingress |
|------|-------------|-------------|
| 对外暴露方式 | 每个Service一个LoadBalancer（贵！） | 一个LoadBalancer + 七层路由 |
| 路由能力 | 仅L4（IP+端口） | L7（域名+路径+Header） |
| TLS终止 | 需要Service自行处理 | Ingress统一管理证书 |
| 流量分发 | 简单轮询 | 灰度、重定向、Header改写 |
| 成本 | 多个LB → 多个云资源 | 单个LB + Ingress Controller Pod |

> 💡 一句话总结：**Ingress是集群的"API网关"，统一管理所有外部流量的七层路由**。没有Ingress，每个对外暴露的Service都需要独立的LoadBalancer（每个LB几十刀/月，多Service时成本爆炸）。

---

## 2. Ingress Controller — 真正的网关引擎

### 2.1 Ingress资源 vs Ingress Controller

这是一个非常重要的区分：

| 概念 | 是什么 | 类比 |
|------|--------|------|
| **Ingress资源** | 一个K8s API对象（YAML配置），定义了路由规则 | Nginx的`server.conf`配置 |
| **Ingress Controller** | 运行在集群中的实际网关程序，**读取Ingress资源并实现路由** | Nginx进程本身 |

> ⚠️ **Ingress只是声明，Ingress Controller才是执行者！** 只创建Ingress YAML但不安装Ingress Controller，路由不会生效。

### 2.2 安装Ingress Controller（最流行的方式）

```bash
# 安装Nginx Ingress Controller（最流行）
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.0/deploy/static/provider/cloud/deploy.yaml

# 验证安装
kubectl get pods -n ingress-nginx
# NAME                                        READY   STATUS
# ingress-nginx-controller-xxxxxxxxx-xxxxx    1/1     Running
```

### 2.3 主流Ingress Controller对比

| Controller | 语言 | 性能 | 特点 | 适用场景 |
|------------|------|------|------|----------|
| **Nginx Ingress** | Go + Lua | ⭐⭐⭐⭐ | 生态最成熟、社区最大、文档最全 | **生产首选** |
| **Traefik** | Go | ⭐⭐⭐⭐ | 自动发现、自动HTTPS、Dashboard可视化 | 微服务、中小规模 |
| **HAProxy Ingress** | Go | ⭐⭐⭐⭐⭐ | 性能极高、配置灵活 | 高并发场景（电商、实时） |
| **Istio Gateway** | Go/Envoy | ⭐⭐⭐ | 服务网格原生、流量管理能力极强 | 需要完整服务网格时 |
| **Kong Ingress** | Lua/Go | ⭐⭐⭐⭐ | 插件生态丰富（限流/认证/日志） | API管理+网关一体化 |
| **Apache APISIX** | Lua/Go | ⭐⭐⭐⭐⭐ | 超高性能、动态路由、丰富插件 | 云原生API网关 |

> 💡 **选型建议**：如果没有特殊需求，直接选**Nginx Ingress Controller**（社区最活跃、厂商支持最多）。需要服务网格时考虑Istio Gateway，需要API管理功能考虑Kong或APISIX。

---

## 3. Ingress规则详解

### 3.1 基础语法结构

```yaml
apiVersion: networking.k8s.io/v1    # ⭐ v1为稳定版（以前为extensions/v1beta1）
kind: Ingress
metadata:
  name: example-ingress
  annotations:                       # 行为控制配置
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx            # ⭐ K8s 1.18+ 替代 annotation
  rules:                             # 路由规则
  - host: api.example.com            # 域名匹配
    http:
      paths:
      - path: /users                 # 路径匹配
        pathType: Prefix             # 匹配类型
        backend:
          service:
            name: user-service       # 目标Service
            port:
              number: 8080
```

### 3.2 pathType匹配规则

| pathType | 匹配逻辑 | 示例 |
|----------|----------|------|
| `Prefix` | 路径前缀匹配（支持通配） | `/api`匹配`/api`、`/api/v1`、`/api/v1/users` |
| `Exact` | 精确匹配 | `/api`**只**匹配`/api`，不匹配`/api/`或`/api/v1` |
| `ImplementationSpecific` | 取决于Controller实现 | Nginx的`=`（精确）或`~`（正则） |

### 3.3 多规则路由

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: multi-route-ingress
spec:
  ingressClassName: nginx
  rules:
  # 规则1：api.example.com → 路由到不同Service根据路径
  - host: api.example.com
    http:
      paths:
      - path: /users
        pathType: Prefix
        backend:
          service:
            name: user-service
            port:
              number: 8080
      - path: /orders
        pathType: Prefix
        backend:
          service:
            name: order-service
            port:
              number: 8080
      - path: /products
        pathType: Prefix
        backend:
          service:
            name: product-service
            port:
              number: 8080

  # 规则2：不同的域名 → 不同Service
  - host: admin.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: admin-service
            port:
              number: 8080

  # 规则3：默认后端（无匹配时的兜底）
  - http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: default-service
            port:
              number: 8080
```

### 3.4 默认后端（Default Backend）

Ingress可以配置一个**默认后端**，当所有规则都不匹配时（如域名不存在）将流量路由到此Service：

```yaml
spec:
  defaultBackend:                # ⭐ 兜底：404页面或统一入口
    service:
      name: default-backend
      port:
        number: 80
```

> 💡 默认后端通常部署一个返回404或友好提示页面的Service，避免访问到不存在的路由时直接连接拒绝。

---

## 4. TLS/HTTPS配置

### 4.1 基础TLS配置

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: tls-ingress
spec:
  ingressClassName: nginx
  tls:                              # ⭐ TLS配置
  - hosts:
    - api.example.com
    secretName: api-tls-secret      # 证书Secret
  - hosts:
    - admin.example.com
    secretName: admin-tls-secret
  rules:
  - host: api.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: api-service
            port:
              number: 8080
```

### 4.2 证书Secret格式

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: api-tls-secret
type: kubernetes.io/tls             # ⭐ 固定类型
data:
  tls.crt: LS0tLS1CRUdJTi...        # Base64编码的证书
  tls.key: LS0tLS1CRUdJTi...        # Base64编码的私钥
```

```bash
# 用自签名证书创建TLS Secret
kubectl create secret tls api-tls-secret \
  --cert=path/to/tls.crt \
  --key=path/to/tls.key
```

### 4.3 cert-manager + Let's Encrypt（自动证书管理）

手工管理证书太麻烦，生产环境推荐使用cert-manager自动申请和续期Let's Encrypt证书：

```yaml
---
# 1. 安装cert-manager
# kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.0/cert-manager.yaml

# 2. 创建ClusterIssuer（证书签发机构）
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@example.com
    privateKeySecretRef:
      name: letsencrypt-prod-key
    solvers:
    - http01:
        ingress:
          class: nginx

---
# 3. Ingress中引用cert-manager（自动创建Secret）
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: auto-tls-ingress
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"  # ⭐ 自动签发
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - api.example.com
    secretName: api-tls-cert          # ⭐ cert-manager自动创建此Secret
  rules:
  - host: api.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: api-service
            port:
              number: 8080
```

> 🎯 **最佳实践**：永远不要手工管理证书！cert-manager + Let's Encrypt实现全自动证书申请、续期、注入，证书过期前自动重新签发，零人工介入。

---

## 5. 常用Annotations速查

Ingress的Annotaions是配置行为的"瑞士军刀"，每个Ingress Controller有自己的Annotations生态。以下是Nginx Ingress最常用的：

| Annotation | 默认值 | 作用 | 示例 |
|------------|--------|------|------|
| `nginx.ingress.kubernetes.io/rewrite-target` | — | **路径重写**（最常用） | `rewrite-target: /$2` |
| `nginx.ingress.kubernetes.io/ssl-redirect` | `true` | HTTP自动跳转到HTTPS | `ssl-redirect: "false"` |
| `nginx.ingress.kubernetes.io/force-ssl-redirect` | `false` | 强制所有访问跳HTTPS | `force-ssl-redirect: "true"` |
| `nginx.ingress.kubernetes.io/proxy-body-size` | `1m` | **请求体大小限制** | `proxy-body-size: "10m"` |
| `nginx.ingress.kubernetes.io/proxy-read-timeout` | `60` | 后端超时时间（秒） | `proxy-read-timeout: "3600"` |
| `nginx.ingress.kubernetes.io/cors-allow-origin` | — | **跨域CORS配置** | `*` 或 `https://app.example.com` |
| `nginx.ingress.kubernetes.io/cors-allow-methods` | `GET, PUT, POST, DELETE` | CORS允许的方法 | `GET, POST, PUT, DELETE, OPTIONS` |
| `nginx.ingress.kubernetes.io/enable-cors` | — | 开启CORS | `"true"` |
| `nginx.ingress.kubernetes.io/limit-rps` | — | **单IP每秒请求数限制** | `limit-rps: "10"` |
| `nginx.ingress.kubernetes.io/whitelist-source-range` | — | IP白名单 | `192.168.0.0/16,10.0.0.0/8` |
| `nginx.ingress.kubernetes.io/configuration-snippet` | — | 注入自定义Nginx配置片段 | 高级定制场景 |
| `nginx.ingress.kubernetes.io/auth-url` | — | **外部认证**配置 | `http://auth-service/auth` |

### 5.1 路径重写详解（rewrite-target）

路径重写是最常用的Annotation，它解决了"Ingress路径不会传递给后端Service"的问题：

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: rewrite-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /$2   # ⭐ 捕获组重写
spec:
  ingressClassName: nginx
  rules:
  - host: api.example.com
    http:
      paths:
      # 请求 /api/users → 转发到user-service:8080/users（去掉/api前缀）
      - path: /api(/|$)(.*)
        pathType: ImplementationSpecific  # ⭐ 正则需用此类型
        backend:
          service:
            name: user-service
            port:
              number: 8080
```

| 请求路径 | 没有rewrite-target | 有rewrite-target: /$2 |
|----------|-------------------|----------------------|
| `/api/users` | `/api/users`（404！后端无此路径） | `/users`（✅ 正常） |
| `/api/v1/orders` | `/api/v1/orders`（404！） | `/v1/orders`（✅ 正常） |
| `/api/payment/callback` | `/api/payment/callback`（404！） | `/payment/callback`（✅ 正常） |

### 5.2 CORS配置示例

```yaml
metadata:
  annotations:
    nginx.ingress.kubernetes.io/enable-cors: "true"
    nginx.ingress.kubernetes.io/cors-allow-origin: "https://admin.example.com"
    nginx.ingress.kubernetes.io/cors-allow-methods: "GET, POST, PUT, DELETE, OPTIONS"
    nginx.ingress.kubernetes.io/cors-allow-headers: "Authorization, Content-Type"
    nginx.ingress.kubernetes.io/cors-allow-credentials: "true"
```

### 5.3 速率限制 + IP白名单

```yaml
metadata:
  annotations:
    nginx.ingress.kubernetes.io/limit-rps: "20"            # 每秒请求数限制
    nginx.ingress.kubernetes.io/limit-burst: "40"          # 突发流量
    nginx.ingress.kubernetes.io/whitelist-source-range: "10.0.0.0/8, 172.16.0.0/12"
```

---

## 6. 金丝雀发布（Canary）

Nginx Ingress Controller支持通过Annotations实现**金丝雀发布**（灰度发布），将部分流量引流到新版本的Service：

### 6.1 金丝雀规则

```yaml
---
# 稳定版本（主Ingress）
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: app-main
spec:
  ingressClassName: nginx
  rules:
  - host: app.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: app-service-v1       # 稳定版v1
            port:
              number: 8080

---
# 金丝雀版本（Canary Ingress）
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: app-canary
  annotations:
    nginx.ingress.kubernetes.io/canary: "true"                # ⭐ 标记为金丝雀
    nginx.ingress.kubernetes.io/canary-weight: "10"           # ⭐ 10%流量到v2
    # 或者按Header/Header + Cookie分发
    # nginx.ingress.kubernetes.io/canary-by-header: "X-Canary"
    # nginx.ingress.kubernetes.io/canary-by-cookie: "canary_user"
spec:
  ingressClassName: nginx
  rules:
  - host: app.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: app-service-v2       # 金丝雀v2
            port:
              number: 8080
```

### 6.2 金丝雀分发策略

| Annotation | 机制 | 典型场景 | 示例值 |
|------------|------|----------|--------|
| `canary-weight` | 按百分比随机分配流量 | 小流量验证新版本 | `5`（5%流量） |
| `canary-by-header` | 按自定义Header分发 | 内部测试：指定Header才走新版本 | `X-Canary: always` |
| `canary-by-cookie` | 按Cookie分发 | 会话级别灰度 | `canary_user: true` |

> 💡 金丝雀发布流程：`weight=5`（5%流量验证）→ `weight=20`（扩大验证）→ `weight=100`（全量）→ 切换主Ingress指向v2 → 下线canary。

---

## 7. Ingress vs Gateway API

### 7.1 Gateway API是什么

Gateway API是K8s SIG-Network推出的**新一代网关标准**（K8s v1.24+ beta），旨在替代Ingress API。它解决了Ingress的多个痛点：

| 维度 | Ingress API | Gateway API |
|------|-------------|-------------|
| 发布阶段 | GA（稳定） | Beta（持续演进） |
| 路由能力 | 域名+路径 | 域名+路径+Header+端口+方法+权重 |
| 多租户支持 | 无 | 原生支持（Role/ClusterRole分离） |
| 协议支持 | HTTP/HTTPS | HTTP/HTTPS/gRPC/TCP/UDP/TLS |
| 精细化流量 | 无 | Header匹配、权重路由、镜像流量 |
| 后端协议 | 仅HTTP | HTTP/gRPC/TLS/TCP |
| 实现方 | 单一Controller | 多个Provider（GatewayClass） |
| 扩展性 | Annotations | 原生Policy Attachment机制 |

### 7.2 Gateway API快速示例

```yaml
---
# Gateway（网络入口）
apiVersion: gateway.networking.k8s.io/v1beta1
kind: Gateway
metadata:
  name: app-gateway
spec:
  gatewayClassName: nginx-gateway
  listeners:
  - name: http
    protocol: HTTP
    port: 80
    hostname: "*.example.com"
  - name: https
    protocol: HTTPS
    port: 443
    hostname: "*.example.com"
    tls:
      mode: Terminate
      certificateRefs:
      - name: example-cert

---
# HTTPRoute（路由规则，比Ingress更丰富）
apiVersion: gateway.networking.k8s.io/v1beta1
kind: HTTPRoute
metadata:
  name: user-api-route
spec:
  parentRefs:
  - name: app-gateway                     # 绑定到Gateway
  hostnames:
  - "api.example.com"
  rules:
  - matches:
    - path:
        type: PathPrefix
        value: /users
    backendRefs:
    - name: user-service-v1
      port: 8080
      weight: 90                          # ⭐ 权重路由（90%流量）
    - name: user-service-v2
      port: 8080
      weight: 10                          # ⭐ 10%流量金丝雀
  - matches:
    - headers:
      - name: X-Env
        value: canary                     # ⭐ Header条件路由
    backendRefs:
    - name: user-service-v2
      port: 8080
```

### 7.3 迁移建议

> 💡 **何时从Ingress迁移到Gateway API？** 当前Ingress对大多数场景已经够用。Gateway API适合以下场景：需要gRPC路由、精细化流量管理、多租户、或需要跨Namespace路由。建议持续关注Gateway API的发展，在K8s版本支持成熟后逐步采用。

---

## 8. 完整示例：多域名多服务路由

以下是一个生产级API网关配置，涵盖多域名、多路径、HTTPS、路径重写、CORS、速率限制。

### 8.1 架构说明

```
                     Internet
                        │
                    ┌───▼───┐
                    │  DNS   │
                    └───┬───┘
                        │
              api.example.com   admin.example.com
                        │              │
                    ┌───▼──────────────▼───┐
                    │   Ingress Controller  │
                    │    (Nginx Ingress)    │ ← HTTPS终止 + 路由 + 限流
                    └───┬──────────────┬───┘
                        │              │
              ┌─────────▼──┐    ┌──────▼──────────┐
              │ user-service│    │  admin-service  │
              │ :8080       │    │  :8080          │
              └─────────────┘    └─────────────────┘
                    │                    │
              ┌─────▼──────┐    ┌───────▼────────┐
              │order-service│    │ audit-service   │
              │ :8080       │    │  :8081          │
              └─────────────┘    └─────────────────┘
```

### 8.2 完整YAML

```yaml
---
# 1. TLS证书Secret（cert-manager自动管理）
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: devops@example.com
    privateKeySecretRef:
      name: letsencrypt-prod-key
    solvers:
    - http01:
        ingress:
          class: nginx

---
# 2. Ingress主配置
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: api-gateway
  annotations:
    # 通用配置
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"          # HTTP→HTTPS自动跳转
    
    # 跨域CORS配置
    nginx.ingress.kubernetes.io/enable-cors: "true"
    nginx.ingress.kubernetes.io/cors-allow-origin: "https://example.com"
    nginx.ingress.kubernetes.io/cors-allow-methods: "GET, POST, PUT, DELETE, PATCH"
    nginx.ingress.kubernetes.io/cors-allow-headers: "Authorization, Content-Type, X-Request-ID"
    nginx.ingress.kubernetes.io/cors-allow-credentials: "true"
    
    # 请求体限制（文件上传）
    nginx.ingress.kubernetes.io/proxy-body-size: "20m"
    
    # 连接超时
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "10"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "60"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "60"
    
    # 速率限制
    nginx.ingress.kubernetes.io/limit-rps: "100"
    nginx.ingress.kubernetes.io/limit-burst: "200"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - api.example.com
    secretName: api-tls-cert              # cert-manager自动创建
  - hosts:
    - admin.example.com
    secretName: admin-tls-cert
  rules:
  # ---- API域名（带路径重写） ----
  - host: api.example.com
    http:
      paths:
      # /api/users/* → user-service:8080（去掉/api前缀）
      - path: /api/users(/|$)(.*)
        pathType: ImplementationSpecific
        backend:
          service:
            name: user-service
            port:
              number: 8080
      # /api/orders/* → order-service:8080
      - path: /api/orders(/|$)(.*)
        pathType: ImplementationSpecific
        backend:
          service:
            name: order-service
            port:
              number: 8080
      # /api/products/* → product-service:8080
      - path: /api/products(/|$)(.*)
        pathType: ImplementationSpecific
        backend:
          service:
            name: product-service
            port:
              number: 8080

  # ---- 管理后台域名 ----
  - host: admin.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: admin-web
            port:
              number: 80
      # 后台API → audit-service
      - path: /api/audit
        pathType: Prefix
        backend:
          service:
            name: audit-service
            port:
              number: 8081
```

### 8.3 验证Ingress配置

```bash
# 查看Ingress状态
kubectl get ingress api-gateway
# NAME          CLASS   HOSTS                                    ADDRESS          PORTS     AGE
# api-gateway   nginx   api.example.com,admin.example.com        192.168.1.200    80, 443   5m

# 查看Ingress详情
kubectl describe ingress api-gateway
# Events:
#   Type    Reason  Age   From                      Message
#   ----    ------  ----  ----                      -------
#   Normal  Sync    2m    nginx-ingress-controller  Scheduled for sync
#   Normal  Sync    2m    nginx-ingress-controller  Successfully synced

# 测试路由
curl -H "Host: api.example.com" http://<INGRESS_IP>/api/users
curl -H "Host: admin.example.com" http://<INGRESS_IP>/api/audit

# 验证HTTPS自动跳转
curl -L http://api.example.com/users  # → 自动302到https://api.example.com/users

# 查看认证的证书
echo | openssl s_client -connect api.example.com:443 -servername api.example.com 2>/dev/null | openssl x509 -noout -dates
```

### 8.4 常见问题排查

| 问题 | 原因 | 解决 |
|------|------|------|
| `503 Service Unavailable` | 后端Service的Pod不存在或未就绪 | `kubectl get endpoints <service>`检查端点 |
| `404 Not Found` | 路由不匹配或Ingress Controller未同步 | `kubectl describe ingress`检查Events |
| `502 Bad Gateway` | 后端响应超时或拒绝连接 | 检查Pod运行状态和`proxy-read-timeout`配置 |
| HTTP→HTTPS不跳转 | `ssl-redirect`被设为false | 检查Ingress Annotations |
| 证书未生效 | cert-manager未完成签发 | `kubectl describe certificate api-tls-cert`查看状态 |
| CORS跨域错误 | Ingress未开启CORS或Origin不匹配 | 检查`cors-allow-origin`配置 |

---

> 🎯 **Ingress核心要点总结**：Ingress是K8s七层入口网关，解决外部访问和路由分发问题。Nginx Ingress Controller是生产首选。用Annotations控制流量行为（路径重写、CORS、速率限制），用cert-manager实现证书自动管理。金丝雀发布通过canary-weight实现平滑升级。Gateway API是下一代标准，提供更强大的路由能力。**没有Ingress Controller的Ingress只是一个空配置**——必须先安装Ingress Controller！
