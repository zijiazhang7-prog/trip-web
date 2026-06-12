- 作用：一条条写测试用例。

# 测试用例（Test Cases）

## 1. 文件用途
本文件用于记录个性化旅游系统项目的测试用例，作为测试执行、缺陷记录、回归验证和测试报告编写的直接依据。

本文件关注的是：
- 具体测什么
- 用什么输入测
- 预期结果是什么
- 当前结果是否通过
- 哪些用例属于当前阶段必须优先执行

本文件应与以下文档配套使用：
- `test-plan.md`
- `bug-log.md`
- `test-report.md`

---

## 2. 当前版本说明

### 2.1 当前定位
当前版本为**当前阶段执行版测试用例集**，重点覆盖：
- P0 主线功能
- 当前接口和页面联调
- 核心边界与异常场景
- 核心安全基础场景

### 2.2 使用原则
1. 先执行 P0 用例，再执行 P1 / P2。
2. 当前仍未实现的功能，可先将执行结果标记为 `未执行`。
3. 发现缺陷后，应在 `bug-log.md` 中登记，并同步更新本文件中的“实际结果 / 状态 / 备注”。
4. 修复缺陷后，应执行对应回归用例。

### 2.3 当前前端联调状态
截至 2026-05-07，前端工程代码暂不可用，因此本文件中已通过的 P0 Diary 相关记录和 P0 主线后端接口演示预检仅代表后端实库接口联调结果，不代表页面联调通过。涉及页面表单、上传组件、路由跳转、前端状态管理的测试需要在前端工程恢复后单独执行。

---

## 3. 用例编号规则

建议采用如下编号格式：

- Auth 模块：`TC-AUTH-001`
- UserPreference 模块：`TC-PREF-001`
- Recommend 模块：`TC-REC-001`
- Route 模块：`TC-ROUTE-001`
- Facility 模块：`TC-FAC-001`
- Food 模块：`TC-FOOD-001`
- Diary 模块：`TC-DIARY-001`
- Admin 模块：`TC-ADMIN-001`
- 文件上传：`TC-FILE-001`
- 数据导入：`TC-IMPORT-001`
- AI 模块：`TC-AI-001`
- 安全与异常：`TC-SEC-001`

---

## 4. 优先级说明

- `P0`：当前必须优先执行，直接影响主线演示
- `P1`：基础闭环跑通后执行
- `P2`：增强或创新功能测试

---

## 5. 执行状态说明

- `未执行`
- `通过`
- `失败`
- `阻塞`
- `部分通过`

---

## 6. 测试用例模板

| 用例编号 | 模块 | 优先级 | 测试目标 | 前置条件 | 测试步骤 | 测试数据 | 预期结果 | 实际结果 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|---|---|---|

---

## 7. P0 核心主线测试用例

## 7.1 Auth 模块

| 用例编号 | 模块 | 优先级 | 测试目标 | 前置条件 | 测试步骤 | 测试数据 | 预期结果 | 实际结果 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-AUTH-001 | Auth | P0 | 正常注册用户 | 系统可访问注册接口 | 1. 打开注册页 2. 输入用户名/密码/昵称 3. 提交注册 | username=test_user_01, password=123456, nickname=测试用户1 | 注册成功，返回成功提示或 userId | 待填写 | 未执行 | |
| TC-AUTH-002 | Auth | P0 | 重复用户名拦截 | 已存在用户 `test_user_01` | 1. 再次使用相同用户名注册 2. 提交 | username=test_user_01 | 返回“用户名已存在”类错误提示 | 待填写 | 未执行 | |
| TC-AUTH-003 | Auth | P0 | 正常登录 | 测试用户已注册 | 1. 打开登录页 2. 输入用户名密码 3. 提交 | username=p0diarytest / auth_user_20260506220129, password=测试口令 | 登录成功，返回 token 和当前用户信息 | 登录成功，返回 `SUCCESS` 和 JWT token；2026-05-06 追加临时普通用户实库认证验证通过 | 通过 | 2026-05-05 后端实库联调；2026-05-06 后端实库认证专项验证 |
| TC-AUTH-004 | Auth | P0 | 错误密码登录失败 | 用户存在 | 1. 输入错误密码 2. 提交登录 | username=test_user_01, password=wrong123 | 返回密码错误提示，不签发 token | 待填写 | 未执行 | |
| TC-AUTH-005 | Auth | P0 | 获取当前用户信息 | 已登录并持有有效 token | 1. 携带 token 请求 `/auth/me` | 合法 Bearer token | 正确返回当前用户 id、username、nickname、role | 返回当前用户信息；2026-05-06 临时普通用户返回 `username=auth_user_20260506220129`、`role=user` | 通过 | 2026-05-05 后端实库联调；2026-05-06 后端实库认证专项验证 |
| TC-AUTH-006 | Auth | P0 | 无 token 获取当前用户失败 | 无登录状态 | 1. 不带 token 请求 `/auth/me` | 无 | 返回未登录或 token 缺失提示 | HTTP 401，返回统一结构，错误码 `AUTH_003` | 通过 | 2026-05-06 后端实库认证专项验证 |
| TC-AUTH-007 | Auth | P0 | 伪造 token 获取当前用户失败 | 无有效登录状态 | 1. 携带伪造 Bearer token 请求 `/auth/me` | `Authorization: Bearer invalid.token.value` | 返回 token 无效提示，不返回用户信息 | HTTP 401，返回统一结构，错误码 `AUTH_004` | 通过 | 2026-05-06 后端实库认证专项验证 |
| TC-AUTH-008 | Auth | P0 | 错误认证头格式被拒绝 | 无有效登录状态 | 1. 携带非 Bearer 认证头请求 `/auth/me` | `Authorization: Token invalid` | 返回未登录或登录失效提示 | HTTP 401，返回统一结构，错误码 `AUTH_003` | 通过 | 2026-05-06 后端实库认证专项验证 |

---

## 7.2 UserPreference 模块

| 用例编号 | 模块 | 优先级 | 测试目标 | 前置条件 | 测试步骤 | 测试数据 | 预期结果 | 实际结果 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-PREF-001 | UserPreference | P0 | 获取当前用户偏好成功 | 用户已登录 | 1. 携带 token 请求 `/user-preferences/me` | 合法 Bearer token | 返回当前用户偏好对象 | 待填写 | 未执行 | |
| TC-PREF-002 | UserPreference | P0 | 保存偏好标签成功 | 用户已登录 | 1. 调用更新偏好接口 2. 保存基础偏好 | preferThemeList=["人文建筑型","自然景观型"] | 返回更新后的偏好对象 | 待填写 | 未执行 | |
| TC-PREF-003 | UserPreference | P0 | 保存自由偏好描述成功 | 用户已登录 | 1. 调用更新偏好接口 2. 填写自由偏好文本 | customPreferenceText=更喜欢安静、人少、适合拍照的地方 | 返回更新后的偏好对象，文本保存成功 | 待填写 | 未执行 | |
| TC-PREF-004 | UserPreference | P0 | 未登录获取偏好失败 | 无登录状态 | 1. 不带 token 请求 `/user-preferences/me` | 无 | 返回未登录提示 | 待填写 | 未执行 | |
| TC-PREF-005 | UserPreference | P0 | 非法偏好参数处理正确 | 用户已登录 | 1. 提交非法偏好参数 | preferThemeList=["@@@"] | 返回参数错误提示 | 待填写 | 未执行 | |

---

## 7.3 Recommend 模块

