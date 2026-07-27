# SSH 密钥管理与认证

> 🔑 Ed25519/RSA 密钥选型、生成部署、ssh-agent 免密中转、authorized_keys 高级控制、known_hosts 管理

---

## 📚 目录

1. [密钥类型选型](#1-密钥类型选型)
2. [密钥生成与部署](#2-密钥生成与部署)
3. [ssh-agent 密钥中转](#3-ssh-agent-密钥中转)
4. [known_hosts 管理](#4-known_hosts-管理)

---

## 1. 密钥类型选型

| 算法 | 安全性 | 性能 | 推荐 |
|------|:----:|:--:|:--:|
| **Ed25519** | 极高 | 极快 | ⭐ 首选 |
| **ECDSA** | 高 | 快 | ⭐ 备选 |
| **RSA 4096** | 中（量子威胁） | 慢 | 兼容老旧系统 |
| **DSA** | 已破解 | - | ❌ 禁用 |

```bash
# 推荐 Ed25519
ssh-keygen -t ed25519 -C "your@email.com"
# 老系统兼容 RSA 4096
ssh-keygen -t rsa -b 4096 -C "your@email.com"
```

---

## 2. 密钥生成与部署

```bash
# 生成（可指定文件名）
ssh-keygen -t ed25519 -f ~/.ssh/id_ed25519_custom -C "comment"
chmod 600 ~/.ssh/id_ed25519_custom    # 私钥必须 600！

# 部署公钥
ssh-copy-id -i ~/.ssh/id_ed25519.pub user@server
# 手动：
cat ~/.ssh/id_ed25519.pub | ssh user@server \
  "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"

# 测试免密
ssh user@server
```

### authorized_keys 高级控制

```bash
# 限制某公钥仅能执行特定命令
command="/opt/backup.sh",no-port-forwarding,no-pty ssh-ed25519 AAA...

# 限制来源 IP
from="10.0.0.5" ssh-ed25519 AAA...

# 组合（堡垒机备份账户）
from="10.0.0.5",command="/opt/backup.sh",no-pty ssh-ed25519 AAA...
```

---

## 3. ssh-agent 密钥中转

```bash
# 启动 agent（登录后运行一次）
eval "$(ssh-agent -s)"

# 添加密钥（输入一次密码）
ssh-add ~/.ssh/id_ed25519
ssh-add -l          # 列出已加载密钥
ssh-add -D          # 清空

# macOS Keychain 持久化
ssh-add --apple-use-keychain ~/.ssh/id_ed25519
```

```text
Agent Forwarding 风险：
  ssh -A bastion → bastion 上 root 可冒用你的 agent
  替代方案：ProxyJump 转发连接而非 agent
  ssh -J user@bastion user@target（更安全）
```

---

## 4. known_hosts 管理

```bash
# 遇到 "REMOTE HOST IDENTIFICATION HAS CHANGED" → 删旧指纹
ssh-keygen -R server.example.com
ssh-keygen -R 192.168.1.100

# 跳过验证（仅测试！）
ssh -o StrictHostKeyChecking=no user@server
```

---

> 🎯 Ed25519 首选，ssh-agent 省密码，known_hosts 报错就 ssh-keygen -R。

---

*创建于：2026年7月*
