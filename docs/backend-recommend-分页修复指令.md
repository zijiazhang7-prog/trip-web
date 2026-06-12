# 后端修复指令：景点/美食 recommend 展示量过少

> **用途**：投喂后端 Codex / 舍友按此改 `trip-web`  
> **现象**：前端只能看到约 **32** 条景点、约 **27** 条美食；数据库实际有上千景点、数百美食  
> **项目路径**：`trip-web/project-root/backend/`（Spring Boot + MyBatis-Plus）

---

## 1. 背景（前端联调现象）

- 前端首页调用 `GET /api/v1/destinations/recommend?pageSize=32&pageNum=1,2,3...` 做无限滚动，但只能看到约 32 条景点；数据库有上千条 `status=1` 的目的地。
- 前端美食页调用 `GET /api/v1/foods/recommend?destinationId=xx&pageSize=32&pageNum=1`，期望每页 32 条，但接口实际只返回约 10 条（未传 `topK` 时默认 10）；多目的地聚合去重后约 27 条。

---

## 2. 根因（已定位）

### 2.1 景点 `recommendDestinations`

**文件**：`src/main/java/com/trip/service/impl/RecommendServiceImpl.java`

当前问题（约 70–82 行）：

1. `destinationQuery.setPageNum(DEFAULT_PAGE_NUM)` — **忽略客户端 `pageNum`，永远查第 1 页**
2. `destinationQuery.setPageSize(MAX_CANDIDATE_SIZE)` — **候选池硬限 100 条**（`MAX_CANDIDATE_SIZE = 100`）
3. `rankForRecommend(..., limit)` — 只返回 Top `limit` 条（`pageSize=32` 时返回 32 条）
4. 响应 `pageNum` 写死为 `DEFAULT_PAGE_NUM`（1），导致前端翻页拿到重复数据

### 2.2 美食 `recommendFoods`

**文件**：`src/main/java/com/trip/service/impl/FoodServiceImpl.java`  
**DTO**：`src/main/java/com/trip/dto/request/FoodRecommendQuery.java`

当前问题：

1. `FoodRecommendQuery` **没有 `pageNum` / `pageSize` 字段**，前端传的 `pageSize=32` 被 Spring 忽略
2. `int limit = topK(query.getTopK())` — 未传 `topK` 时 **默认 10 条**（`DEFAULT_PAGE_SIZE = 10`）
3. 无分页；`searchFoods` 已有正确分页逻辑（`page()` 方法），但 `recommendFoods` 未复用

---

## 3. 修复目标

让 `/recommend` 两个接口行为与前端无限滚动一致：

- 支持真实 `pageNum` + `pageSize` 分页
- `total` / `pages` 反映全量可推荐数据
- 不同 `pageNum` 返回的 `list` 中 **id 不重复**（除非数据总量不足）
- 保持现有排序语义：景点 `sortBy=heat|rating|recommend`，美食 `sortBy=heat|rating`
- **向后兼容**：保留 `topK`；当 `topK` 有值时，行为仍为 Top-K（`pageNum` 可忽略或固定为 1）

---

## 4. 任务 1：修复景点 recommend

### 4.1 修改文件

- `RecommendServiceImpl.java`（主逻辑）
- `QueryService.java` / `QueryServiceImpl.java`（可选：全量召回）
- `RecommendServiceTests.java`（补测试）

### 4.2 推荐实现（与 `FoodServiceImpl.searchFoods` 对齐）

**思路：全量召回 → 排序 → 内存分页切片**

```java
@Override
public PageResultVO<DestinationVO> recommendDestinations(DestinationRecommendQuery query) {
    DestinationRecommendQuery safeQuery = query == null ? new DestinationRecommendQuery() : query;

    // topK 模式：兼容旧调用
    if (safeQuery.getTopK() != null && safeQuery.getTopK() > 0) {
        int limit = Math.min(safeQuery.getTopK(), MAX_PAGE_SIZE); // MAX_PAGE_SIZE=100
        // 召回全量候选，排序后取 top limit
        ...
        return PageResultVO.of(voList, 1, limit, total, pages);
    }

    // 分页模式（前端主路径）
    int pn = pageNum(safeQuery.getPageNum());
    int ps = pageSize(safeQuery.getPageSize());

    DestinationQuery destinationQuery = new DestinationQuery();
    destinationQuery.setType(normalize(safeQuery.getType()));
    destinationQuery.setTheme(normalize(safeQuery.getTheme()));

    // 关键：召回全部符合条件的数据，不要用 MAX_CANDIDATE_SIZE=100 截断
    List<Destination> candidates = queryAllMatchingDestinations(destinationQuery);
    List<Destination> ranked = rankForRecommend(candidates, safeQuery.getSortBy(), null);
    return paginateDestinations(ranked, pn, ps);
}
```

