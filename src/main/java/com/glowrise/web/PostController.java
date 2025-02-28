package com.glowrise.web;

import com.glowrise.service.PostService;
import com.glowrise.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

}
