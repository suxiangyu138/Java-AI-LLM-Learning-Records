# SSH 高阶技巧与排障

> 🔧 Multiplexing 连接复用、ControlMaster 加速、Agent Forwarding 正确姿势、堡垒机实战、十大常见错误排查

---

## 📚 目录

1. [Multiplexing 连接复用](#1-multiplexing-连接复用)
2. [堡垒机与跳板最佳实践](#2-堡垒机与跳板最佳实践)
3. [十大排障速查](#3-十大排障速查)

---

## 1. Multiplexing 连接复用

```bash
# ~/.ssh/config
Host *
    ControlMaster auto
    ControlPath ~/.ssh/control/%C
    ControlPersist 10m
# 效果：第一次连接后，后续 SSH/SCP/SFTP 秒开！
```

```text
原理：
  第一次 ssh → 建立 TCP 连接（正常耗时）
  第二次 ssh → 复用已有连接 → 瞬间完成！
  适合：频繁 scp 文件、多个终端连同一服务器
  不适合：需要不同用户/端口 → 自动新连

效果测试：
  time ssh server exit
  # 首次: 0.5s → 之后: 0.05s（10x 提速）
```

```bash
# 手动管理
ssh -O check server       # 检查 Master 连接状态
ssh -O exit server        # 关闭 Master 连接
ssh -O stop server        # 等所有 Slave 退出后关闭
```

---

## 2. 堡垒机与跳板最佳实践

```bash
# ~/.ssh/config 生产级配置
Host bastion
    HostName bastion.example.com
    User admin
    IdentityFile ~/.ssh/bastion_key
    ServerAliveInterval 30
    ControlMaster auto
    ControlPath ~/.ssh/control/%C
    ControlPersist 60m

# 数据库隧道（通过堡垒机）
Host prod-db-tunnel
    HostName prod-db.internal
    User dbuser
    ProxyJump bastion
    LocalForward 3306 localhost:3306
    IdentityFile ~/.ssh/db_key

# 一键启动所有隧道
Host prod-all
    HostName bastion
    ProxyJump none             # 不跳转
    LocalForward 3306 prod-db.internal:3306
    LocalForward 6379 prod-redis.internal:6379
    LocalForward 9200 prod-es.internal:9200
# ssh -N prod-all → 三条隧道同时建立
```

### SCP 通过跳板机

```bash
# 配置了 ProxyJump 后，scp 自动走跳板
scp file.txt prod-app:/opt/app/

# 如果没配：
scp -o ProxyJump=bastion file.txt target:/path/
```

---

## 3. 十大排障速查

| 问题 | 原因 | 解决 |
|------|------|------|
| **Permission denied (publickey)** | 密钥未部署/权限不对 | `chmod 600 ~/.ssh/*`；检查 authorized_keys |
| **REMOTE HOST IDENTIFICATION CHANGED** | 服务器重装 | `ssh-keygen -R host` |
| **Connection refused** | sshd 没跑/端口错 | `systemctl status sshd`；`ss -tlnp \| grep 22` |
| **Connection timed out** | 网络不通/防火墙 | `telnet host 22`；检查 firewalld/安全组 |
| **Too many authentication failures** | 尝试过多密钥 | `ssh -o IdentitiesOnly=yes -i key user@host` |
| **Permission denied (publickey,password)** | 都有问题 | `ssh -vvv` 看详细日志 |
| **破旧密钥被拒绝** | ssh-rsa 被新版 OpenSSH 禁用 | 换 Ed25519 |
| **agent refused operation** | ssh-agent 没跑 | `eval "$(ssh-agent -s)" && ssh-add` |
| **Write failed: Broken pipe** | 网络断/心跳太短 | 配置 ServerAliveInterval 30 |
| **Host key verification failed** | known_hosts 中密钥不匹配 | `ssh-keygen -R host` 后重连 |

### 调试三板斧

```bash
# 1. 看服务器日志
sudo tail -f /var/log/auth.log    # Ubuntu
sudo tail -f /var/log/secure      # CentOS

# 2. 客户端加 -v 详细输出
ssh -v user@host     # 基本信息
ssh -vv user@host    # 更多信息
ssh -vvv user@host   # 全部细节（调试专用）

# 3. 检查权限
ls -la ~/.ssh/
# drwx------  .ssh/          → 700
# -rw-------  id_ed25519     → 600（私钥）
# -rw-r--r--  id_ed25519.pub → 644
# -rw-------  authorized_keys → 600
```

---

> 🎯 **Multiplexing** 让 SSH 秒开，**ProxyJump+Config** 让堡垒机透明化，**-vvv** 是排障万能钥匙。

---

**返回总览**：[00-SSH知识体系总览](./00-SSH知识体系总览.md) | SSH 隧道 → [端口转发](../端口转发/01-端口转发-SSH隧道全解析.md)

---

*创建于：2026年7月*
