# Android 系统架构与演进

> 🏗️ 从 Android 1.0 到 Android 16，从 Dalvik 到 ART，从 Linux Kernel 到 Framework —— 作为 Java 开发者，你必须理解的 Android 底层架构

---

## 📚 目录

1. [Android 版本演进简史](#1-android-版本演进简史)
2. [Android 系统架构全景](#2-android-系统架构全景)
3. [ART vs Dalvik vs HotSpot JVM](#3-art-vs-dalvik-vs-hotspot-jvm)
4. [APK 结构与编译流程](#4-apk-结构与编译流程)
5. [Android Java 与标准 Java 的差异](#5-android-java-与标准-java-的差异)

---

## 1. Android 版本演进简史

### 1.1 完整版本时间线

| 版本 | API | 代号 | 年份 | 里程碑意义 |
|------|:--:|------|:--:|------|
| 1.0 | 1 | — | 2008 | 首个商用版，Dalvik 虚拟机 |
| 2.2 | 8 | Froyo | 2010 | JIT 编译引入，性能大幅提升 |
| 4.0 | 14 | Ice Cream Sandwich | 2011 | Holo 设计语言，统一手机/平板 |
| 4.4 | 19 | KitKat | 2013 | ART 运行时预览，内存优化 |
| 5.0 | 21 | Lollipop | 2014 | **ART 取代 Dalvik**，Material Design 诞生 |
| 7.0 | 24 | Nougat | 2016 | JIT+AOT 混合编译，多窗口模式 |
| 8.0 | 26 | Oreo | 2017 | 通知渠道、画中画、自动填充 |
| 9 | 28 | Pie | 2018 | 手势导航、Adaptive Battery |
| 10 | 29 | Q | 2019 | 暗色主题、隐私增强、5G |
| 11 | 30 | R | 2020 | 对话通知、一次性权限、无线 ADB |
| 12 | 31 | S | 2021 | Material You、动态取色 |
| 13 | 32 | Tiramisu | 2022 | 通知权限、照片选择器、蓝牙 LE Audio |
| 14 | 33 | Upside Down Cake | 2023 | Health Connect、预测性返回手势 |
| 15 | 35 | Vanilla Ice Cream | 2024 | 卫星通信、应用归档、隐私沙盒 |
| 16 | 36 | — | 2025 | 增强的 AI 能力、多任务改进 |

### 1.2 向后兼容策略

```text
Android API Level 版本分布 (2026 年)：

Android 独特的兼容设计：
├── compileSdk    → 编译用最新的 SDK（如 API 36）
├── minSdk        → 最低支持版本（如 API 24 = Android 7.0）
└── targetSdk     → 目标版本（决定运行时行为适配）

原则：编译用最新，minSdk 尽量低，targetSdk 保持最新
```

> 💡 **关键认知**：Android 的版本碎片化远超 Web 前端，`minSdk` 决定了你能使用哪些 API 和 Jetpack 库。

---

## 2. Android 系统架构全景

### 2.1 分层架构图

```text
┌─────────────────────────────────────────────────────────┐
│                    应用层 (System Apps + Third-party)      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │  Dialer  │ │  Camera  │ │  Email   │ │  Your App│   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │
├─────────────────────────────────────────────────────────┤
│                   Java API Framework (Java/Kotlin)        │
│  ┌──────────┐ ┌──────────┐ ┌────────────┐ ┌────────┐   │
│  │View System│ │Content   │ │Package     │ │Activity│   │
│  │          │ │Providers │ │Manager     │ │Manager │   │
│  └──────────┘ └──────────┘ └────────────┘ └────────┘   │
├─────────────────────────────────────────────────────────┤
│               Native Libraries (C/C++) + ART              │
│  ┌──────┐┌──────┐┌──────┐┌────┐  ┌──────────────────┐   │
│  │WebKit││OpenGL││SQLite││SSL│  │  ART (Android     │   │
│  └──────┘└──────┘└──────┘└────┘  │  Runtime)        │   │
│                                    │  AOT + JIT 混合  │   │
│                                    └──────────────────┘   │
├─────────────────────────────────────────────────────────┤
│               硬件抽象层 (HAL)                              │
│  ┌──────┐┌──────┐┌──────┐┌──────┐┌──────┐             │
│  │Audio ││Camera││GPS   ││WiFi  ││Sensor│             │
│  └──────┘└──────┘└──────┘└──────┘└──────┘             │
├─────────────────────────────────────────────────────────┤
│                    Linux Kernel                           │
│  ┌──────┐┌──────┐┌──────┐┌────┐┌──────────┐           │
│  │Binder││Memory││Power ││WiFi││Display   │           │
│  │IPC   ││Mgt   ││Mgt   ││    ││Driver    │           │
│  └──────┘└──────┘└──────┘└────┘└──────────┘           │
└─────────────────────────────────────────────────────────┘
```

### 2.2 各层与 Java 开发者的关系

| 层级 | 技术栈 | Java 后端开发者的熟悉度 | 关键点 |
|------|--------|:---:|------|
| **Application** | Java/Kotlin + Android SDK | ✅ 直接写 | 业务逻辑层 |
| **Framework** | Java 编写，提供 API | ✅ 直接调用 | Activity/Service 等即在此层 |
| **ART + Native** | C/C++ + ART (AOT/JIT) | ⚠️ JNI 才接触 | `.java→.class→.dex→.odex` |
| **HAL** | C/C++，厂商实现 | ❌ 不接触 | 硬件驱动接口 |
| **Linux Kernel** | C，fork 自 Linux | ❌ 不接触 | Binder IPC、内存管理 |

> 🎯 **核心要点**：Android 本质上是一个 **Linux 系统** + **Java API 框架**。你写的 Android Java 代码运行在 ART 虚拟机上，ART 又运行在 Linux 内核之上。

---

## 3. ART vs Dalvik vs HotSpot JVM

### 3.1 三者对比

| 维度 | HotSpot JVM (标准 Java) | Dalvik (Android 4.x) | ART (Android 5+) |
|------|------------------------|----------------------|-------------------|
| **字节码格式** | `.class` (Java bytecode) | `.dex` (Dalvik bytecode) | `.dex` (Dalvik bytecode) |
| **指令集** | 栈式 (Stack-based) | 寄存器式 (Register-based) | 寄存器式 |
| **编译策略** | JIT (Just-In-Time) | JIT (Trace-based) | **AOT + JIT 混合** |
| **编译时机** | 运行时逐方法编译 | 运行时按热点 Trace 编译 | 安装时 AOT + 运行时 JIT |
| **安装速度** | 不适用 | 快（不编译） | **慢** (AOT 编译耗时) |
| **启动速度** | 中等 | 慢（JIT 预热慢） | **快** (已预编译) |
| **运行性能** | 高（-server 模式） | 较低 | **高** |
| **内存占用** | 中等 | 较小 | 较大 (存 oat 文件) |
| **GC** | G1/ZGC/Shenandoah | 非分代 Mark-Sweep | **分代并发** (Generational CC) |

### 3.2 编译流程：Java 源码 → 运行

```text
标准 Java (HotSpot):
┌──────┐  javac   ┌────────┐  JIT    ┌──────────┐
│ .java │ ──────→ │ .class │ ──────→ │ 机器码执行│
└──────┘         └────────┘ (运行时) └──────────┘

Android (ART):
┌──────┐  javac   ┌────────┐   dx/d8    ┌──────┐   dex2oat   ┌──────────┐
│ .java │──────→ │ .class │ ────────→ │ .dex │ ─────────→│ .odex     │
└──────┘         └────────┘  (转.dex)  └──────┘ (安装时AOT)│(编译后机器码)│
                                                          └──────────┘
                                                           + 运行时 profile 引导 JIT
```

```bash
# 查看 APK 中的 dex 文件
unzip -l app-release.apk | grep ".dex"
# classes.dex  classes2.dex  classes3.dex...

# dex 文件的方法数限制：单个 dex 最多 65536 方法
# 超过后使用 MultiDex 分包
```

### 3.3 Java 后端开发者应知的差异

```text
Android 删除了标准 JDK 中的很多包：
├── ❌ java.awt / javax.swing → 用 Android View/Compose
├── ❌ java.rmi               → 用 Binder IPC
├── ❌ javax.xml.ws           → 用 Retrofit
├── ❌ javax.imageio          → 用 BitmapFactory
├── ⚠️ java.beans             → 部分支持
├── ⚠️ javax.crypto           → 存在但不完整（部分算法缺失）
├── ✅ java.util              → 大部分可用（含 Stream, Optional since API 24）
├── ✅ java.lang              → 几乎完整
├── ✅ java.io / java.nio     → 可用
└── ✅ java.net / java.sql    → 可用

另外：
- 反射被限制（Android 9+ 隐藏 API 灰名单/黑名单）
- 动态加载 ClassLoader 受严格限制
- 线程模型不同：主线程 (UI Thread) 不能阻塞
```

---

## 4. APK 结构与编译流程

### 4.1 APK 文件结构

```text
app-release.apk  (本质上是一个 ZIP 包)
│
├── AndroidManifest.xml         ← 二进制格式的清单文件
├── classes.dex                 ← 编译后的 DEX 字节码
├── classes2.dex                ← MultiDex 时额外分包
├── res/                        ← 编译后的资源
│   ├── layout/                 ← XML 布局编译为二进制
│   ├── drawable/               ← 图片资源 (PNG/WebP/VectorDrawable)
│   ├── values/                 ← 字符串、颜色、尺寸等
│   └── mipmap/                 ← 应用图标（不同分辨率）
├── resources.arsc              ← 编译后的资源索引表
├── lib/                        ← C/C++ 本地库 (.so)
│   ├── arm64-v8a/
│   ├── armeabi-v7a/
│   └── x86_64/
├── META-INF/                   ← 签名信息
│   ├── MANIFEST.MF
│   ├── CERT.SF
│   └── CERT.RSA
└── assets/                     ← 原始资源（不编译，原样打包）
```

### 4.2 AAB (Android App Bundle) vs APK

| 维度 | APK | AAB (App Bundle) |
|------|:---:|:---:|
| **发布格式** | 单一安装包 | 动态分发格式 |
| **Google Play** | 直接上传 | ✅ **必须使用** (2021.08+) |
| **体积** | 包含所有架构的 .so | 按设备架构/语言/Dpi 切片 |
| **下载大小** | 全量 | 减少 20-35% |
| **动态功能** | ❌ | ✅ Dynamic Feature Modules |

---

## 5. Android Java 与标准 Java 的差异

### 5.1 API 级别与 Java 版本对应

| Android API | Android 版本 | 支持的 Java 版本 | Kotlin 版本 |
|:---:|------|:----:|:----:|
| 26+ | 8.0 Oreo | Java 8 (完整) | 1.4+ |
| 31+ | 12 | Java 11 (部分) | 1.6+ |
| 34+ | 14 | Java 17 (核心 API) | 1.9+ |
| 35+ | 15 | Java 21 (records, patterns 预览) | 2.0+ |

```bash
# Android Gradle Plugin 中配置 Java 版本
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
```

### 5.2 Android 独有的内存与进程模型

```text
标准 Java 后端：              Android：
├── 一个 main 方法启动       ├── 没有 main 方法！
├── 长期运行的进程           ├── 系统随时可能杀死进程
├── 手动管理内存             ├── 系统内存不足时回收
├── 异常 = 程序崩溃          ├── 异常 = 弹回上一页或 ANR
└── 无"生命周期"概念         └── 每个组件都有严格的生命周期

Android 进程优先级（从高到低）：
1. 前台进程 (Foreground)      ← 用户正在交互的 Activity
2. 可见进程 (Visible)         ← 被半透明 Activity 覆盖
3. 服务进程 (Service)         ← 正在运行的 Service
4. 后台进程 (Background)      ← 用户不可见的 Activity
5. 空进程 (Empty)             ← 缓存中，最快被杀死
```

> 🎯 **核心要点**：Android 开发最大的思维转变 —— **你的进程随时可能被杀死**。必须通过 `onSaveInstanceState()`、ViewModel、持久化等机制保存状态。

---

**下一模块**：[02-Android开发环境搭建](./02-Android开发环境搭建.md) ｜ **返回总览**：[00-Android知识体系总览](./00-Android知识体系总览.md)

---

*创建于：2026年7月*
