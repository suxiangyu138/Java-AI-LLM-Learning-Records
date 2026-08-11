# 05 - Linux 服务管理

> 🎯 从 systemd 服务单元到定时任务、从文件共享到反向代理 — 掌握 Linux 服务管理是让 Java 应用"活着"且"活得好"的基础能力

---

## 📚 目录

1. [systemd 服务管理](#1-systemd-服务管理)
2. [Crontab 定时任务](#2-crontab-定时任务)
3. [FTP / SFTP 文件传输](#3-ftp-sftp-文件传输)
4. [NFS 网络文件共享](#4-nfs-网络文件共享)
5. [Samba 跨平台文件共享](#5-samba-跨平台文件共享)
6. [Apache HTTP 反向代理](#6-apache-http-反向代理)
7. [邮件服务基础](#7-邮件服务基础)
8. [Java 后端实战场景](#8-java-后端实战场景)
9. [常见问题](#9-常见问题)

---

## 1. systemd 服务管理

> 💡 systemd 是现代 Linux 的 init 系统（PID=1），所有服务都作为 "unit" 管理。

### 1.1 核心命令速查

| 命令 | 说明 |
|------|------|
| `systemctl start <name>` | 启动服务 |
| `systemctl stop <name>` | 停止服务 |
| `systemctl restart <name>` | 重启服务 |
| `systemctl reload <name>` | 重载配置（不中断服务） |
| `systemctl enable <name>` | 开机自启 |
| `systemctl disable <name>` | 取消开机自启 |
| `systemctl status <name>` | 查看状态 |
| `systemctl is-active <name>` | 是否运行中 |
| `systemctl is-enabled <name>` | 是否开机自启 |
| `systemctl cat <name>` | 查看 unit 文件内容 |
| `systemctl list-units --type=service` | 列出所有 service unit |
| `systemctl list-unit-files` | 列出所有 unit 安装状态 |
| `systemctl daemon-reload` | 重载 unit 文件（修改 unit 后执行） |
| `journalctl -u <name> -f` | 实时查看服务日志 |
| `journalctl -u <name> --since "1h ago"` | 查看最近 1 小时日志 |

### 1.2 Java 应用自启动 Unit（⭐ 生产级模板）

```ini
# /etc/systemd/system/myapp.service
[Unit]
Description=My Spring Boot Application
Documentation=https://wiki.example.com/myapp
After=network.target
Wants=network.target

[Service]
Type=simple
User=appuser
Group=appuser
WorkingDirectory=/opt/myapp
ExecStart=/usr/bin/java \
    -Xms2g -Xmx4g \
    -XX:+UseG1GC \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/opt/myapp/logs/ \
    -jar /opt/myapp/current/app.jar
ExecStop=/bin/kill -15 $MAINPID
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=myapp

# 安全加固
NoNewPrivileges=yes
PrivateTmp=yes
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/opt/myapp/logs /opt/myapp/data

# 资源限制
LimitNOFILE=65535
LimitNPROC=32768

[Install]
WantedBy=multi-user.target
```

```bash
# 部署
sudo systemctl daemon-reload
sudo systemctl enable --now myapp
sudo systemctl status myapp

# 日常运维
sudo systemctl restart myapp      # 重启应用
journalctl -u myapp -f            # 查看日志
sudo systemctl stop myapp         # 停止应用
```

### 1.3 Service Type 对比

| Type | 说明 | 适用场景 |
|------|------|----------|
| `simple` | 默认类型，ExecStart 启动的进程即主进程 | ⭐ Java 应用、大多数长期运行服务 |
| `forking` | 父进程 fork 子进程后退出 | 传统守护进程（Nginx、Apache） |
| `oneshot` | 执行一次后退出 | 初始化脚本、数据库迁移 |
| `notify` | 服务就绪后发送通知 | 支持 sd_notify 的应用 |
| `idle` | 所有 job 完成后才启动 | 延迟启动 |

---

## 2. Crontab 定时任务

> 💡 Cron 是 Linux 定时任务调度器，后端必备：日志轮转、数据备份、健康检查、证书续期。

### 2.1 语法格式

```
分  时  日  月  星期  命令
│   │   │   │   │     │
│   │   │   │   │     └── 要执行的命令（建议绝对路径）
│   │   │   │   └─────── 星期 (0-7, 0和7都表示周日)
│   │   │   └─────────── 月份 (1-12)
│   │   └─────────────── 日期 (1-31)
│   └─────────────────── 小时 (0-23)
└─────────────────────── 分钟 (0-59)

特殊符号：* 任意  , 枚举  - 范围  / 步长
```

### 2.2 常用命令

| 命令 | 说明 |
|------|------|
| `crontab -e` | 编辑当前用户定时任务 |
| `crontab -l` | 列出当前用户定时任务 |
| `crontab -r` | 删除所有定时任务（⚠️ 危险） |
| `cat /etc/crontab` | 系统级定时任务 |
| `ls /etc/cron.d/` | 模块化 cron 配置目录 |
| `ls /etc/cron.daily/` | 每日执行的脚本 |
| `systemctl status crond` | 查看 cron 服务状态 |
| `tail -f /var/log/cron` | 查看 cron 执行日志 |

### 2.3 Java 后端典型场景

```bash
# ═══ 应用运维 ═══
# 每天凌晨 2:30 重启 Java 应用（配合滚动部署）
30 2 * * * /opt/myapp/bin/restart.sh >> /var/log/myapp-cron.log 2>&1

# 每 10 分钟检查 Java 进程是否存活
*/10 * * * * pgrep -f "spring-boot" || /opt/myapp/bin/start.sh

# ═══ 日志管理 ═══
# 每天凌晨 3 点清理 30 天前的日志
0 3 * * * find /opt/myapp/logs -name "*.log" -mtime +30 -delete

# 每周日 4 点压缩上周日志
0 4 * * 0 tar -czf /backup/logs-$(date +\%Y\%m\%d).tar.gz /opt/myapp/logs/

# ═══ 数据备份 ═══
# 工作日 9 点备份数据库
0 9 * * 1-5 /opt/scripts/backup-db.sh

# 每月 1 号凌晨归档上个月数据
0 1 1 * * /opt/scripts/monthly-archive.sh

# ═══ 健康检查 ═══
# 每分钟检查应用接口
* * * * * curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health | grep -q 200 || echo "Health check failed" | mail -s "Alert" admin@example.com
```

### 2.4 Crontab 注意事项

> ⚠️ **常见坑**：
> - `%` 在 cron 中有特殊含义，需转义为 `\%`
> - 环境变量与交互式 Shell 不同，建议脚本开头 `source /etc/profile` 或使用绝对路径
> - crontab 编辑后立即生效，无需重启 crond
> - 脚本必须有执行权限 `chmod +x`
> - 建议输出重定向到日志：`>> /var/log/xxx.log 2>&1`

---

## 3. FTP / SFTP 文件传输

### 3.1 vsftpd 安装与配置

```bash
# 安装
yum install -y vsftpd
systemctl enable --now vsftpd
```

`/etc/vsftpd/vsftpd.conf` 关键参数：

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| `anonymous_enable` | `NO` | 禁止匿名登录 |
| `local_enable` | `YES` | 允许本地用户登录 |
| `write_enable` | `YES` | 允许写入 |
| `chroot_local_user` | `YES` | 将用户禁锢在其家目录 |
| `allow_writeable_chroot` | `YES` | chroot 目录允许写（较新版本需要） |
| `pasv_min_port` / `pasv_max_port` | `30000` `31000` | 被动模式端口范围 |

```bash
# 创建 FTP 用户
useradd -d /var/ftp/share -s /sbin/nologin ftpuser
passwd ftpuser
chmod 755 /var/ftp/share

# 防火墙放行
firewall-cmd --add-service=ftp --permanent
firewall-cmd --add-port=30000-31000/tcp --permanent
firewall-cmd --reload
```

### 3.2 vsftpd vs SFTP

| 特性 | vsftpd (FTP) | SFTP |
|------|-------------|------|
| 加密 | 需配置 TLS（FTPS） | **默认 SSH 加密** |
| 端口 | 21 (控制) + 动态数据端口 | **22 (与 SSH 共用)** |
| 配置复杂度 | 较高（被动模式、防火墙） | **零配置**（有 SSH 即有 SFTP） |
| 性能 | 纯文件传输略快 | 加密有少量开销 |
| 推荐度 | 对外提供匿名下载 | ⭐ **内部运维首选** |

> 💡 **建议**：内部运维使用 SFTP（`sftp user@host`），无需额外安装配置，安全可靠。对外提供文件下载再用 FTP/FTPS。

---

## 4. NFS 网络文件共享

> 💡 NFS (Network File System) 允许多台服务器共享同一文件系统。典型场景：多台 Web 服务器共享上传文件目录。

### 4.1 服务端配置

```bash
# 安装
yum install -y nfs-utils rpcbind
systemctl enable --now rpcbind nfs-server

# 配置共享目录
mkdir -p /data/share
chmod 755 /data/share
```

`/etc/exports` 配置：

```
# 格式：<目录> <客户端>(<选项>)
/data/share 192.168.1.0/24(rw,sync,no_root_squash)
/data/share *(ro,sync)                           # 所有人只读
```

| 选项 | 说明 |
|------|------|
| `rw` / `ro` | 读写 / 只读 |
| `sync` / `async` | 同步写入（数据安全）/ 异步写入（性能好） |
| `no_root_squash` | 客户端 root 保持 root 权限（⚠️ 安全风险） |
| `root_squash` | 客户端 root 映射为 nobody（推荐） |
| `all_squash` | 所有用户映射为 anonymous |

```bash
exportfs -arv                    # 重载配置
showmount -e localhost           # 查看共享列表
```

### 4.2 客户端挂载

```bash
# 安装
yum install -y nfs-utils

# 查看可用共享
showmount -e 192.168.1.100

# 临时挂载
mount -t nfs 192.168.1.100:/data/share /mnt/nfs

# 永久挂载 (/etc/fstab)
192.168.1.100:/data/share  /mnt/nfs  nfs  defaults,_netdev,soft,intr  0  0
```

> ⚠️ **NFS 挂载卡住**：使用 `umount -l` 强制卸载（懒卸载）。推荐挂载选项加 `soft,intr` 避免网络中断时进程 D 状态。

---

## 5. Samba 跨平台文件共享

> 💡 Samba 实现 Linux 与 Windows 之间的文件共享（SMB/CIFS 协议）。典型场景：Windows 开发者直接访问 Linux 服务器文件。

### 5.1 安装与配置

```bash
# 安装
yum install -y samba samba-client
systemctl enable --now smb nmb
```

`/etc/samba/smb.conf` 配置：

```ini
[global]
workgroup = WORKGROUP
security = user
map to guest = Bad User

[shared]
comment = Shared Directory
path = /srv/samba/shared
browsable = yes
writable = yes
valid users = @smbgroup
create mask = 0644
directory mask = 0755
```

```bash
# 创建用户
useradd smbuser
smbpasswd -a smbuser
groupadd smbgroup
usermod -aG smbgroup smbuser

# 目录权限
mkdir -p /srv/samba/shared
chown -R :smbgroup /srv/samba/shared
chmod 770 /srv/samba/shared

# SELinux（如启用）
setsebool -P samba_enable_home_dirs on
semanage fcontext -a -t samba_share_t "/srv/samba/shared(/.*)?"
restorecon -Rv /srv/samba/shared

# 防火墙
firewall-cmd --add-service=samba --permanent
firewall-cmd --reload
```

### 5.2 客户端访问

```bash
# Windows 资源管理器
\\192.168.1.100\shared

# Linux 挂载
mount -t cifs //192.168.1.100/shared /mnt/smb -o username=smbuser
```

---

## 6. Apache HTTP 反向代理

> 💡 Apache HTTPD 可作为 Java 应用的前置反向代理，提供静态资源、SSL 终结、负载均衡。

### 6.1 基本安装

```bash
# CentOS
yum install -y httpd
systemctl enable --now httpd

# Ubuntu
apt install -y apache2
systemctl enable --now apache2
```

### 6.2 反向代理 Java 应用

```apache
# /etc/httpd/conf.d/myapp.conf
<VirtualHost *:80>
    ServerName api.example.com

    # 反向代理到 Java 后端
    ProxyPreserveHost On
    ProxyPass / http://localhost:8080/
    ProxyPassReverse / http://localhost:8080/

    # 静态资源直接处理
    ProxyPass /static/ !
    DocumentRoot /opt/myapp/static

    # 超时配置
    ProxyTimeout 60

    # 日志
    ErrorLog /var/log/httpd/myapp-error.log
    CustomLog /var/log/httpd/myapp-access.log combined
</VirtualHost>
```

```bash
# 检查配置并重载
apachectl configtest
systemctl reload httpd
```

### 6.3 Apache vs Nginx

| 特性 | Apache | Nginx |
|------|--------|-------|
| 架构 | 进程/线程（prefork/worker/event） | 事件驱动（异步非阻塞） |
| 静态文件 | 一般 | ⭐ 极快 |
| 动态内容 | 原生模块（mod_php 等） | 需反向代理 |
| 配置 | `.htaccess` 灵活 | 集中配置 |
| 内存占用 | 较高 | 低 |
| Java 后端场景 | 传统 LAMP 栈 | ⭐ 反向代理首选 |

> 💡 **建议**：Java 后端优先选 Nginx 或直接 Spring Boot 内置 Tomcat + 云负载均衡。

---

## 7. 邮件服务基础

### 7.1 邮件协议概览

| 协议 | 端口 | 用途 |
|------|------|------|
| SMTP | 25 / 465 (SSL) / 587 (STARTTLS) | 发送邮件 |
| POP3 | 110 / 995 (SSL) | 接收邮件（下载到本地） |
| IMAP | 143 / 993 (SSL) | 接收邮件（保留在服务器） |

### 7.2 mailx 配置（发送告警邮件）

```bash
# 安装
yum install -y mailx

# 配置外部 SMTP（/etc/mail.rc）
set smtp=smtps://smtp.example.com:465
set smtp-auth=login
set smtp-auth-user=alert@example.com
set smtp-auth-password=your-password
set ssl-verify=ignore
set from=alert@example.com

# 发送邮件
echo "Java 进程已重启" | mail -s "[告警] myapp 重启通知" admin@example.com

# 带附件
mail -s "每日日志" -a /var/log/myapp/app.log admin@example.com < /dev/null
```

---

## 8. Java 后端实战场景

### 8.1 完整部署脚本（利用 systemd + cron）

```bash
#!/bin/bash
# /opt/myapp/bin/deploy.sh — Spring Boot 应用部署脚本
set -euo pipefail

APP_NAME="myapp"
APP_DIR="/opt/myapp"
VERSION=$(date +%Y%m%d%H%M%S)
JAR="$APP_DIR/versions/$VERSION/app.jar"

# 1. 创建版本目录
mkdir -p "$APP_DIR/versions/$VERSION"
cp /tmp/deploy/app.jar "$JAR"

# 2. 原子切换版本
ln -snf "$APP_DIR/versions/$VERSION" "$APP_DIR/current"

# 3. 重启服务
systemctl restart $APP_NAME

# 4. 等待启动
sleep 5

# 5. 健康检查
for i in {1..12}; do
    code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)
    if [ "$code" = "200" ]; then
        echo "✅ $APP_NAME v$VERSION 部署成功"
        exit 0
    fi
    sleep 5
done

# 6. 健康检查失败 — 回滚
echo "❌ 健康检查失败，回滚到上一版本"
ln -snf "$(ls -d $APP_DIR/versions/*/ | tail -2 | head -1)" "$APP_DIR/current"
systemctl restart $APP_NAME
exit 1
```

### 8.2 日志轮转（logrotate 替代手动 cron）

```bash
# /etc/logrotate.d/myapp
/opt/myapp/logs/*.log {
    daily
    rotate 30
    missingok
    notifempty
    compress
    delaycompress
    copytruncate
    dateext
    dateformat -%Y%m%d
}
```

---

## 9. 常见问题

### Q1: Crontab 任务不执行？

检查：crond 服务状态 (`systemctl status crond`)、脚本是否有执行权限、是否使用绝对路径、`/var/log/cron` 日志。

### Q2: vsftpd 报 "500 OOPS: vsftpd: refusing to run with writable root inside chroot()"？

`chroot_local_user=YES` 时，用户家目录不能可写。添加 `allow_writeable_chroot=YES` 或修改家目录权限为 555。

### Q3: NFS 挂载点无响应导致系统卡住？

使用 `umount -l` 强制卸载。编辑 `/etc/fstab` 加 `soft,intr` 选项防止下次卡住。

### Q4: Samba 无法从 Windows 访问？

检查防火墙和 SELinux。`setsebool -P samba_enable_home_dirs on`。确保目录有 `samba_share_t` 上下文。

### Q5: Apache ProxyPass 返回 503 Service Unavailable？

确认后端 Java 进程是否存活（`ss -tlnp | grep 8080`）。确认 `mod_proxy` 和 `mod_proxy_http` 模块已加载（`httpd -M | grep proxy`）。

### Q6: systemd 服务启动后立即退出？

查看日志 `journalctl -u <service> -n 50`。常见原因：Type 配置错误（`forking` vs `simple`）、ExecStart 路径不对、环境变量缺失。

---

**上一模块**：[04-Linux 网络与安全](04-Linux-网络与安全.md) ｜ **下一模块**：[06-命令行速查手册](06-命令行速查手册.md) ｜ **返回总览**：[00-知识体系总览](00-Linux知识体系总览.md)

**【参考来源】**
- systemd Unit 文档：https://www.freedesktop.org/software/systemd/man/systemd.unit.html\n- man 手册：`man crontab` `man vsftpd.conf`
