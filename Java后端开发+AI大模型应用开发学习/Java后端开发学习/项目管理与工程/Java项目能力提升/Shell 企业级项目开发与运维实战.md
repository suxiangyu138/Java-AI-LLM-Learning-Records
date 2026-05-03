03.31 03:35
Shell 企业级项目开发与运维实战
一、Shell 企业级项目概述
Shell 是 Linux/Unix 系统的命令解释器，企业级 Shell 项目主要用于自动化运维、服务部署、日志管理、监控告警、数据备份、定时任务等场景，具备高可用、可复用、可扩展、易维护的特点。
二、环境准备
1. Linux 系统（CentOS 7/8、Ubuntu 20.04+）
2. 基础工具：vim、curl、wget、git、tar、zip、unzip
3. Shell 规范：bash（推荐）、严格语法、注释完善、错误处理
三、企业级 Shell 项目架构
plaintext
/opt/shell-enterprise
├── bin/          # 主脚本入口
├── lib/          # 公共函数库
├── conf/         # 配置文件
├── logs/         # 日志输出
├── tmp/          # 临时文件
├── backup/       # 备份文件
└── tools/        # 辅助脚本
 
四、核心模块开发
1. 公共函数库（lib/common.sh）
bash
#!/bin/bash
# 公共函数库：日志、颜色、错误处理、配置加载
# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color
# 日志文件路径
LOG_DIR="/opt/shell-enterprise/logs"
LOG_FILE="${LOG_DIR}/shell-enterprise.log"
mkdir -p ${LOG_DIR}
# 日志函数
log_info() {
    echo -e "[$(date +'%Y-%m-%d %H:%M:%S')] [INFO] $*" | tee -a ${LOG_FILE}
}
log_warn() {
    echo -e "[$(date +'%Y-%m-%d %H:%M:%S')] [${YELLOW}WARN${NC}] $*" | tee -a ${LOG_FILE}
}
log_error() {
    echo -e "[$(date +'%Y-%m-%d %H:%M:%S')] [${RED}ERROR${NC}] $*" | tee -a ${LOG_FILE}
}
# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "命令 $1 未安装，请先安装"
        exit 1
    fi
}
# 加载配置文件
load_config() {
    local config_file="/opt/shell-enterprise/conf/config.conf"
    if [ ! -f ${config_file} ]; then
        log_error "配置文件 ${config_file} 不存在"
        exit 1
    fi
    source ${config_file}
}
# 检查目录是否存在，不存在则创建
check_dir() {
    if [ ! -d $1 ]; then
        log_info "目录 $1 不存在，自动创建"
        mkdir -p $1
    fi
}
 
2. 配置文件（conf/config.conf）
bash
# 数据库配置
DB_HOST="localhost"
DB_PORT="3306"
DB_USER="root"
DB_PASS="123456"
DB_NAME="enterprise_db"
# 备份配置
BACKUP_DIR="/opt/shell-enterprise/backup"
BACKUP_RETENTION=7 # 保留7天
# 服务配置
SERVICE_NAME="enterprise-project"
SERVICE_PORT="8080"
# 监控配置
ALERT_EMAIL="admin@company.com"
MONITOR_INTERVAL=60 # 监控间隔（秒）
 
3. 数据库备份脚本（tools/backup_db.sh）
bash
#!/bin/bash
# MySQL数据库自动备份脚本
# 加载公共函数库
source /opt/shell-enterprise/lib/common.sh
# 加载配置
load_config
# 检查依赖命令
check_command "mysqldump"
check_command "gzip"
# 检查备份目录
check_dir ${BACKUP_DIR}
# 备份文件名
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_$(date +%Y%m%d_%H%M%S).sql.gz"
# 执行备份
log_info "开始备份数据库 ${DB_NAME}..."
mysqldump -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASS} ${DB_NAME} | gzip > ${BACKUP_FILE}
if [ $? -eq 0 ]; then
    log_info "数据库备份成功：${BACKUP_FILE}"
else
    log_error "数据库备份失败"
    exit 1
fi
# 删除过期备份
log_info "删除 ${BACKUP_RETENTION} 天前的备份文件"
find ${BACKUP_DIR} -name "${DB_NAME}_*.sql.gz" -mtime +${BACKUP_RETENTION} -delete
log_info "数据库备份任务完成"
 
4. 服务监控脚本（tools/monitor_service.sh）
bash
#!/bin/bash
# 服务监控与自动重启脚本
# 加载公共函数库
source /opt/shell-enterprise/lib/common.sh
# 加载配置
load_config
# 检查依赖命令
check_command "curl"
check_command "ps"
# 检查服务状态
log_info "检查服务 ${SERVICE_NAME} 状态..."
SERVICE_PID=$(ps -ef | grep ${SERVICE_NAME} | grep -v grep | grep -v monitor | awk '{print $2}')
if [ -z "${SERVICE_PID}" ]; then
    log_warn "服务 ${SERVICE_NAME} 未运行，尝试启动..."
    /opt/enterprise-project/bin/start.sh
    sleep 5
    # 再次检查
    SERVICE_PID=$(ps -ef | grep ${SERVICE_NAME} | grep -v grep | grep -v monitor | awk '{print $2}')
    if [ -z "${SERVICE_PID}" ]; then
        log_error "服务 ${SERVICE_NAME} 启动失败"
        # 发送告警邮件（需配置sendmail）
        echo "服务 ${SERVICE_NAME} 启动失败" | mail -s "服务告警" ${ALERT_EMAIL}
    else
        log_info "服务 ${SERVICE_NAME} 启动成功，PID: ${SERVICE_PID}"
    fi
