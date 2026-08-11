# 04 - Shell 函数与实战

> 面向 Java 后端开发者的 Shell 脚本进阶指南：函数封装、实战脚本、调试技术与子进程管理。


## 📚 目录

1. [Shell 函数](#1-shell-函数)
2. [编程实战](#2-编程实战)
3. [脚本调试](#3-脚本调试)
4. [子进程与 Subshell](#4-子进程与-subshell)
5. [FAQ / 故障排查](#5-faq-故障排查)

---

## 1. Shell 函数

### 1.1 定义与调用

```bash
# function 可省略，推荐加上提升可读性
function hello() { echo "Hello, $1"; }
hello "Java"
```

### 1.2 参数与返回值

| 机制 | 用法 | 说明 |
|------|------|------|
| 位置参数 | `$1`, `$2`, ..., `$9` | 第 N 个参数 |
| 全部参数 | `$@` | 所有参数列表 |
| 参数个数 | `$#` | 参数总数 |
| 退出码 | `return N` | 0-255，0 成功 |
| 结果返回 | `echo` | 用 `$(func)` 捕获 |

```bash
function add() {
    local sum=$(( $1 + $2 ))
    echo $sum       # 返回计算结果
    return 0        # 返回状态码
}
result=$(add 10 20)
```

**关键区别**：`return` 仅返回退出码（类似 Java 异常），具体结果用 `echo` + `$()` 捕获。

### 1.3 局部变量与函数库

```bash
function deploy() {
    local jar_name="app.jar"     # 函数内临时变量一律用 local
    local log_path="/var/log/$1"
    echo "部署 $jar_name 到 $1"
}
```

```bash
# lib/common.sh — 公共函数库
function check_java() { command -v java || { echo "JDK 未安装"; exit 1; }; }

# deploy.sh — 通过 source 引入
source /opt/scripts/lib/common.sh
check_java
```

---

## 2. 编程实战

### 2.1 部署脚本

```bash
#!/bin/bash
set -euo pipefail
APP_NAME="demo-service.jar"
APP_PATH="/opt/app"
LOG_PATH="/var/log/app"

stop() { local pid; pid=$(pgrep -f "$APP_NAME" || true); [ -n "$pid" ] && kill -9 "$pid"; }
start() { cd "$APP_PATH"; nohup java -Xms512m -Xmx1024m -jar "$APP_NAME" > "$LOG_PATH/console.log" 2>&1 & echo "PID: $!"; }
health() {
    local pid=$(pgrep -f "$APP_NAME" || true)
    if [ -n "$pid" ] && curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "正常 (PID: $pid)"; return 0
    else echo "异常"; return 1; fi
}
case "${1:-}" in
    start) start ;; stop) stop ;; restart) stop; sleep 2; start ;;
    health) health ;; *) echo "用法: $0 {start|stop|restart|health}" ;;
esac
```

### 2.2 备份脚本

```bash
#!/bin/bash
set -euo pipefail
BACKUP_DIR="/data/backup/$(date +%Y%m%d)"
RETENTION_DAYS=30
DB_NAMES=("order_db" "user_db" "product_db")
mkdir -p "$BACKUP_DIR"
for db in "${DB_NAMES[@]}"; do
    mysqldump --single-transaction "$db" | gzip > "$BACKUP_DIR/${db}.sql.gz"
    echo "备份完成: $db"
done
find /data/backup -maxdepth 1 -type d -mtime +$RETENTION_DAYS -exec rm -rf {} \;
```

### 2.3 日志轮转

```bash
#!/bin/bash
LOG_FILE="/var/log/app/app.log"
RETENTION=7
[ -f "$LOG_FILE" ] && mv "$LOG_FILE" "${LOG_FILE}.$(date +%Y%m%d-%H%M%S)" && kill -USR1 "$(pgrep -f demo-service)" 2>/dev/null || true
find "$(dirname "$LOG_FILE")" -name "*.log.*" -mtime +$RETENTION -delete
```

### 2.4 健康检查

```bash
#!/bin/bash
SERVICES=("8080:order-service" "8081:user-service" "8082:gateway")
for entry in "${SERVICES[@]}"; do
    port="${entry%%:*}"; name="${entry##*:}"
    curl -sf "http://localhost:$port/actuator/health" > /dev/null 2>&1 \
        && echo "[OK] $name (:$port)" \
        || echo "[FAIL] $name (:$port)"
done
```

---

## 3. 脚本调试

### 3.1 执行跟踪

```bash
# 全局：bash -x script.sh
# 局部：
set -x   # 启用跟踪
java -jar app.jar
set +x   # 关闭跟踪
```

`+` 开头的行是实际执行的命令及变量展开后的值。

### 3.2 严格模式与信号捕获

```bash
#!/bin/bash
set -euo pipefail
# -e : 命令失败立即退出  -u : 未定义变量报错  -o pipefail : 管道任一失败则整体失败

function cleanup() { echo "[$(date)] 异常退出，行号: $LINENO" >> /var/log/debug.log; }
trap cleanup ERR EXIT
```

### 3.3 日志调试

```bash
#!/bin/bash
DEBUG_LOG="/var/log/deploy-$(date +%Y%m%d).log"
exec > "$DEBUG_LOG" 2>&1
echo "JAVA_HOME=$JAVA_HOME"
echo "APP_PATH=$APP_PATH"
```

---

## 4. 子进程与 Subshell

### 4.1 触发 Subshell 的场景

| 语法 | 示例 | 说明 |
|------|------|------|
| 括号 | `(cd /tmp && ls)` | 括号内命令在子 shell 执行 |
| 管道 | `echo x \| read var` | 两侧各在独立子 shell |
| 命令替换 | `var=$(ls)` | `$()` 内创建子 shell |
| 执行脚本 | `bash script.sh` | 显式创建子进程 |

### 4.2 常见陷阱

```bash
# 陷阱：cd 在子 shell 不生效
(cd /opt/app && pwd)   # 输出 /opt/app
pwd                     # 仍为原目录

# 陷阱：管道中变量赋值丢失
echo "hello" | read msg
echo "$msg"            # 空值 — read 在子 shell 中执行

# 修正：使用进程替换
read msg < <(echo "hello")
echo "$msg"            # hello
```

### 4.3 Java 调用 Shell 的进程管理

```java
ProcessBuilder pb = new ProcessBuilder("bash", "/opt/scripts/deploy.sh");
pb.redirectErrorStream(true);                          // 合并错误流，防阻塞
Process process = pb.start();
boolean finished = process.waitFor(30, TimeUnit.SECONDS);  // 必须设超时
if (!finished) { process.destroyForcibly(); throw new RuntimeException("超时"); }
String output = new String(process.getInputStream().readAllBytes());
System.out.println(output);
```

---

## 5. FAQ / 故障排查

| 问题 | 原因 | 解决 |
|------|------|------|
| 函数未找到 | 定义在调用之后 | 将函数定义移到脚本开头 |
| return 返回了错误值 | `return` 只能返 0-255 | 用 `echo` + `$()` 返回结果 |
| 变量在函数外被修改 | 未使用 `local` | 函数内变量加 `local` |
| 远程执行 `source` 失败 | 库文件路径不存在 | 使用绝对路径 |
| `set -e` 导致脚本意外退出 | 某条命令返回非 0 | 预期失败的命令加 `|| true` |
| Java 调用脚本进程残留 | 未调用 `waitFor()` | 使用 `ProcessBuilder` + 超时控制+`destroy()` |
| 管道中变量值丢失 | 管道创建子 shell | 改用进程替换或临时文件 |
| shellcheck SC2086 | 变量未加双引号 | 使用 `"$var"` 包裹 |

> **一句话原则**：函数内用 `local`，返回结果用 `echo`，调试用 `set -x`，避免不必要括号和管道。

---

**上一模块**：[03-Shell 流程控制](03-Shell-流程控制.md) ｜ **下一模块**：[05-Shell 文本处理](05-Shell-文本处理.md) ｜ **返回总览**：[00-知识体系总览](00-Shell知识体系总览.md)

**【参考来源】**
- Bash 官方手册（Shell 函数）：https://www.gnu.org/software/bash/manual/html_node/Shell-Functions.html\n- Bash 严格模式参考：https://gist.github.com/mohanpedala/1e2ff5661761d3abd0385e8223e5823c
