- 作用：写接口定义。

# API 接口定义文档（API Specification）

## 1. 文档用途
本文件用于说明个性化旅游系统后端 API 的整体设计、接口分组、请求与响应格式、认证方式、主要数据对象和核心接口定义，为前后端并行开发、接口联调、测试执行和答辩说明提供依据。

本文件关注的是：

- 系统对外提供了哪些接口
- 各接口的输入输出是什么
- 哪些接口属于当前 MVP 必做范围
- 前后端联调时应遵循哪些统一约定
- 哪些接口属于创新需求预留能力

相关文档：

- `project-root/docs/04_api/error-codes.md`
- `project-root/docs/04_api/api-changelog.md`
- `project-root/docs/05_modules/*`
- `project-root/docs/03_data/schema.md`

---

## 2. 接口设计原则

### 2.1 基础风格
- 接口风格：RESTful
- 基础前缀：`/api/v1`
- 数据格式：`application/json`
- 文件上传：`multipart/form-data`
- 认证方式：JWT Bearer Token
- 字符集：UTF-8

### 2.2 设计原则
1. 路径语义清晰，尽量用名词而不是动词。
2. GET 接口一般不使用 JSON Body。
3. POST / PUT 接口优先使用 JSON Body。
4. 文件上传接口单独使用 `multipart/form-data`。
5. 所有接口统一返回结构。
6. 受保护接口统一通过 Bearer Token 认证。
7. 管理端接口统一加 `/admin` 前缀。
8. 当前基础主线不依赖外部 AI 模型或外部地图 API 才能运行。
9. AI 接口当前属于 P2 预留 / 增强能力，不阻塞 MVP 主链路。

---

## 3. 通用请求与响应约定

## 3.1 通用请求头
```http
Content-Type: application/json
Authorization: Bearer <token>
````

## 3.2 通用成功响应结构

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "success",
  "data": {},
  "timestamp": "2026-04-29T12:00:00"
}
```

## 3.3 通用失败响应结构

```json
{
  "success": false,
  "code": "AUTH_003",
  "message": "未登录或登录状态已失效",
  "data": null,
  "timestamp": "2026-04-29T12:00:00"
}
```

## 3.4 通用分页响应结构

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "success",
  "data": {
    "list": [],
    "pageNum": 1,
    "pageSize": 10,
    "total": 100,
    "pages": 10
  },
  "timestamp": "2026-04-29T12:00:00"
}
```

### 说明

* `success`：布尔值，表示本次业务调用是否成功
* `code`：业务码，成功时建议为 `SUCCESS`，失败时参见 `error-codes.md`
* `message`：简洁提示信息
* `data`：具体业务数据
* `timestamp`：响应时间

---

## 4. 认证与权限约定

### 4.1 无需登录即可访问

* 用户注册
* 用户登录
* 推荐列表
* 目的地搜索
* 目的地详情
* 场所列表
* 基础设施浏览
* 基础美食浏览
* 日记列表与公开日记详情

### 4.2 需要登录

* 获取当前用户信息
* 获取 / 更新当前用户偏好
* 发布日记
* 查看我的路线历史
* 对日记评分
* 上传文件
* 调用需要用户上下文的 AI 能力
* 查看私有日记或编辑中的草稿（后续）

### 4.3 需要管理员权限

* 管理端所有接口
* 批量导入接口
* 数据维护接口

---

## 5. 接口分组总览

当前 API 按以下分组组织：

1. Health：工程健康检查
2. Auth：用户认证
3. UserPreference：用户偏好
4. Destination：目的地推荐与查询
5. Route：路线规划与路线历史
6. Facility：周边设施
7. Food：美食查询与推荐
8. Diary：旅游日记
9. File：文件上传
10. Admin：管理端数据维护
11. AI：AI 增强能力

### 5.1 Health 接口

#### 5.1.1 后端健康检查

* 方法：`GET`
* 路径：`/api/v1/health`
* 权限：无需登录

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "success",
  "data": {
    "status": "ok"
  },
  "timestamp": "2026-05-05T12:00:00"
}
```

说明：该接口用于工程骨架启动验证和前后端联调探活，不承载业务逻辑。

---

## 6. 核心数据对象定义

## 6.1 UserVO

```json
{
  "id": 1,
  "username": "alice01",
  "nickname": "Alice",
  "avatarUrl": "/files/avatar/a1.png",
  "role": "user"
}
```

## 6.2 UserPreferenceVO

```json
{
  "userId": 1,
  "preferHotLevel": 3,
  "preferThemeList": ["人文建筑型", "自然景观型"],
  "preferFoodType": "面食",
  "preferCrowdLevel": 2,
  "travelStyle": "轻松",
  "customPreferenceText": "更喜欢安静、人少、适合拍照的地方",
  "updatedAt": "2026-04-29T10:00:00"
}
```

## 6.3 DestinationVO

```json
{
  "id": 101,
  "name": "北京邮电大学沙河校区",
  "type": "campus",
  "category": "校园",
  "city": "北京",
  "description": "校园简介",
  "heatScore": 87.5,
  "ratingScore": 4.6,
  "coverUrl": "/files/destination/d101.jpg",
  "tags": ["校园", "文化", "春季"]
}
```

## 6.4 PlaceVO

```json
{
  "id": 1001,
  "destinationId": 101,
  "name": "图书馆",
  "placeType": "building",
  "description": "图书馆简介",
  "lng": 116.123456,
  "lat": 40.123456,
  "floorInfo": "B1-5F",
  "openTimeRule": "08:00-22:00",
  "suggestedDurationMin": 60,
  "costLevel": 1
}
```

## 6.5 FacilityVO

```json
{
  "id": 2001,
  "destinationId": 101,
  "placeId": 1001,
  "name": "一层卫生间",
  "facilityType": "toilet",
  "description": "靠近大厅",
  "address": "图书馆一层大厅东侧",
  "tel": "010-12345678",
  "coverUrl": "/files/facility/f2001.jpg",
  "lng": 116.123001,
  "lat": 40.123002
}
```

## 6.6 FoodVO

```json
{
  "id": 3001,
  "destinationId": 101,
  "facilityId": 2005,
  "name": "牛肉面",
  "foodType": "面食",
  "shopName": "第一食堂一层",
  "description": "招牌牛肉面",
  "heatScore": 82.0,
  "ratingScore": 4.5,
  "avgPrice": 18.0,
  "coverUrl": "/files/food/f3001.jpg",
  "lng": 116.123100,
  "lat": 40.123200
}
```

## 6.7 DiaryVO

```json
{
  "id": 4001,
  "userId": 1,
  "username": "alice01",
  "destinationId": 101,
  "destinationName": "北京邮电大学沙河校区",
  "routeHistoryId": 9001,
  "title": "今天在校园里散步",
  "contentText": "正文摘要",
  "heatScore": 25,
  "ratingScore": 4.8,
  "visibility": "public",
  "mediaList": [
    {
      "id": 5001,
      "mediaType": "image",
      "fileUrl": "/files/diary/d5001.jpg"
    }
  ],
  "createdAt": "2026-04-29T10:00:00"
}
```

