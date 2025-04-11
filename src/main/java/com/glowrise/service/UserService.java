package com.glowrise.service;

import com.glowrise.config.jwt.JWTUtil;
import com.glowrise.domain.User;
import com.glowrise.domain.enumerate.ROLE;
import com.glowrise.domain.enumerate.SITE;
import com.glowrise.repository.BlogRepository;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.UserDTO;
import com.glowrise.service.exception.DuplicateEmailException;
import com.glowrise.service.exception.DuplicateUsernameException;
import com.glowrise.service.exception.InvalidTokenException;
import com.glowrise.service.mapper.UserMapper;
import com.glowrise.service.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final BlogRepository blogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    private static final long ACCESS_TOKEN_VALIDITY_MS = 60 * 60 * 1000L;
    private final UserMapper userMapper;
    private final SecurityUtil securityUtil; // Added
    private static final long REFRESH_TOKEN_VALIDITY_MS = 7 * 24 * 60 * 60 * 1000L;

    @Transactional
    public UserDTO signUp(UserDTO dto) {
        validateUserCreation(dto);

        User user = userMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(ROLE.ROLE_USER);
        user.setSite(SITE.LOCAL);

        User savedUser = userRepository.save(user);
        return userMapper.toDtoWithoutPassword(savedUser);
    }

    public UserDTO getUserProfile(String username) {
        User user = securityUtil.findUserByUsernameOrThrow(username);
        return userMapper.toDtoWithoutPassword(user);
    }

    public UserDTO getCurrentUserWithBlogInfo(Authentication ignoredAuthentication) {
        User user = securityUtil.getCurrentUserOrThrow();
        UserDTO userDTO = userMapper.toDtoWithoutPassword(user);

        blogRepository.findByUserId(user.getId()).ifPresent(blog -> {
            userDTO.setBlogId(blog.getId());
            userDTO.setBlogUrl(blog.getUrl());
        });
        return userDTO;
    }

    @Transactional
    public UserDTO updateUserProfile(Authentication ignoredAuthentication, UserDTO dto) {
        User user = securityUtil.getCurrentUserOrThrow();

        if (StringUtils.hasText(dto.getNickName()) && !Objects.equals(user.getNickName(), dto.getNickName())) {
            String trimmedNickname = validateAndTrimNickname(dto.getNickName(), user.getId());
            user.setNickName(trimmedNickname);
        }

        if (user.getSite() == SITE.LOCAL && StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        return userMapper.toDtoWithoutPassword(user);
    }

    @Transactional
    public Optional<UserDTO> partialUpdateUser(UserDTO dto) {
        return userRepository.findById(dto.getId())
                .map(existingUser -> {
                    userMapper.partialUpdate(existingUser, dto);
                    if (dto.getPassword() != null && !dto.getPassword().isEmpty() && existingUser.getSite() == SITE.LOCAL) {
                        existingUser.setPassword(passwordEncoder.encode(dto.getPassword()));
                    }
                    return existingUser;
                })
                .map(userMapper::toDtoWithoutPassword);
    }

    @Transactional
    public Map<String, String> refreshToken(String refreshToken) {
        validateRefreshToken(refreshToken);
        String username = jwtUtil.getUsername(refreshToken);
        User user = securityUtil.findUserByUsernameOrThrow(username);

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new InvalidTokenException("리프레시 토큰이 일치하지 않습니다.");
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name(), ACCESS_TOKEN_VALIDITY_MS);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsername(), REFRESH_TOKEN_VALIDITY_MS);

        user.setAccessToken(newAccessToken);
        user.setRefreshToken(newRefreshToken);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", newRefreshToken);
        return tokens;
    }

    @Transactional
    public void updateUserNickname(String newNickname, Authentication ignoredAuthentication) {
        User user = securityUtil.getCurrentUserOrThrow();
        String trimmedNickname = validateAndTrimNickname(newNickname, user.getId());
        user.setNickName(trimmedNickname);
    }

    private void validateUserCreation(UserDTO dto) {
        if (!StringUtils.hasText(dto.getUsername())) throw new IllegalArgumentException("사용자명을 입력해주세요.");
        if (!StringUtils.hasText(dto.getEmail())) throw new IllegalArgumentException("이메일을 입력해주세요.");
        if (!StringUtils.hasText(dto.getPassword())) throw new IllegalArgumentException("비밀번호를 입력해주세요.");

        userRepository.findByUsername(dto.getUsername()).ifPresent(u -> {
            throw new DuplicateUsernameException("이미 존재하는 사용자명입니다.");
        });
        userRepository.findByEmail(dto.getEmail()).ifPresent(u -> {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
        });
    }

    private String validateAndTrimNickname(String nickname, Long currentUserId) {
        if (!StringUtils.hasText(nickname)) throw new IllegalArgumentException("닉네임은 비어 있을 수 없습니다.");
        String trimmedNickname = nickname.trim();
        if (trimmedNickname.length() < 2 || trimmedNickname.length() > 15)
            throw new IllegalArgumentException("닉네임은 2자 이상 15자 이하로 입력해주세요.");
        if (!trimmedNickname.matches("^[a-zA-Z0-9가-힣_]+$"))
            throw new IllegalArgumentException("닉네임은 영문, 숫자, 한글, 밑줄(_)만 사용 가능합니다.");

        userRepository.findByNickName(trimmedNickname).ifPresent(user -> {
            if (!Objects.equals(user.getId(), currentUserId)) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }
        });
        return trimmedNickname;
    }

    private void validateRefreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) throw new InvalidTokenException("리프레시 토큰이 제공되지 않았습니다.");
        try {
            if (jwtUtil.isExpired(refreshToken)) throw new InvalidTokenException("리프레시 토큰이 만료되었습니다.");
        } catch (Exception e) {
            throw new InvalidTokenException("리프레시 토큰 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}