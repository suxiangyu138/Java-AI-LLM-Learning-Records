# 自然语言处理（NLP）

## 📌 定位
**课内选修 / 课外自学的深度融合 | 第一梯队 | 大模型核心前置**

NLP是AI大模型（GPT/BERT）的起点——从分词到词向量到Transformer，理解了NLP的演进就理解了大模型的"为什么"。

## 🎯 核心章节

### 1. 文本预处理（传统NLP基础）
- **分词**：中文分词(正向最大匹配/CRF)、英文分词(空格+词形还原)
- **停用词**：高频无意义的词(的/了/is/the)——某些场景去停用词能提升信噪比
- **TF-IDF**：TF(词频)×IDF(逆文档频率)——提取文档关键词

### 2. 词向量（Word Embedding, ⭐ NLP的第一场革命）
- **One-Hot → 分布式表示**：One-Hot维度=词表大小(几十万维，稀疏)→词向量(几百维，稠密)
- **Word2Vec**：CBOW(用上下文预测中间词)+Skip-Gram(用中间词预测上下文)——无监督训练出有语义的向量
  - 经典例子：king - man + woman ≈ queen
- **GloVe**：全局词共现矩阵分解→比Word2Vec更好地利用全局统计信息

### 3. 序列模型
- **RNN/LSTM/GRU**：处理序列信息的经典架构——时间步展开、梯度消失问题
- **Seq2Seq + Attention**：Encoder(S→C)+Decoder(C→T)，Attention让Decoder能看到全部Encoder状态
- **Self-Attention → Transformer**：RNN的终结者——并行计算+长距离直接交互

### 4. 预训练语言模型（⭐ NLP第二场革命）
- **ELMo**：双向LSTM——词向量根据上下文动态变化
- **BERT**：双向Transformer Encoder——MLM(掩码语言模型)+NSP(下一句预测)——理解型任务SOTA
- **GPT系列**：单向Transformer Decoder——自回归语言模型——生成型任务的王者
  - GPT-1→GPT-2→GPT-3(涌现能力)→ChatGPT(RLHF)→GPT-4

### 5. 大模型核心能力（课外重点）
- **涌现能力（Emergent Abilities）**：模型大到一定规模后突然获得的能力(In-Context Learning、CoT推理)
- **In-Context Learning（上下文学习）**：不给梯度更新，通过Prompt中的示例就能学会
- **Chain-of-Thought（思维链）**：Let's think step by step——复杂推理能力

### 6. NLP 经典下游任务
- **文本分类**：情感分析(正面/负面/中性)、垃圾邮件检测
- **命名实体识别(NER)**：识别人名/地名/组织机构/时间
- **机器翻译**：Neural Machine Translation——Transformer的最佳应用之一
- **问答系统**：抽取式(从文本找答案)→生成式(LLM直接生成答案)→RAG(检索增强)

## ✅ 学习建议
- 先理解Word2Vec的词向量空间(为什么king-man+woman=queen)
- Transformer是理解一切的钥匙——花时间彻底搞懂Self-Attention的计算过程
- Hugging Face Transformers库是最佳实践工具——fine-tune一个BERT分类任务
