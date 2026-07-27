# 09 - AI + 爬虫：LLM 驱动的智能数据采集

> 🎯 传统爬虫靠规则提取（CSS/XPath/Regex），遇到非结构化文本（新闻正文、评论、公告）就束手无策。LLM 可以像人一样"读懂"网页内容，从任意格式中提取结构化数据

---

## 目录

1. [LLM + 爬虫的核心价值](#1-llm--爬虫的核心价值)
2. [LLM 提取非结构化文本](#2-llm-提取非结构化文本)
3. [LLM 辅助生成解析规则](#3-llm-辅助生成解析规则)
4. [RAG 数据采集流水线](#4-rag-数据采集流水线)
5. [成本与质量控制](#5-成本与质量控制)

---

## 1. LLM + 爬虫的核心价值

```text
传统爬虫的痛点           LLM 如何解决
─────────────────────────────────────────────
规则脆弱（网站改版就挂）    → LLM"理解"页面结构，自适应变化
非结构化文本难提取       → LLM 直接阅读理解，输出 JSON
多网站结构不同，需逐一适配 → LLM 零样本泛化
数据清洗规则复杂         → LLM 自动标准化/补全
```

### 三种核心应用模式

```text
模式一：LLM 作为"智能解析器"
  爬虫获取 HTML → 提取文本 → LLM 理解 → 输出结构化 JSON

模式二：LLM 作为"规则生成器"
  给你 HTML → LLM 分析结构 → 生成 CSS/XPath 规则

模式三：LLM 作为"质量检察员"
  爬虫产出数据 → LLM 检查完整性/一致性 → 标记低质量
```

## 2. LLM 提取非结构化文本

### 2.1 从任意网页提取结构化信息

```python
import requests
from bs4 import BeautifulSoup
from openai import OpenAI

client = OpenAI(base_url="https://api.deepseek.com")

def extract_with_llm(html: str, schema: dict) -> dict:
    """用 LLM 从 HTML 中提取结构化数据"""

    # 1. 先用 BS4 提取文本（不污染 LLM 上下文）
    soup = BeautifulSoup(html, "lxml")
    # 移除 script/style 标签
    for tag in soup(["script", "style", "nav", "footer", "header"]):
        tag.decompose()
    text = soup.get_text(separator="\n", strip=True)
    # 去重空白行
    text = "\n".join(line for line in text.split("\n") if line.strip())

    # 2. LLM 理解并提取
    prompt = f"""
从以下网页文本中提取结构化信息。

要求的输出 Schema：
{json.dumps(schema, ensure_ascii=False, indent=2)}

网页内容：
{text[:8000]}   ← 截断，控制 token 消耗

只返回 JSON，不要任何解释。
"""

    response = client.chat.completions.create(
        model="deepseek-chat",
        messages=[{"role": "user", "content": prompt}],
        temperature=0.1,       # 低温度 = 稳定输出
        response_format={"type": "json_object"}
    )
    return json.loads(response.choices[0].message.content)


# 使用示例：提取商品信息
schema = {
    "type": "object",
    "properties": {
        "products": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "price": {"type": "number"},
                    "description": {"type": "string"},
                    "specs": {"type": "object"}
                }
            }
        }
    }
}

html = requests.get("https://example.com/product-page").text
data = extract_with_llm(html, schema)
```

### 2.2 批量提取：成本优化

```python
def extract_batch_with_cache(htmls: list[str], schema: dict) -> list[dict]:
    """带缓存的批量 LLM 提取"""
    results = []
    cache = {}  # 简单内存缓存（生产用 Redis）

    for i, html in enumerate(htmls):
        # 1. 提取文本
        text = extract_text(html)

        # 2. 缓存检查
        text_hash = hashlib.md5(text.encode()).hexdigest()
        if text_hash in cache:
            results.append(cache[text_hash])
            continue

        # 3. LLM 提取
        data = extract_with_llm(text, schema)
        cache[text_hash] = data
        results.append(data)

        # 4. 成本日志
        print(f"[{i+1}/{len(htmls)}] 完成")

    return results
```

## 3. LLM 辅助生成解析规则

### 3.1 从示例 HTML 生成 XPath

```python
def generate_xpath_with_llm(html_snippet: str, target_data: str) -> str:
    """让 LLM 分析 HTML 结构，生成 XPath 提取规则"""

    prompt = f"""
分析以下 HTML 片段，找到提取 "{target_data}" 的 XPath 表达式。

HTML:
```html
{html_snippet}
```

要求：
1. 返回最精准的 XPath（不要太泛，也不要太具体）
2. 优先使用 class/id 属性
3. 返回格式：xpath: //div[@class='XXX']/text()
"""

    response = client.chat.completions.create(
        model="deepseek-chat",
        messages=[{"role": "user", "content": prompt}]
    )
    return response.choices[0].message.content.strip()
```

### 3.2 自动生成爬虫代码

```python
def generate_spider_code(url: str, sample_html: str) -> str:
    """让 LLM 分析样本页面，自动生成 Scrapy Spider 代码"""

    prompt = f"""
根据以下网页样本，生成一个 Scrapy Spider。

目标 URL 模式: {url}
样本 HTML（前 2000 字符）:
```html
{sample_html[:2000]}
```

要求：
1. 生成完整的 Spider 类
2. 提取所有可见的数据字段
3. 包含翻页逻辑
4. 使用 CSS 选择器
5. 只返回 Python 代码，不要解释
"""

    response = client.chat.completions.create(
        model="deepseek-chat",
        messages=[{"role": "user", "content": prompt}]
    )
    return response.choices[0].message.content
```

## 4. RAG 数据采集流水线

```text
完整的 RAG 数据采集流水线：

┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  爬虫     │ →  │ LLM 清洗  │ →  │ 文本分块  │ →  │ 向量存储  │
│  采集     │    │ 结构化    │    │ Chunking  │    │ ChromaDB  │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
```

```python
# 完整 RAG 数据采集流水线
def rag_data_pipeline(urls: list[str]) -> None:
    """RAG 数据采集 + 清洗 + 入库"""
    import chromadb
    from openai import OpenAI

    client = OpenAI()
    chroma = chromadb.PersistentClient(path="./rag_data")
    collection = chroma.get_or_create_collection("knowledge_base")

    for url in urls:
        # 1. 爬取
        html = requests.get(url).text
        text = extract_text(html)

        # 2. LLM 清洗（只保留正文）
        cleaned = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{
                "role": "user",
                "content": f"去除以下网页的导航、广告、页脚等噪声，只保留正文内容。如内容不相关（不是知识性内容），返回 SKIP。\n\n{text[:6000]}"
            }]
        ).choices[0].message.content

        if "SKIP" in cleaned:
            continue

        # 3. 分块
        chunks = [cleaned[i:i+500] for i in range(0, len(cleaned), 500)]

        # 4. 向量化 + 存储
        for i, chunk in enumerate(chunks):
            embedding = client.embeddings.create(
                model="text-embedding-3-small",
                input=chunk
            ).data[0].embedding

            collection.add(
                embeddings=[embedding],
                documents=[chunk],
                metadatas=[{"source": url, "chunk": i}],
                ids=[f"{hash(url)}_{i}"]
            )
```

## 5. 成本与质量控制

### 5.1 LLM 提取成本估算

| 模型 | 1000 个网页的成本 (每页~2000 tokens) |
|------|:---:|
| GPT-4o | ~$15-30 |
| GPT-4o-mini | ~$1-3 |
| DeepSeek V3 | **~$0.50-1.50** |
| Claude Haiku | ~$2-5 |

> 🎯 **LLM 提取的数据价值通常远超成本**——1000 条高质量结构化数据，人工收集可能需要 100+ 小时

### 5.2 质量控制策略

```python
def llm_quality_check(data: list[dict], sample_size: int = 50) -> dict:
    """用 LLM 自动抽检数据质量"""
    import random

    samples = random.sample(data, min(sample_size, len(data)))

    prompt = f"""
检查以下 {len(samples)} 条数据，评估质量：

数据样本：
{json.dumps(samples[:5], ensure_ascii=False, indent=2)}  ← 只发 5 条样本

返回 JSON：
{{
  "completeness": 0-100,  // 字段完整度
  "accuracy": 0-100,       // 数据准确度
  "consistency": 0-100,    // 格式一致性
  "issues": ["问题1", "问题2"]
}}
"""

    response = client.chat.completions.create(
        model="gpt-4o-mini",  # 质量检查用小模型即可
        messages=[{"role": "user", "content": prompt}],
        response_format={"type": "json_object"}
    )
    return json.loads(response.choices[0].message.content)
```

## 核心要点回顾

- LLM + 爬虫三模式：智能解析器 / 规则生成器 / 质量检察员
- 用 LLM 提取前先 BS4 去噪（减少 token 消耗 80%+）
- 缓存机制：相同文本哈希 → 跳过 LLM 调用
- RAG 流水线：爬取 → LLM 清洗 → 分块 → 向量化 → 入库
- 成本：DeepSeek V3 提取 1000 个网页只需 ~$1
- 质量：用低端模型（mini）做质量抽检，高价值数据用强模型

## 参考资料

1. OpenAI Structured Outputs 文档
2. LangChain Document Loaders
3. ChromaDB 官方文档
