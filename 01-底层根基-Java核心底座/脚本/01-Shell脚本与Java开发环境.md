# Shell 脚本与 Java 开发环境

> Shell 是 Java 开发者的"操作系统对话界面"——从编译启动到进程管理，从环境配置到批量自动化

## 📚 目录

1. [Shell 脚本基础](#1)
2. [Java 环境变量管理](#2)
3. [Java 项目编译运行脚本](#3)
4. [进程管理与服务控制](#4)
5. [文件操作与日志管理](#5)
6. [跨平台脚本策略](#6)
7. [Shell 脚本调试与错误处理](#7)

---

## 1. Shell 脚本基础 {#1}

### 1.1 什么是 Shell 脚本？

Shell 脚本是将一系列命令行指令写入文件，由 Shell 解释器逐条执行。对 Java 开发者而言，Shell 脚本是自动化构建、部署、运维的基础工具。

| Shell 类型 | 解释器路径 | 主流平台 | Java 开发场景 |
|-----------|-----------|---------|-------------|
| **Bash** | `/bin/bash` | Linux / macOS | 服务器部署、CI/CD |
| **Zsh** | `/bin/zsh` | macOS 默认 | 开发环境快捷操作 |
| **Sh (POSIX)** | `/bin/sh` | 所有 Unix | 兼容性要求高的场景 |
| **PowerShell** | `pwsh.exe` / `powershell.exe` | Windows / 跨平台 | Windows 服务器、.NET 混合 |
| **Batch** | `cmd.exe` | Windows 遗留 | 简单 Windows 自动化 |

### 1.2 Shell 脚本基本结构

```bash
#!/bin/bash
# ──────────────────────────────────
# 脚本名称: build-and-run.sh
# 功能描述: 编译并运行 Java 项目
# 作者: xxx
# 日期: 2026-07-30
# ──────────────────────────────────

# 严格模式：遇到错误立即退出
set -euo pipefail

# ── 变量定义 ──
readonly PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
readonly SRC_DIR="${PROJECT_DIR}/src"
readonly BUILD_DIR="${PROJECT_DIR}/build"
readonly MAIN_CLASS="com.example.Main"

# ── 函数定义 ──
compile() {
    echo "🔨 编译中..."
    mkdir -p "${BUILD_DIR}"
    javac -d "${BUILD_DIR}" $(find "${SRC_DIR}" -name "*.java")
    echo "✅ 编译完成"
}

run() {
    echo "🚀 启动应用..."
    java -cp "${BUILD_DIR}" "${MAIN_CLASS}"
}

clean() {
    echo "🧹 清理构建目录..."
    rm -rf "${BUILD_DIR}"
}

# ── 主流程 ──
case "${1:-build}" in
    clean)  clean ;;
    compile) clean && compile ;;
    run)    compile && run ;;
    *)      echo "用法: $0 {clean|compile|run}" ;;
esac
```

> 🎯 **核心要点**：`set -euo pipefail` 是 Shell 脚本的最佳实践标配——`-e` 遇错即停，`-u` 禁止未定义变量，`-o pipefail` 管道中任一命令失败则整体失败。

### 1.3 常用 Shell 内置变量与操作符

| 变量/语法 | 含义 | 示例 |
|----------|------|------|
| `$0` | 脚本名称 | `build.sh` |
| `$1`~`$9` | 第 N 个参数 | `$1` = 第一个参数 |
| `$#` | 参数个数 | `3` |
| `$@` | 所有参数（列表） | `"$@"` 保留空格 |
| `$?` | 上一条命令退出码 | `0` = 成功 |
| `$$` | 当前 Shell PID | `12345` |
| `$(command)` | 命令替换 | `$(date +%Y%m%d)` |
| `${var:-default}` | 变量为空时用默认值 | `${JAVA_HOME:-/usr/lib/jvm/default}` |

---

## 2. Java 环境变量管理 {#2}

### 2.1 JAVA_HOME 配置

JAVA_HOME 是 Java 生态的基石变量，几乎所有 Java 工具（Maven、Gradle、Tomcat、IDE）都依赖它。

```bash
# ── Linux/macOS (写入 ~/.bashrc 或 ~/.zshrc) ──
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

# ── macOS 多版本切换 (使用 /usr/libexec/java_home) ──
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# ── Windows PowerShell (写入 $PROFILE) ──
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

### 2.2 CLASSPATH 管理

```bash
# CLASSPATH 指定类文件/JAR 搜索路径，优先级：命令行 -cp > CLASSPATH 环境变量

# 临时设置（推荐）
java -cp "lib/*:build/classes:." com.example.Main

# 永久设置（不推荐，容易冲突）
export CLASSPATH=".:/home/user/lib/*"
```

### 2.3 多版本 JDK 切换脚本

```bash
#!/bin/bash
# jdk-switch.sh —— 快速切换 JDK 版本

switch_jdk() {
    local version=$1
    case $version in
        8)  export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 ;;
        11) export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ;;
        17) export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ;;
        21) export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ;;
        25) export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ;;
        *)  echo "不支持的版本: $version"; return 1 ;;
    esac
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "✅ 已切换到 JDK $version: $JAVA_HOME"
    java -version
}

switch_jdk "${1:-21}"
```

> 💡 **提示**：更专业的多版本管理推荐使用 **SDKMAN**（Linux/macOS）或 **jabba**（跨平台），而非手动脚本。

---

## 3. Java 项目编译运行脚本 {#3}

### 3.1 单文件快速编译运行

```bash
#!/bin/bash
# quick-run.sh —— 单文件 Java 快速运行

JAVA_FILE="$1"
CLASS_NAME="${JAVA_FILE%.java}"    # 去掉 .java 后缀

# JDK 11+ 可直接运行单文件
if java --version 2>&1 | grep -qE "version \"(1[1-9]|[2-9][0-9])"; then
    java "$JAVA_FILE"
else
    javac "$JAVA_FILE" && java "$CLASS_NAME"
fi
```

### 3.2 Maven 项目一键脚本

```bash
#!/bin/bash
# mvn-quick.sh —— Maven 项目快速操作

set -euo pipefail

PROFILE="${MVN_PROFILE:-dev}"       # 默认 dev 环境
MVN_OPTS="-T 4"                     # 4 线程并行构建

case "${1:-build}" in
    build)
        ./mvnw clean package -P"${PROFILE}" ${MVN_OPTS}
        ;;
    run)
        ./mvnw spring-boot:run -P"${PROFILE}"
        ;;
    test)
        ./mvnw test -P"${PROFILE}"
        ;;
    docker)
        ./mvnw clean package -P"${PROFILE}" ${MVN_OPTS}
        docker build -t myapp:"${PROFILE}" .
        ;;
    *)
        echo "用法: $0 {build|run|test|docker}"
        ;;
