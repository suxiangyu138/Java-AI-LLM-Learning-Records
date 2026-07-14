# 附录 B：资源索引与速查

> **目标**：长期参考——需要什么查什么，不浪费 Java 主力学习时间
> **策略**：以官方文档和高质量中文资源为主，按需深入，不追求全覆盖

---

## 📌 使用指南

- 🟢 **优先看**：与你的 Java+AI 路线直接相关
- 🟡 **按需看**：有具体需求时查阅
- ⚪ **了解即可**：拓宽视野，不必须深入

---

## 🎯 官方文档（权威参考）

| 资源 | 说明 | 优先级 |
|------|------|:------:|
| [docs.python.org/3](https://docs.python.org/3/) | Python 官方文档（含 Tutorial、Library Reference） | 🟢 |
| [docs.python.org/3/tutorial](https://docs.python.org/3/tutorial/) | 官方入门教程 —— 已会编程者的最佳起点 | 🟢 |
| [docs.python.org/3/library](https://docs.python.org/3/library/) | 标准库参考 —— "需要什么查什么" | 🟢 |
| [peps.python.org](https://peps.python.org/) | Python 增强提案 —— 理解语言设计决策 | 🟡 |
| [devguide.python.org](https://devguide.python.org/) | CPython 贡献指南 —— 想深入内核时看 | ⚪ |

---

## 📚 系统学习资源

### 书籍

| 书名 | 适合阶段 | 说明 |
|------|:--------:|------|
| **《Python 编程：从入门到实践》** | 阶段 1–3 | ⭐ 最适合入门，项目驱动，中文版质量好 |
| **《流畅的 Python》** | 阶段 2+ | ⭐ 进阶必读，理解 Python 语言设计哲学 |
| **《利用 Python 进行数据分析》** | 阶段 4 | Wes McKinney（Pandas 作者），数据分析权威 |
| **《Python 深度学习》** | 阶段 4 | François Chollet，Keras 作者，DL 入门经典 |
| **《Effective Python》** | 阶段 2+ | 90 条 Python 最佳实践，短小精悍 |
| **《Python Cookbook》** | 阶段 3 | 实战代码片段集，类似 Java 的 Effective Java |
| **《Python 3 标准库》** | 阶段 3 | 系统了解标准库，适合查阅 |

### 视频课程

| 课程 | 平台 | 说明 |
|------|------|------|
| **100 Days of Code: Python** | Udemy | 项目驱动的完整 Python 课程 |
| **CS50's Python** | edX / YouTube | 哈佛入门课，免费 |
| **李沐《动手学深度学习》** | B站 / d2l.ai | ⭐ AI 方向首选，中文，代码 + 理论 |
| **莫烦 Python** | B站 / YouTube | 短小精悍，适合快速上手特定库 |
| **Fast.ai** | fast.ai | 实战深度学习，top-down 教学法 |

### 在线教程

| 资源 | 说明 | 优先级 |
|------|------|:------:|
| [realpython.com](https://realpython.com/) | ⭐ 高质量 Python 教程，深度文章 | 🟢 |
| [fullstackpython.com](https://www.fullstackpython.com/) | Python Web 全栈技术地图 | 🟡 |
| [pythontutor.com](https://pythontutor.com/) | 代码执行过程可视化 | 🟢 |
| [learnxinyminutes.com](https://learnxinyminutes.com/docs/python/) | Python 语法速查（一页纸） | 🟢 |
| [pythonsheets.com](https://www.pythonsheets.com/) | Python 速查表合集 | 🟡 |

---

## 🗂️ 各阶段资源映射

```
阶段 1（语法速通）
├── 🟢 Python 官方 Tutorial
├── 🟢 Learn X in Y minutes - Python
├── 🟢 《Python 编程：从入门到实践》前半部分
├── 🟢 Python-100-Days Day01–20
└── 🟡 编程指北 Python 一条龙路线

阶段 2（进阶工程）
├── 🟢 《流畅的 Python》—— 深刻理解 Python
├── 🟢 realpython.com —— OOP、装饰器、生成器专题
├── 🟢 packaging.python.org —— 官方打包指南
├── 🟡 《Effective Python》
└── 🟡 pyproject.toml 官方文档 (PEP 621)

阶段 3（标准库）
├── 🟢 Python 标准库参考文档
├── 🟢 Python-100-Days 标准库章节
├── 🟢 realpython.com —— argparse、pathlib、subprocess 专题
├── 🟡 《Python 3 标准库》
└── 🟡 《Python Cookbook》

阶段 4（AI/数据）
├── 🟢 NumPy 官方 Quickstart Tutorial
├── 🟢 Pandas Getting Started（10 分钟入门 + Cookbook）
├── 🟢 李沐《动手学深度学习》(d2l.ai)
├── 🟢 PyTorch 官方 Tutorials
├── 🟢 FastAPI 官方文档（极其清晰）
├── 🟢 Kaggle Learn —— 免费互动课程
├── 🟡 《利用 Python 进行数据分析》
├── 🟡 《Python 深度学习》
└── 🟡 Hugging Face Course (huggingface.co/learn)

阶段 5（项目实战）
├── 🟢 FastAPI 官方教程中的项目示例
├── 🟢 LangChain / LlamaIndex 文档（RAG 方向）
├── 🟢 ChromaDB 文档（向量数据库）
├── 🟡 Docker 官方 Python 指南
└── 🟡 realpython.com 项目实战文章
```

---

## 🔗 GitHub 仓库推荐

| 仓库 | Stars | 说明 |
|------|:-----:|------|
| [jackfrued/Python-100-Days](https://github.com/jackfrued/python-100-days) | 150k+ | ⭐ Python 全方向 100 天教程，中文 |
| [TheAlgorithms/Python](https://github.com/TheAlgorithms/Python) | 180k+ | 所有算法 Python 实现，刷题参考 |
| [vinta/awesome-python](https://github.com/vinta/awesome-python) | 210k+ | Python 资源大全（库、框架、工具） |
| [faif/python-patterns](https://github.com/faif/python-patterns) | 40k+ | Python 设计模式集合 |
| [donnemartin/system-design-primer](https://github.com/donnemartin/system-design-primer) | 270k+ | 系统设计（含 Python 示例） |
| [d2l-ai/d2l-zh](https://github.com/d2l-ai/d2l-zh) | 60k+ | ⭐ 动手学深度学习，中文版 |
| [shyetang/code-roadmap](https://github.com/shyetang/code-roadmap) | — | 编程学习路线（含 Python 工具语言路线） |
| [realpython/python-guide](https://github.com/realpython/python-guide) | 28k+ | Python 最佳实践指南 |
| [mlabonne/llm-course](https://github.com/mlabonne/llm-course) | 35k+ | LLM 学习路线与代码 |
| [langchain-ai/langchain](https://github.com/langchain-ai/langchain) | 95k+ | LLM 应用开发框架 |

---

## 📋 速查表（Cheat Sheets）

### Python 基础速查

```python
# === 数据结构创建 ===
lst = [1, 2, 3]                 # list
tup = (1, 2, 3)                 # tuple（不可变）
d = {"a": 1, "b": 2}            # dict
s = {1, 2, 3}                   # set（不重复）

# === 推导式 ===
[x**2 for x in range(10)]                       # 列表推导
[x for x in range(10) if x % 2 == 0]            # 带过滤
{x: len(x) for x in ["a", "bb"]}                # 字典推导

# === 常用内置函数 ===
len(x)          # 长度
type(x)         # 类型
range(n)        # 0 到 n-1
enumerate(lst)  # (index, value)
zip(a, b)       # 并行迭代
sorted(lst)     # 返回新排序列表
reversed(lst)   # 返回反转迭代器
map(fn, lst)    # 映射
filter(fn, lst) # 过滤
any(lst)        # 任一为 True
all(lst)        # 全部为 True
sum(lst)        # 求和
max(lst)
min(lst)

# === 字符串 ===
s.strip()           # 去首尾空白
s.split(",")        # 按分隔符切分
",".join(lst)       # 拼接
s.replace("a","b")  # 替换
s.find("sub")       # 查找（-1 为未找到）
s.startswith("pre") # 前缀判断
s.endswith("suf")   # 后缀判断
f"{var:.2f}"        # f-string 格式化

# === 文件操作 ===
with open("f.txt", "r", encoding="utf-8") as f:
    content = f.read()
    for line in f: ...            # 逐行迭代
```

### NumPy / Pandas 速查

```python
# === NumPy ===
np.array([1,2,3])                           # 创建
np.zeros((3,4)), np.ones((2,3))             # 全0/全1
np.arange(0, 10, 2)                         # 范围
np.linspace(0, 1, 100)                      # 等间距
np.random.randn(100)                        # 正态分布
arr.reshape(2, -1)                          # 重塑
np.concatenate([a, b])                      # 拼接
np.sum(arr, axis=0)                         # 沿轴求和
arr[arr > 0]                                # 布尔索引

# === Pandas ===
pd.read_csv("f.csv")                        # 读 CSV
df.head(), df.tail(), df.info()             # 查看
df.describe()                               # 统计摘要
df["col"]                                   # 选列
df[["c1", "c2"]]                            # 选多列
df[df["col"] > 0]                           # 过滤行
df.loc[:, "c1":"c3"]                       # 按标签
df.iloc[:10, :3]                           # 按位置
df.groupby("col").mean()                    # 分组聚合
df.sort_values("col", ascending=False)      # 排序
df["new"] = df["old"].apply(fn)             # 应用函数
df.isna().sum()                             # 缺失统计
df.dropna(), df.fillna(0)                   # 处理缺失
pd.merge(a, b, on="key")                    # 连接
df.to_csv("out.csv", index=False)           # 写文件
```

### 常用 pip 命令速查

```bash
pip install <pkg>                  # 安装
pip install <pkg>==1.2.3          # 指定版本
pip install -r requirements.txt   # 批量安装
pip uninstall <pkg>               # 卸载
pip list                          # 列出已安装
pip show <pkg>                    # 查看详情
pip freeze > requirements.txt     # 导出依赖
python -m venv .venv              # 创建虚拟环境
```

### Git 提交流程速查

```bash
# 标准 Python 项目提交流程
ruff check . && ruff format --check .    # 代码检查
pytest                                   # 运行测试
git add .
git commit -m "feat: 添加 xxx 功能"
git push
```

---

## ⚠️ 常见错误速查

| 错误信息 | 原因 | 解决 |
|---------|------|------|
| `IndentationError` | 缩进不一致（混用 Tab/Space） | 用 4 空格，VSCode 设置 `editor.renderWhitespace` |
| `ModuleNotFoundError: No module named 'xxx'` | 包未安装或虚拟环境未激活 | `pip install xxx` 或激活 `.venv` |
| `AttributeError: 'NoneType' object has no attribute 'xxx'` | 变量为 None | 加 None 检查或跟踪上游返回值 |
| `TypeError: 'list' object is not an iterator` | 把 list 当迭代器用 | list 是可迭代对象，但不是迭代器 |
| `UnboundLocalError` | 在函数内对全局变量赋值 | 加 `global` 声明或改用参数传递 |
| `RecursionError` | 递归深度超限（默认 1000） | `sys.setrecursionlimit()` 或改用迭代 |
| `KeyError` | dict 访问不存在的 key | `d.get(key, default)` 替代 `d[key]` |
| `SyntaxError: invalid syntax` on f-string | Python < 3.6 | 升级 Python 版本 |

---

## 🔄 Java ↔ Python 术语映射

| Java 术语 | Python 术语 | 备注 |
|-----------|------------|------|
| `ArrayList<T>` | `list` | Python 无泛型语法 |
| `HashMap<K,V>` | `dict` | |
| `HashSet<T>` | `set` | |
| `String` | `str` | 都是不可变 |
| `int` / `Integer` | `int` | Python 无原始类型/包装类之分 |
| `double` | `float` | Python float 即双精度 |
| `boolean` | `bool` | True/False 首字母大写 |
| `null` | `None` | 都是单例 |
| `void` | `None`（函数返回值） | |
| `this` | `self` | Python 必须显式传入 |
| `new ClassName()` | `ClassName()` | Python 无 new 关键字 |
| `implements Interface` | 鸭子类型 / `Protocol` | Python 不强制声明接口 |
| `extends` | `class Child(Parent)` | 括号内指定父类 |
| `super()` | `super().__init__()` | Python 需显式调用 |
| `@Override` | 无（自动覆盖） | |
| `try-catch-finally` | `try-except-else-finally` | Python 多了 else |
| `throws` | 无（不声明） | Python 不强制声明抛出异常 |
| `for (T item : list)` | `for item in list` | |
| `list.stream().map()` | `map(fn, list)` / 推导式 | |
| `Maven/Gradle` | `pip` / `poetry` | |
| `pom.xml` / `build.gradle` | `pyproject.toml` / `requirements.txt` | |
| `JAR` | `.whl` (wheel) | |
| `JUnit` | `pytest` / `unittest` | |
| `JVM 参数 -Xmx` | 无简单等价物 | Python 有自己的内存管理 |
| `Spring Boot` | `FastAPI` / `Flask` | FastAPI 体验最接近 Spring Boot |

---

## 🌐 社区与提问

| 平台 | 说明 |
|------|------|
| [Stack Overflow](https://stackoverflow.com/questions/tagged/python) | Python 标签，解决具体技术问题 |
| [r/learnpython](https://reddit.com/r/learnpython/) | Reddit 学习社区 |
| [Python Discord](https://pythondiscord.com/) | 即时聊天社区，有专门的学习频道 |
| [PyCon 演讲](https://www.youtube.com/@PyConUS) | 高水平技术分享 |
| [Python Weekly](https://www.pythonweekly.com/) | 每周 Python 新闻推送 |

---

> **上一阶段** ← [06-开发环境与工具链](06-开发环境与工具链.md)
> **返回总览** → [00-Python学习路线总览](00-Python学习路线总览.md)
