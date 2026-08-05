# 05 - TensorRT 推理加速

> 🎯 TensorRT = NVIDIA 官方的推理加速引擎 — 图优化 + INT8 量化 + Kernel 融合，GPU 推理速度提升 3-5×

### 核心优化

```text
TensorRT 的优化技术栈：

① 层融合 (Layer Fusion)：Conv+BN+ReLU → 一个 Kernel
② 精度校准 (INT8 Calibration)：找到最优量化参数
③ Kernel 自动调优：为 GPU 架构选择最优 Kernel
④ 显存优化：复用中间结果内存
⑤ 动态形状支持：处理可变 batch_size/序列长度
```

### 使用流程

```python
# ① PyTorch → ONNX
torch.onnx.export(model, dummy_input, "model.onnx")

# ② ONNX → TensorRT Engine
trtexec --onnx=model.onnx --saveEngine=model.engine \
        --fp16 --int8 --calib=calibration.cache

# ③ Python 推理
import tensorrt as trt
runtime = trt.Runtime(trt.Logger())
with open("model.engine", "rb") as f:
    engine = runtime.deserialize_cuda_engine(f.read())
```

### 性能对比

| 引擎 | Llama-7B QPS | 延迟 P50 | 显存 |
|------|:---:|:---:|------|
| HF Transformers | 15 | 500ms | 16GB |
| vLLM | 150 | 80ms | 14GB |
| TensorRT-LLM | 200 | 50ms | 12GB |

> TensorRT-LLM 是 NVIDIA 专为 LLM 优化的版本，集成了 FlashAttention、PagedAttention 等最新技术。

---

## 6. TensorRT-LLM 的 2026 现状

**最新状态（2026-08 检索核实）**：v1.3.x（2026-06），新增 **Blackwell (B200) FP4 支持**、增量编译（构建时间缩短 40%）。

**性能数据（2026 基准，量级参考）**：

```text
FP8 模式下：吞吐可达 H100 峰值算力的 ~85%（vLLM 约 65-70%）
对比 vLLM：快 10-30%（高并发下 30-50%）
70B 级/H100：2500-4000+ tok/s（FP8）
```

**核心取舍（面试必答）**：

| 优势 | 代价 |
|------|------|
| 极致性能（NVIDIA 算力利用率最高） | **NVIDIA 独占**（AMD/其他无） |
| FP8/FP4 量化支持 | **离线编译**：换 GPU/批大小/模型结构都要重新编译（30-60 分钟） |
| 高并发吞吐 | 学习曲线陡峭、部署周期以周计 |
| | 无原生 OpenAI 兼容 API（需配 Triton） |

**2026 选型结论**："**对几乎所有人都是错误选择**"——只有"大规模 + 延迟敏感 + 有 CUDA 工程团队"才值得：

```text
选 TensorRT-LLM 的三个条件（缺一不可）：
① NVIDIA 硬件独占（H100/A100 等）
② 极致吞吐需求（QPS 极高）
③ 有 CUDA 工程能力（能承受编译与调优成本）
否则 → vLLM 已足够（"无聊但正确的答案"）
```

---

## 7. 性能对比速查（2026 参考）

| 引擎 | 70B 吞吐（H100） | 部署成本 | 适用 |
|------|:---------------:|:--------:|------|
| vLLM | 1000-2000 tok/s | 低（开箱即用） | 生产默认 |
| TensorRT-LLM | 2500-4000+ tok/s | 高（编译/独占） | 极致场景 |
| SGLang | 极高（DeepSeek 快 3.1x） | 低 | 前缀共享/结构化 |
| Ollama | 低 | 极低 | 本地验证 |

> 🎯 **核心要点**：TensorRT-LLM 的 2026 定位 = "**性能王座但日益小众化**"——**FP8/FP4 是全方位赢家**（吞吐↑延迟↓显存↓），但编译成本与 NVIDIA 独占让它只适合特定团队；生产默认仍是 vLLM。

---

## 8. TensorRT 部署流程速查

**从 PyTorch 到 TensorRT 引擎的五步**：

```text
① 模型导出：PyTorch → ONNX（torch.onnx.export，动态轴）
② TensorRT 解析：trtexec 或 Python API 加载 ONNX
③ 精度模式选择：FP32（基准）/ FP16（默认加速）/ INT8（量化）/ FP8（H100）
④ 引擎构建：离线编译（trtexec --onnx=model.onnx --saveEngine=model.engine）
   → 耗时 30-60 分钟（取决于模型与硬件）
⑤ 部署：TensorRT Runtime 加载 .engine 文件推理
```

