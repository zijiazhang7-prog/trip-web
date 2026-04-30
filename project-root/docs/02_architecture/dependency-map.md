- 作用：记录模块依赖关系。

# 模块依赖关系说明（Dependency Map）

## 1. 文档用途
本文件用于说明系统中各模块之间的依赖关系、开发顺序和联调前置条件，帮助项目在实现过程中避免出现以下问题：

- 按页面顺序开发导致底层能力缺失
- 上层模块依赖未完成而反复返工
- 多人协作时不知道谁先做、谁后做
- 联调时才发现数据库、接口、模块边界不一致

本文件关注的问题包括：
- 模块之间谁依赖谁
- 哪些模块是底层基础能力
- 哪些模块必须先完成
- 当前 MVP 适合按什么顺序实现
- 哪些模块虽然重要，但可以后置

---

## 2. 依赖关系设计原则

本项目模块依赖设计遵循以下原则：

### 2.1 先底层，后上层
优先完成数据库、认证、统一返回、异常处理、公共能力等底层支撑，再实现具体业务模块。

### 2.2 先公共能力，后业务复用
查询、排序、图建模、文件访问等能力优先抽为公共服务，避免多个业务模块重复实现。课堂讨论中也明确提到模块之间应共享数据，统一封装查询和排序能力。

### 2.3 先核心主线，后增强和创新
优先保障“推荐—规划—查询—日记”主线闭环跑通，再补美食增强、多目标路径和 AI 创新能力。

### 2.4 依赖未完成时不强行跨越
若某模块依赖的下层能力未完成，则当前阶段只输出骨架、接口和 TODO，不跨模块乱写。

---

## 3. 总体分层依赖关系

系统的总体依赖关系可概括为：

```text
第 0 层：工程与协作基础
├─ 仓库结构
├─ 开发规范
├─ 环境配置
├─ 文档骨架
└─ Git 协作规则

第 1 层：底层系统基础
├─ 数据库表结构
├─ 统一返回体 / 错误码
├─ 全局异常处理
├─ Security / JWT
├─ 文件上传基础能力
└─ 基础数据导入能力

第 2 层：公共能力层
├─ QueryService
├─ RankService
├─ MapService
├─ SearchService
├─ FileService
├─ AIService
└─ ImportService

第 3 层：核心业务模块
├─ Auth
├─ UserPreference
├─ Recommend
├─ Route
├─ Facility
├─ Food
├─ Diary
└─ Admin

第 4 层：前端页面与联调
├─ 首页 / 推荐页
├─ 偏好设置页
├─ 规划页 / 导航页
├─ 周边设施页
├─ 美食页
├─ 日记页 / 手账页
└─ 管理端页面

第 5 层：测试、验收与交付
├─ 单元测试
├─ 黑盒测试
├─ 白盒测试
├─ 缺陷修复
└─ 演示与答辩材料
```

---

## 4. 模块依赖总图（文本版）  
  
```text  
[工程初始化]  
↓  
[数据库表结构] ───────→ [数据导入脚本]  
↓  
[统一返回体 / 错误码 / 全局异常]  
↓  
[Security / JWT / Auth 基础]  
↓  
[QueryService] ───────────────┐  
[RankService] ────────────────┼──→ [Recommend]  
[MapService] ─────────────────┼──→ [Route]  
[MapService] ─────────────────┼──→ [Facility]  
[SearchService] ──────────────┼──→ [Diary 检索增强]  
[FileService] ────────────────┼──→ [Diary 发布 / 手账基础形态]  
[ImportService] ──────────────┼──→ [Admin / 数据初始化]  
[AIService] ──────────────────┘ [创新功能扩展]  
  
[Auth] ─────────→ [Diary]  
[Auth] ─────────→ [Admin]  
[Auth] ─────────→ [UserPreference]  
  
[UserPreference] ─→ [Recommend]  
[UserPreference] ─→ [AIService]  
  
[QueryService] ─→ [Admin]  
[FileService] ──→ [Admin]  
[ImportService] ─→ [Admin]  
  
[Recommend] ───→ [首页 / 推荐页 / 目的地详情页]  
[Route] ───────→ [导航页 / 路线回顾输入来源]  
[Facility] ────→ [周边设施页]  
[Food] ───────→ [美食页]  
[Diary] ──────→ [日记页 / 手账页]  
[Admin] ──────→ [管理端页面]  
  
[Admin] ───────→ [destination]  
[Admin] ───────→ [place]  
[Admin] ───────→ [facility]  
[Admin] ───────→ [food]  
[Admin] ───────→ [map_node]  
[Admin] ───────→ [map_edge]  
[Admin] ───────→ [diary]  
[Admin] ───────→ [import_batch]  
  
[后端接口完成]  
↓  
[前端页面联调]  
↓  
[测试]  
↓  
[交付]
```

