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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

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
    private final NotificationProducer notificationProducer; // Kafka 프로듀서 주입

    public CommentDTO createComment(CommentDTO dto, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Post post = postRepository.findById(dto.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + dto.getPostId()));

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        Comment comment = commentMapper.toEntity(dto);
        comment.setPost(post);
        comment.setUser(author);
        comment.setParent(null);

        Comment savedComment = commentRepository.save(comment);
        CommentDTO result = commentMapper.toDto(savedComment);
        result.setUpdatedAt(comment.getLastModifiedDate());
        result.setAuthorName(author.getNickName() != null ? author.getNickName() : author.getUsername());

        // 게시글 주인에게 알림 발송 (작성자가 게시글 주인이 아닌 경우에만)
        if (!post.getAuthor().getId().equals(userId)) {
            NotificationEvent event = new NotificationEvent();
            event.setEventType("NEW_COMMENT");
            event.setUserId(post.getAuthor().getId()); // 게시글 주인
            event.setMessage("새로운 댓글이 있습니다.");
            event.setPostId(post.getId());
            event.setCommentId(savedComment.getId());

            notificationProducer.sendNotification(event);
            log.info("새로운 댓글 알림 발송: userId={}, postId={}, commentId={}",
                    post.getAuthor().getId(), post.getId(), savedComment.getId());
        }

        return result;
    }

    public CommentDTO createReply(Long parentId, CommentDTO dto, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Post post = postRepository.findById(dto.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + dto.getPostId()));

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다: " + parentId));

        if (!parent.getPost().getId().equals(dto.getPostId())) {
            throw new IllegalStateException("부모 댓글은 같은 게시글에 속해야 합니다.");
        }

        Comment reply = commentMapper.toEntity(dto);
        reply.setPost(post);
        reply.setUser(author);
        reply.setParent(parent);

        Comment savedReply = commentRepository.save(reply);
        CommentDTO result = commentMapper.toDto(savedReply);
        result.setUpdatedAt(reply.getLastModifiedDate());
        result.setAuthorName(author.getNickName() != null ? author.getNickName() : author.getUsername());

        // 부모 댓글 작성자에게 알림 발송 (답글 작성자가 부모 댓글 작성자가 아닌 경우에만)
        if (!parent.getUser().getId().equals(userId)) {
            NotificationEvent event = new NotificationEvent();
            event.setEventType("NEW_REPLY");
            event.setUserId(parent.getUser().getId()); // 부모 댓글 작성자
            event.setMessage("새로운 답글이 있습니다.");
            event.setPostId(post.getId());
            event.setCommentId(savedReply.getId());
            event.setParentId(parentId);

            notificationProducer.sendNotification(event);
            log.info("새로운 답글 알림 발송: userId={}, postId={}, commentId={}, parentId={}",
                    parent.getUser().getId(), post.getId(), savedReply.getId(), parentId);
        }

        return result;
    }

    public CommentDTO updateComment(Long commentId, CommentDTO dto, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalStateException("댓글을 수정할 권한이 없습니다.");
        }

        if (comment.isDeleted()) {
            throw new IllegalStateException("삭제된 댓글은 수정할 수 없습니다.");
        }

        commentMapper.partialUpdate(comment, dto);

        Comment updatedComment = commentRepository.save(comment);
        CommentDTO result = commentMapper.toDto(updatedComment);
        result.setAuthorName(comment.getUser().getNickName() != null ? comment.getUser().getNickName() : comment.getUser().getUsername());
        return result;
    }

    public void deleteComment(Long commentId, Long userId, Authentication authentication) {
        getUserIdFromAuthentication(authentication); // 로그인 체크
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalStateException("댓글을 삭제할 권한이 없습니다.");
        }

        if (!comment.isDeleted()) {
            comment.markAsDeleted();
            commentRepository.save(comment);
        }
    }

    public List<CommentDTO> getCommentsByPostId(Long postId) {
        if (postId == null) {
            throw new IllegalArgumentException("게시글 ID는 null일 수 없습니다.");
        }
        List<Comment> comments = commentRepository.findByPostId(postId);
        // 루트 댓글만 반환
        return comments.stream()
                .filter(comment -> comment.getParent() == null)
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private CommentDTO convertToDto(Comment comment) {
        CommentDTO dto = commentMapper.toDto(comment);
        dto.setPostId(comment.getPost() != null ? comment.getPost().getId() : null);
        dto.setUserId(comment.getUser() != null ? comment.getUser().getId() : null);
        dto.setParentId(comment.getParent() != null ? comment.getParent().getId() : null); // parentId 설정
        dto.setUpdatedAt(comment.getLastModifiedDate());
        Long userId = dto.getUserId();
        if (userId != null) {
            User author = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자 ID " + userId + "를 찾을 수 없습니다."));
            dto.setAuthorName(author.getNickName() != null ? author.getNickName() : author.getUsername());
            dto.setEmail(author.getEmail());
        } else {
            dto.setAuthorName("익명");
        }

        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            List<CommentDTO> replyDtos = comment.getReplies().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            dto.setReplies(replyDtos);
        }

        return dto;
    }

    public List<CommentDTO> getRepliesByCommentId(Long commentId) {
        List<Comment> replies = commentRepository.findByParentId(commentId);
        return replies.stream().map(comment -> {
            CommentDTO dto = commentMapper.toDto(comment);
            User author = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + dto.getUserId()));
            dto.setAuthorName(author.getNickName() != null ? author.getNickName() : author.getUsername());
            return dto;
        }).collect(Collectors.toList());
    }

    public CommentDTO getCommentById(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));
        CommentDTO result = commentMapper.toDto(comment);
        result.setAuthorName(comment.getUser().getNickName() != null ? comment.getUser().getNickName() : comment.getUser().getUsername());
        return result;
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