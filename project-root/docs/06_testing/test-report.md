- 作用：总结测试结果。

# 测试报告（Test Report）

## 1. 文档用途
本文件用于记录个性化旅游系统项目在当前测试阶段的测试执行情况、结果统计、缺陷情况、风险分析和阶段结论，为项目验收、答辩展示和后续回归测试提供依据。

本文件关注的是：
- 本阶段实际测了什么
- 执行了多少测试用例
- 通过了多少，失败了多少
- 发现了哪些主要问题
- 当前版本是否达到阶段性可交付标准

相关文档：
- `project-root/docs/06_testing/test-plan.md`
- `project-root/docs/06_testing/test-cases.md`
- `project-root/docs/06_testing/bug-log.md`
- `project-root/docs/00_project/progress.md`

---

## 2. 当前版本说明

### 2.0 2026-06-11 评论基础版

- 实现目的地、美食、日记三类一级评论的发布和公开分页列表，以及统一软删除接口。
- 单元测试覆盖内容 trim/长度、Handler 分发、用户与管理员删除权限和非法类型。
- MySQL 8 HTTP 测试验证三类发布、目的地列表、私有日记拒绝、越权删除、管理员隐藏和隐藏后列表过滤。
- 定向执行 `CommentServiceTests`、`CommentIntegrationTests` 均通过。
- 后端全量 `mvn -q test` 共 31 个测试报告、223 个测试，0 失败、0 错误、0 跳过。
- 当前不包含楼中楼、点赞、图片、审核流、热门排序、评论搜索或主表评论数聚合。

### 2.0A 2026-06-10 Diary 评分闭环

- 新增 `diary_rating` 明细表、联合唯一约束和 `diary.rating_count`。
- 评分提交采用事务和日记行锁，同一用户重复评分更新原记录。
- 实现 POST 评分和 GET 当前用户评分接口，两个接口均验证未登录返回 401。
- MySQL 实库测试完成作者自评、重复覆盖、双用户 AVG/COUNT 聚合、列表评分排序和真实 JWT HTTP 成功链路验证。
- 迁移脚本连续执行两次成功；执行前数据库备份位于系统临时目录。
- 后端全量回归共 28 个测试套件、211 个测试，0 失败、0 错误、0 跳过。

### 2.0 2026-06-08 演示数据导入前准备

- 已补 Facility `address/tel/cover_url` 和 Food `lng/lat` 的代码映射、初始化结构与导入字段。
- 2026-06-08 当时只新增三类评论表结构；评论业务 API 已于 2026-06-11 补齐基础版。
- 已新增现有数据库迁移 SQL 和用户/偏好/日记/评论幂等演示数据 SQL。
- 已备份 `tour_system`，实库迁移连续执行两次成功；确认 Facility 三个新列、Food 两个新列和三张评论表存在。
- 首次全量测试因实库尚未迁移而在 Food 查询时报 `Unknown column 'lng'`；执行迁移后重跑 200 个测试，0 失败、0 错误、0 跳过。
- 当前实库不存在“北京邮电大学沙河校区”和对应美食，演示内容脚本已增加前置校验，因此尚未导入用户、偏好、日记和评论，也尚未执行本批日记的压缩回填。

### 2.1 文档定位
当前版本为**初版测试报告模板**，用于后续测试执行后填入真实结果。

### 2.2 当前适用场景
本模板适用于以下阶段：
1. 单模块测试完成后的小结
2. 前后端联调完成后的阶段测试报告
3. 验收前的综合测试报告
4. 缺陷修复后的回归测试报告

### 2.3 使用原则
1. 本文件记录的是“已经实际执行的测试结果”，不是计划。
2. 没有实际执行的测试项，不应写成“已通过”。
3. 报告中的统计数据应与 `test-cases.md` 和 `bug-log.md` 保持一致。
4. 每轮较大的测试后都建议保留一个测试报告版本，便于回溯。

---

## 3. 文档信息

- 项目名称：个性化旅游系统的设计与实现
- 文档名称：测试报告（Test Report）
- 当前版本：V1.19
- 当前状态：已追加 CompressionEngine 历史回填、完整性校验、损坏修复和维护统计测试，前端联调阻塞说明已补充
- 测试阶段：后端实库联调测试 / 前端联调待恢复
- 报告日期：2026-06-07
- 报告编写人：Codex

---

## 4. 测试背景

本轮测试的背景说明如下：

