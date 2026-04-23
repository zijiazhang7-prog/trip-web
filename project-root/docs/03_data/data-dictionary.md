- 作用：记录数据库“字段级详细说明”。

# 数据字典（Data Dictionary）

## 1. 文件用途

本文件用于统一说明系统中主要数据表、字段名称、字段类型、用途和约束，保证需求文档、架构文档、数据库设计、接口定义和测试说明之间的一致性。

本文件关注的是：

- 每张表存什么
- 每个字段表示什么
- 字段是否必填、是否主键
- 各表在系统中的用途是什么

本文件不负责说明完整 ER 关系推导和整体结构设计，相关内容请参见：

- `docs/03_data/schema.md`
- `docs/03_data/er-model.md`

---

## 2. 使用原则

1. 本文件中的字段命名应与数据库实现保持一致。
2. 如果数据库表结构发生变化，应优先同步本文件。
3. 接口文档、模块说明、测试文档中涉及数据字段时，应尽量与本文件保持一致。
4. 若 ER 图与表设计说明表存在细节不一致，以当前已确认的项目表设计为准，并在后续同步更新相关文档。

---

## 3. 命名与设计规范

### 3.1 命名规范

- 表名统一使用小写字母 + 下划线
- 主键统一命名为 `id`
- 外键统一命名为 `xxx_id`
- 时间字段统一命名为 `created_at`、`updated_at`

### 3.2 数据库基础建议

- 数据库名称：`tour_system`
- 数据库类型：MySQL 8
- 字符集建议：`utf8mb4`
- 存储引擎建议：`InnoDB`

---

## 4. 数据表总览

|序号|表名|中文名称|用途说明|
|---|---|---|---|
|1|`user`|用户表|保存系统用户基本信息，支持多用户登录与角色区分|
|2|`user_preference`|用户偏好表|保存用户兴趣、风格和推荐偏好|
|3|`destination`|目的地表|保存景区、校园等目的地信息|
|4|`place`|场所/建筑表|保存景点、教学楼、宿舍楼等点位信息|
|5|`facility`|服务设施表|保存厕所、食堂、商店、图书馆等设施信息|
|6|`map_node`|地图节点表|保存有向图顶点，支撑路径规划|
|7|`map_edge`|地图边表|保存有向图边，支撑最短路径与时间策略|
|8|`food`|美食表|保存店铺、菜品、窗口等信息|
|9|`diary`|旅游日记表|保存日记主体内容|
|10|`diary_media`|日记媒体表|保存日记关联的图片、视频等资源|
|11|`diary_rating`|日记评分表|保存用户对日记的评分|
|12|`route_history`|路线记录表|保存用户历史规划路线|
|13|`import_batch`|导入批次表（可选）|记录批量导入的数据来源与状态|

---

## 5. 各数据表数据字典

## 5.1 用户表 `user`

**表功能说明：**  
用于保存系统用户的账号、密码、角色、状态等基本信息，支撑注册、登录、多用户使用和权限区分。

|字段名|数据类型|主键|非空|默认值|字段说明|
|---|---|--:|--:|---|---|
|id|bigint|是|是||用户唯一标识|
|username|varchar(50)|否|是||登录用户名|
|password_hash|varchar(255)|否|是||加密后的密码|
|nickname|varchar(50)|否|否||用户昵称|
|avatar_url|varchar(255)|否|否||用户头像地址|
|role|varchar(20)|否|是|user|用户角色，如 `user/admin`|
|status|tinyint|否|是|1|账号状态，1 启用，0 禁用|
|created_at|datetime|否|是||创建时间|
|updated_at|datetime|否|是||更新时间|

**建议索引：**

- `username` 唯一索引
- `role` 普通索引

---

## 5.2 用户偏好表 `user_preference`

**表功能说明：**  
用于保存用户偏好信息，为推荐模块提供输入。当前设计按“一用户一份偏好配置”处理。

