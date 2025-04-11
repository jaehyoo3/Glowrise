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
        return Sanitizers.FORMATTING
                .and(Sanitizers.LINKS)
                .and(Sanitizers.IMAGES)
                .and(Sanitizers.BLOCKS)
                .and(Sanitizers.TABLES)
                .and(Sanitizers.STYLES)
                .and(new HtmlPolicyBuilder()
                        .allowAttributes("src", "width", "height", "frameborder").onElements("iframe")
                        .toFactory())
                ;
    }
}