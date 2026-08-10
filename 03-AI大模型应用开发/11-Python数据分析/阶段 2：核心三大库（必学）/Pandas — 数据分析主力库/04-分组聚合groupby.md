# 04-分组聚合 groupby

> 表的"分组"：分组三件套（groupby → agg → reset_index）、多函数聚合、透视表 pivot_table——"分组是分析的核心动作——'发生了什么'全靠分组回答"

---

## 📚 目录

1. [分组三件套](#1-分组三件套)
2. [agg 多函数聚合](#2-agg-多函数聚合)
3. [分组后处理](#3-分组后处理)
4. [pivot_table 透视表](#4-pivot_table-透视表)
5. [value_counts 与分组](#5-value_counts-与分组)
6. [练习 5 题](#6-练习-5-题)
7. [分组模式速查与面试题](#7-分组模式速查与面试题)
8. [本节验收](#8-本节验收)

---

## 1. 分组三件套

**groupby = split-apply-combine**（拆开 → 各自算 → 拼回）——Pandas 分组的标准动作是**三件套**：

```python
# 三件套：groupby → agg（或直接方法）→ reset_index
surv_rate = (
    df.groupby("Sex")                      # 1. 按性别分组（拆）
    ["Survived"].mean()                    # 2. 每组算均值（算）
    .reset_index()                         # 3. 索引变列（拼——整理成表）
)
#    Sex  Survived
# 0  female  0.742038
# 1    male  0.188908
```

**三件套心智**：**"分组三件套 = 拆 → 算 → 拼"**——**第 3 步 reset_index 是新手最大坎**（不拼的话结果是带层级索引的 Series——打印怪、后续操作难——**"groupby 结果第一件事：reset_index"**）；**多列分组**（`groupby(["Sex", "Pclass"])`——两级分组）；**选列**（`groupby("Sex")["Survived"]`——只对目标列算）。

## 2. agg 多函数聚合

```python
# 多个函数同时算（agg = aggregate）
df.groupby("Pclass")["Fare"].agg(["mean", "median", "max", "count"])
#         mean  median    max  count
# Pclass
# 1      84.15   60.28  512.33    216
# 2      20.66   15.74   73.50    184
# 3      13.68    8.05   69.55    491

# 不同列不同函数（字典——高级）
df.groupby("Sex").agg({"age": "mean", "fare": ["sum", "max"], "survived": "mean"})

# 命名聚合（Pandas 新写法——列名清晰）
df.groupby("Sex").agg(avg_age=("age", "mean"), surv_rate=("survived", "mean"))
```

**agg 心智**：**"一个分组多指标 = agg 列表"**（`["mean", "median", ...]`——比逐个算快且整齐）；**字典式 agg 不同列不同函数**（"age 算均值、fare 算总和"）；**命名聚合是推荐姿势**（`avg_age=("age", "mean")`——列名直接可读——`../../阶段 1：前置基础/04-数据容器与表格思维.md` 的列名规范在聚合结果上延续）。

## 3. 分组后处理

```python
# 分组排序（找每组最大的——高频！）
df.groupby("Sex")["Fare"].max().reset_index()

# 分组后取 Top N（nlargest——进阶）
df.groupby("Pclass")["Fare"].nlargest(3)

# 分组转换（transform——保持原行数——中级）
df["fare_diff"] = df["fare"] - df.groupby("Pclass")["fare"].transform("mean")
# 每行票价 - 所在舱位均值（保留原行数——和 agg 不同！）

# 分组后的多列操作
df.groupby("Sex")[["age", "fare"]].mean()    # 多列同函数
```

**处理心智**：**agg 压缩行数、transform 保持行数**（agg 每组一行、transform 每行一个值——**"要"每组一个数"用 agg、要"每行一个偏差"用 transform"**——两个家族的第一次区分）；**nlargest 找每组 Top N**（面试高频——"每组前三名"）；**分组 + 算术**（每行减组均值 = transform 的经典场景——标准化预演）。

## 4. pivot_table 透视表

**透视表 = 二维分组（行 × 列 × 值）**：

```python
pivot = df.pivot_table(
    index="Pclass",            # 行方向分组
    columns="Sex",             # 列方向分组
    values="Survived",         # 要算的列
    aggfunc="mean",            # 聚合函数
    fill_value=0,              # 空值填充（可选）
)
# Sex    female     male
# Pclass
# 1      0.968085  0.368853
# 2      0.921053  0.157407
# 3      0.500000  0.135447

# 多指标：aggfunc 传列表
df.pivot_table(index="Pclass", values=["Fare", "Survived"], aggfunc=["mean", "max"])
```

**透视表心智**：**pivot_table = groupby 的二维版**（index 行、columns 列、values 值、aggfunc 算法——**"一行一列一格"**）；**和 Excel 数据透视表同构**（Excel 里拖过的操作——`../../阶段 1：前置基础/04-数据容器与表格思维.md` 的迁移心法）；**热力图的原料**（`../Matplotlib  Seaborn — 数据可视化/06-分组与分布可视化实战.md` 的 survivorship 热力就是 pivot_table 的结果——**"透视表算出来、热力图画出来"**）。

## 5. value_counts 与分组

```python
df["Sex"].value_counts()                   # 单列计数（类别分布）
df["Sex"].value_counts(normalize=True)     # 占比（0.35 / 0.65）
df["Sex"].value_counts().plot(kind="bar")  # 计数直接画图（Pandas 自带绘图）

# 多列交叉计数（分组计数的快捷版）
df.groupby(["Sex", "Pclass"]).size()       # 组合计数
pd.crosstab(df["Sex"], df["Pclass"])       # 交叉表（二维计数——透视表近亲）
```

**计数心智**：**`value_counts` = 类别分布的第一眼**（阶段 1 的需求在这里正式满足——占比用 normalize）；**`crosstab` = 二维计数**（"性别 × 舱位多少人"——透视表的计数版）；**`value_counts().plot()` 是 Pandas 自带绘图**（快速探索——`../Matplotlib  Seaborn — 数据可视化/` 的轻量入口——"探索用自带 plot、交付用 Matplotlib"）。

## 6. 练习 5 题

1. 分组三件套是哪三件？第 3 步为什么必要？
2. `agg(["mean", "median"])` 和 `agg({"a": "mean"})` 的区别？
3. agg 和 transform 的区别（行数）？
4. pivot_table 的四参数？（index/columns/values/aggfunc）
5. `value_counts(normalize=True)` 返回什么？

## 7. 分组模式速查与面试题

**分组常用模式速查**（本课收口——"分组八式"）：

| 需求 | 写法 |
|------|------|
| 单列分组均值 | `df.groupby("A")["B"].mean().reset_index()` |
| 多列分组 | `df.groupby(["A", "B"])["C"].mean()` |
| 多函数聚合 | `df.groupby("A")["B"].agg(["mean", "max"])` |
| 不同列不同函数 | `df.groupby("A").agg({"B": "mean", "C": "sum"})` |
| 命名聚合 | `df.groupby("A").agg(avg=("B", "mean"))` |
| 分组 Top N | `df.groupby("A")["B"].nlargest(3)` |
| 分组内偏差 | `df["B"] - df.groupby("A")["B"].transform("mean")` |
| 二维透视 | `df.pivot_table(index="A", columns="B", values="C")` |

**面试题**："groupby 的原理是什么？"——答：**"split-apply-combine——拆开（按组分组）→ 各自算（每组应用聚合函数）→ 拼回（结果合并成表）——agg 压缩行数（每组一行）、transform 保持行数（每行一个值）——透视表是 groupby 的二维版"**——**答题结构：原理（三阶段）→ agg vs transform → 透视表**——"答出 split-apply-combine 术语 = 原理分"。

**练习延伸**：`groupby` 和 `pivot_table` 什么关系？（透视 = 二维分组——index 行/columns 列——"groupby 是透视的父级"——09 篇热力图用透视）。

**实战场景**（本课知识在真实分析里的位置）：**"分组是'发生了什么'的答案机"**——业务分析的三大经典问题全靠分组：**对比**（"哪个品类卖得好" → `groupby("category")["sales"].sum()`）；**画像**（"什么人在买" → 多维分组 + 透视——`pivot_table(index="age_group", columns="sex", values="amount")`——09 篇的幸存者画像同款）；**趋势**（"每月怎么变" → `groupby(df["date"].dt.month)` 或 resample——07 篇）——**"分组三件套 + 透视 = 描述性分析的 80%"**——面试讲项目时"我用 groupby 定位了最显著的特征"是核心动作。

**分组的业务纪律**（`../../阶段 1：前置基础/08-数据素养与伦理.md` 的分组版）：**"分组前先想'这个分组有意义吗'"**（按性别分销售有意义、按 ID 末位分无意义——**"分组维度 = 业务假设"**）；**"小样本分组要警惕"**（某组只有 3 人——均值的误差极大——`agg` 里带 `count` 看样本量——**"没看样本量的分组结论不可信"**）；**"分组结论要对比不要孤值"**（"女性 74%" 要配"男性 19%"——**"孤值是噪音、对比是信息"**——09 篇报告的结论格式）。

**一句话记住**：分组三件套 = groupby → agg → reset_index（拆算拼——**reset_index 是新手最大坎**）——agg 压缩行数、transform 保持行数——pivot_table 二维分组——**"分组是'发生了什么'的答案机——'发生了什么'全靠分组回答"**。

**分组面试补充**："分组聚合的性能注意？"——答："**能一次 agg 不多次 groupby**（一次遍历算多指标）、**先筛后分组**（`df[条件].groupby`——先减小数据再分组）、**category 列分组更快**（08 篇）——**"分组前先想'数据减到最小了吗'"**"——09 篇实战的透视表 + 热力图就是本课 pivot_table 的直接应用（算出来 → 画出来——三库协作的样板）——**"分组结论要对比不要孤值——'发生了什么'要配'和谁比'"**（09 篇结论格式的核心）。

## 8. 本节验收

**验收动作**：泰坦尼克分组分析——① 三件套算"性别 × 幸存率"；② `agg` 多函数（舱位 × 票价 mean/median/max）；③ transform 算"每行票价偏离组均值"；④ pivot_table 二维透视（舱位 × 性别 × 幸存率——09 篇热力图的原料）；⑤ `crosstab` 交叉计数——**"三件套 + 透视 = 分组层通过"**——**练习纪律**：每题结果打印验证——"分组结果要 reset_index 后再看"。

> 🎯 **核心要点**：分组三件套 = groupby → agg → reset_index（拆算拼——**reset_index 是新手最大坎**）；agg 多函数（列表/字典/命名聚合）；agg 压缩行数 vs transform 保持行数（"每组一个数" vs "每行一个值"）；pivot_table 二维分组（index/columns/values/aggfunc——热力图原料）；value_counts/crosstab 计数族——**"分组是分析的核心动作——'发生了什么'全靠分组回答"**。

---

**上一模块**：[03-数据清洗.md](./03-数据清洗.md) / **下一模块**：[05-合并与连接.md](./05-合并与连接.md)
