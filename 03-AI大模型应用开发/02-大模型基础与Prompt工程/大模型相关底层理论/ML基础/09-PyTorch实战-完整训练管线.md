# 09 - PyTorch 实战：完整训练管线

> 🎯 从数据到部署的端到端训练管线 — Checkpoint 断点续训、TensorBoard 可视化、混合精度加速、Early Stopping 防过拟合

---

## 目录

1. [端到端训练管线概览](#1-端到端训练管线概览)
2. [完整训练脚本](#2-完整训练脚本)
3. [TensorBoard 可视化](#3-tensorboard-可视化)
4. [混合精度训练](#4-混合精度训练)
5. [分布式训练入门](#5-分布式训练入门)

---

## 1. 端到端训练管线概览

```text
生产级训练管线：

┌──────────────────────────────────────────────────────┐
│ ① 配置管理       ② 数据准备        ③ 模型初始化       │
│ (argparse/yaml)  (Dataset+Loader)  (from_pretrained) │
├──────────────────────────────────────────────────────┤
│ ④ 训练循环                                          │
│   Forward → Loss → Backward → Clip → Optimizer.step │
│   ├── Checkpoint（定时保存）                          │
│   ├── Validation（每个 epoch）                       │
│   ├── Early Stopping（监控 val_loss）                │
│   └── TensorBoard（记录 loss/lr/grad）               │
├──────────────────────────────────────────────────────┤
│ ⑤ 测试评估       ⑥ 模型导出        ⑦ 部署           │
│ (Test Set)       (ONNX/TorchScript) (Java 端)       │
└──────────────────────────────────────────────────────┘
```

---

## 2. 完整训练脚本

```python
import torch
import torch.nn as nn
from torch.utils.data import DataLoader
from torch.utils.tensorboard import SummaryWriter
from torch.cuda.amp import GradScaler, autocast
import argparse
import os
from datetime import datetime

class Trainer:
    """生产级训练器"""
    
    def __init__(self, model, train_loader, val_loader,
                 optimizer, scheduler, device, config):
        self.model = model
        self.train_loader = train_loader
        self.val_loader = val_loader
        self.optimizer = optimizer
        self.scheduler = scheduler
        self.device = device
        self.config = config
        
        # TensorBoard
        self.writer = SummaryWriter(
            f"runs/{config['exp_name']}_{datetime.now():%Y%m%d_%H%M}"
        )
        
        # 混合精度
        self.scaler = GradScaler(enabled=config['use_amp'])
        
        # 追踪
        self.best_val_loss = float('inf')
        self.epochs_no_improve = 0
    
    def train_epoch(self):
        self.model.train()
        total_loss = 0
        
        for step, batch in enumerate(self.train_loader):
            batch = {k: v.to(self.device) for k, v in batch.items()}
            
            self.optimizer.zero_grad()
            
            # 混合精度前向传播
            with autocast(device_type='cuda', 
                         dtype=torch.bfloat16,
                         enabled=self.config['use_amp']):
                outputs = self.model(**batch)
                loss = outputs.loss
            
            # 混合精度反向传播
            self.scaler.scale(loss).backward()
            
            # 梯度裁剪
            self.scaler.unscale_(self.optimizer)
            torch.nn.utils.clip_grad_norm_(
                self.model.parameters(), 
                self.config['max_grad_norm']
            )
            
            # 参数更新
            self.scaler.step(self.optimizer)
            self.scaler.update()
            self.scheduler.step()
            
            total_loss += loss.item()
            
            # 每 N 步记录
            if step % self.config['log_interval'] == 0:
                global_step = self._global_step()
                self.writer.add_scalar('Train/Loss', loss.item(), global_step)
                self.writer.add_scalar('Train/LR', 
                    self.scheduler.get_last_lr()[0], global_step)
        
        return total_loss / len(self.train_loader)
    
    def validate(self):
        self.model.eval()
        total_loss = 0
        
        with torch.no_grad():
            for batch in self.val_loader:
                batch = {k: v.to(self.device) for k, v in batch.items()}
                outputs = self.model(**batch)
                total_loss += outputs.loss.item()
        
        return total_loss / len(self.val_loader)
    
    def train(self, num_epochs):
        for epoch in range(num_epochs):
            train_loss = self.train_epoch()
            val_loss = self.validate()
            
            print(f"Epoch {epoch+1}/{num_epochs} | "
                  f"Train Loss: {train_loss:.4f} | Val Loss: {val_loss:.4f}")
            
            self.writer.add_scalars('Epoch/Loss', {
                'train': train_loss, 'val': val_loss
            }, epoch)
            
            # Checkpoint 保存
            self._save_checkpoint(epoch, val_loss)
            
            # Early Stopping
            if val_loss < self.best_val_loss - self.config['min_delta']:
                self.best_val_loss = val_loss
                self.epochs_no_improve = 0
                self._save_checkpoint(epoch, val_loss, best=True)
            else:
                self.epochs_no_improve += 1
                if self.epochs_no_improve >= self.config['patience']:
                    print(f"Early stopping at epoch {epoch+1}")
                    break
    
    def _save_checkpoint(self, epoch, val_loss, best=False):
        checkpoint = {
            'epoch': epoch,
            'model_state_dict': self.model.state_dict(),
            'optimizer_state_dict': self.optimizer.state_dict(),
            'scheduler_state_dict': self.scheduler.state_dict(),
            'val_loss': val_loss,
            'config': self.config
        }
        prefix = 'best' if best else f'epoch_{epoch+1}'
        torch.save(checkpoint, f"checkpoints/{prefix}.pt")
    
    def _global_step(self):
        return len(self.train_loader) * self._current_epoch + self._current_step

# === 使用 ===
config = {
    'exp_name': 'my_experiment',
    'use_amp': True,
    'max_grad_norm': 1.0,
    'log_interval': 100,
    'patience': 5,
    'min_delta': 0.001,
}

trainer = Trainer(model, train_loader, val_loader,
                  optimizer, scheduler, device, config)
trainer.train(num_epochs=10)
```

---

## 3. TensorBoard 可视化

```bash
# 启动 TensorBoard
tensorboard --logdir=runs --port=6006
# 浏览器打开 http://localhost:6006
```

```python
from torch.utils.tensorboard import SummaryWriter

writer = SummaryWriter("runs/experiment_1")

# 记录标量（Loss、Accuracy、LR）
writer.add_scalar('Loss/train', loss, epoch)
writer.add_scalar('Loss/val', val_loss, epoch)
writer.add_scalar('LR', lr, epoch)

# 记录多个标量（同一图对比 train/val）
writer.add_scalars('Loss', {
    'train': train_loss, 'val': val_loss
}, epoch)

# 记录模型图
writer.add_graph(model, sample_input)

# 记录直方图（权重分布、梯度分布）
writer.add_histogram('fc1.weight', model.fc1.weight, epoch)
writer.add_histogram('fc1.weight.grad', model.fc1.weight.grad, epoch)

# 记录 Embedding（降维可视化）
writer.add_embedding(embeddings, metadata=labels, tag='embeddings')

writer.close()
```

---

## 4. 混合精度训练

### 4.1 原理

```text
混合精度（Automatic Mixed Precision, AMP）：

  前向传播 → FP16/BF16（快，省显存）
  反向传播 → FP16/BF16 梯度
  参数更新 → FP32（精度保证）
  
  BF16 优势：
  ✅ 指数位与 FP32 相同（8位）→ 动态范围大，不易溢出
  ✅ 不需要 Loss Scaling（FP16 需要）
  ✅ LLM 训练标配

显存节省：约 40%
速度提升：约 1.5~2×
```

### 4.2 代码

```python
# BF16 混合精度（推荐）
with torch.cuda.amp.autocast(dtype=torch.bfloat16):
    outputs = model(**batch)
    loss = outputs.loss

# FP16 混合精度（需要 GradScaler）
scaler = torch.cuda.amp.GradScaler()
with torch.cuda.amp.autocast():
    loss = model(**batch).loss

scaler.scale(loss).backward()   # 放大 loss 防止梯度下溢
scaler.step(optimizer)           # 更新前缩小
scaler.update()
```

---

## 5. 分布式训练入门

### 5.1 策略对比

| 策略 | 原理 | 适用 |
|------|------|------|
| **DataParallel (DP)** | 数据分片到多 GPU | 单机多卡（简单） |
| **DistributedDataParallel (DDP)** | 每 GPU 独立进程 | **单机多卡推荐** |
| **FSDP (Fully Sharded)** | 模型参数也分片 | 多机多卡 |
| **DeepSpeed ZeRO** | 极致显存优化 | 超大模型 |

### 5.2 DDP 最小示例

```python
import torch.distributed as dist
from torch.nn.parallel import DistributedDataParallel as DDP

# 初始化进程组
dist.init_process_group(backend='nccl')
local_rank = int(os.environ['LOCAL_RANK'])

# 模型 + DDP
model = model.to(local_rank)
model = DDP(model, device_ids=[local_rank])

# DataLoader 需 DistributedSampler
sampler = DistributedSampler(dataset)
loader = DataLoader(dataset, sampler=sampler, batch_size=32)

# 训练循环（基本不变！）
for epoch in range(num_epochs):
    sampler.set_epoch(epoch)  # 每个 epoch 不同 shuffle
    for batch in loader:
        loss = model(batch.to(local_rank)).loss
        loss.backward()
        optimizer.step()

# 启动命令
# torchrun --nproc_per_node=4 train.py
```

---

## 核心要点回顾

- 生产训练管线 = 配置 + 数据 + 训练循环 + Checkpoint + 可视化 + 导出
- TensorBoard 记录 Loss/LR/Gradient/Embedding
- 混合精度（BF16）省 40% 显存、提 1.5~2× 速度
- DDP 是单机多卡训练的标准方案
- Checkpoint 包含 optimizer 状态 → 支持断点续训
