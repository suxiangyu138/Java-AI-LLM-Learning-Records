# 阶段 3：标准库与日常脚本

> **目标**：能用 Python 标准库快速解决日常开发中的实际问题
> **核心**：路径处理、文件操作、正则、JSON/CSV、子进程调用、日志
> **核心理念**：Python 的生产力来自库生态，标准库又是其中最稳定的部分

---

## 📌 章节定位

如果说阶段 2 让你"写得好"，阶段 3 就是让你"写得快"。Python 标准库覆盖了日常脚本 90% 的需求——不需要引入第三方依赖就能处理文件、解析配置、调用外部程序。

对 Java 开发者来说，这些东西对应 Apache Commons、Jackson、SLF4J 等，但 Python 很多情况下**标准库就够了**。

---

## 🎯 核心章节

### 1. 路径与文件系统操作

```python
# pathlib（Python 3.4+，⭐ 官方推荐替代 os.path）
from pathlib import Path

# 路径创建（跨平台！）
p = Path("data") / "raw" / "input.csv"
# Windows: data\raw\input.csv
# Linux:   data/raw/input.csv

# 路径属性
print(p.name)            # input.csv
print(p.stem)            # input
print(p.suffix)          # .csv
print(p.parent)          # data\raw
print(p.parts)           # ('data', 'raw', 'input.csv')
print(p.exists())        # True/False
print(p.is_file())       # True/False
print(p.is_dir())        # True/False

# 遍历目录
for f in Path("data").glob("**/*.csv"):     # ⭐ 递归 glob
    print(f)
for f in Path("data").rglob("*.csv"):       # 等价于 **/*.csv
    print(f)

# 文件操作
p = Path("output.txt")
p.write_text("Hello\n世界", encoding="utf-8")  # 写文本（自动处理 with open）
content = p.read_text(encoding="utf-8")         # 读文本
p.write_bytes(b"binary data")                   # 写二进制
data = p.read_bytes()                           # 读二进制

# 目录操作
Path("new_dir").mkdir(exist_ok=True)            # 创建目录
Path("nested/dirs").mkdir(parents=True, exist_ok=True)  # 递归创建

# 实用操作
p.rename("new_name.txt")                        # 重命名/移动
p.unlink()                                      # 删除文件（missing_ok=True 可选）
p.stat()                                        # 文件元信息（大小、修改时间等）

# 获取当前脚本目录（⭐ 常见需求）
SCRIPT_DIR = Path(__file__).resolve().parent
```

#### os/shutil 补充（需要时再查）

```python
import os
import shutil

os.getcwd()                     # 当前工作目录
os.chdir("/path/to/dir")        # 切换目录
os.listdir(".")                 # 列出目录内容
os.environ                       # 环境变量 dict
os.environ.get("HOME")          # 获取特定环境变量

shutil.copy("src", "dst")       # 复制文件
shutil.copytree("src", "dst")   # 递归复制目录
shutil.rmtree("dir")            # 递归删除目录（⚠️ 不可逆）
shutil.move("src", "dst")       # 移动
```

### 2. 命令行参数与交互

```python
# sys 模块 —— 基础访问
import sys

print(sys.argv)                  # ['script.py', 'arg1', 'arg2']
print(sys.platform)              # 'linux' / 'darwin' / 'win32'
print(sys.version)               # Python 版本

# 临时重定向 stdout（捕获输出）
import io
buffer = io.StringIO()
sys.stdout = buffer
print("captured")
output = buffer.getvalue()       # 'captured\n'

# argparse —— ⭐ 命令行参数解析（标准库）
import argparse

parser = argparse.ArgumentParser(
    description="批量处理 CSV 文件",
    epilog="示例: python process.py data/ -o output/ --verbose"
)
parser.add_argument("input_dir", help="输入目录路径")
parser.add_argument("-o", "--output", default="output", help="输出目录")
parser.add_argument("-v", "--verbose", action="store_true", help="详细输出")
parser.add_argument("-n", "--count", type=int, default=10, help="处理数量")
parser.add_argument("--pattern", choices=["csv", "json", "xml"], default="csv")
args = parser.parse_args()

print(args.input_dir)            # 位置参数
print(args.output)               # -o/--output 的值
print(args.verbose)              # -v 是否出现（True/False）

# click 库（第三方，但更简洁，推荐用于复杂 CLI）
# import click  ← pip install click
```

### 3. 正则表达式

