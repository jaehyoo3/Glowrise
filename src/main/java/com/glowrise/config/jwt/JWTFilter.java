package com.glowrise.config.jwt;

import com.glowrise.config.jwt.dto.CustomOAuthUser;
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
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String accessToken = getToken(request);

        if (accessToken != null) {
            try {
                if (!jwtUtil.isExpired(accessToken)) {

                    String username = jwtUtil.getUsername(accessToken);
                    String role = jwtUtil.getRole(accessToken);
                    Long userId = jwtUtil.getUserId(accessToken);

                    UserDTO userDTO = new UserDTO();
                    userDTO.setUserId(userId);
                    userDTO.setUsername(username);
                    userDTO.setRole(role);
                    CustomOAuthUser userPrincipal = new CustomOAuthUser(userDTO);

                    Authentication auth = new UsernamePasswordAuthenticationToken(
                            userPrincipal, null, userPrincipal.getAuthorities());

                    Map<String, Object> details = new HashMap<>();
                    details.put("userId", userId);
                    ((UsernamePasswordAuthenticationToken) auth).setDetails(details);

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (ExpiredJwtException e) {
                // Expired token exception handling (if needed beyond SecurityContext clearing)
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return getCookieValue(request, "Authorization");
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}