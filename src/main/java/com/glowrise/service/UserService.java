package com.glowrise.service;

import com.glowrise.repository.MenuRepository;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
}
