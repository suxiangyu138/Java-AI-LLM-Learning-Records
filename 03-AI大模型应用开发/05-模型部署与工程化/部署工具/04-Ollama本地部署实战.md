# 04 - Ollama 本地部署实战

> 🎯 Ollama = 个人开发者的 LLM 部署神器 — 一行命令启动模型、OpenAI 兼容 API、支持 GPU/Metal 加速

### 核心命令

```bash
ollama pull qwen2:7b        # 拉取模型
ollama run qwen2:7b          # 交互运行
ollama serve                  # 启动 API 服务 (端口 11434)
ollama list                   # 已下载模型
ollama rm qwen2:7b           # 删除模型
```

### Modelfile 自定义

```dockerfile
FROM qwen2:7b
PARAMETER temperature 0.7
PARAMETER num_ctx 8192
SYSTEM "你是 Java 后端专家，擅长 Spring Boot 和微服务。"
```

```bash
ollama create java-expert -f Modelfile
ollama run java-expert
```

### API 调用

```python
import requests, json
resp = requests.post("http://localhost:11434/api/generate",
    json={"model": "qwen2:7b", "prompt": "解释 Java 多态", "stream": False})
print(resp.json()["response"])
```

### Java 端调用

```java
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:11434/api/generate"))
    .POST(BodyPublishers.ofString("""
        {"model":"qwen2:7b","prompt":"解释Java多态","stream":false}
    """))
    .build();
```

### 硬件需求

| 模型 | 量化 | 显存 | 推荐设备 |
|------|:---:|------|------|
| Qwen2-7B | Q4 | ~5GB | RTX 3060+ |
| Qwen2-7B | FP16 | ~15GB | RTX 4080+ |
| Qwen2-72B | Q4 | ~40GB | A100 / 双4090 |
| Qwen2-1.5B | Q4 | ~1.5GB | CPU |

> 详见 [Embedding/05-本地Embedding部署](../../01-大模型基础与Prompt工程/Embedding/05-本地Embedding部署-Ollama与BGE-M3.md)

---

## 6. Ollama 的 2026 定位与边界

**Ollama 最新状态（2026-08 检索核实）**：v0.24（2026-05），新增 `ollama compose`（多模型管道）与 `ollama bench`（基准工具）——安装体验业界最佳，但**定位明确：本地开发/个人使用**。

**为什么 Ollama 不适合生产**（面试必答）：

| 维度 | Ollama | vLLM |
|------|:------:|:----:|
| 连续批处理 | ❌ 无 | ✅（吞吐 2-4x） |
| 张量并行（多卡） | ❌ 不支持 | ✅ |
| 并发能力 | 低（单用户级） | 高（生产级） |
| 定位 | 本地验证 | **生产服务** |

**演进路径（2026 共识）**：**Ollama 验证 → vLLM 上生产**——先用 Ollama 快速确认模型效果，生产再迁 vLLM（同模型同量化，切换成本低）。

**DGX Spark 实测参考**（2026 社区）：单流场景 Ollama 反而领先（Gemma-4-26B 达 64 tok/s vs vLLM 30 tok/s），且能加载 vLLM 放不下的超大模型（120B+ 量化）——**"本地单用户用 Ollama 更快"是常见认知**。

---

## 7. Ollama API 速查

```bash
# 常用命令
ollama pull qwen2.5:7b        # 拉取模型
ollama run qwen2.5:7b         # 交互式运行
ollama list                   # 已安装模型
ollama ps                     # 运行中的模型（显存占用）
ollama stop qwen2.5:7b        # 停止模型（释放显存）
ollama bench qwen2.5:7b       # 基准测试（v0.6+）
ollama compose up             # 多模型管道（v0.6+）
```

```bash
# API 速查（OpenAI 兼容）
curl http://localhost:11434/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"model": "qwen2.5:7b", "messages": [{"role": "user", "content": "你好"}]}'

# 流式输出（stream: true）—— SSE 格式
```

> 🎯 **核心要点**：Ollama = "**本地模型管理器**"——`ollama pull/run/list/ps` 四命令 + OpenAI 兼容 API 是其全部心智模型；**生产边界明确（无批处理/无并行），验证后请迁移 vLLM**。

---

## 8. 模型管理速查

**多模型与显存管理**（本地部署核心操作）：

