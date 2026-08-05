# Linux基础与JVM线上排查指南

## 一、Linux常用命令

Linux是Java后端开发工程师的必修课，无论是日常开发、测试环境部署还是线上问题排查，都离不开对Linux命令的熟练运用。本章将从文件操作、文本处理、系统监控、进程管理四个维度，系统性地讲解最常用的命令及其实际场景。

### 1.1 文件操作命令

#### ls —— 列出目录内容

`ls` 是最常用的命令之一，掌握其参数可以极大提高工作效率。

```bash
ls -l       # 以长格式显示，包含权限、所有者、大小、修改时间
ls -a       # 显示所有文件，包括以 . 开头的隐藏文件
ls -lh      # 以人类可读的格式显示文件大小（K、M、G）
ls -lt      # 按修改时间倒序排列，最新的文件在最前面
ls -ltr     # 按修改时间正序排列，最旧的文件在最前面
ls -lS      # 按文件大小降序排列
```

实际场景举例：

```bash
# 查看日志文件的大小，快速定位大文件
ls -lhS /var/log/app/*.log

# 查看最近修改的配置文件
ls -lt /etc/app/ | head -10
```

#### cd、pwd —— 目录切换与查看

```bash
cd /path/to/dir    # 切换到指定目录
cd ..              # 返回上级目录
cd ~               # 返回当前用户的家目录
cd -               # 返回上一个工作目录
pwd                # 显示当前工作目录的绝对路径
```

#### mkdir —— 创建目录

```bash
mkdir dirname                  # 创建单级目录
mkdir -p a/b/c/d               # 递归创建多级目录，父目录不存在时自动创建
mkdir -p {logs,backup,conf}    # 批量创建多个同级目录
```

#### cp —— 复制

```bash
cp file1 file2                  # 复制文件
cp -r src_dir dst_dir           # 递归复制整个目录
cp -i file1 file2               # 复制前提示确认（避免误覆盖）
cp -p file1 file2               # 保留原文件的属性（修改时间、权限等）
cp -a src_dir dst_dir           # 归档复制，保留所有属性，相当于 -dR --preserve=all
```

#### mv —— 移动/重命名

```bash
mv file1 file2                  # 将文件重命名
mv file /path/to/dir/           # 移动文件到指定目录
mv dir1 dir2                    # 将目录重命名或移动
```

#### rm —— 删除（危险操作！）

```bash
rm file.txt                     # 删除文件
rm -r dir/                      # 递归删除目录及其内容
rm -f file                      # 强制删除，不提示确认
rm -rf /                        # ！！！这是最危险的命令，会删除根分区所有数据！！！
rm -rf /*                       # 同样危险！！！
rm -rf ./                       # 如果cd到错误目录会删除当前目录所有内容
```

> **安全建议**：执行 `rm -rf` 前务必 `pwd` 确认当前目录，尤其是生产环境。可以用 `mv` 将文件移到临时目录替代直接删除，确认无误后再清理。养成使用 `-i` 参数（交互式确认）的习惯。

#### find —— 查找文件

`find` 是功能最强大的文件查找命令，远比 `locate` 灵活。

```bash
# 按文件名查找
find /path -name "app.log"          # 精确匹配文件名
find /path -name "*.log"            # 模糊匹配，所有 .log 结尾的文件
find /path -iname "*.LOG"           # 忽略大小写

# 按文件类型查找
find /path -type f                  # 只查找普通文件
find /path -type d                  # 只查找目录
find /path -type l                  # 查找符号链接

# 按文件大小查找
find /path -size +100M              # 查找大于 100MB 的文件
find /path -size -1k                # 查找小于 1KB 的文件
find /path -size +500M -size -1G    # 查找 500MB~1GB 之间的文件

# 按修改时间查找
find /path -mtime +7                # 7天前修改过的文件
find /path -mtime -3                # 3天内修改过的文件
find /path -mmin -30                # 30分钟内修改过的文件

# 执行操作（-exec的终结符是 \;）
find /path -name "*.tmp" -exec rm -f {} \;         # 找到并删除所有 .tmp 文件
find /path -name "*.log" -mtime +7 -exec gzip {} \; # 压缩7天前的日志
find /path -name "*.bak" -exec mv {} /backup/ \;   # 批量移动备份文件
```

#### chmod —— 修改文件权限

Linux权限使用三位八进制数字或 `ugoa+rwx` 符号形式表示。

```bash
# 数字法（最常用）
chmod 755 script.sh      # rwxr-xr-x：所有者可读写执行，组和其他人可读执行
chmod 644 config.conf    # rw-r--r--：所有者可读写，其他人只读
chmod 777 file           # rwxrwxrwx：所有人完全控制（生产环境慎用！）
chmod 600 id_rsa         # rw-------：只有所有者可读写（SSH私钥专用）

# 符号法
chmod u+x script.sh      # 给所有者增加执行权限
chmod g-w file           # 去掉组的写权限
chmod o+r file           # 给其他人增加读权限
chmod a+x file           # 给所有人（all）增加执行权限
chmod u=rwx,g=rx,o=r     # 等价于 754
```

`755` 和 `644` 是最常用的权限配置：
- **755**：用于可执行文件（脚本、程序）和目录，普通用户无法修改但可以执行和读取
- **644**：用于配置文件、文本文件，普通用户只能读取

#### chown —— 修改文件所有者

```bash
chown user:group file        # 同时修改所有者和所属组
chown user file              # 只修改所有者
chown :group file            # 只修改所属组
chown -R user:group dir/     # 递归修改整个目录
```

> **典型场景**：部署Java应用时，将应用目录的所有者改为运行用户，确保应用有权限写入日志和临时文件。

#### ln —— 链接

```bash
ln -s /data/app.jar /opt/app.jar    # 创建软链接（符号链接）
ln /data/app.jar /opt/app.jar       # 创建硬链接
```

**软链接与硬链接的区别**：

| 特性 | 软链接 | 硬链接 |
|------|--------|--------|
| 本质 | 存储目标文件的路径 | 与目标文件共享inode |
| 跨文件系统 | 支持 | 不支持 |
| 链接目录 | 支持 | 不支持（root用户除外） |
| 源文件删除后 | 链接失效（断链） | 不影响链接文件 |
| `ls -l` 显示 | `->` 指向目标 | 与普通文件相同 |

**软链接的典型应用**：

```bash
# Java版本切换（经典用法）
ln -sf /usr/local/jdk17 /usr/local/java
ln -sf /usr/local/jdk8 /usr/local/java
```

### 1.2 文本处理命令

文本处理是线上排查的核心技能，配合管道符可以实现强大的数据处理能力。

#### cat、less、head、tail —— 文件查看

```bash
cat file.txt                        # 输出文件全部内容（小文件适用）
cat -n file.txt                     # 显示行号

less file.txt                       # 分屏查看（大文件适用，支持上下翻页和搜索）
# 在 less 中的操作：
#   /keyword  - 向下搜索
#   ?keyword  - 向上搜索
#   g         - 跳到文件开头
#   G         - 跳到文件末尾
#   q         - 退出

head -20 file.txt                   # 查看文件前20行
head -n 50 file.txt                 # 查看前50行

tail -20 file.txt                   # 查看文件最后20行
tail -f app.log                     # 实时跟踪日志输出（线上排查利器！）
tail -f app.log | grep ERROR        # 实时过滤错误日志
```

> **生产必备技巧**：`tail -f` 结合 `grep` 是实时排查问题的黄金组合。要跟踪多个日志文件可用 `tail -f /var/log/app*.log`。

#### grep —— 文本搜索（核心排查工具）

```bash
# 基本用法
grep "ERROR" app.log                # 搜索包含 ERROR 的行
grep -i "error" app.log             # 忽略大小写搜索
grep -v "DEBUG" app.log             # 反向匹配，排除包含 DEBUG 的行
grep -n "ERROR" app.log             # 显示匹配行及其行号
grep -c "ERROR" app.log             # 只统计匹配行数

# 递归搜索
grep -r "System.exit" /src/         # 递归搜索目录下所有文件

# 上下文显示（排查异常时极其有用）
grep -A 5 "NullPointerException" app.log     # 显示匹配行及其后5行
grep -B 5 "NullPointerException" app.log     # 显示匹配行及其前5行
grep -C 3 "NullPointerException" app.log     # 显示匹配行及其前后各3行

# 正则表达式
grep "^2024-06-.*ERROR" app.log             # 以日期开头且包含 ERROR
grep "[0-9]\{3\}\.[0-9]\{3\}\." app.log     # 匹配IP地址
grep -E "ERROR|FATAL" app.log               # 扩展正则，匹配多个关键字
grep -P "\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}" app.log  # Perl正则匹配IP

# 复合过滤
grep "NullPointerException" app.log | grep "UserService"  # 多个条件的交集
```

> **排查实战**：线上出现OOM时，先用 `grep -i "outofmemory\|oom" app.log` 快速定位异常，再用 `-A 20` 查看上下文，最后用 `grep -r` 在代码中搜索可疑代码。

#### awk —— 列处理神器

