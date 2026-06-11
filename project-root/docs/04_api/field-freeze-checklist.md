# 字段冻结清单（Field Freeze Checklist）

## 1. 文件用途
本文件整理当前接口契约层字段的 v1 冻结建议，避免前后端联调时反复改字段名、类型和语义。冻结对象是“接口字段”，不是前端内部状态字段。

冻结等级：
- 强冻结：已在真实 DTO/VO/Controller 中出现，且属于核心联调契约字段。
- 建议冻结：文档与代码基本一致，但后续仍可能随增强调整。
- 待确认：代码/文档冲突，或只在旧文档中出现但当前后端未找到实现。

## 2. 通用返回体字段

| 接口名/路径 | 字段名 | 位置 | 当前类型 | 当前语义 | 冻结等级 | 允许改名 | 允许改类型 | 允许改语义 | 冻结原因 | 来源证据 | 风险说明 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 所有接口 | `success` | response | boolean | 业务调用是否成功 | 强冻结 | 否 | 否 | 否 | 统一返回体核心字段 | `ApiResponse.java` | 前端全局错误处理依赖 |
| 所有接口 | `code` | response | string | 业务错误码/成功码 | 强冻结 | 否 | 否 | 否 | 错误处理核心字段 | `ApiResponse.java`、`error-codes.md` | 不应依赖 message 判断逻辑 |
| 所有接口 | `message` | response | string | 面向调用方的提示信息 | 强冻结 | 否 | 否 | 是 | 统一返回体核心字段 | `ApiResponse.java` | 文案可优化，但语义不可变 |
| 所有接口 | `data` | response | object/null | 业务数据 | 强冻结 | 否 | 否 | 否 | 所有接口包装业务结果 | `ApiResponse.java` | 不能改成裸返回 |
| 所有接口 | `timestamp` | response | datetime/string 序列化 | 响应时间 | 建议冻结 | 否 | 待确认 | 否 | 代码使用 `LocalDateTime` | `ApiResponse.java` | JSON 格式由 Jackson 默认配置决定 |

## 3. 通用分页字段

| 接口名/路径 | 字段名 | 位置 | 当前类型 | 当前语义 | 冻结等级 | 允许改名 | 允许改类型 | 允许改语义 | 冻结原因 | 来源证据 | 风险说明 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 分页列表 | `list` | response.data | array | 当前页数据 | 强冻结 | 否 | 否 | 否 | 分页 VO 核心字段 | `PageResultVO.java` | 前端列表渲染依赖 |
| 分页列表 | `pageNum` | query/response.data | integer/long | 页码 | 强冻结 | 否 | 否 | 否 | 多个 Query 和分页 VO 复用 | `PageResultVO.java`、多个 Query DTO | 高风险字段 |
| 分页列表 | `pageSize` | query/response.data | integer/long | 每页条数 | 强冻结 | 否 | 否 | 否 | 多个 Query 和分页 VO 复用 | `PageResultVO.java`、多个 Query DTO | 高风险字段 |
| 分页列表 | `total` | response.data | long | 总条数 | 强冻结 | 否 | 否 | 否 | 分页 VO 核心字段 | `PageResultVO.java` | 前端分页器依赖 |
| 分页列表 | `pages` | response.data | long | 总页数 | 强冻结 | 否 | 否 | 否 | 分页 VO 核心字段 | `PageResultVO.java` | 前端分页器依赖 |

## 4. 鉴权相关字段

