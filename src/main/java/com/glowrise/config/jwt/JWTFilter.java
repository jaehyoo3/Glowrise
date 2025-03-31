package com.glowrise.config.jwt;

import com.glowrise.config.jwt.dto.CustomOAuthUser;
import com.glowrise.domain.User;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.UserDTO;
import io.jsonwebtoken.ExpiredJwtException;
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
import java.util.HashMap;

@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String accessToken = getToken(request); // 헤더와 쿠키 모두 확인
        String refreshToken = getCookieValue(request, "RefreshToken");
        String requestUri = request.getRequestURI();
        System.out.println("Request URI: " + requestUri + ", Access Token: " + (accessToken != null ? "present" : "missing") + ", Refresh Token: " + (refreshToken != null ? "present" : "missing"));

        if (requestUri.startsWith("/login") || requestUri.startsWith("/api/users/refresh") || requestUri.startsWith("/api/auth/") || requestUri.startsWith("/h2-console") || requestUri.startsWith("/api/users/signup")) {
            System.out.println("Skipping JWT filter for: " + requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        if (accessToken == null) {
            System.out.println("No access token provided");
            sendUnauthorized(response, "No access token");
            return;
        }

        try {
            if (jwtUtil.isExpired(accessToken)) {
                System.out.println("Access token expired");
                sendUnauthorized(response, "Token expired");
                return;
            }

            String username = jwtUtil.getUsername(accessToken);
            String role = jwtUtil.getRole(accessToken);
            Long userId = jwtUtil.getUserId(accessToken);

            UserDTO userDTO = new UserDTO();
            userDTO.setUserId(userId);
            userDTO.setUsername(username);
            userDTO.setRole(role);

            CustomOAuthUser user = new CustomOAuthUser(userDTO);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            auth.setDetails(new HashMap<String, Object>() {{
                put("userId", userId);
            }});
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            System.out.println("JWT parsing failed: " + e.getMessage());
            sendUnauthorized(response, "Token expired");
        }
    }

    private String getToken(HttpServletRequest request) {
        // 1. 헤더에서 확인
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        // 2. 쿠키에서 확인
        return getCookieValue(request, "Authorization");
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) return cookie.getValue();
            }
        }
        return null;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"" + message + "\"}");
    }
}