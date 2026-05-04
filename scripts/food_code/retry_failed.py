import csv
import requests
import base64
import time
import os
from urllib.parse import quote
import dotenv

dotenv.load_dotenv()

# ================= 配置 =================
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")
REPO_OWNER = os.getenv("GITHUB_USERNAME")
REPO_NAME = "trip-web"
BRANCH = "dev"
UPLOAD_DIR = "scripts/food"
FAILED_LOG = "failed_items.csv"  # 记录失败项的文件

API_BASE = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/contents"
HEADERS = {
    "Authorization": f"Bearer {GITHUB_TOKEN}",
    "Accept": "application/vnd.github.v3+json",
    "User-Agent": "food-retry/1.0"
}

def get_safe_filename(name):
    safe = "".join(c for c in name if c.isalnum() or c in (' ', '-', '_', '·', '（', '）'))
    return safe.strip().replace(' ', '_')[:50]

def upload_single(name, img_url, max_retries=3):
    """单张图片上传，带重试"""
    safe_name = get_safe_filename(name)
    ext = os.path.splitext(img_url.split('?')[0])[-1].lower()
    if ext not in ['.jpg', '.jpeg', '.png', '.gif', '.webp']:
        ext = '.jpg'
    filename = f"{safe_name}{ext}"
    path = f"{UPLOAD_DIR}/{filename}"
    
    for attempt in range(max_retries):
        try:
            # 下载
            resp = requests.get(img_url, timeout=25)
            resp.raise_for_status()
            img_bytes = resp.content
            
            # Base64
            content_b64 = base64.b64encode(img_bytes).decode("utf-8")
            
            # 检查是否存在
            check_url = f"{API_BASE}/{quote(path, safe='')}"
            sha = None
            check_resp = requests.get(check_url, headers=HEADERS, timeout=10)
            if check_resp.status_code == 200:
                sha = check_resp.json().get("sha")
            
            # 上传
            payload = {
                "message": f"Retry add: {filename}",
                "content": content_b64,
                "branch": BRANCH
            }
            if sha:
                payload["sha"] = sha
            
            put_resp = requests.put(check_url, headers=HEADERS, json=payload, timeout=30)
            if put_resp.status_code in (200, 201):
                print(f"✅ 重试成功: {name}")
                return True
            else:
                print(f"❌ GitHub返回错误: {put_resp.json().get('message')}")
                
        except Exception as e:
            wait = 2 ** attempt
            print(f"⚠️ 第{attempt+1}次失败，{wait}秒后重试... ({type(e).__name__})")
            time.sleep(wait)
    
    print(f"💥 最终失败: {name}")
    return False

def main():
    # 手动录入失败项（根据您提供的错误）
    failed_items = [
        {"名称": "致美楼", "图片URL": "https://r1.visitbeijing.com.cn/vbj-s/2016/1025/20161025012746679.png"},
        {"名称": "奶酪魏", "图片URL": "https://r1.visitbeijing.com.cn/vbj-s/2013/1118/20131118111740506.jpg"},
    ]
    
    print(f"🔄 开始重试 {len(failed_items)} 张失败图片...\n")
    
    success = 0
    for item in failed_items:
        name = item['名称']
        img_url = item['图片URL']
        print(f"[{success+1}/{len(failed_items)}] 重试: {name}")
        
        if upload_single(name, img_url):
            success += 1
        else:
            # 记录仍失败的项
            with open(FAILED_LOG, 'a', encoding='utf-8-sig', newline='') as f:
                writer = csv.DictWriter(f, fieldnames=['名称', '图片URL'])
                if f.tell() == 0:
                    writer.writeheader()
                writer.writerow(item)
        
        # 间隔2秒，避免触发限流
        time.sleep(2)
    
    print(f"\n🎯 重试完成: 成功 {success}/{len(failed_items)}")
    if success < len(failed_items):
        print(f"📝 仍失败的项已记录到: {FAILED_LOG}")

if __name__ == "__main__":
    main()