- 当前项目阶段：P0 后端核心业务基础版已完成，进入 P0 联调与测试阶段  
- 本轮测试目标：验证 `Auth -> File -> Diary` 后端实库链路是否可复现跑通  
- 本轮测试范围：Auth 登录与当前用户、FileService diary 图片上传与访问、Diary 发布 / 列表 / 详情 / 目的地相关日记  
- 本轮测试对应版本：P0 后端基础版  
- 本轮测试是否属于回归测试：是，包含 `/files/**` 静态资源访问修复后的回归验证
- 前端联调状态：当前无法提供前端工程代码，因此本报告不记录任何页面联调通过结论，前端相关测试统一视为待恢复 / 阻塞。
- 追加开发验证：P1 Food 基础版已执行后端单元测试、实库接口测试和全量后端测试。
- 追加实库验证：P1 Admin 最小后台已完成管理员权限、目的地、设施、美食维护接口验证。
- 追加回归验证：2026-05-06 执行后端 `mvn test`，132 个测试执行，0 失败，17 个跳过，构建成功。
- 追加 ImportService 验证：2026-05-06 执行 `mvn -q -Dtest=ImportServiceTests test`，验证 CSV / JSON 元信息校验、预览解析、必填警告和目的地导入成功摘要。
- 追加 ImportService 后全量回归：2026-05-06 执行后端 `mvn test`，133 个测试执行，0 失败，17 个跳过，构建成功。
- 追加 ImportService 实库验证：2026-05-06 导入 `init-p1-import-schema.sql`，以管理员 token 调用 multipart 预览和执行接口，验证 `destination` 成功入库、`import_batch` 批次记录和 `import_failure` 失败明细记录。
- 追加 ImportService BOM 回归：2026-05-06 执行 `mvn -q -Dtest=ImportServiceTests test`，验证带 UTF-8 BOM 的 CSV 表头不会再导致必填字段匹配失败。
- 追加 BOM 修复后全量回归：2026-05-06 执行后端 `mvn test`，134 个测试执行，0 失败，构建成功。
- 追加 ImportService 查询接口验证：2026-05-06 执行 `mvn -q -Dtest=AdminServiceTests test`，验证导入批次列表、失败明细列表和不存在批次错误处理。
- 追加 ImportService 查询后全量回归：2026-05-06 执行后端 `mvn test`，137 个测试执行，0 失败，构建成功。
- 追加 Auth / Security 实库验证：2026-05-06 启动后端连接 MySQL 8 实库，使用临时普通用户和临时管理员账号验证登录、`/auth/me`、管理员接口访问、无 token、伪造 token 和错误认证头格式。
- 追加 Admin 场所管理验证：2026-05-06 补齐 `GET/POST/PUT/DELETE /api/v1/admin/places` 后端实现，执行后端 `mvn test`，144 个测试执行，0 失败，构建成功。
- 追加 Admin 场所管理实库验证：2026-05-06 启动后端连接 MySQL 8 实库，使用临时管理员和普通用户验证场所列表、新增、查询、修改、无引用删除、设施引用删除保护、地图节点引用删除保护和 Admin 权限拦截；验证结束后已清理临时用户、场所、设施和地图节点数据。
- 追加 Route 多目标后端验证：2026-05-07 新增 `POST /api/v1/routes/plan/multi` 基础版，采用最近邻启发式 + 分段 Dijkstra + 路径拼接，执行后端 `mvn test`，151 个测试执行，0 失败，构建成功。
- 追加 Route 多目标实库验证：2026-05-07 启动后端连接 MySQL 8 实库，使用临时普通用户和临时目的地 / 地图节点 / 地图边验证 `/api/v1/routes/plan/multi` 成功路径、返回起点、历史记录落库、无 token、重复目标和不可达目标；验证结束后临时用户、目的地、地图节点、地图边和路线历史均已清理。
- 追加 Route 最短时间策略后端验证：2026-05-07 在 `MapService` 中复用有向带权图邻接表和 Dijkstra，按 `strategyType` 在距离权重与时间权重间切换，执行后端 `mvn test`，155 个测试执行，0 失败，构建成功。
- 追加 Route 最短时间策略实库验证：2026-05-07 启动后端连接 MySQL 8 实库，使用临时普通用户和临时目的地 / 地图节点 / 地图边分别请求 `shortest_distance` 与 `shortest_time`，确认两种策略返回不同路径且 `route_history` 正确落库；验证结束后临时数据均已清理。
- 追加 Diary 检索排序后端验证：2026-05-07 为标题检索和正文检索补充 `sortBy=latest/heat/rating` 白名单排序，执行后端 `mvn test`，157 个测试执行，0 失败，构建成功。
- 追加 P0 主线后端接口演示预检：2026-05-07 启动后端 jar 连接 MySQL 8 实库，用同一个临时普通用户 token 依次验证登录、推荐、搜索、单目标路线、文件上传、日记发布、日记列表、日记详情和目的地相关日记；验证 `route_history` 与 `diary_media` 落库正确；验证结束后临时用户、目的地、地图节点、地图边、路线历史和日记数据均已清理。
- 追加 GraphEngine 抽取后 Route / Facility 实库回归：2026-06-03 重新打包并启动后端连接 MySQL 8 实库，使用临时普通用户 token 验证 `shortest_distance`、`shortest_time`、多目标路线和 `/api/v1/facilities/nearby`；确认 GraphEngine 抽取后接口语义和 `route_history` 落库行为保持一致，验证结束后临时数据已清理。
- 追加 Route 历史查询后端与实库回归：2026-06-12 实现 `GET /api/v1/routes/history` 与 `GET /api/v1/routes/history/{id}`，单元测试覆盖当前用户分页、摘要名称、完整路径 JSON 恢复、多目标顺序、旧记录兼容、损坏快照、越权隐藏和查询不调用 `MapService`；MockMvc 覆盖两个接口无 token 返回 `AUTH_003`。停服后已完成 MySQL 增量迁移和真实 HTTP 验证，列表、详情、用户隔离、分页顺序、多目标顺序快照和查询不新增历史均通过。执行 `mvn -q test`，36 个测试套件、256 个测试通过。
- 追加 IndexEngine 实库 HTTP 回归：2026-06-07 执行 `mvn -q -Dtest=IndexEngineDatabaseIntegrationTests test`，通过随机端口真实 HTTP 入口比较索引重建与失效状态下的目的地搜索、美食名称/店铺名搜索、日记标题精确/前缀/非前缀包含检索；10 组请求的列表、顺序和分页对象完全一致，临时数据已清理并恢复索引。
- 追加 IndexEngine 后全量回归：2026-06-07 执行 `mvn -q test`，23 个测试套件、171 个测试执行，0 失败、0 错误、0 跳过。
- 追加 IndexEngine 第二阶段回归：2026-06-07 为 `Diary.contentText` 增加字符位置倒排索引，验证中文连续子串、英文大小写、索引 MISS、目的地过滤、排序和分页；实库日志确认索引启用时 SQL 使用候选 `id IN`，索引失效时使用 `content_text LIKE`。
- 追加第二阶段全量回归：2026-06-07 执行 `mvn -q test`，23 个测试套件、178 个测试执行，0 失败、0 错误、0 跳过。
- 追加 `DIARY_CONTENT` 增量维护回归：2026-06-07 验证单文档新增、替换、删除、Unicode、索引不可用保持、事务提交/回滚、异常失效降级及公开/私有/禁用/启用业务分支。
- 追加增量维护全量回归：2026-06-07 执行 `mvn -q test`，23 个测试套件、186 个测试执行，0 失败、0 错误、0 跳过。
- 追加 CompressionEngine 专项回归：2026-06-07 验证 ASCII、中文、Emoji、辅助平面 Unicode、空文本、单字符、10000 字正文、确定性编码、压缩效果和损坏数据拒绝。
- 追加 Diary 压缩接入回归：2026-06-07 验证发布时同时保存原文和自描述 Huffman 压缩包，并验证压缩异常时原文仍可正常保存。
- 追加 CompressionEngine 后全量回归：2026-06-07 执行 `mvn -q test`，24 个测试套件、194 个测试执行，0 失败、0 错误、0 跳过。
- 追加 CompressionEngine 第二阶段维护回归：2026-06-07 验证主键游标分批回填、已有压缩包校验、损坏或内容不一致修复、并发正文保护及单条失败继续处理。
- 追加第二阶段全量回归：2026-06-07 执行 `mvn -q test`，25 个测试套件、198 个测试执行，0 失败、0 错误、0 跳过。
- 追加 CompressionEngine 第二阶段实库验证：2026-06-07 在 MySQL 8 中对 3 篇历史日记执行批大小为 2 的回填，扫描 3 条、回填 3 条、失败 0；随后开启已有数据校验，3 条全部解压并与原文一致，修复 0、失败 0。

