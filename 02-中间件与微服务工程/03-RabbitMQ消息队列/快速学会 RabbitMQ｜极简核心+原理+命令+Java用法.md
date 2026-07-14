快速学会 RabbitMQ｜极简核心+原理+命令+Java用法
一、RabbitMQ 是什么
- 一款开源消息队列（MQ），基于 AMQP 协议，Erlang 开发
- 核心作用：异步解耦、削峰限流、可靠投递、业务削峰
- 常见场景：秒杀、日志推送、邮件短信、微服务异步通信、订单延迟处理
 
二、五大核心概念（必背）
1. Producer 生产者
    发送消息的一方，把消息发给交换机。
2. Consumer 消费者
    监听队列，被动接收、处理消息。
3. Exchange 交换机
    不存消息，只负责：路由转发消息到对应队列。
4. Queue 队列
    持久化存储消息，等待消费者消费，消息最终都在队列。
5. Binding 绑定
    交换机 和 队列 之间的绑定关系，决定路由规则。
    流转链路：
     生产者 → 交换机Exchange → 绑定Binding → 队列Queue → 消费者 
 
三、四种交换机类型（面试必考）
1. Direct【直连】
    - 精准匹配： routingKey = bindingKey 
    - 一对一，点对点
    - 场景：单业务精准推送、单消费者
2. Topic【主题】⭐最常用
    - 模糊匹配：
    -  *  匹配单个词
    -  #  匹配多个词
    - 场景：日志分类、多模块订阅、复杂业务路由
3. Fanout【广播】
    - 无视 routingKey，全队列广播
    - 一对多，所有绑定队列全部收到
    - 场景：消息群发、集群通知、缓存刷新
4. Headers【头模式】
    - 基于请求头匹配，几乎不用，了解即可
 
四、核心优势
1. 解耦：上下游服务不用同步调用
2. 削峰：秒杀高并发流量缓冲到队列
3. 异步：非核心业务异步化，提升接口速度
4. 可靠：支持消息持久化、死信、重试、防丢失
 
五、Docker 一键部署（直接复制）
bash

# 带控制台、账号密码
docker run -d \
--name rabbitmq \
-p 5672:5672 \
-p 15672:15672 \
-e RABBITMQ_DEFAULT_USER=admin \
-e RABBITMQ_DEFAULT_PASS=123456 \
--restart always \
rabbitmq:3-management
 
- 访问控制台： http://localhost:15672 
- 账号： admin 
- 密码： 123456 
- 程序连接端口： 5672 
 
六、消息可靠机制（重点）
1. 消息持久化
    队列持久化 + 消息持久化，重启不丢消息
2. 生产者确认（Confirm）
    确保消息成功抵达交换机
3. 手动ACK
    消费者处理完业务，手动签收，处理失败可重回队列
4. 死信队列 DLQ
    超时、拒收、重试失败消息转入死信，避免死循环
 
七、Java 核心使用（SpringBoot 主流）
1. 依赖
    xml
    <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>
 
2. 配置
    yaml
    spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: 123456
 
3. 核心注解
    -  @Configuration  配置交换机、队列、绑定
    -  @RabbitListener  消费者监听队列
    -  rabbitTemplate.convertAndSend()  生产者发消息
 
八、常见问题
1. 消息丢失？
    开启：持久化、生产者确认、手动ACK
2. 消息重复消费？
    业务做幂等（唯一key去重）
3. 消息积压？
    增加消费者数量、优化消费速度、拆分队列
 
九、最简记忆口诀
生产发交换，交换绑队列；
直连精准配，主题模糊追；
广播全推送，消息可持久；
异步解耦削高峰，微服务必备。
