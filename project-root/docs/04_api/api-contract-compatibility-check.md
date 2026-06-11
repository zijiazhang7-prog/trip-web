# API 契约恢复与后端兼容性检查记录

## 1. 背景

本记录用于处理一次 API 文档同步风险：后端实现过程中部分接口字段和路径没有及时同步 `swagger-draft.yaml`，随后又一次性将 Swagger 草稿大幅改成“当前后端实现口径”。由于前端接口代码已经依据修改前的 Swagger 生成，继续使用大改后的 Swagger 会导致联调契约漂移。

当前处理原则：

- 本轮联调主契约先以恢复后的 `project-root/docs/04_api/swagger-draft.yaml` 为准。
- 本轮不继续扩大 Swagger 主契约修改。
- 后端优先兼容前端已生成接口契约。
- 若确实需要改契约，必须先由前后端确认，再同步 Swagger。

## 2. 已执行的恢复动作

已将以下两个文件的大幅修改保存到 Git stash，并恢复工作区主契约文件：

- `project-root/docs/04_api/swagger-draft.yaml`
- `project-root/docs/04_api/api-changelog.md`

保存位置：

- `stash@{0}: backup api contract draft sync before frontend compatibility restore`

当前这份 stash 仅作为“后端实现与 Swagger 草稿差异”的参考材料，不作为本轮前端联调的主契约。

## 3. 已读取的关键文件

规则与计划：

- `project-root/AGENTS.md`
- `project-root/coding-rules.md`
- `project-root/security-rules.md`
- `project-root/docs/02_architecture/coding-plan.md`

API 契约：

- `project-root/docs/04_api/swagger-draft.yaml`
- `project-root/docs/04_api/api-changelog.md`

后端入口与关键 DTO / VO：

- `project-root/backend/src/main/java/com/trip/controller/AuthController.java`
- `project-root/backend/src/main/java/com/trip/controller/DestinationController.java`
- `project-root/backend/src/main/java/com/trip/controller/RouteController.java`
- `project-root/backend/src/main/java/com/trip/controller/FacilityController.java`
- `project-root/backend/src/main/java/com/trip/controller/DiaryController.java`
- `project-root/backend/src/main/java/com/trip/controller/FileController.java`
- `project-root/backend/src/main/java/com/trip/controller/FoodController.java`
- `project-root/backend/src/main/java/com/trip/controller/AdminController.java`
- `project-root/backend/src/main/java/com/trip/config/SecurityConfig.java`
- `project-root/backend/src/main/java/com/trip/dto/request/DiaryCreateRequest.java`
- `project-root/backend/src/main/java/com/trip/dto/request/FoodRecommendQuery.java`
- `project-root/backend/src/main/java/com/trip/dto/request/NearbyFacilityQuery.java`
- `project-root/backend/src/main/java/com/trip/vo/response/FileUploadResultVO.java`
- `project-root/backend/src/main/java/com/trip/vo/response/NearbyFacilityVO.java`
- `project-root/backend/src/main/java/com/trip/vo/response/RoutePlanVO.java`

补充说明：

- 当前仓库未找到 `frontend-runtime-facts.md`，因此本检查不能确认前端运行时代码实际调用了哪些接口，只能按“前端已生成接口契约”做后端兼容性分析。

## 4. 当前后端已实现接口入口概览

已在 Controller 中找到的主要接口：

| 模块 | 后端入口 |
|---|---|
| Auth | `POST /api/v1/auth/register`、`POST /api/v1/auth/login`、`GET /api/v1/auth/me` |
| UserPreference | `GET /api/v1/user-preferences/me`、`PUT /api/v1/user-preferences/me` |
| Destination / Recommend | `GET /api/v1/destinations/recommend`、`GET /api/v1/destinations/search`、`GET /api/v1/destinations/{id}`、`GET /api/v1/destinations/{id}/places` |
| Route | `POST /api/v1/routes/plan/single`、`POST /api/v1/routes/plan/multi` |
| Facility | `GET /api/v1/facilities/nearby` |
| File | `POST /api/v1/files/upload` |
| Diary | `POST /api/v1/diaries`、`GET /api/v1/diaries`、`GET /api/v1/diaries/{id}`、`GET /api/v1/destinations/{id}/diaries`、`GET /api/v1/diaries/search/title`、`GET /api/v1/diaries/search/fulltext` |
| Food | `GET /api/v1/foods/recommend`、`GET /api/v1/foods/search` |
| Admin / Import | 管理端目的地、场所、设施、美食、地图节点 / 边、用户状态、日记状态、导入批次相关接口 |

## 5. 按旧 Swagger 发现的高风险兼容项

