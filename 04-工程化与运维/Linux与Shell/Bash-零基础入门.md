Bash 零基础快速入门
Bash（Bourne-Again Shell）是Linux/Unix系统默认的命令行解释器，也是最常用的Shell，以下是新手友好的核心知识点+实操指令，适配快速上手和日常使用。
一、基础操作（必学）
1. 核心导航命令（文件/目录操作）
    bash
    pwd          # 查看当前所在目录（打印工作路径）
    ls           # 列出当前目录文件/文件夹
    ls -l        # 详细列表（含权限、大小、时间，简写ll）
    ls -a        # 显示隐藏文件（以.开头的文件）
    cd 目录名    # 进入指定目录（cd .. 回到上一级，cd ~ 回到家目录，cd / 回到根目录）
    mkdir 文件夹名# 创建文件夹（mkdir -p a/b/c 递归创建多级文件夹）
    touch 文件名 # 创建空文件（如touch test.sh）
    rm 文件名    # 删除文件（rm -f 强制删除，不提示）
    rm -r 文件夹 # 删除文件夹（递归删除）
    mv 原名称 新名称 # 重命名/移动（如mv test.sh /home/）
    cp 原文件 目标路径 # 复制文件（cp -r 复制文件夹）
 
2. 文本查看/编辑（最常用）
    bash
    cat 文件名    # 一次性查看文件全部内容
    more 文件名   # 分页查看（按空格下一页，q退出）
    less 文件名   # 高级分页（支持上下键滚动，q退出）
    head -n 文件名 # 查看前n行（默认前10行，head 文件名）
    tail -n 文件名 # 查看后n行（tail -f 文件名 实时监控文件更新，日志常用）
    vim 文件名    # 文本编辑器（新手入门：按i进入编辑模式，编辑完按Esc，输入:wq保存退出，:q!强制退出不保存）
 
3. 权限修改（关键）
    Linux文件权限分所有者（u）、组（g）、其他（o），权限类型：读（r=4）、写（w=2）、执行（x=1）
    bash
    chmod 755 文件名 # 最常用：所有者rwx(4+2+1)，组和其他rx(4+1)
    chmod +x 文件名  # 给所有用户添加执行权限（脚本运行必加）
    chown 用户名:组名 文件名 # 修改文件所有者（如chown root:root test.sh）
 
二、Bash 脚本基础（从0写第一个脚本）
1. 脚本规范
    - 脚本后缀为 .sh 
    - 第一行必须写解释器声明： #!/bin/bash （告诉系统用Bash执行）
    - 脚本需要执行权限（ chmod +x 脚本名.sh ）
2. 第一个Bash脚本（hello.sh）
    步骤1：创建脚本
    bash
    touch hello.sh
    vim hello.sh
 
步骤2：写入内容（按i进入编辑模式）
bash

# /bin/bash

# 这是注释（#开头为注释，不执行）
echo "Hello Bash!"  # echo 用于输出内容到控制台
 
步骤3：保存退出（Esc → :wq）
步骤4：执行脚本（两种方式）
bash
./hello.sh  # 方式1：当前目录执行（推荐，需执行权限）
bash hello.sh # 方式2：直接用bash解释器执行（无需执行权限）
 
执行结果：控制台输出  Hello Bash! 
3. 脚本核心语法（变量/参数/条件）
    （1）变量定义与使用
    - 定义：无空格， 变量名=值 （如 name="Bash" ，值有空格必须加引号）
    - 使用： $变量名  或  ${变量名} （花括号避免歧义，推荐）
    - 只读变量： readonly 变量名 （定义后不能修改/删除）
    - 取消变量： unset 变量名 （只读变量无法取消）
    bash

# /bin/bash
name="Java后端学习"
echo "我的学习内容：$name"
echo "用花括号：${name}_笔记"
 
（2）接收命令行参数
脚本运行时可传参， $1 表示第一个参数， $2 第二个，依此类推， $0 表示脚本本身名称
bash

# /bin/bash
echo "脚本名：$0"
echo "第一个参数：$1"
echo "第二个参数：$2"
echo "所有参数：$*"  # 所有参数作为一个整体
echo "参数个数：$#"  # 统计参数个数
 
执行： ./test.sh 100 200 ，输出：
plaintext
脚本名：./test.sh
第一个参数：100
第二个参数：200
所有参数：100 200
参数个数：2
 
（3）条件判断（if-else）
语法格式（注意空格：[ 前后必须有空格，条件和符号之间也有空格 ]）
bash

# /bin/bash
a=10
b=20
if [ $a -lt $b ]; then  # -lt 表示小于（less than）
    echo "$a 小于 $b"
