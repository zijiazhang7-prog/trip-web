# 算法重构规划说明

- 状态：Planning
- 日期：2026-05-24
- 作用：记录下一阶段算法层重构方向，避免公共能力层和业务模块继续混写算法细节。
- 范围：仅规划 `IndexEngine`、`GraphEngine`、`CompressionEngine` 以及 `Rank / Search / Diary` 的算法归属调整。

---

## 1. 文档用途

本文件用于说明当前代码库中算法实现的现状、目标拆分方式、迁移顺序和风险控制。

本文件只描述下一阶段重构计划，不表示对应代码已经全部实现。实际代码状态仍应以后端源码和测试结果为准。

---

## 2. 当前代码现状

根据当前后端代码和既有文档，算法能力现状如下：

| 算法点 | 当前主要位置 | 当前状态 | 主要问题 |
| --- | --- | --- | --- |
| Top-K 排序 | `RankServiceImpl` | 已有基于 `PriorityQueue` 的 Top-K 能力 | 需要继续推动 Diary / Search 等模块复用 |
| 图建模与 Dijkstra | `MapServiceImpl` | 已支撑路线规划和设施可达距离 | 图结构、Dijkstra、路径回溯与业务编排混在同一服务中 |
| 最短时间策略 | `MapServiceImpl` / Route 链路 | 已作为路线策略补充 | 需要抽象为边权策略，而不是散落在业务层 |
| 文本检索 | `SearchServiceImpl` / `IndexEngine` / Mapper | 名称与标题使用 Hash / Trie 辅助，日记正文使用字符位置倒排索引 | 索引不可用时保留 Mapper `LIKE` 兜底 |
| 日记压缩 | 已完成 Huffman 压缩及历史数据维护闭环 | 已实现 | 保持 `diary.content_text` 为主数据，压缩副本默认不参与业务读取 |

---

## 3. 重构目标

### 3.1 总体目标

1. 将纯数据结构与算法能力下沉到基础引擎层；
2. 保持现有 Controller、DTO、VO、Service 对外接口尽量不变；
3. 让业务服务只负责业务编排、参数校验、权限上下文和结果组装；
4. 让核心算法便于单元测试、复杂度分析和课程答辩说明；
5. 所有重构都必须分步完成，不一次性重写系统。

### 3.2 目标边界

| 模块 | 应负责 | 不应负责 |
| --- | --- | --- |
| `IndexEngine` | Hash 精确查找、Trie 前缀匹配、日记正文字符位置倒排索引 | 数据库访问、VO 组装、权限校验 |
| `QueryService` | 业务查询调度，决定用索引或 Mapper 兜底 | 自己维护复杂索引结构 |
| `SearchService` | 文本搜索调度，决定用倒排索引或 `LIKE` 兜底 | 查完全部后自行全量排序 |
| `Mapper` | 访问 MySQL | 承担算法逻辑 |
| `RankService` | 排序、Top-K、综合评分 | 业务数据持久化 |
| `GraphEngine` | 图结构、Dijkstra、单源最短路、路径回溯、边权策略 | 查数据库、认识 VO、保存路线历史 |
| `MapService` | 加载地图数据、构建图输入、选择路径策略、转换业务结果 | 直接承载大量 Dijkstra 细节 |
| `RouteService` | 接收路线请求、校验参数、保存路线历史、组装返回 | 直接实现图算法 |
| `FacilityService` | 查设施、筛选类别、组装设施结果、按可达距离排序 | 直接维护邻接表或实现 Dijkstra |
| `CompressionEngine` | Huffman 编码、解码、压缩元数据生成 | 修改日记业务语义、替代原文展示逻辑 |

---

## 4. IndexEngine 规划

### 4.1 职责

`IndexEngine` 作为检索类数据结构的基础能力，当前已分三阶段实现：

- Hash 精确查找；
- Trie 前缀匹配；
- 日记正文字符位置倒排索引；
- 日记正文单文档增量新增、替换和删除；
- 支持索引未构建、未命中或条件不适用时回退到 Mapper 查询。

### 4.2 数据结构