`awk` 是一个强大的文本分析工具，默认按空格/Tab分割每行，适用于结构化日志和命令输出处理。

```bash
# 基本用法
awk '{print $1, $3}' file.txt       # 打印第1列和第3列
awk '{print $NF}' file.txt           # 打印最后一列
awk '{print $(NF-1)}' file.txt       # 打印倒数第二列
awk '{print NR, $0}' file.txt        # NR是行号，$0是整行

# 指定分隔符
awk -F ':' '{print $1, $3}' /etc/passwd           # 以冒号分割
awk -F '[,|]' '{print $1, $2}' data.csv           # 使用多个分隔符

# 条件判断
awk '$3 > 500 {print $1, $3}' file.txt             # 第3列大于500的行
awk '/ERROR/ {print $0}' app.log                   # 类似grep搜索
awk '$1 ~ /2024-06/ {print $NF}' app.log           # 第1列匹配正则

# 统计求和
ps aux | awk '{sum+=$6} END {print "Total MEM:", sum/1024 "MB"}'  # 统计所有进程内存总和
awk '{sum+=$3} END {print "Total:", sum}' data.txt                # 对第3列求和

# 格式化输出
ps aux | awk '{printf "%-20s %-10s %s\n", $11, $6/1024, $2}'     # 格式化打印

# 复杂场景：分析访问日志
cat access.log | awk '{print $1}' | sort | uniq -c | sort -rn | head -10
# 统计访问次数最多的前10个IP
```

**awk与grep的配合**：

```bash
# 从日志中提取所有ERROR的时间戳和消息
grep "ERROR" app.log | awk '{print $1, $2, $NF}'

# 从jstat输出中提取FGC次数
jstat -gcutil <pid> | awk 'NR>1 {print "FGC:" $8, "FGCT:" $9}'
```

#### sed —— 流编辑器

`sed` 用于对文本进行替换、删除、插入等编辑操作，最常用的是替换和删除。

```bash
# 替换操作
sed 's/old/new/g' file.txt                 # 将每行所有 old 替换为 new
sed 's/old/new/' file.txt                  # 只替换每行第一个匹配
sed 's/old/new/2' file.txt                 # 只替换每行第二个匹配
sed 's/old/new/g' file.txt > newfile.txt   # 将结果保存到新文件（不修改原文件）
sed -i 's/old/new/g' file.txt              # 直接修改原文件（-i 参数）
sed -i.bak 's/old/new/g' file.txt          # 备份原文件为.bak，再修改

# 指定行操作
sed '3s/old/new/g' file.txt                # 只替换第3行
sed '3,10s/old/new/g' file.txt             # 替换第3到10行
sed '$s/old/new/g' file.txt                # 替换最后一行

# 删除操作
sed '/pattern/d' file.txt                  # 删除包含pattern的行
sed '3,10d' file.txt                       # 删除第3到10行
sed '/^$/d' file.txt                       # 删除所有空行
sed '/^#/d' /etc/nginx.conf                # 删除注释行（以#开头）

# 行操作
sed -n '10,20p' file.txt                   # 打印第10到20行（-n抑制默认输出）
sed '5a\new line' file.txt                 # 在第5行后追加一行
sed '5i\new line' file.txt                 # 在第5行前插入一行

# 实战：批量替换配置
sed -i 's/JAVA_OPTS="-Xms512m"/JAVA_OPTS="-Xms1024m"/g' start.sh
```

**线上排查经典三板斧**：

```bash
# 1. 高频错误TOP10
grep -o "Exception: [^ ]*" app.log | sort | uniq -c | sort -rn | head -10

# 2. 按分钟统计错误数量
grep "ERROR" app.log | awk -F'[ :]' '{print $1"-"$2":"$3}' | sort | uniq -c

# 3. 提取关键字段分析
awk '/GET \/api/ {print $1, $4, $7, $9}' access.log | awk -F '[:/ ]' '{print $1, $2, $3, $7}' | sort | uniq -c | sort -rn
```

### 1.3 系统监控命令

系统监控是线上排查的眼睛，通过这些命令可以快速了解服务器的运行状态。

#### top —— 系统实时监控

`top` 是系统管理员最重要的工具，提供了系统整体状态的实时视图。

```bash
top                        # 进入交互式监控界面
top -b -n 1                # 以批处理模式运行一次（可用于脚本采集）
top -p 1234                # 只监控指定PID的进程
top -H -p 1234             # 监控指定进程的所有线程（常用！）
```

**top输出解读**：

```
top - 14:23:45 up 30 days,  2:15,  3 users,  load average: 2.50, 1.80, 1.20
Tasks: 123 total,   1 running, 122 sleeping,   0 stopped,   0 zombie
%Cpu(s): 45.0 us, 10.0 sy,  0.0 ni, 44.8 id,  0.0 wa,  0.0 hi,  0.2 si,  0.0 st
MiB Mem :  16000.0 total,   2000.0 free,   8000.0 used,   6000.0 buff/cache
MiB Swap:   2048.0 total,   1800.0 free,    248.0 used.   6000.0 avail Mem

  PID USER      PR  NI    VIRT    RES    SHR S  %CPU  %MEM     TIME+ COMMAND
 5678 appuser   20   0  4.5g 800000  25000 S  99.0   5.0  12:34.56 java
```

**关键指标解读**：

- **load average**（1/5/15分钟）：系统的平均负载，表示处于可运行状态或不可中断睡眠状态的进程平均数。**如果超过CPU核数（如4核CPU负载>4），说明系统过载**。
- **us（user）**：用户态CPU时间占比。Java应用密集运算时此值高。
- **sy（system）**：内核态CPU时间占比。过高可能说明系统调用频繁或存在锁竞争。
- **id（idle）**：空闲CPU占比。持续为0说明CPU资源耗尽。
- **wa（iowait）**：等待I/O的CPU时间占比。过高说明磁盘I/O成为瓶颈。
- **RES**：常驻内存（物理内存），不包含swap。
- **%MEM**：物理内存使用率。
- **SHR**：共享内存大小。

**top交互式快捷键**：

| 按键 | 功能 |
|------|------|
| `P` | 按CPU使用率排序（大写P） |
| `M` | 按内存使用率排序 |
| `T` | 按运行时间排序 |
| `1` | 展开/折叠显示每个CPU核心 |
| `k` | 终止进程（输入PID和信号） |
| `r` | 调整进程优先级（renice） |
| `c` | 显示完整命令行 |
| `H` | 切换线程模式 |
| `q` | 退出 |

#### free —— 内存查看

```bash
free -h                     # 人类可读格式输出
free -m                     # 以MB为单位输出
free -g                     # 以GB为单位输出
free -s 5                   # 每5秒刷新一次
```

**free输出解读**：

```
               total        used        free      shared  buff/cache   available
Mem:           15.6G        7.8G        2.0G        0.1G        5.8G        6.2G
Swap:          2.0G        248M        1.8G
```

- **buff/cache**：内核缓存和缓冲区占用，可在内存紧张时回收。
- **available**：可供新进程使用的实际内存，是最准确的"可用内存"指标。
- **Swap used**：如果持续增长且很大，说明**物理内存不足**，JVM需要调优。

#### df、du —— 磁盘分析

```bash
df -h                       # 查看各分区磁盘使用情况
df -h /data/app             # 查看指定目录所在分区
df -i                       # 查看inode使用情况（inode耗尽时磁盘空间仍有剩余）

du -sh /data/app            # 查看指定目录总大小
du -sh *                    # 查看当前目录下所有子目录/文件的大小
du -h --max-depth=1 /data   # 只看当前层级的目录大小
du -sm * | sort -rn | head -10   # 找出当前目录下最大的10个文件/目录
```

**排查实战**：服务突然不可用，先检查磁盘。

```bash
df -h                       # 检查磁盘是否满了（通常使用率>90%需要关注）
du -sh /data/app/logs/*     # 定位大目录，十有八九是日志文件
ls -lhS /data/app/logs/     # 定位大文件
```

#### netstat、ss —— 网络连接查看

```bash
# netstat（传统工具，部分系统需安装）
netstat -anp | grep 8080               # 查看8080端口的监听状态和连接进程
netstat -tlnp                           # 只显示TCP监听端口及其进程
netstat -ulnp                           # 只显示UDP监听端口
netstat -ant | awk '{print $6}' | sort | uniq -c  # 统计TCP连接状态分布

# ss（新版替代，速度更快）
ss -tlnp                                # 显示所有TCP监听端口
ss -antp                                # 显示所有TCP连接
ss -s                                   # 显示socket统计信息
ss -antp | grep 8080                    # 查看8080端口的连接情况
```

**TCP连接状态排查**：

```bash
# 查看TIME_WAIT和CLOSE_WAIT数量
ss -antp | grep -c TIME_WAIT
ss -antp | grep -c CLOSE_WAIT

# 大量TIME_WAIT是正常的，但数量异常多（>5万）时需调整内核参数
# CLOSE_WAIT过多通常是代码未正确关闭连接，需要排查应用
```

#### lsof —— 查看打开的文件

