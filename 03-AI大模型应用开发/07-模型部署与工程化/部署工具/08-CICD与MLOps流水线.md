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

---

## 6. MLOps 的 2026 实践

**LLM 部署流水线的 2026 标准形态**：

```text
模型训练/微调 → 评估（黄金集）→ 注册（模型仓库）
→ 构建（推理镜像）→ 测试（冒烟）→ 灰度（金丝雀）
→ 上线（vLLM 服务）→ 监控（TTFT/吞吐/漂移）→ 反馈

关键点：
① 模型版本管理：模型 = 权重 + 配置 + 评测结果（一起版本化）
② 评估门禁：质量指标不达标不放行（与 RAG/Agent 评估同源）
③ 安全审核：LLM 输出安全审核（偏见/有害）纳入 CI
④ 回滚：模型回滚 = 切版本（与代码回滚同等重要）
```

**模型注册与版本管理工具**（2026）：

| 工具 | 定位 |
|------|------|
| MLflow | 实验跟踪 + 模型注册（最常用） |
| DVC | 数据/模型版本控制（Git 集成） |
| HuggingFace Hub | 模型托管（私有仓库） |
| 自建注册表 | 企业模型仓库（含评测记录） |

**LLM 部署流水线 vs 传统 CI/CD 的差异**：

| 维度 | 传统 | LLM |
|------|------|-----|
| 产物 | 代码 | **代码 + 模型权重 + 配置** |
| 测试 | 单测 | **评估（黄金集）+ 安全审核** |
| 回滚 | 代码回滚 | 模型版本回滚（+数据回滚） |
| 监控 | 错误率 | **TTFT/吞吐/漂移** |

> 🎯 **核心要点**：MLOps 2026 = "**模型即产物（权重+配置+评测一起版本化）+ 评估门禁 + 安全审核**"——LLM 部署流水线比传统 CI/CD 多"评估与安全"两道门。

---

## 7. 流水线工具速查

**LLM 部署流水线的标准工具链**（2026）：

| 环节 | 工具 | 说明 |
|------|------|------|
| 代码/模型版本 | Git + DVC | 模型权重用 DVC（大文件不进 Git） |
| 实验跟踪 | MLflow | 超参/指标/产物 |
| CI | GitHub Actions/GitLab CI | 构建 + 评估门禁 |
| 模型注册 | MLflow Model Registry | 版本 + 阶段（staging/prod） |
| 镜像 | Docker | 推理镜像 |
| 编排 | K8s + Helm | 部署与灰度 |
| 监控 | Prometheus/Grafana | LLM 指标 |

**GitHub Actions 的 LLM 评估门禁示例**：

```yaml
# .github/workflows/model-deploy.yml
jobs:
  evaluate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run model evaluation
        run: python evaluate.py --golden-set data/golden.json
      - name: Check quality gates
        run: |
          python -c "
          import json
          r = json.load(open('eval_result.json'))
          assert r['faithfulness'] >= 0.9, '质量不达标'
          assert r['task_success'] >= 0.85, '任务成功率不达标'
          "
```

**模型发布的三阶段**（MLflow Registry 实践）：

```text
Staging（评估中）→ Production（已上线）→ Archived（已退役）
流程：训练 → 注册 staging → 评估门禁通过 → 转 production
     → 灰度验证 → 全量 → 新版本覆盖
```

> 🎯 **核心要点**：MLOps 工具链 = "**Git+DVC（版本）+ MLflow（注册）+ CI 门禁（评估）+ K8s（部署）**"——**评估门禁是 LLM 流水线与传统 CI 的最大差异**（质量不达标不放行）。

---

## 8. 流水线完整示例速查

**一个 LLM 服务的端到端 CI 流水线**：

```yaml
# GitHub Actions：训练 → 评估 → 部署 一体化
name: llm-pipeline
on:
  push:
    branches: [main]
  workflow_dispatch: {}

jobs:
  train-evaluate:
    runs-on: [self-hosted, gpu]
    steps:
      - uses: actions/checkout@v4
      - name: 微调模型
        run: python train.py --config configs/finetune.yaml
      - name: 评估（黄金集门禁）
        run: python evaluate.py --golden-set data/golden.json --gate 0.9
      - name: 注册模型
        run: python register_model.py   # MLflow 注册 staging
  deploy:
    needs: train-evaluate
    runs-on: ubuntu-latest
    steps:
      - name: 构建推理镜像
        run: docker build -t llm-service:${GITHUB_SHA} .
      - name: 部署到 staging
        run: helm upgrade llm ./charts/llm --set image.tag=${GITHUB_SHA}
      - name: 冒烟测试
        run: python smoke_test.py --url ${{ secrets.STAGING_URL }}
```

**MLOps 成熟度检查清单**：

```text
□ 模型版本可追溯（权重+配置+评测结果）
□ 评估门禁自动化（黄金集）
□ 安全审核（输出/偏见）
□ 灰度发布流程（金丝雀）
□ 监控（TTFT/吞吐/漂移）
□ 回滚（版本切换）
□ 数据版本（训练数据可复现）
```

> 🎯 **核心要点**：MLOps 流水线 = "**训练 → 评估门禁 → 注册 → 构建 → 部署 → 冒烟**"六步——**评估门禁与安全审核是 LLM 特有的两道门**；成熟度清单七项是团队自检标准。

---

## 9. 常见问题速查

| 问题 | 原因 | 解法 |
|------|------|------|
| 模型文件进 Git | 太大 | DVC/Git LFS/对象存储 |
| 评估不稳定 | 黄金集太少 | 扩充样本 + 多轮取均值 |
| 灰度失败回滚慢 | 模型加载耗时 | 双服务热备（网关切换） |
| 评测与线上不一致 | 环境差异 | staging 与生产同规格 |
| 模型漂移无感知 | 无在线评估 | 抽样评估 + 漂移告警 |

**MLOps 关键认知（2026）**：

```text
① 模型是"活产物"：权重会更新、效果会漂移——版本化是底线
② 评估是"门禁"不是"报告"：不达标不放行（CI 硬门槛）
③ 回滚是"常态"：模型回滚与代码回滚同等重要（预案要有）
④ 数据是"源头"：训练数据版本化才能复现问题
```

> 🎯 **核心要点**：MLOps 排障 = "**模型版本（DVC/注册表）+ 评估门禁（CI）+ 快速回滚（双服务）+ 数据溯源**"四件套——**"模型是活产物"是 2026 的核心认知转变**。
