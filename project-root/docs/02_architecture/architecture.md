- 作用：记录系统总体方案。

# 系统架构设计（Architecture）

## 1. 文档用途
本文件用于说明本项目的总体架构设计方案，包括系统形态、技术选型、总体分层、后端模块组织、关键数据流、当前阶段实现范围以及后续扩展方向。

本文件关注的问题包括：
- 为什么采用当前这套架构
- 系统整体由哪些层组成
- 后端模块如何划分和协作
- 关键公共能力如何抽取
- 当前 MVP 先落哪些能力
- 后续增强和创新需求如何接入当前架构

---

## 2. 架构目标与设计原则

## 2.1 架构目标
本项目的架构设计目标如下：

1. 支撑一个可远程访问的 Web 个性化旅游系统；
2. 覆盖旅游前推荐、旅游中路线规划与场所查询、旅游后日记管理与交流的主线功能；
3. 将排序、查找、图、最短路径、多点路径等数据结构与算法能力落到实际系统中；
4. 在三人协作和课程设计周期内，保证系统可实现、可联调、可演示、可扩展；
5. 为后续创新需求预留清晰扩展空间，但不让创新需求阻塞基础主线落地。

## 2.2 设计原则
本项目架构遵循以下原则：

### 2.2.1 Web 远程系统优先
系统采用 Web 远程访问方式，避免做成只能在单机本地运行的桌面程序，便于演示、联调与后续扩展。

### 2.2.2 B/S 架构
系统采用 Browser / Server 架构，前端通过浏览器访问后端接口服务。

### 2.2.3 前后端分离
前端负责页面交互与展示，后端负责业务逻辑、数据访问、算法能力和接口提供，二者通过 API 通信。

### 2.2.4 单体后端
考虑到本项目为课程设计项目，团队规模为 3 人，且系统需要优先保证最小闭环可运行，因此后端采用单体应用而非微服务架构。

### 2.2.5 算法模块化
排序、查找、图建模、路径规划、检索等算法能力不分散在各业务模块中，而是尽量抽象为公共能力层，减少重复实现并提升可维护性。

### 2.2.6 先主线闭环，后增强扩展
优先实现推荐—规划—查询—日记这条业务主线，在主线跑通之后再逐步增强多目标路径、美食推荐优化和创新功能。

---

## 3. 总体架构设计

## 3.1 系统总体架构图

