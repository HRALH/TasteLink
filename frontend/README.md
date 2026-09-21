# TasteLink 前端

TasteLink 餐饮口碑社区 Web 端（SPA）。对应设计文档 `docs/01-需求文档.md`、`docs/03-前端模块划分.md`、`docs/05-接口API设计.md`。

## 技术栈

| 层 | 选型 | 说明 |
|---|---|---|
| 框架 | React 19 + Vite（TypeScript） | 文档规划 React 18，模板默认 React 19（18 的超集，AntD5 已支持） |
| UI | Ant Design 5 | 表单/列表/上传/分页/消息 |
| 全局状态 | Zustand（persist） | 登录态 token/userInfo，持久化 localStorage |
| 路由 | React Router v6 | 路由表 + 路由守卫（RequireAuth） |
| 请求 | axios | baseURL + JWT 拦截器 + 统一错误 + 401 跳登录 |
| 数据层 | @tanstack/react-query 5 | 列表/详情的缓存与失效；通知未读数 30s 轮询（`refetchInterval`） |

## 目录结构

```
src/
├── main.tsx               入口（ConfigProvider 中文 + 主题色）
├── App.tsx                BrowserRouter 包裹路由
├── api/                   接口层（与后端 1:1）：request / auth / user / shop /
│                          review / interaction / follow / home / file /
│                          notification / report / feed / admin
├── store/authStore.ts     登录态（persist）
├── router/index.tsx       路由表 + RequireAuth / RequireAdmin 守卫
├── types/api.ts           统一返回体 R / PageResult / 各 VO 契约
├── utils/                 constants(错误码/城市/排序) · thumb(对象存储缩略图) ·
│                          compressImage(上传前 canvas 压缩) · jwt · message · motion
├── components/            通用组件：ShopCard / ReviewCard / UploadImage /
│                          FollowButton / UserCard / FollowList / AuthShell /
│                          ReportButton / QueryError，以及 editorial/(Eyebrow /
│                          SectionTitle / PullQuote / RankBadge)、admin/、layout/MainLayout
└── pages/                 home / auth / shop / review / user / following /
                           notification / admin / common(404)
```

## 命令

```bash
npm install        # 安装依赖
npm run dev        # 开发，http://localhost:5173
npm run build      # 类型检查 + 生产构建（tsc -b && vite build）
npm run lint       # oxlint
npm run preview    # 预览构建产物
```

## 环境与联调

- `VITE_API_BASE_URL`：默认 `/api/v1`，dev 经 `vite.config.ts` 代理 `/api → http://localhost:8080`，避免 CORS。
- 后端启动在 `8080` 即可联调；后端未就绪时页面骨架仍可渲染，接口请求会失败。
- 鉴权：登录后 token 持久化，请求自动注入 `Authorization: Bearer`；401 清登录态跳 `/login` 回带 `redirect`。

## 里程碑（docs/03 §6）

| 里程碑 | 内容 | 状态 |
|---|---|---|
| M10 | 工程搭建：Vite + 依赖 + request/authStore/路由/守卫/布局 | ✅ |
| M8 | 登录/注册（密码校验 + redirect 回跳） | ✅ |
| M9 | 首页（城市筛选 + 热门店铺/点评） | ✅ |
| M10b | 店铺列表（搜索/分类/城市/排序/分页）+ 详情（点评列表） | ✅ |
| M11 | 发点评（UploadImage 多图 + 评分）+ 点评详情 | ✅ |
| M12 | 点赞（乐观更新、幂等）+ 评论列表/输入 | ✅ |
| M13 | 用户主页 + 编辑资料 + 关注/粉丝列表 | ✅ |
| M14 | 前后端联调验收 | ✅ 已联调,并已部署到公网服务器(`docs/14`) |
| v2 F1 | 通知铃铛(未读数 30s 轮询)+ 通知列表页 | ✅ |
| v2 F3 | 关注 feed(`/following`,最新/最热排序) | ✅ |
| v2 F4 | 举报入口(`ReportButton`)+ 管理员内容治理页 | ✅ |
| v2 FE-B/C | 管理员后台(店铺编辑乐观锁 409)+ 视觉系统「赤金暖纸」重构 + 前端测试基线 | ✅ |

> 通用约定见 `docs/03 §7`：图片统一走 `/files/image` 拿 URL 后再提交；分页 `page/size` + `PageResult`；排序字典见 `utils/constants.ts`。
