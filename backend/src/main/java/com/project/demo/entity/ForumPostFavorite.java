package com.project.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("forum_post_favorite")
public class ForumPostFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;        // 帖子ID
    private Long userId;       // 用户ID
    private LocalDateTime createTime;
}