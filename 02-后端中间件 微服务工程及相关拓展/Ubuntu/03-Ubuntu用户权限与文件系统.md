# Ubuntu 用户权限与文件系统

> 👤 用户/组管理、chmod 权限体系、chown 归属变更、磁盘挂载 fstab、文件系统类型 —— 多用户环境必备

---

## 📚 目录

1. [用户与组管理](#1-用户与组管理)
2. [权限体系（chmod）](#2-权限体系chmod)
3. [sudo 与 root](#3-sudo-与-root)
4. [磁盘挂载与 fstab](#4-磁盘挂载与-fstab)
5. [文件系统类型](#5-文件系统类型)

---

## 1. 用户与组管理

### 1.1 核心命令

```bash
# ==== 用户管理 ====
sudo useradd -m -s /bin/bash deploy     # 创建用户（带家目录和 shell）
sudo passwd deploy                      # 设置密码
sudo usermod -aG docker deploy          # 添加用户到 docker 组
sudo usermod -L deploy                  # 锁定用户
sudo usermod -U deploy                  # 解锁用户
sudo userdel -r deploy                  # 删除用户（含家目录）

# ==== 组管理 ====
sudo groupadd devops                    # 创建组
sudo usermod -aG devops ubuntu          # 添加用户到组（-a 追加不覆盖）
groups ubuntu                           # 查看用户所属组
getent group docker                     # 查看组成员

# ==== 查看当前用户 ====
whoami        # 当前用户名
id            # uid + gid + groups
who           # 当前登录的用户列表
w             # 更详细的登录信息
```

### 1.2 重要文件

```text
/etc/passwd   → 用户账户信息（每行一个用户）
  格式：username:x:uid:gid:comment:home:shell
  例：deploy:x:1001:1001:Deploy User:/home/deploy:/bin/bash

/etc/shadow   → 加密密码（仅 root 可读）
/etc/group    → 组信息

/etc/sudoers  → sudo 权限配置（用 visudo 编辑！）
  ⚠️ 不要直接编辑！用 sudo visudo
```

---

## 2. 权限体系（chmod）

### 2.1 读、写、执行

```text
Linux 权限模型：每个文件有三组权限

  ┌──────┬──────┬──────┐
  │ User │Group │Other │  所有者 | 组用户 | 其他人
  ├──────┼──────┼──────┤
  │ r w x│ r - x│ r - -│
  │ 4+2+1│ 4+0+1│ 4+0+0│
  └──────┴──────┴──────┘
            = 754

r = 读 (4)  → cat, less, 查看目录内容
w = 写 (2)  → 编辑文件, 在目录中创建/删除文件
x = 执行(1) → 运行可执行文件, 进入目录(cd)

常用组合：
  777  rwxrwxrwx  → 所有人全权限（⚠️ 不安全！）
  755  rwxr-xr-x  → 目录/可执行文件（推荐）
  644  rw-r--r--  → 普通文件（推荐）
  600  rw-------  → 私密文件（密钥、配置）
  400  r--------  → 只读文件
```

### 2.2 实战命令

```bash
# ==== chmod ====
chmod 755 script.sh           # 数字模式
chmod +x script.sh            # 添加执行权限
chmod -R 755 /opt/app         # 递归设置
chmod u+x,g-w,o-rwx file      # 符号模式（u=user,g=group,o=other）

# ==== chown（改归属）====
chown deploy:deploy file.txt  # 改所有者和组
chown -R deploy:deploy /opt/app  # 递归
chown :docker file.txt        # 仅改组

# ==== 查看权限 ====
ls -l                          # 详细权限
ls -ld /opt/app                # 目录本身权限（不加 -d 会列出内容）
stat file.txt                  # 更详细的文件信息
```

### 2.3 特殊权限

| 标记 | 数字 | 文件 | 目录 |
|:---:|:---:|------|------|
| **SUID** | 4 | 以文件所有者身份执行 | 无效 |
| **SGID** | 2 | 以组身份执行 | 新建文件继承目录组 |
| **Sticky** | 1 | 已废弃 | 只有文件所有者能删除（如 /tmp） |

```bash
# SUID 示例：passwd 命令允许普通用户修改自己的密码（需改 /etc/shadow）
ls -l /usr/bin/passwd
# -rwsr-xr-x  (s = SUID)

# Sticky Bit 示例
ls -ld /tmp
# drwxrwxrwt  (t = Sticky Bit)
```

---

## 3. sudo 与 root

### 3.1 sudo 配置

```bash
# ==== 切换到 root ====
sudo -i            # 开启 root shell
sudo su -          # 同上
sudo command       # 单次以 root 执行
exit               # 退出 root

# ==== 配置 sudoers（visudo）====
sudo visudo

# 给用户 deploy 免密码 sudo 权限：
deploy ALL=(ALL) NOPASSWD:ALL

# 仅允许特定命令：
deploy ALL=(ALL) NOPASSWD:/usr/bin/systemctl restart app, /usr/bin/docker

# ==== 查看 sudo 日志 ====
sudo journalctl -u sudo       # systemd 日志
cat /var/log/auth.log         # 传统日志
```

---

## 4. 磁盘挂载与 fstab

### 4.1 挂载命令

```bash
# ==== 查看磁盘与分区 ====
lsblk                          # 树形查看块设备
df -h                          # 查看已挂载磁盘使用量
sudo fdisk -l                  # 查看所有磁盘分区
lsblk -f                       # 查看文件系统和 UUID

# ==== 临时挂载 ====
sudo mount /dev/sdb1 /mnt/data
sudo mount -t ext4 /dev/sdb1 /mnt/data
sudo umount /mnt/data

# ==== 开机自动挂载（/etc/fstab）====
# 格式：设备 挂载点 文件系统 选项 dump pass
# UUID=xxx-xxx   /data  ext4  defaults  0  2

# 1. 查询 UUID
sudo blkid /dev/sdb1

# 2. 创建挂载点
sudo mkdir -p /data

# 3. 编辑 /etc/fstab，添加一行：
UUID=abc-123-def  /data  ext4  defaults,noatime  0  2

# 4. 测试（不重启验证）
sudo mount -a      # 挂载所有 fstab 条目
df -h | grep data  # 验证
```

### 4.2 挂载参数建议

| 参数 | 说明 | 场景 |
|------|------|------|
| `defaults` | 默认选项 | 通用 |
| `noatime` | 不更新访问时间 | 提升性能（推荐） |
| `nodiratime` | 不更新目录访问时间 | 同上 |
| `noexec` | 禁止执行 | /tmp 安全加固 |
| `nosuid` | 禁止 SUID | 安全加固 |

---

## 5. 文件系统类型

| 文件系统 | 特点 | 适用场景 |
|---------|------|---------|
| **ext4** | Linux 默认，稳定可靠 | 系统盘、数据盘 |
| **xfs** | 高性能、支持大文件 | CentOS/RHEL 默认，数据库 |
| **btrfs** | 快照、压缩、CoW | 高级特性需求 |
| **zfs** | 极致数据完整性 | NAS、存储服务器 |
| **ntfs** | Windows 文件系统 | 双系统共享盘 |
| **exfat** | 跨平台、大文件 | U 盘、移动硬盘 |
| **tmpfs** | 内存文件系统 | /tmp, /run |

```bash
# 格式化磁盘
sudo mkfs.ext4 /dev/sdb1        # 格式化为 ext4
sudo mkfs.xfs /dev/sdb1         # 格式化为 xfs

# 查看文件系统类型
df -T
lsblk -f
```

---

> 🎯 **核心要点**：**chmod 755/644** 最常用，**chown deploy:deploy** 改归属，**visudo** 配权限，**fstab** 管开机挂载。目录需要 x 权限才能进入——这是新手常犯的错误。

---

*创建于：2026年7月*