| 用例编号 | 模块 | 优先级 | 测试目标 | 前置条件 | 测试步骤 | 测试数据 | 预期结果 | 实际结果 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-REC-001 | Recommend | P0 | 默认推荐列表返回成功 | `destination` 表已有样例数据 | 1. 打开推荐页 2. 调用推荐接口 | 无特殊参数 | 返回推荐结果列表，列表不为空 | 后端实库接口预检通过：`GET /api/v1/destinations/recommend?sortBy=heat&pageNum=1&pageSize=10&topK=5` 返回 `SUCCESS`，包含本次临时目的地 | 通过 | 2026-05-07 P0 主线后端接口演示预检，未含前端页面 |
| TC-REC-002 | Recommend | P0 | 按热度排序正确 | 样例目的地热度值已知 | 1. 调用推荐接口 2. sortBy=heat | sortBy=heat | 结果按热度降序排列 | 待填写 | 未执行 | |
| TC-REC-003 | Recommend | P0 | 按评分排序正确 | 样例目的地评分值已知 | 1. 调用推荐接口 2. sortBy=rating | sortBy=rating | 结果按评分降序排列 | 待填写 | 未执行 | |
| TC-REC-004 | Recommend | P0 | 关键字搜索成功 | 存在名称中包含“北邮”的目的地 | 1. 调用搜索接口 2. 输入关键字 | keyword=北邮 | 返回匹配目的地，结果包含“北邮”相关项 | 后端实库接口预检通过：`GET /api/v1/destinations/search?keyword=<临时目的地名>&sortBy=rating&pageNum=1&pageSize=10` 返回 `SUCCESS`，结果命中本次临时目的地 | 通过 | 2026-05-07 使用 ASCII 临时目的地名验证，未含前端页面 |
| TC-REC-005 | Recommend | P0 | 类型筛选成功 | 数据中同时存在 scenic 和 campus | 1. 调用搜索/推荐接口 2. type=campus | type=campus | 返回结果均为校园类型 | 待填写 | 未执行 | |
| TC-REC-006 | Recommend | P0 | 无结果时正确返回空列表 | 当前数据中不存在相关关键字 | 1. 调用搜索接口 | keyword=不存在的地方abcxyz | 返回空列表，不报系统异常 | 待填写 | 未执行 | |
| TC-REC-007 | Recommend | P0 | 已设置偏好时推荐接口可正常返回 | 用户已登录，且已设置偏好 | 1. 设置偏好 2. 调用推荐接口 | preferThemeList=["人文建筑型"] | 推荐接口正常返回，不因偏好存在而报错 | 待填写 | 未执行 | |
| TC-REC-008 | Recommend / IndexEngine | P1 | 目的地名称索引启用与失效时结果一致 | MySQL 8 可连接，已准备 4 条唯一前缀目的地 | 1. 重建 `DESTINATION_NAME` 2. 请求精确、前缀及第 2 页 3. 失效索引后重复请求 4. 比较完整分页数据 | `sortBy=heat/rating`, `pageSize=2` | 两种状态的列表、顺序、分页字段完全一致 | `IndexEngineDatabaseIntegrationTests` 通过；精确查询、前缀第 1/2 页的 `data` 完全一致 | 通过 | 2026-06-07 MySQL 8 实库 HTTP 回归；临时数据已清理 |

---

## 7.4 Route 模块

| 用例编号 | 模块 | 优先级 | 测试目标 | 前置条件 | 测试步骤 | 测试数据 | 预期结果 | 实际结果 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-ROUTE-001 | Route | P0 | 单目标路径规划成功 | 目的地下已导入可用节点和边 | 1. 调用单目标路径接口 2. 输入起点和终点 | destinationId=1, startNodeId=101, targetNodeId=110 | 返回路径节点序列、总距离、预计时间 | 后端实库接口预检通过：临时有向图 `A -> B -> C`，`strategyType=shortest_distance` 返回总距离 `200.00`、预计时间 `3`，并返回 `historyId=7` | 通过 | 2026-05-07 P0 主线后端接口演示预检；临时路线历史已清理 |
| TC-ROUTE-002 | Route | P0 | 起点等于终点时处理正确 | 起点节点存在 | 1. 调用单目标路径接口 | startNodeId=101, targetNodeId=101 | 返回零距离或单点路径，不报错 | 待填写 | 未执行 | |
| TC-ROUTE-003 | Route | P0 | 不可达目标点返回合理提示 | 图中存在不可达节点 | 1. 调用单目标路径接口 | startNodeId=101, targetNodeId=999 | 返回“不可达”类提示，不返回错误堆栈 | 待填写 | 未执行 | |
| TC-ROUTE-004 | Route | P0 | 路径结果与图数据一致 | 已手工验证一组最短路径 | 1. 调用单目标路径接口 2. 对比人工期望结果 | 固定测试图数据 | 返回的路径长度与人工验证一致 | 待填写 | 未执行 | |
| TC-ROUTE-005 | Route | P0 | 非法节点输入处理正确 | 接口可调用 | 1. 输入不存在的 startNodeId | startNodeId=-1, targetNodeId=110 | 返回参数错误或节点不存在提示 | 待填写 | 未执行 | |
| TC-ROUTE-006 | Route | P0 | 路线历史查询成功 | 已成功产生至少一条路线历史 | 1. 请求 `/routes/history` | pageNum=1,pageSize=1 | 只返回当前用户路线历史摘要，按时间倒序分页 | 实库返回 `total=2`、`pages=2`、列表 1 条，首条为最新多目标历史；摘要包含目的地和起终点名称且不返回路径明细 | 通过 | 2026-06-12 Service 单元测试 + MySQL 8 HTTP 回归 |
| TC-ROUTE-007 | Route | P1 | 多目标路径规划成功 | 固定测试图数据可用，用户已登录 | 1. 调用 `/routes/plan/multi` 2. 输入多个目标点 | startNodeId=A, targetNodeIds=[B,C], returnToStart=false | 返回拼接后的路径节点、路径边和总距离 | 单元测试通过；实库接口验证通过，临时图数据下 `returnToStart=false` 返回成功，最终路径终点为 C；2026-06-03 GraphEngine 抽取后再次验证路径 `A -> B -> C`、总距离 `200.00`，`route_history` 写入成功 | 通过 | 2026-05-07 后端单元测试 + 实库接口验证；2026-06-03 GraphEngine 抽取后实库回归 |
| TC-ROUTE-008 | Route | P1 | 多目标返回起点处理正确 | 固定测试图数据存在返回边 | 1. 调用多目标接口 2. 设置 returnToStart=true | startNodeId=A, targetNodeIds=[B,C], returnToStart=true | 完成多目标访问后追加返回起点路径 | 单元测试通过；实库接口验证路径为 `A -> B -> C -> A`，总距离 `360.00`，`route_history` 写入并校验通过 | 通过 | 2026-05-07 后端单元测试 + 实库接口验证 |
| TC-ROUTE-009 | Route | P1 | 多目标重复目标被拦截 | 接口可调用 | 1. targetNodeIds 传重复节点 | targetNodeIds=[B,B] | 返回参数错误，不进入路径计算 | 单元测试通过；实库接口返回 HTTP 400，错误码 `COMMON_001` | 通过 | 2026-05-07 后端单元测试 + 实库接口验证 |
| TC-ROUTE-010 | Route | P1 | 多目标目标数量超限被拦截 | 接口可调用 | 1. targetNodeIds 超过 8 个 | targetNodeIds=[2,3,4,5,6,7,8,9,10] | 返回参数错误，不进入路径计算 | `RouteServiceTests.planMultiRouteShouldRejectTooManyTargets` 通过 | 通过 | 2026-05-07 后端单元测试 |
| TC-ROUTE-011 | Route | P1 | 多目标不可达目标返回合理提示 | 图中存在不可达目标 | 1. 调用多目标路径规划 | startNodeId=A, targetNodeIds=[D]，D 无可达边 | 返回不可达类业务错误，不暴露堆栈 | 单元测试通过；实库接口返回 HTTP 422，错误码 `ROUTE_003` | 通过 | 2026-05-07 后端单元测试 + 实库接口验证 |
| TC-ROUTE-012 | Route | P1 | 单目标最短距离策略返回距离最优路径 | 固定测试图数据可用，用户已登录 | 1. 调用 `/routes/plan/single` 2. strategyType=shortest_distance | A->B=100、B->C=100、A->C=300 | 返回距离最短路径 `A -> B -> C`，总距离为 `200.00`，并写入路线历史 | 实库接口返回路径 `A -> B -> C`、总距离 `200.00`、`estimatedTime=3`；2026-06-03 GraphEngine 抽取后再次验证 `route_history.strategy_type=shortest_distance`、总距离和预计时间与接口一致 | 通过 | 2026-05-07 后端单元测试 + 实库接口验证；2026-06-03 GraphEngine 抽取后实库回归 |
| TC-ROUTE-013 | Route | P1 | 单目标最短时间策略返回时间最优路径 | 固定测试图数据可用，用户已登录 | 1. 调用 `/routes/plan/single` 2. strategyType=shortest_time | A->B->C 总时间 20，A->C 总时间 3 | 返回时间最短路径 `A -> C`，总距离可大于最短距离路径，并写入路线历史 | 实库接口返回路径 `A -> C`、总距离 `300.00`、`estimatedTime=3`；2026-06-03 GraphEngine 抽取后再次验证 `route_history.strategy_type=shortest_time`、总距离和预计时间与接口一致 | 通过 | 2026-05-07 后端单元测试 + 实库接口验证；2026-06-03 GraphEngine 抽取后实库回归 |
| TC-ROUTE-019 | Route | P1 | 路线历史详情恢复保存快照 | 当前用户存在单目标和多目标历史 | 1. 请求 `/routes/history/{id}` 2. 检查节点、边、交通方式和目标顺序 | 当前用户 historyId | 返回保存的 `pathNodes/pathEdges/orderedTargetNodeIds`，不重新规划 | 实库单目标详情恢复节点 `[2,1]`、目标顺序为空；多目标详情恢复节点 `[2,1,3]`、目标顺序 `[1,3]`；数据库新字段保存 `[1,3]` | 通过 | 2026-06-12 单元测试 + MySQL 8 HTTP 回归 |
| TC-ROUTE-020 | Route | P1 | 路线历史用户隔离 | 用户 A、B 各有历史 | 用户 B 请求用户 A 的 historyId | 他人 historyId | 返回 `COMMON_003`，不泄漏记录存在性 | 实库返回 HTTP 404、`COMMON_003`；无 token 请求列表返回 HTTP 401、`AUTH_003` | 通过 | 2026-06-12 Service、MockMvc + MySQL 8 HTTP 回归 |
| TC-ROUTE-021 | Route | P1 | 历史查询不重新运行算法 | 已保存有效历史 | 查询列表和详情并监控 MapService 调用 | 任意有效 historyId | MapService/GraphEngine 调用次数不增加，route_history 行数不变 | 单元测试验证 MapService 无交互；实库生成 2 条历史后执行列表和详情 GET，查询前后当前用户历史数均为 2 | 通过 | 2026-06-12 单元测试 + MySQL 8 HTTP 回归 |

