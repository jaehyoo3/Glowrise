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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JWTUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody UserDTO dto) {
        try {
            userService.signUp(dto);
            return ResponseEntity.ok("회원가입 성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("회원가입 실패: " + e.getMessage());
        }
    }


    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserDTO dto) {
        String token = userService.login(dto);
        ResponseCookie cookie = ResponseCookie.from("Authorization", token)
                .maxAge(60 * 60 * 60)
                .path("/")
                .httpOnly(true)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("로그인 성공");
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getProfile(@AuthenticationPrincipal CustomOAuthUser user) {
        UserDTO response = userService.getUserProfile(user.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDTO> updateProfile(@AuthenticationPrincipal CustomOAuthUser user,
                                                 @RequestBody UserDTO dto) {
        UserDTO response = userService.updateUserProfile(user.getName(), dto);
        return ResponseEntity.ok(response);
    }
}
