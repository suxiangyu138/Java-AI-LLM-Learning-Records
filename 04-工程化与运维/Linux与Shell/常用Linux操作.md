常用Linux操作
本文聚焦Java后端开发日常工作中高频使用的Linux操作，按「环境配置、应用部署、日志排查、系统监控、问题定位」五大核心场景分类，每个操作均标注用途、命令及注意事项，摒弃无用命令，确保拿来就用、贴合实际开发需求，同时兼顾新手友好性和生产环境实用性。
一、环境配置类（基础必备）
主要用于Java开发环境（JDK、环境变量）的配置与验证，是应用部署的前提，适配CentOS、Ubuntu主流发行版。
1.1 JDK相关操作

# 1. 解压JDK安装包（常用路径/usr/local）
tar -zxvf jdk-17_linux-x64_bin.tar.gz -C /usr/local/

# 2. 重命名JDK目录（简化路径，便于配置环境变量）
mv /usr/local/jdk-17.0.10 /usr/local/jdk17

# 3. 配置全局环境变量（编辑/etc/profile文件）
vim /etc/profile

# 新增内容（替换为自身JDK路径）
export JAVA_HOME=/usr/local/jdk17
export PATH=$JAVA_HOME/bin:$PATH
export CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar

# 4. 使环境变量立即生效
source /etc/profile

# 5. 验证JDK安装成功
java -version
javac -version

# 6. 快速安装OpenJDK（CentOS，无需手动解压）
yum install openjdk-17-jdk -y

# 7. 快速安装OpenJDK（Ubuntu）
apt install openjdk-17-jdk -y
注意：环境变量配置后，若新终端仍无法识别java命令，需重新登录终端或再次执行source /etc/profile；Ubuntu系统也可修改~/.bashrc配置用户级环境变量。
1.2 环境变量查看与修改

# 1. 查看所有环境变量
env

# 2. 查看指定环境变量（如JAVA_HOME）
echo $JAVA_HOME

# 3. 临时修改环境变量（仅当前终端生效，重启失效）
export JAVA_HOME=/usr/local/jdk17

# 4. 永久修改环境变量（系统级：/etc/profile；用户级：~/.bashrc）
vim /etc/profile # 编辑后执行source生效
二、应用部署类（核心高频）
针对Java应用（Spring Boot jar包、Tomcat项目）的启停、后台运行、开机自启，覆盖开发调试和生产部署场景。
2.1 Spring Boot jar包部署

# 1. 前台运行（调试用，终端关闭则应用停止）
java -jar demo.jar

# 2. 后台运行（常用，&表示后台进程）
java -jar demo.jar &

# 3. 指定多环境配置运行（生产/测试环境切换）
java -jar demo.jar --spring.profiles.active=prod

# 4. 启动时指定JVM参数（内存优化常用）
java -Xms2g -Xmx2g -jar demo.jar # Xms初始堆，Xmx最大堆

# 5. 后台运行并将日志输出到指定文件
nohup java -jar demo.jar --spring.profiles.active=prod > /var/log/java/demo.log 2>&1 
2.2 应用启停与进程管理

# 1. 查找Java进程（两种核心方式）
jps # JDK自带，快速查看Java进程PID和主类名
ps -ef | grep java # 查看所有Java进程，可筛选目标应用（如grep demo.jar）

# 2. 优雅停止应用（推荐，允许释放资源、保存数据）
kill -15 进程PID

# 3. 强制停止应用（紧急情况，仅进程无响应时使用）
kill -9 进程PID

# 4. 查看进程详细信息（如内存、CPU占用）
ps -ef | grep 进程PID
top -p 进程PID
2.3 生产环境守护进程配置（systemd）
解决后台运行（&、nohup）终端关闭、服务器重启后应用停止的问题，实现开机自启、异常重启。

# 1. 创建守护进程配置文件（以demo应用为例）
vim /etc/systemd/system/demo.service

# 2. 写入配置（修改路径为自身应用路径）
[Unit]
Description=demo Spring Boot Application
After=network.target # 网络启动后再启动应用
[Service]
User=java # 运行应用的普通用户（避免root）
WorkingDirectory=/home/java/app # 应用所在目录
ExecStart=/usr/local/jdk17/bin/java -jar demo.jar --spring.profiles.active=prod
SuccessExitStatus=143 # 兼容Spring Boot优雅关闭退出码
Restart=always # 异常停止后自动重启
RestartSec=5 # 重启间隔5秒
[Install]
WantedBy=multi-user.target # 多用户模式开机自启

