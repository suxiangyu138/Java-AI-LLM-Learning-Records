# 快速吃透 Linux

定位：**后端/运维通用、日常操作+服务部署够用、全程可直接复制命令**，零基础快速上手。

***

## 一、Linux 核心认知（3句话）

1. Linux 是**多用户、多任务、开源**服务器操作系统，企业服务器90%以上都是 Linux。
2. 一切皆文件：硬件、进程、目录、网络配置，全部以**文件**形式管理。
3. 核心操作：**命令行**，权限严格，区分大小写，路径、命令全部严格区分。

***

## 二、目录结构（必背，固定不变）

```
/           根目录，最顶层
├── etc/    配置文件目录（nginx、mysql、环境变量都在这）
│   ├── profile     系统级环境变量
│   ├── fstab       开机自动挂载配置
│   └── systemd/    系统服务配置
├── home/   普通用户家目录，个人文件存放
├── root/   管理员root家目录
├── usr/    软件、系统程序安装目录
│   ├── local/      用户自行编译安装的软件
│   ├── bin/        普通用户可执行命令
│   └── sbin/       管理员可执行命令
├── var/    日志、缓存、运行数据
│   ├── log/        系统/应用日志（messages、secure）
│   └── run/        进程PID等运行时文件
├── tmp/    临时文件，重启自动清空（所有用户可写）
├── bin/    基础命令（ls、cd、cp 等，已软链接到 usr/bin）
├── sbin/   管理员命令（已软链接到 usr/sbin）
├── dev/    设备文件（硬盘、终端、零设备等）
├── proc/   进程和内核信息虚拟文件系统（不占磁盘）
│   ├── cpuinfo      CPU信息
│   ├── meminfo      内存信息
│   └── 数字/        对应PID的进程目录
├── opt/    第三方大型软件安装目录（如 jdk、tomcat）
├── boot/   内核、引导文件（grub、vmlinuz）
└── mnt/    临时挂载点（挂载U盘、外接硬盘）
```

> **技巧**：proc 和 dev 是虚拟文件系统，不占磁盘空间，但可以像普通文件一样 cat 查看。

***

## 三、最核心高频命令（开发必用）

### 1. 目录切换 & 查看

```bash
pwd                 # 查看当前所在路径
ls                  # 查看当前目录文件
ls -l               # 详细列表展示（权限、大小、时间）
ls -a               # 显示隐藏文件
cd 目录名           # 进入指定目录
cd ..               # 返回上一级目录
cd /                # 回到根目录
cd ~                # 回到当前用户家目录
```

### 2. 文件/文件夹 增删改查

```bash
mkdir test          # 新建文件夹 test
rmdir test          # 删除空文件夹

touch a.txt         # 新建空文件
rm a.txt            # 删除文件
rm -rf 文件夹       # 强制删除文件夹+所有内容（慎用！）

# 复制
cp 源文件 目标路径
cp -r 文件夹 目标路径  # 递归复制文件夹

# 移动/重命名
mv 旧名 新名         # 重命名
mv 文件 /目标路径    # 移动文件
```

### 3. 文件查看 & 编辑

```bash
cat a.txt           # 一次性查看全部文件内容
less a.txt          # 分页查看，q 退出
tail -f a.txt       # 实时监听文件（看日志神器）

# 极简文件编辑（必会）
vi a.txt
# 按 i 进入编辑模式
# 编辑完成按 Esc
# 输入 :wq 保存并退出
# 输入 :q! 不保存强制退出
```

### 4. 权限管理（Linux 核心）

```bash
chmod 755 文件名     # 修改文件权限
chown 用户:组 文件名  # 修改文件所属用户
```

### 5. 系统信息 & 进程

```bash
hostname            # 查看主机名
uname -r            # 查看系统内核
df -h               # 查看磁盘占用
free -h             # 查看内存占用

ps -ef              # 查看所有进程
kill -9 进程ID      # 强制杀死进程
```

