# AI Agent 智能体开发全套教程（91 集）

> 从 0 搭建 Agent：MCP 协议、工具调用、工作流、项目实战全覆盖。

---

## 第 0 章：大模型基础（2 节）

- `00` 课程简介
- `01` 认识大模型

## 第 1 章：大模型应用与提示词入门（4 节）

- `02-03` 模型应用方向 + Gradio 入门
- `04` 提示词工程
- `05` 提示词实战：图片生成

## 第 2 章：AI 智能体基础认知（8 节）

- `06-07` 智能体能力展示 + 主流产品对比（DeepResearch、Manus）
- `08` 智能体核心特性：自主性、适应性、交互性
- `09` 智能体和大模型的区别
- `10` 智能体 5 级分层及商业应用
- `11` 智能体技术架构讲解：LangGraph + LLM + Tools + MCP
- `12` 本章知识总结和回顾

## 第 3 章：Python 环境 & 大模型调用（9 节）

- `13` Anaconda 多版本管理
- `14` Python 项目管理工具 uv
- `15` Ollama 本地大模型部署
- `16` LangChain-Ollama 调用本地模型
- `17` Ollama 流式调用
- `18` 阿里云百炼平台大模型调用
- `19` 百炼推理大模型调用
- `20` LangChain 框架基本概念
- `21` 本章重点回顾

## 第 4 章：LangChain 提示词模板与自定义工具（9 节）

- `22` LangChain + Qwen 大模型 + Pydantic Settings
- `23-25` 提示词模板：PromptTemplate / ChatPromptTemplate / FewShotPromptTemplate
- `26` 提示词模板对比及链式调用
- `27` 大模型调用自定义工具全流程
- `28` `@tool` 装饰器注册工具 + args_schema 控制参数
- `29` 重点回顾

## 第 5 章：输出解析器 & 企业级工具实战（8 节）

- `30` LangChain Agents 快速创建和调用
- `31` JsonOutputParser 规范返回值
- `32-33` PythonPerlTool 自动编写企业官网
- `34` 智能体官网提示词优化
- `35-36` LangChain 解析器：精准控制格式 + DateOutputParser
- `37` 重点回顾

## 第 6 章：MCP 协议原理 & 高德 MCP 实战（8 节）

- `38` MCP 原理和发展现状
- `39` 高德 MCP 服务接入原理
- `40` LangChain MCP Adapters 创建客户端
- `41` 结合高德 MCP 使智能体具备位置服务能力
- `42` 复杂路径规划 + 可视化展示
- `43` MCP 通讯协议 stdio：本地 MCP 服务端+客户端
- `44` 重点回顾

## 第 7 章：Playwright / GitHub / 高德 MCP & Cursor 集成（10 节）

- `45-46` Node 环境搭建 + LangChain + MCP 读取 Playwright 工具
- `47` LangGraph + create_react_agent 创建智能体 Pipeline
- `48-49` Cursor 接入 Playwright MCP + 工具执行流程分析
- `50` Cursor + GitHub MCP 服务集成
- `51` Cursor + 高德 MCP 旅行计划
- `52` 小项目：Cursor + GitHub MCP 二次开发 Vue-Element-Admin
- `53` LangGraph Agent 接入 GitHub MCP
- `54` 重点回顾

## 第 8 章：多轮对话 & LangChain Runnables（11 节）

- `55` 项目整体架构设计
- `56-57` 多轮对话原理 + LCLE 创建
- `58` ChatMessageHistory 存储历史上下文
- `59` RunnableWithMessageHistory 构建多轮对话链路
- `60-61` 多轮对话交互 + Agent 集成
- `62` LangChain 核心组件 Runnables 介绍
- `63-64` Runnables 核心功能演示
- `65` 重点回顾

## 第 9 章：Agent 记忆持久化（12 节）

- `66` Agent 内存记忆能力
- `67` Windows & macOS Redis 环境搭建
- `68` RedisSaver 实现 Agent 会话持久化
- `69` MongoDB 环境搭建 & 持久化实现
- `70-72` 手写 FileSaver 实现文件持久化 + Agent 多轮对话
- `73` Docker 安装 Redis（加餐）
- `74` Docker 安装 MongoDB（加餐）
- `75` mongosh 命令行工具（加餐）
- `76` 重点回顾

## 第 10 章：终端 Shell / PowerShell MCP 工具开发（10 节）

- `77-78` subprocess run / Popen 方法实战
- `79` MCP 工具封装 + Agent 集成 Shell MCP
- `80` 智能体流式输出 + 视觉优化
- `81-82` macOS 终端工具开发 + 新增工具
- `83` 向终端输入脚本命令 + 获取终端全部信息
- `84` 使用 Cursor 封装终端 MCP 工具
- `85` Windows 利用 psutil + pyautogui 开发 PowerShell 控制工具
- `86` 重点回顾
