# CentOS SELinux 深度解析与安全加固

> 🔒 MAC 强制访问控制、安全上下文(Type/User/Role)、策略排障、audit2allow 自动生成规则、Java 应用部署中 5 个经典 SELinux 问题及解法

---

## 📚 目录

1. [SELinux 原理](#1-selinux-原理)
2. [安全上下文深度解析](#2-安全上下文深度解析)
3. [排障全流程](#3-排障全流程)
4. [Java 应用 5 大实战场景](#4-java-应用-5-大实战场景)
5. [安全加固 Checklist](#5-安全加固-checklist)

---

## 1. SELinux 原理

### 1.1 DAC vs MAC

```text
Linux 传统权限 = DAC（自主访问控制 Discretionary AC）
  → 文件所有者可以随意 chmod
  → 问题：root 可做任何事、恶意程序可获得过高权限

SELinux = MAC（强制访问控制 Mandatory AC）
  → 内核强制检查，不经过所有者同意
  → 每个进程/文件/端口都被打上安全标签
  → 即使 root 也不能违反 SELinux 策略

类比：
  DAC → 你家门锁（主人决定谁可以进门）
  MAC → 小区保安系统（无论主人是否允许，有案底的人不许进小区）
```

### 1.2 三模式

```bash
getenforce                       # 查看当前模式
sestatus                          # SELinux 完整状态

# Enforcing  → 强制执行，拒绝+审计日志（生产）
# Permissive → 仅警告不拒绝（调试用）
# Disabled   → 完全关闭（❌ 自废武功）

# 临时切换
sudo setenforce 0                 # → Permissive
sudo setenforce 1                 # → Enforcing

# 永久配置
sudo vi /etc/selinux/config
# SELINUX=enforcing
```

### 1.3 SELinux vs AppArmor

| 维度 | SELinux (CentOS) | AppArmor (Ubuntu) |
|------|:--------------:|:--------------:|
| 粒度 | 文件/端口/进程/系统调用 → 极细 | 文件路径 → 粗 |
| 标签方式 | inode 级别安全上下文 | 路径名匹配 |
| 策略语言 | 极复杂（Type Enforcement） | 简单 Profile |
| 默认策略 | targeted（仅限关键服务） | 按服务加载 Profile |
| 学习成本 | 高 | 低 |

---

## 2. 安全上下文深度解析

### 2.1 完整结构

```bash
ls -Z /var/www/html/index.html
# system_u:object_r:httpd_sys_content_t:s0
# └──┬──┘ └──┬──┘ └──────┬──────┘ └┬┘
#   用户     角色       类型(Type)   MCS/MLS 级别

# 最重要的字段：Type（类型强制 Type Enforcement）
# SELinux 99% 的规则都是 "Type A 能否访问 Type B"
```

```bash
# ===== 查看安全上下文 =====
ls -Z /var/www/                      # 文件/目录
ps -eZ | grep nginx                  # 进程
id -Z                                # 当前用户
netstat -Ztlnp | grep 8080           # 端口上下文（需 netstat）

# ===== 修改安全上下文 =====
# 临时改（chcon）
chcon -R -t httpd_sys_content_t /var/www/html
chcon -R --reference=/var/www/html /new-webroot   # 复制现有

# 永久改（semanage + restorecon）
sudo semanage fcontext -a -t httpd_sys_content_t "/www(/.*)?"
sudo restorecon -Rv /www            # 应用永久规则

# ===== 端口上下文 =====
semanage port -l | grep http        # 查看 HTTP 允许的端口
semanage port -a -t http_port_t -p tcp 8088   # 添加允许端口
semanage port -d -t http_port_t -p tcp 8088   # 删除

# ===== 布尔值（开关）=====
getsebool -a | grep http            # 查看所有 HTTP 相关开关
setsebool -P httpd_can_network_connect on     # 允许 HTTP 联网
```

### 2.2 Java/中间件相关的关键 Type

| Type | 说明 | 涉及服务 |
|------|------|---------|
| `httpd_sys_content_t` | Web 可读的静态文件 | Nginx/Apache |
| `httpd_t` | Web 服务器进程 | Nginx/Apache |
| `tomcat_var_lib_t` | Tomcat 库目录 | Tomcat |
| `tomcat_log_t` | Tomcat 日志目录 | Tomcat |
| `mysqld_db_t` | MySQL 数据目录 | MySQL |
| `mysqld_t` | MySQL 进程 | MySQL |
| `sshd_t` | SSH 进程 | SSH |
| `user_home_t` | 普通用户家目录 | - |
| `initrc_t` | systemd 启动的脚本 | systemd |

---

## 3. 排障全流程

### 3.1 三板斧

```bash
# 第一步：确认是不是 SELinux 在捣乱
sudo ausearch -m avc -ts recent | tail -20

# 第二步：看详细原因和修复建议
sudo sealert -a /var/log/audit/audit.log
# sealert 会给出"99% confidence"的建议！

# 第三步：生成并安装修复策略
sudo grep denied /var/log/audit/audit.log \
  | audit2allow -M my_java_fix
# → 生成 my_java_fix.te（策略源码）和 my_java_fix.pp（编译模块）
sudo semodule -i my_java_fix.pp

# 查看已安装的自定义策略
sudo semodule -l | grep -v "^100\b"
```

### 3.2 调试流程

```text
标准排障流程：

  1. setenforce 0 → 确认问题消失
     → 消失 = SELinux 的问题 → 进入步骤 2
     → 没消失 = 不是 SELinux → 查别的原因

  2. setenforce 1 → 恢复 Enforcing

  3. ausearch -m avc -ts recent → 找到被拒绝的操作

  4. audit2allow 生成策略

  5. ✅ 永久解决，而非 setenforce 0
```

---

## 4. Java 应用 5 大实战场景

### 4.1 Spring Boot 用非标准端口

```bash
# 问题：server.port=8888 → 启动失败 Permission denied
# 原因：SELinux 限制了 HTTP 端口范围

# 解法：
sudo semanage port -a -t http_port_t -p tcp 8888
```

### 4.2 Nginx 反向代理被拒

```bash
# 问题：Nginx proxy_pass http://localhost:8080 → 502
# 原因：SELinux 默认禁止 httpd 发起网络连接

# 解法：
sudo setsebool -P httpd_can_network_connect on
getsebool httpd_can_network_connect   # 验证
```

### 4.3 自定义部署目录

```bash
# 问题：jar 放在 /opt/myapp，systemd 启动失败
# 原因：/opt/myapp 没有合适的 SELinux 类型

# 解法1：继承现有类型
sudo semanage fcontext -a -t bin_t "/opt/myapp(/.*)?"
sudo restorecon -Rv /opt/myapp

# 解法2：放在 SELinux 已预设的路径
# /usr/share/ → 系统共享文件
# /var/lib/  → 应用数据
# /var/log/  → 应用日志
```

### 4.4 Tomcat 读写应用数据

```bash
# 问题：Tomcat 应用写日志到 /data/logs 被 SELinux 拒绝

# 解法：
sudo semanage fcontext -a -t tomcat_log_t "/data/logs(/.*)?"
sudo restorecon -Rv /data/logs
```

### 4.5 MySQL 自定义数据目录

```bash
# 问题：MySQL datadir 改为 /data/mysql → 启动失败

# 解法：
sudo semanage fcontext -a -t mysqld_db_t "/data/mysql(/.*)?"
sudo restorecon -Rv /data/mysql
sudo chown -R mysql:mysql /data/mysql
```

---

## 5. 安全加固 Checklist

```text
生产环境加固清单：

  SELinux：
    □ setenforce 1（必须是 Enforcing）
    □ 自定义路径/端口用 semanage 配置
    □ 不要永久 setenforce 0

  SSH：
    □ PermitRootLogin no
    □ PasswordAuthentication no（仅密钥）
    □ Port 2222（非默认端口）
    □ 安装 fail2ban

  firewalld：
    □ 默认 zone=public，仅开放必要端口
    □ 数据库端口限来源 IP（rich-rule）

  审计：
    □ 启用 dnf-automatic（安全自动更新）
    □ 定期 ausearch -m avc 审计 SELinux 日志
```

```bash
# 自动安全更新
sudo dnf install dnf-automatic
sudo systemctl enable --now dnf-automatic.timer
```

---

> 🎯 **核心原则**：遇到"权限明明对但就是不行"→ **先查 SELinux**（`ausearch -m avc`）。不要第一反应 `setenforce 0`，而是用 `audit2allow` 生成正确策略。SELinux 是看门狗，不是拦路虎。

---

*创建于：2026年7月*