### 4.1 IndexEngine 回归明细

- 测试数据：临时用户 1 条、目的地 4 条、美食 4 条、公开日记 4 条，名称使用随机唯一前缀，避免与既有数据混淆。
- 索引启用状态：分别重建 `DESTINATION_NAME`、`FOOD_NAME`、`FOOD_SHOP_NAME`、`DIARY_TITLE`，并断言 Hash 精确查询和 Trie 前缀查询能召回预期 ID。
- 索引失效状态：依次调用 `invalidate(namespace)`，断言四个 namespace 均不可用，再以完全相同参数重复 HTTP 请求。
- 比较口径：忽略顶层动态时间戳，完整比较 `data.list`、列表顺序、`pageNum`、`pageSize`、`total`、`pages`。
- 特别结论：标题中间包含关键词但不满足 Trie 前缀条件的日记仍由 MySQL `LIKE` 召回，说明索引接入未改变原有包含匹配语义。
- 首次执行曾因测试将通用包含结果误判为仅 Trie 前缀结果而失败；修正预期后专项测试通过，该问题不属于业务缺陷。
- 第二阶段新增 `DIARY_CONTENT`：索引 HIT 时使用候选 ID；MISS 时直接返回空分页；UNAVAILABLE 时使用 LIKE。
- 正文实库场景覆盖中文连续关键词、英文大小写、跨目的地过滤、热度分页和无结果查询；索引启用与失效时完整 `data` 对象一致。
- 增量维护采用事务提交后回调：提交前和回滚后索引保持原快照，提交成功后才执行单文档 upsert/remove。
- 索引未构建或已失效时，单文档操作不会创建部分索引；维护异常时 namespace 失效，后续查询继续走 LIKE 兜底。

### 4.2 CompressionEngine 回归明细

- 数据结构：Unicode 码点频次 Map、优先队列、Huffman 二叉树、码点编码 Map 和位流字节数组。
- 压缩包包含 magic、版本、原文字节长度、码点数量、频次表、有效位数、压缩位流和 CRC32，可独立完成解码和完整性校验。
- 多语言正文、空文本、单字符文本和 10000 字日记上限均可无损还原。
- 相同正文重复压缩生成相同二进制结果；长重复文本的完整压缩包小于 UTF-8 原文字节。
- magic、版本、截断数据和 CRC32 被篡改时均拒绝解码。
- Diary 发布正常压缩时同时保存 `content_text` 与 `content_compressed`；压缩异常时仅将压缩字段留空，不回滚原文发布。
- `content_compressed` 配置为默认查询不加载，现有列表、详情、全文检索和 API 字段没有变化。
- 历史维护使用递增日记 ID 游标分页，不使用深 Offset，也不一次加载全表。
- 默认 `trip.compression.backfill-enabled=false`；启用后应用启动执行一次维护，不新增 Controller。
- 可选校验已有压缩包；解码失败或解压内容与原文不一致时，使用原文重新压缩。
- 回填与修复更新均校验正文仍未变化；单条失败记录日记 ID 和异常类型后继续处理。
- 维护结果包含扫描、回填、校验、修复、跳过、失败、原文字节、压缩字节和压缩率。
- 本次实库样例原文共 253 个 UTF-8 字节，完整自描述压缩包共 1067 字节，压缩率为 4.2174；短正文因独立频次表和完整性元数据产生膨胀，符合当前实现预期，不应将 Huffman 副本视为所有短文本的空间优化。

示例写法：
> 本轮测试针对系统的核心主线功能展开，重点覆盖登录、目的地推荐、单目标路径规划、周边设施查询、图文日记发布与浏览等功能，用于验证当前版本是否具备基本联调和演示条件。

---

## 5. 测试目标回顾

本轮测试主要验证以下内容：

1. 核心主线功能是否可用  
2. 前后端联调是否打通  
3. 数据库存储与读取是否正确  
4. 核心算法结果是否符合预期  
5. 文件上传与媒体访问是否正常  
6. 权限校验和异常处理是否符合项目规范  
7. 当前版本是否达到进入下一阶段的条件  

---

## 6. 测试范围

## 6.1 本轮实际覆盖范围
请在执行后填写本轮实际测试覆盖的模块：

- Auth
- Recommend
- Route
- Facility
- Food
- Diary
- Admin
- Import
- FileService
- Security
- Frontend
- AI（若有）

### 本轮重点测试模块
- Auth
- Recommend
- Route 单目标
- FileService
- Diary
- Destination 最小前置数据
- Food
- Admin 最小后台
- SearchService 基础版
- ImportService 完整化基础版
- ImportService 实库 multipart 导入验证
- Route 多目标路径规划基础版
- Route 最短时间策略基础版

### 本轮未覆盖模块
- Recommend 完整筛选矩阵
- Route 单目标 / 多目标前端页面联调
- Facility 附近设施查询
- UserPreference
- Import
- AI
- Frontend 页面联调

### 本轮未覆盖原因
- 本轮目标是把 Diary 图文发布依赖的最短后端链路写成可复现测试记录，不覆盖全部 P0 / P1 / P2 模块。
- 前端工程代码暂不可用，无法执行 Auth、File、Diary 页面级联调。

---

## 6.2 本轮不在范围内的内容
本轮未纳入测试范围的内容如下：

- 前端页面联调。
- Diary 私有日记访问边界、路线记录关联、评分、我的日记列表。
- Recommend、Route、Facility 的完整业务联调。
- AI 等 P2 能力。
- 前端路由、页面表单、上传组件、Pinia token 状态和 Axios 拦截器等页面联调内容。

示例：
- 多人旅游规划协商功能尚未实现，因此未纳入本轮测试
- 室内导航示例尚未完成，因此未纳入本轮测试

---

## 7. 测试环境

## 7.1 软件环境
- 前端：Vue 3 + Vite + Element Plus
- 后端：Java 17 + Spring Boot 3 + MyBatis-Plus
- 数据库：MySQL 8
- 数据处理：Python + pandas
- 版本管理：Git + GitHub

## 7.2 执行环境
- 测试环境：本地实库联调环境
- 浏览器：未使用，接口通过 PowerShell / curl 验证
- JDK 版本：Java 17
- MySQL 版本：MySQL 8.0.46
- 操作系统：Windows

