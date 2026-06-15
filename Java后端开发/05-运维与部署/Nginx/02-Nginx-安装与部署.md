# Nginx 安装与部署

## 一、安装方式对比

| 方式 | 适用场景 | 优点 | 缺点 |
|------|---------|------|------|
| **yum/apt** | 测试、简单场景 | 快速、简单 | 版本偏低、模块不全 |
| **源码编译** | **生产环境** | 自定义模块、版本可控 | 步骤较多 |

## 二、yum 安装（快速部署）

```bash
# CentOS 7/8
rpm -Uvh http://nginx.org/packages/centos/7/noarch/RPMS/nginx-release-centos-7-0.el7.ngx.noarch.rpm
yum install -y nginx

systemctl start nginx
systemctl enable nginx

# 验证
nginx -v
curl http://localhost
```

## 三、源码编译安装（生产推荐）

### 3.1 安装编译依赖

```bash
yum install -y gcc gcc-c++ pcre pcre-devel zlib zlib-devel openssl openssl-devel
```

> `openssl-devel` 用于 SSL 模块（HTTPS 必备），`pcre-devel` 用于正则匹配。

### 3.2 下载并编译

```bash
mkdir -p /usr/local/nginx/src && cd /usr/local/nginx/src
wget http://nginx.org/download/nginx-1.24.0.tar.gz
tar -zxvf nginx-1.24.0.tar.gz
cd nginx-1.24.0

# 配置编译参数（适配 Java 后端）
./configure \
  --prefix=/usr/local/nginx \
  --user=nginx \
  --group=nginx \
  --with-http_ssl_module \
  --with-http_gzip_static_module \
  --with-http_stub_status_module \
  --with-pcre \
  --with-zlib \
  --without-http_autoindex_module

make && make install
```

| 参数 | 作用 |
|------|------|
| `--with-http_ssl_module` | SSL/HTTPS 支持（生产必备） |
| `--with-http_gzip_static_module` | 静态资源 Gzip 压缩 |
| `--with-http_stub_status_module` | 基础状态监控 |
| `--without-http_autoindex_module` | 禁用目录浏览（安全） |

### 3.3 编译后配置

```bash
# 创建 nginx 用户
useradd -s /sbin/nologin -M nginx

# 设置目录权限
chown -R nginx:nginx /usr/local/nginx/
chmod -R 755 /usr/local/nginx/
```

### 3.4 安装后目录结构

```
/usr/local/nginx/
├── conf/nginx.conf    # 核心配置文件
├── html/              # 默认静态资源目录
├── logs/              # 日志目录（access.log + error.log）
└── sbin/nginx         # 启动脚本
```

## 四、systemd 服务配置（开机自启）

```bash
vi /usr/lib/systemd/system/nginx.service
```

```ini
[Unit]
Description=Nginx HTTP Server
After=network.target

[Service]
Type=forking
PIDFile=/usr/local/nginx/logs/nginx.pid
ExecStart=/usr/local/nginx/sbin/nginx
ExecReload=/usr/local/nginx/sbin/nginx -s reload
ExecStop=/usr/local/nginx/sbin/nginx -s stop
User=nginx
Group=nginx
PrivateTmp=true

[Install]
WantedBy=multi-user.target
```

```bash
systemctl daemon-reload
systemctl enable nginx
systemctl start nginx
```

## 五、验证部署

```bash
# 检查配置语法
nginx -t

# 查看运行状态
systemctl status nginx

# 测试端口访问
curl http://localhost

# 验证反向代理（配合 Java 服务）
curl http://localhost/api/user/list
```

## 六、性能参数调优

| 参数 | 建议值 | 说明 |
|------|--------|------|
| `worker_processes` | `auto`（CPU 核数） | Worker 进程数 |
| `worker_connections` | 10240 | 单 Worker 最大连接数 |
| `worker_rlimit_nofile` | 65535 | 文件描述符上限 |
| `keepalive_timeout` | 65 | 长连接超时 |
| `gzip` | `on` | 开启响应压缩 |

```nginx
worker_processes  auto;
worker_rlimit_nofile 65535;

events {
    worker_connections  10240;
    use epoll;
    multi_accept on;
}
```

## 七、常见问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| `configure: error: SSL modules require...` | 未安装 openssl-devel | `yum install -y openssl openssl-devel` |
| 启动失败 | 端口占用 / 配置错误 | `nginx -t` 检查语法，`netstat -tulpn` 查看端口 |
| 80 端口被占用 | Apache 等服务占用 | 关闭占用服务或修改 Nginx 监听端口 |
| 日志无写入 | 权限不足 | `chown -R nginx:nginx /usr/local/nginx/` |
