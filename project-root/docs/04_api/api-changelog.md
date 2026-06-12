- 作用：接口一旦改了，记录变更。

# 接口变更记录（API Changelog）

## 1. 文件用途
本文件用于记录个性化旅游系统项目中 API 接口的新增、修改、废弃和删除情况，作为前后端协作、联调排查、测试回归和版本说明的依据。

本文件关注的是：
- 哪个接口发生了变化
- 变化内容是什么
- 为什么改
- 是否影响前端、数据库、测试和文档
- 是否需要做回归测试

相关文档：
- `project-root/docs/04_api/api-spec.md`
- `project-root/docs/04_api/error-codes.md`
- `project-root/docs/06_testing/test-cases.md`
- `project-root/docs/06_testing/bug-log.md`

---

## 2. 使用原则

1. 只要接口的路径、方法、参数、返回结构、错误码或业务语义发生变化，都应记录到本文件。
2. 文档中的变更记录应尽量和代码提交、接口实现保持同步。
3. 同一轮开发中的多次小修改，可以合并为一条变更记录。
4. 如果某次变更会影响前端、测试、数据库或其他模块，必须在记录中写清楚。
5. 已废弃或已删除的接口不能直接从历史中抹掉，应保留变更记录。

---

## 3. 版本记录方式

建议按“版本号 + 日期 + 变更摘要”的方式记录。

## 2026-06-12 AIGC 日记照片动画 MVP

- 新增 `POST /api/v1/diaries/{diaryId}/animation`：仅作者生成或覆盖动画脚本。
- 新增 `GET /api/v1/diaries/{diaryId}/animation`：公开日记匿名可查，私有日记仅作者可查。
- 新增 `DiaryAnimationVO`、`AnimationScriptVO`、`AnimationSceneVO`。
- 新增错误码 `AI_009`、`AI_010`，并正式使用 `AI_001`、`AI_002`、`AI_007`。
- 不修改任何现有 Diary、File 或 AI 预留接口字段语义。
- 新增可配置的 `openai-compatible` 多模态 Provider，默认仍使用 `mock-template`，外部调用失败时自动降级。
- `DiaryAnimationVO.provider` 新增可选值 `openai-compatible`。
- `AnimationSceneVO` 新增向后兼容的可选字段 `visualDescription`；旧 `script_json` 仍可读取。
- 动画接口路径、HTTP 方法、请求体、权限和 `diary_animation` 表结构均未变化。

## 4. 2026-06-10 Diary 评分闭环

- 实现既有 `POST /api/v1/diaries/{id}/ratings`，响应继续为 Boolean，不破坏已生成前端契约。
- 新增 `GET /api/v1/diaries/{id}/ratings/me`，返回 `diaryId/userScore/ratingScore/ratingCount`。
- `DiaryVO` 和管理端日记响应增加 `ratingCount`。
- 两个评分接口均要求 JWT；评分范围 1～5，重复评分覆盖更新。
- 数据库新增 `diary_rating` 明细表及 `diary.rating_count` 聚合字段。

## 4. 2026-06-08 Facility / Food 展示字段补充

- Facility 管理端请求、管理端响应和附近设施响应增加可选字段 `address`、`tel`、`coverUrl`。
- Food 管理端请求和 FoodVO 增加可选字段 `lng`、`lat`。
- API 路径、HTTP 方法、分页、鉴权和排序语义均未变化。
- 三类评论表本轮仅完成数据结构，不新增评论 API。

### 3.1 版本格式建议
可采用以下格式之一：

- `v0.1.0`
- `v0.2.0`
- `v0.2.1`

建议含义如下：

- **主版本号**：接口体系发生明显重构时再变
- **次版本号**：新增接口、重要字段变更、模块级更新
- **修订号**：小修复、小字段补充、错误码调整

### 3.2 当前项目建议
课程项目阶段可以采用较轻量的规则：

