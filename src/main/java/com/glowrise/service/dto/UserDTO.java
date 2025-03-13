package com.glowrise.service.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserDTO {

    private Long id;
    private String username;
    private String email;
    private String nickName;
    private String password;
    private String role;
    private String site;
    private Long blogId;
    private Long userId;
}
