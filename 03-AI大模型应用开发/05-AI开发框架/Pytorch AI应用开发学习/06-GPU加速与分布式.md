# 06 GPU 加速与分布式

> 训练提速三件套：CUDA 设备管理（to(device)）、AMP 混合精度（FP16 显存减半+提速）、DDP 多卡并行——单卡到多卡的标准路径。

## 📚 目录

1. [GPU 设备管理](#1-gpu-设备管理)
2. [显存优化基础](#2-显存优化基础)
3. [AMP 混合精度](#3-amp-混合精度)
4. [单机多卡：DataParallel vs DDP](#4-单机多卡dataparallel-vs-ddp)
5. [DDP 实战](#5-ddp-实战)
6. [显存排查与调优](#6-显存排查与调优)
7. [面试高频问法](#7-面试高频问法)

## 1. GPU 设备管理

### 设备选择

```python
import torch

# 检测 GPU
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# 移动到设备
model.to(device)
x, y = x.to(device), y.to(device)    # 数据和模型都要移！
```

### 多卡管理

```python
torch.cuda.device_count()          # GPU 数量
torch.cuda.get_device_name(0)      # GPU 名称
torch.cuda.set_device(0)           # 设置当前设备
torch.cuda.empty_cache()           # 清缓存（调试用，别滥用）
```

### 常见坑

| 坑 | 现象 |
|---|---|
| 只移模型不移数据 | device mismatch 报错 |
| 忘 to(device) | CPU 训练（慢） |
| 多卡默认只用 0 卡 | 需 DDP/DataParallel |
| CPU 上跑 cuda 代码 | 环境检查 device |

## 2. 显存优化基础

### 显存占用构成

```
训练显存 = 参数 + 梯度 + 优化器状态 + 激活值（中间结果）
激活值是大头（batch 越大越多）
```

### 基础优化手段

| 手段 | 效果 |
|---|---|
| 降 batch_size | 直接降显存（最常用） |
| AMP（下节） | 显存约减半 |
| 梯度累积 | 小 batch 模拟大 batch |
| 梯度检查点 | 牺牲计算换显存 |
| 释放中间变量 | 及时 del |

### 梯度累积（模拟大 batch）

```python
accum_steps = 4    # 等效 batch ×4
optimizer.zero_grad()
for i, (x, y) in enumerate(loader):
    loss = criterion(model(x), y) / accum_steps   # 平均
    loss.backward()
    if (i + 1) % accum_steps == 0:
        optimizer.step()
        optimizer.zero_grad()
```

## 3. AMP 混合精度

### 原理

```
FP16 计算 + FP32 存储主权重：
前向/反向用 FP16（快+省显存）
优化器更新用 FP32（精度）
自动缩放损失防梯度下溢
```

### 使用（torch.cuda.amp）

```python
from torch.cuda.amp import autocast, GradScaler

scaler = GradScaler()          # 损失缩放器

for x, y in loader:
    x, y = x.to(device), y.to(device)
    optimizer.zero_grad()

    with autocast():                       # FP16 计算
        logits = model(x)
        loss = criterion(logits, y)

    scaler.scale(loss).backward()          # 缩放反向
    scaler.step(optimizer)                 # 更新（先 unscale）
    scaler.update()                        # 更新缩放因子
```

### AMP 收益

| 收益 | 说明 |
|---|---|
| 显存 | 约减半（FP16 激活） |
| 速度 | 1.5-3 倍（A100 等支持） |
| 精度 | 几乎无损（主权重 FP32） |

### 注意事项

```
① CPU 不支持 autocast（仅 CUDA）
② 与 torch.compile 兼容
③ 损失必须 scaler 处理（防下溢）
④ 大模型标配（LLM 训练必备）
```

## 4. 单机多卡：DataParallel vs DDP

| 维度 | DataParallel（DP） | DistributedDataParallel（DDP） |
|---|---|---|
| 模型复制 | 每卡一份（主卡汇总） | 每卡一份（梯度同步） |
| 通信 | 主卡瓶颈（慢） | 点对点（高效） |
| 适用 | 小模型/学习 | **生产标准** |
| 推荐 | 不推荐（官方） | **官方推荐** |

### 为什么 DDP 更优

```
DP：前向分散、梯度汇总到主卡（主卡瓶颈）
DDP：每卡独立前向+反向，梯度 All-Reduce 同步（高效）
官方明确：新代码用 DDP
```

## 5. DDP 实战

### 启动方式（torchrun）

```bash
# 单机 4 卡
torchrun --nproc_per_node=4 train_ddp.py
```

### 脚本结构

```python
import torch
import torch.distributed as dist
from torch.nn.parallel import DistributedDataParallel as DDP

def setup():
    dist.init_process_group(backend="nccl")   # 初始化
    torch.cuda.set_device(int(os.environ["LOCAL_RANK"]))

def main():
    setup()
    rank = int(os.environ["LOCAL_RANK"])       # 当前卡号
    device = f"cuda:{rank}"

    model = MyModel().to(device)
    model = DDP(model, device_ids=[rank])      # 包 DDP

    # 数据：每卡分片（DistributedSampler）
    from torch.utils.data.distributed import DistributedSampler
    sampler = DistributedSampler(dataset, shuffle=True)
    loader = DataLoader(dataset, batch_size=32, sampler=sampler)

    for epoch in range(epochs):
        sampler.set_epoch(epoch)               # 每 epoch 重打乱
        for x, y in loader:
            x, y = x.to(device), y.to(device)
            optimizer.zero_grad()
            loss = criterion(model(x), y)
            loss.backward()
            optimizer.step()

    # 只在主卡保存
    if rank == 0:
        torch.save(model.module.state_dict(), "model.pt", weights_only=True)
    dist.destroy_process_group()

if __name__ == "__main__":
    main()
```

### DDP 要点

| 要点 | 说明 |
|---|---|
| torchrun 启动 | --nproc_per_node=卡数 |
| DistributedSampler | 数据分片 + set_epoch |
| 只在主卡保存 | rank==0 判断 |
| model.module | DDP 包后取原始模型 |
| batch 语义 | 每卡 batch×卡数 = 全局 batch |

## 6. 显存排查与调优

### 排查工具

```bash
nvidia-smi                  # 显存/利用率
torch.cuda.memory_summary() # 分配明细
# 或 PyTorch Profiler
```

### 调优流程

```
① OOM？→ 降 batch 或 AMP（先 AMP）
② GPU 利用率低？→ 数据瓶颈（03 篇）
③ 训练慢？→ compile + AMP + 检查瓶颈
④ 显存泄漏？→ 检查 no_grad/缓存
```

### 大模型显存经验公式

```
训练显存 ≈ 参数 × 18 字节（FP16 微调约 ×6）
推理显存 ≈ 参数 × 2 字节（FP16）
例：7B 模型：训练约 126GB（需多卡/量化），推理约 14GB
（详见 `03-AI.../模型推理与部署/` 体系）
```

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| AMP 原理？ | FP16 计算 + FP32 主权重 + 损失缩放 |
| AMP 收益？ | 显存减半、速度 1.5-3 倍 |
| DP vs DDP？ | DDP 梯度同步更高效（官方推荐） |
| DDP 怎么启动？ | torchrun --nproc_per_node=N |
| 梯度累积？ | 小 batch 模拟大 batch（省显存） |
| 显存不够怎么办？ | AMP→降 batch→梯度累积→检查点 |
| 大模型显存公式？ | 训练约 18 字节/参，推理约 2 字节/参 |

### 面试加分表达

> "单卡到多卡的标准路径：先 AMP（显存减半 + 1.5-3 倍提速，损失必须 GradScaler 处理），再 DDP（torchrun 启动 + DistributedSampler 分片 + 只在主卡保存）。显存不够的顺序：AMP → 降 batch → 梯度累积——不要一上来就换卡。官方明确 DataParallel 已不推荐，新代码用 DDP。"

> 🎯 核心要点：GPU 三件套（to(device)/AMP/DDP）；AMP = FP16 计算 + FP32 主权重 + GradScaler（显存减半）；DDP 取代 DP（torchrun + DistributedSampler + 主卡保存）；显存优化顺序（AMP→batch→累积→检查点）；大模型显存公式（训练 18B/参、推理 2B/参）；利用率低先查数据瓶颈。

---

**下一模块**：[07-模型保存部署与推理](07-模型保存部署与推理.md) / **返回总览**：[00-Pytorch学习体系总览](00-Pytorch学习体系总览.md)
