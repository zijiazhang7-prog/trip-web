# 后端任务说明：标签体系（taxonomy）与个性化推荐

> **用途**：投喂后端 Codex / 舍友按此改 `trip-web`  
> **配置文件**：`docs/taxonomy.json`（本仓库）→ 复制到 `backend/src/main/resources/taxonomy.json`  
> **原则**：**第一版不改数据库表**；用现有 `category`、`tag_json`、`food_type` + 映射表运行时打标  
> **前端**：同期换 UI 标签与排序逻辑（见本文 §8）  
> **项目路径**：`trip-web/project-root/backend/`（Spring Boot 3 + MyBatis-Plus，**Java 17**）

---

## 0. AI 执行约束（必读，避免返工）

1. **技术栈**：Java **17**、Spring Boot、现有 `RankService` / `QueryService` 架构不变。
2. **P0 不改数据库**：不 `ALTER TABLE`，不删列，不迁移数据。
3. **不删除现有 API 字段**：保留 `theme`、`preferThemeList`、`foodType`（单值）等，只做扩展与兼容。
4. **新增类统一放在** `com.trip.taxonomy` 包：
   - `TagJsonParser`（公共 parseTags）
   - `TaxonomyProperties` / `TaxonomyService`
   - `ResolvedDestinationTags`、`UserTagSelection`（record）
5. **`parseTags` 只保留一份实现**：`RecommendServiceImpl` 与 `DiaryRecommendServiceImpl` 内各有一份私有方法，**改为调用** `TagJsonParser.parse()`，删除重复私有方法。
6. **分页逻辑不要改回旧版**：
   - 景点 `recommendDestinations`：保持 `queryAllDestinations` 全量召回 → `rankForRecommend` → `paginateDestinations` 内存分页（已修复，见 `backend-recommend-分页修复指令.md`）。
   - 美食 `recommendFoods`：保持 `queryFoods` 召回 → `rankFoods` → `page()` 内存分页。
7. **实体风格**：项目实体为 **普通 JavaBean**（getter/setter）；新增 DTO 可用 **Java 17 `record`**。
8. **未登录用户**：请求参数带了 `destType` / `interestTags` / `cuisineTags` 时，**仍要加权排序**，不得仅依赖 `currentPreferenceThemes()`。
9. **标签筛选语义**：匹配项排前、未匹配项**仍返回**（不 `filter` 掉），与前端一致。
10. **实现顺序**：先 `TagJsonParser` + `TaxonomyService` + 单测，再改 `RecommendServiceImpl` / `FoodServiceImpl`。

---

## 1. 要解决的问题

| 现象 | 原因 |
|------|------|
| 选「古镇」无结果 | UI 标签与 DB `category`（历史古迹、城市公园…）不对齐 |
| 选「川菜」筛不到美食 | DB `food_type` 是「美食老字号、烘烤类…」，不是川菜粤菜 |
| 推荐偏好不生效 | `preferThemeList` 用「人文建筑型」等，与前端兴趣标签不一致 |
| 兴趣匹配不准 | `tag_json` CSV 为 `A\|B\|C`，`parseTags` 只认 JSON 数组 |

---

## 2. 交付物

1. **`taxonomy.json`** — 标准标签 + 映射（已写好，见 `docs/taxonomy.json`）
2. **`com.trip.taxonomy.*`** — `TagJsonParser`、`TaxonomyService`、类型 record
3. **修 `parseTags`** — 支持管道符 `|`，两处调用改公共类
4. **扩展 recommend / search** — 按标准标签加权；美食 `cuisineTags` 反查 `food_type`
5. **扩展用户偏好** — 兼容旧 `preferThemeList`，支持新兴趣标签

**不改表**：不新增 `destination` / `food` 列；第二版可选迁移（§11）。

---

## 3. 配置文件说明（taxonomy.json）

### 3.1 三套 UI 标准标签

| 键 | 数量 | 用途 |
|----|------|------|
| `destTypes` | 10 | 目的地类型（单选为主） |
| `interestTags` | 12 | 兴趣偏好（多选） |
| `cuisineTags` | 10 | 菜系（多选，仅美食） |

### 3.2 映射关系