Linux一切皆文件，`lsof` 可以查看进程打开的所有文件，包括网络连接、socket、普通文件等。

```bash
lsof -i :8080                          # 查看占用8080端口的进程
lsof -p 1234                           # 查看进程1234打开的所有文件
lsof -i -P -n                          # 显示所有网络连接
lsof /var/log/app.log                  # 查看哪些进程正在使用此日志文件
lsof +D /data/app/                     # 查看目录下被打开的文件
```

**典型场景**：

```bash
# 端口冲突时查看谁占用了端口
lsof -i :8080
# 输出：COMMAND   PID   USER   FD   TYPE DEVICE SIZE/OFF NODE NAME
#       java    5678 appuser  121u  IPv4 123456      0t0  TCP *:8080 (LISTEN)

# 日志文件被删除但磁盘空间未释放时
lsof | grep deleted                     # 找到已删除但仍在使用的文件
```

### 1.4 进程管理命令

#### ps —— 进程快照

```bash
ps -ef                                  # 显示所有进程完整格式
ps -aux                                 # 以BSD格式显示所有进程
ps -ef | grep java                      # 查看所有Java进程
ps aux --sort=-%mem | head -10          # 按内存使用率降序排列前10
ps aux --sort=-%cpu | head -10          # 按CPU使用率降序排列前10
ps -Lp 1234 -o pid,tid,pcpu,comm       # 查看进程的所有线程
```

**`ps -ef` vs `ps -aux` 区别**：

- `ps -ef`：标准格式，包含PPID（父进程PID）
- `ps -aux`：BSD格式，包含%CPU、%MEM等百分比信息，排查问题更直观

#### kill —— 发送信号

```bash
kill -15 <pid>              # 默认，SIGTERM，请求进程优雅终止
kill -9 <pid>               # SIGKILL，强制杀死进程（无法被捕获）
kill -3 <pid>               # SIGQUIT，打印线程堆栈（不终止进程）
kill -1 <pid>               # SIGHUP，重新加载配置

# 按进程名批量操作
pkill -f app.jar            # 杀死所有匹配app.jar的进程
pkill -15 -f "java.*order"  # 优雅终止所有order服务的Java进程

# 注意：kill -9 是最后的手段，应先尝试 kill -15 让进程进行清理
```

**信号优先级**：

```bash
kill -15 <pid>              # 先尝试优雅关闭
sleep 5                     # 等待5秒
kill -9 <pid>               # 如果还在运行，强制杀死
```

#### nohup、&、jobs、fg、bg —— 后台运行

```bash
# 后台运行并忽略HUP信号（退出终端后继续运行）
nohup java -jar app.jar > app.log 2>&1 &

# 命令解释：
# nohup   - 忽略挂断信号，退出终端后进程继续运行
# > app.log - 将标准输出重定向到文件
# 2>&1    - 将标准错误重定向到标准输出（都输出到app.log）
# &       - 放入后台运行

nohup java -jar order.jar --server.port=8080 > /var/log/order/console.log 2>&1 &

# 作业管理
jobs                    # 查看当前终端的后台作业列表
fg %1                   # 将作业1调到前台
bg %1                   # 将暂停的作业1设置为后台运行
Ctrl+Z                  # 将当前前台进程暂停并放入后台
```

**生产环境推荐**：使用系统服务管理工具（systemd、supervisor）代替 nohup，支持自动重启、集中管理等高级功能。

### 1.5 常用命令组合（线上排查必备套路）

```bash
# 套路1：提取日志中某个时间段的关键信息
grep "2024-06-13 14:[0-9][0-9]" app.log | grep "ERROR" | awk '{print $1, $2, $NF}'

# 套路2：批量操作文件
find /data/app/logs -name "*.log.gz" -mtime +30 -exec rm -f {} \;

# 套路3：找到CPU最高的进程→找到线程→定位代码
ps -ef | grep java | awk '{print $2}'
top -H -p <pid>                        # 找到CPU最高的TID
printf "%x\n" <tid>                    # 转为十六进制
jstack <pid> | grep -A 30 "nid=0x<tid_hex>"  # 定位线程代码

# 套路4：实时跟踪日志并过滤
tail -f app.log | grep --line-buffered "ERROR" | awk '{print $2, $3, $NF}'

# 套路5：查看磁盘占用并清理
df -h && du -sh /var/log/* && find /var/log -name "*.log" -mtime +7 -exec rm -f {} \;

# 套路6：进程端口对应检查
netstat -tlnp | grep java         # 查看Java进程监听的端口
lsof -i :8080                     # 查看指定端口的进程
ps aux | grep <pid>               # 查看进程详细信息

# 套路7：查看实时网络流量（需要额外工具）
# sar -n DEV 1 3                   # 查看网络接口流量
# iptraf-ng                        # 交互式网络监控
```

---

## 二、Shell脚本基础

Shell脚本是Linux自动化运维的基石，掌握Shell脚本可以大幅提升工作效率。

### 2.1 shebang与基本结构

```bash
#!/bin/bash
# ^^^ shebang，告诉系统用什么解释器执行此脚本
# 其他解释器选项：
# #!/bin/sh     - 基础Shell
# #!/bin/bash   - Bash（功能最全）
# #!/usr/bin/python3 - Python脚本

# 脚本基本结构
#!/bin/bash
set -e                    # 遇到错误立即退出（生产脚本推荐）
set -u                    # 使用未定义变量时报错
set -o pipefail           # 管道中任一部分失败则整体失败

# 或合并写为
set -euo pipefail

echo "脚本开始执行"
```

### 2.2 变量

```bash
# 变量定义（等号两侧不能有空格）
name="AppName"
port=8080
version="1.2.3"

# 使用变量
echo $name
echo ${name}             # 推荐使用花括号，避免歧义
echo "${name}_v2"        # 变量名边界明确

# 环境变量
echo $PATH               # 可执行文件搜索路径
echo $HOME               # 当前用户家目录
echo $JAVA_HOME          # JDK安装路径（需配置）

# 特殊变量
echo $0                  # 脚本自身的文件名
echo $1 $2 $3            # 第1到3个参数
echo $#                  # 参数个数
echo $@                  # 所有参数列表（"$1" "$2" "$3" ...）
echo $*                  # 所有参数（一个字符串）
echo $?                  # 上一条命令的退出码（0=成功，非0=失败）
echo $$                  # 当前进程的PID
echo $!                  # 上一个后台进程的PID

# 命令替换（两种方式）
current_date=$(date +%Y-%m-%d)       # 推荐方式
current_date=`date +%Y-%m-%d`        # 旧式写法

# 算术运算
count=$((count + 1))
sum=$((a + b))
```

### 2.3 条件判断

```bash
# if 结构
if [ condition ]; then
    # 条件为真时执行
elif [ condition2 ]; then
    # 条件2为真时执行
else
    # 都不满足时执行
fi

# 文件判断
[ -f "/etc/config.yml" ]      # 是否为普通文件
[ -d "/data/app" ]            # 是否为目录
[ -e "/path/to/file" ]        # 文件或目录是否存在
[ -s "/path/to/file" ]        # 文件是否存在且非空
[ -r "/path/to/file" ]        # 是否可读
[ -w "/path/to/file" ]        # 是否可写
[ -x "/path/to/file" ]        # 是否可执行
[ -L "/path/to/link" ]        # 是否为符号链接

# 字符串判断
[ -z "$var" ]                 # 字符串是否为空（长度为0）
[ -n "$var" ]                 # 字符串是否非空
[ "$a" = "$b" ]               # 字符串相等（注意：= 两侧有空格）
[ "$a" != "$b" ]              # 字符串不相等
[ "$a" \< "$b" ]              # 字符串小于（字典序）
[ "$a" \> "$b" ]              # 字符串大于

# 数值比较
[ "$a" -eq "$b" ]             # 等于
[ "$a" -ne "$b" ]             # 不等于
[ "$a" -lt "$b" ]             # 小于
[ "$a" -le "$b" ]             # 小于等于
[ "$a" -gt "$b" ]             # 大于
[ "$a" -ge "$b" ]             # 大于等于

# 逻辑运算符
[ "$a" -gt 0 ] && [ "$a" -lt 100 ]   # 逻辑与
[ "$a" -lt 0 ] || [ "$a" -gt 100 ]   # 逻辑或
[ ! -d "/tmp" ]                       # 逻辑非

# 组合测试
[[ "$a" =~ ^[0-9]+$ ]]               # 使用正则（双中括号）
[[ "$name" == "app"* ]]              # 通配符匹配

# case 结构
case "$1" in
    start)
        echo "Starting..."
        ;;
    stop)
        echo "Stopping..."
        ;;
    restart|reload)
        echo "Restarting..."
        ;;
    *)
        echo "Usage: $0 {start|stop|restart}"
        exit 1
        ;;
esac
```

### 2.4 循环