|字段名|数据类型|主键|非空|默认值|字段说明|
|---|---|--:|--:|---|---|
|id|bigint|是|是||主键|
|user_id|bigint|否|是||关联用户 ID|
|prefer_hot_level|int|否|否||对热门程度的偏好|
|prefer_theme|varchar(100)|否|否||偏好主题，如文化/自然/红色|
|prefer_food_type|varchar(100)|否|否||偏好菜系|
|prefer_crowd_level|int|否|否||对拥挤度的接受程度|
|travel_style|varchar(50)|否|否||旅游风格，如小众/打卡/轻松|
|updated_at|datetime|否|是||更新时间|

**建议索引：**

- `user_id` 唯一索引

---

## 5.3 目的地表 `destination`

**表功能说明：**  
用于保存景区、校园等目的地信息，是推荐、搜索、路线规划和日记关联的入口表。

|字段名|数据类型|主键|非空|默认值|字段说明|
|---|---|--:|--:|---|---|
|id|bigint|是|是||主键|
|name|varchar(100)|否|是||目的地名称|
|type|varchar(20)|否|是||目的地类型，如 `scenic/campus`|
|category|varchar(50)|否|否||目的地分类|
|city|varchar(50)|否|否||所在城市|
|description|text|否|否||目的地简介|
|heat_score|decimal(5,2)|否|否|0|热度分|
|rating_score|decimal(3,2)|否|否|0|评分|
|tag_json|text|否|否||标签 JSON|
|cover_url|varchar(255)|否|否||封面图地址|
|status|tinyint|否|否|1|状态|
|created_at|datetime|否|是||创建时间|

**建议索引：**

- `name`
- `type`
- `category`
- `(heat_score, rating_score)`

---

## 5.4 场所/建筑表 `place`

**表功能说明：**  
用于保存目的地内部的景点、教学楼、办公楼、宿舍楼等场所信息，支撑内部查询和导航。

|字段名|数据类型|主键|非空|默认值|字段说明|
|---|---|--:|--:|---|---|
|id|bigint|是|是||主键|
|destination_id|bigint|否|是||所属目的地 ID|
|name|varchar(100)|否|是||场所名称|
|place_type|varchar(50)|否|是||场所类型，如景点/教学楼/宿舍|
|description|text|否|否||场所简介|
|lng|decimal(10,6)|否|否||经度|
|lat|decimal(10,6)|否|否||纬度|
|floor_info|varchar(100)|否|否||楼层信息|
|heat_score|decimal(5,2)|否|否|0|热度分|
|rating_score|decimal(3,2)|否|否|0|评分|

**建议索引：**

- `destination_id`
- `name`
- `place_type`

---

## 5.5 服务设施表 `facility`

**表功能说明：**  
用于保存厕所、食堂、超市、图书馆、咖啡馆等设施信息，支撑周边设施查询。

|字段名|数据类型|主键|非空|默认值|字段说明|
|---|---|--:|--:|---|---|
|id|bigint|是|是||主键|
|destination_id|bigint|否|是||所属目的地 ID|
|place_id|bigint|否|否||所属场所 ID，可为空|
|name|varchar(100)|否|是||设施名称|
|facility_type|varchar(50)|否|是||设施类型，如厕所/食堂/商店|
|description|text|否|否||设施描述|
|lng|decimal(10,6)|否|否||经度|
|lat|decimal(10,6)|否|否||纬度|
|status|tinyint|否|否|1|状态|

**建议索引：**

- `destination_id`
- `facility_type`
- `name`

---

## 5.6 地图节点表 `map_node`

**表功能说明：**  
用于保存有向图顶点，包括路口、建筑物、设施点等，是路径规划和可达性计算的基础。

|字段名|数据类型|主键|非空|默认值|字段说明|
|---|---|--:|--:|---|---|
|id|bigint|是|是||主键|
|destination_id|bigint|否|是||所属目的地 ID|
|node_name|varchar(100)|否|是||节点名称|
|node_type|varchar(50)|否|是||节点类型，如 `intersection/place/facility`|
|ref_id|bigint|否|否||对应场所或设施 ID|
|lng|decimal(10,6)|否|否||经度|
|lat|decimal(10,6)|否|否||纬度|
|floor_no|int|否|否||楼层号|

