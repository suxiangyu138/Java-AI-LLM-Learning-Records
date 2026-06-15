# Shell 文本处理

> 面向 Java 后端开发者的 Shell 文本处理速查。覆盖 grep、sed、awk、cut、sort、uniq、wc 等核心命令，正则表达式在 Shell 中的使用，以及常用文本处理流水线。

---

## 1. 正则表达式：BRE 与 ERE

Shell 中正则分为两种流派，不同命令默认使用的流派不同，是新手最容易踩坑的地方。

| 特性 | BRE（基本正则） | ERE（扩展正则） |
|------|----------------|----------------|
| 默认命令 | `grep` `sed` | `grep -E` `awk` `sed -E` |
| 量词 `+` `?` `{n,m}` `\|` | 需转义：`\+` `\?` `\{n,m}` `\|` | 直接使用 |
| 分组 `()` | 需转义：`\(` `\)` | 直接使用 `()` |

```bash
echo "hello world" | grep "wor\+d"        # BRE: \+ 表示一次或多次
echo "hello world" | grep -E "wor+ d?"    # ERE: + ? 直接使用
```

---

## 2. grep -- 文本搜索

### 常用参数

| 参数 | 作用 | 场景 |
|------|------|------|
| `-n` | 显示行号 | 定位日志行 |
| `-i` | 忽略大小写 | 搜索 ERROR/error |
| `-v` | 反向匹配 | 排除 DEBUG 行 |
| `-E` | 扩展正则 | 多关键字 `-E "ERR|WARN"` |
| `-o` | 仅输出匹配部分 | 提取 IP、URL |
| `-c` | 统计匹配行数 | 计数 |
| `-r` | 递归搜索目录 | 扫描整个日志目录 |
| `-A/-B/-C n` | 显示匹配行后/前/前后 n 行 | 日志上下文 |

### 实战示例

```bash
# 筛选日志中的空指针异常
grep -n "NullPointerException" app.log

# 排除 DEBUG，保留 ERROR 和 WARN
grep -v "DEBUG" app.log | grep -E "ERROR|WARN"

# 提取所有接口路径（日志: [2026-06-13] GET /api/order/list）
grep -oP "GET \K/api/\S+" app.log      # -P = Perl 正则（更强大）
grep -o "/api/[^ ]*" app.log

# 统计异常出现次数
grep -c "Exception" app.log

# 搜索并显示前后 3 行上下文
grep -C 3 "OutOfMemoryError" app.log
```

> **提示**：`grep -P`（Perl 兼容正则）在 GNU grep 中可用，支持 `\K`（丢弃匹配之前的内容）、零宽断言等。

---

## 3. sed -- 流编辑器

### 常用命令

| 命令 | 作用 |
|------|------|
| `s/old/new/` | 替换第一个匹配 |
| `s/old/new/g` | 全局替换所有匹配 |
| `s/old/new/2` | 替换第 2 个匹配 |
| `/pattern/d` | 删除匹配行 |
| `/pattern/a\text` | 在匹配行后追加 |
| `/pattern/i\text` | 在匹配行前插入 |
| `-i` | 原地修改文件（直接改文件，不输出到 stdout） |
| `-E` | 扩展正则模式 |

### 实战示例

```bash
# 替换 application.yml 中数据库密码
sed -i 's/password: old_pass/password: new_pass/' application.yml

# 删除所有注释行和空行
sed -i '/^#/d; /^$/d' application.properties

# 在 # DataSource 后面插入一行配置
sed -i '/^# DataSource/a\spring.datasource.url=jdbc:mysql://localhost:3306/db' application.yml

# 提取 Tomcat 访问日志中的状态码列（固定位置模式）
sed -n 's/.* \([0-9]\{3\}\) [0-9.]*$/\1/p' access_log
# -n: 不自动打印  p: 打印替换结果
```

### 多模式批量替换

```bash
# 一次性替换多个关键字
sed -i 's/192.168.1.100/10.0.0.1/g; s/8080/443/g' nginx.conf
```

---

## 4. awk -- 字段处理与计算

### 基本语法

```bash
awk 'pattern { action }' file
```

- `$0` 整行, `$1` `$2` ... 各字段（默认按空白分割）
- `-F` 指定分隔符，如 `-F':'` 或 `-F ','`

### 内置变量

| 变量 | 含义 |
|------|------|
| `NR` | 当前行号 |
| `NF` | 当前行字段数 |
| `$NF` | 最后一个字段 |
| `FS` | 输入分隔符（默认空白） |
| `OFS` | 输出分隔符（默认空格） |

### 实战示例

```bash
# 打印 /etc/passwd 用户名和 UID
awk -F':' '{ print $1, $3 }' /etc/passwd

# 条件过滤：打印 UID >= 1000 的普通用户
awk -F':' '$3 >= 1000 { print $1 }' /etc/passwd

# 统计 Nginx 日志中各 IP 访问次数
awk '{ ips[$1]++ } END { for (ip in ips) print ip, ips[ip] }' access.log | sort -nr -k2

# 计算 HTTP 响应时间的平均值和最大值
awk '{ sum+=$NF; if ($NF>max) max=$NF } END { print "avg:", sum/NR, "max:", max }' response_times.log

# 格式化输出：printf
awk -F',' '{ printf "| %-20s | %5d |\n", $1, $3 }' users.csv
```

