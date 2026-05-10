03.19 21:20
Nginx监控配置及管理（企业级落地版）
Nginx监控是企业级Web服务运维的核心环节，通过实时监控Nginx运行状态、请求指标、性能瓶颈，可提前发现服务异常（如端口占用、并发过高、后端节点故障），快速定位问题，避免服务中断，保障服务稳定运行。本文摒弃冗余理论，聚焦实战落地，从“监控核心认知→基础状态监控→第三方工具监控（Prometheus+Grafana）→监控告警→日常管理→故障排查”，一步步讲解Nginx监控的全流程配置与管理，所有操作均来自生产环境，可直接复用，兼顾新手友好性和企业级规范性，适配Java后端部署、静态资源服务、负载均衡等各类Nginx应用场景，与此前Nginx缓存、负载均衡、日志管理教程形成完整运维闭环。
实战环境：CentOS 7/8、Nginx 1.24.0（稳定版）、Prometheus 2.45.0、Grafana 10.2.0，默认已完成Nginx基础部署，全程命令行操作，重点突出“配置→部署→监控→告警”的闭环，让你快速掌握Nginx监控的核心方法，实现Nginx服务的可视化、智能化运维。
一、Nginx监控核心认知（必懂基础）
Nginx监控的核心是“采集关键指标、可视化展示、异常告警”，核心目标是“提前发现故障、快速定位问题、优化服务性能”，需明确监控的核心指标、监控方式，为后续配置奠定基础。
1.1 核心监控指标（必关注）
监控Nginx的核心指标分为4类，覆盖运行状态、请求情况、性能瓶颈、后端交互，运维时需重点关注异常波动：
运行状态指标：Nginx进程状态（是否运行）、端口占用情况、worker进程数量、连接数（已连接、空闲连接）；
请求指标：请求总量、每秒请求数（QPS）、异常状态码数量（4xx、5xx）、请求耗时分布；
性能指标：CPU占用率、内存占用、磁盘I/O（日志写入、缓存文件读写）、网络I/O；
后端交互指标（负载均衡/反向代理场景）：后端节点健康状态、请求转发成功率、后端响应耗时、缓存命中情况。
注意：监控核心原则——重点指标实时监控、异常指标及时告警，无需监控所有指标，聚焦“影响服务可用性、性能”的关键指标，避免监控冗余。
1.2 两种核心监控方式（企业级常用）
Nginx监控分为“基础状态监控”和“第三方工具监控”，按需选择，小型服务可使用基础监控，中大型服务（高并发、多节点）推荐使用第三方工具，实现可视化监控和智能告警：
监控方式
核心特点
适用场景
优势
不足
基础状态监控（stub_status）
Nginx内置模块，无需额外安装，仅展示基础运行指标
小型服务、临时监控、快速排查基础故障
配置简单、轻量、无需额外依赖
无可视化界面、无告警功能、指标单一
第三方工具监控（Prometheus+Grafana）
需安装第三方工具，采集多维度指标，支持可视化、告警
中大型服务、高并发场景、多Nginx节点集群
指标全面、可视化清晰、支持自定义告警、可监控多节点
配置稍复杂、需额外部署工具
二、实战一：Nginx基础状态监控（stub_status模块）
Nginx内置stub_status模块，可快速开启基础监控，查看Nginx运行状态、连接数、请求数等核心指标，无需额外安装工具，适合快速排查基础故障，配置简单，一步到位。
2.1 开启stub_status模块（必做）
默认情况下，Nginx已编译stub_status模块（yum安装/源码编译均默认启用），只需在nginx.conf中配置监控路径即可：
步骤1：编辑Nginx主配置文件
# 编辑nginx.conf（yum安装路径：/etc/nginx/nginx.conf；源码编译路径：/usr/local/nginx/conf/nginx.conf）
vi /etc/nginx/nginx.conf
# 在http块中添加监控server块（单独配置监控虚拟主机，避免影响业务）
http {
    include       mime.types;
    default_type  application/octet-stream;
    log_format  custom_log  '$remote_addr [$time_local] "$request" $status $body_bytes_sent "$http_referer" "$http_user_agent" "$http_x_forwarded_for" $upstream_addr $upstream_status $upstream_response_time $request_time $proxy_cache_status';
    access_log  /var/log/nginx/access.log  custom_log;
    error_log   /var/log/nginx/error.log   warn;
    # Nginx基础监控配置（核心）
    server {
        listen       80;
        server_name  monitor.xxx.com;  # 监控域名（本地测试可使用localhost）
        location /nginx_status {
            stub_status on;  # 开启基础监控
            allow 192.168.1.0/24;  # 允许内网IP访问（安全限制，避免公网暴露）
            allow 127.0.0.1;  # 允许本地访问
            deny all;  # 禁止其他IP访问
            access_log off;  # 禁止记录监控接口的访问日志，减少冗余
        }
    }
    # 其他业务配置（负载均衡、缓存等，省略）
}
步骤2：检查配置并重启Nginx
# 1. 检查配置语法，避免配置错误
nginx -t
# 2. 重启Nginx，使配置生效
systemctl restart nginx  # yum安装
# /usr/local/nginx/sbin/nginx -s reload  # 源码编译安装
2.2 查看监控指标（核心操作）
配置生效后，通过浏览器或curl命令访问监控接口，查看基础监控指标，核心指标含义需牢记：
方式1：浏览器访问
访问 http://monitor.xxx.com/nginx_status（或 http://localhost/nginx_status），页面显示如下（核心指标已标注）：
Active connections: 23  # 当前活跃连接数（已建立的连接，包括正在处理的请求）
server accepts handled requests
  12345 12345 34567    # 依次为：总接受连接数、总处理连接数、总请求数
