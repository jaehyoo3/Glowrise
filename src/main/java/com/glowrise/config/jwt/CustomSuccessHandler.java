package com.glowrise.config.jwt;

import com.glowrise.config.jwt.dto.CustomOAuthUser;
import com.glowrise.domain.User;
import com.glowrise.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

@Component
@RequiredArgsConstructor
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomOAuthUser user = (CustomOAuthUser) authentication.getPrincipal();
        String username = user.getName();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();

        // 액세스 토큰과 리프레시 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(username, role, 60 * 60 * 1000L); // 1시간
        String refreshToken = jwtUtil.generateRefreshToken(username, 7 * 24 * 60 * 60 * 1000L); // 7일

        // User 엔티티에 토큰 저장
        User userEntity = userRepository.findByUsername(username);
        userEntity.setAccessToken(accessToken);
        userEntity.setRefreshToken(refreshToken);
        userRepository.save(userEntity);

        // 쿠키에 액세스 토큰 추가
        response.addCookie(createCookie("Authorization", accessToken));
        // 리프레시 토큰은 별도로 전달 (예: 응답 본문 또는 별도 쿠키)
        response.addCookie(createCookie("RefreshToken", refreshToken));

        // 로그인 성공 후 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, "/");
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(60 * 60 * 60); // 기본값 유지
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }
}