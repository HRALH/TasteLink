-- 显式声明字符集，不依赖客户端默认值：MySQL 官方镜像 locale=C，容器内 mysql 客户端
-- 可能落到 latin1，会把文件里的 UTF-8 中文二次编码成乱码存库（见 docs/14 §5.2）。
SET NAMES utf8mb4;
-- =====================================================================
-- TasteLink 种子数据
-- 关联文档: docs/04-数据库表设计.md 第6章
-- 依赖: 先执行 schema.sql 建表
-- 说明: 示例店铺/演示用户仅用于联调与首页展示,正式环境由运营/导入脚本维护
--   - 演示用户 password 为占位 BCrypt 哈希,登录会失败;
--     真实可用账号请通过 POST /api/v1/auth/register 注册后再用。
-- =====================================================================

-- 店铺分类字典
INSERT INTO `t_shop_category` (`code`, `name`, `sort_order`) VALUES
('HOTPOT',  '火锅',  1),
('COFFEE',  '咖啡',  2),
('BBQ',     '烧烤',  3),
('JAPANESE','日料',  4),
('BARBECUE','串串',  5),
('DESSERT', '甜品',  6),
('CHINESE', '中餐',  7),
('WESTERN', '西餐',  8);

-- 示例店铺(category_id 对应上方种子字典 id)
INSERT INTO `t_shop` (`name`, `category_id`, `city`, `address`, `phone`, `cover_url`, `description`) VALUES
('辣府火锅(静安店)', 1, '上海', '上海市静安区南京西路1788号', '021-62880000', '', '人气重庆火锅'),
('豆咖啡 Roastery',   2, '上海', '上海市徐汇区永嘉路25号',     '021-64220000', '', '自家烘焙咖啡馆'),
('老王烧烤铺',        3, '北京', '北京市朝阳区三里屯北街8号', '010-65550000', '', '炭火烧烤'),
('樱日本料理',        4, '杭州', '杭州市西湖区龙井路1号',       '0571-88881234','','怀石日料');

-- 演示用户(密码为占位 BCrypt 哈希,实际由后端注册接口生成,此处仅占位不可登录)
INSERT INTO `t_user` (`username`, `password`, `nickname`, `bio`, `role`) VALUES
('demo_user', '$2a$10$placeholder_bcrypt_hash_replace_me', '美食探店达人', '记录城市味道', 'USER'),
-- 管理员占位账号(v2 Phase B):密码同样为占位哈希不可直接登录。真实管理员请先用
-- POST /api/v1/auth/register 注册普通账号,再执行 UPDATE t_user SET role='ADMIN' WHERE username=?
-- 提权(或用专用初始化脚本),避免在提交进仓库的种子文件中固化任何可用口令。
('admin', '$2a$10$placeholder_bcrypt_hash_replace_me', '运营管理员', '', 'ADMIN');

-- =====================================================================
-- 冷启动种子点评（产品优化 F2）：让新克隆后首屏不空——首页/店铺详情有真实图文点评，
-- 店铺评分/点评数为真实聚合值而非 0。计数与 createReview 增量逻辑手算对齐。
-- 说明:
--   - 作者用户(下述 t_seed_author)沿用「占位 BCrypt 哈希不可登录」约定,不固化可用口令;
--     通知/feed 演示用你自己注册的账号互动作即可。
--   - 图片用外部占位图(picsum.photos),需联网显示;DB 列无域名约束、读路径原样回放。
--   - review_id 按插入自增顺序 = 1..14(下文 like/image/comment 行据此引用,改顺序需同步调整)。
-- =====================================================================

-- 种子作者用户(id 3,4,5)——占位哈希不可登录,真名+外部头像
INSERT INTO `t_user` (`username`, `password`, `nickname`, `avatar_url`, `bio`, `role`) VALUES
('seed_wang',   '$2a$10$placeholder_bcrypt_hash_replace_me', '食探小王',   'https://picsum.photos/seed/avatar_wang/100/100',   '上海街头巷尾找味道', 'USER'),
('seed_coffee', '$2a$10$placeholder_bcrypt_hash_replace_me', '咖啡日记',   'https://picsum.photos/seed/avatar_coffee/100/100', '记录每一杯单品', 'USER'),
('seed_may',    '$2a$10$placeholder_bcrypt_hash_replace_me', '老饕阿May', 'https://picsum.photos/seed/avatar_may/100/100',  '走哪吃哪,只说真话', 'USER');

