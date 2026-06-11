# 项目进度（Progress）

## 1. 文件用途
本文件用于记录项目当前推进状态、阶段性完成情况、下一步计划和阻塞问题，是项目过程管理的核心记录之一。

## 2. 使用原则
1. 本文件关注的是“项目推进情况”，不是零散任务草稿。
2. 更新时应尽量体现：
   - 当前做到哪里了
   - 已完成什么
   - 正在做什么
   - 接下来做什么
   - 卡在哪里
3. 建议每周至少更新一次；如有较大阶段变化，可额外更新。
4. 更新记录按时间倒序保留，便于回顾。

## 3. 当前项目总体状态
- 2026-06-11：完成目的地、美食、日记评论基础版。新增统一 CommentService、三类 Handler/Mapper、六个列表与发布接口和统一软删除接口；实库验证三类发布、公开列表、私有日记拒绝、越权删除与管理员隐藏通过。
- 2026-06-10：完成 P1 Diary 评分闭环。新增评分明细表、评分人数聚合、重复评分覆盖、公开启用状态校验、JWT 鉴权和当前用户评分查询；实库迁移重复执行成功，定向单元、鉴权和 MySQL 聚合排序测试通过。
- 2026-06-08：完成演示数据导入前结构准备，补充 Facility 地址/电话/封面、Food 经纬度、三类评论表、现有库迁移 SQL 和幂等演示内容 SQL；实库迁移重复执行成功，全量 200 个测试通过。当时评论业务 API 尚未实现，已于 2026-06-11 补齐基础版。
- 项目阶段：P0 核心业务模块已完成基础版并完成一次后端主线接口演示预检，P1 Food、Admin 场所管理与后续底层维护能力、SearchService / Diary 检索排序、ImportService 完整化基础版、Route 多目标基础版和 Route 最短时间策略已完成后端实现与关键验证，前端页面联调暂时阻塞
- 当前状态：后端 P0 主线已具备最小可运行闭环；P1 公共能力中 GraphEngine 已完成抽取和实库回归，IndexEngine 已完成 Hash、Trie、正文倒排索引及增量维护，CompressionEngine 已完成 Huffman 无损压缩最小版并接入日记发布。现有 API 和原文语义保持不变；因暂无法提供前端工程代码，页面联调先标记为阻塞
- 当前重点：
  - 稳定 P0 后端接口联调结果
  - 在前端工程代码可用前，先沉淀 P0 后端接口测试记录
  - 按 `coding-plan.md` 顺序继续补 P1 后续增强
  - 补充必要测试记录与交付材料
  - 保持接口文档、模块文档、进度记录与代码实现一致

## 4. 阶段划分建议
项目进度建议按以下阶段管理：

1. 需求分析阶段
2. 总体架构与模块设计阶段
3. 数据结构与数据库设计阶段
4. 接口设计阶段
5. 开发环境与工程骨架搭建阶段
6. 核心模块开发阶段
7. 联调与测试阶段
8. 文档整理与答辩准备阶段

## 5. 进度总表模板

| 阶段 | 当前状态 | 开始时间 | 目标完成时间 | 负责人 | 备注 |
|---|---|---|---|---|---|
| 需求分析 | 进行中 | YYYY-MM-DD | YYYY-MM-DD | 待填写 | 需求文档正在完善 |
| 架构设计 | 未开始 |  |  | 待填写 |  |
| 数据设计 | 未开始 |  |  | 待填写 |  |
| 接口设计 | 未开始 |  |  | 待填写 |  |
| 开发环境搭建 | 未开始 |  |  | 待填写 |  |
| 核心模块开发 | 未开始 |  |  | 待填写 |  |
| 联调与测试 | 未开始 |  |  | 待填写 |  |
| 文档与答辩准备 | 未开始 |  |  | 待填写 |  |

## 6. 当前更新记录

### [第23次更新] 2026-06-10

#### 6.1 本阶段目标
- 完成日记评分明细、聚合、鉴权和排序反馈闭环。

#### 6.2 已完成
- 新增 `diary_rating` 和 `(diary_id,user_id)` 唯一约束。
- 新增 `diary.rating_count`，评分后事务内重新计算 AVG 与 COUNT。
- 实现评分提交/覆盖和当前用户评分查询接口。
- 允许作者自评；禁止评分私有、禁用或不存在的日记。
- 实库迁移连续执行两次成功，实库测试验证重复评分、双用户平均分和评分排序。
- 后端全量回归共 28 个测试套件、211 个测试，0 失败、0 错误、0 跳过。

#### 6.3 下一步计划
- 完成全量回归后进入评论基础能力或前端评分联调。

### [第22次更新] 2026-06-07

#### 6.1 本阶段目标
- 完成 `CompressionEngine` 第二阶段维护闭环，为历史日记提供压缩回填、校验和修复能力。

#### 6.2 已完成
- 新增 `CompressionMaintenanceService`，使用递增主键游标分批读取历史日记。
- 默认仅回填 `content_compressed` 为空的数据；可选校验已有压缩包。
- 压缩包损坏、版本不支持或解压内容与原文不一致时，以 `content_text` 为可信来源重新生成。
- 条件更新同时校验 ID、正文及缺失状态，避免覆盖维护期间发生的业务写入。
- 新增默认关闭的启动配置：`backfill-enabled`、`batch-size`、`verify-existing`。
- 维护任务输出扫描、回填、校验、修复、跳过、失败和压缩率统计，不输出正文。
- 新增 4 项维护服务测试；执行 `mvn -q test` 通过：25 个测试套件、198 个测试，0 失败、0 错误、0 跳过。
- 完成 MySQL 8 实库验证：3 篇历史日记全部回填成功；二次校验 3 篇全部可解压且与原文一致。

#### 6.3 当前结论
- 未新增 Controller、API、DTO、VO、数据库字段或第三方依赖。
- `content_text` 仍是业务读取和损坏修复的主数据，`content_compressed` 仍是可降级副本。
- 默认配置不会在应用启动时修改实库。
- 当前 3 篇短正文原文共 253 字节、压缩包共 1067 字节；独立频次表导致短文本压缩率不佳，后续不能仅以 Huffman 名义宣称节省所有正文空间。

#### 6.4 下一步计划
- 保持启动回填开关默认关闭，后续新增历史数据时再按需执行。
- 暂不进入压缩优先读取或删除原文阶段。

### [第21次更新] 2026-06-07

#### 6.1 本阶段目标
- 实现 `CompressionEngine` 第一阶段 Huffman 无损压缩，并在不改变接口和表结构的前提下接入日记发布。

