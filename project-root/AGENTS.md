- 作用：告诉 AI 这个仓库的使用规则。

---

## 1. Project
本仓库是 3 人课程设计项目《个性化旅游系统》的代码与文档仓库。

## 2. Goal
实现一个面向旅游活动全流程的 Web 个性化旅游系统，覆盖以下主线：

- 旅游前：目的地推荐、搜索与筛选、用户偏好输入
- 旅游中：路线规划、场所 / 周边设施查询
- 旅游后：旅游日记发布、浏览与回顾扩展

系统既要满足业务功能，也要体现数据结构课程特点，将排序、查找、最短路径、多点路径、全文检索、压缩等算法真正落到系统中。

## 3. Architecture Constraints
- Web 远程系统
- B/S 架构
- 前后端分离
- 单体后端
- 算法模块化
- 不允许擅自改成微服务
- 不允许擅自引入消息队列、复杂权限中心、分布式中间件
- 不允许脱离课程要求扩大成复杂商业平台
- 当前主路径算法统一由内部 `MapService` 与自建图数据实现，不允许把外部地图服务替代为主实现
- 当前 AI 能力统一通过 `AIService` 预留，不允许把 AI 黑箱当作基础算法实现本体

## 4. Tech Stack
### Frontend
- Vue 3
- Vite
- Element Plus
- Axios
- Vue Router
- Pinia

### Backend
- Java 17
- Spring Boot 3
- MyBatis-Plus
- Maven
- Spring Security + JWT

### Database / Storage
- MySQL 8
- 本地文件存储 / 统一文件目录

### Scripts / Data
- Python
- pandas

### AI Tools
- Claude Code
- OpenClaw
- ChatGPT
- Codex
- Qoder

## 5. Highest Priority Rules
在生成、修改、重构代码或文档前，优先遵守：

1. `project-root/coding-rules.md`
2. `project-root/security-rules.md`
3. `project-root/README.md`
4. 当前任务相关模块文档
5. 其他项目文档

外部参考资料仅作参考，不高于本仓库内规则。

## 6. Source of Truth Order
若需要确认项目事实，按以下顺序读取：

1. `project-root/coding-rules.md`
2. `project-root/security-rules.md`
3. `project-root/docs/01_requirements/prd.md`
4. `project-root/docs/01_requirements/scope-mvp.md`
5. `project-root/docs/02_architecture/architecture.md`
6. `project-root/docs/02_architecture/tech-stack.md`
7. `project-root/docs/02_architecture/module-map.md`
8. `project-root/docs/02_architecture/dependency-map.md`
9. `project-root/docs/04_api/external-integrations.md`
10. `project-root/docs/03_data/schema.md`
11. `project-root/docs/03_data/data-dictionary.md`
12. `project-root/docs/04_api/api-spec.md`
13. `project-root/docs/05_modules/当前模块说明.md`
14. `project-root/docs/09_decisions/*.md`
15. `project-root/docs/06_testing/*`
16. `project-root/docs/00_project/project-overview.md`
17. `project-root/docs/00_project/progress.md`

如果文档之间冲突：
- 优先采用最近已确认的项目文档
- 若仍无法判断，明确标注“待确认”
- 不允许自行编造业务规则、表结构、接口字段

## 7. Reference Material Policy
- 参考资料仅作参考，不得直接当作当前项目事实
- 若参考资料与项目已确认设计冲突，以项目文档为准

## 8. How to Read Context
处理任务时按最小必要范围读取文档：

### 如果任务是 Auth / UserPreference
优先读：
- `project-root/docs/05_modules/auth-module.md`
- `project-root/docs/05_modules/user-preference-module.md`
- `project-root/docs/03_data/schema.md`
- `project-root/docs/04_api/api-spec.md`

### 如果任务是 Recommend
优先读：
- `project-root/docs/05_modules/recommend-module.md`
- `project-root/docs/02_architecture/module-map.md`
- `project-root/docs/03_data/schema.md`
- `project-root/docs/04_api/api-spec.md`
- `project-root/docs/09_decisions/ADR-004-algorithm-choice.md`

### 如果任务是 Route / Facility
优先读：
- `project-root/docs/05_modules/route-module.md`
- `project-root/docs/05_modules/facility-module.md`（如存在）
- `project-root/docs/03_data/schema.md`
- `project-root/docs/02_architecture/dependency-map.md`
- `project-root/docs/09_decisions/ADR-004-algorithm-choice.md`

### 如果任务是 Diary / AI
优先读：
- `project-root/docs/05_modules/diary-module.md`
- `project-root/docs/05_modules/ai-module.md`
- `project-root/docs/03_data/schema.md`
- `project-root/docs/04_api/api-spec.md`

### 如果任务是测试
优先读：
- `project-root/docs/06_testing/test-plan.md`
- `project-root/docs/06_testing/test-cases.md`
- 当前模块文档

