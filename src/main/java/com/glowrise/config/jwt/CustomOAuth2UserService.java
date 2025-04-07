package com.glowrise.config.jwt;

import com.glowrise.config.jwt.dto.CustomOAuthUser;
import com.glowrise.config.jwt.dto.GoogleDTO;
import com.glowrise.config.jwt.dto.NaverDTO;
import com.glowrise.config.jwt.dto.OAuth2DTO;
import com.glowrise.domain.User;
import com.glowrise.domain.enumerate.ROLE;
import com.glowrise.domain.enumerate.SITE;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional // 사용자 조회 및 저장을 한 트랜잭션으로 묶음
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(request);
        String registrationId = request.getClientRegistration().getRegistrationId();
        OAuth2DTO oAuth2Response;

        // OAuth2 제공자별 DTO 생성
        if (registrationId.equals("naver")) {
            oAuth2Response = new NaverDTO(oAuth2User.getAttributes());
        } else if (registrationId.equals("google")) {
            oAuth2Response = new GoogleDTO(oAuth2User.getAttributes());
        } else {
            // 지원하지 않는 제공자 처리 (로그 또는 예외)
            // 예: throw new OAuth2AuthenticationException("지원되지 않는 제공자입니다: " + registrationId);
            return null; // 또는 적절한 예외 처리
        }

        // --- username은 고유 식별자로 사용 ---
        String username = oAuth2Response.getProvider() + " " + oAuth2Response.getProviderId();

        // 사용자 조회 또는 신규 생성
        User userEntity = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    // --- 신규 사용자 생성 시 nickname은 null 또는 기본값으로 설정 ---
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setEmail(oAuth2Response.getEmail()); // 이메일은 초기 설정
                    // newUser.setNickName(oAuth2Response.getName()); // 자동 설정 제거!
                    newUser.setNickName(null); // 명시적으로 null 또는 빈 문자열 등으로 설정
                    newUser.setRole(ROLE.ROLE_USER);
                    newUser.setSite(registrationId.equals("naver") ? SITE.NAVER : SITE.GOOGLE);
                    // password는 OAuth 사용자의 경우 보통 null 또는 랜덤값으로 설정
                    return userRepository.save(newUser);
                });

        // --- 기존 사용자의 경우, OAuth 정보로 닉네임 등을 덮어쓰지 않음! ---
        // 로그인 시마다 최신 OAuth 정보로 업데이트할 필드가 있다면 여기서 처리 (예: 이메일 동기화 등)
        // userEntity.setEmail(oAuth2Response.getEmail()); // 필요하다면 이메일 업데이트 로직 추가

        // userRepository.save(userEntity); // 위에서 email 등 업데이트 시 저장 필요


        // 최종적으로 UserDTO 생성 (nickname은 DB에 저장된 값 또는 null이 됨)
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(userEntity.getUsername());
        userDTO.setNickName(userEntity.getNickName()); // DB 값 (null일 수 있음)
        userDTO.setRole(userEntity.getRole().name());
        userDTO.setUserId(userEntity.getId()); // userId 추가 (CustomSuccessHandler 등에서 사용)

        return new CustomOAuthUser(userDTO);
    }
}