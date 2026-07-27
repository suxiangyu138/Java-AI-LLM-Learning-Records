# 05 - Matplotlib 与 Seaborn 可视化

> 🎯 一张好图胜过千行数据。Matplotlib 是 Python 绘图的"画布"，Seaborn 是它的"颜值插件"。掌握 5 种核心图表，覆盖 90% 的分析场景

---

## 目录

1. [Matplotlib 基础架构](#1-matplotlib-基础架构)
2. [五种核心图表](#2-五种核心图表)
3. [Seaborn 统计绑图](#3-seaborn-统计绑图)
4. [样式定制与中文支持](#4-样式定制与中文支持)
5. [多子图布局](#5-多子图布局)

---

## 1. Matplotlib 基础架构

```python
import matplotlib.pyplot as plt
import numpy as np

# Matplotlib 两层 API：
# 1. pyplot（快速绘图，类似 MATLAB）→ 日常使用
# 2. Object-Oriented（精细控制）→ 复杂图表

# ---- OO 风格（推荐） ----
fig, ax = plt.subplots(figsize=(10, 6))
ax.plot(x, y, color="blue", linewidth=2, label="Sales")
ax.set_title("Sales Trend", fontsize=14)
ax.set_xlabel("Month")
ax.set_ylabel("Revenue ($)")
ax.legend()
ax.grid(True, alpha=0.3)
plt.tight_layout()
plt.show()
```

## 2. 五种核心图表

### 2.1 折线图 (Line)

```python
# 趋势分析首选
x = np.arange(12)
y1 = np.cumsum(np.random.randn(12)) + 100

fig, ax = plt.subplots(figsize=(10, 5))
ax.plot(x, y1, "b-o", label="Revenue", markersize=6)
ax.fill_between(x, y1 - 10, y1 + 10, alpha=0.2, color="blue")
ax.set_title("Monthly Revenue Trend")
ax.legend()
```

### 2.2 柱状图 (Bar)

```python
# 分类对比
categories = ["电子", "服装", "食品", "家居", "运动"]
values = [450, 320, 280, 200, 150]

fig, ax = plt.subplots(figsize=(10, 5))
bars = ax.bar(categories, values, color=["#1f77b4", "#ff7f0e", "#2ca02c"])
ax.bar_label(bars, fmt="¥%d")   # 柱顶标签
ax.set_title("Sales by Category")
ax.set_ylabel("Revenue (¥10K)")

# 堆叠柱状图
q1 = [200, 150, 100]
q2 = [250, 170, 180]
ax.bar(cat, q1, label="Q1")
ax.bar(cat, q2, bottom=q1, label="Q2")  # ← bottom 参数实现堆叠
```

### 2.3 散点图 (Scatter)

```python
# 相关性分析
x = np.random.randn(500)
y = x * 0.7 + np.random.randn(500) * 0.3

fig, ax = plt.subplots(figsize=(8, 8))
ax.scatter(x, y, alpha=0.6, c="steelblue", edgecolors="white", s=50)

# 趋势线
z = np.polyfit(x, y, 1)
ax.plot(x, np.polyval(z, x), "r--", linewidth=2, label=f"y={z[0]:.2f}x+{z[1]:.2f}")
ax.set_title(f"Correlation: r={np.corrcoef(x, y)[0,1]:.3f}")
```

### 2.4 饼图 (Pie)

```python
sizes = [35, 25, 20, 15, 5]
labels = ["华东", "华南", "华北", "西部", "海外"]

fig, ax = plt.subplots(figsize=(8, 8))
wedges, texts, autotexts = ax.pie(
    sizes, labels=labels, autopct="%1.1f%%",
    startangle=90, explode=(0, 0.1, 0, 0, 0),
    colors=plt.cm.Set3.colors
)
ax.set_title("Sales by Region")
```

### 2.5 热力图 (Heatmap)

```python
# 相关性矩阵热力图
corr = df.corr(numeric_only=True)

fig, ax = plt.subplots(figsize=(10, 8))
im = ax.imshow(corr, cmap="RdYlBu", vmin=-1, vmax=1)

# 标注
for i in range(len(corr.columns)):
    for j in range(len(corr.columns)):
        ax.text(j, i, f"{corr.iloc[i, j]:.2f}",
                ha="center", va="center",
                color="white" if abs(corr.iloc[i, j]) > 0.5 else "black")

ax.set_xticks(range(len(corr.columns)))
ax.set_yticks(range(len(corr.columns)))
ax.set_xticklabels(corr.columns, rotation=45)
ax.set_yticklabels(corr.columns)
plt.colorbar(im)
```

## 3. Seaborn 统计绑图

```python
import seaborn as sns

# Seaborn = Matplotlib 的高级封装，默认样式更好看

# 1. 箱线图（分布+异常值）
sns.boxplot(data=df, x="category", y="amount")
sns.stripplot(data=df, x="category", y="amount", color="black", alpha=0.3)

# 2. 小提琴图（分布形状）
sns.violinplot(data=df, x="category", y="amount")

# 3. 双变量分布
sns.jointplot(data=df, x="price", y="amount", kind="hex")

# 4. 成对关系矩阵
sns.pairplot(df[["price", "amount", "quantity"]], diag_kind="kde")

# 5. 分类聚合图
sns.barplot(data=df, x="category", y="amount", hue="region")
sns.countplot(data=df, x="category")

# 6. 回归图（带拟合线）
sns.regplot(data=df, x="price", y="amount", scatter_kws={"alpha": 0.3})
```

## 4. 样式定制与中文支持

```python
# 中文支持（Windows）
plt.rcParams["font.sans-serif"] = ["SimHei", "Microsoft YaHei"]
plt.rcParams["axes.unicode_minus"] = False   # 解决负号显示

# 全局样式
plt.style.use("seaborn-v0_8-darkgrid")       # 推荐
# "ggplot" | "fivethirtyeight" | "bmh" | "dark_background"

# 配色方案
colors = sns.color_palette("husl", 8)         # 生成 8 色
colors = plt.cm.viridis(np.linspace(0, 1, 8)) # colormap

# 保存
fig.savefig("chart.png", dpi=150, bbox_inches="tight", transparent=False)
```

## 5. 多子图布局

```python
# plt.subplots：网格布局
fig, axes = plt.subplots(2, 3, figsize=(15, 8))

axes[0, 0].plot(x, y1)     # 第 1 行第 1 列
axes[0, 1].bar(x, y2)      # 第 1 行第 2 列
axes[1, 0].scatter(x, y3)  # 第 2 行第 1 列
# ...

# 隐藏空白子图
for ax in axes.flat[5:]:
    ax.set_visible(False)

plt.tight_layout()

# GridSpec：不等分布局
from matplotlib.gridspec import GridSpec
fig = plt.figure(figsize=(12, 8))
gs = GridSpec(3, 3)
ax1 = fig.add_subplot(gs[0, :])     # 第 1 行占满
ax2 = fig.add_subplot(gs[1:, 0])    # 左侧占两行
ax3 = fig.add_subplot(gs[1:, 1:])   # 右侧占两行两列
```

## 核心要点回顾

- Matplotlib OO 风格（fig, ax）> pyplot 风格
- 五种图表对应五种场景：趋势→线、对比→柱、关系→散点、占比→饼、矩阵→热力
- Seaborn 一行代码 = Matplotlib 十行，统计绑图首选
- 中文：`SimHei` + `axes.unicode_minus=False`
- 保存：`dpi=150` + `bbox_inches="tight"`

## 参考资料

1. Matplotlib 官方文档 — matplotlib.org
2. Seaborn 官方文档 — seaborn.pydata.org
3. Python Graph Gallery — python-graph-gallery.com
