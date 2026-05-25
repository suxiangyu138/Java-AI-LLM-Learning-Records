# 快速学会 Shell 脚本
定位：**Linux 标配、自动化运维、后端部署必用**，

零基础1小时上手，只学工作刚需内容。
Shell = Linux 命令 + 脚本编程，用来批量执行命令、自动化部署、定时任务。

---

## 一、基础认知
1. 常见 Shell 类型
    - `bash`：绝大多数 Linux 默认（重点学这个）
    - `sh`：老旧兼容版本
2. 脚本后缀：`.sh`
3. 执行方式：赋予权限运行 / 直接解释器运行

---

## 二、第一个 Shell 脚本

### 1. 创建 `test.sh`
```bash
vim test.sh
```
写入内容：
```sh
#!/bin/bash
echo "Hello Shell"
```
- `#!/bin/bash`：**声明脚本解释器**，必须写在第一行

### 2. 执行脚本两种方式
```bash
# 方式1：直接指定解释器（不用加权限）
bash test.sh

# 方式2：加执行权限后运行（标准写法）
chmod +x test.sh
./test.sh
```

---

## 三、Shell 核心语法（必掌握）

### 1. 变量
```sh
# 定义变量：等号两边不能有空格
name="demo"
num=10

# 使用变量
echo $name
echo ${num}

# 只读变量
readonly num

# 撤销变量
unset name
```

### 2. 字符串
```sh
str1=hello
str2="shell test"
# 拼接
str3=$str1$str2
echo $str3
```

### 3. 数组
```sh
arr=(1 2 3 4 5)
# 取单个元素
echo ${arr[0]}
# 取全部元素
echo ${arr[@]}
# 数组长度
echo ${#arr[@]}
```

---

## 四、运算符

### 1. 算术运算
```sh
a=10
b=20
# 整数运算
c=$((a + b))
echo $c
```

### 2. 关系判断（数字）
- `-eq` 等于
- `-ne` 不等于
- `-gt` 大于
- `-lt` 小于
- `-ge` 大于等于
- `-le` 小于等于

### 3. 字符串判断
- `=` 相等
- `!=` 不等
- `-z` 字符串为空
- `-n` 字符串非空

### 4. 文件判断（高频）
- `-f` 是否为普通文件
- `-d` 是否为目录
- `-e` 文件/目录是否存在
- `-r` 可读
- `-w` 可写
- `-x` 可执行

---

## 五、流程控制（if / for / while）

### 1. if 判断
```sh
#!/bin/bash
a=10
b=20

if [ $a -gt $b ]
then
    echo "a更大"
else
    echo "b更大"
fi
```
⚠️ 注意：`[ ]` 左右**必须加空格**，语法硬性要求。

### 2. 多分支 if-elif
```sh
if [ $a -eq $b ]
then
    echo "相等"
elif [ $a -gt $b ]
then
    echo "a大"
else
    echo "b大"
fi
```

### 3. for 循环
```sh
# 写法1 遍历元素
for i in 1 2 3 4
do
  echo "数字：$i"
done

# 写法2 范围循环
for ((i=1; i<=5; i++))
do
  echo $i
done
```

### 4. while 循环
```sh
i=1
while [ $i -le 3 ]
do
  echo $i
  i=$((i+1))
done
```

---

## 六、函数
```sh
# 定义函数
demo(){
    echo "执行自定义函数"
}

# 调用函数
demo
```

带参数函数：
```sh
add(){
  res=$(( $1 + $2 ))
  echo $res
}

add 10 20
```
- `$1` 第一个参数、`$2` 第二个参数
- `$0` 脚本本身名称
- `$#` 参数个数

---

## 七、脚本传参
新建 `param.sh`
```sh
#!/bin/bash
echo "脚本名：$0"
echo "参数1：$1"
echo "参数2：$2"
```
执行：
```bash
bash param.sh 100 200
```

---

## 八、高频实用案例（工作直接用）

### 案例1：判断文件是否存在
```sh
#!/bin/bash
file="/etc/profile"
if [ -e $file ]
then
  echo "文件存在"
else
  echo "文件不存在"
fi
```

### 案例2：批量解压当前目录所有tar.gz
```sh
for file in *.tar.gz
do
  tar -zxvf $file
done
```

### 案例3：日志定时清理
```sh
#!/bin/bash
rm -rf /tmp/*.log
echo "日志清理完成"
```

---

## 九、配合 Linux + Docker 实战价值
1. 自动化打包、重启 Java 项目
2. 批量操作服务器文件
3. 定时任务 `crontab` + Shell 实现定时备份
4. 批量启停、管理 Docker 容器

---

## 十、最简学习路线
1. 熟记：变量、if、for、文件判断、函数
2. 仿写3个脚本：**文件判断、循环批量操作、项目重启脚本**
3. 结合 crontab 实现定时自动化

---
