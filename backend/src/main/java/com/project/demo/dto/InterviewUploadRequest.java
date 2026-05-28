package com.project.demo.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 面试上传请求
 */
@Data
public class InterviewUploadRequest {

    /**
     * 简历文件（PDF 或 Word）
     */
    @NotNull(message = "简历文件不能为空")
    private MultipartFile file;

    /**
     * JD 文本内容
     */
    @NotBlank(message = "JD 内容不能为空")
    private String jdText;
}
