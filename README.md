# Java后端开发 + AI大模型应用开发 学习笔记

> 面向 **Java后端开发工程师** 与 **AI大模型应用开发工程师** 的系统化学习仓库，按学习路径组织，聚焦核心知识、实战项目、源码沉淀与面试准备。

---

## 仓库结构总览

```
Java-AI-LLM-Learning-Records/
│
├── README.md                      # 本文件
├── 00-学习路线与必做项目/           # 学习路线图 + 各技术栈必做项目清单
│
├── docs/                          # 核心学习笔记（按学习路径编号）
│   ├── 01-JavaSE基础              # 关键字、OOP、封装继承多态、内部类、命名规范
│   ├── 02-JavaSE高级              # 集合、IO、泛型、反射、注解、正则、网络编程、新特性
│   ├── 03-设计模式                # GoF 23种设计模式详解 + UML + 面向对象设计原则
│   ├── 04-JavaWeb与HTTP           # Servlet、JSP、HTTP/HTTPS、TCP/IP、Tomcat源码剖析、前端基础
│   ├── 05-MySQL                   # 基本操作、数据类型、单表/多表、索引、视图、事务、优化、设计
│   ├── 06-MyBatis                 # XML/注解方式、动态SQL、缓存、插件、代码生成器、Spring集成
│   ├── 07-Redis                   # 数据结构、命令、持久化、集群、缓存设计、Spring Data Redis
│   ├── 08-Spring-Framework        # IoC、AOP、Bean装配、注解开发、数据库编程
│   ├── 09-SpringMVC               # 请求处理、拦截器、文件上传下载、数据校验、标签库
│   ├── 10-SpringBoot              # 自动配置、Web开发、数据访问、缓存、消息、安全、监控
│   ├── 11-Spring-Cloud            # 微服务治理、Nacos、Sentinel、Gateway、分布式事务、RocketMQ
│   ├── 12-Spring-Extensions       # Spring Security、Spring Data、Spring AMQP、Spring AI
│   ├── 13-消息队列                # RabbitMQ 全面解析
│   ├── 14-ElasticSearch           # ES搜索引擎
│   ├── 15-MongoDB                 # MongoDB文档数据库
│   ├── 16-Nginx                   # 反向代理、负载均衡、缓存、日志
│   ├── 17-并发编程                # 线程、锁、JUC、高并发设计、Netty网络编程
│   ├── 18-JVM                     # 内存模型、垃圾收集、类加载、编译优化、性能调优、监控工具
│   ├── 19-Docker                  # 镜像、容器、网络、存储、Compose、Swarm、Registry
│   ├── 20-Linux与Shell            # Linux系统管理 + Shell/Bash/BAT脚本编程
│   ├── 21-Git与GitHub             # 版本控制、分支管理、协作开发、GitHub生态
│   ├── 22-Maven与Gradle           # 依赖管理、构建生命周期、插件、多模块
│   ├── 23-软件工程与项目管理       # 架构驱动开发、项目实战文档、CI/CD
│   ├── 24-计算机基础理论           # 操作系统/网络/组成/编译/密码学/数学/数据结构与算法(含《算法导论》)
│   │
│   ├── 25-AI大模型基础            # Transformer/BERT/GPT/GLM、LLM架构、预训练、ML/DL基础、NLP、数学
│   ├── 26-Prompt工程              # Prompt设计、优化技巧、进阶方法论
│   ├── 27-Embedding与向量数据库    # Embedding原理+类型+模型、ChromaDB、Milvus
│   ├── 28-RAG技术                 # RAG完整流程、检索优化、企业级RAG引擎、评估、Data Agent
│   ├── 29-Agent与MCP              # AI Agent核心架构、Skill、MCP协议
│   ├── 30-LangChain与框架         # LangChain核心组件与开发模式
│   ├── 31-Ollama与本地部署         # 模型服务化部署、Ollama本地运行
│   ├── 32-模型微调                # PEFT参数高效微调
│   ├── 33-Spring-AI               # Spring AI框架 + Java后端集成大模型
│   ├── 34-Python语言              # Python基础到高级 + OOP + 异步 + PyTorch机器学习
│   ├── 35-AI开发工具              # AI智能编码、Copilot、LLM API调用、HuggingFace
│   ├── 36-数据分析与爬虫           # NumPy/Pandas数据分析 + Python爬虫
│   ├── 37-AI产品与商业            # AI产品思维、应用架构、运维、容灾
│   ├── 38-AI参考资料              # 推荐书籍与学习资料
│   ├── 39-面试准备                # Java八股文、面试题汇总、AI面试、求职路线
│   └── 40-职业规划与发展           # 职业路径、学习方法、技能提升指南
│
├── projects/                      # 完整多文件项目
│   ├── java/                      # 类QQ通信平台、博客系统、五子棋、TCP/UDP聊天等17个项目
│   └── python/                    # AI Demo、爬虫、数据分析、机器学习练习
│
├── code-practice/                 # 小型独立练习
│   ├── java/                      # IO操作、Stream流、集合、泛型、排序算法等16个练习
│   └── python/                    # 排序算法合集（冒泡/快排/归并/堆/希尔/基数等）
│
├── leetcode/                      # LeetCode刷题（数组/DP/回溯/图/树/链表）
│
├── daily-notes/                   # 日常学习笔记
│   ├── ai-learning/               # AI方向零散记录
│   ├── java-backend/              # Java后端零散记录
│   ├── python/                    # Python零散记录
│   ├── tools/                     # 开发工具与环境
│   ├── study-methods/             # 学习方法与求职
│   ├── homework/                  # 学科作业
│   └── personal/                  # 个人笔记
│
├── thinking/                      # 深度思考随笔
│   ├── java-core/                 # Java后端核心技术思考
│   ├── ai-tools/                  # AI与开发工具思考
│   ├── interview-career/          # 面试与求职思考
│   ├── cs-theory/                 # 计算机基础理论思考
│   └── growth/                    # 学习方法与成长
│
└── images/                        # 文档配图和截图
```

