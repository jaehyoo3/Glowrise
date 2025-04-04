package com.glowrise.service.dto;

import lombok.Data;

@Data
public class BlogDTO {

    private Long id;

    private String title;

    private String description;

    private String url;

    private Long userId;

}
