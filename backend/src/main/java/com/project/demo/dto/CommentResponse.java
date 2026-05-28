package com.project.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private Long id;               // 评论ID
    private Long postId;           // 帖子ID
    private Long userId;           // 评论者ID
    private String authorUsername; // 评论者用户名
    private String authorAvatar;   // 评论者头像
    private Long parentId;         // 父评论ID
    private String parentUsername; // 父评论用户名（被回复的用户）
    private String content;       // 评论内容
    private Integer likeCount;    // 点赞数
    private Boolean isLiked;      // 当前用户是否已点赞
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private List<CommentResponse> replies; // 子评论列表
}