- `v0.1.0`：初版接口草案
- `v0.2.0`：主线接口落地
- `v0.2.1`：联调修订
- `v0.3.0`：增强模块接口加入
- `v1.0.0`：验收前稳定版

---

## 4. 变更类型说明

建议统一使用以下变更类型：

| 类型 | 含义 |
|---|---|
| Added | 新增接口或新增字段 |
| Changed | 修改了接口行为、参数或返回结构 |
| Deprecated | 接口保留但不再建议继续使用 |
| Removed | 接口已删除 |
| Fixed | 修复接口错误，但不一定改动结构 |
| Security | 权限、认证、上传限制等安全相关调整 |

---

## 5. 单条记录模板

建议每条变更记录至少包含以下内容：

### 模板
- **版本**：
- **日期**：
- **变更类型**：
- **接口**：
- **变更内容**：
- **变更原因**：
- **影响范围**：
- **是否需要前端修改**：
- **是否需要测试回归**：
- **备注**：

---

## 6. 当前版本初始记录

## [v0.3.7] - 2026-05-07

### Added
- **接口**：`POST /api/v1/routes/plan/multi`
- **变更内容**：新增 Route 多目标路径规划接口实现，支持 `destinationId`、`startNodeId`、`targetNodeIds`、`strategyType`、`transportType`、`returnToStart`，返回拼接后的 `RoutePlanVO` 并写入 `route_history`。
- **变更原因**：按 `coding-plan.md` P1 顺序补齐 Route 多目标路径规划基础版。
- **影响范围**：Route 模块、MapService 图算法、路线历史、测试用例 `TC-ROUTE-007 ~ TC-ROUTE-010`。
- **是否需要前端修改**：后续前端工程可用时需要对接多目标路线规划页面；当前不修改前端。
- **是否需要测试回归**：是，已补 `MapServiceTests`、`RouteServiceTests` 并执行后端全量 `mvn test`。
- **备注**：当前采用最近邻启发式，不保证 TSP 全局最优；仅支持 `shortest_distance`，最短时间和交通工具约束后续再补。

---

## [v0.3.6] - 2026-05-06

### Added
- **接口**：`GET /api/v1/admin/places`、`POST /api/v1/admin/places`、`PUT /api/v1/admin/places/{id}`、`DELETE /api/v1/admin/places/{id}`
- **变更内容**：新增 Admin 场所 / 建筑物管理接口，支持分页查询、新增、修改和删除场所。
- **变更原因**：补齐 P1 Admin 后台数据维护闭环，支撑目的地内部场所数据维护。
- **影响范围**：Admin 模块、Place 数据维护、测试用例 `TC-ADMIN-004`。
- **是否需要前端修改**：后续前端工程可用时需要对接场所管理页面；当前不修改前端。
- **是否需要测试回归**：是，已补 `AdminServiceTests` 并执行后端全量 `mvn test`。
- **备注**：当前 `place` 表没有 `status` 字段，无下游引用时按物理删除处理；若被设施或地图节点引用，则拒绝删除。

---

## [v0.3.5] - 2026-05-06

### Added
- **接口**：`GET /api/v1/admin/import-batches`、`GET /api/v1/admin/import-batches/{batchId}/failures`
- **变更内容**：新增导入批次分页查询和失败明细分页查询接口，支持管理员回看导入结果和定位失败行。
- **变更原因**：继续完善 P1 Admin 后台支撑，补齐 ImportService 执行后的结果查看能力。
- **影响范围**：Admin 模块、ImportService 查询能力、测试用例 `TC-IMPORT-005 ~ TC-IMPORT-006`。
- **是否需要前端修改**：后续前端工程可用时需要对接导入历史和失败明细页面；当前不修改前端。
- **是否需要测试回归**：是，已补 `AdminServiceTests`，后续可做实库接口验证。
- **备注**：不新增表结构，不改变 multipart 导入接口；查询结果来自已有 `import_batch` 与 `import_failure` 表。

---

## [v0.3.4] - 2026-05-06