```bash
# for 循环
# 列举式
for env in dev test prod; do
    echo "Deploying to $env environment"
done

# 数字范围
for i in {1..10}; do                   # 1到10
    echo "Iteration $i"
done
for i in $(seq 1 2 10); do             # 1,3,5,7,9
    echo $i
done

# 类C风格
for ((i=0; i<10; i++)); do
    echo $i
done

# 文件遍历
for file in /data/logs/*.log; do
    echo "Processing $file"
done

# while 循环
count=0
while [ $count -lt 10 ]; do
    echo "Count: $count"
    count=$((count + 1))
done

# 读取文件
while IFS= read -r line; do
    echo "$line"
done < /etc/hosts

# 无限循环（常用于监控脚本）
while true; do
    cpu=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}')
    echo "CPU: $cpu%"
    sleep 5
done
```

### 2.5 函数

```bash
# 定义函数
log_info() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [INFO] $1"
}

log_error() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [ERROR] $1" >&2
}

# 使用函数
log_info "Application starting..."
log_error "Connection timeout!"

# 带返回值的函数
get_status() {
    local pid=$1            # local声明局部变量
    if ps -p $pid > /dev/null 2>&1; then
        return 0            # 状态码0表示成功
    else
        return 1            # 状态码1表示失败
    fi
}

if get_status $app_pid; then
    echo "Process is running"
else
    echo "Process is not running"
fi
```

### 2.6 脚本实战

#### 日志清理脚本

```bash
#!/bin/bash
# 功能：按天清理N天前的应用日志
# 用法：./clean_logs.sh [日志目录] [保留天数]

set -euo pipefail

LOG_DIR=${1:-"/var/log/app"}        # 默认日志目录
RETENTION_DAYS=${2:-7}              # 默认保留7天
LOG_FILE="/var/log/clean_logs.log"

# 检查日志目录是否存在
if [ ! -d "$LOG_DIR" ]; then
    echo "[ERROR] Directory $LOG_DIR does not exist"
    exit 1
fi

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Cleaning logs older than $RETENTION_DAYS days in $LOG_DIR" >> $LOG_FILE

# 清理 .log 文件
find "$LOG_DIR" -name "*.log" -mtime +$RETENTION_DAYS -exec rm -f {} \; -exec echo "Deleted: {}" >> $LOG_FILE \;

# 清理 .log.gz 文件
find "$LOG_DIR" -name "*.log.gz" -mtime +$((RETENTION_DAYS + 30)) -exec rm -f {} \; -exec echo "Deleted aged archive: {}" >> $LOG_FILE \;

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Cleanup completed" >> $LOG_FILE
```

#### 批量服务重启脚本

```bash
#!/bin/bash
# 功能：批量重启Java微服务
# 用法：./restart_services.sh order payment user

APP_DIR="/data/app"
USER="appuser"

restart_service() {
    local service_name=$1
    local jar_path="$APP_DIR/$service_name/$service_name.jar"
    local log_path="$APP_DIR/$service_name/console.log"
    local pid_file="$APP_DIR/$service_name/pid.txt"

    echo "[$(date '+%H:%M:%S')] Processing $service_name..."

    # 停止旧进程
    if [ -f "$pid_file" ]; then
        old_pid=$(cat "$pid_file")
        if kill -15 "$old_pid" 2>/dev/null; then
            echo "  Sent SIGTERM to PID $old_pid"
            sleep 3
            # 如果进程仍在运行，强制终止
            if kill -0 "$old_pid" 2>/dev/null; then
                kill -9 "$old_pid"
                echo "  Force killed PID $old_pid"
            fi
        fi
    fi

    # 启动新进程
    nohup java -jar "$jar_path" --spring.profiles.active=prod > "$log_path" 2>&1 &
    new_pid=$!
    echo $new_pid > "$pid_file"
    echo "  Started with PID $new_pid"

    # 等待并检查启动状态
    sleep 5
    if kill -0 "$new_pid" 2>/dev/null; then
        echo "  Service $service_name started successfully"
    else
        echo "  ERROR: Service $service_name failed to start!" >&2
        tail -5 "$log_path" | sed 's/^/  /'
    fi
}

# 主流程
if [ $# -eq 0 ]; then
    echo "Usage: $0 <service_name> [service_name ...]"
    exit 1
fi

for service in "$@"; do
    restart_service "$service"
done

echo "All services processed."
```

#### 定时备份脚本

```bash
#!/bin/bash
# 功能：备份MySQL数据库和配置文件到远程服务器
# 配合cron使用：0 3 * * * /opt/scripts/backup.sh

set -euo pipefail

# 配置
BACKUP_DIR="/backup/$(date +%Y%m%d)"
DB_USER="backup"
DB_PASS="$(cat /etc/backup_pass.txt)"  # 从文件读取密码，避免明文
DB_NAME="app_db"
REMOTE_HOST="backup@10.0.0.100"
REMOTE_DIR="/backup/app/$(hostname)/"
RETENTION_DAYS=30

# 创建备份目录
mkdir -p "$BACKUP_DIR"

# 备份数据库
log_info "Backing up database..."
mysqldump -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" | gzip > "$BACKUP_DIR/db_$(date +%H%M%S).sql.gz"

# 配置备份
log_info "Backing up configuration..."
tar czf "$BACKUP_DIR/conf.tar.gz" /etc/app/ /data/app/conf/

# 上传到远程
log_info "Uploading to remote..."
scp -r "$BACKUP_DIR" "$REMOTE_HOST:$REMOTE_DIR"

# 清理本地过期备份
find /backup -maxdepth 1 -type d -name "20*" -mtime +$RETENTION_DAYS -exec rm -rf {} \;

# 清理远程过期备份
ssh "$REMOTE_HOST" "find $REMOTE_DIR -type d -name '20*' -mtime +$RETENTION_DAYS -exec rm -rf {} \;"

log_info "Backup completed successfully"
```

#### 完整Java应用部署脚本

```bash
#!/bin/bash
# 功能：完整Java应用部署脚本
# 流程：拉取代码->编译->停止旧服务->备份->启动新服务
set -euo pipefail

# ============ 配置 ============
APP_NAME="order-service"
GIT_URL="git@github.com:company/order-service.git"
GIT_BRANCH="main"
APP_DIR="/data/app/$APP_NAME"
BACKUP_DIR="/backup/app/$APP_NAME"
JAR_NAME="target/${APP_NAME}-1.0.0.jar"
JAVA_OPTS="-Xms2g -Xmx2g -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPTS="$JAVA_OPTS -XX:HeapDumpPath=$APP_DIR/heapdump.hprof"
JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=prod"
RUN_USER="appuser"
PORT=8080

# ============ 函数 ============
log() {
    local level=$1
    shift
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [$level] $*"
}

check_health() {
    local port=$1
    local max_attempts=30
    local interval=2

    for ((i=1; i<=max_attempts; i++)); do
        if curl -s "http://127.0.0.1:$port/actuator/health" | grep -q "UP"; then
            log "INFO" "Health check passed (attempt $i)"
            return 0
        fi
        sleep $interval
        log "INFO" "Waiting for service... (attempt $i/$max_attempts)"
    done

    log "ERROR" "Health check failed after $max_attempts attempts"
    return 1
}

# ============ 主流程 ============
log "INFO" "Starting deployment of $APP_NAME..."

# Step 1: 拉取最新代码
log "INFO" "Step 1: Pulling latest code..."
if [ -d "$APP_DIR/repo" ]; then
    cd "$APP_DIR/repo"
    git fetch --all && git reset --hard origin/$GIT_BRANCH
else
    git clone -b "$GIT_BRANCH" "$GIT_URL" "$APP_DIR/repo"
    cd "$APP_DIR/repo"
fi

# Step 2: Maven编译
log "INFO" "Step 2: Building application..."
mvn clean package -DskipTests -q
log "INFO" "Build completed"

# Step 3: 停止旧服务
log "INFO" "Step 3: Stopping old service..."
OLD_PID=$(pgrep -f "$APP_NAME" || true)
if [ -n "$OLD_PID" ]; then
    log "INFO" "Found running process PID=$OLD_PID, sending SIGTERM..."
    kill -15 "$OLD_PID" || true
    sleep 5
    # 检查是否已停止
    if kill -0 "$OLD_PID" 2>/dev/null; then
        log "WARN" "Process still running, sending SIGKILL..."
        kill -9 "$OLD_PID" || true
    fi
    log "INFO" "Old service stopped"
fi

# Step 4: 备份旧版本
log "INFO" "Step 4: Backing up old version..."
BACKUP_PATH="$BACKUP_DIR/$(date +%Y%m%d_%H%M%S)"
if [ -f "$APP_DIR/$JAR_NAME" ]; then
    mkdir -p "$BACKUP_PATH"
    cp "$APP_DIR/$JAR_NAME" "$BACKUP_PATH/"
    cp -r "$APP_DIR/repo/config/" "$BACKUP_PATH/" 2>/dev/null || true
    log "INFO" "Backup saved to $BACKUP_PATH"
fi

# Step 5: 部署新版本
log "INFO" "Step 5: Deploying new version..."
cp "$APP_DIR/repo/$JAR_NAME" "$APP_DIR/$JAR_NAME"

# Step 6: 启动新服务
log "INFO" "Step 6: Starting new service..."
chown "$RUN_USER:$RUN_USER" "$APP_DIR/$JAR_NAME"
sudo -u "$RUN_USER" nohup java $JAVA_OPTS -jar "$APP_DIR/$JAR_NAME" > "$APP_DIR/console.log" 2>&1 &
NEW_PID=$!
echo "$NEW_PID" > "$APP_DIR/pid.txt"
log "INFO" "Started new process PID=$NEW_PID"

# Step 7: 健康检查
log "INFO" "Step 7: Running health check..."
if check_health "$PORT"; then
    log "INFO" "========================================"
    log "INFO" "Deployment of $APP_NAME completed successfully!"
    log "INFO" "New PID: $NEW_PID"
    log "INFO" "========================================"
else
    log "ERROR" "Deployment failed, rolling back..."
    if [ -d "$BACKUP_PATH" ]; then
        cp "$BACKUP_PATH/${JAR_NAME##*/}" "$APP_DIR/$JAR_NAME"
        # 启动旧版本...
    fi
    exit 1
fi

# 清理旧备份（保留最近7天）
find "$BACKUP_DIR" -type d -name "20*" -mtime +7 -exec rm -rf {} \; 2>/dev/null || true
```

