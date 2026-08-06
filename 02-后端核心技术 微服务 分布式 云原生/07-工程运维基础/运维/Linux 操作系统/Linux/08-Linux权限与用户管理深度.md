# 08 - Linux 权限与用户管理深度

> Linux 权限模型是后端开发者必须精通的核心知识：从基础的 rwx 到 ACL、sudoers、SUID/SGID、capabilities——权限配置不当是生产事故的头号根源之一。

---

## 📚 目录

1. [基础权限模型](#1-基础权限模型)
2. [特殊权限：SUID、SGID、Sticky Bit](#2-特殊权限suidsgidsticky-bit)
3. [ACL 访问控制列表](#3-acl-访问控制列表)
4. [用户与组管理](#4-用户与组管理)
5. [sudo 配置深度](#5-sudo-配置深度)
6. [文件属性与隐藏权限](#6-文件属性与隐藏权限)
7. [Linux Capabilities](#7-linux-capabilities)
8. [Java 后端视角的权限实战](#8-java-后端视角的权限实战)
9. [常见面试题](#9-常见面试题)

---

## 1. 基础权限模型

### 1.1 rwx 权限位

```bash
# ls -l 输出解析
# -rwxr-xr-- 1 zhangsan developers 4096 Jan 15 app.jar
#  │├─┤├─┤├─┤
#  │ │  │  └── Others (其他人)  r--
#  │ │  └───── Group   (组)     r-x
#  │ └──────── Owner   (所有者)  rwx
#  └────────── Type    (- 文件/d 目录/l 链接)

# 权限数值对照
# r = 4, w = 2, x = 1
# rwx = 7, rw- = 6, r-x = 5, r-- = 4, -wx = 3, -w- = 2, --x = 1

chmod 755 script.sh   # rwxr-xr-x
chmod 644 config.txt  # rw-r--r--
chmod 600 id_rsa      # rw-------

# 符号模式
chmod u+x script.sh       # 所有者加执行权限
chmod g-w config.txt      # 组去掉写权限
chmod o= data/            # 清除其他人所有权限
chmod a+r README.md       # 所有人加读权限
```

### 1.2 目录权限的特殊含义

| 权限 | 文件 | 目录 |
|------|------|------|
| **r** | 读取文件内容 | 列出目录内容（`ls`） |
| **w** | 修改文件内容 | 创建/删除目录中的文件 |
| **x** | 执行文件 | 进入目录（`cd`） |

```bash
# 典型坑：目录没 x 权限
drw-r--r-- 2 root root 4096 Jan 15 data/
# 虽然目录有 r 权限（可以 ls），但没有 x（无法 cd 进去）
# → ls data/ 会报 Permission denied
# → 目录必须 x 才能访问其内容

# Java 后端典型场景：
# /app/logs/ 目录需要服务用户有 rwx 权限
mkdir -p /app/logs
chown appuser:appgroup /app/logs
chmod 750 /app/logs   # rwxr-x---
```

### 1.3 umask 默认权限掩码

```bash
# umask 决定新建文件/目录的默认权限
# 文件默认 666 - umask（文件不自动给 x）
# 目录默认 777 - umask

umask         # 查看当前值 → 0022
umask 027     # 设置新值

# umask 022 → 文件 644 (666-022), 目录 755 (777-022)
# umask 027 → 文件 640, 目录 750 （更安全）
# umask 077 → 文件 600, 目录 700 （最安全，仅所有者可访问）
```

---

## 2. 特殊权限：SUID、SGID、Sticky Bit

### 2.1 三种特殊权限

```bash
# ═══ SUID (Set User ID) — 4000 ═══
# 文件以所有者的身份执行
# 典型：passwd 命令修改 /etc/shadow（普通用户无权直接写）

ls -l /usr/bin/passwd
# -rwsr-xr-x 1 root root ... /usr/bin/passwd
#    ↑ s 表示 SUID（如果所有者本来有 x，显示 s；没 x 显示 S）

# ⚠️ SUID 是安全风险点！不要给自定义脚本/程序设 SUID
# 攻击者可通过 SUID 程序提权

# ═══ SGID (Set Group ID) — 2000 ═══
# 文件：以组的身份执行
# 目录：目录中新建文件的组自动继承目录的组（⭐ 团队共享目录必用）

mkdir /shared/project
chown :developers /shared/project
chmod 2770 /shared/project   # SGID + rwxrwx---
# → 任何人在此目录创建的文件，所属组自动是 developers

# ═══ Sticky Bit — 1000 ═══
# 仅目录有效：只有文件所有者才能删除自己的文件
# 典型：/tmp 目录

ls -ld /tmp
# drwxrwxrwt ... /tmp
#        ↑ t 表示 Sticky Bit

chmod +t /shared/uploads   # 设 Sticky Bit
chmod 1777 /shared/uploads # 数字方式
```

### 2.2 权限的完整数字

```bash
# 4 位数字：特殊权限 + 基本权限
# SUID=4, SGID=2, Sticky=1

chmod 4755 script   # SUID + rwxr-xr-x
chmod 2770 shared/  # SGID + rwxrwx---
chmod 1777 tmp/     # Sticky + rwxrwxrwx
```

---

## 3. ACL 访问控制列表

### 3.1 ACL 解决什么问题

```
传统权限只能设 Owner + Group + Others → 无法给第三个用户单独授权

场景：/app/config/ 目录
  Owner: root
  Group: developers (rwx)
  现在需要给审计用户 auditor 只读权限
  → auditor 不属于 developers 组
  → 改 Group？auditor 又不该有写权限
  → ACL 解决：给 auditor 单独加 read 权限！
```

### 3.2 ACL 操作

```bash
# ═══ 查看 ACL ═══
getfacl /app/config/
# # file: /app/config/
# # owner: root
# # group: developers
# user::rwx
# group::rwx
# other::---

# ═══ 设置 ACL ═══
# 给 auditor 用户设只读权限
setfacl -m u:auditor:r-x /app/config/

# 给 qa 组设只读权限
setfacl -m g:qa:r-x /app/config/

# ═══ 递归设置 ═══
setfacl -R -m u:auditor:r-x /app/config/
# 目录设默认 ACL（目录下新建文件自动继承）
setfacl -d -m u:auditor:r-x /app/config/

# ═══ 删除 ACL ═══
setfacl -x u:auditor /app/config/    # 删除指定条目
setfacl -b /app/config/              # 删除所有 ACL（回退到传统权限）

# ═══ 查看效果 ═══
ls -l /app/config/
# -rwxr-x---+ 1 root developers ... /app/config/
#          ↑ + 号表示有 ACL 扩展权限
```

### 3.3 ACL Mask 机制

```bash
# ACL 有一个 mask 值，实际权限 = 设置的权限 & mask
# setfacl 设置权限时自动更新 mask

getfacl /app/config/
# user:auditor:r-x        # 设置权限
# mask::rwx               # mask 值
# effective:r-x           # 实际生效权限

# 如果 mask 是 r--，则 auditor 实际只有 r-- (r-x & r-- = r--)
```

---

## 4. 用户与组管理

### 4.1 关键文件

| 文件 | 用途 | 格式 |
|------|------|------|
| `/etc/passwd` | 用户账户信息 | `user:x:uid:gid:desc:home:shell` |
| `/etc/shadow` | 加密密码 | `user:hash:lastchg:min:max:warn:inactive:expire` |
| `/etc/group` | 组信息 | `group:x:gid:members` |
| `/etc/sudoers` | sudo 配置 | 只读，通过 `visudo` 编辑 |

### 4.2 常用命令

```bash
# ═══ 用户管理 ═══
useradd -m -s /bin/bash appuser      # 创建用户（-m 建家目录）
useradd -u 1500 -g appgroup appuser   # 指定 UID 和主组
useradd -G docker,developers appuser  # 附加组
usermod -aG docker appuser            # 追加附加组（必须加 -a）
userdel -r appuser                    # 删除用户 + 家目录

# ═══ 组管理 ═══
groupadd developers
groupmod -n newdev developers         # 重命名组
groupdel developers

# ═══ 密码 ═══
passwd appuser                        # 设置密码
passwd -l appuser                     # 锁定账户
passwd -u appuser                     # 解锁
chage -l appuser                      # 查看密码过期策略
chage -M 90 appuser                   # 密码 90 天过期
chage -E 2024-12-31 appuser           # 账户过期日

# ═══ 切换 ═══
su - appuser                          # 切换用户（- 加载环境变量）
sudo -u appuser command               # 以指定用户执行命令
sudo -i                               # 切换到 root（保留环境）

# ═══ 查看 ═══
id appuser                            # 查看 UID/GID/组
whoami                                # 当前用户
groups                                # 当前用户的组
w / who                               # 谁在线
last                                  # 最近登录记录
```

---

## 5. sudo 配置深度

### 5.1 sudoers 语法

```bash
# ⚠️ 永远用 visudo 编辑，不要直接改 /etc/sudoers！
visudo
visudo -f /etc/sudoers.d/java-app  # 推荐：单独文件

# 语法格式：
# 用户  主机=(以谁的身份)  命令
# user  host=(runas)      commands

# ═══ 示例 ═══

# 给 appuser 完全 sudo 权限
appuser ALL=(ALL) ALL

# 给 appuser 无需密码的 sudo（⭐ 常用于 CI/CD）
appuser ALL=(ALL) NOPASSWD: ALL

# 仅允许执行特定命令（⭐ 安全最佳实践）
appuser ALL=(root) NOPASSWD: /bin/systemctl restart myapp
appuser ALL=(root) NOPASSWD: /bin/systemctl status myapp
appuser ALL=(root) NOPASSWD: /usr/bin/journalctl -u myapp -n *

# 允许 DevOps 组管理服务
%devops ALL=(root) /bin/systemctl restart *, /bin/systemctl status *

# 命令别名（管理多个命令）
Cmnd_Alias APP_CMDS = /bin/systemctl restart myapp, \
                      /bin/systemctl status myapp, \
                      /usr/bin/journalctl -u myapp
appuser ALL=(root) NOPASSWD: APP_CMDS
```

### 5.2 sudoers 最佳实践

```bash
# 1. 永远用 visudo 编辑
# 2. 尽量不直接改 /etc/sudoers，在 /etc/sudoers.d/ 下创建独立文件
# 3. 最小权限原则：只给需要的命令
# 4. 能不用 NOPASSWD 就不用
# 5. 记录 sudo 操作：Defaults logfile=/var/log/sudo.log

# /etc/sudoers.d/java-app
# 只给应用启动和日志查看权限
appuser ALL=(root) /bin/systemctl restart myapp
appuser ALL=(root) /usr/bin/journalctl -u myapp
```

---

## 6. 文件属性与隐藏权限

### 6.1 chattr / lsattr

```bash
# Linux 扩展文件属性 — 连 root 都无法绕过

# ═══ 常用属性 ═══
chattr +i important.conf    # 不可变（immutable）— 不能修改、删除、重命名、链接
chattr -i important.conf    # 解除不可变

chattr +a app.log           # 仅追加（append only）— 只能追加内容，不能修改已有
chattr -a app.log

# ═══ 查看 ═══
lsattr important.conf
# ----i--------e----- important.conf

# ═══ 安全场景 ═══
# 1. 锁定关键配置文件
chattr +i /etc/ssh/sshd_config

# 2. 保护日志不被篡改（配合 a 属性）
chattr +a /var/log/audit/audit.log
```

---

## 7. Linux Capabilities

### 7.1 为什么需要 Capabilities

```
传统模型：root = 一切权限
  → 很多程序只需要一种特权（如绑定 1024 以下端口）
  → 却必须整个进程以 root 运行
  → 风险太大！

Capabilities：将 root 的权限拆分为细粒度的能力
  → 给进程只赋予它需要的能力
  → 不需要完整的 root
```

### 7.2 常用 Capabilities

| Capability | 说明 | Java 后端场景 |
|-----------|------|-------------|
| `CAP_NET_BIND_SERVICE` | 绑定 1024 以下端口 | ⭐ Java 应用绑定 80/443 |
| `CAP_NET_RAW` | 使用 RAW/PACKET 套接字 | `ping` 命令 |
| `CAP_SYS_PTRACE` | 跟踪进程 | JVM 调试/性能分析 |
| `CAP_SYS_TIME` | 修改系统时间 | NTP 同步 |
| `CAP_DAC_READ_SEARCH` | 绕过文件读/目录搜索权限 | 备份工具 |
| `CAP_SYS_RESOURCE` | 修改资源限制 | 修改 ulimit |

```bash
# ═══ 给 Java 应用绑定 80 端口 ═══
# 方式一：setcap
sudo setcap 'cap_net_bind_service=+ep' /usr/lib/jvm/java-17/bin/java
# ⚠️ 这会让所有 Java 程序都能绑 80 端口，谨慎！

# 方式二：用 authbind（推荐，更精细）
sudo apt install authbind
sudo touch /etc/authbind/byport/80
sudo chmod 500 /etc/authbind/byport/80
sudo chown appuser /etc/authbind/byport/80
authbind java -jar app.jar

# 方式三：用 iptables 端口转发（最安全）
sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 8080
```

---

## 8. Java 后端视角的权限实战

### 8.1 应用部署的标准权限模型

```bash
# ═══ 推荐的应用部署权限方案 ═══

# 1. 创建专用服务用户（不允许登录 Shell）
useradd -r -s /sbin/nologin -M appuser

# 2. 应用目录所有权
mkdir -p /opt/myapp/{bin,lib,config,logs,data}
chown -R root:appuser /opt/myapp
chmod 750 /opt/myapp

# 3. 可执行文件和配置 — 只读
chmod 750 /opt/myapp/bin/*
chmod 640 /opt/myapp/config/*

# 4. 日志和数据目录 — 可写
chown -R appuser:appuser /opt/myapp/logs
chown -R appuser:appuser /opt/myapp/data
chmod 750 /opt/myapp/logs
chmod 750 /opt/myapp/data

# 5. 禁止修改可执行文件
chattr +i /opt/myapp/bin/*.jar

# 6. systemd 服务以 appuser 运行
# /etc/systemd/system/myapp.service:
[Service]
User=appuser
Group=appuser
NoNewPrivileges=yes         # 禁止提权
ProtectSystem=strict        # /usr, /boot, /etc 只读
ProtectHome=true            # 禁止访问 /home
ReadWritePaths=/opt/myapp/logs /opt/myapp/data
```

### 8.2 常见问题排查

```bash
# 问题1：Java 应用启动报 Permission denied
# → 检查应用目录权限
ls -la /opt/myapp/
# → 检查运行用户
ps aux | grep java
# → 检查是否有 SELinux 阻止
ausearch -m avc -ts recent | audit2why

# 问题2：无法写日志
# → 检查目录所有者和权限
ls -la /opt/myapp/logs/
# → 检查磁盘空间（满了也写不了）
df -h /opt/myapp/logs/
# → 检查 inode
df -i /opt/myapp/logs/

# 问题3：端口绑定失败 (Permission denied, bind)
# → 检查端口号 < 1024 需要 root 或 CAP_NET_BIND_SERVICE
# → 检查端口是否已被占用
ss -tlnp | grep 8080

# 问题4：su / sudo 的细微差别
su appuser        # 切换用户，保持原有 $HOME 和 $PATH（不推荐）
su - appuser      # 切换用户，加载目标用户的环境（推荐！）
# → Java 环境变量 $JAVA_HOME 可能因 su 不带 - 而丢失
```

---

## 9. 常见面试题

### Q1：Linux 文件权限 rwx 对文件和目录分别意味着什么？

> 文件：读内容/修改内容/执行。目录：列出内容/增删文件/进入目录。特别注意目录没有 x 权限时，即使有 r 也无法 cd 进去。详见第1.2节。

### Q2：SUID、SGID、Sticky Bit 的区别和用途？

> SUID(4)：以文件所有者身份执行（如 passwd）。SGID(2)：目录中新建文件继承目录组（团队共享目录）。Sticky(1)：只有所有者能删除文件（如 /tmp）。详见第2节。

### Q3：ACL 解决了传统权限的什么问题？

> 传统权限只能设 Owner+Group+Others，无法给第三个用户单独授权。ACL 允许给任意用户/组设置独立的 rwx 权限。详见第3节。

### Q4：如何安全地让 Java 应用绑定 80 端口而不以 root 运行？

> 方案：setcap `cap_net_bind_service`（不够精细）、authbind（推荐）、iptables 端口转发（最安全）。详见第7.2节。

### Q5：生产中应用服务用户的推荐配置？

> 用专用系统用户（`/sbin/nologin`），应用目录 root:appuser 只读，日志/数据目录 appuser 可写，systemd 配置 NoNewPrivileges+ProtectSystem。详见第8节。

---

**上一模块**：[07-服务器运维基础](07-服务器运维基础.md) ｜ **下一模块**：[09-JVM 排查工具详解](09-JVM排查工具详解.md) ｜ **返回总览**：[00-知识体系总览](00-Linux知识体系总览.md)

**【参考来源】**
- man 手册：`man chmod` `man chown` `man sudoers` `man setcap`\n- Linux Capabilities：https://man7.org/linux/man-pages/man7/capabilities.7.html
