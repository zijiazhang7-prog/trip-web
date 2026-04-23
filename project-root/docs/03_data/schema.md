- 作用：记录数据库“整体结构设计 + 表关系 + 设计思路”。

# 数据库结构设计（Schema）

## 1. 文档用途
本文件用于说明本项目数据库的总体结构设计，包括：

- 数据库整体设计目标
- 核心实体与表结构划分
- 主要表之间的关系
- 关键字段设计思路
- 索引与扩展建议
- 数据库设计与系统功能之间的对应关系

本文件关注的是“数据库整体结构怎么设计”，不替代更细粒度的数据字典。  
字段级详细说明可进一步参见：
- `docs/03_data/data-dictionary.md`
- `docs/03_data/er-model.md`

---

## 2. 数据库总体设计目标

本项目数据库围绕“旅游前推荐—旅游中导航与周边查询—旅游后日记交流”三条主线展开，目标是为以下能力提供结构化数据支撑：

1. 多用户登录与角色区分  
2. 景区 / 校园等目的地推荐与查询  
3. 景区或校园内部图结构建模与路径规划  
4. 周边设施和美食查询  
5. 旅游日记发布、浏览、评分与检索  
6. 路线记录、数据导入与后续创新扩展  

数据库采用 **MySQL 8** 作为主数据库，字符集建议使用 `utf8mb4`，存储引擎建议使用 `InnoDB`。命名规范采用小写下划线分隔，主键统一为 `id`，外键统一为 `xxx_id`。:contentReference[oaicite:2]{index=2}

---

## 3. 数据库总体结构概览

当前数据库围绕以下 13 张核心表组织：

1. `user`
2. `user_preference`
3. `destination`
4. `place`
5. `facility`
6. `map_node`
7. `map_edge`
8. `food`
9. `diary`
10. `diary_media`
11. `diary_rating`
12. `route_history`
13. `import_batch`（可选）

从业务视角看，可划分为 5 组：

### 3.1 用户与偏好数据
- `user`
- `user_preference`

### 3.2 目的地与空间数据
- `destination`
- `place`
- `facility`
- `map_node`
- `map_edge`

### 3.3 美食数据
- `food`

### 3.4 日记内容数据
- `diary`
- `diary_media`
- `diary_rating`

### 3.5 过程与支撑数据
- `route_history`
- `import_batch`

---

## 4. 核心实体关系说明

根据当前 ER 设计，主要实体关系如下：:contentReference[oaicite:4]{index=4}

### 4.1 用户相关关系
- 一个 `user` 对应一份 `user_preference`
- 一个 `user` 可以发布多篇 `diary`
- 一个 `user` 可以对多篇 `diary` 评分
- 一个 `user` 可以拥有多条 `route_history`

### 4.2 目的地相关关系
- 一个 `destination` 包含多个 `place`
- 一个 `destination` 包含多个 `facility`
- 一个 `destination` 包含多个 `map_node`
- 一个 `destination` 包含多条 `map_edge`
- 一个 `destination` 关联多个 `food`
- 一个 `destination` 关联多篇 `diary`
- 一个 `destination` 关联多条 `route_history`

### 4.3 场所与设施关系
- 一个 `place` 可以包含多个 `facility`
- 一个 `place` 可以映射到一个或多个 `map_node`
- 一个 `facility` 可以进一步关联多个 `food`
- 一个 `facility` 也可以映射到一个或多个 `map_node`

### 4.4 日记相关关系
- 一篇 `diary` 可以关联多个 `diary_media`
- 一篇 `diary` 可以收到多个 `diary_rating`

---

## 5. 各核心表结构设计说明

## 5.1 用户表 `user`

### 表定位
用于保存系统用户的基本信息，是认证、角色区分和个性化能力的基础。:contentReference[oaicite:5]{index=5}

### 主要字段
- `id`
- `username`
- `password_hash`
- `nickname`
- `avatar_url`
- `role`
- `status`
- `created_at`
- `updated_at`

### 设计要点
1. `username` 应唯一；
2. 密码以 `password_hash` 形式存储，不保存明文；
3. `role` 用于区分普通用户和管理员；
4. `status` 用于控制账户状态。

### 建议索引
- `username` 唯一索引
- `role` 普通索引

---

## 5.2 用户偏好表 `user_preference`

### 表定位
用于保存用户的兴趣、风格、推荐偏好，为推荐模块提供输入。

### 主要字段
- `id`
- `user_id`
- `prefer_hot_level`
- `prefer_theme`
- `prefer_food_type`
- `prefer_crowd_level`
- `travel_style`
- `updated_at`

### 设计要点
1. 当前按“一用户一份偏好配置”设计；
2. 偏好字段尽量保持可扩展，不在首版中做过度复杂画像；
3. 后续推荐增强可继续扩展偏好维度。

