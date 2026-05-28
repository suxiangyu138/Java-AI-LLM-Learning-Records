Linux 核心知识点
一、 Linux 概述
1. Linux 的定义与特点
    - Linux 是一套免费开源的类 UNIX 操作系统，内核由林纳斯·托瓦兹于 1991 年发布，遵循 GNU 通用公共许可证（GPL），用户可自由修改、分发源代码。
    - 核心特点：开源免费、多用户多任务（支持多个用户同时登录，多个任务并行执行）、高稳定性（可长期不间断运行）、高安全性（内置强大的权限机制）、良好的可移植性（支持 x86、ARM 等多种硬件架构）、丰富的网络功能（原生支持各类网络协议，适合作为服务器系统）。
2. Linux 系统的组成
    - 内核（Kernel）：系统的核心，负责管理硬件资源（CPU、内存、磁盘、网络等）、进程调度、内存管理、文件系统管理等，是用户程序与硬件之间的桥梁。
    - Shell：命令解释器，是用户与内核交互的接口，接收用户输入的命令并解析执行，常见的 Shell 有 Bash（默认）、Zsh、Ksh 等。
    - 文件系统：Linux 中一切皆文件，包括硬件设备、目录、普通文件等，采用树形目录结构，根目录为  / ，所有文件和目录都挂载在根目录下。
    - 应用程序：系统自带的工具和第三方软件，如文本编辑器（vim）、编译器（gcc）、网络工具（curl、wget）等。
3. Linux 发行版
    - 内核是 Linux 的核心，但单独的内核无法直接使用，发行版是将内核与 Shell、应用程序、桌面环境等打包而成的完整系统。
    - 常见发行版：Ubuntu（易用性高，适合新手和桌面用户）、CentOS（稳定可靠，适合服务器，已停止维护，替代版本为 Rocky Linux）、Red Hat Enterprise Linux（RHEL）（商业版，提供技术支持）、Debian（稳定性强，开源社区驱动）、Fedora（更新快，适合尝鲜新技术）。
    二、 Linux 文件系统与目录结构
    1. Linux 文件系统的特点
    - 树形结构：根目录  /  是所有目录的起点，其他目录均为根目录的子目录。
    - 一切皆文件：硬件设备（如硬盘  /dev/sda 、键盘  /dev/input ）、管道、套接字等都以文件形式存在，通过统一的接口访问。
    - 文件类型：不依赖后缀名区分类型，常见类型有：普通文件（ - ）、目录文件（ d ）、链接文件（ l ）、字符设备文件（ c ）、块设备文件（ b ）、管道文件（ p ）、套接字文件（ s ）。
    2. 核心目录功能
    目录 功能说明 
     /  根目录，所有文件和目录的顶级目录 
     /bin  存放系统基本命令的可执行文件（如 ls、cd、cp），所有用户均可执行 
     /sbin  存放系统管理命令的可执行文件（如 ifconfig、reboot），需 root 权限执行 
     /etc  存放系统配置文件（如 passwd、fstab、ssh/sshd_config） 
     /home  普通用户的主目录，每个用户有一个子目录（如  /home/user1 ） 
     /root  超级用户（root）的主目录 
     /usr  存放系统应用程序和文档，相当于 Windows 的 Program Files 
     /var  存放动态变化的文件（如日志文件  /var/log 、邮件  /var/spool ） 
     /tmp  存放临时文件，系统重启后内容会被清空 
     /dev  存放硬件设备文件（如硬盘  /dev/sda 、终端  /dev/tty ） 
     /proc  虚拟文件系统，存放系统运行时的内核信息（如进程、内存状态），不占用磁盘空间 
