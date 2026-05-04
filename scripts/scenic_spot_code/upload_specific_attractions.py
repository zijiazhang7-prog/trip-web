import csv
import requests
import base64
import time
import os
import urllib.parse
import dotenv
from pathlib import Path

# 加载环境变量
dotenv.load_dotenv()

# ================= 配置区 =================
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")
REPO_OWNER = os.getenv("GITHUB_USERNAME")
REPO_NAME = "trip-web"
BRANCH = "dev"
CSV_FILE = "北京景点学校1.csv"
TARGET_DIR = "scripts/scenic_spot2"  # 目标目录

# 目标景点列表（直接使用了你提供的列表）
TARGET_NAMES = [
    "齐白石旧居纪念馆", "齐白石故居", "齐物潭公园", "白河堡水库", "高君宇烈士墓",
    "龙潭公园龙吟阁", "龙潭西湖公园", "龙潭庙会", "黄寺", "鼓楼",
    "鼓楼西剧场", "龚自珍故居", "鲁迅博物馆", "鸣鹤园", "黑塔公园",
    "高里掌文化休闲园", "黑龙潭龙王庙", "龙门店森林公园", "十三陵·碓臼峪自然风景区", "北京拉斐特城堡酒店",
    "英达生态园温泉度假村", "鹿世界主题园景区", "龙潭涧", "鸿坤美术馆", "黄草湾野郊公园",
    "鸟巢CAIA会展中心", "高碑店古家具街", "鸟巢欢乐冰雪季", "黄渠公园", "鸟巢（国家体育场）",
    "龙山森林公园", "龙仙宫旅游景区", "黄梁根金鱼泉", "黍谷山风景区", "龙云山风景区",
    "雾灵湖（遥桥峪水库）", "黄峪口风景区"
]

API_BASE = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/contents"
HEADERS = {
    "Authorization": f"Bearer {GITHUB_TOKEN}",
    "Accept": "application/vnd.github.v3+json"
}

# ================= 辅助函数 =================
def clean_url(url):
    """清理图片 URL 后缀"""
    if not url: return ""
    return url.split('@base@')[0].strip()

def sanitize_filename(name):
    """清理文件名中的非法字符"""
    invalid_chars = '<>:"/\\|？*'
    for char in invalid_chars:
        name = name.replace(char, '_')
    # 仅保留字母、数字、空格、横杠、下划线、括号
    safe_name = "".join(c for c in name if c.isalnum() or c in (' ', '-', '_', '（', '）', '·')).strip()
    return safe_name[:80]

def load_image_mapping():
    """从 CSV 加载名称到图片 URL 的映射"""
    mapping = {}
    if not os.path.exists(CSV_FILE):
        print(f"❌ 找不到 CSV 文件：{CSV_FILE}")
        return mapping
    
    with open(CSV_FILE, 'r', encoding='utf-8-sig') as f:
        reader = csv.DictReader(f)
        for row in reader:
            name = row.get('name', '').strip()
            img = row.get('image', '').strip()
            if name and img:
                mapping[name] = clean_url(img)
    return mapping

def upload_to_github(name, img_url):
    """上传单张图片到 GitHub"""
    if not img_url:
        print(f"⚠️  跳过 {name}: 未找到图片链接")
        return False

    filename = f"{sanitize_filename(name)}.jpg"
    path = f"{TARGET_DIR}/{filename}"
    
    try:
        # 1. 下载图片到内存
        resp = requests.get(img_url, timeout=15)
        resp.raise_for_status()
        img_bytes = resp.content
        
        # 2. Base64 编码
        content_b64 = base64.b64encode(img_bytes).decode("utf-8")
        
        # 3. 检查文件是否已存在（获取 sha）
        check_url = f"{API_BASE}/{urllib.parse.quote(path, safe='/')}"
        sha = None
        check_resp = requests.get(check_url, headers=HEADERS)
        if check_resp.status_code == 200:
            sha = check_resp.json().get("sha")
            print(f"   🔄 文件已存在，将更新：{filename}")
        
        # 4. 上传/更新
        payload = {
            "message": f"Upload attraction image: {filename}",
            "content": content_b64,
            "branch": BRANCH
        }
        if sha:
            payload["sha"] = sha
            
        put_resp = requests.put(check_url, headers=HEADERS, json=payload)
        if put_resp.status_code in (200, 201):
            print(f"✅ 成功：{filename}")
            return True
        else:
            print(f"❌ 失败：{filename} | {put_resp.json().get('message')}")
            return False
            
    except Exception as e:
        print(f"💥 异常：{name} | {e}")
        return False

# ================= 主流程 =================
def main():
    print(" 开始准备上传特定景点图片...")
    
    # 1. 加载数据
    print("📖 读取 CSV 图片映射...")
    img_map = load_image_mapping()
    print(f"✅ 成功加载 {len(img_map)} 条图片记录")
    
    # 2. 过滤目标数据
    tasks = []
    for name in TARGET_NAMES:
        url = img_map.get(name)
        if url:
            tasks.append((name, url))
        else:
            print(f"⚠️  警告：CSV 中未找到 '{name}' 的图片链接，已跳过")
    
    if not tasks:
        print("❌ 没有找到任何可上传的任务，请检查景点名称是否匹配。")
        return
        
    print(f"\n📋 准备上传 {len(tasks)} 张图片到 {TARGET_DIR}/")
    print("="*50)
    
    # 3. 执行上传
    success = 0
    failed = 0
    for i, (name, url) in enumerate(tasks, 1):
        print(f"[{i}/{len(tasks)}] 处理：{name}")
        if upload_to_github(name, url):
            success += 1
        else:
            failed += 1
        
        # 限速
        time.sleep(1.5)
        
        if i % 5 == 0:
            print(f"--- 进度：成功 {success} / 失败 {failed} ---\n")
            
    # 4. 最终报告
    print("\n" + "="*50)
    print("🎉 上传任务完成！")
    print(f"✅ 成功：{success}")
    print(f"❌ 失败：{failed}")
    print(f"🔗 查看目录：https://github.com/{REPO_OWNER}/{REPO_NAME}/tree/{BRANCH}/{TARGET_DIR}")

if __name__ == "__main__":
    main()