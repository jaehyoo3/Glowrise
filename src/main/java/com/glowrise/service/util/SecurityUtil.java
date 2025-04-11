package com.glowrise.service.util;

import com.glowrise.domain.User;
import com.glowrise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UserRepository userRepository;

    public Optional<String> getCurrentUsername() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext == null) {
            return Optional.empty();
        }
        Authentication authentication = securityContext.getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String && !"anonymousUser".equals(principal)) {
            return Optional.of((String) principal);
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            return Optional.of(((org.springframework.security.core.userdetails.UserDetails) principal).getUsername());
        } else if (authentication.getName() != null && !authentication.getName().equalsIgnoreCase("anonymousUser")) {
            // Fallback for other principal types like CustomOAuthUser if getName() returns username
            return Optional.of(authentication.getName());
        }
        return Optional.empty();
    }

    public Optional<Long> getCurrentUserId() {
        return getCurrentUsername()
                .flatMap(userRepository::findByUsername)
                .map(User::getId);
    }

    public Optional<User> getCurrentUser() {
        return getCurrentUsername()
                .flatMap(userRepository::findByUsername);
    }

    public User getCurrentUserOrThrow() {
        return getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다."));
    }

    public Long getCurrentUserIdOrThrow() {
        return getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("인증된 사용자 ID를 찾을 수 없습니다."));
    }


    public User findUserByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
    }

    public User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다 (ID): " + userId));
    }
}