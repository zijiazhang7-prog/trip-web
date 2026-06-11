# 接口契约待确认问题（Open Questions）

## 1. 文件用途
本文件集中记录当前无法仅凭后端代码、现有 docs 和 `/.cursor/context/frontend-runtime-facts.md` 确认的问题。

## 2. 待确认问题清单

| 优先级 | 问题描述 | 涉及接口/模块 | 当前已发现证据 | 为什么无法确认 | 建议找谁确认 |
|---|---|---|---|---|---|
| 中 | 基于“相对路径 + Vite proxy”的联调参数是否还会调整 | 全部接口 | 已确认采用相对路径 + proxy，测试环境 `http://10.21.249.116:8080` | 需要在多环境切换时给出固定规则 | 前端负责人 + 后端负责人 |
| 低 | token 存储策略是否长期保持 localStorage | Auth/全局请求 | 已确认当前采用 localStorage | 后续若转 cookie/httpOnly 需安全评估 | 前端负责人 |
| 高 | request interceptor 统一位置未确认 | 全部受保护接口 | 后端鉴权方式明确，前端当前未实现统一拦截器 | 需要工程实现方案确认 | 前端负责人 |
| 高 | Route 历史接口文档有、代码没找到 | Route | `api-spec.md` 定义 `/api/v1/routes/history` 和 `/{id}`；`RouteController` 只有 plan single/multi | 代码未实现入口 | 后端负责人 / 文档负责人 |
| 高 | Diary 评分接口文档有、代码没找到 | Diary | `api-spec.md`、`swagger-draft.yaml` 定义 `/api/v1/diaries/{id}/ratings`；`DiaryController` 未实现 | 代码未实现入口 | 后端负责人 / 文档负责人 |
| 低 | `GET /api/v1/diaries/me` 文档有、代码没找到 | Diary | `api-spec.md` 标“建议补充”，Swagger 已列出；Controller 未实现 | 已确认本轮不纳入必须调通 | 后端负责人 / 文档负责人 |
| 低 | `GET /api/v1/facilities/search` 文档有、代码没找到 | Facility | `api-spec.md`、`swagger-draft.yaml` 定义；`FacilityController` 只有 `/nearby` | 已确认本轮先调 `nearby` | 后端负责人 / 文档负责人 |
| 高 | Swagger 的 Diary 发布请求字段与代码不一致 | Diary | `swagger-draft.yaml` 使用 `mediaIds`；`DiaryCreateRequest` 使用 `mediaList` | 文档未同步代码 | 文档负责人 / 后端负责人 |
| 高 | Swagger 的文件上传响应字段与代码不一致 | File | `swagger-draft.yaml` 的 `FileUploadResultVO` 含 `id`；代码 `FileUploadResultVO` 无 `id` | 文档未同步代码 | 文档负责人 / 后端负责人 |
| 中 | Food 推荐 query 字段口径不一致 | Food | `api-spec.md` 提到 `sourceNodeId`；代码 `FoodRecommendQuery` 使用 `facilityId` | 不确定后续是否做距离联动 | 后端负责人 / 文档负责人 |
| 中 | Facility nearby 响应字段口径不一致 | Facility | 文档写 `Page<FacilityVO>`；代码返回 `NearbyFacilityVO`，含 `reachableDistance` | 字段命名需统一 | 后端负责人 / 文档负责人 |
| 中 | Swagger 运行时访问方式未确认 | API 文档 | 有 `swagger-draft.yaml`；后端未找到 springdoc 配置 | 不知道是否需要 `/swagger-ui` | 后端负责人 |
| 中 | prod/test profile 是否需要补齐 | Runtime | 只有 `application.yml` 和 `application-dev.yml` | 无 test/prod 配置文件 | 后端负责人 |
| 低 | AI 接口上线时间点与契约冻结时间 | AI | 当前由后端推进，前端本轮不阻塞 | 影响下阶段排期，不影响本轮联调开工 | 项目负责人 / 后端负责人 |
| 中 | Admin 新增/修改返回对象与 Swagger 旧口径不一致 | Admin | Controller 返回 VO；Swagger 部分旧定义可能是 id/boolean | 需要统一最终契约 | 文档负责人 |
| 中 | 交通方式边过滤尚未实现是否影响 Route 契约 | Route | `api-spec.md` 明确 `transportType` 当前主要记录历史，非过滤 | 前端是否展示交通方式切换需确认 | 后端负责人 / 前端负责人 |
| 中 | 日记检索是否后续升级 FULLTEXT/倒排索引 | Diary/Search | `progress.md` 说明当前 LIKE，未实现 FULLTEXT | 当前语义可用，但性能边界需说明 | 后端负责人 |
| 低 | 上传文件访问域名是否需要拼接后端 origin | File | 返回 `fileUrl` 为 `/files/...` 相对路径 | 前端展示时是否需要补 origin 未确认 | 前端负责人 |
| 低 | CORS 允许源是否覆盖实际前端端口 | Runtime | 当前仅允许 `5173` 两种 localhost | 前端实际端口未知 | 前端负责人 / 后端负责人 |

## 3. 必须特别记录的问题类型覆盖

| 类型 | 当前记录 |
|---|---|
| 文档有、代码没找到的接口 | Route 历史、Facility 搜索、Diary 评分、我的日记、AI 接口 |
| token 返回位置不清楚 | 后端已确认在 `data.token`；前端保存位置待确认 |
| 鉴权方式不清楚 | 后端已确认 JWT Bearer；前端注入方式待确认 |
| 返回体不统一 | 大多数接口统一 `ApiResponse`；文档/Swagger 个别响应对象旧口径需同步 |
| DTO/VO 字段语义不清 | `sourceNodeId/facilityId`、`reachableDistance/distance`、`mediaIds/mediaList` |
| 环境配置不完整 | 缺少前端运行时事实、test/prod profile、测试服务器地址 |
| 路由冲突 | 暂未发现后端 Controller 路由冲突；`/api/v1/destinations/{id}/diaries` 由 DiaryController 实现，需文档说明归属 |
| Mock / 假数据 / TODO / FIXME / Not Implemented | 代码扫描未发现业务代码中的 `NOT_IMPLEMENTED`；文档历史记录中存在旧状态，当前需要以最新 progress 为准 |
| frontend-runtime-facts 与后端预期不一致 | 文件已存在，可比对；当前差异是前端仍处于 mock-first |

## 待确认项
- 联调阶段是否要求先切换核心接口到真实后端，再逐步淘汰 mock。
- 当前接口冻结范围是否排除文档有但代码未实现的接口。
- Swagger 是否作为最终契约，还是 `api-spec.md` + 代码为准。

## 代码/文档冲突项
- `mediaIds` vs `mediaList`。
- `FileUploadResultVO.id` 文档存在但代码不存在。
- `FoodRecommendQuery.sourceNodeId` 文档存在但代码使用 `facilityId`。
- `Facility nearby` 响应对象文档与代码不同。
- Route 历史、Diary 评分、我的日记、Facility 搜索、AI 接口文档有但代码入口未找到。

## 建议下一步动作
- 先由后端/文档负责人清理 Swagger 与代码 DTO/VO 的冲突。
- 由前端负责人补 `frontend-runtime-facts.md`，再冻结 baseURL、proxy 和 token 处理方式。
- 联调排期中只纳入“代码已实现 + 权限明确 + 字段已冻结”的接口。
