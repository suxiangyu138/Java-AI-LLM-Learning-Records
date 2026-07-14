# 第13步：实时交互与多模态

> **阶段目标：** 掌握流式响应(Streaming)的实现，理解并应用多模态输入（图像、音频）的处理流程  
> **预计学时：** 1-2周（每天3-4小时）  
> **前置要求：** FastAPI后端开发 + API调用基础  

---

## 📚 目录

- [13.1 流式响应的完整实现](#131-流式响应的完整实现)
- [13.2 WebSocket实时通信](#132-websocket实时通信)
- [13.3 多模态AI基础](#133-多模态ai基础)
- [13.4 图像理解与生成](#134-图像理解与生成)
- [13.5 语音交互](#135-语音交互)
- [13.6 多模态应用架构](#136-多模态应用架构)
- [13.7 阶段练习](#137-阶段练习)

---

## 13.1 流式响应的完整实现

### 13.1.1 为什么需要Streaming？

```
非流式 (传统):
  用户: "写一篇关于AI的500字文章"
  ───────── 等待5-30秒 ─────────
  助手: [一次性显示完整文章]
  体验: ⏳ 等待焦虑，不知道模型在做什么

流式 (Streaming):
  用户: "写一篇关于AI的500字文章"
  助手: 人 → 人工 → 人工智能 → 人工智能是... (逐字显示)
  体验: ✅ 即时反馈，感知速度快3-5倍
```

### 13.1.2 FastAPI SSE流式实现

```python
from fastapi import FastAPI, Request
from fastapi.responses import StreamingResponse
import asyncio
import json
import time

app = FastAPI()

async def stream_llm_response(messages: list, model: str = "gpt-4o", 
                               **kwargs) -> AsyncGenerator[str, None]:
    """
    流式调用LLM并生成SSE事件流
    
    SSE格式:
    data: {"token": "你好"}
    data: {"token": "，"}
    ...
    data: [DONE]
    """
    client = openai.AsyncOpenAI()  # 异步客户端
    
    stream = await client.chat.completions.create(
        model=model,
        messages=messages,
        stream=True,
        stream_options={"include_usage": True},
        **kwargs,
    )
    
    async for chunk in stream:
        # 提取delta
        if chunk.choices and chunk.choices[0].delta.content:
            token = chunk.choices[0].delta.content
            event_data = json.dumps({"token": token}, ensure_ascii=False)
            yield f"data: {event_data}\n\n"
        
        # 最后的usage信息
        if hasattr(chunk, 'usage') and chunk.usage:
            usage_data = json.dumps({
                "type": "usage",
                "prompt_tokens": chunk.usage.prompt_tokens,
                "completion_tokens": chunk.usage.completion_tokens,
            })
            yield f"data: {usage_data}\n\n"
    
    # 结束标记
    yield "data: [DONE]\n\n"


@app.post("/chat/stream")
async def chat_stream_endpoint(request: Request):
    """流式聊天端点"""
    body = await request.json()
    messages = body.get("messages", [])
    
    return StreamingResponse(
        stream_llm_response(messages, model=body.get("model", "gpt-4o-mini")),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",  # 关键：禁用Nginx缓冲
            "Access-Control-Allow-Origin": "*",
        },
    )
```

### 13.1.3 前端Streaming消费

```javascript
// ========== 最佳实践：ReadableStream API ==========
async function streamChat(messages) {
    const response = await fetch('http://localhost:8000/chat/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ messages }),
    });
    
    // 关键：获取ReadableStream
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let fullText = '';
    
    // 创建AbortController用于停止生成
    const abortController = new AbortController();
    
    while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        
        // SSE可能在一个chunk中包含多个data事件
        const chunk = decoder.decode(value, { stream: true });
        const lines = chunk.split('\n');
        
        for (const line of lines) {
            if (line.startsWith('data: ')) {
                const data = line.slice(6).trim();
                if (data === '[DONE]') break;
                
                try {
                    const parsed = JSON.parse(data);
                    if (parsed.token) {
                        fullText += parsed.token;
                        // 更新UI
                        updateAssistantMessage(fullText);
                    }
                } catch (e) {
                    // 忽略非JSON行
                }
            }
        }
    }
    
    return fullText;
}

// 停止生成
function stopGeneration() {
    abortController.abort();
}
```

### 13.1.4 生产级Streaming架构

```python
class StreamingManager:
    """
    生产级流式管理器
    
    特性：
    - 多路复用：同时服务多个客户端
    - 背压处理：慢客户端不拖慢快客户端
    - 超时控制：长时间无输出自动断开
    - 错误传播：LLM错误即时通知客户端
    """
    
    def __init__(self):
        self.active_streams: Dict[str, asyncio.Queue] = {}
        self.timeout = 300  # 5分钟超时
    
    async def create_stream(self, session_id: str) -> AsyncGenerator:
        """为一个会话创建流"""
        queue = asyncio.Queue()
        self.active_streams[session_id] = queue
        
        try:
            while True:
                try:
                    data = await asyncio.wait_for(
                        queue.get(), timeout=self.timeout
                    )
                    if data == "__END__":
                        break
                    yield f"data: {json.dumps(data, ensure_ascii=False)}\n\n"
                except asyncio.TimeoutError:
                    yield f"data: {json.dumps({'error': '流超时'})}\n\n"
                    break
        finally:
            self.active_streams.pop(session_id, None)
    
    async def push_token(self, session_id: str, token: str):
        """推送一个Token到指定会话"""
        if session_id in self.active_streams:
            await self.active_streams[session_id].put({"token": token})
    
    async def push_error(self, session_id: str, error: str):
        """推送错误"""
        if session_id in self.active_streams:
            await self.active_streams[session_id].put({"error": error})
    
    async def end_stream(self, session_id: str):
        """结束流"""
        if session_id in self.active_streams:
            await self.active_streams[session_id].put("__END__")


# 使用示例
stream_mgr = StreamingManager()

@app.post("/chat/stream/{session_id}")
async def managed_stream(session_id: str):
    """管理型流式端点"""
    return StreamingResponse(
        stream_mgr.create_stream(session_id),
        media_type="text/event-stream",
    )

async def process_stream(session_id: str, messages: list):
    """后端处理（在Celery或后台任务中运行）"""
    async for chunk in llm_stream:
        await stream_mgr.push_token(session_id, chunk)
    await stream_mgr.end_stream(session_id)
```

---

## 13.2 WebSocket实时通信

### 13.2.1 WebSocket vs SSE

```
SSE (Server-Sent Events):
├── 单向：服务器 → 客户端
├── 基于HTTP：穿透防火墙/代理容易
├── 自动重连：浏览器原生支持
├── 适合：Chat流式输出、通知推送
└── 不适合：需要客户端频繁发数据的场景

WebSocket:
├── 双向：客户端 ↔ 服务器
├── 独立协议：ws:// / wss://
├── 低延迟：全双工通信
├── 适合：实时对话、协作编辑、Agent交互
└── 不适合：简单的单向数据流
```

### 13.2.2 FastAPI WebSocket实现

```python
from fastapi import WebSocket, WebSocketDisconnect
from typing import Dict

class ConnectionManager:
    """WebSocket连接管理器"""
    
    def __init__(self):
        self.active_connections: Dict[str, WebSocket] = {}
    
    async def connect(self, websocket: WebSocket, client_id: str):
        await websocket.accept()
        self.active_connections[client_id] = websocket
    
    def disconnect(self, client_id: str):
        self.active_connections.pop(client_id, None)
    
    async def send_message(self, client_id: str, message: dict):
        """发送消息给指定客户端"""
        if client_id in self.active_connections:
            await self.active_connections[client_id].send_json(message)
    
    async def broadcast(self, message: dict):
        """广播给所有客户端"""
        for ws in self.active_connections.values():
            await ws.send_json(message)


manager = ConnectionManager()

@app.websocket("/ws/chat/{client_id}")
async def websocket_chat(websocket: WebSocket, client_id: str):
    """WebSocket聊天端点"""
    await manager.connect(websocket, client_id)
    
    # 对话历史（服务端维护）
    conversation = []
    
    try:
        while True:
            # 接收客户端消息
            data = await websocket.receive_json()
            user_message = data.get("message", "")
            
            # 添加到对话历史
            conversation.append({"role": "user", "content": user_message})
            
            # 发送"正在输入"状态
            await manager.send_message(client_id, {
                "type": "status",
                "status": "thinking",
            })
            
            # 流式生成（边生成边发送）
            stream = await openai_client.chat.completions.create(
                model="gpt-4o",
                messages=conversation,
                stream=True,
            )
            
            full_response = ""
            async for chunk in stream:
                if chunk.choices[0].delta.content:
                    token = chunk.choices[0].delta.content
                    full_response += token
                    
                    # 实时发送每个Token
                    await manager.send_message(client_id, {
                        "type": "token",
                        "content": token,
                    })
            
            # 发送完成信号
            await manager.send_message(client_id, {
                "type": "done",
                "full_response": full_response,
            })
            
            # 更新对话历史
            conversation.append({"role": "assistant", "content": full_response})
            
            # WebSocket特有的：支持"停止生成"
            # 可以检查是否有新消息到达来中断生成
            
    except WebSocketDisconnect:
        manager.disconnect(client_id)
    except Exception as e:
        await manager.send_message(client_id, {
            "type": "error",
            "message": str(e),
        })
        manager.disconnect(client_id)
```

---

## 13.3 多模态AI基础

### 13.3.1 多模态全景

```
多模态AI = 处理多种输入/输出类型的AI

输入模态：
├── 文本 (Text)         ← 我们一直在用的
├── 图像 (Image)        ← GPT-4V, Claude Vision
├── 音频 (Audio)        ← Whisper, GPT-4o语音
├── 视频 (Video)        ← 帧序列 + 音频
└── 代码/结构化数据      ← 特殊文本

输出模态：
├── 文本
├── 图像 (DALL-E, Midjourney, Stable Diffusion)
├── 音频 (TTS, ElevenLabs)
└── 代码/JSON

当前趋势：
GPT-4o / Claude 4 / Gemini → "原生多模态"
模型内部统一处理所有模态，而非分开处理
```

### 13.3.2 OpenAI多模态API

```python
# ========== 图像理解 (Vision) ==========
import base64

def encode_image(image_path: str) -> str:
    """将图片编码为base64"""
    with open(image_path, "rb") as f:
        return base64.b64encode(f.read()).decode("utf-8")

# 图像分析
response = client.chat.completions.create(
    model="gpt-4o",  # 支持视觉的模型
    messages=[
        {
            "role": "user",
            "content": [
                {"type": "text", "text": "这张图片里有什么？请详细描述。"},
                {
                    "type": "image_url",
                    "image_url": {
                        "url": f"data:image/jpeg;base64,{encode_image('photo.jpg')}",
                        "detail": "high",  # low/high/auto
                    },
                },
            ],
        }
    ],
    max_tokens=500,
)

print(response.choices[0].message.content)

# ========== 多图对比 ==========
response = client.chat.completions.create(
    model="gpt-4o",
    messages=[{
        "role": "user",
        "content": [
            {"type": "text", "text": "比较这两张图片的差异"},
            {"type": "image_url", "image_url": {"url": f"data:image/png;base64,{img1}"}},
            {"type": "image_url", "image_url": {"url": f"data:image/png;base64,{img2}"}},
        ],
    }],
)
```

### 13.3.3 Claude Vision API

```python
import anthropic

client = anthropic.Anthropic()

with open("document.png", "rb") as f:
    image_data = base64.b64encode(f.read()).decode("utf-8")

response = client.messages.create(
    model="claude-sonnet-4-6",
    max_tokens=1024,
    messages=[{
        "role": "user",
        "content": [
            {
                "type": "image",
                "source": {
                    "type": "base64",
                    "media_type": "image/png",
                    "data": image_data,
                },
            },
            {
                "type": "text",
                "text": "请识别这张文档的内容，并以JSON格式提取关键信息。",
            },
        ],
    }],
)

print(response.content[0].text)

# Claude Vision的优势:
# - 超长上下文：可以分析200页以上的文档
# - 高精度OCR：对表格、手写字识别准确
# - 安全合规：适合处理敏感文档
```

---

## 13.4 图像理解与生成

### 13.4.1 图像分析完整Pipeline

```python
class ImageAnalysisPipeline:
    """图像分析完整管线"""
    
    def __init__(self, vision_client):
        self.client = vision_client
    
    async def analyze(self, image_data: bytes, 
                      analysis_type: str = "general") -> dict:
        """
        图像分析类型：
        - general: 通用描述
        - ocr: 文字提取
        - object: 物体识别
        - scene: 场景理解
        - document: 文档分析
        """
        
        prompts = {
            "general": "请详细描述这张图片的内容。",
            "ocr": "请提取图片中的所有文字，保持原始格式。",
            "object": "列出图片中的所有物体，说明它们的位置关系。",
            "scene": "描述场景的类型、光线、氛围，以及可能的拍摄地点和时间。",
            "document": """
            请分析这份文档：
            1. 文档类型（发票/合同/报告/...）
            2. 关键信息提取
            3. 异常或需要关注的点
            请以JSON格式返回。
            """,
        }
        
        prompt = prompts.get(analysis_type, prompts["general"])
        
        result = await self._call_vision(image_data, prompt)
        
        if analysis_type == "document":
            # 尝试解析为JSON
            try:
                return json.loads(result)
            except:
                return {"raw": result}
        
        return {"description": result}
    
    async def compare_images(self, img1: bytes, img2: bytes) -> dict:
        """对比两张图片"""
        prompt = """
        请对比这两张图片，从以下维度分析：
        1. 相同点和不同点
        2. 如果有差异，具体描述
        3. 可能的关联关系
        """
        result = await self._call_vision_multi([img1, img2], prompt)
        return {"comparison": result}
```

### 13.4.2 DALL-E图像生成

```python
# ========== 文生图 ==========
response = client.images.generate(
    model="dall-e-3",
    prompt="一只穿着宇航服的柴犬在月球上行走，数字艺术风格，高质量",
    size="1024x1024",
    quality="hd",               # standard/hd
    style="vivid",              # vivid/natural
    n=1,
)

image_url = response.data[0].url
revised_prompt = response.data[0].revised_prompt  # DALL-E优化后的Prompt

print(f"图片URL: {image_url}")
print(f"优化后的Prompt: {revised_prompt}")

# ========== 图生图变体 ==========
response = client.images.create_variation(
    image=open("original.png", "rb"),
    n=2,
    size="1024x1024",
)
```

---

## 13.5 语音交互

### 13.5.1 Whisper语音识别

```python
# ========== OpenAI Whisper API ==========
def transcribe_audio(audio_path: str, language: str = "zh") -> str:
    """语音转文字"""
    with open(audio_path, "rb") as f:
        transcript = client.audio.transcriptions.create(
            model="whisper-1",
            file=f,
            language=language,           # 自动检测: None
            prompt="这是一段会议录音",    # 上下文提示（改善专业术语识别）
            response_format="text",      # text/json/verbose_json/srt/vtt
            temperature=0,               # 0=确定性，适合转录
        )
    return transcript

# ========== 本地Whisper（推荐）==========
import whisper

model = whisper.load_model("medium")  # tiny/base/small/medium/large

# 转录
result = model.transcribe(
    "meeting.mp3",
    language="zh",
    task="transcribe",     # transcribe/translate
    verbose=False,
)

print(f"原文: {result['text']}")
print(f"语言: {result['language']}")

# 带时间戳的分段结果
for segment in result['segments']:
    print(f"[{segment['start']:.1f}s - {segment['end']:.1f}s] {segment['text']}")
```

### 13.5.2 TTS文字转语音

```python
# ========== OpenAI TTS ==========
response = client.audio.speech.create(
    model="tts-1",              # tts-1(快)/tts-1-hd(高质量)
    voice="alloy",              # alloy/echo/fable/nova/onyx/shimmer
    input="你好！欢迎使用AI语音助手。今天我能为你做些什么？",
    speed=1.0,                  # 0.25-4.0
    response_format="mp3",      # mp3/opus/aac/flac
)

# 保存音频
response.stream_to_file("output.mp3")

# ========== 实时语音对话架构 ==========
"""
语音对话Pipeline:

用户说话 → 录音 → Whisper(语音→文字) → LLM(生成回复)
                                               ↓
用户听到 ← 播放 ← TTS(文字→语音) ←─────────────┘

延迟优化：
1. 使用本地Whisper（减少网络延迟）
2. 流式TTS（边生成边播放）
3. 预加载常用回复的音频缓存
"""
```

---

## 13.6 多模态应用架构

### 13.6.1 统一多模态处理层

```python
from enum import Enum
from dataclasses import dataclass
from typing import Union, List, Optional

class Modality(Enum):
    TEXT = "text"
    IMAGE = "image"
    AUDIO = "audio"
    VIDEO = "video"

@dataclass
class MultimodalInput:
    """多模态输入"""
    text: Optional[str] = None
    images: List[bytes] = None
    audio: Optional[bytes] = None

@dataclass
class MultimodalOutput:
    """多模态输出"""
    text: Optional[str] = None
    images: List[str] = None    # URL或base64
    audio: Optional[bytes] = None

class MultimodalProcessor:
    """
    多模态处理器
    
    设计原则：
    - 每种模态有独立的处理器
    - 通过统一接口组合
    - 支持模态之间的转换
    """
    
    def __init__(self):
        self.text_processor = LLMProcessor()
        self.vision_processor = VisionProcessor()
        self.audio_processor = AudioProcessor()
    
    async def process(self, input_data: MultimodalInput) -> MultimodalOutput:
        """处理多模态输入"""
        tasks = []
        
        if input_data.text:
            tasks.append(("text", self.text_processor.process(input_data.text)))
        if input_data.images:
            for img in input_data.images:
                tasks.append(("image", self.vision_processor.analyze(img)))
        if input_data.audio:
            tasks.append(("audio", self.audio_processor.transcribe(input_data.audio)))
        
        # 并行处理所有模态
        results = await asyncio.gather(*[t[1] for t in tasks])
        
        # 融合结果
        return self._fuse_results(results)
    
    def _fuse_results(self, results: list) -> MultimodalOutput:
        """融合多模态处理结果"""
        # 将不同模态的分析结果组合成统一回答
        combined_text = "\n".join([
            f"[{modality}分析] {content}" 
            for modality, content in results
        ])
        
        return MultimodalOutput(text=combined_text)
```

### 13.6.2 多模态Chat UI架构

```
多模态聊天应用架构：

┌────────────────────────────────────────────────┐
│                 前端 (Vue/React)                │
│  ┌──────────┐ ┌──────────┐ ┌───────────────┐  │
│  │ 文字输入  │ │ 图片上传  │ │ 语音按钮      │  │
│  │          │ │ 拖拽/粘贴│ │ 按住说话/松开│  │
│  └──────────┘ └──────────┘ └───────────────┘  │
│  ┌──────────────────────────────────────────┐  │
│  │          多模态消息列表                    │  │
│  │  [文字消息] [图片消息] [语音消息]          │  │
│  └──────────────────────────────────────────┘  │
└────────────────────┬───────────────────────────┘
                     ↓ WebSocket
┌────────────────────────────────────────────────┐
│              API Gateway                        │
│  路由：/upload → 文件服务                       │
│       /chat → WebSocket → 多模态处理            │
└────────────────────┬───────────────────────────┘
                     ↓
┌────────────────────────────────────────────────┐
│           多模态处理服务                         │
│  Input → 模态识别 → 分别处理 → 融合 → Output    │
└────────────────────────────────────────────────┘
```

---

## 13.7 阶段练习

### 练习1：流式聊天应用
实现一个完整的流式聊天应用，支持开始/停止生成，显示Token速度。

### 练习2：图片分析工具
做一个上传图片→AI分析→返回结果的工具，支持多种分析模式（描述/OCR/物体识别）。

### 练习3：语音对话Demo
用Whisper+TTS实现一个基础的语音对话流程。

---

> **✅ 阶段完成检查清单：**
> - [ ] 掌握了SSE和WebSocket两种实时通信方式
> - [ ] 能实现完整的流式响应（前端+后端）
> - [ ] 会用GPT-4V/Claude Vision分析图像
> - [ ] 了解Whisper语音识别和TTS的基本使用
> - [ ] 理解多模态应用的架构模式
> - [ ] 完成3个阶段练习
>
> **下一步：** [第14步：复杂AI Agent实战](../14-复杂AI-Agent实战/README.md)
