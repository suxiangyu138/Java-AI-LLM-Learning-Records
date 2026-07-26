# 04 - ChannelPipeline 与 Handler

> 🎯 Pipeline 是 Netty 的大动脉 — 入站事件从 Head 传到 Tail、出站事件从 Tail 传到 Head，理解双向链表的传播机制是用好 Netty 的核心

---

## 目录

1. [ChannelPipeline 结构](#1-channelpipeline-结构)
2. [入站与出站事件](#2-入站与出站事件)
3. [Handler 传播控制](#3-handler-传播控制)
4. [Handler 生命周期](#4-handler-生命周期)
5. [常见 Handler 组合模式](#5-常见-handler-组合模式)

---

## 1. ChannelPipeline 结构

```text
Pipeline = 双向链表，节点 = ChannelHandlerContext（包装了 Handler）

  Head (Context)                          Tail (Context)
   │                                          │
   ├── Decoder-1 (Inbound)         Encoder-1 (Outbound) ──┤
   ├── Decoder-2 (Inbound)         Encoder-2 (Outbound) ──┤
   ├── BizHandler (Inbound)        BizHandler (Outbound) ──┤
   │                                          │
  [Socket]                                  [Socket]

入站事件（读）：Head → Tail 方向传播
出站事件（写）：Tail → Head 方向传播
```

```java
ChannelPipeline pipeline = ch.pipeline();
pipeline.addLast("decoder", new ByteToMessageDecoder() { ... });   // 入站
pipeline.addLast("encoder", new MessageToByteEncoder() { ... });   // 出站
pipeline.addLast("handler", new BizHandler());                     // 双向
```

| 方法 | 说明 |
|------|------|
| `addFirst(name, handler)` | 加到链表头部 |
| `addLast(name, handler)` | 加到链表尾部（最常用） |
| `addBefore(target, name, handler)` | 加到指定 Handler 前 |
| `addAfter(target, name, handler)` | 加到指定 Handler 后 |
| `remove(name)` | 移除 |
| `replace(name, newHandler)` | 替换 |

---

## 2. 入站与出站事件

### 2.1 入站事件（Inbound — 从 Socket 读到数据）

| 事件 | 触发时机 | Handler 方法 |
|------|----------|-------------|
| 通道注册 | Channel 注册到 EventLoop | `channelRegistered` |
| 通道激活 | Channel 连接建立 | `channelActive` |
| 收到数据 | `channelRead()` 触发 | `channelRead(ctx, msg)` |
| 读完成 | 一次读操作完成 | `channelReadComplete` |
| 用户事件 | 自定义事件触发 | `userEventTriggered` |
| 通道异常 | 异常发生 | `exceptionCaught` |

```java
// 入站 Handler 示例
public class MyInboundHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("收到: " + msg);
        ctx.fireChannelRead(msg);    // ⭐ 传给下一个 Inbound Handler
        // 如果不调 fireXxx() → 事件在此终止
    }
}
```

### 2.2 出站事件（Outbound — 向 Socket 写数据）

| 事件 | 触发时机 | Handler 方法 |
|------|----------|-------------|
| 写数据 | `ctx.write(msg)` | `write(ctx, msg, promise)` |
| 刷数据 | `ctx.flush()` | `flush(ctx)` |
| 关闭连接 | `ctx.close()` | `close(ctx, promise)` |
| 绑定/连接 | 客户端连接时 | `connect/bind` |

```java
// 出站 Handler 示例
public class MyOutboundHandler extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        System.out.println("写出: " + msg);
        ctx.write(msg, promise);     // ⭐ 传给上一个 Outbound Handler
    }
}
```

### 2.3 ChannelInboundHandlerAdapter vs SimpleChannelInboundHandler

| 类 | 特点 | 适用 |
|----|------|------|
| `ChannelInboundHandlerAdapter` | 需手动 `ctx.fireChannelRead(msg)` 传递 | 中间 Handler |
| `SimpleChannelInboundHandler<I>` | 自动释放 ByteBuf + 不需手动传递 | ⭐ 业务 Handler |

---

## 3. Handler 传播控制

```java
public class ControlHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // ═══ 传播控制 ═══

        // 1. 继续传给下一个 Handler（默认行为）
        ctx.fireChannelRead(msg);

        // 2. 不调用 fireXxx → 事件终止
        // （直接处理，不传给后续 Handler）

        // 3. 动态添加/删除 Handler
        ctx.pipeline().addAfter("encoder", "compressor", new Compressor());

        // 4. 跳过中间 Handler，直接从 tail 写（不经过 出站 Handler）
        ctx.channel().writeAndFlush(msg);   // 从 tail 出发
        // vs
        ctx.writeAndFlush(msg);             // 从当前 ctx 出发
    }
}
```

| 方式 | 起点 | 经过出站 Handler |
|------|------|:---:|
| `ctx.writeAndFlush(msg)` | 当前 ctx 往前 | ✅ 经过前面的 Outbound |
| `ctx.channel().writeAndFlush(msg)` | Tail 往前 | ✅ 经过所有 Outbound |

---

## 4. Handler 生命周期

```java
@Sharable   // ⚠️ 标注了才能被多个 Channel 共享
public class LifecycleHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        // Handler 被加到 Pipeline 时调用
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        // Handler 从 Pipeline 移除时调用
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // Channel 变为活跃（连接建立）
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // Channel 变为非活跃（连接断开）
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();   // 异常时关闭连接
    }
}
```

> ⚠️ **@Sharable**：标注后才可被多个 Channel Pipeline 共享（无状态 Handler）。有状态的 Handler（如 Decoder）不能共享。

---

## 5. 常见 Handler 组合模式

```java
// ⭐ 典型的 Pipeline 配置
pipeline
    // ← 入站：Head → Tail →
    .addLast(new LengthFieldBasedFrameDecoder(65535, 0, 4, 0, 4))  // ① 粘包处理
    .addLast(new ProtobufDecoder(MyProto.getDefaultInstance()))    // ② 解码
    .addLast(new IdleStateHandler(0, 0, 60))                      // ③ 空闲检测
    .addLast(new HeartbeatHandler())                               // ④ 心跳
    .addLast(new AuthHandler())                                    // ⑤ 认证
    .addLast(new BizHandler())                                     // ⑥ 业务

    // ← 出站：Tail → Head ←
    .addLast(new ProtobufEncoder())                                // ⑦ 编码
    .addLast(new LengthFieldPrepender(4));                         // ⑧ 加长度头
```

| Handler | 类型 | 作用 |
|---------|:---:|------|
| FrameDecoder | Inbound | 处理粘包拆包，输出完整帧 |
| ProtobufDecoder | Inbound | 字节 → Protobuf 对象 |
| IdleStateHandler | Inbound | 触发空闲事件 |
| HeartbeatHandler | Inbound | 处理心跳 PING/PONG |
| BizHandler | Inbound | 业务逻辑 |
| ProtobufEncoder | Outbound | Protobuf 对象 → 字节 |
| LengthFieldPrepender | Outbound | 添加长度字段 |

> 🎯 **Pipeline 口诀**：入站 Head→Tail（读路径），出站 Tail→Head（写路径）。Decoder 是入站，Encoder 是出站，业务 Handler 通常只处理入站。
