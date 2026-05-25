import requests
from bs4 import BeautifulSoup

def simple_spider(url):
    # 更完整的请求头，模拟真实浏览器
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
    try:
        # 发送网络请求
        res = requests.get(url, headers=headers, timeout=10)
        res.encoding = "utf-8"
        
        # 解析网页
        soup = BeautifulSoup(res.text, "html.parser")
        
        # 1. 获取网页标题（加了安全判断）
        title_tag = soup.find("title")
        if title_tag:
            title = title_tag.text
            print("网页标题：", title)
        else:
            print("未找到网页标题")
        print("-" * 50)
        
        # 2. 提取所有超链接
        all_a = soup.find_all("a")
        for a in all_a:
            link = a.get("href")
            text = a.get_text(strip=True)
            if link and text:
                print(f"文字：{text} | 链接：{link}")
                
    except Exception as e:
        print("爬取失败：", e)

if __name__ == "__main__":
    target_url = "https://www.taobao.com?"
    simple_spider(target_url)