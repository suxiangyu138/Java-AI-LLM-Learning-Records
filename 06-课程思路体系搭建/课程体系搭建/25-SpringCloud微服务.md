# 黑马SpringCloud微服务全课程

> 从微服务架构入门到生产级分布式高可用系统，完整覆盖SpringCloud Alibaba核心组件、Docker容器化部署、RabbitMQ消息队列、Elasticsearch搜索引擎、Sentinel流量防护、Seata分布式事务及大厂面试源码剖析的全链路课程体系。

### 模块一：微服务基础与SpringCloud Alibaba核心（微服务入门篇）

**导学**
- 001 微服务技术栈导学1
- 002 微服务技术栈导学2

**微服务架构认知**
- 001 今日课程介绍
- 002 认识微服务-服务架构演变（单体→垂直→微服务）
- 003 认识微服务-微服务技术对比（SpringCloud vs Cloud Alibaba）
- 004 认识微服务-SpringCloud全家桶介绍

**服务拆分与远程调用**
- 005 服务拆分-案例Demo（订单、用户业务拆分）
- 006 服务拆分-服务远程调用（RestTemplate基础调用）

**注册中心Eureka**
- 007 Eureka-提供者与消费者
- 008 Eureka-eureka原理分析（AP架构、自我保护机制）
- 009 Eureka-搭建eureka服务
- 010 Eureka-服务注册
- 011 Eureka-服务发现

**负载均衡Ribbon**
- 012 Ribbon-负载均衡原理
- 013 Ribbon-负载均衡策略（轮询、随机、权重等）
- 014 Ribbon-饥饿加载配置

**注册中心Nacos（Alibaba核心）**
- 015 Nacos-认识和安装Nacos
- 016 Nacos-快速入门（服务注册发现）
- 017 Nacos-服务多级存储模型（集群、分组）
- 018 Nacos-NacosRule自定义负载均衡
- 019 Nacos-服务实例的权重设置
- 020 Nacos-环境隔离（命名空间、分组）
- 021 Nacos-Nacos和Eureka的对比（CP/AP取舍）

**Nacos配置中心**
- 001 今日课程介绍2
- 002 Nacos配置管理-Nacos实现配置管理
- 003 Nacos配置管理-微服务配置拉取
- 004 Nacos配置管理-配置热更新（动态刷新）
- 005 Nacos配置管理-多环境配置共享
- 006 Nacos配置管理-nacos集群搭建

**远程调用Feign**
- 007 Feign-基于Feign远程调用（替代RestTemplate）
- 008 Feign-自定义配置（日志、超时、拦截器）
- 009 Feign-性能优化（连接池、超时参数调优）
- 010 Feign-最佳实践分析
- 011 Feign-实现Feign最佳实践（继承式、抽取API模块）

**网关Gateway**
- 012 Gateway网关-网关作用介绍
- 013 Gateway网关-快速入门
- 014 Gateway网关-路由断言工厂（时间、路径、请求头）
- 015 Gateway网关-路由的过滤器配置（局部过滤器）
- 016 Gateway网关-全局过滤器（统一鉴权、跨域）
- 017 Gateway网关-过滤器链执行顺序
- 018 Gateway网关-网关的cors跨域配置

### 模块二：Docker容器化部署
- 001 今日课程介绍3
- 002 初识Docker-什么是docker
- 003 初识Docker-Docker和虚拟机的差别
- 004 初识Docker-Docker架构（镜像、容器、仓库）
- 005 初识Docker-Docker的安装
- 006 使用Docker-镜像命令（pull、images、rmi）
- 007 使用Docker-镜像命令练习
- 008 使用Docker-容器命令介绍（run、exec、logs、rm）
- 009 使用Docker-容器命令案例1
- 010 使用Docker-容器命令案例2
- 011 使用Docker-容器命令练习
- 012 使用Docker-数据卷命令（数据持久化）
- 013 使用Docker-数据卷挂载案例1
- 014 使用Docker-数据卷挂载案例2
- 015 自定义镜像-镜像分层结构
- 016 自定义镜像-Dockerfile语法与实战
- 017 DockerCompose-初识Compose（批量编排容器）
- 018 DockerCompose-部署微服务集群（一键启动全套服务）
- 019 Docker镜像仓库（私有Harbor仓库搭建）

