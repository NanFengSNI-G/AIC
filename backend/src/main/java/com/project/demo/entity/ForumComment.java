package com.project.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("forum_comment")
public class ForumComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;        // 帖子ID
    private Long userId;        // 评论者ID
    private Long parentId;      // 父评论ID（NULL表示一级评论）
    private String content;     // 评论内容
    private Integer likeCount;  // 点赞数
    private Integer status;     // 状态: 0-已删除, 1-正常
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String authorUsername;  // 评论者用户名

    @TableField(exist = false)
    private String authorAvatar;    // 评论者头像

    @TableField(exist = false)
    private String replyUsername;   // 被回复的用户名（二级评论）

    @TableField(exist = false)
    private Boolean isLiked;        // 当前用户是否已点赞

    @TableField(exist = false)
    private List<ForumComment> replies; // 子评论列表
}
