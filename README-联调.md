# 前后端联调说明（当前版本）

## 1. 项目现状

- 已完成联调能力：
  - 登录/注册、`user-preferences` 偏好读写
  - 旅游推荐 / 美食 / 社群 `diaries` 全链路
  - 路线规划 `routes/plan/multi`、旅行中 `facilities/nearby` + `routes/plan/single`
  - **手账 = 社群**：旅游日记编辑器发布走 `POST /api/v1/diaries`，社群以手账封面风展示
- 模块关系：
  - `旅游日记`：本地手账编辑（Mock 书架）+ **发布/草稿** 同步后端 diaries
  - `社群分享`：大家公开分享的手账书架（同一 `diaries` 数据模型）

## 2. 运行方式

```bash
npm install
npm run dev
```

前端本地端口默认：`5173`

## 3. 联调环境与代理

- 前端采用：相对路径 + Vite Proxy
- 代理目标（默认）：`http://10.21.249.116:8080`
- 已代理路径：
  - `/api`
  - `/files`

如需临时切换代理目标：

```bash
# PowerShell
$env:VITE_API_PROXY_TARGET="http://<your-host>:<port>"
npm run dev
```

## 4. 鉴权约定

- 登录接口：`POST /api/v1/auth/login`
- token 来源：响应 `data.token`
- 本地存储键：`trip_auth_token`
- 自动注入：`Authorization: Bearer <token>`
- 退出登录：清除本地 token

## 5. 社群分享可验证能力

页面：`/community`

- 列表：
  - `GET /api/v1/diaries?sortBy=latest|heat&pageNum=1&pageSize=30`
- 搜索：
  - 标题检索 `GET /api/v1/diaries/search/title?title=...`
  - 全文检索 `GET /api/v1/diaries/search/fulltext?keyword=...`
- 详情：
  - `GET /api/v1/diaries/{id}`
- 发布：
  - `POST /api/v1/diaries`
- 上传：
  - `POST /api/v1/files/upload` (`bizType=diary`)

交互细节：

- 详情弹层支持 ESC 关闭、点击遮罩关闭
- 发布成功后自动尝试滚动定位新卡片并短暂高亮
- 若当前页未命中新发布卡片，会提示“可用标题检索查看”

## 6. 演示推荐顺序（约 5 分钟）

1. 注册/登录
2. 首页 → 行程偏好 → 保存
3. 推荐卡片 → 规划路线 → 生成多景点路线
4. 旅行中 → 查附近设施
5. 旅游日记 → 写手账 → **发布到手账社群**
6. 社群页查看刚发布的手账封面卡片

## 7. 本轮不纳入必须调通

- `GET /api/v1/routes/history` / `{id}`
- `GET /api/v1/facilities/search`（使用 `nearby`）
- `GET /api/v1/destinations/{id}/map-nodes`（未部署时用 `places` 作为节点候选）
- `POST /api/v1/diaries/{id}/ratings`、`GET /api/v1/diaries/me`
- `POST /api/v1/ai/**`（手账内「路线草图」为演示态）

## 8. 已知说明

- 手账正文以 `[[HANDACCOUNT_JSON]]` 元数据 + 纯文本存入 `contentText`，社群详情按双页展开。
- 路线规划节点 ID 来自场所列表回退时，需与后端 `map_node` 配置一致，否则规划可能返回 4xx。
