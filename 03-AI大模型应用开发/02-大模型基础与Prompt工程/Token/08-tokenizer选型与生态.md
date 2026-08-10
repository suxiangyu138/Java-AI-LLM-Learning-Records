# 08 - Tokenizer 选型与生态

> 定位：各家 tokenizer 的地图与选型纪律——"tokenizer 跟着模型走、版本跟着仓库走、计数工具跟着 tokenizer 走——三条铁律记牢，选型就不会错"

---

## 📚 目录

1. [各家 tokenizer 地图](#1-各家-tokenizer-地图)
2. [选型第一铁律：tokenizer 跟着模型走](#2-选型第一铁律tokenizer-跟着模型走)
3. [计数工具的配套选型](#3-计数工具的配套选型)
4. [版本纪律：tokenizer 也是版本](#4-版本纪律tokenizer-也是版本)
5. [跨模型计数的不可比性](#5-跨模型计数的不可比性)
6. [生态工具链](#6-生态工具链)
7. [五个常见坑](#7-五个常见坑)
8. [练习 5 题](#8-练习-5-题)

---

## 1. 各家 tokenizer 地图

| 模型家族 | tokenizer | 词表规模 | 特点 |
|---------|-----------|---------|------|
| OpenAI GPT-3.5/4 | cl100k_base（tiktoken） | ~100K | 老编码，存量模型 |
| OpenAI GPT-4o/GPT-5 系 | o200k_base（tiktoken） | ~200K | 2026 主力，多语言/工具覆盖好 |
| LLaMA-2 | LlamaTokenizer（SentencePiece） | 32K | 经典小词表 |
| LLaMA-3 | Llama3Tokenizer（HF） | 128K | 多语言 + 代码大幅提升 |
| Qwen2/2.5 | Qwen2Tokenizer（HF） | 151K | 中文覆盖最优梯队 |
| Mistral/Gemma | SentencePiece/HF 系 | 32K-256K | 各代演进 |
| Claude（Anthropic） | 闭源（随模型发布） | 未知 | 4.7 换新 tokenizer（05 篇案例） |
| DeepSeek | HF 系（随模型发布） | ~128K | 中文友好 + 便宜 |

**地图的读法**：其一，**2026 新模型词表普遍 100K-200K**（多语言刚需，03 篇第 5 节）；其二，**开源模型 tokenizer 随模型仓库发布**（HF 体系加载链），**闭源模型（Claude）只给计数 API 不给词表文件**——"**闭源模型的 token 数只能以服务端为准**"；其三，**中文场景 Qwen/DeepSeek 词表覆盖好**（同文本 token 更少），**中文应用选型时 tokenizer 是隐形成本项**（05 篇第 4 节）。

## 2. 选型第一铁律：tokenizer 跟着模型走

**tokenizer 与模型权重是同一个训练产物，不可拆分**——**"模型的 tokenizer 就是它的语言，混用等于用错语言跟它说话"**。三个层面的"跟着走"：

**模型层**：选模型 = 选 tokenizer——换模型必须换计数口径（05 篇 tokenizer 换版案例）；**加载层**：开源模型用 `AutoTokenizer.from_pretrained(模型名)`——**tokenizer 文件从模型仓库自动拉取**，与权重同版本（04 篇）；**计数层**：OpenAI 系用 `encoding_for_model`（官方映射表），**新模型未收录时用 `get_encoding("o200k_base")` 兜底**并关注官方更新。

**反例与后果**：拿 cl100k 数 gpt-4o 的文本——**数字偏差 10-30%**（词表不同）；拿 LLaMA-2 的 tokenizer 加载 LLaMA-3 模型——**编码错位，模型输出乱码**。**"tokenizer 选型"这个命题本身是伪命题——真正的问题是"模型选型"，tokenizer 只是跟随变量**（06 篇定价表与 00 篇 600 倍跨度交叉）。

## 3. 计数工具的配套选型

**工具跟随 tokenizer，不跟随"习惯"**：**OpenAI 系 → tiktoken**（官方、Rust 快、版本锁死）；**开源系（Llama/Qwen/DeepSeek）→ HF tokenizers**（AutoTokenizer 底层）；**受限环境 → puretiktoken**（零依赖逐字节兼容，04 篇第 6 节）；**多供应商网关 → 按模型路由计数器**（每个模型一套，网关统一封装）。

**一个应用同时用多家模型的正确姿势**：**封装一个 `count_tokens(model, text)` 函数**——内部按模型名路由到对应 tokenizer——"**业务代码只问'多少 token'，不问'哪个 tokenizer'**"（10 篇实战的中间件雏形）。**常见错误**：全应用一把 tiktoken 数所有模型——**数字对一半错一半，预算看板失真**（06 篇第 5 节）。**网关的注册纪律**：新模型上线前跑一次"样本语料 token 数回归"（对比旧模型/旧版本），**数字异常变化就是注册表配错的第一信号**——"新模型进网关先过回归，再进生产"。

## 4. 版本纪律：tokenizer 也是版本

**tokenizer 是模型的"隐藏版本号"**，三个纪律：

**其一，与模型同版本**——开源模型仓库的 tokenizer 文件与权重同 tag 发布；**加载时用与模型一致的 commit/tag**（HF 的 `revision` 参数）；**其二，tiktoken 锁版本**——0.13.0 锁死，升级前先跑"同一批语料的 token 数对比"（±12% 漂移的教训，04 篇）；**其三，tokenizer 变更 = 发布事件**——**模型升级公告里看 tokenizer 变更说明**（Claude Opus 4.7 案例：同文本 +35% token）——**"tokenizer 换版是成本与效果的双重变量"**（05 篇第 6 节）。**落地**：CI 里放"tokenizer 版本指纹"检查（tokenizer 文件哈希 + tiktoken 版本号），**换了就告警**——"**把 tokenizer 当依赖管，不当配置看**"。

**自建模型场景**：自己训练 tokenizer 时，词表规模与语料配比按目标语言决定（03 篇第 5 节）——**训练完立即冻结并入版本管理**（"自己训的 tokenizer 更要当版本管"）；**红线**：换 tokenizer 不重训 embedding = 灾难（ID 错位，03 篇坑一）——"**tokenizer 与 embedding 是同一个锁链的两环**"。

## 5. 跨模型计数的不可比性

**同一文本在两个模型上的 token 数不同，且没有"换算系数"**——词表不同、合并规则不同、语言覆盖不同，**token 数是"每模型私有度量"**。三个不可比的场景：**场景一**，比"我的 prompt 多少 token"——换个模型数字就变；**场景二**，比"哪个模型便宜"——**必须回到同一条文本、同一个任务，各自数 token 再乘单价**（06 篇公式——"比成本不能只比单价表，要比单次任务成本"）；**场景三**，比压缩率——用 **TPC/BPB**（05 篇三把尺）。**"跨模型比 token 数，就像跨货币比数字——不换汇直接比是错的"**；**换汇公式 = 各自 token 数 × 各自单价，回到同一任务实例**。

## 6. 生态工具链

**tokenizer 生态四件套**：**tiktoken**（OpenAI 计数标准，04 篇）；**HF tokenizers**（Rust 核心的 tokenizer 库：训练/编码/解码 API，AutoTokenizer 的底层引擎——HuggingFace 体系交叉）；**SentencePiece**（Google 的纯算法库：BPE/Unigram/WordPiece 三算法都支持，LLaMA-2/Mistral 曾用——"算法库 vs 模型配套库"的分工：SentencePiece 是通用工具，HF tokenizers 是生态配套）；**puretiktoken/turbobpe**（2026 新工具：受限环境替代与预编译 wheel，04 篇）。**2026 的收敛趋势**：**HF tokenizers 成为开源事实标准**（AutoTokenizer 全家桶），tiktoken 守住 OpenAI 一亩三分地——"**工具在收敛，纪律在加强：跟着模型走 + 锁版本 + 按模型路由**"。

## 7. 五个常见坑

- **坑一**：全应用一把 tiktoken 数所有模型——**数字一半是错的；按模型路由计数**（第 3 节）；
- **坑二**：拿 LLaMA-2 的 tokenizer 加载 LLaMA-3——**编码错位输出乱码；AutoTokenizer 自动拉对应版本**；
- **坑三**：比成本只比单价表——**不看各自 token 数；回到同一任务实例对比**（第 5 节）；
- **坑四**：模型升级不看 tokenizer 变更——**成本与效果双漂移；升级前跑 token 对比样本**（第 4 节）；
- **坑五**：以为 SentencePiece 与 HF tokenizers 二选一——**它们是"算法库"与"生态配套库"的分工**，按模型决定。

## 8. 练习 5 题

1. 画出 2026 各家 tokenizer 地图（家族/编码/规模）？
2. "tokenizer 跟着模型走"的三个层面？反例的后果？
3. 多供应商网关的计数正确姿势？"业务代码只问多少 token"怎么实现？
4. 版本纪律的三条？"tokenizer 是隐藏版本号"指什么？
5. 跨模型计数为什么不可比？"换汇公式"是什么？

> 🎯 **核心要点**：选型 = **三条铁律**——"tokenizer 跟着模型走、版本跟着仓库走、计数工具跟着 tokenizer 走"；**跨模型比 token 数要"换汇"（各自 token × 各自单价，回到同一任务）**；"tokenizer 是模型的隐藏版本号，升级必查、换版必测"。

---

**下一模块**：[09-token与性能优化.md](09-token与性能优化.md) / **返回总览**：[00-Token总览.md](00-Token总览.md)
