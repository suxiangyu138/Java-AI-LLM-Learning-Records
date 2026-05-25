# Java后端开发 + AI大模型应用开发 学习笔记

> 面向 **Java后端开发工程师** 与 **AI大模型应用开发工程师** 的系统化学习仓库，按学习路径组织，聚焦核心知识、实战项目、源码沉淀与面试准备。

---

## 仓库结构总览

```
Java-AI-LLM-Learning-Records/
│
├── README.md                      # 本文件
├── LICENSE
│
├── 00-学习路线与必做项目/           # 学习路线图 + 各技术栈必做项目清单
│
├── 01-JavaSE/                     # Java基础 → 高级：OOP、集合、IO、泛型、反射、多线程、JVM、新特性
│
├── 02-JavaWeb与中间件/             # Web基础 + 数据库 + 中间件
│   ├── JavaWeb与HTTP/             #   Servlet、JSP、HTTP/HTTPS、TCP/IP、Tomcat、前端基础
│   ├── MySQL/                     #   基本操作、索引、事务、优化、设计
│   ├── MyBatis/                   #   XML/注解、动态SQL、缓存、Spring集成
│   ├── Redis/                     #   数据结构、持久化、集群、缓存设计
│   ├── ElasticSearch/             #   全文搜索引擎
│   ├── MongoDB/                   #   文档数据库
│   ├── Nginx/                     #   反向代理、负载均衡
│   └── 消息队列/                   #   RabbitMQ 全面解析
│
├── 03-Spring生态/                  # Spring 全家桶
│   ├── Spring-Framework/          #   IoC、AOP、Bean装配、注解开发
│   ├── SpringMVC/                 #   请求处理、拦截器、文件上传、数据校验
│   ├── SpringBoot/                #   自动配置、Web开发、数据访问、监控
│   ├── Spring-Cloud/              #   微服务治理、Nacos、Sentinel、Gateway
│   ├── Spring-Extensions/         #   Security、Data、AMQP
│   └── Spring-AI/                 #   Spring AI 集成大模型
│
├── 04-工程化与运维/                 # 进阶 + DevOps
│   ├── 并发编程/                   #   线程、锁、JUC、高并发设计
│   ├── JVM/                       #   内存模型、GC、类加载、调优
│   ├── Docker/                    #   镜像、容器、Compose、Swarm
│   ├── Linux与Shell/              #   系统管理 + Shell脚本
│   ├── Git与GitHub/               #   版本控制、分支管理、协作
│   ├── Maven与Gradle/             #   依赖管理、构建、多模块
│   └── 软件工程与项目管理/          #   架构驱动、CI/CD
│
├── 05-计算机基础/                   # 计算机科学核心理论
│   └── 计算机基础理论/             #   操作系统/网络/组成/编译/数据结构/算法/数学
│
├── 06-AI与大模型/                   # AI 核心技术栈
│   ├── AI大模型基础/               #   Transformer/BERT/GPT、LLM架构、ML/DL、NLP
│   ├── Prompt工程/                 #   Prompt设计、优化技巧
│   ├── Embedding与向量数据库/       #   Embedding原理、ChromaDB、Milvus
│   ├── RAG技术/                    #   RAG流程、检索优化、企业级引擎
│   ├── Agent与MCP/                 #   AI Agent架构、MCP协议
│   ├── LangChain与框架/            #   LangChain核心组件
│   ├── Ollama与本地部署/            #   模型服务化、Ollama
│   └── 模型微调/                   #   PEFT参数高效微调
│
├── 07-Python与数据处理/             # Python + 数据
│   ├── Python语言/                 #   Python基础到高级、OOP、异步、PyTorch
│   └── 数据分析与爬虫/              #   NumPy/Pandas + 爬虫
│
├── 08-AI开发工具与产品/             # AI工具 + 产品 + 参考资料
│   ├── AI开发工具/                 #   Cursor、Claude Code、LLM API
│   ├── AI产品与商业/               #   AI产品思维、运维、容灾
│   └── AI参考资料/                 #   推荐书籍与学习资料
│
├── 09-面试与职业/                   # 面试准备 + 职业规划
│   ├── 面试准备/                   #   Java八股文、AI面试
│   └── 职业规划与发展/              #   职业路径、学习方法
│
├── 10-设计模式/                     # GoF 23种设计模式 + UML + 设计原则
│
├── 项目实战/                        # 完整多文件项目
│   ├── java/                      #   QQ聊天平台、博客系统、五子棋、TCP/UDP聊天等
│   └── python/                    #   AI Demo、爬虫、数据分析、机器学习
│
├── 代码练习/                        # 小型独立练习
│   ├── java/                      #   IO、Stream、集合、泛型、排序算法等
│   └── python/                    #   排序算法合集
│
├── leetcode/                      # LeetCode刷题（数组/DP/回溯/图/树/链表）
│
├── 个人发展/                        # 个人成长与管理
│   ├── 学期规划/                   #   学习/身体/财务规划
│   ├── 知识体系/                   #   个人修养/身体素质/英语
│   ├── 每日笔记/                   #   AI学习/Java后端/工具/学习方法/作业
│   └── 思考总结/                   #   Java核心/AI工具/面试/成长/计算机理论
│
├── 参考速查/                        # 核心知识速查手册
└── images/                        # 文档配图和截图
```

---

## 推荐学习路线

### 阶段一：Java基础
```
01-JavaSE → 10-设计模式
```
配合 `代码练习/java/` 动手练习。

### 阶段二：数据库与中间件
```
02-JavaWeb与中间件/MySQL → MyBatis → Redis → 消息队列 → Nginx
```

### 阶段三：JavaWeb与框架
```
02-JavaWeb与中间件/JavaWeb与HTTP → 03-Spring生态/Spring-Framework → SpringMVC → SpringBoot → Spring-Cloud
```

### 阶段四：Java进阶
```
04-工程化与运维/并发编程 → JVM → 05-计算机基础
```

### 阶段五：DevOps与工具
```
04-工程化与运维/Docker → Linux与Shell → Git与GitHub → Maven与Gradle → 软件工程与项目管理
```

### 阶段六：AI大模型开发
```
06-AI与大模型/AI大模型基础 → Prompt工程 → Embedding与向量数据库 → RAG技术 → Agent与MCP → LangChain与框架 → Ollama与本地部署 → 03-Spring生态/Spring-AI
```
Python基础安排在 `07-Python与数据处理/Python语言`，可穿插学习。

### 阶段七：面试冲刺
```
09-面试与职业/面试准备 → 职业规划与发展
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
- 类QQ即时通讯平台（已实现，见 `项目实战/java/QQ聊天平台/`）

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
