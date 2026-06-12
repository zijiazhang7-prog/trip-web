# API 可用矩阵（API Availability）

## 1. 文件用途
本文件基于当前后端代码、`docs/04_api` 接口文档、架构/数据文档和仓库扫描结果，整理接口是否已经有后端入口、是否已在文档中定义，以及当前联调可用状态。

当前没有完整前端工程代码可用；本次扫描也未在仓库中找到 `frontend-runtime-facts.md`。因此本文不判断“前端是否已调用”，也不推断前端 baseURL、代理或 token 存储方式。

## 2. 本次读取的关键来源

### 2.1 后端代码
- `project-root/backend/src/main/java/com/trip/controller/AuthController.java`
- `project-root/backend/src/main/java/com/trip/controller/UserPreferenceController.java`
- `project-root/backend/src/main/java/com/trip/controller/DestinationController.java`
- `project-root/backend/src/main/java/com/trip/controller/RouteController.java`
- `project-root/backend/src/main/java/com/trip/controller/FacilityController.java`
- `project-root/backend/src/main/java/com/trip/controller/FoodController.java`
- `project-root/backend/src/main/java/com/trip/controller/DiaryController.java`
- `project-root/backend/src/main/java/com/trip/controller/FileController.java`
- `project-root/backend/src/main/java/com/trip/controller/AdminController.java`
- `project-root/backend/src/main/java/com/trip/controller/HealthController.java`
- `project-root/backend/src/main/java/com/trip/config/SecurityConfig.java`
- `project-root/backend/src/main/java/com/trip/security/JwtAuthenticationFilter.java`
- `project-root/backend/src/main/java/com/trip/common/ApiResponse.java`
- `project-root/backend/src/main/java/com/trip/vo/response/PageResultVO.java`

### 2.2 项目文档
- `project-root/docs/04_api/api-spec.md`
- `project-root/docs/04_api/swagger-draft.yaml`
- `project-root/docs/04_api/error-codes.md`
- `project-root/docs/02_architecture/architecture.md`
- `project-root/docs/02_architecture/module-map.md`
- `project-root/docs/02_architecture/dependency-map.md`
- `project-root/docs/03_data/schema.md`
- `project-root/docs/03_data/data-dictionary.md`
- `project-root/docs/00_project/progress.md`

### 2.3 前端运行时事实
- `frontend-runtime-facts.md`：本次在仓库中未找到，前端运行时事实全部标记为待确认。

## 3. 接口可用矩阵

