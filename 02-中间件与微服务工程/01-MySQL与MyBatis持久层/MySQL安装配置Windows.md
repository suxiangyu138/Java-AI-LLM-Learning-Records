# MySQL 8.0.46 完整安装与日常操作指南（Windows）

## 一、完整安装步骤

### 1. 准备工作
1. 下载 `mysql-8.0.46-winx64.zip` 压缩包
2. 解压至**纯英文无空格路径**，示例：`D:\mysql-8.0\mysql-8.0.46-winx64`
3. 配置系统环境变量：将 `D:\mysql-8.0\mysql-8.0.46-winx64\bin` 添加到系统 `Path` 中

### 2. 创建配置文件 my.ini
在 MySQL 解压根目录下，新建文本文件并重命名为 `my.ini`，写入以下配置（**路径需与实际解压路径一致**）：
```ini
[mysqld]
# MySQL 根目录
basedir=D:\mysql-8.0\mysql-8.0.46-winx64
# 数据存储目录
datadir=D:\mysql-8.0\mysql-8.0.46-winx64\data
# 端口号
port=3306
# 默认字符集
character-set-server=utf8mb4
# 默认存储引擎
default-storage-engine=INNODB

[mysql]
# 客户端默认字符集
default-character-set=utf8mb4
```

### 3. 管理员身份执行初始化与安装命令
以**管理员身份**打开 PowerShell，依次执行以下命令：
```powershell
# 1. 切换到 MySQL bin 目录
D:
cd D:\mysql-8.0\mysql-8.0.46-winx64\bin

# 2. 初始化数据库（生成 data 目录 + 临时密码，仅执行一次）
mysqld --initialize --console

# 3. 安装 MySQL 服务（自定义服务名，避免冲突）
mysqld --install MySQL80_New2026

# 4. 启动 MySQL 服务
net start MySQL80_New2026
```
> 注意：初始化后，控制台会输出**临时密码**，请务必保存！

### 4. 登录并修改初始密码
```powershell
# 登录 MySQL（输入之前保存的临时密码）
mysql -uroot -p

# 登录成功后，执行修改密码命令
ALTER USER 'root'@'localhost' IDENTIFIED BY '你的新密码';
flush privileges;
exit;
```

## 二、数据库日常操作（进入/退出/服务管理）

### 1. MySQL 服务管理（开机必启）
```powershell
# 启动服务
net start MySQL80_New2026

# 停止服务
net stop MySQL80_New2026
```

### 2. 登录 MySQL
```powershell
# 基础安全登录（回车后输入密码，不显示）
mysql -uroot -p

# 直接带密码登录（适合脚本，不推荐日常使用）
mysql -uroot -p你的新密码

# 自定义端口登录
mysql -uroot -p -P3306
```

### 3. 退出 MySQL
在 `mysql>` 命令行中执行以下任意命令：
```sql
exit;
quit;
```

## 三、常用基础 SQL 命令

### 1. 数据库操作
```sql
-- 查看所有数据库
show databases;

-- 创建数据库（指定字符集）
create database test_db default character set utf8mb4;

-- 切换到指定数据库
use test_db;

-- 删除数据库（谨慎操作）
drop database if exists test_db;
```

### 2. 数据表操作
```sql
-- 查看当前数据库下的所有表
show tables;

-- 创建数据表（示例：用户表）
create table user (
    id int primary key auto_increment,  -- 主键自增
    username varchar(50) not null,       -- 用户名，非空
    age int,                             -- 年龄
    create_time datetime default current_timestamp  -- 创建时间，默认当前时间
);

-- 查看表结构
desc user;

-- 删除数据表（谨慎操作）
drop table if exists user;
```

### 3. 数据增删改查
```sql
-- 插入数据
insert into user (username, age) values ('张三', 20);

-- 查询数据
select * from user;  -- 查询所有字段
select username, age from user where age > 18;  -- 条件查询

-- 更新数据
update user set age = 21 where id = 1;

-- 删除数据（谨慎操作，务必加 where 条件）
delete from user where id = 1;
```

## 四、可视化工具连接推荐
日常开发推荐使用可视化工具管理数据库，连接参数统一：
- 连接类型：MySQL
- 主机：`localhost` / `127.0.0.1`
- 端口：`3306`
- 用户名：`root`
- 密码：你设置的新密码
- 可选数据库：`mysql`（默认）或自定义数据库

**推荐工具**：Navicat Premium、DBeaver、DataGrip

## 五、常见问题排查

### 1. 忘记 root 密码
1. 停止 MySQL 服务：`net stop MySQL80_New2026`
2. 编辑 `my.ini`，在 `[mysqld]` 下添加：`skip-grant-tables`
3. 重启服务：`net start MySQL80_New2026`
4. 免密码登录后，执行修改密码命令
5. 删除 `my.ini` 中的 `skip-grant-tables`，重启服务

### 2. 服务启动失败
1. 检查 `my.ini` 路径：无中文、无空格、配置格式正确
2. 删除自动生成的 `data` 文件夹，重新执行初始化命令：`mysqld --initialize --console`
3. 服务名冲突：更换服务名，重新执行安装命令
