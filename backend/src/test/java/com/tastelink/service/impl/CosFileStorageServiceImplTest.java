package com.tastelink.service.impl;

import com.qcloud.cos.COSClient;
import com.tastelink.config.StorageProperties;
import com.tastelink.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * CosFileStorageServiceImpl 单测：URL / objectKey 形态须与 OssFileStorageServiceImpl 同构
 * （切换存储时历史 URL 的解析与清理逻辑才不用改），以及 isOwnedUrl 白名单与 delete 的失败策略。
 * 客户端为 mock，不连真实 COS；真机联调与迁移见 docs/14 §8.4。
 */
class CosFileStorageServiceImplTest {

    private static final String BUCKET = "tastelink-1300000000";
    private static final String REGION = "ap-shanghai";
    private static final String DEFAULT_BASE = "https://" + BUCKET + ".cos." + REGION + ".myqcloud.com";

    private COSClient cosClient;
    private StorageProperties props;
    private CosFileStorageServiceImpl service;

    @BeforeEach
    void setUp() {
        cosClient = mock(COSClient.class);
        props = new StorageProperties();
        props.getCos().setRegion(REGION);
        props.getCos().setBucket(BUCKET);
        service = new CosFileStorageServiceImpl(cosClient, props);
    }

    // ---------- upload ----------

    @Test
    void upload_returnsUrlAndKeyInExpectedShape() {
        var vo = service.upload(jpeg("photo.JPG"));

        // uploads/yyyy/MM/<32位hex>.<ext>，扩展名归一化为小写
        assertTrue(vo.ossKey().matches("uploads/\\d{4}/\\d{2}/[0-9a-f]{32}\\.jpg"), vo.ossKey());
        assertEquals(DEFAULT_BASE + "/" + vo.ossKey(), vo.url());
        verify(cosClient).putObject(eq(BUCKET), eq(vo.ossKey()), any(), any());
    }

    @Test
    void upload_customDomainTrailingSlash_noDoubleSlash() {
        props.getCos().setDomain("https://img.example.com/");

        var vo = service.upload(jpeg("a.jpg"));

        assertTrue(vo.url().startsWith("https://img.example.com/uploads/"), vo.url());
    }

    @Test
    void upload_invalidImage_rejectedBeforeTouchingCos() {
        // 扩展名合法但文件头不是图片 → 魔数嗅探拦下，绝不能落到对象存储
        var fake = new MockMultipartFile("file", "shell.jpg", "image/jpeg",
                "<html>not an image</html>".getBytes(StandardCharsets.UTF_8));

        assertThrows(BusinessException.class, () -> service.upload(fake));
        verifyNoInteractions(cosClient);
    }

    @Test
    void upload_cosFailure_throwsBusinessException() {
        doThrow(new RuntimeException("403 AccessDenied")).when(cosClient)
                .putObject(any(), any(), any(), any());

        assertThrows(BusinessException.class, () -> service.upload(jpeg("a.jpg")));
    }

    // ---------- ossKeyFromUrl / isOwnedUrl ----------

    @Test
    void ossKeyFromUrl_defaultDomain_roundTrip() {
        String key = "uploads/2026/09/abc.jpg";
        assertEquals(key, service.ossKeyFromUrl(DEFAULT_BASE + "/" + key));
    }

    @Test
    void ossKeyFromUrl_customDomainWithTrailingSlash() {
        props.getCos().setDomain("https://img.example.com/");
        assertEquals("uploads/2026/09/abc.jpg",
                service.ossKeyFromUrl("https://img.example.com/uploads/2026/09/abc.jpg"));
    }

    @Test
    void isOwnedUrl_whitelist() {
        assertTrue(service.isOwnedUrl(DEFAULT_BASE + "/uploads/2026/09/a.jpg"));      // 本实现签发
        assertFalse(service.isOwnedUrl(DEFAULT_BASE + "/uploads/../../etc/passwd"));   // 前缀对但路径穿越
        assertFalse(service.isOwnedUrl("https://evil.example.com/uploads/a.jpg"));     // 外链
        assertFalse(service.isOwnedUrl(""));
        assertFalse(service.isOwnedUrl(null));
    }

    // ---------- delete ----------

    @Test
    void delete_delegatesToCos() {
        service.delete("uploads/2026/09/a.jpg");
        verify(cosClient).deleteObject(BUCKET, "uploads/2026/09/a.jpg");
    }

    @Test
    void delete_blankKey_isNoOp() {
        service.delete("");
        service.delete(null);
        verifyNoInteractions(cosClient);
    }

    @Test
    void delete_failureIsSwallowed() {
        doThrow(new RuntimeException("network")).when(cosClient).deleteObject(any(), any());

        // 不抛：存储故障不能阻断删店主流程（对账调度会重清）
        service.delete("uploads/2026/09/a.jpg");

        verify(cosClient).deleteObject(any(), any());
    }

    /** 文件头带 JPEG 魔数的合法图片（UploadUtil 会嗅探文件头，只改扩展名不够）。 */
    private static MockMultipartFile jpeg(String filename) {
        byte[] content = new byte[64];
        System.arraycopy(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, 0, content, 0, 4);
        return new MockMultipartFile("file", filename, "image/jpeg", content);
    }
}
