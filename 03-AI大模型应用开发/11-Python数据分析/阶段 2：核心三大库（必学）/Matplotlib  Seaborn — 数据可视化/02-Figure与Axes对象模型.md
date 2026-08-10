# 02-Figure 与 Axes 对象模型

> 从 pyplot 快捷模式升级到面向对象 API：Figure/Axes/Axis/Artist 四级对象、subplots 布局、figsize 与 dpi——"生产画图的标准姿势"

---

## 📚 目录

1. [为什么必须学对象模型](#1-为什么必须学对象模型)
2. [四级对象体系](#2-四级对象体系)
3. [OO API 标准姿势](#3-oo-api-标准姿势)
4. [subplots 与布局](#4-subplots-与布局)
5. [figsize 与 dpi](#5-figsize-与-dpi)
6. [pyplot → OO 万能对照表](#6-pyplot--oo-万能对照表)
7. [ax 常用方法速查](#7-ax-常用方法速查)
8. [练习 5 题（自测）](#8-练习-5-题自测)
9. [本节验收](#9-本节验收)

---

## 1. 为什么必须学对象模型

01 篇的 pyplot 是"快捷模式"——三个自动（建画布/叠加/管理）方便但**隐式**——真实项目一复杂就出问题：想画两张图但第二张覆盖了第一张、想单独改某张图的标题却改到了全局、想复用画图函数却耦合了"当前图"——**对象模型（OO API）把"自动"变成"显式"**：`fig`（画布）和 `ax`（格子）是变量，你想操作哪张就操作哪张——**"pyplot 是快速草图、OO API 是工程图"**——本体系从这篇起全部用 OO API（生产标准）。

## 2. 四级对象体系

Matplotlib 的对象层次（从上到下容器关系）：

```text
Figure（画布——最外层）
└── Axes（子图——实际绘图区，一张图可多个）
    ├── Axis（坐标轴——x 轴/y 轴：刻度、标签）
    │   ├── Tick（刻度线）
    │   └── TickLabel（刻度标签）
    └── Artist（元素——线/点/柱/文本/图例……一切画上去的）
```

**心智**：**"Figure 是纸、Axes 是格子、Axis 是格子的边、Artist 是格子里的画"**——95% 的操作发生在 Axes 上（`ax.plot/ax.set_title/ax.legend`）——**"所有 plt.xxx 都能找到对应的 ax.xxx"**（`plt.plot` → `ax.plot`、`plt.title` → `ax.set_title`——这是从 pyplot 迁移到 OO 的万能对照表）。

## 3. OO API 标准姿势

**生产标准两行开场**（本体系所有图的开头）：

```python
import matplotlib.pyplot as plt

fig, ax = plt.subplots(figsize=(8, 5))   # 建画布 + 一个格子（并指定尺寸）
ax.plot(sales, label="销量")              # 在 ax 上画——不污染"当前图"
ax.set_title("每月销量")
ax.set_xlabel("月份"); ax.set_ylabel("销量（万元）")
ax.legend()                              # 图例（label 与 legend 配对）
plt.show()  # 或 fig.savefig(...)（08 篇）
```

**OO 心智**：**`ax.xxx` 全部显式作用于这一个格子**——多图/复用函数/精细控制都不再混乱；**函数封装**（复用画图逻辑）：

```python
def plot_trend(ax, dates, values, title):
    ax.plot(dates, values)
    ax.set_title(title)

fig, axes = plt.subplots(1, 2)           # 一行两格
plot_trend(axes[0], d1, v1, "华东")
plot_trend(axes[1], d2, v2, "华南")
```

**"画图函数化"是第二阶段的能力分水岭**（同一逻辑画多张图——`06` 篇分组可视化的基础）。

**对象模型的调试价值**（为什么生产排查离不开 OO）：**报错定位**——"AxesSubplot has no attribute xxx" 说明你拿错了对象（axes 当 figure 用）；**状态可见**——`fig.axes` 列出所有格子、`ax.get_title()` 读取当前标题（pyplot 的隐式状态查无可查——报错全靠猜）；**复用与封装**——画图函数接收 ax 参数，同一逻辑任意复用（06/07 篇的分组图、多子图全靠它）——**"OO 不是风格偏好，是工程需要——排查/复用/测试都依赖显式对象"**。

**一张图的完整生命周期**（从数据到交付——本体系各篇的流水线总览）：**数据准备**（Pandas 处理——阶段 2 Pandas 篇）→ **创建**（fig, ax = plt.subplots(figsize)）→ **绘制**（ax.plot/Seaborn——03/05 篇）→ **精修**（标签/颜色/注释——04 篇）→ **检查**（规范十条——08 篇）→ **导出**（fig.savefig——08 篇）→ **归档**（output/——09 篇）——**"画图不是一个动作，是一条流水线——每篇解决流水线的一段"**（08 篇的规范十条就是"检查"一步的清单——每张图过一遍——**"流水线的每段都有对应篇目——画图是系统工程，不是一行代码"**）。

## 4. subplots 与布局

```python
fig, axes = plt.subplots(2, 2, figsize=(10, 8))   # 2×2 四个格子
axes[0, 0].plot(...)    # 左上
axes[0, 1].bar(...)     # 右上
axes[1, 0].scatter(...) # 左下
axes[1, 1].hist(...)    # 右下
```

**布局参数**（高频三件）：

```python
fig, axes = plt.subplots(
    2, 2,
    figsize=(10, 8),
    sharex=True,        # 共享 x 轴（时间序列对比必用——刻度对齐）
    sharey=True,        # 共享 y 轴（数值对比必用——比例一致不骗人）
    constrained_layout=True,   # 自动布局防重叠（现代默认推荐）
)
```

**布局心智**：**多子图 = 对比**（同样的图换分组/换指标——一眼看出差异）；**对比必须共享轴**（不共享 = 各自缩放 = 视觉误导——`../../阶段 1：前置基础/08-数据素养与伦理.md` 的双轴陷阱）；**constrained_layout 代替旧版 tight_layout**（自动防标题/标签重叠）。

## 5. figsize 与 dpi

```python
fig, ax = plt.subplots(figsize=(8, 5), dpi=100)   # 屏幕显示
fig.savefig("out.png", dpi=300)                   # 交付高清（08 篇深讲）
```

**figsize 与 dpi 的账本**：**figsize = 物理尺寸（英寸）、dpi = 每英寸点数**——**像素 = figsize × dpi**（8×5 英寸 @300dpi = 2400×1500 像素）——**"显示用小 dpi（100 够）、交付用大 dpi（300 起步）"**——屏幕看着清晰 ≠ 打印/放大清晰。

**选尺寸的心智**：**先想"图放哪"再定尺寸**——报告整页宽 8-10 英寸、半栏 4-5 英寸、PPT 按比例（16:9）——**"figsize 是排版的一部分，不是默认值"**（默认 6.4×4.8 是"图能显示"的最小配置）。

## 6. pyplot → OO 万能对照表

**从 pyplot 迁移到 OO 的对照表**（记住这张表，迁移无障碍）：

| pyplot（隐式） | OO API（显式） | 说明 |
|----------------|----------------|------|
| `plt.figure()` | `fig, ax = plt.subplots()` | 建画布+格子（标准姿势） |
| `plt.plot(x, y)` | `ax.plot(x, y)` | 画图 |
| `plt.title("t")` | `ax.set_title("t")` | 标题 |
| `plt.xlabel/ylabel` | `ax.set_xlabel/set_ylabel` | 轴标签 |
| `plt.legend()` | `ax.legend()` | 图例 |
| `plt.grid()` | `ax.grid()` | 网格 |
| `plt.xlim/ylim` | `ax.set_xlim/set_ylim` | 坐标范围 |
| `plt.xticks()` | `ax.set_xticks()` | 刻度 |
| `plt.savefig()` | `fig.savefig()` | **导出在 fig 上（不是 ax）** |

**迁移心智**：**"所有 plt.xxx 都有对应 ax.xxx（或 ax.set_xxx）——savefig 例外在 fig 上"**——写代码时从 `plt.` 换成 `ax.` 再补 `set_` 前缀就是大部分迁移——**"迁移不是重学，是换前缀"**。

## 7. ax 常用方法速查

**OO API 的日常清单**（按五类记忆——写图时按类脑内检索）：

| 分类 | 方法 | 用途 |
|------|------|------|
| 创建 | `fig, ax = plt.subplots()` | 建画布 + 格子 |
| 绘制 | `ax.plot/scatter/bar/hist/boxplot` | 五类基础图（03 篇） |
| 标签 | `ax.set_title/xlabel/ylabel` | 标题/轴标签（04 篇） |
| 装饰 | `ax.legend/grid/axhline/axvline/annotate` | 图例/网格/参考线/注释（04/07 篇） |
| 范围 | `ax.set_xlim/ylim/set_xticks` | 坐标范围/刻度 |
| 导出 | `fig.savefig` | 输出（注意：在 fig 上——08 篇） |

**速查心智**：**"绘制/标签/装饰/范围/导出五类"**——先分类再函数——写图时按类检索（"要加参考线 → 装饰类 → axhline"）——**"速查表是地图，画 20 张图后就不用了"**。

## 8. 练习 5 题（自测）

1. Figure 和 Axes 的容器关系？（谁装谁）
2. 写出生产画图的标准两行开场
3. `plt.savefig` 迁移到 OO 应该写什么？（fig 还是 ax）
4. `subplots(2, 2)` 返回什么？怎么访问四个格子？
5. 8×5 英寸 @300dpi 的图是多少像素？

**两种心智的切换**（从 pyplot 到 OO 的心理转变）：pyplot 是"**命令式**"（画一笔算一笔——`plt.plot` 就是画）；OO 是"**声明式**"（先建对象再操作——fig/ax 是"画布的地址"）——切换的关键是**"把图当成对象来思考"**：标题是 ax 的属性（`ax.set_title`）、线是 ax 的元素（`ax.plot`）、导出是 fig 的动作（`fig.savefig`）——**"OO 心智 = 图的每个部分都有'归属'——改图 = 找到归属再操作"**（报错"改错对象"时回这句）。

## 9. 本节验收

**验收动作**：用 OO API 重画 01 篇的三张图（每个都是 `fig, ax = plt.subplots()` 开场）——再做一张 `2×2` 子图（四种图各一格 + 共享 x/y 轴）——最后封装一个 `plot_series(ax, x, y, title)` 函数并调用两次——**"OO 三图 + 子图 + 函数封装 = 对象模型通过"**。

> 🎯 **核心要点**：对象模型 = 显式管理（Figure 纸/Axes 格子/Axis 边/Artist 画——95% 操作在 ax）；万能对照表（plt.xxx → ax.xxx/ax.set_xxx——迁移钥匙）；生产姿势（`fig, ax = plt.subplots()` + ax 方法 + 画图函数化）；布局（subplots 多子图 = 对比——共享轴防误导——constrained_layout）；尺寸账本（像素 = figsize × dpi——显示 100/交付 300）——**"pyplot 是草图、OO API 是工程图——生产标准从此篇开始"**。

---

**上一模块**：[01-Matplotlib基础与图形心智.md](./01-Matplotlib基础与图形心智.md) / **下一模块**：[03-图表类型与选型.md](./03-图表类型与选型.md)
