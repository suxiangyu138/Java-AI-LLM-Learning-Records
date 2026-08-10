# 06-统计函数与 axis 深潜

> 数组的"统计"：聚合函数家族、axis 折叠思维深潜、唯一值与计数、排序——"统计是分析的语言——axis 是统计的方向"

---

## 📚 目录

1. [聚合函数家族](#1-聚合函数家族)
2. [axis 折叠思维深潜](#2-axis-折叠思维深潜)
3. [缺失与统计](#3-缺失与统计)
4. [唯一值与计数](#4-唯一值与计数)
5. [排序与分位数](#5-排序与分位数)
6. [练习 5 题](#6-练习-5-题)
7. [统计函数速查与三维 axis](#7-统计函数速查与三维-axis)
8. [本节验收](#8-本节验收)

---

## 1. 聚合函数家族

**聚合 = 整组算一个（或一行/一列）**——家族速查：

```python
arr = np.array([3, 1, 4, 1, 5, 9, 2, 6])

arr.sum()          # 31 总和
arr.mean()         # 3.875 均值
arr.std() / arr.var()   # 标准差 / 方差（阶段 3 统计基础预演）
arr.min() / arr.max()   # 1 / 9
arr.argmin() / arr.argmax()  # 3 / 5 —— 极值的位置（不是值！）
arr.median()       # 3.5 中位数（np.median——不是 arr.median——注意！）
np.percentile(arr, 75)   # 5.75 分位数（阶段 1 describe 的数组版）
```

**易错点**：**`median/percentile` 是 `np.` 开头的函数**（数组方法里没有 `.median()`——`arr.median()` 报错——高频坑）；**`argmin/argmax` 返回索引**（要找值再 `arr[arr.argmax()]`——或直接用 `arr.max()`）。

**聚合心智**：**描述统计一套齐**（sum/mean/std/min/max/median/percentile——就是阶段 1 `describe()` 的数组版——`../../阶段 1：前置基础/06-数据类型与缺失值.md` 的六指标在这里逐个对应）；**聚合和掩码结合**（`arr[arr > 30].mean()` = 筛完再算——分析的最常用组合）。

## 2. axis 折叠思维深潜

**03 篇的折叠思维深潜**——axis 在聚合里的完整行为：

```python
m = np.array([[1, 2, 3],
              [4, 5, 6]])

m.sum()            # 21 —— 不写 axis = 全部折叠 = 全局
m.sum(axis=0)      # [5 7 9] —— 沿轴 0 折叠（行没了）→ 按列
m.sum(axis=1)      # [6 15] —— 沿轴 1 折叠（列没了）→ 按行
m.mean(axis=0)     # [2.5 3.5 4.5] 每列均值
m.max(axis=0)      # [4 5 6] 每列最大
m.argmax(axis=0)   # [1 1 1] 每列最大值的行位置
```

**折叠思维（不背特例）**：**"axis=k = 把第 k 维折叠掉"**——`axis=0` 折叠第 0 维（行维度消失）→ 剩下的每一列各自聚合 → 结果形状 = 去掉第 0 维；**结果形状 = 原形状去掉该维**（(2,3) 折叠 axis=0 → (3,)）——**"看结果形状就知道 axis 对不对"**——三维同理：`(2,3,4)` 折叠 axis=1 → `(2,4)`。

**面试高频题**：`m.sum(axis=0)` 和 `axis=1` 的区别——**标准答法**："axis=0 沿行方向聚合（按列算）、axis=1 沿列方向聚合（按行算）——本质是折叠对应维度"。

## 3. 缺失与统计

**nan 传染 → 用 nan* 家族**（04 篇的实践展开）：

```python
arr = np.array([1.0, np.nan, 3.0, np.nan, 5.0])

arr.mean()         # nan！（默认传播——结果也是 nan）
np.nanmean(arr)    # 3.0 忽略 nan 求均值
np.nansum(arr)     # 9.0
np.nanstd(arr)     # 忽略 nan 求标准差
np.isnan(arr).sum()  # 2 —— nan 计数（体检第一动作）
```

**缺失统计心智**：**"聚合前先 `np.isnan(arr).sum()` 数缺失"**（阶段 1 的 isna().sum() 纪律——数组版）；**nan* 家族默认忽略**（nanmean/nansum/nanstd/nanmedian...）；**筛选代替 nan***（`arr[~np.isnan(arr)].mean()`——掩码版——更灵活——05 篇）。

## 4. 唯一值与计数

**类别数据的数组统计**（阶段 1 的 value_counts 数组版）：

```python
labels = np.array(["a", "b", "a", "c", "b", "a"])

np.unique(labels)            # ['a' 'b' 'c'] 去重排序
np.unique(labels, return_counts=True)  # (array(['a','b','c']), array([3,2,1])) 计数
vals, counts = np.unique(labels, return_counts=True)
counts / counts.sum()        # 占比（[0.5 0.33 0.17]）

np.bincount(np.array([1, 0, 1, 2, 1]))   # [1 3 1] —— 整数计数（比 unique 快——限非负整数）
```

**唯一值心智**：**`unique(return_counts=True)` = 类别分布**（value_counts 的数组版——阶段 1 的 set 去重升级）；**`bincount` 是整数专用快计数**（索引 = 数值——只能非负整数——投票/标签计数场景）；**占比 = counts / sum**（一步算出——`../../阶段 1：前置基础/03-Python速成（数据分析子集）.md` 的 set 在这里升级成统计）。

## 5. 排序与分位数

```python
arr = np.array([3, 1, 4, 1, 5, 9])

np.sort(arr)             # [1 1 3 4 5 9] 排序（返回新数组——arr 不变）
arr.sort()               # 原地排序（arr 变了——无返回值！）
np.argsort(arr)          # [1 3 0 2 4 5] 排序后的位置（索引排序）
np.percentile(arr, [25, 50, 75])   # 四分位数（describe 的数组版）
np.quantile(arr, 0.5)    # 中位数（quantile = 小数版 percentile）
```

**排序心智**：**`np.sort` 返回新数组、`arr.sort()` 原地修改**（和 `sorted` vs `list.sort` 同款——阶段 1 03 篇的认知延续）；**`argsort` 返回索引**（"排完序原来在哪"——重排多个数组的配套技巧）；**分位数 = describe 的骨架**（`percentile(arr, [25,50,75])` 就是箱线图的四分位——`../Matplotlib  Seaborn — 数据可视化/06-分组与分布可视化实战.md` 的箱线五要素数据来源）。

## 6. 练习 5 题

1. `arr.median()` 会报什么错？正确写法？
2. `(3,4)` 数组 `sum(axis=0)` 的结果形状？（验证折叠思维）
3. 聚合前查缺失的第一动作是什么？
4. `np.unique(x, return_counts=True)` 返回什么？怎么算占比？
5. `np.sort` 和 `arr.sort()` 的区别？

## 7. 统计函数速查与三维 axis

**统计函数速查表**（数组统计的完整清单）：

| 函数 | 作用 | 形态 | 注意 |
|------|------|------|------|
| `arr.sum()/mean()/std()/var()` | 和/均值/标准差/方差 | 方法 | 遇 nan 传染 |
| `np.median(arr)` | 中位数 | **np. 函数** | arr.median 不存在 |
| `np.percentile(arr, p)` | 分位数 | np. 函数 | p 可传列表 |
| `np.min/max` 与 `arr.min/max` | 极值 | 都可 | 两个形态都有 |
| `arr.argmin/argmax()` | 极值位置 | 方法 | 返回索引 |
| `np.unique(arr, return_counts)` | 唯一值+计数 | np. 函数 | 类别统计 |
| `np.nanmean/nansum/nanstd` | 忽略 nan 版 | np. 函数 | 缺失统计主力 |
| `np.bincount(整数数组)` | 整数快计数 | np. 函数 | 限非负整数 |

**三维 axis 示例**（折叠思维在三维的验证——背特例必死、折叠思维通吃）：

```python
a3 = np.arange(24).reshape(2, 3, 4)    # 形状 (2, 3, 4)
a3.sum(axis=0)      # 形状 (3, 4) —— 折叠第 0 维（2 没了）
a3.sum(axis=1)      # 形状 (2, 4) —— 折叠第 1 维（3 没了）
a3.sum(axis=2)      # 形状 (2, 3) —— 折叠第 2 维（4 没了）
```

**速查心智**：**"结果形状 = 原形状去掉 axis 那维"**——三维验证后，任何维度的 axis 都能推理（不用背）；**"方法形态（arr.xxx）只有 sum/mean/std/min/max/argmax——其余用 np.xxx"**（median/percentile/unique/nan 家族都是 np. 函数——记这一条就不踩 `arr.median()` 的坑）。

**统计与可视化的对接**（统计的下一步是画出来——`../Matplotlib  Seaborn — 数据可视化/` 姊妹篇）：**分位数喂箱线图**（`np.percentile(arr, [25, 50, 75])` 就是箱线图的四分位数据——`../Matplotlib  Seaborn — 数据可视化/06-分组与分布可视化实战.md` 的箱线五要素）；**分布喂直方**（`plt.hist(arr, bins=30)` 直接吃数组——`np.histogram` 是它的纯 NumPy 版）；**均值对比喂分组柱状**（groupby 算的均值 → barplot——可视化篇的"分组 → 图 → 结论"三段式）——**"统计是分析的语言、图是统计的呈现——NumPy 算出来、Matplotlib 画出来"**——09 篇实战的第五步就是三库协作的第一次完整演练。

**面试题**："m.sum(axis=0) 和 axis=1 的区别？"——标准答法：**"axis 是折叠的轴——axis=0 沿第 0 维（行方向）聚合——折叠行、剩下列——所以是按列求和；axis=1 沿第 1 维（列方向）聚合——折叠列、剩下行——是按行求和——结果形状 = 原形状去掉该维度"**——"补三维例子（(2,3,4) 折叠 axis=1 → (2,4)）显示真正理解而非背特例"——**"折叠思维 + 结果形状验证 = 面试满分答"**（背"0 是列"的答法在三维题前会崩）。

## 8. 本节验收

**验收动作**：① 造 1000 个正态随机数（`np.random.normal(50, 10, 1000)`——阶段 3 预演）算全套统计（sum/mean/std/median/percentile 25-50-75/argmax）；② 2×3 数组做 axis=0/1 聚合并打印结果形状（验证折叠）；③ `np.unique` 统计类别占比；④ 统计前先 `isnan().sum()` 体检——**"全套统计 + axis 形状验证 = 统计层通过"**——**练习纪律**：axis 题打印结果形状验证——**"形状对 = axis 对"**——统计题用 `np.random` 造数据自测（随机数据是免费的练习场——`seed` 固定——08 篇纪律）；median 题先预测再跑——验证"median 是 np. 函数"的认知（预测错 = 坑没记住）。

> 🎯 **核心要点**：聚合家族（sum/mean/std/median/percentile/argmax——**median 是 np. 函数不是方法**）；axis = 折叠思维（axis=k 折叠第 k 维——结果形状去掉该维——看形状验对错）；缺失统计（nan* 家族默认忽略——聚合前先数 nan）；唯一值与计数（unique(return_counts)——bincount 整数快计数）；排序（np.sort 新数组/arr.sort 原地/argsort 索引）——**"统计是分析的语言——axis 是统计的方向"**。

---

**上一模块**：[05-布尔掩码与条件选择.md](./05-布尔掩码与条件选择.md) / **下一模块**：[07-数组操作与变形.md](./07-数组操作与变形.md)
