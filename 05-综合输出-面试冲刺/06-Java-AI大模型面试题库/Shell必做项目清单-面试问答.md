# Shell 必做项目清单 面试问答清单
> 🎯 基于 Shell 实战项目清单，涵盖面试高频问题与完美解答方案，帮助你在面试中脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：Shell 脚本中 `$?`、`$0`、`$@`、`$#` 这些特殊变量分别代表什么？

**面试官意图：** 考察 Shell 脚本基础语法的掌握程度。

**完美解答：**

Shell 特殊变量是脚本编程的基础，必须熟练掌握：

| 变量 | 含义 | 示例场景 |
|------|------|----------|
| `$0` | 脚本文件名 | 帮助信息 `echo "Usage: $0 <option>"` |
| `$1`~`$9` | 位置参数 | `FILE=$1` 传入文件名 |
| `$#` | 参数个数 | 校验参数 `if [ $# -lt 1 ]; then` |
| `$@` | 所有参数列表（独立字符串） | 循环处理 `for arg in "$@"; do` |
| `$*` | 所有参数（单一字符串） | 整体传递 |
| `$?` | 上一条命令退出码（0 成功，非 0 失败） | 错误处理 `if [ $? -ne 0 ]; then` |
| `$$` | 当前 Shell 进程 PID | 临时文件 `TMP_FILE="/tmp/script_$$.tmp"` |
| `$!` | 后台最后一个进程的 PID | 等待后台进程 `wait $!` |

**实用例子：**
```bash
#!/bin/bash
# 脚本参数校验规范写法
if [ $# -lt 2 ]; then
    echo "Usage: $0 <source_dir> <backup_dir>"
    exit 1
fi

SOURCE_DIR=$1
BACKUP_DIR=$2

echo "参数个数: $#"
echo "所有参数: $@"
```

**延伸追问应对：** 如果问 `$@` 和 `$*` 的区别，关键是双引号下的行为：`"$@"` 会保留参数独立性（每个参数单独引号），`"$*"` 将所有参数合并为一个字符串。

---

### Q2：grep、sed、awk 被称为"文本处理三剑客"，你在实际工作中是怎么用它们的？

**面试官意图：** 考察文本处理的实战能力，Shell 脚本最核心的应用就是文本分析。

**完美解答：**

这三个工具各有侧重，我总结了一个选型原则：

| 工具 | 核心能力 | 让我一句话记住 |
|------|----------|----------------|
| `grep` | 过滤匹配行 | "我只负责找行，不负责改" |
| `sed` | 行级编辑替换 | "我可以增删改查，按行操作" |
| `awk` | 列级格式化与统计 | "我把每行拆成列来做表格计算" |

**实战案例一：日志错误统计（三剑客配合）**
```bash
# 统计今天日志中各错误类型的出现次数
grep "2026-07-22" app.log | grep -E "ERROR|WARN" | awk -F '|' '{print $3}' | sort | uniq -c | sort -rn

# 输出格式： 123 空指针异常
#             45 连接超时
#             12 参数校验失败
```

**实战案例二：sed 批量替换配置文件**
```bash
# 在部署时替换数据库连接地址
sed -i "s|jdbc:mysql://localhost:3306|jdbc:mysql://${DB_HOST}:${DB_PORT}|g" application-prod.yml

# 注意：用 | 代替 / 做分隔符，避免路径转义问题
```

**实战案例三：awk 做性能报表**
```bash
# 分析 Nginx 日志，统计每个接口平均响应时间
awk '{print $7, $NF}' access.log | sed 's/"//g' | awk '{
    url=$1; time=$2+0;
    total[url]++; sum[url]+=time;
} END {
    for (url in total) {
        printf "%s avg=%.2fms count=%d\n", url, sum[url]/total[url], total[url]
    }
}' | sort -k2 -rn | head -10
```

> 💡 **面试技巧**：提到这些工具时，一定要带上真实的业务场景，比如"我用 awk 统计过 Nginx 日志中每个接口的 P99 延迟"，比背命令印象深 10 倍。

---

### Q3：Shell 脚本中，单中括号 `[ ]` 和双中括号 `[[ ]]` 有什么区别？

