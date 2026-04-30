## 1. 文件用途说明

本文件用于集中整理本项目会参考的外部规范、官方文档和工具资料索引。  
作用包括：

1. 供组员快速查找外部参考资料
2. 供 AI 在需要更详细规则时进行定向查阅
3. 作为项目级参考资料索引，不直接替代本地规则文件

### 使用原则
- 项目本地规则优先于外部参考资料
- 外部资料仅作为补充说明和查阅来源
- 若外部资料与项目本地规则冲突，以本地规则为准
- `references.md` 仅作为外部参考资料索引，不作为当前项目事实来源

项目事实以仓库内已确认文档和根目录规则文件为准，优先参考：
- `project-root/AGENTS.md`
- `project-root/coding-rules.md`
- `project-root/security-rules.md`
- `project-root/docs/01_requirements/*`
- `project-root/docs/02_architecture/*`
- `project-root/docs/03_data/*`
- `project-root/docs/04_api/*`
- `project-root/docs/05_modules/*`
- `project-root/docs/09_decisions/*`

---

## 2. 推荐使用方式

### 2.1 对人
先看本地项目规则：
- `README.md`
- `AGENTS.md`
- `coding-rules.md`
- `security-rules.md`

本文件用于：
- 查规范出处
- 查官方文档
- 查工具说明
- 查外部资料索引

### 2.2 对 AI
在需要 AI 查阅外部参考资料时，可明确提示：

- 先遵守本地项目规则
- 如果本地规则未覆盖，再参考 `references.md` 中对应资料
- 只查与当前任务直接相关的资料，不要泛化扩展
- 如果当前任务属于 P1 / P2，先确认它是否真的进入当前实现范围

---

## 3. 项目基础说明

项目名称：个性化旅游系统课程设计

当前项目技术路线：
- 前端：Vue 3 + Vite + Element Plus + Vue Router + Pinia + Axios
- 后端：Java 17 + Spring Boot 3 + MyBatis-Plus + Maven + Spring Security + JWT
- 数据库：MySQL 8
- 文件：本地文件存储 / 统一文件目录
- 脚本：Python + pandas
- 协作：GitHub
- 文档：Markdown
- AI 工具：Claude Code / OpenClaw / ChatGPT / Codex

当前项目边界说明：
- 当前主线优先级以 P0 / P1 / P2 为准
- 当前 P0 主线不依赖外部地图服务或外部 AI 服务才能跑通
- 外部地图 / 外部 AI 服务当前属于增强能力和选型预留，不作为当前硬依赖
- 若需要确认主线范围，优先看：
  - `project-root/docs/01_requirements/scope-mvp.md`
  - `project-root/docs/04_api/external-integrations.md`
  - `project-root/docs/09_decisions/ADR-002-mvp-scope.md`

---

## 4. 参考资料索引

---

### 4.1 课程要求与项目背景

#### 数据结构课程设计讲义
- 用途：确认课程要求、功能范围、验收方向、文档要求
- 优先级：最高
- 索引：
- 备注：所有方案设计和开发范围必须先服从课程讲义

#### 项目需求分析相关资料
- 用途：补充项目背景、用户痛点、MVP 范围
- 优先级：高
- 索引：
  - `project-root/docs/01_requirements/prd.md`
  - `project-root/docs/01_requirements/use-cases.md`
  - `project-root/docs/01_requirements/scope-mvp.md`
  - `project-root/docs/00_project/project-overview.md`
- 备注：用于需求分析，不直接作为编码规范

---

### 4.2 文档编写规范

#### GB/T 9385 软件需求规格说明规范
- 用途：编写需求规格说明、需求分析文档
- 优先级：高
- 索引：`project-root/docs/10_references/GB.T 9385-2008《计算机软件需求规格说明规范》.pdf`
- 备注：主要用于需求分析相关文档编写；若项目中保留了本地 PDF，以仓库实际路径为准

#### GB/T 9386 软件测试文档编制规范
- 用途：编写测试计划、测试用例、测试报告
- 优先级：高
- 索引：`project-root/docs/10_references/GB.T 9386-2008《计算机软件测试文档编制规范》.pdf`
- 备注：主要用于测试与验收文档编写；若项目中保留了本地 PDF，以仓库实际路径为准

