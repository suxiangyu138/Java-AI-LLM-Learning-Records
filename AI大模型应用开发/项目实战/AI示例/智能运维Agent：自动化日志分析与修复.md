# 智能运维 Agent：自动化日志分析与修复

> **所属阶段**：阶段三 — Agent 智能体开发
> **技术栈**：Python + LangChain Agent + Shell 工具 + LLM API
> **项目定位**：企业级 Agent 应用，可提交简历

---

## 1. 项目概述

构建一个能自动分析服务器日志、定位异常并生成修复方案的智能运维 Agent。输入"分析服务器日志找出异常"，Agent 自动执行完整的排查流程。

### 核心功能
- 读取并解析日志文件
- 使用正则表达式提取错误信息
- 搜索技术社区寻找解决方案
- 生成结构化的修复报告

---

## 2. Agent 工作流

```
用户输入："分析 /var/log/app.log"
      │
┌─────▼──────────────┐
│ 1. 读取日志文件      │  工具: read_file
│    获取最近 N 行      │
└─────┬──────────────┘
      │
┌─────▼──────────────┐
│ 2. 提取异常信息      │  工具: extract_errors
│    正则匹配 ERROR/   │
│    Exception/Stack  │
└─────┬──────────────┘
      │
┌─────▼──────────────┐
│ 3. 分类与优先级排序  │  工具: classify_errors
│    按类型和频率分组   │
└─────┬──────────────┘
      │
┌─────▼──────────────┐
│ 4. 搜索解决方案      │  工具: search_solution
│    查询 StackOverflow│
│    + 技术文档        │
└─────┬──────────────┘
      │
┌─────▼──────────────┐
│ 5. 生成修复报告      │  输出 Markdown 报告
│    根因 + 方案 + 预防 │
└────────────────────┘
```

---

## 3. 核心实现

### 3.1 工具定义

```python
# ops_tools.py
import re
from langchain_core.tools import tool
from collections import Counter

@tool
def read_log(file_path: str, lines: int = 500) -> str:
    """读取日志文件最后 N 行"""
    try:
        with open(file_path, "r", errors="ignore") as f:
            all_lines = f.readlines()
            return "".join(all_lines[-lines:])
    except FileNotFoundError:
        return f"错误: 文件 {file_path} 不存在"
    except Exception as e:
        return f"读取失败: {str(e)}"

@tool
def extract_errors(log_content: str) -> str:
    """从日志内容中提取所有 ERROR/WARN/Exception"""
    patterns = {
        "ERROR": r"ERROR[^\n]*",
        "WARN": r"WARN[^\n]*",
        "Exception": r"\w+Exception[^\n]*",
        "Stack": r"^\s+at\s+[^\n]+",
    }
    results = {}
    for name, pattern in patterns.items():
        matches = re.findall(pattern, log_content, re.MULTILINE)
        if matches:
            results[name] = matches
    return json.dumps(results, indent=2, ensure_ascii=False)

@tool
def classify_errors(error_json: str) -> str:
    """对错误进行分类和优先级排序"""
    errors = json.loads(error_json)
    all_errors = []
    for category, items in errors.items():
        all_errors.extend([(category, item) for item in items])

    counter = Counter(cat for cat, _ in all_errors)

    report = "## 错误分类报告\n\n"
    report += f"总计: {len(all_errors)} 条异常\n\n"
    for cat, count in counter.most_common():
        report += f"- **{cat}**: {count} 条\n"
    report += "\n### 详细列表\n"
    for cat, msg in all_errors[:10]:  # 前 10 条
        report += f"- [{cat}] {msg.strip()[:100]}\n"
    return report

@tool
def search_solution(error_message: str) -> str:
    """搜索技术社区寻找解决方案"""
    # 简化版 — 实际可接入 Tavily/Google Search API
    solutions = {
        "OutOfMemoryError": "建议: 1) 增加堆内存 -Xmx 2) 排查内存泄漏 3) 使用 jmap 分析 dump",
        "NullPointerException": "建议: 1) 添加 null 检查 2) 使用 Optional 3) 检查调用链",
        "Connection refused": "建议: 1) 检查目标服务是否运行 2) 检查防火墙 3) 检查端口",
    }
    for key, solution in solutions.items():
        if key.lower() in error_message.lower():
            return f"找到: {solution}"
    return "未找到精确匹配，建议查看官方文档或提交 issue"
```

### 3.2 Agent 组装

```python
# ops_agent.py
from langchain.agents import AgentExecutor, create_openai_functions_agent
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from ops_tools import read_log, extract_errors, classify_errors, search_solution

class OpsAgent:
    """智能运维 Agent"""

    def __init__(self, api_key, base_url="https://api.deepseek.com"):
        self.llm = ChatOpenAI(
            model="deepseek-chat",
            api_key=api_key,
            base_url=base_url,
            temperature=0
        )
        self.tools = [read_log, extract_errors, classify_errors, search_solution]
        self.executor = self._create_executor()

    def _create_executor(self):
        prompt = ChatPromptTemplate.from_messages([
            ("system", """你是资深运维工程师。按以下流程分析日志：

1. 先读取日志文件
2. 提取错误信息
3. 对错误分类排序
4. 搜索解决方案
5. 生成 Markdown 格式的修复报告

保持冷静，逐步执行，每步向用户报告进度。"""),
            ("human", "{input}"),
            MessagesPlaceholder("agent_scratchpad")
        ])

        agent = create_openai_functions_agent(self.llm, self.tools, prompt)
        return AgentExecutor(
            agent=agent, tools=self.tools,
            verbose=True, max_iterations=10,
            handle_parsing_errors=True
        )

    def analyze(self, log_path, context=""):
        prompt = f"分析日志文件 {log_path}"
        if context:
            prompt += f"\n用户补充: {context}"
        return self.executor.invoke({"input": prompt})["output"]

# 使用
agent = OpsAgent(api_key="sk-xxx")
report = agent.analyze("/var/log/app.log")
print(report)
```

---

## 4. 使用示例

```
用户: 分析 /var/log/spring-app.log，最近频繁 500 错误

Agent:
> 正在读取日志文件 /var/log/spring-app.log...
> 发现 23 条异常记录
> 错误分类:
>   - NullPointerException: 12 条
>   - SQLException: 8 条
>   - TimeoutException: 3 条
>
> ## 修复报告
>
> ### 1. NullPointerException (12次) — 严重
> **根因**: UserService.getUser() 返回 null
> **方案**: 添加 Optional<User> 包装，全局 null 检查
>
> ### 2. SQLException (8次) — 严重
> **根因**: 连接池耗尽，Connection pool exhausted
> **方案**: 增大 HikariCP maximumPoolSize → 20
>
> ### 3. TimeoutException (3次) — 中等
> **根因**: 第三方 API 超时
> **方案**: 设置 feign.client.config.default.read-timeout=5000
```

---

## 5. 扩展方向

| 方向 | 说明 |
|------|------|
| 自动修复 | 经人工确认后自动执行修复脚本 |
| 告警集成 | 对接 Prometheus/Grafana，自动触发分析 |
| 知识积累 | 每次分析结果存入知识库，形成运维知识图谱 |
| 定时巡检 | CronJob 定时扫描日志，主动发现隐患 |
