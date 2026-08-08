# Selenium 基础
> 安装与 WebDriver、启动浏览器、元素定位、基础操作——"用代码开一个真浏览器"的传统方案

## 📚 目录
1. [Selenium 定位与版本（2026）](#1-selenium-定位与版本2026)
2. [安装与 WebDriver](#2-安装与-webdriver)
3. [启动与关闭浏览器](#3-启动与关闭浏览器)
4. [元素定位八法](#4-元素定位八法)
5. [基础操作：点击/输入/提取](#5-基础操作点击输入提取)
6. [Selenium 的基本局限](#6-selenium-的基本局限)

## 1. Selenium 定位与版本（2026）

| 事实 | 说明 |
|------|------|
| 版本 | Selenium 4.4x（2026）；5.0 重构中（WebDriver BiDi） |
| 定位 | 浏览器自动化老牌方案（2004 年起） |
| 本阶段角色 | 渲染方案之一（Playwright 是新项目首选，Selenium 管存量） |
| 原理 | WebDriver 协议控制真实浏览器 |

> 🎯 2026 现实：**新项目渲染优先 Playwright（[05 章](05-Playwright现代方案.md)），Selenium 用于存量代码与特定场景**——但 Selenium 的知识（定位/等待）与 Playwright 通用（locator 思想一致），先学它有铺垫价值。

## 2. 安装与 WebDriver

```bash
# 安装 Selenium
pip install selenium

# ═══ 方式一（2026 推荐）：Selenium Manager 自动管理驱动 ═══
# Selenium 4.6+ 内置 Selenium Manager：
# 首次启动自动下载对应浏览器驱动（无需手动装 chromedriver）

# ═══ 方式二：手动指定驱动（旧方式）═══
# 下载 chromedriver（与 Chrome 版本匹配）
# https://googlechromelabs.github.io/chrome-for-testing/
# 放到 PATH 或指定路径
```

```python
from selenium import webdriver
from selenium.webdriver.chrome.options import Options

# ═══ 启动 Chrome（基础）═══
driver = webdriver.Chrome()          # Selenium Manager 自动管驱动

# ═══ 常用选项 ═══
options = Options()
options.add_argument("--headless=new")       # 无头模式（服务器环境）
options.add_argument("--no-sandbox")         # 容器/root 环境
options.add_argument("--disable-gpu")        # 无 GPU 环境
options.add_argument("--window-size=1920,1080")
options.add_argument("--disable-blink-features=AutomationControlled")  # 去自动化标志

driver = webdriver.Chrome(options=options)
```

> ⚠️ **驱动版本匹配**是老坑：Chrome 升级 → chromedriver 不匹配 → 启动失败。2026 的 Selenium Manager 自动处理（方式一），**优先用自动管理**。

## 3. 启动与关闭浏览器

```python
from selenium import webdriver

driver = webdriver.Chrome()

# ═══ 打开页面 ═══
driver.get("https://example.com/list")
print(driver.title)                   # 页面标题
print(driver.current_url)             # 当前 URL

# ═══ 拿页面数据 ═══
html = driver.page_source             # 渲染后的完整 HTML（关键！）
# → 这就是"渲染后解析"的数据源（07 章）

# ═══ 关闭 ═══
driver.quit()                         # ⚠️ 必须 quit（释放浏览器进程）
# driver.close()  # 只关当前标签页（少用）
```

| 操作 | 说明 |
|------|------|
| `driver.get(url)` | 打开页面（等页面加载） |
| `driver.page_source` | **渲染后的 HTML**（爬虫的核心获取物） |
| `driver.title` / `current_url` | 元信息 |
| `driver.quit()` | 关闭浏览器（**必须**，防进程泄漏） |
| `driver.implicitly_wait(10)` | 隐式等待（04 章详讲） |

> ⚠️ **quit() 是铁律**：忘记 quit → 浏览器进程泄漏（服务器上 = 内存耗尽）。生产用 `try-finally` 或上下文管理器（04 章给规范写法）。

## 4. 元素定位八法

```python
from selenium.webdriver.common.by import By

# ═══ 八种定位方式（按优先级推荐）═══
driver.find_element(By.ID, "header")              # ID（最快）
driver.find_element(By.CLASS_NAME, "news-title")  # class
driver.find_element(By.CSS_SELECTOR, "ul.news-list li a")   # CSS（推荐主力）
driver.find_element(By.XPATH, "//div[@class='item']")       # XPath
driver.find_element(By.NAME, "username")          # name（表单）
driver.find_element(By.TAG_NAME, "h1")            # 标签
driver.find_element(By.LINK_TEXT, "下一页")        # 链接文本
driver.find_element(By.PARTIAL_LINK_TEXT, "下一")  # 链接部分文本

# ═══ 单数 vs 复数 ═══
el = driver.find_element(By.CSS_SELECTOR, ".title")       # 第一个
els = driver.find_elements(By.CSS_SELECTOR, ".title")     # 全部（列表）

# ═══ 找不到的行为 ═══
# find_element 找不到 → NoSuchElementException（需要等待，见 04）
# find_elements 找不到 → 空列表（安全）
```

| 定位方式 | 推荐度 | 说明 |
|---------|:---:|------|
| CSS_SELECTOR | ⭐ | 与 bs4 选择器同语法（阶段 2 复用） |
| XPATH | ⭐ | 复杂定位（文本/轴） |
| ID | ⭐ | 唯一元素最快 |
| CLASS_NAME | 次 | 多元素场景用 find_elements |
| LINK_TEXT | 次 | "下一页"按钮场景 |

> 🎯 技能复用：**Selenium 的 CSS/XPath 与阶段 2 完全同语法**——已学的选择器知识直接迁移；区别只是 API（`driver.find_element` vs `soup.select`）。

## 5. 基础操作：点击/输入/提取

```python
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
import time

driver.get("https://example.com/search")

# ═══ 输入 ═══
search_box = driver.find_element(By.NAME, "q")
search_box.send_keys("爬虫")               # 输入文字
search_box.send_keys(Keys.ENTER)           # 回车提交

# ═══ 点击 ═══
next_btn = driver.find_element(By.LINK_TEXT, "下一页")
next_btn.click()                            # 点击（翻页/加载更多）

# ═══ 提取文本与属性 ═══
title = driver.find_element(By.CSS_SELECTOR, "h1.title")
print(title.text)                            # 文本
print(title.get_attribute("href"))           # 属性

# ═══ 等待加载（简单版：sleep——04 章讲正规等待）═══
time.sleep(2)                                # ⚠️ 临时方案，勿用于生产
items = driver.find_elements(By.CSS_SELECTOR, "ul.news-list li")
for item in items:
    print(item.find_element(By.CSS_SELECTOR, "a").text)
```

| 操作 | 方法 | 注意 |
|------|------|------|
| 输入 | `send_keys("文字")` | 可模拟按键（Keys.ENTER） |
| 点击 | `click()` | 触发 JS 事件（加载更多） |
| 提取文本 | `element.text` | 渲染后文本 |
| 提取属性 | `get_attribute("href")` | 属性值 |
| 等待 | `time.sleep()`（临时） | **04 章换正规等待** |

> ⚠️ **`time.sleep` 是过渡品不是方案**：页面加载快慢不定，固定 sleep 要么等不够（元素还没出现 → 报错）要么白等（浪费秒级）。04 章讲"显式等待"（等元素出现，不等固定时间）。

## 6. Selenium 的基本局限

```text
Selenium 的已知短板（2026 对比 Playwright）：
  ① 等待靠 WebDriverWait（要手写条件）→ Playwright 自动等待
  ② 无内置请求拦截 → 抓接口数据要 selenium-wire（已归档！）
  ③ 每次启动一个浏览器进程 → 资源开销大
  ④ 代理配置繁琐 → Playwright 每 context 一行
  ⑤ 反检测弱 → 需 undetected-chromedriver 辅助
  ⑥ 调试弱 → 无 trace viewer（Playwright 有）

结论：新项目选 Playwright（05 章）；
      Selenium 的价值在存量代码与教学（概念通用）
```

> 🎯 本阶段学习顺序的意义：**"先学 Selenium 理解'浏览器自动化'概念，再学 Playwright 用现代工具"**——概念（定位/等待/渲染）完全通用，工具选择 2026 偏 Playwright。下一章把 Selenium 的"等待"补全（04），05 章切换到 Playwright 现代路线。

---

**下一模块**：[04-Selenium进阶等待与渲染](04-Selenium进阶等待与渲染.md) / **返回总览**：[00-阶段5动态网页爬虫总览](00-阶段5动态网页爬虫总览.md)
