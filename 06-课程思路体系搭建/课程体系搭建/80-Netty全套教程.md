# 黑马程序员Netty全套课程（共157集）

> 本课程从Java NIO基础讲起，涵盖Netty入门核心API、进阶实战（粘包半包、协议设计、聊天室）以及性能优化、手写RPC框架与核心源码分析，由浅入深为Netty学习打下坚实基础。

### 第1章 Java NIO 基础（底层铺垫，Netty前置知识）

- 01 NIO三大组件-channel-buffer
- 02 NIO三大组件-服务器设计-多线程版
- 03 NIO三大组件-服务器设计-线程池版
- 04 NIO三大组件-服务器设计-selector版
- 05 ByteBuffer-基本使用
- 06 ByteBuffer-内部结构
- 07 ByteBuffer-方法演示1
- 08 ByteBuffer-方法演示2
- 09 ByteBuffer-方法演示3
- 10 ByteBuffer-方法演示4
- 11 ByteBuffer-分散读集中写
- 12 ByteBuffer-黏包半包分析
- 13 ByteBuffer-黏包半包解析
- 14 FileChannel-方法简介
- 15 FileChannel-传输数据
- 16 FileChannel-传输数据大于2g
- 17 Path & Files
- 18 Files-walkFileTree
- 19 Files-walkFileTree-删除多级目录
- 20 Files-walk-拷贝多级目录
- 21 NIO-阻塞模式
- 22 NIO-阻塞模式-调试1
- 23 NIO-阻塞模式-调试2
- 24 NIO-非阻塞模式
- 25 NIO-非阻塞模式-调试
- 26 NIO-Selector-处理accept
- 27 NIO-Selector-cancel
- 28 NIO-Selector-处理read
- 29 NIO-Selector-用完key为何要remove
- 30 NIO-Selector-处理客户端断开
- 31 NIO-Selector-消息边界问题
- 32 NIO-Selector-处理消息边界
- 33 NIO-Selector-处理消息边界-容量超出
- 34 NIO-Selector-处理消息边界-附件与扩容
- 35 NIO-Selector-ByteBuffer扩容
- 36 NIO-Selector-写入内容过多问题
- 37 NIO-Selector-处理可写事件
- 38 NIO-网络编程小结
- 39 NIO-多线程优化-分析
- 40 NIO-多线程优化-Worker编写
- 41 NIO-多线程优化-Worker关联
- 42 NIO-多线程优化-问题分析
- 43 NIO-多线程优化-问题解决
- 44 NIO-多线程优化-问题解决-wakeup
- 45 NIO-多线程优化-Multi Worker
- 46 NIO-概念剖析-Stream vs Channel
- 47 NIO-概念剖析-IO模型-阻塞非阻塞
- 48 NIO-概念剖析-IO模型-多路复用
- 49 NIO-概念剖析-IO模型-异步
- 50 NIO-概念剖析-零拷贝
- 51 NIO-概念剖析-IO模型-异步例子

### 第2章 Netty 入门核心API（基础组件）

- 01 Netty入门-概述
- 02 Netty入门-Hello-Server
- 03 Netty入门-Hello-Client
- 04 Netty入门-Hello-流程分析
- 05 Netty入门-Hello-正确观念
- 06 Netty入门-EventLoop-概述
- 07 Netty入门-EventLoop-普通-定时任务
- 08 Netty入门-EventLoop-IO任务
- 09 Netty入门-EventLoop-分工细化
- 10 Netty入门-EventLoop-分工细化
- 11 Netty入门-EventLoop-切换线程
- 12 Netty入门-Channel
- 13 Netty入门-ChannelFuture-连接问题
- 14 Netty入门-ChannelFuture-处理结果
- 15 Netty入门-ChannelFuture-关闭问题
- 16 Netty入门-ChannelFuture-处理关闭
- 17 Netty入门-ChannelFuture-处理关闭
- 18 Netty入门-为什么要异步
- 19 Netty入门-Future-Promise-概述
- 20 Netty入门-JDK-Future
- 21 Netty入门-Netty-Future
- 22 Netty入门-Netty-Promise
- 23 Netty入门-Pipeline
- 24 Netty入门-Inbound-Handler
- 25 Netty入门-Outbound-Handler
- 26 Netty入门-Embedded-Channel
- 27 Netty入门-ByteBuf-创建
- 28 Netty入门-ByteBuf-是否池化和内存
- 29 Netty入门-ByteBuf-组成
- 30 Netty入门-ByteBuf-写入
- 31 Netty入门-ByteBuf-读取
- 32 Netty入门-ByteBuf-内存释放
- 33 Netty入门-ByteBuf-头尾释放源码
- 34 Netty入门-ByteBuf-零拷贝-slice
- 35 Netty入门-ByteBuf-零拷贝-slice
- 36 Netty入门-ByteBuf-零拷贝-composite
- 37 Netty入门-ByteBuf-小结
- 38 Netty入门-思考问题

