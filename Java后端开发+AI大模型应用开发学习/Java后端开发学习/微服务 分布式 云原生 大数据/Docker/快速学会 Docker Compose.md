04.27 09:35
快速学会 Docker Compose
一、什么是 Docker Compose
1. 用来编排多个容器；
2. 用一个  docker-compose.yml  配置文件，统一管理：启动、停止、重启；
3. 告别敲长串  docker run  命令，一条命令启停整套环境；
4. Docker Desktop 自带，无需额外安装。
 
二、核心 3 条命令（必背）
bash
# 前台启动（看日志）
docker-compose up
# 后台守护启动（开发常用）
docker-compose up -d
# 停止并删除容器、网络
docker-compose down
 
 
三、实战：MySQL + Redis + Milvus 三合一
在电脑任意空白文件夹，新建文件：
文件名： docker-compose.yml 
直接复制下面全部内容：
yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    container_name: mysql8
    restart: always
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: 123456
    volumes:
      - mysql_data:/var/lib/mysql
  redis:
    image: redis:latest
    container_name: redis
    restart: always
    ports:
      - "6379:6379"
  milvus:
    image: milvusdb/milvus:latest
    container_name: milvus
    restart: always
    ports:
      - "19530:19530"
volumes:
  mysql_data:
 
 
四、一键操作
1. 进入这个yml所在文件夹，打开终端
2. 后台启动全部服务：
bash
docker-compose up -d
 
3. 查看运行状态
bash
docker-compose ps
 
4. 整套环境关闭销毁
bash
docker-compose down
 
 
五、连接信息（直接用）
- MySQL
- 端口：3306
- 账号：root
- 密码：123456
- Redis
- 端口：6379
- 无密码
- Milvus
- 端口：19530
- 无认证，直接连接
 
六、关键知识点（极简）
1.  restart: always 
开机/Docker 重启自动拉起服务，持久运行
2.  volumes  数据卷
容器删了，数据不丢失
3. 服务之间内部互通
容器内可以直接用服务名互相访问
例：mysql 容器内直接访问  redis:6379 
 
七、日常开发高频组合
以后你做 Java后端 + RAG 开发，
只需要：
- 保存这份  yml 
- 开机执行： docker-compose up -d 
直接拥有全套中间件环境，不用逐个装、配环境。