### 4.3 必须调整的细节

1. 新增私有方法 `paginateDestinations(List<Destination> ranked, int pageNum, int pageSize)`，逻辑同 `FoodServiceImpl.page()`
2. 修改 `rankForRecommend`：当 `topK == null || topK <= 0` 时，调用 `rankService.rankDestinations(candidates, sortBy, null)` 返回**全量排序列表**（不截断）
3. `effectiveLimit` / `pageSize()` 里 **`Math.min(pageSize, MAX_CANDIDATE_SIZE)` 的 100 上限**只用于单页 size，不要用于候选召回池
4. 响应字段：
   - `pageNum` = 客户端传入值
   - `pageSize` = 客户端传入值
   - `total` = 排序后全量条数
   - `pages` = `ceil(total / pageSize)`
5. 若新增 `QueryService.queryAllDestinations()`：
   - 在 `QueryServiceImpl` 中复用现有 `LambdaQueryWrapper` 条件（`status=1`、type、theme 等）
   - 使用 `destinationMapper.selectList(wrapper)`，**不要** `selectPage` 限 100

### 4.4 全量召回方案（任选其一）

| 方案 | 说明 |
|------|------|
| **A（推荐）** | 新增 `queryAllDestinations(DestinationQuery)` 返回 `List<Destination>`（`selectList`） |
| B | 循环分页拉取直到耗尽后合并（不推荐） |
| C | 将 `MAX_PAGE_SIZE` 提高到能覆盖当前库（如 5000），P1 可接受 |

### 4.5 测试（`RecommendServiceTests.java` 新增）

```java
@Test
void recommendShouldPaginateWithoutDuplicateIds() {
    // mock 返回 5 条候选
    // pageNum=1, pageSize=2 → 2 条
    // pageNum=2, pageSize=2 → 另外 2 条，与 page1 不重复
    // total=5, pages=3
}

@Test
void recommendTopKModeShouldStillWork() {
    // topK=2 时只返回 2 条，pageNum 固定 1（保持现有测试通过）
}
```

---

## 5. 任务 2：修复美食 recommend

### 5.1 修改文件

- `FoodRecommendQuery.java` — 增加分页字段
- `FoodServiceImpl.java` — 改 `recommendFoods`
- `FoodServiceTests.java` — 补测试

### 5.2 DTO 修改（`FoodRecommendQuery.java`）

新增字段（参考 `DestinationRecommendQuery` / `FoodSearchQuery`）：

```java
@Min(1)
private Integer pageNum;

@Min(1)
@Max(100)
private Integer pageSize;
```

并生成 getter/setter。

### 5.3 Service 修改（`FoodServiceImpl.java`）

```java
@Override
public PageResultVO<FoodVO> recommendFoods(FoodRecommendQuery query) {
    ...
    List<Food> candidates = queryService.queryFoods(toFoodQuery(query));

    // topK 优先（向后兼容）
    if (query.getTopK() != null && query.getTopK() > 0) {
        int limit = topK(query.getTopK());
        List<Food> top = rankFoods(candidates, query.getSortBy(), limit);
        return PageResultVO.of(toFoodVOs(top), 1, limit, candidates.size(), pages(candidates.size(), limit));
    }

    // 分页模式：pageSize 优先于默认 10
    List<Food> ranked = rankFoods(candidates, query.getSortBy(), null);
    int pn = pageNum(query.getPageNum());
    int ps = pageSize(query.getPageSize());
    return page(ranked, pn, ps); // 复用现有 private page() 方法
}
```

### 5.4 参数优先级（写进注释）

1. `topK` 有值 → Top-K 模式（忽略 `pageNum` / `pageSize`）
2. 否则 → `pageNum` + `pageSize` 分页（**前端传的 `pageSize=32` 必须生效**）
3. `pageSize` 未传 → 默认 10，`pageNum` 未传 → 默认 1

### 5.5 测试（`FoodServiceTests.java` 新增）

```java
@Test
void recommendShouldPaginateByPageSize() {
    // 3 条候选，pageSize=2 pageNum=1 → 2 条；pageNum=2 → 1 条
}

@Test
void recommendShouldUsePageSizeWhenTopKAbsent() {
    // 不传 topK，pageSize=32 → list.size() <= 32（候选足够时等于 32）
    // 验证不再固定返回 10
}
```

---

## 6. 任务 3：可选优化（时间允许再做）

