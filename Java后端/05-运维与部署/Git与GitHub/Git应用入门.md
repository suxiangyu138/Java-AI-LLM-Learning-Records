Git应用入门
一、Git入门核心认知（Java后端视角）
Git是目前Java后端开发中最主流的分布式版本控制系统，核心作用是管理代码版本、支持团队协作、保障代码安全，是Java后端工程师的必备基础技能。
对于Java后端开发而言，Git不仅是代码提交工具，更是连接需求开发、测试、部署的核心纽带，适配Spring Boot、微服务、CI/CD等主流技术场景。
二、Git安装与基础配置（Java开发环境准备）
1. 安装Git
    - Windows：从Git官网下载安装包，默认安装即可，安装后可通过Git Bash、CMD、IDEA终端使用
    - Mac：通过Homebrew安装（brew install git）
    - Linux：通过yum或apt安装（yum install git / apt install git）
2. 基础配置（首次使用必做）
    配置用户名和邮箱（与代码托管平台账号一致，如GitHub、GitLab）
    plaintext  
    git config --global user.name "你的名字"
    git config --global user.email "你的邮箱"
 
配置完成后，可通过 git config --list 查看配置信息，确保配置生效。
3. 忽略文件配置（Java项目必备）
    Java项目中存在大量无需提交的文件（如编译产物、IDE配置、本地配置），需创建 .gitignore 文件，常见配置如下：
    plaintext  

# IDE配置
.idea/
*.iml
*.iws

# 编译产物
target/
build/

# 本地配置
application-local.yml
application-dev.yml

# 日志文件
logs/
*.log

# 依赖缓存
.m2/
node_modules/
 
将 .gitignore 文件放在项目根目录，Git会自动忽略匹配的文件，避免提交无用内容。
三、Git核心基础操作（Java后端日常开发必备）
1. 仓库初始化与克隆
    - 新项目初始化：在项目根目录执行 git init ，创建本地Git仓库
    - 拉取已有项目：通过 git clone 仓库地址 克隆远程仓库（如GitHub、GitLab、Gitee），适用于加入已有Java后端项目
2. 代码提交三步骤
    Git的核心提交流程分为工作区→暂存区→版本库，对应Java后端日常编码后的提交操作：
    1. 查看文件状态： git status ，查看修改、新增、删除的文件，确认需提交的内容
    2. 添加到暂存区： git add 文件名 （单个文件）或 git add . （所有文件），将修改的Java代码、配置文件加入暂存区
    3. 提交到版本库： git commit -m "提交说明" ，提交说明需规范（如 feat: 新增用户登录接口 、 fix: 修复订单查询bug ），便于后续回溯
3. 查看提交记录
    - 基础查看： git log ，显示所有提交的哈希值、作者、时间、提交说明
    - 精简查看： git log --oneline ，仅显示哈希值前缀和提交说明，适合快速定位版本
    - 图形化查看： git log --graph ，以图形形式展示分支提交关系，适配多分支开发场景
4. 撤销与回滚（Java开发高频操作）
    - 撤销工作区修改： git checkout -- 文件名 ，放弃本地未提交的修改（如写错Java代码，恢复到上一次提交状态）
    - 撤销暂存区添加： git reset HEAD 文件名 ，将暂存区的文件移回工作区，重新筛选提交内容
    - 回滚提交记录： git reset --hard 提交哈希值 ，强制回滚到指定版本（谨慎使用，会丢失后续提交）
    四、Git分支操作（Java后端协作核心）
    分支是Git的核心特性，支持Java后端多需求并行开发，避免不同功能代码相互干扰，是团队协作的基础。
    1. 分支基础命令
    - 查看分支： git branch （本地分支）、 git branch -r （远程分支）
    - 创建分支： git branch 分支名 （如 git branch feature/user ）
    - 切换分支： git checkout 分支名  或  git switch 分支名 （Git 2.23+新命令）
    - 创建并切换分支： git checkout -b 分支名 （如 git checkout -b feature/order ）
    - 删除分支： git branch -d 分支名 （删除已合并分支）、 git branch -D 分支名 （强制删除未合并分支）
    2. Java后端分支使用场景
    - 主分支（main/master）：存放稳定、可上线的代码，禁止直接修改
    - 开发分支（develop）：汇总所有功能分支代码，作为日常开发的基准
    - 功能分支（feature/*）：从develop分支拉取，开发单个需求（如用户模块、订单模块），完成后合并回develop
    - 热修复分支（hotfix/*）：从main分支拉取，修复生产环境紧急bug，完成后合并回main和develop
    五、远程仓库协作（Java后端团队开发必备）
    远程仓库（如GitHub、GitLab、Gitee）用于团队代码共享，是Git协作的核心载体。
    1. 关联远程仓库
    本地仓库关联远程仓库： git remote add origin 远程仓库地址 ， origin 是远程仓库的默认别名
2. 推送与拉取代码
    - 推送代码： git push origin 分支名 ，将本地分支代码推送到远程仓库（如 git push origin feature/user ）
    - 拉取代码： git pull origin 分支名 ，拉取远程分支最新代码，避免多人开发时的代码冲突
3. 解决代码冲突
    多人修改同一Java文件（如 UserService.java ）时，拉取代码会触发冲突，解决步骤：
    1. 打开冲突文件，Git会标记冲突区域（ <<<<<<< HEAD 为本地代码， >>>>>>> 远程分支 为远程代码）
    2. 结合业务逻辑，保留正确代码，删除冲突标记
    3. 重新添加、提交、推送，完成冲突解决
    六、IDEA中Git操作（Java后端开发高效方式）
    Java后端开发主流使用IDEA，其集成了Git可视化操作，无需记忆命令，降低使用门槛：
    1. 克隆项目：File→New→Project from Version Control→Git，输入仓库地址克隆
    2. 提交代码：右键项目→Git→Commit，选择文件、填写提交说明，完成提交
    3. 分支操作：右下角Git分支图标，可查看、创建、切换、合并分支
    4. 推送拉取：右键项目→Git→Push/Pull，完成远程协作
    5. 冲突解决：IDEA会自动识别冲突，提供可视化界面，直观对比本地与远程代码，快速合并
    七、Git入门避坑指南（Java后端新手必看）
    1. 禁止提交敏感信息：数据库密码、密钥、本地配置等，通过 .gitignore 和环境变量隔离
    2. 提交说明需规范：使用 feat 、 fix 、 refactor 等前缀，便于团队协作和版本回溯
    3. 开发前必拉取代码： git pull 获取最新代码，减少冲突概率
    4. 不随意强制推送： git push -f 会覆盖远程代码，仅个人分支可使用，公共分支禁止
    5. 及时提交代码：避免长时间不提交，导致代码丢失或冲突难以解决
    八、总结
    Git应用入门是Java后端开发的基础，核心掌握安装配置、基础提交、分支操作、远程协作四大模块，配合IDEA可视化工具，可快速适配日常开发场景。
    对于Java后端工程师而言，Git不仅是代码管理工具，更是团队协作、版本控制、保障项目稳定的核心能力，熟练掌握Git基础操作，是进阶学习企业级工作流、CI/CD的前提。
