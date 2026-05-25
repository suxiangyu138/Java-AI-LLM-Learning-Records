"""
爬虫练习 - 主入口
示例：使用 requests + BeautifulSoup 爬取网页标题
"""
import requests
from bs4 import BeautifulSoup


def fetch_page_title(url: str) -> str | None:
    """获取网页标题"""
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                      "AppleWebKit/537.36 (KHTML, like Gecko) "
                      "Chrome/120.0.0.0 Safari/537.36"
    }
    try:
        resp = requests.get(url, headers=headers, timeout=10)
        resp.raise_for_status()
        resp.encoding = resp.apparent_encoding
        soup = BeautifulSoup(resp.text, "lxml")
        title = soup.title.string if soup.title else None
        return title
    except requests.RequestException as e:
        print(f"请求失败: {e}")
        return None


def demo_parse():
    """解析 HTML 示例"""
    html = """
    <html>
        <body>
            <div class="movie-list">
                <div class="movie-item">
                    <span class="title">肖申克的救赎</span>
                    <span class="rating">9.7</span>
                </div>
                <div class="movie-item">
                    <span class="title">霸王别姬</span>
                    <span class="rating">9.6</span>
                </div>
            </div>
        </body>
    </html>
    """
    soup = BeautifulSoup(html, "lxml")
    items = soup.select(".movie-item")
    for item in items:
        title = item.select_one(".title").text
        rating = item.select_one(".rating").text
        print(f"  {title} - 评分: {rating}")


if __name__ == "__main__":
    print("=" * 50)
    print("  爬虫练习 - 入门示例")
    print("=" * 50)

    # 示例 1：抓取网页标题
    print("\n[示例 1] 抓取网页标题:")
    title = fetch_page_title("https://www.example.com")
    if title:
        print(f"  标题: {title}")

    # 示例 2：HTML 解析
    print("\n[示例 2] HTML 解析:")
    demo_parse()

    print("\n可以开始练习爬取真实网站数据了！")
