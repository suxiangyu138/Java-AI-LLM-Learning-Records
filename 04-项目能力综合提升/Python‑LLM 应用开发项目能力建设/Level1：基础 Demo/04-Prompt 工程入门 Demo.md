# 04 Prompt 工程入门 Demo

> 调通模型后第一件要做的事：让回答可控。本 Demo 用三个递进的小实验（角色任务约束、few-shot 示例、JSON 结构化输出）建立 Prompt 工程的最小方法论，让大模型从"会聊天"变成"能干活"。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [Prompt 三要素：角色、任务、约束](#2-prompt-三要素角色任务约束)
3. [few-shot：用示例教模型](#3-few-shot用示例教模型)
4. [JSON 结构化输出：让模型返回数据](#4-json-结构化输出让模型返回数据)
5. [提示词模板化与版本管理](#5-提示词模板化与版本管理)
6. [失败模式与迭代方法](#6-失败模式与迭代方法)

---

## 1. 目标与验收

本 Demo 的产出：三个可运行的实验脚本——一个带完整提示词的问答、一个 few-shot 分类、一个 JSON 结构化输出。验收标准：**能独立写出"角色+任务+约束"三件套的 system 提示词**；**能解释 few-shot 为什么比反复描述更有效**；**能稳定解析模型返回的 JSON 并处理解析失败**。与仓库「02-大模型基础与Prompt工程」体系的分工：那边是理论全景（思维链、迭代提示等），这里是最小可用的四个技法，先动手建立体感。

## 2. Prompt 三要素：角色、任务、约束

好的提示词可以拆成三个部分，缺一个回答质量就掉一截：**角色**（你是谁）限定知识范围与口吻；**任务**（做什么）要具体到可执行的动词；**约束**（怎么做/不要怎么做）堵住常见失败模式。对比实验最能建立体感——同样的任务，只有"帮我写个 Python 函数"和完整三件套的回答质量天差地别：

```python
system_prompt = """
你是一位资深 Python 工程师，擅长编写简洁、可测试的代码。

任务：根据用户需求编写 Python 函数。

约束：
1. 只输出函数代码，不要任何解释文字
2. 函数必须有类型注解和 docstring
3. 优先使用标准库
4. 如果需求不明确，先列出需要澄清的问题，不要猜测
"""
```

三个容易被忽略的细节：**约束用"不要"句式堵住具体失败模式**（上面第 1 条直接省掉解析代码的麻烦）；**给模型"澄清优先"的逃生通道**（第 4 条）——没有它模型会编造需求；**系统提示词写在代码里要避免 f-string 误解析**（大括号与变量冲突），模板化见第 5 节。验证方法：同一个任务分别用"无提示词"和"三件套"跑一次，对比输出——这个对比实验本身就是面试可讲的素材。

## 3. few-shot：用示例教模型

描述类约束（"输出要简洁"）不如**给例子**（few-shot）。示例就是"教学数据"，模型从例子中学格式、学风格、学边界，比抽象描述准确得多。典型场景：分类、抽取、格式转换。

```python
few_shot_prompt = """
判断用户问题属于哪个类别：招聘 / 项目 / 技术 / 其他。

示例1：
问题：今年秋招什么时候开始？
类别：招聘

示例2：
问题：你的 RAG 项目用什么向量库？
类别：项目

示例3：
问题：Python 的 GIL 是什么？
类别：技术

问题：你用过 uv 吗，和 pip 有什么区别？
类别：
"""
```

三个要点：**示例覆盖边界情况**（上面三个示例分别对应"问题+类别"的格式、上下文中的词汇混淆、技术问题）；**示例与真实输入保持同分布**（分类的示例也要是"问题+类别"格式，格式漂移会导致输出格式错乱）；**示例数量 2-5 个足够**（多了占 token 且收益递减）。few-shot 的进阶形态是"让模型先给示例再分类"（思维链的一种），Demo 阶段用直接示例即可。

## 4. JSON 结构化输出：让模型返回数据

AI 应用的终极需求：模型返回**程序可直接用的数据**（JSON），而不是散文。两条路线：**对话式要求**（提示词里写清 JSON 格式）与 **response_format 强制**（SDK 层限制输出格式）。2026 年主流模型都支持后者，优先用：

```python
resp = client.chat.completions.create(
    model="deepseek-v4-flash",
    messages=[
        {"role": "system", "content": "你是信息抽取助手。只输出 JSON。"},
        {"role": "user", "content": "从这段话中抽取技能点：我在项目里用 FastAPI 做过 RAG 问答，还用 Streamlit 搭了界面。"},
    ],
    response_format={"type": "json_object"},
)
import json
data = json.loads(resp.choices[0].message.content)
print(data)   # {"skills": ["FastAPI", "RAG", "Streamlit"]} 之类
```

无论走哪条路，**解析都必须防御**——模型输出不可信：`json.loads` 可能失败（输出里混了代码块标记 `json`），所以要 try/except 并提示模型修正，或写个清理函数（strip 掉标记）。**JSON 场景 temperature 设 0**（确定性优先）。进阶：要求模型"先定义 JSON Schema 再按 Schema 输出"（self-consistent schema），Level2 用 Pydantic 做正式校验，Demo 阶段记住"解析必防御"即可。

一个实用的解析失败兜底模式：解析失败时**把错误信息回喂给模型**（"你的输出不是合法 JSON：{错误}，请重新输出纯 JSON"），多数模型一次修正就能成功——这个"错误回传"模式在 07 篇工具调用里是标配，先在 JSON 解析里练熟它。

## 5. 提示词模板化与版本管理

提示词写死在代码里会带来两个问题：**改提示词要改代码**（调试成本高）、**系统提示词里的 {大括号} 与 f-string 冲突**（越狱式报错）。标准做法：提示词独立成文件/常量，用模板填充变量：

```python
# prompts.py —— 提示词集中管理，本文件只放提示词，不写逻辑
SYSTEM_TEMPLATE = """你是一位{role}，擅长{expertise}。
任务：{task}
约束：{constraints}"""

JSON_EXTRACT_TEMPLATE = """从以下文本抽取{target}，输出 JSON：
文本：{text}
输出格式：{"items": [...]}"""

# 使用时
from prompts import SYSTEM_TEMPLATE
prompt = SYSTEM_TEMPLATE.format(role="Python 工程师", expertise="编写简洁代码", task="写函数", constraints="1. 只输出代码")
```

三个习惯：**提示词与代码分离**（独立 prompts.py 或 .md 文件，改提示词不动代码逻辑）；**变量用 .format 或 string.Template 填充**（避开 f-string 的大括号冲突，提示词里大量使用 JSON 示例时这个坑必踩）；**提示词版本留痕**（每个版本记录改动与效果——"加了约束第 3 条后输出规范了"这类笔记在面试讲项目时是金素材）。提示词本质上也是代码——会改、会迭代、要管理——这句结论在 Level2 引入 LangChain 的 PromptTemplate 时会被再次印证，本模块的模板化习惯直接迁移过去。

## 6. 失败模式与迭代方法

Prompt 迭代是"假设-验证"循环，先知道常见失败模式再动手改。下面每条都配了对应的修法——记住"失败模式 → 修法"的对应关系，比背提示词模板更本质。**输出格式漂移**：说好的 JSON 输出却带了解释文字——用 response_format 强制 + 示例固定格式；**幻觉**：模型编造不存在的事实——加约束"不知道就说不知道"，或引入 RAG 检索（06 篇）；**答非所问**：任务描述太泛——拆成单任务，用动词开头；**截断**：回答被 max_tokens 截断——增大上限或让模型分步回答；**中文混乱**：中英混杂——约束"统一使用中文"。

迭代方法论三条：**一次只改一个变量**（同时改提示词和模型参数，无法判断哪个生效）；**用固定测试集回归**（同一批 5-10 个输入，每次改完全量重跑，防"改好 A 弄坏 B"）；**记录效果再改**（每次迭代记录"改了什么、结果如何"，这是面试讲优化过程的素材）。把测试集写成可重跑的脚本是这套方法的载体：

```python
# eval_qa.py —— 提示词改动的回归测试
TEST_CASES = [
    ("提取技能点：我会 FastAPI 和 Streamlit", ["FastAPI", "Streamlit"]),
    ("提取技能点：写过 Java 后端", ["Java"]),
]

def run_eval(questions_and_expect: list[tuple[str, list[str]]]) -> None:
    for question, expect in questions_and_expect:
        result = extract_skills(question)
        hit = all(k in result for k in expect)
        print(f"{'✅' if hit else '❌'} {question} → {result}（期望 {expect}）")

run_eval(TEST_CASES)
```

记住边界：**Prompt 工程调的是"模型的表达"**，模型本身能力不够时（数学、长逻辑），调提示词只能小幅改善——该换模型就换模型（flash 换 pro），该上 RAG 就上 RAG（把知识补进上下文）。"提示词不行先加示例、示例不行再换模型、换模型不行上 RAG"就是迭代的优先级顺序。

> 🎯 **核心要点**：Prompt 工程的最小方法论是**三件套起步、few-shot 教会、JSON 约束产出、模板集中管理、防御式解析**。本 Demo 跑完这三个实验，你就具备构建 06 篇 RAG 检索提示词和 07 篇工具调用提示词的全部基础。

---

**下一模块**：[05 Streamlit 对话 Web Demo](./05-Streamlit%20对话%20Web%20Demo.md) | **返回总览**：[Level1 总览](./00-Level1%20基础%20Demo%20总览.md)

【参考来源】
- [Prompt Engineering Guide（OpenAI）](https://platform.openai.com/docs/guides/prompt-engineering)
- [DeepSeek API Docs - 提示词与结构化输出](https://api-docs.deepseek.com/zh-cn/)
- [Prompt Templates | LangChain Docs](https://python.langchain.com/docs/concepts/prompt_templates/)
