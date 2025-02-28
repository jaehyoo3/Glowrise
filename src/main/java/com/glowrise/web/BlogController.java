package com.glowrise.web;

import com.glowrise.service.BlogService;
import com.glowrise.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

}
