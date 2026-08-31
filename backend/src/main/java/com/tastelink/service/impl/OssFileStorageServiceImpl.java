package com.tastelink.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.tastelink.common.ResultCode;
import com.tastelink.config.StorageProperties;
import com.tastelink.dto.response.FileUploadVO;
import com.tastelink.exception.BusinessException;
import com.tastelink.service.FileStorageService;
import com.tastelink.utils.UploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 阿里云 OSS 存储实现（storage.type=oss）。
 * URL 由 domain 或 https://{bucket}.{endpoint}/{objectKey} 拼装。
 */
@Service
@ConditionalOnProperty(prefix = "tastelink.storage", name = "type", havingValue = "oss")
@RequiredArgsConstructor
public class OssFileStorageServiceImpl implements FileStorageService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM");

    private final OSS ossClient;
    private final StorageProperties props;

    @Override
    public FileUploadVO upload(MultipartFile file) {
        UploadUtil.validateImage(file);
        String ext = UploadUtil.extOf(file.getOriginalFilename());
        String datePath = LocalDate.now().format(DATE_FMT);
        String objectKey = "uploads/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;

        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(file.getSize());
        meta.setContentType(file.getContentType());
        try (InputStream in = file.getInputStream()) {
            ossClient.putObject(props.getOss().getBucket(), objectKey, in, meta);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "上传失败");
        }
        String url = StringUtils.hasText(props.getOss().getDomain())
                ? props.getOss().getDomain() + "/" + objectKey
                : "https://" + props.getOss().getBucket() + "." + props.getOss().getEndpoint() + "/" + objectKey;
        return new FileUploadVO(url, objectKey);
    }

    @Override
    public void delete(String ossKey) {
        if (!StringUtils.hasText(ossKey)) {
            return;
        }
        try {
            ossClient.deleteObject(props.getOss().getBucket(), ossKey);
        } catch (Exception ignored) {
            // 删除失败不影响主流程
        }
    }
}