esac
```

### 3.3 Gradle 项目一键脚本

```bash
#!/bin/bash
# gradle-quick.sh —— Gradle 项目快速操作

set -euo pipefail

TASK="${1:-build}"

case "${TASK}" in
    build)   ./gradlew clean build --parallel ;;
    run)     ./gradlew bootRun ;;
    test)    ./gradlew test ;;
    docker)  ./gradlew bootBuildImage ;;
    dep)     ./gradlew dependencies --configuration runtimeClasspath ;;
    *)       echo "用法: $0 {build|run|test|docker|dep}" ;;
esac
```

### 3.4 多模块项目批量操作

```bash
#!/bin/bash
# multi-module-build.sh —— 多模块项目按依赖顺序构建

MODULES=(
    "common"
    "api-contract"
    "service-user"
    "service-order"
    "service-gateway"
)

build_module() {
    local module=$1
    echo "━━━ 构建 $module ━━━"
    cd "$module" || exit 1
    ./mvnw clean package -DskipTests
    cd ..
}

# 顺序构建（按依赖关系）
for module in "${MODULES[@]}"; do
    build_module "$module"
done

echo "✅ 全部模块构建完成"
```

> 🎯 **核心要点**：多模块项目中，模块间存在依赖关系，必须按依赖顺序构建。CI/CD 中可并行构建无依赖关系的模块以加速。

---

## 4. 进程管理与服务控制 {#4}

### 4.1 Java 进程查找与管理

```bash
# ── 查找 Java 进程 ──
jps -l                          # 显示完整类名
ps aux | grep java              # 传统方式
pgrep -f "spring-boot"          # 按关键字查找

