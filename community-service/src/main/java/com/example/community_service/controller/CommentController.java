package com.example.community_service.controller;

import com.example.community_service.dto.CommentRequest;
import com.example.community_service.dto.CommentResponse;
import com.example.community_service.service.CommentService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 댓글 작성 (userId를 UUID로 받음)
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @RequestParam UUID userId,  // userId를 UUID로 받음
            @RequestBody CommentRequest request
    ) {
        CommentResponse response = commentService.createComment(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 댓글 수정
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @RequestParam UUID userId,  // userId를 UUID로 받음
            @RequestBody Map<String, String> body
    ) {
        String content = body.get("content");
        CommentResponse response = commentService.updateComment(commentId, userId, content);
        return ResponseEntity.ok(response);
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @RequestParam UUID userId  // userId를 UUID로 받음
    ) {
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    // 특정 게시글의 댓글 조회
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<CommentResponse>> getCommentsByPost(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        // ❌ 기존: "desc"라는 필드가 없어서 오류 발생
        // Sort sorting = Sort.by(sort.split(",")); 

        // ✅ 수정: "createdAt" 필드 기준 내림차순 정렬
        Sort sorting = Sort.by(Sort.Direction.DESC, "createdAt");

        List<CommentResponse> responses = commentService.getCommentsByPost(postId, sorting);
        return ResponseEntity.ok(responses);
    }

}