### 如果任务是交付文档
优先读：
- `project-root/docs/07_delivery/*`
- `project-root/docs/00_project/project-overview.md`
- `project-root/docs/00_project/progress.md`

## 9. Development Rules
1. 一次只实现一个模块或一个明确子功能
2. 优先给出最小可运行版本
3. 不要一次性生成整个系统
4. 不要跳过依赖模块直接实现上层功能
5. 若依赖未完成，只输出当前模块骨架和清晰 TODO
6. 修改数据库、接口、模块依赖后，必须提醒同步对应文档
7. 不确定的地方必须标注“待确认”
8. 不允许凭空捏造未确认的数据表、接口或业务规则

## 10. Module Boundary Rules

### 后端生成约束

#### 分层约束
后端按以下分层组织：
- `controller`：接收参数、调用 service、返回响应
- `service`：业务逻辑
- `mapper/repository`：数据访问
- `entity`：数据库实体
- `dto/request`：请求对象
- `vo/response`：响应对象
- `config` / `exception` / `common` / `util`：公共支撑

#### 后端代码要求
1. Controller 不写复杂业务逻辑
2. Service 不直接把 Entity 返回给前端
3. 所有接口统一返回 `ApiResponse`
4. 所有公开类和核心方法写简洁注释
5. 优先抽公共服务，不重复造轮子
6. 代码优先追求最小可运行、便于初学者接手
7. 不要为了“炫技”引入复杂写法

### 前端生成约束
前端按以下结构组织：
- `pages`
- `components`
- `api`
- `store`
- `router`
- `types` / `constants` / `utils`

#### 前端代码要求
1. 页面只保留页面级逻辑
2. 复用逻辑尽量拆组件或抽函数
3. 接口请求集中放在 `api`
4. 页面优先服务任务主线，不做过度装饰
5. 推荐页、导航页、日记页优先突出主任务

## 11. Public Services
优先复用或抽象以下公共能力，不要重复造轮子：
- `QueryService`
- `RankService`
- `MapService`
- `SearchService`
- `AIService`
- `FileService`
- `ImportService`

当前优先级说明：
- `QueryService / RankService / MapService / FileService`：P0
- `SearchService / ImportService`：P1
- `AIService`：P2

## 12. Security & Quality Rules
- 所有输入都视为不可信，必须做参数校验、空值处理、长度与范围限制
- 禁止字符串拼接 SQL，优先参数化查询或 ORM
- 禁止硬编码数据库密码、JWT 密钥、API Key
- 禁止在日志中输出 password、token、secret、cookie 等敏感信息
- 认证接口要考虑 token 缺失、无效、过期
- 管理端接口要考虑角色校验
- 使用统一异常处理，不允许吞异常，不向前端暴露堆栈
- 文件上传必须限制类型、大小、数量，并校验后缀和 MIME
- 外部 API 调用必须设置超时并有失败兜底
- 新增依赖前必须说明用途
- 核心业务方法至少给最小单元测试样例

## 13. Documentation Sync Rules
以下变更发生时，必须提醒同步文档：

- 数据库表结构变化 → `project-root/docs/03_data/*`
- API 变化 → `project-root/docs/04_api/*`
- 模块职责变化 → `project-root/docs/05_modules/*`
- 技术路线变化 → `project-root/docs/02_architecture/*` 与 `project-root/docs/09_decisions/*`
- 外部服务接入边界变化 → `project-root/docs/04_api/external-integrations.md`
- 测试策略变化 → `project-root/docs/06_testing/*`
- 交付方式变化 → `project-root/docs/07_delivery/*`
- 需求或架构发生实质变化 → 更新对应 ADR

### 13.1 ADR 更新规则

以下变更属于“必须更新 ADR 的实质变化”：

#### 1. 需求范围变化
当出现以下任一情况时，必须更新 `ADR-002-mvp-scope.md`：

- 新增、删除或替换一个核心功能模块  
  例如：新增“多人协商规划”作为正式功能；删除“美食推荐”；把“旅游日记”从主线移除
- 调整 P0 / P1 / P2 范围，且影响开发顺序、接口、数据库、测试或验收演示  
  例如：把“多目标路径规划”从 P1 提前到 P0；把“日记评分”从 P1 改成不做
- 改变系统主线闭环  
  例如：原本按“推荐 → 规划 → 查询 → 日记”推进，后来改成“推荐 → 规划 → AI 回顾”，并影响模块与页面设计
- 改变课程基础功能的实现边界  
  例如：不再实现课程题目中的某项基本功能，或将其替换为别的功能
- 改变用户角色或权限边界  
  例如：新增“运营角色”；取消管理端；把管理员能力拆成多个角色

#### 2. 架构与技术路线变化
当出现以下任一情况时，必须更新 `ADR-001-tech-stack.md`，必要时同步更新 `ADR-003-module-split.md`：

