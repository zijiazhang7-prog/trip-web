- 作用：说明“哪些文件是 AI 的上下文源”。

## 1. 文件用途

本文件用于说明：**AI 在进入本项目时，应该优先阅读哪些本地文件作为上下文。**

目标：
1. 统一 AI 进入项目时的阅读顺序
2. 降低不同工具输出不一致的问题
3. 让组员知道“哪些文档必须先补齐”
4. 提高 AI 生成代码、文档和测试时的稳定性

---

## 2. 总原则

### 2.1 上下文优先于猜测
AI 在本项目中，必须优先依据本地上下文文件工作，而不是凭通用经验猜测项目结构。

### 2.2 越靠近当前任务的文件，优先级越高
- 当前模块文档
- 当前目录下说明文件
- 项目级规则文件
- 外部参考资料

### 2.3 不是所有文件都要一开始读完
应按任务类型分层读取，而不是每次把整个仓库全扫一遍。

### 2.4 当前阶段必须优先服从项目已冻结范围
当前项目已经冻结了 P0 / P1 / P2 范围。  
AI 读取上下文时，应首先确认：

- 当前任务属于 P0 / P1 / P2 的哪一层
- 当前任务是否已经有对应模块文档
- 当前任务是否依赖尚未完成的前置模块

如果当前任务属于 P1 / P2，AI 不应默认把它提升为当前主线阻塞项。

---

## 3. AI 进入项目时的默认阅读顺序

## 第一层：项目总入口（必须优先读）
1. `project-root/README.md`
2. `project-root/AGENTS.md`
3. `project-root/coding-rules.md`
4. `project-root/security-rules.md`

### 作用说明
- `README.md`：让 AI 先知道项目是什么、目录怎么分布、从哪开始看
- `AGENTS.md`：告诉 AI 项目级执行规则、开发边界和当前优先级
- `coding-rules.md`：约束代码风格、工程结构、开发约束
- `security-rules.md`：约束安全、输入校验、日志、密钥等行为

---

## 第二层：项目背景与当前进度（建议优先读）
1. `project-root/docs/00_project/project-overview.md`
2. `project-root/docs/00_project/team-roles.md`
3. `project-root/docs/00_project/progress.md`
4. `project-root/docs/09_decisions/*.md`

### 作用说明
- `project-overview.md`：说明项目整体目标、当前范围和当前阶段状态
- `team-roles.md`：说明分工，避免 AI 把任务边界搞乱
- `progress.md`：告诉 AI 当前做到哪一步了
- `project-root/docs/09_decisions/*.md`：记录已经拍板的关键决策，防止 AI 反复推翻既定方案

---

## 第三层：需求与范围（做需求/设计/开发前需要读）
1. `project-root/docs/01_requirements/prd.md`
2. `project-root/docs/01_requirements/use-cases.md`
3. `project-root/docs/01_requirements/scope-mvp.md`
4. `project-root/docs/01_requirements/glossary.md`

### 作用说明
- `prd.md`：项目总体需求说明
- `use-cases.md`：用户场景和业务路径
- `scope-mvp.md`：当前阶段做什么、不做什么
- `glossary.md`：统一术语，避免 AI 乱造名称

---

## 第四层：架构与模块（做系统设计、代码实现前需要读）
1. `project-root/docs/02_architecture/architecture.md`
2. `project-root/docs/02_architecture/tech-stack.md`
3. `project-root/docs/02_architecture/module-map.md`
4. `project-root/docs/02_architecture/dependency-map.md`
5. `project-root/docs/02_architecture/coding-plan.md`
6. `project-root/docs/04_api/external-integrations.md`

### 作用说明
- `architecture.md`：系统总体结构
- `tech-stack.md`：技术选型
- `module-map.md`：模块划分
- `dependency-map.md`：模块依赖关系和实现顺序
- `coding-plan.md`：开发顺序和实现策略
- `external-integrations.md`：外部地图 / 外部 AI 服务的接入边界、降级策略和配置预留

---

## 第五层：数据与接口（做后端、联调、测试前需要读）
1. `project-root/docs/03_data/schema.md`
2. `project-root/docs/03_data/data-dictionary.md`
3. `project-root/docs/03_data/er-model.md`
4. `project-root/docs/03_data/data-source.md`
5. `project-root/docs/03_data/import-plan.md`
6. `project-root/docs/04_api/api-spec.md`
7. `project-root/docs/04_api/error-codes.md`
8. `project-root/docs/04_api/api-changelog.md`
9. `project-root/docs/04_api/swagger-draft.yaml`

