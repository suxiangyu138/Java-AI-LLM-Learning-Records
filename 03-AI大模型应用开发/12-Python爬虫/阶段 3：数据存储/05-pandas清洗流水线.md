# pandas 清洗流水线
> DataFrame 读取、清洗、去重、类型转换、导出——"爬到的原始数据 → 可分析的数据集"

## 📚 目录
1. [pandas 定位与版本](#1-pandas-定位与版本)
2. [读取：CSV/JSON/数据库进 DataFrame](#2-读取csvjson数据库进-dataframe)
3. [清洗：缺失值/空白/去重](#3-清洗缺失值空白去重)
4. [转换：类型/字段/派生列](#4-转换类型字段派生列)
5. [导出：CSV/Excel/数据库](#5-导出csvexcel数据库)
6. [清洗流水线模板](#6-清洗流水线模板)

## 1. pandas 定位与版本

| 事实 | 说明 |
|------|------|
| 版本 | pandas 2.x（2026） |
| 定位 | Python 数据分析核心库（表格处理） |
| 本阶段用途 | **爬虫数据的清洗流水线**（批量/向量化/导出） |
| 核心对象 | DataFrame（表格）/ Series（列） |

> 🎯 与阶段 2 §5 的关系：**阶段 2 的清洗是"逐条函数"，pandas 是"整表向量化"**——100 万行数据，逐条清洗 vs pandas 批量，速度差 100 倍。数据量大了必须换 pandas。

## 2. 读取：CSV/JSON/数据库进 DataFrame

```python
import pandas as pd

# ═══ 从 CSV 读取 ═══
df = pd.read_csv("news.csv")                       # 自动识别表头
df = pd.read_csv("news.csv", encoding="utf-8-sig") # ⚠️ 阶段 2 写入的 BOM 编码

# ═══ 从 JSON 读取 ═══
df = pd.read_json("news.json")

# ═══ 从数据库读取（MySQL）═══
import pymysql
conn = pymysql.connect(host="localhost", user="root", password="xxx",
                       database="crawler_db", charset="utf8mb4")
df = pd.read_sql("SELECT * FROM news", conn)       # SQL 直接进 DataFrame
conn.close()

# ═══ 从列表（爬虫解析结果）═══
items = [{"title": "a", "views": 100}, {"title": "b", "views": 200}]
df = pd.DataFrame(items)
```

| 读取方式 | 场景 |
|---------|------|
| `read_csv` | 文件数据（爬虫输出） |
| `read_json` | JSON 数据 |
| `read_sql` | **数据库直读（清洗回写闭环）** |
| `DataFrame(list)` | 内存数据 |

> 🎯 核心价值：**`read_sql` 让"数据库 ↔ pandas ↔ 数据库"闭环**——爬虫入库 → pandas 清洗 → 回写/导出，全程不用中间文件。

## 3. 清洗：缺失值/空白/去重

```python
# ═══ 查看数据质量 ═══
df.info()                          # 列类型/非空计数（一眼看出缺数据）
df.describe()                      # 数值列统计（min/max/mean）

# ═══ 缺失值处理 ═══
df.isnull().sum()                  # 每列缺失数
df.dropna(subset=["title"])        # 删 title 为空的（关键字段必填）
df["views"].fillna(0)              # 数值列补 0
df["author"].fillna("未知")        # 文本列补默认

# ═══ 空白清洗（pandas 版）═══
df["title"] = df["title"].str.replace("\xa0", " ")   # \xa0（阶段 2 老朋友）
df["title"] = df["title"].str.strip()                # 去首尾
df["content"] = df["content"].str.replace(r"\s+", " ", regex=True)  # 连续空白

# ═══ 去重 ═══
df.drop_duplicates(subset=["url"])                   # URL 去重
df.drop_duplicates(subset=["title"], keep="first")   # 标题去重（留第一条）
df.drop_duplicates(subset=["url"], keep=False)       # 全删重复（keep=False）

# ═══ 过滤（去噪）═══
df = df[~df["title"].str.contains("广告|推广|置顶", na=False)]   # 去噪
df = df[df["views"] > 0]                             # 条件过滤
```

| 清洗操作 | pandas 写法 |
|---------|------------|
| 缺失统计 | `df.isnull().sum()` |
| 删空行 | `dropna(subset=[列])` |
| 补默认 | `fillna(值)` |
| 空白清洗 | `str.replace/.strip`（向量化！） |
| 去重 | `drop_duplicates(subset=[列])` |
| 条件过滤 | `df[df[列] 条件]` |

> 🎯 pandas 清洗哲学：**"向量化 = 一行命令处理整列"**——`df["title"].str.strip()` 一次处理 100 万行；手写 for 循环是阶段 2 的思维，pandas 时代要换成"列操作"思维。

## 4. 转换：类型/字段/派生列

```python
# ═══ 类型转换（字符串 → 正确类型）═══
df["views"] = pd.to_numeric(df["views"], errors="coerce")   # 数字（失败→NaN）
df["date"] = pd.to_datetime(df["date"], errors="coerce")    # 日期
df["price"] = df["price"].str.replace("￥", "").str.replace(",", "") \
                          .astype(float)                     # 金额清洗+转换

# ═══ 字段操作 ═══
df["domain"] = df["url"].str.extract(r"https?://([^/]+)")    # 正则提取新列
df["title_len"] = df["title"].str.len()                      # 派生列
df["year"] = df["date"].dt.year                              # 日期拆年
df = df.rename(columns={"title": "标题", "url": "链接"})      # 重命名

# ═══ 选择与排序 ═══
df = df[["title", "url", "views"]]                  # 选列（顺序可排）
df = df.sort_values("views", ascending=False)       # 按浏览量排序
top10 = df.head(10)                                 # 前 10

# ═══ 分组统计（分析）═══
by_domain = df.groupby("domain")["views"].sum()     # 按域汇总
by_date = df.groupby(df["date"].dt.date).size()     # 每天抓取量
```

| 转换操作 | pandas 写法 |
|---------|------------|
| 字符串→数字 | `pd.to_numeric(errors="coerce")` |
| 字符串→日期 | `pd.to_datetime(errors="coerce")` |
| 正则提取 | `str.extract(r"正则")` |
| 派生列 | `df["新列"] = 表达式` |
| 分组统计 | `groupby(列)[值列].sum()/count()/mean()` |

> 💡 `errors="coerce"` 是转换的"容错开关"：非法值变 NaN（不报错中断）——与阶段 2 的 `to_int` 默认值思想一致，pandas 版是 NaN + `fillna`。

## 5. 导出：CSV/Excel/数据库

```python
# ═══ 导出 CSV ═══
df.to_csv("news_clean.csv", index=False, encoding="utf-8-sig")  # 不写索引行

# ═══ 导出 Excel（交付业务）═══
# pip install openpyxl
df.to_excel("news_clean.xlsx", index=False, sheet_name="新闻数据")

# ═══ 回写数据库（MySQL）═══
from sqlalchemy import create_engine        # pandas 官方推荐方式
# pip install sqlalchemy pymysql
engine = create_engine("mysql+pymysql://root:xxx@localhost/crawler_db?charset=utf8mb4")
df.to_sql("news_clean", engine, if_exists="append", index=False)
# ⚠️ 中文列名/特殊字符注意；大表用 chunksize
```

| 导出方式 | 场景 |
|---------|------|
| `to_csv` | 通用（utf-8-sig 兼容 Excel） |
| `to_excel` | 交付业务（非技术同事） |
| `to_sql` | **清洗结果回写数据库**（闭环） |

> 🎯 闭环思维：**"爬虫入库 → pandas 清洗 → 回写/导出"**——`to_sql` 让清洗结果直接回库（`if_exists="append"` 增量追加），数据分析师直接用干净数据。

## 6. 清洗流水线模板

```python
"""数据清洗流水线（本阶段标准模板）"""
import pandas as pd

def clean_pipeline(input_file="news_raw.csv", output_file="news_clean.csv"):
    # ① 读取
    df = pd.read_csv(input_file, encoding="utf-8-sig")

    # ② 基础清洗（空白/缺失）
    for col in ["title", "author"]:
        if col in df.columns:
            df[col] = df[col].astype(str).str.replace("\xa0", " ").str.strip()
    df = df.dropna(subset=["title"])

    # ③ 类型转换
    if "views" in df.columns:
        df["views"] = pd.to_numeric(df["views"], errors="coerce").fillna(0).astype(int)
    if "date" in df.columns:
        df["date"] = pd.to_datetime(df["date"], errors="coerce")

    # ④ 去重（URL + 内容指纹）
    df = df.drop_duplicates(subset=["url"], keep="first")

    # ⑤ 去噪过滤
    if "title" in df.columns:
        df = df[~df["title"].str.contains("广告|推广", na=False)]

    # ⑥ 导出
    df.to_csv(output_file, index=False, encoding="utf-8-sig")
    print(f"清洗完成：{len(df)} 条 → {output_file}")
    return df

if __name__ == "__main__":
    clean_pipeline()
```

> 🎯 流水线设计原则：**"一步一验证"**——每步后 `print(len(df))` 观察数据量变化（清洗掉的量 = 数据质量报告）；流水线写成函数（输入输出明确），配定时任务就是自动化数据管道（阶段 6 工程化）。

---

**下一模块**：[06-存储选型与生产规范](06-存储选型与生产规范.md) / **返回总览**：[00-阶段3数据存储总览](00-阶段3数据存储总览.md)
