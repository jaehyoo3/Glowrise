package com.glowrise.web;

import com.glowrise.service.CommentService;
import com.glowrise.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

}