## 6.8 RoutePlanVO

```json
{
  "destinationId": 101,
  "strategyType": "shortest_distance",
  "transportType": "walk",
  "totalDistance": 1260.5,
  "estimatedTime": 18,
  "pathNodes": [
    { "nodeId": 1, "nodeName": "校门" },
    { "nodeId": 5, "nodeName": "图书馆" },
    { "nodeId": 9, "nodeName": "食堂" }
  ],
  "pathEdges": [
    { "fromNodeId": 1, "toNodeId": 5, "distance": 500.0 },
    { "fromNodeId": 5, "toNodeId": 9, "distance": 760.5 }
  ],
  "routeSummary": "从校门步行前往图书馆，再到食堂",
  "historyId": 9001
}
```

## 6.9 RouteHistoryVO

```json
{
  "id": 9001,
  "destinationId": 101,
  "destinationName": "北京邮电大学沙河校区",
  "startNodeId": 1,
  "endNodeId": 9,
  "strategyType": "shortest_distance",
  "transportType": "walk",
  "totalDistance": 1260.5,
  "estimatedTime": 18,
  "createdAt": "2026-04-29T09:30:00"
}
```

## 6.10 FileUploadResultVO

```json
{
  "bizType": "diary",
  "fileName": "photo-1.jpg",
  "fileUrl": "/files/diary/d5001.jpg"
}
```

说明：当前 P0 表结构没有独立的文件资源表，上传接口只返回文件访问地址，不返回持久化文件 ID。后续 Diary 发布基础版应直接携带文件 URL 或媒体对象列表。

## 6.11 DiaryDraftVO（AI）

```json
{
  "titleSuggestion": "春日校园散步记",
  "contentDraft": "今天天气很好，我从校门出发，先去了图书馆，再去食堂吃了午饭。",
  "tagList": ["校园", "春天", "散步"],
  "summary": "一段轻松的校园游览记录"
}
```

## 6.12 ImageSummaryVO（AI）

```json
{
  "imageSummary": "画面中是一座现代风格的图书馆建筑，天空晴朗。",
  "tagList": ["图书馆", "建筑", "校园"],
  "sceneType": "campus_building"
}
```

## 6.13 RouteReviewVO（AI）

```json
{
  "routeSummary": "本次路线从校门出发，依次经过图书馆和食堂。",
  "highlightList": ["图书馆打卡", "食堂午餐"],
  "tripReviewText": "整体路线较轻松，适合半日游览。"
}
```

## 6.14 GroupPlanSummaryVO（AI）

```json
{
  "finalPlanSummary": "建议优先选择兼顾人文参观和轻松步行的路线方案 B。",
  "reasonList": ["更符合两位成员共同偏好", "总步行距离较短"],
  "conflictPoints": ["一位成员更偏好自然景观"],
  "suggestionList": ["下午增加校园湖边区域作为补充"]
}
```

---

## 7. Auth 接口

## 7.1 用户注册

* 方法：`POST`
* 路径：`/api/v1/auth/register`

### Request Body

```json
{
  "username": "alice01",
  "password": "123456",
  "nickname": "Alice"
}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "created",
  "data": {
    "userId": 1
  },
  "timestamp": "2026-04-29T12:00:00"
}
```

## 7.2 用户登录

* 方法：`POST`
* 路径：`/api/v1/auth/login`

### Request Body

```json
{
  "username": "alice01",
  "password": "123456"
}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "success",
  "data": {
    "token": "jwt-token-string",
    "user": {
      "id": 1,
      "username": "alice01",
      "nickname": "Alice",
      "role": "user"
    }
  },
  "timestamp": "2026-04-29T12:00:00"
}
```

## 7.3 获取当前用户信息

* 方法：`GET`
* 路径：`/api/v1/auth/me`

### Response

返回 `UserVO`

---

## 8. UserPreference 接口

## 8.1 获取当前用户偏好

* 方法：`GET`
* 路径：`/api/v1/user-preferences/me`

### Response

返回 `UserPreferenceVO`

## 8.2 保存 / 更新当前用户偏好

* 方法：`PUT`
* 路径：`/api/v1/user-preferences/me`

### Request Body

```json
{
  "preferHotLevel": 3,
  "preferThemeList": ["人文建筑型", "自然景观型"],
  "preferFoodType": "面食",
  "preferCrowdLevel": 2,
  "travelStyle": "轻松",
  "customPreferenceText": "更喜欢安静、人少、适合拍照的地方"
}
```

### Response

返回 `UserPreferenceVO`

---

## 9. Destination 接口

## 9.1 获取推荐目的地列表

* 方法：`GET`
* 路径：`/api/v1/destinations/recommend`

### Query 参数

| 参数名      | 类型     | 必填 | 说明                      |
| -------- | ------ | -: | ----------------------- |
| type     | string |  否 | `scenic/campus`         |
| theme    | string |  否 | 主题                      |
| destType | string | 否 | 标准目的地类型；仅在 `sortBy=recommend` 时参与加权，不过滤未命中项 |
| interestTags | string[] | 否 | 标准兴趣标签，可重复传参；仅在 `sortBy=recommend` 时参与加权 |
| sortBy   | string |  否 | `heat/rating/recommend` |
| pageNum  | int    |  否 | 页码，默认 1；未传 `topK` 时生效 |
| pageSize | int    |  否 | 每页数量，默认 10，最大 100；未传 `topK` 时生效 |
| topK     | int    |  否 | Top-K 推荐数，范围 1～100；传入后优先于分页参数 |

### Response

返回 `PageResultVO<DestinationVO>`。

说明：

- 未传 `topK` 时，对全部符合 `status=1`、`type`、`theme` 条件的候选排序后分页。
- 传入 `topK` 时保持兼容模式，忽略 `pageNum/pageSize`，响应 `pageNum=1`、`pageSize=topK`。
- `total` 始终表示过滤后的完整候选数量，`pages=ceil(total/pageSize)`。
- `sortBy=recommend` 时，请求标签与当前登录用户的 `preferThemeList` 合并；匿名请求也可使用
  `destType/interestTags`。
- `sortBy=heat/rating` 时忽略标准标签加权，保持原排序语义。
- `DestinationVO` 追加只读字段 `destType/interestTags`；原 `type/category/tags` 字段不变。

## 9.2 搜索目的地

* 方法：`GET`
* 路径：`/api/v1/destinations/search`

### Query 参数

| 参数名      | 类型     | 必填 | 说明              |
| -------- | ------ | -: | --------------- |
| keyword  | string |  是 | 名称 / 类别 / 关键字   |
| type     | string |  否 | `scenic/campus` |
| sortBy   | string |  否 | `heat/rating`   |
| pageNum  | int    |  否 | 页码              |
| pageSize | int    |  否 | 每页条数            |

### Response

返回 `Page<DestinationVO>`

## 9.3 获取目的地详情

