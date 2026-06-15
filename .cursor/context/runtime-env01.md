# 运行环境与请求约定（Runtime Environment）

## A. Backend Confirmed Facts

| 项 | 当前确认事实 | 来源证据 |
|---|---|---|
| 本地开发端口 | `8080` | `project-root/backend/src/main/resources/application-dev.yml` |
| context-path | 未配置独立 `server.servlet.context-path` | `application.yml`、`application-dev.yml` |
| 实际接口前缀 | 业务接口直接以 `/api/v1` 开头 | 各 Controller 的 `@RequestMapping` / `@GetMapping` |
| 静态文件访问前缀 | `/files` | `application-dev.yml` 的 `trip.file.access-prefix`，`FileResourceConfig.java` |
| 数据源配置位置 | `application-dev.yml` | `spring.datasource.*` |
| 本地数据库 URL | `${DB_URL:jdbc:mysql://localhost:3306/tour_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai}` | `application-dev.yml` |
| 数据库用户名 | `${DB_USERNAME:root}` | `application-dev.yml` |
| 数据库密码 | `${DB_PASSWORD:}`，不在文档记录真实密码 | `application-dev.yml` |
| active profile | 默认 `dev` | `application.yml` |
| dev 配置文件 | `project-root/backend/src/main/resources/application-dev.yml` | 文件存在 |
| test/prod profile | 未找到独立 `application-test.yml` / `application-prod.yml` | resources 扫描结果 |
| multipart 限制 | 默认 5MB，可由 `FILE_MAX_SIZE` 覆盖 | `application-dev.yml` |
| 上传目录 | `${FILE_UPLOAD_DIR:uploads}` | `application-dev.yml` |
| JWT secret | `${JWT_SECRET:dev-only-change-me-to-a-long-random-secret}` | `application-dev.yml` |
| JWT 过期时间 | `${JWT_EXPIRE_MINUTES:120}` 分钟 | `application-dev.yml` |
| 登录 token 返回位置 | `data.token` | `LoginResponse.java`、`AuthController.java`、`api-spec.md` 7.2 |
| 当前用户信息返回位置 | `data` 内为 `UserVO` | `AuthController.java`、`UserVO.java` |
| 后端期望鉴权头 | `Authorization: Bearer <token>` | `JwtAuthenticationFilter.java`、`AuthServiceImpl.java` |
| 鉴权机制 | Spring Security stateless + 自定义 JWT filter | `SecurityConfig.java`、`JwtAuthenticationFilter.java` |
| CORS | 允许 `http://localhost:5173`、`http://127.0.0.1:5173`，方法 `GET/POST/PUT/DELETE/OPTIONS`，允许 credentials | `CorsConfig.java` |
| Swagger/springdoc 运行配置 | 未在后端配置中找到 springdoc 依赖或配置；存在手写 `docs/04_api/swagger-draft.yaml` | resources 与 `pom.xml` 扫描、`swagger-draft.yaml` |
| 统一返回体 | `success/code/message/data/timestamp` | `ApiResponse.java` |
| 统一分页体 | `list/pageNum/pageSize/total/pages` | `PageResultVO.java` |

## B. Frontend Confirmed Facts (From codebase)

以下内容基于对 `project-root/frontend/` 目录的代码扫描确认：

| 项 | 当前确认事实 | 来源证据 |
|---|---|---|
| 前端技术栈 | Vite + React + TypeScript | `vite.config.ts`、`src/main.tsx` |
| 路由模式 | BrowserRouter（非 hash 路由） | `src/App.tsx` |
| API 层现状 | **mock-first，尚未接入真实 HTTP** | `src/api/diary.ts`、`src/api/ai.ts` |
| 具体 mock 实现 | `getDiaryApi()` 全部指向 mock 函数；`getAiApi()` 同样指向 mock | `src/api/diary.ts`、`src/api/ai.ts` |
| baseURL 配置 | ❌ 未配置真实接口 baseURL | 代码扫描未发现 `axios.defaults.baseURL` 或环境变量配置 |
| Vite 代理 (proxy) | ❌ 未配置 `server.proxy` | `vite.config.ts` |
| 鉴权头 (Authorization) | ❌ 未实现统一请求拦截器 / 鉴权头注入 | 代码扫描未发现 Axios 拦截器 |
| token 存储策略 | ❌ 未实现 token 存储（localStorage/sessionStorage/cookie） | 代码扫描未发现相关逻辑 |
| 联调状态 | **前端仍处于 mock 阶段，尚未切换到后端服务** | 所有 API 调用均指向 mock 函数 |

