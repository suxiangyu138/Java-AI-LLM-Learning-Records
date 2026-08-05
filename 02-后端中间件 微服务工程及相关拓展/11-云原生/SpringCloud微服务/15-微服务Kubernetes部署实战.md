# 15 - 微服务 Kubernetes 部署实战

> 🎯 微服务的家永远是 K8s — 从 Deployment 声明式部署到 Service 服务暴露、从 ConfigMap/Secret 配置注入到 Ingress 统一入口，这套 YAML 模板覆盖了微服务上 K8s 的全部姿势

---

## 目录

1. [K8s 部署 vs Docker Compose](#1-k8s-部署-vs-docker-compose)
2. [Spring Boot 微服务 Deployment](#2-spring-boot-微服务-deployment)
3. [Service 与 Ingress](#3-service-与-ingress)
4. [ConfigMap 与 Secret](#4-configmap-与-secret)
5. [健康检查与优雅下线](#5-健康检查与优雅下线)
6. [完整部署流程](#6-完整部署流程)

---

## 1. K8s 部署 vs Docker Compose

| 维度 | Docker Compose | Kubernetes |
|------|:---:|:---:|
| 适用 | 单机开发测试 | 生产集群 |
| 扩缩容 | 手动 | 自动 HPA |
| 服务发现 | Docker DNS | 内置 Service + CoreDNS |
| 滚动更新 | ❌ 手动 | ✅ RollingUpdate 自动 |
| 自愈 | ❌ | ✅ 自动重启/迁移 |
| 配置管理 | 环境变量/文件 | ConfigMap + Secret |

---

## 2. Spring Boot 微服务 Deployment

```yaml
# user-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  labels:
    app: user-service
spec:
  replicas: 3                          # 3 个副本
  selector:
    matchLabels:
      app: user-service
  strategy:
    type: RollingUpdate                # 滚动更新
    rollingUpdate:
      maxSurge: 1                      # 最多多出 1 个 Pod
      maxUnavailable: 0                # 不允许有不可用的 Pod
  template:
    metadata:
      labels:
        app: user-service
        version: v1
    spec:
      serviceAccountName: user-service-sa
      terminationGracePeriodSeconds: 60
      containers:
        - name: user-service
          image: registry.example.com/user-service:1.0.0
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
              protocol: TCP
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: JAVA_OPTS
              value: "-Xms512m -Xmx1g -XX:+UseG1GC"
          envFrom:
            - configMapRef:
                name: user-service-config
            - secretRef:
                name: user-service-secret
          resources:                    # ⭐ 资源限制必配
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          livenessProbe:               # 存活探针
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:              # 就绪探针
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
          volumeMounts:
            - name: logs
              mountPath: /opt/logs
      volumes:
        - name: logs
          emptyDir: {}
```

---

## 3. Service 与 Ingress

```yaml
# ═══ Service：内部暴露 ═══
apiVersion: v1
kind: Service
metadata:
  name: user-service
spec:
  type: ClusterIP                      # 仅集群内访问
  selector:
    app: user-service
  ports:
    - name: http
      port: 8080
      targetPort: 8080
      protocol: TCP

---
# ═══ Ingress：对外暴露 ═══
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: api-ingress
spec:
  ingressClassName: nginx
  rules:
    - host: api.example.com
      http:
        paths:
          - path: /api/users
            pathType: Prefix
            backend:
              service:
                name: user-service
                port:
                  number: 8080
          - path: /api/orders
            pathType: Prefix
            backend:
              service:
                name: order-service
                port:
                  number: 8080
  tls:
    - hosts:
        - api.example.com
      secretName: api-tls-secret        # TLS 证书
```

### K8s 服务调用

```java
// 微服务间调用：使用 Service 名作为域名
// user-service 的 Service 名 = user-service
// → 命名空间内直接访问 http://user-service:8080

@FeignClient(name = "user-service", url = "http://user-service:8080")
// 或通过 Spring Cloud Kubernetes 自动发现（无需 url）
@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/users/{id}")
    User getUser(@PathVariable Long id);
}
```

---

## 4. ConfigMap 与 Secret

```yaml
# ═══ ConfigMap（非敏感配置） ═══
apiVersion: v1
kind: ConfigMap
metadata:
  name: user-service-config
data:
  application-prod.yml: |
    spring:
      cloud:
        nacos:
          discovery:
            server-addr: nacos.default.svc.cluster.local:8848
            namespace: prod
      datasource:
        hikari:
          maximum-pool-size: 50

---
# ═══ Secret（敏感信息） ═══
apiVersion: v1
kind: Secret
metadata:
  name: user-service-secret
type: Opaque
stringData:
  SPRING_DATASOURCE_USERNAME: admin
  SPRING_DATASOURCE_PASSWORD: P@ssw0rd2024
```

---

## 5. 健康检查与优雅下线

### Spring Boot 探针端点

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true        # ⭐ 开启 liveness/readiness 探针
      show-details: always
```

```text
Liveness Probe（存活探针）：
  /actuator/health/liveness
  → 失败 → K8s 重启 Pod
  → 用于检测死锁/OOM 等不可恢复的故障

Readiness Probe（就绪探针）：
  /actuator/health/readiness
  → 失败 → K8s 将 Pod 从 Service 摘除（不接收流量）
  → 用于启动预热、依赖检查（DB/MQ 是否连通）
```

### 优雅下线脚本

```yaml
spec:
  terminationGracePeriodSeconds: 60    # 最多等 60s
  containers:
    lifecycle:
      preStop:
        exec:
          command:
            - /bin/sh
            - -c
            - |
              # 1. 从注册中心注销
              curl -X DELETE http://localhost:8080/actuator/service-registry?status=DOWN
              # 2. 等待流量排干
              sleep 20
```

---

## 6. 完整部署流程

```bash
# 1. 创建命名空间
kubectl create namespace prod

# 2. 部署 ConfigMap 和 Secret
kubectl apply -f k8s/configmap.yaml -n prod
kubectl apply -f k8s/secret.yaml -n prod

# 3. 部署中间件（Nacos/MySQL/Redis — 或使用 Helm）
helm install nacos nacos/nacos -n prod

# 4. 部署微服务
kubectl apply -f k8s/user-service.yaml -n prod
kubectl apply -f k8s/order-service.yaml -n prod
kubectl apply -f k8s/product-service.yaml -n prod

# 5. 部署 Gateway + Ingress
kubectl apply -f k8s/gateway.yaml -n prod
kubectl apply -f k8s/ingress.yaml -n prod

# 6. 验证
kubectl get all -n prod
kubectl logs -f deployment/user-service -n prod
curl http://api.example.com/api/users/1

# 7. 滚动更新
kubectl set image deployment/user-service \
  user-service=registry.example.com/user-service:1.1.0 -n prod
kubectl rollout status deployment/user-service -n prod

# 8. 回滚
kubectl rollout undo deployment/user-service -n prod
```

| 常用运维命令 | 说明 |
|-------------|------|
| `kubectl rollout restart deploy/user-service` | 重启服务 |
| `kubectl scale deploy/user-service --replicas=5` | 扩容 |
| `kubectl top pods -n prod` | 资源使用 |
| `kubectl logs -f deploy/user-service --tail=50` | 实时日志 |

> 🎯 **K8s 微服务三板斧**：Deployment 管理 Pod + Service 暴露端口 + Ingress 对外路由。ConfigMap 管配置、Secret 管密码、探针管健康、RollingUpdate 管发布。
