# 01 - SSH 密钥管理与认证

> **核心摘要**：密钥认证是 SSH 的安全基石——私钥是身份、公钥是锁。本文覆盖密钥类型选型（Ed25519 vs RSA）、生成部署全流程、ssh-agent 免密中转、authorized_keys 高级控制与 known_hosts 管理。

> **前置阅读**：[[00-SSH知识体系总览]]

---

## 📚 目录

1. [认证方式对比](#1-认证方式对比)
2. [密钥类型选型](#2-密钥类型选型)
3. [密钥生成与部署](#3-密钥生成与部署)
4. [私钥的安全管理](#4-私钥的安全管理)
5. [ssh-agent：免密中转](#5-ssh-agent免密中转)
6. [authorized_keys 高级控制](#6-authorized_keys-高级控制)
7. [known_hosts 管理](#7-known_hosts-管理)
8. [核心要点](#8-核心要点)

---

## 1. 认证方式对比

> **背景**：SSH 支持多种认证方式——密码、密钥、键盘交互等。认证方式选择直接决定服务器的安全基线。
> **目的**：理解各方式的优劣，选择并组合正确的认证策略。
> **适用范围**：所有 SSH 服务器与客户端。
> **不适用场景**：一次性临时环境（密码足够，无需密钥部署）。

| 方式 | 安全性 | 便利性 | 适用 |
|------|:---:|:---:|------|
| **密码认证** | 低（可爆破） | 中 | 临时环境 |
| **密钥认证** | **高（不可爆破）** | 高（配合 agent） | **生产标准** |
| **密钥 + 密码短语** | 极高 | 中（每次输短语） | 高安全个人机 |
| **密钥 + 2FA** | 极高 | 中 | 生产加固（见 03 篇） |
| **证书认证** | 极高 | 高 | 大规模企业 |

> 🎯 **2026 基线**：**生产服务器只允许密钥登录**（`PasswordAuthentication no`）——密码爆破是公网服务器第一攻击向量。

---

## 2. 密钥类型选型

### 2.1 类型对比

| 类型 | 强度 | 速度 | 长度 | 2026 建议 |
|------|:---:|:---:|:---:|:---:|
| **Ed25519** | 高 | 快 | 短（68 字节公钥） | ✅ **首选** |
| **RSA 4096** | 高 | 中 | 长 | 兼容旧系统 |
| **RSA 2048** | 中 | 中 | 中 | ❌ 不推荐 |
| **ECDSA** | 高 | 快 | 短 | ⚠️ 有争议（随机数问题） |
| **DSA** | 低 | - | - | ❌ 已废弃 |

### 2.2 为什么 Ed25519 是 2026 首选

```text
Ed25519 的优势
├── ① 安全强度高（Curve25519 椭圆曲线）
├── ② 签名快、密钥短（公钥 68 字节 vs RSA 4096 的 800+ 字节）
├── ③ 无随机数陷阱（ECDSA 的历史问题）
├── ④ 现代系统全部支持（OpenSSH 6.5+，2014 起）
└── ⑤ 唯一需要注意：超老系统（CentOS 6 等）不支持

何时用 RSA 4096
├── 需要兼容远古系统
├── 企业内部旧平台强制
└── 其他场景一律 Ed25519
```

---

## 3. 密钥生成与部署

### 3.1 生成（2026 标准）

```bash
# 生成 Ed25519 密钥（推荐）
ssh-keygen -t ed25519 -C "user@example.com" -f ~/.ssh/id_ed25519

# 交互提示（安全设置）
# > Enter passphrase:  ← 建议设置密码短语（见 4.1）
# 生成两个文件：
#   ~/.ssh/id_ed25519      （私钥——绝不出本机！）
#   ~/.ssh/id_ed25519.pub  （公钥——可以分发）

# 如需 RSA（兼容场景）
ssh-keygen -t rsa -b 4096 -C "user@example.com" -f ~/.ssh/id_rsa
```

### 3.2 部署公钥到服务器

```bash
# 方式 1：ssh-copy-id（推荐——自动处理权限）
ssh-copy-id -i ~/.ssh/id_ed25519.pub user@server

# 方式 2：手动（理解原理）
cat ~/.ssh/id_ed25519.pub | ssh user@server \
    "mkdir -p ~/.ssh && chmod 700 ~/.ssh && \
     cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"

# 方式 3：托管平台（GitHub/GitLab）
# 设置 → SSH Keys → 粘贴公钥内容

# 验证
ssh user@server    # 应直接登录（无需密码）
```

### 3.3 权限要求（易错点）

```text
SSH 文件权限（过宽 = 拒绝认证）
├── ~/.ssh/          目录权限 700
├── ~/.ssh/authorized_keys  600
├── 私钥文件 id_ed25519     600
├── ⚠️ 权限过宽：OpenSSH 拒绝使用（"Permissions too open"）
├── ⚠️ 修复：chmod 700 ~/.ssh && chmod 600 ~/.ssh/authorized_keys
└── ⚠️ 常见场景：git clone 到非 ~/.ssh 路径时权限问题
```

---

## 4. 私钥的安全管理

### 4.1 密码短语（passphrase）

> ⚠️ **私钥是「身份」**——泄露私钥 = 攻击者获得所有配置了对应公钥的服务器访问权：

```text
密码短语的作用
├── ① 私钥文件加密存储（无短语 = 明文私钥）
├── ② 即使文件泄露，无短语也打不开
├── ③ 配合 ssh-agent 只需输一次（见第 5 节）
└── 2026 建议：个人机必设短语；CI 密钥用无短语 + 权限隔离

修改/移除密码短语
ssh-keygen -p -f ~/.ssh/id_ed25519    # 交互修改
```

### 4.2 私钥泄露处置

```text
私钥泄露应急（SOP）
├── ① 立即从所有服务器 authorized_keys 删除对应公钥
├── ② 撤销托管平台（GitHub）上的密钥
├── ③ 重新生成新密钥对并部署
├── ④ 检查泄露期间的异常登录（auth.log）
└── 金句：私钥泄露 = 身份泄露——先撤锁（删公钥）再换锁（新密钥）
```

---

## 5. ssh-agent：免密中转

### 5.1 作用

> **背景**：私钥设了密码短语后每次 SSH 都要输入——ssh-agent 把私钥加载进内存，会话内免密。
> **目的**：一次解锁，多次使用（安全与便利兼得）。
> **适用范围**：个人开发机；跳板机场景（配合 Agent Forwarding，见 04 篇）。

```bash
# 启动 agent + 添加密钥
eval "$(ssh-agent -s)"                    # 启动（写入环境变量）
ssh-add ~/.ssh/id_ed25519                  # 添加密钥（输入一次短语）
ssh-add -l                                 # 查看已加载的密钥

# 会话内免密
ssh user@server                            # 不再提示输入短语

# 系统自动加载（macOS Keychain / Windows 服务）
# macOS: ssh-add --apple-use-keychain ~/.ssh/id_ed25519
# Linux: 用 keychain 工具或桌面会话自动启动
```

### 5.2 常见问题

```text
ssh-agent 常见问题
├── ⚠️ 新终端窗口 agent 丢失：agent 是进程级——eval 需在会话内
│   → Linux 用 keychain / systemd user 服务保持
├── ⚠️ 服务器上找不到 agent：agent 在本机——服务器上用 Agent Forwarding
├── ⚠️ 密钥没加载：ssh-add 报错检查权限与路径
└── ✅ 排查：echo $SSH_AUTH_SOCK 查看 agent socket
```

---

## 6. authorized_keys 高级控制

### 6.1 基础格式

```text
# ~/.ssh/authorized_keys 每行一个公钥
ssh-ed25519 AAAAC3NzaC1lZDI1... user@example.com
#            ↑公钥内容            ↑注释（可选）

# 每行可加前缀选项（以逗号分隔）
command="...",no-port-forwarding ssh-ed25519 AAAA...
```

### 6.2 高级选项（企业控制利器）

```bash
# ① 限制登录命令（只能执行指定命令）
command="/usr/local/bin/backup.sh",no-pty ssh-ed25519 AAAA...

# ② 禁止端口转发（默认允许！）
no-port-forwarding,no-agent-forwarding ssh-ed25519 AAAA...

# ③ 来源限制（只允许从指定 IP 登录）
from="203.0.113.10,192.168.1.0/24" ssh-ed25519 AAAA...

# ④ 只读/限制操作（配合命令限制）
command="/usr/bin/git-shell",no-port-forwarding ssh-ed25519 AAAA...
# Git 服务器常用：只允许 git 操作

# 组合示例（CI 专用密钥——只能部署脚本）
command="/usr/local/bin/deploy.sh",no-pty,no-port-forwarding \
    ssh-ed25519 AAAA... ci-deploy@example.com
```

> 🎯 **企业实践**：不同用途用不同密钥 + authorized_keys 选项限制——**CI 密钥只能跑部署脚本、备份密钥只能执行备份**——最小权限原则的 SSH 落地。

---

## 7. known_hosts 管理

### 7.1 作用

> **背景**：known_hosts 记录访问过的主机公钥指纹——用于**防中间人攻击**（连接前验证对方是「认识的服务器」）。
> **目的**：首次连接信任后，后续连接校验指纹一致才放行。
> **适用范围**：所有 SSH 客户端。

```text
known_hosts 机制
├── ① 首次连接：提示指纹确认（"Are you sure?"）
│   → 确认后写入 ~/.ssh/known_hosts
├── ② 后续连接：比对指纹——不一致则拒绝（警告！）
├── ③ 指纹不一致 = 服务器重装/中间人攻击（需人工确认）
└── ④ 文件位置：~/.ssh/known_hosts（用户）/ /etc/ssh/ssh_known_hosts（全局）
```

### 7.2 常见操作

```bash
# 查看主机指纹
ssh-keygen -F server.example.com        # 查询 known_hosts 中的记录
ssh-keyscan server.example.com          # 获取服务器当前指纹

# 删除失效记录（服务器重装后常见）
ssh-keygen -R server.example.com        # 删除该主机记录
# 然后重新连接确认新指纹

# 跳过校验（⚠️ 仅临时/内网调试！生产禁止）
ssh -o StrictHostKeyChecking=no user@server
# 风险：不校验 = 易受中间人攻击（MITM）

# 自动接受新主机（首次连接不提示——CI/脚本场景）
ssh -o StrictHostKeyChecking=accept-new user@server
```

> ⚠️ **安全提醒**：`StrictHostKeyChecking=no` 会关闭指纹校验——**生产/敏感环境禁止**；CI 场景用 `accept-new`（只自动接受新主机，不放过指纹变更）。

---

## 8. 核心要点

> 🎯 **核心要点**：
> 1. 2026 基线：**Ed25519 密钥 + 生产禁用密码登录**——密码爆破是第一攻击向量
> 2. 私钥是身份：设密码短语 + 600 权限 + 绝不出本机；泄露 SOP = 先撤锁再换锁
> 3. ssh-agent 免密中转：一次解锁会话免密；Agent Forwarding 有风险（04 篇详解）
> 4. authorized_keys 高级选项实现最小权限：CI 密钥限命令、备份密钥限来源
> 5. known_hosts 防中间人：指纹不一致必须人工确认；`StrictHostKeyChecking=no` 生产禁止

---

**下一模块**：[02-SSH-ssh-config配置艺术](02-SSH-ssh-config配置艺术.md) | **返回总览**：[00-SSH知识体系总览](00-SSH知识体系总览.md)
