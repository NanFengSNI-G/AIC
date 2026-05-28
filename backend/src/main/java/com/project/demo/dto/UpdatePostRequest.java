package com.project.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostRequest {

    @NotBlank(message = "标题不能为空")
    @Size(min = 1, max = 200, message = "标题长度必须在1-200字符之间")
    private String title;      // 帖子标题

    @NotBlank(message = "内容不能为空")
    @Size(min = 1, max = 50000, message = "内容长度不能超过50000字符")
    private String content;    // 帖子内容

    private String images;     // 图片URL列表
}
