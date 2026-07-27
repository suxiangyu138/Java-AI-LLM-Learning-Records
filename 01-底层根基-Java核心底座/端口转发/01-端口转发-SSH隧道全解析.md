# SSH 隧道全解析

> 🔐 ssh -L 本地转发、-R 远程转发、-D 动态转发(SOCKS5)、autossh 断线重连、多级跳转 —— 90% 的端口转发需求 SSH 就够了

---

## 📚 目录

1. [SSH 隧道原理](#1-ssh-隧道原理)
2. [本地转发 -L](#2-本地转发--l)
3. [远程转发 -R](#3-远程转发--r)
4. [动态转发 -D (SOCKS5)](#4-动态转发--d-socks5)
5. [高级技巧](#5-高级技巧)

---

## 1. SSH 隧道原理

```text
SSH 隧道 = 在 SSH 加密连接之上承载其他 TCP 流量

三种模式：

  -L（本地转发）
    你 → 远程：把"远程的服务"搬到"本地端口"
    ssh -L 本地端口:目标主机:目标端口 跳板机

  -R（远程转发）
    远程 → 你：把"本地的服务"搬到"远程端口"
    ssh -R 远程端口:目标主机:目标端口 跳板机

  -D（动态转发）
    SOCKS5 代理：浏览器/系统流量走 SSH 隧道
    ssh -D 本地端口 跳板机

关键参数：
  -N  不执行远程命令（纯隧道）
  -f  后台运行
  -C  压缩传输
  -v  详细输出（调试用）
```

### 1.1 如何记忆

```text
-L = Local → 本地监听，连到远程目标
      "我把远程的东西搬到本地"
      例：本地 3306 → 跳板机 → 远程 MySQL 3306

-R = Remote → 远程监听，连到本地目标
      "我把本地的东西搬到远程"
      例：远程 5005 → 跳板机 → 本地 IDE 5005

-D = Dynamic → 不指定目标，应用程序动态决定
      "SOCKS5 代理，流量全走隧道"
```

---

## 2. 本地转发 -L

### 2.1 最常用场景

```bash
# 语法：ssh -L [本地IP:]本地端口:目标主机:目标端口 跳板机

# 场景1：堡垒机后连数据库
ssh -N -L 3306:prod-db.internal:3306 user@bastion.example.com
# 然后本地连接：mysql -h 127.0.0.1 -P 3306 -u root -p

# 场景2：堡垒机后连 Redis
ssh -N -L 6379:prod-redis.internal:6379 user@bastion

# 场景3：同时转发多个端口
ssh -N \
  -L 3306:prod-db.internal:3306 \
  -L 6379:prod-redis.internal:6379 \
  -L 9200:prod-es.internal:9200 \
  user@bastion

# 场景4：转发到非跳板机可达的目标（跳板机中转）
ssh -N -L 8080:staging-api.internal:8080 user@bastion
# 架构：本地 → bastion → staging-api:8080
```

### 2.2 后台运行

```bash
# 方式1：-f 后台 + -N 不执行命令
ssh -fN -L 3306:db.internal:3306 user@bastion

# 方式2：autossh 断线自动重连（推荐！）
autossh -M 0 -fN \
  -o "ServerAliveInterval=30" \
  -o "ServerAliveCountMax=3" \
  -L 3306:db.internal:3306 \
  user@bastion
# -M 0：禁用 autossh 自带的监控端口（用 ServerAlive 替代）
```

### 2.3 允许外部访问

```bash
# 默认只监听 127.0.0.1（仅本地访问）
# 加上 bind_address 让局域网也能访问
ssh -N -L 0.0.0.0:8080:app.internal:8080 user@bastion
# 或指定网卡 IP：
ssh -N -L 192.168.1.100:8080:app.internal:8080 user@bastion

# 前提：sshd 配置需允许 GatewayPorts
# /etc/ssh/sshd_config: GatewayPorts yes
```

---

## 3. 远程转发 -R

### 3.1 使用场景

```text
场景：本地有服务需要暴露给远程服务器访问

典型用例：
  1. 本地 IDE Debug 端口开放给远程 JVM
  2. 本地 Web 服务给远程同事临时预览
  3. 内网服务开放给公网 VPS 中转
```

```bash
# 场景1：远程调试 Java 应用
# 远程服务器上的 JVM 已配置：
# -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
# 但是 5005 端口被防火墙封锁

# 在远程服务器上执行：
ssh -N -R 5005:localhost:5005 user@local-dev-machine
# 本地 IDE 连接 localhost:5005 即可调试！

# 场景2：本地 Web 服务展示给远程
ssh -N -R 8080:localhost:3000 user@vps.example.com
# vps:8080 → 本地:3000
# 此时访问 http://vps:8080 就是本地的开发环境
# 前提：vps 的 /etc/ssh/sshd_config 中 GatewayPorts yes
```

---

## 4. 动态转发 -D (SOCKS5)

```bash
# 启动 SOCKS5 代理
ssh -N -D 1080 user@vps.example.com

# 系统代理设置 → SOCKS5 → 127.0.0.1:1080
# 所有流量走 VPS 出口

# 浏览器设置 SwitchyOmega 插件
# 终端代理（临时）：
export ALL_PROXY=socks5://127.0.0.1:1080
curl https://api.example.com   # 自动走代理

# 恢复：
unset ALL_PROXY
```

---

## 5. 高级技巧

### 5.1 多级跳转

```bash
# 本地 → Jump1 → Jump2 → Target
ssh -N -L 3306:target-db.internal:3306 \
  -J user1@jump1.example.com,user2@jump2.example.com \
  user2@jump2.example.com

# 或配置 ~/.ssh/config 简化
Host target-tunnel
    HostName jump2.internal
    User user2
    ProxyJump user1@jump1.example.com
    LocalForward 3306 target-db.internal:3306
    ServerAliveInterval 30
# 然后只需：
ssh -N target-tunnel
```

### 5.2 查看与管理隧道

```bash
# 查看当前 SSH 连接
ps aux | grep ssh | grep -v grep

# 查找占用某端口的 SSH 隧道
lsof -i :3306 | grep ssh

# 关闭隧道
kill PID
# 或 pkill -f "ssh.*-L 3306"
```

### 5.3 持久化推荐配置

```bash
# 创建 systemd 服务保持隧道常驻
# /etc/systemd/system/ssh-tunnel-db.service
[Unit]
Description=SSH Tunnel to DB
After=network.target

[Service]
Type=simple
User=deploy
ExecStart=/usr/bin/autossh -M 0 -N \
  -o "ServerAliveInterval=30" \
  -o "ServerAliveCountMax=3" \
  -o "ExitOnForwardFailure=yes" \
  -L 3306:prod-db.internal:3306 \
  user@bastion.example.com
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

---

> 🎯 **核心口诀**：**-L 把远的搬来**（连数据库）、**-R 把近的送出**（远程调试）、**-D 做代理**（SOCKS5）。加 `autossh` 防断线，配 `systemd` 常驻。

---

*创建于：2026年7月*
