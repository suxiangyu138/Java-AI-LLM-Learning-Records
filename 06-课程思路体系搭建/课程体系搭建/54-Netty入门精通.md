# Netty入门精通

> 基于NIO网络编程的Netty框架从入门到源码实战，涵盖NIO基础、Netty核心API、粘包半包与协议设计、聊天室案例、性能优化与RPC手写实战、核心源码逐行分析。

## 第1章 Java NIO基础（Netty前置知识）

- `001` nio三大组件-channel-buffer
- `002` nio三大组件-服务器设计-多线程版
- `003` nio三大组件-服务器设计-线程池版
- `004` nio三大组件-服务器设计-selector版
- `005` bytebuffer-基本使用
- `006` bytebuffer-内部结构
- `007` bytebuffer-方法演示1
- `008` bytebuffer-方法演示2
- `009` bytebuffer-方法演示3
- `010` bytebuffer-方法演示4
- `011` bytebuffer-分散读集中写
- `012` bytebuffer-黏包半包分析
- `013` bytebuffer-黏包半包解析
- `014` filechannel-方法简介
- `015` filechannel-传输数据
- `016` filechannel-传输数据大于2g
- `017` path&files
- `018` files-walkfiletree
- `019` files-walkfiletree-删除多级目录
- `020` files-walk-拷贝多级目录
- `021` nio-阻塞模式
- `022` nio-阻塞模式-调试1
- `023` nio-阻塞模式-调试2
- `024` nio-非阻塞模式
- `025` nio-非阻塞模式-调试
- `026` nio-selector-处理accept
- `027` nio-selector-cancel
- `028` nio-selector-处理read
- `029` nio-selector-用完key为何要remove
- `030` nio-selector-处理客户端断开
- `031` nio-selector-消息边界问题
- `032` nio-selector-处理消息边界
- `033` nio-selector-处理消息边界-容量超出
- `034` nio-selector-处理消息边界-附件与扩容
- `035` nio-selector-bytebuffer扩容
- `036` nio-selector-写入内容过多问题
- `037` nio-selector-处理可写事件
- `038` nio-网络编程小结
- `039` nio-多线程优化-分析
- `040` nio-多线程优化-worker编写
- `041` nio-多线程优化-worker关联
- `042` nio-多线程优化-问题分析
- `043` nio-多线程优化-问题解决
- `044` nio-多线程优化-问题解决-wakeup
- `045` nio-多线程优化-multi worker
- `046` nio-概念剖析-stream vs channel
- `047` nio-概念剖析-io模型-阻塞非阻塞
- `048` nio-概念剖析-io模型-多路复用
- `049` nio-概念剖析-io模型-异步
- `050` nio-概念剖析-零拷贝
- `051` nio-概念剖析-io模型-异步例子

## 第2章 Netty入门核心API

- `052` netty入门-概述
- `053` netty入门-hello-server
- `054` netty入门-hello-client
- `055` netty入门-hello-流程分析
- `056` netty入门-hello-正确观念
- `057` netty入门-eventloop-概述
- `058` netty入门-eventloop-普通-定时任务
- `059` netty入门-eventloop-io任务
- `060` netty入门-eventloop-分工细化
- `061` netty入门-eventloop-分工细化
- `062` netty入门-eventloop-切换线程
- `063` netty入门-channel
- `064` netty入门-channelFuture-连接问题
- `065` netty入门-channelFuture-处理结果
- `066` netty入门-channelFuture-关闭问题
- `067` netty入门-channelFuture-处理关闭
- `068` netty入门-channelFuture-处理关闭
- `069` netty入门-为什么要异步
- `070` netty入门-future-promise-概述
- `071` netty入门-jdk-future
- `072` netty入门-netty-future
- `073` netty入门-netty-promise
- `074` netty入门-pipeline
- `075` netty入门-inbound-handler
- `076` netty入门-outbound-handler
- `077` netty入门-embedded-channel
- `078` netty入门-bytebuf-创建
- `079` netty入门-bytebuf-是否池化和内存
- `080` netty入门-bytebuf-组成
- `081` netty入门-bytebuf-写入
- `082` netty入门-bytebuf-读取
- `083` netty入门-bytebuf-内存释放
- `084` netty入门-bytebuf-头尾释放源码
- `085` netty入门-bytebuf-零拷贝-slice
- `086` netty入门-bytebuf-零拷贝-slice
- `087` netty入门-bytebuf-零拷贝-composite
- `088` netty入门-bytebuf-小结
- `089` netty入门-思考问题