## 7.3 测试工具
- 接口测试工具：PowerShell `Invoke-RestMethod`、`curl.exe`
- 数据库查看工具：MySQL 8 客户端
- 缺陷记录方式：`bug-log.md`

---

## 8. 测试数据说明

本轮测试使用的数据包括：

### 8.1 基础测试数据
- 测试用户数量：1
- 样例目的地数量：1
- 场所 / 建筑物数量：待填写
- 设施数量：待填写
- 地图节点数量：待填写
- 地图边数量：待填写
- 样例日记数量：至少 1 条本轮运行时生成日记
- 样例美食数量：待填写

### 8.2 数据来源说明
- `destinationId=1` 使用本地实库最小测试目的地。
- 测试用户使用 `p0diarytest`。
- 日记和上传图片由本轮联调运行时生成。

### 8.3 数据限制说明
- 当前仅覆盖 Diary 主链路所需的最小数据，不代表完整演示数据规模。

---

## 9. 测试执行概况

## 9.1 用例执行统计

| 指标 | 数量 |
|---|---:|
| 测试用例总数 | 38 |
| 已执行用例数 | 38 |
| 通过数 | 38 |
| 失败数 | 0 |
| 阻塞数 | 0 |
| 未执行数 | 0 |
| 通过率 | 100% |

### 通过率计算公式
`通过率 = 通过数 / 已执行用例数 × 100%`

---

## 9.2 按优先级统计

| 优先级 | 用例数 | 已执行 | 通过 | 失败 | 阻塞 |
|---|---:|---:|---:|---:|---:|
| P0 | 13 | 13 | 13 | 0 | 0 |
| P1 | 24 | 24 | 24 | 0 | 0 |
| P2 | 0 | 0 | 0 | 0 | 0 |

---

## 9.3 按模块统计

| 模块 | 用例数 | 已执行 | 通过 | 失败 | 备注 |
|---|---:|---:|---:|---:|---|
| Auth | 5 | 5 | 5 | 0 | 登录、当前用户、无 token、伪造 token、错误认证头 |
| Recommend | 2 | 2 | 2 | 0 | 推荐列表、目的地关键字搜索 |
| Route | 11 | 11 | 11 | 0 | 多目标最近邻、返回起点、最短距离 / 最短时间策略切换、路线历史落库、重复目标、目标超限、不可达、无 token |
| Facility | 待填写 | 待填写 | 待填写 | 待填写 | |
| Food | 4 | 4 | 4 | 0 | 推荐、搜索、菜系过滤、评分排序 |
| Diary | 7 | 7 | 7 | 0 | 发布、列表、详情、目的地相关日记、标题检索、正文检索、检索排序 |
| Admin | 6 | 6 | 6 | 0 | 权限校验、目的地/场所/设施/美食维护 |
| Import / FileService | 5 | 5 | 5 | 0 | 上传与文件 URL 访问、导入解析和摘要 |
| SearchService | 9 | 9 | 9 | 0 | 标题检索、正文检索、检索排序、分页上限、非法参数 |
| Security | 4 | 4 | 4 | 0 | 管理员权限、普通用户越权、401/403 统一错误结构 |
| P0 主线后端接口预检 | 1 | 1 | 1 | 0 | 同一 token 串联登录、推荐、搜索、单目标路线、文件上传、日记发布 / 查看 |

---

## 10. 主要测试结果说明

## 10.1 Auth 模块测试结果
- 使用测试用户 `p0diarytest` 登录成功，返回 `SUCCESS` 和 JWT token。
- 携带 Bearer token 请求当前用户接口成功，返回当前用户 `username=p0diarytest`。
- 2026-05-06 追加实库接口认证专项验证：
  - 使用临时普通用户 `auth_user_20260506220129` 登录成功，`GET /api/v1/auth/me` 返回 `SUCCESS`、`role=user`。
  - 使用临时管理员 `auth_admin_20260506220129` 登录成功，访问 `GET /api/v1/admin/users?pageNum=1&pageSize=10` 返回 `SUCCESS`。
  - 不携带 token 请求 `GET /api/v1/auth/me` 返回 HTTP 401，错误码 `AUTH_003`。
  - 携带伪造 Bearer token 请求 `GET /api/v1/auth/me` 返回 HTTP 401，错误码 `AUTH_004`。
  - 携带非 Bearer 认证头请求 `GET /api/v1/auth/me` 返回 HTTP 401，错误码 `AUTH_003`。

建议从以下角度描述：
- 注册是否正常
- 登录是否正常
- token 校验是否正常
- 当前用户接口是否正常
- 管理员与普通用户角色区分是否正确

---

## 10.2 Recommend 模块测试结果
- 2026-05-07 P0 主线后端接口演示预检中，临时插入 1 条目的地，热度 `99.00`、评分 `4.90`。
- 携带同一个登录 token 调用 `GET /api/v1/destinations/recommend?sortBy=heat&pageNum=1&pageSize=10&topK=5` 返回 `SUCCESS`，结果包含本次临时目的地。
- 调用 `GET /api/v1/destinations/search?keyword=<临时目的地名>&sortBy=rating&pageNum=1&pageSize=10` 返回 `SUCCESS`，搜索结果命中本次临时目的地。
- 本轮只验证推荐列表和关键字搜索的 P0 演示链路，未覆盖所有筛选组合。

建议描述：
- 推荐列表是否可返回
- 搜索和筛选是否正确
- 热度 / 评分排序是否符合预期
- 空结果时是否处理正确

---

## 10.3 Route 模块测试结果
- P0 单目标路线后端接口演示预检已完成：
  - 临时图数据为 3 个节点 A / B / C，边为 `A->B=120`、`B->C=80`、`A->C=260`。
  - 携带同一个临时普通用户 token 调用 `POST /api/v1/routes/plan/single`，`strategyType=shortest_distance`，返回路径总距离 `200.00`、`estimatedTime=3`。
  - 已根据接口返回 `historyId=7` 查询 `route_history`，确认用户、目的地、策略、交通方式和总距离落库正确。
  - 验证结束后临时 `route_history`、`map_edge`、`map_node`、`destination` 和 `user` 均已清理。
