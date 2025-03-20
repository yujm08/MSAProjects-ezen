package com.example.community_service.service;

import com.example.community_service.dto.CommentRequest;
import com.example.community_service.dto.CommentResponse;
import com.example.community_service.entity.Comments;
import com.example.community_service.entity.Posts;
import com.example.community_service.exception.NotFoundException;
import com.example.community_service.exception.UnauthorizedException;
import com.example.community_service.repository.CommentRepository;
import com.example.community_service.repository.PostRepository;
import lombok.RequiredArgsConstructor;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ModelMapper modelMapper;

    // 댓글 작성
    public CommentResponse createComment(UUID userId, CommentRequest request) {
        Posts post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new NotFoundException("Post not found"));

        Comments parent = null;
        if (request.getParentCommentId() != null) {
            parent = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new NotFoundException("Parent comment not found"));
        }

        Comments comment = Comments.builder()
                .post(post)
                .userId(userId)  // userId를 UUID로 변경
                .parentComment(parent)
                .content(request.getContent())
                .build();
        Comments savedComment = commentRepository.save(comment);

        return toResponse(savedComment);
    }

    // 댓글 수정
    public CommentResponse updateComment(Long commentId, UUID userId, String content) {
        Comments comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));

        if (!comment.getUserId().equals(userId)) {
            throw new UnauthorizedException("No permission to update");
        }

        comment.setContent(content);
        Comments updatedComment = commentRepository.save(comment);
        return toResponse(updatedComment);
    }

    // 댓글 삭제
    public void deleteComment(Long commentId, UUID userId) {
        Comments comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));

        if (!comment.getUserId().equals(userId)) {
            throw new UnauthorizedException("No permission to delete");
        }

        commentRepository.delete(comment);
    }

    // 댓글 조회 (부모 댓글 및 대댓글 포함)
    public List<CommentResponse> getCommentsByPost(Long postId, Sort sort) {
        List<Comments> comments = commentRepository.findByPostIdAndParentCommentIsNull(postId, sort);
        return comments.stream()
                .map(this::mapWithReplies)
                .collect(Collectors.toList());
    }

    // 댓글의 대댓글을 조회
    private CommentResponse mapWithReplies(Comments comment) {
        CommentResponse response = toResponse(comment);
        List<Comments> replies = commentRepository.findByParentCommentId(comment.getId(), Sort.by(Sort.Order.asc("createdAt")));
        if (!replies.isEmpty()) {
            List<CommentResponse> replyResponses = replies.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
            response.setReplies(replyResponses);
        }
        return response;
    }

    // Comment 엔티티를 CommentResponse DTO로 변환
    private CommentResponse toResponse(Comments comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .userId(comment.getUserId())  // userId를 UUID로 설정
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replies(new ArrayList<>())  // 대댓글 초기화
                .build();
    }
}
