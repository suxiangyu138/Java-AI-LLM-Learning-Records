# 09 - 自动化运维与 IaC

> 🎯 手动运维 = 人肉背锅。Terraform 管基础设施、Ansible 管配置、GitOps 管部署 — 一切皆代码，一切皆自动化

## 1. Terraform 基础设施即代码

```hcl
# GPU 集群 IaC
resource "google_container_node_pool" "gpu_pool" {
  name       = "gpu-a100-pool"
  cluster    = google_container_cluster.primary.name
  node_count = 2

  node_config {
    machine_type = "a2-highgpu-1g"  # 1×A100
    
    guest_accelerator {
      type  = "nvidia-tesla-a100"
      count = 1
    }
    
    labels = { "gpu-type" = "a100" }
    
    taint {
      key    = "nvidia.com/gpu"
      value  = "true"
      effect = "NO_SCHEDULE"  # 只有请求 GPU 的 Pod 调度
    }
  }
  
  autoscaling {
    min_node_count = 1
    max_node_count = 5
  }
}
```

## 2. GitOps 部署

```yaml
# ArgoCD Application — GitOps 模型
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: llm-inference
spec:
  source:
    repoURL: https://github.com/org/llm-deploy
    path: k8s/production
    targetRevision: main
  destination:
    server: https://kubernetes.default.svc
    namespace: llm-prod
  syncPolicy:
    automated:
      prune: true      # 自动删除 Git 中移除的资源
      selfHeal: true   # 自动修复漂移
```

## 3. 自动扩缩策略

```python
# 基于 GPU 利用率的智能扩缩
class GPUAutoscaler:
    def __init__(self, k8s_client, deployment_name):
        self.k8s = k8s_client
        self.deployment = deployment_name
    
    def evaluate_and_scale(self):
        metrics = self._get_gpu_metrics()
        
        # GPU 利用率 > 70% 持续 5 分钟 → 扩容
        if metrics["avg_util"] > 70 and metrics["duration"] > 300:
            new_replicas = min(self.current_replicas + 2, self.max_replicas)
            self._scale(new_replicas)
        
        # GPU 利用率 < 30% 持续 20 分钟 → 缩容
        elif metrics["avg_util"] < 30 and metrics["duration"] > 1200:
            new_replicas = max(self.current_replicas - 1, self.min_replicas)
            self._scale(new_replicas)
```

## 4. 运维自动化 CheckList

```text
□ 基础设施：Terraform 管理（可复现、可审计）
□ 配置管理：Helm Chart + values 文件（Git 版本化）
□ 部署：GitOps (ArgoCD/FluxCD) → Git 是唯一的真实来源
□ 扩缩：KEDA (事件驱动) + GPU 指标自定义 HPA
□ 证书：cert-manager 自动管理 TLS
□ 密钥：External Secrets Operator (从 Vault/AWS SM 同步)
□ 备份：Velero 定时备份 K8s 资源 + 持久卷
□ 日志：Fluentd → Loki/ES (自动采集)
```