| 键 | 方向 | 示例 |
|----|------|------|
| `destTypeByCategory` | DB `category` → `destType` | `历史古迹` → `历史人文` |
| `defaultInterestByCategory` | DB `category` → 默认兴趣[] | `博物馆/展览馆` → `["历史文化","艺术文艺"]` |
| `interestByTagKeyword` | `tag_json` 细标签关键词 → 兴趣 | `亲子互动` → `亲子友好` |
| `cuisineByFoodType` | DB `food_type` → 菜系 | `美食老字号` → `老字号` |
| `foodTypeByCuisine` | **菜系 → DB 原值[]（查询反查）** | `老字号` → `["美食老字号"]` |
| `legacyThemeToInterests` | 旧 `prefer_theme` → 新兴趣 | 兼容已存用户偏好 |

### 3.3 覆盖保证

- 景点 CSV **16 种 `category`** 均在 `destTypeByCategory` 中
- 每种 `category` 在 `defaultInterestByCategory` 至少有 1 个兴趣
- 美食 CSV **10 种 `food_type`** 均在 `cuisineByFoodType` 中

---

## 4. 任务清单（按优先级）

### P0 — 必做（不改表，约 3～5 人天）

#### 4.1 复制配置并加载

```
trip-web/project-root/backend/src/main/resources/taxonomy.json
```

新建 `TaxonomyProperties`（`@ConfigurationProperties` 或启动时读 classpath JSON）+ `TaxonomyService`。

**类与方法签名（必须按 §13 实现，勿自行发明字段名）：**

```java
package com.trip.taxonomy;

public final class TaxonomyService {
  ResolvedDestinationTags resolve(Destination destination);
  String resolveCuisine(Food food);
  List<String> foodTypesForCuisines(List<String> cuisineTags);
  int scoreDestination(ResolvedDestinationTags tags, UserTagSelection selection);
  int scoreFood(String cuisineTag, UserTagSelection selection);
  UserTagSelection mergeSelection(
      DestinationRecommendQuery query,
      UserPreferenceVO preference);  // 请求参数 + 登录偏好合并，见 §14.3
}
```

#### 4.2 修复 `parseTags` — 抽 `TagJsonParser`

| 文件 | 现状 | 改法 |
|------|------|------|
| `RecommendServiceImpl.java` ~224 行 | 私有 `parseTags` | 改为 `TagJsonParser.parse(tagJson, objectMapper)` |
| `DiaryRecommendServiceImpl.java` ~250 行 | 私有 `parseTags` | 同上 |

**`TagJsonParser` 逻辑（§13 有完整类）：** JSON 数组 → `\|` 分割 → 整串兜底。

#### 4.3 景点标签解析算法

```text
destType  = destTypeByCategory.get(category)           // 缺失：log warn，destType=null
interests = HashSet(defaultInterestByCategory[category])
for (tag in TagJsonParser.parse(tagJson)):
    if interestByTagKeyword.containsKey(tag): interests.add(mapped)
return ResolvedDestinationTags(destType, interests, rawTags)
```

#### 4.4 推荐加权 — `RecommendServiceImpl`

**现有方法（勿删，在其上扩展）：**

```java
// 约 169–174 行
private BigDecimal recommendScore(Destination destination, Set<String> preferenceThemes)

// 约 177–193 行
private int matchCount(Destination destination, Set<String> preferenceThemes)
```

**改法：**

1. `rankForRecommend` 中 `sortBy=recommend` 时，将 `preferenceThemes` 换成 `UserTagSelection`（合并请求参数 + 用户偏好）。
2. `matchCount` 改为调用 `taxonomyService.scoreDestination(resolve(d), selection)`，返回值含义不变（整数命中数）。
3. **算分公式见 §14**，权重读 `taxonomy.json` → `scoringWeights`。
4. `sortBy=heat|rating`：**不**套用标签加权，保持 `rankService.rankDestinations` 原逻辑。

**`toDestinationVO` 填充点（约 220–221 行）：**

```java
return DestinationVO.from(destination, TagJsonParser.parse(destination.getTagJson(), objectMapper));
// 扩展：在 from 之后或 VO 工厂内 setDestType / setInterestTags
```

#### 4.5 美食 — `FoodServiceImpl` + `QueryServiceImpl`

**`FoodServiceImpl.recommendFoods`（约 42–61 行）：**

