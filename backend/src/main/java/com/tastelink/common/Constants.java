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

    /** 单条点评图片上限。 */
    public static final int REVIEW_IMAGE_MAX = 9;

    /** 密码最小长度。 */
    public static final int PASSWORD_MIN_LEN = 8;
}