#### 6.2 已完成
- 新增纯算法 `CompressionEngine`，使用 Unicode 码点频次 Map、优先队列和 Huffman 二叉树完成编码与解码。
- 压缩包包含版本、频次表、原文长度、有效位数和 CRC32，可独立解码并检测损坏数据。
- 复用已有 `diary.content_compressed`，同时保留 `diary.content_text` 原文。
- 日记发布时写入压缩副本；压缩异常时降级为空，不影响原文和媒体发布事务。
- 默认 MyBatis 查询不读取 `content_compressed` BLOB，列表、详情、检索和 API 契约不变。
- 专项测试覆盖 ASCII、中文、Emoji、辅助平面 Unicode、空文本、单字符、10000 字正文、确定性编码、损坏数据和业务降级。
- 执行 `mvn -q test` 通过：24 个测试套件、194 个测试，0 失败、0 错误、0 跳过。

#### 6.3 当前结论
- 未新增数据库表、第三方依赖、Controller、DTO、VO 或 API。
- 短文本可能因自描述头部大于原文；该实现定位为课程算法展示和中小规模正文存储增强。
- 历史日记暂未回填压缩数据。

#### 6.4 下一步计划
- 运行全量测试并补一次 MySQL 实库发布验证，确认 `content_compressed` 实际写入且现有详情和全文检索结果不变。

### [第20次更新] 2026-06-07

#### 6.1 本阶段目标
- 将 `DIARY_CONTENT` 从日记写入后整 namespace 失效，调整为事务提交后单文档增量维护。

#### 6.2 已完成
- `IndexEngine` 新增仅面向 `DIARY_CONTENT` 的 `upsert` 和 `remove`，索引不可用时保持 `UNAVAILABLE`，不创建不完整快照。
- `InvertedIndex` 保存文档码点序列，并通过写时复制更新受影响字符的 posting 子表。
- `IndexMaintenanceService` 新增提交后 upsert、remove 和 invalidate；事务回滚不修改索引，维护异常时自动失效 namespace。
- 公开日记发布后增量加入正文索引，私有日记发布后确保从索引移除。
- 管理员禁用日记后移除正文索引，重新启用公开日记后恢复正文索引。
- `DIARY_TITLE` 调整为事务提交后失效，暂未实现标题增量维护。
- 单元测试覆盖新增、替换、删除、Unicode、不可用状态、提交、回滚、异常降级及业务写入分支。
- 执行专项测试和 `mvn -q test` 均通过；全量结果为 23 个测试套件、186 个测试，0 失败、0 错误、0 跳过。

#### 6.3 当前结论
- 未修改 Controller、DTO、VO、API 路径、接口字段或数据库结构。
- 正文写入不再触发全量索引失效；SearchService 的 HIT / MISS / UNAVAILABLE 和 Mapper 过滤语义保持不变。
- 当前为单体进程内索引方案，不包含多实例同步、定时重建或持久化集合优化。

#### 6.4 下一步计划
- 可补充真实事务与 HTTP 场景下的增量索引实库回归，重点验证发布公开/私有日记及管理员禁用/启用后的检索结果。

### [第19次更新] 2026-06-07

#### 6.1 本阶段目标
- 实现 IndexEngine 第二阶段 `Diary.contentText` 倒排索引全文检索，并保留索引不可用时的 MySQL LIKE 兜底。

#### 6.2 已完成
- 新增 `DIARY_CONTENT` namespace 和字符位置倒排索引，按 Unicode 码点保存“字符 -> 日记 ID -> 出现位置列表”。
- 查询时通过字符对应文档集合和连续位置校验实现中文、英文、数字连续子串匹配，英文统一转小写。
- SearchService 已实现三态调度：HIT 使用候选 ID，MISS 返回空分页，UNAVAILABLE 使用 `content_text LIKE`。
- Mapper 继续负责公开/启用过滤、`destinationId`、`latest/heat/rating` 排序和分页。
- 应用启动时预热公开启用日记正文；日记发布和后台状态修改后同时失效标题与正文索引。
- 单元测试覆盖中文子串、大小写、单字符、错误顺序、非连续字符、重建、失效及三态 SearchService 调度。
- MySQL 8 实库 HTTP 回归覆盖中文、英文、目的地过滤、分页和 MISS；索引启用与失效状态下完整分页响应一致。
- 实库日志确认索引启用时正文查询使用 `id IN`，索引失效后使用 `content_text LIKE`。
- 后端全量测试：23 个测试套件、178 个测试，0 失败、0 错误、0 跳过。
- 测试结束后临时数据全部清理，五个测试 namespace 均已重新构建。

#### 6.3 当前结论
- 第二阶段未修改 Controller、DTO、VO、数据库结构或 API 文档。
- 字符位置倒排索引适合课程设计和中小规模数据，不替代大规模专业搜索引擎。

#### 6.4 下一步计划
- 可进入 CompressionEngine 的 Huffman 无损压缩最小版，继续保持 Diary 对外接口不变。

### [第18次更新] 2026-06-07

#### 6.1 本阶段目标
- 对 IndexEngine 第一阶段做 MySQL 8 实库接口回归，比较索引启用和失效两种状态下的目的地、美食和日记标题搜索契约。

#### 6.2 已完成
- 新增 `IndexEngineDatabaseIntegrationTests`，使用随机端口启动真实 HTTP 服务，不增加生产调试接口。
- 准备随机唯一前缀的临时用户、4 条目的地、4 条美食和 4 篇公开日记。
- 重建并验证 `DESTINATION_NAME`、`FOOD_NAME`、`FOOD_SHOP_NAME`、`DIARY_TITLE`，确认 Hash 精确查询和 Trie 前缀查询能召回预期 ID。
- 覆盖目的地精确/前缀分页、美食名称分页/店铺名精确、日记标题精确/前缀分页/非前缀包含，共 10 组 HTTP 请求。
- 失效全部测试 namespace 后使用相同参数重复请求，完整比较 `data.list`、顺序、`pageNum`、`pageSize`、`total` 和 `pages`，结果完全一致。
- 确认日记标题非前缀包含场景继续由 MySQL `LIKE` 召回，Trie 没有替代原有包含匹配语义。
- 专项命令 `mvn -q -Dtest=IndexEngineDatabaseIntegrationTests test` 执行通过。
- 已执行后端全量测试 `mvn -q test`：23 个测试套件、171 个测试，0 失败、0 错误、0 跳过。
- 测试结束后临时日记、美食、目的地和用户数据均已清理，四个索引已重新构建。
- 已同步 `docs/06_testing/test-cases.md` 和 `docs/06_testing/test-report.md`。

#### 6.3 当前结论
- IndexEngine 第一阶段已通过单元测试和实库 HTTP 接口回归，现有 Controller、DTO、VO、接口路径、分页和排序语义未发生变化。
- 当前索引与 LIKE 仍以兼容性优先方式共同参与查询，本轮结论是语义一致，不代表已完成 SQL 性能基准测试。

#### 6.4 下一步计划
- 运行后端全量测试，确认新增实库测试未影响其他模块。
- 后续可单独评估 IndexEngine 命中率、候选规模和 SQL 执行计划，不在本次兼容性回归范围内。

