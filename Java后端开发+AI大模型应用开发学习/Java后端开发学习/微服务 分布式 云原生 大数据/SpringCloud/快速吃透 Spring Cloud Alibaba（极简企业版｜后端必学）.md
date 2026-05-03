04.27 11:22
快速吃透 Spring Cloud Alibaba（极简企业版｜后端必学）
一、核心认知
1. 什么是微服务
单体项目：所有代码写在一个项目，耦合高、难扩容、难迭代
微服务：按业务拆分（用户服务、订单服务、商品服务），独立部署、独立扩容、团队协作开发
2. Spring Cloud Alibaba 是什么
- 阿里开源微服务全家桶，国内企业主流标配
- 替代原生 Spring Cloud，生态更强、适配国产技术、文档友好
- 核心：一站式解决微服务 注册、配置、限流、熔断、网关、调用
3. 核心组件一览（必学6件套）
1. Nacos：注册中心 + 配置中心（核心重中之重）
2. Sentinel：流量控制、熔断降级、系统防护
3. Dubbo：高性能RPC远程调用（替代Feign）
4. Spring Cloud Gateway：统一网关
5. Seata：分布式事务
6. OSS/SMS：阿里云云服务快速集成
 
二、环境前置依赖
1. 核心版本匹配（避免踩坑）
- JDK 8+
- SpringBoot 2.7.x / 3.x
- Spring Cloud Alibaba 2021.0.1.0 主流稳定版
2. 父工程统一依赖管理
xml
<!-- 统一版本锁定，无需每个模块写版本号 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>2021.0.1.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
 
 
三、核心组件极速上手（代码+作用）
1. Nacos 注册中心（服务注册&发现）
作用
- 所有微服务启动后注册到 Nacos
- 服务之间互相发现，实现远程调用
1）引入依赖
xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
 
2）yml 配置
yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
  application:
    name: user-service # 服务名，注册到nacos
 
3）启动类注解
java
@SpringBootApplication
@EnableDiscoveryClient // 开启服务注册发现
public class UserApplication {
}
 
 
2. Nacos 配置中心（统一配置管理）
作用
- 集中管理所有微服务配置
- 动态刷新配置，不用重启项目
1）依赖
xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
 
2）bootstrap.yml 优先级高于 application
yaml
spring:
  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        file-extension: yml
 
3）动态刷新
java
@RefreshScope // 配置自动刷新
@RestController
public class ConfigController {
}
 
 
3. Dubbo 远程调用（微服务之间通信）
作用
- 高性能RPC调用，比OpenFeign更快
- 接口直接调用，像调用本地方法一样调远程服务
核心流程
1. 服务提供者：定义接口 + @DubboService
2. 服务消费者：@DubboReference 注入远程接口
 
4. Sentinel 熔断限流
作用
- 防止服务雪崩
- 接口限流、异常熔断、降级兜底
- 秒杀、高并发项目必备
核心：流量控制 + 熔断降级 + 热点参数限流
 
5. Gateway 统一网关
作用
- 统一入口，所有请求先经过网关
- 路由转发、鉴权、跨域、限流、负载均衡
- 屏蔽内部服务地址，安全隔离
 
6. Seata 分布式事务
作用
跨服务操作数据库（下单+扣库存+扣余额）
保证 要么全部成功，要么全部回滚，解决分布式数据一致性
 
四、微服务完整调用链路（必背）
plaintext
客户端请求
→ Gateway 网关（拦截、路由、鉴权）
→ Nacos 找到目标服务地址
→ Dubbo/HTTP 远程调用
→ 业务服务执行
→ Sentinel 熔断降级兜底
 
 
五、面试/项目 高频核心概念
1. 注册中心：Nacos 保存服务列表，心跳检测健康状态
2. 配置中心：统一配置、环境隔离、动态发布
3. 负载均衡：多实例服务自动分发请求
4. 服务熔断：下游服务故障，快速失败，避免连锁崩溃
5. 服务降级：高峰期关闭非核心接口，保证核心业务
6. 分布式事务：Seata AT 模式 主流方案
 
六、最简学习路线（2~3天吃透能用）
1. 本地部署 Nacos（一键启动）
2. 搭建 2个微服务：生产者 + 消费者
3. 实现：Nacos注册 + 配置共享 + Dubbo调用
4. 接入 Gateway 网关统一入口
5. 接入 Sentinel 做简单限流熔断
6. 了解 Seata 分布式事务使用场景
 
七、一句话总结
1. Spring Cloud Alibaba = 阿里微服务全家桶
2. 核心基石：Nacos（注册+配置）
3. 通信：Dubbo
4. 防护：Sentinel
5. 入口：Gateway
6. 数据一致：Seata

