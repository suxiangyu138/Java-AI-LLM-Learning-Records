04.28 10:53
Agent Memory 快速入门指南
一、核心定义
Agent Memory 是智能 Agent 的记忆系统，负责存储、管理、检索历史对话、任务经验、外部知识，让大模型摆脱「单次对话无记忆」的局限，具备上下文连贯、持续学习、任务迭代的能力。
简单理解：
大模型本身是瞬时记忆，Agent Memory 给它加上长期记忆。
 
二、Agent Memory 三大核心分类
这是所有框架（LangChain、LlamaIndex、Spring AI）统一的标准划分
1. 短期记忆（Short-term Memory）
- 本质：对话上下文窗口
- 作用：保存当前会话的历史聊天记录，让模型知道「上一句说了什么」
- 特点：
- 存在内存中，会话结束即清空
- 受 LLM 上下文窗口长度限制
- 常见实现： ConversationBufferMemory 
2. 长期记忆（Long-term Memory）
- 本质：持久化存储的知识库
- 作用：保存用户偏好、历史任务、事实知识、行为习惯
- 特点：
- 持久化到数据库/向量库
- 会话重启后仍可复用
- 常见实现：向量数据库、关系型数据库、文件存储
3. 实体记忆（Entity Memory）
- 本质：抽取对话中的关键实体并存储
- 作用：记住特定人物、事物、概念的属性
- 示例：记住用户姓名、喜好、目标
- 常见实现：实体抽取 + 结构化存储
 
三、Agent Memory 四大核心机制
1. 记忆写入（Write）
将对话、任务结果、外部知识结构化处理后存入记忆
- 简单对话：直接拼接文本
- 复杂知识：向量化、实体抽取、摘要压缩
2. 记忆检索（Retrieve）
用户提问时，检索相关历史记忆，拼接到 Prompt 中
- 短期：直接取最近 N 轮对话
- 长期：向量相似度检索、关键词检索
3. 记忆压缩（Compress）
解决上下文窗口溢出问题
- 对长对话做摘要总结
- 合并重复信息
- 只保留关键信息
4. 记忆反思（Reflect）
Agent 对历史行为做复盘总结，提炼经验
- 强化正确行为
- 修正错误决策
- 优化后续行动策略
 
四、最常用的 5 种 Memory 实现（LangChain 标准）
1. ConversationBufferMemory
完整保存全部对话历史，最简单，易超长
2. ConversationSummaryMemory
自动总结长对话，只存摘要，解决窗口溢出
3. ConversationBufferWindowMemory
只保留最近 K 轮对话，滑动窗口机制
4. VectorStoreMemory
长期记忆，对话向量化存入向量库，语义检索
5. EntityMemory
抽取对话中人物/实体信息，结构化存储
 
五、极简 Java 代码示例（Spring AI）
java
// 1. 定义短期记忆
ChatMemory memory = new InMemoryChatMemory();
// 2. 存入对话
memory.add(UserMessage.of("我叫张三"));
memory.add(AssistantMessage.of("你好张三，很高兴认识你"));
// 3. 获取记忆
List<Message> history = memory.get();
// 4. 注入到 Prompt
Prompt prompt = new Prompt("你是谁？", history);
 
 
六、极简 Python 代码示例（LangChain）
python
from langchain.memory import ConversationBufferMemory
# 初始化记忆
memory = ConversationBufferMemory()
# 写入对话
memory.save_context({"input": "我叫张三"}, {"output": "你好张三"})
# 读取记忆
print(memory.load_memory_variables({}))
 
 
七、面试/开发高频核心考点
1. 短期记忆和长期记忆的区别
短期：内存、会话级、上下文连贯
长期：持久化、全局级、知识复用
2. 如何解决记忆溢出？
- 滑动窗口
- 对话摘要压缩
- 向量库检索替代全量拼接
3. 记忆反思的作用？
让 Agent 具备自我迭代能力，越用越聪明
4. RAG 和 Agent Memory 的区别？
RAG：外部静态知识库
Memory：动态对话/行为记忆
 
八、一句话总结
Agent Memory = 短期上下文窗口 + 长期持久知识库 + 实体属性存储 + 反思迭代机制
是智能 Agent 区别于普通对话机器人的核心灵魂组件。