| 优先级 | 接口 / 字段 | 旧 Swagger 契约现状 | 后端实现现状 | 兼容风险 | 建议处理 |
|---|---|---|---|---|---|
| 高 | `POST /api/v1/diaries` 请求体 | `CreateDiaryRequest.mediaIds` | `DiaryCreateRequest.mediaList` | 前端若按 `mediaIds` 提交，后端不会接收媒体列表 | 后端增加兼容接收 `mediaIds`，或前后端确认改用 `mediaList` |
| 高 | `POST /api/v1/files/upload` 响应 | Swagger 定义 `FileUploadResultVO`，历史版本可能包含文件 id | 后端实际返回 `bizType`、`fileName`、`fileUrl` | 前端若依赖 `id` 关联日记媒体，会无法发布图文日记 | 优先确认前端是否只使用 `fileUrl`；必要时后端补兼容字段 |
| 高 | `GET /api/v1/facilities/search` | Swagger 中存在 | 后端未找到对应 Controller 入口 | 前端调用会 404 | 若前端本轮需要场所 / 设施搜索，应后端补最小兼容接口；否则标记非本轮联调 |
| 高 | `GET /api/v1/diaries/me` | Swagger 中存在 | 后端未找到对应 Controller 入口 | 前端调用会 404 | 若前端有“我的日记”页，应后端补最小查询接口 |
| 中 | `GET /api/v1/routes/history`、`GET /api/v1/routes/history/{id}` | Swagger 中存在 | 后端未找到对应 Controller 入口 | 前端调用会 404 | 若本轮只演示路线规划结果，可暂不联调；若有历史页，需要补接口 |
| 中 | `POST /api/v1/diaries/{id}/ratings` | Swagger 中存在 | 后端未找到对应 Controller 入口 | 前端调用会 404 | Diary 评分属于 P1，建议本轮不作为 P0 联调阻塞 |
| 中 | `GET /api/v1/foods/recommend` 查询参数 | 后端当前使用 `destinationId`、`facilityId`、`foodType`、`sortBy`、`topK` | 旧 Swagger 中需重点核对是否使用 `sourceNodeId` | 参数名不一致会导致筛选条件失效 | 以前端已生成字段为准补 alias 或确认改契约 |
| 中 | `GET /api/v1/facilities/nearby` 响应字段 | 旧 Swagger 需与 `NearbyFacilityVO` 对齐 | 后端实际包含 `reachableDistance`、`sourceNodeId` 等 | 前端展示距离 / 节点字段可能不一致 | 优先保证 `id/name/type/distance` 类展示字段可用，补兼容别名需确认 |
| 中 | Route 响应字段 | 旧 Swagger 需与 `RoutePlanVO` 对齐 | 后端实际包含 `pathNodes`、`pathEdges` | 前端路径绘制依赖字段名，改名风险高 | 冻结 `pathNodes`、`pathEdges`，不再随意改名 |

## 6. 权限与联调注意事项

来自 `SecurityConfig.java` 的当前后端事实：

- `GET /api/v1/health` 免登录。
- `POST /api/v1/auth/register`、`POST /api/v1/auth/login` 免登录。
- `GET /api/v1/destinations/**`、`GET /api/v1/facilities/**`、`GET /api/v1/foods/**`、`GET /api/v1/diaries/**` 免登录。
- `GET /api/v1/auth/me` 需要登录。
- `POST /api/v1/files/upload` 需要登录。
- `/api/v1/admin/**` 需要 `admin` 角色。
- 其他未显式放行接口默认需要登录。

因此 P0 演示时：

- 登录接口可直接调用。
- 推荐、搜索、设施查询、日记列表 / 详情可匿名 GET。
- 日记发布、路线规划、文件上传等写操作需要携带 JWT。

## 7. 建议的后端兼容修复顺序

1. 先确认前端本轮实际页面是否会调用 `routes/history*`、`facilities/search`、`diaries/me`、`diaries/{id}/ratings`。
2. 对 P0 主线必须调用的接口，优先补后端兼容，而不是修改 Swagger。
3. 对 Diary 发布，优先解决 `mediaIds` 与 `mediaList` 的兼容问题。
4. 对 File 上传，确认前端是否依赖响应中的 `id`；若依赖，后端需给出兼容字段或调整发布日记流程。
5. 对 Route / Facility / Recommend 的响应字段，冻结当前前端展示所需字段，不再做破坏性改名。

## 8. 待确认项

- 前端生成代码实际依据的是哪一个 Swagger 版本或提交。
- 前端本轮联调是否会调用 `facilities/search`、`diaries/me`、`routes/history*`。
- 前端上传文件后是否依赖文件 `id`，还是只依赖 `fileUrl`。
- 前端日记发布当前使用 `mediaIds` 还是 `mediaList`。
- `frontend-runtime-facts.md` 是否存在于未提交目录或其他路径。

## 9. 代码 / 文档冲突项

- Swagger 中存在 `GET /api/v1/facilities/search`，后端未找到对应接口入口。
- Swagger 中存在 `GET /api/v1/diaries/me`，后端未找到对应接口入口。
- Swagger 中存在 `GET /api/v1/routes/history` 与 `GET /api/v1/routes/history/{id}`，后端未找到对应接口入口。
- Swagger 中存在 `POST /api/v1/diaries/{id}/ratings`，后端未找到对应接口入口。
- 日记发布媒体字段存在 `mediaIds` 与 `mediaList` 的契约差异风险。
- 文件上传响应字段与旧 Swagger / 前端生成代码可能存在 `id` 字段差异风险。

## 10. 建议下一步动作

下一步不要继续改 Swagger 主契约。建议先做一个“P0 前端联调兼容补丁”：

- 若前端需要发布图文日记，后端兼容 `mediaIds` 或明确改成 `mediaList`。
- 若前端需要我的日记页，补 `GET /api/v1/diaries/me` 最小接口。
- 若前端需要设施搜索页，补 `GET /api/v1/facilities/search` 最小接口。
- 若前端不展示路线历史和日记评分，本轮标记为非联调范围，暂不实现。
- 完成后再更新文档：只记录兼容补丁，不重写整体 Swagger。