### Changed
- **接口**：`POST /api/v1/admin/import-batches/preview`、`POST /api/v1/admin/import-batches`
- **变更内容**：导入接口由 JSON 元信息请求改为 `multipart/form-data`，参数为 `targetTable`、`sourceType`、`file`；当前支持 `csv/json`，并返回导入预览或执行摘要。
- **变更原因**：按 `coding-plan.md` P1 顺序完成 ImportService 完整化基础版，支持标准化 CSV / JSON 文件真实解析入库。
- **影响范围**：Admin 模块、ImportService、数据库 `import_batch/import_failure`、测试用例 `TC-IMPORT-001 ~ TC-IMPORT-003`。
- **是否需要前端修改**：后续前端工程可用时需要以 multipart 方式对接；当前不修改前端。
- **是否需要测试回归**：是，已执行 `ImportServiceTests`，后续需要补实库 multipart 接口验证。
- **备注**：Controller 负责接收 `MultipartFile`，ImportService 核心基于 `Reader`，当前不支持 Excel / SQL 上传执行。

---

## [v0.3.3] - 2026-05-06

### Added
- **接口**：`GET /api/v1/diaries/search/title`、`GET /api/v1/diaries/search/fulltext`
- **变更内容**：新增 SearchService 基础版，支持日记标题检索和正文关键词检索，当前基于 MySQL `LIKE` + 分页查询实现。
- **变更原因**：按 `coding-plan.md` P1 顺序进入 SearchService，为 Diary 检索增强提供公共能力。
- **影响范围**：SearchService、Diary 模块、测试用例 `TC-DIARY-011 ~ TC-DIARY-012`。
- **是否需要前端修改**：后续前端工程可用时需要对接；当前不修改前端。
- **是否需要测试回归**：是，已执行 `SearchServiceTests`、`DiaryServiceTests` 和后端全量 `mvn test`。
- **备注**：当前不新增全文索引和第三方检索依赖，适合课程设计小规模样例数据；倒排索引或 MySQL FULLTEXT 留后续增强。

---

## [v0.3.2] - 2026-05-06

### Added
- **接口**：`/api/v1/admin/map/nodes`、`/api/v1/admin/map/edges`、`/api/v1/admin/users`、`/api/v1/admin/diaries`、`/api/v1/admin/import-batches`
- **变更内容**：补充 Admin 后续底层维护接口，支持地图节点 / 边维护、用户状态修改、日记状态修改和导入预览 / 执行入口。
- **变更原因**：按 `coding-plan.md` P1 Admin 后续范围补齐更底层的数据维护能力。
- **影响范围**：Admin 模块、MapService 图数据来源、Diary 状态管理、ImportService 最小骨架、测试用例 `TC-ADMIN-007 ~ TC-ADMIN-011`。
- **是否需要前端修改**：后续前端工程可用时需要对接；当前不修改前端。
- **是否需要测试回归**：是，已执行 `AdminServiceTests` 和后端全量 `mvn test`。
- **备注**：导入入口当前复用 `ImportService` 最小骨架，真实解析和入库留到 ImportService 完整化阶段。

---

## [v0.3.1] - 2026-05-05

### Added
- **接口**：`/api/v1/admin/destinations`、`/api/v1/admin/facilities`、`/api/v1/admin/foods`
- **变更内容**：新增 Admin 最小后台维护接口，支持目的地、设施、美食的列表、新增、修改和删除 / 下架。
- **变更原因**：Food 基础版完成后，按 `coding-plan.md` P1 顺序进入 Admin 最小后台能力。
- **影响范围**：Admin 模块、Security 管理员权限、测试用例 `TC-ADMIN-001`、`TC-ADMIN-002`、`TC-ADMIN-003`、`TC-ADMIN-005`、`TC-ADMIN-006`。
- **是否需要前端修改**：后续前端工程可用时需要对接；当前不修改前端。
- **是否需要测试回归**：是，需覆盖普通用户拒绝访问、管理员访问、基础数据维护。
- **备注**：当前不实现地图节点 / 边管理、用户状态、日记状态和导入入口。

