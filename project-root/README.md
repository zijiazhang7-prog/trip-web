# 个性化旅游系统

## 1. 项目简介
本项目为《数据结构课程设计》课程项目，目标是实现一个面向旅游活动全流程的 Web 个性化旅游系统。  
系统覆盖旅行前推荐、旅行中路线规划与场所查询、旅行后旅游日记生成与交流，并结合 AI 能力实现创新功能。

## 2. 项目目标
- 实现旅游推荐、旅游路线规划、场所查询、旅游日记交流、美食推荐等核心功能
- 将排序、查找、最短路径、多点路径、全文检索、压缩等数据结构与算法落到真实系统中
- 使用智能体辅助需求分析、设计、开发、测试与文档整理
- 形成完整的课程设计文档、测试文档与交付材料

## 3. 小组成员与分工
- 成员A：后端 / 推荐模块 / API文档
- 成员B：算法 / 路线规划 / 数据导入
- 成员C：前端 / 日记模块 / 测试与交付文档

详细分工见：`docs/00_project/team-roles.md`

## 4. 系统形态与架构
- Web 远程系统
- B/S 架构
- 前后端分离
- 单体后端
- 算法模块化封装

## 5. 技术栈
### 前端
- Vue 3
- Element Plus
- Axios
- Vue Router / Pinia

### 后端
- Java 17
- Spring Boot 3
- MyBatis-Plus
- Maven

### 数据与脚本
- MySQL 8
- Python（数据清洗、导入、辅助脚本）

### AI / Agent 工具


## 6. 核心功能
### 旅行前
- 目的地推荐
- 景点 / 学校搜索
- 多维筛选与排序

### 旅行中
- 单目标路线规划
- 多目标路线规划
- 周边设施查询
- 美食推荐

### 旅行后
- 旅游日记发布与浏览
- 日记检索与排序
- AI 辅助日记生成 / 创新功能

## 7. 仓库结构
```text
project-root/
├─ README.md
├─ AGENTS.md
├─ CLAUDE.md
├─ coding-rules.md
├─ security-rules.md
├─ .gitignore
├─ frontend/
├─ backend/
├─ scripts/
├─ data/
├─ docs/
└─ assets/
```

## 8. 快速启动

### 前端

cd frontend  
npm install  
npm run dev

### 后端

cd backend  
mvn clean spring-boot:run

### 数据库

1. 启动 MySQL 8
2. 创建数据库 `tour_system`
3. 按 `docs/03_data/schema.md` 初始化表结构
4. 按 `docs/03_data/import-plan.md` 导入初始数据

## 9. 根目录关键文件说明

- `AGENTS.md`：AI 工具协作总规则
- `CLAUDE.md`：Claude Code 补充约定
- `coding-rules.md`：项目级编码规范
- `security-rules.md`：项目级安全规范
- `.gitignore`：Git 忽略规则

## 10. 关键文档入口

### 项目管理

- 项目总览：`docs/00_project/project-overview.md`
- 成员分工：`docs/00_project/team-roles.md`
- 项目进度：`docs/00_project/progress.md`
- 风险记录：`docs/00_project/risk-log.md`

### 需求与范围

- PRD：`docs/01_requirements/prd.md`
- 用例：`docs/01_requirements/use-cases.md`
- MVP范围：`docs/01_requirements/scope-mvp.md`
- 术语表：`docs/01_requirements/glossary.md`

### 架构与设计

- 总体架构：`docs/02_architecture/architecture.md`
- 模块划分：`docs/02_architecture/module-map.md`
- 依赖关系：`docs/02_architecture/dependency-map.md`
- 技术栈：`docs/02_architecture/tech-stack.md`

### 数据与接口

- 数据库设计：`docs/03_data/schema.md`
- 数据字典：`docs/03_data/data-dictionary.md`
- ER图：`docs/03_data/er-model.md`
- API文档：`docs/04_api/api-spec.md`
- Swagger草稿：`docs/04_api/swagger-draft.yaml`

### 模块设计

- 认证模块：`docs/05_modules/auth-module.md`
- 推荐模块：`docs/05_modules/recommend-module.md`
- 路线模块：`docs/05_modules/route-module.md`
- 日记模块：`docs/05_modules/diary-module.md`
- AI模块：`docs/05_modules/ai-module.md`

### 测试与交付

- 测试计划：`docs/06_testing/test-plan.md`
- 测试用例：`docs/06_testing/test-cases.md`
- 测试报告：`docs/06_testing/test-report.md`
- 用户手册：`docs/07_delivery/user-manual.md`
- 演示脚本：`docs/07_delivery/demo-script.md`

### AI 协作

- AI 使用记录：`docs/08_ai/ai-usage.md`
- 提示词库：`docs/08_ai/prompt-library.md`
- 工具说明：`docs/08_ai/tool-guide.md`
- 上下文文件：`docs/08_ai/context-files.md`

### 决策记录

- 技术选型：`docs/09_decisions/ADR-001-tech-stack.md`
- MVP 范围：`docs/09_decisions/ADR-002-mvp-scope.md`
- 模块拆分：`docs/09_decisions/ADR-003-module-split.md`
- 算法选择：`docs/09_decisions/ADR-004-algorithm-choice.md`

## 11. 参考资料目录

- `docs/08_ai/references.md：外部参考规范、标准、资料索引

> 注意：参考资料不是项目最终事实来源，项目事实以本仓库当前已确认文档为准。

## 12. 当前开发约定

- 一次只实现一个模块，不一次性生成整个系统
- 优先保证最小可运行版本，再逐步增强
- 修改数据库/API/模块依赖后必须同步文档
- 重要设计决策记录在 `docs/09_decisions/`
- 编码与安全规则分别遵循根目录 `coding-rules.md`、`security-rules.md`

## 13. 课程说明

本项目为 3 人小组课程设计，需按软件开发阶段推进，并在开发过程中持续积累完整文档，最终提交源代码、可执行程序和文档材料。