# 3. 常用守护进程命令
systemctl daemon-reload # 重新加载配置（修改后必须执行）
systemctl start demo.service # 启动应用
systemctl status demo.service # 查看应用状态
systemctl stop demo.service # 停止应用
systemctl enable demo.service # 设置开机自启
systemctl disable demo.service # 取消开机自启
2.4 Tomcat项目部署（可选）

# 1. 启动Tomcat
/usr/local/tomcat/bin/startup.sh

# 2. 停止Tomcat
/usr/local/tomcat/bin/shutdown.sh

# 3. 查看Tomcat启动日志（排查启动失败）
tail -f /usr/local/tomcat/logs/catalina.out

# 4. 部署war包（将war包放入webapps目录）
cp demo.war /usr/local/tomcat/webapps/

# 5. 重启Tomcat（部署后生效）
/usr/local/tomcat/bin/shutdown.sh && /usr/local/tomcat/bin/startup.sh
三、日志排查类（问题定位必备）
线上问题（接口报错、应用卡顿）排查的核心，重点掌握日志实时监控、精准检索、超大日志处理技巧，结合Java日志特点（异常堆栈、TraceId）。
3.1 日志实时监控

# 1. 实时监控日志（最常用，-f表示实时滚动）
tail -f /var/log/java/demo.log

# 2. 显示最后200行再实时监控（避免日志刷屏）
tail -n 200 -f /var/log/java/demo.log

# 3. 监控时过滤指定关键词（如ERROR、Exception）
tail -f /var/log/java/demo.log | grep "ERROR"

# 4. 过滤多个关键词（如ERROR和NullPointerException）
tail -f /var/log/java/demo.log | grep -E "ERROR|NullPointerException"
3.2 日志精准检索

# 1. 检索日志中包含指定关键词的内容
grep "NullPointerException" /var/log/java/demo.log

# 2. 显示匹配行前后20行上下文（查看异常堆栈）
grep -C 20 "NullPointerException" /var/log/java/demo.log

# 3. 检索所有切割后的日志文件（如按日期切割的日志）
grep "TraceId-20260325" /var/log/java/demo.log*

# 4. 统计指定关键词出现次数（判断异常频率）
grep -c "RedisConnectionException" /var/log/java/demo.log

# 5. 过滤无关日志（如健康检查日志），只看核心内容
grep -v "HealthCheck" /var/log/java/demo.log
3.3 超大日志文件处理
日志文件过大（几G）时，禁止用cat直接打开，避免终端卡死、内存飙升，优先使用以下命令。

# 1. 按需加载超大日志（less命令）
less /var/log/java/demo.log

# 进入后操作：Shift+G（跳至末尾）、?关键词（反向检索）、n（下一个匹配）、q（退出）

# 2. 切割指定时间窗口的日志（便于下载分析）
sed -n '/2026-03-25 14:00/,/2026-03-25 14:05/p' /var/log/java/demo.log > error_segment.log

# 3. 查看日志文件大小（定位大日志）
du -sh /var/log/java/demo.log
四、系统监控类（性能优化必备）
监控Linux系统CPU、内存、磁盘、网络资源，结合Java应用监控，定位性能瓶颈（如CPU飙高、内存不足）。
4.1 系统资源监控

# 1. CPU监控（实时查看，类似任务管理器）
top # 按P排序（CPU）、M排序（内存）、p+PID聚焦指定进程
top -p 进程PID # 仅监控目标Java进程

# 2. 内存监控（人性化显示）
free -h # 重点关注available（可用内存）、Swap使用率