---

## 7.5 Facility 模块

| 用例编号 | 模块 | 优先级 | 测试目标 | 前置条件 | 测试步骤 | 测试数据 | 预期结果 | 实际结果 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-FAC-001 | Facility | P0 | 根据当前位置查询附近设施成功 | 已有设施、节点和边数据 | 1. 调用附近设施接口 2. 输入当前位置和设施类型 | destinationId=1, sourceNodeId=101, facilityType=toilet | 返回厕所类设施列表 | 2026-06-03 GraphEngine 抽取后实库回归通过：临时设施节点 D 可从 A 到达，`GET /api/v1/facilities/nearby` 返回临时 toilet 设施 | 通过 | 后端实库接口回归；临时数据已清理 |
| TC-FAC-002 | Facility | P0 | 按设施类型过滤正确 | 当前目的地下存在多类设施 | 1. 调用设施接口 2. type=library | facilityType=library | 返回结果均为图书馆类设施 | 待填写 | 未执行 | |
| TC-FAC-003 | Facility | P0 | 图上可达距离排序正确 | 已知两处设施的可达距离顺序 | 1. 调用设施接口 2. 对比结果顺序 | sourceNodeId=101, facilityType=shop | 返回结果按图上可达距离升序排列 | 2026-06-03 GraphEngine 抽取后实库回归通过：返回设施 `reachableDistance=40.00`，来源节点和目标节点与临时图一致 | 通过 | 核心验证点；本次为单设施可达距离核验，排序链路随 `RankService.sortByScore` 执行 |
| TC-FAC-004 | Facility | P0 | 无该类设施时返回空列表 | 当前目的地下无某类设施 | 1. 调用设施接口 | facilityType=medical_station | 返回空列表，不报系统异常 | 2026-06-03 GraphEngine 抽取后实库回归通过：同一目的地下查询 `medical_station` 返回空列表，接口仍为 `SUCCESS` | 通过 | 后端实库接口回归 |
| TC-FAC-005 | Facility | P0 | 非法设施类型处理正确 | 接口可调用 | 1. 输入非法 facilityType | facilityType=@@@ | 返回参数错误提示 | 待填写 | 未执行 | |

---

## 7.6 Diary 模块

