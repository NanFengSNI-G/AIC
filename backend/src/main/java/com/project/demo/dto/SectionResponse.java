package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionResponse {

    private Long id;          // 板块ID
    private String name;      // 板块名称
    private String description; // 板块描述
    private String icon;      // 板块图标
    private Integer postCount; // 帖子总数
}