| 接口名/路径 | 字段名 | 位置 | 当前类型 | 当前语义 | 冻结等级 | 允许改名 | 允许改类型 | 允许改语义 | 冻结原因 | 来源证据 | 风险说明 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `/api/v1/auth/login` | `username` | body | string | 登录用户名 | 强冻结 | 否 | 否 | 否 | 登录 DTO 已实现 | `LoginRequest.java` | 影响登录表单 |
| `/api/v1/auth/login` | `password` | body | string | 登录密码 | 强冻结 | 否 | 否 | 否 | 登录 DTO 已实现 | `LoginRequest.java` | 不得明文存库，传输仍为请求字段 |
| `/api/v1/auth/login` | `token` | response.data | string | JWT token | 强冻结 | 否 | 否 | 否 | 登录响应核心字段 | `LoginResponse.java` | 高风险字段 |
| 受保护接口 | `Authorization` | header | string | `Bearer <token>` | 强冻结 | 否 | 否 | 否 | JWT filter 强约束 | `JwtAuthenticationFilter.java` | 前端拦截器需严格拼接 Bearer |
| `/api/v1/auth/register` | `nickname` | body | string | 用户昵称 | 强冻结 | 否 | 否 | 是 | 注册 DTO 已实现 | `RegisterRequest.java` | 可为空规则需看 DTO 注解 |
| UserVO | `id` | response.data | long | 用户 ID | 强冻结 | 否 | 否 | 否 | 核心身份字段 | `UserVO.java` | 高风险字段 |
| UserVO | `role` | response.data | string | 用户角色 | 强冻结 | 否 | 否 | 否 | Admin 权限依赖 | `UserVO.java`、`JwtAuthenticationFilter.java` | role 值需与 JWT claims 一致 |

## 5. 推荐 / 搜索 / 详情字段

| 接口名/路径 | 字段名 | 位置 | 当前类型 | 当前语义 | 冻结等级 | 允许改名 | 允许改类型 | 允许改语义 | 冻结原因 | 来源证据 | 风险说明 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `/api/v1/destinations/recommend` | `type` | query | string | 目的地类型 | 强冻结 | 否 | 否 | 否 | 查询 DTO 已实现 | `DestinationRecommendQuery.java` | 值域需与数据一致 |
| `/api/v1/destinations/recommend` | `theme` | query | string | 主题筛选 | 建议冻结 | 否 | 否 | 是 | 查询 DTO 已实现 | `DestinationRecommendQuery.java` | 与 `tag_json/tags` 映射需确认 |
| `/api/v1/destinations/recommend` | `sortBy` | query | string | 排序方式 | 强冻结 | 否 | 否 | 否 | 多模块共用排序参数 | `DestinationRecommendQuery.java` | 值域错误会影响联调 |
| `/api/v1/destinations/recommend` | `topK` | query | integer | Top-K 数量 | 强冻结 | 否 | 否 | 否 | 推荐核心字段 | `DestinationRecommendQuery.java` | 高风险字段 |
| `/api/v1/destinations/search` | `keyword` | query | string | 搜索词 | 强冻结 | 否 | 否 | 否 | 搜索 DTO 已实现 | `DestinationSearchQuery.java` | 空值校验需遵循 DTO |
| DestinationVO | `id` | response | long | 目的地 ID | 强冻结 | 否 | 否 | 否 | 多模块引用 | `DestinationVO.java` | 高风险字段 |
| DestinationVO | `heatScore` | response | decimal | 热度分 | 强冻结 | 否 | 否 | 否 | 排序/展示核心字段 | `DestinationVO.java` | 高风险字段 |
| DestinationVO | `ratingScore` | response | decimal | 评分 | 强冻结 | 否 | 否 | 否 | 排序/展示核心字段 | `DestinationVO.java` | 高风险字段 |
| DestinationVO | `coverUrl` | response | string | 封面图 URL | 强冻结 | 否 | 否 | 否 | 展示字段 | `DestinationVO.java` | 高风险字段 |
| PlaceVO | `lng` / `lat` | response | decimal | 经度/纬度 | 强冻结 | 否 | 否 | 否 | 地图展示字段 | `PlaceVO.java` | 高风险字段 |

## 6. 地图 / 路线相关字段

