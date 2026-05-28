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
@TableName("forum_section")
public class ForumSection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;         // 板块名称
    private String description;  // 板块描述
    private String icon;         // 板块图标
    private Integer sortOrder;   // 排序
    private Integer postCount;   // 帖子总数
    private Integer status;      // 状态: 0-禁用, 1-正常
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
