Java后端+RabbitMQ企业级项目实战（消息通知系统）
一、项目概述
1.1 项目定位
本项目是一个企业级「分布式消息通知系统」，基于Java后端（SpringBoot）+ RabbitMQ实现，模拟企业中常见的“异步消息推送”场景（如用户注册成功通知、订单状态变更通知、系统告警通知）。
核心目标：掌握RabbitMQ在Java企业级开发中的核心用法（交换机、队列、绑定、消息可靠投递、消费幂等、异常重试），理解分布式系统中异步通信的设计思路，贴合企业实际开发规范（分层架构、配置分离、日志记录、异常处理）。
1.2 技术栈选型（企业主流）
后端框架：SpringBoot 2.7.x（稳定版，企业常用）
消息中间件：RabbitMQ 3.12.x（最新稳定版，支持多种交换机类型）
开发语言：Java 8（企业级开发主流版本）
依赖管理：Maven 3.6.x
数据库：MySQL 8.0（存储消息日志、用户信息，可选）
日志框架：SLF4J + Logback（企业标准日志输出）
工具类：Lombok（简化实体类代码）、FastJSON（JSON序列化）
1.3 核心功能模块
消息生产者模块：负责生成各类通知消息（如注册通知、订单通知），并发送到RabbitMQ交换机
消息消费者模块：监听RabbitMQ队列，消费消息并执行具体业务（如发送短信、推送站内信）
消息可靠性保障：实现消息持久化、生产者确认、消费者确认、异常重试、死信队列（处理失败消息）
日志与监控：记录消息发送/消费日志，便于问题排查（模拟企业监控需求）
接口测试：提供HTTP接口，模拟前端调用发送消息（贴合企业前后端交互场景）
二、环境搭建（企业级规范）
2.1 本地环境准备
2.1.1 RabbitMQ安装与启动
推荐使用Docker安装（企业级部署常用方式，避免环境冲突），步骤如下：
拉取RabbitMQ镜像（带管理界面，便于可视化操作）： docker pull rabbitmq:3.12-management
启动RabbitMQ容器： docker run -d -p 5672:5672 -p 15672:15672 --name rabbitmq -e RABBITMQ_DEFAULT_USER=admin -e RABBITMQ_DEFAULT_PASS=admin rabbitmq:3.12-management
验证启动：访问 http://localhost:15672 ，使用账号admin/密码admin登录（管理界面，可查看交换机、队列、消息等）
说明：5672是RabbitMQ的默认通信端口（Java程序连接用），15672是管理界面端口（可视化操作）。
2.1.2 JDK、Maven、MySQL配置
JDK 8：配置环境变量JAVA_HOME，验证命令：java -version
Maven 3.6.x：配置阿里云镜像（加速依赖下载），修改settings.xml文件
MySQL 8.0：创建数据库（如rabbitmq_notify），后续用于存储消息日志（可选，简化版可跳过）
2.2 SpringBoot项目初始化
2.2.1 项目创建（IDEA）
新建SpringBoot项目，Group：com.example，Artifact：rabbitmq-enterprise，Package：com.example.rabbitmqenterprise
选择依赖：Spring Web（提供HTTP接口）、Spring for RabbitMQ（RabbitMQ集成）、MySQL Driver（可选）、MyBatis-Plus（可选，简化数据库操作）、Lombok
生成项目后，调整pom.xml依赖（确保版本兼容），最终依赖如下：
2.2.2 核心依赖配置（pom.xml）
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.example</groupId>
    <artifactId>rabbitmq-enterprise</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>rabbitmq-enterprise</name>
    <description>RabbitMQ企业级实战项目</description>
    <properties>
        <java.version>1.8</java.version>
    </properties>
    <dependencies>
        <!-- Spring Web 提供HTTP接口 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- RabbitMQ 核心依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <!-- Lombok 简化代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <!-- FastJSON 序列化 -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.83</version>
        </dependency>
        <!-- 日志框架（Spring Boot 默认已集成，无需重复引入） -->
        <!-- <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
        </dependency> -->
        <!-- MySQL + MyBatis-Plus（用于消息日志存储） -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.5.3.1</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <scope>runtime</scope>
        </dependency>
        <!-- 测试依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.amqp</groupId>
            <artifactId>spring-rabbit-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