---

## 5. 模块依赖总表

| 模块 / 能力          | 它负责什么             | 前置依赖                                                                                                             | 被谁依赖                               | 当前优先级   |
| ---------------- | ----------------- | ---------------------------------------------------------------------------------------------------------------- | ---------------------------------- | ------- |
| 工程初始化            | 仓库、前后端骨架、环境、规范    | 无                                                                                                                | 所有模块                               | P0      |
| 数据库表结构           | 表、字段、索引、关系        | 需求文档、数据设计                                                                                                        | 所有后端模块                             | P0      |
| 统一返回体 / 错误码 / 异常 | 接口统一输出与错误处理       | 后端骨架                                                                                                             | 所有接口模块                             | P0      |
| Security / JWT   | 认证校验、token 处理     | 用户表、后端骨架                                                                                                         | Auth、管理端、需要登录的接口                   | P0      |
| FileService      | 文件上传 / 文件访问       | 文件存储                                                                                                             | Diary、Admin、后续 AI 功能               | P0      |
| QueryService     | 统一查询能力            | 数据库表结构                                                                                                           | Recommend、Facility、Food、Admin      | P0      |
| RankService      | 排序 / Top-K        | QueryService                                                                                                     | Recommend、Facility、Food、Diary 排序增强 | P0      |
| MapService       | 图建模、最短路径、多点路径     | MapNode / MapEdge 表                                                                                              | Route、Facility                     | P0      |
| SearchService    | 全文 / 模糊检索         | 索引能力、文本数据                                                                                                        | Diary 检索增强                         | P1      |
| ImportService    | 导入与初始化            | ImportEngine、MySQL、文件存储                                                                                          | Admin、数据准备                         | P1      |
| AIService        | AI 扩展能力           | 外部 AI 能力、基础主线模块、统一输入输出定义                                                                                         | Diary 增强、Recommend 增强、创新功能         | P2      |
| Auth             | 注册、登录、当前用户        | User 表、Security                                                                                                  | UserPreference、Diary、Admin         | P0      |
| UserPreference   | 偏好标签、自由偏好描述       | Auth、用户偏好表                                                                                                       | Recommend、AIService                | P0 / P1 |
| Recommend        | 推荐、搜索、筛选          | QueryService、RankService、UserPreference                                                                          | 首页、推荐页、目的地详情                       | P0      |
| Route            | 路径规划              | MapService                                                                                                       | 导航页、部分 Diary / AI 扩展               | P0      |
| Facility         | 周边设施查询            | QueryService、RankService、MapService                                                                              | 周边页                                | P0      |
| Food             | 美食推荐              | QueryService、RankService                                                                                         | 美食页                                | P1      |
| Diary            | 发布、浏览、关联查看、手账基础形态 | FileService、基础数据、Auth                                                                                            | 日记页、后续 AI 功能                       | P0 / P1 |
| Admin            | 基础数据维护、状态管理、批量导入  | Auth、QueryService、ImportService、FileService、destination/place/facility/food/map_node/map_edge/diary/import_batch | 管理端页面                              | P1      |


---

## 6. 核心依赖关系详解

## 6.1 Auth 模块依赖关系

### 前置依赖

- 用户表
- 统一返回体 / 异常处理
- Security / JWT

### 为什么必须先做

登录和身份识别是后续日记、管理端、用户偏好设置和个性化能力的前提。没有 Auth，很多接口会缺少最基本的用户上下文。

### 被哪些模块依赖

- UserPreference
- Diary
- Admin
- 个性化推荐增强

---

## 6.2 Recommend 模块依赖关系

### 前置依赖

- 目的地相关数据表
- QueryService
- RankService
- UserPreference（当前至少包含偏好标签）

### 为什么依赖这些能力

推荐模块本质上依赖“先召回候选数据，再进行排序或 Top-K 输出”的能力，不适合把查询和排序逻辑直接写死在控制器或业务层中。  
当前用户偏好标签已经进入 P0，因此 Recommend 应预留读取用户偏好的能力。  
课堂讨论也明确提到推荐应采用检索后二次排序。

### 被哪些模块依赖

- 首页推荐流
- 推荐页
- 目的地详情页
- 后续 AI 推荐理由解释

---

## 6.2A UserPreference 模块依赖关系

### 前置依赖

- Auth
- 用户偏好相关数据表
- 统一返回体 / 异常处理

### 为什么必须明确出来

当前项目中，用户偏好不再只是 Recommend 的隐含输入，而是已经纳入正式需求范围：
- 偏好标签进入 P0
- 自由偏好描述进入 P1

