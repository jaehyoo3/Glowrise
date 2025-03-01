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

        if (registrationId.equals("naver")) {
            oAuth2Response = new NaverDTO(oAuth2User.getAttributes());
        } else if (registrationId.equals("google")) {
            oAuth2Response = new GoogleDTO(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException("지원되지 않는 제공자입니다.");
        }

        String username = oAuth2Response.getProvider() + " " + oAuth2Response.getProviderId();
        User existData = userRepository.findByUsername(username);

        if (existData == null) {
            User userEntity = new User();
            userEntity.setUsername(username);
            userEntity.setEmail(oAuth2Response.getEmail());
            userEntity.setNickName(oAuth2Response.getName());
            userEntity.setRole(ROLE.ROLE_USER);
            userEntity.setSite(registrationId.equals("naver") ? SITE.NAVER : SITE.GOOGLE);

            userRepository.save(userEntity);

            UserDTO userDTO = new UserDTO();
            userDTO.setUsername(username);
            userDTO.setNickName(oAuth2Response.getName());
            userDTO.setRole(ROLE.ROLE_USER.name());

            return new CustomOAuthUser(userDTO);
        } else {
            existData.setEmail(oAuth2Response.getEmail());
            existData.setNickName(oAuth2Response.getName());
            userRepository.save(existData);

            UserDTO userDTO = new UserDTO();
            userDTO.setUsername(existData.getUsername());
            userDTO.setNickName(existData.getNickName());
            userDTO.setRole(existData.getRole().name());

            return new CustomOAuthUser(userDTO);
        }
    }
}