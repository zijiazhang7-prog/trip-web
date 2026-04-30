- 作用：写导入脚本要怎么做。

# 数据导入计划（Import Plan）

## 1. 文件用途
本文件用于说明本项目的数据导入策略、导入流程、文件格式、字段映射、校验规则、批次记录和回滚方式，为以下工作提供执行依据：

- 真实数据与样例数据整理
- Python 清洗脚本编写
- 批量导入数据库
- 文件资源导入文件存储
- 初始化演示环境
- 后续增量更新与问题追踪

本文件关注的是“数据怎么进系统”，不替代数据来源说明和数据库结构说明。

相关文档：
- `project-root/docs/03_data/data-source.md`
- `project-root/docs/03_data/schema.md`
- `project-root/docs/03_data/data-dictionary.md`
- `project-root/docs/03_data/er-model.md`

---

## 2. 导入计划目标

本项目的数据导入计划目标如下：

1. 支持从真实公开数据、人工整理数据和测试样例数据中生成可导入数据集；
2. 支持将结构化数据批量导入 MySQL；
3. 支持将图片、视频、地图 JSON/CSV 等资源导入文件存储；
4. 支持用最小样例先跑通 MVP，再逐步扩展到课程要求的数据规模；
5. 支持记录导入批次、导入状态和失败原因，便于后续排查。

课程要求中明确提出应尽量贴近真实景区和校园，建议爬取真实地图数据，并满足目的地数量、建筑物数量、设施数量、边数和用户数等下限。:contentReference[oaicite:3]{index=3}

---

## 3. 导入范围

当前导入范围分为两类：

### 3.1 首次初始化导入
用于搭建数据库基础数据，支撑系统第一次运行和 MVP 演示，包括：
- 测试用户
- 用户偏好样例
- 目的地数据
- 场所 / 建筑物数据
- 设施数据
- 地图节点与边数据
- 美食数据
- 样例日记与样例评分（可选）
- 样例路线记录（可选）

### 3.2 增量导入
用于后续扩充真实数据或修正已有数据，包括：
- 新增目的地
- 新增设施和美食
- 新增地图节点和边
- 替换或补充封面图、日记图片等媒体资源
- 修复命名、分类、经纬度等字段问题

---

## 4. 导入总体架构

本项目采用“离线清洗 + 统一导入”的方式完成数据入库。