| 数据结构 | 用途 | 示例 |
| --- | --- | --- |
| `Map<String, Set<Long>>` | 名称、类别、标签等精确索引 | 名称到目的地 ID 集合 |
| Trie | 前缀匹配 | 目的地名称、场所名称、日记标题前缀 |
| `Map<CodePoint, Map<Long, List<Position>>>` 字符位置倒排索引 | 日记正文连续子串检索 | Unicode 字符到日记 ID 和出现位置 |

### 4.3 调用流程

```text
QueryService / SearchService
↓
判断索引是否已构建、查询条件是否适合索引
↓
优先调用 IndexEngine
↓
如果索引未命中、索引未构建、或需要模糊兜底
↓
调用 Mapper 执行 MySQL 条件查询 / LIKE
↓
必要时交给 RankService 排序或 Top-K
```

### 4.4 算法思想与复杂度

- Hash 精确查找：通过哈希表把关键字段映射到 ID 集合，平均查询复杂度为 `O(1 + r)`，`r` 为结果数量。
- Trie 前缀匹配：按字符逐层匹配前缀，复杂度为 `O(m + r)`，`m` 为前缀长度。
- 字符位置倒排索引：将 Unicode 字符映射到文档 ID 和位置列表，通过连续位置校验保持 `%keyword%` 子串语义；构建时间和空间复杂度均为 `O(C)`。当前实现对首字符候选位置逐一验证，并对后续字符位置执行二分查找，查询上界约为 `O(P1 * m * log L)`，其中 `P1` 为首字符候选位置数，`m` 为关键词长度，`L` 为单个位置列表长度。

### 4.6 第二阶段已实现状态

- 新增 `DIARY_CONTENT` namespace，只构建倒排索引，不为长正文重复构建 Hash / Trie。
- `SearchService` 在索引 `HIT` 时按候选 ID 查询数据库，由数据库继续负责公开状态、目的地过滤、排序和分页。
- 索引 `MISS` 表示已构建索引中不存在连续关键词，直接返回空分页。
- 索引 `UNAVAILABLE` 时回退 `content_text LIKE`，保证启动预热失败或写操作失效后的可用性。
- 日记发布和后台日记状态修改在事务提交后增量维护 `DIARY_CONTENT`；公开且启用时执行单文档 upsert，私有或禁用时执行 remove。
- `DIARY_TITLE` 本阶段仍在事务提交后整 namespace 失效。
- 索引原本不可用时，单文档操作不会创建不完整索引；增量维护异常时使 namespace 失效，由 SearchService 回退 Mapper `LIKE`。

### 4.7 增量维护数据结构与复杂度

- `InvertedIndex` 额外保存“文档 ID -> 归一化 Unicode 码点序列”，用于定位替换或删除时需清理的旧 posting。
- 更新采用写时复制：复制索引快照、文档映射及受影响字符的 posting 子表，再由 `AtomicReference` 原子替换。
- 正文扫描本身：新增为 `O(Cnew)`，删除为 `O(Cold)`，替换为 `O(Cold + Cnew)`。
- 由于当前使用 Java 标准集合实现不可变快照，实际更新时间还包含外层快照、文档映射和受影响 posting 子表的复制开销；该实现避免扫描全部日记正文，但不是严格的持久化集合 `O(C)` 更新。
- 适用范围：单体部署、课程设计和中小规模日记数据。多实例部署仍需额外的跨实例同步机制。

### 4.5 适用范围

适合名称、标题、类别、标签、短文本关键词等查询；不适合复杂 SQL 组合条件、强事务一致性查询和权限敏感过滤。复杂条件仍应由 Mapper 兜底。

---

## 5. GraphEngine 规划

### 5.1 职责

`GraphEngine` 只负责纯图结构与图算法：

- 有向带权图建模；
- 邻接表维护；
- Dijkstra 最短距离；
- Dijkstra 最短时间；
- 单源最短路距离映射；
- 前驱记录和路径回溯；
- 距离 / 时间等边权策略。

### 5.2 数据结构

| 数据结构 | 用途 |
| --- | --- |
| `Map<Long, GraphNode>` | 节点表 |
| `Map<Long, List<GraphEdge>>` | 邻接表 |
| `PriorityQueue` | Dijkstra 每轮取当前最短候选节点 |
| `Map<Long, Double>` | 起点到各节点的距离或时间 |
| `Map<Long, Long>` | 前驱节点，用于路径回溯 |