# 3. 磁盘监控（避免日志打满磁盘）
df -h # 查看所有磁盘分区占用
du -sh /var/log/* # 查看目录下各文件大小

# 4. 网络监控（端口、连接状态）

# 查看指定端口占用（解决端口被占用问题）
netstat -nlp | grep 8080

# 查看TCP连接状态分布（排查高并发连接）
netstat -n | awk '/^tcp/ {++S($NF)} END {for(a in S) print a, S(a)}'

# 查看服务器IP和端口信息
ifconfig # 或 ip addr
4.2 JVM与系统协同监控

# 1. 查看JVM GC统计信息（每1秒输出1次，共10次）
jstat -gcutil 进程PID 1000 10

# 2. 导出线程堆栈（排查死锁、线程阻塞）
jstack 进程PID > thread.log

# 3. 查看JVM堆内存分布（排查内存溢出）
jmap -heap 进程PID

# 4. 导出堆快照（用于MAT工具分析内存问题）
jmap -dump:format=b,file=heapdump.hprof 进程PID
五、问题定位类（高频痛点）
针对Java后端开发中最常见的Linux相关问题，给出标准排查流程和命令，快速解决问题。
5.1 端口被占用（应用启动失败）

# 1. 查找占用目标端口（如8080）的进程
netstat -nlp | grep 8080

# 2. 查看进程详情，判断是否无用
ps -ef | grep 进程PID

# 3. 优雅停止进程（优先）
kill -15 进程PID

# 4. 强制停止（紧急情况）
kill -9 进程PID
5.2 应用无法访问（防火墙问题）

# 1. 查看防火墙状态
systemctl status firewalld

# 2. 开放应用端口（如8080，永久生效）
firewall-cmd --add-port=8080/tcp --permanent
firewall-cmd --reload

# 3. 临时关闭防火墙（调试用，生产不建议）
systemctl stop firewalld

# 4. 查看已开放端口
firewall-cmd --list-ports
5.3 日志无法写入（权限问题）

# 1. 查看日志目录权限
ls -ld /var/log/java

# 2. 修改目录权限（确保Java用户有写入权限）
chmod 775 /var/log/java
chown -R java:java /var/log/java # 修改所有者为运行应用的用户

# 3. 查看应用jar包权限
ls -l demo.jar

# 4. 修改jar包权限（可读可执行）
chmod 755 demo.jar
5.4 CPU飙高至100%（高频问题）

# 标准排查流程
1. 定位CPU飙高的Java进程
    top # 记录进程PID（如12345）
2. 定位进程下CPU最高的线程
    top -H -p 12345 # 记录线程TID（如12346）
3. TID转换为16进制（jstack输出为16进制）
    printf "%x\n" 12346
4. 导出线程堆栈，查找异常线程
    jstack 12345 | grep -A 20 16进制TID
5. 结合业务代码修复（如死循环、锁竞争）
    六、常用辅助操作

# 1. 文件夹操作
mkdir -p /home/java/app # 递归创建文件夹（不存在则创建）
cd /home/java/app # 进入目录
ls -l # 查看目录下文件详情（权限、大小、修改时间）
rm -rf 文件夹名 # 强制删除文件夹（谨慎使用）
cp 源文件 目标路径 # 复制文件（如cp demo.jar /home/java/app）
mv 源文件 目标路径 # 移动/重命名文件

# 2. 文件编辑（vim常用）
vim 文件名 # 打开文件
i # 进入编辑模式
Esc # 退出编辑模式
:wq # 保存并退出
:q! # 强制退出（不保存）

# 3. 远程文件传输（上传/下载）

# 本地文件上传到服务器
scp 本地文件路径 java@服务器IP:/home/java/app

# 服务器文件下载到本地
scp java@服务器IP:/home/java/app/demo.log 本地路径

# 4. 查看系统时间（同步应用时间）
date # 查看当前时间
timedatectl set-time "2026-03-25 15:30:00" # 设置系统时间

# 5. 重启/关闭服务器（谨慎使用）
reboot # 重启服务器
shutdown -h now # 立即关闭服务器
七、注意事项
生产环境禁止使用root用户部署Java应用，建议创建专用java用户，降低安全风险；
执行rm -rf命令时务必谨慎，建议先通过ls命令确认路径，避免误删系统文件；
修改系统配置文件（如/etc/profile、/etc/sysctl.conf）前，建议备份文件（如cp /etc/profile /etc/profile.bak）；
日志文件建议按日期切割，避免单个文件过大，便于排查和清理；
强制杀死进程（kill -9）会导致应用资源无法释放，仅在进程无响应时使用，优先使用kill -15优雅关闭。