```python
import re

text = "订单号: ORD-2024-001, 金额: ¥99.50, 日期: 2024-03-15"
# 另两单: ORD-2024-042, ORD-2024-103

# 常用方法
pattern = r"ORD-\d{4}-\d{3}"     # ⭐ r"..." 原始字符串，避免转义问题

# 搜索——找到第一个匹配
match = re.search(pattern, text)
if match:
    print(match.group())         # ORD-2024-001
    print(match.start())         # 匹配起始位置
    print(match.end())           # 匹配结束位置

# 查找所有匹配
orders = re.findall(pattern, text)
print(orders)                    # ['ORD-2024-001']

# 替换
clean = re.sub(r"ORD-\d{4}-\d{3}", "[订单号]", text)

# 编译正则（重复使用时的优化）
order_re = re.compile(r"ORD-(?P<year>\d{4})-(?P<seq>\d{3})")
match = order_re.search(text)
if match:
    print(match.group("year"))   # 2024    (命名组)
    print(match.group("seq"))    # 001

# 常用模式速查
# \d    数字        \w    字母数字下划线      \s    空白字符
# \D    非数字      \W    非字母数字下划线    \S    非空白字符
# .     任意字符    ^     行首              $     行尾
# *     0-n 次     +     1-n 次            ?     0-1 次
# {n}   精确 n 次  {m,n} m-n 次            *?    非贪婪
# [abc] 字符类    [^abc] 否定字符类        (a|b) 或
```

### 4. JSON / CSV / 配置文件

```python
# --- JSON ---
import json

# Python ↔ JSON 映射
# dict ←→ object, list ←→ array, str ←→ string
# int/float ←→ number, True/False ←→ true/false, None ←→ null

data = {"users": [{"name": "Alice", "age": 25}], "count": 1}

# 序列化
json_str = json.dumps(data, indent=2, ensure_ascii=False)
print(json_str)
# {
#   "users": [
#     {
#       "name": "Alice",
#       "age": 25
#     }
#   ],
#   "count": 1
# }

# 写入文件
with open("data.json", "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)

# 反序列化
with open("data.json", "r", encoding="utf-8") as f:
    loaded = json.load(f)
print(loaded["users"][0]["name"])  # Alice

# 自定义序列化（处理 datetime 等非标准类型）
from datetime import datetime

def custom_encoder(obj):
    if isinstance(obj, datetime):
        return obj.isoformat()
    raise TypeError(f"Object of type {type(obj)} is not JSON serializable")

json.dumps({"timestamp": datetime.now()}, default=custom_encoder)

# --- CSV ---
import csv

# 读取 CSV
with open("data.csv", "r", newline="", encoding="utf-8") as f:
    reader = csv.DictReader(f)           # 每行变成 dict，列名作为 key
    for row in reader:
        print(row["name"], row["age"])   # 按列名访问

# 写入 CSV
with open("output.csv", "w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=["name", "score"])
    writer.writeheader()
    writer.writerow({"name": "Alice", "score": 95})
    writer.writerows([
        {"name": "Bob", "score": 87},
        {"name": "Charlie", "score": 92},
    ])

# --- 配置文件 ---
# Python 原生读取 .env 风格或用第三方 python-dotenv
# 更推荐：用 YAML 或 TOML 做配置

# TOML 配置（Python 3.11+ 内置 tomllib）
# config.toml:
#   [server]
#   host = "0.0.0.0"
#   port = 8080
#
#   [ai]
#   model = "gpt-4"
#   temperature = 0.7
import tomllib  # Python 3.11+
# import tomli  # Python < 3.11，pip install tomli

with open("config.toml", "rb") as f:
    config = tomllib.load(f)
print(config["server"]["port"])  # 8080
```

### 5. 日期时间处理

```python
from datetime import datetime, date, timedelta, timezone
from zoneinfo import ZoneInfo  # Python 3.9+

# 当前时间
now = datetime.now()                     # 本地时间（无时区信息）
now_utc = datetime.now(timezone.utc)     # UTC 时间（⭐ 推荐存储时区信息）

# 字符串 ↔ datetime
dt = datetime.strptime("2024-03-15 14:30", "%Y-%m-%d %H:%M")
s = dt.strftime("%Y年%m月%d日 %H:%M")    # 2024年03月15日 14:30

# 常用格式化符号
# %Y  四位年份  %m  月份(01-12)  %d  日期(01-31)
# %H  小时(00-23) %M  分钟      %S  秒
# %f  微秒      %z  时区偏移    %A  星期几

# 时间计算
tomorrow = now + timedelta(days=1)
one_hour_ago = now - timedelta(hours=1)
diff = datetime(2024, 12, 31) - datetime(2024, 1, 1)
print(diff.days)                         # 365

# 时间戳
timestamp = now.timestamp()              # datetime → Unix 时间戳
dt_from_ts = datetime.fromtimestamp(timestamp)

# 时区转换（Python 3.9+）
beijing_tz = ZoneInfo("Asia/Shanghai")
bj_now = datetime.now(beijing_tz)
print(bj_now.isoformat())                # 2024-03-15T14:30:00+08:00
```