如果不单独作为依赖关系说明，后续 Recommend、AIService、多人协同决策等功能会反复各自维护一套偏好输入逻辑。

### 被哪些模块依赖

- Recommend
- AIService
- 后续多人规划创新功能

---

## 6.3 Route 模块依赖关系

### 前置依赖

- 地图节点表
- 地图边表
- MapService
- 图算法基础能力

### 为什么必须独立依赖 MapService

路径规划模块是典型的图问题，不能由页面逻辑或普通查询逻辑替代。课程要求中明确把最短路径和多点路径规划作为重点。

### 被哪些模块依赖

- 导航页
- 后续多目标路径增强
- 部分日记路线回顾功能（后续）
- AI 路线回顾说明生成（后续）

### 当前阶段说明

当前 Route 模块统一基于内部 `MapService` 和自建图数据实现；在未确定具体地图 API 前，外部地图服务只作为后续导航、轨迹记录和路线回顾增强能力预留，不替代当前主路径算法。

---

## 6.4 Facility 模块依赖关系

### 前置依赖

- 设施数据表
- QueryService
- RankService
- MapService

### 为什么不是只依赖 QueryService

设施查询不仅要“查出来”，还要根据当前位置和图上可达距离/时间来返回结果，因此除了查询和排序，还需要路径或距离能力支撑。课堂讨论中已明确说明这一点。

### 被哪些模块依赖

- 周边设施页
- 路线规划后的下一步查询动作

---

## 6.5 Food 模块依赖关系

### 前置依赖

- 美食数据表
- QueryService
- RankService

### 当前为什么可以后置

Food 模块重要，但它不阻塞“推荐—规划—查询—日记”这条最小闭环，所以当前可作为 P1 模块，在基础主线跑通后补上。

### 被哪些模块依赖

- 美食页
- 目的地详情页中的扩展入口

---

## 6.6 Diary 模块依赖关系

### 前置依赖

- Auth
- Diary 主表 / 媒体表 / 评分表
- FileService
- 目的地基础数据

### 为什么 Diary 先不强依赖 SearchService

当前 MVP 里，Diary 模块先要跑通“发布—浏览—详情—按目的地查看”这条主线；全文检索属于增强项，可以后续再加 SearchService。

### 被哪些模块依赖

- 日记页
- 目的地相关日记入口
- 手账基础形态
- 后续 AI 增强能力

### 当前阶段说明

Diary 当前优先承接图文日记基础版；  
手账式旅行记录作为 P1，在基础日记稳定后补充；  
AI 日记草稿、图片摘要、手账增强作为 P2 能力后接，不阻塞基础发布与浏览链路。

---

## 6.7 Admin 模块依赖关系

### 前置依赖

- Auth
- QueryService
- ImportService
- FileService（如管理端涉及封面图、媒体或导入文件）
- `destination`
- `place`
- `facility`
- `food`
- `map_node`
- `map_edge`
- `diary`
- `import_batch`

### 当前为什么不作为最先开发模块

管理端是重要支撑模块，但它不一定要在最前面做完整，只要先保证最基本的数据导入与维护能力即可。当前可以先做最小可用后台支撑，再逐步补全。

---

## 7. 公共能力依赖关系详解

## 7.1 QueryService

### 作用

统一承接推荐、设施、美食、管理端等模块的基础查询逻辑。

### 前置依赖

- 数据库表结构
- Mapper
- 必要的索引能力

### 开发优先级

P0

---

## 7.2 RankService

### 作用

统一承接推荐、设施、美食等模块的排序与 Top-K 输出能力。

### 前置依赖

- QueryService
- 排序规则定义

### 开发优先级

P0

---

## 7.3 MapService

### 作用

统一承接图建模、最短路径和多点路径能力，并为周边设施可达距离计算提供基础支撑。

### 前置依赖

- MapNode / MapEdge
- GraphEngine

### 开发优先级

P0

### 当前阶段约束说明

当前路线规划与设施查询统一基于内部 `MapService` 和自建图数据实现；在未确定具体地图 API 前，外部地图服务只作为后续导航、轨迹记录和路线回顾增强能力预留，不替代当前主路径算法。

---

## 7.4 SearchService

### 作用

提供文本检索与模糊检索能力。

### 前置依赖

- 文本数据
- 索引能力

### 开发优先级

P1

---

## 7.5 FileService

### 作用

提供文件上传、访问和基础存储管理能力。

### 前置依赖

- 文件存储目录或对象存储配置
- 上传规则

### 开发优先级

P0

---

## 7.6 AIService

### 作用

作为后续创新功能的统一接入层。

### 前置依赖

