Git更换远程仓库
一、极简总结（换Git远程仓库 3步）
1. 查看当前远程
    bash
    git remote -v
 
2. 更换绑定新仓库（二选一）
    bash

# 直接覆盖（简单）
git remote set-url origin 新仓库地址

# 删旧绑新（干净）
git remote remove origin
git remote add origin 新仓库地址
 
3. 首次推送绑定分支
    bash
    git push -u origin main
 
 
二、HTTPS 与 SSH 区别（大白话）
1. HTTPS 地址
    格式：
    plaintext
    https://github.com/xxx/xxx.git
 
- 登录方式：每次/定期输入 账号+个人令牌/密码
- 优点：
    1. 开箱即用，不用配置密钥
    2. 校园网、公司内网不会被拦截
- 缺点：
    频繁输账号密码，稍麻烦
- 适合：临时使用、个人简单项目、Windows 普通用户
    2. SSH 地址
    格式：
    plaintext
    git@github.com:xxx/xxx.git
 
- 登录方式：本地配置 SSH公私钥，免密
- 优点：
    1. 一次配置，永久免密推送拉取
    2. 日常开发效率高
- 缺点：
    1. 需要手动配置密钥
    2. 部分校园网/防火墙会屏蔽SSH端口
- 适合：长期开发、经常提交代码、稳定网络
 
三、快速选择建议
- 不想折腾、快速换仓库 → 用 HTTPS
- 长期写代码、频繁  push/pull  → 配置密钥用 SSH
 
四、补充高频问题
新仓库有  README  冲突时执行：
bash
git pull origin main --allow-unrelated-histories
