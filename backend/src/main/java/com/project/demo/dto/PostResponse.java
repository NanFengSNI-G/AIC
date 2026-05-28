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
public class PostResponse {

    private Long id;              // 帖子ID
    private Long userId;          // 发布者ID
    private String authorUsername; // 发布者用户名
    private String authorAvatar;  // 发布者头像
    private Long sectionId;       // 板块ID
    private String sectionName;   // 板块名称
    private String title;        // 帖子标题
    private String content;       // 帖子内容
    private List<String> images;  // 图片列表
    private Integer likeCount;   // 点赞数
    private Integer commentCount; // 评论数
    private Integer viewCount;   // 浏览数
    private Boolean isLiked;      // 当前用户是否点赞
    private Boolean isFavorited; // 当前用户是否收藏
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