#### GB/T 8567 软件文档编制规范
- 用途：规范整体项目文档结构与编写内容
- 优先级：高
- 索引：`project-root/docs/10_references/GB.T 8567-2006《计算机软件文档编制规范》.pdf`
- 备注：作为整体文档框架参考；若项目中保留了本地 PDF，以仓库实际路径为准

---

### 4.3 Java 代码风格与工程规范

#### Google Java Style Guide
- 用途：Java 命名、格式、类结构、代码风格基线
- 优先级：中
- 链接：`https://google.github.io/styleguide/javaguide.html`
- 备注：适合格式和命名风格参考

#### 阿里巴巴 Java 开发手册
- 用途：Java 工程实践、异常处理、数据库访问、日志规范
- 优先级：高
- 索引：`docs/10_references/Java开发手册(黄山版).pdf`
- 备注：更适合工程规则参考；若项目中保留了本地 PDF，以仓库实际路径为准

#### Maven 官方文档
- 用途：依赖管理、构建流程、项目结构
- 优先级：中
- 链接：`https://maven.apache.org/guides/`
- 备注：用于解决 Maven 配置、依赖、构建问题

#### Spring Boot 官方文档
- 用途：项目初始化、配置、日志、数据库连接、Web 开发
- 优先级：高
- 链接：`https://docs.spring.io/spring-boot/index.html`
- 备注：后端实现的重要官方来源

#### MyBatis-Plus 官方文档
- 用途：持久层开发、CRUD、分页、条件构造器
- 优先级：高
- 链接：`https://baomidou.com/en/`
- 备注：后端数据库访问优先参考来源

---

### 4.4 前端开发规范与文档

#### Vue 3 官方文档
- 用途：组件开发、组合式 API、项目结构
- 优先级：高
- 链接：`https://vuejs.org/`
- 备注：前端开发首要参考来源

#### Vue Router 官方文档
- 用途：路由配置、页面跳转、嵌套路由
- 优先级：高
- 链接：`https://router.vuejs.org/`
- 备注：用于前端页面路由设计

#### Pinia 官方文档
- 用途：前端状态管理
- 优先级：中
- 链接：`https://pinia.vuejs.org/`
- 备注：用于共享状态管理

#### Element Plus 官方文档
- 用途：前端 UI 组件查阅
- 优先级：高
- 链接：`https://element-plus.org/`
- 备注：页面组件优先复用，不重复造轮子

#### Axios 官方文档
- 用途：前端 HTTP 请求封装
- 优先级：中
- 链接：`https://axios-http.com/docs/intro`
- 备注：接口联调时参考

---

### 4.5 数据库与 SQL 参考

#### MySQL 官方文档
- 用途：数据库语法、建表、索引、查询、字符集
- 优先级：高
- 链接：`https://dev.mysql.com/doc/`
- 备注：涉及 SQL 语法与数据库问题时优先查

#### 数据库设计参考资料
- 用途：ER 图、关系建模、索引设计
- 优先级：中
- 链接：
  - `https://dev.mysql.com/doc/workbench/en/wb-data-modeling.html`
  - `https://dev.mysql.com/doc/workbench/en/wb-eer-diagrams-section.html`
- 备注：辅助数据库表设计

---

### 4.6 安全规范

#### OWASP Cheat Sheet Series
- 用途：输入校验、认证授权、SQL 注入防护、日志脱敏、文件上传安全
- 优先级：高
- 链接：`https://cheatsheetseries.owasp.org/`
- 备注：安全问题优先查这里，不要无依据自行发挥

#### Java Security Cheat Sheet
- 用途：Java 项目安全实现细节
- 优先级：高
- 链接：`https://cheatsheetseries.owasp.org/cheatsheets/Java_Security_Cheat_Sheet.html`
- 备注：适合后端安全实现参考

---

### 4.7 自动检查与代码质量工具

#### Checkstyle 官方文档
- 用途：Java 风格检查
- 优先级：中
- 链接：`https://checkstyle.sourceforge.io/`
- 备注：用于统一代码风格

