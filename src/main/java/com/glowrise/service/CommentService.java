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


    @Transactional
    public CommentDTO createComment(CommentDTO dto, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        User author = findUserByIdOrThrow(userId);
        Post post = findPostByIdOrThrow(dto.getPostId());

        Comment comment = commentMapper.toEntity(dto);
        comment.setPost(post);
        comment.setUser(author);
        comment.setParent(null);

        Comment savedComment = commentRepository.save(comment);
        log.info("게시글 {}에 댓글 생성됨: commentId={}", post.getId(), savedComment.getId());

        if (!post.getAuthor().getId().equals(userId)) {
            sendCommentNotification(post.getAuthor().getId(), post.getId(), savedComment.getId());
        }

        return mapCommentToDtoWithAuthor(savedComment);
    }

    @Transactional
    public CommentDTO createReply(Long parentId, CommentDTO dto, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        User author = findUserByIdOrThrow(userId);
        Post post = findPostByIdOrThrow(dto.getPostId());
        Comment parent = findCommentByIdOrThrow(parentId);

        if (!parent.getPost().getId().equals(post.getId())) {
            throw new IllegalStateException("부모 댓글이 지정된 게시글에 속하지 않습니다.");
        }

        Comment reply = commentMapper.toEntity(dto);
        reply.setPost(post);
        reply.setUser(author);
        reply.setParent(parent);

        Comment savedReply = commentRepository.save(reply);
        log.info("댓글 {}에 대한 답글 생성됨: replyId={}", parentId, savedReply.getId());

        if (!parent.getUser().getId().equals(userId)) {
            sendReplyNotification(parent.getUser().getId(), post.getId(), savedReply.getId(), parentId);
        }

        return mapCommentToDtoWithAuthor(savedReply);
    }

    @Transactional
    public CommentDTO updateComment(Long commentId, CommentDTO dto, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        Comment comment = findCommentByIdOrThrow(commentId);

        ensureCommentOwnership(comment, userId);

        if (comment.isDeleted()) {
            throw new IllegalStateException("삭제된 댓글은 수정할 수 없습니다.");
        }

        commentMapper.partialUpdate(comment, dto);
        Comment updatedComment = commentRepository.save(comment);
        log.info("댓글 업데이트됨: {}", commentId);

        return mapCommentToDtoWithAuthor(updatedComment);
    }

    @Transactional
    public void deleteComment(Long commentId, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        Comment comment = findCommentByIdOrThrow(commentId);

        ensureCommentOwnership(comment, userId);

        if (!comment.isDeleted()) {
            comment.markAsDeleted();
            commentRepository.save(comment);
            log.info("댓글이 삭제됨으로 표시됨: {}", commentId);
        } else {
            log.info("댓글 {}은(는) 이미 삭제됨으로 표시되었습니다.", commentId);
        }
    }

    @Transactional(readOnly = true)
    public List<CommentDTO> getCommentsByPostId(Long postId) {
        if (postId == null) {
            throw new IllegalArgumentException("게시글 ID는 null일 수 없습니다.");
        }
        List<Comment> rootComments = commentRepository.findByPostIdAndParentIsNull(postId);
        return rootComments.stream()
                .map(this::mapCommentToDtoWithAuthorAndReplies)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommentDTO> getRepliesByCommentId(Long parentCommentId) {
        Comment parent = findCommentByIdOrThrow(parentCommentId);
        List<Comment> replies = commentRepository.findByParentId(parentCommentId);
        return replies.stream()
                .map(this::mapCommentToDtoWithAuthor)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CommentDTO getCommentById(Long commentId) {
        Comment comment = findCommentByIdOrThrow(commentId);
        return mapCommentToDtoWithAuthor(comment);
    }


    private Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("인증이 필요합니다.");
        }
        String username = authentication.getName();
        return findUserByUsernameOrThrow(username).getId();
    }

    private User findUserByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다 (사용자 이름): " + username));
    }

    private User findUserByIdOrThrow(Long userId) {
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


    private CommentDTO mapCommentToDtoWithAuthor(Comment comment) {
        CommentDTO dto = commentMapper.toDto(comment);
        if (comment.getUser() != null) {
            dto.setAuthorName(comment.getUser().getNickName() != null ? comment.getUser().getNickName() : comment.getUser().getUsername());
            dto.setEmail(comment.getUser().getEmail());
            dto.setUserId(comment.getUser().getId());
        } else {
            dto.setAuthorName("알 수 없는 사용자");
            dto.setUserId(null);
        }
        dto.setUpdatedAt(comment.getLastModifiedDate());
        dto.setPostId(comment.getPost() != null ? comment.getPost().getId() : null);
        dto.setParentId(comment.getParent() != null ? comment.getParent().getId() : null);

        return dto;
    }


    private CommentDTO mapCommentToDtoWithAuthorAndReplies(Comment comment) {
        CommentDTO dto = mapCommentToDtoWithAuthor(comment);

        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            dto.setReplies(comment.getReplies().stream()
                    .map(this::mapCommentToDtoWithAuthorAndReplies)
                    .collect(Collectors.toList()));
        } else {
            dto.setReplies(List.of());
        }

        return dto;
    }


    private void sendCommentNotification(Long recipientUserId, Long postId, Long commentId) {
        NotificationEvent event = new NotificationEvent();

        event.setEventType("NEW_COMMENT");
        event.setUserId(recipientUserId);
        event.setMessage("게시글에 새 댓글이 달렸습니다.");
        event.setPostId(postId);
        event.setCommentId(commentId);

        try {
            notificationProducer.sendNotification(event);
            log.info("게시글 {}, 댓글 {}에 대한 NEW_COMMENT 알림 발송됨", postId, commentId);
        } catch (Exception e) {
            log.error("게시글 {}, 댓글 {}에 대한 NEW_COMMENT 알림 발송 실패", postId, commentId, e);
        }
    }

    private void sendReplyNotification(Long recipientUserId, Long postId, Long replyId, Long parentCommentId) {
        NotificationEvent event = new NotificationEvent();

        event.setEventType("NEW_REPLY");
        event.setUserId(recipientUserId);
        event.setMessage("댓글에 답글이 달렸습니다.");
        event.setPostId(postId);
        event.setCommentId(replyId);
        event.setParentId(parentCommentId);

        try {
            notificationProducer.sendNotification(event);
            log.info("부모 댓글 {}, 답글 {}에 대한 NEW_REPLY 알림 발송됨", parentCommentId, replyId);
        } catch (Exception e) {
            log.error("부모 댓글 {}, 답글 {}에 대한 NEW_REPLY 알림 발송 실패", parentCommentId, replyId, e);
        }
    }
}