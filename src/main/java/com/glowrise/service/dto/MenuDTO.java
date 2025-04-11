package com.glowrise.service.dto;


import lombok.Data;
import java.util.List;

@Data
public class MenuDTO {
    private Long id;
    private String name;
    private Integer orderIndex;
    private Long blogId;
    private Long parentId;
    private List<Long> subMenuIds;
    private List<Long> postIds;

}
