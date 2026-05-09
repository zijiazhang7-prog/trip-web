import csv
import sys
import os
from datetime import datetime

def escape_sql_string(s):
    if s is None:
        return 'NULL'
    s = str(s).replace('\\', '\\\\')
    s = s.replace("'", "\\'")
    s = s.replace('"', '\\"')
    s = s.replace('\n', ' ')
    s = s.replace('\r', ' ')
    s = s.replace('\t', ' ')
    return f"'{s}'"

def convert_csv_to_sql(csv_file_path, sql_file_path):
    try:
        with open(csv_file_path, 'r', encoding='utf-8') as csv_file:
            reader = csv.DictReader(csv_file)

            with open(sql_file_path, 'w', encoding='utf-8') as sql_file:
                sql_file.write("-- 北京景点数据导入脚本\n")
                sql_file.write(f"-- 生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
                sql_file.write("-- 用于将 CSV 数据导入到 destination 表\n\n")

                sql_file.write("USE tour_system;\n\n")

                sql_file.write("-- 禁用外键检查以提高导入速度\n")
                sql_file.write("SET FOREIGN_KEY_CHECKS = 0;\n\n")

                sql_file.write("-- 导入数据\n")
                sql_file.write("INSERT INTO destination (name, type, category, city, description, heat_score, rating_score, tag_json, cover_url, status) VALUES\n")

                values_list = []
                row_count = 0

                for row in reader:
                    try:
                        name = escape_sql_string(row.get('name', '').strip())
                        dest_type = escape_sql_string(row.get('type', 'scenic').strip())
                        category = escape_sql_string(row.get('category', '').strip())
                        city = escape_sql_string(row.get('city', '').strip())
                        description = escape_sql_string(row.get('description', '').strip())

                        heat_score = row.get('heat_score', '0').strip()
                        heat_score = float(heat_score) if heat_score else 0

                        rating_score = row.get('rating_score', '0').strip()
                        rating_score = float(rating_score) if rating_score else 0

                        tag_json = escape_sql_string(row.get('tag_json', '').strip())
                        cover_url = escape_sql_string(row.get('cover_url', '').strip())

                        status = row.get('status', '1').strip()
                        status = int(status) if status else 1

                        value = f"({name}, {dest_type}, {category}, {city}, {description}, {heat_score}, {rating_score}, {tag_json}, {cover_url}, {status})"
                        values_list.append(value)
                        row_count += 1

                    except Exception as e:
                        print(f"警告: 处理行时出错 - {str(e)}")

                sql_file.write(',\n'.join(values_list))
                sql_file.write(';\n\n')

                sql_file.write("-- 重新启用外键检查\n")
                sql_file.write("SET FOREIGN_KEY_CHECKS = 1;\n\n")

                sql_file.write("-- 验证导入结果\n")
                sql_file.write("SELECT '导入完成！' AS status;\n")
                sql_file.write("SELECT COUNT(*) AS total_records FROM destination;\n")

            print(f"✓ SQL 文件已生成: {sql_file_path}")
            print(f"✓ 共转换 {row_count} 条记录")
            return True

    except Exception as e:
        print(f"错误: {str(e)}")
        return False

if __name__ == '__main__':
    csv_file = os.path.join(os.path.dirname(__file__), '北京景点学校22（捏造评分热度）.csv')
    sql_file = os.path.join(os.path.dirname(__file__), 'import_beijing_destinations_full.sql')

    if not os.path.exists(csv_file):
        print(f"错误: 找不到 CSV 文件: {csv_file}")
        sys.exit(1)

    print("开始转换 CSV 到 SQL...")
    print("="*60)

    if convert_csv_to_sql(csv_file, sql_file):
        print("\n转换完成！")
        print(f"SQL 文件大小: {os.path.getsize(sql_file)} 字节")
        print(f"\n使用方法:")
        print(f"  mysql -u root -p tour_system < \"{sql_file}\"")
    else:
        sys.exit(1)
