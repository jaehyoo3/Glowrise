package com.glowrise.service;

import com.glowrise.config.jwt.JWTUtil;
import com.glowrise.domain.User;
import com.glowrise.domain.enumerate.ROLE;
import com.glowrise.domain.enumerate.SITE;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.UserDTO;
import com.glowrise.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;

    // 일반 회원가입
    public UserDTO signUp(UserDTO dto) {
        validateDuplicateUsername(dto.getUsername());
        validateDuplicateEmail(dto.getEmail());

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setNickName(dto.getNickName());
        user.setRole(ROLE.ROLE_USER);
        user.setSite(SITE.LOCAL);

        User savedUser = userRepository.save(user);
        UserDTO responseDto = userMapper.toDto(savedUser);
        responseDto.setPassword(null); // 응답에서 비밀번호 제외
        return responseDto;
    }

    // 일반 로그인
    public String login(UserDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail());
        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다");
        }

        return jwtUtil.generateToken(user.getEmail(), user.getRole().name(), 60 * 60 * 60L);
    }

    // 사용자 정보 조회
    public UserDTO getUserProfile(String username) {
        User user = findUserByUsername(username);
        UserDTO dto = userMapper.toDto(user);
        dto.setPassword(null); // 응답에서 비밀번호 제외
        return dto;
    }

    // 사용자 정보 수정
    public UserDTO updateUserProfile(String username, UserDTO dto) {
        User user = findUserByUsername(username);

        // 이메일 변경 시 중복 체크
        if (!user.getEmail().equals(dto.getEmail())) {
            validateDuplicateEmail(dto.getEmail());
            user.setEmail(dto.getEmail());
        }

        // 닉네임 업데이트
        if (dto.getNickName() != null) {
            user.setNickName(dto.getNickName());
        }

        // 비밀번호 변경 (로컬 사용자만)
        if (user.getSite() == SITE.LOCAL && dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        UserDTO responseDto = userMapper.toDto(updatedUser);
        responseDto.setPassword(null); // 응답에서 비밀번호 제외
        return responseDto;
    }

    // 중복 체크 메서드
    private void validateDuplicateUsername(String username) {
        if (userRepository.findByUsername(username) != null) {
            throw new RuntimeException("이미 존재하는 사용자명입니다");
        }
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.findByEmail(email) != null) {
            throw new RuntimeException("이미 사용 중인 이메일입니다");
        }
    }

    // 사용자 조회 헬퍼 메서드
    private User findUserByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다");
        }
        return user;
    }
}

