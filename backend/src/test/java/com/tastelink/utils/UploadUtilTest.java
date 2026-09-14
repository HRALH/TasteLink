package com.tastelink.utils;

import com.tastelink.common.ResultCode;
import com.tastelink.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * UploadUtil 魔数校验单测（B1-4）：真图文件头通过；文本/HTML 改名 .jpg 拒绝。
 */
class UploadUtilTest {

    private static MockMultipartFile file(String filename, byte[] content) {
        return new MockMultipartFile("file", filename, "image/jpeg", content);
    }

    @Test
    void validateImage_jpegMagic_passes() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46};
        assertDoesNotThrow(() -> UploadUtil.validateImage(file("a.jpg", jpeg)));
    }

    @Test
    void validateImage_pngMagic_passes() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
        assertDoesNotThrow(() -> UploadUtil.validateImage(file("a.png", png)));
    }

    @Test
    void validateImage_gifMagic_passes() {
        byte[] gif = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00};
        assertDoesNotThrow(() -> UploadUtil.validateImage(file("a.gif", gif)));
    }

    @Test
    void validateImage_webpMagic_passes() {
        byte[] webp = {0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50};
        assertDoesNotThrow(() -> UploadUtil.validateImage(file("a.webp", webp)));
    }

    @Test
    void validateImage_textRenamedAsJpg_rejected() {
        // 伪装内容：HTML/webshell 改名 .jpg（local 模式经 /static/uploads/** 直出 = 存储型 XSS 面）
        byte[] html = "<html><script>alert(1)</script>".getBytes();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> UploadUtil.validateImage(file("evil.jpg", html)));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    void validateImage_truncatedHeader_rejected() {
        byte[] two = {(byte) 0xFF, (byte) 0xD8};   // 不足 JPEG 3 字节签名
        assertThrows(BusinessException.class, () -> UploadUtil.validateImage(file("a.jpg", two)));
    }

    @Test
    void validateImage_badExtension_rejected() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertThrows(BusinessException.class, () -> UploadUtil.validateImage(file("a.txt", png)));
    }

    @Test
    void validateImage_emptyFile_rejected() {
        assertThrows(BusinessException.class, () -> UploadUtil.validateImage(file("a.jpg", new byte[0])));
    }
}