```mermaid
flowchart LR
    User[用户 / 管理员] --> Browser[浏览器]

    subgraph Frontend["前端层 Vue 3 + Vite + Element Plus"]
        SPA[Web 前端应用 SPA]
        UserUI[用户端页面<br/>推荐 / 导航 / 周边 / 美食 / 日记]
        AdminUI[管理端页面<br/>用户 / 景点 / 地图 / 设施 / 日记管理]
        State[Pinia 状态管理]
        Http[Axios API 调用]
    end

    Browser --> SPA
    SPA --> UserUI
    SPA --> AdminUI
    SPA --> State
    SPA --> Http

    subgraph Backend["后端层 Spring Boot 3 + MyBatis-Plus"]
        Gateway[REST API 网关 / Controller]

        Auth[用户与权限模块]
        Rec[推荐与查询模块]
        Route[路线规划模块]
        Poi[周边设施 / 美食模块]
        Diary[旅游日记模块]
        Admin[管理端数据维护模块]

        QuerySvc[QueryService<br/>统一查询]
        RankSvc[RankService<br/>排序 / Top-K]
        MapSvc[MapService<br/>图建模 / Dijkstra / A* / 多点启发式]
        SearchSvc[SearchService<br/>全文检索]
        FileSvc[FileService<br/>文件上传 / 文件访问]
        ImportSvc[ImportService<br/>批量导入]
    end

    Http --> Gateway

    Gateway --> Auth
    Gateway --> Rec
    Gateway --> Route
    Gateway --> Poi
    Gateway --> Diary
    Gateway --> Admin

    Rec --> QuerySvc
    Rec --> RankSvc
    Route --> MapSvc
    Poi --> QuerySvc
    Poi --> RankSvc
    Diary --> SearchSvc
    Diary --> FileSvc
    Admin --> ImportSvc

    subgraph DataLayer["数据存储层"]
        MySQL[(MySQL 8<br/>用户 / 景点 / 边 / 设施 / 美食 / 日记 / 评分)]
        FileStore[(文件存储<br/>图片 / 视频 / 地图 JSON/CSV / 上传附件)]
    end

    Auth --> MySQL
    QuerySvc --> MySQL
    RankSvc --> MySQL
    MapSvc --> MySQL
    SearchSvc --> MySQL
    Admin --> MySQL
    ImportSvc --> MySQL

    FileSvc --> FileStore
    Diary --> FileStore
    ImportSvc --> FileStore

    subgraph ETL["离线数据处理层"]
        PyETL[Python + pandas<br/>爬取 / 清洗 / 转换 / 批量导入]
    end

    PyETL --> ImportSvc
    PyETL --> FileStore
````

## 3.2 总体架构说明

### 3.2.1 前端层

前端层用于承载用户端和管理端页面，包括推荐页、导航页、周边设施页、美食页、日记页以及管理端数据维护页面。  
前端采用 Vue 3 实现单页应用，使用 Element Plus 提供基础 UI 组件，使用 Pinia 管理状态，使用 Axios 调用后端接口。

### 3.2.2 后端层

后端层作为整个系统的核心业务承载层，负责：

- 用户认证与权限校验
- 推荐与查询逻辑
- 路线规划与图算法调度
- 周边设施与美食查询
- 旅游日记保存、检索与文件管理
- 管理端数据维护
- 数据导入与基础支撑能力

### 3.2.3 数据存储层

系统的数据存储分为两部分：

1. **MySQL 8**
    
    - 保存结构化业务数据
    - 包括用户、目的地、场所、地图节点与边、设施、美食、日记、评分等信息
        
2. **文件存储**
    
    - 保存图片、视频、上传附件、地图 JSON/CSV 等非结构化资源
    - 主要服务于日记媒体、导入文件和其他附件场景

### 3.2.4 离线数据处理层

离线数据处理层由 Python + pandas 组成，主要负责：

- 基础数据爬取
- 原始数据清洗
- 数据格式转换
- 批量导入准备

该层不直接服务用户请求，而是通过导入服务将数据送入系统运行环境。

---

## 4. 架构选型说明

## 4.1 为什么采用 Web + B/S 形态

课程与课堂讨论都强调应尽量做成 Web/App 远程系统，而不是单机版。本项目采用 Web 方案，能够更方便实现：

- 多人访问
- 前后端分离协作
- 管理端和用户端统一入口
- 浏览器直接演示和验收

## 4.2 为什么采用前后端分离

前后端分离适合当前项目的协作方式：

- 前端成员可以围绕页面和交互独立推进
- 后端成员可以围绕接口、算法和数据模型独立推进
- 更便于模块分工、接口联调和问题定位

## 4.3 为什么采用单体后端

本项目团队规模小、周期有限、功能边界清晰，更适合单体后端架构。采用单体后端有以下优势：

- 工程复杂度低
- 部署与调试成本低
- 接口联调路径更短
- 更适合课程设计和快速迭代

## 4.4 为什么抽取公共能力层

推荐、周边查询、美食、日记检索和路径规划之间存在大量共享能力，例如：

- 查询
- 排序 / Top-K
- 图建模与路径搜索
- 全文检索
- 文件访问
- 数据导入

如果这些能力完全分散到业务模块中，后期会造成大量重复逻辑，因此需要统一抽取。

---

## 5. 后端架构设计

## 5.1 后端模块结构图

```mermaid
flowchart TB

    subgraph ClientLayer["前端调用层"]
        FE_USER["用户端页面"]
        FE_ADMIN["管理端页面"]
    end

    subgraph ApiLayer["接口层 Controller"]
        CTRL_AUTH["AuthController"]
        CTRL_REC["RecommendController"]
        CTRL_ROUTE["RouteController"]
        CTRL_FAC["FacilityController"]
        CTRL_FOOD["FoodController"]
        CTRL_DIARY["DiaryController"]
        CTRL_ADMIN["AdminController"]
        CTRL_UPLOAD["UploadController"]
    end

    subgraph ServiceLayer["业务层 Service"]
        SVC_AUTH["AuthService"]
        SVC_REC["RecommendService"]
        SVC_ROUTE["RoutePlanService"]
        SVC_FAC["FacilityService"]
        SVC_FOOD["FoodService"]
        SVC_DIARY["DiaryService"]
        SVC_ADMIN["AdminService"]
        SVC_IMPORT["ImportService"]
    end

    subgraph CoreLayer["公共能力层 Core Services"]
        CORE_QUERY["QueryService<br/>统一查询"]
        CORE_RANK["RankService<br/>排序 / Top-K"]
        CORE_SEARCH["SearchService<br/>全文检索"]
        CORE_MAP["MapService<br/>图建模 / Dijkstra / A* / 多点启发式"]
        CORE_AI["AIService<br/>日记生成 / 图片摘要 / 标签提取"]
        CORE_FILE["FileService<br/>文件上传 / 文件访问"]
    end

    subgraph AlgoLayer["算法与基础设施层"]
        ALG_GRAPH["GraphEngine<br/>有向带权图"]
        ALG_INDEX["IndexEngine<br/>Hash / Trie / 倒排索引"]
        ALG_COMP["CompressionEngine<br/>Huffman"]
        ALG_IMPORT["ImportEngine<br/>CSV / JSON / pandas 导入"]
        ALG_SEC["Security / JWT"]
        ALG_BASE["Config / Exception / Response"]
    end

    subgraph DaoLayer["数据访问层 Mapper"]
        MAP_USER["UserMapper"]
        MAP_DEST["DestinationMapper"]
        MAP_PLACE["PlaceMapper"]
        MAP_NODE["MapNodeMapper"]
        MAP_EDGE["MapEdgeMapper"]
        MAP_FAC["FacilityMapper"]
        MAP_FOOD["FoodMapper"]
        MAP_DIARY["DiaryMapper"]
        MAP_RATE["DiaryRatingMapper"]
        MAP_MEDIA["DiaryMediaMapper"]
        MAP_ROUTE["RouteHistoryMapper"]
    end

    subgraph StoreLayer["存储层"]
        DB_MAIN[("MySQL 8")]
        FS_MAIN[("文件存储")]
    end

    FE_USER --> CTRL_AUTH
    FE_USER --> CTRL_REC
    FE_USER --> CTRL_ROUTE
    FE_USER --> CTRL_FAC
    FE_USER --> CTRL_FOOD
    FE_USER --> CTRL_DIARY
    FE_ADMIN --> CTRL_ADMIN
    FE_ADMIN --> CTRL_UPLOAD

    CTRL_AUTH --> SVC_AUTH
    CTRL_REC --> SVC_REC
    CTRL_ROUTE --> SVC_ROUTE
    CTRL_FAC --> SVC_FAC
    CTRL_FOOD --> SVC_FOOD
    CTRL_DIARY --> SVC_DIARY
    CTRL_ADMIN --> SVC_ADMIN
    CTRL_UPLOAD --> SVC_IMPORT

    SVC_AUTH --> CORE_QUERY
    SVC_REC --> CORE_QUERY
    SVC_REC --> CORE_RANK
    SVC_ROUTE --> CORE_MAP
    SVC_FAC --> CORE_QUERY
    SVC_FAC --> CORE_RANK
    SVC_FOOD --> CORE_QUERY
    SVC_FOOD --> CORE_RANK
    SVC_DIARY --> CORE_SEARCH
    SVC_DIARY --> CORE_AI
    SVC_DIARY --> CORE_FILE
    SVC_ADMIN --> CORE_QUERY
    SVC_IMPORT --> CORE_FILE

    CORE_MAP --> ALG_GRAPH
    CORE_QUERY --> ALG_INDEX
    CORE_SEARCH --> ALG_INDEX
    SVC_DIARY --> ALG_COMP
    SVC_IMPORT --> ALG_IMPORT
    SVC_AUTH --> ALG_SEC

    SVC_AUTH --> MAP_USER
    SVC_REC --> MAP_DEST
    SVC_REC --> MAP_PLACE
    SVC_ROUTE --> MAP_NODE
    SVC_ROUTE --> MAP_EDGE
    SVC_FAC --> MAP_FAC
    SVC_FOOD --> MAP_FOOD
    SVC_DIARY --> MAP_DIARY
    SVC_DIARY --> MAP_RATE
    SVC_DIARY --> MAP_MEDIA
    SVC_ROUTE --> MAP_ROUTE
    SVC_ADMIN --> MAP_USER
    SVC_ADMIN --> MAP_DEST
    SVC_ADMIN --> MAP_PLACE
    SVC_ADMIN --> MAP_FAC
    SVC_ADMIN --> MAP_FOOD
    SVC_ADMIN --> MAP_DIARY

    MAP_USER --> DB_MAIN
    MAP_DEST --> DB_MAIN
    MAP_PLACE --> DB_MAIN
    MAP_NODE --> DB_MAIN
    MAP_EDGE --> DB_MAIN
    MAP_FAC --> DB_MAIN
    MAP_FOOD --> DB_MAIN
    MAP_DIARY --> DB_MAIN
    MAP_RATE --> DB_MAIN
    MAP_MEDIA --> DB_MAIN
    MAP_ROUTE --> DB_MAIN

    CORE_FILE --> FS_MAIN
    SVC_DIARY --> FS_MAIN
    SVC_IMPORT --> FS_MAIN
