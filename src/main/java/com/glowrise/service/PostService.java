package com.glowrise.service;

import com.glowrise.domain.*;
import com.glowrise.domain.enumerate.TimePeriod;
import com.glowrise.repository.MenuRepository;
import com.glowrise.repository.PostRepository;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.FileDTO;
import com.glowrise.service.dto.IdCountDTO;
import com.glowrise.service.dto.PostDTO;
import com.glowrise.service.mapper.PostMapper;
import com.glowrise.service.util.QueryDslPagingUtil;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
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
@Slf4j
@RequiredArgsConstructor // 생성자 주입
public class PostService {

    // Constants
    private static final int VIEW_COUNT_TTL_SECONDS = 300; // 5분
    private final MenuRepository menuRepository;
    private final UserRepository userRepository;
    private final FileService fileService;
    private static final String VIEW_COUNT_KEY_PREFIX_USER = "view:post:%d:user:%d:ip:%s";
    private final QueryDslPagingUtil pagingUtil;
    private final JPAQueryFactory queryFactory;
    private static final String VIEW_COUNT_KEY_PREFIX_ANONYMOUS = "view:post:%d:ip:%s";
    // Dependencies
    private final PostRepository postRepository;
    private final PostMapper postMapper; // MapStruct Mapper 활용
    private final RedisTemplate<String, String> redisTemplate;

    // --- Public API ---

    @Transactional // 트랜잭션 보장 (Create)
    public PostDTO createPost(PostDTO dto, List<MultipartFile> files, Authentication authentication) throws IOException {
        Long currentUserId = getCurrentUserId(authentication);
        User author = findUserByIdOrThrow(currentUserId);
        Menu menu = findMenuByIdOrThrow(dto.getMenuId());

        // 권한 검사: 해당 블로그 소유자인지 확인
        checkBlogOwnership(menu, currentUserId);

        // DTO -> Entity 매핑 (Mapper 사용)
        Post post = postMapper.toEntity(dto);
        post.setAuthor(author);
        post.setMenu(menu);
        post.setViewCount(0L); // 초기 조회수 설정

        Post savedPost = postRepository.save(post);

        // 파일 처리
        handleFileUploads(files, savedPost);

        // Entity -> DTO 매핑 (Mapper 사용) + 파일 ID 추가
        PostDTO resultDto = postMapper.toDto(savedPost);
        // 파일 ID는 Mapper에서 처리하거나, 여기서 별도 설정 (Mapper에 로직 추가 권장)
        if (savedPost.getFiles() != null) {
            resultDto.setFileIds(savedPost.getFiles().stream().map(Files::getId).collect(Collectors.toList()));
        } else {
            resultDto.setFileIds(new ArrayList<>());
        }
        // Comment Count는 필요 시 별도 조회 후 설정 (일반적으로 목록 조회 시 처리)
        resultDto.setCommentCount(0L);


        return resultDto;
    }

    @Transactional // 트랜잭션 보장 (Update)
    public PostDTO updatePost(Long postId, PostDTO dto, List<MultipartFile> files, Authentication authentication) throws IOException {
        Long currentUserId = getCurrentUserId(authentication);
        Post post = findPostByIdOrThrow(postId);

        // 권한 검사: 게시글 작성자인지 확인
        checkPostOwnership(post, currentUserId);

        // 메뉴 변경 처리
        handleMenuUpdate(dto.getMenuId(), post, currentUserId);

        // 게시글 내용 업데이트 (Mapper 사용)
        postMapper.partialUpdate(post, dto); // title, content 등 업데이트

        // 파일 처리 (기존 파일 삭제 후 새로 업로드)
        handleFileUpdates(files, post);

        Post updatedPost = postRepository.save(post);

        // Entity -> DTO 매핑 (Mapper 사용) + 파일 ID 추가
        PostDTO resultDto = postMapper.toDto(updatedPost);
        // 파일 ID는 Mapper에서 처리하거나, 여기서 별도 설정 (Mapper에 로직 추가 권장)
        if (updatedPost.getFiles() != null) {
            resultDto.setFileIds(updatedPost.getFiles().stream().map(Files::getId).collect(Collectors.toList()));
        } else {
            resultDto.setFileIds(new ArrayList<>());
        }
        // Comment Count는 필요 시 별도 조회 후 설정
        // 예: resultDto.setCommentCount(commentRepository.countByPostId(postId));
        resultDto.setCommentCount(0L); // 임시

        return resultDto;
    }

