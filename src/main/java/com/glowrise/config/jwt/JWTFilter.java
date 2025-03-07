package com.glowrise.config.jwt;

import com.glowrise.config.jwt.dto.CustomOAuthUser;
import com.glowrise.domain.User;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.UserDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = null;
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("Authorization")) {
                    accessToken = cookie.getValue();
                }
                if (cookie.getName().equals("RefreshToken")) {
                    refreshToken = cookie.getValue();
                }
            }
        }

        String requestUri = request.getRequestURI();
        if (requestUri.startsWith("/api/auth/") || requestUri.startsWith("/login/") || requestUri.startsWith("/oauth2/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 액세스 토큰이 없거나 만료된 경우
        if (accessToken == null || jwtUtil.isExpired(accessToken)) {
            if (refreshToken != null && !jwtUtil.isExpired(refreshToken)) {
                // 리프레시 토큰이 유효하면 새로운 액세스 토큰 발급
                String username = jwtUtil.getUsername(refreshToken);
                User userEntity = userRepository.findByUsername(username).orElseThrow();
                if (userEntity.getRefreshToken().equals(refreshToken)) {
                    String role = userEntity.getRole().name();
                    accessToken = jwtUtil.generateAccessToken(username, role, 60 * 60 * 1000L);
                    userEntity.setAccessToken(accessToken);
                    userRepository.save(userEntity);

                    // 새로운 액세스 토큰을 쿠키에 설정
                    response.addCookie(createCookie("Authorization", accessToken));
                }
            } else {
                filterChain.doFilter(request, response);
                return;
            }
        }

        // 유효한 액세스 토큰으로 인증 설정
        String username = jwtUtil.getUsername(accessToken);
        String role = jwtUtil.getRole(accessToken);

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(username);
        userDTO.setRole(role);

        CustomOAuthUser user = new CustomOAuthUser(userDTO);
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(60 * 60 * 60);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }
}