- Route 多目标基础版已完成后端单元测试：
  - `POST /api/v1/routes/plan/multi` 已接入 `RouteController` 和 `RouteService`，会写入 `route_history`。
  - `MapService` 使用有向带权图邻接表和 Dijkstra 分段求最短路，再用最近邻启发式决定多目标访问顺序。
  - 已覆盖多目标最近邻拼接、返回起点、重复目标拦截、目标数量超过 8 个拦截、不可达目标返回 `ROUTE_003`。
  - 当时已支持 `shortest_distance` 与 `shortest_time`；交通工具约束已于 2026-06-11 后续版本补齐。
  - 当前未执行前端页面联调。
- Route 多目标实库接口验证已完成：
  - 临时图数据：4 个节点 A / B / C / D，边为 `A->B=100`、`B->C=120`、`C->A=140`、`A->C=500`，D 为不可达目标。
  - 携带临时普通用户 token 调用 `POST /api/v1/routes/plan/multi`，`returnToStart=true` 时返回路径 `A -> B -> C -> A`，总距离 `360.00`。
  - 根据接口返回 `historyId` 查询 `route_history`，`user_id`、`destination_id`、`start_node_id`、`end_node_id`、`strategy_type=shortest_distance`、`transport_type=walk`、`total_distance=360.00` 和路径 JSON 均校验通过。
  - `returnToStart=false` 时接口返回成功，最终路径终点为 C。
  - 不带 token 调用返回 HTTP 401；重复目标返回 HTTP 400、`COMMON_001`；不可达目标返回 HTTP 422、`ROUTE_003`。
  - 验证结束后临时 `route_history`、`map_edge`、`map_node`、`destination` 和 `user` 清理剩余 0。
- Route 最短时间策略实库接口验证已完成：
  - 临时图数据：3 个节点 A / B / C，边为 `A->B=100`、`B->C=100`、`A->C=300`；前两段理想速度较低，直达边理想速度较高。
  - 携带临时普通用户 token 调用 `POST /api/v1/routes/plan/single`，`strategyType=shortest_distance` 时返回路径 `A -> B -> C`，总距离 `200.00`，`estimatedTime=3`。
  - 同一组起终点使用 `strategyType=shortest_time` 时返回路径 `A -> C`，总距离 `300.00`，`estimatedTime=3`，说明时间权重生效且路径可与最短距离不同。
  - 已根据接口返回的 `historyId` 查询 `route_history`，确认两条历史记录分别保存 `strategy_type=shortest_distance` 与 `strategy_type=shortest_time`，对应总距离和预计时间与接口返回一致。
  - 不带 token 调用单目标规划返回 HTTP 401，错误码 `AUTH_003`。
  - 验证结束后临时 `route_history`、`map_edge`、`map_node`、`destination` 和 `user` 清理剩余 0。

建议描述：
- 单目标路径规划是否正确
- 起点等于终点场景是否正确
- 不可达场景是否处理正确
- 返回的距离和路径节点是否合理

---

## 10.4 Facility 模块测试结果
- GraphEngine 抽取后 Route / Facility 实库回归已完成：
  - 重新执行 `mvn -q package -DskipTests`，确认新 jar 包含 GraphEngine 抽取后的最新代码。
  - 启动后端连接 MySQL 8 实库，使用临时普通用户登录 token 进行接口调用。
  - 临时图数据：4 个节点 A / B / C / D，边为 `A->B=100`、`B->C=100`、`A->C=300`、`A->D=40`；其中 D 为 `facility` 类型节点并关联临时 toilet 设施。
  - 单目标 `shortest_distance` 返回路径 `A -> B -> C`，总距离 `200.00`，`estimatedTime=3`，并写入 `route_history`。
  - 单目标 `shortest_time` 返回路径 `A -> C`，总距离 `300.00`，`estimatedTime=3`，并写入 `route_history`。
  - 多目标 `returnToStart=false` 返回路径 `A -> B -> C`，总距离 `200.00`，并写入 `route_history`。
  - `GET /api/v1/facilities/nearby` 查询 toilet 设施返回 `reachableDistance=40.00`，`sourceNodeId=A`，`targetNodeId=D`。
  - 同一目的地下查询不存在的 `medical_station` 类型返回空列表且接口为 `SUCCESS`。
  - 已根据接口返回的 3 个 `historyId` 查询 `route_history`，确认 `user_id`、`destination_id`、`strategy_type`、`total_distance`、`estimated_time` 与接口返回一致。
  - 验证结束后临时 `route_history`、`map_edge`、`map_node`、`facility`、`destination` 和 `user` 清理剩余 0。

建议描述：
- 附近设施查询是否正常
- 设施类型过滤是否正确
- 图上可达距离排序是否正确
- 空结果和非法类型是否处理正确

---

## 10.5 Food 模块测试结果
- 已执行 `init-p1-food-schema.sql`，`food` 表创建成功。
- 已插入 3 条最小测试数据：牛肉面、番茄面、鸡腿饭。
- `GET /api/v1/foods/recommend?destinationId=1&sortBy=heat&topK=2` 返回 `SUCCESS`，结果按热度排序。
- `GET /api/v1/foods/recommend?destinationId=1&foodType=面食&sortBy=rating&topK=5` 返回 `SUCCESS`，结果只包含面食并按评分排序。
- `GET /api/v1/foods/search?destinationId=1&keyword=面&sortBy=rating&pageNum=1&pageSize=10` 返回 `SUCCESS`。
- 非法排序 `sortBy=distance` 当前返回 HTTP 400，符合“距离联动后续再补”的设计。
- 当前未执行前端美食页联调。

建议描述：
- 是否能按目的地查询美食
- 菜系过滤是否可用
- 热度 / 评分排序是否正确
- 当前阶段是否只是基础验证

---

## 10.6 Diary 模块测试结果
- 先通过 FileService 上传 diary 图片，获得 `/files/diary/...` URL。
- 使用该 URL 发布公开日记成功，返回 `diaryId=2`。
- 日记列表可查到该日记。
- 日记详情可返回标题、正文和媒体列表，媒体数量为 1。
- 按 `destinationId=1` 查看相关日记时可查到该日记。
- 2026-05-07 P0 主线后端接口演示预检中，使用同一个登录 token 上传最小 png，返回 `/files/diary/20260507/...`；随后发布临时公开图文日记成功，返回 `diaryId=3`；日记列表、日记详情和目的地相关日记接口均可查到该日记，`diary_media` 落库 1 条；验证结束后临时日记和媒体记录已清理。
- P1 SearchService 基础版已完成单元测试，标题检索和正文关键词检索均只返回公开且启用的日记，并已支持 `latest/heat/rating` 排序。
- 本轮未测试评分、我的日记列表、私有日记访问边界。

