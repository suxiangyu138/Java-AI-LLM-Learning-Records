# 快速学会 Bash
Bash = Linux 默认 Shell，**终端命令解析器 + 脚本编程语言**
你之前学的 Linux 命令、Shell 脚本，本质全是 **Bash**。

---

# 一、Bash 核心基础
## 1. 标识
脚本首行必须写（解释器声明）
```bash
#!/bin/bash
```

## 2. 脚本运行 3 种方式
```bash
# 1. 直接调用解释器（无需权限）
bash test.sh

# 2. 加执行权限运行（标准规范）
chmod +x test.sh
./test.sh

# 3. 当前终端执行（继承环境变量）
source test.sh
. test.sh
```

## 3. 注释
```bash
# 单行注释

: '
多行注释
多行注释
'
```

---

# 二、变量（最基础）
## 1. 自定义变量
**等号两边绝对不能有空格**
```bash
name="bash学习"
num=666

# 使用变量
echo $name
echo ${num}

# 只读变量
readonly num

# 删除变量
unset name
```

## 2. 系统内置变量（高频）
```bash
$0   # 脚本名称
$1 $2 # 第1、第2个传入参数
$#   # 参数总个数
$@   # 所有参数
$?   # 上一条命令执行返回值(0=成功，非0=失败)
$$   # 当前进程PID
```

## 3. 命令赋值变量
```bash
now=$(date)
echo $now

# 反引号等价
ip=`hostname -I`
```

---

# 三、三大运算
## 1. 整数运算
```bash
a=10
b=3

res=$((a + b))
echo $res

# 支持 + - * / %
$((a * b))
```

## 2. 小数运算（bc）
```bash
echo "3.14 * 2" | bc
```

## 3. 字符串拼接
```bash
s1="hello"
s2="bash"
s3="$s1 $s2"
echo $s3
```

---

# 四、条件判断（必考必用）
## 1. 语法硬性规则
`[ 条件 ]` → **中括号左右必须有空格**
```bash
if [ 条件 ]; then
  指令
fi
```

## 2. 常用判断参数
### 数字判断
```bash
-eq  等于
-ne  不等于
-gt  大于
-lt  小于
-ge  大于等于
-le  小于等于
```

### 字符串判断
```bash
[ "$a" = "$b" ]    # 相等
[ "$a" != "$b" ]   # 不等
[ -z "$str" ]      # 字符串为空
[ -n "$str" ]      # 字符串非空
```

### 文件判断（后端高频）
```bash
[ -f file ]  # 是否为普通文件
[ -d dir ]   # 是否为目录
[ -e path ]  # 文件/目录是否存在
[ -x file ]  # 是否可执行
```

## 3. 完整 if 示例
```bash
#!/bin/bash
file="/etc/hosts"

if [ -e "$file" ]
then
    echo "文件存在"
else
    echo "文件不存在"
fi
```

## 4. 多分支 elif
```bash
score=85
if [ $score -ge 90 ]; then
  echo "优秀"
elif [ $score -ge 60 ]; then
  echo "及格"
else
  echo "不及格"
fi
```

---

# 五、循环（自动化核心）
## 1. for 循环
```bash
# 遍历集合
for i in 1 2 3 4
do
  echo "数字：$i"
done

# 范围循环 C风格
for ((i=1; i<=5; i++))
do
  echo $i
done

# 遍历目录文件
for f in *.log
do
  echo "日志文件：$f"
done
```

## 2. while 循环
```bash
i=1
while [ $i -le 3 ]
do
  echo $i
  i=$((i+1))
done
```

## 3. 循环控制
```bash
break    # 跳出循环
continue # 跳过本次，进入下一次
```

---

# 六、函数
```bash
# 定义
func() {
  echo "执行Bash函数"
  echo "接收参数1：$1"
}

# 调用
func 123
```

## 函数返回值
```bash
add() {
  return $(( $1 + $2 ))
}
add 2 3
echo $?  # 获取返回值
```

---

# 七、管道 & 重定向（Bash 灵魂）
## 1. 管道 `|`
上一条输出 作为 下一条输入
```bash
ps -ef | grep java
ls | grep sh
```

## 2. 重定向
```bash
# 覆盖写入
echo "test" > a.txt

# 追加写入
echo "追加内容" >> a.txt

# 错误日志重定向
java -jar app.jar > run.log 2>&1
```

---

# 八、正则&文本三剑客（极简）
```bash
# 过滤关键字
grep "error" app.log

# 替换文本
sed 's/旧内容/新内容/g' test.txt

# 列截取
awk '{print $1}' info.txt
```

---

# 九、后端常用 Bash 实战脚本（直接复制）
## 1. 检测服务是否运行
```bash
#!/bin/bash
pid=$(ps -ef | grep java | grep -v grep | awk '{print $2}')
if [ -n "$pid" ];then
  echo "Java 进程运行中，PID：$pid"
else
  echo "Java 未启动"
fi
```

## 2. 批量清理日志
```bash
#!/bin/bash
rm -rf /tmp/*.log
rm -rf ./logs/*.out
echo "日志清理完成"
```

## 3. 简单备份
```bash
tar -zcvf backup_$(date +%Y%m%d).tar.gz /data
```

---

# 十、Bash 必会高频命令缩写
```bash
&&   # 前一条成功才执行后一条
||   # 前一条失败才执行后一条
;    # 多条命令串行执行
&    # 后台运行
```

---

# 十一、极简学习路线（最快吃透）
1. 掌握：**变量 + if 判断 + for/while 循环**
2. 熟记：**文件判断、脚本传参、函数**
3. 吃透：**管道 | 、重定向 > >> 2>&1**
4. 实战：写 3 个脚本 → 项目启动、日志清理、文件备份
5. 结合 `crontab` 定时任务，实现自动化

---