* 方法：`GET`
* 路径：`/api/v1/destinations/{id}`

### Response

返回 `DestinationVO`

## 9.4 获取目的地下的场所列表

* 方法：`GET`
* 路径：`/api/v1/destinations/{id}/places`

### Query 参数

| 参数名       | 类型     | 必填 | 说明     |
| --------- | ------ | -: | ------ |
| placeType | string |  否 | 场所类型筛选 |

### Response

返回 `List<PlaceVO>`

## 9.5 获取目的地相关日记列表

* 方法：`GET`
* 路径：`/api/v1/destinations/{id}/diaries`

### Query 参数

| 参数名      | 类型     | 必填 | 说明                   |
| -------- | ------ | -: | -------------------- |
| sortBy   | string |  否 | `heat/rating/latest` |
| pageNum  | int    |  否 | 页码                   |
| pageSize | int    |  否 | 每页数量                 |

### Response

返回 `Page<DiaryVO>`

---

## 10. Route 接口

## 10.1 单目标路线规划

* 方法：`POST`
* 路径：`/api/v1/routes/plan/single`
* 当前实现状态：已实现，支持 `shortest_distance` 与 `shortest_time`
* 权限：需要登录

### Request Body

```json
{
  "destinationId": 101,
  "startNodeId": 1,
  "targetNodeId": 9,
  "strategyType": "shortest_distance",
  "transportType": "walk"
}
```

### Response

返回 `RoutePlanVO`

说明：
- `strategyType` 当前支持 `shortest_distance` 和 `shortest_time`。
- `shortest_distance` 以 `map_edge.distance` 作为 Dijkstra 边权。
- `transportType` 支持 `walk`、`bike`、`cart`、`mixed`，默认 `walk`。
- `walk/bike/cart` 会按 `map_edge.transport_type` 的组合通行权限过滤道路。
- `shortest_time` 以 `distance / (min(交通工具默认速度, 道路 ideal_speed) * crowd_factor)` 作为 Dijkstra 边权；道路速度为空时使用交通工具默认速度，`estimatedTime` 按分钟返回。
- `mixed` 当前只支持 `shortest_time`，采用任意公共节点零换乘成本的课程 MVP。
- `pathEdges[].transportType` 返回该条路径边实际使用的交通工具。

## 10.2 多目标路线规划

* 方法：`POST`
* 路径：`/api/v1/routes/plan/multi`
* 当前实现状态：已实现，P1 Route 多目标基础版，支持 `shortest_distance` 与 `shortest_time`
* 权限：需要登录

### Request Body

```json
{
  "destinationId": 101,
  "startNodeId": 1,
  "targetNodeIds": [5, 9, 12],
  "strategyType": "shortest_distance",
  "transportType": "walk",
  "returnToStart": true
}
```

### Response

返回 `RoutePlanVO`

说明：
- 当前多目标基础版使用内部 `MapService` 的有向带权图和 Dijkstra 能力，不依赖外部地图 API。
- 当前支持 `strategyType=shortest_distance` 与 `strategyType=shortest_time`。
- 单一交通方式和 `mixed` 规则与单目标接口一致，多目标每一段及返回起点段均使用同一交通约束。
- `targetNodeIds` 不能为空，当前最多支持 8 个目标节点，且不允许重复。
- 多目标访问顺序采用最近邻启发式：每次选择从当前节点到未访问目标中当前策略权重最小的一点；结果不保证 TSP 全局最优。
- 如果 `returnToStart=true`，系统会在访问完目标点后追加返回起点的最短路径。
- 返回结构复用 `RoutePlanVO`，`pathNodes` / `pathEdges` 为拼接后的完整路线，`historyId` 为写入的路线历史记录 ID。

## 10.3 获取当前用户路线历史

* 方法：`GET`
* 路径：`/api/v1/routes/history`

### Query 参数

| 参数名      | 类型  | 必填 | 说明   |
| -------- | --- | -: | ---- |
| pageNum  | int |  否 | 页码   |
| pageSize | int |  否 | 每页数量 |

### Response

返回 `PageResultVO<RouteHistoryVO>`。

列表项包含：

- `id`
- `destinationId`、`destinationName`
- `startNodeId`、`startNodeName`
- `endNodeId`、`endNodeName`
- `strategyType`、`transportType`
- `totalDistance`、`estimatedTime`
- `createdAt`

说明：

- 只返回当前 JWT 用户自己的路线历史，不接收 `userId`。
- 默认 `pageNum=1`、`pageSize=10`，`pageSize` 最大为 100。
- 按 `createdAt DESC, id DESC` 排序。
- 列表不返回完整路径数组，完整快照通过详情接口读取。

## 10.4 获取单条路线历史详情

* 方法：`GET`
* 路径：`/api/v1/routes/history/{id}`

### Response

返回 `RouteHistoryVO`，除摘要字段外包含：

- `pathNodes`
- `pathEdges`
- `orderedTargetNodeIds`

说明：

- 详情只允许当前用户读取自己的记录；记录不存在或属于其他用户时统一返回 `COMMON_003`。
- 历史查询直接读取 `route_history` 中保存的 JSON 快照，不重新调用 `MapService` 或 `GraphEngine`。
- 新生成的多目标历史保存 `orderedTargetNodeIds`；单目标和迁移前旧记录返回空数组。

## 10.5 查询可用室内导航建筑

* 方法：`GET`
* 路径：`/api/v1/indoor/buildings`
* 权限：公开

Query 参数：`destinationId`，必须为正整数。

返回 `List<IndoorBuildingVO>`，包含 `buildingId`、`destinationId`、`buildingName`、
`placeType`、`floorInfo` 和 `floorNos`。只返回已经配置 `map_node.place_id` 与楼层节点的建筑。

## 10.6 获取建筑室内楼层图

* 方法：`GET`
* 路径：`/api/v1/indoor/buildings/{buildingId}/map`
* 权限：公开

返回 `IndoorMapVO`：

- `building`：建筑摘要与可用楼层；
- `nodes`：节点 ID、名称、`nodeType`、`floorNo`、归一化 `x/y`；
- `edges`：边 ID、起终节点及楼层、`edgeType`、`distance`。

前端可使用 `viewBox="0 0 1000 600"` 按 `floorNo` 分组绘制 SVG。跨层
`elevator/stair` 边建议显示为垂直通行图标，不直接跨楼层连线。

## 10.7 室内单目标路线规划

* 方法：`POST`
* 路径：`/api/v1/indoor/routes/plan`
* 权限：需要登录

请求：

```json
{
  "destinationId": 101,
  "buildingId": 201,
  "startNodeId": 10001,
  "targetNodeId": 10015,
  "strategyType": "shortest_time",
  "verticalMode": "elevator"
}
```

说明：

