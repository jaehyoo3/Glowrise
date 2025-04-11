package com.glowrise.service;

import com.glowrise.domain.Comment;
import com.glowrise.domain.Post;
import com.glowrise.domain.User;
import com.glowrise.repository.CommentRepository;
import com.glowrise.repository.PostRepository;
import com.glowrise.service.dto.CommentDTO;
import com.glowrise.service.dto.NotificationEvent;
import com.glowrise.service.mapper.CommentMapper;
import com.glowrise.service.util.NotificationProducer;
import com.glowrise.service.util.SecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final PostRepository postRepository;
    private final SecurityUtil securityUtil;
    private final NotificationProducer notificationProducer;

    @Transactional
    public CommentDTO createComment(CommentDTO dto, Authentication ignoredAuthentication) {
        User author = securityUtil.getCurrentUserOrThrow();
        Long userId = author.getId();
        Post post = findPostByIdOrThrow(dto.getPostId());

        Comment comment = commentMapper.toEntity(dto);
        comment.setPost(post);
        comment.setUser(author);
        comment.setParent(null);

        Comment savedComment = commentRepository.save(comment);

        if (!post.getAuthor().getId().equals(userId)) {
            sendCommentNotification(post.getAuthor().getId(), post.getId(), savedComment.getId());
        }

        return mapCommentToDtoWithAuthor(savedComment);
    }

    @Transactional
    public CommentDTO createReply(Long parentId, CommentDTO dto, Authentication ignoredAuthentication) {
        User author = securityUtil.getCurrentUserOrThrow();
        Long userId = author.getId();
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

        if (!parent.getUser().getId().equals(userId)) {
            sendReplyNotification(parent.getUser().getId(), post.getId(), savedReply.getId(), parentId);
        }

        return mapCommentToDtoWithAuthor(savedReply);
    }

    @Transactional
    @PreAuthorize("@authorizationService.isCommentOwner(#commentId)")
    public CommentDTO updateComment(Long commentId, CommentDTO dto, Authentication ignoredAuthentication) {
        Comment comment = findCommentByIdOrThrow(commentId);

        if (comment.isDeleted()) {
            throw new IllegalStateException("삭제된 댓글은 수정할 수 없습니다.");
        }

        commentMapper.partialUpdate(comment, dto);
        Comment updatedComment = commentRepository.save(comment);

        return mapCommentToDtoWithAuthor(updatedComment);
    }

    @Transactional
    @PreAuthorize("@authorizationService.isCommentOwner(#commentId)")
    public void deleteComment(Long commentId, Authentication ignoredAuthentication) {
        Comment comment = findCommentByIdOrThrow(commentId);

        if (!comment.isDeleted()) {
            comment.markAsDeleted();
            commentRepository.save(comment);
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

    private Post findPostByIdOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다 (ID): " + postId));
    }

    private Comment findCommentByIdOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다 (ID): " + commentId));
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
        notificationProducer.sendNotification(event);
    }

    private void sendReplyNotification(Long recipientUserId, Long postId, Long replyId, Long parentCommentId) {
        NotificationEvent event = new NotificationEvent();
        event.setEventType("NEW_REPLY");
        event.setUserId(recipientUserId);
        event.setMessage("댓글에 답글이 달렸습니다.");
        event.setPostId(postId);
        event.setCommentId(replyId);
        event.setParentId(parentCommentId);
        notificationProducer.sendNotification(event);
    }
}