### 6. 网络操作

```bash
ip addr             # 查看本机IP
ping 域名/IP        # 测试网络连通
curl 网址           # 访问接口/网页（后端调试常用）
```

### 7. 搜索 & 过滤

```bash
grep "关键词" a.txt       # 在文件中搜索关键词
ps -ef | grep java        # 过滤查找java进程
find / -name "xxx"        # 全盘搜索指定文件
```

### 8. 压缩和解压

```bash
# 压缩
tar -zcvf test.tar.gz 文件夹名
# z → gzip格式  c → 创建  v → 显示过程  f → 文件名

# 解压
tar -zxvf test.tar.gz
# x → 解压

# 其他常用格式
tar -jcvf test.tar.bz2 文件夹名    # bzip2压缩（更小）
tar -jxvf test.tar.bz2             # bzip2解压
unzip test.zip                      # 解压zip包
zip -r test.zip 文件夹名            # 压缩为zip
```

### 9. 重定向与管道（核心中的核心）

```bash
# 输出重定向（覆盖）
ls > result.txt           # ls的结果写入文件（覆盖原内容）

# 输出重定向（追加）
ls >> result.txt          # ls的结果追加到文件末尾

# 错误重定向
ls 不存在路径 2> error.txt    # 只把错误信息写入文件
ls 存在路径 2> /dev/null      # 丢弃错误信息（黑洞设备）

# 合并输出和错误
java -jar app.jar > log.out 2>&1   # 标准输出+标准错误都写入同一个文件

# 管道 |  将前一个命令的输出作为后一个命令的输入
ps -ef | grep java        # 查进程 → 过滤出java
cat a.txt | wc -l         # 看内容 → 统计行数
history | tail -20        # 历史命令 → 只显示最后20条

# 输入重定向
mysql < backup.sql        # 把SQL文件导入数据库
```

### 10. 文本处理三剑客（grep / sed / awk）

```bash
# ===== grep 文本搜索 =====
grep "error" app.log                     # 搜索含error的行
grep -i "error" app.log                  # 忽略大小写
grep -n "error" app.log                  # 显示行号
grep -c "error" app.log                  # 只显示匹配行数
grep -v "info" app.log                   # 排除含info的行（取反）
grep -E "error|warn" app.log             # 正则匹配多个关键词（或）
grep -A 3 "error" app.log                # 匹配行及后面3行
grep -B 2 "error" app.log                # 匹配行及前面2行
grep -r "TODO" ./src/                    # 递归搜索目录下所有文件

# ===== sed 流编辑器（查找替换神器）=====
sed 's/旧文本/新文本/g' a.txt           # 替换文本（仅输出，不改文件）
sed -i 's/旧文本/新文本/g' a.txt        # 替换并直接修改文件
sed '/关键词/d' a.txt                    # 删除含关键词的行
sed -n '5,10p' a.txt                     # 只显示第5到第10行
sed -i 's/^/prefix_/' a.txt              # 每行开头加前缀

# ===== awk 文本分析（按列处理神器）=====
awk '{print $1}' access.log              # 打印第1列
awk '{print $1, $3}' access.log          # 打印第1和第3列
awk -F ':' '{print $1}' /etc/passwd      # 指定冒号为分隔符
awk '{sum+=$1} END {print sum}' num.txt  # 第1列求和
df -h | awk '{print $5}'                 # 提取磁盘占用百分比列
```

### 11. 其他高频实用命令