---

## [v0.3.0] - 2026-05-05

### Added
- **接口**：`GET /api/v1/foods/recommend`、`GET /api/v1/foods/search`
- **变更内容**：新增 Food P1 基础版接口，支持按目的地、设施、菜系、关键字召回美食，并按热度或评分排序，推荐接口支持 Top-K 输出。
- **变更原因**：P0 后端基础闭环已完成，前端联调暂因工程代码不可用阻塞，按 `coding-plan.md` 进入 P1 Food 基础版。
- **影响范围**：Food 模块、QueryService、RankService、测试用例 `TC-FOOD-001 ~ TC-FOOD-004`。
- **是否需要前端修改**：后续前端工程可用时需要对接；当前不修改前端。
- **是否需要测试回归**：是，需执行 Food Service 最小单元测试，后续补实库接口测试。
- **备注**：当前基础版不实现价格区间、距离联动、个性化推荐和 Food 详情。

---

## [v0.2.1] - 2026-05-05

### Fixed
- **接口**：`GET /files/**`
- **变更内容**：修复本地上传文件静态资源访问路径，确保 `POST /api/v1/files/upload` 返回的 `/files/diary/...` URL 可直接通过 HTTP 访问。
- **变更原因**：P0 Diary 联调中发现文件已成功落盘，但访问返回 500，影响日记详情页媒体展示。
- **影响范围**：FileService、Diary 发布与详情展示、前端媒体预览。
- **是否需要前端修改**：否。
- **是否需要测试回归**：是，需回归上传后访问、日记发布、日记详情媒体展示。
- **备注**：实库联调验证中，上传后的 `/files/diary/...` 访问状态已为 200。

---

## [v0.2.0] - 2026-05-05

### Added
- **接口**：Auth / UserPreference / Destination / Route / Facility / Diary / File P0 接口组
- **变更内容**：落地 P0 后端基础接口，包括注册、登录、当前用户、当前用户偏好、目的地推荐/搜索/详情/场所、单目标路线规划、附近设施、日记发布/列表/详情/目的地相关日记、文件上传。
- **变更原因**：支撑 `coding-plan.md` 中 P0 “推荐-规划-查询-日记”最小主线闭环。
- **影响范围**：后端 P0 核心业务模块、前端页面联调、测试用例。
- **是否需要前端修改**：是，前端需按 `api-spec.md` 对接对应接口。
- **是否需要测试回归**：是，需覆盖 Auth、File、Diary 以及后续 Recommend / Route / Facility 页面联调。
- **备注**：P0 Diary 实库联调已验证 `Auth -> File -> Diary` 链路可跑通。

---

## [v0.1.1] - 2026-05-05

### Added
- **接口**：`GET /api/v1/health`
- **变更内容**：新增后端健康检查接口，返回统一 `ApiResponse` 与 `{"status":"ok"}` 数据。
- **变更原因**：支持工程骨架启动验证和前后端联调探活。
- **影响范围**：后端工程骨架、接口联调、测试记录。
- **是否需要前端修改**：否。
- **是否需要测试回归**：是，后续可加入基础接口连通性测试。
- **备注**：该接口不承载业务逻辑，不改变 P0 业务接口优先级。

---

## [v0.1.0] - YYYY-MM-DD

### Added
- 建立 `api-changelog.md` 文件，用于统一记录接口版本变化。
- 约定接口变更记录的格式、类型和维护方式。
- 初步约定主线模块接口将覆盖：
  - Auth
  - Recommend
  - Route
  - Facility
  - Food
  - Diary
  - Admin
  - Import
  - AI
  - UserPreference

### Notes
- 当前版本主要用于搭建接口变更记录机制，尚不代表所有接口已实现。
- 具体接口定义以 `api-spec.md` 为准。

---

## 7. 变更记录总表模板