- `toFoodQuery` 增加：若 `query.getCuisineTags()` 非空 → `foodQuery.setFoodTypes(taxonomyService.foodTypesForCuisines(...))`（需在 `FoodQuery` 增加 `List<String> foodTypes` 或 OR 多次 eq）。
- 排序：在 `rankFoods` 前若 `cuisineTags` 非空，用 `sortByScore` + 组合分 `heat + taxonomyScore`（§14.2），未命中仍保留在列表末尾。

**`QueryServiceImpl`（约 224–226 行）：**

```java
// 现状：单值精确匹配
wrapper.eq(Food::getFoodType, foodType);
// 改为：foodTypes 列表非空时 wrapper.in(Food::getFoodType, foodTypes)
```

**禁止**：`wrapper.eq(Food::getFoodType, "老字号")` — UI 名与 DB 原值不同。

#### 4.6 扩展查询参数

**`DestinationRecommendQuery`** 新增：

```java
private String destType;
private List<String> interestTags;
```

**`FoodRecommendQuery`** 新增：

```java
private List<String> cuisineTags;
```

#### 4.7 响应 VO 扩展（建议）

```java
// DestinationVO 新增（普通 JavaBean 字段 + getter/setter）
private String destType;
private List<String> interestTags;

// FoodVO 新增
private String cuisineTag;
```

#### 4.8 用户偏好兼容

**`UserPreferenceVO`** 现有字段（勿删）：

```java
private List<String> preferThemeList;  // 旧：人文建筑型、自然景观型...
private String preferFoodType;         // 旧：单值
```

**读取时**：`legacyThemeToInterests` 把 `preferThemeList` 并入 `UserTagSelection.interestTags`。

**保存时（最小方案）**：`prefer_theme` JSON 逐步改为存标准 `interestTags`；旧数据读取仍兼容。

---

### P1 — 建议做（约 1～2 人天）

| 项 | 说明 |
|----|------|
| 单元测试 | `TaxonomyServiceTest`：T1–T6 + `TagJsonParserTest` 管道格式 |
| `GET /api/v1/taxonomy` | 返回 destTypes / interestTags / cuisineTags |
| 日志 | 未映射的 category / food_type `log.warn` 一次 |

---

### P2 — 可选（改表，约 2～3 人天）

见原 §11 SQL；**P0 不做**。

---

## 5. 关键文件索引

| 文件 | 改动 |
|------|------|
| `src/main/resources/taxonomy.json` | 新增（从本仓库复制） |
| `com/trip/taxonomy/TagJsonParser.java` | **新增** |
| `com/trip/taxonomy/TaxonomyService.java` | **新增** |
| `com/trip/taxonomy/ResolvedDestinationTags.java` | **新增 record** |
| `com/trip/taxonomy/UserTagSelection.java` | **新增 record** |
| `RecommendServiceImpl.java` | 打分 + 去掉私有 parseTags |
| `DiaryRecommendServiceImpl.java` | 改用 TagJsonParser |
| `FoodServiceImpl.java` | cuisine 反查 + 加权排序 |
| `QueryServiceImpl.java` | `food_type IN (...)` |
| `FoodQuery.java` | `List<String> foodTypes` |
| `DestinationRecommendQuery.java` | 新参数 |
| `FoodRecommendQuery.java` | `cuisineTags` |
| `DestinationVO.java` / `FoodVO.java` | 标准标签字段 |
| `UserPreferenceServiceImpl.java` | 新旧 theme 兼容 |

---

## 6. 接口约定（前后端对齐）

### 6.1 景点推荐

```http
GET /api/v1/destinations/recommend?pageNum=1&pageSize=32&sortBy=recommend
    &destType=历史人文
    &interestTags=历史文化&interestTags=艺术文艺
```

### 6.2 美食推荐

```http
GET /api/v1/foods/recommend?destinationId=1&pageNum=1&pageSize=32
    &cuisineTags=老字号&cuisineTags=甜品糖水
```

后端：`cuisineTags` → `food_type IN ('美食老字号','甜品类')`。

### 6.3 用户偏好

```json
{
  "preferDestTypes": ["历史人文"],
  "preferInterestTags": ["历史文化", "艺术文艺"],
  "preferFoodTypes": ["老字号", "甜品糖水"],
  "customPreferenceText": "周末想安静看展"
}
```

---

## 7. 测试用例（验收）

