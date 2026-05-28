Redis快速安装指南
本文将提供Redis在主流操作系统（Linux、Windows、macOS）及Docker环境下的快速安装步骤，全程简化冗余操作，聚焦“快速部署、即时可用”，适合新手快速上手，同时补充关键验证步骤和注意事项，避免安装踩坑。
一、前置说明
1. 推荐安装Redis 6.0+版本，支持更多新特性（如ACL权限控制、IO多线程），本文步骤适配主流稳定版本；
2. 安装完成后均需执行验证步骤，确保Redis服务正常启动；
3. 生产环境需额外配置安全（如密码、IP限制）和性能参数，本文仅覆盖“快速安装”，后续可参考Redis配置指南优化。
    二、各平台快速安装步骤
    （一）Linux系统（Ubuntu/Debian，最常用）
    全程终端命令操作，无需复杂配置，3步完成安装：
    更新软件源，确保包管理器获取最新Redis安装包： sudo apt update
    安装Redis服务，自动完成依赖配置： sudo apt install redis-server -y
    启动服务并设置开机自启（避免重启后失效）： sudo systemctl enable redis-server sudo systemctl start redis-server
    （二）Linux系统（CentOS/RHEL）
    需先添加EPEL源，再执行安装，步骤如下：
    安装EPEL源（Redis不在CentOS默认源中）： sudo yum install epel-release -y
    安装Redis服务： sudo yum install redis -y
    启动服务并设置开机自启： sudo systemctl start redis sudo systemctl enable redis
    （三）macOS系统（Homebrew推荐）
    依赖Homebrew包管理器，未安装Homebrew需先执行安装命令，步骤简洁：
    （可选）安装Homebrew（已安装可跳过）： /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    安装Redis： brew install redis
    启动Redis服务（后台运行）： brew services start redis
    （四）Windows系统（推荐WSL2，原生版不推荐）
    Redis官方已停止维护Windows原生版本，仅适合开发测试，推荐使用WSL2安装（兼容Linux命令，更稳定）：
    方式1：WSL2安装（推荐）
    启用WSL2（管理员身份打开PowerShell）： dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart
    重启电脑后，设置WSL2为默认版本： wsl --set-default-version 2
    Microsoft Store安装Ubuntu（如Ubuntu 22.04 LTS），打开Ubuntu终端，按“Linux（Ubuntu/Debian）”步骤安装Redis即可。
    方式2：Windows原生版（仅开发测试）
    下载微软维护的移植版：访问GitHub地址（https://github.com/microsoftarchive/redis），下载最新.zip压缩包；
    解压压缩包至指定目录（如C:\Redis）；
    管理员打开命令提示符，切换至解压目录，启动服务： cd C:\Redis redis-server.exe redis.windows.conf
    （五）Docker环境（跨平台通用，最便捷）
    适合快速部署、隔离环境，无需配置系统依赖，前提是已安装Docker Desktop：
    拉取Redis最新稳定版镜像： docker pull redis:latest
    启动Redis容器（映射6379端口，后台运行）： docker run --name redis-container -p 6379:6379 -d redis:latest
    （可选）带密码启动（提升安全性）： docker run --name redis-container -p 6379:6379 -d redis:latest --requirepass "yourStrongPassword"
    三、安装验证步骤（所有平台通用）
    安装完成后，执行以下命令验证Redis服务是否正常可用，步骤统一：
    打开终端/命令提示符，进入Redis客户端： - 非Docker环境：redis-cli - Docker环境：docker exec -it redis-container redis-cli（若设置密码，需先执行auth 你的密码）
    执行 ping 命令，返回 PONG 即表示服务正常： ping
    （可选）测试简单键值操作，验证功能正常： set test "redis-install-success"（返回OK） get test（返回"redis-install-success"）
    四、快速卸载步骤（可选）
    若需卸载Redis，执行对应平台命令，快速清理：
    Ubuntu/Debian：sudo apt remove redis-server -y
    CentOS/RHEL：sudo yum remove redis -y
    macOS（Homebrew）：brew uninstall redis
    Docker：docker stop redis-container && docker rm redis-container
    Windows原生版：直接删除解压目录，终止redis-server进程即可。
    五、注意事项（避坑关键）
    Linux/CentOS安装后，默认绑定127.0.0.1，仅本地可访问，若需远程访问，需修改配置文件（如/etc/redis/redis.conf），注释bind 127.0.0.1，重启服务；
    Windows原生版Redis已停止维护，不建议用于生产环境，生产优先选择Linux或Docker部署；
    Docker启动Redis后，容器停止则服务终止，需设置容器自动重启（添加--restart=always参数），避免重启电脑后服务失效；
    安装失败时，优先检查网络（是否能正常获取安装包）、权限（是否使用管理员/root权限），Linux系统可通过sudo systemctl status redis-server查看服务异常日志。
    六、总结
    以上步骤覆盖主流平台，全程聚焦“快速安装”，无需复杂配置，完成安装+验证仅需5分钟。若需用于生产环境，建议后续配置密码、调整内存策略、开启持久化，确保服务稳定安全。
