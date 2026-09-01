package com.tastelink.controller;

import com.tastelink.common.Constants;
import com.tastelink.common.R;
import com.tastelink.dto.response.FileUploadVO;
import com.tastelink.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传：图片经后端代理到 OSS 或本地存储，返回可访问 URL。
 */
@RestController
@RequestMapping(Constants.API_V1 + "/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/image")
    public R<FileUploadVO> uploadImage(@RequestParam("file") MultipartFile file) {
        return R.ok(fileStorageService.upload(file));
    }
}
