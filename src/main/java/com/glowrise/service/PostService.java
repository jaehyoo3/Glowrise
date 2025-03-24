package com.glowrise.service;

import com.glowrise.domain.*;
import com.glowrise.repository.FileRepository;
import com.glowrise.repository.MenuRepository;
import com.glowrise.repository.PostRepository;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.FileDTO;
import com.glowrise.service.dto.PostDTO;
import com.glowrise.service.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final MenuRepository menuRepository;
    private final UserRepository userRepository;
    private final FileService fileService;

    public PostDTO createPost(PostDTO dto, List<MultipartFile> files, Authentication authentication) throws IOException {
        Long userId = getUserIdFromAuthentication(authentication);
        Menu menu = menuRepository.findById(dto.getMenuId())
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + dto.getMenuId()));

        if (!menu.getBlog().getUser().getId().equals(userId)) {
            throw new IllegalStateException("해당 블로그에 게시글을 작성할 권한이 없습니다.");
        }

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        Post post = postMapper.toEntity(dto);
        post.setMenu(menu);
        post.setAuthor(author);

        Post savedPost = postRepository.save(post);

        List<FileDTO> uploadedFiles = fileService.uploadFiles(files, savedPost.getId());
        dto.setFileIds(uploadedFiles.stream().map(FileDTO::getId).collect(Collectors.toList()));

        return postMapper.toDto(savedPost);
    }

    public PostDTO updatePost(Long postId, PostDTO dto, List<MultipartFile> files, Authentication authentication) throws IOException {
        Long userId = getUserIdFromAuthentication(authentication);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new IllegalStateException("게시글을 수정할 권한이 없습니다.");
        }

        if (dto.getMenuId() != null && !dto.getMenuId().equals(post.getMenu().getId())) {
            Menu menu = menuRepository.findById(dto.getMenuId())
                    .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + dto.getMenuId()));
            if (!menu.getBlog().getUser().getId().equals(userId)) {
                throw new IllegalStateException("해당 블로그의 메뉴로 이동할 권한이 없습니다.");
            }
            post.setMenu(menu);
        }

        if (files != null && !files.isEmpty()) {
            fileService.deleteFilesByPostId(postId);
            List<FileDTO> uploadedFiles = fileService.uploadFiles(files, postId);
            dto.setFileIds(uploadedFiles.stream().map(FileDTO::getId).collect(Collectors.toList()));
        }

        postMapper.partialUpdate(post, dto);
        Post updatedPost = postRepository.save(post);
        PostDTO result = postMapper.toDto(updatedPost);
        result.setMenuId(updatedPost.getMenu().getId());
        return result;
    }

    public void deletePost(Long postId, Long userId, Authentication authentication) {
        getUserIdFromAuthentication(authentication); // 로그인 체크
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new IllegalStateException("게시글을 삭제할 권한이 없습니다.");
        }

        fileService.deleteFilesByPostId(postId);
        postRepository.delete(post);
    }

    public List<PostDTO> getAllPosts() {
        List<Post> posts = postRepository.findAll();
        return postMapper.toDto(posts);
    }

    public List<PostDTO> getPostsByUserId(Long userId) {
        List<Post> posts = postRepository.findByAuthorId(userId);
        return postMapper.toDto(posts);
    }

    public PostDTO getPostById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));
        return postMapper.toDto(post);
    }

    public List<PostDTO> getPostsByMenuId(Long menuId) {
        List<Long> menuIds = new ArrayList<>();
        menuIds.add(menuId);

        List<Menu> subMenus = menuRepository.findByParentId(menuId);
        collectSubMenuIds(subMenus, menuIds);

        List<Post> posts = postRepository.findByMenuIdIn(menuIds);
        return posts.stream()
                .map(post -> {
                    PostDTO dto = new PostDTO();
                    dto.setId(post.getId());
                    dto.setTitle(post.getTitle());
                    dto.setContent(post.getContent());
                    dto.setMenuId(post.getMenu().getId());
                    dto.setUserId(post.getAuthor().getId());
                    dto.setCommentsId(post.getComments().stream().map(Comment::getId).collect(Collectors.toList()));
                    dto.setFileIds(post.getFiles().stream().map(Files::getId).collect(Collectors.toList()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<PostDTO> getAllPostsByBlogId(Long blogId) {
        List<Long> menuIds = menuRepository.findByBlogId(blogId).stream()
                .map(Menu::getId)
                .collect(Collectors.toList());

        List<Post> posts = postRepository.findByMenuIdIn(menuIds);
        return posts.stream()
                .map(post -> {
                    PostDTO dto = new PostDTO();
                    dto.setId(post.getId());
                    dto.setTitle(post.getTitle());
                    dto.setContent(post.getContent());
                    dto.setMenuId(post.getMenu().getId());
                    dto.setUserId(post.getAuthor().getId());
                    dto.setCommentsId(post.getComments().stream().map(Comment::getId).collect(Collectors.toList()));
                    dto.setFileIds(post.getFiles().stream().map(Files::getId).collect(Collectors.toList()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private void collectSubMenuIds(List<Menu> menus, List<Long> menuIds) {
        for (Menu menu : menus) {
            menuIds.add(menu.getId());
            List<Menu> subMenus = menuRepository.findByParentId(menu.getId());
            collectSubMenuIds(subMenus, menuIds);
        }
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
        return user.getId();
    }
}