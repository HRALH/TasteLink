package com.tastelink.service.impl;

import com.tastelink.common.Constants;
import com.tastelink.common.ResultCode;
import com.tastelink.dto.request.UpdateShopRequest;
import com.tastelink.dto.response.ShopDetailVO;
import com.tastelink.entity.Shop;
import com.tastelink.entity.ShopCategory;
import com.tastelink.exception.BusinessException;
import com.tastelink.mapper.ReviewMapper;
import com.tastelink.mapper.ShopCategoryMapper;
import com.tastelink.mapper.ShopMapper;
import com.tastelink.mapper.UserMapper;
import com.tastelink.service.ShopCleanupProducer;
import com.tastelink.service.ShopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * AdminShopServiceImpl 逻辑单测（Mockito，不依赖 Docker，随 {@code mvn test} 运行）。
 * 验证字段覆盖、分类校验、乐观锁冲突→409 映射、店铺不存在→404（Phase B 改店），
 * 以及删店标记事务—级联隐藏+回扣计数+afterCommit 投递（Phase C，真 MQ 行为见 ShopCleanupIT）。
 */
@ExtendWith(MockitoExtension.class)
class AdminShopServiceImplTest {

    @Mock
    private ShopMapper shopMapper;
    @Mock
    private ShopCategoryMapper categoryMapper;
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ShopService shopService;
    @Mock
    private ShopCleanupProducer shopCleanupProducer;

    private AdminShopServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminShopServiceImpl(shopMapper, categoryMapper, reviewMapper, userMapper,
                shopService, shopCleanupProducer);
    }

    private Shop shop(long id) {
        Shop s = new Shop();
        s.setId(id);
        s.setName("原名");
        s.setCity("上海");
        s.setVersion(0);
        s.setStatus(Constants.STATUS_NORMAL);
        return s;
    }

    @Test
    void updateShop_success_copiesProvidedFieldsAndDelegatesResponse() {
        Shop s = shop(1L);
        when(shopMapper.selectById(1L)).thenReturn(s);
        when(shopMapper.updateById(any(Shop.class))).thenReturn(1);
        ShopDetailVO vo = new ShopDetailVO();
        vo.setId(1L);
        when(shopService.getShopDetail(1L)).thenReturn(vo);

        UpdateShopRequest req = new UpdateShopRequest();
        req.setName("新名");
        req.setCity("杭州");
        req.setPhone("123");

        ShopDetailVO result = service.updateShop(1L, req);

        assertEquals(vo, result);
        assertEquals("新名", s.getName());
        assertEquals("杭州", s.getCity());
        assertEquals("123", s.getPhone());
        verify(shopMapper).updateById(s);   // 带载入 version 的实体写入
        verify(shopService).getShopDetail(1L);
    }

    @Test
    void updateShop_versionConflict_throws409() {
        Shop s = shop(1L);
        when(shopMapper.selectById(1L)).thenReturn(s);
        // 并发冲突：updateById 影响行数 0（MyBatis-Plus 不抛异常，仅返回 0）
        when(shopMapper.updateById(any(Shop.class))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateShop(1L, new UpdateShopRequest()));
        assertEquals(ResultCode.SHOP_VERSION_CONFLICT.getCode(), ex.getCode());
        assertEquals(ResultCode.SHOP_VERSION_CONFLICT.getHttpStatus(), ex.getHttpStatus());
        verify(shopService, never()).getShopDetail(any());
    }

    @Test
    void updateShop_shopNotFound_throws404() {
        when(shopMapper.selectById(9L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateShop(9L, new UpdateShopRequest()));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        verify(shopMapper, never()).updateById(any(Shop.class));
    }

    @Test
    void updateShop_unknownCategory_throws400() {
        when(shopMapper.selectById(1L)).thenReturn(shop(1L));
        when(categoryMapper.selectById(777L)).thenReturn(null);

        UpdateShopRequest req = new UpdateShopRequest();
        req.setCategoryId(777L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateShop(1L, req));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(shopMapper, never()).updateById(any(Shop.class));
    }

    @Test
    void updateShop_knownCategory_setsIt() {
        Shop s = shop(1L);
        when(shopMapper.selectById(1L)).thenReturn(s);
        when(categoryMapper.selectById(5L)).thenReturn(new ShopCategory());
        when(shopMapper.updateById(any(Shop.class))).thenReturn(1);
        ShopDetailVO vo = new ShopDetailVO();
        when(shopService.getShopDetail(eq(1L))).thenReturn(vo);

        UpdateShopRequest req = new UpdateShopRequest();
        req.setCategoryId(5L);

        service.updateShop(1L, req);
        assertEquals(5L, s.getCategoryId());
        verify(shopMapper).updateById(s);
    }

    // ---------- Phase C 删店标记事务 ----------
    // 标记事务中的级联隐藏点评走 LambdaUpdateWrapper.set，依赖 MyBatis-Plus 的 TableInfo 缓存，
    // 纯 Mockito 无 Spring 上下文时该方法会抛 lambda-cache 错（与 InteractionServiceImpl 一样，
    // 仓库内该类 lambda-update 服务本就不做 Mockito 覆盖）——故 success 级联+回扣留给
    // ShopCleanupIT 真 DB 上下文覆盖。下列用例只触 lambda-free 路径，可在 mvn test 跑。

    @Test
    void deleteShop_noReviews_marksHideSkipsCascadeAndPublishes() {
        Shop s = shop(1L);
        when(shopMapper.selectById(1L)).thenReturn(s);
        when(shopMapper.updateById(any(Shop.class))).thenReturn(1);
        when(reviewMapper.selectList(any())).thenReturn(List.of());

        service.deleteShop(1L);

        verify(reviewMapper, never()).update(any(), any());
        verify(userMapper, never()).update(any(), any());
        verify(shopCleanupProducer).send(1L);
    }

    @Test
    void deleteShop_alreadyHidden_throws404() {
        Shop s = shop(1L);
        s.setStatus(Constants.STATUS_HIDDEN);
        when(shopMapper.selectById(1L)).thenReturn(s);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deleteShop(1L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        verify(shopMapper, never()).updateById(any(Shop.class));
        verifyNoInteractions(shopCleanupProducer);
    }

    @Test
    void deleteShop_notFound_throws404() {
        when(shopMapper.selectById(9L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.deleteShop(9L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        verify(shopMapper, never()).updateById(any(Shop.class));
        verifyNoInteractions(shopCleanupProducer);
    }

    @Test
    void deleteShop_versionConflict_throws409AndDoesNotPublish() {
        Shop s = shop(1L);
        when(shopMapper.selectById(1L)).thenReturn(s);
        when(shopMapper.updateById(any(Shop.class))).thenReturn(0);   // 并发改/删冲突

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deleteShop(1L));
        assertEquals(ResultCode.SHOP_VERSION_CONFLICT.getCode(), ex.getCode());
        verify(reviewMapper, never()).update(any(), any());
        verifyNoInteractions(shopCleanupProducer);
    }
}