3. 文件权限管理
    - 权限的表示：Linux 文件权限分为所有者（u）、所属组（g）、其他用户（o） 三类，每类用户有 读（r）、写（w）、执行（x） 三种权限。
    - 字符表示：如  rwxr-xr-- ，表示所有者可读可写可执行，所属组可读可执行，其他用户只读。
    - 数字表示： r=4 、 w=2 、 x=1 ，权限组合为数字之和，如  rwx  对应  7 ， r-x  对应  5 ， r--  对应  4 ，上述权限的数字表示为  754 。
    - 权限修改命令：
    -  chmod ：修改文件或目录的权限，格式  chmod [选项] 权限 文件名 。
    - 示例： chmod 755 test.txt （设置权限为 rwxr-xr-x）、 chmod u+x test.sh （给所有者添加执行权限）。
    -  chown ：修改文件的所有者和所属组，格式  chown [选项] 所有者:所属组 文件名 。
    - 示例： chown root:root test.txt （将文件所有者和所属组改为 root）。
    -  chgrp ：修改文件的所属组，格式  chgrp 所属组 文件名 。
    三、 Linux 常用命令
    （一） 文件与目录操作命令
    1.  ls ：列出目录内容
    - 常用选项： -l （详细信息）、 -a （显示隐藏文件，以  .  开头的文件）、 -h （人性化显示文件大小）。
    - 示例： ls -lah /home 。
    2.  cd ：切换工作目录
    - 示例： cd /etc （切换到 /etc 目录）、 cd ~ （切换到当前用户主目录）、 cd .. （切换到上级目录）。
    3.  pwd ：显示当前工作目录的绝对路径。
    4.  mkdir ：创建目录
    - 常用选项： -p （递归创建多级目录）。
    - 示例： mkdir -p /tmp/test/abc 。
    5.  rm ：删除文件或目录
    - 常用选项： -r （递归删除目录）、 -f （强制删除，不提示）。
    - 示例： rm -rf /tmp/test （强制删除 test 目录及其内容）。
    6.  cp ：复制文件或目录
    - 常用选项： -r （递归复制目录）、 -p （保留文件属性）。
    - 示例： cp test.txt /home （复制文件到 /home）、 cp -r dir1 /tmp （复制目录到 /tmp）。
    7.  mv ：移动或重命名文件/目录
    - 示例： mv test.txt /home （移动文件）、 mv oldname.txt newname.txt （重命名文件）。
    8.  touch ：创建空文件或修改文件的时间戳
    - 示例： touch newfile.txt 。
    9.  cat ：查看文件内容（适合小文件）
    - 示例： cat /etc/passwd 。
    10.  more/less ：分页查看文件内容（适合大文件）
    -  more ：只能向下翻页，按  q  退出。
    -  less ：可上下翻页，按  q  退出，功能更强大。
    11.  head/tail ：查看文件开头/结尾的内容
    - 常用选项： -n （指定行数，默认 10 行）。
    - 示例： head -n 5 /etc/passwd （查看前 5 行）、 tail -n 10 /var/log/messages （查看后 10 行）、 tail -f /var/log/nginx/access.log （实时跟踪日志文件）。
    12.  find ：查找文件或目录
    - 格式： find 查找路径 -name 文件名 。
    - 示例： find / -name "*.txt" （在根目录下查找所有 .txt 文件）、 find /home -user root （查找 /home 下所有者为 root 的文件）。
    13.  ln ：创建链接文件
    - 硬链接： ln 源文件 链接文件 ，与源文件共享同一个 inode，删除源文件不影响硬链接，不能跨文件系统，不能链接目录。
    - 软链接（符号链接）： ln -s 源文件 链接文件 ，相当于快捷方式，与源文件的 inode 不同，删除源文件后软链接失效，可以跨文件系统，可以链接目录。
    - 示例： ln -s /usr/local/mysql/bin/mysql /usr/bin/mysql （创建软链接，方便全局调用 mysql 命令）。
    （二） 系统管理命令
    1.  whoami ：显示当前登录的用户名。
    2.  who/w ：显示当前登录系统的所有用户信息。
    3.  su ：切换用户
    - 格式： su [用户名] ，不加用户名默认切换到 root 用户。
    - 示例： su root （切换到 root，需要输入 root 密码）、 su - user1 （切换到 user1 并加载其环境变量）。
4.  sudo ：以 root 权限执行命令
    - 需配置  /etc/sudoers  文件，授权普通用户使用 sudo 权限。
    - 示例： sudo yum install nginx （以 root 权限安装 nginx）。
5.  passwd ：修改用户密码
    - 示例： passwd user1 （修改 user1 的密码）、 passwd （修改当前用户的密码）。
6.  useradd/userdel ：创建/删除用户
    - 示例： useradd -m user1 （创建 user1 并自动创建主目录）、 userdel -r user1 （删除 user1 并删除其主目录）。
7.  groupadd/groupdel ：创建/删除用户组
    - 示例： groupadd testgroup （创建 testgroup 组）。
8.  ps ：查看进程状态
    - 常用选项： -ef （查看所有进程的详细信息）、 -aux （查看进程的资源占用情况）。
    - 示例： ps -ef | grep java （查找所有 java 相关的进程）。
9.  top ：实时监控系统资源和进程状态
    - 常用交互命令： P （按 CPU 使用率排序）、 M （按内存使用率排序）、 k （终止指定进程）、 q （退出）。
10.  kill/killall ：终止进程
    -  kill ：通过进程 ID（PID）终止进程，格式  kill [信号] PID ，常用信号  9 （强制终止）。
    - 示例： kill -9 1234 （强制终止 PID 为 1234 的进程）。
    -  killall ：通过进程名终止所有同名进程，示例： killall nginx （终止所有 nginx 进程）。
