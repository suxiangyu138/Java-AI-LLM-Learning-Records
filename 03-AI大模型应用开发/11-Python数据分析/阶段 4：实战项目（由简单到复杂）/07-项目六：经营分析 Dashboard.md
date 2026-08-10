# 07 - 项目六：经营分析 Dashboard

> 中级项目六：把分析"变成能点能看的交互看板"——多表关联 + RFM 分层 + Cohort 留存 + 看板交付——"报告是'静态的结论'，看板是'动态的追问'——老板想自己点两下看看，就是看板的用武之地"

---

## 📚 目录

1. [背景与看板思维](#1-背景与看板思维)
2. [指标体系与多表关联](#2-指标体系与多表关联)
3. [RFM 客户分层](#3-rfm-客户分层)
4. [Cohort 留存分析](#4-cohort-留存分析)
5. [看板搭建与交付](#5-看板搭建与交付)
6. [本节验收](#6-本节验收)

---

## 1. 背景与看板思维

**背景**：经营分析报告（03 篇）做了两个月，老板的需求变了——"别每周发 PDF 了，做个页面我自己点"——**这就是 Dashboard（数据看板）**。

**看板思维（本项目核心）**：

```text
报告 vs 看板
├── 报告：静态文档、讲结论、适合"汇报"（每周一封邮件）
└── 看板：交互页面、能追问、适合"监控"（每天打开扫一眼）
    ——"报告回答'这周怎么样'，看板回答'哪里不对？点开看看'"
```

**看板心智**：**"看板不是'把图拼一起'，是'把指标系统化 + 提供追问路径'"**——看板三层：**指标层**（核心数字——GMV/客单价/留存）、**分析层**（可钻取——按渠道/品类/用户群筛选）、**故事层**（异常标注——"这个月留存掉了，旁边写一句可能原因"）——**"没有指标体系的看板是贴图，有体系的看板是驾驶舱"**；工具选型 2026：**Streamlit（Python 最快上手——本项目用）/ Plotly Dash（更工程化）**——"Streamlit 一行 `st.line_chart` 出图，`st.selectbox` 出筛选器——分析师 1 天交付看板"。

## 2. 指标体系与多表关联

**看板的第一步还是定指标（03 篇指标体系的升级版）**——经营看板核心指标组：

```text
经营看板指标组
├── 规模：GMV / 订单量 / 用户数（日/周/月趋势）
├── 质量：客单价 / 复购率 / 支付转化率
├── 客户：RFM 分层占比（下节）/ Cohort 留存（下下节）
└── 结构：品类占比 / 渠道占比 / 区域占比
```

```python
# 多表关联（阶段 2 05 篇 merge——Dashboard 的数据底座）
import pandas as pd

orders = pd.read_csv("data/orders.csv")     # order_id, user_id, amount, date
users  = pd.read_csv("data/users.csv")      # user_id, register_date, city
items  = pd.read_csv("data/items.csv")      # order_id, product, category

df = orders.merge(users, on="user_id", how="left").merge(items, on="order_id", how="left")
# 体检：merge 后行数（一对多会复制行——检查没爆炸）
assert len(df) >= len(orders)               # items 一对多 → 行数变多是正常的
```

**关联心智**：**"多表看板 = 先拼表、后算指标"**——`on` 指定连接键、`how="left"` 保订单为主表（阶段 2 05 篇的标准姿势）；**merge 后先体检**——行数变化/NaN 检查（"`user_id` 拼不上 = 用户表缺人——先查数据质量再做看板"——`../阶段%203：数据来源（数据从哪里来）/09-数据质量与来源管理.md` 的契约思维）；**指标计算函数化**（`def compute_kpis(df, date_range)`——看板每次刷新调函数——"指标函数 = 看板的口径保证书"）。

## 3. RFM 客户分层

**RFM = 客户价值分层三件套**（2026 年电商分析标配——RetailPulse 项目范式）：

```python
# R 最近一次购买 / F 购买频率 / M 消费金额
now = df["date"].max()
rfm = df.groupby("user_id").agg(
    R=("date", lambda s: (now - s.max()).days),   # 最近购买距今几天（越小越好）
    F=("order_id", "nunique"),                    # 购买次数（越大越好）
    M=("amount", "sum"),                          # 累计金额（越大越好）
)

# 分层：按分位数给 R/F/M 各打 1-4 分（或按业务阈值）
rfm["R_score"] = pd.qcut(rfm["R"], 4, labels=[4, 3, 2, 1])   # R 越小分越高
rfm["F_score"] = pd.qcut(rfm["F"].rank(method="first"), 4, labels=[1, 2, 3, 4])
rfm["M_score"] = pd.qcut(rfm["M"].rank(method="first"), 4, labels=[1, 2, 3, 4])

# 价值分层（经典 8 宫格简化：高/中/低价值）
rfm["segment"] = rfm.apply(
    lambda r: "高价值" if r["R_score"] >= 3 and r["F_score"] >= 3 else
              "流失风险" if r["R_score"] <= 2 and r["F_score"] >= 3 else "一般",
    axis=1)
print(rfm["segment"].value_counts(normalize=True))   # 各层占比
```

**RFM 心智**：**"RFM 回答'哪些客户值得维护'"**——高价值（又近又频又大）要重点维护、流失风险（以前很活跃最近不来了）要召回——**"RFM 分层 = 运营动作的分流器"**（高价值→专属客服、流失风险→优惠券召回）；**分层阈值要业务确认**（分位数是默认，业务可能有自己的标准——"R≤7 天算活跃"——口径契约）；**`qcut` 与 `rank`**：R 是越小越好要翻转标签、F/M 有并列要 `rank` 处理——**"RFM 的坑都在排序方向，写出来先 value_counts 验证"**。

## 4. Cohort 留存分析

**Cohort = 同期用户群体的留存曲线**——"7 月注册的用户，第 2 个月还剩下多少"：

```python
# 每个用户的首购月 = 他的 cohort（同期群）
df["month"] = df["date"].dt.to_period("M")
cohort = df.groupby("user_id")["month"].min().rename("cohort_month")

# 每个用户每次购买的"第 N 月"（相对首购月的偏移）
df2 = df.merge(cohort, on="user_id")
df2["period"] = (df2["month"] - df2["cohort_month"]).apply(lambda p: p.n)

# 留存矩阵：cohort × period 的活跃用户数 → 留存率
retention = df2.groupby(["cohort_month", "period"])["user_id"].nunique().unstack()
retention_pct = retention.div(retention[0], axis=0)   # 除以第 0 期（100%）
print(retention_pct.round(2))
```

**Cohort 心智**：**"Cohort 回答'新用户留下来多少'"**——留存矩阵的阅读：**横着看（同一批人随时间衰减）**、**竖着比（不同批次同期的留存——8 月注册的比 6 月的留存好 = 产品改版有效）**——"竖着比是 Cohort 的精髓：同期对比排除'产品变老'的干扰"；**留存的业务含义**：第 1 期留存（次月留存）是产品健康度第一指标——"次月留存 30% 是很多产品的生死线"；**Cohort 需要数据积累**（至少 2-3 个月的历史）——"刚上线的新产品做不了 Cohort，数据不够"。

## 5. 看板搭建与交付

**Streamlit 看板（2026 年分析师最快的交付方式）**：

```python
# app.py——Streamlit 看板骨架
import streamlit as st
import pandas as pd

st.set_page_config(page_title="经营分析看板", layout="wide")

@st.cache_data                       # 数据缓存——刷新不重算
def load_data():
    return pd.read_parquet("data/processed/sales.parquet")

df = load_data()

# 指标卡（第一行：核心数字）
c1, c2, c3, c4 = st.columns(4)
c1.metric("GMV", f"{df['amount'].sum():,.0f}", "+8% vs 上周")
c2.metric("订单量", f"{df['order_id'].nunique():,}")
c3.metric("客单价", f"{df['amount'].sum() / df['order_id'].nunique():.0f}")
c4.metric("复购率", f"{df.groupby('user_id')['order_id'].nunique().gt(1).mean():.0%}")

# 交互筛选（看板的"追问路径"）
channel = st.sidebar.selectbox("渠道", ["全部"] + list(df["channel"].unique()))
if channel != "全部":
    df = df[df["channel"] == channel]

# 趋势图 + 品类占比
st.line_chart(df.groupby("date")["amount"].sum())          # 一行出图
st.bar_chart(df.groupby("category")["amount"].sum().nlargest(10))
```

**看板心智**：**"看板三要素：指标卡（一眼看全）、筛选器（能追问）、图表（讲细节）"**——`st.cache_data` 缓存是性能关键（数据不变别重算）；**交付动作**：`streamlit run app.py` 本地跑通 → 部署（Streamlit Cloud/Hugging Face Spaces——2026 年免费托管主流——`../../../03-AI大模型应用开发/02-大模型基础与Prompt工程/HuggingFace/` 的 Spaces 也能挂）——**"看板交付 = 能跑 + 能点 + 有部署地址"**；**README 写使用说明**（数据更新方式/指标口径/部署地址——"看板是给团队用的，使用说明是看板的一半"）。

## 6. 本节验收

**验收动作**：① 多表 merge 并体检；② RFM 分层（value_counts 验证分布合理）；③ Cohort 留存矩阵（横看衰减/竖比同期）；④ Streamlit 看板（指标卡 + 筛选器 + 3 张图）；⑤ 部署并交付 README——**"指标 + RFM + Cohort + 看板 = 经营分析能力"**——**练习纪律**：看板的口径和报告口径必须一致（同一套指标函数）——"看板与报告打架，是数据团队最大的信任危机"。

> 🎯 **核心要点**：中级项目六 = 呈现升级——报告 vs 看板（静态结论 vs 动态追问）；指标层/分析层/故事层三层看板；多表 merge 先体检；**RFM 分层（R 翻转排序坑 + 分层=运营分流器）**；**Cohort 留存（横看衰减、竖比同期——次月留存是健康度第一指标）**；Streamlit 交付（指标卡/筛选器/图 + 部署 + README）——**"没有指标体系的看板是贴图，有体系的看板是驾驶舱"**。

---

**上一模块**：[06-项目五：完整数据管道项目.md](./06-项目五：完整数据管道项目.md) / **下一模块**：[08-项目七：A/B 测试实验分析.md](./08-项目七：A-B%20测试实验分析.md)
