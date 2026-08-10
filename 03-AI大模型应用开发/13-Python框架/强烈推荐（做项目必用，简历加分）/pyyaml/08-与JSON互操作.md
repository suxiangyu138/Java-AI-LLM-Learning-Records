# 08 - 与 JSON 互操作

> 定位：YAML 与 JSON 的"子集关系"与互转实践——JSON 是 YAML 的子集、PyYAML 直接吃 JSON、dump 出 JSON 风格、性能差异——"格式选型表"让每个场景都选对格式

---

## 📚 目录

1. [JSON 是 YAML 的子集](#1-json-是-yaml-的子集)
2. [用 PyYAML 读 JSON](#2-用-pyyaml-读-json)
3. [dump 出 JSON 风格](#3-dump-出-json-风格)
4. [性能对比](#4-性能对比)
5. [格式选型表](#5-格式选型表)
6. [实战：JSON/YAML 转换工具](#6-实战jsonyaml-转换工具)
7. [常见坑](#7-常见坑)
8. [练习 5 题](#8-练习-5-题)

---

## 1. JSON 是 YAML 的子集

**核心事实：任何合法 JSON 都是合法 YAML**——JSON 的语法（`{}`、`[]`、`"key"`、逗号）完全落在 YAML 的 flow 风格语法内（02 篇提过 flow 风格）。三个推论：

**推论一：YAML 解析器可以直接解析 JSON 文件**——`yaml.safe_load(json_text)` 返回和 `json.loads(json_text)` 一样的结果——**"YAML 是 JSON 的超集，YAML 工具链天然兼容 JSON 数据"**。

**推论二：反过来不成立**——YAML 的缩进、锚点、多文档、标签、无引号字符串在 JSON 里全是非法语法——**"能写 YAML 的地方不一定能写 JSON"**。

**推论三：类型差异**——YAML 1.1（PyYAML 默认）把 `yes/no/on/off` 当布尔、裸数字 `012` 有歧义，**JSON 的类型语义更严格**（只有 true/false、数字规范）——**"从 JSON 迁移到 YAML 时布尔值要检查"**（02 篇布尔陷阱）。这条子集关系是"为什么很多工具同时接受 .json 与 .yaml 配置"的底层原因（docker-compose 两者皆可就是例子）。

**同数据的两种形态**（对照记忆）：

```json
{"server": {"host": "localhost", "port": 8080}, "tags": ["web", "api"]}
```

```yaml
server:
  host: localhost
  port: 8080
tags:
  - web
  - api
```

**同一份数据两种写法**——JSON 紧凑机器友好，YAML 展开人眼友好；**"配置文件写成 YAML 形态、接口数据用 JSON 形态"**就是子集关系在日常的分工落地。

## 2. 用 PyYAML 读 JSON

```python
import json, yaml

data = json.loads(json_text)        # 标准姿势
data2 = yaml.safe_load(json_text)   # 也可以——结果一样

# 实际应用：管道里混着 json 与 yaml 数据源时统一入口
def load_any(path: str):
    with open(path, encoding="utf-8") as f:
        if path.endswith(".json"):
            return json.load(f)          # JSON 走标准库（快）
        return yaml.safe_load(f)         # YAML 走 PyYAML
```

**实用姿势**：**JSON 文件用 `json.load`（快且语义精确），YAML 用 safe_load**——混用场景（配置目录里 .json/.yaml 都有）用上面的 `load_any` 统一入口。**"能解析"≠"应该解析"**：YAML 解析 JSON 有类型语义差异（第 1 节推论三），**程序生成的 JSON 数据继续用 json 模块**，YAML 解析器吃 JSON 只在"工具兼容性"场景（拿 YAML 工具链处理别人给的 JSON 配置）。

## 3. dump 出 JSON 风格

把 Python 数据输出成 JSON 兼容的 YAML（flow 风格），**作为"多格式输出"的姿势**：

```python
import yaml, json

data = {"servers": [{"name": "a", "port": 1}]}

yaml.safe_dump(data, default_flow_style=True)
# {servers: [{name: a, port: 1}]}          ← flow 风格，接近 JSON

json.dumps(data)
# {"servers": [{"name": "a", "port": 1}]}  ← 真 JSON
```

**三个差异要分清**：flow 风格 YAML **仍不是 JSON**（键可以无引号、布尔可能 1.1 语义）；**"dump 成 JSON"的正确姿势是用 json 模块**（别拿 YAML 硬凑）；**转换工具（第 6 节）里"YAML → JSON"走 load + dumps 而不是流式转换**。**"flow 风格是给人看的紧凑 YAML，不是 JSON"**——这句话避免 90% 的混用困惑。

## 4. 性能对比

| 操作 | json 模块 | PyYAML（纯 Python） | PyYAML（CLoader） |
|------|-----------|---------------------|-------------------|
| 解析 | 极快（C 实现） | 慢（2-10 倍差距） | 接近 json（2-10 倍加速） |
| 序列化 | 极快 | 慢 | 快 |
| 典型用途 | 程序数据交换 | 配置/人读文件 | 性能敏感的大配置 |

**性能心智**：**JSON 的 C 实现（json 模块就是 C）与 PyYAML 纯 Python 差距是数量级的**；PyYAML 的 C 扩展（libyaml）把它拉回同一量级。**选型含义**：**高频解析/大文件用 json 或 CLoader**（09 篇）；**配置场景（启动读一次）性能无所谓**，可读性优先——**"别用性能当不用 YAML 的借口，也别用可读性当不用 JSON 的借口"**。

**实测方法论**（性能对比不是拍脑袋，09 篇有完整 timeit 代码）：

```python
import json, timeit, yaml
text = open("config.yaml", encoding="utf-8").read()   # 同一份数据
json_text = json.dumps(json.loads(yaml.safe_load(text)))  # 转成 JSON 形态

t_json = timeit.timeit("json.loads(json_text)", globals=globals(), number=100)
t_yaml = timeit.timeit("yaml.safe_load(text)", globals=globals(), number=100)
print(f"json: {t_json:.3f}s | yaml: {t_yaml:.3f}s | 差距 {t_yaml/t_json:.1f}x")
```

**结论驱动决策**：差距 <3 倍的场景（小配置）继续 YAML；**差距 10 倍级的高频管道换 JSON/CLoader**——**"性能差距用数据说话，格式选择用场景说话"**。

## 5. 格式选型表

| 场景 | 选择 | 理由 |
|------|------|------|
| 配置文件（人写） | **YAML** | 可读性 + 锚点/注释/多文档 |
| 程序间数据交换 | **JSON** | 严格语义 + C 速度 + 生态通用 |
| API 请求/响应 | JSON | 事实标准（HTTP 生态） |
| CI/部署清单 | YAML | K8s/Actions/compose 的格式惯例 |
| 日志结构化 | JSON | 采集端解析（与 loguru 09 篇一致） |
| 大体积数据文件 | JSON/其他 | YAML 缩进膨胀 + 解析慢 |
| 需要注释的配置 | YAML | JSON 不支持注释（TOML 是另一选项） |

**一条主线**：**"人写的配置用 YAML，机器间的数据用 JSON"**；YAML 的边界是"大 + 频繁解析"（缩进占体积、纯 Python 慢）；**配置 vs 数据的分工判断**是面试常问的选型题（10 篇范式）。

**迁移决策场景**（"现有 JSON 配置要不要迁 YAML"）三步判断：**一**，谁在写——人写/频繁人工编辑（迁 YAML，注释与可读性收益大）、程序生成（留 JSON，省事）；**二**，多大多频——大且高频解析（留 JSON，性能）、小且低频（迁 YAML 无成本）；**三**，生态惯例——所在的工具链/K8s/框架用什么格式就跟随什么（**"惯例优先于偏好"**——别跟生态对着干）。**结论**：**"人不读的 JSON 别迁，人常读的 JSON 值得迁"**是迁移决策的一句话口诀。

## 6. 实战：JSON/YAML 转换工具

```python
#!/usr/bin/env python3
"""json2yaml / yaml2json：多格式配置转换小工具"""
import json, sys, yaml

def convert(src: str, dst: str):
    with open(src, encoding="utf-8") as f:
        data = yaml.safe_load(f) if src.endswith((".yaml", ".yml")) else json.load(f)
    with open(dst, "w", encoding="utf-8", newline="") as f:
        if dst.endswith((".yaml", ".yml")):
            yaml.safe_dump(data, f, allow_unicode=True, sort_keys=False)
        else:
            json.dump(data, f, ensure_ascii=False, indent=2)

convert("config.json", "config.yaml")   # JSON → YAML（人可读）
convert("config.yaml", "config.json")   # YAML → JSON（程序用）
```

**要点**：双向转换都走"**load 成数据 → dump 成目标格式**"（没有流式直转）；**YAML → JSON 时锚点已展开**（引用没了——转换即摊平）；**allow_unicode/ensure_ascii=False 保证中文可读**。生产小技巧：**转换后跑一遍"load 回来对比"**——结构一致才算转换成功（测试脚本标配）。

## 7. 常见坑

**坑一：以为 flow 风格 YAML 就是 JSON**——键无引号、布尔语义差异；**要 JSON 就用 json 模块**（第 3 节）。

**坑二：JSON 的 true/false 与 YAML 1.1 的 yes/no 混用**——跨格式迁移时布尔悄悄变字符串/变布尔；**迁移后全量校验**（第 1 节推论三）。

**坑三：用 YAML 解析器处理海量 JSON 数据**——性能差距数量级；**数据管道用 json**（第 4 节）。

**坑四：转换时锚点丢失以为是 bug**——转换即摊平是语义；**文档化这个行为**（第 6 节）。

**坑五：配置文件里塞 JSON 字符串**——`config: "{\"a\": 1}"` 字符串嵌套 JSON——**直接写嵌套结构**（YAML 就是超集，别双重编码）。

**坑六：转换工具丢注释/格式抱怨"转换不可逆"**——YAML → JSON 天然丢注释（JSON 不支持注释）；**转换是"数据迁移"不是"格式保真"**——需要保真的留 YAML，需要标准的转 JSON（第 5 节主线）。**坑七：忽略 YAML 1.1 与 1.2 的差异**——`yes/no` 布尔语义在 YAML 1.2 是字符串、PyYAML 默认 1.1 是布尔——**跨工具链（K8s 用 1.2、PyYAML 用 1.1）时布尔值行为可能不一致**；**敏感布尔字段一律显式 `true/false` + 引号按需**（02 篇布尔陷阱的工程延伸）。

## 8. 练习 5 题

1. "JSON 是 YAML 的子集"的三个推论？反向为什么不成立？
2. load_any 统一入口解决了什么？为什么"能解析 ≠ 应该解析"？
3. flow 风格 YAML 与 JSON 的三个差异？dump 成 JSON 的正确姿势？
4. 性能对比表说明了什么？"别用性能当借口，也别用可读性当借口"？
5. 格式选型表的主线是什么？双向转换的标准流程？

> 🎯 **核心要点**：JSON 互操作 = **子集关系（YAML 吃 JSON，反向不行）+ 分工主线（人写配置 YAML、机器数据 JSON）+ 转换走"load→dump"摊平锚点**——"格式选型一句话：人读的 YAML，机器读的 JSON"。

---

**下一模块**：[09-性能与生产实践.md](09-性能与生产实践.md) / **返回总览**：[00-pyyaml总览.md](00-pyyaml总览.md)
