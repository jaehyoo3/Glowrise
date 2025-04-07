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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(JWTFilter.class); // 로깅 추가

    private final JWTUtil jwtUtil;
    // UserRepository는 이 필터에서 직접 사용하지 않지만, 필요시 로깅 등에 활용 가능
    // private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String accessToken = getToken(request); // 헤더 또는 쿠키에서 토큰 추출

        // 액세스 토큰이 존재할 경우에만 인증 시도
        if (accessToken != null) {
            try {
                // 1. 토큰 만료 여부 우선 확인 (파싱 전에 확인하는 것이 효율적)
                if (!jwtUtil.isExpired(accessToken)) {

                    // 2. 토큰에서 사용자 정보 추출
                    String username = jwtUtil.getUsername(accessToken);
                    String role = jwtUtil.getRole(accessToken);
                    Long userId = jwtUtil.getUserId(accessToken);

                    // 3. UserDTO 및 CustomOAuthUser 생성 (인증 객체에 사용)
                    UserDTO userDTO = new UserDTO();
                    userDTO.setUserId(userId);
                    userDTO.setUsername(username);
                    userDTO.setRole(role);
                    CustomOAuthUser userPrincipal = new CustomOAuthUser(userDTO);

                    // 4. Spring Security 인증 토큰 생성
                    Authentication auth = new UsernamePasswordAuthenticationToken(
                            userPrincipal, null, userPrincipal.getAuthorities());

                    // 5. (선택적) 인증 객체에 추가 정보 설정 (예: userId)
                    // Authentication 객체의 details 필드를 활용
                    Map<String, Object> details = new HashMap<>();
                    details.put("userId", userId);
                    ((UsernamePasswordAuthenticationToken) auth).setDetails(details);


                    // 6. SecurityContextHolder에 인증 정보 설정
                    // 이 설정이 있어야 @PreAuthorize("isAuthenticated()") 등이 동작함
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    log.debug("[{}] 인증 성공: 사용자 '{}' (ID: {})", request.getRequestURI(), username, userId);

                } else {
                    // 토큰이 만료된 경우 (에러 응답은 보내지 않고 Security가 처리하도록 함)
                    log.debug("[{}] 액세스 토큰 만료됨", request.getRequestURI());
                    // 여기서 리프레시 토큰 로직을 추가할 수도 있지만, 보통은 별도의 /refresh 엔드포인트에서 처리
                }
            } catch (ExpiredJwtException e) {
                // isExpired() 에서 false가 나왔지만 파싱 중 만료된 경우 (거의 발생 안 함)
                log.debug("[{}] 액세스 토큰 만료됨 (파싱 중): {}", request.getRequestURI(), e.getMessage());
            } catch (Exception e) {
                // 기타 JWT 관련 예외 처리 (서명 오류, 잘못된 토큰 등)
                log.error("[{}] 유효하지 않은 JWT 토큰: {}", request.getRequestURI(), e.getMessage());
                // SecurityContext를 클리어하여 이전의 유효하지 않은 인증 정보가 남지 않도록 할 수 있음
                SecurityContextHolder.clearContext();
            }
        } else {
            // 토큰이 없는 경우
            log.debug("[{}] 액세스 토큰 없음", request.getRequestURI());
        }

        // 토큰 유무나 유효성 여부와 관계없이 항상 다음 필터로 체인 계속 진행
        // -> 최종 접근 허용/거부는 SecurityConfig 설정과 후속 필터(AuthorizationFilter 등)가 결정
        filterChain.doFilter(request, response);
    }

    /**
     * 요청 헤더(Authorization: Bearer) 또는 쿠키(Authorization)에서 토큰을 추출합니다.
     * 헤더를 우선적으로 확인합니다.
     *
     * @param request HttpServletRequest 객체
     * @return 추출된 JWT 토큰 문자열, 없으면 null
     */
    private String getToken(HttpServletRequest request) {
        // 1. 헤더에서 'Authorization: Bearer [토큰]' 형식 확인
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 부분을 제외한 실제 토큰 반환
        }

        // 2. 헤더에 없으면 쿠키에서 'Authorization' 이름의 쿠키 확인 (대안)
        //    최근에는 보안상의 이유로 HttpOnly 쿠키에 RefreshToken만 저장하고
        //    AccessToken은 클라이언트 메모리(JS 변수)에 저장하는 방식도 많이 사용됨
        //    여기서는 원본 코드의 로직을 유지
        return getCookieValue(request, "Authorization");
    }

    /**
     * 요청에서 지정된 이름의 쿠키 값을 찾아 반환합니다.
     *
     * @param request HttpServletRequest 객체
     * @param name    찾고자 하는 쿠키의 이름
     * @return 쿠키 값 문자열, 해당 이름의 쿠키가 없으면 null
     */
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

    // isPublicEndpoint 메소드는 제거됨
    // sendUnauthorized 메소드는 제거됨 (Spring Security가 처리)
}