### 作用说明
- `schema.md`：数据库整体结构
- `data-dictionary.md`：字段语义
- `er-model.md`：实体关系
- `data-source.md`：数据来源
- `import-plan.md`：导入策略
- `api-spec.md`：接口定义
- `error-codes.md`：统一错误码
- `api-changelog.md`：接口变更记录
- `swagger-draft.yaml`：结构化接口定义

---

## 第六层：模块专属文档（做某个模块时必须读）
1. `project-root/docs/05_modules/auth-module.md`
2. `project-root/docs/05_modules/user-preference-module.md`
3. `project-root/docs/05_modules/recommend-module.md`
4. `project-root/docs/05_modules/route-module.md`
5. `project-root/docs/05_modules/facility-module.md`
6. `project-root/docs/05_modules/food-module.md`
7. `project-root/docs/05_modules/diary-module.md`
8. `project-root/docs/05_modules/ai-module.md`
9. `project-root/docs/05_modules/admin-module.md`

### 作用说明
当前任务属于哪个模块，就必须优先阅读该模块文档。  
原则上：
- 不做推荐模块，就不用先深读 `recommend-module.md`
- 不做路线规划，就不用先深读 `route-module.md`
- 不做用户偏好模块，就不用先深读 `user-preference-module.md`

---

## 第七层：测试与交付（做测试、答辩材料前需要读）
1. `project-root/docs/06_testing/test-plan.md`
2. `project-root/docs/06_testing/test-cases.md`
3. `project-root/docs/06_testing/bug-log.md`
4. `project-root/docs/06_testing/test-report.md`
5. `project-root/docs/06_testing/performance-notes.md`
6. `project-root/docs/07_delivery/user-manual.md`
7. `project-root/docs/07_delivery/demo-script.md`
8. `project-root/docs/07_delivery/acceptance-checklist.md`
9. `project-root/docs/07_delivery/improvement-notes.md`
10. `project-root/docs/07_delivery/final-report-outline.md`

---

## 第八层：AI 协作专属文件（需要时读）
1. `project-root/docs/08_ai/prompt-library.md`
2. `project-root/docs/08_ai/ai-usage.md`
3. `project-root/docs/08_ai/tool-guide.md`
4. `project-root/docs/08_ai/context-files.md`
5. `project-root/docs/08_ai/references.md`

### 作用说明
- `prompt-library.md`：提供可复用提示词
- `ai-usage.md`：说明 AI 怎么参与项目
- `tool-guide.md`：说明工具分工
- `context-files.md`：说明上下文来源
- `references.md`：外部资料索引，只作补充参考，不作为项目事实来源

---

## 4. 按任务类型推荐阅读顺序

## 4.1 做需求分析 / 项目计划时
建议优先阅读：
1. `project-root/README.md`
2. `project-root/AGENTS.md`
3. `project-root/docs/00_project/project-overview.md`
4. `project-root/docs/01_requirements/prd.md`
5. `project-root/docs/01_requirements/use-cases.md`
6. `project-root/docs/01_requirements/scope-mvp.md`
7. `project-root/docs/09_decisions/*.md`

---

## 4.2 做架构设计时
建议优先阅读：
1. `project-root/README.md`
2. `project-root/AGENTS.md`
3. `project-root/docs/01_requirements/prd.md`
4. `project-root/docs/02_architecture/architecture.md`
5. `project-root/docs/02_architecture/module-map.md`
6. `project-root/docs/02_architecture/dependency-map.md`
7. `project-root/docs/04_api/external-integrations.md`
8. `project-root/docs/09_decisions/*.md`

---

## 4.3 做数据库设计时
建议优先阅读：
1. `project-root/README.md`
2. `project-root/AGENTS.md`
3. `project-root/docs/01_requirements/prd.md`
4. `project-root/docs/03_data/schema.md`
5. `project-root/docs/03_data/data-dictionary.md`
6. `project-root/docs/03_data/er-model.md`
7. `project-root/docs/05_modules/当前模块.md`

---

## 4.4 做接口设计或后端开发时
建议优先阅读：
1. `project-root/README.md`
2. `project-root/AGENTS.md`
3. `project-root/coding-rules.md`
4. `project-root/security-rules.md`
5. `project-root/docs/02_architecture/module-map.md`
6. `project-root/docs/02_architecture/dependency-map.md`
7. `project-root/docs/03_data/schema.md`
8. `project-root/docs/04_api/api-spec.md`
9. `project-root/docs/05_modules/当前模块.md`

---

## 4.5 做前端开发时
建议优先阅读：
1. `project-root/README.md`
2. `project-root/AGENTS.md`
3. `project-root/coding-rules.md`
4. `project-root/docs/01_requirements/prd.md`
5. `project-root/docs/02_architecture/architecture.md`
6. `project-root/docs/04_api/api-spec.md`
7. `project-root/docs/05_modules/当前模块.md`
8. `project-root/docs/07_delivery/user-manual.md`（如页面面向最终用户）

