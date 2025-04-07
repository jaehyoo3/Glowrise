package com.glowrise.service;

import com.glowrise.config.jwt.JWTUtil;
import com.glowrise.config.jwt.dto.CustomOAuthUser;
import com.glowrise.domain.Blog;
import com.glowrise.domain.User;
import com.glowrise.domain.enumerate.ROLE;
import com.glowrise.domain.enumerate.SITE;
import com.glowrise.repository.BlogRepository;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.UserDTO;
import com.glowrise.service.mapper.UserMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
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
public class UserService {
    private final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final BlogRepository blogRepository;
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
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name(), 60 * 60 * 1000L);
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

    public Map<String, String> refreshToken(String refreshToken, HttpServletResponse response) {
        System.out.println("Refresh request received, Refresh Token: " + (refreshToken != null ? "present" : "missing"));

        // 리프레시 토큰이 없는 경우
        if (refreshToken == null) {
            throw new IllegalArgumentException("No refresh token provided");
        }

        try {
            // 리프레시 토큰 만료 확인
            if (jwtUtil.isExpired(refreshToken)) {
                System.out.println("Refresh token expired");
                throw new IllegalArgumentException("Refresh token expired");
            }

            // 사용자 정보 조회
            String username = jwtUtil.getUsername(refreshToken);
            User userEntity = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + username));

            // 리프레시 토큰 일치 여부 확인
            if (!userEntity.getRefreshToken().equals(refreshToken)) {
                System.out.println("Refresh token mismatch");
                throw new IllegalArgumentException("Invalid refresh token");
            }

            // 새 액세스 토큰 발급
            String role = userEntity.getRole().name();
            String newAccessToken = jwtUtil.generateAccessToken(userEntity.getId(), username, role, 60 * 60 * 1000L); // 1시간
            userEntity.setAccessToken(newAccessToken);
            userRepository.save(userEntity);

            // 쿠키에 새 액세스 토큰 설정
            response.addCookie(createCookie("Authorization", newAccessToken));
            System.out.println("New access token issued: " + newAccessToken);

            // 응답 데이터 준비
            Map<String, String> result = new HashMap<>();
            result.put("message", "Token refreshed");
            result.put("accessToken", newAccessToken);
            return result;

        } catch (Exception e) {
            System.out.println("Refresh token processing failed: " + e.getMessage());
            throw new IllegalArgumentException("Invalid refresh token: " + e.getMessage());
        }
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(60 * 60 * 24); // 24시간
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }

    public Map<String, Object> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다.");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        return response;
    }

    public UserDTO getCurrentUser2(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }

        // details에서 userId 추출
        Object details = authentication.getDetails();
        Long userId;
        if (details instanceof Map) {
            Object id = ((Map<?, ?>) details).get("userId");
            if (id instanceof Number) {
                userId = ((Number) id).longValue();
            } else {
                throw new IllegalStateException("userId가 유효하지 않습니다.");
            }
        } else {
            // 대안: username으로 DB 조회
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
            userId = user.getId();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        UserDTO userDTO = userMapper.toDto(user);
        Blog blog = blogRepository.findByUserId(userId).orElse(null);
        if (blog != null) {
            userDTO.setBlogId(blog.getId());
            userDTO.setBlogUrl(blog.getUrl());
        }
        return userDTO;
    }

    /**
     * 현재 로그인된 사용자의 닉네임을 업데이트합니다.
     *
     * @param newNickname    변경할 새 닉네임
     * @param authentication 현재 인증 정보
     * @throws UsernameNotFoundException 사용자를 찾을 수 없을 때
     * @throws IllegalArgumentException  닉네임 유효성 검증 실패 시
     */
    @Transactional
    public void updateUserNickname(String newNickname, Authentication authentication) {
        log.info("닉네임 변경 서비스 시작 - 요청된 닉네임: '{}'", newNickname);

        // 1. 입력값 기본 검증 (null 또는 공백)
        // StringUtils.hasText()는 null, 빈 문자열, 공백만 있는 문자열 모두 false 반환
        if (!StringUtils.hasText(newNickname)) {
            log.warn("닉네임 변경 실패: 닉네임이 비어있거나 공백입니다.");
            throw new IllegalArgumentException("닉네임은 비어 있을 수 없습니다.");
        }
        String trimmedNickname = newNickname.trim(); // 앞뒤 공백 제거 후 사용

        // 2. 인증 정보에서 사용자 ID 추출
        Long userId = getUserIdFromAuthentication(authentication); // 기존 헬퍼 메소드 사용
        log.debug("사용자 ID 추출 완료: {}", userId);

        // 3. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("닉네임 변경 실패: 사용자를 찾을 수 없습니다. ID: {}", userId);
                    // UsernameNotFoundException 또는 커스텀 예외 사용 가능
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId);
                });

        // 4. 닉네임 유효성 검증 (Business Logic Validation) - 이 메소드 내부에서 IllegalArgumentException 발생시킴
        validateNickname(trimmedNickname, user.getId());
        log.debug("닉네임 유효성 검증 통과: {}", trimmedNickname);

        // 5. 닉네임 업데이트 및 저장
        user.setNickName(trimmedNickname);
        // userRepository.save(user); // @Transactional 이므로 변경 감지로 자동 저장됨 (명시적 save도 무방)
        log.info("사용자 ID {}의 닉네임이 '{}'(으)로 성공적으로 변경되었습니다.", userId, trimmedNickname);
    }

    /**
     * Authentication 객체에서 사용자 ID를 추출하는 헬퍼 메소드.
     * (이전 코드와 동일)
     */
    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        // 방법 1: Principal이 CustomOAuthUser 타입인 경우
        if (authentication.getPrincipal() instanceof CustomOAuthUser userPrincipal) {
            if (userPrincipal.getUserId() != null) {
                return userPrincipal.getUserId();
            }
        }
        // 방법 2: JWTFilter에서 details 맵에 userId를 넣은 경우
        Object details = authentication.getDetails();
        if (details instanceof Map) {
            Object userIdObj = ((Map<?, ?>) details).get("userId");
            if (userIdObj instanceof Long) return (Long) userIdObj;
            if (userIdObj instanceof Integer) return ((Integer) userIdObj).longValue();
            if (userIdObj instanceof String) try {
                return Long.parseLong((String) userIdObj);
            } catch (NumberFormatException e) { /* 무시 */ }
        }
        log.error("인증 정보에서 사용자 ID를 추출할 수 없습니다. Principal: {}, Details: {}", authentication.getPrincipal(), authentication.getDetails());
        throw new IllegalStateException("인증 정보에서 사용자 ID를 추출할 수 없습니다.");
    }

    /**
     * 닉네임 유효성을 검증하는 메소드 (예시).
     * (이전 코드와 동일 - IllegalArgumentException 발생시킴)
     */
    private void validateNickname(String nickname, Long currentUserId) {
        // 길이 검증
        if (nickname.length() < 2 || nickname.length() > 15) {
            throw new IllegalArgumentException("닉네임은 2자 이상 15자 이하로 입력해주세요.");
        }
        // 형식 검증 (예: 특수문자 제한)
        if (!nickname.matches("^[a-zA-Z0-9가-힣_]+$")) {
            throw new IllegalArgumentException("닉네임은 영문, 숫자, 한글, 밑줄(_)만 사용 가능합니다.");
        }
        // 중복 검증 (자기 자신 제외)
        userRepository.findByNickName(nickname).ifPresent(user -> {
            if (!Objects.equals(user.getId(), currentUserId)) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }
        });
    }



}