-- 点评(shop_id 1..4;city 冗余店铺城市;like_count/reply_count 与下方 like/comment 行一致)
-- shop1 辣府(上海,4 条) / shop2 豆咖啡(上海,3 条) / shop3 老王烧烤(北京,3 条) / shop4 樱日料(杭州,4 条)
INSERT INTO `t_review` (`shop_id`, `user_id`, `city`, `content`, `rating`, `like_count`, `reply_count`, `status`, `create_time`) VALUES
(1, 3, '上海', '锅底麻辣鲜香，毛肚新鲜脆嫩，蘸料台丰富，环境偏吵但氛围热闹，值得二刷。', 5, 2, 1, 1, '2026-08-20 19:30:00'),
(1, 5, '上海', '重庆朋友带去的，牛油锅底够劲，鸭肠爽脆，人均 150 性价比高。', 4, 0, 0, 1, '2026-08-22 12:15:00'),
(1, 4, '上海', '排队一小时但值得。鲜鸭血嫩滑，服务态度好，会主动加汤。', 5, 1, 0, 1, '2026-08-25 20:05:00'),
(2, 4, '上海', '手冲单品选了耶加雪菲，花果香明亮，店内安静适合带书坐一下午。', 5, 1, 1, 1, '2026-08-27 10:30:00'),
(2, 3, '上海', '冰美式偏苦， cheesecake 还行，适合办公但插座不多。', 4, 0, 0, 1, '2026-08-29 15:00:00'),
(2, 5, '上海', '埃塞尔比亚日晒做得很稳，老板会讲豆子故事，咖啡爱好者会喜欢。', 5, 0, 0, 1, '2026-09-01 09:45:00'),
(3, 5, '北京', '炭火烤肉串比电烤香太多，羊肉串和烤韭菜是必点，烟火气足。', 4, 1, 0, 1, '2026-09-03 21:10:00'),
(3, 3, '北京', '烧烤分量足，烤茄子入味，排烟还得抽好才行，整体满意。', 5, 0, 0, 1, '2026-09-05 18:50:00'),
(3, 4, '北京', '味道还行但偏咸，上菜略慢，工作日晚上人也不少。', 3, 0, 0, 1, '2026-09-07 19:20:00'),
(4, 5, '杭州', '怀石套餐精致克制，刺身新鲜，米饭和汤都讲究，适合慢吃细品。', 5, 2, 1, 1, '2026-09-08 13:25:00'),
(4, 3, '杭州', '环境优雅安静，天妇罗外衣薄脆不腻，服务到位。', 4, 0, 0, 1, '2026-09-09 12:40:00'),
(4, 4, '杭州', '鳗鱼饭火候得当，酱汁不抢味，性价比在日料里算合理。', 5, 1, 0, 1, '2026-09-10 19:30:00'),
(4, 3, '杭州', '午市定食很划算，小菜丰富，刺身量适中，适合工作日午餐。', 5, 0, 0, 1, '2026-09-11 12:10:00'),
(1, 5, '上海', '二刷，依旧水准在线。推荐九宫格，分味不串。', 4, 0, 0, 1, '2026-09-12 18:45:00');

-- 点评图片(8 条点评各 1 张外部占位图,sort_order=0,oss_key 留空)
INSERT INTO `t_review_image` (`review_id`, `url`, `oss_key`, `sort_order`) VALUES
(1,  'https://picsum.photos/seed/lafu1/600/400',  '', 0),
(2,  'https://picsum.photos/seed/lafu3/600/400',  '', 0),
(3,  'https://picsum.photos/seed/lafu2/600/400',  '', 0),
(4,  'https://picsum.photos/seed/coffee1/600/400', '', 0),
(7,  'https://picsum.photos/seed/bbq1/600/400',   '', 0),
(8,  'https://picsum.photos/seed/bbq2/600/400',   '', 0),
(10, 'https://picsum.photos/seed/ryo1/600/400',   '', 0),
(12, 'https://picsum.photos/seed/ryo2/600/400',   '', 0);

-- 点赞(8 行,review_id+user_id 唯一;对应各点评 like_count)
INSERT INTO `t_review_like` (`review_id`, `user_id`, `create_time`) VALUES
(1, 5, '2026-08-21 09:10:00'),
(1, 4, '2026-08-26 11:00:00'),
(3, 5, '2026-08-26 12:30:00'),
(4, 3, '2026-08-28 10:00:00'),
(7, 3, '2026-09-04 08:40:00'),
(10, 3, '2026-09-09 10:15:00'),
(10, 4, '2026-09-10 14:20:00'),
(12, 5, '2026-09-11 16:00:00');

-- 评论(3 行,对应 r1/r4/r10 的 reply_count)
INSERT INTO `t_review_comment` (`review_id`, `user_id`, `content`, `status`, `create_time`) VALUES
(1, 4, '同意，毛肚确实脆，蘸料台是亮点。', 1, '2026-08-21 20:05:00'),
(4, 5, '老板推荐的单品一般都不错。', 1, '2026-08-28 11:30:00'),
(10, 3, '下次带朋友去，环境适合小聚。', 1, '2026-09-09 13:00:00');

-- 计数对齐(createReview 走 setSql 增量、无 recompute,故种子手算一致):
-- shop like_count = 该店所有点评 like_count 之和;review_count/rating_sum 按点评聚合重算 avg
UPDATE `t_shop` SET `review_count`=4, `rating_sum`=18, `avg_rating`=ROUND(18/4,2), `like_count`=3  WHERE `id`=1; -- 辣府: r1(5)+r2(4)+r3(5)+r14(4)=18, likes=2+0+1+0
UPDATE `t_shop` SET `review_count`=3, `rating_sum`=14, `avg_rating`=ROUND(14/3,2), `like_count`=1  WHERE `id`=2; -- 豆咖啡: r4(5)+r5(4)+r6(5)=14, likes=1+0+0
UPDATE `t_shop` SET `review_count`=3, `rating_sum`=12, `avg_rating`=ROUND(12/3,2), `like_count`=1  WHERE `id`=3; -- 老王烧烤: r7(4)+r8(5)+r9(3)=12, likes=1+0+0
UPDATE `t_shop` SET `review_count`=4, `rating_sum`=19, `avg_rating`=ROUND(19/4,2), `like_count`=3  WHERE `id`=4; -- 樱日料: r10(5)+r11(4)+r12(5)+r13(5)=19, likes=2+0+1+0

-- 作者用户 review_count(本人点评条数)
UPDATE `t_user` SET `review_count`=5 WHERE `id`=3; -- seed_wang: r1,r5,r8,r11,r13
UPDATE `t_user` SET `review_count`=4 WHERE `id`=4; -- seed_coffee: r3,r4,r9,r12
UPDATE `t_user` SET `review_count`=5 WHERE `id`=5; -- seed_may: r2,r6,r7,r10,r14
