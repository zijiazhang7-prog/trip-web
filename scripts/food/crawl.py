import requests
from bs4 import BeautifulSoup
import time
import random
import pandas as pd
import os
from datetime import datetime

# ============ 配置区域 ============
BASE_URL = "https://s.visitbeijing.com.cn/foods"
OUTPUT_FILE = f"beijing_foods_{datetime.now().strftime('%Y%m%d_%H%M')}.xlsx"
MIN_WAIT, MAX_WAIT = 4, 8          # 请求间隔(秒)
MAX_RETRIES = 3                    # 失败重试次数
REQUEST_TIMEOUT = 15               # 请求超时(秒)
# =================================

def parse_food_item(item):
    """解析单个美食条目"""
    try:
        # 🖼️ 图片
        img_tag = item.find('img')
        img_url = img_tag.get('src', '') if img_tag else ''
        
        # 📛 标题 + 详情页
        title_tag = item.find('h5')
        if not title_tag or not title_tag.find('a'):
            return None
        title = title_tag.find('a').get_text(strip=True)
        detail_url = title_tag.find('a').get('href', '')
        
        # 📝 简介（保留完整内容，不截断）
        desc_tag = item.find('p')
        description = desc_tag.get_text(strip=True) if desc_tag else ''
        
        # 📍 地址
        addr_tag = item.find('div', class_='adress')
        address = addr_tag.get_text(strip=True) if addr_tag else '—'
        
        # 🏷️ 标签
        theme_tag = item.find('div', class_='theme')
        theme = theme_tag.find('span').get_text(strip=True) if theme_tag and theme_tag.find('span') else ''
        
        return {
            '名称': title,
            '简介': description,
            '地址': address,
            '标签': theme,
            '图片URL': img_url,
            '详情页': detail_url,
            '采集时间': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        }
    except Exception as e:
        print(f"    ⚠️ 解析失败: {e}")
        return None

def fetch_page(url, retry=0):
    """请求单页，带重试机制"""
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        'Accept-Language': 'zh-CN,zh;q=0.9',
    }
    
    try:
        response = requests.get(url, headers=headers, timeout=REQUEST_TIMEOUT)
        response.raise_for_status()
        response.encoding = 'utf-8'
        return BeautifulSoup(response.text, 'lxml')
    except Exception as e:
        if retry < MAX_RETRIES:
            print(f"    🔄 请求失败，{retry+1}/{MAX_RETRIES} 重试中... ({e})")
            time.sleep(2 ** retry)  # 指数退避
            return fetch_page(url, retry + 1)
        print(f"    ❌ 请求最终失败: {e}")
        return None

def get_total_pages():
    """获取总页数"""
    soup = fetch_page(BASE_URL)
    if not soup:
        return 1
    page_div = soup.find('div', class_='page')
    if not page_div:
        return 1
    # 查找最大页码数字
    import re
    page_nums = []
    for link in page_div.find_all('a', href=True):
        match = re.search(r'page=(\d+)', link['href'])
        if match:
            page_nums.append(int(match.group(1)))
    return max(page_nums) if page_nums else 1

def crawl_all_foods():
    """主爬取函数"""
    print(f"🎯 开始爬取北京美食数据")
    print(f"📁 输出文件: {OUTPUT_FILE}")
    print(f"⏱️  请求间隔: {MIN_WAIT}-{MAX_WAIT}秒随机")
    print("=" * 70)
    
    # 获取总页数
    total_pages = get_total_pages()
    print(f"📊 检测到共 {total_pages} 页，预计数据量 ~{total_pages * 10} 条\n")
    
    all_data = []
    start_time = time.time()
    
    for page in range(1, total_pages + 1):
        url = f"{BASE_URL}?page={page}"
        print(f"[{page:2d}/{total_pages}] 请求: {url}")
        
        soup = fetch_page(url)
        if not soup:
            continue
            
        items = soup.find_all('li', class_='clearfix')
        page_count = 0
        
        for item in items:
            result = parse_food_item(item)
            if result:
                all_data.append(result)
                page_count += 1
                # 实时显示进度（每10条打印一次）
                if len(all_data) % 10 == 0:
                    print(f"      ✓ 已采集 {len(all_data)} 条")
        
        print(f"      → 本页获取 {page_count} 条，累计 {len(all_data)} 条")
        
        # 最后一页不需要等待
        if page < total_pages:
            wait_time = random.uniform(MIN_WAIT, MAX_WAIT)
            print(f"      ⏳ 等待 {wait_time:.2f}秒...\n")
            time.sleep(wait_time)
    
    # 计算耗时
    elapsed = time.time() - start_time
    hours, remainder = divmod(elapsed, 3600)
    minutes, seconds = divmod(remainder, 60)
    
    # ============ 保存数据 ============
    if all_data:
        df = pd.DataFrame(all_data)
        
        # 调整列顺序
        cols = ['序号', '名称', '标签', '地址', '简介', '图片URL', '详情页', '采集时间']
        df.insert(0, '序号', range(1, len(df) + 1))
        df = df[[c for c in cols if c in df.columns]]
        
        # 保存Excel（自动调整列宽需要openpyxl）
        try:
            df.to_excel(OUTPUT_FILE, index=False, engine='openpyxl')
            print(f"\n✅ 成功保存 {len(df)} 条数据到: {OUTPUT_FILE}")
        except ImportError:
            #  fallback 到csv
            csv_file = OUTPUT_FILE.replace('.xlsx', '.csv')
            df.to_csv(csv_file, index=False, encoding='utf-8-sig')
            print(f"\n⚠️ 未安装openpyxl，已保存为CSV: {csv_file}")
            print("💡 安装 openpyxl 可输出Excel: pip install openpyxl")
        
        # 📊 简单统计
        print(f"\n📈 数据统计:")
        print(f"   • 总条数: {len(df)}")
        if '标签' in df.columns:
            label_counts = df['标签'].value_counts()
            print(f"   • 标签分布:")
            for label, count in label_counts.items():
                print(f"     - {label}: {count}条")
        if '地址' in df.columns:
            has_addr = df['地址'].ne('—').sum()
            print(f"   • 含地址: {has_addr}条 ({has_addr/len(df)*100:.1f}%)")
        
    else:
        print("\n❌ 未采集到任何数据，请检查网络或页面结构")
    
    print(f"\n⏱️  总耗时: {int(hours)}h {int(minutes)}m {seconds:.1f}s")
    print("=" * 70)
    
    return all_data

# ============ 入口 ============
if __name__ == "__main__":
    # 检查依赖
    try:
        import pandas
        import openpyxl  # 用于Excel输出
    except ImportError as e:
        print(f"⚠️ 缺少依赖: {e}")
        print("📦 请运行: pip install requests beautifulsoup4 lxml pandas openpyxl")
        print("🔄 将继续尝试运行（可能无法输出xlsx）...\n")
    
    crawl_all_foods()