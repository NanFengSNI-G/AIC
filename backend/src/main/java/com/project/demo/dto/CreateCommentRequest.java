package com.project.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    @NotBlank(message = "评论内容不能为空")
    @Size(min = 1, max = 2000, message = "评论长度必须在1-2000字符之间")
    private String content;    // 评论内容

    private Long parentId;      // 父评论ID（可选，用于回复）
}
