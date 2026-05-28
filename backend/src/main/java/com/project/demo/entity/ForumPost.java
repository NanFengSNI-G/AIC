package com.project.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("forum_post")
public class ForumPost {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;        // 发布者ID
    private Long sectionId;     // 板块ID
    private String title;       // 帖子标题
    private String content;     // 帖子内容
    private String images;      // 图片URL列表
    private Integer likeCount;  // 点赞数
    private Integer commentCount; // 评论数
    private Integer viewCount;  // 浏览数
    private Integer isSticky;   // 是否置顶
    private Integer status;     // 状态: 0-已删除, 1-正常, 2-草稿(未发布)

    public static final int STATUS_DELETED = 0;
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_DRAFT = 2;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String authorUsername;  // 发布者用户名

    @TableField(exist = false)
    private String authorAvatar;    // 发布者头像

    @TableField(exist = false)
    private String sectionName;    // 板块名称

    @TableField(exist = false)
    private Boolean isLiked;       // 当前用户是否点赞

    @TableField(exist = false)
    private Boolean isFavorited;   // 当前用户是否收藏
}
