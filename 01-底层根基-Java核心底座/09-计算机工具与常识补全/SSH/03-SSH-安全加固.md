# 03 - SSH 安全加固

> **核心摘要**：公网服务器的 SSH 端口每秒都在被扫描爆破——安全加固不是可选项而是生存基线。本文覆盖 sshd_config 核心配置、禁用密码登录、fail2ban 防爆破、双因素认证与加固自检清单。

> **前置阅读**：[[01-SSH-密钥管理与认证]]

---

## 📚 目录

1. [SSH 威胁模型](#1-ssh-威胁模型)
2. [sshd_config 核心配置](#2-sshd_config-核心配置)
3. [禁用密码登录](#3-禁用密码登录)
4. [修改端口与来源限制](#4-修改端口与来源限制)
5. [fail2ban 防暴力破解](#5-fail2ban-防暴力破解)
6. [双因素认证（2FA）](#6-双因素认证2fa)
7. [安全加固自检清单](#7-安全加固自检清单)
8. [核心要点](#8-核心要点)

---

## 1. SSH 威胁模型

> **背景**：公网 SSH 端口（22）是扫描器与爆破脚本的「第一靶场」——配置弱密码的服务器平均几分钟就会被尝试入侵。
> **目的**：理解攻击路径，用分层防御把风险压到可接受水平。
> **适用范围**：所有暴露公网的 Linux 服务器。
> **前提假设**：**默认不安全**——出厂配置只保证可用，不保证安全。

```text
SSH 攻击路径
├── ① 端口扫描：找到开放的 22（Shodan 全球索引）
├── ② 口令爆破：字典尝试 root/弱密码（每秒数百次）
├── ③ 漏洞利用：旧版本 OpenSSH 已知 CVE
├── ④ 中间人：无 StrictHostKeyChecking 时劫持
└── ⑤ 横向移动：攻破一台 → 密钥/凭据收集 → 内网扩散

分层防御思路（每一层拦截一部分攻击）
├── 第 1 层：认证层——密钥认证（爆破无效）
├── 第 2 层：暴露层——改端口/限来源（扫描命中率下降）
├── 第 3 层：行为层——fail2ban（爆破即封禁）
├── 第 4 层：增强层——2FA（密钥泄露仍可拦截）
└── 第 5 层：监测层——日志/告警（发现异常）
```

---

## 2. sshd_config 核心配置

### 2.1 配置文件定位

> **背景**：`/etc/ssh/sshd_config` 是 SSH 服务端配置——**修改后必须重启 sshd 服务才生效**。
> **目的**：控制认证方式、权限、端口、会话行为。
> **适用范围**：所有 Linux SSH 服务器。
> **不适用场景**：客户端配置在 `~/.ssh/config`（02 篇）——别混。

```bash
# 修改后生效
sudo sshd -t                    # 语法检查（必做！防止改错锁死）
sudo systemctl restart sshd     # 重启生效
# ⚠️ 安全操作：先开新终端测试能登录，再重启（防配置错误锁死自己）
```

### 2.2 核心配置基线（2026）

```text
# /etc/ssh/sshd_config 安全基线
# ═══════════ 认证 ═══════════
PasswordAuthentication no        # 禁用密码登录（核心！）
PubkeyAuthentication yes         # 启用密钥认证
PermitRootLogin prohibit-password  # root 仅密钥登录（不用 no——留应急通道）
# PermitRootLogin no             # 完全禁 root（更严，但可能影响运维）

# ═══════════ 端口（可选修改）═══════════
Port 2222                        # 改高位端口（降低扫描命中）
# ⚠️ 改端口后：防火墙放行新端口 + 客户端 -p 2222

# ═══════════ 权限控制 ═══════════
AllowUsers deploy,ops            # 只允许指定用户登录（白名单）
AllowGroups ssh-users            # 或按组白名单
MaxAuthTries 3                   # 认证尝试次数（防爆破）
LoginGraceTime 30                # 认证超时（防占连接）

# ═══════════ 会话与转发 ═══════════
X11Forwarding no                 # 禁用 X11 转发
AllowTcpForwarding yes           # TCP 转发（需要时保留）
AllowAgentForwarding yes         # Agent 转发（需要时保留，见 04 篇）
PermitUserEnvironment no         # 禁用用户环境文件（防注入）
ClientAliveInterval 300          # 会话保活
ClientAliveCountMax 2            # 保活失败次数

# ═══════════ 协议与日志 ═══════════
Protocol 2                       # 仅 SSHv2（v1 已废弃）
LogLevel VERBOSE                 # 详细日志（审计）
```

> ⚠️ **改配置前先备份**：`cp /etc/ssh/sshd_config /etc/ssh/sshd_config.bak`——配置错误导致无法登录是最高频事故，备份 + `sshd -t` 校验 + 新终端先测是三重保险。

---

## 3. 禁用密码登录

### 3.1 为什么必须禁用

```text
密码认证的致命弱点
├── ① 可爆破：字典 + 暴力（每秒数百次尝试）
├── ② 弱密码普遍：root/123456/admin 是扫描器的第一字典
├── ③ 无锁定机制（默认）：尝试次数无上限（需 fail2ban 补）
├── ④ 日志噪音：爆破失败记录刷屏
└── ⑤ 统计事实：启用密码的服务器，几乎必然被爆破尝试

密钥认证的不可爆破性
├── ① 无密码可猜（公钥加密挑战-应答）
├── ② 私钥 256 位强度（暴力不可能）
└── ③ 前提：私钥未泄露（管理好私钥即可）
```

### 3.2 禁用流程（安全顺序）

```text
禁用密码的正确顺序（防止锁死自己）
├── ① 生成密钥并部署（ssh-copy-id，见 01 篇）
├── ② 新终端验证密钥登录成功
├── ③ 配置 PasswordAuthentication no
├── ④ sshd -t 语法校验
├── ⑤ 重启 sshd
├── ⑥ 再开新终端验证（密钥仍可登录）
└── ⑦ 保持当前会话（直到确认无误）
→ 金句：先确认新锁能用，再拆旧锁
```

---

## 4. 修改端口与来源限制

### 4.1 修改端口（降低扫描命中）

```text
改端口的收益与代价
├── 收益：扫描器默认扫 22——改端口后扫描命中率大幅下降
├── 代价：客户端都要 -p 指定（ssh_config 配置可解）
├── ⚠️ 注意：不是安全手段（端口扫描全端口也有），是「降噪」
└── 2026 建议：配合防火墙限源更有效（见下）

操作：
① sshd_config：Port 2222
② 防火墙放行：ufw allow 2222/tcp（firewalld 同理）
③ 客户端：ssh -p 2222 或 ssh_config 配置 Port
```

### 4.2 来源限制（最有效的手段之一）

```bash
# ufw 只允许公司 IP 访问 SSH
sudo ufw allow from 203.0.113.0/24 to any port 2222 proto tcp

# firewalld 富规则
sudo firewall-cmd --permanent --add-rich-rule='rule family="ipv4" \
    source address="203.0.113.0/24" port port="2222" protocol="tcp" accept'

# iptables
sudo iptables -A INPUT -p tcp --dport 2222 -s 203.0.113.0/24 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 2222 -j DROP   # 其余拒绝

# 云安全组（云服务器第一道防线）
# 入方向：2222/tcp 来源 你的IP/网段（而非 0.0.0.0/0）
```

> 🎯 **效果对比**：改端口（降噪）< 限来源（拒绝）< 两者结合（推荐）——**固定办公 IP 的场景，限源几乎消灭 SSH 攻击面**。

---

## 5. fail2ban 防暴力破解

### 5.1 原理

> **背景**：fail2ban 监控日志中的失败尝试——超过阈值自动封禁来源 IP。
> **目的**：把「暴力破解」变成「自我封禁」——攻击者试几次就被踢出。
> **适用范围**：SSH 爆破防护（也支持 Web/Nginx 等）。
> **前提假设**：有系统日志（journald/rsyslog）供监控。

```text
fail2ban 工作流
├── ① 监控 /var/log/auth.log（SSH 失败记录）
├── ② 同一 IP 失败 N 次（如 5 次）
├── ③ 封禁该 IP（iptables/firewalld 规则）
├── ④ 封禁时长（如 1 小时）
└── ⑤ 解封后再次尝试再次封禁（持续对抗）
```

### 5.2 安装与配置

```bash
# 安装
sudo apt install fail2ban    # Debian/Ubuntu
sudo yum install fail2ban    # RHEL/CentOS

# 配置（/etc/fail2ban/jail.local——覆盖默认）
sudo tee /etc/fail2ban/jail.local <<'EOF'
[sshd]
enabled = true
maxretry = 5              # 5 次失败触发
bantime = 3600            # 封禁 1 小时（秒）
findtime = 600            # 10 分钟内统计
ignoreip = 127.0.0.1/8 203.0.113.0/24   # 白名单（公司 IP）
EOF

# 启动
sudo systemctl enable --now fail2ban

# 查看状态
sudo fail2ban-client status sshd        # 封禁列表
sudo fail2ban-client set sshd unbanip 1.2.3.4   # 手动解封
```

### 5.3 误封与边界

```text
fail2ban 的边界
├── ⚠️ 误封：自己输错密码被自己的规则封了
│   → 白名单 ignoreip 配置公司 IP；或手动解封
├── ⚠️ 分布式攻击：攻击者换 IP 绕过单 IP 封禁
│   → 配合限源（第 4 节）更有效
├── ⚠️ 封禁是 iptables 层（不是应用层）——对已建立的连接无效
└── ✅ 组合：密钥认证（杜绝成功）+ fail2ban（降低噪音）
```

---

## 6. 双因素认证（2FA）

### 6.1 为什么需要 2FA

```text
密钥认证的残余风险
├── ① 私钥泄露（电脑被黑/备份泄露）
├── ② 私钥被复制（同事/恶意软件）
└── → 2FA：即使密钥泄露，还需要第二个因素（TOTP）

2FA 的场景判断
├── 个人服务器：密钥认证 + 强密码短语 通常足够
├── 团队/生产环境：建议 2FA（密钥 + TOTP）
└── 合规环境（金融/政务）：强制 2FA
```

### 6.2 TOTP 2FA 配置（Google Authenticator）

```bash
# 安装（Debian/Ubuntu）
sudo apt install libpam-google-authenticator

# 为每个用户生成 TOTP 密钥（交互式）
google-authenticator
# 输出二维码 + 密钥（手机 Authenticator 扫码）

# 配置 PAM（/etc/pam.d/sshd 开头加）
auth required pam_google_authenticator.so

# sshd_config
ChallengeResponseAuthentication yes
# 重启 sshd 生效
```

```text
登录流程（2FA 后）
├── ① 密钥认证（第一因素）
├── ② 提示输入验证码（第二因素）
├── ③ 手机 Authenticator 取码输入
└── 完成登录
```

> ⚠️ **2FA 的运维成本**：每个用户都要扫码绑定、密钥丢失需管理员重置——**团队环境需配套流程**（绑定指引/重置流程）。2026 实践：生产核心服务器 2FA，普通开发服务器密钥认证即可。

---

## 7. 安全加固自检清单

```text
SSH 加固总检查（上线前过一遍）
[ ] 密码登录已禁用？（PasswordAuthentication no）
[ ] 密钥认证已部署？（ssh-copy-id 完成）
[ ] root 登录受限？（prohibit-password 或 no）
[ ] 用户白名单配置？（AllowUsers）
[ ] 端口已修改/来源已限制？（或至少 fail2ban）
[ ] fail2ban 运行中？（fail2ban-client status）
[ ] 密钥有密码短语？（私钥加密）
[ ] 私钥权限 600？（ls -l ~/.ssh/）
[ ] 旧版本 OpenSSH？（sshd -V 检查 CVE）
[ ] 日志监控？（auth.log 定期查看/告警）
[ ] 无多余密钥在 authorized_keys？（审计）

加固顺序（按优先级）：
① 禁用密码（最重要）
② 密钥管理（密码短语 + 权限）
③ 来源限制/fail2ban
④ root 限制 + 用户白名单
⑤ 2FA（增强）
⑥ 日志告警（监测）
```

---

## 8. 核心要点

> 🎯 **核心要点**：
> 1. SSH 威胁模型：扫描 → 爆破 → 利用 → 横向——**分层防御，每层拦截一部分**
> 2. 核心基线：**PasswordAuthentication no + 密钥认证 + root 受限 + AllowUsers 白名单**
> 3. 改配置安全流程：备份 → sshd -t 校验 → 新终端先测 → 重启——**先确认新锁能用再拆旧锁**
> 4. 防爆破组合：限来源（最有效）+ fail2ban（自动封禁）+ 改端口（降噪）
> 5. 2FA 是增强层：生产核心服务器建议（密钥 + TOTP）；注意运维成本
> 6. 加固顺序：禁用密码 > 密钥管理 > 限源/fail2ban > root 限制 > 2FA > 日志

---

**下一模块**：[04-SSH-高阶技巧与排障](04-SSH-高阶技巧与排障.md) | **返回总览**：[00-SSH知识体系总览](00-SSH知识体系总览.md)