**面试官意图：** 考察对 Shell 语法细节的理解深度。

**完美解答：**

| 特性 | `[ ]` | `[[ ]]` |
|------|-------|---------|
| POSIX 兼容 | 是（所有 Shell 支持） | 否（Bash/Ksh/Zsh 特有） |
| 变量引用 | 必须加引号 `"$var"` | 可以不加引号 |
| 逻辑运算 | `-a` / `-o` | `&&` / `\|\|` |
| 正则匹配 | 不支持 | 支持 `=~` 操作符 |
| 空值安全 | 变量为空可能导致语法错误 | 自动处理空值 |
| 模式匹配 | 不支持 | 支持 `==` 通配符 |

**示例对比：**
```bash
# 传统写法（容易踩坑）
name=""
if [ $name == "admin" ]; then   # 报错：变量为空时变成 [  == "admin" ]
fi

if [ "$name" == "admin" ]; then # 正确：必须加引号
fi

# 推荐写法（Bash 环境）
name=""
if [[ $name == "admin" ]]; then  # 安全：自动处理空值
fi

# 正则匹配（只有 [[ 支持）
if [[ $email =~ ^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$ ]]; then
    echo "Valid email"
fi
```

> ⚠️ **生产建议**：在脚本头声明 `#!/bin/bash` 而不是 `#!/bin/sh`，然后放心使用 `[[ ]]`，代码可读性会提升很多。如果需要兼容 `sh`（如某些 Docker 基础镜像），则必须用 `[ ]` 且注意引号。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：请展示你写过的 MySQL 数据库备份脚本，说说为什么这么设计？

**面试官意图：** 考察实际脚本开发能力和容错设计思维。

**完美解答：**

这是我生产环境在用的备份脚本，核心设计思路是：**防御性编程 + 自动清理 + 告警通知**。

```bash
#!/bin/bash
# MySQL 全量备份脚本
# 功能：备份所有数据库 → 压缩 → 保留 N 天 → 失败告警

# ============ 配置区 ============
DB_USER="backup_user"
DB_PASS="$(cat /etc/mysql_backup.pwd)"  # 不从命令行暴露密码
DB_HOST="localhost"
BACKUP_DIR="/data/backup/mysql"
RETENTION_DAYS=30
LOG_FILE="/var/log/mysql_backup.log"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/full_backup_${DATE}.sql.gz"

# ============ 日志函数 ============
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# ============ 前置检查 ============
# 1. 检查备份目录
mkdir -p "$BACKUP_DIR" || { log "ERROR: 创建备份目录失败"; exit 1; }

# 2. 检查磁盘空间
REQUIRED_SPACE_MB=1024
AVAILABLE_SPACE_MB=$(df -m "$BACKUP_DIR" | awk 'NR==2 {print $4}')
if [ "$AVAILABLE_SPACE_MB" -lt "$REQUIRED_SPACE_MB" ]; then
    log "ERROR: 磁盘空间不足, 可用 ${AVAILABLE_SPACE_MB}MB, 需要 ${REQUIRED_SPACE_MB}MB"
    exit 1
fi

# 3. 检查 MySQL 连接
mysql -u"$DB_USER" -p"$DB_PASS" -h"$DB_HOST" -e "SELECT 1" > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log "ERROR: MySQL 连接失败"
    send_alert "MySQL 备份失败: 数据库连接异常"
    exit 1
fi

# ============ 执行备份 ============
log "开始备份: $BACKUP_FILE"
mysqldump -u"$DB_USER" -p"$DB_PASS" -h"$DB_HOST" \
    --all-databases \
    --single-transaction \        # 不加锁备份，不影响业务
    --routines \                  # 导出存储过程和函数
    --triggers \
    --quick \
    2>> "$LOG_FILE" | gzip > "$BACKUP_FILE"

# ============ 结果验证 ============
BACKUP_EXIT_CODE=${PIPESTATUS[0]}
if [ $BACKUP_EXIT_CODE -ne 0 ]; then
    log "ERROR: 备份执行失败 (退出码: $BACKUP_EXIT_CODE)"
    send_alert "MySQL 备份执行失败"
    exit 1
fi

# 验证备份文件完整性
gzip -t "$BACKUP_FILE" > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log "ERROR: 备份文件损坏"
    rm -f "$BACKUP_FILE"
    send_alert "MySQL 备份文件损坏，已删除"
    exit 1
fi

log "备份完成: $(ls -lh "$BACKUP_FILE" | awk '{print $5}')"

# ============ 清理过期备份 ============
find "$BACKUP_DIR" -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete
log "已清理 ${RETENTION_DAYS} 天前的备份"

# ============ 告警函数 ============
send_alert() {
    local msg="$1"
    curl -s -X POST "https://oapi.dingtalk.com/robot/send?access_token=${WEBHOOK_TOKEN}" \
        -H "Content-Type: application/json" \
        -d "{\"msgtype\":\"text\",\"text\":{\"content\":\"[备份告警] $msg\"}}" > /dev/null
}

# ============ 配置 crontab ============
# 每天凌晨 2 点执行
# 0 2 * * * /opt/scripts/mysql_backup.sh
```

