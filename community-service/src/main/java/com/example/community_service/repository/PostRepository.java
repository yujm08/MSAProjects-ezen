package com.example.community_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import com.example.community_service.entity.Posts; // 엔티티 임포트 필요

public interface PostRepository extends JpaRepository<Posts, Long> {
    // 검색용
    Page<Posts> findByTitleContainingOrContentContaining(String title, String content, Pageable pageable);

    // 인기순 정렬
    @Query("SELECT p FROM Posts p LEFT JOIN p.likes l GROUP BY p ORDER BY COUNT(l) DESC, p.createdAt DESC")
    Page<Posts> findPopularPosts(Pageable pageable);
}
