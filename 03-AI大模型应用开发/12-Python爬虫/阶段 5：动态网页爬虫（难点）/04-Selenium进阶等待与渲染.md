# Selenium 进阶：等待与渲染
> 显式等待（WebDriverWait）、隐式等待、动态内容获取、页面交互——"等对"是动态页爬虫的灵魂

## 📚 目录
1. [为什么等待是灵魂](#1-为什么等待是灵魂)
2. [显式等待：WebDriverWait](#2-显式等待webdriverwait)
3. [隐式等待与全局设置](#3-隐式等待与全局设置)
4. [等待策略对比与选型](#4-等待策略对比与选型)
5. [动态内容获取：加载更多/滚动](#5-动态内容获取加载更多滚动)
6. [Selenium 渲染爬虫模板](#6-selenium-渲染爬虫模板)

## 1. 为什么等待是灵魂

```text
动态页的时序问题：
  打开页面 → JS 执行 → 接口返回 → 渲染 DOM（需要时间！）
  立即 find_element → 元素还没渲染 → NoSuchElementException

三种等待策略：
  ① sleep 固定时间：等不够报错 / 等多了浪费（不准）
  ② 隐式等待：轮询找元素，超时才报错（全局）
  ③ 显式等待：等"某个条件成立"（精准，推荐）

动态页爬虫的 90% 报错 = 没等对
  （NoSuchElementException / StaleElementReferenceException）
```

> 🎯 认知：**"动态页爬虫的核心技能不是定位，是等待"**——定位语法阶段 2 就会了，等待策略才是"渲染方案"与"静态方案"的分水岭。

## 2. 显式等待：WebDriverWait

```python
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By

driver.get("https://example.com/list")

# ═══ 显式等待：等元素出现（最多 10 秒，出现即继续）═══
wait = WebDriverWait(driver, 10)
element = wait.until(
    EC.presence_of_element_located((By.CSS_SELECTOR, "ul.news-list li"))
)
# 元素出现 → 立即继续（不等满 10 秒）

# ═══ 常用等待条件 ═══
wait.until(EC.presence_of_element_located((By.ID, "data")))        # 元素出现
wait.until(EC.visibility_of_element_located((By.CSS_SELECTOR, ".item")))  # 可见
wait.until(EC.element_to_be_clickable((By.LINK_TEXT, "下一页")))   # 可点击
wait.until(EC.text_to_be_present_in_element((By.ID, "total"), "100"))  # 文本出现

# ═══ 等不到 → TimeoutException ═══
from selenium.common.exceptions import TimeoutException
try:
    wait.until(EC.presence_of_element_located((By.ID, "data")))
except TimeoutException:
    print("数据 10 秒内未加载——页面结构变了或加载失败")
```

| 等待条件（EC） | 含义 |
|---------------|------|
| `presence_of_element_located` | 元素在 DOM 中出现 |
| `visibility_of_element_located` | 元素可见（有大小） |
| `element_to_be_clickable` | 可点击（可见 + 可用） |
| `text_to_be_present_in_element` | 元素文本包含某内容 |
| `staleness_of` | 元素失效（页面刷新后） |

> 🎯 显式等待哲学：**"等'条件'而不是等'时间'"**——数据加载完成的条件（元素出现）成立就继续，不成立最多等 N 秒报错。这比 sleep 快且稳（页面快 1 秒就省 1 秒）。

## 3. 隐式等待与全局设置

```python
# ═══ 隐式等待：全局轮询（每次 find 都生效）═══
driver = webdriver.Chrome()
driver.implicitly_wait(10)           # 所有 find_element 最多等 10 秒

# 行为：find_element 找不到 → 每 500ms 重试 → 10 秒超时报错
# 适用：页面加载总体稳定、简单场景

# ═══ 显式 + 隐式混用注意 ═══
# 两者可共存（显式优先），但：
# 隐式等待过大（30s）会让"找不到"的报错也等 30 秒（排查慢）
# 建议：隐式 5-10s + 关键元素显式等待

# ═══ 页面加载超时设置 ═══
driver.set_page_load_timeout(30)     # 页面加载超时（防挂死）
```

| 等待类型 | 范围 | 特点 |
|---------|------|------|
| 隐式等待 | 全局（所有 find） | 简单但"盲等" |
| **显式等待** | 指定条件 | **精准可控（推荐主力）** |
| sleep | 固定时间 | 反模式（仅调试） |

> 🎯 选型结论：**"显式等待是生产标准，隐式等待做兜底，sleep 只用于调试"**——动态页爬虫的规范写法：隐式 5s 兜底 + 关键数据元素显式等待。

## 4. 等待策略对比与选型

| 策略 | 等什么 | 优点 | 缺点 | 适用 |
|------|--------|------|------|------|
| sleep(2) | 固定时间 | 简单 | 不准（快慢都不对） | 调试 |
| 隐式等待 | 任意元素 | 全局生效 | 盲等（不知道等哪个） | 兜底 |
| **显式等待** | 指定条件 | **精准** | 要写条件 | **生产主力** |
| 混合 | 隐式 + 显式 | 稳 | 配置需谨慎 | **推荐** |

```python
# ═══ 生产标准配置 ═══
driver.implicitly_wait(5)                    # 全局兜底 5s
wait = WebDriverWait(driver, 15)             # 关键元素 15s
data = wait.until(EC.presence_of_element_located(
    (By.CSS_SELECTOR, "ul.news-list li")))   # 数据出现才算加载完
```

> 🎯 经验值：**"关键数据元素等待 10-15s，其他走隐式 5s"**——页面加载失败/被验证码拦截时，等待超时 + 检查 page_source（看是不是被拦了）是标准排障路径。

## 5. 动态内容获取：加载更多/滚动

```python
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import time

# ═══ 模式一：点击"加载更多" ═══
wait = WebDriverWait(driver, 10)
for _ in range(5):                          # 最多点 5 次
    try:
        btn = wait.until(EC.element_to_be_clickable(
            (By.CSS_SELECTOR, "button.load-more")))
        btn.click()
        wait.until(EC.presence_of_element_located(          # 等新内容
            (By.CSS_SELECTOR, "ul.news-list li:nth-child(n)")))
        time.sleep(1)                       # 渲染缓冲
    except Exception:
        break                               # 按钮没了/不可点 = 到底

# ═══ 模式二：滚动加载（无限滚动）═══
from selenium.webdriver.common.action_chains import ActionChains

for _ in range(10):
    # 滚到底部
    driver.execute_script("window.scrollTo(0, document.body.scrollHeight)")
    time.sleep(1.5)                         # 等新内容加载
    # 检查是否到底（高度不再变化）
    height = driver.execute_script("return document.body.scrollHeight")
    if height == last_height:
        break
    last_height = height

# ═══ 提取最终内容 ═══
items = driver.find_elements(By.CSS_SELECTOR, "ul.news-list li")
print(f"共加载 {len(items)} 条")
```

| 动态加载模式 | 实现 |
|-------------|------|
| 点击加载更多 | 循环点按钮 + 等新元素 |
| 无限滚动 | 滚动到底 + 高度对比终止 |
| 下拉框筛选 | select 元素 + 触发 change |

> ⚠️ 动态加载的终止判断：**"等'加载不出来了'为止"**——按钮不可点/高度不变/新元素不再出现，三者都是"到底了"的信号；配合页数上限防死循环（阶段 2 §6 的思维迁移）。

## 6. Selenium 渲染爬虫模板

```python
"""Selenium 渲染爬虫标准模板"""
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC


class SeleniumSpider:
    def __init__(self, headless=True):
        options = Options()
        if headless:
            options.add_argument("--headless=new")
        options.add_argument("--no-sandbox")
        options.add_argument("--disable-gpu")
        self.driver = webdriver.Chrome(options=options)
        self.driver.implicitly_wait(5)          # 全局兜底

    def fetch_rendered(self, url, selector, timeout=15):
        """打开页面 → 等数据元素 → 返回渲染后 HTML"""
        self.driver.get(url)
        try:
            WebDriverWait(self.driver, timeout).until(
                EC.presence_of_element_located((By.CSS_SELECTOR, selector))
            )
        except Exception as e:
            print(f"等待超时: {url} — {e}（可能被验证码拦截）")
        return self.driver.page_source          # 渲染后 HTML

    def close(self):
        self.driver.quit()                      # ⚠️ 必须关闭

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.close()


# 使用（with 自动关闭）
with SeleniumSpider(headless=True) as spider:
    html = spider.fetch_rendered(
        "https://example.com/list",
        selector="ul.news-list li",             # 数据元素
    )
    # html → bs4/lxml 解析（阶段 2 技能）
    from bs4 import BeautifulSoup
    soup = BeautifulSoup(html, "lxml")
    for li in soup.select("ul.news-list li"):
        print(li.select_one("a").text)
```

> 🎯 模板要点：**"封装成类 + with 管理 + 等待内建"**——`fetch_rendered`（等数据元素 + 返回渲染 HTML）是 Selenium 爬虫的核心抽象；解析交给阶段 2 的 bs4（渲染后 HTML 就是静态 HTML）。

---

**下一模块**：[05-Playwright现代方案](05-Playwright现代方案.md) / **返回总览**：[00-阶段5动态网页爬虫总览](00-阶段5动态网页爬虫总览.md)