- 改变系统形态  
  例如：从 B/S Web 改成桌面单机；从前后端分离改成前后端不分离
- 改变后端组织方式  
  例如：从单体后端改成微服务；从 Java Spring Boot 改成 Python FastAPI
- 改变数据库或核心存储方案  
  例如：从 MySQL 改成 PostgreSQL / Neo4j；增加对象存储作为正式依赖
- 改变认证与权限基础设施  
  例如：从 JWT 改成 Session；引入新的统一权限系统
- 改变公共能力拆分方式  
  例如：取消 `MapService`；把 `QueryService`、`RankService` 合并或拆成新的公共层
- 改变 AI 能力在系统中的定位  
  例如：AI 从 P2 增强改成 P0 主线依赖；或 AI 不再独立通过 `AIService` 封装

#### 3. 模块划分变化
当出现以下任一情况时，必须更新 `ADR-003-module-split.md`：

- 新增或删除一个主模块  
  例如：新增 `Review` 模块；取消 `Food` 模块
- 调整模块职责边界  
  例如：把“日记评分”从 Diary 模块移到 Recommend；把“设施查询”并入 Route
- 调整公共能力与业务模块的归属关系  
  例如：`SearchService` 不再只服务 Diary，而改成通用搜索层
- 调整模块优先级并影响开发顺序  
  例如：Admin 从 P1 提前为 P0

#### 4. 核心算法与数据结构变化
当出现以下任一情况时，必须更新 `ADR-004-algorithm-choice.md`：

- 更换某个核心功能的主算法  
  例如：单目标路径从 Dijkstra 改成 A* 为默认主算法
- 更换核心数据结构  
  例如：地图从有向带权图改成其他建模方式
- 改变“算法只是增强”与“算法是主实现”的边界  
  例如：推荐从“检索 + 排序 + Top-K”改成外部黑箱服务主导
- 将原本 P1 / P2 的算法能力提前为当前正式主线  
  例如：多目标路径规划提前为必须实现项

#### 5. 不需要更新 ADR 的情况
以下变化通常不需要新改 ADR，只需更新普通文档或代码：

- 文案润色、示例补充、截图更新
- 不影响范围和边界的字段命名优化
- 不影响系统形态的小版本依赖升级
- 不改变职责边界的小型重构
- 增加不影响主线的实现细节说明

## 14. Output Format for AI
每次输出建议遵循以下顺序：
1. 任务理解
2. 依赖检查
3. 需要读取的文档
4. 文件清单
5. 代码 / 修改内容
6. 运行步骤
7. 关键设计点
8. 需要同步更新的文档
9. 当前仍依赖哪些未完成内容

## 15. Preferred Scope
默认优先完成以下 P0 闭环，不要跳过基础主线先做花哨功能：
- Auth 基础版
- UserPreference 基础版
- Recommend 基础版
- Route 单目标基础版
- Facility 基础版
- Diary 基础版
- File 上传基础版

P1 再补：
- Food
- Route 多目标 / 最短时间
- Diary 评分 / 检索
- Admin
- ImportService
- SearchService

P2 再补：
- AI 增强能力
- 多人协同规划
- 外部地图 / 外部模型增强能力

## 16. Collaboration Notes
- 当前项目为 3 人协作
- 必须保持文档、代码、接口三者一致
- 优先服务当前模块开发，不要随意扩展系统边界
- 重要决策写入 `project-root/docs/09_decisions/`

## 17. 代码质量门禁
修改代码后，务必先运行最小范围的相关检查，再声明修改成功。

### 后端
- `mvn -q spotless:apply`
- `mvn -q checkstyle:check`
- `mvn -q pmd:check`
- `mvn -q test`

### 前端
- `npm run format`
- `npm run lint`
- `npm run type-check`
- `npm run test`

如果某个命令失败：
1. 说明失败原因；
2. 修复可以安全修复的问题；
3. 重新运行失败的检查；
4. 报告仍然存在的问题。

## 18. 当你不确定时
如果遇到以下情况：
- 文档冲突
- 当前模块依赖未完成
- 表结构字段不完整
- 接口路径未定
- 业务规则存在多个候选方案

你必须：
1. 明确指出冲突点
2. 给出最小影响的暂行方案
3. 用 `TODO` 或“待确认”标注
4. 不要偷偷替项目做最终决定

## 19. 你不应该做的事
1. 不要一次性生成整个系统
2. 不要把参考资料直接当项目事实
3. 不要默认所有模块都已经完成
4. 不要自己扩展新模块、新中间件、新架构
5. 不要为了代码完整而伪造数据库表、接口或业务字段
6. 不要忽视文档同步
7. 不要忽视安全规则
8. 不要输出“看起来很完整但跑不起来”的大段空心代码