### 5.3 MapService 目标职责

`MapService` 仍作为地图公共能力门面：

- 从 `map_node`、`map_edge` 加载业务数据；
- 根据 `destinationId` 构建图输入；
- 选择 `shortest_distance`、`shortest_time` 等策略；
- 调用 `GraphEngine`；
- 将算法结果转换为 Route / Facility 可用的业务结果。

### 5.4 算法思想与复杂度

- 图采用邻接表保存，适合稀疏图；
- Dijkstra 使用优先队列，时间复杂度为 `O((V + E) log V)`；
- 单源最短路一次计算可复用到设施可达距离查询；
- 路径回溯复杂度为 `O(P)`，`P` 为路径节点数。

### 5.5 适用范围

适合非负权有向图的最短距离、最短时间和设施可达距离查询。不适合存在负权边的图；多目标路径在第一阶段仍可由 `MapService` 编排多次单源最短路完成。

---

## 6. CompressionEngine 实现

### 6.1 职责

`CompressionEngine` 已用于日记正文无损压缩，第一阶段采用基于 Unicode 码点的 Huffman 编码。

### 6.2 存储原则

为降低业务风险，第一阶段复用已有 `diary.content_compressed` 字段保存自描述压缩包，同时保留 `diary.content_text` 原文不变。

当前实现：

- `diary.content_text`：继续保存原文，保证现有发布、详情、检索接口不被破坏；
- `diary.content_compressed`：保存 magic、版本、原文字节长度、Unicode 码点数量、频次表、有效位数、压缩位流和 CRC32；
- 默认查询不读取压缩 BLOB，详情仍返回原文字段；
- 压缩失败时保留原文并允许日记发布成功，压缩字段为空。
- 第二阶段新增按日记主键游标分批执行的历史数据回填；
- 可选校验已有压缩包，发现损坏、版本错误或与原文不一致时，从原文重新压缩修复；
- 回填默认关闭，只能通过配置显式启用，不新增管理接口；
- 条件更新同时校验日记 ID 和正文，避免覆盖并发修改后的数据；
- 任务输出扫描、回填、校验、修复、失败及压缩率统计，不记录正文内容。

### 6.3 数据结构

| 数据结构 | 用途 |
| --- | --- |
| `Map<Character, Integer>` | 字符频次统计 |
| `PriorityQueue<HuffmanNode>` | 构建 Huffman 树 |
| Huffman Tree | 表示最优前缀编码结构 |
| `Map<Character, String>` | 字符到编码的映射 |
| Bit Stream / Byte Array | 保存压缩后的二进制数据 |

### 6.4 算法思想与复杂度

Huffman 编码根据字符出现频率构建最优前缀编码，高频字符使用短编码，低频字符使用长编码，从而实现无损压缩。

- 频次统计复杂度：`O(n)`；
- 构建 Huffman 树复杂度：`O(k log k)`，`k` 为字符种类数；
- 编码复杂度：`O(n)`；
- 解码复杂度：`O(b)`，`b` 为压缩比特流长度。

### 6.5 适用范围

适合课程设计中展示无损压缩数据结构与算法能力。对短文本压缩率可能不明显，因此不应作为 P0 主线阻塞项。

---

## 7. Rank / Diary / Search 调整规划

### 7.1 RankService

当前 `RankServiceImpl` 已经具备基于 `PriorityQueue` 的 Top-K 能力。后续应优先扩大复用范围，而不是重写为全量排序。

适用方式：

```text
候选列表
↓
小顶堆维护 Top-K
↓
局部排序输出
```

复杂度：候选数为 `n`、返回数量为 `k` 时，Top-K 复杂度约为 `O(n log k)`，优于全量排序 `O(n log n)`。

### 7.2 DiaryService

`DiaryService` 应主要负责日记发布、列表、详情、目的地关联、权限上下文和返回组装。后续涉及热度、评分或综合排序时，应尽量复用 `RankService`。

需要注意：数据库分页场景下，直接由 SQL 做基础时间排序仍然合理；只有业务层候选集排序和 Top-K 才应优先抽到 `RankService`。