```bash
# 多模型并行（按需加载）
ollama run qwen2.5:7b          # 加载模型 A
ollama ps                      # 查看：模型 A 占用显存 X GB
ollama run llama3.1:8b         # 加载模型 B（若显存不足会自动卸载 A）

# 显存控制
OLLAMA_NUM_PARALLEL=4          # 并行请求数（默认 1-4，按显存调）
OLLAMA_MAX_LOADED_MODELS=2     # 最多同时驻留模型数
OLLAMA_KEEP_ALIVE=30m          # 模型驻留时间（默认 5 分钟）
```

**Modelfile 定制速查**：

```dockerfile
# Modelfile：从基础模型定制
FROM qwen2.5:7b
SYSTEM "你是中文客服助手，回答要简洁专业"
PARAMETER temperature 0.3          # 采样温度
PARAMETER num_ctx 8192             # 上下文长度（默认 2048，注意调大）
PARAMETER stop "END"               # 停止符

# 构建与使用
ollama create my-assistant -f Modelfile
ollama run my-assistant
```

**Java 调用速查**：

```java
// Spring AI 集成（Ollama 作为本地模型源）
// application.yml：
//   spring.ai.ollama.base-url: http://localhost:11434
//   spring.ai.ollama.chat.model: qwen2.5:7b

// 或 OpenAI 兼容 API（RestClient 直连）
POST http://localhost:11434/v1/chat/completions
{"model": "qwen2.5:7b", "messages": [...]}
```

> 🎯 **核心要点**：Ollama 的运维心智 = "**ollama ps 看显存 + KEEP_ALIVE 控驻留 + num_ctx 调上下文**"——本地部署三大件；Java 接入走 Spring AI 或 OpenAI 兼容 API。

---

## 9. Ollama 实战总结与常见问题

**Ollama 实战问题速查**：

| 问题 | 原因 | 解法 |
|------|------|------|
| 模型加载慢 | 权重读取 + 图加载 | 首次慢正常；`OLLAMA_KEEP_ALIVE` 保持驻留 |
| 显存不足 | 模型太大 | 换小模型/低量化（Q4 省 4 倍）或调 `OLLAMA_MAX_LOADED_MODELS=1` |
| 中文回答差 | 基础模型未针对中文 | 用中文模型（Qwen 系）或 Modelfile SYSTEM 强化 |
| 上下文太短 | 默认 num_ctx=2048 | Modelfile 或 API 参数 `num_ctx: 8192` |
| 并发请求卡 | 默认单并发 | `OLLAMA_NUM_PARALLEL=4`（按显存） |
| API 连不上 | 服务未启动/端口 | `ollama serve` 前台运行；默认 11434 |

**Ollama 全链路速查**（从安装到调用）：

```bash
# 安装（Linux/macOS/Windows）
curl -fsSL https://ollama.com/install.sh | sh

# 完整链路
ollama pull qwen2.5:7b        # ① 拉模型
ollama run qwen2.5:7b         # ② 交互验证
ollama show qwen2.5:7b        # ③ 查看模型信息（参数/量化）
curl localhost:11434/v1/chat/completions ...   # ④ API 调用
```

> 🎯 **核心要点**：Ollama 实战 = "**pull/run/show 三命令 + 六大问题速查**"——**中文用 Qwen 系、上下文调 num_ctx、并发调 NUM_PARALLEL** 是本地部署三大实践。

---

## 10. 与 vLLM 的迁移对照速查

**从 Ollama 迁移到 vLLM 的对照表**（生产化迁移）：

| 环节 | Ollama | vLLM |
|------|:------:|:----:|
| 模型格式 | GGUF（量化） | HF 格式（或 AWQ/GPTQ） |
| 启动 | ollama run | vllm serve |
| API | OpenAI 兼容 | OpenAI 兼容（一致！） |
| 批处理 | 无 | 连续批处理 |
| 多卡 | 无 | tensor-parallel-size |
| 上下文 | num_ctx 手动 | max-model-len |

**迁移注意**：

```text
① 模型格式不同：GGUF → HF（需重新下载/转换）
② 量化不同：Q4 GGUF → FP8/FP16（效果与显存需重新评估）
③ API 兼容：OpenAI 兼容 → 应用层零改动（最大优势）
④ 验证：迁移后黄金集对比（质量一致性确认）

结论：API 层兼容让迁移成本集中在"模型格式与量化"，应用代码基本不动。
```

> 🎯 **核心要点**：Ollama → vLLM 迁移 = "**模型格式（GGUF→HF）+ 量化重评估**"两项工作——**OpenAI 兼容 API 让应用层零改动**，这是两条路共用同一生态的最大红利。
