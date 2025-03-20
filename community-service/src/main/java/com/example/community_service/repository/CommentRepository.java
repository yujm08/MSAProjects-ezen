package com.example.community_service.repository;

import com.example.community_service.entity.Comments;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comments, Long> {
    
    // 특정 게시글의 최상위 댓글 조회 (부모 댓글이 없는 것만)
    List<Comments> findByPostIdAndParentCommentIsNull(Long postId, Sort sort);

    // 특정 부모 댓글의 대댓글 조회
    List<Comments> findByParentCommentId(Long parentCommentId, Sort sort);

    @Transactional
    void deleteByPostId(Long postId);

}
