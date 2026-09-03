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
- **首页**:热门店铺 + 热门点评,按城市筛选
- **图片上传**:阿里云 OSS 为主、本地存储兜底,业务代码只依赖 `FileStorageService` 接口
- **管理员后台**(v2 Phase B):`ADMIN` 角色(ride 在 JWT claim)经 `/api/v1/admin/**` 编辑/删除店铺,并发改同店走 MyBatis-Plus `@Version` 乐观锁,后提交者获 409「请刷新重试」
- **热度排行缓存**(v2 Phase A):首页热门点评读 Redis ZSet(`ZREVRANGE`),点赞在事务提交后(afterCommit)写 ZSet,`ScheduledRankRebuild` 定时对账修复漂移,`RANK_CACHE_ENABLED=false` 降级回 MySQL
- **店铺删除延时清理**(v2 Phase C / C-Full):删店先标记下架(同事务级联 `status=0` + 回扣计数),`afterCommit` 发 RabbitMQ 延时消息(TTL+DLX,免插件),消费端级联物理删评论/点赞/图片,`ScheduledShopCleanupReconcile` 对账兜底
- **关键词检索**(v2 Phase D):店铺 `keyword` 命中走 Elasticsearch(`ShopDoc` + Criteria),异常/禁用降级回 MySQL `LIKE`,`ScheduledShopReconcile` 定时全量重建索引

> MVP 范围与边界见 [`docs/01-需求文档.md`](docs/01-需求文档.md)。v2 中间件升级(见 [`docs/06`](docs/06-中间件升级开发计划.md))已引入 Redis / RabbitMQ / Elasticsearch,三阶段均带特性开关可降级回 MySQL;中间件缺位时应用仍可启动(各自懒加载)。**Canal binlog 增量同步、IK 中文分词**为 infra 待办,当前以定时全量重建兜底同步。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.2.5 · Java 17 · MyBatis-Plus 3.5.7 · Spring Security · jjwt 0.12.5 · MySQL 8 · 阿里云 OSS SDK |
| 前端 | React 19 · Vite 8 · TypeScript 6 · Ant Design 5 · Zustand 5 · react-router-dom 6 · axios |
| 校验/Lint | spring-boot-starter-validation · oxlint |
| 中间件(v2) | Redis 7(Lettuce) · RabbitMQ 3.13(AMQP) · Elasticsearch 8.11(Spring Data ES),均带特性开关可降级回 MySQL |
| 测试(v2) | spring-boot-starter-test(Mockito 单测) · Testcontainers(MySQL/Redis/RabbitMQ/ES 集成测试,`-DRUN_IT=true`) |
| 文档 | `docs/` 下 7 份中文设计文档(需求 / 后端 / 前端 / 数据库 / API / 中间件升级计划 / 前端开发计划) |

## 快速开始

### 环境要求

JDK 17+、Maven(系统 `mvn`,无 wrapper)、Node 20+、MySQL 8。v2 中间件可选:`docker compose up -d redis rabbitmq elasticsearch`(缺位时对应特性自动降级回 MySQL)。

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

填入 DB 密码、JWT secret(≥32 字符);若用 OSS 再填 AK/SK;若启 v2 中间件再填 `REDIS_*`/`RABBITMQ_*`/`ES_URIS`。`application-local.yml` 已 gitignore,不会提交。

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
| `STORAGE_TYPE` | 文件存储 | `local`(兜底) / `oss` |
| `OSS_ENDPOINT` / `OSS_AK` / `OSS_SK` / `OSS_BUCKET` / `OSS_DOMAIN` | OSS 凭据 | 空 |
| `CORS_ORIGINS` | CORS 白名单 | `http://localhost:5173,http://localhost:3000` |
| `REDIS_HOST`/`REDIS_PORT`/`REDIS_PASSWORD`/`REDIS_DB` | Redis(v2 Phase A) | `localhost:6379`/空/`0` |
| `RABBITMQ_HOST`/`_PORT`/`_USER`/`_PASSWORD`/`_VHOST` | RabbitMQ(v2 Phase C) | `localhost:5672`/`guest`/`/` |
| `ES_URIS` | Elasticsearch(v2 Phase D) | `http://localhost:9200` |
| `RANK_CACHE_ENABLED`/`RABBIT_ENABLED`/`SEARCH_ENABLED` | v2 三阶段特性开关 | `true`,置 `false` 降级回 MySQL |
| `RANK_REBUILD_CRON`/`SEARCH_REBUILD_CRON`/`RABBIT_CLEANUP_RECON_CRON` | ZSet / ES 索引 / 删店对账 定时重建 cron | `0 */10 * * * *` 等 |
| `RABBIT_CLEANUP_DELAY_MS` | 删店延时清理窗口(delay-queue `x-message-ttl`) | `5000ms` |

> 本地存储(`STORAGE_TYPE=local`)写入 `./data/uploads/yyyy/MM/uuid.ext`,经 `/static/uploads/**` 静态资源对外提供;OSS 模式用自定义 `domain` 或默认 `https://{bucket}.{endpoint}`。

## 项目结构

```
TasteLink/
├── backend/                # Spring Boot 后端
│   └── src/main/java/com/tastelink/
│       ├── config/         # Security / MybatisPlus / Oss / Storage / MetaObjectHandler / RabbitMQConfig / ShopIndexInitializer / Scheduled*
│       ├── controller/     # /api/v1 下 9 个模块(含 admin)
│       ├── service+impl/   # 业务逻辑(含幂等计数模式、HotRank/Search/ShopCleanup)
│       ├── entity/mapper/  # 8 张表实体 + MyBatis-Plus mapper(+ShopDoc ES 文档)
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
└── docs/                   # 01 需求 · 02 后端 · 03 前端 · 04 数据库 · 05 API · 06 中间件升级 · 07 前端计划
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
| 首页 | `GET /home?city=` | 公开 |
| 上传 | `POST /files/image`(`multipart/form-data`) | 需登录 |
| 管理后台(v2) | `PUT /admin/shops/{id}`(编辑,409 并发冲突) · `DELETE /admin/shops/{id}`(延时清理) | `ADMIN` |

> 幂等码:`40902`(已点赞)、`40903`(已关注)由后端按 HTTP 200 返回,前端 `request.ts` 拦截器当成功处理。`40901` 用户名已存在、`40904` 不可关注自己;v2 新增 `409`(HTTP) `SHOP_VERSION_CONFLICT` —— 管理员并发改同店,`request.ts` 当可处理码抛 `ApiError(code)`,前端弹「加载最新内容」。

## 数据模型(8 张表)

| 表 | 用途 |
|---|---|
| `t_user` | 用户,冗余关注 / 粉丝 / 点评计数;`role`(v2:`USER` 默认 / `ADMIN`) |
| `t_shop` / `t_shop_category` | 店铺与分类字典,冗余评分 / 点评 / 点赞计数;`t_shop.version`(v2 乐观锁) |
| `t_review` | 点评,冗余城市 + 点赞 / 评论计数 |
| `t_review_image` | 点评图片(有序,最多 9) |
| `t_review_like` | 点赞关系(`uk_review_user` 唯一约束) |
| `t_review_comment` | 单层评论(无 `parent_id`) |
| `t_follow` | 关注关系(`uk_follower_followee` 唯一约束) |

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

## 开源协议

本仓库尚未声明开源协议(无 `LICENSE` 文件)。默认著作权保留;如需引用或二次开发,请先与作者联系或补加协议(推荐 MIT / Apache-2.0)。
