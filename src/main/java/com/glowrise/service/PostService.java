package com.glowrise.service;

import com.glowrise.repository.PostRepository;
import com.glowrise.service.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PostService {
    private final PostRepository postRepository;
    private final PostMapper postMapper;
}
