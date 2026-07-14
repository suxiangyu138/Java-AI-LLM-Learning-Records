# Python 脚本与自动化

## 📌 定位
**课外自学 | 第一~二梯队 | 效率工具+AI生态**

Python是AI方向的第一语言，也是运维自动化、数据处理、快速原型验证的效率神器。Java写服务，Python写工具和AI，是最佳搭配。

## 🎯 核心章节

### 1. Python 进阶特性（AI必备）
- **生成器(Generator)**：`yield`——惰性求值，处理大文件/无限流节省内存
- **装饰器**：`@timer/@cache`——日志、性能统计、缓存的无侵入实现
- **上下文管理器**：`with open() as f`——自动资源管理
- **类型提示(Type Hints)**：`def add(a:int, b:int)->int:`配合mypy做静态检查

### 2. 数据处理（AI/分析日常工作）
- **NumPy**：多维数组+向量化运算——比Python原生for循环快50-100倍
- **Pandas**：DataFrame = Excel in Python——数据清洗、分组聚合、透视表
- **数据可视化**：Matplotlib(基础)+Seaborn(统计图表)——分析报告必备

### 3. 自动化脚本
- **文件批处理**：重命名/转换格式/提取信息——`os`+`pathlib`+`shutil`
- **网络爬虫**：requests+BeautifulSoup/scrapy——数据采集
- **定时任务**：schedule库/crontab+Python脚本
- **Web API 调用**：`requests`——调用AI API/管理接口

### 4. 运维与开发工具
- **Fabric/Paramiko**：SSH远程批量执行命令
- **日志分析**：`re`正则+`collections.Counter`——从日志中发现模式
- **测试脚本**：pytest+参数化测试——API接口的冒烟测试

## ✅ 学习建议
- Python重点是NumPy+Pandas+PyTorch——AI三件套
- 推荐《Python Cookbook》——实用技巧速查
- 工作中始终备一个Python脚本文档夹——能快速写的就不要手动做
