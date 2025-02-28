package com.glowrise.service;


import com.glowrise.repository.BlogRepository;
import com.glowrise.service.mapper.BlogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BlogService {
    private final BlogRepository blogRepository;
    private final BlogMapper blogMapper;
}
