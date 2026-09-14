package com.tastelink.utils;

import com.tastelink.common.ResultCode;
import com.tastelink.exception.BusinessException;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * 上传校验工具：图片类型白名单、魔数嗅探、大小校验。
 */
public final class UploadUtil {

    private UploadUtil() {
    }

    public static final Set<String> ALLOWED_IMAGE_EXT =
            Set.of("jpg", "jpeg", "png", "webp", "gif");

    /** 魔数嗅探长度（覆盖 WebP 的 "RIFF....WEBP" 12 字节头） */
    private static final int SNIFF_LEN = 12;

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF_MAGIC = {0x47, 0x49, 0x46, 0x38};   // "GIF8"

    /** 取扩展名（小写，不含点）；无扩展名返回空串。 */
    public static String extOf(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * 校验图片：非空 + 扩展名白名单 + 文件头魔数（B1-4），失败抛业务异常 400。
     * 魔数校验防伪装内容（HTML/webshell 改名 .jpg）：local 模式下该文件经
     * {@code /static/uploads/**} 直出，属存储型 XSS 面。
     */
    public static void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件不能为空");
        }
        if (!ALLOWED_IMAGE_EXT.contains(extOf(file.getOriginalFilename()))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的图片类型");
        }
        byte[] head = new byte[SNIFF_LEN];
        int len;
        try (InputStream in = file.getInputStream()) {
            len = in.readNBytes(head, 0, SNIFF_LEN);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片内容读取失败");
        }
        if (!hasImageMagic(head, len)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片内容不是有效的图片文件");
        }
    }

    /** 文件头是否匹配 JPEG/PNG/GIF/WebP 魔数签名之一。 */
    private static boolean hasImageMagic(byte[] head, int len) {
        if (startsWith(head, len, JPEG_MAGIC) || startsWith(head, len, PNG_MAGIC)
                || startsWith(head, len, GIF_MAGIC)) {
            return true;
        }
        // WebP: "RIFF" + 4 字节长度 + "WEBP"
        return len >= SNIFF_LEN
                && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
    }

    private static boolean startsWith(byte[] head, int len, byte[] magic) {
        if (len < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (head[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }
}