**关键设计决策：**

1. **`--single-transaction`**：InnoDB 引擎使用事务快照，不影响业务读写
2. **密码不直接写在脚本里**：从单独文件读取，避免泄露
3. **磁盘空间预检**：万一空间不足，先报错而不是生成空文件
4. **双重验证**：检查备份退出码 + 校验 gzip 完整性
5. **告警通知**：失败时通过钉钉 Webhook 实时推送

---

### Q5：你写过服务状态监控脚本吗？怎么做到的宕机自动重启和告警？

**面试官意图：** 考察自动化运维脚本的综合设计和实现能力。

**完美解答：**

这是我负责的服务监控脚本，支持多服务监控、自动重启和多渠道告警。

```bash
#!/bin/bash
# 服务监控脚本
# 功能：检测服务状态 → 宕机自动重启 → 重试失败后告警

SERVICES=(
    "nginx:nginx"
    "java:/opt/app/app.jar"
    "redis:redis-server"
)

check_service() {
    local name=$1
    local process=$2
    local retry=0
    local max_retry=3

    # 核心检测逻辑：ps + 端口双重验证
    if pgrep -f "$process" > /dev/null; then
        # 如果有端口定义，额外验证端口
        if [ -n "$3" ]; then
            if ss -tlnp | grep -q "$3"; then
                return 0
            else
                return 1
            fi
        fi
        return 0
    fi
    return 1
}

restart_service() {
    local name=$1
    case $name in
        nginx)
            systemctl restart nginx
            ;;
        java)
            cd /opt/app && ./start.sh
            ;;
        redis)
            systemctl restart redis
            ;;
    esac
}

send_alert() {
    local msg=$1
    # 钉钉告警
    curl -X POST "$DINGTALK_URL" \
        -H "Content-Type: application/json" \
        -d "{\"msgtype\":\"text\",\"text\":{\"content\":\"[服务监控] $msg\"}}"

    # 邮件告警（备选方案）
    echo "$msg" | mail -s "服务监控告警" admin@example.com
}

# 主循环
for service in "${SERVICES[@]}"; do
    IFS=':' read -r name process port <<< "$service"
    if ! check_service "$name" "$process" "$port"; then
        log "WARN: $name 宕机, 尝试重启"
        restart_service "$name"
        sleep 5

        if check_service "$name" "$process" "$port"; then
            log "INFO: $name 重启成功"
            send_alert "$name 已自动重启成功"
        else
            log "ERROR: $name 重启失败"
            send_alert "紧急: $name 重启失败, 请人工介入!"
        fi
    fi
done
```

> 💡 **设计亮点**：双重检测（进程 + 端口）避免误判；三次重启失败后才告警防止抖动；钉钉 + 邮件双通道保证告警可达。

---

### Q6：你写的一键部署脚本是怎么实现回滚机制的？

**面试官意图：** 考察对部署可靠性和容错设计的深层思考。

**完美解答：**

回滚是一键部署中最重要的安全保障。我的设计是"留后路、双重检验、自动回滚"。

