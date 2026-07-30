# Android 构建发布与性能优化

> 📦 Gradle 多模块构建、APK 到 AAB 的进化、签名体系的完整链路、ProGuard/R8 代码混淆、四大性能维度优化、Google Play 发布全流程 —— 从代码到上线的最后一公里

---

## 📚 目录

1. [Gradle 多模块构建](#1-gradle-多模块构建)
2. [APK vs AAB 编译产物](#2-apk-vs-aab-编译产物)
3. [签名体系](#3-签名体系)
4. [代码混淆与压缩：R8 / ProGuard](#4-代码混淆与压缩r8--proguard)
5. [性能优化四大维度](#5-性能优化四大维度)
6. [Google Play 发布流程](#6-google-play-发布流程)

---

## 1. Gradle 多模块构建

### 1.1 模块化架构

```text
单一 module (app) → 多 module 分层架构：

app/                        ← 主应用模块 (壳)
├── feature-login/          ← 登录功能模块
├── feature-home/           ← 首页功能模块
├── feature-settings/       ← 设置功能模块
├── core-network/           ← 网络层封装（Retrofit + OkHttp）
├── core-database/          ← 数据库（Room）
├── core-ui/                ← 公共 UI 组件
├── core-model/             ← 公共数据模型
└── core-common/            ← 通用工具类

好处：
├── 编译速度 ↑（只编译变动的模块）
├── 团队协作 ↑（各小组负责不同模块）
├── 复用性 ↑（多应用共享 core 模块）
└── 动态分发 (Dynamic Feature Modules)
```

### 1.2 settings.gradle.kts

```kotlin
// 项目根目录 → 声明所有子模块
rootProject.name = "MyApplication"

include(":app")
include(":feature-login")
include(":feature-home")
include(":core-network")
include(":core-database")
include(":core-ui")
include(":core-model")
include(":core-common")
```

### 1.3 模块间依赖管理

```kotlin
// 使用 Gradle Version Catalog (推荐) → gradle/libs.versions.toml
[versions]
retrofit = "2.11.0"
room = "2.6.1"
compose-bom = "2024.12.01"

[libraries]
retrofit-core = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-gson = { module = "com.squareup.retrofit2:converter-gson", version.ref = "retrofit" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }

[plugins]
android-application = { id = "com.android.application", version = "8.7.0" }
android-library = { id = "com.android.library", version = "8.7.0" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version = "2.1.0" }
```

```kotlin
// core-network/build.gradle.kts → Library 模块
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.core.network"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
}

dependencies {
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(project(":core-model"))
}

// app/build.gradle.kts → Application 模块
plugins {
    alias(libs.plugins.android.application)
}

dependencies {
    implementation(project(":feature-login"))
    implementation(project(":feature-home"))
    implementation(project(":core-network"))
    implementation(project(":core-database"))
    implementation(project(":core-ui"))
}
```

### 1.4 Gradle 构建优化技巧

```properties
# gradle.properties
# === 内存与JVM ===
org.gradle.jvmargs=-Xmx6g -XX:+UseParallelGC -XX:MaxMetaspaceSize=1g
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

# === Android 构建 ===
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official

# === 编译速度 ===
org.gradle.workers.max=4
kotlin.incremental=true
kotlin.daemon.jvmargs=-Xmx3g
```

---

## 2. APK vs AAB 编译产物

### 2.1 编译产物对比

| 维度 | APK (传统) | AAB (App Bundle) |
|------|-----------|:---:|
| **格式** | 单一文件 `.apk` | 归档文件 `.aab` |
| **包含** | 所有架构、语言、DPI | 全部资源 + Google Play 动态切片 |
| **大小** | 全量（50MB+ 不罕见） | 上传完整，下载按需（省 20-35%） |
| **签名** | 上传 Play 签名或自签名 | **Play 签名（推荐）** |
| **Google Play** | 可上传 | ✅ **新应用必须** (2021.08+) |
| **本地安装** | ✅ adb install | ❌ 需通过 bundletool 转 APK |
| **动态功能** | ❌ | ✅ Dynamic Feature Modules |

### 2.2 构建命令

```bash
# === APK 构建 ===
./gradlew assembleDebug              # Debug APK（开发用）
./gradlew assembleRelease            # Release APK（需配置签名）
# 产物：app/build/outputs/apk/debug/app-debug.apk

# === AAB 构建 ===
./gradlew bundleDebug                # Debug Bundle
./gradlew bundleRelease              # Release Bundle（上传 Play）
# 产物：app/build/outputs/bundle/release/app-release.aab

# === 用 bundletool 处理 AAB ===
# 安装 AAB 到本地设备
bundletool build-apks --bundle=app.aab --output=app.apks
bundletool install-apks --apks=app.apks

# 查看 AAB 大小分析
bundletool get-size total --bundle=app.aab
```

### 2.3 APK 分析

```bash
# 查看 APK 构成（大小分析）
# Android Studio → Build → Analyze APK...
# 或命令行：
aapt2 dump badging app-release.apk

# APK 各部分大小分布：
├── classes.dex    → 你的代码（编译后）
├── res/           → 图片、布局等资源 → 通常最大
├── lib/           → .so 本地库
├── META-INF/      → 签名
└── assets/        → 原始资源
```

---

## 3. 签名体系

### 3.1 签名的作用与类型

```text
为什么 Android 需要签名？
├── 🔒 身份验证：证明 APK 的作者是你
├── 🔐 完整性：确保 APK 没被篡改
├── 🔄 升级验证：Google Play 只允许同签名的升级
└── 🔑 权限保护：相同签名的应用可以共享数据

签名类型：
├── debug.keystore   → Android Studio 自动生成（开发用）
└── release.keystore → 你生成 + 保护的（上线用）
```

### 3.2 生成发布签名

```bash
# 方式一：Android Studio GUI
# Build → Generate Signed Bundle / APK → Create new...

# 方式二：命令行 keytool
keytool -genkey -v \
    -keystore my-release-key.jks \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -alias my-key-alias

# 查看签名信息
keytool -list -v -keystore my-release-key.jks
```

### 3.3 Gradle 签名配置

```kotlin
// app/build.gradle.kts
android {
    signingConfigs {
        create("release") {
            // ⚠️ 绝不要把密码 hardcode！用环境变量或 keystore.properties
            storeFile = file("../my-release-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "password"
            keyAlias = System.getenv("KEY_ALIAS") ?: "my-key-alias"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "password"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

```properties
# keystore.properties（放入 .gitignore！）
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

> ⚠️ **致命错误**：丢失发布签名文件 = **永远无法更新已上线的应用**。必须备份到安全位置（包括 Google Play 签名密钥）。

---

## 4. 代码混淆与压缩：R8 / ProGuard

### 4.1 R8 简介

```text
R8 → Android 官方的代码压缩与混淆工具（替代 ProGuard）

作用：
├── Shrinking（压缩）  → 移除未使用的代码
├── Optimization（优化） → 内联、死代码消除、分支剪枝
├── Obfuscation（混淆） → 类名/方法名 → 短名（a,b,c...）
└── Resource Shrinking → 移除未使用的资源

对比：
├── ProGuard  → Java 工具（也能用于 Android），较慢
└── R8        → Google 重写，速度更快、压缩率更高（Android 默认）
```

### 4.2 混淆规则

```text
# proguard-rules.pro
# 哪些不混淆？→ 所有需要反射访问的类！

# === 数据模型（JSON 反序列化用到）===
-keep class com.example.myapp.model.** { *; }

# === Retrofit 接口 ===
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# === Room Entity ===
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# === 注解（运行时注解需保留）===
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod

# === 枚举 ===
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# === Serializable / Parcelable ===
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# === WebView JS 调用 ===
-keepclassmembers class com.example.myapp.web.JSBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# === 删除日志（Release 构建）===
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
```

### 4.3 混淆后的映射文件

```bash
# 混淆后生成的文件（务必保存！）
app/build/outputs/mapping/release/
├── mapping.txt            ← 混淆前后名称映射（Crash 堆栈还原必备）
├── seeds.txt              ← 保留的类/方法
├── usage.txt              ← 被移除的代码
└── resources.txt          ← 被移除的资源

# 还原混淆后的堆栈（非常重要！）
# Google Play Console 会自动还原，命令行手动：proguardgui
# 或上传 mapping.txt 到 Play Console：
# Play Console → 版本 → App Bundle 资源管理器 → 上传 mapping.txt
```

> ⚠️ **务必保存 mapping.txt**：没有它，线上 Crash 堆栈全是 `a.b.c.d()`，完全无法定位问题。

---

## 5. 性能优化四大维度

### 5.1 启动速度优化

```text
冷启动三阶段：
1. 加载 Application + 初始化
2. 创建 Activity + 加载布局
3. 渲染首帧

优化策略：
├── Application.onCreate() 中延迟初始化（非必须的放后台线程）
├── App Startup 库管理初始化依赖
├── SplashScreen API（Android 12+）→ 替代自定义闪屏
├── 主 Activity 布局简化（减少嵌套）
└── Baseline Profile → AOT 预编译关键代码路径

监控工具：
├── Android Studio Profiler → CPU → Start up
├── Perfetto / systrace
└── Firebase Performance Monitoring
```

### 5.2 内存优化

```text
常见内存问题：
├── 内存泄漏 (Memory Leak)
│   ├── 非静态内部类持有外部 Activity 引用
│   ├── Handler / Runnable 未取消
│   ├── 单例持有 Context（应该用 Application Context）
│   └── 匿名监听器未注销
│
├── 内存抖动 (Memory Churn)
│   ├── 循环中频繁 new 对象
│   ├── onDraw 中分配对象
│   └── 频繁创建 Bitmap
│
└── 大内存对象
    ├── Bitmap → 加载合适的分辨率
    ├── WebView → 独立进程
    └── 数据库查询结果 → 分页加载

监控工具：
├── Android Studio Memory Profiler（GC、Heap dump）
├── LeakCanary（自动检测内存泄漏）
└── MAT (Memory Analyzer Tool) → 分析 hprof 文件
```

### 5.3 APK 包体积优化

```bash
# Android Studio → Build → Analyze APK → 查看各部分大小

# === 优化策略 ===
# 1. 删除未使用的资源
android { buildTypes { release { isShrinkResources = true } }}

# 2. 使用 WebP 替代 PNG（减小 25-50%）
# Android Studio → 右键图片 → Convert to WebP

# 3. 使用 Vector Drawable 替代多分辨率位图
# 一个矢量图 = 所有分辨率的 PNG

# 4. 移除未使用的备用资源
android {
    defaultConfig {
        resConfigs("zh", "en")     # 只保留中文和英文
    }
}

# 5. 只保留需要的 ABI（去掉多余的 .so 文件）
android {
    defaultConfig {
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }
}

# 6. R8 混淆（自动压缩代码）
# 7. 使用 Dynamic Feature Module（按需下载）
# 8. 使用 Android App Bundle（AAB）代替 APK
```

### 5.4 卡顿优化

```text
Android 渲染机制：
60fps → 每帧 16ms → 主线程任务超过 16ms = 丢帧 = 卡顿

常见卡顿原因：
├── 主线程做耗时操作（网络、数据库、复杂计算）
├── 布局嵌套过深（>5 层）
├── Overdraw（同一像素被绘制多次）
├── GC 暂停（内存抖动导致）
└── Bitmap 大图解码在主线程

优化方法：
├── 耗时操作 → 协程 / RxJava / AsyncTask（已弃用）
├── 布局优化 → ConstraintLayout + <merge> + ViewStub
├── Overdraw → 开发者选项 → GPU 过度绘制可视化
├── RecyclerView → DiffUtil 精确更新 + Prefetch
└── Baseline Profile + Startup Profile → 预编译代码路径

监控工具：
├── Android Studio Profiler → CPU / GPU
├── 开发者选项 → GPU 呈现模式分析（柱状图）
├── dumpsys gfxinfo <包名> → 查看帧时间
└── BlockCanary（检测卡顿）
```

---

## 6. Google Play 发布流程

### 6.1 发布准备清单

```text
发布前检查清单：

□ 1. 移除所有调试日志
□ 2. 配置 Release 签名（keystore 已备份！）
□ 3. 启用 R8 混淆 + 资源压缩
□ 4. 关闭 debuggable (android:debuggable="false")
□ 5. 配置 Privacy Policy URL（强制要求）
□ 6. 填写应用分级问卷（Content Rating）
□ 7. 设置目标受众（Target Audience）
□ 8. 准备应用截图（手机/平板/大屏，各 2-8 张）
□ 9. 编写应用描述（多语言）
□ 10. 准备 Feature Graphic（1024×500）
□ 11. AAB 构建 + 签名
□ 12. 在至少一种真机上完整测试
```

### 6.2 发布流程

```text
Google Play Console → 创建应用

1. 基本信息
   ├── 应用名称（50 字符内）
   ├── 简要说明（80 字符内）
   ├── 完整说明（4000 字符内）
   └── 应用类别 + 标签

2. 版本发布
   ├── 生产版本 (Production)        ← 所有人都能看到
   ├── 开放测试 (Open Testing)       ← 任何人都可加入
   ├── 封闭测试 (Closed Testing)     ← 邮箱邀请制
   └── 内部测试 (Internal Testing)   ← 团队内部（最快，几分钟上线）

3. 上传 AAB
   ├── 上传 → Play Console 自动切片
   └── Play 签名管理（可选：上传你的签名密钥让 Play 帮你签）

4. 内容分级
   └── 填写问卷 → 自动生成分级（如 Everyone, Teen, Mature 17+）

5. 定价与分发
   ├── 免费 / 付费
   ├── 分发国家/地区
   └── 广告 (Ads) 声明

6. 提交审核
   └── 通常几小时到 2 天完成
```

### 6.3 版本管理

```kotlin
// app/build.gradle.kts
defaultConfig {
    versionCode = 10       // 整数，必须递增（内部版本号）
    versionName = "2.1.0"  // 用户看到的版本号
}

// 版本命名建议：语义化版本 (Semantic Versioning)
// versionName = "主版本.次版本.修订号"
// 例：2.1.0 → 主2 · 次1 · 修订0

// versionCode 策略（支持多 ABI）：
// arm64-v8a:  versionCode = 10  → 100010
// armeabi-v7a: versionCode = 10  → 200010
```

### 6.4 常用 Gradle 任务速查

```bash
# === 编译 ===
./gradlew assembleDebug              # Debug APK
./gradlew assembleRelease            # Release APK
./gradlew bundleRelease              # Release AAB

# === 测试 ===
./gradlew test                       # 单元测试
./gradlew connectedAndroidTest       # 设备仪器测试
./gradlew lint                       # 静态分析
./gradlew lintRelease                # Release 静态分析

# === 分析 ===
./gradlew assembleRelease &&         # 构建后分析
    open app/build/outputs/apk       # 查看产物
# Android Studio → Build → Analyze APK

# === 签名 ===
./gradlew signReleaseBundle          # 对 AAB 签名
./gradlew verifyReleaseResources     # 资源验证
```

---

**上一模块**：[05-Android数据存储与网络通信](./05-Android数据存储与网络通信.md) ｜ **返回总览**：[00-Android知识体系总览](./00-Android知识体系总览.md)

---

*创建于：2026年7月*
