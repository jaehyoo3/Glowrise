package com.glowrise.config;

import com.glowrise.domain.User;
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
        User user = userRepository.findByEmail(email); // 이메일로 조회
        if (user == null) {
            throw new UsernameNotFoundException("이메일에 해당하는 사용자를 찾을 수 없습니다: " + email);
        }

        String roleName = user.getRole().name().replace("ROLE_", ""); // "ROLE_" 접두어 제거

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername()) // 인증 객체에는 username을 설정
                .password(user.getPassword())
                .roles(roleName)
                .build();
    }
}