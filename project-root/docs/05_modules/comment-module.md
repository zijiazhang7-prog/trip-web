# 评论模块说明

## 1. 当前范围

评论基础版支持目的地、美食、日记三类资源的一级评论发布、分页展示和软删除。三类外部 API
按资源分开，内部复用统一 `CommentService`。

## 2. 模块结构

```text
Controller
  -> CommentService
  -> CommentTargetHandler
  -> DestinationCommentMapper / FoodCommentMapper / DiaryCommentMapper
```

`CommentService` 负责内容校验、JWT 当前用户、分页结果和删除权限；Handler 负责目标校验及表差异。
Mapper 只访问对应固定表，不使用动态表名。

## 3. 业务规则

- 评论列表公开，发布和删除必须登录。
- 评论正文 trim 后不能为空，最大 500 个字符。
- 第一阶段只创建和展示 `parent_comment_id IS NULL` 的一级评论。
- 列表只返回 `status=1`，按 `created_at DESC, id DESC` 排序。
- 目的地必须启用；美食只校验存在；日记必须公开且启用。
- 普通用户只能删除自己的评论，状态改为 2。
- 管理员可隐藏任意正常评论，状态改为 0。
- 当前不维护主表评论数，不接入 Engine 层或 RankService。

## 4. 数据结构与复杂度

- Handler 注册使用 `EnumMap<CommentTargetType, CommentTargetHandler>`，目标类型分发平均为 `O(1)`。
- 评论列表由 MySQL 复合索引按目标、状态和创建时间分页，单页组装约为 `O(p)`，`p` 为当前页条数。
- 用户展示信息按当前页用户 ID 集合批量查询，避免逐条查询。
- 适合课程设计和中小规模评论数据。

## 5. 后续范围

楼中楼回复、点赞、图片、敏感词、审核流、热门排序、全文搜索和评论数聚合均不属于第一阶段。