| 接口功能 | URL 路径 | 方法 | 所属模块 | 后端是否已实现 | 文档是否已定义 | 当前状态 | 请求对象 | 响应对象 | 代码位置 | 证据来源 | 备注 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 健康检查 | `/api/v1/health` | GET | Health | 是 | 是 | 已可用 | 无 | `ApiResponse<Map<String,String>>` | `HealthController.java` | `api-spec.md` 5.1 | 用于启动探活 |
| 用户注册 | `/api/v1/auth/register` | POST | Auth | 是 | 是 | 已可用 | `RegisterRequest` | `RegisterResponse` | `AuthController.java` | `api-spec.md` 7.1 | 返回 `userId` |
| 用户登录 | `/api/v1/auth/login` | POST | Auth | 是 | 是 | 已可用 | `LoginRequest` | `LoginResponse` | `AuthController.java` | `api-spec.md` 7.2 | token 位于 `data.token` |
| 当前用户 | `/api/v1/auth/me` | GET | Auth | 是 | 是 | 已可用 | Header `Authorization` | `UserVO` | `AuthController.java` | `SecurityConfig.java`、`api-spec.md` 7.3 | 需要 Bearer token |
| 获取当前用户偏好 | `/api/v1/user-preferences/me` | GET | UserPreference | 是 | 是 | 已可用 | 无 | `UserPreferenceVO` | `UserPreferenceController.java` | `api-spec.md` 8.1 | 需要登录 |
| 保存/更新当前用户偏好 | `/api/v1/user-preferences/me` | PUT | UserPreference | 是 | 是 | 已可用 | `UserPreferenceRequest` | `UserPreferenceVO` | `UserPreferenceController.java` | `api-spec.md` 8.2 | 需要登录 |
| 推荐目的地 | `/api/v1/destinations/recommend` | GET | Recommend | 是 | 是 | 已可用 | `DestinationRecommendQuery` | `PageResultVO<DestinationVO>` | `DestinationController.java` | `api-spec.md` 9.1 | 公开接口 |
| 搜索目的地 | `/api/v1/destinations/search` | GET | Recommend | 是 | 是 | 已可用 | `DestinationSearchQuery` | `PageResultVO<DestinationVO>` | `DestinationController.java` | `api-spec.md` 9.2 | 公开接口 |
| 目的地详情 | `/api/v1/destinations/{id}` | GET | Recommend | 是 | 是 | 已可用 | path `id` | `DestinationVO` | `DestinationController.java` | `api-spec.md` 9.3 | 公开接口 |
| 目的地下场所列表 | `/api/v1/destinations/{id}/places` | GET | Recommend/Place | 是 | 是 | 已可用 | path `id` + `DestinationPlacesQuery` | `List<PlaceVO>` | `DestinationController.java` | `api-spec.md` 9.4 | 公开接口 |
| 目的地相关日记 | `/api/v1/destinations/{id}/diaries` | GET | Diary | 是 | 是 | 已可用 | path `id` + `DiaryListQuery` | `PageResultVO<DiaryVO>` | `DiaryController.java` | `api-spec.md` 9.5 | 公开接口 |
| 单目标路线规划 | `/api/v1/routes/plan/single` | POST | Route | 是 | 是 | 已可用 | `SingleRoutePlanRequest` | `RoutePlanVO` | `RouteController.java` | `api-spec.md` 10.1 | 需要登录，支持距离/时间策略及 `walk/bike/cart/mixed` |
| 多目标路线规划 | `/api/v1/routes/plan/multi` | POST | Route | 是 | 是 | 已可用 | `MultiRoutePlanRequest` | `RoutePlanVO` | `RouteController.java` | `api-spec.md` 10.2 | 需要登录；支持交通约束；代码实际返回 `RoutePlanVO` |
| 路线历史列表 | `/api/v1/routes/history` | GET | Route | 是 | 是 | 已可用 | `RouteHistoryPageQuery` | `PageResultVO<RouteHistoryVO>` | `RouteController.java`、`RouteServiceImpl.java` | `api-spec.md` 10.3 | JWT 当前用户；不接受 userId；按时间倒序 |
| 路线历史详情 | `/api/v1/routes/history/{id}` | GET | Route | 是 | 是 | 已可用 | path `id` | `RouteHistoryVO` | `RouteController.java`、`RouteServiceImpl.java` | `api-spec.md` 10.4 | 读取历史 JSON 快照，不重新规划；越权按不存在处理 |
| 附近设施 | `/api/v1/facilities/nearby` | GET | Facility | 是 | 是 | 已可用 | `NearbyFacilityQuery` | `PageResultVO<NearbyFacilityVO>` | `FacilityController.java` | `api-spec.md` 11.1 | 公开接口，基于图上可达距离 |
| 搜索设施 | `/api/v1/facilities/search` | GET | Facility | 否 | 是 | 文档有但代码未找到 | 文档定义 query | `Page<FacilityVO>` | 未找到 Controller 入口 | `api-spec.md` 11.2、`swagger-draft.yaml` | 当前只有 nearby |
| 美食推荐 | `/api/v1/foods/recommend` | GET | Food | 是 | 是 | 已可用 | `FoodRecommendQuery` | `PageResultVO<FoodVO>` | `FoodController.java` | `api-spec.md` 12.1 | 公开接口 |
| 美食搜索 | `/api/v1/foods/search` | GET | Food | 是 | 是 | 已可用 | `FoodSearchQuery` | `PageResultVO<FoodVO>` | `FoodController.java` | `api-spec.md` 12.2 | 公开接口 |
| 发布日记 | `/api/v1/diaries` | POST | Diary | 是 | 是 | 已可用 | `DiaryCreateRequest` | `DiaryCreateResponse` | `DiaryController.java` | `api-spec.md` 13.1 | 需要登录 |
| 日记列表 | `/api/v1/diaries` | GET | Diary | 是 | 是 | 已可用 | `DiaryListQuery` | `PageResultVO<DiaryVO>` | `DiaryController.java` | `api-spec.md` 13.2 | 公开接口 |
| 日记详情 | `/api/v1/diaries/{id}` | GET | Diary | 是 | 是 | 已可用 | path `id` | `DiaryVO` | `DiaryController.java`、`DiaryServiceImpl.java`、`DiaryMapper.java` | `api-spec.md` 13.3 | 成功查看后 `heatScore` 原子 +1；私有日记仅作者可查看并计数 |
| 日记标题检索 | `/api/v1/diaries/search/title` | GET | Diary/Search | 是 | 是 | 已可用 | `DiaryTitleSearchQuery` | `PageResultVO<DiaryVO>` | `DiaryController.java` | `api-spec.md` 13.4 | 公开接口 |
| 日记全文检索 | `/api/v1/diaries/search/fulltext` | GET | Diary/Search | 是 | 是 | 已可用 | `DiaryFulltextSearchQuery` | `PageResultVO<DiaryVO>` | `DiaryController.java` | `api-spec.md` 13.5 | 公开接口，当前基于 MySQL LIKE |
| 日记评分 | `/api/v1/diaries/{id}/ratings` | POST | Diary | 是 | 是 | 已可用 | `DiaryRatingRequest` | `Boolean` | `DiaryController.java`、`DiaryRatingServiceImpl.java` | `api-spec.md` 13.6、`swagger-draft.yaml` | 重复评分覆盖更新，需要登录 |
| 我的日记评分 | `/api/v1/diaries/{id}/ratings/me` | GET | Diary | 是 | 是 | 已可用 | path `id` | `DiaryRatingVO` | `DiaryController.java`、`DiaryRatingServiceImpl.java` | `api-spec.md` 13.6A、`swagger-draft.yaml` | 未评分时 `userScore=null`，需要登录 |
| 生成日记照片动画 | `/api/v1/diaries/{diaryId}/animation` | POST | AI/Diary | 是 | 是 | 已可用 | path `diaryId` | `DiaryAnimationVO` | `DiaryAnimationController.java`、`AnimationServiceImpl.java`、`OpenAiCompatibleAnimationProvider.java`、`MockTemplateAnimationProvider.java` | `api-spec.md` 13.9、`swagger-draft.yaml` | 仅作者；默认模板，可配置真实多模态 Provider，失败自动降级；不导出 MP4 |
| 查询日记照片动画 | `/api/v1/diaries/{diaryId}/animation` | GET | AI/Diary | 是 | 是 | 已可用 | path `diaryId` | `DiaryAnimationVO` | `DiaryAnimationController.java`、`AnimationServiceImpl.java` | `api-spec.md` 13.9、`swagger-draft.yaml` | 公开日记匿名可查，私有日记仅作者 |
| 目的地评论列表/发布 | `/api/v1/destinations/{id}/comments` | GET/POST | Comment/Destination | 是 | 是 | 已可用 | `CommentPageQuery` / `CommentCreateRequest` | `PageResultVO<CommentVO>` / `CommentVO` | `DestinationController.java`、`CommentServiceImpl.java` | `api-spec.md` 13.8、`swagger-draft.yaml` | GET 公开，POST 登录 |
| 美食评论列表/发布 | `/api/v1/foods/{id}/comments` | GET/POST | Comment/Food | 是 | 是 | 已可用 | `CommentPageQuery` / `CommentCreateRequest` | `PageResultVO<CommentVO>` / `CommentVO` | `FoodController.java`、`CommentServiceImpl.java` | `api-spec.md` 13.8、`swagger-draft.yaml` | GET 公开，POST 登录 |
| 日记评论列表/发布 | `/api/v1/diaries/{id}/comments` | GET/POST | Comment/Diary | 是 | 是 | 已可用 | `CommentPageQuery` / `CommentCreateRequest` | `PageResultVO<CommentVO>` / `CommentVO` | `DiaryController.java`、`CommentServiceImpl.java` | `api-spec.md` 13.8、`swagger-draft.yaml` | 只允许公开启用日记 |
| 评论软删除/隐藏 | `/api/v1/comments/{commentType}/{commentId}` | DELETE | Comment | 是 | 是 | 已可用 | path `commentType/commentId` | `Boolean` | `CommentController.java`、`CommentServiceImpl.java` | `api-spec.md` 13.8、`swagger-draft.yaml` | 所有者置 2，管理员置 0 |
| 我的日记 | `/api/v1/diaries/me` | GET | Diary | 否 | 是 | 文档有但代码未找到 | query | `Page<DiaryVO>` | 未找到 Controller 入口 | `api-spec.md` 13.7、`swagger-draft.yaml` | 文档标“建议补充” |
| 文件上传 | `/api/v1/files/upload` | POST | File | 是 | 是 | 已可用 | multipart `file,bizType,refId` | `FileUploadResultVO` | `FileController.java` | `api-spec.md` 14.1 | 需要登录 |
| 管理端目的地列表 | `/api/v1/admin/destinations` | GET | Admin | 是 | 是 | 已可用 | `AdminPageQuery` | `PageResultVO<AdminDestinationVO>` | `AdminController.java` | `api-spec.md` 15.2 | 需要 admin |
| 管理端目的地新增 | `/api/v1/admin/destinations` | POST | Admin | 是 | 是 | 已可用 | `AdminDestinationRequest` | `AdminDestinationVO` | `AdminController.java` | `api-spec.md` 15.3 | 文档早期 Swagger 可能写成 IdResult，代码返回 VO |
| 管理端目的地修改 | `/api/v1/admin/destinations/{id}` | PUT | Admin | 是 | 是 | 已可用 | path `id` + `AdminDestinationRequest` | `AdminDestinationVO` | `AdminController.java` | `api-spec.md` 15.4 | 需要 admin |
| 管理端目的地删除/下架 | `/api/v1/admin/destinations/{id}` | DELETE | Admin | 是 | 是 | 已可用 | path `id` | `Boolean` | `AdminController.java` | `api-spec.md` 15.5 | 逻辑下架 |
| 管理端场所维护 | `/api/v1/admin/places[/{id}]` | GET/POST/PUT/DELETE | Admin | 是 | 是 | 已可用 | `AdminPageQuery` / `AdminPlaceRequest` | `PageResultVO<AdminPlaceVO>` / `AdminPlaceVO` / `Boolean` | `AdminController.java` | `api-spec.md` 15.7-15.7C | 需要 admin |
| 管理端设施维护 | `/api/v1/admin/facilities[/{id}]` | GET/POST/PUT/DELETE | Admin | 是 | 是 | 已可用 | `AdminPageQuery` / `AdminFacilityRequest` | `PageResultVO<AdminFacilityVO>` / `AdminFacilityVO` / `Boolean` | `AdminController.java` | `api-spec.md` 15.6、15.8-15.10 | 需要 admin |
| 管理端美食维护 | `/api/v1/admin/foods[/{id}]` | GET/POST/PUT/DELETE | Admin | 是 | 是 | 已可用 | `AdminPageQuery` / `AdminFoodRequest` | `PageResultVO<FoodVO>` / `FoodVO` / `Boolean` | `AdminController.java` | `api-spec.md` 15.11-15.14 | 需要 admin |
| 管理端地图节点维护 | `/api/v1/admin/map/nodes[/{id}]` | GET/POST/PUT/DELETE | Admin | 是 | 是 | 已可用 | `AdminPageQuery` / `AdminMapNodeRequest` | `PageResultVO<AdminMapNodeVO>` / `AdminMapNodeVO` / `Boolean` | `AdminController.java` | `api-spec.md` 15.15-15.15B | 需要 admin |
| 管理端地图边维护 | `/api/v1/admin/map/edges[/{id}]` | GET/POST/PUT/DELETE | Admin | 是 | 是 | 已可用 | `AdminPageQuery` / `AdminMapEdgeRequest` | `PageResultVO<AdminMapEdgeVO>` / `AdminMapEdgeVO` / `Boolean` | `AdminController.java` | `api-spec.md` 15.16-15.16B | 需要 admin |
| 管理端用户列表 | `/api/v1/admin/users` | GET | Admin | 是 | 是 | 已可用 | `AdminPageQuery` | `PageResultVO<AdminUserVO>` | `AdminController.java` | `api-spec.md` 15.1 | 需要 admin |
| 管理端用户状态 | `/api/v1/admin/users/{id}/status` | PUT | Admin | 是 | 是 | 已可用 | `AdminStatusRequest` | `AdminUserVO` | `AdminController.java` | `api-spec.md` 15.17A | 需要 admin |
| 管理端日记列表 | `/api/v1/admin/diaries` | GET | Admin | 是 | 是 | 已可用 | `AdminPageQuery` | `PageResultVO<AdminDiaryVO>` | `AdminController.java` | `api-spec.md` 15.17 | 需要 admin |
| 管理端日记状态 | `/api/v1/admin/diaries/{id}/status` | PUT | Admin | 是 | 是 | 已可用 | `AdminStatusRequest` | `AdminDiaryVO` | `AdminController.java` | `api-spec.md` 15.17B | 需要 admin |
| 导入预览 | `/api/v1/admin/import-batches/preview` | POST | Admin/Import | 是 | 是 | 已可用 | multipart `targetTable,sourceType,file` | `ImportPreviewResult` | `AdminController.java` | `api-spec.md` 15.18 | 需要 admin，不写业务表 |
| 执行导入 | `/api/v1/admin/import-batches` | POST | Admin/Import | 是 | 是 | 已可用 | multipart `targetTable,sourceType,file` | `ImportResult` | `AdminController.java` | `api-spec.md` 15.18 | 需要 admin |
| 导入批次列表 | `/api/v1/admin/import-batches` | GET | Admin/Import | 是 | 是 | 已可用 | `AdminPageQuery` + `status` | `PageResultVO<AdminImportBatchVO>` | `AdminController.java` | `api-spec.md` 15.19 | 需要 admin |
| 导入失败明细 | `/api/v1/admin/import-batches/{batchId}/failures` | GET | Admin/Import | 是 | 是 | 已可用 | path `batchId` + `AdminPageQuery` | `PageResultVO<AdminImportFailureVO>` | `AdminController.java` | `api-spec.md` 15.20 | 需要 admin |
| AI 日记草稿/图片摘要/路线回顾/多人协商/推荐理由 | `/api/v1/ai/**` | POST | AI | 否 | 是 | 文档有但代码未找到 | 文档定义 | 文档定义 | 未找到对应 AI Controller | `api-spec.md` 16 | P2 预留；不包含已实现的日记照片动画资源接口 |

