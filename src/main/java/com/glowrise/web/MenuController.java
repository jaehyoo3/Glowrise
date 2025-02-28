package com.glowrise.web;

import com.glowrise.service.MenuService;
import com.glowrise.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

}
