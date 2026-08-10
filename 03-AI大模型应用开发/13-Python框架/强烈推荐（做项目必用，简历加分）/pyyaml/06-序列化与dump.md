# 06 - 序列化与 dump

> 定位：Python 对象 → YAML 文本的完整控制——dump 参数全解、中文、排序、流式风格、自定义 representer——"dump 的输出不是你直觉的样子，参数要显式配"

---

## 📚 目录

1. [dump 参数全解](#1-dump-参数全解)
2. [中文与编码](#2-中文与编码)
3. [排序与键序](#3-排序与键序)
4. [流式风格控制](#4-流式风格控制)
5. [自定义对象序列化：representer](#5-自定义对象序列化representer)
6. [dump 与文件写入](#6-dump-与文件写入)
7. [常见坑](#7-常见坑)
8. [练习 5 题](#8-练习-5-题)

---

## 1. dump 参数全解

```python
yaml.safe_dump(data, f,
    sort_keys=False,          # 保持键的插入顺序（默认 True 按字母排！）
    default_flow_style=False, # block 风格（默认 False；True 输出 JSON 风格）
    allow_unicode=True,       # 中文原样输出（不传则 \uXXXX 转义）
    indent=2,                 # 缩进宽度（默认 2，与社区惯例一致）
    explicit_start=True,      # 输出开头加 ---
    default_style=None,       # 标量引号风格（None=按需，'"'=全双引号）
    line_break="\n",          # 换行符（跨平台注意 \r\n）
)
```

**六个参数一句话**：`sort_keys` 管键序、`default_flow_style` 管风格、`allow_unicode` 管中文、`indent` 管缩进、`explicit_start` 管 `---`、`default_style` 管引号。**"dump 的默认值按'机器友好'设计，不按'人可读'设计"**——**想输出可读配置，显式参数是标配**（03 篇已预告）。

## 2. 中文与编码

**中文输出是 dump 最经典的"意外"**：

```python
config = {"name": "苏巷雨", "note": "中文配置"}

print(yaml.safe_dump(config))
# name: 苏巷雨        ← 没传 allow_unicode 的默认输出
print(yaml.safe_dump(config, allow_unicode=True))
# name: 苏巷雨                      ← 人类可读
```

**不传 `allow_unicode=True`，中文和所有非 ASCII 字符都被转义成 `\uXXXX`**——文件"语法正确但不可读"（JSON 也这样，但 YAML 是给人读的格式，转义违背了它的存在意义）。**写文件时三条纪律一起**：`encoding="utf-8"`（open 时）+ `allow_unicode=True`（dump 时）+ `line_break="\n"`（Windows 上防 `\r\n` 混入 YAML 文件——**Git 换行策略与 YAML 缩进敏感结合是跨平台事故源**，CI 里统一 `\n`）。

## 3. 排序与键序

**`sort_keys` 默认 True**——字典键按字母排序输出，与源数据的插入顺序无关：

```python
data = {"port": 8080, "host": "localhost", "name": "svc"}
print(yaml.safe_dump(data))
# host: localhost        ← 按字母排了
# name: svc
# port: 8080
```

**什么时候需要 `sort_keys=False`**：配置文件**希望保持书写顺序**（人类可读性：`name` 放前面、`port` 放后面）；diff 友好（改动只在变化处）；**与源文件顺序一致性**（round-trip 场景）。**什么时候默认就好**：程序生成的数据（顺序无意义）、需要确定性输出（CI 快照对比）——**"人读的配置关排序，机器读的配置开排序"**是选型一句话。注意：Python 3.7+ dict 保插入序，`sort_keys=False` 才能利用它。

## 4. 流式风格控制

`default_flow_style` 控制"紧凑 JSON 风格 vs 逐行 block 风格"：

```python
data = {"servers": [{"name": "a", "port": 1}, {"name": "b", "port": 2}]}

# block 风格（default_flow_style=False，默认）——人可读
# servers:
#   - name: a
#     port: 1
#   - name: b
#     port: 2

# flow 风格（default_flow_style=True）——紧凑
# {servers: [{name: a, port: 1}, {name: b, port: 2}]}
```

**选型**：**配置文件用 block**（可读性就是配置的命）；**程序间交换/嵌入其他文本用 flow**（一行搞定、体积小）。还有个中间态：`default_flow_style=False` 时**空映射/空序列仍会输出 flow 形式**（`{}`/`[]`）——这是 PyYAML 的行为细节，别当 bug。

## 5. 自定义对象序列化：representer

**dump 非基础类型会抛 `RepresenterError`**（safe_dump 只认基础类型）。要序列化自定义类，注册 representer——**"如何表示这个对象"的纯函数**：

```python
import yaml
from dataclasses import dataclass, asdict

@dataclass
class Server:
    name: str
    port: int

def server_representer(dumper, data):
    return dumper.represent_dict(asdict(data))

yaml.SafeDumper.add_representer(Server, server_representer)

print(yaml.safe_dump({"main": Server("web", 8080)}))
# main:
#   name: web
#   port: 8080
```

**要点**：representer 是"**把对象压成纯数据**"（asdict/自定义映射）——**与构造函数对称但方向相反**：写出去的是数据、读回来是数据（safe_load 得到 dict 而非 Server——**需要对象就 safe_load 后手动构造**，05 篇红线三的"正确姿势"闭环）。**dataclass + asdict 是最干净的组合**；对象图复杂（嵌套自定义类）时 representer 要递归处理——**"序列化简单化：能转 dict 就转 dict"**。

## 6. dump 与文件写入

```python
def dump_config(config: dict, path: str):
    with open(path, "w", encoding="utf-8", newline="") as f:
        yaml.safe_dump(config, f,
                       allow_unicode=True, sort_keys=False, indent=2)
```

**写文件三件套**：`encoding="utf-8"`（中文）、`newline=""`（防 Windows 自动换行污染 YAML——**YAML 缩进敏感，`\r\n` 在部分解析器里会报错**）、`safe_dump` 显式参数。**原子写**（生产习惯）：先写临时文件再 `os.replace`——**"配置写一半进程崩 = 下次启动读坏配置"**，原子写是配置生成的标准防护（与 07 篇配置管理呼应）。

**dump 的完整实战段落**（"读→改→写"闭环里 dump 的位置，03 篇完整版有全流程代码）：**dump 只负责"数据 → 文本"这一跳**——之前的加载与修改全是数据结构操作，之后的落盘是 `open` 的事——**"dump 是管道的一环，不是全部"**；常见认知错位是"改完配置后 dump 不出来"——**先检查数据结构对不对（print/config 调试），再怀疑 dump 参数**（dump 的报错几乎都是"对象不可表示"而非"数据不对"）。

## 7. 常见坑

**坑一：不传 allow_unicode**——中文全变 `\uXXXX`（第 2 节）。

**坑二：sort_keys 默认开**——配置顺序被打乱，diff 一片红；**人读的配置显式 `sort_keys=False`**。

**坑三：dump 自定义对象报 RepresenterError**——没注册 representer；**先转 dict 再 dump 是最简解**（第 5 节）。

**坑四：Windows 写文件混入 \r\n**——YAML 解析在部分环境报错；**open 加 newline=""**。

**坑五：round-trip 不还原**——dump 后再 load 拿不回原对象类型（safe 体系只保数据）；**期望对象就"数据 + 手动构造"**（第 5 节闭环）。

**坑六：dump 大对象图超慢/爆内存**——递归序列化嵌套结构；**先压成纯 dict（asdict/手动摘字段）再 dump**（第 5 节"能转 dict 就转 dict"）。

**坑七：dump 后手改文件再 dump 覆盖**——程序覆盖人工微调的配置，注释全丢；**"代码生成的配置与手写配置分离"**（生成物进 build 目录，手写配置是源）。

**"dump 的思维模型"**：**dump 是"拍快照"，不是"同步状态"**——它输出的是当前数据的静态表示，**不保留注释、不保留源文件的其他内容、不保留对象身份**（别名会按需生成）；**需要"完整往返保真"（注释/格式）才上 ruamel.yaml**（01 篇选型）。**快照模型的工程含义**：**"dump 输出 = 验收对象"**——配置生成脚本的测试断言直接对比 dump 文本（`assert dump_output == expected`），**"生成的配置要能被断言，才是可回归的配置工程"**；也意味着**"dump 是一次性的，别指望它记住源文件长什么样"**。

**扩展：dump 与安全的关系**（04 篇的对称问题）——**dump 侧没有 RCE 风险**（序列化是"把对象压成文本"，不执行用户代码），但有两个次要注意点：**dump 自定义对象可能意外暴露敏感字段**（representer 把对象全量压平——**用白名单字段而不是 asdict 全量**）；**dump 的输出被下游 unsafe 加载**（你 dump 的是纯数据，但下游如果用 `!!python/object` 标签加工会引狼入室——**"dump 只出纯数据，标签永远不出现"是 dump 侧的安全纪律**）。

## 8. 练习 5 题

1. dump 六个参数各自管什么？为什么说"默认值按机器友好设计"？
2. 中文输出的完整纪律（三条）？\r\n 为什么是 YAML 事故源？
3. sort_keys 的选型判断？"人读关排序、机器读开排序"？
4. block 与 flow 风格的选型？空映射会输出成什么？
5. representer 与 constructor 的对称关系？"能转 dict 就转 dict"？

> 🎯 **核心要点**：dump = **六个显式参数（排序/风格/中文/缩进/分隔/引号）+ 中文三纪律（utf-8 + allow_unicode + newline）+ representer 压纯数据**——"dump 的输出由参数决定，不是由直觉决定；人读的配置每个参数都要显式配"。

---

**下一模块**：[07-文件与配置实战.md](07-文件与配置实战.md) / **返回总览**：[00-pyyaml总览.md](00-pyyaml总览.md)