```bash
#!/bin/bash
# 一键部署脚本（带回滚）

APP_NAME="app"
DEPLOY_DIR="/opt/app"
BACKUP_DIR="/data/backup/deploy"
JAR_NAME="app.jar"
JAVA_OPTS="-Xms512m -Xmx2g"

# 1. 备份旧版本（任何时候都先留后路）
backup_old_version() {
    if [ -f "${DEPLOY_DIR}/${JAR_NAME}" ]; then
        local backup_file="${BACKUP_DIR}/${APP_NAME}_$(date +%Y%m%d_%H%M%S).jar"
        log "备份当前版本到 $backup_file"
        cp "${DEPLOY_DIR}/${JAR_NAME}" "$backup_file"
        # 保留最近 5 个备份
        ls -t ${BACKUP_DIR}/${APP_NAME}_*.jar | tail -n +6 | xargs rm -f
    fi
}

# 2. 停止旧服务
stop_service() {
    log "停止服务: $APP_NAME"
    if systemctl is-active --quiet "$APP_NAME"; then
        systemctl stop "$APP_NAME"
        # 等待进程完全退出
        local timeout=30
        while [ $timeout -gt 0 ]; do
            if ! pgrep -f "$JAR_NAME" > /dev/null; then
                break
            fi
            sleep 1
            ((timeout--))
        done
        if [ $timeout -eq 0 ]; then
            log "WARN: 进程未退出, 强制终止"
            pkill -f "$JAR_NAME"
        fi
    fi
}

# 3. 部署新版本
deploy_new_version() {
    log "部署新版本: $1"
    cp "$1" "${DEPLOY_DIR}/${JAR_NAME}"
    chown appuser:appuser "${DEPLOY_DIR}/${JAR_NAME}"
}

# 4. 启动服务
start_service() {
    systemctl start "$APP_NAME"
}

# 5. 健康检查（关键：决定是否触发回滚）
health_check() {
    local retry=0
    local max_retry=12  # 最多等 60 秒
    
    while [ $retry -lt $max_retry ]; do
        # 检查进程存活
        if pgrep -f "$JAR_NAME" > /dev/null; then
            # 检查 HTTP 健康端点
            local http_code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null)
            if [ "$http_code" == "200" ]; then
                log "健康检查通过"
                return 0
            fi
        fi
        sleep 5
        ((retry++))
    done
    
    return 1  # 超时失败，触发回滚
}

# 6. 回滚
rollback() {
    log "触发回滚..."
    local latest_backup=$(ls -t ${BACKUP_DIR}/${APP_NAME}_*.jar | head -1)
    if [ -n "$latest_backup" ]; then
        log "回滚到: $latest_backup"
        cp "$latest_backup" "${DEPLOY_DIR}/${JAR_NAME}"
        start_service
        if health_check; then
            log "回滚成功"
        else
            log "ERROR: 回滚也失败了, 请紧急人工介入!"
        fi
    fi
}

# ============ 主流程 ============
NEW_JAR=$1
if [ ! -f "$NEW_JAR" ]; then
    echo "Usage: $0 <new_jar_file>"
    exit 1
fi

backup_old_version
stop_service
deploy_new_version "$NEW_JAR"
start_service

if ! health_check; then
    rollback
    send_alert "部署失败, 已自动回滚到上个版本"
    exit 1
fi

log "部署成功!"
```

> 🎯 **核心哲学**：部署的黄金法则是"永远可以回退"。这个脚本最巧妙的地方是健康检查才决定一切——版本上了不算，启动成功不算，API 健康检查通过才算。

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q7：如何设计一个生产级的服务器巡检一体化脚本？

**面试官意图：** 考察综合脚本设计和系统化监控思维。

**完美解答：**

一个生产级巡检脚本需要覆盖"CPU → 内存 → 磁盘 → 网络 → 关键进程 → 安全基线"六个维度，并输出可读报告。

