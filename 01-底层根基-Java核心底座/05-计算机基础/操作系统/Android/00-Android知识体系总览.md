# Android 知识体系

> 🤖 从 Java 后端的视角理解 Android —— Linux 内核、ART 虚拟机、四大组件、Material Design、Jetpack 全家桶，构建从系统层到应用层的完整认知地图

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)

---

## 1. 知识体系导图

```
Android 知识体系 (Java 后端视角)
│
├── 🏗️ 系统认知层
│   └── 01-Android系统架构与演进
│       ├── Android 版本演进 (1.0 → 16)
│       ├── 系统架构：Linux Kernel → HAL → ART → Framework → Apps
│       ├── Dalvik vs ART vs HotSpot JVM 本质差异
│       ├── APK 结构、签名、沙盒机制
│       └── Android 与标准 Java 的 API 差异
│
├── 🛠️ 开发环境层
│   └── 02-Android开发环境搭建
│       ├── Android Studio：从安装到精通
│       ├── SDK Manager：SDK Platform、Build-Tools、NDK
│       ├── AVD 模拟器 vs 真机调试
│       ├── Gradle 构建体系（与 Maven 的对比）
│       └── JDK 版本与 Android 兼容性
│
├── 🧩 核心组件层
│   └── 03-Android四大组件详解
│       ├── Activity：生命周期、启动模式、任务栈
│       ├── Service：前台/后台/绑定服务
│       ├── BroadcastReceiver：系统广播、自定义广播
│       ├── ContentProvider：跨进程数据共享
│       └── Intent：组件间通信的桥梁
│
├── 🎨 UI 界面层
│   └── 04-Android UI与界面开发
│       ├── View 体系：ViewGroup → View 树
│       ├── 经典布局：Linear/Relative/Constraint/Frame
│       ├── Jetpack Compose：声明式 UI 新范式
│       ├── Material Design 3：设计规范与组件
│       └── 资源系统：layout/drawable/values/mipmap
│
├── 💾 数据与网络层
│   └── 05-Android数据存储与网络通信
│       ├── SharedPreferences / DataStore
│       ├── SQLite + Room (ORM，与 JPA/Hibernate 对比)
│       ├── 文件存储：内部/外部/缓存
│       ├── Retrofit + OkHttp：HTTP 客户端双雄
│       └── 序列化：Serializable vs Parcelable vs Gson/Moshi
│
└── 📦 工程化层
    └── 06-Android构建发布与性能优化
        ├── Gradle 多模块构建
        ├── APK vs AAB (Android App Bundle)
        ├── 签名体系：debug/release keystore
        ├── ProGuard / R8 代码混淆收缩
        ├── 性能优化：内存/启动/包体积/卡顿
        └── Google Play 发布流程
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|:-----:|
| 01 | [Android系统架构与演进](./01-Android系统架构与演进.md) | 版本史、Linux 内核、ART 虚拟机、APK 结构、与标准 Java 差异 | 所有人 |
| 02 | [Android开发环境搭建](./02-Android开发环境搭建.md) | Android Studio、SDK Manager、AVD、Gradle vs Maven、JDK 兼容 | 初学者 |
| 03 | [Android四大组件详解](./03-Android四大组件详解.md) | Activity/Service/BroadcastReceiver/ContentProvider 生命周期与通信 | Android 开发者 |
| 04 | [Android UI与界面开发](./04-Android UI与界面开发.md) | View 体系、经典布局、Jetpack Compose、Material Design 3 | Android 开发者 |
| 05 | [Android数据存储与网络通信](./05-Android数据存储与网络通信.md) | Room (ORM)、DataStore、Retrofit/OkHttp、序列化对比 | Android 开发者 |
| 06 | [Android构建发布与性能优化](./06-Android构建发布与性能优化.md) | Gradle 多模块、APK/AAB、签名、R8 混淆、性能调优、发布 | 中高级 |

---

## 3. 学习路线推荐

| 路线 | 路径 | 时长 | 目标 |
|------|------|:--:|------|
| 🟢 **快速入门** | 01 → 02 → 03 | 1 天 | 理解 Android 架构、跑通第一个 Activity |
| 🟡 **能写应用** | + 04 → 05 | 2-3 天 | 独立开发包含 UI + 网络 + 存储的完整 App |
| 🔴 **上线发布** | + 06 | 5-7 天 | 掌握构建优化、签名、发布 Google Play 全流程 |

---

## 4. 核心概念速查

| 概念 | 一句话解释 | 后端类比 | 详见 |
|------|-----------|---------|------|
| **ART** | Android Runtime，AOT+JIT 混合编译的运行时 | HotSpot JVM | 模块01 |
| **Activity** | 一个带 UI 的屏幕界面，用户交互的最小单元 | 类似 Web 的一个 Page | 模块03 |
| **Intent** | 组件间通信的消息载体（显式/隐式） | 类似 HTTP Request | 模块03 |
| **AndroidManifest.xml** | 应用的"配置声明文件"，注册所有组件 | 类似 web.xml/application.yml | 模块03 |
| **Gradle (Android)** | Android 官方构建系统，Kotlin DSL | 类似 Maven pom.xml + 自定义插件 | 模块02/06 |
| **Room** | Android 官方 ORM，封装 SQLite | 类似 JPA / Hibernate（更轻量） | 模块05 |
| **Retrofit** | 类型安全的 HTTP 客户端 | 类似 Spring 的 RestClient / OpenFeign | 模块05 |
| **Jetpack Compose** | 声明式 UI 框架 | 类似 React / Flutter 的声明式写法 | 模块04 |
| **ProGuard / R8** | 代码混淆、收缩、优化工具 | 类似 Java 的 ProGuard（同一个） | 模块06 |
| **AAB** | Android App Bundle，按需分发 | 类似 Docker 分层镜像 | 模块06 |

> 🎯 **开始学习**：[01-Android系统架构与演进](./01-Android系统架构与演进.md) ｜ **快速入门**：[02-Android开发环境搭建](./02-Android开发环境搭建.md)

---

*创建于：2026年7月*