```

## 5.2 后端分层说明

### 5.2.1 接口层（Controller）

接口层负责：

- 接收前端请求
- 完成参数接收与基础校验入口
- 调用业务层
- 返回统一响应结构

接口层不直接承载复杂业务逻辑。

### 5.2.2 业务层（Service）

业务层负责：

- 实现各模块业务流程
- 组织模块内逻辑
- 调用公共能力层
- 组合数据访问结果并返回给接口层

当前主要业务服务包括：

- `AuthService`
- `RecommendService`
- `RoutePlanService`
- `FacilityService`
- `FoodService`
- `DiaryService`
- `AdminService`
- `ImportService`

### 5.2.3 公共能力层（Core Services）

公共能力层用于复用多个模块都会依赖的基础能力，包括：

- `QueryService`：统一查询逻辑
- `RankService`：排序与 Top-K 能力
- `SearchService`：全文检索能力
- `MapService`：图建模、最短路径、多点路径等能力
- `AIService`：AI 生成、摘要、标签提取等能力
- `FileService`：文件上传与文件访问能力

### 5.2.4 算法与基础设施层

该层是后端更底层的支撑层，包括：

- `GraphEngine`：有向带权图建模
- `IndexEngine`：Hash / Trie / 倒排索引等查找与检索支撑
- `CompressionEngine`：压缩相关能力
- `ImportEngine`：导入与格式转换支撑
- `Security`：认证安全与 JWT 相关能力
- `Config / Exception / Response`：基础配置、异常与统一返回机制

### 5.2.5 数据访问层（Mapper）

数据访问层负责与数据库交互，主要包括：

- 用户
- 目的地
- 场所
- 地图节点与边
- 周边设施
- 美食
- 日记
- 日记评分
- 日记媒体
- 路线历史

### 5.2.6 存储层

存储层负责承载真正的数据资源，包括：

- MySQL：结构化业务数据
- 文件存储：图片、视频、地图源文件、上传附件等

---

## 6. 核心模块说明

## 6.1 认证模块（Auth）

负责用户注册、登录、当前用户信息获取以及基础权限校验，为系统多用户能力提供入口。

## 6.2 推荐模块（Recommend）

负责旅游前的目的地推荐、搜索与筛选，并依赖统一查询能力和排序能力实现结果组织。

## 6.3 路线规划模块（Route）

负责目的地内部的图建模、最短路径与多点路径规划，是系统中最能体现图与路径算法的核心模块之一。

## 6.4 场所 / 周边设施模块（Facility）

负责基于当前位置查找周边设施，并结合图上可达距离或时间返回结果。

## 6.5 美食模块（Food）

负责基于目的地或当前位置返回美食相关信息，并支持基础排序和筛选。

## 6.6 日记模块（Diary）

负责旅游日记的发布、浏览、详情展示、按目的地查看以及后续检索和 AI 扩展能力。

## 6.7 管理模块（Admin）

负责基础数据维护，包括目的地、场所、设施、美食、日记等数据的管理与导入支撑。

## 6.8 AI 模块（AIService）

作为系统的扩展能力层，为后续创新需求提供统一接入点，例如：

- 日记生成
- 图片摘要
- 标签提取
- 多人规划协商说明

---

## 7. 关键数据流说明

## 7.1 推荐流程

1. 用户从前端进入推荐页
2. 前端调用推荐接口
3. 推荐模块调用查询服务和排序服务
4. 从数据库读取目的地相关数据
5. 返回推荐列表给前端展示

## 7.2 路线规划流程

1. 用户输入当前位置和目标点
2. 前端调用路线规划接口
3. 路线规划模块调用 `MapService`
4. `MapService` 使用图建模和最短路径算法计算结果
5. 返回路径节点、距离、预计时间和展示结果

## 7.3 周边设施查询流程

1. 用户基于当前位置查询周边设施
2. 接口层调用设施服务
3. 设施服务结合查询服务和图上距离能力返回设施列表
4. 前端按设施类型和距离展示结果

## 7.4 日记发布流程

1. 用户进入日记发布页面
2. 上传图片并输入标题、正文、目的地信息
3. 前端调用文件上传接口和日记接口
4. 后端通过 `FileService` 保存媒体资源
5. 日记服务保存日记主体信息和媒体关联信息
6. 返回发布结果

## 7.5 数据导入流程

1. Python 脚本完成爬取、清洗和格式转换
2. 通过导入服务或导入脚本写入数据库和文件存储
3. 后续用户端功能基于这些数据运行

---

## 8. 当前阶段实现范围与扩展性

## 8.1 当前 MVP 优先范围

当前阶段优先实现以下最小闭环：

1. 用户注册 / 登录
2. 推荐目的地
3. 搜索目的地
4. 单目标路线规划
5. 周边设施查询
6. 发布图文日记
7. 浏览 / 查看相关日记
8. 基础数据导入与最小后台支撑

## 8.2 当前暂不优先做的内容

- 复杂社交功能
- 复杂商业功能
- 微服务与复杂中间件
- 脱离主线的重型创新功能

## 8.3 后续扩展方向

当前架构已经为以下方向预留扩展空间：

- 多目标路径规划
- 最短时间策略
- 更丰富的推荐模型
- 日记全文检索
- 多模态日记生成
- 多人旅游规划协商
- 更完整的管理端能力

---

## 9. 架构与课程要求的对应关系

当前架构与课程要求之间的对应关系如下：

- **旅游推荐** → Recommend 模块 + QueryService + RankService
- **旅游路线规划** → Route 模块 + MapService + GraphEngine
- **场所查询** → Facility 模块 + QueryService + 图上距离能力
- **旅游日记交流** → Diary 模块 + FileService + SearchService
- **数据要求与批量导入** → ImportService + Python ETL
- **排序 / 查找 / 图 / 路径规划等算法要求** → 公共能力层与算法层

这保证了课程要求并不是只体现在文档上，而是体现在系统实现的结构里。

---

## 10. 与其他文档的关系

本文件应与以下文档保持一致：

- `docs/01_requirements/prd.md`
- `docs/01_requirements/scope-mvp.md`
- `docs/02_architecture/module-map.md`
- `docs/02_architecture/dependency-map.md`
- `docs/03_data/schema.md`
- `docs/04_api/api-spec.md`
- `docs/05_modules/*`
- `docs/09_decisions/ADR-001-tech-stack.md`
- `docs/09_decisions/ADR-003-module-split.md`

如果总体架构、模块边界或技术路线发生明显变化，应同步更新上述文档。

---

## 11. 后续维护说明

本文件应在以下场景下更新：

1. 技术栈调整时
2. 系统总体分层发生变化时
3. 新增重要公共能力层或核心模块时
4. 创新需求正式纳入架构时
5. 数据流或模块协作关系发生明显变化时

