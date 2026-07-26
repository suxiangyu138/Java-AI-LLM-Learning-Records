# 01 - Linux 操作系统基础与文件系统

> 🎯 Linux 文件系统是"一切皆文件"哲学的实现载体 — 从根目录结构到 inode 原理、从硬链接到虚拟文件系统，理解这些是"用 Linux 而不是背命令"的分水岭

---

## 目录

1. [Linux 操作系统架构](#1-linux-操作系统架构)
2. [一切皆文件](#2-一切皆文件)
3. [FHS 文件系统层次标准](#3-fhs-文件系统层次标准)
4. [文件类型全解](#4-文件类型全解)
5. [inode 与数据块](#5-inode-与数据块)
6. [硬链接与软链接](#6-硬链接与软链接)
7. [虚拟文件系统：/proc 与 /sys](#7-虚拟文件系统proc-与-sys)
8. [文件系统类型与挂载](#8-文件系统类型与挂载)
9. [Linux 启动流程](#9-linux-启动流程)
10. [Java 后端视角实战](#10-java-后端视角实战)
11. [常见面试题](#11-常见面试题)

---

## 1. Linux 操作系统架构

### 1.1 四层架构

```
┌─────────────────────────────────────┐
│        Applications（应用程序）        │  ← Java / Nginx / MySQL / Docker
├─────────────────────────────────────┤
│        Shell / GUI（用户接口）         │  ← Bash / Zsh / GNOME
├─────────────────────────────────────┤
│        Kernel（内核）                  │  ← 进程调度 / 内存管理 / 文件系统 / 网络
├─────────────────────────────────────┤
│        Hardware（硬件）               │  ← CPU / 内存 / 磁盘 / 网卡
└─────────────────────────────────────┘
```

| 层级 | 职责 | Java 后端关系 |
|------|------|-------------|
| **Kernel** | 管理 CPU、内存、IO、网络、文件系统 | JVM 运行依赖内核调度和内存管理 |
| **Shell** | 命令解释器，人机交互桥梁 | 日常运维的"操控面板" |
| **System Libraries** | libc 等系统调用封装 | JVM 通过 JNI 调用底层库 |
| **Applications** | 用户态程序 | Java 进程运行在这一层 |

### 1.2 内核态 vs 用户态

| 维度 | 内核态 (Kernel Mode) | 用户态 (User Mode) |
|------|---------------------|-------------------|
| 权限 | 可访问所有硬件和内存 | 受限，不能直接操作硬件 |
| 触发 | 系统调用、中断、异常 | 程序正常运行 |
| 代价 | 切换开销大（保存/恢复上下文） | — |
| Java 视角 | `FileInputStream.read()` 底层触发 `read()` 系统调用 | Java 代码运行在用户态 |

```bash
# 查看内核版本
uname -r        # 5.15.0-91-generic
uname -a        # 全部信息

# 查看发行版
cat /etc/os-release
cat /etc/redhat-release     # CentOS/RHEL
lsb_release -a              # Ubuntu
```

---

## 2. 一切皆文件

> 💡 Linux 的核心理念：**Everything is a file** — 普通文件、目录、设备、套接字、管道，都以文件的形式呈现，统一用 `open/read/write/close` 操作。

### 2.1 七种文件类型

| 标识 | 类型 | 说明 | 示例 |
|:---:|------|------|------|
| `-` | 普通文件 (Regular) | 存储数据 | `app.jar`, `config.yml`, `test.txt` |
| `d` | 目录 (Directory) | 文件名→inode 映射 | `/opt/app/` |
| `l` | 符号链接 (Symlink) | 指向另一个路径的"快捷方式" | `/usr/bin/java` → `/etc/alternatives/java` |
| `b` | 块设备 (Block Device) | 随机访问的存储设备 | `/dev/sda`, `/dev/nvme0n1` |
| `c` | 字符设备 (Char Device) | 按字节流访问的设备 | `/dev/tty`, `/dev/null`, `/dev/random` |
| `p` | 命名管道 (FIFO) | 进程间通信 | `mkfifo mypipe` |
| `s` | 套接字 (Socket) | 网络/本地进程通信 | `/var/run/docker.sock`, `/tmp/mysql.sock` |

```bash
# 查看文件类型
ls -l /dev/sda
# brw-rw---- 1 root disk 8, 0 Jan 1 00:00 /dev/sda
# ↑ b = 块设备

ls -l /dev/null
# crw-rw-rw- 1 root root 1, 3 Jan 1 00:00 /dev/null
# ↑ c = 字符设备

# 分辨普通文件 vs 目录
ls -ld /opt/app/
# drwxr-xr-x 2 appuser appuser 4096 Jan 15 /opt/app/
# ↑ d = 目录

# file 命令查看文件实际类型（不看后缀）
file /usr/bin/java        # ELF 64-bit executable
file app.jar              # Zip archive data
file /dev/sda             # block special
```

### 2.2 特殊设备文件（Java 后端必知）

| 设备 | 路径 | 用途 |
|------|------|------|
| 黑洞 | `/dev/null` | 丢弃所有写入，读取返回 EOF |
| 零数据源 | `/dev/zero` | 提供无限 `\0` 字符 |
| 随机数 | `/dev/random` | 真随机数（熵池不足会阻塞） |
| 伪随机 | `/dev/urandom` | 伪随机数（不会阻塞，生产推荐） |
| 标准输入 | `/dev/stdin` | 当前进程的标准输入 |
| 标准输出 | `/dev/stdout` | 当前进程的标准输出 |
| 标准错误 | `/dev/stderr` | 当前进程的错误输出 |

```bash
# Java 后端场景
# 丢弃输出（等价于 > /dev/null）
nohup java -jar app.jar > /dev/null 2>&1 &

# 生成随机密钥
head -c 32 /dev/urandom | base64

# 生成指定大小的测试文件（模拟大日志）
dd if=/dev/zero of=test.log bs=1M count=100
```

---

## 3. FHS 文件系统层次标准

> 🎯 FHS (Filesystem Hierarchy Standard) 定义了 Linux 目录布局。Java 后端要能说出每个目录放什么，排查问题时知道去哪里找。

### 3.1 根目录结构速览

```
/                        # 根目录，一切文件树的起点
├── /bin                 # 基本命令（ls, cp, cat 等），→ /usr/bin（现代发行版）
├── /sbin                # 系统管理命令（fdisk, mount 等），→ /usr/sbin
├── /boot                # 内核文件 + GRUB 引导加载器
├── /dev                 # 设备文件（/dev/sda, /dev/null, /dev/tty）
├── /etc                 # 系统级配置文件（⭐ 最常操作）
├── /home                # 普通用户家目录（/home/appuser/.bashrc）
├── /root                # root 用户家目录
├── /lib /lib64          # 系统库文件（libc.so 等），→ /usr/lib
├── /media /mnt          # 临时挂载点（U盘、NFS、数据盘）
├── /opt                 # 第三方软件安装目录（⭐ 手动安装的 JDK/Maven）
├── /proc                # 进程与内核信息虚拟文件系统（⭐ 排查必用）
├── /run                 # 运行时临时文件（PID、socket），重启清空
├── /srv                 # 服务数据目录（/srv/www, /srv/ftp）
├── /sys                 # 内核设备模型虚拟文件系统
├── /tmp                 # 临时文件，所有人可写（Sticky Bit 保护）
├── /usr                 # 用户软件资源（Unix System Resources）
│   ├── /usr/bin         # 用户命令
│   ├── /usr/lib         # 库文件
│   ├── /usr/local       # 本地编译安装的软件
│   └── /usr/share       # 架构无关的数据（文档、图标）
└── /var                 # 可变数据（⭐ 日志、缓存、数据库文件）
    ├── /var/log         # 系统与应用日志（⭐ 最频繁操作）
    ├── /var/lib         # 持久化状态数据（MySQL 数据库、Docker 数据）
    ├── /var/spool       # 队列任务（cron、邮件）
    └── /var/run         # → /run（运行时数据）
```

### 3.2 Java 后端核心操作目录

| 目录 | 操作 | 场景 |
|------|------|------|
| `/etc/` | 读取/修改配置 | `/etc/nginx/`、`/etc/systemd/system/` |
| `/var/log/` | `tail -f` 查日志 | 应用的 `nohup.out`、`/var/log/messages` |
| `/opt/` | 部署应用 | `/opt/myapp/app.jar` |
| `/proc/` | 读取进程信息 | `cat /proc/cpuinfo`、`/proc/<pid>/status` |
| `/tmp/` | 临时文件 | 文件上传中间件临时目录 |
| `/home/appuser/` | 环境变量配置 | `.bashrc`、`.ssh/authorized_keys` |

### 3.3 关键配置文件速查

```bash
# ═══ 系统配置 ═══
/etc/fstab              # 自动挂载配置
/etc/hosts              # 本地 DNS 解析
/etc/hostname           # 主机名
/etc/resolv.conf        # DNS 服务器配置
/etc/sysctl.conf        # 内核参数
/etc/security/limits.conf  # 资源限制（⭐ ulimit 配置）

# ═══ 用户与权限 ═══
/etc/passwd             # 用户账户信息
/etc/shadow             # 密码哈希
/etc/group              # 组信息
/etc/sudoers            # sudo 权限配置（通过 visudo 编辑）

# ═══ 定时任务 ═══
/etc/crontab            # 系统级 cron
/etc/cron.d/            # 模块化 cron 配置

# ═══ 网络 ═══
/etc/ssh/sshd_config    # SSH 服务端配置
/etc/nginx/nginx.conf   # Nginx 配置
```

---

## 4. 文件类型全解

### 4.1 `file` 命令 — 不看后缀看本质

```bash
# Linux 不依赖扩展名判断文件类型，而是检查文件头（magic number）
file /usr/bin/java
# /usr/bin/java: ELF 64-bit LSB shared object, x86-64

file app.jar
# app.jar: Zip archive data, at least v2.0 to extract

file application.log
# application.log: ASCII text

file /usr/lib/libc.so.6
# /usr/lib/libc.so.6: ELF 64-bit LSB shared object

file /dev/sda
# /dev/sda: block special (8/0)
```

### 4.2 Java 相关文件类型

| 文件类型 | 识别特征 | 交互命令 |
|----------|----------|----------|
| ELF (二进制可执行) | `ELF` 头部 | `ldd` 查看依赖，`readelf` 分析 |
| Shell 脚本 | `#!/bin/bash` shebang | `bash -x` 调试 |
| JAR/WAR | `Zip archive` | `unzip -l`，`jar -tf` |
| 文本日志 | `ASCII/UTF-8 text` | `tail -f`, `grep`, `less` |
| 压缩包 | `gzip/bzip2/XZ compressed` | `tar`, `gunzip` |

```bash
# 检查 JAR 是否是有效的 zip
unzip -t app.jar

# 查看 JAR 内 Manifest
unzip -p app.jar META-INF/MANIFEST.MF

# 查看 ELF 依赖的动态库
ldd /usr/bin/java | head -5
```

---

## 5. inode 与数据块

> 💡 **inode (索引节点)** 是文件系统的核心概念 — 每个文件有且仅有一个 inode，存储文件的元数据（权限、大小、时间戳、数据块指针），但不存储文件名。文件名存在目录的数据块中。

### 5.1 inode 存储的内容

```
inode 包含：
├── 文件类型和权限 (rwx)
├── Owner / Group (UID / GID)
├── 文件大小 (bytes)
├── 时间戳（⭐ 三个时间）
│   ├── atime: 最后访问时间
│   ├── mtime: 最后修改内容时间（⭐ 最常用）
│   └── ctime: 最后修改 inode 时间
├── 链接计数 (硬链接数)
└── 数据块指针（指向磁盘上实际数据的位置）

inode 不包含：
✗ 文件名 → 存在目录的数据块中
✗ 文件内容 → 存在数据块中
```

### 5.2 inode 相关命令

```bash
# 查看文件的 inode 信息
stat app.jar
#   File: app.jar
#   Size: 52428800    Blocks: 102400     IO Block: 4096
# Device: 802h/2050d  Inode: 12345678    Links: 1
# Access: 2024-01-15 10:30:00.000000000
# Modify: 2024-01-15 10:25:00.000000000
# Change: 2024-01-15 10:25:00.000000000

# 查看 inode 编号
ls -i app.jar
# 12345678 app.jar

# 查看分区的 inode 使用情况（⭐ 小文件太多会耗尽 inode）
df -i
# Filesystem      Inodes   IUsed   IFree IUse% Mounted on
# /dev/sda1      6553600  123456 6430144    2% /

# 找出 inode 使用最多的目录（大量小缓存文件）
for i in /*; do echo "$(find $i | wc -l) $i"; done | sort -rn | head -10
```

### 5.3 inode 耗尽问题

```bash
# ⚠️ 典型场景：磁盘空间没满但无法创建文件
df -h        # 磁盘还有空间
df -i        # 但 inode 已耗尽！

# 常见原因：
# 1. 大量小文件（Session 文件、缓存文件、邮件队列）
# 2. 定时任务产生的临时文件未清理
# 3. /tmp 目录下文件暴涨

# 排查：
ls -la /tmp | wc -l
du -sh /tmp/*
find /tmp -type f | wc -l

# 清理：
find /tmp -type f -mtime +7 -delete
```

### 5.4 目录与 inode

```
目录的本质：文件名 → inode 的映射表
├── 每个目录至少有两个硬链接：. (自身) 和 .. (父目录)
├── 这就是为什么 chmod 对目录的 w 权限意味着"创建/删除文件"
│   → 删除文件 = 修改目录的映射表 = 需要目录的 w 权限
│   → 删除文件不需要文件本身的 w 权限！
└── 硬链接数 = 子目录数 + 2（包括 . 和 ..）
```

```bash
# 验证目录的硬链接数
mkdir testdir
ls -ld testdir
# drwxr-xr-x 2 user user 4096 ... testdir
#              ↑ 硬链接数 = 2 (testdir/. 和 testdir/ 本身)

mkdir testdir/sub
ls -ld testdir
# drwxr-xr-x 3 user user 4096 ... testdir
#              ↑ 硬链接数 = 3 (. + .. + sub/..)
```

---

## 6. 硬链接与软链接

### 6.1 对比总览

| 维度 | 硬链接 (Hard Link) | 软链接 (Symbolic Link) |
|------|-------------------|----------------------|
| 本质 | 同一个 inode 的多个目录入口 | 指向目标路径的独立文件 |
| inode | **相同** inode | **不同** inode，存的是路径字符串 |
| 跨文件系统 | ❌ 不可以 | ✅ 可以 |
| 链接目录 | ❌ 不可以（防循环） | ✅ 可以 |
| 原文件删除后 | 数据仍在（链接计数 > 1） | **断链**（悬空链接，红色显示） |
| 文件大小 | 不占额外空间（仅目录条目） | 占少量空间（路径字符串长度） |
| `ls -l` 显示 | 普通文件，链接数 > 1 | `lrwxrwxrwx file -> target` |
| 创建命令 | `ln target linkname` | `ln -s target linkname` |

### 6.2 实验验证

```bash
# ═══ 硬链接实验 ═══
echo "hello" > original.txt
ln original.txt hardlink.txt        # 创建硬链接

ls -li original.txt hardlink.txt
# 12345678 -rw-r--r-- 2 user user 6 Jan 15 10:00 hardlink.txt
# 12345678 -rw-r--r-- 2 user user 6 Jan 15 10:00 original.txt
# ↑ 相同 inode (12345678)，链接计数 = 2

rm original.txt                     # 删除"原文件"
cat hardlink.txt                    # hello → 数据仍在！
ls -li hardlink.txt
# 12345678 -rw-r--r-- 1 user user 6 Jan 15 10:00 hardlink.txt
# ↑ inode 不变，链接计数变为 1

# ═══ 软链接实验 ═══
echo "hello" > original2.txt
ln -s original2.txt softlink.txt    # 创建软链接

ls -li original2.txt softlink.txt
# 87654321 lrwxrwxrwx 1 user user 14 Jan 15 10:01 softlink.txt -> original2.txt
# 11111111 -rw-r--r-- 1 user user  6 Jan 15 10:01 original2.txt
# ↑ 不同 inode

cat softlink.txt                    # hello
rm original2.txt
cat softlink.txt                    # cat: softlink.txt: No such file or directory
ls -l softlink.txt                  # 红色显示 -> 断链！
```

### 6.3 Java 后端实战场景

```bash
# 场景1：多版本 JDK 切换（软链接）
ls -l /usr/bin/java
# /usr/bin/java -> /etc/alternatives/java -> /usr/lib/jvm/java-17/bin/java
# 更新 alternatives 即可全局切换 JDK 版本

# 场景2：日志目录重定向（软链接）
# 磁盘 /data 更大，把 /app/logs 指向 /data/app-logs
mv /app/logs /data/app-logs
ln -s /data/app-logs /app/logs

# 场景3：共享库版本管理
ls -l /usr/lib/libssl.so*
# libssl.so -> libssl.so.3
# libssl.so.3 -> libssl.so.3.0.0
# → 升级 patch 版本不需要重新链接程序

# 场景4：部署目录的原子切换（软链接）
# /opt/myapp/versions/v1.0/app.jar
# /opt/myapp/versions/v1.1/app.jar
ln -snf /opt/myapp/versions/v1.1 /opt/myapp/current
# → Java 服务启动时读 /opt/myapp/current/app.jar，回滚只需 re-link
```

---

## 7. 虚拟文件系统：/proc 与 /sys

> 💡 `/proc` 和 `/sys` 不是真实磁盘文件，而是内核向用户空间暴露信息的接口。读取它们是零开销的内核数据获取方式。

### 7.1 /proc — 进程与系统信息

```bash
# ═══ CPU 信息 ═══
cat /proc/cpuinfo | grep "model name" | uniq
nproc                              # CPU 核心数

# ═══ 内存信息 ═══
cat /proc/meminfo | head -5
# MemTotal:       16384000 kB
# MemFree:         2048000 kB
# MemAvailable:    8192000 kB   ← 更准确（含可回收缓存）

# ═══ 内核参数 ═══
cat /proc/sys/net/ipv4/ip_forward    # IP 转发是否开启
cat /proc/sys/fs/file-max            # 系统最大文件句柄数

# ═══ 磁盘与分区 ═══
cat /proc/partitions                 # 分区信息
cat /proc/mounts                     # 当前挂载信息
```

### 7.2 /proc/<pid>/ — 进程信息（⭐ Java 排查必用）

| 路径 | 内容 | JVM 排查场景 |
|------|------|-------------|
| `/proc/<pid>/cmdline` | 启动命令（含 JVM 参数） | 查看 `-Xmx`、`-XX:+UseG1GC` 等 JVM 参数 |
| `/proc/<pid>/status` | 进程状态、内存占用 | 查看 VmRSS、Threads |
| `/proc/<pid>/fd/` | 打开的文件描述符 | `ls -l /proc/<pid>/fd/` 查文件句柄泄漏 |
| `/proc/<pid>/limits` | 资源限制 | 查看 Max open files |
| `/proc/<pid>/environ` | 环境变量 | 查看 JAVA_HOME、SPRING_PROFILES_ACTIVE |
| `/proc/<pid>/maps` | 内存映射 | 分析内存布局 |
| `/proc/<pid>/task/` | 线程目录 | `ls /proc/<pid>/task/ | wc -l` 查线程数 |

```bash
# 实战：快速查看 Java 进程的完整启动命令
PID=$(pgrep -f "spring-boot")
cat /proc/$PID/cmdline | tr '\0' ' '
# 输出：/usr/bin/java -Xms2g -Xmx4g -XX:+UseG1GC -jar /opt/myapp/app.jar

# 实战：排查文件句柄泄漏
ls -l /proc/$PID/fd/ | wc -l
# 如果数量接近 ulimit -n 的上限 → 句柄泄漏

# 实战：查看进程的环境变量
cat /proc/$PID/environ | tr '\0' '\n' | grep SPRING
```

### 7.3 /sys — 内核设备模型

```bash
# ═══ CPU 信息 ═══
ls /sys/devices/system/cpu/
cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq  # 当前频率

# ═══ 块设备信息 ═══
ls /sys/block/
cat /sys/block/sda/queue/scheduler    # IO 调度器

# ═══ 网络设备 ═══
ls /sys/class/net/
cat /sys/class/net/eth0/speed         # 网卡速率
```

---

## 8. 文件系统类型与挂载

### 8.1 常见文件系统对比

| 文件系统 | 特点 | 适用场景 |
|----------|------|----------|
| **ext4** | 稳定成熟，最大 1EB | 通用 Linux 系统盘、数据盘 |
| **XFS** | 高性能，支持大文件，RHEL7+ 默认 | 大文件存储、数据库 |
| **Btrfs** | Copy-on-Write、快照、压缩 | 容器存储（Docker 推荐） |
| **ZFS** | 数据完整性、快照、去重 | 高可靠性存储 |
| **tmpfs** | 内存文件系统，重启清空 | `/tmp`、`/dev/shm` |
| **NFS** | 网络文件系统 | 多服务器共享存储 |

### 8.2 挂载操作

```bash
# ═══ 查看挂载信息 ═══
mount | column -t
df -hT                    # 显示文件系统类型
lsblk -f                  # 树状展示块设备 + 文件系统

# ═══ 临时挂载 ═══
mount /dev/sdb1 /mnt/data
mount -t ext4 /dev/sdb1 /mnt/data

# ═══ 永久挂载 (/etc/fstab) ═══
# <设备>        <挂载点>  <类型>  <选项>            <dump> <pass>
/dev/sdb1       /data     ext4    defaults,noatime   0      2
UUID=xxx        /data     xfs     defaults           0      2
tmpfs           /dev/shm  tmpfs   defaults,size=8G   0      0

# noatime: 不更新访问时间，减少磁盘 IO（⭐ 推荐）
# nodiratime: 目录也不更新访问时间

# 验证并挂载
mount -a                   # 按 /etc/fstab 挂载所有

# ═══ 卸载 ═══
umount /mnt/data
umount -l /mnt/data        # 懒卸载（繁忙时强制，进程释放后完成）
fuser -mv /mnt/data        # 查看谁在使用该挂载点
```

### 8.3 tmpfs 与 /dev/shm（⭐ Java 后端加速）

```bash
# /dev/shm 是内存文件系统，读写速度远快于磁盘
df -h /dev/shm
# Filesystem      Size  Used Avail Use% Mounted on
# tmpfs           7.8G     0  7.8G   0% /dev/shm

# Java 后端场景：将临时计算文件写入 /dev/shm
# 例如：大量 CSV 文件排序合并的中间结果
# ⚠️ 注意：/dev/shm 默认是内存的 50%，写满会影响系统！
```

---

## 9. Linux 启动流程

> 🎯 理解启动流程有助于排查服务起不来、磁盘挂载失败、内核参数不生效等问题。

### 9.1 启动步骤

```
1. BIOS/UEFI → 硬件自检 (POST)
      │
2. Boot Loader (GRUB2) → 加载内核镜像到内存
      │
3. Kernel → 初始化设备驱动、挂载根文件系统（只读）
      │
4. initramfs → 临时根文件系统，加载必要的驱动（磁盘、LVM）
      │
5. systemd (PID=1) → 挂载真实根文件系统（读写），启动所有服务
      │
6. Targets → multi-user.target / graphical.target
      │
7. Login → getty 登录提示或图形界面
```

### 9.2 systemd 运行级别 (Targets)

| Target | 等价 runlevel | 说明 |
|--------|:---:|------|
| `poweroff.target` | 0 | 关机 |
| `rescue.target` | 1 | 单用户救援模式 |
| `multi-user.target` | 3 | 多用户文本模式（⭐ 服务器默认） |
| `graphical.target` | 5 | 图形界面 |
| `reboot.target` | 6 | 重启 |

```bash
# 查看当前 target
systemctl get-default

# 切换 target
systemctl isolate multi-user.target   # 立即切换
systemctl set-default multi-user.target  # 设置默认

# 查看启动耗时
systemd-analyze
systemd-analyze blame | head -10     # 各服务启动耗时排名
```

---

## 10. Java 后端视角实战

### 10.1 应用部署的标准目录布局

```bash
# ⭐ 推荐的生产级 Java 应用目录结构
/opt/myapp/
├── current -> versions/v1.1/        # 软链接 → 当前版本（⭐ 原子切换）
├── versions/
│   ├── v1.0/
│   │   ├── bin/                     # 启动/停止脚本
│   │   ├── lib/                     # 依赖 JAR
│   │   ├── config/                  # 配置文件（只读）
│   │   └── app.jar
│   └── v1.1/
│       └── ... (同上)
├── logs -> /data/app-logs/          # 软链接 → 大容量数据盘
├── data/                            # 持久化数据
├── shared/                          # 共享文件（上传文件等）
└── tmp/                             # 临时文件
```

### 10.2 初始化新服务器的 Checklist

```bash
# 1. 查看发行版和内核
cat /etc/os-release && uname -r

# 2. 设置主机名
hostnamectl set-hostname prod-app-01

# 3. 配置时区
timedatectl set-timezone Asia/Shanghai
timedatectl set-ntp true

# 4. 创建应用用户
useradd -r -s /sbin/nologin -M appuser

# 5. 创建应用目录
mkdir -p /opt/myapp/{versions,logs,data,config}

# 6. 创建数据盘（如果有独立磁盘）
mkfs.xfs /dev/sdb
mkdir -p /data
mount /dev/sdb /data
echo 'UUID=$(blkid -s UUID -o value /dev/sdb) /data xfs defaults,noatime 0 2' >> /etc/fstab

# 7. 调整内核参数 (/etc/sysctl.d/99-app.conf)
cat >> /etc/sysctl.d/99-app.conf << 'EOF'
net.core.somaxconn = 65535
net.ipv4.tcp_max_syn_backlog = 8192
vm.swappiness = 10                 # 减少 swap 使用
fs.file-max = 1000000
EOF
sysctl -p /etc/sysctl.d/99-app.conf

# 8. 调整资源限制 (/etc/security/limits.d/99-app.conf)
cat >> /etc/security/limits.d/99-app.conf << 'EOF'
appuser soft nofile 65535
appuser hard nofile 65535
appuser soft nproc  32768
appuser hard nproc  32768
EOF
```

### 10.3 文件系统相关排查场景

| 场景 | 排查命令 | 说明 |
|------|----------|------|
| 磁盘满了 | `df -h` + `du -sh /*` | 逐层递进定位大目录 |
| inode 耗尽 | `df -i` | 找大量小文件的目录 |
| 文件删了空间没释放 | `lsof \| grep deleted` | 有进程持有文件句柄 |
| 挂载点不可写 | `mount \| grep ro,` | 检查是否只读挂载 |
| 启动找不到文件 | `findmnt` | 确认分区是否正常挂载 |

---

## 11. 常见面试题

### Q1：硬链接和软链接的区别？

> 硬链接共享 inode，本质是同一文件多个名字；软链接是独立的"快捷方式"文件，存的是目标路径。硬链接不能跨文件系统、不能链接目录；软链接可以。原文件删除后硬链接数据仍在，软链接断链。详见第6节。

### Q2：inode 是什么？存储哪些信息？

> inode 是文件的元数据索引节点，存储文件类型、权限、大小、时间戳、数据块指针等信息，但不存文件名和内容。每个文件有且仅有一个 inode。详见第5节。

### Q3：`/proc` 文件系统有什么用？

> `/proc` 是内核向用户空间暴露信息的虚拟文件系统，读取零开销。可通过 `/proc/<pid>/cmdline` 查看启动参数、`/proc/<pid>/fd/` 排查文件句柄泄漏、`/proc/meminfo` 查看内存。详见第7节。

### Q4：Linux 启动流程是怎样的？

> BIOS → GRUB 加载内核 → initramfs 临时根文件系统 → systemd (PID=1) 挂载真实根文件系统 → 启动服务到 multi-user.target。详见第9节。

### Q5：如何在不重启的情况下让文件系统修改生效？

> `mount -a` 重新挂载 `/etc/fstab` 中未挂载的项；`mount -o remount,rw /` 将只读根文件系统重新挂载为读写。
