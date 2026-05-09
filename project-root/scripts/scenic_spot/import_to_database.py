import csv
import pymysql
from datetime import datetime
import sys
import os

def load_env_file():
    """从 .env 文件加载环境变量"""
    env_file = os.path.join(os.path.dirname(__file__), '.env')

    if not os.path.exists(env_file):
        return None

    env_vars = {}
    with open(env_file, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            if '=' in line:
                key, value = line.split('=', 1)
                env_vars[key.strip()] = value.strip()

    return env_vars

def create_database_if_not_exists(host, port, user, password, database, charset):
    """创建数据库（如果不存在）"""
    try:
        conn = pymysql.connect(
            host=host,
            port=port,
            user=user,
            password=password,
            charset=charset
        )
        cursor = conn.cursor()

        sql = f"CREATE DATABASE IF NOT EXISTS `{database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
        cursor.execute(sql)
        conn.commit()

        print(f"✓ 数据库 '{database}' 创建成功（或已存在）")

        cursor.close()
        conn.close()
        return True

    except Exception as e:
        print(f"✗ 创建数据库失败: {str(e)}")
        return False

def init_database_schema():
    """初始化数据库表结构"""
    schema_file = os.path.join(os.path.dirname(__file__), '..', '..', 'backend', 'src', 'main', 'resources', 'db', 'init-p0-schema.sql')

    if not os.path.exists(schema_file):
        print(f"⚠ 未找到建表脚本: {schema_file}")
        print("请手动执行建表脚本或确保数据库表已创建")
        return False

    env_vars = load_env_file()
    if env_vars:
        host = env_vars.get('MYSQL_HOST', 'localhost')
        port = int(env_vars.get('MYSQL_PORT', 3306))
        user = env_vars.get('MYSQL_USER', 'root')
        password = env_vars.get('MYSQL_PASSWORD', '')
        database = env_vars.get('MYSQL_DATABASE', 'tour_system')
        charset = env_vars.get('MYSQL_CHARSET', 'utf8mb4')
    else:
        host = 'localhost'
        port = 3306
        user = 'root'
        password = os.environ.get('MYSQL_ROOT_PASSWORD', '')
        database = 'tour_system'
        charset = 'utf8mb4'

    try:
        conn = pymysql.connect(
            host=host,
            port=port,
            user=user,
            password=password,
            database=database,
            charset=charset
        )

        with open(schema_file, 'r', encoding='utf-8') as f:
            sql_content = f.read()

        statements = [s.strip() for s in sql_content.split(';') if s.strip() and not s.strip().startswith('--')]

        cursor = conn.cursor()
        for statement in statements:
            if statement:
                try:
                    cursor.execute(statement)
                except Exception as e:
                    if 'already exists' not in str(e).lower():
                        pass

        conn.commit()
        print("✓ 数据库表结构初始化完成（或已存在）")

        cursor.close()
        conn.close()
        return True

    except Exception as e:
        print(f"⚠ 初始化表结构时出现警告: {str(e)}")
        return False

def get_db_connection():
    env_vars = load_env_file()

    if env_vars:
        host = env_vars.get('MYSQL_HOST', 'localhost')
        port = int(env_vars.get('MYSQL_PORT', 3306))
        user = env_vars.get('MYSQL_USER', 'root')
        password = env_vars.get('MYSQL_PASSWORD', '')
        database = env_vars.get('MYSQL_DATABASE', 'tour_system')
        charset = env_vars.get('MYSQL_CHARSET', 'utf8mb4')
    else:
        password = os.environ.get('MYSQL_ROOT_PASSWORD', '')

        if not password:
            print("\n" + "="*60)
            print("⚠ MySQL 密码未设置")
            print("请选择以下任一方式设置密码：")
            print("="*60)
            print("\n方式1: 创建 .env 文件（推荐）")
            print('  1. 复制 .env.example 为 .env')
            print('  2. 修改 .env 中的 MYSQL_PASSWORD 为您的密码')
            print('  3. 重新运行脚本')
            print("\n方式2: 设置环境变量")
            print('  Windows PowerShell: $env:MYSQL_ROOT_PASSWORD="你的密码"')
            print('  Windows CMD: set MYSQL_ROOT_PASSWORD=你的密码')
            print("")
            sys.exit(1)

        host = 'localhost'
        port = 3306
        user = 'root'
        database = 'tour_system'
        charset = 'utf8mb4'

    if not create_database_if_not_exists(host, port, user, password, database, charset):
        sys.exit(1)

    print("\n检查并初始化数据库表结构...")
    init_database_schema()

    return pymysql.connect(
        host=host,
        port=port,
        user=user,
        password=password,
        database=database,
        charset=charset,
        cursorclass=pymysql.cursors.DictCursor
    )

def import_destinations(csv_file_path):
    conn = None
    cursor = None
    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        print("\n" + "="*60)
        print("✓ 数据库连接成功！")
        print(f"  主机: {conn.host}")
        print(f"  数据库: {conn.db}")
        print("="*60 + "\n")

        with open(csv_file_path, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)

            success_count = 0
            error_count = 0
            error_details = []

            for row_num, row in enumerate(reader, start=2):
                try:
                    sql = """
                    INSERT INTO destination
                    (name, type, category, city, description, heat_score, rating_score, tag_json, cover_url, status, created_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """

                    name = row['name'].strip() if row['name'] else None
                    dest_type = row['type'].strip() if row['type'] else None
                    category = row['category'].strip() if row['category'] else None
                    city = row['city'].strip() if row['city'] else None
                    description = row['description'].strip() if row['description'] else None
                    heat_score = float(row['heat_score'].strip()) if row['heat_score'] and row['heat_score'].strip() else 0
                    rating_score = float(row['rating_score'].strip()) if row['rating_score'] and row['rating_score'].strip() else 0
                    tag_json = row['tag_json'].strip() if row['tag_json'] else None
                    cover_url = row['cover_url'].strip() if row['cover_url'] else None
                    status = int(row['status'].strip()) if row['status'] and row['status'].strip() else 1
                    created_at = datetime.now()

                    values = (
                        name, dest_type, category, city, description,
                        heat_score, rating_score, tag_json, cover_url, status, created_at
                    )

                    cursor.execute(sql, values)
                    success_count += 1

                    if success_count % 10 == 0:
                        conn.commit()
                        print(f"已导入 {success_count} 条记录...")

                except Exception as e:
                    error_count += 1
                    error_details.append(f"行 {row_num}: {str(e)}")
                    print(f"警告: 行 {row_num} 导入失败 - {str(e)}")

            conn.commit()

            print("\n" + "="*60)
            print("导入完成！")
            print(f"✓ 成功导入: {success_count} 条")
            print(f"✗ 失败: {error_count} 条")

            if error_count > 0:
                print("\n失败详情:")
                for detail in error_details[:10]:
                    print(f"  - {detail}")
                if len(error_details) > 10:
                    print(f"  ... 还有 {len(error_details) - 10} 条错误")

            return success_count, error_count

    except Exception as e:
        print(f"\n严重错误: {str(e)}")
        if conn:
            conn.rollback()
        return 0, 0

    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()

def verify_import(csv_file_path):
    conn = None
    cursor = None
    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        cursor.execute("SELECT COUNT(*) as count FROM destination")
        result = cursor.fetchone()
        db_count = result['count']

        with open(csv_file_path, 'r', encoding='utf-8') as f:
            csv_count = sum(1 for line in f) - 1

        print("\n验证结果:")
        print(f"  CSV 文件记录数: {csv_count}")
        print(f"  数据库记录数: {db_count}")

        if csv_count == db_count:
            print("  ✓ 数量匹配，导入成功！")
        else:
            print(f"  ⚠ 数量不匹配，差异: {abs(csv_count - db_count)}")

        cursor.execute("SELECT id, name, type, category FROM destination LIMIT 5")
        samples = cursor.fetchall()
        print("\n前5条示例数据:")
        for row in samples:
            print(f"  ID: {row['id']}, 名称: {row['name']}, 类型: {row['type']}, 分类: {row['category']}")

    except Exception as e:
        print(f"验证失败: {str(e)}")

    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()

if __name__ == '__main__':
    csv_file = os.path.join(os.path.dirname(__file__), '北京景点学校22（捏造评分热度）.csv')

    if not os.path.exists(csv_file):
        print(f"错误: 找不到 CSV 文件: {csv_file}")
        sys.exit(1)

    print("开始导入北京景点数据到 destination 表...")
    print("="*60)

    success, failed = import_destinations(csv_file)

    if success > 0:
        verify_import(csv_file)

    sys.exit(0 if failed == 0 else 1)