## 10.6A P0 主线后端接口演示预检结果
- 本轮使用同一个临时普通用户 token 串联验证：登录、推荐、搜索、单目标路线、文件上传、日记发布、日记列表、日记详情和目的地相关日记。
- 后端启动方式：`java -jar target/trip-backend-0.0.1-SNAPSHOT.jar`，健康检查 `GET /api/v1/health` 返回 `SUCCESS`。
- 数据库前置：`user`、`destination`、`map_node`、`map_edge`、`route_history`、`diary`、`diary_media` 表存在。
- 结果：全链路通过，临时数据清理剩余为 0；文档中未记录完整 token、数据库密码或测试口令。
- 说明：本结果只代表后端接口层可演示，不代表前端页面、路由、状态管理或上传组件联调通过。

## 10.6B SearchService 测试结果
- `SearchServiceTests` 覆盖标题检索、正文关键词检索、目的地过滤、`heat/rating` 排序、分页上限和非法参数。
- `DiaryServiceTests` 覆盖 Diary 对 SearchService 的复用，并确认检索结果仍组装为 `DiaryVO`，不会直接返回 Entity。
- 当前检索实现基于 MySQL `LIKE`，未引入额外检索依赖或数据库表结构变化。

建议描述：
- 日记发布是否成功
- 图片上传与媒体关联是否正常
- 日记列表和详情是否可访问
- 按目的地查看日记是否正确
- 评分 / 检索是否已验证

---

## 10.7 Admin / Import 模块测试结果
- Admin 最小后台已完成后端实库接口验证：
  - 普通用户访问 `/api/v1/admin/destinations` 返回 HTTP 403。
  - 管理员登录返回 `SUCCESS`，可访问 `/api/v1/admin/**`。
  - 管理员可新增、修改、下架目的地。
  - 管理员可新增、下架设施。
  - 管理员可新增、查询、删除美食。
- Admin 后续底层维护能力已完成后端单元测试：
  - 场所 / 建筑物可分页查询、新增、修改和删除；删除时会检查设施与地图节点引用，无引用时才执行物理删除。
  - 场所管理已完成实库接口验证：无 token 请求 `GET /api/v1/admin/places` 返回 HTTP 401，普通用户 token 返回 HTTP 403，管理员 token 可执行列表、新增、关键字查询、修改和无引用删除。
  - 场所删除引用保护已完成实库接口验证：被 `facility.place_id` 引用或被 `map_node(node_type=place, ref_id=id)` 引用时，删除接口返回 HTTP 400、错误码 `COMMON_002`。
  - 本次场所实库验证临时创建的用户、场所、设施和地图节点均已清理；中文关键字请求在 PowerShell 验证脚本中出现编码影响，已用 ASCII 测试名复测通过，不作为后端接口缺陷。
  - 地图节点可新增，节点被边引用时拒绝删除。
  - 地图边可新增，跨目的地节点会返回 `ROUTE_009`。
  - 用户状态可按 `0/1` 修改。
  - 日记状态可按 `0/1` 修改，前台 DiaryService 已按 `status=1` 过滤。
- 批量导入预览和执行入口已接入 `ImportService`，当前基础版已支持 CSV / JSON 解析和真实入库。
- ImportService 完整化基础版已完成后端单元测试：
  - Admin 导入接口改为 `multipart/form-data`。
  - Controller 接收 `MultipartFile`，Service 核心基于 `Reader`，便于后续复用其他导入来源。
  - 当前支持 `destination`、`place`、`facility`、`food`、`map_node`、`map_edge` 的 CSV / JSON 最小导入。
  - 当前会写入 `import_batch`，失败行写入 `import_failure`。
  - `ImportServiceTests` 覆盖导入文件校验、预览解析、缺必填字段警告、UTF-8 BOM 表头兼容和 destination 成功入库摘要。
- ImportService 已完成实库 multipart 验证：
  - 已执行 `init-p1-import-schema.sql`，确认 `import_batch` 与 `import_failure` 表存在。
  - 使用管理员 token 调用 `/api/v1/admin/import-batches/preview` 上传 `destinations-nobom.csv`，返回 `PREVIEW_ONLY`、`totalRows=1`，且未写入 `destination` 或 `import_batch`。
  - 使用管理员 token 调用 `/api/v1/admin/import-batches` 上传 `destinations-nobom.csv`，返回 `SUCCESS`、成功 1 行、失败 0 行，`destination` 新增 `P1导入验证目的地无BOM20260506200514`，`import_batch.id=3` 记录成功批次。
  - 上传缺字段 CSV 返回 `FAILED`，`import_failure` 记录第 2 行失败，错误摘要为“导入字段缺失或字段名不匹配”。
  - 已修复 CSV UTF-8 BOM 表头兼容问题，带 BOM 的首列表头会在解析时归一化处理，单元回归测试已通过。
  - 已新增管理端导入批次和失败明细查询接口，单元测试覆盖分页查询、VO 映射和不存在批次返回 `COMMON_003`。

建议描述：
- 管理员访问后台是否正常
- 普通用户是否被拦截
- 批量导入是否成功
- 非法导入文件是否被拦截

---

## 10.8 Security 与异常处理测试结果
- 2026-05-06 已完成实库接口认证专项验证。
- 普通用户 token 访问 `GET /api/v1/admin/users?pageNum=1&pageSize=10` 返回 HTTP 403，错误码 `AUTH_005`。
- 管理员 token 访问同一管理端接口返回 `SUCCESS`，说明 `ROLE_admin` 权限链路可用。
- 无 token、伪造 token、错误认证头均返回统一 `ApiResponse` 错误结构，没有暴露堆栈信息。
- 验证记录未保存完整 JWT token，测试文档中也不记录真实密码。

建议描述：
- 未登录访问是否被正确拦截
- 无效 token 是否被拦截
- 后台接口是否做角色判断
- 参数错误是否返回统一错误结构
- 文件上传限制是否生效

---

## 11. 缺陷统计与分析

## 11.1 缺陷总数统计

| 指标 | 数量 |
|---|---:|
| 本轮新增缺陷数 | 1 |
| 本轮已关闭缺陷数 | 1 |
| 当前未关闭缺陷数 | 0 |

---

## 11.2 按严重程度统计

| 严重程度 | 数量 |
|---|---:|
| S1 | 0 |
| S2 | 1 |
| S3 | 0 |
| S4 | 0 |

---

## 11.3 按优先级统计