### [第17次更新] 2026-06-03

#### 6.1 本阶段目标
- 在 GraphEngine 抽取后，对 Route / Facility 做一次实库接口回归验证，确认接口语义和落库行为没有变化。

#### 6.2 已完成
- 已按 UTF-8 显式编码复核项目规则、Route / Facility 接口入口、请求对象、响应对象、安全配置和测试文档。
- 已确认 `POST /api/v1/routes/plan/single`、`POST /api/v1/routes/plan/multi` 需要登录 token，`GET /api/v1/facilities/nearby` 为公开 GET 接口。
- 已重新执行 `mvn -q package -DskipTests`，使用包含 GraphEngine 抽取后代码的新 jar 启动后端。
- 已连接 MySQL 8 实库并准备临时普通用户、临时目的地、4 个地图节点、4 条地图边和 1 个临时设施。
- 已验证 `strategyType=shortest_distance` 返回路径 `A -> B -> C`，总距离 `200.00`，并写入 `route_history`。
- 已验证 `strategyType=shortest_time` 返回路径 `A -> C`，总距离 `300.00`，预计时间 `3`，并写入 `route_history`。
- 已验证多目标路线 `returnToStart=false` 返回路径 `A -> B -> C`，总距离 `200.00`，并写入 `route_history`。
- 已验证 `/api/v1/facilities/nearby` 能返回临时 toilet 设施，`reachableDistance=40.00`，来源节点和目标设施节点正确。
- 已验证不存在的设施类型返回空列表且接口仍为 `SUCCESS`。
- 已按依赖顺序清理临时 `route_history`、`map_edge`、`map_node`、`facility`、`destination`、`user` 数据，清理后剩余临时数据为 0。
- 已同步更新 `docs/06_testing/test-cases.md` 和 `docs/06_testing/test-report.md`。

#### 6.3 进行中
- GraphEngine 抽取后的 Route / Facility 后端接口回归已通过，前端页面联调仍等待前端工程代码。

#### 6.4 下一步计划
- 若继续算法重构，建议进入 `RankService` 小顶堆 Top-K 优化，保持接口字段不变。
- 若继续联调准备，建议整理 Apifox 可导入接口范围，并继续避免把代码未实现接口纳入当前联调清单。

### [第16次更新] 2026-05-07

#### 6.1 本阶段目标
- 做一次 P0 主线后端接口演示预检，用同一个登录 token 依次验证登录、推荐、搜索、单目标路线、文件上传、日记发布 / 查看，并补齐测试记录。

#### 6.2 已完成
- 已按 UTF-8 显式编码复核项目规则、开发顺序、API 文档、测试用例、测试报告和进度文档。
- 已确认文档中提到的 `project-root/docs/coding-rules.md`、`project-root/docs/security-rules.md` 在仓库中不存在，实际规则文件为 `project-root/coding-rules.md`、`project-root/security-rules.md`。
- 已确认 `DB_USERNAME`、`DB_PASSWORD` 已设置，文档和输出中未记录真实密码；`DB_URL` 未设置时使用 `application-dev.yml` 默认本地 `tour_system`。
- 已确认 P0 关键表存在：`user`、`destination`、`map_node`、`map_edge`、`route_history`、`diary`、`diary_media`。
- 已启动后端 jar 并验证 `GET /api/v1/health` 返回 `SUCCESS`、`data.status=ok`。
- 已注册并登录临时普通用户，使用同一个 Bearer token 串联验证：
  - `GET /api/v1/destinations/recommend?sortBy=heat&pageNum=1&pageSize=10&topK=5` 返回 `SUCCESS`，并命中临时目的地。
  - `GET /api/v1/destinations/search?keyword=<临时目的地名>&sortBy=rating&pageNum=1&pageSize=10` 返回 `SUCCESS`，并命中临时目的地。
  - `POST /api/v1/routes/plan/single` 返回 `SUCCESS`，临时有向图 `A -> B -> C` 的最短距离为 `200.00`，预计时间为 `3`。
  - 已根据 `historyId=7` 查询 `route_history`，确认用户、目的地、策略、交通方式和总距离落库正确。
  - `POST /api/v1/files/upload` 上传最小 png 成功，返回 `/files/diary/20260507/...`。
  - `POST /api/v1/diaries` 发布临时公开图文日记成功，返回 `diaryId=3`。
  - `GET /api/v1/diaries`、`GET /api/v1/diaries/{id}`、`GET /api/v1/destinations/{id}/diaries` 均可查到本次临时日记，`diary_media` 落库 1 条。
- 已按依赖顺序清理临时 `diary_media`、`diary`、`route_history`、`map_edge`、`map_node`、`destination`、`user` 数据，清理后剩余临时数据为 0。

#### 6.3 进行中
- P0 主线后端接口已经具备演示预检结果，前端页面联调仍等待前端工程代码。

#### 6.4 下一步计划
- 若继续测试优先，可补 Facility 附近设施和 Recommend 更多筛选组合的实库接口测试记录。
- 若继续按 `coding-plan.md` 进入 P1，可进入 Diary 评分基础版。

#### 6.5 当前阻塞 / 风险
- 本次只验证后端接口，不代表前端页面、路由、Pinia token 状态、Axios 拦截器或上传组件联调通过。
- 本次使用最小临时数据，适合演示预检；最终验收前仍需要准备更完整的演示数据集。
- 后台启动方式在当前执行环境中需要把启动和验证放在同一个 PowerShell 会话中完成；分离后台进程会被执行环境清理。

#### 6.6 需要同步更新的文档
- 已同步：`docs/06_testing/test-cases.md`、`docs/06_testing/test-report.md`、`docs/00_project/progress.md`。

---

### [第15次更新] 2026-05-07

#### 6.1 本阶段目标
- 按 `coding-plan.md` P1 顺序补 Diary 检索与排序增强，在现有 SearchService 基础版上支持检索结果按最新、热度、评分排序。

#### 6.2 已完成
- 已扩展 `GET /api/v1/diaries/search/title` 的查询参数，支持 `sortBy=latest/heat/rating`。
- 已扩展 `GET /api/v1/diaries/search/fulltext` 的查询参数，支持 `sortBy=latest/heat/rating`，并保留 `destinationId` 过滤。
- 已在 `SearchServiceImpl` 中补充排序字段白名单，非法排序字段返回 `COMMON_008`，避免不可信排序字段直接进入查询。
- 当前排序规则为：`latest` 按创建时间倒序，`heat` 按热度倒序后再按创建时间倒序，`rating` 按评分倒序后再按创建时间倒序。
- 已补充 `SearchServiceTests`，覆盖标题热度排序、正文评分排序和非法排序字段。
- 已执行后端全量测试：`mvn test`，157 个测试执行，0 失败，构建成功。

#### 6.3 进行中
- Diary 检索排序当前已完成后端基础版和单元测试，尚未做实库接口验证。
- 前端页面联调仍等待前端工程代码。