```bash
wc -l a.txt              # 统计行数
wc -w a.txt              # 统计单词数
wc -c a.txt              # 统计字节数

sort a.txt               # 排序（按字母）
sort -n a.txt            # 按数字排序
sort -r a.txt            # 倒序
sort -u a.txt            # 排序后去重

uniq a.txt               # 去重（需要先sort）
sort a.txt | uniq -c     # 去重并统计出现次数

head -5 a.txt            # 显示前5行（默认10行）
tail -20 a.txt           # 显示最后20行（默认10行）

xargs                    # 将标准输入转换为命令参数
find . -name "*.log" | xargs rm -f   # 找到所有log文件并删除

alias                    # 查看所有别名
alias ll='ls -lha'       # 设置别名（写入 ~/.bashrc 永久生效）
unalias ll               # 取消别名

history                  # 查看命令历史
!123                     # 执行历史命令中的第123条
!!                       # 执行上一条命令
!$                       # 引用上一条命令的最后一个参数

tee                      # 同时输出到屏幕和文件
ls -l | tee result.txt   # 屏幕看到结果同时写入文件

diff a.txt b.txt         # 对比两个文件的差异
md5sum a.txt             # 计算文件的MD5校验值（验证文件完整性）
```

***

## 四、用户与权限基础

1. `root`：超级管理员，最高权限，生产环境尽量不直接使用
2. 普通用户：权限受限，保证系统安全
3. 权限标识：
   - `r` 读（4）、`w` 写（2）、`x` 执行（1）
4. 切换用户

```bash
su root             # 切换到root
su 普通用户名       # 切换普通用户
sudo 命令           # 以root身份执行一条命令（免切换）
```

### 权限数字速查表（必背）

```
权限数字 = r(4) + w(2) + x(1) 的组合

数字   权限    含义
 7    rwx     读+写+执行（完全控制）
 6    rw-     读+写（一般文件）
 5    r-x     读+执行（脚本、目录）
 4    r--     只读
 3    -wx     写+执行（极少用）
 2    -w-     只写
 1    --x     只执行
 0    ---     无权限

常用组合：
  755  →  所有者rwx + 同组r-x + 其他人r-x    （脚本、目录最常用）
  644  →  所有者rw- + 同组r-- + 其他人r--    （普通文件最常用）
  777  →  所有人全部权限（极度危险，不要用！）
  600  →  所有者rw- + 同组--- + 其他人---    （私密文件，如密钥）
```

### 符号方式修改权限

```bash
chmod u+x 脚本.sh        # 给所有者(u)加执行权限
chmod g-w a.txt          # 给同组(g)去掉写权限
chmod o= a.txt           # 清空其他人(o)的所有权限
chmod a+r a.txt          # 所有人(a)加读权限
# u=所有者  g=同组  o=其他人  a=所有人
# +加权限  -减权限  =设权限
```

### 用户和组管理

```bash
useradd 用户名                      # 新增用户
passwd 用户名                       # 设置/修改密码
userdel -r 用户名                   # 删除用户及其家目录
usermod -aG 组名 用户名             # 把用户加入某个组（a追加，G附加组）

groupadd 组名                       # 新建用户组
groups 用户名                       # 查看用户属于哪些组

whoami                              # 查看当前登录用户名
id 用户名                           # 查看用户UID、GID和所属组
who                                 # 查看当前登录的所有用户
w                                   # 查看当前登录用户及其操作
last                                # 查看登录历史记录
```

### 特殊权限（了解即可）

```bash
# SUID（4）：以文件所有者的身份执行，如 /usr/bin/passwd
chmod u+s 可执行文件

# SGID（2）：以文件所属组的身份执行
chmod g+s 可执行文件/目录

# Sticky Bit（1）：共享目录中，用户只能删自己的文件，如 /tmp
chmod +t 共享目录
chmod 1777 共享目录    # 等价于上面的命令
```

### umask（默认权限掩码）

```bash
umask               # 查看当前默认权限掩码（通常022）
umask 022           # 设置默认掩码
# 新建文件默认权限 = 666 - umask
# 新建目录默认权限 = 777 - umask
# 022 → 文件644(rw-r--r--)  目录755(rwxr-xr-x)
```

***

## 五、软件安装（CentOS / Ubuntu 双版本）

### CentOS（阿里云/腾讯云默认）

```bash
yum install -y 软件名
# 示例
yum install -y net-tools vim
```

