import csv
import requests
import base64
import time
import os
from urllib.parse import quote
import dotenv

dotenv.load_dotenv()

# ================= 配置区 =================
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")      # GitHub Personal Access Token
REPO_OWNER = os.getenv("GITHUB_USERNAME")     # GitHub 用户名
REPO_NAME = "trip-web"                        # 仓库名
BRANCH = "dev"                                # 分支名
CSV_FILE = "北京美食1.csv"                    # 数据文件
UPLOAD_DIR = "scripts/food"                   # ✅ 仓库内图片保存路径

API_BASE = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/contents"
HEADERS = {
    "Authorization": f"Bearer {GITHUB_TOKEN}",
    "Accept": "application/vnd.github.v3+json",
    "User-Agent": "food-image-uploader/1.0"
}

def get_safe_filename(name):
    """生成安全的文件名，避免特殊字符"""
    safe = "".join(c for c in name if c.isalnum() or c in (' ', '-', '_', '·', '（', '）'))
    return safe.strip().replace(' ', '_')[:50]  # 限制长度，避免路径过长

def upload_image_to_github(name, img_url, dry_run=False):
    """内存下载图片并直传 GitHub，不落盘"""
    try:
        # 1️⃣ 内存下载图片
        resp = requests.get(img_url, timeout=20)
        resp.raise_for_status()
        img_bytes = resp.content

        # 2️⃣ 生成文件名 + 路径
        safe_name = get_safe_filename(name)
        # 从URL提取扩展名，默认jpg
        ext = os.path.splitext(img_url.split('?')[0])[-1].lower()
        if ext not in ['.jpg', '.jpeg', '.png', '.gif', '.webp']:
            ext = '.jpg'
        filename = f"{safe_name}{ext}"
        path = f"{UPLOAD_DIR}/{filename}"

        # 3️⃣ Base64 编码（GitHub API 要求）
        content_b64 = base64.b64encode(img_bytes).decode("utf-8")

        # 4️⃣ 检查文件是否已存在（获取 sha 用于覆盖）
        check_url = f"{API_BASE}/{quote(path, safe='')}"
        sha = None
        check_resp = requests.get(check_url, headers=HEADERS)
        if check_resp.status_code == 200:
            sha = check_resp.json().get("sha")
            if dry_run:
                print(f"  ⚠️  已存在: {filename}")
                return True  # 测试模式跳过重复

        # 5️⃣ 提交到 GitHub
        payload = {
            "message": f"Add food image: {filename}",
            "content": content_b64,
            "branch": BRANCH
        }
        if sha:
            payload["sha"] = sha  # 覆盖更新

        if not dry_run:
            put_resp = requests.put(check_url, headers=HEADERS, json=payload)
            if put_resp.status_code in (200, 201):
                print(f"  ✅ 成功: {filename}")
                return True
            else:
                err_msg = put_resp.json().get('message', 'Unknown error')
                print(f"  ❌ 失败: {filename} | {err_msg}")
                return False
        else:
            print(f"  🔍 [测试] 准备上传: {filename} ({len(img_bytes)} bytes)")
            return True

    except requests.exceptions.RequestException as e:
        print(f"  💥 网络错误: {name} | {e}")
        return False
    except Exception as e:
        print(f"  💥 异常: {name} | {type(e).__name__}: {e}")
        return False

def main(test_mode=True, start_index=0):
    """
    test_mode: True=仅测试前10张，不实际上传；False=正式上传
    start_index: 从第几条开始（用于断点续传）
    """
    if not os.path.exists(CSV_FILE):
        print(f"❌ 找不到文件: {CSV_FILE}")
        return

    print(f"📖 读取 {CSV_FILE} ...")
    with open(CSV_FILE, 'r', encoding='utf-8-sig') as f:
        reader = csv.DictReader(f)
        rows = list(reader)

    print(f"📊 总数据: {len(rows)} 条")
    
    # 🎯 筛选有效数据
    valid_rows = [r for r in rows if r.get('名称', '').strip() and r.get('图片URL', '').strip()]
    print(f"✅ 有效数据: {len(valid_rows)} 条（含名称+图片URL）")
    
    # 📍 支持断点续传
    if start_index > 0:
        valid_rows = valid_rows[start_index:]
        print(f"🔄 从第 {start_index + 1} 条开始，剩余: {len(valid_rows)} 条")

    # 🧪 测试模式：仅前10条
    if test_mode:
        valid_rows = valid_rows[:10]
        print(f"🧪 测试模式：仅处理前 10 条（不实际上传）\n")
    else:
        print(f"🚀 正式上传模式：共 {len(valid_rows)} 张图片\n")

    success = failed = skipped = 0
    for idx, row in enumerate(valid_rows, 1):
        name = row.get('名称', '').strip()
        img_url = row.get('图片URL', '').strip()
        
        if not name or not img_url:
            skipped += 1
            continue

        print(f"[{idx:3d}/{len(valid_rows)}] {name}")
        
        if upload_image_to_github(name, img_url, dry_run=test_mode):
            success += 1
        else:
            failed += 1

        # ⏱️ 速率控制：避免触发 GitHub API 限制
        time.sleep(1.2)

        # 📈 进度汇报
        if idx % 5 == 0 or idx == len(valid_rows):
            mode = "🧪测试" if test_mode else "🚀上传"
            print(f"  → {mode}进度: {idx}/{len(valid_rows)} | ✅{success} | ❌{failed}\n")

    # 📊 汇总
    print("\n" + "="*60)
    if test_mode:
        print(f"🧪 测试完成！准备上传: {success} | 跳过: {skipped} | 失败: {failed}")
        print("💡 确认无误后，修改 test_mode=False 开始正式上传")
    else:
        print(f"🎉 全部完成！成功: {success} | 失败: {failed} | 跳过: {skipped}")
    
    repo_url = f"https://github.com/{REPO_OWNER}/{REPO_NAME}/tree/{BRANCH}/{UPLOAD_DIR}"
    print(f"🔗 查看图片: {repo_url}")
    print("="*60)

if __name__ == "__main__":
    # 🔧 使用指引：
    # 1️⃣ 先测试（不实际上传）: 保持 test_mode=True
    # 2️⃣ 正式上传: 改为 test_mode=False
    # 3️⃣ 断点续传: 设置 start_index=已上传数量
    
    main(test_mode=False, start_index=0)  # ← 首次运行保持 True 测试