| 用例编号 | 模块 | 优先级 | 测试目标 | 前置条件 | 测试步骤 | 测试数据 | 预期结果 | 实际结果 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-DIARY-001 | Diary | P0 | 正常发布图文日记 | 用户已登录，上传接口可用 | 1. 上传图片 2. 调用发布日记接口 | title=P0联调日记二次验证, destinationId=1, visibility=public, mediaList.fileUrl=/files/diary/... | 发布成功，返回日记 ID | 发布成功，返回 `diaryId=2`；2026-05-07 P0 主线后端接口预检再次发布临时公开图文日记成功，返回 `diaryId=3`，媒体 1 条 | 通过 | 2026-05-05 后端实库联调；2026-05-07 临时日记已清理 |
| TC-DIARY-002 | Diary | P0 | 发布私有日记成功 | 用户已登录 | 1. 调用发布接口 2. visibility=private | title=仅自己可见, visibility=private | 发布成功，返回日记 ID | 待填写 | 未执行 | |
| TC-DIARY-003 | Diary | P0 | 日记关联路线记录成功 | 用户已存在 routeHistoryId | 1. 调用发布日记接口 | routeHistoryId=9001 | 发布成功，日记与路线记录正确关联 | 待填写 | 未执行 | |
| TC-DIARY-004 | Diary | P0 | 无标题发布失败 | 用户已登录 | 1. 调用发布接口，不填写 title | title="", contentText=测试内容 | 返回标题必填或参数错误提示 | 待填写 | 未执行 | |
| TC-DIARY-005 | Diary | P0 | 浏览日记列表成功 | 系统中已有样例日记 | 1. 调用日记列表接口 | pageNum=1,pageSize=10, sortBy=latest | 返回分页日记列表 | 列表返回成功，包含 `diaryId=2`；2026-05-07 临时目的地筛选列表包含本次临时 `diaryId=3` | 通过 | 2026-05-05 后端实库联调；2026-05-07 P0 主线后端接口预检 |
| TC-DIARY-006 | Diary | P0 | 查看日记详情成功 | 已有有效日记 ID | 1. 调用详情接口 | diaryId=2 | 返回完整日记内容和媒体列表 | 详情返回成功，标题与媒体列表正确，媒体数量为 1；2026-05-07 临时日记详情返回成功且媒体数量为 1 | 通过 | 2026-05-05 后端实库联调；2026-05-07 P0 主线后端接口预检 |
| TC-DIARY-007 | Diary | P0 | 按目的地查看日记成功 | 某目的地下已有样例日记 | 1. 调用按目的地查看接口 | destinationId=1, pageNum=1,pageSize=10, sortBy=latest | 返回该目的地下的日记列表 | 目的地日记列表返回成功，包含 `diaryId=2`；2026-05-07 临时目的地相关日记接口包含本次临时 `diaryId=3` | 通过 | 2026-05-05 后端实库联调；2026-05-07 P0 主线后端接口预检 |
| TC-DIARY-008 | Diary | P0 | 获取我的日记列表成功 | 用户已登录并发布过日记 | 1. 调用 `/diaries/me` | pageNum=1,pageSize=10 | 返回当前用户的日记分页列表 | 待填写 | 未执行 | |
| TC-DIARY-009 | Diary | P1 | 日记评分成功 | 用户已登录，存在公开启用日记 | 1. 调用评分接口 | diaryId=1, score=5 | 返回评分成功，平均分和评分人数更新 | 单元及 MySQL 实库测试通过 | 通过 | 2026-06-10 |
| TC-DIARY-010 | Diary | P1 | 同一用户重复评分处理正确 | 已对 diaryId=1 评分 | 1. 再次调用评分接口 | diaryId=1, score=4 | 更新原评分且明细数不增加 | 实库验证重复评分由 5 更新为 3，用户明细仍为 1 条 | 通过 | 2026-06-10 |
| TC-DIARY-011 | Diary | P1 | 标题查询成功 | 已实现标题查询接口 | 1. 调用标题查询接口 | title=校园 | 返回匹配标题的公开日记分页 | `SearchServiceTests` 覆盖标题查询、空标题、超长标题和分页上限；`DiaryServiceTests` 覆盖 VO 组装 | 通过 | 2026-05-06 后端单元测试 |
| TC-DIARY-012 | Diary | P1 | 关键词检索成功 | 已实现全文检索接口 | 1. 调用检索接口 | keyword=图书馆, destinationId=101 | 返回正文匹配的公开日记分页 | `SearchServiceTests` 覆盖正文关键词、目的地过滤、空关键词、超长关键词和非法目的地 | 通过 | 2026-05-06 后端单元测试 |
| TC-DIARY-013 | Diary | P1 | 检索结果排序字段校验 | 已实现标题 / 正文检索接口 | 1. 调用标题检索 sortBy=heat 2. 调用正文检索 sortBy=rating 3. 调用非法 sortBy | title=校园, keyword=图书馆, sortBy=heat/rating/unknown | 合法排序返回分页，非法排序返回参数错误 | `SearchServiceTests` 覆盖标题热度排序、正文评分排序和非法排序字段；`mvn test` 157 个测试通过 | 通过 | 2026-05-07 后端单元测试 |
| TC-DIARY-014 | Diary / IndexEngine | P1 | 标题索引启用与失效时检索语义一致 | MySQL 8 可连接，已准备精确、前缀和非前缀包含标题 | 1. 重建 `DIARY_TITLE` 2. 请求精确、前缀分页、非前缀包含 3. 失效索引后重复请求 4. 比较完整分页数据 | `sortBy=latest/heat/rating`, `pageSize=2` | 两种状态结果、顺序和分页完全一致；非前缀包含仍由 LIKE 召回 | `IndexEngineDatabaseIntegrationTests` 通过；非前缀标题命中 1 条，索引失效后结果不变 | 通过 | 2026-06-07 MySQL 8 实库 HTTP 回归；临时数据已清理 |
| TC-DIARY-015 | Diary / IndexEngine | P1 | 正文倒排索引与 LIKE 兜底语义一致 | MySQL 8 可连接，已准备中文、英文、跨目的地正文 | 1. 重建 `DIARY_CONTENT` 2. 验证中文连续子串、英文大小写、目的地过滤和分页 3. 验证索引 MISS 空页 4. 失效索引后重复请求 | `keyword`, `destinationId`, `sortBy=latest/heat/rating` | HIT 使用候选 ID；MISS 返回空页；UNAVAILABLE 使用 LIKE；两种可用路径的响应一致 | 实库 HTTP 回归通过；索引 SQL 使用 `id IN`，失效后使用 `content_text LIKE`，完整分页对象一致 | 通过 | 2026-06-07；临时日记、美食、目的地和用户已清理 |
| TC-DIARY-016 | Diary / IndexEngine | P1 | 正文索引单文档新增、替换和删除 | 已构建 `DIARY_CONTENT` | 1. upsert 新文档 2. 同 ID 替换正文 3. remove 文档 4. 查询新旧关键词 | 中文、英文大小写、Emoji | 新增后 HIT；替换后旧词 MISS、新词 HIT；删除后 MISS；Unicode 位置正确 | `IndexEngineTests` 通过 | 通过 | 2026-06-07 单元测试 |
| TC-DIARY-017 | Diary / IndexEngine | P1 | 增量维护遵守事务提交边界 | 已构建 `DIARY_CONTENT` | 1. 事务内注册更新 2. 提交前查询 3. 触发提交 4. 模拟回滚 | 同 ID 新旧正文 | 提交前旧快照不变；提交后新正文生效；回滚不修改索引 | `IndexMaintenanceServiceTests` 通过 | 通过 | 2026-06-07 单元测试 |
| TC-DIARY-018 | Diary / IndexEngine | P1 | 日记可见性和状态驱动正文索引维护 | 日记写服务可用 | 1. 发布公开日记 2. 发布私有日记 3. 管理员禁用 4. 管理员重新启用公开日记 | `visibility=public/private`, `status=0/1` | 公开启用执行 upsert；私有或禁用执行 remove；标题在提交后失效 | `DiaryServiceTests`、`AdminServiceTests` 通过 | 通过 | 2026-06-07 单元测试 |
| TC-DIARY-019 | Diary / CompressionEngine | P1 | Huffman 压缩可无损还原多语言正文 | CompressionEngine 可用 | 1. 压缩正文 2. 解压二进制包 3. 比较原文 | ASCII、中文、Emoji、辅助平面 Unicode | 解压结果逐码点等于原文，长度和 CRC32 校验通过 | `CompressionEngineTests` 通过 | 通过 | 2026-06-07 单元测试 |
| TC-DIARY-020 | Diary / CompressionEngine | P1 | Huffman 边界和损坏数据处理正确 | CompressionEngine 可用 | 1. 测试空文本和单字符文本 2. 测试 10000 字正文 3. 修改 magic/version/CRC 或截断数据 | 空文本、重复字符、最大日记长度、损坏压缩包 | 合法数据可还原；损坏数据被拒绝；重复压缩结果稳定 | `CompressionEngineTests` 通过 | 通过 | 2026-06-07 单元测试 |
| TC-DIARY-021 | Diary / CompressionEngine | P1 | 日记发布保存压缩副本且压缩故障可降级 | 用户、目的地和发布服务可用 | 1. 发布正常日记 2. 捕获入库实体 3. 解压 `contentCompressed` 4. 模拟压缩异常后再次发布 | `contentText=今天去了图书馆。` | 正常时原文与压缩包同时保存；异常时原文仍保存、压缩字段为空 | `DiaryServiceTests` 通过 | 通过 | 2026-06-07 单元测试 |
| TC-DIARY-022 | Diary / CompressionEngine | P1 | 历史日记按主键游标分批回填 | 存在正文非空且压缩字段为空的历史日记 | 1. 配置批大小 2. 执行维护服务 3. 检查游标和条件更新 | 两批历史日记 | 不遗漏、不重复；仅更新压缩字段为空且正文未变化的数据 | 单元测试通过；MySQL 8 以批大小 2 扫描 3 条并回填 3 条，失败 0 | 通过 | 2026-06-07 单元测试及实库验证 |
| TC-DIARY-023 | Diary / CompressionEngine | P1 | 已有压缩包完整性校验与修复 | 开启 `verify-existing` | 1. 准备正常、损坏、内容不一致压缩包 2. 执行维护 | 正常包、非法字节、旧正文压缩包 | 正常包只统计；损坏和不一致数据从原文重新压缩 | 单元测试覆盖修复分支；实库二次扫描 3 条，校验通过 3 条、修复 0、失败 0 | 通过 | 2026-06-07 单元测试及实库验证 |
| TC-DIARY-024 | Diary / CompressionEngine | P1 | 单条维护失败不阻塞后续记录 | 批次内同时存在异常和正常日记 | 1. 模拟首条压缩异常 2. 执行维护 3. 检查后续更新和统计 | 失败正文、正常正文 | 失败计数增加；后续记录仍成功回填；日志不记录正文 | `CompressionMaintenanceServiceTests` 通过 | 通过 | 2026-06-07 单元测试 |
| TC-DIARY-025 | Diary / CompressionEngine | P1 | 压缩维护后全量回归 | MySQL 8 可连接，默认未开启启动回填 | 1. 执行 `mvn -q test` 2. 汇总 Surefire 报告 | 全部后端测试 | 原有接口、索引和日记行为不变 | 25 个测试套件、198 个测试，0 失败、0 错误、0 跳过 | 通过 | 2026-06-07 后端全量测试 |
| TC-DIARY-026 | Diary | P1 | 成功查看详情后浏览量原子增加 | 存在公开启用日记，初始 `heat_score=2` | 1. 连续查看详情两次 2. 并发执行 8 次原子自增 3. 查询数据库 | diaryId=临时日记 | 两次详情分别返回 3、4；并发后数据库值为 12，不丢失计数 | `DiaryServiceTests` 与 `DiaryHeatDatabaseIntegrationTests` 通过 | 通过 | 2026-06-12 单元及 MySQL 实库测试 |
| TC-DIARY-027 | Diary | P1 | 无效访问不增加浏览量 | 存在禁用或其他用户私有日记 | 1. 请求禁用日记 2. 请求无权访问的私有日记 3. 检查 Mapper 调用 | status=0 / visibility=private | 请求失败，且不执行 `incrementHeatScore` | `DiaryServiceTests` 通过 | 通过 | 2026-06-12 单元测试 |
| TC-DIARY-028 | Diary | P1 | 热度排序使用实时浏览量 | 同一目的地下两篇公开日记，初始热度分别为 2、5 | 1. 查看第一篇两次 2. 再并发增加 8 次 3. 调用 `sortBy=heat` | destinationId=临时目的地 | 第一篇实时热度变为 12，并排在热度 5 的日记之前 | `DiaryHeatDatabaseIntegrationTests` 通过，临时数据已清理 | 通过 | 2026-06-12 MySQL 实库测试 |

---

## 7.7 文件上传与资源访问

