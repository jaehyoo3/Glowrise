package com.glowrise.config;

import org.springframework.security.core.userdetails.User; // 올바른 임포트
import com.glowrise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.glowrise.domain.User user = userRepository.findByEmail(email) // 커스텀 User 엔티티
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        String roleName = user.getRole().name().replace("ROLE_", "");

        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(roleName)
                .build();
    }
}