### 4.1 总体流程
```text
外部数据源 / 人工整理表格 / 测试样例
    ↓
原始数据文件（raw）
    ↓
Python + pandas 清洗与转换
    ↓
标准化导入文件（csv / json）
    ↓
ImportService / SQL 初始化 / 脚本导入
    ↓
MySQL 8 + 文件存储
    ↓
系统运行与前后端联调
````

### 4.2 技术分工

- `Python + pandas`：负责读取、清洗、转换、去重、生成标准导入文件
- `ImportService`：负责接收标准化导入文件并写入数据库/文件存储
- `MySQL 8`：保存结构化核心数据
- 文件存储：保存图片、视频、地图源文件、媒体资源等

系统总体架构图中已明确采用该方案：离线数据处理层使用 Python + pandas，导入通过 `ImportService` 进入 MySQL 和文件存储。

---

## 5. 目录与文件组织建议

建议在仓库中按如下方式组织导入相关文件：

```text
data/
├─ raw/                 # 原始采集数据，不直接入库
│  ├─ destinations/
│  ├─ places/
│  ├─ facilities/
│  ├─ map/
│  ├─ food/
│  └─ media/
│
├─ cleaned/             # 清洗后中间结果
│  ├─ destinations/
│  ├─ places/
│  ├─ facilities/
│  ├─ map/
│  ├─ food/
│  └─ media/
│
├─ import/              # 最终导入文件
│  ├─ users.csv
│  ├─ user_preferences.csv
│  ├─ destinations.csv
│  ├─ places.csv
│  ├─ facilities.csv
│  ├─ map_nodes.csv
│  ├─ map_edges.csv
│  ├─ foods.csv
│  ├─ diaries.csv
│  ├─ diary_media.csv
│  ├─ diary_ratings.csv
│  └─ route_history.csv
│
└─ sample/              # 小规模样例数据，便于联调和测试
```

脚本建议统一放在：

```text
scripts/
├─ import/
│  ├─ clean_destinations.py
│  ├─ clean_places.py
│  ├─ clean_facilities.py
│  ├─ clean_map_data.py
│  ├─ clean_food_data.py
│  ├─ seed_users.py
│  └─ run_import.py
```

---

## 6. 标准导入文件格式建议

### 6.1 结构化主数据

结构化主数据统一使用：

- `CSV`：适合表格类批量导入
- `JSON`：适合层级结构或路径节点序列
- `SQL`：适合初始化固定小样本数据

推荐优先级：

1. `CSV`
2. `JSON`
3. `SQL`

### 6.2 媒体与地图资源

媒体和地图资源统一采用文件形式保存：

- 图片：`.jpg` / `.png`
- 视频：`.mp4`
- 地图原始结构：`.json` / `.csv`

数据库中只保存：

- 文件访问路径
- 文件名
- 类型
- 排序号
- 批次信息（如需要）

---

## 7. 各数据表导入说明

## 7.1 用户表 `user`

### 数据来源

- 小组人工构造测试账号
- 系统正式注册用户

### 导入方式

- 初始化阶段：CSV / SQL 导入测试账号
- 运行阶段：注册接口动态写入

### 导入字段建议

- `username`
- `password_hash`
- `nickname`
- `avatar_url`
- `role`
- `status`

### 当前建议

- 初始化至少准备 10 个测试用户，满足课程要求。

---

## 7.2 用户偏好表 `user_preference`

### 数据来源

- 小组人工构造偏好样例
- 用户后续在系统中修改的偏好

### 导入方式

- 初始化阶段：CSV 导入
- 运行阶段：前端设置后写入数据库

### 导入字段建议

- `user_id`
- `prefer_hot_level`
- `prefer_theme`
- `prefer_food_type`
- `prefer_crowd_level`
- `travel_style`

---

## 7.3 目的地表 `destination`

### 数据来源

- 官方景区 / 校园公开信息
- 文旅局或公开旅游资料
- 开放地图平台基础 POI 补充
- 小组人工整理与标签补充

### 导入方式

- 原始数据先进入 `data/raw/destinations/`
- 脚本清洗为 `destinations.csv`
- 通过导入脚本或 `ImportService` 写入数据库

### 导入字段建议

- `name`
- `type`
- `category`
- `city`
- `description`
- `heat_score`
- `rating_score`
- `tag_json`
- `cover_url`
- `status`

### 当前建议

- 第一阶段先导入少量高质量目的地；
- 第二阶段再扩充数量。

---

## 7.4 场所 / 建筑表 `place`

### 数据来源

- 官方校园地图、景区导览图
- 公开导览资料
- OpenStreetMap / 地图信息抽取
- 小组人工补录

### 导入方式

- 先从导览图或原始数据抽取基础点位
- 清洗并映射到所属 `destination_id`
- 统一导出为 `places.csv`

### 导入字段建议

- `destination_id`
- `name`
- `place_type`
- `description`
- `lng`
- `lat`
- `floor_info`
- `heat_score`
- `rating_score`

### 当前建议

- 每个演示目的地优先保证 20 个以上场所 / 建筑点位。

---

## 7.5 服务设施表 `facility`

### 数据来源

- 校园地图 / 景区导览图上的设施标注
- 地图平台 POI
- 小组人工补录

### 导入方式

- 统一整理为标准设施分类
- 与 `destination_id` 和必要时的 `place_id` 关联
- 导出为 `facilities.csv`

### 导入字段建议

- `destination_id`
- `place_id`
- `name`
- `facility_type`
- `description`
- `lng`
- `lat`
- `status`

### 当前建议

- 设施分类先固定，避免导入后出现同义词混乱；
- 例如：`toilet`、`canteen`、`shop`、`library`、`supermarket`、`cafe` 等。

---

## 7.6 地图节点表 `map_node`

### 数据来源

- OpenStreetMap 区域道路与点位数据
- 校园 / 景区导览图人工建模
- 设施和场所的节点映射结果

### 导入方式

- 先整理原始地图节点或人工标注点
- 补齐所属目的地和节点类型
- 导出为 `map_nodes.csv`

### 导入字段建议

- `destination_id`
- `node_name`
- `node_type`
- `ref_id`
- `lng`
- `lat`
- `floor_no`

### 当前建议

- 首版先保证关键路口、建筑入口和设施点能够形成完整图；
- 不强求一开始就把每个目的地都做满。

---

## 7.7 地图边表 `map_edge`

### 数据来源

- OpenStreetMap 提取的道路关系
- 导览图人工建模
- 小组为时间策略补充的理想速度和拥挤度

### 导入方式

- 将原始线段或道路关系转换为边表
- 明确起点节点、终点节点、距离和边类型
- 导出为 `map_edges.csv`

### 导入字段建议

- `destination_id`
- `from_node_id`
- `to_node_id`
- `distance`
- `ideal_speed`
- `crowd_factor`
- `transport_type`
- `edge_type`
- `bidirectional_flag`

### 当前建议

- 先做最短距离策略必需字段；
- `ideal_speed` 和 `crowd_factor` 可先用规则生成，再逐步细化；
- 边数后续逐步扩展到课程要求下限。

---

## 7.8 美食表 `food`

### 数据来源

- 学校食堂窗口信息
- 景区餐饮公开信息
- 地图平台餐饮类 POI
- 小组人工整理菜系与价格

### 导入方式

- 将餐饮点和菜品信息统一整理为 `foods.csv`
- 与 `destination_id`、`facility_id` 关联
- 后续可增量更新

### 导入字段建议

- `destination_id`
- `facility_id`
- `name`
- `food_type`
- `shop_name`
- `description`
- `heat_score`
- `rating_score`
- `avg_price`
- `cover_url`

---

## 7.9 旅游日记表 `diary`

### 数据来源

- 小组人工构造样例日记
- 系统运行后用户真实发布内容

### 导入方式

- 初始化阶段：准备 `diaries.csv`
- 运行阶段：通过发布接口写入

### 导入字段建议

- `user_id`
- `destination_id`
- `title`
- `content_text`
- `content_compressed`
- `heat_score`
- `rating_score`
- `status`

### 当前建议

- MVP 阶段先准备 20~50 篇样例日记，用于浏览、排序和检索联调。

---

## 7.10 日记媒体表 `diary_media`

### 数据来源

- 小组测试图片 / 视频
- 用户上传媒体文件

### 导入方式

- 媒体文件先放文件存储
- 再导入 `diary_media.csv` 保存元数据

### 导入字段建议

- `diary_id`
- `media_type`
- `file_url`
- `file_name`
- `sort_no`

---

## 7.11 日记评分表 `diary_rating`

### 数据来源

- 小组人工构造评分样例
- 用户运行时评分

### 导入方式

- 初始化阶段可导入少量评分数据
- 运行阶段通过评分接口写入

### 导入字段建议

- `diary_id`
- `user_id`
- `score`
- `created_at`

---

## 7.12 路线记录表 `route_history`

### 数据来源

- 小组人工构造样例路线
- 用户真实规划结果

### 导入方式

- 初始化阶段可准备少量样例路线
- 运行阶段由路径规划模块自动写入

### 导入字段建议

- `user_id`
- `destination_id`
- `start_node_id`
- `end_node_id`
- `path_node_json`
- `strategy_type`
- `created_at`

---

## 7.13 导入批次表 `import_batch`（可选）

### 数据来源

- 每次执行批量导入时由导入程序自动生成

### 导入方式

- 由 `ImportService` 或导入脚本写入
- 不需要外部原始数据文件

### 建议字段

- `batch_name`
- `source_type`
- `file_path`
- `status`
- `created_at`

当前数据库设计中已将 `import_batch` 作为可选支撑表，用于记录批量导入来源和状态。

---

## 8. 导入顺序建议

为避免外键依赖冲突，推荐按如下顺序导入：

### 第一批：基础与无依赖表

1. `user`
2. `destination`

### 第二批：强依赖基础表

3. `user_preference`
4. `place`
5. `facility`
6. `food`

### 第三批：地图数据

7. `map_node`
8. `map_edge`

### 第四批：内容与行为数据

9. `diary`
10. `diary_media`
11. `diary_rating`
12. `route_history`

### 第五批：导入记录

13. `import_batch`（可选）

---

## 9. 数据清洗规则

所有原始数据在导入前，必须经过清洗。清洗至少包括以下内容：

### 9.1 基础清洗

- 去除空行
- 去除无效列
- 去除重复记录
- 统一编码为 UTF-8

### 9.2 字段标准化

- 名称去前后空格
- 同义类别统一
- 空值统一处理
- 数值字段转为合法数值类型
- 坐标字段统一为经纬度格式

### 9.3 主外键映射

- `destination_id` 必须存在
- `place_id`、`facility_id`、`user_id` 等外键必须能映射到主表
- 地图边中的 `from_node_id`、`to_node_id` 必须存在于 `map_node`

### 9.4 媒体路径校验

- 文件必须真实存在
- 文件类型必须匹配 `media_type`
- 文件访问路径应符合系统约定

---

## 10. 数据校验规则

在正式导入前，建议至少执行以下校验：

### 10.1 必填校验

例如：

- `destination.name` 不能为空
- `place.destination_id` 不能为空
- `map_edge.from_node_id` / `to_node_id` 不能为空
- `diary.title` / `content_text` 不能为空

### 10.2 范围校验

例如：

- `rating_score` 在合法范围内
- `crowd_factor` 大于 0 且小于等于 1（若采用该规则）
- `avg_price` 不得为负数
- `score` 在评分范围内

### 10.3 去重校验

例如：

- 用户名去重
- 同一目的地下同名节点需要检查
- `(diary_id, user_id)` 评分组合不得重复

### 10.4 关系校验

例如：

- 每条 `map_edge` 必须归属于同一 `destination`
- `food.facility_id` 所属设施应与 `food.destination_id` 保持一致
- `diary.destination_id` 必须存在于 `destination`

---

## 11. 导入执行方式建议

## 11.1 MVP 阶段

推荐采用：

- Python 脚本清洗
- 生成标准化 CSV / JSON
- 通过后端导入脚本或 SQL 批量导入

## 11.2 后续阶段

可逐步完善为：

- 管理端支持批量导入
- 每次导入自动写 `import_batch`
- 导入失败时生成失败日志

## 11.3 当前推荐执行方式

建议先实现一个统一入口脚本，例如：

```bash
python scripts/import/run_import.py --batch init_demo
```

该脚本可负责：

1. 读取标准导入文件
2. 调用清洗校验逻辑
3. 按顺序导入数据库
4. 记录成功 / 失败信息
5. 输出导入报告

---

## 12. 导入失败与回滚策略

### 12.1 失败记录

每次导入应记录：

- 批次名称
- 来源文件
- 导入时间
- 成功表
- 失败表
- 错误原因

### 12.2 回滚策略

- 对单张表导入失败，建议当前表事务回滚；
- 对整批初始化导入失败，建议记录失败批次并重新执行；
- 对媒体文件导入失败，需保留失败日志，不直接删除数据库已有数据。

### 12.3 当前阶段建议

- 首版先保证“单表事务回滚 + 整批失败日志记录”即可；
- 不必一开始就做复杂的全链路补偿机制。

---

## 13. 演示环境与正式环境的数据策略

### 13.1 演示环境

- 优先使用小规模高质量真实样例数据
- 优先保证主线闭环跑通
- 样例用户、样例日记、样例路线可人工构造

### 13.2 扩展环境

- 逐步扩展到课程要求数量下限
- 增加目的地、边和设施数量
- 提高推荐和查询的真实感

### 13.3 当前阶段建议

采用“两阶段数据策略”：

1. 先做 MVP 小样本
2. 再做课程要求规模扩展

这样更适合课程进度安排和三人小组的开发节奏。课程要求中也强调应按阶段推进开发、测试和文档工作。

---

## 14. 当前阶段任务拆分建议

### 成员 A

- 负责用户、目的地、日记相关导入文件整理
- 负责初始化 SQL / 基础导入脚本

### 成员 B

- 负责地图节点、地图边、设施数据清洗与导入
- 负责路径图相关校验脚本

### 成员 C

- 负责美食、媒体资源、样例日记素材整理
- 负责导入结果验证与测试记录

---

## 15. 当前阶段最小可执行导入清单

在正式开发前，建议先完成以下最小导入集：

1. 10 个测试用户
2. 2~5 个真实目的地
3. 每个目的地 20 个以上场所 / 建筑点
4. 每个目的地若干设施点
5. 1~2 个目的地的完整地图节点与边
6. 少量美食数据
7. 20~50 篇样例日记
8. 少量样例评分与样例路线记录

这样就足以支撑：

- 登录
- 推荐
- 搜索
- 路线规划
- 周边设施查询
- 美食推荐基础版
- 日记发布 / 浏览 / 排序

---

## 16. 与其他文档的关系

本文件应与以下文档保持一致：

- `project-root/docs/03_data/data-source.md
- `project-root/docs/03_data/schema.md`
- `project-root/docs/03_data/data-dictionary.md`
- `project-root/docs/02_architecture/architecture.md`
- `project-root/docs/02_architecture/coding-plan.md`
- `project-root/docs/04_api/api-spec.md`

如果导入顺序、文件格式、字段映射或导入方式发生变化，应同步更新这些文档。

---

## 17. 后续维护说明

本文件应在以下场景下更新：

1. 新增重要数据源或导入表时
2. 导入脚本结构变化时
3. 从样例导入切换到更大规模真实导入时
4. 管理端开始支持批量导入时
5. 创新功能需要新增素材导入时
