# 10 - Python 脚本实战集

> 🎯 10 个高频 Python 脚本模板 — 从 API 批量调用到日志分析，拿来即用

## 1. 批量 API 调用

```python
import asyncio
from openai import AsyncOpenAI

async def batch_process(prompts, concurrency=20):
    client = AsyncOpenAI()
    sem = asyncio.Semaphore(concurrency)
    async def one(prompt):
        async with sem:
            r = await client.chat.completions.create(
                model="gpt-4o-mini", messages=[{"role":"user","content":prompt}]
            )
            return r.choices[0].message.content
    return await asyncio.gather(*[one(p) for p in prompts])

results = asyncio.run(batch_process(["问题1","问题2","问题3"]))
```

## 2. CSV 数据分析

```python
import pandas as pd
df = pd.read_csv("results.csv")
print(df.describe())
df[df["cost"] > 1.0].to_csv("expensive_requests.csv", index=False)
```

## 3. 日志解析

```python
import re
from collections import Counter
errors = Counter()
with open("app.log") as f:
    for line in f:
        if "ERROR" in line:
            m = re.search(r"(\w+Error): (.+)", line)
            if m: errors[m.group(1)] += 1
print(errors.most_common(10))
```

## 4. 文件批量重命名

```python
from pathlib import Path
for i, f in enumerate(Path("docs").glob("*.md")):
    f.rename(f.parent / f"{i:03d}-{f.name}")
```

## 5. JSON 转 CSV

```python
import json, csv
data = [json.loads(line) for line in open("data.jsonl")]
with open("data.csv", "w") as f:
    w = csv.DictWriter(f, fieldnames=data[0].keys())
    w.writeheader(); w.writerows(data)
```

## 6. 进度条

```python
from tqdm import tqdm
for item in tqdm(items, desc="Processing"):
    process(item)
```

## 7. 环境变量管理

```python
import os
from dotenv import load_dotenv
load_dotenv()  # 加载 .env 文件
api_key = os.environ["OPENAI_API_KEY"]
```

## 8. 定时任务

```python
import schedule, time
def job(): print("执行定时任务")
schedule.every().hour.do(job)
while True: schedule.run_pending(); time.sleep(1)
```

## 9. 解压/压缩

```python
import zipfile, tarfile
with zipfile.ZipFile("archive.zip") as z: z.extractall("output/")
with tarfile.open("archive.tar.gz") as t: t.extractall("output/")
```

## 10. 简单的 HTTP 服务

```bash
python -m http.server 8080     # 当前目录 → HTTP 服务
python -m json.tool data.json  # JSON 格式化
```
