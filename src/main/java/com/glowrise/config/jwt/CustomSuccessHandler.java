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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
@RequiredArgsConstructor
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String username;
        String role;

        // OAuth2 로그인 처리
        if (authentication.getPrincipal() instanceof CustomOAuthUser) {
            CustomOAuthUser user = (CustomOAuthUser) authentication.getPrincipal();
            username = user.getName();
            role = authentication.getAuthorities().iterator().next().getAuthority();
        }
        // 일반 로그인 처리
        else if (authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            username = userDetails.getUsername();
            role = authentication.getAuthorities().iterator().next().getAuthority();
        } else {
            throw new IllegalStateException("지원되지 않는 인증 유형입니다");
        }

        // 액세스 토큰과 리프레시 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(username, role, 60 * 60 * 1000L); // 1시간
        String refreshToken = jwtUtil.generateRefreshToken(username, 7 * 24 * 60 * 60 * 1000L); // 7일

        // User 엔티티에 토큰 저장
        User userEntity = userRepository.findByUsername(username);
        if (userEntity != null) {
            userEntity.setAccessToken(accessToken);
            userEntity.setRefreshToken(refreshToken);
            userRepository.save(userEntity);
        }

        // 쿠키에 토큰 추가
        response.addCookie(createCookie("Authorization", accessToken));
        response.addCookie(createCookie("RefreshToken", refreshToken));

        // 리다이렉트 (필요 시 JSON 응답으로 변경 가능)
        getRedirectStrategy().sendRedirect(request, response, "/");
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(60 * 60 * 60);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }
}