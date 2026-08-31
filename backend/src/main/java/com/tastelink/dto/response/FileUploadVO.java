package com.tastelink.dto.response;

/**
 * 图片上传结果。DB 存 url（可访问）+ ossKey（管理用）。
 */
public record FileUploadVO(String url, String ossKey) {
}