Reading: 0 Writing: 12 Waiting: 11  # 依次为：正在读取请求头的连接数、正在写入响应的连接数、空闲连接数
方式2：curl命令访问（推荐，适合服务器端快速查看）
curl http://localhost/nginx_status
输出结果与浏览器一致，可结合shell命令筛选关键指标（如查看当前活跃连接数）：
# 查看当前活跃连接数
curl -s http://localhost/nginx_status | grep "Active connections" | awk '{print $3}'
# 查看总请求数
curl -s http://localhost/nginx_status | awk 'NR==3 {print $3}'
2.3 基础监控核心指标解读（必懂）
Active connections：当前活跃连接数，正常情况下应稳定在一个合理范围（如10-100，根据并发量调整），若持续飙升，说明请求量过大或连接未正常释放；
总接受连接数/总处理连接数：两者应基本相等，若接受连接数远大于处理连接数，说明Nginx无法及时处理连接，可能存在性能瓶颈；
总请求数：累计请求数，可结合时间计算QPS（每秒请求数），如1小时内总请求数36000，则QPS=10；
Reading：正在读取请求头的连接数，若持续过高，说明客户端请求发送缓慢或网络异常；
Writing：正在写入响应的连接数，若持续过高，说明Nginx返回响应缓慢（如缓存未生效、后端响应慢）；
Waiting：空闲连接数，若过少，说明连接资源不足，需优化Nginx连接配置（如worker_connections）。
2.4 基础监控注意事项
安全限制：必须添加allow/deny配置，禁止公网IP访问监控接口，避免泄露Nginx运行信息；
日志优化：关闭监控接口的访问日志（access_log off），避免监控请求产生大量冗余日志；
适用场景：仅用于基础故障排查，无法满足中大型服务的长期监控需求，建议搭配第三方工具使用。
三、实战二：第三方工具监控（Prometheus+Grafana，企业级首选）
基础监控指标单一、无可视化界面，中大型服务需使用Prometheus+Grafana实现全方位监控：Prometheus负责采集Nginx指标，Grafana负责可视化展示，支持自定义仪表盘、异常告警，适配多Nginx节点集群，是企业级Nginx监控的首选方案。
部署流程：安装Prometheus → 安装Nginx监控插件（nginx-prometheus-exporter）→ 配置Prometheus采集指标 → 安装Grafana → 配置Grafana可视化仪表盘 → 配置告警规则。
3.1 前置准备（必做）
确保服务器已安装wget、yum-utils工具，关闭防火墙（或开放监控所需端口：9090、3000、9113）：
# 安装必要工具
yum install -y wget yum-utils
# 关闭防火墙（生产环境可开放指定端口，更安全）
systemctl stop firewalld
systemctl disable firewalld
# 开放端口（生产环境推荐，替代关闭防火墙）
firewall-cmd --add-port=9090/tcp --permanent  # Prometheus端口
firewall-cmd --add-port=3000/tcp --permanent  # Grafana端口
firewall-cmd --add-port=9113/tcp --permanent  # Nginx监控插件端口
firewall-cmd --reload
3.2 安装Prometheus（指标采集核心）
Prometheus是开源的指标采集工具，负责采集Nginx的运行指标，存储在本地时序数据库中，支持自定义采集频率。
步骤1：下载并解压Prometheus
# 1. 进入安装目录
cd /usr/local/
# 2. 下载Prometheus（版本2.45.0，稳定版）
wget https://github.com/prometheus/prometheus/releases/download/v2.45.0/prometheus-2.45.0.linux-amd64.tar.gz
# 3. 解压文件
tar -zxvf prometheus-2.45.0.linux-amd64.tar.gz
# 4. 重命名目录，方便管理
mv prometheus-2.45.0.linux-amd64 prometheus
步骤2：配置Prometheus（采集Nginx指标）
编辑Prometheus配置文件，添加Nginx指标采集规则，关联Nginx监控插件：
# 编辑配置文件
vi /usr/local/prometheus/prometheus.yml
# 新增Nginx采集配置（在scrape_configs节点下添加）
global:
  scrape_interval: 15s  # 采集频率，每15秒采集一次指标
  evaluation_interval: 15s
