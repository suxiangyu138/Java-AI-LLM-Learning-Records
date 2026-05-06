import requests
import re
import logging
from urllib.parse import urljoin

# 配置日志输出格式，方便查看运行状态
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s: %(message)s')

BASE_URL = 'https://ssr1.scrape.center'

def scrape_page(url):
    """
    发送请求获取网页源代码
    """
    logging.info('开始爬取: %s', url)
    try:
        response = requests.get(url)
        if response.status_code == 200:
            return response.text
        logging.error('获取状态码异常 %s', response.status_code)
    except requests.RequestException as e:
        logging.error('抓取失败: %s', e)
    return None

def parse_html(html):
    """
    使用正则表达式解析网页，提取电影信息
    """
    # 提取电影详情页链接和图片链接
    pattern = re.compile(
        '<a.*?href="(.*?)".*?class="name".*?>(.*?)</a>.*?'  # 提取详情链接和电影名
        '<p.*?class="score.*?">(.*?)</p>',                  # 提取评分
        re.S  # 匹配换行符
    )
    
    items = re.findall(pattern, html)
    
    results = []
    for item in items:
        # urljoin 用于将相对路径拼接成完整的绝对路径
        detail_url = urljoin(BASE_URL, item[0])
        name = item[1].strip()
        score = item[2].strip()
        
        results.append({
            'name': name,
            'score': score,
            'detail_url': detail_url
        })
    return results

def main():
    # 只抓取第一页作为演示，网址为 https://ssr1.scrape.center/page/1
    url = f"{BASE_URL}/page/1"
    
    html = scrape_page(url)
    if html:
        movies = parse_html(html)
        print("\n--- 成功提取到本页电影数据 ---")
        for idx, movie in enumerate(movies, 1):
            print(f"{idx}. 电影名: {movie['name']} | 评分: {movie['score']}")
            print(f"   详情链接: {movie['detail_url']}\n")

if __name__ == '__main__':
    main()