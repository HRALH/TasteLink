package com.tastelink.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期格式化工具：统一输出 yyyy-MM-dd HH:mm:ss。
 */
public final class DateUtil {

    private DateUtil() {
    }

    public static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String format(LocalDateTime time) {
        return time == null ? null : time.format(DATETIME);
    }
}