- `strategyType` 支持 `shortest_distance/shortest_time`，默认 `shortest_time`；
- `verticalMode` 支持 `elevator/stair/any`，默认 `any`；
- `elevator` 允许 `corridor + elevator`，`stair` 允许 `corridor + stair`；
- `any` 允许三类室内边，并由 Dijkstra 按当前策略选择；
- 室内接口的 `totalTime`、`pathEdges[].timeCost` 单位固定为秒；
- `pathNodes` 包含 `floorNo/nodeType/x/y`，`pathEdges` 包含
  `edgeType/distance/timeCost`，`steps` 返回可直接展示的中文路径步骤；
- 当前室内路线不写入 `route_history`，不影响室外路线历史契约。

---

## 11. Facility 接口

## 11.1 查询附近设施

* 方法：`GET`
* 路径：`/api/v1/facilities/nearby`

### Query 参数

| 参数名           | 类型     | 必填 | 说明              |
| ------------- | ------ | -: | --------------- |
| destinationId | long   |  是 | 目的地 ID          |
| sourceNodeId  | long   |  是 | 当前节点 ID         |
| facilityType  | string |  否 | 设施类型            |
| radius        | int    |  否 | 搜索范围，单位米        |
| sortBy        | string |  否 | `distance/time` |

### Response

返回 `Page<FacilityVO>`

## 11.2 搜索设施

* 方法：`GET`
* 路径：`/api/v1/facilities/search`

### Query 参数

| 参数名           | 类型     | 必填 | 说明              |
| ------------- | ------ | -: | --------------- |
| destinationId | long   |  是 | 目的地 ID          |
| keyword       | string |  是 | 名称 / 类别关键字      |
| facilityType  | string |  否 | 设施类型            |
| sortBy        | string |  否 | `distance/time` |
| pageNum       | int    |  否 | 页码              |
| pageSize      | int    |  否 | 每页数量            |

### Response

返回 `Page<FacilityVO>`

---

## 12. Food 接口

## 12.1 获取美食推荐列表

* 方法：`GET`
* 路径：`/api/v1/foods/recommend`

### Query 参数

| 参数名           | 类型     | 必填 | 说明                     |
| ------------- | ------ | -: | ---------------------- |
| destinationId | long   |  是 | 目的地 ID                 |
| facilityId    | long   |  否 | 所属设施 ID                |
| foodType      | string |  否 | 菜系                     |
| cuisineTags   | string[] | 否 | 标准口味标签，可重复传参；用于推荐排序，不作为 SQL 硬过滤条件 |
| sortBy        | string |  否 | 当前支持 `heat/rating` |
| pageNum       | int    |  否 | 页码，默认 1；未传 `topK` 时生效 |
| pageSize      | int    |  否 | 每页数量，默认 10，最大 100；未传 `topK` 时生效 |
| topK          | int    |  否 | Top-K 数量，范围 1～100；传入后优先于分页参数 |

### Response

返回 `PageResultVO<FoodVO>`。

说明：

- 未传 `topK` 时，对目的地下全部匹配候选排序后按 `pageNum/pageSize` 分页。
- 传入 `topK` 时保持兼容模式，忽略分页参数，只返回前 K 条。
- `total` 表示过滤后的完整候选数量。
- 传入 `cuisineTags` 时，名称、`foodType`、店铺名命中的条目优先，未命中条目仍保留；
  同一匹配层级内继续使用 `sortBy=heat/rating`。
- `FoodVO` 追加只读字段 `cuisineTags`，由运行时标签映射生成，不新增数据库列。

## 12.2 搜索美食

* 方法：`GET`
* 路径：`/api/v1/foods/search`

### Query 参数

| 参数名           | 类型     | 必填 | 说明                     |
| ------------- | ------ | -: | ---------------------- |
| destinationId | long   |  是 | 目的地 ID                 |
| keyword       | string |  是 | 名称 / 菜系 / 店铺关键字        |
| foodType      | string |  否 | 菜系过滤                   |
| sortBy        | string |  否 | 当前基础版支持 `heat/rating`，`distance` 后续联动 MapService 再补 |
| pageNum       | int    |  否 | 页码                     |
| pageSize      | int    |  否 | 每页数量                   |

### Response

返回 `Page<FoodVO>`

说明：Food 当前支持按目的地 / 设施 / 原始 `foodType` / 关键字召回，并复用
`RankService` 做热度、评分、标准口味标签加权、Top-K 或分页输出；价格区间、距离联动和详情接口后续再补。

---

## 13. Diary 接口

## 13.1 发布日记

* 方法：`POST`
* 路径：`/api/v1/diaries`

### Request Body

```json
{
  "destinationId": 101,
  "routeHistoryId": 9001,
  "title": "今天在校园里散步",
  "contentText": "今天天气很好，我先去了图书馆，再去了食堂。",
  "visibility": "public",
  "mediaList": [
    {
      "mediaType": "image",
      "fileUrl": "/files/diary/20260505/photo-1.jpg",
      "fileName": "photo-1.jpg",
      "sortNo": 0
    }
  ]
}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "created",
  "data": {
    "diaryId": 4001
  },
  "timestamp": "2026-04-29T12:00:00"
}
```

## 13.2 获取日记列表

* 方法：`GET`
* 路径：`/api/v1/diaries`

### Query 参数

| 参数名           | 类型     | 必填 | 说明                   |
| ------------- | ------ | -: | -------------------- |
| destinationId | long   |  否 | 目的地 ID               |
| destinationKeyword | string | 否 | 目的地名称关键字，最大 100 字符；支持精确、前缀和包含匹配 |
| sortBy        | string |  否 | `heat/rating/latest` |
| pageNum       | int    |  否 | 页码                   |
| pageSize      | int    |  否 | 每页数量                 |

说明：

- `destinationKeyword` 先由 `QueryService` 使用目的地名称 Hash/Trie 索引获取候选，并保留 MySQL `name LIKE` 包含匹配兜底。
- 匹配多个目的地时，统一查询这些目的地下公开且启用的日记。
- `destinationId` 与 `destinationKeyword` 同时传入时按交集过滤；无匹配返回正常空分页。

### Response

返回 `Page<DiaryVO>`

## 13.2A 获取当前用户个性化日记推荐

* 方法：`GET`
* 路径：`/api/v1/diaries/recommend`
* 权限：需要 JWT 登录
* 当前实现状态：已实现

### Query 参数

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| sortBy | string | 否 | `interest/heat/rating`，默认 `interest` |
| pageSize | int | 否 | Top-K 数量，默认 10，范围 1～100 |

### Response

返回现有 `PageResultVO<DiaryVO>`，不增加 `recommendScore` 或推荐理由字段。

说明：

- `interest` 读取当前用户 `user_preference`，使用偏好主题、旅游风格、美食偏好和自由偏好文本匹配日记标题、正文及目的地特征。
- 综合分由兴趣匹配、归一化浏览热度和归一化评分组成，权重分别为 0.50、0.30、0.20。
- 用户没有有效文本偏好时，`interest` 自动降级为热度 Top-K。
- 候选只包含 `status=1`、`visibility=public` 的日记，最多召回最近 200 条。
- Top-K 由 `RankService` 的小顶堆实现；推荐列表不会触发详情浏览量自增。

