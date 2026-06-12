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
| 动画 AI Provider | `${AI_ANIMATION_PROVIDER:mock-template}`，可选 `mock-template/openai-compatible` | `application-dev.yml`、`AiAnimationProperties.java` |
| AI 服务地址/密钥/模型 | `AI_API_BASE_URL`、`AI_API_KEY`、`AI_MODEL_NAME`；仓库不保存真实密钥 | `application-dev.yml` |
| AI 超时 | 连接默认 3 秒、读取默认 45 秒，可由 `AI_CONNECT_TIMEOUT/AI_READ_TIMEOUT` 覆盖 | `application-dev.yml` |
| AI 图片限制 | 默认最多 6 张、单图 2MB、总计 8MB | `AI_MAX_IMAGES`、`AI_MAX_IMAGE_SIZE`、`AI_MAX_TOTAL_IMAGE_SIZE` |
| AI 降级 | `${AI_ANIMATION_FALLBACK_ENABLED:true}`；真实 Provider 失败时回退 `mock-template` | `AIServiceImpl.java` |
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

## B. Frontend Confirmed Facts (From frontend-runtime-facts.md)

本次扫描未在仓库中找到 `frontend-runtime-facts.md`，因此不能确认以下内容：

| 项 | 当前结论 |
|---|---|
| 前端技术栈 | 待确认。项目文档描述为 Vue 3 + Vite + Element Plus，但不是来自 `frontend-runtime-facts.md` |
| 路由模式 | 待确认 |
| 当前 API 集成状态 | 待确认 |
| 当前 baseURL / proxy | 待确认 |
| 当前 token / auth header 处理 | 待确认 |
| 当前是否未接入真实 HTTP | 待确认 |
| 联调注意事项 | 待确认；不能伪造 Axios、Pinia、拦截器或本地存储策略 |

## C. Joint Open Questions

| 问题 | 当前证据 | 需要确认对象 |
|---|---|---|
| 最终 API baseURL 是 `http://localhost:8080/api/v1` 还是通过前端 dev proxy 转发 | 后端实际接口前缀为 `/api/v1`；`swagger-draft.yaml` servers 写 `http://localhost:8080/api/v1`；前端事实文件缺失 | 前端负责人 / 后端负责人 |
| 是否启用 Vite dev proxy | 无前端运行时事实来源 | 前端负责人 |
| token 存储策略 | 后端只要求 `Authorization: Bearer <token>`，未规定前端存储位置 | 前端负责人 |
| request interceptor 统一位置 | 无前端代码和运行时事实 | 前端负责人 |
| 测试服务器地址 | 当前只确认本地 `localhost:8080` | 项目负责人 / 部署负责人 |
| Swagger UI 是否需要运行时可访问 | 后端未找到 springdoc 配置，仅有 draft yaml | 后端负责人 |
| prod/test profile 是否需要补配置 | 当前仅有 dev profile | 后端负责人 |

## 待确认项
- `frontend-runtime-facts.md` 的真实路径或是否尚未提交。
- 前端是否使用代理、Axios 实例、统一错误处理和 token 刷新策略。
- 联调时是否固定使用 `/api/v1` 相对路径，还是完整后端 URL。

## 代码/文档冲突项
- 文档有 `swagger-draft.yaml`，但后端未确认有 springdoc 运行时配置。
- 架构文档描述前端使用 Vue 3/Vite/Element Plus/Pinia/Axios，但当前没有前端运行时事实文件支撑，本文不把这些当作已确认运行事实。

## 建议下一步动作
- 补交 `frontend-runtime-facts.md`，至少写明 baseURL、proxy、token 存储、请求拦截器和当前 mock/真实 HTTP 状态。
- 若需要 Swagger UI，后端应明确是否引入 springdoc，并同步 `runtime-env.md`。
- 团队统一一份 `.env.example` 或联调说明，避免 DB/JWT/FILE 配置口径分散。
