# 05 - REST API 数据获取

> 本阶段第五课：把 API 当数据水龙头——认证、分页、限流、重试、JSON 落地——"2026 年最主流的数据交换方式——API 不会给你一张表，它给你 JSON，你把 JSON 变成表"

---

## 📚 目录

1. [API 心智与 requests 基线](#1-api-心智与-requests-基线)
2. [认证：钥匙怎么带](#2-认证钥匙怎么带)
3. [分页：数据一次给不完](#3-分页数据一次给不完)
4. [限流与重试](#4-限流与重试)
5. [JSON 解析与落地](#5-json-解析与落地)
6. [API 获取工程化](#6-api-获取工程化)
7. [练习 5 题](#7-练习-5-题)
8. [本节验收](#8-本节验收)

---

## 1. API 心智与 requests 基线

**API = 提供方把数据做成"水龙头"，你拧开接**——2026 年开放平台（天气/股票/地图/大模型）和业务系统都走 API（`../../12-Python爬虫/阶段%201：前置基础（必备）/02-HTTP协议基础爬虫视角.md` 的 HTTP 对话心智直接复用）。**API 给你的是 JSON，不是表——"把 JSON 变成 DataFrame 是本课的核心能力"**。

基线：**requests 2.34.2**（2026-05 发布——Python 生态事实标准的 HTTP 客户端）；追求异步/高性能可换 httpx（0.28.1）——**"分析师同步调用 requests 够用，异步是工程团队的活"**。

```python
import requests

resp = requests.get("https://api.example.com/v1/weather",
                    params={"city": "北京"},      # 查询参数（自动编码）
                    headers={"User-Agent": "data-analysis/1.0"},
                    timeout=(3, 15))               # 连接 3s / 读取 15s 分离
resp.raise_for_status()                            # 非 2xx 立刻抛异常（必须手动调！）
data = resp.json()                                 # JSON → Python 对象
```

**请求心智**：**`params` 传查询参数（别手拼 URL）、`timeout` 必写、`raise_for_status()` 必调**——这是分析师的请求三件套（爬虫阶段 2 的同款纪律）；**状态码先看再慌**：404 是路径错、401/403 是钥匙问题、429 是太快（本课第 4 节）、500 是对方挂了——**"报错先翻译成'谁的问题'"**（`../../12-Python爬虫/阶段%202：静态网页爬虫（入门）/02-请求进阶异常重试代理.md` 的异常体系）。

## 2. 认证：钥匙怎么带

**大部分业务 API 要钥匙（API Key）**，三种常见带法：

```python
# 方式 1：Header 带 Key（最常见）
resp = requests.get(url, headers={"X-API-Key": API_KEY})

# 方式 2：Bearer Token（OAuth 风格——大模型 API 都是这种）
resp = requests.get(url, headers={"Authorization": f"Bearer {API_KEY}"})

# 方式 3：查询参数带 Key（老平台/简单平台）
resp = requests.get(url, params={"api_key": API_KEY})
```

**认证心智**：**钥匙不要写死在代码里**——放环境变量（`import os; os.environ["API_KEY"]`）或 `.env` 文件（`.env` 要进 `.gitignore`——**"钥匙进 Git 仓库是事故，不是失误"**——`../../阶段%202：核心三大库（必学）` 无此课，但爬虫阶段 1 有同款纪律）；**先读提供方的文档确认带法**——三个平台三种姿势，猜一次错一次（401 就是猜错了）；**免费额度是有限的**——"API Key 就像银行卡，额度不是无限的"。

## 3. 分页：数据一次给不完

**接口不会一次给全部数据——分页是 API 的标准设计**，三种主流模式：

```python
# 模式 1：page 翻页（最直观——页码 + 每页条数）
for page in range(1, 100):
    resp = requests.get(url, params={"page": page, "page_size": 100}, timeout=(3, 15))
    items = resp.json()["data"]
    if not items:                     # 空页 = 拉完了
        break
    all_data.extend(items)

# 模式 2：offset 偏移（和 page 同思路——从第几条开始）
# 模式 3：cursor 游标（最现代——服务器给你"下一页的钥匙"）
next_cursor = None
while True:
    resp = requests.get(url, params={"cursor": next_cursor}, timeout=(3, 15))
    body = resp.json()
    all_data.extend(body["items"])
    next_cursor = body.get("next_cursor")   # 没有 next 就结束
    if not next_cursor:
        break
```

**分页心智**：**"先看文档确认模式，再看返回体确认结束条件"**——结束条件三种：**空页/游标为空/总数达到**——**"分页循环没写结束条件 = 无限循环事故"**；**循环里必带 `timeout` 和异常处理**（爬虫阶段 2 的重试模板直接搬）；**cursor 模式别自己拼页号**——服务器给的游标就是唯一真相——**"游标是你和服务器之间的暗号，别自己发明"**。

## 4. 限流与重试

**API 有免费额度（Rate Limit）——太快会被 429**：

```python
import time

def fetch_with_retry(url, params, max_retries=3):
    for attempt in range(max_retries):
        resp = requests.get(url, params=params, timeout=(3, 15))
        if resp.status_code == 200:
            return resp.json()
        if resp.status_code == 429:                    # 限流
            wait = int(resp.headers.get("Retry-After", 2 ** attempt))
            time.sleep(wait)                            # 指数退避：1s → 2s → 4s
            continue
        resp.raise_for_status()                         # 其他错误直接抛
    raise RuntimeError(f"重试 {max_retries} 次仍失败: {url}")
```

**限流心智**：**429 = "你太快了，等一会儿"**——听 `Retry-After` 头的话（服务器明说等几秒）；**重试用指数退避**（1s → 2s → 4s——爬虫阶段 4 的频率控制同款）；**请求之间主动加 `time.sleep(1)`**（慢一点但稳——**"分析的耐心换来数据的完整"**）；**重试只对"可重试的错"重试**：429/5xx 重试，401/403/404 重试一万次也没用（钥匙错了修钥匙，别修重试）——**"重试不是万能药，分清错误类型才有效"**。

## 5. JSON 解析与落地

**拿到的 JSON 是嵌套的——展开成表再分析**：

```python
import pandas as pd

# 列表套字典（最常见）→ 直接 DataFrame
df = pd.DataFrame(data)                 # data 是 [{...}, {...}]

# 嵌套对象 → json_normalize 展开（03 篇的正式用法）
df = pd.json_normalize(data, record_path="orders", meta=["user_id", "city"])

# 落地：API 数据先存文件再分析（别每次现拉）
df.to_csv("orders_20260810.csv", index=False, encoding="utf-8-sig")
# 或转 Parquet（大/反复用——03 篇）
df.to_parquet("orders.parquet", engine="pyarrow")
```

**落地心智**：**"先落地、再分析"是 API 获取的铁律**——数据拉到本地存一份（CSV/Parquet），分析在本地做——**好处三连：省 API 额度、分析可复现、断网不慌**（爬虫阶段 2 的"失败清单"思路）；**每次拉取记时间戳文件名**（`orders_20260810.csv`——09 篇的数据管理）；**类型要重新体检**——JSON 没有 dtype 概念，`df.info()` 看一遍，`"123"` 变 int、`"2026-08-01"` 变 datetime 是常规操作（`../../阶段%202：核心三大库（必学）/Pandas%20—%20数据分析主力库/03-数据清洗.md` 的类型校准）。

## 6. API 获取工程化

**把获取逻辑函数化——一次写好，天天复用**：

```python
def fetch_weather(city: str, start: str, end: str) -> pd.DataFrame:
    """拉取某城市某时段天气，落地 Parquet 后返回 DataFrame"""
    params = {"city": city, "start": start, "end": end}
    data = fetch_with_retry(API_URL, params)          # 认证+重试封装
    df = pd.json_normalize(data)
    df.to_parquet(f"weather_{city}_{start}_{end}.parquet", engine="pyarrow")
    return df

df = fetch_weather("北京", "2026-01-01", "2026-08-10")
```

**工程化心智**：**"函数名说清做什么、docstring 写清怎么用、返回值一定是 DataFrame"**——这是给"三个月后的自己"写的接口文档；**认证、重试、落地三件事封装一次**，调用方只管传参数——**"获取逻辑与分析逻辑分离——分析代码里不出现 requests"**；面试讲项目时"我把所有数据获取封装成统一的 fetch 函数，带重试和落地"是标准加分项。

**API 文档阅读三要点**（动手前必读，省半小时试错）：**① 看请求示例**（文档给 curl 是最快的——翻译成 requests 十秒钟的事）；**② 看返回结构**（`data` 是列表还是对象、分页字段叫什么——决定 json_normalize 的写法）；**③ 看限流规则**（每分钟多少次、免费额度多少——决定要不要写重试）——**"文档读三处：怎么发、返回啥、多快会被拦"**——**"先读文档再写代码，是 API 调用者的第一礼貌"**。

## 7. 练习 5 题

1. requests 请求三件套是哪三件？为什么 timeout 必写？
2. 三种分页模式各是什么？结束条件怎么判断？
3. 429 和 401 的处理策略有什么不同？
4. 为什么说"先落地、再分析"是 API 获取的铁律？
5. API 返回的日期字符串为什么不能直接用？

## 8. 本节验收

**验收动作**：① 找一个免费公开 API（天气/汇率/榜单都行），完成"认证 + 分页 + 重试 + 落地"全流程；② 把获取逻辑封装成函数；③ 用 json_normalize 展开嵌套返回体并体检——**"三件套 + 分页 + 落地 = 会调接口"**——**练习纪律**：全程不写死 Key（环境变量），落地文件带日期——"工程习惯从第一个练习开始"。

> 🎯 **核心要点**：requests 2.34.2 三件套（params/timeout/raise_for_status）；认证三姿势（Header Key/Bearer/参数 Key——钥匙进环境变量）；分页三模式（page/offset/cursor——写清结束条件）；429 指数退避 + 分清可重试错误；JSON 展开成表 + **先落地再分析**；获取逻辑函数化封装——**"API 给你 JSON，你把 JSON 变成表——表的尽头是落地文件"**。

---

**上一模块**：[04-数据库读取.md](./04-数据库读取.md) / **下一模块**：[06-网页数据获取最小闭环.md](./06-网页数据获取最小闭环.md)
