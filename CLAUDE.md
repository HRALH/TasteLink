# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository overview

TasteLink is a 城市餐饮口碑社区 (city food review community) — a monorepo with a Spring Boot backend (`backend/`), a Vite + React frontend (`frontend/`), and Chinese design docs (`docs/`). The product is a food-recall/review social platform: users post reviews (with images, ratings) on shops, like/comment, follow other users, browse by city.

`docs/` is the canonical spec and is already implemented end-to-end:
- `01-需求文档.md` — requirements & milestones
- `02-后端模块划分.md` — backend module split & key technical design (read this before backend work)
- `03-前端模块划分.md` — frontend module split
- `04-数据库表设计.md` — 8 tables DDL + seed data (`backend/src/main/resources/db/schema.sql`, `data.sql`)
- `05-接口API设计.md` — REST contract, all paths/params/returns. **API changes must update this doc and be reflected in both `frontend/src/api/*` and the relevant backend controller.**
- `06-中间件升级开发计划.md` — v2 middleware upgrade plan (Redis 热度排行 / 角色鉴权+乐观锁 / RabbitMQ 延时清理 / ES+Canal). **主序列已完成并合入 `main`** — Phase 0/A/B/C(C-Full)/D 应用层 + Testcontainers 测试基线 + 文档同步全部落地;§0.6 给出逐阶段核验表。Canal binlog 增量同步、IK 中文分词、XXL-JOB、Redisson 为**计划内延后/infra 待办**;各 Phase 落地细节见下文「Backend architecture」对应小节。
- `07-前端开发计划.md` — v2 frontend plan (admin 后台 FE-0+B+C / 视觉系统「赤金暖纸」重构 / 前端测试基线)。
- `08-运维部署指南.md` — ops/deploy guide (本地起栈 / `docker/` 全栈 Docker 部署 / `.env` 模板 / 中间件配置与降级 / 测试 / 生产清单)。`docker/` 目录含 `Dockerfile.backend`、`Dockerfile.frontend`、`nginx.conf`、`docker/docker-compose.yml`(全栈编排,须 `--project-directory .` 运行)。

## Commands

### Backend (`backend/`, Spring Boot 3.2 + Java 17, no Maven wrapper — use system `mvn`)
```bash
cd backend
mvn spring-boot:run            # run dev server on :8080
mvn clean package              # build jar (target/)
mvn test                       # run unit tests (Mockito-based; no Spring context, so no DB/Redis needed)
mvn test -Dtest=ClassName#method  # single-test pattern (class#method)
mvn test -DRUN_IT=true -Dtest=HotRankServiceIT  # Testcontainers IT — needs local Docker to start a real Redis container
mvn test -DRUN_IT=true -Dtest=AdminShopServiceIT # Testcontainers IT — real MySQL; drives the real @Version interceptor + role gating (unlike HotRankServiceIT, uses @SpringBootTest)
mvn test -DRUN_IT=true -Dtest=ShopCleanupIT     # Testcontainers IT — real RabbitMQ+MySQL; drives the TTL+DLX cleanup pipeline + consumer
mvn test -DRUN_IT=true -Dtest=SearchServiceIT # Testcontainers IT — real Elasticsearch+MySQL; drives keyword search + filters + paging
```
Requires MySQL 8 reachable per `application.yml` defaults (`localhost:3306/tastelink`). DB is **not** auto-initialized — `spring.sql.init.mode=never`; run `db/schema.sql` then `db/data.sql` manually before first start.

### Frontend (`frontend/`, React 19 + Vite + TypeScript)
```bash
cd frontend
npm install
npm run dev        # dev server http://localhost:5173 (proxies /api → http://localhost:8080)
npm run build      # tsc -b && vite build (typecheck is part of build)
npm run lint       # oxlint
npm run test       # vitest watch (jsdom + RTL + msw)
npm run test:run   # vitest one-shot
npm run preview    # serve build output
```
Tests live under `src/test/` (vitest config in `vitest.config.ts`); `src/test/**` is excluded from `tsc -b` so build ignores test files. Start the backend first for full-stack dev; the SPA renders its skeleton even when the API is down.

## Configuration & secrets

