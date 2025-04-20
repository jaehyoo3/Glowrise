package com.glowrise.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDTO {
    private Page<PostDTO> posts; // List -> Page 로 변경
    private Page<UserDTO> users; // List -> Page 로 변경
}