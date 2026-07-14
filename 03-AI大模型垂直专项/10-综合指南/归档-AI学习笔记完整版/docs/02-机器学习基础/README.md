# 第2步：机器学习基础

> **阶段目标：** 理解机器学习核心概念，掌握线性回归和决策树，建立模型训练的基本直觉  
> **预计学时：** 2-3周（每天3-4小时）  
> **前置要求：** Python基础 + NumPy  

---

## 📚 目录

- [2.1 机器学习概述](#21-机器学习概述)
- [2.2 线性回归](#22-线性回归)
- [2.3 决策树](#23-决策树)
- [2.4 模型训练核心概念](#24-模型训练核心概念)
- [2.5 Scikit-learn实战](#25-scikit-learn实战)
- [2.6 阶段练习](#26-阶段练习)
- [2.7 常见问题](#27-常见问题)

---

## 2.1 机器学习概述

### 2.1.1 为什么要学机器学习再学大模型？

大模型（LLM）本质上是极度放大的神经网络。理解基础ML概念后：

| ML概念 | 在大模型中的对应 |
|--------|-----------------|
| 损失函数 | Cross-Entropy Loss（语言模型训练目标） |
| 梯度下降 | Adam/AdamW优化器（训练LLM的标准选择） |
| 过拟合 | LLM的记忆化问题（Memorization） |
| 正则化 | Dropout, Weight Decay（LLM中也用） |
| 训练/验证/测试 | LLM的Pretrain/Fine-tune/Evaluation |

### 2.1.2 机器学习的三种范式

| 范式 | 定义 | LLM中的体现 |
|------|------|------------|
| **监督学习** | 有标签数据训练 | Fine-tuning（指令微调） |
| **无监督学习** | 无标签，找数据规律 | Pretraining（自监督语言建模） |
| **强化学习** | 通过奖励信号学习 | RLHF（人类反馈强化学习） |

### 2.1.3 核心术语表

```
┌────────────────────────────────────────────────────────────┐
│  特征(Feature/X): 输入变量 — 如"房间面积"用于预测房价        │
│  标签(Label/y): 输出变量 — 如"实际房价"                     │
│  模型(Model): 从X到y的映射函数 f(x) = ŷ                     │
│  损失(Loss): 预测值ŷ与真实值y的差距 L(ŷ, y)                 │
│  参数(Parameters/θ): 模型需要学习的权重                      │
│  超参数(Hyperparameters): 人工设定的配置(学习率、层数等)      │
│  训练(Training): 通过数据调整参数使Loss最小的过程             │
│  推理(Inference): 用训练好的模型对新数据做预测                │
└────────────────────────────────────────────────────────────┘
```

---

## 2.2 线性回归

### 2.2.1 核心思想

线性回归是ML中最简单的模型，但它蕴含了所有ML模型的共性：

```
目标：找到一条直线 ŷ = wx + b，使得 ŷ 尽可能接近真实的 y

         y ↑
           │    ·  (真实数据点)
           │  ·   ·
           │ ·  ·╱  ← 拟合直线 ŷ = wx + b
           │· ╱
           │ ╱
           └──────────────→ x
```

### 2.2.2 数学原理

#### 模型定义
```
ŷ = w₁x₁ + w₂x₂ + ... + wₙxₙ + b

其中：
- xᵢ: 第i个特征
- wᵢ: 第i个特征的权重（模型参数）
- b: 偏置项（bias）
```

#### 损失函数：均方误差（MSE）
```
MSE = (1/n) · Σ(ŷᵢ - yᵢ)²

为什么用平方？ → 对大误差惩罚更重，且数学上可导
```

#### 梯度下降
```
参数更新规则：
w := w - α · ∂L/∂w     (α = 学习率 learning rate)
b := b - α · ∂L/∂b

直观理解：向Loss下降最快的方向移动一小步
```

### 2.2.3 从零实现

```python
import numpy as np

class LinearRegression:
    """从零实现线性回归 — 理解ML底层原理"""
    
    def __init__(self, learning_rate: float = 0.01, epochs: int = 1000):
        self.lr = learning_rate
        self.epochs = epochs
        self.weights = None
        self.bias = None
        self.loss_history = []
    
    def fit(self, X: np.ndarray, y: np.ndarray) -> 'LinearRegression':
        """训练模型"""
        n_samples, n_features = X.shape
        
        # 初始化参数
        self.weights = np.zeros(n_features)
        self.bias = 0
        
        for epoch in range(self.epochs):
            # 前向传播：计算预测值
            y_pred = np.dot(X, self.weights) + self.bias
            
            # 计算损失
            loss = np.mean((y_pred - y) ** 2)
            self.loss_history.append(loss)
            
            # 反向传播：计算梯度
            dw = (2 / n_samples) * np.dot(X.T, (y_pred - y))
            db = (2 / n_samples) * np.sum(y_pred - y)
            
            # 更新参数
            self.weights -= self.lr * dw
            self.bias -= self.lr * db
            
            # 每100轮打印进度
            if epoch % 100 == 0:
                print(f"Epoch {epoch}: Loss = {loss:.6f}")
        
        return self
    
    def predict(self, X: np.ndarray) -> np.ndarray:
        """预测"""
        return np.dot(X, self.weights) + self.bias
```

### 2.2.4 大模型中的"线性回归"

```python
# 大模型最后一层的LM Head本质上就是一个线性层！
# Logits = Hidden_States @ W_lm_head + b_lm_head
# 其中 W_lm_head shape: (hidden_dim, vocab_size)
# 这就是从隐藏状态到词汇表概率分布的线性映射
```

---

## 2.3 决策树

### 2.3.1 核心思想

决策树通过一系列"if-else"规则来做预测，是**最可解释的ML模型**。

```
                    [是否下雨?]
                    ╱         ╲
                  是           否
                 ╱              ╲
          [有伞吗?]           [去散步]
          ╱      ╲
        有        无
       ╱           ╲
   [去散步]      [待在家]
```

### 2.3.2 关键概念

| 概念 | 说明 | 类比 |
|------|------|------|
| **根节点** | 第一个分裂特征 | LLM的输入Prompt |
| **分裂** | 按特征值分成子集 | Token-level分类 |
| **信息增益** | 分裂后纯度提升多少 | — |
| **剪枝** | 防止过拟合 | LLM的Dropout |
| **叶节点** | 最终预测 | LLM的输出Token |

### 2.3.3 Scikit-learn实战

```python
from sklearn.tree import DecisionTreeClassifier, plot_tree
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, confusion_matrix
import matplotlib.pyplot as plt

# 模拟文本分类数据（词频作为特征）
# 假设有3类文档：科技、体育、娱乐
np.random.seed(42)
n_samples = 300

# 特征：3个词的词频
X = np.random.rand(n_samples, 3) * 100
# 标签
y = np.argmax(X, axis=1)  # 哪个词出现最多就属于哪类

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)

# 训练
clf = DecisionTreeClassifier(max_depth=3, random_state=42)
clf.fit(X_train, y_train)

# 评估
y_pred = clf.predict(X_test)
print(classification_report(y_test, y_pred))

# 可视化决策树
plt.figure(figsize=(12, 8))
plot_tree(clf, feature_names=['科技词', '体育词', '娱乐词'],
          class_names=['科技', '体育', '娱乐'], filled=True)
```

---

## 2.4 模型训练核心概念

### 2.4.1 数据划分

```
原始数据 (100%)
├── 训练集 Training (70-80%)  → 用来学习模型参数
├── 验证集 Validation (10-15%) → 用来调超参数、早停
└── 测试集 Test (10-15%)      → 最终评估，只用一次！

 ⚠️ 测试集绝不能在训练过程中使用！这会导致"数据泄露"
 ⚠️ 在大模型领域，测试集泄露被称为 Benchmark Contamination
```

### 2.4.2 过拟合 vs 欠拟合

```
欠拟合 (Underfitting)          刚好 (Good Fit)              过拟合 (Overfitting)
    ·  ·                           · ·                           ·····
  ·      ·                       ·     ·                       ·     ·
·          ·                   ·         ·                   ·         ·
  ·      ·                       ·     ·                       ·     ·
    ·  ·                           · ·                           ·····
模型太简单                        刚刚好                         模型太复杂
训练误差高                        训练误差低                     训练误差极低
测试误差高                        测试误差低                     测试误差高
```

**大模型视角：**

| 现象 | 表现 | 解决 |
|------|------|------|
| 欠拟合 | 生成质量差，不连贯 | 增大模型/数据/训练步数 |
| 过拟合 | 背诵训练数据，泛化差 | Data Augmentation, Dropout, 正则化 |

### 2.4.3 常见评估指标

```python
from sklearn.metrics import (
    accuracy_score, precision_score, recall_score,
    f1_score, mean_squared_error, r2_score
)

# 分类指标
y_true = [0, 1, 0, 1, 1, 0, 1, 0]
y_pred = [0, 1, 0, 0, 1, 0, 1, 1]

print(f"Accuracy:  {accuracy_score(y_true, y_pred):.3f}")   # 整体正确率
print(f"Precision: {precision_score(y_true, y_pred):.3f}")  # 预测为正的准不准
print(f"Recall:    {recall_score(y_true, y_pred):.3f}")     # 正样本找出多少
print(f"F1:        {f1_score(y_true, y_pred):.3f}")         # P和R的调和平均

# 回归指标
print(f"MSE:  {mean_squared_error(y_true, y_pred):.3f}")
print(f"R²:   {r2_score(y_true, y_pred):.3f}")
```

### 2.4.4 交叉验证

```python
from sklearn.model_selection import cross_val_score, KFold

# 5折交叉验证
kf = KFold(n_splits=5, shuffle=True, random_state=42)
scores = cross_val_score(clf, X, y, cv=kf, scoring='accuracy')
print(f"CV Accuracy: {scores.mean():.3f} ± {scores.std():.3f}")
```

---

## 2.5 Scikit-learn实战

### 2.5.1 房价预测完整流程

```python
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LinearRegression
from sklearn.tree import DecisionTreeRegressor
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_squared_error, r2_score
import matplotlib.pyplot as plt

# ==================== 1. 数据准备 ====================
# 模拟房价数据
np.random.seed(42)
n = 1000

df = pd.DataFrame({
    'area': np.random.normal(100, 30, n),          # 面积(m²)
    'bedrooms': np.random.randint(1, 5, n),         # 卧室数
    'bathrooms': np.random.randint(1, 3, n),        # 浴室数
    'age': np.random.randint(0, 40, n),             # 房龄(年)
    'floor': np.random.randint(1, 30, n),           # 楼层
    'has_elevator': np.random.randint(0, 2, n),     # 有无电梯
    'dist_to_subway': np.random.exponential(1, n),  # 距地铁距离(km)
})

# 生成房价（基于特征的线性组合+噪声）
true_price = (
    20000 + 
    8000 * df['area'] / 100 +
    50000 * df['bedrooms'] +
    20000 * df['bathrooms'] -
    1000 * df['age'] +
    3000 * df['floor'] / 10 +
    100000 * df['has_elevator'] -
    20000 * df['dist_to_subway'] +
    np.random.normal(0, 50000, n)
)
df['price'] = true_price

# ==================== 2. 数据探索 ====================
print("数据集概况：")
print(df.describe())
print(f"\n相关性矩阵（与房价）：")
print(df.corr()['price'].sort_values(ascending=False))

# ==================== 3. 特征工程 ====================
X = df.drop('price', axis=1)
y = df['price']

# 训练/测试分割
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)

# 标准化（让不同量纲的特征可比）
scaler = StandardScaler()
X_train_scaled = scaler.fit_transform(X_train)
X_test_scaled = scaler.transform(X_test)  # 注意：用训练集的参数！

# ==================== 4. 模型训练 ====================
models = {
    'Linear Regression': LinearRegression(),
    'Decision Tree': DecisionTreeRegressor(max_depth=5, random_state=42),
    'Random Forest': RandomForestRegressor(n_estimators=100, max_depth=10, random_state=42),
}

results = {}
for name, model in models.items():
    model.fit(X_train_scaled, y_train)
    y_pred = model.predict(X_test_scaled)
    
    mse = mean_squared_error(y_test, y_pred)
    r2 = r2_score(y_test, y_pred)
    results[name] = {'MSE': mse, 'R²': r2, 'predictions': y_pred}
    
    print(f"\n{name}:")
    print(f"  MSE: {mse:,.0f}")
    print(f"  R²:  {r2:.4f}")

# ==================== 5. 可视化 ====================
fig, axes = plt.subplots(1, 3, figsize=(15, 4))

for ax, (name, res) in zip(axes, results.items()):
    ax.scatter(y_test, res['predictions'], alpha=0.5, s=10)
    ax.plot([y_test.min(), y_test.max()], [y_test.min(), y_test.max()], 'r--')
    ax.set_xlabel('真实价格')
    ax.set_ylabel('预测价格')
    ax.set_title(f'{name}\nR² = {res["R²"]:.4f}')

plt.tight_layout()
```

---

## 2.6 阶段练习

### 练习1：手写梯度下降
不用sklearn，纯NumPy实现一个能拟合简单数据的线性回归。

### 练习2：过拟合实验
在一个小数据集上，分别训练 max_depth=3 和 max_depth=20 的决策树，观察过拟合现象。

### 练习3：特征重要性分析
训练随机森林后，分析哪些特征对房价预测最重要。

---

## 2.7 常见问题

### Q1: 学ML一定要懂数学吗？

**答：** 入门阶段理解核心直觉即可，不需要手推公式。但如果你想：
- 看懂论文 → 需要线性代数+概率论
- 做模型优化 → 需要微积分+优化理论
- 调参调优 → 直觉+经验即可

**学习建议：** 先用代码跑通，再回头补数学。

### Q2: 线性回归这么简单，为什么要学？

**答：** 因为大模型最后一步就是线性变换！`Logits = Hidden @ W_vocab`。理解线性层的梯度更新，就理解了LLM训练的最后一步。

### Q3: 什么时候用决策树，什么时候用线性回归？

| 场景 | 推荐 |
|------|------|
| 特征和目标线性相关 | 线性回归 |
| 特征和目标有复杂非线性关系 | 决策树/随机森林 |
| 需要可解释性 | 决策树 |
| 需要高精度 | 随机森林/XGBoost（下一步学） |

### Q4: 这个大模型有什么关系？

**答：** 直接的ML模型在RAG中很常用：
- 文档重排序（Re-ranking）→ 用分类/回归模型打分
- 意图识别 → 用分类模型判断用户意图
- Prompt效果预测 → 用回归模型预估输出质量

---

> **✅ 阶段完成检查清单：**
> - [ ] 能手写梯度下降更新参数
> - [ ] 理解过拟合和欠拟合的区别
> - [ ] 能用sklearn完成分类和回归任务
> - [ ] 知道训练集/验证集/测试集的正确划分方式
> - [ ] 完成了3个阶段练习
>
> **下一步：** [第3步：NLP基础](../03-NLP基础/README.md)
