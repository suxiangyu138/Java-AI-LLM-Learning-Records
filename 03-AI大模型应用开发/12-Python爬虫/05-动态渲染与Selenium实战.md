# 05 - 动态渲染与 Selenium 实战

> 🎯 现代网站大量使用 JavaScript 渲染（React/Vue/SPA），传统 requests 拿到的是空壳 HTML。Selenium 和 Playwright 通过控制真实浏览器，能处理任何 JS 动态加载的内容

---

## 目录

1. [何时需要动态渲染](#1-何时需要动态渲染)
2. [Selenium 核心操作](#2-selenium-核心操作)
3. [Playwright 现代选择](#3-playwright-现代选择)
4. [等待策略](#4-等待策略)
5. [反检测与指纹隐藏](#5-反检测与指纹隐藏)

---

## 1. 何时需要动态渲染

```text
判断流程：
├── 查看网页源代码 (Ctrl+U) → 数据在 HTML 中？
│   └── YES → 用 requests + BS4 就行了，不需要浏览器
│
├── 数据在 XHR/Fetch 中？
│   └── YES → 直接请求 API 接口（F12 Network 面板查）
│
├── 数据由 JS 动态生成？
│   └── YES → 需要 Selenium / Playwright
│
└── 有验证码/反爬？
    └── YES → 需要浏览器 + 反检测措施
```

> 🎯 **原则**：能用 requests 就不要上浏览器——浏览器慢 10-100 倍，占内存 10-20 倍

## 2. Selenium 核心操作

### 2.1 初始化

```python
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options

# 无头模式配置
options = Options()
options.add_argument("--headless=new")       # 无头模式（后台运行）
options.add_argument("--no-sandbox")
options.add_argument("--disable-dev-shm-usage")
options.add_argument("--disable-gpu")
options.add_argument("--window-size=1920,1080")
options.add_argument("--disable-blink-features=AutomationControlled")  # 反检测
options.add_experimental_option("excludeSwitches", ["enable-automation"])

driver = webdriver.Chrome(options=options)
```

### 2.2 元素定位与操作

```python
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait

# 定位元素
driver.find_element(By.ID, "username")
driver.find_element(By.CSS_SELECTOR, ".login-form input[name='email']")
driver.find_element(By.XPATH, "//button[contains(text(), '登录')]")

# 操作
elem = driver.find_element(By.ID, "search")
elem.clear()                              # 清空
elem.send_keys("Python 爬虫")              # 输入
elem.submit()                             # 提交表单

# 点击
driver.find_element(By.CSS_SELECTOR, ".submit-btn").click()

# 获取内容
elem.text             # 可见文本
elem.get_attribute("href")  # 属性值
```

### 2.3 页面信息获取

```python
# 页面源码
driver.page_source

# 当前 URL
driver.current_url

# Cookie
driver.get_cookies()

# 截图
driver.save_screenshot("debug.png")

# 执行 JS
driver.execute_script("window.scrollTo(0, document.body.scrollHeight)")
result = driver.execute_script("return document.title")
```

## 3. Playwright 现代选择

### 3.1 为什么选 Playwright

| 维度 | Selenium | Playwright |
|------|:---:|:---:|
| 速度 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| API 设计 | 老式 | 现代 async |
| 自动等待 | ❌ 需手动 | ✅ 内置 |
| 多浏览器 | Chrome/Firefox/Edge | Chromium/Firefox/WebKit |
| 移动端模拟 | ❌ | ✅ 设备模拟 |
| 网络拦截 | ❌ | ✅ 内置 |

### 3.2 Playwright 基础

```python
# pip install playwright
# playwright install chromium

from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()

    # 导航
    page.goto("https://example.com")

    # 自动等待元素出现 + 点击
    page.click("button#submit")

    # 输入
    page.fill("input[name='search']", "Python")

    # 获取文本（自动等待）
    text = page.text_content(".result-title")

    # 获取所有元素
    items = page.query_selector_all(".product-item")
    for item in items:
        name = item.query_selector(".name").text_content()

    # 截图
    page.screenshot(path="screenshot.png", full_page=True)

    browser.close()
```

### 3.3 网络拦截

```python
# 拦截 API 响应——直接从浏览器网络层抓数据
import json

def handle_response(response):
    if "/api/products" in response.url:
        data = response.json()
        print(f"捕获到 {len(data)} 条产品数据")

page.on("response", handle_response)
page.goto("https://example.com/products")
# 页面加载过程中，所有 API 响应都会被拦截
```

## 4. 等待策略

### 4.1 Selenium 等待

```python
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

# 显式等待（推荐）
wait = WebDriverWait(driver, timeout=10)

# 等待元素可见
element = wait.until(EC.visibility_of_element_located((By.ID, "result")))

# 等待元素可点击
element = wait.until(EC.element_to_be_clickable((By.CSS_SELECTOR, ".btn")))

# 等待文本出现
wait.until(EC.text_to_be_present_in_element((By.ID, "status"), "完成"))

# 等待页面加载完成
wait.until(lambda d: d.execute_script("return document.readyState") == "complete")
```

### 4.2 Playwright 等待（更简单）

```python
# Playwright 内置自动等待——默认等待元素可操作

# 等待选择器出现
page.wait_for_selector(".result-item", timeout=10000)

# 等待网络空闲
page.wait_for_load_state("networkidle")

# 等待特定响应
with page.expect_response(lambda r: "/api/data" in r.url) as response_info:
    page.click("#load-more")
response = response_info.value

# 等待指定时间（最后的兜底方案）
page.wait_for_timeout(2000)  # 不推荐，但有时必需
```

## 5. 反检测与指纹隐藏

### 5.1 Selenium 反检测

```python
# 1. 隐藏 webdriver 标记
options.add_argument("--disable-blink-features=AutomationControlled")
options.add_experimental_option("excludeSwitches", ["enable-automation"])
options.add_experimental_option("useAutomationExtension", False)

# 2. 注入 JS 隐藏特征
driver.execute_cdp_cmd("Page.addScriptToEvaluateOnNewDocument", {
    "source": """
    Object.defineProperty(navigator, 'webdriver', {get: () => undefined});
    Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5]});
    Object.defineProperty(navigator, 'languages', {get: () => ['zh-CN', 'zh']});
    """
})
```

### 5.2 Playwright 反检测（更优）

```python
# Playwright 的指纹更接近真实浏览器

browser = p.chromium.launch(
    headless=False,              # 必要时用有头模式
    args=[
        "--disable-blink-features=AutomationControlled",
        "--no-sandbox",
    ]
)

context = browser.new_context(
    viewport={"width": 1920, "height": 1080},
    user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) ...",
    locale="zh-CN",
    timezone_id="Asia/Shanghai",
)

# 注入 stealth 脚本
page.add_init_script("""
    Object.defineProperty(navigator, 'webdriver', {get: () => undefined});
""")
```

## 核心要点回顾

- 判断是否需要浏览器：查看网页源代码 → 数据在 HTML 里？→ 直接用 requests
- Playwright > Selenium：更快、API 更现代、内置自动等待、网络拦截
- 等待策略：显式等待（推荐）> 隐式等待 > `time.sleep()`（最后手段）
- 反检测三件套：隐藏 webdriver 标记 + navigator 伪造 + 真实 UA
- 能用 API 就不用浏览器——F12 Network 面板找 Ajax 接口是最优解

## 参考资料

1. Selenium 官方文档 — selenium.dev
2. Playwright 官方文档 — playwright.dev
3. Selenium Stealth — pypi.org/project/selenium-stealth
