# 北京景点数据导入说明

## 📋 概述

本目录包含将北京景点数据导入到 `destination` 表的完整脚本和工具，支持自动创建数据库和表结构。

## 📊 数据对应关系

### CSV 文件字段 → destination 表字段

| CSV 列名 | 数据库字段 | 数据类型 | 说明 |
|----------|-----------|---------|------|
| name | name | VARCHAR(100) | 目的地名称 ✓ 必填 |
| type | type | VARCHAR(20) | 类型 (scenic/campus)，默认 scenic |
| category | category | VARCHAR(50) | 目的地分类 |
| city | city | VARCHAR(50) | 所在城市 |
| description | description | TEXT | 目的地简介 |
| heat_score | heat_score | DECIMAL(5,2) | 热度分 |
| rating_score | rating_score | DECIMAL(3,2) | 评分 |
| tag_json | tag_json | TEXT | 标签 JSON（使用 \| 分隔）|
| cover_url | cover_url | VARCHAR(255) | 封面图地址 |
| status | status | TINYINT | 状态，默认 1 |

### 自动填充字段
- `id`: 数据库自增，无需手动指定
- `created_at`: 数据库自动使用 CURRENT_TIMESTAMP

## 📁 文件清单

### 核心脚本

| 文件名 | 说明 |
|--------|------|
| **import_to_database.py** | ⭐ **推荐** 直接连接数据库导入数据 |
| csv_to_sql.py | 可选 将 CSV 转换为 SQL 文件 |
| .env.example | 环境变量模板 |

### 数据文件

| 文件名 | 说明 |
|--------|------|
| 北京景点学校22（捏造评分热度）.csv | 源数据（1311条北京景点）|

## 🚀 快速开始

### 步骤 1：配置数据库连接

```powershell
# 进入脚本目录
cd "c:\code\trip-web\project-root\scripts\scenic_spot"

# 创建 .env 文件（复制模板）
copy .env.example .env

# 编辑 .env，设置您的 MySQL 密码
# MYSQL_PASSWORD=您的MySQL密码
```

### 步骤 2：运行导入脚本

```powershell
# 确保已安装依赖
pip install pymysql

# 运行导入
python import_to_database.py
```

### 步骤 3：验证结果

脚本会自动：
- ✅ 创建 `tour_system` 数据库（如不存在）
- ✅ 创建 `destination` 表（如不存在）
- ✅ 导入 1311 条北京景点数据
- ✅ 显示导入统计信息

## 📝 使用方法详解

### 方法一：Python 脚本直接导入（推荐）

```powershell
python import_to_database.py
```

**特点：**
- 自动创建数据库和表结构
- 支持 .env 配置管理敏感信息
- 自动验证导入结果
- 适合自动化部署

**前提条件：**
- MySQL 服务运行中
- 安装 pymysql: `pip install pymysql`

### 方法二：CSV 转 SQL（可选）

如果您需要生成 SQL 文件：

```powershell
# 生成 SQL 文件
python csv_to_sql.py

# 手动执行 SQL
mysql -u root -p tour_system < import_beijing_destinations_full.sql
```

## ⚙️ 数据库配置

### 配置方式 1：.env 文件（推荐）

在脚本目录下创建 `.env` 文件：

```ini
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=您的MySQL密码
MYSQL_DATABASE=tour_system
MYSQL_CHARSET=utf8mb4
```

### 配置方式 2：环境变量

```powershell
# Windows PowerShell
$env:MYSQL_ROOT_PASSWORD="您的密码"
python import_to_database.py

# Windows CMD
set MYSQL_ROOT_PASSWORD=您的密码
python import_to_database.py
```

### 默认配置

| 配置项 | 默认值 |
|--------|--------|
| 主机 | localhost |
| 端口 | 3306 |
| 用户名 | root |
| 数据库 | tour_system |
| 字符集 | utf8mb4 |

## ✅ 验证查询

导入完成后，可执行以下查询验证：

```sql
-- 查看总记录数
SELECT COUNT(*) AS total_destinations FROM destination;

-- 查看前10条数据
SELECT id, name, type, category, city, heat_score, rating_score
FROM destination
ORDER BY id
LIMIT 10;

-- 按分类统计
SELECT category, COUNT(*) AS count
FROM destination
GROUP BY category
ORDER BY count DESC;

-- 按热度排序，查看最热门的景点
SELECT name, heat_score, rating_score, city
FROM destination
ORDER BY heat_score DESC
LIMIT 10;
```

## 📊 数据统计

- **总记录数**: 1,311 条
- **城市**: 全部为北京市
- **类型**: 全部为 scenic（景区）
- **分类**: 包含历史古迹、城市公园、博物馆、纪念馆等 20+ 分类

## 🔧 高级用法

### 重新生成 SQL 文件

如果 CSV 文件已更新，需要重新生成 SQL：

```bash
python csv_to_sql.py
```

### 查看脚本帮助

```bash
python import_to_database.py --help
```

## ⚠️ 注意事项

1. **首次使用**: 脚本会自动创建数据库和表结构，无需手动创建
2. **数据清理**: 如果 destination 表中已有数据，脚本会追加数据（不覆盖）
3. **字符编码**: 使用 UTF-8 编码，支持中文字符
4. **批量导入**: 脚本每 10 条自动提交，确保性能和稳定性
5. **错误处理**: 单条记录失败不影响其他记录导入

## 📞 故障排除

### 问题：MySQL 连接失败

```
Can't connect to MySQL server on 'localhost'
```

**解决方案**:
1. 确保 MySQL 服务已启动
2. 检查 .env 文件中的密码是否正确
3. 检查 MySQL 服务状态：`Get-Service *mysql*`

### 问题：数据库不存在

```
Unknown database 'tour_system'
```

**解决方案**:
脚本会自动创建，无需手动处理。如果失败，手动创建：
```sql
CREATE DATABASE IF NOT EXISTS tour_system CHARACTER SET utf8mb4;
```

### 问题：权限不足

```
Access denied for user 'root'@'localhost'
```

**解决方案**:
1. 检查 .env 文件中的密码是否正确
2. 使用 MySQL Admin 修改密码
3. 或创建新用户：
```sql
CREATE USER 'tripuser'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON tour_system.* TO 'tripuser'@'localhost';
FLUSH PRIVILEGES;
```

### 问题：pymysql 未安装

```
ModuleNotFoundError: No module named 'pymysql'
```

**解决方案**:
```bash
pip install pymysql
```

## 📚 相关文档

- 数据库表结构: `project-root/docs/03_data/schema.md`
- 数据字典: `project-root/docs/03_data/data-dictionary.md`
- 数据库初始化脚本: `project-root/backend/src/main/resources/db/init-p0-schema.sql`
- MySQL 安装指南: `MYSQL_SETUP.md`

## 🔒 安全说明

- ✅ `.env` 文件包含敏感信息，已添加到 `.gitignore`，不会被提交到仓库
- ✅ 使用参数化查询，防止 SQL 注入
- ✅ 不在日志中输出密码等敏感信息
- ✅ 支持环境变量配置，适合 CI/CD 部署

## 📄 许可证

本项目遵循项目主仓库的许可证。
