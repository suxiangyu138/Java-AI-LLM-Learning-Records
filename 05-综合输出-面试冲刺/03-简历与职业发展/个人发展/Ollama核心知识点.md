# Ollama 核心知识点

## 一、基础定义
Ollama 是一款开源、轻量、跨平台的本地大模型运行工具，核心作用是：一键下载、部署、运行、管理各类大语言模型，无需复杂环境配置，让普通电脑也能本地跑大模型。
核心定位
- 专注离线本地部署，数据不出本机，隐私性极强
- 支持 Windows / macOS / Linux / Docker 全平台
- 内置模型库，一键拉取模型，开箱即用
- 提供命令行 + API 接口，方便二次开发与集成

## 二、核心工作原理
1. 底层依赖
    基于 GGUF 格式 模型文件（高效轻量化模型格式，适配CPU/GPU推理），底层依赖 llama.cpp 实现高性能推理。
2. 运行逻辑
    - 下载模型：从 Ollama 官方库拉取 GGUF 格式模型
    - 本地加载：将模型加载到内存/显存
    - 推理交互：通过命令行或 API 接收请求，本地完成生成，无网络传输
3. 核心优势
    - 低门槛：无需编译、无需CUDA复杂配置，一键安装
    - 高性能：llama.cpp 底层优化，CPU也能流畅运行小参数量模型
    - 可定制：支持自定义模型、修改参数、创建Modelfile

## 三、核心常用命令
1. 模型基础操作
```bash
# 拉取模型
ollama pull <model-name>
# 运行/对话模型
ollama run <model-name>
# 查看本地已装模型
ollama list
# 删除模型
ollama rm <model-name>
# 查看模型信息
ollama show <model-name>
```
 
2. 服务与状态管理
```bash
# 启动后台服务
ollama serve
# 查看运行中模型
ollama ps
# 停止运行模型
ollama stop <model-name>
```
 
3. 模型定制
```bash
# 基于Modelfile创建自定义模型
ollama create <custom-name> -f Modelfile
```
 
## 四、核心模型生态
Ollama 官方模型库支持几乎所有主流开源大模型，高频使用分类：
1. 通用对话：Llama3、Qwen、Mistral、Gemma
2. 代码专用：CodeLlama、DeepSeek-Coder
3. 中文优化：通义千问、Qwen、Llama3-Chinese
4. 轻量化小模型：phi3、tinyllama（低配电脑首选）
5. 多模态：LLaVA（图文理解）

## 五、API 接口（开发核心）
Ollama 默认本地开启 HTTP 服务，地址： http://localhost:11434 ，支持 RESTful 接口调用，可无缝对接 Python/Java/前端等项目。
核心接口示例
```bash
# 生成对话
POST http://localhost:11434/api/generate
{
  "model": "llama3",
  "prompt": "你好",
  "stream": false
}
```
 
- 支持流式输出、参数配置、上下文对话
- 可直接用于 RAG、Agent、AI 应用开发

## 六、核心配置与优化
1. 跨设备访问
    修改环境变量，开启局域网访问：
```bash
OLLAMA_HOST=0.0.0.0
```
 
2. 显存/内存优化
    - 自动检测硬件，优先使用GPU加速
    - 支持设置  num_gpu 、 num_thread  控制硬件占用
3. 模型量化
    自动适配 4bit / 8bit / FP16 量化版本，低配电脑优先选量化模型

## 七、Modelfile 自定义模型
通过 Modelfile 可自定义模型提示词、参数、上下文，实现专属模型封装，示例：
```modelfile
FROM llama3
SYSTEM "你是一名Java后端开发工程师，回答简洁专业"
PARAMETER temperature 0.7
```
 
## 八、核心优缺点
优点
- 零门槛本地部署，隐私安全
- 跨平台兼容，低配电脑可用
- 命令行+API双模式，开发友好
- 模型生态丰富，更新迭代快
    缺点
- 大参数量模型（70B+）对硬件要求高
- 仅支持开源模型，无法运行闭源模型
- 无可视化界面，需搭配第三方WebUI使用

## 九、一句话总结
Ollama 是本地大模型的「轻量化运行引擎」，以 GGUF 格式和 llama.cpp 为底层，实现一键部署、本地推理、API 调用，是个人AI学习、隐私化AI应用开发的核心工具。
