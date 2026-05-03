# Spring 完整生态体系
## 一、核心基础层
1. **Spring Framework**
Spring 生态底层核心，包含：IOC容器、AOP、事务管理、Spring MVC、核心工具类、事件驱动、资源加载等，所有Spring组件的基石。

2. **Spring Boot**
**约定大于配置**，快速开发脚手架，自动配置、内嵌服务器、依赖管理、一键启动，现代Java后端开发主流。

3. **Spring Cloud**
微服务生态全套解决方案，用于分布式系统开发。

4. **Spring Cloud Alibaba**
阿里开源微服务套件，替代原生Spring Cloud部分组件，国内企业使用更广。

---

## 二、Web & 网络开发
1. **Spring MVC**：传统MVC架构，Tomcat 阻塞式Web框架
2. **Spring WebFlux**：响应式、非阻塞异步Web框架，高并发场景
3. **Spring GraphQL**：GraphQL接口快速开发支持

---

## 三、数据持久化层
1. **Spring Data**
统一数据操作规范，简化CRUD，子组件：
- Spring Data JPA
- Spring Data Redis
- Spring Data MongoDB
- Spring Data Elasticsearch
- Spring Data MySQL/JDBC
- Spring Data Neo4j

2. **Spring JDBC**：原生JDBC模板封装
3. **Spring Transaction**：统一事务管理器（本地事务+分布式事务）

---

## 四、微服务 & 分布式（Spring Cloud 核心组件）
- **Eureka/Nacos**：注册中心
- **Gateway / Zuul**：API网关
- **OpenFeign**：声明式远程调用
- **Ribbon / LoadBalancer**：负载均衡
- **Sentinel / Hystrix**：熔断、降级、限流
- **Config / Nacos配置中心**：分布式配置
- **Bus**：配置动态刷新消息总线
- **Stream**：消息驱动开发（适配RabbitMQ/Kafka）
- **Sleuth + Zipkin**：分布式链路追踪

---

## 五、安全 & 权限
1. **Spring Security**：认证、授权、防跨域、防XSS、OAuth2、JWT
2. **Spring Session**：分布式Session共享

---

## 六、消息 & 异步
1. **Spring AMQP**：RabbitMQ 整合
2. **Spring for Apache Kafka**：Kafka 整合
3. **Spring Task**：定时任务
4. **Spring Async**：异步任务注解支持

---

## 七、测试 & 运维
1. **Spring Test**：Spring专属单元测试、集成测试
2. **Spring Boot Actuator**：服务监控、健康检查、端点暴露
3. **Spring Boot Admin**：可视化服务监控面板

---

## 八、其他扩展组件
1. **Spring Batch**：批处理、大数据量定时批任务
2. **Spring Integration**：企业级集成框架，异构系统对接
3. **Spring Shell**：命令行应用开发
4. **Spring Modulith**：模块化单体架构方案

---

## 九、你技术栈重点适配（Java后端+AI应用）
日常开发必学：
`Spring Framework` + `Spring Boot` + `Spring MVC` + `Spring Data`
分布式必学：
`Spring Cloud Alibaba` + `Gateway` + `Nacos` + `Sentinel`
中间件整合：
`Spring Data Redis`、`Spring ES`、`Spring AMQP`

