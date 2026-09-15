-- =====================================================================
-- TasteLink 数据库 Schema (MySQL 8.x / InnoDB / utf8mb4)
-- 关联文档: docs/04-数据库表设计.md
-- 使用方式:
--   1) 先创建库: CREATE DATABASE IF NOT EXISTS tastelink DEFAULT CHARSET utf8mb4;
--   2) USE tastelink;  (或在连接串中带上库名)
--   3) 执行本脚本建表;  再执行 data.sql 写入种子数据
--   说明: 全部逻辑外键,无物理 FK; 软删除用 status(1正常/0隐藏)
-- =====================================================================

-- 3.1 用户表
CREATE TABLE IF NOT EXISTS `t_user` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`        VARCHAR(64)  NOT NULL                COMMENT '用户名(登录名,唯一,不可改)',
  `password`        VARCHAR(100) NOT NULL                COMMENT '密码(BCrypt哈希)',
  `nickname`        VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT '昵称',
  `avatar_url`      VARCHAR(512) NOT NULL DEFAULT ''     COMMENT '头像URL',
  `bio`             VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '个人简介',
  `following_count` INT          NOT NULL DEFAULT 0     COMMENT '关注数(冗余)',
  `follower_count`  INT          NOT NULL DEFAULT 0     COMMENT '粉丝数(冗余)',
  `review_count`    INT          NOT NULL DEFAULT 0     COMMENT '发布点评数(冗余)',
  `role`            VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT '角色:USER普通/ADMIN管理员(v2 Phase B)',
  `status`          TINYINT      NOT NULL DEFAULT 1     COMMENT '状态:1正常 0禁用',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 3.2 店铺分类字典表
CREATE TABLE IF NOT EXISTS `t_shop_category` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `code`        VARCHAR(32) NOT NULL                COMMENT '分类编码(HOTPOT/COFFEE/BBQ/JAPANESE)',
  `name`        VARCHAR(32) NOT NULL                COMMENT '分类中文名(火锅/咖啡/烧烤/日料)',
  `sort_order`  INT         NOT NULL DEFAULT 0      COMMENT '展示排序,越小越靠前',
  `icon_url`    VARCHAR(512) NOT NULL DEFAULT ''     COMMENT '分类图标URL',
  `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺分类字典表';

