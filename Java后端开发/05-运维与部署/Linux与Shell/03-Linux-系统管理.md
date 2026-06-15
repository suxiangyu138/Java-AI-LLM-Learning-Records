# Linux 系统管理

## 磁盘管理

### 常用命令

| 命令 | 用途 | 示例 |
|------|------|------|
| `df -h` | 查看文件系统磁盘空间占用 | `df -h` — 人类可读格式 |
| `du -sh <dir>` | 统计目录总大小 | `du -sh /var/log` |
| `lsblk` | 列出块设备 | `lsblk` — 查看磁盘分区结构 |
| `fdisk -l` | 查看/管理分区表 | `sudo fdisk -l /dev/sda` |
| `mount` | 挂载文件系统 | `mount /dev/sdb1 /mnt/data` |
| `umount` | 卸载文件系统 | `umount /mnt/data` |

### 分区与挂载流程

```bash
# 1. 查看磁盘
lsblk
# 2. 分区（交互式）
sudo fdisk /dev/sdb
# 3. 格式化
sudo mkfs.ext4 /dev/sdb1
# 4. 临时挂载
sudo mount /dev/sdb1 /mnt/data
# 5. 永久挂载（写入 /etc/fstab）
echo '/dev/sdb1 /mnt/data ext4 defaults 0 0' | sudo tee -a /etc/fstab
```

### Java 后端实用场景

- **日志盘监控**：`df -h /var/log` 检查日志分区是否写满
- **大文件定位**：`du -sh /app/* | sort -rh | head -10` 找出项目目录中最大的文件
- **数据盘挂载**：MySQL/ES 等组件建议独立挂载数据盘到 `/data`，与系统盘隔离

---

## 进程管理

### 常用命令

| 命令 | 用途 | Java 开发常用场景 |
|------|------|------------------|
| `ps -ef` | 查看所有进程 | `ps -ef \| grep java` 查 Java 进程 |
| `ps aux` | 查看进程 CPU/内存 | `ps aux --sort=-%mem \| head` 查内存占用 top |
| `top -H -p <pid>` | 查看进程内线程 | 排查 Java 线程热点 |
| `kill <pid>` | 终止进程 | `kill -9 <pid>` 强制终止 |
| `nohup` | 后台运行（忽略 HUP） | `nohup java -jar app.jar > app.log 2>&1 &` |
| `jobs -l` | 查看当前 shell 后台任务 | |
| `bg` / `fg` | 前后台切换 | `Ctrl+Z` 暂停，`bg` 后台运行，`fg` 调回前台 |
| `htop` | 增强版 top（需安装） | 交互式进程管理 |

### 查找与终止 Java 进程

```bash
# 查找特定 Java 进程
ps -ef | grep spring-boot-app.jar | grep -v grep
# 使用 jps（JDK 自带）
jps -l
# 终止
kill -15 <pid>   # 优雅停止（推荐）
kill -9 <pid>    # 强制停止（最后手段）
```

### nohup 与后台运行

```bash
# 标准后台启动 Java 服务
nohup java -Xms512m -Xmx2g -jar app.jar --server.port=8080 > logs/app.log 2>&1 &

# 查看后台日志
tail -f logs/app.log

# 将暂停任务切到后台
# Ctrl+Z 暂停 → bg → disown -h
```

> **注意**：生产环境应使用 `systemd` 或容器编排管理进程，避免依赖 `nohup`。

---

## systemd 服务管理

### systemctl 常用命令

| 命令 | 用途 |
|------|------|
| `systemctl start <svc>` | 启动服务 |
| `systemctl stop <svc>` | 停止服务 |
| `systemctl restart <svc>` | 重启服务 |
| `systemctl enable <svc>` | 设置开机自启 |
| `systemctl disable <svc>` | 取消开机自启 |
| `systemctl status <svc>` | 查看服务状态 |
| `systemctl daemon-reload` | 重载 unit 文件 |
| `journalctl -u <svc> -f` | 查看服务日志 |

### 自定义 Java 服务 Unit

```ini
# /etc/systemd/system/myapp.service
[Unit]
Description=My Spring Boot Application
After=network.target

[Service]
Type=simple
User=appuser
WorkingDirectory=/opt/myapp
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar /opt/myapp/app.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# 部署后执行
sudo systemctl daemon-reload
sudo systemctl enable myapp
sudo systemctl start myapp
sudo systemctl status myapp
```

---

## 软件包管理

### RPM / YUM (RHEL/CentOS) 与 APT (Debian/Ubuntu)

| 操作 | YUM | APT |
|------|-----|-----|
| 更新仓库 | `yum update` | `apt update` |
| 安装 | `yum install <pkg>` | `apt install <pkg>` |
| 卸载 | `yum remove <pkg>` | `apt remove <pkg>` |
| 搜索 | `yum search <keyword>` | `apt search <keyword>` |
| 查看已安装 | `rpm -qa \| grep <pkg>` | `dpkg -l \| grep <pkg>` |
| 查看文件归属 | `rpm -qf <file>` | `dpkg -S <file>` |

### Java 环境安装