else
    # 检查端口是否监听
    PORT_STATUS=$(netstat -tlnp | grep ${SERVICE_PORT} | wc -l)
    if [ ${PORT_STATUS} -eq 0 ]; then
        log_warn "服务端口 ${SERVICE_PORT} 未监听，重启服务"
        /opt/enterprise-project/bin/stop.sh
        sleep 3
        /opt/enterprise-project/bin/start.sh
    else
        log_info "服务 ${SERVICE_NAME} 运行正常，PID: ${SERVICE_PID}，端口: ${SERVICE_PORT}"
    fi
fi
 
5. 日志清理脚本（tools/clean_logs.sh）
bash
#!/bin/bash
# 日志文件自动清理脚本
# 加载公共函数库
source /opt/shell-enterprise/lib/common.sh
# 加载配置
load_config
# 清理应用日志
APP_LOG_DIR="/opt/enterprise-project/logs"
if [ -d ${APP_LOG_DIR} ]; then
    log_info "清理应用日志目录 ${APP_LOG_DIR}..."
    find ${APP_LOG_DIR} -name "*.log" -mtime +30 -delete
    log_info "应用日志清理完成"
else
    log_warn "应用日志目录 ${APP_LOG_DIR} 不存在"
fi
# 清理系统日志（可选）
SYS_LOG_DIR="/var/log"
log_info "清理系统日志 ${SYS_LOG_DIR}..."
find ${SYS_LOG_DIR} -name "*.log" -mtime +90 -delete
find ${SYS_LOG_DIR} -name "*.gz" -mtime +90 -delete
log_info "系统日志清理完成"
 
6. 主入口脚本（bin/main.sh）
bash
#!/bin/bash
# Shell企业级项目主入口脚本
# 加载公共函数库
source /opt/shell-enterprise/lib/common.sh
# 检查参数
if [ $# -ne 1 ]; then
    log_error "用法: $0 {backup|monitor|clean}"
    exit 1
fi
# 执行对应功能
case $1 in
    backup)
        log_info "执行数据库备份任务"
        /opt/shell-enterprise/tools/backup_db.sh
        ;;
    monitor)
        log_info "执行服务监控任务"
        /opt/shell-enterprise/tools/monitor_service.sh
        ;;
    clean)
        log_info "执行日志清理任务"
        /opt/shell-enterprise/tools/clean_logs.sh
        ;;
    *)
        log_error "无效参数: $1，支持参数: backup|monitor|clean"
        exit 1
        ;;
esac
log_info "任务执行完成"
 
五、权限配置
bash
# 创建项目目录
mkdir -p /opt/shell-enterprise/{bin,lib,conf,logs,tmp,backup,tools}
# 赋予执行权限
chmod +x /opt/shell-enterprise/bin/main.sh
chmod +x /opt/shell-enterprise/tools/*.sh
chmod +x /opt/shell-enterprise/lib/common.sh
# 设置目录权限
chown -R root:root /opt/shell-enterprise
chmod -R 755 /opt/shell-enterprise
chmod -R 777 /opt/shell-enterprise/logs
chmod -R 777 /opt/shell-enterprise/tmp
 
六、定时任务配置（crontab）
bash
# 编辑定时任务
crontab -e
# 每日凌晨2点备份数据库
0 2 * * * /opt/shell-enterprise/bin/main.sh backup >> /opt/shell-enterprise/logs/cron_backup.log 2>&1
# 每分钟监控服务状态
* * * * * /opt/shell-enterprise/bin/main.sh monitor >> /opt/shell-enterprise/logs/cron_monitor.log 2>&1
# 每周日凌晨4点清理日志
0 4 * * 0 /opt/shell-enterprise/bin/main.sh clean >> /opt/shell-enterprise/logs/cron_clean.log 2>&1
# 查看定时任务
crontab -l
 
七、企业级扩展功能
1. 配置文件加密：使用 openssl 加密敏感配置
2. 并发控制：使用 flock 防止脚本重复执行
3. 钉钉/企业微信告警：替换邮件告警，支持webhook推送
4. 多环境适配：开发、测试、生产环境配置分离
5. 脚本升级：支持自动更新脚本版本
6. 运行统计：记录脚本执行次数、耗时、成功率
八、安全规范
1. 敏感信息（密码、密钥）不硬编码，使用配置文件或环境变量
2. 脚本执行权限最小化，避免使用root用户执行
3. 日志记录完整，便于问题排查
4. 关键操作前备份，操作后校验
5. 定期审计脚本权限与执行记录