#### 6.4 下一步计划
- 按 `coding-plan.md` 继续进入 Diary 评分基础版。
- 如测试优先，可先对 Diary 检索排序做一次实库接口验证。

#### 6.5 当前阻塞 / 风险
- 当前检索仍基于 MySQL `LIKE`，适合课程设计小规模样例，不适合大规模全文搜索。
- 当前尚未实现倒排索引或 MySQL FULLTEXT。
- `rating` 排序依赖后续 Diary 评分机制持续维护 `diary.rating_score`。

#### 6.6 需要同步更新的文档
- 已同步：`docs/04_api/api-spec.md`、`docs/05_modules/diary-module.md`、`docs/06_testing/test-cases.md`、`docs/06_testing/test-report.md`、`docs/00_project/progress.md`。

---

### [第14次更新] 2026-05-07

#### 6.1 本阶段目标
- 对 Route 最短时间策略做 MySQL 8 实库接口验证，并同步测试、进度、模块和 API 文档状态。

#### 6.2 已完成
- 已确认 `DB_USERNAME`、`DB_PASSWORD` 已设置，`DB_URL` 未设置时使用 `application-dev.yml` 默认本地 `tour_system` 连接；文档中不记录真实密码。
- 已确认 `destination`、`map_node`、`map_edge`、`route_history`、`user` 等关键表存在。
- 已构建并启动后端，`GET /api/v1/health` 返回 `SUCCESS`，`data.status=ok`。
- 已注册并登录临时普通用户，使用 Bearer token 调用 `POST /api/v1/routes/plan/single`。
- 已在实库创建临时目的地、3 个临时地图节点和 3 条临时有向边，构造距离最优与时间最优路径不同的测试图。
- 已验证 `strategyType=shortest_distance`：路径为 `A -> B -> C`，总距离 `200.00`，`estimatedTime=3`，并写入 `route_history`。
- 已验证 `strategyType=shortest_time`：路径为 `A -> C`，总距离 `300.00`，`estimatedTime=3`，并写入 `route_history`。
- 已根据接口返回的 `historyId` 查询 `route_history`，确认两条记录的用户、目的地、起点、终点、策略类型、交通方式、总距离和预计时间均正确。
- 已验证无 token 调用单目标规划返回 HTTP 401，错误码 `AUTH_003`。
- 已按外键依赖顺序清理临时 `route_history`、`map_edge`、`map_node`、`destination` 和 `user`，剩余临时数据为 0。

#### 6.3 进行中
- Route 最短时间策略当前已完成后端基础版、单元测试和单目标实库接口验证，尚未做前端页面联调。
- 前端页面联调仍等待前端工程代码。

#### 6.4 下一步计划
- 按 `coding-plan.md` 继续 P1 后续模块，优先补 Diary 检索与排序或 Diary 评分。
- 如继续测试优先，可补 Facility、Recommend 的实库接口测试记录。

#### 6.5 当前阻塞 / 风险
- 当前最短时间策略基于 `ideal_speed` 与 `crowd_factor` 计算时间权重，依赖地图边数据质量。
- 当前交通工具边过滤尚未实现，`transportType` 仍主要用于记录路线历史。
- 当前未执行前端路线规划页面联调。

#### 6.6 需要同步更新的文档
- 已同步：`docs/06_testing/test-cases.md`、`docs/06_testing/test-report.md`、`docs/00_project/progress.md`、`docs/05_modules/route-module.md`、`docs/04_api/api-spec.md`。

---

### [第13次更新] 2026-05-07

#### 6.1 本阶段目标
- 对 Route 多目标路径规划基础版做 MySQL 8 实库接口验证。

#### 6.2 已完成
- 已确认 `DB_USERNAME`、`DB_PASSWORD` 已设置，`DB_URL` 未设置时使用 `application-dev.yml` 默认本地 `tour_system` 连接；文档中不记录真实密码。
- 已确认 `user`、`destination`、`map_node`、`map_edge`、`route_history` 等关键表存在。
- 已构建并启动后端，`GET /api/v1/health` 返回 `SUCCESS`，`data.status=ok`。
- 已注册并登录临时普通用户，使用 Bearer token 调用 `POST /api/v1/routes/plan/multi`。
- 已在实库创建临时目的地、4 个临时地图节点和 4 条临时有向边，构造可预测路径 `A -> B -> C -> A`。
- 已验证 `returnToStart=true` 成功路径：接口返回成功，路径为 `A -> B -> C -> A`，总距离为 `360.00`。
- 已根据接口返回的 `historyId` 查询 `route_history`，确认用户、目的地、起点、终点、策略、交通方式、总距离和路径 JSON 均正确。
- 已验证 `returnToStart=false` 成功返回，最终路径终点为最后访问目标 C。
- 已验证异常与安全边界：无 token 返回 HTTP 401；重复目标返回 HTTP 400、`COMMON_001`；不可达目标返回 HTTP 422、`ROUTE_003`。
- 已按外键依赖顺序清理临时 `route_history`、`map_edge`、`map_node`、`destination` 和 `user`，剩余临时数据为 0。

#### 6.3 进行中
- Route 多目标当前已完成后端基础版、单元测试和实库接口验证，尚未做前端页面联调。
- 前端页面联调仍等待前端工程代码。

#### 6.4 下一步计划
- 按 `coding-plan.md` 继续 P1 Route 最短时间策略。
- 如继续测试优先，可补 Route 单目标、Facility、Recommend 的实库接口测试记录。

#### 6.5 当前阻塞 / 风险
- Route 多目标仍为最近邻启发式，不保证 TSP 全局最优。
- 当前仅验证 `shortest_distance`，最短时间与交通工具边过滤仍未实现。
- 当前未执行前端多目标路线页面联调。

#### 6.6 需要同步更新的文档
- 已同步：`docs/06_testing/test-cases.md`、`docs/06_testing/test-report.md`、`docs/00_project/progress.md`。

---

### [第12次更新] 2026-05-07

#### 6.1 本阶段目标
- 按 `coding-plan.md` P1 顺序实现 Route 多目标路径规划基础版。

#### 6.2 已完成
- 已新增 `POST /api/v1/routes/plan/multi`，请求体包含 `destinationId`、`startNodeId`、`targetNodeIds`、`strategyType`、`transportType`、`returnToStart`。
- 已新增 `MultiRoutePlanRequest` 和 `MultiPathResult`，保持 DTO / Map 结果 / VO 分层。
- 已在 `MapService` 中实现多目标最近邻启发式路径规划：每一步基于当前节点运行 Dijkstra，在未访问目标中选择图上距离最近的节点，再拼接路径。
- 已支持 `returnToStart=true` 时在访问完目标点后追加返回起点路径。
- 已在 `RouteService` 中复用当前 JWT 用户上下文，保存多目标规划结果到 `route_history`，并返回 `RoutePlanVO`。
- 已限制多目标数量最多 8 个，并拦截重复目标、空目标、非法目标和不可达目标。
- 已补充 `MapServiceTests` 和 `RouteServiceTests`，覆盖最近邻拼接、返回起点、重复目标、目标数量超限、不可达目标和路线历史写入。
- 已执行后端全量测试：`mvn test`，151 个测试执行，0 失败，构建成功。

