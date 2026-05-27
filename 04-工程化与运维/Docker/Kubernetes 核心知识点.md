# Kubernetes (k8s) 核心知识点

## 一、概述

Kubernetes（k8s）是 Google 开源的容器编排平台，用于自动化部署、扩展和管理容器化应用。它是云原生架构的核心基础设施，2026 年已成为生产环境大规模微服务和 AI 应用的标准解决方案。

**核心定位：** 容器编排的事实标准，生产环境微服务 + AI 推理服务弹性伸缩的基石。

**官网：** https://kubernetes.io

## 二、核心概念

### 2.1 架构总览

```
┌─────────────────────────────────────────────────────┐
│                   Control Plane（控制平面）           │
│  ┌──────────┬──────────┬──────────┬──────────────┐  │
│  │ API      │ etcd     │Scheduler │Controller    │  │
│  │ Server   │ (状态存储)│(调度器)  │ Manager      │  │
│  └──────────┴──────────┴──────────┴──────────────┘  │
├─────────────────────────────────────────────────────┤
│                  Worker Nodes（工作节点）             │
│  ┌──────────────────┐  ┌──────────────────┐         │
│  │ kubelet + kube-proxy│  │ kubelet + kube-proxy│    │
│  │ ┌──────┬──────┐   │  │ ┌──────┬──────┐   │         │
│  │ │ Pod  │ Pod  │   │  │ │ Pod  │ Pod  │   │         │
│  │ └──────┴──────┘   │  │ └──────┴──────┘   │         │
│  └──────────────────┘  └──────────────────┘         │
└─────────────────────────────────────────────────────┘
```

### 2.2 核心对象

| 对象 | 说明 |
|------|------|
| **Pod** | 最小部署单元，包含一个或多个容器（通常 1:1） |
| **Service** | 为 Pod 提供稳定的网络入口和负载均衡 |
| **Deployment** | 声明式管理 Pod 的副本数、滚动更新、回滚 |
| **ConfigMap** | 非敏感配置（环境变量、配置文件） |
| **Secret** | 敏感信息（密码、Token、TLS 证书） |
| **Ingress** | HTTP/HTTPS 路由规则，对外暴露 Service |
| **PersistentVolume** | 持久化存储抽象 |
| **StatefulSet** | 有状态应用（数据库、消息队列）管理 |
| **Job / CronJob** | 一次性/定时任务 |

## 三、核心功能

### 3.1 弹性伸缩

```yaml
# Horizontal Pod Autoscaler (HPA)
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: my-app-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: my-app
  minReplicas: 2
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

### 3.2 滚动更新与回滚

```bash
# 更新镜像
kubectl set image deployment/my-app app=my-app:v2.0

# 查看更新状态
kubectl rollout status deployment/my-app

# 回滚到上一版本
kubectl rollout undo deployment/my-app

# 回滚到指定版本
kubectl rollout undo deployment/my-app --to-revision=3
```

### 3.3 服务发现与负载均衡

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
  type: ClusterIP  # 集群内部访问
---
apiVersion: v1
kind: Service
metadata:
  name: user-service-external
spec:
  type: LoadBalancer  # 外部负载均衡器暴露
  selector:
    app: user-service
  ports:
    - port: 80
      targetPort: 8080
```

### 3.4 配置管理

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  application.yml: |
    server:
      port: 8080
    spring:
      datasource:
        url: jdbc:mysql://mysql-service:3306/mydb
---
apiVersion: v1
kind: Secret
metadata:
  name: db-secret
type: Opaque
data:
  username: YWRtaW4=     # base64("admin")
  password: cGFzc3dvcmQ= # base64("password")
```

## 四、Java Spring Boot 部署示例

### 4.1 Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-boot-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: spring-boot-app
  template:
    metadata:
      labels:
        app: spring-boot-app
    spec:
      containers:
        - name: app
          image: registry.example.com/spring-app:1.0.0
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "k8s"
            - name: DB_USERNAME
              valueFrom:
                secretKeyRef:
                  name: db-secret
                  key: username
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
```

### 4.2 AI 推理服务部署

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: llm-inference
spec:
  replicas: 2
  template:
    spec:
      containers:
        - name: ollama
          image: ollama/ollama:latest
          resources:
            limits:
              nvidia.com/gpu: 1  # GPU 资源
          volumeMounts:
            - name: models
              mountPath: /root/.ollama
      volumes:
        - name: models
          persistentVolumeClaim:
            claimName: ollama-models-pvc
```

## 五、常用命令速查

```bash
# 集群信息
kubectl cluster-info
kubectl get nodes

# 工作负载
kubectl get pods -o wide
kubectl describe pod <pod-name>
kubectl logs -f <pod-name>
kubectl exec -it <pod-name> -- /bin/sh

# 部署管理
kubectl apply -f deployment.yaml
kubectl delete -f deployment.yaml
kubectl scale deployment/my-app --replicas=5

# 调试
kubectl get events --sort-by=.metadata.creationTimestamp
kubectl port-forward pod/<pod-name> 8080:8080
kubectl top pods  # 资源使用排行
```

## 六、k8s vs Docker Compose

| 维度 | k8s | Docker Compose |
|------|-----|----------------|
| 适用环境 | **生产** | 开发/测试 |
| 节点数量 | 多节点集群 | **单机** |
| 弹性伸缩 | **自动** | 手动 |
| 服务发现 | **内置 DNS** | 基础（服务名） |
| 自愈能力 | **自动重启/迁移** | 重启策略 |
| 滚动更新 | **内置** | 不支持 |
| 学习曲线 | **高** | 低 |

## 七、总结

Kubernetes 是 **生产环境容器编排的标配**，核心价值在于弹性伸缩、自愈能力、滚动更新和服务发现。对 Java 开发者而言，掌握 Deployment + Service + ConfigMap 三个核心对象即可覆盖 80% 的日常部署场景。
