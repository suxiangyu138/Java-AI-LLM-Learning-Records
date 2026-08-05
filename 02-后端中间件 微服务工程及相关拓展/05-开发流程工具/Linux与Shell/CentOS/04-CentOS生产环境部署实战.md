# CentOS 生产环境部署实战

> 🚀 内核调优、systemd 服务、Spring Boot 部署、性能基线、日志轮转、故障排查 —— Java 应用从部署到稳定的全链路

---

## 📚 目录

1. [内核参数调优](#1-内核参数调优)
2. [Systemd 服务深度定制](#2-systemd-服务深度定制)
3. [Spring Boot 生产部署](#3-spring-boot-生产部署)
4. [性能基线检查](#4-性能基线检查)
5. [故障排查工具链](#5-故障排查工具链)

---

## 1. 内核参数调优

### 1.1 sysctl 网络优化

```bash
# /etc/sysctl.d/99-java-performance.conf

# ===== 网络连接优化（高并发 Web 服务必备）=====
net.core.somaxconn = 65535              # 全连接队列长度
net.core.netdev_max_backlog = 65535     # 网卡接收队列
net.ipv4.tcp_max_syn_backlog = 65535    # SYN 队列长度
net.ipv4.tcp_tw_reuse = 1               # 复用 TIME_WAIT 连接
net.ipv4.tcp_fin_timeout = 15           # FIN_WAIT 超时（默认60）
net.ipv4.ip_local_port_range = 1024 65535 # 本地端口范围

# ===== 连接追踪（高并发下防止表满）=====
net.netfilter.nf_conntrack_max = 1048576
net.netfilter.nf_conntrack_tcp_timeout_established = 1200

# ===== 文件描述符 =====
fs.file-max = 655350                    # 系统级文件描述符上限
fs.inotify.max_user_watches = 524288    # 文件监控数（IDE/Jenkins 需要）

# ===== 内存 =====
vm.swappiness = 10                       # 尽量不用 Swap（数据库服务器设 1）
vm.overcommit_memory = 1                # 允许 Overcommit（Java 需要）

# 应用
sudo sysctl -p /etc/sysctl.d/99-java-performance.conf
```

### 1.2 Limits 文件描述符

```bash
# /etc/security/limits.d/99-java.conf
# Java 应用需要大量的文件描述符（socket + jar 内的文件）
deploy   soft    nofile    65536
deploy   hard    nofile    65536
deploy   soft    nproc     4096
deploy   hard    nproc     4096

# 验证：切换到 deploy 用户
ulimit -n    # 应显示 65536
```

---

## 2. Systemd 服务深度定制

### 2.1 Spring Boot 完整配置

```ini
# /etc/systemd/system/myapp.service
[Unit]
Description=My Spring Boot Application
After=network.target
Wants=network.target

[Service]
Type=simple
User=deploy
Group=deploy

WorkingDirectory=/opt/myapp
ExecStart=/usr/bin/java \
    -Xms1024m -Xmx2048m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/var/log/myapp/heap.hprof \
    -XX:+ExitOnOutOfMemoryError \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Shanghai \
    -jar /opt/myapp/app.jar \
    --spring.profiles.active=prod \
    --server.port=8080

# 优雅关闭（Spring Boot 2.3+ 支持）
ExecStop=/bin/kill -15 $MAINPID

Restart=on-failure
RestartSec=10
# Restart=always 不推荐！配置错误会无限重启

# 日志
StandardOutput=append:/var/log/myapp/stdout.log
StandardError=append:/var/log/myapp/stderr.log

# 环境变量
Environment="JAVA_HOME=/usr/lib/jvm/java-21-openjdk"
Environment="SPRING_DATASOURCE_URL=jdbc:mysql://prod-db.internal:3306/mydb"

# 安全限制
LimitNOFILE=65536
LimitNPROC=4096
# 禁止 fork 子进程（安全）
NoNewPrivileges=yes
PrivateTmp=yes

# 依赖其他服务
After=mysql.service redis.service

[Install]
WantedBy=multi-user.target
```

### 2.2 部署 SOP

```bash
# 部署新版本
sudo systemctl stop myapp
cp /tmp/app-new.jar /opt/myapp/app.jar
sudo systemctl start myapp

# 等待启动完成
sleep 10
sudo systemctl is-active --quiet myapp && echo "✅ 启动成功" || echo "❌ 启动失败"

# 健康检查
curl -f http://localhost:8080/actuator/health || exit 1

# 查看日志
sudo journalctl -u myapp -f

# 常用操作
sudo systemctl restart myapp     # 重启
sudo systemctl status myapp      # 状态
sudo systemctl enable myapp      # 开机启动
```

---

## 3. Spring Boot 生产部署

### 3.1 目录结构

```text
/opt/myapp/
├── app.jar                    ← 当前版本（符号链接 → releases/app-1.3.0.jar）
├── config/
│   └── application-prod.yml   ← 外置配置
├── releases/
│   ├── app-1.2.0.jar
│   └── app-1.3.0.jar
└── deploy.sh                  ← 部署脚本
```

### 3.2 部署脚本

```bash
#!/bin/bash
# deploy.sh — 滚动更新 + 健康检查 + 失败回滚
set -euo pipefail

APP="myapp"
VERSION="${1:?请指定版本号，如: $0 1.3.0}"
APP_DIR="/opt/${APP}"
JAR_PATH="${APP_DIR}/app.jar"
RELEASE_JAR="${APP_DIR}/releases/app-${VERSION}.jar"

echo "=== 部署 ${APP} v${VERSION} ==="

# 1. 备份当前版本
if [ -L "$JAR_PATH" ]; then
    OLD=$(readlink -f "$JAR_PATH")
    cp "$OLD" "${APP_DIR}/releases/backup-$(date +%Y%m%d%H%M%S).jar"
fi

# 2. 更新符号链接
ln -sfn "$RELEASE_JAR" "$JAR_PATH"

# 3. 重启
sudo systemctl restart ${APP}
sleep 15

# 4. 健康检查（最多重试 5 次）
for i in {1..5}; do
    if curl -sf http://localhost:8080/actuator/health > /dev/null; then
        echo "✅ 部署成功！v${VERSION}"
        sudo systemctl status ${APP} --no-pager -l | head -5
        exit 0
    fi
    echo "⏳ 等待健康检查... ($i/5)"
    sleep 5
done

# 5. 健康检查失败 → 回滚
echo "❌ 部署失败，回滚中..."
ln -sfn "$OLD" "$JAR_PATH"
sudo systemctl restart ${APP}
echo "已回滚"
exit 1
```

### 3.3 外置配置

```yaml
# /opt/myapp/config/application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:3306/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASS}           # 从环境变量读取！
  redis:
    host: ${REDIS_HOST}
    port: 6379

server:
  port: ${SERVER_PORT:8080}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

---

## 4. 性能基线检查

```bash
#!/bin/bash
# baseline-check.sh —— 快速检查服务器基础性能

echo "=== CPU ==="
lscpu | grep "Model name\|CPU(s):"

echo ""
echo "=== 内存 ==="
free -h

echo ""
echo "=== 磁盘 ==="
df -h / /var /opt 2>/dev/null

echo ""
echo "=== 网络 ==="
echo "监听端口:"; ss -tlnp | grep -E ':(80|443|8080|3306|6379)\s' 2>/dev/null

echo ""
echo "=== 内核 ==="
uname -r

echo ""
echo "=== 系统负载 ==="
uptime

echo ""
echo "=== SELinux ==="
getenforce

echo ""
echo "=== 防火墙 ==="
sudo firewall-cmd --list-services 2>/dev/null
sudo firewall-cmd --list-ports 2>/dev/null
```

---

## 5. 故障排查工具链

| 问题 | 命令 | 说明 |
|------|------|------|
| 端口被占用 | `ss -tlnp \| grep PORT` | 查谁在监听 |
| | `lsof -i :8080` | 同上 |
| 进程是否存活 | `ps aux \| grep java` | 看进程 |
| | `systemctl status myapp` | 看服务 |
| CPU 飙高 | `top -c -p $(pgrep -f app.jar)` | |
| 内存异常 | `free -h && cat /proc/meminfo` | |
| 磁盘满 | `df -h && du -sh /var/log/* \| sort -hr \| head` | 定位大文件 |
| 日志排错 | `journalctl -u myapp --since "10 min ago" -n 50` | |
| | `tail -f /var/log/myapp/*.log` | |
| SELinux 阻塞 | `ausearch -m avc -ts recent` | |

```bash
# 一键 Java 进程诊断
PID=$(pgrep -f app.jar)
echo "PID: $PID"
echo "线程数: $(ps -o nlwp -p $PID | tail -1)"
echo "打开文件数: $(ls /proc/$PID/fd | wc -l)"
echo "内存 RSS: $(ps -o rss -p $PID | tail -1) KB"
```

---

> 🎯 **生产三件套**：**内核调优**（sysctl limits）保证并发能力、**systemd 完整配置**（资源限制+日志+优雅关闭）、**部署脚本+健康检查**确保零失误。

---

**返回总览**：[00-CentOS知识体系总览](./00-CentOS知识体系总览.md)

---

*创建于：2026年7月*
