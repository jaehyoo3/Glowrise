package com.glowrise.web;

import com.glowrise.service.SearchService;
import com.glowrise.service.dto.SearchResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<SearchResultDTO> search(
            @RequestParam String query,
            @PageableDefault(size = 10, sort = "createdDate,desc") Pageable pageable) {

        SearchResultDTO results = searchService.searchIntegrated(query, pageable);
        return ResponseEntity.ok(results);
    }
}