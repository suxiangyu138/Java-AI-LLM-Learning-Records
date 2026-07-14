# 第14步：复杂AI Agent实战

> **阶段目标：** 掌握Agent外部工具调用的高级模式，实现具有自主决策能力的复杂Agent系统  
> **预计学时：** 2-3周（每天3-4小时）  
> **前置要求：** AI Agent概念 + Prompt Engineering + API开发  

---

## 📚 目录

- [14.1 高级工具编排](#141-高级工具编排)
- [14.2 规划与决策](#142-规划与决策)
- [14.3 记忆与状态管理](#143-记忆与状态管理)
- [14.4 Multi-Agent协作](#144-multi-agent协作)
- [14.5 Agent安全与可控](#145-agent安全与可控)
- [14.6 生产级Agent框架设计](#146-生产级agent框架设计)
- [14.7 阶段练习](#147-阶段练习)

---

## 14.1 高级工具编排

### 14.1.1 工具系统设计

```python
from typing import Any, Callable, Dict, List, Optional, Union
from pydantic import BaseModel, Field
from enum import Enum
import inspect

class ToolCategory(Enum):
    SEARCH = "search"         # 搜索类
    COMPUTE = "compute"       # 计算类
    DATA = "data"            # 数据操作
    COMMUNICATION = "comm"    # 通信类
    SYSTEM = "system"        # 系统操作
    CUSTOM = "custom"        # 自定义

class ToolSchema(BaseModel):
    """工具的标准Schema定义"""
    name: str = Field(..., description="工具名称")
    description: str = Field(..., description="工具功能描述，清晰说明何时使用")
    category: ToolCategory = ToolCategory.CUSTOM
    parameters: Dict[str, Any] = Field(..., description="参数JSON Schema")
    examples: List[str] = Field(default_factory=list, description="使用示例")
    requires_confirmation: bool = Field(default=False, description="是否需要用户确认")
    timeout: int = Field(default=30, description="超时时间(秒)")
    retry_count: int = Field(default=0, description="失败重试次数")

class ToolRegistry:
    """
    工具注册表 — 管理所有可用工具
    
    设计原则：
    1. 每个工具有清晰的描述和Schema
    2. 支持工具的启用/禁用
    3. 工具执行有统一的错误处理
    4. 支持工具的权限管理
    """
    
    def __init__(self):
        self._tools: Dict[str, tuple[ToolSchema, Callable]] = {}
    
    def register(self, schema: ToolSchema, func: Callable):
        """注册工具"""
        # 验证函数签名与Schema一致
        sig = inspect.signature(func)
        for param_name in schema.parameters.get("required", []):
            if param_name not in sig.parameters:
                raise ValueError(
                    f"工具 '{schema.name}' 的函数缺少必需参数: {param_name}"
                )
        
        self._tools[schema.name] = (schema, func)
    
    def get_schema(self, name: str) -> ToolSchema:
        return self._tools[name][0]
    
    def get_function(self, name: str) -> Callable:
        return self._tools[name][1]
    
    def list_tools(self, category: ToolCategory = None) -> List[ToolSchema]:
        """列出所有工具"""
        tools = [s for s, _ in self._tools.values()]
        if category:
            tools = [t for t in tools if t.category == category]
        return tools
    
    def to_openai_format(self) -> List[dict]:
        """转为OpenAI Function Calling格式"""
        return [{
            "type": "function",
            "function": {
                "name": schema.name,
                "description": schema.description,
                "parameters": {
                    "type": "object",
                    "properties": schema.parameters.get("properties", {}),
                    "required": schema.parameters.get("required", []),
                }
            }
        } for schema, _ in self._tools.values()]


# ========== 工具注册示例 ==========
registry = ToolRegistry()

# 搜索工具
registry.register(
    ToolSchema(
        name="web_search",
        description="搜索互联网获取最新信息。当需要实时信息或模型不知道的内容时使用。",
        category=ToolCategory.SEARCH,
        parameters={
            "type": "object",
            "properties": {
                "query": {"type": "string", "description": "搜索关键词"},
                "num_results": {"type": "integer", "description": "返回结果数", "default": 5},
            },
            "required": ["query"],
        },
        examples=["搜索'2024年诺贝尔奖得主'"],
        timeout=10,
    ),
    web_search_function,
)

# 代码执行工具
registry.register(
    ToolSchema(
        name="execute_python",
        description="在安全的沙箱中执行Python代码并返回结果。适用于需要计算或数据处理的场景。",
        category=ToolCategory.COMPUTE,
        parameters={
            "type": "object",
            "properties": {
                "code": {"type": "string", "description": "要执行的Python代码"},
            },
            "required": ["code"],
        },
        requires_confirmation=True,  # 需要用户确认！
        timeout=30,
    ),
    execute_python_sandbox,
)

# 文件操作工具
registry.register(
    ToolSchema(
        name="read_file",
        description="读取指定路径的文件内容",
        category=ToolCategory.SYSTEM,
        parameters={
            "type": "object",
            "properties": {
                "path": {"type": "string", "description": "文件路径"},
                "encoding": {"type": "string", "default": "utf-8"},
            },
            "required": ["path"],
        },
    ),
    read_file_function,
)
```

### 14.1.2 工具链编排

```python
class ToolChain:
    """
    工具链编排器
    
    支持模式：
    1. 顺序执行：A → B → C
    2. 条件执行：if 条件 then A else B
    3. 并行执行：A || B || C（同时运行）
    4. 循环执行：while 条件 do A
    """
    
    def __init__(self, registry: ToolRegistry):
        self.registry = registry
    
    async def execute_sequential(self, steps: List[Dict]) -> List[Any]:
        """顺序执行工具链"""
        results = []
        context = {}  # 前一步的结果可以传给下一步
        
        for step in steps:
            tool_name = step["tool"]
            params = self._resolve_params(step.get("params", {}), context)
            
            result = await self._execute_tool(tool_name, params)
            results.append(result)
            context[f"step_{len(results)}"] = result
        
        return results
    
    async def execute_parallel(self, steps: List[Dict]) -> List[Any]:
        """并行执行工具（各步骤独立）"""
        tasks = []
        for step in steps:
            tool_name = step["tool"]
            params = step.get("params", {})
            tasks.append(self._execute_tool(tool_name, params))
        
        return await asyncio.gather(*tasks, return_exceptions=True)
    
    async def execute_conditional(self, condition: str,
                                   if_step: Dict, else_step: Dict = None,
                                   context: Dict = None) -> Any:
        """条件执行"""
        # 用LLM评估条件
        condition_result = await self._evaluate_condition(condition, context)
        
        if condition_result:
            return await self._execute_tool(if_step["tool"], if_step.get("params", {}))
        elif else_step:
            return await self._execute_tool(else_step["tool"], else_step.get("params", {}))
    
    async def _execute_tool(self, name: str, params: Dict) -> Any:
        """执行单个工具，带错误处理和重试"""
        schema = self.registry.get_schema(name)
        func = self.registry.get_function(name)
        
        for attempt in range(schema.retry_count + 1):
            try:
                result = await asyncio.wait_for(
                    func(**params) if inspect.iscoroutinefunction(func) 
                    else asyncio.to_thread(func, **params),
                    timeout=schema.timeout,
                )
                return {"tool": name, "success": True, "result": result}
            except asyncio.TimeoutError:
                if attempt == schema.retry_count:
                    return {"tool": name, "success": False, "error": "超时"}
            except Exception as e:
                if attempt == schema.retry_count:
                    return {"tool": name, "success": False, "error": str(e)}
                await asyncio.sleep(2 ** attempt)  # 指数退避
    
    def _resolve_params(self, params: Dict, context: Dict) -> Dict:
        """解析参数中的引用（如 $step_1.result）"""
        import re
        resolved = {}
        for key, value in params.items():
            if isinstance(value, str):
                # 替换上下文引用
                for ctx_key, ctx_val in context.items():
                    value = value.replace(f"${ctx_key}", str(ctx_val))
            resolved[key] = value
        return resolved
```

---

## 14.2 规划与决策

### 14.2.1 任务分解 (Task Decomposition)

```python
class TaskPlanner:
    """
    任务规划器
    
    将复杂任务分解为可执行的子任务列表
    """
    
    def __init__(self, llm_client):
        self.llm = llm_client
    
    async def plan(self, task: str, available_tools: List[str]) -> List[Dict]:
        """将任务分解为执行计划"""
        prompt = f"""
你是一个任务规划专家。请将以下任务分解为清晰的执行步骤。

# 可用工具
{', '.join(available_tools)}

# 任务
{task}

# 输出格式
请以JSON数组返回执行计划，每个步骤包含：
- step: 步骤编号
- description: 该步骤要做什么
- tool: 需要使用的工具名（如果不需要工具，填null）
- expected_output: 该步骤应该产生什么输出
- dependencies: 依赖的前置步骤编号列表（没有依赖填[]）

示例：
[
  {{"step": 1, "description": "搜索相关信息", "tool": "web_search", 
    "expected_output": "搜索结果列表", "dependencies": []}},
  ...
]
"""
        
        response = self.llm.chat.completions.create(
            model="gpt-4o",
            messages=[{"role": "user", "content": prompt}],
            response_format={"type": "json_object"},
            temperature=0.3,
        )
        
        plan = json.loads(response.choices[0].message.content)
        return plan.get("steps", plan)

class PlanExecutor:
    """
    计划执行器
    
    特点：
    - 支持依赖关系（A完成后才能做B）
    - 失败自动调整（某步失败，重新规划后续步骤）
    - 进度追踪
    """
    
    def __init__(self, tool_chain: ToolChain, llm_client):
        self.tool_chain = tool_chain
        self.llm = llm_client
    
    async def execute(self, plan: List[Dict]) -> Dict:
        """执行计划"""
        results = {}
        completed = set()
        
        for step in plan:
            # 检查依赖
            deps = step.get("dependencies", [])
            if not all(d in completed for d in deps):
                print(f"步骤{step['step']}依赖未满足，等待...")
                continue
            
            print(f"执行步骤{step['step']}: {step['description']}")
            
            if step.get("tool"):
                result = await self.tool_chain.execute_sequential([{
                    "tool": step["tool"],
                    "params": step.get("params", {}),
                }])
                
                if not result[0]["success"]:
                    # 失败时重新规划
                    print(f"步骤{step['step']}失败: {result[0]['error']}")
                    return await self._replan_and_execute(
                        plan, step["step"], result[0]["error"], completed
                    )
                
                results[f"step_{step['step']}"] = result[0]["result"]
            
            completed.add(step["step"])
        
        return {"status": "completed", "results": results}
    
    async def _replan_and_execute(self, original_plan: List[Dict],
                                   failed_step: int, error: str,
                                   completed: set) -> Dict:
        """某步失败后重新规划剩余步骤"""
        remaining = [s for s in original_plan 
                     if s["step"] >= failed_step]
        
        prompt = f"""
原计划中步骤{failed_step}执行失败。
错误信息：{error}

已完成步骤：{list(completed)}
剩余待执行步骤：{json.dumps(remaining, ensure_ascii=False)}

请重新规划剩余步骤，绕过失败的原因。
"""
        
        response = self.llm.chat.completions.create(
            model="gpt-4o",
            messages=[{"role": "user", "content": prompt}],
            response_format={"type": "json_object"},
        )
        
        new_plan = json.loads(response.choices[0].message.content)
        return await self.execute(new_plan.get("revised_plan", []))
```

### 14.2.2 ReAct模式深入

```python
class ReActAgent:
    """
    ReAct Agent的完整实现
    
    ReAct = Reasoning + Acting
    循环模式：Thought → Action → Observation → Thought → ...
    
    关键改进：
    - 结构化输出（强制格式）
    - 步骤计数（防止无限循环）
    - 反思机制（定期回顾进展）
    """
    
    def __init__(self, llm_client, tool_registry: ToolRegistry, 
                 max_steps: int = 15):
        self.llm = llm_client
        self.tools = tool_registry
        self.max_steps = max_steps
    
    async def run(self, task: str) -> Dict:
        """执行ReAct循环"""
        history = []
        context = {"task": task}
        
        system_prompt = self._build_system_prompt()
        
        for step_count in range(self.max_steps):
            # 构建当前步骤的Prompt
            step_prompt = self._build_step_prompt(
                task=task,
                history=history,
                step_count=step_count,
                context=context,
            )
            
            # 调用LLM
            response = self.llm.chat.completions.create(
                model="gpt-4o",
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": step_prompt},
                ],
                tools=self.tools.to_openai_format(),
                tool_choice="auto",
                response_format={"type": "json_object"},
            )
            
            message = response.choices[0].message
            
            # 解析Thought（思考）
            thought = self._extract_thought(message.content)
            
            # 检查是否完成任务
            if self._is_final_answer(thought):
                return {
                    "status": "completed",
                    "answer": self._extract_answer(message.content),
                    "steps": step_count + 1,
                    "history": history,
                }
            
            # 执行Action
            if message.tool_calls:
                for tool_call in message.tool_calls:
                    tool_name = tool_call.function.name
                    tool_args = json.loads(tool_call.function.arguments)
                    
                    observation = await self._execute_tool(tool_name, tool_args)
                    
                    # 记录步骤
                    history.append({
                        "step": step_count + 1,
                        "thought": thought,
                        "action": {"tool": tool_name, "args": tool_args},
                        "observation": observation,
                    })
                    
                    # 更新上下文
                    context[f"observation_{step_count}"] = observation
            
            # 每5步反思一次
            if step_count > 0 and step_count % 5 == 0:
                reflection = await self._reflect(history)
                context["reflection"] = reflection
        
        return {
            "status": "max_steps_reached",
            "last_context": context,
            "history": history,
        }
    
    def _build_system_prompt(self) -> str:
        tools_desc = "\n".join([
            f"- {t.name}: {t.description}" 
            for t in self.tools.list_tools()
        ])
        
        return f"""
你是一个具有自主决策能力的AI Agent。

# 可用工具
{tools_desc}

# ReAct格式
每一步你必须输出Thought，如果需要使用工具，说明要使用的工具和参数。

# 决策指南
1. 如果信息不足，使用工具获取
2. 如果信息足够，直接给出最终答案
3. 如果工具调用失败，尝试其他方法
4. 避免重复调用同样的工具（除非有新的参数）

# 最终回答格式
当任务完成时，Thought中说明"任务完成"并给出完整的最终答案。
"""
    
    async def _reflect(self, history: List[Dict]) -> str:
        """定期反思：检查是否偏离目标，调整策略"""
        prompt = f"""
回顾当前执行历史，总结：
1. 已经完成了什么？
2. 是否偏离了最初的目标？
3. 下一步应该如何调整？
"""
        # 调用LLM反思
        pass
```

---

## 14.3 记忆与状态管理

### 14.3.1 分层记忆系统

```python
class AgentMemory:
    """
    Agent分层记忆系统
    
    L1: 工作记忆 (Working Memory)
        - 当前任务的上下文
        - Token限制 → 需要滑动窗口
        - 存储：messages列表
    
    L2: 情景记忆 (Episodic Memory)
        - 历史任务的执行记录
        - 用于：检索类似任务的解决方案
        - 存储：向量数据库
    
    L3: 语义记忆 (Semantic Memory)
        - 学到的知识、规则、偏好
        - 用于：快速决策、避免重复错误
        - 存储：键值存储 + 向量数据库
    """
    
    def __init__(self, vector_store, embedding_model):
        self.vector_store = vector_store
        self.encoder = embedding_model
        
        # 工作记忆
        self.working: List[Dict] = []
        self.max_working_size = 20
        
        # 语义记忆（键值对）
        self.semantic: Dict[str, Any] = {}
    
    def add_to_working(self, entry: Dict):
        """添加到工作记忆"""
        self.working.append(entry)
        
        # 超出容量时压缩
        if len(self.working) > self.max_working_size:
            self._compress_working()
    
    def _compress_working(self):
        """压缩工作记忆（保留最近的+总结旧的）"""
        recent = self.working[-self.max_working_size // 2:]
        old = self.working[:-self.max_working_size // 2]
        
        # 用LLM总结旧内容
        summary = self._summarize(old)
        
        self.working = [{"role": "system", "content": f"历史摘要: {summary}"}] + recent
    
    def store_episode(self, task: str, result: Dict):
        """存储一个完整的任务执行记录"""
        episode_text = json.dumps({
            "task": task,
            "result": result,
            "timestamp": time.time(),
        }, ensure_ascii=False)
        
        embedding = self.encoder.encode([episode_text])[0]
        
        self.vector_store.add(
            documents=[episode_text],
            embeddings=[embedding],
            metadatas=[{"type": "episode", "status": result.get("status")}],
        )
    
    def recall_similar_episodes(self, task: str, k: int = 3) -> List[Dict]:
        """检索类似的历史任务"""
        query_embedding = self.encoder.encode([task])[0]
        results = self.vector_store.search(query_embedding, k)
        
        return [json.loads(r["content"]) for r in results]
    
    def learn_rule(self, condition: str, action: str):
        """学习一条规则（语义记忆）"""
        self.semantic[f"rule:{condition}"] = {
            "action": action,
            "learned_at": time.time(),
        }
    
    def apply_rules(self, context: Dict) -> Optional[str]:
        """尝试应用已学规则"""
        for key, rule in self.semantic.items():
            if key.startswith("rule:"):
                condition = key[5:]
                if condition in str(context):
                    return rule["action"]
        return None
    
    def _summarize(self, entries: List[Dict]) -> str:
        """用LLM总结一段对话/执行历史"""
        # 实现省略
        pass
```

---

## 14.4 Multi-Agent协作

### 14.4.1 角色分工模式

```python
@dataclass
class AgentRole:
    """Agent角色定义"""
    name: str
    expertise: str
    responsibilities: str
    system_prompt: str
    tools: List[str]  # 该角色可用的工具名

class MultiAgentOrchestrator:
    """
    多Agent编排器
    
    模式1: 顺序协作 (Sequential)
        Agent A → Agent B → Agent C
        适合：流水线型任务
        
    模式2: 讨论协作 (Debate)
        Agent A ⇄ Agent B (多轮讨论)
        适合：需要多视角的任务
        
    模式3: 分层协作 (Hierarchical)
        Manager Agent 分配任务给 Worker Agents
        适合：复杂项目
    """
    
    def __init__(self, llm_client, tool_registry: ToolRegistry):
        self.llm = llm_client
        self.tools = tool_registry
    
    async def sequential(self, task: str, roles: List[AgentRole]) -> str:
        """顺序协作：每个Agent处理前一个的输出"""
        current_output = task
        
        for role in roles:
            prompt = f"""
你是{role.name}，{role.expertise}。
你的职责是{role.responsibilities}。

当前任务：{current_output}

请基于你的专业知识处理这个任务。
"""
            # 为该Role创建Agent，配置对应的工具
            role_tools = [t for t in self.tools.list_tools() 
                         if t.name in role.tools]
            
            agent = ReActAgent(self.llm, role_tools)
            result = await agent.run(prompt)
            current_output = result.get("answer", str(result))
        
        return current_output
    
    async def debate(self, task: str, roles: List[AgentRole], 
                     rounds: int = 3) -> str:
        """讨论协作：多Agent讨论后达成共识"""
        discussion = [f"讨论主题：{task}"]
        
        for round_num in range(rounds):
            for role in roles:
                prompt = f"""
你是{role.name}，{role.expertise}。

# 讨论主题
{task}

# 之前的讨论
{chr(10).join(discussion[-5:])}

请基于你的专业视角发表观点。如果同意之前某人的观点，说明为什么同意。
如果不同意，说明为什么不同意。
"""
                
                response = self.llm.chat.completions.create(
                    model="gpt-4o",
                    messages=[{"role": "user", "content": prompt}],
                )
                
                opinion = f"[{role.name}]: {response.choices[0].message.content}"
                discussion.append(opinion)
        
        # 总结讨论，形成最终结论
        summary_prompt = f"""
基于以下多专家讨论，形成最终结论：

{chr(10).join(discussion)}

请给出：
1. 达成共识的内容
2. 仍存在的分歧
3. 最终建议
"""
        final = self.llm.chat.completions.create(
            model="gpt-4o",
            messages=[{"role": "user", "content": summary_prompt}],
        )
        
        return final.choices[0].message.content
    
    async def hierarchical(self, task: str) -> str:
        """
        分层协作：
        
        Manager (规划分配)
        ├── Researcher (研究分析)
        ├── Coder (代码实现)
        └── Reviewer (审查验证)
        """
        # 1. Manager分解任务
        manager = AgentRole(
            name="项目经理",
            expertise="任务分解和资源分配",
            responsibilities="将复杂任务分解为子任务，分配给合适的专家",
            system_prompt="你是项目经理...",
            tools=[],
        )
        
        workers = [
            AgentRole("研究员", "信息收集和分析", "...", [], ["web_search"]),
            AgentRole("工程师", "代码实现", "...", [], ["execute_python"]),
            AgentRole("审查员", "质量检查和验证", "...", [], []),
        ]
        
        # 2. Manager生成分配计划
        plan = await self._manager_plan(task, manager, workers)
        
        # 3. 并行执行各Worker的任务
        worker_tasks = []
        for assignment in plan.get("assignments", []):
            worker = next(w for w in workers if w.name == assignment["worker"])
            worker_tasks.append(self.sequential(assignment["task"], [worker]))
        
        results = await asyncio.gather(*worker_tasks)
        
        # 4. Manager整合结果
        return await self._manager_integrate(results)
```

---

## 14.5 Agent安全与可控

### 14.5.1 安全防护

```python
class AgentGuard:
    """
    Agent安全守护
    
    防护层：
    L1 - 输入验证：检查用户请求
    L2 - 工具白名单：只允许安全的工具
    L3 - 操作确认：高风险操作需要用户确认
    L4 - 输出审查：检查Agent的输出
    L5 - 审计日志：记录所有操作
    """
    
    def __init__(self):
        self.audit_log = []
        self.blocked_patterns = [
            r"rm\s+-rf",           # 危险命令
            r"DROP\s+TABLE",       # SQL注入
            r"os\.system",         # 系统调用
            r"subprocess",         # 子进程
        ]
    
    def validate_input(self, user_request: str) -> bool:
        """L1: 验证用户输入"""
        import re
        for pattern in self.blocked_patterns:
            if re.search(pattern, user_request, re.IGNORECASE):
                self._log("BLOCKED", f"危险输入模式: {pattern}")
                return False
        return True
    
    def check_tool_permission(self, tool_name: str, 
                              user_role: str = "user") -> bool:
        """L2: 检查工具权限"""
        # 管理员可用所有工具
        if user_role == "admin":
            return True
        
        # 普通用户不可用的工具
        restricted = {"execute_shell", "delete_file", "drop_database"}
        if tool_name in restricted:
            self._log("BLOCKED", f"权限不足: {tool_name}")
            return False
        
        return True
    
    async def confirm_dangerous_action(self, tool_name: str, 
                                        params: Dict) -> bool:
        """L3: 高风险操作确认"""
        high_risk = {
            "send_email": "发送邮件",
            "create_file": "创建文件",
            "execute_python": "执行代码",
            "make_payment": "发起支付",
        }
        
        if tool_name in high_risk:
            # 在实际应用中，这里应该触发用户确认流程
            self._log("CONFIRM", f"需要确认: {high_risk[tool_name]}")
            # 返回True表示已确认（简化示例）
            return True
        
        return True
    
    def review_output(self, output: str) -> bool:
        """L4: 审查Agent输出"""
        sensitive_patterns = [
            r"\b\d{16}\b",        # 信用卡号
            r"\b\d{11}\b",        # 手机号
            r"password",          # 密码关键词
        ]
        
        import re
        for pattern in sensitive_patterns:
            if re.search(pattern, output, re.IGNORECASE):
                self._log("ALERT", f"输出包含敏感信息: {pattern}")
                return False
        
        return True
    
    def _log(self, action: str, detail: str):
        """L5: 审计日志"""
        entry = {
            "timestamp": time.time(),
            "action": action,
            "detail": detail,
        }
        self.audit_log.append(entry)
        logger.info(f"[AgentGuard] {action}: {detail}")
```

---

## 14.6 生产级Agent框架设计

```python
class ProductionAgent:
    """
    生产级Agent — 整合所有组件
    
    特性：
    ✅ 多工具编排
    ✅ ReAct推理循环
    ✅ 分层记忆系统
    ✅ 任务规划与反思
    ✅ 安全守护
    ✅ 审计日志
    ✅ 可观测性（指标/监控）
    """
    
    def __init__(self, config: Dict):
        self.config = config
        self.llm = self._init_llm(config)
        self.tools = self._init_tools(config)
        self.memory = AgentMemory(config.get("vector_store"), 
                                   config.get("embedding_model"))
        self.planner = TaskPlanner(self.llm)
        self.guard = AgentGuard()
        self.metrics = AgentMetrics()
    
    async def execute(self, task: str, user_id: str = None) -> Dict:
        """执行任务的主入口"""
        start_time = time.time()
        
        # L1: 安全检查
        if not self.guard.validate_input(task):
            return {"error": "请求被安全检查拦截"}
        
        # 检查是否有历史经验可复用
        similar = self.memory.recall_similar_episodes(task)
        if similar and similar[0]["result"].get("status") == "completed":
            # 可以借鉴历史方案
            pass
        
        # 如果任务复杂，先做规划
        if self._is_complex_task(task):
            plan = await self.planner.plan(
                task, [t.name for t in self.tools.list_tools()]
            )
            result = await PlanExecutor(ToolChain(self.tools), self.llm).execute(plan)
        else:
            # 直接用ReAct
            react = ReActAgent(self.llm, self.tools,
                              max_steps=self.config.get("max_steps", 10))
            result = await react.run(task)
        
        # 存储这次执行的经验
        self.memory.store_episode(task, result)
        
        # 记录指标
        self.metrics.record(
            task=task,
            duration=time.time() - start_time,
            steps=result.get("steps", 0),
            status=result.get("status"),
        )
        
        return result
    
    def _is_complex_task(self, task: str) -> bool:
        """判断是否需要先规划再执行"""
        complexity_keywords = [
            "分析", "对比", "评估", "设计", "实现",
            "多步", "完整", "复杂", "调研", "报告",
        ]
        return any(kw in task for kw in complexity_keywords)


class AgentMetrics:
    """Agent可观测性"""
    
    def record(self, **kwargs):
        """记录一次执行指标"""
        # Prometheus metrics / 日志 / 事件
        pass
```

---

## 14.7 阶段练习

### 练习1：工具编排Agent
设计3个以上工具，实现一个能自动选择和执行工具的Agent。

### 练习2：Multi-Agent对话
实现两个Agent的对讨论——一个写代码，一个审查，交替工作。

### 练习3：完整Agent系统
整合规划、执行、记忆、安全等组件，构建一个能处理复杂任务的Agent。

---

> **✅ 阶段完成检查清单：**
> - [ ] 理解了工具注册、权限、编排的设计模式
> - [ ] 掌握了ReAct循环的完整实现
> - [ ] 能设计分层记忆系统
> - [ ] 理解Multi-Agent协作的三种模式
> - [ ] 知道Agent安全防护的五个层次
> - [ ] 完成3个阶段练习
>
> **下一步：** [第15步：AI模型微调](../15-AI模型微调/README.md)
