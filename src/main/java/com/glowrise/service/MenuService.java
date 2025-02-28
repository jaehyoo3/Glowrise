package com.glowrise.service;


import com.glowrise.repository.MenuRepository;
import com.glowrise.service.mapper.MenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;
}