---

## 三、JVM排查工具（重点）

JVM排查工具是Java后端工程师处理线上问题的核心武器。掌握这些工具能让你在面对CPU飙高、内存泄漏、死锁、接口超时等问题时游刃有余。

### 3.1 jps —— 查看Java进程

`jps` 是最基本的JVM工具，类似于Linux的 `ps`，但只显示Java进程。

```bash
# 基本用法
jps                                     # 显示Java进程的PID和主类名
jps -l                                  # 显示全类名（推荐，避免同名类混淆）
jps -v                                  # 显示JVM启动参数（查看JVM配置是否正确）
jps -m                                  # 显示main方法的参数

# 常用组合
jps -lv | grep -E "order|payment"       # 过滤指定服务
jps -lv | grep -v "Bootstrap"           # 排除Tomcat内置进程

# 输出示例
# 5678 order-service.jar -Xms2g -Xmx2g -Dspring.profiles.active=prod
# 9012 payment-service.jar -Xms1g -Xmx1g -Dspring.profiles.active=prod
```

> **注意**：如果 `jps` 找不到Java进程，可能因为当前用户权限不足，尝试切换到运行该Java进程的用户，或使用 `ps -ef | grep java`。

**jps与ps对比**：

| 场景 | jps | ps -ef \| grep java |
|------|-----|---------------------|
| 仅看Java进程 | 简洁，直接显示 | 输出杂乱 |
| 查看JVM参数 | jps -v 直接显示 | 需要看完整命令行 |
| 进程不存在时 | 明确不显示 | 有时残留grep自身进程 |
| 权限问题 | 某些环境受限 | 一直可用 |

### 3.2 jstack —— 线程栈分析

`jstack` 是解决死锁、线程挂起、CPU飙升等问题的一把利器。

```bash
jstack <pid>                            # 打印所有线程栈
jstack -l <pid>                         # 显示锁信息（检测死锁必备）
jstack -m <pid>                         # 混合模式，显示本地方法栈

# 常用操作
jstack <pid> | grep -A 30 "BLOCKED"     # 查看所有被阻塞的线程
jstack <pid> | grep -A 30 "WAITING"     # 查看所有等待状态的线程
jstack <pid> > /tmp/thread.dump         # 保存线程栈到文件

# 在线程栈中搜索指定线程ID（十六进制）
jstack <pid> | grep -A 30 "nid=0x1234"
```

**线程状态解读**：

| 状态 | 含义 | 典型原因 |
|------|------|----------|
| RUNNABLE | 正在执行或等待CPU | 正常，但如果大量线程持续RUNNABLE可能CPU过高 |
| BLOCKED | 等待获取锁（被阻塞） | 锁竞争激烈，其他线程持有锁不释放 |
| WAITING | 无限期等待（Object.wait()不带超时、LockSupport.park()） | 池化线程空闲等待，数量多正常 |
| TIMED_WAITING | 有超时等待（sleep、wait(timeout)、parkNanos） | 正常，长时间存在可能有问题 |
| NEW | 线程已创建但未启动 | 罕见 |
| TERMINATED | 线程已结束 | 正常 |

**死锁检测**：

```bash
jstack -l <pid>

# 如果存在死锁，输出中会有明显提示：
# Found one Java-level deadlock:
# =============
# "thread-1":
#   waiting to lock <0x000000076b5f3e78> (a java.lang.String)
#   which is held by "thread-2"
# "thread-2":
#   waiting to lock <0x000000076b5f3e48> (a java.lang.String)
#   which is held by "thread-1"
#
# Found 1 deadlock.
```

**实战排查CPU高**：

```bash
# Step 1: 找到CPU高的Java进程
top -c                                 # 按P（大写P）按CPU排序
# 找到PID: 5678

# Step 2: 查看进程中哪些线程CPU高
top -H -p 5678                         # 按P排序
# 找到TID: 5790（十进制的线程ID）

# Step 3: 将TID转十六进制
printf "%x\n" 5790                     # 输出: 169e

# Step 4: 在线程栈中定位代码
jstack 5678 | grep -A 30 "nid=0x169e"  # 就能看到具体哪行代码在消耗CPU
```

### 3.3 jstat —— JVM统计监控

`jstat` 是监控JVM GC行为的最佳工具，可以实时观察堆内存各区域的变化和GC频率。

```bash
# 基本用法（每1000ms采样一次，共10次）
jstat -gc <pid> 1000 10

# 常用输出格式
jstat -gc <pid>                         # 各区域容量和使用量
jstat -gcutil <pid> 1000                # 各区域使用率百分比（最常用）
jstat -gccause <pid>                    # 最近一次GC的原因
jstat -gcold <pid>                      # 老年代GC情况
```

**jstat -gc输出解读**：

```
S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    YGC   YGCT   FGC   FGCT    GCT
512.0  512.0  128.0  0.0    2048.0   1024.0   4096.0     2048.0   256.0  240.0  150  3.450   5    1.200   4.650
```

| 字段 | 含义 | 排查要点 |
|------|------|----------|
| S0C/S1C | Survivor区容量 | 通常相等 |
| S0U/S1U | Survivor区使用量 | 一空一满正常；S0U>S0C说明对象晋升前就放不下 |
| EC/EU | Eden区容量/使用量 | EU接近EC时即将发生Minor GC |
| OC/OU | 老年代容量/使用量 | OU持续增长且不降，可疑内存泄漏 |
| MC/MU | 元空间容量/使用量 | MU接近MC说明加载类太多 |
| YGC/YGCT | Young GC次数/耗时 | 频率高说明Eden过小 |
| FGC/FGCT | Full GC次数/耗时 | FGC频繁且OU不降，典型内存泄漏信号 |
| GCT | 总GC耗时 | 占比过高影响吞吐量 |

**jstat -gcutil输出解读**：

```
S0     S1     E      O      M     YGC   YGCT   FGC   FGCT   GCT
25.00   0.00  80.00  50.00  93.75  150  3.450    5   1.200  4.650
```

这将各区域的使用量转换为百分比，更加直观。

**线上排查三板斧**：

```bash
# 1. 持续观察GC情况（每2秒输出一次）
jstat -gcutil <pid> 2000

# 2. 判断是否有内存泄漏
# 如果 Full GC 次数（FGC）持续增长，且老年代使用率（O）在每次FGC后不下降或下降很少
# 几乎可以断定存在内存泄漏

# 3. 查看GC原因
jstat -gccause <pid>
# 输出：LGCC（上次GC原因）和GCC（当前GC原因）
# 常见原因：Allocation Failure（分配失败）, System.gc(), Metadata GC Threshold等
```

### 3.4 jmap —— 堆内存分析

`jmap` 是Java堆内存的"CT机"，可以查看堆配置、统计对象分布、导出堆快照。

```bash
# 查看堆概要信息
jmap -heap <pid>

# 查看对象统计（按对象数量/占用内存排序）
jmap -histo <pid> | head -20            # 显示前20个最多的类

# 只统计存活对象（会触发Full GC！慎用！）
jmap -histo:live <pid> | head -20

# 导出堆快照（核心功能）
jmap -dump:format=b,file=/tmp/heap.hprof <pid>      # 导出包含非存活对象的堆
jmap -dump:live,format=b,file=/tmp/heap.hprof <pid>  # 只导出存活对象（推荐，文件更小）
```

**jmap -heap 输出解读**：

```
Attaching to process ID 5678, please wait...
Debugger attached successfully.

using thread-local object allocation.
Garbage Collector (GC) G1 Young Generation, G1 Old Generation

Heap Configuration:
   MinHeapFreeRatio         = 40
   MaxHeapFreeRatio         = 70
   MaxHeapSize              = 2147483648 (2048.0MB)    # -Xmx
   NewSize                  = 536870912 (512.0MB)      # 新生代初始大小
   MaxNewSize               = 536870912 (512.0MB)      # 新生代最大大小
   OldSize                  = 1610612736 (1536.0MB)    # 老年代大小

Heap Usage:
G1 Heap:
   regions  = 2048
   capacity = 2147483648 (2048.0MB)
   used     = 1572864000 (1500.0MB)     # 已使用堆内存
   free     = 574619648 (548.0MB)
   73.24% used

G1 Young Generation:
   Eden regions: 512 -> capacity = 536870912 (512.0MB)
   Survivor regions: 32 -> capacity = 33554432 (32.0MB)

G1 Old Generation:
   regions = 956 -> capacity = 1543503872 (1472.0MB)
   used    = 1472.0MB                   # 老年代几乎占满
```

