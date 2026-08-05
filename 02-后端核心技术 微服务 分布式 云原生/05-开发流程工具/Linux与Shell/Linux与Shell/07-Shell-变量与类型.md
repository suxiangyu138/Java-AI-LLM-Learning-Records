# Shell 变量与类型

## 1. 变量声明与使用

Shell 变量默认是**字符串类型**，声明时等号两侧**不能有空格**。

```bash
# 声明与赋值
NAME="java-app"
version="1.0.0"
_count=10

# 使用变量（推荐用花括号包裹）
echo "${NAME}"
echo "${NAME}-${version}"

# 只读变量
readonly BASE_DIR="/opt/app"

# 删除变量
unset _count
```

## 2. 变量类型

| 类型 | 说明 | 示例 |
|------|------|------|
| 字符串 | 默认类型 | `str="hello"` |
| 整数 | 需 `declare -i` 声明 | `declare -i num=10` |
| 只读 | 不可修改 | `readonly var` |
| 环境变量 | 子进程继承 | `export PATH` |
| 数组 | 索引/关联 | `arr=(a b c)` |

```bash
# 整数运算（推荐 $(( )) ）
declare -i total=0
total=$(( 10 + 20 * 3 ))
echo "${total}"

# 字符串数字需用算术上下文
PORT=8080
echo $(( PORT + 1 ))   # 8081
```

## 3. 引号使用

| 引号 | 作用 | 示例输出 |
|------|------|----------|
| 单引号 `'` | 完全字面量，不解析任何内容 | `'$HOME'` -> `$HOME` |
| 双引号 `"` | 解析变量和命令替换 | `"$HOME"` -> `/root` |
| 反引号 `` ` `` | 旧式命令替换（已不推荐） | `` `date` `` |
| `$()` | 新式命令替换（推荐） | `$(date)` |

```bash
APP="my-api"
echo '${APP}'   # 输出: ${APP}
echo "${APP}"   # 输出: my-api
echo "today is $(date +%F)"  # 输出: today is 2026-06-13
```

## 4. 特殊变量

| 变量 | 含义 |
|------|------|
| `$0` | 脚本文件名 |
| `$1`-`$9` | 位置参数（第1-9个） |
| `${10}` | 第10个及以后位置参数必须用花括号 |
| `$#` | 参数个数 |
| `$@` | 所有参数（每个独立成词） |
| `$*` | 所有参数（合并为一个词） |
| `$?` | 上一条命令退出码（0 成功，非0 失败） |
| `$$` | 当前 Shell PID |
| `$!` | 最后一个后台进程 PID |

```bash
#!/bin/bash
# args.sh
echo "脚本名: $0"
echo "参数个数: $#"
echo "所有参数: $@"
for arg in "$@"; do
  echo "  - ${arg}"
done
echo "退出码: $?"

# 后台进程示例
sleep 5 &
echo "后台PID: $!"
```

## 5. 环境变量 vs 局部变量

```bash
# 局部变量（当前 Shell 生效）
LOCAL_VAR="only_me"
echo "${LOCAL_VAR}"

# 环境变量（子进程继承）
export JAVA_HOME="/usr/lib/jvm/java-17"
./gradlew build   # 子进程可读取 JAVA_HOME

# 一次性导出执行
PATH="/custom/bin:${PATH}" java -jar app.jar

# 查看环境变量
env | grep JAVA
echo "${HOME} ${USER} ${SHELL} ${PWD}"
```

**Java backend 常用环境变量：**

```bash
export JAVA_HOME="/usr/lib/jvm/java-17"
export MAVEN_HOME="/opt/maven"
export PATH="${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${PATH}"
```

## 6. 字符串操作

```bash
str="hello-java-world"

echo "${#str}"                  # 长度: 17
echo "${str:6:4}"               # 子串: java
echo "${str/java/spring}"       # 替换第一个: hello-spring-world
echo "${str//a/@}"              # 替换全部: hello-j@v@-world
echo "${str#*-}"                # 去前缀(最短): java-world
echo "${str##*-}"               # 去前缀(最长): world
echo "${str%-*}"                # 去后缀(最短): hello-java
echo "${str%%-*}"               # 去后缀(最长): hello
echo "${str^^}"                 # 全大写: HELLO-JAVA-WORLD
echo "${str,,}"                 # 全小写: hello-java-world

# 默认值
echo "${UNDEFINED:-default}"    # 变量未定义用 default
echo "${UNDEFINED:=default}"    # 未定义则赋值后再输出
echo "${VAR:?error message}"    # 未定义则报错退出
```

## 7. 数组操作

### 7.1 索引数组

```bash
# 声明
SERVERS=("api" "web" "db")
SERVERS[3]="cache"

# 读取
echo "${SERVERS[0]}"           # api
echo "${SERVERS[@]}"           # api web db cache
echo "${#SERVERS[@]}"          # 长度: 4
echo "${!SERVERS[@]}"          # 索引: 0 1 2 3

# 遍历
for s in "${SERVERS[@]}"; do
  echo "Deploy to ${s}"
done

# 切片
echo "${SERVERS[@]:1:2}"       # web db

# 合并
ALL=("${SERVERS[@]}" "monitor")
```

### 7.2 关联数组（需 Bash 4+）

```bash
declare -A PORTS

PORTS=([api]=8080 [web]=80 [db]=3306)
PORTS[cache]=6379

echo "${PORTS[api]}"           # 8080
echo "${!PORTS[@]}"            # api web db cache
echo "${#PORTS[@]}"            # 4

for key in "${!PORTS[@]}"; do
  echo "${key} -> ${PORTS[${key}]}"
done
```

### 7.3 Java 部署实用示例

```bash
#!/bin/bash
# deploy.sh - 多实例启动辅助脚本

JAR_DIR="/opt/app"
INSTANCES=("gateway" "user" "order")
JAVA_OPTS="-Xms256m -Xmx512m"

for i in "${!INSTANCES[@]}"; do
  app="${INSTANCES[${i}]}"
  pid_file="/var/run/${app}.pid"
  echo "Starting ${app} with PID file ${pid_file} ..."
  nohup java ${JAVA_OPTS} -jar "${JAR_DIR}/${app}.jar" > "/var/log/${app}.log" 2>&1 &
  echo $! > "${pid_file}"
done
```

## 8. FAQ / 常见问题

| 问题 | 原因与解决 |
|------|-----------|
| `变量赋值报错 "command not found"` | 等号两侧有空格。必须写成 `NAME=value` |
| `$var` 和 `"$var"` 有什么区别 | 前者可能被分词(pathname expansion)，后者保留原值。**始终用双引号包裹变量** |
| 如何判断变量是否设置 | `if [ -z "${VAR+set}" ]; then echo "未设置"; fi` |
| 数组长度为 1 而不是期望值 | 元素中包含空格时未用引号：用 `("a b" c)` 而非 `(a b c)` |
| `declare -A` 报错 | 关联数组需要 Bash 4.0+，用 `bash --version` 检查 |
| `${11}` 取不到第11个参数 | 花括号不能省略：必须是 `${11}` |
| `$@` 和 `$*` 差别在哪 | `"$@"` 展开为独立参数，`"$*"` 展开为单个字符串。**始终用 `"$@"`** |
| 为什么 `$?` 在命令后总是 0 | 可能中间插入了 `echo` 或其他命令。`$?` 是上一条命令的退出码，需立即使用 |
