- 作用：写接口定义。

# API 接口定义文档（API Specification）

## 1. 文档用途
本文件用于说明个性化旅游系统后端 API 的整体设计、接口分组、请求与响应格式、认证方式、主要数据对象和核心接口定义，为前后端并行开发、接口联调、测试执行和答辩说明提供依据。

本文件关注的是：
- 系统对外提供了哪些接口
- 各接口的输入输出是什么
- 哪些接口属于当前 MVP 必做范围
- 前后端联调时应遵循哪些统一约定

相关文档：
- `docs/04_api/error-codes.md`
- `docs/04_api/api-changelog.md`
- `docs/05_modules/*`
- `docs/03_data/schema.md`

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
  "timestamp": "2026-03-29T12:00:00"
}
```

## 3.3 通用失败响应结构

```json
{
  "success": false,
  "code": "AUTH_003",
  "message": "未登录或登录状态已失效",
  "data": null,
  "timestamp": "2026-03-29T12:00:00"
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
  "timestamp": "2026-03-29T12:00:00"
}
```

### 说明

- `success`：布尔值，表示本次业务调用是否成功
- `code`：业务码，成功时建议为 `SUCCESS`，失败时参见 `error-codes.md`
- `message`：简洁提示信息
- `data`：具体业务数据
- `timestamp`：响应时间

---

## 4. 认证与权限约定

### 4.1 无需登录即可访问

- 用户注册
- 用户登录
- 推荐列表
- 目的地搜索
- 目的地详情
- 场所列表
- 基础美食 / 设施浏览（如当前设计允许）

### 4.2 需要登录

- 获取当前用户信息
- 更新用户偏好
- 发布日记
- 日记评分
- 获取我的路线历史
- 调用需要用户上下文的 AI 能力

### 4.3 需要管理员权限

- 管理端所有接口
- 批量导入接口
- 数据维护接口

---

## 5. 接口分组总览

当前 API 按以下分组组织：

1. Auth：用户认证
2. UserPreference：用户偏好
3. Destination：目的地推荐与查询
4. Route：路线规划与路线历史
5. Facility：周边设施
6. Food：美食查询与推荐
7. Diary：旅游日记
8. File：文件上传
9. Admin：管理端数据维护
10. AI：AI 增强能力

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
  "preferHotLevel": 3,
  "preferTheme": "文化",
  "preferFoodType": "面食",
  "preferCrowdLevel": 2,
  "travelStyle": "小众"
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
  "floorInfo": "B1-5F"
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
  "coverUrl": "/files/food/f3001.jpg"
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
  "title": "今天在校园里散步",
  "contentText": "正文摘要",
  "heatScore": 25.0,
  "ratingScore": 4.8,
  "mediaList": [
    {
      "id": 5001,
      "mediaType": "image",
      "fileUrl": "/files/diary/d5001.jpg"
    }
  ],
  "createdAt": "2026-03-29T10:00:00"
}
```

## 6.8 RoutePlanVO

```json
{
  "destinationId": 101,
  "strategyType": "shortest_distance",
  "transportType": "walk",
  "totalDistance": 1260.5,
  "estimatedTime": 1080,
  "pathNodes": [
    { "nodeId": 1, "nodeName": "校门" },
    { "nodeId": 5, "nodeName": "图书馆" },
    { "nodeId": 9, "nodeName": "食堂" }
  ],
  "pathEdges": [
    { "fromNodeId": 1, "toNodeId": 5, "distance": 500.0 },
    { "fromNodeId": 5, "toNodeId": 9, "distance": 760.5 }
  ]
}
```

---

## 7. Auth 接口

## 7.1 用户注册

- 方法：`POST`
- 路径：`/api/v1/auth/register`

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
  "timestamp": "2026-03-29T12:00:00"
}
```

## 7.2 用户登录

- 方法：`POST`
- 路径：`/api/v1/auth/login`

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
  "timestamp": "2026-03-29T12:00:00"
}
```

## 7.3 获取当前用户信息

