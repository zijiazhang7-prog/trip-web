# 运行环境与请求约定（Runtime Environment）

## A. 后端事实（Backend Facts）

- 本地开发端口：`8080`
- 接口前缀：`/api/v1`
- 鉴权头：`Authorization: Bearer <token>`
- 登录 token 返回位置：`data.token`
- CORS 已放行：`http://localhost:5173`、`http://127.0.0.1:5173`
- 统一响应体：`success/code/message/data/timestamp`
- 统一分页体：`list/pageNum/pageSize/total/pages`

详见：`/.cursor/context/runtime-env01.md` 的 Backend Confirmed Facts 表格。

## B. 前端事实（Frontend Runtime Facts）

- 前端栈：Vite + React + TypeScript
- 当前为 mock-first，API 工厂仍映射 mock
- 未配置真实 baseURL
- 未配置 Vite `server.proxy`
- 未实现统一请求拦截器与 token 存储策略

详见：`/.cursor/context/frontend-runtime-facts.md`。

## C. 待确认项（Joint Open Questions）

- 已确认采用相对路径 + Vite proxy
- 已确认 token 存储为 localStorage（浏览器重开后保留）
- 统一请求拦截器与 401 处理方案（已进入前端实现范围）
- 测试环境地址与可用账号
- Swagger 是否需要可运行 UI（当前仅确认 draft yaml）

## E. 已拍板联调决策（2026-05-09）

- 前端寻址策略：相对路径 + Vite proxy
- token 策略：登录成功后写入 localStorage
- 联调环境地址：`http://10.21.249.116:8080`
- 本轮暂不纳入必须调通接口：
  - `GET /api/v1/routes/history`
  - `GET /api/v1/routes/history/{id}`
  - `GET /api/v1/facilities/search`（先调 `nearby`）
  - `POST /api/v1/diaries/{id}/ratings`
  - `GET /api/v1/diaries/me`
- `ai/**`：由后端后续推进，本轮不纳入前端联调阻塞项

## D. 当前可执行联调边界

- 可先联调：后端已实现 + 权限明确 + 字段已冻结的接口
- 暂不纳入本轮：文档有但后端入口未实现的接口（如 Route 历史、Diary 评分、`diaries/me`、`facilities/search`、`ai/**`）
