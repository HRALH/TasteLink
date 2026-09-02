package com.tastelink.service.impl;

import com.tastelink.common.PageResult;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.response.ShopVO;
import com.tastelink.entity.ShopDoc;
import com.tastelink.service.ShopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SearchServiceImpl 逻辑单测（Mockito，不依赖 Docker，随 {@code mvn test} 运行）。
 * 验证：禁用/空 keyword→null 降级；ES 异常→null 降级；命中→保 ES 序回填+PageResult 元数据。
 * 真 ES 查询行为见 {@link SearchServiceIT}。
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock
    private ElasticsearchOperations esOps;
    @Mock
    private ShopService shopService;

    private SearchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SearchServiceImpl(esOps, shopService);
        ReflectionTestUtils.setField(service, "enabled", true);
    }

    @Test
    void disabled_returnsNull() {
        ReflectionTestUtils.setField(service, "enabled", false);
        assertNull(service.searchByKeyword("hot", null, null, new PageQuery()));
        verifyNoInteractions(esOps, shopService);
    }

    @Test
    void blankKeyword_returnsNull() {
        assertNull(service.searchByKeyword("  ", null, null, new PageQuery()));
        verifyNoInteractions(esOps);
    }

    @Test
    void esFailure_returnsNullDegradeAndSkipsVOAssembly() {
        when(esOps.search(any(Query.class), eq(ShopDoc.class))).thenThrow(new RuntimeException("es down"));
        assertNull(service.searchByKeyword("hot", null, null, new PageQuery()));
        verifyNoInteractions(shopService);
    }

    @Test
    void success_returnsHitsInEsRankOrder() {
        @SuppressWarnings("unchecked")
        SearchHits<ShopDoc> hits = org.mockito.Mockito.mock(SearchHits.class);
        // 注意：hit() 内部会 stub；先单独完成它们，不要塞进外层 when(...) 的实参里（Mockito 会判「嵌套 stubbing」）
        SearchHit<ShopDoc> h1 = hit(doc(10L));
        SearchHit<ShopDoc> h2 = hit(doc(20L));
        when(hits.getTotalHits()).thenReturn(2L);
        when(hits.getSearchHits()).thenReturn(List.of(h1, h2));
        when(esOps.search(any(Query.class), eq(ShopDoc.class))).thenReturn(hits);
        when(shopService.toVOsByIds(List.of(10L, 20L)))
                .thenReturn(List.of(ShopVO.builder().id(10L).build(), ShopVO.builder().id(20L).build()));

        PageResult<ShopVO> r = service.searchByKeyword("hotpot", null, null, new PageQuery());

        assertNotNull(r);
        assertEquals(2L, r.getTotal());
        // 按 ES 相关性序回填（索引取 10→20 的顺序）
        verify(shopService).toVOsByIds(List.of(10L, 20L));
    }

    private static ShopDoc doc(long id) {
        ShopDoc d = new ShopDoc();
        d.setId(id);
        return d;
    }

    @SuppressWarnings("unchecked")
    private static SearchHit<ShopDoc> hit(ShopDoc d) {
        SearchHit<ShopDoc> h = org.mockito.Mockito.mock(SearchHit.class);
        when(h.getContent()).thenReturn(d);
        return h;
    }
}
