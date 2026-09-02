package com.tastelink.common;

import lombok.Getter;

/**
 * 业务返回码与对应 HTTP 状态。
 * code 写入响应体；httpStatus 由 GlobalExceptionHandler 设置到 HTTP 响应。
 */
@Getter
public enum ResultCode {

    SUCCESS(0, 200, "success"),

    BAD_REQUEST(400, 400, "请求参数错误"),
    UNAUTHORIZED(401, 401, "未登录或登录已失效"),
    FORBIDDEN(403, 403, "无操作权限"),
    NOT_FOUND(404, 404, "资源不存在"),
    SERVER_ERROR(500, 500, "服务器内部错误"),

    // 业务码（幂等冲突等，HTTP 200，前端按成功处理）
    USER_EXISTS(40901, 200, "用户名已存在"),
    ALREADY_LIKED(40902, 200, "已点赞"),
    ALREADY_FOLLOWED(40903, 200, "已关注"),
    CANNOT_FOLLOW_SELF(40904, 200, "不可关注自己"),

    // v2 Phase B：管理员并发改店铺乐观锁冲突（HTTP 409，需前端提示刷新）
    SHOP_VERSION_CONFLICT(409, 409, "店铺已被他人修改，请刷新重试");

    private final int code;
    private final int httpStatus;
    private final String message;

    ResultCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
