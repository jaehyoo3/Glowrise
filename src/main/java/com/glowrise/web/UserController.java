package com.glowrise.web;


import com.glowrise.service.UserService;
import com.glowrise.service.dto.UserDTO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<UserDTO> signUp(@RequestBody UserDTO dto) {
        // 서비스 호출 (예외는 ControllerAdvice 처리 가정)
        UserDTO createdUser = userService.signUp(dto);
        log.info("회원가입 성공: {}", createdUser.getUsername());
        // 201 Created 상태 코드와 함께 생성된 사용자 정보 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    /**
     * 특정 사용자 프로필 조회 (공개 프로필 등)
     */
    @GetMapping("/profile/{username}")
    public ResponseEntity<UserDTO> getUserProfile(@PathVariable String username) {
        // 서비스 호출 (예외는 ControllerAdvice 처리 가정)
        UserDTO userProfile = userService.getUserProfile(username);
        return ResponseEntity.ok(userProfile);
    }

    /**
     * 현재 로그인된 사용자 정보 조회 (본인 정보 + 블로그 정보)
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        // 서비스 호출
        UserDTO userDTO = userService.getCurrentUserWithBlogInfo(authentication);
        return ResponseEntity.ok(userDTO);
    }


    /**
     * 현재 로그인된 사용자 프로필 수정 (닉네임, 비밀번호 등)
     *
     * @param dto            수정할 정보 DTO
     * @param authentication 현재 인증 정보
     */
    @PutMapping("/profile/me") // 엔드포인트 경로 변경됨
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> updateUserProfile(
            @RequestBody UserDTO dto,
            Authentication authentication) { // PathVariable 제거됨

        // 서비스 호출 (서비스 내부에서 authentication 통해 사용자 식별 및 업데이트)
        UserDTO updatedUser = userService.updateUserProfile(authentication, dto);
        log.info("현재 사용자 프로필 업데이트 성공: {}", authentication.getName());
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * 현재 로그인된 사용자 닉네임 수정
     */
    @PutMapping("/me/nickname")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateUserNickname(@RequestBody UserDTO request, Authentication authentication) {
        // 서비스 호출 (예외는 ControllerAdvice 처리 가정)
        userService.updateUserNickname(request.getNickName(), authentication);
        log.info("사용자 닉네임 업데이트 성공: {}", authentication.getName());
        // 성공 시 200 OK 와 함께 본문 없이 반환
        return ResponseEntity.ok().build();
    }


    /**
     * 토큰 갱신
     *
     * @param request  HttpServletRequest (쿠키 접근용)
     * @param response HttpServletResponse (선택적: 쿠키 설정용)
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // 쿠키에서 Refresh Token 추출
        String refreshToken = getCookieValue(request, "RefreshToken");
        // 또는 헤더에서 추출: request.getHeader("X-Refresh-Token");

        // 서비스 호출 (예외는 ControllerAdvice 처리 가정)
        Map<String, String> tokens = userService.refreshToken(refreshToken);

        log.info("토큰 갱신 요청 처리 완료");
        // 새 Access Token (및 필요시 Refresh Token) 반환
        return ResponseEntity.ok(tokens);
    }


    /**
     * HttpServletRequest에서 특정 이름의 쿠키 값을 찾는 헬퍼 메서드
     * @param request 요청 객체
     * @param name 찾을 쿠키 이름
     * @return 쿠키 값 (없으면 null)
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
        log.debug("쿠키 '{}'를 찾을 수 없습니다.", name);
        return null;
    }



}