# Linux 面试宝典
> 基于课程大纲全面覆盖 Linux 面试高频考点，涵盖系统基础、命令操作、权限管理、网络配置、Shell 脚本、性能调优、软件部署及大数据集群运维

## 目录
1. [一、基础概念速答](#一基础概念速答12-18题)
2. [二、深度原理剖析](#二深度原理剖析8-12题)
3. [三、实战场景题](#三实战场景题6-10题)
4. [四、手写代码题](#四手写代码题5-8题)
5. [五、系统设计题](#五系统设计题3-5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板top-5高频题的结构化回答模板)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（12-18题）

### 1.1 Linux 的目录结构（Filesystem Hierarchy Standard）
| 目录 | 用途 | 说明 |
|------|------|------|
| `/` | 根目录 | 所有目录的起点 |
| `/bin` | 用户二进制命令 | `ls`, `cp`, `mv` 等（多为 /usr/bin 符号链接）|
| `/sbin` | 系统管理命令 | `fdisk`, `iptables`, `systemctl` |
| `/etc` | 配置文件 | `nginx.conf`, `my.cnf`, `passwd` |
| `/usr` | 用户程序和数据 | Unix System Resources 缩写 |
| `/var` | 可变数据 | 日志 `/var/log`、缓存、spool |
| `/tmp` | 临时文件 | 重启后清空 |
| `/home` | 用户家目录 | `/home/username` |
| `/root` | root 家目录 | 普通用户无权访问 |
| `/proc` | 虚拟文件系统 | 内存中的内核和进程信息（`/proc/meminfo`）|
| `/dev` | 设备文件 | 硬盘 `/dev/sda`、终端 `/dev/tty` |
| `/opt` | 第三方软件 | 可选安装路径 |
| `/mnt` | 挂载点 | 临时挂载文件系统 |
| `/boot` | 内核和引导文件 | vmlinuz, initramfs, grub |

> 💡 面试常问：`/etc` 存配置，`/var` 存日志，`/proc` 是虚拟文件系统，不占磁盘空间。

### 1.2 Linux 文件类型如何区分？
```bash
ls -l         # 第一个字符表示文件类型
-              # 普通文件
d              # 目录文件
l              # 符号链接文件
b              # 块设备文件（磁盘）
c              # 字符设备文件（终端）
s              # 套接字文件
p              # 管道文件
```
面试高频：输入 `ls -l` 第一列第 1 个字符表示文件类型，例如 `drwxr-xr-x` 的 `d` 表示目录。

### 1.3 Linux 和 Windows 的主要区别？
| 对比维度 | Linux | Windows |
|---------|-------|---------|
| 文件系统 | ext4, xfs, btrfs | NTFS, FAT32 |
| 路径分隔符 | `/` | `\` |
| 命令行 | Bash/Zsh（强大，可编程）| CMD/PowerShell |
| 权限模型 | rwx 三元组 + ACL | ACL |
| 软件管理 | apt/yum/dnf 包管理器 | exe/msi 安装包 |
| 开源 | 开源免费 | 商业收费 |
| 内核 | 宏内核（Monolithic Kernel）| 混合内核 |
| 多用户 | 天生多用户，远程 SSH 友好 | 侧重桌面单用户 |

### 1.4 什么是 inode？
inode（索引节点）是 Linux 文件系统中的元数据结构，存储文件的属性信息（权限、所有者、大小、时间戳、数据块指针），不包含文件名。文件名存放在目录项中，指向 inode。

```bash
stat test.txt          # 查看文件的 inode 信息
ls -i test.txt         # 查看文件的 inode 号码
df -i                  # 查看 inode 使用情况（inode 耗尽也会导致无法创建文件）
```
> ⚠️ 面试题：磁盘还有空间却无法创建文件？答：inode 用尽了，常见于大量小文件场景。

### 1.5 Linux 系统运行级别
| 运行级别 | Systemd Target | 说明 |
|---------|----------------|------|
| 0 | poweroff.target | 关机 |
| 1 | rescue.target | 单用户模式（救援模式）|
| 2 | multi-user.target | 多用户无网络（Debian）|
| 3 | multi-user.target | 多用户命令行模式 |
| 4 | multi-user.target | 未使用/自定义 |
| 5 | graphical.target | 图形化界面 |
| 6 | reboot.target | 重启 |

```bash
runlevel               # 查看当前运行级别
systemctl get-default  # 查看默认启动 target
systemctl set-default multi-user.target  # 设置默认命令行模式启动
```

### 1.6 硬链接和软链接的区别？
| 对比维度 | 硬链接 | 软链接（符号链接）|
|---------|--------|----------------|
| inode | 与原文件相同 | 不同，新的 inode |
| 跨文件系统 | 不支持 | 支持 |
| 链接目录 | 不支持 | 支持 |
| 原文件删除后 | 仍可访问 | 变成断链（dangling）|
| 命令 | `ln target link_name` | `ln -s target link_name` |

### 1.7 Linux 启动过程
```
BIOS/UEFI → MBR/GPT → GRUB2 → Kernel → initrd → systemd (PID 1) → 启动目标
```
详细步骤：① 加电自检（BIOS/UEFI）→ ② 读取引导加载器 GRUB2 → ③ 加载内核到内存 → ④ 加载 initramfs（临时根文件系统）→ ⑤ 挂载真正的根文件系统 → ⑥ systemd 启动并并行初始化服务 → ⑦ 启动 getty 提供登录提示。

### 1.8 什么是交换分区（Swap）？
Swap 是磁盘上的一块空间，当物理内存不足时，内核将不活跃的内存页换出到 Swap。Swap 可以是独立分区或交换文件。
```bash
swapon --show          # 查看当前 Swap 使用情况
free -h                # 查看内存和 Swap 总量/已用
```

### 1.9 简述 Linux 权限模型（ugo/rwx）
权限分为三组：所有者（u）、所属组（g）、其他人（o），每组有读（r=4）、写（w=2）、执行（x=1）三种权限。
```bash
chmod 755 file         # 所有者 rwx，组和其他人 rx
chmod u+x file         # 为所有者添加执行权限
chmod -R 644 dir/      # 递归设置目录下所有文件权限
```

### 1.10 Linux 中的特殊权限位
| 权限 | 符号 | 数值 | 作用 |
|------|------|------|------|
| SUID | `s` 在所有者 x 位 | 4xxx | 执行时以文件所有者身份运行（如 `/usr/bin/passwd`）|
| SGID | `s` 在组 x 位 | 2xxx | 执行时以文件所属组身份运行；目录中新建文件继承组 |
| Sticky Bit | `t` 在其他 x 位 | 1xxx | 仅文件所有者可删除（如 `/tmp`）|

```bash
chmod u+s /usr/bin/myapp   # 设置 SUID
chmod g+s /shared/dir      # 设置 SGID
chmod o+t /tmp              # 设置 Sticky Bit
```

### 1.11 Linux 主要文件系统类型
| 文件系统 | 适用场景 | 最大文件 | 最大卷 | 特性 |
|---------|---------|---------|-------|------|
| ext4 | 通用 Linux | 16TB | 1EB | 向后兼容、日志、extents |
| xfs | 大数据/高性能 | 8EB | 8EB | 高并发、大文件友好 |
| btrfs | 高级场景 | 16EB | 16EB | 写时复制、快照、压缩 |
| ZFS | 企业存储 | 16EB | 256ZB | 存储池、校验和、去重 |

### 1.12 什么是 umask？
umask 决定新建文件和目录的默认权限掩码。文件默认最大 666，目录 777，减去 umask 即得到实际权限。
```bash
umask        # 查看当前 umask（通常为 0022）
umask 0027   # 临时设置
# 文件：666 - 027 = 640
# 目录：777 - 027 = 750
```

---

## 二、深度原理剖析（8-12题）

### 2.1 Linux 进程调度算法
Linux 默认使用 **完全公平调度器（CFS, Completely Fair Scheduler）**，基于红黑树实现。

核心概念：
- **虚拟运行时间（vruntime）**：每个进程按权重分配 CPU 时间，CFS 选择 vruntime 最小的进程运行
- **nice 值**：范围 -20 到 19，越低优先级越高（权重越大）
- **时间片**：由 CFS 动态计算，目标延迟（targeted latency）内确保每个进程至少运行一次

```bash
nice -n -5 ./heavy-task   # 以更高优先级启动（nice -5）
renice -n 10 -p 1234      # 修改 PID 1234 的 nice 值
top                       # 按 P 键按 CPU 排序，按 M 按内存排序
```

### 2.2 进程状态与 Linux 0/1/2/D 状态
```bash
ps aux                     # 查看所有进程状态
# R: Running/Runnable 运行或就绪
# S: Sleeping 可中断睡眠（等待事件）
# D: Uninterruptible Sleep 不可中断睡眠（等待 I/O，常见于磁盘/网络）
# Z: Zombie 僵尸进程（已终止但父进程未回收）
# T: Stopped 暂停（Ctrl+Z 或 SIGSTOP）
# X: Dead 死亡（几乎看不到）
```

> ⚠️ **D 状态进程**：不可中断睡眠，不能被 kill 杀掉，通常等待磁盘 I/O。如果大量 D 状态进程堆积，说明磁盘 I/O 瓶颈或 NFS 挂载阻塞。
>
> **僵尸进程**：子进程终止后，父进程未调用 `wait()` 回收其 PCB，导致进程表项残留。父进程退出后由 init（PID 1）继承回收。

### 2.3 fork() 和 exec() 的工作原理
```c
pid_t pid = fork();      // 创建子进程：复制父进程的 PCB、内存、文件描述符
if (pid == 0) {
    // 子进程
    execlp("/bin/ls", "ls", NULL);  // 替换子进程为 ls 程序
} else {
    // 父进程
    wait(NULL);          // 等待子进程结束
}
```

**fork()**: 写时复制（Copy on Write），子进程共享父进程的物理内存页，直到写入时才复制。
**exec()**: 完全替换当前进程的代码段、数据段、堆、栈，PID 不变。

### 2.4 Linux 内存管理核心概念
| 概念 | 说明 |
|------|------|
| **虚拟内存** | 每个进程有独立 4GB 虚拟地址空间（32位），通过 MMU 映射到物理内存 |
| **页表** | 多级页表（4级：PGD → PUD → PMD → PTE）保存虚拟页到物理页帧的映射 |
| **缺页中断** | 访问未映射虚拟页时触发，内核加载对应数据 |
| **OOM Killer** | 内存耗尽时，内核选择并杀死占用最多的进程释放内存 |
| **Slab 分配器** | 为内核对象（如 inode, task_struct）提供缓存分配 |
| **Swap** | 将不活跃内存页换出到磁盘，扩展可用内存 |

```bash
cat /proc/meminfo        # 查看详细内存信息
vmstat 1                 # 每秒显示内存、swap、I/O、CPU 情况
top -o %MEM              # 按内存使用率排序
```

### 2.5 I/O 多路复用：select/poll/epoll 对比
| 指标 | select | poll | epoll |
|------|--------|------|-------|
| 文件描述符上限 | 1024（FD_SETSIZE）| 无限制 | 无限制 |
| 遍历方式 | 轮询所有 fd | 轮询所有 fd | 回调通知 |
| 触发模式 | 水平触发 | 水平触发 | 水平 + 边缘触发 |
| 效率 | O(n) | O(n) | O(1)（活跃 fd 少时）|
| 跨平台 | 大多数 Unix | 大多数 Unix | 仅 Linux |
| 实现 | 位图 | 链表 | 红黑树 + 就绪链表 |

> 💡 阿里高频题：epoll 为什么高效？答：① 红黑树管理 fd，增删 O(log n)；② 就绪链表避免全量遍历；③ mmap 共享内核和用户态数据，减少拷贝。

### 2.6 零拷贝技术（Zero-Copy）
传统文件发送：磁盘 → 内核缓冲区 → 用户缓冲区 → Socket 缓冲区 → 网卡（4 次拷贝 + 4 次上下文切换）。

零拷贝方案：
- **sendfile()**: 磁盘 → 内核缓冲区 → Socket 缓冲区 → 网卡（2 次拷贝 + 2 次切换）
- **mmap**: 用户态和内核态共享同一块物理内存，绕过了用户缓冲区拷贝
- **splice()**: 在两个文件描述符之间直接移动数据

```bash
# Nginx 启用 sendfile
sendfile on;             # nginx.conf 配置
```

### 2.7 Buffer Cache 和 Page Cache 的区别？
| 缓存 | 作用 | 存储内容 |
|------|------|---------|
| Page Cache | 文件系统缓存 | 文件内容页，加速文件读写 |
| Buffer Cache | 块设备缓存 | 原始块设备元数据（superblock, inode）|

- Page Cache 缓存文件数据（如读取的 .txt 内容）
- Buffer Cache 缓存文件系统元数据（如 inode 信息）
- 两者在 2.4+ 内核中统一管理，`free` 命令中的 `buff/cache` 合并显示

```bash
free -h                  # 查看 buff/cache 使用量
echo 3 > /proc/sys/vm/drop_caches  # 清空缓存（生产谨慎）
```

### 2.8 Linux 中断处理：上半部和下半部
- **上半部（Top Half）**：在中断上下文中执行，响应硬件中断，要求快速完成，只能做最紧急的工作（清除中断标志、拷贝数据到缓冲区）
- **下半部（Bottom Half）**：在软中断上下文中执行，处理耗时逻辑，可以被中断。三种机制：软中断、tasklet、工作队列

```bash
cat /proc/interrupts     # 查看系统中断统计
cat /proc/softirqs       # 查看软中断统计
```

> 💡 面试题：为什么中断要分上下半部？答：中断处理时 CPU 关闭中断（或关闭同级中断），如果处理太久会丢失其他中断。上半部快速处理硬件响应，下半部延后完成数据处理工作。

---

## 三、实战场景题（6-10题）

### 3.1 如何排查 CPU 负载过高？
```bash
# 步骤 1：定位高 CPU 进程
top                       # 按 P 键按 CPU 排序，记录高 CPU 的 PID

# 步骤 2：查看进程中的线程
top -H -p <PID>           # 查看进程中哪个线程消耗 CPU

# 步骤 3：将线程 ID 转为十六进制
printf "%x\n" <THREAD_ID>

# 步骤 4：抓取线程堆栈分析
jstack <PID> | grep -A 30 <HEX_TID>   # Java 应用
strace -p <PID> -c                     # 查看系统调用统计
```

### 3.2 如何排查内存泄漏和 OOM？
```bash
# 查看内存使用概况
free -h                   # Mem: total/used/free, Swap 使用情况
cat /proc/meminfo         # MemTotal, MemFree, MemAvailable, Cached

# 查看进程内存占用
ps aux --sort=-%mem | head -10   # 内存占用 TOP 10
top -o %MEM                       # 交互式按内存排序

# 查看 OOM 记录
dmesg | grep -i oom        # 内核日志中的 OOM Killer 记录
journalctl -k | grep oom   # systemd 内核日志

# 分析进程内存映射
pmap -x <PID>              # 查看进程详细内存分布
cat /proc/<PID>/status     # 查看 VmRSS, VmSize, VmPeak
```

### 3.3 磁盘 I/O 高负载如何排查？
```bash
# 步骤 1：用 iostat 看整体磁盘 I/O
iostat -x 1 5              # 每 1 秒输出一次，共 5 次
# %util 接近 100% 说明磁盘饱和
# await > 10ms 说明 I/O 延迟高（机械盘更高）
# r/s, w/s 每秒读写请求数

# 步骤 2：用 iotop 查哪个进程在大量 I/O
iotop                      # 类似 top 但监控 I/O，需要 root

# 步骤 3：查看 I/O 队列
cat /sys/block/sda/queue/scheduler   # 查看 I/O 调度器
# cfq: 完全公平队列，适合机械盘
# deadline: 截止时间调度，数据库场景
# noop: 先进先出，适合 SSD

# 步骤 4：用 sar 观察历史 I/O 趋势
sar -d -f /var/log/sa/saYYMMDD | head -20
```

### 3.4 服务器不能上网，如何排查网络故障？
```bash
# 第 1 层：物理链路
ip link show              # 查看网卡状态，UP/DOWN
ethtool eth0              # 查看网卡速度、双工模式

# 第 2 层：IP 配置
ip addr show              # 查看 IP 地址配置
ping -c 4 8.8.8.8         # 测试外网连通性

# 第 3 层：路由
ip route show             # 查看默认网关
traceroute 8.8.8.8        # 追踪路由路径

# 第 4 层：DNS 解析
nslookup baidu.com        # 测试 DNS 解析
cat /etc/resolv.conf      # 查看 DNS 配置

# 第 5 层：防火墙策略
iptables -L -n -v         # 查看 iptables 规则
firewall-cmd --list-all   # firewalld 规则
```

### 3.5 如何排查端口被占用或服务无法启动？
```bash
# 查找端口占用
ss -tlnp | grep :8080     # 查看谁在监听 8080（推荐 ss，性能更好）
netstat -tlnp | grep 8080 # 传统方式

# 查看进程监听的所有端口
lsof -i :8080             # 查看端口对应的进程

# 服务无法启动排查步骤
systemctl status mysqld   # 查看服务状态和错误信息
journalctl -u mysqld -e   # 查看服务单元日志
/usr/sbin/mysqld --verbose --help  # 手动启动测试
tail -100 /var/log/mysqld.log      # 查看应用日志
```

### 3.6 如何查找和清理大文件？
```bash
# 查找大文件
find / -type f -size +100M -exec ls -lh {} \;  # 查找 >100MB 的文件
du -sh /* | sort -rh | head -10                  # 统计根目录下各子目录大小
du -sh /var/log/* | sort -rh | head -5           # 检查日志大小

# 清理方式
> /var/log/nginx/access.log   # 清空日志文件（不删除文件）
find /tmp -type f -atime +7 -delete              # 删除 7 天未访问的临时文件
journalctl --vacuum-time=7d                      # 清理 systemd 日志（保留 7 天）

# 找占用文件描述符但已删除的文件（磁盘空间未释放）
lsof | grep '(deleted)'       # grep deleted 标记的文件
# 这些文件已被删除但仍有进程持有 fd，需重启对应进程才能释放
```

### 3.7 如何分析 Nginx 访问日志中的异常请求？
```bash
# 统计访问量最高的 IP
awk '{print $1}' /var/log/nginx/access.log | sort | uniq -c | sort -rn | head -10

# 统计请求最多的 URL
awk '{print $7}' /var/log/nginx/access.log | sort | uniq -c | sort -rn | head -10

# 统计 HTTP 状态码分布
awk '{print $9}' /var/log/nginx/access.log | sort | uniq -c | sort -rn

# 查找 5xx 错误
grep ' 5[0-9][0-9] ' /var/log/nginx/access.log | tail -50

# 统计每小时请求量
awk '{print $4}' /var/log/nginx/access.log | cut -d: -f2 | sort | uniq -c | sort -rn | head -24
```

### 3.8 MySQL 数据库 CPU 飙升如何排查？
```bash
# 步骤 1：定位 MySQL 进程
top                      # 找到 mysqld PID

# 步骤 2：查看 MySQL 内正在运行的 SQL
mysql -uroot -p -e "SHOW FULL PROCESSLIST\G"    # 查看当前连接和执行的 SQL

# 步骤 3：抓取 MySQL 线程堆栈
pstack <MYSQL_PID>        # 查看 MySQL 内部调用栈

# 步骤 4：慢查询分析
tail -100 /var/log/mysql/mysql-slow.log         # 分析慢查询 SQL
# 配置 /etc/my.cnf: slow_query_log=1, long_query_time=2

# 步骤 5：查看 InnoDB 状态
mysql -uroot -p -e "SHOW ENGINE INNODB STATUS\G"  # 分析锁等待和事务
```

---

## 四、手写代码题（5-8题）

### 4.1 写一个 Shell 脚本，检查系统健康状态
```bash
#!/bin/bash
# 系统健康检查脚本
# 检查 CPU 负载、内存使用、磁盘使用、进程数

CPU_LOAD=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d% -f1)
MEM_USED=$(free -m | awk '/Mem:/ {print $3}')
MEM_TOTAL=$(free -m | awk '/Mem:/ {print $2}')
DISK_USAGE=$(df -h / | awk 'NR==2 {print $5}' | cut -d% -f1)

echo "===== 系统健康检查 ====="
echo "主机名: $(hostname)"
echo "CPU 使用率: ${CPU_LOAD}%"
echo "内存使用: ${MEM_USED}MB / ${MEM_TOTAL}MB"

if [ "$DISK_USAGE" -gt 80 ]; then
    echo "⚠️ 警告: 根分区使用率已达 ${DISK_USAGE}%"
else
    echo "✅ 磁盘使用率: ${DISK_USAGE}%"
fi

# 检查关键进程
for proc in nginx mysqld redis-server; do
    if pgrep -x "$proc" > /dev/null; then
        echo "✅ $proc 运行正常"
    else
        echo "❌ $proc 未运行"
    fi
done
```

### 4.2 写一个 Shell 脚本，批量重命名指定目录下的文件
```bash
#!/bin/bash
# 批量重命名脚本：将所有 .txt 文件改为 .bak 后缀
# 用法：./rename.sh /path/to/dir

TARGET_DIR="${1:-.}"
if [ ! -d "$TARGET_DIR" ]; then
    echo "错误: 目录 $TARGET_DIR 不存在"
    exit 1
fi

count=0
for file in "$TARGET_DIR"/*.txt; do
    [ -f "$file" ] || continue
    mv "$file" "${file%.txt}.bak"
    echo "重命名: $file -> ${file%.txt}.bak"
    ((count++))
done

echo "完成: 共重命名 $count 个文件"
```

### 4.3 写一个 Shell 脚本，统计日志中 ERROR 出现的次数并按小时聚合
```bash
#!/bin/bash
# 日志错误统计脚本

LOG_FILE="${1:-/var/log/app.log}"

if [ ! -f "$LOG_FILE" ]; then
    echo "错误: 日志文件 $LOG_FILE 不存在"
    exit 1
fi

echo "===== ERROR 每小时统计 ====="
# 假设日志格式: 2024-01-15 14:23:45 [ERROR] xxx
grep "ERROR" "$LOG_FILE" | awk '{print $1, $2}' | \
    awk -F'[: ]' '{print $1, $2, $3}' | \
    sort | uniq -c | sort -rn | head -20

echo ""
echo "===== 错误类型 TOP 10 ====="
grep "ERROR" "$LOG_FILE" | \
    sed 's/.*\[ERROR\] //' | \
    awk '{print $1}' | \
    sort | uniq -c | sort -rn | head -10

echo "总 ERROR 数: $(grep -c 'ERROR' "$LOG_FILE")"
```

### 4.4 写一个 Shell 脚本，监控目录变化并记录
```bash
#!/bin/bash
# 使用 inotify 监控目录文件变化
# 依赖: yum install inotify-tools

WATCH_DIR="${1:-/data}"
LOG_FILE="/var/log/dir_monitor.log"

if ! command -v inotifywait &> /dev/null; then
    echo "请先安装 inotify-tools"
    exit 1
fi

inotifywait -m -r -e create,delete,modify,move "$WATCH_DIR" --format \
    '%T %w%f %e' --timefmt '%Y-%m-%d %H:%M:%S' | while read event; do
    echo "$event" >> "$LOG_FILE"
    echo "[$(date)] $event"
done
```

### 4.5 写一个 Shell 函数，解析 nginx 日志提取关键指标
```bash
#!/bin/bash
# 函数: 解析 nginx access log
# 用法: parse_nginx_log /var/log/nginx/access.log

parse_nginx_log() {
    local log_file="$1"
    [ -f "$log_file" ] || { echo "文件不存在"; return 1; }

    echo "========== Nginx 日志分析 =========="
    echo "1. 总请求数: $(wc -l < "$log_file")"

    echo -e "\n2. HTTP 状态码分布:"
    awk '{print $9}' "$log_file" | sort | uniq -c | sort -rn | \
        awk '{printf "   %s: %d\n", $2, $1}'

    echo -e "\n3. 独立 IP 数:"
    awk '{print $1}' "$log_file" | sort -u | wc -l

    echo -e "\n4. 请求量 TOP 10 URL:"
    awk '{print $7}' "$log_file" | sort | uniq -c | sort -rn | head -10

    echo -e "\n5. 请求量 TOP 10 IP:"
    awk '{print $1}' "$log_file" | sort | uniq -c | sort -rn | head -10

    echo -e "\n6. 平均响应时间:"
    awk '{sum+=$NF} END {printf "   %.2f ms\n", sum/NR}' "$log_file"
}

# 调用示例
# parse_nginx_log /var/log/nginx/access.log
```

### 5.6 写一个脚本定时备份数据库并清理 7 天前的备份
```bash
#!/bin/bash
# 数据库备份脚本
# 添加到 crontab: 0 2 * * * /root/scripts/backup_db.sh

BACKUP_DIR="/data/backup/mysql"
DB_USER="root"
DB_PASS="yourpassword"
DB_NAME="mydb"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=7

# 创建备份目录
mkdir -p "$BACKUP_DIR"

# 执行备份
mysqldump -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" | \
    gzip > "$BACKUP_DIR/${DB_NAME}_${DATE}.sql.gz"

# 检查备份是否成功
if [ $? -eq 0 ]; then
    echo "[$(date)] 备份成功: ${DB_NAME}_${DATE}.sql.gz"
    echo "文件大小: $(du -h "$BACKUP_DIR/${DB_NAME}_${DATE}.sql.gz" | cut -f1)"
else
    echo "[$(date)] 备份失败!" >&2
    exit 1
fi

# 清理 7 天前的备份
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -mtime +$RETENTION_DAYS \
    -exec rm {} \; -exec echo "删除过期备份: {}" \;
```

---

## 五、系统设计题（3-5题）

### 5.1 设计一个日志采集与分析系统
**需求**：100 台服务器，每台产生 500MB/天日志，实时采集、存储、查询。

**架构设计**：
```
应用服务器 (Filebeat/Loki) → Kafka → Logstash/Fluentd → Elasticsearch → Kibana
                                         ↓
                                    HDFS (冷存储, 7天转储)
```
| 组件 | 选型 | 职责 |
|------|------|------|
| 采集器 | Filebeat / Promtail | 轻量级，tail 日志文件发送到 Kafka |
| 消息队列 | Kafka | 削峰填谷，保证数据不丢，分区存储 |
| 处理器 | Logstash / Fluentd | 解析、过滤、格式化日志 |
| 存储 | Elasticsearch | 全文索引，支持实时搜索和聚合 |
| 冷存储 | HDFS | 超过 7 天的日志压缩存储 |
| 可视化 | Kibana / Grafana | 图表展示、告警配置 |
| 监控 | Prometheus + Alertmanager | 采集节点健康状态并告警 |

**面试关键点**：
- 高可用：Kafka 三副本、ES 多节点、避免单点
- 数据可靠性：Kafka 确认机制（acks=all）、端到端至少一次语义
- 性能优化：日志批量发送（batch size）、ES 索引分片策略（按天建索引）

### 5.2 设计一个高可用 Web 集群架构
**需求**：日均百万 PV，要求 99.9% 可用，支持弹性伸缩。

**架构**：
```
用户 → DNS 轮询/智能解析
  → Nginx 集群（Keepalived + VIP + 多节点负载均衡）
    → Tomcat/Spring Boot 应用集群（至少 2 台）
      → Redis 集群（缓存 + Session 共享）
      → MySQL 主从（读写分离）/ MySQL Cluster
```
| 层次 | 技术选型 | 高可用方案 |
|------|---------|-----------|
| DNS | 多线路解析 + TTL 60s | 主备域名解析 |
| 反向代理 | Nginx + Keepalived | 虚拟 IP 漂移，主备切换 |
| 应用层 | Spring Boot + Nacos | 多节点部署，健康检查自动摘除 |
| 缓存 | Redis Sentinel/Cluster | 哨兵模式自动故障转移 |
| 数据库 | MySQL MGR / PXC | 多主写入，自动选主 |

**自动化部署**：Jenkins + GitLab + Docker + Kubernetes，新代码滚动更新。

### 5.3 设计一个大数据离线数仓平台
**需求**：每天 10TB 数据接入，支持批处理和即席查询。

**架构**：
```
数据源 (MySQL Binlog / 日志 / API) → Flume/Kafka
  → Flink (实时 ETL) / Spark (批处理)
    → HDFS (原始数据存储)
      → Hive (ETL + 数仓分层: ODS → DWD → DWS → ADS)
        → Presto/Trino (即席查询引擎)
          → Superset/Tableau (可视化)
```
| 集群组件 | 节点数 | 配置 |
|---------|--------|------|
| HDFS NameNode | 2（主备）| 32C/64G |
| HDFS DataNode | 10 | 16C/64G + 12×4TB |
| Yarn ResourceManager | 2（主备）| 32C/64G |
| Yarn NodeManager | 10 | 16C/64G |
| Hive Metastore | 2（主备）| 16C/32G |
| Kafka | 5 | 16C/64G + 4×1TB NVMe |

> 💡 面试常问：数仓分层 ODS/DWD/DWS/ADS 各层用途、Hive 分区和分桶的区别、小文件合并策略。

### 5.4 设计一个秒杀系统的 Linux 部署方案
**需求**：整点秒杀活动，并发 10 万 QPS，需提前扩容和防刷。

**部署架构**：
```
CDN (静态资源加速)
  → Nginx + Lua (限流, WAF, IP 黑名单)
    → 网关层 (Kong/Sentinel, 限流降级)
      → 秒杀服务集群 (K8s + HPA 自动扩缩容)
        → Redis 预减库存 + 标记抢购资格
          → RocketMQ/Kafka 异步下单
            → MySQL 落单
```
**Linux 内核调优**：
```bash
# /etc/sysctl.conf 优化参数
net.ipv4.tcp_tw_reuse = 1          # 复用 TIME_WAIT 连接
net.ipv4.tcp_fin_timeout = 15       # 减少 FIN_WAIT 超时
net.core.somaxconn = 65535          # 最大半连接队列
net.ipv4.tcp_max_syn_backlog = 65535 # SYN 队列大小
net.core.netdev_max_backlog = 65535  # 网卡接收队列
# 文件描述符上限
ulimit -n 1000000                   # 全局文件句柄数
```

---

## 六、常见坑点与最佳实践

| 类别 | 坑点 | 原因 | 解决方案 |
|------|------|------|---------|
| **磁盘** | `df -h` 看到磁盘还有空间却无法写入 | inode 用尽（大量小文件）| 使用 `df -i` 检查 inode，定期清理小文件或临时文件 |
| **磁盘** | 删除文件后磁盘空间未释放 | 文件被进程打开持有 fd | `lsof \| grep '(deleted)'` 找到进程，重启或 `> /proc/<PID>/fd/<N>` |
| **内存** | `free -m` 显示 used 很高但应用内存并不高 | PageCache 缓存占用被认为是 used | 查看 `available` 列才是真正可用内存 |
| **网络** | 大量 TIME_WAIT 连接 | 短连接频繁建立/关闭，TCP 主动关闭方等待 2MSL | 开启 `net.ipv4.tcp_tw_reuse` 和 `tcp_tw_recycle`（内核≥4.12 已移除）|
| **进程** | `kill -9` 杀不掉进程 | 进程处于 D 状态（Uninterruptible Sleep）| 等待 I/O 完成，或修复底层存储/网络问题后通常自动恢复 |
| **权限** | 脚本用 `./script.sh` 报 Permission denied | 没有执行权限 | `chmod +x script.sh` 或 `bash script.sh` |
| **Shell** | `rm -rf /*` 误删根目录 | 变量为空导致路径无效 | 使用 `rm -rf /var/log/${app:-backup}/*` 加默认值保护，或先 `ls` 确认 |
| **Shell** | `find -exec rm {} \;` 性能极差 | 每个文件启动一个 rm 进程 | 使用 `find ... -delete` 或 `find ... \| xargs rm -f` |
| **系统** | `crontab` 定时任务不执行 | 环境变量问题（PATH 不包含命令路径）| 脚本开头显式设置 `PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin` |
| **系统** | 系统启动时服务启动慢 | systemd 依赖等待超时 | `systemctl list-dependencies <service>` 检查依赖链，减少无效依赖 |
| **网络** | scp 传输大文件断连重传费时 | 网络不稳定导致连接中断 | 改用 `rsync --partial --progress` 支持断点续传 |
| **日志** | journalctl 日志占用大量磁盘 | systemd 日志无限增长 | `journalctl --vacuum-size=500M` 限制大小，或配置 `SystemMaxUse=500M` |
| **安全** | 使用 root 用户运行所有服务 | 安全风险极大，权限过大 | 为每个服务创建独立系统用户，遵循最小权限原则 |
| **内核** | `echo 1 > /proc/sys/net/ipv4/ip_forward` 重启丢失 | 临时修改不持久化 | 写入 `/etc/sysctl.conf` 然后 `sysctl -p` 持久化配置 |

---

## 七、面试回答模板（Top 5 高频题的结构化回答模板）

### 7.1 请你介绍一下 Linux 的进程管理机制
**回答框架**：
1. **进程定义**：进程是程序的一次执行实例，包含代码段、数据段、堆栈和 PCB（Process Control Block）。
2. **进程状态**：[R] Running, [S] Sleeping, [D] Uninterruptible Sleep, [Z] Zombie, [T] Stopped。
3. **调度算法**：Linux 使用 CFS 完全公平调度器，基于 vruntime 选择优先级最高的进程。
4. **常用命令**：`ps aux` 查看进程列表，`top` 实时监控，`kill -9` 强制终止。
5. **关键机制**：写时复制（fork 优化）、OOM Killer（内存耗尽保护）。
> **加分项**：提及 `/proc` 文件系统、nice 值调整优先级、`cgroups` 资源限制。

### 7.2 Linux 文件权限如何管理？chmod 755 的含义？
**回答框架**：
1. **权限分类**：读（r=4）、写（w=2）、执行（x=1），分为三组（ugo）。
2. **数值计算**：`755` 分解为所有者 7（4+2+1=rwx）、组 5（4+1=rx）、其他人 5（rx）。
3. **查看权限**：`ls -l` 输出第一列 `-rwxr-xr-x`，首位表示文件类型。
4. **修改命令**：`chmod 755 file` 数值法，`chmod u+x,g-w file` 符号法。
5. **默认权限**：`umask` 决定新建文件和目录的默认权限（通常 022，文件 644 目录 755）。
> **加分项**：SUID/SGID/Sticky Bit 特殊权限位，ACL 扩展权限控制。

### 7.3 Linux 网络连接状态有哪些？大量 TIME_WAIT 如何优化？
**回答框架**：
1. **TCP 状态**：ESTABLISHED（已建立）、TIME_WAIT（主动关闭后等待 2MSL）、CLOSE_WAIT（被动关闭等待应用关闭）、FIN_WAIT1/2 等。
2. **TIME_WAIT 原因**：服务器作为主动关闭方，高并发短连接场景下大量出现。
3. **优化方案**：① `net.ipv4.tcp_tw_reuse = 1`（复用 TIME_WAIT 连接）；② 开启长连接复用；③ 使用连接池（如 HTTP 连接池、Druid 连接池）；④ 调小 `net.ipv4.tcp_fin_timeout`。
4. **排查命令**：`ss -tan | awk '{print $1}' | sort | uniq -c` 统计连接状态分布。
> **加分项**：区分 CLOSE_WAIT（应用未 close socket）和 TIME_WAIT（内核 2MSL 等待），CLOSE_WAIT 一般是代码 bug。

### 7.4 Linux 中如何排查服务器性能瓶颈？
**回答框架**（USE 方法论）：
1. **CPU 瓶颈**：`top` 看 us/sy/wa/id 指标；`mpstat -P ALL 1` 看每个核；`vmstat 1` 看上下文切换（cs）和运行队列（r）。
2. **内存瓶颈**：`free -h` 看 total/used/available；`vmstat` 的 si/so 表示 Swap 换入换出，非零则内存不足。
3. **磁盘 I/O 瓶颈**：`iostat -x 1` 看 `%util`（利用率）、`await`（响应时间）、`r/s w/s`（IOPS）。
4. **网络瓶颈**：`sar -n DEV 1` 看网卡吞吐；`iftop` 看实时带宽；`ss -s` 看连接数统计。
5. **定位具体进程**：`pidstat` 按进程查看 CPU/内存/I/O，`strace -p PID -c` 分析系统调用。
> **加分项**：sar 查看历史趋势；perf 采样分析热点函数；bcc/ebpf 工具链。

### 7.5 如何实现 Linux 系统自动化部署和运维？
**回答框架**：
1. **配置管理**：使用 Ansible（无代理 SSH 执行）或 SaltStack 批量管理配置，Playbook 声明式管理。
2. **持续集成**：GitLab CI / Jenkins Pipeline 自动编译打包，Docker 镜像构建。
3. **容器化部署**：Kubernetes 编排容器，Deployment 声明式更新，HPA 自动伸缩。
4. **监控告警**：Prometheus + Grafana 监控指标采集展示，Alertmanager 告警推送。
5. **日志管理**：Filebeat → Elasticsearch → Kibana 集中式日志分析。
6. **备份策略**：crontab 定时脚本 + rsync 增量同步 + 异地容灾。
> **加分项**：云原生（Terraform 基础设施即代码）、GitOps（ArgoCD 声明式部署）、不可变基础设施。

---

## 八、快速查漏补缺 Checklist

### 基础命令
- [ ] `ls -la`、`cd`、`pwd`、`mkdir -p`、`touch` 基础文件操作
- [ ] `cat`、`more`、`less`、`head`、`tail -f` 文件查看
- [ ] `cp -r`、`mv`、`rm -rf`、`find` 文件管理
- [ ] `tar -czvf`、`tar -xzvf`、`zip`、`unzip` 压缩解压
- [ ] `grep -E`、`sed`、`awk`、`sort`、`uniq`、`cut` 文本处理

### 权限管理
- [ ] `chmod` 数值法和符号法
- [ ] `chown user:group file`
- [ ] SUID、SGID、Sticky Bit 理解和设置
- [ ] `umask` 默认权限控制

### 系统管理
- [ ] `systemctl start/stop/enable/disable/status`
- [ ] `journalctl -u -f --since`
- [ ] `crontab -e` 五字段语法
- [ ] `df -h`、`du -sh`、`iostat`、`iostat -x`
- [ ] `free -h`、`vmstat 1`、`top`、`htop`
- [ ] `ps aux`、`ps -ef`、`kill`、`killall`、`pkill`

### 网络管理
- [ ] `ip addr`、`ip route`、`ip link`
- [ ] `ss -tlnp`、`netstat -tlnp`
- [ ] `ping`、`traceroute`、`curl`、`wget`
- [ ] `ssh`、`scp`、`rsync`
- [ ] `iptables -L -n -v`、`firewall-cmd`

### Shell 脚本
- [ ] `#!/bin/bash`、`$0 $1 $# $?` 基础变量
- [ ] `if-then-elif-else`、`for`、`while`、`case`
- [ ] 函数定义：`func() { ... }`
- [ ] `exit` 码、`&&` `||` 短路

### 软件部署
- [ ] yum/apt 包管理
- [ ] MySQL 安装（RPM 包 / 通用二进制 / Docker）
- [ ] Nginx 配置（`location`、`upstream`、反向代理、SSL）
- [ ] Redis 主从 / Sentinel / Cluster
- [ ] RabbitMQ 安装和集群
- [ ] Elasticsearch 单机和集群部署
- [ ] Zookeeper 集群
- [ ] Kafka 集群配置

### 性能调优
- [ ] USE 方法论（Utilization、Saturation、Errors）
- [ ] iostat 指标解读（await、svctm、%util）
- [ ] vmstat 指标解读（r、b、si、so、us、sy、wa）
- [ ] /etc/sysctl.conf 常见优化参数
- [ ] ulimit 调整文件句柄和进程限制

### 大数据集群
- [ ] Hadoop HDFS 架构（NameNode/DataNode/SecondaryNameNode）
- [ ] Yarn 资源调度（ResourceManager/NodeManager/Container）
- [ ] HBase 架构（HMaster/RegionServer/HFile/MemStore）
- [ ] Spark 运行模式（Standalone/Yarn/K8s）
- [ ] Flink 部署架构（JobManager/TaskManager）
- [ ] 集群时间同步（ntp/chrony）、SSH 免密配置、JDK 版本一致性

---

> **参考课程**：[36-Linux零基础教程](../06-课程思路体系搭建/36-Linux零基础教程.md)
> **更新日期**：2026-07-22
