# 第1步：Python编程与核心库

> **阶段目标：** 掌握Python编程基础，熟练使用NumPy进行数据处理，能够用Matplotlib进行数据可视化  
> **预计学时：** 2-3周（每天3-4小时）  
> **前置要求：** 无（零基础可学）  

---

## 📚 目录

- [1.1 Python基础语法](#11-python基础语法)
- [1.2 NumPy数据处理](#12-numpy数据处理)
- [1.3 Matplotlib图表可视化](#13-matplotlib图表可视化)
- [1.4 Pandas数据分析（补充）](#14-pandas数据分析补充)
- [1.5 阶段练习](#15-阶段练习)
- [1.6 常见问题](#16-常见问题)

---

## 1.1 Python基础语法

### 1.1.1 为什么Python是AI开发的首选语言？

| 优势 | 说明 |
|------|------|
| **生态丰富** | NumPy, PyTorch, TensorFlow, HuggingFace 等库均以Python为第一语言 |
| **语法简洁** | 动态类型，代码量少，迭代快 |
| **社区庞大** | 遇到问题容易搜索解决方案 |
| **胶水语言** | 可与C/C++/CUDA高效交互，底层性能不输编译语言 |

### 1.1.2 环境搭建

```bash
# 推荐使用 conda 管理环境
conda create -n ai-learning python=3.10
conda activate ai-learning

# 或使用 venv
python -m venv ai-env
source ai-env/bin/activate  # Linux/Mac
ai-env\Scripts\activate     # Windows

# 安装核心库
pip install numpy pandas matplotlib jupyter
```

### 1.1.3 核心语法速查

#### 变量与数据类型

```python
# 基本类型
name: str = "AI开发"          # 字符串
version: int = 3              # 整数
price: float = 0.002          # 浮点数（Token价格）
is_active: bool = True        # 布尔值

# 容器类型
models: list = ["GPT-4", "Claude", "Gemini"]       # 列表
config: dict = {"temperature": 0.7, "max_tokens": 1024}  # 字典
model_ids: set = {"gpt-4", "gpt-3.5-turbo"}        # 集合
params: tuple = (0.7, 1024, 1.0)                   # 元组（不可变）
```

#### 控制流

```python
# 条件判断 — 在AI中常用于参数校验
def validate_temperature(temp: float) -> bool:
    if not 0 <= temp <= 2.0:
        raise ValueError(f"temperature必须在[0,2]之间，当前值: {temp}")
    return True

# 循环 — 在AI中常用于批量处理
messages = ["你好", "今天天气怎么样", "写一首诗"]
for i, msg in enumerate(messages):
    print(f"[{i}] {msg}")

# 列表推导式 — Pythonic的数据处理方式
token_lengths = [len(msg) for msg in messages if len(msg) > 2]
```

#### 函数

```python
# 类型提示 — 企业级代码的标配
from typing import List, Optional, Dict, Any

def call_llm(
    prompt: str,
    model: str = "gpt-4",
    temperature: float = 0.7,
    max_tokens: int = 1024,
) -> Dict[str, Any]:
    """调用大模型API的统一接口
    
    Args:
        prompt: 用户输入的提示词
        model: 模型名称
        temperature: 创造性参数 (0-2)
        max_tokens: 最大输出Token数
    
    Returns:
        {"content": str, "tokens_used": int, "cost": float}
    """
    # 实际调用逻辑
    pass

# lambda 函数 — 简单数据转换
parse_cost = lambda price, tokens: price * tokens / 1000
```

#### 面向对象

```python
from dataclasses import dataclass
from typing import List

@dataclass
class Message:
    """聊天消息数据类"""
    role: str          # "system" | "user" | "assistant"
    content: str

class ChatSession:
    """聊天会话管理器"""
    
    def __init__(self, system_prompt: str = ""):
        self.messages: List[Message] = []
        if system_prompt:
            self.messages.append(Message(role="system", content=system_prompt))
    
    def add_user_message(self, content: str) -> None:
        self.messages.append(Message(role="user", content=content))
    
    def add_assistant_message(self, content: str) -> None:
        self.messages.append(Message(role="assistant", content=content))
    
    def to_api_format(self) -> List[dict]:
        """转为OpenAI兼容的API格式"""
        return [{"role": m.role, "content": m.content} for m in self.messages]
```

### 1.1.4 错误处理 — 企业级应用必须掌握

```python
import logging
from typing import Optional

logger = logging.getLogger(__name__)

class LLMAPIError(Exception):
    """大模型API自定义异常"""
    pass

def safe_api_call(prompt: str, retries: int = 3) -> Optional[str]:
    """带重试的安全API调用"""
    for attempt in range(retries):
        try:
            response = call_llm(prompt)
            return response["content"]
        except ConnectionError:
            logger.warning(f"连接失败，第{attempt+1}次重试...")
            time.sleep(2 ** attempt)  # 指数退避
        except LLMAPIError as e:
            logger.error(f"API错误: {e}")
            raise  # 业务错误不重试
    return None
```

---

## 1.2 NumPy数据处理

### 1.2.1 为什么AI开发需要NumPy？

在AI开发中，NumPy是数据管道的基石：

- **Token处理**：将文本Token转为数值数组
- **Embedding计算**：向量相似度计算（余弦相似度）
- **概率计算**：Softmax、采样等操作
- **数据预处理**：标准化、归一化、批处理

### 1.2.2 核心操作

#### 数组创建

```python
import numpy as np

# 从列表创建 — 模拟Token IDs
token_ids = np.array([101, 2054, 2003, 1037, 2742, 102])
print(token_ids)        # [101 2054 2003 1037 2742 102]
print(token_ids.shape)  # (6,)
print(token_ids.dtype)  # int64

# 特殊数组
zeros = np.zeros((3, 4))          # 全0 — 用于padding mask
ones = np.ones((2, 3))            # 全1
random_emb = np.random.randn(5, 768)  # 随机 — 模拟Embedding向量
```

#### Embedding操作 — AI开发中最常见的NumPy场景

```python
# 模拟Word Embeddings: 词汇表大小10000, 维度768
vocab_size, embed_dim = 10000, 768
embedding_matrix = np.random.randn(vocab_size, embed_dim) * 0.02

# 查找Embedding
token_ids = np.array([101, 2054, 2003])  # [CLS], "hello", "is"
token_embeddings = embedding_matrix[token_ids]  # (3, 768)

# 余弦相似度 — 判断两个词向量的语义相似度
def cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
    """计算两个向量的余弦相似度"""
    return np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))

hello_emb = embedding_matrix[2054]
hi_emb = embedding_matrix[2789]      # "hi"的ID
cat_emb = embedding_matrix[4930]     # "cat"的ID

print(f"hello vs hi: {cosine_similarity(hello_emb, hi_emb):.4f}")   # 应该较高
print(f"hello vs cat: {cosine_similarity(hello_emb, cat_emb):.4f}") # 应该较低
```

#### Attention矩阵操作

```python
# 模拟Self-Attention中的Score计算
seq_len = 10
d_k = 64

Q = np.random.randn(seq_len, d_k)  # Query
K = np.random.randn(seq_len, d_k)  # Key

# 计算Attention Score: Q @ K^T / sqrt(d_k)
scores = np.dot(Q, K.T) / np.sqrt(d_k)  # (10, 10)

# Softmax（数值稳定版）
def stable_softmax(x: np.ndarray, axis: int = -1) -> np.ndarray:
    """数值稳定的Softmax实现"""
    x_max = np.max(x, axis=axis, keepdims=True)
    exp_x = np.exp(x - x_max)
    return exp_x / np.sum(exp_x, axis=axis, keepdims=True)

attention_weights = stable_softmax(scores)  # (10, 10)
```

#### 批量处理与Broadcasting

```python
# 批量Token计数 — 模拟处理多个prompt
batch_prompts = np.array([
    [101, 2054, 2003, 102, 0, 0],    # padding到相同长度
    [101, 2742, 102, 0, 0, 0],
    [101, 4930, 2003, 1037, 2742, 102],
])

# 创建attention mask（标记真实token位置）
attention_mask = (batch_prompts != 0).astype(np.float32)
print(attention_mask)
# [[1. 1. 1. 1. 0. 0.]
#  [1. 1. 1. 0. 0. 0.]
#  [1. 1. 1. 1. 1. 1.]]

# Broadcasting — 批量归一化
# 假设有batch_size=32, seq_len=128, hidden_dim=768
hidden_states = np.random.randn(32, 128, 768)
mean = np.mean(hidden_states, axis=-1, keepdims=True)  # (32, 128, 1)
std = np.std(hidden_states, axis=-1, keepdims=True)
normalized = (hidden_states - mean) / (std + 1e-5)     # Layer Normalization
```

---

## 1.3 Matplotlib图表可视化

### 1.3.1 AI开发中的可视化场景

| 场景 | 图表类型 | 用途 |
|------|----------|------|
| 训练监控 | 折线图 | Loss曲线、学习率变化 |
| 模型评估 | 混淆矩阵、ROC曲线 | 分类模型效果 |
| Token分析 | 柱状图、直方图 | Token使用量分布 |
| Attention可视化 | 热力图 | Attention权重展示 |
| Embedding可视化 | 散点图（降维后） | 词向量分布 |

### 1.3.2 核心图表

#### 训练Loss曲线

```python
import matplotlib.pyplot as plt
import numpy as np

# 模拟训练数据
epochs = np.arange(1, 51)
train_loss = 2.5 * np.exp(-epochs / 15) + 0.3 + np.random.normal(0, 0.05, 50)
val_loss = 2.5 * np.exp(-epochs / 15) + 0.5 + np.random.normal(0, 0.08, 50)

fig, ax = plt.subplots(figsize=(10, 5))
ax.plot(epochs, train_loss, 'b-', label='Training Loss', linewidth=1.5)
ax.plot(epochs, val_loss, 'r--', label='Validation Loss', linewidth=1.5)
ax.axvline(x=20, color='gray', linestyle=':', alpha=0.7, label='Best Model')
ax.set_xlabel('Epoch', fontsize=12)
ax.set_ylabel('Loss', fontsize=12)
ax.set_title('Model Training Progress', fontsize=14, fontweight='bold')
ax.legend(fontsize=11)
ax.grid(True, alpha=0.3)
plt.tight_layout()
# plt.savefig('training_loss.png', dpi=150, bbox_inches='tight')
```

#### Token使用量分析

```python
# 模拟API调用的Token使用数据
models = ['GPT-4', 'GPT-3.5', 'Claude-3', 'Gemini']
prompt_tokens = [1500, 800, 1200, 900]
completion_tokens = [500, 400, 300, 600]

fig, ax = plt.subplots(figsize=(8, 5))
x = np.arange(len(models))
width = 0.35

bars1 = ax.bar(x - width/2, prompt_tokens, width, label='Prompt Tokens', color='#2E86AB')
bars2 = ax.bar(x + width/2, completion_tokens, width, label='Completion Tokens', color='#A23B72')

ax.set_ylabel('Token Count', fontsize=12)
ax.set_title('Token Usage by Model', fontsize=14)
ax.set_xticks(x)
ax.set_xticklabels(models)
ax.legend()

# 添加数值标签
for bar in bars1:
    ax.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 10,
            str(bar.get_height()), ha='center', va='bottom', fontsize=9)
for bar in bars2:
    ax.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 10,
            str(bar.get_height()), ha='center', va='bottom', fontsize=9)
```

#### Attention热力图

```python
# 模拟Attention权重矩阵
seq_len = 20
attention_weights = np.random.rand(seq_len, seq_len)
# 让对角线附近的值更大（模拟局部注意）
for i in range(seq_len):
    for j in range(seq_len):
        attention_weights[i, j] *= np.exp(-abs(i - j) / 3)

fig, ax = plt.subplots(figsize=(8, 7))
im = ax.imshow(attention_weights, cmap='YlOrRd', aspect='auto')
ax.set_xlabel('Key Position', fontsize=12)
ax.set_ylabel('Query Position', fontsize=12)
ax.set_title('Self-Attention Weights Heatmap', fontsize=14, fontweight='bold')
plt.colorbar(im, ax=ax, label='Attention Weight')
```

---

## 1.4 Pandas数据分析（补充）

> Pandas虽然在第1步不是主角，但在实际AI开发中大量使用，建议同步学习。

### 1.4.1 核心场景

```python
import pandas as pd

# 管理Prompt测试结果
results = pd.DataFrame({
    'prompt_id': [1, 2, 3, 4, 5],
    'model': ['GPT-4', 'GPT-4', 'Claude', 'Claude', 'GPT-3.5'],
    'prompt_tokens': [120, 85, 200, 150, 60],
    'completion_tokens': [300, 150, 400, 250, 100],
    'cost': [0.012, 0.006, 0.008, 0.005, 0.0008],
    'latency_ms': [1200, 800, 1500, 1100, 400],
})

# 按模型汇总
print(results.groupby('model').agg({
    'cost': ['mean', 'sum'],
    'latency_ms': ['mean', 'max'],
    'completion_tokens': 'sum'
}))
```

---

## 1.5 阶段练习

### 练习1：Token计数工具

编写一个函数，统计一段文本的预估Token数：

```python
def estimate_tokens(text: str) -> int:
    """
    粗略估计Token数（英文约4字符=1 Token，中文约1字=1-2 Token）
    精确计算请使用tiktoken库
    """
    # TODO: 实现中英文混合的Token估算
    pass

# 测试
print(estimate_tokens("Hello, how are you?"))  # 约6 tokens
print(estimate_tokens("你好，今天天气怎么样？"))  # 约8-12 tokens
```

### 练习2：Embedding相似度搜索

```python
# 给定一个词向量矩阵和一个查询词，找出最相似的5个词
def find_similar_words(
    query_emb: np.ndarray,
    embedding_matrix: np.ndarray,
    word_ids: np.ndarray,
    top_k: int = 5
) -> np.ndarray:
    """使用余弦相似度找出最相似的k个词"""
    # TODO: 实现
    pass
```

### 练习3：Loss曲线可视化

```python
# 读取CSV格式的训练日志，绘制Loss曲线和Accuracy曲线
def plot_training_history(log_path: str) -> None:
    """从训练日志文件绘制可视化图表"""
    # TODO: 实现
    pass
```

---

## 1.6 常见问题

### Q1: Python要学到什么程度才能开始学AI？

**答：** 不需要精通。掌握以下内容即可开始第2步：
- 基本数据类型和容器（list, dict, tuple）
- 函数定义和调用
- 类的基本使用
- 文件读写
- pip安装包

NumPy和Pandas在实际项目中边用边学效果最好。

### Q2: NumPy的broadcasting规则是什么？

**答：** 从右向左对齐维度，每个维度必须相等或其中一个为1。这是AI开发中批量处理的基础。

### Q3: 可以用Plotly代替Matplotlib吗？

**答：** 完全可以。Matplotlib是基础，Plotly在交互性上更强。企业项目中两者经常混用：
- 静态报告 → Matplotlib
- 交互式Dashboard → Plotly

### Q4: 需要学Jupyter Notebook吗？

**答：** 强烈推荐。Jupyter Notebook是AI开发的"实验室笔记本"，适合：
- 快速实验和数据探索
- 可视化展示
- 教学和文档

生产代码最终应整理为 `.py` 脚本或模块。

---

> **✅ 阶段完成检查清单：**
> - [ ] 能独立编写带类型提示的Python函数
> - [ ] 会用NumPy进行向量运算和矩阵操作
> - [ ] 能用Matplotlib绘制折线图、柱状图、热力图
> - [ ] 能用Pandas读取CSV并进行分组统计
> - [ ] 完成了3个阶段练习
>
> **下一步：** [第2步：机器学习基础](../02-机器学习基础/README.md)
