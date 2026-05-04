import requests
from bs4 import BeautifulSoup
import time
import random

def parse_food_page(url, max_items=30):
    """解析单页美食数据，达到目标数量即停止"""
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    }
    
    try:
        response = requests.get(url, headers=headers, timeout=15)
        response.raise_for_status()
        response.encoding = 'utf-8'
        soup = BeautifulSoup(response.text, 'lxml')
        
        results = []
        items = soup.find_all('li', class_='clearfix')
        
        for item in items:
            if len(results) >= max_items:
                break
            try:
                # 🖼️ 图片
                img_tag = item.find('img')
                img_url = img_tag.get('src', '') if img_tag else ''
                
                # 📛 标题 + 详情页
                title_tag = item.find('h5')
                if title_tag and title_tag.find('a'):
                    title = title_tag.find('a').get_text(strip=True)
                    detail_url = title_tag.find('a').get('href', '')
                else:
                    continue  # 跳过无标题的无效项
                
                # 📝 简介
                desc_tag = item.find('p')
                description = desc_tag.get_text(strip=True) if desc_tag else ''
                
                # 📍 地址
                addr_tag = item.find('div', class_='adress')
                address = addr_tag.get_text(strip=True) if addr_tag else '—'
                
                # 🏷️ 标签
                theme_tag = item.find('div', class_='theme')
                theme = theme_tag.find('span').get_text(strip=True) if theme_tag and theme_tag.find('span') else ''
                
                results.append({
                    '序号': len(results) + 1,
                    '名称': title,
                    '简介': description[:50] + '...' if len(description) > 50 else description,
                    '地址': address,
                    '标签': theme,
                    '详情页': detail_url
                })
                
            except Exception as e:
                print(f"  ⚠️ 解析单条失败: {e}")
                continue
                
        return results
        
    except Exception as e:
        print(f"❌ 请求页面失败: {e}")
        return []

# ============ 🚀 主程序 ============
if __name__ == "__main__":
    base_url = "https://s.visitbeijing.com.cn/foods"
    collected = []
    page = 1
    
    print("🎯 开始测试爬取，目标：30条数据，间隔：4-8秒随机")
    print("=" * 70)
    
    while len(collected) < 30:
        url = f"{base_url}?page={page}"
        print(f"\n📄 请求第 {page} 页 → {url}")
        
        new_items = parse_food_page(url, max_items=30 - len(collected))
        
        for item in new_items:
            collected.append(item)
            # 🖨️ 实时打印每条数据
            print(f"\n[{item['序号']}] {item['名称']}")
            print(f"   🏷️  {item['标签']}")
            print(f"   📍  {item['地址']}")
            print(f"   📝  {item['简介']}")
            print(f"   🔗  {item['详情页']}")
            print("   " + "-" * 60)
            
            # ✅ 达到30条立即停止
            if len(collected) >= 30:
                break
        
        # ⏱️ 随机间隔 4-8 秒（礼貌爬取）
        if len(collected) < 30:
            wait_time = random.uniform(4, 8)
            print(f"⏳ 等待 {wait_time:.2f} 秒...")
            time.sleep(wait_time)
            page += 1
    
    # 📊 测试完成汇总
    print("\n" + "=" * 70)
    print(f"✅ 测试完成！共采集 {len(collected)} 条数据")
    print(f"📄 涉及页数：1 ~ {page}")
    
    # 🔍 简单统计
    if collected:
        labels = [item['标签'] for item in collected if item['标签']]
        from collections import Counter
        print(f"🏷️ 标签分布：{dict(Counter(labels))}")