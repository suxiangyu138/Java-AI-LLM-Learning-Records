# Linux 网络与安全

## 网络配置

### 网卡与 IP 管理

| 命令 | 用途 | 示例 |
|------|------|------|
| `ip addr` | 查看 IP 地址与网卡状态（替代 ifconfig） | `ip addr show eth0` |
| `ip link` | 查看/启停网卡 | `ip link set eth0 up` |
| `ss` | 查看 socket 统计（替代 netstat） | `ss -tlnp` |
| `nmcli` | NetworkManager 命令行工具 | `nmcli dev status` |

### 配置文件路径

- **CentOS/RHEL 7+**: `/etc/sysconfig/network-scripts/ifcfg-eth0`
- **Ubuntu 18.04+**: `/etc/netplan/*.yaml`
- **通用 DNS**: `/etc/resolv.conf`

```
# 静态 IP 配置示例 (CentOS)
TYPE=Ethernet
BOOTPROTO=static
IPADDR=192.168.1.100
NETMASK=255.255.255.0
GATEWAY=192.168.1.1
DNS1=8.8.8.8
ONBOOT=yes
```

### 路由与 DNS

```bash
# 查看路由表
ip route
# 临时路由
ip route add 10.0.0.0/8 via 192.168.1.1 dev eth0
# DNS 解析测试
nslookup api.example.com
dig +short api.example.com
# 测试端口连通性 (Java 服务排查常用)
telnet 192.168.1.200 3306
nc -zv 192.168.1.200 3306
```

### Java 后端常用网络排查

```bash
# 确认服务端口是否监听
ss -tlnp | grep 8080
# 查看进程打开的端口
lsof -i :8080
# 抓包分析 (排查慢请求/连接异常)
tcpdump -i eth0 port 8080 -w capture.pcap
# TCP 连接统计
ss -s
```

---

## 防火墙与网络安全

### firewalld (CentOS 7+ / RHEL 8+)

```bash
# 状态与基本操作
systemctl status firewalld
firewall-cmd --state
firewall-cmd --list-all

# 端口放行 (Java 服务常用)
firewall-cmd --permanent --add-port=8080/tcp
firewall-cmd --permanent --add-port=8443/tcp
firewall-cmd --reload

# 来源 IP 白名单
firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address="192.168.1.0/24" port protocol="tcp" port="8080" accept'
firewall-cmd --reload

# 区域管理
firewall-cmd --get-active-zones
firewall-cmd --zone=public --add-source=10.0.0.0/8
```

### iptables (通用)

```bash
# 查看规则 (-n 不反解, -v 详细)
iptables -L -n -v

# 默认策略: 丢弃入站，允许出站
iptables -P INPUT DROP
iptables -P FORWARD DROP
iptables -P OUTPUT ACCEPT

# 放行回环与已建立连接
iptables -A INPUT -i lo -j ACCEPT
iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# 放行特定端口
iptables -A INPUT -p tcp --dport 22 -j ACCEPT
iptables -A INPUT -p tcp --dport 80 -j ACCEPT
iptables -A INPUT -p tcp --dport 443 -j ACCEPT

# 保存规则
service iptables save          # CentOS 6
iptables-save > /etc/iptables.rules  # 通用
```

### nftables (新发行版默认)

```bash
# 查看规则集
nft list ruleset
# 清空规则
nft flush ruleset
```

### 安全加固实践

| 措施 | 命令/配置 |
|------|-----------|
| 禁用 root SSH 登录 | `/etc/ssh/sshd_config` 中 `PermitRootLogin no` |
| 修改默认 SSH 端口 | `/etc/ssh/sshd_config` 中 `Port 2222` |
| 限制 IP 连接数 | `iptables -A INPUT -p tcp --dport 80 -m connlimit --connlimit-above 100 -j DROP` |
| 防 SYN Flood | `iptables -A INPUT -p tcp --syn -m limit --limit 10/s -j ACCEPT` |
| 端口敲门 (knockd) | 动态打开 SSH 端口，避免暴力扫描 |

---

## 远程登录 (SSH)

### 基本连接

```bash
# 密码登录
ssh -p 22 root@192.168.1.100

# 密钥登录
ssh -i ~/.ssh/id_rsa -p 2222 deploy@api.example.com
```

### 密钥对管理

```bash
# 生成 RSA 密钥对 (建议 ed25519)
ssh-keygen -t ed25519 -C "deploy@example.com"
# 指定文件路径
ssh-keygen -t rsa -b 4096 -f ~/.ssh/deploy_key

# 复制公钥到服务器
ssh-copy-id -i ~/.ssh/id_ed25519.pub user@server
# 手动追加
cat ~/.ssh/id_ed25519.pub | ssh user@server "cat >> ~/.ssh/authorized_keys"
```

### 安全文件传输