### 第3章 Netty进阶：粘包半包、协议设计、聊天室实战

- 01 Netty进阶-黏包半包-现象演示
- 02 Netty进阶-黏包半包-滑动窗口
- 03 Netty进阶-黏包半包-分析
- 04 Netty进阶-黏包半包-解决-短链接
- 05 Netty进阶-黏包半包-解决-定长解码器
- 06 Netty进阶-黏包半包-解决-行解码器
- 07 Netty进阶-黏包半包-解决-LTC解码器
- 08 Netty进阶-黏包半包-解决-LTC解码器
- 09 Netty进阶-协议设计与解析-Redis
- 10 Netty进阶-协议设计与解析-HTTP
- 11 Netty进阶-协议设计与解析-自定义
- 12 Netty进阶-协议设计与解析-编码
- 13 Netty进阶-协议设计与解析-解码
- 14 Netty进阶-协议设计与解析-测试
- 15 Netty进阶-协议设计与解析-测试
- 16 Netty进阶-协议设计与解析-@Sharable
- 17 Netty进阶-协议设计与解析-@Sharable
- 18 Netty进阶-聊天业务-介绍
- 19 Netty进阶-聊天业务-包结构
- 20 Netty进阶-聊天业务-登录
- 21 Netty进阶-聊天业务-登录-线程通信
- 22 Netty进阶-聊天业务-业务消息发送
- 23 Netty进阶-聊天业务-单聊消息处理
- 24 Netty进阶-聊天业务-群聊建群处理
- 25 Netty进阶-聊天业务-群聊消息处理
- 26 Netty进阶-聊天业务-退出处理
- 27 Netty进阶-聊天业务-空闲检测
- 28 Netty进阶-聊天业务-心跳

### 第4章 Netty性能优化、RPC手写实战、核心源码剖析

#### 4.1 Netty参数优化 & 序列化扩展

- 01 Netty优化-扩展序列化算法
- 02 Netty优化-扩展序列化算法-JSON
- 03 Netty优化-扩展序列化算法-测试
- 04 Netty优化-参数-连接超时
- 05 Netty优化-参数-连接超时源码分析
- 06 Netty优化-参数-backlog-连接队列
- 07 Netty优化-参数-backlog-作用演示
- 08 Netty优化-参数-backlog-默认值
- 09 Netty优化-参数-backlog-ulimit & nodelay
- 10 Netty优化-参数-backlog-分配器
- 11 Netty优化-参数-backlog-rcv分配器

#### 4.2 手写简易RPC框架（Netty综合实战）

- 12 Netty优化-RPC-准备
- 13 Netty优化-RPC-服务端实现
- 14 Netty优化-RPC-客户端实现
- 15 Netty优化-RPC-Gson问题解决
- 16 Netty优化-RPC-客户端-获取Channel
- 17 Netty优化-RPC-客户端-代理
- 18 Netty优化-RPC-客户端-获取结果
- 19 Netty优化-RPC-客户端-遗留问题
- 20 Netty优化-RPC-客户端-异常调用

#### 4.3 Netty核心源码逐行分析

- 21 Netty源码-启动流程-NIO回顾
- 22 Netty源码-启动流程-概述
- 23 Netty源码-启动流程-init
- 24 Netty源码-启动流程-register
- 25 Netty源码-启动流程-doBind0
- 26 Netty源码-启动流程-关注accept事件
- 27 Netty源码-EventLoop-Selector何时唤醒
- 28 Netty源码-EventLoop-2个Selector
- 29 Netty源码-EventLoop-线程启动
- 30 Netty源码-EventLoop-wakeup方法
- 31 Netty源码-EventLoop-wakenUp变量
- 32 Netty源码-EventLoop-进入select分支
- 33 Netty源码-EventLoop-select阻塞多久
- 34 Netty源码-EventLoop-select空轮询bug
- 35 Netty源码-EventLoop-ioRatio
- 36 Netty源码-EventLoop-处理事件
- 37 Netty源码-accept流程-NIO回顾
- 38 Netty源码-accept流程
- 39 Netty源码-read流程
