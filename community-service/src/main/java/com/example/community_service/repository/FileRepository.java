package com.example.community_service.repository;

import com.example.community_service.entity.Files;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<Files, Long> {
    List<Files> findByPostId(Long postId);
}
