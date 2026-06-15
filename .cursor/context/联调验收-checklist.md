# 联调验收 Checklist（可打勾）

## A. 环境与启动

- [ ] 前端 `npm install` 成功
- [ ] 前端 `npm run dev` 成功
- [ ] 访问前端首页正常
- [ ] 代理目标为 `http://10.21.249.116:8080`（或已明确替换）
- [ ] 后端服务可访问 `/api/v1/health`

## B. 鉴权链路

- [ ] 注册接口 `POST /api/v1/auth/register` 返回成功
- [ ] 登录接口 `POST /api/v1/auth/login` 返回 `data.token`
- [ ] 浏览器 `localStorage` 可见 `trip_auth_token`
- [ ] 已登录状态下 `GET /api/v1/auth/me` 返回用户信息
- [ ] 退出登录后 token 被清理

## C. 社群分享（/community）

### C1 列表

- [ ] 默认可加载 `GET /api/v1/diaries` 列表
- [ ] 点击“最新发布”可按 `sortBy=latest` 刷新
- [ ] 点击“最受欢迎”可按 `sortBy=heat` 刷新

### C2 搜索

- [ ] 标题检索可调用 `GET /api/v1/diaries/search/title`
- [ ] 全文检索可调用 `GET /api/v1/diaries/search/fulltext`

### C3 详情

- [ ] 点击“查看详情”可调用 `GET /api/v1/diaries/{id}`
- [ ] 详情弹层支持 ESC 关闭
- [ ] 详情弹层支持点击遮罩关闭
- [ ] 详情弹层“上一条/下一条”切换正常

### C4 发布

- [ ] 未登录时发布被前置拦截并提示
- [ ] 登录后可发布 `POST /api/v1/diaries`
- [ ] 可见性字段可传 `public/private`
- [ ] 图片可上传 `POST /api/v1/files/upload`（`bizType=diary`）
- [ ] 发布成功后列表刷新
- [ ] 发布后若卡片存在，自动滚动并高亮；若不存在，有检索提示

## D. 本轮范围确认

- [ ] 团队确认以下接口“不纳入本轮必须调通”：
  - [ ] `GET /api/v1/routes/history`
  - [ ] `GET /api/v1/routes/history/{id}`
  - [ ] `GET /api/v1/facilities/search`
  - [ ] `POST /api/v1/diaries/{id}/ratings`
  - [ ] `GET /api/v1/diaries/me`
  - [ ] `POST /api/v1/ai/**`

## E. 质量门禁

- [ ] `npm run lint` 通过
- [ ] `npm run build` 通过
- [ ] 联调问题已记录到 `/.cursor/context/open-questions.md`
