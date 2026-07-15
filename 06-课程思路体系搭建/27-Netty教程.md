# Netty 教程

> Java 领域网络编程的王者，从 NIO 基础到源码分析，全面掌握高性能网络编程

### 第1章 Java NIO 基础（底层铺垫，Netty前置知识）

- 001 NIO三大组件-channel-buffer
- 002 NIO三大组件-服务器设计-多线程版
- 003 NIO三大组件-服务器设计-线程池版
- 004 NIO三大组件-服务器设计-selector版
- 005 ByteBuffer-基本使用
- 006 ByteBuffer-内部结构
- 007 ByteBuffer-方法演示1
- 008 ByteBuffer-方法演示2
- 009 ByteBuffer-方法演示3
- 010 ByteBuffer-方法演示4
- 011 ByteBuffer-分散读集中写
- 012 ByteBuffer-黏包半包分析
- 013 ByteBuffer-黏包半包解析
- 014 FileChannel-方法简介
- 015 FileChannel-传输数据
- 016 FileChannel-传输数据大于2g
- 017 Path & Files
- 018 Files-walkFileTree
- 019 Files-walkFileTree-删除多级目录
- 020 Files-walk-拷贝多级目录
- 021 NIO-阻塞模式
- 022 NIO-阻塞模式-调试1
- 023 NIO-阻塞模式-调试2
- 024 NIO-非阻塞模式
- 025 NIO-非阻塞模式-调试
- 026 NIO-Selector-处理accept
- 027 NIO-Selector-cancel
- 028 NIO-Selector-处理read
- 029 NIO-Selector-用完key为何要remove
- 030 NIO-Selector-处理客户端断开
- 031 NIO-Selector-消息边界问题
- 032 NIO-Selector-处理消息边界
- 033 NIO-Selector-处理消息边界-容量超出
- 034 NIO-Selector-处理消息边界-附件与扩容
- 035 NIO-Selector-ByteBuffer扩容
- 036 NIO-Selector-写入内容过多问题
- 037 NIO-Selector-处理可写事件
- 038 NIO-网络编程小结
- 039 NIO-多线程优化-分析
- 040 NIO-多线程优化-Worker编写
- 041 NIO-多线程优化-Worker关联
- 042 NIO-多线程优化-问题分析
- 043 NIO-多线程优化-问题解决
- 044 NIO-多线程优化-问题解决-wakeup
- 045 NIO-多线程优化-Multi Worker
- 046 NIO-概念剖析-Stream vs Channel
- 047 NIO-概念剖析-IO模型-阻塞非阻塞
- 048 NIO-概念剖析-IO模型-多路复用
- 049 NIO-概念剖析-IO模型-异步
- 050 NIO-概念剖析-零拷贝
- 051 NIO-概念剖析-IO模型-异步例子

### 第2章 Netty 入门核心API（基础组件）

- 052 Netty入门-概述
- 053 Netty入门-Hello-Server
- 054 Netty入门-Hello-Client
- 055 Netty入门-Hello-流程分析
- 056 Netty入门-Hello-正确观念
- 057 Netty入门-EventLoop-概述
- 058 Netty入门-EventLoop-普通定时任务
- 059 Netty入门-EventLoop-IO任务
- 060 Netty入门-EventLoop-分工细化
- 061 Netty入门-EventLoop-分工细化
- 062 Netty入门-EventLoop-切换线程
- 063 Netty入门-Channel
- 064 Netty入门-ChannelFuture-连接问题
- 065 Netty入门-ChannelFuture-处理结果
- 066 Netty入门-ChannelFuture-关闭问题
- 067 Netty入门-ChannelFuture-处理关闭
- 068 Netty入门-ChannelFuture-处理关闭
- 069 Netty入门-为什么要异步
- 070 Netty入门-Future-Promise-概述
- 071 Netty入门-JDK-Future
- 072 Netty入门-Netty-Future
- 073 Netty入门-Netty-Promise
- 074 Netty入门-Pipeline
- 075 Netty入门-Inbound Handler
- 076 Netty入门-Outbound Handler
- 077 Netty入门-Embedded Channel
- 078 Netty入门-ByteBuf-创建
- 079 Netty入门-ByteBuf-是否池化和内存
- 080 Netty入门-ByteBuf-组成
- 081 Netty入门-ByteBuf-写入
- 082 Netty入门-ByteBuf-读取
- 083 Netty入门-ByteBuf-内存释放
- 084 Netty入门-ByteBuf-头尾释放源码
- 085 Netty入门-ByteBuf-零拷贝-slice
- 086 Netty入门-ByteBuf-零拷贝-slice
- 087 Netty入门-ByteBuf-零拷贝-composite
- 088 Netty入门-ByteBuf-小结
- 089 Netty入门-思考问题

