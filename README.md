# TasteLink

> 城市餐饮口碑社区 —— 发布点评、点赞评论,用 UGC 沉淀一座城市的味道。

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6db33f)
![React](https://img.shields.io/badge/React-19-61dafb)
![Vite](https://img.shields.io/badge/Vite-8-646cff)
![MySQL](https://img.shields.io/badge/MySQL-8-4479a1)

TasteLink 是一个 Monorepo(Spring Boot 后端 + Vite/React 前端 + 中文设计文档),以最小代价验证「店铺浏览 + 点评 UGC + 社交互动」的核心闭环,形成基础的餐饮口碑社区。前端采用「赤金暖纸 · 美食杂志风」编辑式视觉系统。

## 功能特性

- **用户**:账号密码注册/登录(JWT 无状态鉴权,BCrypt 加密),个人主页与资料编辑
- **店铺**:列表(关键词/分类/城市筛选 + 热度排序)、详情、分类字典
- **点评**:对店铺发布图文点评(1~5 分 + 最多 9 张图),店铺下点评流(按时间/热度排序)
- **互动**:点赞、评论(单层,无楼中楼),全部幂等(唯一约束 + 受影响行数保证)
- **关注**:关注/取关、关注列表与粉丝列表,不可关注自己
- **关注动态**:`/following` 只展示已关注用户的点评,支持按最新/最热排序 (v2 F3)
- **通知**:被赞/被评/被关注产生站内通知,顶栏铃铛每 30 秒轮询未读数,支持单条/全部标记已读 (v2 F1)
- **举报与内容治理** (v2 F4):点评/评论/用户/店铺均可举报(唯一约束幂等,重复举报提示「已举报」);管理员可下架/恢复点评,下架即同事务回扣店铺评分与点评数、恢复时校验店铺状态避免虚增
- **首页**:热门店铺 + 热门点评,按城市筛选
- **图片存储**:本地磁盘 / 阿里云 OSS / 腾讯云 COS 三选一(改 `STORAGE_TYPE` 即切换),业务代码只依赖 `FileStorageService` 接口、永不自行拼 URL;上传前前端 canvas 等比压缩,后端做扩展名白名单 + 文件头魔数校验(防改名 webshell)
- **管理员后台**(v2 Phase B):`ADMIN` 角色(ride 在 JWT claim)经 `/api/v1/admin/**` 编辑/删除店铺,并发改同店走 MyBatis-Plus `@Version` 乐观锁,后提交者获 409「请刷新重试」
- **热度排行缓存**(v2 Phase A):首页热门点评读 Redis ZSet(`ZREVRANGE`),点赞在事务提交后(afterCommit)写 ZSet,`ScheduledRankRebuild` 定时对账修复漂移,`RANK_CACHE_ENABLED=false` 降级回 MySQL
- **店铺删除延时清理**(v2 Phase C / C-Full):删店先标记下架(同事务级联 `status=0` + 回扣计数),`afterCommit` 发 RabbitMQ 延时消息(TTL+DLX,免插件),消费端级联物理删评论/点赞/图片,`ScheduledShopCleanupReconcile` 对账兜底
- **关键词检索**(v2 Phase D):店铺 `keyword` 命中走 Elasticsearch(`ShopDoc` + Criteria),异常/禁用降级回 MySQL `LIKE`,`ScheduledShopReconcile` 定时全量重建索引

> MVP 范围与边界见 [`docs/01-需求文档.md`](docs/01-需求文档.md)。v2 中间件升级(见 [`docs/06`](docs/06-中间件升级开发计划.md))已引入 Redis / RabbitMQ / Elasticsearch,三阶段均带特性开关可降级回 MySQL;中间件缺位时应用仍可启动(各自懒加载)。**Canal binlog 增量同步、IK 中文分词**为 infra 待办,当前以定时全量重建兜底同步。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.2.5 · Java 17 · MyBatis-Plus 3.5.7 · Spring Security · jjwt 0.12.5 · MySQL 8 · 阿里云 OSS SDK 3.17.4 · 腾讯云 COS SDK 5.6.279 |
| 前端 | React 19 · Vite 8 · TypeScript 6 · Ant Design 5 · Zustand 5 · react-router-dom 6 · axios |
| 校验/Lint | spring-boot-starter-validation · oxlint |
| 中间件(v2) | Redis 7(Lettuce) · RabbitMQ 3.13(AMQP) · Elasticsearch 8.11(Spring Data ES),均带特性开关可降级回 MySQL |
| 测试(v2) | spring-boot-starter-test(Mockito 单测) · Testcontainers(MySQL/Redis/RabbitMQ/ES 集成测试,`-DRUN_IT=true`) |
| 文档 | `docs/` 下 15 份中文文档(需求 / 后端 / 前端 / 数据库 / API / 中间件升级 / 前后端优化 / 产品优化 / 功能清单 / 本地启动 / 云部署 / 迭代发布),索引见文末「文档」 |

## 快速开始

### 环境要求

JDK 17+、Maven(系统 `mvn`,无 wrapper)、**Node 22+**、MySQL 8。v2 中间件可选:`docker compose up -d redis rabbitmq elasticsearch`(缺位时对应特性自动降级回 MySQL)。

> Node 版本有下限:前端 Vite 8 要求 `^20.19.0 || >=22.12.0`;而 `npm run test:run` 因为 `jsdom → undici 8` 需要 **≥22.19**,跑在 Node 20 上会缺 `webidl.util.markAsUncloneable`、vitest 的 forks worker 全部起不来。本仓库与 CI 统一用 **Node 24**。

### 1. 克隆

```bash
git clone https://github.com/HRALH/TasteLink.git
cd TasteLink
```

### 2. 初始化数据库(MySQL 不会自动建表)

`spring.sql.init.mode=never`,需手动建库与导入:

```sql
CREATE DATABASE tastelink DEFAULT CHARACTER SET utf8mb4;
```

依次执行 `backend/src/main/resources/db/schema.sql`(建表)→ `data.sql`(种子数据:店铺 / 分类 / 城市)。

### 3. 配置后端真实凭据

`application.yml` 只含占位符。复制示例并填入本地凭据:

```bash
cp backend/src/main/resources/application-local.yml.example \
   backend/src/main/resources/application-local.yml
```

填入 DB 密码、JWT secret(≥32 字符);若用对象存储再填 `OSS_*`(阿里云)或 `COS_*`(腾讯云);若启 v2 中间件再填 `REDIS_*`/`RABBITMQ_*`/`ES_URIS`。`application-local.yml` 已 gitignore,不会提交。

### 4. 启动后端(:8080)

```bash
cd backend
mvn spring-boot:run
```

### 5. 启动前端(:5173)

```bash
cd frontend
npm install
npm run dev
```

Vite 把 `/api` 代理到 `http://localhost:8080`,本地开发无 CORS。访问 http://localhost:5173 。

> 前端即使后端未启也能渲染骨架页;完整功能需后端在线。

## 配置项

通过环境变量或 `application-local.yml` 覆盖(默认值见 `application.yml`):

| 变量 | 说明 | 默认 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Spring profile | `local` |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | 数据库地址 | `localhost:3306/tastelink` |
| `DB_USER` / `DB_PASSWORD` | 数据库账号 | `root` / 空 |
| `JWT_SECRET` | JWT 签名密钥(≥32 字符) | 占位符(必须替换) |
| `STORAGE_TYPE` | 文件存储 | `local`(兜底) / `oss` / `cos` |
| `OSS_ENDPOINT` / `OSS_AK` / `OSS_SK` / `OSS_BUCKET` / `OSS_DOMAIN` | 阿里云 OSS 凭据 | 空 |
| `COS_REGION` / `COS_SECRET_ID` / `COS_SECRET_KEY` / `COS_BUCKET` / `COS_DOMAIN` | 腾讯云 COS 凭据(建议子账号密钥、只授权该桶;`region` 须与服务器同地域) | `ap-shanghai` / 空 |
| `CORS_ORIGINS` | CORS 白名单 | `http://localhost:5173,http://localhost:3000` |
| `REDIS_HOST`/`REDIS_PORT`/`REDIS_PASSWORD`/`REDIS_DB` | Redis(v2 Phase A) | `localhost:6379`/空/`0` |
| `RABBITMQ_HOST`/`_PORT`/`_USER`/`_PASSWORD`/`_VHOST` | RabbitMQ(v2 Phase C) | `localhost:5672`/`guest`/`/` |
| `ES_URIS` | Elasticsearch(v2 Phase D) | `http://localhost:9200` |
| `RANK_CACHE_ENABLED`/`RABBIT_ENABLED`/`SEARCH_ENABLED` | v2 三阶段特性开关 | `true`,置 `false` 降级回 MySQL |
| `RANK_REBUILD_CRON`/`SEARCH_REBUILD_CRON`/`RABBIT_CLEANUP_RECON_CRON` | ZSet / ES 索引 / 删店对账 定时重建 cron | `0 */10 * * * *` 等 |
| `RABBIT_CLEANUP_DELAY_MS` | 删店延时清理窗口(delay-queue `x-message-ttl`) | `5000ms` |

> 三种存储形态:`local` 写入 `./data/uploads/yyyy/MM/uuid.ext`,经后端 `/static/uploads/**` 提供(库内存的是**绝对 URL**,换域名要同步改库);`oss` / `cos` 写入 `uploads/yyyy/MM/uuid.ext`,用自定义 `domain` 或各自默认域名对外。`oss` 与 `cos` **同构**(同一 key 形态与 URL 反推规则),切换时清理与解析逻辑不用改;COS 的桶与服务器**同地域**时默认域名会自动解析到内网、流量不计费,`local → COS` 的迁移步骤见 [`docs/14-云服务器部署指南.md`](docs/14-云服务器部署指南.md) §12。

## 项目结构

```
TasteLink/
├── backend/                # Spring Boot 后端
│   └── src/main/java/com/tastelink/
│       ├── config/         # Security / MybatisPlus / Oss / Storage / MetaObjectHandler / RabbitMQConfig / ShopIndexInitializer / Scheduled*
│       ├── controller/     # /api/v1 下 13 个模块(含 admin)
│       ├── service+impl/   # 业务逻辑(含幂等计数模式、HotRank/Search/ShopCleanup)
│       ├── entity/mapper/  # 10 张表实体 + MyBatis-Plus mapper(+ShopDoc ES 文档)
│       ├── security/       # JwtAuthFilter / JwtUtil / SecurityContextHelper
│       └── common/         # R<T> 统一返回体 / ResultCode / PageResult
├── frontend/               # Vite + React SPA
│   └── src/
│       ├── api/            # 1:1 后端模块的 axios 客户端(request.ts 解包 R.data)
│       ├── store/          # Zustand auth 状态(localStorage 持久化)
│       ├── components/     # editorial/ 编辑式组件 + layout/
│       ├── pages/          # home / auth / shop / user / review / common / admin
│       ├── styles/         # tokens.ts —— 视觉 token 单一来源
│       └── router/         # RequireAuth / RequireAdmin 路由守卫
├── docker/                  # 全栈部署:Dockerfile.backend / Dockerfile.frontend / nginx.conf / docker-compose.yml
└── docs/                   # 01 需求 · 02 后端 · 03 前端 · 04 数据库 · 05 API · 06 中间件升级 · 07 前端计划 · 08 运维部署 · 09~11 优化清单 · 12 功能清单 · 13 本地启动 · 14 云部署 · 15 迭代发布
```

## API 速览

统一前缀 `/api/v1`,统一返回 `{code, message, data}`(`code=0` 成功),分页返回 `PageResult`。完整契约见 [`docs/05-接口API设计.md`](docs/05-接口API设计.md)。

| 模块 | 主要端点 | 鉴权 |
|---|---|---|
| 认证 | `POST /auth/register` · `POST /auth/login` | 公开 |
| 用户 | `GET/PUT /users/me` · `GET /users/{id}` · `GET /users/{id}/reviews` | GET 用户主页公开 |
| 店铺 | `GET /shops` · `GET /shops/{id}` · `GET /shops/{id}/reviews` · `GET /shops/categories` | 公开 |
| 点评 | `POST /shops/{id}/reviews` · `GET /reviews/{id}` | 发布需登录 |
| 互动 | `POST/DELETE /reviews/{id}/likes` · `GET/POST /reviews/{id}/comments` | 写需登录 |
| 关注 | `POST/DELETE /users/{id}/follow` · `GET /users/{id}/followings\|followers` | 写需登录 |
| 关注动态(v2 F3) | `GET /feed/following` | 需登录 |
| 通知(v2 F1) | `GET /notifications` · `GET /notifications/unread-count` · `POST /notifications/{id}/read` · `POST /notifications/read-all` | 需登录 |
| 举报(v2 F4) | `POST /reports`(`targetType` ∈ `REVIEW`/`COMMENT`/`USER`/`SHOP`,幂等) | 需登录 |
| 首页 | `GET /home?city=` | 公开 |
| 上传 | `POST /files/image`(`multipart/form-data`) | 需登录 |
| 管理后台(v2) | `PUT /admin/shops/{id}`(编辑,409 并发冲突) · `DELETE /admin/shops/{id}`(延时清理) · `GET /admin/reviews` + `PUT /admin/reviews/{id}/hide\|restore`(内容治理) · `GET /admin/reports`(举报队列) | `ADMIN` |

> 幂等码:`40902`(已点赞)、`40903`(已关注)由后端按 HTTP 200 返回,前端 `request.ts` 拦截器当成功处理;`40905`(已举报过该内容)同样返回 200,但**不做特殊分支**,直接走默认错误提示展示后端文案。`40901` 用户名已存在、`40904` 不可关注自己、`42901` 登录尝试过于频繁(登录限流,Redis 计数,Redis 不可用时放行不阻断登录);v2 新增 `409`(HTTP) `SHOP_VERSION_CONFLICT` —— 管理员并发改同店,`request.ts` 当可处理码抛 `ApiError(code)`,前端弹「加载最新内容」。

## 数据模型(10 张表)

| 表 | 用途 |
|---|---|
| `t_user` | 用户,冗余关注 / 粉丝 / 点评计数;`role`(v2:`USER` 默认 / `ADMIN`) |
| `t_shop` / `t_shop_category` | 店铺与分类字典,冗余评分 / 点评 / 点赞计数;`t_shop.version`(v2 乐观锁) |
| `t_review` | 点评,冗余城市 + 点赞 / 评论计数 |
| `t_review_image` | 点评图片(有序,最多 9);`oss_key` 供删店时清理存储 |
| `t_review_like` | 点赞关系(`uk_review_user` 唯一约束) |
| `t_review_comment` | 单层评论(无 `parent_id`) |
| `t_follow` | 关注关系(`uk_follower_followee` 唯一约束) |
| `t_notification` | 站内通知(v2 F1):接收者 `user_id` + 触发者 `actor_id`,昵称/头像读时回查,不做快照 |
| `t_report` | 内容举报(v2 F4):`uk_reporter_target` 唯一约束保证幂等 |

DDL + 种子数据见 [`docs/04-数据库表设计.md`](docs/04-数据库表设计.md) 与 `backend/src/main/resources/db/`。逻辑删除用 `status` 字段(1 正常 / 0 隐藏),未启用 MyBatis-Plus 全局逻辑删除插件。

## 视觉系统 ——「赤金暖纸 · 美食杂志风」

前端刻意脱离 AntD 默认橘色,采用编辑式美食杂志美学,有单一 token 来源:

- **色板**:`paper` / `surface` / `ink` / `appetite #C8102E` / `ember` / `gold` / `rule` / `muted`,全部从 `src/styles/tokens.ts`(`index.css :root`)取值,禁散落 hex
- **字体**:标题 Noto Serif SC + Playfair Display SC,正文 Karla + Noto Sans SC
- **组件**:`Eyebrow`、`SectionTitle`、`PullQuote`、`RankBadge` 等位于 `src/components/editorial/`;`RankBadge` 仅用于"排名即信息"的榜单,不作普通卡片的装饰编号

详见 [`docs/03-前端模块划分.md`](docs/03-前端模块划分.md)。

## 开发命令

```bash
# 后端
cd backend
mvn spring-boot:run          # 开发服务 :8080
mvn clean package            # 打包到 target/
mvn test                     # 单元测试(Mockito,不需 DB/Redis);Testcontainers 集成测试需本地 Docker
mvn test -DRUN_IT=true -Dtest=HotRankServiceIT    # 真 Redis — 点评审热度排行
mvn test -DRUN_IT=true -Dtest=AdminShopServiceIT # 真 MySQL — @Version 乐观锁 + 角色鉴权
mvn test -DRUN_IT=true -Dtest=ShopCleanupIT       # 真 RabbitMQ+MySQL — 店铺删除延时清理
mvn test -DRUN_IT=true -Dtest=SearchServiceIT     # 真 ES+MySQL — 关键词检索

# 前端
cd frontend
npm run dev                  # 开发服务 :5173
npm run build                # tsc -b && vite build(含类型检查)
npm run lint                 # oxlint
npm run preview              # 预览构建产物
```

> 跨文件的开发约定、架构内幕与容易踩坑的敏感点(白名单顺序、幂等计数模式、统一返回体、v2 各 Phase 落地细节)详见 [`CLAUDE.md`](CLAUDE.md)。

## Docker 部署

一键全栈(MySQL + Redis + RabbitMQ + Elasticsearch + 前后端),核心命令(须在仓库根、带 `--project-directory .`):

```bash
# 1) 在**仓库根**创建 .env(不是 docker/.env —— compose 插值只从「工作目录+项目目录」读,
#    模板见 docs/08 §4.2;至少填 JWT_SECRET / DB_PASSWORD)
# 2) 构建并拉起(首次加 --build):
docker compose --project-directory . -f docker/docker-compose.yml up -d --build
# 前端 http://localhost:8081 · 后端 :8080 · RabbitMQ 管理界面 :15672 · ES :9200
docker compose --project-directory . -f docker/docker-compose.yml logs -f backend
docker compose --project-directory . -f docker/docker-compose.yml down        # 停
```

`docker/`: `Dockerfile.backend`(多阶段 Maven→JRE)、`Dockerfile.frontend`(Node→nginx 反代)、`nginx.conf`、`docker-compose.yml`。完整步骤、`.env` 模板、镜像构建说明、中间件配置/降级、测试与生产清单见 [`docs/08-运维部署指南.md`](docs/08-运维部署指南.md)。注意 `.env` 已被 gitignore(`.env*`),不入库。

要部署到**公网云服务器**,照 [`docs/14-云服务器部署指南.md`](docs/14-云服务器部署指南.md) 走(选型 → 安全组 → 构建 → 逐项自检 → 域名 HTTPS → 图片迁 COS);上线之后每次改代码按 [`docs/15-迭代更新与发布流程.md`](docs/15-迭代更新与发布流程.md) 发布。只想在本机最快跑起来看效果,用 [`docs/13-从零启动指南.md`](docs/13-从零启动指南.md)。

## 文档

| 文档 | 内容 |
|---|---|
| [`docs/01-需求文档.md`](docs/01-需求文档.md) | 需求与里程碑、功能优先级、MVP 边界(§6.2 已标注 v2 转正项) |
| [`docs/02-后端模块划分.md`](docs/02-后端模块划分.md) | 后端模块划分与关键技术设计 |
| [`docs/03-前端模块划分.md`](docs/03-前端模块划分.md) | 前端模块划分、视觉系统 |
| [`docs/04-数据库表设计.md`](docs/04-数据库表设计.md) | 8 表 DDL + 种子数据(role/version 列) |
| [`docs/05-接口API设计.md`](docs/05-接口API设计.md) | REST 契约、错误码、VO(含 admin 端点 + 409) |
| [`docs/06-中间件升级开发计划.md`](docs/06-中间件升级开发计划.md) | v2 Redis/MQ/ES 升级计划与实施状态(§0.6) |
| [`docs/07-前端开发计划.md`](docs/07-前端开发计划.md) | v2 前端重构计划(管理后台 / 视觉系统 / 测试) |
| [`docs/08-运维部署指南.md`](docs/08-运维部署指南.md) | 本地起栈 / Docker 全栈 / `.env` / 中间件降级 / 测试 / 生产清单 |
| [`docs/09-后端优化方案.md`](docs/09-后端优化方案.md) | 后端优化执行计划(安全 P0 / 正确性 / 索引 / 可观测性,`feature/backend-optimization` 分支) |
| [`docs/10-前端优化方案.md`](docs/10-前端优化方案.md) | 前端优化执行计划(真 bug / 数据获取层 / 构建瘦身 / 图片 / 测试,`feature/frontend-optimization` 分支) |
| [`docs/11-产品优化与执行清单.md`](docs/11-产品优化与执行清单.md) | 产品视角优化(F1 通知 / F2 冷启动种子点评 / F3 关注 feed / F4 内容治理 / F5 评分体感)与取舍、延后项 |
| [`docs/12-功能清单与说明.md`](docs/12-功能清单与说明.md) | 功能视角逐项清单:每个功能标注鉴权要求、对应接口、读/写表 |
| [`docs/13-从零启动指南.md`](docs/13-从零启动指南.md) | 面向外部 clone 者的手把手:本地原生起栈 + Docker 全栈一键起 + 提权管理员 + 排错 |
| [`docs/14-云服务器部署指南.md`](docs/14-云服务器部署指南.md) | 公网部署全流程:选型 / 安全组 / 内核参数与 swap / 构建启动 / 逐项自检 / 域名 HTTPS / 备份 / **§12 图片迁移到腾讯云 COS** |
| [`docs/15-迭代更新与发布流程.md`](docs/15-迭代更新与发布流程.md) | 上线**之后**怎么改:本地自检 → CI 门禁 → 按改动范围重建 → 数据安全边界 → 数据库结构变更 → 回滚 → 验收 |

## 开源协议

本仓库尚未声明开源协议(无 `LICENSE` 文件)。默认著作权保留;如需引用或二次开发,请先与作者联系或补加协议(推荐 MIT / Apache-2.0)。
