# 02 - Linux 常用 CLI 工具链

> 🎯 管道 + 文本处理三剑客 + jq + xargs — 后端80%的日志分析和脚本任务靠这几把"瑞士军刀"搞定

---

## 1. 管道与重定向

```bash
# |  管道 — 前一个命令的输出 → 后一个命令的输入
# >  覆盖写   echo "hello" > file.txt
# >> 追加写   echo "world" >> file.txt
# 2> 错误重定向 2>&1 合并错误到标准输出

# 经典组合
cat app.log | grep ERROR | wc -l              # ERROR 总数
tail -f app.log | grep --line-buffered ERROR  # 实时过滤
```

---

## 2. grep — 文本搜索

```bash
grep "ERROR" app.log                    # 基本搜索
grep -i "error" app.log                 # 忽略大小写
grep -v "DEBUG" app.log                 # 排除匹配行
grep -c "NullPointerException" app.log  # 计数
grep -rn "TODO" src/                    # 递归搜索 + 行号
grep -B2 -A2 "Exception" app.log        # 前后各2行上下文
grep -E "ERROR|FATAL" app.log           # 正则 OR
grep "ERROR" *.log | wc -l              # 管道：统计
```

---

## 3. sed — 流编辑器

```bash
sed 's/dev/prod/g' config.yml                      # 替换
sed -i 's/8080/8081/g' application.yml             # 原地修改
sed -n '10,20p' app.log                             # 打印 10-20 行
sed '/DEBUG/d' app.log                              # 删除含 DEBUG 的行
sed -n '/ERROR/p' app.log                           # 仅打印含 ERROR 的行
```

---

## 4. awk — 列处理利器

```bash
# awk 默认按空白符分列：$1=第1列, $NF=最后一列
awk '{print $1}' access.log                          # 第1列
awk '{print $1, $NF}' access.log                     # 第1列+最后列
awk '$3 > 500 {print $1, $3}' access.log             # 条件过滤
awk '{sum+=$3} END {print sum}' access.log           # 求和
awk '{ip[$1]++} END {for(i in ip) print ip[i], i}'   # 按IP分组计数
```

---

## 5. find + xargs — 批量操作

```bash
find . -name "*.log" -mtime +7                       # 7天前的日志
find . -name "*.log" -mtime +7 -delete                # 删除
find . -name "*.java" | xargs grep "TODO"             # 批量搜索
find . -name "*.jar" -exec ls -lh {} \;               # 逐个处理
find src/ -name "*.java" | xargs wc -l                # 代码行数
```

---

## 6. jq — JSON 处理

```bash
curl -s api.example.com/users | jq '.'                # 格式化
curl -s api/users | jq '.data[0].name'               # 取字段
curl -s api/users | jq '.data[] | {id, name}'        # 提取+重组
curl -s api/users | jq '.[] | select(.age > 18)'     # 条件过滤
curl -s api/users | jq 'length'                       # 数组长度
```

---

## 7. 后端日常组合技

```bash
# 找出 QPS Top 10 接口
cat access.log | awk '{print $7}' | sort | uniq -c | sort -rn | head -10

# 杀掉所有包含 spring 的进程
ps -ef | grep spring | awk '{print $2}' | xargs kill -15

# JSON 提取 + 格式化
curl -s localhost:8080/actuator/health | jq '.components.db.status'

# 查磁盘占用最大的目录
du -sh /* 2>/dev/null | sort -rh | head -5
```

> 🎯 **记忆法则**：grep 找行、sed 改行、awk 切列、jq 解JSON、find 找文件、xargs 批量执行。管道串联 = 万能工具。
