package com.glowrise.service;

import com.glowrise.config.jwt.JWTUtil;
import com.glowrise.domain.User;
import com.glowrise.domain.enumerate.ROLE;
import com.glowrise.domain.enumerate.SITE;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.UserDTO;
import com.glowrise.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;

    // 일반 회원가입
    public UserDTO signUp(UserDTO dto) throws Exception {
        validateDuplicateUsername(dto.getUsername());
        validateDuplicateEmail(dto.getEmail());

        User user = userMapper.toEntity(dto); // DTO → 엔티티 변환
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // 비밀번호 암호화
        user.setRole(ROLE.ROLE_USER); // 기본 역할 설정
        user.setSite(SITE.LOCAL); // 기본 사이트 설정

        User savedUser = userRepository.save(user);
        UserDTO responseDto = userMapper.toDto(savedUser);
        responseDto.setPassword(null); // 응답에서 비밀번호 제외
        return responseDto;
    }

    // 사용자 정보 조회
    @Transactional(readOnly = true)
    public UserDTO getUserProfile(String username) throws Exception{
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new Exception("사용자를 찾을 수 없습니다"));

        UserDTO dto = userMapper.toDto(user);
        dto.setPassword(null); // 응답에서 비밀번호 제외
        return dto;
    }

    // 사용자 정보 수정
    public UserDTO updateUserProfile(String username, UserDTO dto) throws Exception {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new Exception("사용자를 찾을 수 없습니다"));

        // 이메일 변경 시 중복 체크
        if (dto.getEmail() != null && !user.getEmail().equals(dto.getEmail())) {
            validateDuplicateEmail(dto.getEmail());
        }

        // 비밀번호 변경 (로컬 사용자만)
        if (user.getSite() == SITE.LOCAL && dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        userMapper.partialUpdate(user, dto); // 부분 업데이트

        User updatedUser = userRepository.save(user);
        UserDTO responseDto = userMapper.toDto(updatedUser);
        responseDto.setPassword(null); // 응답에서 비밀번호 제외
        return responseDto;
    }

    // 부분 업데이트 (선택적)
    public Optional<UserDTO> partialUpdateUser(UserDTO dto) {
        return userRepository.findById(dto.getId())
                .map(existingUser -> {
                    userMapper.partialUpdate(existingUser, dto);
                    if (dto.getPassword() != null && !dto.getPassword().isEmpty() && existingUser.getSite() == SITE.LOCAL) {
                        existingUser.setPassword(passwordEncoder.encode(dto.getPassword()));
                    }
                    return existingUser;
                })
                .map(userRepository::save)
                .map(user -> {
                    UserDTO responseDto = userMapper.toDto(user);
                    responseDto.setPassword(null);
                    return responseDto;
                });
    }

    // 리프레시 토큰을 사용한 토큰 갱신
    @Transactional
    public Map<String, String> refreshToken(String refreshToken) throws Exception {
        if (refreshToken == null || jwtUtil.isExpired(refreshToken)) {
            throw new Exception("유효하지 않거나 만료된 리프레시 토큰입니다");
        }

        String username = jwtUtil.getUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new Exception("사용자를 찾을 수 없습니다"));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new Exception("리프레시 토큰이 일치하지 않습니다");
        }

        // 새로운 토큰 생성
        String newAccessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRole().name(), 60 * 60 * 1000L);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsername(), 7 * 24 * 60 * 60 * 1000L);

        // User 엔티티에 저장
        user.setAccessToken(newAccessToken);
        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        // 토큰 반환
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", newRefreshToken);
        return tokens;
    }

    // 중복 체크 메서드
    private void validateDuplicateUsername(String username) throws  Exception {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new Exception("이미 존재하는 사용자명입니다");
        }
    }

    private void validateDuplicateEmail(String email) throws Exception{
        if (userRepository.findByEmail(email).isPresent()) {
            throw new Exception("이미 사용 중인 이메일입니다");
        }
    }
}