### 6.1 景点候选召回性能

若全量 `selectList` 在数千条时偏慢，可后续改为：

- DB 侧按 `heat_score` / `rating_score` 预排序 + 真分页
- 或 ES / SearchService

**P1 不要求**，当前库规模（约 1000）内存排序可接受。

### 6.2 文档

在 `docs/` 或接口注释标明：

- `/recommend`：支持 `pageNum` + `pageSize` 分页；`topK` 为兼容参数
- `/search`：关键词搜索 + 分页（不变）

---

## 7. 不要改动的部分

- Controller 路径与响应包装 `ApiResponse<PageResultVO<T>>` 不变
- `DestinationSearchQuery` / `FoodSearchQuery` 现有行为不变
- `status=1` 过滤逻辑不变（`QueryServiceImpl`）
- 排序算法（`RankServiceImpl`、`recommendScore` 偏好匹配）不变，只改召回范围与分页切片

---

## 8. 验收标准（必须全部通过）

### 8.1 景点

```http
GET /api/v1/destinations/recommend?sortBy=recommend&pageNum=1&pageSize=32
GET /api/v1/destinations/recommend?sortBy=recommend&pageNum=2&pageSize=32
```

- [ ] page1 与 page2 的 id 集合交集为空（或极小）
- [ ] `total` ≥ 数据库 `destination` 表 `status=1` 的数量级（上千）
- [ ] `pages` = ceil(total / 32)
- [ ] 现有 `RecommendServiceTests` 全部通过

### 8.2 美食

```http
GET /api/v1/foods/recommend?destinationId={id}&sortBy=heat&pageSize=32&pageNum=1
GET /api/v1/foods/recommend?destinationId={id}&sortBy=heat&pageSize=32&pageNum=2
```

- [ ] 传 `pageSize=32` 时第一页最多 32 条（不再固定 10）
- [ ] page2 能拿到下一批
- [ ] 传 `topK=5` 时仍只返回 5 条（兼容）
- [ ] 现有 `FoodServiceTests` 全部通过

### 8.3 构建

```bash
cd project-root/backend
mvn test
mvn package
```

---

## 9. 关键文件清单

| 文件 | 动作 |
|------|------|
| `service/impl/RecommendServiceImpl.java` | 重写 recommend 分页逻辑 |
| `service/impl/QueryServiceImpl.java` | 可选：新增 `queryAllDestinations` |
| `service/QueryService.java` | 可选：接口声明 |
| `service/impl/FoodServiceImpl.java` | recommend 改分页 |
| `dto/request/FoodRecommendQuery.java` | 加 `pageNum` / `pageSize` |
| `test/.../RecommendServiceTests.java` | 补分页测试 |
| `test/.../FoodServiceTests.java` | 补 `pageSize` 测试 |

---

## 10. 提交信息建议

```
fix: recommend 接口支持真分页，修复景点/美食展示量被 Top-K 与 100 候选池限制的问题
```

---

## 11. 前后端分工（联调参考）

| 角色 | 动作 |
|------|------|
| **后端** | 按本文档改 `recommend` 分页；部署联调机 |
| **前端** | 已对齐分页参数（见第 12 节）；后端发版后联调验证 |

前端相关文件（供联调对照）：

- `src/pages/RecommendSection.tsx` — `PAGE_SIZE = 32`
- `src/pages/FoodPage.tsx` — `FOOD_PAGE_SIZE = 32`
- `src/api/food.ts` — `pageNum` + `pageSize`；仅 Top10 显式 `topK`
- `src/api/destination.ts` — `fetchRecommendedDestinationsPage`
- `src/api/pagination.ts` — `inferTotalPages` 优先后端 `pages`

---

## 12. 前端已完成的联调准备（cursor 仓库）

| 改动 | 说明 |
|------|------|
| `api/food.ts` | 美食 recommend 统一传 `pageNum` + `pageSize`；仅 Top10 传 `topK=10` |
| `api/pagination.ts` | `inferTotalPages` 优先使用后端 `pages`；新增 `shouldStopRecommendPagination` |
| `RecommendSection.tsx` | 翻页无新 id 时停止加载，避免旧后端重复页空转 |
| `FoodPage.tsx` | 首屏预取 2 页、后台 8 页；锚点探测 120；单次加载更多 6 页 |

### 联调验收（前端侧）

1. 首页下滑：景点卡片持续增加，`total` 与列表量一致
2. 美食页下滑：每个锚点目的地可翻多页，总量明显大于 27
3. Network：`foods/recommend` 带 `pageSize=32&pageNum=2` 且**无** `topK`（Top10 除外）
