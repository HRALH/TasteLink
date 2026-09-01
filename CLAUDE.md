# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

TasteLink 是面向城市餐饮推荐的口碑社区（MVP v1）：店铺浏览 + 点评分 UGC + 点赞/评论/关注社交闭环。仓库目前含两部分：

- `docs/` — 五份中文设计文档（需求 / 后端模块 / 前端模块 / DB 表 / 接口 API），是整个项目的**事实源**；前端严格对齐 `docs/05-接口API设计.md`。
- `frontend/` — React 19 + Vite + TypeScript SPA，本仓库唯一已落地代码。

后端（`docs/02-后端模块划分.md`、`docs/04-数据库表设计.md` 规划的 Spring 服务）**尚未在本仓库实现**。前端按 `docs/05` 的契约独立开发，dev 通过 Vite 代理打 `http://localhost:8080`；后端未起时页面骨架仍可渲染、接口请求会失败。不要在此仓库找后端代码。前端里程碑 M8–M13 已完成，M14 联调验收待后端就绪。

## 常用命令

所有命令在 `frontend/` 下执行：

```bash
cd frontend
npm install
npm run dev        # Vite dev，http://localhost:5173，/api 代理到 localhost:8080
npm run build      # tsc -b && vite build —— 这是类型检查入口（无独立 tsc/typecheck 脚本）
npm run lint       # oxlint
npm run preview    # 预览构建产物
```

- **构建即类型门**：`npm run build` 先 `tsc -b` 做全量类型检查再打包；验证类型用 `npm run build`，别去找不存在的 `tsc`/`typecheck` 脚本。
- **没有测试框架**：仓库未配置任何测试 runner / `test` 脚本，不要假定 Jest/Vitest 存在。

## 前端架构（读多文件才能理解的大图）

请求链路（一切 HTTP 都走这条管子）：
`pages/* → api/*（与后端 1:1）→ src/api/request.ts 的 http 封装 → axios 实例`

`src/api/request.ts` 是核心约定：
- **baseURL** = `VITE_API_BASE_URL`（默认 `/api/v1`，dev 经 Vite 代理转后端）。
- **请求拦截**：从 `useAuthStore` 注入 `Authorization: Bearer ${token}`。
- **响应拦截解包 `R`**：后端统一返回体 `{ code, message, data }`。`code===0` 成功 → 直接返回 `data`；`code!==0` `message.error` 并 reject。**幂等码特殊处理**：`ALREADY_LIKED`(40902)/`ALREADY_FOLLOWED`(40903) 按业务成功返回 `data`，不报错（满足点赞/关注的乐观更新语义）。因此各 `api/*.ts` 导出的 `http.get<T>` 返回的是**已脱壳的 `Promise<T>`**，不是 `Promise<R<T>>`。
- **HTTP 401**：清登录态 + 跳 `/login?redirect=...`（带去重，避免重复跳转）。

错误码、分页默认值、城市/排序字典全在 `src/utils/constants.ts`（`Code` / `ShopSort` / `ReviewSort` / `CITIES` / `MAX_REVIEW_IMAGES=9` 等）。后端契约类型在 `src/types/api.ts`（VO / 请求体 / 结果体），严格对齐 `docs/05 §5` —— **改契约务必先改 docs 再改 types，勿重复定义**。

路由与鉴权（`src/router/index.tsx`）：React Router v6。登录/注册页**无顶栏布局**；其余页包在 `MainLayout`（带头部）里。`RequireAuth` 守卫对未登录跳 `/login` 并带 `redirect`（登录后回跳）。目前仅 `/shops/:id/review`（发点评）与 `/me`（我的）需登录。

登录态（`src/store/authStore.ts`）：Zustand + `persist`，localStorage key = `tastelink-auth`（`AUTH_STORAGE_KEY`）。存 `token` + `UserInfo`，刷新恢复；暴露 `login/logout/updateProfile`。组件里用 `useAuthStore` 选择器订阅，避免整 store 重渲染。

图片上传约定（**易踩坑**）：点评/头像**绝不把 File/Base64 直接提交业务接口**。先调 `api/file.ts` 的 `uploadImage(file)`（`POST /files/image`，multipart）拿到 `{ url, ossKey }`，再把 `url` 随表单提交（`docs/03 §5.3`）。点评最多 `MAX_REVIEW_IMAGES=9` 张。

分页统一：请求 `{ page?, size? }`（默认 `DEFAULT_PAGE=1` / `DEFAULT_SIZE=10`），响应 `PageResult<T>`（`{ records, total, current, size, pages }`）。列表排序用 `ShopSort`/`ReviewSort` 枚举字符串传 `sortBy`。

## 视觉系统：赤金暖纸 · 美食杂志风

单一事实源三处，改动需保持一致：`src/styles/tokens.ts`（palette/font/space/radius/shadow/motion）+ `src/index.css` 的 `:root` 变量与编辑式工具类（`.eyebrow` `.editorial-title` `.pullquote` `.hairline` `.tl-card` `.tl-rise`）+ `src/main.tsx` 的 AntD `ConfigProvider` 主题。改 UI 时务必遵守：

- **禁止散落 hex**：所有颜色取自 `tokens.ts` 的 `palette`。主色食欲红 `#C8102E`（hover `#A30D24` 深红），暖金 `#B7791F` 仅用于精选/排行，暖纸白 `#FBF6EE` 页底，暖墨黑 `#241A14` 字，暖灰棕 `#8A7B6B` 次文。
- **字体分工**：标题宋体（`Noto Serif SC`）+ Latin `Playfair Display`（品牌字标用 Playfair Display SC 小型大写）；正文 `Karla` + `Noto Sans SC`。Google Fonts 在 `index.html` 引入（`display=swap`）。
- **编辑式组件优先复用** `src/components/editorial/`：`Eyebrow` / `SectionTitle`（eyebrow + 宋体标题，替代散落的 AntD `Typography.Title` 章节头）/ `PullQuote`（点评正文左 ❝ + 宋体引文）/ `RankBadge`。`RankBadge` **仅**用于"排序即信息"的热门榜，普通卡片不加装饰性序号。
- **图标走 `@ant-design/icons`，不用 emoji 当图标**。

## 约定

- UI 为中文（`ConfigProvider locale={zhCN}`），源码注释为中文；写代码时注释风格、用词跟周围代码保持一致。
- 后端契约在 `docs`，前端只消费别自造；遇到字段/接口改动先回 `docs/05-接口API设计.md` 对齐。
- 提交前用 `git status --short` 核对暂存范围。本仓库 `.gitignore` 已排除 `.env*`、`application-local.yml`、`*.key`/`*.pem`、本地 `data/uploads`、工具环境文件（`.agents/` `.sc/` `skills-lock.json`）、`backend/target`、`frontend/node_modules`/`dist`。真实凭据（AK/SK、`LTAI` 开头、真实口令、真实 endpoint/bucket）不入库，配置只写占位符 / 环境变量引用。
