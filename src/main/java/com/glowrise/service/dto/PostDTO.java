package com.glowrise.service.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PostDTO {
    private Long id;

    private String title;

    private String content;


    private Long menuId;

    private Long userId;

    private List<Long> commentsId = new ArrayList<>();
}
