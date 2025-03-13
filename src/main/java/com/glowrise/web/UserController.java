package com.glowrise.web;


import com.glowrise.service.UserService;
import com.glowrise.service.dto.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
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

    // 사용자 정보 조회
    @GetMapping("/profile/{username}")
    public ResponseEntity<UserDTO> getUserProfile(@PathVariable String username) throws Exception {
        UserDTO userProfile = userService.getUserProfile(username);
        return ResponseEntity.ok(userProfile);
    }

    // 사용자 정보 수정
    @PutMapping("/profile/{username}")
    public ResponseEntity<UserDTO> updateUserProfile(
            @PathVariable String username,
            @RequestBody UserDTO dto) throws Exception {
        UserDTO updatedUser = userService.updateUserProfile(username, dto);
        return ResponseEntity.ok(updatedUser);
    }

    // 부분 업데이트
    @PatchMapping("/{id}")
    public ResponseEntity<UserDTO> partialUpdateUser(@PathVariable Long id, @RequestBody UserDTO dto) throws Exception {
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
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        try {
            Map<String, Object> userInfo = userService.getCurrentUser(authentication);
            return ResponseEntity.ok(userInfo);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }
}
