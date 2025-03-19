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

    // 게시글 생성
    public PostDTO createPost(PostDTO dto, List<MultipartFile> files) throws IOException {
        Menu menu = menuRepository.findById(dto.getMenuId())
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + dto.getMenuId()));

        User author = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + dto.getUserId()));

        Post post = postMapper.toEntity(dto);
        post.setMenu(menu);
        post.setAuthor(author);

        Post savedPost = postRepository.save(post);

        // 파일 업로드
        List<FileDTO> uploadedFiles = fileService.uploadFiles(files, savedPost.getId());
        dto.setFileIds(uploadedFiles.stream().map(FileDTO::getId).collect(Collectors.toList()));

        return postMapper.toDto(savedPost);
    }

    // 게시글 수정
    public PostDTO updatePost(Long postId, PostDTO dto, List<MultipartFile> files) throws IOException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));

        if (!post.getAuthor().getId().equals(dto.getUserId())) {
            throw new IllegalStateException("게시글을 수정할 권한이 없습니다.");
        }

        if (dto.getMenuId() != null && !dto.getMenuId().equals(post.getMenu().getId())) {
            Menu menu = menuRepository.findById(dto.getMenuId())
                    .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + dto.getMenuId()));
            post.setMenu(menu);
        }

        // 파일 업로드 (기존 파일 삭제 후 새 파일 추가)
        if (files != null && !files.isEmpty()) {
            fileService.deleteFilesByPostId(postId);
            List<FileDTO> uploadedFiles = fileService.uploadFiles(files, postId);
            dto.setFileIds(uploadedFiles.stream().map(FileDTO::getId).collect(Collectors.toList()));
        }

        postMapper.partialUpdate(post, dto);
        Post updatedPost = postRepository.save(post);
        System.out.println("Updated Post menuId: " + updatedPost.getMenu().getId());

        // PostDTO 수동 매핑
        PostDTO result = postMapper.toDto(updatedPost);
        result.setMenuId(updatedPost.getMenu().getId()); // menuId 수동 설정
        System.out.println("Mapped DTO: " + result.toString());
        return result;
    }

    // 게시글 삭제
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new IllegalStateException("게시글을 삭제할 권한이 없습니다.");
        }

        fileService.deleteFilesByPostId(postId); // 파일 삭제
        postRepository.delete(post); // 게시글 및 댓글 삭제
    }

    // 전체 게시글 목록 조회
    public List<PostDTO> getAllPosts() {
        List<Post> posts = postRepository.findAll();
        return postMapper.toDto(posts);
    }


    // 작성자별 게시글 조회
    public List<PostDTO> getPostsByUserId(Long userId) {
        List<Post> posts = postRepository.findByAuthorId(userId);
        return postMapper.toDto(posts);
    }

    // 특정 게시글 조회
    public PostDTO getPostById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));
        return postMapper.toDto(post);
    }

    public List<PostDTO> getPostsByMenuId(Long menuId) {
        // 해당 메뉴와 모든 자식 메뉴의 ID를 수집
        List<Long> menuIds = new ArrayList<>();
        menuIds.add(menuId);

        // 자식 메뉴 ID 가져오기 (재귀적으로 모든 하위 메뉴 포함)
        List<Menu> subMenus = menuRepository.findByParentId(menuId);
        collectSubMenuIds(subMenus, menuIds);

        // 해당 메뉴들과 연관된 모든 게시글 조회
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
                    // Files 엔티티에서 fileIds 매핑
                    dto.setFileIds(post.getFiles().stream()
                            .map(Files::getId)
                            .collect(Collectors.toList()));
                    System.out.println("Post ID: " + post.getId() +
                            ", Menu ID: " + post.getMenu().getId() +
                            ", User ID: " + post.getAuthor().getId() +
                            ", File IDs: " + dto.getFileIds());
                    System.out.println("Mapped DTO: " + dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // 재귀적으로 자식 메뉴 ID 수집
    private void collectSubMenuIds(List<Menu> menus, List<Long> menuIds) {
        for (Menu menu : menus) {
            menuIds.add(menu.getId());
            List<Menu> subMenus = menuRepository.findByParentId(menu.getId());
            collectSubMenuIds(subMenus, menuIds);
        }
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
                    // Files 엔티티에서 fileIds 매핑
                    dto.setFileIds(post.getFiles().stream()
                            .map(Files::getId)
                            .collect(Collectors.toList()));
                    System.out.println("Post ID: " + post.getId() +
                            ", Menu ID: " + post.getMenu().getId() +
                            ", User ID: " + post.getAuthor().getId() +
                            ", File IDs: " + dto.getFileIds());
                    System.out.println("Mapped DTO: " + dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }
}