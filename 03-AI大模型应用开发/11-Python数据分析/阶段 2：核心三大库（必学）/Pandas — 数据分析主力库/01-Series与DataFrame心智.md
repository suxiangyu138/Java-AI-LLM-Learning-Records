# 01-Series 与 DataFrame 心智

> Pandas 的第一课：两大对象（Series 一列 / DataFrame 表）、列优先心智落地、三种创建方法——"Pandas 的一切都是表思维：整列思考、标签操作"

---

## 📚 目录

1. [Pandas 定位与版本](#1-pandas-定位与版本)
2. [Series：一列](#2-series一列)
3. [DataFrame：表](#3-dataframe表)
4. [三种创建方法](#4-三种创建方法)
5. [列优先心智落地](#5-列优先心智落地)
6. [练习 5 题](#6-练习-5-题)
7. [常用方法速查与面试表述](#7-常用方法速查与面试表述)
8. [本节验收](#8-本节验收)

---

## 1. Pandas 定位与版本

**Pandas = 表格分析主力库**——数据分析的"主心骨"（读表/清洗/分组/合并全是它）——**"NumPy 管数组、Pandas 管表格——表格是数据分析的货币"**（`../../阶段 1：前置基础/04-数据容器与表格思维.md` 的心智在这里变成能力）。2026-08 基线：**Pandas 3.0.3**（3.0 是 2026 年初的大版本——从 2.3 升级路径平滑；需 Python 3.11+；老教程里的部分废弃 API 已移除）。

**Pandas 的两个世界**：**标签**（列名/行索引——人类友好）与**数值**（底层 numpy 数组——机器高效）——**"Pandas 是'带标签的 NumPy'——标签是它存在的理由"**——所以本课的一切都围绕"标签操作"（`df["age"]` 按列名、`df.loc["行名"]` 按行标签）。

## 2. Series：一列

**Series = 一列数据（带索引）**：

```python
import pandas as pd

s = pd.Series([22, 35, 41], index=["a", "b", "c"], name="age")
# a    22
# b    35
# c    41
# Name: age, dtype: int64

s["b"]          # 35 按索引取（标签）
s.iloc[1]       # 35 按位置取
s.values        # array([22, 35, 41]) numpy 数组（底层）
s.index         # Index(['a', 'b', 'c'])
```

**Series 心智**：**Series = 带标签的一维数组**（值 + 索引 + 名字）——**"Series 是 DataFrame 的一列"**（df["age"] 返回 Series）；**Series 的运算就是 NumPy 数组的运算**（`s * 2` 向量化——`../NumPy — 数值计算底层库/` 的知识直接复用）；**Series 的标签是"数据的一部分"**（不是数组的位置——重排后标签跟着走）。

## 3. DataFrame：表

**DataFrame = 二维表（行样本 × 列特征 + 标签）**：

```python
df = pd.DataFrame({
    "age": [22, 35, 41],
    "sex": ["男", "女", "女"],
    "fare": [7.25, 71.28, 8.05],
})
#    age sex   fare
# 0   22  男   7.25
# 1   35  女  71.28
# 2   41  女   8.05

df["age"]            # Series（一列——按列名）
df[["age", "fare"]]  # DataFrame（多列——双层括号！）
df.shape             # (3, 3) 行 × 列
df.index             # RangeIndex(start=0, stop=3) 行标签
df.columns           # Index(['age', 'sex', 'fare']) 列名
df.dtypes            # 每列类型（阶段 1 的 info 底层）
```

**DataFrame 心智**（阶段 1 的完整落地）：**行 = 样本（index）、列 = 特征（columns）**（`../../阶段 1：前置基础/04-数据容器与表格思维.md` 的表格四要素）；**`df["列"]` 取一列（Series）、`df[["列1","列2"]]` 取多列（DataFrame——**双层括号**是高频易错点）；**`df.shape` 是体检第一项**（(行, 列)——行列顺序别反）。

## 4. 三种创建方法

```python
# 1. 列优先字典（最常用——阶段 1 的落地）
df1 = pd.DataFrame({"age": [22, 35], "sex": ["男", "女"]})

# 2. 从 NumPy 数组 + 列名
arr = np.array([[22, 1], [35, 0]])
df2 = pd.DataFrame(arr, columns=["age", "male"], index=["a", "b"])

# 3. 从列表套字典（每行一个字典——接口数据常见）
df3 = pd.DataFrame([{"age": 22, "sex": "男"}, {"age": 35, "sex": "女"}])

# 4. 从 CSV（实战主力——阶段 1 已会）
df4 = pd.read_csv("titanic.csv")
```

**创建心智**：**"列优先字典是标准姿势"**（`{"列名": [值列表]}`——阶段 1 的练习在这里变成正式能力）；**数组转表要指定 columns**（不然列名是 0,1,2...——列名即代码注释——`../../阶段 1：前置基础/04-数据容器与表格思维.md` 的列名规范）；**列表套字典适合接口数据**（`json_normalize` 的兄弟——阶段 3 预演）；**真实数据 90% 从 read_csv 来**（`../../阶段 1：前置基础/05-数据读写与文件格式.md` 的读写课）。

## 5. 列优先心智落地

**"整列思考"从概念变成操作**（阶段 1 的表格思维 × 本课的 API）：

```python
# 整列操作（不是逐行！）
df["fare_double"] = df["fare"] * 2        # 新建列（向量化）
df["fare_log"] = np.log(df["fare"])        # NumPy 函数直接作用于列
df["age_mean"] = df["age"].mean()          # 聚合标量广播到整列

# 列的通用方法
df["age"].mean() / df["fare"].sum()        # 聚合
df["sex"].value_counts()                   # 类别计数（阶段 1 的 value_counts 正式版）
df["sex"].unique()                         # 唯一值（numpy 的 unique 表版）
df.describe()                              # 数值列统计（阶段 1 六指标的完整版）
```

**落地心智**：**"对列做什么，就对整列做"**（新建列 = 列运算表达式）；**`np.xxx` 直接吃 Series**（`np.log(df["fare"])`——NumPy 与 Pandas 无缝协作——`../NumPy — 数值计算底层库/08-与Pandas协作与性能.md`）；**聚合返回标量、广播回整列**（`df["age"].mean()` 是数、`df["age"] * 2` 是列——两个世界要分清）。

## 6. 练习 5 题

1. `df[["a", "b"]]` 和 `df["a"]` 返回什么类型？（DataFrame vs Series）
2. Series 的 index 和 values 各是什么？
3. 列优先字典创建 DataFrame 的语法？
4. `df.shape` 返回什么顺序？（行, 列）
5. `df["age"] * 2` 是逐行还是向量化？

## 7. 常用方法速查与面试表述

**DataFrame 高频方法速查**（本课收口——先记分类再记函数）：

| 分类 | 方法 | 用途 |
|------|------|------|
| 查看 | `head/tail/shape/columns/dtypes/info/describe` | 体检六件套（阶段 1 三动作升级） |
| 取列 | `df["col"]`（Series）/ `df[["a","b"]]`（表） | 一列 vs 多列（双层括号） |
| 聚合 | `mean/sum/max/min/count/std` | 列统计（04 篇深化） |
| 类别 | `value_counts/unique/nunique` | 类别分析（nunique = 去重数） |
| 排序 | `sort_values/sort_index` | 排序（ascending 参数） |
| 转换 | `astype/to_numeric/apply` | 类型/函数（03/06 篇） |

**面试表述**："Pandas 和 NumPy 什么关系？"——答：**"Pandas 建立在 NumPy 之上——DataFrame 的每一列底层都是 numpy 数组——Pandas 加了标签（列名/索引）和表操作（分组/合并）——NumPy 管数组运算、Pandas 管表格分析——数据分析里 Pandas 是主、NumPy 是底"**——**答题结构：关系 → 差异 → 分工**——"答出'每列是数组'显示底层理解"。

**练习延伸**：`nunique()` 和 `unique()` 的区别？（去重个数 vs 去重列表）；`describe()` 输出哪六项？（count/mean/std/min/25%/50%/75%——阶段 1 已学）。

**实战场景**（本课知识在真实分析里的位置）：**"读表 → 体检 → 第一眼结论"**——`read_csv` 读入后固定动作：`df.shape`（多大）→ `df.head()`（长啥样）→ `df.info()`（类型缺失）→ `df.describe()`（数值分布）——**"四连击是每份数据的开场白"**（`../../阶段 1：前置基础/05-数据读写与文件格式.md` 三动作的完整版）——然后才进入清洗（03 篇）与分组（04 篇）——**"Pandas 的读表能力是分析的入口——入口顺则全程顺"**——面试讲项目时"我拿到数据先做四连击体检"是标准开场。

**DataFrame vs Excel 的心智对照**（阶段 1 迁移心法的完成）：**Excel 选区域 = `df.loc`**；**Excel 排序筛选 = `sort_values` + 布尔筛选**；**Excel 透视表 = `pivot_table`**（04 篇）；**Excel 公式 = 整列运算**——**"Excel 里点过的每个动作，Pandas 都有对应代码——表格思维 + 代码 = 可复现分析"**（`../../阶段 1：前置基础/04-数据容器与表格思维.md` 的对照表在这里全部兑现）。

**一句话记住**：Series 是一列（带标签的数组）、DataFrame 是表（行样本列特征）——**"df["列"] 是 Series、df[["列"]] 是表——双层括号别混"**——整列思考、标签操作——**"Pandas = 带标签的 NumPy——标签是它存在的理由"**——练习 5 题全对 + 四连击体检亲手做一遍（shape/head/info/describe）——**"体检四连击是每份数据的开场白——本课验收从它开始"**（数据到手先体检——这是分析师的第一职业习惯——09 篇实战第一步就是四连击——**"四连击不是流程，是本能"**）。

## 8. 本节验收

**验收动作**：① 三种创建方法各建一个表；② 打印 shape/columns/dtypes 体检；③ 新建三列（fare*2、np.log、age-mean 广播）；④ `value_counts` + `describe` 各一次——**"三法 + 整列操作 = 对象心智建立"**——**练习纪律**：每题的答案打印验证——"表的问题跑出来才算数"。

> 🎯 **核心要点**：Series = 带标签的一维数组（DataFrame 的一列——运算同 NumPy）；DataFrame = 行样本 × 列特征 + 标签（`df["列"]` Series、`df[["列"]]` DataFrame——双层括号易错）；创建四法（列优先字典是标准姿势）；整列思考落地（新建列 = 列运算、np.xxx 直接吃 Series、聚合标量 vs 列两个世界）——**"Pandas 是带标签的 NumPy——标签是它存在的理由"**。

---

**上一模块**：[00-Pandas数据分析总览.md](./00-Pandas数据分析总览.md) / **下一模块**：[02-索引与选择.md](./02-索引与选择.md)
