# 极速吃透 Python 爬虫 + 数据分析

> **核心摘要**：Python 爬虫与数据分析的极速入门指南，覆盖 requests + bs4 静态爬虫、Selenium 动态爬虫、pandas 数据分析全流程，2 小时即可上手实战。

---

## 一、整体技术栈

### 1.1 爬虫核心

| 工具 | 用途 |
|------|------|
| `requests` | 同步网页请求（最简单、最常用） |
| `parsel` / `bs4` | 网页解析、提取文本/标签 |
| `re` | 清洗杂乱文本 |
| `selenium` | 动态 JS 页面、反爬、模拟浏览器 |

### 1.2 数据分析核心

| 工具 | 用途 |
|------|------|
| `numpy` | 数值计算、数组 |
| `pandas` | 表格处理、Excel/CSV、筛选统计（核心） |
| `matplotlib` | 基础可视化图表 |

```bash
pip install requests beautifulsoup4 pandas numpy matplotlib parsel
```

## 二、Python 爬虫速成

### 2.1 静态网页爬虫

```python
import requests
from bs4 import BeautifulSoup

headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"}
url = "https://www.baidu.com"
resp = requests.get(url, headers=headers, timeout=10)
resp.encoding = "utf-8"

soup = BeautifulSoup(resp.text, "html.parser")
title = soup.title.string
print(title)
```

### 2.2 数据提取

**CSS 选择器**（推荐）：
```python
a_list = soup.select("a")
for a in a_list:
    text = a.get_text(strip=True)
    link = a.get("href")
```

**正则提取**：
```python
import re
pattern = re.compile(r"https?://.*?.com")
res = pattern.findall(html)
```

### 2.3 保存数据

```python
with open("data.csv", "w", newline="", encoding="utf-8-sig") as f:
    writer = csv.writer(f)
    writer.writerow(["标题", "链接"])
```

### 2.4 动态爬虫 Selenium

```python
from selenium import webdriver
from selenium.webdriver.common.by import By

driver = webdriver.Chrome()
driver.get("目标网址")
text = driver.find_element(By.CLASS_NAME, "content").text
driver.quit()
```

### 2.5 爬虫核心常识

> **重点**：请求头 User-Agent 必须加，伪装浏览器；超时 timeout 防止卡死；延时 sleep() 防频率限制；遵守 robots 协议，合法合规。

## 三、数据分析速成

### 3.1 Pandas 核心操作

```python
import pandas as pd

# 构建表格
data = {"姓名": ["张三", "李四"], "分数": [88, 92]}
df = pd.DataFrame(data)

# 条件筛选
high = df[df["分数"] >= 90]

# 分组统计
df.groupby("姓名")["分数"].mean()

# 缺失值处理
df = df.dropna()
df = df.fillna(0)

# 导出
df.to_csv("result.csv", index=False, encoding="utf-8-sig")
```

### 3.2 可视化

```python
import matplotlib.pyplot as plt

plt.rcParams["font.sans-serif"] = ["SimHei"]
plt.rcParams["axes.unicode_minus"] = False

x = [1, 2, 3]
y = [10, 20, 15]
plt.plot(x, y)
plt.title("测试图表")
plt.show()
```

## 四、爬虫 + 数据分析完整闭环

1. 爬虫（requests/selenium）抓取网页原始数据
2. 解析、清洗（bs4 + 正则）
3. 存入列表/CSV
4. pandas 读取数据、清洗异常值
5. 筛选、分组、统计、分析
6. 图表可视化 / 导出报告

## 核心要点回顾

- 静态页面用 `requests`，动态 JS 用 `selenium`
- 抓标签用 `bs4`，乱码文本用正则
- 表格处理认准 `pandas`，数值计算用 `numpy`
- 爬虫重在伪装 + 延时 + 合规，数据分析重在清洗 + 筛选 + 聚合

## 参考资料

1. BeautifulSoup 官方文档 - css.crummy.com
2. Pandas 官方文档 - 10 Minutes to Pandas
3. Matplotlib 官方文档 - pyplot 教程
