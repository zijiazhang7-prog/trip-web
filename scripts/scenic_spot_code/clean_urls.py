import csv
import os

# crawl_pages.py 文件中提及的 beijing_attractions_all.csv 不是 北京景点学校.csv 
# 配置输入输出文件名
INPUT_FILE = '北京景点学校.csv'
OUTPUT_FILE = '北京景点学校1.csv'

def clean_image_url(url):
    """清理图片URL后缀"""
    if not url or not isinstance(url, str):
        return ''
    # 以 @base@ 为界，只保留前面的纯净链接
    return url.split('@base@')[0].strip()

def process_csv():
    if not os.path.exists(INPUT_FILE):
        print(f"❌ 找不到文件: {INPUT_FILE}")
        print(" 请确保该文件与脚本在同一目录下")
        return

    print(f"📖 正在读取 {INPUT_FILE} ...")
    
    with open(INPUT_FILE, 'r', encoding='utf-8-sig') as f_in, \
         open(OUTPUT_FILE, 'w', encoding='utf-8-sig', newline='') as f_out:
        
        reader = csv.DictReader(f_in)
        
        # 安全检查：确认列名存在
        if 'image' not in reader.fieldnames:
            print("❌ CSV中未找到 'image' 列，请检查表头名称。")
            return
            
        writer = csv.DictWriter(f_out, fieldnames=reader.fieldnames)
        writer.writeheader()
        
        count = 0
        for row in reader:
            # 仅处理 image 列，其他列（含 desc）原样保留
            row['image'] = clean_image_url(row['image'])
            writer.writerow(row)
            count += 1
            
    print(f"✅ 处理完成！共清理 {count} 条数据。")
    print(f"💾 新文件已保存为: {OUTPUT_FILE}")
    print(" 建议用 Excel 打开检查 image 列是否已去除后缀。")

if __name__ == "__main__":
    process_csv()