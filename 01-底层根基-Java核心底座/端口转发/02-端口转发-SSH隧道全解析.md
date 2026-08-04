# 02 - 端口转发：SSH 隧道全解析

> **核心摘要**：SSH 隧道是端口转发的最常用武器——加密、认证、穿透防火墙三合一。90% 的临时转发需求 `ssh -L/-R/-D` 就够了。本文覆盖三种转发模式的原理与实战、参数全解、autossh 断线重连、多级跳转与故障排查。

> **前置阅读**：[[01-端口转发基础与核心原理]]

---

## 📚 目录

1. [SSH 隧道的定位与边界](#1-ssh-隧道的定位与边界)
2. [本地转发 -L：把远程搬到本地](#2-本地转发--l把远程搬到本地)
3. [远程转发 -R：把本地暴露到远程](#3-远程转发--r把本地暴露到远程)
4. [动态转发 -D：SOCKS5 代理](#4-动态转发--dsocks5-代理)
5. [参数全解](#5-参数全解)
6. [autossh：断线重连](#6-autossh断线重连)
7. [多级跳转](#7-多级跳转)
8. [故障排查](#8-故障排查)
9. [核心要点](#9-核心要点)

---

## 1. SSH 隧道的定位与边界

> **背景**：SSH 协议本身是加密远程登录——但其端口转发能力让它成为「加密流量搬运工」。
> **目的**：在 SSH 加密连接之上承载任意 TCP 流量，实现加密转发 + 穿透防火墙。
> **适用范围**：临时访问、加密通道、防火墙穿透、调试场景。
> **不适用场景**：UDP 转发（SSH 仅 TCP！）、高频大流量生产转发（SSH 加解密开销）、长期无人值守（需 autossh + 密钥）。

```text
SSH 隧道三大优势
├── ① 加密：所有流量走 SSH 加密通道（防嗅探）
├── ② 认证：复用 SSH 认证（密钥对更安全）
├── ③ 穿透：只需 22 端口可达（很多防火墙只开 22）

三大限制
├── ① 仅 TCP（UDP 需其他工具）
├── ② 性能开销（加解密）
└── ③ 依赖 SSH 连接稳定（断线隧道即断——用 autossh）
```

---

## 2. 本地转发 -L：把远程搬到本地

### 2.1 原理

```text
本地转发（-L）：本地端口 → 跳板机 → 目标服务
┌────────┐  SSH隧道   ┌────────┐  内网   ┌────────┐
│ 本机     │ ←———————→ │ 跳板机  │ —————→ │ 目标服务 │
│ :3306   │           │ (SSH)  │        │ db:3306 │
└────────┘           └────────┘        └────────┘
命令：ssh -L 本地端口:目标主机:目标端口 跳板机
```

### 2.2 实战：本地连远程数据库（最经典场景）

```bash
# 场景：本机 Navicat/IDEA 要连内网数据库 db:3306（经跳板机 bastion）
ssh -L 3306:db:3306 bastion

# 然后本地连接 localhost:3306 即可（等价直连 db:3306）
mysql -h 127.0.0.1 -P 3306 -u root -p

# 关键参数组合（纯隧道）
ssh -N -f -L 3306:db:3306 bastion
# -N 不执行远程命令（纯转发）
# -f 后台运行

# 绑定其他端口（避免本地 3306 被占用）
ssh -L 13306:db:3306 bastion
mysql -h 127.0.0.1 -P 13306 ...
```

### 2.3 变体：任意目标主机

```text
-L 的目标主机是「跳板机视角」的地址
├── ssh -L 8080:localhost:80 bastion
│   → 目标 localhost 是「跳板机自己」的 80（不是本机的！）
├── ssh -L 8080:web-server:80 bastion
│   → 跳板机可达的内网 web-server:80
└── ⚠️ 易错：以为 localhost 是本机——实际是跳板机的 localhost
```

---

## 3. 远程转发 -R：把本地暴露到远程

### 3.1 原理

```text
远程转发（-R）：远程端口 → 隧道 → 本地服务
┌────────┐  内网   ┌────────┐  SSH隧道  ┌────────┐
│ 本机服务 │ ←————→ │ 跳板机  │ ←——————— │ 远程用户 │
│ :8080   │        │ :5005  │          │        │
└────────┘        └────────┘          └────────┘
命令：ssh -R 远程端口:目标主机:目标端口 跳板机
```

### 3.2 实战：远程调试本地服务（经典场景）

```bash
# 场景：让远程服务器能访问本机 IDEA 调试端口 5005
ssh -R 5005:localhost:5005 dev-server

# 远程服务器上：访问 localhost:5005 = 访问本机 5005
# 本机 IDEA 开启 Remote JVM Debug（监听 5005）

# 另一个场景：远程访问本地 Web 服务（给同事看本地页面）
ssh -R 8080:localhost:8080 bastion
# 同事访问 bastion:8080 = 访问你本机 8080
```

### 3.3 远程转发的安全注意

> ⚠️ **远程转发 = 把本地服务暴露到远端**——安全边界要清醒：

```text
远程转发的风险与防护
├── ⚠️ 风险：任何人能访问跳板机端口 = 能访问你本机服务
├── 防护 1：sshd 配置限制
│   GatewayPorts no              # 只绑定 127.0.0.1（默认）
│   AllowTcpForwarding yes
├── 防护 2：绑定指定端口而非 0.0.0.0
├── 防护 3：用完即关（临时隧道不常驻）
└── 防护 4：目标端口选不常用高位端口
```

---

## 4. 动态转发 -D：SOCKS5 代理

### 4.1 原理

```text
动态转发（-D）：本机 SOCKS5 代理 → 隧道 → 任意目标
┌────────┐  SOCKS5   ┌────────┐  任意目标
│ 浏览器   │ ←———————→ │ 跳板机  │ → 任意站点
│ 系统流量 │  :1080   │        │
└────────┘           └────────┘
命令：ssh -D 1080 跳板机
```

### 4.2 使用（全局代理场景）

```bash
# 建立 SOCKS5 代理（本机 1080 端口）
ssh -N -D 1080 proxy-server

# 浏览器配置（Firefox 为例）
# 设置 → 网络设置 → 手动代理 → SOCKS 主机 127.0.0.1 端口 1080

# 命令行工具走代理
curl --socks5 127.0.0.1:1080 https://example.com
git config --global http.proxy socks5://127.0.0.1:1080

# 系统全局代理（部分系统支持）
export ALL_PROXY=socks5://127.0.0.1:1080
```

> 💡 **-D 的优势**：一个代理端口 = 访问任意目标（无需为每个目标建隧道）——浏览器/命令行/系统流量都可走。

---

## 5. 参数全解

| 参数 | 作用 | 示例 |
|:---:|------|------|
| `-L` | 本地转发 | `-L 3306:db:3306` |
| `-R` | 远程转发 | `-R 5005:localhost:5005` |
| `-D` | 动态转发（SOCKS5） | `-D 1080` |
| `-N` | 不执行远程命令（纯隧道） | `-N -L ...` |
| `-f` | 后台运行 | `-f -N -L ...` |
| `-C` | 压缩传输（慢网络有用） | `-C -L ...` |
| `-v` | 详细输出（调试） | `-v -L ...` |
| `-p` | 指定 SSH 端口（非 22） | `-p 2222` |
| `-i` | 指定密钥文件 | `-i ~/.ssh/id_ed25519` |
| `-o` | 传递 sshd 配置选项 | `-o ServerAliveInterval=60` |
| `-T` | 禁用伪终端（纯隧道配 -N） | `-N -T -L ...` |
| `-J` | 跳板机（ProxyJump，多级） | `-J jump1,jump2` |

```bash
# 生产环境推荐的完整组合（密钥 + 保活 + 后台）
ssh -N -f -T \
    -i ~/.ssh/id_ed25519 \
    -o ServerAliveInterval=30 \
    -o ServerAliveCountMax=3 \
    -o ExitOnForwardFailure=yes \
    -L 3306:db:3306 \
    bastion
# ServerAliveInterval：每 30s 发保活包（检测断线）
# ExitOnForwardFailure：端口绑定失败立即退出（不静默失败）
```

---

## 6. autossh：断线重连

### 6.1 为什么需要 autossh

> ⚠️ **SSH 隧道依赖连接稳定性**——网络抖动/服务器重启 → 隧道断开 → 服务不可用。autossh 自动监控并重建隧道。

### 6.2 使用

```bash
# 安装
sudo apt install autossh    # Debian/Ubuntu
sudo yum install autossh    # RHEL/CentOS

# 基本用法（用 autossh 替代 ssh）
autossh -M 0 -N -L 3306:db:3306 bastion
# -M 0：禁用监控端口（新版推荐，用 ServerAlive 检测）

# 推荐组合（带保活）
autossh -M 0 -N -T \
    -o ServerAliveInterval=30 \
    -o ServerAliveCountMax=3 \
    -o ExitOnForwardFailure=yes \
    -L 3306:db:3306 \
    bastion
```

### 6.3 systemd 托管（长期隧道）

```ini
# /etc/systemd/system/db-tunnel.service
[Unit]
Description=DB SSH Tunnel
After=network-online.target

[Service]
User=tunnel
ExecStart=/usr/bin/autossh -M 0 -N -T \
    -o ServerAliveInterval=30 \
    -o ServerAliveCountMax=3 \
    -i /home/tunnel/.ssh/id_ed25519 \
    -L 3306:db:3306 bastion
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# 启动管理
sudo systemctl daemon-reload
sudo systemctl enable --now db-tunnel
sudo systemctl status db-tunnel
```

---

## 7. 多级跳转

### 7.1 场景与三种实现

```text
多级跳转场景：本机 → 跳板A → 跳板B → 目标（三层网络）
├── 方案 1：ProxyJump（-J，最简）
├── 方案 2：ProxyCommand（ssh_config 配置）
└── 方案 3：逐级隧道（手动串联）
```

```bash
# 方案 1：-J ProxyJump（OpenSSH 7.3+，推荐）
ssh -N -L 3306:db:3306 -J jump1,jump2 bastion2
# 本机 → jump1 → jump2 → bastion2 → db

# 方案 2：~/.ssh/config 配置（复用）
Host final
    HostName bastion2
    ProxyJump jump1,jump2
    User root
# 然后：ssh -N -L 3306:db:3306 final

# 方案 3：逐级（老版本/特殊场景）
ssh -N -L 13306:bastion2:3306 jump1   # 第一跳
ssh -N -L 3306:db:3306 bastion2       # 第二跳（在本地分别执行）
```

### 7.2 多级跳转的注意事项

```text
多级跳转注意
├── ① -J 是「连接路径」不是「转发路径」——转发目标仍写最终目标
├── ② 每一跳都需要认证（密钥 agent 转发或逐跳配置）
├── ③ 层级越深延迟越高（每跳加一跳网络）
└── ④ 调试：-v 逐步查看连接过程（-vvv 最详细）
```

---

## 8. 故障排查

### 8.1 常见问题定位表

| 症状 | 可能原因 | 排查 |
|------|---------|------|
| **连不上跳板机** | SSH 端口/认证问题 | `ssh -v bastion` 看认证过程 |
| **端口绑定失败** | 本地端口被占用 | `ss -tlnp \| grep 3306`；换端口 |
| **隧道建立但访问不通** | 目标主机名解析错误 | 目标用「跳板机视角」地址 |
| **连接频繁断** | 网络不稳定 | ServerAliveInterval + autossh |
| **远程转发不生效** | GatewayPorts 限制 | sshd_config 检查绑定范围 |
| **后台后找不到进程** | -f 无日志 | 用 `pgrep -af ssh` 找；-v 前台先测 |
| **UDP 不转发** | SSH 不支持 UDP | 换 socat/iptables/Nginx stream |
| **退出后隧道还占端口** | 僵尸进程 | `pkill -f "ssh -N -L"` |

### 8.2 排查三板斧

```bash
# ① 前台 + 详细日志（先别用 -f 后台）
ssh -vv -N -L 3306:db:3306 bastion
# 看输出：连接建立/转发请求/目标连接过程

# ② 验证本地端口在监听
ss -tlnp | grep 3306        # 确认隧道端口已监听

# ③ 验证目标可达（在跳板机上）
ssh bastion "nc -vz db 3306"   # 跳板机视角测试目标
# nc -vz host port：端口连通性测试
```

### 8.3 测试验证

```bash
# 隧道建立后验证转发生效
telnet 127.0.0.1 3306            # TCP 连通测试
mysql -h 127.0.0.1 -P 3306 -u root -p   # 应用级验证
curl http://127.0.0.1:8080       # HTTP 验证
```

---

## 9. 核心要点

> 🎯 **核心要点**：
> 1. SSH 隧道三模式：-L（访问远程）/ -R（暴露本地）/ -D（SOCKS5 代理）——90% 临时转发够用
> 2. 三大限制：**仅 TCP**（UDP 用其他工具）、加解密开销、断线即断
> 3. -L 的目标是「跳板机视角」的地址（localhost = 跳板机自己）——最高频误解
> 4. 生产组合：-N -f -T + ServerAliveInterval + ExitOnForwardFailure + 密钥认证；长期用 autossh + systemd
> 5. 多级跳转用 -J（ProxyJump）；远程转发注意 GatewayPorts 暴露边界

---

**下一模块**：[03-端口转发-系统级端口转发](03-端口转发-系统级端口转发.md) | **返回总览**：[00-端口转发知识体系总览](00-端口转发知识体系总览.md)
