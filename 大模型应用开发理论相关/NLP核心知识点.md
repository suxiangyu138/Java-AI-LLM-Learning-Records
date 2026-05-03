NLP 的核心，可以按「任务」「流程」「关键模型」这三块来记，比按算法罗列更利于你之后做大模型 / RAG 项目。 [blog.csdn](https://blog.csdn.net/weixin_44986037/article/details/130259034)

下面我按你 CS 本科+Java/LLM 方向的视角，给一份结构化“知识框架图”。

***

## 1. NLP 是什么 & 两大核心任务

- NLP（Natural Language Processing）研究如何让计算机**理解**和**生成**人类语言，是 CS × AI × 语言学的交叉领域。 [sap](https://www.sap.cn/resources/what-is-natural-language-processing)
- 几乎所有技术都可归到两大任务：自然语言理解（NLU）和自然语言生成（NLG）。 [easyai](https://easyai.tech/ai-definition/nlp/)

**两大任务：**

- 自然语言理解 NLU：  
  - 文本分类（垃圾短信、情感分类）  
  - 情感分析（好评 / 差评、中立）  
  - 命名实体识别 NER（人名、地名、机构名等）  
  - 语法分析、依存分析、指代消解、意图识别等。 [cnblogs](https://www.cnblogs.com/auguse/articles/19111241)
- 自然语言生成 NLG：  
  - 机器翻译  
  - 文本摘要、写作辅助  
  - 对话生成、报告生成等。 [cloud.tencent](https://cloud.tencent.com/developer/article/2023321)

***

## 2. 核心处理流程（从原始文本到下游任务）

可以记成一条管线：**文本 → 预处理 → 表示（向量）→ 模型 → 任务输出**。 [blog.csdn](https://blog.csdn.net/weixin_44986037/article/details/130259034)

### 2.1 文本预处理

- 分词 / Tokenization：英文按空格和符号，中文要用分词器（结巴、HanLP 等）。 [easyai](https://easyai.tech/ai-definition/nlp/)
- 规范化：小写化、去噪、去停用词、表情处理等。 [blog.csdn](https://blog.csdn.net/weixin_44986037/article/details/130259034)
- 词形处理：  
  - Stemming：词干提取，如 running→run。  
  - Lemmatization：词形还原，结合词性还原标准词形。 [easyai](https://easyai.tech/ai-definition/nlp/)

### 2.2 特征表示（从词到向量）

- 传统表示：  
  - One-hot、Bag-of-Words（BOW）、TF-IDF 向量，用于传统 ML 分类等。 [cloud.tencent](https://cloud.tencent.com/developer/article/2023321)
- 分布式词向量：  
  - Word2Vec、GloVe，解决 one-hot 稀疏且不含语义的问题。 [learn.lianglianglee](https://learn.lianglianglee.com/%E4%B8%93%E6%A0%8F/PyTorch%E6%B7%B1%E5%BA%A6%E5%AD%A6%E4%B9%A0%E5%AE%9E%E6%88%98/21%20NLP%E5%9F%BA%E7%A1%80%EF%BC%88%E4%B8%8A%EF%BC%89%EF%BC%9A%E8%AF%A6%E8%A7%A3%E8%87%AA%E7%84%B6%E8%AF%AD%E8%A8%80%E5%A4%84%E7%90%86%E5%8E%9F%E7%90%86%E4%B8%8E%E5%B8%B8%E7%94%A8%E7%AE%97%E6%B3%95.md)
  - 缺点是词向量固定，无法处理多义词语境差异。 [blog.csdn](https://blog.csdn.net/weixin_44986037/article/details/130259034)
- 语境化表示：  
  - ELMo、BERT、GPT 系列，用上下文动态生成词/句向量，是现代 NLP 的主流。 [learn.lianglianglee](https://learn.lianglianglee.com/%E4%B8%93%E6%A0%8F/PyTorch%E6%B7%B1%E5%BA%A6%E5%AD%A6%E4%B9%A0%E5%AE%9E%E6%88%98/21%20NLP%E5%9F%BA%E7%A1%80%EF%BC%88%E4%B8%8A%EF%BC%89%EF%BC%9A%E8%AF%A6%E8%A7%A3%E8%87%AA%E7%84%B6%E8%AF%AD%E8%A8%80%E5%A4%84%E7%90%86%E5%8E%9F%E7%90%86%E4%B8%8E%E5%B8%B8%E7%94%A8%E7%AE%97%E6%B3%95.md)

***

## 3. 传统 NLP 关键模型与算法

在大模型前时代，NLP 主要靠统计与序列模型，现在仍然是打基础、理解 LLM 内部结构的关键。 [learn.lianglianglee](https://learn.lianglianglee.com/%E4%B8%93%E6%A0%8F/PyTorch%E6%B7%B1%E5%BA%A6%E5%AD%A6%E4%B9%A0%E5%AE%9E%E6%88%98/21%20NLP%E5%9F%BA%E7%A1%80%EF%BC%88%E4%B8%8A%EF%BC%89%EF%BC%9A%E8%AF%A6%E8%A7%A3%E8%87%AA%E7%84%B6%E8%AF%AD%E8%A8%80%E5%A4%84%E7%90%86%E5%8E%9F%E7%90%86%E4%B8%8E%E5%B8%B8%E7%94%A8%E7%AE%97%E6%B3%95.md)

- 语言模型（LM）：给一个句子 \(w_1,\dots,w_n\) 计算概率 \(P(w_1,\dots,w_n)\)，常用 n-gram+马尔可夫假设来简化。 [easyai](https://easyai.tech/ai-definition/nlp/)
- 经典序列模型：  
  - HMM（隐马尔可夫）：做分词、词性标注、简单 NER。 [cloud.tencent](https://cloud.tencent.com/developer/article/2023321)
  - CRF（条件随机场）：在整个序列上建模标注，广泛用于分词、NER 等结构化预测任务。 [cloud.tencent](https://cloud.tencent.com/developer/article/2023321)
- 传统机器学习：  
  - 逻辑回归、SVM、朴素贝叶斯、决策树等，用 TF-IDF / n-gram 特征做文本分类、垃圾邮件检测等。 [blog.csdn](https://blog.csdn.net/weixin_44986037/article/details/130259034)

***

## 4. 深度学习 NLP（到 Transformer / LLM）

现代 NLP 基本都是深度学习范式，时间线可以粗记为：RNN → LSTM → CNN → Attention/Transformer → 预训练大模型。 [learn.lianglianglee](https://learn.lianglianglee.com/%E4%B8%93%E6%A0%8F/PyTorch%E6%B7%B1%E5%BA%A6%E5%AD%A6%E4%B9%A0%E5%AE%9E%E6%88%98/21%20NLP%E5%9F%BA%E7%A1%80%EF%BC%88%E4%B8%8A%EF%BC%89%EF%BC%9A%E8%AF%A6%E8%A7%A3%E8%87%AA%E7%84%B6%E8%AF%AD%E8%A8%80%E5%A4%84%E7%90%86%E5%8E%9F%E7%90%86%E4%B8%8E%E5%B8%B8%E7%94%A8%E7%AE%97%E6%B3%95.md)

- RNN/LSTM/GRU：解决序列建模，适合短序列翻译、情感分析，但难并行、长依赖问题明显。 [learn.lianglianglee](https://learn.lianglianglee.com/%E4%B8%93%E6%A0%8F/PyTorch%E6%B7%B1%E5%BA%A6%E5%AD%A6%E4%B9%A0%E5%AE%9E%E6%88%98/21%20NLP%E5%9F%BA%E7%A1%80%EF%BC%88%E4%B8%8A%EF%BC%89%EF%BC%9A%E8%AF%A6%E8%A7%A3%E8%87%AA%E7%84%B6%E8%AF%AD%E8%A8%80%E5%A4%84%E7%90%86%E5%8E%9F%E7%90%86%E4%B8%8E%E5%B8%B8%E7%94%A8%E7%AE%97%E6%B3%95.md)
- CNN（TextCNN）：局部 n-gram 卷积+池化做文本分类，训练速度快、效果不错。 [cloud.tencent](https://cloud.tencent.com/developer/article/2023321)
- Attention 与 Transformer：  
  - Self-Attention 捕捉序列中任意两位置关系，可并行，是 BERT、GPT 的基础。 [learn.lianglianglee](https://learn.lianglianglee.com/%E4%B8%93%E6%A0%8F/PyTorch%E6%B7%B1%E5%BA%A6%E5%AD%A6%E4%B9%A0%E5%AE%9E%E6%88%98/21%20NLP%E5%9F%BA%E7%A1%80%EF%BC%88%E4%B8%8A%EF%BC%89%EF%BC%9A%E8%AF%A6%E8%A7%A3%E8%87%AA%E7%84%B6%E8%AF%AD%E8%A8%80%E5%A4%84%E7%90%86%E5%8E%9F%E7%90%86%E4%B8%8E%E5%B8%B8%E7%94%A8%E7%AE%97%E6%B3%95.md)
  - Transformer encoder：理解型任务（BERT 及变体）。  
  - Transformer decoder：生成型任务（GPT 系列）。 [cloud.tencent](https://cloud.tencent.com/developer/article/2023321)
- 预训练+微调范式：  
  - 先在海量无标注文本上预训练（语言建模/填空），再在具体任务上微调，显著降低标注数据需求，这是 BERT/GPT 成功的关键之一。 [learn.lianglianglee](https://learn.lianglianglee.com/%E4%B8%93%E6%A0%8F/PyTorch%E6%B7%B1%E5%BA%A6%E5%AD%A6%E4%B9%A0%E5%AE%9E%E6%88%98/21%20NLP%E5%9F%BA%E7%A1%80%EF%BC%88%E4%B8%8A%EF%BC%89%EF%BC%9A%E8%AF%A6%E8%A7%A3%E8%87%AA%E7%84%B6%E8%AF%AD%E8%A8%80%E5%A4%84%E7%90%86%E5%8E%9F%E7%90%86%E4%B8%8E%E5%B8%B8%E7%94%A8%E7%AE%97%E6%B3%95.md)

***

## 5. 典型应用任务一览（面向工程实践）

你做 Java+LLM 应用时，会经常遇到这些任务，它们原本是“单点 NLP 任务”，现在大多被一个通用大模型统一搞定。 [cnblogs](https://www.cnblogs.com/auguse/articles/19111241)

- 文本分类：垃圾短信识别、舆情情感分析、话题分类等。 [cnblogs](https://www.cnblogs.com/auguse/articles/19111241)
- 序列标注：  
  - 分词、词性标注 POS  
  - 命名实体识别 NER（人名、地名、组织、时间、金额等）。 [cnblogs](https://www.cnblogs.com/auguse/articles/19111241)
- 句法/依存分析：抽出句子中的主谓宾、修饰关系，支撑信息抽取、问答等。 [cnblogs](https://www.cnblogs.com/auguse/articles/19111241)
- 信息抽取与知识图谱：从文本中抽实体、关系、事件，构建结构化知识，用于搜索、推荐等。 [cloud.tencent](https://cloud.tencent.com/developer/article/2023321)
- 机器翻译：从规则→统计→神经→大模型，是 NLP 工程落地的标志性方向。 [easyai](https://easyai.tech/ai-definition/nlp/)
- 文本摘要 / 改写 / 生成：新闻摘要、产品描述生成、学术摘要等。 [easyai](https://easyai.tech/ai-definition/nlp/)
- 问答与对话系统：检索式 QA、生成式 QA、任务型对话（如客服）、开放式聊天等。 [cnblogs](https://www.cnblogs.com/auguse/articles/19111241)

***

## 6. 给你的一份学习“骨架大纲”

结合你路线（Java 后端 + 大模型应用 + RAG），建议你按这几块系统化掌握，每块都可以配一个小练手项目：

1. **概念与任务图谱**  
   - 能清楚说出：NLP 是什么，两大核心任务（NLU/NLG），常见子任务有哪些。  
2. **文本表示方法**  
   - 从 BOW/TF-IDF → Word2Vec → BERT/GPT 级向量，理解它们的优缺点和适用场景。  
3. **经典模型和评估指标**  
   - HMM、CRF、RNN/LSTM、Attention/Transformer 的基本思想，文本分类/序列标注常见指标（accuracy、precision/recall/F1）。  
4. **预训练模型与微调 / 提示工程**  
   - BERT/GPT 的基本结构，微调思路；在工程上如何通过 prompt 或 RAG 来“用”而不是“训”。  
5. **典型工程场景**  
   - 做一个“文档问答系统”：文本预处理+向量化+检索+LLM 生成，这是你后面 RAG 项目的基石。