elif [ $a -eq $b ]; then # -eq 表示等于（equal）
    echo "$a 等于 $b"
else
    echo "$a 大于 $b"
fi # if结束标志
 
常用判断符：
- 数值比较：-eq（等于）、-ne（不等于）、-lt（小于）、-gt（大于）、-le（小于等于）、-ge（大于等于）
- 字符串比较：==（相等）、!=（不相等）、-z（空字符串）
- 文件判断：-f（是否为文件）、-d（是否为目录）、-x（是否有执行权限）
    （4）循环（for/while）
- for循环（遍历列表/参数）
    bash

# /bin/bash

# 遍历1-5
for i in {1..5}; do
    echo "数字：$i"
done

# 遍历命令行参数
for arg in $*; do
    echo "参数：$arg"
done
 
- while循环（条件满足时执行）
    bash

# /bin/bash
i=1
while [ $i -le 3 ]; do
    echo "循环次数：$i"
    i=$((i+1)) # 数值运算，双括号内直接写表达式
done
 
三、实用进阶技巧（日常高频）
1. 管道与重定向（核心）
    - 管道： |  将前一个命令的输出，作为后一个命令的输入（串联命令）
    bash
    ls -l | grep .sh  # 列出当前目录所有.sh后缀的文件（grep是过滤命令）
    cat test.log | head -20 # 查看test.log前20行
 
- 重定向：
 >  覆盖写入（将命令输出写入文件，覆盖原有内容）
 >>  追加写入（将命令输出追加到文件末尾，不覆盖）
 <  从文件读取输入
bash
echo "测试内容" > test.txt  # 覆盖写入test.txt
echo "追加内容" >> test.txt # 追加到test.txt
cat < test.txt  # 从test.txt读取内容并显示
 
2. 查找命令（find/grep）
    bash

# find 按文件/目录名查找（全系统搜索，-name按名称，-type f找文件，-type d找目录）
find / -name "test.sh"  # 从根目录查找test.sh文件
find /home -type f -name "*.log" # 从/home查找所有.log文件

# grep 按内容过滤（-i忽略大小写，-n显示行号，-r递归查找目录内文件内容）
grep "error" test.log   # 在test.log中查找包含error的行
grep -rn "Java" /home   # 在/home目录下递归查找所有包含Java的内容，显示行号
 
3. 后台执行命令（&/nohup）
    bash

# & 后台执行，关闭终端后命令停止
./test.sh &

# nohup 后台执行，关闭终端仍继续运行，输出默认写入nohup.out
nohup ./test.sh &

# 自定义输出文件
nohup ./test.sh > mylog.log 2>&1 &

# 2>&1 表示将错误输出（2）重定向到标准输出（1），一起写入日志
 
4. 查看进程与杀死进程
    bash
    ps -ef # 查看所有进程（ps -ef | grep 进程名 过滤指定进程，如ps -ef | grep java）
    ps aux # 另一种查看进程的方式，显示更详细的资源占用
    kill -9 进程ID # 强制杀死进程（如kill -9 12345，12345是ps查到的PID）
 
四、新手避坑指南
1. 空格问题：Bash对空格敏感，变量定义 a=10 （无空格），条件判断 [ $a -lt $b ] （前后必须有空格）
2. 路径问题：执行脚本时，当前目录必须加 ./ （如 ./hello.sh ），否则系统会在系统路径中查找，导致找不到
3. 权限问题：运行脚本提示 Permission denied ，直接加执行权限 chmod +x 脚本名.sh 
4. 中文乱码：Linux终端中文乱码，执行 export LANG=zh_CN.UTF-8 临时解决，永久解决需修改系统配置
5. vim操作：新手别忘按 Esc 退出编辑模式，否则无法输入保存命令，强制退出 :q! 
    五、高频实用命令（日常工作/学习必记）
    bash
    df -h        # 查看磁盘空间使用情况（-h人性化显示，单位G/M）
    free -h      # 查看内存/交换分区使用情况
    top          # 实时监控系统进程（按q退出，类似Windows任务管理器）
    netstat -tuln# 查看系统开放的端口（-tTCP，-uUDP，-l监听，-n数字显示）
    ss -tuln     # 替代netstat，更高效的端口查看命令
    tar -zcvf 压缩包名.tar.gz 要压缩的文件/文件夹 # 压缩（gzip）
    tar -zxvf 压缩包名.tar.gz -C 解压目标路径      # 解压（gzip，-C指定路径）
    unzip 压缩包名.zip -d 解压目标路径            # 解压zip文件
    ping 域名/IP # 测试网络连通性（如ping baidu.com，Ctrl+C停止）
    curl 网址    # 访问网址并显示内容（如curl https://www.baidu.com）
