# 04 - SSH 高阶技巧与排障

> **核心摘要**：Multiplexing 连接复用让多条 SSH 共享一条 TCP（秒连）、Agent Forwarding 的正确姿势、堡垒机实战、十大排障速查——SSH 高阶能力让日常操作「快」且「稳」。

> **前置阅读**：[[02-SSH-ssh-config配置艺术]]、[[03-SSH-安全加固]]

---

## 📚 目录

1. [Multiplexing 连接复用](#1-multiplexing-连接复用)
2. [Agent Forwarding 正确姿势](#2-agent-forwarding-正确姿势)
3. [堡垒机与跳板最佳实践](#3-堡垒机与跳板最佳实践)
4. [SCP/SFTP 高效传输](#4-scpsftp-高效传输)
5. [SSH 与自动化（CI 场景）](#5-ssh-与自动化ci-场景)
6. [十大排障速查](#6-十大排障速查)
7. [核心要点](#7-核心要点)

---

## 1. Multiplexing 连接复用

### 1.1 原理与价值

> **背景**：每次 `ssh` 都要完整握手（TCP + 密钥交换 + 认证）——毫秒级开销，但高频操作（git/rsync/批量命令）累积明显。
> **目的**：第一条连接建立后，后续连接复用同一条 TCP（秒连）。
> **适用范围**：高频 SSH 的日常开发；批量操作脚本。
> **不适用场景**：一次性/低频连接（复用管理开销反而多余）。

```text
Multiplexing 工作流
├── ① 第一条 SSH：建立连接（主连接 master）
├── ② 后续 SSH：检测到复用 socket → 直接复用（秒连）
├── ③ 主连接断开：复用连接随之断开
└── 配置：ControlMaster auto + ControlPath socket 路径
```

### 1.2 配置

```text
# ~/.ssh/config
Host *
    ControlMaster auto            # 自动复用（首个连接成为 master）
    ControlPath ~/.ssh/controlmux/%r@%h:%p   # socket 路径
    ControlPersist 10m            # 主连接保持 10 分钟（关终端后仍可复用）

# 创建 socket 目录
mkdir -p ~/.ssh/controlmux
```

```bash
# 效果验证
ssh server1      # 第一次（正常速度）
ssh server1      # 第二次（秒连！）
# rsync/scp/git 到同一主机全部复用

# 手动管理
ssh -O check server1      # 检查复用状态
ssh -O stop server1       # 主动断开复用连接
```

> ⚠️ **注意**：ControlPath 包含特殊字符（%r 用户/%h 主机/%p 端口）——socket 路径过长会报错（Unix socket 限长 108 字符），主机名长时缩短路径。

---

## 2. Agent Forwarding 正确姿势

### 2.1 什么是 Agent Forwarding

> **背景**：跳板机场景下，本机密钥如何让「内网服务器」使用？Agent Forwarding 把本机 ssh-agent 的认证能力「转发」给跳板机——跳板机上的 SSH 可以借用本机密钥认证。
> **目的**：免在跳板机/内网服务器存放私钥（私钥不出本机）。
> **适用范围**：多级跳板 + 需要逐级认证的场景。
> **不适用场景**：**不信任的中间机器**（风险极高，见下）。

```text
Agent Forwarding 流程
┌────────┐  ①ssh -A ┌────────┐  ②ssh ┌────────┐
│ 本机    │ ───────→ │ 跳板机  │ ───→ │ 内网服务器 │
│ agent   │          │ 借用本机│      │ 用本机密钥│
│ (私钥)  │ ←─────── │ 密钥认证│ ←─── │ 认证成功  │
└────────┘          └────────┘      └────────┘
私钥始终在本机 agent——跳板机只能「借用认证」，拿不到私钥内容
```

### 2.2 配置与使用

```bash
# 一次性：ssh -A（forward agent）
ssh -A bastion
# 然后在跳板机上：
ssh internal-server     # 使用本机密钥认证（无需跳板机上放密钥）

# 或 ssh_config 配置
Host bastion
    ForwardAgent yes

# 查看 agent 内容（在跳板机上）
ssh-add -l              # 看到本机密钥被转发（验证生效）
```

### 2.3 安全风险（必须了解）

> ⚠️ **Agent Forwarding 的著名风险**：中间机器被攻破 → 攻击者能**借用你的 agent 认证任何配置了你公钥的服务器**（无法读取私钥，但能使用认证能力）：

```text
风险场景
├── ① 跳板机被攻破 → 攻击者借用 agent → 登录你的其他服务器
├── ② 恶意管理员：故意配置 ForwardAgent 收集认证
├── ③ 转发链越长，风险越大（每跳都多一个借用点）

安全准则
├── ① 只在可信的中间机器上转发
├── ② 服务器端可全局禁用：AllowAgentForwarding no
├── ③ 替代方案：ProxyJump（-J）不需要 agent 转发！
│   → 2026 推荐：ProxyJump 优于 Agent Forwarding（少一层风险）
├── ④ 用完即关（ssh -O stop 或退出）
└── ⑤ 最安全：每跳显式配置密钥（02 篇方案 A）
```

> 🎯 **2026 结论**：**Agent Forwarding 是「便捷但危险」**——多级跳板优先用 ProxyJump + 逐跳密钥；Agent Forwarding 仅用于「无法逐跳配置密钥」的可信内网。

---

## 3. 堡垒机与跳板最佳实践

### 3.1 堡垒机架构

```text
堡垒机（跳板机）模式
┌────────┐        ┌────────┐        ┌────────────┐
│ 开发/运维 │ ────→ │ 堡垒机   │ ────→ │ 内网服务器群  │
│        │        │ 唯一入口 │        │ (无公网IP)  │
└────────┘        └────────┘        └────────────┘

设计原则
├── ① 内网服务器不暴露公网（只经堡垒机）
├── ② 堡垒机是唯一入口（集中审计/控制）
├── ③ 堡垒机最小化（不跑业务服务）
└── ④ 审计日志集中（谁通过堡垒机做了什么）
```

### 3.2 最佳实践配置

```text
# ~/.ssh/config 堡垒机模式
Host bastion
    HostName 203.0.113.20
    User jump
    IdentityFile ~/.ssh/jump_key
    ServerAliveInterval 60

Host 10.* 192.168.*        # 内网网段统一走堡垒机
    User deploy
    ProxyJump bastion
    IdentityFile ~/.ssh/deploy_key

# 使用：ssh 10.0.0.5 → 自动经堡垒机（本机密钥认证内网）
# 金句：ProxyJump 让「堡垒机」对使用者透明
```

### 3.3 堡垒机加固（结合 03 篇）

```text
堡垒机专项加固
├── ① 密钥认证 + 禁用密码（必做）
├── ② 来源限制：只允许公司 IP 访问堡垒机
├── ③ fail2ban 监控堡垒机爆破
├── ④ 审计日志：sshd LogLevel VERBOSE + 日志转存
├── ⑤ 最小权限：堡垒机账号无 sudo、无多余软件
├── ⑥ 可选：2FA（堡垒机是「钥匙串」，值得加锁）
└── ⑦ 会话记录：script/录屏审计（强合规要求时）
```

---

## 4. SCP/SFTP 高效传输

### 4.1 scp 常用

```bash
# 基础
scp file.txt user@server:/tmp/          # 上传
scp user@server:/tmp/file.txt .         # 下载
scp -r ./dir user@server:/tmp/          # 目录递归

# 经跳板机（ProxyJump 同样生效）
scp -J bastion file.txt user@10.0.0.5:/tmp/

# 限速/压缩（慢网络）
scp -C file.txt user@server:/tmp/       # 压缩
scp -l 1000 file.txt user@server:/tmp/  # 限速 1000 kbps
```

### 4.2 断点续传与增量（rsync）

```bash
# rsync 增量传输（大文件/目录首选——支持断点续传）
rsync -avz --partial --progress ./data/ user@server:/data/
# -a 归档 -v 详细 -z 压缩
# --partial 保留部分传输（断线续传）
# --progress 进度显示

# 经跳板机
rsync -avz -e "ssh -J bastion" ./data/ user@10.0.0.5:/data/
```

### 4.3 传输性能对比

| 工具 | 场景 | 特性 |
|------|------|------|
| **scp** | 小文件/单次 | 简单直接 |
| **rsync** | 大目录/增量 | 断点续传/增量/权限保留 |
| **sftp** | 交互式管理 | 文件管理操作 |
| **rsync + zstd** | 超大传输 | 压缩效率高（慢网） |

---

## 5. SSH 与自动化（CI 场景）

### 5.1 CI 部署的 SSH 实践

```text
CI 中 SSH 的安全实践
├── ① 专用部署密钥（不是个人密钥！）
├── ② 密钥存 CI 平台的 Secret（GitHub Actions Secrets）
├── ③ authorized_keys 限制：command= 限制可执行命令（01 篇）
├── ④ StrictHostKeyChecking=accept-new（首次连接自动信任）
└── ⑤ 用完即删：临时密钥轮换

GitHub Actions 示例：
  - name: 部署
    env:
      DEPLOY_KEY: ${{ secrets.DEPLOY_KEY }}
    run: |
      echo "$DEPLOY_KEY" > /tmp/deploy_key
      chmod 600 /tmp/deploy_key
      ssh -i /tmp/deploy_key -o StrictHostKeyChecking=accept-new \
          deploy@server 'bash /opt/deploy.sh'
```

### 5.2 批量命令（多机运维）

```bash
# 批量执行（配合 ssh_config 通配）
for host in 192.168.1.10 192.168.1.11 192.168.1.12; do
    ssh $host 'uptime && df -h /' &
done
wait

# 更专业的工具（大规模）
# pssh / ansible（批量运维标准工具，SSH 是其传输层）
```

---

## 6. 十大排障速查

| # | 症状 | 根因 | 排查/解法 |
|---|------|------|---------|
| 1 | **Permission denied (publickey)** | 密钥未被接受 | `ssh -v` 看认证过程；确认公钥在 authorized_keys、权限 600/700 |
| 2 | **Connection refused** | 端口不对/sshd 未运行 | `ss -tlnp \| grep ssh`；`systemctl status sshd` |
| 3 | **Connection timed out** | 防火墙拦截 | 防火墙放行 + 云安全组；`nc -vz host 22` 测试 |
| 4 | **Host key verification failed** | known_hosts 指纹变化 | `ssh-keygen -R host` 删除后重连（确认不是 MITM） |
| 5 | **Too many authentication failures** | 尝试密钥过多 | 加 `IdentitiesOnly yes` 指定密钥 |
| 6 | **Permission denied (password)** | 密码错误/未启用密码 | 检查 sshd_config PasswordAuthentication；是否被 AllowUsers 排除 |
| 7 | **connection closed by remote host** | 服务器强制断开 | 检查 MaxSessions/MaxStartups；fail2ban 是否封禁你 |
| 8 | **ssh: connect to host port 22: No route** | 网络不可达 | 云安全组/防火墙；`ping` + `traceroute` |
| 9 | **Bad owner or permissions** | 本地权限问题 | `chmod 700 ~/.ssh && chmod 600 ~/.ssh/*` |
| 10 | **Server refused our key** | 服务器拒绝密钥 | 公钥格式错误/authorized_keys 权限/用户不对 |

### 6.1 排障三板斧

```bash
# ① 详细日志（最有效）
ssh -vvv user@server
# 看输出定位：连接建立 → 认证过程 → 失败原因

# ② 服务器侧日志
tail -f /var/log/auth.log        # 服务器视角（谁尝试了什么）

# ③ 连通性分层
nc -vz server 22                  # 端口可达？
ssh -o BatchMode=yes server       # 无交互测试（密钥认证是否成功）
```

---

## 7. 核心要点

> 🎯 **核心要点**：
> 1. Multiplexing（ControlMaster auto）让高频 SSH 秒连——高频操作必备
> 2. **Agent Forwarding 是「便捷但危险」**：2026 优先 ProxyJump + 逐跳密钥；转发仅限可信中间机
> 3. 堡垒机模式：内网不暴露公网 + ProxyJump 透明穿透 + 堡垒机专项加固（密钥/限源/fail2ban/审计）
> 4. 传输选型：小文件 scp、大目录 rsync（断点续传 + 增量）
> 5. CI 自动化：专用部署密钥 + Secret 存储 + authorized_keys command 限制
> 6. 排障三板斧：ssh -vvv（客户端日志）+ auth.log（服务器日志）+ nc 分层测试

---

**返回总览**：[00-SSH知识体系总览](00-SSH知识体系总览.md)