## 13.3 获取日记详情

* 方法：`GET`
* 路径：`/api/v1/diaries/{id}`

成功通过状态与可见性校验后，后端会将该日记的 `heatScore` 原子增加 1，并在本次响应中返回更新后的浏览量。不存在、禁用或当前用户无权查看的日记不会增加浏览量。

### Response

返回 `DiaryVO`

## 13.4 按标题精确查询日记

* 方法：`GET`
* 路径：`/api/v1/diaries/search/title`
* 当前实现状态：已实现，P1 SearchService 基础版，支持基础排序

### Query 参数

| 参数名   | 类型     | 必填 | 说明   |
| ----- | ------ | -: | ---- |
| title | string |  是 | 日记标题 |
| sortBy | string | 否 | `latest/heat/rating`，默认 `latest` |
| pageNum | int | 否 | 页码 |
| pageSize | int | 否 | 每页数量，最大 100 |

### Response

返回 `Page<DiaryVO>`

说明：当前基础版内部使用 MySQL `LIKE` 做标题匹配，只返回 `status=1` 且 `visibility=public` 的日记；排序字段使用白名单校验，非法值返回参数错误。

## 13.5 日记全文检索

* 方法：`GET`
* 路径：`/api/v1/diaries/search/fulltext`
* 当前实现状态：已实现，P1 SearchService 基础版，支持基础排序

### Query 参数

| 参数名           | 类型     | 必填 | 说明      |
| ------------- | ------ | -: | ------- |
| keyword       | string |  是 | 日记正文关键字 |
| destinationId | long   |  否 | 目的地 ID  |
| sortBy        | string |  否 | `latest/heat/rating`，默认 `latest` |
| pageNum       | int    |  否 | 页码      |
| pageSize      | int    |  否 | 每页数量，最大 100 |

### Response

返回 `Page<DiaryVO>`

说明：当前基础版内部使用 MySQL `LIKE` 匹配 `content_text`，只返回公开且启用的日记；排序字段使用白名单校验，非法值返回参数错误；倒排索引或 MySQL FULLTEXT 后续再增强。

## 13.6 对日记评分

* 方法：`POST`
* 路径：`/api/v1/diaries/{id}/ratings`
* 当前实现状态：已实现，需要 JWT 登录

业务规则：

- 评分范围为 1～5；
- 只允许评分 `status=1` 且 `visibility=public` 的日记；
- 同一用户再次评分时更新原评分记录；
- 允许作者评分自己的日记；
- 评分明细、平均分和评分人数在同一事务内更新。

### Request Body

```json
{
  "score": 5
}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "success",
  "data": true,
  "timestamp": "2026-04-29T12:00:00"
}
```

为保持既有前端契约兼容，提交接口继续返回 `Boolean`。最新个人评分和聚合结果通过下方查询接口获取。

## 13.6A 获取当前用户对日记的评分

* 方法：`GET`
* 路径：`/api/v1/diaries/{id}/ratings/me`
* 当前实现状态：已实现，需要 JWT 登录

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "success",
  "data": {
    "diaryId": 1,
    "userScore": 5,
    "ratingScore": 4.6,
    "ratingCount": 12
  },
  "timestamp": "2026-06-10T16:00:00"
}
```

当前用户尚未评分时，`userScore` 为 `null`，聚合字段仍返回当前日记值。

## 13.7 获取我的日记列表（建议补充）

* 方法：`GET`
* 路径：`/api/v1/diaries/me`

### Query 参数

| 参数名        | 类型     | 必填 | 说明               |
| ---------- | ------ | -: | ---------------- |
| pageNum    | int    |  否 | 页码               |
| pageSize   | int    |  否 | 每页数量             |
| visibility | string |  否 | `public/private` |

### Response

返回 `Page<DiaryVO>`

---

## 13.8 景点、美食、日记评论基础版

### 13.8.1 接口

|功能|方法|路径|权限|
|---|---|---|---|
|目的地评论列表|GET|`/api/v1/destinations/{destinationId}/comments`|公开|
|发布目的地评论|POST|`/api/v1/destinations/{destinationId}/comments`|登录用户|
|美食评论列表|GET|`/api/v1/foods/{foodId}/comments`|公开|
|发布美食评论|POST|`/api/v1/foods/{foodId}/comments`|登录用户|
|日记评论列表|GET|`/api/v1/diaries/{diaryId}/comments`|公开|
|发布日记评论|POST|`/api/v1/diaries/{diaryId}/comments`|登录用户|
|删除或隐藏评论|DELETE|`/api/v1/comments/{commentType}/{commentId}`|登录用户|

列表参数为 `pageNum`、`pageSize`，默认值分别为 1、10，`pageSize` 最大为 100。列表只返回
`parent_comment_id IS NULL` 且 `status=1` 的一级评论，按 `created_at DESC, id DESC` 排序。

发布请求：

```json
{
  "contentText": "环境很好，适合周末参观。"
}
```

`contentText` 会去除首尾空白，不能为空，最大 500 个字符。第一阶段不接收
`parentCommentId`、`mediaUrl`、点赞或回复字段。

评论响应字段：

```json
{
  "id": 1,
  "targetType": "destination",
  "targetId": 10,
  "userId": 3,
  "nickname": "测试用户",
  "avatarUrl": null,
  "contentText": "环境很好，适合周末参观。",
  "createdAt": "2026-06-11T20:00:00"
}
```

业务约束：

- 目的地必须存在且 `status=1`。
- 美食表当前没有状态字段，因此只校验美食记录存在。
- 日记必须满足 `status=1`、`visibility=public`。
- 普通用户只能删除自己的正常评论，删除后置 `status=2`。
- 管理员可隐藏任意正常评论，隐藏后置 `status=0`。
- `commentType` 只允许 `destination`、`food`、`diary`。
- 当前不维护目的地、美食、日记主表的评论数聚合字段。

## 13.9 AIGC 日记照片动画

当前实现状态：已实现后端 MVP 和可配置的 OpenAI-compatible 多模态 Provider。默认仍使用离线 `mock-template`；配置真实 Provider 后会读取已落库日记图片并生成结构化动画脚本。外部调用失败、超时或返回非法 JSON 时自动降级到模板结果。当前不导出 MP4。

### 13.9.1 生成或重新生成动画

* 方法：`POST`
* 路径：`/api/v1/diaries/{diaryId}/animation`
* 权限：需要 JWT，且仅日记作者可调用
* Request Body：无

业务规则：

- 日记必须存在且 `status=1`；
- 作者可为自己的公开或私有日记生成动画；
- 只读取该日记已落库且 `media_type=image` 的 `diary_media`；
- 请求不接受图片 URL，脚本不能引用其他日记媒体或任意外链；
- 至少需要一张图片，否则返回 `AI_009`；
- 重复生成覆盖同一条 `diary_animation` 记录。
- 真实 Provider 只读取 `/files/diary/...` 对应的本地图片，不接受请求传入任意外链；
- 默认最多处理 6 张图片，单图默认不超过 2MB，总图片默认不超过 8MB；
- AI 调用在数据库事务外执行，保存前会重新校验日记和媒体快照；
- `provider=openai-compatible` 表示真实多模态调用成功，`provider=mock-template` 表示默认模板或降级结果。

### 13.9.2 查询动画

* 方法：`GET`
* 路径：`/api/v1/diaries/{diaryId}/animation`
* 权限：公开日记允许匿名查询；私有日记仅作者查询

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "success",
  "data": {
    "id": 1,
    "diaryId": 101,
    "provider": "openai-compatible",
    "title": "北京邮电大学沙河校区 · 校园漫步 动画回顾",
    "narration": "今天沿着校园主路游览了图书馆。",
    "status": "ready",
    "script": {
      "schemaVersion": "1.0",
      "aspectRatio": "16:9",
      "totalDurationMs": 4000,
      "backgroundMusic": "light-travel",
      "scenes": [
        {
          "order": 1,
          "mediaId": 501,
          "fileUrl": "/files/diary/20260612/photo.jpg",
          "visualDescription": "画面中有现代校园建筑、道路、绿化和蓝天。",
          "durationMs": 4000,
          "motion": "zoom_in",
          "transition": "fade",
          "subtitle": "第 1 幕 · 北京邮电大学沙河校区",
          "narration": "旅程从北京邮电大学沙河校区的第一张照片开始。"
        }
      ]
    },
    "createdAt": "2026-06-12T10:00:00",
    "updatedAt": "2026-06-12T10:00:00"
  },
  "timestamp": "2026-06-12T10:00:00"
}
```

