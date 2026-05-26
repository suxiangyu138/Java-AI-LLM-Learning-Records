从 Java 后端视角深度剖析 Shell：Shell 编程环境搭建（理论 + 实战）
一、为什么 Java 后端必须搭建 Shell 环境
对 Java 后端来说，Shell 不是“顺便学学”，而是服务器操作、服务部署、日志排查、CI/CD、自动化脚本的基础。
Shell 环境搭建的核心目标：
- 能在本地写 Shell 脚本
- 能连接 Linux 服务器执行命令
- 能语法高亮、格式化、调试、一键运行
- 能和 Java 项目（jar、Docker、服务启停）配合使用
 
二、Shell 运行环境基础认知（Java 后端视角）
1. Shell 本质
    Shell = 命令解释器，用户 ↔ Linux 内核。
    Java 后端常用：
    - bash（几乎所有 Linux 默认）
    - sh（兼容 bash）
    - zsh（本地开发更舒服）
2. 脚本执行三要素
    1. 脚本第一行： #!/bin/bash （指定解释器）
    2. 执行权限： chmod +x xxx.sh 
    3. 运行方式： ./xxx.sh  或  bash xxx.sh 
3. 本地 vs 服务器环境
    - 本地：写脚本、测试简单逻辑
    - 服务器：真实运行 Java 服务、Docker、Nginx、日志等
    环境搭建要本地 + 远程一套配齐。
 
三、Windows 下搭建 Shell 编程环境（最常用）
方案1：Git Bash（最简单、推荐）
适合 Java 后端快速上手，不需要虚拟机。
安装步骤：
1. 下载 Git for Windows
2. 安装时默认下一步
3. 开始菜单找到 Git Bash 打开
4. 测试：
    plaintext
    bash --version
    ls
    pwd
 
优点：
- 自带 ssh、scp、curl、vim
- 能直接写 sh 脚本并运行
- 与服务器 Linux 命令高度一致
    方案2：WSL2（专业级，接近真实 Linux）
    适合想深度学习 Shell、Docker、Java 部署的人。
    开启步骤：
    1. 以管理员打开 PowerShell 执行：
    plaintext
    wsl --install
 
2. 重启电脑
3. 安装 Ubuntu（微软商店）
4. 设置用户名密码
5. 进入 Ubuntu 安装常用工具：
    plaintext
    sudo apt update
    sudo apt install vim curl wget git net-tools
 
之后直接在 WSL 内写 Shell、运行 Java、模拟服务器环境。
方案3：虚拟机 / VMware + CentOS 7/8
企业真实环境几乎都是 CentOS。
适合想完全模拟线上部署的 Java 后端。
 
四、Mac 下搭建 Shell 环境
Mac 自带 Unix 环境，开箱即用。
默认是 zsh，兼容 bash。
打开终端直接：
plaintext
bash --version
vim test.sh
 
如需更舒适：
- 安装 iTerm2
- 配置 oh-my-zsh（可选）
 
五、远程 Linux 服务器环境准备（Java 后端核心）
1. 服务器必备基础工具安装
    CentOS/RHEL：
    plaintext
    yum install -y vim wget curl net-tools lsof telnet
 
Ubuntu/Debian：
plaintext
apt install -y vim wget curl net-tools lsof telnet
 
2. Java 运行环境（必装）
    Shell 脚本最终要启动 jar，所以服务器必须装 JDK。
    plaintext
    java -version
 
没有则安装 OpenJDK 8 / 11。
3. 目录结构规范（建议）
    为后续写脚本做准备，统一结构：
    plaintext
    /home/app/xxx项目
    ├── bin/      脚本（start.sh stop.sh status.sh）
    ├── lib/      jar包
    ├── logs/     日志
    └── conf/     配置
 
 
六、IDE 集成：Java 后端写 Shell 的最佳方式
1. IDEA / PyCharm 配置 Shell 插件
    IDEA 自带 Shell 支持，也可安装：
    - Shell Script（官方）
    - BashSupport
    配置步骤：
    1. File → Settings → Plugins
    2. 搜索 Shell Script 确保启用
    3. 新建文件  test.sh  自动识别
    功能：
    - 语法高亮
    - 语法检查
    - 格式化
    - 直接右键 Run 运行
    - 变量提示、函数提示
    对 Java 后端非常友好，和写 Java 一样舒适。
2. 脚本模板（直接用）
    在 IDEA 里新建脚本时可用固定模板：
    bash

# /bin/bash

# ====================

# 功能:

# 作者:

# 日期:

# ====================
set -euo pipefail

# 变量定义
APP_NAME="demo.jar"

# 主逻辑
echo "start..."
 
 
七、Shell 脚本运行环境配置（关键）
1. 脚本编码统一为 UTF-8
    Windows 下编辑容易出现 GBK 乱码。
    IDEA 设置：
    - File → Settings → Editor → File Encodings
    - 全部设为 UTF-8
2. 换行符统一为 LF（Linux 格式）
    Windows 默认 CRLF，传到 Linux 会报错  ^M 。
    IDEA 右下角切换：
    - CRLF → LF
3. 开启脚本严格模式（企业规范）
    在脚本开头加：
    plaintext
    set -euo pipefail
 
作用：
- -e 出错立即退出
- -u 未定义变量直接报错
- -o pipefail 管道任一环节失败都算失败
    Java 后端部署脚本强烈建议开启，避免线上事故。
 
八、本地测试环境验证（快速入门实战）
新建  hello.sh 
bash

# /bin/bash
echo "Hello Shell from Java Backend"
echo "Current path: $(pwd)"
echo "Java version:"
java -version 2>/dev/null || echo "Java not installed"
 
赋予权限并运行：
plaintext
chmod +x hello.sh
./hello.sh
 
看到正常输出，说明环境搭建完成。
 
九、远程连接环境（Java 后端必备）
1. SSH 连接
    plaintext
    ssh user@server-ip
 
2. 文件传输（上传脚本、jar）
    plaintext
    scp start.sh user@server-ip:/home/app/bin/
 
3. IDEA 远程开发（直接在服务器写脚本）
    使用：
    - IDEA Remote Development
    - 或 SFTP 插件
    可直接在本地编辑服务器上的 sh 脚本，一键保存同步。
 
十、环境搭建完成后的验收标准
1. 本地能写  .sh  脚本，语法高亮、无报错
2. 能运行  bash xxx.sh 
3. 能 SSH 连接 Linux
4. 能 scp 上传脚本到服务器
5. 服务器能执行  chmod +x  并运行
6. 服务器能通过 Shell 脚本启动/停止 Java 进程
    满足以上，Shell 编程环境完全就绪。
 
十一、下一篇预告
Shell 环境搭建完成后，接下来进入：
- 变量、字符串、数组
- 条件判断 if/else
- 循环 for/while
- 函数
- 管道、重定向、grep/awk/sed
- Java 后端自动化部署脚本实战
