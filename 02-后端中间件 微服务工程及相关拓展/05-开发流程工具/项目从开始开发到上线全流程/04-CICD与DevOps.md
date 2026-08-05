# CI/CD 与 DevOps

> 🔄 持续集成→构建→测试→部署流水线、多环境管理、灰度发布、蓝绿部署 —— 让代码安全快速到达生产

---

## 📚 目录

1. [CI/CD 流水线设计](#1-cicd-流水线设计)
2. [多环境管理](#2-多环境管理)
3. [部署策略](#3-部署策略)
4. [灰度发布](#4-灰度发布)
5. [安全与合规检查](#5-安全与合规检查)

---

## 1. CI/CD 流水线设计

### 1.1 标准流水线阶段

```text
┌─────────────────────────────────────────────────────────┐
│                    CI/CD Pipeline                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Push → Build → Test → Sonar → Package → Deploy Staging│
│                                              │          │
│                                     Staging OK?         │
│                                         ↓ Yes           │
│                              Deploy Production          │
│                                                         │
│  每个阶段失败 = Pipeline 失败 = 不能继续                  │
└─────────────────────────────────────────────────────────┘

阶段说明：
  1. Build   → mvn clean package（编译+打包）
  2. Test    → mvn test（单元测试）+ 集成测试
  3. Sonar   → SonarQube 代码质量扫描（Quality Gate）
  4. Package → docker build + docker push（打镜像推送仓库）
  5. Deploy Staging → kubectl apply 或 helm upgrade
  6. Deploy Prod    → 同上（需审批）
```

### 1.2 质量门禁（Quality Gate）

```text
代码合入 main/master 前必须通过：

  ✅ 单元测试 100% 通过
  ✅ 代码覆盖率 ≥ 80%（或新代码覆盖率 ≥ 90%）
  ✅ SonarQube：0 Bug, 0 Vulnerability, 0 Code Smell(严重)
  ✅ 安全扫描：无高危漏洞
  ✅ Code Review：至少 1 人 Approve

不通过 → Merge 按钮灰色 → 不允许合入
```

---

## 2. 多环境管理

### 2.1 环境矩阵

| 环境 | 用途 | 数据 | 部署方式 | 谁用 |
|------|------|------|---------|:--:|
| **dev** | 本地开发 | 本地 H2/Docker | 手动 | 开发者 |
| **test** | 自动化测试 | 测试数据 | CI 自动 | CI |
| **staging** | 预发布验证 | 脱敏生产数据 | CI 自动 | QA/PM |
| **production** | 生产环境 | 真实数据 | 手动审批 | 用户 |

### 2.2 配置分离

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
  redis:
    host: localhost

# application-staging.yml
spring:
  datasource:
    url: jdbc:mysql://staging-db.internal:3306/mydb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

# application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://prod-db.internal:3306/mydb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

```text
敏感信息（密码/密钥）永远不提交到代码仓库！
  → K8s Secret / Vault / 环境变量注入
```

---

## 3. 部署策略

### 3.1 四种策略对比

| 策略 | 原理 | 停机 | 资源 | 回滚 |
|------|------|:--:|:--:|:--:|
| **滚动更新** | 逐个替换 Pod | 无 | 与原来相同 | 慢（需逐个回退） |
| **蓝绿部署** | 两套环境切换 | 无 | 2x | 秒级（切流量） |
| **金丝雀/灰度** | 逐步放量 | 无 | 略多 | 秒级 |
| **重建部署** | 先停旧再启新 | 有 | 与原来相同 | 慢 |

### 3.2 蓝绿部署

```text
蓝绿部署流程：

  Blue 环境（v1.2）—— 100% 流量
  Green 环境（v1.3）—— 部署完成，0% 流量

  ↓ 验证 Green 环境健康检查通过

  切流量：Green 100%，Blue 0%

  ↓ 观察 30 分钟无问题

  回收 Blue 环境

  回滚：如果 v1.3 有问题 → 瞬间切回 Blue
```

### 3.3 K8s 滚动更新配置

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1          # 最多多出 1 个 Pod（峰值 4 个）
      maxUnavailable: 0    # 不允许不可用 Pod（保证零停机）
  template:
    spec:
      containers:
      - name: app
        image: registry.example.com/app:1.3.0
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
```

---

## 4. 灰度发布

### 4.1 按比例分流

```yaml
# Istio VirtualService 灰度配置
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
spec:
  hosts:
  - api.example.com
  http:
  - match:
    - headers:
        x-version:
          exact: "beta"        # 带特定 header → 新版本
    route:
    - destination:
        host: user-service
        subset: v2
  - route:
    - destination:
        host: user-service
        subset: v1
        weight: 95            # 95% → 旧版本
    - destination:
        host: user-service
        subset: v2
        weight: 5             # 5% → 新版本（灰度）
```

### 4.2 灰度发布流程

```text
灰度发布 SOP：

  Step 1: 部署新版本到 1 个 Pod（0% 流量）
  Step 2: 内部测试（通过特定 Header 路由到新版本）
  Step 3: 灰度 5%（观察 30 分钟）
  Step 4: 灰度 20%（观察 30 分钟）
  Step 5: 灰度 50%（观察 1 小时）
  Step 6: 全量 100%
  Step 7: 下线旧版本

  任何阶段：
    → 错误率 > 0.1% 或 P99 恶化 → 立即回滚到旧版本
```

---

## 5. 安全与合规检查

```text
CI/CD 中嵌入安全检查：

  □ 依赖漏洞扫描 → Snyk / OWASP Dependency-Check
  □ 代码安全扫描 → SonarQube Security / Checkmarx
  □ 镜像安全扫描 → Trivy / Clair
  □ Secret 检测   → git-secrets / truffleHog
  □ IaC 安全      → Checkov / tfsec

  ⚠️ 任何高危漏洞 → 构建失败 → 不允许部署
```

---

> 🎯 **核心要点**：CI 是自动化构建+测试+质量门禁，CD 是自动化部署+发布策略。**灰度发布 + 可观测 + 快速回滚** = 安心上线。

---

*创建于：2026年7月*
