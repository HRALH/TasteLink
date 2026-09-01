package com.tastelink.common;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回体：{ code, message, data }，code=0 表示业务成功。
 */
@Data
@NoArgsConstructor
public class R<T> {

    private int code;
    private String message;
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.message = ResultCode.SUCCESS.getMessage();
        r.data = data;
        return r;
    }

    public static <T> R<T> fail(ResultCode rc) {
        R<T> r = new R<>();
        r.code = rc.getCode();
        r.message = rc.getMessage();
        return r;
    }

    public static <T> R<T> fail(ResultCode rc, String message) {
        R<T> r = new R<>();
        r.code = rc.getCode();
        r.message = message != null ? message : rc.getMessage();
        return r;
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
