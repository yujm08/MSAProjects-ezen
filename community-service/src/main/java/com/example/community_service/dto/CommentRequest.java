package com.example.community_service.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {
    private Long postId;  // 댓글이 작성될 게시글 ID
    private Long parentCommentId;  // 대댓글인 경우 부모 댓글 ID (없으면 null)
    private String content;  // 댓글 내용
}