#### PMD 官方文档
- 用途：发现代码坏味道、空 catch、未使用变量等问题
- 优先级：中
- 链接：`https://pmd.github.io/`
- 备注：用于静态检查

#### Spotless 官方文档
- 用途：格式化 Java、Markdown、JSON、YAML 等文件
- 优先级：中
- 链接：`https://github.com/diffplug/spotless`
- 备注：用于统一格式

---

### 4.8 GitHub 协作与项目管理

#### GitHub Docs
- 用途：仓库、分支、Issue、PR、Projects、Actions
- 优先级：高
- 链接：`https://docs.github.com/en`
- 备注：协作流程问题优先查官方文档

#### Git 官方文档
- 用途：分支、合并、rebase、stash、冲突处理
- 优先级：中
- 链接：`https://git-scm.com/docs`
- 备注：基础 Git 操作参考

---

### 4.9 AI coding 工具与使用说明

#### OpenAI Codex CLI 官方文档
- 用途：CLI 使用、模式说明、规则文件、审批机制
- 优先级：高
- 链接：
  - `https://developers.openai.com/codex/cli`
  - `https://developers.openai.com/codex/cli/reference`
  - `https://developers.openai.com/codex/guides/agents-md`
- 备注：使用 Codex 时优先查官方文档

#### Qoder 官方文档
- 用途：模型接入、命令行使用、上下文规则
- 优先级：中
- 链接：
  - `https://docs.qoder.com/`
  - `https://docs.qoder.com/quick-start`
  - `https://docs.qoder.com/cli/quick-start`
- 备注：使用 Qoder 时优先查官方文档

#### 项目本地 AI 协作文档
- 用途：理解本项目中 AI 怎么参与开发、如何喂上下文、如何分工
- 优先级：最高（高于外部工具文档）
- 索引：
  - `project-root/AGENTS.md`
  - `project-root/docs/08_ai/tool-guide.md`
  - `project-root/docs/08_ai/context-files.md`
  - `project-root/docs/08_ai/prompt-library.md`
  - `project-root/docs/08_ai/ai-usage.md`
- 备注：先遵守本地 AI 协作文档，再看外部工具文档

---

### 4.10 外部服务与能力集成参考

#### 外部地图 / 外部 AI 服务参考
- 用途：在后续正式选型后，用于补充地图服务或 AI 模型服务的具体接入资料
- 优先级：中
- 备注：
  - 当前厂商未最终确定
  - 当前项目不应在本文件中预先固定某一家厂商为正式依赖
  - 真正开始选型或接入前，必须先看：
    - `project-root/docs/04_api/external-integrations.md`
    - `project-root/docs/09_decisions/ADR-001-tech-stack.md`
    - `project-root/docs/09_decisions/ADR-002-mvp-scope.md`

---

## 5. AI 查阅外部资料时的提示模板

### 模板 1：只查一个主题
请先遵守本地项目规则。  
如果本地规则未覆盖，请只参考 `references.md` 中“[这里填主题]”部分的资料，查找与当前任务直接相关的内容。  
不要扩展到无关页面，不要泛化输出。

### 模板 2：写代码前查规范
请先阅读：
1. `project-root/AGENTS.md`
2. `project-root/coding-rules.md`
3. `project-root/security-rules.md`

如果仍缺少细节，再参考 `references.md` 中与当前任务相关的官方资料。  
写代码时以本地规则优先，外部资料仅作补充。

### 模板 3：写文档前查规范
请先阅读本地文档模板。  
如果需要外部规范支持，请只参考 `references.md` 中“文档编写规范”部分的资料。  
输出内容要尽量贴合本项目，而不是照抄标准原文。

---

## 6. 维护规则

1. 每新增一类重要外部资料，补充到本文件中
2. 每项资料必须写清“用途”和“备注”
3. 不要无分类地堆资料
4. 优先保留官方文档和权威来源
5. 若某资料路径或链接失效，及时替换
6. 如果项目本地规则已经覆盖某类问题，应减少对外部资料的依赖
7. 本文件应与 `AGENTS.md`、`README.md`、`context-files.md`、`tool-guide.md` 保持一致