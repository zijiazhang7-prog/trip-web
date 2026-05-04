import csv
import requests
import os
import urllib.parse
from dotenv import load_dotenv

load_dotenv()

# ================= 配置区 =================
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")
REPO_OWNER = os.getenv("GITHUB_USERNAME")
REPO_NAME = "trip-web"
BRANCH = "dev"
TARGET_PATH = "scripts/scenic_spot"   # 与上传脚本完全一致
CSV_FILE = "北京景点学校1.csv"
CHECK_LIMIT = 1042                    # 只检查前1042条
# ========================================

HEADERS = {
    "Authorization": f"Bearer {GITHUB_TOKEN}",
    "Accept": "application/vnd.github.v3+json"
}

def sanitize_filename(name):
    """与上传脚本完全一致的文件名清洗规则"""
    invalid_chars = '<>:"/\\|？*'
    for char in invalid_chars:
        name = name.replace(char, '_')
    # 仅保留字母、数字、空格、横杠、下划线
    safe_name = "".join(c for c in name if c.isalnum() or c in (' ', '-', '_')).strip()
    return safe_name[:80]  # 截断防超长

def get_github_files():
    """获取GitHub目录下所有文件名（去后缀）"""
    url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/contents/{urllib.parse.quote(TARGET_PATH, safe='/')}?ref={BRANCH}"
    try:
        resp = requests.get(url, headers=HEADERS, timeout=15)
        if resp.status_code == 403 and "secondary rate limit" in resp.text.lower():
            print("⚠️ 触发GitHub二次限流，请等待1分钟后再试。")
            return set()
        resp.raise_for_status()
        
        items = resp.json()
        # 提取文件名并去掉 .jpg 后缀用于比对
        return {item['name'].replace('.jpg', '') for item in items if item['type'] == 'file'}
    except requests.exceptions.HTTPError as e:
        if resp.status_code == 404:
            print("❌ 目录不存在，请检查分支名或路径是否正确。")
        else:
            print(f"❌ 获取GitHub文件列表失败: {e}")
        return set()

def get_csv_names(limit):
    """读取CSV前N条景点名称"""
    names = []
    if not os.path.exists(CSV_FILE):
        print(f"❌ 找不到文件: {CSV_FILE}")
        return names
        
    with open(CSV_FILE, 'r', encoding='utf-8-sig') as f:
        reader = csv.DictReader(f)
        for i, row in enumerate(reader):
            if i >= limit:
                break
            names.append(row['name'].strip())
    return names

def main():
    print("🔍 正在从GitHub获取已上传的文件列表...")
    github_files = get_github_files()
    if not github_files:
        return
    print(f"✅ GitHub目录中共有 {len(github_files)} 个文件")

    print(f"📖 正在读取CSV前{CHECK_LIMIT}条数据...")
    csv_names = get_csv_names(CHECK_LIMIT)
    print(f"✅ 共读取 {len(csv_names)} 个景点名称")

    print("\n🔄 正在比对缺失的图片...")
    missing = []
    for name in csv_names:
        safe_name = sanitize_filename(name)
        if safe_name not in github_files:
            missing.append(name)

    print(f"\n📊 比对完成！前{CHECK_LIMIT}条中缺失 {len(missing)} 张图片：")
    for i, name in enumerate(missing, 1):
        print(f"{i}. {name}")

    # 自动保存结果到文本文件，方便复制
    if missing:
        out_file = f"missing_top_{CHECK_LIMIT}.txt"
        with open(out_file, "w", encoding="utf-8") as f:
            for name in missing:
                f.write(name + "\n")
        print(f"\n💾 缺失名单已保存至: {out_file}")
    else:
        print("\n🎉 太棒了！前1042条全部上传成功，无缺失！")

if __name__ == "__main__":
    main()