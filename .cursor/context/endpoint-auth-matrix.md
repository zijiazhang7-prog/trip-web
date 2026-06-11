# 接口权限矩阵（Endpoint Auth Matrix）

## 1. 判定依据
- `project-root/backend/src/main/java/com/trip/config/SecurityConfig.java`
- `project-root/backend/src/main/java/com/trip/security/JwtAuthenticationFilter.java`
- `project-root/backend/src/main/java/com/trip/security/JwtTokenProvider.java`
- 各 Controller 映射路径
- `project-root/docs/04_api/api-spec.md`

## 2. 全局鉴权规则

| 规则 | 结论 | 证据 |
|---|---|---|
| 认证方式 | JWT Bearer Token | `JwtAuthenticationFilter.java` 使用 `Authorization` 且要求 `Bearer ` 前缀 |
| Session | 无状态 | `SecurityConfig.java` 设置 `SessionCreationPolicy.STATELESS` |
| 认证失败 | HTTP 401 + `AUTH_003` 或 `AUTH_004` | `SecurityConfig.java`、`JwtAuthenticationFilter.java` |
| 权限不足 | HTTP 403 + `AUTH_005` | `SecurityConfig.java` |
| 管理端角色 | `hasRole("admin")`，JWT claims 角色会转换为 `ROLE_` + role | `SecurityConfig.java`、`JwtAuthenticationFilter.java` |

## 3. 权限矩阵

| 接口 URL | 方法 | 是否需要登录 | 需要的角色/权限 | 鉴权方式 | 证据来源 | 代码位置 | 备注 |
|---|---|---:|---|---|---|---|---|
| `/api/v1/health` | GET | 否 | 无 | 无 | `requestMatchers(GET, "/api/v1/health").permitAll()` | `SecurityConfig.java`、`HealthController.java` | 探活 |
| `/api/v1/auth/register` | POST | 否 | 无 | 无 | `requestMatchers(POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()` | `SecurityConfig.java`、`AuthController.java` | 注册 |
| `/api/v1/auth/login` | POST | 否 | 无 | 无 | 同上 | `SecurityConfig.java`、`AuthController.java` | 登录返回 `data.token` |
| `/api/v1/auth/me` | GET | 是 | 已登录用户 | JWT | `requestMatchers(GET, "/api/v1/auth/me").authenticated()` | `SecurityConfig.java`、`AuthController.java` | Controller 也读取 Authorization header |
| `/api/v1/user-preferences/me` | GET/PUT | 是 | 已登录用户 | JWT | 未被 permitAll/admin 匹配，落入 `.anyRequest().authenticated()` | `SecurityConfig.java`、`UserPreferenceController.java` | 当前用户偏好 |
| `/api/v1/destinations/**` | GET | 否 | 无 | 无 | `requestMatchers(GET, "/api/v1/destinations/**").permitAll()` | `SecurityConfig.java`、`DestinationController.java`、`DiaryController.java` | 包含目的地详情、场所、目的地日记 |
| `/api/v1/routes/plan/single` | POST | 是 | 已登录用户 | JWT | POST 未 permitAll，落入 `.anyRequest().authenticated()` | `SecurityConfig.java`、`RouteController.java` | 会使用当前用户保存历史 |
| `/api/v1/routes/plan/multi` | POST | 是 | 已登录用户 | JWT | 同上 | `SecurityConfig.java`、`RouteController.java` | 会使用当前用户保存历史 |
| `/api/v1/routes/history` | GET | 待确认 | 文档称需要登录 | JWT/待确认 | 文档定义，代码未找到入口 | `api-spec.md` | 当前 Controller 未实现 |
| `/api/v1/facilities/**` | GET | 否 | 无 | 无 | `requestMatchers(GET, "/api/v1/facilities/**").permitAll()` | `SecurityConfig.java`、`FacilityController.java` | 当前实现 `/nearby` |
| `/api/v1/foods/**` | GET | 否 | 无 | 无 | `requestMatchers(GET, "/api/v1/foods/**").permitAll()` | `SecurityConfig.java`、`FoodController.java` | 推荐/搜索公开 |
| `/api/v1/diaries/**` | GET | 否 | 无 | 无 | `requestMatchers(GET, "/api/v1/diaries/**").permitAll()` | `SecurityConfig.java`、`DiaryController.java` | 列表/详情/检索公开 |
| `/api/v1/diaries` | POST | 是 | 已登录用户 | JWT | POST 未 permitAll，落入 `.anyRequest().authenticated()` | `SecurityConfig.java`、`DiaryController.java` | 发布日记 |
| `/api/v1/diaries/{id}/ratings` | POST | 待确认 | 文档称需要登录 | JWT/待确认 | 文档定义，代码未找到入口 | `api-spec.md` | 当前未实现 |
| `/api/v1/files/upload` | POST | 是 | 已登录用户 | JWT | `requestMatchers(POST, "/api/v1/files/upload").authenticated()` | `SecurityConfig.java`、`FileController.java` | multipart 上传 |
| `/files/**` | GET | 否 | 无 | 无 | `requestMatchers(GET, "/files/**").permitAll()` | `SecurityConfig.java`、`FileResourceConfig.java` | 静态访问上传资源 |
| `/api/v1/admin/**` | 全部 | 是 | admin | JWT + `ROLE_admin` | `requestMatchers("/api/v1/admin/**").hasRole("admin")` | `SecurityConfig.java`、`AdminController.java` | 覆盖管理端维护与导入 |
| `/api/v1/ai/**` | POST | 待确认 | 文档倾向需要登录或按能力控制 | 待确认 | 文档有定义，后端未找到 Controller | `api-spec.md` | P2 预留 |

## 待确认项
- Route 历史、Diary 评分、我的日记接口是否补实现，以及权限是否按文档执行。
- AI 接口后续是否统一登录即可访问，还是区分普通用户和管理员。
- 管理端 role 值是否永远使用小写 `admin`；当前 JWT 过滤器直接拼接 `ROLE_` + claims role。

## 代码/文档冲突项
- `api-spec.md` 写“查看我的路线历史”需要登录，但当前代码没有 Route 历史 Controller 入口。
- `api-spec.md` 写“对日记评分”需要登录，但当前代码没有评分 Controller 入口。
- `api-spec.md` 存在 AI 预留接口，当前没有 AI Controller，权限无法从代码确认。

## 建议下一步动作
- 将“文档有但代码未找到”的接口从前端联调范围中暂时排除，或补最小后端入口。
- 前端联调时先按已确认规则实现：公开 GET 不带 token 也可访问；POST 日记、路线、文件上传带 `Authorization: Bearer <token>`；Admin 必须 admin token。
