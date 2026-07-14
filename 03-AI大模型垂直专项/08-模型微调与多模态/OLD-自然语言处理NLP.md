# 自然语言处理（NLP）

> **优先级**：🔴 高优先级（必选）| **类型**：校内提升专业课 | **方向**：AI 能力拔高

## 课程定位

**提升点**：文本预处理与分词、词向量与语义表示、序列模型(RNN/LSTM/Transformer)、预训练语言模型(BERT/GPT)、文本生成与理解、大模型核心前置

**适用人群**：大模型方向必选——做对话机器人/RAG/文本类 LLM 应用的核心专业课

**学习目标**：理解从 N-gram 到 Transformer 的完整技术演进，能手写 Self-Attention 计算过程，能用 HuggingFace 做模型微调

---

# 第一章：NLP 基础与文本预处理
- NLP 特殊性与挑战：歧义性（I saw a man with a telescope）、多义性、上下文依赖
- 中文分词：基于规则(正向最大匹配)→ 基于统计(HMM/CRF)→ 基于深度学习(BiLSTM+CRF)
- 英文预处理：Tokenization、词形还原(Lemmatization)、词干提取(Stemming)
- 文本规范化：大小写、标点、停用词、纠错
- N-gram 语言模型：P(w_n|w_{n-1}...w_1)——基于语料频次估计，平滑(Katz/Good-Turing/Kneser-Ney)

# 第二章：词向量与语义表示
- 离散表示：One-Hot → Bag-of-Words → TF-IDF（TF×log(N/DF)）——无序、稀疏
- 分布式表示（词嵌入）：
  - Word2Vec：CBOW(上下文→中心词) + Skip-Gram(中心词→上下文) + 负采样
  - GloVe：全局词共现矩阵分解——融合了统计和学习
  - FastText：子词(Subword)建模——OOV 词拼出来
- 词向量的性质：语义类比（king-man+woman≈queen）、语义相似度（cosine similarity）
- ELMo：基于双向 LSTM 的上下文词向量——同一词在不同句子中向量不同（NLP 第一次革命）

# 第三章：序列模型与注意力机制
- RNN：共享参数 + 时间步展开——BPTT（时间反向传播），梯度消失/爆炸
- LSTM：遗忘门+输入门(选记忆)+输出门(选输出)，CT 细胞状态长距离传递
- GRU：重置门+更新门——比 LSTM 少一个门
- Seq2Seq：Encoder(RNN编码→C)+Decoder(C+上一输出→当前输出)——机器翻译开创性架构
- 注意力机制（Attention）：Decoder 能看到所有 Encoder 状态——加权平均——解决信息瓶颈
- 自注意力（Self-Attention）：Q/K/V 都来自同一序列——Seq 内部元素间两两交互
  - 计算：Attention(Q,K,V)=softmax(QKᵀ/√d_k)·V（√d_k 防止点积过大导致 softmax 梯度消失）

# 第四章：Transformer 及其变体
- Transformer 架构：Encoder(自注意力+FFN+残差+LayerNorm)×N
- 多头注意力：h组Q/K/V平行计算→Concat→线性投影——不同头关注不同信息
- 位置编码：正弦/余弦编码（原始）→ 可学习位置编码（BERT）→ 旋转位置编码 RoPE（LLaMA/Qwen）
- 前馈网络（FFN）：两层全连接 + ReLU/GELU——SwiGLU（LLaMA 2）
- 残差连接 + LayerNorm：Pre-LN(先归一化再注意力) vs Post-LN——Pre-LN 训练更稳定

# 第五章：预训练语言模型
- BERT：双向 Transformer Encoder——MLM(掩码语言模型)+NSP(下一句预测)——理解型任务
- GPT系列：单向 Transformer Decoder——自回归语言模型(单向→只看前面的Token)
  - GPT-1(117M)→GPT-2(1.5B)→GPT-3(175B,涌现)→ChatGPT(RLHF)→GPT-4(多模态)
- T5/ BART：Encoder-Decoder 架构——序列到序列的通用文本模型
- LLaMA/Qwen/ChatGLM：开源大模型——推动国产化和社区生态

# 第六章：NLP 核心任务
- 文本分类：情感分析、主题分类、垃圾邮件——BERT fine-tune
- 命名实体识别（NER）：序列标注——BIO 标注 + BiLSTM-CRF/BERT+CRF
- 关系抽取：三元组(S,P,O)——远程监督 + Sentence-Level / Bag-Level
- 机器翻译：Transformer 的原生任务——Encoder→Decoder 自回归生成
- 文本摘要：抽取式(选原文句子) vs 生成式(新写摘要)
- 问答系统：抽取式(BERT→SQuAD，从文中找答案跨度)→ 生成式(LLM)→ RAG(检索+生成)

# 第七章：NLP 前沿与落地
- 提示学习（Prompt Learning）：设计模板让模型"填空"而非 fine-tune
- In-Context Learning (ICL)：不更新梯度，通过示例 Prompt 让大模型学会新任务
- Chain-of-Thought（CoT）：Let's think step by step——大模型推理能力的关键
- PEFT（参数高效微调）：LoRA(低秩矩阵)+Q-LoRA(量化版本)——显存需求骤降

---

## 推荐教材
- 《Speech and Language Processing》—— Jurafsky & Martin（NLP 圣经，在线免费）
- 《自然语言处理：基于预训练模型的方法》——车万翔等

## 实验建议
- HuggingFace Transformers fine-tune BERT 做文本分类
- 手写一个 Word2Vec (Skip-Gram + 负采样) 在《三体》语料上训练
- 基于 LangChain 写一个 RAG 问答系统——知识库+检索+大模型生成
