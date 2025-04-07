package com.glowrise.web;


import com.glowrise.service.UserService;
import com.glowrise.service.dto.UserDTO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<UserDTO> signUp(@RequestBody UserDTO dto) throws Exception {
        UserDTO createdUser = userService.signUp(dto);
        return ResponseEntity.ok(createdUser);
    }

    @GetMapping("/profile/{username}")
    public ResponseEntity<UserDTO> getUserProfile(@PathVariable String username) throws Exception {
        UserDTO userProfile = userService.getUserProfile(username);
        return ResponseEntity.ok(userProfile);
    }

    @PutMapping("/profile/{username}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> updateUserProfile(
            @PathVariable String username,
            @RequestBody UserDTO dto,
            Authentication authentication) throws Exception {
        UserDTO updatedUser = userService.updateUserProfile(username, dto);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> partialUpdateUser(
            @PathVariable Long id,
            @RequestBody UserDTO dto,
            Authentication authentication) throws Exception {
        dto.setId(id);
        Optional<UserDTO> updatedUser = userService.partialUpdateUser(dto);
        return updatedUser.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getCookieValue(request, "RefreshToken");
        try {
            Map<String, String> result = userService.refreshToken(refreshToken, response);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\": \"Unauthorized\", \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    @PutMapping("/me/nickname")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateUserNickname(@RequestBody UserDTO request, Authentication authentication) {
        // 서비스 호출 (유효성 검증 및 로직 처리 위임)
        // 서비스에서 예외 발생 시 @ControllerAdvice 에서 처리됨
        userService.updateUserNickname(request.getNickName(), authentication);
        // 서비스 호출이 성공적으로 완료되면 OK 응답 반환
        return ResponseEntity.ok().build();
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

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        UserDTO userDTO = userService.getCurrentUser2(authentication);
        return ResponseEntity.ok(userDTO);
    }
}