```bash
# CentOS / RHEL
sudo yum install -y java-17-openjdk-devel
# Ubuntu / Debian
sudo apt update
sudo apt install -y openjdk-17-jdk

# 验证
java -version
javac -version
```

### 本地 RPM 安装

```bash
# 下载后离线安装
sudo rpm -ivh package.rpm      # 安装
sudo rpm -Uvh package.rpm      # 升级
sudo rpm -e package-name       # 卸载
sudo rpm -ql package-name      # 查看安装路径
```

---

## 用户与权限

### 用户管理

| 命令 | 用途 | 示例 |
|------|------|------|
| `useradd` | 创建用户 | `sudo useradd -m -s /bin/bash appuser` |
| `passwd` | 设置密码 | `sudo passwd appuser` |
| `usermod` | 修改用户属性 | `sudo usermod -aG docker appuser` |
| `userdel` | 删除用户 | `sudo userdel -r appuser` |
| `id` | 查看用户 UID/GID | `id appuser` |
| `groups` | 查看用户组 | `groups appuser` |
| `who` / `w` | 查看登录用户 | |

### 文件权限

```bash
# 权限位：rwx r-x r-x  →  755
#         用户 组  其他
chmod 755 script.sh              # 设置权限
chmod u+x script.sh              # 仅给用户加执行权限
chown appuser:appuser app.jar    # 修改文件所属用户:组
chown -R appuser:appuser /opt/   # 递归修改

# 常用部署目录权限
sudo mkdir -p /opt/myapp/logs
sudo chown -R appuser:appuser /opt/myapp
sudo chmod 755 /opt/myapp
sudo chmod 644 /opt/myapp/app.jar
sudo chmod 775 /opt/myapp/logs
```

### sudo 配置

```bash
# 为 appuser 赋予无需密码的 sudo 权限
echo 'appuser ALL=(ALL) NOPASSWD: ALL' | sudo tee /etc/sudoers.d/appuser
```

---

## 环境变量

### 配置文件层级

| 文件 | 作用域 | 加载时机 |
|------|--------|----------|
| `/etc/environment` | 系统全局 | 登录时 |
| `/etc/profile` | 全局 Shell | 交互式登录 |
| `~/.bashrc` | 当前用户 | 交互式非登录 Shell |
| `~/.bash_profile` | 当前用户 | 交互式登录 |
| `/etc/profile.d/*.sh` | 全局扩展 | 被 /etc/profile 加载 |

### 常用环境变量设置

```bash
# /etc/profile.d/java.sh — Java 环境变量
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH

# 立即生效
source /etc/profile.d/java.sh
```

```bash
# 查看
echo $JAVA_HOME
env | grep JAVA
printenv

# 临时设置（仅当前 Shell 有效）
export MY_VAR=value
```

---

## 基础配置

### 主机名

```bash
hostnamectl set-hostname prod-api-01
hostnamectl status
```

### 时间同步

```bash
# 查看时间与时区
timedatectl status
# 设置时区
sudo timedatectl set-timezone Asia/Shanghai
# 启用 NTP 同步
sudo timedatectl set-ntp true
```

### 网络配置

```bash
# 查看网络接口
ip addr show
# 查看路由
ip route show
# DNS 配置（/etc/resolv.conf）
echo 'nameserver 8.8.8.8' | sudo tee -a /etc/resolv.conf
# 测试连通性
curl -I http://localhost:8080/actuator/health
```

### 防火墙 (firewalld)

```bash
# 开放 Java 服务端口
sudo firewall-cmd --zone=public --add-port=8080/tcp --permanent
sudo firewall-cmd --reload
# 查看开放端口
sudo firewall-cmd --list-all
```

### 内核参数调优（Java 服务常见调整）

```bash
# /etc/sysctl.conf
# 文件句柄上限
fs.file-max = 1000000
# 网络连接
net.ipv4.ip_local_port_range = 1024 65535
net.core.somaxconn = 1024
# 生效
sudo sysctl -p
# 查看当前值
sysctl fs.file-max
```

---

## 常见问题

**Q: 如何查看哪个进程占用了某个端口？**

```bash
sudo lsof -i :8080
sudo netstat -tlnp | grep 8080
ss -tlnp | grep 8080
```

**Q: 磁盘空间满但 `du` 统计不一致？**

可能是已删除但仍有进程占用的文件。使用 `lsof | grep deleted` 找到持有文件句柄的进程并重启。

**Q: Java 进程启动时报 "Out Of Memory" 或 "Cannot allocate memory"？**

检查系统内存 `free -h`，以及是否有 `ulimit` 限制。执行 `ulimit -a` 查看当前限制，修改 `/etc/security/limits.conf` 可调整。

**Q: 修改了 `/etc/profile` 后环境变量不生效？**

执行 `source /etc/profile` 或重新登录。注意不同 Shell 配置文件加载规则不同（`.bashrc` vs `.bash_profile`）。

**Q: `yum install` 报错 "Could not resolve host"？**

检查网络连通性和 DNS 配置。确认 `/etc/resolv.conf` 包含可用 nameserver。
