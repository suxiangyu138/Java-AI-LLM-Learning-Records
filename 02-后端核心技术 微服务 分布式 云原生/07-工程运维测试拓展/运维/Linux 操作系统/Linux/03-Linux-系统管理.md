# 03 - Linux 系统管理

> 🎯 系统管理是后端工程师的"基础设施能力" — 从磁盘分区到进程管理、从 systemd 服务到内核参数调优，掌握这些才能让 Java 应用在生产环境稳定运行

---

## 📚 目录

1. [磁盘管理](#1-磁盘管理)
2. [进程管理](#2-进程管理)
3. [systemd 服务管理](#3-systemd-服务管理)
4. [软件包管理](#4-软件包管理)
5. [用户与权限](#5-用户与权限)
6. [环境变量](#6-环境变量)
7. [基础系统配置](#7-基础系统配置)
8. [Java 后端实战场景](#8-java-后端实战场景)
9. [常见问题](#9-常见问题)

---

## 1. 磁盘管理

### 1.1 常用命令

| 命令 | 用途 | 示例 |
|------|------|------|
| `df -h` | 查看文件系统磁盘空间占用 | `df -h` — 人类可读格式 |
| `df -i` | 查看 inode 使用情况 | `df -i` — ⚠️ 小文件过多会耗尽 inode |
| `du -sh <dir>` | 统计目录总大小 | `du -sh /var/log` |
| `du -h --max-depth=1` | 一级子目录大小 | `du -h --max-depth=1 /opt/` |
| `lsblk` | 列出块设备（树状） | `lsblk` — 查看磁盘分区结构 |
| `fdisk -l` | 查看/管理分区表 | `sudo fdisk -l /dev/sda` |
| `mount` | 挂载文件系统 | `mount /dev/sdb1 /mnt/data` |
| `umount` | 卸载文件系统 | `umount /mnt/data` |
| `blkid` | 查看块设备 UUID | `blkid /dev/sdb1` |
| `iostat -x 1` | IO 性能统计 | `iostat -x 1 5` — 关注 %util 和 await |

### 1.2 分区与挂载完整流程

```bash
# 1. 查看磁盘
lsblk

# 2. 分区（交互式）
sudo fdisk /dev/sdb
# n → 新建分区 → p 主分区 → 回车默认 → 回车默认 → w 写入

# 3. 格式化
sudo mkfs.ext4 /dev/sdb1       # ext4
sudo mkfs.xfs /dev/sdb1        # XFS（推荐大文件场景）

# 4. 临时挂载
sudo mount /dev/sdb1 /mnt/data

# 5. 永久挂载（写入 /etc/fstab）
# ⭐ 推荐用 UUID，比设备名更稳定
UUID=$(sudo blkid -s UUID -o value /dev/sdb1)
echo "UUID=$UUID /data xfs defaults,noatime 0 2" | sudo tee -a /etc/fstab

# 6. 验证
sudo mount -a                  # 按 fstab 挂载所有
df -h /data
```

### 1.3 /etc/fstab 字段详解

| 字段 | 说明 | 示例 |
|------|------|------|
| 设备 | 设备路径或 UUID | `UUID=abc123` 或 `/dev/sdb1` |
| 挂载点 | 目录路径 | `/data` |
| 文件系统 | 类型 | `ext4`, `xfs`, `tmpfs`, `nfs` |
| 选项 | 挂载选项 | `defaults,noatime,nodiratime` |
| dump | 备份标记 (0/1) | `0` 不备份 |
| pass | fsck 检查顺序 | `0` 不检查, `1` 根分区, `2` 其他 |

```bash
# ⭐ 推荐挂载选项
# defaults,noatime  — 不更新访问时间，减少磁盘 IO
# defaults,nofail   — 设备不存在时不阻塞启动（适合外接存储）
```

### 1.4 Java 后端实用场景

| 场景 | 命令 | 说明 |
|------|------|------|
| 日志盘监控 | `df -h /var/log` | 检查日志分区是否写满 |
| 大文件定位 | `du -sh /app/* \| sort -rh \| head -10` | 找出项目目录中最大的文件 |
| inode 问题 | `df -i` | 小文件过多导致无法新建文件 |
| IO 瓶颈分析 | `iostat -x 1 5` | 查看 `%util` 和 `await` |
| 文件删除未释放 | `lsof \| grep deleted` | 进程持有文件句柄 |

---

## 2. 进程管理

### 2.1 常用命令

| 命令 | 用途 | Java 开发常用场景 |
|------|------|------------------|
| `ps -ef` | 查看所有进程 | `ps -ef \| grep java` 查 Java 进程 |
| `ps aux` | 查看进程 CPU/内存 | `ps aux --sort=-%mem \| head` 查内存占用 Top |
| `ps -eLf` | 查看线程 | `ps -eLf \| grep java \| wc -l` 统计 Java 线程数 |
| `top` | 实时进程监控 | `top -p <pid>` 监控指定进程 |
| `top -H -p <pid>` | 查看进程内线程 | 排查 Java 线程热点 |
| `htop` | 增强版 top（需安装） | 交互式进程管理，更直观 |
| `kill -15 <pid>` | 优雅终止 (SIGTERM) | **优先使用** — JVM 可执行 ShutdownHook |
| `kill -9 <pid>` | 强制终止 (SIGKILL) | **最后手段** — JVM 无法捕获 |
| `kill -3 <pid>` | 打印线程堆栈 | 等价于 `jstack <pid>`，输出到 stdout |
| `nohup` | 后台运行（忽略 HUP） | `nohup java -jar app.jar > app.log 2>&1 &` |
| `jobs -l` | 查看当前 Shell 后台任务 | |
| `bg` / `fg` | 前后台切换 | `Ctrl+Z` 暂停 → `bg` 后台 → `fg` 调回 |
| `nice -n -10` | 调整进程优先级 | 提高 Java 服务优先级 |

### 2.2 进程状态详解

| 状态 | 缩写 | 含义 | 排查意义 |
|:---:|------|------|----------|
| Running | **R** | 正在运行或等待运行 | 正常 |
| Sleeping | **S** | 可中断休眠（等待事件） | 正常 — 大多数进程在此状态 |
| Disk Sleep | **D** | 不可中断休眠（等待 IO） | ⚠️ 磁盘 IO 瓶颈 |
| Zombie | **Z** | 僵尸进程（已终止，父进程未回收） | ⚠️ 过多时占用 PID 资源 |
| Stopped | **T** | 被信号暂停 | `Ctrl+Z` 或调试中 |
| Dead | **X** | 已死亡 | 几乎看不到 |

### 2.3 查找与终止 Java 进程

```bash
# ═══ 查找 Java 进程 ═══
# 方式1：ps + grep
ps -ef | grep spring-boot-app.jar | grep -v grep

# 方式2：pgrep（更简洁）
pgrep -f "spring-boot"
pgrep -fl "java"            # 显示进程名

# 方式3：jps（JDK 自带）
jps -l -v                   # 列出 Java 进程 + JVM 参数

# ═══ 优雅停止 ═══
kill -15 <pid>              # SIGTERM → Spring Boot 会执行 @PreDestroy
kill <pid>                  # 默认就是 -15

# ═══ 强制停止 ═══
kill -9 <pid>               # 最后手段

# ═══ 按端口杀进程 ═══
fuser -k 8080/tcp
lsof -ti:8080 | xargs kill -15
```

### 2.4 nohup 与后台运行

```bash
# 标准后台启动 Java 服务
nohup java -Xms512m -Xmx2g -jar app.jar --server.port=8080 > logs/app.log 2>&1 &

# 输出重定向详解
# > logs/app.log    标准输出 → app.log
# 2>&1              标准错误重定向到标准输出
# &                  后台运行

# 查看后台日志
tail -f logs/app.log

# 将暂停任务切到后台并脱离终端
# Ctrl+Z 暂停 → bg → disown -h %1
```

> ⚠️ **注意**：生产环境应使用 `systemd` 或容器编排管理进程，避免依赖 `nohup`。`nohup` 不会自动重启、没有健康检查。

---

## 3. systemd 服务管理

### 3.1 systemctl 核心命令

| 命令 | 用途 |
|------|------|
| `systemctl start <svc>` | 启动服务 |
| `systemctl stop <svc>` | 停止服务 |
| `systemctl restart <svc>` | 重启服务 |
| `systemctl reload <svc>` | 重载配置（不中断服务） |
| `systemctl enable <svc>` | 设置开机自启 |
| `systemctl disable <svc>` | 取消开机自启 |
| `systemctl status <svc>` | 查看服务状态 |
| `systemctl is-active <svc>` | 是否运行中 |
| `systemctl is-enabled <svc>` | 是否开机自启 |
| `systemctl list-unit-files` | 列出所有 unit 文件 |
| `systemctl daemon-reload` | 重载 unit 文件 |
| `journalctl -u <svc> -f` | 查看服务日志（实时） |
| `journalctl -u <svc> --since "1h ago"` | 查看最近 1 小时日志 |

### 3.2 自定义 Java 服务 Unit（⭐ 生产级模板）

```ini
# /etc/systemd/system/myapp.service
[Unit]
Description=My Spring Boot Application
Documentation=https://wiki.example.com/myapp
After=network.target
Wants=network.target

[Service]
Type=simple
User=appuser
Group=appuser
WorkingDirectory=/opt/myapp
ExecStart=/usr/bin/java \
    -Xms2g -Xmx4g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/opt/myapp/logs/ \
    -jar /opt/myapp/current/app.jar
ExecStop=/bin/kill -15 $MAINPID
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=myapp

# ═══ 安全加固 ═══
NoNewPrivileges=yes          # 禁止提权
PrivateTmp=yes                # 隔离 /tmp
ProtectSystem=strict          # /usr, /boot, /etc 只读
ProtectHome=true              # 禁止访问 /home
ReadWritePaths=/opt/myapp/logs /opt/myapp/data

# ═══ 资源限制 ═══
LimitNOFILE=65535
LimitNPROC=32768
MemoryMax=5G

[Install]
WantedBy=multi-user.target
```

```bash
# 部署后执行
sudo systemctl daemon-reload
sudo systemctl enable myapp
sudo systemctl start myapp
sudo systemctl status myapp
journalctl -u myapp -f         # 查看日志
```

---

## 4. 软件包管理

### 4.1 RPM/DNF（RHEL/Rocky/Alma）与 APT（Debian/Ubuntu）

| 操作 | YUM / DNF | APT |
|------|-----------|-----|
| 更新仓库 | `yum update` / `dnf update` | `apt update` |
| 升级软件 | `yum upgrade` | `apt upgrade` |
| 安装 | `yum install <pkg>` | `apt install <pkg>` |
| 卸载 | `yum remove <pkg>` | `apt remove <pkg>` |
| 搜索 | `yum search <keyword>` | `apt search <keyword>` |
| 查看已安装 | `rpm -qa` | `dpkg -l` |
| 查看文件归属 | `rpm -qf <file>` | `dpkg -S <file>` |
| 查看包文件列表 | `rpm -ql <pkg>` | `dpkg -L <pkg>` |
| 清理缓存 | `yum clean all` | `apt clean` |

### 4.2 Java 环境安装

```bash
# CentOS / RHEL
sudo dnf install -y java-17-openjdk-devel
# 查看可用的 Java 版本
dnf search openjdk | grep devel

# Ubuntu / Debian
sudo apt update
sudo apt install -y openjdk-17-jdk
# 切换默认 Java 版本
sudo update-alternatives --config java

# 验证
java -version
javac -version
echo $JAVA_HOME
```

### 4.3 源码编译安装（无网络或定制版本）

```bash
# 1. 下载源码
wget https://example.com/software-1.0.tar.gz
tar -xzf software-1.0.tar.gz
cd software-1.0

# 2. 编译三部曲
./configure --prefix=/opt/software
make -j$(nproc)
sudo make install

# 3. 配置环境变量
echo 'export PATH=/opt/software/bin:$PATH' >> /etc/profile.d/software.sh
```

---

## 5. 用户与权限

### 5.1 用户管理命令

| 命令 | 用途 | 示例 |
|------|------|------|
| `useradd` | 创建用户 | `sudo useradd -m -s /bin/bash appuser` |
| `useradd -r` | 创建系统用户（无家目录） | `sudo useradd -r -s /sbin/nologin appuser` |
| `passwd` | 设置密码 | `sudo passwd appuser` |
| `usermod` | 修改用户属性 | `sudo usermod -aG docker appuser` |
| `userdel` | 删除用户 | `sudo userdel -r appuser` |
| `id` | 查看用户 UID/GID | `id appuser` |
| `groups` | 查看用户组 | `groups appuser` |
| `who` / `w` | 查看登录用户 | |
| `last` | 最近登录记录 | `last -10` |

### 5.2 文件权限（速查）

```bash
# 权限位：rwx r-x r-x  →  755
#         用户 组  其他

chmod 755 script.sh              # 数字方式
chmod u+x script.sh              # 符号方式：用户加执行权限
chmod g-w config.txt             # 组去掉写权限
chmod o= data/                   # 清除其他人所有权限

chown appuser:appuser app.jar    # 修改文件所属用户:组
chown -R appuser:appuser /opt/   # 递归修改

# ⭐ 常用部署目录权限
mkdir -p /opt/myapp/{logs,data,config}
chown -R root:appuser /opt/myapp        # 应用文件 root 所有，appuser 组可读
chmod 750 /opt/myapp
chown -R appuser:appuser /opt/myapp/logs  # 日志目录 appuser 可写
chmod 750 /opt/myapp/logs
```

> 💡 更深入的权限管理（SUID/SGID/ACL/sudo/capabilities）请参见 `13-Linux权限与用户管理深度.md`

### 5.3 sudo 配置（速查）

```bash
# ⚠️ 永远用 visudo 编辑
visudo

# 为 appuser 赋予特定命令的无密码 sudo
echo 'appuser ALL=(root) NOPASSWD: /bin/systemctl restart myapp, /bin/systemctl status myapp' \
  | sudo tee /etc/sudoers.d/appuser
```

---

## 6. 环境变量

### 6.1 配置文件加载层级

| 文件 | 作用域 | 加载时机 |
|------|--------|----------|
| `/etc/environment` | 系统全局 | 登录时（PAM） |
| `/etc/profile` | 全局 Shell | 交互式登录 Shell |
| `/etc/profile.d/*.sh` | 全局扩展 | 被 /etc/profile source 加载 |
| `/etc/bash.bashrc` | 全局 Bash | 交互式非登录 Shell |
| `~/.bash_profile` | 当前用户 | 交互式登录 Shell（优先于 ~/.profile） |
| `~/.bashrc` | 当前用户 | 交互式非登录 Shell |

> 💡 登录 Shell vs 非登录 Shell：SSH 登录触发登录 Shell（读 `~/.bash_profile`）；`bash` 命令开启非登录 Shell（读 `~/.bashrc`）。`~/.bash_profile` 通常会 source `~/.bashrc`。

### 6.2 Java 环境变量配置

```bash
# /etc/profile.d/java.sh — 推荐方式
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH

# 立即生效
source /etc/profile.d/java.sh
```

### 6.3 查看与管理

```bash
echo $JAVA_HOME                  # 查看单个变量
env | grep JAVA                  # 筛选相关变量
printenv                         # 全部环境变量
export MY_VAR=value              # 临时设置（仅当前 Shell）
unset MY_VAR                     # 删除变量
```

---

## 7. 基础系统配置

### 7.1 主机名与时间

```bash
# ═══ 主机名 ═══
hostnamectl set-hostname prod-api-01
hostnamectl status

# ═══ 时区与时间 ═══
timedatectl status
sudo timedatectl set-timezone Asia/Shanghai
sudo timedatectl set-ntp true

# 手动设置时间（NTP 不可用时）
sudo date -s "2024-01-15 10:30:00"
hwclock --systohc                # 同步到硬件时钟
```

### 7.2 网络接口

```bash
# 查看网络接口
ip addr show
ip link show

# 查看路由
ip route show

# DNS 配置
cat /etc/resolv.conf
echo 'nameserver 8.8.8.8' | sudo tee -a /etc/resolv.conf
```

### 7.3 内核参数调优（Java 服务常见调整）

```bash
# /etc/sysctl.d/99-java-app.conf
cat > /etc/sysctl.d/99-java-app.conf << 'EOF'
# 文件句柄上限
fs.file-max = 1000000

# 网络连接优化
net.core.somaxconn = 65535              # 监听队列最大长度
net.ipv4.tcp_max_syn_backlog = 8192     # SYN 队列长度
net.ipv4.ip_local_port_range = 1024 65535  # 本地端口范围
net.ipv4.tcp_tw_reuse = 1               # TIME_WAIT 端口复用

# 内存管理 — 减少 swap 使用（Java 服务不需要 swap）
vm.swappiness = 10                      # 尽量不用 swap（0-100，默认 60）
EOF

# 生效
sudo sysctl -p /etc/sysctl.d/99-java-app.conf

# 查看当前值
sysctl fs.file-max
sysctl vm.swappiness
```

### 7.4 资源限制 (ulimit / limits.conf)

```bash
# 查看当前限制
ulimit -a

# 临时修改
ulimit -n 65535        # 最大打开文件数
ulimit -u 32768        # 最大进程/线程数

# 永久修改：/etc/security/limits.d/99-app.conf
cat > /etc/security/limits.d/99-app.conf << 'EOF'
appuser soft nofile 65535
appuser hard nofile 65535
appuser soft nproc  32768
appuser hard nproc  32768
EOF
```

---

## 8. Java 后端实战场景

### 8.1 磁盘排查完整流程

```bash
# 场景：应用报 "No space left on device"

# 1. 确认分区使用
df -h | grep -v tmpfs

# 2. 检查 inode
df -i | grep -v tmpfs

# 3. 定位大目录
du -sh /* 2>/dev/null | sort -rh | head -10

# 4. 逐层递进
du -sh /opt/* | sort -rh | head -10

# 5. 检查是否有已删除但未释放的文件
lsof | grep deleted | awk '{print $1, $2, $7}' | sort -uk2

# 6. 清理日志
find /opt/myapp/logs -name "*.log" -mtime +30 -delete
journalctl --vacuum-size=500M
```

### 8.2 进程异常排查

```bash
# 场景：Java 进程 CPU 飙升

# 1. 定位进程
top -c
# 记下高 CPU 的 Java 进程 PID

# 2. 定位线程
top -H -p <pid>
# 记下高 CPU 的线程 TID

# 3. TID 转十六进制
printf '%x\n' <tid>

# 4. 查看线程堆栈
jstack <pid> | grep -A 20 <hex_tid>

# 5. 查看 GC 情况
jstat -gc <pid> 1000 10
```

---

## 9. 常见问题

### Q1：如何查看哪个进程占用了某个端口？

```bash
sudo lsof -i :8080
ss -tlnp | grep 8080
fuser -v 8080/tcp
```

### Q2：磁盘空间满但 `du` 统计不一致？

可能是已删除但仍有进程占用的文件。使用 `lsof | grep deleted` 找到持有文件句柄的进程并重启该进程即可释放。

### Q3：Java 进程启动报 "Cannot allocate memory"？

检查系统内存 `free -h`，以及 ulimit 限制。执行 `ulimit -a` 查看当前限制，修改 `/etc/security/limits.conf` 可调整。

### Q4：修改了 `/etc/profile` 后环境变量不生效？

执行 `source /etc/profile` 或重新登录。注意登录 Shell 和非登录 Shell 读取的配置文件不同。

### Q5：systemd 服务启动失败如何排查？

```bash
systemctl status myapp          # 查看状态
journalctl -u myapp -n 50       # 查看最近 50 行日志
journalctl -u myapp --since "5min ago"  # 查看最近 5 分钟
systemctl cat myapp             # 查看 unit 文件内容
```

### Q6：`yum install` 报 "Could not resolve host"？

检查网络连通性和 DNS 配置：`cat /etc/resolv.conf` 确认 nameserver 配置正确。

---

**上一模块**：[02-Linux 常用命令](02-Linux-常用命令.md) ｜ **下一模块**：[04-Linux 网络与安全](04-Linux-网络与安全.md) ｜ **返回总览**：[00-知识体系总览](00-Linux知识体系总览.md)

**【参考来源】**
- systemd 官方文档：https://systemd.io/\n- man 手册：`man fstab` `man systemctl`