### 建议索引
- `user_id` 唯一索引

---

## 5.3 目的地表 `destination`

### 表定位
用于保存景区、校园等旅游目的地信息，是推荐、查询和路线规划入口的核心表。

### 主要字段
- `id`
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
- `created_at`

### 设计要点
1. 统一承载景区和校园两类对象；
2. 用 `type` 区分 `scenic / campus`；
3. 用 `heat_score` 和 `rating_score` 支撑推荐排序；
4. `tag_json` 用于存放扩展标签，便于前期灵活实现。

### 建议索引
- `name`
- `type`
- `category`
- `(heat_score, rating_score)`

---

## 5.4 场所表 `place`

### 表定位
用于保存目的地内部的景点、教学楼、办公楼、宿舍楼等点位信息，是内部查询和导航的重要基础。

### 主要字段
- `id`
- `destination_id`
- `name`
- `place_type`
- `description`
- `lng`
- `lat`
- `floor_info`
- `heat_score`
- `rating_score`

### 设计要点
1. `place` 是目的地内部较抽象的“场所层”；
2. 既可用于展示，也可进一步映射到图上的节点；
3. 后续室内导航场景可通过 `floor_info` 或楼层号扩展。

### 建议索引
- `destination_id`
- `name`
- `place_type`

---

## 5.5 服务设施表 `facility`

### 表定位
用于保存厕所、超市、食堂、图书馆、咖啡馆等服务设施，是周边设施查询模块的核心表。

### 主要字段
- `id`
- `destination_id`
- `place_id`
- `name`
- `facility_type`
- `description`
- `lng`
- `lat`
- `status`

### 设计要点
1. 设施既可以直接属于目的地，也可以挂在某个具体场所下；
2. 通过 `facility_type` 支撑按类别过滤；
3. 通过位置字段与地图节点进行近似映射或精确映射。

### 建议索引
- `destination_id`
- `facility_type`
- `name`

---

## 5.6 地图节点表 `map_node`

### 表定位
用于保存图结构中的节点，是路径规划与可达性计算的基础。

### 主要字段
- `id`
- `destination_id`
- `node_name`
- `node_type`
- `ref_id`
- `lng`
- `lat`
- `floor_no`

### 设计要点
1. 节点可以代表：
   - 路口
   - 建筑
   - 设施点
2. `node_type` 用于区分节点来源；
3. `ref_id` 可用于关联 `place` 或 `facility`；
4. 室内导航可借助 `floor_no` 扩展。

### 建议索引
- `destination_id`
- `(node_type, ref_id)`

---

## 5.7 地图边表 `map_edge`

### 表定位
用于保存有向带权图中的边，是最短路径、最短时间、多点路径等算法能力的直接支撑表。

### 主要字段
- `id`
- `destination_id`
- `from_node_id`
- `to_node_id`
- `distance`
- `ideal_speed`
- `crowd_factor`
- `transport_type`
- `edge_type`
- `bidirectional_flag`

### 设计要点
1. 按有向图建模，而不是简单无向图；
2. `distance` 用于最短距离策略；
3. `ideal_speed` + `crowd_factor` 可支撑最短时间策略；
4. `transport_type` 可为后续交通方式约束预留空间；
5. `edge_type` 可区分道路、楼梯、电梯等类型。

### 建议索引
- `destination_id`
- `from_node_id`
- `to_node_id`
- `transport_type`

---

## 5.8 美食表 `food`

### 表定位
用于保存店铺、窗口和菜品等信息，支撑美食推荐、菜系过滤和模糊查询。

### 主要字段
- `id`
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

### 设计要点
1. `food` 可以归属到某个 `facility`；
2. 同时保留 `destination_id`，方便直接做目的地下的查询；
3. `heat_score` 和 `rating_score` 用于美食推荐排序；
4. `avg_price` 为后续预算维度扩展留接口。

### 建议索引
- `destination_id`
- `name`
- `food_type`
- `shop_name`

---

## 5.9 日记主表 `diary`

### 表定位
用于保存旅游日记主体内容，是旅游后内容记录和交流模块的核心表。

### 主要字段
- `id`
- `user_id`
- `destination_id`
- `title`
- `content_text`
- `content_compressed`（可扩展）
- `heat_score`
- `rating_score`
- `status`
- `created_at`
- `updated_at`

### 设计要点
1. 一篇日记至少关联一个用户和一个目的地；
2. `content_text` 保存正文；
3. `content_compressed` 为压缩存储预留扩展；
4. `heat_score` 和 `rating_score` 支撑排序和推荐；
5. `status` 用于日记可见性或状态控制。

### 建议索引
- `user_id`
- `destination_id`
- `title`
- `(heat_score, rating_score)`

