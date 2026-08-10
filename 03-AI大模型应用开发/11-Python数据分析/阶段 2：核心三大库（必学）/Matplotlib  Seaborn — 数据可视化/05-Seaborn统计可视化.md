# 05-Seaborn 统计可视化

> 统计可视化的高层 API：Seaborn 定位、set_theme 统一风格、三大图族（relplot/displot/catplot）、hue 分面语义——"一条命令出统计图，画布还是 Matplotlib 的"

---

## 📚 目录

1. [Seaborn 定位](#1-seaborn-定位)
2. [set_theme 统一风格](#2-set_theme-统一风格)
3. [三大图族](#3-三大图族)
4. [hue 分组语义](#4-hue-分组语义)
5. [与 Matplotlib 混用](#5-与-matplotlib-混用)
6. [三大图族函数速查](#6-三大图族函数速查)
7. [练习 5 题（自测）](#7-练习-5-题自测)
8. [本节验收](#8-本节验收)

---

## 1. Seaborn 定位

**Seaborn = 建立在 Matplotlib 之上的统计可视化库**——**"画布还是 Matplotlib 的，但画图命令是统计级的"**——它的价值：**一行代码出统计图**（分布/回归/分组对比——这些用 Matplotlib 要写 10 行）——**"Matplotlib 给积木、Seaborn 给成品"**。2026-08 基线：**Seaborn 0.13.2**（0.13 系列稳定；`set_style/set_palette` 已废弃——统一用 `set_theme`）。

**Seaborn 的三个"自动"**：自动处理统计（密度估计/回归拟合/误差线）、自动配色（hue 自动分配语义色）、自动图例——**"输入 DataFrame + 列名，输出统计图"**——**Seaborn 的 API 是"数据驱动的"**（传 `data=df, x=列名, y=列名`——和 Pandas 的列名思维一致——`../../阶段 1：前置基础/04-数据容器与表格思维.md`）。

## 2. set_theme 统一风格

```python
import seaborn as sns

sns.set_theme()                       # 0.13 起统一入口（替代 set_style/set_palette）
sns.set_theme(style="whitegrid", palette="colorblind", font="Microsoft YaHei")
# style: white/whitegrid/dark/darkgrid/ticks
# palette: deep/muted/colorblind/Set2/...（colorblind = 色盲安全）
```

**set_theme 纪律**：**notebook 开头设一次**（全局统一——和 `%matplotlib inline`、中文配置放在一起——04 篇）；**`set_theme` 会覆盖 Matplotlib 的 style**（先设 Matplotlib 再设 sns 的顺序有讲究——**sns 后设生效**）；**palette 选 colorblind 起步**（色盲安全默认化——`../../阶段 1：前置基础/08-数据素养与伦理.md` 的颜色纪律）。

## 3. 三大图族

**Seaborn 的 API 按"问题"组织为三大族**（每个族一个总入口 + 子图函数）：

| 图族 | 回答的问题 | 总入口 | 子图（kind） | 底层图 |
|------|-----------|--------|-------------|--------|
| 关系 relplot | 变量间什么关系 | `relplot` | scatter/line | 散点/折线 |
| 分布 displot | 变量怎么分布 | `displot` | hist/kde/ecdf | 直方/密度 |
| 分类 catplot | 类别间怎么对比 | `catplot` | box/violin/bar/count | 箱线/柱状 |

```python
# 关系：票价 × 年龄，按幸存分组
sns.scatterplot(data=df, x="Age", y="Fare", hue="Survived")

# 分布：年龄分布 + 密度
sns.histplot(data=df, x="Age", kde=True, bins=30)

# 分类：舱位 × 票价箱线，按性别分面
sns.boxplot(data=df, x="Pclass", y="Fare", hue="Sex")
```

**三大族心智**：**"关系/分布/分类 = 数据的三种基本问题"**（和 03 篇选型决策树同构——Seaborn 把决策树变成了 API 命名）——**"先定图族再定 kind"**（`relplot(kind="line")` / `displot(kind="kde")`——一条命令换图型）。

## 4. hue 分组语义

**hue = 用颜色表达第三个维度**（Seaborn 的核心武器）：

```python
sns.scatterplot(data=df, x="Age", y="Fare", hue="Sex")        # 颜色 = 性别
sns.barplot(data=df, x="Pclass", y="Survived", hue="Sex")     # 分组柱状
sns.boxplot(data=df, x="Pclass", y="Fare", hue="Survived")    # 分组箱线
# 分面：col/row 用子图表达维度
sns.relplot(data=df, x="Age", y="Fare", col="Pclass", row="Sex", hue="Survived")
```

**hue 心智**：**hue/col/row = 三个"分组维度"的三种表达**——hue（颜色——同图对比）、col（列方向子图）、row（行方向子图）——**"一图最多 hue + col + row 三个分组维度"**（再多的分组信息塞不进图——换表/换分析）；**hue 的顺序语义**：类别 hue（性别/舱位）用区分色、连续 hue（数值——如票价）用渐变色（`hue=df["Fare"]` 自动渐变）。

**hue 与 06 篇的衔接**：**hue 分组 = groupby 的可视化**——`df.groupby("Sex")["Survived"].mean()` 的结论用 `sns.barplot(hue="Sex")` 画出来——**"分组分析 + hue 图 = 结论有证据"**（06 篇实战）。

## 5. 与 Matplotlib 混用

**Seaborn 出图后，Matplotlib 精修**（两库混用是生产姿势）：

```python
fig, ax = plt.subplots(figsize=(8, 5))          # 1. 先建 Matplotlib 画布
sns.boxplot(data=df, x="Pclass", y="Fare", ax=ax)   # 2. Seaborn 画在指定 ax
ax.set_title("各舱位票价分布——头等舱显著更高")        # 3. Matplotlib 精修（标题写结论）
ax.axhline(df["Fare"].median(), color="gray", ls="--", label="全体中位数")  # 4. 参考线/注释
ax.legend()
fig.savefig("fare_box.png", dpi=300)            # 5. Matplotlib 导出（08 篇）
```

**混用纪律**：**Seaborn 函数都接受 `ax=` 参数**（指定画布——不指定会画到"当前图"）；**精修（标题/注释/参考线）用 Matplotlib**；**导出用 fig.savefig**（08 篇——Seaborn 没有独立的导出 API）——**"Seaborn 画、Matplotlib 修、savefig 交"**——**混用的边界**：统计计算交给 Seaborn（误差线/密度/回归——它自动算）、排版美学交给 Matplotlib（标题/注释/参考线——它控制得细）——**"谁擅长什么谁干——别用 Seaborn 写标题、别用 Matplotlib 手搓密度图"**。

**Seaborn 的数据要求**：**必须传 DataFrame + 列名**（`data=df, x="Age"`——不能传裸列表——这是与 Matplotlib 最大的 API 差异）；**列名即语义**（`hue="Sex"` 直接读列——列名规范就重要了——`../../阶段 1：前置基础/04-数据容器与表格思维.md` 的列名规范）；**长表友好**（tidy data——`../../阶段 1：前置基础/04-数据容器与表格思维.md` 的长表优先——Seaborn 的分组全基于长表的列）——**"给 Seaborn 喂长表，它给你统计图"**——宽表先 melt 再画（阶段 2 Pandas 篇）。

**为什么统计图重要**（Seaborn 存在的原因）：**误差线**（barplot 默认带置信区间——数据量小的组误差大——自动提示可信度）；**密度估计**（kde——直方看不到的分布形状平滑呈现）；**回归拟合**（regplot 一条命令画趋势线 + 置信带）——**"统计图自带'不确定性'的表达——这是 Matplotlib 裸图没有的"**——`../../阶段 1：前置基础/08-数据素养与伦理.md` 的素养课：统计图帮你诚实（误差线 = 数据量的诚实）。

## 6. 三大图族函数速查

**三大族的完整函数地图**（总入口 + kind + 高频子函数）：

| 图族 | 总入口 kind | 高频子函数 | 一句话 |
|------|------------|-----------|--------|
| 关系 | `relplot(kind="scatter"/"line")` | `scatterplot` / `lineplot` / `regplot`（带回归线） | 两变量关系 |
| 分布 | `displot(kind="hist"/"kde"/"ecdf")` | `histplot` / `kdeplot` / `ecdfplot` | 变量分布 |
| 分类 | `catplot(kind="box"/"violin"/"bar"/"count"/"point")` | `boxplot` / `violinplot` / `barplot` / `countplot` | 类别对比 |

**高频组合**（生产最常见的五个）：

```python
sns.scatterplot(data=df, x="Age", y="Fare", hue="Survived")   # 关系+分组
sns.regplot(data=df, x="Age", y="Fare")                        # 关系+回归线
sns.histplot(data=df, x="Age", kde=True)                       # 分布+密度
sns.kdeplot(data=df, x="Fare", hue="Survived", fill=True)      # 分组密度对比
sns.boxplot(data=df, x="Pclass", y="Fare", hue="Sex")          # 分类+分组
```

**速查心智**：**"记总入口 + kind、用子函数写代码"**（`sns.catplot(kind="box")` 与 `sns.boxplot` 等价——总入口适合分面（col/row）、子函数适合单图）——**"先选族（问题）再选 kind（图型）"**。

## 7. 练习 5 题（自测）

1. 为什么说"Seaborn 的画布还是 Matplotlib 的"？
2. `set_theme` 和废弃的 `set_style` 什么关系？
3. 关系/分布/分类三大族各回答什么问题？
4. hue 和 col 表达分组维度的区别？
5. 写出混用五步（建画布/画/精修/参考线/导出）？

**Seaborn 与 Pandas 的配合**（数据分析铁三角的日常）：**Pandas 算、Seaborn 画**——`df.groupby("Sex")["Survived"].mean()` 算完的数字，画图时直接 `sns.barplot(data=df, x="Sex", y="Survived")`（**不传算好的数字——Seaborn 自己会算**——`estimator` 参数控制聚合方式：mean/median/sum）；**宽表先 melt**（`df.melt()`——阶段 2 Pandas 篇——长表才能 hue 分组）；**Pandas 自带绘图**（`df.plot()`——是 Seaborn 的轻量入口——简单探索够用）——**"Pandas 管数据形态、Seaborn 管图形态——数据形态对了图就对了"**。

**一句话总结**：Seaborn = 统计图的成品库（三大族 + hue 分组 + col/row 分面）、画布是 Matplotlib 的（混用姿势：sns 画 + plt 修 + savefig 交）、数据要长表（`data=df` + 列名）——**"给 Seaborn 喂长表，它给你统计图——统计图自带不确定性表达"**。

## 8. 本节验收

**验收动作**：用泰坦尼克数据画六张 Seaborn 图——① `histplot` 年龄 + kde；② `scatterplot` 年龄 × 票价 hue 幸存；③ `boxplot` 舱位 × 票价；④ `barplot` 舱位 × 幸存率 hue 性别；⑤ `relplot` 分面（col=舱位）；⑥ 其中一张做完整混用（ax 指定 + 标题结论 + 参考线 + savefig）——**"六图 + 一图混用 = Seaborn 通过"**。

> 🎯 **核心要点**：Seaborn = 统计可视化的成品库（画布还是 Matplotlib——一行出统计图）；set_theme 统一风格（0.13 起替代 set_style/set_palette——colorblind 起步）；三大图族 = 三种问题（relplot 关系/displot 分布/catplot 分类——先定族再 kind）；hue/col/row 三表达（颜色/列/行——最多三维）；混用姿势（sns 画 + plt 修 + savefig 交）——**"Seaborn 画、Matplotlib 修、savefig 交——一条命令出统计图"**。

---

**上一模块**：[04-颜色样式与标签.md](./04-颜色样式与标签.md) / **下一模块**：[06-分组与分布可视化实战.md](./06-分组与分布可视化实战.md)
