# 接口契约待确认问题（Open Questions）

## 1. 文件用途
本文件集中记录当前无法仅凭后端代码、现有 docs 和 `frontend-runtime-facts.md` 确认的问题。当前仓库未找到 `frontend-runtime-facts.md`，因此前端运行时事实相关问题优先级较高。

## 2. 待确认问题清单

| 优先级 | 问题描述 | 涉及接口/模块 | 当前已发现证据 | 为什么无法确认 | 建议找谁确认 |
|---|---|---|---|---|---|
| 高 | `frontend-runtime-facts.md` 未找到 | 前后端联调 | 仓库扫描无结果 | 缺少前端运行时事实来源 | 前端负责人 |
| 高 | 最终 API baseURL / dev proxy 未确认 | 全部接口 | 后端端口 8080、接口前缀 `/api/v1` 已确认 | 无前端事实文件，不能判断是否走代理 | 前端负责人 |
| 高 | token 存储策略未确认 | Auth/全局请求 | 后端只要求 `Authorization: Bearer <token>` | 无法确认 localStorage/sessionStorage/Pinia/内存 | 前端负责人 |
| 高 | request interceptor 统一位置未确认 | 全部受保护接口 | 后端鉴权方式明确 | 无前端工程代码和运行时事实 | 前端负责人 |
| 高 | Route 历史接口文档有、代码没找到 | Route | `api-spec.md` 定义 `/api/v1/routes/history` 和 `/{id}`；`RouteController` 只有 plan single/multi | 代码未实现入口 | 后端负责人 / 文档负责人 |
| 高 | Diary 评分接口文档有、代码没找到 | Diary | `api-spec.md`、`swagger-draft.yaml` 定义 `/api/v1/diaries/{id}/ratings`；`DiaryController` 未实现 | 代码未实现入口 | 后端负责人 / 文档负责人 |
| 高 | `GET /api/v1/diaries/me` 文档有、代码没找到 | Diary | `api-spec.md` 标“建议补充”，Swagger 已列出；Controller 未实现 | 不确定是否进入当前 P1 | 后端负责人 / 文档负责人 |
| 高 | `GET /api/v1/facilities/search` 文档有、代码没找到 | Facility | `api-spec.md`、`swagger-draft.yaml` 定义；`FacilityController` 只有 `/nearby` | 不确定是否改文档或补接口 | 后端负责人 / 文档负责人 |
| 高 | Swagger 的 Diary 发布请求字段与代码不一致 | Diary | `swagger-draft.yaml` 使用 `mediaIds`；`DiaryCreateRequest` 使用 `mediaList` | 文档未同步代码 | 文档负责人 / 后端负责人 |
| 高 | Swagger 的文件上传响应字段与代码不一致 | File | `swagger-draft.yaml` 的 `FileUploadResultVO` 含 `id`；代码 `FileUploadResultVO` 无 `id` | 文档未同步代码 | 文档负责人 / 后端负责人 |
| 中 | Food 推荐 query 字段口径不一致 | Food | `api-spec.md` 提到 `sourceNodeId`；代码 `FoodRecommendQuery` 使用 `facilityId` | 不确定后续是否做距离联动 | 后端负责人 / 文档负责人 |
| 中 | Facility nearby 响应字段口径不一致 | Facility | 文档写 `Page<FacilityVO>`；代码返回 `NearbyFacilityVO`，含 `reachableDistance` | 字段命名需统一 | 后端负责人 / 文档负责人 |
| 中 | Swagger 运行时访问方式未确认 | API 文档 | 有 `swagger-draft.yaml`；后端未找到 springdoc 配置 | 不知道是否需要 `/swagger-ui` | 后端负责人 |
| 中 | prod/test profile 是否需要补齐 | Runtime | 只有 `application.yml` 和 `application-dev.yml` | 无 test/prod 配置文件 | 后端负责人 |
| 中 | AI 接口是否只是文档预留 | AI | `api-spec.md` 定义 P2 AI；后端未找到 AI Controller | 当前不能按已实现演示 | 项目负责人 / 后端负责人 |
| 高 | OpenAI-compatible 实际厂商兼容性 | AI Animation | 已实现多模态 Chat Completions、Base64 图片和 `response_format=json_object` | 尚未使用真实厂商密钥验证模型名称、请求格式、额度和响应稳定性 | 后端负责人 / 项目负责人 |
| 中 | Admin 新增/修改返回对象与 Swagger 旧口径不一致 | Admin | Controller 返回 VO；Swagger 部分旧定义可能是 id/boolean | 需要统一最终契约 | 文档负责人 |
| 中 | 前端如何展示 mixed 路线中的交通方式切换 | Route | 后端已在 `pathEdges[].transportType` 返回每条边实际工具 | 当前无完整前端工程，无法确认地图分段样式和换乘提示 | 前端负责人 |
| 中 | 日记检索是否后续升级 FULLTEXT/倒排索引 | Diary/Search | `progress.md` 说明当前 LIKE，未实现 FULLTEXT | 当前语义可用，但性能边界需说明 | 后端负责人 |
| 低 | 上传文件访问域名是否需要拼接后端 origin | File | 返回 `fileUrl` 为 `/files/...` 相对路径 | 前端展示时是否需要补 origin 未确认 | 前端负责人 |
| 低 | CORS 允许源是否覆盖实际前端端口 | Runtime | 当前仅允许 `5173` 两种 localhost | 前端实际端口未知 | 前端负责人 / 后端负责人 |

