# 后端联调任务清单（性能优化）

> 整理日期：2026-06-11  
> 前端联系人：智游行 Web 联调  
> 后端环境：`http://10.122.248.221:8080`（dev 代理）

本文档列出**必须由后端配合**的接口改造项。前端已做临时兜底（减请求、超时、缓存、骨架屏），但以下项落地后，白屏、长时间加载、`socket hang up` 等问题可根治。

---

## P0 — 本周优先

### 1. 美食全局 Feed 接口（替代 48 次探测）

**现状问题**

- 美食页进入时需对 `destinationId=1..48` 逐个调用 `GET /api/v1/foods/recommend?pageSize=1` 探测哪些目的地挂了美食。
- 终端大量出现：`http proxy error ... /api/v1/foods/recommend ... socket hang up`。
- 首屏需等待全部探测 + 多页串行拉取后才渲染，体验极差。

**期望接口**

```
GET /api/v1/foods/feed?pageNum=1&pageSize=24&sortBy=heat
```

**响应示例**

```json
{
  "success": true,
  "data": {
    "list": [
      {
        "id": 101,
        "destinationId": 3,
        "name": "护国寺小吃",
        "shopName": "护国寺小吃店",
        "foodType": "京味",
        "coverUrl": "https://...",
        "heatScore": 92,
        "ratingScore": 4.6,
        "priceLevel": "人均 ¥35",
        "tags": ["小吃", "老字号"]
      }
    ],
    "pageNum": 1,
    "pageSize": 24,
    "total": 186,
    "pages": 8
  }
}
```

**要求**

- 跨目的地聚合美食，按热度/评分排序分页返回。
- `pageSize` 支持 24（或明确文档上限）。
- 单请求 P95 响应时间 < 2s。
- 列表项需带 `destinationId`，便于前端跳转/筛选。

---

### 2. 修复 `foods/recommend` 稳定性

**现状问题**

- 同一接口频繁 `socket hang up` / 连接被重置，疑似连接池耗尽、超时未配置或单线程阻塞。

**请排查**

- Tomcat / Netty 连接超时、`keep-alive` 配置。
- 数据库慢查询（按 `destinationId` + `sortBy` 索引）。
- 是否存在未捕获异常导致连接中断。
- 并发 10+ 请求时是否稳定。

**验收**

- 连续 50 次 `GET /api/v1/foods/recommend?destinationId=3&pageNum=1&pageSize=24&sortBy=heat` 成功率 100%，无 hang up。

---

### 3. 目的地接口返回经纬度

**现状问题**

- 路径规划「目的地搜索」拿到 API 数据后，前端需对最多 24 条并行调用高德地理编码补全坐标，拖慢首屏。

**涉及接口**

- `GET /api/v1/destinations/search`
- `GET /api/v1/destinations/recommend`

**期望字段（DestinationVO 扩展）**

```json
{
  "id": 101,
  "name": "故宫博物院",
  "city": "北京",
  "lng": 116.397026,
  "lat": 39.918058,
  "coverUrl": "...",
  "description": "..."
}
```

**要求**

- `lng` / `lat` 为 WGS84 或 GCJ-02（文档注明坐标系，与高德一致则用 GCJ-02）。
- 北京市内景点覆盖率 > 95%。

---

## P1 — 次优先

### 4. 社群 Feed 分页 + 列表轻量化

**现状问题**

- `GET /api/v1/diaries?pageSize=30` 一次返回 30 条**全文** `contentText`，payload 大、解析慢。
- 前端已改为 `pageSize=20`，但仍需完整正文才能开详情。

**期望**

```
GET /api/v1/diaries?sortBy=latest&pageNum=1&pageSize=20
```

**列表项仅返回摘要**

| 字段 | 说明 |
|------|------|
| `id` | 日记 ID |
| `title` | 标题 |
| `excerpt` | 正文摘要（前 120 字，后端截断） |
| `username` | 作者 |
| `destinationName` | 关联目的地 |
| `heatScore` | 热度 |
| `coverUrl` | 手账封面（可从 content 元数据解析后单独字段返回） |
| `mediaList` | 最多 3 条预览图 |

**详情仍走**

```
GET /api/v1/diaries/{id}
```

返回完整 `contentText` + 全部 `mediaList`。

---

### 5. 推荐目的地分页规范

**现状问题**

- 前端首屏 `pageSize=16`，但需确认后端实际上限与性能。
- 偶发重复 `id` 导致前端去重后「无新增」但仍有多页。

**请确认/修复**

- `pageSize` 最大支持值（建议 ≥ 24）。
- `total` / `pages` 与 `list` 一致性。
- 同一 `pageNum` 不重复返回已出现过的 id（或文档说明允许重复时的语义）。

---

### 6. 关键词搜索美食接口（可选但强烈建议）

**现状问题**

- 美食关键词搜索需先拉目的地列表，再对每个目的地并行搜美食，请求链路过长。

**期望**

```
GET /api/v1/foods/search?keyword=烤鸭&pageNum=1&pageSize=24
```

跨目的地搜索，按相关度/热度排序。

---

## P2 — 体验增强

### 7. 统一 API 超时与错误体

**期望**

- 服务端超时 10–15s 内返回结构化错误，避免连接挂死。
- 错误响应保持现有 envelope：`{ success, code, message, data }`。

### 8. 文件上传与日记接口

- `POST /api/v1/files/upload`：大文件上传进度/超时说明。
- 日记 `getBookDetail` 类接口若存在后端版，确认分页加载 entries，避免一次返回整本书所有块。

---

## 联调验收清单（后端同学自测）

- [ ] `foods/feed` 首屏 1 次请求即可展示 24 条美食
- [ ] `foods/recommend` 50 次连续请求无 hang up
- [ ] 目的地 search/recommend 返回 `lng`/`lat`，前端无需高德 geocode
- [ ] 社群列表 `excerpt` 字段可用，详情接口返回全文
- [ ] 所有分页接口 `total`/`pages` 正确

---

## 前端已完成的临时优化（供参考）

| 模块 | 前端兜底 |
|------|----------|
| HTTP | 12s 超时 + 友好错误提示 |
| 美食页 | 探测范围 48→20、session 缓存锚点、首屏只拉第 1 页后先渲染 |
| 目的地搜索 | 保留网格不遮挡、去掉客户端 geocode、本地坐标兜底 |
| 旅游推荐 | pageSize 48→16、骨架屏、延迟自动翻页 |
| 社群 | 拆分 feed/发布 loading、详情优先用 feed 已有数据 |
| 日记 | 素材首批 9 张、导航 hover 预加载 chunk |

后端接口就绪后告知前端，可删除探测逻辑并接入 `foods/feed` 等新接口。