---

## 4.6 做测试时
建议优先阅读：
1. `project-root/README.md`
2. `project-root/AGENTS.md`
3. `project-root/docs/04_api/api-spec.md`
4. `project-root/docs/05_modules/当前模块.md`
5. `project-root/docs/06_testing/test-plan.md`
6. `project-root/docs/06_testing/test-cases.md`
7. `project-root/docs/06_testing/bug-log.md`

---

## 4.7 做答辩与交付时
建议优先阅读：
1. `project-root/README.md`
2. `project-root/docs/00_project/project-overview.md`
3. `project-root/docs/01_requirements/prd.md`
4. `project-root/docs/02_architecture/architecture.md`
5. `project-root/docs/03_data/schema.md`
6. `project-root/docs/04_api/api-spec.md`
7. `project-root/docs/06_testing/test-report.md`
8. `project-root/docs/07_delivery/demo-script.md`
9. `project-root/docs/07_delivery/acceptance-checklist.md`

---

## 4.8 做外部服务 / AI 增强时
建议优先阅读：
1. `project-root/README.md`
2. `project-root/AGENTS.md`
3. `project-root/docs/04_api/external-integrations.md`
4. `project-root/docs/05_modules/ai-module.md`
5. `project-root/docs/04_api/api-spec.md`
6. `project-root/docs/09_decisions/*.md`

说明：
- 当前外部地图服务和外部 AI 服务都不是 P0 主线硬依赖；
- 在未最终选型前，应优先保留抽象层、配置项、mock / stub 和降级逻辑，不要直接绑具体厂商。

---

## 5. 哪些文件最值得“喂给 AI”

如果当前只能给 AI 少量关键文件，优先级建议如下：

### 最高优先级
1. `project-root/AGENTS.md`
2. `project-root/coding-rules.md`
3. `project-root/security-rules.md`
4. `project-root/docs/01_requirements/prd.md`
5. `project-root/docs/01_requirements/scope-mvp.md`
6. `project-root/docs/02_architecture/module-map.md`
7. `project-root/docs/02_architecture/dependency-map.md`
8. `project-root/docs/04_api/external-integrations.md`
9. `project-root/docs/03_data/schema.md`
10. `project-root/docs/04_api/api-spec.md`
11. `project-root/docs/05_modules/当前模块.md`
12. `project-root/docs/00_project/progress.md`

### 第二优先级
1. `project-root/docs/03_data/data-dictionary.md`
2. `project-root/docs/06_testing/test-plan.md`
3. `project-root/docs/07_delivery/user-manual.md`
4. `project-root/docs/09_decisions/*.md`

### 第三优先级
1. `project-root/docs/08_ai/prompt-library.md`
2. `project-root/docs/08_ai/references.md`
3. 其他辅助资料

---

## 6. AI 阅读上下文时的建议提示词

### 模板 1：先读上下文再改代码
请在开始修改代码前，先阅读以下文件：
1. `project-root/README.md`
2. `project-root/AGENTS.md`
3. `project-root/coding-rules.md`
4. `project-root/security-rules.md`
5. 与当前任务直接相关的 docs 文档

请先告诉我：
- 你实际阅读了哪些文件
- 你理解到的当前任务是什么
- 你准备修改哪些文件
然后再开始修改。

### 模板 2：只读与当前模块有关的文件
请不要扫描整个仓库。  
只阅读与“<模块名>”直接相关的文件，包括：
- 项目级规则文件
- 当前模块文档
- 当前模块依赖的数据与接口文档
- 当前模块对应代码目录

阅读后先输出分析结果，不要立即修改。

### 模板 3：检查当前上下文是否足够
请先判断当前给你的上下文文件是否足够支撑实现“<任务名>”。

如果不足，请明确指出还缺哪些文件：
- 需求类
- 架构类
- 数据类
- 接口类
- 当前模块类
不要在上下文不足时直接猜着实现。

---

## 7. 维护规则

1. 如果 docs 目录结构变化，本文件必须同步更新
2. 如果新增高优先级上下文文件，本文件必须补充
3. 如果某些文件长期失效或不再使用，应及时移除
4. 本文件应与 `tool-guide.md`、`README.md`、`AGENTS.md` 保持一致
5. 如果项目的 P0 / P1 / P2 范围或模块优先级发生变化，本文件应同步检查是否需要更新
6. 每个组员在新增重要文档后，应考虑它是否需要进入本文件