### 6. 子进程调用（⭐ 整合 Java 工具链的关键）

```python
import subprocess
import shlex

# --- 基本用法 ---
# 执行命令并等待完成
result = subprocess.run(
    ["python", "--version"],
    capture_output=True,     # 捕获 stdout 和 stderr
    text=True,               # 以文本而非 bytes 返回
    check=True,              # 非零退出码抛异常
)
print(result.stdout)
print(result.returncode)     # 0 表示成功

# --- 管道 —— 调用 Java ---
# 例：编译并运行一个 Java 类
result = subprocess.run(
    ["javac", "Main.java"],
    cwd="./java_project",    # 设置工作目录
    capture_output=True,
    text=True,
)
if result.returncode != 0:
    print(f"编译失败:\n{result.stderr}")
else:
    result = subprocess.run(
        ["java", "Main"],
        input="hello\n",     # 向 stdin 发送数据
        capture_output=True,
        text=True,
        cwd="./java_project",
    )
    print(result.stdout)

# --- 长时间运行的进程 ---
# 例：启动 Java 微服务，读取日志
proc = subprocess.Popen(
    ["java", "-jar", "app.jar"],
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
    text=True,
    bufsize=1,
)

# 逐行读取输出
for line in proc.stdout:
    if "Started" in line:
        print("服务启动完成！")
        break

proc.terminate()             # 优雅终止
# proc.kill()                # 强制终止

# --- 带超时的执行 ---
try:
    result = subprocess.run(
        ["long_running_command"],
        timeout=30,          # 30 秒超时
        capture_output=True,
        text=True,
    )
except subprocess.TimeoutExpired:
    print("命令执行超时")

# --- shlex：安全处理带参数的命令 ---
# 如果你拿到的是一个命令字符串而非列表
cmd_str = 'java -jar app.jar --port 8080'
cmd_list = shlex.split(cmd_str)
# ['java', '-jar', 'app.jar', '--port', '8080']

# ⚠️ 安全提醒：绝不使用 shell=True 拼接用户输入
# subprocess.run(f"rm -rf {user_input}", shell=True)  ← 高危！
```

### 7. 日志

```python
import logging

# ⭐ 最佳实践：使用模块级 logger
logger = logging.getLogger(__name__)

# 基本配置（应用入口处设置一次）
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
    handlers=[
        logging.FileHandler("app.log", encoding="utf-8"),
        logging.StreamHandler(),               # 同时输出到控制台
    ],
)

# 使用
logger.debug("详细调试信息")
logger.info("服务启动，端口 8080")
logger.warning("磁盘使用率超过 80%")
logger.error("数据库连接失败")
logger.exception("捕获异常并记录堆栈")   # 在 except 块中使用
```

### 8. 其他高频标准库速查

