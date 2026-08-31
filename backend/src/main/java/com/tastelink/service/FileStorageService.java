package com.tastelink.service;

import com.tastelink.dto.response.FileUploadVO;
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
}