| 用例编号 | 模块 | 优先级 | 测试目标 | 前置条件 | 测试步骤 | 测试数据 | 预期结果 | 实际结果 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-FILE-001 | FileService | P0 | 正常上传图片成功 | 上传接口可访问 | 1. 选择合法图片文件 2. 上传 | png, 小于限制大小, bizType=diary | 返回文件 URL 或文件 ID | 上传成功，返回 `/files/diary/20260505/5a4e5b3d515a4ddaa22e2377e66bca77.png`；2026-05-07 P0 主线预检再次上传最小 png，返回 `/files/diary/20260507/...` | 通过 | 2026-05-05 后端实库联调；2026-05-07 P0 主线后端接口预检 |
| TC-FILE-002 | FileService | P0 | 非法文件类型上传失败 | 上传接口可访问 | 1. 上传非法类型文件 | test.exe / test.js | 返回文件类型不允许提示 | 待填写 | 未执行 | 安全关键 |
| TC-FILE-003 | FileService | P0 | 超大文件上传失败 | 上传接口可访问 | 1. 上传超出大小限制图片 | 过大文件 | 返回文件过大提示 | 待填写 | 未执行 | 安全关键 |
| TC-FILE-004 | FileService | P0 | 上传后资源可访问 | 已成功上传图片 | 1. 打开返回的文件 URL | `/files/diary/20260505/5a4e5b3d515a4ddaa22e2377e66bca77.png` | 文件可正常访问或预览 | HTTP 状态为 200 | 通过 | 关联回归：BUG-001 |

---

## 7.8 Food 模块（P1）

| 用例编号 | 模块 | 优先级 | 测试目标 | 前置条件 | 测试步骤 | 测试数据 | 预期结果 | 实际结果 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-FOOD-001 | Food | P1 | 按目的地查询美食成功 | `food` 表已有样例数据 | 1. 调用美食列表接口 2. 传 destinationId | destinationId=1 | 返回美食列表 | `GET /foods/recommend?destinationId=1&sortBy=heat&topK=2` 返回 `SUCCESS`，按热度返回牛肉面、鸡腿饭 | 通过 | 2026-05-05 后端实库接口验证 |
| TC-FOOD-002 | Food | P1 | 按菜系过滤正确 | 存在多种 foodType | 1. 调用美食接口 2. foodType=川菜 | destinationId=1, foodType=面食 | 返回结果均为指定菜系 | `GET /foods/recommend?destinationId=1&foodType=面食&sortBy=rating&topK=5` 返回 `SUCCESS`，仅返回面食测试数据 | 通过 | 2026-05-05 后端实库接口验证 |
| TC-FOOD-003 | Food | P1 | 按评分排序正确 | 样例美食评分值已知 | 1. 调用美食接口 2. sortBy=rating | sortBy=rating | 结果按评分降序排列 | 面食评分排序返回番茄面、牛肉面，顺序符合评分值 | 通过 | 2026-05-05 后端实库接口验证 |
| TC-FOOD-004 | Food | P1 | 美食名称模糊查询成功 | 存在名称中含“面”的数据 | 1. 调用接口 2. keyword=面 | keyword=面 | 返回名称或描述匹配结果 | `GET /foods/search?destinationId=1&keyword=面&sortBy=rating` 返回 `SUCCESS`，包含两条面食测试数据 | 通过 | 2026-05-05 后端实库接口验证 |
| TC-FOOD-005 | Food / IndexEngine | P1 | 美食名称和店铺名索引启用与失效时结果一致 | MySQL 8 可连接，同一目的地下已准备 4 条美食 | 1. 重建名称和店铺名索引 2. 请求名称前缀第 1/2 页和店铺名精确查询 3. 失效索引后重复请求 4. 比较完整分页数据 | `sortBy=heat/rating`, `pageSize=2` | 两种状态的列表、顺序、分页字段完全一致 | `IndexEngineDatabaseIntegrationTests` 通过；名称分页和店铺名精确查询的 `data` 完全一致 | 通过 | 2026-06-07 MySQL 8 实库 HTTP 回归；临时数据已清理 |

说明：Food 基础版进入开发后，优先以后端接口和 Service 测试执行 `TC-FOOD-001 ~ TC-FOOD-004`；前端美食页联调不在当前可执行范围内。

---

## 7.9 Admin 与导入流程（P1）

