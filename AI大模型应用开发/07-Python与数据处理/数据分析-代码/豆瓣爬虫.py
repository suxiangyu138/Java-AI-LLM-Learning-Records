import requests
from bs4 import BeautifulSoup

# 1. 准备请求头（伪装成浏览器）
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
}

# 2. 发送请求
url = 'https://movie.douban.com/top250'
response = requests.get(url, headers=headers)

# 3. 解析网页
soup = BeautifulSoup(response.text, 'html.parser')

# 4. 提取数据
items = soup.find_all('div', class_='item')
for item in items:
    # 提取电影名
    title = item.find('span', class_='title').text
    # 提取评分
    rating = item.find('span', class_='rating_num').text
    print(f"电影: {title} | 评分: {rating}")