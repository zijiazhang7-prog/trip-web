# 前端运行时事实（frontend-runtime-facts）

## 1. 文件用途

本文件只记录可以从当前前端代码直接确认的运行时事实，不推测后端实现细节。

## 2. Confirmed Facts (From Frontend Code)

| 项 | 当前确认事实 | 来源证据 |
|---|---|---|
| 前端技术栈 | Vite + React + TypeScript | `vite.config.ts`、`src/main.tsx` |
| 路由模式 | BrowserRouter（非 Hash Router） | `src/App.tsx` |
| API 层现状 | mock-first，尚未接入真实 HTTP | `src/api/diary.ts`、`src/api/ai.ts` |
| Diary API 工厂 | `getDiaryApi()` 当前全部映射到 mock 实现 | `src/api/diary.ts` |
| AI API 工厂 | `getAiApi()` 当前全部映射到 mock 实现 | `src/api/ai.ts` |
| baseURL 配置 | 未发现真实接口 `baseURL` 配置 | 代码检索结果 |
| Vite 代理 | 未发现 `server.proxy` 配置 | `vite.config.ts` |
| 鉴权头注入 | 未发现统一请求拦截器和 `Authorization` 自动注入逻辑 | 代码检索结果 |
| token 存储 | 未发现 `localStorage/sessionStorage/cookie` token 存储逻辑 | 代码检索结果 |
| 联调状态 | 前端仍处于 mock 阶段，尚未切换后端真实服务 | `src/pages/DiaryPage.tsx` + API 工厂实现 |

## 3. 待确认（需前后端协同）

| 项 | 当前状态 | 建议确认人 |
|---|---|---|
| 最终 baseURL 策略（绝对地址或相对地址） | 待确认 | 前端负责人 + 后端负责人 |
| 是否启用 Vite dev proxy | 待确认 | 前端负责人 |
| token 存储方案 | 待确认 | 前端负责人 |
| 请求/响应拦截器规范 | 待确认 | 前端负责人 |
| 测试环境地址 | 待确认 | 项目负责人 / 部署负责人 |

## 4. 备注

- 本文件用于补全联调约束，优先级高于口头约定。
- 若后续接入真实 HTTP，本文件需同步更新为最新事实。
# frontend-runtime-facts

## 1) Scope

This document records runtime facts that can be confirmed from the current frontend codebase.
It is intended to complement backend-side runtime docs during frontend-backend integration.

## 2) Frontend Confirmed Facts (From Code)

### 2.1 Stack and runtime entry

- Frontend stack is Vite + React + TypeScript.
- Runtime entry is `src/main.tsx`.

### 2.2 Routing mode

- Routing uses `BrowserRouter` (`src/App.tsx`), not hash-based routing.

### 2.3 API integration status

- Current implementation is mock-first.
- `src/api/diary.ts` returns mock implementations in `getDiaryApi()`.
- `src/api/ai.ts` returns mock implementations in `getAiApi()`.
- No real HTTP client integration is active in these API factories at this time.

### 2.4 baseURL and proxy

- No confirmed frontend API `baseURL` is configured in current code.
- `vite.config.ts` currently has no `server.proxy` configuration.

### 2.5 Token and auth header handling

- No confirmed unified request interceptor is present in frontend code.
- No confirmed `Authorization: Bearer <token>` injection path is present in frontend code.
- No confirmed token persistence logic (localStorage/sessionStorage/cookie) is present in frontend code.

## 3) Items Not Confirmable From Current Frontend Code

The following items are not currently verifiable in frontend source and must be confirmed jointly:

- Final API base URL per environment (dev/test/prod).
- Whether dev proxy should be enabled, and exact proxy routes.
- Final token storage strategy (cookie/localStorage/sessionStorage).
- Whether request interceptors should be centralized and where.
- Final integration test server address and access constraints.

## 4) Coordination Notes

- Backend may continue providing backend-runtime facts from backend code.
- Frontend should supplement this file once real HTTP integration is enabled.
- During contract alignment, treat this file as the frontend truth source for current implementation status.

## 5) Additional Observation

- `src/main.tsx` includes a debug ingestion request to `http://127.0.0.1:7366/ingest/...`.
- This is not business API traffic, but should be reviewed before team-wide integration to avoid confusion.