**jmap -histo 输出解读**：

```
 num     #instances         #bytes  class name
----------------------------------------------
   1:       1200000      96000000  [B          # byte[] 数组
   2:        800000      64000000  [C          # char[] 数组
   3:       1000000      24000000  java.lang.String
   4:        500000      24000000  com.example.service.Order
   5:        300000      19200000  java.util.HashMap$Node
```

**排查实战**：

```bash
# 1. 快速判断内存泄漏
jstat -gcutil <pid> 2000 5
# 如果 FGC 频繁（10分钟内多次）且 O区使用率不降 -> 内存泄漏

# 2. 找到占用内存最多的对象
jmap -histo:live <pid> | head -30
# 找到 byte[], Order, String 等占用最大的类

# 3. 导出堆快照（线上需谨慎，会STW）
jmap -dump:live,format=b,file=/tmp/heap_$(date +%Y%m%d_%H%M%S).hprof <pid>

# 4. 将hprof文件下载到本地，用MAT分析
```

> **线上注意事项**：`jmap -histo:live` 和 `jmap -dump:live` 会触发Full GC，造成STW（Stop-The-World）。生产环境高峰期间慎用，建议在低峰期操作。如果不想触发Full GC，去掉 `:live` 参数，但导出的文件会更大。

### 3.5 MAT（Memory Analyzer Tool）—— 堆分析

MAT是Eclipse开发的堆内存分析工具，是定位内存泄漏的最终利器。

**核心功能**：

1. **Leak Suspects（泄漏嫌疑分析）**：一键分析，MAT自动找出最可能的内存泄漏点
2. **Dominator Tree（支配树）**：按对象保留的堆大小排序，快速定位大对象
3. **Histogram（直方图）**：与jmap -histo类似，但可交互式查看引用关系
4. **GC Roots追踪**：从GC Roots到对象的完整引用链，判断对象为何没有被回收

**使用步骤**：

```
1. 使用 jmap -dump 导出堆快照
2. 在MAT中打开 .hprof 文件
3. 点击 "Leak Suspects Report" 生成泄漏分析
4. 查看 "Problem Suspects" 查看疑似泄漏点
5. 点击 Details 查看引用链
6. 在代码中找到对应的根对象，修复泄漏
```

**关键视图说明**：

- **Leak Suspects**：给出最可能的内存泄漏点，描述对象大小和保留原因
- **Dominator Tree**：按 "Retained Heap"（保留堆）排序，数值越大说明该对象及其子对象占用的总内存越多
- **Path to GC Roots**：显示GC Roots到目标对象的引用路径，排除不应有的强引用

**MAT替代工具**：

- **VisualVM**：免费可视化工具，集成了jstat/jstack/jmap功能，适合快速观察
- **JProfiler**：商业工具，功能全面，支持CPU/Memory/线程等全面分析
- **Arthas**：阿里开源工具（见下节），可以部分替代MAT的在线分析

### 3.6 jinfo —— JVM参数查看与修改

```bash
# 查看所有JVM参数
jinfo <pid>

# 查看指定参数
jinfo -flag MaxHeapSize <pid>           # 查看最大堆大小
jinfo -flag PrintGCDetails <pid>        # 查看GC日志是否开启
jinfo -flags <pid>                      # 查看JVM参数（非默认值）

# 动态修改JVM参数（仅限manageable参数）
jinfo -flag +PrintGCDetails <pid>       # 开启GC日志
jinfo -flag -PrintGCDetails <pid>       # 关闭GC日志
jinfo -flag HeapDumpPath=/tmp/ <pid>    # 设置堆转储路径

# 注意：不是所有参数都支持动态修改，不可修改的参数会报错
```

### 3.7 Arthas（阿里开源诊断工具，重点推荐）

Arthas是阿里巴巴开源的Java诊断工具，无需修改代码、无需重启应用，即可在线排查问题，是Java线上排查的神器。

```bash
# 安装与启动
curl -O https://arthas.aliyun.com/arthas-boot.jar
java -jar arthas-boot.jar              # 选择目标Java进程
# 或直接指定PID
java -jar arthas-boot.jar <pid>

# 在支持的环境中也可用
wget -O arthas-boot.jar https://arthas.aliyun.com/arthas-boot.jar && java -jar arthas-boot.jar
```

#### dashboard —— 实时数据面板

```bash
# 输入dashboard后显示实时面板，每5秒刷新一次
dashboard

# 显示内容：
# - 线程信息：活跃线程数、守护线程数
# - 内存信息：堆各区域使用量、GC统计
# - 系统信息：CPU使用率、负载等
# - GC信息：YGC次数、FGC次数、GC耗时

# 等同于：top + jstat -gcutil + free 的集合体
```

#### thread —— 线程分析

```bash
thread                               # 显示所有线程及其CPU使用率
thread -b                            # 检测死锁（自动检测并显示死锁线程）
thread -n 3                          # 显示CPU最忙的前3个线程（定位CPU飙高）
thread -n 3 -i 2000                  # 每2秒采集一次，共3次（避免偶然性）

thread <tid>                         # 显示指定线程的栈信息
thread --state BLOCKED               # 显示所有阻塞状态的线程

# 定位CPU高最实用的方式：
# 在Arthas中直接执行 thread -n 3，无需手动转换线程ID
```

#### watch —— 方法观测

`watch` 可以监控方法的入参、返回值、异常，是排查业务逻辑问题的核心命令。

```bash
# 监控方法调用（观察返回值）
watch com.example.service.OrderService getOrder "{params, returnObj}"

# 监控方法异常
watch com.example.service.OrderService getOrder "{params, throwExp}"

# 展开对象深度（-x控制展开层级）
watch com.example.service.OrderService getOrder "{params, returnObj}" -x 3

# 条件过滤（只观测耗时超过100ms的调用）
watch com.example.service.OrderService getOrder "{params, returnObj}" "#cost>100"

# 限制输出次数（-n）
watch com.example.service.OrderService getOrder "{params, returnObj}" -n 5

# 按照方法执行条件观察（只有当第一个参数="123"时才观测）
watch com.example.service.OrderService getOrder "{params, returnObj}" "params[0]=="123""
```

**参数说明**：

| 表达式 | 含义 |
|--------|------|
| `params` | 方法入参数组 |
| `returnObj` | 返回值 |
| `throwExp` | 抛出的异常 |
| `target` | 当前对象（this） |
| `#cost` | 方法执行耗时（毫秒） |
| `-x N` | 展开对象的深度，默认1 |

#### trace —— 方法调用链路追踪

`trace` 是最强大的定位性能瓶颈的命令，可以追踪方法内部每个子调用的耗时。

```bash
# 追踪方法调用链路
trace com.example.service.OrderService getOrder

# 只显示耗时超过100ms的调用
trace com.example.service.OrderService getOrder "#cost>100"

# 跳过JDK类库的追踪
trace --skipJDKMethod false com.example.service.OrderService getOrder

# 按调用次数采样（-n）
trace com.example.service.OrderService getOrder -n 3

# 输出示例：
# `---ts=2024-06-13 14:23:45 thread=http-nio-8080-exec-10
#     `---[10.1234ms] com.example.service.OrderService:getOrder()
#         +---[5.0123ms] com.example.service.OrderService:validateOrder()   # 验证花了5ms
#         +---[3.0012ms] com.example.service.OrderService:calculatePrice()  # 计算花了3ms
#         `---[2.0011ms] com.example.service.OrderService:saveOrder()       # 保存花了2ms
```

#### jad —— 反编译

`jad` 可以实时反编译JVM中加载的类，验证部署的代码是否是最新版本。

```bash
# 反编译整个类
jad com.example.controller.OrderController

# 反编译指定方法
jad com.example.controller.OrderController getOrder

# 仅显示类信息（不显示源代码）
jad --source-only com.example.controller.OrderController

# 反编译并显示行号（对比行号定位问题代码）
jad --lineNumber com.example.controller.OrderController

# 应用场景：怀疑部署的代码不是最新版本时
# 直接反编译对比，比查git版本还快
```

#### monitor —— 方法调用统计

```bash
# 监控方法调用统计（每5秒输出一次）
monitor -c 5 com.example.service.OrderService getOrder

# 输出示例：
# timestamp            class                            method    total  success  fail  avg-rt(ms)
# 2024-06-13 14:23:45  com.example.service.OrderService getOrder  120    118      2     45.23

# 非常适合监控接口的成功率、平均响应时间
```

#### tt（TimeTunnel）—— 时空隧道

`tt` 可以记录方法的每次调用，支持回放历史调用，是复现问题的利器。

```bash
# 记录方法的每次调用
tt -t com.example.service.OrderService getOrder

