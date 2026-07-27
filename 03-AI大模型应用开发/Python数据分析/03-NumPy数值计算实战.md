# 03 - NumPy 数值计算实战

> 🎯 NumPy 是 Python 数据科学的基石——没有它就没有 Pandas、scikit-learn、TensorFlow。理解 ndarray 的广播、切片、向量化操作，是写出高效数据分析代码的前提

---

## 目录

1. [ndarray 核心操作](#1-ndarray-核心操作)
2. [广播机制详解](#2-广播机制详解)
3. [向量化 vs 循环](#3-向量化-vs-循环)
4. [常用数学与统计函数](#4-常用数学与统计函数)
5. [实战：股票数据分析](#5-实战股票数据分析)

---

## 1. ndarray 核心操作

### 1.1 创建数组

```python
import numpy as np

# 从列表创建
a = np.array([1, 2, 3, 4, 5])           # 一维
b = np.array([[1, 2], [3, 4], [5, 6]])  # 二维 (3×2)

# 快捷创建
np.zeros((3, 4))        # 全 0 矩阵 (3×4)
np.ones((2, 3))         # 全 1 矩阵
np.eye(3)               # 单位矩阵 (3×3)
np.arange(0, 10, 2)     # [0, 2, 4, 6, 8]
np.linspace(0, 1, 5)    # [0, 0.25, 0.5, 0.75, 1]
np.random.randn(3, 4)   # 正态分布随机数
```

### 1.2 属性与索引

```python
arr = np.arange(24).reshape(4, 6)  # 4×6 矩阵

arr.shape      # (4, 6)
arr.ndim       # 2（维度数）
arr.size       # 24（元素总数）
arr.dtype      # int64

# 索引与切片
arr[0]         # 第一行
arr[:, 1]      # 第二列
arr[1:3, 2:5]  # 行 1-2, 列 2-4
arr[[0, 2]]    # 第 0 和 2 行（花式索引）

# 布尔索引
arr[arr > 10]  # 所有 > 10 的元素
arr[(arr > 5) & (arr < 15)]  # 复合条件
```

## 2. 广播机制详解

### 2.1 广播规则

```python
# 广播 = 不同形状的数组进行算术运算时，自动扩展维度

# 规则：从后往前对齐维度
# 1. 维度相同 → 直接运算
# 2. 某维度为 1 → 扩展为该维度大小
# 3. 维度不同且不为 1 → 报错

# 示例（形状转换）
a = np.array([[1, 2, 3],    # (3, 3)
              [4, 5, 6],
              [7, 8, 9]])

b = np.array([10, 20, 30])   # (3,) → 广播为 (1, 3) → (3, 3)

result = a + b
# [[11, 22, 33],
#  [14, 25, 36],
#  [17, 28, 39]]

c = np.array([[10],          # (3, 1) → 广播为 (3, 3)
              [20],
              [30]])

result = a + c
# [[11, 12, 13],
#  [24, 25, 26],
#  [37, 38, 39]]
```

### 2.2 常见广播场景

```python
# 归一化（每列减均值除标准差）
data = np.random.randn(100, 5)
normalized = (data - data.mean(axis=0)) / data.std(axis=0)

# 矩阵每行加不同值
matrix = np.ones((5, 3))
row_add = np.array([1, 2, 3, 4, 5]).reshape(-1, 1)  # (5,1)
result = matrix + row_add  # 每行加不同值
```

## 3. 向量化 vs 循环

```python
import time

# 对比：计算 100 万个数的平方
arr = np.random.randn(1_000_000)

# ❌ Python 循环（慢）
start = time.time()
result_loop = [x**2 for x in arr]
print(f"循环: {time.time() - start:.4f}s")  # ~0.2s

# ✅ NumPy 向量化（快）
start = time.time()
result_vec = arr ** 2
print(f"向量化: {time.time() - start:.4f}s")  # ~0.002s → 快 100x

# 任何能用 NumPy 向量化操作的，不要用循环！
```

## 4. 常用数学与统计函数

```python
arr = np.array([[1, 2, 3], [4, 5, 6]])

# 聚合
arr.sum()          # 21（全局）
arr.sum(axis=0)    # [5, 7, 9]（每列）
arr.sum(axis=1)    # [6, 15]（每行）
arr.mean()         # 平均值 → 3.5
arr.std()          # 标准差
arr.min() / max()  # 最值
arr.argmax()       # 最大值的索引

# 数学运算
np.sqrt(arr)       # 开方
np.log(arr)        # 对数
np.exp(arr)        # 指数
np.dot(a, b)       # 矩阵乘法（推荐用 a @ b）

# 统计函数
np.percentile(arr, 50)   # 中位数
np.quantile(arr, [0.25, 0.5, 0.75])  # 四分位数
np.corrcoef(x, y)        # 相关系数矩阵
np.histogram(arr, bins=10)  # 直方图

# 条件
np.where(arr > 3, 1, 0)  # >3 变 1，否则 0
np.clip(arr, 0, 5)       # 裁剪到 [0, 5]
np.unique(arr)           # 去重排序
```

## 5. 实战：股票数据分析

```python
# 模拟 1 年的股票日数据
np.random.seed(42)
dates = 252  # 交易日

# 生成股价（随机游走）
returns = np.random.randn(dates) * 0.02  # 日收益率
price = 100 * np.exp(np.cumsum(returns))  # 累积收益 → 股价

# 分析
print(f"起始价: ${price[0]:.2f}")
print(f"最终价: ${price[-1]:.2f}")
print(f"最高:   ${price.max():.2f}")
print(f"最低:   ${price.min():.2f}")
print(f"日均收益率: {returns.mean():.4%}")
print(f"日波动率:   {returns.std():.4%}")

# 技术指标
ma_20 = np.convolve(price, np.ones(20)/20, mode='valid')  # 20日均线
ma_60 = np.convolve(price, np.ones(60)/60, mode='valid')  # 60日均线

# 最大回撤
peak = np.maximum.accumulate(price)
drawdown = (price - peak) / peak
max_drawdown = drawdown.min()
print(f"最大回撤: {max_drawdown:.2%}")

# 夏普比率（无风险利率 3%）
risk_free = 0.03 / 252
sharpe = (returns.mean() - risk_free) / returns.std() * np.sqrt(252)
print(f"夏普比率: {sharpe:.2f}")
```

## 核心要点回顾

- ndarray = NumPy 一切的核心，3 个关键属性：shape / ndim / dtype
- 广播：不同形状的数组自动对齐维度运算，是 NumPy 最强大的特性
- 向量化 > 循环：100 倍性能差距是常态
- `axis=0` 沿行方向（操作列），`axis=1` 沿列方向（操作行）
- `np.where` / `np.clip` / `np.unique` 是最常用的条件函数

## 参考资料

1. NumPy 官方文档 — numpy.org/doc
2. NumPy 100 题练习 — github.com/rougier/numpy-100