---

## 5. cut / sort / uniq / wc

### cut -- 按列切割

```bash
# 提取 application.properties 中的键名（排除注释）
grep -v '^#' application.properties | cut -d'=' -f1

# 提取 CSV 中第 2、4 列
cut -d',' -f2,4 users.csv

# 按字符位置提取（前 10 个字符）
cut -c1-10 fixed_width.log
```

### sort -- 排序

```bash
# 按数字降序排序（-n 数字, -r 倒序）
sort -nr -k2 response_times.log

# 按逗号分隔的第 3 字段排序
sort -t',' -k3 -n users.csv
```

### uniq -- 去重与统计

```bash
# 统计出现次数（必须先 sort）
grep -oP 'status=\K\d+' access.log | sort | uniq -c | sort -nr

# 只显示重复行
grep "ERROR" app.log | sort | uniq -d

# 只显示不重复行
grep "ERROR" app.log | sort | uniq -u
```

### wc -- 行/词/字符计数

```bash
wc -l app.log                        # 总行数
grep "ERROR" app.log | wc -l         # 统计 ERROR 行数
grep -c "ERROR" app.log              # 同上，更高效
```

---

## 6. 文本处理流水线（pipeline）

Shell 的威力在于用 `|` 串联命令，组合出强大的数据处理流程。

```bash
# 统计 Nginx 访问日志中 TOP 10 IP
awk '{print $1}' access.log | sort | uniq -c | sort -nr | head -10

# 统计各 API 接口被调用的次数和平均响应时间
grep -oP '"(GET|POST) \K/api/[^"]*"' access.log | sort | uniq -c | sort -nr

# 过滤慢查询（>1s 的 SQL），提取时间与 SQL 摘要
grep "SLOW_QUERY" mysql-slow.log | awk -F'[: ]' '{print $3, $5, $NF}' | sort -k3 -rn

# 从 JVM GC 日志提取暂停时间
grep "GC pause" gc.log | grep -oP 'pause=\K[0-9.]+' | awk '{s+=$1} END {print "total GC pause:", s, "ms"}'
```

---

## 7. 文件重定向与管道

| 符号 | 含义 | 示例 |
|------|------|------|
| `>` | 覆盖写入 | `grep ERROR app.log > errors.txt` |
| `>>` | 追加写入 | `echo "new entry" >> config.txt` |
| `<` | 输入重定向 | `sort < unsorted.txt` |
| `2>` | 重定向 stderr | `java -jar app.jar 2> error.log` |
| `&>` | 重定向 stdout+stderr | `cmd &> output.log` |
| `|` | 管道（前 stdout 接到后 stdin） | `ps aux | grep java` |
| `tee` | 分流：写到文件 + 打印到终端 | `cmd | tee log.txt` |

```bash
# 同时保留 stdout 和 stderr 到不同文件
java -jar app.jar > stdout.log 2> stderr.log

# 丢弃所有输出
java -jar app.jar &> /dev/null

# 标准错误并入标准输出
grep -r "Exception" /var/log/ 2>&1 | tee all_errors.log
```

---

## 8. FAQ / 常见问题

### Q1: `grep` 搜不到内容，但确认文件中存在？

- **原因**：正则流派不对。`grep` 默认 BRE，`+` `?` `|` `()` 需转义。改用 `grep -E` 或 `grep -P`。
- 检查文件编码（`file file.txt`），中文日志可能是 GBK，而终端是 UTF-8。

### Q2: `sed -i` 报错 "sed: 1: ... undefined label"？

- macOS 的 sed 与 Linux 有差异。macOS 需传空备份后缀：`sed -i '' 's/a/b/g' file`。Linux 中 `-i` 无需参数。

### Q3: `awk` 中 `$NF` 取不到最后一个字段？

- 确认分隔符设置正确。`-F','` 看字段数 `{print NF}`。如果字段数始终为 1，说明分隔符不匹配。

### Q4: 管道命令中间出错，如何排查？

- 使用 `| tee debug.txt` 在管道中间截留数据。例如 `grep ERROR app.log | tee step1.txt | awk '{...}'`。
- 或者在管道节点后加上 `| head` 分段观察。

### Q5: `sort | uniq` 和 `sort -u` 有什么区别？

- 结果相同。`uniq` 有额外参数 `-c`（计数）`-d`（仅重复行），需独立使用时用 `uniq`，仅去重可用 `sort -u`。

### Q6: 大文件处理太慢？

- `grep` 比 `awk` 快，`awk` 比纯 Shell 循环快。优先用 `grep` 缩小范围再交给 `awk`。
- 避免 `cat file | command`，直接 `command file` 更高效。
- 使用 `LC_ALL=C grep`（关闭 locale 影响，加速 ASCII 文本搜索）。

### Q7: 如何在 Shell 命令中安全处理空格和特殊字符？

- 变量引用时加双引号：`grep "$pattern" file` 而非 `grep $pattern file`。
- 使用 `--` 标志分隔选项与参数：`grep -- "$pattern" file` 防止模式以 `-` 开头。
