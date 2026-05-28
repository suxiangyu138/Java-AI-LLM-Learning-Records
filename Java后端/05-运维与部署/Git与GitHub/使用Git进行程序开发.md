使用Git进行程序开发
一、Git在Java后端开发中的核心定位
Git是Java后端开发的版本控制中枢与协作底座，贯穿需求开发、代码编写、团队协作、测试联调、版本发布、故障修复全流程。
对于Java后端而言，Git不仅是代码提交工具，更是连接Spring Boot/Cloud微服务、Maven/Gradle构建、CI/CD自动化、容器化部署的核心纽带，直接决定开发效率、代码质量与项目稳定性。
二、Java后端开发前的Git准备工作
1. 环境与基础配置
    - 安装Git：Windows（Git Bash）、Mac（Homebrew）、Linux（yum/apt），适配IDEA终端操作
    - 全局配置（首次必做）：
    git config --global user.name "你的姓名"
    git config --global user.email "你的邮箱"
    - 配置别名（提升效率）：
    git config --global alias.st status
    git config --global alias.co checkout
    git config --global alias.cm "commit -m"
2. Java项目专属.gitignore配置
    Java后端项目存在大量无需提交的文件，需在根目录创建.gitignore，核心配置：
    IDE配置
    .idea/
    *.iml
    *.iws
    .vscode/
    编译产物
    target/
    build/
    out/
    依赖缓存
    .m2/
    gradle/
    node_modules/
    本地配置
    application-local.yml
    application-dev.yml
    bootstrap-local.yml
    日志与临时文件
    logs/
    *.log
    *.tmp
    *.swp
3. 远程仓库关联
    - 新建项目：git init → git remote add origin 远程地址（GitHub/GitLab/Gitee）
    - 加入已有项目：git clone 远程地址（直接拉取完整仓库与历史记录）
    三、Java后端开发中的Git核心流程（企业级实战）
    1. 分支创建与切换（多需求并行）
    Java后端多采用分支隔离开发，避免不同功能代码干扰，核心命令：
    查看分支
    git branch（本地）、git branch -r（远程）
    创建并切换功能分支（从开发分支拉取）
    git checkout -b feature/user-login dev
    切换分支
    git switch dev（Git 2.23+推荐）
    分支规范（适配Java后端）：
    - main/master：生产稳定分支，禁止直接提交
    - dev：开发汇总分支，整合所有功能
    - feature/*：功能分支（如feature/order-pay）
    - hotfix/*：热修复分支（如hotfix/pay-bug）
    - release/*：发布分支（如release/v1.0.0）
    2. 代码编写与提交（规范采集历史）
    Java后端编码后，遵循“工作区→暂存区→版本库”流程提交：
    1. 查看状态：git status（确认修改的Java文件、配置文件）
    2. 添加暂存：git add .（所有文件）或 git add src/main/java/（指定模块）
    3. 规范提交：git commit -m "feat(user-service): 实现用户登录JWT认证"
    提交规范（Conventional Commits，Java后端高频）：
    - feat：新功能（如新增接口、服务）
    - fix：BUG修复（如业务逻辑、SQL异常）
    - refactor：重构（如代码优化、结构调整）
    - chore：构建/依赖调整（如pom.xml修改、Maven依赖升级）
    - docs：文档（如接口文档、README更新）
    3. 团队协作：拉取、推送与冲突解决
    （1）日常协作命令
    拉取远程最新代码（避免冲突）
    git pull origin dev
    推送本地分支到远程
    git push origin feature/user-login
    查看远程仓库关联
    git remote -v
    （2）Java后端冲突解决（高频场景）
    冲突场景：多人修改同一Service/Controller、application.yml配置
    解决步骤：
    1. git pull触发冲突，文件标记<<<<<<< HEAD（本地）、>>>>>>> 远程分支
    2. 结合业务逻辑保留正确代码（如保留最新Service实现）
    3. 删除冲突标记，重新add、commit、push
    4. 分支合并与版本发布
    （1）功能分支合并（开发阶段）
    git checkout dev
    git merge feature/user-login
    git push origin dev
    git branch -d feature/user-login（删除已合并分支）
    （2）生产版本发布（上线阶段）
    1. 从dev拉取release分支：git checkout -b release/v1.0.0 dev
    2. 测试修复bug，合并到main：git checkout main → git merge release/v1.0.0
    3. 打版本标签（关联Maven版本）：git tag -a v1.0.0 -m "发布用户服务v1.0.0"
    4. 推送标签：git push origin v1.0.0
    5. 故障修复：热修复流程
    生产环境出现紧急BUG时，基于main分支创建hotfix分支：
    git checkout main
    git checkout -b hotfix/fix-login-bug
    修复代码后提交
    git add .
    git commit -m "fix(user-service): 修复登录token过期BUG"
    合并到main和dev
    git checkout main → git merge hotfix/fix-login-bug
    git checkout dev → git merge hotfix/fix-login-bug
    删除热修复分支
    git branch -d hotfix/fix-login-bug
    四、IDEA中Git操作（Java后端高效开发）
    Java后端主流使用IDEA，可视化操作降低命令记忆成本：
    1. 克隆项目：File→New→Project from Version Control→Git
    2. 提交代码：右键项目→Git→Commit（勾选文件、填写提交信息）
    3. 分支管理：右下角Git图标→查看/创建/切换/合并分支
    4. 冲突解决：IDEA自动识别冲突，可视化对比本地与远程代码
    5. 历史查看：Git→Log→查看提交记录、版本差异、回滚操作
    五、Git与Java后端技术栈的深度结合
    1. 配合Maven/Gradle（构建管理）
    - 提交时排除target、build等编译目录，通过.gitignore实现
    - 版本标签（v1.0.0）与pom.xml中1.0.0保持一致，实现版本统一
    2. 配合Spring Cloud（微服务）
    - 多模块项目（user-service、order-service）按模块划分分支，独立开发合并
    - 利用Git分支隔离不同微服务的迭代，避免跨服务代码干扰
    3. 配合CI/CD（自动化部署）
    - 提交触发Jenkins/GitLab CI：Git提交→拉取代码→Maven编译→单元测试→打包部署
    - 分支触发规则：feature分支→测试环境，main分支→生产环境
4. 配合Docker/K8s（容器化）
    - Git存储Dockerfile、k8s配置文件（deployment.yaml），版本化管理容器配置
    - 代码提交触发镜像构建、推送镜像仓库、K8s滚动更新
    六、Java后端Git开发避坑指南
    1. 禁止提交敏感信息：数据库密码、密钥、本地配置，通过环境变量隔离
    2. 不直接修改主分支：main/dev分支仅通过合并操作更新，禁止直接提交
    3. 开发前必拉取代码：git pull获取最新代码，减少冲突概率
    4. 谨慎使用强制推送：git push -f仅适用于个人分支，公共分支禁止使用
    5. 提交粒度细化：单次提交对应单一功能，避免“大杂烩”提交
    6. 及时清理无用分支：删除已合并的feature/hotfix分支，保持分支整洁
    七、总结
    使用Git进行Java后端开发，核心是以规范为基础、以分支为隔离、以历史为追溯、以协作为目标。
    从代码编写到版本发布，Git贯穿全流程，配合IDEA、Maven、CI/CD等工具，形成高效、稳定、可追溯的开发体系。
    对于Java后端工程师而言，熟练掌握Git的流程化操作与技术栈结合，是提升开发效率、适配企业级开发的必备核心能力。