# ── 优雅关闭 ──
kill -15 <PID>                  # SIGTERM：触发 ShutdownHook
# 或通过 Spring Boot Actuator
curl -X POST http://localhost:8080/actuator/shutdown

# ── 强制关闭（不推荐） ──
kill -9 <PID>                   # SIGKILL：立即终止，无法执行清理逻辑
```

### 4.2 服务启动脚本模板

```bash
#!/bin/bash
# service.sh {start|stop|restart|status} —— Java 服务生命周期管理

set -euo pipefail

readonly APP_NAME="my-spring-app"
readonly APP_HOME="$(cd "$(dirname "$0")" && pwd)"
readonly JAR_FILE="${APP_HOME}/${APP_NAME}.jar"
readonly PID_FILE="${APP_HOME}/${APP_NAME}.pid"
readonly LOG_FILE="${APP_HOME}/logs/${APP_NAME}.log"

# JVM 参数
readonly JVM_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
readonly APP_OPTS="--spring.profiles.active=prod"

get_pid() {
    [[ -f "${PID_FILE}" ]] && cat "${PID_FILE}" || echo ""
}

is_running() {
    local pid=$(get_pid)
    [[ -n "${pid}" ]] && kill -0 "${pid}" 2>/dev/null
}

start() {
    if is_running; then
        echo "⚠️  ${APP_NAME} 已在运行 (PID: $(get_pid))"
        return 1
    fi

    echo "🚀 启动 ${APP_NAME}..."
    mkdir -p "$(dirname "${LOG_FILE}")"

    nohup java ${JVM_OPTS} -jar "${JAR_FILE}" ${APP_OPTS} \
        >> "${LOG_FILE}" 2>&1 &

    echo $! > "${PID_FILE}"
    echo "✅ 已启动 (PID: $(get_pid))"
}

stop() {
    local pid=$(get_pid)
    if [[ -z "${pid}" ]]; then
        echo "⚠️  ${APP_NAME} 未在运行"
        return 1
    fi

    echo "🛑 停止 ${APP_NAME} (PID: ${pid})..."

    # 优雅关闭
    kill -15 "${pid}"

    # 等待最多 60 秒
    local count=0
    while kill -0 "${pid}" 2>/dev/null && [[ ${count} -lt 60 ]]; do
        sleep 1
        ((count++))
    done

    # 仍未退出则强制关闭
    if kill -0 "${pid}" 2>/dev/null; then
        echo "⚠️  强制关闭..."
        kill -9 "${pid}"
    fi

    rm -f "${PID_FILE}"
    echo "✅ 已停止"
}

restart() {
    stop
    sleep 2
    start
}

status() {
    if is_running; then
        echo "✅ ${APP_NAME} 运行中 (PID: $(get_pid))"
    else
        echo "❌ ${APP_NAME} 未运行"
    fi
}

# ── 主流程 ──
case "${1:-}" in
    start)   start ;;
    stop)    stop ;;
    restart) restart ;;
    status)  status ;;
    *)       echo "用法: $0 {start|stop|restart|status}" ;;
esac
```

> ⚠️ **注意**：`nohup` 确保进程在终端关闭后继续运行。生产环境推荐使用 **systemd**（Linux）或容器编排（K8s）替代手工脚本管理。

### 4.3 systemd 服务配置

```ini
# /etc/systemd/system/my-spring-app.service
[Unit]
Description=My Spring Boot Application
After=network.target

[Service]
Type=simple
User=appuser
WorkingDirectory=/opt/myapp
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar /opt/myapp/app.jar
ExecStop=/bin/kill -15 $MAINPID
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

```bash
# 操作命令
sudo systemctl daemon-reload
sudo systemctl start my-spring-app
sudo systemctl enable my-spring-app     # 开机自启
sudo systemctl status my-spring-app
journalctl -u my-spring-app -f            # 查看日志
```

