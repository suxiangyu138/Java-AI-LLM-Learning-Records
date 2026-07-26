# 05-ConfigMap与Secret
> 🎯 配置与代码分离是云原生的第一原则 — 掌握ConfigMap（非敏感配置）和Secret（敏感配置）的创建、挂载、热更新，让SpringBoot在K8s中灵活切换环境

---

## 目录
1. [ConfigMap与Secret概述](#1-configmap与secret概述)
2. [ConfigMap：非敏感配置](#2-configmap非敏感配置)
3. [Secret：敏感配置](#3-secret敏感配置)
4. [配置挂载到Pod的三种方式](#4-配置挂载到pod的三种方式)
5. [SpringBoot + K8s配置最佳实践](#5-springboot--k8s配置最佳实践)
6. [配置热更新与滚动重启](#6-配置热更新与滚动重启)

---

## 1. ConfigMap与Secret概述

| 维度 | ConfigMap | Secret |
|------|-----------|--------|
| 用途 | 非敏感配置 | 敏感信息（密码/Token/证书） |
| 存储方式 | 明文存储 | Base64编码（⚠️ 非加密！） |
| 大小限制 | 1MB | 1MB |
| 典型内容 | application.yml、nginx.conf | DB密码、API Key、TLS证书 |

> ⚠️ Secret只是Base64编码，**不是加密**！`echo "root123" | base64` ≈ `cm9vdDEyMwo=`，任何人都能解码。生产环境需配合K8s加密插件或Vault。

---

## 2. ConfigMap：非敏感配置

### 2.1 创建ConfigMap

```bash
# 方式1：从文件创建
kubectl create configmap app-config --from-file=application.yml

# 方式2：从字面量创建
kubectl create configmap app-config \
  --from-literal=server.port=8080 \
  --from-literal=log.level=DEBUG

# 方式3：从YAML创建
kubectl apply -f configmap.yaml
```

```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: user-service-config
data:
  application.yml: |
    server:
      port: 8080
    spring:
      datasource:
        url: jdbc:mysql://mysql-service:3306/user_db
      redis:
        host: redis-service
```

### 2.2 查看ConfigMap

```bash
kubectl get configmap
kubectl describe configmap user-service-config
kubectl get configmap user-service-config -o yaml
```

---

## 3. Secret：敏感配置

### 3.1 Secret类型

| 类型 | 用途 | 字段 |
|------|------|------|
| **Opaque** | 通用键值对 | 自定义 `data` |
| **kubernetes.io/tls** | TLS证书 | `tls.crt` + `tls.key` |
| **kubernetes.io/dockerconfigjson** | 私有镜像仓库认证 | `.dockerconfigjson` |
| **kubernetes.io/basic-auth** | 基础认证 | `username` + `password` |

### 3.2 创建Secret

```bash
# 从字面量创建
kubectl create secret generic db-secret \
  --from-literal=username=root \
  --from-literal=password=root123

# 从文件创建
kubectl create secret tls my-tls \
  --cert=cert.pem --key=key.pem

# Docker仓库认证
kubectl create secret docker-registry my-registry \
  --docker-server=registry.example.com \
  --docker-username=admin \
  --docker-password=pass123
```

```yaml
# secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: db-secret
type: Opaque
stringData:                  # ← 明文书写，K8s自动Base64编码
  username: root
  password: root123
---
# 等价于手动Base64编码的写法：
# data:
#   username: cm9vdA==
#   password: cm9vdDEyMw==
```

---

## 4. 配置挂载到Pod的三种方式

### 4.1 环境变量注入

```yaml
spec:
  containers:
    - name: user-service
      image: user-service:1.0
      env:
        # 方式A：ConfigMap的单个key → 环境变量
        - name: SERVER_PORT
          valueFrom:
            configMapKeyRef:
              name: user-service-config
              key: server.port
        # 方式B：Secret的单个key → 环境变量
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password
      envFrom:
        # 方式C：ConfigMap全部key → 环境变量
        - configMapRef:
            name: user-service-config
        # 方式D：Secret全部key → 环境变量
        - secretRef:
            name: db-secret
```

### 4.2 Volume挂载（文件方式）

```yaml
spec:
  containers:
    - name: user-service
      image: user-service:1.0
      volumeMounts:
        - name: config
          mountPath: /app/config       # 挂载ConfigMap到此目录
        - name: secret
          mountPath: /app/secrets      # 挂载Secret到此目录
          readOnly: true
  volumes:
    - name: config
      configMap:
        name: user-service-config      # 每个key变成一个文件
    - name: secret
      secret:
        secretName: db-secret
```

### 4.3 三种方式对比

| 方式 | 适用 | 热更新 | 注意 |
|------|------|:---:|------|
| 环境变量 | 简单KV配置 | ❌ 需重启Pod | 无法更新已运行的进程 |
| Volume挂载 | 配置文件 | ✅ 自动同步(~1min) | SpringBoot需要`@RefreshScope`或`spring.cloud.kubernetes.reload` |
| SubPath挂载 | 替换已有文件 | ❌ | 不覆盖整个目录 |

---

## 5. SpringBoot + K8s配置最佳实践

### 5.1 配置分层

```text
├── application.yml          → 打包在JAR内（默认配置）
├── ConfigMap挂载            → K8s环境特定配置（数据库地址等）
└── Secret挂载              → 敏感信息（密码等）

优先级：Secret/ConfigMap挂载 > JAR内application.yml
```

```yaml
# Deployment中同时挂载ConfigMap和Secret
spec:
  containers:
    - name: user-service
      image: user-service:1.0
      envFrom:
        - configMapRef:
            name: user-service-config
        - secretRef:
            name: db-secret
```

### 5.2 Spring Cloud Kubernetes

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-kubernetes-config</artifactId>
</dependency>
```

```yaml
# bootstrap.yml — Spring Cloud K8s自动读取ConfigMap
spring:
  cloud:
    kubernetes:
      config:
        enabled: true
        name: user-service-config    # ConfigMap名称
        namespace: default
```

---

## 6. 配置热更新与滚动重启

### 6.1 配置更新策略

```bash
# 更新ConfigMap
kubectl edit configmap user-service-config

# Volume挂载 → 约1分钟后Pod内文件自动更新（kubelet sync周期）
# 环境变量 → 不更新，需重启Pod

# 触发滚动重启（推荐）
kubectl rollout restart deployment/user-service
```

### 6.2 Reloader自动重启

```yaml
# 使用Stakater Reloader，ConfigMap/Secret变更自动触发滚动重启
# 安装：kubectl apply -f https://.../reloader.yaml

# Deployment添加注解：
metadata:
  annotations:
    reloader.stakater.com/auto: "true"
```

---

> 🎯 **K8s配置管理核心原则**：凡是不敏感的→ConfigMap；凡是密码/Token/证书→Secret；凡是运行环境特有的→K8s管理；凡是通用的→JAR内默认值。
