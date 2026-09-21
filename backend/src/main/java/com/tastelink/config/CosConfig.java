package com.tastelink.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.region.Region;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 COS 客户端：仅 storage.type=cos 时创建（与 {@link OssConfig} 同构）。
 *
 * 不需要配置端点：SDK 按 region 推导出默认域名 {bucket}.cos.{region}.myqcloud.com，
 * 而同地域 CVM 访问该域名会自动解析到内网 IP、走内网流量（不计流量费，仅计请求次数）。
 * 跨地域想走内网才需要「全球内网加速」（cos-internal.accelerate.tencentcos.cn），那是收费功能，本项目不涉及。
 */
@Configuration
public class CosConfig {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "tastelink.storage", name = "type", havingValue = "cos")
    public COSClient cosClient(StorageProperties props) {
        var cos = props.getCos();
        var cred = new BasicCOSCredentials(cos.getSecretId(), cos.getSecretKey());
        return new COSClient(cred, new ClientConfig(new Region(cos.getRegion())));
    }
}