```bash
# trtexec 命令行速查
trtexec --onnx=model.onnx \
  --saveEngine=model.engine \
  --fp16 \                    # FP16 精度
  --workspace=8192            # 工作区内存（MB）
# 输出：引擎文件（硬件绑定！换 GPU 需重新编译）
```

**INT8 量化的三个要点**：

```text
① 校准数据：需要代表性样本（几百条），量化尺度由此确定
② 精度验证：量化后跑黄金集对比（掉点 >1% 需回退 FP16）
③ 收益：显存减半 + 吞吐提升（对延迟敏感场景关键）
```

> 🎯 **核心要点**：TensorRT 部署 = "**ONNX 导出 → 离线编译（硬件绑定）→ 精度模式选择**"三步——**"换 GPU 必须重新编译"是最大的运维约束**（部署周期以周计的原因）。

---

## 9. 决策框架与总结

**"要不要用 TensorRT-LLM"的决策树**：

```text
硬件是 NVIDIA 吗？
├── 否 → 别用（独占）——vLLM（AMD/其他）
└── 是 → 吞吐要求极致吗？
    ├── 否 → vLLM（足够，开箱即用）
    └── 是 → 有 CUDA 工程团队吗？
        ├── 否 → vLLM + FP8（性能已接近）
        └── 是 → TensorRT-LLM（性能王座）
```

**TensorRT-LLM vs vLLM 的最终对比**（2026）：

| 维度 | TensorRT-LLM | vLLM |
|------|:------------:|:----:|
| 性能（FP8/H100） | 2500-4000+ tok/s | 1000-2000 tok/s |
| 部署成本 | 高（编译/独占） | 低（开箱即用） |
| 生态 | 窄（NVIDIA） | 广（硬件/模型/框架） |
| 适合 | 极致场景 | **生产默认** |

**一句话总结**：TensorRT-LLM = "**用部署复杂度换极致性能**"——**2026 年它是'性能王座但日益小众'的定位**；vLLM 是"无聊但正确的答案"。

> 🎯 **核心要点**：决策框架 = "**NVIDIA？→ 极致？→ 团队？**"三问——全"是"才选 TensorRT-LLM；否则 vLLM + FP8 已覆盖 95% 需求（2026 共识）。

---

## 10. 常见问题速查

| 问题 | 原因 | 解法 |
|------|------|------|
| 编译失败 | 算子不支持 | 换 opset/降精度模式/查支持矩阵 |
| 精度下降 | INT8 校准数据差 | 换校准集/回退 FP16 |
| 换 GPU 后引擎失效 | 硬件绑定 | 重新编译（30-60 分钟） |
| 动态形状不支持 | 静态 shape 导出 | 动态轴配置（dynamic_axes） |
| 与框架集成难 | 无 OpenAI 兼容 API | 配 Triton Inference Server |

**性能验证流程**：

```text
① 编译后基准：trtexec --loadEngine=model.engine
   → 看 latency/throughput 报告
② 黄金集精度验证：量化后对比（掉点 >1% 回退）
③ 生产压测：真实负载（并发 + 长上下文）
④ 对比基线：与 vLLM 同负载对比（确认"极致"是否真需要）
```

> 🎯 **核心要点**：TensorRT 运维 = "**编译失败/精度/硬件绑定三大坑 + 四步验证**"——**"换 GPU 重编译"是最高频运维事件**；上线前必须跑黄金集精度验证（量化掉点不可见是事故源）。

---

## 11. 学习与选型总结

**TensorRT 学习路径速查**：

```text
入门：trtexec 命令行（ONNX → engine）→ 理解编译流程
进阶：Python API（精度控制/动态形状）→ 插件开发
高级：TensorRT-LLM（LLM 专用：PagedAttention/FP8）
生产：Triton 集成（服务化/并发管理）

时间投入参考：
  基本流程：1 天
  精度与量化：2-3 天
  TensorRT-LLM 生产化：1-2 周（含编译与调优）
```

**本文件核心结论回顾**：

```text
① TensorRT-LLM = 性能王座（FP8 达 H100 算力 85%）
② 代价 = 编译成本 + NVIDIA 独占（对多数人是错误选择）
③ 决策树：NVIDIA？→ 极致？→ 团队？三问全"是"才选
④ vLLM + FP8 覆盖 95% 需求（2026 共识）
```

> 🎯 **核心要点**：TensorRT 学习 = "**trtexec 入门 → API 进阶 → TRT-LLM 生产**"三阶梯——**在投入 1-2 周前先跑决策树**（多数团队结论是 vLLM 足够）；本文件与 03-vLLM 对照阅读，选型认知最完整。