-- 3.3 店铺表
CREATE TABLE IF NOT EXISTS `t_shop` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '店铺ID',
  `name`         VARCHAR(128) NOT NULL                COMMENT '店铺名称',
  `category_id`  BIGINT       NOT NULL                COMMENT '店铺分类ID(逻辑外键->t_shop_category.id)',
  `city`         VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT '所在城市(首页按城市筛选)',
  `address`      VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '详细地址',
  `longitude`    DECIMAL(10,7) NULL                  COMMENT '经度(MVP预留,不参与筛选)',
  `latitude`     DECIMAL(10,7) NULL                  COMMENT '纬度(MVP预留,不参与筛选)',
  `phone`        VARCHAR(32)  NOT NULL DEFAULT ''     COMMENT '联系电话',
  `cover_url`    VARCHAR(512) NOT NULL DEFAULT ''     COMMENT '店铺封面图URL',
  `description`  TEXT         NULL                   COMMENT '店铺简介',
  `rating_sum`   INT          NOT NULL DEFAULT 0     COMMENT '评分总和(冗余,算avg用)',
  `avg_rating`   DECIMAL(3,2) NOT NULL DEFAULT 0.00  COMMENT '平均评分(冗余,0.00-5.00)',
  `review_count` INT          NOT NULL DEFAULT 0     COMMENT '点评数(冗余,首页热度依据)',
  `like_count`   INT          NOT NULL DEFAULT 0     COMMENT '本店点评累计点赞数(冗余,热度加权用)',
  `version`      INT          NOT NULL DEFAULT 0     COMMENT '乐观锁版本号(v2 Phase B,管理员并发改店铺防覆盖)',
  `status`       TINYINT      NOT NULL DEFAULT 1     COMMENT '状态:1正常 0下架',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_city_status_review`      (`city`, `status`, `review_count`),
  KEY `idx_category_status_review`  (`category_id`, `status`, `review_count`),
  KEY `idx_name`                     (`name`),
  -- B3-1: 首页/全局热榜 WHERE status=1 ORDER BY review_count DESC, like_count DESC（无 city 前缀时用）
  KEY `idx_status_review_like`      (`status`, `review_count` DESC, `like_count` DESC),
  -- B3-1: 删店对账扫描 WHERE status=0 AND update_time<?（ScheduledShopCleanupReconcile）
  KEY `idx_status_update`           (`status`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺表';

-- 3.4 点评表
CREATE TABLE IF NOT EXISTS `t_review` (
  `id`          BIGINT    NOT NULL AUTO_INCREMENT COMMENT '点评ID',
  `shop_id`     BIGINT    NOT NULL                COMMENT '所属店铺ID(逻辑外键->t_shop.id)',
  `user_id`     BIGINT    NOT NULL                COMMENT '发布用户ID(逻辑外键->t_user.id)',
  `city`        VARCHAR(64) NOT NULL DEFAULT ''   COMMENT '冗余店铺城市(首页按城市精选点评,避免JOIN)',
  `content`     TEXT      NOT NULL                COMMENT '点评文字内容',
  `rating`      TINYINT   NOT NULL                COMMENT '评分1-5',
  `like_count`  INT       NOT NULL DEFAULT 0     COMMENT '点赞数(冗余)',
  `reply_count` INT       NOT NULL DEFAULT 0     COMMENT '评论数(冗余)',
  `status`      TINYINT   NOT NULL DEFAULT 1     COMMENT '状态:1正常 0隐藏',
  `create_time` DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_shop_status_create` (`shop_id`, `status`, `create_time`),
  KEY `idx_user_status_create` (`user_id`, `status`, `create_time`),
  KEY `idx_status_like`        (`status`, `like_count`),
  KEY `idx_city_like`          (`city`, `status`, `like_count`),
  -- B3-1: 店铺点评按热度排序 WHERE shop_id=? AND status=1 ORDER BY like_count DESC（ReviewServiceImpl sortBy=like）
  KEY `idx_shop_status_like`   (`shop_id`, `status`, `like_count` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点评表';

-- 3.5 点评图片表
CREATE TABLE IF NOT EXISTS `t_review_image` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `review_id`   BIGINT       NOT NULL                COMMENT '点评ID(逻辑外键->t_review.id)',
  `url`         VARCHAR(512) NOT NULL                COMMENT '图片可访问URL',
  `oss_key`     VARCHAR(512) NOT NULL DEFAULT ''    COMMENT 'OSS对象key(删除时清理存储用)',
  `sort_order`  INT          NOT NULL DEFAULT 0      COMMENT '展示排序',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_review_sort` (`review_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点评图片表';

-- 3.6 点评点赞表
CREATE TABLE IF NOT EXISTS `t_review_like` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `review_id`  BIGINT   NOT NULL                COMMENT '被点赞的点评ID',
  `user_id`    BIGINT   NOT NULL                COMMENT '点赞用户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_user` (`review_id`, `user_id`),
  KEY `idx_user_create` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点评点赞表(MVP仅点评可点赞)';

-- 3.7 点评评论表
CREATE TABLE IF NOT EXISTS `t_review_comment` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `review_id`   BIGINT       NOT NULL                COMMENT '所属点评ID(逻辑外键->t_review.id)',
  `user_id`     BIGINT       NOT NULL                COMMENT '评论用户ID',
  `content`     VARCHAR(500) NOT NULL                COMMENT '评论内容',
  `status`      TINYINT      NOT NULL DEFAULT 1     COMMENT '状态:1正常 0隐藏',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_review_status_create` (`review_id`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点评评论表(MVP单层)';

-- 3.8 关注关系表
CREATE TABLE IF NOT EXISTS `t_follow` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `follower_id` BIGINT   NOT NULL                COMMENT '关注者(发起关注的用户ID)',
  `followee_id` BIGINT   NOT NULL                COMMENT '被关注者(被关注的用户ID)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follower_followee` (`follower_id`, `followee_id`),
  KEY `idx_follower_create` (`follower_id`, `create_time`),
  KEY `idx_followee_create`  (`followee_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注关系表';

-- 3.9 站内通知表（产品优化 F1：补齐社交反馈闭环——被赞/被评/被关注时写一条,
--   接收者在顶栏红点感知；关系式而非快照——actor_id 关联 t_user,读时批量回查昵称/头像避免过期）
CREATE TABLE IF NOT EXISTS `t_notification` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id`      BIGINT       NOT NULL                COMMENT '接收者(被互动方)',
  `actor_id`     BIGINT       NOT NULL                COMMENT '触发者(点赞/评论/关注的人)',
  `type`         VARCHAR(32)  NOT NULL                COMMENT '类型:REVIEW_LIKED/REVIEW_COMMENTED/USER_FOLLOWED',
  `target_type`  VARCHAR(16)  NOT NULL                COMMENT '目标:REVIEW/USER',
  `target_id`    BIGINT       NOT NULL                COMMENT '目标ID',
  `preview`      VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '快照摘要(如评论内容片段)',
  `is_read`      TINYINT      NOT NULL DEFAULT 0     COMMENT '0未读 1已读',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_read_create` (`user_id`, `is_read`, `create_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知表';

-- 3.10 内容举报表（产品优化 F4：对外前最低安全网。uk_reporter_target 限同一用户对同一目标
--   只记一条,重复举报幂等；admin 可在后台据状态下架点评,本表为线索。敏感词/审核工作流后置）
CREATE TABLE IF NOT EXISTS `t_report` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '举报ID',
  `reporter_id` BIGINT       NOT NULL                COMMENT '举报人(登录用户)',
  `target_type` VARCHAR(16)  NOT NULL                COMMENT '目标:REVIEW/COMMENT/USER/SHOP',
  `target_id`   BIGINT       NOT NULL                COMMENT '目标ID',
  `reason`      VARCHAR(255) NOT NULL                COMMENT '举报理由(枚举文案 KEY)',
  `status`      VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING 待处理 / RESOLVED 已处理',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reporter_target` (`reporter_id`, `target_type`, `target_id`),
  KEY `idx_status_create` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容举报表';
