# Linux 核心技能

## 📌 课程定位
Linux是服务端的绝对主流操作系统。不管后端开发、运维、AI训练，你几乎不可能绕开Linux。命令行熟练度和Shell脚本能力直接影响日常开发效率。

## 🎯 核心章节

### 1. Linux 基础
- **Linux 发行版**：RedHat系(CentOS/Rocky/Fedora)、Debian系(Ubuntu/Debian)
- **文件系统层次结构（FHS）**：
  - `/bin`、`/sbin`：核心可执行文件
  - `/etc`：配置文件
  - `/var`：日志、缓存等可变数据
  - `/home`：用户主目录
  - `/tmp`：临时文件(重启清空)
  - `/proc`、`/sys`：内核和进程信息(伪文件系统)
  - `/opt`：第三方软件安装目录

### 2. 命令行基础（⭐ 必熟练掌握）
- **文件操作**：`ls`, `cd`, `pwd`, `mkdir`, `rm`, `cp`, `mv`, `touch`, `cat/tac`, `head/tail`
- **权限管理**：`chmod`(rwx，数字755/644)、`chown`、`chgrp`、umask
- **文本处理三剑客**：
  - **grep**：文本搜索——`grep -r "error" /var/log/`、`-v`(排除)、`-i`(忽略大小写)
  - **sed**：流编辑器——`sed 's/old/new/g' file`（替换）、`sed -n '5,10p'`（打印指定行）
  - **awk**：文本分析——`awk '{print $1, $3}' file`（按列处理）
- **管道与重定向**：
  - `|`：管道，前一个命令的输出作为后一个的输入
  - `>` / `>>`：输出重定向（覆盖/追加）
  - `<`：输入重定向
  - `2>&1`：将标准错误重定向到标准输出

### 3. 进程与服务管理
- **进程查看**：`ps aux`、`top/htop`(交互式)、`pstree`(进程树)
- **后台运行**：`nohup`、`bg/fg`、`jobs`、`screen/tmux`(持久会话)
- **服务管理**：`systemctl start/stop/restart/status/enable service_name`
- **信号**：`kill -9 PID`(SIGKILL,强制)、`kill -15 PID`(SIGTERM,优雅)

### 4. 磁盘与网络
- **磁盘**：`df -h`(分区使用)、`du -sh *`(目录大小)、`fdisk/lsblk`(磁盘列表)
- **网络（⭐ 日常排障）**：
  - `ping`：连通性测试
  - `netstat/ss`：查看端口监听和连接——`ss -tlnp`(TCP监听)、`ss -tanp`(所有TCP连接)
  - `curl`：命令行HTTP客户端——`curl -X POST -H "Content-Type: application/json" -d '{}' url`
  - `tcpdump`：抓包分析——`tcpdump -i eth0 port 80`
  - `iptables/firewalld`：防火墙配置

### 5. Shell 脚本基础
```bash
#!/bin/bash
# 变量：NAME="world"（等号两边不能有空格！）
echo "Hello $NAME"

# 条件判断
if [ -f "file.txt" ]; then  # [ ]内必须有空格！
    echo "file exists"
fi

# 循环
for i in {1..5}; do echo $i; done

# 函数
my_func() { echo "called"; }
```

### 6. 常用工具
- **包管理**：`apt/yum/dnf`——安装/更新/卸载软件
- **压缩**：`tar -czvf`(创建gzip)、`tar -xzvf`(解压)、`gzip/gunzip`
- **查找**：`find /path -name "*.log" -mtime -7`(最近7天修改的日志)
- **vim 基础**：i(插入)→Esc(命令模式)→:wq(保存退出)、/搜索、dd删行、u撤销

## ✅ 学习建议
- 装一个Linux虚拟机（VirtualBox+Ubuntu），日常在上面操作——唯手熟尔
- 学完基本命令后看《鸟哥的Linux私房菜》——华语世界最好的Linux教材
- grep/sed/awk 至少 grep 要用得非常熟——查日志的核心工具
- Docker 是目前最推荐的Linux环境学习方式——秒启动秒销毁
