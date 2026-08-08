# 08 - PyTorch 模型构建与训练循环

> 🎯 nn.Module + DataLoader + Optimizer + 训练循环 = PyTorch 四件套。这是 PyTorch 的核心编程模型，微调 LLM 就是在这套框架上操作

---

## 目录

1. [nn.Module：模型构建](#1-nnmodule模型构建)
2. [DataLoader：数据加载](#2-dataloader数据加载)
3. [损失函数与优化器](#3-损失函数与优化器)
4. [标准训练循环](#4-标准训练循环)
5. [模型保存与加载](#5-模型保存与加载)

---

## 1. nn.Module：模型构建

### 1.1 基本模式

```python
import torch.nn as nn

class MyMLP(nn.Module):
    """所有模型的基类都是 nn.Module"""
    
    def __init__(self, input_dim, hidden_dim, output_dim):
        super().__init__()
        # 定义层
        self.fc1 = nn.Linear(input_dim, hidden_dim)
        self.fc2 = nn.Linear(hidden_dim, hidden_dim)
        self.fc3 = nn.Linear(hidden_dim, output_dim)
        self.dropout = nn.Dropout(0.3)
        self.activation = nn.GELU()  # LLM 标配
    
    def forward(self, x):
        """定义前向传播 — 必须实现"""
        x = self.activation(self.fc1(x))
        x = self.dropout(x)
        x = self.activation(self.fc2(x))
        x = self.dropout(x)
        x = self.fc3(x)  # 输出层不加激活
        return x

model = MyMLP(768, 256, 10)
print(model)
# 查看参数量
print(f"参数总量: {sum(p.numel() for p in model.parameters()):,}")
```

### 1.2 常用层

```python
nn.Linear(in, out)          # 全连接层（含 bias）
nn.Conv2d(in, out, kernel)  # 2D 卷积
nn.LSTM(input, hidden)      # LSTM
nn.Embedding(vocab, dim)    # Embedding 层 → Token→向量
nn.LayerNorm(dim)           # 层归一化 → Transformer 标配
nn.BatchNorm1d(dim)         # 批归一化 → CNN 用
nn.Dropout(p)               # Dropout
nn.ReLU() / nn.GELU()       # 激活函数
nn.Sequential(...)          # 顺序容器
```

### 1.3 加载预训练模型

```python
from transformers import AutoModel

# HuggingFace 模型 = nn.Module 子类
model = AutoModel.from_pretrained("Qwen/Qwen2-7B")

# 冻结参数（只微调部分层）
for param in model.parameters():
    param.requires_grad = False

# 只解冻最后几层
for param in model.layers[-4:].parameters():
    param.requires_grad = True
```

---

## 2. DataLoader：数据加载

### 2.1 Dataset + DataLoader

```python
from torch.utils.data import Dataset, DataLoader

class MyDataset(Dataset):
    """自定义数据集 — 只需实现 __len__ 和 __getitem__"""
    
    def __init__(self, texts, labels, tokenizer, max_len=512):
        self.texts = texts
        self.labels = labels
        self.tokenizer = tokenizer
        self.max_len = max_len
    
    def __len__(self):
        return len(self.texts)
    
    def __getitem__(self, idx):
        encoding = self.tokenizer(
            self.texts[idx],
            max_length=self.max_len,
            padding='max_length',
            truncation=True,
            return_tensors='pt'
        )
        return {
            'input_ids': encoding['input_ids'].squeeze(),
            'attention_mask': encoding['attention_mask'].squeeze(),
            'labels': torch.tensor(self.labels[idx])
        }

# 使用
dataset = MyDataset(texts, labels, tokenizer)
dataloader = DataLoader(
    dataset,
    batch_size=32,
    shuffle=True,           # 训练时打乱
    num_workers=4,          # 多进程加载
    pin_memory=True         # 加速 GPU 传输
)
```

### 2.2 DataLoader 关键参数

| 参数 | 说明 | 建议值 |
|------|------|:---:|
| `batch_size` | 每批样本数 | 16~128（看显存） |
| `shuffle` | 是否打乱 | 训练 True、验证 False |
| `num_workers` | 加载进程数 | CPU 核心数 |
| `pin_memory` | 锁页内存 | GPU 训练时 True |
| `drop_last` | 丢弃最后不完整 batch | 训练 True |

---

## 3. 损失函数与优化器

```python
import torch.optim as optim

# === 损失函数 ===
criterion_cls = nn.CrossEntropyLoss()     # 多分类（LLM 训练用）
criterion_bin = nn.BCEWithLogitsLoss()    # 二分类
criterion_reg = nn.MSELoss()              # 回归

# === 优化器 ===
optimizer = optim.AdamW(
    model.parameters(),
    lr=2e-4,              # LoRA 常用学习率
    weight_decay=0.01     # L2 正则化
)

# === 学习率调度 ===
from torch.optim.lr_scheduler import CosineAnnealingLR

scheduler = CosineAnnealingLR(
    optimizer,
    T_max=100,             # 周期长度
    eta_min=1e-6           # 最小学习率
)
```

---

## 4. 标准训练循环

### 4.1 完整模板

```python
def train_epoch(model, dataloader, optimizer, criterion, device):
    model.train()
    total_loss = 0
    
    for batch in dataloader:
        # ① 移到 GPU
        inputs = batch['input_ids'].to(device)
        labels = batch['labels'].to(device)
        
        # ② 前向传播
        optimizer.zero_grad()
        outputs = model(inputs)
        loss = criterion(outputs, labels)
        
        # ③ 反向传播
        loss.backward()
        
        # ④ 梯度裁剪（LLM 训练必备）
        torch.nn.utils.clip_grad_norm_(model.parameters(), max_norm=1.0)
        
        # ⑤ 参数更新
        optimizer.step()
        
        total_loss += loss.item()
    
    return total_loss / len(dataloader)

def validate(model, dataloader, criterion, device):
    model.eval()
    total_loss = 0
    correct = 0
    total = 0
    
    with torch.no_grad():  # 推理不需梯度
        for batch in dataloader:
            inputs = batch['input_ids'].to(device)
            labels = batch['labels'].to(device)
            
            outputs = model(inputs)
            loss = criterion(outputs, labels)
            
            total_loss += loss.item()
            _, predicted = outputs.max(1)
            total += labels.size(0)
            correct += predicted.eq(labels).sum().item()
    
    return total_loss / len(dataloader), correct / total

# === 训练主循环 ===
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
model = model.to(device)

for epoch in range(num_epochs):
    train_loss = train_epoch(model, train_loader, optimizer, criterion, device)
    val_loss, val_acc = validate(model, val_loader, criterion, device)
    
    print(f"Epoch {epoch+1}: Train Loss={train_loss:.4f}, "
          f"Val Loss={val_loss:.4f}, Val Acc={val_acc:.4f}")
    
    scheduler.step()  # 更新学习率
```

### 4.2 训练模式 vs 评估模式

```text
model.train() 和 model.eval() 控制什么？

  ✅ Dropout：train 时开启（随机丢弃），eval 时关闭
  ✅ BatchNorm：train 时用 batch 统计量，eval 时用全局统计量
  ✅ 其他：自定义层可根据 self.training 调整行为

忘记 model.eval() → Dropout 在推理时仍活跃 → 输出随机 → 评估不准！
```

---

## 5. 模型保存与加载

```python
# === 保存 ===
# 方式 1：保存完整模型
torch.save(model, "model_full.pt")
model = torch.load("model_full.pt")

# 方式 2：只保存权重（推荐）
torch.save(model.state_dict(), "model_weights.pt")
model.load_state_dict(torch.load("model_weights.pt"))

# 方式 3：保存 checkpoint（含优化器状态，可断点续训）
checkpoint = {
    'epoch': epoch,
    'model_state_dict': model.state_dict(),
    'optimizer_state_dict': optimizer.state_dict(),
    'loss': train_loss,
}
torch.save(checkpoint, "checkpoint.pt")

# 恢复训练
checkpoint = torch.load("checkpoint.pt")
model.load_state_dict(checkpoint['model_state_dict'])
optimizer.load_state_dict(checkpoint['optimizer_state_dict'])
start_epoch = checkpoint['epoch']
```

---

## 核心要点回顾

- `nn.Module` 是所有模型的基类，必须实现 `forward()`
- `DataLoader` 自动批处理、打乱、多进程加载
- 训练循环 = Forward → Loss → Backward → Gradient Clip → Update
- `model.train()` / `model.eval()` 控制 Dropout 和 BatchNorm 行为
- 保存 `state_dict()` 比保存完整模型更灵活（推荐）
