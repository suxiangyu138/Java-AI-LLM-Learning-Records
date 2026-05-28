从 Java 后端视角深度剖析 Linux 常用指令（理论+实战，约1万字）
前言
对于 Java 后端开发者而言，Linux 不是“可选技能”，而是生产环境唯一标准操作系统。从服务器部署、JVM 调优、日志排查、容器化、微服务运维到线上故障应急，90% 以上工作都在 Linux 中完成。熟练掌握 Linux 常用指令，不仅是“会用”，更是定位问题、优化性能、保障稳定的核心能力。
本文站在Java 后端开发实战视角，不讲纯运维百科，只讲高频、救命、与 Java 生态强相关的 Linux 指令，兼顾原理、用法、场景、坑点与最佳实践，覆盖文件、进程、网络、磁盘、权限、日志、系统、管道、Shell 基础等模块，全文约 1 万字，可直接作为学习/面试/工作手册。
一、Linux 基础认知（Java 后端必懂理论）
1.1 为什么 Java 后端必须精通 Linux
云服务器（阿里云/腾讯云/AWS）默认 Linux
Docker/K8s 基于 Linux 内核
微服务、网关、注册中心、MQ、DB 均运行在 Linux
Windows 不支持生产级异步、多路复用、高并发模型
线上问题 80% 靠 Linux 指令定位：CPU 高、内存泄漏、句柄泄漏、端口占用、磁盘满、网络丢包等
1.2 Linux 核心哲学
一切皆文件（硬件、进程、网络、套接字都是文件）
小工具、组合强（管道 |、重定向 >、xargs）
文本驱动（日志、配置、输出均可 grep/awk/sed）
权限最小化（用户/组/权限位决定安全边界）
1.3 目录结构（Java 后端重点）
/：根目录
/home：普通用户家目录
/root：超级管理员目录
/etc：配置文件（nginx、redis、mysql、hosts、profile）
/usr/local：软件安装目录（Java、Tomcat、Nginx）
/var/log：系统/应用日志
/tmp：临时文件（重启可能清空）
/proc：虚拟文件系统（进程、内存、CPU 实时信息）
二、文件与目录操作（最基础、最高频）
2.1 目录切换：cd
cd /usr/local  # 绝对路径
cd ~           # 家目录
cd ..          # 上级
cd -           # 回到上一次目录
Java 场景：快速进入 JDK、Tomcat、应用部署目录。
2.2 查看目录：ls / ll
ls
ls -l    # 详细
ls -a    # 隐藏文件
ll       = ls -l（别名）
ll -h    # 人类可读大小
ll -rt   # 按时间倒序（查最新日志必备）
实战：ll -rt /var/log/ 快速定位最新日志。
2.3 创建/删除：mkdir / rmdir / rm
mkdir -p a/b/c    # 递归创建
rmdir dir         # 空目录
rm -f file        # 强制删文件
rm -rf dir        # 递归强制删除（高危！）
警告：rm -rf / 会删根目录，生产严禁直接执行。
2.4 复制/移动：cp / mv
cp file /tmp
cp -r dir /tmp
mv file /tmp
mv oldname newname  # 重命名
Java 场景：备份配置、迁移 war/jar 包。
2.5 查看文件内容（核心）
cat（全量输出）
cat application.yml
more / less（分页）
less catalina.out  # 支持上下翻页、搜索 /xxx
head / tail（头尾）
head -n 20 file
tail -n 100 file
tail -f file       # 实时刷新（Java 日志必备）
tail -F file       # 文件重建也能追（日志切割场景）
实战：tail -F catalina.out | grep ERROR 实时抓错误。
三、文件查找与文本处理（Java 排障神器）
3.1 find（文件查找）
find / -name "*.jar"               # 全局搜 jar
find /usr/local -name "java"
find . -size +100M                 # 大于100M
find . -mtime -1                   # 24小时内修改
find . -type f -delete             # 删除匹配文件
场景：找丢失的 jar、找大文件、清理日志。
3.2 grep（文本搜索，最常用）
grep "ERROR" catalina.out
grep -i "error" file              # 忽略大小写
grep -n "key" file                # 显示行号
grep -C 10 "Exception" file       # 上下文10行
grep -v "INFO"                    # 排除 INFO
grep -E "ERROR|Exception"         # 多关键词（正则）
实战：grep -C 20 "NullPointerException" catalina.out
3.3 awk（列切割，Java 日志统计）
awk '{print $1}' access.log       # 第一列
awk '/ERROR/{print $0}' log
awk '{sum+=$2} END {print sum}'   # 求和
场景：统计接口耗时、QPS、访问IP。
3.4 sed（替换/行操作）
sed 's/old/new/g' file
sed -i 's/debug/info/g' logback.xml  # 直接修改文件
场景：批量改配置、替换端口、环境变量。
3.5 管道 |（组合威力）
cat log | grep ERROR | wc -l
ps -ef | grep java | grep -v grep
四、用户与权限（Java 部署安全基础）
4.1 用户/组
id              # 查看当前用户
whoami
useradd app
passwd app
groupadd apps
usermod -aG apps app
最佳实践：生产不用 root 运行 Java 应用，专用 app 用户。
4.2 权限模型 rwx
r=4 w=2 x=1
属主、属组、其他用户
chmod 755 app.jar
chmod 644 config.yml
chown -R app:apps /usr/local/app
Java 坑：jar 无 x 权限无法执行；日志目录无 w 无法写。
4.3 sudo 提权
sudo vim /etc/hosts
sudo systemctl restart nginx
五、进程管理（Java 后端核心：JVM 进程）
5.1 查看进程
ps -ef | grep java
ps -aux | grep java
top               # 实时进程（CPU/内存）
htop              # 增强版（需安装）
5.2 查找 Java 进程
jps               # JDK 自带：列出 Java 进程（最准）
jps -l
Java 必用：jps > ps，因为能显示主类。
5.3 杀死进程
kill pid
kill -9 pid       # 强制杀（线上最后手段）
pkill -f java     # 杀所有 java
killall java
坑：kill -9 不会触发 JVM 优雅关闭，可能导致事务/缓存不一致。优先：
kill pid（15信号）
5.4 后台运行
nohup java -jar app.jar > log.log 2>&1 &
nohup：忽略挂断信号
&：后台
2>&1：错误重定向到标准输出
六、系统监控（CPU/内存/磁盘/负载）
6.1 top（实时监控）
top
关键字段：
load average：1/5/15 分钟负载（>CPU核心数=繁忙）
%Cpu(s)：us（用户） sy（系统） id（空闲）
KiB Mem：内存使用
6.2 内存 free
free -h
used：已用
free：空闲
buff/cache：缓存（可回收）
available：真正可用
Java 误区：cache 不是内存泄漏。
6.3 磁盘 df / du
df -h          # 磁盘使用率
du -sh *       # 目录大小
du -sh .       # 当前目录总大小
高频问题：磁盘 100% → 无法写日志 → Java 应用假死。
6.4 查看打开文件 lsof
lsof -p pid             # 进程打开文件
lsof -i:8080            # 端口占用
lsof | grep deleted     # 已删但未释放（导致磁盘满）
Java 场景：日志被 rm 但进程仍持有 → 空间不释放，需重启应用。
七、网络管理（端口、连通性、防火墙）
7.1 端口查看
netstat -tulnp | grep 8080
ss -tulnp | grep 8080   # 更快，替代 netstat
7.2 连通性
ping ip
telnet ip 8080
curl ip:8080/actuator/health
wget ip:8080
7.3 防火墙 firewall-cmd / iptables
systemctl status firewalld
firewall-cmd --add-port=8080/tcp --permanent
firewall-cmd --reload
Java 坑：服务启动但外部无法访问 → 防火墙未开端口。
7.4 路由与 hosts
cat /etc/hosts
vim /etc/hosts
八、系统与服务管理（systemd）
8.1 systemctl（CentOS7+）
systemctl start nginx
systemctl stop nginx
systemctl restart nginx
systemctl status nginx
systemctl enable nginx  # 开机自启
8.2 查看日志 journalctl
journalctl -u app.service
journalctl -f
journalctl -u nginx --since "10 min ago"
8.3 关机/重启
reboot
shutdown -h now
九、压缩与解压（部署必备）
tar -zcvf file.tar.gz dir
tar -zxvf file.tar.gz
unzip file.zip
zip -r file.zip dir
场景：部署包上传、JDK 解压、备份日志。
十、Java 后端专用高频指令组合（实战模板）
10.1 一键查 Java 进程
jps -l
ps -ef | grep java | grep -v grep
10.2 实时抓 ERROR 日志
tail -F catalina.out | grep -i error -C 10
10.3 查端口占用
ss -tulnp | grep 8080
lsof -i:8080
10.4 查大文件
du -sh /* | sort -hr | head -10
10.5 查句柄数（解决 Too many open files）
ulimit -n
lsof -p pid | wc -l
10.6 统计异常次数
grep "Exception" catalina.out | wc -l
10.7 磁盘满清理
find /var/log -name "*.log" -mtime +7 -delete
10.8 后台启动 SpringBoot
nohup java -jar app.jar --spring.profiles.active=prod > app.log 2>&1 &
十一、Shell 基础（Java 自动化脚本）
11.1 变量
JAR=app.jar
LOG=app.log
11.2 判断
if [ -f $JAR ]; then
  echo "存在"
fi
11.3 循环
for i in 1 2 3; do echo $i; done
11.4 启动脚本示例（企业级）

# /bin/bash
JAR=app.jar
LOG=app.log
PID=$(jps -l | grep $JAR | awk '{print $1}')
if [ -n "$PID" ]; then
  kill $PID
  sleep 3
fi
nohup java -jar $JAR > $LOG 2>&1 &
echo "启动成功"
十二、常见线上问题与指令对应（面试+工作）
CPU 高 → top + ps + jstack
内存泄漏 → free + jmap + jhat + MAT
磁盘满 → df + du + find + lsof
端口占用 → ss/lsof
应用假死 → tail + jstack + netstat
Too many open files → ulimit + lsof
日志暴涨 → du + grep + truncate
进程自动退出 → dmesg + journalctl（OOM killed）
十三、总结（Java 后端 Linux 指令学习路径）
先掌握：cd/ls/ll/mkdir/rm/cp/mv/cat/less/tail/grep
进程：ps/jps/top/kill/nohup
排障：find/awk/sed/lsof/ss/df/du
服务：systemctl/journalctl
脚本：Shell 基础 + 启动脚本
进阶：性能调优（vmstat/iostat/pidstat）+ 内核参数
Linux 指令不是背出来的，是线上问题逼出来的。
