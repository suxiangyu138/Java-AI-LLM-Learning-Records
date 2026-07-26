# 附录C-组件对比速查表
> 🎯 Spring生态中易混淆组件的全方位对比，帮助快速决策技术选型

---

## 目录
1. [IoC容器：BeanFactory vs ApplicationContext](#1-ioc容器beanfactory-vs-applicationcontext)
2. [代理：JDK动态代理 vs CGLIB](#2-代理jdk动态代理-vs-cglib)
3. [Web层：Spring MVC vs WebFlux](#3-web层spring-mvc-vs-webflux)
4. [过滤器：Filter vs HandlerInterceptor](#4-过滤器filter-vs-handlerinterceptor)
5. [数据访问：JPA vs MyBatis vs JDBC Template](#5-数据访问jpa-vs-mybatis-vs-jdbc-template)
6. [注册中心：Nacos vs Eureka vs Consul vs ZooKeeper](#6-注册中心nacos-vs-eureka-vs-consul-vs-zookeeper)
7. [配置中心：Nacos Config vs Apollo vs Spring Cloud Config](#7-配置中心nacos-config-vs-apollo-vs-spring-cloud-config)
8. [服务调用：OpenFeign vs RestTemplate vs Dubbo vs gRPC](#8-服务调用openfeign-vs-resttemplate-vs-dubbo-vs-grpc)
9. [熔断限流：Sentinel vs Hystrix vs Resilience4j](#9-熔断限流sentinel-vs-hystrix-vs-resilience4j)
10. [网关：Spring Cloud Gateway vs Zuul vs Nginx](#10-网关spring-cloud-gateway-vs-zuul-vs-nginx)
11. [消息队列：RabbitMQ vs RocketMQ vs Kafka](#11-消息队列rabbitmq-vs-rocketmq-vs-kafka)
12. [分布式事务：Seata AT vs TCC vs Saga vs XA](#12-分布式事务seata-at-vs-tcc-vs-saga-vs-xa)
13. [容器：Tomcat vs Jetty vs Undertow](#13-容器tomcat-vs-jetty-vs-undertow)
14. [链路追踪：SkyWalking vs Zipkin vs Jaeger](#14-链路追踪skywalking-vs-zipkin-vs-jaeger)
15. [分布式锁：Redis(Redisson) vs ZooKeeper vs 数据库](#15-分布式锁redisredisson-vs-zookeeper-vs-数据库)
16. [认证方案：JWT vs OAuth2 vs Session](#16-认证方案jwt-vs-oauth2-vs-session)

---

## 1. IoC容器：BeanFactory vs ApplicationContext

| 维度 | BeanFactory | ApplicationContext |
|------|------------|-------------------|
| 定位 | IoC底层接口 | 企业级IoC容器 |
| Bean加载 | **延迟加载**（使用时才创建） | **预加载**（启动时初始化所有单例Bean） |
| 国际化 | ❌ | ✅ MessageSource |
| 事件发布 | ❌ | ✅ ApplicationEventPublisher |
| 资源加载 | ❌ | ✅ ResourceLoader |
| 环境配置 | ❌ | ✅ Environment |
| AOP支持 | ❌ | ✅ |
| 使用场景 | 资源受限环境（如Applet） | **99%的生产场景** |

> 🎯 **结论**：永远使用 ApplicationContext（SpringBoot自动创建），无需关心BeanFactory。

---

## 2. 代理：JDK动态代理 vs CGLIB

| 维度 | JDK动态代理 | CGLIB |
|------|-----------|-------|
| 原理 | 基于**接口**反射，生成`$Proxy`类 | 基于**继承**，生成子类 |
| 要求 | 必须有接口 | 类和方法不能是final |
| 创建速度 | ✅ 快 | ⚠️ 慢（需生成字节码） |
| 执行速度 | ⚠️ 略慢（反射） | ✅ 快（直接调用） |
| Spring默认 | 有接口时使用 | 无接口时使用 |
| SpringBoot 3.x | — | ✅ **默认CGLIB** |

---

## 3. Web层：Spring MVC vs WebFlux

| 维度 | Spring MVC | Spring WebFlux |
|------|-----------|---------------|
| 编程模型 | **同步阻塞**（Servlet 3.1+） | **异步非阻塞**（Reactive Streams） |
| 容器 | Tomcat/Jetty（Servlet） | Netty/Undertow（非Servlet） |
| 线程模型 | 每请求一线程 | 少量线程 + Event Loop |
| 数据库 | JDBC/JPA（成熟） | R2DBC（生态待完善） |
| 适用场景 | **传统CRUD业务** | 高并发网关、流式处理、实时数据 |
| 学习曲线 | ✅ 低 | ⚠️ 高 |
| 市场占比 | **95%+** | 5% |

> 🎯 **结论**：没有特殊需求就用Spring MVC，WebFlux只在网关或极高并发场景使用。

---

## 4. 过滤器：Filter vs HandlerInterceptor

| 维度 | Filter | HandlerInterceptor |
|------|--------|-------------------|
| 规范 | Servlet规范 | Spring MVC |
| 容器管理 | Servlet容器（Tomcat） | Spring IOC容器 |
| 能否注入Bean | ❌ 不能（需通过ApplicationContext获取） | ✅ 可以直接注入 |
| 拦截范围 | **所有请求**（包括静态资源） | 仅进入DispatcherServlet的请求 |
| 生命周期方法 | init → doFilter → destroy | preHandle → postHandle → afterCompletion |
| 适用场景 | 编码设置、XSS过滤、CORS | 登录校验、权限控制、日志记录 |

---

## 5. 数据访问：JPA vs MyBatis vs JDBC Template

| 维度 | Spring Data JPA | MyBatis/MyBatis-Plus | JdbcTemplate |
|------|----------------|---------------------|--------------|
| SQL控制力 | ⚠️ 自动生成（JPQL可控） | ✅ **完全手写SQL** | ✅ 完全手写SQL |
| 开发效率 | ✅✅✅ 高（方法命名自动查询） | ✅✅ 中高（MyBatis-Plus代码生成） | ⚠️ 低 |
| 复杂查询 | ⚠️ 弱（需@Query补救） | ✅✅ **强**（动态SQL） | ✅ 强 |
| ORM映射 | ✅ 完整 | ⚠️ 需手动映射 | ❌ 需手动映射 |
| 学习成本 | ⚠️ 中高 | ✅ 低 | ✅ 低 |
| 适用场景 | 快速开发、标准化CRUD | 复杂SQL、报表系统、中国互联网 | 简单项目 |

---

## 6. 注册中心：Nacos vs Eureka vs Consul vs ZooKeeper

| 维度 | Nacos | Eureka | Consul | ZooKeeper |
|------|-------|--------|--------|-----------|
| CAP模型 | **AP/CP可切换** | AP | CP | CP |
| 一致性协议 | Distro(AP) + Raft(CP) | — | Raft | ZAB |
| 健康检查 | TCP/HTTP/MySQL | HTTP心跳 | TCP/HTTP/gRPC | TCP/KeepAlive |
| 配置中心 | ✅ **内置** | ❌ | ✅ KV存储 | ❌ |
| 雪崩保护 | ✅ | ✅ | ❌ | ❌ |
| 多数据中心 | ✅ | ✅ | ✅ | ❌ |
| 维护状态 | ✅ **活跃** | ⚠️ 2.x停更 | ✅ 活跃 | ✅ 活跃 |
| Spring Cloud集成 | ✅ **第一选择** | ✅ | ✅ | ✅ |

> 🎯 **结论**：新项目一律选 Nacos（同时解决注册+配置），Eureka仅用于维护老项目。

---

## 7. 配置中心：Nacos Config vs Apollo vs Spring Cloud Config

| 维度 | Nacos Config | Apollo | Spring Cloud Config |
|------|-------------|--------|-------------------|
| 配置格式 | YAML/Properties/JSON/XML | Properties/XML/JSON/YML | YAML/Properties |
| 动态刷新 | ✅ | ✅ | ⚠️ 需+bus+MQ |
| 配置灰度 | ✅ | ✅ | ❌ 弱 |
| 权限管理 | ✅ | ✅ 完善 | ⚠️ 弱 |
| 部署复杂度 | ✅ 低 | ⚠️ 中 | ✅ 低 |
| 生态整合 | ✅ Nacos注册中心一体 | ✅ 携程 | ⚠️ 需额外组件 |
| 适用场景 | 中小型团队 | 大型企业 | 简单配置管理 |

> 🎯 **结论**：Nacos统一注册+配置最佳；Apollo适合大企业复杂权限场景。

---

## 8. 服务调用：OpenFeign vs RestTemplate vs Dubbo vs gRPC

| 维度 | OpenFeign | RestTemplate | Dubbo | gRPC |
|------|----------|-------------|-------|------|
| 协议 | HTTP | HTTP | TCP（自定义协议） | HTTP/2 |
| 序列化 | JSON | JSON | Hessian2/Protobuf | **Protobuf** |
| 性能 | ⚠️ 中 | ⚠️ 中 | ✅ 高 | ✅✅ 最高 |
| 跨语言 | ✅ | ✅ | ⚠️ 弱 | ✅ |
| 声明式 | ✅ 接口+注解 | ❌ 手动编码 | ✅ 接口 | ✅ |
| 服务治理 | Spring Cloud | Spring Cloud | ✅ 完善 | ⚠️ 需额外 |
| 学习曲线 | ✅ 低 | ✅ 低 | ⚠️ 中 | ⚠️ 中高 |

> 🎯 **结论**：Spring Cloud生态首选 OpenFeign；高性能内部RPC选 Dubbo；跨语言微服务选 gRPC。

---

## 9. 熔断限流：Sentinel vs Hystrix vs Resilience4j

| 维度 | Sentinel | Hystrix | Resilience4j |
|------|---------|---------|-------------|
| 熔断策略 | 慢调用比例/异常比例/异常数 | 异常比例 | 异常比例/慢调用 |
| 限流 | ✅ **丰富**（QPS/线程/热点/系统） | ⚠️ 信号量 | ✅ RateLimiter |
| 控制台 | ✅ 可视化Dashboard | ✅ Dashboard | ❌ 无（需自建） |
| 规则持久化 | ✅ Nacos/Apollo等 | ❌ | ⚠️ 需编码 |
| 维护状态 | ✅ **活跃**（阿里） | ❌ **停止维护** | ✅ 活跃 |
| 适用场景 | 国内微服务首选 | 老项目 | 函数式编程 |

> 🎯 **结论**：一律选 Sentinel，Hystrix已进入维护模式。

---

## 10. 网关：Spring Cloud Gateway vs Zuul vs Nginx

| 维度 | Spring Cloud Gateway | Zuul 1.x | Nginx |
|------|---------------------|---------|-------|
| 架构 | **异步非阻塞**（Netty） | 同步阻塞（Servlet） | 异步非阻塞 |
| 性能 | ✅✅ 高 | ⚠️ 中 | ✅✅✅ 最高 |
| 协议支持 | HTTP/WebSocket | HTTP | HTTP/HTTPS/TCP/UDP |
| 动态路由 | ✅ | ⚠️ 弱 | ⚠️ 需OpenResty |
| Spring整合 | ✅ **原生** | ✅ | ⚠️ 需Lua/Java |
| 限流 | ✅ RequestRateLimiter | ❌ | ✅ limit_req |
| 适用场景 | **Spring微服务入口网关** | 老项目 | 边缘网关、静态资源 |

> 🎯 **结论**：Nginx做最外层的边缘网关；Gateway做微服务API网关。

---

## 11. 消息队列：RabbitMQ vs RocketMQ vs Kafka

| 维度 | RabbitMQ | RocketMQ | Kafka |
|------|---------|---------|-------|
| 吞吐量 | 万级 | **十万级** | **百万级** |
| 延迟 | 微秒级 | 毫秒级 | 毫秒级 |
| 事务消息 | ❌ | ✅ **支持** | ❌ |
| 顺序消息 | ⚠️ 单队列有序 | ✅ 支持 | ⚠️ 单分区有序 |
| 延时消息 | ✅ DLX+TTL | ✅ **内置18级** | ❌ |
| 消息回溯 | ❌ | ✅ 按时间 | ✅ **按offset** |
| 社区生态 | ✅ | ⚠️ 国内为主 | ✅ **大数据标配** |
| 适用场景 | 中小型、复杂路由 | 电商/金融、事务消息 | 日志采集、流处理 |

> 🎯 **结论**：一般业务选 RabbitMQ；阿里生态/分布式事务选 RocketMQ；大数据/日志选 Kafka。

---

## 12. 分布式事务：Seata AT vs TCC vs Saga vs XA

| 维度 | Seata AT | TCC | Saga | XA |
|------|---------|-----|------|-----|
| 一致性 | 最终一致 | 最终一致 | 最终一致 | **强一致** |
| 性能 | ✅ 高 | ✅ 高 | ✅ 高 | ❌ 极低 |
| 侵入性 | ✅ **无侵入** | ⚠️ 高（需实现Try/Confirm/Cancel） | ⚠️ 中 | ✅ 无 |
| 回滚方式 | 自动（undo_log） | 手动编码 | 正向补偿 | 自动 |
| 适用场景 | 大部分业务 | 资金/核心高一致性 | 长流程/多步骤 | 低并发、强一致 |

---

## 13. 容器：Tomcat vs Jetty vs Undertow

| 维度 | Tomcat | Jetty | Undertow |
|------|--------|-------|----------|
| 性能 | ⚠️ 中 | ✅ 中高 | ✅✅ **最高** |
| 内存占用 | ⚠️ 中 | ✅ 低 | ✅ **最低** |
| 成熟度 | ✅✅✅ | ✅✅ | ✅ |
| Servlet支持 | ✅ 完善 | ✅ | ✅ |
| 适用场景 | 通用 | 云原生、快速启动 | 高并发 |

---

## 14. 链路追踪：SkyWalking vs Zipkin vs Jaeger

| 维度 | SkyWalking | Zipkin | Jaeger |
|------|-----------|--------|--------|
| 存储 | ES/H2/MySQL | ES/MySQL/Cassandra | ES/Cassandra |
| 指标监控 | ✅ **丰富** | ⚠️ 弱 | ✅ |
| JVM监控 | ✅ 内置 | ❌ | ❌ |
| 性能开销 | ✅ 低（字节码增强） | ⚠️ 中 | ⚠️ 中 |
| 中文社区 | ✅✅ **活跃** | ⚠️ 弱 | ⚠️ 弱 |

> 🎯 **结论**：国内首选 SkyWalking。

---

## 15. 分布式锁：Redis(Redisson) vs ZooKeeper vs 数据库

| 维度 | Redis(Redisson) | ZooKeeper | 数据库 |
|------|----------------|-----------|--------|
| 性能 | ✅✅ **最高** | ⚠️ 中 | ❌ 最低 |
| 可靠性 | ⚠️ 中（单点需集群） | ✅ **高（CP）** | ✅ 高 |
| 实现复杂度 | ✅ **低**（Redisson封装） | ⚠️ 中 | ⚠️ 中 |
| 自动续期 | ✅ Watch Dog | ⚠️ 临时节点 | ❌ |

> 🎯 **结论**：绝大多数场景选 Redis + Redisson。

---

## 16. 认证方案：JWT vs OAuth2 vs Session

| 维度 | JWT | OAuth2 | Session |
|------|-----|--------|---------|
| 状态 | **无状态** | 无状态 | 有状态 |
| 服务端存储 | ❌ 不需要 | ❌ 不需要（Token） | ✅ 需要 |
| 跨域 | ✅ 天然支持 | ✅ | ⚠️ 需要处理 |
| 注销/吊销 | ⚠️ 困难 | ✅ | ✅ 简单 |
| 适用场景 | 微服务API | 第三方授权 | 传统Web应用 |

> 🎯 **结论**：微服务API选 JWT；第三方登录选 OAuth2；传统Web选 Session。

---

> 🎯 **使用建议**：技术选型时参考本表，结合团队技术栈和业务需求综合决策。
