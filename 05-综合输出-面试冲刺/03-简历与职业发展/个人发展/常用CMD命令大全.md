# 常用 CMD 命令大全
适用 Windows 系统，直接在  Win+R  → 输入  cmd  打开命令提示符使用

## 一、基础目录操作
```cmd
dir                 # 查看当前目录文件列表
cd 文件夹名          # 进入指定文件夹
cd ..               # 返回上一级目录
cd \                # 直接回到根目录
md 文件夹名          # 创建新文件夹
rd 文件夹名          # 删除空文件夹
rd /s /q 文件夹名   # 强制删除文件夹（含内部所有文件）
```
 
## 二、文件操作
```cmd
copy 源文件 目标路径           # 复制文件
move 源文件 目标路径          # 移动/重命名文件
del 文件名                    # 删除文件
del /f /s /q 文件名           # 强制删除文件
ren 旧文件名 新文件名         # 重命名文件
type 文件名                   # 查看文本文件内容
```
 
## 三、网络相关
```cmd
ipconfig               # 查看本机 IP、子网掩码、网关
ipconfig /all          # 查看完整网络信息（MAC、DNS 等）
ping 网址/IP           # 测试网络连通性
ping -t 网址/IP        # 持续 ping
tracert 网址/IP        # 追踪路由路径
netstat -ano           # 查看端口占用、进程 PID
```
 
## 四、系统信息与进程
```cmd
ver                    # 查看 Windows 版本
systeminfo             # 查看系统详细信息
tasklist               # 查看所有运行进程
taskkill /f /pid 进程ID # 强制结束指定 PID 进程
cls                    # 清空命令行屏幕
exit                   # 退出 cmd
```
 
## 五、磁盘与快捷工具
```cmd
chkdsk                 # 磁盘检查
format 盘符:           # 格式化磁盘
start 文件名/网址      # 用默认程序打开文件/网页
notepad                # 打开记事本
calc                   # 打开计算器
```
 
## 六、权限与高级操作
```cmd
runas /user:管理员用户名 cmd  # 以管理员身份打开 cmd
attrib +h 文件名             # 隐藏文件
attrib -h 文件名             # 取消隐藏文件
```
 
## 七、实用组合示例
```cmd
:: 强制删除文件夹
rd /s /q D:\test
:: 查看 8080 端口占用
netstat -ano | findstr "8080"
:: 持续 ping 百度
ping -t www.baidu.com
```
 
 
小提示
- 右键 CMD 选择以管理员身份运行，可以解锁更多系统级命令权限
- 文件名/路径含空格时，用英文双引号包裹，例如： cd "Program Files"
