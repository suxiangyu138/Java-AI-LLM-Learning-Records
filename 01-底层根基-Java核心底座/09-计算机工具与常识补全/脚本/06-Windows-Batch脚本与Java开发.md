# Windows Batch (.bat) 脚本与 Java 开发

> `.bat` 是 Windows 原生的"自动化母语"——从 JAVA_HOME 配置到服务管理，从 Maven/Gradle Wrapper 到定时任务

## 📚 目录

1. [Batch 脚本基础语法](#1)
2. [Java 环境变量与编译运行](#2)
3. [Maven/Gradle Wrapper —— mvnw.cmd & gradlew.bat](#3)
4. [Java 服务管理脚本](#4)
5. [批量操作与文件处理](#5)
6. [定时任务与自动化](#6)
7. [Batch vs PowerShell 选型](#7)
8. [常见陷阱与调试技巧](#8)

---

## 1. Batch 脚本基础语法 {#1}

### 1.1 Batch 文件基本结构

```batch
@echo off
REM ──────────────────────────────────
REM  脚本名称: build-and-run.bat
REM  功能描述: 编译并运行 Java 项目
REM  作者: xxx
REM  日期: 2026-07-30
REM ──────────────────────────────────

setlocal enabledelayedexpansion

REM ── 变量定义（等号两边不能有空格！）──
set PROJECT_DIR=%~dp0
set SRC_DIR=%PROJECT_DIR%src
set BUILD_DIR=%PROJECT_DIR%build
set MAIN_CLASS=com.example.Main

REM ── 主流程 ──
if "%1"=="" (
    call :compile
    call :run
    goto :eof
)

if /i "%1"=="clean"   call :clean   & goto :eof
if /i "%1"=="compile" call :compile & goto :eof
if /i "%1"=="run"     call :run     & goto :eof

echo 用法: %~nx0 {clean^|compile^|run}
goto :eof

REM ── 函数（使用标签 + goto :eof 模拟）──
:compile
echo 🔨 编译中...
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
dir /s /b "%SRC_DIR%\*.java" > sources.txt
javac -d "%BUILD_DIR%" @sources.txt
del sources.txt
echo ✅ 编译完成
goto :eof

:run
echo 🚀 启动应用...
java -cp "%BUILD_DIR%" %MAIN_CLASS%
goto :eof

:clean
echo 🧹 清理构建目录...
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
goto :eof
```

> 🎯 **核心要点**：`@echo off` 关闭命令回显，`setlocal enabledelayedexpansion` 启用延迟变量展开，`%~dp0` 获取脚本所在目录——这是每个 .bat 脚本的标准开头。

### 1.2 变量与字符串操作

```batch
@echo off
setlocal enabledelayedexpansion

REM ── 变量赋值（= 两边不能有空格！）──
set NAME=Java Developer
set VERSION=21
echo Hello, %NAME% - JDK %VERSION%

REM ── 字符串拼接 ──
set FULL_PATH=%JAVA_HOME%\bin\java.exe
echo 完整路径: %FULL_PATH%

REM ── 字符串替换 ──
set URL=http://localhost:8080/api
set SECURE_URL=%URL:http=https%
echo 安全 URL: %SECURE_URL%

REM ── 子字符串提取 ──
set TIMESTAMP=20260730
set YEAR=%TIMESTAMP:~0,4%
set MONTH=%TIMESTAMP:~4,2%
set DAY=%TIMESTAMP:~6,2%
echo %YEAR%-%MONTH%-%DAY%

REM ── 延迟变量展开（在 for 循环内使用 !!）──
set COUNTER=0
for /l %%i in (1,1,5) do (
    set /a COUNTER+=1
    echo 计数: !COUNTER!       REM 必须用 !! 而非 %%
)

REM ── 数学运算 (set /a) ──
set /a RESULT=10 * 5 + 3
echo 结果: %RESULT%             REM 53
```

### 1.3 条件判断与控制流

```batch
@echo off

REM ── if 字符串比较 ──
if "%JAVA_HOME%"=="" (
    echo ❌ JAVA_HOME 未设置
    exit /b 1
)

REM ── if 数值比较 ──
set /a VERSION=21
if %VERSION% GEQ 17 (
    echo ✅ JDK 版本满足最低要求
) else (
    echo ❌ 需要 JDK 17 以上
)

REM ── if 文件/目录存在 ──
if exist "pom.xml" (
    echo Maven 项目
    call mvnw.cmd clean package
) else if exist "build.gradle" (
    echo Gradle 项目
    call gradlew.bat build
) else (
    echo 未检测到构建工具
)

REM ── for 循环遍历文件 ──
for %%f in (*.java) do (
    echo 发现文件: %%f
)

REM ── for 循环遍历列表 ──
for %%m in (dev staging production) do (
    echo 环境: %%m
)

REM ── for /f 解析命令输出 ──
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    echo Java 版本: %%v
)

REM ── 选择分支 (choice 命令) ──
choice /c YNC /m "确认部署到生产环境?"
if errorlevel 3 goto :cancel
if errorlevel 2 goto :cancel
if errorlevel 1 goto :deploy
```

| 比较操作符 | 含义 | 示例 |
|----------|------|------|
| `EQU` | 等于 (Equal) | `if %A% EQU 10` |
| `NEQ` | 不等于 (Not Equal) | `if %A% NEQ 0` |
| `LSS` | 小于 (Less) | `if %A% LSS 100` |
| `LEQ` | 小于等于 (Less or Equal) | `if %A% LEQ 50` |
| `GTR` | 大于 (Greater) | `if %A% GTR 0` |
| `GEQ` | 大于等于 (Greater or Equal) | `if %A% GEQ 17` |
| `/i` | 忽略大小写 | `if /i "%1"=="help"` |

---

## 2. Java 环境变量与编译运行 {#2}

### 2.1 JAVA_HOME 自动检测脚本

```batch
@echo off
setlocal

REM ── 自动检测本机已安装的 JDK ──
echo ============================================
echo   Java 环境检测
echo ============================================

REM 先检查 JAVA_HOME 是否已设置
if defined JAVA_HOME (
    echo [系统变量] JAVA_HOME = %JAVA_HOME%
    "%JAVA_HOME%\bin\java" -version 2>&1 | findstr /i "version"
    goto :check_path
)

REM 遍历常见安装路径
set "FOUND_JDK="
for %%d in (
    "C:\Program Files\Java\jdk-21"
    "C:\Program Files\Java\jdk-17"
    "C:\Program Files\Java\jdk-11"
    "C:\Program Files\Eclipse Adoptium\jdk-21.0.1.12-hotspot"
    "C:\Program Files\Microsoft\jdk-21.0.1.12-hotspot"
) do (
    if exist %%d\bin\java.exe (
        if not defined FOUND_JDK (
            set "FOUND_JDK=%%~d"
            echo [自动检测] JDK 路径: %%d
            "%%d\bin\java" -version 2>&1 | findstr /i "version"
        )
    )
)

if not defined FOUND_JDK (
    echo ❌ 未检测到已安装的 JDK，请手动设置 JAVA_HOME
    exit /b 1
)

REM 设置 JAVA_HOME 并加入 PATH
setx JAVA_HOME "%FOUND_JDK%" > nul
echo ✅ JAVA_HOME 已设置为: %FOUND_JDK%

:check_path
REM 验证 java 命令在 PATH 中
where java > nul 2>&1
if %errorlevel% neq 0 (
    echo ⚠️  java 不在 PATH 中，正在添加...
    setx PATH "%JAVA_HOME%\bin;%PATH%" > nul
    echo ✅ 已添加 java 到 PATH
)
echo 当前 java 命令路径:
where java
goto :eof
```

### 2.2 一键编译运行脚本

```batch
@echo off
setlocal enabledelayedexpansion

REM ── quick-run.bat —— Java 单文件/项目快速编译运行 ──

set "FILE=%1"
if "%FILE%"=="" (
    echo 用法: %~nx0 ^<JavaFile.java^> [args...]
    exit /b 1
)

REM 检测 JDK 版本
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set VERSION_STR=%%~v
)
set VERSION_STR=%VERSION_STR:"=%

REM JDK 11+ 可直接运行单文件
echo %VERSION_STR% | findstr /r "^1[1-9]\." > nul
if %errorlevel% equ 0 (
    echo 🚀 JDK 11+ 模式：直接运行单文件
    java "%FILE%" %2 %3 %4 %5
    goto :eof
)

REM JDK 8-10 传统编译+运行
echo 🔨 JDK 8 模式：先编译后运行
set "CLASS_NAME=%~n1"
javac "%FILE%"
if %errorlevel% neq 0 exit /b 1
java "%CLASS_NAME%" %2 %3 %4 %5
goto :eof
```

### 2.3 多模块项目构建

```batch
@echo off
setlocal enabledelayedexpansion

REM ── 按依赖顺序构建多模块项目 ──

echo ============================================
echo   多模块批量构建
echo ============================================

set MODULES=common api-contract service-user service-order gateway

REM 检测构建工具
if exist "mvnw.cmd" (
    set BUILD_CMD=call mvnw.cmd
    echo 构建工具: Maven Wrapper
) else if exist "gradlew.bat" (
    set BUILD_CMD=call gradlew.bat
    echo 构建工具: Gradle Wrapper
) else if exist "pom.xml" (
    set BUILD_CMD=call mvn
    echo 构建工具: Maven (系统安装)
) else (
    echo ❌ 未检测到构建工具
    exit /b 1
)

set FAILED=0
set PASSED=0

for %%m in (%MODULES%) do (
    echo.
    echo ━━━ 构建模块: %%m ━━━
    if exist "%%m\" (
        cd %%m
        %BUILD_CMD% clean package -DskipTests
        if !errorlevel! equ 0 (
            echo ✅ %%m 构建成功
            set /a PASSED+=1
        ) else (
            echo ❌ %%m 构建失败
            set /a FAILED+=1
        )
        cd ..
    ) else (
        echo ⚠️  模块目录 %%m 不存在，跳过
    )
)

echo.
echo ============================================
echo 结果: 成功 %PASSED% / 失败 %FAILED%
echo ============================================
if %FAILED% gtr 0 exit /b 1
goto :eof
```

---

## 3. Maven/Gradle Wrapper —— mvnw.cmd & gradlew.bat {#3}

### 3.1 mvnw.cmd 解析

`mvnw.cmd` 是 Maven Wrapper 的 Windows 入口，自动下载并运行指定版本的 Maven：

```batch
REM mvnw.cmd 的核心逻辑（简化版）

@REM 查找 JAVA_HOME
if defined JAVA_HOME goto findJavaFromJavaHome
set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute
...
:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe
if exist "%JAVA_EXE%" goto execute
...

:execute
@REM 用 wrapper.jar 下载对应版本 Maven 并执行
"%JAVA_EXE%" %JVM_CONFIG_MAVEN_PROPS% %MAVEN_OPTS% ^
    -classpath %WRAPPER_JAR% ^
    "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
    %WRAPPER_LAUNCHER% %MAVEN_CONFIG% %*
```

### 3.2 常见 Wrapper 使用场景

```batch
@echo off
REM ── 日常操作速查 ──

REM Maven Wrapper (mvnw.cmd)
mvnw.cmd clean compile                      REM 编译
mvnw.cmd clean package -DskipTests          REM 跳过测试打包
mvnw.cmd clean package -Pproduction         REM 生产 Profile 打包
mvnw.cmd clean test -pl service-user        REM 只测试指定模块
mvnw.cmd spring-boot:run                    REM 启动 Spring Boot
mvnw.cmd dependency:tree                    REM 查看依赖树

REM Gradle Wrapper (gradlew.bat)
gradlew.bat clean build                     REM 编译+测试+打包
gradlew.bat bootRun                         REM 启动 Spring Boot
gradlew.bat build --parallel                REM 并行构建
gradlew.bat build -x test                   REM 跳过测试
gradlew.bat dependencies                    REM 查看依赖树
```

### 3.3 版本升级脚本

```batch
@echo off
REM update-wrapper.bat —— 一键升级 Maven/Gradle Wrapper 版本

setlocal

set NEW_MAVEN_VERSION=3.9.9
set NEW_GRADLE_VERSION=8.9

echo ============================================
echo   Wrapper 版本升级工具
echo ============================================

if exist "mvnw.cmd" (
    echo [Maven] 升级到 %NEW_MAVEN_VERSION%...
    mvnw.cmd wrapper:wrapper -Dmaven=%NEW_MAVEN_VERSION%
    if %errorlevel% equ 0 (
        echo ✅ Maven Wrapper 已升级
    ) else (
        echo ❌ 升级失败
    )
)

if exist "gradlew.bat" (
    echo [Gradle] 升级到 %NEW_GRADLE_VERSION%...
    gradlew.bat wrapper --gradle-version %NEW_GRADLE_VERSION%
    if %errorlevel% equ 0 (
        echo ✅ Gradle Wrapper 已升级
    ) else (
        echo ❌ 升级失败
    )
)

goto :eof
```

---

## 4. Java 服务管理脚本 {#4}

### 4.1 Spring Boot 服务启停管理

```batch
@echo off
setlocal enabledelayedexpansion

REM ── service.bat {install^|start^|stop^|restart^|status} ──
REM 管理 Spring Boot JAR 服务

set APP_NAME=my-spring-app
set APP_HOME=%~dp0
set JAR_FILE=%APP_HOME%%APP_NAME%.jar
set LOG_DIR=%APP_HOME%logs
set LOG_FILE=%LOG_DIR%%APP_NAME%.log
set PID_FILE=%APP_HOME%%APP_NAME%.pid

REM JVM 参数
set JVM_OPTS=-Xms512m -Xmx2g -XX:+UseG1GC
set APP_OPTS=--spring.profiles.active=production

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

REM ── 主命令分发 ──
if /i "%1"=="start"   goto :start
if /i "%1"=="stop"    goto :stop
if /i "%1"=="restart" goto :restart
if /i "%1"=="status"  goto :status
if /i "%1"=="install" goto :install

echo 用法: %~nx0 {install^|start^|stop^|restart^|status}
goto :eof

:start
REM 检查是否已运行
if exist "%PID_FILE%" (
    set /p PID=<"%PID_FILE%"
    tasklist /fi "PID eq !PID!" 2>nul | find /i "!PID!" >nul
    if !errorlevel! equ 0 (
        echo ⚠️  %APP_NAME% 已在运行 (PID: !PID!)
        goto :eof
    )
    del "%PID_FILE%"
)

echo 🚀 启动 %APP_NAME%...

REM 使用 javaw（无控制台窗口）后台启动
start "!APP_NAME!" /B javaw !JVM_OPTS! -jar "!JAR_FILE!" !APP_OPTS! ^
    >> "!LOG_FILE!" 2>&1

REM 获取 PID（需要额外步骤，因为 start /B 不直接返回 PID）
timeout /t 3 /nobreak > nul
for /f "tokens=2" %%p in ('tasklist /fi "WINDOWTITLE eq !APP_NAME!" /fo list ^| findstr "PID:"') do (
    echo %%p > "!PID_FILE!"
    echo ✅ 已启动 (PID: %%p)
    goto :eof
)

echo ⚠️  无法获取 PID，但进程已启动
goto :eof

:stop
if not exist "%PID_FILE%" (
    echo ⚠️  %APP_NAME% 未在运行
    goto :eof
)

set /p PID=<"%PID_FILE%"
echo 🛑 停止 %APP_NAME% (PID: %PID%)...

REM 优雅关闭
taskkill /pid %PID% > nul 2>&1

REM 等待最多 30 秒
set COUNT=0
:wait_stop
timeout /t 1 /nobreak > nul
set /a COUNT+=1
tasklist /fi "PID eq %PID%" 2>nul | find /i "%PID%" >nul
if %errorlevel% neq 0 goto :stopped
if %COUNT% lss 30 goto :wait_stop

REM 强制关闭
echo ⚠️  强制关闭...
taskkill /f /pid %PID% > nul 2>&1

:stopped
del "%PID_FILE%" 2>nul
echo ✅ 已停止
goto :eof

:restart
call :stop
timeout /t 3 /nobreak > nul
call :start
goto :eof

:status
if not exist "%PID_FILE%" (
    echo ❌ %APP_NAME% 未运行
    goto :eof
)

set /p PID=<"%PID_FILE%"
tasklist /fi "PID eq %PID%" 2>nul | find /i "%PID%" >nul
if %errorlevel% equ 0 (
    echo ✅ %APP_NAME% 运行中 (PID: %PID%)
) else (
    echo ❌ %APP_NAME% 已停止（PID 文件过期）
    del "%PID_FILE%"
)
goto :eof

:install
REM 安装为 Windows 服务（使用 nssm 或 sc）
echo 🔧 安装 Windows 服务...
where nssm > nul 2>&1
if %errorlevel% neq 0 (
    echo ⚠️  需要 nssm (Non-Sucking Service Manager)
    echo 下载: https://nssm.cc/download
    echo 或使用: choco install nssm
    goto :eof
)
nssm install %APP_NAME% "%JAVA_HOME%\bin\javaw.exe" ^
    %JVM_OPTS% -jar "%JAR_FILE%" %APP_OPTS%
nssm set %APP_NAME% AppDirectory "%APP_HOME%"
nssm set %APP_NAME% AppStdout "%LOG_FILE%"
nssm set %APP_NAME% AppStderr "%LOG_FILE%"
echo ✅ 服务已安装: %APP_NAME%
goto :eof
```

### 4.2 使用 nssm 注册 Windows 服务

```batch
@echo off
REM install-as-service.bat —— 将 JAR 注册为 Windows 服务

set SERVICE_NAME=MyJavaService
set SERVICE_DESC=My Java Application Service
set JAR_PATH=C:\app\myapp.jar
set JAVA_OPTS=-Xms512m -Xmx2g
set APP_OPTS=--spring.profiles.active=production

REM 安装服务
nssm install "%SERVICE_NAME%" "%JAVA_HOME%\bin\javaw.exe" ^
    %JAVA_OPTS% -jar "%JAR_PATH%" %APP_OPTS%

REM 配置服务属性
nssm set "%SERVICE_NAME%" DisplayName "%SERVICE_DESC%"
nssm set "%SERVICE_NAME%" Description "%SERVICE_DESC%"
nssm set "%SERVICE_NAME%" Start SERVICE_AUTO_START
nssm set "%SERVICE_NAME%" AppDirectory "C:\app"
nssm set "%SERVICE_NAME%" AppStdout "C:\app\logs\stdout.log"
nssm set "%SERVICE_NAME%" AppStderr "C:\app\logs\stderr.log"
nssm set "%SERVICE_NAME%" AppRotateFiles 1
nssm set "%SERVICE_NAME%" AppRotateSeconds 86400

echo ✅ 服务已安装
echo 启动服务: net start %SERVICE_NAME%
echo 停止服务: net stop %SERVICE_NAME%
echo 卸载服务: nssm remove %SERVICE_NAME% confirm
goto :eof
```

---

## 5. 批量操作与文件处理 {#5}

### 5.1 日志清理与归档

```batch
@echo off
setlocal enabledelayedexpansion

REM ── clean-logs.bat —— 日志文件归档与过期清理 ──

set LOG_DIR=C:\app\logs
set ARCHIVE_DIR=%LOG_DIR%\archive
set RETENTION_DAYS=30

if not exist "%ARCHIVE_DIR%" mkdir "%ARCHIVE_DIR%"

REM 获取今天日期
for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do (
    set DATETIME=%%I
)
set TODAY=%DATETIME:~0,8%

echo ============================================
echo 日志清理 - %TODAY%
echo ============================================

REM 归档当天日志（大于 10MB 的文件先压缩再清理）
for %%f in ("%LOG_DIR%\*.log") do (
    set FILESIZE=%%~zf
    if !FILESIZE! gtr 10485760 (
        echo 归档: %%~nxf (!FILESIZE! 字节^)
        powershell -Command ^
            "Compress-Archive -Path '%%f' -DestinationPath '%ARCHIVE_DIR%\%%~nf-%TODAY%.zip'"
        type nul > "%%f"   REM 清空日志文件
    )
)

REM 删除过期归档（使用 PowerShell 处理日期计算）
powershell -Command ^
    "Get-ChildItem '%ARCHIVE_DIR%' -Filter '*.zip' | ^
     Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-%RETENTION_DAYS%) } | ^
     Remove-Item -Force"

echo ✅ 日志清理完成
goto :eof
```

### 5.2 批量 JAR 包更新

```batch
@echo off
setlocal enabledelayedexpansion

REM ── update-jars.bat —— 批量更新 lib 目录下的 JAR 包 ──

set LIB_DIR=.\lib
set BACKUP_DIR=.\lib_backup_%date:~0,4%%date:~5,2%%date:~8,2%

echo ============================================
echo   JAR 包批量更新工具
echo ============================================

REM 备份现有 JAR
if exist "%LIB_DIR%" (
    echo 📦 备份现有 JAR 到 %BACKUP_DIR%...
    xcopy "%LIB_DIR%" "%BACKUP_DIR%\" /e /i /y > nul
    echo ✅ 备份完成
)

REM 列出当前 JAR 版本
echo.
echo 📋 当前 JAR 清单:
for %%f in ("%LIB_DIR%\*.jar") do (
    echo   %%~nxf  (%TIMESTAMP%)
)

REM 检查是否有 new-jars 目录
if not exist "new-jars\" (
    echo.
    echo ⚠️  请将新版本 JAR 放入 new-jars 目录后重新运行
    goto :eof
)

REM 替换 JAR
set UPDATED=0
set SKIPPED=0
for %%f in ("new-jars\*.jar") do (
    if exist "%LIB_DIR%\%%~nxf" (
        copy /y "%%f" "%LIB_DIR%\%%~nxf" > nul
        echo 🔄 更新: %%~nxf
        set /a UPDATED+=1
    ) else (
        copy "%%f" "%LIB_DIR%\" > nul
        echo ➕ 新增: %%~nxf
        set /a UPDATED+=1
    )
)

echo.
echo ============================================
echo 更新完成: %UPDATED% 个文件
echo 备份目录: %BACKUP_DIR%
echo ============================================
goto :eof
```

### 5.3 配置文件批量修改

```batch
@echo off
setlocal enabledelayedexpansion

REM ── replace-in-files.bat —— 批量替换目录中所有 properties 文件的配置值 ──

set "SEARCH=%1"
set "REPLACE=%2"
set "FILE_PATTERN=*.properties"

if "%SEARCH%"=="" (
    echo 用法: %~nx0 ^<搜索内容^> ^<替换内容^> [文件模式]
    echo 示例: %~nx0 localhost prod-server *.properties
    exit /b 1
)
if not "%3"=="" set "FILE_PATTERN=%3"

echo ============================================
echo  批量替换: "%SEARCH%" → "%REPLACE%"
echo  文件模式: %FILE_PATTERN%
echo ============================================

set COUNT=0
for /r %%f in (%FILE_PATTERN%) do (
    echo 处理: %%f

    REM 使用 PowerShell 进行原地替换
    powershell -Command ^
        "(Get-Content '%%f' -Raw) -replace '%SEARCH%', '%REPLACE%' | Set-Content '%%f' -NoNewline"

    if !errorlevel! equ 0 (
        set /a COUNT+=1
    )
)

echo.
echo ✅ 完成，处理了 %COUNT% 个文件
goto :eof
```

---

## 6. 定时任务与自动化 {#6}

### 6.1 Windows Task Scheduler (schtasks)

```batch
@echo off
REM ── 使用 schtasks 创建定时任务 ──

set SCRIPT_PATH=%~dp0deploy.bat
set TASK_NAME=DailyDeploy

REM 创建每天凌晨 2:00 执行的任务
schtasks /create ^
    /tn "%TASK_NAME%" ^
    /tr "cmd.exe /c \"%SCRIPT_PATH%\"" ^
    /sc daily ^
    /st 02:00 ^
    /ru SYSTEM ^
    /f

echo ✅ 定时任务已创建: %TASK_NAME%

REM 其他常用调度模式：
REM /sc minute /mo 30          每 30 分钟
REM /sc hourly /mo 4           每 4 小时
REM /sc weekly /d MON,WED,FRI  每周一、三、五
REM /sc onstart                系统启动时
REM /sc onlogon                用户登录时

REM 查看所有任务
REM schtasks /query /fo LIST /v

REM 删除任务
REM schtasks /delete /tn "%TASK_NAME%" /f
```

### 6.2 循环监控脚本

```batch
@echo off
setlocal enabledelayedexpansion

REM ── watch-service.bat —— 监控 Java 服务健康状态，异常自动重启 ──

set HEALTH_URL=http://localhost:8080/actuator/health
set CHECK_INTERVAL=30
set MAX_RESTART=3
set RESTART_COUNT=0

echo ============================================
echo  服务监控已启动
echo  检查间隔: %CHECK_INTERVAL% 秒
echo  最大重启次数: %MAX_RESTART%
echo ============================================

:loop
REM 执行健康检查（使用 PowerShell 发起 HTTP 请求）
powershell -Command ^
    "try { $r = Invoke-WebRequest -Uri '%HEALTH_URL%' -TimeoutSec 5 -UseBasicParsing; ^
      if ($r.StatusCode -eq 200) { exit 0 } else { exit 1 } } ^
     catch { exit 1 }"

if %errorlevel% equ 0 (
    echo [%date% %time%] ✅ 服务正常
    set RESTART_COUNT=0
    goto :wait
)

REM 健康检查失败
echo [%date% %time%] ❌ 服务异常

if !RESTART_COUNT! geq %MAX_RESTART% (
    echo [%date% %time%] 🚨 已达最大重启次数 %MAX_RESTART%，停止监控
    goto :eof
)

set /a RESTART_COUNT+=1
echo [%date% %time%] 🔄 第 !RESTART_COUNT! 次重启...
call restart-service.bat

:wait
timeout /t %CHECK_INTERVAL% /nobreak > nul
goto :loop
```

### 6.3 构建 + 部署一体化脚本

```batch
@echo off
setlocal enabledelayedexpansion

REM ── ci-local.bat —— 本地模拟 CI/CD 流程 ──

set "ENV=%1"
if "%ENV%"=="" set ENV=dev

echo ================================================
echo   CI/CD 本地模拟 - 环境: %ENV%
echo   %date% %time%
echo ================================================

REM 阶段1: 代码检查
echo.
echo ━━━ 阶段1: 代码静态检查 ━━━
echo 检查是否有未提交的代码...
git diff --quiet
if %errorlevel% neq 0 (
    echo ⚠️  存在未提交的更改:
    git status --short
)

REM 阶段2: 构建
echo.
echo ━━━ 阶段2: 构建 ━━━
set BUILD_START=%time%

if exist "mvnw.cmd" (
    call mvnw.cmd clean package -DskipTests -P%ENV%
) else if exist "gradlew.bat" (
    call gradlew.bat clean build -x test
) else (
    echo ❌ 未找到构建工具
    exit /b 1
)

if %errorlevel% neq 0 (
    echo ❌ 构建失败
    exit /b 1
)
echo ✅ 构建成功

REM 阶段3: 测试
echo.
echo ━━━ 阶段3: 运行测试 ━━━
if exist "mvnw.cmd" (
    call mvnw.cmd test
) else (
    call gradlew.bat test
)

if %errorlevel% neq 0 (
    echo ❌ 测试失败
    exit /b 1
)
echo ✅ 测试通过

REM 阶段4: 打包
echo.
echo ━━━ 阶段4: 准备部署包 ━━━
set DEPLOY_DIR=.\deploy\%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%
mkdir "%DEPLOY_DIR%" 2>nul

REM 复制 JAR 和配置文件
for %%f in (target\*.jar build\libs\*.jar) do (
    if exist "%%f" (
        copy "%%f" "%DEPLOY_DIR%\" > nul
        echo 📦 %%~nxf
    )
)

if exist "docker-compose.yml" copy "docker-compose.yml" "%DEPLOY_DIR%\" > nul
if exist "Dockerfile"         copy "Dockerfile"         "%DEPLOY_DIR%\" > nul
if exist "config\"             xcopy "config" "%DEPLOY_DIR%\config\" /e /i /y /q > nul

echo.
echo ================================================
echo ✅ 构建完成！部署包位置: %DEPLOY_DIR%
echo ================================================
goto :eof
```

---

## 7. Batch vs PowerShell 选型 {#7}

### 7.1 能力对比

| 维度 | Batch (.bat/.cmd) | PowerShell (.ps1) |
|------|:---:|:---:|
| **出现年代** | 1980s (MS-DOS) | 2006 (Windows PowerShell) |
| **Windows 最低版本** | 所有版本 | Windows 7 / Server 2008 R2+ |
| **跨平台** | ❌ 仅 Windows | ✅ Windows / Linux / macOS (PS Core) |
| **语法复杂度** | 简单但晦涩 | 丰富但学习曲线较陡 |
| **字符串处理** | ⭐⭐ 有限 | ⭐⭐⭐⭐⭐ 强大 |
| **JSON/XML 处理** | ❌ 需要外部工具 | ✅ 原生支持 |
| **HTTP 请求** | ❌ 基本不可能 | ✅ `Invoke-WebRequest` |
| **面向对象管道** | ❌ 纯文本管道 | ✅ 对象管道 |
| **异常处理** | ⚠️ `errorlevel` 检查 | ✅ `try/catch/finally` |
| **远程执行** | ❌ | ✅ `Invoke-Command` |
| **执行策略限制** | 无限制 | ⚠️ 默认 Restricted |
| **启动速度** | ⭐⭐⭐⭐⭐ 极快 | ⭐⭐⭐ 稍慢 |

### 7.2 场景选型建议

| 场景 | 推荐 | 理由 |
|------|:---:|------|
| **简单文件操作** | Batch | 最快、最简 |
| **Maven/Gradle Wrapper** | Batch | 业界标准 (mvnw.cmd / gradlew.bat) |
| **环境变量配置** | Batch | 兼容性最好 |
| **HTTP API 调用** | PowerShell | 原生 HTTP 支持 |
| **JSON 数据处理** | PowerShell | `ConvertFrom-Json` |
| **复杂业务逻辑** | PowerShell | 异常处理 + 对象管道 |
| **Windows 系统管理** | PowerShell | 完整的 WMI/CIM 访问 |
| **跨平台脚本** | PowerShell Core | 一次编写，到处运行 |
| **历史遗留系统** | Batch | 兼容 WinXP/Server 2003 |
| **CI/CD 工具** | 两者都行 | GitHub Actions 上 PowerShell 更方便 |

```batch
REM ── Batch 中调用 PowerShell 处理复杂逻辑 ──
@echo off
echo Batch 负责流程编排，PowerShell 负责复杂计算

REM 用 PowerShell 解析 JSON
powershell -Command ^
    "$config = Get-Content 'config.json' | ConvertFrom-Json; ^
     Write-Output $config.database.host"

REM 用 PowerShell 调用 HTTP API
powershell -Command ^
    "$r = Invoke-RestMethod -Uri 'http://localhost:8080/api/status'; ^
     if ($r.status -ne 'UP') { exit 1 }"

echo ✅ 混合脚本执行完成
```

> 🎯 **核心要点**：不要做"纯 Batch 或纯 PowerShell"的二选一——**Batch 做流程编排，PowerShell 做复杂计算**，这是 Windows 上最务实的脚本策略。

---

## 8. 常见陷阱与调试技巧 {#8}

### 8.1 十大常见陷阱

| # | 陷阱 | 错误写法 | 正确写法 |
|---|------|---------|---------|
| 1 | 变量赋值有空格 | `set VAR = value` | `set VAR=value` |
| 2 | 路径有空格未加引号 | `cd %PROGRAM_FILES%` | `cd "%PROGRAM_FILES%"` |
| 3 | for 循环内用 `%var%` | `echo %i%` | `echo !i!` (需 `enabledelayedexpansion`) |
| 4 | `if` 括号不成对 | `if "%VAR%"=="" echo empty` | `if "%VAR%"=="" (echo empty)` |
| 5 | `errorlevel` 检查顺序错位 | 先判断 `errorlevel 0` | 从大到小判断，或用 `%errorlevel%` |
| 6 | `%` 转义错误 | `echo 100%` | `echo 100%%` |
| 7 | 变量在复合语句中不更新 | `set VAR=1 & echo %VAR%` | `set VAR=1 & call echo %%VAR%%` |
| 8 | 中文编码问题 | 文件存为 UTF-8 | 存为 ANSI 或 GBK，或用 `chcp 65001` |
| 9 | 路径结尾反斜杠 | `xcopy src\ dest\` | `xcopy "src" "dest\"` |
| 10 | `goto :eof` 遗漏 | 函数最后缺少 `goto :eof` | 每个函数必须显式 `goto :eof` |

### 8.2 调试技巧

```batch
@echo off

REM ── 技巧1: 回显模式 ──
@echo on                              REM 开启命令回显（调试时）
REM ...调试的代码...
@echo off                             REM 关闭回显

REM ── 技巧2: 条件调试输出 ──
set DEBUG=1
if defined DEBUG (
    echo [DEBUG] 当前目录: %cd%
    echo [DEBUG] JAVA_HOME: %JAVA_HOME%
    echo [DEBUG] 参数个数: %*
)

REM ── 技巧3: 暂停查看中间状态 ──
echo 按任意键继续查看下一步...
pause > nul

REM ── 技巧4: 记录所有输出到文件 ──
REM 运行时: script.bat > log.txt 2>&1

REM ── 技巧5: 断言检查 ──
if "%JAVA_HOME%"=="" (
    echo [ASSERT FAIL] JAVA_HOME 未设置
    echo 调用堆栈: 行号不可用（Batch 的遗憾）
    exit /b 99
)
```

### 8.3 错误码规范

```batch
@echo off
REM ── 统一的错误码定义 ──

REM 0   - 成功
REM 1   - 通用错误
REM 2   - 参数错误
REM 3   - 环境变量未设置
REM 4   - 依赖工具缺失
REM 5   - 文件不存在
REM 6   - 网络错误
REM 10  - 服务启动失败
REM 11  - 服务停止超时

set EXIT_SUCCESS=0
set EXIT_GENERAL_ERROR=1
set EXIT_BAD_ARGUMENT=2
set EXIT_ENV_NOT_SET=3
set EXIT_MISSING_TOOL=4
set EXIT_FILE_NOT_FOUND=5
set EXIT_NETWORK_ERROR=6
set EXIT_SERVICE_START_FAILED=10
set EXIT_SERVICE_STOP_TIMEOUT=11

REM 使用示例
if not defined JAVA_HOME (
    echo ❌ JAVA_HOME 未设置
    exit /b %EXIT_ENV_NOT_SET%
)
```

> 🎯 **核心要点**：Batch 是 Windows 上最"古老"但也是最"稳妥"的自动化方式——Maven Wrapper、Gradle Wrapper 都用 `.cmd`/`.bat`。掌握 Batch 语法是 Windows 平台 Java 开发者的必修课。

---

**返回总览：** [00-脚本知识体系总览](./00-脚本知识体系总览.md) | **上一模块：** [05-脚本安全与最佳实践](./05-脚本安全与最佳实践.md)
