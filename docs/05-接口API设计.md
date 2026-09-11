# TasteLink 接口 API 设计文档

| 项 | 内容 |
|---|---|
| 文档版本 | v1.0 |
| 更新日期 | 2026-08-31 |
| BaseURL | `/api/v1` |
| 关联文档 | `01-需求文档.md`、`04-数据库表设计.md` |

---

## 1. 通用约定

| 项 | 约定 |
|---|---|
| 前缀 | 所有接口统一 `/api/v1` 前缀 |
| 传输 | 请求/响应均为 `application/json; charset=utf-8`；文件上传为 `multipart/form-data` |
| 字符编码 | UTF-8 |
| 时间格式 | 统一 `yyyy-MM-dd HH:mm:ss` 字符串 |
| 分页参数 | `page`(从1起)、`size`(默认10，建议上限 50)；返回 `PageResult` |
| 鉴权 | JWT 无状态，登录后置 `Authorization: Bearer ${token}` |
| 统一返回 | `{ "code": 0, "message": "success", "data": {...} }`，`code=0` 表示业务成功 |

---

## 2. 鉴权机制

### 2.1 JWT 流程
1. `POST /api/v1/auth/login` 成功后返回 `token`（含 userId/username/role，有效期 24h）
2. 前端将 token 存于 `localStorage`，并在请求拦截器注入 `Authorization: Bearer ${token}`
3. 后端 `JwtAuthFilter` 解析 token，校验通过后写入 `SecurityContext`（含角色 `ROLE_USER`/`ROLE_ADMIN`）；服务层通过 `SecurityContextHelper.getCurrentUserId()` 获取登录人
4. token 缺失/过期/非法 → 写操作返回 401 `{code,message}`（公开接口不受影响）

### 2.2 白名单（无需登录）
- `POST /api/v1/auth/register`、`POST /api/v1/auth/login`
- `GET /api/v1/shops`、`GET /api/v1/shops/**`、`GET /api/v1/shops/categories`
- `GET /api/v1/reviews/{id}`、`GET /api/v1/reviews/{id}/comments`
- `GET /api/v1/users/{userId}`、`GET /api/v1/users/{userId}/reviews`
- `GET /api/v1/users/{userId}/followings`、`GET /api/v1/users/{userId}/followers`
- `GET /api/v1/home`、`GET /api/v1/home/**`

> 其余（写操作、`GET /api/v1/users/me`、`POST /api/v1/files/image`、关注/点赞/评论等）均需登录。
> `/users/me` 规则需配置在 `/users/**` 通配规则之前。

### 2.3 角色与后台权限（v2 Phase B）
- `t_user.role` 取值 `USER`（普通，默认）/`ADMIN`（管理员）；`role` 随登录写入 JWT claim。
- `SecurityConfig` 以 `requestMatchers("/api/v1/admin/**").hasRole("ADMIN")` 强制后台路径仅管理员可访问；`hasRole` 自动匹配 `ROLE_ADMIN`。该规则须置于 `anyRequest().authenticated()` 之前。普通用户访问后台接口返回 403。
- 角色鉴权仅服务端把关，登录响应体 `LoginVO` 暂不暴露 `role`（后台前端界面见 `docs/07` 配套阶段）。

---

## 3. 统一返回体与错误码

### 3.1 统一返回体

```json
{ "code": 0, "message": "success", "data": { } }
```

