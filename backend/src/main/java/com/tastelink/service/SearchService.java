package com.tastelink.service;

import com.tastelink.common.PageResult;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.response.ShopVO;

/**
 * 店铺关键词检索（v2 Phase D）。
 * <p>
 * ES 失联/禁用时 {@code searchByKeyword} 返回 {@code null} 作降级信号，由调用方回退 MySQL {@code LIKE}，
 * 与 Phase A/C 吞缓存/MQ 异常 + 兜底同范式。结果 ShopVO 由 MySQL 回查组装，保证计数/分类名与库一致。
 */
public interface SearchService {

    /**
     * 关键词搜索店铺（ES name 字段匹配 + status/categoryId/city 过滤 + 分页）。
     *
     * @return 命中结果（按 ES 相关性序）；{@code null} 表示应降级回 MySQL 路径
     */
    PageResult<ShopVO> searchByKeyword(String keyword, Long categoryId, String city, PageQuery pq);
}