#### 6.3 进行中
- Route 多目标当前完成后端基础版和单元测试，尚未做实库接口验证。
- 前端页面联调仍等待前端工程代码。

#### 6.4 下一步计划
- 按 `coding-plan.md` 继续 P1 Route 最短时间策略。
- 如继续测试优先，可先补 Route 多目标实库接口验证，准备一组最小地图节点和边数据后用登录 token 调用 `/api/v1/routes/plan/multi`。

#### 6.5 当前阻塞 / 风险
- 当前多目标使用最近邻启发式，不保证 TSP 全局最优；适合课程设计小规模点位演示。
- 当前只支持 `shortest_distance`，`shortest_time` 和交通工具边过滤尚未实现。
- 当前未做前端多目标路线页面联调。

#### 6.6 需要同步更新的文档
- 已同步：`docs/04_api/api-spec.md`、`docs/04_api/api-changelog.md`、`docs/05_modules/route-module.md`、`docs/09_decisions/ADR-004-algorithm-choice.md`、`docs/06_testing/test-cases.md`、`docs/06_testing/test-report.md`、`docs/00_project/progress.md`。

---

### [第11次更新] 2026-05-06

#### 6.1 本阶段目标
- 补齐 P1 Admin 场所 / 建筑物管理，完善后台基础数据维护闭环。

#### 6.2 已完成
- 已新增 Admin 场所分页查询接口：`GET /api/v1/admin/places`，支持 `destinationId`、`keyword`、`type`、分页参数。
- 已新增 Admin 场所新增接口：`POST /api/v1/admin/places`。
- 已新增 Admin 场所修改接口：`PUT /api/v1/admin/places/{id}`。
- 已新增 Admin 场所删除接口：`DELETE /api/v1/admin/places/{id}`。
- 已新增 `AdminPlaceRequest` 与 `AdminPlaceVO`，保持 DTO / VO / Entity 分离。
- 已在删除场所前检查 `facility.place_id` 与 `map_node(node_type=place, ref_id=id)` 引用；存在引用时返回业务错误，避免破坏设施或图数据。
- 已补充 `AdminServiceTests`，覆盖场所列表、新增、目的地不存在、修改、引用删除保护和无引用删除。
- 已执行后端全量测试：`mvn test`，144 个测试执行，0 失败，构建成功。

#### 6.3 进行中
- Admin 场所管理当前已完成后端基础版和实库接口验证，尚未做前端页面联调。
- 前端页面联调仍等待前端工程代码。

#### 6.4 下一步计划
- 按 `coding-plan.md` 继续 P1 Route 多目标路径规划。
- 如继续测试优先，可补 Recommend、Route、Facility 的实库接口测试记录。

#### 6.5 当前阻塞 / 风险
- `place` 表当前没有 `status` 字段，因此本次对无引用场所采用物理删除；如后续需要“下架 / 恢复”，需新增字段并同步数据库文档。
- 本次已完成后端实库接口验证，但未声明前端页面联调通过。

#### 6.6 需要同步更新的文档
- 已同步：`docs/04_api/api-spec.md`、`docs/04_api/api-changelog.md`、`docs/05_modules/admin-module.md`、`docs/06_testing/test-cases.md`、`docs/06_testing/test-report.md`、`docs/00_project/progress.md`。

#### 6.7 追加实库接口验证
- 已连接 MySQL 8 实库，确认 `destination`、`place`、`facility`、`map_node`、`user` 等关键表存在，且已有目的地和管理员前置数据。
- 已启动后端并验证 `GET /api/v1/health` 返回 `SUCCESS`。
- 已使用临时管理员和普通用户验证 Admin 场所接口权限：无 token 返回 HTTP 401，普通用户返回 HTTP 403，管理员可访问。
- 已验证 `GET/POST/PUT/DELETE /api/v1/admin/places` 主流程：新增场所、按关键字查询、修改场所、删除无引用场所均通过。
- 已验证删除保护：被 `facility.place_id` 引用或被 `map_node(node_type=place, ref_id=id)` 引用的场所删除时返回 HTTP 400、错误码 `COMMON_002`。
- 已清理本次临时用户、场所、设施和地图节点测试数据；文档中未记录真实密码和 JWT token。
- 验证脚本中中文关键字曾受 PowerShell 请求编码影响，已使用 ASCII 测试名复测通过；该现象记录为测试脚本注意事项，不判定为后端接口缺陷。

---

### [第10次更新] 2026-05-06

#### 6.1 本阶段目标
- 做实库接口认证验证，确认当前 JWT 登录态和管理员权限链路在 MySQL 8 实库环境下可用。

#### 6.2 已完成
- 已启动后端连接 `tour_system` 实库，并验证 `GET /api/v1/health` 返回 `SUCCESS`。
- 已使用临时普通用户 `auth_user_20260506220129` 验证登录和 `GET /api/v1/auth/me`，返回 `role=user`。
- 已使用临时管理员 `auth_admin_20260506220129` 验证登录和 `GET /api/v1/admin/users?pageNum=1&pageSize=10`，返回 `SUCCESS`。
- 已验证普通用户访问管理端接口返回 HTTP 403，错误码 `AUTH_005`。
- 已验证无 token 请求 `/api/v1/auth/me` 返回 HTTP 401，错误码 `AUTH_003`。
- 已验证伪造 Bearer token 请求 `/api/v1/auth/me` 返回 HTTP 401，错误码 `AUTH_004`。
- 已验证非 Bearer 认证头请求 `/api/v1/auth/me` 返回 HTTP 401，错误码 `AUTH_003`。
- 已同步 `docs/06_testing/test-cases.md` 和 `docs/06_testing/test-report.md`。

#### 6.3 进行中
- 前端页面联调仍等待前端工程代码。
- 本次只做认证专项验证，没有继续扩展业务模块。

#### 6.4 下一步计划
- 按 `coding-plan.md` 继续 P1 后续增强，优先可进入 Route 多目标规划，或先补 Admin 场所管理。
- 如继续测试优先，可补 Recommend、Route、Facility 的实库接口测试记录。

#### 6.5 当前阻塞 / 风险
- 本次实库验证创建了临时普通用户和临时管理员账号，仅用于本地测试记录；如后续需要干净演示库，可在准备演示数据时统一清理或重建。
- 文档中未记录完整 JWT token 和真实数据库密码，符合安全约束。

#### 6.6 需要同步更新的文档
- 已同步：`docs/06_testing/test-cases.md`、`docs/06_testing/test-report.md`、`docs/00_project/progress.md`。

