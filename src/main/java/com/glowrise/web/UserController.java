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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<UserDTO> signUp(@RequestBody UserDTO dto) {
        UserDTO createdUser = userService.signUp(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @GetMapping("/profile/{username}")
    public ResponseEntity<UserDTO> getUserProfile(@PathVariable String username) {
        UserDTO userProfile = userService.getUserProfile(username);
        return ResponseEntity.ok(userProfile);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        UserDTO userDTO = userService.getCurrentUserWithBlogInfo(authentication);
        return ResponseEntity.ok(userDTO);
    }

    @PutMapping("/profile/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> updateUserProfile(
            @RequestBody UserDTO dto,
            Authentication authentication) {
        UserDTO updatedUser = userService.updateUserProfile(authentication, dto);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/me/nickname")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateUserNickname(@RequestBody UserDTO request, Authentication authentication) {
        userService.updateUserNickname(request.getNickName(), authentication);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getCookieValue(request, "RefreshToken");
        Map<String, String> tokens = userService.refreshToken(refreshToken);
        return ResponseEntity.ok(tokens);
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