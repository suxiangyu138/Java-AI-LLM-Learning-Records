# Ubuntu 基础入门与命令行

> 🐧 目录结构、终端操作、20+ 核心命令、vim 速查、SSH 远程连接 —— Java 开发者的 Ubuntu 第一步

---

## 📚 目录

1. [Linux 目录结构](#1-linux-目录结构)
2. [终端基础操作](#2-终端基础操作)
3. [核心命令速查](#3-核心命令速查)
4. [vim 极速上手](#4-vim-极速上手)
5. [SSH 远程连接](#5-ssh-远程连接)

---

## 1. Linux 目录结构

```text
/                     ← 根目录
├── /bin              ← 系统基础命令（ls, cat, cp）
├── /boot             ← 内核和启动文件
├── /dev              ← 设备文件（sda=硬盘, tty=终端）
├── /etc              ← 系统配置文件（nginx, ssh, hosts）⭐
├── /home             ← 用户家目录（/home/ubuntu）⭐
│   └── /home/ubuntu  ← ~ （你的目录！）
├── /opt              ← 第三方软件安装
├── /proc             ← 进程和内核信息（虚拟文件系统）
├── /root             ← root 用户家目录
├── /tmp              ← 临时文件（重启清空）
├── /usr              ← 用户软件（/usr/bin, /usr/lib）
│   └── /usr/local    ← 本地编译安装的软件
├── /var              ← 可变数据（日志、数据库）
│   ├── /var/log      ← 系统日志 ⭐
│   └── /var/www      ← Web 服务根目录
└── /srv              ← 服务数据

Windows 对比速记：
  C:\Program Files   → /usr, /opt
  C:\Windows         → /bin, /boot
  C:\Users           → /home
  C:\Windows\System32 → /etc
  D: 盘              → /mnt 下挂载
```

---

## 2. 终端基础操作

### 2.1 常用快捷键

| 快捷键 | 功能 | 说明 |
|--------|------|------|
| `Ctrl+C` | 中断当前命令 | 万能中断键 |
| `Ctrl+D` | EOF / 退出终端 | 退出 shell |
| `Ctrl+Z` | 挂起进程 | `fg` 恢复，`bg` 后台 |
| `Ctrl+L` | 清屏 | 同 `clear` |
| `Ctrl+R` | 搜索历史命令 | 极其实用！ |
| `Ctrl+A/E` | 移到行首/行尾 | 行编辑 |
| `Tab` | 自动补全 | 最常用的键 |
| `↑/↓` | 上/下一条命令 | 历史翻页 |

### 2.2 管道与重定向

```bash
# 管道 | ：将前一个命令的输出作为后一个命令的输入
ps aux | grep java              # 找 Java 进程
cat app.log | grep ERROR | wc -l  # 统计 ERROR 行数
history | tail -20              # 最近 20 条命令

# 重定向：
command > file      # 覆盖写入
command >> file     # 追加写入
command 2> file     # 错误输出重定向
command &> file     # 标准输出+错误都重定向
command < file      # 从文件读取输入

# 实战：
nohup java -jar app.jar > app.log 2>&1 &  # 后台运行 + 日志
```

---

## 3. 核心命令速查

### 3.1 文件与目录

| 命令 | 用途 | 常用示例 |
|------|------|---------|
| `ls` | 列出文件 | `ls -la`（详细信息）, `ls -lh`（可读大小） |
| `cd` | 切换目录 | `cd ~`, `cd ..`, `cd -`（返回上次） |
| `pwd` | 当前路径 | `pwd` |
| `cp` | 复制 | `cp -r dir1 dir2`（递归复制） |
| `mv` | 移动/重命名 | `mv old.txt new.txt` |
| `rm` | 删除 | `rm -rf dir`（⚠️ 危险！） |
| `mkdir` | 创建目录 | `mkdir -p a/b/c`（递归创建） |
| `touch` | 创建文件 | `touch file.txt` |
| `find` | 查找文件 | `find . -name "*.java"` |
| `ln` | 创建链接 | `ln -s /opt/app/bin app`（软链接） |

### 3.2 文本处理

| 命令 | 用途 | 示例 |
|------|------|------|
| `cat` | 查看文件 | `cat file.txt` |
| `less` | 分页查看 | `less app.log`（q 退出） |
| `head` | 查看头部 | `head -20 file.txt` |
| `tail` | 查看尾部 | `tail -f app.log`（实时跟踪日志！） |
| `grep` | 搜索文本 | `grep -i "error" app.log` |
| `wc` | 统计行/词/字 | `wc -l file.txt`（行数） |
| `sort` | 排序 | `sort -n file.txt` |
| `uniq` | 去重 | `sort file.txt \| uniq -c` |
| `awk` | 文本分析 | `awk '{print $1, $3}' file` |
| `sed` | 文本替换 | `sed 's/old/new/g' file` |

### 3.3 系统管理

| 命令 | 用途 | 示例 |
|------|------|------|
| `ps` | 查看进程 | `ps aux \| grep java` |
| `top/htop` | 实时监控 | `htop`（更友好） |
| `kill` | 终止进程 | `kill -9 PID` |
| `df` | 磁盘使用 | `df -h` |
| `du` | 目录大小 | `du -sh ./logs/` |
| `free` | 内存使用 | `free -h` |
| `netstat/ss` | 网络连接 | `ss -tlnp \| grep 8080` |
| `lsof` | 打开的文件 | `lsof -i :8080`（谁占用了 8080） |
| `chmod` | 修改权限 | `chmod +x script.sh` |
| `chown` | 修改所有者 | `chown ubuntu:ubuntu file` |

### 3.4 实用组合拳

```bash
# 查谁用了 8080 端口并杀掉
lsof -ti :8080 | xargs kill -9

# 按大小排序查看当前目录
du -sh * | sort -hr | head -10

# 查找大于 100MB 的文件
find / -type f -size +100M 2>/dev/null

# 实时监控 Java 进程
watch -n 2 'ps aux | grep java'

# 批量替换文件内容
find . -name "*.java" -exec sed -i 's/oldClass/newClass/g' {} \;

# 统计每个 IP 访问次数（Nginx 日志）
awk '{print $1}' access.log | sort | uniq -c | sort -rn | head -10
```

---

## 4. vim 极速上手

```text
三种模式：

  普通模式（默认） → 按 i/a/o 进入插入模式
  插入模式        → 按 Esc 回到普通模式
  命令模式        → 普通模式下按 : 进入（:w 保存, :q 退出）

必备操作（背诵级）：
  i        → 光标前插入
  a        → 光标后插入
  o        → 下一行插入
  Esc      → 回到普通模式
  :w       → 保存
  :q       → 退出
  :wq / ZZ → 保存退出
  :q!      → 不保存强制退出
  dd       → 删除一行
  yy       → 复制一行
  p        → 粘贴
  u        → 撤销
  /word    → 搜索 word
  :%s/old/new/g → 全局替换
  gg       → 跳到文件头
  G        → 跳到文件尾
```

---

## 5. SSH 远程连接

### 5.1 基础用法

```bash
# 密码登录
ssh user@192.168.1.100

# 指定端口
ssh -p 2222 user@192.168.1.100

# 密钥登录（推荐）
# 1. 生成本机密钥
ssh-keygen -t ed25519 -C "your_email@example.com"

# 2. 复制公钥到服务器
ssh-copy-id user@192.168.1.100

# 3. 之后无需密码登录
ssh user@192.168.1.100
```

### 5.2 SSH 配置简化

```bash
# ~/.ssh/config
Host myserver
    HostName 192.168.1.100
    User ubuntu
    Port 22
    IdentityFile ~/.ssh/id_ed25519

Host prod
    HostName prod.example.com
    User deploy
    Port 2222

# 简化后直接：
ssh myserver   → 等同 ssh ubuntu@192.168.1.100
scp file prod:~/app/  → 拷贝文件到生产环境
```

### 5.3 文件传输

```bash
# scp：本地 ↔ 远程
scp file.txt user@server:/remote/path/       # 上传
scp user@server:/remote/file.txt ./          # 下载
scp -r ./dir user@server:/remote/            # 递归上传目录

# rsync：增量同步（更快）
rsync -avz --progress ./target/app.jar user@server:~/app/
rsync -avz --exclude 'logs/' ./app/ user@server:~/app/
```

---

> 🎯 **核心要点**：`man 命令` 查手册，`Ctrl+R` 搜历史，`tail -f` 看日志，`grep` 搜内容，`|` 管道组合。前 20 个命令背熟就能完成 80% 日常工作。

---

*创建于：2026年7月*