---

### [第9次更新] 2026-05-06

#### 6.1 本阶段目标
- 完善后台支撑，补充 ImportService 的导入批次查询与失败明细查询接口。

#### 6.2 已完成
- 已新增 `GET /api/v1/admin/import-batches`，支持管理员分页查看导入批次，并按批次名 / 文件名关键字、目标表和状态过滤。
- 已新增 `GET /api/v1/admin/import-batches/{batchId}/failures`，支持管理员分页查看指定批次的失败明细。
- 已新增 `AdminImportBatchVO`、`AdminImportFailureVO`，避免直接向前端返回 Entity。
- 已补充 `AdminServiceTests`，覆盖导入批次列表、失败明细列表和不存在批次返回 `COMMON_003`。
- 已执行最小测试：`mvn -q -Dtest=AdminServiceTests test`，通过。
- 已执行后端全量测试：`mvn test`，137 个测试执行，0 失败，构建成功。
- 已同步 API、模块、测试和进度文档。

#### 6.3 进行中
- ImportService 查询能力当前完成后端基础版，尚未做实库接口验证。
- 前端页面联调仍等待前端工程代码。

#### 6.4 下一步计划
- 可选做实库接口验证：管理员 token 请求导入批次列表和失败明细列表。
- 按 `coding-plan.md` 继续 P1 Route 多目标规划，或先补 Admin 场所管理。

#### 6.5 当前阻塞 / 风险
- 前端工程代码暂不可用，后台导入历史页面无法联调。
- 当前失败明细返回 `rawDataJson`，适用于当前基础数据导入范围；如后续开放用户或敏感数据导入，需要补脱敏策略。

#### 6.6 需要同步更新的文档
- 已同步：`docs/04_api/api-spec.md`、`docs/04_api/api-changelog.md`、`docs/05_modules/admin-module.md`、`docs/06_testing/test-cases.md`、`docs/06_testing/test-report.md`、`docs/00_project/progress.md`。

---

### [第8次更新] 2026-05-06

#### 6.1 本阶段目标
- 修复 ImportService 的 CSV UTF-8 BOM 表头兼容问题，避免 Windows 环境生成的 CSV 首列表头无法匹配必填字段。

#### 6.2 已完成
- 已在 `ImportServiceImpl` 的 CSV 表头归一化流程中剥离首列表头前的 UTF-8 BOM。
- 已补充 `ImportServiceTests.previewImportShouldParseCsvHeaderWithUtf8Bom` 回归测试，验证 `\uFEFFname,type,city` 可正常解析，且预览 warnings 为空。
- 已执行最小测试：`mvn -q -Dtest=ImportServiceTests test`，通过。
- 已执行后端全量测试：`mvn test`，134 个测试执行，0 失败，构建成功。
- 已同步测试用例和测试报告，记录 BOM 兼容修复状态。

#### 6.3 进行中
- ImportService 当前完成后端基础版、destination 实库 multipart 验证和 CSV BOM 兼容修复；其他支持表如 `place`、`facility`、`food`、`map_node`、`map_edge` 尚未逐表做实库导入验证。
- 前端页面联调仍等待前端工程代码。

#### 6.4 下一步计划
- 按 `coding-plan.md` 继续 P1 后续模块，优先评估 Route 多目标规划。
- 如先完善管理端支撑，可补导入批次 / 失败明细查询接口。

#### 6.5 当前阻塞 / 风险
- 当前 CSV 解析器仍为课程设计最小实现，支持逗号分隔和基础双引号转义，不支持 Excel、复杂换行字段或压缩包。
- 当前导入为行级失败记录，成功行会继续写入；若需要整表事务回滚，需要另行确认策略。
- 当前未支持用户、日记、路线历史等带敏感或复杂业务约束的数据导入。

#### 6.6 需要同步更新的文档
- 已同步：`docs/06_testing/test-cases.md`、`docs/06_testing/test-report.md`、`docs/00_project/progress.md`。

---

### [第7次更新] 2026-05-06

#### 6.1 本阶段目标
- 按 `coding-plan.md` P1 顺序完成 ImportService 完整化基础版，支持标准化 CSV / JSON 文件导入。

#### 6.2 已完成
- 已将 Admin 导入接口改为 `multipart/form-data`：`POST /api/v1/admin/import-batches/preview` 与 `POST /api/v1/admin/import-batches`。
- 已保持 Controller 负责接收 `MultipartFile`，核心 `ImportService` 基于 `Reader` 解析，避免绑定 Web 上传对象。
- 已支持 `destination`、`place`、`facility`、`food`、`map_node`、`map_edge` 的最小真实入库导入。
- 已新增 `import_batch` 与 `import_failure` 表结构、实体和 Mapper，用于导入批次摘要与行级失败明细。
- 已支持 CSV 首行表头映射、基础双引号转义和 JSON 对象数组解析。
- 已补充 `ImportServiceTests`，覆盖 CSV / JSON 元信息校验、预览解析、必填警告和目的地导入成功摘要。
- 已执行最小测试：`mvn -q -Dtest=ImportServiceTests test`，通过。
- 已执行后端全量测试：`mvn test`，133 个测试执行，0 失败，17 个跳过，构建成功。
- 已完成实库 multipart 导入验证：导入 `init-p1-import-schema.sql`，确认 `import_batch` / `import_failure` 表存在；使用管理员 token 调用导入预览接口，确认预览不写业务表和批次表；使用无 BOM UTF-8 `destinations-nobom.csv` 调用执行接口，`destination` 成功新增 1 条记录，`import_batch` 记录成功批次。
- 已完成失败明细验证：上传缺字段 CSV 返回 `FAILED`，`import_failure` 记录第 2 行失败原因；带 UTF-8 BOM 表头 CSV 曾触发字段匹配失败，已在第8次更新中修复。

#### 6.3 进行中
- ImportService 当前完成后端基础版和 destination 实库 multipart 验证；其他支持表如 `place`、`facility`、`food`、`map_node`、`map_edge` 尚未逐表做实库导入验证。
- 前端页面联调仍等待前端工程代码。

#### 6.4 下一步计划
- 按 P1 顺序评估是否进入 Route 多目标规划，或先补 Admin 场所管理和导入批次 / 失败明细查询接口。

#### 6.5 当前阻塞 / 风险
- 当前 CSV 解析器为课程设计最小实现，支持逗号分隔、基础双引号转义和 UTF-8 BOM 表头兼容，不支持 Excel、复杂换行字段或压缩包。
- 当前导入为行级失败记录，成功行会继续写入；若需要整表事务回滚，需要另行确认策略。
- 当前未支持用户、日记、路线历史等带敏感或复杂业务约束的数据导入。