`visualDescription` 为向后兼容的可选字段。旧版 `script_json` 没有该字段时仍可读取，前端 AnimationPlayer 可选择展示或忽略。

## 14. File 接口

## 14.1 上传图片 / 视频

* 方法：`POST`
* 路径：`/api/v1/files/upload`
* Content-Type：`multipart/form-data`
* 权限：需要登录，使用 `Authorization: Bearer <token>`

### Form Data

| 参数名     | 类型     | 必填 | 说明                         |
| ------- | ------ | -: | -------------------------- |
| file    | file   |  是 | 上传文件                       |
| bizType | string |  是 | `avatar/diary/destination` |
| refId   | long   |  否 | 关联业务 ID                    |

### Response

返回 `FileUploadResultVO`

说明：当前 P0 文件上传不写入独立文件资源表，因此返回结果中不包含 `id`。上传成功后返回的 `fileUrl` 可直接通过 `GET /files/...` 访问，例如 Diary 图片上传后返回 `/files/diary/20260505/example.png`，前端可将该 URL 写入日记发布请求的 `mediaList`。如果后续新增文件资源表，再统一补充文件 ID 与 Diary `mediaIds` 方案。

---

## 15. Admin 接口

## 15.1 分页查询用户

* 方法：`GET`
* 路径：`/api/v1/admin/users`
* 权限：管理员

### Query 参数

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| keyword | string | 否 | 用户名 / 昵称关键字 |
| type | string | 否 | 用户角色，如 `user/admin` |
| pageNum | int | 否 | 页码 |
| pageSize | int | 否 | 每页数量 |

### Response

返回 `Page<AdminUserVO>`，不返回 `password_hash`。

## 15.2 分页查询目的地

* 方法：`GET`
* 路径：`/api/v1/admin/destinations`
* 权限：管理员

### Query 参数

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| keyword | string | 否 | 名称 / 城市 / 分类关键字 |
| type | string | 否 | 目的地类型 |
| pageNum | int | 否 | 页码 |
| pageSize | int | 否 | 每页数量 |

### Response

返回 `Page<AdminDestinationVO>`

## 15.3 新增目的地

* 方法：`POST`
* 路径：`/api/v1/admin/destinations`
* 权限：管理员

## 15.4 修改目的地

* 方法：`PUT`
* 路径：`/api/v1/admin/destinations/{id}`
* 权限：管理员

## 15.5 删除 / 下架目的地

* 方法：`DELETE`
* 路径：`/api/v1/admin/destinations/{id}`
* 权限：管理员
* 说明：当前按 `destination.status=0` 做逻辑下架。

## 15.6 分页查询设施

* 方法：`GET`
* 路径：`/api/v1/admin/facilities`
* 权限：管理员

### Query 参数

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| destinationId | long | 否 | 所属目的地 |
| keyword | string | 否 | 名称 / 描述关键字 |
| type | string | 否 | 设施类型 |
| pageNum | int | 否 | 页码 |
| pageSize | int | 否 | 每页数量 |

## 15.7 分页查询场所

* 方法：`GET`
* 路径：`/api/v1/admin/places`
* 权限：管理员

### Query 参数

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| destinationId | long | 否 | 所属目的地 |
| keyword | string | 否 | 名称 / 描述关键字 |
| type | string | 否 | 场所类型，对应 `place.place_type` |
| pageNum | int | 否 | 页码 |
| pageSize | int | 否 | 每页数量，最大 100 |

### Response

返回 `Page<AdminPlaceVO>`，字段包括 `id`、`destinationId`、`name`、`placeType`、`description`、`lng`、`lat`、`floorInfo`、`heatScore`、`ratingScore`、`openTimeRule`、`suggestedDurationMin`、`costLevel`。

## 15.7A 新增场所

* 方法：`POST`
* 路径：`/api/v1/admin/places`
* 权限：管理员

### Request Body

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| destinationId | long | 是 | 所属目的地 |
| name | string | 是 | 场所名称 |
| placeType | string | 是 | 场所类型，如 `building/scenic_spot/dormitory` |
| description | string | 否 | 场所描述 |
| lng | decimal | 否 | 经度 |
| lat | decimal | 否 | 纬度 |
| floorInfo | string | 否 | 楼层信息 |
| heatScore | decimal | 否 | 热度分，默认 0 |
| ratingScore | decimal | 否 | 评分，默认 0 |
| openTimeRule | string | 否 | 开放时间规则 |
| suggestedDurationMin | int | 否 | 建议停留分钟数 |
| costLevel | int | 否 | 成本等级 |

### Response

返回 `AdminPlaceVO`。

## 15.7B 修改场所

* 方法：`PUT`
* 路径：`/api/v1/admin/places/{id}`
* 权限：管理员

### Response

返回 `AdminPlaceVO`。

## 15.7C 删除场所

* 方法：`DELETE`
* 路径：`/api/v1/admin/places/{id}`
* 权限：管理员
* 说明：当前 `place` 表没有 `status` 字段，因此无下游引用时按物理删除处理；如果场所仍被 `facility.place_id` 或 `map_node(node_type=place, ref_id=id)` 引用，则拒绝删除并返回参数不合法类业务错误。

## 15.8 新增设施

* 方法：`POST`
* 路径：`/api/v1/admin/facilities`
* 权限：管理员

