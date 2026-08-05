# 09-SpringBoot应用K8s部署实战
> 🎯 从Dockerfile到生产运行 — 完整的SpringBoot应用K8s部署流程：Dockerfile→Build→Push→Deployment→Service→Ingress→投产

---

## 目录
1. [部署全景流程](#1-部署全景流程)
2. [Dockerfile：为K8s优化](#2-dockerfile为k8s优化)
3. [完整K8s资源清单](#3-完整k8s资源清单)
4. [一键部署与验证](#4-一键部署与验证)
5. [JVM容器适配](#5-jvm容器适配)
6. [CI/CD流水线集成](#6-cicd流水线集成)

---

## 1. 部署全景流程

```text
开发 → 提交代码 → CI自动构建镜像 → 推送到Registry
  → kubectl apply → K8s创建Deployment/Service/Ingress
  → Pod就绪 → 用户通过Ingress访问

六步部署：
  ① Dockerfile → ② Build Image → ③ Push to Registry
  → ④ kubectl apply → ⑤ 等待Pod就绪 → ⑥ 验证
```

---

## 2. Dockerfile：为K8s优化

```dockerfile
# 多阶段构建：Maven编译 + JRE运行
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline      # 缓存依赖（加速后续构建）
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 创建非root用户（K8s安全最佳实践）
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /build/target/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser
EXPOSE 8080

# JVM参数适配容器环境
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport",      # 让JVM感知容器内存限制
    "-XX:MaxRAMPercentage=75.0",     # JVM堆最大不超过容器内存75%
    "-XX:+UseG1GC",                  # 使用G1GC
    "-jar", "app.jar"]
```

---

## 3. 完整K8s资源清单

### 3.1 ConfigMap

```yaml
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: user-service-config
data:
  application.yml: |
    server:
      port: 8080
      shutdown: graceful               # 优雅停机
    spring:
      lifecycle:
        timeout-per-shutdown-phase: 30s # 优雅停机超时
      datasource:
        url: jdbc:mysql://mysql-service:3306/user_db
        hikari:
          maximum-pool-size: 20
          connection-timeout: 30000
    management:
      endpoints:
        web:
          exposure:
            include: health,info
      endpoint:
        health:
          probes:
            enabled: true               # K8s探针端点
```

### 3.2 Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: db-secret
type: Opaque
stringData:
  username: root
  password: ${DB_PASSWORD}              # 实际值通过CI注入
```

### 3.3 Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  labels:
    app: user-service
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0                # 零停机：始终至少3个Pod可用
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      terminationGracePeriodSeconds: 45  # 优雅停机等待时间
      containers:
        - name: user-service
          image: registry.example.com/user-service:1.0.0
          ports:
            - containerPort: 8080
              protocol: TCP
          envFrom:
            - configMapRef:
                name: user-service-config
            - secretRef:
                name: db-secret
          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 15
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
```

### 3.4 Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: user-service
spec:
  selector:
    app: user-service
  ports:
    - port: 8080
      targetPort: 8080
  type: ClusterIP
```

### 3.5 Ingress

```yaml
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
```

---

## 4. 一键部署与验证

```bash
# 1. 应用所有资源
kubectl apply -f k8s/

# 2. 查看部署状态
kubectl get all -l app=user-service
kubectl rollout status deployment/user-service

# 3. 查看Pod日志
kubectl logs -f deployment/user-service

# 4. 端口转发本地验证（不用Ingress也能测）
kubectl port-forward deployment/user-service 8080:8080
curl http://localhost:8080/actuator/health

# 5. 查看Ingress分配的IP
kubectl get ingress api-ingress
# 修改本机hosts：<INGRESS_IP> api.example.com
curl http://api.example.com/api/users/1

# 6. 扩容
kubectl scale deployment/user-service --replicas=5

# 7. 回滚
kubectl rollout undo deployment/user-service
kubectl rollout undo deployment/user-service --to-revision=2
```

---

## 5. JVM容器适配

| 参数 | 作用 | 备注 |
|------|------|------|
| `-XX:+UseContainerSupport` | 让JVM感知容器cgroup内存限制 | Java 8u191+, 10+ 默认开启 |
| `-XX:MaxRAMPercentage=75.0` | 限制JVM堆为容器内存的75% | 替代传统`-Xmx`（适应弹性伸缩） |
| `-XX:InitialRAMPercentage=50.0` | 初始堆为容器内存的50% | 启动时少占内存 |
| `-XX:+ExitOnOutOfMemoryError` | OOM时退出（K8s会重启） | K8s能检测到并重启 |
| `-XX:+HeapDumpOnOutOfMemoryError` | OOM时dump堆 | 用于事后分析 |

> ⚠️ 不要在K8s中同时设置`-Xmx`和`limits.memory`！用`MaxRAMPercentage`让JVM自动适配，否则可能出现Pod的OOMKilled。

---

## 6. CI/CD流水线集成

```yaml
# .github/workflows/deploy.yml
name: Build & Deploy
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Build & Push Docker Image
        run: |
          docker build -t registry.example.com/user-service:${{ github.sha }} .
          docker push registry.example.com/user-service:${{ github.sha }}
      
      - name: Deploy to K8s
        run: |
          kubectl set image deployment/user-service \
            user-service=registry.example.com/user-service:${{ github.sha }}
          kubectl rollout status deployment/user-service
      
      - name: Rollback on Failure
        if: failure()
        run: kubectl rollout undo deployment/user-service
```

---

> 🎯 **K8s部署的成熟标志**：不是能跑起来，而是健康检查通过、优雅停机不丢请求、资源限制合理、CI/CD全自动。