#### 6.6 需要同步更新的文档
- 已同步：`docs/03_data/schema.md`、`docs/03_data/data-dictionary.md`、`docs/03_data/er-model.md`、`docs/03_data/import-plan.md`、`docs/04_api/api-spec.md`、`docs/05_modules/admin-module.md`、`docs/06_testing/test-cases.md`、`docs/06_testing/test-report.md`。

---

### [第6次更新] 2026-05-06

#### 6.1 本阶段目标
- 按 `coding-plan.md` P1 顺序实现 SearchService 基础版，为 Diary 检索增强提供公共能力。

#### 6.2 已完成
- 已新增 `SearchService` / `SearchServiceImpl`，当前基于 MySQL `LIKE` 实现小规模日记文本检索。
- 已新增 `GET /api/v1/diaries/search/title`，支持按标题关键词查询公开且启用的日记。
- 已新增 `GET /api/v1/diaries/search/fulltext`，支持按正文关键词查询公开且启用的日记，并支持 `destinationId` 过滤。
- 已新增检索请求 DTO，限制关键词长度和分页上限。
- 已补充 `SearchServiceTests`，覆盖标题检索、正文检索、分页上限和非法参数。
- 已补充 `DiaryServiceTests`，确认 Diary 复用 SearchService 后仍返回 `DiaryVO`。
- 已执行后端全量测试：`mvn test`，132 个测试执行，0 失败，17 个跳过，构建成功。

#### 6.3 进行中
- SearchService 当前完成基础版，尚未做实库接口联调。
- 前端页面联调仍等待前端工程代码。

#### 6.4 下一步计划
- 按 `coding-plan.md` 进入 ImportService 完整化，补 CSV / JSON 解析、导入批次持久化和失败明细。
- 或先补 Admin 场所管理，再进入 ImportService 真实入库。

#### 6.5 当前阻塞 / 风险
- 当前检索基于 MySQL `LIKE`，适合课程设计小规模样例，不适合大规模全文搜索。
- 未新增倒排索引、MySQL FULLTEXT 或第三方检索引擎。
- 前端工程代码暂不可用，仍不能声明页面联调通过。

#### 6.6 需要同步更新的文档
- 已同步：`docs/04_api/api-spec.md`、`docs/05_modules/diary-module.md`、`docs/06_testing/test-cases.md`、`docs/06_testing/test-report.md`、`docs/04_api/api-changelog.md`。

---

### [第5次更新] 2026-05-06

#### 6.1 本阶段目标
- 实现 P1 Admin 后续更底层的数据维护能力：地图节点 / 边维护、用户状态、日记状态和导入入口。

#### 6.2 已完成
- 已新增 Admin 地图节点接口：列表、新增、修改、删除。
- 已新增 Admin 地图边接口：列表、新增、修改、删除，并校验起点 / 终点节点属于同一目的地。
- 已新增 Admin 用户列表和用户状态修改接口，状态仅允许 `0/1`。
- 已新增 Admin 日记列表和日记状态修改接口，状态仅允许 `0/1`；前台 DiaryService 已按 `status=1` 过滤。
- 已新增 Admin 导入预览和执行入口，复用现有 `ImportService` 最小骨架；真实解析和入库仍留到 ImportService 完整化阶段。
- 已执行后端全量测试：`mvn test`，123 个测试执行，0 失败，17 个跳过，构建成功。

#### 6.3 进行中
- Admin 后续能力当前主要完成后端单元测试，尚未做实库接口联调。
- 前端页面联调仍等待前端工程代码。

#### 6.4 下一步计划
- 按 `coding-plan.md` 进入 SearchService，为 Diary 检索与排序增强做准备。
- 或继续 ImportService 完整化，补 CSV / JSON 解析、导入批次持久化和失败明细。

#### 6.5 当前阻塞 / 风险
- 导入入口当前不会真实写库，`ImportService.runImport` 仍返回 `NOT_IMPLEMENTED`。
- Admin 场所管理、导入批次查看仍未实现。
- 前端工程代码暂不可用，仍不能声明页面联调通过。

#### 6.6 需要同步更新的文档
- 已同步：`docs/04_api/api-spec.md`、`docs/05_modules/admin-module.md`、`docs/06_testing/test-cases.md`、`docs/06_testing/test-report.md`、`docs/04_api/api-changelog.md`。

---

### [第4次更新] 2026-05-06

#### 6.1 本阶段目标
- 对 P1 Food / Admin 最小后台实现做恢复后收尾，确认构建产物忽略规则和后端全量测试结果。

#### 6.2 已完成
- 已确认 `.gitignore` 包含 `project-root/backend/target/`，Maven 构建产物不会进入源码提交。
- 已确认 `project-root/backend/.m2/`、`project-root/backend/uploads/` 当前为 ignored 状态。
- 已执行后端全量测试：`mvn test`，117 个测试执行，0 失败，17 个跳过，构建成功。
- 已同步 `docs/06_testing/test-report.md`，补充 2026-05-06 的全量回归验证结果。

#### 6.3 进行中
- 前端页面联调仍等待前端工程代码。
- Admin 后续范围仍待按 `coding-plan.md` 继续拆分实现。

#### 6.4 下一步计划
- 优先补 Admin 后续范围中更底层的数据维护能力：地图节点 / 边维护、用户状态、日记状态和导入入口。
- 若暂不继续 Admin，则按 P1 顺序进入 SearchService，为 Diary 检索增强做准备。

#### 6.5 当前阻塞 / 风险
- 前端工程代码暂不可用，仍不能声明页面联调通过。
- 当前实库演示数据规模较小，完整演示前需要补目的地、设施、美食、地图节点和地图边数据。

#### 6.6 需要同步更新的文档
- 已同步：`docs/06_testing/test-report.md`。

---

### [第2次更新] 2026-05-05

#### 6.1 本阶段目标
- 按 `coding-plan.md` 的 P0 顺序完成后端核心业务基础版，并开始 P0 主线联调。

#### 6.2 已完成
- 已落地 P0 数据库表结构与初始化 SQL：`user`、`user_preference`、`destination`、`place`、`facility`、`map_node`、`map_edge`、`route_history`、`diary`、`diary_media`。
- 已实现后端基础支撑：统一返回体、错误码、全局异常处理、Security / JWT、CORS、健康检查。
- 已实现 P0 公共能力基础版：`QueryService`、`RankService`、`MapService`、`FileService`。
- 已实现 P0 业务模块基础版：Auth、UserPreference、Recommend、Route 单目标、Facility、Diary 图文基础版。
- 已完成 P0 Diary 实库联调：登录获取 token、上传 diary 文件、使用返回的 `/files/diary/...` URL 发布日记、验证日记列表、详情和目的地相关日记接口。
- 已修复本地上传文件静态访问问题，上传后的 `/files/diary/...` URL 可通过 HTTP 访问。
- 已补充 `.gitignore`，忽略 `project-root/backend/target/`，避免 Maven 构建产物进入提交。

#### 6.3 进行中
- P0 后端接口继续做实库联调和回归验证。
- 前端页面与 P0 后端接口联调准备。

