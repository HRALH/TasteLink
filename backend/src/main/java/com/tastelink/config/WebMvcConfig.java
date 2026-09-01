package com.tastelink.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置：映射本地存储上传目录为静态资源（storage.type=local 时返回的图片 URL 直达此目录）。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${tastelink.storage.local.base-path:./data/uploads}")
    private String localBasePath;

    @Value("${tastelink.storage.local.url-prefix:/static/uploads}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absPath = new File(localBasePath).getAbsolutePath();
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations("file:" + absPath + File.separator);
    }
}
