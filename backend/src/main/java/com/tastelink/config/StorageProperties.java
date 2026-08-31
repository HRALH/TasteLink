package com.tastelink.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置属性（tastelink.storage.*）。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tastelink.storage")
public class StorageProperties {

    /** oss | local */
    private String type = "local";

    private Local local = new Local();
    private Oss oss = new Oss();

    @Getter
    @Setter
    public static class Local {
        private String basePath = "./data/uploads";
        private String urlPrefix = "/static/uploads";
        /** 访问本地图片的对外 BaseURL（前端 dev 跨域，需用后端绝对地址） */
        private String publicBaseUrl = "http://localhost:8080";
    }

    @Getter
    @Setter
    public static class Oss {
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucket;
        /** 自定义访问域名；为空则用 https://{bucket}.{endpoint} */
        private String domain;
    }
}
