快速学会：主流大模型 API 调用｜统一逻辑 + 极简代码 + 开箱即用
一、核心底层原理（所有大模型通用）
1. 所有厂商大模型，API 格式高度统一，全是：
    - 协议： HTTPS 
    - 请求方式： POST 
    - 数据格式： JSON 
    - 身份认证： API-Key  请求头携带
2. 调用流程（100% 通用）
    plaintext
    构造请求体（模型+上下文+参数）
    → 携带 Authorization 密钥
    → 发送 POST 请求
    → 解析 JSON 回答
 
3. 两种模式
    - 普通问答：一次性返回全部结果
    - 流式输出（Stream）：逐字打字效果，聊天页常用
 
二、通用必备参数（所有模型通用）
参数 作用 
 model  模型名称（gpt-3.5、qwen、glm、deepseek） 
 messages  对话上下文： role(用户/助手)+content(内容)  
 temperature  随机性 0~1，越高越放飞，越低越严谨 
 stream  是否流式输出 
统一消息结构：
json
{"role":"user","content":"你好"},
{"role":"assistant","content":"你好呀"}
 
 
三、全网主流 5 大平台 极简调用（Python）
前提： pip install requests 
1. 阿里云 通义千问（Qwen）
    - 地址： https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation 
    - 密钥：阿里云百炼 APIKEY
    python
    import requests
    url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation"
    headers = {
    "Authorization": "Bearer 你的API_KEY",
    "Content-Type": "application/json"
    }
    body = {
    "model": "qwen-turbo",
    "input": {"messages": [{"role":"user","content":"Java是什么"}]},
    "parameters": {"temperature": 0.7}
    }
    res = requests.post(url, headers=headers, json=body)
    print(res.json()["output"]["text"])
 
 
2. 智谱 AI GLM
    - 地址： https://open.bigmodel.cn/api/paas/v4/chat/completions 
    python
    url = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
    headers = {
    "Authorization": "Bearer 你的API_KEY",
    "Content-Type": "application/json"
    }
    body = {
    "model": "glm-4-flash",
    "messages": [{"role":"user","content":"解释RabbitMQ"}],
    "temperature": 0.7
    }
    res = requests.post(url, headers=headers, json=body)
    print(res.json()["choices"][0]["message"]["content"])
 
 
3. DeepSeek 深度求索
    - 地址： https://api.deepseek.com/v1/chat/completions 
    python
    url = "https://api.deepseek.com/v1/chat/completions"
    body = {
    "model": "deepseek-chat",
    "messages": [{"role":"user","content":"ES倒排索引原理"}]
    }
    res = requests.post(url, headers=headers, json=body)
 
 
4. 百度 文心一言
    - 需要额外鉴权，结构稍特殊，企业用得多
 
5. 兼容 OpenAI 标准（重点⭐）
    现在国内90%大模型全部兼容 OpenAI 格式
    包括：DeepSeek、通义、GLM、豆包、星火、本地 Ollama
    统一依赖：
    bash
    pip install openai
 
统一极简写法（换key+换模型名即可）：
python
from openai import OpenAI
client = OpenAI(
    api_key="你的KEY",
    base_url="对应厂商地址"
)
resp = client.chat.completions.create(
    model="模型名",
    messages=[{"role":"user","content":"介绍Docker"}],
    temperature=0.7
)
print(resp.choices[0].message.content)
 
优势：一套代码，切换所有大模型，只改两行配置。
 
四、本地大模型 API（Ollama）无key、免费
本地部署 Ollama 后，自带 OpenAI 兼容接口
- 默认地址： http://localhost:11434/v1 
- 无需 key
    python
    from openai import OpenAI
    client = OpenAI(
    base_url="http://localhost:11434/v1",
    api_key="ollama"
    )
    res = client.chat.completions.create(
    model="llama3",
    messages=[{"role":"user","content":"解释经济危机"}]
    )
    print(res.choices[0].message.content)
 
 
五、Java 后端调用（你技术栈必用）
核心逻辑不变：HttpPost + JSON 请求体 + Header 带 Token
极简伪代码结构：
java
// 1. 构造请求JSON
// 2. 设置请求头：Content-Type、Authorization
// 3. 发送HTTP请求
// 4. 解析JSON返回结果
 
企业常用：
-  OkHttp  /  RestTemplate  /  WebClient 
- 封装统一工具类，一键切换多模型
 
六、流式输出（打字机效果 核心）
1. 请求加参数： "stream": true 
2. 响应是  text/event-stream  流数据
3. 分段解析、拼接内容
    所有厂商逻辑完全一致，适合做 AI 聊天网页/客户端。
 
七、学习路线&记忆总结
1. 所有大模型 API 格式统一、逻辑统一
2. 优先学「OpenAI 兼容写法」，一通百通
3. 开发顺序：
    - 先调 免费公共模型（GLM-4-Flash、DeepSeek免费版）
    - 再整合 Java 后端
    - 最后对接 RAG 项目（Milvus + 大模型API）
4. 关键三要素：
    请求地址 + API_KEY + messages 对话结构
