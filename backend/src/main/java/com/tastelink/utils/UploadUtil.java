package com.tastelink.utils;

import com.tastelink.common.ResultCode;
import com.tastelink.exception.BusinessException;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * 上传校验工具：图片类型白名单、大小校验。
 */
public final class UploadUtil {

    private UploadUtil() {
    }

    public static final Set<String> ALLOWED_IMAGE_EXT =
            Set.of("jpg", "jpeg", "png", "webp", "gif");

    /** 取扩展名（小写，不含点）；无扩展名返回空串。 */
    public static String extOf(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /** 校验图片：非空 + 扩展名白名单，失败抛业务异常 400。 */
    public static void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件不能为空");
        }
        if (!ALLOWED_IMAGE_EXT.contains(extOf(file.getOriginalFilename()))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的图片类型");
        }
    }
}
