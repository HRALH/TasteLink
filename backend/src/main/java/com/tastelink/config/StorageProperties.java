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

    /** local | oss | cos */
    private String type = "local";

    private Local local = new Local();
    private Oss oss = new Oss();
    private Cos cos = new Cos();

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

    /**
     * 腾讯云 COS。默认访问域名 {bucket}.cos.{region}.myqcloud.com——从**同地域** CVM 访问会
     * 自动解析到内网 IP、走内网流量（不计流量费，仅计请求次数），所以无需配置任何端点。
     * {@code domain} 只在要换自定义域名 / CDN 时使用。
     */
    @Getter
    @Setter
    public static class Cos {
        /** 地域，如 ap-shanghai；务必与 CVM 同地域，否则流量与延迟都劣化 */
        private String region;
        /** 建议用子账号密钥，仅授权该 bucket 读写 */
        private String secretId;
        private String secretKey;
        private String bucket;
        /** 自定义访问域名（CDN / 自有域名）；为空则用 https://{bucket}.cos.{region}.myqcloud.com */
        private String domain;
    }
}
