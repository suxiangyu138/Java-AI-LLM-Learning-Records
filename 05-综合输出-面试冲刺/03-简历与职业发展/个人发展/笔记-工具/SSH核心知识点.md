# SSH 核心知识点

## 一、基础定义
SSH（Secure Shell）：一种加密的远程登录协议，用于安全访问远程服务器、执行命令、传输文件，替代不安全的 Telnet、FTP。
- 端口：默认 22
- 核心优势：传输加密、身份认证、防中间人攻击
- 常见用途：远程服务器管理、Git 远程连接、文件传输、端口转发

## 二、核心工作原理
1. 建立 TCP 连接：客户端连接服务器 22 端口
2. 版本协商：两端协商 SSH 协议版本（SSH-2 为主流）
3. 密钥交换：
    - 服务器发送公钥给客户端
    - 客户端验证服务器身份（首次连接需确认指纹）
4. 会话密钥生成：两端通过非对称加密算法，协商出对称加密会话密钥
5. 加密通信：后续所有数据用会话密钥对称加密传输
6. 用户认证：密码认证 / 密钥认证
    非对称加密：只用来协商密钥；对称加密：用来传输数据（速度快）

## 三、两种身份认证方式
1. 密码认证
    - 原理：输入用户名+密码，服务器校验
    - 缺点：暴力破解风险高，安全性弱
2. 密钥认证（推荐）
    核心逻辑：公私钥配对
    - 客户端生成：私钥（本地保存，绝不泄露）、公钥（ .pub ，放到服务器）
    - 服务器校验：客户端用私钥签名，服务器用公钥验签
    - 优势：免密登录、防暴力破解、安全性极高
    常用命令
```bash
# 生成密钥对（默认 RSA）
ssh-keygen
# 推送公钥到远程服务器
ssh-copy-id user@ip
```
 
## 四、常用基础命令
```bash
# 远程登录
ssh user@ip
ssh -p 2222 user@ip  # 指定端口
# 执行远程命令后退出
ssh user@ip "ls -l"
# 本地文件上传到远程
scp file user@ip:/path
# 远程文件下载到本地
scp user@ip:/path/file .
```
 
## 五、SSH 配置文件
客户端配置  ~/.ssh/config 
简化登录，免输 IP/端口/用户名
```plaintext
Host myserver
    HostName 192.168.1.100
    User root
    Port 22
    IdentityFile ~/.ssh/id_rsa
```
 
使用： ssh myserver 
服务器关键配置  /etc/ssh/sshd_config 
```plaintext
Port 22                  # 修改默认端口
PermitRootLogin no       # 禁止 root 直接登录
PasswordAuthentication no # 关闭密码登录（只留密钥）
PubkeyAuthentication yes  # 开启密钥认证
```
 
重启服务：
```bash
# CentOS
systemctl restart sshd
# Ubuntu
systemctl restart ssh
```
 
## 六、三大端口转发（核心高级功能）
1. 本地端口转发
    远程端口映射到本地
```bash
ssh -L 本地端口:目标IP:目标端口 user@ssh服务器
```
 
2. 远程端口转发
    本地端口暴露到远程服务器
```bash
ssh -R 远程端口:本地IP:本地端口 user@ssh服务器
```
 
3. 动态端口转发（SOCKS5 代理）
```bash
ssh -D 1080 user@ip
```
 
## 七、常见问题与安全加固
1. 修改默认 22 端口，降低扫描风险
2. 禁止 root 登录
3. 关闭密码登录，只使用密钥
4. 限制 IP 访问（防火墙/sshd 配置）
5. 私钥权限必须严格：
```bash
chmod 600 ~/.ssh/id_rsa
```
 
## 八、一句话总结
SSH 是一套「非对称加密协商+对称加密传输」的安全远程协议，核心价值是加密通信+免密密钥认证，同时具备强大的端口转发能力，是服务器运维、开发远程连接的基础工具。
