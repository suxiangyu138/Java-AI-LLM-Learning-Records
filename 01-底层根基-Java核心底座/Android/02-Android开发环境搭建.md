# Android 开发环境搭建

> 🛠️ Android Studio、SDK Manager、AVD 模拟器、Gradle 构建工具 —— 从零到运行第一个 Hello World，为 Java 后端开发者定制的 Android 环境搭建指南

---

## 📚 目录

1. [Android Studio 安装与配置](#1-android-studio-安装与配置)
2. [SDK Manager 详解](#2-sdk-manager-详解)
3. [AVD：Android 虚拟设备](#3-avdandroid-虚拟设备)
4. [Gradle 构建体系](#4-gradle-构建体系)
5. [第一个 Android 项目](#5-第一个-android-项目)

---

## 1. Android Studio 安装与配置

### 1.1 系统要求

| 需求 | Windows | macOS | Linux |
|------|---------|-------|-------|
| **OS** | Win10 64-bit+ | macOS 12+ | 64-bit Linux |
| **RAM** | 最少 8 GB（推荐 16 GB+） | ← 同 | ← 同 |
| **磁盘** | 最少 8 GB，推荐 SSD 30 GB+ | ← 同 | ← 同 |
| **CPU** | x86_64, Intel Core i5+ | Apple Silicon 原生支持 | x86_64 |
| **JDK** | JetBrains Runtime (IDE 内置) | ← 同 | ← 同 |

```bash
# 下载 Android Studio (二选一)
# 1. 官网：https://developer.android.com/studio
# 2. 包管理器：
# macOS:   brew install --cask android-studio
# Windows: winget install Google.AndroidStudio
```

### 1.2 首次启动配置

```text
安装向导 (Setup Wizard)：

1. Install Type
   → Standard (推荐) / Custom (手动选择组件)

2. UI Theme
   → Darcula (暗色) / Light

3. SDK Components Setup
   → 默认勾选的：
     ✅ Android SDK (最新版)
     ✅ Android SDK Platform (最新 API)
     ✅ Android Emulator (模拟器)
     ✅ Intel HAXM / Apple Hypervisor (加速)

4. 验证设置
   → SDK 路径（记住这个路径）：
     Windows: %USERPROFILE%\AppData\Local\Android\Sdk
     macOS:   ~/Library/Android/sdk
     Linux:   ~/Android/Sdk
```

### 1.3 IDEA 开发者快速适应

```text
如果你熟悉 IntelliJ IDEA，Android Studio 几乎一样：

相同：
├── 基于 IntelliJ Platform
├── 相同的快捷键体系（Keymap: IntelliJ IDEA Classic）
├── 相同的代码编辑、重构、版本控制
├── File → Settings (Win) / Preferences (Mac)

不同：
├── ➕ 多出了：Logcat、Layout Inspector、Device Manager、Profiler
├── ➖ 缺少了：Spring、JPA、Maven 支持（Gradle 替代）
├── 🔄 变了：Run → Run 'app'（部署到模拟器/真机）
└── 🆕 新增：SDK Manager、AVD Manager、Resource Manager
```

### 1.4 与 VS Code 对比

| 功能 | Android Studio | VS Code + Android 插件 |
|------|:---:|:---:|
| **Java/Kotlin 语言支持** | ✅ 顶级 | ⚠️ 一般（LSP 模式） |
| **布局预览** | ✅ 实时预览 + 拖拽 | ❌ |
| **模拟器集成** | ✅ 内置 | ⚠️ 需命令行操作 |
| **性能分析** | ✅ Profiler 全套 | ❌ |
| **Gradle 支持** | ✅ 深度集成 | ⚠️ 基本 |
| **启动速度** | 慢 | ✅ 快 |
| **内存占用** | 大（4G+） | ✅ 小 |
| **推荐场景** | **所有 Android 开发** | 轻量编辑、前端+Android 混合 |

> 💡 **结论**：Android 开发**强烈推荐 Android Studio**，不要用 VS Code 折腾。

---

## 2. SDK Manager 详解

### 2.1 SDK Platforms (平台)

```text
Android Studio → More Actions → SDK Manager
或 Settings → Appearance & Behavior → System Settings → Android SDK

SDK Platforms 选项卡：
├── Android 15.0 (Vanilla Ice Cream) → API 35 ← 最新
├── Android 14.0 (Upside Down Cake)  → API 34
├── Android 13.0 (Tiramisu)          → API 33
├── Android 12.0 (S)                 → API 31
├── ...（选择你想支持的版本）
└── Show Package Details → 可勾选：
    ├── Android SDK Platform 35          ← 编译用（必须）
    ├── Sources for Android 35           ← 源码（推荐，看实现）
    ├── Google Play services             ← 如用 FCM/Map/定位等
    └── System Images                    ← 模拟器镜像（在 SDK Tools 中选）
```

### 2.2 SDK Tools (工具)

| 工具 | 必须 | 说明 |
|------|:---:|------|
| **Android SDK Build-Tools** | ✅ | dx/d8、aapt2、zipalign 等编译打包工具 |
| **Android Emulator** | ✅ | 模拟器运行环境 |
| **Android SDK Platform-Tools** | ✅ | adb、fastboot、systrace |
| **NDK (Native Development Kit)** | ❌ | C/C++ 编译（游戏/音视频/加密场景才用） |
| **CMake** | ❌ | NDK 的构建工具 |
| **Android Emulator Hypervisor Driver** | 推荐 | 加速模拟器（Windows） |
| **Intel HAXM** | 推荐 | Intel CPU 加速（旧方案，已被 Hypervisor 代替） |
| **Google USB Driver** | 推荐 | Windows 上真机调试驱动 |

### 2.3 多版本 Build-Tools

```text
Android SDK Build-Tools 版本选择：
├── 安装最新版（如 35.0.0）
├── 加上向下兼容的 1-2 个主要版本
└── 每个应用通过 build.gradle 指定使用的版本：
    android {
        buildToolsVersion "35.0.0"
    }

通常不需要手动指定 buildToolsVersion，AGP (Android Gradle Plugin) 会自动选择。
```

---

## 3. AVD：Android 虚拟设备

### 3.1 创建模拟器

```text
Device Manager → Create Device

1. Choose a device definition (选择设备型号)
   推荐：
   ├── Pixel 8 (中高端，接近主流设备)
   ├── Pixel Tablet (平板测试)
   └── Wear OS (如果做手表应用)

2. Select a system image (选择系统镜像)
   推荐：
   ├── API 35 → x86_64 / arm64-v8a (取决于宿主 CPU)
   ├── ABI 选 x86_64 (Windows/Linux Intel)、arm64-v8a (Apple Silicon)
   └── Google APIs (含 Play Store) / Google Play (完整 GMS)

3. Verify configuration
   ├── Graphics: Hardware - GLES 2.0 (硬件加速)
   ├── RAM: 2048 MB (最少 1.5GB)
   ├── VM Heap: 512 MB
   └── Storage: 8 GB +
```

### 3.2 真机调试

```bash
# 1. 手机开启开发者选项
#    设置 → 关于手机 → 连击"版本号" 7 次

# 2. 开启 USB 调试
#    设置 → 开发者选项 → USB 调试 ✅

# 3. Windows 安装驱动
#    厂商官网下载 USB 驱动，或通过 SDK Manager 安装 Google USB Driver

# 4. 验证连接
adb devices
# → List of devices attached
#    R5CT1234ABCD    device    ← 已连接

# 5. 无线调试 (Android 11+)
#    开发者选项 → 无线调试 ✅
#    → 使用配对码配对
adb pair 192.168.1.100:12345
# 输入配对码
adb connect 192.168.1.100:12345
```

### 3.3 adb 常用命令

```bash
# === 设备管理 ===
adb devices -l                    # 详细设备列表
adb -s <device_id> shell          # 指定设备

# === 安装卸载 ===
adb install app-debug.apk         # 安装 APK
adb install -r app-debug.apk      # 覆盖安装（保留数据）
adb uninstall com.example.app     # 卸载（包名）

# === 文件操作 ===
adb push local.txt /sdcard/       # 推送文件到设备
adb pull /sdcard/remote.txt .     # 从设备拉取文件

# === 调试 ===
adb logcat                        # 查看日志
adb logcat | grep "MyApp"         # 过滤
adb logcat -c                     # 清空日志缓冲

# === Shell ===
adb shell                         # 进入设备 Shell
adb shell dumpsys meminfo com.example.app  # 内存信息
adb shell dumpsys activity top    # 当前栈顶 Activity
```

---

## 4. Gradle 构建体系

### 4.1 Gradle vs Maven（Java 后端开发者视角）

| 维度 | Maven | Gradle (Android) |
|------|-------|-------------------|
| **配置文件** | `pom.xml` (XML) | `build.gradle.kts` (Kotlin DSL) |
| **构建脚本文件** | 单一的 pom.xml | `settings.gradle.kts` + `build.gradle.kts` (每个模块) |
| **依赖格式** | `groupId:artifactId:version` | 同样格式 + `implementation` 配置 |
| **坐标仓库** | Maven Central | Maven Central + Google Maven |
| **插件系统** | 声明式 | 声明式 + 可编程 (Groovy/Kotlin) |
| **构建速度** | 较慢 | 快 (增量编译 + 守护进程) |
| **多模块** | `<modules>` | `include(":app", ":lib")` |

### 4.2 Android 项目的 Gradle 文件结构

```text
MyApplication/
├── settings.gradle.kts              ← 项目级配置（包含哪些模块）
├── build.gradle.kts                 ← 项目级构建（全局插件 + 仓库）
├── gradle.properties                ← Gradle 属性
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties  ← Gradle 版本
└── app/
    └── build.gradle.kts              ← 模块级构建（核心配置都在这里）
```

### 4.3 关键配置文件

**`settings.gradle.kts`** —— 声明项目包含哪些模块：

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "MyApplication"
include(":app")
```

**`app/build.gradle.kts`** —— 应用模块核心配置：

```kotlin
plugins {
    id("com.android.application")   // 应用模块
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.myapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // AndroidX 核心
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Jetpack Compose (声明式 UI)
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    // 网络
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // 测试
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
```

### 4.4 Gradle Wrapper 与常用命令

```bash
# === Gradle Wrapper（每个项目自带，无需全局安装 Gradle）===
# Maven 用 mvnw，Gradle 用 gradlew
./gradlew tasks                # macOS/Linux
gradlew tasks                  # Windows

# === 常用构建命令 ===
./gradlew assembleDebug        # 编译 Debug APK
./gradlew assembleRelease      # 编译 Release APK
./gradlew installDebug         # 编译 + 安装到模拟器
./gradlew clean                # 清理
./gradlew build                # 完整构建
./gradlew test                 # 运行单元测试
./gradlew connectedAndroidTest # 运行仪器测试
./gradlew dependencies         # 查看依赖树（类似 mvn dependency:tree）

# === 编译速度优化 ===
# gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
org.gradle.configuration-cache=true     # 配置缓存（Gradle 8.1+）
```

---

## 5. 第一个 Android 项目

### 5.1 创建项目

```text
Android Studio → New Project

模板选择：
├── Empty Views Activity     ← 传统 View 体系（Java/XML） → 推荐入门
├── Empty Activity (Compose)  ← Jetpack Compose (Kotlin) → 现代风格
├── Bottom Navigation Activity ← 底部导航（含 Fragment）
└── Navigation Drawer Views   ← 侧滑菜单

配置填写：
├── Name: My First App
├── Package name: com.example.myfirstapp
├── Save location: ~/AndroidStudioProjects/MyFirstApp
├── Language: Java / Kotlin（Java 后端开发者可先选 Java）
├── Minimum SDK: API 26 (Android 8.0)  ← 覆盖 95%+ 设备
└── Build configuration language: Kotlin DSL
```

### 5.2 项目结构速览

```text
app/
└── src/
    ├── main/
    │   ├── java/com/example/myfirstapp/
    │   │   └── MainActivity.java          ← 入口 Activity
    │   ├── res/
    │   │   ├── layout/
    │   │   │   └── activity_main.xml       ← 布局文件
    │   │   ├── values/
    │   │   │   ├── strings.xml             ← 字符串资源
    │   │   │   ├── colors.xml              ← 颜色
    │   │   │   └── themes.xml              ← 主题
    │   │   ├── drawable/                   ← 图片资源
    │   │   └── mipmap/                     ← 应用图标
    │   └── AndroidManifest.xml             ← 清单文件（核心配置）
    ├── test/                               ← 单元测试 (JVM 上跑)
    └── androidTest/                        ← 仪器测试 (设备上跑)
```

### 5.3 MainActivity.java —— 你的第一个 Activity

```java
package com.example.myfirstapp;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

// Java 后端开发者注意：没有 public static void main(String[] args)！
// 入口由系统通过 AndroidManifest.xml 中注册的 Activity 确定
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);       // 必须调用父类方法
        setContentView(R.layout.activity_main);   // 加载布局文件

        // 找到布局中的 TextView 并设置文字
        TextView textView = findViewById(R.id.textView);
        textView.setText("Hello from Java Backend Developer!");
    }
}
```

### 5.4 AndroidManifest.xml —— 应用的"DNA"

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="com.example.myfirstapp">

    <!-- 权限声明（类似 Spring Security 的 @PreAuthorize） -->
    <uses-permission android:name="android.permission.INTERNET" />

    <!-- 应用配置 -->
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.MyFirstApp">

        <!-- Activity 注册（必须在清单中声明！） -->
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <!-- 声明这是主入口 / 桌面启动图标点开的那个页面 -->
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
</manifest>
```

---

**上一模块**：[01-Android系统架构与演进](./01-Android系统架构与演进.md) ｜ **下一模块**：[03-Android四大组件详解](./03-Android四大组件详解.md) ｜ **返回总览**：[00-Android知识体系总览](./00-Android知识体系总览.md)

---

*创建于：2026年7月*