    @Transactional // 트랜잭션 보장 (Delete)
    public void deletePost(Long postId, Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication); // 로그인 및 사용자 유효성 검사 포함
        Post post = findPostByIdOrThrow(postId);

        // 권한 검사: 게시글 작성자인지 확인
        checkPostOwnership(post, currentUserId);

        // 연관된 파일 먼저 삭제
        fileService.deleteFilesByPostId(postId);

        // 게시글 삭제
        postRepository.delete(post);
    }

    @Transactional(readOnly = true) // 읽기 전용 트랜잭션
    public PostDTO getPostById(Long postId, String clientIp, Authentication authentication) {
        Post post = findPostByIdOrThrow(postId);

        // 조회수 증가 시도 (중복 방지)
        log.info("test {}", clientIp);
        tryIncrementViewCount(post, clientIp, authentication);

        PostDTO postDTO = postMapper.toDto(post);
        postDTO.setUpdatedAt(post.getLastModifiedDate());
        // 파일 ID 설정 (Mapper에서 처리 권장)
        if (post.getFiles() != null) {
            postDTO.setFileIds(post.getFiles().stream().map(Files::getId).collect(Collectors.toList()));
        } else {
            postDTO.setFileIds(new ArrayList<>());
        }
        postDTO.setCommentCount(0L); // 임시


        return postDTO;
    }

    @Transactional(readOnly = true)
    public List<PostDTO> getAllPosts() {
        return postRepository.findAll().stream()
                .map(postMapper::toDto) // Mapper 사용
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PostDTO> getPostsByUserId(Long userId) {
        findUserByIdOrThrow(userId); // 사용자가 존재하는지 확인
        return postRepository.findByAuthorId(userId).stream()
                .map(postMapper::toDto) // Mapper 사용
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PostDTO> getPosts(Long blogId, Long menuId, String searchKeyword, Pageable pageable) {
        // QueryDSL 엔티티 변수 정의 (Q-Type)
        QPost post = QPost.post;
        QMenu menu = QMenu.menu;
        QComment comment = QComment.comment;
        QFiles files = QFiles.files; // Files 엔티티에 대한 Q-Type

        // 1. 검색 대상 메뉴 ID 목록 조회 (하위 메뉴 포함)
        List<Long> targetMenuIds = (menuId != null) ? menuRepository.findAllMenuIdsByParent(menuId) : null;

        // 2. 기본 게시글 조회 쿼리 (Post 엔티티 조회, 연관된 Menu, Author는 fetch join)
        JPQLQuery<Post> baseQuery = queryFactory
                .selectFrom(post)
                .join(post.menu, menu).fetchJoin() // N+1 방지 (Menu)
                .join(post.author).fetchJoin()    // N+1 방지 (Author)
                .where(
                        menu.blog.id.eq(blogId), // 특정 블로그의 게시글
                        menuId != null ? post.menu.id.in(targetMenuIds) : null, // 특정 메뉴 (하위 포함) 필터링
                        searchKeyword != null ? post.title.containsIgnoreCase(searchKeyword) // 제목 또는 내용 검색
                                .or(post.content.containsIgnoreCase(searchKeyword)) : null
                )
                .orderBy(post.createdDate.desc()); // 최신 생성일 순 정렬

        // 3. 페이징 적용하여 게시글 엔티티 목록 조회
        // pagingUtil은 내부적으로 offset, limit 등을 적용하여 쿼리를 실행하고 Page 객체를 반환한다고 가정
        Page<Post> postPage = pagingUtil.getPage(baseQuery, pageable);
        List<Post> posts = postPage.getContent();

        // 조회된 게시글이 없으면 빈 페이지 반환
        if (posts.isEmpty()) {
            return Page.empty(pageable);
        }

        // 4. 조회된 Post 엔티티 목록에서 ID 추출
        List<Long> postIds = posts.stream()
                .map(Post::getId)
                .collect(Collectors.toList());

        // 5. 댓글 수 조회 (게시글 ID별 그룹핑, Tuple 대신 Projection 클래스 사용)
        Map<Long, Long> commentCountMap = queryFactory
                .select(Projections.constructor(IdCountDTO.class, // 결과 매핑 클래스 지정
                        comment.post.id,
                        comment.count().longValue()))
                .from(comment)
                .where(comment.post.id.in(postIds)) // 조회된 게시글 ID들만 대상
                .groupBy(comment.post.id)           // 게시글 ID로 그룹핑
                .fetch()
                .stream()
                .collect(Collectors.toMap(IdCountDTO::getId, IdCountDTO::getCount)); // Map으로 변환

        // 6. 파일 개수 조회 (게시글 ID별 그룹핑, Tuple 대신 Projection 클래스 사용)
        Map<Long, Long> fileCountMap = queryFactory
                .select(Projections.constructor(IdCountDTO.class, // 결과 매핑 클래스 지정
                        files.post.id,
                        files.count().longValue()))
                .from(files)
                .where(files.post.id.in(postIds)) // 조회된 게시글 ID들만 대상
                .groupBy(files.post.id)         // 게시글 ID로 그룹핑
                .fetch()
                .stream()
                .collect(Collectors.toMap(IdCountDTO::getId, IdCountDTO::getCount)); // Map으로 변환

        // 7. DTO 변환 및 추가 정보(댓글 수, 파일 수) 설정
        List<PostDTO> postDtos = posts.stream()
                .map(p -> {
                    // 기본 정보는 Mapper를 통해 변환 (Mapper가 commentCount, fileCount 등은 무시하도록 설정 필요)
                    PostDTO dto = postMapper.toDto(p);
                    // 댓글 수 설정 (없으면 0)
                    dto.setCommentCount(commentCountMap.getOrDefault(p.getId(), 0L));
                    // 파일 개수 설정 (없으면 0)
                    dto.setFileCount(fileCountMap.getOrDefault(p.getId(), 0L));
                    dto.setUpdatedAt(p.getLastModifiedDate());
                    // 파일 ID 목록은 더 이상 설정하지 않음
                    return dto;
                })
                .collect(Collectors.toList());

        // 8. 최종 Page 객체 생성 및 반환 (PageImpl 사용)
        // PageImpl을 직접 반환하므로, 이전의 PageImpl 직렬화 경고 해결 방법(VIA_DTO 또는 Custom DTO)이 적용되어 있어야 함
        return new PageImpl<>(postDtos, pageable, postPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<PostDTO> getPopularPostsByRecentModification(TimePeriod period, int limit) {
        QPost post = QPost.post;
        QComment comment = QComment.comment;
        QMenu menu = QMenu.menu; // fetch join용

        // 1. 기간 필터링을 위한 시작 일시 계산
        LocalDateTime startDateTime = calculateStartDateTime(period);

        // 2. 인기도 점수 계산식 정의
        // viewCount가 null일 경우 0으로 처리
        NumberExpression<Long> popularityScore = post.viewCount.coalesce(0L).multiply(3)
                .add(comment.countDistinct().multiply(5)); // countDistinct() 사용 고려

        // 3. 인기도 점수 기준 내림차순 정렬 (+ 최신 수정일 내림차순 추가)
        OrderSpecifier<?> scoreOrder = popularityScore.desc();
        OrderSpecifier<?> dateOrder = post.lastModifiedDate.desc(); // 2차 정렬 기준

        // 4. 지정된 기간 내의 게시글 ID 목록 조회 (인기도 순)
        List<Long> topPostIds = queryFactory
                .select(post.id)
                .from(post)
                .leftJoin(post.comments, comment) // 댓글 수 계산 위해 조인
                // *** 시간 필터링 조건 추가 ***
                .where(post.lastModifiedDate.goe(startDateTime)) // goe = greater than or equal
                // viewCount는 Post 필드이므로 groupBy에 포함
                .groupBy(post.id, post.viewCount, post.lastModifiedDate) // 정렬 기준 필드도 groupBy에 포함
                .orderBy(scoreOrder, dateOrder) // 인기도 점수 > 최신 수정일 순 정렬
                .limit(limit) // 파라미터로 받은 limit 사용
                .fetch();

        // 조회된 게시글 ID가 없으면 빈 리스트 반환
        if (topPostIds == null || topPostIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 5. 조회된 ID 목록으로 실제 Post 엔티티 조회 (fetch join 포함)
        List<Post> postsUnordered = queryFactory
                .selectFrom(post)
                .join(post.menu, menu).fetchJoin() // 메뉴 정보 fetch join
                .join(post.author).fetchJoin()    // 작성자 정보 fetch join
                .where(post.id.in(topPostIds))   // 조회된 ID 목록에 해당하는 게시글만
                .fetch();

        // 6. 인기도 점수 순서(topPostIds 순서)대로 Post 엔티티 재정렬
        Map<Long, Post> postMap = postsUnordered.stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        List<Post> postsOrdered = topPostIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull) // 혹시 모를 null 제거
                .collect(Collectors.toList());

        // 7. 추가 정보(전체 댓글 수, 파일 수) 조회 및 DTO 변환/설정
        // enrichPostsWithCounts 헬퍼 메소드를 사용하여 DTO 완성
        return enrichPostsWithCounts(postsOrdered);
    }

    /**
     * Post 엔티티 목록에 전체 댓글 수와 파일 수를 조회하여 PostDTO 목록으로 변환합니다.
     * PostMapper는 title, viewCount 등 기본 정보를 매핑한다고 가정합니다.
     * (이전 코드와 동일)
     *
     * @param posts 댓글/파일 수를 추가할 Post 엔티티 목록
     * @return 댓글 수, 파일 수 등이 포함된 PostDTO 목록
     */
    private List<PostDTO> enrichPostsWithCounts(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());

        QComment comment = QComment.comment;
        QFiles files = QFiles.files; // QFiles 객체 import 확인 필요

        // 댓글 수 조회
        Map<Long, Long> commentCountMap = queryFactory
                .select(Projections.constructor(IdCountDTO.class, comment.post.id, comment.countDistinct().longValue())) // countDistinct() 사용 고려
                .from(comment)
                .where(comment.post.id.in(postIds))
                .groupBy(comment.post.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(IdCountDTO::getId, IdCountDTO::getCount));

        // 파일 수 조회
        Map<Long, Long> fileCountMap = queryFactory
                .select(Projections.constructor(IdCountDTO.class, files.post.id, files.countDistinct().longValue())) // countDistinct() 사용 고려
                .from(files)
                .where(files.post.id.in(postIds))
                .groupBy(files.post.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(IdCountDTO::getId, IdCountDTO::getCount));

        // DTO 변환 및 카운트 설정
        return posts.stream()
                .map(p -> {
                    PostDTO dto = postMapper.toDto(p); // 기본 정보 매핑
                    // User 정보가 null일 수 있으므로 null 체크 추가
                    if (p.getAuthor() != null) {
                        dto.setUserId(p.getAuthor().getId());
                        // 필요하다면 author의 다른 정보(닉네임 등)도 설정
                        // dto.setAuthorNickname(p.getAuthor().getNickName());
                    }
                    // Menu 정보가 null일 수 있으므로 null 체크 추가
                    if (p.getMenu() != null) {
                        // 필요하다면 menu의 다른 정보(이름 등)도 설정
                        // dto.setMenuName(p.getMenu().getName());
                    }
                    dto.setUpdatedAt(p.getLastModifiedDate()); // 수정일 설정
                    dto.setCommentCount(commentCountMap.getOrDefault(p.getId(), 0L)); // 댓글 수 설정
                    dto.setFileCount(fileCountMap.getOrDefault(p.getId(), 0L)); // 파일 수 설정
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 기간(DAILY, WEEKLY, MONTHLY)에 따른 시작 일시를 계산합니다.
     * (이전 코드와 동일)
     *
     * @param period 기간 타입
     * @return 해당 기간의 시작 LocalDateTime
     */
    private LocalDateTime calculateStartDateTime(TimePeriod period) {
        LocalDateTime now = LocalDateTime.now(); // 실제 서비스에서는 Clock 주입 사용 고려
        switch (period) {
            case DAILY:
                return now.toLocalDate().atStartOfDay(); // 오늘 0시 0분 0초
            case WEEKLY:
                // 이번 주 월요일 0시 0분 0초 (주의: 한 주의 시작 요일 설정에 따라 달라질 수 있음)
                return now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
            case MONTHLY:
                return now.toLocalDate().withDayOfMonth(1).atStartOfDay(); // 이번 달 1일 0시 0분 0초
            default:
                // 기본값 또는 예외 처리 (여기서는 DAILY와 동일하게 처리)
                // throw new IllegalArgumentException("지원하지 않는 기간입니다: " + period);
                return now.toLocalDate().atStartOfDay();
        }
    }

    /**
     * 인증 정보에서 사용자 ID를 추출합니다. 비로그인 상태거나 유효하지 않은 사용자인 경우 예외를 발생시킵니다.
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // 실제 애플리케이션에서는 Custom Authentication Exception 사용 고려
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        String username = authentication.getName();
        // findByUsernameOrThrow 같은 메소드 사용 고려
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username))
                .getId();
    }

    /**
     * ID로 사용자를 찾아 반환합니다. 없으면 예외를 발생시킵니다.
     */
    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    /**
     * ID로 메뉴를 찾아 반환합니다. 없으면 예외를 발생시킵니다.
     */
    private Menu findMenuByIdOrThrow(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + menuId));
    }

    /**
     * ID로 게시글을 찾아 반환합니다. 없으면 예외를 발생시킵니다.
     */
    private Post findPostByIdOrThrow(Long postId) {
        // 성능 고려 시 fetch join으로 연관 엔티티 미리 로드 가능
        // 예: postRepository.findWithAuthorAndMenuById(postId)
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));
    }


    /**
     * 메뉴가 속한 블로그의 소유권이 현재 사용자와 일치하는지 확인합니다.
     */
    private void checkBlogOwnership(Menu menu, Long currentUserId) {
        if (!Objects.equals(menu.getBlog().getUser().getId(), currentUserId)) {
            throw new IllegalStateException("해당 블로그에 접근할 권한이 없습니다.");
        }
    }

    /**
     * 게시글의 작성자가 현재 사용자와 일치하는지 확인합니다.
     */
    private void checkPostOwnership(Post post, Long currentUserId) {
        if (!Objects.equals(post.getAuthor().getId(), currentUserId)) {
            throw new IllegalStateException("해당 게시글에 대한 권한이 없습니다.");
        }
    }

    /**
     * 게시글 수정 시 메뉴 변경 요청을 처리합니다.
     */
    private void handleMenuUpdate(Long requestedMenuId, Post post, Long currentUserId) {
        // 메뉴 변경 요청이 있고, 기존 메뉴와 다를 경우
        if (requestedMenuId != null && !Objects.equals(requestedMenuId, post.getMenu().getId())) {
            Menu newMenu = findMenuByIdOrThrow(requestedMenuId);
            // 변경하려는 메뉴가 속한 블로그의 소유권 확인
            checkBlogOwnership(newMenu, currentUserId);
            post.setMenu(newMenu);
        }
    }

    /**
     * 첨부된 파일들을 업로드하고 게시글과 연결합니다.
     */
    private void handleFileUploads(List<MultipartFile> files, Post post) throws IOException {
        if (files != null && !files.isEmpty()) {
            List<FileDTO> uploadedFileDtos = fileService.uploadFiles(files, post.getId());
            List<Files> fileEntities = mapFileDtosToEntities(uploadedFileDtos, post);
            post.setFiles(fileEntities); // 연관관계 설정
            // fileRepository.saveAll(fileEntities); // Cascade 설정에 따라 필요 없을 수 있음
        }
    }

    /**
     * 게시글 수정 시 파일을 처리합니다. (기존 파일 삭제 후 새로 업로드)
     */
    private void handleFileUpdates(List<MultipartFile> files, Post post) throws IOException {
        // 새로 첨부된 파일이 있을 경우에만 기존 파일 삭제 및 재업로드 수행
        if (files != null && !files.isEmpty()) {
            // 1. 기존 파일 삭제 (DB 및 실제 파일)
            fileService.deleteFilesByPostId(post.getId());
            // 2. Post 엔티티에서 기존 파일 컬렉션 비우기 (JPA가 orphanRemoval=true 등으로 자동 처리하게 둘 수도 있음)
            if (post.getFiles() != null) {
                post.getFiles().clear();
            } else {
                post.setFiles(new ArrayList<>());
            }

            // 3. 새 파일 업로드 및 연결
            List<FileDTO> uploadedFileDtos = fileService.uploadFiles(files, post.getId());
            List<Files> newFileEntities = mapFileDtosToEntities(uploadedFileDtos, post);
            post.getFiles().addAll(newFileEntities); // 새 파일 추가
        }
        // 파일을 첨부하지 않은 경우는 기존 파일 유지 (삭제 로직 없음)
    }

    /**
     * FileDTO 리스트를 Files 엔티티 리스트로 변환합니다.
     */
    private List<Files> mapFileDtosToEntities(List<FileDTO> fileDtos, Post post) {
        return fileDtos.stream()
                .map(dto -> {
                    Files file = new Files();
                    // ID는 DTO에 이미 할당되어 있다고 가정 (FileService에서 반환 시)
                    file.setId(dto.getId());
                    file.setFileName(dto.getFileName());
                    file.setFilePath(dto.getFilePath());
                    file.setContentType(dto.getContentType());
                    file.setFileSize(dto.getFileSize());
                    file.setPost(post); // 연관관계 설정
                    return file;
                })
                .collect(Collectors.toList());
    }

    /**
     * Redis를 이용하여 중복 조회를 방지하고 게시글 조회수를 증가시킵니다.
     *
     * @return 조회수가 실제로 증가했으면 true, 아니면 false
     */
    private boolean tryIncrementViewCount(Post post, String clientIp, Authentication authentication) {
        String redisKey = buildViewCountRedisKey(post.getId(), clientIp, authentication);
        log.info("Generated Redis Key for view count check: {}", redisKey);

        Boolean isNewView = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", VIEW_COUNT_TTL_SECONDS, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(isNewView)) {
            int updatedRows = postRepository.incrementViewCount(post.getId());

            if (updatedRows > 0) {
                // DB 업데이트 성공. 현재 메모리에 있는 post 객체의 viewCount도 수동으로 증가시켜
                // 메소드 반환 직후 DTO 변환 시 최신 값을 반영하도록 함.
                // (DB는 이미 +1 되었지만, 이 post 객체는 직접 업데이트하지 않으면 이전 값을 가짐)
                post.setViewCount(post.getViewCount() == null ? 1L : post.getViewCount() + 1);
                return true; // 조회수 증가 성공
            } else {
                // 업데이트가 되지 않은 경우 (예: ID가 잘못됨 등). 로그 기록 등 고려
                return false;
            }
        }
        return false; // 이미 조회한 경우
    }

    private String buildViewCountRedisKey(Long postId, String clientIp, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            // 로그인 사용자: post ID + user ID + IP 조합
            Long userId = getCurrentUserId(authentication); // 내부적으로 예외 처리 포함
            return String.format(VIEW_COUNT_KEY_PREFIX_USER, postId, userId, clientIp);
        } else {
            // 비로그인 사용자: post ID + IP 조합
            return String.format(VIEW_COUNT_KEY_PREFIX_ANONYMOUS, postId, clientIp);
        }
    }
}