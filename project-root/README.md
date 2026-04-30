# 个性化旅游系统

## 1. 项目简介
本项目为《数据结构课程设计》课程项目，目标是实现一个面向旅游活动全流程的 Web 个性化旅游系统。  
系统围绕“旅行前推荐—旅行中规划与查询—旅行后日记记录与交流”构建完整功能链路，并在实现过程中体现排序、查找、图、最短路径、多点路径、全文检索、压缩等数据结构与算法能力。

## 2. 当前项目目标
- 实现一个可远程访问的 Web 个性化旅游系统
- 先跑通当前 P0 主线闭环
- 形成前后端、数据库、接口、测试与文档的一致版本
- 为后续 P1 / P2 增强能力与创新能力预留扩展空间
- 形成可用于中期汇报与最终课程验收的代码和材料

## 3. 当前范围说明

### P0（当前优先）
- 用户注册 / 登录
- 用户偏好输入
- 目的地推荐与搜索
- 单目标路线规划
- 周边设施查询
- 图文日记发布 / 浏览 / 详情
- 文件上传与媒体访问

### P1（主线跑通后补）
- 美食推荐
- 多目标路径规划
- 最短时间策略
- 日记评分
- 日记检索
- 管理端基础数据维护
- 导入流程完善

### P2（后续增强）
- AI 日记草稿
- 图片摘要
- 路线回顾
- 推荐理由解释
- 多人旅游规划协商
- 外部地图 / 外部模型服务增强接入

## 4. 小组成员与分工
- 成员 A：后端 / Auth / UserPreference / Recommend / API 文档
- 成员 B：算法 / Route / Facility / 数据导入
- 成员 C：前端 / Diary / 测试与交付文档

详细分工见：`project-root/docs/00_project/team-roles.md`

## 5. 系统形态与架构
- Web 远程系统
- B/S 架构
- 前后端分离
- 单体后端
- 算法模块化封装

## 6. 技术栈

### 前端
- Vue 3
- Vite
- Element Plus
- Axios
- Vue Router
- Pinia

### 后端
- Java 17
- Spring Boot 3
- MyBatis-Plus
- Maven
- Spring Security + JWT

### 数据与脚本
- MySQL 8
- 本地文件存储 / 统一文件目录
- Python
- pandas

### AI / Agent 工具
- Claude Code
- OpenClaw
- ChatGPT
- Codex

## 7. 仓库结构

```text
trip-web/
├─ project-root/
│  ├─ README.md
│  ├─ AGENTS.md
│  ├─ CLAUDE.md
│  ├─ coding-rules.md
│  ├─ security-rules.md
│  ├─ frontend/
│  ├─ backend/
│  ├─ scripts/
│  ├─ data/
│  ├─ docs/
│  └─ assets/
│  
├─ .gitignore
├─ README.md
└─ LICENSE
```

## 8. 快速启动

### 前端

```
cd frontendnpm installnpm run dev
```

### 后端

```
cd backendmvn clean spring-boot:run
```

### 数据库

1. 启动 MySQL 8
2. 创建数据库 `tour_system`
3. 按 `project-root/docs/03_data/schema.md` 初始化表结构
4. 按导入方案准备基础样例数据

## 9. project-root/根目录关键文件说明

- `AGENTS.md`：AI 工具协作总规则
- `CLAUDE.md`：Claude Code 补充约定
- `coding-rules.md`：项目级编码规范
- `security-rules.md`：项目级安全规范

## 10. 关键文档入口

### 项目管理

- 项目总览：`project-root/docs/00_project/project-overview.md`
- 成员分工：`project-root/docs/00_project/team-roles.md`
- 项目进度：`project-root/docs/00_project/progress.md`
- 风险记录：`project-root/docs/00_project/risk-log.md`

### 需求与范围

- PRD：`project-root/docs/01_requirements/prd.md`
- 用例：`project-root/docs/01_requirements/use-cases.md`
- MVP 范围：`project-root/docs/01_requirements/scope-mvp.md`
- 术语表：`project-root/docs/01_requirements/glossary.md`

### 架构与设计

- 总体架构：`project-root/docs/02_architecture/architecture.md`
- 技术栈：`project-root/docs/02_architecture/tech-stack.md`
- 模块划分：`project-root/docs/02_architecture/module-map.md`
- 依赖关系：`project-root/docs/02_architecture/dependency-map.md`
- 外部集成：`project-root/docs/04_api/external-integrations.md`

### 数据与接口

- 数据库设计：`project-root/docs/03_data/schema.md`
- 数据字典：`project-root/docs/03_data/data-dictionary.md`
- ER 图：`project-root/docs/03_data/er-model.md`
- API 文档：`project-root/docs/04_api/api-spec.md`
- Swagger 草稿：`project-root/docs/04_api/swagger-draft.yaml`

### 模块设计

- 认证模块：`project-root/docs/05_modules/auth-module.md`
- 用户偏好模块：`project-root/docs/05_modules/user-preference-module.md`
- 推荐模块：`project-root/docs/05_modules/recommend-module.md`
- 路线模块：`project-root/docs/05_modules/route-module.md`
- 日记模块：`project-root/docs/05_modules/diary-module.md`
- AI 模块：`project-root/docs/05_modules/ai-module.md`

### 测试与交付

- 测试计划：`project-root/docs/06_testing/test-plan.md`
- 测试用例：`project-root/docs/06_testing/test-cases.md`
- 测试报告：`project-root/docs/06_testing/test-report.md`
- 用户手册：`project-root/docs/07_delivery/user-manual.md`
- 演示脚本：`project-root/docs/07_delivery/demo-script.md`

### AI 协作

- AI 使用记录：`project-root/docs/08_ai/ai-usage.md`
- 提示词库：`project-root/docs/08_ai/prompt-library.md`
- 工具说明：`project-root/docs/08_ai/tool-guide.md`
- 上下文文件：`project-root/docs/08_ai/context-files.md`

### 决策记录

- 技术选型：`project-root/docs/09_decisions/ADR-001-tech-stack.md`
- MVP 范围：`project-root/docs/09_decisions/ADR-002-mvp-scope.md`
- 模块拆分：`project-root/docs/09_decisions/ADR-003-module-split.md`
- 算法选择：`project-root/docs/09_decisions/ADR-004-algorithm-choice.md`

## 11. 当前开发约定

- 一次只实现一个模块，不一次性生成整个系统
- 优先保证 P0 主线跑通，再逐步增强
- 修改数据库 / API / 模块依赖后必须同步文档
- 重要设计决策记录在 `project-root/docs/09_decisions/`
- 编码与安全规则分别遵循`project-root/`根目录 `coding-rules.md`、`security-rules.md`
- 外部地图与外部 AI 服务当前只做预留，不作为当前主链路硬依赖

## 12. 当前项目状态

当前项目已完成项目前期核心文档整理，当前正准备按 P0 主线进入正式开发与联调阶段。

## 13. 课程说明

本项目为 3 人小组课程设计，需按软件开发阶段推进，并在开发过程中持续积累完整文档，最终提交源代码、可执行程序和文档材料。