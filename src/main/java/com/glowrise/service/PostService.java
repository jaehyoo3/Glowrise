package com.glowrise.service;

import com.glowrise.domain.Menu;
import com.glowrise.domain.Post;
import com.glowrise.domain.User;
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
        return postMapper.toDto(updatedPost);
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

    // 메뉴별 게시글 조회
    public List<PostDTO> getPostsByMenuId(Long menuId) {
        List<Post> posts = postRepository.findByMenuId(menuId);
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
}