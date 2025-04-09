package com.glowrise.config;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HtmlSanitizerConfig {

    @Bean
    public PolicyFactory htmlPolicyFactory() {
        // 필요한 HTML 태그와 속성을 허용하는 정책 정의
        // 예시: 기본 포맷팅, 링크, 이미지, 기본 블록 태그 허용
        return Sanitizers.FORMATTING
                .and(Sanitizers.LINKS)       // <a> 태그 (href 속성 포함, rel="nofollow" 자동 추가될 수 있음)
                .and(Sanitizers.IMAGES)      // <img> 태그 (src, alt, width, height 등 허용)
                .and(Sanitizers.BLOCKS)      // <p>, <div>, <h1>-<h6>, <blockquote>, <ul>, <ol>, <li> 등
                .and(Sanitizers.TABLES)   // <table>, <tr>, <td>, <th> 등 필요시 추가
                .and(Sanitizers.STYLES)   // 인라인 style 속성 필요시 추가 (주의: 허용할 스타일 속성 제한 필요)
                .and(new HtmlPolicyBuilder() // 필요하다면 특정 태그나 속성 커스텀 추가/제거
                        .allowAttributes("src", "width", "height", "frameborder").onElements("iframe")
                        .toFactory())
                ;
    }
}