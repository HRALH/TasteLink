package com.tastelink.dto.request;

import com.tastelink.common.Constants;
import lombok.Data;

/**
 * 分页基类参数：page 从 1 起，size 上限 50。
 * 客户端可不传或传非法值，统一由 *OrDefault 归一化，容错优先于校验报错。
 */
@Data
public class PageQuery {

    private Integer page;
    private Integer size;

    public int getPageOrDefault() {
        return page == null || page < Constants.DEFAULT_PAGE ? Constants.DEFAULT_PAGE : page;
    }

    public int getSizeOrDefault() {
        if (size == null || size < 1) {
            return Constants.DEFAULT_SIZE;
        }
        return Math.min(size, Constants.MAX_SIZE);
    }
}
