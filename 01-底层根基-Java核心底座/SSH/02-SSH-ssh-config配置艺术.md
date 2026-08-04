# 02 - ssh_config 配置艺术

> **核心摘要**：`~/.ssh/config` 把「又长又复杂的 ssh 命令」变成「一行别名」——Host 别名、通配符、ProxyJump 跳板、Include 拆分是四大能力。配置好的 ssh_config 是 SSH 效率的倍增器。

> **前置阅读**：[[01-SSH-密钥管理与认证]]

---

## 📚 目录

1. [ssh_config 的定位](#1-ssh_config-的定位)
2. [基础语法与常用关键字](#2-基础语法与常用关键字)
3. [Host 别名与通配符](#3-host-别名与通配符)
4. [ProxyJump 跳板机配置](#4-proxyjump-跳板机配置)
5. [Include 拆分管理](#5-include-拆分管理)
6. [匹配条件的进阶用法](#6-匹配条件的进阶用法)
7. [常见陷阱](#7-常见陷阱)
8. [核心要点](#8-核心要点)

---

## 1. ssh_config 的定位

> **背景**：每次 SSH 都要敲 `ssh -i key -p 2222 user@192.168.1.100`——长、易错、记不住。ssh_config 把这些「永久参数」固化为配置，命令变成 `ssh dev`。
> **目的**：别名化（短命令）+ 参数固化（不敲错）+ 环境隔离（各环境独立配置）。
> **适用范围**：任何高频 SSH 访问的开发/运维环境。
> **前提假设**：配置存在 `~/.ssh/config`（用户级）或 `/etc/ssh/ssh_config`（全局）。

```text
配置前后的对比
├── 之前：ssh -i ~/.ssh/prod_key -p 2222 deploy@192.168.1.100
├── 之后：ssh prod
└── 之前：ssh -o ProxyJump=jump1,jump2 -i key user@final
    ├── 之后：ssh final
    └── 本质：配置 = 参数的「命名空间」
```

---

## 2. 基础语法与常用关键字

### 2.1 语法结构

```
# ~/.ssh/config 语法
Host 名称              # 匹配块（别名/主机名/通配符）
    关键字 值          # 缩进（Tab 或空格均可）

规则：
├── 第一个匹配的 Host 块生效（首个匹配原则——注意顺序！）
├── 关键字大小写不敏感
└── 注释用 #
```

### 2.2 常用关键字

| 关键字 | 作用 | 示例 |
|--------|------|------|
| **HostName** | 真实主机名/IP | `HostName 192.168.1.100` |
| **User** | 登录用户名 | `User root` |
| **Port** | 端口（默认 22） | `Port 2222` |
| **IdentityFile** | 私钥路径 | `IdentityFile ~/.ssh/prod_key` |
| **IdentitiesOnly** | 只用指定密钥 | `IdentitiesOnly yes` |
| **ProxyJump** | 跳板机 | `ProxyJump jump1,jump2` |
| **ServerAliveInterval** | 保活间隔（秒） | `ServerAliveInterval 60` |
| **ServerAliveCountMax** | 保活失败次数 | `ServerAliveCountMax 3` |
| **Compression** | 压缩传输（慢网） | `Compression yes` |
| **ForwardAgent** | Agent 转发 | `ForwardAgent yes` |
| **StrictHostKeyChecking** | 主机密钥校验 | `StrictHostKeyChecking yes` |
| **LogLevel** | 日志级别 | `LogLevel VERBOSE` |

### 2.3 基础配置示例

```text
# ~/.ssh/config
# 开发服务器
Host dev
    HostName 192.168.1.50
    User devuser
    Port 22
    IdentityFile ~/.ssh/dev_key
    ServerAliveInterval 60

# 生产服务器（不同密钥/端口）
Host prod
    HostName 203.0.113.10
    User deploy
    Port 2222
    IdentityFile ~/.ssh/prod_key
    ServerAliveInterval 30
    ServerAliveCountMax 3

# 使用
ssh dev       # = ssh -i ~/.ssh/dev_key devuser@192.168.1.50
ssh prod      # = ssh -i ~/.ssh/prod_key -p 2222 deploy@203.0.113.10
scp file.txt prod:/tmp/    # scp/sftp 同样生效！
```

---

## 3. Host 别名与通配符

### 3.1 通配符匹配

```text
Host 支持通配符
├── *   匹配任意字符
├── ?   匹配单个字符
└── !   排除

应用场景（批量配置）：
├── Host *.example.com      # 公司所有服务器
├── Host 192.168.1.*        # 网段
└── Host !prod              # 排除（配合默认块）
```

### 3.2 通配符实战

```text
# 公司网段统一配置
Host 192.168.1.*
    User devuser
    IdentityFile ~/.ssh/company_key
    StrictHostKeyChecking accept-new

# 默认配置（未匹配任何 Host 时的兜底）
Host *
    ServerAliveInterval 60
    Compression yes
    AddKeysToAgent yes        # 自动加入 agent（2026 常用）

# 使用：ssh 192.168.1.100 → 自动用 company_key + devuser
```

### 3.3 匹配优先级（关键）

```text
⚠️ ssh_config 的「首个匹配」规则
├── 从上到下匹配，第一个匹配的块生效
├── 后面块的同类关键字不会覆盖（不是「后覆盖前」！）
└── 所以：具体配置放前面，通配/默认放后面

正确顺序示例：
Host prod            # 具体配置在前
    Port 2222
Host *               # 默认配置在后
    ServerAliveInterval 60
```

---

## 4. ProxyJump 跳板机配置

### 4.1 单跳板

```text
# 场景：访问内网服务器（需经跳板机 bastion）
Host bastion
    HostName 203.0.113.20
    User jumpuser
    IdentityFile ~/.ssh/jump_key

Host internal-server
    HostName 192.168.10.5
    User deploy
    ProxyJump bastion        # 经跳板机连接
    IdentityFile ~/.ssh/app_key

# 使用：ssh internal-server
# 链路：本机 → bastion → internal-server
# scp 同样生效：scp file internal-server:/tmp/
```

### 4.2 多级跳板

```text
Host jump1
    HostName 203.0.113.20
    User jump

Host jump2
    HostName 192.168.0.10
    User jump
    ProxyJump jump1          # jump2 经 jump1

Host final
    HostName 10.0.0.5
    User deploy
    ProxyJump jump1,jump2    # 逗号串联多级跳板
# 使用：ssh final → 本机 → jump1 → jump2 → final
```

### 4.3 ProxyJump 与 Agent 转发（配合要点）

```text
多级跳板的认证链路
├── 每跳都需要认证（密钥）
├── 方案 A：各跳配置自己的 IdentityFile
│   → 简单直接（推荐）
├── 方案 B：Agent Forwarding（转发本机密钥）
│   → 少配置但安全风险（见 04 篇）
└── 2026 推荐：方案 A（每跳显式配置密钥）
```

---

## 5. Include 拆分管理

### 5.1 为什么拆分

```text
ssh_config 拆分的价值
├── ① 单文件膨胀：几十个 Host 一个文件难维护
├── ② 按项目/环境隔离：每个项目自己的配置文件
├── ③ 团队共享：仓库里维护、成员拉取
└── ④ 权限隔离：敏感环境配置单独管理

Include 语法（OpenSSH 7.3+）
Include ~/.ssh/config.d/*        # 支持通配符
```

### 5.2 拆分实践

```text
# ~/.ssh/config（主文件——只放基础与 Include）
Host *
    ServerAliveInterval 60
    AddKeysToAgent yes
Include ~/.ssh/config.d/*

# ~/.ssh/config.d/company.conf（公司环境）
Host corp-*
    User devuser
    IdentityFile ~/.ssh/company_key

# ~/.ssh/config.d/personal.conf（个人环境）
Host home-nas
    HostName 192.168.1.100
    User admin
    IdentityFile ~/.ssh/nas_key

# 目录结构
~/.ssh/
├── config                  # 主配置
├── config.d/               # 拆分配置
│   ├── company.conf
│   └── personal.conf
├── id_ed25519              # 私钥
└── known_hosts
```

> 🎯 **团队实践**：公司环境配置入库（git 管理）+ 个人配置不入库——`Include config.d/*` 让两者共存互不干扰。

---

## 6. 匹配条件的进阶用法

### 6.1 Match 条件块

```text
Match 块（按条件匹配，非 Host 名）
├── Match host 192.168.1.*      # 按主机
├── Match user root             # 按用户
├── Match exec "命令"            # 按命令结果（高级）
└── 用途：Host 无法表达的动态条件

示例：
Match host 192.168.1.* user root
    Port 2222                    # 网段 + root 用户用 2222

Match all                        # 匹配所有（兜底）
    IdentityFile ~/.ssh/default
```

### 6.2 常见组合（企业场景）

```text
# 场景 1：GitHub 不同账号
Host github.com
    User git
    IdentityFile ~/.ssh/github_personal

Host github-work
    HostName github.com
    User git
    IdentityFile ~/.ssh/github_work
# 使用：git@github.com（个人）/ git@github-work（公司）

# 场景 2：AWS 批量主机
Host aws-*
    User ec2-user
    IdentityFile ~/.ssh/aws_key
    StrictHostKeyChecking accept-new
```

---

## 7. 常见陷阱

| # | 陷阱 | 表现 | 解法 |
|---|------|------|------|
| 1 | **首个匹配原则** | 通配块覆盖了具体配置 | 具体在前、通配在后 |
| 2 | **IdentitiesOnly 缺失** | 用了错误的密钥被拒 | 加 `IdentitiesOnly yes` |
| 3 | **权限过宽** | config 文件权限问题 | `chmod 600 ~/.ssh/config` |
| 4 | **Include 顺序** | 拆分文件被忽略 | Include 放前面 |
| 5 | **ProxyJump 循环** | 跳板配置互指 | 检查跳板链 |
| 6 | **scp 不生效** | 只配置了 ssh | scp/sftp 自动读同一配置（确认拼写） |
| 7 | **别名被 DNS 解析** | Host 别名与真实域名冲突 | 用 HostName 指定真实目标 |

```bash
# 排查配置
ssh -G dev           # 打印 dev 的最终生效配置（2026 排查利器！）
ssh -v dev           # 详细连接日志
ssh -T git@github.com   # 测试 GitHub 认证（输出识别信息）
```

---

## 8. 核心要点

> 🎯 **核心要点**：
> 1. ssh_config 三价值：别名化（短命令）+ 参数固化（不敲错）+ 环境隔离
> 2. **首个匹配原则**：具体配置在前、通配/默认在后（ssh_config 最大的坑）
> 3. ProxyJump 让跳板机配置化：`ssh final` 一条命令穿透多级网络；scp/sftp 同样生效
> 4. Include 拆分：主配置 + config.d/* 按项目/环境隔离——团队配置入库共享
> 5. `ssh -G dev` 查看最终生效配置是 2026 排查利器；IdentitiesOnly 防错用密钥

---

**下一模块**：[03-SSH-安全加固](03-SSH-安全加固.md) | **返回总览**：[00-SSH知识体系总览](00-SSH知识体系总览.md)
