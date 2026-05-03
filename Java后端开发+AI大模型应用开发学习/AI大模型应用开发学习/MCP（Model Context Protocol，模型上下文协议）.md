04.27 10:39
MCP（Model Context Protocol，模型上下文协议）
一、一句话懂 MCP
MCP = AI 世界的 USB‑C 通用接口
- 由 Anthropic 推出的开放标准
- 作用：让任何大模型，标准化、安全地调用任何外部工具/数据（文件、数据库、API、本地程序）
解决的痛点（必记）
传统：M 个模型 × N 个工具 = M×N 套适配代码，成本高、不可复用
MCP：统一协议 → M+N，一次开发，所有模型可用
 
二、MCP 核心架构（客户端‑服务器）
1）角色
- MCP Host（主机）：AI 客户端，如 Claude、Cursor、VS Code Copilot、你自己的 Agent 
- MCP Client（客户端）：Host 里的模块，负责和 Server 通信
- MCP Server（服务器）：把「工具/数据」包装成标准 MCP 服务（一个 Server 对应一类能力）
2）流程（3 步闭环）
1. 能力交换：Host 问 Server：你有哪些工具？Server 返回工具列表+参数说明
2. 动态调用：用户提问 → LLM 决定用哪个工具 → Client 发请求给 Server
3. 结果返回：Server 执行（查文件/调API）→ 返回结果 → LLM 整合回答
类比（好记）
- LLM = 大脑（会思考，但没手脚）
- Skill = 手脚（具体能力）
- MCP = 统一的神经接口（让大脑安全、标准化控制各种手脚）
- Agent = 调度员（决定何时用哪个手脚）
 
三、MCP 和你学过的概念关系（必考）
- RAG：一种「文档检索 Skill」，可封装为 MCP Server
- Skill/Tool：MCP Server 提供的能力单元
- Agent：MCP 的调用方（Host），负责决策调度
- Function Calling：MCP 的子集/前身（只支持简单函数调用；MCP 更通用、安全、双向）
一句话：MCP 是标准化、企业级、可远程的 Skill 调用协议，是 Agent 的基础设施。
 
四、MCP 核心能力（5 类）
1. 文件操作：读写本地/服务器文件（AI 写代码、分析文档）
2. 数据库查询：MySQL/Postgres/Redis（查业务数据）
3. API 调用：天气、搜索、支付、私有接口
4. 代码执行：运行 Python/Shell、数据分析、脚本任务
5. 多模型协作：Server 主动调用其他 LLM，实现复杂工作流
 
五、快速上手：3 步跑通 MCP（极简版）
前提
- 安装 Python 3.10+
- 安装 MCP SDK：
bash
pip install mcp
 
1）写一个最简 MCP Server（工具端）
python
# mcp_server_demo.py
from mcp.server import Server
from mcp.types import Tool
server = Server("demo-server")
# 定义一个工具：加法
@server.tool()
def add(a: int, b: int) -> int:
    """两数相加"""
    return a + b
if __name__ == "__main__":
    server.run()
 
2）启动 Server
bash
python mcp_server_demo.py
 
3）用 Client（Agent/LLM）调用
python
# mcp_client_demo.py
from mcp.client import Client
client = Client("http://localhost:8000")
# 列出可用工具
print(client.list_tools())
# 调用 add 工具
result = client.call_tool("add", {"a": 2, "b": 3})
print(result)  # 输出 5
 
这就是完整 MCP 闭环：Server 暴露能力 → Client 发现并调用
 
六、实战场景（直接能用）
1. VS Code + MCP：AI 读写本地项目、查数据库、执行脚本 
2. 私有知识库 RAG：把向量库封装成 MCP Server，所有 Agent 可调用
3. 企业数据安全：数据不出内网，MCP Server 本地部署，LLM 远程调用
4. 多工具编排：Agent 同时调用「搜索 MCP + 数据库 MCP + 代码执行 MCP」
 
七、MCP 学习路线（2 小时速成）
1. 10 分钟：理解定义、架构、和 RAG/Agent 关系
2. 30 分钟：跑通上面极简 Demo（Server + Client）
3. 60 分钟：封装 2 个实用 Server
- 文件检索 Server（对接本地文档）
- RAG 检索 Server（对接向量库）
4. 20 分钟：用 Agent 调用多个 MCP Server，完成复杂任务
 
八、背诵口诀（快速回忆）
- MCP = AI 的 USB‑C
- LLM 动脑，Skill 动手，MCP 通接口，Agent 来调度
- 一次开发，全模型复用；本地数据，安全可控

