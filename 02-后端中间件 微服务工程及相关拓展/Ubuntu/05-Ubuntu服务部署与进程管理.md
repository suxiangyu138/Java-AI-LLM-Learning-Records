# Ubuntu 服务部署与进程管理

> 🚀 systemd 服务管理、Spring Boot 部署、Nginx 反向代理、日志轮转、进程守护 —— Java 应用生产化部署

---

## 📚 目录

1. [systemd 服务管理](#1-systemd-服务管理)
2. [Spring Boot 生产部署](#2-spring-boot-生产部署)
3. [Nginx 反向代理](#3-nginx-反向代理)
4. [日志管理](#4-日志管理)
5. [进程排查工具链](#5-进程排查工具链)

---

## 1. systemd 服务管理

### 1.1 核心命令

```bash
# ==== 服务生命周期 ====
sudo systemctl start app         # 启动
sudo systemctl stop app          # 停止
sudo systemctl restart app       # 重启
sudo systemctl reload app        # 重载配置（不重启进程）
sudo systemctl status app        # 查看状态
sudo systemctl enable app        # 开机自启
sudo systemctl disable app       # 取消自启
sudo systemctl is-active app     # 检查是否运行

# ==== 日志 ====
sudo journalctl -u app           # 查看服务日志
sudo journalctl -u app -f        # 实时跟踪
sudo journalctl -u app --since "10 min ago"
sudo journalctl -u app -n 100    # 最近 100 行

# ==== 系统 ====
sudo systemctl daemon-reload     # 重载 systemd 配置
sudo systemctl list-units --type=service  # 列出所有服务
sudo systemctl list-unit-files | grep enabled  # 开机自启的服务
```

### 1.2 Spring Boot systemd 配置

```ini
# /etc/systemd/system/myapp.service
[Unit]
Description=My Spring Boot Application
After=network.target

[Service]
Type=simple
User=deploy
Group=deploy
WorkingDirectory=/opt/myapp
ExecStart=/usr/bin/java -jar /opt/myapp/app.jar --spring.profiles.active=prod
ExecStop=/bin/kill -15 $MAINPID
Restart=on-failure
RestartSec=10
StandardOutput=append:/var/log/myapp/app.log
StandardError=append:/var/log/myapp/app.log

# JVM 选项（通过环境变量）
Environment="JAVA_OPTS=-Xms512m -Xmx2048m -XX:+UseG1GC"

# 资源限制
LimitNOFILE=65535
LimitNPROC=4096

[Install]
WantedBy=multi-user.target
```

```bash
# 部署步骤
sudo systemctl daemon-reload
sudo systemctl enable myapp
sudo systemctl start myapp
sudo systemctl status myapp   # 验证
sudo journalctl -u myapp -f   # 查看日志
```

---

## 2. Spring Boot 生产部署

### 2.1 部署目录结构

```text
/opt/myapp/
├── app.jar              ← 当前运行的 jar
├── releases/            ← 历史版本
│   ├── app-1.0.0.jar
│   └── app-1.0.1.jar
├── config/
│   └── application-prod.yml
├── logs/
│   └── app.log
└── deploy.sh            ← 部署脚本
```

### 2.2 滚动部署脚本

```bash
#!/bin/bash
# deploy.sh —— Spring Boot 零停机更新

set -e
APP_NAME="myapp"
APP_DIR="/opt/${APP_NAME}"
JAR_FILE="${APP_DIR}/app.jar"
NEW_VERSION="$1"

if [ -z "$NEW_VERSION" ]; then
    echo "用法: $0 <version> 如: $0 1.0.2"
    exit 1
fi

echo "=== 部署 ${APP_NAME} v${NEW_VERSION} ==="

# 1. 备份旧版本
if [ -f "$JAR_FILE" ]; then
    cp "$JAR_FILE" "${APP_DIR}/releases/backup-$(date +%Y%m%d%H%M%S).jar"
fi

# 2. 部署新版本（假设新 jar 已在 /tmp）
cp "/tmp/app-${NEW_VERSION}.jar" "$JAR_FILE"
cp "/tmp/app-${NEW_VERSION}.jar" "${APP_DIR}/releases/"

# 3. 重启服务
sudo systemctl restart ${APP_NAME}

# 4. 等待启动
sleep 10
sudo systemctl is-active --quiet ${APP_NAME}

# 5. 健康检查
HEALTH_URL="http://localhost:8080/actuator/health"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" $HEALTH_URL)

if [ "$HTTP_CODE" -eq 200 ]; then
    echo "✅ 部署成功！v${NEW_VERSION}"
else
    echo "❌ 健康检查失败！HTTP ${HTTP_CODE}"
    echo "回滚中..."
    sudo systemctl stop ${APP_NAME}
    # 恢复旧版本...
    exit 1
fi
```

### 2.3 Spring Boot 外置配置

```yaml
# /opt/myapp/config/application-prod.yml
# 外置配置优先于 jar 内的 application.yml

server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://prod-db.internal:3306/mydb
    username: ${DB_USERNAME}      # 从环境变量读取
    password: ${DB_PASSWORD}
  redis:
    host: prod-redis.internal
    port: 6379

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

---

## 3. Nginx 反向代理

### 3.1 安装与基本配置

```bash
sudo apt install nginx
sudo systemctl enable nginx
sudo systemctl start nginx
```

```nginx
# /etc/nginx/sites-available/api.example.com
server {
    listen 80;
    server_name api.example.com;

    # 日志
    access_log /var/log/nginx/api_access.log;
    error_log  /var/log/nginx/api_error.log;

    # 请求体大小限制
    client_max_body_size 10M;

    # 反向代理到 Spring Boot
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 超时
        proxy_connect_timeout 30s;
        proxy_read_timeout 60s;
    }

    # 静态资源直接由 Nginx 返回
    location /static/ {
        root /opt/myapp;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

```bash
# 启用站点
sudo ln -s /etc/nginx/sites-available/api.example.com /etc/nginx/sites-enabled/
sudo nginx -t                    # 检查配置语法
sudo systemctl reload nginx      # 重载配置（不中断服务）
```

---

## 4. 日志管理

### 4.1 logrotate 日志轮转

```bash
# /etc/logrotate.d/myapp
/var/log/myapp/*.log {
    daily                   # 每天轮转
    rotate 30               # 保留 30 天
    size 100M               # 超过 100MB 也轮转
    compress                # 压缩旧日志
    delaycompress           # 延迟一天压缩
    missingok               # 文件不存在不报错
    notifempty              # 空文件不轮转
    copytruncate            # 复制后清空（Java 应用必须！）
    dateext                 # 日期后缀
}

# 验证
sudo logrotate -d /etc/logrotate.d/myapp    # 调试模式
sudo logrotate -f /etc/logrotate.d/myapp    # 强制执行
```

### 4.2 journald 配置

```bash
# /etc/systemd/journald.conf
[Journal]
SystemMaxUse=500M        # 日志最大占用
SystemMaxFileSize=50M    # 单个文件大小
MaxRetentionSec=30day    # 最多保留 30 天

sudo systemctl restart systemd-journald
```

---

## 5. 进程排查工具链

```bash
# ==== 进程是否存活 ====
systemctl status myapp
ps aux | grep java

# ==== CPU/内存异常 ====
top -p $(pgrep -f app.jar)       # 单个进程监视
htop -p $(pgrep -f app.jar)      # 更友好的版本

# ==== 线程分析 ====
# 找到 Java 进程 PID
jps -l                            # 列出所有 Java 进程

# 线程快照（jstack）
jstack PID > thread_dump.txt      # 导出线程快照
jstack PID | grep -A 10 "BLOCKED" # 查找死锁

# 找出 CPU 最高的线程
top -H -p PID                     # 显示所有线程
# 记下 CPU 最高的线程 ID (TID) → 转十六进制
printf "%x\n" TID
# 在 jstack 输出中搜索该 nid
grep "nid=0xHEX" thread_dump.txt

# ==== 内存分析 ====
jmap -heap PID                    # 堆概况
jmap -histo PID | head -20        # 对象统计 Top20
jmap -dump:live,file=heap.hprof PID  # 堆 dump
```

---

> 🎯 **核心要点**：**systemd** 管理服务生命周期（开机启动+自动重启），**Nginx** 做反向代理和静态资源，**logrotate** 管日志轮转（`copytruncate` 对 Java 很重要），**jstack/jmap** 做线上排查。

---

*创建于：2026年7月*