## 4. 待确认项
- `frontend-runtime-facts.md` 未找到，无法确认前端技术栈、路由模式、baseURL、proxy、token 存储和 auth header 处理。
- Route 历史接口已补后端实现，待前端页面和 MySQL 实库接口回归。
- 我的日记列表接口是否仍属于 P1 当前范围。
- `GET /api/v1/facilities/search` 是否需要补实现，或只保留 `nearby`。
- AI 接口是否只保留文档预留，不进入当前联调。

## 5. 代码/文档冲突项
- Route 历史接口原先存在“文档已定义、代码未实现”的冲突，当前已消除。
- `api-spec.md` / `swagger-draft.yaml` 定义 `GET /api/v1/facilities/search`，当前 `FacilityController` 只实现 `nearby`。
- `api-spec.md` / `swagger-draft.yaml` 定义我的日记列表接口，当前 `DiaryController` 未找到对应入口。
- `api-spec.md` 说明文件上传返回不包含 `id`，`swagger-draft.yaml` 的 `FileUploadResultVO` 仍包含 `id`；代码 `FileUploadResultVO` 不包含 `id`。
- `swagger-draft.yaml` 的 `CreateDiaryRequest` 使用 `mediaIds`，代码 `DiaryCreateRequest` 使用 `mediaList`。

## 6. 建议下一步动作
- 优先决定文档有但代码未实现的接口是否进入近期开发。
- 将 `swagger-draft.yaml` 与当前 DTO/VO 同步，尤其是 `DiaryCreateRequest`、`FileUploadResultVO`、Admin 新增/修改返回对象。
- 补充或提交 `frontend-runtime-facts.md`，再更新运行环境和鉴权联调约定。
