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

@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = getCookieValue(request, "Authorization");
        String refreshToken = getCookieValue(request, "RefreshToken");
        String requestUri = request.getRequestURI();
        System.out.println("Request URI: " + requestUri + ", Access Token: " + (accessToken != null ? "present" : "missing") + ", Refresh Token: " + (refreshToken != null ? "present" : "missing"));

        // 인증이 필요 없는 경로 제외
        if (requestUri.startsWith("/login") || requestUri.startsWith("/api/users/refresh") || requestUri.startsWith("/api/auth/")) {
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
            User userEntity = userRepository.findByUsername(username).orElseThrow();

            UserDTO userDTO = new UserDTO();
            userDTO.setUserId(userEntity.getId());
            userDTO.setUsername(username);
            userDTO.setRole(role);

            CustomOAuthUser user = new CustomOAuthUser(userDTO);
            Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            System.out.println("JWT parsing failed: " + e.getMessage());
            sendUnauthorized(response, "Token expired");
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"" + message + "\"}");
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
}