- 方法：`GET`
- 路径：`/api/v1/auth/me`

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "success",
  "data": {
    "id": 1,
    "username": "alice01",
    "nickname": "Alice",
    "avatarUrl": "/files/avatar/a1.png",
    "role": "user"
  },
  "timestamp": "2026-03-29T12:00:00"
}
```

---

## 8. UserPreference 接口

## 8.1 获取当前用户偏好

- 方法：`GET`
- 路径：`/api/v1/users/me/preferences`

## 8.2 更新当前用户偏好

- 方法：`PUT`
- 路径：`/api/v1/users/me/preferences`

### Request Body

```json
{
  "preferHotLevel": 3,
  "preferTheme": "文化",
  "preferFoodType": "面食",
  "preferCrowdLevel": 2,
  "travelStyle": "小众"
}
```

---

## 9. Destination 接口

## 9.1 获取推荐目的地列表

- 方法：`GET`
- 路径：`/api/v1/destinations/recommend`

### Query 参数

|参数名|类型|必填|说明|
|---|---|--:|---|
|type|string|否|scenic/campus|
|theme|string|否|主题|
|sortBy|string|否|heat/rating/recommend|
|pageNum|int|否|页码|
|pageSize|int|否|每页数量|
|topK|int|否|Top-K 推荐数|

## 9.2 搜索目的地

- 方法：`GET`
- 路径：`/api/v1/destinations/search`

### Query 参数

|参数名|类型|必填|说明|
|---|---|--:|---|
|keyword|string|是|名称/类别/关键字|
|type|string|否|scenic/campus|
|sortBy|string|否|heat/rating|
|pageNum|int|否|页码|
|pageSize|int|否|每页条数|

## 9.3 获取目的地详情

- 方法：`GET`
- 路径：`/api/v1/destinations/{id}`

## 9.4 获取目的地下的场所列表

- 方法：`GET`
- 路径：`/api/v1/destinations/{id}/places`

---

## 10. Route 接口

## 10.1 单目标路线规划

- 方法：`POST`
- 路径：`/api/v1/routes/plan/single`

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

## 10.2 多目标路线规划

- 方法：`POST`
- 路径：`/api/v1/routes/plan/multi`

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

## 10.3 获取当前用户路线历史

- 方法：`GET`
- 路径：`/api/v1/routes/history`

---

## 11. Facility 接口

## 11.1 查询附近设施

- 方法：`GET`
- 路径：`/api/v1/facilities/nearby`

### Query 参数

|参数名|类型|必填|说明|
|---|---|--:|---|
|destinationId|long|是|目的地ID|
|sourceNodeId|long|是|当前节点ID|
|facilityType|string|否|设施类型|
|radius|int|否|搜索范围，单位米|
|sortBy|string|否|distance/time|

## 11.2 搜索设施

- 方法：`GET`
- 路径：`/api/v1/facilities/search`

---

## 12. Food 接口

## 12.1 获取美食推荐列表

- 方法：`GET`
- 路径：`/api/v1/foods/recommend`

### Query 参数

|参数名|类型|必填|说明|
|---|---|--:|---|
|destinationId|long|是|目的地ID|
|sourceNodeId|long|否|当前节点ID|
|foodType|string|否|菜系|
|sortBy|string|否|heat/rating/distance|
|topK|int|否|Top-K 数量|

## 12.2 搜索美食

- 方法：`GET`
- 路径：`/api/v1/foods/search`

---

## 13. Diary 接口

## 13.1 发布日记

- 方法：`POST`
- 路径：`/api/v1/diaries`

### Request Body

```json
{
  "destinationId": 101,
  "title": "今天在校园里散步",
  "contentText": "今天天气很好，我先去了图书馆，再去了食堂。",
  "mediaIds": [5001, 5002]
}
```

## 13.2 获取日记列表

- 方法：`GET`
- 路径：`/api/v1/diaries`

## 13.3 获取日记详情

- 方法：`GET`
- 路径：`/api/v1/diaries/{id}`

## 13.4 按标题精确查询日记

- 方法：`GET`
- 路径：`/api/v1/diaries/search/title`

## 13.5 日记全文检索

- 方法：`GET`
- 路径：`/api/v1/diaries/search/fulltext`

## 13.6 对日记评分

- 方法：`POST`
- 路径：`/api/v1/diaries/{id}/ratings`

---

## 14. File 接口

## 14.1 上传图片 / 视频

- 方法：`POST`
    
- 路径：`/api/v1/files/upload`
    
- Content-Type：`multipart/form-data`
    

### Form Data

|参数名|类型|必填|说明|
|---|---|--:|---|
|file|file|是|上传文件|
|bizType|string|是|avatar/diary/destination|
|refId|long|否|关联业务ID|

---

## 15. Admin 接口

## 15.1 分页查询用户

- 方法：`GET`
- 路径：`/api/v1/admin/users`

## 15.2 新增目的地

- 方法：`POST`
- 路径：`/api/v1/admin/destinations`

## 15.3 修改目的地

- 方法：`PUT`
- 路径：`/api/v1/admin/destinations/{id}`

## 15.4 删除目的地

- 方法：`DELETE`
- 路径：`/api/v1/admin/destinations/{id}`

## 15.5 新增场所

- 方法：`POST`
- 路径：`/api/v1/admin/places`

## 15.6 新增设施

- 方法：`POST`
- 路径：`/api/v1/admin/facilities`

## 15.7 新增美食

- 方法：`POST`
- 路径：`/api/v1/admin/foods`

## 15.8 新增地图节点

- 方法：`POST`
- 路径：`/api/v1/admin/map/nodes`

## 15.9 新增地图边

- 方法：`POST`
- 路径：`/api/v1/admin/map/edges`

## 15.10 管理端分页查询日记

- 方法：`GET`
- 路径：`/api/v1/admin/diaries`

## 15.11 批量导入数据

- 方法：`POST`
- 路径：`/api/v1/admin/import-batches`

---

## 16. AI 接口（后续增强）

- AI 接口（P2 后续增强）
- 当前阶段预留，不阻塞基础主线实现

## 16.1 日记草稿生成

- 方法：`POST`
- 路径：`/api/v1/ai/diary-draft`

## 16.2 图片摘要

- 方法：`POST`
- 路径：`/api/v1/ai/image-summary`

## 16.3 路线回顾

- 方法：`POST`
- 路径：`/api/v1/ai/route-review`

## 16.4 多人规划协商摘要

- 方法：`POST`
- 路径：`/api/v1/ai/group-plan-summary`

---

## 17. 当前建议的接口实现优先级

### P0 必做

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/destinations/recommend`
- `GET /api/v1/destinations/search`
- `GET /api/v1/destinations/{id}`
- `POST /api/v1/routes/plan/single`
- `GET /api/v1/facilities/nearby`
- `GET /api/v1/foods/recommend`
- `POST /api/v1/diaries`
- `GET /api/v1/diaries`
- `GET /api/v1/diaries/{id}`
- `POST /api/v1/files/upload`

### P1 建议补充

- `PUT /api/v1/users/me/preferences`
- `POST /api/v1/routes/plan/multi`
- `GET /api/v1/diaries/search/fulltext`
- `POST /api/v1/diaries/{id}/ratings`
- 管理端新增 / 修改 / 删除接口

### P2 创新增强

- AI 模块全部接口
- 更复杂的 Admin 管理与批量导入能力