```bash
#!/bin/bash
# 服务器巡检脚本
# 输出: Markdown 格式巡检报告

REPORT_FILE="/tmp/inspection_$(date +%Y%m%d).md"

# 巡检维度 1: 系统基本信息
collect_system_info() {
    echo "## 系统信息" >> "$REPORT_FILE"
    echo "- 主机名: $(hostname)" >> "$REPORT_FILE"
    echo "- OS: $(cat /etc/os-release | grep PRETTY_NAME | cut -d'"' -f2)" >> "$REPORT_FILE"
    echo "- 内核: $(uname -r)" >> "$REPORT_FILE"
    echo "- 运行时间: $(uptime -p)" >> "$REPORT_FILE"
    echo "- 负载: $(uptime | awk -F'load average:' '{print $2}')" >> "$REPORT_FILE"
}

# 巡检维度 2: CPU 状态
collect_cpu_info() {
    echo "" >> "$REPORT_FILE"
    echo "## CPU 状态" >> "$REPORT_FILE"
    echo "| 指标 | 值 | 状态 |" >> "$REPORT_FILE"
    echo "|------|-----|------|" >> "$REPORT_FILE"
    
    local cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d. -f1)
    if [ "$cpu_usage" -gt 80 ]; then
        echo "| CPU 使用率 | ${cpu_usage}% | ⚠️ 告警 |" >> "$REPORT_FILE"
    else
        echo "| CPU 使用率 | ${cpu_usage}% | ✅ 正常 |" >> "$REPORT_FILE"
    fi
    
    # CPU 负载与核心数对比
    local cores=$(nproc)
    local load=$(uptime | awk -F'load average:' '{print $2}' | cut -d, -f1)
    echo "| 每核负载 | $(echo "scale=2; $load / $cores" | bc) | - |" >> "$REPORT_FILE"
}

# 巡检维度 3: 内存状态
collect_memory_info() {
    echo "" >> "$REPORT_FILE"
    echo "## 内存状态" >> "$REPORT_FILE"
    echo "| 指标 | 值 | 状态 |" >> "$REPORT_FILE"
    echo "|------|-----|------|" >> "$REPORT_FILE"
    
    local mem_info=$(free -h | grep Mem)
    local mem_used=$(echo $mem_info | awk '{print $3}')
    local mem_total=$(echo $mem_info | awk '{print $2}')
    local mem_percent=$(free | grep Mem | awk '{print $3/$2 * 100.0}' | cut -d. -f1)
    
    if [ "$mem_percent" -gt 90 ]; then
        echo "| 内存使用 | ${mem_used}/${mem_total} (${mem_percent}%) | 🔴 严重 |" >> "$REPORT_FILE"
    elif [ "$mem_percent" -gt 70 ]; then
        echo "| 内存使用 | ${mem_used}/${mem_total} (${mem_percent}%) | ⚠️ 告警 |" >> "$REPORT_FILE"
    else
        echo "| 内存使用 | ${mem_used}/${mem_total} (${mem_percent}%) | ✅ 正常 |" >> "$REPORT_FILE"
    fi
    
    echo "| Swap 使用 | $(free -h | grep Swap | awk '{print $3}') | - |" >> "$REPORT_FILE"
}

# 巡检维度 4: 磁盘状态
collect_disk_info() {
    echo "" >> "$REPORT_FILE"
    echo "## 磁盘状态" >> "$REPORT_FILE"
    echo "| 挂载点 | 总大小 | 已用 | 可用 | 使用率 | 状态 |" >> "$REPORT_FILE"
    echo "|--------|--------|------|------|--------|------|" >> "$REPORT_FILE"
    
    df -h | grep "^/dev" | while read line; do
        local mount=$(echo $line | awk '{print $6}')
        local size=$(echo $line | awk '{print $2}')
        local used=$(echo $line | awk '{print $3}')
        local avail=$(echo $line | awk '{print $4}')
        local usage=$(echo $line | awk '{print $5}' | sed 's/%//')
        
        if [ "$usage" -gt 90 ]; then
            echo "| $mount | $size | $used | $avail | ${usage}% | 🔴 严重 |" >> "$REPORT_FILE"
        elif [ "$usage" -gt 75 ]; then
            echo "| $mount | $size | $used | $avail | ${usage}% | ⚠️ 告警 |" >> "$REPORT_FILE"
        else
            echo "| $mount | $size | $used | $avail | ${usage}% | ✅ 正常 |" >> "$REPORT_FILE"
        fi
    done
}

# 巡检维度 5: 关键端口和服务
collect_service_info() {
    echo "" >> "$REPORT_FILE"
    echo "## 关键端口监听" >> "$REPORT_FILE"
    echo "| 端口 | 状态 | 进程 |" >> "$REPORT_FILE"
    echo "|------|------|------|" >> "$REPORT_FILE"
    
    for port in 80 443 8080 3306 6379 11434; do
        if ss -tlnp | grep -q ":$port "; then
            local process=$(ss -tlnp "sport = :$port" | grep -oP 'users:\(\(".*?"\)' | head -1)
            echo "| $port | ✅ 监听 | ${process:-未知} |" >> "$REPORT_FILE"
        else
            echo "| $port | ❌ 未监听 | - |" >> "$REPORT_FILE"
        fi
    done
}

# 主流程
echo "# 服务器巡检报告" > "$REPORT_FILE"
echo "> 巡检时间: $(date '+%Y-%m-%d %H:%M:%S')" >> "$REPORT_FILE"
echo "---" >> "$REPORT_FILE"

collect_system_info
collect_cpu_info
collect_memory_info
collect_disk_info
collect_service_info

# 输出报告
cat "$REPORT_FILE"
```