| # | 场景 | 期望 |
|---|------|------|
| T1 | `category=历史古迹` | `destType=历史人文`，`interests` 含 `历史文化` |
| T2 | `tag_json=亲子互动\|网红打卡` | `interests` 含 `亲子友好`、`网红打卡` |
| T3 | `food_type=美食老字号` | `cuisineTag=老字号` |
| T4 | 请求 `cuisineTags=老字号` | SQL 命中 `food_type=美食老字号` |
| T5 | `preferTheme=[人文建筑型]` 旧数据 | 等效 `interestTags` 含 `历史文化`、`艺术文艺` |
| T6 | recommend + `interestTags=历史文化` | 故宫、国博等排序靠前，未命中条目仍返回但靠后 |
| T7 | `sortBy=heat` + `interestTags` | 仅按热度，**不**因兴趣标签改变顺序 |
| T8 | 未登录 + `interestTags=历史文化` | 仍加权排序（不依赖 JWT） |

---

## 8. 前端同期工作（简要）

| 项 | 说明 |
|----|------|
| 替换 `siteData` 三套标签 | 与 `taxonomy.json` 一致 |
| 排序 | 匹配分前置，不隐藏未命中项 |
| DeepSeek | 用户话 → taxonomy 标签 JSON |
| 共享映射 | `import taxonomy.json` 或 `GET /taxonomy` |

---

## 9. AI 辅助建议

| 步骤 | 用法 |
|------|------|
| 生成 `com.trip.taxonomy` 包 | 严格按 §12–§15，不要改分页与表结构 |
| 补单元测试 | T1–T8 |
| 用户意图 → 标签 | **前端 DeepSeek**；后端只消费结构化标签 |

---

## 10. 实施顺序

```text
1. 复制 taxonomy.json → resources
2. TagJsonParser + TaxonomyService + record 类型 + 单测 T1–T5
3. RecommendServiceImpl：mergeSelection + scoreDestination 接入 recommendScore
4. FoodServiceImpl + QueryServiceImpl：cuisineTags 反查 + 排序
5. VO 字段 + Query DTO 扩展
6. 联调前端 + 验收 T1–T8
7. （可选 P2）改表迁移
```

---

## 11. 与现有文档关系

- 分页行为以 `docs/backend-recommend-分页修复指令.md` 为准
- 本文在其之上增加标签标准化与推荐加权，可同批合并 `dev`

---

## 12. 现有代码锚点（贴给 AI，禁止猜测字段名）

### 12.1 `Destination` 实体

路径：`com/trip/entity/Destination.java`

```java
@TableName("destination")
public class Destination {
    private Long id;
    private String name;
    private String type;        // DB: scenic | campus（不是 UI 目的地类型）
    private String category;    // 历史古迹、城市公园、博物馆/展览馆...（String，非枚举）
    private String city;
    private String description;
    private BigDecimal heatScore;
    private BigDecimal ratingScore;
    private String tagJson;     // DB 列 tag_json；"亲子互动|网红打卡" 或 JSON 数组字符串
    private String coverUrl;
    private Integer status;
    private LocalDateTime createdAt;
}
```

### 12.2 `Food` 实体

路径：`com/trip/entity/Food.java`

```java
@TableName("food")
public class Food {
    private Long id;
    private Long destinationId;
    private Long facilityId;
    private String name;        // 菜品名
    private String foodType;    // DB food_type；String 自由文本，非枚举。例：美食老字号、烘烤类
    private String shopName;
    private BigDecimal heatScore;
    private BigDecimal ratingScore;
    // ...
}
```

### 12.3 `RecommendServiceImpl` 现有推荐分

路径：`com/trip/service/impl/RecommendServiceImpl.java`

```java
private static final BigDecimal HEAT_WEIGHT = new BigDecimal("0.6");
private static final BigDecimal RATING_WEIGHT = new BigDecimal("3.0");
private static final BigDecimal PREFERENCE_MATCH_WEIGHT = new BigDecimal("10.0");

private BigDecimal recommendScore(Destination destination, Set<String> preferenceThemes) {
    BigDecimal heatScore = scoreOf(destination.getHeatScore()).multiply(HEAT_WEIGHT);
    BigDecimal ratingScore = scoreOf(destination.getRatingScore()).multiply(RATING_WEIGHT);
    BigDecimal preferenceScore = BigDecimal.valueOf(matchCount(destination, preferenceThemes))
            .multiply(PREFERENCE_MATCH_WEIGHT);
    return heatScore.add(ratingScore).add(preferenceScore);
}
```

