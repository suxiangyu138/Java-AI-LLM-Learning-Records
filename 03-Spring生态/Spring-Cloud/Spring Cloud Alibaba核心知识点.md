Spring Cloud Alibaba 是阿里巴巴开源的微服务解决方案，在Spring Cloud Netflix组件停更后成为企业级项目的主流选择，提供基于Spring Boot和Spring Cloud的全链路微服务架构 。 [blog.csdn](https://blog.csdn.net/weixin_52236586/article/details/143425891)

## 核心组件

### Nacos - 注册与配置中心
Nacos整合了服务注册发现与配置管理两大功能，完全替代Eureka和Spring Cloud Config 。服务启动时向Nacos注册元数据并开启心跳机制（默认5秒），Nacos会自动剔除长时间无心跳的实例 。 [juejin](https://juejin.cn/post/7492621778319818802)

### Sentinel - 流控与熔断降级
Sentinel提供流量控制、熔断降级、系统负载保护等服务雪崩防护机制 。支持多维度规则配置（QPS、线程数、响应时间），通过控制台动态热更新规则，支持注解式和代码式接入 。默认集成WebServlet、OpenFeign、Gateway等组件的限流降级功能 。 [github](https://github.com/scottxing/Spring-Cloud-Alibaba-Practice)

### OpenFeign - 声明式服务调用
通过接口方式定义HTTP调用，集成LoadBalancer实现负载均衡 。支持服务间端到端的远程调用，简化RestTemplate的复杂配置 。 [blog.csdn](https://blog.csdn.net/weixin_52236586/article/details/143425891)

### Gateway - API网关
Spring Cloud Gateway是官方推荐的网关方案，支持动态路由、权限控制、限流等功能 。可结合Sentinel实现网关层的流量控制，通过配置Predicates实现路径匹配转发 。 [juejin](https://juejin.cn/post/7492621778319818802)

### Seata - 分布式事务
提供企业级分布式事务解决方案，支持AT模式（关系型数据库自动代理）和TCC模式（手动定义Try/Confirm/Cancel） 。事务发起方通过TC（事务协调器）进行全局事务协调，分支事务通过TM与RM注册到TC执行提交或回滚 。 [blog.csdn](https://blog.csdn.net/weixin_52236586/article/details/143425891)

### RocketMQ - 消息驱动
阿里开源的分布式消息队列，支持高可用、事务消息、定时消息等特性 。基于Spring Cloud Stream为微服务应用构建消息驱动能力 。 [github](https://github.com/scottxing/Spring-Cloud-Alibaba-Practice)

## 负载均衡策略

Spring Cloud Alibaba集成Ribbon实现客户端负载均衡 。通过`@LoadBalanced`注解开启负载均衡，支持多种策略：

- **RoundRobinRule**：轮询策略（默认）
- **RandomRule**：随机选择
- **WeightedResponseTimeRule**：响应时间加权，响应快的服务器被选中概率更大
- **AvailabilityFilteringRule**：过滤故障和高并发连接的服务器
- **ZoneAvoidanceRule**：综合区域性能和服务器可用性轮询选择 

配置建议：机器配置一致时使用默认轮询策略，配置差异大时可选择WeightedResponseTimeRule 。

## 技术栈对比

| 组件类型 | Spring Cloud | Spring Cloud Alibaba |
|---------|--------------|---------------------|
| 注册中心 | Eureka | Nacos |
| 配置中心 | Config | Nacos |
| 熔断降级 | Hystrix | Sentinel |
| 网关 | Zuul | Gateway |
| 分布式事务 | - | Seata |
| 消息队列 | - | RocketMQ |

## 版本兼容

推荐使用官方BOM管理依赖版本，典型组合为Spring Boot 2.3.x + Spring Cloud Hoxton + Alibaba Cloud 2.2.x 。 [blog.csdn](https://blog.csdn.net/weixin_52236586/article/details/143425891)
