package com.glowrise.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glowrise.config.jwt.CustomOAuth2UserService;
import com.glowrise.config.jwt.CustomSuccessHandler;
import com.glowrise.config.jwt.JWTFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomSuccessHandler customSuccessHandler;
    private final JWTFilter jwtFilter; // 주입받음

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // YAML에서 읽은 CORS 설정 적용
                .addFilterAfter(jwtFilter, UsernamePasswordAuthenticationFilter.class) // 주입받은 빈 사용
                .formLogin(form -> form
                        .loginProcessingUrl("/login") // 로그인 요청 URL
                        .usernameParameter("email")   // 이메일 필드 이름 설정
                        .passwordParameter("password") // 비밀번호 필드 이름 (기본값이지만 명시)
                        .successHandler(customSuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                            Map<String, String> error = new HashMap<>();
                            error.put("error", "로그인 실패");
                            error.put("message", exception.getMessage());
                            response.getWriter().write(new ObjectMapper().writeValueAsString(error));
                        })
                        .permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfoEndpointConfig ->
                                userInfoEndpointConfig.userService(customOAuth2UserService))
                        .successHandler(customSuccessHandler))
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/api/users/signup", "/login", "/api/users/refresh", "/h2-console/**").permitAll()
                                .anyRequest().authenticated())
                .headers(httpSecurityHeadersConfigurer ->
                        httpSecurityHeadersConfigurer.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000")); // Vue 프론트엔드 출처
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH")); // 허용 메서드
        config.setAllowedHeaders(List.of("*")); // 모든 헤더 허용
        config.setAllowCredentials(true); // 쿠키/인증 정보 허용
        config.setMaxAge(3600L); // 사전 요청 캐시 시간 (1시간)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // 모든 경로에 적용
        return source;
    }
}