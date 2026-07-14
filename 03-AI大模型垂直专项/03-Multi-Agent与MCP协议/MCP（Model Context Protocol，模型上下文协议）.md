# 🔌 MCP（Model Context Protocol，模型上下文协议）

> **核心摘要**：MCP 是 Anthropic 推出的开放标准，相当于 AI 世界的 USB-C 通用接口——让任何大模型以标准化、安全的方式调用任何外部工具/数据。传统方式需要 M × N 套适配代码，MCP 只需 M + N，一次开发所有模型可用。

**前置阅读**：[[基于大模型的RAG应用开发与优化——Data Agent开发核心知识点大全]] | [[MCP协议深度解析]]

---

## 一、一句话懂 MCP

**MCP**（Model Context Protocol）= AI 世界的 USB-C 通用接口。由 Anthropic 推出的开放标准，作用：让任何大模型标准化、安全地调用任何外部工具/数据（文件、数据库、API、本地程序）。

> **解决的痛点**：传统 M 个模型 × N 个工具 = M×N 套适配代码，成本高、不可复用。MCP 统一协议 → M+N，一次开发，所有模型可用。

## 二、MCP 核心架构（客户端-服务器）

### 角色

| 角色 | 说明 |
|------|------|
| **MCP Host（主机）** | AI 客户端，如 Claude、Cursor、VS Code Copilot |
| **MCP Client（客户端）** | Host 里的模块，负责和 Server 通信 |
| **MCP Server（服务器）** | 把工具/数据包装成标准 MCP 服务 |

### 流程（3 步闭环）

1. **能力交换**：Host 问 Server：你有哪些工具？Server 返回工具列表 + 参数说明
2. **动态调用**：用户提问 → LLM 决定用哪个工具 → Client 发请求给 Server
3. **结果返回**：Server 执行 → 返回结果 → LLM 整合回答

### 类比（好记）

| 概念 | 类比 |
|------|------|
| **LLM** | 大脑（会思考，但没手脚） |
| **Skill** | 手脚（具体能力） |
| **MCP** | 统一的神经接口 |
| **Agent** | 调度员（决定何时用哪个手脚） |

## 三、MCP 和相关概念的关系

| 概念 | 关系 |
|------|------|
| **RAG** | 一种"文档检索 Skill"，可封装为 MCP Server |
| **Skill/Tool** | MCP Server 提供的能力单元 |
| **Agent** | MCP 的调用方（Host），负责决策调度 |
| **Function Calling** | MCP 的子集/前身（只支持简单函数调用） |

> **一句话**：MCP 是标准化、企业级、可远程的 Skill 调用协议，是 Agent 的基础设施。

## 四、MCP 核心能力（5 类）

| 能力 | 说明 |
|------|------|
| 文件操作 | 读写本地/服务器文件 |
| 数据库查询 | MySQL/PostgreSQL/Redis |
| API 调用 | 天气、搜索、支付、私有接口 |
| 代码执行 | 运行 Python/Shell、数据分析 |
| 多模型协作 | Server 主动调用其他 LLM |

## 五、快速上手：3 步跑通 MCP

### 前提

```bash
pip install mcp
```

### 1. 写一个最简 MCP Server

```python
# mcp_server_demo.py
from mcp.server import Server

server = Server("demo-server")

@server.tool()
def add(a: int, b: int) -> int:
    """两数相加"""
    return a + b

if __name__ == "__main__":
    server.run()
```

### 2. 启动 Server

```bash
python mcp_server_demo.py
```

### 3. 用 Client 调用

```python
# mcp_client_demo.py
from mcp.client import Client

client = Client("http://localhost:8000")
print(client.list_tools())
result = client.call_tool("add", {"a": 2, "b": 3})
print(result)  # 输出 5
```

## 六、实战场景

| 场景 | 说明 |
|------|------|
| VS Code + MCP | AI 读写本地项目、查数据库、执行脚本 |
| 私有知识库 RAG | 向量库封装为 MCP Server |
| 企业数据安全 | 数据不出内网，MCP Server 本地部署 |
| 多工具编排 | Agent 同时调用多个 MCP Server |

## 七、学习路线（2 小时速成）

| 阶段 | 时间 | 内容 |
|------|------|------|
| 理解概念 | 10 分钟 | 定义、架构、与 RAG/Agent 关系 |
| 跑通 Demo | 30 分钟 | Server + Client 极简示例 |
| 封装 Server | 60 分钟 | 文件检索 Server + RAG 检索 Server |
| 多 Server 调用 | 20 分钟 | Agent 调用多个 MCP Server |

---

## 核心要点回顾

- MCP = AI 的 USB-C，标准化 LLM 与工具的连接
- 架构：Host → Client ↔ Protocol ↔ Server → 工具
- 流程：能力交换 → 动态调用 → 结果返回
- 与 RAG/Agent 关系：RAG 是 Skill，Agent 是调度方，MCP 是协议

## 参考资料

1. [[MCP协议深度解析]]
2. [[基于大模型的RAG应用开发与优化——Data Agent开发核心知识点大全]]
3. [[快速吃透 LangChain]]
