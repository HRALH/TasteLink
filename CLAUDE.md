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

## Commands

### Backend (`backend/`, Spring Boot 3.2 + Java 17, no Maven wrapper — use system `mvn`)
```bash
cd backend
mvn spring-boot:run            # run dev server on :8080
mvn clean package              # build jar (target/)
mvn test                       # run unit tests (Mockito-based; no Spring context, so no DB/Redis needed)
mvn test -Dtest=ClassName#method  # single-test pattern (class#method)
mvn test -DRUN_IT=true -Dtest=HotRankServiceIT  # Testcontainers IT — needs local Docker to start a real Redis container
```
Requires MySQL 8 reachable per `application.yml` defaults (`localhost:3306/tastelink`). DB is **not** auto-initialized — `spring.sql.init.mode=never`; run `db/schema.sql` then `db/data.sql` manually before first start.

### Frontend (`frontend/`, React 19 + Vite + TypeScript)
```bash
cd frontend
npm install
npm run dev        # dev server http://localhost:5173 (proxies /api → http://localhost:8080)
npm run build      # tsc -b && vite build (typecheck is part of build)
npm run lint       # oxlint
npm run preview    # serve build output
```
There is no test runner configured. Start the backend first for full-stack dev; the SPA renders its skeleton even when the API is down.

## Configuration & secrets

- `application.yml` is committed with **placeholders only** (env-var refs). Real DB password, OSS AK/SK, and JWT secret go in `application-local.yml`, which is gitignored. Copy from `application-local.yml.example`.
- Default active profile is `local`. Override per-environment via `SPRING_PROFILES_ACTIVE` / env vars (`DB_HOST`, `JWT_SECRET`, `STORAGE_TYPE=oss|local`, `CORS_ORIGINS`, etc.).
- **Redis** (v2 Phase A — 点评审热度排行缓存): config under `spring.data.redis.*` / `tastelink.rank.*`. Env: `REDIS_HOST`/`REDIS_PORT`/`REDIS_PASSWORD`/`REDIS_DB`; set `RANK_CACHE_ENABLED=false` to fall back to pure MySQL ranking. Local: `docker compose up -d redis`. Lettuce connects lazily — the app starts fine without Redis; only the home hot-review path degrades to MySQL on cache miss/error. The Testcontainers IT (`HotRankServiceIT`) is gated behind `-DRUN_IT=true` and skipped in a normal `mvn test` (no Docker required).
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
- **Logical foreign keys** (no physical FK), referential integrity validated in Service. **Soft delete** is a `status` column (1 normal / 0 hidden), filtered per-query — the global MyBatis-Plus logic-delete plugin is intentionally **not** enabled.

## Frontend architecture

`src/api/*` is a 1:1 client per backend module; all routes go through `src/api/request.ts` — the single axios instance whose:
- baseURL is `VITE_API_BASE_URL` (default `/api/v1`, proxied to `:8080` in dev so there's no CORS during local dev),
- request interceptor injects `Authorization: Bearer ${token}` from the Zustand auth store,
- response interceptor **unwraps `R.data`**, treats idempotent codes `40902`/`40903` as success (returns `data`), and on HTTP 401 clears auth + redirects to `/login?redirect=...`.

So API client functions return `Promise<T>` of the already-unwrapped `data` — never the `R` envelope. Error codes live in `src/utils/constants.ts` (`Code`) and must stay aligned with `docs/05 §3.2` and backend `ResultCode`.

- **Auth state**: `src/store/authStore.ts` (Zustand + `persist`) holds `token`/`userInfo`, persisted to `localStorage` key `tastelink-auth`. `isLoggedIn` gates routes.
- **Routing** (`src/router/index.tsx`): `RequireAuth` wraps protected routes (`/me`, `/shops/:id/review`); auth pages render without `MainLayout`.
- **Contracts** (`src/types/api.ts`): shared `R` / `PageResult` / VO shapes matching backend DTOs.
- **Image upload convention** (`docs/03 §7`): first `POST /files/image` to get a URL, then include that URL in the review submit — never inline binary in JSON.

### Visual system — 「赤金暖纸 · 美食杂志风」
The frontend was deliberately reskinned away from Ant Design's default orange to an editorial food-magazine aesthetic. This is load-bearing, not cosmetic, and has a **single source of truth**:
- `src/styles/tokens.ts` — palette (`paper`/`surface`/`ink`/`appetite` `#C8102E`/`ember`/`gold`/`rule`/`muted`), radius, space, shadow, font stacks.
- `src/index.css` — `:root` CSS variables + editorial utility classes (`.eyebrow`, `.editorial-title`, `.pullquote`, `.hairline`, `.tl-card`, `.tl-rise`).
- `src/main.tsx` — AntD `ConfigProvider` theme tokens.

Rules when touching UI: pull all colors/values from `tokens.ts` (`index.css` `:root`) — **no scattered hex**. Headings use Noto Serif SC + Playfair Display SC; body uses Karla + Noto Sans SC. No emoji as icons — use `@ant-design/icons`. Signature editorial components (`Eyebrow`, `SectionTitle`, `PullQuote`, `RankBadge`) live in `src/components/editorial/`; `RankBadge` only where the rank itself is the information (hot lists), never as decorative numbering on ordinary cards.