alerting:
  alertmanagers:
  - static_configs:
    - targets:
rule_files:
scrape_configs:
  # 采集Prometheus自身指标（默认配置）
  - job_name: 'prometheus'
    static_configs:
    - targets: ['localhost:9090']
  # 采集Nginx指标（核心配置）
  - job_name: 'nginx'
    static_configs:
    - targets: ['localhost:9113']  # Nginx监控插件的端口（默认9113）
    scrape_interval: 10s  # Nginx指标采集频率，每10秒一次
步骤3：启动Prometheus（设置开机自启）
# 1. 启动Prometheus（后台运行）
nohup /usr/local/prometheus/prometheus --config.file=/usr/local/prometheus/prometheus.yml &
# 2. 查看启动状态，确认启动成功
ps -ef | grep prometheus
# 3. 设置开机自启（编辑rc.local文件）
echo "/usr/local/prometheus/prometheus --config.file=/usr/local/prometheus/prometheus.yml &" >> /etc/rc.local
chmod +x /etc/rc.local
验证：浏览器访问 http://服务器IP:9090，若能打开Prometheus界面，说明启动成功。
3.3 安装Nginx监控插件（nginx-prometheus-exporter）
Prometheus本身无法直接采集Nginx指标，需安装nginx-prometheus-exporter插件，该插件通过访问Nginx的stub_status接口，采集指标并转换为Prometheus可识别的格式，供Prometheus采集。
步骤1：下载并安装插件
# 1. 进入安装目录
cd /usr/local/
# 2. 下载插件（版本1.1.0，稳定版）
wget https://github.com/nginxinc/nginx-prometheus-exporter/releases/download/v1.1.0/nginx-prometheus-exporter_1.1.0_linux_amd64.tar.gz
# 3. 解压文件
tar -zxvf nginx-prometheus-exporter_1.1.0_linux_amd64.tar.gz
# 4. 重命名目录，方便管理
mv nginx-prometheus-exporter_1.1.0_linux_amd64 nginx-exporter
步骤2：启动插件（关联stub_status接口）
启动插件时，需指定Nginx的stub_status接口地址，确保插件能采集到Nginx指标：
# 1. 后台启动插件（指定stub_status接口地址）
nohup /usr/local/nginx-exporter/nginx-prometheus-exporter -nginx.scrape-uri=http://localhost/nginx_status &
# 2. 查看启动状态，确认启动成功
ps -ef | grep nginx-prometheus-exporter
# 3. 设置开机自启
echo "/usr/local/nginx-exporter/nginx-prometheus-exporter -nginx.scrape-uri=http://localhost/nginx_status &" >> /etc/rc.local
验证：浏览器访问 http://服务器IP:9113/metrics，若能看到Nginx相关指标（如nginx_connections_active），说明插件启动成功，指标采集正常。
3.4 安装Grafana（可视化展示核心）
Grafana是开源的可视化工具，可连接Prometheus，通过自定义仪表盘，将Nginx指标以图表形式展示（如折线图、柱状图），直观呈现Nginx运行状态，支持自定义告警。
步骤1：添加Grafana软件源并安装
# 1. 添加Grafana软件源
cat > /etc/yum.repos.d/grafana.repo << EOF
[grafana]
name=grafana
baseurl=https://packages.grafana.com/oss/rpm
repo_gpgcheck=1
enabled=1
gpgcheck=1
gpgkey=https://packages.grafana.com/gpg.key
sslverify=1
sslcacert=/etc/pki/tls/certs/ca-bundle.crt
EOF
# 2. 安装Grafana
yum install -y grafana
# 3. 启动Grafana并设置开机自启
systemctl start grafana-server
systemctl enable grafana-server
# 4. 查看启动状态
systemctl status grafana-server
验证：浏览器访问 http://服务器IP:3000，若能打开Grafana登录界面（默认账号/密码：admin/admin），说明启动成功。
步骤2：配置Grafana连接Prometheus
登录Grafana（首次登录需修改密码，建议设置复杂密码）；
点击左侧「Configuration」→「Data Sources」→「Add data source」，选择「Prometheus」；
在「URL」中输入Prometheus地址：http://服务器IP:9090，其他默认，点击「Save & Test」，提示“Data source is working”即连接成功。
步骤3：导入Nginx可视化仪表盘（核心）
Grafana有大量开源的Nginx仪表盘模板，无需手动创建，直接导入即可，推荐使用模板ID：12708（适配nginx-prometheus-exporter插件）：
点击左侧「Dashboards」→「Import」；
在「Import via grafana.com」中输入模板ID：12708，点击「Load」；
在「Data source」中选择已配置的Prometheus，点击「Import」；
导入成功后，即可看到Nginx可视化仪表盘，包含连接数、请求数、异常状态码、后端响应耗时等多维度指标，实时更新。
补充：若模板ID 12708不适用，可在Grafana官网（https://grafana.com/grafana/dashboards/）搜索“Nginx”，选择适配nginx-prometheus-exporter的模板，导入即可。
3.5 配置监控告警（企业级必备）
监控的核心目的是“异常告警”，当Nginx指标出现异常（如活跃连接数过高、5xx状态码激增），Grafana需及时发送告警通知（如邮件、企业微信），确保运维人员及时处理。
步骤1：配置告警渠道（以企业微信为例）
登录企业微信，创建应用（如“Nginx监控告警”），获取「CorpID」「AgentID」「Secret」；
在Grafana中，点击左侧「Alerting」→「Contact points」→「Add contact point」；
「Name」输入“企业微信告警”，「Type」选择“Enterprise WeChat”，填写CorpID、AgentID、Secret，点击「Test」，测试是否能收到告警消息，测试成功后点击「Save」。
步骤2：配置告警规则（核心指标）
针对Nginx核心指标，配置告警规则，示例如下（可根据业务需求调整阈值）：
点击左侧「Alerting」→「Alert rules」→「Create alert rule」；
「Rule name」输入“Nginx活跃连接数过高”；
「Data source」选择Prometheus，「Query」输入指标：nginx_connections_active，「Threshold」设置阈值（如500），条件选择“Is above”；
「Evaluation group」选择“default”，「Evaluation interval」设置为10s；
「Contact point」选择已配置的“企业微信告警”，「Message」填写告警内容（如“Nginx活跃连接数超过500，当前值：{{ $value }}，请及时排查”）；
点击「Save」，告警规则生效，当活跃连接数超过500时，会自动发送告警通知。
推荐配置的告警规则（企业级常用）：
活跃连接数：超过500（根据并发量调整）；
5xx状态码：1分钟内超过10个；
后端响应耗时：超过1秒的请求占比超过10%；
Nginx进程：未运行（指标nginx_up == 0）。
四、Nginx监控日常管理（企业级规范）
监控配置完成后，需做好日常管理，定期检查监控状态、优化监控策略、清理监控数据，确保监控服务稳定运行，避免监控失效。
4.1 日常检查（每日必做）
检查监控服务状态：确认Prometheus、Grafana、nginx-prometheus-exporter均正常运行； systemctl status prometheus # 若未设置systemd，用ps -ef | grep prometheus systemctl status grafana-server ps -ef | grep nginx-prometheus-exporter
检查指标采集情况：访问Grafana仪表盘，查看指标是否实时更新，无数据缺失；
检查告警规则：确认告警规则正常生效，测试告警渠道是否能正常接收通知；
查看监控日志：排查监控服务异常，如Prometheus采集失败、Grafana连接异常。
4.2 监控数据清理（定期必做）
Prometheus会将采集的指标存储在本地磁盘，长期运行会导致磁盘空间占用过大，需定期清理过期数据，配置数据保留时间：
# 编辑Prometheus配置文件，添加数据保留时间（保留30天，可调整）
vi /usr/local/prometheus/prometheus.yml
# 在global节点下添加
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  storage_retention: 30d  # 数据保留30天，过期自动清理
# 重启Prometheus，使配置生效
pkill prometheus
nohup /usr/local/prometheus/prometheus --config.file=/usr/local/prometheus/prometheus.yml 
补充：若磁盘空间紧张，可将保留时间调整为15天，同时定期检查磁盘空间，手动清理异常数据。
4.3 监控策略优化（定期优化）
调整采集频率：根据业务需求调整，高并发场景可将采集频率改为5s，低并发场景可改为30s，避免采集过于频繁占用资源；
优化告警阈值：根据实际运行情况调整告警阈值，避免误告警（如活跃连接数阈值，根据日常并发量调整）；
新增监控指标：若业务有特殊需求（如缓存命中率、负载均衡节点健康状态），可新增采集规则，补充到Grafana仪表盘；
多节点监控：若有多个Nginx节点，只需在每个节点安装nginx-prometheus-exporter，在Prometheus配置文件中添加多个targets，即可实现多节点统一监控。
五、监控常见故障排查（实战必备）
监控实战中，常见故障集中在“指标采集失败、Grafana无数据、告警不生效”，以下是最常见的故障及排查方法，贴合生产环境场景，结合此前教程的负载均衡、缓存、日志管理知识，快速定位问题。
故障1：Prometheus采集不到Nginx指标（Grafana无数据）
原因：nginx-prometheus-exporter未启动、插件未正确关联stub_status接口、Prometheus配置错误、端口被占用。
解决方案： 检查nginx-prometheus-exporter状态：执行ps -ef | grep nginx-prometheus-exporter，确认插件已启动；验证插件指标采集：访问 http://服务器IP:9113/metrics，查看是否有nginx相关指标，若没有，重启插件并检查启动命令（确保指定了正确的stub_status接口地址）；检查Prometheus配置：确认prometheus.yml中nginx的targets地址（localhost:9113）正确，重启Prometheus；检查端口占用：执行netstat -tulpn | grep 9113，确认9113端口未被其他服务占用，若占用，停止占用服务或修改插件端口。
故障2：Grafana连接Prometheus失败
原因：Prometheus未启动、Prometheus端口未开放、Grafana中Prometheus地址配置错误、网络异常。
解决方案： 检查Prometheus状态：确认Prometheus已启动，访问 http://服务器IP:9090，确认能正常打开；检查端口开放：执行firewall-cmd --list-ports，确认9090端口已开放；检查Grafana配置：确认Data Sources中Prometheus的URL地址正确（如http://192.168.1.100:9090，而非localhost:9090，避免本地访问限制）；测试网络连接：在Grafana服务器上执行curl http://服务器IP:9090，确认能正常访问Prometheus。
故障3：告警不生效（异常时未收到通知）
原因：告警规则配置错误、告警渠道配置错误、指标未达到告警阈值、Grafana未启动。
解决方案： 检查告警规则：确认告警指标、阈值、条件配置正确，测试告警规则（手动触发告警）；检查告警渠道：确认企业微信/邮件等告警渠道配置正确，点击「Test」测试是否能收到通知；检查指标状态：查看Grafana仪表盘，确认指标是否达到告警阈值，若未达到，调整阈值或模拟异常场景；检查Grafana状态：确认Grafana已启动，查看Grafana错误日志，排查告警服务异常：cat /var/log/grafana/grafana.log。
故障4：stub_status接口无法访问
原因：Nginx配置错误、allow/deny权限限制、Nginx未重启、端口被占用。
解决方案： 检查Nginx配置：确认stub_status on已配置，allow允许当前IP访问，deny配置正确；检查Nginx状态：重启Nginx，执行nginx -t排查配置语法错误；测试本地访问：在Nginx服务器上执行curl http://localhost/nginx_status，若能访问，说明是远程IP权限限制，调整allow配置；检查端口占用：确认80端口未被其他服务占用，若占用，修改Nginx监听端口。
六、实战总结
Nginx监控的实战核心，是“基础监控兜底、第三方工具升级”，其核心价值是“提前发现故障、快速定位问题、保障服务稳定”，是企业级Nginx运维的必备技能，与此前Nginx缓存、负载均衡、日志管理形成完整的运维闭环。对运维人员和Java后端开发而言，重点掌握以下几点，即可应对绝大多数企业级Nginx监控需求：
核心重点：基础监控（stub_status）适合快速排查基础故障，第三方工具（Prometheus+Grafana）适合中大型服务，实现可视化和告警；
必做操作：开启stub_status模块，部署Prometheus+Grafana+nginx-prometheus-exporter，导入可视化仪表盘，配置核心指标告警规则；
监控核心：重点关注活跃连接数、QPS、异常状态码、后端响应耗时，这些指标直接反映Nginx运行状态和服务可用性；
管理核心：定期检查监控服务状态、清理过期监控数据、优化监控策略，确保监控服务稳定运行，避免监控失效；
排查核心：指标采集失败查插件/配置，Grafana无数据查连接/Prometheus，告警不生效查规则/渠道，结合日志管理快速定位问题。
实际运维中，可根据服务规模、并发量、业务需求，灵活选择监控方式，调整采集频率、告警阈值，结合本文的实战配置和排查方法，可快速实现Nginx监控的规范化、可视化管理，为Nginx服务的稳定运行提供保障，提升运维效率，减少服务中断风险。