- `application.yml` is committed with **placeholders only** (env-var refs). Real DB password, OSS AK/SK, and JWT secret go in `application-local.yml`, which is gitignored. Copy from `application-local.yml.example`.
- Default active profile is `local`. Override per-environment via `SPRING_PROFILES_ACTIVE` / env vars (`DB_HOST`, `JWT_SECRET`, `STORAGE_TYPE=oss|local`, `CORS_ORIGINS`, etc.).
- **Redis** (v2 Phase A — 点评审热度排行缓存): config under `spring.data.redis.*` / `tastelink.rank.*`. Env: `REDIS_HOST`/`REDIS_PORT`/`REDIS_PASSWORD`/`REDIS_DB`; set `RANK_CACHE_ENABLED=false` to fall back to pure MySQL ranking. Local: `docker compose up -d redis`. Lettuce connects lazily — the app starts fine without Redis; only the home hot-review path degrades to MySQL on cache miss/error. The Testcontainers IT (`HotRankServiceIT`) is gated behind `-DRUN_IT=true` and skipped in a normal `mvn test` (no Docker required).
- **RabbitMQ** (v2 Phase C — 店铺删除延时清理): config under `spring.rabbitmq.*` / `tastelink.rabbitmq.*`. Env: `RABBITMQ_HOST`/`RABBITMQ_PORT`/`RABBITMQ_USER`/`RABBITMQ_PASSWORD`/`RABBITMQ_VHOST`; set `RABBIT_ENABLED=false` to skip MQ and have the reconcile scheduler do physical cleanup directly (app still starts fine without a broker — AMQP is lazy). Local: `docker compose up -d rabbitmq`. Cleanup delay window via `RABBIT_CLEANUP_DELAY_MS` (delay-queue `x-message-ttl`, default 5000ms); reconcile cadence `RABBIT_CLEANUP_RECON_CRON` (default `0 */2 * * * *`). Uses **TTL+DLX (no plugin)** — standard `rabbitmq:3-management` image works for both local + Testcontainers. The IT (`ShopCleanupIT`) is `@SpringBootTest` + real RabbitMQ+MySQL, gated behind `-DRUN_IT=true`.
- **Elasticsearch** (v2 Phase D — 店铺关键词检索): config under `spring.elasticsearch.*` / `tastelink.search.*`. Env: `ES_URIS` (default `http://localhost:9200`); set `SEARCH_ENABLED=false` to skip ES and fall back to MySQL `LIKE` for keyword (app starts fine without ES — ES is lazy, `ShopIndexInitializer` silently no-ops). Local: `docker compose up -d elasticsearch` (single-node, xpack security off). Rebuild cadence `SEARCH_REBUILD_CRON` (default `0 */10 * * * *`). Uses standard `elasticsearch:8.x` image + default analyzer; **IK Chinese tokenizer + Canal binlog sync are deferred infra** (see arch bullet). The IT (`SearchServiceIT`) is `@SpringBootTest` + real ES+MySQL, gated behind `-DRUN_IT=true`.
- File storage is swappable via `storage.type`: `local` (default) writes to `./data/uploads/yyyy/MM/uuid.ext` and serves via `/static/uploads/**`; `oss` uses Aliyun OSS. Both impl `FileStorageService`; business code depends only on the interface and never constructs URLs.
- **Never commit real credentials.** `application-local.yml`, `.env*`, `*.key/*.pem`, and `**/data/uploads/` are gitignored — keep them out of commits.

## Backend architecture

Package root `com.tastelink` (see `docs/02` for the full map): `config / common / exception / security / entity / mapper / service+impl / controller / dto(request|response) / utils`. Controllers all mount under `/api/v1`.

