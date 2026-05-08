import pandas as pd
import re

# ============ 配置 ============
INPUT_FILE = "北京美食.csv"
OUTPUT_FILE = "北京美食1.csv"
# =============================

def clean_image_url(url):
    """清理图片URL后缀，保留 .jpg/.png 等原始地址"""
    if pd.isna(url) or url == "":
        return ""
    # 方法1：按 @ 分割，取第一部分（推荐，更稳定）
    if "@" in str(url):
        return url.split("@")[0].strip()
    # 方法2：正则匹配（备用）
    match = re.match(r'(https?://.+?\.(jpg|jpeg|png|gif|webp))', str(url), re.I)
    return match.group(1) if match else url.strip()

def main():
    print(f"📖 读取文件: {INPUT_FILE}")
    df = pd.read_csv(INPUT_FILE, encoding='utf-8-sig')  # 兼容中文编码
    
    print(f"📊 原始数据: {len(df)} 条")
    
    # 检查列名（兼容不同命名）
    url_col = None
    for col in ['图片URL', '图片url', 'image_url', 'img_url']:
        if col in df.columns:
            url_col = col
            break
    
    if not url_col:
        print("❌ 未找到图片URL列，请检查CSV表头")
        print(f"可用列: {list(df.columns)}")
        return
    
    # 清理前预览
    sample = df[url_col].dropna().head(3).tolist()
    print(f"\n🔍 清理前示例:")
    for s in sample:
        print(f"   {s[:80]}...")
    
    # 执行清理
    df[url_col] = df[url_col].apply(clean_image_url)
    
    # 清理后预览
    print(f"\n✨ 清理后示例:")
    for s in sample:
        cleaned = clean_image_url(s)
        print(f"   {cleaned}")
    
    # 保存结果
    df.to_csv(OUTPUT_FILE, index=False, encoding='utf-8-sig')
    print(f"\n✅ 已保存: {OUTPUT_FILE}")
    
    # 统计信息
    valid_urls = df[url_col].str.startswith('http').sum()
    print(f"📈 有效图片链接: {valid_urls}/{len(df)} ({valid_urls/len(df)*100:.1f}%)")

if __name__ == "__main__":
    main()