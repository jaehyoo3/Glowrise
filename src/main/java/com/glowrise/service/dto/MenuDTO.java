package com.glowrise.service.dto;


import lombok.Data;
import java.util.List;

@Data
public class MenuDTO {
    private Long id; // 메뉴 ID
    private String name; // 메뉴 이름
    private Integer orderIndex; // 메뉴 순서
    private Long blogId; // 블로그 ID
    private Long parentId; // 부모 메뉴 ID (없을 수 있음)
    private List<Long> subMenuIds; // 하위 메뉴들의 ID 목록
    private List<Long> postIds; // 게시글 ID 목록

}
