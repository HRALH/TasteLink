package com.tastelink.service.impl;

import com.tastelink.common.ResultCode;
import com.tastelink.dto.request.UpdateShopRequest;
import com.tastelink.dto.response.ShopDetailVO;
import com.tastelink.entity.Shop;
import com.tastelink.entity.ShopCategory;
import com.tastelink.exception.BusinessException;
import com.tastelink.mapper.ShopCategoryMapper;
import com.tastelink.mapper.ShopMapper;
import com.tastelink.service.ShopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AdminShopServiceImpl 逻辑单测（Mockito，不依赖 Docker，随 {@code mvn test} 运行）。
 * 验证字段覆盖、分类校验、乐观锁冲突→409 映射、店铺不存在→404。
 * 真 DB 乐观锁行为见 {@link AdminShopServiceIT}。
 */
@ExtendWith(MockitoExtension.class)
class AdminShopServiceImplTest {

    @Mock
    private ShopMapper shopMapper;
    @Mock
    private ShopCategoryMapper categoryMapper;
    @Mock
    private ShopService shopService;

    private AdminShopServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminShopServiceImpl(shopMapper, categoryMapper, shopService);
    }

    private Shop shop(long id) {
        Shop s = new Shop();
        s.setId(id);
        s.setName("原名");
        s.setCity("上海");
        s.setVersion(0);
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
}