Conventions that span multiple files and aren't obvious from a single one:
- **Unified envelope**: every controller returns `R<T> = {code, message, data}`; `code=0` = success. `GlobalExceptionHandler` (`@RestControllerAdvice`) maps validation→400, `BusinessException`→its code, auth→401/403, and a catch-all→500 — and does **not** leak SQL/stacks to the client. Throw `BusinessException(ResultCode.X, msg)` for business errors.
- **Pagination**: MyBatis-Plus `PaginationInnerInterceptor(MYSQL)` (in `MybatisPlusConfig`); use `selectPage(page, wrapper)` and convert via `PageResult.from(page, vos)`. Params `page` (1-based) / `size`. Complex SQL goes in `resources/mapper/*.xml`.
- **Auto-fill**: `MetaObjectHandler` fills `createTime` (INSERT) and `updateTime` (INSERT+UPDATE); entities annotate with `@TableField(fill=...)`.
- **Stateless JWT security** (`SecurityConfig`): `SessionCreationPolicy.STATELESS`, CSRF off, `JwtAuthFilter` before `UsernamePasswordAuthenticationFilter`. Filter silently skips on bad tokens (the `authenticationEntryPoint` renders 401 as `R`). **Whitelist order is sensitive**: `GET /users/me` (authenticated) MUST be declared before `GET /users/**` (permitAll). Public = auth endpoints + GET browse endpoints (shops/reviews/home/public user profiles). Everything else requires login. Get the caller via `SecurityContextHelper.getCurrentUserId()`.
- **Passwords**: `BCryptPasswordEncoder(strength=10)`. Reponse `VO`s must never include `password`.
- **Idempotent counters** (like/follow): rely on unique constraints (`uk_review_user`, `uk_follower_followee`); catch `DuplicateKeyException` and treat as success returning current count — never "read-then-write". Cancel/unfollow: decrement only when `affected rows > 0`, always via `GREATEST(0, n-1)`. All redundant counter updates (review.like_count, shop aggregates) happen inside the **same `@Transactional` method** as the row write. See `InteractionServiceImpl` for the reference pattern.
- **Hot review ranking** (v2 Phase A): `ReviewServiceImpl.hotReviews` reads from a Redis ZSet (`HotRankService.topReviewIds`) when no city filter and cache enabled — batch-loads reviews by id, **reorders to the cache rank**, and falls back to MySQL (`hotReviewsFromDb`) otherwise. Likes/unlikes push the ZSet in an `afterCommit` hook (inside the insert try-block, so it fires only on a real new like — reuses the idempotency pattern above; DB rollback never advances the cache). `ScheduledRankRebuild` reconciles drift from MySQL. Composite score = `like_count*100 + min(reply_count,99)`; Phase A caches **global** hot only — city-filtered home still hits MySQL.
- **Admin role & optimistic lock** (v2 Phase B): `t_user.role` (`USER` default / `ADMIN`) rides in the JWT `role` claim; `LoginUser.getAuthorities()` emits `ROLE_<role>` so `SecurityConfig`'s `requestMatchers("/api/v1/admin/**").hasRole("ADMIN")` (declared **before** `anyRequest().authenticated()`) gates the admin path. `AdminShopService.updateShop` edits shops via optimistic lock: `Shop.version` is `@Version`, `MybatisPlusConfig` registers `OptimisticLockerInnerInterceptor` (**before** pagination) so `updateById` appends `WHERE id=? AND version=?` + `SET version+1`. **MP returns 0 affected rows on conflict — it does not throw `OptimisticLockingFailureException`** — so the service maps `affected==0` to `BusinessException(ResultCode.SHOP_VERSION_CONFLICT)`(409). `update(null, wrapper)` counter writes (entity null) skip the version path, so Phase A's `like_count` setSql updates are unaffected. Seeded `admin` account is a non-loginable placeholder; provision a real admin by registering then `UPDATE t_user SET role='ADMIN'` (see `db/data.sql`).
- **Shop delete MQ cleanup** (v2 Phase C, C-Full): `AdminShopService.deleteShop` runs a two-stage flow — a marking `@Transactional` sets `shop.status=0` + cascades `review.status=0` (immediately hides from all public reads, including home hot which filters `review.status=normal`) + **前置** mirrors the user `review_count` decrement (grouped per owner, `GREATEST(0,n-1)`); `afterCommit` then publishes a delayed `ShopCleanupMessage` (`ShopCleanupProducer` swallows broker errors — see Phase-A pattern). The consumer (`ShopCleanupConsumer`→`ShopCleanupService.cleanup`) does the **physical** cascade (image→like→comment→review→shop), deletes OSS/local images via `FileStorageService.ossKeyFromUrl(url)` (the `oss_key` column is stored empty — `url` is the sole reliable reverse-source), and `hotRankService.onDelete(rid)` (zrem). Reliability: TTL+DLX delayed delivery (**no plugin**; `shop.cleanup.delay.queue` ttl → `shop.cleanup.dlx` → `shop.cleanup.queue`); poison DLQ via `spring.rabbitmq.listener.simple.retry` + `default-requeue-rejected=false` + queue `x-dead-letter-exchange`. **No separate outbox table** — `ScheduledShopCleanupReconcile` `@Scheduled` scans lingering `status=0` shops past `delay+grace` and re-runs `cleanup` directly (idempotent) as the at-least-once backstop, mirroring Phase A's `ScheduledRankRebuild`. `RABBIT_ENABLED=false` still deletes shops (mark+hide+回扣 immediate; physical cleanup via reconcile).
- **ES shop keyword search** (v2 Phase D): `ShopController.listShops` routes `keyword` (non-blank) to `SearchService.searchByKeyword`, which queries ES via Spring-Data-ES `ElasticsearchOperations` + `Criteria` (`name` MUST matches + `status/categoryId/city` MUST filters + `PageRequest` 0-based), then reassembles `ShopVO` via `ShopService.toVOsByIds(orderedIds)` preserving ES relevance order (the MySQL回查 also re-checks `status=normal` — `selectBatchIds` doesn't filter status so a wrapper is used). The `ShopDoc` (`@Document(indexName="tastelink-shop", createIndex=false)`) carries only search/filter fields (no `avgRating/reviewCount/categoryName` — those come from the MySQL回查). **ES exceptions / disabled → returns `null`** (the controller falls back to `shopService.listShops`, i.e. the old `LIKE name`) — same swallow+fallback pattern as Phase A/C. `ShopIndexInitializer` `ApplicationRunner` creates the index (`createWithMapping`) if ES is up at boot; `ScheduledShopReconcile` `@Scheduled` full-rebuilds the index from MySQL `status=normal` shops (the only sync source since Canal is deferred, mirroring Phase A's reconcile). **Deferred infra**: (A) **IK Chinese tokenizer** — standard ES image has no IK, so CJK keyword hits are weak (≈​LIKE) until `analysis-ik` is installed + the index mapping specifies `ik_max_word`/`ik_smart` (requires reindex; doesn't touch app code); (B) **Canal binlog sync** — canal-server+canal-adapter are standalone processes (not app beans), untestable in-session, so committed infra-config was deferred — the scheduled reconcile keeps ES eventually consistent meanwhile. keyword is currently name-only (multi-field `should+filter` hits a `minimum_should_match` wrt a Criteria limitation; a follow-up `NativeQuery multi_match` over name/address/description is the natural extension — `ShopDoc` already indexes those).

## Frontend architecture

`src/api/*` is a 1:1 client per backend module; all routes go through `src/api/request.ts` — the single axios instance whose:
- baseURL is `VITE_API_BASE_URL` (default `/api/v1`, proxied to `:8080` in dev so there's no CORS during local dev),
- request interceptor injects `Authorization: Bearer ${token}` from the Zustand auth store,
- response interceptor **unwraps `R.data`**, treats idempotent codes `40902`/`40903` as success (returns `data`), and on HTTP 401 clears auth + redirects to `/login?redirect=...`. Non-success `code≠0` rejects an `ApiError` (carrying `code`/`data`); **processable codes** (`PROCESSABLE_CODES`, e.g. `SHOP_VERSION_CONFLICT=409`) suppress the auto-`message.error` and just throw `ApiError(code)` so callers branch (admin edit page → "加载最新内容" modal). `409` travels as **HTTP 409**, handled in the error branch.

So API client functions return `Promise<T>` of the already-unwrapped `data` — never the `R` envelope. Error codes live in `src/utils/constants.ts` (`Code`) and must stay aligned with `docs/05 §3.2` and backend `ResultCode`.

- **Auth state**: `src/store/authStore.ts` (Zustand + `persist`) holds `token`/`userInfo`/`role`/`isLoggedIn`, persisted to `localStorage` key `tastelink-auth`. `.isLoggedIn` gates routes; `isAdmin` (derived from `role === 'ADMIN'`) gates admin UI. **`role` is decoded from the JWT** (`src/utils/jwt.ts` — backend `LoginVO`/`UserVO` don't return role): purely advisory client-side; real `/admin/**` authorization is enforced by the backend `hasRole('ADMIN')` (403), so the decoded role only drives UI affordances, never access.
- **Routing** (`src/router/index.tsx`): `RequireAuth` wraps protected routes (`/me`, `/shops/:id/review`); `RequireAdmin` wraps admin routes (`/admin/shops`, `/admin/shops/:id/edit`) — not-logged-in → `/login`, logged-in non-admin → `/` (the backend still 403s real admin calls); auth pages render without `MainLayout`. Tests live in `src/test/` (vitest + RTL + msw), excluded from `tsc -b` via `tsconfig.app.json`.
- **Contracts** (`src/types/api.ts`): shared `R` / `PageResult` / VO shapes matching backend DTOs.
- **Image upload convention** (`docs/03 §7`): first `POST /files/image` to get a URL, then include that URL in the review submit — never inline binary in JSON.

### Visual system — 「赤金暖纸 · 美食杂志风」
The frontend was deliberately reskinned away from Ant Design's default orange to an editorial food-magazine aesthetic. This is load-bearing, not cosmetic, and has a **single source of truth**:
- `src/styles/tokens.ts` — palette (`paper`/`surface`/`ink`/`appetite` `#C8102E`/`ember`/`gold`/`rule`/`muted`), radius, space, shadow, font stacks.
- `src/index.css` — `:root` CSS variables + editorial utility classes (`.eyebrow`, `.editorial-title`, `.pullquote`, `.hairline`, `.tl-card`, `.tl-rise`).
- `src/main.tsx` — AntD `ConfigProvider` theme tokens.

Rules when touching UI: pull all colors/values from `tokens.ts` (`index.css` `:root`) — **no scattered hex**. Headings use Noto Serif SC + Playfair Display SC; body uses Karla + Noto Sans SC. No emoji as icons — use `@ant-design/icons`. Signature editorial components (`Eyebrow`, `SectionTitle`, `PullQuote`, `RankBadge`) live in `src/components/editorial/`; `RankBadge` only where the rank itself is the information (hot lists), never as decorative numbering on ordinary cards.