分页返回（`data` 为 `PageResult`）：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [ ],
    "total": 100,
    "current": 1,
    "size": 10,
    "pages": 10
  }
}
```

### 3.2 错误码表

| code | HTTP | 含义 |
|---|---|---|
| 0 | 200 | 业务成功 |
| 400 | 400 | 参数校验失败 |
| 401 | 401 | 未登录/Token失效 |
| 403 | 403 | 无权限（如改他人资料） |
| 404 | 404 | 资源不存在 |
| 40901 | 200 | 用户名已存在 |
| 40902 | 200 | 已点赞（幂等返回，业务成功） |
| 40903 | 200 | 已关注（幂等返回，业务成功） |
| 40904 | 200 | 不可关注自己 |
| 42901 | 200 | 登录尝试过于频繁，请 10 分钟后再试（登录限流） |
| 409 | 409 | 店铺已被他人修改，请刷新重试（管理员并发改店铺，乐观锁冲突） |
| 500 | 500 | 服务器内部错误（不泄露堆栈） |

> 点赞/关注幂等场景：重复操作视为成功，返回当前计数或当前态，避免前端处理冲突。

---

## 4. 接口清单

### 4.1 认证模块

#### POST `/api/v1/auth/register`（公开）
注册新用户。

请求体：
```json
{ "username": "tom", "password": "Abc12345" }
```
响应 `data`：
```json
{ "userId": 1001, "username": "tom" }
```
规则：`username` 唯一；密码长度≥8 且含字母+数字；密码 BCrypt 加密存储；用户名已存在返回 `40901`。

#### POST `/api/v1/auth/login`（公开）
账号密码登录。

请求体：
```json
{ "username": "tom", "password": "Abc12345" }
```
响应 `data`：
```json
{
  "token": "eyJhbGciOi...",
  "expiresInSec": 86400,
  "userId": 1001,
  "username": "tom",
  "nickname": "Tom",
  "avatarUrl": "https://.../a.jpg"
}
```
规则：校验通过签发 JWT；用户名或密码错误返回 401。登录防爆破：同一用户名 10 分钟内连续失败 5 次返回 `42901`（HTTP 200 + 业务码，前端按普通错误提示）；登录成功自动清零。

---

### 4.2 用户模块

#### GET `/api/v1/users/me`（登录）
获取当前登录用户完整资料。

响应 `data`：见 [`UserVO`](#51-uservo)（本人含全部字段）。

#### PUT `/api/v1/users/me`（登录）
修改当前用户资料（昵称/头像/简介）。

请求体（字段均可选）：
```json
{ "nickname": "新昵称", "avatarUrl": "https://.../new.jpg", "bio": "新简介" }
```
响应 `data`：更新后的 `UserVO`。规则：仅可改本人资料；`username` 不可改。

#### GET `/api/v1/users/{userId}`（公开）
查看某用户主页资料。

响应 `data`：`UserVO`（展示字段：id, nickname, avatarUrl, bio, followingCount, followerCount, reviewCount）。

#### GET `/api/v1/users/{userId}/reviews`（公开）
某用户发布的点评列表。

请求参数：`page`, `size`
响应 `data`：`PageResult<ReviewVO>`

### 4.3 店铺模块

#### GET `/api/v1/shops`（公开）
店铺列表（搜索/分类/城市筛选 + 排序）。

请求参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| keyword | 否 | 关键词搜索：v2 Phase D 起走 **Elasticsearch**（`name` 字段 + relevance 排序）；ES 失联/禁用自动**降级回 MySQL `LIKE name`**，前端无感。中文分词需 IK 插件，未装时中文连续命中弱（见 `docs/02 §4.9`） |
| categoryId | 否 | 分类 id |
| city | 否 | 城市 |
| sortBy | 否 | 默认 `review_count`（热度）；可扩展 `rating`。注：`keyword` 命中 ES 时按 relevance 排序，忽略 `sortBy` |
| page | 否 | 默认 1 |
| size | 否 | 默认 10 |

响应 `data`：`PageResult<ShopVO>`（命中结果由 MySQL 回查组装，计数/分类名与库一致）

#### GET `/api/v1/shops/{shopId}`（公开）
店铺详情。

响应 `data`：`ShopDetailVO`（店铺基础信息 + 近期 3 条点评预览 `List<ReviewVO>`）

#### GET `/api/v1/shops/{shopId}/reviews`（公开）
店铺下点评列表。

请求参数：`page`, `size`, `sortBy`（`time` 默认 / `like` 热度）
响应 `data`：`PageResult<ReviewVO>`

#### GET `/api/v1/shops/categories`（公开）
分类字典列表。

响应 `data`：`List<CategoryVO>`
```json
[ { "id": 1, "code": "HOTPOT", "name": "火锅", "iconUrl": "...", "sortOrder": 1 } ]
```

---

### 4.4 点评模块

#### POST `/api/v1/shops/{shopId}/reviews`（登录）
对店铺发布点评。

请求体：
```json
{
  "content": "汤底很正宗，毛肚嫩滑，推荐！",
  "rating": 5,
  "imageUrls": ["https://.../img1.jpg", "https://.../img2.jpg"]
}
```
规则：`rating` 1~5；图片≤9 张；图片需先经 `/files/image` 上传拿到 URL；服务端用点评所属店铺的 `city` 冗余写入点评 `city` 字段；同事务维护店铺 `review_count`/`rating_sum`/`avg_rating` 与用户 `review_count`。

响应 `data`：`ReviewVO`（含生成的 `id`）

#### GET `/api/v1/reviews/{reviewId}`（公开）
点评详情。

响应 `data`：`ReviewVO`（含图片列表、作者用户信息、点赞数、评论数；若登录则含 `hasLiked` 标志）

---

### 4.5 互动模块

#### POST `/api/v1/reviews/{reviewId}/likes`（登录）
点赞点评（幂等）。

请求体：无
响应 `data`：
```json
{ "likeCount": 128 }
```
规则：已点赞返回当前计数不变（`40902` 业务成功语义，前端当成功处理）。

#### DELETE `/api/v1/reviews/{reviewId}/likes`（登录）
取消点赞（幂等）。

响应 `data`：
```json
{ "likeCount": 127 }
```
规则：未点过赞也返回成功，计数不变。

#### GET `/api/v1/reviews/{reviewId}/comments`（公开）
点评下评论列表（单层）。

请求参数：`page`, `size`
响应 `data`：`PageResult<CommentVO>`

#### POST `/api/v1/reviews/{reviewId}/comments`（登录）
发表评论。

请求体：
```json
{ "content": "握手，毛肚确实好吃" }
```
响应 `data`：`CommentVO`；同事务维护点评 `reply_count`。

---

### 4.6 关注模块

#### POST `/api/v1/users/{userId}/follow`（登录）
关注用户（幂等）。

请求体：无
响应 `data`：
```json
{ "followingCount": 42 }
```
规则：不可关注自己（`40904`）；已关注返回成功不变（`40903`）；同事务维护双方 `following_count`/`follower_count`。

#### DELETE `/api/v1/users/{userId}/follow`（登录）
取消关注（幂等）。

响应 `data`：
```json
{ "followingCount": 41 }
```

#### GET `/api/v1/users/{userId}/followings`（公开）
某用户的关注列表。

请求参数：`page`, `size`
响应 `data`：`PageResult<UserVO>`（关注者摘要）

#### GET `/api/v1/users/{userId}/followers`（公开）
某用户的粉丝列表。

请求参数：`page`, `size`
响应 `data`：`PageResult<UserVO>`（粉丝摘要）

---

### 4.7 首页模块

#### GET `/api/v1/home`（公开）
首页热门内容。

请求参数：`city`（可选）

响应 `data`：`HomeVO`
```json
{
  "hotShops": [ /* ShopVO, 默认 10 条 */ ],
  "hotReviews": [ /* ReviewVO, 默认 10 条 */ ]
}
```
查询：
- 热门店铺：`WHERE city=? AND status=1 ORDER BY review_count DESC, like_count DESC LIMIT 10`（无 city 则全局）
- 热门店铺点评：`WHERE city=? AND status=1 ORDER BY like_count DESC, reply_count DESC LIMIT 10`（无 city 则全局）

---

### 4.8 文件上传模块

#### POST `/api/v1/files/image`（登录）
上传图片（后端代理 OSS 或本地存储）。

请求体：`multipart/form-data`，字段 `file`
响应 `data`：
```json
{ "url": "https://.../2026/08/uuid.jpg", "ossKey": "2026/08/uuid.jpg" }
```
规则：仅登录用户；校验类型（jpg/png/webp 等）与大小；文件名 UUID 化；返回 `url` 可直接 `<img src>` 展示。本地兜底时返回后端静态资源 URL。

### 4.9 管理员后台模块（v2 Phase B）

> 路径前缀 `/api/v1/admin/**`，`SecurityConfig` 强制 `hasRole('ADMIN')`；普通用户访问返回 403，未登录返回 401。店铺删除（延时清理）见 `docs/06` Phase C。

#### PUT `/api/v1/admin/shops/{shopId}`（管理员）
编辑店铺资料。并发控制走乐观锁（`t_shop.version`）：服务端 `selectById` 载入当前版本 → 覆盖请求体中**非空/null 提供的字段** → `updateById` 自动校验版本；冲突则影响行数 0，返回 409。

请求体（字段均可选，非空/null 才更新；`version` 不暴露，服务端自动处理）：
```json
{
  "name": "辣府火锅(静安店)",
  "categoryId": 1,
  "city": "上海",
  "address": "上海市静安区南京西路1788号",
  "phone": "021-62880000",
  "coverUrl": "https://.../cover.jpg",
  "description": "人气重庆火锅"
}
```
响应 `data`：`ShopDetailVO`（与 `GET /shops/{shopId}` 一致，含分类名与近期点评）。
字段约束：`name`≤128、`city`≤64、`address`≤255、`phone`≤32、`coverUrl`≤512、`description`≤1000。
规则：店铺不存在返回 404；`categoryId` 非空时校验分类存在性（不存在 400）；并发冲突返回 409 `SHOP_VERSION_CONFLICT`，前端应提示「内容已被他人修改，请刷新重试」。

#### DELETE `/api/v1/admin/shops/{shopId}`（管理员，v2 Phase C）
删除店铺（物理删路径 + RabbitMQ 延时清理）。

请求体：无。
响应 `data`：无（`R<Void>`）。
规则：同事务内标记 `shop.status=0` + 级联 `review.status=0`（即时从公开读路径消失）+ 对称回扣发布用户 `review_count`；事务提交后投递延时清理消息，**到期由异步消费者物理级联删** `t_review_image → t_review_like → t_review_comment → t_review → t_shop`、删 OSS/本地图、从热度 ZSet 摘除。延迟窗口（默认 5s）便于「撤销误删」。并发改/删冲突仍返回 409 `SHOP_VERSION_CONFLICT`；二次删除返回 404（幂等口风）。broker 缺失时 afterCommit 不发消息，由对账调度直接物理清理（`tastelink.rabbitmq.enabled=false` 同此降级）。详见 `docs/02 §4.8`。

---

## 5. 数据模型（VO）

### 5.1 UserVO
| 字段 | 类型 | 说明 |
|---|---|---|
| id | number | 用户ID |
| username | string | 用户名（仅本人主页或必要场景返回） |
| nickname | string | 昵称 |
| avatarUrl | string | 头像URL |
| bio | string | 简介 |
| followingCount | number | 关注数 |
| followerCount | number | 粉丝数 |
| reviewCount | number | 发布点评数 |
| hasFollowed | boolean | 当前登录人是否已关注（未登录为 false） |

> `password` 永不出现在任何 VO。

### 5.2 ShopVO
| 字段 | 类型 | 说明 |
|---|---|---|
| id | number | 店铺ID |
| name | string | 店铺名称 |
| categoryId | number | 分类ID |
| categoryName | string | 分类中文名 |
| city | string | 城市 |
| address | string | 地址 |
| coverUrl | string | 封面图 |
| avgRating | number | 平均评分(0.00-5.00) |
| reviewCount | number | 点评数 |

### 5.3 ShopDetailVO
继承 `ShopVO` 字段，并附加：

| 字段 | 类型 | 说明 |
|---|---|---|
| phone | string | 联系电话 |
| description | string | 店铺简介 |
| likeCount | number | 店铺点评累计点赞 |
| topReviews | ReviewVO[] | 近期点评预览(默认3) |

### 5.4 ReviewVO
| 字段 | 类型 | 说明 |
|---|---|---|
| id | number | 点评ID |
| shopId | number | 店铺ID |
| shopName | string | 店铺名（首页/列表场景） |
| userId | number | 作者ID |
| userNickname | string | 作者昵称 |
| userAvatarUrl | string | 作者头像 |
| content | string | 点评文字 |
| rating | number | 评分1-5 |
| likeCount | number | 点赞数 |
| replyCount | number | 评论数 |
| images | string[] | 图片URL列表(有序) |
| hasLiked | boolean | 当前登录人是否已点赞 |
| createTime | string | 发表时间 |

### 5.5 CommentVO
| 字段 | 类型 | 说明 |
|---|---|---|
| id | number | 评论ID |
| reviewId | number | 所属点评ID |
| userId | number | 评论用户ID |
| userNickname | string | 评论用户昵称 |
| userAvatarUrl | string | 评论用户头像 |
| content | string | 评论内容 |
| createTime | string | 评论时间 |

### 5.6 HomeVO
| 字段 | 类型 | 说明 |
|---|---|---|
| hotShops | ShopVO[] | 热门店铺 |
| hotReviews | ReviewVO[] | 热门点评 |

---

## 6. 接口-模块-数据表速查

| 接口 | 主写表 | 主读表 | 核心冗余维护 |
|---|---|---|---|
| 注册 | t_user | t_user | — |
| 登录 | — | t_user | — |
| 改资料 | t_user | t_user | — |
| 店铺列表/详情 | — | t_shop, t_review, t_review_image | — |
| 发点评 | t_review, t_review_image | t_shop, t_user | shop.rating_sum/avg_rating/review_count, user.review_count |
| 点评详情 | t_review_like(读) | t_review, t_review_image, t_user | — |
| 点赞 | t_review_like | t_review, t_shop | review.like_count (+shop.like_count) |
| 评论 | t_review_comment | t_review | review.reply_count |
| 关注/取关 | t_follow | t_user | 双方 following_count / follower_count |
| 首页 | — | t_shop, t_review | — |
| 上传 | — | （FileStorageService） | — |
