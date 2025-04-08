package com.glowrise.service;

import com.glowrise.domain.Comment;
import com.glowrise.domain.Post;
import com.glowrise.domain.User;
import com.glowrise.repository.CommentRepository;
import com.glowrise.repository.PostRepository;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.CommentDTO;
import com.glowrise.service.dto.NotificationEvent;
import com.glowrise.service.mapper.CommentMapper;
import com.glowrise.service.util.NotificationProducer;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationProducer notificationProducer;

    // --- 공개 API 메소드 ---

    @Transactional
    public CommentDTO createComment(CommentDTO dto, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication); // 인증된 사용자 ID 가져오기
        User author = findUserByIdOrThrow(userId); // 작성자 한 번 조회
        Post post = findPostByIdOrThrow(dto.getPostId()); // 게시글 조회 또는 예외 발생

        Comment comment = commentMapper.toEntity(dto);
        comment.setPost(post);
        comment.setUser(author);
        comment.setParent(null); // 루트 댓글은 명시적으로 null

        Comment savedComment = commentRepository.save(comment);
        log.info("게시글 {}에 댓글 생성됨: commentId={}", post.getId(), savedComment.getId());

        // 댓글 작성자가 게시글 작성자가 아닌 경우 알림 발송
        if (!post.getAuthor().getId().equals(userId)) {
            sendCommentNotification(post.getAuthor().getId(), post.getId(), savedComment.getId());
        }

        // 저장 및 알림 발송 가능성 후에 DTO로 매핑
        return mapCommentToDtoWithAuthor(savedComment);
    }

    @Transactional
    public CommentDTO createReply(Long parentId, CommentDTO dto, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication); // 인증된 사용자 ID 가져오기
        User author = findUserByIdOrThrow(userId); // 작성자 조회
        Post post = findPostByIdOrThrow(dto.getPostId()); // 게시글 조회 또는 예외 발생
        Comment parent = findCommentByIdOrThrow(parentId); // 부모 댓글 조회 또는 예외 발생

        // 부모 댓글이 동일한 게시글에 속하는지 확인
        if (!parent.getPost().getId().equals(post.getId())) {
            throw new IllegalStateException("부모 댓글이 지정된 게시글에 속하지 않습니다.");
        }

        Comment reply = commentMapper.toEntity(dto);
        reply.setPost(post);
        reply.setUser(author);
        reply.setParent(parent);

        Comment savedReply = commentRepository.save(reply);
        log.info("댓글 {}에 대한 답글 생성됨: replyId={}", parentId, savedReply.getId());

        // 답글 작성자가 부모 댓글 작성자가 아닌 경우 알림 발송
        if (!parent.getUser().getId().equals(userId)) {
            sendReplyNotification(parent.getUser().getId(), post.getId(), savedReply.getId(), parentId);
        }

        return mapCommentToDtoWithAuthor(savedReply);
    }

    @Transactional
    public CommentDTO updateComment(Long commentId, CommentDTO dto, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication); // 인증된 사용자 ID 가져오기
        Comment comment = findCommentByIdOrThrow(commentId); // 댓글 조회 또는 예외 발생

        // 소유권 확인
        ensureCommentOwnership(comment, userId);

        // 삭제된 댓글 수정 방지
        if (comment.isDeleted()) {
            throw new IllegalStateException("삭제된 댓글은 수정할 수 없습니다.");
        }

        commentMapper.partialUpdate(comment, dto); // DTO로부터 변경 사항 적용
        Comment updatedComment = commentRepository.save(comment); // 변경 사항 저장
        log.info("댓글 업데이트됨: {}", commentId);

        return mapCommentToDtoWithAuthor(updatedComment);
    }

    @Transactional
    public void deleteComment(Long commentId, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication); // 소유권 확인을 위해 사용자 ID 가져오기
        Comment comment = findCommentByIdOrThrow(commentId); // 댓글 조회 또는 예외 발생

        // 소유권 확인
        ensureCommentOwnership(comment, userId);

        // 소프트 삭제: 아직 삭제되지 않았다면 삭제됨으로 표시
        if (!comment.isDeleted()) {
            comment.markAsDeleted(); // Comment 엔티티에 이 메소드가 존재한다고 가정
            commentRepository.save(comment);
            log.info("댓글이 삭제됨으로 표시됨: {}", commentId);
        } else {
            log.info("댓글 {}은(는) 이미 삭제됨으로 표시되었습니다.", commentId);
        }
        // 참고: 부모 댓글 삭제가 자식 댓글에 영향을 미쳐야 하는지 고려 (예: 자식 댓글도 삭제됨으로 표시?)
        // 이는 요구사항에 따라 다릅니다. 현재 로직은 대상 댓글만 삭제합니다.
    }

    // --- 조회 메소드 ---

    @Transactional(readOnly = true)
    public List<CommentDTO> getCommentsByPostId(Long postId) {
        if (postId == null) {
            throw new IllegalArgumentException("게시글 ID는 null일 수 없습니다.");
        }
        // 최적화: 레포지토리에서 직접 루트 댓글만 조회
        List<Comment> rootComments = commentRepository.findByPostIdAndParentIsNull(postId);
        // 지연 로딩된 답글이 N+1 문제를 일으키는 경우 EntityGraph 사용 고려
        return rootComments.stream()
                .map(this::mapCommentToDtoWithAuthorAndReplies) // 재귀 매핑 헬퍼 사용
                .collect(Collectors.toList());
    }

    // 사용되지 않음? 또는 직접 답글 조회가 필요하다면 유지. getCommentsByPostId가 답글을 포함할 수 있음.
    @Transactional(readOnly = true)
    public List<CommentDTO> getRepliesByCommentId(Long parentCommentId) {
        Comment parent = findCommentByIdOrThrow(parentCommentId); // 부모 존재 확인
        List<Comment> replies = commentRepository.findByParentId(parentCommentId);
        return replies.stream()
                .map(this::mapCommentToDtoWithAuthor) // 답글 매핑, 보통 여기서 답글의 답글까지 매핑할 필요는 없음
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CommentDTO getCommentById(Long commentId) {
        Comment comment = findCommentByIdOrThrow(commentId); // 댓글 조회 또는 예외 발생
        // 여기서 답글을 포함해야 하는지 결정. 그렇다면 mapCommentToDtoWithAuthorAndReplies 사용
        return mapCommentToDtoWithAuthor(comment);
    }

    // --- 비공개 헬퍼 메소드 ---

    private Long getAuthenticatedUserId(Authentication authentication) {
        // BlogService와 동일한 로직 재사용, 공유 컴포넌트/유틸로 이동 고려
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("인증이 필요합니다.");
        }
        String username = authentication.getName();
        return findUserByUsernameOrThrow(username).getId();
    }

    private User findUserByUsernameOrThrow(String username) {
        // BlogService와 동일한 로직 재사용
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다 (사용자 이름): " + username));
    }

    private User findUserByIdOrThrow(Long userId) {
        // BlogService와 동일한 로직 재사용
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다 (ID): " + userId));
    }

    private Post findPostByIdOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다 (ID): " + postId));
    }

    private Comment findCommentByIdOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다 (ID): " + commentId));
    }

    private void ensureCommentOwnership(Comment comment, Long userId) {
        if (comment.getUser() == null || !comment.getUser().getId().equals(userId)) {
            log.warn("사용자 {}가 댓글 {} 수정을 시도하여 접근 거부됨", userId, comment.getId());
            throw new AccessDeniedException("이 댓글을 수정할 권한이 없습니다.");
        }
    }

    /**
     * Comment 엔티티를 DTO로 매핑하고 작성자 정보를 추가합니다.
     */
    private CommentDTO mapCommentToDtoWithAuthor(Comment comment) {
        CommentDTO dto = commentMapper.toDto(comment); // 기본 매핑
        if (comment.getUser() != null) {
            // Mapstruct는 user.nickName과 같은 중첩 속성을 매핑하도록 구성할 수 있습니다.
            // 그렇지 않은 경우 수동으로 추가해야 합니다:
            dto.setAuthorName(comment.getUser().getNickName() != null ? comment.getUser().getNickName() : comment.getUser().getUsername());
            dto.setEmail(comment.getUser().getEmail()); // DTO에 필요한 경우 이메일 추가
            dto.setUserId(comment.getUser().getId());
        } else {
            dto.setAuthorName("알 수 없는 사용자"); // 또는 요구사항에 따라 처리
            dto.setUserId(null);
        }
        dto.setUpdatedAt(comment.getLastModifiedDate()); // 타임스탬프 매핑 확인
        dto.setPostId(comment.getPost() != null ? comment.getPost().getId() : null);
        dto.setParentId(comment.getParent() != null ? comment.getParent().getId() : null);

        // 간단한 경우 중복 조회를 피하기 위해 여기서 답글을 매핑하지 않음
        // dto.setReplies(null); // 또는 매퍼가 기본적으로 매핑하지 않도록 보장

        return dto;
    }

    /**
     * Comment 엔티티를 DTO로 매핑하고, 작성자 정보를 추가하며 답글을 재귀적으로 매핑합니다.
     * 적절한 조회 전략 없이는 깊은 중첩이나 성능 문제를 피하기 위해 신중하게 사용하세요.
     */
    private CommentDTO mapCommentToDtoWithAuthorAndReplies(Comment comment) {
        CommentDTO dto = mapCommentToDtoWithAuthor(comment); // 작성자 정보가 포함된 기본 DTO 가져오기

        // 답글이 존재하고 로드된 경우 재귀적으로 매핑 (EntityGraph 또는 fetch join 없이는 N+1 주의)
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            dto.setReplies(comment.getReplies().stream()
                    .map(this::mapCommentToDtoWithAuthorAndReplies) // 재귀 호출
                    .collect(Collectors.toList()));
        } else {
            dto.setReplies(List.of()); // 답글 리스트가 초기화되었는지 확인 (비어 있음)
        }

        return dto;
    }


    // --- 알림 헬퍼 메소드 ---


    private void sendCommentNotification(Long recipientUserId, Long postId, Long commentId) {
        // NotificationEvent 객체 생성
        NotificationEvent event = new NotificationEvent();

        // Setter 메소드를 사용하여 필드 값 설정
        event.setEventType("NEW_COMMENT");
        event.setUserId(recipientUserId);
        event.setMessage("게시글에 새 댓글이 달렸습니다.");
        event.setPostId(postId);
        event.setCommentId(commentId);
        // event.setParentId(null); // 필요하다면 명시적으로 null 설정

        try {
            notificationProducer.sendNotification(event);
            log.info("게시글 {}, 댓글 {}에 대한 NEW_COMMENT 알림 발송됨", postId, commentId);
        } catch (Exception e) {
            log.error("게시글 {}, 댓글 {}에 대한 NEW_COMMENT 알림 발송 실패", postId, commentId, e);
            // Kafka 오류 처리 방법 결정 (예: 재시도, 로그 기록, 무시?)
        }
    }

    private void sendReplyNotification(Long recipientUserId, Long postId, Long replyId, Long parentCommentId) {
        // NotificationEvent 객체 생성
        NotificationEvent event = new NotificationEvent();

        // Setter 메소드를 사용하여 필드 값 설정
        event.setEventType("NEW_REPLY");
        event.setUserId(recipientUserId);
        event.setMessage("댓글에 답글이 달렸습니다.");
        event.setPostId(postId);
        event.setCommentId(replyId);
        event.setParentId(parentCommentId); // 답글의 경우 parentId 설정

        try {
            notificationProducer.sendNotification(event);
            log.info("부모 댓글 {}, 답글 {}에 대한 NEW_REPLY 알림 발송됨", parentCommentId, replyId);
        } catch (Exception e) {
            log.error("부모 댓글 {}, 답글 {}에 대한 NEW_REPLY 알림 발송 실패", parentCommentId, replyId, e);
            // Kafka 오류 처리 방법 결정
        }
    }
}