```bash
# SCP
scp -P 2222 app.jar deploy@server:/opt/app/
scp -r config/ deploy@server:/opt/app/config/

# SFTP (交互式)
sftp -P 2222 deploy@server
```

### SSH 隧道 (端口转发)

```bash
# 本地转发: 将本地 3306 转发到远程 MySQL
ssh -L 3306:127.0.0.1:3306 jump-server
# 远程转发: 将内网服务暴露到公网跳板机
ssh -R 8080:127.0.0.1:8080 jump-server

# SSH 配置文件 ~/.ssh/config
Host prod
    HostName 192.168.1.100
    Port 2222
    User deploy
    IdentityFile ~/.ssh/deploy_key
    LocalForward 3306 127.0.0.1:3306
```

### sshd 安全配置

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| `Port` | 非 22 | 减小暴力扫描风险 |
| `PermitRootLogin` | no | 禁止 root 直接登录 |
| `PasswordAuthentication` | no | 禁用密码登录，仅密钥 |
| `PubkeyAuthentication` | yes | 启用密钥认证 |
| `AllowUsers` | deploy app | 仅允许指定用户登录 |
| `MaxAuthTries` | 3 | 最大认证尝试次数 |
| `ClientAliveInterval` | 300 | 空闲断开时间(秒) |

---

## 病毒木马防御

### 常见入侵类型

| 类型 | 特征 | Java 后端场景 |
|------|------|---------------|
| 挖矿木马 | CPU 持续 100%, 外连矿池 | 常伪装成 java/nginx 进程 |
| 反弹 Shell | 异常外连连接 | 利用漏洞植入后门 |
| SSH 爆破 | /var/log/secure 大量失败 | 弱密码/默认端口 |
| WebShell | 网页木马文件 | Tomcat 上传漏洞 |
| Rootkit | 隐藏进程/文件 | 内核级后门难以检测 |

### 应急排查步骤

```bash
# 1. 查看异常进程
top -c
ps aux --sort=-%cpu | head -10

# 2. 检查网络连接
ss -antp | grep -E "ESTAB|SYN"
lsof -i

# 3. 查看登录记录
last -10
lastb -10                          # 失败登录
cat /var/log/secure | grep "Failed"

# 4. 检查开机自启
systemctl list-unit-files | grep enabled
cat /etc/rc.local
ls -la /etc/init.d/

# 5. 检查定时任务
crontab -l
cat /etc/crontab
ls -la /etc/cron.*

# 6. 检查隐藏文件/可疑脚本
find / -name ".*" -type f | xargs ls -la
find /tmp -type f -executable
```

### fail2ban 配置

```bash
# 安装
yum install fail2ban -y

# /etc/fail2ban/jail.local
[DEFAULT]
bantime = 3600
findtime = 600
maxretry = 5

[sshd]
enabled = true
port = 2222
logpath = /var/log/secure
maxretry = 3

# 启动
systemctl enable fail2ban --now
fail2ban-client status sshd
```

### SELinux 基础

```bash
# 查看状态
getenforce                     # Enforcing / Permissive / Disabled
sestatus

# 临时切换
setenforce 0                   # Permissive (调试用)
setenforce 1                   # Enforcing

# 修改配置文件 (/etc/selinux/config)
# SELINUX=enforcing

# 查看/修改文件上下文
ls -Z /opt/app.jar
chcon -t httpd_sys_content_t /opt/app.jar

# 查看 Booleans
getsebool -a | grep httpd
setsebool -P httpd_can_network_connect on
```

---

## 常见问题

**Q: 启动 Java 服务后无法访问端口，但 netstat 显示已监听？**

A: 检查防火墙规则。执行 `iptables -L -n` 或 `firewall-cmd --list-all` 确认端口未被拦截。也可能是云服务商安全组未放行。

**Q: SSH 连接慢（卡在 "Connection established"）？**

A: 开启 `UseDNS no` 和 `GSSAPIAuthentication no` 关闭反向 DNS 解析。

**Q: 如何快速定位哪个进程占用了端口？**

A: `lsof -i :8080` 或 `ss -tlnp | grep 8080`。

**Q: 服务器疑似被挖矿，重启也没用？**

A: 检查 crontab、systemd 自启服务、rc.local。挖矿木马通常植入了持久化机制，重启不删除。断网后离线排查，用 `busybox` 等静态编译工具检查被替换的系统命令。

**Q: iptables 重启后规则丢失？**

A: 使用 `iptables-save > /etc/iptables/rules.v4` 保存，并在 `/etc/rc.local` 中添加 `iptables-restore < /etc/iptables/rules.v4`。

**Q: Spring Boot Actuator 暴露了 /health /info 等端点，如何保护？**

A: 配置 `management.server.port=9090` 单独监听端口，防火墙限制该端口仅内网或跳板机 IP 访问。