**`rankForRecommend` 触发条件（约 129–147 行）：**

- 仅当 `sortBy=recommend` 且 `currentPreferenceThemes()` 非空时走 `recommendScore`
- 否则 `sortBy=heat|rating` 走 `rankService.rankDestinations`

**`recommendDestinations` 分页（约 69–93 行）：**

```java
List<Destination> candidates = queryService.queryAllDestinations(destinationQuery);
List<Destination> ranked = rankForRecommend(candidates, safeQuery.getSortBy(), null);
return paginateDestinations(ranked, requestedPageNum, requestedPageSize);
```

### 12.4 `FoodServiceImpl` 现有美食推荐

路径：`com/trip/service/impl/FoodServiceImpl.java`

```java
List<Food> candidates = queryService.queryFoods(toFoodQuery(query));
List<Food> ranked = rankFoods(candidates, query.getSortBy(), null);
return page(ranked, pageNum(query.getPageNum()), pageSize(query.getPageSize()));
```

`rankFoods` 仅按 `heatScore` 或 `ratingScore` 排序，**尚无偏好加权**。

### 12.5 `UserPreferenceVO` 现有字段

```java
private List<String> preferThemeList;  // 旧主题：人文建筑型、自然景观型...
private String preferFoodType;         // 单值，如 面食
```

---

## 13. 新增类型定义（完整代码，照抄）

### 13.1 `ResolvedDestinationTags`

```java
package com.trip.taxonomy;

import java.util.List;
import java.util.Set;

public record ResolvedDestinationTags(
        String destType,
        Set<String> interests,
        List<String> rawTags
) {
    public static ResolvedDestinationTags empty() {
        return new ResolvedDestinationTags(null, Set.of(), List.of());
    }
}
```

### 13.2 `UserTagSelection`

```java
package com.trip.taxonomy;

import java.util.List;

public record UserTagSelection(
        List<String> destTypes,
        List<String> interestTags,
        List<String> cuisineTags
) {
    public static UserTagSelection empty() {
        return new UserTagSelection(List.of(), List.of(), List.of());
    }

    public boolean isEmpty() {
        return destTypes.isEmpty() && interestTags.isEmpty() && cuisineTags.isEmpty();
    }
}
```

### 13.3 `TagJsonParser`

```java
package com.trip.taxonomy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.springframework.util.StringUtils;

public final class TagJsonParser {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private TagJsonParser() {}

    public static List<String> parse(String tagJson, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(tagJson)) {
            return List.of();
        }
        String trimmed = tagJson.trim();
        try {
            List<String> parsed = objectMapper.readValue(trimmed, STRING_LIST_TYPE);
            if (parsed != null && !parsed.isEmpty()) {
                return parsed.stream().map(String::trim).filter(StringUtils::hasText).toList();
            }
        } catch (JsonProcessingException ignored) {
            // fall through
        }
        if (trimmed.contains("|")) {
            return Arrays.stream(trimmed.split("\\|"))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        }
        return List.of(trimmed);
    }
}
```

---

## 14. 算分公式（精确，接入现有 recommendScore）

### 14.1 景点标签匹配分 `scoreDestination`

权重来自 `taxonomy.json` → `scoringWeights`（默认 `destTypeMatch=3`, `interestMatch=2`）。

```java
public int scoreDestination(ResolvedDestinationTags tags, UserTagSelection sel) {
    if (sel.isEmpty() || tags == null) {
        return 0;
    }
    int score = 0;
    int destTypeWeight = properties.getScoringWeights().getDestTypeMatch();   // 3
    int interestWeight = properties.getScoringWeights().getInterestMatch();   // 2

    if (tags.destType() != null && sel.destTypes().contains(tags.destType())) {
        score += destTypeWeight;
    }
    if (tags.interests() != null && sel.interestTags() != null) {
        for (String interest : tags.interests()) {
            if (sel.interestTags().contains(interest)) {
                score += interestWeight;
            }
        }
    }
    return score;
}
```

**接入 `recommendScore`（替换原 `matchCount` 返回值）：**

```java
private BigDecimal recommendScore(Destination destination, UserTagSelection selection) {
    BigDecimal heatScore = scoreOf(destination.getHeatScore()).multiply(HEAT_WEIGHT);
    BigDecimal ratingScore = scoreOf(destination.getRatingScore()).multiply(RATING_WEIGHT);
    ResolvedDestinationTags tags = taxonomyService.resolve(destination);
    int tagMatch = taxonomyService.scoreDestination(tags, selection);
    BigDecimal preferenceScore = BigDecimal.valueOf(tagMatch).multiply(PREFERENCE_MATCH_WEIGHT);
    return heatScore.add(ratingScore).add(preferenceScore);
}
```

