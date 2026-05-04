import csv
import requests
import base64
import time
import os
from urllib.parse import quote
import dotenv

dotenv.load_dotenv()

# ================= 配置区 =================
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")    # 从环境变量获取 Token
REPO_OWNER = os.getenv("GITHUB_USERNAME")   # GitHub 用户名
REPO_NAME = "trip-web"        # 仓库名
BRANCH = "dev"                            # 分支名
CSV_FILE = "北京景点学校1.csv"            # 你的 CSV 文件
UPLOAD_DIR = "scripts/scenic_spot2"                      # 仓库内保存路径，一开始是scenic_spot，后来是scenic_spot2

API_BASE = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/contents"
HEADERS = {
    "Authorization": f"Bearer {GITHUB_TOKEN}",
    "Accept": "application/vnd.github.v3+json"
}

def upload_image_to_github(name, img_url):
    """内存下载并直传 GitHub，不落盘"""
    try:
        # 1. 内存下载图片
        resp = requests.get(img_url, timeout=15)
        resp.raise_for_status()
        img_bytes = resp.content

        # 2. 生成安全文件名
        safe_name = "".join(c for c in name if c.isalnum() or c in (' ', '-', '_')).strip()
        filename = f"{safe_name}.jpg"
        path = f"{UPLOAD_DIR}/{filename}"

        # 3. Base64 编码（GitHub API 要求）
        content_b64 = base64.b64encode(img_bytes).decode("utf-8")

        # 4. 检查文件是否已存在（获取 sha 用于覆盖）
        check_url = f"{API_BASE}/{quote(path, safe='')}"
        sha = None
        check_resp = requests.get(check_url, headers=HEADERS)
        if check_resp.status_code == 200:
            sha = check_resp.json().get("sha")

        # 5. 提交到 GitHub
        payload = {
            "message": f"Add attraction image: {filename}",
            "content": content_b64,
            "branch": BRANCH
        }
        if sha:
            payload["sha"] = sha

        put_resp = requests.put(check_url, headers=HEADERS, json=payload)
        if put_resp.status_code in (200, 201):
            print(f"✅ 成功: {filename}")
            return True
        else:
            print(f"❌ 失败: {filename} | {put_resp.json().get('message')}")
            return False

    except Exception as e:
        print(f"💥 异常: {name} | {e}")
        return False

def main():
    if not os.path.exists(CSV_FILE):
        print(f"❌ 找不到 CSV 文件: {CSV_FILE}")
        return

    print(f"📖 读取 {CSV_FILE} ...")
    with open(CSV_FILE, 'r', encoding='utf-8-sig') as f:
        reader = csv.DictReader(f)
        rows = list(reader)

    # 🧪 测试模式：仅前 30 条
    # 第二次传：从 1042 条开始传
    rows = rows[1041:]
    print(f"🚀 开始直传 GitHub（共 {len(rows)} 张，不落盘）\n")

    success = failed = 0
    for idx, row in enumerate(rows, 1):
        name = row.get('name', '').strip()
        img_url = row.get('image', '').strip()
        if not name or not img_url:
            continue

        if upload_image_to_github(name, img_url):
            success += 1
        else:
            failed += 1

        # ⏱️ 严格遵守 GitHub 速率限制（约 1 次/秒）
        time.sleep(1.2)

        if idx % 5 == 0:
            print(f" 进度: {idx}/{len(rows)} | 成功: {success} | 失败: {failed}\n")

    print(f"\n🎉 完成！成功: {success} | 失败: {failed}")
    print(f"🔗 查看仓库: https://github.com/{REPO_OWNER}/{REPO_NAME}/tree/{BRANCH}/{UPLOAD_DIR}")

if __name__ == "__main__":
    main()