| 用例编号 | 模块 | 优先级 | 测试目标 | 前置条件 | 测试步骤 | 测试数据 | 预期结果 | 实际结果 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-ADMIN-001 | Admin | P1 | 管理员访问后台成功 | 管理员账号已存在 | 1. 管理员登录 2. 进入后台页面 | admin 账号 | 成功进入后台 | 管理员登录返回 `SUCCESS`，可调用 `/api/v1/admin/**` 后端接口 | 通过 | 2026-05-05 后端实库接口验证，未含前端页面 |
| TC-ADMIN-002 | Admin | P1 | 普通用户访问后台失败 | 普通用户已登录 | 1. 普通用户访问后台接口或页面 | 普通用户 token | 返回权限不足或跳转拦截 | 普通用户请求 `/api/v1/admin/destinations` 返回 HTTP 403 | 通过 | 2026-05-05 后端实库接口验证 |
| TC-ADMIN-003 | Admin | P1 | 目的地新增成功 | 管理员已登录 | 1. 调用新增目的地接口 | 合法 destination 表单 | 新增成功，列表可查到新记录 | 管理员调用 `POST /api/v1/admin/destinations` 返回 `SUCCESS` 和目的地 ID | 通过 | 2026-05-05 后端实库接口验证 |
| TC-ADMIN-004 | Admin | P1 | 场所新增成功 | 管理员已登录，目的地已存在 | 1. 调用新增场所接口 2. 按关键字查询 3. 修改场所 4. 删除无引用场所 | 合法 place 表单 | 新增成功，列表可查到新记录，修改后字段生效，无引用场所可删除 | 实库接口验证通过：管理员 token 可调用 `GET/POST/PUT/DELETE /api/v1/admin/places`；无 token 返回 401，普通用户返回 403；ASCII 测试名查询返回 total=1，修改后名称匹配，删除后数据库剩余 0 行 | 通过 | 2026-05-06 后端实库接口验证；中文关键字由 PowerShell 验证脚本编码导致查询失败，已用 ASCII 名称复测通过，不判定为接口缺陷 |
| TC-ADMIN-005 | Admin | P1 | 设施新增成功 | 管理员已登录，目的地已存在 | 1. 调用新增设施接口 | 合法 facility 表单 | 新增成功，列表可查到新记录 | 管理员调用 `POST /api/v1/admin/facilities` 返回 `SUCCESS` 和设施 ID | 通过 | 2026-05-05 后端实库接口验证 |
| TC-ADMIN-006 | Admin | P1 | 美食新增成功 | 管理员已登录，目的地已存在 | 1. 调用新增美食接口 | 合法 food 表单 | 新增成功，列表可查到新记录 | 管理员调用 `POST /api/v1/admin/foods` 返回 `SUCCESS`，列表可按关键字查到新记录 | 通过 | 2026-05-05 后端实库接口验证 |
| TC-ADMIN-007 | Admin | P1 | 地图节点新增成功 | 管理员已登录，目的地已存在 | 1. 调用新增地图节点接口 | 合法 map_node 表单 | 新增成功，节点列表可查到新记录 | `AdminServiceTests` 覆盖地图节点新增并返回节点 ID | 通过 | 2026-05-06 后端单元测试 |
| TC-ADMIN-008 | Admin | P1 | 地图边新增成功 | 管理员已登录，相关节点已存在 | 1. 调用新增地图边接口 | 合法 map_edge 表单 | 新增成功，边列表可查到新记录 | `AdminServiceTests` 覆盖合法地图边新增；跨目的地节点返回 `ROUTE_009` | 通过 | 2026-05-06 后端单元测试 |
| TC-ADMIN-009 | Admin | P1 | 管理端分页查询日记成功 | 管理员已登录，系统中已有样例日记 | 1. 调用管理端日记列表接口 | pageNum=1,pageSize=10 | 返回分页日记列表 | 已实现 `GET /api/v1/admin/diaries`，当前以全量后端测试保证编译与路由装配 | 部分通过 | 待实库接口验证 |
| TC-ADMIN-010 | Admin | P1 | 用户状态修改成功 | 管理员已登录，用户存在 | 1. 调用用户状态接口 | status=0/1 | 用户状态更新成功 | `AdminServiceTests` 覆盖用户状态修改 | 通过 | 2026-05-06 后端单元测试 |
| TC-ADMIN-011 | Admin | P1 | 日记状态修改成功 | 管理员已登录，日记存在 | 1. 调用日记状态接口 | status=0/1 | 日记状态更新成功，前台隐藏下架日记 | `AdminServiceTests` 覆盖日记状态修改；DiaryService 已按 status 过滤 | 通过 | 2026-05-06 后端单元测试 |
| TC-ADMIN-012 | Admin | P1 | 场所删除引用保护正确 | 管理员已登录，场所存在 | 1. 删除无引用场所 2. 删除被设施或地图节点引用的场所 | placeId=存在 / 被引用 | 无引用时删除成功；存在引用时返回业务错误，不破坏设施或地图节点 | 实库接口验证通过：删除被 `facility.place_id` 引用的场所返回 HTTP 400、`COMMON_002`；删除被 `map_node(node_type=place, ref_id=id)` 引用的场所返回 HTTP 400、`COMMON_002`；临时设施、节点、场所和用户均已清理 | 通过 | 2026-05-06 后端实库接口验证 |
| TC-IMPORT-001 | Import | P1 | 批量导入基础数据成功 | 已准备标准化导入文件 | 1. 以 multipart/form-data 调用导入接口 2. 上传标准 CSV / JSON | `destinations-nobom.csv` | 返回成功条数，数据库写入成功，并记录导入批次 | 实库调用 `/api/v1/admin/import-batches/preview` 返回 `PREVIEW_ONLY` 且不写业务表 / 批次表；调用 `/api/v1/admin/import-batches` 返回 `SUCCESS`、成功 1 行、失败 0 行，`destination` 新增 `P1导入验证目的地无BOM20260506200514`，`import_batch.id=3` 记录成功批次 | 通过 | 2026-05-06 后端实库 multipart 验证 |
| TC-IMPORT-002 | Import | P1 | 非法导入文件被拦截 | 导入入口可访问 | 1. 上传非法格式文件或错误字段 CSV | 错误列结构 CSV / 不支持的 sourceType | 返回导入失败和原因说明 | `ImportServiceTests` 覆盖 unsupported sourceType、扩展名不匹配、unsupported targetTable、文件过大、缺必填字段预览警告 | 通过 | 2026-05-06 后端单元测试 |
| TC-IMPORT-003 | Import | P1 | 导入失败明细可记录 | 已准备含错误行的标准文件 | 1. 调用执行导入接口 2. 部分行字段缺失或外键不存在 | `destinations-bad.csv` | 返回 `PARTIAL_SUCCESS` 或 `FAILED`，写入 `import_failure` | 实库调用执行导入返回 `FAILED`、成功 0 行、失败 1 行；`import_batch.id=1/2` 记录失败批次，`import_failure.id=1/2` 记录第 2 行错误，错误摘要为“导入字段缺失或字段名不匹配” | 通过 | 2026-05-06 后端实库 multipart 验证 |
| TC-IMPORT-004 | Import | P1 | 带 UTF-8 BOM 的 CSV 表头可正常解析 | 导入入口可访问，ImportService 已支持 CSV 表头映射 | 1. 使用带 BOM 的 CSV 执行预览解析 | `\uFEFFname,type,city` | 不因首列表头 BOM 导致必填字段缺失 | `ImportServiceTests.previewImportShouldParseCsvHeaderWithUtf8Bom` 通过，预览总行数为 1，warnings 为空 | 通过 | 2026-05-06 后端单元回归测试 |
| TC-IMPORT-005 | Import | P1 | 管理端查询导入批次成功 | 已存在导入批次数据，管理员已登录 | 1. 调用 `/api/v1/admin/import-batches` 2. 按关键字、目标表或状态过滤 | keyword=destinations, type=destination, status=SUCCESS | 返回分页批次列表，包含批次状态、总行数、成功 / 失败行数 | `AdminServiceTests` 覆盖批次分页查询和 VO 映射 | 通过 | 2026-05-06 后端单元测试 |
| TC-IMPORT-006 | Import | P1 | 管理端查询导入失败明细成功 | 已存在失败批次，管理员已登录 | 1. 调用 `/api/v1/admin/import-batches/{batchId}/failures` 2. 查看失败行 | batchId=存在失败明细的批次 | 返回分页失败明细，包含行号、字段名、错误信息和原始行 JSON；batchId 不存在时返回资源不存在 | `AdminServiceTests` 覆盖失败明细分页查询和不存在批次返回 `COMMON_003` | 通过 | 2026-05-06 后端单元测试 |

---

## 7.10 安全与异常处理

| 用例编号 | 模块 | 优先级 | 测试目标 | 前置条件 | 测试步骤 | 测试数据 | 预期结果 | 实际结果 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-SEC-001 | Security | P0 | 未登录访问需登录接口被拦截 | 无 token | 1. 请求需要登录的接口，如保存偏好、发布日记、路线规划 | 无 token | 返回未登录提示 | 多目标路线规划实库验证中，不带 token 调用 `/api/v1/routes/plan/multi` 返回 HTTP 401；最短时间实库验证中，不带 token 调用 `/api/v1/routes/plan/single` 返回 HTTP 401、`AUTH_003` | 通过 | 2026-05-07 Route 多目标与最短时间实库接口验证 |
| TC-SEC-002 | Security | P0 | 无效 token 被拦截 | 无效 token | 1. 携带伪造 token 请求接口 | fake token | 返回 token 无效或未登录提示 | 待填写 | 未执行 | |
| TC-SEC-003 | Exception | P0 | 非法参数返回统一错误结构 | 接口可访问 | 1. 传非法参数请求推荐/设施/路线接口 | 非法 facilityType / 非法 nodeId | 返回统一错误码与消息，不暴露堆栈 | 待填写 | 未执行 | |
| TC-SEC-004 | Security | P0 | 管理端接口角色校验正确 | 普通用户 token 可用 | 1. 请求后台接口 | 普通用户 token | 返回权限不足 | 待填写 | 未执行 | |
| TC-SEC-005 | Validation | P0 | 核心输入字段空值校验正确 | 接口可访问 | 1. 提交空标题日记或空用户名注册 | title="", username="" | 返回明确参数校验错误 | 待填写 | 未执行 | |
| TC-SEC-006 | Validation | P0 | 偏好更新接口非法参数校验正确 | 用户已登录 | 1. 提交非法偏好字段 | preferThemeList=["@@@"] | 返回明确参数校验错误 | 待填写 | 未执行 | |

---

## 7.11 AI 模块（P2 占位）

| 用例编号 | 模块 | 优先级 | 测试目标 | 前置条件 | 测试步骤 | 测试数据 | 预期结果 | 实际结果 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-AI-001 | AI | P2 | AI 日记草稿生成成功 | AI 接口可访问，已有目的地或图片输入 | 1. 调用 AI 日记草稿接口 | destinationId=1, contentHint=今天去了图书馆 | 返回标题建议、正文草稿、标签或摘要 | 待填写 | 未执行 | P2 占位 |
| TC-AI-002 | AI | P2 | AI 图片摘要成功 | AI 接口可访问，已有图片输入 | 1. 调用图片摘要接口 | imageUrls=[...] | 返回图片摘要、标签、场景类型 | 待填写 | 未执行 | P2 占位 |
| TC-AI-003 | AI | P2 | AI 输入参数不完整时返回合理错误 | AI 接口可访问 | 1. 调用 AI 接口但缺少必要参数 | 空请求体或缺失关键字段 | 返回参数错误提示，不暴露底层异常 | 待填写 | 未执行 | P2 占位 |
| TC-AI-004 | AI | P2 | 外部 AI 服务失败时系统正确兜底 | AI 接口可访问 | 1. 模拟外部 AI 调用失败 | 模拟超时 / 服务不可用 | 返回“生成失败，请稍后重试”类提示，不拖垮主流程 | 待填写 | 未执行 | P2 占位 |

---

## 7.12 P0 主线后端接口演示预检

