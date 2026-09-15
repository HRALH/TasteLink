package com.tastelink.common;

/**
 * 全局常量。
 */
public final class Constants {

    private Constants() {
    }

    /** 接口统一前缀。 */
    public static final String API_V1 = "/api/v1";

    /** 状态字段取值。 */
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_HIDDEN = 0;

    /** 分页默认值。 */
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 50;

    /** 评分范围（含两端）。 */
    public static final int RATING_MIN = 1;
    public static final int RATING_MAX = 5;

    /** 角色（v2 Phase B）：存 t_user.role / JWT role claim。 */
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    /** 单条点评图片上限。 */
    public static final int REVIEW_IMAGE_MAX = 9;

    /** 密码最小长度。 */
    public static final int PASSWORD_MIN_LEN = 8;

    /** 通知类型（产品优化 F1，存 t_notification.type）。 */
    public static final String NOTIFY_REVIEW_LIKED = "REVIEW_LIKED";
    public static final String NOTIFY_REVIEW_COMMENTED = "REVIEW_COMMENTED";
    public static final String NOTIFY_USER_FOLLOWED = "USER_FOLLOWED";

    /** 通知目标类型。 */
    public static final String TARGET_REVIEW = "REVIEW";
    public static final String TARGET_USER = "USER";

    /** 举报目标类型（产品优化 F4，存 t_report.target_type）。 */
    public static final String REPORT_TARGET_REVIEW = "REVIEW";
    public static final String REPORT_TARGET_COMMENT = "COMMENT";
    public static final String REPORT_TARGET_USER = "USER";
    public static final String REPORT_TARGET_SHOP = "SHOP";

    /** 举报状态。 */
    public static final String REPORT_PENDING = "PENDING";
    public static final String REPORT_RESOLVED = "RESOLVED";
}
