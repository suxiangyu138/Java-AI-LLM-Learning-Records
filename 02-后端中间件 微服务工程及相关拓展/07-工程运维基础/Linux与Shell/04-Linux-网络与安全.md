# 04 - Linux 网络与安全

> 🎯 网络是服务的"血管"、安全是服务的"免疫系统" — 从 TCP/IP 配置到防火墙策略、从 SSH 远程管理到入侵排查，后端工程师必须能独立保障服务的连通性和安全性

---

## 目录

1. [网络配置](#1-网络配置)
2. [网络诊断工具集](#2-网络诊断工具集)
3. [防火墙管理](#3-防火墙管理)
4. [SSH 远程管理](#4-ssh-远程管理)
5. [安全加固实践](#5-安全加固实践)
6. [入侵排查与应急响应](#6-入侵排查与应急响应)
7. [SELinux 基础](#7-selinux-基础)
8. [Java 后端网络排查实战](#8-java-后端网络排查实战)
9. [常见问题](#9-常见问题)

---

## 1. 网络配置

### 1.1 接口与 IP 管理

| 命令 | 用途 | 示例 |
|------|------|------|
| `ip addr` | 查看 IP 与网卡状态（替代 `ifconfig`） | `ip addr show eth0` |
| `ip link` | 查看/启停网卡 | `ip link set eth0 up` |
| `ip route` | 查看/管理路由表 | `ip route show` |
| `ss` | 查看 Socket 统计（替代 `netstat`） | `ss -tlnp` |
| `nmcli` | NetworkManager 命令行 | `nmcli dev status` |
| `ethtool` | 网卡硬件信息 | `ethtool eth0` |

### 1.2 配置文件路径（按发行版）

| 发行版 | 配置文件路径 |
|--------|-------------|
| CentOS/RHEL 7+ | `/etc/sysconfig/network-scripts/ifcfg-eth0` |
| Ubuntu 18.04+ | `/etc/netplan/*.yaml` |
| 通用 DNS | `/etc/resolv.conf` |

```
# CentOS 静态 IP 配置示例
TYPE=Ethernet
BOOTPROTO=static
NAME=eth0
DEVICE=eth0
ONBOOT=yes
IPADDR=192.168.1.100
NETMASK=255.255.255.0
GATEWAY=192.168.1.1
DNS1=8.8.8.8
DNS2=114.114.114.114
```

### 1.3 路由与 DNS

```bash
# ═══ 路由 ═══
ip route show                              # 查看路由表
ip route add 10.0.0.0/8 via 192.168.1.1 dev eth0   # 添加临时路由

# ═══ DNS ═══
cat /etc/resolv.conf                       # 查看 DNS 配置
nslookup api.example.com                   # DNS 解析测试
dig +short api.example.com                 # 更详细的信息

# ═══ hosts 文件 ═══
cat /etc/hosts                             # 本地 DNS（优先级高于 DNS 服务器）
# 127.0.0.1  localhost
# 192.168.1.100  db-internal
```

---

## 2. 网络诊断工具集

### 2.1 连通性测试

| 命令 | 用途 | 示例 |
|------|------|------|
| `ping` | ICMP 连通性 | `ping -c 4 8.8.8.8` |
| `traceroute` | 路由追踪 | `traceroute 10.0.0.1` |
| `telnet` | TCP 端口测试 | `telnet 192.168.1.200 3306` |
| `nc -zv` | 端口扫描 | `nc -zv 192.168.1.200 3306` |
| `mtr` | ping + traceroute 组合 | `mtr 8.8.8.8` |

### 2.2 HTTP 调试

```bash
# curl 常用参数速查
curl -I https://api.example.com                    # 仅响应头
curl -v https://api.example.com                    # 详细输出（调试）
curl -L https://short.link                         # 跟随重定向
curl -s -o /dev/null -w "%{http_code}" https://... # 仅返回状态码
curl -X POST -H "Content-Type: application/json" \
  -d '{"key":"value"}' http://localhost:8080/api   # POST JSON
curl -u user:pass https://api.example.com          # 基本认证
curl -k https://self-signed.example.com            # 忽略 SSL 证书

# Spring Boot Actuator 健康检查
curl -s http://localhost:8080/actuator/health
```

### 2.3 端口与连接分析

```bash
# ═══ 监听端口 ═══
ss -tlnp                              # TCP 监听端口
ss -ulnp                              # UDP 监听端口
ss -tlnp | grep 8080                  # 指定端口

# ═══ 连接状态统计 ═══
ss -s                                 # 总体统计
ss -ant | awk '{print $1}' | sort | uniq -c  # 按状态统计

# ═══ 谁在占用端口 ═══
lsof -i :8080
fuser -v 8080/tcp

# ═══ TCP 连接状态分布 ═══
ss -ant | awk 'NR>1 {print $1}' | sort | uniq -c | sort -rn
# 大量 TIME_WAIT → 短连接过多，考虑连接池
# 大量 CLOSE_WAIT → 应用未正确关闭连接（代码 bug）
```

### 2.4 抓包分析 (tcpdump)

```bash
# 基本使用
tcpdump -i eth0 port 8080 -w capture.pcap   # 抓取 8080 端口流量存文件
tcpdump -i eth0 host 192.168.1.100          # 按 IP 过滤
tcpdump -i any port 3306 -A                  # MySQL 流量，ASCII 输出
tcpdump -r capture.pcap | head -50           # 读取 pcap 文件

# Java 后端常用场景
tcpdump -i eth0 port 8080 and 'tcp[tcpflags] & tcp-syn != 0'  # 抓取 SYN 包
tcpdump -i eth0 'dst port 8080 and tcp[tcpflags] & tcp-rst != 0'  # 抓 RST 包
```

---

## 3. 防火墙管理

### 3.1 firewalld (CentOS 7+ / RHEL 8+)

```bash
# ═══ 基本操作 ═══
systemctl status firewalld
firewall-cmd --state
firewall-cmd --list-all                        # 查看当前区域全部规则
firewall-cmd --get-active-zones                # 查看活跃区域

# ═══ 端口放行（Java 服务常用） ═══
firewall-cmd --permanent --add-port=8080/tcp
firewall-cmd --permanent --add-port=8443/tcp
firewall-cmd --reload

# ═══ 服务放行 ═══
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-service=https

# ═══ 来源 IP 白名单（⭐ 推荐） ═══
firewall-cmd --permanent --add-rich-rule='
  rule family="ipv4"
  source address="192.168.1.0/24"
  port protocol="tcp" port="8080" accept'
firewall-cmd --reload

# ═══ 删除规则 ═══
firewall-cmd --permanent --remove-port=8080/tcp
firewall-cmd --permanent --remove-rich-rule='...'
firewall-cmd --reload
```

### 3.2 iptables（通用，底层工具）

```bash
# ═══ 查看规则 ═══
iptables -L -n -v                              # -n 不反解 DNS, -v 详细信息
iptables -t nat -L -n -v                       # 查看 NAT 表

# ═══ 默认策略 ═══
iptables -P INPUT DROP                         # 默认丢弃入站
iptables -P FORWARD DROP
iptables -P OUTPUT ACCEPT                      # 默认允许出站

# ═══ 基础规则 ═══
iptables -A INPUT -i lo -j ACCEPT              # 放行回环
iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT  # 放行已有连接
iptables -A INPUT -p tcp --dport 22 -j ACCEPT  # 放行 SSH
iptables -A INPUT -p tcp --dport 8080 -j ACCEPT # 放行应用端口

# ═══ 保存与恢复 ═══
iptables-save > /etc/iptables/rules.v4
iptables-restore < /etc/iptables/rules.v4
```

### 3.3 nftables（新一代，替代 iptables）

```bash
nft list ruleset                                # 查看所有规则
nft flush ruleset                               # 清空规则
nft -f /etc/nftables.conf                       # 从文件加载
```

### 3.4 ufw (Ubuntu 默认，iptables 前端)

```bash
ufw status
ufw allow 8080/tcp
ufw allow from 192.168.1.0/24 to any port 8080
ufw enable
```

---

## 4. SSH 远程管理

### 4.1 基本连接

```bash
# ═══ 连接 ═══
ssh -p 22 root@192.168.1.100
ssh -i ~/.ssh/id_rsa -p 2222 deploy@api.example.com

# ═══ 密钥对管理 ═══
# 生成 ED25519 密钥（⭐ 推荐，比 RSA 更快更安全）
ssh-keygen -t ed25519 -C "deploy@example.com"

# 生成 RSA 4096 密钥（兼容旧系统）
ssh-keygen -t rsa -b 4096 -f ~/.ssh/deploy_key

# 复制公钥到服务器
ssh-copy-id -i ~/.ssh/id_ed25519.pub user@server

# 手动追加公钥
cat ~/.ssh/id_ed25519.pub | ssh user@server "cat >> ~/.ssh/authorized_keys"
```

### 4.2 文件传输

```bash
# SCP
scp -P 2222 app.jar deploy@server:/opt/app/
scp -r config/ deploy@server:/opt/app/config/

# SFTP（交互式）
sftp -P 2222 deploy@server
# > put app.jar
# > get /var/log/app.log

# rsync（增量同步，⭐ 推荐大文件/目录）
rsync -avzP --delete ./target/ deploy@server:/opt/myapp/
```

### 4.3 SSH 隧道（端口转发）

```bash
# ═══ 本地转发 ═══
# 将本地 3306 转发到远程服务器的 MySQL（通过跳板机访问内网数据库）
ssh -L 3306:db-internal:3306 jump-server

# ═══ 远程转发 ═══
# 将内网服务 8080 暴露到公网跳板机的 9090
ssh -R 9090:127.0.0.1:8080 jump-server

# ═══ 动态转发（SOCKS 代理） ═══
ssh -D 1080 jump-server
# 浏览器配置 SOCKS5 代理 → 127.0.0.1:1080
```

### 4.4 SSH 客户端配置 (~/.ssh/config)

```ssh-config
# ~/.ssh/config
Host prod-app
    HostName 192.168.1.100
    Port 2222
    User deploy
    IdentityFile ~/.ssh/deploy_key
    LocalForward 3306 127.0.0.1:3306
    ServerAliveInterval 60

Host jump
    HostName jump.example.com
    User admin

Host db-internal
    HostName 10.0.1.50
    User admin
    ProxyJump jump              # 通过跳板机连接
```

```bash
# 使用别名连接
ssh prod-app                    # 等价于 ssh -p 2222 -i ~/.ssh/deploy_key deploy@192.168.1.100
scp app.jar prod-app:/opt/app/
```

### 4.5 sshd 服务端安全配置

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| `Port` | 非 22（如 2222） | 减小暴力扫描风险 |
| `PermitRootLogin` | `no` | 禁止 root 直接登录 |
| `PasswordAuthentication` | `no` | 禁用密码登录，仅允许密钥 |
| `PubkeyAuthentication` | `yes` | 启用密钥认证 |
| `AllowUsers` | `deploy app` | 仅允许指定用户登录 |
| `MaxAuthTries` | `3` | 最大认证尝试次数 |
| `ClientAliveInterval` | `300` | 空闲心跳间隔（秒） |
| `ClientAliveCountMax` | `3` | 心跳超时次数 |
| `UseDNS` | `no` | 关闭反向 DNS 解析（加速连接） |
| `GSSAPIAuthentication` | `no` | 关闭 GSSAPI 认证（加速连接） |

```bash
# 应用配置
sudo vi /etc/ssh/sshd_config
sudo systemctl reload sshd     # 重载配置（不中断现有连接）
```

---

## 5. 安全加固实践

### 5.1 基础加固 Checklist

| 措施 | 命令/配置 | 优先级 |
|------|-----------|:---:|
| 禁用 root SSH 登录 | `/etc/ssh/sshd_config`: `PermitRootLogin no` | 🔴 |
| 修改 SSH 默认端口 | `/etc/ssh/sshd_config`: `Port 2222` | 🟡 |
| 仅密钥登录 | `/etc/ssh/sshd_config`: `PasswordAuthentication no` | 🔴 |
| 限制登录用户 | `/etc/ssh/sshd_config`: `AllowUsers deploy` | 🟡 |
| 安装 fail2ban | `yum install fail2ban -y` | 🟡 |
| 配置防火墙 | 仅开放必要端口（80/443/SSH） | 🔴 |
| 定期更新系统 | `yum update -y` | 🔴 |
| 禁用不需要的服务 | `systemctl disable --now <service>` | 🟢 |
| 审计日志 | `auditd` + `/var/log/secure` | 🟡 |

### 5.2 fail2ban 防暴力破解

```bash
# 安装
yum install fail2ban -y       # CentOS
apt install fail2ban -y       # Ubuntu

# /etc/fail2ban/jail.local
[DEFAULT]
bantime = 3600                # 封禁时间（秒）
findtime = 600                # 检测窗口（秒）
maxretry = 5                  # 最大重试次数

[sshd]
enabled = true
port = 2222
logpath = /var/log/secure
maxretry = 3
bantime = 86400               # SSH 暴力破解封禁 24 小时

# 启动
systemctl enable fail2ban --now
fail2ban-client status sshd   # 查看封禁状态
```

### 5.3 端口敲门 (Port Knocking)

```bash
# 通过预定义的端口访问序列动态打开 SSH 端口
# 安装 knockd: yum install knockd
# 原理：先按顺序"敲击"7000→8000→9000 端口，SSH 端口才开放

# /etc/knockd.conf
[openSSH]
sequence    = 7000,8000,9000
seq_timeout = 5
command     = /sbin/iptables -A INPUT -s %IP% -p tcp --dport 22 -j ACCEPT
tcpflags    = syn

[closeSSH]
sequence    = 9000,8000,7000
seq_timeout = 5
command     = /sbin/iptables -D INPUT -s %IP% -p tcp --dport 22 -j ACCEPT
tcpflags    = syn
```

---

## 6. 入侵排查与应急响应

### 6.1 常见入侵类型

| 类型 | 特征 | Java 后端场景 |
|------|------|-------------|
| 挖矿木马 | CPU 持续 100%，外连矿池 | 常伪装成 java/nginx 进程名 |
| 反弹 Shell | 异常外连连接 | 利用 Web 应用漏洞植入后门 |
| SSH 爆破 | `/var/log/secure` 大量失败记录 | 弱密码/默认端口 |
| WebShell | 网页木马文件 | Tomcat/Nginx 上传漏洞 |
| Rootkit | 隐藏进程/文件 | 内核级后门，难以检测 |
| DDoS 肉鸡 | 大量异常出站流量 | 被植入僵尸网络客户端 |

### 6.2 应急排查步骤

```bash
# ═══ 第1步：查看异常进程 ═══
top -c                                  # 按 CPU 排序
ps aux --sort=-%cpu | head -10          # CPU Top10
ps aux --sort=-%mem | head -10          # 内存 Top10

# ═══ 第2步：检查网络连接 ═══
ss -antp | grep -E "ESTAB|SYN"          # 所有连接
ss -antp | grep -v "192.168\|127.0.0.1" # 排除内网连接
lsof -i                                 # 进程打开的网络连接

# ═══ 第3步：查看登录记录 ═══
last -20                                # 成功登录记录
lastb -20                               # 失败登录记录
cat /var/log/secure | grep "Failed"     # 认证失败记录
cat /var/log/secure | grep "Accepted"   # 认证成功记录

# ═══ 第4步：检查持久化 ═══
systemctl list-unit-files | grep enabled      # 开机自启服务
crontab -l                                     # 用户定时任务
cat /etc/crontab                                # 系统定时任务
ls -la /etc/cron.*                              # 定时任务目录
cat /etc/rc.local                               # 开机执行脚本

# ═══ 第5步：检查可疑文件 ═══
find / -name ".*" -type f -mtime -3 2>/dev/null         # 最近3天新建的隐藏文件
find /tmp -type f -perm /111 2>/dev/null                 # /tmp 下的可执行文件
find /var/www -name "*.php" -o -name "*.jsp" 2>/dev/null # WebShell 特征

# ═══ 第6步：检查系统命令是否被替换 ═══
rpm -Vf /bin/ls                          # 验证文件完整性 (CentOS)
debsums -c                               # 验证文件完整性 (Ubuntu)
```

### 6.3 应急处理操作

```bash
# 1. 断网隔离（在确认不影响业务的前提下）
ifdown eth0

# 2. 备份关键日志
cp -r /var/log /tmp/forensics-logs/

# 3. 删除恶意定时任务
crontab -r                               # 清理用户 cron
> /etc/crontab                            # 清空系统 cron

# 4. 终止恶意进程
kill -9 <pid>

# 5. 删除恶意文件
# 先确认文件来源再删除

# 6. 恢复系统（重装是最彻底的方案）
# 从备份恢复数据，不要直接使用被入侵的系统
```

---

## 7. SELinux 基础

### 7.1 基本操作

```bash
# ═══ 查看状态 ═══
getenforce                              # Enforcing / Permissive / Disabled
sestatus                                # 详细信息

# ═══ 临时切换 ═══
setenforce 0                            # Permissive（仅记录不阻止，调试用）
setenforce 1                            # Enforcing

# ═══ 永久配置 ═══
# /etc/selinux/config
# SELINUX=enforcing

# ═══ 审计日志 ═══
ausearch -m avc -ts recent              # 查看最近的 SELinux 拒绝事件
sealert -a /var/log/audit/audit.log     # 分析审计日志（含修复建议）

# ═══ 文件上下文 ═══
ls -Z /opt/app.jar                      # 查看文件 SELinux 上下文
chcon -t bin_t /opt/app.jar             # 修改上下文
restorecon -v /opt/app.jar              # 恢复默认上下文

# ═══ Boolean 开关 ═══
getsebool -a | grep httpd               # 查看布尔值
setsebool -P httpd_can_network_connect on  # 允许 HTTPD 发起网络连接
```

### 7.2 Java 应用常见 SELinux 问题

| 场景 | 错误 | 解决方案 |
|------|------|----------|
| Java 绑定 80 端口 | `Permission denied` | `setcap 'cap_net_bind_service=+ep'` 或端口转发 |
| 应用写日志到非标准目录 | `Permission denied` | `semanage fcontext -a -t httpd_log_t "/opt/myapp/logs(/.*)?"` |
| 连接外部数据库 | 连接超时 | `setsebool -P httpd_can_network_connect_db on` |

---

## 8. Java 后端网络排查实战

### 8.1 服务端口不通排查流程

```bash
# 故障：curl http://localhost:8080 无响应

# 1. 确认服务是否在监听
ss -tlnp | grep 8080
# 如果没有输出 → 服务未启动或未绑定成功

# 2. 确认防火墙
sudo firewall-cmd --list-all | grep 8080
sudo iptables -L -n | grep 8080

# 3. 确认端口绑定地址
ss -tlnp | grep 8080
# 127.0.0.1:8080 → 仅本地可访问（需改成 0.0.0.0）
# 0.0.0.0:8080   → 所有网络接口可访问

# 4. 云服务器检查安全组规则
# 登录云控制台 → 安全组 → 入方向规则

# 5. 测试连通性
telnet localhost 8080
telnet <公网IP> 8080
```

### 8.2 连接超时 vs 连接拒绝

| 现象 | 原因 | 排查方向 |
|------|------|----------|
| `Connection refused` | 端口未监听 | Java 服务未启动或监听地址错误 |
| `Connection timed out` | 网络不通 | 防火墙/安全组拦截，路由不可达 |
| `No route to host` | 路由不可达 | 网络配置错误或主机离线 |

### 8.3 Spring Boot 安全端点配置

```yaml
# application.yml — 保护 Actuator 端点
management:
  server:
    port: 9090              # 单独监听端口
  endpoints:
    web:
      exposure:
        include: health,info  # 仅暴露必要端点
```

```bash
# 防火墙限制 Actuator 管理端口仅内网访问
firewall-cmd --permanent --add-rich-rule='
  rule family="ipv4"
  source address="10.0.0.0/8"
  port protocol="tcp" port="9090" accept'
```

---

## 9. 常见问题

### Q1：启动 Java 服务后无法访问端口，但 ss 显示已监听？

检查防火墙规则：`firewall-cmd --list-all` 或 `iptables -L -n`。也可能是云服务商安全组未放行。确认监听地址是否为 `0.0.0.0` 而非 `127.0.0.1`。

### Q2：SSH 连接慢（卡在 "Connection established"）？

开启 `UseDNS no` 和 `GSSAPIAuthentication no` 关闭反向 DNS 解析。修改 `/etc/ssh/sshd_config` 后 `systemctl reload sshd`。

### Q3：服务器疑似被挖矿，重启也没用？

挖矿木马通常植入了持久化机制（crontab、systemd 服务、rc.local）。断网后离线排查启动项，用 `busybox` 等静态编译工具检查被替换的系统命令。

### Q4：iptables 重启后规则丢失？

使用 `iptables-save > /etc/iptables/rules.v4` 保存，可配合 `systemd` 服务在启动时自动恢复。或安装 `iptables-persistent` 包自动保存。

### Q5：大量 `TIME_WAIT` 连接怎么处理？

```bash
# 查看 TIME_WAIT 数量
ss -ant | grep TIME_WAIT | wc -l

# 内核参数优化
sysctl -w net.ipv4.tcp_tw_reuse=1      # 复用 TIME_WAIT 连接
sysctl -w net.ipv4.tcp_fin_timeout=30  # 缩短 FIN_WAIT 时间
```

### Q6：`curl` 能通但浏览器不能访问？

检查是否被云服务商安全组拦截；检查是否使用了非标准端口（部分运营商封锁非 80/443 端口）；检查是否有 HTTP 重定向到了浏览器不可达的地址。
