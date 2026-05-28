# 数据分析 (Data Analysis)

> Python 数据分析实战练习：NumPy → Pandas → 可视化 → 完整分析报告

## 项目概述

Python 数据分析系统性实践项目，使用 NumPy、Pandas、Matplotlib/Seaborn 三大核心库，覆盖数据清洗、探索性分析（EDA）、统计分析和可视化报告生成的完整数据工作流。

## 技术栈

| 技术 | 说明 |
|------|------|
| Python | 3.10+ |
| NumPy | 数值计算与数组操作 |
| Pandas | 数据处理与分析核心 |
| Matplotlib | 基础绑图 |
| Seaborn | 统计可视化 |
| Plotly | 交互式可视化（可选） |
| Jupyter | 交互式分析环境 |

## 学习路线

### 第一阶段：NumPy 基础
- ndarray 创建与操作
- 矩阵运算、广播机制
- 统计函数（mean, std, percentile）
- 随机数生成

### 第二阶段：Pandas 数据处理
- Series 和 DataFrame
- 数据读取（CSV, Excel, JSON, SQL）
- 数据清洗（缺失值、重复值、异常值、类型转换）
- 数据筛选、排序、分组聚合
- 表连接（merge, join, concat）
- 数据透视表（pivot_table）

### 第三阶段：数据可视化
- Matplotlib 基础图表（折线图、柱状图、散点图、饼图）
- Seaborn 统计图（箱线图、小提琴图、热力图、pairplot）
- 多子图布局与样式定制
- 时间序列可视化

### 第四阶段：综合实战
- 探索性数据分析（EDA）
- 相关性分析
- 趋势分析与预测
- 分析报告生成

## 项目结构

```
数据分析/
├── data/                            # 数据文件
│   ├── raw/                         # 原始数据
│   └── processed/                   # 处理后数据
├── notebooks/                       # Jupyter Notebook
│   ├── 01_data_loading.ipynb
│   ├── 02_data_cleaning.ipynb
│   ├── 03_exploratory_analysis.ipynb
│   └── 04_visualization.ipynb
├── src/
│   ├── load_data.py                 # 数据加载
│   ├── clean_data.py                # 数据清洗
│   ├── analysis.py                  # 分析逻辑
│   └── visualize.py                 # 可视化
├── output/                          # 图表输出
├── requirements.txt
├── main.py
└── README.md
```

## 快速开始

```bash
# 安装依赖
pip install -r requirements.txt

# 运行分析脚本
python main.py

# 或启动 Jupyter
jupyter notebook
```

## 核心知识点

| 知识点 | 说明 |
|--------|------|
| DataFrame 操作 | 索引、切片、筛选、排序、分组 |
| 缺失值处理 | dropna / fillna / interpolate |
| 数据聚合 | groupby + agg（sum/mean/count） |
| 数据合并 | merge / join / concat |
| 透视表 | pivot_table |
| 时间序列 | resample / rolling / shift |
| apply / map | 自定义数据转换 |
| 可视化 | 选择合适的图表类型表达数据 |

## 推荐练习数据集

- Titanic（Kaggle - 入门经典）
- Netflix Movies and TV Shows
- NYC Taxi Trip Duration
- COVID-19 全球数据
- 链家/贝壳 房价数据（爬虫获取）

## 注意事项

- Pandas 链式操作注意 SettingWithCopyWarning
- 大数据集（>1GB）考虑使用 Dask 或 Polars
- 分析前先了解数据字典（字段含义）
- 可视化时注意色盲友好配色