### 模块三：RabbitMQ消息队列 & SpringAMQP
- 001 今日课程介绍4
- 002 初识MQ--同步通讯的优缺点
- 003 初识MQ--异步通讯的优缺点
- 004 初识MQ--mq常见技术介绍（RabbitMQ/RocketMQ/Kafka）
- 005 RabbitMQ快速入门--介绍和安装
- 006 RabbitMQ快速入门--消息模型介绍（5大模型）
- 007 RabbitMQ快速入门--简单队列模型
- 008 SpringAMQP--基本介绍
- 009 SpringAMQP--入门案例的消息发送
- 010 SpringAMQP--入门案例的消息接收
- 011 SpringAMQP--WorkQueue工作队列模型（能者多劳）
- 012 SpringAMQP--发布订阅模型介绍
- 013 SpringAMQP--FanoutExchange扇形交换机（广播）
- 014 SpringAMQP--DirectExchange直连交换机（精准匹配）
- 015 SpringAMQP--TopicExchange主题交换机（模糊匹配）
- 016 SpringAMQP--消息转换器（JSON序列化替代JDK序列化）

### 模块四：Elasticsearch搜索引擎（ES）

**ES基础入门**
- 001 今日课程介绍5
- 002 初识ES-什么是elasticsearch
- 003 初识ES-倒排索引原理
- 004 初识ES-es与mysql的概念对比
- 005 初识ES-安装es（单节点）
- 006 初识ES-安装kibana（可视化控制台）
- 007 初识ES-安装IK分词器
- 008 初识ES-IK分词器的拓展和停用词典

**索引库、文档CRUD**
- 009 操作索引库-mapping属性（字段类型、分词规则）
- 010 操作索引库-创建索引库
- 011 操作索引库-查询、删除、修改索引库
- 012 文档操作-新增、查询、删除文档
- 013 文档操作-修改文档

**Java客户端RestClient操作ES**
- 014 RestClient操作索引库-导入demo
- 015 RestClient操作索引库-hotel数据结构分析
- 016 RestClient操作索引库-初始化RestClient
- 017 RestClient操作索引库-创建索引库
- 018 RestClient操作索引库-删除和判断索引库
- 019 RestClient操作文档-新增文档
- 020 RestClient操作文档-查询文档
- 021 RestClient操作文档-更新文档
- 022 RestClient操作文档-删除文档
- 023 RestClient操作文档-批量导入文档

**DSL查询语法**
- 001 今日课程介绍6
- 002 DSL查询语法-DSL查询分类和基本语法
- 003 DSL查询语法-全文检索查询（match/multi_match）
- 004 DSL查询语法-精确查询（term/range）
- 005 DSL查询语法-地理查询（geo_distance附近搜索）
- 006 DSL查询语法-相关性算分（TF-IDF、BM25）
- 007 DSL查询语法-FunctionScoreQuery（自定义权重打分）
- 008 DSL查询语法-BooleanQuery（多条件组合）

**搜索结果处理**
- 009 搜索结果处理-排序
- 010 搜索结果处理-分页
- 011 搜索结果处理-高亮

**RestClient实现DSL查询**
- 012 RestClient查询文档-快速入门
- 013 RestClient查询文档-match、term、range、bool…
- 014 RestClient查询文档-排序和分页
- 015 RestClient查询文档-高亮显示

**实战：黑马酒店搜索案例**
- 016 黑马旅游案例-搜索、分页
- 017 黑马旅游案例-条件过滤
- 018 黑马旅游案例-我附近的酒店（地理检索）
- 019 黑马旅游案例-广告置顶（FunctionScore）

**ES数据聚合、自动补全**
- 001 今日内容介绍7
- 002 数据聚合-聚合的分类（Bucket桶聚合、Metrics指标聚合）
- 003 数据聚合-DSL实现Bucket聚合
- 004 数据聚合-DSL实现Metrics聚合
- 005 数据聚合-RestClient实现聚合
- 006 数据聚合-多条件聚合
- 007 数据聚合-带过滤条件的聚合
- 008 自动补全-安装拼音分词器
- 009 自动补全-自定义分词器
- 010 自动补全-DSL实现自动补全查询
- 011 自动补全-修改酒店索引库数据结构
- 012 自动补全-RestAPI实现自动补全查询
- 013 自动补全-实现搜索框自动补全前端交互

