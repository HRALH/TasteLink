package com.tastelink.service;

import com.tastelink.dto.response.FileUploadVO;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储抽象：业务层只依赖此接口，不拼 URL。
 * 由 storage.type 配置决定注入 OSS 或本地实现。
 */
public interface FileStorageService {

    /**
     * 上传图片。
     *
     * @param file 前端上传的图片
     * @return 可访问 URL 与存储对象 key
     */
    FileUploadVO upload(MultipartFile file);

    /** 按存储 key 删除对象（删除点评/资源时清理存储）。 */
    void delete(String ossKey);

    /**
     * 由可访问 URL 反推删除用的存储对象 key（v2 Phase C 删店清理图片用）。
     * 各实现只反转自己 upload 时拼装的 URL 形态；URL 不匹配（外链/历史脏数据）返回 null，
     * 调用方据此跳过。B1-1 起 t_review_image.oss_key 真实入库，此反推仅兜底历史空串行。
     *
     * @param url upload 返回的可访问 URL
     * @return 存储对象 key，或 null 表示不可解析
     */
    String ossKeyFromUrl(String url);

    /**
     * 校验 url 是否为本存储实现签发（upload 拼装形态）且存储 key 形态安全（B1-1）。
     * 用于点评图片等用户可提交字段的入库前校验：防路径穿越（前缀匹配但 key 含 ".."）
     * 与外链注入（他人域名 URL 入库后污染清理/展示路径）。
     */
    default boolean isOwnedUrl(String url) {
        return isSafeKey(ossKeyFromUrl(url));
    }

    /** 存储 key 形态安全：非空、非绝对/反斜杠路径、不含 ".." 段。 */
    static boolean isSafeKey(String key) {
        if (!StringUtils.hasText(key) || key.startsWith("/") || key.contains("\\")) {
            return false;
        }
        for (String seg : key.split("/")) {
            if ("..".equals(seg)) {
                return false;
            }
        }
        return true;
    }
}