| 优先级 | 数量 |
|---|---:|
| P0 | 0 |
| P1 | 1 |
| P2 | 0 |
| P3 | 0 |

---

## 11.4 主要缺陷摘要
请列出本轮最重要的缺陷：

| 缺陷编号 | 所属模块 | 标题 | 严重程度 | 当前状态 |
|---|---|---|---|---|
| BUG-001 | FileService / Diary | 上传文件已落盘但 `/files/diary/...` 访问返回 500 | S2 | 已关闭 |

---

## 11.5 缺陷特点分析
- 本轮缺陷集中在本地文件静态资源映射，不涉及数据库持久化或 Diary 业务入参。
- 修复后通过文件访问和日记详情媒体返回两条回归路径验证。

建议从这些角度分析：
1. 是否主要集中在某一模块  
2. 是否属于前后端字段不一致问题  
3. 是否属于图算法或数据映射问题  
4. 是否属于权限 / 安全校验问题  
5. 是否属于数据准备不充分导致的问题  

---

## 12. 回归测试情况

### 12.1 本轮是否执行回归测试
- 是

### 12.2 回归范围
- `TC-FILE-004`
- `TC-DIARY-006`

### 12.3 回归结果摘要
- 修复静态资源映射后，上传返回的 `/files/diary/...` URL 可访问，HTTP 状态为 200。
- 日记详情接口可返回媒体列表，Diary 图文展示的数据链路已恢复。

可参考写法：
> 本轮对登录、单目标路径规划、日记发布和文件上传相关问题进行了回归测试。主要缺陷已修复，原有问题未再次出现，但 Recommend 模块的部分排序边界场景仍需后续继续验证。

---

## 13. 风险与遗留问题

### 13.1 当前主要风险
- 当前测试数据规模较小，仅覆盖 Diary 主链路最小数据。
- 前端工程代码暂不可用，页面联调当前阻塞，无法验证用户界面层请求与展示。
- Route 多目标和最短时间策略已完成后端单元测试和实库接口验证；Facility、Recommend 仍缺少完整实库接口测试报告。
- Diary 评分、我的日记、手账、AI 仍为 P1 / P2 后续范围；基础检索已完成，增强检索算法仍待后续扩展。
- Admin 当前已补场所管理、地图节点 / 边、用户状态、日记状态、导入入口、导入批次列表和失败明细查询；ImportService 已完成基础解析入库、CSV BOM 兼容修复和实库 multipart 接口验证。

建议从这些角度填写：
1. 某些主线功能虽可运行，但数据量偏小  
2. 多目标路径规划和最短时间策略已完成后端基础版和实库接口验证，但尚未完成前端联调  
3. AI 功能尚未进入稳定测试  
4. 管理端只实现了最小可用版本  
5. 测试覆盖仍偏重主线，增强功能覆盖不足  

### 13.2 当前遗留问题
- 需要补充完整 P0 主线端到端测试，覆盖 Recommend、Route、Facility 和前端页面。
- 需要准备更完整的演示数据，包括目的地、地图节点、地图边、设施和多篇日记。
- 前端工程恢复后，需要补执行 Auth + File + Diary 页面联调，并将页面测试结果单独追加到本报告或形成下一版报告。
- Admin 场所管理已完成后端基础版；导入批次查询 / 失败明细查询已完成后端基础版。

### 13.3 对验收的影响判断
- 不影响当前 Diary 后端基础链路演示。
- 若要进入完整课程验收，还需要继续补前端联调、Route / Facility 实库数据和 P1 增强测试。

建议说明：
- 是否影响当前主线演示
- 是否影响课程验收
- 是否属于可解释的延期项

---

## 14. 当前版本结论

请从以下三类结论中选择并填写：

### 14.1 可进入下一阶段
适用条件：
- P0 主线基本通过
- 高优先级缺陷已收敛
- 当前版本可进入下一轮开发或联调

### 14.2 可演示但需继续修复
适用条件：
- 主线基本可用
- 仍存在部分 P1 / P2 缺陷
- 可用于阶段汇报，但不适合直接作为最终验收版

### 14.3 暂不建议进入下一阶段
适用条件：
- 仍存在阻塞主线的高优先级问题
- 当前版本不稳定
- 需要先完成集中修复

### 当前建议结论
- 后端可进入下一阶段；前端联调待恢复

### 结论说明
- 当前版本已跑通 Diary 图文基础链路，并完成 P1 Food 基础版、Admin 场所管理与后续底层维护能力、SearchService 基础版、ImportService 完整化基础版、Route 多目标路径规划和 Route 最短时间策略；ImportService 已补充实库 multipart 导入验证、CSV BOM 兼容回归测试、导入批次查询和失败明细查询。由于前端工程代码暂不可用，页面联调暂不推进；后端可继续按 `coding-plan.md` 进入后续 P1 增强。

示例写法：
> 当前版本已基本跑通登录、推荐、单目标路径规划、周边设施查询和图文日记发布这条主线，能够用于阶段演示。仍存在部分边界输入处理和后台导入能力不足的问题，建议继续修复后再进入最终验收准备阶段。

---

## 15. 后续建议

### 15.1 短期建议
- 前端工程代码可用后，再推进 Auth + File + Diary 页面联调。
- 补充 Recommend、Route、Facility 的实库测试记录。
- 固化一组可复用的 P0 演示数据。
- 按顺序继续补 Diary 检索与排序、Diary 评分，或补 Facility / Recommend 的实库接口测试记录。

例如：
1. 优先修复所有 P0 缺陷  
2. 补做 Route 不可达场景和 Facility 图上排序的回归测试  
3. 完善日记评分与检索的测试覆盖  

### 15.2 中期建议
- 补充 Food、Admin、SearchService 等 P1 模块实库接口测试。
- 扩展 Diary 评分、检索、我的日记列表等增强能力测试。

例如：
1. 完善 Food 和 Admin 的测试覆盖  
2. 扩展测试数据规模  
3. 补充 Diary 评分与检索实库接口测试  

### 15.3 验收前建议
- 执行完整主线回归测试。
- 整理接口返回、页面截图和关键数据作为答辩材料。

例如：
1. 做一次完整主线回归测试  
2. 整理截图和执行结果作为答辩材料  
3. 补齐最终测试情况说明  

---

## 16. 附录

## 16.1 相关文档引用
- `project-root/docs/06_testing/test-plan.md`
- `project-root/docs/06_testing/test-cases.md`
- `project-root/docs/06_testing/bug-log.md`
- `project-root/docs/05_modules/*`
- `project-root/docs/04_api/api-spec.md`