# 查看历史记录
tt -l

# 查看某次调用的详情（-i 指定索引）
tt -i 1000

# 回放某次调用（重新执行）
tt -i 1000 -p

# 应用场景：线上偶发问题，记录下异常调用后，回放分析
```

#### redefine —— 热替换class

`redefine` 可以在不重启JVM的情况下替换class文件，用于线上紧急修复。

```bash
# 将编译好的 class 文件热替换到 JVM 中
redefine -p /tmp/OrderService.class

# 也可以替换多个类
redefine -p /tmp/OrderService.class /tmp/OrderController.class
```

> **注意**：`redefine` 不能添加/删除字段或方法，只能修改方法体。这是最后的手段，正规修复仍然需要走发布流程。

---

## 四、CPU/内存/网络问题排查流程

### 4.1 CPU 100% 排查流程

这是线上最常见的故障场景之一，以下是标准排查流程：

```
┌──────────────────────────────────────────────────────────────────┐
│                       排查CPU飙高                              │
│                                                                  │
│  1. top 找到高CPU进程PID                                         │
│     ↓                                                            │
│  2. top -H -p <PID> 找到高CPU线程TID                             │
│     ↓                                                            │
│  3. printf "%x\n" <TID> 转为十六进制 nid                         │
│     ↓                                                            │
│  4. jstack <PID> | grep -A 30 "nid=0x<nid>" 定位代码            │
│     ↓                                                            │
│  5. 在IDE中查看对应源代码，分析为什么CPU高                       │
│                                                                  │
│  替代方案：Arthas直连后 thread -n 3                              │
└──────────────────────────────────────────────────────────────────┘
```

**完整命令执行示例**：

```bash
# Step 1: 找到CPU最高的进程
top -c
# 按 P 键排序，发现 PID=5678 的 Java 进程 CPU > 100%

# Step 2: 查看进程内线程CPU消耗
top -H -p 5678
# 发现 TID=5790 的线程 CPU 最高

# Step 3: 转换线程ID为十六进制
printf "%x\n" 5790
# 输出: 169e

# Step 4: 在线程栈中定位
jstack 5678 | grep -A 30 "nid=0x169e"

# 输出可能是：
# "http-nio-8080-exec-10" #30 daemon prio=5 os_prio=0 tid=0x00007f...
#   nid=0x169e runnable [0x00007f...
#   java.lang.Thread.State: RUNNABLE
#       at com.example.service.OrderService.calculatePrice(OrderService.java:85)
#       at com.example.service.OrderService.getOrder(OrderService.java:120)

# 此时打开 OrderService.java:85 行，查看是否有死循环、频繁计算、正则回溯等问题
```

**常见原因及解决方案**：

| 现象 | 原因 | 解决 |
|------|------|------|
| 代码中有while(true)死循环 | 逻辑错误 | 修复代码，增加退出条件 |
| 正则表达式回溯 | 复杂的正则匹配大量数据 | 优化正则，使用 `^` `$` 锚定 |
| 频繁GC导致CPU高 | 内存不足导致GC线程持续运行 | 扩大堆内存或排查内存泄漏 |
| 大量线程在RUNNABLE状态 | 线程池过大，或请求量突增 | 限流或扩容 |
| JSON序列化大对象 | Jackson/Gson序列化耗时 | 缓存序列化结果或减少数据量 |

### 4.2 内存OOM排查流程

当应用出现 `OutOfMemoryError` 或频繁Full GC时，按以下流程排查：

```
┌────────────────────────────────────────────────────────────────────┐
│                       排查内存OOM                                 │
│                                                                    │
│  1. jstat -gcutil <PID> 2000 观察GC情况                          │
│     ↓ FGC频繁且O区不降 → 确认内存泄漏                             │
│  2. jmap -histo:live <PID> | head-30 查看对象统计                │
│     ↓ 找到占用最多的对象类型                                       │
│  3. jmap -dump:live,format=b,file=heap.hprof <PID> 导出堆        │
│     ↓ 下载hprof到本地                                              │
│  4. 用MAT打开分析，查看Leak Suspects                              │
│     ↓                                                              │
│  5. GC Root路径追踪，定位到代码                                    │
│                                                                    │
│  如果提前加了 -XX:+HeapDumpOnOutOfMemoryError                     │
│  OOM时会自动生成堆快照                                             │
└────────────────────────────────────────────────────────────────────┘
```

**关键判断指标**：

```bash
# 用jstat持续观察
jstat -gcutil 5678 2000

# 如果输出显示：
# S0  S1   E    O    M   YGC  YGCT  FGC  FGCT  GCT
# 0   0    95   98   95  500  12.5   50   25.0  37.5
#
# 注意：Full GC 已经发生了50次，老年代使用率98%仍然不降
# 每次FGC后 O区使用率下降极少 → 确定内存泄漏
# 正常情况FGC应该很少（几小时甚至几天一次）
```

**典型内存泄漏模式**：

```bash
# 情况1：HashMap/String/byte[] 特别多
# 通常是没有清理的缓存（如往ConcurrentHashMap中不断put且不remove）
# 或是字符串截取导致大char[]无法释放

# 情况2：自定义对象特别多（如 Order 实例）
# 通常是集合类持有引用未释放

# 情况3：Class对象加载过多
# 通常是动态代理/反射生成太多类，或框架实现不合理
```

**预防措施**：

```bash
# JVM启动参数中务必添加：
-XX:+HeapDumpOnOutOfMemoryError       # OOM时自动导出堆快照
-XX:HeapDumpPath=/data/app/dump/      # 快照保存路径
-XX:MetaspaceSize=256m                 # 元空间大小，防止类过多OOM
-Xms2g -Xmx2g                         # 设置堆大小（最小值=最大值避免动态调整）
```

### 4.3 死锁排查

死锁是指两个或多个线程互相持有对方需要的锁，导致所有线程都无法继续执行。

**Arthas方案（推荐）**：

```bash
# 一行命令即可检测死锁
thread -b
# 如果存在死锁，会显示：
# "thread-1": waiting for java.lang.String@<hash>
# "thread-2": waiting for java.lang.String@<hash>
```

**jstack方案**：

```bash
jstack -l <pid> | grep -A 50 "deadlock"

# 输出会包含：
# Found one Java-level deadlock:
# =============================
# "thread-1":
#   waiting to lock <0x000000076b5f3e78> (a java.lang.String)
#   which is held by "thread-2"
# "thread-2":
#   waiting to lock <0x000000076b5f3e48> (a java.lang.String)
#   which is held by "thread-1"
```

**死锁代码示例（反面教材）**：

```java
// 以下代码必然导致死锁
public void transfer(Account from, Account to, double amount) {
    synchronized (from) {
        synchronized (to) {
            from.debit(amount);
            to.credit(amount);
        }
    }
}
// 线程A: transfer(a, b, 100)
// 线程B: transfer(b, a, 50)
// 两个线程死锁
```

**预防死锁的最佳实践**：

```java
// 方法1：固定锁顺序（按hashCode排序）
int fromHash = System.identityHashCode(from);
int toHash = System.identityHashCode(to);
Object firstLock = fromHash < toHash ? from : to;
Object secondLock = fromHash < toHash ? to : from;
synchronized (firstLock) {
    synchronized (secondLock) {
        // 操作
    }
}

// 方法2：使用ReentrantLock.tryLock()（带超时）
if (lock.tryLock(1, TimeUnit.SECONDS)) {
    try {
        // 业务逻辑
    } finally {
        lock.unlock();
    }
} else {
    // 获取锁超时，做补偿处理
}
```

### 4.4 接口慢排查流程

```bash
# Step 1: 用 Arthas trace 追踪接口调用链路
trace com.example.controller.OrderController getOrder "#cost>200"

# 输出显示：
# `---[1200.12ms] OrderController.getOrder()
#     +---[150.01ms] OrderService.getOrderById()
#     +---[800.50ms] OrderService.getUserInfo()     ← 这里最慢！
#     `---[50.01ms]  OrderService.getProductList()

# Step 2: 深入追踪慢方法
trace com.example.service.OrderService getUserInfo "#cost>500"

# Step 3: 分析慢的原因
# - 如果耗在网络IO：检查下游服务/数据库响应
# - 如果耗在CPU计算：看是否存在频繁序列化、加解密、大循环
# - 如果耗在锁等待：使用 thread -b 检查死锁
```

**接口慢原因速查**：

| 耗时模式 | 可能原因 | 排查方向 |
|----------|----------|----------|
| 网络IO耗时高 | 下游服务慢、数据库慢、Redis慢 | 排查下游依赖、SQL慢查询 |
| CPU计算耗时高 | 序列化、加密、大循环、正则 | 优化算法、缓存结果 |
| 锁等待耗时高 | 锁竞争激烈、死锁 | 优化锁粒度、尝试无锁数据结构 |
| GC耗时高 | Full GC频繁 | 内存泄漏或GC参数不合适 |

### 4.5 Full GC频繁排查流程

```bash
# Step 1: 确认是否FGC频繁
jstat -gcutil <pid> 2000 10

