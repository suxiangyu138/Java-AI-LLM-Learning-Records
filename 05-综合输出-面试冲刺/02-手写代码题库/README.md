# 综合面试模拟 + 手写代码

> 面试全真模拟 + 高频代码手写训练 | 复习第 14 天

---

## 面试模拟清单

### 一、Java 后端基础面试（约 30 min）

| 考点 | 高频题 |
|------|--------|
| 集合 | HashMap 原理、ConcurrentHashMap 分段锁 |
| 并发 | volatile、synchronized 锁升级、AQS、线程池参数 |
| JVM | 内存模型、GC 算法、类加载双亲委派 |
| MySQL | B+树、MVCC、事务隔离级别、SQL 优化 |
| Redis | 数据结构、持久化、缓存三问题、分布式锁 |
| MQ | 消息丢失/重复/积压、死信队列 |
| 微服务 | Nacos、Gateway、Sentinel、分布式事务 |

### 二、AI 专项面试（约 20 min）

| 考点 | 内容 |
|------|------|
| RAG | 完整链路、多路召回、RRF、MMR、幻觉抑制 |
| Agent | ReAct 循环、Planner、Multi-Agent 协作 |
| 大模型网关 | 多厂商抽象、Resilience4j 熔断、安全护栏 |
| Prompt | 思维链、Few-shot、Function Calling |
| 向量库 | Milvus 索引类型、ANN 算法选择 |

### 三、手写代码重点

| 类别 | 题目 |
|------|------|
| 集合 | 手写简易 HashMap（数组+链表+扩容） |
| 并发 | 自定义线程池（核心池+阻塞队列+拒绝策略） |
| 分布式锁 | Redis Lua 脚本分布式锁 |
| 延迟队列 | Redis ZSet 实现延迟关单 |
| 流式输出 | Spring Boot SSE 简易实现 |
| RAG Demo | 极简 RAG 检索代码（Embedding + 相似度 + LLM） |
| Agent | 简易 ReAct Agent 循环代码 |

### 四、项目口述标准化

每项目按模板：**架构 → 难点 → 方案 → 优化指标**

> Flavor Dash：单体架构 + AI 六大模块 + 多模型网关  
> SuGuangMall：微服务拆分 + Multi-Agent + 秒杀零超卖  
> LingShu：14步RAG + ReAct Agent + 医疗安全护栏

---

> 📅 复习建议：1 天，模拟面试 + 手写代码，重点关注薄弱环节