## 16.2 可附加材料
后续可在此附加：
- 测试截图
- 接口返回示例
- 路线规划结果截图
- 导入结果截图
- 关键 bug 修复前后对比

---

## 17. 与其他文档的关系

本文件应与以下文档保持一致：

- `project-root/docs/06_testing/test-plan.md`
- `project-root/docs/06_testing/test-cases.md`
- `project-root/docs/06_testing/bug-log.md`
- `project-root/docs/00_project/progress.md`
- `project-root/docs/00_project/risk-log.md`

如果测试范围、模块实现范围、缺陷统计口径或阶段结论发生变化，应同步更新本文件。

---

## 18. 后续维护说明

本文件应在以下场景下更新：

1. 完成一轮正式测试后  
2. 完成一轮主要缺陷修复并回归后  
3. 进入中期汇报前  
4. 进入最终验收前  
5. 需要输出最终测试情况说明材料时

## 19. 2026-06-11 路线交通工具策略回归

- 新增道路组合权限、交通工具默认速度、道路限速和拥挤度时间成本测试。
- 验证 walk/bike/cart 会实际改变合法边集合。
- 验证 mixed 可在公共节点切换交通工具，且路径边保留实际工具。
- 验证单目标、多目标、路线历史和 Facility 原有调用均未发生接口路径回归。
- 执行 `mvn -q test`，全量测试通过。
- 未执行 bike/cart/mixed 实库 HTTP 验证：当前数据库道路仍均为 `walk`，需要先准备演示子图。

## 20. 2026-06-12 AIGC 日记照片动画后端测试

- 新增 `AIServiceTests` 和 `AnimationServiceTests`。
- 已验证模板 provider 按图片顺序生成合法脚本、JSON 可序列化、总时长正确。
- 已验证未登录、非作者、无图片、私有日记、正常生成、重复生成和不存在动画等分支。
- 已验证 provider 不能引用当前日记之外的媒体，provider 异常不会执行动画表写入。
- 定向执行 `mvn -q "-Dtest=AIServiceTests,AnimationServiceTests" test` 通过。
- 执行 `mvn -q test`，34 个测试套件、243 个测试，0 失败、0 错误、0 跳过。
- `diary_animation` 实库迁移、HTTP 接口回归和前端播放器尚未执行。

## 21. 2026-06-12 AIGC 多模态 Provider 回归

- 新增 OpenAI-compatible 多模态 Provider，测试通过本机 Mock Server 执行，不访问公网、不使用真实密钥。
- 已验证请求包含配置模型、Bearer 鉴权、日记上下文和本地图片 Base64 data URI。
- 已验证合法严格 JSON 可转换为统一动画脚本，`mediaId` 由模型返回、`fileUrl` 由服务端可信数据回填。
- 已验证非法 JSON、读取超时、HTTP 500、缺少 API Key 时自动降级到 `mock-template`。
- 已验证只允许读取 `/files/diary/...` 日记图片，并校验数量、大小、文件签名和路径边界。
- 已验证 AI 调用前后使用独立短事务，媒体快照变化时不保存过期脚本。
- 执行 `mvn -q test`，35 个测试套件、250 个测试，0 失败、0 错误、0 跳过。
- 尚未执行真实厂商带密钥联调，因此不能把模型兼容性、视觉理解质量、额度和平均响应时间标记为已验证。

## 22. 2026-06-12 Route 路线历史查询实库回归

- 执行 `migrate-route-history-query-schema.sql`，确认
  `route_history.ordered_target_node_json` 已创建为可空 `TEXT`。
- 使用临时普通用户和目的地 10 的现有连通节点 `2 -> 1 -> 3` 完成真实 HTTP 验证。
- 单目标规划写入历史，详情恢复节点 `[2,1]`，旧式单目标的
  `orderedTargetNodeIds` 返回空数组。
- 多目标规划写入历史，详情恢复节点 `[2,1,3]`，接口和数据库均记录实际目标顺序 `[1,3]`。
- 列表使用 `pageNum=1&pageSize=1` 时返回 `total=2`、`pages=2`，首条为最新多目标历史；
  摘要返回起终点名称，不携带路径明细。
- 未登录请求列表返回 HTTP 401、`AUTH_003`；另一临时用户请求历史详情返回
  HTTP 404、`COMMON_003`。
- 执行列表与详情查询前后，当前用户 `route_history` 行数均为 2，确认历史查询不重新规划、
  不新增历史。
- 验证结束后临时用户和路线历史均已清理，实库遗留数量为 0。

## 23. 2026-06-12 日记浏览量即热度回归

- `DiaryMapper.incrementHeatScore` 使用单条 `UPDATE diary SET heat_score = heat_score + 1`，避免并发下先查后写造成计数丢失。
- `DiaryServiceTests` 验证公开日记、私有日记作者、禁用日记、无权访问和原子更新失败分支。
- MySQL 实库测试中，公开日记初始浏览量为 2，连续两次详情响应分别返回 3、4。
- 随后并发执行 8 次原子更新，8 次均更新 1 行，数据库最终浏览量为 12。
- 同一目的地下另一篇日记浏览量为 5；按 `sortBy=heat` 查询时，浏览量 12 的日记实时排在首位。
- 已执行 `migrate-diary-view-heat-schema.sql`，实库字段确认为 `BIGINT UNSIGNED NOT NULL DEFAULT 0`，无空值或负值记录。
- 测试使用的临时用户、目的地和两篇日记已全部清理。
- 执行全量 `mvn -q test`：37 个测试套件、259 个测试，0 失败、0 错误、0 跳过。

## 24. 2026-06-12 按目的地名称查询相关日记回归

- `GET /api/v1/diaries` 已支持 `destinationKeyword`，返回结构和原分页字段不变。
- 单元测试覆盖关键字 trim、无匹配空分页、`destinationId` 与关键字交集，以及 QueryService 索引调用。
- MySQL 8 真实 HTTP 测试覆盖目的地名称前缀两页、中间包含、无匹配、热度和评分排序。
- 分别在 `DESTINATION_NAME` 索引已构建和失效状态请求，完整 `data` 对象递归比较一致。
- 定向执行 `QueryServiceTests,DiaryServiceTests,IndexEngineDatabaseIntegrationTests`，0 失败、0 错误。
- 实库临时用户、目的地、美食和日记数据在测试后已清理，并恢复索引。
