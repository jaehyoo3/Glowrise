package com.glowrise.service;

import com.glowrise.domain.*;
import com.glowrise.repository.MenuRepository;
import com.glowrise.repository.PostRepository;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.FileDTO;
import com.glowrise.service.dto.PostDTO;
import com.glowrise.service.mapper.PostMapper;
import com.glowrise.service.util.QueryDslPagingUtil;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final MenuRepository menuRepository;
    private final UserRepository userRepository;
    private final FileService fileService;
    private final QueryDslPagingUtil pagingUtil;
    private final JPAQueryFactory queryFactory;
    private final RedisTemplate<String, String> redisTemplate; // RedisTemplate 주입

    private static final int VIEW_COUNT_TTL = 300; // 5분 (초 단위)

    public PostDTO createPost(PostDTO dto, List<MultipartFile> files, Authentication authentication) throws IOException {
        Long userId = getUserIdFromAuthentication(authentication);
        Menu menu = menuRepository.findById(dto.getMenuId())
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + dto.getMenuId()));

        if (!menu.getBlog().getUser().getId().equals(userId)) {
            throw new IllegalStateException("해당 블로그에 게시글을 작성할 권한이 없습니다.");
        }

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // PostDTO -> Post (수동 매핑)
        Post post = new Post();
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setViewCount(0L);
        post.setMenu(menu);
        post.setAuthor(author);

        Post savedPost = postRepository.save(post);

        // 파일 업로드
        List<FileDTO> uploadedFiles = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            uploadedFiles = fileService.uploadFiles(files, savedPost.getId());
            // Post 엔티티에 파일 연결
            List<Files> fileEntities = uploadedFiles.stream()
                    .map(fileDTO -> {
                        Files file = new Files();
                        file.setId(fileDTO.getId());
                        file.setFileName(fileDTO.getFileName());
                        file.setFilePath(fileDTO.getFilePath());
                        file.setContentType(fileDTO.getContentType());
                        file.setFileSize(fileDTO.getFileSize());
                        file.setPost(savedPost);
                        return file;
                    })
                    .collect(Collectors.toList());
            savedPost.setFiles(fileEntities);
        }

        // Post -> PostDTO (수동 매핑)
        PostDTO result = new PostDTO();
        result.setId(savedPost.getId());
        result.setTitle(savedPost.getTitle());
        result.setContent(savedPost.getContent());
        result.setMenuId(savedPost.getMenu() != null ? savedPost.getMenu().getId() : null);
        result.setUserId(savedPost.getAuthor() != null ? savedPost.getAuthor().getId() : null);
        result.setCommentCount(0L);
        result.setViewCount(savedPost.getViewCount());
        result.setFileIds(uploadedFiles.stream().map(FileDTO::getId).collect(Collectors.toList()));
        result.setCreatedAt(savedPost.getCreatedDate());
        result.setUpdatedAt(savedPost.getLastModifiedDate());

        return result;
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

        // Post 업데이트 (수동 매핑)
        if (dto.getTitle() != null) {
            post.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            post.setContent(dto.getContent());
        }

        // 파일 처리
        List<FileDTO> uploadedFiles = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            fileService.deleteFilesByPostId(postId);
            post.getFiles().clear(); // 기존 파일 목록 비우기
            uploadedFiles = fileService.uploadFiles(files, postId);
            List<Files> fileEntities = uploadedFiles.stream()
                    .map(fileDTO -> {
                        Files file = new Files();
                        file.setId(fileDTO.getId());
                        file.setFileName(fileDTO.getFileName());
                        file.setFilePath(fileDTO.getFilePath());
                        file.setContentType(fileDTO.getContentType());
                        file.setFileSize(fileDTO.getFileSize());
                        file.setPost(post);
                        return file;
                    })
                    .collect(Collectors.toList());
            post.setFiles(fileEntities);
        }

        Post updatedPost = postRepository.save(post);

        // Post -> PostDTO (수동 매핑)
        PostDTO result = new PostDTO();
        result.setId(updatedPost.getId());
        result.setTitle(updatedPost.getTitle());
        result.setContent(updatedPost.getContent());
        result.setMenuId(updatedPost.getMenu() != null ? updatedPost.getMenu().getId() : null);
        result.setUserId(updatedPost.getAuthor() != null ? updatedPost.getAuthor().getId() : null);
        result.setCommentCount(0L);
        result.setViewCount(updatedPost.getViewCount());
        result.setFileIds(uploadedFiles.stream().map(FileDTO::getId).collect(Collectors.toList()));
        result.setCreatedAt(updatedPost.getCreatedDate());
        result.setUpdatedAt(updatedPost.getLastModifiedDate());

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

    public PostDTO getPostById(Long postId, String clientIp, Authentication authentication) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));

        // 조회수 증가 로직
        incrementViewCount(post, clientIp, authentication);
        postRepository.save(post);

        // Post -> PostDTO (수동 매핑)
        PostDTO postDTO = new PostDTO();
        postDTO.setId(post.getId());
        postDTO.setTitle(post.getTitle());
        postDTO.setContent(post.getContent());
        postDTO.setMenuId(post.getMenu() != null ? post.getMenu().getId() : null);
        postDTO.setUserId(post.getAuthor() != null ? post.getAuthor().getId() : null);
        postDTO.setCommentCount(0L); // 실제 댓글 수 로직 필요 시 추가
        postDTO.setViewCount(post.getViewCount());
        List<Files> files = post.getFiles();
        if (files != null) {
            postDTO.setFileIds(files.stream()
                    .map(Files::getId)
                    .collect(Collectors.toList()));
        } else {
            postDTO.setFileIds(new ArrayList<>());
        }
        postDTO.setCreatedAt(post.getCreatedDate());
        postDTO.setUpdatedAt(post.getLastModifiedDate());

        return postDTO;
    }

    private void incrementViewCount(Post post, String clientIp, Authentication authentication) {
        String redisKey;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Long userId = getUserIdFromAuthentication(authentication);
            redisKey = String.format("view:post:%d:user:%d:ip:%s", post.getId(), userId, clientIp);
        } else {
            redisKey = String.format("view:post:%d:ip:%s", post.getId(), clientIp);
        }

        // Redis에서 키가 존재하는지 확인하고 없으면 조회수 증가
        Boolean isNewView = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", VIEW_COUNT_TTL, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(isNewView)) {
            post.setViewCount(post.getViewCount() == null ? 1 : post.getViewCount() + 1);
            postRepository.save(post);
        }
    }

    public Page<PostDTO> getPosts(Long blogId, Long menuId, String searchKeyword, Pageable pageable) {
        QPost post = QPost.post;
        QMenu menu = QMenu.menu;
        QFiles files = QFiles.files;
        QComment comment = QComment.comment;

        // 메뉴 ID 목록 조회 최적화 (재귀 호출 제거)
        List<Long> menuIds = menuId != null ? menuRepository.findAllMenuIdsByParent(menuId) : null;

        // 댓글 수를 group by를 사용하여 효율적으로 계산
        Map<Long, Long> commentCountMap = queryFactory
                .from(comment)
                .select(comment.post.id, comment.count().longValue())
                .groupBy(comment.post.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(tuple -> tuple.get(0, Long.class), tuple -> tuple.get(1, Long.class)));

        // 게시글 목록 조회
        JPQLQuery<PostDTO> query = queryFactory
                .select(Projections.constructor(PostDTO.class,
                        post.id,
                        post.title,
                        post.content,
                        post.menu.id,
                        post.author.id,
                        post.viewCount,
                        Expressions.constant(new ArrayList<Long>()), // fileIds 후처리
                        post.createdDate,
                        post.lastModifiedDate,
                        post.files.size()
                ))
                .from(post)
                .join(post.menu, menu)
                .where(
                        menu.blog.id.eq(blogId),
                        menuId != null ? post.menu.id.in(menuIds) : null,
                        searchKeyword != null ? post.title.containsIgnoreCase(searchKeyword).or(post.content.containsIgnoreCase(searchKeyword)) : null
                )
                .orderBy(post.lastModifiedDate.desc())
                .distinct();

        // 페이징 적용
        Page<PostDTO> postsPage = pagingUtil.getPage(query, pageable);
        List<PostDTO> dtos = postsPage.getContent();

        // 한 번의 쿼리로 모든 파일 ID 조회 후 매핑
        Map<Long, List<Long>> fileIdsMap = queryFactory
                .from(files)
                .select(files.post.id, files.id)
                .where(files.post.id.in(dtos.stream().map(PostDTO::getId).collect(Collectors.toList())))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(tuple -> tuple.get(0, Long.class),
                        Collectors.mapping(tuple -> tuple.get(1, Long.class), Collectors.toList())));

        // 결과 DTO에 댓글 수 및 파일 ID 설정
        dtos.forEach(dto -> {
            dto.setCommentCount(commentCountMap.getOrDefault(dto.getId(), 0L));
            dto.setFileIds(fileIdsMap.getOrDefault(dto.getId(), new ArrayList<>()));
        });

        return new PageImpl<>(dtos, pageable, postsPage.getTotalElements());
    }

    private List<Post> mostViewPostList() {
        return postRepository.findTop10ByOrderByViewCountDesc();
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