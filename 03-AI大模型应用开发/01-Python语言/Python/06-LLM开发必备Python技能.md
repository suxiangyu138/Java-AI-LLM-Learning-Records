# 06 - LLM 开发必备 Python 技能

> 🎯 Java 后端做 LLM 开发，Python 至少要会这 5 件事：调 API、写流式、做异步、算 Token、用 LangChain 最小集

## 1. OpenAI API 调用（同步+流式）

```python
from openai import OpenAI
client = OpenAI()

# 同步
resp = client.chat.completions.create(
    model="gpt-4o-mini",
    messages=[{"role": "user", "content": "解释 Java GC"}],
    temperature=0.7, max_tokens=512
)
print(resp.choices[0].message.content)

# 流式 → 逐 token 输出
stream = client.chat.completions.create(
    model="gpt-4o-mini", messages=[...], stream=True
)
for chunk in stream:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="")
```

## 2. 异步并发

```python
import asyncio
from openai import AsyncOpenAI

client = AsyncOpenAI()

async def process_one(prompt):
    resp = await client.chat.completions.create(
        model="gpt-4o-mini", messages=[{"role":"user","content":prompt}]
    )
    return resp.choices[0].message.content

# 并发处理 100 个请求
prompts = [f"问题{i}" for i in range(100)]
tasks = [process_one(p) for p in prompts]
results = await asyncio.gather(*tasks)  # 比串行快 20×
```

## 3. Token 计数

```python
import tiktoken

enc = tiktoken.encoding_for_model("gpt-4o")
tokens = enc.encode("解释 Java 多态")
print(f"Token数: {len(tokens)}")  # ~12 tokens

def estimate_cost(texts, model="gpt-4o-mini", price_per_1m=0.15):
    total = sum(len(enc.encode(t)) for t in texts)
    return total / 1_000_000 * price_per_1m
```

## 4. LangChain 最小集

```python
# 只需这 3 个类就能做 RAG
from langchain_community.vectorstores import Chroma
from langchain_community.embeddings import OllamaEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter

# 文档 → 切片 → Embedding → 向量库
splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)
chunks = splitter.split_text(document)

embeddings = OllamaEmbeddings(model="bge-m3")
vectorstore = Chroma.from_texts(chunks, embeddings)

# 检索 + 生成
docs = vectorstore.similarity_search("查询内容", k=5)
context = "\n".join([d.page_content for d in docs])
answer = client.chat.completions.create(
    model="gpt-4o-mini",
    messages=[{"role":"user","content":f"参考资料：{context}\n问题：查询内容"}]
)
```

## 5. 错误处理与重试

```python
import tenacity

@tenacity.retry(
    wait=tenacity.wait_exponential(min=1, max=60),
    stop=tenacity.stop_after_attempt(5),
    retry=tenacity.retry_if_exception_type(RateLimitError)
)
def call_llm(prompt):
    return client.chat.completions.create(
        model="gpt-4o", messages=[{"role":"user","content":prompt}]
    )
```
