# 快速学会 BAT 脚本
BAT = Windows 原生批处理脚本，后缀 `.bat`，**无需装软件、双击直接运行、CMD 原生支持**，适合自动化、批量处理、快捷启动、清理文件、本地开发自动化。

---

## 一、基础入门

### 1. 新建&运行
1. 桌面新建文本文档，改后缀为 `test.bat`
2. 编辑写入代码，保存
3. 双击直接运行 / CMD 中执行：`test.bat`

### 2. 首行常用
```bat
@echo off
```
- `@`：不显示当前本行命令
- `echo off`：关闭所有命令打印，只看输出

### 3. 暂停&防止闪退
```bat
pause
```
脚本执行完黑框一闪而过？结尾加 `pause`。

---

## 二、输出 & 注释
```bat
@echo off
:: 这是BAT单行注释（推荐）
rem 这也是注释
echo 你好，BAT脚本
```

---

## 三、变量（核心）

### 1. 定义&使用
```bat
@echo off
set name=su
set num=2026

echo 用户名：%name%
echo 数字：%num%
```
规则：
- `set 变量名=值`，**等号两边不能有空格**
- 取值用 `%变量%`

### 2. 输入交互
```bat
@echo off
set /p msg=请输入内容：
echo 你输入的是：%msg%
```

---

## 四、算数运算
```bat
@echo off
set /a a=10
set /a b=5
set /a c=a+b

echo 结果：%c%
```
`set /a` 专门做整数计算：`+ - * / %`

---

## 五、条件判断 if

### 1. 基础判断
```bat
@echo off
set age=20

if %age% geq 18 (
    echo 已成年
) else (
    echo 未成年
)
```

### 2. 常用比较符
```
==        等于
neq       不等于
gtr       大于
lss       小于
geq       大于等于
leq       小于等于
```

### 3. 文件/文件夹判断（高频）
```bat
:: 判断文件是否存在
if exist test.txt (
    echo 文件存在
)

:: 判断文件夹
if exist D:\tools\ (
    echo 文件夹存在
)
```

### 4. 判断上一条命令是否成功
```bat
if %errorlevel% equ 0 (
    echo 执行成功
)
```

---

## 六、循环 for（批量处理神器）

### 1. 遍历当前目录所有文件
```bat
@echo off
for %%i in (*.*) do (
    echo 文件：%%i
)
```
> BAT文件内循环变量必须用 `%%i`，CMD直接执行用 `%i`

### 2. 遍历指定后缀（批量删/复制）
```bat
:: 遍历所有txt
for %%f in (*.txt) do echo %%f
```

### 3. 数字区间循环
```bat
for /l %%i in (1,1,5) do (
    echo 序号：%%i
)
```
格式：`(起始,步长,结束)`

---

## 七、常用文件/目录操作
```bat
:: 创建文件夹
md mydir

:: 删除文件
del test.txt

:: 强制删除文件夹+所有内容
rd /s /q mydir

:: 复制文件
copy a.txt D:\backup\

:: 移动文件
move a.txt D:\target\
```

---

## 八、跳转 & 标签
BAT 没有函数，用 `:标签 + goto` 实现分段
```bat
@echo off
echo 开始执行
goto end

:test
echo 这是标签内容

:end
echo 结束
pause
```

---

## 九、后台运行 & 调用其他程序
```bat
:: 打开软件
start "" "D:\xxx\xxx.exe"

:: 打开网址
start https://github.com

:: 调用另一个bat
call other.bat
```

---

## 十、实用符号
```bat
:: 换行空行
echo.

:: 静默执行、不输出
>nul 2>&1

:: 拼接多条命令
dir && echo 执行成功
```

---

## 十一、开发高频实战脚本（直接复制即用）

### 1. 一键清理日志/缓存
```bat
@echo off
echo 正在清理临时文件...
del /f /s /q *.log
del /f /s /q *.tmp
echo 清理完成
pause
```

### 2. 一键启动 SpringBoot Jar
```bat
@echo off
@echo 启动Java项目
java -jar app.jar
pause
```

### 3. 检测文件夹是否存在，不存在则创建
```bat
@echo off
if not exist D:\docker\data (
    md D:\docker\data
    echo 目录创建成功
) else (
    echo 目录已存在
)
pause
```

---

## 十二、BAT 快速学习路线
1. 必背：`@echo off`、变量 `set`、`if`、`for`、`pause`
2. 掌握：文件判断 `exist`、文件增删改
3. 实战：写3个脚本
   - 垃圾清理脚本
   - 项目一键启动脚本
   - 批量重命名/复制脚本
4. 结合 Windows 任务计划程序，实现定时自动化

---
