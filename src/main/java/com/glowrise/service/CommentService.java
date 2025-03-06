package com.glowrise.service;

import com.glowrise.domain.Comment;
import com.glowrise.domain.Post;
import com.glowrise.domain.User;
import com.glowrise.repository.CommentRepository;
import com.glowrise.repository.PostRepository;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.CommentDTO;
import com.glowrise.service.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // 댓글 생성
    public CommentDTO createComment(CommentDTO dto) {
        Post post = postRepository.findById(dto.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + dto.getPostId()));

        User author = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + dto.getUserId()));

        Comment comment = commentMapper.toEntity(dto);
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setParent(null); // 일반 댓글은 부모가 없음

        Comment savedComment = commentRepository.save(comment);
        return commentMapper.toDto(savedComment);
    }

    // 답글 생성
    public CommentDTO createReply(Long parentId, CommentDTO dto) {
        Post post = postRepository.findById(dto.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + dto.getPostId()));

        User author = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + dto.getUserId()));

        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다: " + parentId));

        // 부모 댓글이 같은 게시글에 속하는지 확인
        if (!parent.getPost().getId().equals(dto.getPostId())) {
            throw new IllegalStateException("부모 댓글은 같은 게시글에 속해야 합니다.");
        }

        Comment reply = commentMapper.toEntity(dto);
        reply.setPost(post);
        reply.setAuthor(author);
        reply.setParent(parent);

        Comment savedReply = commentRepository.save(reply);
        return commentMapper.toDto(savedReply);
    }

    // 댓글 수정
    public CommentDTO updateComment(Long commentId, CommentDTO dto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        if (!comment.getAuthor().getId().equals(dto.getUserId())) {
            throw new IllegalStateException("댓글을 수정할 권한이 없습니다.");
        }

        if (comment.isDeleted()) {
            throw new IllegalStateException("삭제된 댓글은 수정할 수 없습니다.");
        }

        // 내용만 업데이트
        commentMapper.partialUpdate(comment, dto);

        Comment updatedComment = commentRepository.save(comment);
        return commentMapper.toDto(updatedComment);
    }

    // 댓글 삭제 (논리적 삭제)
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new IllegalStateException("댓글을 삭제할 권한이 없습니다.");
        }

        if (!comment.isDeleted()) {
            comment.markAsDeleted();
            commentRepository.save(comment);
        }
    }

    // 게시글별 댓글 목록 조회
    public List<CommentDTO> getCommentsByPostId(Long postId) {
        List<Comment> comments = commentRepository.findByPostId(postId);
        return commentMapper.toDto(comments);
    }

    // 특정 댓글의 답글 목록 조회
    public List<CommentDTO> getRepliesByCommentId(Long commentId) {
        List<Comment> replies = commentRepository.findByParentId(commentId);
        return commentMapper.toDto(replies);
    }

    // 특정 댓글 조회
    public CommentDTO getCommentById(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));
        return commentMapper.toDto(comment);
    }
}