> 说明：`tagMatch` 替代原 `matchCount` 的 int 值，仍乘 `PREFERENCE_MATCH_WEIGHT=10`。热度/评分权重不变。

### 14.2 美食标签匹配分 `scoreFood`

```java
public int scoreFood(String cuisineTag, UserTagSelection sel) {
    if (sel == null || sel.cuisineTags().isEmpty() || cuisineTag == null) {
        return 0;
    }
    return sel.cuisineTags().contains(cuisineTag)
            ? properties.getScoringWeights().getCuisineMatch()  // 2
            : 0;
}
```

**`FoodServiceImpl` 加权排序（有 cuisineTags 时）：**

```java
BigDecimal heat = scoreOf(food.getHeatScore());
int cuisineScore = taxonomyService.scoreFood(taxonomyService.resolveCuisine(food), selection);
BigDecimal total = heat.add(BigDecimal.valueOf(cuisineScore * 10L)); // 系数可与景点一致调参
```

未选 `cuisineTags` 时保持原 `rankFoods` 逻辑。

### 14.3 `mergeSelection` — 请求参数与用户偏好合并

```java
public UserTagSelection mergeSelection(DestinationRecommendQuery query, UserPreferenceVO pref) {
    List<String> destTypes = new ArrayList<>();
    List<String> interests = new ArrayList<>();
    List<String> cuisines = new ArrayList<>();

    if (query != null && StringUtils.hasText(query.getDestType())) {
        destTypes.add(query.getDestType().trim());
    }
    if (query != null && query.getInterestTags() != null) {
        interests.addAll(query.getInterestTags());
    }
    if (pref != null && pref.getPreferThemeList() != null) {
        for (String legacy : pref.getPreferThemeList()) {
            interests.addAll(properties.legacyThemeToInterests(legacy));
        }
    }
    // 去重
    return new UserTagSelection(
            destTypes.stream().distinct().toList(),
            interests.stream().distinct().toList(),
            cuisines.stream().distinct().toList());
}
```

**优先级**：请求参数与偏好 **并集**（不是覆盖）。若并集后 `isEmpty()` 且 `sortBy=recommend`，回退 `sortBy=heat` 行为。

---

## 15. 禁止修改清单

| 禁止项 | 原因 |
|--------|------|
| `ALTER TABLE destination/food`（P0） | 第一版运行时打标 |
| 删除 `theme` / `preferThemeList` / `foodType` 字段 | 向后兼容 |
| 改 `queryAllDestinations` 为 SQL `LIMIT 100` | 会破坏真分页 |
| 用 UI 菜系名直接 `eq(food_type)` | DB 存的是「美食老字号」等原值 |
| 标签不匹配时 `filter` 掉条目 | 产品要求：排后但不隐藏 |
| 在 `sortBy=heat|rating` 时强行套标签分 | 见 T7 |
| 新建第三套 `parseTags` 实现 | 必须只用 `TagJsonParser` |
| 修改 `destination.type` 语义 | 仍为 scenic/campus，与 UI destType 无关 |

---

## 16. 喂 AI 专用摘要（复制到 Prompt 首段）

```text
在 trip-web Spring Boot 后端实现 taxonomy 标签体系（P0 不改表）。
必读：docs/backend-标签体系与个性化推荐任务.md §0、§12–§15。
复制 docs/taxonomy.json 到 src/main/resources/。
新增包 com.trip.taxonomy：TagJsonParser、TaxonomyService、ResolvedDestinationTags、UserTagSelection。
RecommendServiceImpl：保留 heat/rating 权重，用 scoreDestination 替换 matchCount；sortBy=recommend 时合并请求 interestTags 与用户 preferThemeList（legacy 映射）。
FoodServiceImpl：cuisineTags 经 foodTypeByCuisine 反查后 IN 查询；有 cuisineTags 时加权排序。
DiaryRecommendServiceImpl：私有 parseTags 改为 TagJsonParser。
不要改分页全量召回逻辑，不要改表，不要过滤未命中条目。
验收：T1–T8。
```
