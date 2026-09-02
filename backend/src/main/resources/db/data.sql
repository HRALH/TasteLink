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
