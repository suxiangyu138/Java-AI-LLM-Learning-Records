# 00 - Shell 知识体系总览

> Shell 是运维的母语：6 篇文档从脚本基础到文本处理三剑客，覆盖变量/流程/函数/实战，产出可维护的自动化运维脚本。运行环境为 Linux（关联体系见 [Linux 知识体系总览](../Linux/00-Linux知识体系总览.md)）。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)

## 1. 知识体系导图

```text
Shell 脚本编程体系（6 篇）
│
├── 01-Shell-入门          # 概述/环境搭建/Shebang/脚本基础/调试
├── 02-Shell-变量与类型     # 变量/数组/字符串/环境变量/declare
├── 03-Shell-流程控制       # if/case/for/while/until/select
├── 04-Shell-函数与实战     # 函数/参数/返回值/部署备份脚本/子进程
├── 05-Shell-文本处理       # grep/sed/awk 三剑客 + 正则/管道
│
└── 📌 00-Shell知识体系总览  # ← 本文件
```

> 🔗 **关联体系**：[Linux 运维](../Linux/00-Linux知识体系总览.md)（独立体系，12 篇）——命令速查、服务管理、JVM 线上排查。

## 2. 模块导航

| # | 模块 | 核心内容 | 级别 |
|:---:|------|---------|:---:|
| 01 | [Shell 入门](01-Shell-入门.md) | 概述/Bash 环境/Shebang/变量基础/执行方式/调试 | ⭐ |
| 02 | [Shell 变量与类型](02-Shell-变量与类型.md) | 变量/引号/特殊变量/数组/字符串操作/declare | ⭐⭐ |
| 03 | [Shell 流程控制](03-Shell-流程控制.md) | if/elif/case/for/while/until/break/continue/select | ⭐⭐ |
| 04 | [Shell 函数与实战](04-Shell-函数与实战.md) | 函数定义/参数/返回值/实战脚本/子进程/严格模式 | ⭐⭐ |
| 05 | [Shell 文本处理](05-Shell-文本处理.md) | grep/sed/awk 三剑客 + 正则/管道/实战流水线 | ⭐⭐⭐ |

## 3. 学习路线推荐

### 🟢 L1：能写脚本（半天）

```
01-入门 → 02-变量与类型 → 03-流程控制
产出：能写批量操作、条件判断、循环遍历的基础脚本
```

### 🔵 L2：能上生产（1 天）

```
L1 全部 + 04-函数与实战 → 05-文本处理
产出：能写部署脚本、备份脚本、日志分析流水线（配合 shellcheck 检查）
```

### 🟣 L3：文本处理大师（1 天加练）

```
05-文本处理 精读 + 实战：访问日志 IP 统计、错误率聚合、批量替换配置
产出：日志分析、监控脚本、批量运维一把梭
```

> 💡 前置要求：需具备 Linux 基础命令能力（`ls/cd/grep/管道`），可先在[Linux 体系 02-常用命令](../Linux/02-Linux-常用命令.md)补齐。

## 4. 核心概念速查

### 4.1 高频概念速查

| 概念 | 一句话说明 | 所在模块 |
|------|-----------|---------|
| Shebang | `#!/bin/bash` 声明解释器，必须是脚本第一行 | 01 |
| 执行方式 | `bash script.sh`（子进程）vs `source script.sh`（当前 shell） | 01 |
| 退出码 | `$?` 上一条命令的退出码，0=成功；`exit n` 显式返回 | 01/04 |
| 命令替换 | `$(cmd)` 与反引号等价，推荐 `$()`（可嵌套） | 01/02 |
| 变量引用 | **始终用 `"$var"` 加引号**——防分词、防通配符展开 | 02/03 |
| `$@` vs `$*` | `"$@"` 展开为独立参数（正确）；`"$*"` 拼成单字符串（慎用） | 02 |
| 关联数组 | `declare -A map`，需 Bash 4+ | 02 |
| `[ ]` vs `[[ ]]` | `[[ ]]` 是 Bash 关键字：支持 `&&`/`||`/正则 `=~`/空安全 | 03 |
| 进程替换 | `<(...)` 让「输出当文件用」，规避管道子 shell 陷阱 | 04 |
| 严格模式 | `set -euo pipefail`：失败即退/未定义报错/管道整体失败 | 04 |
| 三剑客分工 | grep 过滤、sed 行处理/替换、awk 列处理/统计 | 05 |
| shellcheck | 脚本静态检查工具，SC2086（变量未加引号）是最常见警告 | 04 |

### 4.2 常用速记

| 需求 | 一行命令 |
|------|---------|
| 批量重命名 | `for f in *.log; do mv "$f" "${f%.log}.bak"; done` |
| 逐行读文件 | `while IFS= read -r line; do echo "$line"; done < file` |
| 等幂日志路径 | `mkdir -p /var/log/app && LOG_FILE="/var/log/app/app.log"` |
| 启动 Spring Boot | `nohup java -jar app.jar > app.log 2>&1 & echo $! > app.pid` |
| IP 访问 Top10 | `awk '{print $1}' access.log \| sort \| uniq -c \| sort -rn \| head -10` |

> 🎯 **核心要点**：Shell 脚本质量三支柱——**引号**（所有变量引用加双引号）、**严格模式**（set -euo pipefail）、**工具化**（sed/awk 替代手工循环）；写任何脚本先跑 `shellcheck`。Linux 命令与运维知识见[关联体系](../Linux/00-Linux知识体系总览.md)。

---

**下一模块**：[01-Shell 入门](01-Shell-入门.md)

**【参考来源】**
- Bash 官方手册：https://www.gnu.org/software/bash/manual/
- ShellCheck 官方文档：https://www.shellcheck.net/
- Bash Hackers Wiki：https://wiki.bash-hackers.org/
