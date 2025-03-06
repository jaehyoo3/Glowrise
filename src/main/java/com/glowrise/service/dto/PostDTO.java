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
    private List<Long> fileIds = new ArrayList<>(); // 첨부된 파일 ID 목록

}
