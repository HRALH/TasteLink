package com.tastelink.service.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.tastelink.common.ResultCode;
import com.tastelink.config.StorageProperties;
import com.tastelink.dto.response.FileUploadVO;
import com.tastelink.exception.BusinessException;
import com.tastelink.service.FileStorageService;
import com.tastelink.utils.UploadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 腾讯云 COS 存储实现（storage.type=cos）。
 * URL = {domain} 或 https://{bucket}.cos.{region}.myqcloud.com，后接 objectKey；
 * 与 OssFileStorageServiceImpl 保持同一形态（uploads/yyyy/MM/uuid.ext + 可反推），
 * 以便两种远端存储之间切换时 URL 解析与历史数据清理逻辑不变。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "tastelink.storage", name = "type", havingValue = "cos")
@RequiredArgsConstructor
public class CosFileStorageServiceImpl implements FileStorageService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM");

    private final COSClient cosClient;
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
            cosClient.putObject(props.getCos().getBucket(), objectKey, in, meta);
        } catch (Exception e) {
            log.warn("cos put failed: key={}, err={}", objectKey, e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "上传失败");
        }
        return new FileUploadVO(publicBase() + "/" + objectKey, objectKey);
    }

    @Override
    public void delete(String ossKey) {
        if (!StringUtils.hasText(ossKey)) {
            return;
        }
        try {
            cosClient.deleteObject(props.getCos().getBucket(), ossKey);
        } catch (Exception e) {
            // 删除失败不影响主流程（删店对账会重清），但需留痕
            log.warn("cos delete failed: key={}, err={}", ossKey, e.getMessage());
        }
    }

    @Override
    public String ossKeyFromUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String prefix = publicBase() + "/";
        return url.startsWith(prefix) ? url.substring(prefix.length()) : null;
    }

    /** upload 拼装用的公网前缀；domain 末尾多余的 / 会被容忍（与 OSS 实现一致）。 */
    private String publicBase() {
        var cos = props.getCos();
        String domain = cos.getDomain();
        if (StringUtils.hasText(domain)) {
            return domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
        }
        return "https://" + cos.getBucket() + ".cos." + cos.getRegion() + ".myqcloud.com";
    }
}