### 前端联调待办清单

| 待办项 | 优先级 | 说明 |
|---|---|---|
| 安装/配置 Axios | 🔴 高 | 当前项目是否已有 Axios？若没有需安装 |
| 创建 Axios 实例并配置 baseURL | 🔴 高 | baseURL 应为 `http://localhost:8080/api/v1`（见 A 部分） |
| 配置 Vite 代理 | 🟡 中 | 解决本地开发跨域问题，代理 `/api` 到 `http://localhost:8080` |
| 实现请求拦截器 | 🔴 高 | 自动注入 `Authorization: Bearer <token>` |
| 实现响应拦截器 | 🟡 中 | 统一处理 401、错误提示等 |
| 确定 token 存储位置 | 🔴 高 | 推荐 `localStorage`，配合登录/登出逻辑 |
| 逐步替换 mock 为真实 API 调用 | 🔴 高 | 从最核心的业务接口开始，逐个切换 |

### 前端与后端联调时的关键对齐项

| 对齐项 | 后端约定（来自 A 部分） | 前端需实现 |
|---|---|---|
| baseURL | `http://localhost:8080/api/v1` | Axios 实例配置 |
| 鉴权头 | `Authorization: Bearer <token>` | 请求拦截器注入 |
| token 来源 | 登录接口返回 `data.token` | 存储到 localStorage，登录时保存，登出时清除 |
| CORS | 后端已允许 `localhost:5173` | 本地开发时前端端口需为 5173，或用 Vite 代理 |

## C. Joint Open Questions

| 问题 | 当前证据 | 需要确认对象 |
|---|---|---|
| 最终 API baseURL 是 `http://localhost:8080/api/v1` 还是通过前端 dev proxy 转发 | 后端实际接口前缀为 `/api/v1`；`swagger-draft.yaml` servers 写 `http://localhost:8080/api/v1`；前端当前未配置 baseURL/proxy | 前端负责人 / 后端负责人 |
| 是否启用 Vite dev proxy | 前端当前 `vite.config.ts` 未配置 `server.proxy` | 前端负责人 |
| token 存储策略 | 后端只要求 `Authorization: Bearer <token>`，未规定前端存储位置 | 前端负责人 |
| request interceptor 统一位置 | 前端当前未实现统一请求拦截器 | 前端负责人 |
| 测试服务器地址 | 当前只确认本地 `localhost:8080` | 项目负责人 / 部署负责人 |
| Swagger UI 是否需要运行时可访问 | 后端未找到 springdoc 配置，仅有 draft yaml | 后端负责人 |
| prod/test profile 是否需要补配置 | 当前仅有 dev profile | 后端负责人 |

## 待确认项
- 是否以 `/.cursor/context/frontend-runtime-facts.md` 作为唯一前端运行时事实来源并纳入评审门禁。
- 前端是否使用代理、Axios 实例、统一错误处理和 token 刷新策略。
- 联调时是否固定使用 `/api/v1` 相对路径，还是完整后端 URL。

## 代码/文档冲突项
- 文档有 `swagger-draft.yaml`，但后端未确认有 springdoc 运行时配置。
- 架构文档描述前端使用 Vue 3/Vite/Element Plus/Pinia/Axios，但当前没有前端运行时事实文件支撑，本文不把这些当作已确认运行事实。

## 建议下一步动作
- 补交 `frontend-runtime-facts.md`，至少写明 baseURL、proxy、token 存储、请求拦截器和当前 mock/真实 HTTP 状态。
- 若需要 Swagger UI，后端应明确是否引入 springdoc，并同步 `runtime-env.md`。
- 团队统一一份 `.env.example` 或联调说明，避免 DB/JWT/FILE 配置口径分散。