11.  df ：查看磁盘空间使用情况
    - 常用选项： -h （人性化显示）、 -T （显示文件系统类型）。
    - 示例： df -h 。
12.  du ：查看文件或目录的磁盘占用空间
    - 常用选项： -h （人性化显示）、 -s （显示总大小）。
    - 示例： du -sh /var/log （查看 /var/log 目录的总大小）。
13.  free ：查看内存和交换分区使用情况
    - 常用选项： -h （人性化显示）。
    - 示例： free -h 。
14.  ifconfig/ip ：查看和配置网络接口
    -  ifconfig ：传统命令，示例： ifconfig eth0 （查看 eth0 网卡信息）。
    -  ip ：新一代命令，功能更强大，示例： ip addr show （查看所有网卡信息）、 ip route show （查看路由表）。
15.  ping ：测试网络连通性
    - 示例： ping www.baidu.com 。
16.  netstat/ss ：查看网络连接状态
    -  netstat -tulnp ：查看所有监听的端口和对应的进程， -t （TCP）、 -u （UDP）、 -l （监听）、 -n （数字显示 IP 和端口）、 -p （显示进程 PID）。
    -  ss ：替代 netstat 的命令，速度更快，示例： ss -tulnp 。
    （三） 压缩与解压命令
    1.  tar ：打包压缩/解压文件，支持多种压缩格式
    - 打包： tar -cvf 打包文件名.tar 源文件/目录 。
    - 打包并压缩（gzip）： tar -zcvf 压缩文件名.tar.gz 源文件/目录 。
    - 打包并压缩（bzip2）： tar -jcvf 压缩文件名.tar.bz2 源文件/目录 。
    - 解压： tar -xvf 压缩文件名 （自动识别压缩格式）、 tar -zxvf xxx.tar.gz （解压 gzip 格式）、 tar -jxvf xxx.tar.bz2 （解压 bzip2 格式）。
    - 选项说明： -c （创建）、 -x （解压）、 -v （显示过程）、 -f （指定文件名）、 -z （gzip 压缩）、 -j （bzip2 压缩）。
    2.  zip/unzip ：压缩/解压 zip 格式文件
    - 压缩： zip -r 压缩文件名.zip 源文件/目录 。
    - 解压： unzip 压缩文件名.zip -d 目标目录 。
    （四） 文本编辑命令
    1.  vim ：功能强大的文本编辑器，是 vi 的增强版，有三种模式：
    - 命令模式：默认模式，可执行光标移动、复制、删除、粘贴等操作，按  i/a/o  进入输入模式，按  :  进入末行模式。
    - 输入模式：用于编辑文本内容，按  Esc  回到命令模式。
    - 末行模式：执行保存、退出、查找替换等操作，常用命令：
    -  :w ：保存文件； :wq ：保存并退出； :q! ：强制退出不保存。
    -  :set nu ：显示行号； :set nonu ：隐藏行号。
    -  :n ：跳转到第 n 行； :%s/old/new/g ：全局替换所有 old 为 new。
    四、 Shell 脚本基础
    1. Shell 脚本的定义
    - Shell 脚本是包含一系列 Linux 命令的文本文件，以  #!/bin/bash  开头（指定解释器为 Bash），赋予执行权限后可直接运行，用于自动化执行重复任务。
    2. Shell 脚本的创建与执行
    - 创建：使用 vim 编写脚本，示例  test.sh ：
    bash  

# /bin/bash
echo "Hello Linux!"
ls -l /home
 
- 赋予执行权限： chmod +x test.sh 。
- 执行方式：
- 相对路径： ./test.sh 。
- 绝对路径： /home/user1/test.sh 。
- 通过 Bash 执行： bash test.sh （无需执行权限）。
    3. Shell 脚本的基本语法
- 变量：定义变量  变量名=值 （等号两边无空格），使用变量  $变量名  或  ${变量名} 。
- 示例：
    bash  
    name="Linux"
    echo "Hello $name"
 
- 参数传递：脚本执行时可传入参数， $1  表示第一个参数， $2  表示第二个参数， $0  表示脚本名， $#  表示参数个数。
- 示例脚本  param.sh ：
    bash  

# /bin/bash
echo "脚本名：$0"
echo "第一个参数：$1"
echo "参数个数：$#"
 
- 执行： ./param.sh hello world 。
- 条件判断： if [ 条件 ]; then ... fi ，条件表达式需用空格包围。
- 示例：
    bash  
    if [ $1 -eq 10 ]; then
    echo "参数等于 10"
    else
    echo "参数不等于 10"
    fi
 
