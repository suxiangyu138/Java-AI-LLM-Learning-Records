04.27 10:37
快速吃透：AI 领域 Skill｜是什么、和Agent关系、怎么学、怎么用
一、先定义：AI 里的 Skill 是什么
AI 语境下：
Skill = 大模型/Agent 可调用的「具体能力工具」
翻译：
- 中文常叫：技能、工具、能力插件
- 英文：Skill / Tool / Function
大白话
LLM 本身只会打字、推理；
Skill 就是给大模型装的「一个个专属手脚」，让它能干具体事。
 
二、Skill 能干什么（AI 常见技能）
AI 项目里标准 Skill 清单：
1. 联网搜索 Skill：实时查新闻、查资料、查最新技术
2. 文档检索 Skill：对接RAG/向量库，查私有笔记、课程资料
3. 数据库Skill：查询 MySQL、Redis、业务数据
4. 代码执行Skill：运行Python、算数学、写脚本、排错
5. 接口调用Skill：调用第三方API、天气、时间、爬虫
6. 系统操作Skill：读写本地文件、整理MD笔记、批量处理
 
三、Skill / LLM / Agent 三者核心关系（必背）
1. LLM 大模型
核心：思考、理解、推理、写文案
短板：无行动能力、不能联网、不能操作外部软件
2. Skill 技能
核心：纯执行、纯工具
特点：不会思考，只负责干固定的一件事
3. Agent 智能体
核心：调度者、指挥官
逻辑：
- Agent（大脑调度）
- 思考需要哪个 Skill
- 自动调用对应 Skill
- 拿到结果再交给 LLM 整合回答
层级关系
plaintext
LLM（思考决策）
 ↓
Agent（任务拆解+调度）
 ↓
Skill 1 / Skill 2 / Skill 3（具体干活）
 
一句话：
LLM动脑，Skill动手，Agent管调度。
 
四、AI Skill 的核心特性
1. 单一职责
一个 Skill 只做一件事：比如「只查向量库」「只联网搜索」
2. 标准化入参出参
统一入参、统一返回格式，Agent 好调用
3. 可插拔
想用就加载、不用就关闭，随时新增自定义技能
4. 结构化描述
必须写清楚：技能用途、参数、使用场景
 大模型靠这段描述，判断什么时候调用它
 
五、主流框架中怎么写 Skill（极简模板）
LangChain / LlamaIndex / 各类Agent框架 写法统一：
1. 定义函数（具体功能）
2. 加描述注释（给大模型看）
3. 注册到技能列表
4. Agent 自动识别、调用
最简 AI Skill 代码示例
python
from langchain_core.tools import tool
# 自定义一个AI Skill
@tool
def search_tech_knowledge(keyword: str) -> str:
    """
    技能用途：查询计算机后端、AI相关技术知识点
    参数：
        keyword: 要查询的技术名词
    """
    return f"【技能检索】{keyword} 核心原理、用法已查询完成"
# 把技能交给Agent
skills = [search_tech_knowledge]
 
-  @tool  就是标记这是一个 AI Skill
- 注释 = 大模型识别这个技能的说明书
 
六、RAG、Skill、Agent 区别速记
1. RAG
一种专属Skill：知识检索技能，只负责查文档
2. Skill
泛指所有工具能力：检索、搜索、数据库、代码
3. Agent
调度中心：按需选择不同 Skill 组合完成复杂任务
 
七、如何快速学会 AI Skill（3步速成）
1. 先分清边界
- LLM：负责想
- Skill：负责做
- Agent：负责派活
2. 掌握 Skill 固定结构
- 名称
- 功能描述
- 入参
- 执行逻辑
- 返回结果
3. 亲手写 2 个最小Skill
- 文档检索Skill（对接RAG）
- 简单自定义工具Skill
马上就能融会贯通。
 
八、最终极简背诵版
1. AI领域 Skill = 可调用工具/技能插件
2. LLM 无行动能力，靠 Skill 落地执行
3. Agent 负责判断+调用各类 Skill
4. RAG 本质就是一种「知识库检索 Skill」
5. 未来AI开发：大模型 + 多Skill + Agent编排 是标准架构

