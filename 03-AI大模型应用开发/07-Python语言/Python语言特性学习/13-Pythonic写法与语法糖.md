# 13 - Pythonic 写法与语法糖

> 同一个需求，Java 式写法 15 行，Pythonic 写法 3 行且更快。这一篇把散落的语法糖收敛成一套可直接套用的写法清单

---

## 📚 目录

1. [推导式全家桶](#1-推导式全家桶)
2. [解包的九种用法](#2-解包的九种用法)
3. [切片的完整语义](#3-切片的完整语义)
4. [条件表达式与短路](#4-条件表达式与短路)
5. [海象运算符](#5-海象运算符)
6. [match 结构化模式匹配](#6-match-结构化模式匹配)
7. [f-string 进阶](#7-f-string-进阶)
8. [标准库高频武器](#8-标准库高频武器)
9. [反模式对照表](#9-反模式对照表)
10. [核心要点回顾](#10-核心要点回顾)

---

## 1. 推导式全家桶

```python
nums = range(1, 11)

# 四种推导式
squares    = [x**2 for x in nums]                      # list
unique     = {x % 3 for x in nums}                     # set
mapping    = {x: x**2 for x in nums}                   # dict
lazy       = (x**2 for x in nums)                      # 生成器（不是 tuple！）
print(tuple(x**2 for x in nums))                       # 要 tuple 得显式转

# 带条件（filter）
evens = [x for x in nums if x % 2 == 0]

# 带三元（map 分支）
labels = ["偶" if x % 2 == 0 else "奇" for x in nums]

# 多层循环（顺序与嵌套 for 一致）
pairs = [(i, j) for i in range(3) for j in range(3) if i != j]

# 嵌套推导式：矩阵转置
matrix = [[1,2,3], [4,5,6]]
print([[row[i] for row in matrix] for i in range(3)])   # [[1,4],[2,5],[3,6]]
print(list(zip(*matrix)))                               # 更 Pythonic
```

```python
# 实用组合
# ① 字典反转
inv = {v: k for k, v in mapping.items()}
# ② 过滤字典
big = {k: v for k, v in mapping.items() if v > 50}
# ③ 扁平化
nested = [[1,2], [3,4], [5]]
flat = [x for sub in nested for x in sub]               # [1,2,3,4,5]
# ④ 从 JSONL 提字段
texts = [json.loads(l)["text"] for l in open("data.jsonl") if l.strip()]
# ⑤ 批量清洗
clean = [s.strip().lower() for s in raw if s and s.strip()]
```

| 什么时候**不要**用推导式 | 改用 |
|------------------------|------|
| 超过两层嵌套、看不懂了 | 普通 for 循环 |
| 只为副作用（打印、写库） | for 循环（`[print(x) for x in y]` 是反模式） |
| 数据量大、只需遍历一次 | 生成器表达式 `(...)` |
| 只是求和/计数/取最值 | `sum()` / `len()` / `max()` 直接配生成器 |
| 逻辑复杂需要 try/except | 普通循环或独立函数 |

```python
# 生成器表达式作为唯一参数时可省略括号
print(sum(x**2 for x in range(1000)))          # 不需要写 sum((...))
print(any(x > 5 for x in nums))                # 短路：找到就停
print(all(isinstance(x, int) for x in nums))
print(max(items, key=lambda d: d["score"]))
```

> 💡 推导式在 3.12+（PEP 709）会被内联，不再创建独立函数帧，比等价的 for 循环快约 10%。而 `[...]` 与 `(...)` 的差别是**内存**：前者立刻物化全部结果，后者常量内存。

---

## 2. 解包的九种用法

```python
# ① 基础解包
a, b, c = 1, 2, 3
a, b = b, a                                     # 交换，无需临时变量

# ② 星号解包（PEP 3132）
first, *rest = [1, 2, 3, 4]                     # 1  [2,3,4]
*init, last = [1, 2, 3, 4]                      # [1,2,3]  4
first, *mid, last = [1, 2, 3, 4, 5]             # 1  [2,3,4]  5

# ③ 嵌套解包
(name, (x, y)), score = ("A", (1, 2)), 99

# ④ 忽略不需要的值
_, important, *_ = get_tuple()

# ⑤ 函数调用时解包（PEP 448）
def f(a, b, c): return a + b + c
args, kwargs = [1, 2, 3], {"a": 1, "b": 2, "c": 3}
f(*args); f(**kwargs)

# ⑥ 字面量中解包合并
merged_list = [*[1, 2], *[3, 4]]                # [1,2,3,4]
merged_dict = {**{"a": 1}, **{"b": 2}, "c": 3}  # 后者覆盖前者
merged_set  = {*[1, 2], *[2, 3]}

# 3.9+ 字典合并运算符
d = {"a": 1} | {"b": 2}                         # 新字典
d |= {"c": 3}                                   # 原地更新

# ⑦ for 循环中解包
for i, (k, v) in enumerate({"a": 1}.items()):
    print(i, k, v)

# ⑧ 返回多值（其实是返回 tuple 再解包）
def stats(xs): return min(xs), max(xs), sum(xs) / len(xs)
lo, hi, avg = stats([1, 2, 3])

# ⑨ 解包到函数参数（转发）
def wrapper(*args, **kwargs): return target(*args, **kwargs)
```

```python
# zip / enumerate：几乎所有「带索引/配对」的循环都该用它们
names, scores = ["A", "B", "C"], [90, 85, 88]

for i, name in enumerate(names, start=1):        # ✅ 不要 range(len(names))
    print(i, name)

for name, score in zip(names, scores):           # ✅ 不要用索引对齐
    print(name, score)

print(dict(zip(names, scores)))                  # 两列表 → 字典
print(list(zip(*[(1,"a"), (2,"b")])))            # 转置：[(1,2), ('a','b')]

# zip 默认截断到最短；要严格等长用 strict（3.10+）
list(zip([1,2,3], [1,2], strict=True))           # ValueError ← 提前发现数据错位
from itertools import zip_longest
list(zip_longest([1,2,3], [1,2], fillvalue=0))   # [(1,1),(2,2),(3,0)]
```

---

## 3. 切片的完整语义

```python
s = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]

s[2:5]        # [2,3,4]        [start:stop) 左闭右开
s[:3]         # [0,1,2]        省略 start = 0
s[7:]         # [7,8,9]        省略 stop = len
s[:]          # 全部（浅拷贝）
s[::2]        # [0,2,4,6,8]    步长
s[::-1]       # 反转
s[::-2]       # [9,7,5,3,1]
s[-3:]        # [7,8,9]        末尾 3 个
s[:-3]        # [0..6]         去掉末尾 3 个
s[5:2:-1]     # [5,4,3]        负步长时 start > stop

# 越界不报错——这是与索引访问的关键区别
s[100:200]    # []             不抛 IndexError
s[5:100]      # [5,...,9]      自动截断
# s[100]      # IndexError!
```

```python
# 切片赋值（仅可变序列）
t = [0,1,2,3,4,5]
t[1:3] = ["a","b","c"]         # [0,'a','b','c',3,4,5]  长度可变
t[::2] = [0]*4                 # 扩展切片赋值长度必须严格匹配
del t[1:3]                     # 删除区间
t[:] = [9, 9]                  # 原地替换全部内容（保留对象身份）

# slice 对象：把切片规则存成变量
HEADER = slice(0, 10)
BODY   = slice(10, None)
line = "0123456789ABCDEF"
print(line[HEADER], line[BODY])

# indices()：把切片规范化为具体索引（处理负数/越界）
print(slice(-5, None, 2).indices(10))    # (5, 10, 2)
```

| 常见惯用法 | 写法 |
|-----------|------|
| 反转 | `s[::-1]`（字符串/tuple 也行） |
| 浅拷贝 | `s[:]` 或 `list(s)` 或 `s.copy()` |
| 每 n 个取一个 | `s[::n]` |
| 去掉首尾 | `s[1:-1]` |
| 原地清空但保留引用 | `s[:] = []` 或 `s.clear()` |
| 分块 | `[s[i:i+n] for i in range(0, len(s), n)]` |
| 分块（3.12+） | `list(itertools.batched(s, n))` |

> ⚠️ 切片产生**浅拷贝**：`copy = matrix[:]` 之后 `copy[0].append(x)` 会影响原矩阵。深拷贝要 `copy.deepcopy`（见 02 章）。

---

## 4. 条件表达式与短路

```python
# 三元表达式
status = "成年" if age >= 18 else "未成年"
value = data.get("k") if data else None

# 链式比较（Java 做不到）
if 0 <= x <= 100: ...                   # 而不是 x >= 0 and x <= 100
if a < b < c < d: ...
if x == y == z: ...

# 短路取值
name = user_input or "默认值"            # 空字符串/None/0 都会取默认值 ⚠️
name = user_input if user_input is not None else "默认值"   # 更精确
config = a or b or c or {}

# and 短路做守卫
result = obj and obj.method()           # obj 为假则返回 obj 本身
length = data and len(data)             # ⚠️ data=[] 时返回 [] 而非 0

# in 代替多重 ==
if cmd in ("start", "run", "go"): ...   # 而不是 cmd=="start" or ...
if ext in {".jpg", ".png", ".gif"}: ... # 集合更快（O(1)）

# 真值判断
if items: ...                           # ✅ 而不是 if len(items) > 0
if not items: ...                       # ✅ 而不是 if items == []
if x is None: ...                       # ✅ None 必须用 is
```

```python
# 字典替代长 if-elif 链（分派表）
def handle_a(): return "A"
def handle_b(): return "B"

HANDLERS = {"a": handle_a, "b": handle_b}
result = HANDLERS.get(cmd, lambda: "未知")()

# dict.get / setdefault 消灭判断
count = counts.get(key, 0) + 1
groups.setdefault(key, []).append(item)   # 或用 defaultdict

# for-else / while-else：循环正常结束（未 break）才执行 else
for item in items:
    if item.matches(): break
else:
    raise ValueError("没找到匹配项")       # 只在没 break 时执行
```

---

## 5. 海象运算符

PEP 572（3.8+）：`:=` 在表达式内部完成赋值。

```python
# ① while 读取
while (line := f.readline()):
    process(line)
# 旧写法需要 while True + if not line: break

while (chunk := stream.read(8192)):
    hasher.update(chunk)

# ② 推导式里避免重复计算
# ❌ 计算两次
results = [expensive(x) for x in data if expensive(x) > 0]
# ✅ 只算一次
results = [y for x in data if (y := expensive(x)) > 0]

# ③ if 里同时赋值和判断
if (m := re.search(r"\d+", text)):
    print(m.group())

if (n := len(items)) > 100:
    print(f"太多了：{n} 项")

if (user := db.find(uid)) is not None:
    print(user.name)

# ④ 减少函数调用
if (cnt := text.count("a")) >= 3:
    print(f"出现 {cnt} 次")

# ⑤ any/all 里捕获中间值
if any((found := x) > 10 for x in nums):
    print(f"第一个超过 10 的是 {found}")
```

```python
# ⚠️ 什么时候不要用
# ① 只是普通赋值 → 用 =
# x := 5           # SyntaxError，必须加括号 (x := 5)
# ② 让代码更难读
result = [y := f(x), y**2, y**3]        # ❌ 副作用藏在列表里
# ③ 在 lambda / 全局赋值场景滥用
```

---

## 6. match 结构化模式匹配

PEP 634（3.10+）。**它不是 switch**——核心能力是「解构」，最接近的类比是 Scala/Rust 的 match。

```python
# ① 字面量与或模式
def http_msg(status):
    match status:
        case 200 | 201 | 204:   return "成功"
        case 400:               return "请求错误"
        case 401 | 403:         return "无权限"
        case n if 500 <= n < 600: return f"服务端错误 {n}"     # guard 守卫
        case _:                 return "未知"                  # 通配

# ② 序列模式（解构 + 长度匹配）
def parse_cmd(tokens):
    match tokens:
        case []:                    return "空命令"
        case ["quit" | "exit"]:     return "退出"
        case ["move", direction]:   return f"移动 {direction}"
        case ["set", key, value]:   return f"{key}={value}"
        case ["sum", *nums]:        return sum(map(int, nums))   # 捕获剩余
        case [cmd, *_]:             return f"未知命令 {cmd}"

print(parse_cmd(["sum", "1", "2", "3"]))     # 6

# ③ 映射模式（处理 JSON / LLM 输出）
def handle_event(evt):
    match evt:
        case {"type": "message", "content": str(text)}:          # 带类型校验
            return f"消息: {text}"
        case {"type": "tool_call", "name": name, "args": dict(args)}:
            return f"调用 {name}({args})"
        case {"type": "error", "code": int(code), **rest}:        # **rest 收集其余键
            return f"错误 {code}, 额外字段 {rest}"
        case {"type": t}:
            return f"未处理类型 {t}"
# 映射模式是「至少包含」语义，多余的键不影响匹配

# ④ 类模式（解构对象属性）
from dataclasses import dataclass
@dataclass
class Point: x: int; y: int
@dataclass
class Circle: center: Point; r: float

def describe(shape):
    match shape:
        case Point(x=0, y=0):              return "原点"
        case Point(x=0, y=y):              return f"Y 轴上，y={y}"
        case Point(x=x, y=y) if x == y:    return f"对角线上 {x}"
        case Circle(center=Point(x=0, y=0), r=r):  return f"以原点为心，半径 {r}"
        case Circle():                     return "普通圆"
        case _:                            return "其他"

print(describe(Circle(Point(0,0), 2.5)))    # 以原点为心，半径 2.5

# ⑤ 位置参数模式（需 __match_args__，dataclass 自动生成）
match Point(1, 2):
    case Point(1, y): print(f"x=1, y={y}")

# ⑥ as 模式：既匹配又绑定整体
match shape:
    case Circle(r=r) as c if r > 10:
        print(f"大圆 {c}")
```

```python
# ⚠️ 陷阱：裸名字是「捕获」不是「比较」
SUCCESS = 200
match status:
    case SUCCESS:   ...          # ❌ 这是把 status 赋值给 SUCCESS，永远匹配！
    case Status.OK: ...          # ✅ 带点的名字才是值比较
    case 200:       ...          # ✅ 字面量
    case x if x == SUCCESS: ...  # ✅ 用 guard
```

| 模式类型 | 语法 | 用途 |
|---------|------|------|
| 字面量 | `case 200:` | 常量比较 |
| 捕获 | `case x:` | 绑定任意值 |
| 通配 | `case _:` | 兜底 |
| 或 | `case 1 \| 2 \| 3:` | 多选一 |
| 序列 | `case [a, b, *rest]:` | 解构 list/tuple |
| 映射 | `case {"k": v, **rest}:` | 解构 dict（至少包含） |
| 类 | `case Point(x=0):` | 解构对象属性 |
| 守卫 | `case x if x > 0:` | 附加条件 |
| as | `case Point() as p:` | 匹配 + 绑定整体 |

> 🎯 `match` 最适合的场景：**解析嵌套 JSON、处理 AST、状态机、LLM 的 tool_call 分派**。如果只是值到值的映射，字典分派表更简单也更快。

---

## 7. f-string 进阶

```python
name, score, ratio = "Tom", 95.6789, 0.8532
items = {"a": 1}

# 基础
f"{name} 得了 {score} 分"
f"{score:.2f}"                  # 95.68        小数位
f"{score:10.2f}"                # '     95.68'  宽度 + 右对齐（数字默认）
f"{name:<10}|{name:>10}|{name:^10}|"          # 左/右/居中对齐
f"{name:*^12}"                  # ***Tom****** 填充字符
f"{ratio:.1%}"                  # 85.3%        百分比
f"{1234567:,}"                  # 1,234,567    千分位
f"{1234567:_}"                  # 1_234_567
f"{255:b} {255:o} {255:x} {255:X} {255:#x}"   # 二/八/十六进制
f"{1e10:.2e}"                   # 1.00e+10     科学计数
f"{42:05d}"                     # 00042        零填充
f"{-5:+d} {5:+d}"               # -5 +5        显示符号

# 表达式与调试
f"{score * 2:.1f}"              # 任意表达式
f"{items['a']}"                 # 下标（注意引号不能与外层冲突，3.12+ 已放宽）
f"{name.upper()}"               # 方法调用
f"{score=}"                     # score=95.6789   ← 调试神器（3.8+）
f"{score * 2 = :.2f}"           # score * 2 = 191.36

# 转换标志
f"{name!r}"                     # 'Tom'        用 repr()
f"{name!s}"                     # Tom          用 str()
f"{name!a}"                     # ascii()

# 嵌套花括号（动态精度/宽度）
prec, width = 3, 12
f"{score:{width}.{prec}f}"      # '      95.679'

# 日期时间
from datetime import datetime
f"{datetime(2026,7,28):%Y-%m-%d %H:%M:%S}"    # 2026-07-28 00:00:00
f"{datetime(2026,7,28):%Y年%m月%d日}"

# 多行 f-string
msg = (
    f"用户: {name}\n"
    f"分数: {score:.1f}\n"
)

# 3.12+ 放宽限制（PEP 701）：可复用同种引号、可写反斜杠、可嵌任意表达式
f"{items["a"]}"                               # 3.12+ 合法
f"{'\n'.join(['a','b'])}"                     # 3.12+ 合法
```

| 格式化方案 | 性能 | 可读性 | 适用 |
|-----------|:----:|:-----:|------|
| **f-string** | 最快 | 最好 | 默认选择 |
| `str.format()` | 中 | 中 | 模板需要复用、格式串来自配置 |
| `%` 格式化 | 中 | 差 | 遗留代码 |
| `Template` | 慢 | 好 | 处理不可信的用户模板（安全） |
| `logging` 的 `%s` | — | — | **日志必须用它**：`log.info("x=%s", x)` 延迟求值，未启用级别时零开销 |

> ⚠️ 日志里不要用 f-string：`log.debug(f"data={huge_obj}")` 即使 DEBUG 关闭也会执行昂贵的 `repr`。正确写法 `log.debug("data=%s", huge_obj)`。

---

## 8. 标准库高频武器

```python
# collections
from collections import defaultdict, Counter, deque, namedtuple, ChainMap, OrderedDict

groups = defaultdict(list); groups["k"].append(1)      # 自动建默认值
nested = defaultdict(lambda: defaultdict(int))         # 多层嵌套

c = Counter("mississippi")
c.most_common(3)                                       # [('i',4),('s',4),('p',2)]
c1 + c2; c1 - c2; c1 & c2; c1 | c2                     # Counter 支持集合运算
Counter(words).total()                                 # 3.10+

dq = deque([1,2,3], maxlen=5)                          # 定长环形缓冲（滑动窗口）
dq.appendleft(0); dq.rotate(1); dq.extendleft([-1])    # 两端 O(1)

cfg = ChainMap(cli_args, env_vars, defaults)           # 多层配置回退查找

# itertools（见 06 章）
from itertools import (chain, islice, groupby, count, cycle, repeat,
                       accumulate, product, combinations, permutations,
                       zip_longest, tee, pairwise, batched, takewhile, dropwhile)
list(chain.from_iterable([[1,2],[3]]))                 # 扁平化
list(islice(infinite_gen(), 10))                       # 取前 10 个
list(accumulate([1,2,3,4]))                            # [1,3,6,10] 前缀和
list(pairwise([1,2,3,4]))                              # [(1,2),(2,3),(3,4)] 3.10+
list(batched(range(7), 3))                             # [(0,1,2),(3,4,5),(6,)] 3.12+
list(product("ab", repeat=2))                          # 笛卡尔积
list(combinations([1,2,3], 2))                         # 组合

# functools（见 07 章）
from functools import lru_cache, cache, cached_property, partial, reduce, wraps
@cache                                                 # 3.9+ = lru_cache(maxsize=None)
def fib(n): return n if n < 2 else fib(n-1) + fib(n-2)

# operator：替代简单 lambda，更快且可 pickle
from operator import itemgetter, attrgetter, methodcaller, add
sorted(records, key=itemgetter("score", "name"))       # 多级排序
sorted(objs, key=attrgetter("user.age"))               # 支持点号链
max(strs, key=methodcaller("count", "a"))
reduce(add, [1,2,3])

# pathlib：彻底告别 os.path
from pathlib import Path
p = Path("data") / "raw" / "corpus.jsonl"              # / 拼路径，跨平台
p.parent, p.name, p.stem, p.suffix, p.suffixes
p.exists(); p.is_file(); p.stat().st_size
p.read_text(encoding="utf-8"); p.write_text(s, encoding="utf-8")
p.read_bytes(); p.parent.mkdir(parents=True, exist_ok=True)
list(Path("docs").glob("**/*.md"))                     # 递归查找
list(Path("docs").rglob("*.md"))                       # 同上，更短
p.with_suffix(".txt"); p.with_stem("new"); p.resolve()

# 其他
import json, csv, re, textwrap, enum, contextlib, statistics, secrets
from datetime import datetime, timedelta, timezone
from decimal import Decimal          # 金额计算，不用 float
from fractions import Fraction
from dataclasses import dataclass, field, asdict, replace
from enum import Enum, IntEnum, StrEnum, auto              # StrEnum 3.11+
from typing import Final

class Model(StrEnum):                                     # 3.11+：直接当字符串用
    OPUS = "claude-opus-5"
    SONNET = "claude-sonnet-5"
print(f"调用 {Model.OPUS}")                                # 调用 claude-opus-5

statistics.mean([1,2,3]); statistics.median(xs); statistics.stdev(xs)
textwrap.dedent("""  缩进会被统一去掉  """)                 # 处理多行字符串
secrets.token_hex(16)                                      # 安全随机（不要用 random 做密钥）
```

---

## 9. 反模式对照表

| ❌ 非 Pythonic | ✅ Pythonic | 原因 |
|--------------|-----------|------|
| `for i in range(len(xs)): xs[i]` | `for x in xs:` | 直接迭代 |
| `for i in range(len(xs)): i, xs[i]` | `for i, x in enumerate(xs):` | 内置支持 |
| `i = 0; while i < len(a): a[i], b[i]` | `for x, y in zip(a, b):` | 无需索引 |
| `if len(xs) > 0:` | `if xs:` | 真值判断 |
| `if x == None:` | `if x is None:` | None 是单例 |
| `if type(x) == int:` | `if isinstance(x, int):` | 支持继承 |
| `if x == True:` | `if x:` | 多余比较 |
| `xs.append(f(x)) in loop` | `[f(x) for x in xs]` | 推导式 |
| `d.keys()` 里判断存在 | `k in d` | 直接查字典 O(1) |
| `if k in d: v = d[k] else: v = 0` | `v = d.get(k, 0)` | 一步到位 |
| `if k not in d: d[k] = []` | `defaultdict(list)` | 免判断 |
| `try: ... except: pass` | `except SpecificError:` | 裸 except 会吞掉 `KeyboardInterrupt` |
| `s = ""; s += x in loop` | `"".join(parts)` | 字符串不可变，`+=` 是 O(n²) |
| `open(f).read()` | `with open(f) as fp:` | 保证关闭 |
| `os.path.join(a, b)` | `Path(a) / b` | pathlib |
| `lambda x: x["k"]` | `itemgetter("k")` | 更快、可 pickle |
| `list(range(...))` 只为遍历 | `range(...)` | 惰性 |
| `sorted(xs)[0]` | `min(xs)` | O(n) vs O(n log n) |
| `sorted(xs, reverse=True)[:3]` | `heapq.nlargest(3, xs)` | 大数据集更快 |
| `x in big_list` 频繁查询 | `x in big_set` | O(n) → O(1) |
| `def f(x=[])` | `def f(x=None): x = x or []` | 可变默认值陷阱 |
| `global` 传状态 | 参数传递 / 类属性 | 可测试性 |
| `%` / `.format()` 拼日志 | `log.info("%s", x)` | 延迟求值 |
| `time.time()` 测耗时 | `time.perf_counter()` | 单调时钟，精度高 |
| `random` 生成 token | `secrets` | 密码学安全 |
| `float` 算钱 | `Decimal` | 精度 |
| 手写单例/缓存 | `@cache` / 模块级变量 | 模块天然是单例 |
| 到处 `try/except KeyError` | EAFP 但要具体 | 见下 |

```python
# EAFP vs LBYL：Python 偏好 EAFP（先做再处理异常）
# ❌ LBYL：检查后仍可能竞态（文件在检查后被删）
if os.path.exists(path):
    with open(path) as f: data = f.read()

# ✅ EAFP：直接做，处理具体异常
try:
    with open(path) as f: data = f.read()
except FileNotFoundError:
    data = ""

# 但异常不能滥用于正常控制流（异常有开销）
# ❌ 用异常做循环退出
# ✅ 用 in / get / 条件判断处理「预期会经常发生」的情况
```

```python
# 字符串拼接性能实测
import timeit
# O(n²)：每次 += 都创建新字符串
timeit.timeit('s=""\nfor i in range(10000): s += "x"', number=100)   # 慢
# O(n)：join 一次分配
timeit.timeit('"".join("x" for _ in range(10000))', number=100)      # 快 5-10 倍
```

---

## 10. 核心要点回顾

- 四种推导式：`[]` list、`{}` set、`{k:v}` dict、`()` **生成器（不是 tuple）**
- 超过两层嵌套或只为副作用时，回到普通 for 循环
- `enumerate` / `zip` 消灭 90% 的索引循环；`zip(..., strict=True)` 提前发现长度错位
- 星号解包 `first, *rest`、PEP 448 字面量解包 `[*a, *b]` / `{**a, **b}`、3.9+ 的 `d1 | d2`
- 切片**越界不报错**，返回浅拷贝；`s[:] = [...]` 原地替换保留对象身份
- 链式比较 `0 <= x <= 100` 是 Python 独有的可读性优势
- `or` 取默认值时注意「空字符串/0/空列表都是假」，精确判断用 `is not None`
- `for-else`：循环未 break 才执行 else，适合「找不到就报错」
- 海象 `:=` 的三个主场：`while (line := f.readline())`、推导式避免重复计算、`if (m := re.search(...))`
- `match` 的核心是**解构**而非 switch；裸名字 `case NAME:` 是捕获不是比较（高频陷阱）
- f-string 记住 `{x=}` 调试、`{x:.2%}`、`{x:,}`、`{x!r}`、`{x:{w}.{p}f}` 嵌套
- **日志必须用 `%s` 占位符**，不用 f-string（延迟求值）
- 五个必装武器：`collections`、`itertools`、`functools`、`operator`、`pathlib`
- 字符串拼接用 `join`，`+=` 在循环里是 O(n²)
- EAFP（先做再处理异常）优于 LBYL（先检查），但 `except` 必须写具体异常类型

---

## 参考资料

| 类型 | 名称 |
|------|------|
| PEP | **20（Zen）**、8（风格指南）、**572（海象）**、**634/635/636（match）**、448（解包泛化）、498/701（f-string）、3132（星号解包） |
| 文档 | Python Tutorial、`collections` / `itertools` / `functools` / `pathlib` 官方文档、Format Specification Mini-Language |
| 书籍 | **《Effective Python》（125 条具体建议，本篇的最佳延伸）**、《Fluent Python》、《Python Cookbook》 |
| 工具 | `ruff`（自动检出大量反模式）、`black`（格式化） |

---

**上一模块**：[12 并发模型与 GIL](./12-并发模型与GIL.md) ／ **下一模块**：[14 面试高频考点与陷阱集](./14-面试高频考点与陷阱集.md) ／ **返回总览**：[00 总览](./00-Python语言特性知识体系总览.md)