请求和响应新增可选字段：`address`、`tel`、`coverUrl`。字段分别表示设施地址、联系电话和封面图 URL；其余字段保持不变。

## 15.9 修改设施

* 方法：`PUT`
* 路径：`/api/v1/admin/facilities/{id}`
* 权限：管理员

## 15.10 删除 / 下架设施

* 方法：`DELETE`
* 路径：`/api/v1/admin/facilities/{id}`
* 权限：管理员
* 说明：当前按 `facility.status=0` 做逻辑下架。

## 15.11 分页查询美食

* 方法：`GET`
* 路径：`/api/v1/admin/foods`
* 权限：管理员

### Query 参数

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| destinationId | long | 否 | 所属目的地 |
| keyword | string | 否 | 名称 / 店铺 / 描述关键字 |
| type | string | 否 | 菜系 |
| pageNum | int | 否 | 页码 |
| pageSize | int | 否 | 每页数量 |

## 15.12 新增美食

* 方法：`POST`
* 路径：`/api/v1/admin/foods`
* 权限：管理员

请求和响应新增可选字段：`lng`、`lat`，分别表示美食店铺或窗口自身经纬度；为空时不自动继承关联设施坐标。

## 15.13 修改美食

* 方法：`PUT`
* 路径：`/api/v1/admin/foods/{id}`
* 权限：管理员

## 15.14 删除美食

* 方法：`DELETE`
* 路径：`/api/v1/admin/foods/{id}`
* 权限：管理员
* 说明：当前 `food` 表没有 `status` 字段，因此按物理删除处理。

## 15.15 新增地图节点

* 方法：`GET`
* 路径：`/api/v1/admin/map/nodes`
* 权限：管理员
* 说明：按 `destinationId`、`type`、`keyword` 查询地图节点。

---

* 方法：`POST`
* 路径：`/api/v1/admin/map/nodes`
* 权限：管理员

### Request Body

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| destinationId | long | 是 | 所属目的地 |
| nodeName | string | 是 | 节点名称 |
| nodeType | string | 是 | 节点类型 |
| refId | long | 否 | 关联 place / facility 等业务对象 ID |
| placeId | long | 否 | 室内节点所属建筑，对应 `place.id` |
| lng | decimal | 否 | 经度 |
| lat | decimal | 否 | 纬度 |
| floorNo | int | 否 | 楼层 |
| indoorX | decimal | 否 | 楼层 SVG 归一化 X 坐标 |
| indoorY | decimal | 否 | 楼层 SVG 归一化 Y 坐标 |

### Response

返回 `AdminMapNodeVO`。

## 15.15A 修改地图节点

* 方法：`PUT`
* 路径：`/api/v1/admin/map/nodes/{id}`
* 权限：管理员

## 15.15B 删除地图节点

* 方法：`DELETE`
* 路径：`/api/v1/admin/map/nodes/{id}`
* 权限：管理员
* 说明：当前为物理删除；如果节点已被 `map_edge` 引用，则拒绝删除。

## 15.16 新增地图边

* 方法：`GET`
* 路径：`/api/v1/admin/map/edges`
* 权限：管理员
* 说明：按 `destinationId`、`type` 查询地图边，其中 `type` 当前对应 `transportType`。

---

* 方法：`POST`
* 路径：`/api/v1/admin/map/edges`
* 权限：管理员

### Request Body

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| destinationId | long | 是 | 所属目的地 |
| fromNodeId | long | 是 | 起点节点 |
| toNodeId | long | 是 | 终点节点 |
| distance | decimal | 是 | 距离权重 |
| idealSpeed | decimal | 否 | 理想速度 |
| crowdFactor | decimal | 否 | 拥挤系数 |
| transportType | string | 否 | 交通方式 |
| edgeType | string | 否 | 边类型 |
| bidirectionalFlag | int | 否 | 是否双向标记 |

### Response

返回 `AdminMapEdgeVO`。

说明：新增 / 修改地图边时会校验起点和终点都存在，且与 `destinationId` 属于同一目的地；`bidirectionalFlag` 仅保存字段，不自动创建反向边。

## 15.16A 修改地图边

* 方法：`PUT`
* 路径：`/api/v1/admin/map/edges/{id}`
* 权限：管理员

## 15.16B 删除地图边

* 方法：`DELETE`
* 路径：`/api/v1/admin/map/edges/{id}`
* 权限：管理员

## 15.17 管理端分页查询日记

* 方法：`GET`
* 路径：`/api/v1/admin/diaries`
* 权限：管理员

### Query 参数

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| destinationId | long | 否 | 目的地 ID |
| keyword | string | 否 | 日记标题关键字 |
| pageNum | int | 否 | 页码 |
| pageSize | int | 否 | 每页数量 |

### Response

返回 `Page<AdminDiaryVO>`。

## 15.17A 修改用户状态

* 方法：`PUT`
* 路径：`/api/v1/admin/users/{id}/status`
* 权限：管理员
* 说明：`status` 只允许 `0/1`，用于禁用 / 启用用户。

## 15.17B 修改日记状态

* 方法：`PUT`
* 路径：`/api/v1/admin/diaries/{id}/status`
* 权限：管理员
* 说明：`status` 只允许 `0/1`，用于下架 / 恢复日记；前台日记列表和详情只展示 `status=1` 的日记。

## 15.18 批量导入数据

* 方法：`POST`
* 路径：`/api/v1/admin/import-batches/preview`
* 权限：管理员
* Content-Type：`multipart/form-data`
* 说明：调用 `ImportService.previewImport`，解析 CSV / JSON 并返回预览行数与校验警告，不写入业务表。

### Form Data

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| targetTable | string | 是 | 目标表名，当前支持 `destination/place/facility/food/map_node/map_edge` |
| sourceType | string | 是 | `csv/json` |
| file | file | 是 | 标准化导入文件，最大 20MB，当前最多 1000 行 |

### Response

返回 `ImportPreviewResult`，包含 `batchName`、`targetTable`、`sourceType`、`fileName`、`status`、`totalRows`、`warnings`。

---

* 方法：`POST`
* 路径：`/api/v1/admin/import-batches`
* 权限：管理员
* Content-Type：`multipart/form-data`
* 说明：调用 `ImportService.runImport`。Controller 负责接收 `MultipartFile`，Service 核心基于 `Reader` 解析并执行真实入库。

### Form Data

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| targetTable | string | 是 | 目标表名，当前支持 `destination/place/facility/food/map_node/map_edge` |
| sourceType | string | 是 | `csv/json` |
| file | file | 是 | 标准化导入文件，最大 20MB，当前最多 1000 行 |

### Response

返回 `ImportResult`，包含 `batchName`、`targetTable`、`status`、`totalRows`、`successRows`、`failedRows`、`errors`。

说明：
- 当前导入成功行会写入对应业务表，失败行不会写入；
- 每次执行会写入 `import_batch`，失败行会写入 `import_failure`；
- 当前不支持 `user`、`diary`、`diary_media`、`route_history` 导入，不支持 Excel / SQL 上传执行；
- CSV 使用首行表头映射字段；JSON 使用对象数组格式，例如 `[{"name":"测试目的地","type":"campus"}]`。

