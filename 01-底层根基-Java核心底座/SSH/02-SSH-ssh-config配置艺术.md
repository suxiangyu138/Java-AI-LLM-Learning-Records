# ssh_config 配置艺术

> ⚙️ ~/.ssh/config 完整语法、Host 别名、ProxyJump 跳板、Include 拆分 —— 把复杂命令变成简单别名

---

## 📚 目录

1. [基础语法与常用关键字](#1-基础语法与常用关键字)
2. [Host 别名与通配符](#2-host-别名与通配符)
3. [ProxyJump 跳板机配置](#3-proxyjump-跳板机配置)
4. [Include 拆分管理](#4-include-拆分管理)

---

## 1. 基础语法与常用关键字

```bash
chmod 600 ~/.ssh/config   # 权限必须是 600！
```

| 关键字 | 说明 | 常用值 |
|--------|------|--------|
| `HostName` | 实际 IP/域名 | `192.168.1.100` |
| `User` | 登录用户名 | `deploy` |
| `Port` | 端口 | `2222` |
| `IdentityFile` | 私钥路径 | `~/.ssh/id_ed25519` |
| `ServerAliveInterval` | 心跳秒数 | `30`（防断） |
| `ServerAliveCountMax` | 心跳失败上限 | `3` |
| `ConnectTimeout` | 连接超时 | `10` |
| `Compression` | 压缩传输 | `yes` |
| `ForwardAgent` | Agent 转发 | `no`（安全） |

---

## 2. Host 别名与通配符

```bash
# 最简别名
Host prod
    HostName 10.0.1.50
    User deploy
    Port 2222
    IdentityFile ~/.ssh/prod_key
# ssh prod → 等同完整命令

# 通配符
Host *.internal
    User ubuntu
    IdentityFile ~/.ssh/id_ed25519
    ServerAliveInterval 30

Host prod-*
    User deploy
    IdentityFile ~/.ssh/prod_key

# 全局默认
Host *
    ServerAliveInterval 30
    ConnectTimeout 10
    ForwardAgent no
```

---

## 3. ProxyJump 跳板机配置

```bash
# 目标通过 bastion 跳转
Host target
    HostName 10.0.1.100
    User deploy
    ProxyJump bastion

Host bastion
    HostName bastion.example.com
    User admin

# 多级跳转
Host deep-target
    HostName 10.0.2.50
    ProxyJump bastion,internal-jump
```

---

## 4. Include 拆分管理

```bash
# ~/.ssh/config 主文件
Include ~/.ssh/config.d/*.conf

# 目录结构：
# ~/.ssh/config.d/00-global.conf
# ~/.ssh/config.d/10-dev.conf
# ~/.ssh/config.d/20-staging.conf
# ~/.ssh/config.d/30-prod.conf
```

```bash
# 20-staging.conf
Host staging-api
    HostName 10.0.10.10
    User staging
Host staging-db
    HostName staging-db.internal
    LocalForward 3307 localhost:3306
```

---

> 🎯 **Host 别名**替代长命令，**ProxyJump** 省去手动跳板，**Include** 防止配置文件膨胀。

---

*创建于：2026年7月*