---

## 5.10 日记媒体表 `diary_media`

### 表定位
用于保存日记关联的图片、视频等媒体文件元数据。

### 主要字段
- `id`
- `diary_id`
- `media_type`
- `file_url`
- `file_name`
- `sort_no`

### 设计要点
1. 实际文件不直接放数据库，数据库只存元数据；
2. `sort_no` 用于控制展示顺序；
3. 可支持一篇日记多图、多视频扩展。

### 建议索引
- `diary_id`

---

## 5.11 日记评分表 `diary_rating`

### 表定位
用于保存用户对日记的评分结果，支撑热度 / 评分排序能力。

### 主要字段
- `id`
- `diary_id`
- `user_id`
- `score`
- `created_at`

### 设计要点
1. 一个用户对同一篇日记通常只允许保留一条评分记录；
2. 该表既可支撑展示评分，也可反哺日记排序。

### 建议索引
- `(diary_id, user_id)` 唯一索引

---

## 5.12 路线记录表 `route_history`

### 表定位
用于保存用户历史规划路线，为“我的路线”“路径回顾”和后续 AI 日记联动提供支撑。

### 主要字段
- `id`
- `user_id`
- `destination_id`
- `start_node_id`
- `end_node_id`
- `path_node_json`
- `strategy_type`
- `created_at`

### 设计要点
1. 路线结果先以 JSON 形式保存路径节点序列；
2. 后续若需要更细粒度分析，可拆出更规范的路径明细表；
3. 当前设计足够支撑 MVP 阶段的历史记录与回顾。

### 建议索引
- `user_id`
- `destination_id`
- `created_at`

---

## 5.13 导入批次表 `import_batch`（可选）

### 表定位
用于记录批量导入的数据来源、批次状态和导入时间，便于跟踪导入问题。:contentReference[oaicite:17]{index=17}

### 主要字段
- `id`
- `batch_name`
- `source_type`
- `file_path`
- `status`
- `created_at`

### 设计要点
1. 该表不是首发业务主线必需表；
2. 但对后续真实数据导入和追踪很有帮助；
3. 可在数据导入阶段补充落地。

---

## 6. 数据库设计与系统功能的对应关系

当前数据库设计与系统功能的关系如下：

| 功能方向 | 主要数据表 |
|---|---|
| 用户注册 / 登录 | `user` |
| 用户偏好推荐 | `user_preference`、`destination` |
| 目的地推荐与查询 | `destination`、`place` |
| 路线规划 | `map_node`、`map_edge`、`route_history` |
| 周边设施查询 | `facility`、`map_node`、`map_edge` |
| 美食推荐 | `food`、`facility`、`destination` |
| 日记发布与浏览 | `diary`、`diary_media`、`diary_rating` |
| 数据导入 | `import_batch`（可选） |

---

## 7. 当前阶段数据库落地建议

## 7.1 P0（当前必须优先建表）
以下表建议作为当前 MVP 阶段优先落地：

- `user`
- `user_preference`
- `destination`
- `place`
- `facility`
- `map_node`
- `map_edge`
- `diary`
- `diary_media`
- `route_history`

## 7.2 P1（基础闭环跑通后补充）
- `food`
- `diary_rating`
- `import_batch`

### 当前判断依据
因为当前 MVP 更强调先跑通：
- 推荐
- 路线规划
- 周边设施
- 日记发布 / 浏览

评分、美食增强和导入追踪可以稍后补齐。

---

## 8. 当前设计中的扩展点

当前数据库设计已经为后续增强和创新功能预留了扩展空间：

1. `user_preference` 可继续扩展推荐画像字段  
2. `map_edge` 可继续扩展交通方式与策略字段  
3. `diary` 可继续扩展压缩存储、AI 摘要等字段  
4. `route_history` 可与日记回顾、AI 日记生成联动  
5. `import_batch` 可支撑真实数据导入追踪  
6. 后续创新功能若正式纳入，可再增补协商过程、日记生成素材等新表

---

## 9. 与其他文档的关系

本文件应与以下文档保持一致：

- `docs/01_requirements/prd.md`
- `docs/02_architecture/architecture.md`
- `docs/02_architecture/module-map.md`
- `docs/03_data/data-dictionary.md`
- `docs/03_data/er-model.md`
- `docs/03_data/import-plan.md`
- `docs/04_api/api-spec.md`

当表结构、关系设计或优先级发生变化时，应同步更新上述文档。

---

## 10. 后续维护说明

本文件应在以下场景下更新：

1. 新增核心实体表时  
2. 表关系调整时  
3. 创新需求正式落表时  
4. 路线、日记、推荐等主线数据结构明显变化时  
5. P0 / P1 建表优先级发生调整时