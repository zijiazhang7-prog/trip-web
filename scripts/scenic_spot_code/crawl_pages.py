import requests
from bs4 import BeautifulSoup
import time
import csv
import random
import os

# ================= 配置区 =================
BASE_URL = "https://s.visitbeijing.com.cn/attractions"
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
    "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
    "Connection": "keep-alive"
}
FIELDNAMES = ['name', 'detail_url', 'desc', 'address', 'tags', 'image']

# ================= 解析函数 =================
def parse_attraction(html):
    """解析景点列表页 HTML"""
    soup = BeautifulSoup(html, 'html.parser')
    attractions = []
    
    # 精确匹配每个景点卡片
    items = soup.select('ul#listItems > li.clearfix')
    
    for item in items:
        try:
            # 1. 名称 & 详情页链接
            name_tag = item.select_one('h5 a')
            name = name_tag.get_text(strip=True) if name_tag else ''
            detail_url = name_tag['href'] if name_tag and name_tag.has_attr('href') else ''
            
            # ✅ 修复 desc 为空：直接在 .info 容器内找 p 标签（原写法 div.info fl > p 语法错误）
            desc_tag = item.select_one('div.info > p')
            desc = desc_tag.get_text(strip=True) if desc_tag else ''
            
            # 2. 地址（️ 注意网站源码 class 拼写为 adress，不是 address）
            addr_tag = item.select_one('div.adress')
            address = addr_tag.get_text(strip=True) if addr_tag else ''
            
            # 3. 标签（可能有多个，用 | 拼接）
            tags = ' | '.join([
                t.get_text(strip=True) 
                for t in item.select('div.theme span') 
                if t.get_text(strip=True)
            ])
            
            # 4. 图片
            img_tag = item.select_one('a.pic img')
            img_url = img_tag['src'] if img_tag and img_tag.has_attr('src') else ''
            
            attractions.append({
                'name': name,
                'detail_url': detail_url,
                'desc': desc,
                'address': address,
                'tags': tags,
                'image': img_url
            })
        except Exception as e:
            print(f"⚠️ 解析单条数据失败: {e}")
            continue
            
    return attractions

# ================= 爬虫主函数 =================
def crawl_pages(start=1, end=3, output_file='beijing_attractions_all.csv'):
    """爬取指定页码范围，支持追加写入与礼貌延迟"""
    all_data = []
    is_new_file = not os.path.exists(output_file) or os.path.getsize(output_file) == 0
    
    if not is_new_file:
        print(f"📁 检测到已存在文件 {output_file}，将以追加模式写入")
    else:
        print(f"🆕 创建新文件 {output_file}")

    for page in range(start, end + 1):
        # 该网站分页必须携带 star_rating=10 参数才能正常翻页
        url = f"{BASE_URL}?star_rating=10&page={page}"
        try:
            print(f" 请求第 {page} 页...")
            resp = requests.get(url, headers=HEADERS, timeout=15)
            resp.encoding = 'utf-8'
            
            if resp.status_code == 200 and 'listItems' in resp.text:
                data = parse_attraction(resp.text)
                if data:
                    # 追加写入 CSV
                    with open(output_file, 'a', encoding='utf-8-sig', newline='') as f:
                        writer = csv.DictWriter(f, fieldnames=FIELDNAMES)
                        if is_new_file:
                            writer.writeheader()
                            is_new_file = False  # 后续页不再重复写表头
                        writer.writerows(data)
                    
                    all_data.extend(data)
                    print(f"✅ 第{page}页 | 成功获取 {len(data)} 条 | 最新: {data[-1]['name']}")
                else:
                    print(f"️ 第{page}页 | 未解析到数据，可能已到底或结构变化")
                    
                # 🕒 礼貌延迟：4 ~ 8 秒随机间隔，防封 IP
                time.sleep(random.uniform(4.0, 8.0))
            else:
                print(f"❌ 第{page}页 | 请求异常 (状态码: {resp.status_code})，5秒后重试...")
                time.sleep(5)
                
        except Exception as e:
            print(f"💥 第{page}页 | 发生异常: {e}")
            time.sleep(5)

    print(f"\n🎉 任务完成！共获取 {len(all_data)} 条数据，已保存至: {output_file}")
    return all_data

# ================= 执行入口 =================
if __name__ == "__main__":
    # print("🚀 开始爬取测试（前3页）...")
    # crawl_pages(start=1, end=3, output_file='beijing_attractions_test.csv')
    
    # 💡 测试无误后，取消下方注释即可全量爬取（约211页，需10~20分钟）
    print("\n📥 开始全量爬取（第1页至第211页）...")
    crawl_pages(start=1, end=211, output_file='beijing_attractions_all.csv')