### Ubuntu

```bash
apt update
apt install -y 软件名
```

***

## 六、systemd 服务管理（CentOS7+/Ubuntu16+ 必会）

`systemctl` 是现代 Linux 的服务管理核心，替代了旧的 `service` 命令。

```bash
# 服务运行控制
systemctl start nginx        # 启动服务
systemctl stop nginx         # 停止服务
systemctl restart nginx      # 重启服务
systemctl reload nginx       # 重新加载配置（不中断服务，更优雅）
systemctl status nginx       # 查看服务状态（含最新日志）

# 开机自启管理
systemctl enable nginx       # 设置开机自启
systemctl disable nginx      # 取消开机自启
systemctl is-enabled nginx   # 检查是否已设置开机自启

# 查看所有服务
systemctl list-units --type=service         # 列出所有运行中的服务
systemctl list-units --type=service --all   # 列出全部服务（含未运行的）
systemctl list-unit-files --type=service    # 列出所有服务的开机自启状态

# 查看系统状态
systemctl is-active nginx    # 检查服务是否正在运行
journalctl -u nginx -f       # 实时查看某个服务的日志
journalctl -u nginx --since "10 min ago"  # 查看最近10分钟的服务日志
systemctl daemon-reload      # 修改服务配置文件后重新加载（重要！）
```

***

## 七、环境变量（开发必知）

```bash
# 查看环境变量
env                          # 查看所有环境变量
echo $PATH                   # 查看PATH（可执行文件搜索路径）
echo $JAVA_HOME              # 查看某个具体变量
echo $HOME                   # 当前用户家目录
echo $USER                   # 当前用户名

# 临时设置（仅当前会话有效）
export MY_VAR="hello"        # 设置环境变量
export PATH=$PATH:/new/path   # 追加路径到PATH

# 永久生效（写入配置文件中，如下任一）
# 方式1：仅对当前用户有效
echo 'export JAVA_HOME=/usr/lib/jvm/java-8' >> ~/.bashrc
source ~/.bashrc              # 立即生效

# 方式2：对所有用户有效（需root权限）
echo 'export JAVA_HOME=/usr/lib/jvm/java-8' >> /etc/profile
source /etc/profile

# 配置文件加载顺序（知道即可）
# 登录shell：/etc/profile → ~/.bash_profile → ~/.bashrc
# 非登录shell：~/.bashrc
```

### 常用环境变量示例

```bash
# Java开发环境配置示例（写入 ~/.bashrc）
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk
export MAVEN_HOME=/opt/maven
export PATH=$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH
export CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
```

***

## 八、SSH 远程连接与管理

```bash
# 基本连接
ssh user@192.168.1.100               # 默认22端口连接
ssh -p 2222 user@192.168.1.100       # 指定端口连接
ssh -i ~/.ssh/my_key user@host       # 使用密钥文件连接

# 密钥登录（免密登录）配置
ssh-keygen -t rsa -b 4096            # 本地生成密钥对（一路回车）
ssh-copy-id user@192.168.1.100       # 把公钥复制到远程服务器
# 此后 ssh user@192.168.1.100 直接登录，无需密码

# 文件传输（scp）
scp a.txt user@host:/remote/path/        # 上传文件
scp -r 文件夹/ user@host:/remote/path/   # 上传文件夹
scp user@host:/remote/file ./            # 从远程下载
scp -P 2222 a.txt user@host:/path/       # 指定端口传输

# SSH配置文件（简化连接）~/.ssh/config
# Host my-server
#     HostName 192.168.1.100
#     User root
#     Port 2222
#     IdentityFile ~/.ssh/my_key
# 之后只需: ssh my-server
```

***

## 九、防火墙管理（firewalld / iptables）

