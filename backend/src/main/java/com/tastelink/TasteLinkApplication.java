package com.tastelink;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TasteLink 后端启动类。
 * 统一扫描 Mapper 接口包；其余组件默认扫描 com.tastelink 下。
 */
@SpringBootApplication
@MapperScan("com.tastelink.mapper")
public class TasteLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(TasteLinkApplication.class, args);
    }
}
