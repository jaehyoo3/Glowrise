package com.glowrise.config.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glowrise.config.jwt.dto.CustomOAuthUser;
import com.glowrise.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        String username;
        String role;

        if (authentication.getPrincipal() instanceof CustomOAuthUser user) {
            username = user.getName();
            role = authentication.getAuthorities().iterator().next().getAuthority();
        } else if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
            role = authentication.getAuthorities().iterator().next().getAuthority();
        } else {
            throw new IllegalStateException("지원 되지 않는 인증 유형입니다");
        }

        String accessToken = jwtUtil.generateAccessToken(username, role, 60 * 60 * 1000L);
        String refreshToken = jwtUtil.generateRefreshToken(username, 7 * 24 * 60 * 60 * 1000L);

        com.glowrise.domain.User userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다: " + username));
        userEntity.setAccessToken(accessToken);
        userEntity.setRefreshToken(refreshToken);
        userRepository.save(userEntity);

        response.addCookie(createCookie("Authorization", accessToken));
        response.addCookie(createCookie("RefreshToken", refreshToken));

        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("accessToken", accessToken);
        responseBody.put("refreshToken", refreshToken);
        responseBody.put("username", username);
        responseBody.put("message", "로그인 성공");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(new ObjectMapper().writeValueAsString(responseBody));
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(60 * 60 * 60);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }
}