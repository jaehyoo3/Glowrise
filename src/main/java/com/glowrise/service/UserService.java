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
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션 설정
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final BlogRepository blogRepository;
    // 상수 정의
    private static final long ACCESS_TOKEN_VALIDITY_MS = 60 * 60 * 1000L; // 1시간
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    private static final long REFRESH_TOKEN_VALIDITY_MS = 7 * 24 * 60 * 60 * 1000L; // 7일
    private final UserMapper userMapper; // MapStruct 매퍼 주입

    /**
     * 일반 회원가입
     *
     * @param dto 회원가입 정보 DTO
     * @return 생성된 사용자 정보 DTO (비밀번호 제외)
     */
    @Transactional // 쓰기 작업
    public UserDTO signUp(UserDTO dto) {
        log.info("회원가입 서비스 시작 - Username: {}", dto.getUsername());
        validateUserCreation(dto); // 중복 및 유효성 검사

        User user = userMapper.toEntity(dto); // DTO -> Entity 변환
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(ROLE.ROLE_USER);
        user.setSite(SITE.LOCAL);

        User savedUser = userRepository.save(user);
        log.info("회원가입 완료 - User ID: {}", savedUser.getId());
        // 비밀번호 제외 DTO 반환 (UserMapper에 toDtoWithoutPassword 구현 가정)
        return userMapper.toDtoWithoutPassword(savedUser);
    }

    /**
     * 사용자명으로 사용자 프로필 조회
     *
     * @param username 조회할 사용자명
     * @return 사용자 정보 DTO (비밀번호 제외)
     */
    public UserDTO getUserProfile(String username) {
        log.debug("사용자 프로필 조회 시도 - Username: {}", username);
        User user = findUserByUsername(username);
        // 비밀번호 제외 DTO 반환 (UserMapper에 toDtoWithoutPassword 구현 가정)
        return userMapper.toDtoWithoutPassword(user);
    }

    /**
     * 현재 인증된 사용자 정보 및 블로그 정보 조회
     *
     * @param authentication 현재 인증 정보
     * @return 사용자 정보 DTO (비밀번호 제외, 블로그 정보 포함)
     */
    public UserDTO getCurrentUserWithBlogInfo(Authentication authentication) {
        User user = findUserFromAuthentication(authentication);
        log.debug("현재 사용자 정보 조회 완료 - User ID: {}", user.getId());

        // 비밀번호 제외 DTO 반환 (UserMapper에 toDtoWithoutPassword 구현 가정)
        UserDTO userDTO = userMapper.toDtoWithoutPassword(user);

        // 블로그 정보 조회 및 DTO에 추가
        blogRepository.findByUserId(user.getId()).ifPresent(blog -> {
            userDTO.setBlogId(blog.getId());
            userDTO.setBlogUrl(blog.getUrl());
            log.debug("사용자 블로그 정보 추가 - Blog ID: {}", blog.getId());
        });

        return userDTO;
    }


    /**
     * 현재 인증된 사용자 정보 수정 (닉네임, 비밀번호 등)
     *
     * @param authentication 현재 인증 정보
     * @param dto            수정할 정보 DTO
     * @return 업데이트된 사용자 정보 DTO (비밀번호 제외)
     */
    @Transactional // 쓰기 작업
    public UserDTO updateUserProfile(Authentication authentication, UserDTO dto) {
        log.info("현재 사용자 정보 수정 서비스 시작");
        User user = findUserFromAuthentication(authentication); // 인증 정보로 사용자 조회
        log.debug("수정 대상 사용자 확인 - User ID: {}", user.getId());

        // 닉네임 변경 시 유효성 및 중복 검사
        // dto에 nickName이 있고, 기존 값과 다를 때만 검증 및 업데이트 수행
        if (StringUtils.hasText(dto.getNickName()) && !Objects.equals(user.getNickName(), dto.getNickName())) {
            log.debug("닉네임 변경 시도: {} -> {}", user.getNickName(), dto.getNickName());
            String trimmedNickname = validateAndTrimNickname(dto.getNickName(), user.getId()); // 검증 및 공백 제거
            user.setNickName(trimmedNickname);
            log.info("닉네임 변경 완료 - User ID: {}", user.getId());
        }

        // 비밀번호 변경 (로컬 사용자 & 비밀번호 입력 시)
        if (user.getSite() == SITE.LOCAL && StringUtils.hasText(dto.getPassword())) {
            log.debug("비밀번호 변경 시도 - User ID: {}", user.getId());
            // 비밀번호 유효성 검사 로직 추가 (예: 길이, 복잡도)
            // if (!isValidPassword(dto.getPassword())) {
            //     throw new IllegalArgumentException("비밀번호 형식이 올바르지 않습니다.");
            // }
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
            log.info("비밀번호 변경 완료 - User ID: {}", user.getId());
        }

        // 이메일 변경 로직은 제외 (필요 시 추가)

        // 다른 필드 업데이트 (필요한 경우 여기에 추가)
        // 예: user.setSomeField(dto.getSomeField());

        // @Transactional 에 의해 변경 감지되어 자동 저장됨 (save 호출 불필요)
        log.info("사용자 정보 수정 완료 - User ID: {}", user.getId());
        // 비밀번호 제외 DTO 반환 (UserMapper에 toDtoWithoutPassword 구현 가정)
        return userMapper.toDtoWithoutPassword(user);
    }

    /**
     * ID 기반 사용자 부분 업데이트 (UserMapper의 partialUpdate 활용 예시)
     * 사용 권장하지 않음 - /profile/me 엔드포인트 사용 권장
     * 필요하다면 강력한 권한 검증 로직 추가 필요
     */
    @Transactional
    public Optional<UserDTO> partialUpdateUser(UserDTO dto) {
        log.warn("ID 기반 부분 업데이트 시도 (사용주의) - User ID: {}", dto.getId());
        return userRepository.findById(dto.getId())
                .map(existingUser -> {
                    // 권한 검증 로직 필요! (예: 관리자 또는 본인)

                    // MapStruct의 partialUpdate 활용 (null이 아닌 필드만 업데이트하도록 설정 필요)
                    userMapper.partialUpdate(existingUser, dto);

                    // 비밀번호는 별도 처리
                    if (dto.getPassword() != null && !dto.getPassword().isEmpty() && existingUser.getSite() == SITE.LOCAL) {
                        log.debug("부분 업데이트: 비밀번호 변경 시도 - User ID: {}", dto.getId());
                        existingUser.setPassword(passwordEncoder.encode(dto.getPassword()));
                    }
                    // @Transactional이므로 save는 선택적
                    // return userRepository.save(existingUser);
                    return existingUser;
                })
                .map(user -> userMapper.toDtoWithoutPassword(user)); // 비밀번호 제외 DTO 반환
    }


    /**
     * 리프레시 토큰을 사용한 토큰 갱신
     *
     * @param refreshToken 리프레시 토큰
     * @return 새로 발급된 Access Token 및 Refresh Token 맵
     */
    @Transactional // 쓰기 작업 (토큰 업데이트)
    public Map<String, String> refreshToken(String refreshToken) {
        log.info("토큰 갱신 서비스 시작");

        validateRefreshToken(refreshToken); // Null, 만료 검증
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

        // DB에 토큰 저장 정책에 따라 업데이트
        user.setAccessToken(newAccessToken); // 필요시 업데이트
        user.setRefreshToken(newRefreshToken); // 갱신된 리프레시 토큰 저장
        // userRepository.save(user); // @Transactional이면 생략 가능

        log.info("새 토큰 정보 DB 업데이트 완료 (필요시) - User ID: {}", user.getId());
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", newRefreshToken); // 새 리프레시 토큰도 반환 (클라이언트에서 관리 시)
        log.info("토큰 갱신 완료 - User ID: {}", user.getId());
        return tokens;
    }


    /**
     * 현재 로그인된 사용자의 닉네임을 업데이트합니다.
     *
     * @param newNickname    변경할 새 닉네임
     * @param authentication 현재 인증 정보
     */
    @Transactional // 쓰기 작업
    public void updateUserNickname(String newNickname, Authentication authentication) {
        log.info("닉네임 변경 서비스 시작 - 요청 닉네임: '{}'", newNickname);
        User user = findUserFromAuthentication(authentication); // 인증 정보로 사용자 찾기

        String trimmedNickname = validateAndTrimNickname(newNickname, user.getId()); // 유효성 검증
        log.debug("닉네임 유효성 검증 통과: {}", trimmedNickname);

        user.setNickName(trimmedNickname); // 닉네임 업데이트
        log.info("사용자 ID {}의 닉네임이 '{}'(으)로 성공적으로 변경되었습니다.", user.getId(), trimmedNickname);
    }

    /**
     * 회원가입 시 사용자명, 이메일 중복 및 유효성 검사
     *
     * @param dto 검사할 사용자 DTO
     */
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
            throw new DuplicateUsernameException("이미 존재하는 사용자명입니다."); // 커스텀 예외 사용 권장
        });
        userRepository.findByEmail(dto.getEmail()).ifPresent(u -> {
            log.warn("회원가입 실패: 중복된 이메일 - {}", dto.getEmail());
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다."); // 커스텀 예외 사용 권장
        });
    }

    /**
     * 닉네임 유효성 검증 및 공백 제거
     *
     * @param nickname      검증할 닉네임
     * @param currentUserId 현재 사용자 ID (중복 검사 시 제외용)
     * @return 공백 제거된 유효한 닉네임
     */
    private String validateAndTrimNickname(String nickname, Long currentUserId) {
        if (!StringUtils.hasText(nickname)) {
            log.warn("닉네임 유효성 검증 실패: 비어 있음 - User ID: {}", currentUserId);
            throw new IllegalArgumentException("닉네임은 비어 있을 수 없습니다.");
        }
        String trimmedNickname = nickname.trim();

        // 길이 검증
        if (trimmedNickname.length() < 2 || trimmedNickname.length() > 15) {
            log.warn("닉네임 유효성 검증 실패: 길이 오류 ({}) - User ID: {}", trimmedNickname.length(), currentUserId);
            throw new IllegalArgumentException("닉네임은 2자 이상 15자 이하로 입력해주세요.");
        }
        // 형식 검증 (예: 영문, 숫자, 한글, 밑줄)
        if (!trimmedNickname.matches("^[a-zA-Z0-9가-힣_]+$")) {
            log.warn("닉네임 유효성 검증 실패: 형식 오류 ('{}') - User ID: {}", trimmedNickname, currentUserId);
            throw new IllegalArgumentException("닉네임은 영문, 숫자, 한글, 밑줄(_)만 사용 가능합니다.");
        }
        // 중복 검증 (자기 자신 제외)
        userRepository.findByNickName(trimmedNickname).ifPresent(user -> {
            if (!Objects.equals(user.getId(), currentUserId)) {
                log.warn("닉네임 유효성 검증 실패: 중복 ('{}') - User ID: {}", trimmedNickname, currentUserId);
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다."); // DuplicateNicknameException 권장
            }
        });
        return trimmedNickname;
    }

    /**
     * Refresh Token 유효성 검증 (Null 또는 만료)
     *
     * @param refreshToken 검증할 리프레시 토큰
     */
    private void validateRefreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            log.warn("리프레시 토큰 유효성 검증 실패: 토큰 없음");
            throw new InvalidTokenException("리프레시 토큰이 제공되지 않았습니다."); // 커스텀 예외 사용 권장
        }
        try {
            if (jwtUtil.isExpired(refreshToken)) {
                log.warn("리프레시 토큰 유효성 검증 실패: 만료됨");
                throw new InvalidTokenException("리프레시 토큰이 만료되었습니다.");
            }
        } catch (Exception e) { // jwtUtil.isExpired 에서 발생할 수 있는 모든 예외 처리
            log.error("리프레시 토큰 검증 중 오류 발생", e);
            throw new InvalidTokenException("리프레시 토큰 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 사용자명으로 User 엔티티 조회 (찾지 못하면 예외 발생)
     *
     * @param username 조회할 사용자명
     * @return User 엔티티
     */
    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("사용자 조회 실패: 사용자명을 찾을 수 없음 - {}", username);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
                });
    }

    /**
     * Authentication 객체에서 User 엔티티 조회
     *
     * @param authentication 인증 정보
     * @return User 엔티티
     */
    private User findUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("인증 정보 없음 또는 인증되지 않음");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다.");
        }

        // Authentication 이름(일반적으로 username) 사용
        String username = authentication.getName();
        if (username == null) {
            log.error("Authentication에서 username을 추출할 수 없습니다. Principal: {}", authentication.getPrincipal());
            throw new IllegalStateException("인증 정보에서 사용자 이름을 찾을 수 없습니다.");
        }
        log.debug("Authentication에서 사용자명 추출: {}", username);
        // 추출된 username으로 DB에서 User 조회
        return findUserByUsername(username);
    }
}