**建议索引：**

- `destination_id`
- `(node_type, ref_id)`

---

## 5.7 地图边表 `map_edge`

**表功能说明：**  
用于保存有向带权图中的边，支撑最短距离、最短时间、多点路径等算法能力。

|字段名|数据类型|主键|非空|默认值|字段说明|
|---|---|--:|--:|---|---|
|id|bigint|是|是||主键|
|destination_id|bigint|否|是||所属目的地 ID|
|from_node_id|bigint|否|是||起始节点 ID|
|to_node_id|bigint|否|是||终止节点 ID|
|distance|decimal(8,2)|否|是||边长度|
|ideal_speed|decimal(5,2)|否|否||理想速度|
|crowd_factor|decimal(3,2)|否|否||拥挤度系数|
|transport_type|varchar(20)|否|否||交通方式，如 `walk/bike/cart`|
|edge_type|varchar(20)|否|否||边类型，如 `road/stairs/elevator`|
|bidirectional_flag|tinyint|否|否|0|是否双向|

**建议索引：**

- `destination_id`
- `from_node_id`
- `to_node_id`
- `transport_type`

---

## 5.8 美食表 `food`

**表功能说明：**  
用于保存店铺、窗口和菜品等美食信息，支撑美食推荐和筛选。

|字段名|数据类型|主键|非空|默认值|字段说明|
|---|---|--:|--:|---|---|
|id|bigint|是|是||主键|
|destination_id|bigint|否|是||所属目的地 ID|
|facility_id|bigint|否|否||所属设施 ID，可为空|
|name|varchar(100)|否|是||美食名称|
|food_type|varchar(50)|否|否||菜系|
|shop_name|varchar(100)|否|否||店铺 / 窗口名称|
|description|text|否|否||描述|
|heat_score|decimal(5,2)|否|否|0|热度分|
|rating_score|decimal(3,2)|否|否|0|评分|
|avg_price|decimal(8,2)|否|否||平均价格|
|cover_url|varchar(255)|否|否||封面图地址|

**建议索引：**

- `destination_id`
- `name`
- `food_type`
- `shop_name`

---

## 5.9 旅游日记表 `diary`

**表功能说明：**  
用于保存日记主体内容，支撑日记发布、浏览、详情展示和后续推荐、检索能力。

> 说明：为与当前表设计说明表保持一致，本表保留 `content_compressed` 扩展字段；该字段尚未在当前 ER 图主图中展开。

|字段名|数据类型|主键|非空|默认值|字段说明|
|---|---|--:|--:|---|---|
|id|bigint|是|是||主键|
|user_id|bigint|否|是||作者用户 ID|
|destination_id|bigint|否|是||关联目的地 ID|
|title|varchar(150)|否|是||日记标题|
|content_text|longtext|否|是||日记正文|
|content_compressed|longblob|否|否||压缩内容，扩展字段|
|heat_score|decimal(5,2)|否|否|0|热度分|
|rating_score|decimal(3,2)|否|否|0|平均评分|
|status|tinyint|否|否|1|状态|
|created_at|datetime|否|是||创建时间|
|updated_at|datetime|否|是||更新时间|

**建议索引：**

- `user_id`
    
- `destination_id`
    
- `title`
    
- `(heat_score, rating_score)`
    

---

## 5.10 日记媒体表 `diary_media`

**表功能说明：**  
用于保存日记关联的图片、视频等媒体资源元数据。

|字段名|数据类型|主键|非空|默认值|字段说明|
|---|---|--:|--:|---|---|
|id|bigint|是|是||主键|
|diary_id|bigint|否|是||所属日记 ID|
|media_type|varchar(20)|否|是||媒体类型，`image/video`|
|file_url|varchar(255)|否|是||文件访问地址|
|file_name|varchar(100)|否|否||文件名|
|sort_no|int|否|否|0|排序号|

**建议索引：**