| 用例编号 | 模块 | 优先级 | 测试目标 | 前置条件 | 测试步骤 | 测试数据 | 预期结果 | 实际结果 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-E2E-001 | P0 主线后端接口 | P0 | 用同一个登录 token 串联验证登录、推荐、搜索、单目标路线、文件上传和日记发布 / 查看 | MySQL 8 `tour_system` 可连接，后端 jar 可启动，P0 表存在 | 1. 启动后端并健康检查 2. 注册并登录临时普通用户 3. 插入临时目的地、3 个地图节点、3 条有向边 4. 请求推荐与搜索 5. 请求单目标路线并核验 `route_history` 6. 上传 diary 图片 7. 发布公开日记 8. 验证日记列表、详情和目的地日记 9. 清理临时数据 | 临时用户、临时目的地、临时图 `A -> B -> C`，边权 `120 + 80 < 260`，最小 png 图片 | 全链路返回 `SUCCESS`；路线总距离为 `200.00`；日记媒体 1 条；临时数据清理为 0 | 通过：登录成功；推荐返回 `SUCCESS` 且命中临时目的地；搜索命中临时目的地；单目标路线返回 `historyId=7`、总距离 `200.00`、预计时间 `3`；上传返回 `/files/diary/20260507/...`；日记发布返回 `diaryId=3`，列表 / 详情 / 目的地日记均可查到；清理后剩余临时数据为 0 | 通过 | 2026-05-07 后端实库接口预检；未记录 JWT、数据库密码或真实敏感信息；不代表前端页面联调通过 |

---

## 8. P1 / P2 扩展测试用例占位

以下内容建议在功能实现后继续补充详细测试用例：

### 8.1 Route 增强
- 多目标路径规划（已完成后端基础版、单元测试和实库接口验证，待前端联调）
- 最短时间策略（已完成后端基础版、单元测试和单目标实库接口验证，待前端联调）
- 交通工具约束
- 室内导航示例

### 8.2 Diary 增强
- 全文检索
- 压缩存储（Huffman 编解码、发布写入、历史回填与校验修复已完成）
- 路线回顾页
- AI 生成草稿

### 8.3 Food 增强
- 价格区间过滤
- 按价格排序
- 与设施距离联动

### 8.4 Admin 增强
- 导入批次查看
- 日记状态管理
- 用户状态管理
- 地图节点和边的图形化维护
- 导入预校验页面

---

## 9. 回归测试建议用例

每次修复以下问题后，应至少执行对应回归测试：

| 回归编号 | 触发场景 | 建议回归范围 |
|---|---|---|
| REG-001 | 登录相关 bug 修复 | Auth 全部 P0 用例 |
| REG-002 | 偏好接口或偏好字段修改 | UserPreference 相关用例 + Recommend 相关用例 |
| REG-003 | 推荐排序逻辑修改 | Recommend 排序与搜索相关用例 |
| REG-004 | 图算法修改 | Route + Facility 相关用例 |
| REG-005 | 文件上传逻辑修改 | 上传与日记发布相关用例 |
| REG-006 | 日记表或媒体逻辑修改 | Diary 发布 / 列表 / 详情 / 评分相关用例 |
| REG-007 | 权限逻辑修改 | Auth + Admin + Security 相关用例 |
| REG-008 | IndexEngine、QueryService 或 SearchService 索引调度修改 | 目的地搜索 + 美食搜索 + 日记标题检索 + 日记正文全文检索 |

---

## 10. 当前阶段执行建议

建议当前先按下面顺序执行：

### 第一批（必须先测）
- TC-AUTH-001 ~ 006
- TC-PREF-001 ~ 005
- TC-REC-001 ~ 007
- TC-ROUTE-001 ~ 006
- TC-FAC-001 ~ 005
- TC-DIARY-001 ~ 008
- TC-FILE-001 ~ 004
- TC-SEC-001 ~ 006

当前执行限制：前端工程代码暂不可用，第一批用例中涉及“打开页面”“选择文件组件”“页面跳转”的步骤暂按后端接口方式替代验证，页面级结果后续补测。

### 第二批（基础版后补）
- TC-FOOD-001 ~ 004
- TC-DIARY-009 ~ 012
- TC-ADMIN-001 ~ 012
- TC-IMPORT-001 ~ 002

### 第三批（增强与创新）
- TC-AI-001 ~ 004

---

## 11. 执行记录建议

实际执行时，建议把每条用例至少补充以下信息：

- 实际返回结果摘要
- 是否通过
- 若失败，对应 bug 编号
- 测试执行日期
- 执行人

例如：

| 用例编号 | 实际结果 | 状态 | 对应 Bug | 执行日期 | 执行人 |
|---|---|---|---|---|---|
| TC-AUTH-001 | 注册成功，返回 userId=11 | 通过 |  | YYYY-MM-DD | 张三 |
| TC-ROUTE-003 | 返回 500，未给出不可达提示 | 失败 | BUG-012 | YYYY-MM-DD | 李四 |

---

## 12. 与其他文档的关系

本文件应与以下文档保持一致：

- `project-root/docs/06_testing/test-plan.md`
- `project-root/docs/06_testing/bug-log.md`
- `project-root/docs/06_testing/test-report.md`
- `project-root/docs/04_api/api-spec.md`
- `project-root/docs/05_modules/*`
- `project-root/docs/09_decisions/ADR-002-mvp-scope.md`
- `project-root/docs/09_decisions/ADR-003-module-split.md`
- `project-root/docs/00_project/progress.md`

如果当前优先测试范围、模块实现范围或接口行为发生变化，应同步更新本文件。

## 13. 后续维护说明

本文件应在以下场景下更新：

1. 新增核心功能时；
2. 某模块从未实现变为已实现时；
3. 接口路径、请求参数或返回字段发生变化时；
4. P0 / P1 / P2 用例优先级发生调整时；
5. UserPreference、RouteHistory、Diary 可见性等主线能力新增或修改时；
6. Food、Admin、Import、AI 等模块正式进入测试执行范围时；
7. 新增高优先级缺陷后需要补回归用例时；
8. 验收前需要收敛为最终执行版测试用例集时。

## 14. 演示数据导入前变更测试

| 用例编号 | 模块 | 优先级 | 测试目标 | 验收标准 | 当前状态 |
|---|---|---|---|---|---|
| TC-DATA-001 | Schema | P0 | 现有库重复执行迁移 | 新字段和评论表存在，重复执行不报重复列/表错误 | 通过：迁移连续执行两次成功 |
| TC-DATA-002 | Import | P0 | 新旧 Facility/Food 模板兼容 | 新字段可导入，旧模板缺少可选字段仍成功 | 通过：相关单测及全量回归通过 |
| TC-DATA-003 | Demo Data | P0 | 保留现有用户并幂等导入 | 不修改旧用户；演示用户、偏好、日记、评论无重复 | 待实库执行 |
| TC-DATA-004 | Compression | P0 | 导入日记压缩回填 | 正文非空日记压缩字段非空，解压等于原文，失败数为 0 | 待实库执行 |
| TC-DATA-005 | Index | P0 | 导入后索引重建 | 目的地、美食、日记标题和正文搜索命中新数据 | 待实库执行 |
| TC-DATA-006 | API | P1 | Facility/Food 新字段返回 | 管理端与用户端响应字段正确，原排序和分页不变 | 单元回归通过，待实库 HTTP 回归 |

## 15. 评论基础版测试

|用例编号|模块|优先级|测试目标|验收标准|当前状态|
|---|---|---|---|---|---|
|TC-COMMENT-001|Comment|P1|三类评论发布成功|登录用户可分别发布目的地、美食、公开日记评论|通过：MySQL 8 HTTP 测试|
|TC-COMMENT-002|Comment|P1|未登录写操作拦截|POST/DELETE 返回 401、`AUTH_003`|通过|
|TC-COMMENT-003|Comment|P1|评论正文校验|空白或超过 500 字符返回 `COMMENT_003`|通过：单元测试|
|TC-COMMENT-004|Comment|P1|目标状态校验|禁用目的地、私有/禁用日记和不存在美食不可评论|通过：Handler 单元测试 + 私有日记实库验证|
|TC-COMMENT-005|Comment|P1|一级评论分页展示|只返回 `parent_comment_id IS NULL`、`status=1`，按时间和 ID 倒序|通过：实库 SQL/HTTP 验证|
|TC-COMMENT-006|Comment|P1|所有者软删除|普通用户只能删除自己的评论并置 `status=2`|通过：单元测试|
|TC-COMMENT-007|Comment|P1|越权删除拦截|普通用户删除他人评论返回 403、`COMMENT_004`|通过：MySQL 8 HTTP 测试|
|TC-COMMENT-008|Comment|P1|管理员隐藏评论|管理员可将任意正常评论置 `status=0`，列表不再展示|通过：MySQL 8 HTTP 测试|

## 16. 路线交通工具策略测试