- 外部 AI 能力
- Diary、Recommend、Route、UserPreference 等基础主线模块
- 必要的输入输出定义

### 开发优先级

P2

### 当前阶段约束说明

当前 AI 能力统一通过 `AIService` 抽象；在未确定具体模型 API 前，先使用统一输入输出结构和 mock / stub 实现，不在业务模块中直接绑定具体模型厂商。

### 被哪些能力依赖

- Diary 增强（AI 日记草稿、图片摘要、手账增强）
- Recommend 增强（推荐理由解释）
- Route / Diary 扩展（路线回顾）
- 多人旅游规划协商

---

## 7.7 ImportService

### 作用

负责批量导入与初始化数据写入。

### 前置依赖

- ImportEngine
- MySQL
- 文件存储
- 数据源准备

### 开发优先级

P1

---

## 8. 当前推荐开发顺序

当前推荐的开发顺序如下：

### 第一步：工程和规则基础

1. 仓库与目录初始化
2. 根目录规范文件
3. 前后端工程骨架
4. 基础配置与环境跑通

### 第二步：数据与底层支撑

5. 数据库表结构
6. 统一返回体 / 错误码 / 全局异常
7. Security / JWT
8. FileService
9. 基础数据导入能力

### 第三步：公共能力

10. QueryService
11. RankService
12. MapService

### 第四步：核心业务模块

13. Auth
14. UserPreference（先偏好标签）
15. Recommend
16. Route（先单目标）
17. Facility
18. Diary（图文基础版）

### 第五步：联调与增强

19. 前端页面联调
20. Food
21. Admin（Admin 当前优先实现最小后台能力：目的地、场所、设施、美食的基础维护与导入入口；地图节点 / 边维护和日记管理可后续补齐。）
22. SearchService
23. Route 多目标增强
24. Diary 检索增强
25. Diary 手账基础形态
26. UserPreference 自由偏好描述

### 第六步：创新与扩展

27. AIService
28. AI 日记草稿 / 图片摘要
29. 路线回顾
30. 多人规划协商
31. 外部地图 API / 多模态 AI API 评估与接入

---

## 9. 当前阶段的阻塞判断规则

如果出现以下情况，说明当前不应继续往上层模块推进：

1. 数据库表结构未稳定
2. 统一返回体和异常机制未定
3. Auth 未完成却尝试做完整日记能力
4. UserPreference 未完成却尝试做依赖真实偏好的推荐或多人协同
5. MapService 未完成却尝试强行联调路线规划
6. FileService 未完成却尝试做完整日记上传
7. QueryService / RankService 未完成却在多个模块中重复手写查询与排序
8. AIService 未完成却在 Diary / Recommend / Route 中直接绑定具体模型厂商
9. 外部地图 API 未定却试图让它替代当前内部图规划主实现

---

## 10. 前后端联调前置条件

各页面在进入联调前，应至少满足以下条件：

### 推荐页

- Recommend 接口可返回有效数据
- 排序字段和结果结构稳定

### 导航页

- Route 接口可返回路径节点和距离结果
- 当前目的地图数据可用

### 周边设施页

- Facility 接口可返回设施列表
- 距离或可达结果结构稳定

### 日记页

- Diary 发布 / 列表 / 详情接口可用
- FileService 上传能力已可用

### 美食页

- Food 接口可返回基础结果

### 管理端

- Auth 基础权限可用
- Admin 的最小数据维护接口可用

---

## 11. 与其他文档的关系

本文件应与以下文档保持一致：

- `project-root/docs/01_requirements/scope-mvp.md`
- `project-root/docs/01_requirements/use-cases.md`
- `project-root/docs/02_architecture/architecture.md`
- `project-root/docs/02_architecture/module-map.md`
- `project-root/docs/03_data/schema.md`
- `project-root/docs/04_api/api-spec.md`
- `project-root/docs/05_modules/*`
- `project-root/docs/09_decisions/ADR-003-module-split.md`
- `project-root/docs/04_api/external-integrations.md`（用于统一记录外部 API 接入方式、配置变量、调用限制与降级方案）

如果开发顺序、依赖边界、模块优先级或外部 API 接入边界发生变化，应同步更新这些文档。

---

## 12. 后续维护说明

本文件应在以下场景下更新：

1. 某模块新增关键前置依赖时
2. 公共能力拆分方式变化时
3. MVP 开发顺序调整时
4. 创新需求正式接入架构时
5. 联调阶段发现原有依赖判断不准确时
6. UserPreference 从隐含输入变为独立模块时
7. Diary 正式承接手账基础形态时
8. AIService 正式接入外部模型 API 时
9. 外部地图 API 正式接入并影响 Route / MapService 边界时