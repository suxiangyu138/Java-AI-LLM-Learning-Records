# Ubuntu Shell 脚本实战

> 📜 Bash 语法速查、变量与函数、条件判断、循环、定时任务、实战脚本模板 —— 让重复劳动自动化

---

## 📚 目录

1. [Bash 基础语法](#1-bash-基础语法)
2. [变量与字符串处理](#2-变量与字符串处理)
3. [条件判断与循环](#3-条件判断与循环)
4. [函数](#4-函数)
5. [定时任务 crontab](#5-定时任务-crontab)
6. [实战脚本模板](#6-实战脚本模板)

---

## 1. Bash 基础语法

```bash
#!/bin/bash
# ↑ shebang：必须放在第一行，指定解释器

# ==== 脚本执行方式 ====
chmod +x script.sh
./script.sh                # 子 shell 中执行
bash script.sh             # 等同于上面
source script.sh           # 当前 shell 执行（变量保留）
. script.sh                # 等同于 source

# ==== 调试 ====
bash -x script.sh          # 打印每条命令
# 脚本内局部调试：
set -x                     # 开启调试
# ... 被调试的代码 ...
set +x                     # 关闭调试

# ==== 安全选项 ====
set -e                     # 任何命令失败则退出
set -u                     # 使用未定义变量则报错
set -o pipefail            # 管道中任一命令失败则整体失败
# 推荐：脚本开头加这三行
```

---

## 2. 变量与字符串处理

### 2.1 变量基础

```bash
# 定义与引用
name="hello"               # = 两边不能有空格！
readonly PI=3.14           # 只读变量
echo $name
echo ${name}               # 推荐用 {} 包裹
echo "${name}_world"       # 拼接时需要 {}

# 特殊变量
$0    # 脚本名
$1 $2 # 第 1、2 个参数
$#    # 参数个数
$@    # 所有参数（每个独立）→ 推荐
$*    # 所有参数（合并为一个）
$?    # 上一条命令的退出码（0=成功）
$$    # 当前进程 PID
$!    # 最后一个后台进程 PID
```

### 2.2 字符串操作

```bash
str="hello-world-2026"

# 长度
echo ${#str}               # 17

# 截取子串
echo ${str:6:5}            # world（从下标 6 取 5 个）

# 替换
echo ${str/world/ubuntu}   # hello-ubuntu-2026（替换第一个）
echo ${str//-/ }           # hello world 2026（替换所有）

# 删除
echo ${str#hello-}         # world-2026（删最短前缀）
echo ${str##*-}            # 2026（删最长前缀）
echo ${str%-*}             # hello-world（删最短后缀）
echo ${str%%-*}            # hello（删最长后缀）

# 默认值
echo ${VAR:-default}       # VAR 为空则用 default
echo ${VAR:=default}       # VAR 为空则赋值 default
```

---

## 3. 条件判断与循环

### 3.1 if 条件

```bash
# ==== 文件判断 ====
[ -f file ]    # 是普通文件
[ -d dir ]     # 是目录
[ -e path ]    # 存在
[ -x file ]    # 可执行
[ -s file ]    # 非空

# ==== 字符串判断 ====
[ "$a" = "$b" ]     # 相等
[ "$a" != "$b" ]    # 不等
[ -z "$str" ]       # 为空
[ -n "$str" ]       # 非空

# ==== 数值判断 ====
[ $a -eq $b ]  # ==
[ $a -ne $b ]  # !=
[ $a -gt $b ]  # >
[ $a -lt $b ]  # <
[ $a -ge $b ]  # >=
[ $a -le $b ]  # <=

# ==== 逻辑运算 ====
[ cond1 ] && [ cond2 ]   # AND
[ cond1 ] || [ cond2 ]   # OR
[ ! cond ]                # NOT
```

```bash
# 实战：脚本参数检查
#!/bin/bash
if [ $# -lt 2 ]; then
    echo "用法: $0 <环境> <版本号>"
    echo "示例: $0 prod 1.2.3"
    exit 1
fi

ENV=$1
VERSION=$2

if [ "$ENV" = "prod" ]; then
    echo "⚠️ 生产环境部署，确认要继续吗？(yes/no)"
    read CONFIRM
    if [ "$CONFIRM" != "yes" ]; then
        echo "已取消"
        exit 0
    fi
fi
```

### 3.2 循环

```bash
# for 循环
for i in 1 2 3 4 5; do
    echo "Number: $i"
done

for i in {1..10}; do         # {1..10} 范围
    echo $i
done

for file in *.txt; do        # 遍历文件
    echo "Processing $file"
done

# while 循环
count=0
while [ $count -lt 5 ]; do
    echo $count
    ((count++))               # 自增
done

# 读取文件每一行
while IFS= read -r line; do
    echo "$line"
done < file.txt

# case
read -p "输入 (start/stop/restart): " ACTION
case $ACTION in
    start)   echo "启动中...";;
    stop)    echo "停止中...";;
    restart) echo "重启中...";;
    *)       echo "未知操作";;
esac
```

---

## 4. 函数

```bash
# 定义
hello() {
    local name=$1          # local 限制作用域
    echo "Hello, $name!"
}

# 参数
deploy() {
    local env=$1
    local version=$2
    echo "部署 $version 到 $env 环境"
}

deploy prod 1.2.3   # 调用

# 返回值
is_running() {
    systemctl is-active --quiet "$1"
    return $?              # 返回退出码（0-255）
}

if is_running myapp; then
    echo "运行中"
else
    echo "已停止"
fi
```

---

## 5. 定时任务 crontab

### 5.1 格式与示例

```text
crontab 格式：
  *    *    *    *    *    command
  ┬    ┬    ┬    ┬    ┬
  │    │    │    │    └── 星期 (0-7, 0=周日)
  │    │    │    └────── 月 (1-12)
  │    │    └────────── 日 (1-31)
  │    └────────────── 时 (0-23)
  └────────────────── 分 (0-59)

示例：
  0 2 * * *           每天 2:00 AM
  */5 * * * *         每 5 分钟
  0 9-17 * * 1-5      周一到周五 9-17 每个整点
  0 0 1 * *           每月 1 日 0:00
```

```bash
# crontab 命令
crontab -e           # 编辑当前用户定时任务
crontab -l           # 列出
crontab -r           # 删除

# 推荐：定时任务中加日志
0 2 * * * /opt/scripts/backup.sh >> /var/log/backup.log 2>&1
```

### 5.2 定时任务模板

```bash
#!/bin/bash
# /opt/scripts/cleanup.sh —— 清理过期日志和临时文件

set -e
LOG_DIR="/var/log/myapp"
TMP_DIR="/tmp/myapp"
RETENTION_DAYS=7

echo "=== $(date '+%Y-%m-%d %H:%M:%S') 清理开始 ==="

# 删除 N 天前的日志
find "$LOG_DIR" -name "*.log" -mtime +$RETENTION_DAYS -delete
echo "清理 $LOG_DIR 中 ${RETENTION_DAYS} 天前的日志"

# 清理临时文件
find "$TMP_DIR" -type f -mtime +1 -delete
echo "清理 $TMP_DIR 中 1 天前的临时文件"

# 清理 Docker 未使用的资源
docker system prune -f --volumes 2>/dev/null || true

echo "=== 清理完成 ==="
```

---

## 6. 实战脚本模板

### 6.1 Spring Boot 应用管理脚本

```bash
#!/bin/bash
# appctl.sh —— Spring Boot 应用启动/停止/重启/状态

APP_NAME="myapp"
JAR_PATH="/opt/${APP_NAME}/app.jar"
LOG_PATH="/var/log/${APP_NAME}/app.log"
PID_FILE="/var/run/${APP_NAME}.pid"
JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC"

start() {
    if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
        echo "⚠️ ${APP_NAME} 已在运行"
        exit 1
    fi
    echo "启动 ${APP_NAME}..."
    nohup java $JAVA_OPTS -jar "$JAR_PATH" > "$LOG_PATH" 2>&1 &
    echo $! > "$PID_FILE"
    echo "✅ PID: $(cat $PID_FILE)"
}

stop() {
    if [ ! -f "$PID_FILE" ]; then
        echo "⚠️ ${APP_NAME} 未运行"
        return
    fi
    PID=$(cat "$PID_FILE")
    echo "停止 ${APP_NAME} (PID: $PID)..."
    kill "$PID"
    for i in {1..30}; do
        kill -0 "$PID" 2>/dev/null || break
        sleep 1
    done
    kill -9 "$PID" 2>/dev/null || true
    rm -f "$PID_FILE"
    echo "✅ 已停止"
}

status() {
    if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
        echo "✅ ${APP_NAME} 运行中 (PID: $(cat $PID_FILE))"
    else
        echo "❌ ${APP_NAME} 未运行"
    fi
}

case "${1:-}" in
    start)   start;;
    stop)    stop;;
    restart) stop; sleep 2; start;;
    status)  status;;
    *)       echo "用法: $0 {start|stop|restart|status}"; exit 1;;
esac
```

### 6.2 数据库备份脚本

```bash
#!/bin/bash
# db-backup.sh

DB_NAME="mydb"
DB_USER="backup"
DB_PASS="secret"
BACKUP_DIR="/backup/mysql"
RETENTION_DAYS=30

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql.gz"

mkdir -p "$BACKUP_DIR"

echo "备份 ${DB_NAME} → ${BACKUP_FILE}"
mysqldump -u"$DB_USER" -p"$DB_PASS" --single-transaction --routines "$DB_NAME" \
    | gzip > "$BACKUP_FILE"

echo "清理 ${RETENTION_DAYS} 天前的备份"
find "$BACKUP_DIR" -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete

echo "✅ 备份完成，大小: $(du -h $BACKUP_FILE | cut -f1)"
```

---

> 🎯 **核心要点**：脚本开头加 `set -euo pipefail`；变量用 `{}` 包裹；`$?` 检查上条命令；`local` 限定函数内变量；**crontab** 定时自动执行。Shell 不追求完美，实用至上。

---

**返回总览**：[00-Ubuntu知识体系总览](./00-Ubuntu知识体系总览.md)

---

*创建于：2026年7月*