```bash
# ===== firewalld（CentOS7+默认）=====
systemctl start firewalld              # 启动防火墙
systemctl enable firewalld             # 开机自启
firewall-cmd --state                   # 查看防火墙状态

# 开放端口
firewall-cmd --zone=public --add-port=8080/tcp --permanent   # 永久开放8080端口
firewall-cmd --zone=public --add-port=3306/tcp --permanent   # 开放MySQL端口
firewall-cmd --reload                  # 重新加载配置（开放端口后必须执行！）

# 查看已开放端口
firewall-cmd --zone=public --list-ports
firewall-cmd --zone=public --list-all

# 移除端口
firewall-cmd --zone=public --remove-port=8080/tcp --permanent
firewall-cmd --reload

# 完全关闭防火墙（仅开发环境）
systemctl stop firewalld
systemctl disable firewalld

# ===== 云服务器额外注意 =====
# 阿里云/腾讯云等还需在控制台的安全组中开放端口，否则外部仍无法访问
```

***

## 十、定时任务 crontab（自动化利器）

```bash
# crontab 格式：分 时 日 月 周  命令
# 示例
crontab -e                 # 编辑当前用户的定时任务
crontab -l                 # 查看当前用户的定时任务
crontab -r                 # 删除所有定时任务（小心！）

# 时间格式速查
*  *  *  *  *              # 每分钟执行
0  *  *  *  *              # 每小时整点执行
0  2  *  *  *              # 每天凌晨2点执行
0  3  *  *  0              # 每周日凌晨3点执行
0  0  1  *  *              # 每月1号0点执行
*/5 *  *  *  *              # 每5分钟执行一次
*/30 *  *  *  *             # 每30分钟执行一次
0  9-18 *  *  *             # 每天9点到18点每小时整点执行

# 实战示例
0 2 * * * /usr/bin/mysqldump -u root db_name > /backup/db_$(date +\%Y\%m\%d).sql
# 每天凌晨2点备份数据库

0 0 * * 0 find /logs -name "*.log" -mtime +7 -delete
# 每周日凌晨删除7天前的日志

*/10 * * * * /usr/local/app/health_check.sh >> /var/log/check.log 2>&1
# 每10分钟执行健康检查脚本并记录日志

# 查看定时任务日志
tail -f /var/log/cron              # CentOS
tail -f /var/log/syslog | grep CRON  # Ubuntu
```

***

## 十一、后台运行程序（部署必备）

### nohup 方式（传统、简单）

```bash
nohup java -jar app.jar > log.out 2>&1 &
# 作用：后端运行jar包，日志输出到 log.out，关闭终端不退出
```

### screen / tmux 方式（推荐，可随时重连）

```bash
# screen
screen -S myapp             # 创建新会话
# 在新会话中启动程序，然后 Ctrl+A D 分离
screen -ls                  # 查看所有会话
screen -r myapp             # 重新连接会话

# tmux
tmux new -s myapp           # 创建新会话
# Ctrl+B D 分离会话
tmux ls                     # 查看所有会话
tmux attach -t myapp        # 重新连接会话
```

### 最佳实践：用 systemd 管理 Java 应用

```bash
# 创建服务文件 /etc/systemd/system/myapp.service
```

```
[Unit]
Description=My Java Application
After=network.target

[Service]
Type=simple
User=appuser
WorkingDirectory=/opt/myapp
ExecStart=/usr/bin/java -jar /opt/myapp/app.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
systemctl daemon-reload
systemctl enable myapp
systemctl start myapp
# 之后 systemctl status myapp 查看状态
# 崩溃自动重启，服务器开机自动启动，比nohup可靠得多！
```

***

## 十二、磁盘与存储管理

```bash
df -h                    # 查看磁盘分区占用（最常用）
df -i                    # 查看inode使用情况

du -sh 目录名            # 查看目录总大小
du -sh * | sort -rh      # 当前目录下各文件/目录大小排序（找大文件神器）
du -sh * | sort -rh | head -10  # 只看最大的10个

lsblk                    # 查看所有磁盘和分区（块设备列表）
fdisk -l                 # 查看磁盘分区详情（需root）

mount /dev/sdb1 /mnt/data   # 挂载磁盘分区
umount /mnt/data            # 卸载挂载点
```

