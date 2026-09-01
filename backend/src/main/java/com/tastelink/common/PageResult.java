package com.tastelink.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 统一分页返回体：{ records, total, current, size, pages }。
 */
@Data
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long current;
    private long size;
    private long pages;

    public static <T> PageResult<T> of(List<T> records, long total, long current, long size) {
        PageResult<T> p = new PageResult<>();
        p.records = records == null ? Collections.emptyList() : records;
        p.total = total;
        p.current = current;
        p.size = size;
        p.pages = size <= 0 ? 0 : (total + size - 1) / size;
        return p;
    }

    /** MyBatis-Plus Page 查询实体，外部转成 VO 列表。 */
    public static <E, T> PageResult<T> from(Page<E> page, List<T> records) {
        return of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }
}