#### 6.4 下一步计划
- 优先推进前端 Auth + File + Diary 页面联调，跑通“登录-上传-发布-浏览日记”用户主线。
- 补充 P0 联调测试记录和必要的测试用例文档。
- 在 P0 主线稳定后，再进入 P1：Food、Admin、SearchService、Route 多目标、Diary 检索与评分。

#### 6.5 当前阻塞 / 风险
- 前端工程和页面联调状态仍需确认。
- 当前实库仅有最小测试目的地数据，后续演示需要更完整的目的地、地图节点、设施和日记样例数据。
- P1 能力尚未实现，包括 Food、Admin、SearchService、Diary 评分 / 检索、Route 多目标 / 最短时间。
- 本地数据库密码依赖环境变量配置，团队成员需要各自确认 `DB_USERNAME`、`DB_PASSWORD`、`DB_URL`。

#### 6.6 需要同步更新的文档
- 已同步：`docs/04_api/api-changelog.md`、`docs/04_api/api-spec.md`、`docs/05_modules/diary-module.md`。
- 后续如补充测试记录，应同步：`docs/06_testing/test-cases.md`、`docs/06_testing/test-report.md`。

---

### [第3次更新] 2026-05-05

#### 6.1 本阶段目标
- 在无法提供前端工程代码的情况下，先完成 P0 后端实库联调测试记录收口，并按 `coding-plan.md` 进入 P1 Food 基础版。

#### 6.2 已完成
- 已明确前端页面联调当前为外部阻塞项，不将页面联调写成已完成。
- 已将 P0 Diary 后端实库联调沉淀为可复现测试记录：登录、当前用户、文件上传、文件访问、日记发布、列表、详情、目的地相关日记均通过。
- 已确认下一阶段不进入 AI / SearchService / Admin 完整后台，优先按 P1 顺序处理 Food 基础版。
- 已完成 Food 基础版后端实现：`food` 建表 SQL、实体、Mapper、查询条件、VO、Service、Controller 和单元测试。
- 已执行后端测试：`mvn -q "-Dtest=FoodServiceTests,QueryServiceTests" test` 与 `mvn test` 均通过。
- 已完成 Food 实库验证：导入 `food` 表，插入 3 条最小测试数据，验证推荐、菜系过滤、搜索和非法排序。
- 已完成 Admin 最小后台基础版：目的地、设施、美食的列表 / 新增 / 修改 / 删除接口，以及管理员权限校验。
- 已完成 Admin 后端实库验证：普通用户访问 Admin 返回 403，管理员可维护目的地、设施和美食。
- 已执行后端全量测试：`mvn test`，117 个测试通过。

#### 6.3 进行中
- Admin 后续范围评估：地图节点 / 边维护、用户状态、日记状态、导入入口是否继续补齐。

#### 6.4 下一步计划
- 后续补 Admin 后续范围或按 `coding-plan.md` 进入 SearchService。
- 当前仍不处理前端页面联调，直到前端工程代码可用。

#### 6.5 当前阻塞 / 风险
- 前端工程代码暂不可用，无法验证页面请求、页面展示、路由和状态管理。
- 当前实库演示数据仍偏少，Food、Route、Facility 的完整演示需要补充目的地、设施、美食、地图节点和地图边数据。
- Admin 当前只覆盖目的地、设施、美食三类基础维护；地图节点 / 边、用户状态、日记状态和导入入口尚未实现。

#### 6.6 需要同步更新的文档
- 已同步 / 待同步：`docs/06_testing/test-cases.md`、`docs/06_testing/test-report.md`、`docs/04_api/api-spec.md`、`docs/04_api/api-changelog.md`、`docs/05_modules/food-module.md`、`docs/05_modules/admin-module.md`。

---

### [第1次更新] 2026-05-05

#### 6.1 本阶段目标
- 从文档收口切换到工程骨架与基础环境阶段，先保证后端最小工程可验证。

#### 6.2 已完成
- 已建立后端 Spring Boot 3 工程骨架。
- 已建立统一返回体 `ApiResponse`、基础错误码 `ErrorCode`、业务异常与全局异常处理。
- 已新增 `GET /api/v1/health` 健康检查接口，并同步到 API 文档和接口变更记录。
- 已补充项目内 Maven settings，使依赖缓存优先落在后端工程目录内，避免依赖本机全局 Maven 仓库权限。
- 已执行后端 `mvn test`，当前 1 个 Spring 上下文测试通过。
- 已执行 `mvn package -DskipTests`，生成可运行 Spring Boot jar。
- 已通过 `http://localhost:8080/api/v1/health` 验证健康检查接口，返回 `success=true` 且 `data.status=ok`。

#### 6.3 进行中
- 数据库初始化脚本和 P0 基础表落地准备。
- 前端 Vue 3 工程骨架准备。

#### 6.4 下一步计划
- 在后续后端改动后持续执行 `mvn test` 验证。
- 根据 `schema.md` 生成 P0 初始化 SQL。
- 初始化前端 Vue 3 + Vite + Element Plus 工程骨架。

#### 6.5 当前阻塞 / 风险
- 全局 Maven 配置指向本机不可写目录；当前已用项目内 `.mvn/settings.xml` 绕开，但团队其他成员仍需注意本机 Maven 仓库权限。
- 当前 `frontend/` 目录尚未发现可运行工程文件。

#### 6.6 需要同步更新的文档
- 已同步：`docs/04_api/api-spec.md`、`docs/04_api/api-changelog.md`。
- 后续如落地数据库 SQL，需同步：`docs/03_data/schema.md`、`docs/03_data/data-dictionary.md`、`docs/03_data/er-model.md`。

---

## 7. 周度更新模板

### [第X次更新] YYYY-MM-DD

#### 7.1 本阶段目标
- 待填写

#### 7.2 已完成
- 待填写

#### 7.3 进行中
- 待填写

#### 7.4 下一步计划
- 待填写

#### 7.5 当前阻塞 / 风险
- 待填写

#### 7.6 需要同步更新的文档
- 待填写

---

## 8. 状态建议
任务或阶段状态建议统一使用以下几类：
- `未开始`
- `进行中`
- `待确认`
- `已完成`
- `已暂停`

## 9. 填写建议
1. “已完成”写结果，不写空泛过程。
2. “进行中”写当前实际推进中的内容，不要写所有想做的事。
3. “下一步计划”数量不宜过多，尽量保持聚焦。
4. “阻塞问题”写清楚原因和影响，必要时同步到 `risk-log.md`。
5. 如进度涉及数据库、接口、模块职责变化，应明确提醒同步相关文档。

## 10. 更新时机
建议在以下场景更新：
- 每周例会后
- 一个阶段完成后
- 需求 / 架构 / 数据设计发生明显变化后
- 核心模块开始或结束时
- 联调或测试进入新阶段时