**巡检报告结果示例：**
```markdown
# 服务器巡检报告
> 巡检时间: 2026-07-22 10:00:00

## CPU 状态
| 指标 | 值 | 状态 |
|------|-----|------|
| CPU 使用率 | 45% | ✅ 正常 |
| 每核负载 | 0.82 | - |
```

---

### Q8：Shell 脚本和 Python 在自动化运维中如何选型？你什么场景用什么？

**面试官意图：** 考察技术选型的判断力和实际工程经验。

**完美解答：**

| 维度 | Shell | Python |
|------|-------|--------|
| 学习曲线 | 平缓，1 天上手 | 稍陡峭 |
| 系统调用（进程/文件） | 原生支持，极其高效 | 需要 os/subprocess 模块 |
| 文本处理 | grep/sed/awk 组合极强 | 需要 re/pandas |
| 复杂逻辑（数据结构/异常） | 弱，数组和对象受限 | 强，完整编程语言 |
| 跨平台 | 差（Windows 需 WSL） | 好（全平台支持） |
| 第三方库 | 无 | 极其丰富 |
| 执行效率 | 每次启动解释器 | 启动慢但运行快 |
| JSON/YAML 处理 | 麻烦（依赖 jq/yq） | 内置支持 |

**我的选型原则：**

```bash
# 适合 Shell 的场景：
# 1. 简单的文件操作和系统管理
# 2. 调用系统命令链（管道组合）
# 3. crontab 定时任务
# 4. 部署脚本和 CI/CD 环节
# 5. 服务器初始化配置

# 适合 Python 的场景：
# 1. 复杂的业务逻辑
# 2. 需要调用 API/数据库
# 3. JSON/YAML 数据解析与转换
# 4. 跨平台工具开发
# 5. 需要单元测试保证质量
```

> 💡 **最佳实践**：我的习惯是"Shell 做胶水，Python 做逻辑"。用 Shell 编排流程、调用系统工具，当需要复杂处理时交给 Python 脚本。比如备份脚本中，调度和压缩用 Shell，数据一致性校验用 Python。

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q9：你写的一个脚本在生产环境出了问题（比如误删了文件），你后来怎么优化避免再犯？

**面试官意图：** 考察从故障中学习和改进的能力。

**完美解答：**

曾经踩过的坑：日志清理脚本中 `find` 路径写成了变量但未做空值校验，导致某次环境变量被清空时，`find $LOG_DIR -delete` 变成了 `find -delete`，相当于删除了当前目录下所有文件。

**修复后的脚本加了 4 道保险：**

