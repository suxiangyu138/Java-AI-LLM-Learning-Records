# 04 - Pandas 数据处理深度实战

> 🎯 Pandas 是 Python 数据分析的灵魂——80% 的数据分析工作就是 DataFrame 的增删改查、分组聚合、合并重塑。掌握本章，就能应对绝大多数数据分析需求

---

## 目录

1. [DataFrame 核心操作](#1-dataframe-核心操作)
2. [GroupBy 分组聚合](#2-groupby-分组聚合)
3. [数据合并与重塑](#3-数据合并与重塑)
4. [窗口函数与排名](#4-窗口函数与排名)
5. [实战：销售数据分析](#5-实战销售数据分析)

---

## 1. DataFrame 核心操作

### 1.1 增删改查

```python
import pandas as pd
import numpy as np

df = pd.read_csv("sales.csv")

# ---- 查 ----
df.head(10)                    # 前 10 行
df.info()                      # 列类型+非空计数
df.describe()                  # 数值列统计（均值/标准差/分位数）
df["category"].value_counts()  # 分类计数
df.nlargest(10, "amount")      # amount 最大的 10 行

# ---- 选 ----
df["name"]                     # 选一列 → Series
df[["name", "price"]]          # 选多列 → DataFrame
df.loc[0:9, ["name", "price"]] # 按标签选
df.iloc[0:10, 0:3]             # 按位置选
df[df["price"] > 100]          # 条件筛选
df[df["category"].isin(["电子", "服装"])]  # 多值筛选
df.query("price > 100 and category == '电子'")  # SQL 风格

# ---- 增 ----
df["total"] = df["price"] * df["quantity"]
df["month"] = pd.to_datetime(df["date"]).dt.month
df["category_code"] = df["category"].map({"电子": 1, "服装": 2})

# ---- 改 ----
df.loc[df["price"] < 0, "price"] = 0        # 条件修改
df["name"] = df["name"].str.upper()          # 字符串操作

# ---- 删 ----
df.drop("temp_column", axis=1, inplace=True) # 删列
df.drop_duplicates(subset=["order_id"])      # 去重
df.dropna(subset=["price", "quantity"])      # 删缺失
```

### 1.2 常用 Series 方法

```python
s = df["price"]

# 统计
s.mean(), s.median(), s.std(), s.quantile(0.95)
s.value_counts()          # 计数
s.nunique()               # 去重计数

# 字符串（需 .str 访问器）
df["name"].str.contains("Pro")
df["name"].str.extract(r"(\d+)")
df["name"].str.replace("_", " ")

# 日期（需 .dt 访问器）
pd.to_datetime(df["date"]).dt.year
pd.to_datetime(df["date"]).dt.dayofweek
```

## 2. GroupBy 分组聚合

### 2.1 分组三部曲

```text
Split → Apply → Combine
  分组    计算    合并
```

```python
# 基本分组聚合
df.groupby("category")["amount"].sum()
df.groupby("category").agg({
    "amount": ["sum", "mean", "count"],
    "price": "mean"
})

# 多列分组
df.groupby(["year", "month"])["amount"].sum().unstack()

# 自定义聚合函数
def range_func(x):
    return x.max() - x.min()

df.groupby("category")["amount"].agg(["sum", "mean", range_func])
```

### 2.2 Transform 与 Filter

```python
# Transform：保持原形状，给每行添加分组统计结果
df["pct_of_category"] = (
    df["amount"] / df.groupby("category")["amount"].transform("sum")
)

df["category_avg"] = df.groupby("category")["amount"].transform("mean")
df["above_avg"] = df["amount"] > df["category_avg"]

# Filter：按分组条件筛选
df.groupby("category").filter(lambda g: g["amount"].sum() > 10000)
```

## 3. 数据合并与重塑

### 3.1 合并方式

```python
# join 风格（按 index）
df1.join(df2, how="left")

# merge 风格（按列值）
pd.merge(orders, products, on="product_id", how="left")
pd.merge(left, right, left_on="user_id", right_on="id")

# concat 风格（纵向或横向拼接）
pd.concat([df1, df2], axis=0)  # 纵向追加
pd.concat([df1, df2], axis=1)  # 横向拼接
```

### 3.2 重塑

```python
# Pivot Table（数据透视表）
pd.pivot_table(df,
    values="amount",
    index="category",
    columns="month",
    aggfunc="sum",
    fill_value=0
)

# Melt（宽表 → 长表）
pd.melt(df, id_vars=["name"], value_vars=["Q1", "Q2", "Q3", "Q4"])

# Stack / Unstack（行列转换）
df.set_index(["date", "category"]).unstack()
```

## 4. 窗口函数与排名

```python
# 滚动窗口
df["ma_7"] = df["amount"].rolling(window=7).mean()   # 7日移动平均
df["sum_30d"] = df["amount"].rolling("30D", on="date").sum()

# 扩展窗口
df["cumsum"] = df["amount"].expanding().sum()          # 累积和
df["cummax"] = df["amount"].expanding().max()          # 累积最大值

# 排名
df["rank"] = df["amount"].rank(ascending=False)         # 降序排名
df["pct_rank"] = df["amount"].rank(pct=True)            # 百分比排名
df.groupby("category")["amount"].rank(method="dense")   # 分组排名

# 位移（取上一行/下一行）
df["prev_amount"] = df["amount"].shift(1)                # 上一行
df["change"] = df["amount"] - df["amount"].shift(1)      # 环比变化
df["pct_change"] = df["amount"].pct_change()             # 百分比变化
```

## 5. 实战：销售数据分析

```python
# 模拟数据
np.random.seed(42)
n = 1000
df = pd.DataFrame({
    "date": pd.date_range("2026-01-01", periods=n, freq="D"),
    "product": np.random.choice(["A", "B", "C", "D"], n),
    "category": np.random.choice(["电子", "服装", "食品"], n),
    "amount": np.random.normal(500, 150, n).clip(0),
    "quantity": np.random.randint(1, 10, n),
    "region": np.random.choice(["华东", "华南", "华北", "西部"], n),
})
df["revenue"] = df["amount"] * df["quantity"]

# ---- 分析 ----
# 1. 各类目销售额占比
cat_revenue = df.groupby("category")["revenue"].sum()
print(cat_revenue / cat_revenue.sum())

# 2. 月度趋势
df["month"] = df["date"].dt.to_period("M")
monthly = df.groupby("month")["revenue"].agg(["sum", "mean", "count"])
monthly["mom_change"] = monthly["sum"].pct_change()   # 环比

# 3. 各地区 Top 3 产品
top_products = (df.groupby(["region", "product"])["revenue"]
                .sum()
                .groupby(level=0)
                .nlargest(3))

# 4. RFM 简化版（每个产品的最后购买日期、购买次数）
rfm = df.groupby("product").agg(
    last_purchase=("date", "max"),
    frequency=("date", "count"),
    total_revenue=("revenue", "sum")
)
rfm["recency_days"] = (df["date"].max() - rfm["last_purchase"]).dt.days
```

## 核心要点回顾

- `loc`（标签）vs `iloc`（位置）vs `[]`（列名/条件）三种索引方式
- GroupBy = Split-Apply-Combine：分组 → 计算 → 合并
- Transform 不改变形状（给每行加分组结果），agg 改变形状（输出汇总行）
- Merge 四连：`left/right/inner/outer`，默认 inner
- 窗口函数：rolling(固定窗口) / expanding(累积) / shift(位移)

## 参考资料

1. Pandas 官方文档 — pandas.pydata.org/docs
2. Pandas Cheat Sheet — pandas.pydata.org/cheatsheet