```python
# --- collections ---
from collections import defaultdict, Counter, deque, namedtuple

# Counter：统计频率（⭐ 极其常用）
words = ["a", "b", "a", "c", "b", "a"]
counts = Counter(words)
print(counts)                    # Counter({'a': 3, 'b': 2, 'c': 1})
print(counts.most_common(2))     # [('a', 3), ('b', 2)]

# defaultdict：带默认值的字典
d = defaultdict(list)            # 访问不存在的 key 时自动创建 list
for word, count in [("a", 1), ("b", 2), ("a", 3)]:
    d[word].append(count)
# d["a"] → [1, 3]

# deque：双端队列（两端 O(1) 操作）
dq = deque([1, 2, 3])
dq.appendleft(0)                 # deque([0, 1, 2, 3])
dq.pop()                         # 3
dq.popleft()                     # 0

# --- itertools ---
import itertools

# 笛卡尔积
list(itertools.product("AB", "12"))  # [('A','1'),('A','2'),('B','1'),('B','2')]

# 排列与组合
list(itertools.permutations("ABC", 2))  # 排列
list(itertools.combinations("ABC", 2))  # 组合

# 无限迭代器（需配合 islice 截断）
list(itertools.islice(itertools.count(10, 2), 5))  # [10, 12, 14, 16, 18]

# --- functools ---
from functools import lru_cache, partial, reduce

# lru_cache：函数结果缓存（⭐ 记忆化搜索/性能优化神器）
@lru_cache(maxsize=128)
def fib(n):
    if n < 2: return n
    return fib(n-1) + fib(n-2)

# partial：部分应用函数参数
def multiply(x, y):
    return x * y

double = partial(multiply, 2)    # double(5) → 10

# --- random ---
import random

random.seed(42)                  # 设定种子（可复现）
random.randint(1, 10)           # [1, 10] 随机整数
random.random()                  # [0.0, 1.0) 随机浮点
random.choice(["a", "b", "c"])   # 随机选择一个元素
random.shuffle(my_list)          # 原地打乱列表（返回 None！）
random.sample(my_list, k=3)      # 不放回抽样 k 个

# --- hashlib ---
import hashlib

content = "hello world"
h = hashlib.sha256(content.encode()).hexdigest()
print(h)  # SHA256 哈希值（64 个十六进制字符）

# 逐块计算（大文件适用）
sha = hashlib.sha256()
with open("large_file.bin", "rb") as f:
    while chunk := f.read(8192):
        sha.update(chunk)
print(sha.hexdigest())

# --- tempfile ---
import tempfile

with tempfile.NamedTemporaryFile(
    suffix=".txt", delete=False
) as tmp:
    tmp.write(b"temporary data")
    print(tmp.name)              # 临时文件路径
# 程序结束后需手动清理
```

---

## 🛠️ 实战脚本模板

### 模板 1：批量文件处理脚本

```python
#!/usr/bin/env python3
"""批量重命名文件 —— 为所有 .txt 文件添加序号前缀。"""
import argparse
from pathlib import Path

def main():
    parser = argparse.ArgumentParser(description="批量重命名文件")
    parser.add_argument("directory", type=Path, help="目标目录")
    parser.add_argument("--ext", default=".txt", help="文件扩展名")
    parser.add_argument("--dry-run", action="store_true", help="预览模式")
    args = parser.parse_args()

    files = sorted(args.directory.glob(f"*{args.ext}"))
    for i, f in enumerate(files, 1):
        new_name = f.parent / f"{i:03d}_{f.name}"
        if args.dry_run:
            print(f"[预览] {f.name} → {new_name.name}")
        else:
            f.rename(new_name)
            print(f"[完成] {f.name} → {new_name.name}")

    print(f"处理完成，共 {len(files)} 个文件")

if __name__ == "__main__":
    main()
```

### 模板 2：日志分析脚本

```python
#!/usr/bin/env python3
"""分析 Java 应用日志，统计 ERROR 分布。"""
import re
from pathlib import Path
from collections import Counter

LOG_DIR = Path("logs")
ERROR_PATTERN = re.compile(r"\[ERROR\]\s+(.+)")  # 提取错误信息

def analyze_logs():
    error_counter = Counter()
    error_files = Counter()

    for log_file in LOG_DIR.glob("*.log"):
        try:
            content = log_file.read_text(encoding="utf-8")
            for match in ERROR_PATTERN.finditer(content):
                error_msg = match.group(1)[:80]  # 截取前 80 字符
                error_counter[error_msg] += 1
                error_files[log_file.name] += 1
        except Exception as e:
            print(f"读取 {log_file} 失败: {e}")

    print("=== TOP 10 高频错误 ===")
    for msg, count in error_counter.most_common(10):
        print(f"[{count:4d}] {msg}")

    print("\n=== 各文件错误分布 ===")
    for fname, count in error_files.most_common():
        print(f"  {fname}: {count}")

if __name__ == "__main__":
    analyze_logs()
```

---

## ✅ 阶段验收

1. 写一个脚本：扫描项目中所有 `.java` 文件，统计代码行数（去掉空行和注释）
2. 写一个脚本：读取 JSON 配置文件，通过 subprocess 调用 Java 程序，传入解析后的参数
3. 写一个脚本：从 CSV 文件中读取数据，按某列分组聚合，输出结果 JSON

---

> **上一阶段** ← [02-进阶语法与工程基础](02-进阶语法与工程基础.md)
> **下一阶段** → [04-AI 与数据科学生态](04-AI与数据科学生态.md)
