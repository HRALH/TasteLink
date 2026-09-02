package com.tastelink.service.impl;

import com.tastelink.common.Constants;
import com.tastelink.common.PageResult;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.response.ShopVO;
import com.tastelink.entity.ShopDoc;
import com.tastelink.service.SearchService;
import com.tastelink.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * ES 关键词检索实现（v2 Phase D）。
 * <p>
 * 关键词走 ES {@code name} 字段（MUST match），并 AND {@code status=NORMAL}/{@code categoryId}/{@code city}（均 MUST 过滤）。
 * <b>多字段（name/address/description）should + filter 存在 minimum_should_match 坑</b>，本版先用 name 单字段
 * Criteria（与 v1 {@code LIKE name} 等宽字段、严格升级为 relevance + IK-ready）；ShopDoc 已索引 name/address/description，
 * 后续以 NativeQuery multi_match 扩展即可。ES 任意异常吞掉返回 {@code null}（降级信号），不污染调用方。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchOperations esOps;
    private final ShopService shopService;

    @Value("${tastelink.search.enabled:true}")
    private boolean enabled;

    @Override
    public PageResult<ShopVO> searchByKeyword(String keyword, Long categoryId, String city, PageQuery pq) {
        if (!enabled || !StringUtils.hasText(keyword)) {
            return null;
        }
        try {
            Criteria c = new Criteria("name").matches(keyword)
                    .and(new Criteria("status").is(Constants.STATUS_NORMAL));
            if (categoryId != null) {
                c = c.and(new Criteria("categoryId").is(categoryId));
            }
            if (StringUtils.hasText(city)) {
                c = c.and(new Criteria("city").is(city));
            }
            // PageQuery 1-based → Spring Data Pageable 0-based
            Pageable pageable = PageRequest.of(pq.getPageOrDefault() - 1, pq.getSizeOrDefault());
            CriteriaQuery cq = new CriteriaQuery(c, pageable);
            SearchHits<ShopDoc> hits = esOps.search(cq, ShopDoc.class);
            List<Long> ids = hits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .map(ShopDoc::getId)
                    .toList();
            // 保 ES 相关性序，回 MySQL 组 ShopVO（计数/分类名与库一致），status=NORMAL 第二道过滤
            List<ShopVO> vos = shopService.toVOsByIds(ids);
            return PageResult.of(vos, hits.getTotalHits(), pq.getPageOrDefault(), pq.getSizeOrDefault());
        } catch (Exception e) {
            // ES 失联/索引未建/查询异常 → 降级信号，调用方回 MySQL LIKE
            log.warn("es searchByKeyword failed, fallback to MySQL: keyword={}, err={}",
                    keyword, e.getMessage());
            return null;
        }
    }
}