```bash
#!/bin/bash
# 安全版日志清理脚本

# 保险 1：变量空值校验
LOG_DIR="/var/log/app"
if [ -z "$LOG_DIR" ]; then
    echo "ERROR: LOG_DIR 为空，拒绝执行"
    exit 1
fi

# 保险 2：路径存在性校验
if [ ! -d "$LOG_DIR" ]; then
    echo "ERROR: 目录 $LOG_DIR 不存在"
    exit 1
fi

# 保险 3：路径合理性校验（防止删除关键系统目录）
SAFE_PATHS=("/var/log" "/opt/app/logs" "/data/logs")
IS_SAFE=false
for safe_path in "${SAFE_PATHS[@]}"; do
    if [[ "$LOG_DIR" == "$safe_path"* ]]; then
        IS_SAFE=true
        break
    fi
done
if [ "$IS_SAFE" = false ]; then
    echo "ERROR: $LOG_DIR 不在安全目录列表中"
    exit 1
fi

# 保险 4：先 dry-run 预览再执行
echo "以下文件将被清理:"
find "$LOG_DIR" -name "*.log" -mtime +30 -print
echo "----------------"

read -p "确认清理以上文件？[y/N] " confirm
if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
    echo "取消执行"
    exit 0
fi

find "$LOG_DIR" -name "*.log" -mtime +30 -delete
echo "清理完成"
```

> 🎯 **经验教训**：别相信任何变量不为空，别相信目录范围安全，别直接执行破坏性操作。这条原则适用于所有运维脚本。

---

### Q10：线上 Java 服务日志文件增长太快，磁盘快满了，你用一条 Shell 命令先应急再根治？

**面试官意图：** 考察应急处理能力和优化思维。

**完美解答：**

**应急处理（先保证服务不挂）：**

```bash
# 方法 1：清空日志文件而不重启服务（推荐）
> /opt/app/logs/app.log

# 方法 2：截断文件到指定大小
truncate -s 0 /opt/app/logs/app.log

# 方法 3：备份当前日志并新建
mv /opt/app/logs/app.log /opt/app/logs/app.log.$(date +%Y%m%d)
systemctl restart app  # 某些应用需要重启才能写入新文件
```

> ⚠️ `rm` 再重新创建文件会导致文件描述符失效，Java 进程可能不再写日志。用 `>` 或 `truncate` 直接在原文件上截断，进程 fd 不受影响。

**根治方案（配置 logrotate）：**

```bash
# /etc/logrotate.d/java-app
/opt/app/logs/*.log {
    daily                  # 每天轮转
    rotate 30              # 保留 30 天
    compress               # 压缩旧日志
    delaycompress          # 延迟一天压缩
    missingok              # 文件缺失不报错
    notifempty             # 空文件不轮转
    copytruncate           # 先复制后截断（不影响进程写入）
    postrotate
        # 可以在此触发日志备份或通知
    endscript
}
```

**效果对比：**
```bash
# 配置前：日志无限制增长 → 磁盘写满 → 服务宕机
# 配置后：每天切割 → 压缩 → 保留 30 天 → 自动清理
```

---

### Q11：如何在 Shell 中实现"并发执行多个任务"并等待所有任务完成？

**面试官意图：** 考察 Shell 编程中并发处理和进程管理的进阶能力。

**完美解答：**

Shell 原生支持并发执行，利用 `&` 后台运行 + `wait` 同步等待的机制：

**基础版：**
```bash
#!/bin/bash
# 并行备份多个数据库

backup_db() {
    local db=$1
    mysqldump -u root "$db" > "/backup/${db}.sql" 2>/dev/null
    echo "$db 备份完成"
}

# 并发执行
backup_db "db1" &
backup_db "db2" &
backup_db "db3" &

# 等待所有子进程结束
wait
echo "所有数据库备份完成"
```

**进阶版（控制并发数量 + 错误处理）：**
```bash
#!/bin/bash
# 并行 SSH 批量执行命令，控制最多 5 个并发

MAX_PARALLEL=5
HOSTS=("192.168.1.1" "192.168.1.2" "192.168.1.3" "192.168.1.4" "192.168.1.5")
COMMAND="df -h | grep /dev/sda1"

run_command() {
    local host=$1
    ssh -o ConnectTimeout=5 "$host" "$COMMAND" 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "[$host] 执行成功"
    else
        echo "[$host] 执行失败"
    fi
}

count=0
for host in "${HOSTS[@]}"; do
    run_command "$host" &
    ((count++))
    if [ $count -ge $MAX_PARALLEL ]; then
        wait -n  # 等待任意一个完成
        ((count--))
    fi
done

wait  # 等待剩余任务完成
echo "所有任务执行完毕"
```

