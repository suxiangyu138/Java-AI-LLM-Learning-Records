# 03 - 安装与基础 API

> 定位：PyYAML 的全部基础调用——load/safe_load/load_all/dump/safe_dump/dump_all 家族——"读用 safe_load、写用 safe_dump，其余是变体"的一句话心智

---

## 📚 目录

1. [安装与版本锁定](#1-安装与版本锁定)
2. [读取：load 家族](#2-读取load-家族)
3. [写入：dump 家族](#3-写入dump-家族)
4. [多文档流：load_all 与 dump_all](#4-多文档流load_all-与-dump_all)
5. [编码与中文](#5-编码与中文)
6. [异常处理](#6-异常处理)
7. [完整示例：读写闭环](#7-完整示例读写闭环)
8. [常见坑](#8-常见坑)
9. [练习 5 题](#9-练习-5-题)

---

## 1. 安装与版本锁定

```bash
pip install "pyyaml>=6.0.3,<7"     # 2026-08 基线：6.0.3
```

**版本锁定的理由**：6.0 是安全分水岭（默认 SafeLoader），**低于 6.0 的旧版存在 RCE 漏洞家族（5.x 及更早）**——锁 `>=6.0.3` 保证历史 loader 修复全带上；同时 PyYAML 是**纯 C 扩展 + 纯 Python 双轨**包：带 C 扩展的 wheel 性能好（CLoader 可用，09 篇），**无编译器环境下装源码包会退回纯 Python**（能跑但慢）——**装完验证 C 扩展**：`python -c "import yaml; print(yaml.__with_libyaml__)"`（True = C 扩展可用）。导入惯例：`import yaml`（包名 pyyaml，导入名小写 yaml——01 篇提过，装包与导入名不同是这里最常见的坑）。

## 2. 读取：load 家族

```python
import yaml

data = yaml.safe_load("port: 8080\n")          # 从字符串
with open("config.yaml", encoding="utf-8") as f:
    data = yaml.safe_load(f)                   # 从文件（生产标准姿势）
```

**四个读取函数一句话**：

| 函数 | 语义 | 生产使用 |
|------|------|---------|
| `yaml.safe_load(s)` | **安全加载**（只构造 str/int/float/bool/null/list/dict） | **✅ 唯一入口** |
| `yaml.load(s, Loader=...)` | 指定 Loader 加载 | ⚠️ 必须显式传安全 Loader |
| `yaml.load(s)` | 无 Loader 参数 | 6.0 起默认 SafeLoader，但**不推荐依赖隐式行为** |
| `yaml.unsafe_load(s)` | 完整 Loader（可执行任意代码） | ❌ 禁止用于不可信输入（04 篇） |

**safe_load 的返回类型**：永远是纯数据结构（dict/list/str/int/float/bool/None）——**"安全 = 不可能构造出你没想到的对象"**。**字符串与文件的两种输入**：safe_load 接受**字符串**（`safe_load(text)`）或**文件对象**（`safe_load(f)`）——**文件对象时 PyYAML 直接流式读取，不需要先 `f.read()`**；两种姿势等价，**生产用文件对象（少一步 read，内存友好）**，测试/调试用字符串。文件读取的完整姿势（生产模板）：

```python
def load_config(path: str) -> dict:
    with open(path, encoding="utf-8") as f:
        return yaml.safe_load(f) or {}     # 空文件返回 None，兜底成 {}
```

**空文件/纯注释文件返回 None**——不兜底的话后续 `config["x"]` 直接 TypeError，**"or {}" 是配置读取的标准防护**。

## 3. 写入：dump 家族

```python
config = {"server": {"host": "localhost", "port": 8080}, "enabled": True}

text = yaml.safe_dump(config)                  # 转字符串
with open("config.yaml", "w", encoding="utf-8") as f:
    yaml.safe_dump(config, f, allow_unicode=True)   # 写文件
```

**safe_dump 与 dump**：safe_dump 只序列化基础类型（与 safe_load 对称），**dump 可序列化任意 Python 对象（配合自定义 representer，06 篇）**——生产写配置用 safe_dump 足够；**`allow_unicode=True` 必须传**（否则中文变 `\uXXXX` 转义，06 篇详讲）。dump 的完整参数（06 篇逐个展开）：`sort_keys`（默认 True——**输出键按字母排序，与源文件顺序不同**）、`default_flow_style`（默认 False=block 风格）、`indent`（默认 2）、`explicit_start`（是否输出 `---` 开头）。

## 4. 多文档流：load_all 与 dump_all

```python
docs = list(yaml.safe_load_all(open("multi.yaml", encoding="utf-8")))
# multi.yaml 里每个 --- 分隔的文档对应一个元素

yaml.safe_dump_all([doc1, doc2], f, allow_unicode=True)
```

**safe_load_all 返回生成器**（懒加载——大文件逐个处理省内存）；**每个文档独立加载**（一个文档语法错不影响其他已产出文档——但会抛异常终止迭代）。**多文档流的典型场景**：K8s 清单（一个文件多个资源）、批量配置、数据管道元数据集合。**"单文档用 safe_load，多文档用 safe_load_all"** 是选择一句话。

## 5. 编码与中文

**中文处理的完整纪律**：**读**——文件用 `encoding="utf-8"` 打开（PyYAML 不处理文件编码，编码是 `open()` 的事——**Windows 上漏配 utf-8 会 GBK 解码报错或乱码**）；**写**——`safe_dump(..., allow_unicode=True)`（否则中文与特殊字符被转义成 `\uXXXX`，文件"能读但不可读"）。**验证**：读出来的字符串与源文件一致、写回去的文件中文原样——**"读写双向 utf-8"是中文环境第一纪律**（与 loguru 的 encoding="utf-8" 同一条纪律的不同落点）。

**编码问题排查三问**（遇到乱码/报错的顺序）：**一**，`open` 有没有配 `encoding="utf-8"`（90% 的乱码源头）；**二**，源文件本身是什么编码（用编辑器/`file` 命令确认——**"代码里配了 utf-8 但文件是 GBK"是第二高频**）；**三**，dump 有没有 `allow_unicode=True`（"读对了写出来变 \uXXXX"是这个）。**"先看 open，再看文件，再看 dump"**十分钟定位全部中文编码问题。

## 6. 异常处理

**使用流四步**（任何项目的标准节奏）：**装（锁版本）→ 读（safe_load 入口）→ 用（数据结构操作）→ 写（safe_dump 显式参数）**——本体系 04-09 篇按这个流展开；**第一步的验证**（03 篇纪律）：`yaml.__with_libyaml__` 确认 C 扩展，`yaml.__version__` 确认 6.0+——**"版本不对一切白搭"**。

PyYAML 的异常体系（从 `yaml.error` 导入）：

```python
from yaml.error import YAMLError, MarkedYAMLError

try:
    data = yaml.safe_load(text)
except yaml.YAMLError as e:
    # MarkedYAMLError 带行号/列号/上下文（problem/context/problem_mark）
    print(f"YAML 解析失败: {e}")
```

**三个层级**：`YAMLError`（总基类）、`MarkedYAMLError`（带位置标记——**生产调试最有用**：报错信息含行号与问题描述）、具体子类（`ScannerError` 语法错、`ParserError` 结构错、`ConstructorError` 构造错——**"看见 ConstructorError 先想标签/类型问题"**）。**生产姿势**：配置文件解析包 try/except，失败时**完整打印错误（含行号）并快速失败**（配置错 = 启动即失败，不要带错运行——07 篇启动检查）。

## 7. 完整示例：读写闭环

一个"读配置 → 改配置 → 写回"的完整闭环（把前六节串起来）：

```python
import yaml

# 读：安全加载 + 兜底
with open("app.yaml", encoding="utf-8") as f:
    config = yaml.safe_load(f) or {}

# 改：纯数据结构操作（新增/覆盖键）
config.setdefault("server", {})["port"] = 9090
config["features"] = ["search", "rerank"]

# 写：显式参数（中文 + 保序 + block 风格 + 原子写）
import os, tempfile
with tempfile.NamedTemporaryFile("w", encoding="utf-8", newline="",
                                 dir=".", delete=False) as tmp:
    yaml.safe_dump(config, tmp, allow_unicode=True,
                   sort_keys=False, indent=2)
os.replace(tmp.name, "app.yaml")     # 原子替换：写一半崩了不损坏原文件

# 验证：读回来对比（round-trip 校验）
with open("app.yaml", encoding="utf-8") as f:
    assert yaml.safe_load(f) == config
```

**这个闭环的三个要点**：**改的是数据不是文本**（加载后一切操作是 Python 数据结构）；**写是"快照"不保注释**（06 篇思维模型——需要保注释才上 ruamel）；**原子写 + 回读验证**是生产级配置生成的标准流程（06 篇写文件三件套 + 回读断言）。**把闭环对应到本体系**：读的细节在本篇、写的参数在 06 篇、配置模式在 07 篇、安全在 04/05 篇——**"本篇是入口，闭环的每一环都有专门篇目"**。

## 8. 常见坑

**坑一：yaml 与 yaml 模块混淆**——`import yaml` 的是 PyYAML；装了 `pyyaml` 却 `import yamllint`（那是 lint 工具）——**装包名 pyyaml、导入名 yaml、lint 工具 yamllint，三个名字别混**（01 篇）。

**坑二：文件打开不配 encoding**——Windows 默认 GBK，UTF-8 的 YAML 直接 UnicodeDecodeError 或乱码；**open 必配 `encoding="utf-8"`**（03 篇编码纪律）。

**坑三：safe_load 返回 None 直接当 dict 用**——空文件/纯注释文件的 None 引发 TypeError；**`or {}` 兜底**（第 2 节）。

**坑四：不检查异常**——解析失败静默吞掉，配置错漏上线；**try/except 带行号 + 快速失败**（第 6 节）。

**坑五：load 不带 Loader 参数**——6.0 起默认安全，但**显式才是无歧义**（04 篇）；代码评审里"裸 load"是差评写法——**统一写 `yaml.safe_load`，让"safe"成为代码里可见的意图**。

## 9. 练习 5 题

1. 装包名与导入名分别是什么？怎么验证 C 扩展可用？
2. load 家族的四个函数各自语义？为什么 safe_load 是唯一生产入口？
3. 空文件 safe_load 返回什么？"or {}" 兜底解决什么？
4. safe_dump 与 dump 的差异？allow_unicode 不传的后果？
5. YAMLError 的三个层级？ConstructorError 提示什么问题？

> 🎯 **核心要点**：基础 API = **读用 `safe_load`（唯一入口）+ 写用 `safe_dump`（allow_unicode）+ 多文档用 `*_all` 家族 + 中文读写双 utf-8 + 解析失败带行号快速失败**——"六行代码覆盖 90% 场景，剩下的细节在 04-09 篇"。

---

**下一模块**：[04-安全红线.md](04-安全红线.md) / **返回总览**：[00-pyyaml总览.md](00-pyyaml总览.md)
