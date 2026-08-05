# 06 - ByteBuf 内存管理

> 🎯 ByteBuf 是 Netty 高性能的基石 — 池化内存避免 GC 压力、直接内存避免拷贝、引用计数精准释放、零拷贝减少数据搬运。理解 ByteBuf 才能写出高性能 Netty 代码

---

## 目录

1. [ByteBuf 概述](#1-bytebuf-概述)
2. [堆内存 vs 直接内存](#2-堆内存-vs-直接内存)
3. [池化 vs 非池化](#3-池化-vs-非池化)
4. [引用计数与内存泄漏](#4-引用计数与内存泄漏)
5. [零拷贝机制](#5-零拷贝机制)
6. [ByteBuf 使用规范](#6-bytebuf-使用规范)

---

## 1. ByteBuf 概述

### 1.1 ByteBuf vs ByteBuffer

| 维度 | ByteBuf | JDK ByteBuffer |
|------|---------|---------------|
| 读写指针 | 双指针（readerIndex / writerIndex） | 单指针（position），需 flip() |
| 容量 | 自动扩容 | 固定，需手动 |
| 池化 | ✅ PooledByteBufAllocator | ❌ |
| 引用计数 | ✅ 自动/手动释放 | ❌ 依赖 GC |
| 零拷贝 | ✅ slice / duplicate / composite | ❌ |

### 1.2 双指针模型

```text
             readerIndex    writerIndex   capacity
               │              │              │
  ┌────────────┼──────────────┼──────────────┼────┐
  │ discarded  │   readable   │   writable   │    │
  └────────────┴──────────────┴──────────────┴────┘
     已读(可丢弃)    可读区域        可写区域

readableBytes = writerIndex - readerIndex
writableBytes = capacity - writerIndex
```

```java
ByteBuf buf = Unpooled.buffer(16);
buf.writeInt(42);                   // writerIndex += 4
buf.writeLong(123L);                // writerIndex += 8
int value = buf.readInt();          // readerIndex += 4

// discardReadBytes() — 压缩已读空间
buf.discardReadBytes();             // 将可读区域移到开头

// clear() — 重置读写指针（不清理数据）
buf.clear();                        // readerIndex = writerIndex = 0
```

---

## 2. 堆内存 vs 直接内存

| 维度 | 堆内存 (Heap) | 直接内存 (Direct) |
|------|:---:|:---:|
| 分配位置 | JVM 堆 | 堆外（OS 内存） |
| IO 操作 | 需拷贝到 Direct Buffer | 零拷贝（直接 IO） |
| GC 影响 | 受 GC 管理，可能 STW | 不受 GC（但引用对象在堆） |
| 分配速度 | 快 | 较慢 |
| 适用场景 | 业务逻辑（堆内编解码） | ⭐ 网络 IO（读写 Socket） |

```java
// 创建方式
ByteBuf heapBuf = Unpooled.buffer();                 // 堆内存
ByteBuf directBuf = Unpooled.directBuffer();          // 直接内存
ByteBuf pooledBuf = PooledByteBufAllocator.DEFAULT.buffer();  // 池化（默认 Direct）
```

> 💡 **Netty 默认策略**：IO 读写用 Direct 内存（走 PooledByteBufAllocator），业务 Handler 内编解码可用 Heap 内存。

---

## 3. 池化 vs 非池化

### 3.1 池化原理

```text
PooledByteBufAllocator 内部结构：

  PoolArena（内存区域 — 按内存大小分类）
  ├── Tiny （< 512B）
  ├── Small（512B ~ 8KB）
  ├── Normal（8KB ~ 16MB）
  └── Huge  （> 16MB，不池化）

  每个 PoolArena 包含：
  ├── PoolChunkList（Chunk 链表）
  │   └── PoolChunk（16MB 连续内存块）
  │       └── Page（8KB）× 2048
  └── PoolSubpage（小于 Page 的分配）
```

```java
// 分配池化的 ByteBuf
ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer(1024);
// 释放 → 归还池中，下次复用（不释放堆外内存）
buf.release();
```

| 对比 | 非池化 | 池化 |
|------|:---:|:---:|
| 分配速度 | ⭐⭐ 每次 malloc | ⭐⭐⭐ 从池中取 |
| 内存碎片 | 高 | 低 |
| GC 压力 | 高（频繁回收） | 低（复用） |
| **推荐** | 开发测试 | ⭐ 生产环境 |

```java
// Server 端开启池化
ServerBootstrap bootstrap = new ServerBootstrap();
bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
         .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
```

---

## 4. 引用计数与内存泄漏

> ⚠️ Direct ByteBuf 不受 GC 管理，必须手动 release()。Netty 使用引用计数（ReferenceCounted）管理。

```java
ByteBuf buf = Unpooled.directBuffer();
buf.retain();        // refCnt++（引用计数+1）
buf.release();       // refCnt--（引用计数-1），归零时释放
buf.release();       // refCnt 归零 → 释放 Direct 内存
```

### 4.1 内存泄漏检测

```java
// 启动参数开启泄漏检测
// -Dio.netty.leakDetectionLevel=PARANOID

// 四个级别：
// DISABLED — 关闭
// SIMPLE   — 1% 采样，默认
// ADVANCED — 报告泄漏位置
// PARANOID — 100% 检测（开发环境）

ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
```

### 4.2 谁创建谁释放

```java
// ✅ 正确：在创建该 ByteBuf 的 Handler 中释放
public class BizHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        try {
            // 处理...
        } finally {
            ReferenceCountUtil.release(msg);  // ⚠️ 必须释放！
        }
    }
}

// ✅ 或使用 SimpleChannelInboundHandler（自动释放）
public class BizHandler2 extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        // SimpleChannelInboundHandler 方法结束后自动 release
    }
}
```

---

## 5. 零拷贝机制

### 5.1 三种零拷贝方式

| 方式 | 说明 | 适用 |
|------|------|------|
| **slice()** | 切分，共享同一块内存（引用计数+1） | 分帧处理 |
| **duplicate()** | 复制，读写指针独立但共享内存 | 多线程读 |
| **CompositeByteBuf** | 聚合多个 ByteBuf（虚拟合并） | 合并消息 |

```java
// ═══ slice — 零拷贝切分 ═══
ByteBuf original = Unpooled.buffer(100);
ByteBuf slice = original.slice(0, 50);   // 共享 original 的前 50 字节
// ⚠️ slice 和 original 共享内存，修改互相影响
// ⚠️ release original → slice 也失效

// ═══ CompositeByteBuf — 零拷贝聚合 ═══
CompositeByteBuf composite = Unpooled.compositeBuffer();
composite.addComponent(true, headerBuf);    // addComponent 不拷贝
composite.addComponent(true, bodyBuf);      // 逻辑上是拼接的完整消息
// 直接 write CompositeByteBuf 到 Socket → 零拷贝
ctx.writeAndFlush(composite);
```

### 5.2 FileRegion（文件传输零拷贝）

```java
// 直接将文件从磁盘 → Socket，不经过用户态
File file = new File("/data/large_file.iso");
FileRegion region = new DefaultFileRegion(
    new FileInputStream(file).getChannel(), 0, file.length()
);
ctx.writeAndFlush(region);     // 零拷贝（sendfile）
```

---

## 6. ByteBuf 使用规范

| ✅ DO | ❌ DON'T |
|-------|----------|
| 用 `SimpleChannelInboundHandler` 自动释放 | 忘记 release Direct ByteBuf |
| 生产环境用 PooledByteBufAllocator | 生产环境用 Unpooled |
| 开启 PARANOID 泄漏检测开发调试 | 忽略 LEAK 日志 |
| `slice()` 后原始 buf 不能 release | `slice()` 后立即 release 原始 buf |
| IO 读写用 Direct，业务编解码用 Heap | 混用导致多次拷贝 |

```java
// ⭐ 内存管理黄金法则
// 生产环境强制开启
System.setProperty("io.netty.leakDetection.level", "PARANOID");
System.setProperty("io.netty.allocator.type", "pooled");
```

> 🎯 **内存三板斧**：池化（避免频繁 malloc）、Direct 零拷贝（避免堆→堆外拷贝）、引用计数（避免泄漏）。三者叠加 = Netty 极致性能。