|用例编号|模块|优先级|测试目标|验收标准|当前状态|
|---|---|---|---|---|---|
|TC-ROUTE-014|GraphEngine|P0|单一交通工具过滤|walk/bike/cart 只能通过对应或组合权限道路|通过：单元测试|
|TC-ROUTE-015|GraphEngine|P0|共享道路时间成本|同一 `walk_bike` 道路按实际工具速度计算不同时间|通过：单元测试|
|TC-ROUTE-016|GraphEngine|P0|mixed 最短时间|路径可组合 walk 与 bike/cart，分边返回实际工具|通过：单元测试|
|TC-ROUTE-017|MapService|P0|约束贯穿单目标和多目标|所有分段及返回起点段使用同一交通约束|通过：单元测试|
|TC-ROUTE-018|Route|P0|路线历史交通信息|顶层保存请求模式，路径边 JSON 保存实际工具|通过：单元测试|
|TC-ROUTE-022|Route|P1|实库交通道路演示|校园 bike、景区 cart、mixed 返回可解释的差异路线|待补演示数据后执行|

## 17. AIGC 日记照片动画测试

|用例编号|模块|优先级|测试目标|验收标准|当前状态|
|---|---|---|---|---|---|
|TC-AI-ANIM-001|AIService|P1|模板生成合法脚本|场景顺序、总时长、动效和 JSON 均合法|通过：单元测试|
|TC-AI-ANIM-002|Animation|P1|未登录生成拦截|返回 `AUTH_003`|通过：单元测试|
|TC-AI-ANIM-003|Animation|P1|非作者生成拦截|返回 403、`AUTH_005`|通过：单元测试|
|TC-AI-ANIM-004|Animation|P1|无图片日记拒绝|返回 422、`AI_009`|通过：单元测试|
|TC-AI-ANIM-005|Animation|P1|公开/私有日记生成|作者均可生成，日记原流程不变|通过：单元测试|
|TC-AI-ANIM-006|Animation|P1|重复生成覆盖|同一 `diary_id` 更新原记录，不新增版本|通过：单元测试|
|TC-AI-ANIM-007|Animation|P1|媒体引用安全|脚本引用其他日记媒体时返回 `AI_007`|通过：单元测试|
|TC-AI-ANIM-008|Animation|P1|查询权限|公开动画匿名可查，私有动画仅作者可查|通过：单元测试|
|TC-AI-ANIM-009|Animation|P1|实库接口回归|迁移建表后生成、重复生成、查询与落库一致|待执行|
|TC-AI-ANIM-010|Frontend|P1|照片动画播放|前端按 `script.scenes` 完成字幕、动效和转场播放|待前端实现|
|TC-AI-ANIM-011|AI Provider|P1|真实协议请求|Mock Server 收到模型名、Bearer 头、文本上下文和 Base64 图片|通过：单元测试|
|TC-AI-ANIM-012|AI Provider|P1|严格 JSON 解析|合法 JSON 生成统一脚本；非 JSON、字段非法和错误媒体引用触发降级|通过：单元测试|
|TC-AI-ANIM-013|AI Provider|P1|超时和 HTTP 错误降级|读取超时或 5xx 时返回 `mock-template`，不影响日记接口|通过：Mock Server 测试|
|TC-AI-ANIM-014|AI Provider|P1|本地图片安全读取|只允许 `/files/diary/...`，限制数量/大小/格式并校验路径边界|通过：单元测试|
|TC-AI-ANIM-015|Animation|P1|媒体快照并发保护|AI 调用期间媒体集合变化时不保存过期脚本|通过：单元测试|
|TC-AI-ANIM-016|AI Provider|P1|真实厂商联调|设置环境变量后响应 `provider=openai-compatible` 且视觉描述与图片相关|待提供测试密钥后执行|

## 18. 按目的地名称查询相关日记测试

|用例编号|模块|优先级|测试目标|验收标准|当前状态|
|---|---|---|---|---|---|
|TC-DIARY-029|Diary/Query/IndexEngine|P0|目的地名称精确和前缀查询|返回所有匹配目的地下公开启用日记，分页正确|通过：单元测试与 MySQL 8 HTTP 测试|
|TC-DIARY-030|Diary/Query|P0|目的地名称中间包含查询|Trie 未覆盖的包含语义仍由 `name LIKE` 正确召回|通过：MySQL 8 HTTP 测试|
|TC-DIARY-031|Diary|P0|热度和评分排序|目的地关键字过滤后按数据库实时 `heat_score/rating_score` 排序|通过：MySQL 8 HTTP 测试|
|TC-DIARY-032|Diary/IndexEngine|P1|索引失效降级一致性|索引启用和失效时列表、顺序、分页字段完全一致|通过：`IndexEngineDatabaseIntegrationTests`|
|TC-DIARY-033|Diary|P1|无匹配与组合过滤|无匹配返回空分页；`destinationId` 与关键字同时传入时取交集|通过：单元测试|

## 19. 个性化日记推荐测试

|用例编号|模块|优先级|测试目标|验收标准|当前状态|
|---|---|---|---|---|---|
|TC-DIARY-REC-001|Diary/Recommend|P0|兴趣偏好改变顺序|匹配用户主题的日记优先于不匹配候选|通过：单元测试|
|TC-DIARY-REC-002|Diary/Recommend|P0|无偏好降级|无有效文本偏好时按实时 `heat_score` Top-K|通过：单元测试|
|TC-DIARY-REC-003|Diary/Rank|P0|评分 Top-K 与稳定同分顺序|按 `rating_score` 选 K，同分保持候选顺序|通过：单元测试|
|TC-DIARY-REC-004|Diary/Recommend|P1|空值与零热度|空标题、正文、目的地字段和全零热度不报错|通过：单元测试|
|TC-DIARY-REC-005|Diary/Security|P0|匿名访问拦截|未携带 JWT 返回 HTTP 401、`AUTH_003`|通过：MockMvc|
|TC-DIARY-REC-006|Diary/Recommend|P1|实库偏好变化回归|同一批日记下，两组明显不同的偏好分别命中对应兴趣日记，响应字段不变|通过：MySQL 8 真实 JWT HTTP 测试|
|TC-DIARY-REC-007|Diary/Recommend|P1|非个性化策略用户无关性|两个用户请求 `heat/rating` 时返回完全相同的日记 ID 顺序|通过：MySQL 8 真实 JWT HTTP 测试|
|TC-DIARY-REC-008|Diary/Recommend|P1|推荐查询无浏览量副作用|六次推荐请求前后测试日记 `heat_score` 不变|通过：MySQL 8 数据库核验|
|TC-DIARY-REC-009|Demo Data|P0|演示偏好等级合法且可重复导入|三个演示用户的热门、拥挤等级均为 `1..5`，重复执行 SQL 不产生重复偏好|等级静态测试通过；实库幂等导入待校园美食数据补齐|

实库回归入口：

```powershell
cd project-root/backend
mvn -q -DskipTests package
cd ../..
powershell -NoProfile -ExecutionPolicy Bypass `
  -File .\project-root\scripts\verify-diary-recommend.ps1 `
  -BaseUrl http://127.0.0.1:18080
```

脚本使用随机测试标记创建两个临时用户、一条目的地和四篇公开日记，并在 `finally`
中清理。测试账号口令和 JWT 只保存在当前进程内，不写入报告。

## 20. 目的地与美食推荐真分页测试

|用例编号|模块|优先级|测试目标|验收标准|当前状态|
|---|---|---|---|---|---|
|TC-RECOMMEND-PAGE-001|Recommend|P0|目的地相邻页切片|第 1、2 页各按 `pageSize` 返回，ID 无交集|通过：单元测试与 MySQL 8 HTTP 测试|
|TC-RECOMMEND-PAGE-002|Recommend|P0|目的地完整候选统计|`total` 为全部启用且符合筛选条件的候选数，`pages=ceil(total/pageSize)`|通过：实库返回 `total=1315`、`pages=42`|
|TC-RECOMMEND-PAGE-003|Recommend|P1|目的地 Top-K 兼容|传 `topK` 时忽略分页参数，固定返回第 1 页前 K 条|通过：单元测试|
|TC-FOOD-PAGE-001|Food|P0|美食推荐分页参数生效|未传 `topK` 时使用 `pageNum/pageSize`，不再默认截为 10 条|通过：单元测试与 MySQL 8 HTTP 测试|
|TC-FOOD-PAGE-002|Food|P1|美食 Top-K 兼容|显式 `topK=5` 时仍只返回 5 条，响应页码为 1|通过：单元测试与 MySQL 8 HTTP 测试|
|TC-FOOD-PAGE-003|Food|P1|美食跨页切片|65 条候选按 32 条分页应返回 32、32、1，页间 ID 不重复|通过：构造候选单元测试|