| 接口名/路径 | 字段名 | 位置 | 当前类型 | 当前语义 | 冻结等级 | 允许改名 | 允许改类型 | 允许改语义 | 冻结原因 | 来源证据 | 风险说明 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `/api/v1/routes/plan/single` | `destinationId` | body | long | 目的地 ID | 强冻结 | 否 | 否 | 否 | 路线必需字段 | `SingleRoutePlanRequest.java` | 高风险字段 |
| `/api/v1/routes/plan/single` | `startNodeId` | body | long | 起点地图节点 ID | 强冻结 | 否 | 否 | 否 | 路线必需字段 | `SingleRoutePlanRequest.java` | 高风险字段 |
| `/api/v1/routes/plan/single` | `targetNodeId` | body | long | 终点地图节点 ID | 强冻结 | 否 | 否 | 否 | 路线必需字段 | `SingleRoutePlanRequest.java` | 高风险字段 |
| `/api/v1/routes/plan/multi` | `targetNodeIds` | body | array<long> | 多目标节点 ID 列表 | 强冻结 | 否 | 否 | 否 | 多目标路线必需字段 | `MultiRoutePlanRequest.java` | 数量上限需遵循后端 |
| `/api/v1/routes/plan/multi` | `returnToStart` | body | boolean | 是否回到起点 | 强冻结 | 否 | 否 | 否 | 多目标路线字段 | `MultiRoutePlanRequest.java` | 默认值需确认前端展示 |
| RoutePlanVO | `pathNodes` | response | array | 路径节点序列 | 强冻结 | 否 | 否 | 否 | 路线展示核心字段 | `RoutePlanVO.java` | 高风险字段 |
| RoutePlanVO | `pathEdges` | response | array | 路径边序列 | 强冻结 | 否 | 否 | 否 | 路线展示核心字段 | `RoutePlanVO.java` | 高风险字段 |
| RoutePlanVO | `totalDistance` | response | decimal | 总距离 | 强冻结 | 否 | 否 | 否 | 演示核心字段 | `RoutePlanVO.java` | 高风险字段 |
| RoutePlanVO | `estimatedTime` | response | integer | 预计时间，按分钟 | 强冻结 | 否 | 否 | 否 | 文档与代码均已使用 | `RoutePlanVO.java`、`api-spec.md` | 单位必须固定 |
| RoutePlanVO | `historyId` | response | long | 路线历史 ID | 强冻结 | 否 | 否 | 否 | 日记可关联路线历史 | `RoutePlanVO.java` | 高风险字段 |
| RoutePathEdgeVO | `fromNodeId/toNodeId/distance` | response.pathEdges | long/decimal | 边起点、终点、距离 | 强冻结 | 否 | 否 | 否 | 路径边展示字段 | `RoutePathEdgeVO.java` | 前端画线依赖 |
| Map node/admin | `lng` / `lat` | body/response | decimal | 节点经纬度 | 强冻结 | 否 | 否 | 否 | 管理端地图维护字段 | `AdminMapNodeRequest.java`、`AdminMapNodeVO.java` | 高风险字段 |

## 7. 周边设施字段

| 接口名/路径 | 字段名 | 位置 | 当前类型 | 当前语义 | 冻结等级 | 允许改名 | 允许改类型 | 允许改语义 | 冻结原因 | 来源证据 | 风险说明 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `/api/v1/facilities/nearby` | `destinationId` | query | long | 目的地 ID | 强冻结 | 否 | 否 | 否 | 查询必需字段 | `NearbyFacilityQuery.java` | 高风险字段 |
| `/api/v1/facilities/nearby` | `sourceNodeId` | query | long | 当前地图节点 ID | 强冻结 | 否 | 否 | 否 | 可达距离计算必需字段 | `NearbyFacilityQuery.java` | 高风险字段 |
| `/api/v1/facilities/nearby` | `facilityType` | query/response | string | 设施类型 | 强冻结 | 否 | 否 | 否 | 筛选/展示字段 | `NearbyFacilityQuery.java`、`NearbyFacilityVO.java` | 值域需统一 |
| NearbyFacilityVO | `reachableDistance` | response | decimal | 图上可达距离 | 强冻结 | 否 | 否 | 否 | 周边排序核心字段 | `NearbyFacilityVO.java` | 文档中曾称 distance，代码字段为 reachableDistance |
| Facility | `address` / `tel` / `coverUrl` | body/response | string | 设施地址、联系电话、封面图 URL | 强冻结 | 否 | 否 | 否 | 已进入 Entity、管理端请求和用户端响应 | `Facility.java`、`AdminFacilityRequest.java`、`NearbyFacilityVO.java` | 前端类型需同步生成 |
| NearbyFacilityVO | `facilityId` | response | 不存在 | 设施 ID 语义由 `id` 承担 | 待确认 | 是 | 是 | 是 | 用户特别关注字段，但当前 VO 使用 `id` | `NearbyFacilityVO.java` | 若前端期待 `facilityId` 会冲突 |

