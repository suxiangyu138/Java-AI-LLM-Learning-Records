# SSH 安全加固

> 🔒 sshd_config 核心配置、禁用密码登录、修改端口、fail2ban 防暴力破解、双因素认证 —— 让你的服务器不被脚本小子攻破

---

## 📚 目录

1. [sshd_config 核心配置](#1-sshd_config-核心配置)
2. [fail2ban 防暴力破解](#2-fail2ban-防暴力破解)
3. [高级安全措施](#3-高级安全措施)

---

## 1. sshd_config 核心配置

```bash
# /etc/ssh/sshd_config
sudo vi /etc/ssh/sshd_config
sudo systemctl reload sshd    # 重载配置（不断现有连接！）
# ⚠️ 改配置前保持一个 SSH 连接开着以防锁死！
```

### 关键配置项

```ini
# ===== 端口 =====
Port 2222                      # 改掉默认 22（减少 90% 自动扫描）

# ===== 禁止 root 直接登录 =====
PermitRootLogin no             # 必须！用普通用户+sudo

# ===== 仅密钥登录，禁用密码 =====
PasswordAuthentication no      # 核心！彻底杜绝暴力破解
ChallengeResponseAuthentication no
PubkeyAuthentication yes

# ===== 用户白名单 =====
AllowUsers deploy devops       # 仅允许这些用户 SSH 登录

# ===== 协议与算法 =====
Protocol 2                     # 仅 SSHv2
HostKeyAlgorithms ssh-ed25519,rsa-sha2-512  # 弱算法排除

# ===== 连接限制 =====
MaxAuthTries 3                 # 最多尝试 3 次
MaxSessions 10                 # 单连接最多 10 个会话
MaxStartups 10:30:100          # 未认证连接限制
LoginGraceTime 30              # 30 秒内完成登录

# ===== 其他 =====
X11Forwarding no               # 用不到就关
AllowTcpForwarding yes         # 开发环境保留（SSH 隧道需要）
ClientAliveInterval 60         # 心跳：60 秒无操作发 keepalive
ClientAliveCountMax 3          # 3 次心跳失败断开
```

### 配置后验证

```bash
# 检查配置语法
sudo sshd -t

# 重载（不断现有连接）
sudo systemctl reload sshd

# 另外开一个终端测试能否登录！
ssh -p 2222 user@server
```

---

## 2. fail2ban 防暴力破解

```bash
# 安装
sudo apt install fail2ban    # Ubuntu
sudo dnf install fail2ban    # CentOS

sudo systemctl enable --now fail2ban
```

```ini
# /etc/fail2ban/jail.local（不要改 jail.conf）
[sshd]
enabled = true
port = 2222                   # 改成你的 SSH 端口
maxretry = 3                  # 失败 3 次封禁
bantime = 3600                # 封 1 小时
findtime = 600                # 10 分钟内

# 针对多次被封的 IP 永久禁止
bantime.increment = true
bantime.factor = 2            # 每次翻倍
bantime.maxtime = 604800      # 最长封 1 周
```

```bash
# fail2ban 管理
sudo fail2ban-client status sshd     # 查看 SSH 监狱状态
sudo fail2ban-client set sshd unbanip 1.2.3.4  # 解封 IP
sudo fail2ban-client status           # 所有监狱

# 查看封禁日志
sudo zgrep "Ban" /var/log/fail2ban.log*
```

---

## 3. 高级安全措施

```bash
# ===== 双因素认证（TOTP）=====
sudo apt install libpam-google-authenticator
google-authenticator            # 生成二维码，扫码绑定

# /etc/pam.d/sshd 中添加一行
auth required pam_google_authenticator.so

# /etc/ssh/sshd_config
ChallengeResponseAuthentication yes
AuthenticationMethods publickey,keyboard-interactive
# 先验证密钥，再验证 TOTP

# ===== 仅允许特定 IP 段 =====
# sshd_config 中没有 IP 限制，用 firewalld/iptables
sudo firewall-cmd --add-rich-rule='rule family="ipv4" source address="10.0.0.0/16" service name="ssh" accept' --permanent
```

### 快速加固 Checklist

```text
生产服务器 SSH 加固清单：
  □ PermitRootLogin no
  □ PasswordAuthentication no（仅密钥）
  □ Port 非 22
  □ AllowUsers 白名单
  □ fail2ban 安装启用
  □ MaxAuthTries 3
  □ 改完配置另开终端验证！
```

---

> 🎯 **最低底线**：关闭密码登录 + 关闭 root 直接登录。这两个配置能防御 99% 的自动攻击。

---

*创建于：2026年7月*
