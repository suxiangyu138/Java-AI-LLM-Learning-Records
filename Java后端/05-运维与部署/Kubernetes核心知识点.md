# Kubernetes 核心知识点

## 概念定位

Kubernetes (K8s) 是容器编排平台。它告诉你"这个应用要跑 3 个副本，占用 2 核 4G 内存，对外暴露 8080 端口"，然后 K8s 自动调度、重启、扩缩容。

## 核心组件

```
Master 节点（控制平面）：
  API Server  — 所有操作入口，REST API
  Scheduler   — 决定 Pod 部署到哪个 Worker
  Controller Manager — 维护期望状态（副本数等）
  etcd        — 分布式 KV 存储，存所有集群状态

Worker 节点（数据平面）：
  kubelet     — 管理本机 Pod 的生命周期
  kube-proxy  — 网络代理，实现 Service 的负载均衡
  Container Runtime — 容器运行时（Docker / containerd）
```

## 核心资源对象

### Pod
- 最小调度单位，包含一个或多个容器
- 同一 Pod 内的容器共享网络和存储
- Pod 是临时的，随时可能被替换

### Service
- 一组 Pod 的抽象入口，提供稳定 IP 和 DNS
- 四种类型：ClusterIP（集群内）、NodePort（节点端口）、LoadBalancer（云负载均衡）、ExternalName

### Deployment
- 声明式管理 Pod 副本数、滚动更新、回滚
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 3                     # 3个副本
  selector:
    matchLabels:
      app: order-service
  template:                       # Pod 模板
    metadata:
      labels:
        app: order-service
    spec:
      containers:
      - name: order-service
        image: order-service:1.0
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1"
```

### ConfigMap & Secret
- ConfigMap：非敏感配置（数据库 URL、日志级别）
- Secret：敏感配置（密码、Token），Base64 编码存储

### Ingress
- 七层负载均衡，HTTP/HTTPS 路由
- 将外部请求路由到内部 Service

## Spring Boot 上 K8s

**Dockerfile：**
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

**K8s 部署命令：**
```bash
kubectl apply -f deployment.yaml        # 部署
kubectl get pods                        # 查看 Pod 状态
kubectl logs -f order-service-7d5f-xxx  # 查看日志
kubectl scale deployment order-service --replicas=5  # 扩容
kubectl rollout undo deployment/order-service          # 回滚
```

## 常用命令速查

| 命令 | 用途 |
|------|------|
| `kubectl get pods` | 查看 Pod |
| `kubectl describe pod <name>` | Pod 详情 |
| `kubectl logs -f <pod>` | 实时日志 |
| `kubectl exec -it <pod> -- /bin/bash` | 进入容器 |
| `kubectl apply -f file.yaml` | 应用配置 |
| `kubectl delete -f file.yaml` | 删除资源 |
| `kubectl get svc` | 查看 Service |
| `kubectl get ingress` | 查看路由 |

## Java 后端需要掌握的层次

作为 Java 后端开发者，你不需要成为 K8s 专家。需要掌握的是：

1. **能看懂 YAML**：Deployment、Service、ConfigMap 的基本写法
2. **能部署自己的服务**：写好 Dockerfile，配好 Deployment YAML，`kubectl apply`
3. **能排查问题**：`kubectl logs`、`kubectl describe`、`kubectl exec` 进入容器
4. **会用 ConfigMap 管理配置**：替代 application.yml 里的环境相关配置
5. **健康检查**：配置 Spring Boot Actuator 的 readiness/liveness 探针

```yaml
livenessProbe:     # 存活探针：容器是否还活着
  httpGet:
    path: /actuator/health/liveness
    port: 8080
readinessProbe:    # 就绪探针：服务是否可以接收流量
  httpGet:
    path: /actuator/health/readiness
    port: 8080
```