## 8. 美食字段

| 接口名/路径 | 字段名 | 位置 | 当前类型 | 当前语义 | 冻结等级 | 允许改名 | 允许改类型 | 允许改语义 | 冻结原因 | 来源证据 | 风险说明 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `/api/v1/foods/recommend` | `destinationId` | query | long | 目的地 ID | 强冻结 | 否 | 否 | 否 | 查询必需字段 | `FoodRecommendQuery.java` | 高风险字段 |
| `/api/v1/foods/recommend` | `facilityId` | query | long | 所属设施 ID | 强冻结 | 否 | 否 | 否 | 查询 DTO 已实现 | `FoodRecommendQuery.java` | 文档部分位置更强调 sourceNodeId，需同步 |
| `/api/v1/foods/search` | `keyword` | query | string | 搜索词 | 强冻结 | 否 | 否 | 否 | 搜索 DTO 已实现 | `FoodSearchQuery.java` |  |
| FoodVO | `foodType` | response | string | 菜系/类型 | 强冻结 | 否 | 否 | 否 | 展示与筛选字段 | `FoodVO.java` |  |
| FoodVO | `heatScore` / `ratingScore` | response | decimal | 热度/评分 | 强冻结 | 否 | 否 | 否 | 排序字段 | `FoodVO.java` | 高风险字段 |
| FoodVO | `coverUrl` | response | string | 封面图 URL | 强冻结 | 否 | 否 | 否 | 展示字段 | `FoodVO.java` | 高风险字段 |
| FoodVO | `lng` / `lat` | body/response | decimal | 店铺或窗口自身经纬度 | 强冻结 | 否 | 否 | 否 | 已进入 Entity、管理端请求和用户端响应 | `Food.java`、`AdminFoodRequest.java`、`FoodVO.java` | 空值不继承 Facility 坐标 |

## 9. 日记字段

| 接口名/路径 | 字段名 | 位置 | 当前类型 | 当前语义 | 冻结等级 | 允许改名 | 允许改类型 | 允许改语义 | 冻结原因 | 来源证据 | 风险说明 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `/api/v1/diaries` POST | `destinationId` | body | long | 关联目的地 ID | 强冻结 | 否 | 否 | 否 | 发布必需字段 | `DiaryCreateRequest.java` | 高风险字段 |
| `/api/v1/diaries` POST | `routeHistoryId` | body | long | 可选关联路线历史 ID | 强冻结 | 否 | 否 | 否 | 路线/日记联动字段 | `DiaryCreateRequest.java` | 高风险字段 |
| `/api/v1/diaries` POST | `title` | body | string | 日记标题 | 强冻结 | 否 | 否 | 否 | 发布核心字段 | `DiaryCreateRequest.java` |  |
| `/api/v1/diaries` POST | `contentText` | body/response | string | 日记正文 | 强冻结 | 否 | 否 | 否 | 发布/详情核心字段 | `DiaryCreateRequest.java`、`DiaryVO.java` |  |
| `/api/v1/diaries` POST | `visibility` | body/response | string | 可见性 | 强冻结 | 否 | 否 | 否 | 公开/私有控制字段 | `DiaryCreateRequest.java`、`DiaryVO.java` |  |
| `/api/v1/diaries` POST | `mediaList` | body/response | array | 日记媒体列表 | 强冻结 | 否 | 否 | 否 | 代码真实字段 | `DiaryCreateRequest.java`、`DiaryVO.java` | Swagger 中 `mediaIds` 与代码不一致 |
| DiaryMedia | `fileUrl` | body/response | string | 媒体访问 URL | 强冻结 | 否 | 否 | 否 | 上传与日记联动核心字段 | `DiaryMediaRequest.java`、`DiaryMediaVO.java` | 高风险字段 |
| DiaryVO | `id` | response | long | 日记 ID | 强冻结 | 否 | 否 | 否 | 详情/列表核心字段 | `DiaryVO.java` | 高风险字段 |
| DiaryVO | `userId` | response | long | 作者用户 ID | 强冻结 | 否 | 否 | 否 | 归属字段 | `DiaryVO.java` | 高风险字段 |
| DiaryVO | `heatScore` / `ratingScore` | response | decimal | 热度/评分 | 强冻结 | 否 | 否 | 否 | 排序字段 | `DiaryVO.java` | 高风险字段 |
| DiaryVO | `ratingCount` | response | integer | 有效评分人数 | 强冻结 | 否 | 否 | 否 | 评分聚合字段已落地 | `Diary.java`、`DiaryVO.java` | 必须与评分明细数量一致 |
| `/api/v1/diaries/{id}/ratings` | `score` | body | integer | 当前用户评分，范围 1～5 | 强冻结 | 否 | 否 | 否 | 请求 DTO 和数据库约束已实现 | `DiaryRatingRequest.java`、`diary_rating.score` | 越界返回 `DIARY_008` |
| `/api/v1/diaries/{id}/ratings/me` | `diaryId` / `userScore` / `ratingScore` / `ratingCount` | response | long / integer / decimal / integer | 当前用户评分及日记聚合结果 | 强冻结 | 否 | 否 | 否 | 接口和 VO 已实现 | `DiaryRatingVO.java` | 未评分时仅 `userScore` 可空 |
| `/api/v1/diaries/search/title` | `title` | query | string | 标题关键词 | 强冻结 | 否 | 否 | 否 | 检索 DTO 已实现 | `DiaryTitleSearchQuery.java` |  |
| `/api/v1/diaries/search/fulltext` | `keyword` | query | string | 正文关键词 | 强冻结 | 否 | 否 | 否 | 检索 DTO 已实现 | `DiaryFulltextSearchQuery.java` |  |