## 3. 必须特别记录的问题类型覆盖

| 类型 | 当前记录 |
|---|---|
| 文档有、代码没找到的接口 | Route 历史、Facility 搜索、我的日记、AI 草稿/图片摘要/路线回顾等预留接口；日记评分和照片动画已有代码 |
| token 返回位置不清楚 | 后端已确认在 `data.token`；前端保存位置待确认 |
| 鉴权方式不清楚 | 后端已确认 JWT Bearer；前端注入方式待确认 |
| 返回体不统一 | 大多数接口统一 `ApiResponse`；文档/Swagger 个别响应对象旧口径需同步 |
| DTO/VO 字段语义不清 | `sourceNodeId/facilityId`、`reachableDistance/distance`、`mediaIds/mediaList` |
| 环境配置不完整 | 缺少前端运行时事实、test/prod profile、测试服务器地址 |
| 路由冲突 | 暂未发现后端 Controller 路由冲突；`/api/v1/destinations/{id}/diaries` 由 DiaryController 实现，需文档说明归属 |
| Mock / 假数据 / TODO / FIXME / Not Implemented | 动画默认仍为 `mock-template`，配置真实 Provider 后才调用多模态模型；其他历史状态以最新 progress 为准 |
| frontend-runtime-facts 与后端预期不一致 | 文件缺失，无法比对 |

## 待确认项
- 前端事实文件路径或是否需要新建。
- 当前接口冻结范围是否排除文档有但代码未实现的接口。
- Swagger 是否作为最终契约，还是 `api-spec.md` + 代码为准。

## 代码/文档冲突项
- `mediaIds` vs `mediaList`。
- `FileUploadResultVO.id` 文档存在但代码不存在。
- `FoodRecommendQuery.sourceNodeId` 文档存在但代码使用 `facilityId`。
- `Facility nearby` 响应对象文档与代码不同。
- Route 历史、我的日记、Facility 搜索、AI 草稿/图片摘要/路线回顾等预留接口文档有但代码入口未找到；日记照片动画已通过独立资源接口实现。

## 建议下一步动作
- 先由后端/文档负责人清理 Swagger 与代码 DTO/VO 的冲突。
- 由前端负责人补 `frontend-runtime-facts.md`，再冻结 baseURL、proxy 和 token 处理方式。
- 联调排期中只纳入“代码已实现 + 权限明确 + 字段已冻结”的接口。
