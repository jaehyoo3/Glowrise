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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final BlogRepository blogRepository;
    private static final long ACCESS_TOKEN_VALIDITY_MS = 60 * 60 * 1000L;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    private static final long REFRESH_TOKEN_VALIDITY_MS = 7 * 24 * 60 * 60 * 1000L;
    private final UserMapper userMapper;

    @Transactional
    public UserDTO signUp(UserDTO dto) {
        log.info("회원가입 서비스 시작 - Username: {}", dto.getUsername());
        validateUserCreation(dto);

        User user = userMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(ROLE.ROLE_USER);
        user.setSite(SITE.LOCAL);

        User savedUser = userRepository.save(user);
        log.info("회원가입 완료 - User ID: {}", savedUser.getId());
        return userMapper.toDtoWithoutPassword(savedUser);
    }

    public UserDTO getUserProfile(String username) {
        log.debug("사용자 프로필 조회 시도 - Username: {}", username);
        User user = findUserByUsername(username);
        return userMapper.toDtoWithoutPassword(user);
    }

    public UserDTO getCurrentUserWithBlogInfo(Authentication authentication) {
        User user = findUserFromAuthentication(authentication);
        log.debug("현재 사용자 정보 조회 완료 - User ID: {}", user.getId());

        UserDTO userDTO = userMapper.toDtoWithoutPassword(user);

        blogRepository.findByUserId(user.getId()).ifPresent(blog -> {
            userDTO.setBlogId(blog.getId());
            userDTO.setBlogUrl(blog.getUrl());
            log.debug("사용자 블로그 정보 추가 - Blog ID: {}", blog.getId());
        });

        return userDTO;
    }

    @Transactional
    public UserDTO updateUserProfile(Authentication authentication, UserDTO dto) {
        log.info("현재 사용자 정보 수정 서비스 시작");
        User user = findUserFromAuthentication(authentication);
        log.debug("수정 대상 사용자 확인 - User ID: {}", user.getId());

        if (StringUtils.hasText(dto.getNickName()) && !Objects.equals(user.getNickName(), dto.getNickName())) {
            log.debug("닉네임 변경 시도: {} -> {}", user.getNickName(), dto.getNickName());
            String trimmedNickname = validateAndTrimNickname(dto.getNickName(), user.getId());
            user.setNickName(trimmedNickname);
            log.info("닉네임 변경 완료 - User ID: {}", user.getId());
        }

        if (user.getSite() == SITE.LOCAL && StringUtils.hasText(dto.getPassword())) {
            log.debug("비밀번호 변경 시도 - User ID: {}", user.getId());
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
            log.info("비밀번호 변경 완료 - User ID: {}", user.getId());
        }

        log.info("사용자 정보 수정 완료 - User ID: {}", user.getId());
        return userMapper.toDtoWithoutPassword(user);
    }

    @Transactional
    public Optional<UserDTO> partialUpdateUser(UserDTO dto) {
        log.warn("ID 기반 부분 업데이트 시도 (사용주의) - User ID: {}", dto.getId());
        return userRepository.findById(dto.getId())
                .map(existingUser -> {
                    userMapper.partialUpdate(existingUser, dto);
                    if (dto.getPassword() != null && !dto.getPassword().isEmpty() && existingUser.getSite() == SITE.LOCAL) {
                        log.debug("부분 업데이트: 비밀번호 변경 시도 - User ID: {}", dto.getId());
                        existingUser.setPassword(passwordEncoder.encode(dto.getPassword()));
                    }
                    return existingUser;
                })
                .map(user -> userMapper.toDtoWithoutPassword(user));
    }

    @Transactional
    public Map<String, String> refreshToken(String refreshToken) {
        log.info("토큰 갱신 서비스 시작");

        validateRefreshToken(refreshToken);
        log.debug("리프레시 토큰 기본 유효성 통과");

        String username = jwtUtil.getUsername(refreshToken);
        User user = findUserByUsername(username);
        log.debug("토큰 사용자 조회 성공 - Username: {}", username);

        if (!refreshToken.equals(user.getRefreshToken())) {
            log.warn("리프레시 토큰 불일치 - User ID: {}", user.getId());
            throw new InvalidTokenException("리프레시 토큰이 일치하지 않습니다.");
        }
        log.debug("저장된 리프레시 토큰 일치 확인");

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name(), ACCESS_TOKEN_VALIDITY_MS);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsername(), REFRESH_TOKEN_VALIDITY_MS);
        log.debug("새 토큰 생성 완료 - User ID: {}", user.getId());

        user.setAccessToken(newAccessToken);
        user.setRefreshToken(newRefreshToken);

        log.info("새 토큰 정보 DB 업데이트 완료 (필요시) - User ID: {}", user.getId());
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", newRefreshToken);
        log.info("토큰 갱신 완료 - User ID: {}", user.getId());
        return tokens;
    }

    @Transactional
    public void updateUserNickname(String newNickname, Authentication authentication) {
        log.info("닉네임 변경 서비스 시작 - 요청 닉네임: '{}'", newNickname);
        User user = findUserFromAuthentication(authentication);

        String trimmedNickname = validateAndTrimNickname(newNickname, user.getId());
        log.debug("닉네임 유효성 검증 통과: {}", trimmedNickname);

        user.setNickName(trimmedNickname);
        log.info("사용자 ID {}의 닉네임이 '{}'(으)로 성공적으로 변경되었습니다.", user.getId(), trimmedNickname);
    }

    private void validateUserCreation(UserDTO dto) {
        if (!StringUtils.hasText(dto.getUsername())) {
            throw new IllegalArgumentException("사용자명을 입력해주세요.");
        }
        if (!StringUtils.hasText(dto.getEmail())) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }

        userRepository.findByUsername(dto.getUsername()).ifPresent(u -> {
            log.warn("회원가입 실패: 중복된 사용자명 - {}", dto.getUsername());
            throw new DuplicateUsernameException("이미 존재하는 사용자명입니다.");
        });
        userRepository.findByEmail(dto.getEmail()).ifPresent(u -> {
            log.warn("회원가입 실패: 중복된 이메일 - {}", dto.getEmail());
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
        });
    }

    private String validateAndTrimNickname(String nickname, Long currentUserId) {
        if (!StringUtils.hasText(nickname)) {
            log.warn("닉네임 유효성 검증 실패: 비어 있음 - User ID: {}", currentUserId);
            throw new IllegalArgumentException("닉네임은 비어 있을 수 없습니다.");
        }
        String trimmedNickname = nickname.trim();

        if (trimmedNickname.length() < 2 || trimmedNickname.length() > 15) {
            log.warn("닉네임 유효성 검증 실패: 길이 오류 ({}) - User ID: {}", trimmedNickname.length(), currentUserId);
            throw new IllegalArgumentException("닉네임은 2자 이상 15자 이하로 입력해주세요.");
        }
        if (!trimmedNickname.matches("^[a-zA-Z0-9가-힣_]+$")) {
            log.warn("닉네임 유효성 검증 실패: 형식 오류 ('{}') - User ID: {}", trimmedNickname, currentUserId);
            throw new IllegalArgumentException("닉네임은 영문, 숫자, 한글, 밑줄(_)만 사용 가능합니다.");
        }
        userRepository.findByNickName(trimmedNickname).ifPresent(user -> {
            if (!Objects.equals(user.getId(), currentUserId)) {
                log.warn("닉네임 유효성 검증 실패: 중복 ('{}') - User ID: {}", trimmedNickname, currentUserId);
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }
        });
        return trimmedNickname;
    }

    private void validateRefreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            log.warn("리프레시 토큰 유효성 검증 실패: 토큰 없음");
            throw new InvalidTokenException("리프레시 토큰이 제공되지 않았습니다.");
        }
        try {
            if (jwtUtil.isExpired(refreshToken)) {
                log.warn("리프레시 토큰 유효성 검증 실패: 만료됨");
                throw new InvalidTokenException("리프레시 토큰이 만료되었습니다.");
            }
        } catch (Exception e) {
            log.error("리프레시 토큰 검증 중 오류 발생", e);
            throw new InvalidTokenException("리프레시 토큰 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("사용자 조회 실패: 사용자명을 찾을 수 없음 - {}", username);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
                });
    }

    private User findUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("인증 정보 없음 또는 인증되지 않음");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다.");
        }

        String username = authentication.getName();
        if (username == null) {
            log.error("Authentication에서 username을 추출할 수 없습니다. Principal: {}", authentication.getPrincipal());
            throw new IllegalStateException("인증 정보에서 사용자 이름을 찾을 수 없습니다.");
        }
        log.debug("Authentication에서 사용자명 추출: {}", username);
        return findUserByUsername(username);
    }
}