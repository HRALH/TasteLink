package com.tastelink.service.impl;

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地文件存储实现（storage.type=local 或缺省）。
 * 落地 ./data/uploads/yyyy/MM/uuid.ext，WebMvcConfig 映射为 /static/uploads/** 静态资源。
 */
@Service
@ConditionalOnProperty(prefix = "tastelink.storage", name = "type", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM");

    private final StorageProperties props;

    @Override
    public FileUploadVO upload(MultipartFile file) {
        UploadUtil.validateImage(file);
        String ext = UploadUtil.extOf(file.getOriginalFilename());
        String datePath = LocalDate.now().format(DATE_FMT);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String relative = datePath + "/" + fileName;

        try {
            Path dir = Paths.get(props.getLocal().getBasePath()).toAbsolutePath().resolve(datePath);
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "上传失败");
        }
        String url = props.getLocal().getPublicBaseUrl()
                + props.getLocal().getUrlPrefix() + "/" + relative;
        return new FileUploadVO(url, relative);
    }

    @Override
    public void delete(String ossKey) {
        if (!StringUtils.hasText(ossKey)) {
            return;
        }
        try {
            Path path = Paths.get(props.getLocal().getBasePath()).toAbsolutePath().resolve(ossKey);
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 删除失败不影响主流程
        }
    }

    @Override
    public String ossKeyFromUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        // upload 拼装形态：publicBaseUrl + urlPrefix + "/" + relative（relative=yyyy/MM/uuid.ext）
        String prefix = props.getLocal().getPublicBaseUrl() + props.getLocal().getUrlPrefix() + "/";
        return url.startsWith(prefix) ? url.substring(prefix.length()) : null;
    }
}