**MySQL与ES数据同步**
- 014 数据同步-同步方案分析（双写、MQ、Canal）
- 015 数据同步-导入酒店管理项目
- 016 数据同步-声明队列和交换机
- 017 数据同步-发送mq消息（增删改时发消息）
- 018 数据同步-监听MQ消息，更新ES文档
- 019 数据同步-测试同步功能

**ES集群**
- 020 es集群-集群结构介绍（主分片、副本分片）
- 021 es集群-搭建集群
- 022 es集群-集群职责及脑裂问题
- 023 ES集群-分布式新增和查询流程
- 024 ES集群-故障转移机制

### 模块五：微服务高级篇（分布式高可用核心技术）

**Day1 流量防护Sentinel**
- 001 高级篇Day1-01-初识Sentinel
- 002 高级篇Day1-02-限流规则（流控、热点参数限流）
- 003 高级篇Day1-03-隔离和降级（熔断、慢调用降级）
- 004 高级篇Day1-04-授权规则及规则持久化

**Day2 分布式事务Seata**
- 001 高级篇Day2-01-分布式事务理论基础（CAP、BASE、2PC、TCC）
- 002 高级篇Day2-02-初识Seata（AT模式）
- 003 高级篇Day2-03-动手实践Seata分布式事务
- 004 高级篇Day2-04-Seata高可用集群搭建

**Day3 Redis高可用架构**
- 001 高级篇Day3-01-Redis持久化（RDB/AOF）
- 002 高级篇Day3-02-Redis主从复制
- 003 高级篇Day3-03-Redis哨兵Sentinel（故障自动转移）
- 004 高级篇Day3-04-Redis分片集群（水平扩容）

**Day4 多级缓存方案**
- 001 高级篇Day4-01-多级缓存意义及JVM进程缓存（Caffeine）
- 002 高级篇Day4-02-Lua脚本语法（Redis原子操作）
- 003 高级篇Day4-03-多级缓存整合实战
- 004 高级篇Day4-04-缓存同步、缓存穿透/击穿/雪崩解决方案

**Day5 MQ高级特性与集群**
- 001 高级篇Day5-01-MQ常见问题及消息可靠性（丢失、重复、顺序）
- 002 高级篇Day5-02-死信交换机（消息失败重试）
- 003 高级篇Day5-03-消息堆积及惰性队列
- 004 高级篇Day5-04-MQ集群（普通集群、镜像集群、仲裁队列）

### 模块六：面试篇（源码深度剖析，大厂高频面试题）
- 001 面试篇-00-课程介绍
- 002 面试篇-01-SpringCloud常见组件有哪些

**Nacos源码专题**
- 003 面试篇-02-Nacos注册表结构-多级存储模型
- 004 面试篇-03-Nacos注册表结构-Nacos源码运行
- 005 面试篇-04-Nacos注册表结构-深入源码分析
- 006 面试篇-05-Nacos如何应对高并发的注册压力
- 007 面试篇-06-Nacos如何解决并发读写冲突问题
- 008 面试篇-07-Nacos如何实现服务健康检测
- 009 面试篇-08-Nacos的服务拉取和订阅机制

**Sentinel限流熔断源码专题**
- 010 面试篇-09-Sentinel的线程隔离与Hystrix的区别
- 011 面试篇-10-Sentinel与Gateway的限流对比，滑动窗口、令牌桶、漏桶
- 012 面试篇-11-Sentinel的核心API-ProcessorSlotChain责任链
- 013 面试篇-12-Sentinel的核心API-Entry
- 014 面试篇-13-Sentinel的核心API-Context的初始化
- 015 面试篇-14-Sentinel的核心API综合流程分析
- 016 面试篇-15-Sentinel执行链路-NodeSelectSlot和ClusterSlot
- 017 面试篇-16-Sentinel执行链路-StatisticSlot（统计指标）
- 018 面试篇-17-Sentinel执行链路-AuthoritySlot（黑白名单）
- 019 面试篇-18-Sentinel执行链路-SystemSlot（系统保护）
- 020 面试篇-19-Sentinel执行链路-ParamFlowSlot的热点令牌桶限流
- 021 面试篇-20-Sentinel执行链路-FlowSlot的滑动时间窗口限流
- 022 面试篇-21-Sentinel执行链路-FlowSlot的漏桶模式
- 023 面试篇-22-Sentinel执行链路-DegradeSlot-熔断器降级
