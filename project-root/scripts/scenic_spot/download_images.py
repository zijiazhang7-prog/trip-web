import pandas as pd
import requests
import os
import re
import time
from urllib.parse import urlparse

# ================= 配置区 =================
CSV_FILE = '北京景点学校1.csv'      # CSV文件名
OUTPUT_DIR = '北京景点图片'         # 图片保存文件夹
REQUEST_DELAY = 1.5                # 请求间隔(秒)，避免触发反爬
# ==========================================

# 创建输出目录
os.makedirs(OUTPUT_DIR, exist_ok=True)

# 读取CSV（常见中文CSV编码为 utf-8-sig 或 gbk，若报错可切换编码）
try:
    df = pd.read_csv(CSV_FILE, encoding='utf-8-sig')
except UnicodeDecodeError:
    df = pd.read_csv(CSV_FILE, encoding='gbk')

# 检查必要列是否存在
if 'image' not in df.columns:
    raise ValueError("CSV文件中未找到 'image' 列，请检查表头。")

def clean_filename(name):
    """清理文件名中的非法字符"""
    return re.sub(r'[\\/*?:"<>|\r\n\t]', '_', str(name))[:100]  # 限制长度防超限

print(f"📂 开始下载，共 {len(df)} 条数据...\n")

success_count, fail_count, skip_count = 0, 0, 0

for idx, row in df.iterrows():
    img_url = row.get('image')
    spot_name = row.get('name', f'未命名_{idx}')
    
    # 跳过空链接
    if pd.isna(img_url) or not str(img_url).strip():
        print(f"[{idx+1}/{len(df)}] ⏭️ 跳过: 无图片URL -> {spot_name}")
        skip_count += 1
        continue

    img_url = str(img_url).strip()
    safe_name = clean_filename(spot_name)
    
    # 自动获取文件后缀，默认 .jpg
    _, ext = os.path.splitext(urlparse(img_url).path)
    ext = ext if ext.lower() in ['.jpg', '.jpeg', '.png', '.gif', '.webp'] else '.jpg'
    
    filename = f"{safe_name}{ext}"
    filepath = os.path.join(OUTPUT_DIR, filename)

    try:
        # 模拟浏览器请求头，防止部分服务器拒绝访问
        headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
        resp = requests.get(img_url, headers=headers, timeout=15)
        resp.raise_for_status()  # 检查HTTP状态码

        with open(filepath, 'wb') as f:
            f.write(resp.content)
        print(f"[{idx+1}/{len(df)}] ✅ 成功: {filename}")
        success_count += 1
    except requests.exceptions.RequestException as e:
        print(f"[{idx+1}/{len(df)}] ❌ 失败: {spot_name} | 原因: {e}")
        fail_count += 1
    except Exception as e:
        print(f"[{idx+1}/{len(df)}] ⚠️ 异常: {e}")
        fail_count += 1

    # 礼貌性延迟
    time.sleep(REQUEST_DELAY)

print("\n" + "="*40)
print("🎉 下载任务完成！")
print(f"✅ 成功: {success_count} | ❌ 失败: {fail_count} | ⏭️ 跳过: {skip_count}")
print(f"📁 保存路径: {os.path.abspath(OUTPUT_DIR)}")