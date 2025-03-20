package com.example.community_service.controller;

import com.example.community_service.dto.PostRequest;
import com.example.community_service.dto.PostResponse;
import com.example.community_service.service.PostService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 게시글 작성 (userId를 UUID로 받음)
    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @RequestParam UUID userId,  // UUID 형식으로 userId를 받음
            @ModelAttribute PostRequest request
    ) {
        PostResponse response = postService.createPost(userId, request.getTitle(), request.getContent(), request.getFiles());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 게시글 수정
    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long postId,
            @RequestParam UUID userId,  // UUID 형식으로 userId를 받음
            @ModelAttribute PostRequest request
    ) {
        PostResponse response = postService.updatePost(postId, userId, request);
        return ResponseEntity.ok(response);
    }

    // 게시글 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @RequestParam UUID userId  // UUID 형식으로 userId를 받음
    ) {
        postService.deletePost(postId, userId);
        return ResponseEntity.noContent().build();
    }

    // 모든 게시글 조회
    @GetMapping
    public ResponseEntity<Page<PostResponse>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostResponse> result = postService.getAllPosts(pageable);
        return ResponseEntity.ok(result);
    }

    // 게시글 조회
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long postId) {
        PostResponse response = postService.getPost(postId);
        return ResponseEntity.ok(response);
    }

    // 게시글 검색
    @GetMapping("/search")
    public ResponseEntity<Page<PostResponse>> searchPosts(
            @RequestParam String keyword,  // 검색 키워드
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostResponse> result = postService.searchPosts(keyword, pageable);
        return ResponseEntity.ok(result);
    }

    // 인기순 게시글 조회
    @GetMapping("/popular")
    public ResponseEntity<Page<PostResponse>> getPopularPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> result = postService.getPopularPosts(pageable);
        return ResponseEntity.ok(result);
    }

    // 조회수 증가 (게시글 조회 시 자동 호출되는 방식)
    @PostMapping("/{postId}/increment-view")
    public ResponseEntity<Void> incrementViewCount(@PathVariable Long postId) {
        postService.incrementViewCount(postId);
        return ResponseEntity.noContent().build();
    }
}
