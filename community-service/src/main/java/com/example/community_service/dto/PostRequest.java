package com.example.community_service.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import io.micrometer.common.lang.Nullable;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {
    private String title;
    private String content;

    // 파일 업로드 처리를 위한 필드
    @Nullable // 파일이 없어도 에러가 발생하지 않도록 설정
    private List<MultipartFile> files;  // MultipartFile로 파일 리스트 받기
}

