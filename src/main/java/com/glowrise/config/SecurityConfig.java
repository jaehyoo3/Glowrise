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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@EnableWebSecurity
@EnableMethodSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomSuccessHandler customSuccessHandler;
    private final JWTFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                // 인증 없이 접근 가능한 엔드포인트
                                .requestMatchers(
                                        "/api/posts",           // 전체 게시글 조회
                                        "/api/posts/{postId}",  // 특정 게시글 조회
                                        "/api/posts/user/{userId}", // 사용자별 게시글 조회
                                        "/api/posts/blog/{blogId}", // 블로그별 게시글 조회
                                        "/api/posts/blog/{blogId}/{menuId}", // 블로그 메뉴별 게시글 조회
                                        "/api/blogs",           // 전체 블로그 조회
                                        "/api/blogs/{url}",     // URL로 블로그 조회
                                        "/api/blogs/id/{id}",   // ID로 블로그 조회
                                        "/api/blogs/user/{userId}", // 사용자별 블로그 조회
                                        "/api/menus/blog/{blogId}", // 블로그별 메뉴 조회
                                        "/api/menus/{menuId}/submenus", // 서브메뉴 조회
                                        "/api/comments/post/{postId}", // 게시글별 댓글 조회
                                        "/api/comments/{commentId}", // 특정 댓글 조회
                                        "/api/comments/{commentId}/replies", // 댓글의 답글 조회
                                        "/api/files/{fileId}",  // 파일 정보 조회
                                        "/api/files/download/{fileId}" // 파일 다운로드
                                ).permitAll()
                                // 회원가입, 로그인, 리프레시 토큰 등 인증 없이 접근 가능한 엔드포인트
                                .requestMatchers(
                                        "/api/users/signup",
                                        "/login",
                                        "/api/users/refresh",
                                        "/oauth2/authorization/**",
                                        "/login/oauth2/code/**",
                                        "/h2-console/**",
                                        "/ws/**"
                                ).permitAll()
                                // 나머지 모든 요청은 인증 필요
                                .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(customSuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            Map<String, String> error = new HashMap<>();
                            error.put("error", "로그인 실패");
                            error.put("message", exception.getMessage());
                            response.getWriter().write(new ObjectMapper().writeValueAsString(error));
                        })
                        .permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfoEndpointConfig ->
                                userInfoEndpointConfig.userService(customOAuth2UserService))
                        .successHandler(customSuccessHandler)
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/oauth2/authorization"))
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/login/oauth2/code/*")))
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityMatcher(AntPathRequestMatcher.antMatcher("/**"))
                .securityMatcher(request -> !request.getRequestURI().startsWith("/h2-console"))
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}