---

## 推荐学习路线

### 阶段一：Java基础（docs/01-03）
```
01-JavaSE基础 → 02-JavaSE高级 → 03-设计模式
```
配合 `code-practice/java/` 动手练习。

### 阶段二：数据库与中间件（docs/05-07, 13-16）
```
05-MySQL → 06-MyBatis → 07-Redis → 13-消息队列 → 16-Nginx
```

### 阶段三：JavaWeb与框架（docs/04, 08-12）
```
04-JavaWeb与HTTP → 08-Spring-Framework → 09-SpringMVC → 10-SpringBoot → 11-Spring-Cloud
```

### 阶段四：Java进阶（docs/17-18, 24）
```
17-并发编程 → 18-JVM → 24-计算机基础理论
```

### 阶段五：DevOps与工具（docs/19-23）
```
19-Docker → 20-Linux与Shell → 21-Git与GitHub → 22-Maven与Gradle → 23-软件工程与项目管理
```

### 阶段六：AI大模型开发（docs/25-37）
```
25-AI大模型基础 → 26-Prompt工程 → 27-Embedding与向量数据库 → 28-RAG技术
→ 29-Agent与MCP → 30-LangChain与框架 → 31-Ollama与本地部署 → 33-Spring-AI
```
Python基础安排在 `34-Python语言`，可穿插学习。

### 阶段七：面试冲刺（docs/39-40）
```
39-面试准备 → 40-职业规划与发展
```
配合 `00-学习路线与必做项目/` 中的必做项目清单。

---

## 笔记规范

### 单篇笔记模板
```md
# 标题
## 1. 核心概念
## 2. 原理分析
## 3. 常见面试题
## 4. 实战应用
## 5. 易错点
## 6. 总结
```

### 输出要求
- 以 Markdown 为主，保证可直接用于 GitHub 展示
- 每篇笔记回答"是什么、为什么、怎么用、有哪些坑"
- 理论与实践分目录整理，代码就近放置
- 每学完一个专题输出一份总结文档

---

## 推荐实战项目

### Java后端项目
- 后台管理系统：用户、角色、权限、JWT、RBAC、日志审计
- 文件上传下载系统：本地存储 / OSS、断点续传、文件校验
- 秒杀系统：Redis + MQ + 限流 + 异步削峰
- 订单系统：分库分表、分布式事务、幂等控制
- 类QQ即时通讯平台（已实现，见 `projects/java/qq-chat-platform/`）

### AI大模型项目
- RAG知识库问答系统
- 企业文档智能助手
- 基于 Ollama 的本地 AI 助手
- 多 Agent 协作任务系统
- Java + Python 混合架构的大模型应用平台
- ChromaDB / Milvus 向量数据库实战

---

## 致自己

保持长期主义，拒绝零散学习。

把每一篇笔记都写成未来面试、简历、项目复盘时可以直接复用的资产。