- 循环结构：
-  for  循环：
    bash  
    for i in {1..5}
    do
    echo "循环次数：$i"
    done
 
-  while  循环：
    bash  
    i=1
    while [ $i -le 5 ]
    do
    echo "循环次数：$i"
    i=$((i+1))
    done
 
五、 Linux 服务管理
1. 服务的概念
    - Linux 服务是在后台运行的程序，用于提供特定功能（如 web 服务 nginx、数据库服务 mysql），也称为守护进程（以  d  结尾，如  sshd 、 crond ）。
2. 服务管理命令
    - systemctl：CentOS 7 及以上版本的服务管理工具，替代传统的  service  和  chkconfig  命令。
    - 启动服务： systemctl start 服务名 （如  systemctl start nginx ）。
    - 停止服务： systemctl stop 服务名 。
    - 重启服务： systemctl restart 服务名 。
    - 查看服务状态： systemctl status 服务名 。
    - 设置开机自启： systemctl enable 服务名 。
    - 关闭开机自启： systemctl disable 服务名 。
    - 查看所有已启动的服务： systemctl list-units --type=service 。
3. 计划任务（crontab）
    - 用于定时执行任务，分为系统计划任务（ /etc/crontab ）和用户计划任务（ crontab -e ）。
    - crontab 语法格式： * * * * * 命令 ，五个  *  分别表示 分钟（0-59）、小时（0-23）、日期（1-31）、月份（1-12）、星期（0-7，0 和 7 均为周日）。
    - 特殊符号： * （所有值）、 / （间隔，如  */5  表示每 5 分钟）、 - （范围，如  1-5  表示 1 到 5）、 , （列表，如  1,3,5  表示 1、3、5）。
    - 常用命令：
    -  crontab -e ：编辑当前用户的计划任务。
    -  crontab -l ：查看当前用户的计划任务。
    -  crontab -r ：删除当前用户的所有计划任务。
    - 示例： */5 * * * * /usr/bin/backup.sh （每 5 分钟执行一次 backup.sh 脚本）。
    六、 Linux 软件安装方式
    1. 源码编译安装
    - 步骤：下载源码包 → 解压 → 配置（ ./configure --prefix=安装路径 ）→ 编译（ make ）→ 安装（ make install ）。
    - 优点：可自定义安装路径和功能模块，适配性强；缺点：编译时间长，依赖手动解决。
    2. RPM 包安装
    - RPM 是 Red Hat 系列的软件包格式（ .rpm ），适用于 CentOS、RHEL 等系统。
    - 命令： rpm -ivh 软件包.rpm （安装）、 rpm -e 软件名 （卸载）、 rpm -ql 软件名 （查看软件安装的文件）。
    - 缺点：需手动解决依赖关系。
    3. YUM 安装
    - YUM 是基于 RPM 的包管理器，自动解决依赖关系，适用于 Red Hat 系列系统。
    - 命令： yum install 软件名 -y （安装）、 yum remove 软件名 -y （卸载）、 yum list 软件名 （查看软件是否安装）、 yum update 软件名 -y （更新软件）。
4. APT 安装
    - APT 是 Debian 系列的包管理器，适用于 Ubuntu、Debian 等系统，自动解决依赖关系。
    - 命令： apt-get install 软件名 -y （安装）、 apt-get remove 软件名 -y （卸载）、 apt-get update （更新软件源）、 apt-get upgrade -y （更新系统软件）。
    七、 Linux 系统安全
    1. 用户权限控制
    - 遵循最小权限原则，普通用户仅授予必要权限，避免使用 root 用户直接操作。
    - 定期检查  /etc/sudoers  文件，防止未授权用户获取 sudo 权限。
    2. 防火墙配置
    - firewalld：CentOS 7 及以上默认防火墙，通过  firewall-cmd  命令管理。
    - 常用命令： firewall-cmd --zone=public --add-port=80/tcp --permanent （永久开放 80 端口）、 firewall-cmd --reload （重新加载配置）、 firewall-cmd --list-ports （查看开放的端口）。
    - iptables：传统防火墙，基于规则链过滤数据包，适用于所有 Linux 发行版。
    3. 禁用不必要的服务
    - 关闭不需要的服务（如  telnet 、 ftp ），减少攻击面，命令： systemctl disable 服务名 && systemctl stop 服务名 。
    4. 定期更新系统
    - 使用  yum update  或  apt-get upgrade  定期更新系统补丁，修复安全漏洞。
5. 日志监控
    - 定期查看系统日志（ /var/log/messages ）、安全日志（ /var/log/secure ），及时发现异常登录和操作。