### 7.3 SearchService

`SearchService` 后续应从“直接 LIKE 查询”升级为：

```text
关键词
↓
IndexEngine 召回候选 ID
↓
Mapper 批量查询对象
↓
RankService 排序 / Top-K
↓
索引不可用时 Mapper LIKE 兜底
```

这样可以兼顾课程算法展示和当前系统稳定性。

---

## 8. 分阶段实施顺序

### 阶段 A：文档同步

- 新增本规划文档；
- 同步 `module-map.md`、`dependency-map.md`、`ADR-004-algorithm-choice.md`；
- 明确哪些是当前实现，哪些是下一阶段规划。

### 阶段 B：GraphEngine 抽取

- 先从 `MapServiceImpl` 中抽出纯图结构、Dijkstra、路径回溯；
- 保持 `MapService` 对外接口不变；
- Route、Facility 接口不改路径、不改请求响应字段；
- 增加 GraphEngine 单元测试。

### 阶段 C：IndexEngine 最小版

- 先支持目的地、场所名称的 Hash 精确查找和 Trie 前缀匹配；
- QueryService / SearchService 接入索引优先、Mapper 兜底；
- 不改现有接口契约。

### 阶段 D：Search / Diary 复用 RankService

- SearchService 避免查完全部后全量排序；
- DiaryService 的候选集排序逐步复用 RankService；
- 保留数据库分页排序作为基础列表能力。

### 阶段 E：CompressionEngine（已完成第一阶段）

- 复用已有 `diary.content_compressed`，不新增表结构；
- 实现 Huffman 编码 / 解码；
- 不替换 `diary.content_text` 原文字段；
- 增加压缩率、还原一致性、Unicode、单字符和损坏数据测试。

### 阶段 F：CompressionEngine 维护闭环（已完成第二阶段）

- 使用主键游标分批回填历史日记压缩副本；
- 通过默认关闭的配置开关控制启动时执行；
- 可选验证已有压缩包并修复损坏或不一致数据；
- 保持原文为可信主数据，不改变详情和检索读取链路；
- 汇总压缩率和维护结果，不新增 API 或数据库结构。

---

## 9. 验收标准

1. 重构后现有 Controller 路径和 DTO/VO 字段不发生不必要变化；
2. Route、Facility 的实库接口结果与重构前语义一致；
3. GraphEngine 有独立单元测试覆盖最短距离、最短时间、不可达、路径回溯；
4. IndexEngine 有独立单元测试覆盖精确查找、前缀匹配、未命中和兜底；
5. CompressionEngine 有独立测试证明压缩后可无损还原；
6. ADR 和模块文档能清楚说明算法归属、复杂度和适用范围。

---

## 10. 风险与控制

| 风险 | 影响 | 控制方式 |
| --- | --- | --- |
| 抽取 GraphEngine 时改变路线结果语义 | Route / Facility 联调受影响 | 先保留 MapService 接口不变，增加回归测试 |
| IndexEngine 数据与数据库不一致 | 查询结果缺失或过期 | 第一阶段仅做只读快照索引，并保留 Mapper 兜底 |
| SearchService 接入索引后结果顺序变化 | 前端展示和测试预期变化 | 排序统一交给 RankService，并记录语义变化 |
| Huffman 压缩影响日记详情读取 | 日记主线不稳定 | 原文字段保持不变，压缩 BLOB 默认不参与查询，压缩失败时降级为空 |
| 算法重构范围过大 | 影响后续模块进度 | 按 GraphEngine、IndexEngine、CompressionEngine 分阶段推进 |

---

## 11. 与其他文档的关系

本文件应与以下文档保持一致：

- `project-root/docs/02_architecture/module-map.md`
- `project-root/docs/02_architecture/dependency-map.md`
- `project-root/docs/09_decisions/ADR-004-algorithm-choice.md`
- `project-root/docs/05_modules/recommend-module.md`
- `project-root/docs/05_modules/route-module.md`
- `project-root/docs/05_modules/diary-module.md`

如果后续实际实现与本规划不一致，应优先更新本文件和 ADR，再进入代码调整。
