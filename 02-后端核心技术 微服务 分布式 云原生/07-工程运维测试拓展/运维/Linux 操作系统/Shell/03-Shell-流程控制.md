# 03 - Shell 流程控制

> 面向 Java 后端开发者的 Shell 流程控制速查，涵盖条件判断与循环结构。

---


## 📚 目录

1. [条件判断](#1-条件判断)
2. [循环结构](#2-循环结构)
3. [组合示例：Spring Boot 应用管理](#3-组合示例spring-boot-应用管理)
4. [FAQ](#4-faq)
5. [速查表](#5-速查表)

---

## 1. 条件判断

### 1.1 `if` / `elif` / `else` / `fi`

```bash
if [[ condition ]]; then
    # 条件成立
elif [[ condition ]]; then
    # 另一条件
else
    # 兜底
fi
```

> **`[[ ]]` 是 bash 关键字**，支持 `&&`、`||`、`==`、`=~`（正则），推荐替代 `[ ]`。

### 1.2 `test` 命令与 `[ ]`

`[ condition ]` 等价于 `test condition`，左右必须有空格。

```bash
if [ -f "/opt/app/app.jar" ]; then echo "JAR 存在"; fi
if [ -d "/var/log/tomcat" ]; then echo "日志目录存在"; fi
```

### 1.3 常用文件测试符

| 符号 | 含义 | 示例 |
|------|------|------|
| `-f` | 普通文件 | `[ -f /path ]` |
| `-d` | 目录 | `[ -d /path ]` |
| `-x` | 可执行 | `[ -x /usr/bin/java ]` |
| `-e` | 存在 | `[ -e /path ]` |
| `-r` | 可读 | `[ -r /etc/passwd ]` |
| `-s` | 大小 > 0 | `[ -s /var/log/syslog ]` |
| `-L` | 符号链接 | `[ -L /usr/bin/java ]` |

### 1.4 字符串与数值比较

| 类型 | 操作符 | 示例 |
|------|--------|------|
| 数值相等 | `-eq` | `[ "$a" -eq 3 ]` |
| 数值不等 | `-ne` | `[ "$a" -ne 3 ]` |
| 数值大于 | `-gt` | `[ "$a" -gt 3 ]` |
| 数值小于 | `-lt` | `[ "$a" -lt 3 ]` |
| 字符串相等 | `==` | `[[ "$s" == "dev" ]]` |
| 字符串不等 | `!=` | `[[ "$s" != "prod" ]]` |
| 字符串为空 | `-z` | `[ -z "$var" ]` |
| 字符串非空 | `-n` | `[ -n "$var" ]` |

```bash
# 根据环境切换 JVM 参数
env="${DEPLOY_ENV:-dev}"
if [[ "$env" == "prod" ]]; then
    JVM_OPTS="-Xms4g -Xmx4g"
elif [[ "$env" == "staging" ]]; then
    JVM_OPTS="-Xms2g -Xmx2g"
else
    JVM_OPTS="-Xms512m -Xmx512m"
fi
java $JVM_OPTS -jar app.jar
```

### 1.5 `case` 语句

```bash
case "$1" in
    start)
        systemctl start myapp
        ;;
    stop)
        systemctl stop myapp
        ;;
    restart|reload)
        systemctl restart myapp
        ;;
    *)  # 通配兜底
        echo "Usage: $0 {start|stop|restart}"
        exit 1
        ;;
esac
```

> 类似 Java `switch`：`;;` 相当于 `break`，`*)` 相当于 `default`。

---

## 2. 循环结构

### 2.1 `for` 循环

```bash
# 列表迭代
for env in dev staging prod; do echo "Deploying to $env..."; done

# 类 C 风格
for ((i=0; i<${#arr[@]}; i++)); do echo "${arr[$i]}"; done

# 命令输出（遍历 Java 进程）
for pid in $(pgrep -f java); do echo "Java PID: $pid"; done
```

### 2.2 `while` 循环

```bash
# 逐行读取配置文件
while IFS= read -r line; do
    [[ -z "$line" || "$line" =~ ^# ]] && continue
    echo "Config: $line"
done < app.conf

# 轮询等待 Spring Boot 就绪
count=0
while ! curl -sf http://localhost:8080/actuator/health; do
    sleep 2
    ((count++))
    if [[ $count -gt 30 ]]; then echo "Timeout"; exit 1; fi
done
echo "App is ready"
```

### 2.3 `until` 循环

条件为假时执行，与 `while` 逻辑相反。

```bash
until pgrep -x java > /dev/null; do sleep 1; done
echo "Java 进程已启动"
```

### 2.4 `break` / `continue`

```bash
for jar in lib/*.jar; do
    jar tf "$jar" | grep -q "Application.class" || continue
    echo "Found main jar: $jar"
    break
done
```

- `break n`：跳出 n 层循环
- `continue n`：跳过 n 层当前迭代

### 2.5 `select` 菜单

```bash
PS3="请选择部署环境: "
select env in dev staging prod quit; do
    case $env in
        dev|staging|prod) echo "Deploying to $env ..."; break ;;
        quit) exit 0 ;;
        *) echo "无效选择";;
    esac
done
```

`select` 自动输出带编号的菜单，`PS3` 自定义提示符。

---

## 3. 组合示例：Spring Boot 应用管理

```bash
#!/usr/bin/env bash
set -euo pipefail
APP_JAR="/opt/app/app.jar"
PID_FILE="/var/run/app.pid"

start() {
    if [[ -f "$PID_FILE" ]] && kill -0 $(<"$PID_FILE") 2>/dev/null; then
        echo "App 已在运行"; exit 1
    fi
    nohup java -jar "$APP_JAR" > app.log 2>&1 &
    echo $! > "$PID_FILE"
    echo "启动成功 (PID: $!)"
}
stop() {
    [[ ! -f "$PID_FILE" ]] && { echo "未找到 PID 文件"; exit 1; }
    kill $(<"$PID_FILE") 2>/dev/null && echo "已停止" || echo "进程不存在"
    rm -f "$PID_FILE"
}
status() {
    if [[ -f "$PID_FILE" ]] && kill -0 $(<"$PID_FILE") 2>/dev/null; then
        echo "运行中 (PID: $(<"$PID_FILE"))"
    else
        echo "未运行"
    fi
}
case "${1:-help}" in
    start|stop|status) "$1" ;;
    restart) stop; sleep 1; start ;;
    *) echo "用法: $0 {start|stop|status|restart}" ;;
esac
```

---

## 4. FAQ

### Q1: `[ ]` 和 `[[ ]]` 有什么区别？

| 特性 | `[ ]` | `[[ ]]` |
|------|-------|---------|
| POSIX 兼容 | 是 | 否（bash/zsh） |
| 支持 `&&` `\|\|` | 否（用 `-a` `-o`） | 是 |
| 模式/正则匹配 | 否 | `==` 通配 / `=~` 正则 |
| 空变量安全 | 需引号 | 是 |

> **建议**：脚本用 `#!/bin/bash` 时可全用 `[[ ]]`。

### Q2: 条件判断报错的常见原因？

- **空格缺失**：`[$var=1]` 应为 `[ "$var" = 1 ]`
- **变量未加引号**：`$var` 为空时报错，改为 `"$var"`
- **比较符号用错**：数字用 `-eq`，字符串用 `==`

### Q3: `for i in $(command)` 和 `while read` 选哪个？

- `for i in $(...)`: 一次加载全部输出，适用小量、无空格结果
- `while read -r line`: 逐行读取，内存友好，推荐用于文件处理

### Q4: 如何调试循环和条件？

```bash
set -x    # 开启追踪
# ... 代码 ...
set +x    # 关闭追踪
```

### Q5: `break` 和 `exit` 的区别？

- `break`：跳出当前循环，脚本继续
- `exit`：终止整个脚本，可指定退出码

## 5. 速查表

| 需求 | 写法 |
|------|------|
| 文件存在 | `[[ -f "$path" ]]` |
| 目录存在 | `[[ -d "$dir" ]]` |
| 变量非空 | `[[ -n "$var" ]]` |
| 数字比较 | `[[ $n -gt 10 ]]` |
| 字符串相等 | `[[ "$s" == "dev" ]]` |
| 正则匹配 | `[[ "$s" =~ ^test- ]]` |
| 定长 for | `for ((i=0; i<10; i++))` |
| 遍历数组 | `for e in "${arr[@]}"` |
| 读取文件 | `while IFS= read -r line` |
| 菜单选择 | `select var in list` |

---

**上一模块**：[02-Shell 变量与类型](02-Shell-变量与类型.md) ｜ **下一模块**：[04-Shell 函数与实战](04-Shell-函数与实战.md) ｜ **返回总览**：[00-知识体系总览](00-Shell知识体系总览.md)

**【参考来源】**
- Bash 官方手册（条件表达式）：https://www.gnu.org/software/bash/manual/html_node/Bash-Conditional-Expressions.html
