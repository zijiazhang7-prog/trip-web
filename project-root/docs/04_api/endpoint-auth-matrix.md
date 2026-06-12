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
| `/api/v1/routes/history` | GET | 是 | 已登录用户，仅本人 | JWT | 未命中公开 GET，落入 `.anyRequest().authenticated()`；Service 固定使用 JWT userId | `SecurityConfig.java`、`RouteController.java`、`RouteServiceImpl.java` | 不接受 userId |
| `/api/v1/routes/history/{id}` | GET | 是 | 已登录用户，仅记录所有者 | JWT + Service 所有者条件 | Service 使用 `id + 当前 userId` 联合查询 | `RouteController.java`、`RouteServiceImpl.java` | 他人记录与不存在记录统一返回 404 |
| `/api/v1/facilities/**` | GET | 否 | 无 | 无 | `requestMatchers(GET, "/api/v1/facilities/**").permitAll()` | `SecurityConfig.java`、`FacilityController.java` | 当前实现 `/nearby` |
| `/api/v1/foods/**` | GET | 否 | 无 | 无 | `requestMatchers(GET, "/api/v1/foods/**").permitAll()` | `SecurityConfig.java`、`FoodController.java` | 推荐/搜索公开 |
| `/api/v1/diaries/recommend` | GET | 是 | 已登录用户 | JWT | 专用 authenticated matcher 位于日记公开 GET 规则之前 | `SecurityConfig.java`、`DiaryController.java`、`DiaryRecommendServiceImpl.java` | 使用当前用户偏好返回 Top-K |
| `/api/v1/diaries/**` | GET | 否 | 无 | 无 | `requestMatchers(GET, "/api/v1/diaries/**").permitAll()` | `SecurityConfig.java`、`DiaryController.java` | 除推荐和个人评分外，列表/详情/检索公开 |
| `/api/v1/diaries` | POST | 是 | 已登录用户 | JWT | POST 未 permitAll，落入 `.anyRequest().authenticated()` | `SecurityConfig.java`、`DiaryController.java` | 发布日记 |
| `/api/v1/diaries/{id}/ratings` | POST | 是 | 已登录用户 | JWT | POST 落入 `.anyRequest().authenticated()` | `SecurityConfig.java`、`DiaryController.java` | 提交或更新评分 |
| `/api/v1/diaries/{id}/ratings/me` | GET | 是 | 已登录用户 | JWT | 专用 authenticated matcher 位于日记公开 GET 规则之前 | `SecurityConfig.java`、`DiaryController.java` | 查询当前用户评分 |
| `/api/v1/diaries/{diaryId}/animation` | POST | 是 | 日记作者 | JWT + Service 所有者校验 | POST 落入 authenticated，Service 比对当前用户与 `diary.user_id` | `SecurityConfig.java`、`DiaryAnimationController.java`、`AnimationServiceImpl.java` | 作者可为自己的公开/私有日记生成 |
| `/api/v1/diaries/{diaryId}/animation` | GET | 公开日记否，私有日记是 | 公开访问或私有日记作者 | 无/JWT + Service 可见性校验 | GET 命中日记公开规则，私有资源由 Service 二次校验 | `SecurityConfig.java`、`DiaryAnimationController.java`、`AnimationServiceImpl.java` | 未生成返回 `AI_010` |
| `/api/v1/destinations/{id}/comments` | GET/POST | GET 否，POST 是 | 发布为已登录用户 | GET 无 / POST JWT | GET 命中目的地公开规则，POST 落入 authenticated | `SecurityConfig.java`、`DestinationController.java` | 一级评论 |
| `/api/v1/foods/{id}/comments` | GET/POST | GET 否，POST 是 | 发布为已登录用户 | GET 无 / POST JWT | GET 命中美食公开规则，POST 落入 authenticated | `SecurityConfig.java`、`FoodController.java` | 一级评论 |
| `/api/v1/diaries/{id}/comments` | GET/POST | GET 否，POST 是 | 发布为已登录用户 | GET 无 / POST JWT | GET 命中日记公开规则，POST 落入 authenticated | `SecurityConfig.java`、`DiaryController.java` | 仅公开启用日记 |
| `/api/v1/comments/{commentType}/{commentId}` | DELETE | 是 | 评论所有者或 admin | JWT + Service 权限校验 | 未 permitAll，落入 authenticated；Service 校验所有者/管理员 | `SecurityConfig.java`、`CommentController.java`、`CommentServiceImpl.java` | 普通用户置 2，管理员置 0 |
| `/api/v1/files/upload` | POST | 是 | 已登录用户 | JWT | `requestMatchers(POST, "/api/v1/files/upload").authenticated()` | `SecurityConfig.java`、`FileController.java` | multipart 上传 |
| `/files/**` | GET | 否 | 无 | 无 | `requestMatchers(GET, "/files/**").permitAll()` | `SecurityConfig.java`、`FileResourceConfig.java` | 静态访问上传资源 |
| `/api/v1/admin/**` | 全部 | 是 | admin | JWT + `ROLE_admin` | `requestMatchers("/api/v1/admin/**").hasRole("admin")` | `SecurityConfig.java`、`AdminController.java` | 覆盖管理端维护与导入 |
| `/api/v1/ai/**` | POST | 待确认 | 文档倾向需要登录或按能力控制 | 待确认 | 文档有定义，后端未找到对应 Controller | `api-spec.md` | 草稿/摘要等 P2 预留，不包含已实现动画接口 |

## 待确认项
- 我的日记列表接口是否补实现，以及权限是否按文档执行。
- AI 接口后续是否统一登录即可访问，还是区分普通用户和管理员。
- 管理端 role 值是否永远使用小写 `admin`；当前 JWT 过滤器直接拼接 `ROLE_` + claims role。

## 代码/文档冲突项
- Route 历史鉴权冲突已消除；当前代码与文档均要求 JWT 且只能查询本人。
- `api-spec.md` 存在 AI 预留接口，当前没有 AI Controller，权限无法从代码确认。

## 建议下一步动作
- 将“文档有但代码未找到”的接口从前端联调范围中暂时排除，或补最小后端入口。
- 前端联调时先按已确认规则实现：公开 GET 不带 token 也可访问；POST 日记、路线、文件上传带 `Authorization: Bearer <token>`；Admin 必须 admin token。