| 版本 | 日期 | 类型 | 接口 | 变更摘要 | 影响范围 | 前端是否需改 | 是否需回归 |
|---|---|---|---|---|---|---|---|
| v0.1.0 | YYYY-MM-DD | Added | 全局 | 建立接口变更记录文档 | API 文档管理 | 否 | 否 |

---

## 8. 后续记录模板（可直接复制）

### [v0.x.x] - YYYY-MM-DD

#### Added
- **接口**：  
- **变更内容**：  
- **变更原因**：  
- **影响范围**：  
- **是否需要前端修改**：  
- **是否需要测试回归**：  
- **备注**：  

#### Changed
- **接口**：  
- **变更内容**：  
- **变更原因**：  
- **影响范围**：  
- **是否需要前端修改**：  
- **是否需要测试回归**：  
- **备注**：  

#### Fixed
- **接口**：  
- **变更内容**：  
- **变更原因**：  
- **影响范围**：  
- **是否需要前端修改**：  
- **是否需要测试回归**：  
- **备注**：  

#### Deprecated
- **接口**：  
- **变更内容**：  
- **变更原因**：  
- **影响范围**：  
- **是否需要前端修改**：  
- **是否需要测试回归**：  
- **备注**：  

#### Removed
- **接口**：  
- **变更内容**：  
- **变更原因**：  
- **影响范围**：  
- **是否需要前端修改**：  
- **是否需要测试回归**：  
- **备注**：  

#### Security
- **接口**：  
- **变更内容**：  
- **变更原因**：  
- **影响范围**：  
- **是否需要前端修改**：  
- **是否需要测试回归**：  
- **备注**：  

---

## 9. 推荐记录粒度

为了防止文档过碎或过粗，建议按下面粒度记录：

### 9.1 适合单独记录的变化
1. 新增一个接口
2. 删除一个接口
3. 路径变了
4. 请求方法变了
5. 请求参数变了
6. 返回字段结构变了
7. 错误码口径变了
8. 权限要求变了
9. 上传规则变了
10. 业务语义明显变化了

### 9.2 不必单独记录的变化
1. 注释文案小修
2. 接口说明错别字修正
3. 不影响调用方的小型内部重构
4. 不影响返回结果的日志调整

---

## 10. 推荐填写示例

### [v0.2.0] - YYYY-MM-DD

#### Added
- **接口**：`POST /api/v1/auth/login`
- **变更内容**：新增用户登录接口，返回 token 与当前用户基础信息。
- **变更原因**：支持前端登录态建立与后续受保护接口访问。
- **影响范围**：Auth 模块、前端登录页、权限校验。
- **是否需要前端修改**：是
- **是否需要测试回归**：是
- **备注**：需同步更新 `test-cases.md` 中 Auth 登录相关用例。

#### Added
- **接口**：`POST /api/v1/routes/plan/single`
- **变更内容**：新增单目标路径规划接口，支持起点、终点、策略类型输入。
- **变更原因**：支撑“旅游中”核心导航主线。
- **影响范围**：Route 模块、导航页、Facility 联动。
- **是否需要前端修改**：是
- **是否需要测试回归**：是
- **备注**：需重点验证不可达场景和非法节点输入。

#### Changed
- **接口**：`GET /api/v1/destinations/search`
- **变更内容**：新增 `type` 筛选参数，并统一分页返回结构。
- **变更原因**：支持校园 / 景区分类检索，并减少前后端字段不一致问题。
- **影响范围**：Recommend 模块、推荐页、测试用例。
- **是否需要前端修改**：是
- **是否需要测试回归**：是
- **备注**：需同步更新 `api-spec.md` 和推荐模块测试用例。

#### Fixed
- **接口**：`POST /api/v1/diaries`
- **变更内容**：修复上传媒体后 `mediaList` 未正确落库的问题。
- **变更原因**：联调阶段发现日记详情页媒体为空。
- **影响范围**：Diary 模块、日记详情页。
- **是否需要前端修改**：否
- **是否需要测试回归**：是
- **备注**：对应缺陷编号 `BUG-0XX`。