---

## 5. 文件操作与日志管理 {#5}

### 5.1 日志轮转脚本

```bash
#!/bin/bash
# log-rotate.sh —— 应用日志归档与清理

set -euo pipefail

readonly LOG_DIR="/opt/myapp/logs"
readonly ARCHIVE_DIR="${LOG_DIR}/archive"
readonly RETENTION_DAYS=30

mkdir -p "${ARCHIVE_DIR}"

# ── 按日期归档当天日志 ──
DATE=$(date +%Y%m%d)
for logfile in "${LOG_DIR}"/*.log; do
    [[ -f "${logfile}" ]] || continue
    base=$(basename "${logfile}")
    tar -czf "${ARCHIVE_DIR}/${base%.log}-${DATE}.tar.gz" "${logfile}"
    > "${logfile}"    # 清空原文件（不删除，避免应用找不到文件）
done

# ── 清理过期归档 ──
find "${ARCHIVE_DIR}" -name "*.tar.gz" -mtime +${RETENTION_DAYS} -delete

echo "✅ 日志轮转完成，保留 ${RETENTION_DAYS} 天"
```

### 5.2 配置文件备份脚本

```bash
#!/bin/bash
# config-backup.sh —— 配置文件备份与恢复

readonly BACKUP_DIR="/opt/backups/config"
readonly CONFIG_DIR="/opt/myapp/config"

backup() {
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_file="${BACKUP_DIR}/config-${timestamp}.tar.gz"

    mkdir -p "${BACKUP_DIR}"
    tar -czf "${backup_file}" -C "$(dirname "${CONFIG_DIR}")" "$(basename "${CONFIG_DIR}")"

    echo "✅ 配置已备份至: ${backup_file}"
}

restore() {
    local backup_file="$1"
    [[ -f "${backup_file}" ]] || { echo "❌ 备份文件不存在"; exit 1; }

    # 先备份当前配置
    backup

    tar -xzf "${backup_file}" -C "$(dirname "${CONFIG_DIR}")"
    echo "✅ 配置已恢复"
}

case "${1:-backup}" in
    backup)  backup ;;
    restore) restore "$2" ;;
    *)       echo "用法: $0 {backup|restore <file>}" ;;
esac
```

---

## 6. 跨平台脚本策略 {#6}

### 6.1 平台检测

```bash
#!/bin/bash
# 跨平台适配框架

detect_os() {
    case "$(uname -s)" in
        Linux*)     echo "linux" ;;
        Darwin*)    echo "macos" ;;
        CYGWIN*|MINGW*|MSYS*) echo "windows" ;;
        *)          echo "unknown" ;;
    esac
}

OS=$(detect_os)

# ── 按平台设置变量 ──
case "${OS}" in
    linux|macos)
        JAVA_CMD="java"
        PATH_SEP=":"
        FILE_SEP="/"
        ;;
    windows)
        JAVA_CMD="java.exe"
        PATH_SEP=";"
        FILE_SEP="\\"
        ;;
esac

echo "检测到平台: ${OS}"
"${JAVA_CMD}" -version
```

### 6.2 Windows PowerShell 等效脚本

```powershell
# run-app.ps1 —— Windows 下 Java 应用管理

param(
    [ValidateSet("start","stop","restart","status")]
    [string]$Action = "start"
)

$AppName = "my-spring-app"
$JarFile = ".\$AppName.jar"
$LogFile = ".\logs\$AppName.log"

function Start-App {
    Write-Host "🚀 启动 $AppName..."
    $process = Start-Process java `
        -ArgumentList "-Xms512m -Xmx2g -jar $JarFile" `
        -NoNewWindow -PassThru `
        -RedirectStandardOutput $LogFile `
        -RedirectStandardError $LogFile
    $process.Id | Out-File "$AppName.pid"
    Write-Host "✅ 已启动 (PID: $($process.Id))"
}

function Stop-App {
    if (Test-Path "$AppName.pid") {
        $pid = Get-Content "$AppName.pid"
        Stop-Process -Id $pid -Force
        Remove-Item "$AppName.pid"
        Write-Host "✅ 已停止"
    }
}

switch ($Action) {
    "start"   { Start-App }
    "stop"    { Stop-App }
    "restart" { Stop-App; Start-Sleep 2; Start-App }
}
```

