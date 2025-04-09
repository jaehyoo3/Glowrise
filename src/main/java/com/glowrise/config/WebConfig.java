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

    // Logger 추가
    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Path 객체를 사용하여 OS 독립적인 경로 생성
        String resourcePath = Paths.get(uploadDir).toAbsolutePath().normalize().toString();
        // 중요: "file:" 접두사 및 경로 끝에 '/' 추가 확인!
        // Windows 경로 구분자 '\'가 있다면 '/'로 변경
        String resourceLocation = "file:" + resourcePath.replace("\\", "/") + "/";

        log.info("정적 리소스 핸들러 설정: URL 경로 /uploads/** -> 물리적 위치: {}", resourceLocation);

        registry.addResourceHandler("/uploads/**") // 클라이언트가 요청할 URL 패턴
                .addResourceLocations(resourceLocation); // 실제 파일이 있는 디스크 경로
    }
}