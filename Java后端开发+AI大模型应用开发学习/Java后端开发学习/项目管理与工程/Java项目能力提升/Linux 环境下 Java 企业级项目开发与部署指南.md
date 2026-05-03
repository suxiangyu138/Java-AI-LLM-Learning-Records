03.31 03:32
Linux 环境下 Java 企业级项目开发与部署指南
一、Linux 环境准备（CentOS 7 / Ubuntu 20.04）
1. 系统更新与基础工具安装
CentOS 7
yum update -y
yum install -y wget curl vim unzip zip git gcc gcc-c++ make
Ubuntu 20.04
apt update -y
apt install -y wget curl vim unzip zip git gcc g++ make
2. 安装 JDK 11（企业级推荐）
cd /usr/local
wget https://repo.huaweicloud.com/java/jdk/11.0.18+9/jdk-11.0.18_linux-x64_bin.tar.gz
tar -zxvf jdk-11.0.18_linux-x64_bin.tar.gz
mv jdk-11.0.18 jdk11
配置环境变量
vim /etc/profile
添加
export JAVA_HOME=/usr/local/jdk11
export PATH=JAVA_HOME/bin:PATH
生效
source /etc/profile
java -version
3. 安装 Maven 3.8
cd /usr/local
wget https://repo.huaweicloud.com/apache/maven/maven-3/3.8.8/binaries/apache-maven-3.8.8-bin.tar.gz
tar -zxvf apache-maven-3.8.8-bin.tar.gz
mv apache-maven-3.8.8 maven
配置环境变量
vim /etc/profile
添加
export MAVEN_HOME=/usr/local/maven
export PATH=MAVEN_HOME/bin:PATH
生效
source /etc/profile
mvn -v
配置阿里云镜像
vim /usr/local/maven/conf/settings.xml
在  内添加
aliyunmaven
central
https://maven.aliyun.com/repository/public
4. 安装 MySQL 8.0
CentOS 7
wget https://dev.mysql.com/get/mysql80-community-release-el7-3.noarch.rpm
rpm -ivh mysql80-community-release-el7-3.noarch.rpm
yum install -y mysql-community-server
启动
systemctl start mysqld
systemctl enable mysqld
获取初始密码
grep 'temporary password' /var/log/mysqld.log
登录并修改密码
mysql -uroot -p
ALTER USER 'root'@'localhost' IDENTIFIED BY '你的密码';
CREATE DATABASE enterprise_db DEFAULT CHARSET utf8mb4;
Ubuntu 20.04
apt install -y mysql-server
systemctl start mysql
systemctl enable mysql
mysql_secure_installation
5. 安装 Redis 6.2
cd /usr/local
wget https://download.redis.io/releases/redis-6.2.13.tar.gz
tar -zxvf redis-6.2.13.tar.gz
cd redis-6.2.13
make
make install
后台启动
redis-server --daemonize yes
6. 安装 Tomcat 9（可选，传统部署）
cd /usr/local
wget https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.85/bin/apache-tomcat-9.0.85.tar.gz
tar -zxvf apache-tomcat-9.0.85.tar.gz
mv apache-tomcat-9.0.85 tomcat9
启动
/usr/local/tomcat9/bin/startup.sh
二、Linux 下 Spring Boot 企业级项目开发（基于前面项目）
1. 项目目录结构（Linux 标准）
/opt/enterprise-project
├── bin          # 启动脚本
├── conf         # 配置文件
├── lib          # 依赖 jar
├── logs         # 日志
├── target       # 编译产物
└── src          # 源码
2. 克隆/上传项目到 Linux
方式1 Git 克隆
cd /opt
git clone 你的项目仓库地址 enterprise-project
方式2 本地打包上传
本地执行
mvn clean package -DskipTests
上传到 Linux
scp target/*.jar root@服务器IP:/opt/enterprise-project/
3. 项目配置文件（application.yml）
cd /opt/enterprise-project/conf
vim application.yml
server:
port: 8080
servlet:
context-path: /api
spring:
datasource:
url: jdbc:mysql://localhost:3306/enterprise_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
username: root
password: 你的密码
driver-class-name: com.mysql.cj.jdbc.Driver
redis:
host: localhost
port: 6379
logging:
file:
name: /opt/enterprise-project/logs/app.log
level:
root: info
com.company: debug
4. 编写启动脚本（企业级标准）
cd /opt/enterprise-project/bin
vim start.sh
#!/bin/bash
Spring Boot 启动脚本
APP_NAME="enterprise-project"
JAR_PATH="/opt/enterprise-project/target/${APP_NAME}.jar"
CONF_PATH="/opt/enterprise-project/conf/application.yml"
LOG_PATH="/opt/enterprise-project/logs"
PID_FILE="/opt/enterprise-project/bin/app.pid"
if [ -f $PID_FILE ]; then
PID=$(cat $PID_FILE)
if ps -p $PID > /dev/null; then
echo "$APP_NAME is running, PID: $PID"
exit 1
fi
fi
echo "Starting $APP_NAME..."
nohup java -jar JAR_PATH --spring.config.location=CONF_PATH > $LOG_PATH/start.log 2>&1 &
echo $! > $PID_FILE
echo "$APP_NAME started successfully, PID: $(cat $PID_FILE)"
添加执行权限
chmod +x start.sh
5. 编写停止脚本
vim stop.sh
#!/bin/bash
APP_NAME="enterprise-project"
PID_FILE="/opt/enterprise-project/bin/app.pid"
if [ -f $PID_FILE ]; then
PID=$(cat $PID_FILE)
kill -15 $PID
echo "Stopping $APP_NAME, PID: $PID"
rm -f $PID_FILE
else
echo "$APP_NAME is not running"
fi
chmod +x stop.sh
三、Linux 项目部署与运行
1. 编译项目
cd /opt/enterprise-project
mvn clean package -DskipTests
2. 启动项目
cd bin
./start.sh
3. 查看日志
tail -f ../logs/app.log
4. 测试接口
curl http://localhost:8080/api/user/1
四、Linux 企业级运维配置
1. 配置防火墙开放端口
firewall-cmd --zone=public --add-port=8080/tcp --permanent
firewall-cmd --reload
2. 配置系统服务（systemd 托管，推荐）
vim /etc/systemd/system/enterprise.service
[Unit]
Description=Enterprise Project
After=network.target mysql.service redis.service
[Service]
Type=forking
User=root
WorkingDirectory=/opt/enterprise-project
ExecStart=/opt/enterprise-project/bin/start.sh
ExecStop=/opt/enterprise-project/bin/stop.sh
Restart=on-failure
RestartSec=5
[Install]
WantedBy=multi-user.target
生效
systemctl daemon-reload
systemctl start enterprise
systemctl enable enterprise
3. 日志切割（logrotate）
vim /etc/logrotate.d/enterprise
/opt/enterprise-project/logs/*.log {
daily
rotate 30
compress
missingok
notifempty
copytruncate
}
五、Linux 企业级项目扩展
1. Nginx 反向代理（生产必备）
安装 Nginx
yum install -y nginx 或 apt install -y nginx
配置
vim /etc/nginx/conf.d/enterprise.conf
server {
listen 80;
server_name 你的域名;
location /api {
proxy_pass http://127.0.0.1:8080;
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
}
}
启动
systemctl start nginx
systemctl enable nginx
2. 定时任务（crontab）
crontab -e
每日凌晨备份数据库
0 0 * * * mysqldump -uroot -p密码 enterprise_db > /opt/backup/db_$(date +%Y%m%d).sql
3. 性能监控（top / jstat / jmap）
查看进程
top -p 进程ID
JVM 监控
jstat -gc 进程ID 1000
内存快照
jmap -dump:format=b,file=/opt/heap.hprof 进程ID
六、Linux 安全加固（企业级）
1. 禁用 root 远程登录
vim /etc/ssh/sshd_config
PermitRootLogin no
systemctl restart sshd
2. 配置防火墙规则
只开放 80、443、22 端口
3. 定期更新系统
yum update -y 或 apt upgrade -y
4. 数据库授权最小权限
CREATE USER 'app'@'localhost' IDENTIFIED BY '密码';
GRANT SELECT,INSERT,UPDATE,DELETE ON enterprise_db.* TO 'app'@'localhost';
七、项目目录权限规范
chown -R root:root /opt/enterprise-project
chmod -R 755 /opt/enterprise-project
chmod -R 777 /opt/enterprise-project/logs