- `diary_id`
    

---

## 5.11 日记评分表 `diary_rating`

**表功能说明：**  
用于保存用户对日记的评分结果，支撑评分展示与排序。

|字段名|数据类型|主键|非空|默认值|字段说明|
|---|---|--:|--:|---|---|
|id|bigint|是|是||主键|
|diary_id|bigint|否|是||日记 ID|
|user_id|bigint|否|是||评分用户 ID|
|score|int|否|是||评分值|
|created_at|datetime|否|是||评分时间|

**建议索引：**

- `(diary_id, user_id)` 唯一索引
    

---

## 5.12 路线记录表 `route_history`

**表功能说明：**  
用于保存用户历史规划路线，支撑“我的路线”“路线回顾”和后续日记联动。

|字段名|数据类型|主键|非空|默认值|字段说明|
|---|---|--:|--:|---|---|
|id|bigint|是|是||主键|
|user_id|bigint|否|是||用户 ID|
|destination_id|bigint|否|是||目的地 ID|
|start_node_id|bigint|否|否||起点节点 ID|
|end_node_id|bigint|否|否||终点节点 ID|
|path_node_json|text|否|否||路径节点序列 JSON|
|strategy_type|varchar(50)|否|否||路径策略类型|
|created_at|datetime|否|是||创建时间|

**建议索引：**

- `user_id`
    
- `destination_id`
    
- `created_at`
    

---

## 5.13 导入批次表 `import_batch`（可选）

**表功能说明：**  
用于记录批量导入的数据来源、文件路径和状态，便于排查导入问题。

> 说明：本表在当前表设计说明表中已列出，但未在当前 ER 主图中展开，可作为后续数据导入阶段补充落地的支撑表。

|字段名|数据类型|主键|非空|默认值|字段说明|
|---|---|--:|--:|---|---|
|id|bigint|是|是||主键|
|batch_name|varchar(100)|否|是||批次名称|
|source_type|varchar(50)|否|否||来源类型，如 CSV/JSON/API|
|file_path|varchar(255)|否|否||源文件路径|
|status|varchar(20)|否|否||状态，如成功/失败/处理中|
|created_at|datetime|否|是||创建时间|

---

## 6. 主要表关系摘要

|主表|从表|关系|说明|
|---|---|---|---|
|`user`|`user_preference`|1:1|一个用户对应一份偏好配置|
|`user`|`diary`|1:N|一个用户可发布多篇日记|
|`user`|`diary_rating`|1:N|一个用户可对多篇日记评分|
|`user`|`route_history`|1:N|一个用户可保存多条路线|
|`destination`|`place`|1:N|一个目的地下有多个场所|
|`destination`|`facility`|1:N|一个目的地下有多个设施|
|`destination`|`map_node`|1:N|一个目的地下有多个地图节点|
|`destination`|`map_edge`|1:N|一个目的地下有多条地图边|
|`destination`|`food`|1:N|一个目的地下有多条美食数据|
|`destination`|`diary`|1:N|一个目的地关联多篇日记|
|`place`|`facility`|1:N|一个场所可包含多个设施|
|`facility`|`food`|1:N|一个设施可关联多个美食|
|`diary`|`diary_media`|1:N|一篇日记可关联多个媒体文件|
|`diary`|`diary_rating`|1:N|一篇日记可收到多个评分|

---

## 7. 当前阶段落地建议

### 7.1 P0（当前优先）

建议当前先优先保证以下表可落地并可用：

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

### 7.2 P1（基础闭环后补充）

- `food`
- `diary_rating`
- `import_batch`

### 7.3 原因

这样可以先跑通：

- 用户登录
- 推荐与搜索
- 路径规划
- 周边设施查询
- 图文日记发布 / 浏览

---

## 8. 后续维护说明

本文件应在以下场景下更新：

1. 新增表或字段时
2. 字段类型、约束或默认值发生变化时
3. ER 图与实际表结构出现偏差时
4. 创新功能正式落表时
5. 数据导入、日记增强、推荐增强引入新字段时