极速吃透 Python 爬虫 + 数据分析
一、整体技术栈（必学就这几个）
爬虫核心
-  requests ：同步网页请求（最简单、最常用）
-  parsel / bs4 ：网页解析、提取文本/标签
-  re  正则：清洗杂乱文本
-  selenium ：动态JS页面、反爬、模拟浏览器
    数据分析核心
-  numpy ：数值计算、数组
-  pandas ：表格处理、Excel/CSV、筛选统计（核心）
-  matplotlib ：基础可视化图表
    安装依赖
    bash
    pip install requests beautifulsoup4 pandas numpy matplotlib parsel
 
 
第一部分：Python 爬虫 速成
1. 基础爬虫｜静态网页（90% 简单网站够用）
    发请求 + 解析
    python
    import requests
    from bs4 import BeautifulSoup

# 请求头，伪装浏览器，防基础反爬
headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
}
url = "https://www.baidu.com"
resp = requests.get(url, headers=headers, timeout=10)
resp.encoding = "utf-8"

# 解析网页
soup = BeautifulSoup(resp.text, "html.parser")

# 提取标题
title = soup.title.string
print(title)
 
2. 数据提取 两种主流写法
    ① CSS 选择器（好写、推荐）
    python

# 查找所有 a 标签
a_list = soup.select("a")
for a in a_list:
    text = a.get_text(strip=True)
    link = a.get("href")
 
② 正则提取（抓不规则文本）
python
import re
html = resp.text

# 提取所有网址
pattern = re.compile(r"https?://.*?.com")
res = pattern.findall(html)
 
3. 保存数据｜本地 TXT / CSV
    python

# 保存文本
with open("data.txt", "w", encoding="utf-8") as f:
    f.write(resp.text)

# 单行写入csv
import csv
with open("data.csv", "w", newline="", encoding="utf-8-sig") as f:
    writer = csv.writer(f)
    writer.writerow(["标题", "链接"])
 
4. 动态爬虫 Selenium（JS 渲染、反爬网站）
    bash
    pip install selenium
 
python
from selenium import webdriver
from selenium.webdriver.common.by import By
import time
driver = webdriver.Chrome()
driver.get("目标网址")
time.sleep(2)

# 提取元素
text = driver.find_element(By.CLASS_NAME, "content").text
driver.quit()
 
5. 爬虫必懂 核心常识
    1. 请求头 User-Agent：必须加，伪装浏览器
    2. 超时 timeout：防止卡死
    3. 延时 time.sleep()：防频率限制
    4. robots协议 + 合法合规：禁止爬隐私、付费、敏感数据
    5. 反爬梯度：Header → Cookie → 验证码 → 设备指纹
 
第二部分：数据分析 速成（pandas 为主）
1. Numpy 快速入门
    python
    import numpy as np

# 创建数组
arr = np.array([1,2,3,4])

# 基础运算
print(arr * 2)

# 统计
print(arr.mean(), arr.max(), arr.min())
 
2. Pandas 核心｜表格操作（重中之重）
    构建表格、读取文件
    python
    import pandas as pd

# 1. 手动构造数据表
data = {
    "姓名": ["张三","李四"],
    "分数": [88, 92]
}
df = pd.DataFrame(data)

# 2. 读取外部文件

# df = pd.read_csv("data.csv", encoding="utf-8-sig")

# df = pd.read_excel("data.xlsx")
 
常用筛选、查询
python

# 查看前几行
df.head()

# 条件筛选
high = df[df["分数"] >= 90]

# 单列取值
names = df["姓名"]

# 缺失值处理
df = df.dropna()   # 删除空值
df = df.fillna(0) # 填充空值
 
分组、统计、聚合
python

# 分组统计
df.groupby("姓名")["分数"].mean()

# 整体描述性统计
df.describe()
 
导出保存
python
df.to_csv("result.csv", index=False, encoding="utf-8-sig")
df.to_excel("result.xlsx", index=False)
 
3. 简单可视化 matplotlib
    python
    import matplotlib.pyplot as plt
    plt.rcParams["font.sans-serif"] = ["SimHei"]
    plt.rcParams["axes.unicode_minus"] = False
    x = [1,2,3]
    y = [10,20,15]
    plt.plot(x, y)
    plt.title("测试图表")
    plt.show()
 
 
三、爬虫 + 数据分析 完整闭环流程（工作标准）
1. 爬虫  requests/selenium  抓取网页原始数据
2. 解析、清洗（bs4 + 正则）
3. 存入列表/CSV
4.  pandas  读取数据、清洗异常值
5. 筛选、分组、统计、分析
6. 图表可视化 / 导出报告
 
四、你不用深挖的内容 & 必须掌握的内容
✅ 必掌握（考试/项目/实习够用）
1. requests 同步爬取 + bs4 解析
2. 正则基础文本清洗
3. pandas 增删改查、筛选、分组、读写文件
4. 简单可视化
    ❌ 暂时不用学
    - 分布式爬虫、代理池、高级加密逆向
    - 深度学习数据分析、复杂建模
 
五、极简记忆口诀
1. 静态页面用  requests ，动态JS 用  selenium 
2. 抓标签用  bs4 ，乱码文本用  正则 
3. 表格处理认准  pandas ，数值计算用  numpy 
4. 爬虫重在伪装+延时+合规，数据分析重在清洗+筛选+聚合
