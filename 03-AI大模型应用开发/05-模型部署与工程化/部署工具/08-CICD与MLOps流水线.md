# 08 - CI/CD 与 MLOps 流水线

> 🎯 MLOps = ML + DevOps — 模型版本管理、自动化测试、持续部署，让 ML 从"手工作坊"走向"工业流水线"

### MLOps 核心组件

```text
┌─────────────────────────────────────────────┐
│  数据：DVC (数据版本)    Git LFS (大文件)    │
│  模型：Model Registry (MLflow/HuggingFace)  │
│  训练：Experiment Tracking (W&B/MLflow)     │
│  测试：准确率/延迟回归测试                    │
│  部署：Docker Registry → K8s/CD             │
│  监控：Prometheus + 模型漂移检测             │
└─────────────────────────────────────────────┘
```

### 模型版本管理

```python
# MLflow 模型管理
import mlflow

with mlflow.start_run():
    mlflow.log_param("lr", 2e-4)
    mlflow.log_metric("val_loss", 0.15)
    mlflow.pytorch.log_model(model, "model")
    mlflow.register_model("runs:/.../model", "Qwen2-Finetuned")
```

### GitHub Actions CI/CD

```yaml
name: Deploy Model
on:
  push:
    paths: ['model/**']
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build & Push
        run: |
          docker build -t registry/my-model:${{ github.sha }} .
          docker push registry/my-model:${{ github.sha }}
      - name: Deploy to K8s
        run: |
          kubectl set image deployment/llm llm=registry/my-model:${{ github.sha }}
          kubectl rollout status deployment/llm
```

### 模型测试关卡

```text
部署前必过的测试门禁：
  ① 模型准确率不低于上一版本
  ② 推理延迟不高于阈值的 120%
  ③ 输出格式合规率 100%
  ④ 安全审核通过（无偏见/有害输出）
```