2.2.3 项目配置文件（application.yml）
采用yml格式（企业常用），分离核心配置，包含RabbitMQ、MySQL（可选）、日志等配置，代码如下：
server:
  port: 8080 # 项目端口
  servlet:
    context-path: /rabbitmq # 上下文路径，企业级项目常用

# Spring 核心配置
spring:

  # RabbitMQ 配置（核心）
  rabbitmq:
    host: localhost # RabbitMQ地址（本地环境），生产环境替换为服务器地址
    port: 5672 # 通信端口
    username: admin # 登录账号
    password: admin # 登录密码
    virtual-host: / # 虚拟主机（默认）

    # 生产者确认配置（消息可靠投递第一步）
    publisher-confirm-type: correlated # 开启生产者确认（异步回调）
    publisher-returns: true # 开启消息返回（当消息无法路由时回调）

    # 消费者配置（消息可靠消费）
    listener:
      simple:
        acknowledge-mode: manual # 手动确认消费（避免消息丢失）
        concurrency: 1 # 消费者并发数（企业级可根据业务调整）
        max-concurrency: 5 # 最大并发数
        retry:
          enabled: true # 开启重试机制
          max-attempts: 3 # 最大重试次数
          initial-interval: 1000 # 首次重试间隔（1秒）
          multiplier: 2 # 重试间隔倍数（每次重试间隔翻倍）

  # MySQL 配置（可选，用于消息日志存储）
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/rabbitmq_notify?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8
    username: root # 数据库账号
    password: 123456 # 数据库密码

# MyBatis-Plus 配置（可选）
mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.example.rabbitmqenterprise.entity
  configuration:
    map-underscore-to-camel-case: true # 下划线转驼峰
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl # 打印SQL日志（开发环境用）

# 日志配置（企业级规范，区分开发/生产环境）
logging:
  level:
    root: info
    com.example.rabbitmqenterprise: debug # 项目包日志级别（开发环境debug，生产环境info）
  file:
    name: logs/rabbitmq-notify.log # 日志文件存储路径
    max-size: 10MB # 单个日志文件最大大小
    max-history: 7 # 日志保留7天
