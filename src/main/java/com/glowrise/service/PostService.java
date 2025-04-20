package com.glowrise.service;

import com.glowrise.domain.*;
import com.glowrise.domain.enumerate.TimePeriod;
import com.glowrise.repository.FileRepository;
import com.glowrise.repository.MenuRepository;
import com.glowrise.repository.PostRepository;
import com.glowrise.service.dto.FileDTO;
import com.glowrise.service.dto.IdCountDTO;
import com.glowrise.service.dto.PostDTO;
import com.glowrise.service.mapper.PostMapper;
import com.glowrise.service.util.QueryDslPagingUtil;
import com.glowrise.service.util.SecurityUtil;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.owasp.html.PolicyFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final int VIEW_COUNT_TTL_SECONDS = 300;
    private static final String VIEW_COUNT_KEY_PREFIX_USER = "view:post:%d:user:%d:ip:%s";
    private static final String VIEW_COUNT_KEY_PREFIX_ANONYMOUS = "view:post:%d:ip:%s";

    private final MenuRepository menuRepository;
    private final SecurityUtil securityUtil;
    private final PolicyFactory htmlPolicyFactory;
    private final FileRepository fileRepository;
    private final FileService fileService;
    private final QueryDslPagingUtil pagingUtil;
    private final JPAQueryFactory queryFactory;
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    @PreAuthorize("@authorizationService.isBlogOwnerByMenuId(#dto.menuId)")
    public PostDTO createPost(PostDTO dto, List<MultipartFile> files, Authentication ignoredAuthentication) throws IOException {
        User author = securityUtil.getCurrentUserOrThrow();
        Menu menu = findMenuByIdOrThrow(dto.getMenuId());

        Post post = postMapper.toEntity(dto);

        String originalHtmlContent = dto.getContent();
        String sanitizedContent = htmlPolicyFactory.sanitize(originalHtmlContent);
        post.setContent(sanitizedContent);

        post.setAuthor(author);
        post.setMenu(menu);
        post.setViewCount(0L);

        Post savedPost = postRepository.save(post);

        handleFileUploads(files, savedPost);

        List<Long> inlineIds = dto.getInlineImageFileIds();
        if (inlineIds != null && !inlineIds.isEmpty()) {
            List<StoredFile> inlineFilesToUpdate = fileRepository.findAllById(inlineIds);
            for (StoredFile inlineFile : inlineFilesToUpdate) {
                if (inlineFile.getPost() == null) {
                    inlineFile.setPost(savedPost);
                    fileRepository.save(inlineFile);
                }
            }
        }

        PostDTO resultDto = postMapper.toDto(savedPost);
        List<FileDTO> attachedFileDtos = fileService.getFilesByPostId(savedPost.getId());
        resultDto.setFileIds(attachedFileDtos.stream().map(FileDTO::getId).collect(Collectors.toList()));
        resultDto.setCommentCount(0L);
        int inlineImageCount = (inlineIds != null) ? inlineIds.size() : 0;
        resultDto.setFileCount((long) attachedFileDtos.size() + inlineImageCount);

        return resultDto;
    }

    @Transactional
    @PreAuthorize("@authorizationService.isPostOwner(#postId)")
    public PostDTO updatePost(Long postId, PostDTO dto, List<MultipartFile> files, Authentication ignoredAuthentication) throws IOException {
        Long currentUserId = securityUtil.getCurrentUserIdOrThrow(); // Needed for handleMenuUpdate check
        Post post = findPostByIdOrThrow(postId);

        handleMenuUpdate(dto.getMenuId(), post, currentUserId);

        String newHtmlContent = dto.getContent();
        String sanitizedContent = htmlPolicyFactory.sanitize(newHtmlContent);

        post.setTitle(dto.getTitle());
        post.setContent(sanitizedContent);

        handleFileUpdates(files, post);

        Post updatedPost = postRepository.save(post);

        PostDTO resultDto = postMapper.toDto(updatedPost);
        if (updatedPost.getFiles() != null) {
            resultDto.setFileIds(updatedPost.getFiles().stream().map(StoredFile::getId).collect(Collectors.toList()));
        } else {
            resultDto.setFileIds(new ArrayList<>());
        }
        resultDto.setCommentCount(post.getComments() != null ? post.getComments().size() : 0L);

        return resultDto;
    }

    @Transactional
    @PreAuthorize("@authorizationService.isPostOwner(#postId)")
    public void deletePost(Long postId, Authentication ignoredAuthentication) {
        Post post = findPostByIdOrThrow(postId);
        fileService.deleteFilesByPostId(postId);
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public PostDTO getPostById(Long postId, String clientIp, Authentication authentication) {
        Post post = findPostByIdOrThrow(postId);
        tryIncrementViewCount(post, clientIp, authentication);

        PostDTO postDTO = postMapper.toDto(post);
        postDTO.setUpdatedAt(post.getLastModifiedDate());
        if (post.getFiles() != null) {
            postDTO.setFileIds(post.getFiles().stream().map(StoredFile::getId).collect(Collectors.toList()));
        } else {
            postDTO.setFileIds(new ArrayList<>());
        }
        postDTO.setCommentCount(0L);
        postDTO.setMenuId(post.getMenu().getId());

        return postDTO;
    }

    @Transactional(readOnly = true)
    public List<PostDTO> getAllPosts() {
        return postRepository.findAll().stream()
                .map(postMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PostDTO> getPostsByUserId(Long userId) {
        securityUtil.findUserByIdOrThrow(userId);
        return postRepository.findByAuthorId(userId).stream()
                .map(postMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PostDTO> getPosts(Long blogId, Long menuId, String searchKeyword, Pageable pageable) {
        QPost post = QPost.post;
        QMenu menu = QMenu.menu;
        QComment comment = QComment.comment;
        QStoredFile files = QStoredFile.storedFile;

        List<Long> targetMenuIds = null;
        if (menuId != null) {
            targetMenuIds = menuRepository.findAllMenuIdsByParent(menuId);
            if (targetMenuIds == null || targetMenuIds.isEmpty()) return Page.empty(pageable);
        }

        JPQLQuery<Post> baseQuery = queryFactory
                .selectFrom(post)
                .join(post.menu, menu).fetchJoin()
                .join(post.author).fetchJoin()
                .where(
                        menu.blog.id.eq(blogId),
                        menuId != null && !targetMenuIds.isEmpty()
                                ? post.menu.id.in(targetMenuIds) : null,
                        (searchKeyword != null && !searchKeyword.trim().isEmpty())
                                ? post.title.containsIgnoreCase(searchKeyword)
                                .or(post.content.containsIgnoreCase(searchKeyword)) : null
                )
                .orderBy(post.lastModifiedDate.desc());

        Page<Post> postPage = pagingUtil.getPage(baseQuery, pageable);
        List<Post> posts = postPage.getContent();

        if (posts.isEmpty()) {
            return Page.empty(pageable);
        }
        List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());

        Map<Long, Long> commentCountMap = queryFactory
                .select(Projections.constructor(IdCountDTO.class, comment.post.id, comment.countDistinct().longValue()))
                .from(comment)
                .where(comment.post.id.in(postIds))
                .groupBy(comment.post.id)
                .fetch().stream().collect(Collectors.toMap(IdCountDTO::getId, IdCountDTO::getCount));

        Map<Long, Long> fileCountMap = queryFactory
                .select(Projections.constructor(IdCountDTO.class, files.post.id, files.countDistinct().longValue()))
                .from(files)
                .where(files.post.id.in(postIds))
                .groupBy(files.post.id)
                .fetch().stream().collect(Collectors.toMap(IdCountDTO::getId, IdCountDTO::getCount));

        List<PostDTO> postDtos = posts.stream()
                .map(p -> {
                    PostDTO dto = postMapper.toDto(p);
                    dto.setCommentCount(commentCountMap.getOrDefault(p.getId(), 0L));
                    dto.setFileCount(fileCountMap.getOrDefault(p.getId(), 0L));
                    dto.setUpdatedAt(p.getLastModifiedDate());
                    return dto;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(postDtos, pageable, postPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<PostDTO> getPopularPostsByRecentModification(TimePeriod period, int limit) {
        QPost post = QPost.post;
        QComment comment = QComment.comment;
        QMenu menu = QMenu.menu;

        LocalDateTime startDateTime = calculateStartDateTime(period);

        NumberExpression<Long> popularityScore = post.viewCount.coalesce(0L).multiply(3)
                .add(comment.countDistinct().multiply(5));

        OrderSpecifier<?> scoreOrder = popularityScore.desc();
        OrderSpecifier<?> dateOrder = post.lastModifiedDate.desc();

        List<Long> topPostIds = queryFactory
                .select(post.id)
                .from(post)
                .leftJoin(post.comments, comment)
                .where(post.lastModifiedDate.goe(startDateTime))
                .groupBy(post.id, post.viewCount, post.lastModifiedDate)
                .orderBy(scoreOrder, dateOrder)
                .limit(limit)
                .fetch();

        if (topPostIds == null || topPostIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Post> postsUnordered = queryFactory
                .selectFrom(post)
                .join(post.menu, menu).fetchJoin()
                .join(post.author).fetchJoin()
                .where(post.id.in(topPostIds))
                .fetch();

        Map<Long, Post> postMap = postsUnordered.stream().collect(Collectors.toMap(Post::getId, p -> p));
        List<Post> postsOrdered = topPostIds.stream().map(postMap::get).filter(Objects::nonNull).collect(Collectors.toList());

        return enrichPostsWithCounts(postsOrdered);
    }

    private List<PostDTO> enrichPostsWithCounts(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());

        QComment comment = QComment.comment;
        QStoredFile files = QStoredFile.storedFile;

        Map<Long, Long> commentCountMap = queryFactory
                .select(Projections.constructor(IdCountDTO.class, comment.post.id, comment.countDistinct().longValue()))
                .from(comment).where(comment.post.id.in(postIds)).groupBy(comment.post.id)
                .fetch().stream().collect(Collectors.toMap(IdCountDTO::getId, IdCountDTO::getCount));

        Map<Long, Long> fileCountMap = queryFactory
                .select(Projections.constructor(IdCountDTO.class, files.post.id, files.countDistinct().longValue()))
                .from(files).where(files.post.id.in(postIds)).groupBy(files.post.id)
                .fetch().stream().collect(Collectors.toMap(IdCountDTO::getId, IdCountDTO::getCount));

        return posts.stream().map(p -> {
            PostDTO dto = postMapper.toDto(p);
            if (p.getAuthor() != null) dto.setUserId(p.getAuthor().getId());
            dto.setUpdatedAt(p.getLastModifiedDate());
            dto.setCommentCount(commentCountMap.getOrDefault(p.getId(), 0L));
            dto.setFileCount(fileCountMap.getOrDefault(p.getId(), 0L));
            return dto;
        }).collect(Collectors.toList());
    }

    private LocalDateTime calculateStartDateTime(TimePeriod period) {
        LocalDateTime now = LocalDateTime.now();
        switch (period) {
            case DAILY:
                return now.toLocalDate().atStartOfDay();
            case WEEKLY:
                return now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
            case MONTHLY:
                return now.toLocalDate().withDayOfMonth(1).atStartOfDay();
            default:
                return now.toLocalDate().atStartOfDay();
        }
    }

    private Menu findMenuByIdOrThrow(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new EntityNotFoundException("메뉴를 찾을 수 없습니다: " + menuId));
    }

    private Post findPostByIdOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다: " + postId));
    }

    private void checkBlogOwnership(Menu menu, Long currentUserId) {
        if (!Objects.equals(menu.getBlog().getUser().getId(), currentUserId)) {
            throw new IllegalStateException("해당 블로그에 접근할 권한이 없습니다.");
        }
    }

    private void handleMenuUpdate(Long requestedMenuId, Post post, Long currentUserId) {
        if (requestedMenuId != null && !Objects.equals(requestedMenuId, post.getMenu().getId())) {
            Menu newMenu = findMenuByIdOrThrow(requestedMenuId);
            checkBlogOwnership(newMenu, currentUserId);
            post.setMenu(newMenu);
        }
    }

    private void handleFileUploads(List<MultipartFile> files, Post post) throws IOException {
        if (files != null && !files.isEmpty()) {
            List<FileDTO> uploadedFileDtos = fileService.uploadFiles(files, post.getId());
            List<StoredFile> fileEntities = mapFileDtosToEntities(uploadedFileDtos, post);
            post.setFiles(fileEntities);
        }
    }

    private void handleFileUpdates(List<MultipartFile> files, Post post) throws IOException {
        if (files != null && !files.isEmpty()) {
            fileService.deleteFilesByPostId(post.getId());
            if (post.getFiles() != null) {
                post.getFiles().clear();
            } else {
                post.setFiles(new ArrayList<>());
            }
            List<FileDTO> uploadedFileDtos = fileService.uploadFiles(files, post.getId());
            List<StoredFile> newFileEntities = mapFileDtosToEntities(uploadedFileDtos, post);
            post.getFiles().addAll(newFileEntities);
        }
    }

    private List<StoredFile> mapFileDtosToEntities(List<FileDTO> fileDtos, Post post) {
        return fileDtos.stream().map(dto -> {
            StoredFile file = new StoredFile();
            file.setId(dto.getId());
            file.setFileName(dto.getFileName());
            file.setFilePath(dto.getFilePath());
            file.setContentType(dto.getContentType());
            file.setFileSize(dto.getFileSize());
            file.setPost(post);
            return file;
        }).collect(Collectors.toList());
    }

    private boolean tryIncrementViewCount(Post post, String clientIp, Authentication authentication) {
        String redisKey = buildViewCountRedisKey(post.getId(), clientIp, authentication);
        Boolean isNewView = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", VIEW_COUNT_TTL_SECONDS, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(isNewView)) {
            int updatedRows = postRepository.incrementViewCount(post.getId());
            if (updatedRows > 0) {
                post.setViewCount(post.getViewCount() == null ? 1L : post.getViewCount() + 1);
                return true;
            }
        }
        return false;
    }

    private String buildViewCountRedisKey(Long postId, String clientIp, Authentication authentication) {
        return securityUtil.getCurrentUserId()
                .map(userId -> String.format(VIEW_COUNT_KEY_PREFIX_USER, postId, userId, clientIp))
                .orElseGet(() -> String.format(VIEW_COUNT_KEY_PREFIX_ANONYMOUS, postId, clientIp));
    }
}