#### Security
- **接口**：`POST /api/v1/admin/import`
- **变更内容**：新增管理员角色校验，普通用户无法访问导入接口。
- **变更原因**：避免未授权用户触发批量导入。
- **影响范围**：Admin 模块、Import 模块、安全测试。
- **是否需要前端修改**：否
- **是否需要测试回归**：是
- **备注**：需回归 `TC-ADMIN-002`。

---

## 11. 与其他文档的联动规则

### 11.1 与 `api-spec.md` 的关系
- `api-spec.md` 记录“当前接口长什么样”
- `api-changelog.md` 记录“接口怎么变过”

### 11.2 与 `error-codes.md` 的关系
如果接口错误码发生新增、删除或语义变化，应同时更新：
- `api-changelog.md`
- `error-codes.md`

### 11.3 与测试文档的关系
如果接口发生如下变化，应同步检查并更新：
- `test-cases.md`
- `test-plan.md`
- `bug-log.md`
- `test-report.md`

### 11.4 与模块文档的关系
如果接口变化会影响模块职责、输入输出或依赖关系，应同步更新对应模块文档：
- `auth-module.md`
- `recommend-module.md`
- `route-module.md`
- `facility-module.md`
- `food-module.md`
- `diary-module.md`
- `admin-module.md`
- `ai-module.md`

---

## 12. 当前阶段推荐记录方式

你们现在还在开发前期，建议用最轻量的方式维护：

### 当前建议
1. 每完成一个新接口，就加一条 `Added`
2. 每次联调改接口字段，就加一条 `Changed`
3. 每修一个明显接口 bug，就加一条 `Fixed`
4. 每次加权限拦截、上传限制、token 校验，就加一条 `Security`

### 不建议
- 等到接口改了一大堆以后再回忆补文档
- 把所有变化都混成一句“更新接口若干”

---

## 13. 与测试和缺陷的联动建议

建议在记录里尽量带上这些信息：

- 对应模块
- 对应测试用例
- 对应 bug 编号（若有）

例如：
- **关联测试用例**：`TC-ROUTE-001`
- **关联缺陷**：`BUG-012`

这样后面查问题会非常方便。

---

## 14. 与其他文档的关系

本文件应与以下文档保持一致：

- `project-root/docs/04_api/api-spec.md`
- `project-root/docs/04_api/error-codes.md`
- `project-root/docs/05_modules/*`
- `project-root/docs/06_testing/test-cases.md`
- `project-root/docs/06_testing/bug-log.md`

如果接口路径、方法、参数、返回结构、错误码口径或权限要求发生变化，应同步更新本文件。

---

## 15. 后续维护说明

本文件应在以下场景下更新：

1. 新增接口时  
2. 修改接口字段或语义时  
3. 删除或废弃接口时  
4. 增加权限或安全限制时  
5. 联调中发现前后端接口口径不一致时  
6. 验收前需要整理稳定版 API 历史时

---

## 16. 2026-06-11 评论基础版

- 新增目的地、美食、日记评论列表与发布接口。
- 新增 `DELETE /api/v1/comments/{commentType}/{commentId}` 统一软删除/隐藏接口。
- 新增 `CommentCreateRequest`、`CommentVO` 和评论分页契约。
- GET 评论列表公开；发布和删除需要 JWT。
- 第一阶段只支持一级评论，正文最大 500 字符。

## 17. 2026-06-11 路线交通工具策略

- 路线 API 路径保持不变。
- 请求 `transportType` 增加 `mixed`，并正式启用 `walk/bike/cart` 道路过滤。
- `RouteEdgeVO` 兼容性新增 `transportType`，表示该条路径边实际使用的交通工具。
- `mixed` 当前仅支持 `shortest_time`，允许在公共节点零成本换乘。
- 数据库表结构不变，`map_edge.transport_type` 使用七种固定道路权限值。
