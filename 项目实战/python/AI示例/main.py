"""
AI 示例 - 主入口
演示 OpenAI / Anthropic API 调用、Ollama 本地模型调用
"""
import os
from dotenv import load_dotenv

load_dotenv()


def demo_openai():
    """OpenAI API 调用示例"""
    try:
        from openai import OpenAI
        client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": "你是一个有帮助的助手，回答简洁。"},
                {"role": "user", "content": "用一句话解释什么是 RAG？"}
            ],
            temperature=0.7,
            max_tokens=150,
        )
        print(f"\nOpenAI 回复:\n{response.choices[0].message.content}")

    except ImportError:
        print("请先安装 openai: pip install openai")
    except Exception as e:
        print(f"OpenAI 调用失败: {e}")


def demo_anthropic():
    """Anthropic API 调用示例"""
    try:
        from anthropic import Anthropic
        client = Anthropic(api_key=os.getenv("ANTHROPIC_API_KEY"))

        message = client.messages.create(
            model="claude-sonnet-4-6",
            max_tokens=200,
            system="你是一个有帮助的助手，回答简洁。",
            messages=[
                {"role": "user", "content": "用一句话解释什么是 Function Calling？"}
            ]
        )
        print(f"\nAnthropic 回复:\n{message.content[0].text}")

    except ImportError:
        print("请先安装 anthropic: pip install anthropic")
    except Exception as e:
        print(f"Anthropic 调用失败: {e}")


def demo_ollama():
    """Ollama 本地模型调用示例"""
    try:
        import requests

        response = requests.post(
            f"{os.getenv('OLLAMA_BASE_URL', 'http://localhost:11434')}/api/generate",
            json={
                "model": "llama3",
                "prompt": "用一句话解释什么是向量数据库？",
                "stream": False,
            },
            timeout=30,
        )
        if response.status_code == 200:
            print(f"\nOllama 回复:\n{response.json()['response']}")
        else:
            print(f"Ollama 请求失败: HTTP {response.status_code}")

    except ImportError:
        print("请先安装 requests: pip install requests")
    except Exception as e:
        print(f"Ollama 调用失败: {e} (确保 Ollama 已启动且已下载模型)")


if __name__ == "__main__":
    print("=" * 50)
    print("  AI 大模型示例集")
    print("=" * 50)
    print("\n提示: 请在 .env 文件中配置 API Key")
    print("  OPENAI_API_KEY=sk-xxx")
    print("  ANTHROPIC_API_KEY=sk-ant-xxx")
    print("  OLLAMA_BASE_URL=http://localhost:11434")

    print("\n" + "-" * 30)
    demo_openai()

    print("\n" + "-" * 30)
    demo_anthropic()

    print("\n" + "-" * 30)
    demo_ollama()

    print("\n更多示例请查看 src/ 目录。")
