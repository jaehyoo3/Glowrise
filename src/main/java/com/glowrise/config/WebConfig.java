package com.glowrise.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;


@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String resourcePath = Paths.get(uploadDir).toAbsolutePath().normalize().toString();
        String resourceLocation = "file:" + resourcePath.replace("\\", "/") + "/";

        log.info("정적 리소스 핸들러 설정: URL 경로 /uploads/** -> 물리적 위치: {}", resourceLocation);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }
}