package com.glowrise.service.dto;


import com.glowrise.domain.enumerate.ROLE;
import com.glowrise.domain.enumerate.SITE;
import lombok.Data;

@Data
public class UserDTO {

    private Long id;

    private String email;

    private String nickName;

    private String password;

    private ROLE role;

    private SITE site;

    private Long blogId;
}