## 15.19 查询导入批次

* 方法：`GET`
* 路径：`/api/v1/admin/import-batches`
* 权限：管理员

### Query 参数

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| keyword | string | 否 | 批次名 / 文件名关键字 |
| type | string | 否 | 目标表名，如 `destination` |
| status | string | 否 | 导入状态，如 `SUCCESS/FAILED/PARTIAL_SUCCESS/RUNNING` |
| pageNum | int | 否 | 页码 |
| pageSize | int | 否 | 每页数量，最大 100 |

### Response

返回 `Page<AdminImportBatchVO>`，字段包括 `id`、`batchName`、`targetTable`、`sourceType`、`fileName`、`fileSize`、`status`、`totalRows`、`successRows`、`failedRows`、`errorMessage`、`createdAt`、`updatedAt`。

## 15.20 查询导入失败明细

* 方法：`GET`
* 路径：`/api/v1/admin/import-batches/{batchId}/failures`
* 权限：管理员

### Query 参数

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| pageNum | int | 否 | 页码 |
| pageSize | int | 否 | 每页数量，最大 100 |

### Response

返回 `Page<AdminImportFailureVO>`，字段包括 `id`、`batchId`、`rowNo`、`fieldName`、`errorMessage`、`rawDataJson`、`createdAt`。

说明：如果 `batchId` 不存在，返回资源不存在错误；当前 `rawDataJson` 用于辅助管理员定位标准化导入文件中的错误行。

> 说明：当前阶段管理端以基础数据维护为主；如果后续正式纳入偏好标签管理、手账管理、多人协同会话管理、轨迹管理，再扩展新的 Admin 接口分组。

---

## 16. AI 接口（后续增强）

> 日记照片动画已通过 Diary 资源接口实现后端 MVP。以下日记草稿、图片摘要、路线回顾、多人协商和推荐理由接口仍是规划项，代码未实现，不纳入当前联调范围。

## 16.1 日记草稿生成

* 方法：`POST`
* 路径：`/api/v1/ai/diary-draft`

### Request Body

```json
{
  "destinationId": 101,
  "imageUrls": [
    "/files/diary/d5001.jpg",
    "/files/diary/d5002.jpg"
  ],
  "contentHint": "今天在校园里散步，拍了图书馆和食堂。",
  "routeHistoryId": 9001
}
```

### Response

返回 `DiaryDraftVO`

## 16.2 图片摘要

* 方法：`POST`
* 路径：`/api/v1/ai/image-summary`

### Request Body

```json
{
  "destinationId": 101,
  "imageUrls": [
    "/files/diary/d5001.jpg"
  ]
}
```

### Response

返回 `ImageSummaryVO`

## 16.3 路线回顾

* 方法：`POST`
* 路径：`/api/v1/ai/route-review`

### Request Body

```json
{
  "routeHistoryId": 9001
}
```

### Response

返回 `RouteReviewVO`

## 16.4 多人规划协商摘要

* 方法：`POST`
* 路径：`/api/v1/ai/group-plan-summary`

### Request Body

```json
{
  "participants": [
    {
      "displayName": "A",
      "preferThemeList": ["人文建筑型"],
      "customPreferenceText": "不想走太远"
    },
    {
      "displayName": "B",
      "preferThemeList": ["自然景观型"],
      "customPreferenceText": "希望风景好、适合拍照"
    }
  ],
  "constraints": {
    "timeLimitMin": 180,
    "budgetLevel": 2
  },
  "candidatePlans": [
    {
      "name": "方案A",
      "summary": "图书馆-教学楼-食堂"
    },
    {
      "name": "方案B",
      "summary": "湖边-图书馆-食堂"
    }
  ]
}
```

### Response

返回 `GroupPlanSummaryVO`

## 16.5 推荐理由解释（建议预留）

* 方法：`POST`
* 路径：`/api/v1/ai/recommend-reason`

### Request Body

```json
{
  "userPreference": {
    "preferThemeList": ["人文建筑型", "自然景观型"],
    "customPreferenceText": "喜欢安静、适合拍照的地方"
  },
  "candidateDestinations": [
    {
      "id": 101,
      "name": "北京邮电大学沙河校区"
    }
  ]
}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "success",
  "data": {
    "recommendReason": "该目的地兼具校园人文氛围与开阔景观，较符合你的偏好。"
  },
  "timestamp": "2026-04-29T12:00:00"
}
```

---

## 17. 当前建议的接口实现优先级

### P0 必做

* `POST /api/v1/auth/register`
* `POST /api/v1/auth/login`
* `GET /api/v1/auth/me`
* `GET /api/v1/user-preferences/me`
* `PUT /api/v1/user-preferences/me`
* `GET /api/v1/destinations/recommend`
* `GET /api/v1/destinations/search`
* `GET /api/v1/destinations/{id}`
* `GET /api/v1/destinations/{id}/places`
* `POST /api/v1/routes/plan/single`
* `GET /api/v1/routes/history`
* `GET /api/v1/facilities/nearby`
* `POST /api/v1/diaries`
* `GET /api/v1/diaries`
* `GET /api/v1/diaries/{id}`
* `GET /api/v1/destinations/{id}/diaries`
* `POST /api/v1/files/upload`

### P1 建议补充

* `GET /api/v1/foods/recommend`
* `GET /api/v1/foods/search`
* `POST /api/v1/routes/plan/multi`
* `GET /api/v1/diaries/search/title`
* `GET /api/v1/diaries/search/fulltext`
* `POST /api/v1/diaries/{id}/ratings`
* `GET /api/v1/diaries/me`
* 管理端新增 / 修改 / 删除接口

### P2 创新增强

* `POST /api/v1/ai/diary-draft`
* `POST /api/v1/ai/image-summary`
* `POST /api/v1/ai/route-review`
* `POST /api/v1/ai/group-plan-summary`
* `POST /api/v1/ai/recommend-reason`

---

## 18. 接口联调注意事项

1. `UserPreference` 当前建议统一使用 `/user-preferences/me`，不要和旧版 `/users/me/preferences` 混用。
2. `Diary` 发布接口已补充 `visibility` 和可选 `routeHistoryId`，前后端字段名要保持一致。
3. `Route` 返回里的 `estimatedTime` 建议统一按“分钟”理解，不要一会儿秒、一会儿分钟。
4. `AI` 接口当前是预留能力，后端可以先 mock / stub，不要在业务模块里直接绑具体模型厂商。
5. `Diary` 图文发布的推荐联调流程为：先调用 `POST /api/v1/files/upload` 上传图片并取得 `fileUrl`，再调用 `POST /api/v1/diaries`，将该 `fileUrl` 写入 `mediaList.fileUrl`。
6. `swagger-draft.yaml` 需要与本文件同步更新，否则后面 Swagger Editor 校验结果会和文档口径不一致。
