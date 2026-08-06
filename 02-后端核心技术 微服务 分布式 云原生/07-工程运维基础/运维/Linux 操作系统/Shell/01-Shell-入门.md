# 01 - Shell 入门

> 面向 Java 后端开发者的 Shell / Bash 快速上手指南。涵盖环境搭建、脚本编写、调试技巧与常见问题。

---


## 📚 目录

1. [Shell 概述](#1-shell-概述)
2. [Bash 环境搭建](#2-bash-环境搭建)
3. [第一个 Shell 脚本](#3-第一个-shell-脚本)
4. [基础语法](#4-基础语法)
5. [脚本执行与调试](#5-脚本执行与调试)
6. [FAQ / 常见问题](#6-faq-常见问题)

---

## 1. Shell 概述

**Shell** 是用户与操作系统内核之间的命令行解释器。对于 Java 后端开发者，Shell 用于：

- 启动 / 停止 Java 进程（`java -jar`）
- 编写 CI/CD 脚本（编译、打包、部署）
- 日志分析、文件操作、批量处理
- 配置服务器环境（`$JAVA_HOME`、`$PATH`）

常见 Shell：**Bash**（最主流）、Zsh、Fish。本文件以 Bash 为准。

---

## 2. Bash 环境搭建

### 2.1 查看当前 Shell

```bash
echo "$SHELL"
echo "$BASH_VERSION"
```

### 2.2 `$PATH` 与 `~/.bashrc`

`$PATH` 决定 Shell 去哪里找可执行文件。Java 开发者需确保 `$JAVA_HOME/bin` 在 `$PATH` 中。

编辑 `~/.bashrc`（或 `~/.bash_profile` / `~/.zshrc`）：

```bash
# ~/.bashrc 示例
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export MAVEN_HOME=/opt/apache-maven-3.9
export PATH=$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH
```

使配置生效：

```bash
source ~/.bashrc
# 或
. ~/.bashrc
```

验证：

```bash
java -version
mvn -version
echo "$PATH"
```

---

## 3. 第一个 Shell 脚本

### 3.1 Shebang（`#!`）

脚本第一行指定解释器：

```bash
#!/bin/bash
```

### 3.2 创建并运行

```bash
cat > hello.sh << 'EOF'
#!/bin/bash
# 我的第一个 Shell 脚本
echo "Hello, Shell!"
echo "当前目录: $(pwd)"
EOF
```

**添加执行权限**：

```bash
chmod +x hello.sh
```

**四种执行方式**：

| 方式 | 命令 | 说明 |
|------|------|------|
| 相对路径 | `./hello.sh` | 需要 `x` 权限，会读取 shebang |
| 绝对路径 | `/path/to/hello.sh` | 同上 |
| 解释器直接执行 | `bash hello.sh` | 不需要 `x` 权限，忽略 shebang |
| `source` 执行 | `source hello.sh`（或 `. hello.sh`） | 在当前 Shell 进程运行，变量会保留 |

> 注意：`./hello.sh` 依赖 shebang；`bash hello.sh` 始终用 Bash 执行。

### 3.3 脚本入门模板

```bash
#!/bin/bash
set -e          # 任何命令失败即退出
set -u          # 使用未定义变量时报错
set -o pipefail # 管道中任一命令失败即视为整体失败

# 或简写为
set -euo pipefail

echo "脚本开始"
# ... 业务逻辑 ...
echo "脚本结束"
```

---

## 4. 基础语法

### 4.1 变量

```bash
# 赋值（等号两侧无空格）
name="world"
count=10
greeting="Hello, $name!"   # 双引号内插值
greeting2='Hello, $name!'  # 单引号原样输出

# 使用变量
echo "$greeting"
echo "${name}_suffix"       # 花括号明确边界

# 命令替换
now=$(date)
files_count=$(ls | wc -l)

# 特殊变量
echo "脚本名: $0"
echo "参数个数: $#"
echo "所有参数: $*"
echo "第一个参数: $1"
echo "第二个参数: $2"
echo "上条命令退出码: $?"
echo "当前 PID: $$"
```

### 4.2 注释

```bash
# 单行注释

: '
多行注释
第二行
'
```

### 4.3 字符串操作

```bash
str="hello-world"
echo "${#str}"          # 长度: 11
echo "${str:0:5}"       # 子串: hello
echo "${str/world/java}" # 替换: hello-java
echo "${str/-/}"        # 删除第一个 -: helloworld
```

### 4.4 退出码

```bash
command_succeeds  && echo "成功"   # 前一条成功才执行
command_fails     || echo "失败"   # 前一条失败才执行
command           ;  echo "总是执行"
```

---

## 5. 脚本执行与调试

### 5.1 执行方法对比

| 方法 | 新进程 | 需要 `x` 权限 | 变量影响父 Shell |
|------|--------|----------------|------------------|
| `./script.sh` | 是 | 是 | 否 |
| `bash script.sh` | 是 | 否 | 否 |
| `source script.sh` | 否 | 否 | **是** |
| `. script.sh` | 否 | 否 | **是** |

### 5.2 调试技巧

```bash
# 全局调试
#!/bin/bash
set -x   # 跟踪执行，打印每条命令（含展开后）
set -e   # 出错即停

# 局部调试
set -x
# ... 需要调试的代码 ...
set +x

# bash -x 方式
# bash -x script.sh arg1 arg2
```

```bash
# 调试示例
#!/bin/bash
set -ex
user="admin"
echo "User: $user"
```

---

## 6. FAQ / 常见问题

### Q1: `Permission denied`

**原因**：文件没有执行权限。  
**解决**：

```bash
chmod +x script.sh
# 或仅用 bash 执行
bash script.sh
```

### Q2: `command not found: java`

**原因**：`$JAVA_HOME` 未正确配置。  
**解决**：

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH
# 写入 ~/.bashrc 使其永久生效
```

### Q3: 脚本中 `cd` 后目录没有改变？

**原因**：用 `./script.sh` 执行会在**子 Shell** 中运行，`cd` 不影响当前终端。  
**解决**：用 `source script.sh` 或 `. script.sh` 执行，或在脚本末尾执行目标操作（如 `cd /deploy && java -jar app.jar`）。

### Q4: 变量赋值 `name = "world"` 报错

**原因**：等号两侧不能有空格。  
**解决**：写为 `name="world"`。

### Q5: `set -e` 导致脚本中途退出

**原因**：某条命令返回非零退出码。  
**解决**：如果预期某条命令可能失败但不希望终止，在其后加 `|| true`：

```bash
grep "pattern" file.log || true
```

### Q6: Windows 上编写的脚本在 Linux 运行出错（`^M`）

**原因**：Windows 换行符 `\r\n` 与 Linux `\n` 不兼容。  
**解决**：

```bash
# Linux 上转换
sed -i 's/\r$//' script.sh
# 或使用 dos2unix
dos2unix script.sh
```

---

**上一模块**：[05-Linux 服务管理](../Linux/05-Linux-服务管理.md) ｜ **下一模块**：[02-Shell 变量与类型](02-Shell-变量与类型.md) ｜ **返回总览**：[00-知识体系总览](00-Shell知识体系总览.md)

**【参考来源】**
- Bash 官方手册：https://www.gnu.org/software/bash/manual/\n- ShellCheck（脚本静态检查）：https://www.shellcheck.net/