## 第3章 Netty进阶：粘包半包、协议设计、聊天室实战

- `090` netty进阶-黏包半包-现象演示
- `091` netty进阶-黏包半包-滑动窗口
- `092` netty进阶-黏包半包-分析
- `093` netty进阶-黏包半包-解决-短链接
- `094` netty进阶-黏包半包-解决-定长解码器
- `095` netty进阶-黏包半包-解决-行解码器
- `096` netty进阶-黏包半包-解决-LTC解码器
- `097` netty进阶-黏包半包-解决-LTC解码器
- `098` netty进阶-协议设计与解析-redis
- `099` netty进阶-协议设计与解析-http
- `100` netty进阶-协议设计与解析-自定义
- `101` netty进阶-协议设计与解析-编码
- `102` netty进阶-协议设计与解析-解码
- `103` netty进阶-协议设计与解析-测试
- `104` netty进阶-协议设计与解析-测试
- `105` netty进阶-协议设计与解析-@sharable
- `106` netty进阶-协议设计与解析-@sharable
- `107` netty进阶-聊天业务-介绍
- `108` netty进阶-聊天业务-包结构
- `109` netty进阶-聊天业务-登录
- `110` netty进阶-聊天业务-登录-线程通信
- `111` netty进阶-聊天业务-业务消息发送
- `112` netty进阶-聊天业务-单聊消息处理
- `113` netty进阶-聊天业务-群聊建群处理
- `114` netty进阶-聊天业务-群聊消息处理
- `115` netty进阶-聊天业务-退出处理
- `116` netty进阶-聊天业务-空闲检测
- `117` netty进阶-聊天业务-心跳

## 第4章 Netty性能优化、RPC手写实战、核心源码剖析

### 4.1 Netty参数优化与序列化扩展

- `118` netty优化-扩展序列化算法
- `119` netty优化-扩展序列化算法-json
- `120` netty优化-扩展序列化算法-测试
- `121` netty优化-参数-连接超时
- `122` netty优化-参数-连接超时源码分析
- `123` netty优化-参数-backlog-连接队列
- `124` netty优化-参数-backlog-作用演示
- `125` netty优化-参数-backlog-默认值
- `126` netty优化-参数-backlog-ulimit&nodelay
- `127` netty优化-参数-backlog-分配器
- `128` netty优化-参数-backlog-rcv分配器

### 4.2 手写简易RPC框架

- `129` netty优化-rpc-准备
- `130` netty优化-rpc-服务端实现
- `131` netty优化-rpc-客户端实现
- `132` netty优化-rpc-gson问题解决
- `133` netty优化-rpc-客户端-获取channel
- `134` netty优化-rpc-客户端-代理
- `135` netty优化-rpc-客户端-获取结果
- `136` netty优化-rpc-客户端-遗留问题
- `137` netty优化-rpc-客户端-异常调用

### 4.3 Netty核心源码逐行分析

- `138` netty源码-启动流程-nio回顾
- `139` netty源码-启动流程-概述
- `140` netty源码-启动流程-init
- `141` netty源码-启动流程-register
- `142` netty源码-启动流程-dobind0
- `143` netty源码-启动流程-关注accept事件
- `144` netty源码-eventloop-selector何时唤醒
- `145` netty源码-eventloop-2个selector
- `146` netty源码-eventloop-线程启动
- `147` netty源码-eventloop-wakeup方法
- `148` netty源码-eventloop-wakenUp变量
- `149` netty源码-eventloop-进入select分支
- `150` netty源码-eventloop-select阻塞多久
- `151` netty源码-eventloop-select空轮询bug
- `152` netty源码-eventloop-ioratio
- `153` netty源码-eventloop-处理事件
- `154` netty源码-accept流程-nio回顾
- `155` netty源码-accept流程
- `156` netty源码-read流程
- `157` 剩余章节：更多源码分析、底层原理与生产问题调优