三、项目架构设计（企业级分层）
采用「分层架构」，符合企业开发规范，各层职责清晰，便于维护和扩展，架构如下：
com.example.rabbitmqenterprise
├── config/          # 配置层：RabbitMQ配置、MyBatis配置等
├── controller/      # 控制层：提供HTTP接口，接收前端请求
├── service/         # 业务层：核心业务逻辑（消息发送、消费处理）
│   ├── impl/        # 业务层实现类
├── mapper/          # 数据访问层：操作数据库（可选）
├── entity/          # 实体层：消息实体、用户实体等
├── dto/             # 数据传输对象：接收前端参数、返回结果
├── exception/       # 异常层：自定义异常、全局异常处理
├── util/            # 工具层：通用工具类（JSON、日志等）
└── RabbitmqEnterpriseApplication.java # 项目启动类
3.1 核心包说明
config：核心是RabbitMQ的交换机、队列、绑定关系配置，以及生产者/消费者的自定义配置
service：分为生产者服务（发送消息）和消费者服务（处理消息），业务逻辑与控制层分离
entity：包含消息实体（存储消息内容、状态等）、用户实体（模拟业务场景）
exception：自定义异常（如消息发送失败异常、消费异常），全局异常处理器统一捕获异常
四、核心编码开发（企业级实战）
4.1 实体类开发（entity）
4.1.1 消息实体（MessageLog.java）- 可选（用于消息日志存储）
package com.example.rabbitmqenterprise.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;
/**
 * 消息日志实体（用于记录消息发送/消费状态，便于排查问题）
     */
    @Data
    @TableName("message_log")
    public class MessageLog {
    // 主键（自增）
    @TableId(type = IdType.AUTO)
    private Long id;
    // 消息唯一标识（用于幂等性判断）
    private String messageId;
    // 消息类型（如REGISTER：注册通知，ORDER：订单通知）
    private String messageType;
    // 消息内容（JSON格式）
    private String messageContent;
    // 交换机名称
    private String exchangeName;
    // 队列名称
    private String queueName;
    // 路由键
    private String routingKey;
    // 消息状态（0：待发送，1：发送成功，2：发送失败，3：消费成功，4：消费失败）
    private Integer status;
    // 重试次数
    private Integer retryCount;
    // 创建时间
    private Date createTime;
    // 更新时间
    private Date updateTime;
    }
    4.1.2 消息DTO（MessageDTO.java）- 用于前后端数据传输
    package com.example.rabbitmqenterprise.dto;
    import lombok.Data;
    import javax.validation.constraints.NotBlank;
    import javax.validation.constraints.NotNull;
    /**
 * 消息传输对象（接收前端发送消息的参数）
     */
    @Data
    public class MessageDTO {
    // 消息类型（必传）
    @NotBlank(message = "消息类型不能为空")
    private String messageType;
    // 接收人ID（如用户ID，必传）
    @NotNull(message = "接收人ID不能为空")
    private Long receiverId;
    // 消息内容（必传）
    @NotBlank(message = "消息内容不能为空")
    private String content;
    }
    4.2 RabbitMQ配置（config）
    企业级开发中，RabbitMQ的交换机、队列、绑定关系，均通过代码配置（而非管理界面手动创建），便于部署和版本控制。本项目采用「主题交换机（TopicExchange）」（最灵活，支持通配符路由，企业常用）。
    4.2.1 RabbitMQ核心配置（RabbitMQConfig.java）
    package com.example.rabbitmqenterprise.config;
    import org.springframework.amqp.core.*;
    import org.springframework.amqp.rabbit.connection.ConnectionFactory;
    import org.springframework.amqp.rabbit.core.RabbitTemplate;
    import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
    import org.springframework.amqp.support.converter.MessageConverter;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    /**
 * RabbitMQ 核心配置：交换机、队列、绑定关系、消息转换器、生产者确认等
     */
    @Configuration
    public class RabbitMQConfig {
    // 1. 定义交换机（主题交换机，支持通配符）
    public static final String NOTIFY_EXCHANGE = "notify_topic_exchange";
    // 2. 定义队列（不同类型的消息对应不同队列，解耦）
    // 注册通知队列
    public static final String REGISTER_QUEUE = "register_notify_queue";
    // 订单通知队列
    public static final String ORDER_QUEUE = "order_notify_queue";
    // 死信队列（处理消费失败、重试次数耗尽的消息）
    public static final String DEAD_LETTER_QUEUE = "dead_letter_queue";
    // 死信交换机
    public static final String DEAD_LETTER_EXCHANGE = "dead_letter_exchange";
    // 3. 定义路由键（通配符格式，便于灵活匹配）
    public static final String REGISTER_ROUTING_KEY = "notify.register.#"; // # 匹配任意多级
    public static final String ORDER_ROUTING_KEY = "notify.order.#";
    public static final String DEAD_LETTER_ROUTING_KEY = "dead.letter.#";
    // 4. 创建交换机
    @Bean
    public TopicExchange notifyExchange() {
        // durable=true：交换机持久化（重启RabbitMQ后不丢失）
        // autoDelete=false：不自动删除（交换机无绑定关系时不删除）
        return new TopicExchange(NOTIFY_EXCHANGE, true, false);
    }
    // 5. 创建死信交换机
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DEAD_LETTER_EXCHANGE, true, false);
    }
    // 6. 创建队列（普通队列 + 死信队列）
    @Bean
    public Queue registerQueue() {
        // 给普通队列绑定死信交换机和死信路由键
        return QueueBuilder.durable(REGISTER_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE) // 死信交换机
                .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY) // 死信路由键
                .ttl(60000) // 消息超时时间（60秒，超时未消费则进入死信队列）
                .build();
    }
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
                .ttl(60000)
                .build();
    }
    @Bean
    public Queue deadLetterQueue() {
        // 死信队列无需绑定死信交换机（最终存储失败消息）
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }
    // 7. 绑定队列与交换机（通过路由键）
    @Bean
    public Binding registerBinding() {
        // 注册队列绑定到通知交换机，路由键为notify.register.#
        return BindingBuilder.bind(registerQueue())
                .to(notifyExchange())
                .with(REGISTER_ROUTING_KEY);
    }
    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue())
                .to(notifyExchange())
                .with(ORDER_ROUTING_KEY);
    }
    @Bean
    public Binding deadLetterBinding() {
        // 死信队列绑定到死信交换机
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DEAD_LETTER_ROUTING_KEY);
    }
    // 8. 消息转换器（将Java对象转为JSON，便于传输和解析，企业常用）
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    // 9. 配置RabbitTemplate（生产者发送消息的核心工具）
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // 设置消息转换器
        rabbitTemplate.setMessageConverter(messageConverter());
        // 开启生产者确认（异步回调）
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            // correlationData：消息唯一标识（用于关联消息）
            // ack：true=发送成功，false=发送失败
            // cause：失败原因
            if (ack) {
                // 发送成功，更新消息日志状态（可选）
                System.out.println("消息发送成功，消息ID：" + (correlationData != null ? correlationData.getId() : "无"));
            } else {
                // 发送失败，处理重试或记录日志
                System.err.println("消息发送失败，原因：" + cause + "，消息ID：" + (correlationData != null ? correlationData.getId() : "无"));
            }
        });
        // 开启消息返回（当消息无法路由到队列时回调）
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            System.err.println("消息无法路由，交换机：" + exchange + "，路由键：" + routingKey + "，原因：" + replyText);
        });
        return rabbitTemplate;
    }
    }
    4.3 业务层开发（service）
    业务层分为「生产者服务」和「消费者服务」，分离发送和消费逻辑，符合单一职责原则。
    4.3.1 消息生产者服务（MessageProducerService.java）
    package com.example.rabbitmqenterprise.service;
    import com.alibaba.fastjson.JSON;
    import com.example.rabbitmqenterprise.config.RabbitMQConfig;
    import com.example.rabbitmqenterprise.dto.MessageDTO;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.amqp.rabbit.core.RabbitTemplate;
    import org.springframework.amqp.rabbit.support.CorrelationData;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;
    import java.util.UUID;
    /**
 * 消息生产者服务：负责发送各类通知消息到RabbitMQ
     */
    @Slf4j
    @Service
    public class MessageProducerService {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    /**
     * 发送消息（核心方法）
     * @param messageDTO 消息传输对象
     */
    public void sendMessage(MessageDTO messageDTO) {
        try {
            // 1. 生成消息唯一ID（用于幂等性判断、生产者确认、日志记录）
            String messageId = UUID.randomUUID().toString().replace("-", "");
            // 2. 构建路由键（根据消息类型匹配不同队列）
            String routingKey = buildRoutingKey(messageDTO.getMessageType());
            // 3. 消息内容JSON序列化
            String messageContent = JSON.toJSONString(messageDTO);
            // 4. 构建消息唯一标识（用于生产者确认回调）
            CorrelationData correlationData = new CorrelationData(messageId);
            // 5. 发送消息到交换机
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFY_EXCHANGE, // 交换机名称
                    routingKey, // 路由键
                    messageContent, // 消息内容
                    correlationData // 消息唯一标识
            );
            log.info("消息发送成功，消息ID：{}，消息类型：{}，路由键：{}，消息内容：{}",
                    messageId, messageDTO.getMessageType(), routingKey, messageContent);
            // 6. 记录消息日志（可选，存入MySQL）
            // 此处省略日志记录逻辑，可自行实现（插入message_log表，状态为待发送）
        } catch (Exception e) {
            log.error("消息发送失败，消息内容：{}，异常信息：{}", JSON.toJSONString(messageDTO), e.getMessage(), e);
            // 抛出自定义异常，由全局异常处理器捕获
            throw new RuntimeException("消息发送失败，请稍后重试");
        }
    }
    /**
     * 构建路由键（根据消息类型匹配）
     * @param messageType 消息类型
     * @return 路由键
     */
    private String buildRoutingKey(String messageType) {
        // 匹配RabbitMQConfig中的路由键格式
        switch (messageType) {
            case "REGISTER":
                return "notify.register.user"; // 匹配register路由键（notify.register.#）
            case "ORDER":
                return "notify.order.pay"; // 匹配order路由键（notify.order.#）
            default:
                throw new RuntimeException("未知消息类型：" + messageType);
        }
    }
    }
    4.3.2 消息消费者服务（MessageConsumerService.java）
    消费者通过@RabbitListener注解监听队列，手动确认消费（ack），确保消息可靠消费；同时处理异常重试，重试失败后消息进入死信队列。
    package com.example.rabbitmqenterprise.service;
    import com.alibaba.fastjson.JSON;
    import com.example.rabbitmqenterprise.config.RabbitMQConfig;
    import com.example.rabbitmqenterprise.dto.MessageDTO;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.amqp.core.Message;
    import org.springframework.amqp.rabbit.annotation.RabbitListener;
    import org.springframework.amqp.rabbit.core.RabbitTemplate;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;
    import com.rabbitmq.client.Channel;
    import java.io.IOException;
    /**
 * 消息消费者服务：监听RabbitMQ队列，消费消息并处理业务逻辑
     */
    @Slf4j
    @Service
    public class MessageConsumerService {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    /**
     * 监听注册通知队列
     * @param message 消息对象
     * @param channel 信道（用于手动确认消费）
     * @throws IOException 异常
     */
    @RabbitListener(queues = RabbitMQConfig.REGISTER_QUEUE)
    public void consumeRegisterMessage(Message message, Channel channel) throws IOException {
        // 手动确认消费的核心：获取消息投递标签（唯一标识当前消息）
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // 1. 解析消息内容（JSON转DTO）
            String messageContent = new String(message.getBody(), "UTF-8");
            MessageDTO messageDTO = JSON.parseObject(messageContent, MessageDTO.class);
            log.info("消费注册通知消息，消息内容：{}，投递标签：{}", messageContent, deliveryTag);
            // 2. 执行核心业务逻辑（如发送注册成功短信、推送站内信）
            // 模拟业务处理（企业中此处会调用短信服务、站内信服务）
            handleRegisterNotify(messageDTO);
            // 3. 手动确认消费（ack=true：确认消费，消息从队列中删除）
            channel.basicAck(deliveryTag, false);
            log.info("注册通知消息消费成功，消息ID：{}", message.getMessageProperties().getCorrelationId());
            // 4. 更新消息日志状态（可选，改为消费成功）
        } catch (Exception e) {
            log.error("注册通知消息消费失败，投递标签：{}，异常信息：{}", deliveryTag, e.getMessage(), e);
            // 处理消费异常：重试次数耗尽后，拒绝消费，消息进入死信队列
            // basicNack：拒绝消费，requeue=false：不重新入队（进入死信队列）
            channel.basicNack(deliveryTag, false, false);
            // 记录消费失败日志（可选）
        }
    }
    /**
     * 监听订单通知队列
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void consumeOrderMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            String messageContent = new String(message.getBody(), "UTF-8");
            MessageDTO messageDTO = JSON.parseObject(messageContent, MessageDTO.class);
            log.info("消费订单通知消息，消息内容：{}，投递标签：{}", messageContent, deliveryTag);
            // 模拟订单通知业务处理（如订单支付成功通知、订单取消通知）
            handleOrderNotify(messageDTO);
            channel.basicAck(deliveryTag, false);
            log.info("订单通知消息消费成功，消息ID：{}", message.getMessageProperties().getCorrelationId());
        } catch (Exception e) {
            log.error("订单通知消息消费失败，投递标签：{}，异常信息：{}", deliveryTag, e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
    /**
     * 监听死信队列（处理失败消息，企业中可人工介入处理）
     */
    @RabbitListener(queues = RabbitMQConfig.DEAD_LETTER_QUEUE)
    public void consumeDeadLetterMessage(Message message) {
        String messageContent = new String(message.getBody(), "UTF-8");
        log.error("死信队列消息，消费失败的消息内容：{}，消息ID：{}",
                messageContent, message.getMessageProperties().getCorrelationId());
        // 企业中此处可做：记录死信日志、发送告警通知（如邮件、钉钉）、人工处理
    }
    /**
     * 处理注册通知业务
     */
    private void handleRegisterNotify(MessageDTO messageDTO) {
        // 模拟发送短信：接收人ID（messageDTO.getReceiverId()），消息内容（messageDTO.getContent()）
        log.info("给用户ID：{} 发送注册通知，内容：{}", messageDTO.getReceiverId(), messageDTO.getContent());
        // 实际开发中：调用第三方短信API（如阿里云短信、腾讯云短信）
    }
    /**
     * 处理订单通知业务
     */
    private void handleOrderNotify(MessageDTO messageDTO) {
        log.info("给用户ID：{} 发送订单通知，内容：{}", messageDTO.getReceiverId(), messageDTO.getContent());
        // 实际开发中：调用站内信服务，推送订单状态变更消息
    }
    }
    4.4 控制层开发（controller）
    提供HTTP接口，模拟前端调用，发送消息，支持参数校验（企业级接口规范）。
    package com.example.rabbitmqenterprise.controller;
    import com.example.rabbitmqenterprise.dto.MessageDTO;
    import com.example.rabbitmqenterprise.service.MessageProducerService;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.validation.annotation.Validated;
    import org.springframework.web.bind.annotation.PostMapping;
    import org.springframework.web.bind.annotation.RequestBody;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RestController;
    /**
 * 消息通知控制层：提供HTTP接口，接收前端请求，调用生产者服务发送消息
     */
    @Slf4j
    @RestController
    @RequestMapping("/api/notify")
    public class NotifyController {
    @Autowired
    private MessageProducerService messageProducerService;
    /**
     * 发送通知消息
     * @param messageDTO 消息参数（需校验）
     * @return 响应结果
     */
    @PostMapping("/send")
    public ResponseEntity<String> sendNotify(@Validated @RequestBody MessageDTO messageDTO) {
        try {
            // 调用生产者服务发送消息
            messageProducerService.sendMessage(messageDTO);
            return ResponseEntity.ok("消息发送成功");
        } catch (Exception e) {
            log.error("发送消息接口异常，参数：{}，异常信息：{}", messageDTO, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("消息发送失败：" + e.getMessage());
        }
    }
    }
    4.5 异常处理（exception）
    企业级开发中，需统一异常处理，避免返回杂乱的异常信息，提升接口友好性。
    4.5.1 自定义异常（BusinessException.java）
    package com.example.rabbitmqenterprise.exception;
    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.NoArgsConstructor;
    /**
 * 自定义业务异常（用于业务逻辑中的异常场景）
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class BusinessException extends RuntimeException {
    // 异常码（企业级可扩展，用于前端判断异常类型）
    private Integer code;
    // 异常信息
    private String message;
    // 简化构造方法
    public BusinessException(String message) {
        this.message = message;
        this.code = 500; // 默认异常码（服务器内部错误）
    }
    }
    4.5.2 全局异常处理器（GlobalExceptionHandler.java）
    package com.example.rabbitmqenterprise.exception;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.validation.BindingResult;
    import org.springframework.validation.FieldError;
    import org.springframework.web.bind.MethodArgumentNotValidException;
    import org.springframework.web.bind.annotation.ExceptionHandler;
    import org.springframework.web.bind.annotation.RestControllerAdvice;
    import java.util.HashMap;
    import java.util.Map;
    /**
 * 全局异常处理器：统一捕获所有控制器异常，返回标准化响应
     */
    @Slf4j
    @RestControllerAdvice
    public class GlobalExceptionHandler {
    // 处理参数校验异常（如@NotBlank、@NotNull）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        Map<String, String> errorMap = new HashMap<>();
        // 遍历所有校验失败的字段
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errorMap.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.error("参数校验异常：{}", errorMap);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMap);
    }
    // 处理自定义业务异常
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusinessException(BusinessException e) {
        Map<String, String&gt; errorMap = new HashMap<>();
        errorMap.put("code", e.getCode().toString());
        errorMap.put("message", e.getMessage());
        log.error("业务异常：{}", errorMap);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMap);
    }
    // 处理其他未知异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        Map&lt;String, String&gt; errorMap = new HashMap<>();
        errorMap.put("code", "500");
        errorMap.put("message", "服务器内部错误，请联系管理员");
        log.error("未知异常：{}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMap);
    }
    }
    4.6 工具类（util）- 可选
    封装通用工具类，如JSON工具、日志工具等，简化代码。
    package com.example.rabbitmqenterprise.util;
    import com.alibaba.fastjson.JSON;
    import com.alibaba.fastjson.TypeReference;
    /**
 * JSON工具类（封装FastJSON，简化序列化/反序列化）
     */
    public class JsonUtil {
    // 序列化：Java对象转JSON字符串
    public static <T> String toJson(T obj) {
        return JSON.toJSONString(obj);
    }
    // 反序列化：JSON字符串转Java对象
    public static <T> T fromJson(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }
    // 反序列化：复杂类型（如List、Map）
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        return JSON.parseObject(json, typeReference);
    }
    }
    4.7 项目启动类
    package com.example.rabbitmqenterprise;
    import org.mybatis.spring.annotation.MapperScan;
    import org.springframework.amqp.rabbit.annotation.EnableRabbit;
    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;
    /**
 * 项目启动类
 * EnableRabbit：开启RabbitMQ注解支持（@RabbitListener）
 * MapperScan：扫描MyBatis的mapper接口（可选，用于消息日志存储）
     */
    @SpringBootApplication
    @EnableRabbit
    @MapperScan("com.example.rabbitmqenterprise.mapper")
    public class RabbitmqEnterpriseApplication {
    public static void main(String[] args) {
        SpringApplication.run(RabbitmqEnterpriseApplication.class, args);
        System.out.println("RabbitMQ企业级项目启动成功！访问地址：http://localhost:8080/rabbitmq");
    }
    }
    五、项目测试（企业级流程）
    测试分为「接口测试」「消息发送/消费测试」「异常场景测试」，模拟企业实际测试流程。
    5.1 环境启动
    启动RabbitMQ容器（确保5672、15672端口正常）
    启动MySQL（可选），创建rabbitmq_notify数据库（若使用消息日志）
    启动SpringBoot项目，查看控制台是否有启动成功日志
    5.2 接口测试（Postman）
    调用控制层提供的HTTP接口，发送消息，测试生产者功能。
    请求地址：http://localhost:8080/rabbitmq/api/notify/send
    请求方式：POST
    请求体（JSON）： { "messageType": "REGISTER", "receiverId": 1001, "content": "恭喜您注册成功，欢迎加入我们！" }
    响应结果：{"status":200,"body":"消息发送成功"}
    5.3 消息发送/消费验证
    查看项目控制台日志，是否有“消息发送成功”“消费注册通知消息”的日志
    访问RabbitMQ管理界面（http://localhost:15672）：
    Exchanges：查看notify_topic_exchange是否存在，绑定关系是否正确
    Queues：查看register_notify_queue的消息数（发送后消息数为0，因为已被消费）
    Dead Letter Queue：正常消费时，死信队列消息数为0
    5.4 异常场景测试（企业级重点）
    消费失败测试：修改consumeRegisterMessage方法，手动抛出异常（模拟业务处理失败），重启项目后发送消息，查看日志：
    消息会重试3次（配置中max-attempts=3）
    重试失败后，消息进入死信队列（dead_letter_queue）
    查看管理界面，死信队列会有1条消息
    参数校验测试：发送消息时，不传递messageType或receiverId，查看接口响应是否返回参数校验错误
    生产者发送失败测试：关闭RabbitMQ，发送消息，查看控制台是否有“消息发送失败”的日志，且全局异常处理器返回对应错误信息
    六、企业级优化点（拓展）
    本项目已覆盖RabbitMQ企业级核心用法，可进一步优化以下点，贴合实际生产环境：
    消息幂等性：使用messageId作为唯一标识，消费前判断该消息是否已消费（避免重复消费，如网络重试导致的重复消息）
    消息日志完善：将消息发送/消费状态存入MySQL，便于后续排查问题、统计消息成功率
    监控告警：集成Prometheus + Grafana，监控RabbitMQ的消息堆积、消费速率、失败率，设置告警阈值（如消息堆积超过100条发送钉钉告警）
    分布式部署：将生产者和消费者分离部署（微服务架构），避免单点故障
    消息加密：敏感消息（如用户手机号、订单金额）传输前加密，消费后解密，保障数据安全
    动态配置：使用Nacos/Apollo配置中心，动态调整RabbitMQ的并发数、重试次数等参数，无需重启项目
    七、项目总结
    本项目完整模拟了Java后端+RabbitMQ的企业级开发流程，从环境搭建、架构设计、编码开发到测试部署，覆盖了RabbitMQ的核心用法：
    交换机、队列、绑定关系的代码配置（企业级规范，避免手动操作）
    消息可靠投递（生产者确认、消息持久化）
    消息可靠消费（手动确认、异常重试、死信队列）
    企业级分层架构、异常处理、日志记录、参数校验
    通过本项目，可快速掌握RabbitMQ在Java企业级开发中的实际应用，理解异步通信的设计思路，为后续微服务架构中的消息中间件使用打下基础。
