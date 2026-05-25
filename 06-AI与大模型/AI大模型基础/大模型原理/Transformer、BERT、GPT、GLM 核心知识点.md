Transformer、BERT、GPT、GLM 核心知识点
一、Transformer（核心架构）
Transformer 是 2017 年由 Google 团队在《Attention Is All You Need》中提出的深度学习架构，彻底摒弃了传统 RNN、LSTM 的序列依赖（逐词处理），采用“自注意力机制”作为核心，实现了并行计算，大幅提升了模型训练效率和效果，是后续 BERT、GPT、GLM 等大语言模型（LLM）的基础架构。
核心知识点
核心机制：自注意力机制（Self-Attention）：能够让模型在处理每个token（词/子词）时，同时关注输入序列中所有其他token的关联程度，计算每个token与其他token的注意力权重，从而捕捉全局上下文信息（区别于RNN的局部上下文）。
整体结构：编码器（Encoder）+ 解码器（Decoder）
编码器（6层堆叠）：由“多头注意力（Multi-Head Attention）”和“前馈神经网络（Feed-Forward Network）”组成，负责对输入序列进行特征提取，输出全局上下文表征；
解码器（6层堆叠）：在编码器结构基础上，增加了“掩码多头注意力（Masked Multi-Head Attention）”，用于防止预测时提前看到后续token（适用于生成任务）。
关键细节：包含位置编码（Positional Encoding），用于补充序列的位置信息（因为自注意力本身不具备顺序感知能力）；采用残差连接（Residual Connection）和层归一化（Layer Normalization），缓解梯度消失问题。
核心优势：并行计算（无需逐词处理）、捕捉长距离依赖、特征提取能力强；核心局限：计算复杂度高（与序列长度的平方成正比）。
二、BERT（双向预训练模型）
BERT（Bidirectional Encoder Representations from Transformers）是 2018 年由 Google 提出的预训练语言模型，基于 Transformer 的编码器构建，核心特点是“双向注意力”，打破了传统模型（如ELMo）单向上下文的局限，成为后续很多微调任务（分类、问答等）的基础。
核心知识点
核心架构：仅使用 Transformer 的编码器（无解码器），分为基础版（BERT-base：12层编码器、12头注意力、768维隐藏层）和大型版（BERT-large：24层编码器、16头注意力、1024维隐藏层）。
预训练任务（核心创新）
掩码语言模型（MLM，Masked Language Model）：随机掩盖输入序列中15%的token，让模型预测被掩盖的token，迫使模型学习双向上下文关联（区别于单向的语言模型）；
下一句预测（NSP，Next Sentence Prediction）：输入两个句子，让模型判断第二个句子是否是第一个句子的下一句，用于学习句子级别的语义关联。
核心特点：双向上下文感知、预训练+微调（Pre-train + Fine-tune）范式，适配自然语言理解（NLU）任务（如情感分析、文本分类、问答、命名实体识别NER）。
局限：仅基于编码器，不具备生成能力；MLM任务存在“掩码偏差”（训练时用[MASK]，推理时无[MASK]，导致分布不一致）。
三、GPT（生成式预训练模型）
GPT（Generative Pre-trained Transformer）是 2018 年由 OpenAI 提出的生成式语言模型，基于 Transformer 的解码器构建，核心特点是“单向自回归”，专注于自然语言生成（NLG）任务，后续迭代出 GPT-2、GPT-3、GPT-4 等版本，逐步提升模型规模和能力。
核心知识点
核心架构：仅使用 Transformer 的解码器（无编码器），采用“ decoder-only ”结构，通过掩码注意力（Masked Attention）确保生成时只能看到当前及之前的token，无法看到后续token，符合自回归生成逻辑。
预训练任务：单向语言模型（Causal Language Model, CLM）：给定前序token，预测下一个token的概率，本质是“自回归生成”，让模型学习语言的序列规律和上下文关联。
核心特点：自回归生成、擅长长文本生成（如文章、对话、代码）；后续版本（GPT-3及以后）通过“Few-Shot/Zero-Shot”能力，无需大量微调即可适配多种任务（打破预训练+微调范式）。
与 BERT 的核心区别：BERT 是双向编码器（NLU 擅长），GPT 是单向解码器（NLG 擅长）；BERT 依赖微调，GPT 擅长零样本/少样本生成。
局限：单向上下文，无法充分利用后续信息；自回归生成速度慢（逐词生成）；易产生重复文本。
四、GLM（通用语言模型）
GLM（General Language Model）是 2022 年由清华大学团队提出的通用语言模型，核心是“基于 Transformer 编码器的双向自回归”，融合了 BERT（双向上下文）和 GPT（自回归生成）的优势，实现了“理解+生成”双任务适配，是中文场景下表现优异的开源大模型（如 GLM-4、ChatGLM 系列）。
核心知识点
核心架构：基于 Transformer 编码器，创新引入“自回归填充（Autoregressive Blank Infilling）”机制，本质是“双向上下文+单向生成”的结合，既保留了 BERT 的双向语义理解能力，又具备 GPT 的生成能力。
预训练任务（核心创新）：自回归填充任务：将输入序列中的连续token替换为一个“空白标记”，让模型自回归地生成空白部分的内容，既学习了双向上下文（理解空白前后的信息），又训练了自回归生成能力（生成空白内容）。
核心特点：统一理解与生成任务（无需分别适配 NLU 和 NLG）；中文优化（针对中文语义、分词、文化场景优化）；开源可定制（相较于 GPT 闭源，GLM 系列开源，可本地化部署）。
与 BERT、GPT 的区别：
与 BERT：GLM 具备生成能力，解决了 BERT 无法生成的问题；
与 GPT：GLM 采用双向上下文，解决了 GPT 单向上下文的局限，生成质量和语义连贯性更优（尤其中文场景）。
代表模型：ChatGLM-6B（轻量开源，适配中文对话）、GLM-4（性能接近 GPT-4，支持多模态）。
五、四大模型核心对比总结
模型/架构
核心结构
核心机制
核心优势
核心应用场景
Transformer
编码器+解码器
自注意力、多头注意力
并行计算、捕捉长距离依赖
所有大语言模型的基础架构
BERT
仅编码器
双向注意力、MLM/NSP
语义理解能力强
文本分类、问答、NER（NLU任务）
GPT
仅解码器
单向自回归、CLM
长文本生成能力强
文章生成、对话、代码生成（NLG任务）
GLM
仅编码器（改进）
双向自回归、空白填充
理解+生成双适配、中文优化
中文对话、多任务适配（NLU+NLG）