### 6.3 跨平台策略总结

| 策略 | 适用场景 | 推荐度 |
|------|---------|:-----:|
| **WSL2** | Windows 开发者，生产环境为 Linux | ⭐⭐⭐⭐⭐ |
| **Git Bash** | Windows 上运行 Bash 脚本 | ⭐⭐⭐⭐ |
| **双脚本** | 同时维护 .sh 和 .ps1 | ⭐⭐⭐ |
| **Maven/Gradle 插件** | 通过构建工具抽象平台差异 | ⭐⭐⭐⭐⭐ |
| **Docker** | 完全消除环境差异 | ⭐⭐⭐⭐⭐ |

> 🎯 **核心要点**：现代 Java 项目最佳跨平台方案是 **WSL2 + Docker**——开发环境与生产环境完全一致，Shell 脚本只需写 Linux 版本。

---

## 7. Shell 脚本调试与错误处理 {#7}

### 7.1 调试技巧

```bash
#!/bin/bash

# ── 方法1: 启动时开启调试 ──
bash -x script.sh              # 打印每条执行的命令

# ── 方法2: 脚本内局部调试 ──
set -x   # 开启调试
# ... 需要调试的代码 ...
set +x   # 关闭调试

# ── 方法3: 打印关键信息 ──
echo "[DEBUG] 当前目录: $(pwd)"
echo "[DEBUG] JAVA_HOME: ${JAVA_HOME}"
echo "[DEBUG] 参数个数: $#"
```

### 7.2 错误处理模式

```bash
#!/bin/bash
set -euo pipefail

# ── 模式1: trap 捕获清理 ──
cleanup() {
    echo "🧹 清理临时文件..."
    rm -rf "${TEMP_DIR}"
}
trap cleanup EXIT                    # 脚本退出时执行
trap 'echo "❌ 第 $LINENO 行出错"' ERR  # 出错时打印行号

# ── 模式2: 函数返回值检查 ──
run_with_retry() {
    local max_attempts=${1:-3}
    local cmd="$2"

    for i in $(seq 1 $max_attempts); do
        if eval "$cmd"; then
            return 0
        fi
        echo "⚠️  第 ${i} 次尝试失败，${max_attempts}s 后重试..."
        sleep $((i * 5))
    done
    echo "❌ 重试 ${max_attempts} 次后仍失败"
    return 1
}

# ── 模式3: 锁文件防止重复执行 ──
readonly LOCK_FILE="/tmp/myapp-deploy.lock"
exec 200>"${LOCK_FILE}"
if ! flock -n 200; then
    echo "❌ 另一个部署进程正在运行"
    exit 1
fi

# ── 模式4: 超时控制 ──
timeout 300 ./long-running-build.sh || {
    echo "❌ 构建超时（300s）"
    exit 1
}
```

### 7.3 常见陷阱与规避

| 陷阱 | 问题 | 解决方案 |
|------|------|---------|
| 变量未加引号 | `$VAR` 含空格时被拆分 | 始终使用 `"$VAR"` |
| `cd` 失败未检查 | 后续操作在错误目录执行 | `cd /path || exit 1` |
| 管道隐藏错误 | `false \| true` 返回 0 | `set -o pipefail` |
| 未定义变量 | 拼写错误变量名返回空串 | `set -u` |
| `rm -rf $VAR/` | 变量为空时变成 `rm -rf /` | 先检查变量非空 |
| 信号处理缺失 | Ctrl+C 后临时文件残留 | `trap cleanup EXIT` |

> 🎯 **核心要点**：Shell 脚本的信条是 **"Fail Fast, Fail Loud"**——错误应立即暴露，不容忍任何静默失败。

---

**返回总览：** [00-脚本知识体系总览](./00-脚本知识体系总览.md) | **下一模块：** [02-JShell —— Java 原生脚本利器](./02-JShell-Java原生脚本利器.md)