***

## 十三、开发高频实战场景（扩充版）

### 场景1：查看项目日志

```bash
tail -f /usr/local/app/logs/app.log
tail -n 200 app.log | grep -E "ERROR|WARN"    # 只看最近200行的错误和警告
```

### 场景2：排查端口占用

```bash
netstat -tulpn | grep 端口号         # 传统方式
ss -tulpn | grep 端口号              # 现代方式（更快）
lsof -i:端口号                       # 查看哪个进程占用了端口
```

### 场景3：找出占用CPU/内存最高的进程

```bash
top                        # 实时交互查看，按M按内存排序，按P按CPU排序
ps aux --sort=-%cpu | head -10   # 按CPU排序前10
ps aux --sort=-%mem | head -10   # 按内存排序前10
```

### 场景4：批量替换多个文件中的字符串

```bash
find /path -name "*.properties" -exec sed -i 's/旧IP/新IP/g' {} \;
# 把所有properties文件中的旧IP替换成新IP
```

### 场景5：查看日志中某时间段的内容

```bash
sed -n '/2025-01-01 10:00/,/2025-01-01 11:00/p' app.log
# 查看10点到11点之间的所有日志
```

### 场景6：快速清空大日志文件

```bash
> app.log               # 清空文件内容（比rm再touch快，不改变inode）
cat /dev/null > app.log # 等价方式
```

### 场景7：传文件到服务器

```bash
# scp 方式
scp -r ./dist/ user@host:/usr/share/nginx/html/

# rsync 方式（增量同步，更高效）
rsync -avz ./dist/ user@host:/usr/share/nginx/html/
# -a 归档模式  -v 显示详情  -z 压缩传输
```

### 场景8：一条命令完成部署（脚本化）

```bash
#!/bin/bash
# deploy.sh 部署脚本示例
mvn clean package -DskipTests
scp target/app.jar user@server:/opt/app/
ssh user@server "systemctl restart myapp && systemctl status myapp"
```

***

## 十四、极简学习路线（快速精通）

1. 熟记上面所有命令，做到随手敲
2. 掌握 `vi` 编辑器、日志查看、进程排查
3. 学会 **systemd 管理服务**，用 systemctl 替代 nohup
4. 学会 Linux 部署 Java 项目、Docker+Linux 联合使用
5. 掌握防火墙端口管理、SSH密钥登录
6. 了解定时任务、Shell脚本自动化运维

***

## 附赠：Linux 避坑提醒（升级版）

1. ⚠️ **高危命令**：禁止随便执行 `rm -rf /`、`chmod -R 777 /`、`mkfs` 等破坏性命令
2. ⚠️ **rm -rf 后一定跟路径**：如 `rm -rf ./test` 而不是 `rm -rf / test`（空格致命！）
3. ⚠️ **路径严格区分大小写**：`Home` ≠ `home`，空格不能乱加
4. ⚠️ **生产环境操作前先备份**：`cp 原文件 原文件.bak.$(date +%Y%m%d)`
5. ⚠️ **修改环境变量前先备份**：`cp ~/.bashrc ~/.bashrc.bak`
6. ⚠️ **慎用 `kill -9`**：-9 是强制杀死，进程无法做清理。先用 -15（默认）优雅停止
7. ⚠️ **防火墙操作**：`--permanent` 别漏了，操作后一定要 `--reload`
8. ⚠️ **crontab 中的百分号 `%`**：需要用 `\%` 转义，否则会被解释为换行
9. ⚠️ **rm 误删挽救**：一旦 `rm` 执行就几乎无法恢复。建议用 `alias rm='rm -i'`（删除前确认）
10. ✅ **好习惯**：操作前 `pwd` 确认当前路径、`ls` 确认目标文件