## 10. 文件上传字段

| 接口名/路径 | 字段名 | 位置 | 当前类型 | 当前语义 | 冻结等级 | 允许改名 | 允许改类型 | 允许改语义 | 冻结原因 | 来源证据 | 风险说明 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `/api/v1/files/upload` | `file` | multipart | file | 上传文件 | 强冻结 | 否 | 否 | 否 | Controller 已实现 | `FileController.java` |  |
| `/api/v1/files/upload` | `bizType` | multipart | string | 业务类型 | 强冻结 | 否 | 否 | 否 | Controller 已实现 | `FileController.java` |  |
| `/api/v1/files/upload` | `refId` | multipart | long | 关联业务 ID | 建议冻结 | 否 | 否 | 否 | Controller 已实现，可选 | `FileController.java` |  |
| FileUploadResultVO | `fileUrl` | response | string | 文件访问地址 | 强冻结 | 否 | 否 | 否 | 日记发布依赖 | `FileUploadResultVO.java` | 高风险字段 |
| FileUploadResultVO | `id` | response | 不存在 | 当前不返回文件 ID | 待确认 | 是 | 是 | 是 | Swagger 与代码不一致 | `FileUploadResultVO.java`、`swagger-draft.yaml` | 前端不能依赖 `id` |

## 待确认项
- `frontend-runtime-facts.md` 缺失，无法冻结前端内部字段、baseURL、proxy 和 token 存储字段。
- `FacilityVO` 文档字段 `distance/estimatedTime` 与代码 `NearbyFacilityVO.reachableDistance/sourceNodeId/targetNodeId` 需要统一。
- Route 历史、我的日记列表接口尚无代码入口，对应字段只能待确认。

## 代码/文档冲突项
- `CreateDiaryRequest`：Swagger 中是 `mediaIds`，后端代码是 `mediaList`。
- `FileUploadResultVO`：Swagger 中包含 `id`，后端代码不包含。
- Food 推荐：`api-spec.md` 提到 `sourceNodeId`，当前 `FoodRecommendQuery` 是 `facilityId`。
- Facility nearby：`api-spec.md` 响应写 `Page<FacilityVO>`，代码返回 `PageResultVO<NearbyFacilityVO>`。

## 建议下一步动作
- 联调前强冻结：通用返回体、分页体、登录 token、Authorization、目的地/路线/日记/文件上传核心字段。
- 先修正文档与代码冲突的 Swagger 字段，再让前端按 v1 契约接入。
- 对待确认字段不要进入前端强依赖，避免后续返工。
