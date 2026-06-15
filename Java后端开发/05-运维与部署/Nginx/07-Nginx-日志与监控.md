# Nginx 日志与监控

## 一、日志类型

| 日志 | 默认路径（yum） | 默认路径（源码） | 作用 |
|------|----------------|----------------|------|
| **access.log** | `/var/log/nginx/access.log` | `/usr/local/nginx/logs/access.log` | 记录所有客户端请求 |
| **error.log** | `/var/log/nginx/error.log` | `/usr/local/nginx/logs/error.log` | 记录 Nginx 运行错误 |

## 二、自定义日志格式

```nginx
http {
    log_format  custom_log  '$remote_addr [$time_local] "$request" '
                            '$status $body_bytes_sent "$http_referer" '
                            '"$http_user_agent" "$http_x_forwarded_for" '
                            '$upstream_addr $upstream_status '
                            '$upstream_response_time $request_time '
                            '$proxy_cache_status';

    access_log  /var/log/nginx/access.log  custom_log;
    error_log   /var/log/nginx/error.log   warn;    # 生产环境建议 warn
}
```

### 关键变量

| 变量 | 含义 |
|------|------|
| `$remote_addr` | 客户端 IP |
| `$request` | 请求方法 + 路径 + 协议 |
| `$status` | 响应状态码 |
| `$upstream_addr` | 后端节点 IP:端口 |
| `$upstream_response_time` | 后端响应耗时 |
| `$request_time` | 总请求耗时 |
| `$proxy_cache_status` | 缓存命中状态（HIT/MISS/EXPIRED） |
| `$http_x_forwarded_for` | 客户端真实 IP（经过代理） |

## 三、错误日志级别

| 级别 | 说明 | 适用场景 |
|------|------|---------|
| `debug` | 详细调试信息 | 仅开发调试，生产禁用 |
| `info` | 正常运行信息 | 无需关注 |
| `notice` | 轻微异常 | 留意即可 |
| **`warn`** | 潜在故障 | **生产推荐** |
| `error` | 严重异常 | 必须排查 |
| `crit` | 致命异常 | 需立即处理 |

## 四、按虚拟主机拆分日志

```nginx
server {
    listen       80;
    server_name  api.example.com;
    access_log  /var/log/nginx/api_access.log  custom_log;
    error_log   /var/log/nginx/api_error.log   warn;
}

server {
    listen       80;
    server_name  www.example.com;
    access_log  /var/log/nginx/www_access.log  custom_log;
    error_log   /var/log/nginx/www_error.log   warn;
}
```

## 五、自动日志切割

### 切割脚本

```bash
#!/bin/bash
# /usr/local/nginx/shell/nginx_log_cut.sh

LOG_PATH="/var/log/nginx"
DATE=$(date +%Y%m%d)
RETENTION_DAYS=7

# 重命名当前日志
mv $LOG_PATH/access.log $LOG_PATH/access_$DATE.log
mv $LOG_PATH/error.log $LOG_PATH/error_$DATE.log

# 通知 Nginx 重新打开日志文件
kill -USR1 $(cat /var/run/nginx.pid)

# 压缩归档
gzip $LOG_PATH/access_$DATE.log
gzip $LOG_PATH/error_$DATE.log

# 删除过期日志
find $LOG_PATH -name "*.log.gz" -mtime +$RETENTION_DAYS -delete
```

### 定时任务

```bash
chmod +x /usr/local/nginx/shell/nginx_log_cut.sh
crontab -e
```

```
0 0 * * * /usr/local/nginx/shell/nginx_log_cut.sh    # 每天 0 点执行
```

## 六、日志分析命令

```bash
# Top 10 访问 IP
awk '{print $1}' access.log | sort | uniq -c | sort -nr | head -10

# 状态码统计
awk '{print $9}' access.log | sort | uniq -c | sort -nr

# 502 错误请求（后端故障）
grep " 502 " access.log | head -20

# 请求耗时 Top 10（慢请求）
awk '{print $NF, $0}' access.log | sort -nr | head -10

# 后端节点请求分布（负载均衡验证）
grep -o '192.168.1.[0-9]*:8080' access.log | sort | uniq -c | sort -nr

# 缓存命中率
grep -o 'HIT\|MISS\|EXPIRED' access.log | sort | uniq -c

# 实时监控错误日志
tail -f /var/log/nginx/error.log
```

## 七、基础监控（stub_status）

```nginx
server {
    listen       80;
    server_name  monitor.example.com;

    location /nginx_status {
        stub_status on;
        allow 192.168.1.0/24;     # 仅允许内网
        allow 127.0.0.1;
        deny all;
        access_log off;
    }
}
```

```bash
curl http://localhost/nginx_status
# Active connections: 23          ← 当前活跃连接数
# server accepts handled requests
#   12345 12345 34567              ← 接收/处理/总请求数
# Reading: 0 Writing: 12 Waiting: 11
```

| 指标 | 说明 |
|------|------|
| Active connections | 当前活跃连接数 |
| accepts / handled | 接受/处理连接数（应基本相等） |
| requests | 累计请求数 |
| Waiting | 空闲连接数（过低说明资源不足） |

## 八、Prometheus + Grafana 监控

### 8.1 安装 nginx-prometheus-exporter

```bash
cd /usr/local/
wget https://github.com/nginxinc/nginx-prometheus-exporter/releases/download/v1.1.0/nginx-prometheus-exporter_1.1.0_linux_amd64.tar.gz
tar -zxvf nginx-prometheus-exporter_1.1.0_linux_amd64.tar.gz

# 启动（关联 stub_status 接口）
nohup ./nginx-prometheus-exporter -nginx.scrape-uri=http://localhost/nginx_status &
```

### 8.2 Prometheus 配置

```yaml
scrape_configs:
  - job_name: 'nginx'
    static_configs:
    - targets: ['localhost:9113']
    scrape_interval: 10s
```

### 8.3 Grafana 仪表盘

导入模板 ID：**12708**（适配 nginx-prometheus-exporter），即可获得连接数、请求数、异常状态码等可视化面板。

### 8.4 告警规则（推荐）

| 指标 | 阈值 | 说明 |
|------|------|------|
| 活跃连接数 | > 500 | 根据实际并发量调整 |
| 5xx 状态码 | 1 分钟 > 10 个 | 后端服务异常 |
| 响应时间 | P95 > 1s | 性能瓶颈 |
| Nginx 进程 | nginx_up == 0 | 服务宕机 |

## 九、常见问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 日志不生成 | 权限不足 / 路径错误 | `chown nginx:nginx` + 检查配置路径 |
| 日志文件过大 | 未配置切割 | 配置定时切割任务 |
| 日志无后端节点信息 | 日志格式未含 `$upstream_addr` | 自定义日志格式添加对应变量 |
| stub_status 无权限 | IP 未在 allow 列表 | 添加 allow 规则 |
| Prometheus 无数据 | exporter 未启动 / 端口不通 | 检查 exporter + 网络连通性 |