### 第3章 Netty进阶：粘包半包、协议设计、聊天室实战

- 090 Netty进阶-黏包半包-现象演示
- 091 Netty进阶-黏包半包-滑动窗口
- 092 Netty进阶-黏包半包-分析
- 093 Netty进阶-黏包半包-解决-短链接
- 094 Netty进阶-黏包半包-解决-定长解码器
- 095 Netty进阶-黏包半包-解决-行解码器
- 096 Netty进阶-黏包半包-解决-LTC解码器
- 097 Netty进阶-黏包半包-解决-LTC解码器
- 098 Netty进阶-协议设计与解析-redis
- 099 Netty进阶-协议设计与解析-http
- 100 Netty进阶-协议设计与解析-自定义
- 101 Netty进阶-协议设计与解析-编码
- 102 Netty进阶-协议设计与解析-解码
- 103 Netty进阶-协议设计与解析-测试
- 104 Netty进阶-协议设计与解析-测试
- 105 Netty进阶-协议设计与解析-@Sharable
- 106 Netty进阶-协议设计与解析-@Sharable
- 107 Netty进阶-聊天业务-介绍
- 108 Netty进阶-聊天业务-包结构
- 109 Netty进阶-聊天业务-登录
- 110 Netty进阶-聊天业务-登录-线程通信
- 111 Netty进阶-聊天业务-业务消息发送
- 112 Netty进阶-聊天业务-单聊消息处理
- 113 Netty进阶-聊天业务-群聊建群处理
- 114 Netty进阶-聊天业务-群聊消息处理
- 115 Netty进阶-聊天业务-退出处理
- 116 Netty进阶-聊天业务-空闲检测
- 117 Netty进阶-聊天业务-心跳

### 第4章 Netty性能优化、RPC手写实战、核心源码剖析

#### 4.1 Netty参数优化 & 序列化扩展

- 118 Netty优化-扩展序列化算法
- 119 Netty优化-扩展序列化算法-JSON
- 120 Netty优化-扩展序列化算法-测试
- 121 Netty优化-参数-连接超时
- 122 Netty优化-参数-连接超时源码分析
- 123 Netty优化-参数-backlog-连接队列
- 124 Netty优化-参数-backlog-作用演示
- 125 Netty优化-参数-backlog-默认值
- 126 Netty优化-参数-backlog-ulimit & nodelay
- 127 Netty优化-参数-backlog-分配器
- 128 Netty优化-参数-backlog-RCV分配器

#### 4.2 手写简易RPC框架（Netty综合实战）

- 129 Netty优化-RPC-准备
- 130 Netty优化-RPC-服务端实现
- 131 Netty优化-RPC-客户端实现
- 132 Netty优化-RPC-Gson问题解决
- 133 Netty优化-RPC-客户端-获取Channel
- 134 Netty优化-RPC-客户端-代理
- 135 Netty优化-RPC-客户端-获取结果
- 136 Netty优化-RPC-客户端-遗留问题
- 137 Netty优化-RPC-客户端-异常调用

#### 4.3 Netty核心源码逐行分析

- 138 Netty源码-启动流程-NIO回顾
- 139 Netty源码-启动流程-概述
- 140 Netty源码-启动流程-init
- 141 Netty源码-启动流程-register
- 142 Netty源码-启动流程-doBind0
- 143 Netty源码-启动流程-关注accept事件
- 144 Netty源码-EventLoop-Selector何时唤醒
- 145 Netty源码-EventLoop-2个Selector
- 146 Netty源码-EventLoop-线程启动
- 147 Netty源码-EventLoop-wakeup方法
- 148 Netty源码-EventLoop-wakenUp变量
- 149 Netty源码-EventLoop-进入select分支
- 150 Netty源码-EventLoop-select阻塞多久
- 151 Netty源码-EventLoop-select空轮询bug
- 152 Netty源码-EventLoop-ioRatio
- 153 Netty源码-EventLoop-处理事件
- 154 Netty源码-accept流程-NIO回顾
- 155 Netty源码-accept流程
- 156 Netty源码-read流程
