package com.example.community_service.service;

import com.example.community_service.dto.PostRequest;
import com.example.community_service.dto.PostResponse;
import com.example.community_service.entity.Files;
import com.example.community_service.entity.Posts;
import com.example.community_service.exception.UnauthorizedException;
import com.example.community_service.exception.NotFoundException;
import com.example.community_service.repository.CommentRepository;
import com.example.community_service.repository.FileRepository;
import com.example.community_service.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class PostService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final FileRepository fileRepository;
    private final ModelMapper modelMapper;

    @Value("${spring.file-upload.upload-dir}")
    private String uploadDir;  // application.yml에서 설정한 경로를 자동으로 주입

    // 게시글 작성 메서드 (파일 업로드 처리)
    @Transactional
    public PostResponse createPost(UUID userId, String title, String content, List<MultipartFile> files) {
        Posts post = Posts.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .build();

        Posts savedPost = postRepository.save(post);

        // 파일 저장
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                String filePath = saveFile(file);
                log.debug("File saved at: " + filePath);  // 로그 추가
                Files fileEntity = Files.builder()
                        .fileName(file.getOriginalFilename())
                        .filePath(filePath)
                        .fileSize(file.getSize())
                        .fileType(file.getContentType())
                        .post(savedPost)
                        .build();
                fileRepository.save(fileEntity);
            }
        }

        return toResponse(savedPost);
    }

    // 파일을 지정된 경로에 저장하는 메서드
    private String saveFile(MultipartFile file) {
        try {
            // 애플리케이션 실행 경로를 기준으로 상대 경로를 절대 경로로 변환
            String absolutePath = System.getProperty("user.dir") + "/uploads";  // 실행 디렉토리 하위에 uploads 폴더를 설정

            // 파일 이름 생성
            String fileName = file.getOriginalFilename();
            String filePath = absolutePath + "/" + fileName;

            // 디렉토리가 없다면 생성
            File directory = new File(absolutePath);
            if (!directory.exists()) {
                directory.mkdirs();  // 디렉토리 생성
            }

            // 실제 파일 경로로 파일 저장
            File destinationFile = new File(filePath);
            file.transferTo(destinationFile);

            return filePath;  // 저장된 파일 경로 반환
        } catch (IOException e) {
            throw new RuntimeException("파일 저장에 실패했습니다.", e);
        }
    }

    // 게시글 수정
    public PostResponse updatePost(Long postId, UUID userId, PostRequest request) {
        Posts post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));

        if (!post.getUserId().equals(userId)) {
            throw new UnauthorizedException("No permission to update");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());

        Posts updatedPost = postRepository.save(post);
        return toResponse(updatedPost);
    }

    // 게시글 삭제
    @Transactional
    public void deletePost(Long postId, UUID userId) {
        Posts post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));

        if (!post.getUserId().equals(userId)) {
            throw new UnauthorizedException("No permission to delete");
        }

        // **1️⃣ 관련 댓글 먼저 삭제**
        commentRepository.deleteByPostId(postId);

        // **2️⃣ 게시글 삭제**
        postRepository.delete(post);
    }

    // 모든 게시글 검색
    public Page<PostResponse> getAllPosts(Pageable pageable) {
        Page<Posts> posts = postRepository.findAll(pageable);
        return posts.map(this::toResponse);
    }
    

    // 게시글 조회 (조회수 증가)
    @Transactional
    public PostResponse getPost(Long postId) {
        Posts post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));

        // 조회수 증가 (비동기적 처리할 수도 있음)
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);

        return toResponse(post);
    }

    // 게시글 검색 (제목, 내용, 작성자)
    public Page<PostResponse> searchPosts(String keyword, Pageable pageable) {
        Page<Posts> posts = postRepository.findByTitleContainingOrContentContaining(keyword, keyword, pageable);
        return posts.map(this::toResponse);
    }

    // 게시글 목록 (인기순)
    public Page<PostResponse> getPopularPosts(Pageable pageable) {
        Page<Posts> posts = postRepository.findPopularPosts(pageable);
        return posts.map(this::toResponse);
    }

    // 게시글 조회수 증가 메서드 (Redis 또는 MySQL에서 처리)
    public void incrementViewCount(Long postId) {
        // Redis를 사용하면 조회수 증가를 빠르게 처리하고, 일정 주기마다 MySQL로 동기화 가능
        // 예: Redis에서 조회수 증가 후 일정 시간마다 MySQL에 반영
    }

    // DTO 변환
    private PostResponse toResponse(Posts post) {
        PostResponse response = modelMapper.map(post, PostResponse.class);
        
        // getLikes()가 null인 경우 빈 리스트로 처리
        int likeCount = post.getLikes() != null ? post.getLikes().size() : 0;
        response.setLikeCount(likeCount);  // 좋아요 수 설정
        
        log.debug("PostResponse: " + response);  // 로그 추가
        return response;
    }

    @GetMapping("/files/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        Files fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File not found"));

        Path path = Paths.get(fileEntity.getFilePath());
        Resource resource = new FileSystemResource(path);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileEntity.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileEntity.getFileName() + "\"")
                .body(resource);
    }
}
