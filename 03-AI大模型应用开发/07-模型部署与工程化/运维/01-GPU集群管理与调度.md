# 01 - GPU 集群管理与调度

> 🎯 GPU 是 LLM 的核心生产资料 — 不会管 GPU，就像开工厂不会管机器。NVLink 拓扑、GPU 池化、亲和性调度是三大核心

## 目录

1. [GPU 硬件拓扑](#1-gpu-硬件拓扑)
2. [GPU 资源监控](#2-gpu-资源监控)
3. [GPU 调度策略](#3-gpu-调度策略)
4. [GPU 池化与共享](#4-gpu-池化与共享)
5. [常见 GPU 故障排查](#5-常见-gpu-故障排查)

---

## 1. GPU 硬件拓扑

```bash
# 查看 GPU 拓扑
nvidia-smi topo -m

# 典型 4×A100 拓扑：
#         GPU0    GPU1    GPU2    GPU3
# GPU0     X      NV12    SYS     SYS     ← NV12=NVLINK 12通道
# GPU1    NV12     X      SYS     SYS
# GPU2    SYS     SYS      X      NV12
# GPU3    SYS     SYS     NV12     X

# NVLink 直连：GPU0↔GPU1, GPU2↔GPU3 高速通信
# SYS 连接：GPU0↔GPU2 需通过 CPU → 慢 5-10×
```

### 关键指标

| 指标 | 命令 | 关注点 |
|------|------|--------|
| GPU 利用率 | `nvidia-smi` | >80% 需扩容 |
| 显存使用 | `nvidia-smi` | >90% 告警 |
| GPU 温度 | `nvidia-smi -q -d TEMPERATURE` | >80°C 降频 |
| ECC Error | `nvidia-smi -q -d ECC` | 有增量=硬件故障 |
| NVLink 带宽 | `nvidia-smi nvlink -s` | 低于标称的80% |
| 功耗 | `nvidia-smi -q -d POWER` | 接近 TDP 上限 |

---

## 2. GPU 资源监控

```python
# nvidia-ml-py 精确获取 GPU 指标
import pynvml
pynvml.nvmlInit()

device_count = pynvml.nvmlDeviceGetCount()
for i in range(device_count):
    handle = pynvml.nvmlDeviceGetHandleByIndex(i)
    
    # 基本信息
    name = pynvml.nvmlDeviceGetName(handle)
    
    # 显存（字节）
    mem_info = pynvml.nvmlDeviceGetMemoryInfo(handle)
    mem_used_gb = mem_info.used / 1024**3
    mem_total_gb = mem_info.total / 1024**3
    
    # 利用率（百分比）
    util = pynvml.nvmlDeviceGetUtilizationRates(handle)
    
    # 温度
    temp = pynvml.nvmlDeviceGetTemperature(handle, pynvml.NVML_TEMPERATURE_GPU)
    
    # 运行的进程
    processes = pynvml.nvmlDeviceGetComputeRunningProcesses(handle)
    
    print(f"GPU{i} [{name}]: {mem_used_gb:.1f}/{mem_total_gb:.1f}GB, "
          f"Util={util.gpu}%, Temp={temp}°C, Procs={len(processes)}")
```

### Prometheus GPU Exporter

```yaml
# DCGM Exporter → Prometheus
# 部署：nvidia/dcgm-exporter
scrape_configs:
  - job_name: 'dcgm'
    static_configs:
      - targets: ['gpu-node:9400']
```

---

## 3. GPU 调度策略

```yaml
# K8s GPU 亲和性调度
apiVersion: v1
kind: Pod
spec:
  affinity:
    # ① 模型推理 Pod 尽量分散
    podAntiAffinity:
      preferredDuringScheduling:
        - weight: 100
          podAffinityTerm:
            labelSelector:
              matchLabels:
                app: llm-inference
            topologyKey: kubernetes.io/hostname  # 不同节点
    
    # ② Tensor Parallelism 需要 NVLink 直连 GPU
    nodeAffinity:
      requiredDuringScheduling:
        nodeSelectorTerms:
          - matchExpressions:
              - key: nvidia.com/gpu.product
                operator: In
                values: ["NVIDIA-A100-SXM4-40GB"]  # SXM 有 NVLink
```

### GPU 碎片整理

```bash
# 监控 GPU 碎片
kubectl describe node gpu-node-1 | grep nvidia.com/gpu
# nvidia.com/gpu:  4
# nvidia.com/gpu:  2   ← 用了 2 张，还剩 2 张

# 碎片问题：4 个节点各有 1 张空闲 → 需要 2 卡的 Pod 调度失败！
# 解决：GPU 共享（MIG）+ 碎片整理调度器（Volcano）
```

---

## 4. GPU 池化与共享

| 方案 | 粒度 | 隔离性 | 适用 |
|------|:---:|:---:|------|
| **MIG** | 物理隔离（A100/H100） | 强 | 多租户 |
| **MPS** | 进程级共享 | 中 | 同应用 |
| **Time-Slicing** | 时间片轮转 | 弱 | 开发测试 |
| **vCUDA (OrionX等)** | 虚拟化 | 强 | 企业 |

```bash
# A100 MIG 配置：1 张 A100 → 7 个独立 GPU 实例
nvidia-smi mig -cgi 9,9,9,9,9,9,9  # 7×5GB 实例
nvidia-smi mig -cci                 # 创建对应的计算实例
```

---

## 5. 常见 GPU 故障排查

| 故障 | 现象 | 排查 | 解决 |
|------|------|------|------|
| **ECC Error** | 推理结果异常，dmesg 报错 | `nvidia-smi -q -d ECC` | 换卡/重启重置 |
| **Xid Error** | GPU 从总线断开 | `dmesg \| grep Xid` | Xid 79/48=硬件故障 |
| **OOM** | CUDA out of memory | `nvidia-smi` 显存 | 减 batch/量化/换卡 |
| **Throttling** | 性能下降 | 温度/功耗 | 散热/降频 |
| **NVLINK 断** | 多卡训练变慢 | `nvidia-smi nvlink -e` | 重新插拔/换卡 |
