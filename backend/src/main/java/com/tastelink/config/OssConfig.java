package com.tastelink.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 OSS 客户端：仅 storage.type=oss 时创建。
 */
@Configuration
public class OssConfig {

    @Bean
    @ConditionalOnProperty(prefix = "tastelink.storage", name = "type", havingValue = "oss")
    public OSS ossClient(StorageProperties props) {
        var oss = props.getOss();
        return new OSSClientBuilder().build(oss.getEndpoint(), oss.getAccessKeyId(), oss.getAccessKeySecret());
    }
}