# Step 2: 判断原因
# 情况A：O区在FGC后使用率明显下降（正常），但很快又涨回去
#   → 对象生命周期合理，但对象创建太快，可以考虑加大O区或-Xmx
# 情况B：O区在FGC后使用率几乎不降
#   → 内存泄漏，需要进行堆转储分析

# Step 3: 如果是内存泄漏，导出堆快照
jmap -dump:live,format=b,file=heap.hprof <pid>

# Step 4: 使用MAT分析，定位泄漏源头

# Step 5: 快速缓解措施（临时）
# - 暂时扩大堆内存
# - 重启服务
# - 必要时限流
```

---

## 五、日志管理

完善的日志系统是排查问题的前提。没有日志的排查如同盲人摸象。

### 5.1 日志框架体系

Java日志框架经过多年发展，形成了以 **SLF4J（门面）+ Logback（实现）** 为标准的架构。

**为什么选择SLF4J + Logback？**

```xml
<!-- Maven依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- Spring Boot自动引入SLF4J+Logback -->
</dependency>
```

**日志级别**：

```
ERROR > WARN > INFO > DEBUG > TRACE
```

| 级别 | 生产环境 | 说明 |
|------|----------|------|
| ERROR | 使用 | 影响功能的严重错误，需要立即关注 |
| WARN | 使用 | 不正常的但可恢复的情况 |
| INFO | 使用（推荐） | 关键业务流程记录，如请求开始结束、订单创建等 |
| DEBUG | 关闭 | 开发调试信息，生产关闭 |
| TRACE | 关闭 | 最细粒度的追踪信息 |

> **生产建议**：生产环境设置为 INFO 级别。过高的级别（DEBUG）会产生大量日志，影响性能并占满磁盘。过低的级别（ERROR）会丢失关键信息。

### 5.2 Logback配置

以下是一个生产级别的 `logback-spring.xml` 配置示例：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true" scanPeriod="60 seconds">

    <!-- 彩色日志依赖 -->
    <conversionRule conversionWord="clr" converterClass="org.springframework.boot.logging.logback.ColorConverter"/>

    <!-- 日志格式 -->
    <property name="CONSOLE_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"/>

    <property name="FILE_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"/>

    <!-- 日志文件路径 -->
    <property name="LOG_PATH" value="${LOG_PATH:-/var/log/app}"/>
    <property name="APP_NAME" value="${APP_NAME:-app}"/>

    <!-- 控制台输出（开发环境使用） -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 滚动文件输出（生产环境使用） -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- 按天滚动，保留30天 -->
            <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <!-- 每个日志文件最大100MB -->
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
        <encoder>
            <pattern>${FILE_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 性能日志（独立文件） -->
    <appender name="PERF_LOG" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}-perf.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${APP_NAME}-perf.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
        <encoder>
            <pattern>${FILE_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 错误日志单独输出 -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}-error.log</file>
        <!-- 只记录ERROR级别 -->
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${APP_NAME}-error.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxHistory>60</maxHistory>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
        <encoder>
            <pattern>${FILE_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- Spring Profile 环境区分 -->
    <springProfile name="dev">
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <springProfile name="test,prod">
        <root level="INFO">
            <appender-ref ref="FILE"/>
            <appender-ref ref="ERROR_FILE"/>
        </root>
    </springProfile>

    <!-- 性能日志的logger -->
    <logger name="com.example.app.perf" level="INFO" additivity="false">
        <appender-ref ref="PERF_LOG"/>
    </logger>
</configuration>
```

### 5.3 日志最佳实践

#### 1. 使用占位符，不要字符串拼接

```java
// ❌ 错误：字符串拼接会执行 toString()，即使日志级别不输出
logger.debug("Order id: " + orderId + ", user: " + user.getName());

// ✅ 正确：使用占位符，只在输出时才拼接
logger.debug("Order id: {}, user: {}", orderId, user.getName());
```

#### 2. 异常信息必须包含堆栈

```java
// ❌ 错误：丢失堆栈信息
logger.error("Error processing order: " + e.getMessage());

// ✅ 正确：传入异常对象，输出完整堆栈
logger.error("Error processing order: {}", orderId, e);
```

#### 3. 合理使用日志级别

```java
// 关键业务流程记录INFO
logger.info("Order created successfully, orderId={}, amount={}", orderId, amount);

// 业务异常使用WARN（非程序错误，如参数校验失败）
logger.warn("Invalid order request: {}", validationError);

// 系统错误使用ERROR
logger.error("Database connection failed", e);

// 调试信息使用DEBUG（生产不输出）
logger.debug("Processing payment, orderId={}, step={}", orderId, step);
```

#### 4. 敏感信息脱敏

```java
// 对手机号、身份证号等敏感信息进行脱敏
logger.info("User login, phone={}", maskPhone(phone));
// 输出：User login, phone=138****1234
```

#### 5. 日志文件管理

```bash
# 生产环境日志切割策略
# - 按天切割，避免单个文件过大
# - 保留30天，或根据磁盘容量调整
# - 设置最大文件大小（如100MB），避免单文件过大
# - 使用.gz压缩，减少磁盘占用

# 日志清理crontab
0 4 * * * find /var/log/app -name "*.log.gz" -mtime +30 -delete
```

#### 6. 日志链路追踪

```java
// 使用MDC（Mapped Diagnostic Context）传递链路信息
// 在过滤器或拦截器中设置
MDC.put("traceId", UUID.randomUUID().toString().replace("-", ""));
MDC.put("userId", userId);

// 在日志配置中引用
// %X{traceId} - 输出traceId
// %X{userId}  - 输出userId

// 输出示例：
// 2024-06-13 14:23:45.123 [http-nio-8080-exec-10] INFO [a1b2c3d4, user123] OrderService - ...

// 请求结束后清除
MDC.clear();
```

---

## 六、排查速查表

### CPU问题

| 步骤 | 命令 | 说明 |
|------|------|------|
| 1 | `top -c` | 找到高CPU进程PID |
| 2 | `top -H -p <pid>` | 找到高CPU线程TID |
| 3 | `printf "%x\n" <tid>` | TID转十六进制nid |
| 4 | `jstack <pid> \| grep -A 30 "nid=0x<nid>"` | 定位代码 |
| 替代 | `thread -n 3`（Arthas） | Arthas一键定位 |

### 内存问题

| 步骤 | 命令 | 说明 |
|------|------|------|
| 1 | `jstat -gcutil <pid> 2000` | 观察GC情况 |
| 2 | `jmap -histo:live <pid> \| head-30` | 对象统计 |
| 3 | `jmap -dump:live,format=b,file=heap.hprof <pid>` | 导出堆快照 |
| 4 | MAT分析hprof | 查看Leak Suspects |

### 死锁

| 步骤 | 命令 | 说明 |
|------|------|------|
| 1 | `thread -b`（Arthas） | 一键检测死锁 |
| 2 | `jstack -l <pid> \| grep "deadlock" -A 50` | 传统方式 |

### 接口慢

| 步骤 | 命令 | 说明 |
|------|------|------|
| 1 | `trace <class> <method> "#cost>200"` （Arthas） | 追踪耗时链 |
| 2 | `monitor -c 5 <class> <method>`（Arthas） | 统计成功率/RT |

### Full GC频繁

| 步骤 | 命令 | 说明 |
|------|------|------|
| 1 | `jstat -gcutil <pid> 2000` | 确认FGC频率 |
| 2 | `jstat -gccause <pid>` | 查看GC原因 |
| 3 | `jmap -dump:live,format=b,file=heap.hprof <pid>` | 导出堆分析 |

### 进程/端口

| 场景 | 命令 |
|------|------|
| 查看Java进程 | `jps -lv` 或 `ps -ef \| grep java` |
| 查看端口占用 | `lsof -i :8080` 或 `ss -tlnp \| grep 8080` |
| 查看进程打开文件 | `lsof -p <pid>` |
| 查看进程监听端口 | `netstat -tlnp \| grep <pid>` |

### 磁盘

| 场景 | 命令 |
|------|------|
| 磁盘空间 | `df -h` |
| 目录大小 | `du -sh <dir>` |
| 大文件查找 | `find / -type f -size +1G` |
| 已删文件未释放 | `lsof \| grep deleted` |

### 日志

| 场景 | 命令 |
|------|------|
| 实时跟踪日志 | `tail -f app.log` |
| 实时过滤错误 | `tail -f app.log \| grep ERROR` |
| 错误上下文 | `grep -A 10 -B 5 "ERROR" app.log` |
| 统计错误频率 | `grep -o "Exception: [^ ]*" app.log \| sort \| uniq -c \| sort -rn` |

---

> **最后的话**：线上排查最重要的不是记住所有命令，而是**建立排查思维**：先看现象（Top/Free/DF）→ 缩小范围（Jstat/Jstack）→ 精准定位（Jmap/Arthas）→ 解决验证。结合本文的工具和技术，建立自己的排查体系，遇到线上问题才能从容应对。
