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

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
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
            throw new OAuth2AuthenticationException("지원되지 않는 제공자입니다: " + registrationId);
        }

        String username = oAuth2Response.getProvider() + " " + oAuth2Response.getProviderId();

        // 사용자 조회 (Optional 사용)
        User userEntity = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    // 신규 사용자 생성
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setEmail(oAuth2Response.getEmail());
                    newUser.setNickName(oAuth2Response.getName());
                    newUser.setRole(ROLE.ROLE_USER);
                    newUser.setSite(registrationId.equals("naver") ? SITE.NAVER : SITE.GOOGLE);
                    return userRepository.save(newUser);
                });

        // 기존 사용자 정보 업데이트
        if (userEntity.getId() != null) { // 이미 저장된 경우
            userEntity.setEmail(oAuth2Response.getEmail());
            userEntity.setNickName(oAuth2Response.getName());
            userRepository.save(userEntity);
        }

        // UserDTO 생성 및 반환
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(userEntity.getUsername());
        userDTO.setNickName(userEntity.getNickName());
        userDTO.setRole(userEntity.getRole().name());

        return new CustomOAuthUser(userDTO);
    }
}