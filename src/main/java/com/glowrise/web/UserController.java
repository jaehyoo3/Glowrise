package com.glowrise.web;

import com.glowrise.config.jwt.JWTUtil;
import com.glowrise.config.jwt.dto.CustomOAuthUser;
import com.glowrise.service.UserService;
import com.glowrise.service.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
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

    // 리프레시 토큰을 사용한 토큰 갱신
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestBody Map<String, String> request) throws Exception {
        String refreshToken = request.get("refreshToken");
        Map<String, String> tokens = userService.refreshToken(refreshToken);
        return ResponseEntity.ok(tokens);
    }
}