> 💡 在面试中提这个，说明你写 Shell 不只会顺序执行，有"工程效率意识"，这对面试官是一个很加分的信号。

---

### Q12：你遇到过 Shell 脚本最难调试的问题是什么？怎么解决的？

**面试官意图：** 考察调试能力和系统性解决问题的思维。

**完美解答：**

**我遇到最棘手的问题：** 一个部署脚本在某些服务器上正常，在某些服务器上 `sed` 替换不生效，但没有任何报错。排查了整整一天。

**定位过程：**

1. 加入 `set -x` 开启调试模式，发现 `sed` 的 `-i` 参数在 Linux（GNU sed）和 macOS（BSD sed）上语法不同
2. GNU sed 是 `sed -i 's/old/new/g' file`
3. BSD sed 需要 `sed -i '' 's/old/new/g' file`

**解决方案：**
```bash
# 方案 1：检测平台并适配
OS=$(uname -s)
if [ "$OS" = "Darwin" ]; then
    SED_CMD="sed -i ''"
else
    SED_CMD="sed -i"
fi
$SED_CMD 's/old/new/g' file.txt

# 方案 2：使用更通用的 Perl（跨平台一致）
perl -i -pe 's/old/new/g' file.txt

# 方案 3：用临时文件（兼容性最好）
sed 's/old/new/g' file.txt > file.txt.tmp && mv file.txt.tmp file.txt
```

**调试技巧总结：**
```bash
# 1. 开启调试模式
set -x        # 打印每一条命令和展开结果
set -e        # 出错立即退出
set -o pipefail  # 管道中任一步出错即退出

# 2. 语法检查（不执行）
bash -n script.sh

# 3. 分步检查
bash -x script.sh 2>&1 | head -50  # 只看前 50 步

# 4. 检查退出码
ls /nonexist; echo "退出码: $?"
```

> 🎯 总结：跨平台兼容性是 Shell 脚本中最容易被忽视的坑。从此以后，我所有脚本开头都会加一句 `#!/bin/bash` 声明 + `set -eo pipefail`。

---

## 💎 面试加分金句

1. "我写 Shell 脚本的黄金法则：先想好容错再写功能。所有关键步骤都有退出码检查，所有变量都有空值校验。"
2. "自动化不是'写个脚本就自动了'，而是要设计'异常路径'——磁盘满了怎么办？数据库连不上怎么办？脚本中断了怎么恢复？"
3. "我习惯用 Shell 做系统层自动化（因为系统调用效率最高），用 Python 做复杂业务逻辑（因为数据结构更强），两者结合才是最佳实践。"
4. "每个生产级脚本都包含三个要素：日志可追溯、状态可监控、执行可回滚。少一个都不能上生产。"
5. "监控脚本不是为了发现宕机，而是为了在用户发现之前你已经发现了。所以我坚持做到实时告警 + 自动恢复。"

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| 怎么处理脚本中的竞态条件？ | 用 `flock` 文件锁防止同一脚本多次并发执行，或用 `PID` 文件检测 |
| Shell 数组和关联数组怎么用？ | 普通数组 `arr=(a b c)`，关联数组 `declare -A map; map[key]=value`（Bash 4+） |
| 为什么选择 `[[ ]]` 而不是 `[ ]`？ | 安全（空值不报错）、功能强（正则、通配符）、可读性好 |
| JSON 在 Shell 中怎么解析？ | 推荐 `jq` 工具；如果没有，可以用 `grep -oP` 正则提取 |
| 你写过多少行 Shell 脚本？ | 用具体数字量化：备份脚本 200+ 行，部署脚本 300+ 行，巡检脚本 400+ 行 |

## 🔗 关联知识点

- [Linux必做项目清单-面试问答](./Linux必做项目清单-面试问答.md) — Shell 脚本的基础是 Linux 命令
- [三大核心项目-面试问答](./三大核心项目-面试问答.md) — 自动化部署脚本是项目落地的关键环节
- CI/CD 流水线 — Shell 脚本是 Jenkins/GitHub Actions 中流水线的核心
- 定时任务 Crontab — Shell 脚本 + Crontab 是自动化运维的最佳拍档
